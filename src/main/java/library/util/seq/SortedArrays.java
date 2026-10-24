package library.util.seq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

import library.util.collections.IntArrayList;
import library.util.collections.LongArrayList;

public class SortedArrays {
	/**
	 * ソート済み配列a, bをマージした配列を返す。a, bは昇順にソートされているとする。
	 * @param a
	 * @param b
	 * @return
	 */
	public static int[] merge(int[] a, int[] b) {
		int[] c = new int[a.length + b.length];
		int i = 0;
		int j = 0;
		int k = 0;
		while (i < a.length && j < b.length) {
			if (a[i] <= b[j]) c[k++] = a[i++];
			else c[k++] = b[j++];
		}
		while (i < a.length) c[k++] = a[i++];
		while (j < b.length) c[k++] = b[j++];
		return c;
	}
	
	/**
	 * ソート済み配列a, bをマージした配列を返す。a, bは昇順にソートされているとする。
	 * @param a
	 * @param b
	 * @return
	 */
	public static long[] merge(long[] a, long[] b) {
		long[] c = new long[a.length + b.length];
		int i = 0;
		int j = 0;
		int k = 0;
		while (i < a.length && j < b.length) {
			if (a[i] <= b[j]) c[k++] = a[i++];
			else c[k++] = b[j++];
		}
		while (i < a.length) c[k++] = a[i++];
		while (j < b.length) c[k++] = b[j++];
		return c;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 */
	public static int floor(int[] a, int key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m] <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int floor(IntUnaryOperator f, int left, int right, int key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsInt(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long floor(LongUnaryOperator f, long left, long right, long key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsLong(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long floor(LongToIntFunction f, long left, long right, int key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsInt(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int floor(char[][] a, char[] key) {
		return floor(a, key, Arrays::compare);
	}

	/**
	 * a[i] < key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int lower(char[][] a, char[] key) {
		return lower(a, key, Arrays::compare);
	}

	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int floor(int[][] a, int[] key) {
		return floor(a, key, Arrays::compare);
	}

	/**
	 * a[i] < key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int lower(int[][] a, int[] key) {
		return lower(a, key, Arrays::compare);
	}

	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int floor(long[][] a, long[] key) {
		return floor(a, key, Arrays::compare);
	}

	/**
	 * a[i] < key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int lower(long[][] a, long[] key) {
		return lower(a, key, Arrays::compare);
	}

	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 */
	public static int floor(IntArrayList a, int key) {
		int ok = -1;
		int ng = a.size();
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a.get(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int floor(T[] a, T key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m].compareTo(key) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int floor(IntFunction<T> f, int left, int right, T key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long floor(LongFunction<T> f, long left, long right, T key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * a[i] < key となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int lower(T[] a, T key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m].compareTo(key) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) < key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) < key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int lower(IntFunction<T> f, int left, int right, T key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) < key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) < key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long lower(LongFunction<T> f, long left, long right, T key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * a[i] < key となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int lower(List<T> a, T key) {
		int ok = -1;
		int ng = a.size();
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a.get(m).compareTo(key) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int floor(ArrayList<T> a, T key) {
		int ok = -1;
		int ng = a.size();
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a.get(m).compareTo(key) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}


	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int ceil(int[] a, int key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (a[m] >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int ceil(IntUnaryOperator f, int left, int right, int key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsInt(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long ceil(LongToIntFunction f, long left, long right, int key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsInt(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int ceil(char[][] a, char[] key) {
		return ceil(a, key, Arrays::compare);
	}

	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int higher(char[][] a, char[] key) {
		return higher(a, key, Arrays::compare);
	}

	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int ceil(int[][] a, int[] key) {
		return ceil(a, key, Arrays::compare);
	}

	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int higher(int[][] a, int[] key) {
		return higher(a, key, Arrays::compare);
	}

	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int ceil(long[][] a, long[] key) {
		return ceil(a, key, Arrays::compare);
	}

	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * @param a
	 * @param key
	 * @return
	 * O(L log N) (L: key.length, N: a.length)
	 */
	public static int higher(long[][] a, long[] key) {
		return higher(a, key, Arrays::compare);
	}

	/**
	 * key <= a[i] となる最大の i を返す。aは降順にソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 * verified:https://atcoder.jp/contests/abc431/submissions/70816641
	 */
	public static int ceilDesc(long[] a, long key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m] >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) >= key となる最大の i in [left, right) を返す。f は広義単調減少とする。
	 * @param f 降順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) >= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int ceilDesc(IntUnaryOperator f, int left, int right, int key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsInt(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) >= key となる最大の i in [left, right) を返す。f は広義単調減少とする。
	 * @param f 降順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) >= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int ceilDesc(IntToLongFunction f, int left, int right, long key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsLong(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) >= key となる最大の i in [left, right) を返す。f は広義単調減少とする。
	 * @param f 降順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) >= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long ceilDesc(LongUnaryOperator f, long left, long right, long key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsLong(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) >= key となる最大の i in [left, right) を返す。f は広義単調減少とする。
	 * @param f 降順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) >= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long ceilDesc(LongToIntFunction f, long left, long right, int key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsInt(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int ceil(T[] a, T key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (a[m].compareTo(key) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int ceil(IntFunction<T> f, int left, int right, T key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long ceil(LongFunction<T> f, long left, long right, T key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T extends Comparable<? super T>> int ceil(ArrayList<T> a, T key) {
		int ok = a.size();
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (a.get(m).compareTo(key) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T> int ceil(T[] a, T key, Comparator<T> comp) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a[m]) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int ceil(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long ceil(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key <= a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T> int ceil(ArrayList<T> a, T key, Comparator<T> comp) {
		//https://atcoder.jp/contests/abc212/submissions/74399058
		int ok = a.size();
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a.get(m)) <= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * 未テスト
	 * @param <T>
	 * @param a
	 * @param key
	 * @param comp
	 * @return
	 */
	public static <T> int higher(ArrayList<T> a, T key, Comparator<T> comp) {
		int ok = a.size();
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a.get(m)) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T> int higher(T[] a, T key, Comparator<T> comp) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a[m]) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int higher(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long higher(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) < 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int higher(IntFunction<T> f, int left, int right, T key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) > 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long higher(LongFunction<T> f, long left, long right, T key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (f.apply(m).compareTo(key) > 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int higher(long[] a, long key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int higher(IntToLongFunction f, int left, int right, long key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < f.applyAsLong(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long higher(LongUnaryOperator f, long left, long right, long key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (key < f.applyAsLong(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long higher(LongToIntFunction f, long left, long right, int key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (key < f.applyAsInt(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int higher(ArrayList<Long> a, long key) {
		int ok = a.size();
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < a.get(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int higher(ArrayList<Integer> a, int key) {
		int ok = a.size();
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < a.get(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key >= a[i] となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T> int floor(T[] a, T key, Comparator<T> comp) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a[m]) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key >= f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key >= f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int floor(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key >= f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key >= f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long floor(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) >= 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > a[i] となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static <T> int lower(T[] a, T key, Comparator<T> comp) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, a[m]) > 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int lower(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) > 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long lower(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (comp.compare(key, f.apply(m)) > 0)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key > a[i] となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int lower(long[] a, long key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int lower(IntToLongFunction f, int left, int right, long key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > f.applyAsLong(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long lower(LongUnaryOperator f, long left, long right, long key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (key > f.applyAsLong(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long lower(LongToIntFunction f, long left, long right, int key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (key > f.applyAsInt(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * key > a[i] となる最大の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int lower(int[] a, int key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int lower(IntUnaryOperator f, int left, int right, int key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > f.applyAsInt(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int floor(long[] a, long key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m] <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int floor(IntToLongFunction f, int left, int right, long key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsLong(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * a[i] <= key となる最大の i を返す。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int floor(LongArrayList a, long key) {
		int ok = -1;
		int ng = a.size();
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a.get(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= a[i] となる最小の i を返す。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int ceil(long[] a, long key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (a[m] >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int ceil(IntToLongFunction f, int left, int right, long key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsLong(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long ceil(LongUnaryOperator f, long left, long right, long key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsLong(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}


	/**
	 * comp(key, a[i]) = 0 となる最小の i を返す。存在しなければ-1
	 * @param a
	 * @param key
	 * @return
	 * https://atcoder.jp/contests/abc442/submissions/72739506
	 */
	public static <T> int indexOf(T[] a, T key, Comparator<T> comp) {
		int ret=SortedArrays.lower(a, key, comp)+1;
		if(0 <= ret && ret < a.length && comp.compare(a[ret], key) == 0) return ret;
		else return -1;
	}

	/**
	 * comp(f(i), key) = 0 となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return comp(f(i), key) = 0 となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int indexOf(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ret = lower(f, left, right, key, comp) + 1;
		if (left <= ret && ret < right && comp.compare(f.apply(ret), key) == 0) return ret;
		else return -1;
	}

	/**
	 * comp(f(i), key) = 0 となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return comp(f(i), key) = 0 となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long indexOf(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ret = lower(f, left, right, key, comp) + 1;
		if (left <= ret && ret < right && comp.compare(f.apply(ret), key) == 0) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int indexOf(IntFunction<T> f, int left, int right, T key) {
		int ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.apply(ret).compareTo(key) == 0) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long indexOf(LongFunction<T> f, long left, long right, T key) {
		long ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.apply(ret).compareTo(key) == 0) return ret;
		else return -1;
	}

	/**
	 * comp(f(i), key) = 0 となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return comp(f(i), key) = 0 となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> int lastIndexOf(IntFunction<T> f, int left, int right, T key, Comparator<T> comp) {
		int ret = higher(f, left, right, key, comp) - 1;
		if (left <= ret && ret < right && comp.compare(f.apply(ret), key) == 0) return ret;
		else return -1;
	}

	/**
	 * comp(f(i), key) = 0 となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @param comp 比較器
	 * @return comp(f(i), key) = 0 となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T> long lastIndexOf(LongFunction<T> f, long left, long right, T key, Comparator<T> comp) {
		long ret = higher(f, left, right, key, comp) - 1;
		if (left <= ret && ret < right && comp.compare(f.apply(ret), key) == 0) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> int lastIndexOf(IntFunction<T> f, int left, int right, T key) {
		int ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.apply(ret).compareTo(key) == 0) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param <T> 要素の型
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static <T extends Comparable<? super T>> long lastIndexOf(LongFunction<T> f, long left, long right, T key) {
		long ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.apply(ret).compareTo(key) == 0) return ret;
		else return -1;
	}
	
	
	/**
	 * key = a[i] となる最小の i を返す。存在しなければ-1
	 * 未テスト
	 * @param a
	 * @param key
	 * @return
	 */
	public static int indexOf(long[] a, long key) {
		int ret=lower(a, key)+1;
		if(0 <= ret && ret < a.length && a[ret] == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int indexOf(IntToLongFunction f, int left, int right, long key) {
		int ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.applyAsLong(ret) == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long indexOf(LongUnaryOperator f, long left, long right, long key) {
		long ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.applyAsLong(ret) == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long indexOf(LongToIntFunction f, long left, long right, int key) {
		long ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.applyAsInt(ret) == key) return ret;
		else return -1;
	}
	
	/**
	 * key = a[i] となる最小の i を返す。存在しなければ-1
	 * 未テスト
	 * @param a
	 * @param key
	 * @return
	 */
	public static int indexOf(int[] a, int key) {
		int ret=lower(a, key)+1;
		if(0 <= ret && ret < a.length && a[ret] == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最小の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最小の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int indexOf(IntUnaryOperator f, int left, int right, int key) {
		int ret = lower(f, left, right, key) + 1;
		if (left <= ret && ret < right && f.applyAsInt(ret) == key) return ret;
		else return -1;
	}
	
	
	/**
	 * key = a[i] となる最後の i を返す。存在しなければ-1
	 * 未テスト
	 * @param a
	 * @param key
	 * @return
	 */
	public static int lastIndexOf(long[] a, long key) {
		int ret=higher(a, key)-1;
		if(0 <= ret && ret < a.length && a[ret] == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int lastIndexOf(IntToLongFunction f, int left, int right, long key) {
		int ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.applyAsLong(ret) == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long lastIndexOf(LongUnaryOperator f, long left, long right, long key) {
		long ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.applyAsLong(ret) == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long lastIndexOf(LongToIntFunction f, long left, long right, int key) {
		long ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.applyAsInt(ret) == key) return ret;
		else return -1;
	}
	
	
	/**
	 * key = a[i] となる最後の i を返す。存在しなければ-1
	 * 未テスト
	 * @param a
	 * @param key
	 * @return
	 */
	public static int lastIndexOf(int[] a, int key) {
		int ret=higher(a, key)-1;
		if(0 <= ret && ret < a.length && a[ret] == key) return ret;
		else return -1;
	}

	/**
	 * f(i) = key となる最後の i in [left, right) を返す。存在しなければ -1。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) = key となる最後の i。存在しない場合は -1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int lastIndexOf(IntUnaryOperator f, int left, int right, int key) {
		int ret = higher(f, left, right, key) - 1;
		if (left <= ret && ret < right && f.applyAsInt(ret) == key) return ret;
		else return -1;
	}
	
	
	/**
	 * key < a[i] となる最小の i を返す。aはソートされているとする。
	 * 
	 * @param a
	 * @param key
	 * @return
	 */
	public static int higher(int[] a, int key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int higher(IntUnaryOperator f, int left, int right, int key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < f.applyAsInt(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}
	
	/**
	 * rankは0-origin. aに含まれないlowerBound以上の数のうち、rank番目(0-origin)に小さい数を返す
	 * @param a
	 * @param k
	 * @return
	 */
	public static long kthOnComplementSet(long[] a, long rank, long lowerBound) {
		//https://atcoder.jp/contests/abc440/submissions/72388760
		int L = floor(a, lowerBound - 1);
        int ok = L;
        int ng = a.length;
        while (Math.abs(ok - ng) != 1) {
            int mid = (ok + ng) / 2;
            long v = (a[mid] - (lowerBound - 1)) - (mid - L);// [lowerBound:a[mid]] に何個のaに含まれない数があるか。
            if (v < rank+1) {
                ok = mid;
            } else {
                ng = mid;
            }
        }
        if (ok == -1) {
        	return lowerBound + rank;
        } else {
        	long x = a[ok] - (lowerBound - 1) - (ok - L);// [lowerBound:a[ok]] に何個のaに含まれない数があるか。
        	return a[ok] + (rank+1) - x;
        }
	}

	/**
	 * rank は 0-origin。f([left, right)) に含まれない lowerBound 以上の整数のうち、rank 番目(0-origin)に小さい数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param rank 順位 (0-origin)
	 * @param lowerBound 下限値
	 * @return rank 番目に小さい非含まれ数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long kthOnComplementSet(IntToLongFunction f, int left, int right, long rank, long lowerBound) {
		int L = floor(f, left, right, lowerBound - 1);
		int ok = L;
		int ng = right;
		while (Math.abs(ok - ng) != 1) {
			int mid = (ok + ng) / 2;
			long v = (f.applyAsLong(mid) - (lowerBound - 1)) - (mid - L);
			if (v < rank + 1) {
				ok = mid;
			} else {
				ng = mid;
			}
		}
		if (ok == left - 1) {
			return lowerBound + rank;
		} else {
			long x = f.applyAsLong(ok) - (lowerBound - 1) - (ok - L);
			return f.applyAsLong(ok) + (rank + 1) - x;
		}
	}

	/**
	 * rank は 0-origin。f([left, right)) に含まれない lowerBound 以上の整数のうち、rank 番目(0-origin)に小さい数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param rank 順位 (0-origin)
	 * @param lowerBound 下限値
	 * @return rank 番目に小さい非含まれ数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long kthOnComplementSet(LongUnaryOperator f, long left, long right, long rank, long lowerBound) {
		long L = floor(f, left, right, lowerBound - 1);
		long ok = L;
		long ng = right;
		while (Math.abs(ok - ng) != 1) {
			long mid = (ok + ng) / 2;
			long v = (f.applyAsLong(mid) - (lowerBound - 1)) - (mid - L);
			if (v < rank + 1) {
				ok = mid;
			} else {
				ng = mid;
			}
		}
		if (ok == left - 1) {
			return lowerBound + rank;
		} else {
			long x = f.applyAsLong(ok) - (lowerBound - 1) - (ok - L);
			return f.applyAsLong(ok) + (rank + 1) - x;
		}
	}
	
	public static int countLeq(long[] a, long v) {
		//https://atcoder.jp/contests/abc437/submissions/73654891
		return floor(a, v) + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLeq(IntUnaryOperator f, int left, int right, int v) {
		return floor(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLeq(IntToLongFunction f, int left, int right, long v) {
		return floor(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLeq(LongUnaryOperator f, long left, long right, long v) {
		return floor(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLeq(LongToIntFunction f, long left, long right, int v) {
		return floor(f, left, right, v) - left + 1;
	}
	
	public static int countLess(long[] a, long v) {
		//https://atcoder.jp/contests/abc437/submissions/73654891
		return lower(a, v) + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLess(IntUnaryOperator f, int left, int right, int v) {
		return lower(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLess(IntToLongFunction f, int left, int right, long v) {
		return lower(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLess(LongUnaryOperator f, long left, long right, long v) {
		return lower(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLess(LongToIntFunction f, long left, long right, int v) {
		return lower(f, left, right, v) - left + 1;
	}
	
	public static int countGeq(long[] a, long v) {
		//https://atcoder.jp/contests/abc437/submissions/73654891
		return a.length - countLess(a, v);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGeq(IntUnaryOperator f, int left, int right, int v) {
		return right - (lower(f, left, right, v) + 1);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGeq(IntToLongFunction f, int left, int right, long v) {
		return right - (lower(f, left, right, v) + 1);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGeq(LongUnaryOperator f, long left, long right, long v) {
		return right - (lower(f, left, right, v) + 1);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGeq(LongToIntFunction f, long left, long right, int v) {
		return right - (lower(f, left, right, v) + 1);
	}
	
	public static int countGreater(long[] a, long v) {
		//https://atcoder.jp/contests/abc437/submissions/73654891
		return a.length - countLeq(a, v);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGreater(IntUnaryOperator f, int left, int right, int v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGreater(IntToLongFunction f, int left, int right, long v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGreater(LongUnaryOperator f, long left, long right, long v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGreater(LongToIntFunction f, long left, long right, int v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * key <= a[i] となる最小の i を返す。
	 *
	 * @param a
	 * @param key
	 * @return
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int ceil(double[] a, double key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (a[m] >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int ceil(IntToDoubleFunction f, int left, int right, double key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsDouble(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key <= f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long ceil(LongToDoubleFunction f, long left, long right, double key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsDouble(m) >= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > a[i] となる最大の i を返す。aはソートされているとする。
	 *
	 * @param a
	 * @param key
	 * @return
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int lower(double[] a, double key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int lower(IntToDoubleFunction f, int left, int right, double key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (key > f.applyAsDouble(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key > f(i) となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key > f(i) となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long lower(LongToDoubleFunction f, long left, long right, double key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (key > f.applyAsDouble(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key <= a[i] となる要素の個数を返す。aはソートされているとする。
	 *
	 * @param a
	 * @param v
	 * @return
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int countGeq(double[] a, double v) {
		return a.length - (lower(a, v) + 1);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGeq(IntToDoubleFunction f, int left, int right, double v) {
		return right - (lower(f, left, right, v) + 1);
	}

	/**
	 * f(i) >= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) >= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGeq(LongToDoubleFunction f, long left, long right, double v) {
		return right - (lower(f, left, right, v) + 1);
	}

	/**
	 * a[i] <= key となる最大のインデックス i を返す。
	 *
	 * @param a 昇順にソートされた double 配列
	 * @param key 探索値
	 * @return a[i] <= key となる最大の i。存在しない場合は -1
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int floor(double[] a, double key) {
		int ok = -1;
		int ng = a.length;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (a[m] <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int floor(IntToDoubleFunction f, int left, int right, double key) {
		int ok = left - 1;
		int ng = right;
		while (ng - ok > 1) {
			int m = (ok + ng) / 2;
			if (f.applyAsDouble(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * f(i) <= key となる最大の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return f(i) <= key となる最大の i。存在しない場合は left - 1
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long floor(LongToDoubleFunction f, long left, long right, double key) {
		long ok = left - 1;
		long ng = right;
		while (ng - ok > 1) {
			long m = (ok + ng) / 2;
			if (f.applyAsDouble(m) <= key)
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < a[i] となる最小のインデックス i を返す。
	 *
	 * @param a 昇順にソートされた double 配列
	 * @param key 探索値
	 * @return key < a[i] となる最小の i。存在しない場合は a.length
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int higher(double[] a, double key) {
		int ok = a.length;
		int ng = -1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < a[m])
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int higher(IntToDoubleFunction f, int left, int right, double key) {
		int ok = right;
		int ng = left - 1;
		while (ok - ng > 1) {
			int m = (ok + ng) / 2;
			if (key < f.applyAsDouble(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * key < f(i) となる最小の i in [left, right) を返す。f は広義単調増加とする。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param key 探索値
	 * @return key < f(i) となる最小の i。存在しない場合は right
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long higher(LongToDoubleFunction f, long left, long right, double key) {
		long ok = right;
		long ng = left - 1;
		while (ok - ng > 1) {
			long m = (ok + ng) / 2;
			if (key < f.applyAsDouble(m))
				ok = m;
			else
				ng = m;
		}
		return ok;
	}

	/**
	 * a[i] <= v となる要素の個数を返す。
	 *
	 * @param a 昇順にソートされた double 配列
	 * @param v 基準値
	 * @return a[i] <= v となる要素数
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int countLeq(double[] a, double v) {
		return floor(a, v) + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLeq(IntToDoubleFunction f, int left, int right, double v) {
		return floor(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) <= v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) <= v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLeq(LongToDoubleFunction f, long left, long right, double v) {
		return floor(f, left, right, v) - left + 1;
	}

	/**
	 * a[i] < v となる要素の個数を返す。
	 *
	 * @param a 昇順にソートされた double 配列
	 * @param v 基準値
	 * @return a[i] < v となる要素数
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int countLess(double[] a, double v) {
		return lower(a, v) + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countLess(IntToDoubleFunction f, int left, int right, double v) {
		return lower(f, left, right, v) - left + 1;
	}

	/**
	 * f(i) < v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) < v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countLess(LongToDoubleFunction f, long left, long right, double v) {
		return lower(f, left, right, v) - left + 1;
	}

	/**
	 * a[i] > v となる要素の個数を返す。
	 *
	 * @param a 昇順にソートされた double 配列
	 * @param v 基準値
	 * @return a[i] > v となる要素数
	 *
	 * 計算量: O(log N)
	 * // 未テスト
	 */
	public static int countGreater(double[] a, double v) {
		return a.length - countLeq(a, v);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static int countGreater(IntToDoubleFunction f, int left, int right, double v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * f(i) > v となる i in [left, right) の個数を返す。
	 * @param f 昇順にソートされた関数
	 * @param left 探索区間の左端 (inclusive)
	 * @param right 探索区間の右端 (exclusive)
	 * @param v 基準値
	 * @return f(i) > v となる要素数
	 * 計算量: O(log(right - left))
	 */
	// 未テスト
	public static long countGreater(LongToDoubleFunction f, long left, long right, double v) {
		return right - (floor(f, left, right, v) + 1);
	}

	/**
	 * ソート済みの配列から隣接する重複した要素を排除した新たな配列を返す。
	 *
	 * @param a ソート済みの配列
	 * @return 重複が排除されたソート済み配列
	 *
	 * 計算量: O(N)
	 * // 未テスト
	 */
	public static long[] unique(long[] a) {
		if (a.length == 0)
			return new long[0];

		long[] temp = new long[a.length];
		temp[0] = a[0];
		int pointer = 1;
		for (int i = 1; i < a.length; i++) {
			if (a[i] != temp[pointer - 1]) {
				temp[pointer++] = a[i];
			}
		}
		return Arrays.copyOf(temp, pointer);
	}

}