package library.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class DualSlopeTrickSuperTest {

    /**
     * [-500, 500] の離散格子上における DualSlopeTrickSuper のリファレンス愚直実装。
     */
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
    void testMinPlusConvolutionViewMaxRightMarginals() {
        DualSlopeTrickSuper left = new DualSlopeTrickSuper();
        left.extendRight(2, 3); // marginals: [2, 2, 2]
        left.extendRight(5, 2); // marginals: [2, 2, 2, 5, 5]

        DualSlopeTrickSuper right = new DualSlopeTrickSuper();
        right.extendRight(1, 2); // marginals: [1, 1]
        right.extendRight(4, 2); // marginals: [1, 1, 4, 4]

        var conv = left.minPlusConvolutionView(right);

        // Union of marginals: [1, 1, 2, 2, 2, 4, 4, 5, 5]
        // Prefix sums from l=0:
        // r=0: 0
        // r=1: 1
        // r=2: 2
        // r=3: 4
        // r=4: 6
        // r=5: 8
        // r=6: 12
        // r=7: 16
        // r=8: 21
        // r=9: 26

        assertEquals(0, conv.maxRightMarginals(0, 0));
        assertEquals(1, conv.maxRightMarginals(0, 1));
        assertEquals(2, conv.maxRightMarginals(0, 2));
        assertEquals(2, conv.maxRightMarginals(0, 3)); // 1 + 1 + 2 = 4 > 3
        assertEquals(3, conv.maxRightMarginals(0, 4));
        assertEquals(4, conv.maxRightMarginals(0, 6));
        assertEquals(5, conv.maxRightMarginals(0, 8));
        assertEquals(5, conv.maxRightMarginals(0, 11)); // 8 + 4 = 12 > 11
        assertEquals(6, conv.maxRightMarginals(0, 12));
        assertEquals(7, conv.maxRightMarginals(0, 16));
        assertEquals(9, conv.maxRightMarginals(0, 100));

        assertEquals(0, conv.maxRightMarginalsSumLeq(0, 0));
        assertEquals(9, conv.maxRightMarginalsSumLeq(0, 100));

        // Test updating original function and checking live view
        right.extendRight(0, 2); // adds marginals [0, 0] to right -> Union: [0, 0, 1, 1, 2, 2, 2, 4, 4, 5, 5]
        assertEquals(2, conv.maxRightMarginals(0, 0)); // two 0s
        assertEquals(3, conv.maxRightMarginals(0, 1));
        assertEquals(4, conv.maxRightMarginals(0, 2));
    }

    @Test
    void testMinPlusConvolutionViewWithTranslate() {
        DualSlopeTrickSuper left = new DualSlopeTrickSuper();
        left.extendRight(2, 3); // marginal = 2, len = 3
        left.getDualSlopeTrickSuper().translate(10); // shifts breakpoints by +10 -> marginal = 12, len = 3

        DualSlopeTrickSuper right = new DualSlopeTrickSuper();
        right.extendRight(1, 2); // marginal = 1, len = 2

        var conv = left.minPlusConvolutionView(right);
        // Marginals: [1, 1, 12, 12, 12]
        // Prefix sums:
        // r=0: 0
        // r=1: 1
        // r=2: 2
        // r=3: 14
        assertEquals(0, conv.maxRightMarginals(0, 0));
        assertEquals(1, conv.maxRightMarginals(0, 1));
        assertEquals(2, conv.maxRightMarginals(0, 2));
        assertEquals(2, conv.maxRightMarginals(0, 13));
        assertEquals(3, conv.maxRightMarginals(0, 14));
    }

    @Test
    void testMaxRightMarginalsSumLeqAndPredicate() {
        DualSlopeTrickSuper dst = new DualSlopeTrickSuper();
        dst.extendRight(2, 3); // slope=2, len=3 -> marginals: [2, 2, 2]
        dst.extendRight(5, 2); // slope=5, len=2 -> marginals: [2, 2, 2, 5, 5]
        dst.getDualSlopeTrickSuper().verifyInvariants();

        // Marginals: [2, 2, 2, 5, 5]
        assertEquals(0, dst.maxRightMarginalsSumLeq(0, 0));
        assertEquals(0, dst.maxRightMarginals(0, s -> s <= 0));

        assertEquals(1, dst.maxRightMarginalsSumLeq(0, 2));
        assertEquals(1, dst.maxRightMarginals(0, s -> s <= 2));

        assertEquals(2, dst.maxRightMarginalsSumLeq(0, 4));
        assertEquals(2, dst.maxRightMarginals(0, s -> s <= 4));

        assertEquals(3, dst.maxRightMarginalsSumLeq(0, 6));
        assertEquals(3, dst.maxRightMarginals(0, s -> s <= 6));

        assertEquals(3, dst.maxRightMarginalsSumLeq(0, 10)); // 6 + 5 = 11 > 10, so r=3
        assertEquals(4, dst.maxRightMarginalsSumLeq(0, 11)); // 6 + 5 = 11 <= 11, so r=4
        assertEquals(5, dst.maxRightMarginalsSumLeq(0, 16));

        // From l=3: [5, 5]
        assertEquals(3, dst.maxRightMarginalsSumLeq(3, 0));
        assertEquals(4, dst.maxRightMarginalsSumLeq(3, 5));
        assertEquals(5, dst.maxRightMarginalsSumLeq(3, 10));
    }

    @Test
    void testDrawAndToChart() {
        DualSlopeTrickSuper dst = new DualSlopeTrickSuper();
        dst.extendRight(2, 3);
        dst.extendLeft(-1, 2);

        org.knowm.xchart.XYChart chart = dst.toChart();
        assertNotNull(chart);
        assertEquals("x", chart.getXAxisTitle());
        assertEquals("f(x)", chart.getYAxisTitle());

        try {
            dst.draw();
        } catch (java.awt.HeadlessException e) {
            // HeadlessException is expected and fine in headless environments
        } catch (Exception e) {
            fail("draw() threw an unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testBasicDualOperations() {
        DualSlopeTrickSuper dst = new DualSlopeTrickSuper();
        dst.getDualSlopeTrickSuper().verifyInvariants();

        assertEquals(0, dst.getAtZero());
        assertEquals(0, dst.getAt(0));
        assertEquals(0, dst.domainLeft());
        assertEquals(0, dst.domainRight());
        assertEquals(0, dst.getMin());

        var arg0 = dst.argmin();
        assertEquals(0, arg0.min());
        assertEquals(0, arg0.lo());
        assertEquals(0, arg0.hi());

        dst.extendRight(2, 3);
        dst.getDualSlopeTrickSuper().verifyInvariants();

        assertEquals(0, dst.getAt(0));
        assertEquals(2, dst.getAt(1));
        assertEquals(4, dst.getAt(2));
        assertEquals(6, dst.getAt(3));
        assertEquals(0, dst.domainLeft());
        assertEquals(3, dst.domainRight());

        assertEquals(2, dst.getSlopeLeft(1));
        assertEquals(2, dst.getSlopeRight(1));
        assertEquals(2, dst.getSubgradient(1));
    }

    @Test
    void testMinPlusConvolutionViewQueries() {
        DualSlopeTrickSuper f = new DualSlopeTrickSuper();
        f.extendRight(1, 2); // f(0)=0, f(1)=1, f(2)=2; domain [0, 2]; marginals: [1, 1]
        f.extendRight(3, 1); // f(3)=5; domain [0, 3]; marginals: [1, 1, 3]

        DualSlopeTrickSuper g = new DualSlopeTrickSuper();
        g.extendRight(2, 2); // g(0)=0, g(1)=2, g(2)=4; domain [0, 2]; marginals: [2, 2]

        var view = f.minPlusConvolutionView(g);

        // Convolution domain: [0 + 0, 3 + 2] = [0, 5]
        assertEquals(0, view.domainLeft());
        assertEquals(5, view.domainRight());

        // Min value: f.getMin() + g.getMin() = 0 + 0 = 0
        assertEquals(0, view.getMin());
        var arg = view.argmin();
        assertEquals(0, arg.min());
        assertEquals(0, arg.lo());
        assertEquals(0, arg.hi());

        // Destructive convolve copy to compare exact values
        DualSlopeTrickSuper fCopy = new DualSlopeTrickSuper();
        fCopy.extendRight(1, 2);
        fCopy.extendRight(3, 1);

        DualSlopeTrickSuper gCopy = new DualSlopeTrickSuper();
        gCopy.extendRight(2, 2);

        fCopy.minPlusConvolveDestructive(gCopy);

        // Values at x in [0, 5]
        for (int x = 0; x <= 5; x++) {
            assertEquals(fCopy.getAt(x), view.getAt(x), "Mismatch at x=" + x);
            assertEquals(fCopy.getSlopeLeft(x), view.getSlopeLeft(x), "SlopeLeft mismatch at x=" + x);
            assertEquals(fCopy.getSlopeRight(x), view.getSlopeRight(x), "SlopeRight mismatch at x=" + x);
            assertEquals(fCopy.getSubgradient(x), view.getSubgradient(x), "Subgradient mismatch at x=" + x);
        }

        // Out of domain
        assertEquals(SlopeTrickSuper.INF, view.getAt(-1));
        assertEquals(SlopeTrickSuper.INF, view.getAt(6));

        // Marginals: sorted union of [1, 1, 3] and [2, 2] -> [1, 1, 2, 2, 3]
        assertEquals(1, view.marginal(0));
        assertEquals(1, view.marginal(1));
        assertEquals(2, view.marginal(2));
        assertEquals(2, view.marginal(3));
        assertEquals(3, view.marginal(4));

        assertEquals(9, view.sumMarginals(0, 5));
        assertEquals(5, view.sumMarginals(1, 4));
    }

    @Test
    void testGetMinUnboundedBelow() {
        DualSlopeTrickSuper d = new DualSlopeTrickSuper();
        d.clampSlope(1, 2);
        assertEquals(-SlopeTrickSuper.INF, d.getMin());
    }

    @Test
    void testClampSlopeEmptyIntersectionThrows() {
        DualSlopeTrickSuper d = new DualSlopeTrickSuper();
        d.clampSlope(1, 2);
        assertThrows(IllegalArgumentException.class, () -> d.clampSlope(3, 4));
    }

    @Test
    void testMinPlusConvolveUnboundedBelowThrows() {
        DualSlopeTrickSuper f = new DualSlopeTrickSuper();
        f.clampSlope(1, 2);

        DualSlopeTrickSuper g = new DualSlopeTrickSuper();
        g.clampSlope(3, 4);

        assertThrows(IllegalArgumentException.class, () -> f.minPlusConvolveDestructive(g));
    }

    @Test
    void testRandomizedDifferentialWithNaiveDual() {
        long seed = 999L;
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
                    fail("Dual invariant failure at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + "): " + ae.getMessage());
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
