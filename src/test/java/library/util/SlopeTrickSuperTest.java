package library.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SlopeTrickSuperTest {

    /**
     * [-500, 500] の離散格子上における愚直区分線形関数のリファレンス実装。
     */
    private static class NaiveConvexFunction {
        static final int MIN_X = -500;
        static final int MAX_X = 500;
        static final long INF = 1L << 50;

        long[] f = new long[MAX_X - MIN_X + 1];

        NaiveConvexFunction() {
            Arrays.fill(f, 0);
        }

        long getAt(int x) {
            if (x < MIN_X || x > MAX_X) return INF;
            return f[x - MIN_X];
        }

        long getAtExt(int x) {
            if (x < MIN_X) {
                if (f[0] >= INF) return INF;
                long slope = f[1] - f[0];
                return f[0] + slope * (x - MIN_X);
            }
            if (x > MAX_X) {
                int last = f.length - 1;
                if (f[last] >= INF) return INF;
                long slope = f[last] - f[last - 1];
                return f[last] + slope * (x - MAX_X);
            }
            return f[x - MIN_X];
        }

        void addConst(long b) {
            for (int i = 0; i < f.length; i++) {
                if (f[i] < INF) f[i] += b;
            }
        }

        void addLinear(long a, long b) {
            for (int x = MIN_X; x <= MAX_X; x++) {
                if (f[x - MIN_X] < INF) {
                    f[x - MIN_X] += a * x + b;
                }
            }
        }

        void translate(int dx) {
            long[] next = new long[f.length];
            for (int x = MIN_X; x <= MAX_X; x++) {
                next[x - MIN_X] = getAtExt(x - dx);
            }
            f = next;
        }

        void addRelu(int a, long w) {
            for (int x = MIN_X; x <= MAX_X; x++) {
                if (f[x - MIN_X] < INF) {
                    f[x - MIN_X] += w * Math.max(0, x - a);
                }
            }
        }

        void addIrelu(int a, long w) {
            for (int x = MIN_X; x <= MAX_X; x++) {
                if (f[x - MIN_X] < INF) {
                    f[x - MIN_X] += w * Math.max(0, a - x);
                }
            }
        }

        void restrictDomain(int l, int r) {
            for (int x = MIN_X; x <= MAX_X; x++) {
                if (x < l || x > r) {
                    f[x - MIN_X] = INF;
                }
            }
        }

        void moveLeftCurve(int w) {
            long[] next = new long[f.length];
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int y = 0; y <= w; y++) {
                    minV = Math.min(minV, getAtExt(x + y));
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }

        void moveRightCurve(int w) {
            long[] next = new long[f.length];
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int y = 0; y <= w; y++) {
                    minV = Math.min(minV, getAtExt(x - y));
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }

        void convolveLinearSegment(int from, int to, int slope, int intercept) {
            long[] next = new long[f.length];
            Arrays.fill(next, INF);
            for (int x = MIN_X; x <= MAX_X; x++) {
                long minV = INF;
                for (int d = from; d <= to; d++) {
                    long prevVal = getAtExt(x - d);
                    if (prevVal < INF) {
                        minV = Math.min(minV, prevVal + (long) slope * d + intercept);
                    }
                }
                next[x - MIN_X] = minV;
            }
            f = next;
        }
    }

    @Test
    void testTranslateLazyPushAggregateCorrectness() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(10, 3);
        st.addRelu(20, 2);

        // Originally: sumXDelta for (10, 3) is 30, for (20, 2) is 40.
        // Total sumXDelta = 70.
        st.translate(5);
        // Translate +5 => keys become 15 and 25.
        // sumXDelta should be 15*3 + 25*2 = 45 + 50 = 95.

        // prodBreakpointsByWeight causes split/merge which triggers push()
        SlopeTrickSuper.Aggregate agg = st.prodBreakpointsByWeight(0, 5);
        assertEquals(5, agg.sumDelta());
        assertEquals(95, agg.sumX());

        st.verifyInvariants();
    }

    @Test
    void testMaxRightBreakpointsByWeightSumLeqAndPredicate() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        // Breakpoints with various weights and x coordinates:
        // x = 2, w = 3 (sumX = 6)
        // x = 5, w = 2 (sumX = 10)
        // x = 10, w = 4 (sumX = 40)
        st.addRelu(2, 3);
        st.addRelu(5, 2);
        st.addRelu(10, 4);
        st.verifyInvariants();

        long totalWeight = 9;

        // Breakpoints expanded: [2, 2, 2, 5, 5, 10, 10, 10, 10]
        // Prefix sums from l=0:
        // r=0: 0
        // r=1: 2
        // r=2: 4
        // r=3: 6
        // r=4: 11
        // r=5: 16
        // r=6: 26
        // r=7: 36
        // r=8: 46
        // r=9: 56

        assertEquals(0, st.maxRightBreakpointsByWeightSumLeq(0, 0));
        assertEquals(0, st.maxRightBreakpointsByWeight(0, sum -> sum <= 0));

        assertEquals(1, st.maxRightBreakpointsByWeightSumLeq(0, 2));
        assertEquals(1, st.maxRightBreakpointsByWeight(0, sum -> sum <= 2));

        assertEquals(2, st.maxRightBreakpointsByWeightSumLeq(0, 5));
        assertEquals(2, st.maxRightBreakpointsByWeight(0, sum -> sum <= 5));

        assertEquals(3, st.maxRightBreakpointsByWeightSumLeq(0, 6));
        assertEquals(3, st.maxRightBreakpointsByWeight(0, sum -> sum <= 6));

        assertEquals(3, st.maxRightBreakpointsByWeightSumLeq(0, 10)); // 6 + 5 > 10, so r=3
        assertEquals(4, st.maxRightBreakpointsByWeightSumLeq(0, 11)); // 6 + 5 <= 11, so r=4
        assertEquals(5, st.maxRightBreakpointsByWeightSumLeq(0, 16));

        assertEquals(9, st.maxRightBreakpointsByWeightSumLeq(0, 100)); // All 9 fit
        assertEquals(9, st.maxRightBreakpointsByWeight(0, sum -> sum <= 100));

        // Testing non-zero l
        // From l=3 (breakpoints: [5, 5, 10, 10, 10, 10]):
        // r=3: sum=0
        // r=4: sum=5
        // r=5: sum=10
        // r=6: sum=20
        assertEquals(3, st.maxRightBreakpointsByWeightSumLeq(3, 0));
        assertEquals(4, st.maxRightBreakpointsByWeightSumLeq(3, 5));
        assertEquals(4, st.maxRightBreakpointsByWeightSumLeq(3, 9));
        assertEquals(5, st.maxRightBreakpointsByWeightSumLeq(3, 10));

        // Test boundary exceptions
        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeightSumLeq(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeightSumLeq(10, 10));
        assertThrows(IllegalArgumentException.class, () -> st.maxRightBreakpointsByWeightSumLeq(0, -1));

        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeight(-1, s -> s <= 10));
        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeight(10, s -> s <= 10));
        assertThrows(IllegalArgumentException.class, () -> st.maxRightBreakpointsByWeight(0, s -> false));
    }

    @Test
    void testMaxRightWithLazyShift() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(1, 5);
        st.addRelu(3, 5);
        st.translate(10); // x coordinates shifted to 11 (w=5) and 13 (w=5)
        st.verifyInvariants();

        // l=0, limit=11 -> r=1 (x=11)
        assertEquals(1, st.maxRightBreakpointsByWeightSumLeq(0, 11));
        assertEquals(1, st.maxRightBreakpointsByWeight(0, sum -> sum <= 11));

        // limit = 11 * 5 = 55 -> r=5
        assertEquals(5, st.maxRightBreakpointsByWeightSumLeq(0, 55));
        assertEquals(5, st.maxRightBreakpointsByWeight(0, sum -> sum <= 55));

        // limit = 55 + 13 = 68 -> r=6
        assertEquals(6, st.maxRightBreakpointsByWeightSumLeq(0, 68));
        assertEquals(6, st.maxRightBreakpointsByWeight(0, sum -> sum <= 68));
    }

    @Test
    void testMaxRightRandomizedDifferential() {
        Random rng = new Random(777L);
        for (int iter = 0; iter < 100; iter++) {
            SlopeTrickSuper st = new SlopeTrickSuper();
            int numNodes = rng.nextInt(20) + 1;
            long currentX = 1;
            java.util.List<Long> expandedX = new java.util.ArrayList<>();
            for (int i = 0; i < numNodes; i++) {
                currentX += rng.nextInt(5) + 1;
                long w = rng.nextInt(5) + 1;
                st.addRelu(currentX, w);
                for (int k = 0; k < w; k++) {
                    expandedX.add(currentX);
                }
            }
            if (rng.nextBoolean()) {
                long dx = rng.nextInt(10) + 1;
                st.translate(dx);
                for (int i = 0; i < expandedX.size(); i++) {
                    expandedX.set(i, expandedX.get(i) + dx);
                }
            }

            int totalW = expandedX.size();
            for (int query = 0; query < 20; query++) {
                long l = rng.nextInt(totalW + 1);
                long limit = rng.nextInt(500);

                // Naive calculation of max r from l such that sum_{k=l}^{r-1} expandedX[k] <= limit
                long curSum = 0;
                long expectedR = l;
                for (long r = l + 1; r <= totalW; r++) {
                    curSum += expandedX.get((int) (r - 1));
                    if (curSum <= limit) {
                        expectedR = r;
                    } else {
                        break;
                    }
                }

                long actSumLeq = st.maxRightBreakpointsByWeightSumLeq(l, limit);
                long actPredicate = st.maxRightBreakpointsByWeight(l, sum -> sum <= limit);

                assertEquals(expectedR, actSumLeq, "iter=" + iter + " l=" + l + " limit=" + limit);
                assertEquals(expectedR, actPredicate, "iter=" + iter + " l=" + l + " limit=" + limit);
            }
        }
    }

    @Test
    void testDrawAndToChart() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addAbs(5);
        st.addRelu(10, 2);

        org.knowm.xchart.XYChart chart = st.toChart();
        assertNotNull(chart);
        assertEquals("x", chart.getXAxisTitle());
        assertEquals("f(x)", chart.getYAxisTitle());

        try {
            st.draw();
        } catch (java.awt.HeadlessException e) {
            // HeadlessException is expected and fine in headless environments
        } catch (Exception e) {
            fail("draw() threw an unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testBasicOperations() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0);
        st.addIrelu(0);
        st.verifyInvariants();
        // f(x) = |x|

        assertEquals(0, st.getAt(0));
        assertEquals(2, st.getAt(2));
        assertEquals(2, st.getAt(-2));

        SlopeTrickSuper.QueryResult qr = st.getMin();
        assertEquals(0, qr.min());
        assertEquals(0, qr.lo());
        assertEquals(0, qr.hi());
    }

    @Test
    void testExhaustiveConvolveLinearSegment() {
        // 全 7 パターンの slope 位置 x from < 0, from = 0, from > 0 x length = 0, length > 0
        int[] slopes = {-5, -2, -1, 0, 1, 3, 10};
        int[] froms = {-3, 0, 2};
        int[] lens = {0, 3};

        for (int slope : slopes) {
            for (int from : froms) {
                for (int len : lens) {
                    int to = from + len;

                    SlopeTrickSuper st = new SlopeTrickSuper();
                    st.addRelu(-2, 2);
                    st.addIrelu(1, 1);
                    st.verifyInvariants();

                    NaiveConvexFunction naive = new NaiveConvexFunction();
                    naive.addRelu(-2, 2);
                    naive.addIrelu(1, 1);

                    st.convolveLinearSegment(from, to, slope, 10);
                    st.verifyInvariants();

                    naive.convolveLinearSegment(from, to, slope, 10);

                    for (int x = -10; x <= 10; x++) {
                        long exp = naive.getAt(x);
                        long act = st.getAt(x);
                        if (exp >= NaiveConvexFunction.INF) {
                            assertTrue(act >= SlopeTrickSuper.INF / 2, "slope=" + slope + " from=" + from + " to=" + to + " x=" + x);
                        } else {
                            assertEquals(exp, act, "slope=" + slope + " from=" + from + " to=" + to + " x=" + x);
                        }
                    }
                }
            }
        }
    }

    @Test
    void testMoveLeftRightCurveDirectComparison() {
        for (int w = 0; w <= 5; w++) {
            SlopeTrickSuper stL = new SlopeTrickSuper();
            stL.addAbs(0, 2);
            stL.moveLeftCurve(w);
            stL.verifyInvariants();

            NaiveConvexFunction naiveL = new NaiveConvexFunction();
            naiveL.addRelu(0, 2);
            naiveL.addIrelu(0, 2);
            naiveL.moveLeftCurve(w);

            for (int x = -10; x <= 10; x++) {
                assertEquals(naiveL.getAt(x), stL.getAt(x), "moveLeftCurve w=" + w + " x=" + x);
            }

            SlopeTrickSuper stR = new SlopeTrickSuper();
            stR.addAbs(0, 2);
            stR.moveRightCurve(w);
            stR.verifyInvariants();

            NaiveConvexFunction naiveR = new NaiveConvexFunction();
            naiveR.addRelu(0, 2);
            naiveR.addIrelu(0, 2);
            naiveR.moveRightCurve(w);

            for (int x = -10; x <= 10; x++) {
                assertEquals(naiveR.getAt(x), stR.getAt(x), "moveRightCurve w=" + w + " x=" + x);
            }
        }
    }

    @Test
    void testPropertyInverseOperations() {
        Random rng = new Random(12345);
        for (int iter = 0; iter < 100; iter++) {
            SlopeTrickSuper st = new SlopeTrickSuper();
            st.addAbs(0, 3);
            st.addRelu(5, 2);
            st.verifyInvariants();

            long[] snapshot = new long[21];
            for (int x = -10; x <= 10; x++) {
                snapshot[x + 10] = st.getAt(x);
            }

            int a = rng.nextInt(10) - 5;
            long w = rng.nextInt(3) + 1;

            st.addRelu(a, w);
            st.verifyInvariants();
            assertTrue(st.canRemoveRelu(a, w));
            st.removeRelu(a, w);
            st.verifyInvariants();

            for (int x = -10; x <= 10; x++) {
                assertEquals(snapshot[x + 10], st.getAt(x), "Inverse relu iter " + iter + " x=" + x);
            }

            st.addIrelu(a, w);
            st.verifyInvariants();
            assertTrue(st.canRemoveIrelu(a, w));
            st.removeIrelu(a, w);
            st.verifyInvariants();

            for (int x = -10; x <= 10; x++) {
                assertEquals(snapshot[x + 10], st.getAt(x), "Inverse irelu iter " + iter + " x=" + x);
            }
        }
    }

    @Test
    void testMetamorphicAlgebraicIdentities() {
        Random rng = new Random(54321);
        for (int iter = 0; iter < 50; iter++) {
            int a1 = rng.nextInt(10) - 5;
            int a2 = rng.nextInt(10) - 5;

            DualSlopeTrickSuper dst1 = new DualSlopeTrickSuper();
            dst1.addLinearOrZero(1, a1);
            dst1.addLinearOrZero(-1, a1);

            DualSlopeTrickSuper dst2 = new DualSlopeTrickSuper();
            dst2.addLinearOrZero(1, a2);

            DualSlopeTrickSuper dst1Copy = new DualSlopeTrickSuper();
            dst1Copy.addLinearOrZero(1, a1);
            dst1Copy.addLinearOrZero(-1, a1);

            DualSlopeTrickSuper dst2Copy = new DualSlopeTrickSuper();
            dst2Copy.addLinearOrZero(1, a2);

            SlopeTrickSuper st1 = dst1Copy.getDualSlopeTrickSuper();
            SlopeTrickSuper st2 = dst2Copy.getDualSlopeTrickSuper();

            // (f Box g)* = f* + g*
            st1.mergeDestructive(st2);
            st1.verifyInvariants();

            dst1.minPlusConvolveDestructive(dst2);
            dst1.getDualSlopeTrickSuper().verifyInvariants();

            for (int p = -5; p <= 5; p++) {
                long valFStar = st1.getAt(p);
                long valDualFStar = dst1.getDualSlopeTrickSuper().getAt(p);
                if (valFStar < SlopeTrickSuper.INF / 2 && valDualFStar < SlopeTrickSuper.INF / 2) {
                    assertEquals(valFStar, valDualFStar, "Metamorphic iter " + iter + " p=" + p);
                }
            }
        }
    }

    @Test
    void testRandomizedDifferentialWithNaive() {
        long seed = 42L;
        Random rng = new Random(seed);
        for (int iter = 0; iter < 100; iter++) {
            SlopeTrickSuper st = new SlopeTrickSuper();
            NaiveConvexFunction naive = new NaiveConvexFunction();

            for (int op = 0; op < 20; op++) {
                int type = rng.nextInt(7);
                String opStr = "";
                switch (type) {
                    case 0 -> {
                        long b = rng.nextInt(10) - 5;
                        opStr = "addConst(" + b + ")";
                        st.addConst(b);
                        naive.addConst(b);
                    }
                    case 1 -> {
                        long a = rng.nextInt(5) - 2;
                        long b = rng.nextInt(10) - 5;
                        opStr = "addLinear(" + a + ", " + b + ")";
                        st.addLinear(a, b);
                        naive.addLinear(a, b);
                    }
                    case 2 -> {
                        int dx = rng.nextInt(5) - 2;
                        opStr = "translate(" + dx + ")";
                        st.translate(dx);
                        naive.translate(dx);
                    }
                    case 3 -> {
                        int a = rng.nextInt(10) - 5;
                        long w = rng.nextInt(3) + 1;
                        opStr = "addRelu(" + a + ", " + w + ")";
                        st.addRelu(a, w);
                        naive.addRelu(a, w);
                    }
                    case 4 -> {
                        int a = rng.nextInt(10) - 5;
                        long w = rng.nextInt(3) + 1;
                        opStr = "addIrelu(" + a + ", " + w + ")";
                        st.addIrelu(a, w);
                        naive.addIrelu(a, w);
                    }
                    case 5 -> {
                        int w = rng.nextInt(3);
                        opStr = "moveLeftCurve(" + w + ")";
                        st.moveLeftCurve(w);
                        naive.moveLeftCurve(w);
                    }
                    case 6 -> {
                        int w = rng.nextInt(3);
                        opStr = "moveRightCurve(" + w + ")";
                        st.moveRightCurve(w);
                        naive.moveRightCurve(w);
                    }
                }

                try {
                    st.verifyInvariants();
                } catch (AssertionError ae) {
                    fail("Invariant failure at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + "): " + ae.getMessage());
                }

                for (int x = -10; x <= 10; x++) {
                    long expected = naive.getAt(x);
                    long actual = st.getAt(x);
                    if (expected >= NaiveConvexFunction.INF) {
                        assertTrue(actual >= SlopeTrickSuper.INF / 2, "Failed at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + ") x=" + x);
                    } else {
                        assertEquals(expected, actual, "Failed at seed=" + seed + " iter=" + iter + " op=" + op + " (" + opStr + ") x=" + x);
                    }
                }
            }
        }
    }
}
