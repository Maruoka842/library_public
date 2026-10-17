package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;

public class CompressedSparseBinaryIndexedTree3DTest {

    @Test
    public void testBasic() {
        // Uses the default constructor (which uses LongAddAbelianGroupStrategy.STRATEGY)
        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D();

        // Register points
        bit.register(10, 10, 10);
        bit.register(20, 20, 20);
        bit.register(10, 20, 30);
        bit.register(30, 10, 20);
        bit.register(30, 30, 30);

        // Build
        bit.build();

        // Set values via add
        bit.add(10, 10, 10, 5);
        bit.add(20, 20, 20, 10);
        bit.add(10, 20, 30, 15);
        bit.add(30, 10, 20, 20);
        bit.add(30, 30, 30, 25);

        // Assert single point values
        assertEquals(5, bit.get(10, 10, 10));
        assertEquals(10, bit.get(20, 20, 20));
        assertEquals(15, bit.get(10, 20, 30));
        assertEquals(20, bit.get(30, 10, 20));
        assertEquals(25, bit.get(30, 30, 30));

        // prefixSum queries: half-open interval (-inf, x) x (-inf, y) x (-inf, z)
        assertEquals(0, bit.prefixSum(10, 10, 10));
        assertEquals(5, bit.prefixSum(11, 11, 11)); // contains (10, 10, 10) -> 5
        assertEquals(5, bit.prefixSum(20, 20, 20)); // contains (10, 10, 10) -> 5
        assertEquals(15, bit.prefixSum(21, 21, 21)); // contains (10, 10, 10) and (20, 20, 20) -> 15

        // Rectangular range query sum(lx, ly, lz, rx, ry, rz): [lx, rx) x [ly, ry) x [lz, rz)
        assertEquals(10, bit.sum(20, 20, 20, 21, 21, 21)); // (20, 20, 20)

        // sum(10, 10, 10, 30, 30, 30) -> only contains (10,10,10) and (20,20,20)
        assertEquals(15, bit.sum(10, 10, 10, 30, 30, 30));

        // sum(10, 10, 10, 35, 35, 35) -> contains all five points
        assertEquals(75, bit.sum(10, 10, 10, 35, 35, 35));

        // sum of partial cuboid: x in [10, 25), y in [10, 25), z in [10, 25) -> (10, 10, 10) and (20, 20, 20)
        assertEquals(15, bit.sum(10, 10, 10, 25, 25, 25));

        // sum of region with only (10, 20, 30): x in [10, 20), y in [15, 25), z in [25, 35) -> 15
        assertEquals(15, bit.sum(10, 15, 25, 20, 25, 35));

        // Test clear
        bit.clear();
        assertEquals(0, bit.get(10, 10, 10));
        assertEquals(0, bit.get(20, 20, 20));
        assertEquals(0, bit.prefixSum(35, 35, 35));
    }

    @Test
    public void testAbelianGroupStrategy() {
        // Implement custom LongAbelianGroupStrategy (XOR strategy)
        LongAbelianGroupStrategy xorStrategy = new LongAbelianGroupStrategy() {
            @Override
            public long identity() {
                return 0L;
            }
            @Override
            public long mul(long a, long b) {
                return a ^ b;
            }
            @Override
            public long inverse(long a) {
                return a;
            }
        };

        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D(xorStrategy);
        bit.register(1, 1, 1);
        bit.register(1, 1, 2);
        bit.register(1, 2, 1);
        bit.register(1, 2, 2);
        bit.register(2, 1, 1);
        bit.register(2, 1, 2);
        bit.register(2, 2, 1);
        bit.register(2, 2, 2);

        bit.build();

        bit.add(1, 1, 1, 1);
        bit.add(1, 1, 2, 2);
        bit.add(1, 2, 1, 4);
        bit.add(1, 2, 2, 8);
        bit.add(2, 1, 1, 16);
        bit.add(2, 1, 2, 32);
        bit.add(2, 2, 1, 64);
        bit.add(2, 2, 2, 128);

        assertEquals(1, bit.get(1, 1, 1));
        assertEquals(2, bit.get(1, 1, 2));
        assertEquals(4, bit.get(1, 2, 1));
        assertEquals(8, bit.get(1, 2, 2));
        assertEquals(16, bit.get(2, 1, 1));
        assertEquals(32, bit.get(2, 1, 2));
        assertEquals(64, bit.get(2, 2, 1));
        assertEquals(128, bit.get(2, 2, 2));

        assertEquals(1 ^ 2 ^ 4 ^ 8 ^ 16 ^ 32 ^ 64 ^ 128, bit.prefixSum(3, 3, 3));
        assertEquals(1 ^ 2 ^ 4 ^ 8, bit.sum(1, 1, 1, 2, 3, 3));
    }

    @Test
    public void testCommutativeMonoidStrategy() {
        // Custom addition monoid without inverse
        LongCommutativeMonoidStrategy addMonoid = new LongCommutativeMonoidStrategy() {
            @Override
            public long identity() {
                return 0L;
            }
            @Override
            public long mul(long a, long b) {
                return a + b;
            }
        };

        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D(addMonoid);
        bit.register(1, 1, 1);
        bit.register(2, 2, 2);

        bit.build();

        bit.add(1, 1, 1, 10);
        bit.add(2, 2, 2, 20);

        // prefixSum is fully supported
        assertEquals(0, bit.prefixSum(1, 1, 1));
        assertEquals(10, bit.prefixSum(2, 2, 2));
        assertEquals(30, bit.prefixSum(3, 3, 3));

        // sum and get are not supported and should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> bit.sum(1, 1, 1, 3, 3, 3));
        assertThrows(UnsupportedOperationException.class, () -> bit.get(1, 1, 1));
    }

    @Test
    public void testNegativeAndDuplicateCoordinates() {
        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D();

        // Register coordinates including duplicates
        bit.registerKey(-5, -100, -50);
        bit.registerKey(-5, -100, -50);
        bit.registerKey(100, 200, 300);
        bit.registerKey(-5, 200, -50);

        bit.build();

        bit.add(-5, -100, -50, -10);
        bit.add(100, 200, 300, 20);
        bit.add(-5, 200, -50, 5);

        assertEquals(-10, bit.get(-5, -100, -50));
        assertEquals(20, bit.get(100, 200, 300));
        assertEquals(5, bit.get(-5, 200, -50));

        // sum of all
        assertEquals(15, bit.sum(-10, -200, -100, 200, 300, 400));
    }

    @Test
    public void testUnregisteredThrows() {
        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D();
        bit.register(5, 5, 5);
        bit.build();

        // Valid
        bit.add(5, 5, 5, 10);
        assertEquals(10, bit.get(5, 5, 5));

        // Unregistered x coordinate
        assertThrows(AssertionError.class, () -> bit.add(10, 5, 5, 20));

        // Unregistered y coordinate for registered x
        assertThrows(AssertionError.class, () -> bit.add(5, 10, 5, 20));

        // Unregistered z coordinate for registered x, y
        assertThrows(AssertionError.class, () -> bit.add(5, 5, 10, 20));
    }

    @Test
    public void testNotBuiltThrows() {
        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D();
        bit.register(1, 1, 1);
        assertThrows(AssertionError.class, () -> bit.add(1, 1, 1, 10));
        assertThrows(AssertionError.class, () -> bit.prefixSum(2, 2, 2));
    }

    @Test
    public void testEmpty() {
        CompressedSparseBinaryIndexedTree3D bit = new CompressedSparseBinaryIndexedTree3D();
        bit.build(); // Empty coordinates

        assertEquals(0, bit.prefixSum(10, 10, 10));
        assertEquals(0, bit.sum(-100, -100, -100, 100, 100, 100));
        assertThrows(AssertionError.class, () -> bit.add(0, 0, 0, 10));
    }
}
