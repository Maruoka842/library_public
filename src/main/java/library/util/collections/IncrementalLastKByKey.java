package library.util.collections;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * キーごとに最大の値だけを残し、全体の上位 k 件を値の降順に維持する。
 * <p>
 * {@link java.util.HashMap} のように {@code put(key, value)} で登録する。
 * 同じキー（{@link Objects#equals}）には値を 1 個だけ保持し、{@code comp} で大きい方を残す。
 * 追加のみ可能で、削除はできない（上位 k 件より下は捨てる）。
 * <ul>
 *   <li>不変条件: 保持するキーは互いに異なる。</li>
 *   <li>キーごとの最大のみが意味を持つので、一度押し出されたキーが後で小さい値で来ても結果は変わらない。</li>
 * </ul>
 *
 * @param <K> キーの型（null 可）
 * @param <V> 値の型（null 不可）
 */
public class IncrementalLastKByKey<K, V> implements Iterable<Map.Entry<K, V>> {
//https://atcoder.jp/contests/abc447/submissions/73770589
//https://atcoder.jp/contests/abc345/submissions/74247226
//https://atcoder.jp/contests/abc485/submissions/81023568
	/**
	 * 値の降順に並べたキーと値。長さは最大要素数 k で固定。
	 * 不変条件: {@code [0..size)} が有効で、{@code values} が {@code comp} について降順。
	 * {@code [size..)} は null。{@code keys[i]} と {@code values[i]} が 1 組。
	 */
	private final K[] keys;
	private final V[] values;

	/** 現在の要素数。{@code 0 <= size <= keys.length}。 */
	private int size = 0;

	/** 値の大小を決める比較器。大きい値ほど前に並ぶ。 */
	private final Comparator<? super V> comp;

	/**
	 * 最大要素数と値の比較器を指定して初期化する。
	 * <ul>
	 *   <li>計算量: O(k)</li>
	 * </ul>
	 *
	 * @param k 保持する最大要素数（0 以上）
	 * @param comp 値の大小を決める比較器。大きい値ほど前に並ぶ。
	 * @throws IllegalArgumentException {@code k < 0} のとき
	 * @throws NullPointerException {@code comp} が null のとき
	 */
	@SuppressWarnings("unchecked")
	public IncrementalLastKByKey(int k, Comparator<? super V> comp) {
		if (k < 0) throw new IllegalArgumentException("k must be >= 0: " + k);
		this.comp = Objects.requireNonNull(comp, "comp is null");
		this.keys = (K[]) new Object[k];
		this.values = (V[]) new Object[k];
	}

	/**
	 * 最大要素数を指定して初期化する。値は自然順序（{@link Comparable}）で比較する。
	 * <ul>
	 *   <li>事前条件: {@code V} が {@link Comparable} であること。
	 *       満たさない型だと、最初の {@link #put} で {@link ClassCastException} になる。</li>
	 *   <li>計算量: O(k)</li>
	 * </ul>
	 *
	 * @param k 保持する最大要素数（0 以上）
	 * @throws IllegalArgumentException {@code k < 0} のとき
	 */
	@SuppressWarnings("unchecked")
	public IncrementalLastKByKey(int k) {
		this(k, (Comparator<V>) (Comparator<?>) Comparator.naturalOrder());
	}
	
	
	/**
	 * キーと値を登録する。
	 * 同じキーが既にあれば、新しい値が大きい場合のみ置き換える。
	 * 新しいキーなら、上位 k 件に入る場合のみ挿入し、溢れた最小要素は捨てる。
	 * 値が同じ（{@code comp} で 0）の要素は、既存の要素の後ろに入る。
	 * <p>
	 * {@link Map#put} と違い、戻り値は古い値ではなく「保持内容が変化したか」。
	 * <ul>
	 *   <li>計算量: O(k)</li>
	 * </ul>
	 *
	 * @param key キー
	 * @param value 値（null 不可）
	 * @return 保持内容が変化した場合は true
	 */
	public boolean put(K key, V value) {
		Objects.requireNonNull(value, "value is null");
		if (keys.length == 0) return false;

		for (int i = 0; i < size; i++) {
			if (!Objects.equals(key, keys[i])) continue;
			if (comp.compare(value, values[i]) <= 0) return false;
			int j = i; // 置き換えて前方へ移動
			while (j > 0 && comp.compare(values[j - 1], value) < 0) {
				keys[j] = keys[j - 1];
				values[j] = values[j - 1];
				j--;
			}
			keys[j] = key;
			values[j] = value;
			return true;
		}

		int i;
		if (size == keys.length) {
			if (comp.compare(value, values[size - 1]) <= 0) return false; // 最小以下なら入らない
			i = size - 1; // 末尾を押し出す
		} else {
			i = size++;
		}
		while (i > 0 && comp.compare(values[i - 1], value) < 0) {
			keys[i] = keys[i - 1];
			values[i] = values[i - 1];
			i--;
		}
		keys[i] = key;
		values[i] = value;
		return true;
	}

	/**
	 * キーに対応する値を返す。保持されていなければ null。
	 * <ul>
	 *   <li>計算量: O(k)</li>
	 * </ul>
	 */
	public V get(K key) {
		for (int i = 0; i < size; i++) {
			if (Objects.equals(key, keys[i])) return values[i];
		}
		return null;
	}

	/** キーが保持されているか。O(k)。 */
	public boolean containsKey(K key) {
		for (int i = 0; i < size; i++) {
			if (Objects.equals(key, keys[i])) return true;
		}
		return false;
	}

	/** 最大の値を返す。空なら {@link NoSuchElementException}。 */
	public V maxValue() {
		if (size == 0) throw new NoSuchElementException();
		return values[0];
	}

	/** 2 番目に大きい値を返す。要素が 2 個未満なら {@link NoSuchElementException}。 */
	public V secondMaxValue() {
		if (size < 2) throw new NoSuchElementException();
		return values[1];
	}

	/** 要素があれば最大の値、なければ defaultValue を返す。 */
	public V maxValueOrDefault(V defaultValue) {
		return size >= 1 ? values[0] : defaultValue;
	}

	/** 要素が 2 個以上あれば 2 番目に大きい値、なければ defaultValue を返す。 */
	public V secondMaxValueOrDefault(V defaultValue) {
		return size >= 2 ? values[1] : defaultValue;
	}

	/**
	 * キー {@code key} を取り除いたと仮定した場合の最大値を返す。
	 * そのキーが保持されていなければ（棄却・押し出し済みを含む）、取り除いても最大は変わらない。
	 * <ul>
	 *   <li>事前条件: {@code capacity() >= 2}。k が 1 以下だと 2 番目を捨てているため正しく答えられない。</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 *
	 * @param key 取り除くキー
	 * @param defaultValue 取り除いた後に要素が残らないときに返す値
	 * @throws IllegalStateException {@code capacity() < 2} のとき
	 */
	public V maxValueExcludingKeyOrDefault(K key, V defaultValue) {
		if (keys.length < 2) throw new IllegalStateException("requires k >= 2: " + keys.length);
		if (size == 0) return defaultValue;
		if (Objects.equals(key, keys[0])) return secondMaxValueOrDefault(defaultValue);
		return values[0];
	}

	/**
	 * 降順で i 番目（0 始まり）のキーを返す。
	 *
	 * @throws IndexOutOfBoundsException {@code i < 0 || i >= size()} のとき
	 */
	public K keyAt(int i) {
		checkIndex(i);
		return keys[i];
	}

	/**
	 * 降順で i 番目（0 始まり）の値を返す。
	 *
	 * @throws IndexOutOfBoundsException {@code i < 0 || i >= size()} のとき
	 */
	public V valueAt(int i) {
		checkIndex(i);
		return values[i];
	}

	private void checkIndex(int i) {
		if (i < 0 || i >= size) throw new IndexOutOfBoundsException("index: " + i + ", size: " + size);
	}

	public int size() { return size; }
	public int capacity() { return keys.length; }
	public boolean isEmpty() { return size == 0; }
	public boolean isFull() { return size == keys.length; }

	public void clear() {
		Arrays.fill(keys, null);
		Arrays.fill(values, null);
		size = 0;
	}

	/** 値の降順に（キー, 値）を返す。返す {@link Map.Entry} は変更不可。 */
	@Override
	public Iterator<Map.Entry<K, V>> iterator() {
		return new Iterator<>() {
			int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < size;
			}

			@Override
			public Map.Entry<K, V> next() {
				if (!hasNext()) throw new NoSuchElementException();
				Map.Entry<K, V> e = new AbstractMap.SimpleImmutableEntry<>(keys[idx], values[idx]);
				idx++;
				return e;
			}
		};
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < size; i++) {
			if (i > 0) sb.append(", ");
			sb.append(keys[i]).append('=').append(values[i]);
		}
		return sb.append('}').toString();
	}
}
