package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class MathAlgorithmsTest {

    @Test(dataProvider = "createTestNextPermutationData") 
    public void testNextPermutation(int n, int r, int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextPermutation(n, r, a);
        if (expected != null) {
            assertTrue(actual);
            assertThat(a, is(expected));
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextPermutationData() {
        return new Object[][]{
            { 0, 0, new int[]{}, null },
            { 1, 1, new int[]{ 0 }, null },
            { 2, 2, new int[]{ 0, 1 }, new int[]{ 1, 0 } },
            { 2, 2, new int[]{ 1, 0 }, null },
            { 6, 6, new int[]{ 2, 5, 1, 4, 3, 0 }, new int[]{ 2, 5, 3, 0, 1, 4 } },
            { 4, 4, new int[]{ 0, 3, 2, 1 }, new int[]{ 1, 0, 2, 3 } },
            { 5, 5, new int[]{ 4, 3, 0, 1, 2 }, new int[]{ 4, 3, 0, 2, 1 } },
            { 5, 5, new int[]{ 0, 1, 3, 4, 2 }, new int[]{ 0, 1, 4, 2, 3 } },
            { 5, 5, new int[]{ 3, 4, 1, 2, 0 }, new int[]{ 3, 4, 2, 0, 1 } },
            { 7, 7, new int[]{ 5, 6, 0, 3, 1, 2, 4 }, new int[]{ 5, 6, 0, 3, 1, 4, 2 } },
            { 8, 8, new int[]{ 2, 0, 4, 1, 7, 6, 5, 3 }, new int[]{ 2, 0, 4, 3, 1, 5, 6, 7 } }
        };
    }

    @Test
    public void testNextRPermutation() {
        int n = 5, r = 3;
        int[] a = MathAlgorithms.firstPermutation(r);
        List<String> permutations = new ArrayList<>();
        permutations.add(stringifyPermutation(a));
        while (MathAlgorithms.nextPermutation(n, r, a)) {
            permutations.add(stringifyPermutation(a));
        }
        List<String> expected = Arrays.asList(
            "012", "021", "102", "120", "201", "210", 
            "013", "031", "103", "130", "301", "310",
            "014", "041", "104", "140", "401", "410", 
            "023", "032", "203", "230", "302", "320",
            "024", "042", "204", "240", "402", "420", 
            "034", "043", "304", "340", "403", "430", 
            "123", "132", "213", "231", "312", "321", 
            "124", "142", "214", "241", "412", "421", 
            "134", "143", "314", "341", "413", "431",
            "234", "243", "324", "342", "423", "432");
        assertThat(permutations, is(expected));
    }

    private static String stringifyPermutation(int[] a) {
        StringBuilder s = new StringBuilder();
        for (int d : a) {
            s.append(d);
        }
        return s.toString();
    }

    @Test(dataProvider = "createTestNextNPermutationData") 
    public void testNextNPermutation(int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextNPermutation(a);
        if (expected != null) {
            assertTrue(actual);
            assertThat(a, is(expected));
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextNPermutationData() {
        return new Object[][]{
            { new int[]{}, null },
            { new int[]{ 1 }, null },
            { new int[]{ 0, 1 }, new int[]{ 1, 0 } },
            { new int[]{ 1, 0 }, null },
            { new int[]{ 2, 5, 1, 4, 3, 0 }, new int[]{ 2, 5, 3, 0, 1, 4 } },
            { new int[]{ 0, 1, 2 }, new int[]{ 0, 2, 1 } },
            { new int[]{ 0, 2, 1 }, new int[]{ 1, 0, 2 } },
            { new int[]{ 1, 0, 2 }, new int[]{ 1, 2, 0 } },
            { new int[]{ 1, 2, 0 }, new int[]{ 2, 0, 1 } },
            { new int[]{ 2, 0, 1 }, new int[]{ 2, 1, 0 } },
            { new int[]{ 2, 1, 0 }, null },
            { new int[]{ 0, 3, 2, 1 }, new int[]{ 1, 0, 2, 3 } },
            { new int[]{ 4, 3, 0, 1, 2 }, new int[]{ 4, 3, 0, 2, 1 } },
            { new int[]{ 0, 1, 3, 4, 2 }, new int[]{ 0, 1, 4, 2, 3 } },
            { new int[]{ 3, 4, 1, 2, 0 }, new int[]{ 3, 4, 2, 0, 1 } },
            { new int[]{ 5, 6, 0, 3, 1, 2, 4 }, new int[]{ 5, 6, 0, 3, 1, 4, 2 } },
            { new int[]{ 2, 0, 4, 1, 7, 6, 5, 3 }, new int[]{ 2, 0, 4, 3, 1, 5, 6, 7 } }
        };
    }

    @Test(dataProvider = "createTestFirstPermutationData")
    public void testFirstPermutation(int r, int[] expected) {
        int[] actual = MathAlgorithms.firstPermutation(r);
        assertThat(actual, is(expected));
    }

    @DataProvider
    public Object[][] createTestFirstPermutationData() {
        return new Object[][]{
            { 0, new int[0] },
            { 1, new int[]{ 0 } },
            { 2, new int[]{ 0, 1 } }
        };
    }

    @Test(dataProvider = "createTestNextCombinationData") 
    public void testNextCombination(int n, int r, int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextCombination(n, r, a);
        if (expected != null) {
            assertTrue(actual);
            assertThat(a, is(expected));
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextCombinationData() {
        return new Object[][]{
            { 0, 0, new int[0], null },
            { 1, 0, new int[0], null },
            { 2, 0, new int[0], null },
            { 1, 1, new int[]{ 0 }, null },
            { 2, 1, new int[]{ 0 }, new int[]{ 1 } },
            { 2, 1, new int[]{ 1 }, null },
            { 2, 2, new int[]{ 0, 1 }, null },
            { 6, 4, new int[]{ 0, 1, 4, 5 }, new int[]{ 0, 2, 3, 4 } },
            { 5, 3, new int[]{ 0, 1, 2 }, new int[]{ 0, 1, 3 } },
            { 5, 3, new int[]{ 0, 1, 3 }, new int[]{ 0, 1, 4 } },
            { 5, 3, new int[]{ 0, 1, 4 }, new int[]{ 0, 2, 3 } },
            { 5, 3, new int[]{ 0, 2, 3 }, new int[]{ 0, 2, 4 } },
            { 5, 3, new int[]{ 0, 2, 4 }, new int[]{ 0, 3, 4 } },
            { 5, 3, new int[]{ 0, 3, 4 }, new int[]{ 1, 2, 3 } },
            { 5, 3, new int[]{ 1, 2, 3 }, new int[]{ 1, 2, 4 } },
            { 5, 3, new int[]{ 1, 2, 4 }, new int[]{ 1, 3, 4 } },
            { 5, 3, new int[]{ 1, 3, 4 }, new int[]{ 2, 3, 4 } },
            { 5, 3, new int[]{ 2, 3, 4 }, null }
        };
    }

    @Test(dataProvider = "createTestShuffleListData")
    public void testShuffleList(List<Object> items) {
        System.out.println("before MathAlgorithms.shuffleList: " + items);
        MathAlgorithms.shuffleList(items, TestResourceLoader.RAND_GEN);
        System.out.println("after MathAlgorithms.shuffleList: " + items);
    }

    @DataProvider
    public Object[][] createTestShuffleListData() {
        return new Object[][]{
            { new ArrayList<>(Arrays.asList()) },
            { new ArrayList<>(Arrays.asList(true)) },
            { new ArrayList<>(Arrays.asList(1, 2, 3)) },
            { new ArrayList<>(Arrays.asList("bye", "me", "creation")) }
        };
    }

    @Test(dataProvider = "createTestNextCartesianProductTupleData")
    public void testNextCartesianProductTuple(int[] setSizes, int[] t, int[] expected) {
        boolean actual = MathAlgorithms.nextCartesianProductTuple(setSizes, t);
        if (expected != null) {
            assertTrue(actual);
            assertThat(t, is(expected));
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextCartesianProductTupleData() {
        return new Object[][]{
            { new int[]{ 0 }, new int[]{ 0 }, null },
            { new int[]{ 1 }, new int[]{ 0 }, null },
            { new int[]{ 2 }, new int[]{ 0 }, new int[]{ 1 }  },
            { new int[]{ 2 }, new int[]{ 1 }, null },
            { new int[]{ 4 }, new int[]{ 0 }, new int[]{ 1 }  },
            { new int[]{ 4 }, new int[]{ 1 }, new int[]{ 2} },
            { new int[]{ 4 }, new int[]{ 2 }, new int[]{ 3 }  },
            { new int[]{ 4 }, new int[]{ 3 }, null },
            { new int[]{ 2, 3 }, new int[]{ 0, 0 }, new int[]{ 0, 1 } },
            { new int[]{ 2, 3 }, new int[]{ 0, 1 }, new int[]{ 0, 2 } },
            { new int[]{ 2, 3 }, new int[]{ 0, 2 }, new int[]{ 1, 0 } },
            { new int[]{ 2, 3 }, new int[]{ 1, 0 }, new int[]{ 1, 1 } },
            { new int[]{ 2, 3 }, new int[]{ 1, 1 }, new int[]{ 1, 2 } },
            { new int[]{ 2, 3 }, new int[]{ 1, 2 }, null },
            { new int[]{ 2, 1, 3 }, MathAlgorithms.firstCartesianProductTuple(3), 
                new int[]{ 0, 0, 1 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 0, 0, 1 }, new int[]{ 0, 0, 2 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 0, 0, 2 }, new int[]{ 1, 0, 0 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 1, 0, 0 }, new int[]{ 1, 0, 1 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 1, 0, 1 }, new int[]{ 1, 0, 2 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 1, 0, 2 }, null },
            { new int[]{ 0, 1, 3 }, new int[]{ 0, 0, 0 }, null }
        };
    }

    /**
     * Reuse data provider from simpler cartesian product algorithm
     * to demonstrate that data expectations still work with this
     * one under test.
     */
    @Test(dataProvider = "createTestNextCartesianProductTupleData")
    public void testNextCartesianProductTupleAlt(int[] setSizes, int[] t, int[] expected) {
        Function<Integer, Integer> firstElemFunction = idx-> {
            return setSizes[idx] > 0 ? 0 : null;
        };
        BiFunction<Integer, Integer, Integer> nextElemFunction = (idx, currEl) -> {
            if (currEl < setSizes[idx] - 1) {
                return currEl + 1;
            }
            else {
                return null;
            }
        };
        List<Integer> tAsObjList = new ArrayList<>();
        for (int i = 0; i < t.length; i++) {
            if (setSizes[i] > 0) {
                tAsObjList.add(t[i]);
            }
            else {
                tAsObjList.add(null);
            }
        }
        boolean actual = MathAlgorithms.nextCartesianProductTuple(firstElemFunction,
            nextElemFunction, tAsObjList);
        if (expected != null) {
            assertTrue(actual);
            
            t = new int[tAsObjList.size()];
            for (int i = 0; i < tAsObjList.size(); i++) {
                Integer el = tAsObjList.get(i);
                if (el != null) {
                    t[i] = el;
                }
                else {
                    t[i] = -1;
                }
            }
            assertThat(t, is(expected));
        }
        else {
            assertFalse(actual);
        }
    }
}