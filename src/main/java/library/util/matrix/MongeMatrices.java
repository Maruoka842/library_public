package library.util.matrix;

import java.util.Arrays;
import library.util.ArrayUtils;

/**
 * Monge 行列に関するアルゴリズムを格納する。
 *
 * <p>
 * Monge 行列とは、任意の i &lt; k, j &lt; l について
 * <pre>
 * a[i][j] + a[k][l] &lt;= a[i][l] + a[k][j]
 * </pre>
 * を満たす行列である。
 * </p>
 */
public final class MongeMatrices {

	private MongeMatrices() {}

	/**
	 * 行・列の置換を表す。
	 *
	 * <p>
	 * rows[i] は置換後の i 行目に来る元の行番号、
	 * cols[j] は置換後の j 列目に来る元の列番号を格納する。
	 * したがって、
	 * <pre>
	 * b[i][j] = a[rows[i]][cols[j]]
	 * </pre>
	 * が Monge 行列になる。
	 * </p>
	 */
	public record Permutation(int[] rows, int[] cols) {}

	/**
	 * 行・列を並べ替えることで、与えられた長方形行列 a を Monge 行列に変換する置換を求める。
	 * 変換不可能な場合は null を返す。
	 *
	 * <p>
	 * H = a.length, W = a[0].length とする。
	 * 差・和・積などの中間計算が long の範囲に収まることを仮定する。
	 * </p>
	 *
	 * @param a 長方形行列
	 * @return a を Monge 行列にする行・列置換。不可能な場合は null
	 * @throws NullPointerException a が null の場合
	 * @throws IllegalArgumentException 行列が長方形でない場合
	 *
	 * 計算量: O(H * W + H * log H + W * log W)
	 */
	// 未テスト
	public static Permutation findMongePermutation(long[][] a) {
		checkRectangular(a);

		int H = a.length;
		if (H == 0) {
			return new Permutation(new int[0], new int[0]);
		}

		int W = a[0].length;

		int[] identityRows = range(H);
		int[] identityCols = range(W);

		if (H <= 1 || W <= 1) {
			return new Permutation(identityRows, identityCols);
		}

		/*
		 * Step 1.
		 *
		 * row 0 と比較して、列差
		 *
		 *     a[0][j] - a[r][j]
		 *
		 * がすべて同じでない行 r を探す。
		 *
		 * 全行について一定なら、すべての 2x2 Monge 不等式が
		 * equality になるので、任意の行列順でよい。
		 */
		int r = -1;

		for (int i = 1; i < H && r == -1; i++) {
			long base = a[0][0] - a[i][0];

			for (int j = 1; j < W; j++) {
				if (a[0][j] - a[i][j] != base) {
					r = i;
					break;
				}
			}
		}

		if (r == -1) {
			return new Permutation(identityRows, identityCols);
		}

		/*
		 * Step 2.
		 *
		 * row 0, r に対して
		 *
		 *     a[0][j] - a[r][j]
		 *
		 * が昇順になるように列を仮に並べる。
		 */
		long[] keys = new long[W];
		for (int j = 0; j < W; j++) {
			keys[j] = a[0][j] - a[r][j];
		}

		int[] rho = ArrayUtils.argSort(keys);

		/*
		 * 同じ差を持つ列は ambiguity set。
		 *
		 * 最初の ambiguity set のサイズを b、
		 * 最後の ambiguity set のサイズを B とする。
		 */
		long firstDiff = a[0][rho[0]] - a[r][rho[0]];
		long lastDiff = a[0][rho[W - 1]] - a[r][rho[W - 1]];

		int b = 1;
		while (b < W
				&& a[0][rho[b]] - a[r][rho[b]] == firstDiff) {
			b++;
		}

		int B = 1;
		while (B < W
				&& a[0][rho[W - 1 - B]] - a[r][rho[W - 1 - B]]
						== lastDiff) {
			B++;
		}

		/*
		 * Step 3.
		 *
		 * 正しい最左列・最右列が ambiguity set 内で不明なので、
		 * 最初と最後の ambiguity set 全体を使って
		 * 各行のキーを作る。
		 *
		 * key[i]
		 *   = B * sum(first b columns)
		 *   - b * sum(last B columns)
		 *
		 * このキーの昇順に行を並べる。
		 */
		long[] rowKey = new long[H];

		for (int i = 0; i < H; i++) {
			long leftSum = 0;
			for (int j = 0; j < b; j++) {
				leftSum += a[i][rho[j]];
			}

			long rightSum = 0;
			for (int j = W - B; j < W; j++) {
				rightSum += a[i][rho[j]];
			}

			rowKey[i] = (long) B * leftSum - (long) b * rightSum;
		}

		int[] rows = ArrayUtils.argSort(rowKey);

		/*
		 * Step 4.
		 *
		 * 行順が分かったので、その最初・最後の行を使えば
		 * 正しい列順を直接決められる。
		 */
		int top = rows[0];
		int bottom = rows[H - 1];

		long[] colKey = new long[W];
		for (int j = 0; j < W; j++) {
			colKey[j] = a[top][j] - a[bottom][j];
		}

		int[] cols = ArrayUtils.argSort(colKey);

		/*
		 * Step 5.
		 *
		 * 候補 permutation が本当に Monge か検証。
		 *
		 * 隣接 2x2 の条件だけ調べれば十分。
		 */
		if (!isMonge(a, rows, cols)) {
			return null;
		}

		return new Permutation(rows, cols);
	}

	/**
	 * 与えられた行列 a がそのままの状態で Monge 行列であるかを判定する。
	 *
	 * @param a 長方形行列
	 * @return a が Monge 行列の場合は true, そうでない場合は false
	 * @throws NullPointerException a が null の場合
	 * @throws IllegalArgumentException 行列が長方形でない場合
	 *
	 * 計算量: O(H * W)
	 */
	// 未テスト
	public static boolean isMonge(long[][] a) {
		checkRectangular(a);

		if (a.length == 0) {
			return true;
		}

		return isMonge(a, range(a.length), range(a[0].length));
	}

	/**
	 * b[i][j] = a[rows[i]][cols[j]] としたとき、b が Monge 行列か判定する。
	 *
	 * @param a 元の行列
	 * @param rows 行の置換
	 * @param cols 列の置換
	 * @return 置換後の行列が Monge 行列の場合は true, そうでない場合は false
	 *
	 * 計算量: O(rows.length * cols.length)
	 */
	// 未テスト
	public static boolean isMonge(
			long[][] a,
			int[] rows,
			int[] cols) {

		for (int i = 0; i + 1 < rows.length; i++) {
			int r0 = rows[i];
			int r1 = rows[i + 1];

			for (int j = 0; j + 1 < cols.length; j++) {
				int c0 = cols[j];
				int c1 = cols[j + 1];

				if (a[r0][c0] + a[r1][c1]
						> a[r0][c1] + a[r1][c0]) {
					return false;
				}
			}
		}

		return true;
	}

	private static int[] range(int n) {
		int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		return a;
	}

	private static void checkRectangular(long[][] a) {
		if (a == null) {
			throw new NullPointerException();
		}

		if (a.length == 0) {
			return;
		}

		int W = a[0].length;

		for (int i = 1; i < a.length; i++) {
			if (a[i].length != W) {
				throw new IllegalArgumentException(
						"Matrix must be rectangular.");
			}
		}
	}
}