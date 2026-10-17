package library.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * long 型のキーに対する、モノイド fold を保持する Treap Map。
 *
 * <p>
 * 各ノードは部分木の値をキーの昇順に fold した値を保持する。
 * {@code op} は結合的で、{@code identity} はその単位元である必要がある。
 * {@code op} は可換である必要はない。
 *
 * <p>
 * 主な操作:
 * <ul>
 *   <li>put / remove / get</li>
 *   <li>キー区間・添字区間に対する fold</li>
 *   <li>prefix / suffix fold</li>
 *   <li>Segment Tree の maxRight / minLeft に相当するモノイド二分探索</li>
 * </ul>
 *
 * <p>
 * 各操作の期待計算量は O(log N)。
 *
 * @param <T> モノイド要素の型
 */
public class FoldTreapMap<T> {
	/** 優先度決定用の乱数生成器。 */
	private static final Random rnd = new Random();

	/** 二項演算。 */
	private final BinaryOperator<T> op;

	/** モノイドの単位元。 */
	private final T identity;

	/** Treap の根ノード。 */
	private Node<T> root;

	/** マップの要素数。 */
	private int size;

	/**
	 * 部分木の情報を保持するノード。
	 *
	 * @param <T> モノイド要素の型
	 */
	private static class Node<T> {
		/** キー。 */
		long key;

		/** 値。 */
		T val;

		/** 部分木を key 昇順に fold した値。 */
		T prod;

		/** 優先度。 */
		int priority;

		/** 部分木のサイズ。 */
		int size;

		/** 左の子ノード。 */
		Node<T> left;

		/** 右の子ノード。 */
		Node<T> right;

		/**
		 * 新しいノードを構築します。
		 *
		 * @param key キー
		 * @param val 値
		 */
		Node(long key, T val) {
			this.key = key;
			this.val = val;
			this.prod = val;
			this.priority = rnd.nextInt();
			this.size = 1;
		}
	}

	/**
	 * マップのキーと値のペアを表すエントリ。
	 *
	 * @param <T> モノイド要素の型
	 */
	public static class Entry<T> {
		/** キー。 */
		public final long key;

		/** 値。 */
		public final T value;

		/**
		 * 新しいエントリを構築します。
		 *
		 * @param key キー
		 * @param value 値
		 */
		public Entry(long key, T value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString() {
			return "(" + key + "," + value + ")";
		}
	}

	/**
	 * モノイドの二項演算と単位元を指定して FoldTreapMap を構築します。
	 *
	 * @param op 二項演算
	 * @param identity 単位元
	 * @complexity O(1)
	 */
	// 未テスト
	public FoldTreapMap(BinaryOperator<T> op, T identity) {
		this.op = op;
		this.identity = identity;
	}

	/**
	 * 指定された二項演算、単位元、根ノードから FoldTreapMap を構築します。
	 *
	 * @param op 二項演算
	 * @param identity 単位元
	 * @param root 根ノード
	 * @complexity O(1)
	 */
	// 未テスト
	private FoldTreapMap(
			BinaryOperator<T> op,
			T identity,
			Node<T> root) {

		this.op = op;
		this.identity = identity;
		this.root = root;
		this.size = size(root);
	}

	// ============================================================
	// basic
	// ============================================================

	/**
	 * マップを空にします。
	 *
	 * @complexity O(1)
	 */
	// 未テスト
	public void clear() {
		root = null;
		size = 0;
	}

	/**
	 * マップの要素数を返します。
	 *
	 * @return 要素数
	 * @complexity O(1)
	 */
	// 未テスト
	public int size() {
		return size;
	}

	/**
	 * マップが空であるかどうかを判定します。
	 *
	 * @return 空であれば true、そうでなければ false
	 * @complexity O(1)
	 */
	// 未テスト
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * 部分木のサイズを返します。
	 *
	 * @param t 部分木の根ノード
	 * @return 部分木のサイズ
	 * @complexity O(1)
	 */
	// 未テスト
	private static int size(Node<?> t) {
		return t == null ? 0 : t.size;
	}

	/**
	 * 部分木のモノイドの積を返します。ノードが null の場合は単位元を返します。
	 *
	 * @param t 部分木の根ノード
	 * @return 部分木のモノイド積
	 * @complexity O(1)
	 */
	// 未テスト
	private T prod(Node<T> t) {
		return t == null ? identity : t.prod;
	}

	/**
	 * 指定されたノードのサイズおよび部分木の積を再計算します。
	 *
	 * @param t 再計算対象のノード
	 * @complexity O(1)
	 */
	// 未テスト
	private void recalc(Node<T> t) {
		if (t == null)
			return;

		t.size =
				1
				+ size(t.left)
				+ size(t.right);

		/*
		 * 非可換でもよいので順序が重要。
		 *
		 * left -> self -> right
		 */
		t.prod = op.apply(
				op.apply(
						prod(t.left),
						t.val),
				prod(t.right));
	}

	// ============================================================
	// rotation
	// ============================================================

	/**
	 * 右回転（Right Rotation）を行います。
	 *
	 * @param y 回転対象の部分木の根
	 * @return 回転後の新しい部分木の根
	 * @complexity O(1)
	 */
	// 未テスト
	private Node<T> rotateRight(Node<T> y) {
		Node<T> x = y.left;

		y.left = x.right;
		x.right = y;

		recalc(y);
		recalc(x);

		return x;
	}

	/**
	 * 左回転（Left Rotation）を行います。
	 *
	 * @param x 回転対象の部分木の根
	 * @return 回転後の新しい部分木の根
	 * @complexity O(1)
	 */
	// 未テスト
	private Node<T> rotateLeft(Node<T> x) {
		Node<T> y = x.right;

		x.right = y.left;
		y.left = x;

		recalc(x);
		recalc(y);

		return y;
	}

	// ============================================================
	// map operations
	// ============================================================

	/**
	 * 指定された部分木にキーと値を挿入します。
	 *
	 * @param t 部分木の根
	 * @param key キー
	 * @param val 値
	 * @return 更新後の部分木の根
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Node<T> put(
			Node<T> t,
			long key,
			T val) {

		if (t == null) {
			size++;
			return new Node<>(key, val);
		}

		if (key < t.key) {
			t.left = put(
					t.left,
					key,
					val);

			if (t.left.priority > t.priority)
				t = rotateRight(t);

		} else if (key > t.key) {
			t.right = put(
					t.right,
					key,
					val);

			if (t.right.priority > t.priority)
				t = rotateLeft(t);

		} else {
			t.val = val;
		}

		recalc(t);
		return t;
	}

	/**
	 * key に val を設定する。
	 * 既に key が存在すれば上書きする。
	 *
	 * @param key キー
	 * @param val 値
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public void put(long key, T val) {
		root = put(root, key, val);
	}

	/**
	 * 指定されたキーを持つノードを探索します。
	 *
	 * @param key キー
	 * @return キーを持つノード、存在しない場合は null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Node<T> findNode(long key) {
		Node<T> t = root;

		while (t != null) {
			if (key == t.key)
				return t;

			t = key < t.key
					? t.left
					: t.right;
		}

		return null;
	}

	/**
	 * 指定されたキーに対応する値を取得します。
	 *
	 * @param key キー
	 * @return キーに対応する値
	 * @throws AssertionError キーがマップに含まれていない場合
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public T get(long key) {
		Node<T> t = findNode(key);

		if (t == null)
			throw new AssertionError(
					"key is not contained: " + key);

		return t.val;
	}

	/**
	 * 指定されたキーに対応する値を取得します。存在しない場合はデフォルト値を返します。
	 *
	 * @param key キー
	 * @param defaultValue デフォルト値
	 * @return キーに対応する値、またはデフォルト値
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public T getOrDefault(
			long key,
			T defaultValue) {

		Node<T> t = findNode(key);

		return t == null
				? defaultValue
				: t.val;
	}

	/**
	 * 指定されたキーがマップに含まれているかどうかを判定します。
	 *
	 * @param key キー
	 * @return 含まれていれば true、そうでなければ false
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public boolean containsKey(long key) {
		return findNode(key) != null;
	}

	/**
	 * Java Map.merge 相当。
	 *
	 * @param key キー
	 * @param value マージする値
	 * @param mergeFunction マージ時の関数
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public void merge(
			long key,
			T value,
			BinaryOperator<T> mergeFunction) {

		Node<T> t = findNode(key);

		put(
				key,
				t == null
						? value
						: mergeFunction.apply(
								t.val,
								value));
	}

	/**
	 * 指定された部分木からキーを削除します。
	 *
	 * @param t 部分木の根ノード
	 * @param key 削除対象のキー
	 * @return 更新後の部分木の根ノード
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Node<T> remove(
			Node<T> t,
			long key) {

		if (t == null)
			return null;

		if (key < t.key) {
			t.left = remove(
					t.left,
					key);

		} else if (key > t.key) {
			t.right = remove(
					t.right,
					key);

		} else {
			if (t.left == null
					&& t.right == null) {

				size--;
				return null;

			} else if (t.left == null) {
				t = rotateLeft(t);
				t.left = remove(
						t.left,
						key);

			} else if (t.right == null) {
				t = rotateRight(t);
				t.right = remove(
						t.right,
						key);

			} else if (
					t.left.priority
					> t.right.priority) {

				t = rotateRight(t);
				t.right = remove(
						t.right,
						key);

			} else {
				t = rotateLeft(t);
				t.left = remove(
						t.left,
						key);
			}
		}

		recalc(t);
		return t;
	}

	/**
	 * key が存在すれば削除する。
	 *
	 * @param key キー
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public void remove(long key) {
		root = remove(
				root,
				key);
	}

	// ============================================================
	// fold
	// ============================================================

	/**
	 * モノイドの二項演算を適用します。
	 *
	 * @param a 左辺値
	 * @param b 右辺値
	 * @return 演算結果
	 * @complexity O(1)
	 */
	// 未テスト
	public T apply(T a, T b) {
		return op.apply(a, b);
	}

	/**
	 * 全要素を key 昇順に fold する。
	 *
	 * @return モノイドの総積
	 * @complexity O(1)
	 */
	// 未テスト
	public T allFold() {
		return prod(root);
	}

	/**
	 * key を昇順に並べたときの [l, r) 番目の値を fold する。
	 *
	 * @param l 範囲の下限インデックス（含む）
	 * @param r 範囲の上限インデックス（含まない）
	 * @return インデックス範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T foldByIndex(int l, int r) {
		l = Math.max(
				0,
				l);

		r = Math.min(
				size,
				r);

		if (l >= r)
			return identity;

		return prodByIndex(
				root,
				l,
				r);
	}

	/**
	 * 部分木について、指定されたインデックス範囲 [l, r) の値を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param l 範囲の下限インデックス
	 * @param r 範囲の上限インデックス
	 * @return インデックス範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodByIndex(
			Node<T> t,
			int l,
			int r) {

		if (t == null
				|| r <= 0
				|| t.size <= l)
			return identity;

		if (l <= 0
				&& t.size <= r)
			return t.prod;

		int ls = size(t.left);

		T res = prodByIndex(
				t.left,
				l,
				r);

		if (l <= ls && ls < r)
			res = op.apply(
					res,
					t.val);

		return op.apply(
				res,
				prodByIndex(
						t.right,
						l - ls - 1,
						r - ls - 1));
	}

	/**
	 * l <= key < r を満たす値を key 昇順に fold する。
	 *
	 * @param l キーの下限（含む）
	 * @param r キーの上限（含まない）
	 * @return キー範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T fold(long l, long r) {
		if (l >= r)
			return identity;

		return prodRange(
				root,
				l,
				r);
	}

	/**
	 * 部分木について、指定されたキー範囲 [l, r) の値を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param l キーの下限
	 * @param r キーの上限
	 * @return キー範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodRange(
			Node<T> t,
			long l,
			long r) {

		if (t == null)
			return identity;

		/*
		 * t.key が左境界より左なら、
		 * 左部分木もすべて範囲外。
		 */
		if (t.key < l)
			return prodRange(
					t.right,
					l,
					r);

		/*
		 * t.key が右境界以上なら、
		 * 右部分木もすべて範囲外。
		 */
		if (t.key >= r)
			return prodRange(
					t.left,
					l,
					r);

		/*
		 * l <= t.key < r。
		 *
		 * 左部分木から key >= l、
		 * 右部分木から key < r
		 * を取得する。
		 */
		return op.apply(
				op.apply(
						prodGreaterEqual(
								t.left,
								l),
						t.val),
				prodLessThan(
						t.right,
						r));
	}

	/**
	 * k < key を満たす値を key 昇順に fold する。
	 *
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T foldLessThan(long key) {
		return prodLessThan(
				root,
				key);
	}

	/**
	 * 部分木について、キーが指定キー未満の要素を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodLessThan(
			Node<T> t,
			long key) {

		if (t == null)
			return identity;

		if (key <= t.key)
			return prodLessThan(
					t.left,
					key);

		return op.apply(
				op.apply(
						prod(t.left),
						t.val),
				prodLessThan(
						t.right,
						key));
	}

	/**
	 * k <= key を満たす値を key 昇順に fold する。
	 *
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T foldLessEqual(long key) {
		return prodLessEqual(
				root,
				key);
	}

	/**
	 * 部分木について、キーが指定キー以下の要素を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodLessEqual(
			Node<T> t,
			long key) {

		if (t == null)
			return identity;

		if (key < t.key)
			return prodLessEqual(
					t.left,
					key);

		return op.apply(
				op.apply(
						prod(t.left),
						t.val),
				prodLessEqual(
						t.right,
						key));
	}

	/**
	 * key < k を満たす値を key 昇順に fold する。
	 *
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T foldGreaterThan(long key) {
		return prodGreaterThan(
				root,
				key);
	}

	/**
	 * 部分木について、キーが指定キーより大きい要素を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodGreaterThan(
			Node<T> t,
			long key) {

		if (t == null)
			return identity;

		if (t.key <= key)
			return prodGreaterThan(
					t.right,
					key);

		return op.apply(
				prodGreaterThan(
						t.left,
						key),
				op.apply(
						t.val,
						prod(t.right)));
	}

	/**
	 * key <= k を満たす値を key 昇順に fold する。
	 *
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public T foldGreaterEqual(long key) {
		return prodGreaterEqual(
				root,
				key);
	}

	/**
	 * 部分木について、キーが指定キー以上の要素を fold します。
	 *
	 * @param t 部分木の根ノード
	 * @param key 境界キー
	 * @return 範囲のモノイド積
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private T prodGreaterEqual(
			Node<T> t,
			long key) {

		if (t == null)
			return identity;

		if (t.key < key)
			return prodGreaterEqual(
					t.right,
					key);

		return op.apply(
				prodGreaterEqual(
						t.left,
						key),
				op.apply(
						t.val,
						prod(t.right)));
	}

	// ============================================================
	// monoid binary search
	// ============================================================

	/**
	 * 値を一時的に格納するためのボックス。
	 *
	 * @param <T> 値の型
	 */
	private static class Box<T> {
		/** 保持している値。 */
		T value;

		/**
		 * 新しいボックスを構築します。
		 *
		 * @param value 初期値
		 */
		Box(T value) {
			this.value = value;
		}
	}

	/**
	 * {@code from} 以降の値を key 昇順に fold していったとき、
	 * predicate が初めて false になる要素の key を返す。
	 *
	 * <p>
	 * inclusive == true なら key >= from、
	 * false なら key > from を対象とする。
	 *
	 * <p>
	 * 全 prefix で predicate が true なら null。
	 *
	 * <p>
	 * predicate(identity) == true であり、
	 * 対象範囲の prefix を伸ばすにつれて
	 *
	 * <pre>
	 * true, ..., true, false, ..., false
	 * </pre>
	 *
	 * となることを要求する。
	 *
	 * <p>
	 * Segment Tree の maxRight に相当する。
	 *
	 * @param from 開始キー
	 * @param inclusive 開始キーを含むかどうか
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になる要素のキー、すべて真なら null
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public Long firstPrefixFailure(
			long from,
			boolean inclusive,
			Predicate<T> predicate) {

		if (!predicate.test(identity))
			throw new IllegalArgumentException(
					"predicate(identity) must be true");

		return firstPrefixFailure(
				root,
				from,
				inclusive,
				new Box<>(identity),
				predicate);
	}

	/**
	 * 部分木について、firstPrefixFailure を再帰的に実行します。
	 *
	 * @param t 部分木の根ノード
	 * @param from 開始キー
	 * @param inclusive 開始キーを含むかどうか
	 * @param acc これまでの累積値
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になるキー、存在しなければ null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Long firstPrefixFailure(
			Node<T> t,
			long from,
			boolean inclusive,
			Box<T> acc,
			Predicate<T> predicate) {

		if (t == null)
			return null;

		boolean before =
				t.key < from
				|| (!inclusive
						&& t.key == from);

		/*
		 * t.key 自身が範囲より前なら、
		 * 左部分木も全て範囲より前。
		 */
		if (before) {
			return firstPrefixFailure(
					t.right,
					from,
					inclusive,
					acc,
					predicate);
		}

		/*
		 * 左部分木には from より前の要素と
		 * 対象要素が混在している可能性がある。
		 */
		Long res = firstPrefixFailure(
				t.left,
				from,
				inclusive,
				acc,
				predicate);

		if (res != null)
			return res;

		// self
		T next = op.apply(
				acc.value,
				t.val);

		if (!predicate.test(next))
			return t.key;

		acc.value = next;

		/*
		 * 右部分木は全て対象範囲なので、
		 * subtree prod を使って探索する。
		 */
		return firstPrefixFailureAll(
				t.right,
				acc,
				predicate);
	}

	/**
	 * t の全要素が探索対象である場合の binary search 探索。
	 *
	 * @param t 部分木の根ノード
	 * @param acc これまでの累積値
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になるキー、存在しなければ null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Long firstPrefixFailureAll(
			Node<T> t,
			Box<T> acc,
			Predicate<T> predicate) {

		if (t == null)
			return null;

		/*
		 * 部分木全体を追加しても predicate が true なら
		 * 部分木を丸ごと飛ばす。
		 */
		T whole = op.apply(
				acc.value,
				t.prod);

		if (predicate.test(whole)) {
			acc.value = whole;
			return null;
		}

		// left
		if (t.left != null) {
			T next = op.apply(
					acc.value,
					t.left.prod);

			if (!predicate.test(next)) {
				return firstPrefixFailureAll(
						t.left,
						acc,
						predicate);
			}

			acc.value = next;
		}

		// self
		T next = op.apply(
				acc.value,
				t.val);

		if (!predicate.test(next))
			return t.key;

		acc.value = next;

		// right
		return firstPrefixFailureAll(
				t.right,
				acc,
				predicate);
	}

	/**
	 * {@code to} 以下の値について、
	 * 右から左へ範囲を広げていったとき、
	 * predicate が初めて false になる要素の key を返す。
	 *
	 * <p>
	 * inclusive == true なら key <= to、
	 * false なら key < to を対象とする。
	 *
	 * <p>
	 * fold 自体の順序は常に key 昇順。
	 *
	 * <p>
	 * 全 suffix で predicate が true なら null。
	 *
	 * <p>
	 * predicate(identity) == true であり、
	 * suffix を左へ伸ばすにつれて
	 *
	 * <pre>
	 * true, ..., true, false, ..., false
	 * </pre>
	 *
	 * となることを要求する。
	 *
	 * <p>
	 * Segment Tree の minLeft に相当する。
	 *
	 * @param to 上限キー
	 * @param inclusive 上限キーを含むかどうか
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になる要素のキー、すべて真なら null
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public Long lastSuffixFailure(
			long to,
			boolean inclusive,
			Predicate<T> predicate) {

		if (!predicate.test(identity))
			throw new IllegalArgumentException(
					"predicate(identity) must be true");

		return lastSuffixFailure(
				root,
				to,
				inclusive,
				new Box<>(identity),
				predicate);
	}

	/**
	 * 部分木について、lastSuffixFailure を再帰的に実行します。
	 *
	 * @param t 部分木の根ノード
	 * @param to 上限キー
	 * @param inclusive 上限キーを含むかどうか
	 * @param acc これまでの累積値
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になるキー、存在しなければ null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Long lastSuffixFailure(
			Node<T> t,
			long to,
			boolean inclusive,
			Box<T> acc,
			Predicate<T> predicate) {

		if (t == null)
			return null;

		boolean after =
				t.key > to
				|| (!inclusive
						&& t.key == to);

		/*
		 * t.key が対象範囲より後なら、
		 * 右部分木も全て対象外。
		 */
		if (after) {
			return lastSuffixFailure(
					t.left,
					to,
					inclusive,
					acc,
					predicate);
		}

		/*
		 * 右部分木には対象外と対象が
		 * 混在している可能性がある。
		 */
		Long res = lastSuffixFailure(
				t.right,
				to,
				inclusive,
				acc,
				predicate);

		if (res != null)
			return res;

		/*
		 * suffix なので self は acc の左に追加。
		 */
		T next = op.apply(
				t.val,
				acc.value);

		if (!predicate.test(next))
			return t.key;

		acc.value = next;

		/*
		 * 左部分木は全て対象範囲。
		 */
		return lastSuffixFailureAll(
				t.left,
				acc,
				predicate);
	}

	/**
	 * t の全要素が探索対象である場合の suffix binary search 探索。
	 *
	 * @param t 部分木の根ノード
	 * @param acc これまでの累積値
	 * @param predicate 判定条件
	 * @return 条件が初めて偽になるキー、存在しなければ null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Long lastSuffixFailureAll(
			Node<T> t,
			Box<T> acc,
			Predicate<T> predicate) {

		if (t == null)
			return null;

		T whole = op.apply(
				t.prod,
				acc.value);

		if (predicate.test(whole)) {
			acc.value = whole;
			return null;
		}

		// right
		if (t.right != null) {
			T next = op.apply(
					t.right.prod,
					acc.value);

			if (!predicate.test(next)) {
				return lastSuffixFailureAll(
						t.right,
						acc,
						predicate);
			}

			acc.value = next;
		}

		// self
		T next = op.apply(
				t.val,
				acc.value);

		if (!predicate.test(next))
			return t.key;

		acc.value = next;

		// left
		return lastSuffixFailureAll(
				t.left,
				acc,
				predicate);
	}

	// ============================================================
	// order statistics
	// ============================================================

	/**
	 * key を昇順に並べたときの k 番目 (0-indexed) の key。
	 * 存在しなければ null。
	 *
	 * @param k インデックス
	 * @return k 番目のキー、存在しなければ null
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public Long kthKey(int k) {
		if (k < 0 || k >= size)
			return null;

		Node<T> t = root;

		while (true) {
			int ls = size(t.left);

			if (k < ls) {
				t = t.left;

			} else if (k == ls) {
				return t.key;

			} else {
				k -= ls + 1;
				t = t.right;
			}
		}
	}

	/**
	 * キーを昇順に並べたときの k 番目（0-indexed）の Entry を返します。
	 *
	 * @param k インデックス
	 * @return k 番目の Entry、存在しなければ null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Entry<T> kthEntry(int k) {
		if (k < 0 || k >= size)
			return null;

		Node<T> t = root;

		while (true) {
			int ls = size(t.left);

			if (k < ls) {
				t = t.left;

			} else if (k == ls) {
				return new Entry<>(
						t.key,
						t.val);

			} else {
				k -= ls + 1;
				t = t.right;
			}
		}
	}

	// ============================================================
	// first / last
	// ============================================================

	/**
	 * マップ内の最小キーを返します。
	 *
	 * @return 最小キー、マップが空なら null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long firstKey() {
		if (root == null)
			return null;

		Node<T> t = root;

		while (t.left != null)
			t = t.left;

		return t.key;
	}

	/**
	 * マップ内の最小キーを持つ Entry を返します。
	 *
	 * @return 最小キーの Entry、マップが空なら null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Entry<T> firstEntry() {
		if (root == null)
			return null;

		Node<T> t = root;

		while (t.left != null)
			t = t.left;

		return new Entry<>(
				t.key,
				t.val);
	}

	/**
	 * マップ内の最大キーを返します。
	 *
	 * @return 最大キー、マップが空なら null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long lastKey() {
		if (root == null)
			return null;

		Node<T> t = root;

		while (t.right != null)
			t = t.right;

		return t.key;
	}

	/**
	 * マップ内の最大キーを持つ Entry を返します。
	 *
	 * @return 最大キーの Entry、マップが空なら null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Entry<T> lastEntry() {
		if (root == null)
			return null;

		Node<T> t = root;

		while (t.right != null)
			t = t.right;

		return new Entry<>(
				t.key,
				t.val);
	}

	// ============================================================
	// predecessor / successor
	// ============================================================

	/**
	 * 指定キー以下の最大キーを返します。
	 *
	 * @param key 探索の基準となるキー
	 * @return 指定キー以下の最大キー、存在しない場合は null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long floorKey(long key) {
		Node<T> t = root;
		Long res = null;

		while (t != null) {
			if (t.key == key)
				return key;

			if (t.key < key) {
				res = t.key;
				t = t.right;
			} else {
				t = t.left;
			}
		}

		return res;
	}

	/**
	 * 指定キーより小さい最大キーを返します。
	 *
	 * @param key 探索の基準となるキー
	 * @return 指定キーより小さい最大キー、存在しない場合は null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long lowerKey(long key) {
		Node<T> t = root;
		Long res = null;

		while (t != null) {
			if (t.key < key) {
				res = t.key;
				t = t.right;
			} else {
				t = t.left;
			}
		}

		return res;
	}

	/**
	 * 指定キー以上の最小キーを返します。
	 *
	 * @param key 探索の基準となるキー
	 * @return 指定キー以上の最小キー、存在しない場合は null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long ceilKey(long key) {
		Node<T> t = root;
		Long res = null;

		while (t != null) {
			if (t.key == key)
				return key;

			if (t.key > key) {
				res = t.key;
				t = t.left;
			} else {
				t = t.right;
			}
		}

		return res;
	}

	/**
	 * 指定キーより大きい最小キーを返します。
	 *
	 * @param key 探索の基準となるキー
	 * @return 指定キーより大きい最小キー、存在しない場合は null
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	public Long higherKey(long key) {
		Node<T> t = root;
		Long res = null;

		while (t != null) {
			if (t.key > key) {
				res = t.key;
				t = t.left;
			} else {
				t = t.right;
			}
		}

		return res;
	}

	// ============================================================
	// rank
	// ============================================================

	/**
	 * key 以下のキーの個数。
	 *
	 * @param key 基準キー
	 * @return 指定キー以下の要素数
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public long countLeq(long key) {
		Node<T> t = root;
		int res = 0;

		while (t != null) {
			if (t.key <= key) {
				res +=
						1
						+ size(t.left);

				t = t.right;
			} else {
				t = t.left;
			}
		}

		return res;
	}

	/**
	 * key 未満のキーの個数。
	 *
	 * @param key 基準キー
	 * @return 指定キー未満の要素数
	 * @complexity expected O(log N)
	 */
	// 未テスト
	public long countLt(long key) {
		Node<T> t = root;
		int res = 0;

		while (t != null) {
			if (t.key < key) {
				res +=
						1
						+ size(t.left);

				t = t.right;
			} else {
				t = t.left;
			}
		}

		return res;
	}

	// ============================================================
	// internal split / merge
	// ============================================================

	/**
	 * 全ての key in l が全ての key in r より小さいことを要求する。
	 *
	 * @param l 左部分木
	 * @param r 右部分木
	 * @return マージされた部分木の根ノード
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	private Node<T> mergeNodes(
			Node<T> l,
			Node<T> r) {

		if (l == null)
			return r;

		if (r == null)
			return l;

		if (l.priority > r.priority) {
			l.right = mergeNodes(
					l.right,
					r);

			recalc(l);
			return l;

		} else {
			r.left = mergeNodes(
					l,
					r.left);

			recalc(r);
			return r;
		}
	}

	/**
	 * {key < splitKey}, {key >= splitKey} に分割する。
	 *
	 * @param t 分割対象の部分木
	 * @param splitKey 境界キー
	 * @return 分割後の [左部分木, 右部分木] のペア配列
	 * @complexity 期待値 O(log N)
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	private Node<T>[] splitNodes(
			Node<T> t,
			long splitKey) {

		if (t == null)
			return (Node<T>[]) new Node<?>[] {
					null,
					null
			};

		if (t.key < splitKey) {
			Node<T>[] a = splitNodes(
					t.right,
					splitKey);

			t.right = a[0];
			recalc(t);

			return (Node<T>[]) new Node<?>[] {
					t,
					a[1]
			};

		} else {
			Node<T>[] a = splitNodes(
					t.left,
					splitKey);

			t.left = a[1];
			recalc(t);

			return (Node<T>[]) new Node<?>[] {
					a[0],
					t
			};
		}
	}

	// ============================================================
	// list / copy
	// ============================================================

	/**
	 * キーの昇順にすべての Entry を格納したリストを返します。
	 *
	 * @return Entry のリスト
	 * @complexity O(N)
	 */
	// 未テスト
	public List<Entry<T>> entryList() {
		List<Entry<T>> res =
				new ArrayList<>();

		inOrderEntries(
				root,
				res);

		return res;
	}

	/**
	 * 通りがけ順に部分木を走査し、Entry をリストに追加します。
	 *
	 * @param t 部分木の根ノード
	 * @param res 結果を格納するリスト
	 * @complexity O(N)
	 */
	// 未テスト
	private void inOrderEntries(
			Node<T> t,
			List<Entry<T>> res) {

		if (t == null)
			return;

		inOrderEntries(
				t.left,
				res);

		res.add(
				new Entry<>(
						t.key,
						t.val));

		inOrderEntries(
				t.right,
				res);
	}

	/**
	 * 木構造を含めたコピーを返す。
	 *
	 * <p>
	 * T 自体はコピーしない。
	 *
	 * @return ディープコピーされたマップ
	 * @complexity O(N)
	 */
	// 未テスト
	public FoldTreapMap<T> copy() {
		return new FoldTreapMap<>(
				op,
				identity,
				copyNode(root));
	}

	/**
	 * 指定された部分木をコピーします。
	 *
	 * @param t コピー対象のノード
	 * @return コピーされたノード
	 * @complexity O(N)
	 */
	// 未テスト
	private Node<T> copyNode(Node<T> t) {
		if (t == null)
			return null;

		Node<T> cp =
				new Node<>(
						t.key,
						t.val);

		cp.priority = t.priority;
		cp.size = t.size;
		cp.prod = t.prod;

		cp.left =
				copyNode(t.left);

		cp.right =
				copyNode(t.right);

		return cp;
	}

	// ============================================================
	// O(N) build
	// ============================================================

	/**
	 * strictly increasing な keys と対応する values から
	 * O(N) で Treap を構築する。
	 *
	 * @param <T> モノイド要素の型
	 * @param keys ソート済みのキー配列
	 * @param values キーに対応する値の配列
	 * @param op モノイド二項演算
	 * @param identity モノイド単位元
	 * @return 構築されたマップ
	 * @throws IllegalArgumentException keys と values のサイズが異なる場合、または keys が昇順でない場合
	 * @complexity O(N)
	 */
	// 未テスト
	public static <T> FoldTreapMap<T> buildFromSorted(
			long[] keys,
			T[] values,
			BinaryOperator<T> op,
			T identity) {

		if (keys.length != values.length)
			throw new IllegalArgumentException(
					"keys.length != values.length");

		FoldTreapMap<T> map =
				new FoldTreapMap<>(
						op,
						identity);

		int n = keys.length;

		if (n == 0)
			return map;

		@SuppressWarnings("unchecked")
		Node<T>[] stack =
				(Node<T>[]) new Node<?>[n];

		int sp = 0;

		for (int i = 0; i < n; i++) {
			if (i > 0
					&& keys[i - 1] >= keys[i]) {

				throw new IllegalArgumentException(
						"keys must be strictly increasing");
			}

			Node<T> cur =
					new Node<>(
							keys[i],
							values[i]);

			Node<T> last = null;

			while (sp > 0
					&& stack[sp - 1].priority
							< cur.priority) {

				last =
						stack[--sp];

				map.recalc(last);
			}

			cur.left = last;

			if (sp > 0)
				stack[sp - 1].right = cur;

			stack[sp++] = cur;
		}

		while (sp > 0)
			map.recalc(
					stack[--sp]);

		map.root = stack[0];
		map.size = n;

		return map;
	}

	// ============================================================
	// verify
	// ============================================================

	/**
	 * マップ（Treap 構造）の健全性を検証します。
	 *
	 * @throws AssertionError 構造に不備がある場合
	 * @complexity O(N)
	 */
	// 未テスト
	public void verify() {
		VerifyResult<T> res =
				verify(
						root,
						null,
						null);

		if (res.size != size) {
			throw new AssertionError(
					"size mismatch: expected="
					+ res.size
					+ ", actual="
					+ size);
		}
	}

	/**
	 * 検証の結果を表すオブジェクト。
	 *
	 * @param <T> モノイド要素の型
	 */
	private static class VerifyResult<T> {
		/** 部分木サイズ。 */
		int size;

		/** 部分木モノイド積。 */
		T prod;

		/**
		 * 新しい検証結果を構築します。
		 *
		 * @param size サイズ
		 * @param prod 積
		 */
		VerifyResult(
				int size,
				T prod) {

			this.size = size;
			this.prod = prod;
		}
	}

	/**
	 * 部分木について構造の健全性を検証します。
	 *
	 * @param t 部分木の根ノード
	 * @param lowerExclusive キーの下限（排他的）
	 * @param upperExclusive キーの上限（排他的）
	 * @return 検証結果
	 * @throws AssertionError 構造に不備がある場合
	 * @complexity O(N)
	 */
	// 未テスト
	private VerifyResult<T> verify(
			Node<T> t,
			Long lowerExclusive,
			Long upperExclusive) {

		if (t == null)
			return new VerifyResult<>(
					0,
					identity);

		if (lowerExclusive != null
				&& t.key <= lowerExclusive) {

			throw new AssertionError(
					"BST property violated at key: "
					+ t.key);
		}

		if (upperExclusive != null
				&& t.key >= upperExclusive) {

			throw new AssertionError(
					"BST property violated at key: "
					+ t.key);
		}

		if (t.left != null
				&& t.left.priority > t.priority) {

			throw new AssertionError(
					"Priority property violated at left of: "
					+ t.key);
		}

		if (t.right != null
				&& t.right.priority > t.priority) {

			throw new AssertionError(
					"Priority property violated at right of: "
					+ t.key);
		}

		VerifyResult<T> l =
				verify(
						t.left,
						lowerExclusive,
						t.key);

		VerifyResult<T> r =
				verify(
						t.right,
						t.key,
						upperExclusive);

		int expectedSize =
				1
				+ l.size
				+ r.size;

		T expectedProd =
				op.apply(
						op.apply(
								l.prod,
								t.val),
						r.prod);

		if (t.size != expectedSize) {
			throw new AssertionError(
					"size mismatch at key "
					+ t.key
					+ ": expected="
					+ expectedSize
					+ ", actual="
					+ t.size);
		}

		if (!Objects.equals(
				t.prod,
				expectedProd)) {

			throw new AssertionError(
					"prod mismatch at key "
					+ t.key
					+ ": expected="
					+ expectedProd
					+ ", actual="
					+ t.prod);
		}

		return new VerifyResult<>(
				expectedSize,
				expectedProd);
	}

	// ============================================================
	// Object
	// ============================================================

	@Override
	// 未テスト
	public String toString() {
		return entryList().toString();
	}

	@Override
	// 未テスト
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (!(o instanceof FoldTreapMap<?>))
			return false;

		FoldTreapMap<?> other =
				(FoldTreapMap<?>) o;

		if (size() != other.size())
			return false;

		List<Entry<T>> a =
				entryList();

		List<? extends Entry<?>> b =
				other.entryList();

		for (int i = 0; i < a.size(); i++) {
			Entry<T> x = a.get(i);
			Entry<?> y = b.get(i);

			if (x.key != y.key
					|| !Objects.equals(
							x.value,
							y.value)) {

				return false;
			}
		}

		return true;
	}

	@Override
	// 未テスト
	public int hashCode() {
		int h = 0;

		for (Entry<T> e : entryList()) {
			h +=
					Long.hashCode(e.key)
					^ Objects.hashCode(e.value);
		}

		return h;
	}
}
