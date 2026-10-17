package library.util.graph;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Set;

public class SparseGraphTest {

    @Test
    public void testBasicOperations() {
        SparseGraph g = new SparseGraph();
        g.addVertex(3);
        g.addVertex(5);
        g.addVertex(10);

        assertEquals(3, g.N());
        assertTrue(g.vertices().contains(3));
        assertTrue(g.vertices().contains(5));
        assertTrue(g.vertices().contains(10));

        g.addEdge(3, 5);
        g.addEdge(5, 10);

        assertEquals(1, g.deg(3));
        assertEquals(2, g.deg(5));
        assertEquals(1, g.deg(10));

        assertTrue(g.adjacents(3).contains(5));
        assertTrue(g.adjacents(5).contains(3));
        assertTrue(g.adjacents(5).contains(10));
        assertTrue(g.adjacents(10).contains(5));

        g.removeEdge(3, 5);
        assertFalse(g.adjacents(3).contains(5));
        assertFalse(g.adjacents(5).contains(3));

        g.removeVertex(5);
        assertEquals(2, g.N());
        assertFalse(g.vertices().contains(5));
    }

    @Test
    public void testContraction() {
        // Test 1: keep 1, remove 2 (1 absorbs 2, 2 is removed, leaving 1 and 3)
        SparseGraph g1 = new SparseGraph();
        g1.addVertex(1);
        g1.addVertex(2);
        g1.addVertex(3);
        g1.addEdge(1, 2);
        g1.addEdge(2, 3);

        g1.contractVertexThenRemoveMultipleEdge(1, 2);
        assertEquals(2, g1.N());
        assertTrue(g1.vertices().contains(1));
        assertTrue(g1.vertices().contains(3));
        assertFalse(g1.vertices().contains(2));
        assertTrue(g1.adjacents(1).contains(3));
        assertTrue(g1.adjacents(3).contains(1));

        // Test 2: keep 20, remove 10 (20 absorbs 10, 10 is removed, leaving 20 and 30)
        SparseGraph g2 = new SparseGraph();
        g2.addVertex(10);
        g2.addVertex(20);
        g2.addVertex(30);
        g2.addEdge(10, 20);
        g2.addEdge(20, 30);

        g2.contractVertexThenRemoveMultipleEdge(20, 10);
        assertEquals(2, g2.N());
        assertTrue(g2.vertices().contains(20));
        assertTrue(g2.vertices().contains(30));
        assertFalse(g2.vertices().contains(10));
        assertTrue(g2.adjacents(20).contains(30));
        assertTrue(g2.adjacents(30).contains(20));

        // Test 3: keep 200, remove 100 (200 absorbs 100, 100 is removed, leaving 200 and 300)
        SparseGraph g3 = new SparseGraph();
        g3.addVertex(100);
        g3.addVertex(200);
        g3.addVertex(300);
        g3.addEdge(100, 200);
        g3.addEdge(200, 300);

        g3.contractVertexThenRemoveMultipleEdge(200, 100);
        assertEquals(2, g3.N());
        assertTrue(g3.vertices().contains(200));
        assertTrue(g3.vertices().contains(300));
        assertFalse(g3.vertices().contains(100));
        assertTrue(g3.adjacents(200).contains(300));
        assertTrue(g3.adjacents(300).contains(200));
    }

    @Test
    public void testContractionError() {
        SparseGraph g = new SparseGraph();
        g.addVertex(5);
        assertThrows(IllegalArgumentException.class, () -> {
            g.contractVertexThenRemoveMultipleEdge(5, 5);
        });
    }

    @Test
    public void testDrawSafe() {
        SparseGraph g = new SparseGraph();
        g.addVertex(1);
        g.addVertex(2);
        g.addEdge(1, 2);

        // Verify draw() runs without throwing uncaught exceptions in standard environments,
        // and catches java.awt.HeadlessException if running in headless environments (e.g. CI).
        try {
            g.draw();
        } catch (java.awt.HeadlessException e) {
            // Expected and acceptable in headless mode.
        } catch (Exception e) {
            fail("draw() threw an unexpected exception: " + e.getMessage());
        }
    }
}
