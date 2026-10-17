package library.util.graph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class DoubleValueDAGTest {

    @Test
    public void testOfNaturalReverseOrder() {
        int N = 3;
        DoubleValueDAG g = DoubleValueDAG.ofNaturalReverseOrder(N);

        // This should succeed
        g.addEdge(2, 1, 1.5);
        g.addEdge(1, 0, 2.5);

        // Check topological order
        int[] ord = g.topologicalOrder();
        assertArrayEquals(new int[]{2, 1, 0}, ord);

        // Adding edge 1 -> 2 or 0 -> 1 or 0 -> 2 should throw AssertionError
        assertThrows(AssertionError.class, () -> g.addEdge(1, 2, 1.0));
        assertThrows(AssertionError.class, () -> g.addEdge(0, 1, 1.0));
        assertThrows(AssertionError.class, () -> g.addEdge(0, 2, 1.0));
    }

    @Test
    public void testShortestPathLength() {
        int N = 3;
        DoubleValueDAG g = DoubleValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 1.0);
        g.addEdge(0, 2, 1.0);
        g.addEdge(1, 2, 1.0);

        int[] shortestLen = g.shortestPathLengthForEachStart();
        int[] longestLen = g.longestPathLengthForEachStart();

        assertArrayEquals(new int[]{1, 1, 0}, shortestLen);
        assertArrayEquals(new int[]{2, 1, 0}, longestLen);
    }

    @Test
    public void testShortestPathCost() {
        int N = 3;
        DoubleValueDAG g = DoubleValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 5.0);
        g.addEdge(0, 2, 10.0);
        g.addEdge(1, 2, 2.0);

        double[] shortestCost = g.shortestPathCostForEachStart();
        double[] longestCost = g.longestPathCostForEachStart();

        assertArrayEquals(new double[]{7.0, 2.0, 0.0}, shortestCost, 1e-9);
        assertArrayEquals(new double[]{10.0, 2.0, 0.0}, longestCost, 1e-9);
    }

    @Test
    public void testShortestPathCostFixingEnd() {
        int N = 5;
        DoubleValueDAG g = DoubleValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 2.0);
        g.addEdge(0, 2, 8.0);
        g.addEdge(1, 2, 3.0);
        g.addEdge(1, 3, 1.0);
        g.addEdge(2, 3, 4.0);
        // vertex 4 is completely isolated

        double[] shortestCost = g.shortestPathCostForEachStartFixingEnd(3);
        double[] longestCost = g.longestPathCostForEachStartFixingEnd(3);

        assertEquals(3.0, shortestCost[0], 1e-9);
        assertEquals(1.0, shortestCost[1], 1e-9);
        assertEquals(4.0, shortestCost[2], 1e-9);
        assertEquals(0.0, shortestCost[3], 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, shortestCost[4]);

        assertEquals(12.0, longestCost[0], 1e-9); // 0 -> 2 -> 3 (8+4 = 12.0)
        assertEquals(7.0, longestCost[1], 1e-9); // 1 -> 2 -> 3 (3+4 = 7.0)
        assertEquals(4.0, longestCost[2], 1e-9);
        assertEquals(0.0, longestCost[3], 1e-9);
        assertEquals(Double.NEGATIVE_INFINITY, longestCost[4]);
    }
}
