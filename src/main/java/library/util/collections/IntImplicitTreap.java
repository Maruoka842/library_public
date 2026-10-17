package library.util.collections;

import java.util.Random;

/**
 * {@code int} 型に特殊化した Implicit Treap (配列の割り込み・削除・ランダムアクセスを対数時間で行う平衡二分木)。
 *
 * 未テスト
 */
public class IntImplicitTreap {
	/** 乱数生成器 */
	Random rnd = new Random();
	/** 要素数 */
	int size = 0;

	/**
	 * Treapのノードを表すクラス。
	 *
	 * 未テスト
	 */
	public class Node {
		/** 格納されている値 */
		int key;
		/** 左の子ノード */
		Node left;
		/** 右の子ノード */
		Node right;
		/** 部分木のサイズ */
		int size = 1;
		/** ヒープ条件のための優先度 */
		int priority;

		/**
		 * 指定された値を持つノードを構築します。
		 *
		 * @param key 格納する値
		 * 計算量: O(1)
		 * 未テスト
		 */
		public Node(int key) {
			this.key = key;
			this.priority = rnd.nextInt();
			recalc();
		}

		/**
		 * 部分木のサイズを再計算します。
		 *
		 * 計算量: O(1)
		 * 未テスト
		 */
		public void recalc() {
			size = 1;
			if (left != null) {
				size += left.size;
			}
			if (right != null) {
				size += right.size;
			}
		}

		/**
		 * 部分木のサイズを取得します。
		 *
		 * @return 部分木のサイズ
		 * 計算量: O(1)
		 * 未テスト
		 */
		public int size() {
			return size;
		}

		/**
		 * ノードに格納されている値を手に入れます。
		 *
		 * @return 格納されている値
		 * 計算量: O(1)
		 * 未テスト
		 */
		public int key() {
			return key;
		}
	}

	/** 根ノード */
	private Node root;

	/**
	 * 2つの Treap を結合します。
	 *
	 * @param l 左側の Treap の根ノード
	 * @param r 右側の Treap の根ノード
	 * @return 結合された Treap の根ノード
	 * 計算量: O(log N)
	 * 未テスト
	 */
	public Node merge(Node l, Node r) {
		if (l == null)
			return r;
		if (r == null)
			return l;
		if (l.priority > r.priority) {
			l.right = merge(l.right, r);
			l.recalc();
			return l;
		} else {
			r.left = merge(l, r.left);
			r.recalc();
			return r;
		}
	}

	/**
	 * 分割結果を保持するペア構造体。
	 *
	 * 未テスト
	 */
	public class Pair {
		/** 先頭 k 個の要素を含む Treap */
		private final Node left;
		/** 残りの要素を含む Treap */
		private final Node right;

		/**
		 * 分割ペアを構築します。
		 *
		 * @param left 左側の Treap
		 * @param right 右側の Treap
		 * 計算量: O(1)
		 * 未テスト
		 */
		public Pair(Node left, Node right) {
			this.left = left;
			this.right = right;
		}

		/**
		 * 左側の Treap を取得します。
		 *
		 * @return 左側の Treap の根ノード
		 * 計算量: O(1)
		 * 未テスト
		 */
		public Node left() {
			return left;
		}

		/**
		 * 右側の Treap を取得します。
		 *
		 * @return 右側の Treap の根ノード
		 * 計算量: O(1)
		 * 未テスト
		 */
		public Node right() {
			return right;
		}
	}

	/**
	 * 指定したノードを先頭 k 個とそれ以外に分割します。
	 *
	 * @param node 分割する Treap の根
	 * @param k 分割位置（先頭からの要素数）
	 * @return 分割された左右の Treap ペア
	 * 計算量: O(log N)
	 * 未テスト
	 */
	Pair split(Node node, int k) {
		if (node == null) {
			return new Pair(null, null);
		}
		if (size(node.left) < k) {
			var a = split(node.right, k - size(node.left) - 1);
			node.right = a.left();
			node.recalc();
			return new Pair(node, a.right());
		} else {
			var a = split(node.left, k);
			node.left = a.right();
			node.recalc();
			return new Pair(a.left(), node);
		}
	}

	/**
	 * 指定した位置 (0-indexed) に要素を挿入します。
	 *
	 * @param pos 挿入位置 (0 <= pos <= size)
	 * @param val 挿入する値
	 * @return 挿入されたノード
	 * 計算量: O(log N)
	 * 未テスト
	 */
	public Node insert(int pos, int val) {
		if (pos < 0 || pos > size) throw new IndexOutOfBoundsException("pos: " + pos + ", size: " + size);
		Node node = new Node(val);
		Pair p = split(root, pos);
		root = merge(merge(p.left(), node), p.right());
		size++;
		return node;
	}

	/**
	 * 指定した位置 (0-indexed) の要素を削除します。
	 *
	 * @param pos 削除位置 (0 <= pos < size)
	 * 計算量: O(log N)
	 * 未テスト
	 */
	public void erase(int pos) {
		if (pos < 0 || pos >= size) throw new IndexOutOfBoundsException("pos: " + pos + ", size: " + size);
		Pair p1 = split(root, pos);
		Pair p2 = split(p1.right(), 1);
		root = merge(p1.left(), p2.right());
		--size;
	}

	/**
	 * 要素数を取得します。
	 *
	 * @return 要素数
	 * 計算量: O(1)
	 * 未テスト
	 */
	public int size() {
		return size;
	}

	/**
	 * 平衡二分探索木を空にします。
	 *
	 * 計算量: O(1)
	 * 未テスト
	 */
	public void clear() {
		root = null;
		size = 0;
	}

	/**
	 * トリープが空であるかどうかを判定します。
	 *
	 * @return 要素数が0であれば true、そうでなければ false
	 * 計算量: O(1)
	 * 未テスト
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 根ノードを取得します。
	 *
	 * @return 根ノード
	 * 計算量: O(1)
	 * 未テスト
	 */
	public Node root() {
		return root;
	}

	private int size(Node node) {
		return node == null ? 0 : node.size;
	}

	/**
	 * 先頭から k 番目 (0-indexed) の要素を取得します。
	 *
	 * @param k 要素のインデックス (0 <= k < size)
	 * @return k 番目の要素の値
	 * 計算量: O(log N)
	 * 未テスト
	 */
	public int kth(int k) {
		if (k < 0 || k >= size)
			throw new IndexOutOfBoundsException("k: " + k + ", size: " + size);
		Node node = root;
		while (true) {
			if (node.left != null) {
				if (k < node.left.size) {
					node = node.left;
					continue;
				} else {
					k -= node.left.size;
				}
			}
			if (k == 0)
				return node.key;
			k--;
			node = node.right;
		}
	}

	/**
	 * トリープの文字列表現を取得します。
	 *
	 * @return 要素を空白区切りで並べた文字列
	 * 計算量: O(N)
	 * 未テスト
	 */
	@Override
	public String toString() {
		return toString(root);
	}

	private String toString(Node node) {
		if (node == null)
			return "";
		return toString(node.left) + node.key + " " + toString(node.right);
	}

	/**
	 * デバッグ用に集合の内容をインデックス順に標準出力に出力します。
	 *
	 * 計算量: O(N)
	 * 未テスト
	 */
	public void dump() {
		if (root == null) {
			System.out.println("空集合");
		} else {
			dump(root, 0);
		}
	}

	private int dump(Node node, int index) {
		if (node == null) return index;
		int nextIndex = dump(node.left, index);
		System.out.println(nextIndex + ": " + node.key);
		return dump(node.right, nextIndex + 1);
	}

	/**
	 * 要素をインデックス順に primitive int 配列として返します。
	 *
	 * @return 全要素を格納した配列
	 * 計算量: O(N)
	 * 未テスト
	 */
	public int[] toArray() {
		int[] ret = new int[size];
		toArray(root, ret, 0);
		return ret;
	}

	private int toArray(Node node, int[] ret, int pos) {
		if (node == null)
			return pos;

		pos = toArray(node.left, ret, pos);
		ret[pos++] = node.key;
		return toArray(node.right, ret, pos);
	}
}
