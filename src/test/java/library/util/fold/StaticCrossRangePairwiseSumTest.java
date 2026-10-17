package library.util.fold;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class StaticCrossRangePairwiseSumTest {

	/**
	 * 積 K(x, y) = x * y を計算するカーネル
	 */
	static class ProductKernel implements StaticCrossRangePairwiseSum.Kernel {
		@Override
		public void evaluate(long[] left, int leftLength, long[] right, int rightLength, long[] out) {
			long sumLeft = 0;
			for (int i = 0; i < leftLength; i++) {
				sumLeft += left[i];
			}
			for (int j = 0; j < rightLength; j++) {
				out[j] = sumLeft * right[j];
			}
		}

		@Override
		public StaticCrossRangePairwiseSum.Kernel transpose() {
			return this;
		}
	}

	/**
	 * 差の絶対値 K(x, y) = |x - y| を計算するカーネル
	 */
	static class AbsDiffKernel implements StaticCrossRangePairwiseSum.Kernel {
		@Override
		public void evaluate(long[] left, int leftLength, long[] right, int rightLength, long[] out) {
			for (int j = 0; j < rightLength; j++) {
				long sum = 0;
				for (int i = 0; i < leftLength; i++) {
					sum += Math.abs(left[i] - right[j]);
				}
				out[j] = sum;
			}
		}

		@Override
		public StaticCrossRangePairwiseSum.Kernel transpose() {
			return this;
		}
	}

	@Test
	public void testProductKernel() {
		long[] a = {2, 5, 1, 4, 3};
		long[] b = {3, 1, 4, 2};

		StaticCrossRangePairwiseSum scps = new StaticCrossRangePairwiseSum(a, b, new ProductKernel(), 2);

		// naive computation verification
		for (int al = 0; al <= a.length; al++) {
			for (int ar = al; ar <= a.length; ar++) {
				for (int bl = 0; bl <= b.length; bl++) {
					for (int br = bl; br <= b.length; br++) {
						long expected = 0;
						for (int i = al; i < ar; i++) {
							for (int j = bl; j < br; j++) {
								expected += a[i] * b[j];
							}
						}
						long actual = scps.get(al, ar, bl, br);
						assertEquals(expected, actual, "Failed at al=" + al + ", ar=" + ar + ", bl=" + bl + ", br=" + br);
					}
				}
			}
		}
	}

	@Test
	public void testAbsDiffKernelRandom() {
		Random rnd = new Random(42);
		int n = 20;
		int m = 15;
		long[] a = new long[n];
		long[] b = new long[m];
		for (int i = 0; i < n; i++) a[i] = rnd.nextInt(100) - 50;
		for (int j = 0; j < m; j++) b[j] = rnd.nextInt(100) - 50;

		StaticCrossRangePairwiseSum scps = new StaticCrossRangePairwiseSum(a, b, new AbsDiffKernel(), 3);

		for (int iter = 0; iter < 100; iter++) {
			int al = rnd.nextInt(n + 1);
			int ar = rnd.nextInt(n + 1 - al) + al;
			int bl = rnd.nextInt(m + 1);
			int br = rnd.nextInt(m + 1 - bl) + bl;

			long expected = 0;
			for (int i = al; i < ar; i++) {
				for (int j = bl; j < br; j++) {
					expected += Math.abs(a[i] - b[j]);
				}
			}
			long actual = scps.get(al, ar, bl, br);
			assertEquals(expected, actual);
		}
	}

	@Test
	public void testWithExpectedQueriesBlockSize() {
		long[] a = new long[100];
		long[] b = new long[100];
		ProductKernel kernel = new ProductKernel();

		// N=100, M=100
		// sContinuous = sqrt(2 * 100 * 100 / K)
		// For K=2: sContinuous = sqrt(10000) = 100
		StaticCrossRangePairwiseSum scps1 = StaticCrossRangePairwiseSum.withExpectedQueries(a, b, kernel, 2);
		assertEquals(100, scps1.blockSize());

		// For K=20000: sContinuous = sqrt(1) = 1
		StaticCrossRangePairwiseSum scps3 = StaticCrossRangePairwiseSum.withExpectedQueries(a, b, kernel, 20000);
		assertEquals(1, scps3.blockSize());
	}

	@Test
	public void testWithExpectedQueriesQueries() {
		long[] a = {3, 1, 4, 1, 5, 9, 2, 6, 5, 35};
		long[] b = {2, 7, 1, 8, 2, 8, 1, 8, 2, 84};
		ProductKernel kernel = new ProductKernel();

		StaticCrossRangePairwiseSum scpsInt = StaticCrossRangePairwiseSum.withExpectedQueries(a, b, kernel, 15);

		long expected = 0;
		for (int i = 1; i < 4; i++) {
			for (int j = 2; j < 6; j++) {
				expected += a[i] * b[j];
			}
		}
		assertEquals(expected, scpsInt.get(1, 4, 2, 6));
	}

	@Test
	public void testWithExpectedQueriesInvalidArgs() {
		long[] a = {1, 2};
		long[] b = {3, 4};
		ProductKernel kernel = new ProductKernel();

		assertThrows(IllegalArgumentException.class, () -> {
			StaticCrossRangePairwiseSum.withExpectedQueries(a, b, kernel, 0);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			StaticCrossRangePairwiseSum.withExpectedQueries(a, b, kernel, -5);
		});
		assertThrows(NullPointerException.class, () -> {
			StaticCrossRangePairwiseSum.withExpectedQueries(null, b, kernel, 10);
		});
	}
}
