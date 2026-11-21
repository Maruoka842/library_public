package library.util.seq;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class MultiPermutationTest {

    @Test
    public void testOccurrencesStandard() {
        int[] a = {1, 0, 1, 2};
        int[] prev = MultiPermutation.prevOccurrences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesNegative() {
        int[] a = {-1, -2, -1, 3};
        int[] prev = MultiPermutation.prevOccurrences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesLargeValues() {
        int[] a = {10, 5, 10, 20};
        int[] prev = MultiPermutation.prevOccurrences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0, -1}, prev);
        assertArrayEquals(new int[]{2, 4, 4, 4}, next);
    }

    @Test
    public void testOccurrencesEmpty() {
        int[] a = {};
        int[] prev = MultiPermutation.prevOccurrences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{}, prev);
        assertArrayEquals(new int[]{}, next);
    }

    @Test
    public void testOccurrencesExtremelyLarge() {
        int[] a = {1_000_000_000, -1_000_000_000, 1_000_000_000};
        int[] prev = MultiPermutation.prevOccurrences(a);
        int[] next = MultiPermutation.nextOccurences(a);

        assertArrayEquals(new int[]{-1, -1, 0}, prev);
        assertArrayEquals(new int[]{2, 3, 3}, next);
    }
}
