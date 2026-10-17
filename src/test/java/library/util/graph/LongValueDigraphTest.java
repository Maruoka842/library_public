package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LongValueDigraphTest {
    /** 固定サイズの距離・親・到達不能な負閉路を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void acceptsRedundantNegativeEdges() {
        LongValueDigraph g = new LongValueDigraph(5);
        g.addEdge(2, 0, 5);
        g.addEdge(2, 1, 2);
        g.addEdge(0, 1, -3);
        g.addEdge(3, 4, -2);
        g.addEdge(4, 3, 1);
        var result = g.tryDijkstraIgnoringNegativeEdges(2).orElseThrow();
        assertArrayEquals(new long[] {5, 2, 0, Long.MAX_VALUE, Long.MAX_VALUE}, result.dist());
        assertArrayEquals(new int[] {2, 2, -1, -1, -1}, result.parent());
        g.addEdge(0, 1, -2);
        assertTrue(g.tryDijkstraIgnoringNegativeEdges(2).isPresent());
    }

    /** 固定サイズの距離短縮・到達可能性変化・負閉路を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void rejectsImprovingNegativeEdges() {
        LongValueDigraph g = new LongValueDigraph(3);
        g.addEdge(0, 1, 5);
        g.addEdge(0, 2, 3);
        g.addEdge(1, 2, -3);
        assertTrue(g.tryDijkstraIgnoringNegativeEdges(0).isEmpty());
        LongValueDigraph newlyReachable = new LongValueDigraph(2);
        newlyReachable.addEdge(0, 1, -1);
        assertTrue(newlyReachable.tryDijkstraIgnoringNegativeEdges(0).isEmpty());
        LongValueDigraph cycle = new LongValueDigraph(2);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 0, -2);
        assertTrue(cycle.tryDijkstraIgnoringNegativeEdges(0).isEmpty());
        cycle.addEdge(0, 0, -1);
        assertTrue(cycle.tryDijkstraIgnoringNegativeEdges(0).isEmpty());
    }

    /** 固定サイズの非負辺・零閉路・多重辺を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void matchesDijkstraWithoutNegativeEdges() {
        LongValueDigraph g = new LongValueDigraph(4);
        g.addEdge(0, 1, 8);
        g.addEdge(0, 1, 2);
        g.addEdge(0, 2, 6);
        g.addEdge(1, 2, 0);
        g.addEdge(2, 1, 0);
        var result = g.tryDijkstraIgnoringNegativeEdges(0).orElseThrow();
        assertArrayEquals(g.dijkstra(0).dist(), result.dist());
        assertArrayEquals(new int[] {-1, 0, 1, -1}, result.parent());
    }

    /** 固定サイズの long 境界で加算の安全性を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void handlesLongBoundaries() {
        LongValueDigraph g = new LongValueDigraph(3);
        g.addEdge(0, 1, Long.MAX_VALUE - 1);
        g.addEdge(0, 2, 1);
        g.addEdge(1, 2, Long.MAX_VALUE);
        g.addEdge(1, 2, -(Long.MAX_VALUE - 2));
        assertArrayEquals(new long[] {0, Long.MAX_VALUE - 1, 1},
                g.tryDijkstraIgnoringNegativeEdges(0).orElseThrow().dist());
        g.addEdge(1, 2, Long.MIN_VALUE);
        assertTrue(g.tryDijkstraIgnoringNegativeEdges(0).isEmpty());
    }
}
