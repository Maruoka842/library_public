package library.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ArrayUtilsTest {

    @Test
    public void testMaxExceptSelfLong() {
        long[] a = {10, 5, 10, 8};
        long[] expected = {10, 10, 10, 10};
        assertArrayEquals(expected, ArrayUtils.maxExceptSelf(a));

        long[] b = {10, 5, 3, 8};
        long[] expectedB = {8, 10, 10, 10};
        assertArrayEquals(expectedB, ArrayUtils.maxExceptSelf(b));

        long[] c = {10};
        long[] expectedC = {Long.MIN_VALUE};
        assertArrayEquals(expectedC, ArrayUtils.maxExceptSelf(c));
    }

    @Test
    public void testMaxExceptSelfInt() {
        int[] a = {10, 5, 10, 8};
        int[] expectedA = {10, 10, 10, 10};
        assertArrayEquals(expectedA, ArrayUtils.maxExceptSelf(a));

        int[] b = {10, 5, 3, 8};
        int[] expectedB = {8, 10, 10, 10};
        assertArrayEquals(expectedB, ArrayUtils.maxExceptSelf(b));

        int[] c = {10};
        int[] expectedC = {Integer.MIN_VALUE};
        assertArrayEquals(expectedC, ArrayUtils.maxExceptSelf(c));

        int[] d = {-3, -8, -1, -5};
        int[] expectedD = {-1, -1, -3, -1};
        assertArrayEquals(expectedD, ArrayUtils.maxExceptSelf(d));
    }

    @Test
    public void testMinExceptSelf() {
        long[] a = {3, 8, 3, 5};
        long[] expected = {3, 3, 3, 3};
        assertArrayEquals(expected, ArrayUtils.minExceptSelf(a));

        long[] b = {3, 8, 4, 5};
        long[] expectedB = {4, 3, 3, 3};
        assertArrayEquals(expectedB, ArrayUtils.minExceptSelf(b));
    }

    @Test
    public void testSecondMaxLong() {
        assertEquals(10, ArrayUtils.secondMax(new long[]{10, 5, 10, 8}));
        assertEquals(8, ArrayUtils.secondMax(new long[]{10, 5, 3, 8}));
        assertEquals(5, ArrayUtils.secondMax(new long[]{5, 5}));
        assertEquals(Long.MIN_VALUE, ArrayUtils.secondMax(new long[]{10}));
        assertEquals(Long.MIN_VALUE, ArrayUtils.secondMax(new long[]{}));
    }

    @Test
    public void testSecondMaxInt() {
        assertEquals(10, ArrayUtils.secondMax(new int[]{10, 5, 10, 8}));
        assertEquals(8, ArrayUtils.secondMax(new int[]{10, 5, 3, 8}));
        assertEquals(5, ArrayUtils.secondMax(new int[]{5, 5}));
        assertEquals(Integer.MIN_VALUE, ArrayUtils.secondMax(new int[]{10}));
        assertEquals(Integer.MIN_VALUE, ArrayUtils.secondMax(new int[]{}));
    }

    @Test
    public void testSecondMaxDouble() {
        assertEquals(10.0, ArrayUtils.secondMax(new double[]{10.0, 5.0, 10.0, 8.0}), 1e-9);
        assertEquals(8.0, ArrayUtils.secondMax(new double[]{10.0, 5.0, 3.0, 8.0}), 1e-9);
        assertEquals(5.0, ArrayUtils.secondMax(new double[]{5.0, 5.0}), 1e-9);
        assertEquals(Double.NEGATIVE_INFINITY, ArrayUtils.secondMax(new double[]{10.0}), 1e-9);
        assertEquals(Double.NEGATIVE_INFINITY, ArrayUtils.secondMax(new double[]{}), 1e-9);
    }

    @Test
    public void testSecondMinLong() {
        assertEquals(3, ArrayUtils.secondMin(new long[]{3, 8, 3, 5}));
        assertEquals(4, ArrayUtils.secondMin(new long[]{3, 8, 4, 5}));
        assertEquals(5, ArrayUtils.secondMin(new long[]{5, 5}));
        assertEquals(Long.MAX_VALUE, ArrayUtils.secondMin(new long[]{3}));
        assertEquals(Long.MAX_VALUE, ArrayUtils.secondMin(new long[]{}));
    }

    @Test
    public void testSecondMinInt() {
        assertEquals(3, ArrayUtils.secondMin(new int[]{3, 8, 3, 5}));
        assertEquals(4, ArrayUtils.secondMin(new int[]{3, 8, 4, 5}));
        assertEquals(5, ArrayUtils.secondMin(new int[]{5, 5}));
        assertEquals(Integer.MAX_VALUE, ArrayUtils.secondMin(new int[]{3}));
        assertEquals(Integer.MAX_VALUE, ArrayUtils.secondMin(new int[]{}));
    }

    @Test
    public void testSecondMinDouble() {
        assertEquals(3.0, ArrayUtils.secondMin(new double[]{3.0, 8.0, 3.0, 5.0}), 1e-9);
        assertEquals(4.0, ArrayUtils.secondMin(new double[]{3.0, 8.0, 4.0, 5.0}), 1e-9);
        assertEquals(5.0, ArrayUtils.secondMin(new double[]{5.0, 5.0}), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, ArrayUtils.secondMin(new double[]{3.0}), 1e-9);
        assertEquals(Double.POSITIVE_INFINITY, ArrayUtils.secondMin(new double[]{}), 1e-9);
    }

    @Test
    public void testCharGridRotation() {
        char[][] grid = {
            {'1', '2', '3'},
            {'4', '5', '6'}
        };
        char[][] rotatedLeft = {
            {'3', '6'},
            {'2', '5'},
            {'1', '4'}
        };
        char[][] rotatedRight = {
            {'4', '1'},
            {'5', '2'},
            {'6', '3'}
        };

        assertDeepEquals(rotatedLeft, ArrayUtils.rotateLeftGrid(grid));
        assertDeepEquals(rotatedRight, ArrayUtils.rotateRightGrid(grid));

        // Rotating left 4 times returns to the original
        char[][] current = grid;
        for (int i = 0; i < 4; i++) {
            current = ArrayUtils.rotateLeftGrid(current);
        }
        assertDeepEquals(grid, current);

        // Rotating right then left returns to the original
        assertDeepEquals(grid, ArrayUtils.rotateLeftGrid(ArrayUtils.rotateRightGrid(grid)));
        assertDeepEquals(grid, ArrayUtils.rotateRightGrid(ArrayUtils.rotateLeftGrid(grid)));
    }

    @Test
    public void testIntGridRotation() {
        int[][] grid = {
            {1, 2, 3},
            {4, 5, 6}
        };
        int[][] rotatedLeft = {
            {3, 6},
            {2, 5},
            {1, 4}
        };
        int[][] rotatedRight = {
            {4, 1},
            {5, 2},
            {6, 3}
        };

        assertDeepEquals(rotatedLeft, ArrayUtils.rotateLeftGrid(grid));
        assertDeepEquals(rotatedRight, ArrayUtils.rotateRightGrid(grid));

        // Rotating left 4 times returns to the original
        int[][] current = grid;
        for (int i = 0; i < 4; i++) {
            current = ArrayUtils.rotateLeftGrid(current);
        }
        assertDeepEquals(grid, current);

        // Rotating right then left returns to the original
        assertDeepEquals(grid, ArrayUtils.rotateLeftGrid(ArrayUtils.rotateRightGrid(grid)));
        assertDeepEquals(grid, ArrayUtils.rotateRightGrid(ArrayUtils.rotateLeftGrid(grid)));
    }

    @Test
    public void testLongGridRotation() {
        long[][] grid = {
            {1L, 2L, 3L},
            {4L, 5L, 6L}
        };
        long[][] rotatedLeft = {
            {3L, 6L},
            {2L, 5L},
            {1L, 4L}
        };
        long[][] rotatedRight = {
            {4L, 1L},
            {5L, 2L},
            {6L, 3L}
        };

        assertDeepEquals(rotatedLeft, ArrayUtils.rotateLeftGrid(grid));
        assertDeepEquals(rotatedRight, ArrayUtils.rightRotateGrid(grid));

        // Rotating left 4 times returns to the original
        long[][] current = grid;
        for (int i = 0; i < 4; i++) {
            current = ArrayUtils.rotateLeftGrid(current);
        }
        assertDeepEquals(grid, current);

        // Rotating right then left returns to the original
        assertDeepEquals(grid, ArrayUtils.rotateLeftGrid(ArrayUtils.rightRotateGrid(grid)));
        assertDeepEquals(grid, ArrayUtils.rightRotateGrid(ArrayUtils.rotateLeftGrid(grid)));
    }

    @Test
    public void testSuffixSumDouble() {
        double[] a = {1.5, 2.5, 3.5};
        double[] expected = {7.5, 6.0, 3.5};
        double[] expectedFromEmpty = {7.5, 6.0, 3.5, 0.0};
        assertArrayEquals(expected, ArrayUtils.suffixSum(a), 1e-9);
        assertArrayEquals(expectedFromEmpty, ArrayUtils.suffixSumFromEmpty(a), 1e-9);

        double[] empty = {};
        double[] expectedEmpty = {};
        double[] expectedEmptyFromEmpty = {0.0};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixSum(empty), 1e-9);
        assertArrayEquals(expectedEmptyFromEmpty, ArrayUtils.suffixSumFromEmpty(empty), 1e-9);
    }

    @Test
    public void testSuffixSumFloat() {
        float[] a = {1.5f, 2.5f, 3.5f};
        float[] expected = {7.5f, 6.0f, 3.5f};
        float[] expectedFromEmpty = {7.5f, 6.0f, 3.5f, 0.0f};
        assertArrayEquals(expected, ArrayUtils.suffixSum(a), 1e-6f);
        assertArrayEquals(expectedFromEmpty, ArrayUtils.suffixSumFromEmpty(a), 1e-6f);

        float[] empty = {};
        float[] expectedEmpty = {};
        float[] expectedEmptyFromEmpty = {0.0f};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixSum(empty), 1e-6f);
        assertArrayEquals(expectedEmptyFromEmpty, ArrayUtils.suffixSumFromEmpty(empty), 1e-6f);
    }

    @Test
    public void testSuffixSumLong() {
        long[] a = {10, 20, 30};
        long[] expected = {60, 50, 30};
        long[] expectedFromEmpty = {60, 50, 30, 0};
        assertArrayEquals(expected, ArrayUtils.suffixSum(a));
        assertArrayEquals(expectedFromEmpty, ArrayUtils.suffixSumFromEmpty(a));

        long[] empty = {};
        long[] expectedEmpty = {};
        long[] expectedEmptyFromEmpty = {0};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixSum(empty));
        assertArrayEquals(expectedEmptyFromEmpty, ArrayUtils.suffixSumFromEmpty(empty));
    }

    @Test
    public void testSuffixSumInt() {
        int[] a = {1, 2, 3};
        long[] expected = {6, 5, 3};
        long[] expectedFromEmpty = {6, 5, 3, 0};
        assertArrayEquals(expected, ArrayUtils.suffixSum(a));
        assertArrayEquals(expectedFromEmpty, ArrayUtils.suffixSumFromEmpty(a));

        int[] empty = {};
        long[] expectedEmpty = {};
        long[] expectedEmptyFromEmpty = {0};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixSum(empty));
        assertArrayEquals(expectedEmptyFromEmpty, ArrayUtils.suffixSumFromEmpty(empty));
    }

    @Test
    public void testSuffixMaxDouble() {
        double[] a = {3.5, 1.5, 4.5, 2.5};
        double[] expected = {4.5, 4.5, 4.5, 2.5};
        assertArrayEquals(expected, ArrayUtils.suffixMax(a), 1e-9);

        double[] empty = {};
        double[] expectedEmpty = {};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixMax(empty), 1e-9);

        // test inplace
        double[] c = {3.5, 1.5, 4.5, 2.5};
        ArrayUtils.suffixMaxInplace(c);
        assertArrayEquals(expected, c, 1e-9);
    }

    @Test
    public void testSuffixMaxFloat() {
        float[] a = {3.5f, 1.5f, 4.5f, 2.5f};
        float[] expected = {4.5f, 4.5f, 4.5f, 2.5f};
        assertArrayEquals(expected, ArrayUtils.suffixMax(a), 1e-6f);

        float[] empty = {};
        float[] expectedEmpty = {};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixMax(empty), 1e-6f);

        // test inplace
        float[] c = {3.5f, 1.5f, 4.5f, 2.5f};
        ArrayUtils.suffixMaxInplace(c);
        assertArrayEquals(expected, c, 1e-6f);
    }

    @Test
    public void testSuffixMaxInt() {
        int[] a = {3, 1, 4, 2};
        int[] expected = {4, 4, 4, 2};
        assertArrayEquals(expected, ArrayUtils.suffixMax(a));

        int[] empty = {};
        int[] expectedEmpty = {};
        assertArrayEquals(expectedEmpty, ArrayUtils.suffixMax(empty));

        // test inplace (since it was already implemented)
        int[] c = {3, 1, 4, 2};
        ArrayUtils.suffixMaxInplace(c);
        assertArrayEquals(expected, c);
    }

    @Test
    public void testSuffixMin() {
        long[] aLong = {3, 1, 4, 2};
        long[] expectedLong = {1, 1, 2, 2};
        assertArrayEquals(expectedLong, ArrayUtils.suffixMin(aLong));

        int[] aInt = {3, 1, 4, 2};
        int[] expectedInt = {1, 1, 2, 2};
        assertArrayEquals(expectedInt, ArrayUtils.suffixMin(aInt));

        double[] aDouble = {3.5, 1.5, 4.5, 2.5};
        double[] expectedDouble = {1.5, 1.5, 2.5, 2.5};
        assertArrayEquals(expectedDouble, ArrayUtils.suffixMin(aDouble), 1e-9);

        float[] aFloat = {3.5f, 1.5f, 4.5f, 2.5f};
        float[] expectedFloat = {1.5f, 1.5f, 2.5f, 2.5f};
        assertArrayEquals(expectedFloat, ArrayUtils.suffixMin(aFloat), 1e-6f);
    }

    @Test
    public void testGenericNthElement() {
        Integer[] a = {5, 2, 9, 1, 7, 6, 3};
        ArrayUtils.nthElement(a, 3, Integer::compare);
        assertEquals(5, a[3]);
        for (int i = 0; i < 3; i++) {
            assertTrue(a[i] <= a[3]);
        }
        for (int i = 4; i < a.length; i++) {
            assertTrue(a[i] >= a[3]);
        }
    }

    private void assertDeepEquals(char[][] expected, char[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    private void assertDeepEquals(int[][] expected, int[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    private void assertDeepEquals(long[][] expected, long[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void testRotateLeftInplace() {
        int[] emptyInt = new int[0];
        ArrayUtils.rotateLeftInplace(emptyInt, 3);
        assertEquals(0, emptyInt.length);

        int[] aInt = {1, 2, 3, 4, 5};
        ArrayUtils.rotateLeftInplace(aInt, 2);
        assertArrayEquals(new int[]{3, 4, 5, 1, 2}, aInt);

        ArrayUtils.rotateLeftInplace(aInt, -2);
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, aInt);

        ArrayUtils.rotateLeftInplace(aInt, 7);
        assertArrayEquals(new int[]{3, 4, 5, 1, 2}, aInt);

        ArrayUtils.rotateLeftInplace(aInt, 0);
        assertArrayEquals(new int[]{3, 4, 5, 1, 2}, aInt);

        long[] aLong = {10L, 20L, 30L, 40L};
        ArrayUtils.rotateLeftInplace(aLong, 1);
        assertArrayEquals(new long[]{20L, 30L, 40L, 10L}, aLong);

        ArrayUtils.rotateLeftInplace(aLong, -1);
        assertArrayEquals(new long[]{10L, 20L, 30L, 40L}, aLong);
    }
}
