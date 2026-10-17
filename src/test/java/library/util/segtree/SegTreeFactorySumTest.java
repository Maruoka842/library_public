package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

public class SegTreeFactorySumTest {
    @Test
    public void createsBinaryIndexedTreeSupportingExistingOperations() {
        var sum = SegTreeFactory.sum(5);

        assertInstanceOf(LongAbelianGroupBinaryIndexedTree.class, sum);
        sum.add(1, 3);
        sum.add(3, 7);
        assertEquals(10, sum.fold(0, 5));
        assertEquals(3, sum.get(1));

        sum.set(1, -2);
        assertEquals(5, sum.fold(1, 4));
        assertEquals(-2, sum.get(1));

        sum.clear();
        assertEquals(0, sum.fold(0, 5));
    }

    @Test
    public void createsModularAbelianGroupBinaryIndexedTree() {
        LongAbelianGroupBinaryIndexedTree sum = SegTreeFactory.modSum(5, 7);
        sum.add(0, 10);
        sum.add(1, -2);
        sum.add(3, Long.MAX_VALUE);

        assertEquals(3, sum.get(0));
        assertEquals(5, sum.get(1));
        assertEquals(Long.MAX_VALUE % 7, sum.get(3));
        assertEquals((3 + 5 + Long.MAX_VALUE % 7) % 7, sum.fold(0, 5));

        sum.set(1, 16);
        assertEquals(2, sum.get(1));
        sum.clear();
        assertEquals(0, sum.fold(0, 5));
    }
}
