package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ExpandPolynomialSumTest {
	private static final long MOD = 998244353L;
	private static final PolynomialFpDynamic POLY = PolynomialFpDynamic.MOD998244353;

	@Test
	void matchesNaiveExpansionDefaultDE() {
		Random random = new Random(20261024L);
		for (int trial = 0; trial < 50; trial++) {
			int n = random.nextInt(20) + 1;
			long[] a = new long[n];
			int[] b = new int[n];
			int[] c = new int[n];
			for (int i = 0; i < n; i++) {
				a[i] = random.nextLong(-MOD, MOD + 1);
				b[i] = random.nextInt(15);
				c[i] = random.nextInt(30);
			}

			long[] actual = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, 1, 1);
			long[] expected = naiveExpansion(a, b, c, 1, 1, MOD);
			assertArrayEquals(expected, actual);

			long[] staticActual = PolynomialFp.expandPolynomialSum(a, b, c, 1, 1);
			assertArrayEquals(expected, staticActual);
		}
	}

	@Test
	void matchesNaiveExpansionCustomDE() {
		Random random = new Random(20261025L);
		for (int trial = 0; trial < 50; trial++) {
			int n = random.nextInt(20) + 1;
			long[] a = new long[n];
			int[] b = new int[n];
			int[] c = new int[n];
			long valD = random.nextLong(-MOD, MOD + 1);
			long valE = random.nextLong(-MOD, MOD + 1);
			for (int i = 0; i < n; i++) {
				a[i] = random.nextLong(-MOD, MOD + 1);
				b[i] = random.nextInt(15);
				c[i] = random.nextInt(30);
			}

			long[] actual = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, valD, valE);
			long[] expected = naiveExpansion(a, b, c, valD, valE, MOD);
			assertArrayEquals(expected, actual);

			long[] staticActual = PolynomialFp.expandPolynomialSum(a, b, c, valD, valE);
			assertArrayEquals(expected, staticActual);
		}
	}

	@Test
	void matchesNaiveExpansionArraysDE() {
		Random random = new Random(20261026L);
		for (int trial = 0; trial < 50; trial++) {
			int n = random.nextInt(15) + 1;
			long[] a = new long[n];
			int[] b = new int[n];
			int[] c = new int[n];
			long d = random.nextLong(-MOD, MOD + 1);
			long e = random.nextLong(-MOD, MOD + 1);
			for (int i = 0; i < n; i++) {
				a[i] = random.nextLong(-MOD, MOD + 1);
				b[i] = random.nextInt(10);
				c[i] = random.nextInt(25);
			}

			long[] actual = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, d, e);
			long[] expected = naiveExpansion(a, b, c, d, e, MOD);
			assertArrayEquals(expected, actual);

			long[] staticActual = PolynomialFp.expandPolynomialSum(a, b, c, d, e);
			assertArrayEquals(expected, staticActual);
		}
	}

	@Test
	void handlesLargeExponentsNTTHorner() {
		Random random = new Random(20261027L);
		int n = 30;
		long[] a = new long[n];
		int[] b = new int[n];
		int[] c = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = random.nextLong(1, MOD);
			b[i] = random.nextInt(50);
			c[i] = random.nextInt(200) + 100;
		}

		long[] actual = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, 3, 5);
		long[] expected = naiveExpansion(a, b, c, 3, 5, MOD);
		assertArrayEquals(expected, actual);
	}

	@Test
	void handlesEmptyAndZeroInputs() {
		assertArrayEquals(new long[0], POLY.sumPowerProductsBySqrtDecomposition(new long[0], new int[0], new int[0], 1, 1));
		assertArrayEquals(new long[0], POLY.sumPowerProductsBySqrtDecomposition(new long[] { 0, 0 }, new int[] { 1, 2 }, new int[] { 3, 4 }, 1, 1));
	}

	@Test
	void handlesSpecialDandE() {
		// d = 0
		long[] a = { 3 };
		int[] b = { 2 };
		int[] c = { 4 };
		long[] actualD0 = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, 0, 2);
		long[] expectedD0 = naiveExpansion(a, b, c, 0, 2, MOD);
		assertArrayEquals(expectedD0, actualD0);

		// e = 0
		long[] actualE0 = POLY.sumPowerProductsBySqrtDecomposition(a, b, c, 5, 0);
		long[] expectedE0 = naiveExpansion(a, b, c, 5, 0, MOD);
		assertArrayEquals(expectedE0, actualE0);
	}

	@Test
	void throwsOnNonNTTMod() {
		PolynomialFpDynamic nonNttPoly = PolynomialFpDynamic.of(1000000007L);
		long[] a = { 2, 3 };
		int[] b = { 1, 0 };
		int[] c = { 3, 2 };
		assertThrows(IllegalArgumentException.class, () -> nonNttPoly.sumPowerProductsBySqrtDecomposition(a, b, c, 2, 3));
	}

	@Test
	void throwsOnInvalidInputs() {
		assertThrows(IllegalArgumentException.class, () -> POLY.sumPowerProductsBySqrtDecomposition(new long[2], new int[1], new int[2], 1, 1));
		assertThrows(IllegalArgumentException.class, () -> POLY.sumPowerProductsBySqrtDecomposition(new long[] { 1 }, new int[] { -1 }, new int[] { 1 }, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> POLY.sumPowerProductsBySqrtDecomposition(new long[] { 1 }, new int[] { 1 }, new int[] { -1 }, 1, 1));
	}

	private long[] naiveExpansion(long[] a, int[] b, int[] c, long d, long e, long mod) {
		PolynomialFpDynamic p = PolynomialFpDynamic.of(mod);
		long[] res = new long[0];
		for (int i = 0; i < a.length; i++) {
			long ai = Math.floorMod(a[i], mod);
			if (ai == 0) continue;
			long di = Math.floorMod(d, mod);
			long ei = Math.floorMod(e, mod);
			long[] binomial = new long[] { di, ei };
			long[] powBinomial = p.powFull(binomial, c[i]);
			long[] term = new long[b[i] + powBinomial.length];
			for (int j = 0; j < powBinomial.length; j++) {
				term[b[i] + j] = Math.floorMod(ai * powBinomial[j], mod);
			}
			res = p.add(res, term);
		}
		int finalDeg = p.deg(res);
		return finalDeg < 0 ? new long[0] : java.util.Arrays.copyOf(res, finalDeg + 1);
	}
}
