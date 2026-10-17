package library.util.graph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import library.util.graph.tree.Forest;

public class GraphTest {

    @Test
    public void testCopy() {
        Graph g1 = new Graph(3);
        g1.addEdge(0, 1);
        g1.addEdge(1, 2);

        Graph g2 = g1.copy();

        // Check if basic properties are the same
        assertEquals(g1.N, g2.N);
        assertEquals(g1.M, g2.M);

        // Check if adjacency lists are the same
        for (int i = 0; i < g1.N; i++) {
            assertEquals(g1.adj[i].size(), g2.adj[i].size());
            for (int j = 0; j < g1.adj[i].size(); j++) {
                assertEquals(g1.adj[i].get(j), g2.adj[i].get(j));
            }
            // Ensure they are not the same object
            assertNotSame(g1.adj[i], g2.adj[i]);
        }

        // Modify g1 and check if g2 is unchanged
        g1.addEdge(0, 2);
        assertEquals(3, g1.M);
        assertEquals(2, g2.M);
        assertEquals(2, g1.adj[0].size());
        assertEquals(1, g2.adj[0].size());
        assertEquals(2, g1.adj[2].size());
        assertEquals(1, g2.adj[2].size());

        // Modify g2 and check if g1 is unchanged
        g2.addEdge(2, 2); // self loop
        assertEquals(3, g2.M);
        assertEquals(3, g1.M);
        assertEquals(2, g2.adj[2].size()); // originally 1 (from 1-2 edge), now 2 (from 2-2 loop)
        assertEquals(2, g1.adj[2].size()); // g1 was 2 from g1.addEdge(0, 2)
    }

    @Test
    public void testCopyEmpty() {
        Graph g1 = new Graph(0);
        Graph g2 = g1.copy();
        assertEquals(0, g2.N);
        assertEquals(0, g2.M);
    }

    @Test
    public void testCopyNoEdges() {
        Graph g1 = new Graph(5);
        Graph g2 = g1.copy();
        assertEquals(5, g2.N);
        assertEquals(0, g2.M);
        for(int i=0; i<5; i++) {
            assertEquals(0, g2.adj[i].size());
        }
    }

    @Test
    public void testRandomDfsSpanningForestEmpty() {
        Graph g = new Graph(0);
        Forest forest = g.randomDfsSpanningForest();
        assertEquals(0, forest.N);
        assertEquals(0, forest.M);
    }

    @Test
    public void testRandomDfsSpanningForestSingleVertex() {
        Graph g = new Graph(1);
        Forest forest = g.randomDfsSpanningForest(0, new Random(1));
        assertEquals(1, forest.N);
        assertEquals(0, forest.M);
        assertTrue(forest.isRooted());
    }

    @Test
    public void testRandomDfsSpanningForestConnectedGraph() {
        // Complete graph K_5
        Graph g = new Graph(5);
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                g.addEdge(i, j);
            }
        }

        Random rnd = new Random(42);
        Forest forest = g.randomDfsSpanningForest(2, rnd);

        assertEquals(5, forest.N);
        assertEquals(4, forest.M);
        assertTrue(forest.isConnected());
        assertTrue(forest.isRooted());
        assertNull(forest.findCycleVertices());
    }

    @Test
    public void testRandomDfsSpanningForestDisconnectedGraph() {
        // Graph with 2 components: {0, 1, 2} and {3, 4}
        Graph g = new Graph(5);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0); // triangle
        g.addEdge(3, 4);

        Forest forest = g.randomDfsSpanningForest();

        assertEquals(5, forest.N);
        assertEquals(3, forest.M); // (3 - 1) + (2 - 1) = 3
        assertEquals(2, forest.numberOfConnectedComponents());
        assertNull(forest.findCycleVertices());
    }

    @Test
    public void testRandomDfsSpanningForestMultigraphAndSelfLoops() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        g.addEdge(0, 1); // duplicate edge
        g.addEdge(1, 1); // self loop
        g.addEdge(1, 2);

        Forest forest = g.randomDfsSpanningForest();

        assertEquals(3, forest.N);
        assertEquals(2, forest.M);
        assertTrue(forest.isConnected());
        assertNull(forest.findCycleVertices());
    }

    @Test
    public void testRandomDfsSpanningForestInvalidRoot() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        g.addEdge(1, 2);

        Random rnd = new Random(1);
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningForest(-1, rnd));
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningForest(3, rnd));
    }

    @Test
    public void testRandomDfsSpanningForestDeterministicWithSeed() {
        Graph g = Graph.randomConnectedGraph(10, 20);

        Forest forest1 = g.randomDfsSpanningForest(0, new Random(12345));
        Forest forest2 = g.randomDfsSpanningForest(0, new Random(12345));

        assertEquals(forest1, forest2);
    }

    @Test
    public void testRandomDfsSpanningForestOverloads() {
        Graph g = Graph.randomConnectedGraph(6, 10);

        Random rnd = new Random(999);
        Forest f1 = g.randomDfsSpanningForest(1, rnd);
        Forest f2 = g.randomDfsSpanningForest(1);
        Forest f3 = g.randomDfsSpanningForest(rnd);
        Forest f4 = g.randomDfsSpanningForest();

        for (Forest f : new Forest[]{f1, f2, f3, f4}) {
            assertEquals(6, f.N);
            assertEquals(5, f.M);
            assertTrue(f.isConnected());
            assertTrue(f.isRooted());
            assertNull(f.findCycleVertices());
        }
    }

    @Test
    public void testRandomDfsSpanningTreeEmpty() {
        Graph g = new Graph(0);
        Forest tree = g.randomDfsSpanningTree();
        assertEquals(0, tree.N);
        assertEquals(0, tree.M);
    }

    @Test
    public void testRandomDfsSpanningTreeSingleVertex() {
        Graph g = new Graph(1);
        Forest tree = g.randomDfsSpanningTree(0, new Random(1));
        assertEquals(1, tree.N);
        assertEquals(0, tree.M);
        assertTrue(tree.isRooted());
    }

    @Test
    public void testRandomDfsSpanningTreeConnectedGraph() {
        // Complete graph K_5
        Graph g = new Graph(5);
        for (int i = 0; i < 5; i++) {
            for (int j = i + 1; j < 5; j++) {
                g.addEdge(i, j);
            }
        }

        Random rnd = new Random(42);
        Forest tree = g.randomDfsSpanningTree(2, rnd);

        assertEquals(5, tree.N);
        assertEquals(4, tree.M);
        assertTrue(tree.isConnected());
        assertTrue(tree.isRooted());
        assertNull(tree.findCycleVertices());
    }

    @Test
    public void testRandomDfsSpanningTreeDisconnectedGraph() {
        // Graph with 2 components: {0, 1, 2} and {3, 4}
        Graph g = new Graph(5);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0); // triangle
        g.addEdge(3, 4);

        Random rnd = new Random(100);
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningTree(0, rnd));
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningTree(3, rnd));
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningTree());
    }

    @Test
    public void testRandomDfsSpanningTreeMultigraphAndSelfLoops() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        g.addEdge(0, 1); // duplicate edge
        g.addEdge(1, 1); // self loop
        g.addEdge(1, 2);

        Forest tree = g.randomDfsSpanningTree();

        assertEquals(3, tree.N);
        assertEquals(2, tree.M);
        assertTrue(tree.isConnected());
        assertNull(tree.findCycleVertices());
    }

    @Test
    public void testRandomDfsSpanningTreeInvalidRoot() {
        Graph g = new Graph(3);
        g.addEdge(0, 1);
        g.addEdge(1, 2);

        Random rnd = new Random(1);
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningTree(-1, rnd));
        assertThrows(IllegalArgumentException.class, () -> g.randomDfsSpanningTree(3, rnd));
    }

    @Test
    public void testRandomDfsSpanningTreeDeterministicWithSeed() {
        Graph g = Graph.randomConnectedGraph(10, 20);

        Forest tree1 = g.randomDfsSpanningTree(0, new Random(12345));
        Forest tree2 = g.randomDfsSpanningTree(0, new Random(12345));

        assertEquals(tree1, tree2);
    }

    @Test
    public void testRandomDfsSpanningTreeOverloads() {
        Graph g = Graph.randomConnectedGraph(6, 10);

        Random rnd = new Random(999);
        Forest t1 = g.randomDfsSpanningTree(1, rnd);
        Forest t2 = g.randomDfsSpanningTree(1);
        Forest t3 = g.randomDfsSpanningTree(rnd);
        Forest t4 = g.randomDfsSpanningTree();

        for (Forest t : new Forest[]{t1, t2, t3, t4}) {
            assertEquals(6, t.N);
            assertEquals(5, t.M);
            assertTrue(t.isConnected());
            assertTrue(t.isRooted());
            assertNull(t.findCycleVertices());
        }
    }

    @Test
    public void testRandomKruskalSpanningForest() {
        Graph gConnected = Graph.randomConnectedGraph(8, 15);
        Random rnd = new Random(123);

        Forest forest1 = gConnected.randomKruskalSpanningForest(rnd);
        assertEquals(8, forest1.N);
        assertEquals(7, forest1.M);
        assertTrue(forest1.isConnected());
        assertTrue(forest1.isRooted());
        assertNull(forest1.findCycleVertices());

        Forest forest2 = gConnected.randomKruskalSpanningForest();
        assertEquals(8, forest2.N);
        assertEquals(7, forest2.M);
        assertTrue(forest2.isConnected());
        assertTrue(forest2.isRooted());
        assertNull(forest2.findCycleVertices());

        Graph gDisconnected = new Graph(5);
        gDisconnected.addEdge(0, 1);
        gDisconnected.addEdge(1, 2);
        gDisconnected.addEdge(2, 0); // triangle
        gDisconnected.addEdge(3, 4);

        Forest forest3 = gDisconnected.randomKruskalSpanningForest(new Random(456));
        assertEquals(5, forest3.N);
        assertEquals(3, forest3.M);
        assertEquals(2, forest3.numberOfConnectedComponents());
        assertTrue(forest3.isRooted());
        assertNull(forest3.findCycleVertices());

        Graph gEmpty = new Graph(0);
        Forest forestEmpty = gEmpty.randomKruskalSpanningForest();
        assertEquals(0, forestEmpty.N);
        assertEquals(0, forestEmpty.M);
    }

    @Test
    public void testRandomKruskalSpanningTree() {
        Graph gConnected = Graph.randomConnectedGraph(8, 15);
        Random rnd = new Random(123);

        Forest tree1 = gConnected.randomKruskalSpanningTree(rnd);
        assertEquals(8, tree1.N);
        assertEquals(7, tree1.M);
        assertTrue(tree1.isConnected());
        assertTrue(tree1.isRooted());
        assertNull(tree1.findCycleVertices());

        Forest tree2 = gConnected.randomKruskalSpanningTree();
        assertEquals(8, tree2.N);
        assertEquals(7, tree2.M);
        assertTrue(tree2.isConnected());
        assertTrue(tree2.isRooted());
        assertNull(tree2.findCycleVertices());

        Graph gEmpty = new Graph(0);
        Forest treeEmpty = gEmpty.randomKruskalSpanningTree();
        assertEquals(0, treeEmpty.N);
        assertEquals(0, treeEmpty.M);
    }

    @Test
    public void testRandomKruskalSpanningTreeDisconnected() {
        Graph g = new Graph(5);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0); // triangle
        g.addEdge(3, 4);

        assertThrows(IllegalArgumentException.class, () -> g.randomKruskalSpanningTree(new Random(456)));
        assertThrows(IllegalArgumentException.class, () -> g.randomKruskalSpanningTree());
    }
}
