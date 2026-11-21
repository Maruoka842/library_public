package library.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class HashDigraph {
	Map<Integer, Set<Integer>> out;

	public HashDigraph() {
		out = new HashMap<>();
	}

	/**
	 * 孤立頂点0,1,..,N-1からなるグラフ
	 * @param N
	 */
	public HashDigraph(int N) {
		this();
		for (int i = 0; i < N; i++) addVertex(i);
	}

	public int vertexCount() {
		return out.size();
	}

	public Set<Integer> vertices() {
		return out.keySet();
	}

	public void addVertex(int v) {
		if (out.containsKey(v)) throw new AssertionError("頂点" + v + "は既に存在する");
		out.put(v, new HashSet<>());
	}

	public void addEdge(int u, int v) {
		if (!out.containsKey(v)) throw new AssertionError("頂点" + v + "は存在しない");
		out.get(u).add(v);
	}

	public void removeEdge(int u, int v) {
		out.get(u).remove(v);
	}

	public void removeVertex(int v) {
		if (!out.containsKey(v)) return;
		out.remove(v);
		for (Set<Integer> s : out.values()) s.remove(v);
	}

	/**
	 * 頂点keep, removeを縮約する。removeに接続していた辺はkeepに付け替えられ、
	 * 多重辺は1本にまとめられる。辺(keep, remove)や(remove, keep)、removeの自己ループは、
	 * keepの自己ループになる。
	 */
	public void contract(int keep, int remove) {
		if (keep == remove) {
			return;
		}
		out.get(keep).addAll(out.get(remove));
		out.remove(remove);
		for (Set<Integer> s : out.values()) {
			if (s.remove(remove)) s.add(keep);
		}
	}

	public HashDigraph copy() {
		HashDigraph g = new HashDigraph();
		for (int v : out.keySet()) g.addVertex(v);
		for (var e : out.entrySet()) {
			int u = e.getKey();
			for (int v : e.getValue()) g.addEdge(u, v);
		}
		return g;
	}
	
	public HashMap<Integer, Integer> inDegrees() {
		HashMap<Integer, Integer>ret=new HashMap<>();
		for (var entry : out.entrySet()) {
			ret.put(entry.getKey(), 0);
		}
		for (var entry : out.entrySet()) {
			for (int to : entry.getValue()) ret.merge(to, 1, Integer::sum);
		}
		return ret;
	}
	
	/**
	 * a < b <=> a から b への path が存在
	 * という条件の元で頂点の全順序 v[0] <= v[1] <= .. <= v[n-1] を定め、vを返す。 
	 * つまり小さい方から大きい方へ辺があるとする。
	 * DAGでない場合は null を返す。
	 * @return トポロジカルソート順に並んだ頂点配列。DAGでない場合は null
	 */
	public int[] topologicalOrder() {
		int N = vertexCount();
		var inDegrees = inDegrees();
		Queue<Integer> que = new ArrayDeque<>();
		for (int i : out.keySet()) {
			if (inDegrees.get(i) == 0) que.add(i);
		}
		int[] order = new int[N];
		int p = 0;
		while (!que.isEmpty()) {
			int v = que.poll();
			order[p++] = v;
			for (int u : out.get(v)) {
				inDegrees.merge(u, -1, Integer::sum);
				if (inDegrees.get(u) == 0) {
					que.add(u);
				}
			}
		}
		if (p != N) return null;
		return order;
	}
	
	public boolean isDAG() {
		int N = vertexCount();
		var inDegrees = inDegrees();
		Queue<Integer> que = new ArrayDeque<>();
		for (int i : out.keySet()) {
			if (inDegrees.get(i) == 0) que.add(i);
		}
		int p = 0;
		while (!que.isEmpty()) {
			int v = que.poll();
			p++;
			for (int u : out.get(v)) {
				inDegrees.merge(u, -1, Integer::sum);
				if (inDegrees.get(u) == 0) {
					que.add(u);
				}
			}
		}
		return p == N;
	}
	
	public boolean hasVertex(int v) {
		return out.containsKey(v);
	}

	/**
	 * 各弱連結成分を誘導部分グラフとして独立した新たな HashDigraph インスタンスに再構築し、その配列を返す。
	 * 辺の向きを無視した連結成分（弱連結成分）ごとに分解し、各成分の頂点番号は 0 以上 各成分の頂点数未満 にリラベルされる。
	 * 連結成分の順序は各成分に含まれる最小頂点番号の昇順となる。
	 *
	 * 計算量: O(N + M) 平均
	 *
	 * @return 各弱連結成分を表す HashDigraph の配列
	 */
	// 未テスト
	public HashDigraph[] connectedComponents() {
		Map<Integer, Set<Integer>> undir = new HashMap<>();
		for (int u : out.keySet()) {
			undir.put(u, new HashSet<>());
		}
		for (int u : out.keySet()) {
			for (int v : out.get(u)) {
				undir.get(u).add(v);
				if (undir.containsKey(v)) {
					undir.get(v).add(u);
				}
			}
		}
		Set<Integer> visited = new HashSet<>();
		List<List<Integer>> comps = new ArrayList<>();
		List<Integer> allVertices = new ArrayList<>(out.keySet());
		Collections.sort(allVertices);
		for (int s : allVertices) {
			if (visited.contains(s)) continue;
			List<Integer> comp = new ArrayList<>();
			ArrayDeque<Integer> q = new ArrayDeque<>();
			q.add(s);
			visited.add(s);
			while (!q.isEmpty()) {
				int v = q.poll();
				comp.add(v);
				for (int u : undir.get(v)) {
					if (!visited.contains(u)) {
						visited.add(u);
						q.add(u);
					}
				}
			}
			comps.add(comp);
		}
		int k = comps.size();
		HashDigraph[] res = new HashDigraph[k];
		for (int c = 0; c < k; c++) {
			List<Integer> comp = comps.get(c);
			int sz = comp.size();
			Map<Integer, Integer> mapping = new HashMap<>();
			for (int i = 0; i < sz; i++) {
				mapping.put(comp.get(i), i);
			}
			HashDigraph g = new HashDigraph(sz);
			for (int u : comp) {
				int newU = mapping.get(u);
				for (int v : out.get(u)) {
					g.addEdge(newU, mapping.get(v));
				}
			}
			res[c] = g;
		}
		return res;
	}
}