package library.util.matrix;

/**
 * Monge 性を満たす long 値の暗黙的行列を表すインターフェース。
 *
 * <p>
 * Monge 行列とは、任意の i &lt; k, j &lt; l について
 * <pre>
 * A[i][j] + A[k][l] &lt;= A[i][l] + A[k][j]
 * </pre>
 * を満たす行列である。
 * </p>
 *
 * // 未テスト
 */
public interface MongeLong extends LongMatrix {
	/**
	 * 要素ごとの和を返す。
	 *
	 * @param other 加算する Monge 行列
	 * @return 要素ごとの和を表す Monge 行列
	 * @throws IllegalArgumentException 行列のサイズが一致しない場合
	 */
	default MongeLong add(MongeLong other) {
		if (rows() != other.rows() || cols() != other.cols()) {
			throw new IllegalArgumentException("dimension mismatch");
		}
		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return self.rows();
			}

			@Override
			public int cols() {
				return self.cols();
			}

			@Override
			public long get(int i, int j) {
				return self.get(i, j) + other.get(i, j);
			}
		};
	}

	/**
	 * 非負整数倍を返す。
	 *
	 * @param k 倍率
	 * @return k 倍された Monge 行列
	 * @throws IllegalArgumentException k &lt; 0 の場合
	 */
	default MongeLong scale(long k) {
		if (k < 0) throw new IllegalArgumentException("k < 0");
		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return self.rows();
			}

			@Override
			public int cols() {
				return self.cols();
			}

			@Override
			public long get(int i, int j) {
				return k * self.get(i, j);
			}
		};
	}

	/**
	 * 符号を反転する。
	 *
	 * @return 符号反転した AntiMonge 行列
	 */
	default AntiMongeLong negate() {
		MongeLong self = this;
		return new AntiMongeLong() {
			@Override
			public int rows() {
				return self.rows();
			}

			@Override
			public int cols() {
				return self.cols();
			}

			@Override
			public long get(int i, int j) {
				return -self.get(i, j);
			}
		};
	}

	/**
	 * 転置を返す。
	 *
	 * @return 転置した Monge 行列
	 */
	default MongeLong transpose() {
		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return self.cols();
			}

			@Override
			public int cols() {
				return self.rows();
			}

			@Override
			public long get(int i, int j) {
				return self.get(j, i);
			}
		};
	}

	/**
	 * 各行に任意のポテンシャルを加える。
	 *
	 * @param potential 各行に加えるポテンシャル配列
	 * @return 行ポテンシャルが加算された Monge 行列
	 * @throws IllegalArgumentException 配列長が行数と一致しない場合
	 */
	default MongeLong addRowPotential(long[] potential) {
		if (potential.length != rows()) {
			throw new IllegalArgumentException();
		}
		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return self.rows();
			}

			@Override
			public int cols() {
				return self.cols();
			}

			@Override
			public long get(int i, int j) {
				return self.get(i, j) + potential[i];
			}
		};
	}

	/**
	 * 各列に任意のポテンシャルを加える。
	 *
	 * @param potential 各列に加えるポテンシャル配列
	 * @return 列ポテンシャルが加算された Monge 行列
	 * @throws IllegalArgumentException 配列長が列数と一致しない場合
	 */
	default MongeLong addColumnPotential(long[] potential) {
		if (potential.length != cols()) {
			throw new IllegalArgumentException();
		}
		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return self.rows();
			}

			@Override
			public int cols() {
				return self.cols();
			}

			@Override
			public long get(int i, int j) {
				return self.get(i, j) + potential[j];
			}
		};
	}

	/**
	 * 行・列を順序を保って抜き出した部分行列を返す。
	 *
	 * @param selectedRows 選択する行インデックス配列 (狭義単調増加)
	 * @param selectedCols 選択する列インデックス配列 (狭義単調増加)
	 * @return 抜き出した部分 Monge 行列
	 * @throws IllegalArgumentException インデックスが狭義単調増加でない場合
	 */
	default MongeLong submatrix(int[] selectedRows, int[] selectedCols) {
		for (int i = 1; i < selectedRows.length; i++) {
			if (selectedRows[i - 1] >= selectedRows[i]) {
				throw new IllegalArgumentException();
			}
		}
		for (int j = 1; j < selectedCols.length; j++) {
			if (selectedCols[j - 1] >= selectedCols[j]) {
				throw new IllegalArgumentException();
			}
		}

		MongeLong self = this;
		return new MongeLong() {
			@Override
			public int rows() {
				return selectedRows.length;
			}

			@Override
			public int cols() {
				return selectedCols.length;
			}

			@Override
			public long get(int i, int j) {
				return self.get(selectedRows[i], selectedCols[j]);
			}
		};
	}
}
