package library.util.collections;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;

/**
 * {@code double} に特殊化した Treap ベースの順序付き集合。
 *
 * <ul>
 * <li>キーに関して二分探索木の順序を満たす</li>
 * <li>優先度(priority)に関して最大ヒープの性質を満たす</li>
 * </ul>
 */
// 未テスト
public class DoubleTreapSet implements Iterable<Double> {
	final static Random rnd = new Random();
	private int size = 0;

	public class Node {
		double key;
		int priority;
		Node left, right;
		int size = 1;

		public Node(double key) {
			this.key = key;
			this.priority = rnd.nextInt();
			recalc();
		}

		public void recalc() {
			size = 1;
			if (left != null) {
				size += left.size;
			}
			if (right != null) {
				size += right.size;
			}
		}

		public int size() {
			return size;
		}

		public double key() {
			return key;
		}
	}

	private Node root;

	private Node rotateRight(Node y) {
		Node x = y.left;
		y.left = x.right;
		x.right = y;
		y.recalc();
		x.recalc();
		return x;
	}

	private Node rotateLeft(Node x) {
		Node y = x.right;
		x.right = y.left;
		y.left = x;
		x.recalc();
		y.recalc();
		return y;
	}

	private Node add(Node node, double key) {
		if (node == null) {
			++size;
			return new Node(key);
		}
		int cmp = Double.compare(key, node.key);
		if (cmp < 0) {
			node.left = add(node.left, key);
			if (node.left.priority > node.priority) {
				node = rotateRight(node);
			}
		} else if (cmp > 0) {
			node.right = add(node.right, key);
			if (node.right.priority > node.priority) {
				node = rotateLeft(node);
			}
		}
		node.recalc();
		return node;
	}

	/**
	 * 指定キーを集合に追加します。
	 *
	 * @param key 追加するキー
	 * O(log N)
	 */
	// 未テスト
	public void add(double key) {
		root = add(root, key);
	}

	public boolean contains(Node node, double key) {
		if (node == null) {
			return false;
		}
		int cmp = Double.compare(key, node.key);
		if (cmp == 0) {
			return true;
		}
		return cmp < 0 ? contains(node.left, key) : contains(node.right, key);
	}

	/**
	 * 集合に指定キーが含まれているか判定します。
	 *
	 * @param key キー
	 * @return 含まれていれば true
	 * O(log N)
	 */
	// 未テスト
	public boolean contains(double key) {
		return contains(root, key);
	}

	private Node remove(Node node, double key) {
		if (node == null) {
			return null;
		}
		int cmp = Double.compare(key, node.key);
		if (cmp < 0) {
			node.left = remove(node.left, key);
		} else if (cmp > 0) {
			node.right = remove(node.right, key);
		} else {
			if (node.left == null && node.right == null) {
				--size;
				return null;
			} else if (node.left == null) {
				node = rotateLeft(node);
				node.left = remove(node.left, key);
			} else if (node.right == null) {
				node = rotateRight(node);
				node.right = remove(node.right, key);
			} else {
				if (node.left.priority > node.right.priority) {
					node = rotateRight(node);
					node.right = remove(node.right, key);
				} else {
					node = rotateLeft(node);
					node.left = remove(node.left, key);
				}
			}
		}
		node.recalc();
		return node;
	}

	/**
	 * 指定キーを集合から削除します。
	 *
	 * @param key 削除するキー
	 * O(log N)
	 */
	// 未テスト
	public void remove(double key) {
		root = remove(root, key);
	}

	/**
	 * セットを空にします。
	 *
	 * O(1)
	 */
	// 未テスト
	public void clear() {
		root = null;
		size = 0;
	}

	/**
	 * 要素数を返します。
	 *
	 * @return 要素数
	 * O(1)
	 */
	// 未テスト
	public int size() {
		return size;
	}

	/**
	 * 集合が空かどうかを判定します。
	 *
	 * @return 集合が空であれば true、そうでなければ false
	 * O(1)
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 集合内の最小要素を取得します。
	 *
	 * @return 集合内の最小要素（集合が空の場合は null）
	 * O(log N)
	 */
	// 未テスト
	public Double first() {
		if (root == null) {
			return null;
		}
		Node node = root;
		while (node.left != null) {
			node = node.left;
		}
		return node.key;
	}

	/**
	 * 集合内の最大要素を取得します。
	 *
	 * @return 集合内の最大要素（集合が空の場合は null）
	 * O(log N)
	 */
	// 未テスト
	public Double last() {
		if (root == null) {
			return null;
		}
		Node node = root;
		while (node.right != null) {
			node = node.right;
		}
		return node.key;
	}

	/**
	 * 集合内の最小要素を取得し、集合から削除します。
	 *
	 * @return 削除された最小要素（集合が空の場合は null）
	 * O(log N)
	 */
	// 未テスト
	public Double pollFirst() {
		Double res = first();
		if (res != null) {
			remove(res);
		}
		return res;
	}

	/**
	 * 集合内の最大要素を取得し、集合から削除します。
	 *
	 * @return 削除された最大要素（集合が空の場合は null）
	 * O(log N)
	 */
	// 未テスト
	public Double pollLast() {
		Double res = last();
		if (res != null) {
			remove(res);
		}
		return res;
	}

	/**
	 * k 番目に小さい要素を取得します（0-origin）。存在しなければ null。
	 *
	 * @param k インデックス
	 * @return k 番目に小さい要素
	 * O(log N)
	 */
	// 未テスト
	public Double kth(int k) {
		if (k < 0 || k >= size) {
			return null;
		}
		Node node = root;
		while (true) {
			if (node.left != null) {
				if (k < node.left.size) {
					node = node.left;
					continue;
				}
				k -= node.left.size;
			}
			if (k == 0) {
				return node.key;
			}
			k--;
			node = node.right;
		}
	}

	/**
	 * 指定キー以下の最大の値を取得します。存在しなければ null。
	 *
	 * @param key キー
	 * @return 指定キー以下の最大値
	 * O(log N)
	 */
	// 未テスト
	public Double floor(double key) {
		Node node = root;
		Double res = null;
		while (node != null) {
			int cmp = Double.compare(node.key, key);
			if (cmp == 0) {
				return node.key;
			}
			if (cmp < 0) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定キー未満の最大の値を取得します。存在しなければ null。
	 *
	 * @param key キー
	 * @return 指定キー未満の最大値
	 * O(log N)
	 */
	// 未テスト
	public Double lower(double key) {
		Node node = root;
		Double res = null;
		while (node != null) {
			if (Double.compare(node.key, key) < 0) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定キーより大きい最小の値を取得します。存在しなければ null。
	 *
	 * @param key キー
	 * @return 指定キーより大きい最小値
	 * O(log N)
	 */
	// 未テスト
	public Double higher(double key) {
		Node node = root;
		Double res = null;
		while (node != null) {
			if (Double.compare(node.key, key) > 0) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * 指定キー以上の最小の値を取得します。存在しなければ null。
	 *
	 * @param key キー
	 * @return 指定キー以上の最小値
	 * O(log N)
	 */
	// 未テスト
	public Double ceil(double key) {
		Node node = root;
		Double res = null;
		while (node != null) {
			int cmp = Double.compare(node.key, key);
			if (cmp == 0) {
				return node.key;
			}
			if (cmp > 0) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * key 以下のキーの個数を返します。
	 *
	 * @param key キー
	 * @return key 以下のキー数
	 * O(log N)
	 */
	// 未テスト
	public int countLeq(double key) {
		Node node = root;
		int res = 0;
		while (node != null) {
			if (Double.compare(node.key, key) <= 0) {
				res += 1;
				if (node.left != null) {
					res += node.left.size;
				}
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * key 未満のキーの個数を返します。
	 *
	 * @param key キー
	 * @return key 未満のキー数
	 * O(log N)
	 */
	// 未テスト
	public int countLess(double key) {
		return countLess(root, key);
	}

	/**
	 * key 以上のキーの個数を返します。
	 *
	 * @param key キー
	 * @return key 以上のキー数
	 * O(log N)
	 */
	// 未テスト
	public int countGeq(double key) {
		return size - countLess(key);
	}

	/**
	 * key より大きいキーの個数を返します。
	 *
	 * @param key キー
	 * @return key より大きいキー数
	 * O(log N)
	 */
	// 未テスト
	public int countGreater(double key) {
		return size - countLeq(key);
	}

	private int countLess(Node node, double v) {
		if (node == null) {
			return 0;
		}
		if (Double.compare(v, node.key) <= 0) {
			return countLess(node.left, v);
		}
		int leftSize = node.left == null ? 0 : node.left.size;
		return leftSize + 1 + countLess(node.right, v);
	}

	public Node merge(Node l, Node r) {
		if (l == null) {
			return r;
		}
		if (r == null) {
			return l;
		}
		if (l.priority > r.priority) {
			l.right = merge(l.right, r);
			l.recalc();
			return l;
		}
		r.left = merge(l, r.left);
		r.recalc();
		return r;
	}

	public class Pair {
		private final Node left;
		private final Node right;

		public Pair(Node left, Node right) {
			this.left = left;
			this.right = right;
		}

		public Node left() {
			return left;
		}

		public Node right() {
			return right;
		}
	}

	/**
	 * 指定キーで Treap を分割します。
	 *
	 * @param key 分割基準キー
	 * @return Pair(key 未満の Treap, key 以上の Treap)
	 * O(log N)
	 */
	// 未テスト
	public Pair split(double key) {
		return split(root, key);
	}

	Pair split(Node node, double key) {
		if (node == null) {
			return new Pair(null, null);
		}
		if (Double.compare(node.key, key) < 0) {
			var a = split(node.right, key);
			node.right = a.left();
			node.recalc();
			return new Pair(node, a.right());
		}
		var a = split(node.left, key);
		node.left = a.right();
		node.recalc();
		return new Pair(a.left(), node);
	}

	public Node root() {
		return root;
	}

	/**
	 * この集合のコピー（ディープコピー）を返します。
	 *
	 * @return この集合のコピー
	 * O(N)
	 */
	// 未テスト
	public DoubleTreapSet copy() {
		DoubleTreapSet res = new DoubleTreapSet();
		res.size = this.size;
		res.root = res.copyNode(this.root);
		return res;
	}

	private Node copyNode(Node node) {
		if (node == null) {
			return null;
		}
		Node cp = new Node(node.key);
		cp.priority = node.priority;
		cp.size = node.size;
		cp.left = copyNode(node.left);
		cp.right = copyNode(node.right);
		return cp;
	}

	/**
	 * 指定した集合のすべての要素をこの集合に追加します。
	 *
	 * @param a 追加する要素を持つ集合
	 * O(M log N)
	 */
	// 未テスト
	public void addAll(DoubleTreapSet a) {
		addAll(a.root);
	}

	private void addAll(Node node) {
		if (node == null) return;
		add(node.key);
		addAll(node.left);
		addAll(node.right);
	}

	/**
	 * ソートされたキーの配列から、O(N)時間で DoubleTreapSet を構築します。
	 *
	 * @param keys ソート済みのキー配列
	 * @return 構築された DoubleTreapSet
	 * O(N)
	 */
	// 未テスト
	public static DoubleTreapSet buildFromSorted(double[] keys) {
		int n = keys.length;
		DoubleTreapSet set = new DoubleTreapSet();
		if (n == 0) {
			return set;
		}
		Node[] stack = new Node[n];
		int sp = 0;
		for (int i = 0; i < n; i++) {
			Node curr = set.new Node(keys[i]);
			Node last = null;
			while (sp > 0 && stack[sp - 1].priority < curr.priority) {
				last = stack[--sp];
				last.recalc();
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
			stack[--sp].recalc();
		}
		set.root = stack[0];
		set.size = n;
		return set;
	}

	/**
	 * この集合と別の集合の積集合（共通部分）を計算します。
	 *
	 * @param other 比較対象の集合
	 * @return 積集合となる新しい DoubleTreapSet
	 * O(N_1 + N_2)
	 */
	// 未テスト
	public DoubleTreapSet cap(DoubleTreapSet other) {
		int n1 = this.size;
		int n2 = other.size;
		double[] common = new double[Math.min(n1, n2)];
		int ptr = 0;
		PrimitiveIterator.OfDouble it1 = this.iterator();
		PrimitiveIterator.OfDouble it2 = other.iterator();
		if (it1.hasNext() && it2.hasNext()) {
			double v1 = it1.nextDouble();
			double v2 = it2.nextDouble();
			while (true) {
				int cmp = Double.compare(v1, v2);
				if (cmp == 0) {
					common[ptr++] = v1;
					if (!it1.hasNext() || !it2.hasNext()) break;
					v1 = it1.nextDouble();
					v2 = it2.nextDouble();
				} else if (cmp < 0) {
					if (!it1.hasNext()) break;
					v1 = it1.nextDouble();
				} else {
					if (!it2.hasNext()) break;
					v2 = it2.nextDouble();
				}
			}
		}
		double[] keys = java.util.Arrays.copyOf(common, ptr);
		return buildFromSorted(keys);
	}

	/**
	 * 昇順イテレータを取得します。
	 *
	 * @return PrimitiveIterator.OfDouble
	 * O(1)
	 */
	// 未テスト
	@Override
	public PrimitiveIterator.OfDouble iterator() {
		return new PrimitiveIterator.OfDouble() {
			private final ArrayDeque<Node> stack = new ArrayDeque<>();

			{
				pushLeftPath(root);
			}

			private void pushLeftPath(Node node) {
				while (node != null) {
					stack.push(node);
					node = node.left;
				}
			}

			@Override
			public boolean hasNext() {
				return !stack.isEmpty();
			}

			@Override
			public double nextDouble() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				Node node = stack.pop();
				pushLeftPath(node.right);
				return node.key;
			}
		};
	}

	@Override
	public String toString() {
		return toString(root);
	}

	private String toString(Node node) {
		if (node == null) {
			return "";
		}
		return toString(node.left) + node.key + " " + toString(node.right);
	}

	/**
	 * デバッグ用に集合の内容を昇順に標準出力に出力します。
	 *
	 * O(N)
	 */
	// 未テスト
	public void dump() {
		if (root == null) {
			System.out.println("空集合");
		} else {
			dump(root);
		}
	}

	private void dump(Node node) {
		if (node == null) return;
		dump(node.left);
		System.out.println(node.key);
		dump(node.right);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DoubleTreapSet other)) return false;
		if (this.size != other.size) return false;

		PrimitiveIterator.OfDouble it1 = this.iterator();
		PrimitiveIterator.OfDouble it2 = other.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			if (Double.compare(it1.nextDouble(), it2.nextDouble()) != 0) return false;
		}
		return !it1.hasNext() && !it2.hasNext();
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + size;
		PrimitiveIterator.OfDouble it = this.iterator();
		while (it.hasNext()) {
			result = 31 * result + Double.hashCode(it.nextDouble());
		}
		return result;
	}
}
