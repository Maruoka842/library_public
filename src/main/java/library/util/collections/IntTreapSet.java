package library.util.collections;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;

/**
 * {@code int} に特殊化した Treap ベースの順序付き集合。
 *
 * <ul>
 * <li>キーに関して二分探索木の順序を満たす</li>
 * <li>優先度(priority)に関して最大ヒープの性質を満たす</li>
 * </ul>
 * 未テスト
 */
public class IntTreapSet implements Iterable<Integer> {
	final static Random rnd = new Random();
	private int size = 0;

	public class Node {
		int key;
		int priority;
		Node left, right;
		int size = 1;

		public Node(int key) {
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

		public int key() {
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

	private Node add(Node node, int key) {
		if (node == null) {
			++size;
			return new Node(key);
		}
		if (key < node.key) {
			node.left = add(node.left, key);
			if (node.left.priority > node.priority) {
				node = rotateRight(node);
			}
		} else if (key > node.key) {
			node.right = add(node.right, key);
			if (node.right.priority > node.priority) {
				node = rotateLeft(node);
			}
		}
		node.recalc();
		return node;
	}

	public void add(int key) {
		root = add(root, key);
	}

	public boolean contains(Node node, int key) {
		if (node == null) {
			return false;
		}
		if (key == node.key) {
			return true;
		}
		return key < node.key ? contains(node.left, key) : contains(node.right, key);
	}

	public boolean contains(int key) {
		return contains(root, key);
	}

	private Node remove(Node node, int key) {
		if (node == null) {
			return null;
		}
		if (key < node.key) {
			node.left = remove(node.left, key);
		} else if (key > node.key) {
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

	public void remove(int key) {
		root = remove(root, key);
	}

	/**
	 * セットを空にする。
	 * O(1)
	 * // 未テスト
	 */
	public void clear() {
		root = null;
		size = 0;
	}

	public int size() {
		return size;
	}

	/**
	 * 集合が空かどうかを判定します。
	 *
	 * @return 集合が空であれば true、そうでなければ false
	 * @complexity O(1)
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 集合内の最小要素を取得します。
	 *
	 * @return 集合内の最小要素（集合が空の場合は null）
	 * @complexity O(log N)
	 */
	// 未テスト
	public Integer first() {
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
	 * @complexity O(log N)
	 */
	// 未テスト
	public Integer last() {
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
	 * @complexity O(log N)
	 */
	// 未テスト
	public Integer pollFirst() {
		Integer res = first();
		if (res != null) {
			remove(res);
		}
		return res;
	}

	/**
	 * 集合内の最大要素を取得し、集合から削除します。
	 *
	 * @return 削除された最大要素（集合が空の場合は null）
	 * @complexity O(log N)
	 */
	// 未テスト
	public Integer pollLast() {
		Integer res = last();
		if (res != null) {
			remove(res);
		}
		return res;
	}

	/**
	 * k は 0-origin。存在しなければ {@code null}。
	 */
	public Integer kth(int k) {
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
	 * 指定キー以下の最大の値。存在しなければ {@code null}。
	 */
	public Integer floor(int key) {
		Node node = root;
		Integer res = null;
		while (node != null) {
			if (node.key == key) {
				return key;
			}
			if (node.key < key) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定キー未満の最大の値。存在しなければ {@code null}。
	 * @complexity O(log N)
	 */
	public Integer lower(int key) {
		Node node = root;
		Integer res = null;
		while (node != null) {
			if (node.key < key) {
				res = node.key;
				node = node.right;
			} else {
				node = node.left;
			}
		}
		return res;
	}

	/**
	 * 指定キーより大きい最小の値。存在しなければ {@code null}。
	 * @complexity O(log N)
	 */
	public Integer higher(int key) {
		Node node = root;
		Integer res = null;
		while (node != null) {
			if (node.key > key) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * 指定キー以上の最小の値。存在しなければ {@code null}。
	 */
	public Integer ceil(int key) {
		Node node = root;
		Integer res = null;
		while (node != null) {
			if (node.key == key) {
				return key;
			}
			if (node.key > key) {
				res = node.key;
				node = node.left;
			} else {
				node = node.right;
			}
		}
		return res;
	}

	/**
	 * {@code key} 以下のキーの個数を返します。
	 */
	public int countLeq(int key) {
		Node node = root;
		int res = 0;
		while (node != null) {
			if (node.key <= key) {
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
	 * {@code key} 未満のキーの個数を返します。
	 *
	 * @param key 基準とするキー
	 * @return {@code key} 未満のキーの個数
	 * @complexity O(log N)
	 * // 未テスト
	 */
	public int countLess(int key) {
		return countLessHelper(root, key);
	}

	private int countLessHelper(Node node, int v) {
		if (node == null) {
			return 0;
		}
		if (v <= node.key) {
			return countLessHelper(node.left, v);
		}
		int leftSize = node.left == null ? 0 : node.left.size;
		return leftSize + 1 + countLessHelper(node.right, v);
	}

	/**
	 * {@code x} 以上の整数のうち、集合に含まれない最小値を返します。Integer.MAX_VALUE+1のときはnull
	 *
	 * @param x 開始値
	 * @return {@code x} 以上の最小の未含まれる整数（存在しなければ null）
	 * @complexity O(log N)
	 * // 未テスト
	 */
	public Integer firstMissingAtLeast(int x) {
		int rank = countLess(x);
		Integer found = firstMissingAtLeast(root, 0, rank, x);
		if (found != null) {
			return found;
		}
		if ((long) x + size - rank - 1 == Integer.MAX_VALUE) return null;
		return x + size - rank;
	}

	private Integer firstMissingAtLeast(Node node, int indexOffset, int rank, int x) {
		if (node == null) {
			return null;
		}
		int leftSize = node.left == null ? 0 : node.left.size;
		int nodeIndex = indexOffset + leftSize;
		if (nodeIndex < rank) {
			return firstMissingAtLeast(node.right, nodeIndex + 1, rank, x);
		}
		long expectedLong = (long) x + nodeIndex - rank;
		if (expectedLong > Integer.MAX_VALUE) {
			return null;
		}
		int expected = (int) expectedLong;
		if (node.key == expected) {
			return firstMissingAtLeast(node.right, nodeIndex + 1, rank, x);
		}
		Integer leftRes = firstMissingAtLeast(node.left, indexOffset, rank, x);
		if (leftRes != null) {
			return leftRes;
		}
		return expected;
	}

	/**
	 * {@code key} 以上のキーの個数を返します。
	 *
	 * @param key 基準とするキー
	 * @return {@code key} 以上のキーの個数
	 * @complexity O(log N)
	 * // 未テスト
	 */
	public int countGeq(int key) {
		return size - countLess(key);
	}

	/**
	 * {@code key} より大きいキーの個数を返します。
	 *
	 * @param key 基準とするキー
	 * @return {@code key} より大きいキーの個数
	 * @complexity O(log N)
	 * // 未テスト
	 */
	public int countGreater(int key) {
		return size - countLeq(key);
	}

	/**
	 * 集合に含まれない最小の非負整数を返します。
	 *
	 * <p>数学的定義: mex(S) = min { x ∈ ℕ₀ | x ∉ S }</p>
	 *
	 * @return 集合に含まれない最小の非負整数
	 * @complexity O(log N)
	 */
	public int mex() {
		int c = countLeq(-1);
		int res = size - c;
		Node node = root;
		int offset = 0;
		while (node != null) {
			int rank = offset + (node.left == null ? 0 : node.left.size);
			if (node.key < 0) {
				offset = rank + 1;
				node = node.right;
			} else {
				int nonNegRank = rank - c;
				if (node.key > nonNegRank) {
					res = nonNegRank;
					node = node.left;
				} else {
					offset = rank + 1;
					node = node.right;
				}
			}
		}
		return res;
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

	public Pair split(int key) {
		return split(root, key);
	}

	/**
	 * Pair.left = key 未満のノードを持つ Treap。
	 * Pair.right = key 以上のノードを持つ Treap。
	 */
	Pair split(Node node, int key) {
		if (node == null) {
			return new Pair(null, null);
		}
		if (node.key < key) {
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
	 * @complexity O(N)
	 */
	// 未テスト
	public IntTreapSet copy() {
		IntTreapSet res = new IntTreapSet();
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
	
	public void addAll(IntTreapSet a) {
		addAll(a.root);
	}
	
	private void addAll(Node node) {
		if(node==null)return;
		add(node.key);
		addAll(node.left);
		addAll(node.right);
	}

	/**
	 * ソートされたキーの配列から、O(N)時間でIntTreapSetを構築します。
	 *
	 * @param keys ソート済みのキー配列
	 * @return 構築されたIntTreapSet
	 * @complexity O(N) (N は要素数)
	 */
	// 未テスト
	public static IntTreapSet buildFromSorted(int[] keys) {
		int n = keys.length;
		IntTreapSet set = new IntTreapSet();
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
	 * この集合と別の集合 of 積集合（共通部分）を計算します。
	 * 計算量は O(|this| + |other|) です。
	 *
	 * @param other 比較対象の集合
	 * @return 積集合となる新しい IntTreapSet
	 * @complexity O(N_1 + N_2) (N_1, N_2 はそれぞれの集合の要素数)
	 */
	// 未テスト
	public IntTreapSet cap(IntTreapSet other) {
		int n1 = this.size;
		int n2 = other.size;
		int[] common = new int[Math.min(n1, n2)];
		int ptr = 0;
		PrimitiveIterator.OfInt it1 = this.iterator();
		PrimitiveIterator.OfInt it2 = other.iterator();
		if (it1.hasNext() && it2.hasNext()) {
			int v1 = it1.nextInt();
			int v2 = it2.nextInt();
			while (true) {
				if (v1 == v2) {
					common[ptr++] = v1;
					if (!it1.hasNext() || !it2.hasNext()) break;
					v1 = it1.nextInt();
					v2 = it2.nextInt();
				} else if (v1 < v2) {
					if (!it1.hasNext()) break;
					v1 = it1.nextInt();
				} else {
					if (!it2.hasNext()) break;
					v2 = it2.nextInt();
				}
			}
		}
		int[] keys = java.util.Arrays.copyOf(common, ptr);
		return buildFromSorted(keys);
	}
	
	//未テスト
	@Override
	public PrimitiveIterator.OfInt iterator() {
		return new PrimitiveIterator.OfInt() {
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
			public int nextInt() {
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
	 * 未テスト
	 * @complexity O(N) (N は要素数)
	 */
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

	/**
	 * この集合と別のオブジェクトの同値性を判定します。
	 * すべての要素が一致する場合に同値とみなします（順序付き集合のため、同一の要素集合を持つか検証します）。
	 *
	 * <p>計算量: O(N)（N は要素数）</p>
	 *
	 * @param obj 比較対象のオブジェクト
	 * @return 同値であれば true, そうでなければ false
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof IntTreapSet)) return false;
		IntTreapSet other = (IntTreapSet) obj;
		if (this.size != other.size) return false;

		PrimitiveIterator.OfInt it1 = this.iterator();
		PrimitiveIterator.OfInt it2 = other.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			if (it1.nextInt() != it2.nextInt()) return false;
		}
		return !it1.hasNext() && !it2.hasNext();
	}

	/**
	 * この集合のハッシュコードを計算します。
	 *
	 * <p>計算量: O(N)（N は要素数）</p>
	 *
	 * @return ハッシュコード
	 */
	// 未テスト
	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + size;
		PrimitiveIterator.OfInt it = this.iterator();
		while (it.hasNext()) {
			result = 31 * result + Integer.hashCode(it.nextInt());
		}
		return result;
	}

	/**
	 * 要素を昇順に primitive int 配列として返します。
	 *
	 * @return 全要素を昇順に格納した配列
	 * @complexity O(N)
	 */
	// 未テスト
	public int[] toArray() {
		int[] ret = new int[size];
		toArray(root, ret, 0);
		return ret;
	}

	private int toArray(Node node, int[] ret, int pos) {
		if (node == null) {
			return pos;
		}
		pos = toArray(node.left, ret, pos);
		ret[pos++] = node.key;
		return toArray(node.right, ret, pos);
	}
}
