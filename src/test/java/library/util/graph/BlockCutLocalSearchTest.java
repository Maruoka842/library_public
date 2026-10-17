package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import library.util.collections.IntArrayList;

public class BlockCutLocalSearchTest {

    @Test
    public void testBlockProperties() {
        BlockCutLocalSearch.Block block = new BlockCutLocalSearch.Block();
        block.vertices = new int[]{2, 5, 8, 10};
        block.adj = new IntArrayList[4];
        for (int i = 0; i < 4; i++) {
            block.adj[i] = new IntArrayList();
        }

        assertEquals(0, block.indexOf(2));
        assertEquals(2, block.indexOf(8));
        assertTrue(block.indexOf(9) < 0);

        assertTrue(block.isSimple());
        assertEquals(0.0, block.weight(), 1e-9);

        block.adj[0].add(1);
        block.adj[0].add(2);
        block.adj[0].add(3);

        assertFalse(block.isSimple());
        assertEquals(8.0, block.weight(), 1e-9);
    }

    @Test
    public void testBlockCutForestIntegration() {
        Graph g = new Graph(4);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        g.addEdge(2, 3);

        BlockCutForest bcf = new BlockCutForest(g);
        assertEquals(6, bcf.N); // 4 original vertices + 2 blocks

        int[] vertexColor = new int[]{0, 1, 0, 1};
        IntArrayList path = BlockCutLocalSearch.solve(g, 0, 3, vertexColor, 100, new Random(42));
        assertNotNull(path);
        assertTrue(path.size() >= 2);
        assertEquals(0, path.get(0));
        assertEquals(3, path.get(path.size() - 1));
    }

    @Test
    public void testSolveGridSimple() {
        int H = 5, W = 5;
        boolean[][] isObstacle = new boolean[H][W];

        int si = 0, sj = 0;
        int ti = 4, tj = 4;
        long timeBudget = 100;

        List<int[]> path = BlockCutLocalSearch.solveGrid(H, W, isObstacle, si, sj, ti, tj, timeBudget);
        assertNotNull(path);
        assertFalse(path.isEmpty());

        assertEquals(si, path.get(0)[0]);
        assertEquals(sj, path.get(0)[1]);

        boolean visitedTarget = false;
        Set<String> visited = new HashSet<>();

        for (int i = 0; i < path.size(); i++) {
            int[] cell = path.get(i);
            int r = cell[0];
            int c = cell[1];

            String key = r + "," + c;
            assertFalse(visited.contains(key), "Cell " + key + " visited multiple times");
            visited.add(key);

            if (r == ti && c == tj) {
                visitedTarget = true;
            }

            if (i > 0) {
                int[] prev = path.get(i - 1);
                int dist = Math.abs(prev[0] - r) + Math.abs(prev[1] - c);
                assertEquals(1, dist, "Adjacent cells in path must be distance 1");
            }
        }

        assertTrue(visitedTarget, "Path must reach target cell (ti, tj)");
    }

    @Test
    public void testSolveGridWithObstacles() {
        int H = 5, W = 5;
        boolean[][] isObstacle = new boolean[H][W];
        isObstacle[1][1] = true;
        isObstacle[1][4] = true;
        isObstacle[2][4] = true;
        isObstacle[3][0] = true;
        isObstacle[3][2] = true;
        isObstacle[3][4] = true;

        List<int[]> path = BlockCutLocalSearch.solveGrid(H, W, isObstacle, 0, 0, 4, 3, 200, new Random(123));
        assertNotNull(path);
        assertFalse(path.isEmpty());

        assertEquals(0, path.get(0)[0]);
        assertEquals(0, path.get(0)[1]);

        boolean reachedTarget = false;
        Set<String> visited = new HashSet<>();

        for (int i = 0; i < path.size(); i++) {
            int[] cell = path.get(i);
            int r = cell[0];
            int c = cell[1];

            assertFalse(isObstacle[r][c]);

            String key = r + "," + c;
            assertFalse(visited.contains(key));
            visited.add(key);

            if (r == 4 && c == 3) {
                reachedTarget = true;
            }

            if (i > 0) {
                int[] prev = path.get(i - 1);
                int dist = Math.abs(prev[0] - r) + Math.abs(prev[1] - c);
                assertEquals(1, dist);
            }
        }

        assertTrue(reachedTarget);
    }

    @Test
    public void testSolveWithoutTarget() {
        Graph g = new Graph(4);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);

        int[] vertexColor = new int[]{0, 1, 0, 1};
        IntArrayList path = BlockCutLocalSearch.solve(g, 0, vertexColor, 100, new Random(42));
        assertNotNull(path);
        assertTrue(path.size() >= 2);
        assertEquals(0, path.get(0));

        Set<Integer> visited = new HashSet<>();
        for (int i = 0; i < path.size(); i++) {
            int u = path.get(i);
            assertFalse(visited.contains(u), "Vertex " + u + " visited multiple times");
            visited.add(u);
        }
    }

    @Test
    public void testSolveGridWithoutTarget() {
        int H = 5, W = 5;
        boolean[][] isObstacle = new boolean[H][W];
        isObstacle[1][1] = true;
        isObstacle[2][2] = true;

        List<int[]> path = BlockCutLocalSearch.solveGrid(H, W, isObstacle, 0, 0, 200, new Random(123));
        assertNotNull(path);
        assertFalse(path.isEmpty());

        assertEquals(0, path.get(0)[0]);
        assertEquals(0, path.get(0)[1]);

        Set<String> visited = new HashSet<>();

        for (int i = 0; i < path.size(); i++) {
            int[] cell = path.get(i);
            int r = cell[0];
            int c = cell[1];

            assertFalse(isObstacle[r][c]);

            String key = r + "," + c;
            assertFalse(visited.contains(key));
            visited.add(key);

            if (i > 0) {
                int[] prev = path.get(i - 1);
                int dist = Math.abs(prev[0] - r) + Math.abs(prev[1] - c);
                assertEquals(1, dist);
            }
        }
    }
}
