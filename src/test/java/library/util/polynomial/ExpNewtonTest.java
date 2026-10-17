package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class ExpNewtonTest {
	private static final PolynomialFpDynamic2D POLY = PolynomialFpDynamic2D.MOD998244353;

	@Test
	void matchesCoefficientRecurrence() {
		Random random = new Random(20260919L);
		for (int trial = 0; trial < 100; trial++) {
			int nx = random.nextInt(24) + 1;
			int ny = random.nextInt(12) + 1;
			long[][] a = new long[nx][ny];
			for (int i = 0; i < nx; i++) {
				for (int j = i == 0 ? 1 : 0; j < ny; j++) a[i][j] = random.nextLong(POLY.mod);
			}
			assertTrue(POLY.equals(expNaive(a, nx, ny), POLY.exp(a, nx, ny)));
		}
	}

	@Test
	void handlesTruncationAndRejectsNonzeroScalarConstant() {
		assertTrue(POLY.equals(POLY.one(), POLY.exp(new long[][] {{0}, {3, 4}}, 1, 5)));
		assertTrue(POLY.equals(POLY.zero(), POLY.exp(new long[][] {{0}}, 0, 5)));
		assertTrue(POLY.equals(POLY.zero(), POLY.exp(new long[][] {{0}}, 5, 0)));
		assertTrue(POLY.equals(new long[][] {{1, 1, POLY.fp.inv(2)}},
				POLY.exp(new long[][] {{0, 1}}, 1, 3)));
		assertThrows(IllegalArgumentException.class, () -> POLY.exp(new long[][] {{1}}, 5, 5));
	}

	private long[][] expNaive(long[][] a, int nx, int ny) {
		long[][] result = new long[nx][ny];
		result[0][0] = 1;
		for (int i = 1; i < nx; i++) {
			for (int k = 1; k <= i; k++) {
				long[] product = POLY.poly1d.mul(a[k], result[i - k]);
				for (int j = 0; j < Math.min(product.length, ny); j++) {
					result[i][j] = (result[i][j] + k * product[j]) % POLY.mod;
				}
			}
			long inverse = POLY.fp.inv(i);
			for (int j = 0; j < ny; j++) result[i][j] = result[i][j] * inverse % POLY.mod;
		}
		long[] constantFactor = POLY.poly1d.exp(a[0]);
		return truncate(POLY.mul(result, new long[][] {constantFactor}), nx, ny);
	}

	private long[][] truncate(long[][] polynomial, int nx, int ny) {
		long[][] result = new long[Math.min(polynomial.length, nx)][];
		for (int i = 0; i < result.length; i++) {
			result[i] = java.util.Arrays.copyOf(polynomial[i], Math.min(polynomial[i].length, ny));
		}
		return POLY.resize(result);
	}
}
