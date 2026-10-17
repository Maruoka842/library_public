package library.util.graph.tree;

import java.util.Arrays;

import library.util.ArrayUtils;
import library.util.MaxPlus;
import library.util.graph.tree.Tree.DpStrategy;
import library.util.seq.SortedArrays;

public class TreedpFactory {
	
	/**
	 * 頂点vの重みをA[v]とする木上の最大重み独立集合（Maximum Weighted Independent Set; MWIS）について、
	 * 「独立集合のサイズごとの最大重み」を計算
	 *
	 * <p>
	 * 各頂点 v に対して、次の DP テーブルを構築。
	 * 
	 * <pre>
	 *   dp[v][0][k] … v を選ばない場合における、
	 *                  「v を根とする部分木内でサイズ k の独立集合の最大重み」
	 *   dp[v][1][k] … v を選ぶ場合における、
	 *                  「v を根とする部分木内でサイズ k の独立集合の最大重み」
	 * </pre>
	 * <p>
	 *
	 * @param tree
	 * @param A
	 * @return
	 */
	public static long[][][] maximumWeightedIndependentSet(Tree tree, long[] A) {
		long INF = Long.MAX_VALUE / 3;
		var dp = tree.treeDP(new DpStrategy<long[][]>() {
			@Override
			public long[][] merge(long[][] a, long[][] b) {
				long[][] ret = new long[2][a[0].length + b[0].length - 1];
				ArrayUtils.fill(ret, -INF);
				for (int used = 0; used <= 1; used++) {
					for (int i = 0; i < a[used].length; i++) {
						if (a[used][i] == -INF)
							continue;
						for (int used2 = 0; used2 <= 1; used2++) {
							if (used == 1 && used2 == 1)
								continue;
							for (int j = 0; j < b[used2].length; j++) {
								if (b[used2][j] == -INF)
									continue;
								ret[used][i + j] = Math.max(ret[used][i + j], a[used][i] + b[used2][j]);
							}
						}
					}
				}
				return ret;
			}

			@Override
			public long[][] single(int v) {
				long[][] a = new long[2][2];
				ArrayUtils.fill(a, -INF);
				a[0][0] = 0;
				a[1][1] = A[v];
				return a;
			}
		});
		return dp;
	}

	/**
	 * 木の頂点をいくつか選んだとき、その隣接頂点（選んだ頂点自身も含む）の集合のサイズがkになるものの数え上げ。
	 * [部分木の根][根が覆われているか][根が直接覆われているか][覆われた頂点数]という状態で返す。
	 * https://atcoder.jp/contests/abc207/tasks/abc207_f
	 */
	public static long[][][][] vertexNeighborCover(Tree tree) {
		long[][][] single = new long[2][2][2];// [根が覆われているか][根が直接覆われているか][覆われた頂点数]
		single[0][0][0] = 1;
		single[1][1][1] = 1;

		return tree.treeDP(v -> single, (x, y) -> {
			int xsz = x[0][0].length;
			int ysz = y[0][0].length;
			long[][][] ret = new long[2][2][xsz + ysz - 1];
			for (int cover = 0; cover <= 1; cover++) {
				for (int dircover = 0; dircover <= 1; dircover++) {
					for (int cover2 = 0; cover2 <= 1; cover2++) {
						for (int dircover2 = 0; dircover2 <= 1; dircover2++) {
							for (int size = 0; size < xsz; size++) {
								for (int size2 = 0; size2 < ysz; size2++) {
									long par = x[cover][dircover][size];
									long ch = y[cover2][dircover2][size2];
									if (par == 0 || ch == 0)
										continue;
									int nsize = size + size2;
									if (dircover == 1 && cover2 == 0)
										nsize++;
									if (dircover2 == 1 && cover == 0)
										nsize++;
									ret[cover | dircover2][dircover][nsize] += par * ch;
								}
							}
						}
					}
				}
			}
			return ret;
		});
	}

	/**
	 * 木の各辺を向きづけるコストの最小値を計算する。
	 *
	 * <p>
	 * 各頂点 v について、その入次数が k であるときのコスト f(v, k) = costs[v][k] が与えられる。
	 * 全ての辺をどちらかの向きに向きづけたときの、全頂点のコストの総和の最小値を求める。
	 * </p>
	 *
	 * @param tree
	 * @param costs costs[v][k] は頂点 v の入次数が k のときのコスト
	 * @return 最小コスト
	 */
	public static long minimumCostOrientation(Tree tree, long[][] costs) {
		int N = tree.N;
		if (N == 0)
			return 0;
		if (!tree.isRooted())
			tree.rooted(0);
		int[] order = tree.postOrder();
		long[][] dp = new long[N][2];
		long INF = Long.MAX_VALUE / 3;

		for (int v : order) {
			int m = tree.childs[v].size();
			long[] diffs = new long[m];
			long baseCost = 0;
			for (int i = 0; i < m; i++) {
				int u = tree.childs[v].get(i);
				baseCost += dp[u][1];
				diffs[i] = dp[u][0] - dp[u][1];
			}
			Arrays.sort(diffs);
			long[] prefixSum = ArrayUtils.prefixSumFromEmpty(diffs);

			// dp[v][0]: 頂点 v とその親 p の間の辺が v -> p と向きづけられている（v の入次数に寄与しない）
			// dp[v][1]: 頂点 v とその親 p の間の辺が p -> v と向きづけられている（v の入次数が 1 増える）
			for (int b = 0; b <= 1; b++) {
				if (v == tree.root() && b == 1) {
					dp[v][b] = INF;
					continue;
				}
				long minV = INF;
				for (int j = 0; j <= m; j++) {
					// v の入次数は、子からの入辺数 j と親からの入辺数 b の和
					if (j + b < costs[v].length) {
						minV = Math.min(minV, costs[v][j + b] + prefixSum[j]);
					}
				}
				dp[v][b] = baseCost + minV;
			}
		}
		return dp[tree.root()][0];
	}

	/**
	 * 各頂点 v に非負整数 x_v を割り当て、S_v = sum_(u in subtree(v)) x_u とするとき、
	 * 各頂点 t について、ある祖先 v (自身を含む) が存在して、
	 * ability[v][S_v] >= requirement[t] を満たすようにする最小コスト
	 * sum_(v) unitCost[v] * x_v を求める。
	 *
	 * <p>
	 * ability[v][k] は k について単調非減少である。
	 * </p>
	 *
	 * 各頂点 v について、
	 * {@code ability[v][k] >= max(requirement[t] : t in subtree(v))}
	 * を満たす {@code k <= |subtree(v)|} が存在するとき O(N^3)。
	 * </p>
	 *
	 * @param tree 根付き木
	 * @param requirement 各頂点 t の必要閾値 requirement[t]
	 * @param unitCost 各頂点 v に 1 単位の量を配置するコスト unitCost[v]
	 * @param ability ability[v][k] は頂点 v において配置された合計量が k であるときの能力
	 * @return 最小の配置コスト、不可能な場合は極大値
	 * @throws IllegalArgumentException 配列のサイズが N でない場合や、必要な量が N を超える場合
	 * @throws AssertionError 木が根付きでない場合
	 */
	// 未テスト
	public static long minSubtreeSumAncestorCover(
			Tree tree,
			long[] requirement,
			long[] unitCost,
			long[][] ability) {
		//https://atcoder.jp/contests/joi2026sf/submissions/78363935

		if (!tree.isRooted())
			throw new AssertionError("tree must be rooted.");

		final long INF = Long.MAX_VALUE / 4;

		int n = requirement.length;

		if (unitCost.length != n || ability.length != n)
			throw new IllegalArgumentException();

		int root = tree.root();
		int[] order = tree.postOrder();

		// requirement の座標圧縮。
		long[] levels = ArrayUtils.sortq(requirement);
		int m = levels.length;

		int[] requirementIndex = ArrayUtils.compress(requirement);
		ArrayUtils.incrementAll(requirementIndex);

		/*
		 * maxRequirement[v]:
		 * T_v 内の requirement の最大値。
		 *
		 * minCost[v]:
		 * T_v 内の unitCost の最小値。
		 */
		long[] maxRequirement = requirement.clone();
		long[] minCost = unitCost.clone();
		for (int v : order) {
			for (int c : tree.childs[v]) {
				maxRequirement[v] =Math.max(maxRequirement[v], maxRequirement[c]);
				minCost[v] = Math.min(minCost[v], minCost[c]);
			}
		}

		/*
		 * limit[v]:
		 *
		 * ability(v, k) だけで T_v の全 requirement を
		 * cover できる最小 k。
		 */
		int[] limit = new int[n];

		for (int v = 0; v < n; v++) {
			int k = 0;

			while (k <= n) {
				long power = k < ability[v].length ? ability[v][k] : ability[v][ability[v].length - 1];
				if (power >= maxRequirement[v]) {
					break;
				}
				k++;
			}

			if (k > n) {
				throw new IllegalArgumentException("required amount exceeds N at vertex " + v);
			}

			limit[v] = k;
		}

		/*
		 * dp[v][j][k]:
		 *
		 * T_v 内に合計 j 単位置き、
		 * まだ T_v 内では cover されていない頂点の
		 * requirement の最大値が levels[k - 1] 以下
		 * であるときの最小コスト。
		 *
		 * k = 0 は未 cover 頂点なし。
		 */
		long[][][] dp = new long[n][][];

		for (int v : order) {

			/*
			 * 子を全部 merge。
			 *
			 * cur[j][k]:
			 * 子部分木に合計 j 単位、
			 * k = 0 なら residual なし、
			 * k > 0 なら residual requirement <= levels[k - 1]
			 */
			long[][] cur = new long[1][m + 1];

			int curMax = 0;

			for (int c : tree.childs[v]) {
				int nextMax = Math.min(limit[v], curMax + limit[c]);

				long[][] next = new long[nextMax + 1][m + 1];
				ArrayUtils.fill(next, INF);
				// next[j][k] = 部分木T_cまでに合計ｊ個配置して、残っているcover requirementが高々levels[k-1]となる最小コスト

				for (int a = 0; a <= curMax; a++) {
					for (int b = 0; b <= limit[c] && a + b <= nextMax; b++) {

						int j = a + b;

						for (int k = 0; k <= m; k++) {
							long x = cur[a][k];
							long y = dp[c][b][k];

							if (x >= INF || y >= INF)
								continue;

							next[j][k] = Math.min(next[j][k], x + y);
						}
					}
				}

				cur = next;
				curMax = nextMax;

				dp[c] = null;
			}

			long[][] base = new long[limit[v] + 1][m + 1];
			ArrayUtils.fill(base, INF);

			/*
			 * base[j][k]:
			 *
			 * T_v 内に合計 j 単位置き、
			 * 子部分木内で可能な cover を行い、
			 * v 自身の requirement も residual に加えた後の最小コスト。
			 *
			 * ただし、まだ v 自身の ability[v][j] による cover は行わない。
			 *
			 * k = 0 は residual なし、
			 * k > 0 は residual requirement <= levels[k - 1]。
			 */
			
			int rv = requirementIndex[v];

			for (int j = 0; j <= curMax; j++) {
				for (int k = rv; k <= m; k++) {
					base[j][k] = Math.min(base[j][k], cur[j][k]);
				}
			}

			/*
			 * T_v 内に追加の量を置く。
			 *
			 * 追加量は T_v 内の最安頂点へ置けばよい。
			 */
			for (int j = 1; j <= limit[v]; j++) {
				for (int k = 0; k <= m; k++) {
					if (base[j - 1][k] >= INF)
						continue;

					base[j][k] = Math.min(base[j][k], base[j - 1][k] + minCost[v]);
				}
			}
			
			/*
			 * res[j][k]:
			 *
			 * T_v 内に合計 j 単位置き、
			 * T_v 内で可能な cover を行った後の最小コスト。
			 *
			 * k = 0:
			 *   T_v 内の全頂点が cover 済み。
			 *
			 * k > 0:
			 *   まだ cover されていない頂点の requirement が
			 *   すべて levels[k - 1] 以下。
			 */
			long[][] res = new long[limit[v] + 1][m + 1];
			ArrayUtils.fill(res, INF);

			for (int j = 0; j <= limit[v]; j++) {

				/*
				 * まず residual をそのまま上へ送る。
				 */
				System.arraycopy(base[j], 0, res[j], 0, m + 1);

				/*
				 * ability(v, j) 以下の residual は
				 * v によって全部 cover できる。
				 */
				long power = j < ability[v].length ? ability[v][j] : ability[v][ability[v].length - 1];

				int covered = SortedArrays.higher(levels, power);

				res[j][0] = Math.min(res[j][0], base[j][covered]);

				/*
				 * residual <= k の at-most closure。
				 */
				for (int k = 1; k <= m; k++) {
					res[j][k] = Math.min(res[j][k], res[j][k - 1]);
				}
			}

			dp[v] = res;
		}

		long ans = INF;

		for (int j = 0; j <= limit[root]; j++) {
			ans = Math.min(ans, dp[root][j][0]);
		}

		return ans;
	}

}
