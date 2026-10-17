package library.util.seq;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class MultiPermutationTest {

    @Test
    public void testPositions() {
        int[] a = {1, 0, 1, 2};
        int[][] res = MultiPermutation.positions(a);

        assertEquals(4, res.length);
        assertArrayEquals(new int[]{1}, res[0]);
        assertArrayEquals(new int[]{0, 2}, res[1]);
        assertArrayEquals(new int[]{3}, res[2]);
        assertArrayEquals(new int[]{}, res[3]);
    }

    @Test
    public void testPositionsEmpty() {
        int[] a = {};
        int[][] res = MultiPermutation.positions(a);
        assertEquals(0, res.length);
    }

    @Test
    public void testPositionsAllSame() {
        int[] a = {2, 2, 2};
        int[][] res = MultiPermutation.positions(a);
        assertEquals(3, res.length);
        assertArrayEquals(new int[]{}, res[0]);
        assertArrayEquals(new int[]{}, res[1]);
        assertArrayEquals(new int[]{0, 1, 2}, res[2]);
    }

    @Test
    public void testPositionsRangeLimit() {
        // According to the class Javadoc, 0 <= a[i] < a.length is assumed.
        int[] a = {0, 1};
        int[][] res = MultiPermutation.positions(a);
        assertEquals(2, res.length);
        assertArrayEquals(new int[]{0}, res[0]);
        assertArrayEquals(new int[]{1}, res[1]);
    }

    @Test
    public void testOccurrencesStandard() {
        int[] a = {1, 0, 1, 2};
        int[] prev = MultiPermutation.prevOccurences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesNegative() {
        int[] a = {-1, -2, -1, 3};
        int[] prev = MultiPermutation.prevOccurences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesLargeValues() {
        int[] a = {10, 5, 10, 20};
        int[] prev = MultiPermutation.prevOccurences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesEmpty() {
        int[] a = {};
        int[] prev = MultiPermutation.prevOccurences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{}, prev);
        assertArrayEquals(new int[]{}, next);
    }

    @Test
    public void testOccurrencesExtremelyLarge() {
        int[] a = {1_000_000_000, -1_000_000_000, 1_000_000_000};
        int[] prev = MultiPermutation.prevOccurences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0}, prev);
        assertArrayEquals(new int[]{2, 3, 3}, next);
    }
}
