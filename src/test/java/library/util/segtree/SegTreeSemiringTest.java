package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import library.util.algebra.strategy.SemiRingStrategy;
import library.util.algebra.strategy.SubarraySumSemiringStrategy;
import library.util.algebra.strategy.KSubarraySumSemiringStrategy;
import library.util.algebra.strategy.monoid.MonoidStrategy;
import library.util.algebra.strategy.longs.LongSemiRingStrategy;
import library.util.algebra.strategy.longs.LongMonoidStrategy;

public class SegTreeSemiringTest {

    private static final SemiRingStrategy<Long> LONG_SEMIRING = new SemiRingStrategy<>() {
        @Override
        public Long zero() { return 0L; }
        @Override
        public Long one() { return 1L; }
        @Override
        public Long add(Long a, Long b) { return a + b; }
        @Override
        public Long mul(Long a, Long b) { return a * b; }
        @Override
        public boolean equals(Long a, Long b) { return a.equals(b); }
    };

    private static final LongSemiRingStrategy PRIMITIVE_LONG_SEMIRING = new LongSemiRingStrategy() {
        @Override
        public long zero() { return 0L; }
        @Override
        public long one() { return 1L; }
        @Override
        public long add(long a, long b) { return a + b; }
        @Override
        public long mul(long a, long b) { return a * b; }
        @Override
        public boolean equals(long a, long b) { return a == b; }
    };

    @Test
    public void testSegTreeWithMonoidStrategy() {
        MonoidStrategy<Long> mulMonoid = LONG_SEMIRING.multiplicativeMonoid();
        SegTree<Long> seg = new SegTree<>(4, mulMonoid);
        assertEquals(1L, seg.fold(0, 4));

        seg.set(0, 2L);
        seg.set(1, 3L);
        seg.set(2, 4L);
        assertEquals(24L, seg.fold(0, 3));
        assertEquals(12L, seg.fold(1, 3));
    }

    @Test
    public void testSegTreeWithArrayAndMonoidStrategy() {
        Long[] arr = {2L, 3L, 4L, 5L};
        MonoidStrategy<Long> mulMonoid = LONG_SEMIRING.multiplicativeMonoid();
        SegTree<Long> seg = new SegTree<>(arr, mulMonoid);
        assertEquals(120L, seg.fold(0, 4));
        assertEquals(6L, seg.fold(0, 2));
        assertEquals(12L, seg.fold(1, 3));
    }

    @Test
    public void testSegTreelongWithLongMonoidStrategy() {
        LongMonoidStrategy mulMonoid = PRIMITIVE_LONG_SEMIRING.multiplicativeMonoid();
        SegTreelong seg = new SegTreelong(4, mulMonoid);
        assertEquals(1L, seg.fold(0, 4));

        seg.set(0, 2);
        seg.set(1, 3);
        seg.set(2, 5);
        assertEquals(30L, seg.fold(0, 3));
    }

    @Test
    public void testSegTreeWithSubarraySumSemiringStrategy() {
        SubarraySumSemiringStrategy<Long> strat = new SubarraySumSemiringStrategy<>(LONG_SEMIRING);
        SegTree<SubarraySumSemiringStrategy.Node<Long>> seg = new SegTree<>(4, strat);

        seg.set(0, strat.singleton(2L));
        seg.set(1, strat.singleton(3L));
        seg.set(2, strat.singleton(4L));

        SubarraySumSemiringStrategy.Node<Long> res = seg.fold(0, 3);
        // best subarray sum of products in [2, 3, 4]
        // subarrays: [2], [3], [4], [2,3]->6, [3,4]->12, [2,3,4]->24
        // best = sum of all subarray products = 2 + 3 + 4 + 6 + 12 + 24 = 51
        assertEquals(51L, res.best);
    }

    @Test
    public void testSegTreeWithKSubarraySumSemiringStrategy() {
        KSubarraySumSemiringStrategy<Long> strat = new KSubarraySumSemiringStrategy<>(LONG_SEMIRING, 2);
        SegTree<KSubarraySumSemiringStrategy.Node<Long>> seg = new SegTree<>(3, strat);

        seg.set(0, strat.singleton(2L));
        seg.set(1, strat.singleton(3L));
        seg.set(2, strat.singleton(4L));

        KSubarraySumSemiringStrategy.Node<Long> res = seg.fold(0, 3);
        assertNotNull(res);
        assertFalse(res.isIdentity);
    }

    @Test
    public void testCompressedSparseSegTreeWithStrategies() {

        MonoidStrategy<Long> mulMonoid = LONG_SEMIRING.multiplicativeMonoid();
        CompressedSparseSegTree<Long> seg2 = new CompressedSparseSegTree<>(mulMonoid);
        seg2.registerKey(10);
        seg2.registerKey(20);
        seg2.build();
        seg2.set(10, 3L);
        seg2.set(20, 7L);
        assertEquals(21L, seg2.fold(0, 30));
    }
}
