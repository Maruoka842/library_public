package library.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import library.util.seq.Permutation;
import org.junit.jupiter.api.Disabled;

public class ItertoolsTest {

    @Test
    public void testPermutationsWithSign() {
        // n = 0
        int count0 = 0;
        for (Itertools.PermutationWithSign ps : Itertools.permutationsWithSign(0)) {
            assertEquals(0, ps.p().length);
            assertEquals(1, ps.sign());
            count0++;
        }
        assertEquals(1, count0);

        // n = 1
        int count1 = 0;
        for (Itertools.PermutationWithSign ps : Itertools.permutationsWithSign(1)) {
            assertArrayEquals(new int[]{0}, ps.p());
            assertEquals(1, ps.sign());
            count1++;
        }
        assertEquals(1, count1);

        // n = 2
        List<int[]> pList2 = new ArrayList<>();
        List<Integer> signList2 = new ArrayList<>();
        for (Itertools.PermutationWithSign ps : Itertools.permutationsWithSign(2)) {
            pList2.add(ps.p().clone());
            signList2.add(ps.sign());
        }
        assertEquals(2, pList2.size());
        assertArrayEquals(new int[]{0, 1}, pList2.get(0));
        assertEquals(1, signList2.get(0));
        assertArrayEquals(new int[]{1, 0}, pList2.get(1));
        assertEquals(-1, signList2.get(1));

        // n = 3
        List<int[]> pList3 = new ArrayList<>();
        List<Integer> signList3 = new ArrayList<>();
        for (Itertools.PermutationWithSign ps : Itertools.permutationsWithSign(3)) {
            pList3.add(ps.p().clone());
            signList3.add(ps.sign());
        }
        assertEquals(6, pList3.size());
        for (int i = 0; i < pList3.size(); i++) {
            int[] p = pList3.get(i);
            int expectedSign = Permutation.sign(p);
            assertEquals(expectedSign, signList3.get(i));
        }

        // n = 4
        List<int[]> pList4 = new ArrayList<>();
        List<Integer> signList4 = new ArrayList<>();
        for (Itertools.PermutationWithSign ps : Itertools.permutationsWithSign(4)) {
            pList4.add(ps.p().clone());
            signList4.add(ps.sign());
        }
        assertEquals(24, pList4.size());
        for (int i = 0; i < pList4.size(); i++) {
            int[] p = pList4.get(i);
            int expectedSign = Permutation.sign(p);
            assertEquals(expectedSign, signList4.get(i));
            if (i > 0) {
                assertTrue(Arrays.compare(pList4.get(i - 1), p) < 0);
            }
        }
    }


    @Test
    public void testMultiplicativePartitions() {
        // n = 12
        // Partitions: [12], [2, 6], [2, 2, 3], [3, 4]
        List<long[]> result = new ArrayList<>();
        for (long[] p : Itertools.multiplicativePartitions(12)) {
            result.add(p.clone());
        }
        assertEquals(4, result.size());

        List<String> sortedResults = result.stream()
                .map(Arrays::toString)
                .sorted()
                .collect(Collectors.toList());

        assertTrue(sortedResults.contains(Arrays.toString(new long[]{12})));
        assertTrue(sortedResults.contains(Arrays.toString(new long[]{2, 6})));
        assertTrue(sortedResults.contains(Arrays.toString(new long[]{2, 2, 3})));
        assertTrue(sortedResults.contains(Arrays.toString(new long[]{3, 4})));
    }

    @Test
    public void testDistinctMultiplicativePartitions() {
        // n = 12
        // Distinct partitions: [12], [2, 6], [3, 4]
        // [2, 2, 3] is NOT distinct because 2 is repeated.
        List<long[]> result = new ArrayList<>();
        for (long[] p : Itertools.distinctMultiplicativePartitions(12)) {
            result.add(p.clone());
        }
        assertEquals(3, result.size());

        List<String> sortedResults = result.stream()
                .map(Arrays::toString)
                .sorted()
                .collect(Collectors.toList());

        assertTrue(sortedResults.contains(Arrays.toString(new long[]{12})));
        assertTrue(sortedResults.contains(Arrays.toString(new long[]{2, 6})));
        assertTrue(sortedResults.contains(Arrays.toString(new long[]{3, 4})));
        assertFalse(sortedResults.contains(Arrays.toString(new long[]{2, 2, 3})));
    }

    @Test
    public void testMultiplicativePartitionsSmall() {
        // n = 1
        int count1 = 0;
        for (long[] p : Itertools.multiplicativePartitions(1)) {
            assertArrayEquals(new long[]{1}, p);
            count1++;
        }
        assertEquals(1, count1);

        // n = 2
        int count2 = 0;
        for (long[] p : Itertools.multiplicativePartitions(2)) {
            assertArrayEquals(new long[]{2}, p);
            count2++;
        }
        assertEquals(1, count2);
    }

    @Test
    public void testMultiplicativePartitionsError() {
        assertThrows(IllegalArgumentException.class, () -> Itertools.multiplicativePartitions(0));
        assertThrows(IllegalArgumentException.class, () -> Itertools.multiplicativePartitions(-1));
    }

    @Test
    public void testMultiplicativePartitions30() {
        // n = 30
        // [30], [2, 15], [2, 3, 5], [3, 10], [5, 6]
        List<long[]> result = new ArrayList<>();
        for (long[] p : Itertools.multiplicativePartitions(30)) {
            result.add(p.clone());
        }
        assertEquals(5, result.size());
    }

    @Test
    public void testMultiplicativePartitions16() {
        // n = 16
        // [16], [2, 8], [2, 2, 4], [2, 2, 2, 2], [4, 4]
        List<long[]> result = new ArrayList<>();
        for (long[] p : Itertools.multiplicativePartitions(16)) {
            result.add(p.clone());
        }
        assertEquals(5, result.size());

        // distinct n = 16
        // [16], [2, 8]
        List<long[]> resultDistinct = new ArrayList<>();
        for (long[] p : Itertools.distinctMultiplicativePartitions(16)) {
            resultDistinct.add(p.clone());
        }
        assertEquals(2, resultDistinct.size());
    }

    @Test
    public void testGroupByLong() {
        // Empty array
        long[] empty = {};
        assertArrayEquals(new int[0][], Itertools.groupBy(empty));

        // Single element
        long[] single = {100L};
        int[][] resSingle = Itertools.groupBy(single);
        assertEquals(1, resSingle.length);
        assertArrayEquals(new int[]{0}, resSingle[0]);

        // Multiple groups
        long[] a = {1L, 1L, 2L, 3L, 3L, 3L, 2L};
        int[][] resA = Itertools.groupBy(a);
        assertEquals(4, resA.length);
        assertArrayEquals(new int[]{0, 1}, resA[0]);
        assertArrayEquals(new int[]{2}, resA[1]);
        assertArrayEquals(new int[]{3, 4, 5}, resA[2]);
        assertArrayEquals(new int[]{6}, resA[3]);

        // Custom predicate: groups elements within difference of 1
        long[] b = {1L, 2L, 4L, 5L, 7L};
        int[][] resB = Itertools.groupBy(b, (u, v) -> Math.abs(u - v) <= 1);
        assertEquals(3, resB.length);
        assertArrayEquals(new int[]{0, 1}, resB[0]);
        assertArrayEquals(new int[]{2, 3}, resB[1]);
        assertArrayEquals(new int[]{4}, resB[2]);
    }

    @Test
    public void testCombinationsWithReplacement() {
        // n = 3, k = 2
        // Combinations with replacement: [0,0], [0,1], [0,2], [1,1], [1,2], [2,2]
        List<int[]> result = new ArrayList<>();
        List<int[]> sharedRefList = new ArrayList<>();
        for (int[] c : Itertools.combinationsWithReplacement(3, 2)) {
            result.add(c.clone());
            sharedRefList.add(c);
        }

        assertEquals(6, result.size());
        assertArrayEquals(new int[]{0, 0}, result.get(0));
        assertArrayEquals(new int[]{0, 1}, result.get(1));
        assertArrayEquals(new int[]{0, 2}, result.get(2));
        assertArrayEquals(new int[]{1, 1}, result.get(3));
        assertArrayEquals(new int[]{1, 2}, result.get(4));
        assertArrayEquals(new int[]{2, 2}, result.get(5));

        // Since the iterator updates the array in-place, all elements in sharedRefList
        // refer to the same array instance, which ultimately has the last state.
        for (int[] c : sharedRefList) {
            assertSame(sharedRefList.get(0), c);
            assertArrayEquals(new int[]{2, 2}, c);
        }

        // n = 4, k = 1
        List<int[]> result2 = new ArrayList<>();
        for (int[] c : Itertools.combinationsWithReplacement(4, 1)) {
            result2.add(c.clone());
        }
        assertEquals(4, result2.size());
        assertArrayEquals(new int[]{0}, result2.get(0));
        assertArrayEquals(new int[]{1}, result2.get(1));
        assertArrayEquals(new int[]{2}, result2.get(2));
        assertArrayEquals(new int[]{3}, result2.get(3));

        // k = 0
        List<int[]> resultEmpty = new ArrayList<>();
        for (int[] c : Itertools.combinationsWithReplacement(3, 0)) {
            resultEmpty.add(c.clone());
        }
        assertEquals(1, resultEmpty.size());
        assertEquals(0, resultEmpty.get(0).length);
    }

    @Test
    public void testPermutationsN() {
        // n = 3
        List<int[]> result = new ArrayList<>();
        for (int[] p : Itertools.permutations(3)) {
            result.add(p.clone());
        }
        assertEquals(6, result.size());
        assertArrayEquals(new int[]{0, 1, 2}, result.get(0));
        assertArrayEquals(new int[]{0, 2, 1}, result.get(1));
        assertArrayEquals(new int[]{1, 0, 2}, result.get(2));
        assertArrayEquals(new int[]{1, 2, 0}, result.get(3));
        assertArrayEquals(new int[]{2, 0, 1}, result.get(4));
        assertArrayEquals(new int[]{2, 1, 0}, result.get(5));


        // n = 0
        List<int[]> result0 = new ArrayList<>();
        for (int[] p : Itertools.permutations(0)) {
            result0.add(p.clone());
        }
        assertEquals(1, result0.size());
        assertEquals(0, result0.get(0).length);

        // n = 4
        List<int[]> result4 = new ArrayList<>();
        for (int[] p : Itertools.permutations(4)) {
            result4.add(p.clone());
        }
        assertEquals(24, result4.size());
        for (int i = 0; i < result4.size() - 1; i++) {
            assertTrue(Arrays.compare(result4.get(i), result4.get(i + 1)) < 0);
        }
    }

    @Test
    public void testPermutationsNK() {
        // n = 3, k = 2
        List<int[]> result = new ArrayList<>();
        for (int[] p : Itertools.permutations(3, 2)) {
            result.add(p.clone());
        }
        assertEquals(6, result.size());
        assertArrayEquals(new int[]{0, 1}, result.get(0));
        assertArrayEquals(new int[]{0, 2}, result.get(1));
        assertArrayEquals(new int[]{1, 0}, result.get(2));
        assertArrayEquals(new int[]{1, 2}, result.get(3));
        assertArrayEquals(new int[]{2, 0}, result.get(4));
        assertArrayEquals(new int[]{2, 1}, result.get(5));


        // n = 4, k = 2
        List<int[]> result42 = new ArrayList<>();
        for (int[] p : Itertools.permutations(4, 2)) {
            result42.add(p.clone());
        }
        assertEquals(12, result42.size());
        for (int i = 0; i < result42.size() - 1; i++) {
            assertTrue(Arrays.compare(result42.get(i), result42.get(i + 1)) < 0);
        }

        // k = 0
        List<int[]> resultK0 = new ArrayList<>();
        for (int[] p : Itertools.permutations(4, 0)) {
            resultK0.add(p.clone());
        }
        assertEquals(1, resultK0.size());
        assertEquals(0, resultK0.get(0).length);

        // k = n
        List<int[]> result44 = new ArrayList<>();
        for (int[] p : Itertools.permutations(4, 4)) {
            result44.add(p.clone());
        }
        assertEquals(24, result44.size());

        // Invalid cases: k > n, k < 0, n < 0
        List<int[]> resultInvalid1 = new ArrayList<>();
        for (int[] p : Itertools.permutations(3, 4)) {
            resultInvalid1.add(p.clone());
        }
        assertEquals(0, resultInvalid1.size());

        List<int[]> resultInvalid2 = new ArrayList<>();
        for (int[] p : Itertools.permutations(3, -1)) {
            resultInvalid2.add(p.clone());
        }
        assertEquals(0, resultInvalid2.size());

        List<int[]> resultInvalid3 = new ArrayList<>();
        for (int[] p : Itertools.permutations(-1, 2)) {
            resultInvalid3.add(p.clone());
        }
        assertEquals(0, resultInvalid3.size());
    }
}
