package library.util.graph.tree;

import library.util.graph.*;

import library.tools.FastScanner;

public class DoubleValueForest extends DoubleValueGraph {

	public DoubleValueForest(int N) {
		super(N);
	}

	/**
	 * この森と同じ頂点集合および重み付き辺集合を持つ森を返す。
	 * 辺オブジェクトと隣接リストを共有しない。
	 * O(N + M)
	 *
	 * @return この森の深いコピー
	 */
	@Override
	// 未テスト
	public DoubleValueForest copy() {
		DoubleValueForest ret = new DoubleValueForest(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			for (DoubleValueEdge e : adj[i]) {
				ret.adj[i].add(new DoubleValueEdge(e.src, e.dst, e.cost));
			}
		}
		return ret;
	}

	@Override
	public void addEdge(int u, int v, double cost) {
		super.addEdge(u, v, cost);
		if (u == v) throw new AssertionError("森で自己ループは禁止");
	}

	public static DoubleValueForest read(int N, int M) {
		DoubleValueForest forest = new DoubleValueForest(N);
		FastScanner sc = FastScanner.getInstance();
		for (int i = 0; i < M; i++) {
			int a = sc.nextInt() - 1;
			int b = sc.nextInt() - 1;
			double c = sc.nextDouble();
			forest.addEdge(a, b, c);
		}
		return forest;
	}
}
