package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
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

    /**
     * Simulates this NFA to match a substring of symbols
     * @param input string of symbols.
     * @param startIndex inclusive start index into input string
     * @param endIndex exclusive end index int input string
     * @param statesUnderObservation optional state numbers to observe during simulation.
     * At start and upon any advance in input string, if there are next states in this
     * list, a record is made with those states against the position in the input string 
     * at that moment.
     * @return -1 if simulation successfully matched all symbols in specified substring of
     * input; a positive integer indicating the position after the last successful match
     * is returned; or 0 if a match was never made in the string.
     */
    public int simulate(int[] input, int startIndex, int endIndex, Set<Integer> statesUnderObservation) {
        this.statesUnderObservation = statesUnderObservation;
        observations = new ArrayList<>();

        int i = startIndex;
        Set<Integer> subsetOfStates = NfaToDfaConvertor.emptyStringClosure(emptyStringGraph,
            FiniteStateAutomaton.newSet(nfa.getStartState()));
        recordObservation(subsetOfStates, i);
        while (i < endIndex) {
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
        
        Set<Integer> observedStates = FiniteStateAutomaton.newSet();
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