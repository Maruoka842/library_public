package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class SegTreeMinPrefixSumTest {

    @Test
    public void testStateMergeAndToString() {
        assertEquals(0, SegTreeFactory.State.IDENTITY.sum);
        assertEquals(Integer.MAX_VALUE, SegTreeFactory.State.IDENTITY.minOfPrefixSum);

        SegTreeFactory.State s1 = new SegTreeFactory.State(2, 2);
        SegTreeFactory.State s2 = new SegTreeFactory.State(-5, -5);
        SegTreeFactory.State merged = s1.merge(s2);

        assertEquals(-3, merged.sum);
        assertEquals(-3, merged.minOfPrefixSum);
        assertEquals("(-3 -3)", merged.toString());

        SegTreeFactory.State s3 = new SegTreeFactory.State(10, 3);
        SegTreeFactory.State merged2 = merged.merge(s3);
        assertEquals(7, merged2.sum);
        assertEquals(-3, merged2.minOfPrefixSum);

        // Test IDENTITY merge
        assertSame(s1, s1.merge(SegTreeFactory.State.IDENTITY));
        assertSame(s1, SegTreeFactory.State.IDENTITY.merge(s1));

        // Test MonoidElement interface methods
        assertSame(SegTreeFactory.State.STRATEGY, s1.parent());
        assertSame(s1, s1.self());
        assertSame(SegTreeFactory.State.IDENTITY, s1.one());

        SegTreeFactory.State mulRes = s1.mul(s2);
        assertEquals(-3, mulRes.sum);
        assertEquals(-3, mulRes.minOfPrefixSum);
    }

    @Test
    public void testSegTreeFactoryMinPrefixSum() {
        int n = 5;
        var seg = SegTreeFactory.minPrefixSum(n);

        assertNotNull(seg);

        int[] vals = {2, -5, 3, -1, 4};
        for (int i = 0; i < n; i++) {
            seg.set(i, new SegTreeFactory.State(vals[i], vals[i]));
        }

        SegTreeFactory.State resAll = seg.fold(0, 5);
        assertEquals(3, resAll.sum);
        assertEquals(-3, resAll.minOfPrefixSum);

        SegTreeFactory.State resSub = seg.fold(2, 5); // [3, -1, 4]
        assertEquals(6, resSub.sum);
        assertEquals(2, resSub.minOfPrefixSum);

        // Point update at index 1: change -5 to 1
        seg.set(1, new SegTreeFactory.State(1, 1)); // [2, 1, 3, -1, 4]
        SegTreeFactory.State resUpdated = seg.fold(0, 5);
        assertEquals(9, resUpdated.sum);
        assertEquals(2, resUpdated.minOfPrefixSum);

        // Fold empty range should return identity
        assertSame(SegTreeFactory.State.IDENTITY, seg.fold(2, 2));
    }
}
