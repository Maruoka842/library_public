package library.util.poset;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import library.util.ArrayUtils;
import library.util.Fp;
import library.util.Ints;
import library.util.MathUtils;
import library.util.polynomial.PolynomialFpDynamic;

public class BooleanLattice {
	
	/**
	 * b(S) = op_(S ⊆ T) a(T) を計算する。
	 *
	 * @param a 入力配列
	 * @param op 二項演算
	 * @return superset zeta transform
	 */
	public static long[] supsetZeta(long[] a, LongBinaryOperator op) {
		long[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 1) continue;
				b[s] = op.applyAsLong(b[s], b[s | (1 << i)]);
			}
		}
		return b;
	}

	/**
	 * b(S) = op_(S ⊆ T) a(T) を計算する。
	 *
	 * @param a 入力配列
	 * @param op 二項演算
	 * @return superset zeta transform
	 */
	public static int[] supsetZeta(int[] a, IntBinaryOperator op) {
		int[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 1) continue;
				b[s] = op.applyAsInt(b[s], b[s | (1 << i)]);
			}
		}
		return b;
	}
	
	
	/**
	 * b(S) = op_(T ⊆ S) a(T) を計算する。
	 *
	 * @param a 入力配列
	 * @param op 二項演算
	 * @return subset zeta transform
	 */
	public static long[] subsetZeta(long[] a, LongBinaryOperator op) {
		long[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 0) continue;
				b[s] = op.applyAsLong(b[s], b[s ^ (1 << i)]);
			}
		}
		return b;
	}

	/**
	 * b(S) = op_(T ⊆ S) a(T) を計算する。
	 *
	 * @param a 入力配列
	 * @param op 二項演算
	 * @return subset zeta transform
	 */
	public static int[] subsetZeta(int[] a, IntBinaryOperator op) {
		int[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 0) continue;
				b[s] = op.applyAsInt(b[s], b[s ^ (1 << i)]);
			}
		}
		return b;
	}
	
	/**
	 * https://judge.yosupo.jp/submission/358736
	 * @param a
	 * @param b
	 * @param mod
	 * @return
	 */
	public static long[] compose(long[]a, long[]b, long mod) {
		// a(b) = Σ a^(i)(b[0]) * (b-b[0])^i /i!
		int n = MathUtils.floorLog2(b.length);
		long[] powb0 = new long[a.length+1];// powb0[i] = b[0] ^ i
		powb0[0] = 1;
		for (int i = 0; i < powb0.length - 1; i++) {
			powb0[i + 1] = b[0] * powb0[i] % mod;
		}
		long[][] c = new long[n + 1][1];//c[i] = a^(i)(b[0])
		for (int i = 0; i <= n && i <= a.length; i++) {
			for (int j = i; j < a.length; j++) {
				long fac = 1;
				for (int k = 0; k < i; k++) {
					fac = fac * (j - k) % mod;
				}
				c[i][0] += fac * a[j] % mod * powb0[j - i];
				c[i][0] %= mod;
			}
		}
		// d = b - b[0] と置く。
		// a(b) = c(d) = Σ c[i] * d^i /i!
		
		// c[k] = a^(k)(b) ただしx_{i+1},x_{i+2},..,x_{n-1}は0とする
		//　となるようにテイラー展開を用いて更新する。
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n - i; j++) {
				long[]g = Arrays.copyOfRange(b, 1 << i, 1 << (i + 1));
				long[]h = mul(c[j + 1], g, mod);
				c[j] = Arrays.copyOf(c[j], 2 * c[j].length);
				for (int k = 0; k < h.length; k++) {
					c[j][k | (1 << i)] = h[k];
				}
			}
		}
		return c[0];
	}
	
	/**
	 * 集合関数 a の指数関数 exp(a) を計算する。
	 * ただし、a[0] = 0 である必要がある。
	 * exp(a0 + a1 X) = exp(a0) + exp(a0) a1 X の関係を用いて、
	 * ビットごとに再帰的に計算する。
	 * 計算量: O(N^2 2^N) (N = log2(as.length))
	 * @param as 集合関数
	 * @param mod 法
	 * @return exp(as)
	 */
	public static long[] exp(long[] as, long mod) {
		if (as[0] != 0) throw new AssertionError();
		int n = MathUtils.floorLog2(as.length);
		long[] bs = {1};
		for (int m = 0; m < n; m++) {
			long[] a_part = Arrays.copyOfRange(as, 1 << m, 1 << (m + 1));
			long[] cs = mul(bs, a_part, mod);
			long[] next_bs = new long[bs.length + cs.length];
			System.arraycopy(bs, 0, next_bs, 0, bs.length);
			System.arraycopy(cs, 0, next_bs, bs.length, cs.length);
			bs = next_bs;
		}
		return bs;
	}

	public static long[] mul(long[]a, long[]b, long mod) {
		//https://judge.yosupo.jp/submission/371224
		long[][] zetaA = rankLiftedZeta(a, mod);
		long[][] zetaB = rankLiftedZeta(b, mod);
		int n = zetaA.length - 1;
		int mask = 1 << n;
		long[][] h = new long[n + 1][mask];
		for (int s = 0; s < mask; s++) {
			for (int i = 0; i <= n; i++) {
				if (zetaA[i][s] == 0) continue;
				for (int j = 0; i + j <= n; j++) {
					h[i + j][s] = (h[i + j][s] + zetaA[i][s] * zetaB[j][s]) % mod;
				}
			}
		}
		return rankLiftedMoebiusAndUnlift(h, mod);
	}

	/**
	 * 集合関数 a の Rank-Lifted Zeta 変換を計算する。
	 * 集合のサイズ k ごとに、そのサイズの集合の部分集合の値を累積する。
	 * 計算量: O(N^2 2^N) (N = log2(a.length))
	 * @param a 集合関数
	 * @param mod 法
	 * @return zeta[k][S] (size(S)=k のとき a[S], それ以外は 0) を Zeta 変換した二次元配列
	 */
	public static long[][] rankLiftedZeta(long[] a, long mod) {
		//https://judge.yosupo.jp/submission/371243
		int n = MathUtils.floorLog2(a.length);
		int mask = 1 << n;
		long[][] zeta = new long[n + 1][mask];
		for (int i = 0; i < mask; i++) {
			zeta[Integer.bitCount(i)][i] = a[i];
		}
		for (int k = 0; k <= n; k++) {
			for (int i = 0; i < n; i++) {
				for (int s = 0; s < mask; s++) {
					if (Integer.bitCount(s) < k) continue;
					if ((s & (1 << i)) == 0) {
						zeta[k][s | (1 << i)] += zeta[k][s];
						if (zeta[k][s | (1 << i)] >= mod) zeta[k][s | (1 << i)] -= mod;
					}
				}
			}
		}
		return zeta;
	}

	/**
	 * 未テスト
	 * Rank-Lifted Zeta 変換された二次元配列に対し、Rank-Lifted Moebius 逆変換を施して元の集合関数を復元する。
	 * 計算量: O(N^2 2^N)
	 * @param zeta Rank-Lifted Zeta 変換後の二次元配列
	 * @param mod 法
	 * @return 復元された集合関数
	 */
	public static long[] rankLiftedMoebiusAndUnlift(long[][] zeta, long mod) {
		int n = zeta.length - 1;
		int mask = 1 << n;
		for (int k = 0; k <= n; k++) {
			for (int i = 0; i < n; i++) {
				for (int s = mask - 1; s >= 0; s--) {
					if (Integer.bitCount(s) > k) continue;
					if ((s & (1 << i)) != 0) {
						zeta[k][s] -= zeta[k][s ^ (1 << i)];
						if (zeta[k][s] < 0) zeta[k][s] += mod;
					}
				}
			}
		}
		long[] res = new long[mask];
		for (int i = 0; i < mask; i++) {
			res[i] = zeta[Integer.bitCount(i)][i];
		}
		return res;
	}
	
	
	/**
	 * c[S]=Σ[S=U∪V]a[U]b[V]
	 * O(n2^n)
	 * @param a
	 * @param b
	 * @return
	 */
	public static long[] orConvolution(long[] a, long[] b) {
		var a2=zeta(a);
		var b2=zeta(b);
		for (int i = 0; i < a2.length; i++) {
			a2[i]*=b2[i];
		}
		return moebius(a2);
	}

	/**
	 * {@code b[S] = product_(T subseteq S) a[T] (mod mod)} で定まる
	 * multiplicative subset zeta 変換 {@code b} を返す。
	 * 入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(n 2^n)}、追加領域: {@code O(2^n)}。
	 *
	 * @param a 長さ {@code 2^n} の配列。各要素は {@code [0, mod)}
	 * @param mod 法。{@code 1 < mod} かつ {@code (mod - 1)^2 <= Long.MAX_VALUE}
	 * @return multiplicative subset zeta 変換後の配列
	 * @throws IllegalArgumentException 配列長が 2 の冪でない場合、または {@code mod} が条件を満たさない場合
	 */
	// 未テスト
	public static long[] multiplicativeZeta(long[] a, long mod) {
		int n = Integer.numberOfTrailingZeros(a.length);
		if (a.length == 0 || a.length != 1 << n) {
			throw new IllegalArgumentException("a.length must be a power of two");
		}
		if (mod <= 1 || mod > 3037000500L) {
			throw new IllegalArgumentException("mod must satisfy 1 < mod and (mod - 1)^2 <= Long.MAX_VALUE");
		}
		long[] b = Arrays.copyOf(a, a.length);
		for (int i = 0; i < n; ++i) {
			for (int s = 0; s < 1 << n; ++s) {
				if ((s & (1 << i)) == 0) {
					b[s | (1 << i)] = b[s | (1 << i)] * b[s] % mod;
				}
			}
		}
		return b;
	}

	/**
	 * Boolean lattice {@code 2^[n]} 上の OR convolution を {@code *} とする。
	 * {@code f_i = delta_empty + coefficients[i] delta_{subsets[i]}} とおくとき、
	 * {@code product_(0 <= i < m) f_i^{exponents[i]}} を返す。
	 * ここで冪および積は {@code *} に関するものであり、全演算は {@code mod} を法とする。
	 *
	 * <p>{@code subsets} に同じ集合が複数回現れてもよい。入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(m log E + n 2^n)}、追加領域: {@code O(2^n)}。
	 * ただし {@code m = subsets.length}、{@code E = max_i exponents[i]}。
	 *
	 * @param n 台集合の要素数。{@code 0 <= n < 31}
	 * @param subsets 第 {@code i} 因子の非定数項の集合を表すビットマスク
	 * @param coefficients 第 {@code i} 因子の非定数項の係数。各要素は {@code [0, mod)}
	 * @param exponents 第 {@code i} 因子の正整数指数
	 * @param mod 法。{@code 1 < mod} かつ {@code (mod - 1)^2 <= Long.MAX_VALUE}
	 * @return OR convolution に関する積の係数配列
	 * @throws IllegalArgumentException 入力が各引数に記した条件を満たさない場合
	 */
	// 未テスト
	public static long[] orConvolutionProductOfBinomialPowers(
			int n, int[] subsets, long[] coefficients, long[] exponents, long mod) {
		if (n < 0 || n >= 31) throw new IllegalArgumentException("n must be in [0, 31)");
		if (subsets.length != coefficients.length || subsets.length != exponents.length) {
			throw new IllegalArgumentException("input lengths must be equal");
		}
		if (mod <= 1 || mod > 3037000500L) {
			throw new IllegalArgumentException("mod must satisfy 1 < mod and (mod - 1)^2 <= Long.MAX_VALUE");
		}
		int size = 1 << n;
		long[] factors = new long[size];
		Arrays.fill(factors, 1);
		for (int i = 0; i < subsets.length; ++i) {
			if (subsets[i] < 0 || subsets[i] >= size) {
				throw new IllegalArgumentException("subsets[" + i + "] is outside [0, 2^n)");
			}
			if (exponents[i] <= 0) {
				throw new IllegalArgumentException("exponents[" + i + "] must be positive");
			}
			if (coefficients[i] < 0 || coefficients[i] >= mod) {
				throw new IllegalArgumentException("coefficients[" + i + "] is outside [0, mod)");
			}
			long zetaValue = coefficients[i] + 1;
			if (zetaValue == mod) zetaValue = 0;
			factors[subsets[i]] = factors[subsets[i]]
					* MathUtils.modPow(zetaValue, exponents[i], mod) % mod;
		}
		return moebius(multiplicativeZeta(factors, mod), mod);
	}
	
	/**
	 * 未テスト
	 * c[S]=Σ[S=U∩V]a[U]b[V]
	 * O(n2^n)
	 * @param a
	 * @param b
	 * @return
	 */
	public static long[] andConvolution(long[] a, long[] b) {
		long[] a2 = supsetZeta(a);
		long[] b2 = supsetZeta(b);
		for (int i = 0; i < a2.length; i++) {
			a2[i] *= b2[i];
		}
		return supsetMoebius(a2);
	}
	
	/**
	 * c[S]=Σ[S=U∩V]a[U]b[V]
	 * O(n2^n)
	 * @param a
	 * @param b
	 * @param mod
	 * @return
	 */
	public static long[] andConvolution(long[] a, long[] b, long mod) {
		//https://judge.yosupo.jp/submission/370183
		long[] a2 = supsetZeta(a, mod);
		long[] b2 = supsetZeta(b, mod);
		for (int i = 0; i < a2.length; i++) {
			a2[i] = a2[i] * b2[i] % mod;
		}
		return supsetMoebius(a2, mod);
	}
	
	/**
	 * <p>
	 * {@code a[S] == true} は部分集合 {@code S} が利用可能であることを表す。
	 * このメソッドは、利用可能な部分集合のみを用いて全体集合
	 * {@code {0,1,...,n-1}} を被覆するために必要な集合数の最小値を返す。
	 * </p>
	 * O(2^n)
	 * @param a 利用可能な部分集合を表す配列。
	 *          {@code a[S]} が {@code true} なら部分集合 {@code S} を選択可能。
	 *          配列長は {@code 2^n} でなければならない。
	 * @return 全体集合を被覆するために必要な集合数の最小値。
	 *         被覆できない場合は {@code -1} を返す。
	 */
	public static int minmumSetCoverSize(boolean[] a) {
		// https://atcoder.jp/contests/past18-open/editorial/11427
		int n=MathUtils.floorLog2(a.length);
		long[]f=new long[1<<n];
		for (int i = 0; i < a.length; i++) {
			f[i]=a[i]?1:0;
		}
		long[]dp=new long[1<<n];
		Arrays.fill(dp, 1);
		long[]zeta=zeta(f);
		for (int i = 1; i <= n; i++) {//i乗を計算する
			for (int j = 0; j < 1 << (n - i + 1); j++) {
				dp[j]=dp[j]*zeta[j];
			}
			// val = mobius(dp) [(1 << n) - 1)] 
			long val=0;
			for (int j = 0; j < 1 << (n - i + 1); j++) {
				if((n - i + 1 -Integer.bitCount(j))%2==0) {
					val+=dp[j];
				} else {
					val-=dp[j];
				}
			}
			if(val>0)return i;
			for (int j = 0; j < 1 << (n - i); j++) {
				dp[j] = -dp[j] + dp[j | (1 << (n - i))];
			}
		}
		return -1;
	}

	
	/**
	 * O(3^n)
	 * @param a
	 * @param mod
	 * @return
	 */
	public static long[] log(long[] a, long mod) {
		if (a[0] != 1) throw new AssertionError();
		long[] b = Arrays.copyOf(a, a.length);
		b[0] = 0;
		int n=MathUtils.floorLog2(a.length);
		for (int s = 1; s < 1 << n; ++s) {
			int mask=(1<<n)-1-s;
			for (int t = 0; t <= mask; t=(t-mask)&mask) {
				//subsetを昇順に舐める。
				//s=mask^tと置くと
				//(t-mask)&mask
				//=(-s)&mask
				//=(2^n-1-(s-1))&mask
				
				if(t>s) {
					b[s|t]-=b[t]*b[s];
					b[s|t]%=mod;
					if(b[s|t]<0)b[s|t]+=mod;
				}
    			if(t==mask)break;
    		}
    	}
		return b;
	}
	
	/**
	 * O(3^n)
	 * @param a
	 * @param mod
	 * @return
	 */
	public static long[] expNaive(long[] a, long mod) {
		if(a[0]!=0)throw new AssertionError();
		int n = MathUtils.floorLog2(a.length);
		long[] b = new long[a.length];
		b[0]=1;
		for (int s = 1; s < 1 << n; ++s) {
    		for (int t = s; t >= 1; t=(t-1)&s) {
    			if(t>(s^t)) {
    				b[s]+=a[t]*b[s^t];
    				b[s]%=mod;
    			}
    			if(t==0)break;
    		}
    	}
		return b;
	}
	
	//https://atcoder.jp/contests/abc294/submissions/78521482
	public static long[] pow(long[] a, int k, long mod) {
		if (k < 0) {
			throw new AssertionError();
		}

		int n = MathUtils.floorLog2(a.length);

		if (k == 0) {
			long[] ret = new long[a.length];
			ret[0] = 1;
			return ret;
		}
		if (k == 1) {
			return a.clone();
		}

		int maxDerivative = Math.min(n, k);
		long[][] c = new long[maxDerivative + 1][1];

		/*
		 * c[j][0] = (k)_j * a[0]^(k-j)
		 */
		c[maxDerivative][0] = MathUtils.modPow(a[0], k - maxDerivative, mod);
		for (int j = maxDerivative - 1; j >= 0; j--) {
			c[j][0] = c[j + 1][0] * a[0] % mod;
		}

		long falling = 1;
		for (int j = 0; j <= maxDerivative; j++) {
			c[j][0] = c[j][0] * falling % mod;
			if (j < maxDerivative) {
				falling = falling * ((k - j) % mod) % mod;
			}
		}

		/*
		 * 最大の subset convolution は dimension n-1。
		 *
		 * workspace は一度だけ最大サイズで確保して全段で再利用する。
		 * これにより、段を跨いだ巨大配列の GC 遅延による
		 * peak memory 増加も避ける。
		 */
		CompactSubsetConvolutionMultiplier multiplier =
				new CompactSubsetConvolutionMultiplier(Math.max(0, n - 1), mod);

		for (int i = 0; i < n; i++) {
			int size = 1 << i;

			/*
			 * g[S] = a[size + S]
			 *
			 * copyOfRange も行わず、a の slice を直接 Rank-Lifted Zeta する。
			 */
			multiplier.prepareRight(a, size, i);

			int nextMaxDerivative = Math.min(maxDerivative, n - i - 1);

			for (int j = 0; j <= nextMaxDerivative; j++) {
				long[] h = j < maxDerivative ? multiplier.mul(c[j + 1]) : null;

				/*
				 * c[nextMaxDerivative + 1] はこれが最後の使用。
				 */
				if (j == nextMaxDerivative && j < maxDerivative) {
					c[j + 1] = null;
				}

				c[j] = Arrays.copyOf(c[j], size << 1);

				if (h != null) {
					System.arraycopy(h, 0, c[j], size, size);
				}
			}
		}

		return c[0];
	}


	private static class CompactSubsetConvolutionMultiplier {
		final long mod;

		final byte[] rank;
		final int[] lowerOffset;
		final int[] upperOffset;

		/*
		 * lowerA, lowerB:
		 *
		 *   rank <= |S|
		 *
		 * の部分だけを保存する Rank-Lifted Zeta。
		 *
		 * upper:
		 *
		 *   rank >= |S|
		 *
		 * の部分だけを保存する積および Möbius 用配列。
		 */
		final long[] lowerA;
		final long[] lowerB;
		final long[] upper;

		int n;
		int mask;
		int lowerSize;
		int upperSize;

		CompactSubsetConvolutionMultiplier(int maxN, long mod) {
			this.mod = mod;

			int maxMask = 1 << maxN;

			rank = new byte[maxMask];
			lowerOffset = new int[maxMask + 1];
			upperOffset = new int[maxMask + 1];

			int maxTriangularSize;
			if (maxN == 0) {
				maxTriangularSize = 1;
			} else {
				maxTriangularSize = (maxN + 2) * (1 << (maxN - 1));
			}

			lowerA = new long[maxTriangularSize];
			lowerB = new long[maxTriangularSize];
			upper = new long[maxTriangularSize];
		}

		/*
		 * b[S] = source[sourceOffset + S] として右辺を準備する。
		 */
		void prepareRight(long[] source, int sourceOffset, int n) {
			this.n = n;
			this.mask = 1 << n;

			rank[0] = 0;
			for (int s = 1; s < mask; s++) {
				rank[s] = (byte) ((rank[s >>> 1] & 0xff) + (s & 1));
			}

			lowerOffset[0] = 0;
			upperOffset[0] = 0;

			for (int s = 0; s < mask; s++) {
				int q = rank[s] & 0xff;
				lowerOffset[s + 1] = lowerOffset[s] + q + 1;
				upperOffset[s + 1] = upperOffset[s] + n - q + 1;
			}

			lowerSize = lowerOffset[mask];
			upperSize = upperOffset[mask];

			Arrays.fill(lowerB, 0, lowerSize, 0);

			for (int s = 0; s < mask; s++) {
				int q = rank[s] & 0xff;
				long x = source[sourceOffset + s] % mod;
				if (x < 0) {
					x += mod;
				}
				lowerB[lowerOffset[s] + q] = x;
			}

			lowerZeta(lowerB);
		}

		long[] mul(long[] a) {
			Arrays.fill(lowerA, 0, lowerSize, 0);

			for (int s = 0; s < mask; s++) {
				int q = rank[s] & 0xff;
				long x = a[s] % mod;
				if (x < 0) {
					x += mod;
				}
				lowerA[lowerOffset[s] + q] = x;
			}

			lowerZeta(lowerA);

			Arrays.fill(upper, 0, upperSize, 0);

			/*
			 * Rank-Lifted Zeta 後の多項式積。
			 *
			 * 最終的に必要なのは Möbius 後の
			 *
			 *   rank = |S|
			 *
			 * だけ。
			 *
			 * rank r の値は |S| <= r の状態からしか
			 * diagonal に寄与しないため、
			 *
			 *   i + j >= |S|
			 *
			 * の項だけ保存すればよい。
			 */
			for (int s = 0; s < mask; s++) {
				int q = rank[s] & 0xff;
				int lo = lowerOffset[s];
				int hi = upperOffset[s];

				for (int i = 0; i <= q; i++) {
					long x = lowerA[lo + i];
					if (x == 0) {
						continue;
					}

					int from = Math.max(0, q - i);
					int to = Math.min(q, n - i);

					for (int j = from; j <= to; j++) {
						long y = lowerB[lo + j];
						if (y == 0) {
							continue;
						}

						int r = i + j;
						int index = hi + r - q;

						long v = upper[index] + x * y % mod;
						if (v >= mod) {
							v -= mod;
						}
						upper[index] = v;
					}
				}
			}

			/*
			 * Upper-triangular なまま Möbius。
			 *
			 * subset S には rank >= |S| の成分しか保存していない。
			 */
			for (int bit = 1; bit < mask; bit <<= 1) {
				int block = bit << 1;

				for (int base = 0; base < mask; base += block) {
					for (int d = 0; d < bit; d++) {
						int lo = base + d;
						int hi = lo + bit;

						int q = rank[hi] & 0xff;

						int hiOffset = upperOffset[hi];
						int loOffset = upperOffset[lo];

						/*
						 * |hi| = q
						 * |lo| = q - 1
						 *
						 * rank r の格納位置は
						 *
						 * hi: r - q
						 * lo: r - (q-1) = r-q+1
						 */
						for (int r = q; r <= n; r++) {
							int p = hiOffset + r - q;

							long x = upper[p] - upper[loOffset + r - q + 1];
							if (x < 0) {
								x += mod;
							}
							upper[p] = x;
						}
					}
				}
			}

			long[] ret = new long[mask];

			/*
			 * upperOffset[S] は rank = |S| の位置。
			 */
			for (int s = 0; s < mask; s++) {
				ret[s] = upper[upperOffset[s]];
			}

			return ret;
		}

		void lowerZeta(long[] zeta) {
			for (int bit = 1; bit < mask; bit <<= 1) {
				int block = bit << 1;

				for (int base = 0; base < mask; base += block) {
					for (int d = 0; d < bit; d++) {
						int lo = base + d;
						int hi = lo + bit;

						int q = rank[lo] & 0xff;
						int loOffset = lowerOffset[lo];
						int hiOffset = lowerOffset[hi];

						for (int r = 0; r <= q; r++) {
							long x = zeta[hiOffset + r] + zeta[loOffset + r];
							if (x >= mod) {
								x -= mod;
							}
							zeta[hiOffset + r] = x;
						}
					}
				}
			}
		}
	}
	
	
	public static long[] pow(long[] a, int k) {
		if(k == 0) {
			long[]ret=new long[a.length];
			ret[0]=1;
			return ret;
		}
		var ret = pow(mulNaive(a, a), k/2);
		if (k % 2 == 1) {
			ret = mulNaive(ret, a);
		}
		return ret;
	}
	
	public static long[] add(long[] a, long[] b, long mod) {
		long[] c=new long[a.length];
		for (int i = 0; i < a.length; i++) {
			c[i]=(a[i]+b[i])%mod;
		}
		return c;
	}
	
	/**
	 * c[s] = Σ (a[s \ t] * b[t])
	 * O(3^n)
	 * @param a
	 * @param b
	 * @param mod
	 * @return
	 */
    public static long[] mulNaive(long[] a, long[] b, long mod) {
    	int n = MathUtils.floorLog2(a.length);
    	long[] c = new long[a.length];
    	for (int s = 0; s < 1 << n; ++s) {
    		for (int t = s; t >= 0; t=(t-1)&s) {
    			c[s]+=a[s^t]*b[t];
    			c[s]%=mod;
    			if(t==0)break;
    		}
    	}
    	return c;
    }
    
    
    public static long[] mulNaive(long[] a, long[] b) {
    	int n = MathUtils.floorLog2(a.length);
    	long[] c = new long[a.length];
    	for (int s = 0; s < 1 << n; ++s) {
    		for (int t = s; t >= 0; t=(t-1)&s) {
    			c[s]+=a[s^t]*b[t];
    			if(t==0)break;
    		}
    	}
    	return c;
    }
    
    public static long[] zeta(long[] a, long mod) {
    	long[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]+=b[s];
    			b[s|(1<<i)]%=mod;
    		}
    	}
    	return b;
    }
    
    /**
     * + を logical or で定義
     * @param a
     * @return
     */
    public static boolean[] zeta(boolean[] a) {
    	boolean[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]|=b[s];
    		}
    	}
    	return b;
    }
    
    
    public static long[] zeta(long[] a) {
    	long[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]+=b[s];
    		}
    	}
    	return b;
    }
    
    public static double[] zeta(double[] a) {
    	double[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]+=b[s];
    		}
    	}
    	return b;
    }
    
    /**
     * 未テスト
     * b[S] = Σ[S ⊆ T] a[T]
     * @param a
     * @return
     */
    public static long[] supsetZeta(long[] a) {
    	long[] b = Arrays.copyOf(a, a.length);
    	int N = MathUtils.floorLog2(a.length);
    	for (int i = 0; i < N; ++i) {
    		for (int s = 0; s < 1 << N; ++s) {
    			int ns = s | (1 << i);
    			if (ns == s)
    				continue;
    			b[s] += b[ns];
    		}
    	}
    	return b;
    }
    
    /**
     * 未テスト
     * b[S] = Σ[S ⊆ T] a[T]
     * @param a
     * @param mod
     * @return
     */
    public static long[] supsetZeta(long[] a, long mod) {
    	//https://judge.yosupo.jp/submission/370183
    	long[] b = Arrays.copyOf(a, a.length);
    	int N = MathUtils.floorLog2(a.length);
    	for (int i = 0; i < N; ++i) {
    		for (int s = 0; s < 1 << N; ++s) {
    			int ns = s | (1 << i);
    			if (ns == s)
    				continue;
    			b[s] += b[ns];
    			b[s] %= mod;
    		}
    	}
    	return b;
    }
    
	public static long[] moebius(long[] a) {
		int n = Integer.numberOfTrailingZeros(a.length);
		long[] b = Arrays.copyOf(a, a.length);
		for (int i = 0; i < n; ++i) {
			for (int s = (1 << n) - 1; s >= 0; --s) {
				int ns = s | (1 << i);
				if (ns == s)
					continue;
				b[ns] -= b[s];
			}
		}
		return b;
	}
	
	
	public static long[] moebius(long[] a, long mod) {
		int n = Integer.numberOfTrailingZeros(a.length);
		long[] b = Arrays.copyOf(a, a.length);
		for (int i = 0; i < n; ++i) {
			for (int s = (1 << n) - 1; s >= 0; --s) {
				int ns = s | (1 << i);
				if (ns == s)
					continue;
				b[ns] += mod - b[s];
				if (b[ns] >= mod) b[ns] -= mod;
			}
		}
		return b;
	}

	/**
	 * 未テスト。
	 * {@code g[S] = Σ_(T ⊆ S) f[T]} に対し、
	 * {@code h[S] = Σ_(T ⊂ S) f[T] = g[S] - f[S]} を返す。
	 * 入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
	 *
	 * @param g subset zeta 変換済みの長さ {@code N} の配列
	 * @return strict subset zeta 変換後の配列
	 */
	public static long[] strictZetaFromZeta(long[] g) {
		long[] f = moebius(g);
		for (int i = 0; i < g.length; ++i) f[i] = g[i] - f[i];
		return f;
	}

	/**
	 * 未テスト。
	 * {@code g[S] = Σ_(T ⊆ S) f[T] (mod mod)} に対し、
	 * {@code h[S] = Σ_(T ⊂ S) f[T] = g[S] - f[S] (mod mod)} を返す。
	 * 入力の各要素は {@code [0, mod)} にあるものとし、入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
	 *
	 * @param g subset zeta 変換済みの長さ {@code N} の配列
	 * @param mod 法
	 * @return strict subset zeta 変換後の配列
	 */
	public static long[] strictZetaFromZeta(long[] g, long mod) {
		long[] f = moebius(g, mod);
		for (int i = 0; i < g.length; ++i) {
			f[i] = g[i] - f[i];
			if (f[i] < 0) f[i] += mod;
		}
		return f;
	}
	
	
	
	/***
	 * a(S) = sum[S ⊆ T] b(T)
	 * 
	 * @param a
	 * @return
	 */
	public static long[] supsetMoebius(long[] a) {
		long[] b = Arrays.copyOf(a, a.length);
		int n = Integer.numberOfTrailingZeros(a.length);
		for (int i = 0; i < n; ++i) {
			for (int s = 0; s < 1 << n; ++s) {
				int ns = s | (1 << i);
				if (ns == s)
					continue;
				b[s] -= b[ns];
			}
		}
		return b;
	}
	
	/***
	 * a(S) = sum[S ⊆ T] b(T)
	 * 
	 * @param a
	 * @param mod
	 * @return
	 */
	public static long[] supsetMoebius(long[] a, long mod) {
		//https://judge.yosupo.jp/submission/370183
		long[] b = Arrays.copyOf(a, a.length);
		int n = Integer.numberOfTrailingZeros(a.length);
		for (int i = 0; i < n; ++i) {
			for (int s = 0; s < 1 << n; ++s) {
				int ns = s | (1 << i);
				if (ns == s)
					continue;
				b[s] -= b[ns];
				b[s] %= mod;
				if (b[s] < 0)
					b[s] += mod;
			}
		}
		return b;
	}

	/**
	 * 未テスト。
	 * {@code g[S] = Σ_(S ⊆ T) f[T]} に対し、
	 * {@code h[S] = Σ_(S ⊂ T) f[T] = g[S] - f[S]} を返す。
	 * 入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
	 *
	 * @param g superset zeta 変換済みの長さ {@code N} の配列
	 * @return strict superset zeta 変換後の配列
	 */
	public static long[] supsetStrictZetaFromZeta(long[] g) {
		long[] f = supsetMoebius(g);
		for (int i = 0; i < g.length; ++i) f[i] = g[i] - f[i];
		return f;
	}

	/**
	 * 未テスト。
	 * {@code g[S] = Σ_(S ⊆ T) f[T] (mod mod)} に対し、
	 * {@code h[S] = Σ_(S ⊂ T) f[T] = g[S] - f[S] (mod mod)} を返す。
	 * 入力の各要素は {@code [0, mod)} にあるものとし、入力配列は変更しない。
	 *
	 * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
	 *
	 * @param g superset zeta 変換済みの長さ {@code N} の配列
	 * @param mod 法
	 * @return strict superset zeta 変換後の配列
	 */
	public static long[] supsetStrictZetaFromZeta(long[] g, long mod) {
		long[] f = supsetMoebius(g, mod);
		for (int i = 0; i < g.length; ++i) {
			f[i] = g[i] - f[i];
			if (f[i] < 0) f[i] += mod;
		}
		return f;
	}

	/***
	 * Σa[i]x^i = Σb[i](1+x)^i としたときの b を返す。 
	 * a = Σf(S)x^#S, b = Σg(S)x^#S, g(S) = Σ[S ⊆ T] f(T) としたときこの関係を満たす
	 * @param a
	 * @return
	 */
	public static long[] supsetRankedMoebius(long[] a) {
		long[] b = new long[a.length];
		for (int i = a.length - 1; i >= 0; --i) {
			for (int j = b.length - 1; j >= 0; --j) {
				b[j] = -b[j] + (j > 0 ? b[j - 1] : 0);
			}
			b[0] += a[i];
		}
		return b;
	}
	

	public static long[] rankedMoebius(long[] a) {
		long[] revA=a.clone();
		ArrayUtils.reverse(revA);
		revA=supsetRankedMoebius(revA);
		ArrayUtils.reverse(revA);
		return revA;
	}
	

	
	/**
	 * 未テスト
	 * Σa[i]x^i = Σb[i](1+x)^i としたときの b を返す。
	 * a = Σf(S)x^#S, b = Σg(S)x^#S, g(S) = Σ[S ⊆ T] f(T) としたときこの関係を満たす。
	 * PolynomialFpDynamic の taylorShift(a, mod - 1) を用いて計算する。
	 * 計算量: O(N log N) (NTT-friendly mod の場合) または O(N^2) (N = a.length)
	 *
	 * @param a 集合関数
	 * @param mod 法
	 * @return 変換後の配列
	 */
	public static long[] supsetRankedMoebius(long[] a, long mod) {
		PolynomialFpDynamic poly = PolynomialFpDynamic.of(mod);
		return poly.taylorShift(a, mod - 1);
	}
	
	
	/** Σa[i][j]x^iy^j = Σb[i](1+x)^i(1+y)^j としたときの b を返す。 
	 */
	public static long[][] supsetRankedMoebius(long[][] a, long mod) {
		//https://atcoder.jp/contests/abc242/submissions/74986770
		long[][] b = new long[a.length][a[0].length];
		Fp fp=new Fp(mod);
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				for (int k = 0; k <= i; k++) {
					for (int l = 0; l <= j; l++) {
						b[k][l]+=fp.mul(fp.comb(i, k), fp.comb(j, l), a[i][j], (i-k)%2==1?(mod-1):1, (j-l)%2==1?(mod-1):1);
						b[k][l]%=mod;
					}
				}
			}
		}
		return b;
	}
	
	
	/**
	 * 未テスト
	 * g(S) = Σ_(T ⊆ S) f(T) とする。
	 * {@code a[k] = Σ_(|S| = k) g(S)} が与えられたとき、
	 * このメソッドは
	 * {@code b[k] = Σ_(|S| = k) f(S)}
	 * を満たす配列 {@code b} を返す。
	 * supsetRankedMoebius を呼び出すことで、Taylor Shift を用いて計算する。
	 * 計算量: O(N log N) (NTT-friendly mod の場合) または O(N^2) (N = a.length)
	 *
	 * @param a {@code a[k] = Σ_(|S| = k) g(S)}
	 * @param mod 法
	 * @return ランク付き Möbius 逆変換後の配列
	 * @see #supsetRankedMoebius(long[], long)
	 */
	public static long[] rankedMoebius(long[] a, long mod) {
		long[] revA=a.clone();
		ArrayUtils.reverse(revA);
		revA=supsetRankedMoebius(revA, mod);
		ArrayUtils.reverse(revA);
		return revA;
	}
	
	
	public static long[][] rankedMoebius(long[][] a, long mod) {
		long[][]revA=ArrayUtils.copy(a);
		ArrayUtils.rotate180(revA);
		revA=supsetRankedMoebius(revA, mod);
		ArrayUtils.rotate180(revA);
		return revA;
	}
	
	
	/**
	 * a(S) = sum[S ⊆ T] b(T)について、sumをbitorで定義
	 * @return
	 */
	public static int[] supsetBitOrZeta(int[] a) {
		int[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]|=b[s];
    		}
    	}
    	return b;
	}
	
	/**
	 * a(S) = sum[S ⊆ T] b(T)について、sumをbitorで定義
	 * @return
	 */
	public static long[] supsetBitOrZeta(long[] a) {
		long[]b=Arrays.copyOf(a, a.length);
    	int N=MathUtils.floorLog2(a.length);
    	for (int i=0;i<N;++i) {
    		for(int s=0;s<1<<N;++s) {
    			if(Ints.bitAt(s, i)==1)continue;
    			b[s|(1<<i)]|=b[s];
    		}
    	}
    	return b;
	}
	
	/**
	 * a(S) = OR_(T ⊆ S) b(T) について、sum を bitwise OR で定義。
	 *
	 * @return subset bitwise-OR zeta transform
	 */
	public static long[] subsetBitOrZeta(long[] a) {
		long[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 0) continue;
				b[s] |= b[s ^ (1 << i)];
			}
		}
		return b;
	}
	
	/**
	 * b(S) = OR_(T ⊆ S) a(T) を計算する。
	 *
	 * @return subset bitwise-OR zeta transform
	 */
	public static int[] subsetBitOrZeta(int[] a) {
		int[] b = Arrays.copyOf(a, a.length);
		int N = MathUtils.floorLog2(a.length);
		for (int i = 0; i < N; ++i) {
			for (int s = 0; s < 1 << N; ++s) {
				if (Ints.bitAt(s, i) == 0) continue;
				b[s] |= b[s ^ (1 << i)];
			}
		}
		return b;
	}
	
	/**
	 * 未テスト
	 * y[B] = Σ_(B　∩　U=　∅　) s[U]x[B　∪　U]
	 * <p>O(N^2 2^N) （N = log_2(s.length)）</p>
	 * @param s 集合べき級数
	 * @param x 入力配列（転置写像への入力）
	 * @param mod 法
	 * @return 転置写像による像 y
	 */
	public static long[] transposedSubsetConvolution(long[] s, long[] x, long mod) {
		long[] revX = x.clone();
		ArrayUtils.reverse(revX);
		long[] conv = mul(revX, s, mod);
		ArrayUtils.reverse(conv);
		return conv;
	}

	/**
	 * 未テスト
	 * EGF 版の集合べき級数の Power Projection を計算する。
	 * 定数項が 0 である集合べき級数 s と、重み配列 w に対し、
	 * ∑_t w[t] [x^t] (s^k / k!) を k = 0, 1, ..., N について一括計算する。
	 *
	 * <p>計算量: O(N^2 2^N) （N = log_2(s.length)）</p>
	 *
	 * @param w 
	 * @param s 定数項が 0 の集合べき級数
	 * @param mod 法
	 * @return 評価結果の配列（長さ N + 1）
	 */
	public static long[] powerProjectionOfSpsEgf(long[] w, long[] s, long mod) {
		int N = MathUtils.floorLog2(s.length);
		if (s.length != (1 << N) || w.length != (1 << N)) {
			throw new IllegalArgumentException("Length of s and wt must be 2^N");
		}
		if (s[0] != 0) {
			throw new IllegalArgumentException("Constant term of s must be 0");
		}
		long[] y = new long[N + 1];
		y[0] = w[0];
		long[] dp = w.clone();
		for (int i = 0; i < N; i++) {
			//i個のブロックを確定済み
			//dp[T] = まだ選んでいない要素の集合がTであるような状態の重み
			long[] newdp = new long[1 << (N - 1 - i)];
			for (int j = 0; j < N - i; j++) {
				//jは今回確定するブロックの最大元
				//j+1,j+2,..,N-1はすでに確定済み
				long[] a = Arrays.copyOfRange(s, 1 << j, 2 << j);
				long[] b = Arrays.copyOfRange(dp, 1 << j, 2 << j);
				b = transposedSubsetConvolution(a, b, mod);
				for (int k = 0; k < b.length; k++) {
					newdp[k] = (newdp[k] + b[k]) % mod;
				}
			}
			dp = newdp;
			y[1 + i] = dp[0];
		}
		return y;
	}

	/**
	 * 未テスト
	 * 集合べき級数の Power Projection を計算する（多項式と集合べき級数の合成の転置）。
	 * 集合べき級数 s と、錘（weight）の配列 wt に対し、
	 * ∑_t wt[t] [x^t] s^k を k = 0, 1, ..., M - 1 について一括計算する。
	 *
	 * <p>計算量: O(N^2 2^N + M log M)（NTT-friendly mod の場合）または O(N^2 2^N + M^2) （N = log_2(s.length)）</p>
	 *
	 * @param wt 錘（weight）の配列
	 * @param s 集合べき級数
	 * @param M 計算する累乗数（k = 0, ..., M - 1）
	 * @param mod 法
	 * @return 評価結果の配列（長さ M）
	 */
	public static long[] powerProjectionOfSps(long[] wt, long[] s, int M, long mod) {
		int N = MathUtils.floorLog2(s.length);
		if (s.length != (1 << N) || wt.length != (1 << N)) {
			throw new IllegalArgumentException("Length of s and wt must be 2^N");
		}
		long c = s[0];
		long[] sPrime = s.clone();
		sPrime[0] = 0;
		// c を s の定数項として s' = s - c とする。
		// (c + s')^k / k! = comb(k, i) c^i (s')^{k-i) 
		// g[i] = c^i / i! とすると
		// (c + s')^k / k! = g[i] (s')^{k-i) / (k - i)! 
		long[] x = powerProjectionOfSpsEgf(wt, sPrime, mod);

		Fp fp = new Fp(mod);
		long[] g = new long[M];
		long pow = 1;
		for (int i = 0; i < M; i++) {
			g[i] = pow * fp.ifac(i) % mod;
			pow = pow * c % mod;
		}

		PolynomialFpDynamic poly = PolynomialFpDynamic.of(mod);
		long[] conv = poly.mul(x, g);

		long[] res = new long[M];
		for (int i = 0; i < M; i++) {
			long val = i < conv.length ? conv[i] : 0;
			res[i] = val * fp.fac(i) % mod;
		}
		return res;
	}

	/**
	 * 真偽値関数を {@code long} 配列にビットパックした表現に対して、
	 * superset OR ゼータ変換を行う。
	 *
	 * <p>
	 * 入力は、{@code B} 要素集合の部分集合上の真偽値関数
	 * {@code f : {0, ..., 2^B - 1} -> {false, true}} を表す。
	 * 部分集合 {@code S} に対する値 {@code f(S)} は、次の位置に格納される。
	 * </p>
	 *
	 * <pre>
	 * word = S >>> 6
	 * bit  = S & 63
	 * f(S) = ((a[word] >>> bit) & 1) != 0
	 * </pre>
	 *
	 * <p>
	 * {@code B < 6} の場合、配列の長さは {@code 1} であり、
	 * 下位 {@code 2^B} ビットのみが有効である。
	 * それより上位の未使用ビットは {@code 0} でなければならない。
	 * </p>
	 *
	 * <p>
	 * 返される配列は、各部分集合 {@code S} に対して
	 * </p>
	 *
	 * <pre>
	 * g(S) = OR { f(T) | S ⊆ T }
	 * </pre>
	 *
	 * <p>
	 * で定義される superset OR ゼータ変換 {@code g} を、
	 * 入力と同じビットパック形式で表したものである。
	 * 入力配列 {@code a} 自体は変更されない。
	 * </p>
	 *
	 * <p>
	 * 下位6次元は各 {@code long} 内のビット演算でまとめて処理し、
	 * それより上位の次元は {@code long} 配列上で通常の
	 * superset OR ゼータ変換を行う。
	 * </p>
	 *
	 * <p>
	 * 計算量は
	 * {@code O((B + 1) * 2^max(0, B - 6))} 時間、
	 * {@code O(2^max(0, B - 6))} 追加空間である。
	 * </p>
	 *
	 * @param a 真偽値関数をビットパックした配列。
	 *          長さは {@code max(1, 2^(B - 6))} でなければならない
	 * @param B 集合の要素数
	 * @return superset OR ゼータ変換後の真偽値関数を
	 *         ビットパックした新しい配列
	 * @throws IllegalArgumentException
	 *         {@code B < 0} の場合、
	 *         {@code a} の長さが不正な場合、
	 *         または {@code B < 6} で未使用ビットに {@code 1} が含まれる場合
	 */
	public static long[] supsetOrZetaPacked(long[] a, int B) {
		//https://yukicoder.me/submissions/1175027
		if (B < 0) {
			throw new IllegalArgumentException("B must be at least 0");
		}
		int expectedLength = B < 6 ? 1 : 1 << (B - 6);
		if (a.length != expectedLength) {
			throw new IllegalArgumentException("Array length must be " + expectedLength);
		}
		long[] b = a.clone();

		if (B < 6) {
			int numBits = 1 << B;
			if (numBits < 64) {
				long mask = ~((1L << numBits) - 1);
				if ((b[0] & mask) != 0) {
					throw new IllegalArgumentException("Out-of-bounds bits must be 0");
				}
			}
		}

		// Intra-word dimensions (0 to 5)
		// B の次元数まで OR ゼータ変換を施す。
		for (int s = 0; s < b.length; s++) {
			long val = b[s];

			// Dimension 0 (width = 1)
			if (B > 0) {
				long mask0 = 0b0101010101010101010101010101010101010101010101010101010101010101L;
				val |= (val >>> 1) & mask0;
			}

			// Dimension 1 (width = 2)
			if (B > 1) {
				long mask1 = 0b0011001100110011001100110011001100110011001100110011001100110011L;
				val |= (val >>> 2) & mask1;
			}

			// Dimension 2 (width = 4)
			if (B > 2) {
				long mask2 = 0b0000111100001111000011110000111100001111000011110000111100001111L;
				val |= (val >>> 4) & mask2;
			}

			// Dimension 3 (width = 8)
			if (B > 3) {
				long mask3 = 0b0000000011111111000000001111111100000000111111110000000011111111L;
				val |= (val >>> 8) & mask3;
			}

			// Dimension 4 (width = 16)
			if (B > 4) {
				long mask4 = 0b0000000000000000111111111111111100000000000000001111111111111111L;
				val |= (val >>> 16) & mask4;
			}

			// Dimension 5 (width = 32)
			if (B > 5) {
				long mask5 = 0b0000000000000000000000000000000011111111111111111111111111111111L;
				val |= (val >>> 32) & mask5;
			}

			b[s] = val;
		}

		// Inter-word dimensions (6 to B-1)
		if (B > 6) {
			int n = B - 6;
			for (int i = 0; i < n; i++) {
				int step = 1 << i;
				for (int s = 0; s < (1 << n); s++) {
					if ((s & step) == 0) {
						b[s] |= b[s | step];
					}
				}
			}
		}
		return b;
	}

	/**
	 * 真偽値関数を {@code long} 配列にビットパックした表現に対して、
	 * subset OR ゼータ変換を行う。
	 *
	 * <p>
	 * 入力は、{@code B} 要素集合の部分集合上の真偽値関数
	 * {@code f : {0, ..., 2^B - 1} -> {false, true}} を表す。
	 * 部分集合 {@code S} の値 {@code f(S)} は、次の位置に格納される。
	 * </p>
	 *
	 * <pre>
	 * word = S >>> 6
	 * bit  = S & 63
	 * f(S) = ((a[word] >>> bit) & 1) != 0
	 * </pre>
	 *
	 * <p>
	 * {@code B < 6} の場合、配列の長さは {@code 1} であり、
	 * 下位 {@code 2^B} ビットのみが有効である。
	 * それより上位の未使用ビットは {@code 0} でなければならない。
	 * </p>
	 *
	 * <p>
	 * 返される配列は、各部分集合 {@code S} に対して
	 * </p>
	 *
	 * <pre>
	 * g(S) = OR { f(T) | T ⊆ S }
	 * </pre>
	 *
	 * <p>
	 * で定義される subset OR ゼータ変換 {@code g} を、
	 * 入力と同じビットパック形式で表したものである。
	 * 入力配列 {@code a} 自体は変更されない。
	 * </p>
	 *
	 * <p>
	 * 下位6次元は各 {@code long} 内のビット演算でまとめて処理し、
	 * それより上位の次元は {@code long} 配列上で通常の
	 * subset OR ゼータ変換を行う。
	 * </p>
	 *
	 * <p>
	 * 計算量は
	 * {@code O((B + 1) * 2^max(0, B - 6))} 時間、
	 * {@code O(2^max(0, B - 6))} 追加空間である。
	 * </p>
	 *
	 * @param a 真偽値関数をビットパックした配列。
	 *          長さは {@code max(1, 2^(B - 6))} でなければならない
	 * @param B 集合の要素数
	 * @return subset OR ゼータ変換後の真偽値関数をビットパックした新しい配列
	 * @throws IllegalArgumentException
	 *         {@code B < 0} の場合、
	 *         {@code a} の長さが不正な場合、
	 *         または {@code B < 6} で未使用ビットに {@code 1} が含まれる場合
	 */
	public static long[] subsetOrZetaPacked(long[] a, int B) {
		if (B < 0) {
			throw new IllegalArgumentException("B must be at least 0");
		}
		int expectedLength = B < 6 ? 1 : 1 << (B - 6);
		if (a.length != expectedLength) {
			throw new IllegalArgumentException("Array length must be " + expectedLength);
		}
		long[] b = a.clone();

		if (B < 6) {
			int numBits = 1 << B;
			if (numBits < 64) {
				long mask = ~((1L << numBits) - 1);
				if ((b[0] & mask) != 0) {
					throw new IllegalArgumentException("Out-of-bounds bits must be 0");
				}
			}
		}

		// Intra-word dimensions (0 to 5)
		// B の次元数まで OR ゼータ変換を施す。
		for (int s = 0; s < b.length; s++) {
			long val = b[s];

			// Dimension 0 (width = 1)
			if (B > 0) {
				long mask0 = 0b1010101010101010101010101010101010101010101010101010101010101010L;
				val |= (val << 1) & mask0;
			}

			// Dimension 1 (width = 2)
			if (B > 1) {
				long mask1 = 0b1100110011001100110011001100110011001100110011001100110011001100L;
				val |= (val << 2) & mask1;
			}

			// Dimension 2 (width = 4)
			if (B > 2) {
				long mask2 = 0b1111000011110000111100001111000011110000111100001111000011110000L;
				val |= (val << 4) & mask2;
			}

			// Dimension 3 (width = 8)
			if (B > 3) {
				long mask3 = 0b1111111100000000111111110000000011111111000000001111111100000000L;
				val |= (val << 8) & mask3;
			}

			// Dimension 4 (width = 16)
			if (B > 4) {
				long mask4 = 0b1111111111111111000000000000000011111111111111110000000000000000L;
				val |= (val << 16) & mask4;
			}

			// Dimension 5 (width = 32)
			if (B > 5) {
				long mask5 = 0b1111111111111111111111111111111100000000000000000000000000000000L;
				val |= (val << 32) & mask5;
			}

			b[s] = val;
		}

		// Inter-word dimensions (6 to B-1)
		if (B > 6) {
			int n = B - 6;
			for (int i = 0; i < n; i++) {
				int step = 1 << i;
				for (int s = 0; s < (1 << n); s++) {
					if ((s & step) == 0) {
						b[s | step] |= b[s];
					}
				}
			}
		}
		return b;
	}

    static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
    
}
