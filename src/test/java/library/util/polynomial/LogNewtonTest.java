package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class LogNewtonTest {
	private static final PolynomialFpDynamic2D POLY = PolynomialFpDynamic2D.MOD998244353;

	@Test
	void expOfLogIsOriginalSeries() {
		Random random = new Random(20260919L);
		for (int trial = 0; trial < 100; trial++) {
			int nx = random.nextInt(24) + 1;
			int ny = random.nextInt(12) + 1;
			long[][] a = new long[nx][ny];
			a[0][0] = 1;
			for (int i = 0; i < nx; i++) {
				for (int j = i == 0 ? 1 : 0; j < ny; j++) a[i][j] = random.nextLong(POLY.mod);
			}
			assertTrue(POLY.equals(POLY.resize(a), POLY.exp(POLY.log(a, nx, ny), nx, ny)));
		}
	}

	@Test
	void acceptsNonconstantAZeroYAndRejectsWrongScalarConstant() {
		long inverseTwo = POLY.fp.inv(2);
		assertTrue(POLY.equals(new long[][] {{0, 1, POLY.mod - inverseTwo}},
				POLY.log(new long[][] {{1, 1}}, 1, 3)));
		assertThrows(IllegalArgumentException.class, () -> POLY.log(new long[][] {{2, 1}}, 5, 5));
		assertTrue(POLY.equals(POLY.zero(), POLY.log(new long[][] {{1, 1}}, 0, 5)));
		assertTrue(POLY.equals(POLY.zero(), POLY.log(new long[][] {{1, 1}}, 5, 0)));
	}
}
