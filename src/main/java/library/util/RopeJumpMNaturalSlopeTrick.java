package library.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 整数区間 [left, right] 上で定義された有限 1 次元 jump M-natural-convex 関数を保持・操作するクラス。
 *
 * 局所座標 i = 0, 1, ..., n-1 (実座標 x = left + i, n = right - left + 1) に対し、
 * 1 階差分 d_i = f(left + i + 1) - f(left + i) および 2 階和差分 s_i = f(left + i + 2) - f(left + i) = d_i + d_(i+1) とおく。
 * 1 次元 jump M-natural-convexity は条件 d_i <= d_(i+2) と同値であり、これより s_i <= s_(i+1) が成り立つ。
 *
 * 内部ではパリティごとの単調増加列
 * D0 = (d_0, d_2, d_4, ...), D1 = (d_1, d_3, d_5, ...),
 * S0 = (s_0, s_2, s_4, ...), S1 = (s_1, s_3, s_5, ...)
 * を永続 AVL Rope 構造で管理する。
 * 出力木の構築中に生成した専有ノードだけを書き換え、既存の共有部分木は変更しない。
 */
public class RopeJumpMNaturalSlopeTrick {
	/**
	 * 定義域の左端座標 left。
	 */
	private long left;

	/**
	 * 定義域の右端座標 right。
	 */
	private long right;

	/**
	 * 左端における関数値 f(left)。
	 */
	private long f0;

	/**
	 * パリティ 0 の 1 階差分列 D0 = (d_0, d_2, d_4, ...)。
	 */
	private Rope d0;

	/**
	 * パリティ 1 の 1 階差分列 D1 = (d_1, d_3, d_5, ...)。
	 */
	private Rope d1;

	/**
	 * パリティ 0 の 2 階和差分列 S0 = (s_0, s_2, s_4, ...)。
	 */
	private Rope s0;

	/**
	 * パリティ 1 の 2 階和差分列 S1 = (s_1, s_3, s_5, ...)。
	 */
	private Rope s1;

	/**
	 * コンストラクタ。
	 * O(1)
	 *
	 * @param left  定義域左端
	 * @param right 定義域右端
	 * @param f0    f(left) の値
	 * @param d0    D0 Rope
	 * @param d1    D1 Rope
	 * @param s0    S0 Rope
	 * @param s1    S1 Rope
	 */
	// 未テスト
	private RopeJumpMNaturalSlopeTrick(long left, long right, long f0,
			Rope d0, Rope d1, Rope s0, Rope s1) {
		this.left = left;
		this.right = right;
		this.f0 = f0;
		this.d0 = d0;
		this.d1 = d1;
		this.s0 = s0;
		this.s1 = s1;
	}

	/**
	 * 1 点 x のみで値が value となる単点関数を作成する。
	 * O(1)
	 *
	 * @param x     定義域の点
	 * @param value x における関数値
	 * @return 構築された JumpMNaturalSlopeTrick インスタンス
	 */
	// 未テスト
	public static RopeJumpMNaturalSlopeTrick singleton(long x, long value) {
		return new RopeJumpMNaturalSlopeTrick(x, x, value, null, null, null, null);
	}

	/**
	 * f(left + i) = values[i] となる関数を構築し、jump M-natural-convexity 条件を検証する。
	 * O(N)
	 *
	 * @param left   定義域の左端
	 * @param values 各点における関数値の配列 (要素数 1 以上)
	 * @return 構築された JumpMNaturalSlopeTrick インスタンス
	 */
	// 未テスト
	public static RopeJumpMNaturalSlopeTrick of(long left, long[] values) {
		return of(left, values, true);
	}

	/**
	 * f(left + i) = values[i] となる関数を構築する。
	 * O(N)
	 *
	 * @param left     定義域の左端
	 * @param values   各点における関数値の配列 (要素数 1 以上)
	 * @param validate 条件の検証を行うかどうかのフラグ
	 * @return 構築された JumpMNaturalSlopeTrick インスタンス
	 */
	// 未テスト
	public static RopeJumpMNaturalSlopeTrick of(long left, long[] values, boolean validate) {
		if (values.length == 0) throw new IllegalArgumentException("values must be nonempty");
		int n = values.length;
		long[] d = new long[Math.max(0, n - 1)];
		for (int i = 0; i < d.length; i++) d[i] = values[i + 1] - values[i];
		if (validate) {
			for (int i = 0; i + 2 < d.length; i++) {
				if (d[i] > d[i + 2]) {
					throw new IllegalArgumentException("not jump M^natural-convex at local d index " + i);
				}
			}
		}
		long[] s = new long[Math.max(0, n - 2)];
		for (int i = 0; i < s.length; i++) s[i] = d[i] + d[i + 1];
		if (validate) {
			for (int i = 0; i + 1 < s.length; i++) {
				if (s[i] > s[i + 1]) throw new AssertionError("internal characterization broken");
			}
		}
		return new RopeJumpMNaturalSlopeTrick(
				left,
				left + n - 1L,
				values[0],
				Rope.build(takeParity(d, 0)),
				Rope.build(takeParity(d, 1)),
				Rope.build(takeParity(s, 0)),
				Rope.build(takeParity(s, 1)));
	}

	/**
	 * 定義域の左端 left を取得する。
	 * O(1)
	 *
	 * @return 定義域左端 left
	 */
	// 未テスト
	public long left() {
		return left;
	}

	/**
	 * 定義域の右端 right を取得する。
	 * O(1)
	 *
	 * @return 定義域右端 right
	 */
	// 未テスト
	public long right() {
		return right;
	}

	/**
	 * 定義域内の点数 (right - left + 1) を取得する。
	 * O(1)
	 *
	 * @return 定義域の要素数
	 */
	// 未テスト
	public int size() {
		return Math.toIntExact(right - left + 1);
	}

	/**
	 * 指定した点 x における関数値 f(x) を評価する。
	 * O(log N)
	 *
	 * @param x 評価する点 (left <= x <= right)
	 * @return 点 x における関数値
	 */
	// 未テスト
	public long valueAt(long x) {
		if (x < left || x > right) throw new IllegalArgumentException("x outside domain: " + x);
		int i = Math.toIntExact(x - left);
		if ((i & 1) == 0) {
			int k = i >>> 1;
			return f0 + Rope.prefixSum(s0, k);
		} else {
			int k = i >>> 1;
			return f0 + dAt0() + Rope.prefixSum(s1, k);
		}
	}

	/**
	 * 定数 c を関数全体に加算する: f(x) = f(x) + c。
	 * O(1)
	 *
	 * @param c 加算する定数
	 */
	// 未テスト
	public void addConstant(long c) {
		f0 += c;
	}

	/**
	 * 一次式 a * x + b を関数全体に加算する: f(x) = f(x) + a * x + b。
	 * O(1)
	 *
	 * @param a 傾き
	 * @param b 切片
	 */
	// 未テスト
	public void addLinear(long a, long b) {
		f0 += a * left + b;
		d0 = Rope.addAll(d0, a);
		d1 = Rope.addAll(d1, a);
		s0 = Rope.addAll(s0, 2L * a);
		s1 = Rope.addAll(s1, 2L * a);
	}

	/**
	 * 非負定数 c を関数全体に乗算する: f(x) = f(x) * c。
	 * O(1)
	 *
	 * @param c 乗算する非負定数
	 */
	// 未テスト
	public void scale(long c) {
		if (c < 0) throw new IllegalArgumentException("c must be nonnegative");
		f0 *= c;
		d0 = Rope.scale(d0, c);
		d1 = Rope.scale(d1, c);
		s0 = Rope.scale(s0, c);
		s1 = Rope.scale(s1, c);
	}

	/**
	 * 関数全体を dx だけ平行移動する: g(x) = f(x - dx)。
	 * O(1)
	 *
	 * @param dx 移動量
	 */
	// 未テスト
	public void shift(long dx) {
		left += dx;
		right += dx;
	}

	/**
	 * 定義域を [l, r] との共通部分に制限する。
	 * O(log N)
	 *
	 * @param l 制限後の左端
	 * @param r 制限後の右端
	 */
	// 未テスト
	public void restrict(long l, long r) {
		long nl = Math.max(left, l);
		long nr = Math.min(right, r);
		if (nl > nr) throw new IllegalArgumentException("empty restriction is not supported");
		if (nl == left && nr == right) return;

		int a = Math.toIntExact(nl - left);
		int b = Math.toIntExact(nr - left);
		long nf0 = valueAt(nl);

		Rope od0 = d0, od1 = d1, os0 = s0, os1 = s1;
		Rope[] od = {od0, od1};
		Rope[] os = {os0, os1};
		Rope[] nd = new Rope[2];
		Rope[] ns = new Rope[2];

		for (int q = 0; q < 2; q++) {
			int p = q ^ (a & 1);
			nd[q] = sliceFullParity(od[p], p, a, b);
		}
		int send = Math.max(a, b - 1);
		for (int q = 0; q < 2; q++) {
			int p = q ^ (a & 1);
			ns[q] = sliceFullParity(os[p], p, a, send);
		}

		left = nl;
		right = nr;
		f0 = nf0;
		d0 = nd[0]; d1 = nd[1]; s0 = ns[0]; s1 = ns[1];
	}

	/**
	 * 右ヒンジ関数 w * max(0, x - a) を加算する (w >= 0)。
	 * O(log N)
	 *
	 * @param a 屈曲点
	 * @param w 重み (非負)
	 */
	// 未テスト
	public void addRightHinge(long a, long w) {
		if (w < 0) throw new IllegalArgumentException("w must be nonnegative");
		if (w == 0) return;
		int n = size();
		f0 += w * Math.max(0L, left - a);
		long tt = a - left;
		int t = tt <= Integer.MIN_VALUE ? Integer.MIN_VALUE
				: tt >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) tt;

		d0 = addRangeByFullIndex(d0, 0, t, n - 1, w);
		d1 = addRangeByFullIndex(d1, 1, t, n - 1, w);

		s0 = addRangeByFullIndex(s0, 0, t, n - 2, 2L * w);
		s1 = addRangeByFullIndex(s1, 1, t, n - 2, 2L * w);
		int z = t - 1;
		s0 = addRangeByFullIndex(s0, 0, z, z + 1, w);
		s1 = addRangeByFullIndex(s1, 1, z, z + 1, w);
	}

	/**
	 * 左ヒンジ関数 w * max(0, a - x) を加算する (w >= 0)。
	 * O(log N)
	 *
	 * @param a 屈曲点
	 * @param w 重み (非負)
	 */
	// 未テスト
	public void addLeftHinge(long a, long w) {
		if (w < 0) throw new IllegalArgumentException("w must be nonnegative");
		if (w == 0) return;
		int n = size();
		f0 += w * Math.max(0L, a - left);
		long tt = a - left;
		int t = tt <= Integer.MIN_VALUE ? Integer.MIN_VALUE
				: tt >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) tt;

		d0 = addRangeByFullIndex(d0, 0, 0, t, -w);
		d1 = addRangeByFullIndex(d1, 1, 0, t, -w);

		s0 = addRangeByFullIndex(s0, 0, 0, t - 1, -2L * w);
		s1 = addRangeByFullIndex(s1, 1, 0, t - 1, -2L * w);
		int z = t - 1;
		s0 = addRangeByFullIndex(s0, 0, z, z + 1, -w);
		s1 = addRangeByFullIndex(s1, 1, z, z + 1, -w);
	}

	/**
	 * 絶対値関数 w * |x - a| を加算する (w >= 0)。
	 * O(log N)
	 *
	 * @param a 屈曲点
	 * @param w 重み (非負)
	 */
	// 未テスト
	public void addAbs(long a, long w) {
		addLeftHinge(a, w);
		addRightHinge(a, w);
	}

	/**
	 * 3 点 カーネルとの min-plus convolution を行う:
	 * g(x) = min(f(x-shift) + c0, f(x-shift-1) + c1, f(x-shift-2) + c2)。
	 * 制限後の定義域は [left + shift, right + shift + 2] となる。
	 * O(log N)
	 *
	 * @param shift 平行移動量
	 * @param c0    j=0 のコスト
	 * @param c1    j=1 のコスト
	 * @param c2    j=2 のコスト
	 */
	// 未テスト
	public void relax3(long shift, long c0, long c1, long c2) {
		final int n = size();
		final Rope[] D = {d0, d1};
		final Rope[] S = {s0, s1};
		final long u = c1 - c0;
		final long v = c2 - c1;
		final long w = c2 - c0;

		Cuts[] cut = {
				makeCuts(0, n, D, S, u, v, w),
				makeCuts(1, n, D, S, u, v, w)
		};

		Rope[] nd = new Rope[2];
		Rope[] ns = new Rope[2];
		for (int p = 0; p < 2; p++) {
			nd[p] = buildNewDParity(p, n, cut, D, S, u, v, w);
			ns[p] = buildNewSParity(p, n, cut, D, S, u, v, w);
		}

		f0 += c0;
		left += shift;
		right += shift + 2;
		d0 = nd[0]; d1 = nd[1]; s0 = ns[0]; s1 = ns[1];

		int nn = n + 2;
		if (Rope.size(d0) != countParityLength(nn - 1, 0)
				|| Rope.size(d1) != countParityLength(nn - 1, 1)
				|| Rope.size(s0) != countParityLength(nn - 2, 0)
				|| Rope.size(s1) != countParityLength(nn - 2, 1)) {
			throw new AssertionError("internal length mismatch after relax3");
		}
	}

	/**
	 * 1 ステップ遷移の min-plus convolution を行う:
	 * g(x) = min(f(x+1) + leftCost, f(x) + stayCost, f(x-1) + rightCost)。
	 * O(log N)
	 *
	 * @param leftCost  左遷移コスト
	 * @param stayCost  滞留コスト
	 * @param rightCost 右遷移コスト
	 */
	// 未テスト
	public void relax(long leftCost, long stayCost, long rightCost) {
		relax3(-1, leftCost, stayCost, rightCost);
	}

	/**
	 * 定義域内での最小値 min_(left <= x <= right) f(x) を取得する。
	 * O(log N)
	 *
	 * @return 最小関数値
	 */
	// 未テスト
	public long minValue() {
		return valueAt(argmin());
	}

	/**
	 * 最小値をとる最小の x 座標 argmin_(x) f(x) を取得する。
	 * O(log N)
	 *
	 * @return 最小値をとる最小の x 座標
	 */
	// 未テスト
	public long argmin() {
		int n = size();
		int evenCount = countParityLength(n, 0);
		int oddCount = countParityLength(n, 1);

		int ke = evenCount == 0 ? -1 : Math.min(Rope.lowerBound(s0, 0), evenCount - 1);
		long xe = ke < 0 ? Long.MAX_VALUE : left + 2L * ke;
		long ve = ke < 0 ? Long.MAX_VALUE : f0 + Rope.prefixSum(s0, ke);

		if (oddCount == 0) return xe;
		int ko = Math.min(Rope.lowerBound(s1, 0), oddCount - 1);
		long xo = left + 1L + 2L * ko;
		long vo = f0 + dAt0() + Rope.prefixSum(s1, ko);

		if (ve != vo) return ve < vo ? xe : xo;
		return Math.min(xe, xo);
	}

	/**
	 * 定義域内のすべての点における関数値を配列として展開して返す。
	 * O(N log N)
	 *
	 * @return 全関数値の配列
	 */
	// 未テスト
	public long[] toArray() {
		int n = size();
		long[] a = new long[n];
		for (int i = 0; i < n; i++) a[i] = valueAt(left + i);
		return a;
	}

	/**
	 * 内部の Rope 構造と定義域・階差の整合性を検査する。
	 * O(N log N)
	 *
	 * @return 内部状態が正常であれば true
	 */
	// 未テスト
	public boolean checkInternal() {
		long[] a = toArray();
		long[] d = new long[Math.max(0, a.length - 1)];
		for (int i = 0; i < d.length; i++) d[i] = a[i + 1] - a[i];
		for (int i = 0; i + 2 < d.length; i++) if (d[i] > d[i + 2]) return false;
		long[] s = new long[Math.max(0, a.length - 2)];
		for (int i = 0; i < s.length; i++) s[i] = a[i + 2] - a[i];
		for (int i = 0; i + 1 < s.length; i++) if (s[i] > s[i + 1]) return false;
		return Arrays.equals(takeParity(d, 0), Rope.toArray(d0))
				&& Arrays.equals(takeParity(d, 1), Rope.toArray(d1))
				&& Arrays.equals(takeParity(s, 0), Rope.toArray(s0))
				&& Arrays.equals(takeParity(s, 1), Rope.toArray(s1));
	}

	/**
	 * relax3 において各パリティ上で最適な選択肢 j が変化する境界位置を保持するクラス。
	 */
	private static final class Cuts {
		/**
		 * 対象パリティにおける要素数。
		 */
		final int length;

		/**
		 * 選択肢 1 が存在する区切りかどうかを示すフラグ。
		 */
		final boolean hasMiddle;

		/**
		 * 選択肢が 0 から 1 へ切り替わるインデックス。
		 */
		final int a;

		/**
		 * 選択肢が 1 から 2 へ切り替わるインデックス。
		 */
		final int b;

		/**
		 * 選択肢が 0 から 2 へ直接切り替わるインデックス (hasMiddle = false の場合)。
		 */
		final int z;

		/**
		 * 中間選択肢 1 が存在する場合の Cuts を構築する。
		 * O(1)
		 *
		 * @param length 要素数
		 * @param a      0 -> 1 切り替え点
		 * @param b      1 -> 2 切り替え点
		 */
		// 未テスト
		Cuts(int length, int a, int b) {
			this.length = length;
			this.hasMiddle = true;
			this.a = a; this.b = b; this.z = -1;
		}

		/**
		 * 中間選択肢 1 が跳ばされる場合の Cuts を構築する。
		 * O(1)
		 *
		 * @param length 要素数
		 * @param z      0 -> 2 切り替え点
		 */
		// 未テスト
		Cuts(int length, int z) {
			this.length = length;
			this.hasMiddle = false;
			this.a = this.b = -1; this.z = z;
		}

		/**
		 * インデックス k における最適選択肢 j (0, 1, 2) を取得する。
		 * O(1)
		 *
		 * @param k インデックス
		 * @return 最適選択肢 j
		 */
		// 未テスト
		int at(int k) {
			if (k < 0 || k >= length) throw new IndexOutOfBoundsException("cut index=" + k + ", len=" + length);
			if (hasMiddle) return k < a ? 0 : (k < b ? 1 : 2);
			return k < z ? 0 : 2;
		}

		// k より後にある次の選択肢の境界。
		int next(int k) {
			if (hasMiddle) return k < a ? a : (k < b ? b : length);
			return k < z ? z : length;
		}

	}

	/**
	 * 指定されたパリティに対する Cuts 情報を作成する。
	 * O(log N)
	 *
	 * @param p パリティ (0 または 1)
	 * @param n 現在のサイズ
	 * @param D 1 階差分 Rope 配列
	 * @param S 2 階和差分 Rope 配列
	 * @param u コスト差 u = c1 - c0
	 * @param v コスト差 v = c2 - c1
	 * @param w コスト差 w = c2 - c0
	 * @return 構築された Cuts オブジェクト
	 */
	// 未テスト
	private static Cuts makeCuts(int p, int n, Rope[] D, Rope[] S,
			long u, long v, long w) {
		int m = countParityLength(n + 2, p);

		int t01;
		if (p == 0) t01 = 1 + Rope.upperBound(D[1], u);
		else t01 = Rope.upperBound(D[0], u);
		t01 = Math.min(t01, m);
		if ((n & 1) == p) t01 = Math.min(t01, (n - p) / 2);

		int t12 = Math.min(m, 1 + Rope.upperBound(D[p], v));
		if (((n + 1) & 1) == p) t12 = Math.min(t12, (n + 1 - p) / 2);

		if (t01 <= t12) return new Cuts(m, t01, t12);

		int t02 = Math.min(m, 1 + Rope.upperBound(S[p], w));
		int firstEdgeX = Math.max(n, 2);
		int edge = ceilDivNonnegative(firstEdgeX - p, 2);
		if (0 <= edge && edge < m) t02 = Math.min(t02, edge);

		return new Cuts(m, t02);
	}

	/**
	 * relax3 後における新しいパリティ p の D 列を構築する。
	 * O(log N)
	 *
	 * @param p   パリティ
	 * @param n   サイズ
	 * @param cut Cuts 配列
	 * @param D   D Rope 配列
	 * @param S   S Rope 配列
	 * @param u   u = c1 - c0
	 * @param v   v = c2 - c1
	 * @param w   w = c2 - c0
	 * @return 新しい D の Rope
	 */
	// 未テスト
	private static Rope buildNewDParity(int p, int n, Cuts[] cut,
			Rope[] D, Rope[] S, long u, long v, long w) {
		int len = countParityLength(n + 1, p);
		if (len == 0) return null;
		int offset = (p == 0 ? 0 : 1);
		Cuts ca = cut[p], cb = cut[1 - p];
		Rope.Edit edit = new Rope.Edit();
		Rope base = D[p], middle = null;
		int prefix = 0, suffix = Rope.size(base);
		for (int l = 0; l < len;) {
			int r = Math.min(len, Math.min(ca.next(l), cb.next(l + offset) - offset));
			int a = ca.at(l), b = cb.at(l + offset);
			if (l == 0 && a == 0 && b == 0) prefix = r;
			else if (r == len && a == 2 && b == 2) suffix = l - 1;
			else middle = edit.concat(middle, dExpression(p, a, b, l, r, D, S, u, v, w, edit));
			l = r;
		}
		// 両端の再利用区間が 1 要素だけ重なる場合、その要素を複製する。
		if (prefix > suffix) {
			middle = edit.concat(middle, edit.slice(base, suffix, prefix));
			suffix = prefix;
		}
		return edit.replace(base, prefix, suffix, middle);
	}

	/**
	 * 区間 [l, r) 上での新しい D 列の代数表現を Rope として返す。
	 * O(log N)
	 *
	 * @param p パリティ
	 * @param a 左側選択肢
	 * @param b 右側選択肢
	 * @param l 左端
	 * @param r 右端
	 * @param D D Rope 配列
	 * @param S S Rope 配列
	 * @param u u
	 * @param v v
	 * @param w w
	 * @return 代数表現 Rope
	 */
	// 未テスト
	private static Rope dExpression(int p, int a, int b, int l, int r,
			Rope[] D, Rope[] S, long u, long v, long w, Rope.Edit edit) {
		int len = r - l;
		int crossOffset = (p == 0 ? -1 : 0);
		if (a == 0 && b == 0) {
			return edit.slice(D[p], l, r);
		}
		if (a == 0 && b == 1) {
			return Rope.run(u, len);
		}
		if (a == 0 && b == 2) {
			if (len != 1) throw new AssertionError("(Jx,Jx+1)=(0,2) persisted for " + len + " points");
			int idx = l + crossOffset;
			long z = w - Rope.get(D[1 - p], idx);
			return Rope.run(z, 1);
		}
		if (a == 1 && b == 0) {
			return edit.addAll(edit.slice(S[1 - p], l + crossOffset, r + crossOffset), -u);
		}
		if (a == 1 && b == 1) {
			return edit.slice(D[1 - p], l + crossOffset, r + crossOffset);
		}
		if (a == 1 && b == 2) {
			return Rope.run(v, len);
		}
		if (a == 2 && b == 0) {
			throw new AssertionError("(Jx,Jx+1)=(2,0) is impossible");
		}
		if (a == 2 && b == 1) {
			return edit.addAll(edit.slice(S[p], l - 1, r - 1), -v);
		}
		if (a == 2 && b == 2) {
			return edit.slice(D[p], l - 1, r - 1);
		}
		throw new AssertionError();
	}

	/**
	 * relax3 後における新しいパリティ p の S 列を構築する。
	 * O(log N)
	 *
	 * @param p   パリティ
	 * @param n   サイズ
	 * @param cut Cuts 配列
	 * @param D   D Rope 配列
	 * @param S   S Rope 配列
	 * @param u   u
	 * @param v   v
	 * @param w   w
	 * @return 新しい S の Rope
	 */
	// 未テスト
	private static Rope buildNewSParity(int p, int n, Cuts[] cut,
			Rope[] D, Rope[] S, long u, long v, long w) {
		int len = countParityLength(n , p);
		if (len == 0) return null;
		int offset = 1;
		Cuts ca = cut[p], cb = cut[p];
		Rope.Edit edit = new Rope.Edit();
		Rope base = S[p], middle = null;
		int prefix = 0, suffix = Rope.size(base);
		for (int l = 0; l < len;) {
			int r = Math.min(len, Math.min(ca.next(l), cb.next(l + offset) - offset));
			int a = ca.at(l), b = cb.at(l + offset);
			if (l == 0 && a == 0 && b == 0) prefix = r;
			else if (r == len && a == 2 && b == 2) suffix = l - 1;
			else middle = edit.concat(middle, sExpression(p, a, b, l, r, D, S, u, v, w, edit));
			l = r;
		}
		// 両端の再利用区間が 1 要素だけ重なる場合、その要素を複製する。
		if (prefix > suffix) {
			middle = edit.concat(middle, edit.slice(base, suffix, prefix));
			suffix = prefix;
		}
		return edit.replace(base, prefix, suffix, middle);
	}

	/**
	 * 区間 [l, r) 上での新しい S 列の代数表現を Rope として返す。
	 * O(log N)
	 *
	 * @param p パリティ
	 * @param a 左側選択肢
	 * @param b 右側選択肢
	 * @param l 左端
	 * @param r 右端
	 * @param D D Rope 配列
	 * @param S S Rope 配列
	 * @param u u
	 * @param v v
	 * @param w w
	 * @return 代数表現 Rope
	 */
	// 未テスト
	private static Rope sExpression(int p, int a, int b, int l, int r,
			Rope[] D, Rope[] S, long u, long v, long w, Rope.Edit edit) {
		int len = r - l;
		int crossOffset = (p == 0 ? -1 : 0);
		if (a == 0 && b == 0) {
			return edit.slice(S[p], l, r);
		}
		if (a == 0 && b == 1) {
			return edit.addAll(edit.slice(D[p], l, r), u);
		}
		if (a == 0 && b == 2) {
			return Rope.run(w, len);
		}
		if (a == 1 && b == 1) {
			return edit.slice(S[1 - p], l + crossOffset, r + crossOffset);
		}
		if (a == 1 && b == 2) {
			return edit.addAll(edit.slice(D[1 - p], l + crossOffset, r + crossOffset), v);
		}
		if (a == 2 && b == 2) {
			return edit.slice(S[p], l - 1, r - 1);
		}
		throw new AssertionError("J_x must be nondecreasing on a fixed parity: " + a + " -> " + b);
	}

	/**
	 * 最初の 1 階差分 d_0 の値を取得する。
	 * O(log N)
	 *
	 * @return d_0 の値
	 */
	// 未テスト
	private long dAt0() {
		if (size() < 2) throw new IllegalStateException("d0 does not exist");
		return Rope.first(d0);
	}

	/**
	 * 配列 a からパリティ p の要素を取り出した部分配列を作成する。
	 * O(N)
	 *
	 * @param a 元の配列
	 * @param p パリティ (0 または 1)
	 * @return パリティ p の要素のみからなる配列
	 */
	// 未テスト
	private static long[] takeParity(long[] a, int p) {
		int n = countParityLength(a.length, p);
		long[] b = new long[n];
		for (int k = 0; k < n; k++) b[k] = a[p + 2 * k];
		return b;
	}

	/**
	 * 全体インデックス区間 [start, end) に含まれるパリティ p の要素に対応する Rope 部分木を切り出す。
	 * O(log N)
	 *
	 * @param root  元の Rope
	 * @param p     パリティ
	 * @param start 開始インデックス
	 * @param end   終了インデックス (exclusive)
	 * @return 切り出された Rope
	 */
	// 未テスト
	private static Rope sliceFullParity(Rope root, int p, int start, int end) {
		if (root == null || start >= end) return null;
		int first = start;
		if ((first & 1) != p) first++;
		if (first >= end) return null;
		int lastExclusive = end;
		int l = (first - p) / 2;
		int cnt = (lastExclusive - 1 - first) / 2 + 1;
		return Rope.slice(root, l, l + cnt);
	}

	/**
	 * 全体インデックス区間 [start, end) に対応するパリティ p の Rope 要素に delta を加算する。
	 * O(log N)
	 *
	 * @param root  元の Rope
	 * @param p     パリティ
	 * @param start 開始インデックス
	 * @param end   終了インデックス (exclusive)
	 * @param delta 加算値
	 * @return 更新後の Rope
	 */
	// 未テスト
	private static Rope addRangeByFullIndex(Rope root, int p, int start, int end, long delta) {
		if (root == null || delta == 0 || start >= end) return root;
		int logicalEnd = p + 2 * Rope.size(root);
		int s = Math.max(start, 0);
		int e = Math.min(end, logicalEnd + 1);
		if (s >= e) return root;
		int first = s;
		if ((first & 1) != p) first++;
		if (first >= e) return root;
		int l = (first - p) / 2;
		int cnt = (e - 1 - first) / 2 + 1;
		int r = Math.min(Rope.size(root), l + cnt);
		if (l < 0) l = 0;
		if (l >= r) return root;
		return Rope.addRange(root, l, r, delta);
	}

	/**
	 * 長さ length のインデックスのうち、パリティ p のものの個数を計算する。
	 * O(1)
	 *
	 * @param length 長さ
	 * @param p      パリティ (0 または 1)
	 * @return パリティ p の要素数
	 */
	// 未テスト
	private static int countParityLength(int length, int p) {
		if (length <= p) return 0;
		return (length - 1 - p) / 2 + 1;
	}

	/**
	 * 非負整数 a / b の切り上げ (ceil) を計算する。
	 * O(1)
	 *
	 * @param a 被除数
	 * @param b 除数
	 * @return ceil(a / b)
	 */
	// 未テスト
	private static int ceilDivNonnegative(int a, int b) {
		if (a <= 0) return 0;
		return (a + b - 1) / b;
	}

	/**
	 * 単調増加 long 数列を保持する永続 AVL Rope 構造。
	 * ラン長圧縮により連続する同一値区間を O(1) で管理する。
	 */
	private static final class Rope {
		private Edit owner;
		static long first(Rope t) {
			long m = 1, a = 0;
			while (!t.isLeaf()) { a += m * t.add; m *= t.mul; t = t.left; }
			return m * t.last + a;
		}

		/**
		 * 1 本の出力木の構築中だけ使う所有権。別の Edit から見たノードは不変。
		 * concat/replace はこの Edit が生成した一時木を消費する。
		 * slice の入力は構築開始前の木に限定し、一時木を複数箇所で共有しない。
		 */
		private static final class Edit {
			Rope node(Rope l, Rope r, Rope reuse) {
				if (l == null) return r;
				if (r == null) return l;
				if (reuse == null || reuse.owner != this) {
					Rope t = new Rope(l, r, 1, 0);
					t.owner = this;
					return t;
				}
				reuse.left = l; reuse.right = r;
				reuse.size = l.size + r.size;
				reuse.height = 1 + Math.max(l.height, r.height);
				reuse.mul = 1; reuse.add = 0;
				reuse.sum = l.sum + r.sum;
				reuse.last = r.last;
				return reuse;
			}

			Rope transform(Rope t, long m, long a) {
				if (t == null || (m == 1 && a == 0)) return t;
				if (t.owner != this) {
					t = Rope.transform(t, m, a);
					t.owner = this;
					return t;
				}
				t.sum = m * t.sum + a * t.size;
				t.last = m * t.last + a;
				if (!t.isLeaf()) { t.mul = m * t.mul; t.add = m * t.add + a; }
				return t;
			}

			Rope addAll(Rope t, long a) { return transform(t, 1, a); }

			Rope concat(Rope a, Rope b) {
				if (a == null) return b;
				if (b == null) return a;
				if (a.height > b.height + 1) {
					Rope l = transform(a.left, a.mul, a.add);
					Rope r = transform(a.right, a.mul, a.add);
					return balance(l, concat(r, b), a);
				}
				if (b.height > a.height + 1) {
					Rope l = transform(b.left, b.mul, b.add);
					Rope r = transform(b.right, b.mul, b.add);
					return balance(concat(a, l), r, b);
				}
				if (a.isLeaf() && b.isLeaf() && a.last == b.last)
					return run(a.last, a.size + b.size);
				return node(a, b, null);
			}

			Rope balance(Rope l, Rope r, Rope reuse) {
				if (l.height > r.height + 1) {
					Rope ll = transform(l.left, l.mul, l.add);
					Rope lr = transform(l.right, l.mul, l.add);
					if (ll.height >= lr.height)
						return node(ll, node(lr, r, l), reuse);
					Rope ql = transform(lr.left, lr.mul, lr.add);
					Rope qr = transform(lr.right, lr.mul, lr.add);
					return node(node(ll, ql, l), node(qr, r, lr), reuse);
				}
				if (r.height > l.height + 1) {
					Rope rl = transform(r.left, r.mul, r.add);
					Rope rr = transform(r.right, r.mul, r.add);
					if (rr.height >= rl.height)
						return node(node(l, rl, r), rr, reuse);
					Rope ql = transform(rl.left, rl.mul, rl.add);
					Rope qr = transform(rl.right, rl.mul, rl.add);
					return node(node(l, ql, rl), node(qr, rr, r), reuse);
				}
				return node(l, r, reuse);
			}


			Rope replace(Rope t, int l, int r, Rope middle) {
				if (l == 0 && r == size(t)) return middle;
				if (t.isLeaf()) return concat(concat(run(t.last, l), middle), run(t.last, t.size - r));
				Rope a = transform(t.left, t.mul, t.add);
				Rope b = transform(t.right, t.mul, t.add);
				int n = a.size;
				if (r <= n) return concat(replace(a, l, r, middle), b);
				if (l >= n) return concat(a, replace(b, l - n, r - n, middle));
				return appendPrefix(a, l, prependSuffix(middle, b, r - n));
			}

			Rope appendPrefix(Rope t, int k, Rope b) {
				if (k == 0) return b;
				if (k == t.size) return concat(t, b);
				if (t.isLeaf()) return concat(run(t.last, k), b);
				Rope l = transform(t.left, t.mul, t.add);
				Rope r = transform(t.right, t.mul, t.add);
				if (k <= l.size) return appendPrefix(l, k, b);
				return concat(l, appendPrefix(r, k - l.size, b));
			}

			Rope prependSuffix(Rope a, Rope t, int k) {
				if (k == t.size) return a;
				if (k == 0) return concat(a, t);
				if (t.isLeaf()) return concat(a, run(t.last, t.size - k));
				Rope l = transform(t.left, t.mul, t.add);
				Rope r = transform(t.right, t.mul, t.add);
				if (k >= l.size) return prependSuffix(a, r, k - l.size);
				return concat(prependSuffix(a, l, k), r);
			}

			Rope slice(Rope t, int l, int r) {
				if (l < 0 || r < l || r > size(t)) throw new IndexOutOfBoundsException();
				if (l == r) return null;
				if (l == 0 && r == t.size) return t;
				if (t.isLeaf()) return run(t.last, r - l);
				int n = t.left.size;
				Rope out;
				if (r <= n) out = slice(t.left, l, r);
				else if (l >= n) out = slice(t.right, l - n, r - n);
				else out = concat(slice(t.left, l, n), slice(t.right, 0, r - n));
				return transform(out, t.mul, t.add);
			}
		}

		/**
		 * 左部分木 Rope (葉ノードの場合は null)。
		 */
		Rope left;

		/**
		 * 右部分木 Rope (葉ノードの場合は null)。
		 */
		Rope right;

		/**
		 * この Rope が表す数列の長要素数。
		 */
		int size;

		/**
		 * 部分木の高さ。
		 */
		int height;


		/**
		 * 遅延評価用一次変換の乗数 mul。
		 */
		long mul;

		/**
		 * 遅延評価用一次変換の加数 add。
		 */
		long add;

		/**
		 * この Rope が表す数列の総和。
		 */
		long sum;


		/**
		 * この Rope が表す数列の末尾要素の値。
		 */
		long last;

		/**
		 * 葉ノード (同値ラン) を構築するプライベートコンストラクタ。
		 * O(1)
		 *
		 * @param value  ランの値
		 * @param length ランの長さ (1 以上)
		 */
		// 未テスト
		private Rope(long value, int length) {
			if (length <= 0) throw new IllegalArgumentException();
			left = right = null;
			size = length;
			height = 1;
			mul = 1;
			add = 0;
			sum = value * length;
			last = value;
		}

		/**
		 * 内部ノードを構築するプライベートコンストラクタ。
		 * O(1)
		 *
		 * @param left  左部分木
		 * @param right 右部分木
		 * @param mul   一次変換乗数
		 * @param add   一次変換加数
		 */
		// 未テスト
		private Rope(Rope left, Rope right, long mul, long add) {
			if (left == null || right == null) throw new IllegalArgumentException();
			this.left = left;
			this.right = right;
			size = left.size + right.size;
			height = 1 + Math.max(left.height, right.height);
			this.mul = mul;
			this.add = add;
			long baseSum = left.sum + right.sum;
			sum = mul * baseSum + add * size;
			last = mul * right.last + add;
		}

		/**
		 * 葉ノードかどうかを判定する。
		 * O(1)
		 *
		 * @return 葉ノードなら true
		 */
		// 未テスト
		boolean isLeaf() {
			return left == null;
		}

		/**
		 * Rope の要素数を取得する (t が null なら 0)。
		 * O(1)
		 *
		 * @param t Rope
		 * @return 要素数
		 */
		// 未テスト
		static int size(Rope t) {
			return t == null ? 0 : t.size;
		}

		/**
		 * Rope の高さを取得する (t が null なら 0)。
		 * O(1)
		 *
		 * @param t Rope
		 * @return 高さ
		 */
		// 未テスト
		static int height(Rope t) {
			return t == null ? 0 : t.height;
		}

		/**
		 * 長さ length の同値ラン Rope を構築する (length = 0 なら null)。
		 * O(1)
		 *
		 * @param value  値
		 * @param length 長さ
		 * @return 構築された Rope
		 */
		// 未テスト
		static Rope run(long value, int length) {
			return length == 0 ? null : new Rope(value, length);
		}

		/**
		 * 配列 a から Rope を構築する。
		 * O(N)
		 *
		 * @param a 単調増加配列
		 * @return 構築された Rope
		 */
		// 未テスト
		static Rope build(long[] a) {
			if (a.length == 0) return null;
			List<Rope> runs = new ArrayList<>();
			int i = 0;
			while (i < a.length) {
				int j = i + 1;
				while (j < a.length && a[j] == a[i]) j++;
				runs.add(run(a[i], j - i));
				i = j;
			}
			return buildRuns(runs, 0, runs.size());
		}

		/**
		 * ランのリストから平衡二分木 Rope を構築する。
		 * O(M)
		 *
		 * @param a ランリスト
		 * @param l 左端
		 * @param r 右端
		 * @return 構築された Rope
		 */
		// 未テスト
		private static Rope buildRuns(List<Rope> a, int l, int r) {
			if (l >= r) return null;
			if (l + 1 == r) return a.get(l);
			int m = (l + r) >>> 1;
			return raw(buildRuns(a, l, m), buildRuns(a, m, r));
		}

		/**
		 * Rope の各要素に delta を加算した新しい Rope を返す。
		 * O(1)
		 *
		 * @param t     元の Rope
		 * @param delta 加算値
		 * @return 加算後の Rope
		 */
		// 未テスト
		static Rope addAll(Rope t, long delta) {
			return transform(t, 1, delta);
		}

		/**
		 * Rope の各要素に非負定数 c を乗算した新しい Rope を返す。
		 * O(1)
		 *
		 * @param t 元の Rope
		 * @param c 乗算値 (非負)
		 * @return 乗算後の Rope
		 */
		// 未テスト
		static Rope scale(Rope t, long c) {
			if (c < 0) throw new IllegalArgumentException();
			return transform(t, c, 0);
		}

		/**
		 * Rope の各要素に一次変換 mul * x + add を適用した新しい Rope を返す。
		 * O(1)
		 *
		 * @param t   元の Rope
		 * @param mul 乗数
		 * @param add 加数
		 * @return 変換後の Rope
		 */
		// 未テスト
		private static Rope transform(Rope t, long mul, long add) {
			if (t == null || (mul == 1 && add == 0)) return t;
			if (mul == 0) return run(add, t.size);
			if (t.isLeaf()) return run(mul * t.last + add, t.size);
			return new Rope(t.left, t.right, mul * t.mul, mul * t.add + add);
		}

		/**
		 * Rope の左右子ノードのペアを表すレコード風クラス。
		 */
		private static final class Pair {
			/**
			 * 評価済み左子ノード。
			 */
			final Rope l;

			/**
			 * 評価済み右子ノード。
			 */
			final Rope r;

			/**
			 * Pair を構築する。
			 * O(1)
			 *
			 * @param l 左子ノード
			 * @param r 右子ノード
			 */
			// 未テスト
			Pair(Rope l, Rope r) { this.l = l; this.r = r; }
		}

		/**
		 * 遅延評価タグを子ノードへ伝播させた左右の子 Rope ペアを取得する。
		 * O(1)
		 *
		 * @param t 対象の Rope
		 * @return 評価済み左右の子ノードペア
		 */
		// 未テスト
		private static Pair children(Rope t) {
			if (t == null || t.isLeaf()) throw new IllegalArgumentException();
			Rope l = t.left, r = t.right;
			if (t.mul != 1 || t.add != 0) {
				l = transform(l, t.mul, t.add);
				r = transform(r, t.mul, t.add);
			}
			return new Pair(l, r);
		}

		/**
		 * 2 つの Rope を直接結合した内部ノードを作成する。
		 * O(1)
		 *
		 * @param a 左 Rope
		 * @param b 右 Rope
		 * @return 結合 Rope
		 */
		// 未テスト
		private static Rope raw(Rope a, Rope b) {
			if (a == null) return b;
			if (b == null) return a;
			return new Rope(a, b, 1, 0);
		}

		/**
		 * 単調性を維持しつつ 2 つの Rope を結合する (a.last <= first(b) が必要)。
		 * O(log N)
		 *
		 * @param a 左 Rope
		 * @param b 右 Rope
		 * @return 結合後の Rope
		 */
		// 未テスト
		static Rope concatSorted(Rope a, Rope b) {
			if (a == null) return b;
			if (b == null) return a;
			if (a.isLeaf() && b.isLeaf() && a.last == b.last) {
				return run(a.last, a.size + b.size);
			}
			return concat(a, b);
		}

		/**
		 * AVL 木の高さバランスを維持しながら 2 つの Rope を結合する。
		 * O(log N)
		 *
		 * @param a 左 Rope
		 * @param b 右 Rope
		 * @return 結合・再平衡後の Rope
		 */
		// 未テスト
		private static Rope concat(Rope a, Rope b) {
			if (a == null) return b;
			if (b == null) return a;
			if (height(a) > height(b) + 1) {
				Pair ac = children(a);
				return balance(ac.l, concat(ac.r, b));
			}
			if (height(b) > height(a) + 1) {
				Pair bc = children(b);
				return balance(concat(a, bc.l), bc.r);
			}
			return raw(a, b);
		}

		/**
		 * AVL 木の回転による平衡化を行う。
		 * O(1)
		 *
		 * @param l 左部分木
		 * @param r 右部分木
		 * @return 平衡化後の Rope
		 */
		// 未テスト
		private static Rope balance(Rope l, Rope r) {
			int hl = height(l), hr = height(r);
			if (hl > hr + 1) {
				Pair lc = children(l);
				if (height(lc.l) >= height(lc.r)) {
					return raw(lc.l, raw(lc.r, r));
				} else {
					Pair q = children(lc.r);
					return raw(raw(lc.l, q.l), raw(q.r, r));
				}
			}
			if (hr > hl + 1) {
				Pair rc = children(r);
				if (height(rc.r) >= height(rc.l)) {
					return raw(raw(l, rc.l), rc.r);
				} else {
					Pair q = children(rc.l);
					return raw(raw(l, q.l), raw(q.r, rc.r));
				}
			}
			return raw(l, r);
		}

		// 区間加算。境界を含む枝だけをコピーし、完全に含まれる部分木は遅延タグで更新する。
		static Rope addRange(Rope t, int l, int r, long delta) {
			if (l == r || delta == 0) return t;
			if (l == 0 && r == t.size) return addAll(t, delta);
			if (t.isLeaf()) {
				return concatSorted(concatSorted(run(t.last, l),
						run(t.last + delta, r - l)), run(t.last, t.size - r));
			}
			Pair c = children(t);
			int n = c.l.size;
			Rope a = l < n ? addRange(c.l, l, Math.min(r, n), delta) : c.l;
			Rope b = r > n ? addRange(c.r, Math.max(0, l - n), r - n, delta) : c.r;
			return concatSorted(a, b);
		}

		/**
		 * インデックス区間 [l, r) の部分 Rope を切り出す。
		 * O(log N)
		 *
		 * @param t 元の Rope
		 * @param l 開始インデックス
		 * @param r 終了インデックス (exclusive)
		 * @return 切り出された Rope
		 */
		// 未テスト
		static Rope slice(Rope t, int l, int r) {
			if (l < 0 || r < l || r > size(t)) {
				throw new IndexOutOfBoundsException("slice [" + l + "," + r + ") / " + size(t));
			}
			if (l == r) return null;
			if (l == 0 && r == t.size) return t;
			if (t.isLeaf()) return run(t.last, r - l);
			int ls = t.left.size;
			Rope out;
			if (r <= ls) out = slice(t.left, l, r);
			else if (l >= ls) out = slice(t.right, l - ls, r - ls);
			else out = concatSorted(slice(t.left, l, ls), slice(t.right, 0, r - ls));
			return transform(out, t.mul, t.add);
		}

		/**
		 * インデックス i における要素値を取得する。
		 * O(log N)
		 *
		 * @param t Rope
		 * @param i インデックス
		 * @return i 番目の要素値
		 */
		// 未テスト
		static long get(Rope t, int i) {
			if (t == null || i < 0 || i >= t.size) throw new IndexOutOfBoundsException();
			long m = 1, a = 0;
			while (!t.isLeaf()) {
				a += m * t.add; m *= t.mul;
				if (i < t.left.size) t = t.left;
				else { i -= t.left.size; t = t.right; }
			}
			return m * t.last + a;
		}

		/**
		 * 値が x 以上となる最初のインデックスを取得する (二分探索)。
		 * O(log N)
		 *
		 * @param t Rope
		 * @param x 探索値
		 * @return 該当する最小インデックス
		 */
		// 未テスト
		static int lowerBound(Rope t, long x) {
			if (t == null) return 0;
			if (t.last < x) return t.size;
			long m = 1, a = 0;
			int at = 0;
			while (!t.isLeaf()) {
				a += m * t.add; m *= t.mul;
				if (m * t.left.last + a >= x) t = t.left;
				else { at += t.left.size; t = t.right; }
			}
			return at;
		}

		/**
		 * 値が x より大きくなる最初のインデックスを取得する (二分探索)。
		 * O(log N)
		 *
		 * @param t Rope
		 * @param x 探索値
		 * @return 該当する最小インデックス
		 */
		// 未テスト
		static int upperBound(Rope t, long x) {
			if (t == null) return 0;
			if (t.last <= x) return t.size;
			long m = 1, a = 0;
			int at = 0;
			while (!t.isLeaf()) {
				a += m * t.add; m *= t.mul;
				if (m * t.left.last + a > x) t = t.left;
				else { at += t.left.size; t = t.right; }
			}
			return at;
		}

		/**
		 * 先頭 k 個の要素の総和 sum_(j=0)^(k-1) a_j を計算する。
		 * O(log N)
		 *
		 * @param t Rope
		 * @param k 要素数 (0 <= k <= t.size)
		 * @return 先頭 k 個の総和
		 */
		// 未テスト
		static long prefixSum(Rope t, int k) {
			if (k < 0 || k > size(t)) throw new IndexOutOfBoundsException();
			long ans = 0, m = 1, a = 0;
			while (k != 0) {
				if (k == t.size) return ans + m * t.sum + a * k;
				if (t.isLeaf()) return ans + (m * t.last + a) * k;
				a += m * t.add; m *= t.mul;
				if (k <= t.left.size) t = t.left;
				else {
					ans += m * t.left.sum + a * t.left.size;
					k -= t.left.size; t = t.right;
				}
			}
			return ans;
		}

		/**
		 * Rope 内の全要素を配列に展開して返す。
		 * O(N)
		 *
		 * @param t Rope
		 * @return 全要素の配列
		 */
		// 未テスト
		static long[] toArray(Rope t) {
			long[] a = new long[size(t)];
			fill(t, a, 0);
			return a;
		}

		/**
		 * 配列 a の指定位置 at から Rope の全要素を順次格納する。
		 * O(N)
		 *
		 * @param t  Rope
		 * @param a  格納先配列
		 * @param at 格納開始位置
		 * @return 書き込み完了後の次の位置
		 */
		// 未テスト
		private static int fill(Rope t, long[] a, int at) {
			if (t == null) return at;
			if (t.isLeaf()) {
				Arrays.fill(a, at, at + t.size, t.last);
				return at + t.size;
			}
			Pair c = children(t);
			at = fill(c.l, a, at);
			return fill(c.r, a, at);
		}
	}
}
