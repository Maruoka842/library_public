package library.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DualSlopeTrickTest {

    private static class NaiveDualFunction {
        static final int MIN_X = -500;
        static final int MAX_X = 500;
        static final long INF = 1L << 50;

        long[] f = new long[MAX_X - MIN_X + 1];

        NaiveDualFunction() {
            Arrays.fill(f, INF);
            f[0 - MIN_X] = 0; // f(0) = 0, x != 0 -> +inf
        }

        long getAt(int x) {
            if (x < MIN_X || x > MAX_X) return INF;
            return f[x - MIN_X];
        }

        void addConst(long b) {
            for (int i = 0; i < f.length; i++) {
                if (f[i] < INF) f[i] += b;
            }
        }

        void shift(int d) {
            long[] next = new long[f.length];
            Arrays.fill(next, INF);
            for (int x = MIN_X; x <= MAX_X; x++) {
                int prevX = x - d;
                if (prevX >= MIN_X && prevX <= MAX_X) {
                    next[x - MIN_X] = f[prevX - MIN_X];
                }
            }
            f = next;
        }

        void extendRight(int slope, int length) {
            long[] next = new long[f.length];
            Arrays.fill(next, INF);
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int k = 0; k <= length; k++) {
                    int prevX = x - k;
                    if (prevX >= MIN_X && prevX <= MAX_X && f[prevX - MIN_X] < INF) {
                        minV = Math.min(minV, f[prevX - MIN_X] + (long) slope * k);
                    }
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }

        void extendLeft(int slope, int length) {
            long[] next = new long[f.length];
            Arrays.fill(next, INF);
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int k = 0; k <= length; k++) {
                    int prevX = x + k;
                    if (prevX >= MIN_X && prevX <= MAX_X && f[prevX - MIN_X] < INF) {
                        minV = Math.min(minV, f[prevX - MIN_X] - (long) slope * k);
                    }
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }

        void slideMin(int a, int b) {
            long[] next = new long[f.length];
            Arrays.fill(next, INF);
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int d = a; d <= b; d++) {
                    int prevX = x - d;
                    if (prevX >= MIN_X && prevX <= MAX_X && f[prevX - MIN_X] < INF) {
                        minV = Math.min(minV, f[prevX - MIN_X]);
                    }
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }
    }

    @Test
    void testBasicDualOperations() {
        DualSlopeTrickSuper dst = new DualSlopeTrickSuper();
        assertEquals(0, dst.getAtZero());
        assertEquals(0, dst.getAt(0));

        dst.extendRight(2, 3);

        assertEquals(0, dst.getAt(0));
        assertEquals(2, dst.getAt(1));
        assertEquals(4, dst.getAt(2));
        assertEquals(6, dst.getAt(3));
    }

    @Test
    void testRandomizedDifferentialWithNaiveDual() {
        long seed = 777L;
        Random rng = new Random(seed);
        for (int iter = 0; iter < 100; iter++) {
            DualSlopeTrickSuper dst = new DualSlopeTrickSuper();
            NaiveDualFunction naive = new NaiveDualFunction();

            for (int op = 0; op < 15; op++) {
                int type = rng.nextInt(6);
                String opStr = "";
                switch (type) {
                    case 0 -> {
                        long b = rng.nextInt(10) - 5;
                        opStr = "addConst(" + b + ")";
                        dst.addConst(b);
                        naive.addConst(b);
                    }
                    case 1 -> {
                        int d = rng.nextInt(5) - 2;
                        opStr = "shift(" + d + ")";
                        dst.shift(d);
                        naive.shift(d);
                    }
                    case 2 -> {
                        int slope = rng.nextInt(5) - 2;
                        int len = rng.nextInt(3) + 1;
                        opStr = "extendRight(" + slope + ", " + len + ")";
                        dst.extendRight(slope, len);
                        naive.extendRight(slope, len);
                    }
                    case 3 -> {
                        int slope = rng.nextInt(5) - 2;
                        int len = rng.nextInt(3) + 1;
                        opStr = "extendLeft(" + slope + ", " + len + ")";
                        dst.extendLeft(slope, len);
                        naive.extendLeft(slope, len);
                    }
                    case 4 -> {
                        int a = rng.nextInt(3) - 1;
                        int b = a + rng.nextInt(3);
                        opStr = "slideMin(" + a + ", " + b + ")";
                        dst.slideMin(a, b);
                        naive.slideMin(a, b);
                    }
                    case 5 -> {
                        long a = rng.nextInt(5) - 2;
                        long b = rng.nextInt(10) - 5;
                        opStr = "addLinear(" + a + ", " + b + ")";
                        dst.addLinear(a, b);
                        for (int x = NaiveDualFunction.MIN_X; x <= NaiveDualFunction.MAX_X; x++) {
                            if (naive.f[x - NaiveDualFunction.MIN_X] < NaiveDualFunction.INF) {
                                naive.f[x - NaiveDualFunction.MIN_X] += a * x + b;
                            }
                        }
                    }
                }

                try {
                    dst.getDualSlopeTrickSuper().verifyInvariants();
                } catch (AssertionError ae) {
                    fail("Invariant failure at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + "): " + ae.getMessage());
                }

                for (int x = -10; x <= 10; x++) {
                    long expected = naive.getAt(x);
                    long actual = dst.getAt(x);
                    if (expected >= NaiveDualFunction.INF) {
                        assertTrue(actual >= SlopeTrickSuper.INF / 2, "Failed at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + ") x=" + x);
                    } else {
                        assertEquals(expected, actual, "Failed at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + ") x=" + x);
                    }
                }
            }
        }
    }
}
