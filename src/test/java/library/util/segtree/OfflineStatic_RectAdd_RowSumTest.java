package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class OfflineStatic_RectAdd_RowSumTest {
    @Test
    public void sumsSingleRectangleAtArbitraryBoundaries() {
        OfflineStatic_RectAdd_RowSum rows = new OfflineStatic_RectAdd_RowSum();
        rows.rectAdd(10, 10, 20, 20, 3);
        rows.build();

        assertEquals(0, rows.sum(9, 0, 30));
        assertEquals(30, rows.sum(10, 0, 30));
        assertEquals(18, rows.sum(15, 12, 18));
        assertEquals(6, rows.sum(15, 8, 12));
        assertEquals(0, rows.sum(20, 0, 30));
    }

    @Test
    public void sumsOverlappingRectanglesAndBase() {
        OfflineStatic_RectAdd_RowSum rows = new OfflineStatic_RectAdd_RowSum(2);
        rows.addAll(5);
        rows.rectAdd(0, 0, 10, 10, 1);
        rows.rectAdd(5, 5, 15, 15, 2);
        rows.build();

        assertEquals(70, rows.sum(2, -1, 11));
        assertEquals(70, rows.sum(7, 0, 10));
        assertEquals(105, rows.sum(7, 0, 15));
        assertEquals(95, rows.sum(12, 0, 15));
    }

    @Test
    public void handlesEmptyUpdatesEmptyIntervalsAndNegativeCoordinates() {
        OfflineStatic_RectAdd_RowSum empty = new OfflineStatic_RectAdd_RowSum();
        empty.build();
        assertEquals(0, empty.sum(Long.MIN_VALUE, -100, 100));

        OfflineStatic_RectAdd_RowSum rows = new OfflineStatic_RectAdd_RowSum();
        rows.rectAdd(-10, -7, 0, -2, -4);
        rows.rectAdd(0, 0, 0, 10, 100);
        rows.build();
        assertEquals(-20, rows.sum(-5, -10, 0));
        assertEquals(0, rows.sum(-5, -4, -4));
        assertEquals(0, rows.sum(0, -10, 0));
    }

    @Test
    public void rejectsInvalidCallOrderAndRanges() {
        OfflineStatic_RectAdd_RowSum rows = new OfflineStatic_RectAdd_RowSum();
        assertThrows(AssertionError.class, () -> rows.sum(0, 0, 1));
        rows.build();
        assertThrows(AssertionError.class, () -> rows.sum(0, 2, 1));
        rows.sum(1, 0, 1);
        assertThrows(AssertionError.class, () -> rows.sum(0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new OfflineStatic_RectAdd_RowSum(-1));
    }
}
