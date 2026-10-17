package library.util.graph;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class LongValueRandomGraphTest {

    @Test
    public void testLongValueDigraphRandom() {
        Random rnd = new Random(42);
        LongValueDigraph g1 = LongValueDigraph.randomDiGraph(10, 15, 5, 20, rnd);
        assertEquals(10, g1.N);
        assertEquals(15, g1.M);
        for (var e : g1.edges()) {
            assertTrue(e.cost >= 5 && e.cost <= 20);
        }

        LongValueDigraph g2 = LongValueDigraph.randomDiGraph(5, 8);
        assertEquals(5, g2.N);
        assertEquals(8, g2.M);

        LongValueDigraph g3 = LongValueDigraph.randomDiGraph(6);
        assertEquals(6, g3.N);
        assertTrue(g3.M >= 0 && g3.M <= 36);
    }

    @Test
    public void testLongValueGraphRandom() {
        Random rnd = new Random(42);
        LongValueGraph g1 = LongValueGraph.randomGraph(10, 15, 10, 100, rnd);
        assertEquals(10, g1.N);
        assertEquals(15, g1.M);
        for (var e : g1.edges()) {
            assertTrue(e.cost >= 10 && e.cost <= 100);
        }

        LongValueGraph g2 = LongValueGraph.randomGraph(5);
        assertEquals(5, g2.N);
        assertTrue(g2.M >= 0 && g2.M <= 10);

        LongValueGraph cg1 = LongValueGraph.randomConnectedGraph(8, 12, 1, 5, rnd);
        assertEquals(8, cg1.N);
        assertEquals(12, cg1.M);
        assertTrue(cg1.isConnected());

        LongValueGraph cg2 = LongValueGraph.randomConnectedGraph(6);
        assertEquals(6, cg2.N);
        assertTrue(cg2.isConnected());
    }

    @Test
    public void testLongValueFunctionalGraphRandom() {
        Random rnd = new Random(42);
        LongValueFunctionalGraph fg1 = LongValueFunctionalGraph.randomFunctionalGraph(10, rnd);
        assertEquals(10, fg1.N);
        assertNotNull(fg1.order());

        LongValueFunctionalGraph fg2 = LongValueFunctionalGraph.randomFunctionalGraph(10, 1, 50, Long::sum, rnd);
        assertEquals(10, fg2.N);
        assertEquals(10, fg2.data.length);
        for (long val : fg2.data) {
            assertTrue(val >= 1 && val <= 50);
        }
    }
}
