package library.util.collections;

import java.util.Random;
import java.util.function.BinaryOperator;
import library.util.segtree.LazySegTreeStrategy;

/**
 * 配列（列）に対する区間反転、モノイド積の区間集約（非可換対応）、区間遅延作用をサポートする平衡二分木。
 * 
 * 列を A = (A[0], A[1], ..., A[N-1]) とする。
 * 
 * モノイド (Acted, *, e) に対し、集約は非可換な積 A[l] * A[l+1] * ... * A[r-1] を計算する。
 * 区間 [l, r) の反転操作は、部分列 (A[l], A[l+1], ..., A[r-1]) を (A[r-1], ..., A[l+1], A[l]) に並び替える。
 * 非可換なモノイドに対しても、反転後の区間積は反転後の順序に従って正しく計算される。
 * 
 * 作用素モノイド (Acting, composition, id_A) による作用 mapping: Acting x Acted x int -> Acted は、
 * 長さ len の区間の集約値 x に対し、各要素へ作用 f を施した後の集約値を返す。
 * 
 * 計算量: 各操作 expected O(log N)
 * 
 * @param <Acting> 作用素の型
 * @param <Acted> モノイド要素の型
 */
public class ReversibleLazyImplicitTreap<Acting, Acted> {

	/**
	 * 作用を適用するための関数型インターフェース。
	 *
	 * @param <Acting> 作用素の型
	 * @param <Acted> モノイド要素の型
	 */
	@FunctionalInterface
	public interface Mapping<Acting, Acted> {
		/**
		 * 作用を適用する。
		 *
		 * @param action 作用素
		 * @param aggregate 現在の集約値
		 * @param size 部分木のサイズ
		 * @return 作用適用後の集約値
		 */
		Acted apply(Acting action, Acted aggregate, int size);
	}

	/**
	 * モノイド演算および作用素の振る舞いを定義する戦略インターフェース。
	 * {@link LazySegTreeStrategy} を継承し、必要に応じて区間長に応じた作用も定義可能とする。
	 *
	 * @param <Acting> 作用素の型
	 * @param <Acted> モノイド要素の型
	 */
	public interface Strategy<Acting, Acted> extends LazySegTreeStrategy<Acting, Acted> {
		/**
		 * 長さ size の区間の集約値 aggregate に対して作用素 action を適用する。
		 * デフォルトでは mergeAX(action, aggregate) を返す。
		 *
		 * 計算量: O(1)
		 *
		 * @param action 作用素
		 * @param aggregate 集約値
		 * @param size 部分木の要素数
		 * @return 作用適用後の値
		 */
		// 未テスト
		default Acted apply(Acting action, Acted aggregate, int size) {
			return mergeAX(action, aggregate);
		}
	}

	/** 乱数生成器 */
	private final Random rnd = new Random();
	/** 二項演算 */
	private final BinaryOperator<Acted> op;
	/** モノイドの単位元 */
	private final Acted identity;
	/** 作用素の合成演算 */
	private final BinaryOperator<Acting> composition;
	/** 作用素の単位元 */
	private final Acting actionIdentity;
	/** 作用を適用する写像 */
	private final Mapping<Acting, Acted> mapping;

	/** 列の要素数 */
	private int size = 0;
	/** Treap の根ノード */
	private Node<Acting, Acted> root;

	/**
	 * Treap のノードを表すクラス。
	 *
	 * @param <Acting> 作用素の型
	 * @param <Acted> モノイド要素の型
	 */
	public static class Node<Acting, Acted> {
		/** 格納されている値 */
		Acted key;
		/** 部分木の積 (in-order 順の積) */
		Acted prod;
		/** 部分木の反転積 (in-order 逆順の積) */
		Acted revProd;
		/** 遅延評価用作用素 */
		Acting lazy;
		/** 遅延作用素が存在するかどうかのフラグ */
		boolean hasLazy;
		/** 反転フラグ */
		boolean rev;
		/** 左の子ノード */
		Node<Acting, Acted> left;
		/** 右の子ノード */
		Node<Acting, Acted> right;
		/** 部分木のサイズ */
		int size = 1;
		/** ヒープ条件のための優先度 */
		int priority;

		/**
		 * 指定された値と優先度を持つノードを構築する。
		 *
		 * 計算量: O(1)
		 *
		 * @param key 格納する値
		 * @param actionIdentity 作用素の単位元
		 * @param priority 優先度
		 */
		// 未テスト
		public Node(Acted key, Acting actionIdentity, int priority) {
			this.key = key;
			this.prod = key;
			this.revProd = key;
			this.lazy = actionIdentity;
			this.hasLazy = false;
			this.rev = false;
			this.priority = priority;
			this.size = 1;
		}

		/**
		 * 部分木の要素数を取得する。
		 *
		 * 計算量: O(1)
		 *
		 * @return 部分木のサイズ
		 */
		// 未テスト
		public int size() {
			return size;
		}

		/**
		 * ノードに格納されている値を取得する。
		 *
		 * 計算量: O(1)
		 *
		 * @return ノードの値
		 */
		// 未テスト
		public Acted key() {
			return key;
		}
	}

	/**
	 * 分割結果を保持するペア。
	 *
	 * @param <Acting> 作用素の型
	 * @param <Acted> モノイド要素の型
	 */
	public static class Pair<Acting, Acted> {
		/** 左側の部分木 */
		private final Node<Acting, Acted> left;
		/** 右側の部分木 */
		private final Node<Acting, Acted> right;

		/**
		 * 分割ペアを構築する。
		 *
		 * 計算量: O(1)
		 *
		 * @param left 左側の部分木
		 * @param right 右側の部分木
		 */
		// 未テスト
		public Pair(Node<Acting, Acted> left, Node<Acting, Acted> right) {
			this.left = left;
			this.right = right;
		}

		/**
		 * 左側の部分木を取得する。
		 *
		 * 計算量: O(1)
		 *
		 * @return 左側の部分木の根
		 */
		// 未テスト
		public Node<Acting, Acted> left() {
			return left;
		}

		/**
		 * 右側の部分木を取得する。
		 *
		 * 計算量: O(1)
		 *
		 * @return 右側の部分木の根
		 */
		// 未テスト
		public Node<Acting, Acted> right() {
			return right;
		}
	}

	/**
	 * LazySegTreeStrategy を用いて LazyImplicitTreap を構築する。
	 *
	 * 計算量: O(1)
	 *
	 * @param strategy モノイドおよび作用素の振る舞いを定義する戦略
	 */
	// 未テスト
	public ReversibleLazyImplicitTreap(LazySegTreeStrategy<Acting, Acted> strategy) {
		this(
				strategy::mergeX,
				strategy.identityX(),
				strategy::mergeA,
				strategy.identityA(),
				(action, aggregate, size) -> {
					if (strategy instanceof Strategy<Acting, Acted> s) {
						return s.apply(action, aggregate, size);
					}
					return strategy.mergeAX(action, aggregate);
				}
		);
	}

	/**
	 * モノイド演算と作用素を定義して LazyImplicitTreap を構築する。
	 *
	 * 計算量: O(1)
	 *
	 * @param op 二項演算 * : Acted x Acted -> Acted
	 * @param identity 単位元 e in Acted
	 * @param composition 作用素の合成 (f, g) -> f o g (g を適用した後に f を適用)
	 * @param actionIdentity 作用素の単位元 id_A in Acting
	 * @param mapping 作用の適用 mapping: Acting x Acted x int -> Acted
	 */
	// 未テスト
	public ReversibleLazyImplicitTreap(
			BinaryOperator<Acted> op,
			Acted identity,
			BinaryOperator<Acting> composition,
			Acting actionIdentity,
			Mapping<Acting, Acted> mapping) {
		this.op = op;
		this.identity = identity;
		this.composition = composition;
		this.actionIdentity = actionIdentity;
		this.mapping = mapping;
	}

	/**
	 * 2つの Treap を結合する。
	 * l の全要素が r の全要素より前に位置することを前提とする。
	 *
	 * 計算量: expected O(log N)
	 *
	 * @param l 左側の部分木の根
	 * @param r 右側の部分木の根
	 * @return 結合後の部分木の根
	 */
	// 未テスト
	public Node<Acting, Acted> merge(Node<Acting, Acted> l, Node<Acting, Acted> r) {
		if (l == null)
			return r;
		if (r == null)
			return l;
		if (l.priority > r.priority) {
			push(l);
			l.right = merge(l.right, r);
			recalc(l);
			return l;
		} else {
			push(r);
			r.left = merge(l, r.left);
			recalc(r);
			return r;
		}
	}

	/**
	 * 部分木 node を先頭 k 個の要素とそれ以降の要素に分割する。
	 *
	 * 計算量: expected O(log N)
	 *
	 * @param node 分割する部分木の根
	 * @param k 分割位置 (先頭からの要素数)
	 * @return 先頭 k 個からなる部分木と残りの部分木を表す Pair
	 */
	// 未テスト
	public Pair<Acting, Acted> split(Node<Acting, Acted> node, int k) {
		if (node == null) {
			return new Pair<>(null, null);
		}
		push(node);
		if (size(node.left) < k) {
			Pair<Acting, Acted> a = split(node.right, k - size(node.left) - 1);
			node.right = a.left();
			recalc(node);
			return new Pair<>(node, a.right());
		} else {
			Pair<Acting, Acted> a = split(node.left, k);
			node.left = a.right();
			recalc(node);
			return new Pair<>(a.left(), node);
		}
	}

	/**
	 * 位置 pos に要素 val を挿入する。
	 * 挿入前の列を (A[0], ..., A[N-1]) とすると、
	 * 挿入後の列は (A[0], ..., A[pos-1], val, A[pos], ..., A[N-1]) となる。
	 *
	 * 事前条件: 0 <= pos <= N
	 * 例外・未定義条件: pos < 0 または pos > N のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param pos 挿入位置 (0 <= pos <= N)
	 * @param val 挿入する値
	 * @return 挿入されたノード
	 */
	// 未テスト
	public Node<Acting, Acted> insert(int pos, Acted val) {
		if (pos < 0 || pos > size)
			throw new IndexOutOfBoundsException("pos: " + pos + ", size: " + size);
		Node<Acting, Acted> node = new Node<>(val, actionIdentity, rnd.nextInt());
		Pair<Acting, Acted> p = split(root, pos);
		root = merge(merge(p.left(), node), p.right());
		size++;
		return node;
	}

	/**
	 * 位置 pos の要素 A[pos] を削除する。
	 * 削除前の列を (A[0], ..., A[N-1]) とすると、
	 * 削除後の列は (A[0], ..., A[pos-1], A[pos+1], ..., A[N-1]) となる。
	 *
	 * 事前条件: 0 <= pos < N
	 * 例外・未定義条件: pos < 0 または pos >= N のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param pos 削除位置 (0 <= pos < N)
	 */
	// 未テスト
	public void erase(int pos) {
		if (pos < 0 || pos >= size)
			throw new IndexOutOfBoundsException("pos: " + pos + ", size: " + size);
		Pair<Acting, Acted> p1 = split(root, pos);
		Pair<Acting, Acted> p2 = split(p1.right(), 1);
		root = merge(p1.left(), p2.right());
		size--;
	}

	/**
	 * 位置 k (0-indexed) の要素 A[k] を取得する。
	 *
	 * 事前条件: 0 <= k < N
	 * 例外・未定義条件: k < 0 または k >= N のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param k 要素のインデックス (0 <= k < N)
	 * @return A[k]
	 */
	// 未テスト
	public Acted kth(int k) {
		if (k < 0 || k >= size)
			throw new IndexOutOfBoundsException("k: " + k + ", size: " + size);
		Node<Acting, Acted> node = root;
		while (true) {
			push(node);
			int leftSize = size(node.left);
			if (k < leftSize) {
				node = node.left;
			} else if (k == leftSize) {
				return node.key;
			} else {
				k -= leftSize + 1;
				node = node.right;
			}
		}
	}

	/**
	 * 位置 pos の要素の値を val に更新する (A[pos] <- val)。
	 *
	 * 事前条件: 0 <= pos < N
	 * 例外・未定義条件: pos < 0 または pos >= N のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param pos 更新位置 (0 <= pos < N)
	 * @param val 新しい値
	 */
	// 未テスト
	public void set(int pos, Acted val) {
		if (pos < 0 || pos >= size)
			throw new IndexOutOfBoundsException("pos: " + pos + ", size: " + size);
		Pair<Acting, Acted> p1 = split(root, pos);
		Pair<Acting, Acted> p2 = split(p1.right(), 1);
		p2.left().key = val;
		p2.left().prod = val;
		p2.left().revProd = val;
		p2.left().lazy = actionIdentity;
		p2.left().hasLazy = false;
		p2.left().rev = false;
		root = merge(p1.left(), merge(p2.left(), p2.right()));
	}

	/**
	 * 半開区間 [l, r) の要素の並び順を反転する。
	 * (A[l], A[l+1], ..., A[r-1]) を (A[r-1], ..., A[l+1], A[l]) に置き換える。
	 *
	 * 事前条件: 0 <= l <= r <= N
	 * 例外・未定義条件: l < 0 または r > N または l > r のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param l 反転区間の左端 (0 <= l <= r)
	 * @param r 反転区間の右端 (l <= r <= N)
	 */
	// 未テスト
	public void reverse(int l, int r) {
		//https://atcoder.jp/contests/abc350/submissions/81091352
		checkInterval(l, r);
		if (l >= r - 1)
			return;
		if (l == 0 && r == size) {
			allReverse(root);
			return;
		}
		Pair<Acting, Acted> p1 = split(root, l);
		Pair<Acting, Acted> p2 = split(p1.right(), r - l);
		allReverse(p2.left());
		root = merge(p1.left(), merge(p2.left(), p2.right()));
	}

	/**
	 * 半開区間 [l, r) の要素のモノイド積 A[l] * A[l+1] * ... * A[r-1] を計算する。
	 * l = r のときは単位元 e を返す。演算 * は非可換であってもよい。
	 *
	 * 事前条件: 0 <= l <= r <= N
	 * 例外・未定義条件: l < 0 または r > N または l > r のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param l 集約区間の左端 (0 <= l <= r)
	 * @param r 集約区間の右端 (l <= r <= N)
	 * @return 区間積 A[l] * A[l+1] * ... * A[r-1] (l = r のときは e)
	 */
	// 未テスト
	public Acted prod(int l, int r) {
		checkInterval(l, r);
		if (l == r)
			return identity;
		if (l == 0 && r == size)
			return prod(root);
		Pair<Acting, Acted> p1 = split(root, l);
		Pair<Acting, Acted> p2 = split(p1.right(), r - l);
		Acted ans = prod(p2.left());
		root = merge(p1.left(), merge(p2.left(), p2.right()));
		return ans;
	}

	/**
	 * 半開区間 [l, r) の各要素 A[i] (l <= i < r) に対し、作用素 f を適用する (A[i] <- f(A[i]))。
	 * l = r のときは何もしない。
	 *
	 * 事前条件: 0 <= l <= r <= N
	 * 例外・未定義条件: l < 0 または r > N または l > r のとき IndexOutOfBoundsException を送出する。
	 * 計算量: expected O(log N)
	 *
	 * @param l 作用区間の左端 (0 <= l <= r)
	 * @param r 作用区間の右端 (l <= r <= N)
	 * @param f 適用する作用素
	 */
	// 未テスト
	public void apply(int l, int r, Acting f) {
		//https://atcoder.jp/contests/abc350/submissions/81091352
		checkInterval(l, r);
		if (l == r)
			return;
		if (l == 0 && r == size) {
			allApply(root, f);
			return;
		}
		Pair<Acting, Acted> p1 = split(root, l);
		Pair<Acting, Acted> p2 = split(p1.right(), r - l);
		allApply(p2.left(), f);
		root = merge(p1.left(), merge(p2.left(), p2.right()));
	}

	/**
	 * 列の要素数 N = |A| を返す。
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
	 * 列が空 (|A| = 0) であるかを判定する。
	 *
	 * 計算量: O(1)
	 *
	 * @return |A| = 0 ならば true、そうでなければ false
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 列を空 (|A| = 0) にする。
	 *
	 * 副作用: 保持している全要素が破棄される。
	 * 計算量: O(1)
	 */
	// 未テスト
	public void clear() {
		root = null;
		size = 0;
	}

	/**
	 * 根ノードを取得する。
	 *
	 * 計算量: O(1)
	 *
	 * @return 根ノード
	 */
	// 未テスト
	public Node<Acting, Acted> root() {
		return root;
	}

	/**
	 * 列の全要素 (A[0], A[1], ..., A[N-1]) をインデックス順に配列として返す。
	 *
	 * 計算量: O(N)
	 *
	 * @param generator 配列生成関数 (例: String[]::new)
	 * @return 全要素を格納した配列
	 */
	// 未テスト
	public Acted[] toArray(java.util.function.IntFunction<Acted[]> generator) {
		Acted[] ret = generator.apply(size);
		toArray(root, ret, 0);
		return ret;
	}

	private int toArray(Node<Acting, Acted> node, Acted[] ret, int pos) {
		if (node == null)
			return pos;
		push(node);
		pos = toArray(node.left, ret, pos);
		ret[pos++] = node.key;
		return toArray(node.right, ret, pos);
	}

	/**
	 * デバッグ用に列の内容をインデックス順に出力する。
	 *
	 * 計算量: O(N)
	 */
	// 未テスト
	public void dump() {
		if (root == null) {
			System.out.println("空集合");
		} else {
			dump(root, 0);
		}
	}

	private int dump(Node<Acting, Acted> node, int index) {
		if (node == null)
			return index;
		push(node);
		int nextIndex = dump(node.left, index);
		System.out.println(nextIndex + ": " + node.key);
		return dump(node.right, nextIndex + 1);
	}

	/**
	 * 列の全要素を空白区切りで並べた文字列表現を返す。
	 *
	 * 計算量: O(N)
	 *
	 * @return 空白区切りの文字列表現
	 */
	// 未テスト
	@Override
	public String toString() {
		return toString(root);
	}

	private String toString(Node<Acting, Acted> node) {
		if (node == null)
			return "";
		push(node);
		return toString(node.left) + node.key + " " + toString(node.right);
	}

	private void checkInterval(int l, int r) {
		if (l < 0 || r > size || l > r) {
			throw new IndexOutOfBoundsException("Invalid interval [" + l + ", " + r + ") for size " + size);
		}
	}

	private int size(Node<Acting, Acted> node) {
		return node == null ? 0 : node.size;
	}

	private Acted prod(Node<Acting, Acted> node) {
		return node == null ? identity : node.prod;
	}

	private Acted revProd(Node<Acting, Acted> node) {
		return node == null ? identity : node.revProd;
	}

	private void recalc(Node<Acting, Acted> node) {
		if (node == null)
			return;
		node.size = 1 + size(node.left) + size(node.right);
		node.prod = op.apply(op.apply(prod(node.left), node.key), prod(node.right));
		node.revProd = op.apply(op.apply(revProd(node.right), node.key), revProd(node.left));
	}

	private void allReverse(Node<Acting, Acted> node) {
		if (node == null)
			return;
		Node<Acting, Acted> tmp = node.left;
		node.left = node.right;
		node.right = tmp;
		Acted tmpProd = node.prod;
		node.prod = node.revProd;
		node.revProd = tmpProd;
		node.rev = !node.rev;
	}

	private void allApply(Node<Acting, Acted> node, Acting action) {
		if (node == null)
			return;
		node.key = mapping.apply(action, node.key, 1);
		node.prod = mapping.apply(action, node.prod, node.size);
		node.revProd = mapping.apply(action, node.revProd, node.size);
		if (node.hasLazy) {
			node.lazy = composition.apply(action, node.lazy);
		} else {
			node.lazy = action;
			node.hasLazy = true;
		}
	}

	private void push(Node<Acting, Acted> node) {
		if (node == null)
			return;
		if (node.rev) {
			allReverse(node.left);
			allReverse(node.right);
			node.rev = false;
		}
		if (node.hasLazy) {
			allApply(node.left, node.lazy);
			allApply(node.right, node.lazy);
			node.lazy = actionIdentity;
			node.hasLazy = false;
		}
	}
}
