package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;

public class FastUpdateAbelianGroupSqrtDecompositionLongTest {
    @Test
    public void randomUpdatesAndFoldsMatchNaiveSums() {
        Random random = new Random(2);
        for (int n = 0; n <= 50; n++) {
            long[] values = new long[n];
            for (int i = 0; i < n; i++) values[i] = random.nextInt(201) - 100;
            FastUpdateAbelianGroupSqrtDecompositionLong sequence =
                    FastUpdateAbelianGroupSqrtDecompositionFactory.sum(values);
            for (int query = 0; query < 300; query++) {
                if (n > 0 && random.nextBoolean()) {
                    int i = random.nextInt(n);
                    values[i] = random.nextInt(201) - 100;
                    sequence.set(i, values[i]);
                } else {
                    int l = random.nextInt(n + 1);
                    int r = l + random.nextInt(n - l + 1);
                    assertEquals(naiveSum(values, l, r), sequence.fold(l, r));
                }
            }
            assertEquals(naiveSum(values, 0, n), sequence.prodAll());
        }
    }

    @Test
    public void setUsesOneInverseAndFoldUsesNoInverse() {
        CountingAddGroup strategy = new CountingAddGroup();
        FastUpdateAbelianGroupSqrtDecompositionLong sequence =
                new FastUpdateAbelianGroupSqrtDecompositionLong(
                        new long[] {1, 2, 3, 4, 5}, strategy);

        assertEquals(0, strategy.inverseCount);
        sequence.set(2, 10);
        assertEquals(1, strategy.inverseCount);
        assertEquals(22, sequence.fold(0, 5));
        assertEquals(1, strategy.inverseCount);
        sequence.mul(0, 7);
        assertEquals(1, strategy.inverseCount);
        assertEquals(29, sequence.prodAll());
    }

    @Test
    public void emptySequenceAndBoundsAreDefined() {
        FastUpdateAbelianGroupSqrtDecompositionLong empty =
                FastUpdateAbelianGroupSqrtDecompositionFactory.sum(0);

        assertEquals(0, empty.fold(0, 0));
        assertEquals(0, empty.prodAll());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.fold(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.fold(0, 1));
    }

    private static long naiveSum(long[] values, int l, int r) {
        long result = 0;
        for (int i = l; i < r; i++) result += values[i];
        return result;
    }

    private static class CountingAddGroup implements LongAbelianGroupStrategy {
        /** {@link #inverse(long)} の呼び出し回数。 */
        int inverseCount;

        @Override
        public long identity() {
            return 0;
        }

        @Override
        public long mul(long a, long b) {
            return a + b;
        }

        @Override
        public long inverse(long a) {
            inverseCount++;
            return -a;
        }
    }
}
