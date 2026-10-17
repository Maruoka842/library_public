package library.util.mo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import library.util.graph.tree.Tree;

/**
 * 静的な木の単純パスに対するオフラインクエリを Mo 法で処理する。
 * クエリ (u, v) の対象は、端点を含む単純パス P(u, v) の頂点集合である。
 * 構築後、全クエリの処理が終わるまで tree の辺および根を変更してはならない。
 */
public final class TreeMo {
	/** クエリ対象の根付き木。 */
	private final Tree tree;
	/** 各頂点が最初に Euler tour に現れる位置。 */
	private final int[] enter;
	/** 各頂点が二度目に Euler tour に現れる位置。 */
	private final int[] leave;
	/** 各頂点を入時と出時に一度ずつ記録した長さ 2N の Euler tour。 */
	private final int[] euler;
	/** 追加順に保持したパスクエリ。 */
	private final ArrayList<Query> queries = new ArrayList<>();

	/**
	 * Euler tour 区間 [l, r) と、区間外から補う頂点 extra を表す。
	 * extra = -1 の場合は補う頂点が存在しない。
	 */
	private record Query(int l, int r, int extra, int id) {}

	/**
	 * 木 tree に対する空のクエリ列を構築する。
	 * tree が根を持たない場合は頂点 0 を根として tree を根付ける。
	 * 計算量: O(N)。
	 *
	 * @param tree 頂点集合 {0, ..., N - 1} を持つ N >= 1 の木
	 */
	// 未テスト
	public TreeMo(Tree tree) {
		if (tree.N == 0) throw new IllegalArgumentException("tree must be nonempty");
		this.tree = tree;
		if (!tree.isRooted()) tree.rooted(0);
		enter = new int[tree.N];
		leave = new int[tree.N];
		euler = new int[2 * tree.N];
		buildEulerTour();
	}

	/**
	 * 端点を含む単純パス P(u, v) に対するクエリを末尾へ追加する。
	 * 返り値は追加されたクエリの 0-indexed ID である。
	 * 計算量: 最初の追加は O(N log N)、以降は O(log N)。
	 *
	 * @param u パスの一方の端点
	 * @param v パスのもう一方の端点
	 * @return 追加されたクエリの ID
	 * @throws IndexOutOfBoundsException u または v が [0, N) に含まれない場合
	 */
	// 未テスト
	public int addQuery(int u, int v) {
		if (u < 0 || u >= tree.N || v < 0 || v >= tree.N) {
			throw new IndexOutOfBoundsException("vertices must be in [0, " + tree.N + ")");
		}
		if (enter[u] > enter[v]) {
			int tmp = u;
			u = v;
			v = tmp;
		}
		int lca = tree.lca(u, v);
		int id = queries.size();
		if (lca == u) {
			queries.add(new Query(enter[u], enter[v] + 1, -1, id));
		} else {
			queries.add(new Query(leave[u], enter[v] + 1, lca, id));
		}
		return id;
	}

	/**
	 * 単純パス P(u, v) 上の辺に対するクエリを末尾へ追加する。
	 * cursor に渡される頂点 x は辺 (parent(x), x) を表し、根は渡されない。
	 * 返り値は追加されたクエリの 0-indexed ID である。
	 * 計算量: 最初の追加は O(N log N)、以降は O(log N)。
	 *
	 * @param u パスの一方の端点
	 * @param v パスのもう一方の端点
	 * @return 追加されたクエリの ID
	 * @throws IndexOutOfBoundsException u または v が [0, N) に含まれない場合
	 */
	// 未テスト
	public int addEdgeQuery(int u, int v) {
		if (u < 0 || u >= tree.N || v < 0 || v >= tree.N) {
			throw new IndexOutOfBoundsException("vertices must be in [0, " + tree.N + ")");
		}
		if (enter[u] > enter[v]) {
			int tmp = u;
			u = v;
			v = tmp;
		}
		int lca = tree.lca(u, v);
		int id = queries.size();
		if (lca == u) {
			queries.add(new Query(enter[u] + 1, enter[v] + 1, -1, id));
		} else {
			queries.add(new Query(leave[u], enter[v] + 1, -1, id));
		}
		return id;
	}

	/**
	 * 登録済みの全クエリを削除する。
	 * 次に追加されるクエリの ID は 0 となる。
	 * 計算量: O(Q)。
	 */
	// 未テスト
	public void clear() {
		queries.clear();
	}

	/**
	 * 全クエリを処理し、ans[i] を ID i のクエリの値とするリスト ans を返す。
	 * 呼び出し時の cursor の頂点集合は空でなければならない。
	 * 終了時の cursor の頂点集合は未規定である。
	 * U を addVertex/removeVertex の最大計算量、G を getValue の計算量、Q をクエリ数とすると、
	 * 計算量: O((N sqrt(Q) + Q) U + QG + Q log Q)。
	 *
	 * @param <T> クエリの返り値の型
	 * @param cursor 空集合に対応する初期状態を持つカーソル
	 * @return クエリの追加順に並んだ結果
	 */
	// 未テスト
	public <T> List<T> run(TreeMoCursor<T> cursor) {
		int queryCount = queries.size();
		if (queryCount == 0) return Collections.emptyList();

		Query[] order = queries.toArray(Query[]::new);
		int blockSize = Math.max(1, (int) (euler.length / Math.sqrt(queryCount)));
		Arrays.sort(order, (a, b) -> {
			int aBlock = a.l / blockSize;
			int bBlock = b.l / blockSize;
			if (aBlock != bBlock) return Integer.compare(aBlock, bBlock);
			return (aBlock & 1) == 0
					? Integer.compare(a.r, b.r)
					: Integer.compare(b.r, a.r);
		});
		
		boolean[] active = new boolean[tree.N];
		Object[] answers = new Object[queryCount];
		int left = 0;
		int right = 0;
		//cursor が保持している頂点集合
		//= Euler tour の区間 [left, right) に「奇数回」出現する頂点集合
		for (Query query : order) {
			while (left > query.l) toggle(euler[--left], active, cursor);
			while (right < query.r) toggle(euler[right++], active, cursor);
			while (left < query.l) toggle(euler[left++], active, cursor);
			while (right > query.r) toggle(euler[--right], active, cursor);
			if (query.extra >= 0) cursor.addVertex(query.extra);
			answers[query.id] = cursor.getValue(query.id);
			if (query.extra >= 0) cursor.removeVertex(query.extra);
		}
		@SuppressWarnings("unchecked")
		List<T> result = (List<T>) (List<?>) Arrays.asList(answers);
		return result;
	}

	/**
	 * 各頂点 v を入時 enter[v] と出時 leave[v] に記録する。
	 * 計算量: O(N)。
	 */
	// 未テスト
	private void buildEulerTour() {
		int[] vertices = new int[tree.N];
		int[] nextChild = new int[tree.N];
		int top = 0;
		int pointer = 0;
		vertices[0] = tree.root();
		enter[vertices[0]] = pointer;
		euler[pointer++] = vertices[0];
		while (top >= 0) {
			int v = vertices[top];
			if (nextChild[top] < tree.childs[v].size()) {
				int child = tree.childs[v].get(nextChild[top]++);
				vertices[++top] = child;
				nextChild[top] = 0;
				enter[child] = pointer;
				euler[pointer++] = child;
			} else {
				leave[v] = pointer;
				euler[pointer++] = v;
				top--;
			}
		}
	}

	/**
	 * active[v] を反転し、cursor の頂点集合を一致させる。
	 * 計算量: addVertex/removeVertex の計算量。
	 *
	 * @param <T> クエリの返り値の型
	 * @param v 反転する頂点
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 更新対象のカーソル
	 */
	// 未テスト
	private static <T> void toggle(int v, boolean[] active, TreeMoCursor<T> cursor) {
		if (active[v]) {
			cursor.removeVertex(v);
		} else {
			cursor.addVertex(v);
		}
		active[v] = !active[v];
	}
}
