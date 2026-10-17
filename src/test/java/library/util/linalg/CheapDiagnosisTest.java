package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CheapDiagnosisTest {
	@Test
	void shearHasFixedStateMovableDirectionAndInvariant() {
		double[][] t = {{1, 1}, {0, 1}};
		CheapDiagnosis.Result r = CheapDiagnosis.diagnose(new double[][][] {t});
		assertEquals(1, r.F()[0].length);
		assertEquals(1, r.M()[0].length);
		assertEquals(1, r.L()[0].length);
		assertEquals(0, r.F()[1][0], 1e-9);
		assertEquals(0, r.M()[1][0], 1e-9);
		assertEquals(0, r.L()[0][0], 1e-9);
		assertTrue(Math.abs(r.F()[0][0]) > 1e-9);
		assertTrue(Math.abs(r.M()[0][0]) > 1e-9);
		assertTrue(Math.abs(r.L()[1][0]) > 1e-9);
		assertEquals(0, dotColumn(r.L(), 0, r.M(), 0), 1e-9);
	}

	@Test
	void severalTransformationsUseIntersectionAndSum() {
		double[][][] ts = {{{1, 1}, {0, 1}}, {{1, 0}, {0, 2}}};
		CheapDiagnosis.Result r = CheapDiagnosis.diagnose(ts);
		assertEquals(1, r.F()[0].length);
		assertEquals(2, r.M()[0].length);
		assertEquals(0, r.L()[0].length);
		for (double[][] t : ts) {
			assertEquals(r.F()[0][0], t[0][0] * r.F()[0][0] + t[0][1] * r.F()[1][0], 1e-9);
			assertEquals(r.F()[1][0], t[1][0] * r.F()[0][0] + t[1][1] * r.F()[1][0], 1e-9);
		}
		assertEquals(2, MatrixUtilsDouble.rank(r.M()));
	}

	@Test
	void identityOperation() {
		CheapDiagnosis.Result identity = CheapDiagnosis.diagnose(new double[][][] {{{1, 0}, {0, 1}}});
		assertEquals(2, MatrixUtilsDouble.rank(identity.F()));
		assertEquals(2, MatrixUtilsDouble.rank(identity.L()));
		assertEquals(0, identity.M()[0].length);
	}

	@Test
	void rejectsIncorrectDimensions() {
		assertThrows(IllegalArgumentException.class, () -> CheapDiagnosis.diagnose(new double[0][][]));
		assertThrows(IllegalArgumentException.class, () -> CheapDiagnosis.diagnose(new double[][][] {{{1}, {0}}}));
		assertThrows(IllegalArgumentException.class, () -> CheapDiagnosis.diagnose(new double[][][] {{{1, 0}, {0}}}));
	}

	/**
	 * A[*][i]^T B[*][j] を返す。未テスト。計算量 O(N)。
	 * @param a 左の列基底
	 * @param i 左の列番号
	 * @param b 右の列基底
	 * @param j 右の列番号
	 * @return 内積
	 */
	private static double dotColumn(double[][] a, int i, double[][] b, int j) {
		double result = 0;
		for (int k = 0; k < a.length; k++) result += a[k][i] * b[k][j];
		return result;
	}
}
