package library.util.polynomial;

import java.util.ArrayList;
import java.util.Arrays;

import library.util.ArrayUtils;
import library.util.MathUtils;
import library.util.Zn;
import library.util.algebra.strategy.CommutativeRingStrategy;

/**
 * 剰余環 Z/nZ 上の多項式。
 */
// 未テスト
public class PolynomialZnDynamic implements CommutativeRingStrategy<long[]> {
	/** 法 mod。 */
	public final long mod;
	/** 剰余演算器。 */
	public final Zn zn;

	public PolynomialZnDynamic(long mod) {
		this.mod = mod;
		this.zn = new Zn(mod);
	}

	protected long addMod(long a, long b) {
		long sum = a + b;
		return sum >= mod ? sum - mod : sum;
	}

	protected long subMod(long a, long b) {
		long diff = a - b;
		return diff < 0 ? diff + mod : diff;
	}

	/**
	 * 多項式の非ゼロ項数を指定した上限まで数える。
	 *
	 * @param a     多項式
	 * @param limit 上限
	 * @return 非ゼロ項数。上限を超える場合は limit + 1
	 *
	 *         <p>
	 *         計算量: O(N)
	 */
	public int countTerms(long[] a, int limit) {
		int count = 0;
		for (long v : a) {
			if (zn.reduce(v) != 0) {
				count++;
				if (count > limit)
					return count;
			}
		}
		return count;
	}

	/**
	 * 0 <= a[i], b[i] < mod を仮定する。
	 *
	 * @param a 多項式の係数配列
	 * @param b 多項式の係数配列
	 * @return a(x) * b(x)
	 */
	public long[] mulNaive(long[] a, long[] b) {
		long[] c = new long[a.length + b.length - 1];
		for (int i = 0; i < a.length; i++) {
			if (a[i] == 0)
				continue;
			for (int j = 0; j < b.length; j++) {
				if (b[j] == 0)
					continue;
				c[i + j] = (c[i + j] + a[i] * b[j]) % mod;
			}
		}
		return c;
	}

	protected long[] mulCRT(long[] a, long[] b) {
		int n = a.length, m = b.length;
		long m1 = 998244353L, m2 = 469762049L, m3 = 167772161L;
		double maxVal = (double) Math.min(n, m) * (mod - 1) * (mod - 1);
		int k_count = 3;
		if (maxVal < m1)
			k_count = 1;
		else if (maxVal < (double) m1 * m2)
			k_count = 2;

		long[] res1 = PolynomialFpDynamic.MOD998244353.mulFFT(a, b);
		if (k_count == 1) {
			for (int i = 0; i < res1.length; i++)
				res1[i] %= mod;
			return res1;
		}
		long[] res2 = PolynomialFpDynamic.MOD469762049.mulFFT(a, b);
		if (k_count == 2) {
			int len = res1.length;
			long[] res = new long[len];
			long[] v = new long[2];
			long[] ms = { m1, m2 };
			for (int i = 0; i < len; i++) {
				v[0] = res1[i];
				v[1] = res2[i];
				res[i] = Zn.crt(v, ms) % mod;
			}
			return res;
		}
		long[] res3 = PolynomialFpDynamic.MOD167772161.mulFFT(a, b);
		int len = res1.length;
		long[] res = new long[len];
		long inv123 = MathUtils.modInv(m1 % m3 * (m2 % m3) % m3, m3);
		long m12m = (m1 % mod) * (m2 % mod) % mod;
		long[] v = new long[2];
		long[] ms = { m1, m2 };
		for (int i = 0; i < len; i++) {
			v[0] = res1[i];
			v[1] = res2[i];
			long x12 = Zn.crt(v, ms);
			long k3 = (res3[i] - x12 % m3 + m3) % m3 * inv123 % m3;
			res[i] = (x12 % mod + (k3 % mod) * m12m % mod) % mod;
		}
		return res;
	}

	/**
	 * 返す配列の長さは a.length + b.length - 1 ただし、a.length = 0 or b.length = 0 の場合は長さ 0
	 * {@inheritDoc}
	 */
	@Override
	public long[] mul(long[] a, long[] b) {
		if (a.length == 0 || b.length == 0)
			return new long[0];
		if (a.length == 1 && b.length == 1)
			return new long[] { a[0] * b[0] % mod };

		int n = a.length, m = b.length;
		if (n + m - 1 <= 128)
			return mulNaive(a, b);

		int sparseThreshold = 50;
		int countB = countTerms(b, sparseThreshold);
		if (countB <= sparseThreshold)
			return sparseMul(a, getTerms(b, countB), b.length);
		int countA = countTerms(a, sparseThreshold);
		if (countA <= sparseThreshold)
			return sparseMul(b, getTerms(a, countA), a.length);

		if (n + m - 1 > 128) {
			return mulCRT(a, b);
		}
		return mulNaive(a, b);
	}

	/**
	 * 多項式の自乗 a(x)^2 を計算する。
	 *
	 * @param a 多項式の係数配列
	 * @return a(x)^2
	 */
	public long[] squared(long[] a) {
		if (a.length == 0)
			return new long[0];
		if (a.length == 1)
			return new long[] { a[0] * a[0] % mod };

		int len = 2 * a.length - 1;
		if (len > 128) {
			return mulCRT(a, a);
		}
		return squaredNaive(a);
	}

	/**
	 * 多項式の自乗 a(x)^2 を O(N^2) の素朴な方法で計算する。
	 *
	 * <p>
	 * 計算量: O(N^2)
	 * </p>
	 *
	 * @param a 多項式の係数配列
	 * @return a(x)^2
	 */
	public long[] squaredNaive(long[] a) {
		int len = 2 * a.length - 1;
		long[] ret = new long[len];
		for (int i = 0; i < a.length; ++i) {
			if (a[i] == 0)
				continue;
			for (int j = i + 1; j < a.length; ++j) {
				if (a[j] == 0)
					continue;
				ret[i + j] = (ret[i + j] + 2 * a[i] * a[j]) % mod;
			}
		}
		for (int i = 0; i < a.length; ++i) {
			if (a[i] == 0)
				continue;
			ret[2 * i] = (ret[2 * i] + a[i] * a[i]) % mod;
		}
		return ret;
	}

	public long[] powFull(long[] a, int n) {
		if (n == 0)
			return new long[] { 1 };
		if (n == 1)
			return a.clone();
		int d = deg(a);
		if (d == -1)
			return new long[0];
		if (d == 0)
			return new long[] { MathUtils.modPow(a[0], n, mod) };
		long[] ret = new long[1];
		ret[0] = 1;
		long[] b = a.clone();
		while (n != 0) {
			if (n % 2 == 1) {
				ret = mul(ret, b);
			}
			n /= 2;
			if (n == 0)
				break;
			b = mul(b, b);
		}
		return ret;
	}

	@Override
	public long[] zero() {
		return new long[0];
	}

	@Override
	public long[] one() {
		return new long[] { 1 };
	}

	public long[] x() {
		return new long[] { 0, 1 };
	}

	@Override
	public long[] add(long[] a, long[] b) {
		long[] ret = new long[Math.max(a.length, b.length)];
		for (int i = 0; i < ret.length; ++i) {
			ret[i] = (i < a.length ? a[i] : 0) + (i < b.length ? b[i] : 0);
			if (ret[i] >= mod)
				ret[i] -= mod;
		}
		return ret;
	}

	@Override
	public long[] neg(long[] a) {
		long[] ret = new long[a.length];
		for (int i = 0; i < a.length; i++) {
			if (a[i] != 0)
				ret[i] = mod - a[i];
		}
		return ret;
	}

	@Override
	public boolean equals(long[] a, long[] b) {
		int n = Math.max(a.length, b.length);
		for (int i = 0; i < n; i++) {
			long va = i < a.length ? a[i] : 0;
			long vb = i < b.length ? b[i] : 0;
			if (va != vb)
				return false;
		}
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PolynomialZnDynamic that = (PolynomialZnDynamic) o;
		return mod == that.mod;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(mod);
	}

	public long norm(long[] a) {
		return deg(a) + 1;
	}

	public long[] canonicalUnit(long[] a) {
		int d = deg(a);
		if (d == -1)
			return one();
		return new long[] { a[d] };
	}

	@Override
	public long[] sub(long[] a, long[] b) {
		if (a.length == 1 && b.length == 1) {
			long diff = a[0] - b[0];
			if (diff < 0)
				diff += mod;
			return new long[] { diff };
		}
		long[] ret = new long[Math.max(a.length, b.length)];
		for (int i = 0; i < ret.length; ++i) {
			ret[i] = (i < a.length ? a[i] : 0) - (i < b.length ? b[i] : 0);
			if (ret[i] < 0)
				ret[i] += mod;
		}
		return ret;
	}

	/**
	 * 多項式の非ゼロ項数を返す。
	 *
	 * @param a 多項式
	 * @return 非ゼロ項数
	 *
	 *         <p>
	 *         計算量: O(N)
	 */
	public int countTerms(long[] a) {
		int count = 0;
		for (long v : a)
			if (zn.reduce(v) != 0)
				count++;
		return count;
	}

	/**
	 * 多項式の次数を返す。ゼロ多項式なら-1。
	 */
	public int deg(long[] a) {
		int n = a.length;
		if (n == 0)
			return -1;
		if (a[n - 1] != 0)
			return n - 1;
		for (int i = n - 2; i >= 0; i--) {
			if (a[i] != 0)
				return i;
		}
		return -1;
	}

	/**
	 * 多項式 f が 0 かを判定する。
	 *
	 * @param f 多項式
	 * @return f が 0 なら true
	 *
	 *         <p>
	 *         計算量: O(deg f)
	 */
	public boolean isZero(long[] f) {
		return deg(f) == -1;
	}

	/**
	 * 末尾の0を削った長さにする。
	 */
	public long[] resize(long[] a) {
		return Arrays.copyOf(a, Math.max(0, deg(a)) + 1);
	}

	public static class Term {
		public final int d;
		public final long v;

		public Term(int d, long v) {
			this.d = d;
			this.v = v;
		}
	}

	/**
	 * 稀な多項式（項数が少ない多項式）との乗算 a(x) * sparsePoly(x) を行う。
	 *
	 * @param a          多項式
	 * @param sparsePoly 稀な多項式
	 * @return a(x) * sparsePoly(x)
	 *
	 *         <p>
	 *         計算量: O(NK)。ここで N = deg a, K = (sparsePoly の非ゼロ項数)。
	 */
	public long[] sparseMul(long[] a, long[] sparsePoly) {
		if (a.length == 0 || sparsePoly.length == 0)
			return new long[0];
		ArrayList<Term> terms = new ArrayList<>();
		for (int i = 0; i < sparsePoly.length; i++) {
			long v = zn.reduce(sparsePoly[i]);
			if (v != 0)
				terms.add(new Term(i, v));
		}
		return sparseMul(a, terms, sparsePoly.length);
	}

	/**
	 * 非ゼロ項がリスト形式で与えられた稀な多項式との乗算を行う。
	 *
	 * @param a           多項式
	 * @param sparseTerms 稀な多項式の非ゼロ項のリスト
	 * @param sparseLen   稀な多項式の元の長さ
	 * @return a(x) * sparsePoly(x)
	 *
	 *         <p>
	 *         計算量: O(NK)
	 */
	public long[] sparseMul(long[] a, ArrayList<Term> sparseTerms, int sparseLen) {
		if (a.length == 0 || sparseLen == 0)
			return new long[0];
		long[] res = new long[a.length + sparseLen - 1];
		if (sparseTerms.isEmpty())
			return res;
		for (int i = 0; i < a.length; i++) {
			long v = zn.reduce(a[i]);
			if (v == 0)
				continue;
			for (Term t : sparseTerms) {
				res[i + t.d] = (res[i + t.d] + v * t.v) % mod;
			}
		}
		return res;
	}

	/**
	 * 多項式の非ゼロ項sのリストを返す。
	 *
	 * @param p 多項式
	 * @return 非ゼロ項のリスト
	 */
	public ArrayList<Term> getTerms(long[] p) {
		ArrayList<Term> terms = new ArrayList<>();
		for (int i = 0; i < p.length; i++) {
			long v = zn.reduce(p[i]);
			if (v != 0)
				terms.add(new Term(i, v));
		}
		return terms;
	}

	/**
	 * 多項式の非ゼロ項のリストを、予測される容量を指定して返す。
	 *
	 * @param p               多項式
	 * @param initialCapacity 初期容量
	 * @return 非ゼロ項のリスト
	 */
	public ArrayList<Term> getTerms(long[] p, int initialCapacity) {
		ArrayList<Term> terms = new ArrayList<>(initialCapacity);
		for (int i = 0; i < p.length; i++) {
			long v = zn.reduce(p[i]);
			if (v != 0)
				terms.add(new Term(i, v));
		}
		return terms;
	}

	public long[] negatedX(long[] f) {
		long[] g = Arrays.copyOf(f, f.length);
		for (int i = 1; i < g.length; i += 2)
			if (g[i] != 0)
				g[i] = mod - g[i];
		return g;
	}

	public long[] divideByX(long[] f, int repeat) {
		return Arrays.copyOfRange(f, repeat, f.length);
	}

	public long[] multiplyByX(long[] f, int repeat) {
		long[] ret = new long[f.length + repeat];
		for (int i = 0; i < f.length; ++i)
			ret[repeat + i] = f[i];
		return ret;
	}

	public CommutativeRingStrategy<long[]> truncatedStrategy(int n) {
		if (n < 0) {
			throw new IllegalArgumentException("n must be non-negative");
		}
		return new CommutativeRingStrategy<long[]>() {
			private long[] truncateAndTrim(long[] a, int limit) {
				if (a.length <= limit) {
					return a;
				}
				return Arrays.copyOf(a, limit);
			}

			@Override
			public long[] zero() {
				return PolynomialZnDynamic.this.zero();
			}

			@Override
			public long[] one() {
				return truncateAndTrim(PolynomialZnDynamic.this.one(), n);
			}

			@Override
			public long[] add(long[] a, long[] b) {
				return truncateAndTrim(PolynomialZnDynamic.this.add(truncateAndTrim(a, n), truncateAndTrim(b, n)), n);
			}

			@Override
			public long[] sub(long[] a, long[] b) {
				return truncateAndTrim(PolynomialZnDynamic.this.sub(truncateAndTrim(a, n), truncateAndTrim(b, n)), n);
			}

			@Override
			public long[] mul(long[] a, long[] b) {
				return truncateAndTrim(PolynomialZnDynamic.this.mul(truncateAndTrim(a, n), truncateAndTrim(b, n)), n);
			}

			@Override
			public long[] neg(long[] a) {
				return truncateAndTrim(PolynomialZnDynamic.this.neg(truncateAndTrim(a, n)), n);
			}

			@Override
			public boolean equals(long[] a, long[] b) {
				return PolynomialZnDynamic.this.equals(truncateAndTrim(a, n), truncateAndTrim(b, n));
			}
		};
	}
}
