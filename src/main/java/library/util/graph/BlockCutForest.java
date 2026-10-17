package library.util.graph;

import java.util.Arrays;

import library.util.collections.IntArrayList;
import library.util.graph.tree.Forest;

/**
 * https://twitter.com/noshi91/status/1529858538650374144?s=20&t=eznpFbuD9BDhfTb4PplFUg
 * https://judge.yosupo.jp/submission/345098
 */
public class BlockCutForest extends Forest {
    Graph g;

    int numberOfConnectedComponents = 0;

    public BlockCutForest(Graph g) {
		this(g, true);
    }

	/**
	 * この block-cut forest と同じ頂点集合および辺集合を持つ block-cut forest を返す。
	 * 元グラフ、隣接リストおよび構築用の内部配列を共有しない。
	 * O(N + M)。N, M は元グラフと block-cut forest の頂点数・辺数の合計で評価する。
	 *
	 * @return この block-cut forest の深いコピー
	 */
	@Override
	// 未テスト
	public BlockCutForest copy() {
		BlockCutForest ret = new BlockCutForest(g.copy(), false);
		ret.N = N;
		ret.M = M;
		ret.adj = new IntArrayList[N];
		for (int i = 0; i < N; i++) {
			ret.adj[i] = adj[i].copy();
		}
		ret.numberOfConnectedComponents = numberOfConnectedComponents;
		ret.buffer = buffer == null ? null : buffer.copy();
		ret.nxtCutNode = nxtCutNode;
		ret.min = min == null ? null : min.clone();
		ret.vis = vis == null ? null : vis.clone();
		ret.depth = depth == null ? null : depth.clone();
		if (isRooted()) {
			int firstRoot = 0;
			while (firstRoot < N && !isRoot(firstRoot)) {
				firstRoot++;
			}
			ret.rooted(firstRoot);
		}
		return ret;
	}

	/**
	 * build が真なら g の block-cut forest を構築し、偽なら空のコピー先を構築する。
	 * build が真なら O(g.N + g.M)、偽なら O(1)。
	 *
	 * @param g 対応する元グラフ
	 * @param build block-cut forest を直ちに構築するか
	 */
	// 未テスト
	private BlockCutForest(Graph g, boolean build) {
		super(build ? 2 * g.N : 0);
		this.g = g;
		if (build) {
			build();
		}
	}
    /**
     * dfsをそのまま実装すると再帰が深いケースで4倍ぐらい時間が掛かるので仕方なく非再帰に....
     */
    void build() {
    	buffer = new IntArrayList(g.N);
    	nxtCutNode = g.N;
        boolean[] vis = new boolean[g.N];
        int[] depth = new int[g.N];
        int[] min = new int[g.N];
        int[] parent = new int[g.N];
        int[] it = new int[g.N];// 次に見る隣接辺
        for (int root = 0; root < g.N; root++) {
            if (vis[root]) {
                continue;
            }
            ++numberOfConnectedComponents;
            IntArrayList stack = new IntArrayList();
            stack.add(root);
            parent[root] = -1;
            depth[root] = 0;
            int rootChilds = 0;
            while (stack.isNonEmpty()) {
            	int v = stack.get(stack.size() - 1);
                if (it[v] == 0) {
                	buffer.add(v);
                    vis[v] = true;
                    min[v] = depth[v];
                }
                if (it[v] < g.adj[v].size()) {
                    int u = g.adj[v].get(it[v]++);
                    if (u == parent[v]) {
                        continue;
                    }
                    if (v == root) rootChilds++;
                    if (vis[u]) {
                        min[v] = Math.min(min[v], depth[u]);
                    } else {
                        parent[u] = v;
                        depth[u] = depth[v] + 1;
                        stack.add(u);
                    }
                } else {
                    stack.pollLast();
                    if (v != root) {
                        min[parent[v]] = Math.min(min[parent[v]], min[v]);
                        
                        //root≠parent[v]の下でparent[v]がvに対してcutNodeである場合
                        //root=parent[v]の下でparent[v]がvに対してcutNodeで、vが2番目以降の子である場合
                        if ((parent[v] != root && min[v] >= depth[parent[v]]) || (parent[v] == root && rootChilds >= 2)) {
                            super.addEdge(nxtCutNode, parent[v]);
                            while (true) {
                            	int u = buffer.pollLast();
                            	super.addEdge(nxtCutNode, u);
                            	if (u == v) break;
                            }
                            nxtCutNode++;
                        }
                    }
                }
            }
            for (int v : buffer)
            	super.addEdge(nxtCutNode, v);
            nxtCutNode++;
            buffer.clear();
        }
        super.adj = Arrays.copyOf(super.adj, nxtCutNode);
        N = nxtCutNode;
    }
    

    void slowbuild() {
        buffer = new IntArrayList(g.N);
        nxtCutNode = g.N;
        min = new int[g.N];
        depth = new int[g.N];
        vis = new boolean[g.N];
        for (int i = 0; i < g.N; i++) {
            if (!vis[i]) {
                ++numberOfConnectedComponents;
                dfs(i, -1);
                if (!buffer.isEmpty()) {
                    // 根がcutNodeでない(子が0個か1個）場合、bufferに残っている
                    for (int j = 0; j < buffer.size(); j++) {
                        super.addEdge(nxtCutNode, buffer.get(j));
                    }
                    buffer.clear();
                    nxtCutNode++;
                }
            }
        }
        super.adj = Arrays.copyOf(super.adj, nxtCutNode);
        N = nxtCutNode;
    }

    IntArrayList buffer;

    int nxtCutNode;

    int[] min;

    boolean[] vis;

    int[] depth;

    // min[v]:=dfs木で, 辺(v,parent[v])を切った時、後退辺を高々１回使ってどの深さまで戻れるか。
    int dfs(int v, int p) {
        if (p != (-1)) {
            depth[v] = depth[p] + 1;
        }
        min[v] = depth[v];
        vis[v] = true;
        int allSubTreeMin = Integer.MAX_VALUE;
        int childs = 0;// 子の数

        for (int u : g.adj[v]) {
            if (u == p) {
                continue;
            }
            if (vis[u]) {
                // 後退辺。辺(v,parent[v])ではないのでminを更新。
                min[v] = Math.min(min[v], depth[u]);
            } else {
                int initialSize = buffer.size();
                childs++;
                int subTreeMin = dfs(u, v);
                allSubTreeMin = Math.min(allSubTreeMin, subTreeMin);
                if ((p != -1 && (min[u] >= depth[v])) || (p == -1 && childs >= 2)) {
                    // 子に対してvがcutNodeである場合　(v=root⇔p=-1で場合分け)
                    super.addEdge(nxtCutNode, v);
                    for (int i = initialSize; i < buffer.size(); i++) {
                        super.addEdge(nxtCutNode, buffer.get(i));
                    }
                    buffer.tail = initialSize;
                    nxtCutNode++;
                }
            }
        }
        buffer.add(v);
        min[v] = Math.min(min[v], allSubTreeMin);
        return min[v];
    }

    /**
     * https://judge.u-aizu.ac.jp/onlinejudge/review.jsp?rid=11228483#1
     *
     * @param v
     * @return  */
    public boolean isCutNode(int v) {
        if (!((0 <= v) && (v < g.N))) {
            throw new AssertionError();
        }
        return deg(v) >= 2;
    }

    public int numberOfConnectedComponents() {
        return numberOfConnectedComponents;
    }
}
