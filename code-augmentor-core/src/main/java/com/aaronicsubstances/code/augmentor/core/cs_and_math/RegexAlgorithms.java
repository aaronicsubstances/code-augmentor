package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.ConcatRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.LiteralStringRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexToNfaConvertor;

public class RegexAlgorithms {

    /**
     * Matches a string against a regular expression using 
     * {@link #simulateNfa(FiniteStateAutomaton, int[])}.
     * 
     * @param regexNodes list of string or RegexNode instances which will be 
     * concatenated for matching.
     * @param inputString
     * @return -1 if match was made; nonnegative integer for position of mismatch.
     */
    public static int match(List<Object> regexNodes, String inputString) {
        // Since regular expression and NFA simulation do not use alphabet of finite
        // state automatons, ignore alphabet determination.
        // Also, map strings to int arrays using their 16-bit char elements.
        List<RegexNode> concatRegexChildren = new ArrayList<>();
        for (Object regexNode : regexNodes) {
            if (regexNode instanceof RegexNode) {
                concatRegexChildren.add((RegexNode) regexNode);
            }
            else {
                String s = (String) regexNode;
                int[] charArray = toChars(s);
                concatRegexChildren.add(new LiteralStringRegexNode(charArray));
            }
        }
        ConcatRegexNode concatRegex = new ConcatRegexNode(concatRegexChildren);
        FiniteStateAutomaton nfa = (FiniteStateAutomaton) concatRegex.accept(
            new RegexToNfaConvertor(new HashSet<>()));
            
        int[] inputChars = toChars(inputString);

        return simulateNfa(nfa, inputChars);
    }

    private static int[] toChars(String s) {
        int[] charArray = new int[s.length()];
        for (int i = 0; i < charArray.length; i++) {
            charArray[i] = s.charAt(i);
        }
        return charArray;
    }

    /**
     * Simulates NFA on an string of input symbols in order to match it.
     * <p>
     * Implements Algorithm 3.22 of Compilers - Principles, Techniques and Tools
     * (aka Dragon Book), 2nd edition.
     * 
     * @param nfa NFA
     * @param stringInput list of valid symbols from alphabet of NFA
     * @return -1 if NFA matches whole of string; else a nonnegative integer
     * which indicates the position in (or at end of) input where NFA
     * failed to make progress.
     */
    public static int simulateNfa(FiniteStateAutomaton nfa, int[] stringInput) {
        // initialize variables needed for efficient implementation 
        // of NFA simulation algorithm. 
        Stack<Integer> oldStateStack = new Stack<>();
        Stack<Integer> newStateStack = new Stack<>();
        int maxState = 0;
        if (!nfa.getStates().isEmpty()) {
            maxState = Collections.max(nfa.getStates());
        }
        boolean[] alreadyOn = new boolean[maxState + 1];

        // begin algorithm 
        initialEmptyStringClosure(nfa, oldStateStack, newStateStack, alreadyOn);
        int i;
        for (i = 0; i < stringInput.length; i++) {
            int c = stringInput[i];
            boolean canContinue = emptyStringClosure(nfa, oldStateStack, 
                newStateStack, alreadyOn, c);
            if (!canContinue) {
                break;
            }
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

    static Set<Integer> move(FiniteStateAutomaton nfa, int state, int c) {
        Map<Integer, Set<Integer>> stateOutTransitions = nfa.getNfaTransitionTable().get(state);
        if (stateOutTransitions == null) {
            return null;
        }
        return stateOutTransitions.get(c);
    }

    static void initialEmptyStringClosure(FiniteStateAutomaton nfa, Stack<Integer> oldStateStack,
            Stack<Integer> newStateStack, boolean[] alreadyOn) {
        addState(nfa, newStateStack, alreadyOn, nfa.getStartState());
        while (!newStateStack.isEmpty()) {
            int s = newStateStack.pop();
            oldStateStack.push(s);
            alreadyOn[s] = false;
        }
    }

    static boolean emptyStringClosure(FiniteStateAutomaton nfa, 
            Stack<Integer> oldStateStack, Stack<Integer> newStateStack,
            boolean[] alreadyOn, int c) {
        while (!oldStateStack.isEmpty()) {
            int s = oldStateStack.pop();
            Set<Integer> moveResult = move(nfa, s, c);
            if (moveResult != null) {
                for (int t : moveResult) {
                    if (!alreadyOn[t]) {
                        addState(nfa, newStateStack, alreadyOn, t);
                    }
                }
            }
        }
        boolean anyNewStateFound = false;
        while (!newStateStack.isEmpty()) {
            int s = newStateStack.pop();
            oldStateStack.push(s);
            alreadyOn[s] = false;
            anyNewStateFound = true;
        }
        return anyNewStateFound;
    }

    static void addState(FiniteStateAutomaton nfa, Stack<Integer> newStateStack,
            boolean[] alreadyOn, int s) {
        newStateStack.push(s);
        alreadyOn[s] = true;
        Set<Integer> moveResult = move(nfa, s, FiniteStateAutomaton.NULL_SYMBOL);
        if (moveResult != null) {
            for (int t : moveResult) {
                if (!alreadyOn[t]) {
                    addState(nfa, newStateStack, alreadyOn, t);
                }
            }
        }
    }
}