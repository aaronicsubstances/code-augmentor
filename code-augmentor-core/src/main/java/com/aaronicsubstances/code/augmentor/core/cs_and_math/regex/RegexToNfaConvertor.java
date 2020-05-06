package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;

/**
 * Converts a regular expression syntax tree into an NFA which can be simulated
 * to match strings.
 * <p>
 * Implements Algorithm 3.23 of Compilers - Principles, Techniques and Tools
 * (aka Dragon Book), 2nd edition.
 * Also known as McNaughton-Yamada- Thompson algorithm to convert
 * a regular expression to an NFA.
 */
public class RegexToNfaConvertor implements RegexNodeVisitor {
    private final Set<Integer> alphabet;
    private int stateGenerator = 1;

    private boolean trackStateNameChanges;
    private final Map<Integer, Integer> oldStateNameMap;
    private final Map<Integer, Integer> newStateNameMap;

    public RegexToNfaConvertor(Set<Integer> alphabet) {
        this.alphabet = alphabet;
        this.oldStateNameMap = new HashMap<Integer, Integer>();
        this.newStateNameMap = new HashMap<Integer, Integer>();
    }

    public boolean isTrackStateNameChanges() {
        return trackStateNameChanges;
    }

    public void setTrackStateNameChanges(boolean trackStateNameChanges) {
        this.trackStateNameChanges = trackStateNameChanges;
    }

    public Map<Integer, Integer> getOldStateNameMap() {
        return oldStateNameMap;
    }

    public Map<Integer, Integer> getNewStateNameMap() {
        return newStateNameMap;
    }

    public void resetStateGenerator(int newStart) {
        this.stateGenerator = newStart;
    }
    
    @Override
    public Object visit(LiteralStringRegexNode node) {
        Set<Integer> states = FiniteStateAutomaton.newSet();
        int startState = stateGenerator++;
        states.add(startState);
        Set<Integer> finalStates = FiniteStateAutomaton.newSet();
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = new HashMap<>();

        // for each symbol of string literal create a non initial state
        // for it and make a transition to it on the symbol from the previous state.
        Map<Integer, Set<Integer>> stateOutTransitions = new HashMap<>();
        nfaTransitionTable.put(startState, stateOutTransitions);
        
        int nextState = stateGenerator++;
        states.add(nextState);

        int[] literalString = node.getLiteralString();

        // cater for empty string using null symbol.
        int symbol = FiniteStateAutomaton.NULL_SYMBOL;
        if (literalString.length > 0) {
            symbol = literalString[0];
        }
        stateOutTransitions.put(symbol, FiniteStateAutomaton.newSet(nextState));

        // for non empty strings, use remaining symbols to make further
        // table entries.
        for (int i = 1; i < literalString.length; i++) {
            stateOutTransitions = new HashMap<>();
            nfaTransitionTable.put(nextState, stateOutTransitions);

            int newState = stateGenerator++;
            states.add(newState);

            symbol = literalString[i];
            stateOutTransitions.put(symbol, FiniteStateAutomaton.newSet(newState));

            nextState = newState;
        }
        finalStates.add(nextState);
        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, 
            nfaTransitionTable, null);
        return nfa;
    }

    @Override
    public Object visit(ConcatRegexNode node) {
        // convert each child regex to nfa.
        List<FiniteStateAutomaton> childNfas = new ArrayList<>();
        for (RegexNode child : node.getChildren()) {
            FiniteStateAutomaton childNfa = (FiniteStateAutomaton) child.accept(this);
            childNfas.add(childNfa);
        }
        
        return makeConcatNfa(childNfas);
    }

    @Override
    public Object visit(UnionRegexNode node) {
        // convert each child regex to nfa.
        List<FiniteStateAutomaton> childNfas = new ArrayList<>();
        for (RegexNode child : node.getChildren()) {
            FiniteStateAutomaton childNfa = (FiniteStateAutomaton) child.accept(this);
            childNfas.add(childNfa);
        }
        
        return makeUnionNfa(childNfas);
    }

    @Override
    public Object visit(KleeneClosureRegexNode node) {
        FiniteStateAutomaton childNfa = (FiniteStateAutomaton) node.getChild().accept(this);

        return makeKleeneClosureNfa(childNfa);
    }


    public FiniteStateAutomaton makeKleeneClosureNfa(FiniteStateAutomaton childNfa) {
        // Reuse childNfa states and transition table.

        Set<Integer> states = childNfa.getStates();
        int startState = stateGenerator++;
        states.add(startState);
        int finalState = stateGenerator++;
        states.add(finalState);
        Set<Integer> finalStates = FiniteStateAutomaton.newSet(finalState);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = 
            childNfa.getNfaTransitionTable();

        // make empty strings transitions
        // - from new start state to child start state
        // - from new start state to new final state
        Map<Integer, Set<Integer>> stateOutTransitions = new HashMap<>();
        nfaTransitionTable.put(startState, stateOutTransitions);
        stateOutTransitions.put(FiniteStateAutomaton.NULL_SYMBOL, 
            FiniteStateAutomaton.newSet(childNfa.getStartState(), finalState));

        // also make empty string transitions
        // - from child final state to new final state
        // - from child final state to child start state
        for (int childFinalState : childNfa.getFinalStates()) {
            // leverage fact that final states of child NFAs have no outbound transitions
            // given how they are constructed.
            stateOutTransitions = new HashMap<>();
            nfaTransitionTable.put(childFinalState, stateOutTransitions);
            stateOutTransitions.put(FiniteStateAutomaton.NULL_SYMBOL, 
                FiniteStateAutomaton.newSet(childNfa.getStartState(), finalState));
        }

        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
        return nfa;
    }

    public FiniteStateAutomaton makeUnionNfa(List<FiniteStateAutomaton> childNfas) {
        if (childNfas.isEmpty()) {
            // make outcome equivalent to empty string.
            return (FiniteStateAutomaton) visit(new LiteralStringRegexNode(new int[0]));
        }
        if (childNfas.size() == 1) {
            // make outcome equivalent to sole child. 
            return childNfas.get(0);
        }

        // Reuse states and transition table of child nfa with 
        // most number of states.
        FiniteStateAutomaton largestChildNfa = childNfas.stream()
            .max((x, y) -> Integer.compare(x.getStates().size(), y.getStates().size()))
            .get();
        Set<Integer> states = largestChildNfa.getStates();
        int startState = stateGenerator++;
        states.add(startState);
        int finalState = stateGenerator++;
        states.add(finalState);
        Set<Integer> finalStates = FiniteStateAutomaton.newSet(finalState);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = 
            largestChildNfa.getNfaTransitionTable();

        // Copy over child nfa states and transition tables except for
        // and largest.
        // Make null string transitions from new start state to each child nfa's start
        // state, and make null string transitions from each child nfa's final state
        // to the new final state.
        Set<Integer> newStartNextStates = FiniteStateAutomaton.newSet();
        Map<Integer, Set<Integer>> stateOutTransitions = new HashMap<>();
        nfaTransitionTable.put(startState, stateOutTransitions);
        stateOutTransitions.put(FiniteStateAutomaton.NULL_SYMBOL, 
            newStartNextStates);
        for (FiniteStateAutomaton childNfa : childNfas) {
            if (childNfa != largestChildNfa) {
                states.addAll(childNfa.getStates());
                nfaTransitionTable.putAll(childNfa.getNfaTransitionTable());
            }

            // connect new start state to each child nfa's start state.
            newStartNextStates.add(childNfa.getStartState());

            // leverage fact that final states of child NFAs have no outbound transitions
            // given how they are constructed.
            for (int childFinalState : childNfa.getFinalStates()) {
                stateOutTransitions = new HashMap<>();
                nfaTransitionTable.put(childFinalState, stateOutTransitions);
                stateOutTransitions.put(FiniteStateAutomaton.NULL_SYMBOL, 
                    FiniteStateAutomaton.newSet(finalState));
            }
        }

        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
        return nfa;
    }

	public FiniteStateAutomaton makeConcatNfa(List<FiniteStateAutomaton> childNfas) {
		if (childNfas.isEmpty()) {
            // make outcome equivalent to empty string.
            return (FiniteStateAutomaton)visit(new LiteralStringRegexNode(new int[0]));
        }
        if (childNfas.size() == 1) {
            // make outcome equivalent to sole child. 
            return childNfas.get(0);
        }

        // Reuse states and transition table of child nfa with 
        // most number of states.
        FiniteStateAutomaton largestChildNfa = childNfas.stream()
            .max((x, y) -> Integer.compare(x.getStates().size(), y.getStates().size()))
            .get();
        Set<Integer> states = largestChildNfa.getStates();
        int startState = childNfas.get(0).getStartState();
        Set<Integer> finalStates = childNfas.get(childNfas.size() - 1).getFinalStates();
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = 
            largestChildNfa.getNfaTransitionTable();

        // Copy over child nfa states and transition tables except for
        // and largest.
        // for each child nfa other than the first, remove its start state
        // from the states, and re-insert the removed start state's out-
        // transitions under final state of the previous child.
        for (int i = 0; i < childNfas.size(); i++) {
            FiniteStateAutomaton childNfa = childNfas.get(i);
            if (childNfa != largestChildNfa) {
                states.addAll(childNfa.getStates());
                nfaTransitionTable.putAll(childNfa.getNfaTransitionTable());
            }

            if (i == 0) {
                continue;
            }

            states.remove(childNfa.getStartState());
            Map<Integer, Set<Integer>> newNextStates = nfaTransitionTable.remove(
                childNfa.getStartState());
            FiniteStateAutomaton previousChildNfa = childNfas.get(i - 1);
            for (int childFinalState : previousChildNfa.getFinalStates()) {
                nfaTransitionTable.put(childFinalState, newNextStates);
                if (trackStateNameChanges) {
                    newStateNameMap.put(childFinalState, childNfa.getStartState());
                    oldStateNameMap.put(childNfa.getStartState(), childFinalState);
                }
            }
        }

        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
        return nfa;
	}
}