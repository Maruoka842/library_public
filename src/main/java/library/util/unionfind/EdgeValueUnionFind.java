package library.util.unionfind;

import java.util.Arrays;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

import library.util.algebra.strategy.GroupStrategy;
import library.util.algebra.strategy.longs.LongGroupStrategy;
/**
 * https://judge.yosupo.jp/submission/355199
 * @param <T>
 */
public class EdgeValueUnionFind<T> extends UnionFind {
	T[] v;
	GroupStrategy<T> st;
	
	public EdgeValueUnionFind(int n, GroupStrategy<T> st) {
		super(n);
		v = (T[]) new Object[n];
		Arrays.fill(v, st.identity());
		this.st = st;
	}
	
	public int root(int x) {
		if (parent[x] < 0) return x;
		else {
			int par = parent[x];
			int r = root(par);
			v[x] = st.mul(v[par], v[x]);
			return parent[x]=r;
		}
	}
	
	public boolean isRoot(int x) {
		return parent[x] < 0;
	}
	
	public boolean equiv(int x, int y) {
		return root(x) == root(y);
	}
	
	/***
	 * 辺重みvによるroot-aパスの重みAとroot-bパスの重みBについて
	 * A = B * w となるように v[root(a)] を定め、root(b)をunion後のrootに。そのような v が存在すれば true, しなければ false を返す。
	 * A ≤ B + w を扱いたい場合は最短路双対へ。
	 * @param a
	 * @param b
	 * @param w
	 * @return
	 */
	public boolean union(int a, int b, T w) {
		int rootA=root(a);
		int rootB=root(b);
		if (rootA == rootB) {
			return st.equals(v[a], st.mul(v[b], w));
		}
		v[rootA] = st.mul(st.mul(v[b], w), st.inverse(v[a]));// v[rootA]v[a]== v[b] * w 親を新たに付け加える際は差分が保たれるように、左に掛ける
		parent[rootB]+=parent[rootA];
		parent[rootA]=rootB;
		numberConnectedComponents--;
		return true;
	}
	
	/**
	 * root-iパスの重みを返す(rootの重みは任意性があるが、identityとする)。
	 * @param i
	 * @return
	 */
	public T getValue(int i) {
		root(i);
		return v[i];
	}

	public void dump() {
		int n = parent.length;

		for (int r = 0; r < n; r++) {
			if (!isRoot(r)) continue;

			System.out.printf("component(root=%d, size=%d)%n", r, -parent[r]);

			for (int i = 0; i < n; i++) {
				if (root(i) == r) {
					System.out.printf("  %d : %s%n", i, getValue(i));
				}
			}
		}
	}
	
	public void draw() {
        System.setProperty("org.graphstream.ui", "swing");
		org.graphstream.graph.Graph graph = new SingleGraph("Graph");
		for (int i = 0; i < parent.length; ++i) {
			Node node = graph.addNode(String.valueOf(i));
	        String label = String.format("%d", i);
	        node.setAttribute("ui.label", label);
		}
		for (int i = 0; i < parent.length; ++i) {
	        if (parent[i] >= 0) {
			graph.addEdge(i+":"+parent[i], i, parent[i], true).setAttribute("ui.label", v[i]);
	        }
		}
        graph.setAttribute("ui.stylesheet",
		    "node { " +
		    "   fill-color: lightblue; " +
		    "   size: 30px; " +
		    "   text-alignment: center; " +
		    "   text-size: 20; " +
		    "   text-color: black; " +
		    "   text-mode: normal;" +
		    "}"
		    +
		    "edge { " +
		    "   text-size: 20px; " +
		    "   text-alignment: above; " +
		    "} "
		);
        graph.display();
	}

}
