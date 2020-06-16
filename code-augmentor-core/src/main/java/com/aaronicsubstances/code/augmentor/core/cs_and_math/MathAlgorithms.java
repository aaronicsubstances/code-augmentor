package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Contains miscellaneous collection of discrete mathematics algorithms.
 */
public class MathAlgorithms {

    /**
     * Randomly rearranges the items of a list in place.
     * 
     * @param <T> type of list item.
     * @param items list to shuffle randomly in place.
     * @param randGen source of randomness
     */
    public static <T> void shuffleList(List<T> items, Random randGen) {
        int n = items.size();
        for (int i = 0; i < items.size(); i++) {
            // generate random position from 0 ..< (n- i)
            int r = randGen.nextInt(n - i);
            // swap (n - i - 1)th term and rth term.
            T n_i_th_term = items.get(n - i - 1);
            T r_th_term = items.get(r);
            items.set(r, n_i_th_term);
            items.set(n - i - 1, r_th_term);
        }
    }

    /**
     * Generates the next r-combination in lexicographical order.
     * <p>
     * Uses Algorithm 3 in Section 6.6 of Discrete Mathematics and its Applications, 7e
     * by Kenneth Rosen.
     * 
     * @param n size of universal set. must be nonnegative.
     * @param r size of a combination. must be nonnegative integer not greater than n.
     * @param a current combination. must contain sorted distinct integers in the range
     *          0 ... n - 1
     * @return true if next combination was found; false if current combination is the last one.
     */
    public static boolean nextCombination(int n, int r, int[] a) {
        // First, locate the last element a[i] in the
        // sequence such that a[i] < n − r + i
        // NB: book used != (and == in while loop), but < is used to prevent
        // infinite looping with invalid a[] combinations if this subroutine
        // is used to fetch all combinations.
        int i;
        for (i = r - 1; i >= 0; i--) {
            if (a[i] < n - r + i) {
                break;
            }
        }
        if (i < 0) {
            // means a[] already has the last combination in 
            // lexicographical order
            return false;
        }

        // Then, replace a[i] with a[i] + 1
        a[i]++;

        // Finally replace a[j] with a[i] + j − i,
        // for j = i + 1, i + 2, ... end
        for (int j = i + 1; j < r; j++) {
            a[j] = a[i] + j - i;
        }
        return true;
    }

    /**
     * Gets the permutation/combination of r elements from any universal set of size
     * n >= r , which is the first in lexicographical order as determined 
     * by {@link #nextCombination(int, int, int[])} and
     * {@link #nextPermutation(int, int, int[])} methods.
     * 
     * @param r size of permutation/combination
     * @return first r-permutation or r-combination in lexicographical order.
     */
    public static int[] firstPermutation(int r) {
        int[] a = new int[r];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        return a;
    }
    
    /**
     * Generates the next r-permutation in an order determined by two criteria:
     * combination, followed by permutation. 
     * <p> E.g. 142 sorts before 135
     * (since the normalized combination 124 comes before 135) 
     * and 142 sorts before 412
     * (since the combinations are the same and hence sorting result is as dictated by permutations).
     * 
     * @param n size of universal set. must be nonnegative.
     * @param r size of a combination. must be nonnegative integer not greater than n.
     * @param a current permutation. must contain distinct integers in the range
     *          0 .. n - 1.
     * @return true if next permutation was found; false if current permutation is the last one.
     */
    public static boolean nextPermutation(int n, int r, int[] a) {
        // implementation uses the fact that nPr = nCr * r!.
        // hence every combination in nCr has r! permutations.
        boolean perCombinationPermsStillRemaining = nextNPermutation(a);
        if (perCombinationPermsStillRemaining) {
            return true;
        }
        // means a[] now has the last per combination permutation in 
        // lexicographical order.
        // wrap around to first permutation in lexicographical order.
        Arrays.sort(a);
        // Proceed to get next one.
        boolean combinationsStillRemaining = nextCombination(n, r, a);
        if (combinationsStillRemaining) {
            return true;
        }
        return false;
    }

    /**
     * Generates the next n-permutation in lexicographical order.
     * <p>
     * Uses Algorithm 1 in Section 6.6 of Discrete Mathematics and its Applications, 7e
     * by Kenneth Rosen.
     * 
     * @param a current permutation. must contain distinct integers. will be 
     * modified to contain the next permutation.
     * @return true if next permutation was found; false if current permutation
     * was last in lexicographical order.
     */
    public static boolean nextNPermutation(int a[]) {
        int n = a.length;
        // First find the largest j such that a[j] < a[j + 1]
        // and a[j + 1] > a[j + 2] > ... > a[end]
        int j;
        for (j = n - 2; j >= 0; j--) {
            if (a[j] < a[j + 1]) {
                break;
            }
        }
        if (j < 0) {
            // means a[] already has the last permutation in 
            // lexicographical order
            return false;
        }

        // Next look for a[k] such that a[k] is the smallest integer greater than 
        // a[j] to the right of j.
        // since a[j + 1], a[j + 2] .. a[end] are sorted, start looking
        // from a[end] towards a[j + 1].
        int k;
        for (k = n - 1; k > j; k--) {
            if (a[k] > a[j]) {
                break;
            }
        }

        // Swap a[k] with a[j]
        int temp = a[k];
        a[k] = a[j];
        a[j] = temp;

        // Finally restore sorted property of a[j + 1], a[j + 2] .. a[end]
        Arrays.sort(a, j + 1, n);

        return true;
    }

    /**
     * Gets the next cartesian product tuple in lexicographical order following/after 
     * given tuple.
     * 
     * @param setSizes the sizes or cardinalities of the sets involved in the cartesian product.
     * @param t current tuple. Must contain integers such that 0 <= t[i] < setSizes[i] for
     * i = 0 .. < setSizes. Modified to contain next tuple if one is found.
     * @return true if next tuple is found, false if current tuple is the last
     * one.
     */
    public static boolean nextCartesianProductTuple(int[] setSizes, int[] t) {
        // if any set size is empty, then cartesian product is empty set.
        for (int setSize : setSizes) {
            if (setSize <= 0) {
                return false;
            }
        }
        
        // the next tuple is found by locating the first position from the right that is
        // not at its maximum size, then setting all elements to the right of this position to 0s, 
        // and incrementing this found position (from the right) by 1.
        int i;
        for (i = setSizes.length - 1; i >= 0; i--) {
            int maxEl = setSizes[i] - 1;
            int el = t[i];
            if (el < maxEl) {
                break;
            }
        }
        if (i < 0) {
            // means t[] is the last cartesian product tuple in 
            // lexicographical order
            return false;
        }
        t[i]++;
        i++;
        for ( ;i < t.length; i++) {
            t[i] = 0;
        }
        return true;
    }

    /**
     * Gets the first cartesian product tuple in lexicographical order for use with
     * {@link #nextCartesianProductTuple(int[], int[])}
     * 
     * @param tupleSize size of tuple. must be non negative.
     * @return array of zeros and of requested size.
     */
    public static int[] firstCartesianProductTuple(int tupleSize) {
        int[] firstTuple = new int[tupleSize];
        // Java already initializes array to zeros.
        return firstTuple;
    }

    /**
     * Generates the next cartesian product tuple in a specified order after given tuple.
     * <p>
     * Unlike {@link #nextCartesianProductTuple(int[], int[])}, this function is intended
     * to deal with general cases where the elements of the sets involved in the cartesian product
     * are sets of some generic element types with some particular ordering.
     * <p>
     * The elements must be partially ordered in the following way:
     * <ul> 
     *  <li>there must be a first element and a last element.</li>
     *  <li>starting at the first element, it is possible to get the next element in order, and
     *      arrive at the last element.</li>
     * </ul>
     * 
     * @param <T> type of tuple element.
     * @param firstElementFunction function which returns the first element at a given position. 
     * @param nextElementFunction function which returns the next element after the element at a 
     * given position.
     * @param t the current tuple, which will be modified to contain the next one
     * in order.
     * @return true if t[] now has the next tuple; false if t[] is the last tuple
     */
    public static <T> boolean nextCartesianProductTuple(
            Function<Integer, T> firstElementFunction,
            BiFunction<Integer, T, T> nextElementFunction,
            List<T> t) {
        // if any set size is empty, then cartesian product is empty set.
        // Unlike in simpler cartesian product case where set sizes were available,
        // here it is assumed that a set size of 0 means some positions in the current tuple
        // will have null elements.
        // if the firstElementFunction() is used to generate the first elements of the tuple
        // then this assumption will hold.
        for (T el : t) {
            if (el == null) {
                return false;
            }
        }
        
        // the next tuple is found by locating the first position from the right which still
        // has next elements. Then all all elements to the right of this position are set to 
        // whatever firstElementFunction() provides.
        // Then the found position (from the right) is set with the next element found.
        int i;
        T nextElemFound = null;
        for (i = t.size() - 1; i >= 0; i--) {
            nextElemFound = nextElementFunction.apply(i, t.get(i));
            if (nextElemFound != null) {
                break;
            }
        }
        if (i < 0) {
            // means t[] is the last cartesian product tuple in 
            // order
            return false;
        }
        t.set(i, nextElemFound);
        i++;
        for ( ; i < t.size(); i++) {
            T firstElem = firstElementFunction.apply(i);
            if (firstElem == null) {
                return false;
            }
            t.set(i, firstElem);
        }
        return true;
    }
}