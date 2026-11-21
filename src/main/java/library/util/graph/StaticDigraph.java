package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PrimitiveIterator;

public class StaticDigraph {
	public int N;
	public int M;
	public int[] to;
	private int[] outDeg;
	public int[] start;
	
	public StaticDigraph(int N, List<int[]> edges) {
		this.N = N;
		this.M = edges.size();
		to = new int[edges.size()];
		outDeg=new int[N];
		for (int[] e : edges) {
			outDeg[e[0]]++;
		}
		start = new int[N];
		for (int i = 1; i < outDeg.length; i++) {
			start[i] += start[i - 1] + outDeg[i - 1];
		}
		int[] id = Arrays.copyOf(start, start.length);
		for (int[] e : edges) {
			to[id[e[0]]] = e[1];
			id[e[0]]++;
		}
	}

	/**
	 * この有向グラフと同じ頂点集合および辺集合を持つ有向グラフを返す。
	 * 内部の隣接配列を共有しない。
	 * O(N + M)
	 *
	 * @return この有向グラフの深いコピー
	 */
	// 未テスト
	public StaticDigraph copy() {
		StaticDigraph ret = new StaticDigraph(0, new ArrayList<>());
		ret.N = N;
		ret.M = M;
		ret.to = to.clone();
		ret.outDeg = outDeg.clone();
		ret.start = start.clone();
		return ret;
	}
	
	
	public StaticDigraph(int N, List<int[]> edges, boolean reversed) {
		this.N = N;
		this.M = edges.size();
		to = new int[edges.size()];
		outDeg=new int[N];
		for (int[] e : edges) {
			if (!reversed) {
				outDeg[e[0]]++;
			}  else {
				outDeg[e[1]]++;
			}
		}
		start = new int[N];
		for (int i = 1; i < outDeg.length; i++) {
			start[i] += start[i - 1] + outDeg[i - 1];
		}
		int[] id = Arrays.copyOf(start, start.length);
		for (int[] e : edges) {
			if (!reversed) {
				to[id[e[0]]] = e[1];
				id[e[0]]++;
			} else {
				to[id[e[1]]] = e[0];
				id[e[1]]++;
			}
		}
	}
	
	public int outDegree(int i) {
		return outDeg[i];
	}
	
	public int[] outDegrees() {
		return Arrays.copyOf(outDeg, N);
	}
	
	public StaticDigraph reverse() {
		ArrayList<int[]> edges = new ArrayList<>(M);
		for (int i = 0; i < N; i++) {
			for (int v : adj(i)) {
				edges.add(new int[] {v, i});
			}
		}
		return new StaticDigraph(N, edges);
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
	
	
	@FunctionalInterface
	public interface IntIterable {
		PrimitiveIterator.OfInt iterator();
	}
	
	
	/**
	 * 各弱連結成分を誘導部分グラフとして独立した新たな StaticDigraph インスタンスに再構築し、その配列を返す。
	 * 辺の向きを無視した連結成分（弱連結成分）ごとに分解し、各成分の頂点番号は 0 以上 各成分の頂点数未満 にリラベルされる。
	 * 連結成分の順序は各成分に含まれる最小頂点番号の昇順となる。
	 *
	 * 計算量: O(N + M alpha(N))
	 *
	 * @return 各弱連結成分を表す StaticDigraph の配列
	 */
	// 未テスト
	public StaticDigraph[] connectedComponents() {
		library.util.unionfind.UnionFind uf = new library.util.unionfind.UnionFind(N);
		for (int u = 0; u < N; u++) {
			for (int v : adj(u)) {
				uf.union(u, v);
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
		@SuppressWarnings("unchecked")
		ArrayList<Integer>[] comps = new ArrayList[k];
		for (int i = 0; i < k; i++) comps[i] = new ArrayList<>();
		for (int i = 0; i < N; i++) {
			comps[compId[i]].add(i);
		}
		int[] mapping = new int[N];
		@SuppressWarnings("unchecked")
		List<int[]>[] compEdges = new List[k];
		for (int c = 0; c < k; c++) {
			compEdges[c] = new ArrayList<>();
			ArrayList<Integer> comp = comps[c];
			for (int i = 0; i < comp.size(); i++) {
				mapping[comp.get(i)] = i;
			}
		}
		for (int u = 0; u < N; u++) {
			int c = compId[u];
			for (int v : adj(u)) {
				compEdges[c].add(new int[] {mapping[u], mapping[v]});
			}
		}
		StaticDigraph[] res = new StaticDigraph[k];
		for (int c = 0; c < k; c++) {
			res[c] = new StaticDigraph(comps[c].size(), compEdges[c]);
		}
		return res;
	}

	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
