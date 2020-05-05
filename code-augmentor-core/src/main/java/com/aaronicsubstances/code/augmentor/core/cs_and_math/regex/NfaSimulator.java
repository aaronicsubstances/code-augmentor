package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;

/**
 * Simulates NFA on an string of input symbols in order to match it.
 * <p>
 * Implements Algorithm 3.22 of Compilers - Principles, Techniques and Tools
 * (aka Dragon Book), 2nd edition.
 */
public class NfaSimulator {
    private final FiniteStateAutomaton nfa;
    private final Map<Integer, Set<Integer>> emptyStringGraph;

    private Set<Integer> statesUnderObservation;
    private List<Observation> observations;

    public NfaSimulator(FiniteStateAutomaton nfa) {
        this.nfa = nfa;

        // build adjacency list type of graph with empty string transitions 
        emptyStringGraph = NfaToDfaConvertor.buildEmptyStringGraph(nfa);
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public int simulate(int[] input, Set<Integer> statesUnderObservation) {
        this.statesUnderObservation = statesUnderObservation;
        observations = new ArrayList<>();

        int i = 0;
        Set<Integer> subsetOfStates = NfaToDfaConvertor.emptyStringClosure(emptyStringGraph,
            new HashSet<>(Arrays.asList(nfa.getStartState())));
        recordObservation(subsetOfStates, i);
        while (i < input.length) {
            int c = input[i];
            subsetOfStates = NfaToDfaConvertor.move(nfa, subsetOfStates, c);
            subsetOfStates = NfaToDfaConvertor.emptyStringClosure(emptyStringGraph, subsetOfStates);
            if (subsetOfStates.isEmpty()) {
                break;
            }
            i++;
            recordObservation(subsetOfStates, i);
        }

        // look for intersection of state subset and final states
        // which is expected to have only 1 state.
        Set<Integer> finalStates = nfa.getFinalStates();
        for (int candidateFinalState : subsetOfStates) {
            if (finalStates.contains(candidateFinalState)) {
                return -1;
            }
        }
        return i;
    }

    private void recordObservation(Set<Integer> subsetOfStates, int endIndex) {
        if (statesUnderObservation == null) return;
        
        Set<Integer> observedStates = new HashSet<>();
        for (Integer state : subsetOfStates) {
            if (statesUnderObservation.contains(state)) {
                observedStates.add(state);
            }
        }
        if (!observedStates.isEmpty()) {
            observations.add(new Observation(observedStates, endIndex));
        }
    }

    public static class Observation {
        private final Set<Integer> states;
        private final int endIndex;

        public Observation(Set<Integer> states, int endIndex) {
            this.states = states;
            this.endIndex = endIndex;
        }

        public Set<Integer> getStates() {
            return states;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }
}