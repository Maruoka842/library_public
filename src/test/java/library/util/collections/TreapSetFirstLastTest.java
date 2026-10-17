package library.util.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TreapSetFirstLastTest {

    @Test
    public void testLongTreapSetFirstLastAndPoll() {
        LongTreapSet set = new LongTreapSet();

        // Empty set behavior
        assertTrue(set.isEmpty());
        assertNull(set.first());
        assertNull(set.last());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());

        // Single element
        set.add(100L);
        assertFalse(set.isEmpty());
        assertEquals(100L, (long) set.first());
        assertEquals(100L, (long) set.last());

        // Multiple elements
        set.add(50L);
        set.add(200L);
        set.add(-10L);
        set.add(300L);

        // Current elements: -10, 50, 100, 200, 300
        assertEquals(5, set.size());
        assertEquals(-10L, (long) set.first());
        assertEquals(300L, (long) set.last());

        // pollFirst
        assertEquals(-10L, (long) set.pollFirst());
        assertEquals(4, set.size());
        assertFalse(set.contains(-10L));
        assertEquals(50L, (long) set.first());

        // pollLast
        assertEquals(300L, (long) set.pollLast());
        assertEquals(3, set.size());
        assertFalse(set.contains(300L));
        assertEquals(200L, (long) set.last());

        // Poll remaining
        assertEquals(50L, (long) set.pollFirst());
        assertEquals(200L, (long) set.pollLast());
        assertEquals(100L, (long) set.first());
        assertEquals(100L, (long) set.last());

        assertEquals(100L, (long) set.pollFirst());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertNull(set.first());
        assertNull(set.last());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
    }

    @Test
    public void testIntTreapSetFirstLastAndPoll() {
        IntTreapSet set = new IntTreapSet();

        // Empty set behavior
        assertTrue(set.isEmpty());
        assertNull(set.first());
        assertNull(set.last());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());

        // Single element
        set.add(100);
        assertFalse(set.isEmpty());
        assertEquals(100, (int) set.first());
        assertEquals(100, (int) set.last());

        // Multiple elements
        set.add(50);
        set.add(200);
        set.add(-10);
        set.add(300);

        // Current elements: -10, 50, 100, 200, 300
        assertEquals(5, set.size());
        assertEquals(-10, (int) set.first());
        assertEquals(300, (int) set.last());

        // pollFirst
        assertEquals(-10, (int) set.pollFirst());
        assertEquals(4, set.size());
        assertFalse(set.contains(-10));
        assertEquals(50, (int) set.first());

        // pollLast
        assertEquals(300, (int) set.pollLast());
        assertEquals(3, set.size());
        assertFalse(set.contains(300));
        assertEquals(200, (int) set.last());

        // Poll remaining
        assertEquals(50, (int) set.pollFirst());
        assertEquals(200, (int) set.pollLast());
        assertEquals(100, (int) set.first());
        assertEquals(100, (int) set.last());

        assertEquals(100, (int) set.pollFirst());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertNull(set.first());
        assertNull(set.last());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
    }
}
