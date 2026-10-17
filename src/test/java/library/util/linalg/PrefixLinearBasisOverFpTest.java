package library.util.linalg;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import library.util.MathUtils;

/**
 * PrefixLinearBasisOverFp の動作確認を行うユニットテスト。
 */
public class PrefixLinearBasisOverFpTest {

    @Test
    public void testBasic() {
        long mod = 998244353;
        // ベクトル配列:
        // a[0] = {1, 0, 0, 0}
        // a[1] = {0, 2, 0, 0}
        // a[2] = {0, 0, 3, 0}
        // a[3] = {0, 0, 0, 4}
        long[][] a = {
            {1, 0, 0, 0},
            {0, 2, 0, 0},
            {0, 0, 3, 0},
            {0, 0, 0, 4}
        };
        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        assertEquals(4, plb.size());

        // [0, 4) の基底
        long[][] basis = plb.basis(0, 4);
        assertEquals(4, basis.length);
        // 各基底はピボット位置で規格化されているはずなので、{1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1} となる
        assertArrayEquals(new long[]{1, 0, 0, 0}, basis[0]);
        assertArrayEquals(new long[]{0, 1, 0, 0}, basis[1]);
        assertArrayEquals(new long[]{0, 0, 1, 0}, basis[2]);
        assertArrayEquals(new long[]{0, 0, 0, 1}, basis[3]);

        // rank checks
        assertEquals(4, plb.rank(0, 4));
        assertEquals(2, plb.rank(1, 3)); // {0,2,0,0} and {0,0,3,0} -> rank 2

        // contains checks
        assertTrue(plb.contains(0, 4, new long[]{5, 6, 7, 8}));
        assertTrue(plb.contains(1, 3, new long[]{0, 42, 17, 0}));
        assertFalse(plb.contains(1, 3, new long[]{1, 0, 0, 0})); // index 0 is excluded
    }

    @Test
    public void testLinearDependency() {
        long mod = 7;
        // {1, 2, 3}, {2, 4, 6} (dependent), {0, 1, 5}
        long[][] a = {
            {1, 2, 3},
            {2, 4, 6},
            {0, 1, 5}
        };
        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        assertEquals(3, plb.size());
        assertEquals(2, plb.rank(0, 3)); // {1, 2, 3} and {0, 1, 5} are independent

        assertTrue(plb.contains(0, 3, new long[]{3, 6, 9})); // {3, 6, 9} = 3 * {1, 2, 3}
        assertTrue(plb.contains(0, 2, new long[]{2, 4, 6}));
        assertFalse(plb.contains(0, 2, new long[]{0, 1, 5})); // {0, 1, 5} is at index 2 (excluded in [0, 2))
    }

    @Test
    public void testSingleElement() {
        long mod = 11;
        long[][] a = {
            {3, 5}
        };
        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        assertEquals(1, plb.size());
        assertEquals(0, plb.basis(0, 0).length);
        assertEquals(0, plb.rank(0, 0));
        assertTrue(plb.contains(0, 0, new long[]{0, 0}));
        assertFalse(plb.contains(0, 0, new long[]{3, 5}));

        assertEquals(1, plb.rank(0, 1));
        assertTrue(plb.contains(0, 1, new long[]{3, 5}));
        assertTrue(plb.contains(0, 1, new long[]{6, 10})); // 2 * {3, 5} = {6, 10}
    }

    @Test
    public void testEmptyArrayForbidden() {
        long mod = 11;
        long[][] a = {};
        assertThrows(IllegalArgumentException.class, () -> {
            new PrefixLinearBasisOverFp(a, mod);
        });
    }

    @Test
    public void testDimensionMismatch() {
        long mod = 11;
        long[][] a = {
            {1, 2},
            {3, 4, 5}
        };
        assertThrows(IllegalArgumentException.class, () -> {
            new PrefixLinearBasisOverFp(a, mod);
        });
    }

    @Test
    public void testNullArrayAndElements() {
        long mod = 11;
        assertThrows(IllegalArgumentException.class, () -> {
            new PrefixLinearBasisOverFp(null, mod);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            long[][] a = { null };
            new PrefixLinearBasisOverFp(a, mod);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            long[][] a = { {1, 2}, null };
            new PrefixLinearBasisOverFp(a, mod);
        });
    }

    @Test
    public void testReduce() {
        long mod = 5;
        // a = { {1, 2, 0}, {0, 1, 3} }
        long[][] a = {
            {1, 2, 0},
            {0, 1, 3}
        };
        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        // x = {2, 0, 1}
        // reduce by {1, 2, 0}: x' = {2, 0, 1} - 2*{1, 2, 0} = {0, 1, 1} (mod 5)
        // reduce by {0, 1, 3}: x'' = {0, 1, 1} - 1*{0, 1, 3} = {0, 0, 3} (mod 5)
        long[] reduced = plb.reduce(0, 2, new long[]{2, 0, 1});
        assertArrayEquals(new long[]{0, 0, 3}, reduced);
    }

    @Test
    public void testMaxVector() {
        long mod = 5;
        // a = { {1, 2, 0}, {0, 1, 3} }
        long[][] a = {
            {1, 2, 0},
            {0, 1, 3}
        };
        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        // Max vector in span of { {1, 2, 0}, {0, 1, 3} }:
        // For pivot 0 (which has {1, 2, 0}): want index 0 to be 4 (mod-1). Coeff of first vector is 4.
        // Result starts: 4 * {1, 2, 0} = {4, 3, 0}.
        // For pivot 1 (which has {0, 1, 3}): currently index 1 is 3. Want it to be 4.
        // So we add 1 * {0, 1, 3}.
        // Result becomes: {4, 3, 0} + 1 * {0, 1, 3} = {4, 4, 3}.
        // Thus, lexicographically largest vector should be {4, 4, 3}.
        long[] maxVec = plb.maxVector(0, 2);
        assertArrayEquals(new long[]{4, 4, 3}, maxVec);
    }

    @Test
    public void testStressAgainstNaive() {
        Random rand = new Random(42);
        int N = 50;
        int d = 6;
        long mod = 998244353;
        long[][] a = new long[N][d];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < d; j++) {
                a[i][j] = rand.nextInt((int) mod);
            }
        }

        PrefixLinearBasisOverFp plb = new PrefixLinearBasisOverFp(a, mod);

        for (int step = 0; step < 200; step++) {
            int l = rand.nextInt(N);
            int r = rand.nextInt(N - l) + l + 1; // [l, r)

            long[][] basis = plb.basis(l, r);
            verifyBasisCorrectness(a, l, r, basis, d, mod);

            // test contains and reduce
            for (int k = 0; k < 10; k++) {
                long[] x = new long[d];
                for (int j = 0; j < d; j++) {
                    x[j] = rand.nextInt((int) mod);
                }
                boolean containsPlb = plb.contains(l, r, x);
                boolean containsNaive = containsNaive(basis, x, d, mod);
                assertEquals(containsNaive, containsPlb);

                long[] reducedPlb = plb.reduce(l, r, x);
                long[] reducedNaive = reduceNaive(basis, x, d, mod);
                assertArrayEquals(reducedNaive, reducedPlb);
            }

            // test maxVector
            long[] maxPlb = plb.maxVector(l, r);
            long[] maxNaive = maxVectorNaive(basis, d, mod);
            assertArrayEquals(maxNaive, maxPlb);
        }
    }

    private void verifyBasisCorrectness(long[][] a, int l, int r, long[][] basis, int d, long mod) {
        // 1. All elements in basis are linearly independent and normalized
        for (int i = 0; i < basis.length; i++) {
            long[] val = basis[i];
            // Find pivot
            int pivot = -1;
            for (int j = 0; j < d; j++) {
                if (val[j] != 0) {
                    pivot = j;
                    break;
                }
            }
            assertNotEquals(-1, pivot);
            assertEquals(1L, val[pivot]); // normalized pivot should be 1

            // Check that it's linearly independent
            long[] x = val.clone();
            for (int j = 0; j < basis.length; j++) {
                if (i != j) {
                    int otherPivot = -1;
                    for (int k = 0; k < d; k++) {
                        if (basis[j][k] != 0) {
                            otherPivot = k;
                            break;
                        }
                    }
                    if (otherPivot != -1 && x[otherPivot] != 0) {
                        long factor = x[otherPivot];
                        for (int k = otherPivot; k < d; k++) {
                            x[k] = (x[k] - factor * basis[j][k]) % mod;
                            if (x[k] < 0) x[k] += mod;
                        }
                    }
                }
            }
            boolean isZero = true;
            for (int k = 0; k < d; k++) {
                if (x[k] != 0) {
                    isZero = false;
                    break;
                }
            }
            assertFalse(isZero);
        }

        // 2. All elements in a[l..r) can be represented by basis
        for (int i = l; i < r; i++) {
            assertTrue(containsNaive(basis, a[i], d, mod), "basis should contain " + i);
        }

        // 3. Size of basis equals rank of subsegment
        long[][] naiveBasis = getNaiveBasis(a, l, r, d, mod);
        assertEquals(naiveBasis.length, basis.length);
    }

    private long[][] getNaiveBasis(long[][] a, int l, int r, int d, long mod) {
        long[][] b = new long[d][];
        int count = 0;
        for (int i = l; i < r; i++) {
            long[] val = a[i].clone();
            for (int bPos = 0; bPos < d; bPos++) {
                long valB = val[bPos];
                if (valB != 0) {
                    if (b[bPos] == null) {
                        long inv = MathUtils.modInv(valB, mod);
                        long[] normalized = new long[d];
                        for (int k = bPos; k < d; k++) {
                            normalized[k] = val[k] * inv % mod;
                        }
                        b[bPos] = normalized;
                        count++;
                        break;
                    }
                    long factor = valB;
                    long[] bVec = b[bPos];
                    for (int k = bPos; k < d; k++) {
                        val[k] = (val[k] - factor * bVec[k]) % mod;
                        if (val[k] < 0) val[k] += mod;
                    }
                }
            }
        }
        long[][] res = new long[count][d];
        int idx = 0;
        for (int bPos = 0; bPos < d; bPos++) {
            if (b[bPos] != null) {
                res[idx++] = b[bPos];
            }
        }
        return res;
    }

    private boolean containsNaive(long[][] basis, long[] x, int d, long mod) {
        long[] red = reduceNaive(basis, x, d, mod);
        for (int k = 0; k < d; k++) {
            if (red[k] != 0) return false;
        }
        return true;
    }

    private long[] reduceNaive(long[][] basis, long[] x, int d, long mod) {
        long[] res = new long[d];
        for (int k = 0; k < d; k++) {
            res[k] = (x[k] % mod + mod) % mod;
        }
        for (int b = 0; b < d; b++) {
            long valB = res[b];
            if (valB != 0) {
                // Find basis vector with pivot b
                long[] bVec = null;
                for (long[] vec : basis) {
                    int p = -1;
                    for (int k = 0; k < d; k++) {
                        if (vec[k] != 0) {
                            p = k;
                            break;
                        }
                    }
                    if (p == b) {
                        bVec = vec;
                        break;
                    }
                }
                if (bVec != null) {
                    long factor = valB;
                    for (int k = b; k < d; k++) {
                        res[k] = (res[k] - factor * bVec[k]) % mod;
                        if (res[k] < 0) res[k] += mod;
                    }
                }
            }
        }
        return res;
    }

    private long[] maxVectorNaive(long[][] basis, int d, long mod) {
        long[] res = new long[d];
        for (int b = 0; b < d; b++) {
            // Find basis vector with pivot b
            long[] bVec = null;
            for (long[] vec : basis) {
                int p = -1;
                for (int k = 0; k < d; k++) {
                    if (vec[k] != 0) {
                        p = k;
                        break;
                    }
                }
                if (p == b) {
                    bVec = vec;
                    break;
                }
            }
            if (bVec != null) {
                long current = res[b];
                long target = mod - 1;
                long coeff = (target - current + mod) % mod;
                if (coeff != 0) {
                    for (int k = b; k < d; k++) {
                        res[k] = (res[k] + coeff * bVec[k]) % mod;
                    }
                }
            }
        }
        return res;
    }
}
