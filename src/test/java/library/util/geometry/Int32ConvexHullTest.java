package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** 32-bit整数凸包の述語と逐次構築を検証する。 */
class Int32ConvexHullTest {
    /**
     * long中間値に収まる大きな整数座標の向きと、退化単体に対する摂動符号を検査する。
     * 計算量O(d!)、領域計算量O(d^2)。未テスト
     */
    @Test
    void longRangeOrientationAndSymbolicDegeneracy() {
        int[][] simplex = {
                {-1_000_000, -1_000_000},
                {1_000_000, -1_000_000},
                {-1_000_000, 1_000_000}
        };
        assertEquals(1, Int32ConvexHull.orientation(simplex));
        int[][] line = {{0, 0}, {1, 0}, {2, 0}};
        assertNotEquals(0, Int32ConvexHull.symbolicOrientation(line, new int[] {0, 1, 2}));
        assertEquals(-Int32ConvexHull.symbolicOrientation(line, new int[] {0, 1, 2}),
                Int32ConvexHull.symbolicOrientation(line, new int[] {1, 0, 2}));
    }

    /**
     * 4次元超立方体でconflict graph版の支持超平面と挿入後不変条件を検査する。
     * 計算量O(nF*d!)、領域計算量O(F+E)。未テスト
     */
    @Test
    void degenerateFourDimensionalCube() {
        int[][] cube = new int[16][4];
        for (int mask = 0; mask < 16; mask++) for (int axis = 0; axis < 4; axis++)
            cube[mask][axis] = (mask >>> axis) & 1;
        var hull = Int32ConvexHull.build(cube, 4, 29L, true);
        assertEquals(Int32ConvexHull.Status.FULL_DIMENSIONAL, hull.status());
        assertEquals(8, hull.supportingHyperplanes().size());
    }
}
