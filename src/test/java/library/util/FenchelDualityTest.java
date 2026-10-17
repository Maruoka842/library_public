package library.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import library.util.geometry.LongPoint;

public class FenchelDualityTest {

    @Test
    public void testIntegerFenchelBiconjugate() {
        // 1. Simple cases
        assertArrayEquals(new long[]{0, 1, 3}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, 2, 3}));
        assertArrayEquals(new long[]{0, 1, 3}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, 3, 3}));
        assertArrayEquals(new long[]{0, 0, 1}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, 1, 1}));

        // 2. Already convex sequences
        assertArrayEquals(new long[]{0, 1, 3}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, 1, 3}));
        assertArrayEquals(new long[]{3, 1, 0}, FenchelDuality.integerFenchelBiconjugate(new long[]{3, 1, 0}));

        // 3. Edge cases
        assertArrayEquals(new long[]{42}, FenchelDuality.integerFenchelBiconjugate(new long[]{42}));
        assertArrayEquals(new long[]{10, 20}, FenchelDuality.integerFenchelBiconjugate(new long[]{10, 20}));
        assertArrayEquals(new long[]{20, 10}, FenchelDuality.integerFenchelBiconjugate(new long[]{20, 10}));

        // 4. Concave sequence
        assertArrayEquals(new long[]{0, 0, 0}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, 10, 0}));

        // 5. Large value/overflow checks
        assertArrayEquals(new long[]{0, 0, 0}, FenchelDuality.integerFenchelBiconjugate(new long[]{0, Long.MAX_VALUE / 2, 0}));
        long[] bigConvex = {Long.MAX_VALUE / 2, 0, Long.MAX_VALUE / 2};
        assertArrayEquals(bigConvex, FenchelDuality.integerFenchelBiconjugate(bigConvex));

        // 6. Randomized property checks
        java.util.Random rnd = new java.util.Random(42);
        for (int t = 0; t < 100; t++) {
            int len = rnd.nextInt(50) + 3;
            long[] f = new long[len];
            for (int i = 0; i < len; i++) {
                f[i] = rnd.nextLong(-1000, 1001);
            }
            long[] g = FenchelDuality.integerFenchelBiconjugate(f);

            // Check properties
            assertEquals(f.length, g.length);
            for (int i = 0; i < len; i++) {
                assertTrue(g[i] <= f[i], "g[" + i + "] = " + g[i] + " must be <= f[" + i + "] = " + f[i]);
            }
            for (int i = 1; i < len - 1; i++) {
                assertTrue(g[i] - g[i - 1] <= g[i + 1] - g[i], "g must be convex at " + i);
            }

            // Check that it is maximum: any other convex h <= f has h <= g
            for (int i = 0; i < len; i++) {
                g[i]++;
                boolean stillValid = true;
                for (int k = 0; k < len; k++) {
                    if (g[k] > f[k]) stillValid = false;
                }
                for (int k = 1; k < len - 1; k++) {
                    if (g[k] - g[k - 1] > g[k + 1] - g[k]) stillValid = false;
                }
                assertFalse(stillValid, "g should be maximum, but incrementing g[" + i + "] kept it valid");
                g[i]--; // restore
            }
        }
    }

    @Test
    public void testFenchelConjugateVerticesSimple() {
        // f(x) = |x| defined on [-1, 1]
        // x = [-1, 0, 1], y = [1, 0, 1]
        // Conjugate f*(p) should have vertices exactly at (-1, 0) and (1, 0).
        long[] x = {-1, 0, 1};
        long[] y = {1, 0, 1};

        LongPoint[] conj = FenchelDuality.fenchelConjugate(x, y);
        assertEquals(2, conj.length);
        assertEquals(new LongPoint(-1, 0), conj[0]);
        assertEquals(new LongPoint(1, 0), conj[1]);
    }

    @Test
    public void testFenchelConjugateVerticesIntegerSlopes() {
        // f(x) = x^2 approximation: x = [-2, -1, 0, 1, 2], y = [4, 1, 0, 1, 4]
        // Slopes of segments: -3, -1, 1, 3
        // Conjugate f*(p) vertices should be:
        // (-3, 2), (-1, 0), (1, 0), (3, 2)
        long[] x = {-2, -1, 0, 1, 2};
        long[] y = {4, 1, 0, 1, 4};

        LongPoint[] conj = FenchelDuality.fenchelConjugate(x, y);
        assertEquals(4, conj.length);
        assertEquals(new LongPoint(-3, 2), conj[0]);
        assertEquals(new LongPoint(-1, 0), conj[1]);
        assertEquals(new LongPoint(1, 0), conj[2]);
        assertEquals(new LongPoint(3, 2), conj[3]);
    }

    @Test
    public void testNonIntegerSlopesAndNonConvex() {
        // Non-convex and Non-integer slopes!
        // f(x) defined at:
        // x = [0, 2, 3], y = [0, 1, 3]
        // Slopes are:
        // Segment 0: (1-0)/(2-0) = 0.5 (not an integer!)
        // Segment 1: (3-1)/(3-2) = 2.0
        // Lower convex envelope is already convex, vertices are (0,0), (2,1), (3,3).
        // Candidates for Fenchel conjugate corners:
        // floor(0.5)=0, ceil(0.5)=1, floor(2.0)=2, ceil(2.0)=2.
        // Unique candidates: 0, 1, 2.
        // Values: f*(0)=0, f*(1)=1, f*(2)=3.
        // All are non-collinear, so conjugate vertices are:
        // (0, 0), (1, 1), (2, 3)
        long[] x = {0, 2, 3};
        long[] y = {0, 1, 3};

        LongPoint[] conj = FenchelDuality.fenchelConjugate(x, y);
        assertEquals(3, conj.length);
        assertEquals(new LongPoint(0, 0), conj[0]);
        assertEquals(new LongPoint(1, 1), conj[1]);
        assertEquals(new LongPoint(2, 3), conj[2]);
    }

    @Test
    public void testCollinearAndConcaveZigzag() {
        // Zigzag (non-convex):
        // x = [0, 1, 2, 3], y = [0, 10, 2, 4]
        // Lower convex envelope vertices: (0,0), (2,2), (3,4)
        // slopes of envelope segments: s_0 = 1, s_1 = 2 (both integers!)
        // Candidates: floor(1)=1, ceil(1)=1, floor(2)=2, ceil(2)=2.
        // Sorted/Deduplicated candidates: 1, 2.
        // f*(1) = 1*0 - 0 = 0. f*(2) = 2*2 - 2 = 2.
        // Conjugate vertices: (1, 0), (2, 2).
        long[] x = {0, 1, 2, 3};
        long[] y = {0, 10, 2, 4};

        LongPoint[] conj = FenchelDuality.fenchelConjugate(x, y);
        assertEquals(2, conj.length);
        assertEquals(new LongPoint(1, 0), conj[0]);
        assertEquals(new LongPoint(2, 2), conj[1]);
    }
}
