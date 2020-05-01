package com.aaronicsubstances.code.augmentor.core.fsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class NfaFactory {
    private final Set<String> alphabet;
    private int stateCounter = 1;

    public NfaFactory(Set<String> alphabet) {
        this.alphabet = alphabet;
    }

    public Nfa convertRegularExpression(RegexNode r) {
        switch (r.getType()) {
            case RegexNode.TYPE_SYMBOL:
                return convertSymbol(r);
            case RegexNode.TYPE_KLEENE_CLOSURE:
                return convertKleeneClosure(r);
            case RegexNode.TYPE_CONCATENATION:
                return convertConcatenation(r);
            case RegexNode.TYPE_UNION:
                return convertUnion(r);
            default:
                throw new RuntimeException("Unknown regex node type: " + r.getType());
        }
    }

    private Set<Integer> newModifableSet(Integer... initialEntries) {
        return new HashSet<>(Arrays.asList(initialEntries));
    }

    private int singleValue(Set<Integer> s) {
        assert s.size() == 1;
        return s.iterator().next();
    }

    private Nfa convertSymbol(RegexNode r) {
        int startState = stateCounter++;
        int finalState = stateCounter++;
        Set<Integer> states = newModifableSet(startState, finalState);
        Map<TransitionTableKey, Set<Integer>> transitionTable = new HashMap<>();
        transitionTable.put(new TransitionTableKey(startState, r.getSymbol()),
            newModifableSet(finalState));
        return new Nfa(alphabet, states, transitionTable, startState,
            newModifableSet(finalState));
    }

    private Nfa convertUnion(RegexNode r) {
        Nfa leftChildNfa = convertRegularExpression(r.getLeftChild());
        Nfa rightChildNfa = convertRegularExpression(r.getRightChild());

        // combine child nfa states with these 2 new ones.
        int startState = stateCounter++;
        int finalState = stateCounter++;
        Set<Integer> states = newModifableSet(startState, finalState);
        states.addAll(leftChildNfa.states);
        states.addAll(rightChildNfa.states);

        // combine transition tables and make 3 new additions.
        Map<TransitionTableKey, Set<Integer>> transitionTable = new HashMap<>();
        transitionTable.putAll(leftChildNfa.transitionTable);
        transitionTable.putAll(rightChildNfa.transitionTable);

        // make new null transitions from new inital state to initial states of children
        transitionTable.put(new TransitionTableKey(startState, null),
            newModifableSet(leftChildNfa.startState, rightChildNfa.startState));
        // make a null transition each from final states of children to new final state.
        transitionTable.put(new TransitionTableKey(singleValue(leftChildNfa.finalStates),
            null), newModifableSet(finalState));
        transitionTable.put(new TransitionTableKey(singleValue(rightChildNfa.finalStates),
            null), newModifableSet(finalState));

        return new Nfa(alphabet, states, transitionTable, startState,
            newModifableSet(finalState));
    }

    private Nfa convertKleeneClosure(RegexNode r) {
        Nfa childNfa = convertRegularExpression(r.getLeftChild());

        // combine child nfa states with these 2 new ones.
        int startState = stateCounter++;
        int finalState = stateCounter++;
        Set<Integer> states = newModifableSet(startState, finalState);
        states.addAll(childNfa.states);

        // combine transition tables and make 3 new additions.
        Map<TransitionTableKey, Set<Integer>> transitionTable = new HashMap<>();
        transitionTable.putAll(childNfa.transitionTable);

        // make new null transitions from new inital state to initial states of
        // child and new final state.
        transitionTable.put(new TransitionTableKey(startState, null),
            newModifableSet(childNfa.startState, finalState));
        // make a null transition each from final state of child to new final state
        // and initial state of child.
        transitionTable.put(new TransitionTableKey(singleValue(childNfa.finalStates),
            null), newModifableSet(childNfa.startState));
        transitionTable.put(new TransitionTableKey(singleValue(childNfa.finalStates),
            null), newModifableSet(finalState));

        return new Nfa(alphabet, states, transitionTable, startState,
            newModifableSet(finalState));
    }

    private Nfa convertConcatenation(RegexNode r) {
        Nfa leftChildNfa = convertRegularExpression(r.getLeftChild());
        Nfa rightChildNfa = convertRegularExpression(r.getRightChild());

        // use final state of left child to replace initial state of right child.
        Set<Integer> states = new HashSet<>();
        int mergedState = singleValue(leftChildNfa.finalStates);
        states.addAll(leftChildNfa.states);
        for (int state : rightChildNfa.states) {
            if (state != rightChildNfa.startState) {
                states.add(state);
            }
        }

        // remap transitions of initial state of right child
        Map<TransitionTableKey, Set<Integer>> transitionTable = new HashMap<>();
        transitionTable.putAll(leftChildNfa.transitionTable);
        for (Map.Entry<TransitionTableKey, Set<Integer>> entry : 
                rightChildNfa.transitionTable.entrySet()) {
            int fromState = entry.getKey().state, toState = singleValue(entry.getValue());
            if (fromState == rightChildNfa.startState) {
                fromState = mergedState;
            }
            if (toState == rightChildNfa.startState) {
                toState = mergedState;
            }
            transitionTable.put(new TransitionTableKey(fromState, 
                entry.getKey().symbol), newModifableSet(toState));
        }

        return new Nfa(alphabet, states, transitionTable, leftChildNfa.startState, 
            rightChildNfa.finalStates);
    }
}