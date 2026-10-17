package library.util.poset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.LongBinaryOperator;

import library.util.ArrayUtils;
import library.util.Fp;
import library.util.Itertools;
import library.util.MathUtils;
import library.util.Sieve;
import library.util.collections.ArrayOnQuotient;
import library.util.collections.IntArrayList;
import library.util.collections.LongArrayList;
import library.util.seq.SortedArrays;

public class DivisorLattice {
	
    
	/**
	 * 約数束 (divisor lattice) 上の関数を，
	 * その部分順序集合に制限した上での Möbius 変換を計算する。
	 *
	 * <p>{@code divs} を割り切り関係で順序付けられた集合とみなし，
	 * {@code a[i]} を {@code divs.get(i)} における関数値とする。
	 * このメソッドは，各 {@code x = divs.get(i)} に対して
	 *
	 * <pre>{@code
	 * a[i] = Σ f(d)  （d | x, d ∈ divs）
	 * }</pre>
	 *
	 * のように約数ゼータ変換された値 {@code a} から，元の関数 {@code f} を復元する。
	 *
	 * <p>すなわち，約数束におけるゼータ変換
	 *
	 * <pre>{@code
	 * g(x) = Σ f(d)  （d | x）
	 * }</pre>
	 *
	 * の逆変換を，整数全体ではなく {@code divs} が張る部分順序集合上で行う。
	 *
	 * <p>実装は，各素数 {@code p} による被覆関係
	 * {@code d → d * p} に沿って，ゼータ変換で加算された寄与を逆向きに打ち消すことで
	 * 元の値を復元している。
	 * ゼータ変換では小さい約数側から大きい倍数側へ値を伝播させるのに対し，
	 * このメソッドでは逆変換のために，添字を大きい側から小さい側へ逆順に走査する。
	 *
	 * <h2>前提条件</h2>
	 * <ul>
	 *   <li>{@code divs} は昇順にソートされていること</li>
	 *   <li>{@code a.length == divs.size()} であること</li>
	 *   <li>divs 上に張った x -> x*p 辺によるゼータ変換の逆変換</li>
	 * </ul>
	 *
	 * <h2>計算量</h2>
	 *  O(|divs| × |primes|)。
	 *
	 * <h2>備考</h2>
	 * このメソッドは {@link #zetaOnSubposet(long[], ArrayList, ArrayList, long)} の逆変換になっており，
	 * 適切な入力に対して
	 *
	 * <pre>{@code
	 * moebius(zeta(f, divs, primes, mod), divs, primes, mod)
	 * }</pre>
	 *
	 * は {@code f} を復元する。
	 *
	 * @param a      約数ゼータ変換後の関数値配列
	 * @param divs   約数集合（割り切り順序で扱う部分集合）
	 * @param primes 被覆関係を生成するための素数集合
	 * @param mod    加算・減算に用いる法
	 * @return Möbius 変換後の配列（元の関数値）
	 */
	public static long[] moebiusOnSubposet(long[] a, IntArrayList divs, IntArrayList primes, long mod) {
    	//https://atcoder.jp/contests/abc349/submissions/71431698
    	long[]b=Arrays.copyOf(a, a.length);
    	for (long p:primes) {
    		for (int i = divs.size() - 1; i >= 0; i--) {
    			if(divs.get(i)<=divs.get(divs.size()-1)/p) {
    				long nd=divs.get(i)*p;
    				int id=SortedArrays.floor(divs, (int)nd);
    				if(divs.get(id)!=nd)continue;
    				b[id]=(b[id]+mod-b[i])%mod;
    			}
			}
    	}
    	return b;
    }
	
	public static long[] moebiusOnSuposet(long[] a, IntArrayList divs, IntArrayList primes, long mod) {
    	//https://atcoder.jp/contests/abc349/submissions/71431698
    	long[]b=Arrays.copyOf(a, a.length);
    	for (long p:primes) {
    		for (int i = divs.size() - 1; i >= 0; i--) {
    			if(divs.get(i)<=divs.get(divs.size()-1)/p) {
    				long nd=divs.get(i)*p;
    				int id=SortedArrays.floor(divs, (int)nd);
    				if(divs.get(id)!=nd)continue;
    				b[id]=(b[id]+mod-b[i])%mod;
    			}
			}
    	}
    	return b;
    }

    
    /**
     * 約数束 (divisor lattice) 上の関数を，
     * その部分順序集合（特に下に閉じた部分集合）に制限した上でのゼータ変換を計算する。
     *
     * <p>{@code divs} を割り切り関係で順序付けられた集合とみなし，
     * {@code a[i]} を {@code divs.get(i)} における関数値とする。
     * このとき，各 {@code x = divs.get(i)} に対して
     *
     * <pre>{@code
     * b[i] = Σ a[j]  （divs.get(j) が x を割り切る）
     * }</pre>
     *
     * を満たす配列 {@code b} を返す。
     *
     * <p>すなわち，約数束におけるゼータ変換
     *
     * <pre>{@code
     * g(x) = Σ f(d)  （d | x）
     * }</pre>
     *
     * を，整数全体ではなく {@code divs} が張る部分順序集合上で計算する。
     *
     * <p>実装は，各素数 {@code p} による被覆関係
     * {@code d → d * p} を用いて値を上方へ伝播させることで，
     * 約数関係に沿った累積和を構成している。
     *
     * <h2>前提条件</h2>
     * <ul>
     *   <li>{@code divs} は昇順にソートされていること</li>
     *   <li>{@code a.length == divs.size()} であること</li>
     * </ul>
     *
     * <h2>計算量</h2>
     * O(|divs| × |primes|)。
     * @param a      関数値配列（{@code a[i]} は {@code divs.get(i)} に対応）
     * @param divs   約数集合（割り切り順序で扱う部分集合）
     * @param primes 被覆関係を生成するための素数集合
     * @param mod    加算に用いる法
     * @return 約数ゼータ変換後の配列
     */
    public static long[] zetaOnSubposet(long[] a, LongArrayList divs, LongArrayList primes, long mod) {
    	//https://atcoder.jp/contests/abc349/submissions/71431698
    	long[]b=Arrays.copyOf(a, a.length);
    	for (long p:primes) {
    		for (int i = 0; i < divs.size(); i++) {
    			if(divs.get(i)<=divs.get(divs.size()-1)/p) {
    				long nd=divs.get(i)*p;
				int id=SortedArrays.floor(divs, nd);
    				if(divs.get(id)!=nd)continue;
    				b[id]=(b[id]+b[i])%mod;
    			}
			}
    	}
    	return b;
    }

    /**
     * 約数束上の zeta 変換を、任意の二項演算で行う。
     * 更新は {@code b[id] = op.applyAsLong(b[id], b[i])} の順で適用される。
     * @param a 各約数に対応する値の配列
     * @param divs 昇順に並んだ約数列
     * @param primes 最大元の相異なる素因数列
     * @param op 伝播先の値と伝播元の値をマージする演算
     * @return zeta 変換後の配列
     */
    public static long[] zeta(long[] a, ArrayList<Long> divs, ArrayList<Long> primes, LongBinaryOperator op) {
    	//https://atcoder.jp/contests/abc349/submissions/71431698
    	long[]b=Arrays.copyOf(a, a.length);
    	for (long p:primes) {
    		for (int i = 0; i < divs.size(); i++) {
    			if(divs.get(i)<=divs.get(divs.size()-1)/p) {
    				long nd=divs.get(i)*p;
				int id=SortedArrays.floor(divs, nd);
    				if(divs.get(id)!=nd)continue;
    				b[id]=op.applyAsLong(b[id], b[i]);
    			}
			}
    	}
    	return b;
    }
    
    
    /**
     * 未検証！！！
     * @param a
     * @return
     */
    public static long[] moebius(long[] a) {
    	long[]b=Arrays.copyOf(a, a.length);
        for (int i = 1; i < a.length; ++i) {
            for (int j = 2*i; j < a.length; j+=i) {
                b[j] -= b[i];
            }
        }
    	return b;
    }
    
    /**
     * @param a
     * @return
     */
    public static long[] moebius(long[] a, long mod) {
    	long[]b=Arrays.copyOf(a, a.length);
        for (int i = 1; i < a.length; ++i) {
            for (int j = 2*i; j < a.length; j+=i) {
                b[j] -= b[i];
                b[j] = (b[j] + mod) % mod;
            }
        }
    	return b;
    }

    /**
     * 未テスト。
     * {@code g[x] = Σ_(d | x) f[d]} に対し、
     * {@code h[x] = Σ_(d | x, d != x) f[d] = g[x] - f[x]} を返す。
     * 入力配列は変更しない。
     *
     * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
     *
     * @param g divisor zeta 変換済みの長さ {@code N} の配列
     * @return strict divisor zeta 変換後の配列
     */
    public static long[] strictZetaFromZeta(long[] g) {
        long[] f = moebius(g);
        for (int i = 0; i < g.length; ++i) f[i] = g[i] - f[i];
        return f;
    }

    /**
     * 未テスト。
     * {@code g[x] = Σ_(d | x) f[d] (mod mod)} に対し、
     * {@code h[x] = Σ_(d | x, d != x) f[d] = g[x] - f[x] (mod mod)} を返す。
     * 入力の各要素は {@code [0, mod)} にあるものとし、入力配列は変更しない。
     *
     * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
     *
     * @param g divisor zeta 変換済みの長さ {@code N} の配列
     * @param mod 法
     * @return strict divisor zeta 変換後の配列
     */
    public static long[] strictZetaFromZeta(long[] g, long mod) {
        long[] f = moebius(g, mod);
        for (int i = 0; i < g.length; ++i) {
            f[i] = g[i] - f[i];
            if (f[i] < 0) f[i] += mod;
        }
        return f;
    }
    
    /**
     * @param a
     * @param mod
     * @return
     */
    public static long[] supsetMoebius(long[] a, long mod) {
    	//https://atcoder.jp/contests/abc248/submissions/72061542
    	long[]b=Arrays.copyOf(a, a.length);
    	Sieve.expandPrimes(a.length+1);
    	for (int i=2;i<a.length;++i) {
    		if(Sieve.isPrime(i)) {
    			for (int j = 1; i * j < a.length; j ++) {
					b[j] += mod - b[i * j];
					if (b[j] >= mod) b[j] -= mod;
    			}
    		}
    	}
    	return b;
    }
    
    
	/**
	 * a[a.length-1]は最大元として扱う。
	 * @param a
	 * @return
	 */
    public static long[] supsetMoebius(long[] a) {
    	//verified:https://atcoder.jp/contests/abc361/submissions/70345263
    	Sieve.expandPrimes(a.length);
    	long[] b=Arrays.copyOf(a, a.length - 1);
    	for (int i = 1; i < b.length; i++) {
			b[i] -= a[a.length - 1];
		}
    	for (int i = 2; i < b.length; i++) {
    		if(Sieve.isPrime(i)) {
				for (int j = 1; j <= (b.length-1)/i; j++) {
					b[j] -= b[i * j];
				}
			}
		}
    	return ArrayUtils.concat(b, new long[] {a[a.length - 1]});
    }

    /**
     * 未テスト。
     * {@link #supsetMoebius(long[])} が定める順序で
     * {@code g[x] = Σ_(x <= m) f[m]} に対し、
     * {@code h[x] = Σ_(x < m) f[m] = g[x] - f[x]} を返す。
     * 入力配列は変更しない。
     *
     * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
     *
     * @param g multiple zeta 変換済みの長さ {@code N} の配列
     * @return strict multiple zeta 変換後の配列
     */
    public static long[] supsetStrictZetaFromZeta(long[] g) {
        long[] f = supsetMoebius(g);
        for (int i = 0; i < g.length; ++i) f[i] = g[i] - f[i];
        return f;
    }

    /**
     * 未テスト。
     * {@code g[x] = Σ_(x | m) f[m] (mod mod)} に対し、
     * {@code h[x] = Σ_(x | m, x != m) f[m] = g[x] - f[x] (mod mod)} を返す。
     * 入力の各要素は {@code [0, mod)} にあるものとし、入力配列は変更しない。
     *
     * <p>計算量: {@code O(N log N)}、追加領域: {@code O(N)}。
     *
     * @param g multiple zeta 変換済みの長さ {@code N} の配列
     * @param mod 法
     * @return strict multiple zeta 変換後の配列
     */
    public static long[] supsetStrictZetaFromZeta(long[] g, long mod) {
        long[] f = supsetMoebius(g, mod);
        for (int i = 0; i < g.length; ++i) {
            f[i] = g[i] - f[i];
            if (f[i] < 0) f[i] += mod;
        }
        return f;
    }
    

	/**
	 * @param a
	 * @return
	 */
    public static long[] supsetZeta(long[] a) {
    	//https://atcoder.jp/contests/abc393/submissions/75161322
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = 1; i < a.length; ++i) {
        	for (int j = 2*i; j < a.length; j+=i) {
				b[i]+=b[j];
			}
        }
    	return b;
    }

    public static long[] supsetZeta(long[] a, LongBinaryOperator op) {
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = 1; i < a.length; ++i) {
        	for (int j = 2*i; j < a.length; j+=i) {
        		b[i]=op.applyAsLong(b[i], b[j]);
			}
        }
    	return b;
    }

    public static long[] zeta(long[] a) {
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = a.length-1; i >=1; --i) {
        	for (int j = 2*i; j < a.length; j+=i) {
				b[j]+=b[i];
			}
        }
    	return b;
    }

    public static long[] zeta(long[] a, LongBinaryOperator op) {
    	//https://atcoder.jp/contests/abc393/submissions/75161322
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = a.length-1; i >=1; --i) {
        	for (int j = 2*i; j < a.length; j+=i) {
        		b[j]=op.applyAsLong(b[i], b[j]);
			}
        }
    	return b;
    }
    
    /**
     * 未テスト
     * c[k] = Σ[k = gcd(i, j)] a[i]b[j]
     * @param a
     * @param b
     * @return
     */
    public static long[] gcdConvolution(long[] a, long[] b) {
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	if (a.length != b.length) throw new AssertionError();
    	int n = a.length;
    	long[] a2 = supsetZeta(a);
    	long[] b2 = supsetZeta(b);
    	for (int i = 1; i < n; i++) {
			a2[i] *= b2[i];
		}
    	Sieve.expandPrimes(n + 1);
    	for (int i = 2; i < n; ++i) {
    		if(Sieve.isPrime(i)) {
    			for (int j = 1; i * j < n; j ++) {
					a2[j] -= a2[i * j];
    			}
    		}
    	}
    	return a2;
    }

	/**
	 * 未テスト
	 *
	 * 添字の順序関係 i <= j を考慮した最大公約数畳み込みを計算します。
	 *
	 * 長さが等しい配列 a, b（ただし a[0] = b[0] = 0）に対し、
	 * 各 k について以下の値を満たす配列 c を返します。
	 * c[k] = sum_(k = gcd(i, j), i <= j) a[i] * b[j]
	 *
	 * 計算量: O(N log N)
	 *
	 * @param a 長さ N の配列（a[0] = 0）
	 * @param b 長さ N の配列（b[0] = 0）
	 * @return 畳み込み結果の配列 c
	 */
	public static long[] gcdConvolutionOrdered(long[] a, long[] b) {
		if (a[0] != 0 || b[0] != 0) throw new AssertionError();
		if (a.length != b.length) throw new AssertionError();
		int n = a.length;
		int[] A = new int[n];
		int[] B = new int[n];
		for (int i = 0; i < n; i++) {
			A[i] = i == 0 ? 1 : i;
			B[i] = i == 0 ? 1 : i;
		}
		return gcdConvolutionOrdered(A, B, a, b);
	}

	/**
	 * 未テスト
	 *
	 * 添字の順序関係 i <= j を考慮した法 mod における最大公約数畳み込みを計算します。
	 *
	 * 長さが等しい配列 a, b（ただし a[0] = b[0] = 0）に対し、
	 * 各 k について以下の値を満たす配列 c を返します。
	 * c[k] = (sum_(k = gcd(i, j), i <= j) a[i] * b[j]) mod mod
	 *
	 * 計算量: O(N log N)
	 *
	 * @param a   長さ N の配列（a[0] = 0）
	 * @param b   長さ N の配列（b[0] = 0）
	 * @param mod 法
	 * @return 畳み込み結果 of 配列 c
	 */
	public static long[] gcdConvolutionOrdered(long[] a, long[] b, long mod) {
		if (a[0] != 0 || b[0] != 0) throw new AssertionError();
		if (a.length != b.length) throw new AssertionError();
		int n = a.length;
		int[] A = new int[n];
		int[] B = new int[n];
		for (int i = 0; i < n; i++) {
			A[i] = i == 0 ? 1 : i;
			B[i] = i == 0 ? 1 : i;
		}
		return gcdConvolutionOrdered(A, B, a, b, mod);
	}

	/**
	 * 未テスト
	 *
	 * 添字の順序関係 i <= j を考慮した最大公約数畳み込みの転置を計算します。
	 *
	 * 長さが等しい配列 f, h（ただし f[0] = h[0] = 0）に対し、
	 * 各 j について以下の値を満たす配列 d を返します。
	 * d[j] = sum_(i <= j) f[i] * h[gcd(i, j)]
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f 長さ N の配列（f[0] = 0）
	 * @param h 長さ N の配列（h[0] = 0）
	 * @return 転置畳み込み結果の配列 d
	 */
	public static long[] gcdConvolutionOrderedTranspose(long[] f, long[] h) {
		if (f[0] != 0 || h[0] != 0) throw new AssertionError();
		if (f.length != h.length) throw new AssertionError();
		int n = f.length;
		int[] A = new int[n];
		int[] B = new int[n];
		for (int i = 0; i < n; i++) {
			A[i] = i == 0 ? 1 : i;
			B[i] = i == 0 ? 1 : i;
		}
		return gcdConvolutionOrderedTranspose(A, B, f, h);
	}

	/**
	 * 未テスト
	 *
	 * 添字の順序関係 i <= j を考慮した法 mod における最大公約数畳み込みの転置を計算します。
	 *
	 * 長さが等しい配列 f, h（ただし f[0] = h[0] = 0）に対し、
	 * 各 j について以下の値を満たす配列 d を返します。
	 * d[j] = (sum_(i <= j) f[i] * h[gcd(i, j)]) mod mod
	 *
	 * 計算量: O(N log N)
	 *
	 * @param f   長さ N の配列（f[0] = 0）
	 * @param h   長さ N の配列（h[0] = 0）
	 * @param mod 法
	 * @return 転置畳み込み結果 of 配列 d
	 */
	public static long[] gcdConvolutionOrderedTranspose(long[] f, long[] h, long mod) {
		if (f[0] != 0 || h[0] != 0) throw new AssertionError();
		if (f.length != h.length) throw new AssertionError();
		int n = f.length;
		int[] A = new int[n];
		int[] B = new int[n];
		for (int i = 0; i < n; i++) {
			A[i] = i == 0 ? 1 : i;
			B[i] = i == 0 ? 1 : i;
		}
		return gcdConvolutionOrderedTranspose(A, B, f, h, mod);
	}

	/**
	 * 未テスト
	 *
	 * 配列 A, B に対し、添字の順序関係 i <= j を考慮した一般化最大公約数畳み込みを計算します。
	 *
	 * 各 k について以下の値を満たす配列 C を返します。
	 * C[k] = sum_(i <= j, gcd(A[i], B[j]) = k) f[i] * g[j]
	 *
	 * 計算量: O((N + V) log V)
	 *
	 * @param A 配列 A
	 * @param B 配列 B
	 * @param f 配列 f
	 * @param g 配列 g
	 * @return 畳み込み結果の配列 C
	 */
	public static long[] gcdConvolutionOrdered(int[] A, int[] B, long[] f, long[] g) {
		if (A.length != B.length || A.length != f.length || A.length != g.length) {
			throw new AssertionError();
		}
		int n = A.length;
		int V = 0;
		for (int x : A) V = Math.max(V, x);
		for (int x : B) V = Math.max(V, x);

		int[] countA = new int[V + 1];
		int[] countB = new int[V + 1];
		for (int i = 0; i < n; i++) {
			int valA = A[i];
			if (valA > 0) {
				for (int d = 1; d * d <= valA; d++) {
					if (valA % d == 0) {
						countA[d]++;
						if (d * d != valA) countA[valA / d]++;
					}
				}
			}
			int valB = B[i];
			if (valB > 0) {
				for (int d = 1; d * d <= valB; d++) {
					if (valB % d == 0) {
						countB[d]++;
						if (d * d != valB) countB[valB / d]++;
					}
				}
			}
		}

		int[] offsetA = new int[V + 2];
		int[] offsetB = new int[V + 2];
		for (int d = 1; d <= V; d++) {
			offsetA[d + 1] = offsetA[d] + countA[d];
			offsetB[d + 1] = offsetB[d] + countB[d];
		}
		int[] listA = new int[offsetA[V + 1]];
		int[] listB = new int[offsetB[V + 1]];
		int[] curA = offsetA.clone();
		int[] curB = offsetB.clone();
		for (int i = 0; i < n; i++) {
			int valA = A[i];
			if (valA > 0) {
				for (int d = 1; d * d <= valA; d++) {
					if (valA % d == 0) {
						listA[curA[d]++] = i;
						if (d * d != valA) listA[curA[valA / d]++] = i;
					}
				}
			}
			int valB = B[i];
			if (valB > 0) {
				for (int d = 1; d * d <= valB; d++) {
					if (valB % d == 0) {
						listB[curB[d]++] = i;
						if (d * d != valB) listB[curB[valB / d]++] = i;
					}
				}
			}
		}

		long[] S = new long[V + 1];
		for (int d = 1; d <= V; d++) {
			int ptrA = offsetA[d];
			int endA = offsetA[d + 1];
			int ptrB = offsetB[d];
			int endB = offsetB[d + 1];
			long sumF = 0;
			long sum = 0;
			while (ptrB < endB) {
				int j = listB[ptrB];
				while (ptrA < endA && listA[ptrA] <= j) {
					sumF += f[listA[ptrA]];
					ptrA++;
				}
				sum += sumF * g[j];
				ptrB++;
			}
			S[d] = sum;
		}

		Sieve.expandPrimes(V + 1);
		for (int p = 2; p <= V; p++) {
			if (Sieve.isPrime(p)) {
				for (int j = 1; p * j <= V; j++) {
					S[j] -= S[p * j];
				}
			}
		}

		return S;
	}

	/**
	 * 未テスト
	 *
	 * 配列 A, B に対し、法 mod における添字の順序関係 i <= j を考慮した一般化最大公約数畳み込みを計算します。
	 *
	 * 各 k について以下の値を満たす配列 C を返します。
	 * C[k] = (sum_(i <= j, gcd(A[i], B[j]) = k) f[i] * g[j]) mod mod
	 *
	 * 計算量: O((N + V) log V)
	 *
	 * @param A   配列 A
	 * @param B   配列 B
	 * @param f   配列 f
	 * @param g   配列 g
	 * @param mod 法
	 * @return 畳み込み結果の配列 C
	 */
	public static long[] gcdConvolutionOrdered(int[] A, int[] B, long[] f, long[] g, long mod) {
		if (A.length != B.length || A.length != f.length || A.length != g.length) {
			throw new AssertionError();
		}
		int n = A.length;
		int V = 0;
		for (int x : A) V = Math.max(V, x);
		for (int x : B) V = Math.max(V, x);

		int[] countA = new int[V + 1];
		int[] countB = new int[V + 1];
		for (int i = 0; i < n; i++) {
			int valA = A[i];
			if (valA > 0) {
				for (int d = 1; d * d <= valA; d++) {
					if (valA % d == 0) {
						countA[d]++;
						if (d * d != valA) countA[valA / d]++;
					}
				}
			}
			int valB = B[i];
			if (valB > 0) {
				for (int d = 1; d * d <= valB; d++) {
					if (valB % d == 0) {
						countB[d]++;
						if (d * d != valB) countB[valB / d]++;
					}
				}
			}
		}

		int[] offsetA = new int[V + 2];
		int[] offsetB = new int[V + 2];
		for (int d = 1; d <= V; d++) {
			offsetA[d + 1] = offsetA[d] + countA[d];
			offsetB[d + 1] = offsetB[d] + countB[d];
		}
		int[] listA = new int[offsetA[V + 1]];
		int[] listB = new int[offsetB[V + 1]];
		int[] curA = offsetA.clone();
		int[] curB = offsetB.clone();
		for (int i = 0; i < n; i++) {
			int valA = A[i];
			if (valA > 0) {
				for (int d = 1; d * d <= valA; d++) {
					if (valA % d == 0) {
						listA[curA[d]++] = i;
						if (d * d != valA) listA[curA[valA / d]++] = i;
					}
				}
			}
			int valB = B[i];
			if (valB > 0) {
				for (int d = 1; d * d <= valB; d++) {
					if (valB % d == 0) {
						listB[curB[d]++] = i;
						if (d * d != valB) listB[curB[valB / d]++] = i;
					}
				}
			}
		}

		long[] S = new long[V + 1];
		for (int d = 1; d <= V; d++) {
			int ptrA = offsetA[d];
			int endA = offsetA[d + 1];
			int ptrB = offsetB[d];
			int endB = offsetB[d + 1];
			long sumF = 0;
			long sum = 0;
			while (ptrB < endB) {
				int j = listB[ptrB];
				while (ptrA < endA && listA[ptrA] <= j) {
					sumF = (sumF + (f[listA[ptrA]] % mod + mod)) % mod;
					ptrA++;
				}
				long gj = (g[j] % mod + mod) % mod;
				sum = (sum + sumF * gj) % mod;
				ptrB++;
			}
			S[d] = sum;
		}

		Sieve.expandPrimes(V + 1);
		for (int p = 2; p <= V; p++) {
			if (Sieve.isPrime(p)) {
				for (int j = 1; p * j <= V; j++) {
					S[j] = (S[j] - S[p * j]) % mod;
					if (S[j] < 0) {
						S[j] += mod;
					}
				}
			}
		}

		return S;
	}

	/**
	 * 未テスト
	 *
	 * 配列 A, B に対し、添字の順序関係 i <= j を考慮した一般化最大公約数畳み込みの転置を計算します。
	 *
	 * 各 j について以下の値を満たす配列 D を返します。
	 * D[j] = sum_(i <= j) f[i] * h[gcd(A[i], B[j])]
	 *
	 * 計算量: O((N + V) log V)
	 *
	 * @param A 配列 A
	 * @param B 配列 B
	 * @param f 配列 f
	 * @param h 配列 h
	 * @return 転置畳み込み結果の配列 D
	 */
	public static long[] gcdConvolutionOrderedTranspose(int[] A, int[] B, long[] f, long[] h) {
		if (A.length != B.length || A.length != f.length) {
			throw new AssertionError();
		}
		int n = A.length;
		int V = 0;
		for (int x : A) V = Math.max(V, x);
		for (int x : B) V = Math.max(V, x);
		if (h.length < V + 1) {
			throw new AssertionError();
		}

		DivisorsCSR.expand(V);

		/*
		 * coeff は divisor Möbius inversion:
		 *
		 * h[x] = sum_{d | x} coeff[d].
		 *
		 * divisorList は昇順なので、
		 * x 自身は区間の最後にある。
		 */
		long[] coeff = Arrays.copyOf(h, V + 1);

		for (int x = 1; x <= V; x++) {
			int l = DivisorsCSR.offset[x];
			int r = DivisorsCSR.offset[x + 1];

			// 最後の要素 x 自身を除く
			for (int k = l; k + 1 < r; k++) {
				coeff[x] -= coeff[DivisorsCSR.list[k]];
			}
		}

		/*
		 * sum[d]
		 * = sum_{i <= 現在位置, d | A[i]} f[i]
		 */
		long[] sum = new long[V + 1];
		long[] D = new long[n];

		for (int j = 0; j < n; j++) {
			int a = A[j];
			int b = B[j];
			long fj = f[j];

			/*
			 * A[j] == B[j] なら、
			 * update と query を同じ divisor loop で処理できる。
			 *
			 * 今回の gcdConvolutionOrderedTranspose(A, A, ...)
			 * は常にこちらに入る。
			 */
			if (a > 0 && a == b) {
				long ret = 0;

				int l = DivisorsCSR.offset[a];
				int r = DivisorsCSR.offset[a + 1];

				for (int k = l; k < r; k++) {
					int d = DivisorsCSR.list[k];

					long s = sum[d] + fj;
					sum[d] = s;

					ret += s * coeff[d];
				}

				D[j] = ret;
				continue;
			}

			/*
			 * 先に A[j] を登録することで i <= j。
			 */
			if (a > 0) {
				int l = DivisorsCSR.offset[a];
				int r = DivisorsCSR.offset[a + 1];

				for (int k = l; k < r; k++) {
					sum[DivisorsCSR.list[k]] += fj;
				}
			}

			if (b > 0) {
				long ret = 0;

				int l = DivisorsCSR.offset[b];
				int r = DivisorsCSR.offset[b + 1];

				for (int k = l; k < r; k++) {
					int d = DivisorsCSR.list[k];
					ret += sum[d] * coeff[d];
				}

				D[j] = ret;
			}
		}

		return D;
	}

	/**
	 * 未テスト
	 *
	 * 配列 A, B に対し、法 mod における添字の順序関係 i <= j を考慮した一般化最大公約数畳み込みの転置を計算します。
	 *
	 * 各 j について以下の値を満たす配列 D を返します。
	 * D[j] = (sum_(i <= j) f[i] * h[gcd(A[i], B[j])]) mod mod
	 *
	 * 計算量: O((N + V) log V)
	 *
	 * @param A   配列 A
	 * @param B   配列 B
	 * @param f   配列 f
	 * @param h   配列 h
	 * @param mod 法
	 * @return 転置畳み込み結果の配列 D
	 */
	public static long[] gcdConvolutionOrderedTranspose(int[] A, int[] B, long[] f, long[] h, long mod) {
		//https://atcoder.jp/contests/arc185/submissions/78182835
		if (A.length != B.length || A.length != f.length) {
			throw new AssertionError();
		}
		int n = A.length;
		int V = 0;
		for (int x : A) V = Math.max(V, x);
		for (int x : B) V = Math.max(V, x);
		if (h.length < V + 1) {
			throw new AssertionError();
		}

		DivisorsCSR.expand(V);

		long[] coeff = new long[V + 1];
		for (int i = 0; i <= V; i++) {
			coeff[i] = (h[i] % mod + mod) % mod;
		}

		for (int x = 1; x <= V; x++) {
			int l = DivisorsCSR.offset[x];
			int r = DivisorsCSR.offset[x + 1];

			for (int k = l; k + 1 < r; k++) {
				coeff[x] -= coeff[DivisorsCSR.list[k]];
			}
			coeff[x] %= mod;
			if (coeff[x] < 0) {
				coeff[x] += mod;
			}
		}

		long[] sum = new long[V + 1];
		long[] D = new long[n];

		for (int j = 0; j < n; j++) {
			int a = A[j];
			int b = B[j];
			long fj = (f[j] % mod + mod) % mod;

			if (a > 0 && a == b) {
				long ret = 0;

				int l = DivisorsCSR.offset[a];
				int r = DivisorsCSR.offset[a + 1];

				for (int k = l; k < r; k++) {
					int d = DivisorsCSR.list[k];

					long s = sum[d] + fj;
					if (s >= mod) s -= mod;
					sum[d] = s;

					ret = (ret + s * coeff[d]) % mod;
				}

				D[j] = ret;
				continue;
			}

			if (a > 0) {
				int l = DivisorsCSR.offset[a];
				int r = DivisorsCSR.offset[a + 1];

				for (int k = l; k < r; k++) {
					int d = DivisorsCSR.list[k];
					long s = sum[d] + fj;
					if (s >= mod) s -= mod;
					sum[d] = s;
				}
			}

			if (b > 0) {
				long ret = 0;

				int l = DivisorsCSR.offset[b];
				int r = DivisorsCSR.offset[b + 1];

				for (int k = l; k < r; k++) {
					int d = DivisorsCSR.list[k];
					ret = (ret + sum[d] * coeff[d]) % mod;
				}

				D[j] = ret;
			}
		}

		return D;
	}
    
    /**
     * 未テスト
     * c[k] = Σ[k = gcd(i, j)] a[i]b[j]
     * @param a
     * @param b
     * @param mod
     * @return
     */
    public static long[] gcdConvolution(long[] a, long[] b, long mod) {
    	//https://judge.yosupo.jp/submission/370196
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	if (a.length != b.length) throw new AssertionError();
    	int n = a.length;
    	long[] a2 = supsetZeta(a, (x, y) -> (x + y) % mod);
    	long[] b2 = supsetZeta(b, (x, y) -> (x + y) % mod);
    	for (int i = 1; i < n; i++) {
			a2[i] = a2[i] * b2[i] % mod;
		}
    	return supsetMoebius(a2, mod);
    }
    
    /**
     * 未テスト
     * c[k] = Σ[k = lcm(i, j)] a[i]b[j]
     * @param a
     * @param b
     * @return
     */
    public static long[] lcmConvolution(long[] a, long[] b) {
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	if (a.length != b.length) throw new AssertionError();
    	int n = a.length;
    	long[] a2 = zeta(a);
    	long[] b2 = zeta(b);
    	for (int i = 1; i < n; i++) {
			a2[i] *= b2[i];
		}
    	return moebius(a2);
    }
    
    /**
     * c[k] = Σ[k = lcm(i, j)] a[i]b[j]
     * @param a
     * @param b
     * @param mod
     * @return
     */
    public static long[] lcmConvolution(long[] a, long[] b, long mod) {
    	//https://judge.yosupo.jp/submission/370202
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	if (a.length != b.length) throw new AssertionError();
    	int n = a.length;
    	long[] a2 = zeta(a, (x, y) -> (x + y) % mod);
    	long[] b2 = zeta(b, (x, y) -> (x + y) % mod);
    	for (int i = 1; i < n; i++) {
			a2[i] = a2[i] * b2[i] % mod;
		}
    	return moebius(a2, mod);
    }

    public static long[] mul(long[] a, long[] b) {
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	long[]ret=new long[Math.max(a.length, b.length)];
    	for (int i = 1; i < a.length; i++) {
    		for (int j = 1; j <= (ret.length - 1) / i && j < b.length; j++) {
				ret[i * j] += a[i] * b[j];
			}
		}
    	return ret;
    }
    
    public static long[] mul(long[] a, long[] b, long mod) {
    	if (a[0] !=0 || b[0] != 0) throw new AssertionError();
    	long[]ret=new long[Math.max(a.length, b.length)];
    	for (int i = 1; i < a.length; i++) {
    		for (int j = 1; j <= (ret.length - 1) / i && j < b.length; j++) {
				ret[i * j] += a[i] * b[j];
				ret[i * j] %= mod;
			}
		}
    	return ret;
    }
    
    /**
     * O(n log n)
     * @param a
     * @param mod
     * @return
     */
    public static long[] inv(long[] a, long mod) {
    	Fp mo=new Fp(mod);
    	if (a[0] != 0) throw new AssertionError();
    	if (a[1] == 0) throw new AssertionError();
    	long[] b=new long[a.length];
    	long ia = mo.inv(a[1]);
    	b[1] = ia;
    	for (int i = 1; i + 1 < a.length; i++) {
    		//b[i]まで確定済み
    		for (int j = 2; i * j < a.length; j++) {
				b[i * j] -= a[j] * b[i];
				b[i * j] = mo.reduce(b[i * j]);
    		}
    		b[i + 1] = mo.reduce(b[i + 1] * ia);
    		//k ≠ 1 のとき 0 = ∑ a[u] b[v] for uv = k
    		//           0 = a[1]b[k] + ∑ a[u] b[v] for uv = k and u ≠ 1
    	}
    	return b;
    }
    
    /**
     * 
     * @param a
     * @param mod
     * @return
     * verified:https://atcoder.jp/contests/abc428/submissions/70321995
     */
    public static long[] log(long[] a, long mod) {
    	if (a[0] != 0) throw new AssertionError();
    	if (a[1] != 1) throw new AssertionError();
    	// F'=a'/a
    	return integrate(mul(derivative(a, mod), inv(a, mod), mod), mod);
    }
    
    public static long[] derivative(long[] a, long mod) {
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = 1; i < b.length; i++) {
    		b[i] = b[i] * Sieve.omega(i) % mod;
    	}
    	return b;
    }
    
    public static long[] integrate(long[] a, long mod) {
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = 1; i < b.length; i++) {
			b[i] = b[i] * MathUtils.modInv(Sieve.omega(i), mod) % mod;
		}
    	return b;
    }
    
    /**
     * 1+a+a^2+...
     * @param a
     * @param mod
     * @return
     */
    public static long[] geometricSum(long[] a, long mod) {
    	//https://atcoder.jp/contests/abc239/tasks/abc239_h (サンプルのみ)
    	if(a[0]!=0)throw new AssertionError();
    	if(a[1]==1)throw new AssertionError();
    	long[]b=Arrays.copyOf(a, a.length);
    	for (int i = 2; i < a.length; i++) {
			b[i]=mod-a[i];
		}
    	b[1]=(1+mod-a[1])%mod;
    	if(b[1]==1)
    		return inv(b, mod);
    	else {
    		long invB1=MathUtils.modInv(b[1], mod);
    		for (int i = 0; i < b.length; i++) {
				b[i]= invB1 * b[i] %mod;
			}
    		long[] ret=inv(b, mod);
    		for (int i = 0; i < ret.length; i++) {
				ret[i]=ret[i]*invB1%mod;
			}
    		return ret;
    	}
    }
    
    public static ArrayOnQuotient prefixSumOfInv(ArrayOnQuotient sumA, long mod) {
    	//https://atcoder.jp/contests/abc239/submissions/74378739
    	//https://judge.yosupo.jp/problem/dirichlet_inverse_and_prefix_sums
    	// b =  a ^ -1 を求める。a(1) = 1 と仮定する。
    	// δ = a * b より
    	// 1 = a(1) b(1)
    	// 0 = a(n) b(1) + Σ[xy=n,y≠1]a(x)b(y)
    	// よって
    	// b(n) =  1 - Σ[xy=n,y≠1]a(x)b(y)
    	
    	// ∑[n=1..N] b(n) 
    	// = 1 - Σ[n=1..N]Σ[xy=n,x≠1]a(x)b(y)
    	// = 1 - Σ[1 <= xy <= N,x≠1]a(x)b(y)
    	// = 1 - Σ[x=2..N]a(x)∑[y=1..N/x]b(y)
    	
    	// k <= N / x < k + 1
    	//  N / (k + 1) < x <= N / k
    	// より x ∈ {[N/k] : k ∈ {1,2,..,n}} に対する
    	// ∑[i=1..x] a(i) が必要。
    	
    	
    	long n=sumA.n();
    	
    	long a1 = sumA.getByQuotient(1);
    	long ia1 = MathUtils.modInv(a1, mod);
    	for (var range:Itertools.floorRange(n)) {
    		sumA.setByQuotient(range.quotient(), sumA.getByQuotient(range.quotient()) * ia1 % mod);
    	}
	int sqrtn = (int)MathUtils.sqrt(sumA.n());
    	int threshold = (int) (n / (sqrtn + 1));
    	long[] a = sumA.rawArray();
    	long[] b = new long[a.length];
    	for (long q = 1; q <= n; q = n / (n / (q + 1))) {
            // ∑[n=1..range.quotient()] b(n) を求める
            long sum = 1;
            for (long p = 1; p <= q; p = q / (q / (p + 1))) {
                // 1 - Σ[x=2..N]a(x)∑[y=1..N/x]b(y) の N/x を p とした場合
            	long lower = q / (p + 1) + 1;
            	long upper = q / p;
            	// 閉区間[lower, upper]を考える。
            	lower = Math.max(lower, 2);
            	if (lower <= upper) {
            		long u = b[(int)(p <= threshold ? (b.length - p) : (n / p - 1))];//sumB.getByQuotient(p)
//            		long v = sumA.getByQuotient(upper) - sumA.getByQuotient(lower - 1);
            		long v = a[(int)(upper <= threshold ? (a.length - upper) : (n / upper - 1))] - a[(int)(lower - 1 <= threshold ? (a.length - lower + 1) : (n / (lower - 1) - 1))];
            		sum = (sum - u * v) % mod;
            	}
                if (p == q) break;
            }
            if (sum < 0) {
                sum += mod;
            }
            b[(int)(q <= threshold ? (b.length - q) : (n / q - 1))] = sum;
//            sumB.setByQuotient(q, sum);
            if (n == q) break;
        }
    	for (int i = 0; i < b.length; i++) {
			b[i] = b[i] * ia1 % mod;
		}
    	var sumB=new ArrayOnQuotient(n);
    	sumB.data = b;
    	return sumB;
    }
    
    static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
    
}
