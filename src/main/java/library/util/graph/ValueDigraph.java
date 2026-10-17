package library.util.graph;

import java.util.ArrayList;
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
}
