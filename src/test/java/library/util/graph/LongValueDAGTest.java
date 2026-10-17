package library.util.graph;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class LongValueDAGTest {

    @Test
    public void testFactoryAndConstraints() {
        // Natural order: from < to
        LongValueDAG g1 = LongValueDAG.ofNaturalOrder(3);
        g1.addEdge(0, 1, 10L);
        g1.addEdge(1, 2, 20L);
        assertThrows(AssertionError.class, () -> g1.addEdge(1, 0, 5L));
        assertThrows(AssertionError.class, () -> g1.addEdge(2, 1, 5L));
        assertArrayEquals(new int[]{0, 1, 2}, g1.topologicalOrder());

        // Reverse natural order: from > to
        LongValueDAG g2 = LongValueDAG.ofNaturalReverseOrder(3);
        g2.addEdge(2, 1, 10L);
        g2.addEdge(1, 0, 20L);
        assertThrows(AssertionError.class, () -> g2.addEdge(0, 1, 5L));
        assertThrows(AssertionError.class, () -> g2.addEdge(1, 2, 5L));
        assertArrayEquals(new int[]{2, 1, 0}, g2.topologicalOrder());
    }

    @Test
    public void testPathLengths() {
        int N = 3;
        LongValueDAG g = LongValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 5L);
        g.addEdge(0, 2, 15L);
        g.addEdge(1, 2, 5L);

        int[] shortestLen = g.shortestPathLengthForEachStart();
        int[] longestLen = g.longestPathLengthForEachStart();

        assertArrayEquals(new int[]{1, 1, 0}, shortestLen);
        assertArrayEquals(new int[]{2, 1, 0}, longestLen);
    }

    @Test
    public void testPathCosts() {
        int N = 3;
        LongValueDAG g = LongValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 5L);
        g.addEdge(0, 2, 15L);
        g.addEdge(1, 2, 3L);

        long[] shortestCost = g.shortestPathCostForEachStart();
        long[] longestCost = g.longestPathCostForEachStart();

        assertArrayEquals(new long[]{8L, 3L, 0L}, shortestCost);
        assertArrayEquals(new long[]{15L, 3L, 0L}, longestCost);
    }

    @Test
    public void testPathCostsFixingEnd() {
        int N = 5;
        LongValueDAG g = LongValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 2L);
        g.addEdge(0, 2, 8L);
        g.addEdge(1, 2, 3L);
        g.addEdge(1, 3, 1L);
        g.addEdge(2, 3, 4L);
        // vertex 4 is completely isolated

        long[] shortestCost = g.shortestPathCostForEachStartFixingEnd(3);
        long[] longestCost = g.longestPathCostForEachStartFixingEnd(3);

        assertEquals(3L, shortestCost[0]);
        assertEquals(1L, shortestCost[1]);
        assertEquals(4L, shortestCost[2]);
        assertEquals(0L, shortestCost[3]);
        assertEquals(Long.MAX_VALUE, shortestCost[4]);

        assertEquals(12L, longestCost[0]); // 0 -> 2 -> 3 (8+4 = 12)
        assertEquals(7L, longestCost[1]); // 1 -> 2 -> 3 (3+4 = 7)
        assertEquals(4L, longestCost[2]);
        assertEquals(0L, longestCost[3]);
        assertEquals(Long.MIN_VALUE, longestCost[4]);
    }

    @Test
    public void testReverse() {
        int N = 3;
        LongValueDAG g = LongValueDAG.ofNaturalOrder(N);
        g.addEdge(0, 1, 5L);
        g.addEdge(1, 2, 10L);

        LongValueDAG rev = g.reverse();
        // Edge directions should be reversed, so 1 -> 0 and 2 -> 1
        // We should be able to run topologicalSort on rev.
        int[] ord = rev.topologicalOrder();
        assertArrayEquals(new int[]{2, 1, 0}, ord);

        long[] longestCost = rev.longestPathCostForEachStart();
        assertArrayEquals(new long[]{0L, 5L, 15L}, longestCost); // dp[0]=0, dp[1]=5, dp[2]=15
    }
}
