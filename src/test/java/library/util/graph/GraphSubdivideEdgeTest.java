package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GraphSubdivideEdgeTest {

    @Test
    public void testSubdivideSimpleEdge() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        g.addEdge(1, 2);

        int origN = g.N;
        int origM = g.M;
        int w = g.subdivideEdge(0, 1);

        assertEquals(3, w);
        assertEquals(origN + 1, g.N);
        assertEquals(origM + 1, g.M);

        // Check degree of each vertex
        assertEquals(1, g.deg(0));
        assertEquals(2, g.deg(1));
        assertEquals(1, g.deg(2));
        assertEquals(2, g.deg(w));

        // 0 should be connected only to w
        assertEquals(1, g.adj[0].size());
        assertEquals(w, g.adj[0].get(0));

        // 1 should be connected to 2 and w
        assertEquals(2, g.adj[1].size());
        int[] adj1 = g.adj[1].toArray();
        assertTrue((adj1[0] == 2 && adj1[1] == w) || (adj1[0] == w && adj1[1] == 2));

        // w should be connected to 0 and 1
        assertEquals(2, g.adj[w].size());
        int[] adjW = g.adj[w].toArray();
        assertTrue((adjW[0] == 0 && adjW[1] == 1) || (adjW[0] == 1 && adjW[1] == 0));
    }

    @Test
    public void testSubdivideMultipleEdges() {
        Graph g = new Graph(2);
        g.addEdge(0, 1);
        g.addEdge(0, 1);
        assertEquals(2, g.M);

        int w = g.subdivideEdge(0, 1);
        assertEquals(2, w);
        assertEquals(3, g.N);
        assertEquals(3, g.M);

        // One {0, 1} edge remains, plus {0, w} and {w, 1}
        assertEquals(2, g.deg(0));
        assertEquals(2, g.deg(1));
        assertEquals(2, g.deg(w));

        boolean hasDirect01 = false;
        boolean has0W = false;
        for (int v : g.adj[0]) {
            if (v == 1) hasDirect01 = true;
            if (v == w) has0W = true;
        }
        assertTrue(hasDirect01);
        assertTrue(has0W);
    }

    @Test
    public void testSubdivideSelfLoop() {
        Graph g = new Graph(2);
        g.addEdge(0, 0);
        g.addEdge(0, 1);
        assertEquals(2, g.M);

        int w = g.subdivideEdge(0, 0);
        assertEquals(2, w);
        assertEquals(3, g.N);
        assertEquals(3, g.M);

        // Loop {0, 0} is removed; edges {0, w} and {w, 0} are added (2 edges connecting 0 and w).
        // 0's incident edges: 1, w, w -> deg(0) == 3
        assertEquals(3, g.deg(0));
        // 1's incident edges: 0 -> deg(1) == 1
        assertEquals(1, g.deg(1));
        // w's incident edges: 0, 0 -> deg(w) == 2
        assertEquals(2, g.deg(w));

        int count0ToW = 0;
        for (int v : g.adj[0]) {
            if (v == w) count0ToW++;
        }
        assertEquals(2, count0ToW);

        int countWTo0 = 0;
        for (int v : g.adj[w]) {
            if (v == 0) countWTo0++;
        }
        assertEquals(2, countWTo0);
    }

    @Test
    public void testSubdivideNonExistentEdgeThrows() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        assertThrows(AssertionError.class, () -> g.subdivideEdge(0, 2));
        assertThrows(AssertionError.class, () -> g.subdivideEdge(1, 2));
    }

    @Test
    public void testSubdivideRepeatedly() {
        Graph g = new Graph(2);
        g.addEdge(0, 1);

        int w1 = g.subdivideEdge(0, 1);
        assertEquals(2, w1);
        assertEquals(3, g.N);
        assertEquals(2, g.M);

        // Now path is 0 - 2 - 1
        int w2 = g.subdivideEdge(0, w1);
        assertEquals(3, w2);
        assertEquals(4, g.N);
        assertEquals(3, g.M);

        // Now path is 0 - 3 - 2 - 1
        int[] dist = g.bfsDistances(0);
        assertEquals(0, dist[0]);
        assertEquals(1, dist[3]);
        assertEquals(2, dist[2]);
        assertEquals(3, dist[1]);
    }
}
