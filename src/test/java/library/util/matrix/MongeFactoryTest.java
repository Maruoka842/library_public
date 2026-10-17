package library.util.matrix;

import library.util.geometry.LongPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MongeFactoryTest {

    @Test
    public void testTwoChainPointMeasureCoreBasic() {
        LongPoint[] upperChain = new LongPoint[]{
                new LongPoint(0, 10),
                new LongPoint(2, 20),
                new LongPoint(5, 30)
        };
        LongPoint[] lowerChain = new LongPoint[]{
                new LongPoint(10, 0),
                new LongPoint(15, 2),
                new LongPoint(20, 5)
        };

        LongPoint[] points = new LongPoint[]{
                new LongPoint(1, 15), // upper[0].x <= 1 < lower[0].x(10) && lower[0].y(0) <= 15 < upper[0].y(10) -> false (15 >= 10)
                                      // upper[1].x(2) <= 1 -> false
                                      // for i=0: upper[0].y=10, 15 < 10 false; upper[1].y=20, 15 < 20 true!
                                      // r0: upperBoundY(upperChain, 15) -> upper[0].y=10 <= 15 (low=1), upper[1].y=20 > 15 -> r0=1
                                      // r1: upperBoundX(upperChain, 1) -> upper[0].x=0 <= 1 (low=1), upper[1].x=2 > 1 -> r1=1
                                      // r0 >= r1 (1 >= 1) -> continue
                new LongPoint(3, 15), // r0: upper[1].y=20 > 15 -> r0=1. r1: upper[1].x=2 <= 3, upper[2].x=5 > 3 -> r1=2. [1, 2)
                                      // c0: lower[0].x=10 > 3 -> c0=0. c1: lower[0].y=0 <= 15, lower[1].y=2 <= 15, lower[2].y=5 <= 15 -> c1=3. [0, 3)
                                      // r0=1, r1=2, c0=0, c1=3 -> row strip!
                new LongPoint(12, 1)  // x=12, y=1
        };

        long[] weights = new long[]{10, 5, 7};

        AntiMongeCoreLong core = MongeFactory.twoChainPointMeasureCore(upperChain, lowerChain, points, weights);
        DenseAntiMongeLong dense = core.dense();

        int R = upperChain.length;
        int C = lowerChain.length;

        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                long expectedSum = 0;
                for (int k = 0; k < points.length; k++) {
                    LongPoint p = points[k];
                    if (upperChain[i].x() <= p.x() && p.x() < lowerChain[j].x()
                            && lowerChain[j].y() <= p.y() && p.y() < upperChain[i].y()) {
                        expectedSum += weights[k];
                    }
                }
                assertEquals(expectedSum, dense.get(i, j), "Mismatch at (" + i + ", " + j + ")");
            }
        }
    }

    @Test
    public void testTwoChainPointMeasureCoreComprehensive() {
        LongPoint[] upperChain = new LongPoint[]{
                new LongPoint(0, 10),
                new LongPoint(1, 15),
                new LongPoint(3, 20),
                new LongPoint(5, 25)
        };

        LongPoint[] lowerChain = new LongPoint[]{
                new LongPoint(6, 0),
                new LongPoint(8, 2),
                new LongPoint(10, 5),
                new LongPoint(12, 8)
        };

        // Grid of test points covering various combinations
        LongPoint[] points = new LongPoint[]{
                new LongPoint(0, 5),   // x=0, y=5
                new LongPoint(2, 12),  // x=2, y=12
                new LongPoint(4, 18),  // x=4, y=18
                new LongPoint(5, 22),  // x=5, y=22
                new LongPoint(7, 1),   // x=7, y=1
                new LongPoint(9, 3),   // x=9, y=3
                new LongPoint(11, 6),  // x=11, y=6
                new LongPoint(2, 3),   // x=2, y=3
                new LongPoint(4, 7),   // x=4, y=7
                new LongPoint(1, 18)   // x=1, y=18
        };

        long[] weights = new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

        AntiMongeCoreLong core = MongeFactory.twoChainPointMeasureCore(upperChain, lowerChain, points, weights);
        DenseAntiMongeLong dense = core.dense();

        int R = upperChain.length;
        int C = lowerChain.length;

        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                long expected = 0;
                for (int k = 0; k < points.length; k++) {
                    LongPoint p = points[k];
                    if (upperChain[i].x() <= p.x() && p.x() < lowerChain[j].x()
                            && lowerChain[j].y() <= p.y() && p.y() < upperChain[i].y()) {
                        expected += weights[k];
                    }
                }
                assertEquals(expected, dense.get(i, j), "Mismatch at (" + i + ", " + j + ")");
            }
        }
    }

    @Test
    public void testEmptyChainsAndPoints() {
        LongPoint[] upperChain = new LongPoint[0];
        LongPoint[] lowerChain = new LongPoint[0];
        LongPoint[] points = new LongPoint[0];
        long[] weights = new long[0];

        AntiMongeCoreLong core = MongeFactory.twoChainPointMeasureCore(upperChain, lowerChain, points, weights);
        assertEquals(0, core.rows());
        assertEquals(0, core.cols());
    }

    @Test
    public void testValidationAndExceptions() {
        LongPoint[] validUpper = new LongPoint[]{new LongPoint(0, 10)};
        LongPoint[] validLower = new LongPoint[]{new LongPoint(10, 0)};

        // Null checks
        assertThrows(NullPointerException.class, () -> MongeFactory.twoChainPointMeasureCore(null, validLower, new LongPoint[0], new long[0]));
        assertThrows(NullPointerException.class, () -> MongeFactory.twoChainPointMeasureCore(validUpper, null, new LongPoint[0], new long[0]));
        assertThrows(NullPointerException.class, () -> MongeFactory.twoChainPointMeasureCore(validUpper, validLower, null, new long[0]));
        assertThrows(NullPointerException.class, () -> MongeFactory.twoChainPointMeasureCore(validUpper, validLower, new LongPoint[0], null));

        // Mismatched length of points and weights
        assertThrows(IllegalArgumentException.class, () -> MongeFactory.twoChainPointMeasureCore(validUpper, validLower, new LongPoint[1], new long[0]));

        // Negative weight
        assertThrows(IllegalArgumentException.class, () -> MongeFactory.twoChainPointMeasureCore(validUpper, validLower, new LongPoint[]{new LongPoint(1, 1)}, new long[]{-1}));

        // Non-monotonic chain
        LongPoint[] badUpper = new LongPoint[]{new LongPoint(2, 10), new LongPoint(1, 10)};
        assertThrows(IllegalArgumentException.class, () -> MongeFactory.twoChainPointMeasureCore(badUpper, validLower, new LongPoint[0], new long[0]));

        // Disjoint chain separation violation
        LongPoint[] badUpperX = new LongPoint[]{new LongPoint(0, 10), new LongPoint(15, 20)};
        assertThrows(IllegalArgumentException.class, () -> MongeFactory.twoChainPointMeasureCore(badUpperX, validLower, new LongPoint[0], new long[0]));
    }
}
