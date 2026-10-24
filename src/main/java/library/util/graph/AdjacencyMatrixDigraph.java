package library.util.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import library.util.collections.IntQueue;

/**
 * 密な有向グラフを隣接行列 {@code boolean[][] adj} で表現するクラス。
 *
 * <p>数学的仕様:
 * 頂点集合を V = {0, 1, ..., N-1} とする。
 * 隣接行列 adj[u][v] は頂点 u から頂点 v への有向辺 (u, v) が存在するかどうかを表す。
 */
// 未テスト
public class AdjacencyMatrixDigraph {
	public int N;
	public int M;
	public boolean[][] adj;

	/**
	 * N 頂点 0 辺の密な有向グラフを構築する。
	 * <ul>
	 *   <li>事前条件: N >= 0</li>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @param N 頂点数
	 */
	public AdjacencyMatrixDigraph(int N) {
		if (N < 0) throw new IllegalArgumentException("N must be non-negative");
		this.N = N;
		this.M = 0;
		this.adj = new boolean[N][N];
	}

	/**
	 * 指定された隣接行列 {@code adjcentMatrix} から密な有向グラフを構築する。
	 * <ul>
	 *   <li>事前条件: {@code adjcentMatrix} は N x N の正方行列。</li>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @param adjcentMatrix 隣接行列
	 */
	public AdjacencyMatrixDigraph(boolean[][] adjcentMatrix) {
		if (adjcentMatrix == null) throw new IllegalArgumentException("adjcentMatrix cannot be null");
		int n = adjcentMatrix.length;
		for (int i = 0; i < n; i++) {
			if (adjcentMatrix[i] == null || adjcentMatrix[i].length != n) {
				throw new IllegalArgumentException("adjcentMatrix must be a square matrix");
			}
		}
		this.N = n;
		this.adj = new boolean[n][n];
		int edgeCount = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				this.adj[i][j] = adjcentMatrix[i][j];
				if (adjcentMatrix[i][j]) {
					edgeCount++;
				}
			}
		}
		this.M = edgeCount;
	}

	/**
	 * 有向辺 (from, to) を追加する。
	 * 既に辺が存在する場合は変化しない。
	 * <ul>
	 *   <li>事前条件: 0 <= from < N, 0 <= to < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param from 始点
	 * @param to 終点
	 */
	public void addEdge(int from, int to) {
		checkVertexBounds(from);
		checkVertexBounds(to);
		if (!adj[from][to]) {
			adj[from][to] = true;
			M++;
		}
	}

	/**
	 * 有向辺 (from, to) を削除する。
	 * 辺が存在しない場合は変化しない。
	 * <ul>
	 *   <li>事前条件: 0 <= from < N, 0 <= to < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param from 始点
	 * @param to 終点
	 */
	public void removeEdge(int from, int to) {
		checkVertexBounds(from);
		checkVertexBounds(to);
		if (adj[from][to]) {
			adj[from][to] = false;
			M--;
		}
	}

	/**
	 * 頂点 from から頂点 to への有向辺が存在するか判定する。
	 * <ul>
	 *   <li>事前条件: 0 <= from < N, 0 <= to < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param from 始点
	 * @param to 終点
	 * @return 辺が存在する場合 true
	 */
	public boolean hasEdge(int from, int to) {
		checkVertexBounds(from);
		checkVertexBounds(to);
		return adj[from][to];
	}

	/**
	 * 頂点 v の出次数を返す。
	 * <ul>
	 *   <li>事前条件: 0 <= v < N</li>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * @param v 頂点
	 * @return 出次数
	 */
	public int outDegree(int v) {
		checkVertexBounds(v);
		int deg = 0;
		for (int u = 0; u < N; u++) {
			if (adj[v][u]) deg++;
		}
		return deg;
	}

	/**
	 * 頂点 v の入次数を返す。
	 * <ul>
	 *   <li>事前条件: 0 <= v < N</li>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * @param v 頂点
	 * @return 入次数
	 */
	public int inDegree(int v) {
		checkVertexBounds(v);
		int deg = 0;
		for (int u = 0; u < N; u++) {
			if (adj[u][v]) deg++;
		}
		return deg;
	}

	/**
	 * 全頂点の出次数配列を返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 各頂点の出次数配列
	 */
	public int[] outDegrees() {
		int[] degs = new int[N];
		for (int i = 0; i < N; i++) {
			degs[i] = outDegree(i);
		}
		return degs;
	}

	/**
	 * 全頂点の入次数配列を返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 各頂点の入次数配列
	 */
	public int[] inDegrees() {
		int[] degs = new int[N];
		for (int i = 0; i < N; i++) {
			degs[i] = inDegree(i);
		}
		return degs;
	}

	/**
	 * 始点 src からの BFS 距離配列を返す。
	 * 到達不能な頂点の距離は Integer.MAX_VALUE / 3 とする。
	 * <ul>
	 *   <li>事前条件: 0 <= src < N</li>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @param src 始点
	 * @return 距離配列
	 */
	public int[] bfsDistances(int src) {
		//https://atcoder.jp/contests/abc218/submissions/80224764
		checkVertexBounds(src);
		int INF = Integer.MAX_VALUE / 3;
		int[] dist = new int[N];
		Arrays.fill(dist, INF);
		dist[src] = 0;
		IntQueue que=new IntQueue();
		que.add(src);
		while (!que.isEmpty()) {
			int v = que.poll();
			for (int u = 0; u < N; u++) {
				if (adj[v][u] && dist[u] == INF) {
					dist[u] = dist[v] + 1;
					que.add(u);
				}
			}
		}
		return dist;
	}

	/**
	 * BFS 距離配列 {@code dist} をもとに、終点 {@code dst} までの最短経路（頂点列）を復元する。
	 * 到達不能な場合（{@code dist[dst] >= Integer.MAX_VALUE / 3}）は {@code null} を返す。
	 * <ul>
	 *   <li>事前条件: {@code dist != null}, {@code 0 <= dst < N}</li>
	 *   <li>計算量: O(N * dist[dst])</li>
	 * </ul>
	 * @param dist BFS 距離配列
	 * @param dst 終点
	 * @return 最短経路上の頂点列（始点から dst まで）、到達不能な場合は null
	 */
	public int[] restoreShortestPath(int[] dist, int dst) {
		//https://atcoder.jp/contests/abc218/submissions/80224764
		checkVertexBounds(dst);
		int INF = Integer.MAX_VALUE / 3;
		if (dist[dst] < 0) throw new AssertionError();
		if (dist == null || dst < 0 || dst >= N || dist[dst] >= INF) {
			return null;
		}
		int len = dist[dst] + 1;
		int[] path = new int[len];
		int curr = dst;
		path[len - 1] = curr;
		for (int i = len - 2; i >= 0; i--) {
			int prev = -1;
			for (int u = 0; u < N; u++) {
				if (adj[u][curr] && dist[u] == dist[curr] - 1) {
					prev = u;
					break;
				}
			}
			if (prev == -1) return null;
			curr = prev;
			path[i] = curr;
		}
		return path;
	}

	/**
	 * 逆有向グラフ（すべての有向辺の向きを反転させたグラフ）を生成して返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 逆有向グラフ
	 */
	public AdjacencyMatrixDigraph reverse() {
		AdjacencyMatrixDigraph rev = new AdjacencyMatrixDigraph(N);
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (adj[i][j]) {
					rev.addEdge(j, i);
				}
			}
		}
		return rev;
	}

	/**
	 * 補有向グラフ（自己ループを除くすべての有向辺の有無を反転させたグラフ）を生成して返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 補有向グラフ
	 */
	public AdjacencyMatrixDigraph complement() {
		AdjacencyMatrixDigraph res = new AdjacencyMatrixDigraph(N);
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (i != j && !adj[i][j]) {
					res.addEdge(i, j);
				}
			}
		}
		return res;
	}

	/**
	 * 隣接リスト表現の {@link Digraph} インスタンスに変換する。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return {@link Digraph} インスタンス
	 */
	public Digraph toDigraph() {
		Digraph g = new Digraph(N);
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				if (adj[i][j]) {
					g.addEdge(i, j);
				}
			}
		}
		return g;
	}

	/**
	 * 有向グラフの深いコピーを生成して返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return ディープコピー
	 */
	public AdjacencyMatrixDigraph copy() {
		AdjacencyMatrixDigraph ret = new AdjacencyMatrixDigraph(N);
		ret.M = this.M;
		for (int i = 0; i < N; i++) {
			System.arraycopy(this.adj[i], 0, ret.adj[i], 0, N);
		}
		return ret;
	}

	/**
	 * ワーシャルフロイド法により推移閉包 (reachable[i][j] == true ⇔ i から j へ到達可能) を求める。
	 * <ul>
	 *   <li>計算量: O(N^3)</li>
	 * </ul>
	 * @return 到達可能性行列
	 */
	public boolean[][] transitiveClosure() {
		boolean[][] reach = new boolean[N][N];
		for (int i = 0; i < N; i++) {
			System.arraycopy(adj[i], 0, reach[i], 0, N);
			reach[i][i] = true;
		}
		for (int k = 0; k < N; k++) {
			for (int i = 0; i < N; i++) {
				if (!reach[i][k]) continue;
				for (int j = 0; j < N; j++) {
					reach[i][j] |= reach[k][j];
				}
			}
		}
		return reach;
	}

	/**
	 * トポロジカルソート順に並んだ頂点配列を返す。
	 * DAGでない場合は null を返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return トポロジカルソート順配列、DAG でない場合は null
	 */
	public int[] topologicalOrder() {
		int[] inDegs = inDegrees();
		Queue<Integer> que = new ArrayDeque<>();
		for (int i = 0; i < N; i++) {
			if (inDegs[i] == 0) que.add(i);
		}
		int[] order = new int[N];
		int p = 0;
		while (!que.isEmpty()) {
			int v = que.poll();
			order[p++] = v;
			for (int u = 0; u < N; u++) {
				if (adj[v][u]) {
					inDegs[u]--;
					if (inDegs[u] == 0) {
						que.add(u);
					}
				}
			}
		}
		if (p != N) return null;
		return order;
	}

	/**
	 * 有向グラフが DAG (有向非巡回グラフ) であるか判定する。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return DAG であれば true
	 */
	public boolean isDAG() {
		return topologicalOrder() != null;
	}

	public void dump() {
		System.out.println("N=" + N + " M=" + M);
		for (int i = 0; i < N; i++) {
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < N; j++) {
				sb.append(adj[i][j] ? '1' : '0');
			}
			System.out.println(sb);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AdjacencyMatrixDigraph other)) return false;
		if (N != other.N || M != other.M) return false;
		for (int i = 0; i < N; i++) {
			if (!Arrays.equals(adj[i], other.adj[i])) return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 31 * N + M;
		for (int i = 0; i < N; i++) {
			result = 31 * result + Arrays.hashCode(adj[i]);
		}
		return result;
	}

	private void checkVertexBounds(int v) {
		if (v < 0 || v >= N) {
			throw new IndexOutOfBoundsException("Vertex index out of bounds: " + v);
		}
	}
}
