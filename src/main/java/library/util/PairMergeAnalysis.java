package library.util;

import java.util.Arrays;

/**
 * 完全 DFA における状態ペアの併合 (synchronization / merge) 可能性および最短併合系列の解析を行う構造体。
 *
 * <p>
 * DFA の状態数を N, アルファベットサイズを K とするとき、
 * 構築の時間計算量は O(K N^2), 空間計算量は O(N^2 + K N) である。
 */
public class PairMergeAnalysis {
	/**
	 * DFA の遷移テーブルの参照。
	 */
	private final int[][] adj;

	/**
	 * pairId(u, v) に対する最小併合語長。存在しない場合は -1。
	 */
	private final int[] dist;

	/**
	 * pairId(u, v) (u != v) を併合する最短語の先頭記号。singleton または併合不可能な場合は -1。
	 */
	private final int[] nextSymbol;

	/**
	 * 全状態集合 Q に対し |δ(Q, w)| = 1 となる語 w が存在するかどうか。
	 */
	private final boolean synchronizing;

	/**
	 * DFA の状態ペア併合構造を構築する。
	 *
	 * @param dfa 対象の完全 DFA
	 * @throws IllegalStateException DFA に未定義遷移 (-1) が含まれる場合
	 */
	// 未テスト
	public PairMergeAnalysis(DFA dfa) {
		int n = dfa.n;
		int k = dfa.alphabetSize;

		adj = dfa.adj;
		for (int u = 0; u < n; u++) {
			for (int c = 0; c < k; c++) {
				if (adj[u][c] == -1) {
					throw new IllegalStateException(
							"PairMergeAnalysis requires a complete DFA.");
				}
			}
		}

		int pairCount = Math.toIntExact((long) n * (n + 1) / 2);

		dist = new int[pairCount];
		nextSymbol = new int[pairCount];

		Arrays.fill(dist, -1);
		Arrays.fill(nextSymbol, -1);

		/*
		 * predHead[c][v] を先頭として、
		 * {u | δ(u,c)=v}
		 * を predNext[c] で連結する。
		 */
		int[][] predHead = new int[k][n];
		int[][] predNext = new int[k][n];

		for (int c = 0; c < k; c++) {
			Arrays.fill(predHead[c], -1);

			for (int u = 0; u < n; u++) {
				int v = adj[u][c];

				predNext[c][u] = predHead[c][v];
				predHead[c][v] = u;
			}
		}

		/*
		 * queueU[t], queueV[t]:
		 * reverse BFS の t 番目の pair {queueU[t],queueV[t]}。
		 */
		int[] queueU = new int[pairCount];
		int[] queueV = new int[pairCount];
		int head = 0;
		int tail = 0;

		for (int v = 0; v < n; v++) {
			int id = pairId(v, v);

			dist[id] = 0;
			queueU[tail] = v;
			queueV[tail] = v;
			tail++;
		}

		while (head < tail) {
			int a = queueU[head];
			int b = queueV[head];
			head++;

			int cur = pairId(a, b);

			for (int c = 0; c < k; c++) {
				if (a != b) {
					/*
					 * δ(u,c)=a, δ(v,c)=b を全列挙。
					 */
					for (int u = predHead[c][a];
							u != -1;
							u = predNext[c][u]) {

						for (int v = predHead[c][b];
								v != -1;
								v = predNext[c][v]) {

							int src = pairId(u, v);

							if (dist[src] != -1)
								continue;

							dist[src] = dist[cur] + 1;
							nextSymbol[src] = c;

							queueU[tail] = u;
							queueV[tail] = v;
							tail++;
						}
					}
				} else {
					/*
					 * δ(u,c)=δ(v,c)=a となる unordered pair を全列挙。
					 *
					 * predecessor list 上で v を u 以降だけ走らせることで、
					 * 各 pair をちょうど一度列挙する。
					 */
					for (int u = predHead[c][a];
							u != -1;
							u = predNext[c][u]) {

						for (int v = u;
								v != -1;
								v = predNext[c][v]) {

							int src = pairId(u, v);

							if (dist[src] != -1)
								continue;

							dist[src] = dist[cur] + 1;
							nextSymbol[src] = c;

							queueU[tail] = u;
							queueV[tail] = v;
							tail++;
						}
					}
				}
			}
		}

		synchronizing = tail == pairCount;
	}

	/**
	 * 状態 u と 状態 v が併合可能か（ある語 w により δ(u, w) = δ(v, w) となるか）を判定する。
	 *
	 * 時間計算量: O(1)
	 *
	 * @param u 状態番号 (0 <= u < N)
	 * @param v 状態番号 (0 <= v < N)
	 * @return 併合可能なら true、そうでなければ false
	 */
	// 未テスト
	public boolean canMerge(int u, int v) {
		return dist[pairId(u, v)] != -1;
	}

	/**
	 * 状態 u と 状態 v を併合する最短語の長さを返す。
	 *
	 * 時間計算量: O(1)
	 *
	 * @param u 状態番号 (0 <= u < N)
	 * @param v 状態番号 (0 <= v < N)
	 * @return 最短併合語の長さ。併合不可能な場合は -1
	 */
	// 未テスト
	public int mergeDistance(int u, int v) {
		return dist[pairId(u, v)];
	}

	/**
	 * 状態 u と 状態 v を併合する最短語（記号の配列）を復元して返す。
	 *
	 * 時間計算量: O(L) (L は復元される語の長さ, L <= N(N-1)/2)
	 *
	 * @param u 状態番号 (0 <= u < N)
	 * @param v 状態番号 (0 <= v < N)
	 * @return 最短併合語の記号列。併合不可能な場合は null
	 */
	// 未テスト
	public int[] restoreMergeWord(int u, int v) {
		int id = pairId(u, v);
		int len = dist[id];

		if (len == -1)
			return null;

		int[] ret = new int[len];

		for (int i = 0; i < len; i++) {
			int c = nextSymbol[id];

			ret[i] = c;

			u = adj[u][c];
			v = adj[v][c];
			id = pairId(u, v);
		}

		return ret;
	}

	/**
	 * DFA が synchronizing（任意の2状態ペアが併合可能、すなわち全状態を1つの状態へ縮約する語が存在する）かどうかを返す。
	 *
	 * 時間計算量: O(1)
	 *
	 * @return synchronizing であれば true、そうでなければ false
	 */
	// 未テスト
	public boolean isSynchronizing() {
		return synchronizing;
	}

	/**
	 * 2つの状態番号 (u, v) に対する非順序ペア {u, v} の識別 ID を計算する。
	 *
	 * 時間計算量: O(1)
	 *
	 * @param u 状態番号 1
	 * @param v 状態番号 2
	 * @return ペア ID
	 */
	// 未テスト
	private static int pairId(int u, int v) {
		if (u > v) {
			int tmp = u;
			u = v;
			v = tmp;
		}

		return (int) ((long) v * (v + 1) / 2 + u);
	}
}
