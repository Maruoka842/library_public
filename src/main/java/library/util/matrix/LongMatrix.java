package library.util.matrix;

/**
 * long 値を持つ暗黙的な行列。
 *
 * <p>実体を long[][] として保持する必要はない。
 * {@link #get(int, int)} が各要素を計算できればよい。
 * </p>
 *
 * // 未テスト
 */
public interface LongMatrix {
	/**
	 * 行数を返す。
	 *
	 * @return 行数
	 */
	int rows();

	/**
	 * 列数を返す。
	 *
	 * @return 列数
	 */
	int cols();

	/**
	 * (row, col) 要素を返す。
	 *
	 * @param row 行インデックス
	 * @param col 列インデックス
	 * @return (row, col) 要素の値
	 */
	long get(int row, int col);

	/**
	 * 行列要素計算用の関数型インターフェース。
	 */
	@FunctionalInterface
	interface EntryFunction {
		/**
		 * (row, col) 要素を計算する。
		 *
		 * @param row 行インデックス
		 * @param col 列インデックス
		 * @return (row, col) 要素の値
		 */
		long get(int row, int col);
	}
}
