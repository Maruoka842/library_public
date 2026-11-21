package library.util.graph;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AllConnectedComponentsTest {

    @Test
    public void testDigraph() {
        // Component 0: 0 -> 1, 1 -> 0
        // Component 1: 2 -> 3
        // Component 2: 4 (isolated)
        Digraph g = new Digraph(5);
        g.addEdge(0, 1);
        g.addEdge(1, 0);
        g.addEdge(2, 3);

        Digraph[] comps = g.connectedComponents();
        assertEquals(3, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(2, comps[0].M);
        assertEquals(1, comps[0].adj[0].get(0));
        assertEquals(0, comps[0].adj[1].get(0));

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].M);
        assertEquals(1, comps[1].adj[0].get(0));
        assertEquals(0, comps[1].adj[1].size());

        assertEquals(1, comps[2].N);
        assertEquals(0, comps[2].M);
    }

    @Test
    public void testLongValueGraph() {
        LongValueGraph g = new LongValueGraph(4);
        g.addEdge(0, 1, 100);
        g.addEdge(2, 3, 200);

        LongValueGraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals(100, comps[0].adj[0].get(0).cost);

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].M);
        assertEquals(200, comps[1].adj[0].get(0).cost);
    }

    @Test
    public void testDoubleValueGraph() {
        DoubleValueGraph g = new DoubleValueGraph(4);
        g.addEdge(0, 1, 1.5);
        g.addEdge(2, 3, 2.5);

        DoubleValueGraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals(1.5, comps[0].adj[0].get(0).cost, 1e-9);

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].M);
        assertEquals(2.5, comps[1].adj[0].get(0).cost, 1e-9);
    }

    @Test
    public void testValueGraph() {
        ValueGraph<String> g = new ValueGraph<>(4);
        g.addEdge(0, 1, "hello");
        g.addEdge(2, 3, "world");

        ValueGraph<String>[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals("hello", comps[0].adj[0].get(0).weight());

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].M);
        assertEquals("world", comps[1].adj[0].get(0).weight());
    }

    @Test
    public void testLongValueDigraph() {
        LongValueDigraph g = new LongValueDigraph(3);
        g.addEdge(0, 1, 50);
        // vertex 2 is isolated

        LongValueDigraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals(1, comps[0].adj[0].get(0).dst);
        assertEquals(50, comps[0].adj[0].get(0).cost);

        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }

    @Test
    public void testDoubleValueDigraph() {
        DoubleValueDigraph g = new DoubleValueDigraph(3);
        g.addEdge(0, 1, 3.14);

        DoubleValueDigraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals(3.14, comps[0].adj[0].get(0).cost, 1e-9);

        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }

    @Test
    public void testValueDigraph() {
        ValueDigraph<Integer> g = new ValueDigraph<>(3);
        g.addEdge(0, 1, 42);

        ValueDigraph<Integer>[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertEquals(42, comps[0].adj[0].get(0).weight());

        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }

    @Test
    public void testAdjacencyMatrixGraph() {
        AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(4);
        g.addEdge(0, 1);
        g.addEdge(2, 3);

        AdjacencyMatrixGraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertTrue(comps[0].adj[0][1]);
        assertTrue(comps[0].adj[1][0]);

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].M);
        assertTrue(comps[1].adj[0][1]);
        assertTrue(comps[1].adj[1][0]);
    }

    @Test
    public void testAdjacencyMatrixDigraph() {
        AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(3);
        g.addEdge(0, 1);

        AdjacencyMatrixDigraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);
        assertTrue(comps[0].adj[0][1]);
        assertFalse(comps[0].adj[1][0]);

        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }

    @Test
    public void testStaticGraph() {
        StaticGraph g = new StaticGraph(4);
        g.addEdge(0, 1);
        g.addEdge(2, 3);
        g.build();

        StaticGraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].deg(0));
        assertEquals(1, comps[0].deg(1));

        assertEquals(2, comps[1].N);
        assertEquals(1, comps[1].deg(0));
        assertEquals(1, comps[1].deg(1));
    }

    @Test
    public void testStaticDigraph() {
        StaticDigraph g = new StaticDigraph(3, List.of(new int[] {0, 1}));

        StaticDigraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N);
        assertEquals(1, comps[0].M);

        assertEquals(1, comps[1].N);
        assertEquals(0, comps[1].M);
    }

    @Test
    public void testHashGraph() {
        HashGraph g = new HashGraph(4);
        g.addEdge(0, 1);
        g.addEdge(2, 3);

        HashGraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].N());
        assertTrue(comps[0].adjacents(0).contains(1));

        assertEquals(2, comps[1].N());
        assertTrue(comps[1].adjacents(0).contains(1));
    }

    @Test
    public void testHashDigraph() {
        HashDigraph g = new HashDigraph(3);
        g.addEdge(0, 1);

        HashDigraph[] comps = g.connectedComponents();
        assertEquals(2, comps.length);

        assertEquals(2, comps[0].vertexCount());
        assertTrue(comps[0].out.get(0).contains(1));

        assertEquals(1, comps[1].vertexCount());
        assertEquals(0, comps[1].out.get(0).size());
    }
}
