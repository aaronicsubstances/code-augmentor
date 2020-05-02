package com.aaronicsubstances.code.augmentor.core.cs_and_math;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
     * @param leastElem the smallest possible element of a combination, usually 0 or 1.
     * @param a current combination. must contain sorted distinct integers in the range
     *          (leastElem) .. (leastElem + n - 1).
     * @return true if next combination was found; false if current combination is the last one.
     */
    public static boolean nextCombination(int n, int r, int leastElem, int[] a) {
        // First, locate the last element a[i] in the
        // sequence such that a[i] < n − r + i (assuming 0 is least element)
        // NB: book used != (and == in while loop), but < is used to prevent
        // infinite looping with invalid a[] combinations if this subroutine
        // is used to fetch all combinations.
        int i;
        for (i = r - 1; i >= 0; i--) {
            if (a[i] < n - r + i + leastElem) {
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
     * by {@link #nextCombination(int, int, int, int[])} and
     * {@link #nextPermutation(int, int, int, int[])} methods.
     * 
     * @param r size of permutation/combination
     * @param leastElem least possible element of permutation/combination, usually 0 or 1.
     * @return first r-permutation or r-combination in lexicographical order.
     */
    public static int[] firstPermutation(int r, int leastElem) {
        int[] a = new int[r];
        for (int i = 0; i < a.length; i++) {
            a[i] = leastElem + i;
        }
        return a;
    }
    
    /**
     * Generates the next r-permutation in an order determined by two criteria:
     * combination, followed by permutation. 
     * <p> E.g. 142 sorts before 135
     * (since the normalized combination 124 comes before 135) and 412
     * (since the combinations are the same and hence sorting result is as dictated by permutations).
     * 
     * @param n size of universal set. must be nonnegative.
     * @param r size of a combination. must be nonnegative integer not greater than n.
     * @param leastElem the smallest possible element of a permutation, usually 0 or 1.
     * @param a current permutation. must contain distinct integers in the range
     *          (leastElem) .. (leastElem + n - 1).
     * @return true if next permutation was found; false if current permutation is the last one.
     */
    public static boolean nextPermutation(int n, int r, int leastElem, int[] a) {
        // implementation uses the fact that nPr = nCr * r!.
        // hence every combination in nCr has r! permutations.
        boolean perCombinationPermsStillRemaining = nextNPermutation(a);
        if (perCombinationPermsStillRemaining) {
            return true;
        }
        // at this stage due to nextPermutation wrap around last permutation to first one
        // a[] combination is in its original sorted form.
        // So proceed to get next one.
        boolean combinationsStillRemaining = nextCombination(n, r, leastElem, a);
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
     * modified to contain the next permutation. if current permutation is the
     * last one, will be sorted in place.
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
            // lexicographical order.
            // wrap around to first permutation in lexicographical order.
            Arrays.sort(a);
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
}