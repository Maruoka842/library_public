package library.util.graph;

import java.util.Arrays;

import library.util.collections.IntArrayList;
import library.util.graph.tree.Tree;

/**
 * 有向グラフにおける Dominator Tree（支配木）を計算するクラス。
 * Lengauer-Tarjan のアルゴリズムを用いて、指定された始点 s から到達可能な全頂点に対する
 * 即時支配者（immediate dominator: idom）を求める。
 *
 * 頂点 u が頂点 v を支配する (u dom v) とは、s から v へのすべての有向パス上に u が存在することをいう。
 * v != s かつ v が s から到達可能であるとき、v の即時支配者 idom(v) は v 以外の v の支配者のうち
 * v に最も近い（支配木における親となる）頂点として一意に存在する。
 * s から到達不能な頂点 u については idom(u) = -1 と定義し、idom(s) = s と定義する。
 */
public class DominatorTree {
	// 頂点数
	private final int N;
	// 始点頂点
	private final int s;
	// idom[v] は頂点 v の即時支配者
	private final int[] idom;
	// 支配木における頂点 v の深さ
	private final int[] domDepth;
	// 支配木における 2^k 先の親を保持するダブリング配列
	private final int[][] domParent;
	// reachable[v] は頂点 v が s から到達可能かを表す
	private final boolean[] reachable;

	// DFS 訪問カウンター
	private int timer;
	// dfnum[v] は頂点 v の DFS 行き順（1-indexed）
	private final int[] dfnum;
	// vertex[i] は dfnum が i である頂点番号
	private final int[] vertex;
	// parent[v] は DFS 木における v の親
	private final int[] parent;
	// sdom[v] は v の semi-dominator の dfnum
	private final int[] sdom;
	// DSU の親配列
	private final int[] ancestor;
	// DSU の最小 sdom 頂点ラベル
	private final int[] label;

	/**
	 * 指定された有向グラフ g と始点 s に対する Dominator Tree を構築する。
	 *
	 * O((N + M) alpha(N, M))
	 *
	 * @param g 有向グラフ
	 * @param s 始点頂点
	 */
	// 未テスト
	public DominatorTree(Digraph g, int s) {
		if (s < 0 || s >= g.N) {
			throw new IllegalArgumentException("Invalid source vertex: " + s);
		}
		this.N = g.N;
		this.s = s;
		this.idom = new int[N];
		Arrays.fill(idom, -1);
		this.reachable = new boolean[N];

		this.dfnum = new int[N];
		this.vertex = new int[N + 1];
		this.parent = new int[N];
		this.sdom = new int[N];
		this.ancestor = new int[N];
		this.label = new int[N];

		IntArrayList[] rg = new IntArrayList[N];
		IntArrayList[] bucket = new IntArrayList[N + 1];
		for (int i = 0; i < N; i++) {
			rg[i] = new IntArrayList();
			bucket[i] = new IntArrayList();
		}
		bucket[N] = new IntArrayList();

		this.timer = 0;
		dfs(g, s, rg);

		int K = timer;
		if (K > 0) {
			for (int i = K; i >= 2; i--) {
				int w = vertex[i];

				for (int j = 0; j < rg[w].size(); j++) {
					int v = rg[w].get(j);
					if (dfnum[v] != 0) {
						int u = eval(v);
						if (sdom[u] < sdom[w]) {
							sdom[w] = sdom[u];
						}
					}
				}

				bucket[vertex[sdom[w]]].add(w);
				link(parent[w], w);

				IntArrayList pBucket = bucket[parent[w]];
				for (int j = 0; j < pBucket.size(); j++) {
					int v = pBucket.get(j);
					int u = eval(v);
					if (sdom[u] < sdom[v]) {
						idom[v] = u;
					} else {
						idom[v] = parent[w];
					}
				}
				pBucket.clear();
			}

			for (int i = 2; i <= K; i++) {
				int w = vertex[i];
				if (idom[w] != vertex[sdom[w]]) {
					idom[w] = idom[idom[w]];
				}
			}
			idom[s] = s;
			for (int i = 1; i <= K; i++) {
				reachable[vertex[i]] = true;
			}
		}

		this.domDepth = new int[N];
		int logN = 1;
		while ((1 << logN) <= N) logN++;
		this.domParent = new int[logN][N];
		for (int k = 0; k < logN; k++) {
			Arrays.fill(domParent[k], -1);
		}

		if (reachable[s]) {
			IntArrayList[] domChildren = new IntArrayList[N];
			for (int i = 0; i < N; i++) domChildren[i] = new IntArrayList();
			for (int i = 0; i < N; i++) {
				if (reachable[i] && i != s) {
					domChildren[idom[i]].add(i);
				}
			}

			IntArrayList q = new IntArrayList();
			q.add(s);
			domDepth[s] = 0;
			domParent[0][s] = s;

			int head = 0;
			while (head < q.size()) {
				int u = q.get(head++);
				for (int j = 0; j < domChildren[u].size(); j++) {
					int v = domChildren[u].get(j);
					domDepth[v] = domDepth[u] + 1;
					domParent[0][v] = u;
					q.add(v);
				}
			}

			for (int k = 1; k < logN; k++) {
				for (int i = 0; i < N; i++) {
					if (reachable[i] && domParent[k - 1][i] != -1) {
						domParent[k][i] = domParent[k - 1][domParent[k - 1][i]];
					}
				}
			}
		}
	}

	private void dfs(Digraph g, int s, IntArrayList[] rg) {
		int[] stack = new int[N];
		int[] edgeIdx = new int[N];
		int top = 0;

		timer++;
		dfnum[s] = timer;
		vertex[timer] = s;
		sdom[s] = timer;
		label[s] = s;
		ancestor[s] = 0;
		stack[top] = s;
		edgeIdx[s] = 0;

		while (top >= 0) {
			int u = stack[top];
			if (edgeIdx[u] < g.adj[u].size()) {
				int v = g.adj[u].get(edgeIdx[u]++);
				rg[v].add(u);
				if (dfnum[v] == 0) {
					timer++;
					dfnum[v] = timer;
					vertex[timer] = v;
					sdom[v] = timer;
					label[v] = v;
					ancestor[v] = 0;
					parent[v] = u;

					top++;
					stack[top] = v;
					edgeIdx[v] = 0;
				}
			} else {
				top--;
			}
		}
	}

	private int eval(int v) {
		if (ancestor[v] == 0) {
			return label[v];
		} else {
			compress(v);
			return label[v];
		}
	}

	private void compress(int v) {
		if (ancestor[ancestor[v]] != 0) {
			compress(ancestor[v]);
			if (sdom[label[ancestor[v]]] < sdom[label[v]]) {
				label[v] = label[ancestor[v]];
			}
			ancestor[v] = ancestor[ancestor[v]];
		}
	}

	private void link(int p, int w) {
		ancestor[w] = p;
	}

	/**
	 * 各頂点 v に対する即時支配者 idom[v] の配列を返す。
	 * 到達不能な頂点 u については idom[u] = -1、始点 s については idom[s] = s となる。
	 *
	 * O(N)
	 *
	 * @return 即時支配者の配列
	 */
	// 未テスト
	public int[] getIdom() {
		return idom.clone();
	}

	/**
	 * 頂点 v の即時支配者 idom(v) を返す。
	 * 到達不能な頂点 v については -1、始点 s については s を返す。
	 *
	 * O(1)
	 *
	 * @param v 頂点番号
	 * @return 頂点 v の即時支配者
	 */
	// 未テスト
	public int idom(int v) {
		if (v < 0 || v >= N) {
			throw new IllegalArgumentException("Vertex index out of bounds: " + v);
		}
		return idom[v];
	}

	/**
	 * 頂点 v が始点 s から到達可能か判定する。
	 *
	 * O(1)
	 *
	 * @param v 頂点番号
	 * @return 到達可能な場合 true、到達不能な場合 false
	 */
	// 未テスト
	public boolean isReachable(int v) {
		if (v < 0 || v >= N) {
			throw new IllegalArgumentException("Vertex index out of bounds: " + v);
		}
		return reachable[v];
	}

	/**
	 * 頂点 u が頂点 v を支配する（s から v へのすべての有向パス上に u が存在する）か判定する。
	 * u と v の両方が到達可能で、支配木において u が v の祖先（u == v を含む）である場合 true を返す。
	 *
	 * O(log N)
	 *
	 * @param u 支配者候補の頂点
	 * @param v 支配される側の頂点
	 * @return u が v を支配する場合 true、それ以外は false
	 */
	// 未テスト
	public boolean isDominator(int u, int v) {
		if (u < 0 || u >= N || v < 0 || v >= N) {
			throw new IllegalArgumentException("Vertex index out of bounds: u=" + u + ", v=" + v);
		}
		if (!reachable[u] || !reachable[v]) return false;
		if (u == v) return true;
		if (domDepth[u] > domDepth[v]) return false;

		int diff = domDepth[v] - domDepth[u];
		int curr = v;
		for (int k = domParent.length - 1; k >= 0; k--) {
			if ((diff & (1 << k)) != 0) {
				curr = domParent[k][curr];
			}
		}
		return curr == u;
	}

	/**
	 * 頂点 v のすべての支配者を始点 s から v への順序で並べた配列を返す。
	 * v が到達不能な場合は空配列を返す。
	 *
	 * O(k) (k は支配者の個数 <= N)
	 *
	 * @param v 頂点番号
	 * @return s から v への支配者順列の配列
	 */
	// 未テスト
	public int[] getDominators(int v) {
		if (v < 0 || v >= N) {
			throw new IllegalArgumentException("Vertex index out of bounds: " + v);
		}
		if (!reachable[v]) return new int[0];

		IntArrayList list = new IntArrayList();
		int curr = v;
		while (curr != s) {
			list.add(curr);
			curr = idom[curr];
		}
		list.add(s);
		list.reverse();
		return list.toArray();
	}

	/**
	 * 始点 s を根とする Dominator Tree（支配木）を表す Tree インスタンスを構築して返す。
	 * s から到達不能な頂点がある場合、それらは根 s の直下の子として接続される。
	 *
	 * O(N)
	 *
	 * @return 支配木を表す Tree オブジェクト
	 */
	// 未テスト
	public Tree dominatorTree() {
		Tree tree = new Tree(N);
		for (int i = 0; i < N; i++) {
			if (i == s) continue;
			if (reachable[i]) {
				tree.addEdge(idom[i], i);
			} else {
				tree.addEdge(s, i);
			}
		}
		tree.rooted(s);
		return tree;
	}
}
