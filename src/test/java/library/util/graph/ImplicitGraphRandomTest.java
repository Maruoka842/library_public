package library.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.collections.OpenHashMap;
import library.util.collections.OpenHashSet;

public class ImplicitGraphRandomTest {

	/** ランダムグラフの生成回数。 */
	private static final int CASES = 500;
	/** 到達不能を表す距離。 */
	private static final int INF = 1 << 28;

	/**
	 * JUnit テストランナーからランダムグラフテストを実行する。
	 *
	 * <p>計算量: 最大頂点数を N、最大辺数を M として O(CASES * N * (N + M))、
	 * 空間 O(N + M)。</p>
	 */
	// 未テスト
	@Test
	public void testRandomGraph() {
		main(new String[0]);
	}

	/**
	 * ランダムな単純無向グラフについて、各探索メソッドを素朴な BFS の結果と照合する。
	 *
	 * <p>計算量: 最大頂点数を N、最大辺数を M として O(CASES * N * (N + M))、
	 * 空間 O(N + M)。</p>
	 *
	 * @param args 使用しない
	 */
	// 未テスト
	public static void main(String[] args) {
		Random random = new Random(0x1A2B3C4D5E6F7809L);
		for (int tc = 0; tc < CASES; tc++) {
			int n = 1 + random.nextInt(25);
			List<Integer>[] adjacent = randomGraph(random, n);
			ImplicitGraph<Integer> graph = new ImplicitGraph<Integer>() {
				@Override
				public Iterable<Integer> nextStates(Integer v) {
					return adjacent[v];
				}
			};

			for (int v = 0; v < n; v++) {
				if (graph.degree(v) != adjacent[v].size()) fail(tc, "degree");
			}

			for (int s = 0; s < n; s++) {
				int[] expected = naiveDistances(adjacent, List.of(s));
				checkSingleSourceMethods(tc, graph, adjacent, s, expected, n);
			}

			List<Integer> sources = new ArrayList<>();
			for (int v = 0; v < n; v++) {
				if (random.nextBoolean()) sources.add(v);
			}
			int[] expected = naiveDistances(adjacent, sources);
			checkDistances(tc, graph.bfsDistances(sources), expected, Integer.MAX_VALUE);
			int cutoff = random.nextInt(n + 2) - 1;
			checkDistances(tc, graph.bfsDistances(sources, cutoff), expected, cutoff);
		}
		System.out.println("OK: " + CASES + " random graphs");
	}

	/**
	 * 一つの始点に対する全探索 API を検査する。
	 *
	 * <p>計算量: 頂点数を N、辺数を M として O(N * (N + M))、空間 O(N)。</p>
	 *
	 * @param tc テストケース番号
	 * @param graph 検査対象
	 * @param adjacent 明示的な隣接リスト
	 * @param source 始点
	 * @param expected 素朴な BFS による距離
	 * @param n 頂点数
	 */
	// 未テスト
	private static void checkSingleSourceMethods(int tc, ImplicitGraph<Integer> graph,
			List<Integer>[] adjacent, int source, int[] expected, int n) {
		checkDistances(tc, graph.bfsDistances(source), expected, Integer.MAX_VALUE);
		int reachableCount = 0;
		for (int distance : expected) if (distance < INF) reachableCount++;
		if (graph.countReachable(source) != reachableCount) fail(tc, "countReachable");
		checkSet(tc, graph.reachables(source), expected, Integer.MAX_VALUE);
		checkOrder(tc, graph.bfsOrder(source), expected, Integer.MAX_VALUE);

		for (int cutoff = -1; cutoff <= n; cutoff++) {
			checkDistances(tc, graph.bfsDistances(source, cutoff), expected, cutoff);
			checkSet(tc, graph.reachables(source, cutoff), expected, cutoff);
			checkOrder(tc, graph.bfsOrder(source, cutoff), expected, cutoff);
			int count = 0;
			for (int distance : expected) if (distance < INF && distance <= cutoff) count++;
			if (graph.countReachable(source, cutoff) != count) fail(tc, "countReachable cutoff");
		}

		for (int target = 0; target < n; target++) {
			long distance = expected[target] == INF ? -1 : expected[target];
			if (graph.dist(source, target) != distance) fail(tc, "dist");
			if (graph.isConnected(source, target) != (distance >= 0)) fail(tc, "isConnected");
			checkPath(tc, graph.path(source, target), adjacent, source, target, distance);
			for (int cutoff = -1; cutoff <= n; cutoff++) {
				long bounded = distance >= 0 && distance <= cutoff ? distance : -1;
				if (graph.dist(source, target, cutoff) != bounded) fail(tc, "dist cutoff");
				if (graph.isConnected(source, target, cutoff) != (bounded >= 0))
					fail(tc, "isConnected cutoff");
				checkPath(tc, graph.path(source, target, cutoff), adjacent, source, target, bounded);
			}
		}
	}

	/**
	 * ランダムな単純無向グラフの隣接リストを返す。
	 *
	 * <p>計算量: 頂点数を N として O(N^2)、空間 O(N^2)。</p>
	 *
	 * @param random 乱数生成器
	 * @param n 頂点数
	 * @return 対称な隣接リスト
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	private static List<Integer>[] randomGraph(Random random, int n) {
		List<Integer>[] adjacent = new List[n];
		for (int v = 0; v < n; v++) adjacent[v] = new ArrayList<>();
		for (int u = 0; u < n; u++) {
			for (int v = u + 1; v < n; v++) {
				if (random.nextInt(5) == 0) {
					adjacent[u].add(v);
					adjacent[v].add(u);
				}
			}
		}
		return adjacent;
	}

	/**
	 * 複数始点 BFS による距離を返す。
	 *
	 * <p>計算量: 頂点数を N、辺数を M として O(N + M)、空間 O(N)。</p>
	 *
	 * @param adjacent 隣接リスト
	 * @param sources 始点の集合
	 * @return 各頂点への最短距離。到達不能なら {@link #INF}
	 */
	// 未テスト
	private static int[] naiveDistances(List<Integer>[] adjacent, Iterable<Integer> sources) {
		int[] distances = new int[adjacent.length];
		Arrays.fill(distances, INF);
		Queue<Integer> queue = new ArrayDeque<>();
		for (int source : sources) {
			if (distances[source] == INF) {
				distances[source] = 0;
				queue.add(source);
			}
		}
		while (!queue.isEmpty()) {
			int v = queue.poll();
			for (int u : adjacent[v]) {
				if (distances[u] == INF) {
					distances[u] = distances[v] + 1;
					queue.add(u);
				}
			}
		}
		return distances;
	}

	/**
	 * 距離マップが期待値と一致することを検査する。
	 *
	 * <p>計算量: 頂点数を N として期待 O(N)、空間 O(1)。</p>
	 *
	 * @param tc テストケース番号
	 * @param actual 検査対象の距離マップ
	 * @param expected 期待する距離
	 * @param cutoff 距離の上限
	 */
	// 未テスト
	private static void checkDistances(int tc, OpenHashMap<Integer, Integer> actual,
			int[] expected, int cutoff) {
		int size = 0;
		for (int v = 0; v < expected.length; v++) {
			boolean contained = expected[v] < INF && expected[v] <= cutoff;
			if (actual.containsKey(v) != contained) fail(tc, "bfsDistances keys");
			if (contained) {
				size++;
				if (actual.get(v) != expected[v]) fail(tc, "bfsDistances values");
			}
		}
		if (actual.size() != size) fail(tc, "bfsDistances size");
	}

	/**
	 * 頂点集合が期待する距離球と一致することを検査する。
	 *
	 * <p>計算量: 頂点数を N として期待 O(N)、空間 O(1)。</p>
	 *
	 * @param tc テストケース番号
	 * @param actual 検査対象の集合
	 * @param expected 始点からの距離
	 * @param cutoff 距離の上限
	 */
	// 未テスト
	private static void checkSet(int tc, OpenHashSet<Integer> actual, int[] expected, int cutoff) {
		int size = 0;
		for (int v = 0; v < expected.length; v++) {
			boolean contained = expected[v] < INF && expected[v] <= cutoff;
			if (actual.contains(v) != contained) fail(tc, "reachables keys");
			if (contained) size++;
		}
		if (actual.size() != size) fail(tc, "reachables size");
	}

	/**
	 * BFS 順が距離球の全頂点を距離非減少順に含むことを検査する。
	 *
	 * <p>計算量: 頂点数を N として O(N)、空間 O(N)。</p>
	 *
	 * @param tc テストケース番号
	 * @param actual 検査対象の頂点列
	 * @param expected 始点からの距離
	 * @param cutoff 距離の上限
	 */
	// 未テスト
	private static void checkOrder(int tc, List<Integer> actual, int[] expected, int cutoff) {
		boolean[] appeared = new boolean[expected.length];
		int lastDistance = -1;
		for (int v : actual) {
			if (appeared[v] || expected[v] > cutoff || expected[v] < lastDistance)
				fail(tc, "bfsOrder");
			appeared[v] = true;
			lastDistance = expected[v];
		}
		for (int v = 0; v < expected.length; v++) {
			if (appeared[v] != (expected[v] < INF && expected[v] <= cutoff)) fail(tc, "bfsOrder keys");
		}
	}

	/**
	 * 返された経路が期待する最短距離を持つことを検査する。
	 *
	 * <p>計算量: 経路長を L、最大次数を D として O(LD)、空間 O(1)。</p>
	 *
	 * @param tc テストケース番号
	 * @param path 検査対象の経路
	 * @param adjacent 隣接リスト
	 * @param source 始点
	 * @param target 終点
	 * @param distance 期待する距離。経路が存在しないなら -1
	 */
	// 未テスト
	private static void checkPath(int tc, List<Integer> path, List<Integer>[] adjacent,
			int source, int target, long distance) {
		if (distance < 0) {
			if (path != null) fail(tc, "path must be null");
			return;
		}
		if (path == null || path.size() != distance + 1
				|| path.get(0) != source || path.get(path.size() - 1) != target)
			fail(tc, "path endpoints or length");
		for (int i = 1; i < path.size(); i++) {
			if (!adjacent[path.get(i - 1)].contains(path.get(i))) fail(tc, "path edge");
		}
	}

	/**
	 * テスト失敗を通知する。
	 *
	 * <p>計算量: O(1)。</p>
	 *
	 * @param tc テストケース番号
	 * @param method 失敗した検査名
	 */
	// 未テスト
	private static void fail(int tc, String method) {
		throw new AssertionError("case=" + tc + ", method=" + method);
	}
}
