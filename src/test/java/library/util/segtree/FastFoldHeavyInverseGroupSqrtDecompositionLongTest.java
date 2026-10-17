package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import library.util.algebra.strategy.longs.LongGroupStrategy;

public class FastFoldHeavyInverseGroupSqrtDecompositionLongTest {
    @Test
    public void foldUsesNoInverseAndSetUsesOneInverse() {
        CountingAddGroup strategy = new CountingAddGroup();
        long[] values = {3, -1, 4, 1, 5, -9, 2, 6, 5};
        FastFoldHeavyInverseGroupSqrtDecompositionLong sequence =
                new FastFoldHeavyInverseGroupSqrtDecompositionLong(values, strategy);
        assertEquals(values.length, strategy.inverseCount);

        strategy.inverseCount = 0;
        for (int l = 0; l <= values.length; l++) {
            for (int r = l; r <= values.length; r++) {
                assertEquals(naiveSum(values, l, r), sequence.fold(l, r));
            }
        }
        assertEquals(0, strategy.inverseCount);

        sequence.set(4, 12);
        values[4] = 12;
        assertEquals(1, strategy.inverseCount);
        assertEquals(naiveSum(values, 2, 8), sequence.fold(2, 8));
        assertEquals(1, strategy.inverseCount);

        sequence.mul(3, 7);
        values[3] += 7;
        assertEquals(2, strategy.inverseCount);
        assertEquals(naiveSum(values, 0, values.length), sequence.prodAll());
    }

    @Test
    public void cachedInversesPreserveOrderInANonCommutativeGroup() {
        CountingPermutationGroup strategy = new CountingPermutationGroup();
        long[] values = {
                0b10_00_01,
                0b01_10_00,
                0b00_10_01,
                0b01_00_10,
                0b10_01_00,
                0b00_01_10,
                0b10_00_01
        };
        FastFoldHeavyInverseGroupSqrtDecompositionLong sequence =
                new FastFoldHeavyInverseGroupSqrtDecompositionLong(values, strategy);

        assertAllPermutationFolds(values, sequence, strategy);
        assertEquals(values.length, strategy.inverseCount);

        values[3] = 0b10_01_00;
        sequence.set(3, values[3]);
        assertAllPermutationFolds(values, sequence, strategy);
        assertEquals(values.length + 1, strategy.inverseCount);
    }

    private static long naiveSum(long[] values, int l, int r) {
        long result = 0;
        for (int i = l; i < r; i++) result += values[i];
        return result;
    }

    private static void assertAllPermutationFolds(long[] values,
            FastFoldHeavyInverseGroupSqrtDecompositionLong sequence,
            CountingPermutationGroup strategy) {
        for (int l = 0; l <= values.length; l++) {
            for (int r = l; r <= values.length; r++) {
                long expected = strategy.identity();
                for (int i = l; i < r; i++) expected = strategy.mul(expected, values[i]);
                assertEquals(expected, sequence.fold(l, r));
            }
        }
    }

    private static class CountingAddGroup implements LongGroupStrategy {
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

    private static class CountingPermutationGroup implements LongGroupStrategy {
        /** {@link #inverse(long)} の呼び出し回数。 */
        int inverseCount;

        @Override
        public long identity() {
            return 0b10_01_00;
        }

        @Override
        public long mul(long a, long b) {
            long result = 0;
            for (int i = 0; i < 3; i++) {
                int bi = (int) (b >>> (2 * i)) & 3;
                long abi = (a >>> (2 * bi)) & 3;
                result |= abi << (2 * i);
            }
            return result;
        }

        @Override
        public long inverse(long a) {
            inverseCount++;
            long result = 0;
            for (int i = 0; i < 3; i++) {
                int ai = (int) (a >>> (2 * i)) & 3;
                result |= (long) i << (2 * ai);
            }
            return result;
        }
    }
}
