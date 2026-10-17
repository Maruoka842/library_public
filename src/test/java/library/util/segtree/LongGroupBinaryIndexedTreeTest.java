package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongZStrategy;

public class LongGroupBinaryIndexedTreeTest {
    static class LongZAbelianGroupStrategy implements LongAbelianGroupStrategy {
        LongZStrategy z = new LongZStrategy();
        @Override public long identity() { return z.zero(); }
        @Override public long mul(long a, long b) { return z.add(a, b); }
        @Override public long inverse(long a) { return z.neg(a); }
    }

    @Test
    public void testBasic() {
        int n = 10;
        LongZAbelianGroupStrategy strategy = new LongZAbelianGroupStrategy();
        LongAbelianGroupBinaryIndexedTree bit = new LongAbelianGroupBinaryIndexedTree(n, strategy);

        bit.add(2, 5);
        bit.add(5, 10);
        bit.add(8, -3);

        assertEquals(0, bit.prefixSum(0));
        assertEquals(0, bit.prefixSum(1));
        assertEquals(0, bit.prefixSum(2));
        assertEquals(5, bit.prefixSum(3));
        assertEquals(5, bit.prefixSum(4));
        assertEquals(5, bit.prefixSum(5));
        assertEquals(15, bit.prefixSum(6));
        assertEquals(15, bit.prefixSum(7));
        assertEquals(15, bit.prefixSum(8));
        assertEquals(12, bit.prefixSum(9));
        assertEquals(12, bit.prefixSum(10));

        assertEquals(5, bit.fold(2, 3));
        assertEquals(10, bit.fold(5, 6));
        assertEquals(-3, bit.fold(8, 9));
        assertEquals(15, bit.fold(0, 6));
        assertEquals(12, bit.fold(0, 10));
        assertEquals(7, bit.fold(5, 10));
        assertEquals(0, bit.fold(3, 3));
        assertEquals(0, bit.fold(5, 3));
    }

    @Test
    public void testClear() {
        int n = 5;
        LongZAbelianGroupStrategy strategy = new LongZAbelianGroupStrategy();
        LongAbelianGroupBinaryIndexedTree bit = new LongAbelianGroupBinaryIndexedTree(n, strategy);

        bit.add(1, 10);
        bit.add(3, 20);
        assertEquals(30, bit.prefixSum(5));

        bit.clear();
        assertEquals(0, bit.prefixSum(5));
        assertEquals(0, bit.fold(0, 5));
    }

    @Test
    public void testBuild() {
        int n = 5;
        LongZAbelianGroupStrategy strategy = new LongZAbelianGroupStrategy();
        LongAbelianGroupBinaryIndexedTree bit = new LongAbelianGroupBinaryIndexedTree(n, strategy);

        long[] arr = {3, 1, 4, 1, 5};
        bit.build(arr);

        assertEquals(0, bit.prefixSum(0));
        assertEquals(3, bit.prefixSum(1));
        assertEquals(4, bit.prefixSum(2));
        assertEquals(8, bit.prefixSum(3));
        assertEquals(9, bit.prefixSum(4));
        assertEquals(14, bit.prefixSum(5));

        assertEquals(3, bit.get(0));
        assertEquals(1, bit.get(1));
        assertEquals(4, bit.get(2));
        assertEquals(1, bit.get(3));
        assertEquals(5, bit.get(4));

        assertEquals(6, bit.fold(1, 4)); // 1 + 4 + 1
        assertEquals(14, bit.fold(0, 5));

        // Re-building with a new array
        long[] newArr = {10, 20, 30, 40, 50};
        bit.build(newArr);

        assertEquals(10, bit.get(0));
        assertEquals(150, bit.prefixSum(5));
        assertEquals(90, bit.fold(1, 4)); // 20 + 30 + 40
    }

    @Test
    public void testBuildEdgeCases() {
        int n = 5;
        LongZAbelianGroupStrategy strategy = new LongZAbelianGroupStrategy();
        LongAbelianGroupBinaryIndexedTree bit = new LongAbelianGroupBinaryIndexedTree(n, strategy);

        // Array shorter than N
        long[] shortArr = {5, 10};
        bit.build(shortArr);
        assertEquals(5, bit.get(0));
        assertEquals(10, bit.get(1));
        assertEquals(0, bit.get(2));
        assertEquals(15, bit.prefixSum(5));

        // Array longer than N
        long[] longArr = {1, 2, 3, 4, 5, 6, 7};
        bit.build(longArr);
        assertEquals(1, bit.get(0));
        assertEquals(5, bit.get(4));
        assertEquals(15, bit.prefixSum(5));
    }

    @Test
    public void testMonoidBuild() {
        int n = 4;
        LongZAbelianGroupStrategy strategy = new LongZAbelianGroupStrategy();
        LongCommutativeMonoidBinaryIndexedTree bit = new LongCommutativeMonoidBinaryIndexedTree(n, strategy);

        long[] arr = {2, 4, 6, 8};
        bit.build(arr);

        assertEquals(0, bit.prefixSum(0));
        assertEquals(2, bit.prefixSum(1));
        assertEquals(6, bit.prefixSum(2));
        assertEquals(12, bit.prefixSum(3));
        assertEquals(20, bit.prefixSum(4));
    }
}
