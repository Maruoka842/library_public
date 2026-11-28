package library.util.graph.tree;

import library.util.collections.MeldableLazyOrderStatisticMultiset;

public class MinCostFlowOnTree {

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
	 * <p>
	 * 最小費用流の言葉では、 区分 {@code [begin, endExclusive)} は
	 *
	 * <pre>
	 * 容量 (endExclusive - begin)、 単位コスト gain
	 * </pre>
	 *
	 * の 1 本の辺に対応する。 凸関数の差分は単位コストの非減少列なので、 区分の列は 同じ 2 頂点間を結ぶ並列辺の集合であり、 流量が増えるにつれて
	 * 単位コストの小さい辺から順に使われる。
	 *
	 * @param endExclusive この区分の右端（非包含）
	 * @param gain         この区分における差分値
	 */
	public record SlopeSegment(int endExclusive, long gain) {
	}

	/**
	 * 根付き木上の最小費用流として、 流量 {@code k} ごとの最小費用を求める。
	 *
	 * <p>
	 * 根付き木の頂点集合を {@code V}、 頂点 {@code v} の部分木を {@code T_v} とする。 次のネットワークを考える。
	 *
	 * <pre>
	 * S    : 始点
	 * T    : 終点
	 *
	 * S    -> v             (v ∈ V)       容量 1、 コスト 0
	 * v    -> parent(v)     (v != root)   流量 f に対するコスト phi_v(f)、 容量 |T_v|
	 * root -> T                           流量 f に対するコスト phi_root(f)、 容量 N
	 * </pre>
	 *
	 * 辺 {@code v -> parent(v)} （根では {@code root -> T}）を {@code e_v} と書く。
	 * 各頂点で流量保存則が成り立つので、 {@code S -> v} に流した頂点の集合を {@code X} とすると、
	 *
	 * <pre>
	 * (e_v の流量) = |X ∩ T_v|
	 * </pre>
	 *
	 * となる。 したがって {@code S} から {@code T} へ流量ちょうど {@code k} を流す最小費用流は、
	 *
	 * <pre>
	 * min  sum_v phi_v(|X ∩ T_v|)
	 * subject to X ⊆ V, |X| = k
	 * </pre>
	 *
	 * に等しい。 これが {@code ret[k]} である。
	 *
	 * <p>
	 * 各辺のコスト関数 {@code phi_v(f)} は離散凸であり、 {@link SlopeSegment} の列として 「容量
	 * {@code endExclusive - begin}、 単位コスト {@code gain} の並列辺」の集合で表される。 そのため上のネットワークは
	 * 線形コストの辺だけを持つ通常の最小費用流に帰着でき、 {@code ret} は {@code k} について凸になる。 本メソッドは流量を 1
	 * ずつ増やす最短路反復を行う代わりに、 木上の DP の差分列 （流量を 1 増やすときの追加コストを小さい順に並べたもの）を 昇順に保ったまま
	 * 子から親へマージして求める。
	 *
	 * <p>
	 * 入力は次の形式で与える。{@code slopes[v]} は {@code e_v} の並列辺、 すなわち差分列
	 *
	 * <pre>
	 * phi_v(i + 1) - phi_v(i)    (0 <= i < |T_v|)
	 * </pre>
	 *
	 * を {@link SlopeSegment} の列で区分一定に表したものである。 {@code phi_v} が離散凸であるため、
	 * この差分列（単位コスト）は非減少でなければならない。
	 *
	 * <p>
	 * {@code slopes[v]} は区間 {@code [0, |T_v|)} （{@code e_v} の流量の範囲）を
	 * 隙間なく覆わなければならない。
	 *
	 * <p>
	 * {@code B} を全頂点の {@link SlopeSegment} 数の総和（並列辺の総数）とすると、 期待計算量は O((N + B) log
	 * N)、補助空間計算量は O(N)。 特に各 {@code phi_v} が O(1) 個の区分で表される場合は期待 O(N log N)。
	 *
	 * @param tree   根付き木。事前に {@link Tree#rooted(int)} が呼ばれている必要がある
	 * @param slopes 各 {@code phi_v} の差分列の区分一定表現
	 * @return 長さ {@code N + 1} の配列。 {@code ret[k]} はちょうど {@code k} 頂点を選択したときの最小値
	 * @throws IllegalArgumentException {@code tree} が根付けされていない場合、 または入力配列の長さや
	 *                                  {@code slopes} が不正な場合
	 */
	public static long[] minCostFlowByCardinality(Tree tree, SlopeSegment[][] slopes) {
		if (!tree.isRooted()) {
			throw new IllegalArgumentException("tree must be rooted");
		}

		int[] order = tree.bfsOrder();
		int n = order.length;
		int root = tree.root();

		if (slopes.length != n) {
			throw new IllegalArgumentException("length mismatch");
		}

		// DP の差分値を小さい順に持つ。
		var strategy = new MeldableLazyOrderStatisticMultiset.Strategy<Long, Long>() {

			@Override
			public int compare(Long a, Long b) {
				return Long.compare(a, b);
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
		 * bfsOrder の逆順に処理する。 頂点 v の処理時には、全ての子の DP の差分列が dp[v] に meld 済みになっている。
		 */
		for (int oi = n - 1; oi >= 0; oi--) {
			int v = order[oi];

			SlopeSegment[] segments = slopes[v];
			int subtreeSize = tree.size(v);

			if (segments == null || segments.length == 0) {
				throw new IllegalArgumentException("slopes[" + v + "] is empty");
			}

			/*
			 * [0, subtreeSize) を覆うまでの Segment だけ使用する。 それより後の Segment はこの頂点では不要なので無視する。
			 *
			 * 例えば subtreeSize = 1 のとき
			 *
			 * (1, -1), (N, 0)
			 *
			 * が与えられても、最初の Segment だけを使用する。
			 */
			int m = 1;
			while (m < segments.length && segments[m - 1].endExclusive() < subtreeSize) {
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
			 * 現在の d は、子部分木と v 自身だけを考えた DP の差分列（昇順）である。
			 *
			 * その第 k 要素に
			 *
			 * phi_v(k + 1) - phi_v(k)
			 *
			 * を加えることで、T_v 全体の DP の差分列にする。 phi_v は凸なのでこの加算値は非減少であり、加算後も昇順が保たれる。
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
		 * 根の DP の差分列から、 選択頂点数ごとの最小値を復元する。
		 */
		long[] ret = new long[n + 1];

		long value = 0;
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

			if (i > 0 && previousGain > s.gain()) {
				throw new IllegalArgumentException("slopes[" + v + "] is not non-decreasing");
			}

			previousEnd = s.endExclusive();
			previousGain = s.gain();
		}

		if (previousEnd < subtreeSize) {
			throw new IllegalArgumentException("slopes[" + v + "] does not cover [0, " + subtreeSize + ")");
		}
	}
}