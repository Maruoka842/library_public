package library.util.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import library.util.collections.Hash;
import library.util.collections.OpenHashMap;
import library.util.collections.OpenHashSet;

/**
 * 暗黙的に定義された無向グラフを扱うための抽象クラス。
 * {@link #nextStates(Object)} が定める隣接関係は対称である。
 *
 * @param <V> 頂点の型
 */
public abstract class ImplicitGraph<V> {

	/** 頂点のハッシュ値と同値性を定める戦略。 */
	protected final Hash.Strategy<V> strategy;

	/**
	 * デフォルトのハッシュ戦略を使用するコンストラクタ。
	 */
	protected ImplicitGraph() {
		this(Hash.defaultStrategy());
	}

	/**
	 * 指定されたハッシュ戦略を使用するコンストラクタ。
	 *
	 * @param strategy ハッシュ戦略
	 */
	protected ImplicitGraph(Hash.Strategy<V> strategy) {
		this.strategy = strategy;
	}

	/**
	 * 頂点 {@code v} に隣接する全頂点を返す。
	 * 任意の頂点 {@code u}, {@code v} に対して、{@code u} が {@code nextStates(v)} に含まれることと
	 * {@code v} が {@code nextStates(u)} に含まれることは同値である。
	 *
	 * <p>計算量: 具象クラスの契約による。</p>
	 *
	 * @param v 頂点
	 * @return {@code v} に隣接する全頂点
	 */
	public abstract Iterable<V> nextStates(V v);

	/**
	 * 頂点 {@code src} と {@code dst} の最短距離を双方向 BFS で返す。
	 * 各反復では未展開頂点数が少ない側の同一距離層を一括して展開する。
	 *
	 * <p>計算量: 探索した頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @param dst 終点
	 * @return 最短距離。連結でない場合は -1
	 */
	// 未テスト
	public long dist(V src, V dst) {
		return dist(src, dst, Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code src} と {@code dst} の最短距離が {@code cutoff} 以下なら、その距離を
	 * 双方向 BFS で返す。最短距離が {@code cutoff} より大きい場合は、その判定に不要な
	 * 距離層を展開しない。
	 *
	 * <p>計算量: 距離 {@code cutoff} までに探索した頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @param dst 終点
	 * @param cutoff 探索する距離の上限
	 * @return 最短距離。最短距離が {@code cutoff} より大きいか、連結でない場合は -1
	 */
	public long dist(V src, V dst, int cutoff) {
		//https://atcoder.jp/contests/abc336/submissions/79308149
		if (cutoff < 0) return -1;
		if (strategy.equals(src, dst)) return 0;

		OpenHashMap<V, Integer> srcDist = new OpenHashMap<>(strategy);
		OpenHashMap<V, Integer> dstDist = new OpenHashMap<>(strategy);
		List<V> srcFrontier = new ArrayList<>();
		List<V> dstFrontier = new ArrayList<>();
		srcDist.put(src, 0);
		dstDist.put(dst, 0);
		srcFrontier.add(src);
		dstFrontier.add(dst);
		long best = Long.MAX_VALUE;

		while (!srcFrontier.isEmpty() && !dstFrontier.isEmpty()) {
			int srcDepth = srcDist.get(srcFrontier.get(0));
			int dstDepth = dstDist.get(dstFrontier.get(0));
			long lowerBound = (long) srcDepth + dstDepth + 1;
			if (lowerBound >= best) return best;
			if (lowerBound > cutoff) return best == Long.MAX_VALUE ? -1 : best;

			boolean fromSrc = srcFrontier.size() <= dstFrontier.size();
			List<V> frontier = fromSrc ? srcFrontier : dstFrontier;
			OpenHashMap<V, Integer> ownDist = fromSrc ? srcDist : dstDist;
			OpenHashMap<V, Integer> otherDist = fromSrc ? dstDist : srcDist;
			List<V> nextFrontier = new ArrayList<>();

			for (V v : frontier) {
				int nextDistance = ownDist.get(v) + 1;
				for (V u : nextStates(v)) {
					if (!ownDist.containsKey(u)) {
						ownDist.put(u, nextDistance);
						nextFrontier.add(u);
						if (otherDist.containsKey(u)) {
							best = Math.min(best, (long) nextDistance + otherDist.get(u));
						}
					}
				}
			}
			if (fromSrc) srcFrontier = nextFrontier;
			else dstFrontier = nextFrontier;
		}
		return best == Long.MAX_VALUE ? -1 : best;
	}

	/**
	 * 頂点 {@code src} から {@code dst} への最短経路を双方向 BFS で返す。
	 *
	 * <p>計算量: 探索した頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @param dst 終点
	 * @return 始点と終点を含む最短経路。連結でない場合は null
	 */
	// 未テスト
	public List<V> path(V src, V dst) {
		return path(src, dst, Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code src} から {@code dst} への最短距離が {@code cutoff} 以下なら、
	 * その最短経路を双方向 BFS で返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} までに探索した頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @param dst 終点
	 * @param cutoff 探索する距離の上限
	 * @return 始点と終点を含む最短経路。最短距離が {@code cutoff} より大きいか、連結でない場合は null
	 */
	// 未テスト
	public List<V> path(V src, V dst, int cutoff) {
		if (cutoff < 0) return null;
		if (strategy.equals(src, dst)) return new ArrayList<>(List.of(src));

		OpenHashMap<V, Integer> srcDist = new OpenHashMap<>(strategy);
		OpenHashMap<V, Integer> dstDist = new OpenHashMap<>(strategy);
		OpenHashMap<V, V> srcPrev = new OpenHashMap<>(strategy);
		OpenHashMap<V, V> dstNext = new OpenHashMap<>(strategy);
		List<V> srcFrontier = new ArrayList<>();
		List<V> dstFrontier = new ArrayList<>();
		srcDist.put(src, 0);
		dstDist.put(dst, 0);
		srcPrev.put(src, null);
		dstNext.put(dst, null);
		srcFrontier.add(src);
		dstFrontier.add(dst);

		while (!srcFrontier.isEmpty() && !dstFrontier.isEmpty()) {
			int srcDepth = srcDist.get(srcFrontier.get(0));
			int dstDepth = dstDist.get(dstFrontier.get(0));
			if ((long) srcDepth + dstDepth >= cutoff) return null;

			boolean fromSrc = srcFrontier.size() <= dstFrontier.size();
			List<V> frontier = fromSrc ? srcFrontier : dstFrontier;
			OpenHashMap<V, Integer> ownDist = fromSrc ? srcDist : dstDist;
			OpenHashMap<V, Integer> otherDist = fromSrc ? dstDist : srcDist;
			OpenHashMap<V, V> ownLink = fromSrc ? srcPrev : dstNext;
			List<V> nextFrontier = new ArrayList<>();
			long answer = Long.MAX_VALUE;
			V meeting = null;

			for (V v : frontier) {
				int nextDistance = ownDist.get(v) + 1;
				for (V u : nextStates(v)) {
					if (!ownDist.containsKey(u)) {
						ownDist.put(u, nextDistance);
						ownLink.put(u, v);
						nextFrontier.add(u);
						if (otherDist.containsKey(u)) {
							long candidate = (long) nextDistance + otherDist.get(u);
							if (candidate < answer) {
								answer = candidate;
								meeting = u;
							}
						}
					}
				}
			}
			if (meeting != null) {
				List<V> ret = new ArrayList<>();
				for (V v = meeting; v != null; v = srcPrev.get(v)) ret.add(v);
				Collections.reverse(ret);
				for (V v = dstNext.get(meeting); v != null; v = dstNext.get(v)) ret.add(v);
				return ret;
			}
			if (fromSrc) srcFrontier = nextFrontier;
			else dstFrontier = nextFrontier;
		}
		return null;
	}

	/**
	 * 頂点 {@code src} から到達可能な各頂点への最短距離を返す。
	 *
	 * <p>計算量: 到達可能な頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @return 到達可能な頂点をキー、その最短距離を値とするマップ
	 */
	// 未テスト
	public OpenHashMap<V, Integer> bfsDistances(V src) {
		return bfsDistances(List.of(src), Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code src} からの最短距離が {@code cutoff} 以下である各頂点への最短距離を返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} 以内の頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 始点
	 * @param cutoff 距離の上限
	 * @return 条件を満たす頂点をキー、その最短距離を値とするマップ
	 */
	// 未テスト
	public OpenHashMap<V, Integer> bfsDistances(V src, int cutoff) {
		return bfsDistances(List.of(src), cutoff);
	}

	/**
	 * 集合 {@code sources} からの最短距離が有限である各頂点について、その最短距離を返す。
	 *
	 * <p>計算量: 到達可能な頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param sources 始点の集合
	 * @return 到達可能な頂点をキー、最も近い始点からの最短距離を値とするマップ
	 */
	// 未テスト
	public OpenHashMap<V, Integer> bfsDistances(Iterable<V> sources) {
		return bfsDistances(sources, Integer.MAX_VALUE);
	}

	/**
	 * 集合 {@code sources} からの最短距離が {@code cutoff} 以下である各頂点について、
	 * その最短距離を返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} 以内の頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param sources 始点の集合
	 * @param cutoff 距離の上限
	 * @return 条件を満たす頂点をキー、最も近い始点からの最短距離を値とするマップ
	 */
	// 未テスト
	public OpenHashMap<V, Integer> bfsDistances(Iterable<V> sources, int cutoff) {
		OpenHashMap<V, Integer> distances = new OpenHashMap<>(strategy);
		if (cutoff < 0) return distances;

		Queue<V> queue = new ArrayDeque<>();
		for (V src : sources) {
			if (!distances.containsKey(src)) {
				distances.put(src, 0);
				queue.add(src);
			}
		}
		while (!queue.isEmpty()) {
			V v = queue.poll();
			int distance = distances.get(v);
			if (distance == cutoff) continue;
			for (V u : nextStates(v)) {
				if (!distances.containsKey(u)) {
					distances.put(u, distance + 1);
					queue.add(u);
				}
			}
		}
		return distances;
	}

	/**
	 * 頂点 {@code src} と {@code dst} が同じ連結成分に属するかを返す。
	 *
	 * <p>計算量: 探索した頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 一方の頂点
	 * @param dst 他方の頂点
	 * @return 同じ連結成分に属するなら true
	 */
	// 未テスト
	public boolean isConnected(V src, V dst) {
		return dist(src, dst) >= 0;
	}

	/**
	 * 頂点 {@code src} と {@code dst} の最短距離が {@code cutoff} 以下であるかを返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} までに探索した頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param src 一方の頂点
	 * @param dst 他方の頂点
	 * @param cutoff 距離の上限
	 * @return 最短距離が {@code cutoff} 以下なら true
	 */
	// 未テスト
	public boolean isConnected(V src, V dst, int cutoff) {
		return dist(src, dst, cutoff) >= 0;
	}

	/**
	 * 頂点 {@code start} から到達可能な頂点の集合を返す。
	 *
	 * <p>計算量: 到達可能な頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @return {@code start} の連結成分に含まれる頂点の集合
	 */
	// 未テスト
	public OpenHashSet<V> reachables(V start) {
		return reachables(start, Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code start} からの最短距離が {@code cutoff} 以下である頂点の集合を返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} 以内の頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @param cutoff 距離の上限
	 * @return 条件を満たす頂点の集合
	 */
	// 未テスト
	public OpenHashSet<V> reachables(V start, int cutoff) {
		OpenHashSet<V> ret = new OpenHashSet<>(strategy);
		for (var entry : bfsDistances(start, cutoff).entrySet()) ret.add(entry.key);
		return ret;
	}

	/**
	 * 頂点 {@code start} から到達可能な頂点数を返す。
	 *
	 * <p>計算量: 到達可能な頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @return {@code start} の連結成分の頂点数
	 */
	// 未テスト
	public int countReachable(V start) {
		return countReachable(start, Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code start} からの最短距離が {@code cutoff} 以下である頂点数を返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} 以内の頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @param cutoff 距離の上限
	 * @return 条件を満たす頂点数
	 */
	// 未テスト
	public int countReachable(V start, int cutoff) {
		return bfsDistances(start, cutoff).size();
	}

	/**
	 * 頂点 {@code v} の次数を返す。
	 *
	 * <p>計算量: {@code degree(v)} を d として O(d)、空間 O(1)。</p>
	 *
	 * @param v 頂点
	 * @return {@code v} に隣接する頂点数
	 */
	// 未テスト
	public int degree(V v) {
		int ret = 0;
		for (V ignored : nextStates(v)) ret++;
		return ret;
	}

	/**
	 * 頂点 {@code start} から到達可能な頂点を BFS で発見した順に返す。
	 *
	 * <p>計算量: 到達可能な頂点数を V、列挙した辺数を M として期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @return BFS で発見した順の頂点列
	 */
	// 未テスト
	public List<V> bfsOrder(V start) {
		return bfsOrder(start, Integer.MAX_VALUE);
	}

	/**
	 * 頂点 {@code start} からの最短距離が {@code cutoff} 以下である頂点を、
	 * BFS で発見した順に返す。
	 *
	 * <p>計算量: 距離 {@code cutoff} 以内の頂点数を V、列挙した辺数を M として
	 * 期待 O(V + M)、空間 O(V)。</p>
	 *
	 * @param start 始点
	 * @param cutoff 距離の上限
	 * @return 条件を満たす頂点の BFS 発見順
	 */
	// 未テスト
	public List<V> bfsOrder(V start, int cutoff) {
		List<V> ret = new ArrayList<>();
		if (cutoff < 0) return ret;

		OpenHashMap<V, Integer> distances = new OpenHashMap<>(strategy);
		Queue<V> queue = new ArrayDeque<>();
		distances.put(start, 0);
		queue.add(start);
		while (!queue.isEmpty()) {
			V v = queue.poll();
			ret.add(v);
			int distance = distances.get(v);
			if (distance == cutoff) continue;
			for (V u : nextStates(v)) {
				if (!distances.containsKey(u)) {
					distances.put(u, distance + 1);
					queue.add(u);
				}
			}
		}
		return ret;
	}
}
