package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.aaronicsubstances.code.augmentor.core.TestResourceLoader;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.*;

public class MathAlgorithmsTest {

    @Test(dataProvider = "createTestNextPermutationData") 
    public void testNextPermutation(int n, int r, int leastElem, int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextPermutation(n, r, leastElem, a);
        if (expected != null) {
            assertTrue(actual);
            assertEquals(a, expected);
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextPermutationData() {
        return new Object[][]{
            { 0, 0, 0, new int[]{}, null },
            { 1, 1, 2, new int[]{ 2 }, null },
            { 2, 2, 1, new int[]{ 1, 2 }, new int[]{ 2, 1 } },
            { 2, 2, 1, new int[]{ 2, 1 }, null },
            { 6, 6, 1, new int[]{ 3, 6, 2, 5, 4, 1 }, new int[]{ 3, 6, 4, 1, 2, 5 } },
            { 4, 4, 1, new int[]{ 1, 4, 3, 2 }, new int[]{ 2, 1, 3, 4 } },
            { 5, 5, 1, new int[]{ 5, 4, 1, 2, 3 }, new int[]{ 5, 4, 1, 3, 2 } },
            { 5, 5, 1, new int[]{ 1, 2, 4, 5, 3 }, new int[]{ 1, 2, 5, 3, 4 } },
            { 5, 5, 1, new int[]{ 4, 5, 2, 3, 1 }, new int[]{ 4, 5, 3, 1, 2 } },
            { 7, 7, 1, new int[]{ 6, 7, 1, 4, 2, 3, 5 }, new int[]{ 6, 7, 1, 4, 2, 5, 3 } },
            { 8, 8, 1, new int[]{ 3, 1, 5, 2, 8, 7, 6, 4 }, new int[]{ 3, 1, 5, 4, 2, 6, 7, 8 } }
        };
    }

    @Test(dataProvider = "createTestNextNPermutationData") 
    public void testNextNPermutation(int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextNPermutation(a);
        if (expected != null) {
            assertTrue(actual);
            assertEquals(a, expected);
        }
        else {
            assertFalse(actual);
            // assert sorted in ascending order.
            boolean sortedAsc = true;
            for (int i = 0; i < a.length - 1; i++) {
                if (a[i + 1] < a[i]) {
                    sortedAsc = false;
                    break;
                }
            }
            assertTrue(sortedAsc);
        }
    }

    @Test
    public void testNextRPermutation() {
        int n = 5, r = 3, leastElem = 1;
        int[] a = MathAlgorithms.firstPermutation(r, leastElem);
        List<String> permutations = new ArrayList<>();
        permutations.add(stringifyPermutation(a));
        while (MathAlgorithms.nextPermutation(n, r, leastElem, a)) {
            permutations.add(stringifyPermutation(a));
        }
        List<String> expected = Arrays.asList(
            "123", "132", "213", "231", "312", "321", 
            "124", "142", "214", "241", "412", "421",
            "125", "152", "215", "251", "512", "521", 
            "134", "143", "314", "341", "413", "431",
            "135", "153", "315", "351", "513", "531", 
            "145", "154", "415", "451", "514", "541", 
            "234", "243", "324", "342", "423", "432", 
            "235", "253", "325", "352", "523", "532", 
            "245", "254", "425", "452", "524", "542",
            "345", "354", "435", "453", "534", "543");
        assertThat(permutations, is(expected));
    }

    private static String stringifyPermutation(int[] a) {
        StringBuilder s = new StringBuilder();
        for (int d : a) {
            s.append(d);
        }
        return s.toString();
    }

    @DataProvider
    public Object[][] createTestNextNPermutationData() {
        return new Object[][]{
            { new int[]{}, null },
            { new int[]{ 2 }, null },
            { new int[]{ 1, 2 }, new int[]{ 2, 1 } },
            { new int[]{ 2, 1 }, null },
            { new int[]{ 3, 6, 2, 5, 4, 1 }, new int[]{ 3, 6, 4, 1, 2, 5 } },
            { new int[]{ 1, 2, 3 }, new int[]{ 1, 3, 2 } },
            { new int[]{ 1, 3, 2 }, new int[]{ 2, 1, 3 } },
            { new int[]{ 2, 1, 3 }, new int[]{ 2, 3, 1 } },
            { new int[]{ 2, 3, 1 }, new int[]{ 3, 1, 2 } },
            { new int[]{ 3, 1, 2 }, new int[]{ 3, 2, 1 } },
            { new int[]{ 3, 2, 1 }, null },
            { new int[]{ 1, 4, 3, 2 }, new int[]{ 2, 1, 3, 4 } },
            { new int[]{ 5, 4, 1, 2, 3 }, new int[]{ 5, 4, 1, 3, 2 } },
            { new int[]{ 1, 2, 4, 5, 3 }, new int[]{ 1, 2, 5, 3, 4 } },
            { new int[]{ 4, 5, 2, 3, 1 }, new int[]{ 4, 5, 3, 1, 2 } },
            { new int[]{ 6, 7, 1, 4, 2, 3, 5 }, new int[]{ 6, 7, 1, 4, 2, 5, 3 } },
            { new int[]{ 3, 1, 5, 2, 8, 7, 6, 4 }, new int[]{ 3, 1, 5, 4, 2, 6, 7, 8 } }
        };
    }

    @Test(dataProvider = "createTestFirstPermutationData")
    public void testFirstPermutation(int r, int leastElem, int[] expected) {
        int[] actual = MathAlgorithms.firstPermutation(r, leastElem);
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestFirstPermutationData() {
        return new Object[][]{
            { 0, 0, new int[0] },
            { 0, 1, new int[0] },
            { 1, 0, new int[]{ 0 } },
            { 1, 1, new int[]{ 1 } },
            { 2, 0, new int[]{ 0, 1 } },
            { 2, 1, new int[]{ 1, 2 } }
        };
    }

    @Test(dataProvider = "createTestNextCombinationData") 
    public void testNextCombination(int n, int r, int leastElem, int a[], int[] expected) {
        boolean actual = MathAlgorithms.nextCombination(n, r, leastElem, a);
        if (expected != null) {
            assertTrue(actual);
            assertEquals(a, expected);
        }
        else {
            assertFalse(actual);
        }
    }

    @DataProvider
    public Object[][] createTestNextCombinationData() {
        return new Object[][]{
            { 0, 0, 0, new int[0], null },
            { 1, 0, 0, new int[0], null },
            { 2, 0, 0, new int[0], null },
            { 1, 1, 0, new int[]{ 0 }, null },
            { 1, 1, 1, new int[]{ 1 }, null },
            { 2, 1, 0, new int[]{ 0 }, new int[]{ 1 } },
            { 2, 1, 0, new int[]{ 1 }, null },
            { 2, 2, 0, new int[]{ 0, 1 }, null },
            { 2, 2, 1, new int[]{ 1, 2 }, null },
            { 6, 4, 1, new int[]{ 1, 2, 5, 6 }, new int[]{ 1, 3, 4, 5 } },
            { 5, 3, 1, new int[]{ 1, 2, 3 }, new int[]{ 1, 2, 4 } },
            { 5, 3, 1, new int[]{ 1, 2, 4 }, new int[]{ 1, 2, 5 } },
            { 5, 3, 1, new int[]{ 1, 2, 5 }, new int[]{ 1, 3, 4 } },
            { 5, 3, 1, new int[]{ 1, 3, 4 }, new int[]{ 1, 3, 5 } },
            { 5, 3, 1, new int[]{ 1, 3, 5 }, new int[]{ 1, 4, 5 } },
            { 5, 3, 1, new int[]{ 1, 4, 5 }, new int[]{ 2, 3, 4 } },
            { 5, 3, 1, new int[]{ 2, 3, 4 }, new int[]{ 2, 3, 5 } },
            { 5, 3, 1, new int[]{ 2, 3, 5 }, new int[]{ 2, 4, 5 } },
            { 5, 3, 1, new int[]{ 2, 4, 5 }, new int[]{ 3, 4, 5 } },
            { 5, 3, 1, new int[]{ 3, 4, 5 }, null }
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
}