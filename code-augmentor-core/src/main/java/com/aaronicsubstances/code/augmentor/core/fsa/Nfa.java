package com.aaronicsubstances.code.augmentor.core.fsa;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Nfa {
    public Set<String> alphabet;
    public Set<Integer> states;
    public Map<TransitionTableKey, Set<Integer>> transitionTable;
    public int startState;
    public Set<Integer> finalStates;

    public Nfa(Set<String> alphabet, Set<Integer> states, 
            Map<TransitionTableKey, Set<Integer>> transitionTable,
            int startState, Set<Integer> finalStates) {
        this.alphabet = alphabet;
        this.states = states;
        this.transitionTable = transitionTable;
        this.startState = startState;
        this.finalStates = finalStates;
    }

    Dfa constructDfa() {
        Set<NfaAggregateState> interimStates = new HashSet<>();
        Map<InterimTransitionTableKey, NfaAggregateState> interimTransitionTable = new HashMap<>();
        NfaAggregateState initialDfaState = runSubsetConstructionAlgorithm(interimStates, 
            interimTransitionTable);

        // remap aggregate states to simple integers. let initial state start from 0.
        Map<TransitionTableKey, Integer> dfaTransitionTable = new HashMap<>();
        Set<Integer> dfaFinalStates = new HashSet<Integer>();

        Map<NfaAggregateState, Integer> mappedStates = new HashMap<>();
        int stateCounter = 1;
        for (NfaAggregateState interimState : interimStates) {
            int assignedState;
            if (interimState.equals(initialDfaState)) {
                assignedState = 0;
            }
            else {
                assignedState = stateCounter++;
            }
            mappedStates.put(interimState, assignedState);
        }

        // the accepting states of D are all
        // those sets of N's states that include at least one accepting state of N
        for (NfaAggregateState interimState : interimStates) {
            if (interimState.states.stream().anyMatch(x -> finalStates.contains(x))) {
                dfaFinalStates.add(mappedStates.get(interimState));
            }
        }

        // finally transfer NFA transition table entries to DFA.
        for (Map.Entry<InterimTransitionTableKey, NfaAggregateState> entry : 
                interimTransitionTable.entrySet()) {
            int dfaFromState = mappedStates.get(entry.getKey().state); 
            int dfaToState = mappedStates.get(entry.getValue());
            dfaTransitionTable.put(new TransitionTableKey(dfaFromState, 
                entry.getKey().symbol), dfaToState);
        }

        return new Dfa(0, dfaFinalStates, dfaTransitionTable);
    }

    private NfaAggregateState runSubsetConstructionAlgorithm(Set<NfaAggregateState> dfaStates,
            Map<InterimTransitionTableKey, NfaAggregateState> dfaTransitionTable) {      
        Set<NfaAggregateState> unmarkedDfaStates = new HashSet<>();

        // The start state of D is E-closure( so)
        // initially, t- closure(so) is the only state in Dstates, and it is unmarked;
        NfaAggregateState initialDfaState = new NfaAggregateState(nullStringClosure(startState));
        dfaStates.add(initialDfaState);
        unmarkedDfaStates.add(initialDfaState);
        
        // while ( there is an unmarked state T in Dstates ) {
        while (true) {
            Optional<NfaAggregateState> unmarkedDfaStateSearch =
                unmarkedDfaStates.stream().findAny();
            if (!unmarkedDfaStateSearch.isPresent()) {
                break;
            }
            NfaAggregateState Tstate = unmarkedDfaStateSearch.get();
            
            // mark T
            unmarkedDfaStates.remove(Tstate);

            // for ( each input symbol a ) {
            for (String a : alphabet) {
                // U = t- closure( move(T, a)) ;
                NfaAggregateState Ustate = new NfaAggregateState(
                    nullStringClosure(move(Tstate.states, a)));
                // if ( U is not in Dstates )
                if (!dfaStates.contains(Ustate)) {
                    // add U as an unmarked state to Dstates;
                    unmarkedDfaStates.add(Ustate);
                }
                // Dtran[T, a] = U;
                dfaTransitionTable.put(new InterimTransitionTableKey(Tstate, a), Ustate);
            }
        }
        return initialDfaState;
    }

    private Set<Integer> nullStringClosure(int state) {
        return nullStringClosure(Arrays.asList(state));
    }

    private Set<Integer> nullStringClosure(Collection<Integer> Tstate) {
        // push all states of T onto stack;
        Stack<Integer> stack = new Stack<>();
        stack.addAll(Tstate);

        // initialize t- closure(T) to T
        Set<Integer> result = new HashSet<>(Tstate);
        while (!stack.isEmpty()) {
            // pop t, the top element, off stack;
            int t = stack.pop();

            //for ( each state u with an edge from t to u labeled epsilon )
            Stream<Integer> uStates = states.stream().filter(u -> transitionTable.entrySet().stream()
                .filter(e -> e.getKey().state == t &&
                             e.getValue().contains(u) &&
                             e.getKey().symbol == null).findAny().isPresent());
            uStates.forEach(u -> {
                // if ( u is not in t- closure(T) ) {
                if (!result.contains(u)) {
                    // add u to t- closure(T) ;
                    result.add(u);
                    // push u onto stack;
                    stack.push(u);
                }
            });
        }

        return result;
    }
    
    /*
     * Set of NFA states to which there is a transition on
     * input symbol a from some state s in T.
     * 
     */ 
    private Set<Integer> move(Collection<Integer> Tstate, String a) {
        Set<Integer> result = new HashSet<>();
        for (int s : Tstate) {
            TransitionTableKey key = new TransitionTableKey(s, a);
            Set<Integer> toStates = transitionTable.get(key);
            result.addAll(toStates);
        }
        return result;
    }

    static class InterimTransitionTableKey {
        public final NfaAggregateState state;
        public final String symbol;

        public InterimTransitionTableKey(NfaAggregateState state, String symbol) {
            this.state = state;
            this.symbol = symbol;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((state == null) ? 0 : state.hashCode());
            result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
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
            InterimTransitionTableKey other = (InterimTransitionTableKey) obj;
            if (state == null) {
                if (other.state != null)
                    return false;
            } else if (!state.equals(other.state))
                return false;
            if (symbol == null) {
                if (other.symbol != null)
                    return false;
            } else if (!symbol.equals(other.symbol))
                return false;
            return true;
        }
    } 
    
    static class NfaAggregateState {
        public final List<Integer> states;
    
        public NfaAggregateState(Collection<Integer> states) {
            this.states = states.stream().distinct().sorted().collect(Collectors.toList());
        }
    
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
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
            NfaAggregateState other = (NfaAggregateState) obj;
            if (states == null) {
                if (other.states != null)
                    return false;
            } else if (!states.equals(other.states))
                return false;
            return true;
        }
    }
}