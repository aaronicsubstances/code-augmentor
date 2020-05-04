package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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
    private final LinkedList<Integer> oldStateStack;
    private final LinkedList<Integer> newStateStack; 
    private final boolean[] alreadyOn;

    private Set<Integer> statesUnderObservation;
    private List<Observation> observations;

    public NfaSimulator(FiniteStateAutomaton nfa) {
        this.nfa = nfa;
        this.oldStateStack = new LinkedList<>();
        this.newStateStack = new LinkedList<>();
        int maxState = 0;
        if (!nfa.getStates().isEmpty()) {
            maxState = Collections.max(nfa.getStates());
        }
        this.alreadyOn = new boolean[maxState + 1];
    }

    public List<Observation> getObservations() {
        return observations;
    }

    public int simulate(int[] input, Set<Integer> statesUnderObservation) {
        this.statesUnderObservation = statesUnderObservation;
        observations = new ArrayList<>();

        int i = 0;
        initialEmptyStringClosure();
        recordObservation(i);
        while (i < input.length) {
            int c = input[i];
            emptyStringClosure(c);
            if (oldStateStack.isEmpty()) {
                break;
            }
            i++;
            recordObservation(i);
        }

        // look for intersection of oldStateStack and final states
        // which is expected to have only 1 state.
        Set<Integer> finalStates = nfa.getFinalStates();
        while (!oldStateStack.isEmpty()) {
            int candidateFinalState = oldStateStack.pop();
            if (finalStates.contains(candidateFinalState)) {
                return -1;
            }
        }
        return i;
    }

    private void initialEmptyStringClosure() {
        addState(nfa.getStartState());
        while (!newStateStack.isEmpty()) {
            int s = newStateStack.pop();
            oldStateStack.push(s);
            alreadyOn[s] = false;
        }
    }

    private void emptyStringClosure(int c) {
        while (!oldStateStack.isEmpty()) {
            int s = oldStateStack.pop();
            Set<Integer> moveResult = move(s, c);
            if (moveResult != null) {
                for (int t : moveResult) {
                    if (!alreadyOn[t]) {
                        addState(t);
                    }
                }
            }
        }
        while (!newStateStack.isEmpty()) {
            int s = newStateStack.pop();
            oldStateStack.push(s);
            alreadyOn[s] = false;
        }
    }

    private void addState(int s) {
        newStateStack.push(s);
        alreadyOn[s] = true;
        Set<Integer> moveResult = move(s, FiniteStateAutomaton.NULL_SYMBOL);
        if (moveResult != null) {
            for (int t : moveResult) {
                if (!alreadyOn[t]) {
                    addState(t);
                }
            }
        }
    }

    private Set<Integer> move(int state, int c) {
        Map<Integer, Set<Integer>> stateOutTransitions = nfa.getNfaTransitionTable().get(state);
        if (stateOutTransitions == null) {
            return null;
        }
        return stateOutTransitions.get(c);
    }

    private void recordObservation(int endIndex) {
        if (statesUnderObservation == null) return;
        
        Set<Integer> observedStates = new HashSet<>();
        for (Integer state : oldStateStack) {
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