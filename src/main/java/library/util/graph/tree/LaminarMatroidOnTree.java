package library.util.graph.tree;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Queue;
import library.util.collections.MeldableSkewHeap;
import library.util.graph.tree.Tree;

/**
 * 根付き木上のラミナーマトロイド最大独立集合（最大重み割り当て）問題を貪欲法で解くクラス。
 */
public class LaminarMatroidOnTree {

	/**
	 * ヒープに格納する要素。
	 */
	public static class Element {
		public final long weight;
		public final int id;
		public long count;

		public Element(long weight, int id, long count) {
			this.weight = weight;
			this.id = id;
			this.count = count;
		}
	}

	private static final Comparator<Element> CMP = (e1, e2) -> Long.compare(e1.weight, e2.weight);

	/**
	 * 双対問題の最適解を保持するレコード。
	 */
	public static class DualResult {
		/** 各頂点 v の双対変数 y_v の配列。y_v >= 0 */
		public final long[] y;
		/** 各頂点 v の双対変数 z_v の配列。z_v >= 0 */
		public final long[] z;

		public DualResult(long[] y, long[] z) {
			this.y = y;
			this.z = z;
		}
	}

	/**
	 * 内部で計算されたヒープと閾値を保持する内部クラス。
	 */
	private static class DPResult {
		final MeldableSkewHeap.Node<Element>[] heaps;
		final long[] cutoffWeight;

		DPResult(MeldableSkewHeap.Node<Element>[] heaps, long[] W) {
			this.heaps = heaps;
			this.cutoffWeight = W;
		}
	}

	private static DPResult runBottomUpDP(Tree tree, long[] A, long[] C, long[] B) {
		int N = tree.N;
		if (N == 0) return new DPResult(null, new long[0]);
		if (A.length != N || C.length != N || B.length != N) {
			throw new IllegalArgumentException("Array lengths must equal the tree size N");
		}
		for (int i = 0; i < N; i++) {
			if (C[i] < 0 || B[i] < 0) {
				throw new IllegalArgumentException("Capacities must be non-negative");
			}
		}

		if (!tree.isRooted()) {
			tree.rooted(0);
		}

		MeldableSkewHeap<Element> heap = new MeldableSkewHeap<>(CMP);

		@SuppressWarnings("unchecked")
		MeldableSkewHeap.Node<Element>[] heaps = new MeldableSkewHeap.Node[N];
		long[] totalCounts = new long[N];
		long[] W = new long[N];

		int[] order = tree.bfsOrder().clone();
		// ボトムアップに処理するためにBFS順を反転する
		for (int i = 0; i < N / 2; i++) {
			int tmp = order[i];
			order[i] = order[N - 1 - i];
			order[N - 1 - i] = tmp;
		}

		for (int v : order) {
			MeldableSkewHeap.Node<Element> merged = null;
			long totalCount = 0;
			for (int i = 0; i < tree.childs[v].size(); i++) {
				int child = tree.childs[v].get(i);
				merged = heap.meld(merged, heaps[child]);
				totalCount += totalCounts[child];
			}

			long actualC = A[v] > 0 ? C[v] : 0;
			if (actualC > 0) {
				Element elem = new Element(A[v], v, actualC);
				MeldableSkewHeap.Node<Element> currNode = new MeldableSkewHeap.Node<>(elem);
				merged = heap.meld(merged, currNode);
				totalCount += actualC;
			}

			long limit = B[v];
			long threshold = 0;
			if (totalCount > limit) {
				while (totalCount > limit) {
					long excess = totalCount - limit;
					threshold = merged.val.weight;
					if (merged.val.count <= excess) {
						totalCount -= merged.val.count;
						merged = heap.pop(merged);
					} else {
						merged.val.count -= excess;
						totalCount -= excess;
					}
				}
			}
			W[v] = threshold;

			heaps[v] = merged;
			totalCounts[v] = totalCount;
		}

		return new DPResult(heaps, W);
	}

	/**
	 * <pre>
	 * maximize  Σ_v A_v x_v
	 *
	 * subject to
	 *   Σ_(u ∈ subtree(v)) x_u ≤ B_v
	 *   0 ≤ x_v ≤ C_v
	 * </pre>
	 * 計算量: O(N log N)
	 *
	 * @param tree 根付き木
	 * @param A 各頂点 v の重み A_v の配列
	 * @param C 各頂点 v の個別上限 C_v の配列
	 * @param B 各頂点 v の部分木容量 B_v の配列
	 * @return 目的関数の最大値
	 */
	// 未テスト
	public static long maxSubtreePackingValue(Tree tree, long[] A, long[] C, long[] B) {
		long[] x = maxSubtreePacking(tree, A, C, B);
		long ans = 0;
		for (int i = 0; i < x.length; i++) {
			ans += x[i] * A[i];
		}
		return ans;
	}

	/**
	 * 根付き木 T において、各頂点 v に割り当てる非負整数 x_v が満たすべき制約
	 * 0 <= x_v <= C_v
	 * かつ、任意の頂点 v に対して
	 * Σ_(u ∈ subtree(v)) x_u <= B_v
	 * のもとで、目的関数
	 * Σ_(v) A_v x_v
	 * を最大化する非負整数割り当て x_v を求めます。
	 *
	 * 計算量: O(N log N)
	 *
	 * @param tree 根付き木
	 * @param A 各頂点 v の重み A_v の配列
	 * @param C 各頂点 v の個別上限 C_v の配列
	 * @param B 各頂点 v の部分木容量 B_v の配列
	 * @return 最適な割り当て x の配列（長さ N）
	 */
	// 未テスト
	public static long[] maxSubtreePacking(Tree tree, long[] A, long[] C, long[] B) {
		int N = tree.N;
		if (N == 0) return new long[0];
		DPResult dpRes = runBottomUpDP(tree, A, C, B);

		long[] x = new long[N];
		int root = tree.root();
		MeldableSkewHeap.Node<Element> rootHeap = dpRes.heaps[root];
		if (rootHeap != null) {
			Queue<MeldableSkewHeap.Node<Element>> queue = new ArrayDeque<>();
			queue.add(rootHeap);
			while (!queue.isEmpty()) {
				MeldableSkewHeap.Node<Element> curr = queue.poll();
				x[curr.val.id] += curr.val.count;
				if (curr.l != null) queue.add(curr.l);
				if (curr.r != null) queue.add(curr.r);
			}
		}

		return x;
	}

	/**
	 * 主問題に対する双対問題の最適解 (y_v, z_v) を求めます。
	 * 任意の頂点 v について
	 * y_v >= 0, z_v >= 0
	 * であり、さらに
	 * y_v + Σ_(u ∈ ancestors(v)) z_u >= A_v
	 * を満たし、目的関数
	 * Σ_(v) C_v y_v + Σ_(v) B_v z_v
	 * を最小化する最適解を計算します。
	 *
	 * 計算量: O(N log N)
	 *
	 * @param tree 根付き木
	 * @param A 各頂点 v の重み A_v の配列
	 * @param C 各頂点 v の個別上限 C_v の配列
	 * @param B 各頂点 v の部分木容量 B_v の配列
	 * @return 双対問題の最適解 y, z を含む DualResult
	 */
	// 未テスト
	public static DualResult minAncestorCover(Tree tree, long[] A, long[] C, long[] B) {
		int N = tree.N;
		if (N == 0) return new DualResult(new long[0], new long[0]);

		DPResult dpRes = runBottomUpDP(tree, A, C, B);
		long[] cutoffWeight = dpRes.cutoffWeight;

		// 双対変数の計算
		long[] d = new long[N];
		long[] z = new long[N];
		int[] bfsOrder = tree.bfsOrder();
		int root = tree.root();

		d[root] = cutoffWeight[root];
		z[root] = cutoffWeight[root];

		for (int v : bfsOrder) {
			if (v == root) continue;
			int p = tree.parent(v);
			d[v] = Math.max(d[p], cutoffWeight[v]);
			z[v] = d[v] - d[p];
		}

		long[] y = new long[N];
		for (int i = 0; i < N; i++) {
			y[i] = Math.max(0, A[i] - d[i]);
		}

		return new DualResult(y, z);
	}
}
