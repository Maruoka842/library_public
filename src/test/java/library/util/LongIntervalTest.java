package library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LongIntervalTest {

    @Test
    public void testEmptyIntervalNormalization() {
        LongInterval empty1 = new LongInterval(5, 5);
        assertEquals(0, empty1.l());
        assertEquals(0, empty1.r());
        assertTrue(empty1.isEmpty());

        LongInterval empty2 = new LongInterval(8, 3);
        assertEquals(0, empty2.l());
        assertEquals(0, empty2.r());
        assertTrue(empty2.isEmpty());

        assertEquals(empty1, empty2); // 空区間はすべて [0, 0) に正規化されるため同値
    }

    @Test
    public void testLengthAndIsEmpty() {
        LongInterval a = new LongInterval(3, 8);
        assertEquals(5, a.length());
        assertFalse(a.isEmpty());
    }

    @Test
    public void testContainsValue() {
        LongInterval a = new LongInterval(3, 8);
        assertFalse(a.contains(2));
        assertTrue(a.contains(3));
        assertTrue(a.contains(7));
        assertFalse(a.contains(8));
        assertFalse(a.contains(9));

        LongInterval empty = new LongInterval(5, 5);
        assertFalse(empty.contains(0));
    }

    @Test
    public void testContainsInterval() {
        LongInterval a = new LongInterval(3, 8);
        LongInterval b = new LongInterval(4, 7);
        LongInterval c = new LongInterval(3, 8);
        LongInterval d = new LongInterval(2, 5);
        LongInterval empty = new LongInterval(5, 5);

        assertTrue(a.contains(b));
        assertTrue(a.contains(c));
        assertFalse(a.contains(d));
        assertTrue(a.contains(empty));
    }

    @Test
    public void testHasOverlap() {
        LongInterval a = new LongInterval(3, 8);
        LongInterval b = new LongInterval(5, 10);
        LongInterval c = new LongInterval(8, 12);
        LongInterval d = new LongInterval(10, 15);
        LongInterval empty = new LongInterval(5, 5);

        assertTrue(a.hasOverlap(b));
        assertFalse(a.hasOverlap(c)); // [3, 8) と [8, 12) はオーバーラップしない
        assertFalse(a.hasOverlap(d));
        assertFalse(a.hasOverlap(empty));
    }

    @Test
    public void testIntersection() {
        LongInterval a = new LongInterval(3, 8);
        LongInterval b = new LongInterval(5, 10);
        LongInterval inter = a.intersection(b);
        assertEquals(new LongInterval(5, 8), inter);

        LongInterval c = new LongInterval(8, 12);
        LongInterval emptyInter = a.intersection(c);
        assertEquals(new LongInterval(0, 0), emptyInter);
    }

    @Test
    public void testSpan() {
        LongInterval a = new LongInterval(3, 8);
        LongInterval b = new LongInterval(5, 12);
        LongInterval s = a.span(b);
        assertEquals(new LongInterval(3, 12), s);

        LongInterval empty = new LongInterval(10, 5);
        assertEquals(a, a.span(empty));
        assertEquals(a, empty.span(a));
    }

    @Test
    public void testShiftExpand() {
        LongInterval a = new LongInterval(3, 8);
        assertEquals(new LongInterval(5, 10), a.shift(2));
        assertEquals(new LongInterval(3, 11), a.expandRight(3));
        assertEquals(new LongInterval(1, 8), a.expandLeft(2));

        LongInterval empty = new LongInterval(5, 5);
        assertEquals(new LongInterval(0, 0), empty.shift(5));
        assertEquals(new LongInterval(0, 0), empty.expandRight(5));
        assertEquals(new LongInterval(0, 0), empty.expandLeft(5));
    }

    @Test
    public void testCompareTo() {
        LongInterval a = new LongInterval(3, 8);
        LongInterval b = new LongInterval(3, 10);
        LongInterval c = new LongInterval(4, 5);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(c) < 0);
        assertEquals(0, a.compareTo(new LongInterval(3, 8)));
    }
}
