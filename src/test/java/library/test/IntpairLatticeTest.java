package library.test;

import library.util.poset.IntpairPoset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.ArrayList;

public class IntpairLatticeTest {

    @Test
    public void testEmpty() {
        long[] x = {};
        long[] y = {};
        long[][][] resStrict = IntpairPoset.decomposeIntoLIS(x, y, true);
        assertEquals(0, resStrict.length);

        long[][][] resWeak = IntpairPoset.decomposeIntoLIS(x, y, false);
        assertEquals(0, resWeak.length);
    }

    @Test
    public void testSingle() {
        long[] x = {5};
        long[] y = {10};
        long[][][] res = IntpairPoset.decomposeIntoLIS(x, y, true);
        assertEquals(1, res.length);
        assertEquals(1, res[0].length);
        assertEquals(5, res[0][0][0]);
        assertEquals(10, res[0][0][1]);
    }

    @Test
    public void testStrictDuplicates() {
        long[] x = {1, 1, 1};
        long[] y = {2, 2, 2};
        long[][][] res = IntpairPoset.decomposeIntoLIS(x, y, true);
        // Under strict, no two duplicates can be in the same chain.
        // So we must have exactly 3 chains of length 1.
        assertEquals(3, res.length);
        for (int i = 0; i < 3; i++) {
            assertEquals(1, res[i].length);
            assertEquals(1, res[i][0][0]);
            assertEquals(2, res[i][0][1]);
        }
    }

    @Test
    public void testWeakDuplicates() {
        long[] x = {1, 1, 1};
        long[] y = {2, 2, 2};
        long[][][] res = IntpairPoset.decomposeIntoLIS(x, y, false);
        // Under weak, all duplicates can be in the same chain.
        // So we must have exactly 1 chain of length 3.
        assertEquals(1, res.length);
        assertEquals(3, res[0].length);
        for (int i = 0; i < 3; i++) {
            assertEquals(1, res[0][i][0]);
            assertEquals(2, res[0][i][1]);
        }
    }

    @Test
    public void testSmallComprehensive() {
        long[] x = {1, 2, 1, 2, 3};
        long[] y = {3, 1, 2, 2, 1};

        // Strict LIS decomposition
        long[][][] strictRes = IntpairPoset.decomposeIntoLIS(x, y, true);
        verifyDecomposition(x, y, strictRes, true);

        // Weak LIS decomposition
        long[][][] weakRes = IntpairPoset.decomposeIntoLIS(x, y, false);
        verifyDecomposition(x, y, weakRes, false);
    }

    @Test
    public void testRandomComprehensive() {
        java.util.Random rng = new java.util.Random(42);
        for (int t = 0; t < 50; t++) {
            int n = rng.nextInt(100) + 1;
            long[] x = new long[n];
            long[] y = new long[n];
            for (int i = 0; i < n; i++) {
                x[i] = rng.nextInt(20);
                y[i] = rng.nextInt(20);
            }

            long[][][] strictRes = IntpairPoset.decomposeIntoLIS(x, y, true);
            verifyDecomposition(x, y, strictRes, true);

            long[][][] weakRes = IntpairPoset.decomposeIntoLIS(x, y, false);
            verifyDecomposition(x, y, weakRes, false);
        }
    }

    private void verifyDecomposition(long[] x, long[] y, long[][][] res, boolean strict) {
        int N = x.length;

        // 1. Check partition size: total points in chains must equal N
        int totalPoints = 0;
        ArrayList<long[]> allPointsInChains = new ArrayList<>();
        for (long[][] chain : res) {
            assertTrue(chain.length > 0, "Each chain must contain at least one point");
            totalPoints += chain.length;
            for (long[] p : chain) {
                allPointsInChains.add(p);
            }
        }
        assertEquals(N, totalPoints, "Total number of points in chains must equal N");

        // Check that the multiset of points in chains is exactly the input multiset
        long[] xInChains = new long[N];
        long[] yInChains = new long[N];
        for (int i = 0; i < N; i++) {
            xInChains[i] = allPointsInChains.get(i)[0];
            yInChains[i] = allPointsInChains.get(i)[1];
        }

        // Sort both input and chain points to compare
        Integer[] inputOrder = new Integer[N];
        Integer[] chainOrder = new Integer[N];
        for (int i = 0; i < N; i++) {
            inputOrder[i] = i;
            chainOrder[i] = i;
        }
        Arrays.sort(inputOrder, (i, j) -> {
            if (x[i] != x[j]) return Long.compare(x[i], x[j]);
            return Long.compare(y[i], y[j]);
        });
        Arrays.sort(chainOrder, (i, j) -> {
            if (xInChains[i] != xInChains[j]) return Long.compare(xInChains[i], xInChains[j]);
            return Long.compare(yInChains[i], yInChains[j]);
        });

        for (int i = 0; i < N; i++) {
            int idxIn = inputOrder[i];
            int idxCh = chainOrder[i];
            assertEquals(x[idxIn], xInChains[idxCh], "Point X mismatch at index " + i);
            assertEquals(y[idxIn], yInChains[idxCh], "Point Y mismatch at index " + i);
        }

        // 2. Check each chain is increasing
        for (long[][] chain : res) {
            for (int j = 1; j < chain.length; j++) {
                long[] pPrev = chain[j - 1];
                long[] pCurr = chain[j];
                if (strict) {
                    assertTrue(pPrev[0] < pCurr[0] && pPrev[1] < pCurr[1],
                            String.format("Chain not strictly increasing: (%d,%d) -> (%d,%d)",
                                    pPrev[0], pPrev[1], pCurr[0], pCurr[1]));
                } else {
                    assertTrue(pPrev[0] <= pCurr[0] && pPrev[1] <= pCurr[1],
                            String.format("Chain not weakly increasing: (%d,%d) -> (%d,%d)",
                                    pPrev[0], pPrev[1], pCurr[0], pCurr[1]));
                }
            }
        }

        // 3. Verify minimality of the number of chains
        int expectedMinChains = computeTheoreticalMinChains(x, y, strict);
        assertEquals(expectedMinChains, res.length, "Number of chains is not minimal");
    }

    private int computeTheoreticalMinChains(long[] x, long[] y, boolean strict) {
        int N = x.length;
        Integer[] id = new Integer[N];
        for (int i = 0; i < N; i++) id[i] = i;

        if (strict) {
            Arrays.sort(id, (i, j) -> {
                if (x[i] != x[j]) return Long.compare(x[i], x[j]);
                return Long.compare(y[j], y[i]); // Y descending
            });
            // Under strict, min chains = length of longest weakly decreasing subsequence of Y
            int[] dp = new int[N];
            int maxLen = 0;
            for (int i = 0; i < N; i++) {
                dp[i] = 1;
                long py = y[id[i]];
                for (int j = 0; j < i; j++) {
                    long prevY = y[id[j]];
                    if (prevY >= py) { // weakly decreasing
                        dp[i] = Math.max(dp[i], dp[j] + 1);
                    }
                }
                maxLen = Math.max(maxLen, dp[i]);
            }
            return maxLen;
        } else {
            Arrays.sort(id, (i, j) -> {
                if (x[i] != x[j]) return Long.compare(x[i], x[j]);
                return Long.compare(y[i], y[j]); // Y ascending
            });
            // Under weak, min chains = length of longest strictly decreasing subsequence of Y
            int[] dp = new int[N];
            int maxLen = 0;
            for (int i = 0; i < N; i++) {
                dp[i] = 1;
                long py = y[id[i]];
                for (int j = 0; j < i; j++) {
                    long prevY = y[id[j]];
                    if (prevY > py) { // strictly decreasing
                        dp[i] = Math.max(dp[i], dp[j] + 1);
                    }
                }
                maxLen = Math.max(maxLen, dp[i]);
            }
            return maxLen;
        }
    }
}
