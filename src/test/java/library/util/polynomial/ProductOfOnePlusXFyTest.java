package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class ProductOfOnePlusXFyTest {
	private static final long MOD = 998244353L;
	private static final PolynomialFpDynamic2D POLY = PolynomialFpDynamic2D.MOD998244353;

	@Test
	void matchesNaiveProduct() {
		Random random = new Random(20260919L);
		for (int trial = 0; trial < 100; trial++) {
			int size = random.nextInt(12);
			int nx = random.nextInt(9) + 1;
			int ny = random.nextInt(9) + 1;
			List<long[]> factors = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				long[] factor = new long[random.nextInt(6)];
				for (int j = 0; j < factor.length; j++) factor[j] = random.nextLong(-MOD, MOD + 1);
				factors.add(factor);
			}
			long[][] actual = POLY.productOfOnePlusXFy(factors, nx, ny);
			assertTrue(POLY.equals(naive(factors, nx, ny), actual));
			assertRectangular(actual, nx, ny);
		}
	}

	@Test
	void reconstructsFromPolynomialPowerSums() {
		List<long[]> factors = List.of(
				new long[] {1, 2, 3},
				new long[] {4, 0, 5},
				new long[] {7, 1});
		int nx = 6;
		int ny = 8;
		long[][] powerSums = powerSums(factors, nx, ny);
		long[][] expected = naive(factors, nx, ny);
		long[][] actual = POLY.productOfOnePlusXFyFromPowerSums(powerSums, ny);
		assertTrue(POLY.equals(expected, actual));
		assertRectangular(actual, nx, ny);
	}

	@Test
	void handlesEmptyProductsAndTruncation() {
		assertTrue(POLY.equals(POLY.one(), POLY.productOfOnePlusXFy(List.of(), 5, 5)));
		assertTrue(POLY.equals(POLY.one(), POLY.productOfOnePlusXFy(List.of(new long[] {2, 3}), 1, 5)));
		long[][] zeroX = POLY.productOfOnePlusXFy(List.of(new long[] {2, 3}), 0, 5);
		long[][] zeroY = POLY.productOfOnePlusXFy(List.of(new long[] {2, 3}), 5, 0);
		assertRectangular(zeroX, 0, 5);
		assertRectangular(zeroY, 5, 0);
	}

	private void assertRectangular(long[][] polynomial, int nx, int ny) {
		org.junit.jupiter.api.Assertions.assertEquals(nx, polynomial.length);
		for (long[] row : polynomial) org.junit.jupiter.api.Assertions.assertEquals(ny, row.length);
	}

	private long[][] powerSums(List<long[]> factors, int nx, int ny) {
		long[][] result = new long[nx][ny];
		for (long[] factor : factors) {
			long[] base = Arrays.copyOf(factor, Math.min(factor.length, ny));
			for (int j = 0; j < base.length; j++) base[j] = Math.floorMod(base[j], MOD);
			long[] power = base;
			for (int k = 1; k < nx; k++) {
				for (int j = 0; j < power.length; j++) result[k][j] = (result[k][j] + power[j]) % MOD;
				if (k + 1 < nx) power = truncateY(POLY.poly1d.mul(power, base), ny);
			}
		}
		return result;
	}

	private long[][] naive(List<long[]> factors, int nx, int ny) {
		if (nx <= 0 || ny <= 0) return POLY.zero();
		long[][] result = POLY.one();
		for (long[] factor : factors) {
			long[][] onePlusXFy = new long[2][];
			onePlusXFy[0] = new long[] {1};
			onePlusXFy[1] = Arrays.copyOf(factor, Math.min(factor.length, ny));
			for (int j = 0; j < onePlusXFy[1].length; j++) onePlusXFy[1][j] = Math.floorMod(onePlusXFy[1][j], MOD);
			result = truncate(POLY.mul(result, onePlusXFy), nx, ny);
		}
		return POLY.resize(result);
	}

	private long[] truncateY(long[] polynomial, int ny) {
		return Arrays.copyOf(polynomial, Math.min(polynomial.length, ny));
	}

	private long[][] truncate(long[][] polynomial, int nx, int ny) {
		long[][] result = new long[Math.min(polynomial.length, nx)][];
		for (int i = 0; i < result.length; i++) result[i] = truncateY(polynomial[i], ny);
		return result;
	}
}
