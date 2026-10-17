package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;
import library.util.SlopeTrick;
import library.util.segtree.LiChaoTree;
import library.util.segtree.SparseSegtree;

public class ClearMethodTest {

    @Test
    public void testTreapSetClear() {
        TreapSet<Integer> set = new TreapSet<>();
        set.add(10);
        set.add(20);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testIntTreapSetClear() {
        IntTreapSet set = new IntTreapSet();
        set.add(10);
        set.add(20);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testLongTreapSetClear() {
        LongTreapSet set = new LongTreapSet();
        set.add(10L);
        set.add(20L);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30L);
        assertEquals(1, set.size());
    }

    @Test
    public void testIntTreapMultiSetClear() {
        IntTreapMultiSet set = new IntTreapMultiSet();
        set.add(10);
        set.add(10);
        set.add(20);
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testLongTreapMultiSetClear() {
        LongTreapMultiSet set = new LongTreapMultiSet();
        set.add(10L);
        set.add(10L);
        set.add(20L);
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30L);
        assertEquals(1, set.size());
    }

    @Test
    public void testShiftableTreapSetClear() {
        ShiftableTreapSet.Strategy<Long, Long> strategy = new ShiftableTreapSet.Strategy<>() {
            @Override
            public Long mergeA(Long newer, Long older) { return newer + older; }
            @Override
            public Long mergeAX(Long a, Long b) { return a + b; }
        };
        ShiftableTreapSet<Long, Long> set = new ShiftableTreapSet<>(strategy);
        set.add(10L);
        assertEquals(1, set.size());
        set.clear();
        assertEquals(0, set.size());
    }

    @Test
    public void testOpenHashSetClear() {
        OpenHashSet<Integer> set = new OpenHashSet<>();
        set.add(10);
        set.add(20);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testLongOpenHashSetClear() {
        LongOpenHashSet set = new LongOpenHashSet();
        set.add(10L);
        set.add(20L);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30L);
        assertEquals(1, set.size());
    }

    @Test
    public void testTreeMultiSetClear() {
        TreeMultiSet<Integer> set = new TreeMultiSet<>();
        set.add(10);
        set.add(10);
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testHashMultiSetClear() {
        HashMultiSet<Integer> set = new HashMultiSet<>();
        set.add(10);
        set.add(10);
        assertEquals(1, set.size()); // tracks distinct count in size()
        set.clear();
        assertEquals(0, set.size());
        set.add(30);
        assertEquals(1, set.size());
    }

    @Test
    public void testLastKMultiSetClear() {
        LastKMultiSet<Integer> set = new LastKMultiSet<>(2, Integer::sum, x -> -x, 0);
        set.add(10);
        set.add(20);
        set.add(30); // top 2 are 20 and 30, sum = 50
        assertEquals(50, set.get());
        set.clear();
        assertEquals(0, set.get());
        set.add(15);
        assertEquals(15, set.get());
    }

    @Test
    public void testDynamicParkingMultisetClear() {
        DynamicParkingMultiset.Strategy<Long> strategy = new DynamicParkingMultiset.Strategy<>() {
            @Override
            public Long op(Long a, Long b) { return a + b; }
            @Override
            public Long identity() { return 0L; }
            @Override
            public Long lift(long d) { return d; }
            @Override
            public Long shift(long delta, Long aggregate, int size) { return aggregate + delta * size; }
        };
        DynamicParkingMultiset<Long> set = new DynamicParkingMultiset<>(strategy);
        set.add(3);
        set.add(3);
        // displacementProd should not throw exception
        assertNotNull(set.displacementFold());
        set.clear();
        assertEquals(0L, set.displacementFold());
    }

    @Test
    public void testPrimitiveQueuesClear() {
        IntQueue iq = new IntQueue();
        iq.add(10);
        assertEquals(1, iq.size());
        iq.clear();
        assertEquals(0, iq.size());

        LongQueue lq = new LongQueue();
        lq.add(10L);
        assertEquals(1, lq.size());
        lq.clear();
        assertEquals(0, lq.size());

        CharQueue cq = new CharQueue();
        cq.add('a');
        assertEquals(1, cq.size());
        cq.clear();
        assertEquals(0, cq.size());

        DoubleQueue dq = new DoubleQueue();
        dq.add(1.5);
        assertEquals(1, dq.size());
        dq.clear();
        assertEquals(0, dq.size());
    }

    @Test
    public void testPartiallyRetroactivePriorityQueueClear() {
        PartiallyRetroactivePriorityQueue pq = new PartiallyRetroactivePriorityQueue(10);
        pq.add(1, 100);
        pq.add(2, 50);
        assertEquals(50, pq.peek());
        pq.clear();
        // Since cleared, it goes back to initial state (filled with INF values)
        assertTrue(pq.peek() > 1000000L);
    }

    @Test
    public void testTrieMapClear() {
        TrieMap map = new TrieMap();
        map.add("hello".toCharArray(), 100);
        assertTrue(map.contains("hello".toCharArray()));
        assertEquals(1, map.size());
        map.clear();
        assertFalse(map.contains("hello".toCharArray()));
        assertEquals(0, map.size());
    }

    @Test
    public void testImplicitTreapClear() {
        ImplicitTreap<Integer> treap = new ImplicitTreap<>();
        treap.insert(0, 10);
        assertEquals(1, treap.size());
        treap.clear();
        assertEquals(0, treap.size());
    }

    @Test
    public void testLeafyBinaryTrieFromTopBitsClear() {
        LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(10);
        trie.clear();
        assertEquals(0, trie.root.subtreeHit);
    }

    @Test
    public void testSlopeTrickClear() {
        SlopeTrick st = new SlopeTrick();
        st.addRelu(10);
        assertEquals(1, st.sizeR());
        st.clear();
        assertEquals(0, st.sizeR());
        assertEquals(0, st.sizeL());
    }

    @Test
    public void testLiChaoTreeClear() {
        LiChaoTree tree = new LiChaoTree(new long[]{1, 2, 3});
        tree.addLine(1, 1);
        tree.clear();
        // after clear, no valid lines should exist
        assertFalse(tree.get(1).isValid());
    }

    @Test
    public void testSparseSegtreeClear() {
        SparseSegtree<Long> tree = new SparseSegtree<>(10, Long::sum, 0L);
        tree.clear();
        // should be cleared back to empty root state
    }
}
