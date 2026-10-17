package library.util.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

/**
 * {@code double} 型に特殊化した動的配列。
 *
 * <p>要素数が配列長に対して小さくなっても、配列を縮小再確保しません。</p>
 */
// 未テスト
public class DoubleArrayList implements Iterable<Double> {
	int defaultCapacity = 16;
	double[] a;
	public int tail = 0;

	/**
	 * 初期容量16で DoubleArrayList を構築します。
	 *
	 * O(1)
	 */
	// 未テスト
	public DoubleArrayList() {
		a = new double[defaultCapacity];
	}

	/**
	 * 指定された初期容量で DoubleArrayList を構築します。
	 *
	 * @param initialCapacity 初期容量
	 * O(initialCapacity)
	 */
	// 未テスト
	public DoubleArrayList(int initialCapacity) {
		a = new double[initialCapacity];
	}

	/**
	 * リストを空にします。
	 *
	 * O(1)
	 */
	// 未テスト
	public void clear() {
		tail = 0;
	}

	/**
	 * 先頭要素を取得します。
	 *
	 * @return 先頭要素
	 * O(1)
	 */
	// 未テスト
	public double peekFirst() {
		if (tail == 0) throw new NoSuchElementException();
		return a[0];
	}

	/**
	 * 末尾要素を取得します。
	 *
	 * @return 末尾要素
	 * O(1)
	 */
	// 未テスト
	public double peekLast() {
		if (tail == 0) throw new NoSuchElementException();
		return a[tail - 1];
	}

	/**
	 * 末尾要素を取得します。空の場合はデフォルト値を返します。
	 *
	 * @param defaultValue デフォルト値
	 * @return 末尾要素、またはデフォルト値
	 * O(1)
	 */
	// 未テスト
	public double peekLastOrDefault(double defaultValue) {
		if (tail == 0) return defaultValue;
		return a[tail - 1];
	}

	/**
	 * 末尾に要素を追加します。
	 *
	 * @param v 追加する要素
	 * 均し O(1)
	 */
	// 未テスト
	public void add(double v) {
		if (tail == a.length) resize(Math.max(1, 2 * a.length));
		a[tail] = v;
		tail++;
	}

	/**
	 * 指定されたリストの全要素を追加します。
	 *
	 * @param list 追加する要素を含むリスト
	 * O(M) (M は追加する要素数)
	 */
	// 未テスト
	public void addAll(DoubleArrayList list) {
		ensureCapacity(tail + list.tail);
		System.arraycopy(list.a, 0, a, tail, list.tail);
		tail += list.tail;
	}

	/**
	 * 指定された配列の全要素を追加します。
	 *
	 * @param values 追加する要素の配列
	 * O(M) (M は追加する要素数)
	 */
	// 未テスト
	public void addAll(double[] values) {
		ensureCapacity(tail + values.length);
		System.arraycopy(values, 0, a, tail, values.length);
		tail += values.length;
	}

	/**
	 * 末尾の要素を取り出して削除します。
	 *
	 * @return 削除された末尾要素
	 * O(1)
	 */
	// 未テスト
	public double pollLast() {
		if (tail == 0) throw new NoSuchElementException();
		double ret = a[tail - 1];
		tail--;
		return ret;
	}

	/**
	 * リストが空かどうか判定します。
	 *
	 * @return 空であれば true、そうでなければ false
	 * O(1)
	 */
	// 未テスト
	public boolean isEmpty() {
		return tail == 0;
	}

	/**
	 * リストが空でないか判定します。
	 *
	 * @return 空でなければ true、空であれば false
	 * O(1)
	 */
	// 未テスト
	public boolean isNonEmpty() {
		return tail != 0;
	}

	/**
	 * 指定インデックスの要素を取得します。
	 *
	 * @param id インデックス
	 * @return 要素
	 * O(1)
	 */
	// 未テスト
	public double get(int id) {
		if (id < 0 || id >= tail) throw new IndexOutOfBoundsException("get(" + id + ")は添え字0以上" + (tail - 1) + "以下に違反");
		return a[id];
	}

	/**
	 * 指定インデックスの要素を取得します。範囲外の場合はデフォルト値を返します。
	 *
	 * @param id インデックス
	 * @param defaultValue デフォルト値
	 * @return 要素、またはデフォルト値
	 * O(1)
	 */
	// 未テスト
	public double getOrDefault(int id, double defaultValue) {
		if (id < 0 || id >= tail) return defaultValue;
		return a[id];
	}

	/**
	 * 指定インデックスの要素を上書きします。
	 *
	 * @param id インデックス
	 * @param value 新しい値
	 * O(1)
	 */
	// 未テスト
	public void set(int id, double value) {
		if (id < 0 || id >= tail) throw new IndexOutOfBoundsException();
		a[id] = value;
	}

	void resize(int size) {
		a = Arrays.copyOf(a, size);
	}

	void ensureCapacity(int capacity) {
		if (capacity <= a.length) return;
		int size = Math.max(1, a.length);
		while (size < capacity) size *= 2;
		resize(size);
	}

	/**
	 * 要素数を返します。
	 *
	 * @return 要素数
	 * O(1)
	 */
	// 未テスト
	public int size() {
		return tail;
	}

	/**
	 * 配列に変換して返します。
	 *
	 * @return 要素の配列
	 * O(N)
	 */
	// 未テスト
	public double[] toArray() {
		return Arrays.copyOf(a, tail);
	}

	/**
	 * 2つのインデックスの要素を入れ替えます。
	 *
	 * @param i インデックス1
	 * @param j インデックス2
	 * O(1)
	 */
	// 未テスト
	public void swap(int i, int j) {
		if (i == j) return;
		double tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	/**
	 * 要素の並びを反転します。
	 *
	 * O(N)
	 */
	// 未テスト
	public void reverse() {
		int s = 0;
		int t = tail - 1;
		while (s < t) {
			double tmp = a[s];
			a[s] = a[t];
			a[t] = tmp;
			++s;
			--t;
		}
	}

	/**
	 * イテレータを取得します。
	 *
	 * @return PrimitiveIterator.OfDouble イテレータ
	 * O(1)
	 */
	// 未テスト
	@Override
	public PrimitiveIterator.OfDouble iterator() {
		return new PrimitiveIterator.OfDouble() {
			int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < tail;
			}

			@Override
			public double nextDouble() {
				if (!hasNext()) throw new NoSuchElementException();
				return get(idx++);
			}
		};
	}

	/**
	 * 要素を昇順にソートします。
	 *
	 * O(N log N)
	 */
	// 未テスト
	public void sort() {
		Arrays.sort(a, 0, tail);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof DoubleArrayList that)) return false;
		if (tail != that.tail) return false;
		for (int i = 0; i < tail; i++) {
			if (Double.compare(a[i], that.a[i]) != 0) return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = 0; i < tail; i++) {
			result = 31 * result + Double.hashCode(a[i]);
		}
		return result;
	}

	@Override
	public String toString() {
		return Arrays.toString(toArray());
	}

	/**
	 * 現在の状態をコピーした新しいインスタンスを返します。
	 *
	 * @return コピーされた DoubleArrayList
	 * O(N)
	 */
	// 未テスト
	public DoubleArrayList copy() {
		DoubleArrayList ret = new DoubleArrayList(a.length);
		ret.tail = this.tail;
		ret.defaultCapacity = this.defaultCapacity;
		System.arraycopy(this.a, 0, ret.a, 0, this.a.length);
		return ret;
	}

	/**
	 * リストの内容を標準出力に出力します。
	 *
	 * O(N)
	 */
	// 未テスト
	public void dump() {
		System.out.println(toString());
	}
}
