package library.util.linalg;

/** 複数の線形変換に共通する固定空間、変化方向、保存量を求める。 */
public final class CheapDiagnosis {
	/**
	 * インスタンス化を禁止する。未テスト。計算量 O(1)。
	 */
	private CheapDiagnosis() {}

	/**
	 * F, M, L の基底。各行列の列が基底ベクトルであり、
	 * F と L は n×dim(F), n×dim(L)、M は n×dim(M)。
	 * 配列は呼び出し側が所有する。
	 *
	 * @param F ⋂ ker(T_i-I) の基底
	 * @param M Σ im(T_i-I) の基底
	 * @param L ⋂ ker((T_i-I)^T) の基底
	 */
	public record Result(double[][] F, double[][] M, double[][] L) {}

	/**
	 * E_i=T_i-I とし、F=⋂ker(E_i)、M=Σim(E_i)、L=⋂ker(E_i^T) を返す。
	 * |pivot| <= eps は零とみなす。未テスト。計算量 O(m n^3)、空間量 O(m n^2)。
	 *
	 * @param transformations n×n 行列 T_i の配列
	 * @param eps 零判定の非負閾値
	 * @return F, M, L の列基底
	 */
	public static Result diagnose(double[][][] transformations, double eps) {
		if (!Double.isFinite(eps) || eps < 0) throw new IllegalArgumentException("invalid eps");
		int m = transformations.length;
		if (m == 0) throw new IllegalArgumentException("no transformations");
		int n = transformations[0].length;
		if (n == 0) {
			for (double[][] t : transformations) if (t.length != 0) throw new IllegalArgumentException("dimensions differ");
			return new Result(new double[0][0], new double[0][0], new double[0][0]);
		}
		if ((long) m * n > Integer.MAX_VALUE) throw new IllegalArgumentException("matrix is too large");
		double[][] stacked = new double[m * n][n];
		double[][] stackedTransposes = new double[m * n][n];
		for (int t = 0; t < m; t++) {
			if (transformations[t].length != n) throw new IllegalArgumentException("dimensions differ");
			for (int i = 0; i < n; i++) {
				if (transformations[t][i].length != n) throw new IllegalArgumentException("dimensions differ");
				for (int j = 0; j < n; j++) {
					double value = transformations[t][i][j] - (i == j ? 1 : 0);
					stacked[t * n + i][j] = value;
					stackedTransposes[t * n + j][i] = value;
				}
			}
		}
		double[][] f = MatrixUtilsDouble.nullSpace(stacked, eps);
		double[][] l = MatrixUtilsDouble.nullSpace(stackedTransposes, eps);
		double[][] rref = MatrixUtilsDouble.reducedRowEchelonForm(stackedTransposes, eps);
		int rank = MatrixUtilsDouble.rank(stackedTransposes, eps);
		double[][] movable = new double[n][rank];
		for (int i = 0; i < rank; i++) for (int j = 0; j < n; j++) movable[j][i] = rref[i][j];
		return new Result(f, movable, l);
	}

	/**
	 * E_i=T_i-I とし、F=⋂ker(E_i)、M=Σim(E_i)、L=⋂ker(E_i^T) を返す。
	 * |pivot| <= MatrixUtilsDouble.EPS は零とみなす。
	 * 未テスト。計算量 O(m n^3)、空間量 O(m n^2)。
	 *
	 * @param transformations n×n 行列 T_i の配列
	 * @return F, M, L の列基底
	 */
	public static Result diagnose(double[][][] transformations) {
		return diagnose(transformations, MatrixUtilsDouble.EPS);
	}
}
