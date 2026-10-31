package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryIndexedTree2DTest {

    @Test
    public void testGeneric() {
        // XOR group
        int H = 3, W = 3;
        BinaryIndexedTree2D bit = new BinaryIndexedTree2D(H, W, (x, y) -> x ^ y, x -> x, 0L);

        bit.add(0, 0, 1);
        bit.add(0, 1, 2);
        bit.add(1, 0, 4);
        bit.add(1, 1, 8);

        assertEquals(1, bit.get(0, 0));
        assertEquals(2, bit.get(0, 1));
        assertEquals(4, bit.get(1, 0));
        assertEquals(8, bit.get(1, 1));

        assertEquals(1 ^ 2 ^ 4 ^ 8, bit.prefixSum(2, 2));
        assertEquals(2 ^ 8, bit.sum(0, 1, 2, 2));
    }
}
