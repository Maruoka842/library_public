package library.util.mo;

/**
 * State cursor for Mo's algorithm operating on interval [l, r).
 *
 * @param <T> query return type
 */
public abstract class MoCursor<T> {
	protected int l = 0;
	protected int r = 0;

	/**
	 * (i, r) を [i, r) に
	 * @param i
	 */
	public abstract void addLeft(int i);

	/**
	 * [i, r) を (i, r) に
	 * @param i
	 */
	public abstract void popLeft(int i);

	/**
	 * [l, i) を [l, i] に
	 * @param i
	 */
	public abstract void addRight(int i);

	/**
	 * [l, i] を [l, i) に
	 * @param i
	 */
	public abstract void popRight(int i);

	public final void internal_addLeft() {
		addLeft(--l);
	}

	public final void internal_popLeft() {
		popLeft(l++);
	}

	public final void internal_addRight() {
		addRight(r++);
	}

	public final void internal_popRight() {
		popRight(--r);
	}

	/**
	 * Returns current left boundary l of interval [l, r).
	 * Time complexity: O(1)
	 *
	 * @return left boundary index l
	 */
	// 未テスト
	public int l() {
		return l;
	}

	/**
	 * Returns current right boundary r of interval [l, r).
	 * Time complexity: O(1)
	 *
	 * @return right boundary index r
	 */
	// 未テスト
	public int r() {
		return r;
	}

	/**
	 * Increments left boundary l by 1, removing element at index l.
	 * Time complexity: time complexity of popLeft
	 */
	// 未テスト
	public void incrementL() {
		internal_popLeft();
	}

	/**
	 * Decrements left boundary l by 1, adding element at index l - 1.
	 * Time complexity: time complexity of addLeft
	 */
	// 未テスト
	public void decrementL() {
		internal_addLeft();
	}

	/**
	 * Increments right boundary r by 1, adding element at index r.
	 * Time complexity: time complexity of addRight
	 */
	// 未テスト
	public void incrementR() {
		internal_addRight();
	}

	/**
	 * Decrements right boundary r by 1, removing element at index r - 1.
	 * Time complexity: time complexity of popRight
	 */
	// 未テスト
	public void decrementR() {
		internal_popRight();
	}

	/**
	 * Returns debug string representation of cursor position and value for queryId.
	 * Time complexity: time complexity of getValue
	 *
	 * @param queryId query ID
	 * @return debug string
	 */
	// 未テスト
	public String dump(int queryId) {
		return "l=" + l + ", r=" + r + ", value=" + getValue(queryId);
	}

	/**
	 * Returns the value of interval [l, r) for queryId.
	 * Time complexity: implementation-dependent.
	 *
	 * @param queryId query ID assigned by {@link Mo#addQuery(int, int)}
	 * @return query value
	 */
	public abstract T getValue(int queryId);
}
