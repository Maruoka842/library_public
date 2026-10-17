package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Random;

public class StaticRangeFirstMissingTest {

    @Test
    public void testBasic() {
        int[] a = {0, 1, 2, 3, 4};
        StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

        // Entire array [0, 5) with lower=0. Missing should be 5.
        assertEquals(5, query.firstMissing(0, 5, 0));

        // Subsegment [1, 4) which is {1, 2, 3}. Missing >= 0 is 0.
        assertEquals(0, query.firstMissing(1, 4, 0));

        // Subsegment [0, 3) which is {0, 1, 2}. Missing >= 1 is 3.
        assertEquals(3, query.firstMissing(0, 3, 1));

        // Subsegment [2, 5) which is {2, 3, 4}. Missing >= 2 is 5.
        assertEquals(5, query.firstMissing(2, 5, 2));

        // Subsegment [2, 5) which is {2, 3, 4}. Missing >= 3 is 5.
        assertEquals(5, query.firstMissing(2, 5, 3));

        // Subsegment [2, 5) which is {2, 3, 4}. Missing >= 0 is 0.
        assertEquals(0, query.firstMissing(2, 5, 0));
    }

    @Test
    public void testSingleElement() {
        int[] a = {0};
        StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

        assertEquals(1, query.firstMissing(0, 1, 0));
        assertEquals(1, query.firstMissing(0, 1, 1));
    }

    @Test
    public void testEmptyAndSingleSubsegments() {
        int[] a = {0, 3, 0, 1}; // n = 4
        StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

        // Empty range [1, 1)
        assertEquals(0, query.firstMissing(1, 1, 0));
        assertEquals(4, query.firstMissing(1, 1, 4));

        // Range [1, 2) which is {3}.
        assertEquals(0, query.firstMissing(1, 2, 0));
        assertEquals(4, query.firstMissing(1, 2, 3));
    }

    @Test
    public void testLargeLower() {
        int[] a = {0, 1, 2};
        StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

        // lower > n
        assertEquals(10, query.firstMissing(0, 3, 10));
    }

    @Test
    public void testExceptions() {
        int[] a = {0, 1, 2};
        StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

        assertThrows(IndexOutOfBoundsException.class, () -> query.firstMissing(-1, 2, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> query.firstMissing(1, 4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> query.firstMissing(2, 1, 0));
    }

    @Test
    public void testRandomStress() {
        Random rnd = new Random(42);
        int numArrays = 100;
        int maxN = 100;

        for (int t = 0; t < numArrays; t++) {
            int n = rnd.nextInt(maxN) + 1;
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                // Elements of a are non-negative and <= n
                a[i] = rnd.nextInt(n + 1);
            }

            StaticRangeFirstMissing query = new StaticRangeFirstMissing(a);

            // Run 200 random queries on this array
            for (int q = 0; q < 200; q++) {
                int l = rnd.nextInt(n + 1);
                int r = rnd.nextInt(n + 1);
                if (l > r) {
                    int tmp = l;
                    l = r;
                    r = tmp;
                }

                // lower is non-negative and <= n
                int lower = rnd.nextInt(n + 1);

                // Brute force calculation
                int expected = bruteForce(a, l, r, lower);
                int actual = query.firstMissing(l, r, lower);
                assertEquals(expected, actual, "Failed on array " + java.util.Arrays.toString(a) + ", l=" + l + ", r=" + r + ", lower=" + lower);
            }
        }
    }

    private int bruteForce(int[] a, int l, int r, int lower) {
        HashSet<Integer> set = new HashSet<>();
        for (int i = l; i < r; i++) {
            set.add(a[i]);
        }
        int cur = lower;
        while (set.contains(cur)) {
            cur++;
        }
        return cur;
    }
}
