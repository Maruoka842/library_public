package library.util.collections;

import library.util.segtree.LazySegTreeStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class LazyImplicitTreapTest {

    @Test
    public void testBasicInsertEraseAndKth() {
        ReversibleLazyImplicitTreap<Integer, Integer> treap = new ReversibleLazyImplicitTreap<>(
                Integer::sum,
                0,
                Integer::sum,
                0,
                (action, aggregate, size) -> aggregate + action * size
        );

        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());

        treap.insert(0, 10);
        treap.insert(1, 20);
        treap.insert(1, 15); // [10, 15, 20]

        assertEquals(3, treap.size());
        assertFalse(treap.isEmpty());
        assertEquals(10, treap.kth(0));
        assertEquals(15, treap.kth(1));
        assertEquals(20, treap.kth(2));

        assertEquals(45, treap.prod(0, 3));
        assertEquals(25, treap.prod(0, 2));
        assertEquals(35, treap.prod(1, 3));
        assertEquals(0, treap.prod(1, 1));

        treap.erase(1); // removes 15 -> [10, 20]
        assertEquals(2, treap.size());
        assertEquals(10, treap.kth(0));
        assertEquals(20, treap.kth(1));
        assertEquals(30, treap.prod(0, 2));

        treap.clear();
        assertTrue(treap.isEmpty());
        assertEquals(0, treap.size());
    }

    @Test
    public void testRangeReverseSimple() {
        ReversibleLazyImplicitTreap<String, String> treap = new ReversibleLazyImplicitTreap<>(
                (a, b) -> a + b,
                "",
                (f, g) -> f + g,
                "",
                (f, x, size) -> x // no action needed for simple reverse
        );

        String[] initial = {"A", "B", "C", "D", "E"};
        for (int i = 0; i < initial.length; i++) {
            treap.insert(i, initial[i]);
        }

        assertEquals("ABCDE", treap.prod(0, 5));
        assertArrayEquals(new String[]{"A", "B", "C", "D", "E"}, treap.toArray(String[]::new));

        // reverse [1, 4): "B", "C", "D" -> "D", "C", "B"
        treap.reverse(1, 4);
        assertArrayEquals(new String[]{"A", "D", "C", "B", "E"}, treap.toArray(String[]::new));
        assertEquals("ADCBE", treap.prod(0, 5));
        assertEquals("DCB", treap.prod(1, 4));
        assertEquals("CB", treap.prod(2, 4));
        assertEquals("AD", treap.prod(0, 2));

        // reverse whole array [0, 5)
        treap.reverse(0, 5);
        assertArrayEquals(new String[]{"E", "B", "C", "D", "A"}, treap.toArray(String[]::new));
        assertEquals("EBCDA", treap.prod(0, 5));
        assertEquals("BC", treap.prod(1, 3));
        assertEquals("CDA", treap.prod(2, 5));
    }

    @Test
    public void testNonCommutativeProdWithReverse() {
        // Test non-commutative 2x2 matrix multiplication:
        // [a b]
        // [c d]
        class Mat {
            final long a, b, c, d;
            static final long MOD = 998244353;

            Mat(long a, long b, long c, long d) {
                this.a = a % MOD;
                this.b = b % MOD;
                this.c = c % MOD;
                this.d = d % MOD;
            }

            Mat multiply(Mat o) {
                return new Mat(
                        (a * o.a + b * o.c) % MOD,
                        (a * o.b + b * o.d) % MOD,
                        (c * o.a + d * o.c) % MOD,
                        (c * o.b + d * o.d) % MOD
                );
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof Mat o)) return false;
                return a == o.a && b == o.b && c == o.c && d == o.d;
            }

            @Override
            public String toString() {
                return "[" + a + " " + b + "; " + c + " " + d + "]";
            }
        }

        Mat id = new Mat(1, 0, 0, 1);
        ReversibleLazyImplicitTreap<Void, Mat> treap = new ReversibleLazyImplicitTreap<>(
                Mat::multiply,
                id,
                (f, g) -> null,
                null,
                (f, x, size) -> x
        );

        Mat m0 = new Mat(1, 2, 3, 4);
        Mat m1 = new Mat(2, 0, 1, 3);
        Mat m2 = new Mat(1, 1, 0, 2);
        Mat m3 = new Mat(3, 1, 2, 1);

        treap.insert(0, m0);
        treap.insert(1, m1);
        treap.insert(2, m2);
        treap.insert(3, m3);

        // Expected in-order product: m0 * m1 * m2 * m3
        Mat expectedAll = m0.multiply(m1).multiply(m2).multiply(m3);
        assertEquals(expectedAll, treap.prod(0, 4));

        // Subsegment [1, 3): m1 * m2
        assertEquals(m1.multiply(m2), treap.prod(1, 3));

        // Reverse [1, 3) -> order becomes m0, m2, m1, m3
        treap.reverse(1, 3);
        Mat expectedAfterRev = m0.multiply(m2).multiply(m1).multiply(m3);
        assertEquals(expectedAfterRev, treap.prod(0, 4));
        assertEquals(m2.multiply(m1), treap.prod(1, 3));

        // Reverse [0, 4) -> order becomes m3, m1, m2, m0
        treap.reverse(0, 4);
        Mat expectedAllRev = m3.multiply(m1).multiply(m2).multiply(m0);
        assertEquals(expectedAllRev, treap.prod(0, 4));
        assertEquals(m1.multiply(m2), treap.prod(1, 3));
        assertEquals(m3.multiply(m1), treap.prod(0, 2));
        assertEquals(m2.multiply(m0), treap.prod(2, 4));
    }

    @Test
    public void testLazyApplyAndReverseInteraction() {
        // Affine transformation on sum:
        // Value: sum of elements
        // Action: pair (a, b) mapping x -> a * x + b
        // Composition: (a1, b1) o (a2, b2) = (a1 * a2, a1 * b2 + b1)
        record Affine(long a, long b) {
            static final long MOD = 1000000007;

            static Affine compose(Affine f, Affine g) {
                // f(g(x)) = a1 * (a2 * x + b2) + b1 = (a1 * a2) * x + (a1 * b2 + b1)
                return new Affine((f.a * g.a) % MOD, ((f.a * g.b) % MOD + f.b) % MOD);
            }
        }

        long MOD = 1000000007;
        Affine idAction = new Affine(1, 0);

        ReversibleLazyImplicitTreap<Affine, Long> treap = new ReversibleLazyImplicitTreap<>(
                (x, y) -> (x + y) % MOD,
                0L,
                Affine::compose,
                idAction,
                (action, aggregate, size) -> (action.a * aggregate % MOD + action.b * size % MOD) % MOD
        );

        for (int i = 0; i < 5; i++) {
            treap.insert(i, (long) (i + 1)); // [1, 2, 3, 4, 5]
        }

        assertEquals(15L, treap.prod(0, 5));

        // Apply * 2 + 3 to [1, 4) (indices 1, 2, 3 -> values 2, 3, 4)
        // 2 * 2 + 3 = 7
        // 3 * 2 + 3 = 9
        // 4 * 2 + 3 = 11
        // array is now: [1, 7, 9, 11, 5]
        treap.apply(1, 4, new Affine(2, 3));

        assertEquals(1L, treap.kth(0));
        assertEquals(7L, treap.kth(1));
        assertEquals(9L, treap.kth(2));
        assertEquals(11L, treap.kth(3));
        assertEquals(5L, treap.kth(4));
        assertEquals(33L, treap.prod(0, 5));
        assertEquals(27L, treap.prod(1, 4));

        // Reverse [0, 4): [1, 7, 9, 11] -> [11, 9, 7, 1], so full array: [11, 9, 7, 1, 5]
        treap.reverse(0, 4);

        assertEquals(11L, treap.kth(0));
        assertEquals(9L, treap.kth(1));
        assertEquals(7L, treap.kth(2));
        assertEquals(1L, treap.kth(3));
        assertEquals(5L, treap.kth(4));
        assertEquals(33L, treap.prod(0, 5));
        assertEquals(27L, treap.prod(0, 3));

        // Apply +10 to [2, 5) (indices 2, 3, 4 -> values 7, 1, 5 become 17, 11, 15)
        // full array: [11, 9, 17, 11, 15]
        treap.apply(2, 5, new Affine(1, 10));

        assertEquals(11L, treap.kth(0));
        assertEquals(9L, treap.kth(1));
        assertEquals(17L, treap.kth(2));
        assertEquals(11L, treap.kth(3));
        assertEquals(15L, treap.kth(4));
        assertEquals(63L, treap.prod(0, 5));
    }

    @Test
    public void testRandomizedStressComparison() {
        // Compare with naive List implementation over random operations
        Random rng = new Random(42);
        List<Long> naive = new ArrayList<>();

        ReversibleLazyImplicitTreap<Long, Long> treap = new ReversibleLazyImplicitTreap<>(
                Long::sum,
                0L,
                Long::sum,
                0L,
                (action, aggregate, size) -> aggregate + action * size
        );

        int nOps = 1000;
        for (int op = 0; op < nOps; op++) {
            int type = rng.nextInt(7);
            if (naive.isEmpty()) {
                type = 0; // force insert if empty
            }

            switch (type) {
                case 0 -> { // insert
                    int pos = rng.nextInt(naive.size() + 1);
                    long val = rng.nextInt(100);
                    naive.add(pos, val);
                    treap.insert(pos, val);
                }
                case 1 -> { // erase
                    int pos = rng.nextInt(naive.size());
                    naive.remove(pos);
                    treap.erase(pos);
                }
                case 2 -> { // kth
                    int pos = rng.nextInt(naive.size());
                    assertEquals(naive.get(pos), treap.kth(pos));
                }
                case 3 -> { // set
                    int pos = rng.nextInt(naive.size());
                    long val = rng.nextInt(100);
                    naive.set(pos, val);
                    treap.set(pos, val);
                }
                case 4 -> { // reverse
                    int l = rng.nextInt(naive.size());
                    int r = rng.nextInt(naive.size()) + 1;
                    if (l > r) {
                        int tmp = l;
                        l = r;
                        r = tmp;
                    }
                    Collections.reverse(naive.subList(l, r));
                    treap.reverse(l, r);
                }
                case 5 -> { // apply
                    int l = rng.nextInt(naive.size());
                    int r = rng.nextInt(naive.size()) + 1;
                    if (l > r) {
                        int tmp = l;
                        l = r;
                        r = tmp;
                    }
                    long add = rng.nextInt(50) - 25;
                    for (int i = l; i < r; i++) {
                        naive.set(i, naive.get(i) + add);
                    }
                    treap.apply(l, r, add);
                }
                case 6 -> { // prod
                    int l = rng.nextInt(naive.size());
                    int r = rng.nextInt(naive.size()) + 1;
                    if (l > r) {
                        int tmp = l;
                        l = r;
                        r = tmp;
                    }
                    long expectedSum = 0;
                    for (int i = l; i < r; i++) {
                        expectedSum += naive.get(i);
                    }
                    assertEquals(expectedSum, treap.prod(l, r));
                }
            }
            assertEquals(naive.size(), treap.size());
        }

        // Final check of entire list
        for (int i = 0; i < naive.size(); i++) {
            assertEquals(naive.get(i), treap.kth(i));
        }
        assertArrayEquals(naive.toArray(Long[]::new), treap.toArray(Long[]::new));
    }

    @Test
    public void testBoundsAndExceptions() {
        ReversibleLazyImplicitTreap<Integer, Integer> treap = new ReversibleLazyImplicitTreap<>(
                Integer::sum,
                0,
                Integer::sum,
                0,
                (action, aggregate, size) -> aggregate + action * size
        );

        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(0));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.insert(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.insert(1, 10));

        treap.insert(0, 10);
        treap.insert(1, 20);

        assertThrows(IndexOutOfBoundsException.class, () -> treap.erase(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.erase(2));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.kth(2));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.set(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.set(2, 5));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.prod(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.prod(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.prod(2, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.reverse(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.reverse(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.reverse(2, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> treap.apply(-1, 1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.apply(0, 3, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> treap.apply(2, 1, 5));
    }

    @Test
    public void testLazySegTreeStrategyConstructor() {
        // Range addition on max query using standard LazySegTreeStrategy
        LazySegTreeStrategy<Long, Long> strategy = new LazySegTreeStrategy<>() {
            @Override
            public Long identityX() {
                return Long.MIN_VALUE / 2;
            }

            @Override
            public Long mergeX(Long a, Long b) {
                return Math.max(a, b);
            }

            @Override
            public Long identityA() {
                return 0L;
            }

            @Override
            public Long mergeA(Long newer, Long older) {
                return newer + older;
            }

            @Override
            public Long mergeAX(Long a, Long x) {
                return a + x;
            }
        };

        ReversibleLazyImplicitTreap<Long, Long> treap = new ReversibleLazyImplicitTreap<>(strategy);

        treap.insert(0, 10L);
        treap.insert(1, 20L);
        treap.insert(2, 5L);

        assertEquals(20L, treap.prod(0, 3));
        assertEquals(20L, treap.prod(0, 2));

        // Add 15 to [1, 3) -> 20+15=35, 5+15=20
        treap.apply(1, 3, 15L);
        assertEquals(35L, treap.prod(0, 3));
        assertEquals(35L, treap.kth(1));
        assertEquals(20L, treap.kth(2));

        // Reverse [0, 3) -> [20, 35, 10]
        treap.reverse(0, 3);
        assertEquals(20L, treap.kth(0));
        assertEquals(35L, treap.kth(1));
        assertEquals(10L, treap.kth(2));
        assertEquals(35L, treap.prod(0, 2));
    }

}
