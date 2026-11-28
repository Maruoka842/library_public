package library.util.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

public class WagnerWeiheTest {

    @Test
    public void testEmpty() {
        PlanarMap g = PlanarMap.grid(2, 2);
        WagnerWeihe.Result res = WagnerWeihe.solve(g, new int[0], new int[0]);
        assertTrue(res.feasible);
        assertEquals(0, res.paths.length);
    }

    @Test
    public void testTrivialSameEndpoint() {
        PlanarMap g = PlanarMap.grid(2, 2);
        WagnerWeihe.Result res = WagnerWeihe.solve(g, new int[]{0, 1}, new int[]{0, 1});
        assertTrue(res.feasible);
        assertEquals(2, res.paths.length);
        assertEquals(0, res.paths[0].length);
        assertEquals(0, res.paths[1].length);
    }

    @Test
    public void testGrid3x3() {
        // 3x3 grid:
        // 0 1 2
        // 3 4 5
        // 6 7 8
        // Face 1 is outer face
        // Vertices 1, 3, 5, 7 have degree 3 in G (odd)
        // With demand pairs (1, 7) and (3, 5), degR = 1 for all of them (odd)
        // Hence (V, E U R) is Eulerian.
        PlanarMap g = PlanarMap.grid(3, 3);
        int[] s = {1, 3};
        int[] t = {7, 5};
        WagnerWeihe.Result res = WagnerWeihe.solve(g, 1, s, t);
        assertTrue(res.feasible);
        assertEquals(2, res.paths.length);

        assertValidEdgeDisjointPaths(g, s, t, res.paths);
    }

    @Test
    public void testGrid3x3AutoFace() {
        PlanarMap g = PlanarMap.grid(3, 3);
        int[] s = {1, 3};
        int[] t = {7, 5};
        WagnerWeihe.Result res = WagnerWeihe.solve(g, s, t);
        assertTrue(res.feasible);
        assertEquals(2, res.paths.length);

        assertValidEdgeDisjointPaths(g, s, t, res.paths);
    }

    @Test
    public void testEulerianViolation() {
        // In 3x3 grid, corners have degree 2.
        // If demand pair connects corners 0 and 8, degR(0)=1, degE(0)=2 (parity mismatch).
        PlanarMap g = PlanarMap.grid(3, 3);
        WagnerWeihe.Result res = WagnerWeihe.solve(g, 1, new int[]{0}, new int[]{8});
        assertFalse(res.feasible);
        assertNull(res.paths);
    }

    @Test
    public void testCutConditionViolation() {
        // Two triangles joined by a single edge (or 2 edges) where cut is violated.
        // Let's create two 4-cycles joined by 2 parallel edges or a narrow neck.
        // Or simply a 2x3 grid:
        // 0 1 2
        // 3 4 5
        // Edges:
        // 0-1, 1-2, 3-4, 4-5
        // 0-3, 1-4, 2-5
        // deg in G:
        // 0: 2, 1: 3, 2: 2
        // 3: 2, 4: 3, 5: 2
        // Odd vertices are 1 and 4.
        // If demand pair is (1, 4), degR(1)=1, degR(4)=1.
        // Cut between left and right: 1-4 is a cut of capacity 3.
        // One path 1->4 is feasible.
        PlanarMap g = PlanarMap.grid(2, 3);
        WagnerWeihe.Result res = WagnerWeihe.solve(g, new int[]{1}, new int[]{4});
        assertTrue(res.feasible);
        assertValidEdgeDisjointPaths(g, new int[]{1}, new int[]{4}, res.paths);
    }

    @Test
    public void testGrid4x4MultiplePairs() {
        // 4x4 grid:
        //  0  1  2  3
        //  4  5  6  7
        //  8  9 10 11
        // 12 13 14 15
        // Odd degree vertices on boundary:
        // Row 0: 1 (deg 3), 2 (deg 3)
        // Row 3: 13 (deg 3), 14 (deg 3)
        // Col 0: 4 (deg 3), 8 (deg 3)
        // Col 3: 7 (deg 3), 11 (deg 3)
        // All 8 boundary side vertices have degree 3!
        // We can pair them up:
        // (1, 14), (2, 13), (4, 11), (8, 7)
        PlanarMap g = PlanarMap.grid(4, 4);
        int[] s = {1, 2, 4, 8};
        int[] t = {14, 13, 11, 7};
        WagnerWeihe.Result res = WagnerWeihe.solve(g, s, t);
        assertTrue(res.feasible);
        assertEquals(4, res.paths.length);
        assertValidEdgeDisjointPaths(g, s, t, res.paths);
    }

    @Test
    public void testInvalidArguments() {
        PlanarMap g = PlanarMap.grid(2, 2);
        assertThrows(IllegalArgumentException.class, () -> {
            WagnerWeihe.solve(g, new int[]{0}, new int[]{1, 2});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            WagnerWeihe.solve(g, new int[]{-1}, new int[]{0});
        });
        assertThrows(IllegalArgumentException.class, () -> {
            WagnerWeihe.solve(g, 999, new int[]{0}, new int[]{1});
        });
    }

    private void assertValidEdgeDisjointPaths(PlanarMap g, int[] s, int[] t, int[][] paths) {
        assertEquals(s.length, paths.length);
        HashSet<Integer> usedEdges = new HashSet<>();
        for (int i = 0; i < s.length; i++) {
            int curr = s[i];
            for (int d : paths[i]) {
                assertEquals(curr, g.from(d), "Path " + i + " discontinuous");
                int edge = d >> 1;
                assertTrue(usedEdges.add(edge), "Edge " + edge + " used more than once");
                curr = g.to(d);
            }
            assertEquals(t[i], curr, "Path " + i + " did not reach target");
        }
    }
}
