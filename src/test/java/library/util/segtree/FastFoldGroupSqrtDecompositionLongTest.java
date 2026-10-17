package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.algebra.strategy.longs.LongGroupStrategy;

public class FastFoldGroupSqrtDecompositionLongTest {
    /** 積の順序を検査するための 3 次対称群。 */
    private static final LongGroupStrategy PERMUTATION_GROUP = new LongGroupStrategy() {
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
            long result = 0;
            for (int i = 0; i < 3; i++) {
                int ai = (int) (a >>> (2 * i)) & 3;
                result |= (long) i << (2 * ai);
            }
            return result;
        }
    };

    @Test
    public void factorySumSupportsBuildFoldAndUpdates() {
        FastFoldGroupSqrtDecompositionLong sequence =
                FastFoldGroupSqrtDecompositionFactory.sum(new long[] {1, 2, 3, 4, 5});

        assertEquals(0, sequence.fold(2, 2));
        assertEquals(9, sequence.fold(1, 4));
        assertEquals(15, sequence.prodAll());
        assertEquals(12, sequence.suffixFold(2));

        sequence.set(2, 10);
        assertEquals(16, sequence.fold(1, 4));
        sequence.mul(4, -2);
        assertEquals(20, sequence.prodAll());
        assertEquals(2, sequence.get(1));
    }

    @Test
    public void foldPreservesOrderInANonCommutativeGroup() {
        long[] permutations = {
                0b10_00_01,
                0b01_10_00,
                0b00_10_01,
                0b01_00_10,
                0b10_01_00,
                0b00_01_10,
                0b10_00_01
        };
        FastFoldGroupSqrtDecompositionLong sequence =
                new FastFoldGroupSqrtDecompositionLong(permutations, PERMUTATION_GROUP);

        for (int l = 0; l <= permutations.length; l++) {
            for (int r = l; r <= permutations.length; r++) {
                assertEquals(naiveFold(permutations, l, r), sequence.fold(l, r));
            }
        }

        permutations[3] = 0b10_01_00;
        sequence.set(3, permutations[3]);
        for (int l = 0; l <= permutations.length; l++) {
            for (int r = l; r <= permutations.length; r++) {
                assertEquals(naiveFold(permutations, l, r), sequence.fold(l, r));
            }
        }
    }

    @Test
    public void randomPointAssignmentsMatchNaiveFolds() {
        Random random = new Random(1);
        for (int n = 0; n <= 40; n++) {
            long[] values = new long[n];
            for (int i = 0; i < n; i++) values[i] = random.nextInt(101) - 50;
            FastFoldGroupSqrtDecompositionLong sequence =
                    FastFoldGroupSqrtDecompositionFactory.sum(values);
            for (int query = 0; query < 200; query++) {
                if (n > 0 && random.nextBoolean()) {
                    int i = random.nextInt(n);
                    values[i] = random.nextInt(101) - 50;
                    sequence.set(i, values[i]);
                } else {
                    int l = random.nextInt(n + 1);
                    int r = l + random.nextInt(n - l + 1);
                    assertEquals(naiveSum(values, l, r), sequence.fold(l, r));
                }
            }
        }
    }

    @Test
    public void emptySequenceAndBoundsAreDefined() {
        FastFoldGroupSqrtDecompositionLong empty = FastFoldGroupSqrtDecompositionFactory.sum(0);

        assertEquals(0, empty.fold(0, 0));
        assertEquals(0, empty.prodAll());
        assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.fold(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.fold(0, 1));
    }

    @Test
    public void modularFactoryPreservesElementsAndNormalizesFolds() {
        FastFoldGroupSqrtDecompositionLong sequence =
                FastFoldGroupSqrtDecompositionFactory.modSum(new long[] {-1, 9, 12}, 7);

        assertEquals(-1, sequence.get(0));
        assertEquals(6, sequence.fold(0, 3));
        sequence.set(1, -9);
        assertEquals(-9, sequence.get(1));
        assertEquals(2, sequence.prodAll());
    }

    @Test
    public void modularProductFactoryUsesThePrimeFieldMultiplicativeGroup() {
        long mod = 1_000_000_007L;
        FastFoldHeavyInverseGroupSqrtDecompositionLong sequence =
                FastFoldGroupSqrtDecompositionFactory.modProduct(new long[] {2, 3, 4, 5}, mod);

        assertEquals(24, sequence.fold(0, 3));
        assertEquals(60, sequence.fold(1, 4));
        sequence.set(1, mod - 1);
        assertEquals(mod - 40, sequence.prodAll());
        sequence.mul(2, 3);
        assertEquals(mod - 120, sequence.prodAll());
        assertThrows(ArithmeticException.class, () -> sequence.set(0, 0));
        assertEquals(mod - 120, sequence.prodAll());
    }

    private static long naiveFold(long[] values, int l, int r) {
        long result = PERMUTATION_GROUP.identity();
        for (int i = l; i < r; i++) result = PERMUTATION_GROUP.mul(result, values[i]);
        return result;
    }

    private static long naiveSum(long[] values, int l, int r) {
        long result = 0;
        for (int i = l; i < r; i++) result += values[i];
        return result;
    }
}
