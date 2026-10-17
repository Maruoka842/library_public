package library.util.fold;

import java.util.Arrays;

/**
 * 2 つの静的配列 A, B に対し、任意の 2 区間の全要素対への kernel の総和を求める。
 *
 * <pre>
 * get(al, ar, bl, br)
 *   = sum_(al <= i < ar) sum_(bl <= j < br) K(A[i], B[j])
 * </pre>
 *
 * <p>
 * {@link Kernel} は、ソート済み配列 X, Y に対して
 *
 * <pre>
 * out[j] = sum_i K(X[i], Y[j])
 * </pre>
 *
 * を一括評価する。
 * </p>
 *
 * <p>
 * {@code blockSize = S} とし、Kernel.evaluate が
 * O(|X| + |Y|) で動作するとする。
 * A の長さを N、B の長さを M とすると、
 * </p>
 *
 * <ul>
 * <li>構築: O(NM / S + (N + M) log S)</li>
 * <li>クエリ: O(S)</li>
 * <li>空間: O(NM / S + N + M)</li>
 * </ul>
 *
 * <p>
 * 構築後に入力配列を変更しても、このオブジェクトには影響しない。
 * クエリ用の作業領域を内部で再利用するため thread-safe ではない。
 * </p>
 */
public class StaticCrossRangePairwiseSum {
	/**
	 * 2 項関数 K(x, y) の一括評価器。
	 */
	public interface Kernel {
		/**
		 * {@code left[0, leftLength)} と {@code right[0, rightLength)} は
		 * それぞれ昇順にソートされているものとする。
		 *
		 * <pre>
		 * out[j] = sum_(0 <= i < leftLength) K(left[i], right[j])
		 * </pre>
		 *
		 * を {@code 0 <= j < rightLength} について書き込む。
		 *
		 * <p>
		 * StaticCrossRangePairwiseSum の計算量保証を得るには
		 * O(leftLength + rightLength) で動作する必要がある。
		 * </p>
		 */
		void evaluate(
				long[] left, int leftLength,
				long[] right, int rightLength,
				long[] out);

		/**
		 * 転置 kernel
		 *
		 * <pre>
		 * K^T(x, y) = K(y, x)
		 * </pre>
		 *
		 * を返す。
		 *
		 * <p>対称 kernel なら {@code return this;} でよい。</p>
		 */
		Kernel transpose();
	}

	private final int n;
	private final int m;
	private final int bs;
	private final Kernel kernel;

	/**
	 * leftBoundary[b][j]
	 * = sum_(i < b * bs) sum_(q < j) K(A[i], B[q])
	 */
	private final long[][] leftBoundary;

	/**
	 * rightBoundary[b][i]
	 * = sum_(p < i) sum_(j < b * bs) K(A[p], B[j])
	 */
	private final long[][] rightBoundary;

	/**
	 * 各ブロックを値順に並べたもの。
	 * 末尾の不完全ブロックも含む。
	 */
	private final Block[] aBlocks;
	private final Block[] bBlocks;

	/** クエリ用作業領域。 */
	private final long[] leftScratch;
	private final long[] rightScratch;
	private final long[] outScratch;

	public StaticCrossRangePairwiseSum(long[] a, long[] b, Kernel kernel, int blockSize) {
		if (a == null || b == null || kernel == null) {
			throw new NullPointerException();
		}
		if (blockSize <= 0) {
			throw new IllegalArgumentException("blockSize must be positive");
		}

		this.n = a.length;
		this.m = b.length;
		this.bs = blockSize;
		this.kernel = kernel;

		this.aBlocks = buildBlocks(a, bs);
		this.bBlocks = buildBlocks(b, bs);

		int fullA = n / bs;
		int fullB = m / bs;

		this.leftBoundary = new long[fullA + 1][m + 1];
		this.rightBoundary = new long[fullB + 1][n + 1];

		long[] out = new long[Math.max(n, m)];

		/*
		 * leftBoundary[ib][j]
		 * = sum K(A[i], B[q])
		 *   (i < ib * bs, q < j)
		 */
		for (int ib = 0; ib < fullA; ib++) {
		    Block blockA = aBlocks[ib];
		    long[] prev = leftBoundary[ib];
		    long[] next = leftBoundary[ib + 1];

		    for (int jb = 0; jb < bBlocks.length; jb++) {
		        Block blockB = bBlocks[jb];

		        kernel.evaluate(
		                blockA.values, blockA.values.length,
		                blockB.values, blockB.values.length,
		                out);

		        int base = jb * bs;
		        for (int p = 0; p < blockB.values.length; p++) {
		            next[base + blockB.positions[p] + 1] = out[p];
		        }
		    }

		    long run = 0;
		    for (int j = 0; j < m; j++) {
		        run += next[j + 1];
		        next[j + 1] = prev[j + 1] + run;
		    }
		}

		/*
		 * rightBoundary[ib][i]
		 * = sum K(A[p], B[j])
		 *   (p < i, j < ib * bs)
		 *
		 * B 側を block として評価するため転置 kernel を使う。
		 */
		Kernel transposed = kernel.transpose();
		if (transposed == null) {
			throw new NullPointerException("kernel.transpose() returned null");
		}

		for (int ib = 0; ib < fullB; ib++) {
			Block blockB = bBlocks[ib];

			long[] prev = rightBoundary[ib];
			long[] next = rightBoundary[ib + 1];

			for (int ia = 0; ia < aBlocks.length; ia++) {
				Block blockA = aBlocks[ia];

				transposed.evaluate(
						blockB.values, blockB.values.length,
						blockA.values, blockA.values.length,
						out);

				int base = ia * bs;
				for (int p = 0; p < blockA.values.length; p++) {
					next[base + blockA.positions[p] + 1] = out[p];
				}
			}

			long run = 0;
			for (int i = 0; i < n; i++) {
				run += next[i + 1];
				next[i + 1] = prev[i + 1] + run;
			}
		}
		
		this.leftScratch = new long[bs];
		this.rightScratch = new long[bs];
		this.outScratch = new long[bs];
	}

	/**
	 * <pre>
	 * sum_(0 <= i < aTo) sum_(0 <= j < bTo) K(A[i], B[j])
	 * </pre>
	 *
	 * を返す。
	 *
	 * <p>計算量: O(blockSize)</p>
	 */
	public long prefix(int aTo, int bTo) {
		if (aTo < 0 || aTo > n || bTo < 0 || bTo > m) {
			throw new IndexOutOfBoundsException();
		}
		if (aTo == 0 || bTo == 0) return 0;

		int ab = aTo / bs;
		int bb = bTo / bs;

		int aBase = ab * bs;
		int bBase = bb * bs;

		long ret = leftBoundary[ab][bTo] + rightBoundary[bb][aTo] - leftBoundary[ab][bBase];

		int aRem = aTo - aBase;
		int bRem = bTo - bBase;

		if (aRem == 0 || bRem == 0) return ret;

		extractPrefix(aBlocks[ab], aRem, leftScratch);
		extractPrefix(bBlocks[bb], bRem, rightScratch);

		kernel.evaluate(
				leftScratch, aRem,
				rightScratch, bRem,
				outScratch);

		for (int j = 0; j < bRem; j++) {
			ret += outScratch[j];
		}

		return ret;
	}

	/**
	 * <pre>
	 * sum_(al <= i < ar) sum_(bl <= j < br) K(A[i], B[j])
	 * </pre>
	 *
	 * を返す。
	 *
	 * <p>計算量: O(blockSize)</p>
	 *
	 * @param al A 側区間の下端 inclusive
	 * @param ar A 側区間の上端 exclusive
	 * @param bl B 側区間の下端 inclusive
	 * @param br B 側区間の上端 exclusive
	 */
	public long get(int al, int ar, int bl, int br) {
		if (al < 0 || al > ar || ar > n
				|| bl < 0 || bl > br || br > m) {
			throw new IndexOutOfBoundsException();
		}

		return prefix(ar, br)
				- prefix(al, br)
				- prefix(ar, bl)
				+ prefix(al, bl);
	}

	public int blockSize() {
		return bs;
	}

	/**
	 * 想定クエリ数 K = expectedQueries に対し、コスト関数 T(S) = 2 * N * M / S + K * S
	 * を最小化するブロックサイズ S でインスタンスを構築する。
	 *
	 * <p>計算量: O(N * M / S + (N + M) log S)</p>
	 *
	 * // 未テスト
	 *
	 * @param a 配列 A
	 * @param b 配列 B
	 * @param kernel カーネル
	 * @param expectedQueries 想定クエリ数 K (> 0)
	 * @return StaticCrossRangePairwiseSum インスタンス
	 */
	public static StaticCrossRangePairwiseSum withExpectedQueries(long[] a, long[] b, Kernel kernel, int expectedQueries) {
		if (a == null || b == null || kernel == null) {
			throw new NullPointerException();
		}
		if (expectedQueries <= 0) {
			throw new IllegalArgumentException("expectedQueries must be positive");
		}
		int blockSize = optimalBlockSize(a.length, b.length, expectedQueries);
		return new StaticCrossRangePairwiseSum(a, b, kernel, blockSize);
	}

	private static int optimalBlockSize(int n, int m, double k) {
		if (n == 0 || m == 0) return 1;
		int maxDim = Math.max(n, m);
		double sContinuous = Math.sqrt(2.0 * n * m / k);
		int s1 = (int) Math.floor(sContinuous);
		s1 = Math.max(1, Math.min(s1, maxDim));
		int s2 = Math.min(s1 + 1, maxDim);

		double cost1 = 2.0 * n * m / s1 + k * s1;
		double cost2 = 2.0 * n * m / s2 + k * s2;

		return (cost1 <= cost2) ? s1 : s2;
	}

	private static int defaultBlockSize(long[] a, long[] b) {
		if (a == null || b == null) throw new NullPointerException();
		int z = Math.max(a.length, b.length);
		return Math.max(1, (int) Math.ceil(Math.sqrt(z)));
	}

	private static void extractPrefix(Block block, int prefixLength, long[] out) {
		int p = 0;
		for (int i = 0; i < block.values.length; i++) {
			if (block.positions[i] < prefixLength) {
				out[p++] = block.values[i];
			}
		}
	}

	private static Block[] buildBlocks(
			long[] a,
			int bs) {

		int nb = (a.length + bs - 1) / bs;
		Block[] ret = new Block[nb];

		for (int ib = 0; ib < nb; ib++) {
			int l = ib * bs;
			int r = Math.min(l + bs, a.length);
			long[] sub = Arrays.copyOfRange(a, l, r);

			int[] positions = library.util.ArrayUtils.argSort(sub);
			long[] values = library.util.ArrayUtils.take(sub, positions);

			ret[ib] = new Block(values, positions);
		}

		return ret;
	}

	private static final class Block {
		final long[] values;
		final int[] positions;

		Block(long[] values, int[] positions) {
			this.values = values;
			this.positions = positions;
		}
	}


}