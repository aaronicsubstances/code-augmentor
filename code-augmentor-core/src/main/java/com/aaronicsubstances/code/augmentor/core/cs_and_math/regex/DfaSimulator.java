package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;

/**
 * Simulates DFA on an string of input symbols in order to match it.
 * <p>
 * Implements Algorithm 3.18 of Compilers - Principles, Techniques and Tools
 * (aka Dragon Book), 2nd edition.
 */
public class DfaSimulator {
    private final FiniteStateAutomaton dfa;

    private Set<Integer> statesUnderObservation;
    private List<Observation> observations;

    public DfaSimulator(FiniteStateAutomaton dfa) {
        this.dfa = dfa;
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public int simulate(int[] input, Set<Integer> statesUnderObservation) {
        this.statesUnderObservation = statesUnderObservation;
        observations = new ArrayList<>();
        int i = 0;
        int s = dfa.getStartState();
        recordObservation(s, i);
        while (i < input.length) {
            int c = input[i];
            Map<Integer, Integer> stateOutTransitions = dfa.getDfaTransitionTable().get(s);
            if (stateOutTransitions == null || !stateOutTransitions.containsKey(c)) {
                break;
            }
            s = stateOutTransitions.get(c);
            i++;
            recordObservation(s, i);
        }
        if (dfa.getFinalStates().contains(s)) {
            return -1;
        }
        return i;
    }

    private void recordObservation(int state, int endIndex) {
        if (statesUnderObservation != null && 
                statesUnderObservation.contains(state)) {
            observations.add(new Observation(state, endIndex));
        }
    }

    public static class Observation {
        private final int state;
        private final int endIndex;

        public Observation(int state, int endIndex) {
            this.state = state;
            this.endIndex = endIndex;
        }

        public int getState() {
            return state;
        }

        public int getEndIndex() {
            return endIndex;
        }
    }
}