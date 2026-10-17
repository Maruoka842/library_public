package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;
import library.util.seq.SortedArrays;

public class CompressedSparseSegTree2DTest {

    @Test
    public void testBasic() {
        // Addition strategy
        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(Long::sum, 0L);

        // Register points
        tree.register(10, 10);
        tree.register(30, 30);
        tree.register(10, 30);
        tree.register(20, 20);

        // Build
        tree.build();

        // Set values via mul (which applies the binary operator)
        tree.mul(10, 10, 5);
        tree.mul(30, 30, 15);
        tree.mul(10, 30, 25);
        tree.mul(20, 20, 10);

        // Assert single point values
        assertEquals(5, tree.get(10, 10));
        assertEquals(25, tree.get(10, 30));
        assertEquals(10, tree.get(20, 20));
        assertEquals(15, tree.get(30, 30));

        // Unregistered should return identity
        assertEquals(0, tree.get(5, 5));
        assertEquals(0, tree.get(10, 20));

        // Rectangular range query fold(lx, ly, rx, ry): [lx, rx) x [ly, ry)
        assertEquals(10, tree.fold(20, 20, 21, 21)); // (20, 20)
        assertEquals(40, tree.fold(10, 10, 25, 35)); // (10,10), (10,30), (20,20) -> 5 + 25 + 10 = 40
        assertEquals(50, tree.fold(10, 20, 35, 35)); // (10,30), (20,20), (30,30) -> 25 + 10 + 15 = 50
        assertEquals(55, tree.fold(10, 10, 35, 35)); // all four points -> 55

        // Empty/wrong range
        assertEquals(0, tree.fold(10, 10, 10, 20));
        assertEquals(0, tree.fold(10, 10, 5, 20));

        // Test clear
        tree.clear();
        assertEquals(0, tree.get(10, 10));
        assertEquals(0, tree.get(20, 20));
        assertEquals(0, tree.fold(5, 5, 35, 35));
    }

    @Test
    public void testCommutativeMonoidStrategy() {
        // Implement custom LongCommutativeMonoidStrategy (Max strategy)
        LongCommutativeMonoidStrategy maxMonoid = new LongCommutativeMonoidStrategy() {
            @Override
            public long identity() {
                return Long.MIN_VALUE;
            }
            @Override
            public long mul(long a, long b) {
                return Math.max(a, b);
            }
        };

        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(maxMonoid);
        tree.register(1, 1);
        tree.register(1, 2);
        tree.register(2, 1);
        tree.register(2, 2);

        tree.build();

        tree.mul(1, 1, 10);
        tree.mul(1, 2, 20);
        tree.mul(2, 1, 5);
        tree.mul(2, 2, 15);

        assertEquals(10, tree.get(1, 1));
        assertEquals(20, tree.get(1, 2));
        assertEquals(5, tree.get(2, 1));
        assertEquals(15, tree.get(2, 2));

        // Query ranges
        assertEquals(20, tree.fold(1, 1, 3, 3)); // Max of all: 20
        assertEquals(10, tree.fold(1, 1, 2, 2)); // (1, 1) -> 10
        assertEquals(20, tree.fold(1, 1, 2, 3)); // (1, 1), (1, 2) -> 20
        assertEquals(15, tree.fold(2, 1, 3, 3)); // (2, 1), (2, 2) -> 15
    }

    @Test
    public void testNegativeAndDuplicateCoordinates() {
        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(Long::sum, 0L);

        // Register coordinates including duplicates
        tree.registerKey(-5, -100);
        tree.registerKey(-5, -100);
        tree.registerKey(100, 200);
        tree.registerKey(-5, 200);

        tree.build();

        tree.mul(-5, -100, -10);
        tree.mul(100, 200, 20);
        tree.mul(-5, 200, 5);

        assertEquals(-10, tree.get(-5, -100));
        assertEquals(20, tree.get(100, 200));
        assertEquals(5, tree.get(-5, 200));

        // sum of all
        assertEquals(15, tree.fold(-10, -200, 200, 300));
    }

    @Test
    public void testUnregisteredThrows() {
        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(Long::sum, 0L);
        tree.register(5, 5);
        tree.build();

        // Valid
        tree.mul(5, 5, 10);
        assertEquals(10, tree.get(5, 5));

        // Unregistered x coordinate
        assertThrows(AssertionError.class, () -> tree.mul(10, 5, 20));

        // Unregistered y coordinate for registered x
        assertThrows(AssertionError.class, () -> tree.mul(5, 10, 20));
    }

    @Test
    public void testNotBuiltThrows() {
        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(Long::sum, 0L);
        tree.register(1, 1);
        assertThrows(AssertionError.class, () -> tree.mul(1, 1, 10));
        assertThrows(AssertionError.class, () -> tree.fold(1, 1, 2, 2));
    }

    @Test
    public void testEmpty() {
        CompressedSparseSegTree2D tree = new CompressedSparseSegTree2D(Long::sum, 0L);
        tree.build(); // Empty coordinates

        assertEquals(0, tree.fold(10, 10, 20, 20));
        assertEquals(0, tree.fold(-100, -100, 100, 100));
        assertThrows(AssertionError.class, () -> tree.mul(0, 0, 10));
    }

    @Test
    public void testUnique() {
        long[] a = {1, 1, 2, 2, 2, 3, 4, 4, 5};
        long[] expected = {1, 2, 3, 4, 5};
        assertArrayEquals(expected, SortedArrays.unique(a));

        long[] empty = {};
        assertArrayEquals(empty, SortedArrays.unique(empty));

        long[] single = {42};
        assertArrayEquals(single, SortedArrays.unique(single));
    }
}
