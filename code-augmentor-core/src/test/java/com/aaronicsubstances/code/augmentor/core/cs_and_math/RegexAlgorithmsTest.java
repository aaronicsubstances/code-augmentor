package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.KleeneClosureRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.LiteralStringRegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.RegexNode;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.regex.UnionRegexNode;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

public class RegexAlgorithmsTest {

    @Test(dataProvider = "createTestSimulateNfaData")
    public void testSimulateNfa(TestArg<FiniteStateAutomaton> nfa,
            int[] stringInput, int expected) {
        int actual = RegexAlgorithms.simulateNfa(nfa.value, stringInput);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestSimulateNfaData() {
        // NFA for (a|b)*abb
        final int nullSymbol = FiniteStateAutomaton.NULL_SYMBOL;
        Set<Integer> alphabet = Sets.newHashSet(97, 98);
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        int startState = 0;
        Set<Integer> finalStates = Sets.newHashSet(10);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 7))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(2, 4))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(97, Sets.newHashSet(3))))),
                newMapEntry(3, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(6))))),
                newMapEntry(4, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(5))))),
                newMapEntry(5, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(6))))),
                newMapEntry(6, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 7))))),
                newMapEntry(7, newMap(Arrays.asList(newMapEntry(97, Sets.newHashSet(8))))),
                newMapEntry(8, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(9))))),
                newMapEntry(9, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(10)))))
            ));
        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
        TestArg<FiniteStateAutomaton> nfaWrapper = new TestArg<>(nfa);
        
        // Generate test inputs to match or not match (a|b)*abb
        final int a = 97, b = 98;
        return new Object[][] {
            { nfaWrapper, new int[]{}, 0 },
            { nfaWrapper, new int[]{ a, b, b }, -1 },
            { nfaWrapper, new int[]{ a, b, b, a, a, a, a, b, b }, -1 },
            { nfaWrapper, new int[]{ a, b }, 2 },
            { nfaWrapper, new int[]{ a, b, a, b }, 4 },
            { nfaWrapper, new int[]{ a, 3, a, b }, 1 }
        };
    }

    @Test(dataProvider = "createTestMatchData")
    public void testMatch(List<Object> regex,
            String stringInput, int expected) {
        int actual = RegexAlgorithms.match(regex, stringInput);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestMatchData() {
        RegexNode zeroOrMoreWs = new KleeneClosureRegexNode(new UnionRegexNode(
            new LiteralStringRegexNode((int)' '), 
            new LiteralStringRegexNode((int)'\t')));
        return new Object[][]{
            { Arrays.asList(""), "", -1 },
            { Arrays.asList("shoe"), "shoe", -1 },
            { Arrays.asList("shoe"), "soe", 1 },
            { Arrays.asList(zeroOrMoreWs), "", -1 },
            { Arrays.asList(zeroOrMoreWs, "socks"), "socks", -1 },
            { Arrays.asList(zeroOrMoreWs), " ", -1 },
            { Arrays.asList(zeroOrMoreWs), "\t", -1 },
            { Arrays.asList(zeroOrMoreWs), "\t \t\t \t\t\t  ", -1 },
            { Arrays.asList(zeroOrMoreWs), "   d ", 3 }
        };
    }

    @Test(dataProvider = "createTestSimulateDfaData")
    public void testSimulateDfa(TestArg<FiniteStateAutomaton> dfa,
            int[] stringInput, boolean expected) {
        int actual = RegexAlgorithms.simulateDfa(dfa.value, stringInput);
        assertEquals(actual, expected ? -1 : stringInput.length);
    }

    @DataProvider
    public Object[][] createTestSimulateDfaData() {
        // The following DFAs are from Kenneth Rosen's Discrete Mathematics, 7e,
        // Section 13.3 Example 6

        // First example: the set of bit strings that begin with two 0s.
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3);
        Set<Integer> finalStates = Sets.newHashSet(2);
        Map<Integer, Map<Integer, Integer>> dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(0, 1), newMapEntry(1, 3)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 3)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 2)))),
            newMapEntry(3, newMap(Arrays.asList(newMapEntry(0, 3), newMapEntry(1, 3))))
        ));
        FiniteStateAutomaton dfa1 = new FiniteStateAutomaton(Sets.newHashSet(0, 1), 
            states, 0, finalStates, null, dfaTransitionTable);
        TestArg<FiniteStateAutomaton> dfa1Wrapper = new TestArg<>(dfa1);
        
        // Second example: the set of bit strings that contain two consecutive 0s
        states = Sets.newHashSet(0, 1, 2);
        finalStates = Sets.newHashSet(2);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(0, 1), newMapEntry(1, 0)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 0)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 2))))
        ));
        FiniteStateAutomaton dfa2 = new FiniteStateAutomaton(Sets.newHashSet(0, 1), 
            states, 0, finalStates, null, dfaTransitionTable);
        TestArg<FiniteStateAutomaton> dfa2Wrapper = new TestArg<>(dfa2);
        
        // Third example: the set of bit strings that do not contain two consecutive 0s
        states = Sets.newHashSet(0, 1, 2);
        finalStates = Sets.newHashSet(0, 1);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(0, 1), newMapEntry(1, 0)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 0)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 2))))
        ));
        FiniteStateAutomaton dfa3 = new FiniteStateAutomaton(Sets.newHashSet(0, 1), 
            states, 0, finalStates, null, dfaTransitionTable);
        TestArg<FiniteStateAutomaton> dfa3Wrapper = new TestArg<>(dfa3);
        
        // Fourth example: the set of bit strings that end with two 0s
        states = Sets.newHashSet(0, 1, 2);
        finalStates = Sets.newHashSet(2);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(0, 1), newMapEntry(1, 0)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 0)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 0))))
        ));
        FiniteStateAutomaton dfa4 = new FiniteStateAutomaton(Sets.newHashSet(0, 1), 
            states, 0, finalStates, null, dfaTransitionTable);
        TestArg<FiniteStateAutomaton> dfa4Wrapper = new TestArg<>(dfa4);
        
        // Fifth example: the set of bit strings that contain at least two 0s
        states = Sets.newHashSet(0, 1, 2);
        finalStates = Sets.newHashSet(2);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(0, 1), newMapEntry(1, 0)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 1)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(0, 2), newMapEntry(1, 2))))
        ));
        FiniteStateAutomaton dfa5 = new FiniteStateAutomaton(Sets.newHashSet(0, 1), 
            states, 0, finalStates, null, dfaTransitionTable);
        TestArg<FiniteStateAutomaton> dfa5Wrapper = new TestArg<>(dfa5);

        return new Object[][] {
            // First example
            { dfa1Wrapper, new int[]{ 0, 0 }, true },
            { dfa1Wrapper, new int[]{ 0, 1 }, false },
            { dfa1Wrapper, new int[]{ 0, 0, 0, 1 }, true },
            { dfa1Wrapper, new int[]{ 1, 0, 0, 1 }, false },

            // Second example
            { dfa2Wrapper, new int[]{ 0, 0 }, true },
            { dfa2Wrapper, new int[]{ 0, 1 }, false },
            { dfa2Wrapper, new int[]{ 0, 0, 0, 1 }, true },
            { dfa2Wrapper, new int[]{ 1, 0, 0, 1 }, true },

            // Third example
            { dfa3Wrapper, new int[]{ 0, 0 }, false },
            { dfa3Wrapper, new int[]{ 0, 1 }, true },
            { dfa3Wrapper, new int[]{ 0, 0, 0, 1 }, false },
            { dfa3Wrapper, new int[]{ 1, 0, 0, 1 }, false },

            // Fourth example
            { dfa4Wrapper, new int[]{ 0, 0 }, true },
            { dfa4Wrapper, new int[]{ 0, 1 }, false },
            { dfa4Wrapper, new int[]{ 0, 0, 0, 1 }, false },
            { dfa4Wrapper, new int[]{ 1, 0, 1, 0, 0 }, true },

            // Fifth example
            { dfa5Wrapper, new int[]{ 0, 0 }, true },
            { dfa5Wrapper, new int[]{ 0, 1 }, false },
            { dfa5Wrapper, new int[]{ 0, 0, 0, 1 }, true },
            { dfa5Wrapper, new int[]{ 1, 0, 1, 0, 0 }, true },
            { dfa5Wrapper, new int[]{ 1, 0, 1, 1, 0 }, true },
            { dfa5Wrapper, new int[]{ 1, 0, 1, 1, 1 }, false }
        };
    }
}