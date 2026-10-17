package library.util.segtree;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

import library.util.ArrayUtils;
import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongCommutativeMonoidStrategy;
import library.util.algebra.strategy.longs.LongGroupStrategy;
import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;
import library.util.collections.LongArrayList;
import library.util.seq.SortedArrays;

/**
 * 3次元座標圧縮三次元 Binary Indexed Tree (Fenwick Tree)。
 *
 * 各外側・中間 BIT ノードに必要な座標だけを持たせることで、空間計算量 O(N log^2 N) を達成する。
 * 点更新と長方体領域の和取得を O(log^3 N) で行う。
 *
 * <p>契約:</p>
 * <ul>
 *   <li>事前条件: 使用する座標 (x, y, z) は事前に registerKey または register によって登録されている必要がある。</li>
 *   <li>計算量: 構築に O(N log^2 N)、一点作用・領域クエリに O(log^3 N)</li>
 * </ul>
 */
// 未テスト
public class CompressedSparseBinaryIndexedTree3D {
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
	 * 各外側・中間 BIT ノード (1-indexed) に対応する、登録された一意なZ座標のソート済み配列
	 */
	private long[][][] zs;
	/**
	 * 各外側・中間 BIT ノードにおける 1D BIT データの配列
	 */
	private long[][][] tree;
	/**
	 * 構築が完了したかを表すフラグ
	 */
	private boolean built = false;

	/**
	 * 事前登録されたX座標のリスト
	 */
	private final LongArrayList regX = new LongArrayList();
	/**
	 * 事前登録されたY座標のリスト
	 */
	private final LongArrayList regY = new LongArrayList();
	/**
	 * 事前登録されたZ座標のリスト
	 */
	private final LongArrayList regZ = new LongArrayList();

	/**
	 * 標準的な加法群 (long, +, -, 0) を用いて 3次元 圧縮 BIT を構築する。
	 */
	public CompressedSparseBinaryIndexedTree3D() {
		this(LongAddAbelianGroupStrategy.STRATEGY);
	}

	/**
	 * 可換モノイドの戦略を用いて 3次元 圧縮 BIT を構築する。
	 *
	 * @param strategy 可換モノイドの戦略
	 */
	public CompressedSparseBinaryIndexedTree3D(LongCommutativeMonoidStrategy strategy) {
		this.op = strategy::mul;
		this.inv = strategy instanceof LongGroupStrategy ? ((LongGroupStrategy) strategy)::inverse : null;
		this.identity = strategy.identity();
	}

	/**
	 * アーベル群の戦略を用いて 3次元 圧縮 BIT を構築する。
	 *
	 * @param strategy アーベル群の戦略
	 */
	public CompressedSparseBinaryIndexedTree3D(LongAbelianGroupStrategy strategy) {
		this.op = strategy::mul;
		this.inv = strategy::inverse;
		this.identity = strategy.identity();
	}

	/**
	 * 指定された演算を用いて 3次元 圧縮 BIT を構築する。
	 *
	 * @param op 二項演算
	 * @param inv 逆元（逆元が存在しない場合は null）
	 * @param identity 単位元
	 */
	public CompressedSparseBinaryIndexedTree3D(LongBinaryOperator op, LongUnaryOperator inv, long identity) {
		this.op = op;
		this.inv = inv;
		this.identity = identity;
	}

	/**
	 * 更新を作用させる座標 (x, y, z) を事前に登録する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(1)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @param z Z座標
	 */
	// 未テスト
	public void registerKey(long x, long y, long z) {
		regX.add(x);
		regY.add(y);
		regZ.add(z);
	}

	/**
	 * 更新を作用させる座標 (x, y, z) を事前に登録する。 (registerKey の別名)
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(1)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @param z Z座標
	 */
	// 未テスト
	public void register(long x, long y, long z) {
		registerKey(x, y, z);
	}

	/**
	 * 登録された座標情報に基づいて 3次元 圧縮 BIT を構築する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: 登録済みの座標が 1 つ以上存在することが望ましい。</li>
	 *   <li>計算量: O(N log^2 N) (N は登録された座標の個数)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 */
	// 未テスト
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
		for (int idx = 1; idx <= H; idx++) {
			this.ys[idx] = ArrayUtils.sortq(tempYs[idx].toArray());
		}

		this.zs = new long[H + 1][][];
		this.tree = new long[H + 1][][];
		LongArrayList[][] tempZs = new LongArrayList[H + 1][];
		for (int idx = 1; idx <= H; idx++) {
			int lenY = ys[idx].length;
			tempZs[idx] = new LongArrayList[lenY + 1];
			for (int jdx = 1; jdx <= lenY; jdx++) {
				tempZs[idx][jdx] = new LongArrayList();
			}
		}

		for (int p = 0; p < N; p++) {
			long x = regX.get(p);
			long y = regY.get(p);
			long z = regZ.get(p);
			int i = SortedArrays.indexOf(xs, x) + 1;
			for (int idx = i; idx <= H; idx += idx & -idx) {
				int j = SortedArrays.indexOf(ys[idx], y) + 1;
				int lenY = ys[idx].length;
				for (int jdx = j; jdx <= lenY; jdx += jdx & -jdx) {
					tempZs[idx][jdx].add(z);
				}
			}
		}

		for (int idx = 1; idx <= H; idx++) {
			int lenY = ys[idx].length;
			this.zs[idx] = new long[lenY + 1][];
			this.tree[idx] = new long[lenY + 1][];
			for (int jdx = 1; jdx <= lenY; jdx++) {
				long[] sortedZ = ArrayUtils.sortq(tempZs[idx][jdx].toArray());
				this.zs[idx][jdx] = sortedZ;
				this.tree[idx][jdx] = new long[sortedZ.length + 1];
				if (identity != 0L) {
					Arrays.fill(this.tree[idx][jdx], identity);
				}
			}
		}
		this.built = true;
	}

	/**
	 * 座標 (x, y, z) に値 val を作用させる。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: 座標 (x, y, z) が事前に登録され、build() が完了していること。</li>
	 *   <li>計算量: O(log^3 N)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @param z Z座標
	 * @param val 作用させる値
	 */
	// 未テスト
	public void add(long x, long y, long z, long val) {
		if (!built) throw new AssertionError("Not built yet.");
		int i = SortedArrays.indexOf(xs, x) + 1;
		if (i == 0) throw new AssertionError("x coordinate is not registered: " + x);
		int H = xs.length;
		for (int idx = i; idx <= H; idx += idx & -idx) {
			int j = SortedArrays.indexOf(ys[idx], y) + 1;
			if (j == 0) throw new AssertionError("y coordinate is not registered at outer BIT node " + idx + ": " + y);
			int lenY = ys[idx].length;
			for (int jdx = j; jdx <= lenY; jdx += jdx & -jdx) {
				int k = SortedArrays.indexOf(zs[idx][jdx], z) + 1;
				if (k == 0) throw new AssertionError("z coordinate is not registered at middle BIT node " + jdx + " under outer node " + idx + ": " + z);
				int lenZ = zs[idx][jdx].length;
				for (int kdx = k; kdx <= lenZ; kdx += kdx & -kdx) {
					tree[idx][jdx][kdx] = op.applyAsLong(tree[idx][jdx][kdx], val);
				}
			}
		}
	}

	/**
	 * 半開区間 (-inf, x) x (-inf, y) x (-inf, z) の総和を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log^3 N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param x X座標の上限 (exclusive)
	 * @param y Y座標の上限 (exclusive)
	 * @param z Z座標の上限 (exclusive)
	 * @return 領域内の総和
	 */
	// 未テスト
	public long prefixSum(long x, long y, long z) {
		if (!built) throw new AssertionError("Not built yet.");
		long res = identity;
		int i = SortedArrays.ceil(xs, x);
		for (int idx = i; idx > 0; idx -= idx & -idx) {
			int j = SortedArrays.ceil(ys[idx], y);
			for (int jdx = j; jdx > 0; jdx -= jdx & -jdx) {
				int k = SortedArrays.ceil(zs[idx][jdx], z);
				for (int kdx = k; kdx > 0; kdx -= kdx & -kdx) {
					res = op.applyAsLong(res, tree[idx][jdx][kdx]);
				}
			}
		}
		return res;
	}

	/**
	 * 直方体領域 [lx, rx) x [ly, ry) x [lz, rz) の総和を返す。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log^3 N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param lx 最小のX座標 (inclusive)
	 * @param ly 最小のY座標 (inclusive)
	 * @param lz 最小のZ座標 (inclusive)
	 * @param rx 最大のX座標 (exclusive)
	 * @param ry 最大のY座標 (exclusive)
	 * @param rz 最大のZ座標 (exclusive)
	 * @return 領域内の総和
	 */
	// 未テスト
	public long sum(long lx, long ly, long lz, long rx, long ry, long rz) {
		if (!built) throw new AssertionError("Not built yet.");
		if (inv == null) throw new UnsupportedOperationException("sum is not supported because inverse operation is not provided.");
		if (lx >= rx || ly >= ry || lz >= rz) return identity;
		long res = prefixSum(rx, ry, rz);
		// 1つの l を含む項の減算
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(lx, ry, rz)));
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(rx, ly, rz)));
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(rx, ry, lz)));
		// 2つの l を含む項の加算
		res = op.applyAsLong(res, prefixSum(lx, ly, rz));
		res = op.applyAsLong(res, prefixSum(lx, ry, lz));
		res = op.applyAsLong(res, prefixSum(rx, ly, lz));
		// 3つの l を含む項の減算
		res = op.applyAsLong(res, inv.applyAsLong(prefixSum(lx, ly, lz)));
		return res;
	}

	/**
	 * 指定した座標 (x, y, z) の現在の値を取得する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>事前条件: build() が完了していること。</li>
	 *   <li>計算量: O(log^3 N)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 * @param x X座標
	 * @param y Y座標
	 * @param z Z座標
	 * @return 座標 (x, y, z) の値
	 */
	// 未テスト
	public long get(long x, long y, long z) {
		return sum(x, y, z, x + 1, y + 1, z + 1);
	}

	/**
	 * 全ての要素を identity で初期化する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(N log^2 N) (N は登録された座標の個数)</li>
	 *   <li>破壊的変更: あり</li>
	 * </ul>
	 */
	// 未テスト
	public void clear() {
		if (built) {
			for (int idx = 1; idx < tree.length; idx++) {
				for (int jdx = 1; jdx < tree[idx].length; jdx++) {
					Arrays.fill(tree[idx][jdx], identity);
				}
			}
		}
	}

	/**
	 * 内部状態を標準出力に出力する。
	 *
	 * <p>契約:</p>
	 * <ul>
	 *   <li>計算量: O(N log^2 N) (N は登録された座標の個数)</li>
	 *   <li>破壊的変更: なし</li>
	 * </ul>
	 */
	// 未テスト
	public void dump() {
		System.out.println("CompressedSparseBinaryIndexedTree3D { xs: " + Arrays.toString(xs) + ", ys: " + Arrays.deepToString(ys) + ", zs: " + Arrays.deepToString(zs) + ", tree: " + Arrays.deepToString(tree) + " }");
	}
}
