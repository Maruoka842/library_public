package library.util.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TreapSetMethodConsistencyTest {

    @Test
    public void testLongTreapSetNewMethods() {
        LongTreapSet set = new LongTreapSet();
        assertNull(set.lower(10L));
        assertNull(set.higher(10L));

        set.add(10L);
        set.add(20L);
        set.add(30L);

        // lower & higher
        assertNull(set.lower(10L));
        assertEquals(10L, (long) set.lower(15L));
        assertEquals(10L, (long) set.lower(20L));
        assertEquals(20L, (long) set.lower(25L));

        assertEquals(20L, (long) set.higher(10L));
        assertEquals(20L, (long) set.higher(15L));
        assertEquals(30L, (long) set.higher(20L));
        assertNull(set.higher(30L));

        // addAll
        LongTreapSet other = new LongTreapSet();
        other.add(5L);
        other.add(20L);
        other.add(25L);

        set.addAll(other);
        assertEquals(5, set.size());
        assertTrue(set.contains(5L));
        assertTrue(set.contains(10L));
        assertTrue(set.contains(20L));
        assertTrue(set.contains(25L));
        assertTrue(set.contains(30L));
    }

    @Test
    public void testIntTreapSetNewMethods() {
        IntTreapSet set = new IntTreapSet();
        set.add(10);
        set.add(20);
        set.add(30);

        // countLess, countGeq, countGreater
        assertEquals(0, set.countLess(10));
        assertEquals(1, set.countLess(15));
        assertEquals(1, set.countLess(20));
        assertEquals(2, set.countLess(25));
        assertEquals(3, set.countLess(35));

        assertEquals(3, set.countGeq(10));
        assertEquals(2, set.countGeq(15));
        assertEquals(2, set.countGeq(20));
        assertEquals(1, set.countGeq(25));
        assertEquals(0, set.countGeq(35));

        assertEquals(2, set.countGreater(10));
        assertEquals(2, set.countGreater(15));
        assertEquals(1, set.countGreater(20));
        assertEquals(1, set.countGreater(25));
        assertEquals(0, set.countGreater(30));

        // firstMissingAtLeast
        assertEquals(0, (int) set.firstMissingAtLeast(0));
        assertEquals(1, (int) set.firstMissingAtLeast(1));
        assertEquals(11, (int) set.firstMissingAtLeast(10));
        assertEquals(21, (int) set.firstMissingAtLeast(20));

        IntTreapSet dense = new IntTreapSet();
        dense.add(5);
        dense.add(6);
        dense.add(7);
        assertEquals(8, (int) dense.firstMissingAtLeast(5));
    }

    @Test
    public void testIntTreapSetToArray() {
        IntTreapSet emptySet = new IntTreapSet();
        assertArrayEquals(new int[0], emptySet.toArray());

        IntTreapSet set = new IntTreapSet();
        set.add(30);
        set.add(10);
        set.add(20);
        set.add(5);
        set.add(25);

        assertArrayEquals(new int[]{5, 10, 20, 25, 30}, set.toArray());
    }

    @Test
    public void testLongTreapSetToArray() {
        LongTreapSet emptySet = new LongTreapSet();
        assertArrayEquals(new long[0], emptySet.toArray());

        LongTreapSet set = new LongTreapSet();
        set.add(300L);
        set.add(100L);
        set.add(200L);
        set.add(50L);
        set.add(250L);

        assertArrayEquals(new long[]{50L, 100L, 200L, 250L, 300L}, set.toArray());
    }
}
