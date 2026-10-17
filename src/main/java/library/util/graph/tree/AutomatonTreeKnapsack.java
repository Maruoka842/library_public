package library.util.graph.tree;

import java.util.Arrays;
import java.util.Objects;

/**
 * 根付き {@link Tree} の各部分木に対し、有限状態オートマトンで制御される long 型ナップサック DP を計算する。
 *
 * <p>各頂点 {@code v} の各 pattern は、子 {@code tree.childs[v].get(i)} に渡す状態を
 * {@link Strategy#childState(int, int, int)} で定める。全ての使用する子を処理した配列を
 * {@link Strategy#finish(int, int, long[], long[][])} が頂点 {@code v} の状態別結果へ反映する。
 * 値の意味、添字の意味および合成則は strategy が定める。</p>
 *
 * <p>{@code N = tree.N}, {@code S = states}, {@code C = capacity + 1} とし、全頂点で
 * {@code numPatterns(v) <= δ} とする。{@code numPatterns}, {@code childState} が O(1)、
 * {@code finish} が O(C) のとき、{@link #solveAllSubtrees()} の時間計算量は次の通り。</p>
 *
 * <pre>
 * δ = 1: O((S + 1) C N log N)
 * δ > 1: O((S + δ) C N^(log_2(1 + δ)))
 * </pre>
 *
 * <p>特に {@code S, δ = O(1)} なら、それぞれ O(C N log N) および
 * O(C N^(log_2(1 + δ))) である。返り値の空間計算量は O(N S C)、
 * 再帰スタックは O(N)。</p>
 */
public class AutomatonTreeKnapsack {
	/** 子部分木を使用しないことを表す状態番号。 */
	public static final int NONE = -1;

	/** max-plus DP で到達不能を表す既定値。 */
	public static final long DEFAULT_UNREACHABLE = Long.MIN_VALUE / 4;

	/** 頂点ごとの遷移を定める。 */
	public interface Strategy {
		/**
		 * 頂点 {@code v} で列挙する pattern の個数を返す。
		 * O(1)
		 *
		 * @param v 頂点
		 * @return pattern の個数。0 以上
		 */
		// 未テスト
		int numPatterns(int v);

		/**
		 * v の pattern において子へ渡す状態を返す。
		 * 返り値が {@link AutomatonTreeKnapsack#NONE} のとき、その子は合成しない。
		 * O(1)
		 *
		 * @param v 親頂点
		 * @param pattern {@code 0 <= pattern < numPatterns(v)} を満たす pattern 番号
		 * @param child 頂点 {@code v} の子
		 * @return {@code NONE} または {@code 0 <= q < states} を満たす状態 q
		 */
		// 未テスト
		int childState(int v, int pattern, int child);

		/**
		 * 1 つの {@code pattern} について子部分木を全て処理した結果に、
		 * 頂点 {@code v} 自身の遷移を適用し、{@code result} へ併合する。
		 *
		 * <p>{@code accumulated[k]} は、頂点 {@code v} の部分木の計算開始時に与えられた
		 * 初期 DP から始め、この {@code pattern} について各子 {@code child} を
		 * {@code childState(v, pattern, child)} の状態で順に処理した後の、
		 * ナップサック添字 {@code k} の値である。
		 * {@code childState(...)} が {@link AutomatonTreeKnapsack#NONE} の子は処理されない。
		 * この時点では頂点 {@code v} 自身はまだ処理されていない。
		 * {@code accumulated} を変更してはならない。</p>
		 *
		 * <p>{@code result[q][k]} は、頂点 {@code v} 自身まで処理した後の
		 * 状態 {@code q}、ナップサック添字 {@code k} の値を格納する。
		 * {@code result} の大きさは {@code states × (capacity + 1)}。
		 * 最初の pattern を処理する前は全要素が
		 * {@link AutomatonTreeKnapsack#DEFAULT_UNREACHABLE} である。
		 * 各 pattern に対して同じ {@code result} が渡されるため、
		 * このメソッドは既に別の pattern から格納された値を考慮して結果を併合しなければならない。</p>
		 *
		 * @param v 頂点
		 * @param pattern pattern 番号
		 * @param accumulated この pattern に従って子部分木のみを処理した、
		 *                    長さ {@code capacity + 1} の DP 配列
		 * @param result 頂点 {@code v} 自身まで処理した結果を蓄積する
		 *               {@code states × (capacity + 1)} の DP 配列
		 */
		void finish(int v, int pattern, long[] accumulated, long[][] result);
	}

	/** 計算対象の根付き木。 */
	private final Tree tree;

	/** 計算対象の木の重軽分解。 */
	private final HLDecomposition hld;

	/** 状態数。 */
	private final int states;

	/** ナップサック容量。 */
	private final int capacity;

	/** 頂点遷移。 */
	private final Strategy strategy;

	/**
	 * 根付き木 {@code tree} に対する solver を構築する。
	 * 到達不能値には {@link #DEFAULT_UNREACHABLE} を用いる。
	 * O(N)
	 *
	 * @param tree 根付き木。solver の使用中に変更または根の変更をしてはならない
	 * @param states 状態数
	 * @param capacity ナップサック添字の最大値
	 * @param strategy 頂点遷移
	 * @throws IllegalArgumentException 頂点集合が空、または引数の範囲が不正な場合
	 * @throws IllegalStateException tree が根付きでない場合
	 * @throws NullPointerException tree または strategy が null の場合
	 */
	// 未テスト
	public AutomatonTreeKnapsack(Tree tree, int states, int capacity, Strategy strategy) {
		Objects.requireNonNull(tree, "tree");
		Objects.requireNonNull(strategy, "strategy");
		if (tree.N == 0) throw new IllegalArgumentException("tree must be nonempty");
		if (!tree.isRooted()) throw new IllegalStateException("tree must be rooted");
		if (states <= 0) throw new IllegalArgumentException("states must be positive: " + states);
		if (capacity < 0) throw new IllegalArgumentException("capacity must be nonnegative: " + capacity);

		this.tree = tree;
		this.hld = new HLDecomposition(tree);
		this.states = states;
		this.capacity = capacity;
		this.strategy = strategy;
	}

	/**
	 * 各頂点 {@code v} について、単位 DP から始めた {@code v} の部分木 DP を返す。
	 * 返り値を {@code dp} とすると、{@code dp[v][q][k]} は頂点 {@code v} を根とする部分木について、
	 * 状態 {@code q}、ナップサック添字 {@code k} の値である。
	 *
	 * <p>計算量はクラスの説明を参照。</p>
	 *
	 * @return 全頂点の状態別 DP。各行は互いに参照を共有しない
	 */
	// 未テスト
	public long[][][] solveAllSubtrees() {
		long[] base = new long[capacity + 1];
		Arrays.fill(base, DEFAULT_UNREACHABLE);
		base[0] = 0;
		long[][][] dp = new long[tree.N][states][capacity + 1];
		for (int v : tree.preOrder())
			if (hld.head[v] == v) dfs(v, base, dp);
		return dp;
	}

	/**
	 * 頂点 v の部分木を計算する。
	 * 計算量は呼び出される strategy メソッドと生成される作業配列に依存する。
	 *
	 * @param v 部分木の根
	 * @param base 初期 DP
	 * @param dp 全頂点の結果格納先
	 * @return 状態別 DP
	 */
	// 未テスト
	private long[][] dfs(int v, long[] base, long[][][] dp) {
		int heavyChild = hld.heavy[v];
		long[][] heavyResult = heavyChild < 0 ? null : dfs(heavyChild, base, dp);

		long[][] result = dp[v];
		for (long[] row : result) Arrays.fill(row, DEFAULT_UNREACHABLE);

		int patterns = strategy.numPatterns(v);
		for (int pattern = 0; pattern < patterns; pattern++) {
			long[] current = base;

			if (heavyChild >= 0) {
				int q = strategy.childState(v, pattern, heavyChild);
				if (q != NONE) current = heavyResult[q];
			}

			for (int i = 0; i < tree.childs[v].size(); i++) {
				int child = tree.childs[v].get(i);
				if (child == heavyChild) continue;
				int q = strategy.childState(v, pattern, child);
				if (q == NONE) continue;

				long[][] subResult = dfs(child, current, dp);
				current = subResult[q];
			}

			strategy.finish(v, pattern, current, result);
		}

		return result;
	}
}
