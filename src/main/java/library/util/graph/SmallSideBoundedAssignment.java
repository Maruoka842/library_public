package library.util.graph;

import java.util.Arrays;
import library.util.collections.TreapSet;

/**
 * グループ数 K が小さい場合の bounded semi-assignment（有界半配属問題）を解くクラス。
 *
 * N 個の要素を K 個のグループへ割り当てる。
 * 要素 i をグループ g に割り当てたときの価値を value[i][g] とする。
 * 各グループ g について、割り当てられる要素数 count[g] が
 * lower[g] <= count[g] <= upper[g]
 * を満たす割り当てのうち、価値の総和 Σ_(i=0)^(N-1) value[i][assignment[i]] を最大化する。
 * 全要素はちょうど1つのグループに割り当てられる。
 *
 * 計算量:
 *   期待時間計算量: O(N K^2 log N + K^3)
 *   （特に K <= N のとき、O(N K^2 log N)）
 *   空間計算量: O(N K + K^2)
 */
public final class SmallSideBoundedAssignment {

	/**
	 * 割り当て問題の最適解を表すクラス。
	 */
	public static final class Result {
		/**
		 * 割り当ての価値総和の最大値。
		 */
		public final long maxValue;

		/**
		 * assignment[i] = 要素 i が所属するグループ。
		 */
		public final int[] assignment;

		/**
		 * 結果オブジェクトを構築するコンストラクタ。
		 *
		 * @param maxValue 割り当ての価値総和の最大値
		 * @param assignment 各要素が所属するグループの配列
		 */
		Result(long maxValue, int[] assignment) {
			this.maxValue = maxValue;
			this.assignment = assignment;
		}
	}

	/**
	 * 各要素とキーの情報を保持する、Comparableを実装したクラス。
	 * TreapSet 内での順序付けに使用される。
	 */
	private static final class Element implements Comparable<Element> {
		final int item;
		final long key;

		Element(int item, long key) {
			this.item = item;
			this.key = key;
		}

		@Override
		public int compareTo(Element o) {
			if (this.key != o.key) {
				return Long.compare(this.key, o.key);
			}
			return Integer.compare(this.item, o.item);
		}
	}

	/**
	 * lower[g] <= count[g] <= upper[g] の制約の下で、価値の総和を最大化する。
	 *
	 * 計算量:
	 *   期待時間計算量: O(N K^2 log N + K^3)
	 *   （特に K <= N のとき、O(N K^2 log N)）
	 *   空間計算量: O(N K + K^2)
	 *
	 * @param value 各要素を各グループに割り当てたときの価値。N x K の2次元配列。
	 * @param lower 各グループに割り当てる要素数の下限。長さ K の配列。
	 * @param upper 各グループに割り当てる要素数の上限。長さ K の配列.
	 * @return 最大価値とその割り当て結果を格納した Result オブジェクト
	 */
	// 未テスト
	public static Result solveMax(
			long[][] value,
			int[] lower,
			int[] upper) {

		return new Solver(value, lower, upper).solve();
	}

	/**
	 * count[g] = quota[g] の制約の下で、価値の総和を最大化する。
	 *
	 * 計算量:
	 *   期待時間計算量: O(N K^2 log N + K^3)
	 *   （特に K <= N のとき、O(N K^2 log N)）
	 *   空間計算量: O(N K + K^2)
	 *
	 * @param value 各要素を各グループに割り当てたときの価値。N x K の2次元配列。
	 * @param quota 各グループに割り当てる要素数。長さ K の配列。
	 * @return 最大価値とその割り当て結果を格納した Result オブジェクト
	 */
	// 未テスト
	public static Result solveMaxExact(
			long[][] value,
			int[] quota) {

		return solveMax(value, quota, quota);
	}

	/**
	 * Bounded semi-assignment問題のソルバー。
	 */
	private static final class Solver {

		/**
		 * 十分に大きな値。無限大の代用。
		 */
		private static final long INF = Long.MAX_VALUE / 4;

		/**
		 * 超始点（source）を表す定数。
		 */
		private static final int SOURCE = -2;

		/**
		 * 各要素を各グループに割り当てたときの価値。N x K の2次元配列。
		 */
		final long[][] value;

		/**
		 * 各グループに割り当てる要素数の下限。
		 */
		final int[] lower;

		/**
		 * 各グループに割り当てる要素数の上限。
		 */
		final int[] upper;

		/**
		 * 要素数。
		 */
		final int N;

		/**
		 * グループ数。
		 */
		final int K;

		/**
		 * 未割当状態を表す値（K）。
		 */
		final int UNASSIGNED;

		/**
		 * assignment[i] = 現在の所属グループ。
		 * UNASSIGNED はまだ割り当てられていないことを表す。
		 */
		final int[] assignment;

		/**
		 * 現在の各グループの人数。
		 */
		final int[] load;

		/**
		 * 現在 source -> g に許している容量。
		 *
		 * phase 1: lower[g]
		 * phase 2: upper[g]
		 */
		final int[] capacity;

		/**
		 * root[from][state]
		 *
		 * state < K の場合:
		 *   現在 state に所属する item i について
		 *     value[i][state] - value[i][from]
		 *   を持つ TreapSet。
		 *
		 * これは compressed residual edge
		 *     from -> state
		 * のコスト候補。
		 *
		 * state == UNASSIGNED の場合:
		 *   未割当 item i について
		 *     -value[i][from]
		 *   を持つ。
		 *
		 * これは
		 *     from -> sink
		 * のコスト候補。
		 */
		final TreapSet<Element>[][] root;

		/**
		 * 各(from, item)のノードに対応する、現在のキー値。
		 * 削除の際に古いキーを知るために使用する。
		 */
		final long[] key;

		/**
		 * compressed graph の Johnson potential。
		 * 頂点:
		 *   0 ... K-1 : group
		 *   K         : sink
		 * source の potential は常に 0 とみなす。
		 */
		final long[] potential;

		/**
		 * 最短距離。
		 */
		final long[] dist;

		/**
		 * 探索済フラグ。
		 */
		final boolean[] used;

		/**
		 * 最短経路における前の頂点。
		 */
		final int[] prev;

		/**
		 * 最短経路における前の遷移に使われたアイテム。
		 */
		final int[] prevItem;

		/**
		 * ソルバーオブジェクトを初期化する。
		 *
		 * 計算量: O(N * K)
		 *
		 * @param value 価値行列
		 * @param lower 各グループの要素数下限
		 * @param upper 各グループの要素数上限
		 */
		// 未テスト
		@SuppressWarnings("unchecked")
		Solver(
				long[][] value,
				int[] lower,
				int[] upper) {

			if (value == null
					|| lower == null
					|| upper == null) {

				throw new NullPointerException();
			}

			N = value.length;
			K = lower.length;
			UNASSIGNED = K;

			if (upper.length != K) {
				throw new IllegalArgumentException(
					"lower.length != upper.length");
			}

			this.value = value;
			this.lower = lower.clone();
			this.upper = upper.clone();

			long sumLower = 0;
			long sumUpper = 0;

			for (int g = 0; g < K; g++) {
				if (lower[g] < 0
						|| lower[g] > upper[g]) {

					throw new IllegalArgumentException(
						"invalid bounds at group " + g);
				}

				sumLower += lower[g];
				sumUpper += upper[g];
			}

			if (sumLower > N || sumUpper < N) {
				throw new IllegalArgumentException(
					"infeasible group bounds");
			}

			for (int i = 0; i < N; i++) {
				if (value[i] == null
						|| value[i].length != K) {

					throw new IllegalArgumentException(
						"value must be N x K");
				}
			}

			assignment = new int[N];
			Arrays.fill(assignment, UNASSIGNED);

			load = new int[K];

			/*
			 * 最初は lower を capacity とする。
			 */
			capacity = this.lower.clone();

			root = new TreapSet[K][K + 1];

			for (int i = 0; i < K; i++) {
				for (int j = 0; j <= K; j++) {
					root[i][j] = new TreapSet<>();
				}
			}

			long nodeCountLong = (long) N * K;

			if (nodeCountLong > Integer.MAX_VALUE) {
				throw new IllegalArgumentException(
					"N * K is too large");
			}

			int nodeCount = (int) nodeCountLong;

			key = new long[nodeCount];

			potential = new long[K + 1];

			dist = new long[K + 1];
			used = new boolean[K + 1];
			prev = new int[K + 1];
			prevItem = new int[K + 1];

			initializeTreaps();
		}

		/**
		 * Treapを初期化する。
		 *
		 * 計算量: O(N * K log N)
		 */
		// 未テスト
		private void initializeTreaps() {
			for (int from = 0; from < K; from++) {
				for (int item = 0; item < N; item++) {

					int node = nodeId(from, item);

					/*
					 * 最初は全 item が未割当。
					 *
					 * from -> sink の候補コストは
					 *
					 *   -value[item][from].
					 */
					key[node] = -value[item][from];

					root[from][UNASSIGNED].add(new Element(item, key[node]));
				}
			}
		}

		/**
		 * 2つのフェーズ（下限充足と上限充足）で最小費用流を流すことで問題を解く。
		 *
		 * 計算量:
		 *   期待時間計算量: O(N K^2 log N + K^3)
		 *   （特に K <= N のとき、O(N K^2 log N)）
		 *
		 * @return 最適な価値とその割り当て
		 */
		// 未テスト
		Result solve() {

			int sumLower = 0;

			for (int x : lower) {
				sumLower += x;
			}

			/*
			 * Phase 1
			 *
			 * capacity[g] = lower[g]
			 *
			 * として sum(lower) 人を最適に割り当てる。
			 *
			 * 総流量が sum(lower) なので終了時には必ず
			 *
			 *   load[g] = lower[g]
			 *
			 * となる。
			 */
			if (sumLower > 0) {
				recomputePotentials();

				for (int f = 0; f < sumLower; f++) {
					augment();
				}
			}

			/*
			 * Phase 2
			 *
			 * 上限を upper まで開放する。
			 */
			System.arraycopy(
				upper, 0,
				capacity, 0,
				K);

			if (sumLower < N) {

				/*
				 * phase 1 では capacity=0 などにより
				 * source から到達不能だった group がありうる。
				 *
				 * capacity を増やすと新しい residual edge
				 *
				 *   source -> group
				 *
				 * が出現するので、potential を作り直す。
				 */
				recomputePotentials();

				for (int f = sumLower; f < N; f++) {
					augment();
				}
			}

			long answer = 0;

			for (int i = 0; i < N; i++) {
				int g = assignment[i];

				if (g < 0 || g >= K) {
					throw new AssertionError(
						"unassigned item remains");
				}

				answer += value[i][g];
			}

			for (int g = 0; g < K; g++) {
				if (load[g] < lower[g]
						|| load[g] > upper[g]) {

					throw new AssertionError(
						"group bound violation");
				}
			}

			return new Result(
				answer,
				assignment.clone());
		}

		/**
		 * 現在の compressed residual graph 上で
		 * feasible potential を再計算する。
		 *
		 * 頂点数 O(K)、辺数 O(K^2) なので
		 * Bellman-Ford を O(K^3) で行う。
		 *
		 * これは phase の切り替え時にしか呼ばない。
		 */
		// 未テスト
		private void recomputePotentials() {

			Arrays.fill(potential, INF);

			/*
			 * source -> g, cost 0
			 */
			for (int g = 0; g < K; g++) {
				if (load[g] < capacity[g]) {
					potential[g] = 0;
				}
			}

			/*
			 * source からの raw shortest distance を求める。
			 *
			 * potential[v] = distance(source, v)
			 *
			 * とすれば
			 *
			 *   cost(u,v) + p[u] - p[v] >= 0
			 *
			 * になる。
			 */
			for (int it = 0; it <= K; it++) {

				boolean changed = false;

				for (int u = 0; u < K; u++) {

					long du = potential[u];

					if (du == INF) continue;

					/*
					 * group u -> group v
					 */
					for (int v = 0; v < K; v++) {

						if (u == v) continue;

						TreapSet<Element> ts = root[u][v];

						if (ts.isEmpty()) continue;

						Element minElem = ts.kth(0);

						long nd =
							du + minElem.key;

						if (nd < potential[v]) {
							potential[v] = nd;
							changed = true;
						}
					}

					/*
					 * group u -> sink
					 */
					TreapSet<Element> ts =
						root[u][UNASSIGNED];

					if (!ts.isEmpty()) {

						Element minElem = ts.kth(0);

						long nd =
							du + minElem.key;

						if (nd < potential[K]) {
							potential[K] = nd;
							changed = true;
						}
					}
				}

				if (!changed) break;

				if (it == K) {
					throw new AssertionError(
						"negative residual cycle");
				}
			}

			/*
			 * source から到達不能な頂点は今回の augment では
			 * 使用されない。
			 */
			for (int v = 0; v <= K; v++) {
				if (potential[v] == INF) {
					potential[v] = 0;
				}
			}
		}

		/**
		 * compressed residual graph 上で 1 unit augment する。
		 *
		 * source -> g:
		 *
		 *   load[g] < capacity[g]
		 *
		 * のとき cost 0。
		 *
		 *
		 * a -> b:
		 *
		 *   min_(i : assignment[i] = b)
		 *       value[i][b] - value[i][a]
		 *
		 *
		 * a -> sink:
		 *
		 *   min_(i : assignment[i] = UNASSIGNED)
		 *       -value[i][a]
		 */
		// 未テスト
		private void augment() {

			final int sink = K;

			Arrays.fill(dist, INF);
			Arrays.fill(used, false);
			Arrays.fill(prev, -1);
			Arrays.fill(prevItem, -1);

			/*
			 * source は明示的には持たない。
			 *
			 * reduced cost:
			 *
			 *   0 + p[source] - p[g]
			 * = -p[g].
			 */
			for (int g = 0; g < K; g++) {

				if (load[g] >= capacity[g]) {
					continue;
				}

				long d = -potential[g];

				if (d < 0) {
					throw new AssertionError(
						"negative reduced source edge");
				}

				dist[g] = d;
				prev[g] = SOURCE;
			}

			/*
			 * 頂点数 K+1 の dense Dijkstra。
			 *
			 * O(K^2)
			 */
			for (int it = 0; it <= K; it++) {

				int u = -1;
				long best = INF;

				for (int v = 0; v <= K; v++) {

					if (!used[v]
							&& dist[v] < best) {

						best = dist[v];
						u = v;
					}
				}

				if (u == -1) break;

				used[u] = true;

				if (u == sink) {
					continue;
				}

				/*
				 * u -> v
				 *
				 * 現在 v にいる item を u へ移す。
				 */
				for (int v = 0; v < K; v++) {

					if (v == u) continue;

					TreapSet<Element> ts = root[u][v];

					if (ts.isEmpty()) continue;

					Element minElem = ts.kth(0);

					long rawCost = minElem.key;

					long reducedCost =
						rawCost
						+ potential[u]
						- potential[v];

					if (reducedCost < 0) {
						throw new AssertionError(
							"negative reduced exchange edge");
					}

					long nd =
						dist[u] + reducedCost;

					if (nd < dist[v]) {

						dist[v] = nd;
						prev[v] = u;
						prevItem[v] = minElem.item;
					}
				}

				/*
				 * u -> sink
				 *
				 * 未割当 item を u に入れる。
				 */
				TreapSet<Element> ts =
					root[u][UNASSIGNED];

				if (!ts.isEmpty()) {

					Element minElem = ts.kth(0);

					long rawCost = minElem.key;

					long reducedCost =
						rawCost
						+ potential[u]
						- potential[sink];

					if (reducedCost < 0) {
						throw new AssertionError(
							"negative reduced terminal edge");
					}

					long nd =
						dist[u] + reducedCost;

					if (nd < dist[sink]) {

						dist[sink] = nd;
						prev[sink] = u;
						prevItem[sink] = minElem.item;
					}
				}
			}

			if (dist[sink] == INF) {
				throw new AssertionError(
					"no augmenting path");
			}

			/*
			 * Johnson potential update。
			 */
			for (int v = 0; v <= K; v++) {

				if (dist[v] != INF) {
					potential[v] += dist[v];
				}
			}

			/*
			 * augmenting path:
			 *
			 * source
			 *   -> g0
			 *   -> g1
			 *   -> ...
			 *   -> gm
			 *   -> sink
			 *
			 * が見つかったとする。
			 *
			 * 実際には
			 *
			 * item(g1): g1 -> g0
			 * item(g2): g2 -> g1
			 * ...
			 * new item : U  -> gm
			 *
			 * と移動する。
			 *
			 * 後ろから適用する。
			 */

			int g = prev[sink];
			int item = prevItem[sink];

			if (g < 0 || item < 0) {
				throw new AssertionError();
			}

			/*
			 * UNASSIGNED -> g
			 */
			moveItem(item, g);

			while (prev[g] != SOURCE) {

				int p = prev[g];
				int moved = prevItem[g];

				if (p < 0 || moved < 0) {
					throw new AssertionError();
				}

				if (assignment[moved] != g) {
					throw new AssertionError(
						"broken augmenting path");
				}

				/*
				 * g -> p
				 */
				moveItem(moved, p);

				g = p;
			}

			/*
			 * 中間 group は
			 *
			 *   -1 + 1 = 0
			 *
			 * なので人数不変。
			 *
			 * source 直後の group だけ +1。
			 */
			load[g]++;
		}

		/**
		 * item の所属を oldState -> newState に変更する。
		 *
		 * item に関係する K 個の TreapSet を更新する。
		 *
		 * 期待時間計算量: O(K log N)
		 *
		 * @param item アイテムのインデックス
		 * @param newState 変更後の状態
		 */
		// 未テスト
		private void moveItem(
				int item,
				int newState) {

			int oldState =
				assignment[item];

			if (oldState == newState) {
				return;
			}

			for (int from = 0;
					from < K;
					from++) {

				int node =
					nodeId(from, item);

				long oldKey = key[node];

				root[from][oldState].remove(new Element(item, oldKey));

				long newKey;
				if (newState == UNASSIGNED) {

					newKey =
						-value[item][from];

				} else {

					/*
					 * item が newState にいるとき、
					 *
					 * compressed edge
					 *
					 *   from -> newState
					 *
					 * は item を
					 *
					 *   newState -> from
					 *
					 * に移すことを表す。
					 *
					 * 最大化問題を min-cost residual
					 * として見ると、そのコストは
					 *
					 *   value[item][newState]
					 * - value[item][from].
					 */
					newKey =
						value[item][newState]
						- value[item][from];
				}

				key[node] = newKey;

				root[from][newState].add(new Element(item, newKey));
			}

			assignment[item] = newState;
		}

		/**
		 * アイテムと状態のペアからノードIDを計算する。
		 *
		 * 計算量: O(1)
		 *
		 * @param from 状態
		 * @param item アイテム
		 * @return ノードID
		 */
		// 未テスト
		private int nodeId(
				int from,
				int item) {

			return from * N + item;
		}
	}
}
