package library.util.segtree;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

import library.util.ArrayUtils;
import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;
import library.util.algebra.strategy.longs.LongGroupStrategy;
import library.util.collections.LongArrayList;
import library.util.seq.SortedArrays;

/**
 * 2次元座標圧縮二次元 Binary Indexed Tree (Fenwick Tree)。
 *
 * 各外側 BIT ノードに必要な座標だけを持たせることで、空間計算量 O(N log N) を達成する。
 * 点更新と長方形領域の和取得を O(log N log N) で行う。
 *
 * <p>契約:</p>
 * <ul>
 *   <li>事前条件: 使用する座標 (x, y) は事前に registerKey または register によって登録されている必要がある。</li>
 *   <li>計算量: 構築に O(N log N)、一点作用・領域クエリに O(log N log N)</li>
 * </ul>
 */
// 未テスト
public class CompressedSparseBinaryIndexedTree2D {
	//https://atcoder.jp/contests/abc266/submissions/78070059
	/**
	 * 作用させる二項演算（アーベル群または可換モノイドの演算）
	 */
	private final LongBinaryOperator op;
	/**
	 * 逆元（可換モノイドの場合は null になることがある）
	 */
	private final LongUnaryOperator inv;
	/**
	 * 単位元
	 */
	private final long identity;

	/**
	 * 登録された一意なX座標のソート済み配列
	 */
	private long[] xs;
	/**
	 * 各外側 BIT ノード (1-indexed) に対応する、登録された一意なY座標のソート済み配列
	 */
	private long[][] ys;
	/**
	 * 各外側 BIT ノードにおける 1D BIT データの配列
	 */
	private long[][] tree;
	/**
	 * 構築が完了したかを表すフラグ
	 */
	private boolean built = false;

	/**
	 * 事前登録されたX座標のリスト
	 */
	private final LongArrayList regX = new LongArrayList();
	/**
	 * 事前登録されたY座標 of リスト
	 */
	private final LongArrayList regY = new LongArrayList();

	/**
	 * 可換モノイドの戦略を用いて 2次元 圧縮 BIT を構築する。
	 *
	 * @param strategy 可換モノイドの戦略
	 */
	public CompressedSparseBinaryIndexedTree2D(LongCommutativeMonoidStrategy strategy) {
		this.op = strategy::mul;
		this.inv = strategy instanceof LongGroupStrategy ? ((LongGroupStrategy) strategy)::inverse : null;
		this.identity = strategy.identity();
	}

	/**
	 * アーベル群の戦略を用いて 2次元 圧縮 BIT を構築する。
	 *
	 * @param strategy アーベル群の戦略
	 */
	public CompressedSparseBinaryIndexedTree2D(LongAbelianGroupStrategy strategy) {
		this.op = strategy::mul;
		this.inv = strategy::inverse;
		this.identity = strategy.identity();
	}

	/**
	 * 指定された演算を用いて 2次元 圧縮 BIT を構築する。
	 *
	 * @param op 二項演算
	 * @param inv 逆元（逆元が存在しない場合は null）
	 * @param identity 単位元
	 */
	public CompressedSparseBinaryIndexedTree2D(LongBinaryOperator op, LongUnaryOperator inv, long identity) {
		this.op = op;
		this.inv = inv;
		this.identity = identity;
	}

	/**
	 * 更新を作用させる座標 (x, y) を事前に登録する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(1)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 */
	public void registerKey(long x, long y) {
		regX.add(x);
		regY.add(y);
	}

	/**
	 * 登録された座標情報に基づいて 2次元 圧縮 BIT を構築する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: 登録済みの座標が 1 つ以上存在することが望ましい。</li>
	 *   <li>計算量: O(N log N) (N は登録された座標の個数)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 */
	public void build() {
		if (built) return;
		long[] allX = regX.toArray();
		this.xs = ArrayUtils.sortq(allX);
		int H = xs.length;

		LongArrayList[] tempYs = new LongArrayList[H + 1];
		for (int i = 0; i <= H; i++) {
			tempYs[i] = new LongArrayList();
		}

		int N = regX.size();
		for (int p = 0; p < N; p++) {
			long x = regX.get(p);
			long y = regY.get(p);
			int i = SortedArrays.indexOf(xs, x) + 1;
			for (int idx = i; idx <= H; idx += idx & -idx) {
				tempYs[idx].add(y);
			}
		}

		this.ys = new long[H + 1][];
		this.tree = new long[H + 1][];
		for (int idx = 1; idx <= H; idx++) {
			long[] sortedY = ArrayUtils.sortq(tempYs[idx].toArray());
			this.ys[idx] = sortedY;
			this.tree[idx] = new long[sortedY.length + 1];
			if (identity != 0L) {
				Arrays.fill(this.tree[idx], identity);
			}
		}
		this.built = true;
	}

	/**
	 * 座標 (x, y) に値 val を作用させる。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: 座標 (x, y) が事前に登録され、build() が完了していること。</li>
	 *   <li>計算量: O(log N log N)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @param val 作用させる値
	 */
	public void add(long x, long y, long val) {
		if (!built) throw new AssertionError("Not built yet.");
		int i = SortedArrays.indexOf(xs, x) + 1;
		if (i == 0) throw new AssertionError("x coordinate is not registered: " + x);
		int H = xs.length;
		for (int idx = i; idx <= H; idx += idx & -idx) {
			int j = SortedArrays.indexOf(ys[idx], y) + 1;
			if (j == 0) throw new AssertionError("y coordinate is not registered at outer BIT node " + idx + ": " + y);
			int lenY = ys[idx].length;
			for (int jdx = j; jdx <= lenY; jdx += jdx & -jdx) {
				tree[idx][jdx] = op.applyAsLong(tree[idx][jdx], val);
			}
		}
	}

	/**
	 * 半開区間 (-inf, x) x (-inf, y) の総和を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log N log N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param x X座標の上限 (exclusive)
	 * @param y Y座標の上限 (exclusive)
	 * @return 領域内の総和
	 */
	public long prefixSum(long x, long y) {
		if (!built) throw new AssertionError("Not built yet.");
		long res = identity;
		int i = SortedArrays.ceil(xs, x);
		for (int idx = i; idx > 0; idx -= idx & -idx) {
			int j = SortedArrays.ceil(ys[idx], y);
			for (int jdx = j; jdx > 0; jdx -= jdx & -jdx) {
				res = op.applyAsLong(res, tree[idx][jdx]);
			}
		}
		return res;
	}

	/**
	 * 矩形領域 [lx, rx) x [ly, ry) の総和を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log N log N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param lx 最小のX座標 (inclusive)
	 * @param ly 最小 of Y座標 (inclusive)
	 * @param rx 最大のX座標 (exclusive)
	 * @param ry 最大のY座標 (exclusive)
	 * @return 領域内の総和
	 */
	public long sum(long lx, long ly, long rx, long ry) {
		if (!built) throw new AssertionError("Not built yet.");
		if (inv == null) throw new UnsupportedOperationException("sum is not supported because inverse operation is not provided.");
		if (lx >= rx || ly >= ry) return identity;
		long res = prefixSum(rx, ry);
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(lx, ry)));
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(rx, ly)));
		res = op.applyAsLong(res, prefixSum(lx, ly));
		return res;
	}

	/**
	 * 指定した座標 (x, y) の現在の値を取得する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log N log N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @return 座標 (x, y) の値
	 */
	public long get(long x, long y) {
		return sum(x, y, x + 1, y + 1);
	}

	/**
	 * 全ての要素を identity で初期化する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(N log N) (N は登録された座標 of 個数)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 */
	public void clear() {
		if (built) {
			for (int idx = 1; idx < tree.length; idx++) {
				Arrays.fill(tree[idx], identity);
			}
		}
	}

	/**
	 * 内部状態を標準出力に出力する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(N log N) (N は登録された座標の個数)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 */
	// 未テスト
	public void dump() {
		System.out.println("CompressedSparseBinaryIndexedTree2D { xs: " + Arrays.toString(xs) + ", ys: " + Arrays.deepToString(ys) + ", tree: " + Arrays.deepToString(tree) + " }");
	}
}
