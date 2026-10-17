package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import library.util.segtree.OfflineStatic_RectAdd_RectModSum.Rectangle;

public class OfflineStatic_RectAdd_RectModSumTest {
    @Test
    public void solvesRectangleSumsModuloMod() {
        OfflineStatic_RectAdd_RectModSum rectangles = new OfflineStatic_RectAdd_RectModSum(2, 7);
        rectangles.addAll(10);
        rectangles.rectAdd(-2, -1, 4, 5, 9);
        rectangles.rectAdd(1, 2, 6, 7, -3);

        List<Rectangle> queries = List.of(
                new Rectangle(-3, -3, 8, 8),
                new Rectangle(0, 0, 3, 4),
                new Rectangle(2, 2, 2, 9));
        assertArrayEquals(new long[] {3, 6, 0}, rectangles.solve(queries));
        assertArrayEquals(new long[] {3, 6, 0}, rectangles.solve(queries));
    }

    @Test
    public void answersPrefixPotentialModuloMod() {
        OfflineStatic_RectAdd_RectModSum rectangles = new OfflineStatic_RectAdd_RectModSum(11);
        rectangles.addAll(3);
        rectangles.rectAdd(0, 0, 4, 5, 8);
        rectangles.build();

        assertEquals(0, rectangles.prefixRectSum(0, 100));
        assertEquals(0, rectangles.prefixRectSum(1, 3));
        assertEquals(0, rectangles.prefixRectSum(4, 5));
        assertThrows(AssertionError.class,
                () -> rectangles.solve(List.of(new Rectangle(0, 0, 1, 1))));
    }

    @Test
    public void clearAllowsReuse() {
        OfflineStatic_RectAdd_RectModSum rectangles = new OfflineStatic_RectAdd_RectModSum(5);
        rectangles.rectAdd(0, 0, 2, 2, 4);
        rectangles.build();
        rectangles.prefixRectSum(1, 1);
        rectangles.clear();

        assertThrows(AssertionError.class, () -> rectangles.prefixRectSum(1, 1));
        assertArrayEquals(new long[] {0},
                rectangles.solve(List.of(new Rectangle(0, 0, 2, 2))));
    }
}
