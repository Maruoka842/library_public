package library.util.segtree;

/**
 * 長さ N の列 A = (a_0, a_1, ..., a_(N-1)) に対して、固定長 K の各連続部分列を
 * 関数 phi(a_i, ..., a_(i+K-1)) で評価し、それらをモノイド演算 op で集約した値
 *   F(A) = op_(i=0)^(N-K) phi(a_i, ..., a_(i+K-1))
 * を動的に点更新しながら管理・取得するためのセグメント木。
 * 各ノードは、区間内の先頭および末尾の高々 K-1 要素と、区間に完全に含まれる窓の評価値の集約を保持する。
 *
 * 計算量:
 * 構築: O(N * K + N * T_phi)
 * 点更新: O(K * (K + T_phi) * log N)
 * ここで T_phi は関数 phi の評価に要する時間（通常 O(K)）。
 *
 * @param <E> 列の要素の型
 * @param <R> 評価結果・集約値の型
 */
public class WindowEvaluationSegTree<E, R> extends SegTree<WindowEvaluationState<E, R>> {
	private final WindowEvaluationStrategy<E, R> strategy;

	/**
	 * 指定された要素数 N および窓評価戦略 strategy に基づいて、空のセグメント木を構築する。
	 *
	 * 計算量: O(N)
	 *
	 * @param N 要素数
	 * @param strategy 窓評価戦略
	 */
	// 未テスト
	public WindowEvaluationSegTree(int N, WindowEvaluationStrategy<E, R> strategy) {
		super(N, (L, R) -> mergeStates(L, R, strategy), null);
		this.strategy = strategy;
	}

	/**
	 * 初期配列 initialArray および窓評価戦略 strategy に基づいてセグメント木を構築する。
	 *
	 * 計算量: O(N * K + N * T_phi)
	 *
	 * @param initialArray 初期配列
	 * @param strategy 窓評価戦略
	 */
	// 未テスト
	public WindowEvaluationSegTree(E[] initialArray, WindowEvaluationStrategy<E, R> strategy) {
		super(initialArray.length, (L, R) -> mergeStates(L, R, strategy), null);
		this.strategy = strategy;
		@SuppressWarnings("unchecked")
		WindowEvaluationState<E, R>[] leaves = (WindowEvaluationState<E, R>[]) new WindowEvaluationState[initialArray.length];
		for (int i = 0; i < initialArray.length; i++) {
			leaves[i] = createSingleState(initialArray[i], strategy);
		}
		this.build(leaves);
	}

	/**
	 * インデックス k の位置の要素を value (個数 1) に更新し、セグメント木をボトムアップに更新する。
	 *
	 * 事前条件: 0 <= k < N
	 * 計算量: O(K * (K + T_phi) * log N)
	 *
	 * @param k 更新対象のインデックス
	 * @param value 新しい値
	 */
	// 未テスト
	public void setElement(int k, E value) {
		setElement(k, value, 1);
	}

	/**
	 * インデックス k の位置の要素を、value を count 個並べた複数要素ブロックに更新し、セグメント木をボトムアップに更新する。
	 *
	 * 事前条件: 0 <= k < N, count >= 0
	 * 計算量: O((K * (K + T_phi) + log(count)) * log N)
	 *
	 * @param k 更新対象のインデックス
	 * @param value 新しい値
	 * @param count 要素の個数
	 */
	// 未テスト
	public void setElement(int k, E value, int count) {
		super.set(k, createState(value, count, strategy));
	}

	/**
	 * インデックス k の位置の要素を空（要素数 0）に更新し、セグメント木をボトムアップに更新する。
	 *
	 * 事前条件: 0 <= k < N
	 * 計算量: O(K * (K + T_phi) * log N)
	 *
	 * @param k 更新対象のインデックス
	 */
	// 未テスト
	public void setEmpty(int k) {
		super.set(k, null);
	}

	/**
	 * 指定されたノード内の境界要素（プレフィックスまたはサフィックス）から、インデックス idx に対応する要素を抽出する。
	 * node.len が K 未満かつ prefix と suffix の有効範囲が重なる場合、preferSuffix に従ってサフィックス側を優先して選択する。
	 *
	 * 事前条件: 0 <= idx < node.len
	 * 計算量: O(1)
	 *
	 * @param node 取得元のノード状態
	 * @param idx 取得対象のインデックス
	 * @param preferSuffix インデックス重複時にサフィックス側を優先するかどうか
	 * @return 対応する要素
	 */
	// 未テスト
	private static <E> E getElement(WindowEvaluationState<E, ?> node, int idx, boolean preferSuffix) {
		if (preferSuffix) {
			int suffixStart = node.len - node.suffix.length;
			if (idx >= suffixStart) {
				return node.suffix[idx - suffixStart];
			}
			if (idx < node.prefix.length) {
				return node.prefix[idx];
			}
		} else {
			if (idx < node.prefix.length) {
				return node.prefix[idx];
			}
			int suffixStart = node.len - node.suffix.length;
			if (idx >= suffixStart) {
				return node.suffix[idx - suffixStart];
			}
		}
		throw new AssertionError("Index " + idx + " out of accessible bounds in node of len " + node.len);
	}

	/**
	 * 単一の要素 value から、長さ 1 の葉ノードに対応するセグメント木の状態を生成する。
	 *
	 * 計算量: O(K)
	 *
	 * @param <E> 要素の型
	 * @param <R> 集約値の型
	 * @param value 葉要素の値
	 * @param strategy 窓評価戦略
	 * @return 葉ノードの状態表現
	 */
	// 未テスト
	public static <E, R> WindowEvaluationState<E, R> createSingleState(E value, WindowEvaluationStrategy<E, R> strategy) {
		return createState(value, 1, strategy);
	}

	/**
	 * 指定された要素 value を count 個並べた葉ノードに対応するセグメント木の状態を生成する。
	 * count が 0 の場合、空を意味する null を返す。
	 *
	 * 計算量: O(K + log(count))
	 *
	 * @param <E> 要素の型
	 * @param <R> 集約値の型
	 * @param value 葉要素の値
	 * @param count 要素の個数
	 * @param strategy 窓評価戦略
	 * @return 葉ノードの状態表現（count が 0 の場合は null）
	 */
	// 未テスト
	public static <E, R> WindowEvaluationState<E, R> createState(E value, int count, WindowEvaluationStrategy<E, R> strategy) {
		if (count < 0) {
			throw new IllegalArgumentException("count must be non-negative");
		}
		if (count == 0) {
			return null;
		}
		int K = strategy.K();
		if (K <= 0) {
			throw new IllegalArgumentException("K must be positive");
		}
		Class<E> clazz = strategy.clazz();

		int prefixLen = Math.min(count, K - 1);
		@SuppressWarnings("unchecked")
		E[] prefix = (E[]) java.lang.reflect.Array.newInstance(clazz, prefixLen);
		java.util.Arrays.fill(prefix, value);

		int suffixLen = Math.min(count, K - 1);
		@SuppressWarnings("unchecked")
		E[] suffix = (E[]) java.lang.reflect.Array.newInstance(clazz, suffixLen);
		java.util.Arrays.fill(suffix, value);

		int len = Math.min(count, K);

		R agg = strategy.identity();
		if (count >= K) {
			@SuppressWarnings("unchecked")
			E[] window = (E[]) java.lang.reflect.Array.newInstance(clazz, K);
			java.util.Arrays.fill(window, value);
			R val = strategy.phi(window);
			agg = power(val, count - K + 1, strategy);
		}

		return new WindowEvaluationState<>(prefix, suffix, len, agg);
	}

	/**
	 * 二項演算 op に基づき、値 val を power 回累積した結果を二分累乗法により計算する。
	 *
	 * 計算量: O(log(power))
	 *
	 * @param <E> 要素の型
	 * @param <R> 集約値の型
	 * @param val 累積する元データ
	 * @param power 累積回数
	 * @param strategy 窓評価戦略
	 * @return 累積結果
	 */
	// 未テスト
	private static <E, R> R power(R val, int power, WindowEvaluationStrategy<E, R> strategy) {
		if (power <= 0) {
			return strategy.identity();
		}
		R res = null;
		R base = val;
		int p = power;
		while (p > 0) {
			if ((p & 1) != 0) {
				if (res == null) {
					res = base;
				} else {
					res = strategy.op(res, base);
				}
			}
			base = strategy.op(base, base);
			p >>= 1;
		}
		return res == null ? strategy.identity() : res;
	}

	/**
	 * 隣接する 2 つのセグメント木状態 L および R をマージし、新しい統合された状態を返す。
	 * L.suffix と R.prefix を結合した上で、境界をまたぐ長さ K の全窓を phi で順次評価し、op で集約する。
	 *
	 * 計算量: O(K * (K + T_phi))
	 *
	 * @param <E> 要素の型
	 * @param <R> 集約値の型
	 * @param L 左側の子ノード状態
	 * @param R 右側の子ノード状態
	 * @param strategy 窓評価戦略
	 * @return 統合された新しい親ノード状態
	 */
	// 未テスト
	private static <E, R> WindowEvaluationState<E, R> mergeStates(WindowEvaluationState<E, R> L, WindowEvaluationState<E, R> R, WindowEvaluationStrategy<E, R> strategy) {
		if (L == null) return R;
		if (R == null) return L;

		final int K = strategy.K();
		final R identityR = strategy.identity();
		final Class<E> clazz = strategy.clazz();

		int newLen = Math.min(L.len + R.len, K);

		// prefix directly from L & R
		int prefixLen = Math.min(newLen, K - 1);
		@SuppressWarnings("unchecked")
		E[] prefix = (E[]) java.lang.reflect.Array.newInstance(clazz, prefixLen);
		for (int i = 0; i < prefixLen; i++) {
			if (i < L.len) {
				prefix[i] = getElement(L, i, false);
			} else {
				prefix[i] = getElement(R, i - L.len, false);
			}
		}

		// suffix directly from L & R
		int suffixLen = Math.min(newLen, K - 1);
		@SuppressWarnings("unchecked")
		E[] suffix = (E[]) java.lang.reflect.Array.newInstance(clazz, suffixLen);
		for (int i = 0; i < suffixLen; i++) {
			int globalIdx = L.len + R.len - suffixLen + i;
			if (globalIdx < L.len) {
				suffix[i] = getElement(L, globalIdx, true);
			} else {
				suffix[i] = getElement(R, globalIdx - L.len, true);
			}
		}

		// Boundary evaluation B = L.suffix + R.prefix
		int dMin = Math.max(1, K - R.len);
		int dMax = Math.min(K - 1, L.len);
		int crossingCount = Math.max(0, dMax - dMin + 1);
		R crossingAgg = null;
		if (crossingCount > 0) {
			@SuppressWarnings("unchecked")
			E[] B = (E[]) java.lang.reflect.Array.newInstance(clazz, L.suffix.length + R.prefix.length);
			System.arraycopy(L.suffix, 0, B, 0, L.suffix.length);
			System.arraycopy(R.prefix, 0, B, L.suffix.length, R.prefix.length);

			// For each crossing window: it has length K, starts at L.suffix.length - d
			boolean first = true;
			for (int d = dMax; d >= dMin; d--) {
				int start = L.suffix.length - d;
				@SuppressWarnings("unchecked")
				E[] window = (E[]) java.lang.reflect.Array.newInstance(clazz, K);
				System.arraycopy(B, start, window, 0, K);
				R val = strategy.phi(window);
				if (first) {
					crossingAgg = val;
					first = false;
				} else {
					crossingAgg = strategy.op(crossingAgg, val);
				}
			}
		}

		R newAgg = L.agg;
		if (crossingAgg != null) {
			newAgg = java.util.Objects.equals(newAgg, identityR) ? crossingAgg : strategy.op(newAgg, crossingAgg);
		}
		if (!java.util.Objects.equals(R.agg, identityR)) {
			newAgg = java.util.Objects.equals(newAgg, identityR) ? R.agg : strategy.op(newAgg, R.agg);
		}

		return new WindowEvaluationState<>(prefix, suffix, newLen, newAgg);
	}
}
