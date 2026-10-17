package library.util.algebra.strategy.longs;

/**
 * primitive long に特化した半環の代数的構造。
 */
public interface LongSemiRingStrategy {
	/**
	 * @return 加法の単位元
	 */
	long zero();
	/**
	 * @return 乗法の単位元
	 */
	long one();
	/**
	 * @param a
	 * @param b
	 * @return a + b
	 */
	long add(long a, long b);
	/**
	 * @param a
	 * @param b
	 * @return a * b
	 */
	long mul(long a, long b);
	/**
	 * @param a
	 * @param b
	 * @return a == b
	 */
	boolean equals(long a, long b);

	/**
	 * 加法構造 (add, zero) をモノイド戦略として取り出す。
	 *
	 * <p>計算量: O(1)</p>
	 * // 未テスト
	 * @return 加法モノイド戦略
	 */
	default LongMonoidStrategy additiveMonoid() {
		return new LongMonoidStrategy() {
			@Override
			public long identity() {
				return zero();
			}

			@Override
			public long mul(long a, long b) {
				return add(a, b);
			}
		};
	}

	/**
	 * 乗法構造 (mul, one) をモノイド戦略として取り出す。
	 *
	 * <p>計算量: O(1)</p>
	 * // 未テスト
	 * @return 乗法モノイド戦略
	 */
	default LongMonoidStrategy multiplicativeMonoid() {
		return new LongMonoidStrategy() {
			@Override
			public long identity() {
				return one();
			}

			@Override
			public long mul(long a, long b) {
				return LongSemiRingStrategy.this.mul(a, b);
			}
		};
	}
}
