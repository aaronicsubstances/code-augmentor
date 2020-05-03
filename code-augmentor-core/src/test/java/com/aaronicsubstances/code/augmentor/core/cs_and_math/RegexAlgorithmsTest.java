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
            { Arrays.asList(zeroOrMoreWs), " ", -1 },
            { Arrays.asList(zeroOrMoreWs), "\t", -1 },
            { Arrays.asList(zeroOrMoreWs), "\t \t\t \t\t\t  ", -1 },
            { Arrays.asList(zeroOrMoreWs), "   d ", 3 }
        };
    }
}