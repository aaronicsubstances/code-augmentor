package com.aaronicsubstances.code.augmentor.core.fsa;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dfa {
    private final int startState;
    private final Set<Integer> finalStates;
    private final Map<TransitionTableKey, Integer> transitionTable;

    Dfa(int startState, Set<Integer> finalStates, 
            Map<TransitionTableKey, Integer> transitionTable) {
        this.startState = startState;
        this.finalStates = finalStates;
        this.transitionTable = transitionTable;
    }

    public static Dfa makeDfa(Set<String> alphabet, RegexNode r) {
        NfaFactory nfaFactory = new NfaFactory(alphabet);
        Nfa nfa = nfaFactory.convertRegularExpression(r);
        Dfa dfa = nfa.constructDfa();
        return dfa;
    }

    public MatchResult match(List<String> input) {
        int s = startState;
        int lastMatchIndex = -1;
        for (int i = 0; i < input.size(); i++) {
            String c = input.get(i);
            s = move(s, c);
            if (finalStates.contains(s)) {
                lastMatchIndex = i;
            }
        }
        boolean matches = finalStates.contains(s);
        return new MatchResult(matches, matches ? -1 : lastMatchIndex + 1);
    }

    private int move(int state, String symbol) {
        TransitionTableKey key = new TransitionTableKey(state, symbol);
        return transitionTable.get(key);
    }
    
    public static class MatchResult {
        public final boolean success;
        public final int errorIndex;
    
        public MatchResult(boolean success, int errorIndex) {
            this.success = success;
            this.errorIndex = errorIndex;
        }
    }
}