package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.segtree.OfflineStatic_RectAdd_RectSum.Rectangle;

public class OfflineStatic_RectAdd_RectSumTest {
    @Test
    public void sumsSingleRectangleAndArbitraryIntersections() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
        rectangles.rectAdd(10, 20, 30, 40, 3);
        List<Rectangle> queries = List.of(
                new Rectangle(0, 0, 50, 50),
                new Rectangle(15, 25, 20, 35),
                new Rectangle(0, 30, 15, 50),
                new Rectangle(-10, -10, 0, 0));

        assertArrayEquals(new long[] {1200, 150, 150, 0}, rectangles.solve(queries));
    }

    @Test
    public void handlesOverlapBaseNegativeCoordinatesAndEmptyRectangles() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum(3);
        rectangles.addAll(2);
        rectangles.rectAdd(-5, -4, 5, 6, 1);
        rectangles.rectAdd(0, 0, 10, 10, 3);
        rectangles.rectAdd(0, 0, 0, 10, 100);
        List<Rectangle> queries = List.of(
                new Rectangle(-10, -10, 10, 10),
                new Rectangle(-2, -2, 2, 2),
                new Rectangle(1, 1, 1, 8),
                new Rectangle(20, 20, 30, 30));

        assertArrayEquals(new long[] {1200, 60, 0, 200}, rectangles.solve(queries));
        assertArrayEquals(new long[] {1200, 60, 0, 200}, rectangles.solve(queries));
    }

    @Test
    public void agreesWithUnitGridBruteForce() {
        Random random = new Random(1);
        for (int iteration = 0; iteration < 100; iteration++) {
            OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
            long[][] grid = new long[8][8];
            for (int update = 0; update < 15; update++) {
                int x0 = random.nextInt(9);
                int x1 = x0 + random.nextInt(9 - x0);
                int y0 = random.nextInt(9);
                int y1 = y0 + random.nextInt(9 - y0);
                int add = random.nextInt(11) - 5;
                rectangles.rectAdd(x0, y0, x1, y1, add);
                for (int x = x0; x < x1; x++) {
                    for (int y = y0; y < y1; y++) grid[x][y] += add;
                }
            }
            long[] expected = new long[20];
            List<Rectangle> queries = new ArrayList<>();
            for (int query = 0; query < expected.length; query++) {
                int x0 = random.nextInt(9);
                int x1 = x0 + random.nextInt(9 - x0);
                int y0 = random.nextInt(9);
                int y1 = y0 + random.nextInt(9 - y0);
                queries.add(new Rectangle(x0, y0, x1, y1));
                for (int x = x0; x < x1; x++) {
                    for (int y = y0; y < y1; y++) expected[query] += grid[x][y];
                }
            }
            assertArrayEquals(expected, rectangles.solve(queries));
        }
    }

    @Test
    public void validatesBoundsAndCapacities() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
        assertThrows(AssertionError.class, () -> rectangles.prefixRectSum(0, 1));
        assertThrows(AssertionError.class,
                () -> rectangles.solve(List.of(new Rectangle(1, 0, 0, 1))));
        assertThrows(AssertionError.class,
                () -> rectangles.solve(List.of(new Rectangle(0, 1, 1, 0))));
        assertThrows(IllegalArgumentException.class, () -> new OfflineStatic_RectAdd_RectSum(-1));
    }

    @Test
    public void answersPrefixRectanglesOnlineInNonDecreasingXOrder() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
        rectangles.addAll(2);
        rectangles.rectAdd(-5, 0, 5, 10, 3);
        rectangles.rectAdd(3, 5, 8, 15, 1);
        rectangles.build();

        assertEquals(150, rectangles.prefixRectSum(0, 100));
        assertEquals(0, rectangles.prefixRectSum(0, -100));
        assertEquals(250, rectangles.prefixRectSum(2, 10));
        assertEquals(355, rectangles.prefixRectSum(4, 10));
        assertEquals(400, rectangles.prefixRectSum(4, 15));
        assertEquals(175, rectangles.prefixRectSum(4, 5));
        assertEquals(590, rectangles.prefixRectSum(8, 15));

        assertThrows(AssertionError.class, () -> rectangles.prefixRectSum(7, 10));
        rectangles.build();
        assertEquals(250, rectangles.prefixRectSum(2, 10));
        assertThrows(AssertionError.class,
                () -> rectangles.solve(List.of(new Rectangle(0, 0, 1, 1))));
        assertEquals(250, rectangles.prefixRectSum(2, 10));
    }

    @Test
    public void answersTwoDimensionalPrefixPotential() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
        rectangles.addAll(1);
        rectangles.rectAdd(0, 0, 4, 5, 4);
        rectangles.build();

        assertEquals(0, rectangles.prefixRectSum(0, 100));
        assertEquals(15, rectangles.prefixRectSum(1, 3));
        assertEquals(100, rectangles.prefixRectSum(4, 5));
    }

    @Test
    public void clearRestoresInitialStateAndAllowsReuse() {
        OfflineStatic_RectAdd_RectSum rectangles = new OfflineStatic_RectAdd_RectSum();
        rectangles.addAll(7);
        rectangles.rectAdd(0, 0, 10, 10, 3);
        rectangles.build();
        assertEquals(40, rectangles.prefixRectSum(2, 2));

        rectangles.clear();
        assertThrows(AssertionError.class, () -> rectangles.prefixRectSum(2, 2));
        assertArrayEquals(new long[] {0},
                rectangles.solve(List.of(new Rectangle(0, 0, 10, 10))));

        rectangles.rectAdd(-2, -2, 2, 2, 5);
        assertArrayEquals(new long[] {40},
                rectangles.solve(List.of(new Rectangle(-1, -10, 1, 10))));
    }
}
