package library.util;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class SternBrocotTreeNodeRandomTest {

	// ランダムテストの試行回数。
	private static final int CASES = 1_000_000;
	// 生成するノードの深さの上限。
	private static final int MAX_DEPTH = 80;

	/**
	 * JUnit テストランナーからランダムテストを実行する。
	 *
	 * <p>計算量: {@code O(CASES * MAX_DEPTH)} 時間、
	 * {@code O(MAX_DEPTH)} 補助領域。
	 */
	// 未テスト
	@Test
	public void testRandomSternBrocotTreeNode() {
		main(new String[0]);
	}

	/**
	 * 固定 seed で {@link SternBrocotTreeNode#lca(SternBrocotTreeNode)} を検査する。
	 *
	 * <p>計算量: {@code O(CASES * MAX_DEPTH)} 時間、
	 * {@code O(MAX_DEPTH)} 補助領域。
	 *
	 * @param args 使用しない
	 */
	// 未テスト
	public static void main(String[] args) {
		Random random = new Random(0x5EEDB0C07L);

		for (int tc = 0; tc < CASES; tc++) {
			boolean[] aPath = randomPath(random);
			boolean[] bPath = randomPath(random);
			SternBrocotTreeNode a = nodeOf(aPath);
			SternBrocotTreeNode b = nodeOf(bPath);
			String aBefore = a.toString();
			String bBefore = b.toString();
			SternBrocotTreeNode copy = a.copy();
			if (copy == a
					|| copy.depth() != a.depth()
					|| !copy.get().equals(a.get())
					|| !copy.toString().equals(a.get().toString())) {
				throw new AssertionError("invalid copy or toString: case=" + tc);
			}
			copy.goLeft();
			if (!a.toString().equals(aBefore) || copy.depth() != a.depth() + 1)
				throw new AssertionError("copy is not independent: case=" + tc);

			int common = 0;
			while (common < aPath.length
					&& common < bPath.length
					&& aPath[common] == bPath[common])
				common++;

			SternBrocotTreeNode expected = nodeOf(aPath, common);
			SternBrocotTreeNode actual = a.lca(b);
			if (actual.depth() != common
					|| !actual.get().equals(expected.get())
					|| !a.toString().equals(aBefore)
					|| !b.toString().equals(bBefore)) {
				throw new AssertionError(
						"case=" + tc
						+ ", expected=" + expected + " depth=" + common
						+ ", actual=" + actual + " depth=" + actual.depth()
						+ ", a=" + a + ", b=" + b);
			}
		}

		System.out.println("OK: " + CASES + " random cases");
	}

	/**
	 * 長さが {@code [0, MAX_DEPTH]} から一様に選ばれたランダムな移動列を返す。
	 *
	 * <p>計算量: 返り値の長さを {@code n} として {@code O(n)} 時間、
	 * {@code O(n)} 領域。
	 *
	 * @param random 乱数生成器
	 * @return {@code false} を左、{@code true} を右とする移動列
	 */
	// 未テスト
	private static boolean[] randomPath(Random random) {
		boolean[] path = new boolean[random.nextInt(MAX_DEPTH + 1)];
		for (int i = 0; i < path.length; i++)
			path[i] = random.nextBoolean();
		return path;
	}

	/**
	 * {@code path} 全体を根から辿ったノードを返す。
	 *
	 * <p>計算量: {@code O(path.length)} 時間、{@code O(1)} 補助領域。
	 *
	 * @param path {@code false} を左、{@code true} を右とする移動列
	 * @return 移動後のノード
	 */
	// 未テスト
	private static SternBrocotTreeNode nodeOf(boolean[] path) {
		return nodeOf(path, path.length);
	}

	/**
	 * {@code path[0:length]} を根から辿ったノードを返す。
	 *
	 * <p>事前条件: {@code 0 <= length <= path.length}。
	 *
	 * <p>計算量: {@code O(length)} 時間、{@code O(1)} 補助領域。
	 *
	 * @param path {@code false} を左、{@code true} を右とする移動列
	 * @param length 使用する接頭辞の長さ
	 * @return 移動後のノード
	 */
	// 未テスト
	private static SternBrocotTreeNode nodeOf(boolean[] path, int length) {
		SternBrocotTreeNode node = new SternBrocotTreeNode();
		for (int i = 0; i < length; i++) {
			if (path[i])
				node.goRight();
			else
				node.goLeft();
		}
		return node;
	}
}
