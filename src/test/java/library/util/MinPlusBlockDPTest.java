package library.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class MinPlusBlockDPTest {

    private static final long INF = Long.MAX_VALUE / 4;

    private static long naiveSolve(long[] A, long[] c, long[] mul, long[] p, long[] q) {
        int n = A.length;
        int maxState = n + 2;
        long[] f = new long[maxState];
        Arrays.fill(f, INF);
        f[0] = 0;

        for (int i = 0; i < n; i++) {
            long[] nextF = new long[maxState];
            Arrays.fill(nextF, INF);

            for (int x = 0; x < maxState; x++) {
                long val1 = (x + 1 < maxState && f[x + 1] < INF) ? f[x + 1] + c[i] : INF;

                long val2 = INF;
                for (int k = 1; x - k >= 0; k++) {
                    if (f[x - k] < INF) {
                        val2 = Math.min(val2, f[x - k] + k * A[i]);
                    }
                }

                long best = Math.min(val1, val2);
                if (best < INF) {
                    best = mul[i] * best + p[i] * x + q[i];
                }
                nextF[x] = best;
            }
            f = nextF;
        }
        return Math.min(f[0], f.length > 1 ? f[1] : INF);
    }

    private static long[] naiveApplyBlock(long[] f, long[] A, long[] c, long[] mul, long[] p, long[] q, int off, int m, int outMax) {
        long[] current = f.clone();
        for (int t = 0; t < m; t++) {
            int idx = off + t;
            long a = A[idx], cc = c[idx], w = mul[idx], pi = p[idx], qi = q[idx];
            int curLen = current.length;
            int maxState = curLen + 1;
            long[] nextF = new long[maxState];
            Arrays.fill(nextF, INF);

            for (int x = 0; x < maxState; x++) {
                long val1 = (x + 1 < curLen && current[x + 1] < INF) ? current[x + 1] + cc : INF;
                long val2 = INF;
                for (int k = 1; x - k >= 0 && x - k < curLen; k++) {
                    if (current[x - k] < INF) {
                        val2 = Math.min(val2, current[x - k] + k * a);
                    }
                }
                long best = Math.min(val1, val2);
                if (best < INF) {
                    best = w * best + pi * x + qi;
                }
                nextF[x] = best;
            }
            current = nextF;
        }
        long[] res = new long[outMax + 1];
        Arrays.fill(res, INF);
        for (int x = 0; x <= outMax && x < current.length; x++) {
            res[x] = current[x];
        }
        return res;
    }

    @Test
    public void testBasicSolve() {
        long[] A = {5, 6, 7};
        long[] c = {2, 3, 1};

        long expected = naiveSolve(A, c, MinPlusBlockDP.ones(3), new long[3], new long[3]);
        long actual = MinPlusBlockDP.solve(A, c);
        assertEquals(expected, actual);
    }

    @Test
    public void testSolveWithLinearCost() {
        long[] A = {10, 8, 12, 9};
        long[] c = {3, 2, 4, 1};
        long[] p = {1, 2, 0, 3};
        long[] q = {5, 1, 4, 2};

        long expected = naiveSolve(A, c, MinPlusBlockDP.ones(4), p, q);
        long actual = MinPlusBlockDP.solve(A, c, p, q);
        assertEquals(expected, actual);
    }

    @Test
    public void testSolveWithScalingAndLinearCost() {
        long[] A = {10, 8, 12, 9, 11};
        long[] c = {3, 2, 4, 1, 5};
        long[] mul = {1, 2, 1, 3, 1};
        long[] p = {1, 2, 0, 3, 1};
        long[] q = {5, 1, 4, 2, 0};

        long expected = naiveSolve(A, c, mul, p, q);
        long actual = MinPlusBlockDP.solve(A, c, mul, p, q);
        assertEquals(expected, actual);
    }

    @Test
    public void testRandomizedSolve() {
        Random rnd = new Random(42);
        for (int iter = 0; iter < 100; iter++) {
            int n = rnd.nextInt(15) + 1;
            long[] A = new long[n];
            long[] c = new long[n];
            long[] mul = new long[n];
            long[] p = new long[n];
            long[] q = new long[n];

            for (int i = 0; i < n; i++) {
                A[i] = rnd.nextInt(20) + 5;
                c[i] = rnd.nextInt((int) A[i] - 1) + 1; // 0 < c[i] < A[i]
                mul[i] = rnd.nextInt(3) + 1; // mul > 0
                p[i] = rnd.nextInt(5); // p >= 0
                q[i] = rnd.nextInt(10);
            }

            long expected = naiveSolve(A, c, mul, p, q);
            long actual = MinPlusBlockDP.solve(A, c, mul, p, q);
            assertEquals(expected, actual, "Failed on iteration " + iter);
        }
    }

    @Test
    public void testApplyBlockDirect() {
        long[] f = new long[]{0, INF, INF, INF};
        long[] A = {10, 8, 12};
        long[] c = {3, 2, 4};
        long[] mul = {1, 2, 1};
        long[] p = {1, 0, 2};
        long[] q = {2, 1, 0};
        int off = 0;
        int m = 3;
        int outMax = 5;

        long[] expected = naiveApplyBlock(f, A, c, mul, p, q, off, m, outMax);
        long[] actual = MinPlusBlockDP.applyBlock(f, A, c, mul, p, q, off, m, outMax);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testIllegalArgumentExceptions() {
        long[] A = {5, 6};
        long[] c = {2}; // mismatched length
        long[] mul = {1, 1};
        long[] p = {0, 0};
        long[] q = {0, 0};

        assertThrows(IllegalArgumentException.class, () -> MinPlusBlockDP.solve(A, c, mul, p, q));

        long[] A2 = {5, 6};
        long[] c2 = {2, 3};
        long[] mul2 = {0, 1}; // mul <= 0 invalid
        assertThrows(IllegalArgumentException.class, () -> MinPlusBlockDP.solve(A2, c2, mul2, p, q));

        long[] mul3 = {1, 1};
        long[] p2 = {-1, 0}; // p < 0 invalid
        assertThrows(IllegalArgumentException.class, () -> MinPlusBlockDP.solve(A2, c2, mul3, p2, q));
    }
}
