package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.ConcatRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.DfaSimulator;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.LiteralStringRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.NfaSimulator;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.NfaToDfaConvertor;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexToNfaConvertor;

public class RegexAlgorithms {

    /**
     * Matches a string against a regular expression using 
     * {@link #simulateNfa(FiniteStateAutomaton, int[]))}.
     * 
     * @param regexNodeSpecs list of literal strings or RegexNode instances which will be 
     * concatenated for matching.
     * @param inputSpec literal string to match
     * @return -1 if match was made; nonnegative integer for position of mismatch.
     */
    public static int match(List<Object> regexNodeSpecs, Object inputSpec) {
        RegexNode regexTree = createRegexTree(regexNodeSpecs);

        // Since regular expression and NFA simulation do not use alphabet of finite
        // state automatons, ignore alphabet determination.
        FiniteStateAutomaton nfa = convertRegexTreeToNfa(regexTree, null);
            
        int[] input = getLiteralString(inputSpec);

        return simulateNfa(nfa, input);
    }

    public static FiniteStateAutomaton convertRegexTreeToNfa(RegexNode regexTree,
            Set<Integer> alphabet) {
        FiniteStateAutomaton nfa = (FiniteStateAutomaton) regexTree.accept(
            new RegexToNfaConvertor(alphabet));
        return nfa;
    }

    public static FiniteStateAutomaton convertRegexTreeToDfa(List<Object> regexNodeSpecs, 
            Set<Integer> alphabet) {
		RegexNode regexAst = createRegexTree(regexNodeSpecs);
        FiniteStateAutomaton nfa = convertRegexTreeToNfa(regexAst, alphabet);
        FiniteStateAutomaton dfa = nfaToDfa(nfa);
        return dfa;
	}

    public static RegexNode createRegexTree(List<Object> regexNodeSpecs) {
        List<RegexNode> childRegexNodes = new ArrayList<>();
        for (Object regexNodeSpec : regexNodeSpecs) {
            if (regexNodeSpec instanceof RegexNode) {
                childRegexNodes.add((RegexNode) regexNodeSpec);
            }
            else {
                int[] literalString = getLiteralString(regexNodeSpec);
                childRegexNodes.add(new LiteralStringRegexNode(literalString));
            }
        }
        RegexNode regexNode = new ConcatRegexNode(childRegexNodes);
        return regexNode;
    }

    public static int[] getLiteralString(Object literalStringSpec) {
        if (literalStringSpec instanceof int[]) {
            return (int[]) literalStringSpec;
        }
        else {
            return ((String) literalStringSpec).codePoints().toArray();
        }
    }

    /**
     * Simulates NFA on an string of input symbols in order to match it.
     * 
     * @param nfa NFA
     * @param input string of valid symbols from alphabet of NFA
     * @return -1 if NFA matches whole of string; else a nonnegative integer
     * which indicates the position in (or at end of) input where NFA
     * failed to make progress.
     */
    public static int simulateNfa(FiniteStateAutomaton nfa, int[] input) {
        NfaSimulator nfaSimulator = new NfaSimulator(nfa);
        return nfaSimulator.simulate(input, null);
    }

    /**
     * Simulates DFA on an string of input symbols in order to match it.
     * 
     * @param dfa DFA
     * @param input string of valid symbols from alphabet of DFA
     * @return -1 if DFA matches whole of string; else a nonnegative integer
     * which indicates the position in (or at end of) input where DFA
     * failed to make progress.
     */
    public static int simulateDfa(FiniteStateAutomaton dfa, int[] input) {
        DfaSimulator dfaSimulator = new DfaSimulator(dfa);
        return dfaSimulator.simulate(input, null);
    }

    /**
     * Converts an NFA to a DFA.
     * 
     * @param nfa
     * @return DFA
     */
    public static FiniteStateAutomaton nfaToDfa(FiniteStateAutomaton nfa) {
        NfaToDfaConvertor convertor = new NfaToDfaConvertor(nfa);
        return convertor.convert(true);
    }
}