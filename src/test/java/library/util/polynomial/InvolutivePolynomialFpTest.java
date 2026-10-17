package library.util.polynomial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

public class InvolutivePolynomialFpTest {

	@Test
	public void testFftAndIfftAndMulWithMultipleModuli() {
		long[] moduli = {998244353L, 1000000007L, 998244353L * 2 + 1};
		Random rng = new Random(42);

		for (long mod : moduli) {
			InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

			// Test both N <= MUL_NAIVE_THRESHOLD and N > MUL_NAIVE_THRESHOLD
			int[] sizes = {4, 8, 16, 32, 64};
			for (int n : sizes) {
				long[] a = new long[n];
				long[] b = new long[n];
				for (int i = 0; i < n; i++) {
					a[i] = rng.nextInt((int) Math.min(mod, 100000));
					b[i] = rng.nextInt((int) Math.min(mod, 100000));
				}

				// Verify FFT/IFFT roundtrip restores original scaled elements (fft followed by ifft and dividing by N)
				long[] aFFT = a.clone();
				poly.fft(aFFT);
				long[] aIFFT = aFFT.clone();
				poly.ifft(aIFFT);

				library.util.Fp mo = new library.util.Fp(mod);
				long invN = mo.inv(n);
				for (int i = 0; i < n; i++) {
					long restored = aIFFT[i] * invN % mod;
					assertEquals(a[i] % mod, restored);
				}

				// Verify mul vs mulNaive vs mulFFT directly
				long[] mulCombined = poly.mul(a, b);
				long[] mulNaive = poly.mulNaive(a, b);
				long[] mulFFT = poly.mulFFT(a, b);
				assertArrayEquals(mulNaive, mulCombined, "mul output should produce correct results under mod " + mod + " for N=" + n);
				assertArrayEquals(mulFFT, mulCombined, "mul output should match mulFFT under mod " + mod + " for N=" + n);
			}
		}
	}

	@Test
	public void testEvenModulusFallback() {
		// Even modulus where 2 does not have modular inverse
		long mod = 1000000008L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		// N = 32 (which is > MUL_NAIVE_THRESHOLD, but mod % 2 == 0 so it must use mulNaive instead of failing in mulFFT)
		int n = 32;
		long[] a = new long[n];
		long[] b = new long[n];
		Random rng = new Random(42);
		for (int i = 0; i < n; i++) {
			a[i] = rng.nextInt((int) (mod - 1));
			b[i] = rng.nextInt((int) (mod - 1));
		}

		long[] result = poly.mul(a, b);
		long[] expected = poly.mulNaive(a, b);
		assertArrayEquals(expected, result, "Multiplication under even modulus should fallback to naive and succeed");
	}

	@Test
	public void testAddAndInverse() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		long[] a = {1, 2, 3, 4};
		long[] b = {5, 6, 7, 8};
		long[] sum = poly.add(a, b);
		long[] expectedSum = {6, 8, 10, 12};
		assertArrayEquals(expectedSum, sum);

		// test inverse: x * inverse(x) should be [1, 0, 0, 0] (unit element)
		long[] x = {10, 3, 5, 2};
		long[] invX = poly.inverse(x);
		long[] prod = poly.mul(x, invX);
		long[] expectedProd = {1, 0, 0, 0};
		assertArrayEquals(expectedProd, prod);
	}

	@Test
	public void testGeometricSeries() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		// geometricSeries computes a^0 + a^1 + a^2
		long[] a = {2, 0, 0, 0}; // just a constant term
		long[] res = poly.geometricSeries(a, 3);
		// expected: 2^0 + 2^1 + 2^2 = 1 + 2 + 4 = 7
		long[] expected = {7, 0, 0, 0};
		assertArrayEquals(expected, res);
	}

	@Test
	public void testMulUnequalAndNonPowerOfTwoAndEmpty() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		// 1. Unequal lengths
		long[] a1 = {1, 2, 3};
		long[] b1 = {4, 5};
		// Under the hood, length should be treated up to power-of-two = 4
		// a1 is treated as {1, 2, 3, 0}
		// b1 is treated as {4, 5, 0, 0}
		// Products:
		// i=0:
		//   j=0: (i^j=0): a1[0]*b1[0] = 4
		//   j=1: (i^j=1): a1[0]*b1[1] = 5
		// i=1:
		//   j=0: (i^j=1): a1[1]*b1[0] = 8
		//   j=1: (i^j=0): a1[1]*b1[1] = 10
		// i=2:
		//   j=0: (i^j=2): a1[2]*b1[0] = 12
		//   j=1: (i^j=3): a1[2]*b1[1] = 15
		// Expected sums:
		// c[0] = 4 + 10 = 14
		// c[1] = 5 + 8 = 13
		// c[2] = 12
		// c[3] = 15
		long[] expected1 = {14, 13, 12, 15};

		long[] resNaive1 = poly.mulNaive(a1, b1);
		long[] resFFT1 = poly.mulFFT(a1, b1);
		long[] resMul1 = poly.mul(a1, b1);

		assertArrayEquals(expected1, resNaive1);
		assertArrayEquals(expected1, resFFT1);
		assertArrayEquals(expected1, resMul1);

		// 2. Non-power-of-two, identical lengths
		long[] a2 = {1, 2, 3};
		long[] b2 = {4, 5, 6};
		// Treated as {1, 2, 3, 0} and {4, 5, 6, 0}
		// Products:
		// i=0:
		//   j=0 (0): 4
		//   j=1 (1): 5
		//   j=2 (2): 6
		// i=1:
		//   j=0 (1): 8
		//   j=1 (0): 10
		//   j=2 (3): 12
		// i=2:
		//   j=0 (2): 12
		//   j=1 (3): 15
		//   j=2 (0): 18
		// Expected sums:
		// c[0] = 4 + 10 + 18 = 32
		// c[1] = 5 + 8 = 13
		// c[2] = 6 + 12 = 18
		// c[3] = 12 + 15 = 27
		long[] expected2 = {32, 13, 18, 27};

		long[] resNaive2 = poly.mulNaive(a2, b2);
		long[] resFFT2 = poly.mulFFT(a2, b2);
		long[] resMul2 = poly.mul(a2, b2);

		assertArrayEquals(expected2, resNaive2);
		assertArrayEquals(expected2, resFFT2);
		assertArrayEquals(expected2, resMul2);

		// 3. One or both empty
		long[] empty = {};
		// Max length of empty and a1 is 3 -> p = 4. Since one operand is empty, all coefficients should be 0.
		long[] resEmpty1 = poly.mul(empty, a1);
		long[] resEmpty2 = poly.mul(b1, empty); // Max length of b1 and empty is 2 -> p = 2.
		long[] resEmpty3 = poly.mul(empty, empty);

		assertEquals(4, resEmpty1.length);
		assertArrayEquals(new long[]{0, 0, 0, 0}, resEmpty1);

		assertEquals(2, resEmpty2.length);
		assertArrayEquals(new long[]{0, 0}, resEmpty2);

		assertEquals(0, resEmpty3.length);

		long[] resEmptyNaive1 = poly.mulNaive(empty, a1);
		assertEquals(4, resEmptyNaive1.length);
		assertArrayEquals(new long[]{0, 0, 0, 0}, resEmptyNaive1);

		long[] resEmptyFFT1 = poly.mulFFT(empty, a1);
		assertEquals(4, resEmptyFFT1.length);
		assertArrayEquals(new long[]{0, 0, 0, 0}, resEmptyFFT1);
	}

	@Test
	public void testSubsetXorSum() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		int[] a = {1, 2};
		// subsets: {}, {1}, {2}, {1, 2}
		// xor sums: 0, 1, 2, 3
		// counts of xor sums:
		// sum 0: 1 (from {})
		// sum 1: 1 (from {1})
		// sum 2: 1 (from {2})
		// sum 3: 1 (from {1, 2})
		long[] res = poly.subsetXorSum(a);
		assertEquals(1, res[0]);
		assertEquals(1, res[1]);
		assertEquals(1, res[2]);
		assertEquals(1, res[3]);
		// Other slots should be 0
		for (int i = 4; i < res.length; i++) {
			assertEquals(0, res[i]);
		}
	}

	private long[] powerProjectionNaive(InvolutivePolynomialFp poly, long[] f, int a, int M) {
		long[] g = new long[a + 1];
		g[a] = 1;
		return powerProjectionNaive(poly, f, g, M);
	}

	private long[] powerProjectionNaive(InvolutivePolynomialFp poly, long[] f, long[] g, int M) {
		int N = 1;
		while (N < f.length || N < g.length) {
			N <<= 1;
		}
		long[] A = new long[N];
		System.arraycopy(f, 0, A, 0, f.length);

		long[] B = new long[N];
		System.arraycopy(g, 0, B, 0, g.length);

		long[] res = new long[M + 1];
		long[] cur = new long[N];
		cur[0] = 1; // f^0 = [1, 0, 0, ...]

		long sum0 = 0;
		for (int a = 0; a < N; a++) {
			sum0 = (sum0 + B[a] * cur[a]) % poly.mod;
		}
		res[0] = (sum0 + poly.mod) % poly.mod;

		for (int i = 1; i <= M; i++) {
			cur = poly.mul(cur, A);
			long sum = 0;
			for (int a = 0; a < N; a++) {
				sum = (sum + B[a] * cur[a]) % poly.mod;
			}
			res[i] = (sum + poly.mod) % poly.mod;
		}
		return res;
	}

	@Test
	public void testPowerProjectionSmallHandCalculated() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		// Case 1: f = {1, 1}, a = 1, M = 3
		// i=0: f^0 = [1, 0] -> coeff at x^1 is 0
		// i=1: f^1 = [1, 1] -> coeff at x^1 is 1
		// i=2: f^2 = [2, 2] -> coeff at x^1 is 2
		// i=3: f^3 = [4, 4] -> coeff at x^1 is 4
		// Expected: [0, 1, 2, 4]
		long[] f1 = {1, 1};
		long[] res1 = poly.powerProjection(f1, 1, 3);
		assertArrayEquals(new long[]{0, 1, 2, 4}, res1);

		// Case 2: f = {2}, a = 0, M = 3
		// f^0 = 1 -> 1
		// f^1 = 2 -> 2
		// f^2 = 4 -> 4
		// f^3 = 8 -> 8
		// Expected: [1, 2, 4, 8]
		long[] f2 = {2};
		long[] res2 = poly.powerProjection(f2, 0, 3);
		assertArrayEquals(new long[]{1, 2, 4, 8}, res2);

		// Case 3: f = {1, 1}, g = {2, 3}, M = 3
		// i=0: f^0 = [1, 0] -> \sum_a g_a [x^a] f^0 = g_0 * 1 = 2
		// i=1: f^1 = [1, 1] -> \sum_a g_a [x^a] f^1 = g_0 * 1 + g_1 * 1 = 5
		// i=2: f^2 = [2, 2] -> \sum_a g_a [x^a] f^2 = g_0 * 2 + g_1 * 2 = 10
		// i=3: f^3 = [4, 4] -> \sum_a g_a [x^a] f^3 = g_0 * 4 + g_1 * 4 = 20
		// Expected: [2, 5, 10, 20]
		long[] g3 = {2, 3};
		long[] res3 = poly.powerProjection(f1, g3, 3);
		assertArrayEquals(new long[]{2, 5, 10, 20}, res3);
	}

	@Test
	public void testPowerProjectionEdgeAndValidationCases() {
		long mod = 998244353L;
		InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

		// M < 0 should return empty array
		assertArrayEquals(new long[0], poly.powerProjection(new long[]{1, 2}, 0, -1));
		assertArrayEquals(new long[0], poly.powerProjection(new long[]{1, 2}, new long[]{1}, -1));

		// Null f or g should throw NullPointerException
		assertThrows(NullPointerException.class, () -> {
			poly.powerProjection(null, 0, 3);
		});
		assertThrows(NullPointerException.class, () -> {
			poly.powerProjection(new long[]{1}, null, 3);
		});
		assertThrows(NullPointerException.class, () -> {
			poly.powerProjection(null, new long[]{1}, 3);
		});

		// Negative a should throw IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> {
			poly.powerProjection(new long[]{1}, -1, 3);
		});

		// mod == 2 should throw IllegalArgumentException
		InvolutivePolynomialFp polyMod2 = new InvolutivePolynomialFp(2L);
		assertThrows(IllegalArgumentException.class, () -> {
			polyMod2.powerProjection(new long[]{1}, 0, 3);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			polyMod2.powerProjection(new long[]{1}, new long[]{1}, 3);
		});

		// Empty f should be treated as [0]
		// [x^0] f^0 = 1, [x^0] f^i = 0 for i > 0
		long[] resEmpty = poly.powerProjection(new long[0], 0, 3);
		assertArrayEquals(new long[]{1, 0, 0, 0}, resEmpty);
	}

	@Test
	public void testPowerProjectionRandomStress() {
		long[] moduli = {998244353L, 1000000007L, 998244353L * 2 + 1}; // NTT-friendly, non-NTT-friendly, custom primes fitting in long multiplication
		Random rng = new Random(12345);

		for (long mod : moduli) {
			InvolutivePolynomialFp poly = new InvolutivePolynomialFp(mod);

			for (int step = 0; step < 50; step++) {
				int n = 1 << rng.nextInt(5); // N = 1, 2, 4, 8, 16
				int M = rng.nextInt(10); // M in [0, 9]

				long[] f = new long[n];
				for (int i = 0; i < n; i++) {
					f[i] = rng.nextLong() & Long.MAX_VALUE;
					if (f[i] >= mod) f[i] %= mod;
				}

				// g can have varying length up to n
				int gLen = rng.nextInt(n) + 1;
				long[] g = new long[gLen];
				for (int i = 0; i < gLen; i++) {
					g[i] = rng.nextLong() & Long.MAX_VALUE;
					if (g[i] >= mod) g[i] %= mod;
				}

				long[] expected = powerProjectionNaive(poly, f, g, M);
				long[] actual = poly.powerProjection(f, g, M);

				assertArrayEquals(expected, actual, "Mismatch under mod " + mod + " step " + step + " N=" + n + " M=" + M + " gLen=" + gLen);
			}
		}
	}
}
