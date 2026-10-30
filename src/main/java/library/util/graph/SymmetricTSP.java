package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import library.util.ArrayUtils;
import library.util.Zn;

/**
 * 対称巡回セールスマン問題 (Symmetric TSP) に対する局所探索およびヒューリスティック解法を提供するクラス。
 *
 * <p>
 * 頂点集合 V = {0, 1, ..., n-1} 上の距離関数 d(u, v) (d(u, v) = d(v, u)) に対して、
 * Nearest Neighbor 法による初期解構築、2-opt, 3-opt 局所探索、および Double Bridge 摂動を行う。
 * </p>
 *
 * 未テスト
 */
public class SymmetricTSP {

	/**
	 * 隣接頂点とその距離を表すレコード。
	 *
	 * @param to 宛先頂点
	 * @param weight 辺の重み
	 *
	 * 未テスト
	 */
	public record Adjacent(int to, long weight) {}

	/**
	 * 距離関数のインターフェース。
	 *
	 * 未テスト
	 */
	@FunctionalInterface
	public interface DistanceFunction {
		/**
		 * 頂点 u と v の間の距離を返す。
		 * 時間計算量: O(1)
		 *
		 * @param u 頂点 u
		 * @param v 頂点 v
		 * @return 距離 d(u, v)
		 */
		long dist(int u, int v);
	}

	/**
	 * 隣接リスト取得関数のインターフェース。
	 *
	 * 未テスト
	 */
	@FunctionalInterface
	public interface AdjacentsFunction {
		/**
		 * 頂点 u に隣接する頂点集合を返す。効率的な探索のため距離の昇順で並んでいることが望ましい。
		 * 時間計算量: O(deg(u))
		 *
		 * @param u 頂点 u
		 * @return 隣接頂点の Iterable
		 */
		Iterable<Adjacent> adjacents(int u);
	}

	/**
	 * 距離行列および隣接情報のインターフェース。
	 *
	 * 未テスト
	 */
	public interface DistanceMatrix {
		/**
		 * 頂点数 n を返す。
		 * 時間計算量: O(1)
		 *
		 * @return 頂点数 n
		 */
		int n();

		/**
		 * 頂点 u と v の間の距離を返す。
		 * 時間計算量: O(1)
		 *
		 * @param u 頂点 u
		 * @param v 頂点 v
		 * @return 距離 d(u, v)
		 */
		long dist(int u, int v);

		/**
		 * 頂点 u に隣接する頂点集合を返す。
		 * 時間計算量: O(deg(u))
		 *
		 * @param u 頂点 u
		 * @return 隣接頂点の Iterable
		 */
		Iterable<Adjacent> adjacents(int u);
	}

	/**
	 * TSPの解 (巡路と総距離) を表すクラス。
	 *
	 * 未テスト
	 */
	public static class Solution {
		public long cost;
		public int[] path;

		/**
		 * 解を構築する。
		 * 時間計算量: O(1)
		 *
		 * @param cost 総距離
		 * @param path 頂点順序を表す配列
		 */
		public Solution(long cost, int[] path) { // 未テスト
			this.cost = cost;
			this.path = path;
		}

		/**
		 * 解の文字列表現を返す。
		 * 時間計算量: O(n)
		 *
		 * @return 文字列表現
		 */
		@Override
		public String toString() { // 未テスト
			StringBuilder sb = new StringBuilder();
			sb.append("[cost=").append(cost).append(", path=(");
			if (path != null && path.length > 0) {
				for (int i = 0; i < path.length; i++) {
					sb.append(path[i]).append(",");
				}
				sb.append(path[0]);
			}
			sb.append(")]");
			return sb.toString();
		}
	}

	private final int n;
	private final DistanceFunction dist;
	private final AdjacentsFunction adjacents;
	private final Zn zn;
	private int lastI = 0;

	/**
	 * SymmetricTSP インスタンスを構築する。
	 * 時間計算量: O(1)
	 *
	 * @param n 頂点数
	 * @param dist 距離関数
	 * @param adjacents 隣接関数
	 */
	public SymmetricTSP(int n, DistanceFunction dist, AdjacentsFunction adjacents) { // 未テスト
		this.n = n;
		this.dist = dist;
		this.adjacents = adjacents;
		this.zn = new Zn(n);
	}

	/**
	 * DistanceMatrix から SymmetricTSP インスタンスを構築する。
	 * 時間計算量: O(1)
	 *
	 * @param distanceMatrix 距離行列オブジェクト
	 */
	public SymmetricTSP(DistanceMatrix distanceMatrix) { // 未テスト
		this(distanceMatrix.n(), distanceMatrix::dist, distanceMatrix::adjacents);
	}

	/**
	 * HeldKarp.DistanceMatrix から SymmetricTSP インスタンスを構築する。
	 * 時間計算量: O(1)
	 *
	 * @param distanceMatrix HeldKarpの距離行列オブジェクト
	 */
	public SymmetricTSP(HeldKarp.DistanceMatrix distanceMatrix) { // 未テスト
		this.n = distanceMatrix.n();
		this.dist = distanceMatrix::dist;
		this.adjacents = u -> {
			List<Adjacent> list = new ArrayList<>();
			for (HeldKarp.Adjacent adj : distanceMatrix.adjacents(u)) {
				list.add(new Adjacent(adj.to(), adj.weight()));
			}
			return list;
		};
		this.zn = new Zn(n);
	}

	/**
	 * 2次元距離行列配列から SymmetricTSP インスタンスを構築する。
	 * 各頂点の隣接リストは距離の昇順でソートされる。
	 * 時間計算量: O(n^2 log n)
	 *
	 * @param distMatrix 2次元距離配列
	 */
	public SymmetricTSP(long[][] distMatrix) { // 未テスト
		this.n = distMatrix.length;
		this.dist = (u, v) -> distMatrix[u][v];
		@SuppressWarnings("unchecked")
		List<Adjacent>[] adj = new List[n];
		for (int u = 0; u < n; u++) {
			List<Adjacent> list = new ArrayList<>(n - 1);
			for (int v = 0; v < n; v++) {
				if (u == v) continue;
				list.add(new Adjacent(v, distMatrix[u][v]));
			}
			list.sort(Comparator.comparingLong(Adjacent::weight));
			adj[u] = list;
		}
		this.adjacents = u -> adj[u];
		this.zn = new Zn(n);
	}

	/**
	 * 頂点数を返す。
	 * 時間計算量: O(1)
	 *
	 * @return 頂点数 n
	 */
	public int n() { // 未テスト
		return n;
	}


	/**
	 * 与えられた巡路の総距離を評価・再計算する。
	 * 時間計算量: O(n)
	 *
	 * @param sol 解
	 * @return 総距離
	 */
	public long eval(Solution sol) { // 未テスト
		if (sol.path == null || sol.path.length == 0) return 0;
		long ret = 0;
		int now = sol.path[sol.path.length - 1];
		for (int nxt : sol.path) {
			ret += dist.dist(now, nxt);
			now = nxt;
		}
		return ret;
	}

	/**
	 * 指定された初期頂点 init から Nearest Neighbor 法により貪欲に初期解を構築する。
	 * 時間計算量: O(n^2)
	 *
	 * @param init 開始頂点
	 * @return 構築された解
	 */
	public Solution nearestNeighbor(int init) { // 未テスト
		if (n == 0) return new Solution(0, new int[0]);
		int now = init;
		int[] ret = new int[n];
		boolean[] alive = new boolean[n];
		Arrays.fill(alive, true);

		ret[0] = now;
		alive[now] = false;
		long len = 0;
		int count = 1;
		while (count < n) {
			int nxt = -1;
			for (int i = 0; i < n; i++) {
				if (alive[i] && (nxt < 0 || dist.dist(now, i) < dist.dist(now, nxt))) {
					nxt = i;
				}
			}
			ret[count++] = nxt;
			alive[nxt] = false;
			len += dist.dist(now, nxt);
			now = nxt;
		}
		len += dist.dist(ret[n - 1], ret[0]);
		return new Solution(len, ret);
	}

	/**
	 * 解 sol に対して 2-opt 局所探索を行い、改善されなくなるまで解を破壊的に更新する。
	 * 時間計算量: 1反復あたり O(n * K) (K は各頂点の候補隣接数)
	 *
	 * @param sol 改善対象の解 (インプレースで更新される)
	 */
	public void twoOpt(Solution sol) { // 未テスト
		if (n <= 3) return;
		int[] vToI = new int[n];
		for (int i = 0; i < n; i++) {
			vToI[sol.path[i]] = i;
		}
		while (true) {
			boolean updated = false;
			for (int i = 0; i < n && !updated; i++) {
				int u = sol.path[i];
				int nxtu = sol.path[(int) zn.reduce(i + 1)];
				long dunxtu = dist.dist(u, nxtu);

				for (Adjacent adj : adjacents.adjacents(u)) {
					int v = adj.to();
					long duv = adj.weight();
					if (duv >= dunxtu) break;
					int j = vToI[v];
					int nxtv = sol.path[(int) zn.reduce(j + 1)];
					long diff = duv + dist.dist(nxtu, nxtv) - dunxtu - dist.dist(v, nxtv);
					if (diff < 0) {
						sol.cost += diff;
						int l, r;
						if (zn.reduce(j - i) < zn.reduce(i - j)) {
							l = (int) zn.reduce(i + 1);
							r = j;
						} else {
							l = (int) zn.reduce(j + 1);
							r = i;
						}
						while (l != r) {
							int tmp = sol.path[l];
							sol.path[l] = sol.path[r];
							sol.path[r] = tmp;
							vToI[sol.path[l]] = l;
							vToI[sol.path[r]] = r;
							l = (int) zn.reduce(l + 1);
							if (l == r) break;
							r = (int) zn.reduce(r - 1);
						}
						updated = true;
						break;
					}
				}
				if (updated) break;

				for (Adjacent adj : adjacents.adjacents(nxtu)) {
					int nxtv = adj.to();
					long dnxtunxtv = adj.weight();
					if (dnxtunxtv >= dunxtu) break;
					int j = (int) zn.reduce(vToI[nxtv] - 1);
					int v = sol.path[j];
					long diff = dist.dist(u, v) + dnxtunxtv - dunxtu - dist.dist(v, nxtv);
					if (diff < 0) {
						sol.cost += diff;
						int l, r;
						if (zn.reduce(j - i) < zn.reduce(i - j)) {
							l = (int) zn.reduce(i + 1);
							r = j;
						} else {
							l = (int) zn.reduce(j + 1);
							r = i;
						}
						while (l != r) {
							int tmp = sol.path[l];
							sol.path[l] = sol.path[r];
							sol.path[r] = tmp;
							vToI[sol.path[l]] = l;
							vToI[sol.path[r]] = r;
							l = (int) zn.reduce(l + 1);
							if (l == r) break;
							r = (int) zn.reduce(r - 1);
						}
						updated = true;
						break;
					}
				}
			}
			if (!updated) break;
		}
	}

	/**
	 * 解 sol に対して 3-opt 局所探索を 1 ラウンド試み、改善可能な組み替えが見つかれば破壊的に適用する。
	 * 時間計算量: O(n * K^2) (K は各頂点の候補隣接数)
	 *
	 * @param sol 改善対象の解 (インプレースで更新される)
	 * @return 解が改善された場合は true、改善されなかった場合は false
	 */
	public boolean threeOpt(Solution sol) { // 未テスト
		if (n < 6) return false;
		int[] vToI = new int[n];
		for (int i = 0; i < n; i++) {
			vToI[sol.path[i]] = i;
		}

		for (int nseen = 0; nseen < n; nseen++, lastI = (int) zn.reduce(lastI + 1)) {
			int u = sol.path[(int) zn.reduce(lastI - 1)];
			int nxtu = sol.path[lastI];
			long dunxtu = dist.dist(u, nxtu);

			// type 1 / 3
			for (Adjacent adj1 : adjacents.adjacents(u)) {
				int nxtv = adj1.to();
				long dunxtv = adj1.weight();
				if (dunxtv >= dunxtu) break;
				int v = sol.path[(int) zn.reduce(vToI[nxtv] - 1)];
				long dvnxtv = dist.dist(v, nxtv);

				// type 1
				for (Adjacent adj2 : adjacents.adjacents(v)) {
					int nxtw = adj2.to();
					long dvnxtw = adj2.weight();
					if (nxtw == nxtv || nxtw == nxtu) continue;
					if (dunxtv + dvnxtw >= dunxtu + dvnxtv) break;
					int w = sol.path[(int) zn.reduce(vToI[nxtw] - 1)];

					if (!checkUvwOrder(vToI, u, v, w)) continue;

					long current = dunxtu + dvnxtv + dist.dist(w, nxtw);
					long diff = dunxtv + dist.dist(w, nxtu) + dvnxtw - current;
					if (diff < 0) {
						sol.cost += diff;
						rev(sol.path, vToI, nxtu, v);
						rev(sol.path, vToI, nxtv, w);
						rev(sol.path, vToI, nxtw, u);
						return true;
					}
				}

				// type 3
				for (Adjacent adj2 : adjacents.adjacents(v)) {
					int w = adj2.to();
					long dvw = adj2.weight();
					if (dunxtv + dvw >= dunxtu + dvnxtv) break;
					if (!checkUvwOrder(vToI, u, v, w)) continue;

					int nxtw = sol.path[(int) zn.reduce(vToI[w] + 1)];

					long current = dunxtu + dvnxtv + dist.dist(w, nxtw);
					long diff = dunxtv + dvw + dist.dist(nxtu, nxtw) - current;
					if (diff < 0) {
						sol.cost += diff;
						rev(sol.path, vToI, nxtw, u);
						rev(sol.path, vToI, nxtv, w);
						return true;
					}
				}
			}

			// type 2
			for (Adjacent adj1 : adjacents.adjacents(nxtu)) {
				int nxtv = adj1.to();
				long dnxtunxtv = adj1.weight();
				if (dnxtunxtv >= dunxtu) break;
				int v = sol.path[(int) zn.reduce(vToI[nxtv] - 1)];
				long dvnxtv = dist.dist(v, nxtv);

				for (Adjacent adj2 : adjacents.adjacents(v)) {
					int nxtw = adj2.to();
					long dvnxtw = adj2.weight();
					if (dnxtunxtv + dvnxtw >= dunxtu + dvnxtv) break;
					int w = sol.path[(int) zn.reduce(vToI[nxtw] - 1)];
					if (!checkUvwOrder(vToI, u, v, w)) continue;

					long current = dunxtu + dvnxtv + dist.dist(w, nxtw);
					long diff = dist.dist(u, w) + dnxtunxtv + dvnxtw - current;
					if (diff < 0) {
						sol.cost += diff;
						rev(sol.path, vToI, nxtu, v);
						rev(sol.path, vToI, nxtw, u);
						return true;
					}
				}
			}

			// type 4
			for (Adjacent adj1 : adjacents.adjacents(u)) {
				int v = adj1.to();
				long duv = adj1.weight();
				if (duv >= dunxtu) break;
				int nxtv = sol.path[(int) zn.reduce(vToI[v] + 1)];
				long dvnxtv = dist.dist(v, nxtv);

				for (Adjacent adj2 : adjacents.adjacents(nxtv)) {
					int nxtw = adj2.to();
					long dnxtvnxtw = adj2.weight();
					if (duv + dnxtvnxtw >= dunxtu + dvnxtv) break;
					int w = sol.path[(int) zn.reduce(vToI[nxtw] - 1)];
					if (!checkUvwOrder(vToI, u, v, w)) continue;

					long current = dunxtu + dvnxtv + dist.dist(w, nxtw);
					long diff = duv + dist.dist(nxtu, w) + dnxtvnxtw - current;
					if (diff < 0) {
						sol.cost += diff;
						rev(sol.path, vToI, nxtu, v);
						rev(sol.path, vToI, nxtv, w);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * 解 sol に対して Double Bridge 摂動 (4-opt キック) を適用し、局所最適解からの脱出を図る。
	 * 時間計算量: O(n)
	 *
	 * @param sol 対象の解 (インプレースで更新される)
	 * @param rng 乱数生成器
	 * @return n >= 8 で適用された場合は true、n < 8 の場合は false
	 */
	public boolean doubleBridge(Solution sol, Random rng) { // 未テスト
		if (n < 8) return false;

		int[] p = sol.path;
		int randRot = rng.nextInt(n);
		ArrayUtils.rotateLeftInplace(p, randRot);

		int[] arr = new int[3];
		for (int i = 0; i < 3; i++) {
			arr[i] = 2 + rng.nextInt(n - 6 - 2 + 1);
		}
		Arrays.sort(arr);
		int i = arr[0];
		int j = arr[1] + 2;
		int k = arr[2] + 4;

		long[] diffs = new long[2];
		for (int d = 0; d < 2; d++) {
			int u = p[n - 1];
			int nxtu = p[0];
			int v = p[i - 1];
			int nxtv = p[i];
			int w = p[j - 1];
			int nxtw = p[j];
			int x = p[k - 1];
			int nxtx = p[k];
			diffs[d] = dist.dist(u, nxtu) + dist.dist(v, nxtv) + dist.dist(w, nxtw) + dist.dist(x, nxtx);
			if (d == 1) break;
			ArrayUtils.reverse(p, 0, i);
			ArrayUtils.reverse(p, i, j);
			ArrayUtils.reverse(p, j, k);
			ArrayUtils.reverse(p, k, n);
		}
		sol.cost += diffs[1] - diffs[0];
		return true;
	}

	/**
	 * 解 sol に対してデフォルトの乱数生成器を用いて Double Bridge 摂動を適用する。
	 * 時間計算量: O(n)
	 *
	 * @param sol 対象の解
	 * @return 適用されたかどうか
	 */
	public boolean doubleBridge(Solution sol) { // 未テスト
		return doubleBridge(sol, new Random());
	}

	private boolean checkUvwOrder(int[] vToI, int u, int v, int w) {
		int i = vToI[u];
		int j = vToI[v];
		int k = vToI[w];
		if (i < j && j < k) return true;
		if (j < k && k < i) return true;
		if (k < i && i < j) return true;
		return false;
	}

	private void rev(int[] path, int[] vToI, int u, int v) {
		int l = vToI[u];
		int r = vToI[v];
		while (l != r) {
			int tmp = path[l];
			path[l] = path[r];
			path[r] = tmp;
			l = (int) zn.reduce(l + 1);
			if (l == r) break;
			r = (int) zn.reduce(r - 1);
		}
	}
}
