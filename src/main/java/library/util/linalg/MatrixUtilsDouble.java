package library.util.linalg;

import java.util.Arrays;

/** double 係数の行列演算。 */
public final class MatrixUtilsDouble {
	/** 掃き出し法で零とみなす絶対値の上限。 */
	public static final double EPS = 1e-10;

	/**
	 * インスタンス化を禁止する。未テスト。計算量 O(1)。
	 */
	private MatrixUtilsDouble() {}

	/**
	 * A + B を返す。未テスト。計算量 O(NM)。
	 * @param a N×M 行列 A
	 * @param b N×M 行列 B
	 * @return A + B
	 */
	public static double[][] add(double[][] a, double[][] b) {
		int m = columns(a);
		if (a.length != b.length || (b.length != 0 && columns(b) != m)) throw new IllegalArgumentException("dimensions differ");
		double[][] c = new double[a.length][m];
		for (int i = 0; i < a.length; i++) for (int j = 0; j < m; j++) c[i][j] = a[i][j] + b[i][j];
		return c;
	}

	/**
	 * AB を返す。未テスト。計算量 O(NMK)、A は N×M、B は M×K。
	 * @param a 行列 A
	 * @param b 行列 B
	 * @return AB
	 */
	public static double[][] mul(double[][] a, double[][] b) {
		int m = columns(a);
		if (m != b.length) throw new IllegalArgumentException("dimensions differ");
		int k = columns(b);
		double[][] c = new double[a.length][k];
		for (int i = 0; i < a.length; i++) for (int j = 0; j < m; j++) for (int l = 0; l < k; l++) c[i][l] += a[i][j] * b[j][l];
		return c;
	}

	/**
	 * Av を返す。未テスト。計算量 O(NM)。
	 * @param a N×M 行列 A
	 * @param v 長さ M の列ベクトル
	 * @return Av
	 */
	public static double[] mul(double[][] a, double[] v) {
		int m = columns(a);
		if (m != v.length) throw new IllegalArgumentException("dimensions differ");
		double[] c = new double[a.length];
		for (int i = 0; i < a.length; i++) for (int j = 0; j < m; j++) c[i] += a[i][j] * v[j];
		return c;
	}

	/**
	 * vA を返す。未テスト。計算量 O(NM)。
	 * @param v 長さ N の行ベクトル
	 * @param a N×M 行列 A
	 * @return vA
	 */
	public static double[] mul(double[] v, double[][] a) {
		int m = columns(a);
		if (v.length != a.length) throw new IllegalArgumentException("dimensions differ");
		double[] c = new double[m];
		for (int i = 0; i < a.length; i++) for (int j = 0; j < m; j++) c[j] += v[i] * a[i][j];
		return c;
	}

	/**
	 * A^T を返す。未テスト。計算量 O(NM)。
	 * @param a N×M 行列 A
	 * @return A^T
	 */
	public static double[][] transpose(double[][] a) {
		int m = columns(a);
		double[][] b = new double[m][a.length];
		for (int i = 0; i < a.length; i++) for (int j = 0; j < m; j++) b[j][i] = a[i][j];
		return b;
	}

	/**
	 * I_N を返す。未テスト。計算量 O(N^2)。
	 * @param n 次数 N
	 * @return I_N
	 */
	public static double[][] identity(int n) {
		double[][] a = new double[n][n];
		for (int i = 0; i < n; i++) a[i][i] = 1;
		return a;
	}

	/**
	 * A^e を返す。e >= 0。未テスト。計算量 O(N^3 log(e + 1))。
	 * @param a N×N 行列 A
	 * @param e 指数
	 * @return A^e
	 */
	public static double[][] pow(double[][] a, long e) {
		if (e < 0) throw new IllegalArgumentException("negative exponent");
		int n = a.length;
		if (columns(a) != n) throw new IllegalArgumentException("matrix is not square");
		double[][] ans = identity(n), base = copy(a);
		while (e != 0) {
			if ((e & 1) != 0) ans = mul(ans, base);
			e >>>= 1;
			if (e != 0) base = mul(base, base);
		}
		return ans;
	}

	/**
	 * A の既約行階段形を返す。|pivot| <= eps は零とみなす。
	 * 未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @param eps 零判定の非負閾値
	 * @return A の既約行階段形
	 */
	public static double[][] reducedRowEchelonForm(double[][] a, double eps) {
		checkEps(eps);
		double[][] b = copy(a);
		eliminate(b, columns(a), eps);
		return b;
	}

	/**
	 * A の既約行階段形を返す。|pivot| <= EPS は零とみなす。
	 * 未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @return A の既約行階段形
	 */
	public static double[][] reducedRowEchelonForm(double[][] a) {
		return reducedRowEchelonForm(a, EPS);
	}

	/**
	 * rank(A) を返す。|pivot| <= eps は零とみなす。
	 * 未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @param eps 零判定の非負閾値
	 * @return rank(A)
	 */
	public static int rank(double[][] a, double eps) {
		checkEps(eps);
		return eliminate(copy(a), columns(a), eps).length;
	}

	/**
	 * rank(A) を返す。|pivot| <= EPS は零とみなす。
	 * 未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @return rank(A)
	 */
	public static int rank(double[][] a) {
		return rank(a, EPS);
	}

	/**
	 * det(A) を返す。|pivot| <= eps は零とみなす。
	 * 未テスト。計算量 O(N^3)。
	 * @param a N×N 行列 A
	 * @param eps 零判定の非負閾値
	 * @return det(A)
	 */
	public static double determinant(double[][] a, double eps) {
		checkEps(eps);
		int n = a.length;
		if (columns(a) != n) throw new IllegalArgumentException("matrix is not square");
		double[][] b = copy(a);
		double det = 1;
		for (int j = 0; j < n; j++) {
			int p = j;
			for (int i = j + 1; i < n; i++) if (Math.abs(b[i][j]) > Math.abs(b[p][j])) p = i;
			if (Math.abs(b[p][j]) <= eps) return 0;
			if (p != j) { double[] tmp = b[p]; b[p] = b[j]; b[j] = tmp; det = -det; }
			double pivot = b[j][j];
			det *= pivot;
			for (int i = j + 1; i < n; i++) {
				double factor = b[i][j] / pivot;
				for (int k = j + 1; k < n; k++) b[i][k] -= factor * b[j][k];
				b[i][j] = 0;
			}
		}
		return det;
	}

	/**
	 * det(A) を返す。|pivot| <= EPS は零とみなす。
	 * 未テスト。計算量 O(N^3)。
	 * @param a N×N 行列 A
	 * @return det(A)
	 */
	public static double determinant(double[][] a) {
		return determinant(a, EPS);
	}

	/**
	 * A^-1 を返す。|pivot| <= eps の場合は null。
	 * 未テスト。計算量 O(N^3)。
	 * @param a N×N 行列 A
	 * @param eps 零判定の非負閾値
	 * @return A^-1、逆行列が得られない場合 null
	 */
	public static double[][] inv(double[][] a, double eps) {
		checkEps(eps);
		int n = a.length;
		if (columns(a) != n) throw new IllegalArgumentException("matrix is not square");
		double[][] b = new double[n][2 * n];
		for (int i = 0; i < n; i++) {
			System.arraycopy(a[i], 0, b[i], 0, n);
			b[i][n + i] = 1;
		}
		if (eliminate(b, n, eps).length != n) return null;
		double[][] ans = new double[n][n];
		for (int i = 0; i < n; i++) System.arraycopy(b[i], n, ans[i], 0, n);
		return ans;
	}

	/**
	 * A^-1 を返す。|pivot| <= EPS の場合は null。
	 * 未テスト。計算量 O(N^3)。
	 * @param a N×N 行列 A
	 * @return A^-1、逆行列が得られない場合 null
	 */
	public static double[][] inv(double[][] a) {
		return inv(a, EPS);
	}

	/**
	 * Ax=b の解を {x, v_1, ..., v_d} の行ベクトルとして返す。
	 * Av_i=0、x は自由変数を零とした解。|pivot| <= eps は零とみなす。
	 * 未テスト。計算量 O(N(M+1) min(N,M+1))。
	 * @param a N×M 行列 A
	 * @param b 長さ N の列ベクトル
	 * @param eps 零判定の非負閾値
	 * @return 解と核の基底、解が得られない場合 null
	 */
	public static double[][] solveLinearEquation(double[][] a, double[] b, double eps) {
		checkEps(eps);
		int m = columns(a), n = a.length;
		if (b.length != n) throw new IllegalArgumentException("dimensions differ");
		double[][] mat = new double[n][m + 1];
		for (int i = 0; i < n; i++) {
			System.arraycopy(a[i], 0, mat[i], 0, m);
			mat[i][m] = b[i];
		}
		int[] pivots = eliminate(mat, m, eps);
		for (int i = pivots.length; i < n; i++) if (Math.abs(mat[i][m]) > eps) return null;
		boolean[] used = new boolean[m];
		for (int p : pivots) used[p] = true;
		double[][] ans = new double[1 + m - pivots.length][m];
		for (int i = 0; i < pivots.length; i++) ans[0][pivots[i]] = mat[i][m];
		int t = 1;
		for (int j = 0; j < m; j++) if (!used[j]) {
			ans[t][j] = 1;
			for (int i = 0; i < pivots.length; i++) ans[t][pivots[i]] = -mat[i][j];
			t++;
		}
		return ans;
	}

	/**
	 * Ax=b の解と核の基底を返す。|pivot| <= EPS は零とみなす。
	 * 未テスト。計算量 O(N(M+1) min(N,M+1))。
	 * @param a N×M 行列 A
	 * @param b 長さ N の列ベクトル
	 * @return {x, v_1, ..., v_d}、解が得られない場合 null
	 */
	public static double[][] solveLinearEquation(double[][] a, double[] b) {
		return solveLinearEquation(a, b, EPS);
	}

	/**
	 * AB=0 を満たす核の基底 B を返す。B[*][j] は第 j 基底。
	 * |pivot| <= eps は零とみなす。未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @param eps 零判定の非負閾値
	 * @return M×(M-rank(A)) 行列 B
	 */
	public static double[][] nullSpace(double[][] a, double eps) {
		checkEps(eps);
		int m = columns(a);
		double[][] mat = copy(a);
		int[] pivots = eliminate(mat, m, eps);
		boolean[] used = new boolean[m];
		for (int p : pivots) used[p] = true;
		double[][] ans = new double[m][m - pivots.length];
		int t = 0;
		for (int j = 0; j < m; j++) if (!used[j]) {
			ans[j][t] = 1;
			for (int i = 0; i < pivots.length; i++) ans[pivots[i]][t] = -mat[i][j];
			t++;
		}
		return ans;
	}

	/**
	 * AB=0 を満たす核の基底 B を返す。|pivot| <= EPS は零とみなす。
	 * 未テスト。計算量 O(NM min(N,M))。
	 * @param a N×M 行列 A
	 * @return M×(M-rank(A)) 行列 B
	 */
	public static double[][] nullSpace(double[][] a) {
		return nullSpace(a, EPS);
	}

	/**
	 * A の各行を複製する。未テスト。計算量 O(NM)。
	 * @param a N×M 行列 A
	 * @return A の独立した複製
	 */
	private static double[][] copy(double[][] a) {
		columns(a);
		double[][] b = new double[a.length][];
		for (int i = 0; i < a.length; i++) b[i] = Arrays.copyOf(a[i], a[i].length);
		return b;
	}

	/**
	 * 行列の列数を返し、矩形性を検査する。未テスト。計算量 O(N)。
	 * @param a 行列
	 * @return 列数（零行の場合は 0）
	 */
	private static int columns(double[][] a) {
		int m = a.length == 0 ? 0 : a[0].length;
		for (double[] row : a) if (row.length != m) throw new IllegalArgumentException("ragged matrix");
		return m;
	}

	/**
	 * eps >= 0 かつ有限であることを検査する。未テスト。計算量 O(1)。
	 * @param eps 零判定の閾値
	 */
	private static void checkEps(double eps) {
		if (!Double.isFinite(eps) || eps < 0) throw new IllegalArgumentException("invalid eps");
	}

	/**
	 * 左側 cols 列を既約行階段形にし、ピボット列を返す。
	 * |pivot| <= eps は零とみなす。未テスト。計算量 O(NM min(N,cols))。
	 * @param a 破壊的に変更する N×M 行列
	 * @param cols 掃き出す列数
	 * @param eps 零判定の閾値
	 * @return ピボット列の昇順配列
	 */
	private static int[] eliminate(double[][] a, int cols, double eps) {
		int n = a.length, m = columns(a), rank = 0;
		int[] pivots = new int[Math.min(n, cols)];
		for (int j = 0; j < cols && rank < n; j++) {
			int p = rank;
			for (int i = rank + 1; i < n; i++) if (Math.abs(a[i][j]) > Math.abs(a[p][j])) p = i;
			if (Math.abs(a[p][j]) <= eps) continue;
			double[] tmp = a[p]; a[p] = a[rank]; a[rank] = tmp;
			double pivot = a[rank][j];
			for (int k = j; k < m; k++) a[rank][k] /= pivot;
			for (int i = 0; i < n; i++) if (i != rank) {
				double factor = a[i][j];
				for (int k = j + 1; k < m; k++) a[i][k] -= factor * a[rank][k];
				a[i][j] = 0;
			}
			pivots[rank++] = j;
		}
		return Arrays.copyOf(pivots, rank);
	}
}
