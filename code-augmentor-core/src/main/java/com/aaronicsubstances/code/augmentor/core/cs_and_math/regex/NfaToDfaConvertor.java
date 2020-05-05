package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;

/**
 * Converts an NFA to a DFA.
 * <p>
 * Implements Algorithm 3.20 of Compilers - Principles, Techniques and Tools
 * (aka Dragon Book), 2nd edition.
 * Also known as subset construction of DFA from NFA.
 * 
 */
public class NfaToDfaConvertor {
    private final FiniteStateAutomaton nfa;
    private final Map<Integer, Set<Integer>> emptyStringGraph;

    private List<Set<Integer>> nfaStateSubsets;
    
    public NfaToDfaConvertor(FiniteStateAutomaton nfa) {
        this.nfa = nfa;

        // build adjacency list type of graph with empty string transitions 
        emptyStringGraph = buildEmptyStringGraph(nfa);
    }

    public List<Set<Integer>> getNfaStateSubsets() {
        return nfaStateSubsets;
    }
    
    public FiniteStateAutomaton convert(boolean ignoreEmptyState) {
        Set<Integer> dfaStates = new HashSet<>();
        Set<Integer> dfaFinalStates = new HashSet<>();
        Map<Integer, Map<Integer, Integer>> dfaTransitionTable = new HashMap<>();
        nfaStateSubsets = new ArrayList<>();
        Set<Integer> startNfaStateSubset = emptyStringClosure(emptyStringGraph,
            new HashSet<>(Arrays.asList(nfa.getStartState())));
        nfaStateSubsets.add(startNfaStateSubset);
        int processedCount = 0;
        while (processedCount < nfaStateSubsets.size()) {
            int nextDState = processedCount;
            Set<Integer> nextNfaStateSubset = nfaStateSubsets.get(nextDState);
            processedCount++;

            dfaStates.add(nextDState);
            for (int s : nextNfaStateSubset) {
                if (nfa.getFinalStates().contains(s)) {
                    dfaFinalStates.add(nextDState);
                    break;
                }
            }

            // skip transitions from empty states if requested.
            if (ignoreEmptyState && nextNfaStateSubset.isEmpty()) {
                continue;
            }
            
            // for each input symbol discover new states
            Map<Integer, Integer> dfaOutStateTransitions = new HashMap<>();
            dfaTransitionTable.put(nextDState, dfaOutStateTransitions);
            for (int c : nfa.getAlphabet()) {
                Set<Integer> discoveredNfaStateSubset = move(nfa, nextNfaStateSubset, c);
                discoveredNfaStateSubset = emptyStringClosure(emptyStringGraph,
                    discoveredNfaStateSubset);
                int discoveredDState = nfaStateSubsets.indexOf(discoveredNfaStateSubset);
                if (discoveredDState == -1) {
                    discoveredDState = nfaStateSubsets.size();
                    nfaStateSubsets.add(discoveredNfaStateSubset);
                }
                if (ignoreEmptyState && discoveredNfaStateSubset.isEmpty()) {
                    // skip transitions to empty state if requested.
                }
                else {
                    dfaOutStateTransitions.put(c, discoveredDState);
                }
            }
        }
        FiniteStateAutomaton dfa = new FiniteStateAutomaton(nfa.getAlphabet(), 
            dfaStates, 0, dfaFinalStates, null, dfaTransitionTable);
        return dfa;
    }

    static Set<Integer> move(FiniteStateAutomaton nfa, Set<Integer> states, int c) {
        Set<Integer> nextStates = new HashSet<>();
        for (int s : states) {
            Map<Integer, Set<Integer>> stateOutTransitions = nfa.getNfaTransitionTable().get(s);
            if (stateOutTransitions != null &&
                    stateOutTransitions.containsKey(c)) {
                nextStates.addAll(stateOutTransitions.get(c));
            }
        }
        return nextStates;
    }

    static Map<Integer, Set<Integer>> buildEmptyStringGraph(FiniteStateAutomaton nfa) {
        Map<Integer, Set<Integer>> emptyStringGraph = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, Set<Integer>>> entry : 
                nfa.getNfaTransitionTable().entrySet()) {
            if (entry.getValue().containsKey(FiniteStateAutomaton.NULL_SYMBOL)) {
                emptyStringGraph.put(entry.getKey(), entry.getValue().get(
                    FiniteStateAutomaton.NULL_SYMBOL));
            }
        }
        return emptyStringGraph;
    }

    static Set<Integer> emptyStringClosure(
            Map<Integer, Set<Integer>> emptyStringGraph,
            Set<Integer> startStates) {
        // NB: resembles breadth first search graph algorithm.
        Set<Integer> closureResult = new HashSet<>(startStates);
        LinkedList<Integer> processedStates = new LinkedList<>(startStates);
        while (!processedStates.isEmpty()) {
            int t = processedStates.removeFirst();
            if (emptyStringGraph.containsKey(t)) {
                for (int u : emptyStringGraph.get(t)) {
                    if (!closureResult.contains(u)) {
                        closureResult.add(u);
                        processedStates.addLast(u);
                    }
                }
            }
        }
        return closureResult;
    }
}