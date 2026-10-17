package library.util.mo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import library.util.graph.tree.Tree;

/**
 * 木上の Mo 法で用いる {@link TreeMoCursor} の状態遷移を検証する。
 */
public final class TreeMoCursorValidator {
	/**
	 * インスタンス化を禁止する。
	 * 計算量: O(1)。
	 */
	// 未テスト
	private TreeMoCursorValidator() {}

	/**
	 * tree の Euler tour の全区間 [l, r) について、境界トグルの逆元性と
	 * (l, r) から (l + 1, r + 1) へ至る二経路の値の一致を検証する。
	 * また、全ての非祖先パスについて LCA の一時的な追加と削除が元の値を復元することを検証する。
	 * cursor は空集合に対応する状態でなければならず、検証終了後は空集合に対応する状態へ戻る。
	 * tree が根を持たない場合は頂点 0 を根として tree を根付ける。
	 * U を addVertex/removeVertex の最大計算量、G を getValue の計算量とすると、
	 * 計算量: O(N^2(log N + U + G))。
	 *
	 * @param tree 頂点集合 {0, ..., N - 1} を持つ N >= 1 の木
	 * @param cursor 検証する空状態のカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 * @throws IllegalArgumentException tree が空の場合
	 * @throws AssertionError 状態遷移の整合性が満たされない場合
	 */
	// 未テスト
	public static void validate(Tree tree, TreeMoCursor<?> cursor, int queryId) {
		if (tree.N == 0) throw new IllegalArgumentException("tree must be nonempty");
		if (!tree.isRooted()) tree.rooted(0);
		EulerTour tour = buildEulerTour(tree);
		boolean[] active = new boolean[tree.N];
		Object initialValue = cursor.getValue(queryId);

		validateInverses(tour.euler, active, cursor, queryId);
		validateSquares(tour.euler, active, cursor, queryId);
		validateExtraLcas(tree, tour, active, cursor, queryId);

		if (!Objects.deepEquals(initialValue, cursor.getValue(queryId))) {
			throw new AssertionError(
					"TreeMoCursor validation did not restore the initial value\n"
					+ "before : " + initialValue + "\n"
					+ "after  : " + cursor.getValue(queryId));
		}
	}

	/**
	 * Euler tour の全合法境界移動について、同じ境界を再度トグルすると値が元に戻ることを検証する。
	 * 計算量: O(N^2(U + G))。
	 *
	 * @param euler 長さ 2N の Euler tour
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 */
	// 未テスト
	private static void validateInverses(
			int[] euler, boolean[] active, TreeMoCursor<?> cursor, int queryId) {
		int left = 0;
		int right = 0;
		for (int targetRight = 0; targetRight <= euler.length; targetRight++) {
			while (right < targetRight) toggle(euler[right++], active, cursor);
			for (int targetLeft = 0; targetLeft <= targetRight; targetLeft++) {
				while (left < targetLeft) toggle(euler[left++], active, cursor);
				if (left > 0) checkInverse(euler[left - 1], left, right, active, cursor, queryId, "decrementL -> incrementL");
				if (left < right) checkInverse(euler[left], left, right, active, cursor, queryId, "incrementL -> decrementL");
				if (left < right) checkInverse(euler[right - 1], left, right, active, cursor, queryId, "decrementR -> incrementR");
				if (right < euler.length) checkInverse(euler[right], left, right, active, cursor, queryId, "incrementR -> decrementR");
			}
			while (left > 0) toggle(euler[--left], active, cursor);
		}
		while (right > 0) toggle(euler[--right], active, cursor);
	}

	/**
	 * Euler tour の全 elementary square について二経路の終点の値が一致することを検証する。
	 * 計算量: O(N^2(U + G))。
	 *
	 * @param euler 長さ 2N の Euler tour
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 */
	// 未テスト
	private static void validateSquares(
			int[] euler, boolean[] active, TreeMoCursor<?> cursor, int queryId) {
		int left = 0;
		int right = 0;
		for (int targetRight = 0; targetRight < euler.length; targetRight++) {
			while (right < targetRight) toggle(euler[right++], active, cursor);
			for (int targetLeft = 0; targetLeft < targetRight; targetLeft++) {
				while (left < targetLeft) toggle(euler[left++], active, cursor);
				checkSquare(euler, left, right, active, cursor, queryId);
			}
			while (left > 0) toggle(euler[--left], active, cursor);
		}
		while (right > 0) toggle(euler[--right], active, cursor);
	}

	/**
	 * 全頂点対のパス区間に移動し、区間外の LCA の追加と削除が値を復元することを検証する。
	 * 計算量: O(N^2(log N + U + G))。
	 *
	 * @param tree 検証対象の根付き木
	 * @param tour tree の Euler tour
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 */
	// 未テスト
	private static void validateExtraLcas(
			Tree tree, EulerTour tour, boolean[] active, TreeMoCursor<?> cursor, int queryId) {
		ArrayList<LcaQuery> queries = new ArrayList<>();
		for (int first = 0; first < tree.N; first++) {
			for (int second = 0; second < tree.N; second++) {
				int u = first;
				int v = second;
				if (tour.enter[u] > tour.enter[v]) {
					int tmp = u;
					u = v;
					v = tmp;
				}
				int lca = tree.lca(u, v);
				if (lca != u) {
					queries.add(new LcaQuery(tour.leave[u], tour.enter[v] + 1, lca, u, v));
				}
			}
		}
		LcaQuery[] order = queries.toArray(LcaQuery[]::new);
		int blockSize = Math.max(1, (int) (tour.euler.length / Math.sqrt(Math.max(1, order.length))));
		Arrays.sort(order, (a, b) -> {
			int aBlock = a.left / blockSize;
			int bBlock = b.left / blockSize;
			if (aBlock != bBlock) return Integer.compare(aBlock, bBlock);
			return (aBlock & 1) == 0
					? Integer.compare(a.right, b.right)
					: Integer.compare(b.right, a.right);
		});

		int left = 0;
		int right = 0;
		for (LcaQuery query : order) {
			while (left > query.left) toggle(tour.euler[--left], active, cursor);
			while (right < query.right) toggle(tour.euler[right++], active, cursor);
			while (left < query.left) toggle(tour.euler[left++], active, cursor);
			while (right > query.right) toggle(tour.euler[--right], active, cursor);
			checkExtraLca(query.lca, query.u, query.v, active, cursor, queryId);
		}
		while (left < right) toggle(tour.euler[left++], active, cursor);
	}

	/**
	 * 頂点 v のトグルを二度適用した前後で値が一致することを検証する。
	 * 計算量: O(U + G)。
	 *
	 * @param v トグルする頂点
	 * @param left 現在区間の左端
	 * @param right 現在区間の右端
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 * @param operations 対応する境界操作
	 */
	// 未テスト
	private static void checkInverse(
			int v, int left, int right, boolean[] active,
			TreeMoCursor<?> cursor, int queryId, String operations) {
		Object before = cursor.getValue(queryId);
		toggle(v, active, cursor);
		toggle(v, active, cursor);
		Object after = cursor.getValue(queryId);
		if (!Objects.deepEquals(before, after)) {
			throw new AssertionError(
					"TreeMoCursor inverse validation failed\n"
					+ "interval   : [" + left + ", " + right + ")\n"
					+ "vertex     : " + v + "\n"
					+ "operations : " + operations + "\n"
					+ "before     : " + before + "\n"
					+ "after      : " + after);
		}
	}

	/**
	 * 一つの elementary square の二経路で終点の値が一致することを検証する。
	 * 計算量: O(U + G)。
	 *
	 * @param euler 長さ 2N の Euler tour
	 * @param left 正方形の左座標
	 * @param right 正方形の右座標
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 */
	// 未テスト
	private static void checkSquare(
			int[] euler, int left, int right, boolean[] active,
			TreeMoCursor<?> cursor, int queryId) {
		Object start = cursor.getValue(queryId);
		toggle(euler[right], active, cursor);
		Object firstA = cursor.getValue(queryId);
		toggle(euler[left], active, cursor);
		Object resultA = cursor.getValue(queryId);
		toggle(euler[left], active, cursor);
		toggle(euler[right], active, cursor);

		toggle(euler[left], active, cursor);
		Object firstB = cursor.getValue(queryId);
		toggle(euler[right], active, cursor);
		Object resultB = cursor.getValue(queryId);
		if (!Objects.deepEquals(resultA, resultB)) {
			throw new AssertionError(
					"TreeMoCursor elementary square validation failed\n"
					+ "square   : (" + left + ", " + right + ") -> ("
					+ (left + 1) + ", " + (right + 1) + ")\n"
					+ "start    : " + start + "\n"
					+ "path A   : " + firstA + " -> " + resultA + "\n"
					+ "path B   : " + firstB + " -> " + resultB);
		}
		toggle(euler[right], active, cursor);
		toggle(euler[left], active, cursor);
	}

	/**
	 * 区間外の LCA を追加して削除した前後で値が一致することを検証する。
	 * 計算量: O(U + G)。
	 *
	 * @param lca 一時的に追加する LCA
	 * @param u 正規化後の一方の端点
	 * @param v 正規化後のもう一方の端点
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 検証するカーソル
	 * @param queryId getValue の検証に使用するクエリ ID
	 */
	// 未テスト
	private static void checkExtraLca(
			int lca, int u, int v, boolean[] active,
			TreeMoCursor<?> cursor, int queryId) {
		if (active[lca]) {
			throw new AssertionError("LCA must be outside the Euler interval: " + lca);
		}
		Object before = cursor.getValue(queryId);
		cursor.addVertex(lca);
		Object withLca = cursor.getValue(queryId);
		cursor.removeVertex(lca);
		Object after = cursor.getValue(queryId);
		if (!Objects.deepEquals(before, after)) {
			throw new AssertionError(
					"TreeMoCursor extra LCA validation failed\n"
					+ "path     : (" + u + ", " + v + ")\n"
					+ "lca      : " + lca + "\n"
					+ "before   : " + before + "\n"
					+ "with LCA : " + withLca + "\n"
					+ "after    : " + after);
		}
	}

	/**
	 * active[v] を反転し、cursor の頂点集合を一致させる。
	 * 計算量: addVertex/removeVertex の計算量。
	 *
	 * @param v 反転する頂点
	 * @param active 各頂点が現在の集合に含まれるかを表す配列
	 * @param cursor 更新対象のカーソル
	 */
	// 未テスト
	private static void toggle(int v, boolean[] active, TreeMoCursor<?> cursor) {
		if (active[v]) {
			cursor.removeVertex(v);
		} else {
			cursor.addVertex(v);
		}
		active[v] = !active[v];
	}

	/**
	 * 各頂点を入時と出時に一度ずつ記録した Euler tour を返す。
	 * 計算量: O(N)。
	 *
	 * @param tree 根付き木
	 * @return tree の Euler tour と各頂点の入出位置
	 */
	// 未テスト
	private static EulerTour buildEulerTour(Tree tree) {
		int[] enter = new int[tree.N];
		int[] leave = new int[tree.N];
		int[] euler = new int[2 * tree.N];
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
		return new EulerTour(enter, leave, euler);
	}

	/**
	 * 各頂点の入位置、出位置、および長さ 2N の Euler tour を保持する。
	 */
	private record EulerTour(int[] enter, int[] leave, int[] euler) {}

	/**
	 * 非祖先パスの Euler tour 区間 [left, right) と区間外の LCA を保持する。
	 */
	private record LcaQuery(int left, int right, int lca, int u, int v) {}
}
