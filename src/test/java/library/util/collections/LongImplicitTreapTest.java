package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LongImplicitTreapTest {

    @Test
    public void testIsEmptyAndSize() {
        LongImplicitTreap treap = new LongImplicitTreap();
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());

        treap.insert(0, 10L);
        assertFalse(treap.isEmpty());
        assertEquals(1, treap.size());

        treap.erase(0);
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());
    }

    @Test
    public void testInsertAndKth() {
        LongImplicitTreap treap = new LongImplicitTreap();
        treap.insert(0, 100000000000L); // [100000000000L]
        treap.insert(1, 300000000000L); // [100000000000L, 300000000000L]
        treap.insert(1, 200000000000L); // [100000000000L, 200000000000L, 300000000000L]
        treap.insert(0, 50000000000L);  // [50000000000L, 100000000000L, 200000000000L, 300000000000L]

        assertEquals(4, treap.size());
        assertEquals(50000000000L, treap.kth(0));
        assertEquals(100000000000L, treap.kth(1));
        assertEquals(200000000000L, treap.kth(2));
        assertEquals(300000000000L, treap.kth(3));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(4));
    }

    @Test
    public void testErase() {
        LongImplicitTreap treap = new LongImplicitTreap();
        treap.insert(0, 10L);
        treap.insert(1, 20L);
        treap.insert(2, 30L);
        treap.insert(3, 40L); // [10, 20, 30, 40]

        treap.erase(1); // removes 20 -> [10, 30, 40]
        assertEquals(3, treap.size());
        assertEquals(10L, treap.kth(0));
        assertEquals(30L, treap.kth(1));
        assertEquals(40L, treap.kth(2));

        treap.erase(0); // removes 10 -> [30, 40]
        assertEquals(2, treap.size());
        assertEquals(30L, treap.kth(0));
        assertEquals(40L, treap.kth(1));

        treap.erase(1); // removes 40 -> [30]
        assertEquals(1, treap.size());
        assertEquals(30L, treap.kth(0));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.erase(1));
    }

    @Test
    public void testClear() {
        LongImplicitTreap treap = new LongImplicitTreap();
        treap.insert(0, 1L);
        treap.insert(1, 2L);
        treap.insert(2, 3L);
        assertFalse(treap.isEmpty());

        treap.clear();
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());
        assertNull(treap.root());
    }

    @Test
    public void testToArrayAndToString() {
        LongImplicitTreap treap = new LongImplicitTreap();
        treap.insert(0, 10L);
        treap.insert(1, 20L);
        treap.insert(2, 30L);

        long[] arr = treap.toArray();
        assertArrayEquals(new long[]{10L, 20L, 30L}, arr);

        String str = treap.toString().trim();
        assertTrue(str.contains("10") && str.contains("20") && str.contains("30"));
    }

    @Test
    public void testDump() {
        LongImplicitTreap treap = new LongImplicitTreap();
        treap.dump(); // Empty dump
        treap.insert(0, 100L);
        treap.insert(1, 200L);
        treap.dump(); // Non-empty dump
        assertNotNull(treap.root());
        assertEquals(2, treap.size());
        assertEquals(100L, treap.kth(0));
        assertEquals(200L, treap.kth(1));
    }
}
