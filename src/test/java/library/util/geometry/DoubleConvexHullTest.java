package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Exact-predicate and exhaustive-reference smoke tests. */
class DoubleConvexHullTest {
    /**
     * Checks orientation antisymmetry, exact wide arithmetic, and a degenerate SoS case.
     * Time O(1) for fixed d; space O(1).
     * 未テスト
     */
    @Test
    void exactAndSymbolicPredicates() {
        long[][] t = {{0, 0}, {1, 0}, {0, 1}};
        assertEquals(1, DoubleConvexHull.orientation(t));
        assertEquals(-1, DoubleConvexHull.orientation(new long[][] {{1, 0}, {0, 0}, {0, 1}}));
        assertEquals(1, DoubleConvexHull.orientation(new long[][] {{Long.MIN_VALUE, 0}, {Long.MAX_VALUE, 0}, {Long.MIN_VALUE, 1}}));
        long[][] collinear = {{0, 0}, {1, 0}, {2, 0}};
        assertNotEquals(0, DoubleConvexHull.symbolicOrientation(collinear, new int[] {0, 1, 2}));
        assertEquals(1, DoubleConvexHull.symbolicOrientation(t, new int[] {0, 1, 2}));
    }

    /**
     * Checks exhaustive simplex and support-plane results for a square and 4-simplex.
     * Time O(n^(d+1)); space O(n^d), for the tested small fixed inputs.
     * 未テスト
     */
    @Test
    void exhaustiveReferenceShapes() {
        var square = DoubleConvexHull.exhaustiveReference(new long[][] {
                {0, 0}, {1, 0}, {1, 1}, {0, 1}, {0, 0}, {0, 0}
        });
        assertEquals(4, square.supportingHyperplanes().size());
        assertEquals(4, square.simplices().size());
        var simplex = DoubleConvexHull.exhaustiveReference(new long[][] {
                {0, 0, 0, 0}, {1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}
        });
        assertEquals(5, simplex.simplices().size());
        assertEquals(5, simplex.supportingHyperplanes().size());
        assertTrue(DoubleConvexHull.deduplicate(new long[][] {{1, 2}, {1, 2}}).length == 1);
    }

    /**
     * Compares incremental scan and conflict-list results on a cube and checks low dimension status.
     * Time O(n^4) for these fixed small inputs; space O(n^3).
     * 未テスト
     */
    @Test
    void incrementalStrategiesAgreeOnCube() {
        long[][] cube = new long[8][3];
        for (int mask = 0; mask < 8; mask++) for (int axis = 0; axis < 3; axis++) cube[mask][axis] = (mask >>> axis) & 1;
        var scan = DoubleConvexHull.build(cube, 3, 17, DoubleConvexHull.Strategy.FULL_SCAN);
        var graph = DoubleConvexHull.build(cube, 3, 17, DoubleConvexHull.Strategy.CONFLICT_GRAPH);
        assertEquals(DoubleConvexHull.Status.OK, scan.status());
        assertEquals(DoubleConvexHull.Status.OK, graph.status());
        assertEquals(scan.facets().size(), graph.facets().size());
        assertEquals(12, scan.facets().size());
        assertEquals(12, graph.facets().stream().filter(f -> f.neighbors().stream().allMatch(n -> n >= 0)).count());
        var low = DoubleConvexHull.build(new long[][] {{0, 0, 0}, {1, 0, 0}, {2, 0, 0}}, 3, 1,
                DoubleConvexHull.Strategy.FULL_SCAN);
        assertEquals(DoubleConvexHull.Status.INSUFFICIENT_AFFINE_DIMENSION, low.status());
    }

    /**
     * Confirms the generalized code handles one and five dimensions beyond its primary 2D–4D use.
     * Time O(n^(d+1)) for the exhaustive check; space O(n^d).
     * 未テスト
     */
    @Test
    void supportsAdditionalPositiveDimensions() {
        var line = DoubleConvexHull.build(new long[][] {{-2}, {0}, {3}, {0}}, 1, 9,
                DoubleConvexHull.Strategy.CONFLICT_GRAPH, true);
        assertEquals(DoubleConvexHull.Status.OK, line.status());
        assertEquals(2, line.facets().size());
        assertEquals(2, line.supportingHyperplanes().size());
        long[][] simplex5 = new long[6][5];
        for (int i = 1; i < 6; i++) simplex5[i][i - 1] = 1;
        var reference5 = DoubleConvexHull.exhaustiveReference(simplex5);
        var hull5 = DoubleConvexHull.build(simplex5, 5, 3,
                DoubleConvexHull.Strategy.CONFLICT_GRAPH, true);
        assertEquals(6, reference5.simplices().size());
        assertEquals(reference5.simplices().size(), hull5.facets().size());
    }

    /**
     * Differentially checks cofactor candidates against the slow determinant expansion on degenerate tuples.
     * Time O(T*d!2^(d(d+1))) for T generated tuples; space O(T) in this small test.
     * 未テスト
     */
    @Test
    void fastSymbolicPredicateMatchesExpansion() {
        java.util.Random random = new java.util.Random(8128);
        for (int d : new int[] {2, 3, 4}) {
            int n = d + 5;
            long[][] points = new long[n][d];
            for (int i = 0; i < n; i++) for (int j = 0; j < d; j++) points[i][j] = random.nextInt(3);
            for (int t = 0; t < 80; t++) {
                int[] ids = new int[d + 1];
                java.util.List<Integer> pool = new java.util.ArrayList<>(); for (int i = 0; i < n; i++) pool.add(i);
                java.util.Collections.shuffle(pool, random);
                for (int i = 0; i <= d; i++) ids[i] = pool.get(i);
                assertEquals(DoubleConvexHull.symbolicOrientationSlow(points, ids),
                        DoubleConvexHull.symbolicOrientation(points, ids), "d=" + d + ", tuple=" + java.util.Arrays.toString(ids));
            }
        }
    }

    /**
     * Checks that nonintegral coordinates are accepted directly by the general hull API.
     * Time O(n^5) for the small four-dimensional hull; space O(n^3).
     * 未テスト
     */
    @Test
    void acceptsDoubleCoordinates() {
        var hull = DoubleConvexHull.build(new double[][] {
                {0.25, 0.5}, {1.25, 0.5}, {0.25, 1.5}, {0.5, 0.75}, {0.25, 0.5}
        }, 2, 4, DoubleConvexHull.Strategy.CONFLICT_GRAPH, true);
        assertEquals(DoubleConvexHull.Status.OK, hull.status());
        assertEquals(0, hull.inputToPoint().get(0));
    }
}
