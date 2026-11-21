package library.util.collections;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;

import library.util.ArrayUtils;
import library.util.IntComparator;

/***
 * tailがa.lengthに比べて小さくなっても配列を取り直さない。
 * @param <T>
 */
public class IntArrayList implements Iterable<Integer> {
	@SuppressWarnings("unchecked")
	int defaultCapacity = 16;
	int[] a;
	public int tail = 0;
	public IntArrayList() {
		a = new int[defaultCapacity];
	}
	
	public int[] rawArray() {
		return a;
	}
	
	public IntArrayList(int initialCapacity) {
		a = new int[initialCapacity];
	}
	
	public void clear() {
		tail = 0;
	}
	
	public int peekFirst() {
		return a[0];
	}
	
	public int peekLast() {
		return a[tail - 1];
	}
	
	
	public int peekLastOrDefault(int defaultValue) {
		if (tail == 0) return defaultValue;
		else return a[tail - 1];
	}
	
	public void add(int v) {
		if (tail == a.length) resize(2 * a.length);
		a[tail] = v;
		tail++;
	}

	/**
	 * 添え字 index の位置に value を挿入し、index 以降の要素を 1 つ右へ移動する。
	 * 計算量: O(N - index)（容量拡張時は O(N)）
	 *
	 * @param index 挿入位置。0 <= index <= size()
	 * @param value 挿入する値
	 * @throws IndexOutOfBoundsException 0 <= index <= size() が成り立たない場合
	 */
	// 未テスト
	public void insert(int index, int value) {
		if (index < 0 || index > tail) throw new IndexOutOfBoundsException();
		ensureCapacity(tail + 1);
		int moved = tail - index;
		if (moved > 0) System.arraycopy(a, index, a, index + 1, moved);
		a[index] = value;
		tail++;
	}

	public void addAll(IntArrayList list) {
		ensureCapacity(tail + list.tail);
		System.arraycopy(list.a, 0, a, tail, list.tail);
		tail += list.tail;
	}

	public void addAll(int[] values) {
		ensureCapacity(tail + values.length);
		System.arraycopy(values, 0, a, tail, values.length);
		tail += values.length;
	}
	
	public int pollLast() {
		if (tail == 0) throw new NoSuchElementException();
		int ret = a[tail - 1];
		tail--;
		return ret;
	}

	/**
	 * 添え字 i の要素を削除する。削除後は i より後ろの要素が 1 つ左へ移動する。
	 * 計算量: O(N - i)
	 *
	 * @param i 削除する要素の添え字。0 <= i < size()
	 * @throws IndexOutOfBoundsException 0 <= i < size() が成り立たない場合
	 */
	// 未テスト
	public void removeAt(int i) {
		if (i < 0 || i >= tail) throw new IndexOutOfBoundsException();
		int moved = tail - i - 1;
		if (moved > 0) System.arraycopy(a, i + 1, a, i, moved);
		tail--;
	}
	
	/**
	 * 半開区間 [l, r) の要素を削除する。
	 * 削除後は r 以降の要素が l まで移動する。
	 *
	 * 計算量: O(N - r)
	 *
	 * @param l 削除区間の開始位置。0 <= l <= r
	 * @param r 削除区間の終了位置。r <= size()
	 * @throws IndexOutOfBoundsException 0 <= l <= r <= size() が成り立たない場合
	 */
	public void removeRange(int l, int r) {
	    if (l < 0 || l > r || r > tail) throw new IndexOutOfBoundsException();
	    int moved = tail - r;
	    if (moved > 0) System.arraycopy(a, r, a, l, moved);
	    tail -= r - l;
	}

	
	/**
	 * リスト list の全要素を添え字 l の位置に挿入する。
	 *
	 * 計算量: O(N-l+M)（M = list.size()、容量拡張時は O(N+M)）
	 *
	 * @param l 挿入位置。0 <= l <= size()
	 * @param list 挿入する要素列
	 */
	public void insertRange(int l, IntArrayList list) {
	    if (l < 0 || l > tail) throw new IndexOutOfBoundsException();
	    int len = list.tail;
	    ensureCapacity(tail + len);
	    System.arraycopy(a, l, a, l + len, tail - l);
	    System.arraycopy(list.a, 0, a, l, len);
	    tail += len;
	}
	
	
	/**
	 * 値 v と等しい最初の要素を削除する。
	 * 計算量: O(N)
	 *
	 * @param v 削除する値
	 * @return v と等しい要素が存在して削除した場合は true
	 */
	// 未テスト
	public boolean remove(int v) {
		for (int i = 0; i < tail; i++) {
			if (a[i] == v) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}
	
	public boolean isEmpty() {
		return tail == 0;
	}
	
	public boolean isNonEmpty() {
		return tail != 0;
	}
	
	public int get(int id) {
		if (id < 0 || id >= tail) throw new IndexOutOfBoundsException("get("+id+")は添え字"+0+"以上"+(tail-1)+"以下に違反");
		return a[id];
	}
	
	public int getOrDefault(int id, int defaultValue) {
		if (id < 0 || id >= tail) return defaultValue;
		return a[id];
	}
	
	public void set(int id, int value) {
		if (id < 0 || id >= tail) throw new IndexOutOfBoundsException();
		a[id] = value;
	}
	
	void resize(int size) {
		a = Arrays.copyOf(a, size);
	}

	void ensureCapacity(int capacity) {
		if (capacity <= a.length) return;
		int size = a.length;
		while (size < capacity) size *= 2;
		resize(size);
	}
	
	public int size() {
		return tail;
	}
	
	public int[] toArray() {
		return Arrays.copyOf(a, tail);
	}
	
	public long[] toLongArray() {
		long[]ret=new long[tail];
		for (int i = 0; i < tail; i++) {
			ret[i]=get(i);
		}
		return ret;
	}
	
	public void swap(int i, int j) {
		if(i==j)return;
		var tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}
	
	public void reverse() {
		int s = 0;
		int t = tail - 1;
		while (s < t) {
			{
				var tmp = a[s];
				a[s] = a[t];
				a[t] = tmp;
			}
			++s;--t;
		}
	}
	
	public void reverse(int s, int t) {
		t--;
		while (s < t) {
			{
				var tmp = a[s];
				a[s] = a[t];
				a[t] = tmp;
			}
			++s;--t;
		}
	}

	
	
    @Override
    public PrimitiveIterator.OfInt iterator() {
		return new PrimitiveIterator.OfInt() {
			int idx = 0;

			@Override
			public boolean hasNext() {
				return idx < tail;
			}

			@Override
			public int nextInt() {
				if (!hasNext()) throw new NoSuchElementException();
				return get(idx++);
			}
		};
    }
	
    
    public void sort() {
    	Arrays.sort(a, 0, tail);
    }

	/**
	 * 指定された比較関数 cmp に従って、リスト内の要素 a[0], a[1], ..., a[N-1] をソートする。
	 * すべての 0 <= i < j < N に対し、cmp.compare(a[i], a[j]) <= 0 が成り立つように並び替える。
	 *
	 * 計算量: O(N log N)
	 *
	 * @param cmp 比較関数
	 */
	// 未テスト
	public void sort(IntComparator cmp) {
		if (tail <= 1) return;
		ArrayUtils.sort(a, 0, tail - 1, cmp);
	}
	
	public void shuffle(Random r) {
		for (int i = tail - 1; i >= 1; i--) {
			int j=r.nextInt(i+1);
			{
				var tmp = a[i];
				a[i] = a[j];
				a[j] = tmp;
			}
		}
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntArrayList that)) return false;
        if (tail != that.tail) return false;
        for (int i = 0; i < tail; i++) {
            if (a[i] != that.a[i]) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < tail; i++) {
            result = 31 * result + a[i];
        }
        return result;
    }

    @Override
	public String toString() {
	return Arrays.toString(toArray());
	}

	/**
	 * IntArrayListの現在の状態をコピーした新しいインスタンスを返す。
	 * <ul>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * @return IntArrayListのコピー
	 */
	public IntArrayList copy() {
		IntArrayList ret = new IntArrayList(a.length);
		ret.tail = this.tail;
		ret.defaultCapacity = this.defaultCapacity;
		System.arraycopy(this.a, 0, ret.a, 0, this.a.length);
		return ret;
	}
	
	public int min() {
		int ret=Integer.MAX_VALUE;
		for (int i = 0; i < tail; i++) {
			ret=Math.min(ret, a[i]);
		}
		return ret;
	}

	/**
	 * リストの内容を標準出力に出力する。
	 * O(N)
	 * // 未テスト
	 */
	public void dump() {
		System.out.println(toString());
	}
}


