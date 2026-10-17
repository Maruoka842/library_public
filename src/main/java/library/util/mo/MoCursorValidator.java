package library.util.mo;

import java.util.Objects;

/**
 * Mo 法で用いる {@link MoCursor} の実装を検証するクラス。
 *
 * <p>
 * 区間領域 {@code 0 <= l <= r <= n} および
 * グリッド領域 {@code 0 <= l < n, 0 <= r < m} 上で、
 * カーソルの状態遷移が経路によらず整合していることを全探索で検証する。
 * </p>
 *
 * <p>
 * まず全ての辺について inverse を検証し、それらが全て正しいことを確認した後に
 * elementary square を検証する。
 * </p>
 */
public final class MoCursorValidator {
	private MoCursorValidator() {}

	/**
	 * 区間領域 {@code 0 <= l <= r <= n} 上で
	 * {@link MoCursor} の状態遷移を検証する。
	 *
	 * <p>
	 * 以下をこの順に検証する。
	 * </p>
	 *
	 * <ol>
	 * <li>全ての合法な1ステップについて、逆向きに戻ると元の値に戻ること</li>
	 * <li>全ての elementary square について、対角点への2通りの経路が同じ値を与えること</li>
	 * </ol>
	 *
	 * <p>
	 * 検証終了後、カーソルは {@code (0, 0)} に戻る。
	 * </p>
	 *
	 * @param c 検証するカーソル
	 * @param n 座標の上限
	 * @param queryId getValue の検証に使用するクエリ ID
	 * @throws IllegalArgumentException カーソルの初期位置が {@code (0, 0)} でない場合
	 * @throws AssertionError 状態遷移の整合性が満たされない場合
	 * @complexity O(n^2)
	 */
	// 未テスト
	public static void validate(MoCursor<?> c, int n, int queryId) {
		checkInitialPosition(c);

		// inverse
		for (int r = 0; r <= n; r++) {
			while (c.r() < r) c.incrementR();

			for (int l = 0; l <= r; l++) {
				while (c.l() < l) c.incrementL();

				if (l > 0) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.decrementL();
					c.incrementL();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"decrementL -> incrementL");
				}

				if (l < r) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.incrementL();
					c.decrementL();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"incrementL -> decrementL");
				}

				if (l < r) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.decrementR();
					c.incrementR();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"decrementR -> incrementR");
				}

				if (r < n) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.incrementR();
					c.decrementR();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"incrementR -> decrementR");
				}
			}

			while (c.l() > 0) c.decrementL();
		}
		while (c.r() > 0) c.decrementR();

		// elementary square
		for (int r = 0; r < n; r++) {
			while (c.r() < r) c.incrementR();

			for (int l = 0; l < r; l++) {
				while (c.l() < l) c.incrementL();
				checkSquare(c, l, r, queryId);
			}

			while (c.l() > 0) c.decrementL();
		}
		while (c.r() > 0) c.decrementR();
	}

	/**
	 * 長方形グリッド領域 {@code 0 <= l < n, 0 <= r < m} 上で
	 * {@link MoCursor} の状態遷移を検証する。
	 *
	 * <p>
	 * まず全ての合法な1ステップについて inverse を検証し、
	 * それらが全て正しいことを確認した後に、
	 * 全ての elementary square について2通りの経路を比較する。
	 * </p>
	 *
	 * <p>
	 * 検証終了後、カーソルは {@code (0, 0)} に戻る。
	 * </p>
	 *
	 * @param c 検証するカーソル
	 * @param n {@code l} 座標の取り得る値の個数
	 * @param m {@code r} 座標の取り得る値の個数
	 * @param queryId getValue の検証に使用するクエリ ID
	 * @throws IllegalArgumentException カーソルの初期位置が {@code (0, 0)} でない場合
	 * @throws AssertionError 状態遷移の整合性が満たされない場合
	 * @complexity O(n m)
	 */
	// 未テスト
	public static void validateGrid(MoCursor<?> c, int n, int m, int queryId) {
		checkInitialPosition(c);

		if (n <= 0 || m <= 0) return;

		// inverse
		for (int r = 0; r < m; r++) {
			while (c.r() < r) c.incrementR();

			for (int l = 0; l < n; l++) {
				while (c.l() < l) c.incrementL();

				if (l > 0) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.decrementL();
					c.incrementL();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"decrementL -> incrementL");
				}

				if (l < n - 1) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.incrementL();
					c.decrementL();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"incrementL -> decrementL");
				}

				if (r > 0) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.decrementR();
					c.incrementR();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"decrementR -> incrementR");
				}

				if (r < m - 1) {
					Object before = c.getValue(queryId);
					String beforeDump = c.dump(queryId);

					c.incrementR();
					c.decrementR();

					checkInverse(c, l, r, before, beforeDump, queryId,
							"incrementR -> decrementR");
				}
			}

			while (c.l() > 0) c.decrementL();
		}
		while (c.r() > 0) c.decrementR();

		// elementary square
		for (int r = 0; r < m - 1; r++) {
			while (c.r() < r) c.incrementR();

			for (int l = 0; l < n - 1; l++) {
				while (c.l() < l) c.incrementL();
				checkSquare(c, l, r, queryId);
			}

			while (c.l() > 0) c.decrementL();
		}
		while (c.r() > 0) c.decrementR();
	}

	private static void checkInitialPosition(MoCursor<?> c) {
		if (c.l() != 0 || c.r() != 0)
			throw new IllegalArgumentException(
					"cursor must start at (0, 0), but was (" + c.l() + ", " + c.r() + ")");
	}

	private static void checkInverse(
			MoCursor<?> c, int l, int r,
			Object before, String beforeDump, int queryId, String operations) {

		if (c.l() != l || c.r() != r || !Objects.deepEquals(before, c.getValue(queryId)))
			throw new AssertionError(
					"MoCursor inverse validation failed\n"
					+ "position   : (" + l + ", " + r + ")\n"
					+ "operations : " + operations + "\n"
					+ "before     : " + beforeDump + "\n"
					+ "after      : " + c.dump(queryId));
	}

	private static void checkSquare(MoCursor<?> c, int l, int r, int queryId) {
		String start = c.dump(queryId);

		// path A:
		// (l,r) -> (l,r+1) -> (l+1,r+1)
		c.incrementR();
		String a1 = c.dump(queryId);

		c.incrementL();
		Object a = c.getValue(queryId);
		String a2 = c.dump(queryId);

		// inverse は既に全て検証済み。
		c.decrementL();
		c.decrementR();

		// path B:
		// (l,r) -> (l+1,r) -> (l+1,r+1)
		c.incrementL();
		String b1 = c.dump(queryId);

		c.incrementR();
		Object b = c.getValue(queryId);
		String b2 = c.dump(queryId);

		if (!Objects.deepEquals(a, b))
			throw new AssertionError(
					"MoCursor elementary square validation failed\n"
					+ "square     : (" + l + ", " + r + ")"
					+ " -> (" + (l + 1) + ", " + (r + 1) + ")\n"
					+ "start      : " + start + "\n"
					+ "\n"
					+ "path A     : incrementR -> incrementL\n"
					+ "  step 1   : " + a1 + "\n"
					+ "  result   : " + a2 + "\n"
					+ "\n"
					+ "path B     : incrementL -> incrementR\n"
					+ "  step 1   : " + b1 + "\n"
					+ "  result   : " + b2);

		c.decrementR();
		c.decrementL();
	}
}
