package library.util.linalg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IntegerLinearProgramming2DLong} の単体テスト。
 */
// 未テスト
public class IntegerLinearProgramming2DLongTest {

    @Test
    public void testOptimalSingleBounded() {
        // x <= 5, y <= 5, x >= 0 (-x <= 0), y >= 0 (-y <= 0)
        // maximize 2x + 3y => (5, 5) => 25
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(4);
        ilp.leq(1, 0, 5);
        ilp.leq(0, 1, 5);
        ilp.leq(-1, 0, 0);
        ilp.leq(0, -1, 0);

        IntegerLinearProgramming2DLong.Result res = ilp.maximize(2, 3);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(5, res.x());
        assertEquals(5, res.y());
        assertEquals(25, res.value());
    }

    @Test
    public void testGeq() {
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(4);
        ilp.leq(1, 0, 5);
        ilp.leq(0, 1, 5);
        assertSame(ilp, ilp.geq(1, 0, 0));
        assertSame(ilp, ilp.geq(0, 1, 0));

        IntegerLinearProgramming2DLong.Result res = ilp.maximize(2, 3);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(5, res.x());
        assertEquals(5, res.y());
        assertEquals(25, res.value());
    }

    @Test
    public void testGeqRejectsUnnegatableCoefficientWithoutAddingConstraint() {
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(0);
        assertThrows(ArithmeticException.class, () -> ilp.geq(Long.MIN_VALUE, 0, 0));
        assertThrows(ArithmeticException.class, () -> ilp.geq(0, Long.MIN_VALUE, 0));
        assertThrows(ArithmeticException.class, () -> ilp.geq(0, 0, Long.MIN_VALUE));

        assertEquals(IntegerLinearProgramming2DLong.Status.UNBOUNDED, ilp.maximize(1, 0).status());
    }

    @Test
    public void testMinimize() {
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(4);
        ilp.leq(1, 0, 5);
        ilp.leq(0, 1, 5);
        ilp.geq(1, 0, 0);
        ilp.geq(0, 1, 0);

        IntegerLinearProgramming2DLong.Result res = ilp.minimize(2, 3);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(0, res.x());
        assertEquals(0, res.y());
        assertEquals(0, res.value());
    }

    @Test
    public void testStaticMinimizeInfeasible() {
        long[] a = {1, -1};
        long[] b = {0, 0};
        long[] c = {2, -3};

        IntegerLinearProgramming2DLong.Result res =
                IntegerLinearProgramming2DLong.minimize(a, b, c, 1, 1);
        assertEquals(IntegerLinearProgramming2DLong.Status.INFEASIBLE, res.status());
    }

    @Test
    public void testMinimizeUnbounded() {
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(1);
        ilp.geq(1, 0, 0);

        assertEquals(IntegerLinearProgramming2DLong.Status.UNBOUNDED, ilp.minimize(-1, 0).status());
    }

    @Test
    public void testMinimizePromotesOverflowingRationalIntermediates() {
        IntegerLinearProgramming2DLong ilp = new IntegerLinearProgramming2DLong(3);
        ilp.geq(86, 89, 6_620_707);
        ilp.geq(1, 0, 0);
        ilp.geq(0, 1, 0);

        IntegerLinearProgramming2DLong.Result res = ilp.minimize(641_848, 4_970_314);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(49_412_668_280L, res.value());
        assertTrue(86 * res.x() + 89 * res.y() >= 6_620_707);
        assertTrue(res.x() >= 0);
        assertTrue(res.y() >= 0);
    }

    @Test
    public void testInfeasible() {
        // x <= 2, x >= 3 (-x <= -3)
        long[] a = {1, -1};
        long[] b = {0, 0};
        long[] c = {2, -3};

        IntegerLinearProgramming2DLong.Result res = IntegerLinearProgramming2DLong.maximize(a, b, c, 1, 1);
        assertEquals(IntegerLinearProgramming2DLong.Status.INFEASIBLE, res.status());
    }

    @Test
    public void testUnbounded() {
        // x >= 0 (-x <= 0), y >= 0 (-y <= 0)
        // maximize x + y => UNBOUNDED
        long[] a = {-1, 0};
        long[] b = {0, -1};
        long[] c = {0, 0};

        IntegerLinearProgramming2DLong.Result res = IntegerLinearProgramming2DLong.maximize(a, b, c, 1, 1);
        assertEquals(IntegerLinearProgramming2DLong.Status.UNBOUNDED, res.status());
    }

    @Test
    public void testZeroObjective() {
        // x <= 3, y <= 3, x >= 0, y >= 0
        // maximize 0x + 0y
        long[] a = {1, 0, -1, 0};
        long[] b = {0, 1, 0, -1};
        long[] c = {3, 3, 0, 0};

        IntegerLinearProgramming2DLong.Result res = IntegerLinearProgramming2DLong.maximize(a, b, c, 0, 0);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(0, res.value());
        assertTrue(res.x() >= 0 && res.x() <= 3);
        assertTrue(res.y() >= 0 && res.y() <= 3);
    }

    @Test
    public void testBruteForceComparison() {
        // 2x + 3y <= 12
        // -x + y <= 3
        // x >= 0 (-x <= 0)
        // y >= 0 (-y <= 0)
        // maximize 4x + 5y
        long[] a = {2, -1, -1, 0};
        long[] b = {3, 1, 0, -1};
        long[] c = {12, 3, 0, 0};

        long maxVal = Long.MIN_VALUE;
        long bestX = -1, bestY = -1;
        for (long x = 0; x <= 15; x++) {
            for (long y = 0; y <= 15; y++) {
                if (2 * x + 3 * y <= 12 && -x + y <= 3) {
                    long val = 4 * x + 5 * y;
                    if (val > maxVal) {
                        maxVal = val;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        IntegerLinearProgramming2DLong.Result res = IntegerLinearProgramming2DLong.maximize(a, b, c, 4, 5);
        assertEquals(IntegerLinearProgramming2DLong.Status.OPTIMAL, res.status());
        assertEquals(bestX, res.x());
        assertEquals(bestY, res.y());
        assertEquals(maxVal, res.value());
    }

    @Test
    public void testIsGuaranteedLongSafe() {
        long[] a = {1, -1};
        long[] b = {2, -2};
        long[] c = {10, -5};
        assertTrue(IntegerLinearProgramming2DLong.isGuaranteedLongSafe(a, b, c, 3, 4));
    }
}
