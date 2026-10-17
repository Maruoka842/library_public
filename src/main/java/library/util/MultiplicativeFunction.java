package library.util;

import java.util.Map;
import library.util.collections.ArrayOnQuotient;

/**
 * 乗法的関数を表すインターフェース。
 * // 未テスト
 *
 * 乗法的関数とは、f(1) = 1 であり、互いに素な任意の正整数 a, b に対して f(ab) = f(a)f(b) を満たす数論的関数のこと。
 * 開発者は素数冪 p^e における値 f(p^e) を定義する。
 *
 * @author Jules
 */
@FunctionalInterface
public interface MultiplicativeFunction {

	/**
	 * 素数冪 p^e における乗法的関数の値 f(p^e) を計算する。
	 * e = 0 のときは 1 を返す必要がある。
	 *
	 * @param p 素数 p
	 * @param e 指数 e
	 * @return f(p^e) の値
	 */
	long evaluatePrimePower(long p, int e);

	/**
	 * 正整数 n に対する乗法的関数の値 f(n) を計算する。
	 * n を素因数分解し、各素数冪における値の積をとる。
	 * // 未テスト
	 *
	 * 計算量: O(n^(1/4))
	 *
	 * @param n 正整数 n
	 * @return f(n) の値
	 */
	default long evaluate(long n) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		if (n == 1) {
			return 1;
		}
		Map<Long, Integer> factors = MathUtils.factor(n);
		long res = 1;
		for (Map.Entry<Long, Integer> entry : factors.entrySet()) {
			res *= evaluatePrimePower(entry.getKey(), entry.getValue());
		}
		return res;
	}

	/**
	 * 正整数 n に対する乗法的関数の値 f(n) mod mod を計算する。
	 * n を素因数分解し、各素数冪における値の積を法 mod の下でとる。
	 * // 未テスト
	 *
	 * 計算量: O(n^(1/4))
	 *
	 * @param n 正整数 n
	 * @param mod 法
	 * @return f(n) mod mod の値
	 */
	default long evaluate(long n, long mod) {
		if (n <= 0) {
			throw new IllegalArgumentException("n must be positive");
		}
		if (n == 1) {
			long r = 1 % mod;
			if (r < 0) r += mod;
			return r;
		}
		Map<Long, Integer> factors = MathUtils.factor(n);
		long res = 1 % mod;
		if (res < 0) res += mod;
		for (Map.Entry<Long, Integer> entry : factors.entrySet()) {
			long val = evaluatePrimePower(entry.getKey(), entry.getValue()) % mod;
			if (val < 0) val += mod;
			res = (res * val) % mod;
		}
		return res;
	}

	/**
	 * この乗法的関数と別の乗法的関数 other とのディリクレ積（Dirichlet Convolution）を表す新しい乗法的関数を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e)
	 *
	 * @param other 別の乗法的関数
	 * @return 新しい乗法的関数
	 */
	default MultiplicativeFunction convolve(MultiplicativeFunction other) {
		return MultiplicativeFunctionUtils.convolve(this, other);
	}

	/**
	 * この乗法的関数と別の乗法的関数 other との法 mod の下でのディリクレ積を表す新しい乗法的関数を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e)
	 *
	 * @param other 別の乗法的関数
	 * @param mod 法
	 * @return 新しい乗法的関数
	 */
	default MultiplicativeFunction convolve(MultiplicativeFunction other, long mod) {
		return MultiplicativeFunctionUtils.convolve(this, other, mod);
	}

	/**
	 * この乗法的関数のディリクレ逆元（Dirichlet Inverse）を表す新しい乗法的関数を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e^2)
	 *
	 * @return ディリクレ逆元を表す乗法的関数
	 */
	default MultiplicativeFunction inverse() {
		return MultiplicativeFunctionUtils.inverse(this);
	}

	/**
	 * この乗法的関数の法 mod の下でのディリクレ逆元を表す新しい乗法的関数を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e^2)
	 *
	 * @param mod 法
	 * @return ディリクレ逆元を表す乗法的関数
	 */
	default MultiplicativeFunction inverse(long mod) {
		return MultiplicativeFunctionUtils.inverse(this, mod);
	}

	/**
	 * 素数の和のテーブル（ArrayOnQuotient）から、この乗法的関数の累積和のテーブル（ArrayOnQuotient）を法 mod の下で求める。
	 * // 未テスト
	 *
	 * @param primeSum 素数の和のテーブル
	 * @param mod 法
	 * @return 乗法的関数の累積和のテーブル
	 */
	default ArrayOnQuotient primeSumToMultiplicativeFunctionSumOnFp(ArrayOnQuotient primeSum, long mod) {
		return PrimeCount.primeSumToMultiplicativeFunctionSumOnFp(primeSum, this, mod);
	}
}
