package library.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import library.util.algebra.strategy.RingStrategy;
import library.util.algebra.strategy.longs.LongRingStrategy;

public class ComplementGraph {
	public int N;
	public long M;
	public HashSet<Integer>[] nonAdj;
	
	
	public ComplementGraph(int N) {
		this.N = N;
		this.M = (long) N * (N - 1) / 2;
		nonAdj=new HashSet[N];
		for (int i = 0; i < N; i++) {
			nonAdj[i]=new HashSet<>();
		}
	}
	
	/**
	 * K^nから辺を削除していく
	 * @param a
	 * @param b
	 */
	public void removeEdge(int a, int b) {
		if (nonAdj[a].add(b)) {
			nonAdj[b].add(a);
			M--;
		}
	}

	/**
	 * 頂点数を返す。
	 * <ul>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * 未テスト
	 * @return 頂点数
	 */
	public int vertexCount() {
		return N;
	}

	/**
	 * 辺数を返す。
	 * <ul>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * 未テスト
	 * @return 辺数
	 */
	public long edgeCount() {
		return M;
	}
	
	/**
	 * 
	 * @param src
	 * @return
	 */
	public int[] distances(int src) {
		//verified:https://atcoder.jp/contests/abc319/submissions/70920123
		Queue<Integer>que=new ArrayDeque<>();
		que.add(src);
		boolean[] blocked=new boolean[N];
		int[]dist=new int[N];
		final int INF=Integer.MAX_VALUE/3;
		Arrays.fill(dist, INF);
		dist[src]=0;
		ArrayList<Integer>list=new ArrayList<>();
		for (int i = 0; i < N; i++) {
			if(i!=src)list.add(i);
		}
		while(!que.isEmpty()) {
			int v=que.poll();
			ArrayList<Integer> nlist=new ArrayList<>();
			for (int u : nonAdj[v]) {
				blocked[u]=true;
			}
			for (int u:list) {
				if (dist[u]!=INF)continue;
				if (blocked[u]) {
					nlist.add(u);
				} else {
					dist[u]=dist[v]+1;
					que.add(u);
				}
			}
			list=nlist;
			for (int u : nonAdj[v]) {
				blocked[u] = false;
			}
		}
		
		return dist;
	}

	/**
	 * 隣接リスト表現の {@link Graph} インスタンスに変換する。
	 * 完全グラフ K_N から nonAdj に含まれる辺を除外したグラフを構築する。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * 未テスト
	 * @return {@link Graph} インスタンス
	 */
	public Graph toGraph() {
		Graph g = new Graph(N);
		for (int i = 0; i < N; i++) {
			for (int j = i + 1; j < N; j++) {
				if (!nonAdj[i].contains(j)) {
					g.addEdge(i, j);
				}
			}
		}
		return g;
	}

	/**
	 * 補グラフを画面上に描画する。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * 未テスト
	 */
	public void draw() {
		toGraph().draw();
	}


	/**
	 * 始点 v から各頂点 w へのすべての最短パスについて、パス上の頂点の重みの積の和を環構造 ring 上で計算する。
	 * パス P = (u_0, u_1, ..., u_d) (ただし u_0 = v, u_d = w, d = dist(v, w)) の重みを prod_{i=0}^d f[u_i] と定義し、
	 * w への最短パス全体にわたる重み積の和を求める。w が v から到達不能な場合は ring.zero() となる。
	 * <ul>
	 *   <li>計算量: O(N + M) (M は nonAdj に含まれる無向辺数)</li>
	 * </ul>
	 * 未テスト
	 * @param <T> 環の要素の型
	 * @param v 始点 (0 <= v < N)
	 * @param f 各頂点の重み配列 (f.length >= N)
	 * @param ring 環構造の戦略
	 * @return 各頂点 w への最短パスにおける重み積の総和を格納した長さ N の配列
	 */
	@SuppressWarnings("unchecked")
	public <T> T[] shortestPathWeightSum(int v, T[] f, RingStrategy<T> ring) {
		int[] dist = distances(v);
		int maxDist = -1;
		for (int i = 0; i < N; i++) {
			if (dist[i] != Integer.MAX_VALUE / 3) {
				maxDist = Math.max(maxDist, dist[i]);
			}
		}
		T[] dp = (T[]) java.lang.reflect.Array.newInstance(f.getClass().getComponentType(), N);
		for (int i = 0; i < N; i++) {
			dp[i] = ring.zero();
		}
		if (maxDist < 0) {
			return dp;
		}
		ArrayList<Integer>[] layers = new ArrayList[maxDist + 1];
		for (int i = 0; i <= maxDist; i++) {
			layers[i] = new ArrayList<>();
		}
		for (int i = 0; i < N; i++) {
			if (dist[i] <= maxDist) {
				layers[dist[i]].add(i);
			}
		}

		dp[v] = f[v];

		for (int d = 1; d <= maxDist; d++) {
			T layerSum = ring.zero();
			for (int u : layers[d - 1]) {
				layerSum = ring.add(layerSum, dp[u]);
			}
			for (int w : layers[d]) {
				T nonAdjSum = ring.zero();
				for (int u : nonAdj[w]) {
					if (dist[u] == d - 1) {
						nonAdjSum = ring.add(nonAdjSum, dp[u]);
					}
				}
				T validSum = ring.sub(layerSum, nonAdjSum);
				dp[w] = ring.mul(validSum, f[w]);
			}
		}

		return dp;
	}

	/**
	 * 始点 v から各頂点 w へのすべての最短パスについて、パス上の頂点の重みの積の和を primitive long 用の環構造 ring 上で計算する。
	 * パス P = (u_0, u_1, ..., u_d) (ただし u_0 = v, u_d = w, d = dist(v, w)) の重みを prod_{i=0}^d f[u_i] と定義し、
	 * w への最短パス全体にわたる重み積の和を求める。w が v から到達不能な場合は ring.zero() となる。
	 * <ul>
	 *   <li>計算量: O(N + M) (M は nonAdj に含まれる無向辺数)</li>
	 * </ul>
	 * 未テスト
	 * @param v 始点 (0 <= v < N)
	 * @param f 各頂点の重み配列 (f.length >= N)
	 * @param ring primitive long 用の環構造の戦略
	 * @return 各頂点 w への最短パスにおける重み積の総和を格納した長さ N の配列
	 */
	public long[] shortestPathWeightSum(int v, long[] f, LongRingStrategy ring) {
		int[] dist = distances(v);
		int maxDist = -1;
		for (int i = 0; i < N; i++) {
			if (dist[i] != Integer.MAX_VALUE / 3) {
				maxDist = Math.max(maxDist, dist[i]);
			}
		}
		long[] dp = new long[N];
		for (int i = 0; i < N; i++) {
			dp[i] = ring.zero();
		}
		if (maxDist < 0) {
			return dp;
		}
		ArrayList<Integer>[] layers = new ArrayList[maxDist + 1];
		for (int i = 0; i <= maxDist; i++) {
			layers[i] = new ArrayList<>();
		}
		for (int i = 0; i < N; i++) {
			if (dist[i] <= maxDist) {
				layers[dist[i]].add(i);
			}
		}

		dp[v] = f[v];

		for (int d = 1; d <= maxDist; d++) {
			long layerSum = ring.zero();
			for (int u : layers[d - 1]) {
				layerSum = ring.add(layerSum, dp[u]);
			}
			for (int w : layers[d]) {
				long nonAdjSum = ring.zero();
				for (int u : nonAdj[w]) {
					if (dist[u] == d - 1) {
						nonAdjSum = ring.add(nonAdjSum, dp[u]);
					}
				}
				long validSum = ring.sub(layerSum, nonAdjSum);
				dp[w] = ring.mul(validSum, f[w]);
			}
		}

		return dp;
	}
}
