package library.util.fold;

import library.util.ArrayUtils;

/**
 * 2次元配列を無限に周期複製したグリッド上での累積和・2次元矩形和構造。
 */
// 未テスト
public class CircularDoubleSum2D {

	final double[][] a;
	double[][] prefixSum;
	int N;
	int M;

	/**
	 * 指定された double 2次元配列で CircularDoubleSum2D を構築します。
	 *
	 * @param a 入力2次元配列
	 * O(NM)
	 */
	// 未テスト
	public CircularDoubleSum2D(double[][] a) {
		this.a = ArrayUtils.copy(a);
		this.N = a.length;
		this.M = a[0].length;
	}

	/**
	 * 指定された int 2次元配列で CircularDoubleSum2D を構築します。
	 *
	 * @param a 入力2次元配列
	 * O(NM)
	 */
	// 未テスト
	public CircularDoubleSum2D(int[][] a) {
		this.a = new double[a.length][a[0].length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				this.a[i][j] = a[i][j];
			}
		}
		this.N = a.length;
		this.M = a[0].length;
	}

	/**
	 * a（入力配列）を無限個並べたグリッド上での矩形領域 [x0, x1) × [y0, y1) の和を返します。
	 *
	 * @param x0 行開始インデックス（包含）
	 * @param y0 列開始インデックス（包含）
	 * @param x1 行終了インデックス（除外）
	 * @param y1 列終了インデックス（除外）
	 * @return 矩形和
	 * O(1) （構築後） / O(NM) （初回構築時）
	 */
	// 未テスト
	public double sum(long x0, long y0, long x1, long y1) {
		if (x1 - x0 <= 0 || y1 - y0 <= 0) return 0;
		if (prefixSum == null) {
			this.prefixSum = new double[2 * N][2 * M];
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < M; j++) {
					this.prefixSum[i][j] = a[i][j];
					this.prefixSum[i][j + M] = a[i][j];
					this.prefixSum[i + N][j] = a[i][j];
					this.prefixSum[i + N][j + M] = a[i][j];
				}
			}
			this.prefixSum = ArrayUtils.prefixSum(prefixSum);
		}
		long lenX = x1 - x0;
		long lenY = y1 - y0;
		long qX = lenX / N;
		long qY = lenY / M;
		x0 = (x0 % N + N) % N;
		y0 = (y0 % M + M) % M;
		x1 = x0 + lenX % N;
		y1 = y0 + lenY % M;
		double ret = prefixSum[N - 1][M - 1] * qX * qY;
		ret += smallsum(x0 % N, y0 % M, x0 % N + lenX % N, y0 % M + lenY % M);
		ret += smallsum(0, y0 % M, N, y0 % M + lenY % M) * qX;
		ret += smallsum(x0 % N, 0, x0 % N + lenX % N, M) * qY;
		return ret;
	}

	private double smallsum(long x0, long y0, long x1, long y1) {
		if (x1 == 0 || y1 == 0) return 0;
		double ret = prefixSum[(int) x1 - 1][(int) y1 - 1];
		if (x0 != 0) ret -= prefixSum[(int) x0 - 1][(int) y1 - 1];
		if (y0 != 0) ret -= prefixSum[(int) x1 - 1][(int) y0 - 1];
		if (x0 != 0 && y0 != 0) ret += prefixSum[(int) x0 - 1][(int) y0 - 1];
		return ret;
	}

	/**
	 * 内部状態を標準出力に出力します。
	 *
	 * O(NM)
	 */
	// 未テスト
	public void dump() {
		System.out.println("CircularDoubleSum2D { a: " + java.util.Arrays.deepToString(a) + ", prefixSum: " + (prefixSum == null ? "null" : java.util.Arrays.deepToString(prefixSum)) + " }");
	}
}
