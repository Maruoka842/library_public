package library.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.LongUnaryOperator;

import library.util.collections.IntQueue;
import library.util.graph.Digraph;
import library.util.graph.LongValueDigraph;
import library.util.seq.SortedArrays;

public class MaxPotentialDifference {
	int N;
	LongValueDigraph g;
	boolean containsNegativeEdge=false;
	
	public MaxPotentialDifference(int N) {
		this.N = N;
		g = new LongValueDigraph(N);
	}
	
	/**
	 * <pre>v[a] = v[b] + w</pre>
	 * 内部ではbからaへの重みwの辺とaからbへの重み-wの辺が貼られる。Bellman-Ford確定。
	 * ただし、負辺を無視しても距離が変わらない場合は Dijkstra の結果を使う。
	 * @param a
	 * @param b
	 */
	public void forceEqual(int a, int b, long w) {
		if (a == b) return;
		g.addEdge(a, b, -w);
		g.addEdge(b, a, w);
		containsNegativeEdge=true;
	}
	
	/**
	 * <pre>v[a] ≤ v[b] + w</pre>
	 * 内部ではbからaへの重みwの辺が貼られる。
	 * @param a
	 * @param b
	 */
	public void forceDiffLeq(int a, int b, long w) {
		g.addEdge(b, a, w);
		if(w<0)containsNegativeEdge=true;
	}

	/**
	 * すべての (i,j) について p[i] <= q[j] なら
	 * x[v[i]] - x[u[j]] <= H(q[j]) - H(p[i]) + alpha[i] + beta[j] を追加する。
	 * H は単調非減少、alpha と beta は非負とする。キーごとの補助変数に
	 * 追加される頂点数・制約数は O(n + m)、構築時間は O((n + m) log(n + m))。
	 * @param v 候補側の変数番号
	 * @param p 候補側のキー
	 * @param h キーから H(key) への関数
	 * @param alpha 候補側の非負コスト
	 * @param u 制約を受ける側の変数番号
	 * @param q 制約を受ける側のキー
	 * @param beta 制約を受ける側の非負コスト
	 * @throws IllegalArgumentException 配列長が一致しない、または alpha/beta に負値がある場合
	 * @throws ArithmeticException H の差やコスト和を long で表せない場合
	 */
	public void forceKeyOrderedDiffLeq(int[] v, long[] p, LongUnaryOperator h, long[] alpha,
			int[] u, long[] q, long[] beta) {
		if (v.length != p.length || v.length != alpha.length
				|| u.length != q.length || u.length != beta.length) {
			throw new IllegalArgumentException("配列長が一致しません");
		}
		for (long cost : alpha) if (cost < 0) throw new IllegalArgumentException("alpha は非負である必要があります");
		for (long cost : beta) if (cost < 0) throw new IllegalArgumentException("beta は非負である必要があります");

		long[] keys = ArrayUtils.sortq(p);
		int keyCount = keys.length;
		long[] hValues = new long[keyCount];
		for (int i = 0; i < keyCount; i++) {
			hValues[i] = h.applyAsLong(keys[i]);
			if (i > 0 && hValues[i] < hValues[i - 1]) {
				throw new IllegalArgumentException("H は単調非減少である必要があります");
			}
		}

		int[] pIndex = new int[p.length];
		int[] qPrefixIndex = new int[q.length];
		for (int i = 0; i < p.length; i++) pIndex[i] = SortedArrays.indexOf(keys, p[i]);
		for (int j = 0; j < q.length; j++) qPrefixIndex[j] = SortedArrays.floor(keys, q[j]);

		long[] chainCosts = new long[keyCount];
		for (int i = 1; i < keyCount; i++) chainCosts[i] = Math.subtractExact(hValues[i], hValues[i - 1]);
		long[] queryCosts = new long[q.length];
		for (int j = 0; j < q.length; j++) {
			int index = qPrefixIndex[j];
			if (index >= 0) {
				long hq = h.applyAsLong(q[j]);
				long gap = Math.subtractExact(hq, hValues[index]);
				if (gap < 0) throw new IllegalArgumentException("H は単調非減少である必要があります");
				queryCosts[j] = Math.addExact(gap, beta[j]);
			}
		}

		int[] aux = new int[keyCount];
		for (int i = 0; i < keyCount; i++) aux[i] = g.addNode();
		N = g.N;
		for (int i = 0; i < p.length; i++) forceDiffLeq(v[i], aux[pIndex[i]], alpha[i]);
		for (int i = 1; i < keyCount; i++) forceDiffLeq(aux[i - 1], aux[i], chainCosts[i]);
		for (int j = 0; j < q.length; j++) {
			if (qPrefixIndex[j] >= 0) forceDiffLeq(aux[qPrefixIndex[j]], u[j], queryCosts[j]);
		}
	}

	/**
	 * 差分制約 v[a] - v[b] >= w を設定する。
	 * 内部では forceDiffLeq(b, a, -w) を呼び出す。
	 * 計算量: O(1)。
	 * // 未テスト
	 * @param a 対象頂点1
	 * @param b 対象頂点2
	 * @param w 下限値
	 */
	public void forceDiffGeq(int a, int b, long w) {
		forceDiffLeq(b, a, -w);
	}

	/**
	 * 差分に関する範囲制約 lo <= v[a] - v[b] <= hi を設定する。
	 * 内部では forceDiffGeq(a, b, lo) および forceDiffLeq(a, b, hi) を呼び出す。
	 * 計算量: O(1)。
	 * // 未テスト
	 * @param a 対象頂点1
	 * @param b 対象頂点2
	 * @param lo 下限値
	 * @param hi 上限値
	 */
	public void forceDiffRange(int a, int b, long lo, long hi) {
		forceDiffGeq(a, b, lo);
		forceDiffLeq(a, b, hi);
	}
	
	class Leq {
		int v;
		long w;
		public Leq(int v, long w) {
			this.v=v;
			this.w=w;
		}
	}
	
	ArrayList<Leq> leqs=new ArrayList<>();
	
	class Geq {
		int v;
		long w;
		public Geq(int v, long w) {
			this.v = v;
			this.w = w;
		}
	}

	ArrayList<Geq> geqs = new ArrayList<>();
	
	/**
	 * <pre>v[a] ≤ w</pre>
	 * 内部ではfixからaへの重みwの辺が貼られる。
	 */
	public void forceLeq(int a, long w) {
		leqs.add(new Leq(a, w));
	}

	/**
	 * ポテンシャルに関する下限制約 v[a] >= w を設定する。
	 * 内部では fix から a への差分制約辺が追加される。
	 * 計算量: O(1)。
	 * // 未テスト
	 * @param a 対象頂点
	 * @param w 下限値
	 */
	public void forceGeq(int a, long w) {
		geqs.add(new Geq(a, w));
	}

	/**
	 * ポテンシャルに関する範囲制約 lo <= v[a] <= hi を設定する。
	 * 内部では forceGeq(a, lo) および forceLeq(a, hi) を呼び出す。
	 * 計算量: O(1)。
	 * // 未テスト
	 * @param a 対象頂点
	 * @param lo 下限値
	 * @param hi 上限値
	 */
	public void forceRange(int a, long lo, long hi) {
		forceGeq(a, lo);
		forceLeq(a, hi);
	}
	
	/**
	 * <pre>|v[a]-v[b]| ≤ w</pre>
	 * 内部ではbからaへの重みwの辺とaからbへの重みwの辺が貼られる。
	 * @param a
	 * @param b
	 */
	public void forceAbsDiffLeq(int a, int b, long w) {
		forceDiffLeq(a, b, w);
		forceDiffLeq(b, a, w);
	}
	
	/**
	 * v[fix] = 0 の条件で絶対値制約をグラフへ移す。成功時は leqs と geqs を消費する。
	 * 計算量: O(L + G + E')。未テスト。
	 * @param fix 固定頂点
	 * @return 固定頂点に対する上下限制約が整合するとき true
	 */
	private boolean flushLeqs(int fix) {
		return flushLeqs(fix, 0);
	}

	/**
	 * u[i] = v[i] - offset とした座標で絶対値制約をグラフへ移し、u[fix] = 0 と整合するか判定する。
	 * leq v[a] <= w は u[a] <= w - offset、geq v[a] >= w は u[a] >= w - offset に変換する。
	 * 成功時は leqs と geqs を消費し、変換演算が long の範囲を超える場合は ArithmeticException を送出する。
	 * 計算量: O(L + G + E')。未テスト。
	 * @param fix 0 に固定する頂点
	 * @param offset 座標の平行移動量
	 * @return 固定頂点に対する上下限制約が整合するとき true
	 */
	private boolean flushLeqs(int fix, long offset) {
		long[] upper = new long[leqs.size()];
		long[] lower = new long[geqs.size()];
		long[] lowerEdge = new long[geqs.size()];
		for (int i = 0; i < leqs.size(); i++) upper[i] = Math.subtractExact(leqs.get(i).w, offset);
		for (int i = 0; i < geqs.size(); i++) {
			lower[i] = Math.subtractExact(geqs.get(i).w, offset);
			lowerEdge[i] = Math.negateExact(lower[i]);
		}
		for (int i = 0; i < leqs.size(); i++) {
			var leq = leqs.get(i);
			if (leq.v == fix) {
				if (upper[i] < 0) return false;
			}
		}
		for (int i = 0; i < geqs.size(); i++) {
			var geq = geqs.get(i);
			if (geq.v == fix) {
				if (lower[i] > 0) return false;
			}
		}
		for (int i = 0; i < leqs.size(); i++) if (leqs.get(i).v != fix) forceDiffLeq(leqs.get(i).v, fix, upper[i]);
		for (int i = 0; i < geqs.size(); i++) if (geqs.get(i).v != fix) forceDiffLeq(fix, geqs.get(i).v, lowerEdge[i]);
		leqs.clear();
		geqs.clear();
		return true;
	}
	
	
	/**
	 * v[fix]=0の下で各iについてv[i]を最大化する。
	 * <ul>
	 * <li>負辺がある場合　O(NM) (Bellman-Ford)</li>
	 * <li>負辺がない場合 Dijkstra</li>
	 * </ul>
	 * 負辺があっても無視できる場合は O(N + M log(M + 2))。
	 * @param s
	 * @param t
	 * @return
	 */
	public long[] maximize(int fix) {
		//https://atcoder.jp/contests/past21-open/submissions/75015150
		if (!flushLeqs(fix)) return null;
		long[] ret;
		if(containsNegativeEdge) {
			ret = g.tryDijkstraIgnoringNegativeEdges(fix)
					.map(LongValueDigraph.DijkstraResult::dist).orElseGet(() -> g.bellmanFord(fix));
		} else {
			ret = g.dijkstra(fix).dist();
		}
		for (int i = 0; i < ret.length; i++) {
			if(ret[i]==Long.MIN_VALUE) return null;
		}
		return ret;
	}
	
	public boolean isFeasible(int fix) {
		if (!flushLeqs(fix)) return false;
		return !g.containsNegativeCycle();
	}
	
	/**
	 * v[fix]=0の下で各iについてv[i]を最小化する。
	 * 非負だとDijkstra
	 * -∞のときはLong.MIN_VALUE。
	 * 解が存在しないときはnull
	 * 計算量: 負辺を無視できる場合 O(N + M log(M + 2))、それ以外は O(N + M log(M + 2) + NM)。
	 * @param fix
	 * @return
	 */
	public long[] minimize(int fix) {
		return minimize(fix, 0);
	}

	/**
	 * v[fix] = fixValue の下で各 i の v[i] の最小値を返す。u[i] = v[i] - fixValue とし、
	 * 差分制約を保ったまま絶対値制約を u 座標へ平行移動して既存の最小化を行う。
	 * 計算量: 負辺を無視できる場合 O(N + M log(M + 2))、それ以外は O(N + M log(M + 2) + NM)。未テスト。
	 * @param fix 値を固定する頂点
	 * @param fixValue 固定値
	 * @return 元の座標系での各頂点の最小値。不可能な場合は null
	 */
	public long[] minimize(int fix, long fixValue) {
		//https://atcoder.jp/contests/abc404/submissions/75016005
		/* v[a] <= v[b] + w の下で各 i について v[i] を最小化
		 * 
		 * u[i]=-v[i] と置くと
		 *   v[a] <= v[b] + w
		 * ⇔u[a] >= u[b] - w
		 * ⇔u[b] <= u[a] + w
		 */
		if (!flushLeqs(fix, fixValue)) return null;
		long[] ret;
		LongValueDigraph reversed = g.reverse();
	    if (containsNegativeEdge) {
			ret = reversed.tryDijkstraIgnoringNegativeEdges(fix)
					.map(LongValueDigraph.DijkstraResult::dist)
					.orElseGet(() -> g.tryDijkstraIgnoringNegativeEdges(fix)
							.flatMap(result -> tryReweightedReverseDistances(reversed, fix, result.dist()))
							.orElseGet(() -> reversed.bellmanFord(fix)));
	    } else {
	        ret = reversed.dijkstra(fix).dist();
	    }
		for (int i = 0; i < g.N; i++) {
			if(ret[i]==Long.MAX_VALUE) {
				ret[i]=Long.MIN_VALUE;
			} else if(ret[i]==Long.MIN_VALUE) {
				return null;
			} else {
				ret[i] = -ret[i];
			}
		}
		for (int i = 0; i < ret.length; i++) {
			if (ret[i] != Long.MIN_VALUE) ret[i] = Math.addExact(ret[i], fixValue);
		}
		return ret;
	}
	
	
    public record MinMaxResult(long[] min, long[] max) {};

	/**
	 * p[v] = dist_G(fix, v), p[fix] = 0, 0 <= p[v] < Long.MAX_VALUE のとき、
	 * 逆辺 (v, u, w) の重み w + p[u] - p[v] による距離 d' から
	 * dist_G^R(fix, v) = d'[v] - p[v] を返す。到達不能は Long.MAX_VALUE。
	 * p に無限大がある場合、または変換後の重み・有限距離を long で表現できない場合は空。
	 * 計算量: 時間 O(N + M log(M + 2))、追加空間 O(N + M)。未テスト。
	 * @param reversed G の逆グラフ
	 * @param fix 始点
	 * @param p G の非負の最短距離
	 * @return 逆グラフの距離、または重み変換を適用できない場合は空
	 */
	private Optional<long[]> tryReweightedReverseDistances(LongValueDigraph reversed, int fix, long[] p) {
		for (long value : p) {
			if (value == Long.MAX_VALUE) return Optional.empty();
		}
		try {
			LongValueDigraph reweighted = new LongValueDigraph(g.N);
			for (int v = 0; v < g.N; v++) {
				for (var e : reversed.adj[v]) {
					long cost = Math.addExact(e.cost, p[e.dst] - p[v]);
					reweighted.addEdge(v, e.dst, cost);
				}
			}
			long[] dist = reweighted.tryDijkstraIgnoringNegativeEdges(fix).orElseThrow().dist();
			for (int v = 0; v < g.N; v++) {
				if (dist[v] == Long.MAX_VALUE) continue;
				for (var e : reweighted.adj[v]) {
					if (dist[e.dst] == Long.MAX_VALUE) return Optional.empty();
				}
			}
			for (int v = 0; v < g.N; v++) {
				if (dist[v] != Long.MAX_VALUE) dist[v] -= p[v];
			}
			return Optional.of(dist);
		} catch (ArithmeticException ex) {
			return Optional.empty();
		}
	}

	/**
	 * v[fix] = 0 の下で各 v[i] の最小値・最大値を返す。
	 * 下限 -∞ は Long.MIN_VALUE、上限 +∞ は Long.MAX_VALUE、到達可能な負閉路がある場合は null。
	 * 計算量: 重み変換または負辺無視で求まる場合 O(N + M log(M + 2))、
	 * それ以外は O(N + M log(M + 2) + NM)。未テスト。
	 * @param fix 値を 0 に固定する頂点
	 * @return 各頂点の下限と上限
	 */
	public  MinMaxResult getRange(int fix) {
		//https://atcoder.jp/contests/past21-open/submissions/75016213
		if (!flushLeqs(fix)) return null;
		long[] lower;
		long[] upper;
		LongValueDigraph reversed = g.reverse();
	    if (containsNegativeEdge) {
			var result = g.tryDijkstraIgnoringNegativeEdges(fix);
			upper = result.map(LongValueDigraph.DijkstraResult::dist).orElseGet(() -> g.bellmanFord(fix));
			Optional<long[]> reweighted = result.isPresent()
					? tryReweightedReverseDistances(reversed, fix, upper) : Optional.empty();
			lower = reweighted.orElseGet(() -> reversed.tryDijkstraIgnoringNegativeEdges(fix)
					.map(LongValueDigraph.DijkstraResult::dist).orElseGet(() -> reversed.bellmanFord(fix)));
	    } else {
	        lower = reversed.dijkstra(fix).dist();
	        upper = g.dijkstra(fix).dist();
	    }
		for (int i = 0; i < g.N; i++) {
			if(lower[i]==Long.MAX_VALUE) {
				lower[i]=Long.MIN_VALUE;
			} else if(lower[i]==Long.MIN_VALUE) {
				return null;
			} else {
				lower[i] = -lower[i];
			}
		}
		for (int i = 0; i < upper.length; i++) {
			if(upper[i]==Long.MIN_VALUE) return null;
		}
		return new MinMaxResult(lower, upper);
	}
}
