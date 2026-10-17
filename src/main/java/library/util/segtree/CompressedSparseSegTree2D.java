package library.util.segtree;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;

import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;
import library.util.ArrayUtils;
import library.util.collections.LongArrayList;
import library.util.seq.SortedArrays;

/**
 * 2次元座標圧縮 Segment Tree。
 *
 * <p>
 * 更新される座標 (x, y) を事前に登録し、各外側 Segment Tree ノードに
 * 必要な y 座標だけを保持する。
 * </p>
 *
 * <p>
 * 可換モノイドに対して、点更新と矩形領域の集約を
 * O(log^2 N) で行う。
 * </p>
 *
 * <p>契約:</p>
 * <ul>
 *   <li>更新する座標は build() より前に registerKey() で登録する。</li>
 *   <li>build() 後は新しい座標を登録できない。</li>
 *   <li>空間計算量: O(N log N)</li>
 *   <li>構築: O(N log N)</li>
 *   <li>一点作用: O(log^2 N)</li>
 *   <li>矩形クエリ: O(log^2 N)</li>
 * </ul>
 */
// 未テスト
public class CompressedSparseSegTree2D {

	/** 可換モノイドの演算 */
	private final LongBinaryOperator op;

	/** 単位元 */
	private final long identity;

	/** 登録された一意な x 座標 */
	private long[] xs;

	/**
	 * 外側 Segment Tree の各ノードに存在する一意な y 座標。
	 *
	 * ys[v] は昇順。
	 */
	private long[][] ys;

	/**
	 * 各外側ノードに対応する内側 Segment Tree。
	 */
	private long[][] tree;

	/**
	 * 各内側 Segment Tree の葉数。
	 */
	private int[] innerSize;

	/**
	 * 外側 Segment Tree の葉数。
	 */
	private int outerSize;

	private boolean built = false;

	private final LongArrayList regX = new LongArrayList();
	private final LongArrayList regY = new LongArrayList();

	/**
	 * 可換モノイドを用いて構築する。
	 *
	 * @param strategy 可換モノイド
	 */
	public CompressedSparseSegTree2D(
			LongCommutativeMonoidStrategy strategy) {
		this.op = strategy::mul;
		this.identity = strategy.identity();
	}

	/**
	 * 指定された可換演算を用いて構築する。
	 *
	 * @param op 可換かつ結合的な二項演算
	 * @param identity 単位元
	 */
	public CompressedSparseSegTree2D(
			LongBinaryOperator op, long identity) {
		this.op = op;
		this.identity = identity;
	}

	/**
	 * 更新する座標 (x, y) を事前登録する。
	 *
	 * @param x x 座標
	 * @param y y 座標
	 */
	public void registerKey(long x, long y) {
		if (built)
			throw new AssertionError("Already built.");
		regX.add(x);
		regY.add(y);
	}

	/**
	 * registerKey() の別名。
	 */
	public void register(long x, long y) {
		registerKey(x, y);
	}

	/**
	 * 登録された座標からデータ構造を構築する。
	 *
	 * <p>
	 * 各 x に対応する葉で y 座標を sort/unique し、
	 * 内部ノードでは左右の y 座標列をマージする。
	 * </p>
	 *
	 * <p>計算量: O(N log N)</p>
	 */
	public void build() {
		if (built)
			return;

		xs = ArrayUtils.sortq(regX.toArray());

		int H = xs.length;

		outerSize = 1;
		while (outerSize < H)
			outerSize <<= 1;

		ys = new long[outerSize << 1][];

		LongArrayList[] leafYs = new LongArrayList[H];

		for (int p = 0; p < regX.size(); p++) {
			int i = Arrays.binarySearch(xs, regX.get(p));
			if (leafYs[i] == null)
				leafYs[i] = new LongArrayList();
			leafYs[i].add(regY.get(p));
		}

		// leaves
		for (int i = 0; i < outerSize; i++) {
			int v = outerSize + i;
			if (i < H && leafYs[i] != null)
				ys[v] = ArrayUtils.sortq(leafYs[i].toArray());
			else
				ys[v] = new long[0];
		}

		// internal nodes
		for (int v = outerSize - 1; v >= 1; v--) {
			ys[v] = SortedArrays.unique(SortedArrays.merge(
					ys[v << 1],
					ys[v << 1 | 1]));
		}

		tree = new long[outerSize << 1][];
		innerSize = new int[outerSize << 1];

		for (int v = 1; v < (outerSize << 1); v++) {
			int s = 1;
			while (s < ys[v].length)
				s <<= 1;

			innerSize[v] = s;
			tree[v] = new long[s << 1];

			if (identity != 0)
				Arrays.fill(tree[v], identity);
		}

		built = true;
	}

	/**
	 * 座標 (x, y) の値に val を作用させる。
	 *
	 * <p>
	 * 現在値を a とすると、新しい値は op(a, val)。
	 * </p>
	 *
	 * @param x x 座標
	 * @param y y 座標
	 * @param val 作用させる値
	 *
	 * <p>計算量: O(log^2 N)</p>
	 */
	public void mul(long x, long y, long val) {
		checkBuilt();

		int xi = Arrays.binarySearch(xs, x);
		if (xi < 0)
			throw new AssertionError(
					"x coordinate is not registered: " + x);

		for (int v = outerSize + xi; v > 0; v >>= 1) {
			int yi = Arrays.binarySearch(ys[v], y);
			if (yi < 0)
				throw new AssertionError(
						"Coordinate (" + x + ", " + y
						+ ") was not registered before build().");
			innerAdd(v, yi, val);
		}
	}

	/**
	 * [lx, rx) × [ly, ry) の集約値を返す。
	 *
	 * <p>
	 * BIT 版とは異なり、逆元を必要としない。
	 * </p>
	 *
	 * @param lx x 座標の下限 inclusive
	 * @param ly y 座標の下限 inclusive
	 * @param rx x 座標の上限 exclusive
	 * @param ry y 座標の上限 exclusive
	 * @return 矩形領域の集約値
	 *
	 * <p>計算量: O(log^2 N)</p>
	 */
	public long fold(long lx, long ly, long rx, long ry) {
		checkBuilt();

		if (lx >= rx || ly >= ry)
			return identity;

		int l = SortedArrays.ceil(xs, lx);
		int r = SortedArrays.ceil(xs, rx);

		return queryX(l, r, ly, ry);
	}

	/**
	 * 指定された座標 (x, y) の現在値を返す。
	 *
	 * <p>
	 * 登録されていない座標なら identity を返す。
	 * </p>
	 *
	 * @param x x 座標
	 * @param y y 座標
	 * @return 現在値
	 *
	 * <p>計算量: O(log N)</p>
	 */
	public long get(long x, long y) {
		checkBuilt();

		int xi = Arrays.binarySearch(xs, x);
		if (xi < 0)
			return identity;

		int v = outerSize + xi;
		int yi = Arrays.binarySearch(ys[v], y);
		if (yi < 0)
			return identity;

		return tree[v][innerSize[v] + yi];
	}

	/**
	 * 全要素を identity に戻す。
	 *
	 * <p>計算量: O(N log N)</p>
	 */
	public void clear() {
		if (!built)
			return;

		for (int v = 1; v < tree.length; v++) {
			Arrays.fill(tree[v], identity);
		}
	}

	/**
	 * 内部状態を出力する。
	 */
	// 未テスト
	public void dump() {
		System.out.println(
				"CompressedSparseSegmentTree2D {"
				+ " xs=" + Arrays.toString(xs)
				+ ", ys=" + Arrays.deepToString(ys)
				+ ", tree=" + Arrays.deepToString(tree)
				+ " }");
	}

	/* ---------------- internal ---------------- */

	private void innerAdd(int v, int i, long val) {
		int p = innerSize[v] + i;

		tree[v][p] = op.applyAsLong(tree[v][p], val);

		for (p >>= 1; p > 0; p >>= 1) {
			tree[v][p] = op.applyAsLong(
					tree[v][p << 1],
					tree[v][p << 1 | 1]);
		}
	}

	private long queryX(
			int l, int r,
			long ly, long ry) {

		long res = identity;

		l += outerSize;
		r += outerSize;

		while (l < r) {
			if ((l & 1) != 0)
				res = op.applyAsLong(
						res, innerQuery(l++, ly, ry));

			if ((r & 1) != 0)
				res = op.applyAsLong(
						res, innerQuery(--r, ly, ry));

			l >>= 1;
			r >>= 1;
		}

		return res;
	}

	private long innerQuery(int v, long ly, long ry) {
		int l = SortedArrays.ceil(ys[v], ly);
		int r = SortedArrays.ceil(ys[v], ry);

		l += innerSize[v];
		r += innerSize[v];

		long res = identity;

		while (l < r) {
			if ((l & 1) != 0)
				res = op.applyAsLong(res, tree[v][l++]);

			if ((r & 1) != 0)
				res = op.applyAsLong(res, tree[v][--r]);

			l >>= 1;
			r >>= 1;
		}

		return res;
	}

	private void checkBuilt() {
		if (!built)
			throw new AssertionError("Not built yet.");
	}
}