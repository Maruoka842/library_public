package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.List;

public class IntTreapMultiSetTest {

    @Test
    public void testBasicOperations() {
        IntTreapMultiSet set = new IntTreapMultiSet();
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
    }

    @Test
    public void testNavigation() {
        IntTreapMultiSet set = new IntTreapMultiSet();
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

        IntTreapMultiSet.Entry first = set.peekFirstEntry();
        assertEquals(10, first.key);
        assertEquals(1, first.value);

        IntTreapMultiSet.Entry last = set.peekLastEntry();
        assertEquals(30, last.key);
        assertEquals(1, last.value);
    }

    @Test
    public void testRankAndMex() {
        IntTreapMultiSet set = new IntTreapMultiSet();
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

        // mex({0, 1, 1, 3, 3, 3}) = 2
        assertEquals(2, set.mex());

        set.add(2);
        // mex({0, 1, 1, 2, 3, 3, 3}) = 4
        assertEquals(4, set.mex());

        set.remove(0);
        // mex({1, 1, 2, 3, 3, 3}) = 0
        assertEquals(0, set.mex());
    }

    @Test
    public void testPoll() {
        IntTreapMultiSet set = new IntTreapMultiSet();
        set.add(10);
        set.add(5);
        set.add(15);

        assertEquals(5, set.pollFirst());
        assertEquals(15, set.pollLast());
        assertEquals(10, set.peekFirst());
        assertEquals(1, set.size());
    }

    @Test
    public void testNegativeAndLarge() {
        IntTreapMultiSet set = new IntTreapMultiSet();
        set.add(-10);
        set.add(-5);
        set.add(0);

        assertEquals(-10, set.peekFirst());
        assertEquals(0, set.peekLast());
        assertEquals(1, set.mex()); // mex of {-10, -5, 0} is 1

        set.add(Integer.MAX_VALUE);
        assertTrue(set.contains(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, set.peekLast());

        set.add(Integer.MIN_VALUE);
        assertTrue(set.contains(Integer.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE, set.peekFirst());
    }

    @Test
    public void testCopy() {
        IntTreapMultiSet emptySet = new IntTreapMultiSet();
        IntTreapMultiSet emptyCopy = emptySet.copy();
        assertEquals(emptySet, emptyCopy);
        assertEquals(0, emptyCopy.size());

        IntTreapMultiSet original = new IntTreapMultiSet();
        original.add(10, 2);
        original.add(5, 1);
        original.add(15, 3);

        IntTreapMultiSet copy = original.copy();
        assertEquals(original, copy);
        assertEquals(original.size(), copy.size());
        assertEquals(original.numberDistinctElements(), copy.numberDistinctElements());

        // Modify copy and assert independence
        copy.add(10, 1);
        assertEquals(3, copy.count(10));
        assertEquals(2, original.count(10));

        copy.add(20, 5);
        assertTrue(copy.contains(20));
        assertFalse(original.contains(20));

        copy.remove(5);
        assertFalse(copy.contains(5));
        assertTrue(original.contains(5));
    }

    @Test
    public void testSplitAndJoin() {
        IntTreapMultiSet a = new IntTreapMultiSet();
        a.add(1, 2);
        a.add(3, 4);
        a.add(5, 1);
        a.add(-10, 3);

        IntTreapMultiSet b = new IntTreapMultiSet();
        b.add(2, 5);
        b.add(4, 2);
        b.add(6, 1);
        b.add(-5, 2);

        int value = 3;
        IntTreapMultiSet.Split splitA = a.splitByValue(value);
        IntTreapMultiSet.Split splitB = b.splitByValue(value);

        // a should be empty after split
        assertTrue(a.isEmpty());
        assertEquals(0, a.size());
        // b should be empty after split
        assertTrue(b.isEmpty());
        assertEquals(0, b.size());

        IntTreapMultiSet c = IntTreapMultiSet.join(splitA.less(), splitB.greaterOrEqual());
        IntTreapMultiSet d = IntTreapMultiSet.join(splitB.less(), splitA.greaterOrEqual());

        // splitA.less() and splitB.greaterOrEqual() should be empty after join
        assertTrue(splitA.less().isEmpty());
        assertTrue(splitB.greaterOrEqual().isEmpty());

        // check content of C
        // a.less(): -10 (3個), 1 (2個)
        // b.greaterOrEqual(): 4 (2個), 6 (1個)
        assertEquals(8, c.size());
        assertEquals(3, c.count(-10));
        assertEquals(2, c.count(1));
        assertEquals(2, c.count(4));
        assertEquals(1, c.count(6));
        assertEquals(0, c.count(3));
        assertEquals(0, c.count(5));

        // check content of D
        // b.less(): -5 (2個), 2 (5個)
        // a.greaterOrEqual(): 3 (4個), 5 (1個)
        assertEquals(12, d.size());
        assertEquals(2, d.count(-5));
        assertEquals(5, d.count(2));
        assertEquals(4, d.count(3));
        assertEquals(1, d.count(5));
        assertEquals(0, d.count(1));
    }

    @Test
    public void testSplitAndJoinEdgeCases() {
        // Empty sets
        IntTreapMultiSet empty1 = new IntTreapMultiSet();
        IntTreapMultiSet empty2 = new IntTreapMultiSet();
        IntTreapMultiSet.Split splitEmpty1 = empty1.splitByValue(5);
        IntTreapMultiSet.Split splitEmpty2 = empty2.splitByValue(5);

        IntTreapMultiSet joinedEmpty = IntTreapMultiSet.join(splitEmpty1.less(), splitEmpty2.greaterOrEqual());
        assertTrue(joinedEmpty.isEmpty());

        // Single elements
        IntTreapMultiSet single1 = new IntTreapMultiSet();
        single1.add(10, 5);
        IntTreapMultiSet.Split splitSingle1 = single1.splitByValue(10);
        assertEquals(0, splitSingle1.less().size());
        assertEquals(5, splitSingle1.greaterOrEqual().size());
        assertEquals(5, splitSingle1.greaterOrEqual().count(10));

        IntTreapMultiSet single2 = new IntTreapMultiSet();
        single2.add(5, 3);
        IntTreapMultiSet.Split splitSingle2 = single2.splitByValue(10);
        assertEquals(3, splitSingle2.less().size());
        assertEquals(3, splitSingle2.less().count(5));
        assertEquals(0, splitSingle2.greaterOrEqual().size());
    }
}
