package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntImplicitTreapTest {

    @Test
    public void testIsEmptyAndSize() {
        IntImplicitTreap treap = new IntImplicitTreap();
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());

        treap.insert(0, 10);
        assertFalse(treap.isEmpty());
        assertEquals(1, treap.size());

        treap.erase(0);
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());
    }

    @Test
    public void testInsertAndKth() {
        IntImplicitTreap treap = new IntImplicitTreap();
        treap.insert(0, 10); // [10]
        treap.insert(1, 30); // [10, 30]
        treap.insert(1, 20); // [10, 20, 30]
        treap.insert(0, 5);  // [5, 10, 20, 30]

        assertEquals(4, treap.size());
        assertEquals(5, treap.kth(0));
        assertEquals(10, treap.kth(1));
        assertEquals(20, treap.kth(2));
        assertEquals(30, treap.kth(3));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(4));
    }

    @Test
    public void testErase() {
        IntImplicitTreap treap = new IntImplicitTreap();
        treap.insert(0, 10);
        treap.insert(1, 20);
        treap.insert(2, 30);
        treap.insert(3, 40); // [10, 20, 30, 40]

        treap.erase(1); // removes 20 -> [10, 30, 40]
        assertEquals(3, treap.size());
        assertEquals(10, treap.kth(0));
        assertEquals(30, treap.kth(1));
        assertEquals(40, treap.kth(2));

        treap.erase(0); // removes 10 -> [30, 40]
        assertEquals(2, treap.size());
        assertEquals(30, treap.kth(0));
        assertEquals(40, treap.kth(1));

        treap.erase(1); // removes 40 -> [30]
        assertEquals(1, treap.size());
        assertEquals(30, treap.kth(0));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.erase(1));
    }

    @Test
    public void testClear() {
        IntImplicitTreap treap = new IntImplicitTreap();
        treap.insert(0, 1);
        treap.insert(1, 2);
        treap.insert(2, 3);
        assertFalse(treap.isEmpty());

        treap.clear();
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());
        assertNull(treap.root());
    }

    @Test
    public void testToArrayAndToString() {
        IntImplicitTreap treap = new IntImplicitTreap();
        treap.insert(0, 10);
        treap.insert(1, 20);
        treap.insert(2, 30);

        int[] arr = treap.toArray();
        assertArrayEquals(new int[]{10, 20, 30}, arr);

        String str = treap.toString().trim();
        assertTrue(str.contains("10") && str.contains("20") && str.contains("30"));
    }

    @Test
    public void testDump() {
        IntImplicitTreap treap = new IntImplicitTreap();
        treap.dump(); // Empty dump
        treap.insert(0, 100);
        treap.insert(1, 200);
        treap.dump(); // Non-empty dump
        assertNotNull(treap.root());
        assertEquals(2, treap.size());
        assertEquals(100, treap.kth(0));
        assertEquals(200, treap.kth(1));
    }
}
