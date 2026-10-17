package library.util.graph;

import java.util.Arrays;
import library.util.ArrayUtils;

/**
 * 辺に long 型のコストを持つ有向非巡回グラフ (DAG)。
 */
public class LongValueDAG extends LongValueDigraph {
	/** トポロジカルソート順 */
	int[] order;
	/** 自然順序であることを示すフラグ */
	boolean isNaturalOrder;
	/** 逆自然順序であることを示すフラグ */
	boolean isNaturalReverseOrder;

	/**
	 * 指定された頂点数を持つ DAG を構築する。
	 *
	 * @param N 頂点数
	 */
	public LongValueDAG(int N) {
		super(N);
	}

	/**
	 * この DAG と同じ頂点集合、重み付き辺集合および既知のトポロジカル順序を持つ DAG を返す。
	 * 辺オブジェクト、隣接リストおよび順序配列を共有しない。
	 * O(N + M)
	 *
	 * @return この DAG の深いコピー
	 */
	@Override
	// 未テスト
	public LongValueDAG copy() {
		LongValueDAG ret = new LongValueDAG(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			for (Edge e : adj[i]) {
				ret.adj[i].add(new Edge(e.src, e.dst, e.cost));
			}
		}
		ret.isNaturalOrder = isNaturalOrder;
		ret.isNaturalReverseOrder = isNaturalReverseOrder;
		ret.order = order == null ? null : order.clone();
		return ret;
	}

	/**
	 * 自然順序 (from < to) のトポロジカル順序を持つ DAG を構築する。
	 * 各頂点 i (0 <= i < N) に対して、初期順序は i となる。
	 * 計算量: O(N)
	 * 未テスト。
	 *
	 * @param N 頂点数
	 * @return 構築された DAG インスタンス
	 */
	public static LongValueDAG ofNaturalOrder(int N) {
		LongValueDAG g = new LongValueDAG(N);
		g.isNaturalOrder = true;
		g.order = new int[N];
		for (int i = 0; i < N; i++) g.order[i] = i;
		return g;
	}

	/**
	 * 逆自然順序 (from > to) のトポロジカル順序を持つ DAG を構築する。
	 * 各頂点 i (0 <= i < N) に対して、初期順序は N - 1 - i となる。
	 * 計算量: O(N)
	 * 未テスト。
	 *
	 * @param N 頂点数
	 * @return 構築された DAG インスタンス
	 */
	public static LongValueDAG ofNaturalReverseOrder(int N) {
		LongValueDAG g = new LongValueDAG(N);
		g.isNaturalReverseOrder = true;
		g.order = new int[N];
		for (int i = 0; i < N; i++) g.order[i] = N - 1 - i;
		return g;
	}

	@Override
	public void addEdge(int from, int to, long cost) {
		if (isNaturalOrder && from >= to) throw new AssertionError("natural order DAG requires from < to: " + from + " -> " + to);
		if (isNaturalReverseOrder && from <= to) throw new AssertionError("natural reverse order DAG requires from > to: " + from + " -> " + to);
		super.addEdge(from, to, cost);
	}

	/**
	 * トポロジカルソート順を求める。
	 * グラフが DAG でない場合は例外（AssertionError）をスローする。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return トポロジカル順序を格納した配列
	 */
	public int[] topologicalOrder() {
		// 未テスト
		if (order == null) {
			int[] inDegrees = inDegrees();
			java.util.Queue<Integer> que = new java.util.ArrayDeque<>();
			for (int i = 0; i < N; ++i) {
				if (inDegrees[i] == 0) que.add(i);
			}
			boolean[] vis = new boolean[N];
			int[] ord = new int[N];
			int p = 0;
			while (!que.isEmpty()) {
				int v = que.poll();
				ord[p++] = v;
				vis[v] = true;
				for (var e : adj[v]) {
					if (!vis[e.dst]) {
						--inDegrees[e.dst];
						if (inDegrees[e.dst] == 0) {
							que.add(e.dst);
						}
					}
				}
			}
			if (p != N) throw new AssertionError("DAGではない");
			order = ord;
		}
		return order;
	}

	/**
	 * 各頂点 s を始点、任意のシンク（出次数が0の頂点）を終点とする最長パスの辺数を求める。
	 * 各頂点 i に対する最長パスの辺数を dp[i] とすると、dp[i] = max_(e) (dp[e.dst] + 1) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最長パス長を格納した配列
	 */
	public int[] longestPathLengthForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		int[] dp = new int[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				dp[i] = Math.max(dp[i], dp[e.dst] + 1);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、任意のシンク（出次数が0の頂点）を終点とする最短パスの辺数を求める。
	 * 始点がシンクである場合は 0 と定義される。
	 * 各頂点 i に対する最短パスの辺数を dp[i] とすると、dp[i] = min_(e) (dp[e.dst] + 1) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最短パス長を格納した配列
	 */
	public int[] shortestPathLengthForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		int[] dp = new int[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			if (adj[i].isEmpty()) {
				dp[i] = 0;
			} else {
				dp[i] = Integer.MAX_VALUE;
				for (var e : adj[i]) {
					dp[i] = Math.min(dp[i], dp[e.dst] + 1);
				}
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点としたときの最長パスコストの総和を求める。
	 * 辺の cost の総和を最大化し、長さ0のパス（自身にとどまる）も候補に含めるため、下限は 0 となる。
	 * 各頂点 i に対する最長パスコストを dp[i] とすると、dp[i] = max_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最長パス最大コストを格納した配列
	 */
	public long[] longestPathCostForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		long[] dp = new long[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				dp[i] = Math.max(dp[i], dp[e.dst] + e.cost);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、任意のシンク（出次数が0の頂点）を終点とする最短パスのコストの総和を求める。
	 * 始点がシンクである場合は 0 と定義される。
	 * 各頂点 i に対する最短パスの最小コストを dp[i] とすると、dp[i] = min_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最短パス最小コストを格納した配列
	 */
	public long[] shortestPathCostForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		long[] dp = new long[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			if (adj[i].isEmpty()) {
				dp[i] = 0;
			} else {
				dp[i] = Long.MAX_VALUE;
				for (var e : adj[i]) {
					dp[i] = Math.min(dp[i], dp[e.dst] + e.cost);
				}
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、指定された終点 end を固定したときの最長パスコストの総和を求める。
	 * 終点 end に到達不可能な頂点の値は Long.MIN_VALUE と定義される。
	 * 終点自体の値は dp[end] = 0 となり、それ以外の頂点 i に対しては dp[i] = max_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @param end 終点頂点のインデックス
	 * @return 各頂点からの最長パス最大コストを格納した配列
	 */
	public long[] longestPathCostForEachStartFixingEnd(int end) {
		// 未テスト
		if (order == null) order = topologicalOrder();
		long[] dp = new long[N];
		Arrays.fill(dp, Long.MIN_VALUE);
		dp[end] = 0;
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				if (dp[e.dst] == Long.MIN_VALUE) continue;
				dp[i] = Math.max(dp[i], dp[e.dst] + e.cost);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、指定された終点 end を固定したときの最短パスコストの総和を求める。
	 * 終点 end に到達不可能な頂点の値は Long.MAX_VALUE と定義される。
	 * 終点自体の値は dp[end] = 0 となり、それ以外の頂点 i に対しては dp[i] = min_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @param end 終点頂点のインデックス
	 * @return 各頂点からの最短パス最小コストを格納した配列
	 */
	public long[] shortestPathCostForEachStartFixingEnd(int end) {
		// 未テスト
		if (order == null) order = topologicalOrder();
		long[] dp = new long[N];
		Arrays.fill(dp, Long.MAX_VALUE);
		dp[end] = 0;
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				if (dp[e.dst] == Long.MAX_VALUE) continue;
				dp[i] = Math.min(dp[i], dp[e.dst] + e.cost);
			}
		}
		return dp;
	}

	/**
	 * 全ての辺の向きを反転させた新しい LongValueDAG インスタンスを生成する。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 反転された DAG
	 */
	public LongValueDAG reverse() {
		// 未テスト
		LongValueDAG g = new LongValueDAG(N);
		for (int i = 0; i < N; ++i) {
			for (var e : adj[i]) {
				g.addEdge(e.dst, i, e.cost);
			}
		}
		if (order == null) order = topologicalOrder();
		g.order = Arrays.copyOf(order, N);
		ArrayUtils.reverse(g.order);
		return g;
	}

	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
