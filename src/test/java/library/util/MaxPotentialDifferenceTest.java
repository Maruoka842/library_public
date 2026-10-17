package library.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class MaxPotentialDifferenceTest {
    /** 固定値の平行移動、元座標への復元、非負辺での探索を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void minimizesWithNonzeroFixedValue() {
        MaxPotentialDifference zero = new MaxPotentialDifference(2);
        zero.forceGeq(1, 0);
        zero.forceLeq(1, 10);
        MaxPotentialDifference shifted = new MaxPotentialDifference(2);
        shifted.forceGeq(1, 0);
        shifted.forceLeq(1, 10);
        assertArrayEquals(zero.minimize(0), shifted.minimize(0, 0));

        MaxPotentialDifference positive = new MaxPotentialDifference(2);
        positive.forceGeq(1, 0);
        positive.forceLeq(1, 10);
        assertArrayEquals(new long[] {5, 0}, positive.minimize(0, 5));
        assertTrue(positive.g.tryDijkstraIgnoringNegativeEdges(0).isPresent());
        for (var edge : positive.g.edges()) assertTrue(edge.cost >= 0);
    }

    /** 固定値に対する上下限制約の矛盾と差分制約系の結果を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void checksShiftedBoundsAtFixedVertex() {
        MaxPotentialDifference feasible = new MaxPotentialDifference(2);
        feasible.forceLeq(0, 8);
        feasible.forceGeq(1, 3);
        feasible.forceDiffLeq(1, 0, 2);
        assertArrayEquals(new long[] {7, 3}, feasible.minimize(0, 7));

        MaxPotentialDifference infeasible = new MaxPotentialDifference(1);
        infeasible.forceGeq(0, 8);
        assertNull(infeasible.minimize(0, 7));
    }

    /** 逆グラフで負辺無視が失敗する場合の上下限を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void reweightsReverseGraph() {
        MaxPotentialDifference solver = new MaxPotentialDifference(3);
        solver.forceDiffLeq(1, 0, 5);
        solver.forceDiffLeq(2, 0, 2);
        solver.forceDiffLeq(2, 1, -3);
        solver.forceDiffLeq(0, 1, 10);
        solver.forceDiffLeq(0, 2, 4);
        assertTrue(solver.g.tryDijkstraIgnoringNegativeEdges(0).isPresent());
        assertTrue(solver.g.reverse().tryDijkstraIgnoringNegativeEdges(0).isEmpty());
        var range = solver.getRange(0);
        assertNotNull(range);
        assertArrayEquals(new long[] {0, 5, 2}, range.max());
        assertArrayEquals(new long[] {0, -1, -4}, range.min());
        assertArrayEquals(range.min(), solver.minimize(0));
    }

    /** 順方向・逆方向の到達不能を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void handlesUnreachableVertices() {
        MaxPotentialDifference solver = new MaxPotentialDifference(2);
        solver.forceDiffLeq(0, 1, -2);
        var range = solver.getRange(0);
        assertArrayEquals(new long[] {0, Long.MAX_VALUE}, range.max());
        assertArrayEquals(new long[] {0, 2}, range.min());
        MaxPotentialDifference outward = new MaxPotentialDifference(3);
        outward.forceDiffLeq(1, 0, 5);
        outward.forceDiffLeq(2, 0, 2);
        outward.forceDiffLeq(2, 1, -3);
        assertArrayEquals(new long[] {0, Long.MIN_VALUE, Long.MIN_VALUE}, outward.getRange(0).min());
    }

    /** 元グラフの負辺無視が失敗する場合と負閉路を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void fallsBackWhenForwardDistancesChange() {
        MaxPotentialDifference solver = new MaxPotentialDifference(2);
        solver.forceDiffLeq(1, 0, -2);
        solver.forceDiffLeq(0, 1, 3);
        var range = solver.getRange(0);
        assertArrayEquals(new long[] {0, -2}, range.max());
        assertArrayEquals(new long[] {0, -3}, range.min());
        solver.forceDiffLeq(0, 1, 1);
        assertNull(solver.getRange(0));
    }

    /** 変換後の有限距離が Long.MAX_VALUE になる場合を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void fallsBackWhenReweightedDistanceIsTooLarge() {
        MaxPotentialDifference solver = new MaxPotentialDifference(3);
        solver.forceDiffLeq(1, 0, Long.MAX_VALUE - 1);
        solver.forceDiffLeq(2, 0, 0);
        solver.forceDiffLeq(2, 1, -1);
        solver.forceDiffLeq(0, 1, 1);
        var range = solver.getRange(0);
        assertArrayEquals(new long[] {0, Long.MAX_VALUE - 1, 0}, range.max());
        assertArrayEquals(new long[] {0, -1, Long.MIN_VALUE}, range.min());
    }

    /** forceGeq と forceRange の設定と上下限計算を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void testForceGeqAndForceRange() {
        MaxPotentialDifference solver = new MaxPotentialDifference(3);
        solver.forceRange(1, 3, 7);
        solver.forceGeq(2, -5);
        solver.forceLeq(2, 10);
        var range = solver.getRange(0);
        assertNotNull(range);
        assertArrayEquals(new long[] {0, 3, -5}, range.min());
        assertArrayEquals(new long[] {0, 7, 10}, range.max());

        long[] min = solver.minimize(0);
        long[] max = solver.maximize(0);
        assertArrayEquals(new long[] {0, 3, -5}, min);
        assertArrayEquals(new long[] {0, 7, 10}, max);
    }

    /** 矛盾する forceRange (lo > hi) や fix 自身の不整合で null が返されることを検証する。計算量 O(1)。未テスト。 */
    @Test
    public void testInfeasibleForceRange() {
        MaxPotentialDifference solver = new MaxPotentialDifference(2);
        solver.forceRange(1, 10, 5);
        assertNull(solver.getRange(0));
        assertNull(solver.maximize(0));
        assertNull(solver.minimize(0));

        MaxPotentialDifference solverFixInfeasible = new MaxPotentialDifference(1);
        solverFixInfeasible.forceGeq(0, 5);
        assertNull(solverFixInfeasible.getRange(0));

        MaxPotentialDifference solverFixLeqInfeasible = new MaxPotentialDifference(1);
        solverFixLeqInfeasible.forceLeq(0, -3);
        assertNull(solverFixLeqInfeasible.getRange(0));
    }

    /** forceDiffGeq と forceDiffRange の設定と上下限計算を検証する。計算量 O(1)。未テスト。 */
    @Test
    public void testForceDiffGeqAndForceDiffRange() {
        MaxPotentialDifference solver = new MaxPotentialDifference(3);
        // v[1] - v[0] >= 3, v[1] - v[0] <= 7  => 3 <= v[1] - v[0] <= 7
        solver.forceDiffRange(1, 0, 3, 7);
        // v[2] - v[1] >= -2 => v[2] - v[1] >= -2
        solver.forceDiffGeq(2, 1, -2);
        // v[2] - v[1] <= 4
        solver.forceDiffLeq(2, 1, 4);

        var range = solver.getRange(0);
        assertNotNull(range);
        assertArrayEquals(new long[] {0, 3, 1}, range.min());
        assertArrayEquals(new long[] {0, 7, 11}, range.max());

        assertArrayEquals(new long[] {0, 3, 1}, solver.minimize(0));
        assertArrayEquals(new long[] {0, 7, 11}, solver.maximize(0));
    }

    /** 矛盾する forceDiffRange (lo > hi) や差分閉路の矛盾で null が返されることを検証する。計算量 O(1)。未テスト。 */
    @Test
    public void testInfeasibleForceDiffRange() {
        MaxPotentialDifference solver = new MaxPotentialDifference(2);
        // v[1] - v[0] >= 10 and v[1] - v[0] <= 5 (infeasible lo > hi)
        solver.forceDiffRange(1, 0, 10, 5);
        assertNull(solver.getRange(0));
        assertNull(solver.maximize(0));
        assertNull(solver.minimize(0));

        MaxPotentialDifference cycleInfeasible = new MaxPotentialDifference(3);
        // v[1] - v[0] >= 5, v[2] - v[1] >= 5, v[0] - v[2] >= -8 (so v[0] - v[0] >= 2, contradiction)
        cycleInfeasible.forceDiffGeq(1, 0, 5);
        cycleInfeasible.forceDiffGeq(2, 1, 5);
        cycleInfeasible.forceDiffGeq(0, 2, -8);
        assertNull(cycleInfeasible.getRange(0));
        assertNull(cycleInfeasible.maximize(0));
        assertNull(cycleInfeasible.minimize(0));
    }
}
