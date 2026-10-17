package library.util.collections;

import java.util.Arrays;

/**
 * 整数全射域 [0, n) 上の有限集合 S を管理する Sparse Set。
 */
public class IntSparseSet {

	/** 各要素の dense 配列内インデックスを保持する配列 */
	private final int[] sparse;
	/** 集合に含まれる要素を保持する配列 */
	private final int[] dense;
	/** 現在の集合の要素数 */
	private int size;

	/**
	 * 値域 [0, n) を扱う空集合 S を構築する。
	 *
	 * 計算量: O(n)
	 *
	 * @param n 値域の大きさ
	 */
	// 未テスト
	public IntSparseSet(int n) {
		if (n < 0)
			throw new IllegalArgumentException("n must be non-negative: " + n);

		sparse = new int[n];
		dense = new int[n];
	}

	/**
	 * 要素 x (0 <= x < capacity()) が集合 S に含まれるかを判定する。
	 *
	 * 計算量: O(1)
	 *
	 * @param x 判定対象の要素
	 * @return x が集合に含まれていれば true、そうでなければ false
	 */
	// 未テスト
	public boolean contains(int x) {
		checkRange(x);
		int i = sparse[x];
		return i < size && dense[i] == x;
	}

	/**
	 * 要素 x (0 <= x < capacity()) を集合 S に追加する。
	 *
	 * 計算量: O(1)
	 *
	 * @param x 追加する要素
	 * @return 新しく追加された場合 true、既に存在していた場合 false
	 */
	// 未テスト
	public boolean add(int x) {
		checkRange(x);

		int i = sparse[x];
		if (i < size && dense[i] == x)
			return false;

		sparse[x] = size;
		dense[size++] = x;
		return true;
	}

	/**
	 * 要素 x (0 <= x < capacity()) を集合 S から削除する。
	 *
	 * 計算量: O(1)
	 *
	 * @param x 削除する要素
	 * @return 削除された場合 true、存在しなかった場合 false
	 */
	// 未テスト
	public boolean remove(int x) {
		checkRange(x);

		int i = sparse[x];
		if (i >= size || dense[i] != x)
			return false;

		int y = dense[--size];
		dense[i] = y;
		sparse[y] = i;

		return true;
	}

	/**
	 * 集合を空集合にする。
	 *
	 * 計算量: O(1)
	 */
	// 未テスト
	public void clear() {
		size = 0;
	}

	/**
	 * 現在の集合の要素数を返す。
	 *
	 * 計算量: O(1)
	 *
	 * @return 要素数
	 */
	// 未テスト
	public int size() {
		return size;
	}

	/**
	 * 集合が空かどうかを判定する。
	 *
	 * 計算量: O(1)
	 *
	 * @return 要素数が 0 であれば true、そうでなければ false
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 内部列挙順における i 番目の要素を返す。
	 *
	 * 計算量: O(1)
	 *
	 * @param i インデックス (0 <= i < size())
	 * @return i 番目の要素
	 */
	// 未テスト
	public int get(int i) {
		if (i < 0 || i >= size)
			throw new IndexOutOfBoundsException("i=" + i + ", size=" + size);
		return dense[i];
	}

	/**
	 * 現在集合に含まれる全要素を含む配列を返す。順序は保証されない。
	 *
	 * 計算量: O(size())
	 *
	 * @return 要素配列
	 */
	// 未テスト
	public int[] toArray() {
		return Arrays.copyOf(dense, size);
	}

	/**
	 * 扱える要素の値域の大きさ n (x < capacity()) を返す。
	 *
	 * 計算量: O(1)
	 *
	 * @return 値域の大きさ
	 */
	// 未テスト
	public int capacity() {
		return dense.length;
	}

	/**
	 * この集合の独立なコピーを作成して返す。
	 *
	 * 計算量: O(capacity())
	 *
	 * @return この集合と等しい状態を持つ新しい IntSparseSet
	 */
	// 未テスト
	public IntSparseSet copy() {
		IntSparseSet res = new IntSparseSet(dense.length);
		res.size = this.size;
		System.arraycopy(this.sparse, 0, res.sparse, 0, this.sparse.length);
		System.arraycopy(this.dense, 0, res.dense, 0, this.dense.length);
		return res;
	}

	private void checkRange(int x) {
		if (x < 0 || x >= dense.length)
			throw new IndexOutOfBoundsException(
					"x=" + x + ", range=[0, " + dense.length + ")");
	}
}
