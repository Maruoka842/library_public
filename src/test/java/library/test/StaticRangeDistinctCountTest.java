package library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.fold.StaticRangeDistinctCount;

public class StaticRangeDistinctCountTest {

    @Test
    public void testRandom() {
        Random rnd = new Random(42);
        int N = 200;
        int Q = 200;
        int[] a = new int[N];
        for (int i = 0; i < N; i++) {
            a[i] = rnd.nextInt(50);
        }

        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        for (int i = 0; i < Q; i++) {
            int l = rnd.nextInt(N);
            int r = rnd.nextInt(N + 1);
            if (l > r) {
                int tmp = l;
                l = r;
                r = tmp;
            }
            int expected = naiveCount(a, l, r);
            int actual = rdc.count(l, r);
            assertEquals(expected, actual, "Failed at l=" + l + ", r=" + r);
        }
    }

    @Test
    public void testRandomLong() {
        Random rnd = new Random(43);
        int N = 200;
        int Q = 200;
        long[] a = new long[N];
        for (int i = 0; i < N; i++) {
            a[i] = rnd.nextLong(1000000000000L);
        }

        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        for (int i = 0; i < Q; i++) {
            int l = rnd.nextInt(N);
            int r = rnd.nextInt(N + 1);
            if (l > r) {
                int tmp = l;
                l = r;
                r = tmp;
            }
            int expected = naiveCount(a, l, r);
            int actual = rdc.count(l, r);
            assertEquals(expected, actual, "Failed at l=" + l + ", r=" + r);
        }
    }

    @Test
    public void testEmpty() {
        int[] a = new int[0];
        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);
        assertEquals(0, rdc.count(0, 0));
    }

    @Test
    public void testSingle() {
        int[] a = new int[]{5};
        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);
        assertEquals(1, rdc.count(0, 1));
        assertEquals(0, rdc.count(0, 0));
        assertEquals(0, rdc.count(1, 1));
    }

    @Test
    public void testAllIdentical() {
        int[] a = new int[]{1, 1, 1, 1, 1};
        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);
        assertEquals(1, rdc.count(0, 5));
        assertEquals(1, rdc.count(1, 4));
        assertEquals(0, rdc.count(2, 2));
    }

    @Test
    public void testAllDistinct() {
        int[] a = new int[]{1, 2, 3, 4, 5};
        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);
        assertEquals(5, rdc.count(0, 5));
        assertEquals(3, rdc.count(1, 4));
    }

    @Test
    public void testNewMethodsRandom() {
        Random rnd = new Random(44);
        int N = 200;
        int Q = 500;
        int[] a = new int[N];
        for (int i = 0; i < N; i++) {
            a[i] = rnd.nextInt(30);
        }

        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        for (int i = 0; i < Q; i++) {
            int l = rnd.nextInt(N + 1);
            int m = rnd.nextInt(N + 1);
            int r = rnd.nextInt(N + 1);

            // Sort to ensure l <= m <= r
            int[] sorted = new int[]{l, m, r};
            java.util.Arrays.sort(sorted);
            l = sorted[0];
            m = sorted[1];
            r = sorted[2];

            assertEquals(naiveCountOnlyRight(a, l, m, r), rdc.countOnlyRight(l, m, r), "Failed onlyRight at l=" + l + ", m=" + m + ", r=" + r);
            assertEquals(naiveCountOnlyLeft(a, l, m, r), rdc.countOnlyLeft(l, m, r), "Failed onlyLeft at l=" + l + ", m=" + m + ", r=" + r);
            assertEquals(naiveCountCommon(a, l, m, r), rdc.countCommon(l, m, r), "Failed common at l=" + l + ", m=" + m + ", r=" + r);
        }
    }

    @Test
    public void testNewMethodsRandomLong() {
        Random rnd = new Random(45);
        int N = 200;
        int Q = 500;
        long[] a = new long[N];
        for (int i = 0; i < N; i++) {
            a[i] = rnd.nextLong(1000000000000L) % 50;
        }

        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        for (int i = 0; i < Q; i++) {
            int l = rnd.nextInt(N + 1);
            int m = rnd.nextInt(N + 1);
            int r = rnd.nextInt(N + 1);

            // Sort to ensure l <= m <= r
            int[] sorted = new int[]{l, m, r};
            java.util.Arrays.sort(sorted);
            l = sorted[0];
            m = sorted[1];
            r = sorted[2];

            assertEquals(naiveCountOnlyRight(a, l, m, r), rdc.countOnlyRight(l, m, r), "Failed onlyRight (long) at l=" + l + ", m=" + m + ", r=" + r);
            assertEquals(naiveCountOnlyLeft(a, l, m, r), rdc.countOnlyLeft(l, m, r), "Failed onlyLeft (long) at l=" + l + ", m=" + m + ", r=" + r);
            assertEquals(naiveCountCommon(a, l, m, r), rdc.countCommon(l, m, r), "Failed common (long) at l=" + l + ", m=" + m + ", r=" + r);
        }
    }

    @Test
    public void testKthDistinctAndDuplicateAndPreviousOccurrenceRandom() {
        Random rnd = new Random(46);
        int N = 100;
        int Q = 500;
        int[] a = new int[N];
        for (int i = 0; i < N; i++) {
            a[i] = rnd.nextInt(20);
        }

        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        for (int i = 0; i < Q; i++) {
            int l = rnd.nextInt(N);
            int r = rnd.nextInt(N + 1);
            if (l > r) {
                int tmp = l;
                l = r;
                r = tmp;
            }
            int k = rnd.nextInt(Math.max(1, r - l + 2)) - 1; // can be -1, 0, 1, ... up to r - l + 1

            int expectedDistinct = naiveKthDistinctIndex(a, l, r, k);
            int actualDistinct = rdc.kthDistinctIndex(l, r, k);
            assertEquals(expectedDistinct, actualDistinct, "Failed kthDistinctIndex at l=" + l + ", r=" + r + ", k=" + k);

            int expectedDuplicate = naiveKthDuplicateIndex(a, l, r, k);
            int actualDuplicate = rdc.kthDuplicateIndex(l, r, k);
            assertEquals(expectedDuplicate, actualDuplicate, "Failed kthDuplicateIndex at l=" + l + ", r=" + r + ", k=" + k);

            int expectedPrevOccur = naiveKthPreviousOccurrence(a, l, r, k);
            int actualPrevOccur = rdc.kthPreviousOccurrence(l, r, k);
            assertEquals(expectedPrevOccur, actualPrevOccur, "Failed kthPreviousOccurrence at l=" + l + ", r=" + r + ", k=" + k);
        }
    }

    private int naiveKthDistinctIndex(int[] a, int l, int r, int k) {
        if (l >= r || k < 0) return -1;
        HashSet<Integer> seen = new HashSet<>();
        int count = 0;
        for (int i = l; i < r; i++) {
            if (!seen.contains(a[i])) {
                seen.add(a[i]);
                if (count == k) {
                    return i;
                }
                count++;
            }
        }
        return -1;
    }

    private int naiveKthDuplicateIndex(int[] a, int l, int r, int k) {
        if (l >= r || k < 0) return -1;
        HashSet<Integer> seen = new HashSet<>();
        int count = 0;
        for (int i = l; i < r; i++) {
            if (seen.contains(a[i])) {
                if (count == k) {
                    return i;
                }
                count++;
            } else {
                seen.add(a[i]);
            }
        }
        return -1;
    }

    private static class ElementWithPrev {
        int index;
        int prev;
        ElementWithPrev(int index, int prev) {
            this.index = index;
            this.prev = prev;
        }
    }

    private int naiveKthPreviousOccurrence(int[] a, int l, int r, int k) {
        if (l >= r || k < 0 || k >= r - l) return -1;
        HashSet<Integer> seen = new HashSet<>();
        java.util.List<ElementWithPrev> list = new java.util.ArrayList<>();
        for (int i = l; i < r; i++) {
            if (!seen.contains(a[i])) {
                seen.add(a[i]);
                // find last occurrence of a[i] before l
                int lastOccur = -1;
                for (int j = l - 1; j >= 0; j--) {
                    if (a[j] == a[i]) {
                        lastOccur = j;
                        break;
                    }
                }
                list.add(new ElementWithPrev(i, lastOccur));
            }
        }
        if (k >= list.size()) return -1;

        list.sort((e1, e2) -> {
            if (e1.prev != e2.prev) {
                return Integer.compare(e1.prev, e2.prev);
            }
            return Integer.compare(e1.index, e2.index);
        });

        return list.get(k).prev;
    }

    @Test
    public void testNewMethodsBoundaryAndViolations() {
        int[] a = new int[]{1, 2, 1, 3, 2, 4};
        StaticRangeDistinctCount rdc = new StaticRangeDistinctCount(a);

        // m >= r cases
        assertEquals(0, rdc.countOnlyRight(1, 4, 3));
        assertEquals(rdc.count(1, 4), rdc.countOnlyLeft(1, 4, 3));
        assertEquals(0, rdc.countCommon(1, 4, 3));

        // l >= m cases
        assertEquals(rdc.count(2, 5), rdc.countOnlyRight(3, 2, 5));
        assertEquals(0, rdc.countOnlyLeft(3, 2, 5));
        assertEquals(0, rdc.countCommon(3, 2, 5));

        // both l >= m and m >= r
        assertEquals(0, rdc.countOnlyRight(5, 4, 3));
        assertEquals(0, rdc.countOnlyLeft(5, 4, 3));
        assertEquals(0, rdc.countCommon(5, 4, 3));

        // m == r or l == m
        assertEquals(0, rdc.countOnlyRight(1, 3, 3));
        assertEquals(rdc.count(1, 3), rdc.countOnlyLeft(1, 3, 3));
        assertEquals(0, rdc.countCommon(1, 3, 3));

        assertEquals(rdc.count(3, 5), rdc.countOnlyRight(3, 3, 5));
        assertEquals(0, rdc.countOnlyLeft(3, 3, 5));
        assertEquals(0, rdc.countCommon(3, 3, 5));
    }

    private int naiveCount(int[] a, int l, int r) {
        HashSet<Integer> set = new HashSet<>();
        for (int i = l; i < r; i++) {
            set.add(a[i]);
        }
        return set.size();
    }

    private int naiveCount(long[] a, int l, int r) {
        HashSet<Long> set = new HashSet<>();
        for (int i = l; i < r; i++) {
            set.add(a[i]);
        }
        return set.size();
    }

    private int naiveCountOnlyRight(int[] a, int l, int m, int r) {
        HashSet<Integer> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Integer> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (int val : rightSet) {
            if (!leftSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }

    private int naiveCountOnlyRight(long[] a, int l, int m, int r) {
        HashSet<Long> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Long> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (long val : rightSet) {
            if (!leftSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }

    private int naiveCountOnlyLeft(int[] a, int l, int m, int r) {
        HashSet<Integer> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Integer> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (int val : leftSet) {
            if (!rightSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }

    private int naiveCountOnlyLeft(long[] a, int l, int m, int r) {
        HashSet<Long> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Long> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (long val : leftSet) {
            if (!rightSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }

    private int naiveCountCommon(int[] a, int l, int m, int r) {
        HashSet<Integer> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Integer> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (int val : leftSet) {
            if (rightSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }

    private int naiveCountCommon(long[] a, int l, int m, int r) {
        HashSet<Long> leftSet = new HashSet<>();
        for (int i = l; i < m; i++) {
            leftSet.add(a[i]);
        }
        HashSet<Long> rightSet = new HashSet<>();
        for (int i = m; i < r; i++) {
            rightSet.add(a[i]);
        }
        int cnt = 0;
        for (long val : leftSet) {
            if (rightSet.contains(val)) {
                cnt++;
            }
        }
        return cnt;
    }
}
