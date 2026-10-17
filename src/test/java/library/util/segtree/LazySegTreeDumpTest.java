package library.util.segtree;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LazySegTreeDumpTest {

    @Test
    public void testLazySegTreeDumpNonDestructive() {
        LazySegTree<Long, Long> seg = new LazySegTree<>(4, new LazySegTreeStrategy<Long, Long>() {
            @Override
            public Long mergeX(Long x, Long y) {
                return x + y;
            }

            @Override
            public Long mergeA(Long a, Long b) {
                return a + b;
            }

            @Override
            public Long mergeAX(Long a, Long x) {
                return a + x;
            }

            @Override
            public Long identityX() {
                return 0L;
            }

            @Override
            public Long identityA() {
                return 0L;
            }
        });

        seg.act(0, 2, 5L);

        Object[] vArray = (Object[]) (Object) seg.v;
        Object[] lazyArray = (Object[]) (Object) seg.lazy;

        Object[] vBefore = vArray.clone();
        Object[] lazyBefore = lazyArray.clone();

        assertEquals(5L, seg.getNonDestructive(0));
        assertEquals(5L, seg.getNonDestructive(1));
        assertEquals(0L, seg.getNonDestructive(2));
        assertEquals(0L, seg.getNonDestructive(3));

        assertDoesNotThrow(() -> seg.dump());

        assertTrue(Arrays.equals(vBefore, (Object[]) (Object) seg.v));
        assertTrue(Arrays.equals(lazyBefore, (Object[]) (Object) seg.lazy));
    }

    @Test
    public void testGetNonDestructiveNonCommutative() {
        // Range Affine transformation (a, b): x -> a * x + b
        // Composition: f2(f1(x)) = a2 * (a1 * x + b1) + b2 = (a2 * a1) * x + (a2 * b1 + b2)
        record Affine(long a, long b) {}

        LazySegTree<Affine, Long> seg = new LazySegTree<>(4, new LazySegTreeStrategy<Affine, Long>() {
            @Override
            public Long mergeX(Long x, Long y) {
                return x + y;
            }

            @Override
            public Affine mergeA(Affine f2, Affine f1) {
                // f2 is applied AFTER f1: f2(f1(x))
                return new Affine(f2.a() * f1.a(), f2.a() * f1.b() + f2.b());
            }

            @Override
            public Long mergeAX(Affine f, Long x) {
                return f.a() * x + f.b();
            }

            @Override
            public Long identityX() {
                return 0L;
            }

            @Override
            public Affine identityA() {
                return new Affine(1L, 0L);
            }
        });

        // Initialize leaves: [1, 1, 1, 1]
        seg.build(new Long[]{1L, 1L, 1L, 1L});

        // Act 1: x -> 2x + 3 over [0, 2). Resulting leaves: [5, 5, 1, 1]
        seg.act(0, 2, new Affine(2L, 3L));

        // Act 2: x -> 3x + 1 over [0, 4). Resulting leaves: [3(5)+1=16, 16, 3(1)+1=4, 4]
        seg.act(0, 4, new Affine(3L, 1L));

        // Verify non-destructive values match fold() and get() exactly
        for (int i = 0; i < 4; i++) {
            Long expected = seg.get(i);
            assertEquals(expected, seg.getNonDestructive(i));
        }

        assertDoesNotThrow(() -> seg.dump());
    }
}
