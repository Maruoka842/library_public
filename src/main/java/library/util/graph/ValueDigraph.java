package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 辺の重みを型 T で持つ有向グラフを表すクラスです。
 * @param <T> 辺の重みの型
 */
public class ValueDigraph<T> {
	public int N;
	public int M;
	public ArrayList<ValueEdge<T>>[] adj;
	
	@SuppressWarnings("unchecked")
	public ValueDigraph(int N) {
		this.N = N;
		this.adj = new ArrayList[N];
		for (int i = 0; i < N; i++) {
			adj[i] = new ArrayList<>();
		}
	}

	/**
	 * この有向グラフと同じ頂点集合および重み付き辺集合を持つ有向グラフを返す。
	 * 隣接リストと辺レコードは共有しないが、各辺の重みへの参照は共有する。
	 * O(N + M)
	 *
	 * @return この有向グラフの構造的コピー
	 */
	// 未テスト
	public ValueDigraph<T> copy() {
		ValueDigraph<T> ret = new ValueDigraph<>(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			for (ValueEdge<T> e : adj[i]) {
				ret.adj[i].add(new ValueEdge<>(e.src(), e.dst(), e.weight()));
			}
		}
		return ret;
	}

	/**
	 * 有向辺を追加します。
	 * @param from 始点
	 * @param to 終点
	 * @param weight 辺の重み
	 */
	public void addEdge(int from, int to, T weight) {
		if (from < 0 || from >= N || to < 0 || to >= N) {
			throw new IndexOutOfBoundsException();
		}
		adj[from].add(new ValueEdge<>(from, to, weight));
		M++;
	}

	/**
	 * 全ての辺のリストを返します。
	 * @return 辺のリスト
	 */
	public List<ValueEdge<T>> edges() {
		List<ValueEdge<T>> edges = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			edges.addAll(adj[i]);
		}
		return edges;
	}
	
	/**
	 * 辺重みを取り払った有向グラフを返す
	 * @return
	 */
	public Digraph rawDiraph() {
		Digraph h=new Digraph(N);
		for (int i = 0; i < N; i++) {
			for (var e : adj[i]) {
				h.addEdge(e.src(), e.dst());
			}
		}
		return h;
	}

	/**
	 * 各弱連結成分を誘導部分グラフとして独立した新たな ValueDigraph インスタンスに再構築し、その配列を返す。
	 * 辺の向きを無視した連結成分（弱連結成分）ごとに分解し、各成分の頂点番号は 0 以上 各成分の頂点数未満 にリラベルされる。
	 * 連結成分の順序は各成分に含まれる最小頂点番号の昇順となる。
	 * 各有向辺の向き、重み（参照）、多重辺、自己ループを含むすべての辺情報が各部分グラフに保持される。
	 *
	 * 計算量: O(N + M alpha(N))
	 *
	 * @return 各弱連結成分を表す ValueDigraph の配列
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public ValueDigraph<T>[] connectedComponents() {
		library.util.unionfind.UnionFind uf = new library.util.unionfind.UnionFind(N);
		for (int u = 0; u < N; u++) {
			for (ValueEdge<T> e : adj[u]) {
				uf.union(u, e.dst());
			}
		}
		int[] compId = new int[N];
		Arrays.fill(compId, -1);
		int k = 0;
		for (int i = 0; i < N; i++) {
			int root = uf.root(i);
			if (compId[root] == -1) {
				compId[root] = k++;
			}
			compId[i] = compId[root];
		}
		ArrayList<Integer>[] comps = new ArrayList[k];
		for (int i = 0; i < k; i++) comps[i] = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			comps[compId[i]].add(i);
		}
		ValueDigraph<T>[] res = new ValueDigraph[k];
		int[] mapping = new int[N];
		for (int c = 0; c < k; c++) {
			ArrayList<Integer> comp = comps[c];
			int sz = comp.size();
			for (int i = 0; i < sz; i++) {
				mapping[comp.get(i)] = i;
			}
			ValueDigraph<T> g = new ValueDigraph<>(sz);
			for (int u : comp) {
				int newU = mapping[u];
				for (ValueEdge<T> e : adj[u]) {
					g.addEdge(newU, mapping[e.dst()], e.weight());
				}
			}
			res[c] = g;
		}
		return res;
	}
}
