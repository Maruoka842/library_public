package library.util;

/**
 * Stern-Brocot 木のノード。
 *
 * <p>根は {@code 1/1}。
 *
 * <p>各ノードは、その部分木に対応する区間
 *
 * <pre>
 * (lowerBound(), upperBound())
 * </pre>
 *
 * を保持する。左右端を
 *
 * <pre>
 * L = ln / ld
 * R = rn / rd
 * </pre>
 *
 * とすると、現在のノードは
 *
 * <pre>
 * (ln + rn) / (ld + rd)
 * </pre>
 *
 * である。
 *
 * <p>常に
 *
 * <pre>
 * rn * ld - ln * rd = 1
 * </pre>
 *
 * が成り立つ。
 *
 * <p>移動履歴は保持しない。
 * 親方向への移動は左右端から復元する。
 *
 * <p>long のオーバーフローは検査しない。
 */
public class SternBrocotTreeNode {

	private long ln = 0;
	private long ld = 1;

	private long rn = 1;
	private long rd = 0;

	private long depth = 0;

	/**
	 * 根 {@code 1/1} を生成する。
	 */
	public SternBrocotTreeNode() {}

	/**
	 * 指定された正の有理数に対応するノードを生成する。
	 *
	 * @param numerator 分子
	 * @param denominator 分母
	 */
	public SternBrocotTreeNode(long numerator, long denominator) {
		if (numerator <= 0 || denominator <= 0)
			throw new IllegalArgumentException();

		long g = MathUtils.gcd(numerator, denominator);
		long p = numerator / g;
		long q = denominator / g;

		while (p != q) {
			if (p < q) {
				long k = (q - 1) / p;
				goLeft(k);
				q -= k * p;
			} else {
				long k = (p - 1) / q;
				goRight(k);
				p -= k * q;
			}
		}
	}

	/**
	 * {@code numerator / denominator} を含む Stern-Brocot 区間を、
	 * 分母 {@code maxDenominator} の範囲で可能な限り狭める。
	 *
	 * <p>既約化した target 自身の分母が
	 * {@code maxDenominator} 以下なら、
	 * target 自身に対応するノードを返す。
	 *
	 * <p>そうでなければ、返されたノードについて
	 *
	 * <pre>
	 * lowerBound()
	 * upperBound()
	 * </pre>
	 *
	 * はそれぞれ
	 *
	 * <pre>
	 * max { p/q <= target | q <= maxDenominator }
	 * min { p/q >= target | q <= maxDenominator }
	 * </pre>
	 *
	 * となる。
	 *
	 * @param numerator target の分子
	 * @param denominator target の分母
	 * @param maxDenominator 分母の上限
	 * @return target を含む限界まで狭めた Stern-Brocot 区間
	 */
	public static SternBrocotTreeNode boundedInterval(
			long numerator,
			long denominator,
			long maxDenominator) {

		if (numerator <= 0 || denominator <= 0 || maxDenominator <= 0)
			throw new IllegalArgumentException();

		long g = MathUtils.gcd(numerator, denominator);
		long num = numerator / g;
		long den = denominator / g;

		SternBrocotTreeNode node = new SternBrocotTreeNode();

		/*
		 * target のベクトル (den, num) を
		 *
		 *   alpha * L + beta * R
		 *
		 * と表したときの係数。
		 *
		 * 根では
		 *
		 *   L = (1, 0)
		 *   R = (0, 1)
		 *
		 * なので alpha = den, beta = num。
		 *
		 * goLeft(k)  : alpha -= k * beta
		 * goRight(k) : beta  -= k * alpha
		 *
		 * となり、Euclid の互除法そのものになる。
		 */
		long alpha = den;
		long beta = num;

		while (true) {
			long mediantDenominator = node.ld + node.rd;

			// これ以上内部に分母制限内の分数は存在しない。
			if (mediantDenominator > maxDenominator)
				return node;

			// target 自身が現在の mediant。
			if (alpha == beta)
				return node;

			if (alpha > beta) {
				/*
				 * target < mediant
				 * → 左へ。
				 *
				 * target を追い越さない最大回数。
				 */
				long byTarget = (alpha - 1) / beta;

				/*
				 * R.denominator <= maxDenominator
				 * を保てる最大回数。
				 */
				long byDenominator = node.ld == 0
						? Long.MAX_VALUE
						: (maxDenominator - node.rd) / node.ld;

				long k = Math.min(byTarget, byDenominator);

				if (k == 0)
					return node;

				node.goLeft(k);
				alpha -= k * beta;

			} else {
				/*
				 * target > mediant
				 * → 右へ。
				 */
				long byTarget = (beta - 1) / alpha;

				long byDenominator = node.rd == 0
						? Long.MAX_VALUE
						: (maxDenominator - node.ld) / node.rd;

				long k = Math.min(byTarget, byDenominator);

				if (k == 0)
					return node;

				node.goRight(k);
				beta -= k * alpha;
			}
		}
	}

	/**
	 * 現在のノードを返す。
	 */
	public LongFraction get() {
		return new LongFraction(ln + rn, ld + rd);
	}

	/**
	 * 現在の部分木に対応する開区間の左端を返す。
	 *
	 * <p>この端点自身は区間に含まれない。
	 *
	 * @return 現在の部分木に対応する開区間の左端
	 */
	public LongFraction lowerBound() {
		return new LongFraction(ln, ld);
	}

	/**
	 * 現在の部分木に対応する開区間の右端を返す。
	 *
	 * <p>この端点自身は区間に含まれない。
	 *
	 * <p>正の無限大は {@code 1/0} で表す。
	 *
	 * @return 現在の部分木に対応する開区間の右端
	 */
	public LongFraction upperBound() {
		return new LongFraction(rn, rd);
	}

	/**
	 * 根からの深さを返す。
	 */
	public long depth() {
		return depth;
	}

	/**
	 * 根かどうかを返す。
	 */
	public boolean isRoot() {
		return depth == 0;
	}

	/**
	 * このノードと同じノードを表す独立したコピーを返す。
	 *
	 * <p>返り値に対する移動はこのノードに影響せず、このノードに対する移動は
	 * 返り値に影響しない。
	 *
	 * <p>計算量: {@code O(1)} 時間、{@code O(1)} 領域。
	 *
	 * @return このノードのコピー
	 */
	// 未テスト
	public SternBrocotTreeNode copy() {
		SternBrocotTreeNode ret = new SternBrocotTreeNode();
		ret.ln = ln;
		ret.ld = ld;
		ret.rn = rn;
		ret.rd = rd;
		ret.depth = depth;
		return ret;
	}

	/**
	 * このノードと {@code other} の最深共通祖先を返す。
	 *
	 * <p>返り値を根からの移動列で表すと、このノードと {@code other} の
	 * 移動列の最長共通接頭辞に等しい。返り値は新しいノードであり、
	 * このノードと {@code other} は変更されない。
	 *
	 * <p>計算量: {@code p/q = get()}, {@code r/s = other.get()} とすると
	 * {@code O(log(max(p, q, r, s)))} 時間、{@code O(1)} 補助領域。
	 *
	 * @param other もう一方のノード
	 * @return このノードと {@code other} の最深共通祖先
	 */
	// 未テスト
	public SternBrocotTreeNode lca(SternBrocotTreeNode other) {
		LongFraction a = get();
		LongFraction b = other.get();
		long ap = a.numerator();
		long aq = a.denominator();
		long bp = b.numerator();
		long bq = b.denominator();
		SternBrocotTreeNode ret = new SternBrocotTreeNode();

		while (ap != aq && bp != bq) {
			boolean aGoesLeft = ap < aq;
			boolean bGoesLeft = bp < bq;
			if (aGoesLeft != bGoesLeft)
				break;

			long ak = aGoesLeft ? (aq - 1) / ap : (ap - 1) / aq;
			long bk = bGoesLeft ? (bq - 1) / bp : (bp - 1) / bq;
			long k = Math.min(ak, bk);

			if (aGoesLeft) {
				ret.goLeft(k);
				aq -= k * ap;
				bq -= k * bp;
			} else {
				ret.goRight(k);
				ap -= k * aq;
				bp -= k * bq;
			}

			if (ak != bk)
				break;
		}

		return ret;
	}

	public void goLeft() {
		goLeft(1);
	}

	/**
	 * 左方向へ {@code k} 段まとめて進む。
	 *
	 * <pre>
	 * R <- R + k L
	 * </pre>
	 */
	public void goLeft(long k) {
		if (k < 0)
			throw new IllegalArgumentException();

		rn += k * ln;
		rd += k * ld;
		depth += k;
	}

	public void goRight() {
		goRight(1);
	}

	/**
	 * 右方向へ {@code k} 段まとめて進む。
	 *
	 * <pre>
	 * L <- L + k R
	 * </pre>
	 */
	public void goRight(long k) {
		if (k < 0)
			throw new IllegalArgumentException();

		ln += k * rn;
		ld += k * rd;
		depth += k;
	}

	/**
	 * 親へ1段戻る。
	 *
	 * @return 根なら {@code false}
	 */
	public boolean goParent() {
		if (isRoot())
			return false;

		goParent(1);
		return true;
	}

	/**
	 * 親方向へ {@code k} 段まとめて戻る。
	 *
	 * <p>移動履歴は保持せず、左右端から最後の移動方向を復元する。
	 *
	 * @param k 戻る段数
	 */
	public void goParent(long k) {
		if (k < 0 || k > depth)
			throw new IllegalArgumentException();

		long remain = k;

		while (remain > 0) {
			if (ln >= rn && ld >= rd) {
				// 最後の run は right。
				long t = Math.min(
						remain,
						maxSubtract(ln, ld, rn, rd));

				ln -= t * rn;
				ld -= t * rd;
				remain -= t;

			} else {
				// 最後の run は left。
				long t = Math.min(
						remain,
						maxSubtract(rn, rd, ln, ld));

				rn -= t * ln;
				rd -= t * ld;
				remain -= t;
			}
		}

		depth -= k;
	}

	private static long maxSubtract(
			long xn, long xd,
			long yn, long yd) {

		long k = Long.MAX_VALUE;

		if (yn != 0)
			k = Math.min(k, xn / yn);

		if (yd != 0)
			k = Math.min(k, xd / yd);

		return k;
	}
	
	/**
	 * 現在のノードを表す既約分数 {@code numerator/denominator} を返す。
	 *
	 * <p>計算量: {@code O(log(min(numerator, denominator)))} 時間、
	 * {@code O(1)} 補助領域。
	 *
	 * @return 現在のノードの文字列表現
	 */
	@Override
	public String toString() {
		return get().toString();
	}
}
