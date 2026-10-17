package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MatrixUtilsDoubleTest {
	@Test
	void arithmeticAndPower() {
		double[][] a = {{1, 2}, {3, 4}};
		assertMatrixEquals(new double[][] {{2, 4}, {6, 8}}, MatrixUtilsDouble.add(a, a));
		assertMatrixEquals(new double[][] {{7, 10}, {15, 22}}, MatrixUtilsDouble.mul(a, a));
		assertArrayEquals(new double[] {5, 11}, MatrixUtilsDouble.mul(a, new double[] {1, 2}), 1e-9);
		assertArrayEquals(new double[] {7, 10}, MatrixUtilsDouble.mul(new double[] {1, 2}, a), 1e-9);
		assertMatrixEquals(new double[][] {{1, 3}, {2, 4}}, MatrixUtilsDouble.transpose(a));
		assertMatrixEquals(MatrixUtilsDouble.mul(a, a), MatrixUtilsDouble.pow(a, 2));
		assertMatrixEquals(MatrixUtilsDouble.identity(2), MatrixUtilsDouble.pow(a, 0));
	}

	@Test
	void pivotingInverseAndDeterminant() {
		double[][] a = {{0, 2}, {3, 4}};
		assertEquals(-6, MatrixUtilsDouble.determinant(a), 1e-9);
		assertEquals(2, MatrixUtilsDouble.rank(a));
		assertMatrixEquals(MatrixUtilsDouble.identity(2), MatrixUtilsDouble.mul(a, MatrixUtilsDouble.inv(a)));
		assertMatrixEquals(new double[][] {{1, 0}, {0, 1}}, MatrixUtilsDouble.reducedRowEchelonForm(a));
		assertMatrixEquals(new double[][] {{0, 2}, {3, 4}}, a);
		assertNull(MatrixUtilsDouble.inv(new double[][] {{1, 2}, {2, 4}}));
		assertEquals(0, MatrixUtilsDouble.determinant(new double[][] {{1, 2}, {2, 4}}), 1e-9);
		assertEquals(1, MatrixUtilsDouble.determinant(new double[0][0]), 1e-9);
	}

	@Test
	void affineSolutionAndNullSpace() {
		double[][] a = {{1, 2, 3}, {2, 4, 6}};
		double[][] solution = MatrixUtilsDouble.solveLinearEquation(a, new double[] {4, 8});
		assertEquals(3, solution.length);
		assertArrayEquals(new double[] {4, 8}, MatrixUtilsDouble.mul(a, solution[0]), 1e-9);
		for (int i = 1; i < solution.length; i++) assertArrayEquals(new double[] {0, 0}, MatrixUtilsDouble.mul(a, solution[i]), 1e-9);
		assertNull(MatrixUtilsDouble.solveLinearEquation(a, new double[] {4, 9}));
		double[][] kernel = MatrixUtilsDouble.nullSpace(a);
		assertEquals(3, kernel.length);
		assertEquals(2, kernel[0].length);
		assertMatrixEquals(new double[2][2], MatrixUtilsDouble.mul(a, kernel));
	}

	@Test
	void thresholdCanBeChosen() {
		double[][] a = {{1e-12}};
		assertEquals(0, MatrixUtilsDouble.rank(a));
		assertEquals(1, MatrixUtilsDouble.rank(a, 0));
		assertNull(MatrixUtilsDouble.inv(a));
		assertEquals(1e12, MatrixUtilsDouble.inv(a, 0)[0][0], 1);
	}

	/**
	 * 行列の各成分を誤差 1e-9 以下で比較する。未テスト。計算量 O(NM)。
	 * @param expected 期待する N×M 行列
	 * @param actual 比較する N×M 行列
	 */
	private static void assertMatrixEquals(double[][] expected, double[][] actual) {
		assertNotNull(actual);
		assertEquals(expected.length, actual.length);
		for (int i = 0; i < expected.length; i++) assertArrayEquals(expected[i], actual[i], 1e-9);
	}
}
