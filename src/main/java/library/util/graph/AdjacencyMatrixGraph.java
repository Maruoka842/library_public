package library.util.graph;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import library.util.collections.IntArrayList;

/**
 * 密な無向グラフを隣接行列 {@code boolean[][] adj} で表現するクラス。
 *
 * <p>数学的仕様:
 * 頂点集合を V = {0, 1, ..., N-1} とする。
 * 隣接行列 adj[u][v] は頂点 u と頂点 v の間に辺が存在するかどうかを表す。
 * 無向グラフであるため、常に adj[u][v] == adj[v][u] が保持される。
 */
// 未テスト
public class AdjacencyMatrixGraph {
	public int N;
	public int M;
	public boolean[][] adj;

	/**
	 * N 頂点 0 辺の密な無向グラフを構築する。
	 * <ul>
	 *   <li>事前条件: N >= 0</li>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @param N 頂点数
	 */
	public AdjacencyMatrixGraph(int N) {
		if (N < 0) throw new IllegalArgumentException("N must be non-negative");
		this.N = N;
		this.M = 0;
		this.adj = new boolean[N][N];
	}

	/**
	 * 指定された隣接行列 {@code adjcentMatrix} から密な無向グラフを構築する。
	 * 行列は正方行列であり、かつ対称（adjcentMatrix[i][j] == adjcentMatrix[j][i]）でなければならない。
	 * <ul>
	 *   <li>事前条件: {@code adjcentMatrix} は N x N の対称行列。</li>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @param adjcentMatrix 隣接行列
	 */
	public AdjacencyMatrixGraph(boolean[][] adjcentMatrix) {
		if (adjcentMatrix == null) throw new IllegalArgumentException("adjcentMatrix cannot be null");
		int n = adjcentMatrix.length;
		for (int i = 0; i < n; i++) {
			if (adjcentMatrix[i] == null || adjcentMatrix[i].length != n) {
				throw new IllegalArgumentException("adjcentMatrix must be a square matrix");
			}
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (adjcentMatrix[i][j] != adjcentMatrix[j][i]) {
					throw new IllegalArgumentException("adjcentMatrix must be symmetric for undirected graph");
				}
			}
		}
		this.N = n;
		this.adj = new boolean[n][n];
		int edgeCount = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				this.adj[i][j] = adjcentMatrix[i][j];
				if (adjcentMatrix[i][j] && i <= j) {
					edgeCount++;
				}
			}
		}
		this.M = edgeCount;
	}

	/**
	 * 無向辺 {u, v} を追加する。
	 * 既に辺が存在する場合は変化しない。
	 * <ul>
	 *   <li>事前条件: 0 <= u < N, 0 <= v < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param u 端点 u
	 * @param v 端点 v
	 */
	public void addEdge(int u, int v) {
		checkVertexBounds(u);
		checkVertexBounds(v);
		if (!adj[u][v]) {
			adj[u][v] = true;
			adj[v][u] = true;
			M++;
		}
	}

	/**
	 * 無向辺 {u, v} を削除する。
	 * 辺が存在しない場合は変化しない。
	 * <ul>
	 *   <li>事前条件: 0 <= u < N, 0 <= v < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param u 端点 u
	 * @param v 端点 v
	 */
	public void removeEdge(int u, int v) {
		checkVertexBounds(u);
		checkVertexBounds(v);
		if (adj[u][v]) {
			adj[u][v] = false;
			adj[v][u] = false;
			M--;
		}
	}

	/**
	 * 頂点 u と頂点 v の間に辺が存在するか判定する。
	 * <ul>
	 *   <li>事前条件: 0 <= u < N, 0 <= v < N</li>
	 *   <li>計算量: O(1)</li>
	 * </ul>
	 * @param u 頂点 u
	 * @param v 頂点 v
	 * @return 辺が存在する場合 true
	 */
	public boolean hasEdge(int u, int v) {
		checkVertexBounds(u);
		checkVertexBounds(v);
		return adj[u][v];
	}

	/**
	 * 頂点 v の次数（隣接する頂点数）を返す。
	 * 自己ループ {v, v} が存在する場合、次数への寄与は 1（adj[v][v] が true）とする。
	 * <ul>
	 *   <li>事前条件: 0 <= v < N</li>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * @param v 頂点 v
	 * @return 次数
	 */
	public int degree(int v) {
		checkVertexBounds(v);
		int deg = 0;
		for (int u = 0; u < N; u++) {
			if (adj[v][u]) deg++;
		}
		return deg;
	}

	/**
	 * 全頂点の次数配列を返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 各頂点の次数配列
	 */
	public int[] degrees() {
		int[] degs = new int[N];
		for (int i = 0; i < N; i++) {
			degs[i] = degree(i);
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
		checkVertexBounds(src);
		int INF = Integer.MAX_VALUE / 3;
		int[] dist = new int[N];
		Arrays.fill(dist, INF);
		dist[src] = 0;
		Queue<Integer> que = new ArrayDeque<>();
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
		checkVertexBounds(dst);
		int INF = Integer.MAX_VALUE / 3;
		if (dist == null || dst < 0 || dst >= N || dist[dst] >= INF || dist[dst] < 0) {
			return null;
		}
		int len = dist[dst] + 1;
		int[] path = new int[len];
		int curr = dst;
		path[len - 1] = curr;
		for (int i = len - 2; i >= 0; i--) {
			int prev = -1;
			for (int u = 0; u < N; u++) {
				if (adj[curr][u] && dist[u] == dist[curr] - 1) {
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
	 * 補グラフ（自己ループを除く辺の有無を反転させたグラフ）を生成して返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return 補グラフ
	 */
	public AdjacencyMatrixGraph complement() {
		AdjacencyMatrixGraph res = new AdjacencyMatrixGraph(N);
		for (int i = 0; i < N; i++) {
			for (int j = i + 1; j < N; j++) {
				if (!adj[i][j]) {
					res.addEdge(i, j);
				}
			}
		}
		return res;
	}

	/**
	 * 隣接リスト表現の {@link Graph} インスタンスに変換する。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return {@link Graph} インスタンス
	 */
	public Graph toGraph() {
		Graph g = new Graph(N);
		for (int i = 0; i < N; i++) {
			for (int j = i; j < N; j++) {
				if (adj[i][j]) {
					g.addEdge(i, j);
				}
			}
		}
		return g;
	}

	/**
	 * グラフの深いコピーを生成して返す。
	 * <ul>
	 *   <li>計算量: O(N^2)</li>
	 * </ul>
	 * @return ディープコピー
	 */
	public AdjacencyMatrixGraph copy() {
		AdjacencyMatrixGraph ret = new AdjacencyMatrixGraph(N);
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
		if (!(o instanceof AdjacencyMatrixGraph other)) return false;
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
