package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class InvNewtonTest {
	private static final PolynomialFpDynamic2D POLY = PolynomialFpDynamic2D.MOD998244353;

	@Test
	void matchesCoefficientRecurrence() {
		Random random = new Random(20260919L);
		for (int trial = 0; trial < 100; trial++) {
			int nx = random.nextInt(24) + 1;
			int ny = random.nextInt(12) + 1;
			long[][] a = new long[nx][ny];
			a[0][0] = 1;
			for (int i = 0; i < nx; i++) {
				for (int j = i == 0 ? 1 : 0; j < ny; j++) a[i][j] = random.nextLong(POLY.mod);
			}
			assertTrue(POLY.equals(POLY.inv(a), POLY.inv(a, nx, ny)));
		}
	}

	@Test
	void acceptsNonconstantAZeroYAndRejectsWrongScalarConstant() {
		long[][] a = {{1, 2, 3}, {4, 5}};
		long[][] inverse = POLY.inv(a, 5, 6);
		assertTrue(POLY.equals(POLY.one(), truncate(POLY.mul(a, inverse), 5, 6)));
		assertThrows(IllegalArgumentException.class, () -> POLY.inv(new long[][] {{2, 1}}, 5, 5));
		assertTrue(POLY.equals(POLY.zero(), POLY.inv(a, 0, 5)));
		assertTrue(POLY.equals(POLY.zero(), POLY.inv(a, 5, 0)));
	}

	private long[][] truncate(long[][] polynomial, int nx, int ny) {
		long[][] result = new long[Math.min(polynomial.length, nx)][];
		for (int i = 0; i < result.length; i++) {
			result[i] = java.util.Arrays.copyOf(polynomial[i], Math.min(polynomial[i].length, ny));
		}
		return POLY.resize(result);
	}
}
