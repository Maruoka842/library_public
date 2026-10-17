package library.util.segtree;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * 3次元 Binary Indexed Tree (Fenwick Tree)。
 * 点更新と直方体領域の集約を O(log H log W log D) で行う。
 *
 * <p>演算はアーベル群である必要がある。</p>
 *
 * <p>メモリは O(HWD)。</p>
 */
public class BinaryIndexedTree3D {
	private final long[][][] a;
	private final LongBinaryOperator op;
	private final LongUnaryOperator inv;
	private final long identity;
	private final int H, W, D;

	/**
	 * HxWxD の 3次元 Binary Indexed Tree を構築する。
	 * 加法群 (long, +, -, 0) を使用する。
	 *
	 * @param H 高さ
	 * @param W 幅
	 * @param D 奥行き
	 */
	public BinaryIndexedTree3D(int H, int W, int D) {
		this(H, W, D, (x, y) -> x + y, x -> -x, 0L);
	}

	/**
	 * HxWxD の 3次元 Binary Indexed Tree を構築する。
	 *
	 * @param H 高さ
	 * @param W 幅
	 * @param D 奥行き
	 * @param op 二項演算（アーベル群の演算である必要がある）
	 * @param inv 逆元
	 * @param identity 単位元
	 */
	public BinaryIndexedTree3D(int H, int W, int D,
			LongBinaryOperator op, LongUnaryOperator inv, long identity) {
		this.H = H;
		this.W = W;
		this.D = D;
		this.a = new long[H + 1][W + 1][D + 1];

		if (identity != 0L) {
			for (int i = 0; i <= H; i++) {
				for (int j = 0; j <= W; j++) {
					Arrays.fill(a[i][j], identity);
				}
			}
		}

		this.op = op;
		this.inv = inv;
		this.identity = identity;
	}

	/**
	 * 座標 (h, w, d) に val を作用させる。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: 0 <= h < H, 0 <= w < W, 0 <= d < D</li>
	 *   <li>事後条件: A[h][w][d] = op(A[h][w][d], val)</li>
	 *   <li>計算量: O(log H log W log D)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 *
	 * @param h 第1座標
	 * @param w 第2座標
	 * @param d 第3座標
	 * @param val 作用させる値
	 */
	public void add(int h, int w, int d, long val) {
		if (h < 0 || h >= H || w < 0 || w >= W || d < 0 || d >= D) return;

		for (int i = h + 1; i <= H; i += i & -i) {
			for (int j = w + 1; j <= W; j += j & -j) {
				for (int k = d + 1; k <= D; k += k & -k) {
					a[i][j][k] = op.applyAsLong(a[i][j][k], val);
				}
			}
		}
	}

	/**
	 * 領域 [0, h) x [0, w) x [0, d) の集約値を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(log H log W log D)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 *
	 * @param h 第1座標の上端 (exclusive)
	 * @param w 第2座標の上端 (exclusive)
	 * @param d 第3座標の上端 (exclusive)
	 * @return 領域の集約値
	 */
	public long prefixSum(int h, int w, int d) {
		long res = identity;

		h = Math.min(h, H);
		w = Math.min(w, W);
		d = Math.min(d, D);

		for (int i = h; i > 0; i -= i & -i) {
			for (int j = w; j > 0; j -= j & -j) {
				for (int k = d; k > 0; k -= k & -k) {
					res = op.applyAsLong(res, a[i][j][k]);
				}
			}
		}
		return res;
	}

	/**
	 * 領域 [h1, h2) x [w1, w2) x [d1, d2) の集約値を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(log H log W log D)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 *
	 * @param h1 第1座標の開始位置 (inclusive)
	 * @param w1 第2座標の開始位置 (inclusive)
	 * @param d1 第3座標の開始位置 (inclusive)
	 * @param h2 第1座標の終了位置 (exclusive)
	 * @param w2 第2座標の終了位置 (exclusive)
	 * @param d2 第3座標の終了位置 (exclusive)
	 * @return 領域の集約値
	 */
	public long sum(int h1, int w1, int d1, int h2, int w2, int d2) {
		if (h1 >= h2 || w1 >= w2 || d1 >= d2) return identity;

		long res = prefixSum(h2, w2, d2);

		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(h1, w2, d2)));
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(h2, w1, d2)));
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(h2, w2, d1)));

		res = op.applyAsLong(res, prefixSum(h1, w1, d2));
		res = op.applyAsLong(res, prefixSum(h1, w2, d1));
		res = op.applyAsLong(res, prefixSum(h2, w1, d1));

		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(h1, w1, d1)));

		return res;
	}

	/**
	 * 指定した座標 (h, w, d) の現在の値を取得する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(log H log W log D)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 *
	 * @param h 第1座標
	 * @param w 第2座標
	 * @param d 第3座標
	 * @return 座標 (h, w, d) の値
	 */
	public long get(int h, int w, int d) {
		return sum(h, w, d, h + 1, w + 1, d + 1);
	}

	/**
	 * 全ての要素を identity で初期化する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(HWD)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 */
	public void clear() {
		for (int i = 0; i <= H; i++) {
			for (int j = 0; j <= W; j++) {
				Arrays.fill(a[i][j], identity);
			}
		}
	}

	/**
	 * 内部状態を標準出力に出力する。
	 *
	 * <ul>
	 *   <li>副作用: 標準出力への出力。</li>
	 *   <li>計算量: O(HWD)</li>
	 *   <li>破壊的変更: なし。</li>
	 * </ul>
	 */
	// 未テスト
	public void dump() {
		System.out.println(
				"BinaryIndexedTree3D { a: " + Arrays.deepToString(a)
				+ ", H: " + H
				+ ", W: " + W
				+ ", D: " + D + " }");
	}
}