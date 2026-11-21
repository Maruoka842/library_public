package library.util.linalg;

/**
 * OR-AND 半環上の行列演算。
 */
public final class MatrixUtilsOrAnd {
	private MatrixUtilsOrAnd() {}

	/**
	 * {@code c[i][j] = OR_k (a[i][k] AND b[k][j])} を返す。
	 *
	 * <p>未テスト。計算量: {@code O(nm ceil(k / w))}、{@code w = }{@link Long#SIZE}。</p>
	 *
	 * @param a {@code n × k} 行列
	 * @param b {@code k × m} 行列
	 * @return {@code n × m} 行列
	 * @throws AssertionError 行列の列数と行数が一致しない場合、または行の長さが不揃いの場合
	 */
	public static boolean[][] mul(boolean[][] a, boolean[][] b) {
		int n = a.length;
		int k = n == 0 ? b.length : a[0].length;
		if (b.length != k) throw new AssertionError();
		int m = b.length == 0 ? 0 : b[0].length;
		for (boolean[] row : a) if (row.length != k) throw new AssertionError();
		for (boolean[] row : b) if (row.length != m) throw new AssertionError();
		long outputCells = (long) n * m;
		if (outputCells < 256 || k < 6) {
			boolean[][] c = new boolean[n][m];
			for (int i = 0; i < n; i++) {
				for (int t = 0; t < k; t++) {
					if (!a[i][t]) continue;
					for (int j = 0; j < m; j++) c[i][j] |= b[t][j];
				}
			}
			return c;
		}
		return mulPacked(a, b);
	}

	/**
	 * {@code c[i][j] = OR_k (a[i][k] AND b[k][j])} を返す。
	 *
	 * <p>未テスト。計算量: {@code O(nm ceil(k / w))}、{@code w = }{@link Long#SIZE}。</p>
	 *
	 * @param a {@code n × k} 行列
	 * @param b {@code k × m} 行列
	 * @return {@code n × m} 行列
	 */
	private static boolean[][] mulPacked(boolean[][] a, boolean[][] b) {
		int n = a.length;
		int k = b.length;
		int m = b.length == 0 ? 0 : b[0].length;
		return unpack(mulPacked(pack(a, k), pack(b, m), n, k, m), n, m);
	}

	/**
	 * {@code n × m} 行列を行ごとのビット列に変換する。
	 *
	 * <p>未テスト。計算量: {@code O(nm)}。</p>
	 *
	 * @param a {@code n × m} 行列
	 * @param m 列数
	 * @return {@code a[i][j]} を bit {@code j} に格納した行列
	 */
	private static long[][] pack(boolean[][] a, int m) {
		int words = (m + Long.SIZE - 1) / Long.SIZE;
		long[][] ret = new long[a.length][words];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < m; j++) if (a[i][j]) ret[i][j / Long.SIZE] |= 1L << (j % Long.SIZE);
		}
		return ret;
	}

	/**
	 * 行ごとのビット列を {@code n × m} 論理行列に戻す。
	 *
	 * <p>未テスト。計算量: {@code O(nm)}。</p>
	 *
	 * @param a 行ごとのビット列
	 * @param n 行数
	 * @param m 列数
	 * @return 論理行列
	 */
	private static boolean[][] unpack(long[][] a, int n, int m) {
		boolean[][] ret = new boolean[n][m];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) ret[i][j] = ((a[i][j / Long.SIZE] >>> (j % Long.SIZE)) & 1L) != 0;
		}
		return ret;
	}

	/**
	 * ビット列形式の {@code n × k} 行列と {@code k × m} 行列の OR-AND 積を返す。
	 *
	 * <p>未テスト。計算量: {@code O(nm ceil(k / w))}、{@code w = }{@link Long#SIZE}。</p>
	 *
	 * @param a 行ごとのビット列形式の {@code n × k} 行列
	 * @param b 行ごとのビット列形式の {@code k × m} 行列
	 * @param n 行数
	 * @param k 中間次元
	 * @param m 列数
	 * @return 行ごとのビット列形式の積
	 */
	private static long[][] mulPacked(long[][] a, long[][] b, int n, int k, int m) {
		int wordsK = (k + Long.SIZE - 1) / Long.SIZE;
		int wordsM = (m + Long.SIZE - 1) / Long.SIZE;
		long[][] bt = new long[m][wordsK];
		for (int t = 0; t < k; t++) {
			for (int j = 0; j < m; j++) {
				if (((b[t][j / Long.SIZE] >>> (j % Long.SIZE)) & 1L) != 0) {
					bt[j][t / Long.SIZE] |= 1L << (t % Long.SIZE);
				}
			}
		}
		long[][] ret = new long[n][wordsM];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				for (int w = 0; w < wordsK; w++) {
					if ((a[i][w] & bt[j][w]) != 0) {
						ret[i][j / Long.SIZE] |= 1L << (j % Long.SIZE);
						break;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * {@code c[i][j] = a[i][j] OR b[i][j]} を返す。
	 *
	 * <p>未テスト。計算量: {@code O(nm)}。</p>
	 *
	 * @param a {@code n × m} 行列
	 * @param b {@code n × m} 行列
	 * @return 要素ごとの論理和
	 * @throws AssertionError 行列の形状が一致しない場合
	 */
	public static boolean[][] add(boolean[][] a, boolean[][] b) {
		if (a.length != b.length) throw new AssertionError();
		boolean[][] ret = new boolean[a.length][];
		for (int i = 0; i < a.length; i++) {
			if (a[i].length != b[i].length) throw new AssertionError();
			ret[i] = new boolean[a[i].length];
			for (int j = 0; j < a[i].length; j++) ret[i][j] = a[i][j] | b[i][j];
		}
		return ret;
	}

	/**
	 * OR-AND 半環の {@code n × n} 単位行列 {@code I}（{@code I[i][j] = (i = j)}）を返す。
	 *
	 * <p>未テスト。計算量: {@code O(n^2)}。</p>
	 *
	 * @param n 次元
	 * @return 単位行列
	 */
	public static boolean[][] booleanMatrixIdentity(int n) {
		boolean[][] ret = new boolean[n][n];
		for (int i = 0; i < n; i++) ret[i][i] = true;
		return ret;
	}

	/**
	 * {@code a^e} を OR-AND 半環上で返す。{@code a^0 = I}。
	 *
	 * <p>未テスト。計算量: {@code O(n^3 ceil(n / w) log(e + 1))}、{@code w = }{@link Long#SIZE}。</p>
	 *
	 * @param a {@code n × n} 行列
	 * @param e {@code e >= 0}
	 * @return {@code a^e}
	 * @throws AssertionError {@code e < 0} または {@code a} が正方行列でない場合
	 */
	public static boolean[][] pow(boolean[][] a, long e) {
		//https://atcoder.jp/contests/abc388/submissions/80788424
		if (e < 0) throw new AssertionError();
		int n = a.length;
		for (boolean[] row : a) if (row.length != n) throw new AssertionError();
		long[][] result = new long[n][(n + Long.SIZE - 1) / Long.SIZE];
		for (int i = 0; i < n; i++) result[i][i / Long.SIZE] |= 1L << (i % Long.SIZE);
		long[][] base = pack(a, n);
		while (e != 0) {
			if ((e & 1) != 0) result = mulPacked(result, base, n, n, n);
			e >>>= 1;
			if (e != 0) base = mulPacked(base, base, n, n, n);
		}
		return unpack(result, n, n);
	}

	/**
	 * {@code I OR A OR ... OR A^(n-1)} を OR-AND 半環上で返す。
	 *
	 * <p>未テスト。計算量: {@code O(d^3 ceil(d / w) log(n + 1))}、{@code w = }{@link Long#SIZE}。</p>
	 *
	 * @param a {@code d × d} 行列
	 * @param n 項数。{@code n >= 0}
	 * @return {@code A^0 OR A^1 OR ... OR A^(n-1)}
	 * @throws AssertionError {@code n < 0} または {@code a} が正方行列でない場合
	 */
	public static boolean[][] geometricSum(boolean[][] a, long n) {
		if (n < 0) throw new AssertionError();
		int d = a.length;
		for (boolean[] row : a) if (row.length != d) throw new AssertionError();
		boolean[][] sum = new boolean[d][d];
		boolean[][] power = booleanMatrixIdentity(d);
		long count = n;
		boolean[][] blockPower = a;
		boolean[][] blockSum = booleanMatrixIdentity(d);
		while (count != 0) {
			if ((count & 1) != 0) {
				sum = add(sum, mul(power, blockSum));
				power = mul(power, blockPower);
			}
			count >>>= 1;
			if (count != 0) {
				blockSum = add(blockSum, mul(blockPower, blockSum));
				blockPower = mul(blockPower, blockPower);
			}
		}
		return sum;
	}
}
