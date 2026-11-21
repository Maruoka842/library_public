package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PrimitiveIterator;

public class StaticGraph {
	public int N;
	public int M;
	private int[] to;
	private int[] deg;
	private int[] start;
	ArrayList<int[]> edges;
	
	public StaticGraph(int N) {
		this.N = N;
		edges=new ArrayList<>();
	}

	/**
	 * このグラフと同じ頂点集合、追加済みの辺および構築済みの隣接配列を持つグラフを返す。
	 * 内部の辺配列と隣接配列を共有しない。
	 * O(N + M)
	 *
	 * @return このグラフの深いコピー
	 */
	// 未テスト
	public StaticGraph copy() {
		StaticGraph ret = new StaticGraph(N);
		ret.M = M;
		for (int[] edge : edges) {
			ret.edges.add(edge.clone());
		}
		ret.to = to == null ? null : to.clone();
		ret.deg = deg == null ? null : deg.clone();
		ret.start = start == null ? null : start.clone();
		return ret;
	}
	
	public void setN(int N) {
		this.N = N;
	}
	
	public void addEdge(int a, int b) {
		edges.add(new int[] {a, b});
	}
	
	/**
	 * 自己辺は未対応
	 */
	public void build() {
		this.M = edges.size();
		to = new int[2 * edges.size()];
		deg=new int[N];
		for (int[] e : edges) {
			deg[e[0]]++;
			deg[e[1]]++;
		}
		start = new int[N];
		for (int i = 1; i < deg.length; i++) {
			start[i] += start[i - 1] + deg[i - 1];
		}
		int[] id = Arrays.copyOf(start, start.length);
		for (int[] e : edges) {
			to[id[e[0]]] = e[1];
			id[e[0]]++;
			to[id[e[1]]] = e[0];
			id[e[1]]++;
		}
	}
	
	
	public int deg(int i) {
		return deg[i];
	}
	
	public Iterable<Integer> adj(int v) {
		int begin = start[v];
		int end = (v + 1 < N ? start[v + 1] : M);
		return () -> new PrimitiveIterator.OfInt() {
			int cur = begin;
			
			@Override
			public boolean hasNext() {
				return cur < end;
			}
			
			@Override
			public int nextInt() {
				return to[cur++];
			}
		};
	}
	
	/**
	 * 各連結成分を誘導部分グラフとして独立した新たな StaticGraph インスタンスに再構築し、その配列を返す。
	 * 各成分の頂点番号は 0 以上 各成分の頂点数未満 にリラベルされる。
	 * 連結成分の順序は各成分に含まれる最小頂点番号の昇順となる。
	 * 返される各グラフは build() 済みの状態となる。
	 *
	 * 計算量: O(N + M alpha(N))
	 *
	 * @return 各連結成分を表す StaticGraph の配列
	 */
	// 未テスト
	public StaticGraph[] connectedComponents() {
		library.util.unionfind.UnionFind uf = new library.util.unionfind.UnionFind(N);
		for (int[] e : edges) {
			uf.union(e[0], e[1]);
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
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] comps = new ArrayList[k];
		for (int i = 0; i < k; i++) comps[i] = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			comps[compId[i]].add(i);
		}
		int[] mapping = new int[N];
		StaticGraph[] res = new StaticGraph[k];
		for (int c = 0; c < k; c++) {
			ArrayList<Integer> comp = comps[c];
			int sz = comp.size();
			for (int i = 0; i < sz; i++) {
				mapping[comp.get(i)] = i;
			}
			res[c] = new StaticGraph(sz);
		}
		for (int[] e : edges) {
			int c = compId[e[0]];
			res[c].addEdge(mapping[e[0]], mapping[e[1]]);
		}
		for (int c = 0; c < k; c++) {
			res[c].build();
		}
		return res;
	}

	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
