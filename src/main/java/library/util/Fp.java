package library.util;

import java.util.Arrays;

import library.util.algebra.strategy.longs.LongFieldStrategy;
import library.util.collections.LongArrayList;
import library.util.polynomial.HolonomicSequence;
import library.util.polynomial.PolynomialFpDynamic;

public class Fp extends Zn implements LongFieldStrategy {
	/** 998244353 を法とする有限体 F_998244353。 */
	public static final Fp MOD998244353 = new Fp(998244353);
	public static final Fp MOD1000000007 = new Fp(1000000007);
	
	public Fp(long mod) {
		super(mod);
	}

	/** fac(n) 用に遅延初期化される階乗テーブル。 */
	int[] fac = new int[0];
	/** ifac(n) 用に遅延初期化される逆階乗テーブル。 */
	int[] ifac = new int[0];
	/** inv(n) 用に遅延初期化される逆元テーブル。 */
	int[] inv = new int[0];
	/** fib(n) 用に遅延初期化されるフィボナッチ数テーブル。 */
	long[] fib = new long[0];
	
	/**
	 * fac, ifac, invを長さnまで延長
	 * @param n
	 */
	public void expand(int n) {
		if (n <= fac.length) return;
		fac = new int[n];
		ifac = new int[n];
		inv = new int[n];
		Arrays.fill(fac, 1);
		Arrays.fill(ifac, 1);
		Arrays.fill(inv, 1);
		for (int i = 2; i < n; ++i) {
			fac[i] = (int) (i * (fac[i - 1] & 0xffffffffL) % mod);
			inv[i] = (int) (mod - (mod / i) * (inv[(int) (mod % i)] & 0xffffffffL) % mod);
			ifac[i] = (int) ((inv[i] & 0xffffffffL) * (ifac[i - 1] & 0xffffffffL) % mod);
		}
	}
	
	public long fac(int n) {
		if (n < 0) return 0;
		if (fac.length <= n) {
			expand(Math.max(2 * fac.length, n + 1));
		}
		return fac[n] & 0xffffffffL;
	}
	
	public long ifac(int n) {
		if (ifac.length <= n) {
			expand(Math.max(2 * ifac.length, n + 1));
		}
		return ifac[n] & 0xffffffffL;
	}
	
	/**
	 * n < 0 でもok
	 * @param n
	 * @return
	 */
	public long inv(long n) {
		if(n < 0) {
			n = reduce(n);
		}
		return n < inv.length ?  inv[(int) n] & 0xffffffffL : MathUtils.modInv(n, mod);
	}

	/**
	 * 事前条件: b != 0 in F_mod。返り値 r は r = a / b in F_mod。
	 * 未テスト
	 * 計算量: O(log mod)
	 * @param a 被除数
	 * @param b 除数
	 * @return a / b mod mod
	 */
	@Override
	public long div(long a, long b) {
		return mul(a, inv(b));
	}

	/**
	 * 事前条件: b != 0 in F_mod。返り値 r は r = 0。
	 * 未テスト
	 * 計算量: O(1)
	 * @param a 被除数
	 * @param b 除数
	 * @return 0
	 */
	@Override
	public long mod(long a, long b) {
		if (equals(b, zero())) throw new ArithmeticException("Division by zero");
		return zero();
	}

	/**
	 * 返り値 r は a = 0 なら r = 0、そうでなければ r = 1。
	 * 未テスト
	 * 計算量: O(1)
	 * @param a 対象
	 * @return a のノルム
	 */
	@Override
	public long norm(long a) {
		return equals(a, zero()) ? 0 : 1;
	}

	/**
	 * 返り値 u は a = 0 なら u = 1、そうでなければ u = a。
	 * 未テスト
	 * 計算量: O(1)
	 * @param a 対象
	 * @return a = u * canonical(a) を満たす単元 u
	 */
	@Override
	public long canonicalUnit(long a) {
		if (equals(a, zero())) return one();
		return a;
	}

	
	/**
	 * comb(n+k-1,k)を返す。n=k=0のときは1を返す。
	 * @param n
	 * @param k
	 * @return
	 */
	public long combrep(int n, int k) {
		if (k < 0) return 0;
		if (n == 0 && k == 0) return 1;
		return comb(n + k - 1, k);
	}
	
	public long combrepNaive(long n, long k) {
		if (k < 0) return 0;
		if (n == 0 && k == 0) return 1;
		return combNaive(n + k - 1, k);
	}
	
	/**
	 * comb(0, 0)=1とする。
	 * @param n
	 * @param k
	 * @return
	 */
	public long comb(int n, int k) {
		if (k < 0 || n - k < 0) return 0;
		return fac(n) * ifac(k) % mod * ifac(n - k) % mod;
	}
	
	/**
	 * comb(n, k)(-1)^k
	 * @param n
	 * @param k
	 * @return
	 */
	public long signedComb(int n, int k) {
		long ret = comb(n, k);
		if (k % 2 == 1 && ret != 0) ret = mod - ret;
		return ret;
	}
	
	public long combNaive(long n, long k) {
		//https://yukicoder.me/submissions/1153305
		if (k < 0 || n - k < 0) return 0;
		k = Math.min(k, n - k);
		long ret = 1;
		n %= mod;
		for (int i = 0; i < k; i++) {
			ret = ret * (n - i) % mod;
			ret = ret * inv(i + 1) % mod;
		}
		ret = reduce(ret);
		return ret;
	}
	
	/**
	 * n!/(n-k)!
	 * @param n
	 * @param k
	 * @return
	 */
	public long perm(int n, int k) {
		if(k>n||k<0)return 0;
		if(n<=100000000) {
			return fac(n) * ifac(n - k) % mod;
		} else {
			long ret = 1;
			for (int i = n-k+1; i <= n; i++) {
				ret=ret*i%mod;
			}
			return ret;
		}
	}
	
	public long iperm(int n, int k) {
		if(k>n||k<0)return 0;
		return ifac(n) * fac(n - k) % mod;
	}
	
	public long fib(int n) {
		if (fib.length <= n) {
			fib = new long[2 * Integer.highestOneBit(Math.max(1, n))];
			fib[0] = 1;
			fib[1] = 1;
			for (int i = 2; i < fib.length; ++i) {
				fib[i] = (fib[i - 1] + fib[i - 2]) % mod;
			}
		}
		return fib[n];
	}
	
	/**
	 * a は 32bit 整数を仮定
	 */
	public long pow(long a, long n) {
		if (a == 1) return 1;
		if (n < 0) {
			a = inv(a);
			n = -n;
		}
		return MathUtils.modPow(a, n, mod);
	}
	
	/**
	 * doubleの仮数部が52bitなので、そのあたりまでは動く...はず....
	 * @param a
	 * @param b
	 * @return
	 */
    public long mul_52bit(long a, long b) {
    	long ret=a*b-(long)(1.*a*b/mod)*mod;
    	while(ret<0)ret+=mod;
    	while(ret>=mod)ret-=mod;
    	return ret;
    }
    
	/**
	 * doubleの仮数部が52bitなので、そのあたりまでは動く...はず....
	 * @param a
	 * @param b
	 * @return
	 */
    public long pow_52bit(long a, long n) {
    	if(n==0)return 1;
    	if(n==1)return a;
    	long ret=pow_52bit(mul_52bit(a,a),n/2);
    	if(n%2==1)ret=mul_52bit(ret, a);
    	return ret;
    }
    
    /**
     * a の乗法位数を返す。
     *
     * <p>primeDivisors には mod - 1 の異なる素因数をすべて渡す。
     * {@code k = primeDivisors.size()} とすると、
 	 * O(log(mod) log k)。
     * @param a 位数を求める非零元
     * @param primeDivisors mod - 1 の異なる素因数
     * @return a の乗法位数
     */
    public long order_52bit(long a, LongArrayList primeDivisors) {
    	//https://atcoder.jp/contests/abc335/submissions/79278466
    	int k = primeDivisors.size();
    	if (k == 0)
    		return 1;

    	long[] prime = new long[k];
    	long[] primePower = new long[k];

    	long rest = mod - 1;
    	int i = 0;

    	for (long p : primeDivisors) {
    		long q = 1;
    		while (rest % p == 0) {
    			rest /= p;
    			q *= p;
    		}

    		if (q == 1)
    			throw new IllegalArgumentException();

    		prime[i] = p;
    		primePower[i] = q;
    		i++;
    	}

    	if (rest != 1)
    		throw new IllegalArgumentException();

    	long[] product = new long[4 * k];
    	buildOrderProductTree(
    			1, 0, k,
    			primePower, product);

    	a %= mod;
    	if (a < 0)
    		a += mod;

    	if (a == 0)
    		throw new ArithmeticException(
    				"0 has no multiplicative order");

    	/*
    	 * powPair_52bit の返り値用。
    	 * 再帰中ずっと同じ配列を使い回す。
    	 */
    	long[] pair = new long[2];

    	return order_52bitProductTree(
    			a,
    			1, 0, k,
    			prime, product, pair);
    }


    /**
     * 各ノードについて、その区間に含まれる
     * prime power の積を求める。
     */
    private long buildOrderProductTree(
    		int node,
    		int l,
    		int r,
    		long[] primePower,
    		long[] product) {

    	if (r - l == 1)
    		return product[node] = primePower[l];

    	int mid = (l + r) >>> 1;

    	return product[node] =
    			buildOrderProductTree(
    					node << 1,
    					l, mid,
    					primePower, product)
    			*
    			buildOrderProductTree(
    					node << 1 | 1,
    					mid, r,
    					primePower, product);
    }


    /**
     * ord(x) | product[node] を仮定して x の位数を求める。
     */
    private long order_52bitProductTree(
    		long x,
    		int node,
    		int l,
    		int r,
    		long[] prime,
    		long[] product,
    		long[] pair) {

    	if (r - l == 1) {
    		long p = prime[l];
    		long order = 1;

    		if (p == 2) {
    			while (x != 1) {
    				x = mul_52bit(x, x);
    				order <<= 1;
    			}
    		} else {
    			while (x != 1) {
    				x = pow_52bit(x, p);
    				order *= p;
    			}
    		}

    		return order;
    	}

    	int leftNode = node << 1;
    	int rightNode = leftNode | 1;
    	int mid = (l + r) >>> 1;

    	/*
    	 * L = product[leftNode]
    	 * R = product[rightNode]
    	 *
    	 * x^R は左側成分だけ、
    	 * x^L は右側成分だけを残す。
    	 *
    	 * 2 回 pow_52bit する代わりに、
    	 * 同じ x の平方列を共有する。
    	 */
    	powPair_52bit(
    			x,
    			product[rightNode],
    			product[leftNode],
    			pair);

    	long left = pair[0];
    	long right = pair[1];

    	return order_52bitProductTree(
    				left,
    				leftNode, l, mid,
    				prime, product, pair)
    			*
    			order_52bitProductTree(
    				right,
    				rightNode, mid, r,
    				prime, product, pair);
    }


    /**
     * x^a, x^b を同時に計算する。
     *
     * <p>通常
     *
     * <pre>
     * pow_52bit(x, a);
     * pow_52bit(x, b);
     * </pre>
     *
     * とすると x, x^2, x^4, ... の計算を2回行うが、
     * 本メソッドではこれを共有する。
     *
     * <p>out[0] = x^a, out[1] = x^b。
     */
    private void powPair_52bit(
    		long x,
    		long a,
    		long b,
    		long[] out) {

    	long ra = 1;
    	long rb = 1;

    	boolean hasA = false;
    	boolean hasB = false;

    	long y = x;

    	while ((a | b) != 0) {
    		if ((a & 1) != 0) {
    			if (hasA)
    				ra = mul_52bit(ra, y);
    			else {
    				ra = y;
    				hasA = true;
    			}
    		}

    		if ((b & 1) != 0) {
    			if (hasB)
    				rb = mul_52bit(rb, y);
    			else {
    				rb = y;
    				hasB = true;
    			}
    		}

    		a >>>= 1;
    		b >>>= 1;

    		if ((a | b) != 0)
    			y = mul_52bit(y, y);
    	}

    	out[0] = ra;
    	out[1] = rb;
    }
    
    
	/**
	 * {@code p = mod}, {@code L = floor(sqrt(p))},
	 * {@code x = v mod p}（{@code 0 <= x < p}）とする。
	 * {@code x = 0} のとき {@code "0"} を返す。
	 * {@code x != 0} のとき、次のすべてを満たす整数 {@code a, b} を
	 * 10 進表記した文字列 {@code "a/b"} を返す。
	 *
	 * <ul>
	 * <li>{@code 0 < |a| <= L}</li>
	 * <li>{@code 1 <= b <= L}</li>
	 * <li>{@code gcd(|a|, b) = 1}</li>
	 * <li>{@code a == b * x (mod p)}</li>
	 * </ul>
	 *
	 * <p>{@code a/b} は有理数として既約であり、分母は正である。
	 * 上の条件を満たす有理数が複数存在する場合、どれが返るかは規定しない。</p>
	 *
	 * <p>事前条件: {@code p} は素数。</p>
	 * <p>この事前条件の下では、{@code x != 0} のとき、上記4条件を満たす
	 * {@code (a, b) in Z^2} が少なくとも1組存在する。したがって、任意の
	 * {@code v} に対して規定された返り値が必ず存在する。</p>
	 * <p>未テスト</p>
	 * <p>計算量: O(log p) 時間、O(1) 追加領域。</p>
	 *
	 * @param v 復元する剰余類の任意の整数代表元
	 * @return {@code x = 0} なら {@code "0"}、それ以外なら上記の {@code "a/b"}
	 */
    public String restoreRational(long v) {
    	v = reduce(v);
    	if (v == 0) return "0";

    	long lim = MathUtils.sqrt(mod);

    	/*
    	 * r = s * mod + t * v
    	 *
    	 * を満たす Euclid 列を追う。
    	 * したがって r ≡ t*v (mod mod)。
    	 */
    	long r0 = mod, r1 = v;
    	long t0 = 0,   t1 = 1;

    	// |t0|, |t1|。符号は交互なので絶対値はこの漸化式になる。
    	long d0 = 0, d1 = 1;

    	long ansA = 0;
    	long ansB = 0;

    	while (true) {
    		// |a| <= sqrt(mod), b <= sqrt(mod) の候補
    		if (r1 != 0 && r1 <= lim) {
    			long a = r1;
    			long b = t1;

    			if (b < 0) {
    				a = -a;
    				b = -b;
    			}

    			ansA = a;
    			ansB = b;
    		}

    		if (r1 == 0) break;

    		long q = r0 / r1;

    		/*
    		 * 次の denominator は
    		 *
    		 * d2 = d0 + q*d1
    		 *
    		 * これが lim を超えるなら、それ以降も denominator は増えるので終了。
    		 * この書き方なら q*d1 の overflow も避けられる。
    		 */
    		if (q > (lim - d0) / d1) break;

    		long r2 = r0 % r1;
    		long t2 = t0 - q * t1;
    		long d2 = d0 + q * d1;

    		r0 = r1;
    		r1 = r2;
    		t0 = t1;
    		t1 = t2;
    		d0 = d1;
    		d1 = d2;
    	}

    	if (ansB == 0) throw new AssertionError();

    	return ansA + "/" + ansB;
    }
    
    
	/**
	 * start+(start+1)+..+(start+n-1)
	 * @param a
	 * @param b
	 * @return
	 */
	public long pow1Sum(long start, long length) {
		if(length<=0) return 0;
		length%=mod;
		start%=mod;
		long ret=length*(length-1+2*start)%mod*inv(2)%mod;
		return reduce(ret);
	}
	
	/**
	 * start^2 + (start+1)^2 + ... + (start+length-1)^2 を mod 上で計算する。
	 * 未テスト
	 * 計算量: O(1)
	 * @param start 開始値
	 * @param length 項数
	 * @return 2乗和の mod
	 */
	public long pow2Sum(long start, long length) {
		if (length <= 0) return 0;
		if (mod <= 1) return 0;
		long A = reduce(start);

		long term1 = A * A % mod;
		long term2 = A * (reduce(length - 1)) % mod;
		long term12 = (term1 + term2) % mod;
		long term12L = (length % mod) * term12 % mod;
		long term3 = sumOfSquaresTerm(length);
		long sum = (term12L + term3) % mod;
		return reduce(sum);
	}

	/**
	 * start^3 + (start+1)^3 + ... + (start+length-1)^3 を mod 上で計算する。
	 * 未テスト
	 * 計算量: O(1)
	 * @param start 開始値
	 * @param length 項数
	 * @return 3乗和の mod
	 */
	public long pow3Sum(long start, long length) {
		if (length <= 0) return 0;
		if (mod <= 1) return 0;
		long A = reduce(start);

		long H;
		if (length % 2 == 0) {
			H = (length / 2) % mod * ((length - 1) % mod) % mod;
		} else {
			H = length % mod * (((length - 1) / 2) % mod) % mod;
		}
		H = reduce(H);

		long term1 = (length % mod) * (A * A % mod) % mod * A % mod;
		long term2 = 3 * (A * A % mod) % mod * H % mod;
		long term3 = 3 * A % mod * sumOfSquaresTerm(length) % mod;
		long term4 = H * H % mod;
		long sum = (term1 + term2 + term3 + term4) % mod;
		return reduce(sum);
	}

	private long sumOfSquaresTerm(long length) {
		long[] factors = {length, length - 1, 2 * length - 1};
		if (factors[0] % 2 == 0) {
			factors[0] /= 2;
		} else {
			factors[1] /= 2;
		}
		if (factors[0] % 3 == 0) {
			factors[0] /= 3;
		} else if (factors[1] % 3 == 0) {
			factors[1] /= 3;
		} else {
			factors[2] /= 3;
		}
		long res = 1;
		for (long f : factors) {
			res = res * reduce(f) % mod;
		}
		return res;
	}
	
	public long[][] mulTable(int n) {
		long[][] ret=new long[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				ret[i][j]=1L*i*j%mod;
			}
		}
		return ret;
	}
	
    /** always x + e >= y
     *  (a, b) -> (c, d)
     *  (1, 0), (0, 1) 
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @return
     */
    public long mirror(int a, int b, int c, int d, int e){
    	//https://atcoder.jp/contests/abc216/submissions/74255616
        if (a + e < b || c + e < d) return 0;
        if (a > c || b > d) return 0;
        a += e;
        c += e;
        long ret=comb(c + d - a - b, c - a) - comb(c + d - a - b, c - b + 1); 
        ret=reduce(ret);
        return ret;
    }
    
    
    
    /** always x + e >= y
     *  (0, 0) -> (c, d)
     *  未テスト
     * @param a
     * @param b
     * @param c
     * @param d
     * @param e
     * @return
     */
    public long mirror(int c, int d, int e){
        if (e < 0 || c + e < d) return 0;
        if (0 > c || 0 > d) return 0;
        long ret=comb(c + d, c) - comb(c + d, c + e + 1); 
        ret=reduce(ret);
        return ret;
    }
    
    /**
     * (1, 1), (1, -1) のステップを合計で n 個使って、(0, 0) から (n, level) に行く方法の数。
     * ただし、全ての途中の点 (i, y_i) で y_i >= level を満たす必要がある。
     * 計算量: O(1)
     * @param n ステップ数
     * @param level 到着点の y 座標
     * @return 方法の数
     */
    public long mirror(int n, int level) {
        if ((n + level) % 2 != 0 || Math.abs(level) > n) return 0;
        int x = (n + level) / 2;
        int y = (n - level) / 2;
        return mirror(x, y, -level);
    }

	/**
	 * n! mod mod を O(√n log n) で計算する。
	 * @param n 階乗を求める数
	 * @return n! mod mod
	 */
	public long facLarge(long n) {
		if (n >= mod) return 0;
		if (n < fac.length) return fac((int) n);
		PolynomialFpDynamic poly = PolynomialFpDynamic.of(mod);
		return HolonomicSequence.prefixProduct(new long[] {1, 1}, n, poly);
	}

	/**
	 * nPk mod mod を O(√k log k) で計算する。
	 * @param n 総数
	 * @param k 選択する数
	 * @return nPk mod mod
	 */
	public long permLarge(long n, long k) {
		if (k < 0 || n < k) return 0;
		if (k == 0) return 1;
		if (n / mod > (n - k) / mod) return 0;
		if (n < fac.length && k < fac.length) {
			return fac((int) n) * ifac((int) (n - k)) % mod;
		}
		PolynomialFpDynamic poly = PolynomialFpDynamic.of(mod);
		return HolonomicSequence.prefixProduct(new long[] {n % mod, mod - 1}, k, poly);
	}

	/**
	 * nCk mod mod を O(√k log k) で計算する。
	 * @param n 総数
	 * @param k 選択する数
	 * @return nCk mod mod
	 */
	public long combLarge(long n, long k) {
		if (k < 0 || n < k) return 0;
		if (k == 0 || k == n) return 1;
		if (k > n / 2) k = n - k;
		if (n < fac.length) return comb((int) n, (int) k);
		long num = permLarge(n, k);
		long den = facLarge(k);
		return num * MathUtils.modInv(den, mod) % mod;
	}

	/**
	 * リュカの定理を用いて、法 mod が素数のときの二項係数 comb(n, k) mod mod を計算する。
	 *
	 * 計算量: mod が小さい（10^6 未満）場合は O(mod + log_mod n)、
	 *         mod が大きい場合は O(sqrt(mod) log(mod) log_mod n)
	 *
	 * @param n 総数
	 * @param k 選択する数
	 * @return comb(n, k) mod mod
	 */
	public long combLucas(long n, long k) {
		if (k < 0 || n < k) return 0;
		if (mod <= 1) return 0;
		long ans = 1;
		long tempN = n;
		long tempK = k;
		while (tempN > 0 || tempK > 0) {
			long ni = tempN % mod;
			long ki = tempK % mod;
			if (ni < ki) return 0;
			long digitComb = 1;
			if (ki > 0 && ni != ki) {
				long targetK = Math.min(ki, ni - ki);
				if (ni < fac.length) {
					digitComb = comb((int) ni, (int) ki);
				} else if (mod < 1000000) {
					digitComb = comb((int) ni, (int) ki);
				} else if (targetK <= 1000) {
					digitComb = combNaive(ni, ki);
				} else {
					digitComb = combLarge(ni, ki);
				}
			}
			ans = ans * digitComb % mod;
			tempN /= mod;
			tempK /= mod;
		}
		return ans;
	}

	/**
	 * 負の二項係数に対応した comb(n, k)。
	 * n < 0 の場合は (-1)^k * comb(-n+k-1, k) を返す。
	 *
	 * 計算量: O(1)
	 * @param n 総数
	 * @param k 選択する数
	 * @return comb(n, k) mod mod
	 */
	public long combGeneralized(int n, int k) {
		if (k < 0) return 0;
		if (n >= 0) return comb(n, k);
		return signedComb(-n + k - 1, k);
	}

	/**
	 * 多項係数を計算する。
	 * ((Σ k_i)!)/(Π k_i!)
	 *
	 * 計算量: O(Σ k_i)
	 * @param ks
	 * @return
	 */
	public long multinomial(int... ks) {
		int n = 0;
		for (int k : ks) {
			if (k < 0) return 0;
			n += k;
		}
		if (n == 0) return 1;
		long res = fac(n);
		for (int k : ks) {
			res = res * ifac(k) % mod;
		}
		return res;
	}

    /**
     * 等比重み付き冪和
     *
     *   sum_(i=0)^(n-1) i^d r^i
     *
     * を mod 上で返す。
     *
     * r = 1 のときは冪和の多項式補間、r != 1 のときは
     * (1 - r x)^(d + 1) を掛けた差分で項数を O(d) 個に落として計算する。
     *
     * @param r 等比の公比
     * @param d 冪の次数。d >= 0 を仮定する。
     * @param n 和の項数。n <= 0 のときは 0 を返す。
     * @return {@code sum_(i=0)^(n-1) i^d r^i} の mod
     *
     * 計算量: O(d log d + log n)
     */
    public long geometricPowerSum(long r, int d, long n) {
    	//https://judge.yosupo.jp/submission/371786
		if (r == 1) {
			long ans = 0;
			int deg = d + 1;
			long[] f = new long[deg + 1];
			for (int i = 1; i <= deg; ++i) {
				f[i] = pow(i, d);
			}
			for (int i = 1; i < f.length; ++i) {
				f[i] += f[i - 1];
				f[i] %= mod;
			}
			long  point = n - 1;
			if (point < 0) return 0;
			if (point < f.length) {
				ans = f[(int) point];
			} else {
				point %= mod;
				for (int i = 0; i < f.length; ++i) {
					f[i] = f[i] * ifac(i) % mod * ifac(deg - i) % mod;
					if ((deg - i) % 2 == 1) f[i] = mod - f[i];
				}
				long lp = 1;
				long rp = 1;
				for (int i = 0; i < f.length; ++i) {
					f[i] = f[i] * lp % mod;
					lp = lp * (point - i) % mod;
				}
				for (int i = f.length - 1; i >= 0; --i) {
					f[i] = f[i] * rp % mod;
					rp = rp * (point - i) % mod;
				}
				for (int i = 0; i < f.length; ++i) {
					ans = (ans + f[i]) % mod;
				}
			}
			if (n > 0) ans = (ans + mod + pow(r, 0) * pow(0, d) % mod) % mod;
			return ans;
		}
		long[] sum = new long[d + 2];
		{
			long pwr = 1;
			for (int i = 0; i <= d + 1; ++i) {
				sum[i] = (i % 2 == 0 ? comb(d + 1, i) : -comb(d + 1, i)) * pwr % mod;
				pwr = pwr * r % mod;
			}
			
		}
		for (int i = 1; i < sum.length; ++i) {
			sum[i] = (sum[i] + sum[i - 1]) % mod;
		}
		long ans = 0;
		int m1 = (int) Math.min(n - 1, d + 1);
		long m2 = Math.max(m1 + 1, n);
		{// m1 次以下
			long pwr = r;
			for (int i = 1; i <= m1; ++i) {
				ans += pow(i , d) * pwr % mod * sum[m1 - i] % mod;
				ans %= mod;
				pwr = pwr * r % mod;
			}
		}
		{// m2 次以上 (n - 1) + (d + 1) 次以下
			long src = Math.max(0, m2 - (d + 1));
			long pwr = pow(r, src);
			for (long i = Math.max(0, m2 - (d + 1)); i <= n - 1; ++i) {
				long add = pow(i % mod , d) * pwr % mod * (sum[(int) Math.min(d + 1, n + d - i)] - (m2 - i - 1 >= 0 ? sum[(int)(m2 - i - 1)] : 0)) % mod;
				ans += add;
				ans %= mod;
				pwr = pwr * r % mod;
			}
		}
		ans = ans * inv(pow(1 - r, (d + 1))) % mod;
		if (n > 0) ans = (ans + mod + pow(r, 0) * pow(0, d) % mod) % mod;
		return ans;
	}
    
    
    /**
     * 等比重み付き冪和
     *
     *   sum_(n=0)^(∞) n^d r^n
     *
     * を mod 上で返す。
     *
     * 公式
     *
     *   sum_(n=0)^(∞) n^d x^n = A_d(x) / (1 - x)^(d + 1)
     *
     * を使う。ただし A_d(x) は Eulerian polynomial 型の分子で、
     *
     *   A_d(x) = (1 - x)^(d + 1) sum_(n=0)^(∞) n^d x^n
     *
     * で定義される多項式。
     *
     * 計算量: 畳み込み部分 O(d)。i^d の列挙は線形篩と素数ごとの pow で行う。
     *
     * 注意:
     * - r != 1 mod mod を仮定する。
     * - pow(0, 0) = 1 として扱う実装なら、d = 0 のときも
     *   sum_(n=0)^(∞) r^n = 1 / (1 - r)
     *   になる。
     */
    public long infiniteGeometricPowerSum(long r, int d) {
    	//https://judge.yosupo.jp/submission/371780
        r %= mod;
        if (r < 0) r += mod;
        if (d == 0) {
        	//A_d=1となるので場合分け
        	return inv((1 + mod - r) % mod);
        }

        long[] pref = new long[d + 2];

        /*
         * pref[i] に
         *
         *   (-1)^i C(d + 1, i) r^i
         *
         * を入れる。
         *
         * これは (1 - r x)^{d + 1} の係数に対応する。
         */
        long pwr = 1;
        for (int i = 0; i <= d + 1; ++i) {
            long coef = comb(d + 1, i) * pwr % mod;
            if ((i & 1) == 1) coef = -coef;
            pref[i] = coef % mod;
            pwr = pwr * r % mod;
        }

        /*
         * pref[k] =
         *
         *   sum_{i=0}^{k} (-1)^i C(d + 1, i) r^i
         *
         * にする。
         */
        for (int i = 1; i < pref.length; ++i) {
            pref[i] = (pref[i] + pref[i - 1]) % mod;
        }

        /*
         * 分子 A_d(r) を計算する。
         *　A_d(x) = (1 - x)^{d + 1} sum_{n=0}^{∞} n^d x^n　で A_d は d 次式。
         * 展開すると
         *
         *   A_d(r)
         *   = sum_{i=1}^{d} i^d r^i
         *       sum_{j=0}^{d+1-i} (-1)^j C(d+1, j) r^j
         */
        long numerator = 0;
        pwr = r;
        for (int i = 1; i <= d; ++i) {
            numerator += pow(i, d) * pwr % mod * pref[d - i] % mod;
            numerator %= mod;
            pwr = pwr * r % mod;
        }

        /*
         * sum n^d r^n = A_d(r) / (1 - r)^{d + 1}
         */
        long denominator = pow(1 - r, d + 1);
        long ans = numerator * inv(denominator) % mod;

        if (ans < 0) ans += mod;
        return ans;
    }
    
	/**
	 * n^(underline k) = n(n-1)...(n-k+1)
	 * k < 0 の場合は 0、k = 0 の場合は 1 を返す。
	 */
	public long fallingFactorial(int n, int k) {
		if (k < 0) return 0;
		if (k == 0) return 1;
		if (k >= mod) return 0;
		if (k > n) return 0;
		return fac[n] * ifac[n - k] % mod;
	}
	
	
	/**
	 * sum_{i+j=t} falling(i,p) falling(j,q) comb(a, i) comb(b, j) = falling(a,p) falling(b,q) comb(a+b-p-q, t-p-q)
	 *
	 * @param a 1つ目の総数
	 * @param b 2つ目の総数
	 * @param p 1つ目の下降階乗の次数
	 * @param q 2つ目の下降階乗の次数
	 * @param t 和のインデックスの合計 i + j = t
	 * @return 畳み込み和の mod
	 */
	public long vandermonde(int a, int b, int p, int q, int t) {
		if (p < 0 || q < 0 || t < p + q) return 0;
		if (a < 0 || b < 0) return 0;
		long termA = fallingFactorial(a, p);
		if (termA == 0) return 0;
		long termB = fallingFactorial(b, q);
		if (termB == 0) return 0;
		long termC = comb(a + b - p - q, t - p - q);
		return termA * termB % mod * termC % mod;
	}

    

}
