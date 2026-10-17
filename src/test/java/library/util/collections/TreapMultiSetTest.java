package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;
import org.junit.jupiter.api.Test;

public class TreapMultiSetTest {

    @Test
    public void testBasicOperations() {
        TreapMultiSet<Integer> set = new TreapMultiSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        set.add(5);
        set.add(3);
        set.add(5);
        set.add(7);

        assertFalse(set.isEmpty());
        assertEquals(4, set.size());
        assertEquals(3, set.numberDistinctElements());

        assertEquals(2, set.count(5));
        assertEquals(1, set.count(3));
        assertEquals(1, set.count(7));
        assertEquals(0, set.count(10));

        assertTrue(set.contains(5));
        assertFalse(set.contains(10));

        assertTrue(set.remove(5));
        assertEquals(1, set.count(5));
        assertEquals(3, set.size());

        set.add(5, 3);
        assertEquals(4, set.count(5));
        assertEquals(6, set.size());

        set.remove(5, 2);
        assertEquals(2, set.count(5));
        assertEquals(4, set.size());

        set.remove(3, 1);
        assertFalse(set.contains(3));
        assertEquals(2, set.numberDistinctElements());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(0, set.numberDistinctElements());
    }

    @Test
    public void testNavigation() {
        TreapMultiSet<Integer> set = new TreapMultiSet<>();
        set.add(10);
        set.add(20);
        set.add(30);

        assertEquals(10, set.peekFirst());
        assertEquals(30, set.peekLast());

        assertEquals(10, set.floor(15));
        assertEquals(20, set.floor(20));
        assertEquals(20, set.ceil(15));
        assertEquals(20, set.ceil(20));

        assertEquals(10, set.lower(20));
        assertEquals(30, set.higher(20));
        assertNull(set.lower(10));
        assertNull(set.higher(30));

        TreapMultiSet.Entry<Integer> first = set.peekFirstEntry();
        assertEquals(10, first.key);
        assertEquals(1, first.value);

        TreapMultiSet.Entry<Integer> last = set.peekLastEntry();
        assertEquals(30, last.key);
        assertEquals(1, last.value);
    }

    @Test
    public void testRankAndKth() {
        TreapMultiSet<Integer> set = new TreapMultiSet<>();
        // {0, 1, 1, 3, 3, 3}
        set.add(0);
        set.add(1, 2);
        set.add(3, 3);

        assertEquals(6, set.size());
        assertEquals(0, set.kthKey(0));
        assertEquals(1, set.kthKey(1));
        assertEquals(1, set.kthKey(2));
        assertEquals(3, set.kthKey(3));
        assertEquals(3, set.kthKey(4));
        assertEquals(3, set.kthKey(5));
        assertNull(set.kthKey(6));

        assertEquals(1, set.countLeq(0));
        assertEquals(3, set.countLeq(1));
        assertEquals(3, set.countLeq(2));
        assertEquals(6, set.countLeq(3));

        assertEquals(6, set.countGeq(0));
        assertEquals(5, set.countGeq(1));
        assertEquals(3, set.countGeq(3));
    }

    @Test
    public void testPoll() {
        TreapMultiSet<Integer> set = new TreapMultiSet<>();
        set.add(10);
        set.add(5);
        set.add(15);

        assertEquals(5, set.pollFirst());
        assertEquals(15, set.pollLast());
        assertEquals(10, set.peekFirst());
        assertEquals(1, set.size());
    }

    @Test
    public void testStringKeys() {
        TreapMultiSet<String> set = new TreapMultiSet<>();
        set.add("apple");
        set.add("banana", 2);
        set.add("cherry");

        assertEquals(4, set.size());
        assertEquals("apple", set.peekFirst());
        assertEquals("cherry", set.peekLast());
        assertEquals("banana", set.lower("cherry"));
        assertEquals("cherry", set.higher("banana"));
        assertEquals(2, set.count("banana"));
    }

    @Test
    public void testCopyAndEquals() {
        TreapMultiSet<Integer> emptySet = new TreapMultiSet<>();
        TreapMultiSet<Integer> emptyCopy = emptySet.copy();
        assertEquals(emptySet, emptyCopy);
        assertEquals(emptySet.hashCode(), emptyCopy.hashCode());

        TreapMultiSet<Integer> original = new TreapMultiSet<>();
        original.add(10, 2);
        original.add(5, 1);
        original.add(15, 3);

        TreapMultiSet<Integer> copy = original.copy();
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());

        copy.add(10, 1);
        assertNotEquals(original, copy);
        assertEquals(3, copy.count(10));
        assertEquals(2, original.count(10));
    }

    @Test
    public void testSplitAndJoin() {
        TreapMultiSet<Integer> a = new TreapMultiSet<>();
        a.add(1, 2);
        a.add(3, 4);
        a.add(5, 1);
        a.add(-10, 3);

        TreapMultiSet<Integer> b = new TreapMultiSet<>();
        b.add(2, 5);
        b.add(4, 2);
        b.add(6, 1);
        b.add(-5, 2);

        int value = 3;
        TreapMultiSet.Split<Integer> splitA = a.splitByValue(value);
        TreapMultiSet.Split<Integer> splitB = b.splitByValue(value);

        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());

        TreapMultiSet<Integer> c = TreapMultiSet.join(splitA.less(), splitB.greaterOrEqual());
        TreapMultiSet<Integer> d = TreapMultiSet.join(splitB.less(), splitA.greaterOrEqual());

        assertTrue(splitA.less().isEmpty());
        assertTrue(splitB.greaterOrEqual().isEmpty());

        assertEquals(8, c.size());
        assertEquals(3, c.count(-10));
        assertEquals(2, c.count(1));
        assertEquals(2, c.count(4));
        assertEquals(1, c.count(6));

        assertEquals(12, d.size());
        assertEquals(2, d.count(-5));
        assertEquals(5, d.count(2));
        assertEquals(4, d.count(3));
        assertEquals(1, d.count(5));
    }

    @Test
    public void testCap() {
        TreapMultiSet<Integer> a = new TreapMultiSet<>();
        a.add(1, 3);
        a.add(2, 1);
        a.add(3, 4);

        TreapMultiSet<Integer> b = new TreapMultiSet<>();
        b.add(1, 2);
        b.add(3, 5);
        b.add(4, 2);

        TreapMultiSet<Integer> intersection = a.cap(b);
        assertEquals(2, intersection.numberDistinctElements());
        assertEquals(2, intersection.count(1));
        assertEquals(4, intersection.count(3));
        assertEquals(0, intersection.count(2));
        assertEquals(0, intersection.count(4));
        assertEquals(6, intersection.size());
    }

    @Test
    public void testCustomComparator() {
        TreapMultiSet<Integer> set = new TreapMultiSet<>(Comparator.reverseOrder());
        set.add(10);
        set.add(20);
        set.add(5);

        assertEquals(20, set.peekFirst());
        assertEquals(5, set.peekLast());
        assertEquals(10, set.kthKey(1));
        assertEquals(20, set.floor(15));
        assertEquals(5, set.ceil(8));
    }
}
