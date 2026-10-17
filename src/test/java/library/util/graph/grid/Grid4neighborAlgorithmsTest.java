package library.util.graph.grid;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import library.util.graph.Graph;
import static org.junit.jupiter.api.Assertions.*;

public class Grid4neighborAlgorithmsTest {

    @Test
    public void testToGraphBasic() {
        int R = 2;
        int C = 3;
        boolean[][] isObstacle = {
            {false, false, true},
            {false, false, false}
        };

        Graph g = Grid4neighborAlgorithms.toGraph(R, C, isObstacle);
        assertNotNull(g);
        assertEquals(6, g.N);

        // Check edges for (0,0) [id=0] -> (0,1) [id=1] and (1,0) [id=3]
        assertTrue(contains(g.adj[0], 1));
        assertTrue(contains(g.adj[0], 3));
        assertEquals(2, g.adj[0].size());

        // (0,1) [id=1]: (0,2) is obstacle, so connected to (0,0) [id=0] and (1,1) [id=4]
        assertTrue(contains(g.adj[1], 0));
        assertTrue(contains(g.adj[1], 4));
        assertFalse(contains(g.adj[1], 2));
        assertEquals(2, g.adj[1].size());

        // (0,2) [id=2]: is obstacle, degree should be 0
        assertEquals(0, g.adj[2].size());

        // Total edges M
        // Edges: (0,0)-(0,1), (0,0)-(1,0), (0,1)-(1,1), (1,0)-(1,1), (1,1)-(1,2) -> 5 edges
        assertEquals(5, g.M);
    }

    @Test
    public void testToGraphWithWallsAndObstacles() {
        int R = 2;
        int C = 2;
        boolean[][] blockedRight = {{true}, {false}}; // wall between (0,0) and (0,1)
        boolean[][] blockedDown = {{false, true}};   // wall between (0,1) and (1,1)
        boolean[][] isObstacle = {{false, false}, {true, false}}; // (1,0) is obstacle

        Graph g = Grid4neighborAlgorithms.toGraph(R, C, blockedRight, blockedDown, isObstacle);
        assertEquals(4, g.N);

        // Cell (0,0) [0]: right is blocked by wall, down is (1,0) which is obstacle. Degree 0.
        assertEquals(0, g.adj[0].size());

        // Cell (0,1) [1]: left is blocked by wall, down is blocked by wall. Degree 0.
        assertEquals(0, g.adj[1].size());

        // Cell (1,0) [2]: is obstacle. Degree 0.
        assertEquals(0, g.adj[2].size());

        // Cell (1,1) [3]: left is (1,0) obstacle, up is wall. Degree 0.
        assertEquals(0, g.adj[3].size());

        assertEquals(0, g.M);
    }

    @Test
    public void testToGraphEmptyAndOverloads() {
        Graph g0 = Grid4neighborAlgorithms.toGraph(0, 0);
        assertEquals(0, g0.N);
        assertEquals(0, g0.M);

        Graph g1 = Grid4neighborAlgorithms.toGraph(1, 1);
        assertEquals(1, g1.N);
        assertEquals(0, g1.M);

        Graph g3x3 = Grid4neighborAlgorithms.toGraph(3, 3);
        assertEquals(9, g3x3.N);
        // 3x3 grid without obstacles/walls has 2 * 3 * 2 = 12 undirected edges
        assertEquals(12, g3x3.M);

        Graph gWalls = Grid4neighborAlgorithms.toGraph(2, 2, new boolean[2][1], new boolean[1][2]);
        assertEquals(4, gWalls.N);
        assertEquals(4, gWalls.M);
    }

    private boolean contains(library.util.collections.IntArrayList list, int val) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == val) return true;
        }
        return false;
    }

    @Test
    public void testManualSmallGridBFS() {
        int R = 2;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        blockedRight[0][0] = true; // wall between (0,0) and (0,1)
        blockedRight[1][1] = true; // wall between (1,1) and (1,2)
        blockedDown[0][1] = true; // wall between (0,1) and (1,1)

        int[][] dist = Grid4neighborAlgorithms.bfs(R, C, blockedRight, blockedDown, 0, 0);

        assertEquals(0, dist[0][0]);
        assertEquals(1, dist[1][0]);
        assertEquals(2, dist[1][1]);
        assertEquals(Integer.MAX_VALUE, dist[0][1]);
        assertEquals(Integer.MAX_VALUE, dist[0][2]);
        assertEquals(Integer.MAX_VALUE, dist[1][2]);
    }

    @Test
    public void testRestoreShortestPathFromBfsDistances() {
        boolean[][] isObstacle = {
                {false, false, true, false},
                {true, false, true, false},
                {false, false, false, false}
        };
        int[][] dist = Grid4neighborAlgorithms.bfs(3, 4, isObstacle, 0, 0);

        assertArrayEquals(new int[][]{
                {0, 0}, {0, 1}, {1, 1}, {2, 1}, {2, 2}, {2, 3}
        }, Grid4neighborAlgorithms.restoreShortestPath(dist, 0, 0, 2, 3));
    }

    @Test
    public void testRestoreShortestPathEndpointCases() {
        boolean[][] isObstacle = {
                {false, true},
                {true, false}
        };
        int[][] dist = Grid4neighborAlgorithms.bfs(2, 2, isObstacle, 0, 0);

        assertArrayEquals(new int[][]{{0, 0}},
                Grid4neighborAlgorithms.restoreShortestPath(dist, 0, 0, 0, 0));
        assertNull(Grid4neighborAlgorithms.restoreShortestPath(dist, 0, 0, 1, 1));
        assertNull(Grid4neighborAlgorithms.restoreShortestPath(dist, 0, 0, -1, 0));
    }

    @Test
    public void testFindPathWithMultipleSourcesAndTargets() {
        int[][] sources = {{0, 0}, {2, 0}};
        int[][] targets = {{0, 2}, {2, 2}};

        assertArrayEquals(new int[][]{{0, 0}, {0, 1}, {0, 2}},
                Grid4neighborAlgorithms.findPath(3, 3, sources, targets));

        boolean[][] isObstacle = {
                {false, true, false},
                {false, false, false},
                {false, false, false}
        };
        assertArrayEquals(new int[][]{{2, 0}, {2, 1}, {2, 2}},
                Grid4neighborAlgorithms.findPath(3, 3, null, null, isObstacle, sources, targets));
    }

    @Test
    public void testFindPathWithMultipleSourcesAndTargetsUnreachable() {
        boolean[][] blockedRight = {{true, false}};

        assertNull(Grid4neighborAlgorithms.findPath(1, 3, blockedRight, new boolean[0][3],
                new int[][]{{0, 0}}, new int[][]{{0, 2}}));
        assertNull(Grid4neighborAlgorithms.findPath(1, 3, new int[0][2], new int[][]{{0, 2}}));
    }

    @Test
    public void testWeightedFindPathWithMultipleSourcesAndTargets() {
        long[][] longWeight = {{100, 1, 100, 1, 1}};
        int[][] sources = {{0, 0}, {0, 4}};
        int[][] targets = {{0, 1}, {0, 3}};

        assertArrayEquals(new int[][]{{0, 4}, {0, 3}},
                Grid4neighborAlgorithms.findPath(1, 5, longWeight, sources, targets));

        int[][] intWeight = {{100, 1, 100, 1, 1}};
        assertArrayEquals(new int[][]{{0, 4}, {0, 3}},
                Grid4neighborAlgorithms.findPath(1, 5, intWeight, sources, targets));
    }

    @Test
    public void testBfsRejectsInvalidWallDimensions() {
        int R = 3;
        int C = 4;
        boolean[][] validV = new boolean[R][C - 1];
        boolean[][] validH = new boolean[R - 1][C];

        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, new boolean[R - 1][C - 1], validH, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, new boolean[R][C], validH, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, validV, new boolean[R][C], 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, validV, new boolean[R - 1][C - 1], 0, 0));

        boolean[][] vWithNullRow = new boolean[R][];
        vWithNullRow[0] = new boolean[C - 1];
        vWithNullRow[2] = new boolean[C - 1];
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, vWithNullRow, validH, 0, 0));

        boolean[][] hWithNullRow = new boolean[R - 1][];
        hWithNullRow[1] = new boolean[C];
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfs(R, C, validV, hWithNullRow, 0, 0));
    }

    @Test
    public void testManualSmallGridIcefloor() {
        int R = 2;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        blockedRight[0][0] = true;
        blockedRight[1][1] = true;
        blockedDown[0][1] = true;

        int[][][] dist = Grid4neighborAlgorithms.bfsIcefloor(R, C, blockedRight, blockedDown, 0, 0);
        assertNotNull(dist);
        assertEquals(4, dist.length);
        assertEquals(R, dist[0].length);
        assertEquals(C, dist[0][0].length);
    }

    @Test
    public void testBfsIcefloorRejectsInvalidWallDimensions() {
        int R = 3;
        int C = 4;
        boolean[][] validV = new boolean[R][C - 1];
        boolean[][] validH = new boolean[R - 1][C];

        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, new boolean[R - 1][C - 1], validH, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, new boolean[R][C], validH, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, validV, new boolean[R][C], 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, validV, new boolean[R - 1][C - 1], 0, 0));

        boolean[][] vWithNullRow = new boolean[R][];
        vWithNullRow[0] = new boolean[C - 1];
        vWithNullRow[2] = new boolean[C - 1];
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, vWithNullRow, validH, 0, 0));

        boolean[][] hWithNullRow = new boolean[R - 1][];
        hWithNullRow[1] = new boolean[C];
        assertThrows(IllegalArgumentException.class,
                () -> Grid4neighborAlgorithms.bfsIcefloor(R, C, validV, hWithNullRow, 0, 0));
    }

    @Test
    public void testEquivalenceWithGrid2D() {
        Random rnd = new Random(12345);
        int R = 15;
        int C = 15;

        for (int t = 0; t < 10; t++) {
            char[][] map = new char[R][C];
            boolean[][] isObstacle = new boolean[R][C];

            for (int i = 0; i < R; i++) {
                for (int j = 0; j < C; j++) {
                    if (rnd.nextDouble() < 0.25) {
                        map[i][j] = '#';
                        isObstacle[i][j] = true;
                    } else {
                        map[i][j] = '.';
                        isObstacle[i][j] = false;
                    }
                }
            }

            int si = rnd.nextInt(R);
            int sj = rnd.nextInt(C);
            map[si][sj] = '.';
            isObstacle[si][sj] = false;

            Grid2D g2d = new Grid2D(map);
            int[][] d2d = g2d.bfs(si, sj);
            int[][][] d2dIce = g2d.bfs_icefloor(si, sj);

            int[][] d4n = Grid4neighborAlgorithms.bfs(R, C, null, null, isObstacle, si, sj);
            int[][] d4nWithoutWalls = Grid4neighborAlgorithms.bfs(R, C, isObstacle, si, sj);
            int[][][] d4nIce = Grid4neighborAlgorithms.bfsIcefloor(R, C, null, null, isObstacle, si, sj);

            for (int i = 0; i < R; i++) {
                for (int j = 0; j < C; j++) {
                    assertEquals(d2d[i][j], d4n[i][j]);
                    assertEquals(d2d[i][j], d4nWithoutWalls[i][j]);
                }
            }

            for (int dir = 0; dir < 4; dir++) {
                int grid4Direction = directionIndex(
                        Grid2D.dh[dir], Grid2D.dw[dir],
                        Grid4neighborAlgorithms.dh, Grid4neighborAlgorithms.dw);
                for (int i = 0; i < R; i++) {
                    for (int j = 0; j < C; j++) {
                        assertEquals(d2dIce[dir][i][j], d4nIce[grid4Direction][i][j]);
                    }
                }
            }
        }
    }

    /**
     * 固定 seed で生成した障害物グリッドについて、滑る床の距離を独立な Dijkstra 法の結果と比較する。
     * 失敗時には seed、ケース番号、盤面、始点、方向、座標、期待値、実値を出力する。
     *
     * @complexity O(T · R · C log(R · C))（T は生成するケース数）
     */
    // 未テスト
    @Test
    public void testIcefloorRandomizedAgainstDijkstra() {
        long seed = 0x5EED_01BF_5L;
        Random random = new Random(seed);
        int testCases = 200;

        for (int testCase = 0; testCase < testCases; testCase++) {
            int rows = 1 + random.nextInt(8);
            int columns = 1 + random.nextInt(8);
            char[][] map = new char[rows][columns];
            boolean[][] isObstacle = new boolean[rows][columns];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < columns; c++) {
                    isObstacle[r][c] = random.nextDouble() < 0.3;
                    map[r][c] = isObstacle[r][c] ? '#' : '.';
                }
            }

            int startRow = random.nextInt(rows);
            int startColumn = random.nextInt(columns);
            isObstacle[startRow][startColumn] = false;
            map[startRow][startColumn] = '.';
            String input = randomIcefloorInput(seed, testCase, map, startRow, startColumn);

            int[][][] expectedGrid2D = dijkstraIcefloor(
                    isObstacle, startRow, startColumn, Grid2D.dh, Grid2D.dw);
            int[][][] actualGrid2D = new Grid2D(map).bfs_icefloor(startRow, startColumn);
            assertIcefloorDistancesEqual(expectedGrid2D, actualGrid2D, input, "Grid2D.bfs_icefloor");

            int[][][] expectedGrid4 = dijkstraIcefloor(
                    isObstacle, startRow, startColumn,
                    Grid4neighborAlgorithms.dh, Grid4neighborAlgorithms.dw);
            int[][][] actualGrid4 = Grid4neighborAlgorithms.bfsIcefloor(
                    rows, columns, null, null, isObstacle, startRow, startColumn);
            assertIcefloorDistancesEqual(expectedGrid4, actualGrid4, input, "Grid4neighborAlgorithms.bfsIcefloor");
        }
    }

    @Test
    public void testFindPathStandard() {
        int R = 3;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        blockedDown[0][1] = true;
        blockedDown[1][1] = true;
        blockedRight[1][0] = true;

        int[][] path = Grid4neighborAlgorithms.findShortestPath(R, C, blockedRight, blockedDown, 0, 1, 2, 1);
        assertNotNull(path);

        assertEquals(5, path.length);
        assertArrayEquals(new int[]{0, 1}, path[0]);
        assertArrayEquals(new int[]{2, 1}, path[4]);
        assertTrue(
            (path[1][0] == 0 && path[1][1] == 0 && path[2][0] == 1 && path[2][1] == 0 && path[3][0] == 2 && path[3][1] == 0) ||
            (path[1][0] == 0 && path[1][1] == 2 && path[2][0] == 1 && path[2][1] == 2 && path[3][0] == 2 && path[3][1] == 2),
            "Path should go via (0,0) or (0,2)"
        );

        int[][] findPathResult = Grid4neighborAlgorithms.findPath(R, C, blockedRight, blockedDown, 0, 1, 2, 1);
        assertArrayEquals(path, findPathResult);

        blockedRight[0][0] = true;
        blockedDown[0][0] = true;
        int[][] unreached = Grid4neighborAlgorithms.findShortestPath(R, C, blockedRight, blockedDown, 0, 0, 2, 2);
        assertNull(unreached);
        assertNull(Grid4neighborAlgorithms.findPath(R, C, blockedRight, blockedDown, 0, 0, 2, 2));

        int[][] trivialPath = Grid4neighborAlgorithms.findShortestPath(R, C, blockedRight, blockedDown, 1, 1, 1, 1);
        assertNotNull(trivialPath);
        assertEquals(1, trivialPath.length);
        assertArrayEquals(new int[]{1, 1}, trivialPath[0]);
    }

    @Test
    public void testFindPathIcefloor() {
        int R = 3;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        int[][] pathRight = Grid4neighborAlgorithms.findPathIcefloor(R, C, blockedRight, blockedDown, 0, 0, 0, 2);
        assertNotNull(pathRight);
        assertEquals(3, pathRight.length);
        assertArrayEquals(new int[]{0, 0}, pathRight[0]);
        assertArrayEquals(new int[]{0, 1}, pathRight[1]);
        assertArrayEquals(new int[]{0, 2}, pathRight[2]);

        blockedRight[0][1] = true;
        int[][] pathWithWall = Grid4neighborAlgorithms.findPathIcefloor(R, C, blockedRight, blockedDown, 0, 0, 0, 1);
        assertNotNull(pathWithWall);
        assertEquals(2, pathWithWall.length);
        assertArrayEquals(new int[]{0, 0}, pathWithWall[0]);
        assertArrayEquals(new int[]{0, 1}, pathWithWall[1]);

        blockedRight[0][0] = true;
        blockedDown[0][0] = true;
        int[][] unreachable = Grid4neighborAlgorithms.findPathIcefloor(R, C, blockedRight, blockedDown, 0, 0, 2, 2);
        assertNull(unreachable);
    }

    @Test
    public void testIcefloorWithStopping() {
        int R = 3;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        // Let's test standard findPathIcefloor vs findPathIcefloorWithStopping.
        // Under standard sliding, if we start at (0,0) and slide right, we MUST slide all the way to (0,2).
        // If the target is (1,1), under standard sliding, we cannot reach it directly because sliding right goes to (0,2),
        // sliding down from (0,2) goes to (2,2), sliding left from (2,2) goes to (2,0), etc.
        // But with stopping allowed:
        // We can slide right from (0,0), stop at (0,1), then turn down and slide to (1,1)!

        int[][] stdPath = Grid4neighborAlgorithms.findPathIcefloor(R, C, blockedRight, blockedDown, 0, 0, 1, 1);
        // Under standard rules, (1,1) is unreachable from (0,0) on a 3x3 empty grid!
        assertNull(stdPath);

        // With stopping allowed:
        int[][] stopPath = Grid4neighborAlgorithms.findPathIcefloorWithStopping(R, C, blockedRight, blockedDown, 0, 0, 1, 1);
        assertNotNull(stopPath);

        // Expected path is either:
        // Option 1: (0,0) -> (0,1) -> (1,1)
        // Option 2: (0,0) -> (1,0) -> (1,1)
        assertEquals(3, stopPath.length);
        assertArrayEquals(new int[]{0, 0}, stopPath[0]);
        assertArrayEquals(new int[]{1, 1}, stopPath[2]);
        assertTrue(
            (stopPath[1][0] == 0 && stopPath[1][1] == 1) ||
            (stopPath[1][0] == 1 && stopPath[1][1] == 0),
            "Intermediate cell should be (0,1) or (1,0)"
        );
    }

    @Test
    public void testBfsWithoutWalls() {
        int R = 3;
        int C = 3;
        boolean[][] isObstacle = new boolean[R][C];
        isObstacle[1][1] = true;

        // BFS with isObstacle only
        int[][] distObstacle = Grid4neighborAlgorithms.bfs(R, C, isObstacle, 0, 0);
        int[][] distExpected = Grid4neighborAlgorithms.bfs(R, C, null, null, isObstacle, 0, 0);
        assertArrayEquals(distExpected, distObstacle);

        assertEquals(0, distObstacle[0][0]);
        assertEquals(1, distObstacle[0][1]);
        assertEquals(1, distObstacle[1][0]);
        assertEquals(2, distObstacle[0][2]);
        assertEquals(Integer.MAX_VALUE, distObstacle[1][1]);
        assertEquals(4, distObstacle[2][2]);

        // BFS with null isObstacle (no obstacles or walls)
        int[][] distNoObstacles = Grid4neighborAlgorithms.bfs(R, C, (boolean[][]) null, 0, 0);
        int[][] distNoObstaclesExpected = Grid4neighborAlgorithms.bfs(R, C, null, null, (boolean[][]) null, 0, 0);
        assertArrayEquals(distNoObstaclesExpected, distNoObstacles);

        assertEquals(0, distNoObstacles[0][0]);
        assertEquals(1, distNoObstacles[0][1]);
        assertEquals(2, distNoObstacles[1][1]);
        assertEquals(4, distNoObstacles[2][2]);
    }

    @Test
    public void testVertexWeightedDijkstra() {
        int R = 2;
        int C = 3;
        long INF = Long.MAX_VALUE / 2;
        long[][] weight = {
            {2, 10, INF},
            {3,  4,  5}
        };

        // From (0,0) to all cells without walls
        long[][] dist = Grid4neighborAlgorithms.dijkstra(R, C, weight, 0, 0);

        // Path to (0,0): cost 2
        assertEquals(2, dist[0][0]);
        // Path (0,0) -> (1,0): cost 2 + 3 = 5
        assertEquals(5, dist[1][0]);
        // Path (0,0) -> (1,0) -> (1,1): cost 5 + 4 = 9 (cheaper than (0,0)->(0,1) which costs 2 + 10 = 12)
        assertEquals(9, dist[1][1]);
        // Path (0,0) -> (0,1): cost 2 + 10 = 12
        assertEquals(12, dist[0][1]);
        // Path (0,0) -> (1,0) -> (1,1) -> (1,2): cost 9 + 5 = 14
        assertEquals(14, dist[1][2]);
        // Cell (0,2) is INF (obstacle), unreachable
        assertEquals(INF, dist[0][2]);

        // Test with int[][] weight
        int[][] intWeight = {
            {2, 10, Integer.MAX_VALUE},
            {3,  4,  5}
        };
        long[][] distInt = Grid4neighborAlgorithms.dijkstra(R, C, intWeight, 0, 0);

        assertEquals(2, distInt[0][0]);
        assertEquals(5, distInt[1][0]);
        assertEquals(9, distInt[1][1]);
        assertEquals(12, distInt[0][1]);
        assertEquals(14, distInt[1][2]);
        assertEquals(INF, distInt[0][2]);

        // Test with walls
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];
        // Wall between (1,0) and (1,1)
        blockedRight[1][0] = true;

        long[][] distWall = Grid4neighborAlgorithms.dijkstra(R, C, blockedRight, blockedDown, weight, 0, 0);
        // Path to (1,1) must go (0,0) -> (0,1) -> (1,1) with cost 2 + 10 + 4 = 16
        assertEquals(16, distWall[1][1]);
        // Path to (1,2) must go (0,0) -> (0,1) -> (1,1) -> (1,2) with cost 16 + 5 = 21
        assertEquals(21, distWall[1][2]);
    }

    @Test
    public void testVertexWeightedFindPath() {
        int R = 2;
        int C = 3;
        long INF = Long.MAX_VALUE / 2;
        long[][] weight = {
            {2, 10, INF},
            {3,  4,  5}
        };

        // Path (0,0) -> (1,2): should be (0,0) -> (1,0) -> (1,1) -> (1,2)
        int[][] path = Grid4neighborAlgorithms.findPath(R, C, weight, 0, 0, 1, 2);
        assertNotNull(path);
        assertEquals(4, path.length);
        assertArrayEquals(new int[]{0, 0}, path[0]);
        assertArrayEquals(new int[]{1, 0}, path[1]);
        assertArrayEquals(new int[]{1, 1}, path[2]);
        assertArrayEquals(new int[]{1, 2}, path[3]);

        // Path to unreachable cell (0,2)
        int[][] unreachedPath = Grid4neighborAlgorithms.findPath(R, C, weight, 0, 0, 0, 2);
        assertNull(unreachedPath);

        // Test path with int[][] weight and walls
        int[][] intWeight = {
            {2, 10, Integer.MAX_VALUE},
            {3,  4,  5}
        };
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];
        blockedRight[1][0] = true; // Wall between (1,0) and (1,1)

        int[][] pathWall = Grid4neighborAlgorithms.findPath(R, C, blockedRight, blockedDown, intWeight, 0, 0, 1, 2);
        assertNotNull(pathWall);
        assertEquals(4, pathWall.length);
        assertArrayEquals(new int[]{0, 0}, pathWall[0]);
        assertArrayEquals(new int[]{0, 1}, pathWall[1]);
        assertArrayEquals(new int[]{1, 1}, pathWall[2]);
        assertArrayEquals(new int[]{1, 2}, pathWall[3]);
    }

    @Test
    public void testVertexWeightedEdgeCases() {
        int R = 2;
        int C = 2;
        long INF = Long.MAX_VALUE / 2;
        long[][] weight = {
            {INF, 1},
            {1,   1}
        };

        // Start cell is obstacle (INF)
        long[][] distBlockedStart = Grid4neighborAlgorithms.dijkstra(R, C, weight, 0, 0);
        assertEquals(INF, distBlockedStart[0][0]);
        assertEquals(INF, distBlockedStart[0][1]);

        // Out of bounds start
        long[][] distOOB = Grid4neighborAlgorithms.dijkstra(R, C, weight, -1, 0);
        assertEquals(INF, distOOB[0][0]);

        // Null weight
        long[][] distNull = Grid4neighborAlgorithms.dijkstra(R, C, (long[][]) null, 0, 0);
        assertEquals(INF, distNull[0][0]);
    }

    @Test
    public void testIsAdjacentWithWalls() {
        int R = 3;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];

        blockedRight[0][0] = true; // wall between (0,0) and (0,1)
        blockedDown[0][1] = true; // wall between (0,1) and (1,1)

        // Blocked by vertical wall (0,0) <-> (0,1)
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 0, 1));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 1, 0, 0));

        // Open (0,0) <-> (1,0)
        assertTrue(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 1, 0));

        // Blocked by horizontal wall (0,1) <-> (1,1)
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 1, 1, 1));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 1, 1, 0, 1));

        // Non-adjacent distance > 1
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 0, 2));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 2, 0));

        // Diagonal
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 1, 1));

        // Same point
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, 0, 0, 0, 0));
    }

    @Test
    public void testIsAdjacentWithBoundsAndObstacles() {
        int R = 3;
        int C = 3;
        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];
        boolean[][] isObstacle = new boolean[R][C];
        isObstacle[1][1] = true;

        // Out of bounds checks
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, -1, 0, 0, 0));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 0, 0, -1, 0));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 0, 0, 3, 0));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 0, 3, 0, 2));

        // Obstacle checks
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 0, 1, 1, 1));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 1, 1, 1, 0));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, isObstacle, 0, 1, 1, 1));

        // Valid movement avoiding obstacle
        assertTrue(Grid4neighborAlgorithms.isAdjacent(R, C, isObstacle, 0, 0, 0, 1));
        assertTrue(Grid4neighborAlgorithms.isAdjacent(R, C, blockedRight, blockedDown, isObstacle, 0, 0, 0, 1));
    }

    @Test
    public void testBfs01SmallGridEdgeWeights() {
        int R = 2;
        int C = 3;
        int[][][] edgeWeight = new int[R][C][4];
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                Arrays.fill(edgeWeight[r][c], 1);
            }
        }
        // (0,0) -> (0,1) dir=1 (右): 0
        edgeWeight[0][0][1] = 0;
        edgeWeight[0][1][3] = 0; // (0,1) -> (0,0) dir=3 (左)
        // (0,1) -> (1,1) dir=0 (下): 0
        edgeWeight[0][1][0] = 0;
        edgeWeight[1][1][2] = 0; // (1,1) -> (0,1) dir=2 (上)

        int[][] dist = Grid4neighborAlgorithms.bfs01(R, C, edgeWeight, 0, 0);

        assertEquals(0, dist[0][0]);
        assertEquals(0, dist[0][1]);
        assertEquals(1, dist[0][2]);
        assertEquals(1, dist[1][0]);
        assertEquals(0, dist[1][1]);
        assertEquals(1, dist[1][2]);

        boolean[][] blockedRight = new boolean[R][C - 1];
        boolean[][] blockedDown = new boolean[R - 1][C];
        blockedDown[0][1] = true; // Wall between (0,1) and (1,1)

        int[][] distWalls = Grid4neighborAlgorithms.bfs01(R, C, blockedRight, blockedDown, edgeWeight, 0, 0);
        assertEquals(0, distWalls[0][0]);
        assertEquals(0, distWalls[0][1]);
        assertEquals(1, distWalls[0][2]);
        assertEquals(2, distWalls[1][2]);
        assertEquals(2, distWalls[1][1]);
    }

    @Test
    public void testFindPath01RandomTieBreaking() {
        int R = 3;
        int C = 3;
        int[][][] edgeWeight = new int[R][C][4]; // all 0 weights

        java.util.Set<String> seenIntermediateCells = new java.util.HashSet<>();
        for (int seed = 0; seed < 100; seed++) {
            Random rnd = new Random(seed);
            int[][] path = Grid4neighborAlgorithms.findPath01(R, C, edgeWeight, 0, 0, 2, 2, rnd);
            assertNotNull(path);
            assertTrue(path.length >= 5);
            assertArrayEquals(new int[]{0, 0}, path[0]);
            assertArrayEquals(new int[]{2, 2}, path[path.length - 1]);
            seenIntermediateCells.add(path[1][0] + "," + path[1][1]);
        }
        assertTrue(seenIntermediateCells.size() > 1, "Random tie breaking should produce multiple path branches");
    }

    @Test
    public void testBfs01RandomizedAgainstDijkstra() {
        Random rnd = new Random(54321);
        int R = 8;
        int C = 8;

        for (int t = 0; t < 20; t++) {
            boolean[][] isObstacle = new boolean[R][C];
            int[][][] edgeWeight = new int[R][C][4];
            for (int r = 0; r < R; r++) {
                for (int c = 0; c < C; c++) {
                    isObstacle[r][c] = rnd.nextDouble() < 0.2;
                    for (int dir = 0; dir < 4; dir++) {
                        edgeWeight[r][c][dir] = rnd.nextInt(2);
                    }
                }
            }

            int si = rnd.nextInt(R);
            int sj = rnd.nextInt(C);
            isObstacle[si][sj] = false;

            int[][] dist01 = Grid4neighborAlgorithms.bfs01(R, C, isObstacle, edgeWeight, si, sj);

            int[][] distRef = new int[R][C];
            for (int[] row : distRef) Arrays.fill(row, Integer.MAX_VALUE);
            PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
            distRef[si][sj] = 0;
            pq.add(new int[]{0, si, sj});
            while (!pq.isEmpty()) {
                int[] curr = pq.poll();
                int d = curr[0];
                int r = curr[1];
                int c = curr[2];
                if (d > distRef[r][c]) continue;
                int[] dh = Grid4neighborAlgorithms.dh;
                int[] dw = Grid4neighborAlgorithms.dw;
                for (int dir = 0; dir < 4; dir++) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    if (nr >= 0 && nr < R && nc >= 0 && nc < C && !isObstacle[nr][nc]) {
                        int w = edgeWeight[r][c][dir];
                        if (distRef[r][c] + w < distRef[nr][nc]) {
                            distRef[nr][nc] = distRef[r][c] + w;
                            pq.add(new int[]{distRef[nr][nc], nr, nc});
                        }
                    }
                }
            }

            for (int r = 0; r < R; r++) {
                for (int c = 0; c < C; c++) {
                    assertEquals(distRef[r][c], dist01[r][c]);
                }
            }
        }
    }

    @Test
    public void testIsAdjacentNullWalls() {
        int R = 2;
        int C = 2;
        assertTrue(Grid4neighborAlgorithms.isAdjacent(R, C, null, null, 0, 0, 0, 1));
        assertTrue(Grid4neighborAlgorithms.isAdjacent(R, C, null, null, 0, 0, 1, 0));
        assertFalse(Grid4neighborAlgorithms.isAdjacent(R, C, null, null, 0, 0, 1, 1));
    }

    /**
     * 方向ベクトル (deltaRow, deltaColumn) の方向番号を返す。
     *
     * @param deltaRow 行インデックスの変化量
     * @param deltaColumn 列インデックスの変化量
     * @param deltaRows 各方向の行インデックスの変化量
     * @param deltaColumns 各方向の列インデックスの変化量
     * @return 指定した方向ベクトルの添字
     * @throws IllegalArgumentException 指定した方向ベクトルが存在しない場合
     * @complexity O(deltaRows.length)
     */
    // 未テスト
    private static int directionIndex(
            int deltaRow, int deltaColumn, int[] deltaRows, int[] deltaColumns) {
        for (int direction = 0; direction < deltaRows.length; direction++) {
            if (deltaRows[direction] == deltaRow && deltaColumns[direction] == deltaColumn) {
                return direction;
            }
        }
        throw new IllegalArgumentException(
                "direction not found: (" + deltaRow + ", " + deltaColumn + ")");
    }

    /**
     * 障害物グリッド上の滑る床の最短距離を Dijkstra 法で計算する。
     * 移動継続辺の重みを0、停止位置での方向転換辺の重みを1、各初期状態の距離を1とする。
     *
     * @param isObstacle true のマスを通行不能とする R x C 配列
     * @param startRow 始点の行インデックス
     * @param startColumn 始点の列インデックス
     * @param deltaRows 各方向の行インデックスの変化量
     * @param deltaColumns 各方向の列インデックスの変化量
     * @return direction、row、column の順に添字を持つ最短距離
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    private static int[][][] dijkstraIcefloor(
            boolean[][] isObstacle, int startRow, int startColumn,
            int[] deltaRows, int[] deltaColumns) {
        int rows = isObstacle.length;
        int columns = isObstacle[0].length;
        int infinity = Integer.MAX_VALUE / 3;
        int[][][] distance = new int[4][rows][columns];
        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));

        for (int direction = 0; direction < 4; direction++) {
            for (int row = 0; row < rows; row++) {
                Arrays.fill(distance[direction][row], infinity);
            }
            distance[direction][startRow][startColumn] = 1;
            queue.add(new int[] {1, direction, startRow, startColumn});
        }

        while (!queue.isEmpty()) {
            int[] state = queue.poll();
            int currentDistance = state[0];
            int direction = state[1];
            int row = state[2];
            int column = state[3];
            if (currentDistance != distance[direction][row][column]) {
                continue;
            }

            int nextRow = row + deltaRows[direction];
            int nextColumn = column + deltaColumns[direction];
            boolean canMove = 0 <= nextRow && nextRow < rows
                    && 0 <= nextColumn && nextColumn < columns
                    && !isObstacle[nextRow][nextColumn];
            if (canMove) {
                if (currentDistance < distance[direction][nextRow][nextColumn]) {
                    distance[direction][nextRow][nextColumn] = currentDistance;
                    queue.add(new int[] {currentDistance, direction, nextRow, nextColumn});
                }
            } else {
                for (int nextDirection = 0; nextDirection < 4; nextDirection++) {
                    if (nextDirection == direction) {
                        continue;
                    }
                    int nextDistance = currentDistance + 1;
                    if (nextDistance < distance[nextDirection][row][column]) {
                        distance[nextDirection][row][column] = nextDistance;
                        queue.add(new int[] {nextDistance, nextDirection, row, column});
                    }
                }
            }
        }
        return distance;
    }

    /**
     * 2つの滑る床距離配列が等しいことを検査する。
     *
     * @param expected 期待する距離
     * @param actual 実際の距離
     * @param input 失敗時に表示する入力
     * @param implementation 実装名
     * @complexity O(R · C)
     */
    // 未テスト
    private static void assertIcefloorDistancesEqual(
            int[][][] expected, int[][][] actual, String input, String implementation) {
        for (int direction = 0; direction < 4; direction++) {
            for (int row = 0; row < expected[direction].length; row++) {
                for (int column = 0; column < expected[direction][row].length; column++) {
                    if (expected[direction][row][column] != actual[direction][row][column]) {
                        fail(input
                                + System.lineSeparator() + "implementation=" + implementation
                                + System.lineSeparator() + "direction=" + direction
                                + System.lineSeparator() + "cell=(" + row + "," + column + ")"
                                + System.lineSeparator() + "expectedOutput=" + expected[direction][row][column]
                                + System.lineSeparator() + "actualOutput=" + actual[direction][row][column]);
                    }
                }
            }
        }
    }

    /**
     * ランダムテストの入力を再現可能な文字列として返す。
     *
     * @param seed 乱数 seed
     * @param testCase ケース番号
     * @param map 障害物グリッド
     * @param startRow 始点の行インデックス
     * @param startColumn 始点の列インデックス
     * @return seed、ケース番号、寸法、始点、盤面を含む文字列
     * @complexity O(R · C)
     */
    // 未テスト
    private static String randomIcefloorInput(
            long seed, int testCase, char[][] map, int startRow, int startColumn) {
        StringBuilder result = new StringBuilder();
        result.append("seed=").append(seed)
                .append(System.lineSeparator()).append("testCase=").append(testCase)
                .append(System.lineSeparator()).append("R=").append(map.length)
                .append(System.lineSeparator()).append("C=").append(map[0].length)
                .append(System.lineSeparator()).append("start=(")
                .append(startRow).append(',').append(startColumn).append(')')
                .append(System.lineSeparator()).append("map:");
        for (char[] row : map) {
            result.append(System.lineSeparator()).append(row);
        }
        return result.toString();
    }

    @Test
    public void testGenerateRandomGridBasic() {
        int[] sizes = {1, 2, 3, 5, 10, 20};
        for (int N : sizes) {
            Random rnd = new Random(12345 + N);
            boolean[][] grid = Grid4neighborAlgorithms.generateRandomGrid(N, rnd);
            assertNotNull(grid);
            assertEquals(N, grid.length);
            for (int r = 0; r < N; r++) {
                assertEquals(N, grid[r].length);
            }
            assertFalse(grid[0][0], "Start (0,0) must be an empty cell");
            assertFalse(grid[N - 1][N - 1], "Goal (N-1, N-1) must be an empty cell");
        }
    }

    @Test
    public void testGenerateRandomGridConnectivity() {
        int[] sizes = {2, 3, 5, 10, 15, 20};
        for (int N : sizes) {
            for (int trial = 0; trial < 10; trial++) {
                Random rnd = new Random(N * 100 + trial);
                boolean[][] grid = Grid4neighborAlgorithms.generateRandomGrid(N, rnd);

                int[][] dist = Grid4neighborAlgorithms.bfs(N, N, grid, 0, 0);

                int totalEmpty = 0;
                int reachableEmpty = 0;
                for (int r = 0; r < N; r++) {
                    for (int c = 0; c < N; c++) {
                        if (!grid[r][c]) {
                            totalEmpty++;
                            if (dist[r][c] != Integer.MAX_VALUE) {
                                reachableEmpty++;
                            }
                        }
                    }
                }

                assertEquals(totalEmpty, reachableEmpty,
                        "All empty cells must form a single connected component for N=" + N + ", trial=" + trial);
            }
        }
    }

    @Test
    public void testGenerateRandomGridDeterminismAndOverloads() {
        int N = 8;
        long seed = 987654321L;

        boolean[][] grid1 = Grid4neighborAlgorithms.generateRandomGrid(N, new Random(seed));
        boolean[][] grid2 = Grid4neighborAlgorithms.generateRandomGrid(N, new Random(seed));

        for (int r = 0; r < N; r++) {
            assertArrayEquals(grid1[r], grid2[r]);
        }

        boolean[][] gridNoArg1 = Grid4neighborAlgorithms.generateRandomGrid(N);
        assertNotNull(gridNoArg1);
        assertEquals(N, gridNoArg1.length);
    }

    @Test
    public void testGenerateRandomGridEdgeCases() {
        boolean[][] empty = Grid4neighborAlgorithms.generateRandomGrid(0, new Random());
        assertEquals(0, empty.length);

        boolean[][] size1 = Grid4neighborAlgorithms.generateRandomGrid(1, new Random());
        assertEquals(1, size1.length);
        assertEquals(1, size1[0].length);
        assertFalse(size1[0][0]);
    }

    @Test
    public void testGenerateRandomGridCustomFixedEmpty() {
        int N = 5;
        int[][] fixed = {{0, 1}, {2, 2}, {4, 3}};
        Random rnd = new Random(42);

        boolean[][] grid = Grid4neighborAlgorithms.generateRandomGrid(N, fixed, rnd);
        assertNotNull(grid);
        assertFalse(grid[0][1], "Fixed cell (0,1) must be empty");
        assertFalse(grid[2][2], "Fixed cell (2,2) must be empty");
        assertFalse(grid[4][3], "Fixed cell (4,3) must be empty");

        // Verify connectivity starting from one of the fixed empty cells
        int[][] dist = Grid4neighborAlgorithms.bfs(N, N, grid, 0, 1);
        int totalEmpty = 0;
        int reachableEmpty = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!grid[r][c]) {
                    totalEmpty++;
                    if (dist[r][c] != Integer.MAX_VALUE) {
                        reachableEmpty++;
                    }
                }
            }
        }
        assertEquals(totalEmpty, reachableEmpty, "All empty cells must be connected when custom fixed cells are specified");
    }

    @Test
    public void testGenerateRandomGridNullFixedEmpty() {
        int N = 5;
        Random rnd = new Random(42);

        boolean[][] grid = Grid4neighborAlgorithms.generateRandomGrid(N, (int[][]) null, rnd);
        assertNotNull(grid);
        assertEquals(N, grid.length);

        int startR = -1, startC = -1;
        int totalEmpty = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!grid[r][c]) {
                    totalEmpty++;
                    if (startR == -1) {
                        startR = r;
                        startC = c;
                    }
                }
            }
        }
        assertTrue(totalEmpty > 0);
        int[][] dist = Grid4neighborAlgorithms.bfs(N, N, grid, startR, startC);
        int reachableEmpty = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!grid[r][c] && dist[r][c] != Integer.MAX_VALUE) {
                    reachableEmpty++;
                }
            }
        }
        assertEquals(totalEmpty, reachableEmpty, "All empty cells must be connected when fixedEmptyCells is null");
    }
}
