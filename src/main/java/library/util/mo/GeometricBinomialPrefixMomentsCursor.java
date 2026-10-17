package library.util.mo;

import java.util.Arrays;

import library.util.Fp;

/**
 * 各 k = 0, ..., degree について
 *
 * S_k(l, r) = sum_(i=0)^l i^(underline k) a^i C(r, i) mod mod
 *
 * を動的に維持する MoCursor。
 *
 * <p>
 * ここで
 *
 * i^(underline k) = i(i-1)...(i-k+1)
 *
 * とする。
 *
 * <p>
 * 多項式
 *
 * p(x) = sum_k c_k x^(underline k)
 *
 * に対して
 *
 * sum_(i=0)^l p(i) a^i C(r, i)
 * = sum_k c_k S_k(l, r)
 *
 * となるため、多項式部分はこのクラスの外で下降階乗基底に変換すればよい。
 *
 * <p>
 * l < 0 のときは prefix を空和とみなし、すべての S_k を 0 とする。
 *
 * <p>
 * 事前条件:
 * degree >= 0,
 * 1 + a != 0 (mod mod)
 *
 * <p>
 * 各カーソル移動の計算量は O(degree)。
 * a^x は必要になった範囲まで遅延前計算する。
 *
 * @param <T> getValue(int) の返り値
 */
public abstract class GeometricBinomialPrefixMomentsCursor<T> extends MoCursor<T> {

	/** 有限体構造 */
	protected final Fp fp;

	/** 下降階乗の最大次数 */
	protected final int degree;

	/** 等比公比 a (mod mod) */
	protected final long a;

	/**
	 * s[k] = sum_(i=0)^l i^(underline k) a^i C(r, i) mod mod
	 */
	protected final long[] s;

	/** mod */
	private final long mod;

	/** 1 + a */
	private final long onePlusA;

	/** (1 + a)^(-1) */
	private final long invOnePlusA;

	/** ak[k] = a * k */
	private final long[] ak;

	/**
	 * boundary[k] = x^(underline k) a^x
	 *
	 * 直前に setBoundary した x に対する値を保持する。
	 */
	private final long[] boundary;

	/** boundary が現在表している x */
	private int boundaryX = Integer.MIN_VALUE;

	/**
	 * powA[x] = a^x。
	 *
	 * 必要になった位置まで遅延前計算する。
	 */
	private long[] powA = { 1 };

	/**
	 * GeometricBinomialPrefixMomentsCursor を構築する。
	 *
	 * <p>
	 * 事前条件:
	 * degree >= 0,
	 * 1 + a != 0 (mod mod)
	 *
	 * <p>
	 * 計算量: O(degree)
	 *
	 * @param degree 下降階乗の最大次数
	 * @param a 等比公比
	 * @param fp 有限体構造
	 */
	public GeometricBinomialPrefixMomentsCursor(int degree, long a, Fp fp) {
		if (degree < 0) {
			throw new IllegalArgumentException("degree < 0");
		}

		this.fp = fp;
		this.degree = degree;
		this.a = fp.reduce(a);
		this.mod = fp.modulus();

		this.s = new long[degree + 1];
		this.ak = new long[degree + 1];
		this.boundary = new long[degree + 1];

		for (int k = 1; k <= degree; k++) {
			ak[k] = fp.mul(this.a, k);
		}

		onePlusA = fp.reduce(1 + this.a);
		if (onePlusA == 0) {
			throw new IllegalArgumentException("1 + a == 0 mod mod");
		}
		invOnePlusA = fp.inv(onePlusA);

		// 初期状態 (l, r) = (0, 0)
		// S_0 = C(0,0) = 1
		s[0] = 1;
	}

	/**
	 * 左端 l を i + 1 から i へ進める。
	 *
	 * <p>
	 * prefix の上端が i+1 から i になるので、
	 * x=i+1 の項を取り除く。
	 *
	 * <p>
	 * x < 0 ではどちらも空 prefix なので何もしない。
	 *
	 * @param i 移動後の l
	 */
	@Override
	public void addLeft(int i) {
		int x = i + 1;
		if (x < 0) {
			return;
		}

		setBoundary(x);
		long c = fp.comb(r, x);

		for (int k = 0; k <= degree; k++) {
			s[k] = subMod(s[k], fp.mul(boundary[k], c));
		}
	}

	/**
	 * 左端 l を i から i + 1 へ進める。
	 *
	 * <p>
	 * prefix に x=i+1 の項を追加する。
	 *
	 * <p>
	 * x < 0 ではどちらも空 prefix なので何もしない。
	 *
	 * @param i 移動前の l
	 */
	@Override
	public void popLeft(int i) {
		int x = i + 1;
		if (x < 0) {
			return;
		}

		setBoundary(x);
		long c = fp.comb(r, x);

		for (int k = 0; k <= degree; k++) {
			s[k] = addMod(s[k], fp.mul(boundary[k], c));
		}
	}

	/**
	 * 右端 r を i から i + 1 へ拡張する。
	 *
	 * <p>
	 * 恒等式
	 *
	 * (x+1)^(underline k)
	 * = x^(underline k) + k x^(underline{k-1})
	 *
	 * から
	 *
	 * S_k(L,R+1)
	 * = (1+a)S_k(L,R)
	 * + ak S_(k-1)(L,R)
	 * - (L+1)^(underline k) a^(L+1) C(R,L)
	 *
	 * が成り立つ。
	 *
	 * <p>
	 * l < 0 では prefix は空なので何もしない。
	 *
	 * @param i 拡張前の r
	 */
	@Override
	public void addRight(int i) {
		if (l < 0) {
			return;
		}

		setBoundary(l + 1);
		long c = fp.comb(i, l);

		/*
		 * s[k-1] は更新前の値を使う必要があるため、
		 * k の降順に更新する。
		 */
		for (int k = degree; k >= 0; k--) {
			long v = fp.mul(onePlusA, s[k]);

			if (k > 0) {
				v = addMod(v, fp.mul(ak[k], s[k - 1]));
			}

			v = subMod(v, fp.mul(boundary[k], c));
			s[k] = v;
		}
	}

	/**
	 * 右端 r を i + 1 から i へ縮小する。
	 *
	 * <p>
	 * addRight の関係
	 *
	 * T_k
	 * = (1+a)S_k
	 * + ak S_(k-1)
	 * - B_k
	 *
	 * を k=0,1,... の順に逆算する。
	 *
	 * <p>
	 * l < 0 では prefix は空なので何もしない。
	 *
	 * @param i 縮小後の r
	 */
	@Override
	public void popRight(int i) {
		if (l < 0) {
			return;
		}

		setBoundary(l + 1);
		long c = fp.comb(i, l);

		/*
		 * s[k-1] は復元後の値を使うため、
		 * k の昇順に更新する。
		 */
		for (int k = 0; k <= degree; k++) {
			long v = s[k];

			if (k > 0) {
				v = subMod(v, fp.mul(ak[k], s[k - 1]));
			}

			v = addMod(v, fp.mul(boundary[k], c));
			s[k] = fp.mul(v, invOnePlusA);
		}
	}

	/**
	 * S_k(l,r) =
	 * sum_(i=0)^l i^(underline k) a^i C(r,i)
	 * を返す。
	 *
	 * @param k 下降階乗の次数
	 * @return S_k(l,r)
	 */
	public long moment(int k) {
		if (k < 0 || k > degree) {
			throw new IndexOutOfBoundsException();
		}
		return s[k];
	}

	/**
	 * boundary[k] =
	 * x^(underline k) a^x
	 *
	 * を設定する。
	 *
	 * <p>
	 * 同じ x が連続して要求された場合は何もしない。
	 * Mo では左端を固定したまま右端を連続して動かすことが多いため、
	 * このキャッシュが有効になる。
	 */
	private void setBoundary(int x) {
		if (x == boundaryX) {
			return;
		}
		boundaryX = x;

		boundary[0] = powerA(x);

		for (int k = 1; k <= degree; k++) {
			boundary[k] = fp.mul(
					boundary[k - 1],
					fp.reduce((long) x - k + 1));
		}
	}

	/**
	 * a^x を返す。
	 *
	 * <p>
	 * 必要になった最大 x まで順番に計算するため、
	 * 全体で O(max x)、1回あたり償却 O(1)。
	 */
	private long powerA(int x) {
		if (x < powA.length) {
			return powA[x];
		}

		int oldLength = powA.length;
		int newLength = oldLength;

		while (newLength <= x) {
			newLength <<= 1;
		}

		powA = Arrays.copyOf(powA, newLength);

		for (int i = oldLength; i < newLength; i++) {
			powA[i] = fp.mul(powA[i - 1], a);
		}

		return powA[x];
	}

	/**
	 * 0 <= x,y < mod を仮定して x+y mod mod を返す。
	 */
	private long addMod(long x, long y) {
		long z = x + y;
		return z >= mod ? z - mod : z;
	}

	/**
	 * 0 <= x,y < mod を仮定して x-y mod mod を返す。
	 */
	private long subMod(long x, long y) {
		long z = x - y;
		return z < 0 ? z + mod : z;
	}
}
