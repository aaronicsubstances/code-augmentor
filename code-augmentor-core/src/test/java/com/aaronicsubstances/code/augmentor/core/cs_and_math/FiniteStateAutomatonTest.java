package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.TestArg;
import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;

//import static org.hamcrest.MatcherAssert.*;
//import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class FiniteStateAutomatonTest {

    @Test(dataProvider = "createTestGenerateCopyData")
    public void testGenerateCopy(TestArg<FiniteStateAutomaton> instance, 
            Map<Integer, Integer> stateMap,
            TestArg<FiniteStateAutomaton> expected) {
        FiniteStateAutomaton actual = instance.value.generateCopy(stateMap);
        //assertThat(actual, is(expected.value));
        assertEquals(actual, expected.value);
        // print degenerate cases and confirm print out.
        if (actual.getAlphabet().isEmpty() || actual.getStates().isEmpty()) {
            TestResourceLoader.printTestHeader("testGenerateCopy", instance, stateMap, expected);
            System.out.println(actual);
        }
    }

    @DataProvider
    public Object[][] createTestGenerateCopyData() {
        Set<Integer> alphabet = Sets.newHashSet(0, 1);
        Set<Integer> states = Sets.newHashSet(0, 1, 2, 3);
        int startState = 0;
        Set<Integer> finalStates = Sets.newHashSet(0, 3);
        Map<Integer, Map<Integer, Integer>> dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 1)))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 2)))),
            newMapEntry(2, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 0)))),
            newMapEntry(3, newMap(Arrays.asList(
                newMapEntry(0, 2), newMapEntry(1, 1))))
        ));
        FiniteStateAutomaton fsa1 = new FiniteStateAutomaton(alphabet, states, startState, 
            finalStates, null, dfaTransitionTable);
            
        states = Sets.newHashSet(10, 11, 12, 13);
        startState = 10;
        finalStates = Sets.newHashSet(10, 13);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(10, newMap(Arrays.asList(
                newMapEntry(0, 10), newMapEntry(1, 11)))),
            newMapEntry(11, newMap(Arrays.asList(
                newMapEntry(0, 10), newMapEntry(1, 12)))),
            newMapEntry(12, newMap(Arrays.asList(
                newMapEntry(0, 10), newMapEntry(1, 10)))),
            newMapEntry(13, newMap(Arrays.asList(
                newMapEntry(0, 12), newMapEntry(1, 11))))
        ));
        FiniteStateAutomaton fsa2 = new FiniteStateAutomaton(alphabet, states, startState, 
            finalStates, null, dfaTransitionTable);
            
        states = Sets.newHashSet(0, 1, 2, 3);
        startState = 0;
        finalStates = Sets.newHashSet(2, 3);
        Map<Integer, Map<Integer, Set<Integer>>> nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1)), newMapEntry(1, Sets.newHashSet(3))))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0)), newMapEntry(1, Sets.newHashSet(1, 3))))),
            newMapEntry(2, newMap(Arrays.asList(
                /*NULL,*/ newMapEntry(1, Sets.newHashSet(0, 2))))),
            newMapEntry(3, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1, 2)), newMapEntry(1, Sets.newHashSet(1)))))
        ));
        FiniteStateAutomaton fsa3 = new FiniteStateAutomaton(alphabet, states, startState, 
            finalStates, nfaTransitionTable, null);
            
        states = Sets.newHashSet(10, 11, 12, 13);
        startState = 10;
        finalStates = Sets.newHashSet(12, 13);
        nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(10, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(10, 11)), newMapEntry(1, Sets.newHashSet(13))))),
            newMapEntry(11, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(10)), newMapEntry(1, Sets.newHashSet(11, 13))))),
            newMapEntry(12, newMap(Arrays.asList(
                /*NULL,*/ newMapEntry(1, Sets.newHashSet(10, 12))))),
            newMapEntry(13, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(10, 11, 12)), newMapEntry(1, Sets.newHashSet(11)))))
        ));
        FiniteStateAutomaton fsa4 = new FiniteStateAutomaton(alphabet, states, startState, 
            finalStates, nfaTransitionTable, null);

        // use fsa5 to fsa10 to test degenerate cases of either empty alphabet or
        // empty states doesn't cause runtime exceptions.
        states = Sets.newHashSet(0, 1, 2, 3);
        startState = 0;
        finalStates = Sets.newHashSet(2, 3);
        nfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1)), newMapEntry(1, Sets.newHashSet(3))))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0)), newMapEntry(1, Sets.newHashSet(1, 3))))),
            newMapEntry(2, newMap(Arrays.asList(
                /*NULL,*/ newMapEntry(1, Sets.newHashSet(0, 2))))),
            newMapEntry(3, newMap(Arrays.asList(
                newMapEntry(0, Sets.newHashSet(0, 1, 2)), newMapEntry(1, Sets.newHashSet(1)))))
        ));
        FiniteStateAutomaton fsa5 = new FiniteStateAutomaton(Sets.newHashSet(), states, startState, 
            finalStates, nfaTransitionTable, null);

        FiniteStateAutomaton fsa6 = new FiniteStateAutomaton(alphabet, Sets.newHashSet(), startState, 
            finalStates, nfaTransitionTable, null);

        FiniteStateAutomaton fsa7 = new FiniteStateAutomaton(Sets.newHashSet(), Sets.newHashSet(), startState, 
            finalStates, nfaTransitionTable, null);
            
        states = Sets.newHashSet(0, 1, 2, 3);
        startState = 0;
        finalStates = Sets.newHashSet(0, 3);
        dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 1)))),
            newMapEntry(1, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 2)))),
            newMapEntry(2, newMap(Arrays.asList(
                newMapEntry(0, 0), newMapEntry(1, 0)))),
            newMapEntry(3, newMap(Arrays.asList(
                newMapEntry(0, 1), newMapEntry(1, 1))))
        ));

        FiniteStateAutomaton fsa8 = new FiniteStateAutomaton(Sets.newHashSet(), Sets.newHashSet(), startState, 
            finalStates, null, dfaTransitionTable);

        FiniteStateAutomaton fsa9 = new FiniteStateAutomaton(Sets.newHashSet(), states, startState, 
            finalStates, null, dfaTransitionTable);

        FiniteStateAutomaton fsa10 = new FiniteStateAutomaton(alphabet, Sets.newHashSet(), startState, 
            finalStates, null, dfaTransitionTable);

        Map<Integer, Integer> identityMap = newMap(Arrays.asList(
            newMapEntry(0, 0), newMapEntry(1, 1), newMapEntry(2, 2), newMapEntry(3, 3)
        ));

        Map<Integer, Integer> fsa1to2Map = newMap(Arrays.asList(
            newMapEntry(0, 10), newMapEntry(1, 11), newMapEntry(2, 12), newMapEntry(3, 13)
        ));
        return new Object[][] {
            { new TestArg<>(fsa1), identityMap, new TestArg<>(fsa1) },
            { new TestArg<>(fsa1), fsa1to2Map, new TestArg<>(fsa2) },
            { new TestArg<>(fsa3), identityMap, new TestArg<>(fsa3) },
            { new TestArg<>(fsa3), fsa1to2Map, new TestArg<>(fsa4) },
            { new TestArg<>(fsa5), identityMap, new TestArg<>(fsa5) },
            { new TestArg<>(fsa6), identityMap, new TestArg<>(fsa6) },
            { new TestArg<>(fsa7), identityMap, new TestArg<>(fsa7) },
            { new TestArg<>(fsa8), identityMap, new TestArg<>(fsa8) },
            { new TestArg<>(fsa9), identityMap, new TestArg<>(fsa9) },
            { new TestArg<>(fsa10), identityMap, new TestArg<>(fsa10) },
        };
    }
}