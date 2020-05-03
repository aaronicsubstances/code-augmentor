package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
//import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.FsaEquivalenceMatcher;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;

public class RegexToNfaConvertorTest {

    @Test(dataProvider = "createTestVisitLiteralStringRegexNodeData")
    public void testVisitLiteralStringRegexNode(
            Set<Integer> alphabet, LiteralStringRegexNode node,
            TestArg<FiniteStateAutomaton> expected) {
        RegexNodeVisitor converter = new RegexToNfaConvertor(alphabet);
        Object actual = converter.visit(node);
        assertThat(actual, isA(FiniteStateAutomaton.class));
        assertThat((FiniteStateAutomaton) actual, new FsaEquivalenceMatcher(expected.value));
    }

    @DataProvider
    public Object[][] createTestVisitLiteralStringRegexNodeData() {
        final int nullSymbol = FiniteStateAutomaton.NULL_SYMBOL;
        Set<Integer> alphabet = Sets.newHashSet(41, 42, 43);
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3);
        int startState = 0;
        Set<Integer> finalStates = Sets.newHashSet(3);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(1))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(42, Sets.newHashSet(2))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(43, Sets.newHashSet(3)))))
            ));
        FiniteStateAutomaton nfa1 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
            
        states = Sets.newHashSet(0, 1);
        finalStates = Sets.newHashSet(1);
        nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(1)))))
            ));
        FiniteStateAutomaton nfa2 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
            
        states = Sets.newHashSet(0, 1);
        finalStates = Sets.newHashSet(1);
        nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1)))))
            ));
        FiniteStateAutomaton nfa3 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
            
        return new Object[][]{
            { alphabet, new LiteralStringRegexNode(new int[]{ 41, 42, 43 }),
                new TestArg<>(nfa1) },
            { alphabet, new LiteralStringRegexNode(41),
                new TestArg<>(nfa2) },
            { alphabet, new LiteralStringRegexNode(new int[]{}),
                new TestArg<>(nfa3) }
        };
    }

    @Test(dataProvider = "createTestVisitKleeneClosureRegexNodeData")
    public void testVisitKleeneClosureRegexNode(
            Set<Integer> alphabet, KleeneClosureRegexNode node,
            TestArg<FiniteStateAutomaton> expected) {
        RegexNodeVisitor converter = new RegexToNfaConvertor(alphabet);
        Object actual = converter.visit(node);
        assertThat(actual, isA(FiniteStateAutomaton.class));
        assertThat((FiniteStateAutomaton) actual, new FsaEquivalenceMatcher(expected.value));
    }

    @DataProvider
    public Object[][] createTestVisitKleeneClosureRegexNodeData() {
        final int nullSymbol = FiniteStateAutomaton.NULL_SYMBOL;
        Set<Integer> alphabet = Sets.newHashSet(41, 42, 43);
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3, 4, 5);
        int startState = 0;
        Set<Integer> finalStates = Sets.newHashSet(5);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 5))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(2))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(42, Sets.newHashSet(3))))),
                newMapEntry(3, newMap(Arrays.asList(newMapEntry(43, Sets.newHashSet(4))))),
                newMapEntry(4, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 5)))))
            ));
        FiniteStateAutomaton nfa1 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);

        Set<Integer> alphabet2 = Sets.newHashSet(41);
        states = Sets.newHashSet(0, 1, 2, 3);
        startState = 0;
        finalStates = Sets.newHashSet(3);
        nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 3))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(2))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 3)))))
            ));
        FiniteStateAutomaton nfa2 = new FiniteStateAutomaton(alphabet2, 
            states, startState, finalStates, nfaTransitionTable, null);

        return new Object[][]{
            { alphabet, new KleeneClosureRegexNode(
                new LiteralStringRegexNode(new int[]{ 41, 42, 43 })),
                new TestArg<>(nfa1) },
            { alphabet2, new KleeneClosureRegexNode(
                new LiteralStringRegexNode(41)),
                new TestArg<>(nfa2) },
        };
    }

    @Test(dataProvider = "createTestVisitUnionRegexNodeData")
    public void testVisitUnionRegexNode(
            Set<Integer> alphabet, UnionRegexNode node,
            TestArg<FiniteStateAutomaton> expected) {
        RegexNodeVisitor converter = new RegexToNfaConvertor(alphabet);
        Object actual = converter.visit(node);
        assertThat(actual, isA(FiniteStateAutomaton.class));
        assertThat((FiniteStateAutomaton) actual, new FsaEquivalenceMatcher(expected.value));
    }

    @DataProvider
    public Object[][] createTestVisitUnionRegexNodeData() {
        final int nullSymbol = FiniteStateAutomaton.NULL_SYMBOL;
        Set<Integer> alphabet = Sets.newHashSet(97, 98);
        Set<Integer> states = Sets.newHashSet(1, 2, 3, 4, 5, 6);
        int startState = 1;
        Set<Integer> finalStates = Sets.newHashSet(6);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(2, 4))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(97, Sets.newHashSet(3))))),
                newMapEntry(3, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(6))))),
                newMapEntry(4, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(5))))),
                newMapEntry(5, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(6)))))
            ));
        FiniteStateAutomaton nfa1 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);

        Set<Integer> alphabet2 = Sets.newHashSet(97, 98, 99, 100);
        states = Sets.newHashSet(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        startState = 0;
        finalStates = Sets.newHashSet(4);
        nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(1, 5, 8))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(100, Sets.newHashSet(2))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(97, Sets.newHashSet(3))))),
                newMapEntry(3, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(10))))),
                newMapEntry(10, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(4))))),
                newMapEntry(5, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(6))))),
                newMapEntry(6, newMap(Arrays.asList(newMapEntry(98, Sets.newHashSet(7))))),
                newMapEntry(7, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(4))))),
                newMapEntry(8, newMap(Arrays.asList(newMapEntry(99, Sets.newHashSet(9))))),
                newMapEntry(9, newMap(Arrays.asList(newMapEntry(nullSymbol, Sets.newHashSet(4)))))
            ));
        FiniteStateAutomaton nfa2 = new FiniteStateAutomaton(alphabet2, 
            states, startState, finalStates, nfaTransitionTable, null);

        return new Object[][]{
            { alphabet, new UnionRegexNode(new LiteralStringRegexNode(97),
                new LiteralStringRegexNode(98)),
                new TestArg<>(nfa1) },
            { alphabet2, new UnionRegexNode(new LiteralStringRegexNode(99),
                new LiteralStringRegexNode(new int[]{ 100, 97, 98 }),
                new LiteralStringRegexNode(new int[]{ 98, 98 })),
                new TestArg<>(nfa2) }
        };
    }

    @Test(dataProvider = "createTestVisitConcatRegexNodeData")
    public void testVisitConcatRegexNode(
            Set<Integer> alphabet, ConcatRegexNode node,
            TestArg<FiniteStateAutomaton> expected) {
        RegexNodeVisitor converter = new RegexToNfaConvertor(alphabet);
        Object actual = converter.visit(node);
        assertThat(actual, isA(FiniteStateAutomaton.class));
        assertThat((FiniteStateAutomaton) actual, new FsaEquivalenceMatcher(expected.value));
    }

    @DataProvider
    public Object[][] createTestVisitConcatRegexNodeData() {
        Set<Integer> alphabet = Sets.newHashSet(41, 42, 43);
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3);
        int startState = 0;
        Set<Integer> finalStates = Sets.newHashSet(3);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(1))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(42, Sets.newHashSet(2))))),
                newMapEntry(2, newMap(Arrays.asList(newMapEntry(43, Sets.newHashSet(3)))))
            ));
        FiniteStateAutomaton nfa1 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
            
        states = Sets.newHashSet(0, 1, 2);
        finalStates = Sets.newHashSet(2);
        nfaTransitionTable =
            newMap(Arrays.asList(
                newMapEntry(0, newMap(Arrays.asList(newMapEntry(41, Sets.newHashSet(1))))),
                newMapEntry(1, newMap(Arrays.asList(newMapEntry(42, Sets.newHashSet(2)))))
            ));
        FiniteStateAutomaton nfa2 = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);
            
        return new Object[][]{
            { alphabet, new ConcatRegexNode(new LiteralStringRegexNode(41),
                new LiteralStringRegexNode(42), new LiteralStringRegexNode(43)),
                new TestArg<>(nfa1) },
            { alphabet, new ConcatRegexNode(new LiteralStringRegexNode(41),
                new LiteralStringRegexNode(42)),
                new TestArg<>(nfa2) },
        };
    }

    @Test
    public void testTextBookConversionExample() {
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
        FiniteStateAutomaton expected = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);

        RegexNode node = new ConcatRegexNode(
            new KleeneClosureRegexNode(new UnionRegexNode(new LiteralStringRegexNode(97),
                new LiteralStringRegexNode(98))),
            new LiteralStringRegexNode(new int[]{97, 98, 98})
        );
        RegexNodeVisitor converter = new RegexToNfaConvertor(alphabet);
        Object actual = node.accept(converter);
        assertThat(actual, isA(FiniteStateAutomaton.class));
        assertThat((FiniteStateAutomaton) actual, new FsaEquivalenceMatcher(expected));
    }
}