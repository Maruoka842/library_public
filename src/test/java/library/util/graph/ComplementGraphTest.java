package library.util.graph;

import org.junit.jupiter.api.Test;
import library.util.algebra.strategy.ZnStrategy;
import library.util.algebra.strategy.ZStrategy;
import library.util.algebra.strategy.longs.LongZStrategy;

import java.awt.HeadlessException;

import static org.junit.jupiter.api.Assertions.*;

class ComplementGraphTest {

    @Test
    void testToGraphAndRemoveEdge() {
        ComplementGraph cg = new ComplementGraph(4);
        assertEquals(4, cg.vertexCount());
        assertEquals(6, cg.edgeCount());

        // Initially complete graph K_4: 4 * 3 / 2 = 6 edges
        Graph g0 = cg.toGraph();
        assertEquals(4, g0.N);
        assertEquals(6, g0.M);

        // Delete edge (0, 1) and (1, 2)
        cg.removeEdge(0, 1);
        cg.removeEdge(1, 2);

        assertEquals(4, cg.vertexCount());
        assertEquals(4, cg.edgeCount());

        Graph g = cg.toGraph();
        assertEquals(4, g.N);
        assertEquals(4, g.M);

        // Verify adjacencies in g
        assertTrue(cg.nonAdj[0].contains(1));
        assertTrue(cg.nonAdj[1].contains(0));
        assertTrue(cg.nonAdj[1].contains(2));

        // Check edges in converted Graph
        int[] distFrom0 = cg.distances(0);
        assertArrayEquals(g.bfsDistances(0), distFrom0);
    }

    @Test
    void testDrawDoesNotThrow() {
        ComplementGraph cg = new ComplementGraph(3);
        cg.removeEdge(0, 1);
        try {
            cg.draw();
        } catch (HeadlessException e) {
            // Expected in headless environment
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    static class LongZnRingStrategy implements library.util.algebra.strategy.longs.LongRingStrategy {
        final long mod;
        LongZnRingStrategy(long mod) { this.mod = mod; }
        @Override public long zero() { return 0; }
        @Override public long one() { return 1 % mod; }
        @Override public long add(long a, long b) { long s = (a + b) % mod; return s < 0 ? s + mod : s; }
        @Override public long mul(long a, long b) { long res = (a % mod) * (b % mod) % mod; return res < 0 ? res + mod : res; }
        @Override public long neg(long a) { long res = -a % mod; return res < 0 ? res + mod : res; }
        @Override public boolean equals(long a, long b) { return (a % mod + mod) % mod == (b % mod + mod) % mod; }
    }

    @Test
    void testShortestPathWeightSumSingleVertex() {
        ComplementGraph cg = new ComplementGraph(1);
        long[] f = {5};
        long mod = 998244353;
        long[] res = cg.shortestPathWeightSum(0, f, new LongZnRingStrategy(mod));
        assertArrayEquals(new long[]{5}, res);
    }

    @Test
    void testShortestPathWeightSumDisconnected() {
        ComplementGraph cg = new ComplementGraph(3);
        cg.removeEdge(0, 1);
        cg.removeEdge(0, 2);
        cg.removeEdge(1, 2); // K_3 with all 3 edges removed -> 0 edges in complement graph

        long[] f = {2, 3, 5};
        long mod = 998244353;
        long[] res0 = cg.shortestPathWeightSum(0, f, new LongZnRingStrategy(mod));
        assertArrayEquals(new long[]{2, 0, 0}, res0);
    }

    @Test
    void testShortestPathWeightSumWithBruteForce() {
        long mod = 998244353;
        int N = 6;
        ComplementGraph cg = new ComplementGraph(N);
        cg.removeEdge(0, 1);
        cg.removeEdge(1, 2);
        cg.removeEdge(2, 3);
        cg.removeEdge(0, 4);

        long[] f = {2, 5, 7, 3, 11, 13};

        for (int src = 0; src < N; src++) {
            long[] expected = bruteForceShortestPathWeightSum(cg, src, f, mod);
            long[] actual = cg.shortestPathWeightSum(src, f, new LongZnRingStrategy(mod));
            assertArrayEquals(expected, actual, "Mismatch for src=" + src);
        }
    }

    @Test
    void testShortestPathWeightSumWithRingStrategy() {
        long mod = 998244353;
        ZnStrategy ring = new ZnStrategy(mod);
        int N = 6;
        ComplementGraph cg = new ComplementGraph(N);
        cg.removeEdge(0, 1);
        cg.removeEdge(1, 2);
        cg.removeEdge(2, 3);
        cg.removeEdge(0, 4);

        Long[] f = {2L, 5L, 7L, 3L, 11L, 13L};

        for (int src = 0; src < N; src++) {
            long[] primitiveF = new long[N];
            for (int i = 0; i < N; i++) primitiveF[i] = f[i];

            long[] expectedPrimitive = bruteForceShortestPathWeightSum(cg, src, primitiveF, mod);
            Long[] actualRing = cg.shortestPathWeightSum(src, f, ring);

            for (int i = 0; i < N; i++) {
                assertEquals(expectedPrimitive[i], actualRing[i].longValue(), "Mismatch for src=" + src + ", i=" + i);
            }
        }
    }

    @Test
    void testShortestPathWeightSumWithZStrategy() {
        ZStrategy ring = new ZStrategy();
        int N = 5;
        ComplementGraph cg = new ComplementGraph(N);
        cg.removeEdge(0, 1);
        cg.removeEdge(1, 2);

        Long[] f = {2L, 3L, 4L, 5L, 6L};

        Long[] actual = cg.shortestPathWeightSum(0, f, ring);
        // Compare with brute force in Z
        long[] primitiveF = {2L, 3L, 4L, 5L, 6L};
        long largeMod = 1_000_000_000_000_000L;
        long[] expected = bruteForceShortestPathWeightSum(cg, 0, primitiveF, largeMod);

        for (int i = 0; i < N; i++) {
            assertEquals(expected[i], actual[i].longValue());
        }
    }

    @Test
    void testShortestPathWeightSumWithLongRingStrategy() {
        LongZStrategy ring = new LongZStrategy();
        int N = 5;
        ComplementGraph cg = new ComplementGraph(N);
        cg.removeEdge(0, 1);
        cg.removeEdge(1, 2);

        long[] f = {2L, 3L, 4L, 5L, 6L};
        long[] actual = cg.shortestPathWeightSum(0, f, ring);

        long largeMod = 1_000_000_000_000_000L;
        long[] expected = bruteForceShortestPathWeightSum(cg, 0, f, largeMod);

        assertArrayEquals(expected, actual);
    }

    @Test
    void testShortestPathWeightSumNegativeWeights() {
        long mod = 998244353;
        int N = 5;
        ComplementGraph cg = new ComplementGraph(N);
        cg.removeEdge(0, 2);
        cg.removeEdge(1, 3);

        long[] f = {-2, 5, -7, 3, -11};
        for (int i = 0; i < N; i++) {
            f[i] = (f[i] % mod + mod) % mod;
        }

        for (int src = 0; src < N; src++) {
            long[] expected = bruteForceShortestPathWeightSum(cg, src, f, mod);
            long[] actual = cg.shortestPathWeightSum(src, f, new LongZnRingStrategy(mod));
            assertArrayEquals(expected, actual, "Mismatch for src=" + src);
        }
    }

    private long[] bruteForceShortestPathWeightSum(ComplementGraph cg, int src, long[] f, long mod) {
        Graph g = cg.toGraph();
        int N = cg.N;
        int[] dist = g.bfsDistances(src);
        long[] res = new long[N];
        for (int target = 0; target < N; target++) {
            if (dist[target] == Integer.MAX_VALUE / 3) continue;
            res[target] = dfsBruteForce(g, src, target, dist[target], 0, (f[src] % mod + mod) % mod, f, mod);
        }
        return res;
    }

    private long dfsBruteForce(Graph g, int u, int target, int targetDist, int currentDist, long currentProd, long[] f, long mod) {
        if (currentDist == targetDist) {
            return u == target ? currentProd : 0;
        }
        long sum = 0;
        for (int i = 0; i < g.adj[u].size(); i++) {
            int v = g.adj[u].get(i);
            long nextProd = (currentProd * ((f[v] % mod + mod) % mod)) % mod;
            sum = (sum + dfsBruteForce(g, v, target, targetDist, currentDist + 1, nextProd, f, mod)) % mod;
        }
        return sum;
    }
}
