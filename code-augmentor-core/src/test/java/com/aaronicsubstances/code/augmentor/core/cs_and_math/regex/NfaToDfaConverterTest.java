package com.aaronicsubstances.code.augmentor.core.cs_and_math.regex;

import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMap;
import static com.aaronicsubstances.code.augmentor.core.TestResourceLoader.newMapEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.aaronicsubstances.code.augmentor.core.cs_and_math.FiniteStateAutomaton;
import com.aaronicsubstances.code.augmentor.core.cs_and_math.FsaEquivalenceMatcher;

import org.testng.annotations.Test;
import org.testng.collections.Sets;

public class NfaToDfaConverterTest {

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
        FiniteStateAutomaton nfa = new FiniteStateAutomaton(alphabet, 
            states, startState, finalStates, nfaTransitionTable, null);

        states = Sets.newHashSet(0, 1, 2, 3, 4);
        startState = 0;
        finalStates = Sets.newHashSet(4);
        Map<Integer, Map<Integer, Integer>> dfaTransitionTable = newMap(Arrays.asList(
            newMapEntry(0, newMap(Arrays.asList(newMapEntry(97, 1), newMapEntry(98, 2)))),
            newMapEntry(1, newMap(Arrays.asList(newMapEntry(97, 1), newMapEntry(98, 3)))),
            newMapEntry(2, newMap(Arrays.asList(newMapEntry(97, 1), newMapEntry(98, 2)))),
            newMapEntry(3, newMap(Arrays.asList(newMapEntry(97, 1), newMapEntry(98, 4)))),
            newMapEntry(4, newMap(Arrays.asList(newMapEntry(97, 1), newMapEntry(98, 2))))
        ));
        FiniteStateAutomaton expected = new FiniteStateAutomaton(alphabet,
            states, startState, finalStates, null, dfaTransitionTable);

        List<Set<Integer>> expectedAggregatedNfaStates = Arrays.asList(
            Sets.newHashSet(0, 1, 2, 4, 7),
            Sets.newHashSet(1, 2, 3, 4, 6, 7, 8),
            Sets.newHashSet(1, 2, 4, 5, 6, 7),
            Sets.newHashSet(1, 2, 4, 5, 6, 7, 9),
            Sets.newHashSet(1, 2, 4, 5, 6, 7, 10)
        );
        
        NfaToDfaConvertor instance = new NfaToDfaConvertor(nfa);
        FiniteStateAutomaton actual = instance.convert(false);
        assertThat(actual, new FsaEquivalenceMatcher(expected));
        assertThat(instance.getNfaStateSubsets(), is(expectedAggregatedNfaStates));
    }
}