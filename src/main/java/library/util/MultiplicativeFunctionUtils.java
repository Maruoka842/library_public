package library.util;

import library.util.poset.DivisorLattice;

/**
 * 乗法的関数（Multiplicative Function）に関するユーティリティクラス。
 * // 未テスト
 *
 * 乗法的関数とは、f(1) = 1 であり、互いに素な任意の正整数 a, b に対して f(ab) = f(a)f(b) を満たす数論的関数のこと。
 * 素数冪 p^e における値 f(p^e) を定義することで、任意の正整数に対する f(n) を線形篩を用いて O(N) 時間で計算する。
 *
 * @author Jules
 */
public class MultiplicativeFunctionUtils {

	/**
	 * 線形篩を用いて、乗法的関数の 1 から N までの値を一括して列挙する。
	 * // 未テスト
	 *
	 * 計算量: O(N)
	 *
	 * @param N 上限値
	 * @param f 素数冪における評価器
	 * @return 長さ N + 1 の配列。res[i] = f(i) (1 <= i <= N), res[0] = 0
	 */
	public static long[] enumValues(int N, MultiplicativeFunction f) {
		if (N < 0) {
			throw new IllegalArgumentException("N must be non-negative");
		}
		long[] res = new long[N + 1];
		if (N >= 0) {
			res[0] = 0;
		}
		if (N >= 1) {
			res[1] = 1;
		}
		if (N <= 1) {
			return res;
		}

		int[] primes = new int[N + 1];
		int primeCount = 0;
		boolean[] isComposite = new boolean[N + 1];
		int[] pp = new int[N + 1];
		int[] pe = new int[N + 1];

		for (int i = 2; i <= N; i++) {
			if (!isComposite[i]) {
				primes[primeCount++] = i;
				pp[i] = i;
				pe[i] = 1;
				res[i] = f.evaluatePrimePower(i, 1);
			}
			for (int j = 0; j < primeCount; j++) {
				int p = primes[j];
				if (p > N / i) {
					break;
				}
				int next = i * p;
				isComposite[next] = true;
				if (i % p == 0) {
					pp[next] = pp[i] * p;
					pe[next] = pe[i] + 1;
					res[next] = res[i / pp[i]] * f.evaluatePrimePower(p, pe[next]);
					break;
				} else {
					pp[next] = p;
					pe[next] = 1;
					res[next] = res[i] * res[p];
				}
			}
		}
		return res;
	}

	/**
	 * 線形篩を用いて、乗法的関数の 1 から N までの値（法 mod の下）を一括して列挙する。
	 * // 未テスト
	 *
	 * 計算量: O(N)
	 *
	 * @param N 上限値
	 * @param f 素数冪における評価器
	 * @param mod 法
	 * @return 長さ N + 1 の配列。res[i] = f(i) mod mod (1 <= i <= N), res[0] = 0
	 */
	public static long[] enumValues(int N, MultiplicativeFunction f, long mod) {
		if (N < 0) {
			throw new IllegalArgumentException("N must be non-negative");
		}
		long[] res = new long[N + 1];
		if (N >= 0) {
			res[0] = 0;
		}
		if (N >= 1) {
			long r1 = 1 % mod;
			if (r1 < 0) r1 += mod;
			res[1] = r1;
		}
		if (N <= 1) {
			return res;
		}

		int[] primes = new int[N + 1];
		int primeCount = 0;
		boolean[] isComposite = new boolean[N + 1];
		int[] pp = new int[N + 1];
		int[] pe = new int[N + 1];

		for (int i = 2; i <= N; i++) {
			if (!isComposite[i]) {
				primes[primeCount++] = i;
				pp[i] = i;
				pe[i] = 1;
				long val = f.evaluatePrimePower(i, 1) % mod;
				if (val < 0) val += mod;
				res[i] = val;
			}
			for (int j = 0; j < primeCount; j++) {
				int p = primes[j];
				if (p > N / i) {
					break;
				}
				int next = i * p;
				isComposite[next] = true;
				if (i % p == 0) {
					pp[next] = pp[i] * p;
					pe[next] = pe[i] + 1;
					long val = f.evaluatePrimePower(p, pe[next]) % mod;
					if (val < 0) val += mod;
					long term = (res[i / pp[i]] * val) % mod;
					if (term < 0) term += mod;
					res[next] = term;
					break;
				} else {
					pp[next] = p;
					pe[next] = 1;
					long term = (res[i] * res[p]) % mod;
					if (term < 0) term += mod;
					res[next] = term;
				}
			}
		}
		return res;
	}

	/**
	 * 定数関数 1(n) = 1 を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(1)
	 *
	 * @return 定数関数 1 の評価器
	 */
	public static MultiplicativeFunction constant() {
		return (p, e) -> 1;
	}

	/**
	 * ディリクレ積 of 単位元 I(1) = 1, I(n) = 0 (n > 1) を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(1)
	 *
	 * @return 単位元 I の評価器
	 */
	public static MultiplicativeFunction identity() {
		return (p, e) -> e == 0 ? 1 : 0;
	}

	/**
	 * 恒等関数 Id(n) = n を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(1)
	 *
	 * @return 恒等関数 Id の評価器
	 */
	public static MultiplicativeFunction id() {
		return (p, e) -> e == 0 ? 1 : MathUtils.pow(p, e);
	}

	/**
	 * べき乗関数 Id_k(n) = n^k を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(log(ek))
	 *
	 * @param k 指数
	 * @return べき乗関数 Id_k の評価器
	 */
	public static MultiplicativeFunction id(int k) {
		return (p, e) -> e == 0 ? 1 : MathUtils.pow(p, (long) k * e);
	}

	/**
	 * べき乗関数 Id_k(n) = n^k mod mod を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(log(ek))
	 *
	 * @param k 指数
	 * @param mod 法
	 * @return べき乗関数 Id_k (mod mod) の評価器
	 */
	public static MultiplicativeFunction id(int k, long mod) {
		return (p, e) -> {
			if (e == 0) {
				long val = 1 % mod;
				if (val < 0) val += mod;
				return val;
			}
			return MathUtils.modPow(p, (long) k * e, mod);
		};
	}

	/**
	 * メビウス関数 mu を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(1)
	 *
	 * @return メビウス関数 mu の評価器
	 */
	public static MultiplicativeFunction moebius() {
		return (p, e) -> {
			if (e == 0) return 1;
			return e == 1 ? -1 : 0;
		};
	}

	/**
	 * オイラーのφ関数 phi を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(log e)
	 *
	 * @return オイラーのφ関数 phi の評価器
	 */
	public static MultiplicativeFunction totient() {
		return (p, e) -> {
			if (e == 0) return 1;
			long pe = MathUtils.pow(p, e - 1);
			return pe * (p - 1);
		};
	}

	/**
	 * オイラーのφ関数 phi (mod mod) を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(log e)
	 *
	 * @param mod 法
	 * @return オイラーのφ関数 phi (mod mod) の評価器
	 */
	public static MultiplicativeFunction totient(long mod) {
		return (p, e) -> {
			if (e == 0) {
				long val = 1 % mod;
				if (val < 0) val += mod;
				return val;
			}
			long pe = MathUtils.modPow(p, e - 1, mod);
			long val = (pe * ((p - 1) % mod)) % mod;
			if (val < 0) val += mod;
			return val;
		};
	}

	/**
	 * 約数関数 sigma_k(n) = sum_(d | n) d^k を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e log k)
	 *
	 * @param k 指数
	 * @return 約数関数 sigma_k の評価器
	 */
	public static MultiplicativeFunction divisor(int k) {
		return (p, e) -> {
			if (e == 0) return 1;
			long sum = 0;
			long pk = MathUtils.pow(p, k);
			long cur = 1;
			for (int i = 0; i <= e; i++) {
				sum += cur;
				cur *= pk;
			}
			return sum;
		};
	}

	/**
	 * 約数関数 sigma_k(n) = sum_(d | n) d^k mod mod を表す評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e log k)
	 *
	 * @param k 指数
	 * @param mod 法
	 * @return 約数関数 sigma_k (mod mod) の評価器
	 */
	public static MultiplicativeFunction divisor(int k, long mod) {
		return (p, e) -> {
			if (e == 0) {
				long val = 1 % mod;
				if (val < 0) val += mod;
				return val;
			}
			long sum = 0;
			long pk = MathUtils.modPow(p, k, mod);
			long cur = 1;
			for (int i = 0; i <= e; i++) {
				sum = (sum + cur) % mod;
				cur = (cur * pk) % mod;
			}
			if (sum < 0) sum += mod;
			return sum;
		};
	}

	/**
	 * 2つの乗法的関数のディリクレ積 (f * g)(p^e) = sum_(j=0)^e f(p^j) g(p^(e-j)) を評価する新しい評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e)
	 *
	 * @param f 評価器 f
	 * @param other 別の乗法的関数
	 * @return f と g のディリクレ積 of 評価器
	 */
	public static MultiplicativeFunction convolve(MultiplicativeFunction f, MultiplicativeFunction other) {
		return (p, e) -> {
			long sum = 0;
			for (int j = 0; j <= e; j++) {
				sum += f.evaluatePrimePower(p, j) * other.evaluatePrimePower(p, e - j);
			}
			return sum;
		};
	}

	/**
	 * 2つの乗法的関数のディリクレ積 (f * g)(p^e) = sum_(j=0)^e f(p^j) g(p^(e-j)) mod mod を評価する新しい評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e)
	 *
	 * @param f 評価器 f
	 * @param other 別の乗法的関数
	 * @param mod 法
	 * @return f と g のディリクレ積 of 評価器
	 */
	public static MultiplicativeFunction convolve(MultiplicativeFunction f, MultiplicativeFunction other, long mod) {
		return (p, e) -> {
			long sum = 0;
			for (int j = 0; j <= e; j++) {
				long valF = f.evaluatePrimePower(p, j) % mod;
				long valG = other.evaluatePrimePower(p, e - j) % mod;
				sum = (sum + valF * valG) % mod;
			}
			if (sum < 0) sum += mod;
			return sum;
		};
	}

	/**
	 * 乗法的関数のディリクレ逆元 g = f^(-1) (g(p^e) = - sum_(j=1)^e f(p^j) g(p^(e-j))) を評価する新しい評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e^2)
	 *
	 * @param f 評価器 f
	 * @return f のディリクレ逆元の評価器
	 */
	public static MultiplicativeFunction inverse(MultiplicativeFunction f) {
		return new MultiplicativeFunction() {
			@Override
			public long evaluatePrimePower(long p, int e) {
				if (e == 0) return 1;
				long[] g = new long[e + 1];
				g[0] = 1;
				for (int i = 1; i <= e; i++) {
					long sum = 0;
					for (int j = 1; j <= i; j++) {
						sum -= f.evaluatePrimePower(p, j) * g[i - j];
					}
					g[i] = sum;
				}
				return g[e];
			}
		};
	}

	/**
	 * 乗法的関数のディリクレ逆元 g = f^(-1) mod mod を評価する新しい評価器を返す。
	 * // 未テスト
	 *
	 * 計算量: O(e^2)
	 *
	 * @param f 評価器 f
	 * @param mod 法
	 * @return f のディリクレ逆元の評価器
	 */
	public static MultiplicativeFunction inverse(MultiplicativeFunction f, long mod) {
		return new MultiplicativeFunction() {
			@Override
			public long evaluatePrimePower(long p, int e) {
				if (e == 0) {
					long val = 1 % mod;
					if (val < 0) val += mod;
					return val;
				}
				long[] g = new long[e + 1];
				g[0] = 1;
				for (int i = 1; i <= e; i++) {
					long sum = 0;
					for (int j = 1; j <= i; j++) {
						long valF = f.evaluatePrimePower(p, j) % mod;
						sum = (sum - valF * g[i - j]) % mod;
					}
					if (sum < 0) sum += mod;
					g[i] = sum;
				}
				return g[e];
			}
		};
	}

	/**
	 * 任意の2つの配列（乗法的とは限らない）に対して、ディリクレ積 (f * g)[n] = sum_(d | n) f[d] g[n/d] を計算する。
	 * // 未テスト
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f 配列 f
	 * @param g 配列 g
	 * @return ディリクレ積 of 配列
	 */
	public static long[] convolve(long[] f, long[] g) {
		return DivisorLattice.mul(f, g);
	}

	/**
	 * 任意の2つの配列（乗法的とは限らない）に対して、ディリクレ積 (f * g)[n] = sum_(d | n) f[d] g[n/d] mod mod を計算する。
	 * // 未テスト
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f 配列 f
	 * @param g 配列 g
	 * @param mod 法
	 * @return ディリクレ積 of 配列
	 */
	public static long[] convolve(long[] f, long[] g, long mod) {
		return DivisorLattice.mul(f, g, mod);
	}

	/**
	 * 任意の配列（乗法的とは限らない）に対して、ディリクレ逆元 g = f^(-1) を計算する。
	 * // 未テスト
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f 配列 f。事前条件として f[1] は 1 または -1 である必要がある。
	 * @return ディリクレ逆元の配列
	 */
	public static long[] inverse(long[] f) {
		int N = f.length - 1;
		if (N < 0) return new long[0];
		long[] res = new long[N + 1];
		if (N >= 1) {
			if (Math.abs(f[1]) != 1) {
				throw new ArithmeticException("f[1] must be 1 or -1 for integer Dirichlet inverse");
			}
			long invF1 = f[1];
			res[1] = invF1;
			for (int i = 1; i <= N; i++) {
				if (i > 1) {
					res[i] = -res[i] * invF1;
				}
				for (int j = 2; i * j <= N; j++) {
					res[i * j] += f[j] * res[i];
				}
			}
		}
		return res;
	}

	/**
	 * 任意の配列（乗法的とは限らない）に対して、ディリクレ逆元 g = f^(-1) mod mod を計算する。
	 * // 未テスト
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f 配列 f。事前条件として f[1] は mod と互いに素である必要がある。
	 * @param mod 法
	 * @return ディリクレ逆元の配列
	 */
	public static long[] inverse(long[] f, long mod) {
		return DivisorLattice.inv(f, mod);
	}
}
