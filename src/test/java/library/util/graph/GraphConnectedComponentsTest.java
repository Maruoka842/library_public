package library.util.graph;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class GraphConnectedComponentsTest {

    @Test
    public void testEmptyGraph() {
        Graph g = new Graph(0);
        Graph[] comps = g.connectedComponents();
        assertNotNull(comps);
        assertEquals(0, comps.length);
    }

    @Test
    public void testSingleVertexNoEdges() {
        Graph g = new Graph(1);
        Graph[] comps = g.connectedComponents();
        assertNotNull(comps);
        assertEquals(1, comps.length);
        assertEquals(1, comps[0].N);
        assertEquals(0, comps[0].M);
        assertTrue(comps[0].isConnected());
    }

    @Test
    public void testMultipleIsolatedVertices() {
        Graph g = new Graph(4);
        Graph[] comps = g.connectedComponents();
        assertEquals(4, comps.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(1, comps[i].N);
            assertEquals(0, comps[i].M);
        }
    }

    @Test
    public void testSingleConnectedComponent() {
        Graph g = new Graph(4);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        Graph[] comps = g.connectedComponents();
        assertEquals(1, comps.length);
        assertEquals(4, comps[0].N);
        assertEquals(3, comps[0].M);
        assertTrue(comps[0].isConnected());
    }

    @Test
    public void testMultipleConnectedComponents() {
        // Component 0: {0, 2} with edge (0, 2)
        // Component 1: {1, 3, 4} with edges (1, 3), (3, 4), (4, 1) [cycle of length 3]
        // Component 2: {5} isolated
        Graph g = new Graph(6);
        g.addEdge(0, 2);
        g.addEdge(1, 3);
        g.addEdge(3, 4);
        g.addEdge(4, 1);

        Graph[] comps = g.connectedComponents();
        assertEquals(3, comps.length);

        // First component: vertices {0, 2}
        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertTrue(comps[0].isConnected());
        // Verify edge in comps[0]
        assertEquals(1, comps[0].adj[0].size());
        assertEquals(1, comps[0].adj[0].get(0));
        assertEquals(1, comps[0].adj[1].size());
        assertEquals(0, comps[0].adj[1].get(0));

        // Second component: vertices {1, 3, 4}
        assertEquals(3, comps[1].N);
        assertEquals(3, comps[1].M);
        assertTrue(comps[1].isConnected());
        for (int i = 0; i < 3; i++) {
            assertEquals(2, comps[1].adj[i].size());
        }

        // Third component: vertex {5}
        assertEquals(1, comps[2].N);
        assertEquals(0, comps[2].M);
    }

    @Test
    public void testMultipleEdgesAndSelfLoopsPreserved() {
        // Component with multigraph and self loops:
        // Vertex 0: self loop (0, 0)
        // Vertices 0 and 1: 2 parallel edges (0, 1) and (0, 1)
        // Vertex 2: isolated
        Graph g = new Graph(3);
        g.addEdge(0, 0); // self-loop
        g.addEdge(0, 1);
        g.addEdge(0, 1); // parallel edge

        Graph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        // Comp 0: vertices {0, 1}
        assertEquals(2, comps[0].N);
        assertEquals(3, comps[0].M); // 1 self loop + 2 parallel edges

        // Self-loop on vertex 0 in comps[0]:
        // In g, adj[0] contains 0, 1, 1. So comps[0].adj[0] should contain 0, 1, 1.
        // comps[0].adj[1] should contain 0, 0.
        assertEquals(3, comps[0].adj[0].size());
        assertEquals(2, comps[0].adj[1].size());

        // Comp 1: vertex {2}
        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }
}
