package library.util.graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.function.DoubleToLongFunction;
import java.util.function.IntPredicate;

import library.util.collections.IntArrayList;
import library.util.collections.IntDeque;

/**
 * コンストラクタ呼び出しの時点で、サイクルの頂点の列挙とorderの計算を行う。
 */
public class FunctionalGraph {
	public int N;
	public int[] parent;
	public ArrayList<ArrayList<Integer>> cycles;
	public boolean[] isCycleVertice;
	int[] order;//葉を貪欲に取り除いて順に並べ、その後、サイクルの頂点を並べたもの。
	public int LOG=64;//2^LOG未満の値を探索可能
	/**
	 * parentOf2powers[i][j] は j に parent を i 回適用した頂点 ( 0 <= i < 64 )
	 */
	public int[][] parentOf2powers;
	
	public FunctionalGraph(int[] parent) {
		this.N = parent.length;
		this.parent = parent;
		build();
	}

	/**
	 * parent'(v) = parent(v) を満たす functional graph を返す。
	 * 親配列および構築済みのダブリング表を共有しない。
	 * O(N log K)。K は構築済みのダブリング表が扱う最大反復回数であり、未構築なら O(N)。
	 *
	 * @return この functional graph の深いコピー
	 */
	// 未テスト
	public FunctionalGraph copy() {
		FunctionalGraph ret = new FunctionalGraph(parent.clone());
		ret.LOG = LOG;
		if (parentOf2powers != null) {
			ret.buildDoubling(LOG);
		}
		return ret;
	}
	
	public void buildDoubling(int LOG) {
		this.LOG = LOG;
		parentOf2powers = new int[LOG][N];
		parentOf2powers[0] = parent;
		for (int i = 0; i + 1 < LOG; ++i) {
			for (int j = 0; j < N; ++j) {
				parentOf2powers[i + 1][j] = parentOf2powers[i][parentOf2powers[i][j]];
			}
		}
	}
	
	public void buildDoubling() {
		buildDoubling(64);
	}
	
	public int getPower2LevelAncestor(int v, int loglevel) {
		if(parentOf2powers == null) buildDoubling();
		return parentOf2powers[loglevel][v];
	}
	
	/**
	 *  vertexのrepeat個上の祖先を返す。
	 * @param vertex
	 * @param repeat
	 * @return
	 */
	public int getLevelAncestor(int vertex, long repeat) {
		if(repeat<0)throw new AssertionError();
		if(parentOf2powers == null) buildDoubling();
		for (int i = 0; i < LOG; ++i) {
			if (repeat % 2 == 1) {
				vertex = parentOf2powers[i][vertex];
			}
			repeat /= 2;
		}
		return vertex;
	}
	
	private void build() {
		int[] inDegree = new int[N];
		order = new int[N];
		int cnt = 0;
		for (int i = 0; i < N; ++i) inDegree[parent[i]]++;
		Queue<Integer> que = new ArrayDeque<>();
		for (int i = 0; i < N; ++i) {
			if (inDegree[i] == 0) {
				que.add(i);
			}
		}
		while (!que.isEmpty()) {
			int v = que.poll();
			int u = parent[v];
			order[cnt++] = v;
			inDegree[u]--;
			if (inDegree[u] == 0)  {
				que.add(u);
			}
		}
		cycles = new ArrayList<>();
		isCycleVertice = new boolean[N];
		for (int i = 0; i< N; ++i) {
			if (inDegree[i] == 1) {
				ArrayList<Integer> cycle = new ArrayList<>();
				int v = i;
				do {
					cycle.add(v);
					order[cnt++] = v;
					isCycleVertice[v] = true;
					inDegree[parent[v]]--;
					v = parent[v];
				} while (v != i);
				cycles.add(cycle);
			}
		}
		
	}
	
	
	/**
	 * 葉を貪欲に取り除いて順に並べ、その後、サイクルの頂点を並べたもの。
	 * @return
	 */
	public int[] order() {
		return order;
	}
	
	/**
	 * 部分木のサイズを頂点ごとに並べた配列を返す
	 * @return
	 */
	public int[] subtreeSizes() {
		int[]size=new int[N];
		Arrays.fill(size, 1);
		for (int i:order()) {
			if (!isCycleVertice[i]) {
				size[parent[i]]+=size[i];
			}
		}
		return size;
	}

	/**
	 * 始点集合 {@code sources} から、有向辺 v -> parent[v] に沿って距離 {@code cutoffDistance} 以内で到達可能な頂点列を BFS 順に返す。
	 * {@code dist} は BFS の途中状態の距離配列と見なし、{@code sources} の各頂点 {@code s} について
	 * {@code dist[s]} を上書きせずにそのまま探索の初期距離として使用する。
	 * 遷移先頂点 {@code u} について {@code dist[u] > d + 1} のときのみ {@code dist[u] = d + 1} と更新して探索を継続する。
	 *
	 * <p>計算量: 到達可能な頂点数を V として O(V)。空間計算量 O(V)。</p>
	 *
	 * @param sources 始点集合
	 * @param dist 各頂点への最短距離を記録する配列（呼び出し前の各始点 {@code s} の {@code dist[s]} はそのまま用いられる）
	 * @param cutoffDistance 探索する最大距離（負数の場合は空配列を返す）
	 * @return 到達可能な頂点を BFS 順に格納した配列
	 */
	// 未テスト
	public int[] bfs(int[] sources, int[] dist, int cutoffDistance) {
		if (cutoffDistance < 0 || sources == null || sources.length == 0 || N == 0) {
			return new int[0];
		}
		IntDeque que = new IntDeque();
		IntArrayList res = new IntArrayList();

		int baseDist = dist[sources[0]];
		for (int s : sources) {
			if (dist[s] != baseDist) {
				throw new AssertionError("sources have inconsistent distances");
			}
		}

		if (baseDist > cutoffDistance) {
			return new int[0];
		}

		for (int s : sources) {
			que.addLast(s);
			res.add(s);
		}

		while (que.isNonEmpty()) {
			int v = que.pollFirst();
			int d = dist[v];
			if (d >= cutoffDistance) continue;
			int u = parent[v];
			if (u < 0 || u >= N) continue;
			if (dist[u] > d + 1) {
				dist[u] = d + 1;
				res.add(u);
				que.addLast(u);
			}
		}

		return res.toArray();
	}
	
	public void draw() {
		Digraph g = new Digraph(N);
		for (int i = 0; i < N; i++) {
			g.addEdge(i, parent[i]);
		}
		g.draw();
	}
	
}
