package library.util.mo;

/**
 * 木上の Mo 法が保持する頂点集合に対する状態を表す。
 * 各頂点は高々一度だけ含まれる。getValue の呼び出し時にはクエリ対象の集合と一致するが、
 * クエリ間の遷移中は単純パスでない頂点集合を経由し得る。
 *
 * @param <T> クエリの返り値の型
 */
public interface TreeMoCursor<T> {
	/**
	 * 現在の頂点集合 S を S ∪ {v} に変更する。
	 * 呼び出し時に v ∉ S が成り立つ。
	 * 計算量: 利用側の状態更新に依存する。
	 *
	 * @param v 追加する頂点
	 */
	void addVertex(int v);

	/**
	 * 現在の頂点集合 S を S \ {v} に変更する。
	 * 呼び出し時に v ∈ S が成り立つ。
	 * 計算量: 利用側の状態更新に依存する。
	 *
	 * @param v 削除する頂点
	 */
	void removeVertex(int v);

	/**
	 * 現在の頂点集合 S とクエリ ID に対応する値を返す。
	 * 計算量: 利用側の値取得に依存する。
	 *
	 * @param queryId {@link TreeMo#addQuery(int, int)} または
	 *                {@link TreeMo#addEdgeQuery(int, int)} が返したクエリ ID
	 * @return 現在の頂点集合に対応する値
	 */
	T getValue(int queryId);
}
