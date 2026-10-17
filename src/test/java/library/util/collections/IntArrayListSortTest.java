package library.util.collections;

import library.util.IntComparator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class IntArrayListSortTest {

    @Test
    public void testEmptyAndSingleElement() {
        IntArrayList listEmpty = new IntArrayList();
        listEmpty.sort((x, y) -> Integer.compare(x, y));
        assertArrayEquals(new int[0], listEmpty.toArray());

        IntArrayList listSingle = new IntArrayList();
        listSingle.add(42);
        listSingle.sort((x, y) -> Integer.compare(x, y));
        assertArrayEquals(new int[]{42}, listSingle.toArray());
    }

    @Test
    public void testAscendingAndDescending() {
        IntArrayList list = new IntArrayList();
        list.add(5);
        list.add(2);
        list.add(8);
        list.add(1);
        list.add(3);

        list.sort((x, y) -> Integer.compare(x, y));
        assertArrayEquals(new int[]{1, 2, 3, 5, 8}, list.toArray());

        list.sort((x, y) -> Integer.compare(y, x));
        assertArrayEquals(new int[]{8, 5, 3, 2, 1}, list.toArray());
    }

    @Test
    public void testCustomComparator() {
        IntArrayList list = new IntArrayList();
        list.add(-10);
        list.add(3);
        list.add(-2);
        list.add(7);
        list.add(0);

        // Sort by absolute value ascending
        IntComparator absCmp = (x, y) -> Integer.compare(Math.abs(x), Math.abs(y));
        list.sort(absCmp);
        assertArrayEquals(new int[]{0, -2, 3, 7, -10}, list.toArray());
    }

    @Test
    public void testDuplicates() {
        IntArrayList list = new IntArrayList();
        list.add(3);
        list.add(1);
        list.add(3);
        list.add(1);
        list.add(2);

        list.sort((x, y) -> Integer.compare(x, y));
        assertArrayEquals(new int[]{1, 1, 2, 3, 3}, list.toArray());
    }

    @Test
    public void testRandomizedStressTest() {
        Random rnd = new Random(123456);
        for (int iter = 0; iter < 100; iter++) {
            int n = rnd.nextInt(200);
            IntArrayList list = new IntArrayList();
            Integer[] expected = new Integer[n];
            for (int i = 0; i < n; i++) {
                int val = rnd.nextInt(1000) - 500;
                list.add(val);
                expected[i] = val;
            }

            IntComparator cmp = (x, y) -> {
                // Modulo 7 comparison then value comparison
                int m1 = Math.floorMod(x, 7);
                int m2 = Math.floorMod(y, 7);
                if (m1 != m2) return Integer.compare(m1, m2);
                return Integer.compare(x, y);
            };

            list.sort(cmp);

            Arrays.sort(expected, (x, y) -> {
                int m1 = Math.floorMod(x, 7);
                int m2 = Math.floorMod(y, 7);
                if (m1 != m2) return Integer.compare(m1, m2);
                return Integer.compare(x, y);
            });

            int[] actual = list.toArray();
            int[] expectedPrimitive = Arrays.stream(expected).mapToInt(Integer::intValue).toArray();

            assertArrayEquals(expectedPrimitive, actual, "Failed on iteration " + iter);
        }
    }
}
