package library.util.fold;

import library.util.ArrayUtils;

/**
 * 1次元配列を無限に周期複製した配列上での累積和・区間和構造。
 */
// 未テスト
public class CircularDoubleSum {
	final double[] a;
	double[] prefixSum;
	int N;

	/**
	 * 指定された double 配列で CircularDoubleSum を構築します。
	 *
	 * @param a 入力配列
	 * O(N)
	 */
	// 未テスト
	public CircularDoubleSum(double[] a) {
		this.a = ArrayUtils.copy(a);
		this.N = a.length;
	}

	/**
	 * 指定された int 配列で CircularDoubleSum を構築します。
	 *
	 * @param a 入力配列
	 * O(N)
	 */
	// 未テスト
	public CircularDoubleSum(int[] a) {
		this.a = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			this.a[i] = a[i];
		}
		this.N = a.length;
	}

	/**
	 * 周期位置の要素を取得します。
	 *
	 * @param id インデックス
	 * @return 要素
	 * O(1)
	 */
	// 未テスト
	public double get(int id) {
		id = id % a.length;
		if (id < 0) id += a.length;
		return a[id];
	}

	/**
	 * a（入力配列）を無限個並べた配列上での半開区間 [leftInclusive, rightExclusive) の累積和を返します。
	 *
	 * @param leftInclusive 開始インデックス（包含）
	 * @param rightExclusive 終了インデックス（除外）
	 * @return 区間和
	 * O(1) （構築後） / O(N) （初回構築時）
	 */
	// 未テスト
	public double sum(long leftInclusive, long rightExclusive) {
		if (leftInclusive > rightExclusive) throw new IllegalArgumentException("leftInclusive (" + leftInclusive + ") > rightExclusive (" + rightExclusive + ")");
		if (prefixSum == null) build();
		long len = rightExclusive - leftInclusive;
		long q = len / N;
		leftInclusive = (leftInclusive % N + N) % N;
		rightExclusive = leftInclusive + len % N;
		return prefixSum[N - 1] * q + (rightExclusive == 0 ? 0 : prefixSum[(int) rightExclusive - 1]) - (leftInclusive == 0 ? 0 : prefixSum[(int) leftInclusive - 1]);
	}

	private void build() {
		this.prefixSum = ArrayUtils.prefixSum(ArrayUtils.concat(a, a));
	}

	/**
	 * 内部状態を標準出力に出力します。
	 *
	 * O(N)
	 */
	// 未テスト
	public void dump() {
		System.out.println("CircularDoubleSum { a: " + java.util.Arrays.toString(a) + ", prefixSum: " + (prefixSum == null ? "null" : java.util.Arrays.toString(prefixSum)) + " }");
	}
}
