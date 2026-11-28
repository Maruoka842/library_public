package library.util.graph.tree;

import library.util.unionfind.UnionFind;

public class DecrementalNearestMarkedAncestor {
	//https://atcoder.jp/contests/abc268/submissions/81047134
    private final int n;
    private final int[] parent;      // 元の木の親。根は n (番兵) を指す
    private final int[] label;   // 根 r の集合における、最も上の印付き頂点
    private final boolean[] marked;
    private final UnionFind uf;
    
    /** parent[root] = -1。全頂点が印付きの状態で始める。 */
    public DecrementalNearestMarkedAncestor(int[] parent) {
        this(parent, null);
    }

    /** marked[v] が false の頂点は最初から印なし。marked が null なら全頂点が印付き。 */
    public DecrementalNearestMarkedAncestor(int[] parent, boolean[] init) {
    	this.n = parent.length;
    	this.uf = new UnionFind(n + 1);
        this.parent = new int[n + 1];
        this.label = new int[n + 1];
        this.marked = new boolean[n + 1];
        for (int v = 0; v <= n; v++) {
            label[v] = v;
            marked[v] = true;
            if (v < n) this.parent[v] = parent[v] == -1 ? n : parent[v];
        }
        this.parent[n] = n;
        if (init != null) {
            for (int v = 0; v < n; v++) if (!init[v]) unmark(v);
        }
    }

    /** v の印を外す。すでに外れていれば何もしない。 */
    public void unmark(int v) {
        if (!marked[v]) return;
        marked[v] = false;
        int u = uf.root(parent[v]);
        int lab = label[u];
        uf.union(v, u);
        label[uf.root(v)] = lab;
    }

    /** v 自身を含む、最も近い印付き祖先。なければ -1。 */
    public int find(int v) {
        int r = label[uf.root(v)];
        return r == n ? -1 : r;
    }
}