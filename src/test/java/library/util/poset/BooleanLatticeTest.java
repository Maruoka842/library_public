package library.util.poset;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class BooleanLatticeTest {
	@Test
	public void testMultiplicativeZeta() {
		long mod = 998244353;
		long[] a = {2, 3, 5, 7, 11, 13, 17, 19};
		long[] expected = new long[a.length];
		for (int s = 0; s < a.length; ++s) {
			expected[s] = 1;
			for (int t = s; ; t = (t - 1) & s) {
				expected[s] = expected[s] * a[t] % mod;
				if (t == 0) break;
			}
		}

		assertArrayEquals(expected, BooleanLattice.multiplicativeZeta(a, mod));
	}

	@Test
	public void testOrConvolutionProductOfBinomialPowers() {
		long mod = 998244353;
		int n = 4;
		int[] subsets = {0, 1, 6, 6, 9};
		long[] coefficients = {2, 3, 5, 7, 11};
		long[] exponents = {2, 3, 1, 2, 4};
		long[] expected = new long[1 << n];
		expected[0] = 1;
		for (int i = 0; i < subsets.length; ++i) {
			long[] factor = new long[1 << n];
			factor[0] = 1;
			factor[subsets[i]] += coefficients[i];
			factor[subsets[i]] %= mod;
			for (long e = 0; e < exponents[i]; ++e) {
				expected = orConvolutionNaive(expected, factor, mod);
			}
		}

		long[] actual = BooleanLattice.orConvolutionProductOfBinomialPowers(
				n, subsets, coefficients, exponents, mod);
		assertArrayEquals(expected, actual);
	}

	private static long[] orConvolutionNaive(long[] a, long[] b, long mod) {
		long[] c = new long[a.length];
		for (int s = 0; s < a.length; ++s) {
			for (int t = 0; t < b.length; ++t) {
				int u = s | t;
				c[u] = (c[u] + a[s] * b[t]) % mod;
			}
		}
		return c;
	}

	@Test
	public void testStrictZetaFromZeta() {
		long[] f = { 3, -2, 5, 7, -11, 13, 17, -19 };
		long[] expected = new long[f.length];
		for (int s = 0; s < f.length; ++s) {
			for (int t = 0; t < f.length; ++t) {
				if (t != s && (t & s) == t) expected[s] += f[t];
			}
		}

		long[] g = BooleanLattice.zeta(f);
		long[] original = g.clone();
		assertArrayEquals(expected, BooleanLattice.strictZetaFromZeta(g));
		assertArrayEquals(original, g);
	}

	@Test
	public void testSupsetStrictZetaFromZeta() {
		long[] f = { 3, -2, 5, 7, -11, 13, 17, -19 };
		long[] expected = new long[f.length];
		for (int s = 0; s < f.length; ++s) {
			for (int t = 0; t < f.length; ++t) {
				if (t != s && (t & s) == s) expected[s] += f[t];
			}
		}

		long[] g = BooleanLattice.supsetZeta(f);
		long[] original = g.clone();
		assertArrayEquals(expected, BooleanLattice.supsetStrictZetaFromZeta(g));
		assertArrayEquals(original, g);
	}

	@Test
	public void testStrictZetaFromZetaModulo() {
		long mod = 11;
		long[] f = { 3, 9, 5, 7, 0, 2, 6, 4 };
		long[] lower = BooleanLattice.zeta(f, mod);
		long[] upper = BooleanLattice.supsetZeta(f, mod);
		long[] expectedLower = new long[f.length];
		long[] expectedUpper = new long[f.length];
		for (int s = 0; s < f.length; ++s) {
			for (int t = 0; t < f.length; ++t) {
				if (t != s && (t & s) == t) expectedLower[s] = (expectedLower[s] + f[t]) % mod;
				if (t != s && (t & s) == s) expectedUpper[s] = (expectedUpper[s] + f[t]) % mod;
			}
		}

		assertArrayEquals(expectedLower, BooleanLattice.strictZetaFromZeta(lower, mod));
		assertArrayEquals(expectedUpper, BooleanLattice.supsetStrictZetaFromZeta(upper, mod));
	}

    @Test
    public void testExpConsistency() {
        long mod = 998244353;
        int n = 4;
        long[] a = new long[1 << n];
        Random rnd = new Random(42);
        for (int i = 1; i < (1 << n); i++) {
            a[i] = rnd.nextInt((int) mod);
        }

        long[] resNew = BooleanLattice.exp(a, mod);
        long[] resNaive = BooleanLattice.expNaive(a, mod);

        assertArrayEquals(resNaive, resNew, "New exp should match naive exp");
    }

    @Test
    public void testExpSmall() {
        long mod = 998244353;
        int n = 2;
        long[] a = new long[1 << n];
        Random rnd = new Random(43);
        for (int i = 1; i < (1 << n); i++) {
            a[i] = rnd.nextInt((int) mod);
        }

        long[] resNew = BooleanLattice.exp(a, mod);
        long[] resNaive = BooleanLattice.expNaive(a, mod);

        assertArrayEquals(resNaive, resNew);
    }

    private static long[] supsetRankedMoebiusNaive(long[] a, long mod) {
        long[] b = new long[a.length];
        for (int i = a.length - 1; i >= 0; --i) {
            for (int j = b.length - 1; j >= 0; --j) {
                b[j] = -b[j] + (j > 0 ? b[j - 1] : 0);
                b[j] %= mod;
            }
            b[0] += a[i];
            b[0] %= mod;
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = (b[i] % mod + mod) % mod;
        }
        return b;
    }

    private static long[] rankedMoebiusNaive(long[] a, long mod) {
        long[] revA = a.clone();
        library.util.ArrayUtils.reverse(revA);
        revA = supsetRankedMoebiusNaive(revA, mod);
        library.util.ArrayUtils.reverse(revA);
        return revA;
    }

    @Test
    public void testSupsetRankedMoebius() {
        long[] moduli = {998244353L, 1000000007L, 3L, 101L};
        int[] sizes = {1, 2, 5, 16, 37, 64, 100};
        Random rnd = new Random(1337);

        for (long mod : moduli) {
            for (int size : sizes) {
                long[] a = new long[size];
                for (int i = 0; i < size; i++) {
                    a[i] = rnd.nextInt((int) mod);
                }

                long[] expected = supsetRankedMoebiusNaive(a, mod);
                long[] actual = BooleanLattice.supsetRankedMoebius(a, mod);

                assertArrayEquals(expected, actual, "Failed on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testRankedMoebius() {
        long[] moduli = {998244353L, 1000000007L, 5L, 103L};
        int[] sizes = {1, 2, 5, 16, 37, 64, 100};
        Random rnd = new Random(1338);

        for (long mod : moduli) {
            for (int size : sizes) {
                long[] a = new long[size];
                for (int i = 0; i < size; i++) {
                    a[i] = rnd.nextInt((int) mod);
                }

                long[] expected = rankedMoebiusNaive(a, mod);
                long[] actual = BooleanLattice.rankedMoebius(a, mod);

                assertArrayEquals(expected, actual, "Failed on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testSupsetOrZetaPackedCorrectness() {
        // Let's test B = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Random rnd = new Random(4242);
        for (int B = 0; B <= 10; B++) {
            int numWords = B < 6 ? 1 : 1 << (B - 6);
            long[] a = new long[numWords];
            for (int i = 0; i < numWords; i++) {
                a[i] = rnd.nextLong();
                if (B < 6) {
                    // clear out-of-bounds bits so it is a valid input
                    int numBits = 1 << B;
                    if (numBits < 64) {
                        a[i] &= (1L << numBits) - 1;
                    }
                }
            }

            // Naive superset OR zeta transform
            boolean[] truthValues = new boolean[1 << B];
            for (int i = 0; i < (1 << B); i++) {
                int U = i >>> 6;
                int L = i & 63;
                truthValues[i] = ((a[U] >>> L) & 1L) == 1L;
            }

            boolean[] expectedZeta = new boolean[1 << B];
            // g(S) = OR_{S \subseteq T} f(T)
            // We can do this naively:
            for (int S = 0; S < (1 << B); S++) {
                boolean combined = false;
                // In B-dimensional boolean lattice, S \subseteq T means (S & T) == S.
                // We iterate over all supersets of S. We can do that by submask of (~S) with a shift, or just iterate.
                // Since B <= 10, iterating over all T from 0 to 2^B-1 is very fast.
                for (int T = 0; T < (1 << B); T++) {
                    if ((S & T) == S) {
                        combined |= truthValues[T];
                    }
                }
                expectedZeta[S] = combined;
            }

            // Convert expectedZeta back to packed form
            long[] expectedPacked = new long[numWords];
            for (int i = 0; i < (1 << B); i++) {
                if (expectedZeta[i]) {
                    int U = i >>> 6;
                    int L = i & 63;
                    expectedPacked[U] |= (1L << L);
                }
            }

            long[] actualPacked = BooleanLattice.supsetOrZetaPacked(a, B);
            assertArrayEquals(expectedPacked, actualPacked, "Failed on B = " + B);
        }
    }

    @Test
    public void testSubsetOrZetaPackedCorrectness() {
        // Let's test B = 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        Random rnd = new Random(4243);
        for (int B = 0; B <= 10; B++) {
            int numWords = B < 6 ? 1 : 1 << (B - 6);
            long[] a = new long[numWords];
            for (int i = 0; i < numWords; i++) {
                a[i] = rnd.nextLong();
                if (B < 6) {
                    // clear out-of-bounds bits so it is a valid input
                    int numBits = 1 << B;
                    if (numBits < 64) {
                        a[i] &= (1L << numBits) - 1;
                    }
                }
            }

            // Naive subset OR zeta transform
            boolean[] truthValues = new boolean[1 << B];
            for (int i = 0; i < (1 << B); i++) {
                int U = i >>> 6;
                int L = i & 63;
                truthValues[i] = ((a[U] >>> L) & 1L) == 1L;
            }

            boolean[] expectedZeta = new boolean[1 << B];
            // g(S) = OR_{T \subseteq S} f(T)
            // We can do this naively:
            for (int S = 0; S < (1 << B); S++) {
                boolean combined = false;
                // In B-dimensional boolean lattice, T \subseteq S means (T & S) == T.
                for (int T = 0; T < (1 << B); T++) {
                    if ((T & S) == T) {
                        combined |= truthValues[T];
                    }
                }
                expectedZeta[S] = combined;
            }

            // Convert expectedZeta back to packed form
            long[] expectedPacked = new long[numWords];
            for (int i = 0; i < (1 << B); i++) {
                if (expectedZeta[i]) {
                    int U = i >>> 6;
                    int L = i & 63;
                    expectedPacked[U] |= (1L << L);
                }
            }

            long[] actualPacked = BooleanLattice.subsetOrZetaPacked(a, B);
            assertArrayEquals(expectedPacked, actualPacked, "Failed on B = " + B);
        }
    }

    @Test
    public void testSupsetOrZetaPackedExceptions() {
        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.supsetOrZetaPacked(new long[1], -1);
        }, "Should throw if B < 0");

        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.supsetOrZetaPacked(new long[2], 5);
        }, "Should throw if array size doesn't match 1 (B < 6)");

        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.supsetOrZetaPacked(new long[2], 6);
        }, "Should throw if array size doesn't match 2^(B-6) (B >= 6)");

        // Out-of-bounds non-zero bits for B < 6
        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.supsetOrZetaPacked(new long[]{ 0b10000L }, 2); // 4 elements valid (indices 0..3), bit index 4 is 1
        }, "Should throw if out-of-bounds bits are non-zero");
    }

    @Test
    public void testSubsetOrZetaPackedExceptions() {
        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.subsetOrZetaPacked(new long[1], -1);
        }, "Should throw if B < 0");

        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.subsetOrZetaPacked(new long[2], 5);
        }, "Should throw if array size doesn't match 1 (B < 6)");

        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.subsetOrZetaPacked(new long[2], 6);
        }, "Should throw if array size doesn't match 2^(B-6) (B >= 6)");

        // Out-of-bounds non-zero bits for B < 6
        assertThrows(IllegalArgumentException.class, () -> {
            BooleanLattice.subsetOrZetaPacked(new long[]{ 0b10000L }, 2); // 4 elements valid (indices 0..3), bit index 4 is 1
        }, "Should throw if out-of-bounds bits are non-zero");
    }
}
