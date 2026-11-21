package library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ArrayUtilsArgSecondMinTest {

    @Test
    public void testIntDistinct() {
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{10, 20, 30}));
        assertEquals(0, ArrayUtils.argSecondMin(new int[]{20, 10, 30}));
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{30, 20, 10}));
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{5, 9}));
        assertEquals(0, ArrayUtils.argSecondMin(new int[]{9, 5}));
    }

    @Test
    public void testIntDuplicates() {
        // (a[i], i) tie-breaking:
        // [5, 5, 10] -> min at 0 (5), 2nd min at 1 (5)
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{5, 5, 10}));
        // [10, 5, 5] -> min at 1 (5), 2nd min at 2 (5)
        assertEquals(2, ArrayUtils.argSecondMin(new int[]{10, 5, 5}));
        // [5, 10, 5] -> min at 0 (5), 2nd min at 2 (5)
        assertEquals(2, ArrayUtils.argSecondMin(new int[]{5, 10, 5}));
        // [5, 5, 5] -> min at 0 (5), 2nd min at 1 (5)
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{5, 5, 5}));
        // [5, 5] -> min at 0, 2nd min at 1
        assertEquals(1, ArrayUtils.argSecondMin(new int[]{5, 5}));
        // [2, 10, 10, 2] -> (2, 0), (2, 3), (10, 1), (10, 2)
        assertEquals(3, ArrayUtils.argSecondMin(new int[]{2, 10, 10, 2}));
    }

    @Test
    public void testIntExceptions() {
        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new int[]{}));
        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new int[]{1}));
    }

    @Test
    public void testLong() {
        assertEquals(1, ArrayUtils.argSecondMin(new long[]{10L, 20L, 30L}));
        assertEquals(0, ArrayUtils.argSecondMin(new long[]{20L, 10L, 30L}));
        assertEquals(1, ArrayUtils.argSecondMin(new long[]{5L, 5L, 10L}));
        assertEquals(2, ArrayUtils.argSecondMin(new long[]{5L, 10L, 5L}));
        assertEquals(1, ArrayUtils.argSecondMin(new long[]{5L, 5L}));

        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new long[]{}));
        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new long[]{1L}));
    }

    @Test
    public void testDouble() {
        assertEquals(1, ArrayUtils.argSecondMin(new double[]{1.5, 2.5, 3.5}));
        assertEquals(0, ArrayUtils.argSecondMin(new double[]{2.5, 1.5, 3.5}));
        assertEquals(1, ArrayUtils.argSecondMin(new double[]{1.5, 1.5, 3.5}));
        assertEquals(2, ArrayUtils.argSecondMin(new double[]{1.5, 3.5, 1.5}));
        assertEquals(1, ArrayUtils.argSecondMin(new double[]{1.5, 1.5}));

        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new double[]{}));
        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new double[]{1.5}));
    }

    @Test
    public void testChar() {
        assertEquals(1, ArrayUtils.argSecondMin(new char[]{'a', 'b', 'c'}));
        assertEquals(0, ArrayUtils.argSecondMin(new char[]{'b', 'a', 'c'}));
        assertEquals(1, ArrayUtils.argSecondMin(new char[]{'a', 'a', 'c'}));
        assertEquals(2, ArrayUtils.argSecondMin(new char[]{'a', 'c', 'a'}));
        assertEquals(1, ArrayUtils.argSecondMin(new char[]{'a', 'a'}));

        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new char[]{}));
        assertThrows(AssertionError.class, () -> ArrayUtils.argSecondMin(new char[]{'a'}));
    }
}
