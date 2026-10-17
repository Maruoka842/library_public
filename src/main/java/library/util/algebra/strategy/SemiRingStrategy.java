package library.util.algebra.strategy;

import java.util.Objects;

public interface SemiRingStrategy<T> {
	T zero();
	T one();
	T add(T a, T b);
	T mul(T a, T b);
	boolean equals(T a, T b);

	/**
	 * a^n を返す。
	 * 事前条件: n >= 0。
	 * 事後条件: 戻り値 = a^n。
	 * 副作用: なし。
	 * 計算量: O(log n * mul)。
	 * 破壊的変更: なし。
	 * 参照共有・所有権: 戻り値は新規作成されるか、a または one() と共有される。
	 * 例外・未定義条件: n < 0 のとき未定義。
	 * @param a 底
	 * @param n 指数
	 * @return a^n
	 */
	default T pow(T a, long n) {
		if (n < 0) throw new IllegalArgumentException("Exponent must be non-negative");
		T res = one();
		T base = a;
		while (n > 0) {
			if ((n & 1) == 1) res = mul(res, base);
			base = mul(base, base);
			n >>= 1;
		}
		return res;
	}
	
	default boolean isZero(T a) {
		return equals(zero(), a);
	}
	
	default boolean isOne(T a) {
		return equals(one(), a);
	}
	
	/**
	 * 配列の隣接する各要素の間に {@code +} または {@code *} を挿入して得られる
	 * すべての式の値の総和を返す。
	 *
	 * @return {@code +} または {@code *} を挿入して得られるすべての式の値の総和
	 * @implNote 時間計算量は {@code O(a.length)}、追加領域は {@code O(1)}
	 */
	default T sumOfAllAddMulExpressions(T[] a) {
		//https://atcoder.jp/contests/abc224/submissions/78702302
		if (a.length == 0) return zero();
		T completed = zero();//全式について「最後の積ブロック以外」の値を全部足したもの。
		T tail = a[0];//全式について最後の積ブロックを全部足したもの。
		T count = one();

		for (int i = 1; i < a.length; i++) {
			T x = a[i];

			completed = add(completed, add(completed, tail));
			tail = add(mul(tail, x), mul(count, x));
			count = add(count, count);
		}

		return add(completed, tail);
	}

	/**
	 * a のハッシュ値を返す。
	 * equals(a, b) なら hashCode(a) == hashCode(b) である必要がある。
	 * @param a 対象。
	 * @return ハッシュ値。
	 */
	default int hashCode(T a) {
		return Objects.hashCode(a);
	}

	/**
	 * 加法構造 (add, zero) をモノイド戦略として取り出す。
	 *
	 * <p>計算量: O(1)</p>
	 * // 未テスト
	 * @return 加法モノイド戦略
	 */
	default library.util.algebra.strategy.monoid.MonoidStrategy<T> additiveMonoid() {
		return new library.util.algebra.strategy.monoid.MonoidStrategy<T>() {
			@Override
			public T identity() {
				return zero();
			}

			@Override
			public T mul(T a, T b) {
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
	default library.util.algebra.strategy.monoid.MonoidStrategy<T> multiplicativeMonoid() {
		return new library.util.algebra.strategy.monoid.MonoidStrategy<T>() {
			@Override
			public T identity() {
				return one();
			}

			@Override
			public T mul(T a, T b) {
				return SemiRingStrategy.this.mul(a, b);
			}
		};
	}
}
