package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ProductOfOnePlusAXBTest {
	private static final long MOD = 998244353L;
	private static final PolynomialFpDynamic POLY = PolynomialFpDynamic.MOD998244353;

	@Test
	void matchesNaiveProduct() {
		Random random = new Random(20260920L);
		for (int trial = 0; trial < 100; trial++) {
			int n = random.nextInt(15);
			int L = random.nextInt(20) + 1;
			long[] a = new long[n];
			int[] b = new int[n];
			for (int i = 0; i < n; i++) {
				a[i] = random.nextLong(-MOD, MOD + 1);
				b[i] = random.nextInt(L + 2);
			}

			long[] actual = POLY.productOfOnePlusAXB(a, b, L);
			long[] expected = naiveProduct(a, b, L);
			assertArrayEquals(expected, actual);

			long[] staticActual = PolynomialFp.productOfOnePlusAXB(a, b, L);
			assertArrayEquals(expected, staticActual);
		}
	}

	@Test
	void handlesCountsAndMultiplicities() {
		long[][] factors = {
				{2, 1, 3}, // (1 + 2x)^3
				{3, 2, 2}, // (1 + 3x^2)^2
				{0, 1, 5}, // (1 + 0x)^5 = 1
				{4, 0, 1}  // (1 + 4x^0) = 5
		};
		int L = 10;
		long[] actual = POLY.productOfOnePlusAXB(factors, L);

		long[] f1 = POLY.powFull(new long[] {1, 2}, 3);
		long[] f2 = POLY.powFull(new long[] {1, 0, 3}, 2);
		long[] expected = POLY.mul(f1, f2);
		expected = POLY.mul(expected, new long[] {5});
		if (expected.length > L) {
			expected = java.util.Arrays.copyOf(expected, L);
		} else if (expected.length < L) {
			expected = java.util.Arrays.copyOf(expected, L);
		}

		assertArrayEquals(expected, actual);
		assertArrayEquals(expected, PolynomialFp.productOfOnePlusAXB(factors, L));
	}

	@Test
	void handlesZeroConstantFactor() {
		// 1 + (-1)x^0 = 0 -> whole product becomes 0
		long[] a = { -1, 3 };
		int[] b = { 0, 2 };
		int L = 5;
		long[] actual = POLY.productOfOnePlusAXB(a, b, L);
		assertArrayEquals(new long[L], actual);
	}

	@Test
	void handlesEmptyAndZeroLength() {
		assertArrayEquals(new long[0], POLY.productOfOnePlusAXB(new long[] { 2 }, new int[] { 1 }, 0));
		assertArrayEquals(new long[] { 1, 0, 0 }, POLY.productOfOnePlusAXB(new long[0], new int[0], 3));
	}

	private long[] naiveProduct(long[] a, int[] b, int L) {
		if (L <= 0) return new long[0];
		long[] res = new long[] { 1 };
		for (int i = 0; i < a.length; i++) {
			long ai = Math.floorMod(a[i], MOD);
			int bi = b[i];
			long[] term;
			if (bi == 0) {
				term = new long[] { Math.floorMod(1 + ai, MOD) };
			} else {
				term = new long[bi + 1];
				term[0] = 1;
				term[bi] = ai;
			}
			res = POLY.mul(res, term);
			if (res.length > L) {
				res = java.util.Arrays.copyOf(res, L);
			}
		}
		if (res.length < L) {
			res = java.util.Arrays.copyOf(res, L);
		}
		return res;
	}
}
