package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;
import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;

public class CompressedSparseBinaryIndexedTree2DTest {

    @Test
    public void testBasic() {
        // Uses the LongAddAbelianGroupStrategy
        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(LongAddAbelianGroupStrategy.STRATEGY);

        // Register points
        bit.registerKey(10, 10);
        bit.registerKey(30, 30);
        bit.registerKey(10, 30);
        bit.registerKey(20, 20);

        // Build
        bit.build();

        // Set values via add
        bit.add(10, 10, 5);
        bit.add(30, 30, 15);
        bit.add(10, 30, 25);
        bit.add(20, 20, 10);

        // Assert single point values (get uses sum(x, y, x+1, y+1))
        assertEquals(5, bit.get(10, 10));
        assertEquals(25, bit.get(10, 30));
        assertEquals(10, bit.get(20, 20));
        assertEquals(15, bit.get(30, 30));

        // prefixSum queries: half-open interval (-inf, x) x (-inf, y)
        assertEquals(0, bit.prefixSum(10, 10));
        assertEquals(5, bit.prefixSum(11, 11));
        assertEquals(5, bit.prefixSum(20, 20));
        assertEquals(15, bit.prefixSum(21, 21)); // (10,10) and (20,20) -> 5 + 10 = 15
        assertEquals(15, bit.prefixSum(30, 30)); // (10,10) and (20,20) -> 5 + 10 = 15
        assertEquals(55, bit.prefixSum(31, 31)); // all four points -> 5 + 25 + 10 + 15 = 55
        assertEquals(55, bit.prefixSum(35, 35)); // all four points -> 5 + 25 + 10 + 15 = 55

        // Rectangular range query sum(lx, ly, rx, ry): [lx, rx) x [ly, ry)
        assertEquals(10, bit.sum(20, 20, 21, 21)); // (20, 20)
        assertEquals(40, bit.sum(10, 10, 25, 35)); // (10,10), (10,30), (20,20) -> 5 + 25 + 10 = 40
        assertEquals(50, bit.sum(10, 20, 35, 35)); // (10,30), (20,20), (30,30) -> 25 + 10 + 15 = 50
        assertEquals(55, bit.sum(10, 10, 35, 35)); // all four points -> 55

        // Test clear
        bit.clear();
        assertEquals(0, bit.get(10, 10));
        assertEquals(0, bit.get(20, 20));
        assertEquals(0, bit.prefixSum(35, 35));
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

        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(xorStrategy);
        bit.registerKey(1, 1);
        bit.registerKey(1, 2);
        bit.registerKey(2, 1);
        bit.registerKey(2, 2);

        bit.build();

        bit.add(1, 1, 1);
        bit.add(1, 2, 2);
        bit.add(2, 1, 4);
        bit.add(2, 2, 8);

        assertEquals(1, bit.get(1, 1));
        assertEquals(2, bit.get(1, 2));
        assertEquals(4, bit.get(2, 1));
        assertEquals(8, bit.get(2, 2));

        assertEquals(1 ^ 2 ^ 4 ^ 8, bit.prefixSum(3, 3));
        assertEquals(1 ^ 2, bit.sum(1, 1, 2, 3));
    }

    @Test
    public void testCommutativeMonoidStrategy() {
        // Custom monoid strategy without inverse (e.g. gcd/min/max? wait, Fenwick tree only supports
        // correct updates if we accumulate, let's use bitwise OR or custom addition monoid)
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

        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(addMonoid);
        bit.registerKey(1, 1);
        bit.registerKey(2, 2);

        bit.build();

        bit.add(1, 1, 10);
        bit.add(2, 2, 20);

        // prefixSum is fully supported
        assertEquals(0, bit.prefixSum(1, 1));
        assertEquals(10, bit.prefixSum(2, 2));
        assertEquals(30, bit.prefixSum(3, 3));

        // sum and get are not supported and should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> bit.sum(1, 1, 3, 3));
        assertThrows(UnsupportedOperationException.class, () -> bit.get(1, 1));
    }

    @Test
    public void testNegativeAndDuplicateCoordinates() {
        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(LongAddAbelianGroupStrategy.STRATEGY);

        // Register coordinates including duplicates
        bit.registerKey(-5, -100);
        bit.registerKey(-5, -100);
        bit.registerKey(100, 200);
        bit.registerKey(-5, 200);

        bit.build();

        bit.add(-5, -100, -10);
        bit.add(100, 200, 20);
        bit.add(-5, 200, 5);

        assertEquals(-10, bit.get(-5, -100));
        assertEquals(20, bit.get(100, 200));
        assertEquals(5, bit.get(-5, 200));

        // sum of all
        assertEquals(15, bit.sum(-10, -200, 200, 300));
    }

    @Test
    public void testUnregisteredThrows() {
        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(LongAddAbelianGroupStrategy.STRATEGY);
        bit.registerKey(5, 5);
        bit.build();

        // Valid
        bit.add(5, 5, 10);
        assertEquals(10, bit.get(5, 5));

        // Unregistered x coordinate
        assertThrows(AssertionError.class, () -> bit.add(10, 5, 20));

        // Unregistered y coordinate for registered x
        assertThrows(AssertionError.class, () -> bit.add(5, 10, 20));
    }

    @Test
    public void testNotBuiltThrows() {
        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(LongAddAbelianGroupStrategy.STRATEGY);
        bit.registerKey(1, 1);
        assertThrows(AssertionError.class, () -> bit.add(1, 1, 10));
        assertThrows(AssertionError.class, () -> bit.prefixSum(2, 2));
    }

    @Test
    public void testEmpty() {
        CompressedSparseBinaryIndexedTree2D bit = new CompressedSparseBinaryIndexedTree2D(LongAddAbelianGroupStrategy.STRATEGY);
        bit.build(); // Empty coordinates

        assertEquals(0, bit.prefixSum(10, 10));
        assertEquals(0, bit.sum(-100, -100, 100, 100));
        assertThrows(AssertionError.class, () -> bit.add(0, 0, 10));
    }
}
