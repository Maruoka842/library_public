package library.util.algebra.strategy;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KSubarraySumSemiringStrategyTest {

    private static final SemiRingStrategy<Long> LONG_SEMIRING = new SemiRingStrategy<Long>() {
        @Override
        public Long add(Long a, Long b) {
            return a + b;
        }

        @Override
        public Long mul(Long a, Long b) {
            return a * b;
        }

        @Override
        public Long zero() {
            return 0L;
        }

        @Override
        public Long one() {
            return 1L;
        }

        @Override
        public boolean equals(Long a, Long b) {
            return Objects.equals(a, b);
        }
    };

    /**
     * Brute force calculation for k disjoint non-empty subarrays in array A.
     */
    private static long bruteForceKSubarraySum(long[] a, int targetK) {
        int n = a.length;
        long total = 0;
        // Enumerate targetK disjoint non-empty intervals: [l_0, r_0), [l_1, r_1), ..., [l_{k-1}, r_{k-1})
        // 0 <= l_0 < r_0 <= l_1 < r_1 <= ... <= l_{k-1} < r_{k-1} <= n
        total += enumerateIntervals(a, 0, 0, targetK);
        return total;
    }

    private static long enumerateIntervals(long[] a, int lastR, int currentK, int targetK) {
        if (currentK == targetK) {
            return 1L;
        }
        long sum = 0;
        int n = a.length;
        for (int l = lastR; l < n; l++) {
            long prod = 1;
            for (int r = l + 1; r <= n; r++) {
                prod *= a[r - 1];
                long rest = enumerateIntervals(a, r, currentK + 1, targetK);
                sum += prod * rest;
            }
        }
        return sum;
    }

    @Test
    void testIdentityLaws() {
        int k = 3;
        KSubarraySumSemiringStrategy<Long> strat = new KSubarraySumSemiringStrategy<>(LONG_SEMIRING, k);
        var id = strat.identity();

        var node1 = strat.singleton(5L);
        var node2 = strat.singleton(3L);
        var merged = strat.mul(node1, node2);

        var idMergedLeft = strat.mul(id, merged);
        var idMergedRight = strat.mul(merged, id);

        for (int i = 0; i <= k; i++) {
            assertEquals(merged.best[i], idMergedLeft.best[i]);
            assertEquals(merged.best[i], idMergedRight.best[i]);
            assertEquals(merged.prefix[i], idMergedLeft.prefix[i]);
            assertEquals(merged.prefix[i], idMergedRight.prefix[i]);
            assertEquals(merged.suffix[i], idMergedLeft.suffix[i]);
            assertEquals(merged.suffix[i], idMergedRight.suffix[i]);
            assertEquals(merged.both[i], idMergedLeft.both[i]);
            assertEquals(merged.both[i], idMergedRight.both[i]);
        }
    }

    @Test
    void testBruteForceSmallArrays() {
        int kMax = 3;
        KSubarraySumSemiringStrategy<Long> strat = new KSubarraySumSemiringStrategy<>(LONG_SEMIRING, kMax);

        long[][] testCases = {
                {2},
                {2, 3},
                {2, 3, 4},
                {1, 2, 3, 4},
                {-1, 2, -3, 4, -5},
                {3, 1, 4, 1, 5, 9}
        };

        for (long[] arr : testCases) {
            // Build tree / node via sequential mul
            KSubarraySumSemiringStrategy.Node<Long> node = strat.identity();
            for (long val : arr) {
                node = strat.mul(node, strat.singleton(val));
            }

            for (int k = 1; k <= kMax; k++) {
                long expected = bruteForceKSubarraySum(arr, k);
                long actual = node.best[k];
                assertEquals(expected, actual, "Failed for arr=" + java.util.Arrays.toString(arr) + " k=" + k);
            }
        }
    }

    @Test
    void testAssociativity() {
        int k = 3;
        KSubarraySumSemiringStrategy<Long> strat = new KSubarraySumSemiringStrategy<>(LONG_SEMIRING, k);

        var a = strat.singleton(2L);
        var b = strat.singleton(3L);
        var c = strat.singleton(4L);
        var d = strat.singleton(5L);

        var leftFirst = strat.mul(strat.mul(strat.mul(a, b), c), d);
        var rightFirst = strat.mul(a, strat.mul(b, strat.mul(c, d)));
        var treeMerged = strat.mul(strat.mul(a, b), strat.mul(c, d));

        for (int i = 0; i <= k; i++) {
            assertEquals(leftFirst.best[i], rightFirst.best[i]);
            assertEquals(leftFirst.best[i], treeMerged.best[i]);
        }
    }

    @Test
    void testKEqualsZero() {
        KSubarraySumSemiringStrategy<Long> strat = new KSubarraySumSemiringStrategy<>(LONG_SEMIRING, 0);
        var a = strat.singleton(5L);
        var b = strat.singleton(3L);
        var merged = strat.mul(a, b);

        assertEquals(1L, merged.best[0]);
    }
}
