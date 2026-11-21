package library.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArrayUtils2DTest {

    private static void assertDeepEquals(char[][] expected, char[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    private static void assertDeepEquals(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    private static void assertDeepEquals(long[][] expected, long[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testRotations() {
        int[][] grid = {
            {1, 2, 3},
            {4, 5, 6}
        };
        int[][] right = ArrayUtils2D.rotateRightGrid(grid);
        int[][] expectedRight = {
            {4, 1},
            {5, 2},
            {6, 3}
        };
        assertDeepEquals(expectedRight, right);

        int[][] left = ArrayUtils2D.rotateLeftGrid(grid);
        int[][] expectedLeft = {
            {3, 6},
            {2, 5},
            {1, 4}
        };
        assertDeepEquals(expectedLeft, left);

        long[][] lGrid = {
            {1L, 2L, 3L},
            {4L, 5L, 6L}
        };
        assertDeepEquals(new long[][]{{4L, 1L}, {5L, 2L}, {6L, 3L}}, ArrayUtils2D.rightRotateGrid(lGrid));
        assertDeepEquals(new long[][]{{3L, 6L}, {2L, 5L}, {1L, 4L}}, ArrayUtils2D.rotateLeftGrid(lGrid));

        char[][] cGrid = {
            {'a', 'b', 'c'},
            {'d', 'e', 'f'}
        };
        assertDeepEquals(new char[][]{{'d', 'a'}, {'e', 'b'}, {'f', 'c'}}, ArrayUtils2D.rotateRightGrid(cGrid));
        assertDeepEquals(new char[][]{{'c', 'f'}, {'b', 'e'}, {'a', 'd'}}, ArrayUtils2D.rotateLeftGrid(cGrid));

        // 180 rotation
        int[][] rot180 = ArrayUtils2D.copy(grid);
        ArrayUtils2D.rotate180(rot180);
        assertDeepEquals(new int[][]{{6, 5, 4}, {3, 2, 1}}, rot180);

        long[][] rot180Long = ArrayUtils2D.copy(lGrid);
        ArrayUtils2D.rotate180(rot180Long);
        assertDeepEquals(new long[][]{{6L, 5L, 4L}, {3L, 2L, 1L}}, rot180Long);

        char[][] rot180Char = ArrayUtils2D.copy(cGrid);
        ArrayUtils2D.rotate180(rot180Char);
        assertDeepEquals(new char[][]{{'f', 'e', 'd'}, {'c', 'b', 'a'}}, rot180Char);

        // rotateRightJagged
        char[][] jagged = {
            {'a', 'b'},
            {'c'}
        };
        char[][] rotJagged = ArrayUtils2D.rotateRightJagged(jagged, '.');
        assertNotNull(rotJagged);
    }

    @Test
    public void testTransposeReshapeFlatten() {
        int[][] a = {
            {1, 2, 3},
            {4, 5, 6}
        };
        int[][] tInt = ArrayUtils2D.transpose(a);
        assertDeepEquals(new int[][]{{1, 4}, {2, 5}, {3, 6}}, tInt);

        long[][] l = {
            {10L, 20L},
            {30L, 40L}
        };
        assertDeepEquals(new long[][]{{10L, 30L}, {20L, 40L}}, ArrayUtils2D.transpose(l));

        char[][] c = {
            {'x', 'y'},
            {'z', 'w'}
        };
        assertDeepEquals(new char[][]{{'x', 'z'}, {'y', 'w'}}, ArrayUtils2D.transpose(c));

        int[] flat = ArrayUtils2D.flatten(a);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, flat);

        int[][] reshaped = ArrayUtils.reshape(flat, 2, 3);
        assertDeepEquals(a, reshaped);

        long[] lflat = {1L, 2L, 3L, 4L};
        long[][] lreshaped = ArrayUtils.reshape(lflat, 2, 2);
        assertDeepEquals(new long[][]{{1L, 2L}, {3L, 4L}}, lreshaped);
    }

    @Test
    public void testColumnOperations() {
        int[][] a = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        assertArrayEquals(new int[]{2, 5, 8}, ArrayUtils2D.getColumn(a, 1));
        assertArrayEquals(new char[]{'b', 'e'}, ArrayUtils2D.getColumn(new char[][]{{'a', 'b'}, {'d', 'e'}}, 1));
        assertArrayEquals(new long[]{20L, 50L}, ArrayUtils2D.getColumn(new long[][]{{10L, 20L}, {40L, 50L}}, 1));
        assertArrayEquals(new double[]{2.0, 5.0}, ArrayUtils2D.getColumn(new double[][]{{1.0, 2.0}, {4.0, 5.0}}, 1));

        long[][] swapL = {
            {1L, 2L, 3L},
            {4L, 5L, 6L}
        };
        ArrayUtils2D.swapColumns(0, 2, swapL);
        assertDeepEquals(new long[][]{{3L, 2L, 1L}, {6L, 5L, 4L}}, swapL);

        String[][] swapObj = {
            {"a", "b"},
            {"c", "d"}
        };
        ArrayUtils2D.swapColumns(0, 1, swapObj);
        assertEquals("b", swapObj[0][0]);
        assertEquals("a", swapObj[0][1]);

        int[][] rotCol = {
            {1, 10},
            {2, 20},
            {3, 30}
        };
        ArrayUtils2D.rotateColumnUpInplace(rotCol, 0, 0, 3, 1);
        assertArrayEquals(new int[]{2, 10}, rotCol[0]);
        assertArrayEquals(new int[]{3, 20}, rotCol[1]);
        assertArrayEquals(new int[]{1, 30}, rotCol[2]);

        ArrayUtils2D.reverseColumn(rotCol, 0, 0, 3);
        assertArrayEquals(new int[]{1, 10}, rotCol[0]);
        assertArrayEquals(new int[]{3, 20}, rotCol[1]);
        assertArrayEquals(new int[]{2, 30}, rotCol[2]);
    }

    @Test
    public void testReverseHorizontallyAndVertically() {
        int[][] a = {
            {1, 2, 3},
            {4, 5, 6}
        };
        ArrayUtils2D.reverseHolizontally(a);
        assertDeepEquals(new int[][]{{3, 2, 1}, {6, 5, 4}}, a);

        ArrayUtils2D.reverseVertically(a);
        assertDeepEquals(new int[][]{{6, 5, 4}, {3, 2, 1}}, a);

        long[][] l = {
            {1L, 2L},
            {3L, 4L}
        };
        ArrayUtils2D.reverseVertically(l);
        assertDeepEquals(new long[][]{{3L, 4L}, {1L, 2L}}, l);
    }

    @Test
    public void testCopyAndRange() {
        int[][] a = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int[][] sub = ArrayUtils2D.copyOfRange2D(a, 0, 1, 2, 3);
        assertDeepEquals(new int[][]{{2, 3}, {5, 6}}, sub);

        long[][] l = {
            {10L, 20L, 30L},
            {40L, 50L, 60L}
        };
        long[][] lSub = ArrayUtils2D.copyOfRange2D(l, 0, 0, 2, 2);
        assertDeepEquals(new long[][]{{10L, 20L}, {40L, 50L}}, lSub);

        int[][] copyA = ArrayUtils2D.copy(a);
        assertTrue(ArrayUtils2D.equals(a, copyA));
        copyA[0][0] = 999;
        assertEquals(1, a[0][0]);

        long[][] copyL = ArrayUtils2D.copy(l);
        assertTrue(ArrayUtils2D.equals(l, copyL));

        char[][] c = {{'a', 'b'}};
        char[][] copyC = ArrayUtils2D.copy(c);
        assertTrue(ArrayUtils2D.equals(c, copyC));

        boolean[][] b = {{true, false}};
        boolean[][] copyB = ArrayUtils2D.copy(b);
        assertTrue(ArrayUtils2D.equals(b, copyB));

        double[][] d = {{1.5, 2.5}};
        double[][] copyD = ArrayUtils2D.copy(d);
        assertEquals(d[0][0], copyD[0][0], 1e-9);

        long[][] conv = ArrayUtils2D.tolong(new int[][]{{1, 2}, {3, 4}});
        assertDeepEquals(new long[][]{{1L, 2L}, {3L, 4L}}, conv);
    }

    @Test
    public void testFillAndBoundsAndDecrement() {
        int[][] a = new int[2][3];
        ArrayUtils2D.fill(a, 5);
        for (int[] row : a) {
            for (int val : row) {
                assertEquals(5, val);
            }
        }
        ArrayUtils2D.decrementAll(a);
        for (int[] row : a) {
            for (int val : row) {
                assertEquals(4, val);
            }
        }

        assertTrue(ArrayUtils2D.checkBounds(1, 2, a));
        assertFalse(ArrayUtils2D.checkBounds(2, 0, a));
        assertFalse(ArrayUtils2D.checkBounds(-1, 0, a));

        long[][] l = new long[2][2];
        ArrayUtils2D.fill(l, 10L);
        assertTrue(ArrayUtils2D.checkBounds(0, 1, l));
        assertFalse(ArrayUtils2D.checkBounds(0, 2, l));
    }

    @Test
    public void testPrefixOperations() {
        long[][] a = {
            {1, 2},
            {3, 4}
        };
        long[][] pref = ArrayUtils2D.prefixSum(a);
        assertDeepEquals(new long[][]{{1, 3}, {4, 10}}, pref);

        long[][] diff = ArrayUtils2D.prefixDiff(pref);
        assertDeepEquals(a, diff);

        double[][] d = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        double[][] dPref = ArrayUtils2D.prefixSum(d);
        assertEquals(10.0, dPref[1][1], 1e-9);

        long[][] prefMax = ArrayUtils2D.prefixMax(new long[][]{{1, 5}, {4, 2}});
        assertDeepEquals(new long[][]{{1, 5}, {4, 5}}, prefMax);

        long[][] prefMaxLB = ArrayUtils2D.prefixMaxFromLeftBottom(new long[][]{{1, 2}, {4, 3}});
        assertDeepEquals(new long[][]{{4, 4}, {4, 4}}, prefMaxLB);
    }

    @Test
    public void testAggregations() {
        int[][] a = {
            {1, 2},
            {3, 4}
        };
        assertEquals(10L, ArrayUtils2D.sum(a));
        assertEquals(4, ArrayUtils2D.max(a));
        assertEquals(1, ArrayUtils2D.min(a));

        long[][] l = {
            {10L, 20L},
            {30L, 40L}
        };
        assertEquals(100L, ArrayUtils2D.sum(l));
        assertEquals(40L, ArrayUtils2D.max(l));
        assertEquals(10L, ArrayUtils2D.min(l));

        assertEquals(100L % 7, ArrayUtils2D.modSum(l, 7));
        assertEquals(10L % 7, ArrayUtils2D.modSum(a, 7));

        double[][] d = {
            {1.5, 2.5},
            {3.0, 4.0}
        };
        assertEquals(11.0, ArrayUtils2D.sum(d), 1e-9);
    }

    @Test
    public void testSortAndTake() {
        int[][] a = {
            {3, 1},
            {1, 5},
            {1, 2}
        };
        ArrayUtils2D.sort(a);
        assertDeepEquals(new int[][]{{1, 2}, {1, 5}, {3, 1}}, a);

        ArrayUtils2D.rsort(a);
        assertDeepEquals(new int[][]{{3, 1}, {1, 5}, {1, 2}}, a);

        char[][] c = {
            {'b', 'a'},
            {'a', 'b'},
            {'a', 'b'}
        };
        char[][] uniqueC = ArrayUtils2D.sortq(c);
        assertDeepEquals(new char[][]{{'a', 'b'}, {'b', 'a'}}, uniqueC);

        int[] arg = ArrayUtils2D.argSort(new char[][]{{'b'}, {'a'}});
        assertArrayEquals(new int[]{1, 0}, arg);

        int[][] taken = ArrayUtils2D.take(a, new int[]{0, 2});
        assertDeepEquals(new int[][]{{3, 1}, {1, 2}}, taken);

        int[][] taken2D = ArrayUtils2D.take(new int[][]{{10, 20, 30}, {40, 50, 60}}, new int[]{1, 0}, new int[]{2, 0});
        assertDeepEquals(new int[][]{{60, 40}, {30, 10}}, taken2D);
    }
}
