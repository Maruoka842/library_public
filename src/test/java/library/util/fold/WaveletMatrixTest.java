package library.util.fold;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class WaveletMatrixTest {

    private int naiveKthIndexInRangeFiltered(long[] a, int l, int r, long lo, long hi, int k) {
        if (l < 0 || r > a.length || l > r) return -1;
        if (lo >= hi) return -1;
        if (k < 0) return -1;
        int count = 0;
        for (int i = l; i < r; i++) {
            if (a[i] >= lo && a[i] < hi) {
                if (count == k) {
                    return i;
                }
                count++;
            }
        }
        return -1;
    }

    private int naiveKthIndexInRangeFiltered(int[] a, int l, int r, long lo, long hi, int k) {
        long[] la = new long[a.length];
        for (int i = 0; i < a.length; i++) la[i] = a[i];
        return naiveKthIndexInRangeFiltered(la, l, r, lo, hi, k);
    }

    @Test
    public void testEmptyArray() {
        int[] a = new int[0];
        WaveletMatrix wm = new WaveletMatrix(a);
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 0, 0, 10, 0));
    }

    @Test
    public void testSingleElement() {
        int[] a = { 5 };
        WaveletMatrix wm = new WaveletMatrix(a);
        assertEquals(0, wm.kthIndexInRangeFiltered(0, 1, 0, 10, 0));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 1, 0, 10, 1));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 1, 6, 10, 0));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 1, 0, 5, 0));
    }

    @Test
    public void testMultipleDuplicates() {
        int[] a = { 3, 5, 3, 8, 3, 5, 9 };
        WaveletMatrix wm = new WaveletMatrix(a);

        // Find 3s in range [0, 7) (indices 0, 2, 4)
        assertEquals(0, wm.kthIndexInRangeFiltered(0, 7, 3, 4, 0));
        assertEquals(2, wm.kthIndexInRangeFiltered(0, 7, 3, 4, 1));
        assertEquals(4, wm.kthIndexInRangeFiltered(0, 7, 3, 4, 2));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 7, 3, 4, 3));

        // Find 3s in range [1, 5) (indices 2, 4)
        assertEquals(2, wm.kthIndexInRangeFiltered(1, 5, 3, 4, 0));
        assertEquals(4, wm.kthIndexInRangeFiltered(1, 5, 3, 4, 1));
        assertEquals(-1, wm.kthIndexInRangeFiltered(1, 5, 3, 4, 2));
    }

    @Test
    public void testEdgeCases() {
        int[] a = { 2, 4, 6, 8 };
        WaveletMatrix wm = new WaveletMatrix(a);

        // Invalid ranges
        assertEquals(-1, wm.kthIndexInRangeFiltered(-1, 3, 0, 10, 0));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 5, 0, 10, 0));
        assertEquals(-1, wm.kthIndexInRangeFiltered(3, 2, 0, 10, 0));

        // lo >= hi
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 4, 5, 5, 0));
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 4, 6, 5, 0));

        // Negative k
        assertEquals(-1, wm.kthIndexInRangeFiltered(0, 4, 0, 10, -1));
    }

    @Test
    public void testSuffixCompatibility() {
        long[] a = { 10, 5, 2, 8, 15, 3, 7, 12, 1 };
        WaveletMatrix wm = new WaveletMatrix(a);
        int n = a.length;

        for (int start = 0; start < n; start++) {
            for (long lo = 0; lo < 20; lo++) {
                for (long hi = lo + 1; hi <= 20; hi++) {
                    for (int k = 0; k < n; k++) {
                        int suffixResult = wm.kthIndexInRangeFilteredSuffix(start, lo, hi, k);
                        int rangeResult = wm.kthIndexInRangeFiltered(start, n, lo, hi, k);
                        assertEquals(suffixResult, rangeResult,
                            "Mismatch at start=" + start + ", lo=" + lo + ", hi=" + hi + ", k=" + k);
                    }
                }
            }
        }
    }

    @Test
    public void testRandomIntArray() {
        Random rnd = new Random(42);
        int n = 100;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt(50);
        }

        WaveletMatrix wm = new WaveletMatrix(a);

        for (int i = 0; i < 1000; i++) {
            int l = rnd.nextInt(n);
            int r = rnd.nextInt(n - l + 1) + l;
            long lo = rnd.nextInt(30);
            long hi = rnd.nextInt(30) + lo;
            int k = rnd.nextInt(15);

            int expected = naiveKthIndexInRangeFiltered(a, l, r, lo, hi, k);
            int actual = wm.kthIndexInRangeFiltered(l, r, lo, hi, k);
            assertEquals(expected, actual,
                "Mismatch at step " + i + " [l, r)=[" + l + ", " + r + ") lo=" + lo + " hi=" + hi + " k=" + k);
        }
    }

    @Test
    public void testRandomLongArray() {
        Random rnd = new Random(43);
        int n = 100;
        long[] a = new long[n];
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt(50);
        }

        WaveletMatrix wm = new WaveletMatrix(a);

        for (int i = 0; i < 1000; i++) {
            int l = rnd.nextInt(n);
            int r = rnd.nextInt(n - l + 1) + l;
            long lo = rnd.nextInt(30);
            long hi = rnd.nextInt(30) + lo;
            int k = rnd.nextInt(15);

            int expected = naiveKthIndexInRangeFiltered(a, l, r, lo, hi, k);
            int actual = wm.kthIndexInRangeFiltered(l, r, lo, hi, k);
            assertEquals(expected, actual,
                "Mismatch at step " + i + " [l, r)=[" + l + ", " + r + ") lo=" + lo + " hi=" + hi + " k=" + k);
        }
    }
}
