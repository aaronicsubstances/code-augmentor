package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FiniteStateAutomaton {
    public static final int NULL_SYMBOL = -1;

    private final Set<Integer> alphabet;
    private final Set<Integer> states;
    private final int startState;
    private final Set<Integer> finalStates;
    private final Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable;
    private final Map<Integer, Map<Integer, Integer>> dfaTransitionTable;

    public FiniteStateAutomaton(Set<Integer> alphabet,
            Set<Integer> states, int startState, Set<Integer> finalStates,            
            Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable,            
            Map<Integer, Map<Integer, Integer>> dfaTransitionTable) {
        this.states = states;
        this.startState = startState;
        this.finalStates = finalStates;
        this.alphabet = alphabet;
        this.nfaTransitionTable = nfaTransitionTable;
        this.dfaTransitionTable = dfaTransitionTable;
    }

    public Set<Integer> getAlphabet() {
        return alphabet;
    }

    public Set<Integer> getStates() {
        return states;
    }

    public int getStartState() {
        return startState;
    }

    public Set<Integer> getFinalStates() {
        return finalStates;
    }

    public Map<Integer, Map<Integer, Set<Integer>>> getNfaTransitionTable() {
        return nfaTransitionTable;
    }

    public Map<Integer, Map<Integer, Integer>> getDfaTransitionTable() {
        return dfaTransitionTable;
    }

    public FiniteStateAutomaton generateCopy(Map<Integer, Integer> stateTranslationMap) {
        Set<Integer> newStates = new HashSet<>();
        for (int state : states) {
            int newState = stateTranslationMap.get(state);
            newStates.add(newState);
        }
        int newStartState = stateTranslationMap.get(startState);
        Set<Integer> newFinalStates = new HashSet<>();
        for (int state : finalStates) {
            int newFinalState = stateTranslationMap.get(state);
            newFinalStates.add(newFinalState);
        }
        Map<Integer, Map<Integer, Set<Integer>>> newNfaTransitionTable = null;
        if (nfaTransitionTable != null) {
            newNfaTransitionTable = new HashMap<>();
            for (Map.Entry<Integer, Map<Integer, Set<Integer>>> e : nfaTransitionTable.entrySet()) {
                int newTransitionTableStateKey = stateTranslationMap.get(e.getKey());
                Map<Integer, Set<Integer>> newMap = new HashMap<>();
                newNfaTransitionTable.put(newTransitionTableStateKey, newMap);
                for (Map.Entry<Integer, Set<Integer>> e2 : e.getValue().entrySet()) {
                    Set<Integer> multipleStateValues = new HashSet<>();
                    for (int s : e2.getValue()) {
                        int translated = stateTranslationMap.get(s);
                        multipleStateValues.add(translated);
                    }
                    newMap.put(e2.getKey(), multipleStateValues);
                }
            }
        }
        Map<Integer, Map<Integer, Integer>> newDfaTransitionTable = null;
        if (dfaTransitionTable != null) {
            newDfaTransitionTable = new HashMap<>();
            for (Map.Entry<Integer, Map<Integer, Integer>> e : dfaTransitionTable.entrySet()) {
                int newTransitionTableStateKey = stateTranslationMap.get(e.getKey());
                Map<Integer, Integer> newMap = new HashMap<>();
                newDfaTransitionTable.put(newTransitionTableStateKey, newMap);
                for (Map.Entry<Integer, Integer> e2 : e.getValue().entrySet()) {
                    int singleStateValue = stateTranslationMap.get(e2.getValue());
                    newMap.put(e2.getKey(), singleStateValue);
                }
            }
        }
        FiniteStateAutomaton copy = new FiniteStateAutomaton(alphabet, newStates, newStartState, 
            newFinalStates, newNfaTransitionTable, newDfaTransitionTable);
        return copy;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alphabet == null) ? 0 : alphabet.hashCode());
		result = prime * result + ((dfaTransitionTable == null) ? 0 : dfaTransitionTable.hashCode());
		result = prime * result + ((finalStates == null) ? 0 : finalStates.hashCode());
		result = prime * result + ((nfaTransitionTable == null) ? 0 : nfaTransitionTable.hashCode());
		result = prime * result + startState;
		result = prime * result + ((states == null) ? 0 : states.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FiniteStateAutomaton other = (FiniteStateAutomaton) obj;
		if (alphabet == null) {
			if (other.alphabet != null)
				return false;
		} else if (!alphabet.equals(other.alphabet))
			return false;
		if (dfaTransitionTable == null) {
			if (other.dfaTransitionTable != null)
				return false;
		} else if (!dfaTransitionTable.equals(other.dfaTransitionTable))
			return false;
		if (finalStates == null) {
			if (other.finalStates != null)
				return false;
		} else if (!finalStates.equals(other.finalStates))
			return false;
		if (nfaTransitionTable == null) {
			if (other.nfaTransitionTable != null)
				return false;
		} else if (!nfaTransitionTable.equals(other.nfaTransitionTable))
			return false;
		if (startState != other.startState)
			return false;
		if (states == null) {
			if (other.states != null)
				return false;
		} else if (!states.equals(other.states))
			return false;
		return true;
	}

    @Override
	public String toString() {
        List<Integer> alphabetList = new ArrayList<>();
        if (alphabet != null) {
            alphabetList = setToList(alphabet);
        }
        else {
            // get all symbols which appear in transition tables.
            Set<Integer> temp = new HashSet<>();
            if (nfaTransitionTable != null) {
                for (Map<Integer, Set<Integer>> stateOutTransitions : nfaTransitionTable.values()) {
                    temp.addAll(stateOutTransitions.keySet());
                }
            }
            if (dfaTransitionTable != null) {
                for (Map<Integer, Integer> stateOutTransitions : dfaTransitionTable.values()) {
                    temp.addAll(stateOutTransitions.keySet());
                }
            }
            alphabetList = setToList(temp);
        }
        List<Integer> stateList = setToList(states);
        List<Integer> finalStateList = setToList(finalStates);
        StringBuilder fsaRepr = new StringBuilder();
        fsaRepr.append(FiniteStateAutomaton.class.getSimpleName()).append("{\n");
        fsaRepr.append("alphabet: ").append(setToString(alphabetList)).append("\n");
        fsaRepr.append("states: ").append(setToString(stateList)).append("\n"); 
        fsaRepr.append("start state: ").append(startState).append("\n");
        fsaRepr.append("final states: ").append(setToString(finalStateList)).append("\n");
        Set<Integer> invalidStates = new HashSet<>();
        Set<Integer> invalidSymbols = new HashSet<>();
        List<String[]> table = new ArrayList<>();
        if (nfaTransitionTable != null) {
            // notify of any invalid states and symbols
            // after displaying alphabet above, add null symbol to make it count as valid.
            if (!alphabetList.contains(NULL_SYMBOL)) {
                alphabetList.add(NULL_SYMBOL);
            }
            for (int state : nfaTransitionTable.keySet()) {
                if (!states.contains(state)) {
                    invalidStates.add(state);
                }
                if (nfaTransitionTable.containsKey(state)) {
                    Map<Integer, Set<Integer>> stateOutTransitions = nfaTransitionTable.get(state);
                    for (Map.Entry<Integer, Set<Integer>> entry : stateOutTransitions.entrySet()) {
                        if (!alphabetList.contains(entry.getKey())) {
                            invalidSymbols.add(entry.getKey());
                        }
                        for (int nextState : entry.getValue()) {
                            if (!states.contains(nextState)) {
                                invalidStates.add(nextState);
                            }
                        }
                    }
                }
            }
            // then work with valid states and symbols.
            for (int state : stateList) {
                Map<Integer, Set<Integer>> stateOutTransitions = null;
                if (nfaTransitionTable.containsKey(state)) {
                    stateOutTransitions = nfaTransitionTable.get(state);
                }
                String[] rowForState = new String[alphabetList.size() + 1];
                table.add(rowForState);
                rowForState[0] = "" + state;
                for (int i = 1; i < rowForState.length; i++) {
                    int symbol = alphabetList.get(i - 1);
                    if (stateOutTransitions != null && 
                            stateOutTransitions.containsKey(symbol)) {
                        Set<Integer> nextStates = stateOutTransitions.get(symbol);
                        rowForState[i] = setToString(nextStates);
                    }
                    else {
                        rowForState[i] = "";
                    }
                }
            }
        }
        if (dfaTransitionTable != null) {
            // notify of any invalid states and symbols
            for (int state : dfaTransitionTable.keySet()) {
                if (!states.contains(state)) {
                    invalidStates.add(state);
                }
                if (dfaTransitionTable.containsKey(state)) {
                    Map<Integer, Integer> stateOutTransitions = dfaTransitionTable.get(state);
                    for (Map.Entry<Integer, Integer> entry : stateOutTransitions.entrySet()) {
                        if (!alphabetList.contains(entry.getKey())) {
                            invalidSymbols.add(entry.getKey());
                        }
                        if (!states.contains(entry.getValue())) {
                            invalidStates.add(entry.getValue());
                        }
                    }
                }
            }
            // then work with valid states and symbols.
            for (int state : stateList) {
                Map<Integer, Integer> stateOutTransitions = null;
                if (dfaTransitionTable.containsKey(state)) {
                    stateOutTransitions = dfaTransitionTable.get(state);
                }
                String[] rowForState = new String[alphabetList.size() + 1];
                table.add(rowForState);
                rowForState[0] = "" + state;
                for (int i = 1; i < rowForState.length; i++) {
                    int symbol = alphabetList.get(i - 1);
                    if (stateOutTransitions != null && 
                            stateOutTransitions.containsKey(symbol)) {
                        int nextState = stateOutTransitions.get(symbol);
                        rowForState[i] = "" + nextState;
                    }
                    else {
                        rowForState[i] = "";
                    }
                }
            }
        }
        if (!invalidStates.isEmpty()) {
            fsaRepr.append("invalid states: ");
            fsaRepr.append(setToString(invalidStates)).append("\n");
        }
        if (!invalidSymbols.isEmpty()) {
            fsaRepr.append("invalid symbols: ");
            fsaRepr.append(setToString(invalidSymbols)).append("\n");
        }

        // calculate widest column to use for print table.
        int widestColumn = 0;
        if (!table.isEmpty()) {
            widestColumn = table.stream().flatMap(x -> Arrays.stream(x))
                .map(x -> x.length())
                .max(Integer::compare)            
                .get();
        }        

        // add 2 surrounding spaces to widest column and ensure minimum length of 8
        widestColumn = Math.max(widestColumn + 2, 8);
        
        // calculate table length 
        // - vertical border
        // - state column
        // - vertical border
        // - alphabet column plus vertical border for each alphabet
        int length = 1 + widestColumn + 1 + alphabetList.size() * (widestColumn + 1);

        char horLnChar = '-', vertLnChar = '|';
        String horizontalBorder = strMultiply("" + horLnChar, length) + "\n";

        String title = "DFA";
        if (nfaTransitionTable != null) {
            title = "NFA";
        }

        // begin printing table.
        fsaRepr.append(horizontalBorder);

        // "title" and "input" heading line
        fsaRepr.append(vertLnChar).append(strRightPad(" " + title, widestColumn)).append(vertLnChar);
        if (!alphabetList.isEmpty()) {
            fsaRepr.append(strRightPad(" Input", widestColumn));
            fsaRepr.append(strRightPad("", (alphabetList.size() - 1) * (widestColumn + 1)));
            fsaRepr.append(vertLnChar);
        }
        fsaRepr.append("\n");

        fsaRepr.append(horizontalBorder);

        // "state" heading and heading for each alphabet
        fsaRepr.append(vertLnChar);
        fsaRepr.append(strRightPad(" State", widestColumn));
        fsaRepr.append(vertLnChar);
        for (int symbol : alphabetList) {
            fsaRepr.append(strRightPad(" " + symbol, widestColumn)).append(vertLnChar);
        }
        fsaRepr.append("\n");

        fsaRepr.append(horizontalBorder);

        // print table content rows.
        for (String[] row : table) {
            fsaRepr.append(vertLnChar);
            for (String cell : row) {
                fsaRepr.append(strRightPad(" " + cell, widestColumn)).append(vertLnChar);
            }
            fsaRepr.append("\n");
            fsaRepr.append(horizontalBorder);
        }

        // end of table printing

        fsaRepr.append("}").append("\n");
        return fsaRepr.toString();
    }
    
    private static String strRightPad(String s, int totalCount) {
        StringBuilder padded = new StringBuilder();
        padded.append(s);
        while (padded.length() < totalCount) {
            padded.append(" ");
        }
        return padded.toString();
    }

    private static String strMultiply(String s, int count) {
        StringBuilder repetition = new StringBuilder();
        for (int i = 0; i < count; i++) {
            repetition.append(s);
        }
        return repetition.toString();
	}

	public static List<Integer> setToList(Set<Integer> set) {
        return set.stream().sorted().collect(Collectors.toList());
    }

    public static String setToString(Set<Integer> set) {
        return setToString(setToList(set));
    }

    public static String setToString(List<Integer> set) {
        StringBuilder repr = new StringBuilder();
        repr.append("{");
        int addedElemCount = 0;
        for (int el : set) {
            if (addedElemCount == 0) {
                repr.append(" ");
            }
            else {
                repr.append(", ");
            }
            repr.append(el);
            addedElemCount++;
        }
        if (addedElemCount > 0) {
            repr.append(" ");
        }
        repr.append("}");
        return repr.toString();
    }

    public static boolean areEquivalent(FiniteStateAutomaton actual, 
            FiniteStateAutomaton expected) {
        // for equivalence 
        //  - alphabets must be equal
        //  - size of states must be equal
        //  - size of final states must be equal
        //  - there must be a clone of actual with states mapped to expected,
        //    which equals expected.

        if (!Objects.equals(actual.alphabet, expected.alphabet)) {
            return false;
        }
        
        List<Integer> actualFinalStateList = setToList(actual.finalStates);
        actualFinalStateList.remove((Object) actual.startState);
        List<Integer> actualNonFinalStateList = setToList(actual.states);
        actualNonFinalStateList.removeAll(actualFinalStateList);
        actualNonFinalStateList.remove((Object) actual.startState);

        List<Integer> expectedFinalStateList = setToList(expected.finalStates);
        expectedFinalStateList.remove((Object) expected.startState);
        List<Integer> expectedNonFinalStateList = setToList(expected.states);
        expectedNonFinalStateList.removeAll(expectedFinalStateList);
        expectedNonFinalStateList.remove((Object) expected.startState);

        if (actualFinalStateList.size() != expectedFinalStateList.size()) {
            return false;
        }
        if (actualNonFinalStateList.size() != expectedNonFinalStateList.size()) {
            return false;
        }

        int finalStSz = actualFinalStateList.size();
        int nonFinalSz = actualNonFinalStateList.size();
        
        // go through all pairs of permutations of final and non final states,
        // at least once.

        // now possible number of mappings of actual to expected is
        // = (N-F-1)! times F!
        // where F is number of final states excluding any initial state,
        // and N is the total number of states.
        // due to exponential running time, limit iterations
        final int ITER_LIMIT = 1_000_000;
        int iterCount = 1;

        int[] finalStPerm = MathAlgorithms.firstPermutation(finalStSz, 0);
        while (true) {
            int[] nonFinalStPerm = MathAlgorithms.firstPermutation(nonFinalSz, 0);
            while (true) {
                // create a mapping from actual to expected using permutations.
                Map<Integer, Integer> stateTranslationMap = new HashMap<>();
                stateTranslationMap.put(actual.startState, expected.startState);
                for (int j = 0; j < finalStPerm.length; j++) {
                    int actualSt = actualFinalStateList.get(j);
                    int mappedExpectedSt = expectedFinalStateList.get(finalStPerm[j]);
                    stateTranslationMap.put(actualSt, mappedExpectedSt);
                }
                for (int j = 0; j < nonFinalStPerm.length; j++) {
                    int actualSt = actualNonFinalStateList.get(j);
                    int mappedExpectedSt = expectedNonFinalStateList.get(nonFinalStPerm[j]);
                    stateTranslationMap.put(actualSt, mappedExpectedSt);
                }

                // create a copy of actual to resemble expected, and if actually
                // equal to expected, then actual is equivalent to expected.
                FiniteStateAutomaton actualCopy = actual.generateCopy(stateTranslationMap);
                if (actualCopy.equals(expected)) {
                    //System.out.println("Found match after " + iterCount + " attempt(s). " +
                    //    "State translation map: " + stateTranslationMap);
                    return true;
                }

                if (iterCount >= ITER_LIMIT || !MathAlgorithms.nextNPermutation(nonFinalStPerm)) {
                    break;
                }

                iterCount++;
            }
            if (iterCount >= ITER_LIMIT || !MathAlgorithms.nextNPermutation(finalStPerm)) {
                break;
            }
            iterCount++;
        }

        //System.out.println("Couldn't find match after " + iterCount + " attempt(s).");

        return false;
    }
}