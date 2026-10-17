package library.util.collections;

import java.util.Comparator;

/**
 * 任意の要素型 E を格納できる、マージ可能な Skew Heap。
 * 遅延加算をサポートしない軽量な実装。
 *
 * @param <E> 保持する要素の型
 */
public class MeldableSkewHeap<E> {
	/**
	 * Skew Heap のノード。
	 *
	 * @param <E> 保持する要素の型
	 */
	public static class Node<E> {
		public E val;
		public Node<E> l, r;

		public Node(E val) {
			this.val = val;
		}
	}

	private final Comparator<? super E> cmp;

	/**
	 * 自然順序（Comparable）を用いる空の MeldableSkewHeap を構築します。
	 * 要素型 E は Comparable<? super E> を実装している必要があります。
	 *
	 * 計算量: O(1)
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public MeldableSkewHeap() {
		this.cmp = (a, b) -> ((Comparable<? super E>) a).compareTo(b);
	}

	/**
	 * 指定された比較器（Comparator）を用いる MeldableSkewHeap を構築します。
	 *
	 * 計算量: O(1)
	 *
	 * @param cmp 要素の順序を決定する比較器
	 */
	// 未テスト
	public MeldableSkewHeap(Comparator<? super E> cmp) {
		if (cmp == null) {
			throw new IllegalArgumentException("Comparator cannot be null");
		}
		this.cmp = cmp;
	}

	/**
	 * 2 つのヒープをマージします。
	 *
	 * 計算量: O(log N)
	 *
	 * @param a ヒープ1
	 * @param b ヒープ2
	 * @return マージ後のヒープの根
	 */
	// 未テスト
	public Node<E> meld(Node<E> a, Node<E> b) {
		if (a == null)
			return b;
		if (b == null)
			return a;
		if (cmp.compare(a.val, b.val) > 0) {
			Node<E> tmp = a;
			a = b;
			b = tmp;
		}
		a.r = meld(a.r, b);
		Node<E> tmp = a.l;
		a.l = a.r;
		a.r = tmp;
		return a;
	}

	/**
	 * 最小値を取り除いた後のヒープを返します。
	 *
	 * 計算量: O(log N)
	 *
	 * @param a ヒープの根
	 * @return 最小値除外後のヒープの根
	 */
	// 未テスト
	public Node<E> pop(Node<E> a) {
		if (a == null)
			return null;
		return meld(a.l, a.r);
	}
}
