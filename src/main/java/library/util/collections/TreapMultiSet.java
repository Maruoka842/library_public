package library.util.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * {@link Comparable} な要素に対応した Treap ベースのマルチセット。
 *
 * @param <E> 要素の型（{@link Comparable} を実装している必要があります）
 */
public class TreapMultiSet<E extends Comparable<? super E>> {
	private static final Random rnd = new Random();

	private static class Node<E> {
		E key;
		int priority;
		Node<E> left, right;
		int size;
		long sum;
		long val;

		public Node(E key, long val) {
			this.key = key;
			this.val = val;
			this.priority = rnd.nextInt();
			recalc(this);
		}
	}

	private static <E> void recalc(Node<E> t) {
		if (t == null) return;
		t.sum = t.val;
		t.size = 1;
		if (t.left != null) {
			t.sum += t.left.sum;
			t.size += t.left.size;
		}
		if (t.right != null) {
			t.sum += t.right.sum;
			t.size += t.right.size;
		}
	}

	private Node<E> root;
	private final Comparator<? super E> comp;
	/** 重複を含めた総要素数 */
	private long size = 0;

	/**
	 * 自然順序を使用する空のマルチセットを構築します。
	 *
	 * @complexity O(1)
	 */
	// 未テスト
	public TreapMultiSet() {
		this(null);
	}

	/**
	 * 指定された比較子を使用する空のマルチセットを構築します。
	 *
	 * @param comp 要素の比較子（null の場合は自然順序を使用）
	 * @complexity O(1)
	 */
	// 未テスト
	public TreapMultiSet(Comparator<? super E> comp) {
		this.comp = comp;
		this.root = null;
		this.size = 0;
	}

	private TreapMultiSet(Node<E> root, long size, Comparator<? super E> comp) {
		this.root = root;
		this.size = size;
		this.comp = comp;
	}

	@SuppressWarnings("unchecked")
	private int compare(E a, E b) {
		if (comp != null) {
			return comp.compare(a, b);
		}
		return a.compareTo(b);
	}

	/**
	 * このマルチセットのコピー（ディープコピー）を返します。
	 *
	 * @return このマルチセットのコピー
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	public TreapMultiSet<E> copy() {
		return new TreapMultiSet<>(copyNode(this.root), this.size, this.comp);
	}

	private Node<E> copyNode(Node<E> node) {
		if (node == null) return null;
		Node<E> cp = new Node<>(node.key, node.val);
		cp.priority = node.priority;
		cp.size = node.size;
		cp.sum = node.sum;
		cp.left = copyNode(node.left);
		cp.right = copyNode(node.right);
		return cp;
	}

	/**
	 * このマルチセットと別のマルチセットの共通部分（積集合）を計算します。
	 * 各要素の個数は、双方のマルチセットにおける個数の最小値となります。
	 *
	 * @param other 比較対象のマルチセット
	 * @return 積集合となる新しい TreapMultiSet
	 * @complexity O(N_1 + N_2) (N_1, N_2 はそれぞれの異なる要素の数)
	 */
	// 未テスト
	public TreapMultiSet<E> cap(TreapMultiSet<E> other) {
		List<Entry<E>> list1 = this.entryList();
		List<Entry<E>> list2 = other.entryList();
		int n1 = list1.size();
		int n2 = list2.size();
		@SuppressWarnings("unchecked")
		E[] keys = (E[]) new Comparable<?>[Math.min(n1, n2)];
		long[] values = new long[Math.min(n1, n2)];
		int ptr = 0;
		int i = 0, j = 0;
		long totalSize = 0;
		while (i < n1 && j < n2) {
			Entry<E> e1 = list1.get(i);
			Entry<E> e2 = list2.get(j);
			int c = compare(e1.key, e2.key);
			if (c == 0) {
				long minCount = Math.min(e1.value, e2.value);
				if (minCount > 0) {
					keys[ptr] = e1.key;
					values[ptr] = minCount;
					totalSize += minCount;
					ptr++;
				}
				i++;
				j++;
			} else if (c < 0) {
				i++;
			} else {
				j++;
			}
		}
		E[] finalKeys = java.util.Arrays.copyOf(keys, ptr);
		long[] finalValues = java.util.Arrays.copyOf(values, ptr);
		Node<E> newRoot = buildFromSorted(finalKeys, finalValues);
		return new TreapMultiSet<>(newRoot, totalSize, this.comp);
	}

	private Node<E> buildFromSorted(E[] keys, long[] values) {
		int n = keys.length;
		if (n == 0) return null;
		@SuppressWarnings("unchecked")
		Node<E>[] stack = new Node[n];
		int sp = 0;
		for (int i = 0; i < n; i++) {
			Node<E> curr = new Node<>(keys[i], values[i]);
			Node<E> last = null;
			while (sp > 0 && stack[sp - 1].priority < curr.priority) {
				last = stack[--sp];
				recalc(last);
			}
			if (last != null) {
				curr.left = last;
			}
			if (sp > 0) {
				stack[sp - 1].right = curr;
			}
			stack[sp++] = curr;
		}
		while (sp > 0) {
			recalc(stack[--sp]);
		}
		return stack[0];
	}

	private Node<E> rotateRight(Node<E> y) {
		Node<E> x = y.left;
		y.left = x.right;
		x.right = y;
		recalc(y);
		recalc(x);
		return x;
	}

	private Node<E> rotateLeft(Node<E> x) {
		Node<E> y = x.right;
		x.right = y.left;
		y.left = x;
		recalc(x);
		recalc(y);
		return y;
	}

	private Node<E> put(Node<E> node, E key, long val) {
		if (node == null) {
			return new Node<>(key, val);
		}
		int c = compare(key, node.key);
		if (c < 0) {
			node.left = put(node.left, key, val);
			if (node.left.priority > node.priority) {
				node = rotateRight(node);
			}
		} else if (c > 0) {
			node.right = put(node.right, key, val);
			if (node.right.priority > node.priority) {
				node = rotateLeft(node);
			}
		} else {
			node.val = val;
		}
		recalc(node);
		return node;
	}

	/**
	 * 要素を {@code repeat} 回追加します。
	 * {@code repeat} が負の場合は削除を意味しますが、操作後の個数が負になる場合は {@link AssertionError} を投げます。
	 *
	 * @param element 追加する要素
	 * @param repeat 追加する回数
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public void add(E element, long repeat) {
		if (repeat == 0) return;
		size += repeat;
		long num = count(element) + repeat;
		if (num < 0) throw new AssertionError();
		if (num == 0) removeKey(element);
		else root = put(root, element, num);
	}

	/**
	 * 要素を1つ追加します。
	 *
	 * @param element 追加する要素
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public void add(E element) {
		add(element, 1);
	}

	private Node<E> removeNode(Node<E> node, E key) {
		if (node == null) return null;
		int c = compare(key, node.key);
		if (c < 0) {
			node.left = removeNode(node.left, key);
		} else if (c > 0) {
			node.right = removeNode(node.right, key);
		} else {
			if (node.left == null && node.right == null) {
				return null;
			} else if (node.left == null) {
				node = rotateLeft(node);
				node.left = removeNode(node.left, key);
			} else if (node.right == null) {
				node = rotateRight(node);
				node.right = removeNode(node.right, key);
			} else {
				if (node.left.priority > node.right.priority) {
					node = rotateRight(node);
					node.right = removeNode(node.right, key);
				} else {
					node = rotateLeft(node);
					node.left = removeNode(node.left, key);
				}
			}
		}
		recalc(node);
		return node;
	}

	private void removeKey(E key) {
		root = removeNode(root, key);
	}

	/**
	 * 要素を1つ削除します。
	 *
	 * @param element 削除する要素
	 * @return 削除に成功した（もともと要素が存在した）場合は {@code true}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public boolean remove(E element) {
		return remove(element, 1);
	}

	/**
	 * 要素を最大 {@code repeat} 回削除します。
	 *
	 * @param element 削除する要素
	 * @param repeat 削除する回数
	 * @return もともと入っていた要素数が {@code repeat} 以上の場合は {@code true}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public boolean remove(E element, long repeat) {
		long num = count(element);
		boolean ret = num >= repeat;
		repeat = Math.min(repeat, num);
		size -= repeat;
		if (num == repeat) removeKey(element);
		else root = put(root, element, num - repeat);
		return ret;
	}

	/**
	 * キーと要素の個数を保持するエントリークラス。
	 *
	 * @param <E> 要素の型
	 */
	public static class Entry<E> {
		public final E key;
		public final long value;

		public Entry(E key, long value) {
			this.key = key;
			this.value = value;
		}
	}

	/**
	 * キーが要素、値が要素の個数であるエントリーのリストを返します。
	 *
	 * @return エントリーのリスト
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	public List<Entry<E>> entryList() {
		List<Entry<E>> res = new ArrayList<>();
		inOrderEntries(root, res);
		return res;
	}

	private void inOrderEntries(Node<E> node, List<Entry<E>> res) {
		if (node == null) return;
		inOrderEntries(node.left, res);
		res.add(new Entry<>(node.key, node.val));
		inOrderEntries(node.right, res);
	}

	/**
	 * 重複を込めた総要素数を返します。
	 *
	 * @return 総要素数
	 * @complexity O(1)
	 */
	// 未テスト
	public long size() {
		return size;
	}

	/**
	 * マルチセットを空にします。
	 *
	 * @complexity O(1)
	 */
	// 未テスト
	public void clear() {
		root = null;
		size = 0;
	}

	/**
	 * 異なる要素の数を返します。
	 *
	 * @return 異なる要素の数
	 * @complexity O(1)
	 */
	// 未テスト
	public int numberDistinctElements() {
		return root == null ? 0 : root.size;
	}

	/**
	 * 集合が空であるかどうかを返します。
	 *
	 * @return 空であれば {@code true}
	 * @complexity O(1)
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 最小の要素を1つ取り出して削除します。
	 *
	 * @return 最小の要素（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E pollFirst() {
		E e = peekFirst();
		if (e != null) remove(e);
		return e;
	}

	/**
	 * 最大の要素を1つ取り出して削除します。
	 *
	 * @return 最大の要素（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E pollLast() {
		E e = peekLast();
		if (e != null) remove(e);
		return e;
	}

	/**
	 * 最小の要素を返します（削除はしません）。
	 *
	 * @return 最小の要素（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E peekFirst() {
		if (root == null) return null;
		Node<E> node = root;
		while (node.left != null) {
			node = node.left;
		}
		return node.key;
	}

	/**
	 * 最大の要素を返します（削除はしません）。
	 *
	 * @return 最大の要素（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E peekLast() {
		if (root == null) return null;
		Node<E> node = root;
		while (node.right != null) {
			node = node.right;
		}
		return node.key;
	}

	/**
	 * 指定された値より厳密に小さい最大の要素を返します。
	 *
	 * @param v 値
	 * @return 存在するならその要素、存在しなければ {@code null}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E lower(E v) {
		Node<E> node = root;
		E res = null;
		while (node != null) {
			int c = compare(node.key, v);
			if (c < 0) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定された値より厳密に大きい最小の要素を返します。
	 *
	 * @param v 値
	 * @return 存在するならその要素、存在しなければ {@code null}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E higher(E v) {
		Node<E> node = root;
		E res = null;
		while (node != null) {
			int c = compare(node.key, v);
			if (c > 0) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * 指定された値以下の最大の要素を返します。
	 *
	 * @param v 値
	 * @return 存在するならその要素、存在しなければ {@code null}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E floor(E v) {
		Node<E> node = root;
		E res = null;
		while (node != null) {
			int c = compare(node.key, v);
			if (c == 0) return node.key;
			if (c < 0) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定された値以上の最小の要素を返します。
	 *
	 * @param v 値
	 * @return 存在するならその要素、存在しなければ {@code null}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E ceil(E v) {
		Node<E> node = root;
		E res = null;
		while (node != null) {
			int c = compare(node.key, v);
			if (c == 0) return node.key;
			if (c > 0) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * 最大の要素のエントリーを返します。
	 *
	 * @return 最大の要素のエントリー（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public Entry<E> peekLastEntry() {
		if (root == null) return null;
		Node<E> node = root;
		while (node.right != null) {
			node = node.right;
		}
		return new Entry<>(node.key, node.val);
	}

	/**
	 * 最小の要素のエントリーを返します。
	 *
	 * @return 最小の要素のエントリー（空の場合は {@code null}）
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public Entry<E> peekFirstEntry() {
		if (root == null) return null;
		Node<E> node = root;
		while (node.left != null) {
			node = node.left;
		}
		return new Entry<>(node.key, node.val);
	}

	/**
	 * 指定された要素の個数を返します。
	 *
	 * @param element 要素
	 * @return 個数
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public long count(E element) {
		Node<E> node = root;
		while (node != null) {
			int c = compare(element, node.key);
			if (c == 0) return node.val;
			if (c < 0) node = node.left;
			else node = node.right;
		}
		return 0L;
	}

	/**
	 * 指定された要素が含まれているかどうかを返します。
	 *
	 * @param element 要素
	 * @return 含まれていれば {@code true}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public boolean contains(E element) {
		return count(element) > 0;
	}

	/**
	 * 指定された値以下の要素の総数を返します。
	 *
	 * @param element 値
	 * @return 総数
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public long countLeq(E element) {
		Node<E> node = root;
		long res = 0;
		while (node != null) {
			int c = compare(node.key, element);
			if (c <= 0) {
				res += node.val;
				if (node.left != null) res += node.left.sum;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定された値以上の要素の総数を返します。
	 *
	 * @param element 値
	 * @return 総数
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public long countGeq(E element) {
		Node<E> node = root;
		long res = 0;
		while (node != null) {
			int c = compare(node.key, element);
			if (c >= 0) {
				res += node.val;
				if (node.right != null) res += node.right.sum;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * 重複を込めて昇順に並べたときの {@code k} 番目の要素を返します。
	 *
	 * @param k インデックス (0-origin)
	 * @return {@code k} 番目の要素。存在しなければ {@code null}
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public E kthKey(long k) {
		if (k < 0 || k >= size) return null;
		Node<E> node = root;
		while (true) {
			if (node.left != null) {
				if (k < node.left.sum) {
					node = node.left;
					continue;
				} else {
					k -= node.left.sum;
				}
			}
			if (k < node.val) return node.key;
			k -= node.val;
			node = node.right;
		}
	}

	/**
	 * 集合の状態を文字列として表します。
	 *
	 * @return 集合の状態を表す文字列
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	@Override
	public String toString() {
		if (isEmpty()) {
			return "空集合";
		} else {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (var entry : entryList()) {
				if (!first) sb.append("\n");
				sb.append(entry.key).append(" が ").append(entry.value).append("個");
				first = false;
			}
			return sb.toString();
		}
	}

	/**
	 * 集合の状態を標準出力にダンプします。
	 *
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	public void dump() {
		System.out.println(toString());
	}

	/**
	 * 分割操作の結果を保持するクラス。
	 * {@code less()} は指定された値未満の要素を含むマルチセット、
	 * {@code greaterOrEqual()} は指定された値以上の要素を含むマルチセットを返します。
	 *
	 * @param <E> 要素の型
	 */
	public static class Split<E extends Comparable<? super E>> {
		private final TreapMultiSet<E> less;
		private final TreapMultiSet<E> greaterOrEqual;

		/**
		 * 新しい Split インスタンスを構築します。
		 *
		 * @param less 指定値未満の要素を含むマルチセット
		 * @param greaterOrEqual 指定値以上の要素を含むマルチセット
		 */
		// 未テスト
		public Split(TreapMultiSet<E> less, TreapMultiSet<E> greaterOrEqual) {
			this.less = less;
			this.greaterOrEqual = greaterOrEqual;
		}

		/**
		 * 指定された値未満の要素を含むマルチセットを返します。
		 *
		 * @return 指定された値未満の要素を含むマルチセット
		 * @complexity O(1)
		 */
		// 未テスト
		public TreapMultiSet<E> less() {
			return less;
		}

		/**
		 * 指定された値以上の要素を含むマルチセットを返します。
		 *
		 * @return 指定された値以上の要素を含むマルチセット
		 * @complexity O(1)
		 */
		// 未テスト
		public TreapMultiSet<E> greaterOrEqual() {
			return greaterOrEqual;
		}
	}

	private Node<E>[] split(Node<E> node, E key) {
		if (node == null) {
			@SuppressWarnings("unchecked")
			Node<E>[] res = new Node[2];
			return res;
		}
		int c = compare(node.key, key);
		if (c < 0) {
			Node<E>[] a = split(node.right, key);
			node.right = a[0];
			recalc(node);
			@SuppressWarnings("unchecked")
			Node<E>[] res = (Node<E>[]) new Node<?>[] { node, a[1] };
			return res;
		} else {
			Node<E>[] a = split(node.left, key);
			node.left = a[1];
			recalc(node);
			@SuppressWarnings("unchecked")
			Node<E>[] res = (Node<E>[]) new Node<?>[] { a[0], node };
			return res;
		}
	}

	/**
	 * このマルチセットを、指定された値未満の要素と以上の要素の2つに分割します。
	 * 分割後の元のマルチセットは空になります。
	 *
	 * @param value 分割のしきい値となる値
	 * @return 分割結果を保持する {@link Split} オブジェクト
	 * @complexity O(log N) (N は異なる要素の数)
	 */
	// 未テスト
	public Split<E> splitByValue(E value) {
		Node<E>[] splitNodes = split(root, value);
		long sizeL = splitNodes[0] == null ? 0 : splitNodes[0].sum;
		long sizeR = splitNodes[1] == null ? 0 : splitNodes[1].sum;
		TreapMultiSet<E> less = new TreapMultiSet<>(splitNodes[0], sizeL, comp);
		TreapMultiSet<E> greaterOrEqual = new TreapMultiSet<>(splitNodes[1], sizeR, comp);
		clear();
		return new Split<>(less, greaterOrEqual);
	}

	private Node<E> merge(Node<E> l, Node<E> r) {
		if (l == null) return r;
		if (r == null) return l;
		if (l.priority > r.priority) {
			l.right = merge(l.right, r);
			recalc(l);
			return l;
		} else {
			r.left = merge(l, r.left);
			recalc(r);
			return r;
		}
	}

	/**
	 * 2つの相異なる値を持つマルチセットをマージし、1つの新しいマルチセットを返します。
	 * {@code left} のすべての要素が {@code right} のすべての要素より小さい必要があります。
	 * この操作は破壊的であり、引数として渡されたマルチセットは空になります。
	 *
	 * @param <E> 要素の型
	 * @param left 結合対象の左側のマルチセット
	 * @param right 結合対象の右側のマルチセット
	 * @return 結合された新しいマルチセット
	 * @complexity O(log(N_1 + N_2)) (N_1, N_2 はそれぞれの異なる要素の数)
	 */
	// 未テスト
	public static <E extends Comparable<? super E>> TreapMultiSet<E> join(TreapMultiSet<E> left, TreapMultiSet<E> right) {
		Comparator<? super E> comp = left != null ? left.comp : (right != null ? right.comp : null);
		if (left == null) left = new TreapMultiSet<E>(comp);
		if (right == null) right = new TreapMultiSet<E>(comp);
		Node<E> rootL = left.root;
		Node<E> rootR = right.root;
		long mergedSize = (rootL == null ? 0 : rootL.sum) + (rootR == null ? 0 : rootR.sum);
		Node<E> mergedRoot = left.merge(rootL, rootR);
		TreapMultiSet<E> res = new TreapMultiSet<>(mergedRoot, mergedSize, comp);
		left.clear();
		right.clear();
		return res;
	}

	/**
	 * このマルチセットと別のオブジェクトの同値性を判定します。
	 * 内部の各要素とその個数が一致する場合に同値とみなします。
	 *
	 * @param obj 比較対象のオブジェクト
	 * @return 同値であれば true, そうでなければ false
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TreapMultiSet)) return false;
		TreapMultiSet<?> other = (TreapMultiSet<?>) obj;
		if (this.size != other.size) return false;
		if (this.numberDistinctElements() != other.numberDistinctElements()) return false;

		List<Entry<E>> thisEntries = this.entryList();
		List<?> otherEntries = other.entryList();
		if (thisEntries.size() != otherEntries.size()) return false;
		for (int i = 0; i < thisEntries.size(); i++) {
			Entry<E> e1 = thisEntries.get(i);
			Object o2 = otherEntries.get(i);
			if (!(o2 instanceof Entry)) return false;
			Entry<?> e2 = (Entry<?>) o2;
			if (e1.key == null ? e2.key != null : !e1.key.equals(e2.key)) return false;
			if (e1.value != e2.value) return false;
		}
		return true;
	}

	/**
	 * このマルチセットのハッシュコードを計算します。
	 *
	 * @return ハッシュコード
	 * @complexity O(N) (N は異なる要素の数)
	 */
	// 未テスト
	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + Long.hashCode(size);
		for (Entry<E> e : entryList()) {
			result = 31 * result + (e.key != null ? e.key.hashCode() : 0);
			result = 31 * result + Long.hashCode(e.value);
		}
		return result;
	}
}
