package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class ProductTreeSumTest {
	@Test
	public void testSmall() {
		PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
		int N = 4;
		ProductTreeSum pts = new ProductTreeSum(N, poly);
		long[][] L = {
			{1, 2},
			{1, 3},
			{1, 4}
		};
		long[][] R = {
			null,
			{1, 5},
			{1, 6},
			{1, 7}
		};
		for (int i = 0; i < N - 1; i++) pts.setL(i, L[i]);
		for (int i = 1; i < N; i++) pts.setR(i, R[i]);

		long[][] f = {
			{1, 2},
			{3, 4},
			{5, 6},
			{7, 8}
		};
		long[] ans = pts.calc(f);

		long[] expected = new long[0];
		for (int i = 0; i < N; i++) {
			long[] cur = f[i];
			for (int j = 0; j < i; j++) cur = poly.mul(cur, L[j]);
			for (int j = i + 1; j < N; j++) cur = poly.mul(cur, R[j]);
			expected = poly.add(expected, cur);
		}

		assertArrayEquals(poly.resize(expected), poly.resize(ans));
	}

	@Test
	public void testRandom() {
		Random rnd = new Random(42);
		PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
		int numTests = 10;
		for (int t = 0; t < numTests; t++) {
			int N = rnd.nextInt(20) + 1;
			ProductTreeSum pts = new ProductTreeSum(N, poly);
			long[][] L = new long[N - 1][];
			long[][] R = new long[N][];
			for (int i = 0; i < N - 1; i++) {
				L[i] = new long[rnd.nextInt(5) + 1];
				for (int j = 0; j < L[i].length; j++) L[i][j] = rnd.nextInt((int) poly.mod);
				pts.setL(i, L[i]);
			}
			for (int i = 1; i < N; i++) {
				R[i] = new long[rnd.nextInt(5) + 1];
				for (int j = 0; j < R[i].length; j++) R[i][j] = rnd.nextInt((int) poly.mod);
				pts.setR(i, R[i]);
			}
			long[][] f = new long[N][];
			for (int i = 0; i < N; i++) {
				f[i] = new long[rnd.nextInt(10) + 1];
				for (int j = 0; j < f[i].length; j++) f[i][j] = rnd.nextInt((int) poly.mod);
			}
			long[] ans = pts.calc(f);

			long[] expected = new long[0];
			for (int i = 0; i < N; i++) {
				long[] cur = f[i];
				for (int j = 0; j < i; j++) cur = poly.mul(cur, L[j]);
				for (int j = i + 1; j < N; j++) cur = poly.mul(cur, R[j]);
				expected = poly.add(expected, cur);
			}
			assertArrayEquals(poly.resize(expected), poly.resize(ans));
		}
	}

	@Test
	public void testSumPowerProductsEdgeCases() {
		PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
		// N = 0
		long[] empty = poly.sumPowerProductsByBoundedVariation(new long[] { 1, 1 }, new long[] { 2, 3 }, new long[0], new int[0], new int[0]);
		assertArrayEquals(new long[0], empty);

		// N = 1
		long[] f = { 1, 2 };
		long[] g = { 3, 4 };
		long[] A = { 5 };
		int[] B = { 2 };
		int[] C = { 3 };
		long[] ans1 = poly.sumPowerProductsByBoundedVariation(f, g, A, B, C);
		long[] expected1 = poly.mul(poly.powFull(f, 2), poly.powFull(g, 3));
		expected1 = poly.mul(expected1, 5);
		assertArrayEquals(poly.resize(expected1), poly.resize(ans1));
	}

	@Test
	public void testSumPowerProductsSmall() {
		PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
		long[] f = { 1, 1 };
		long[] g = { 2, 1 };
		long[] A = { 3, 4, 5, 2 };

		// Test various sequences (monotone, valley, mountain)
		int[][] B_cases = {
			{ 1, 2, 3, 4 },
			{ 4, 3, 2, 1 },
			{ 5, 1, 0, 4 },
			{ 0, 3, 3, 0 },
			{ 2, 2, 2, 2 }
		};

		int[][] C_cases = {
			{ 2, 1, 0, 3 },
			{ 0, 2, 4, 1 },
			{ 3, 3, 3, 3 },
			{ 1, 0, 1, 0 },
			{ 0, 1, 2, 3 }
		};

		for (int c = 0; c < B_cases.length; c++) {
			int[] B = B_cases[c];
			int[] C = C_cases[c];
			long[] ans = poly.sumPowerProductsByBoundedVariation(f, g, A, B, C);

			long[] expected = new long[0];
			for (int i = 0; i < A.length; i++) {
				long[] fPow = poly.powFull(f, (int) B[i]);
				long[] gPow = poly.powFull(g, (int) C[i]);
				long[] term = poly.mul(poly.mul(fPow, gPow), A[i]);
				expected = poly.add(expected, term);
			}

			assertArrayEquals(poly.resize(expected), poly.resize(ans));
		}
	}

	@Test
	public void testSumPowerProductsRandom() {
		Random rnd = new Random(12345);
		PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
		int numTests = 20;

		for (int t = 0; t < numTests; t++) {
			int N = rnd.nextInt(15) + 1;
			long[] f = new long[rnd.nextInt(3) + 1];
			for (int i = 0; i < f.length; i++) f[i] = rnd.nextInt(100);
			long[] g = new long[rnd.nextInt(3) + 1];
			for (int i = 0; i < g.length; i++) g[i] = rnd.nextInt(100);

			long[] A = new long[N];
			int[] B = new int[N];
			int[] C = new int[N];

			for (int i = 0; i < N; i++) {
				A[i] = rnd.nextInt(100);
			}

			// Generate B, C with random steps
			B[0] = rnd.nextInt(5);
			C[0] = rnd.nextInt(5);
			for (int i = 1; i < N; i++) {
				B[i] = Math.max(0, B[i - 1] + rnd.nextInt(5) - 2);
				C[i] = Math.max(0, C[i - 1] + rnd.nextInt(5) - 2);
			}

			long[] ans = poly.sumPowerProductsByBoundedVariation(f, g, A, B, C);

			long[] expected = new long[0];
			for (int i = 0; i < N; i++) {
				long[] fPow = poly.powFull(f, (int) B[i]);
				long[] gPow = poly.powFull(g, (int) C[i]);
				long[] term = poly.mul(poly.mul(fPow, gPow), poly.fp.reduce(A[i]));
				expected = poly.add(expected, term);
			}

			assertArrayEquals(poly.resize(expected), poly.resize(ans));
		}
	}
}
