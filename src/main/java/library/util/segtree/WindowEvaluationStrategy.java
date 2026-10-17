package library.util.segtree;

/**
 * 長さ N の列に対して、固定長 K の各連続部分列（窓）を評価し、
 * それらをモノイド演算で集約するための戦略（ストラテジー）を表すインターフェース。
 *
 * @param <E> 列の要素の型
 * @param <R> 評価結果・集約値の型
 */
public interface WindowEvaluationStrategy<E, R> {
	/**
	 * 窓の固定長 K を取得する。
	 *
	 * 計算量: O(1)
	 *
	 * @return 窓の長さ K
	 */
	int K();

	/**
	 * 長さ K の窓の要素の配列を評価する。
	 *
	 * 計算量: O(K) などの評価関数に依存
	 *
	 * @param elements 長さ K の窓の要素の配列
	 * @return 評価結果
	 */
	R phi(E[] elements);

	/**
	 * 評価結果のモノイド演算（マージ演算）を行う二項演算。
	 *
	 * 計算量: モノイド演算に依存
	 *
	 * @param a 左側の値
	 * @param b 右側の値
	 * @return マージ結果
	 */
	R op(R a, R b);

	/**
	 * 評価結果のモノイド演算における単位元を取得する。
	 *
	 * 計算量: O(1)
	 *
	 * @return 単位元 identity
	 */
	R identity();

	/**
	 * 要素の型を表す Class オブジェクトを取得する（配列生成に必要）。
	 *
	 * 計算量: O(1)
	 *
	 * @return 要素の型
	 */
	Class<E> clazz();
}
