package library.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * 辺の重みを型 T で持つ無向グラフを表すクラスです。
 * @param <T> 辺の重みの型
 */
public class ValueGraph<T> {
	/** 頂点数 */
	public int N;
	/** 辺数 */
	public int M;
	/** 隣接リスト */
	public ArrayList<ValueEdge<T>>[] adj;

	/**
	 * 頂点数 N の無向グラフを構築します。
	 * @param N 頂点数
	 * @complexity O(N)
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public ValueGraph(int N) {
		this.N = N;
		this.adj = new ArrayList[N];
		for (int i = 0; i < N; i++) {
			adj[i] = new ArrayList<>();
		}
	}

	/**
	 * このグラフと同じ頂点集合および重み付き辺集合を持つグラフを返す。
	 * 隣接リストと辺レコードは共有しないが、各辺の重みへの参照は共有する。
	 * O(N + M)
	 *
	 * @return このグラフの構造的コピー
	 */
	// 未テスト
	public ValueGraph<T> copy() {
		ValueGraph<T> ret = new ValueGraph<>(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			for (ValueEdge<T> e : adj[i]) {
				ret.adj[i].add(new ValueEdge<>(e.src(), e.dst(), e.weight()));
			}
		}
		return ret;
	}

	/**
	 * 無向辺を追加します。
	 * @param from 一方の端点
	 * @param to もう一方の端点
	 * @param weight 辺の重み
	 * @complexity O(1)
	 */
	// 未テスト
	public void addEdge(int from, int to, T weight) {
		if (from < 0 || from >= N || to < 0 || to >= N) {
			throw new IndexOutOfBoundsException();
		}
		adj[from].add(new ValueEdge<>(from, to, weight));
		if (from != to) {
			adj[to].add(new ValueEdge<>(to, from, weight));
		}
		M++;
	}

	/**
	 * e.src <= e.dst を満たす全ての辺のリストを返します。
	 * @return 辺のリスト
	 * @complexity O(N + M)
	 */
	// 未テスト
	public List<ValueEdge<T>> edges() {
		List<ValueEdge<T>> edges = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			for (ValueEdge<T> e : adj[i]) {
				if (e.dst() >= i) {
					edges.add(e);
				}
			}
		}
		return edges;
	}

	/**
	 * 各連結成分を誘導部分グラフとして独立した新たな ValueGraph インスタンスに再構築し、その配列を返す。
	 * 各成分の頂点番号は 0 以上 各成分の頂点数未満 にリラベルされる。
	 * 連結成分の順序は各成分に含まれる最小頂点番号の昇順となる。
	 * 辺の重み（オブジェクト参照）、多重辺、自己ループを含むすべての辺情報が各部分グラフに保持される。
	 *
	 * 計算量: O(N + M)
	 *
	 * @return 各連結成分を表す ValueGraph の配列
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public ValueGraph<T>[] connectedComponents() {
		int[] color = new int[N];
		Arrays.fill(color, -1);
		Queue<Integer> que = new ArrayDeque<>();
		int k = 0;
		for (int i = 0; i < N; i++) {
			if (color[i] != -1) continue;
			que.add(i);
			color[i] = k;
			while (!que.isEmpty()) {
				int v = que.poll();
				for (ValueEdge<T> e : adj[v]) {
					int u = e.dst();
					if (color[u] != -1) continue;
					color[u] = k;
					que.add(u);
				}
			}
			k++;
		}
		ArrayList<Integer>[] comps = new ArrayList[k];
		for (int i = 0; i < k; i++) comps[i] = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			comps[color[i]].add(i);
		}
		ValueGraph<T>[] res = new ValueGraph[k];
		int[] mapping = new int[N];
		for (int c = 0; c < k; c++) {
			ArrayList<Integer> comp = comps[c];
			int sz = comp.size();
			for (int i = 0; i < sz; i++) {
				mapping[comp.get(i)] = i;
			}
			ValueGraph<T> g = new ValueGraph<>(sz);
			for (int u : comp) {
				int newU = mapping[u];
				for (ValueEdge<T> e : adj[u]) {
					int v = e.dst();
					if (u < v) {
						g.addEdge(newU, mapping[v], e.weight());
					} else if (u == v) {
						g.addEdge(newU, newU, e.weight());
					}
				}
			}
			res[c] = g;
		}
		return res;
	}
}
