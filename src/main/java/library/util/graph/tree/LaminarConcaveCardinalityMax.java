package library.util.graph.tree;

import java.util.Arrays;

import library.util.collections.MeldableLazyOrderStatisticMultiset;

public class LaminarConcaveCardinalityMax {

	/**
	 * 
	 * 離散関数の差分列において、差分値が一定である区間を表す。
	 *
	 * <p>
	 * 離散関数 {@code phi} に対し、
	 *
	 * <pre>
	 * 
	 * diff(i) = phi(i + 1) - phi(i)
	 * </pre>
	 *
	 * とする。
	 *
	 * <p>
	 * この区分の開始位置を {@code begin} とすると、
	 *
	 * <pre>
	 * 
	 * begin <= i < endExclusive
	 * </pre>
	 *
	 * を満たすすべての {@code i} について
	 *
	 * <pre>
	 * 
	 * diff(i) = gain
	 * </pre>
	 *
	 * であることを表す。
	 *
	 * <p>
	 * 複数の {@code SlopeSegment} を先頭から並べる場合、 最初の区分は 0 から始まり、それ以降の区分は 直前の区分の
	 * {@code endExclusive} から始まる。
	 * 
	 * @param endExclusive この区分の右端（非包含）
	 * @param gain         この区分における差分値
	 */
	public record SlopeSegment(int endExclusive, long gain) {
	}

	/**
	 * 根付き木上で、選択頂点数ごとの最大値を求める。
	 *
	 * <p>
	 * 頂点 {@code v} の部分木を {@code T_v} とし、 各頂点に離散凹関数
	 *
	 * <pre>
	 * phi_v : {0, 1, ..., |T_v|} -> long
	 * </pre>
	 *
	 * が与えられる。すべての {@code k = 0, 1, ..., N} について
	 *
	 * <pre>
	 * max  sum_v phi_v(|X ∩ T_v|)
	 *
	 * subject to X ⊆ V, |X| = k
	 * </pre>
	 *
	 * を求める。
	 *
	 * <p>
	 * {@code valueAtZero[v]} は {@code phi_v(0)} を表す。 {@code slopes[v]} は差分列
	 *
	 * <pre>
	 * phi_v(i + 1) - phi_v(i)    (0 <= i < |T_v|)
	 * </pre>
	 *
	 * を {@link SlopeSegment} の列で区分一定に表したものである。 {@code phi_v}
	 * が離散凹であるため、この差分列は非増加でなければならない。
	 *
	 * <p>
	 * {@code slopes[v]} は区間 {@code [0, |T_v|)} を 隙間なく覆わなければならない。
	 *
	 * <p>
	 * {@code B} を全頂点の {@link SlopeSegment} 数の総和とすると、 期待計算量は O((N + B) log
	 * N)、補助空間計算量は O(N)。 特に各 {@code phi_v} が O(1) 個の区分で表される場合は期待 O(N log N)。
	 *
	 * @param tree        根付き木。事前に {@link Tree#rooted(int)} が呼ばれている必要がある
	 * @param valueAtZero {@code valueAtZero[v] = phi_v(0)}
	 * @param slopes      各 {@code phi_v} の差分列の区分一定表現
	 * @return 長さ {@code N + 1} の配列。 {@code ret[k]} はちょうど {@code k} 頂点を選択したときの最大値
	 * @throws IllegalArgumentException {@code tree} が根付けされていない場合、 または入力配列の長さや
	 *                                  {@code slopes} が不正な場合
	 */
	public static long[] maximizeByCardinality(Tree tree, long[] valueAtZero, SlopeSegment[][] slopes) {
		//https://atcoder.jp/contests/abc369/submissions/78735034
		if (!tree.isRooted()) {
			throw new IllegalArgumentException("tree must be rooted");
		}

		int[] order = tree.bfsOrder();
		int n = order.length;
		int root = tree.root();

		if (valueAtZero.length != n || slopes.length != n) {
			throw new IllegalArgumentException("length mismatch");
		}

		// DP の差分値を大きい順に持つ。
		var strategy = new MeldableLazyOrderStatisticMultiset.Strategy<Long, Long>() {

			@Override
			public int compare(Long a, Long b) {
				return Long.compare(b, a);
			}

			@Override
			public Long mapping(Long add, Long value) {
				return value + add;
			}

			@Override
			public Long composition(Long f, Long g) {
				return f + g;
			}
		};

		@SuppressWarnings("unchecked")
		MeldableLazyOrderStatisticMultiset<Long, Long>[] dp = (MeldableLazyOrderStatisticMultiset<Long, Long>[]) new MeldableLazyOrderStatisticMultiset<?, ?>[n];

		/*
		 * phi_v(0) は選択集合 X に依存しないので、 全頂点分を共通定数としてまとめておく。
		 */
		long constant = 0;
		for (long x : valueAtZero) {
			constant += x;
		}

		/*
		 * bfsOrder の逆順に処理する。 頂点 v の処理時には、全ての子の DP の差分列が dp[v] に meld 済みになっている。
		 */
		for (int oi = n - 1; oi >= 0; oi--) {
			int v = order[oi];

			SlopeSegment[] segments = slopes[v];
			int subtreeSize = tree.size(v);

			/*
			 * [0, subtreeSize) を覆うまでの Segment だけ使用する。 それより後の Segment はこの頂点では不要なので無視する。
			 *
			 * 例えば subtreeSize = 1 のとき
			 *
			 * (1, 1), (N, 0)
			 *
			 * が与えられても、最初の Segment だけを使用する。
			 */
			int m = 1;
			while (segments[m - 1].endExclusive() < subtreeSize) {
				m++;
			}

			validateSlopes(v, subtreeSize, segments, m);

			MeldableLazyOrderStatisticMultiset<Long, Long> d = dp[v];

			if (d == null) {
				d = new MeldableLazyOrderStatisticMultiset<>(strategy);
			}

			/*
			 * v 自身を選ぶかどうかは、値 0 の差分を1個追加することに対応する。
			 */
			d.add(0L);

			/*
			 * 現在の d は、子部分木と v 自身だけを考えた DP の差分列である。
			 *
			 * その第 k 要素に
			 *
			 * phi_v(k + 1) - phi_v(k)
			 *
			 * を加えることで、T_v 全体の DP の差分列にする。
			 *
			 * slopes[v] はこの加算値を区分一定に表している。
			 */
			long lastGain = segments[m - 1].gain();

			if (lastGain != 0) {
				d.applyAll(lastGain);
			}

			for (int i = m - 2; i >= 0; i--) {
				long add = segments[i].gain() - segments[i + 1].gain();

				if (add != 0) {
					d.applyByRank(0, segments[i].endExclusive(), add);
				}
			}

			dp[v] = d;

			/*
			 * 完成した T_v の DP の差分列を親へ meld する。 親を処理するときには、全ての子の差分列が揃っている。
			 */
			if (v != root) {
				int p = tree.parent(v);

				if (dp[p] == null) {
					dp[p] = new MeldableLazyOrderStatisticMultiset<>(strategy);
				}

				dp[p].meldDestructive(d);
				dp[v] = null;
			}
		}

		/*
		 * 根の DP の差分列から、 選択頂点数ごとの最大値を復元する。
		 */
		long[] ret = new long[n + 1];

		ret[0] = constant;

		long value = constant;
		int k = 0;

		for (var e : dp[root].entries()) {
			for (long c = 0; c < e.multiplicity(); c++) {
				value += e.value();
				ret[++k] = value;
			}
		}

		if (k != n) {
			throw new AssertionError("root slope sequence length is " + k + ", expected " + n);
		}

		return ret;
	}

	private static void validateSlopes(int v, int subtreeSize, SlopeSegment[] segments, int usedSegments) {

		if (segments == null || segments.length == 0) {
			throw new IllegalArgumentException("slopes[" + v + "] is empty");
		}

		int previousEnd = 0;
		long previousGain = 0;

		/*
		 * 実際に使用する Segment だけ検証する。 [0, subtreeSize) を覆った後の Segment は無視する。
		 */
		for (int i = 0; i < usedSegments; i++) {
			SlopeSegment s = segments[i];

			if (s == null) {
				throw new IllegalArgumentException("slopes[" + v + "][" + i + "] is null");
			}

			if (s.endExclusive() <= previousEnd) {
				throw new IllegalArgumentException("segment ends are not strictly increasing at v = " + v);
			}

			if (i > 0 && previousGain < s.gain()) {
				throw new IllegalArgumentException("slopes[" + v + "] is not non-increasing");
			}

			previousEnd = s.endExclusive();
			previousGain = s.gain();
		}

		if (previousEnd < subtreeSize) {
			throw new IllegalArgumentException("slopes[" + v + "] does not cover [0, " + subtreeSize + ")");
		}
	}
}