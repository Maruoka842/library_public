package library.util.graph;

import java.awt.Component;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.view.Viewer;

import library.util.collections.IntQueue;

public class SparseGraph{
	private Map<Integer, Set<Integer>> adj;
	private static int drawCount = 0; // 呼び出しカウンタ
	
	public SparseGraph() {
		adj = new HashMap<>();
	}

	public SparseGraph(int N) {
		adj = new HashMap<>();
		for (int i = 0; i < N; i++) {
			adj.put(i, new HashSet<>());
		}

	}
	
	public int N() {
		return adj.size();
	}
	
	public Set<Integer> adjacents(int v) {
		var ret = adj.get(v);
		if (ret == null) throw new AssertionError("頂点"+v+"は存在しない");
		return ret;
	}
	
	public Set<Integer> vertices() {
		return adj.keySet();
	}
	
	public boolean hasSelfLoop(int v) {
		return adj.get(v).contains(v);
	}
	
	public void addVertex(int v) {
		if (adj.containsKey(v)) throw new AssertionError();
		adj.put(v, new HashSet<>());
	}
	
	public void addEdge(int u, int v) {
		var setU=adj.get(u);
		setU.add(v);
		if (u == v) return;
		var setV=adj.get(v);
		setV.add(u);
	}
	
	public void removeVertex(int v) {
		for (int u:adj.get(v)) {
			if (u != v) adj.get(u).remove(v);
		}
		adj.remove(v);
	}
	
	public void removeEdge(int u, int v) {
		adj.get(u).remove(v);
		adj.get(v).remove(u);
	}

	/**
	 * 頂点keep, removeを縮約する。辺(keep, remove)があった場合、自己ループができる。
	 * 多重辺は多重でない辺に置き換えられる。
	 *
	 * <ul>
	 *   <li>事前条件: keep, removeはグラフ内の異なる頂点である。</li>
	 *   <li>事後条件: keepとremoveが1つの頂点に統合される。頂点removeに接続されていた辺はすべて頂点keepに接続し直され、頂点removeはグラフから削除される。</li>
	 *   <li>例外・未定義条件: keep == remove の場合、IllegalArgumentExceptionを投げる。</li>
	 *   <li>計算量: O(deg(remove))</li>
	 * </ul>
	 * 未テスト
	 * @param keep 残す側の頂点
	 * @param remove 削除する側の頂点
	 */
	public void contractVertexThenRemoveMultipleEdge(int keep, int remove) {
		if (keep == remove) {
			throw new IllegalArgumentException("keep and remove vertices must be different");
		}
		for (int x : adj.get(remove)) {
			addEdge(x, keep);
		}
		removeVertex(remove);
	}
	
	public SparseGraph copy() {
		SparseGraph g=new SparseGraph();
		for(int v:adj.keySet()) {
			g.adj.put(v, new HashSet<>());
			
		}
		for (var es:adj.entrySet()) {
			int u=es.getKey();
			for (int v:es.getValue()) {
				if(u<=v) {
					g.addEdge(u, v);
				}
			}
		}
		return g;
	}
	
	public int deg(int v) {
		return adj.get(v).size();
	}
	
	/**
	 * start から各頂点への最短距離を幅優先探索で求める。
	 *
	 * <p>到達不能な頂点は返り値に含まれない。
	 *
	 * <p>計算量: O(V' + E')。
	 * V', E' は start から到達可能な部分グラフの頂点数・辺数。
	 *
	 * @param start 探索開始頂点
	 * @return 各到達可能頂点から、その頂点への最短距離へのマップ
	 */
	public Map<Integer, Integer> bfsDistances(int start) {
		if (!adj.containsKey(start)) {
			throw new AssertionError("頂点" + start + "は存在しない");
		}

		Map<Integer, Integer> dist = new HashMap<>();
		IntQueue queue = new IntQueue();
		
		dist.put(start, 0);
		queue.add(start);

		while (!queue.isEmpty()) {
			int v = queue.poll();
			int nd = dist.get(v) + 1;

			for (int u : adj.get(v)) {
				if (!dist.containsKey(u)) {
					dist.put(u, nd);
					queue.add(u);
				}
			}
		}

		return dist;
	}

	/**
	 * グラフを描画する。
	 * <ul>
	 *   <li>事前条件: なし。</li>
	 *   <li>事後条件: グラフをSwingウィンドウに可視化して表示する。</li>
	 *   <li>計算量: O(V + E)</li>
	 * </ul>
	 * 未テスト
	 */
	public void draw() {
		System.setProperty("org.graphstream.ui", "swing");
		String graphName = "MySparseGraph-" + (++drawCount);
		org.graphstream.graph.Graph graph = new MultiGraph(graphName);

		for (int v : adj.keySet()) {
			graph.addNode(String.valueOf(v));
		}
		int edgeCnt = 0;
		for (int u : adj.keySet()) {
			Set<Integer> neighbors = adj.get(u);
			if (neighbors != null) {
				for (int v : neighbors) {
					if (u <= v) {
						graph.addEdge(String.valueOf(edgeCnt++), String.valueOf(u), String.valueOf(v), false);
					}
				}
			}
		}
		for (Node node : graph) {
			node.setAttribute("ui.label", node.getId());
		}
		graph.setAttribute("ui.stylesheet",
			"node { " +
			"   fill-color: lightblue; " +
			"   size: 30px; " +
			"   text-alignment: center; " +
			"   text-size: 20; " +
			"   text-color: black; " +
			"}"
		);
		Viewer viewer = graph.display();
		SwingUtilities.invokeLater(() -> {
			JFrame frame = (JFrame) SwingUtilities.getWindowAncestor((Component) viewer.getDefaultView());
			if (frame != null) {
				frame.setTitle(graphName);
			}
		});
	}
}
