package library.util.fold;

/**
 * 静的な int 配列に対する Range Minimum Query。
 *
 * <p>
 * 配列を 32 要素ずつのブロックに分割し、
 * 同一ブロック内の RMQ は monotone stack を bitmask として保持することで O(1) で処理する。
 * 複数の完全なブロックにまたがる部分の RMQ は、
 * 各ブロックの最小値に対する {@link SparseTableInt} に移譲する。
 * </p>
 *
 * <p>
 * 構築後に入力配列を変更してはならない。
 * </p>
 *
 * <ul>
 * <li>構築: O(N + (N / 32) log(N / 32))</li>
 * <li>{@link #fold(int, int)}: O(1)</li>
 * <li>空間: O(N + (N / 32) log(N / 32))</li>
 * </ul>
 */
public class StaticRangeMinimumInt {
	//https://judge.yosupo.jp/submission/396033
	private static final int SHIFT = 5;
	private static final int BLOCK_SIZE = 1 << SHIFT;
	private static final int BLOCK_MASK = BLOCK_SIZE - 1;

	private final int[] a;

	/**
	 * {@code mask[i]} は、{@code i} と同じブロック内において、
	 * 右端を {@code i} に固定した区間の最小値になり得る位置を表す bitmask。
	 *
	 * <p>
	 * ブロックの先頭を {@code base} とすると、
	 * bit {@code j} が立っている位置 {@code base + j} は、
	 * ある {@code base <= l <= i} に対して
	 * {@code min(a[l], ..., a[i])} を与える。
	 * </p>
	 */
	private final int[] mask;

	/**
	 * 各ブロックの最小値に対する RMQ。
	 */
	private final SparseTableInt blockRMQ;

	public StaticRangeMinimumInt(int[] a) {
		this.a = a;

		int n = a.length;
		mask = new int[n];

		int blockCount = (n + BLOCK_SIZE - 1) >>> SHIFT;

		if (blockCount == 0) {
			blockRMQ = null;
			return;
		}

		int[] blockMin = new int[blockCount];

		for (int b = 0; b < blockCount; b++) {
			int from = b << SHIFT;
			int to = Math.min(from + BLOCK_SIZE, n);

			int m = 0;

			for (int i = from; i < to; i++) {
				int x = a[i];
				while (m != 0) {
					int p = 31 - Integer.numberOfLeadingZeros(m);
					if (a[from + p] < x) {
						break;
					}
					m ^= 1 << p;
				}

				m |= 1 << (i - from);
				mask[i] = m;
			}

			int p = Integer.numberOfTrailingZeros(mask[to - 1]);
			blockMin[b] = a[from + p];
		}

		blockRMQ = new SparseTableInt(blockMin, Math::min);
	}

	/**
	 * {@code a[l:r)} の最小値を返す。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param l 区間の左端
	 * @param r 区間の右端
	 * @return {@code min(a[l], ..., a[r - 1])}
	 */
	public int fold(int l, int r) {
		int last = r - 1;

		int lb = l >>> SHIFT;
		int rb = last >>> SHIFT;

		if (lb == rb) {
			return foldInBlock(l, r);
		}

		int ans = foldInBlock(l, (lb + 1) << SHIFT);

		int x = foldInBlock(rb << SHIFT, r);
		if (x < ans) {
			ans = x;
		}

		if (lb + 1 < rb) {
			x = blockRMQ.fold(lb + 1, rb);
			if (x < ans) {
				ans = x;
			}
		}

		return ans;
	}

	/**
	 * 同一ブロック内の {@code a[l:r)} の最小値を返す。
	 */
	private int foldInBlock(int l, int r) {
		int last = r - 1;
		int blockStart = l & ~BLOCK_MASK;// l/32*32

		int m = mask[last] & (-1 << (l - blockStart));

		int p = Integer.numberOfTrailingZeros(m);
		return a[blockStart + p];
	}
}