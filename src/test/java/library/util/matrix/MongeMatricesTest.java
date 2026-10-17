package library.util.matrix;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class MongeMatricesTest {

	@Test
	public void testEmpty() {
		long[][] empty = new long[0][0];
		assertTrue(MongeMatrices.isMonge(empty));

		MongeMatrices.Permutation perm = MongeMatrices.findMongePermutation(empty);
		assertNotNull(perm);
		assertEquals(0, perm.rows().length);
		assertEquals(0, perm.cols().length);
	}

	@Test
	public void testSingleElement() {
		long[][] matrix = {{42}};
		assertTrue(MongeMatrices.isMonge(matrix));

		MongeMatrices.Permutation perm = MongeMatrices.findMongePermutation(matrix);
		assertNotNull(perm);
		assertArrayEquals(new int[]{0}, perm.rows());
		assertArrayEquals(new int[]{0}, perm.cols());
	}

	@Test
	public void testSingleRowOrColumn() {
		long[][] rowMatrix = {{1, 2, 3}};
		assertTrue(MongeMatrices.isMonge(rowMatrix));
		MongeMatrices.Permutation permRow = MongeMatrices.findMongePermutation(rowMatrix);
		assertNotNull(permRow);
		assertArrayEquals(new int[]{0}, permRow.rows());
		assertArrayEquals(new int[]{0, 1, 2}, permRow.cols());

		long[][] colMatrix = {{1}, {2}, {3}};
		assertTrue(MongeMatrices.isMonge(colMatrix));
		MongeMatrices.Permutation permCol = MongeMatrices.findMongePermutation(colMatrix);
		assertNotNull(permCol);
		assertArrayEquals(new int[]{0, 1, 2}, permCol.rows());
		assertArrayEquals(new int[]{0}, permCol.cols());
	}

	@Test
	public void testIsMongeTrueAndFalse() {
		// Valid Monge
		long[][] monge = {
			{10, 12, 13},
			{14, 15, 16},
			{17, 18, 19}
		};
		assertTrue(MongeMatrices.isMonge(monge));

		// Invalid Monge
		long[][] nonMonge = {
			{10, 15, 13},
			{14, 15, 16},
			{17, 18, 19}
		};
		assertFalse(MongeMatrices.isMonge(nonMonge));
	}

	@Test
	public void testFindMongePermutationAlreadyMonge() {
		long[][] monge = {
			{3, 4, 5},
			{5, 5, 5},
			{7, 6, 5}
		};
		assertTrue(MongeMatrices.isMonge(monge));

		MongeMatrices.Permutation perm = MongeMatrices.findMongePermutation(monge);
		assertNotNull(perm);

		// Apply perm and verify it is Monge
		assertTrue(isAppliedMonge(monge, perm));
	}

	@Test
	public void testFindMongePermutationWithShuffledMonge() {
		// A Monge matrix: A[i][j] = (i - j)^2
		int H = 5;
		int W = 6;
		long[][] monge = new long[H][W];
		for (int i = 0; i < H; i++) {
			for (int j = 0; j < W; j++) {
				monge[i][j] = (long) (i - j) * (i - j);
			}
		}
		assertTrue(MongeMatrices.isMonge(monge));

		// Shuffle row and col indices
		int[] rowShuf = {3, 0, 4, 1, 2};
		int[] colShuf = {5, 1, 4, 0, 3, 2};

		long[][] shuffled = new long[H][W];
		for (int i = 0; i < H; i++) {
			for (int j = 0; j < W; j++) {
				shuffled[i][j] = monge[rowShuf[i]][colShuf[j]];
			}
		}

		MongeMatrices.Permutation perm = MongeMatrices.findMongePermutation(shuffled);
		assertNotNull(perm);
		assertTrue(isAppliedMonge(shuffled, perm));
	}

	@Test
	public void testFindMongePermutationUnsolvable() {
		// A 3x3 matrix with no Monge permutation
		long[][] impossible = {
			{0, 0, 0},
			{0, 0, 1},
			{0, 1, 1}
		};
		assertNull(MongeMatrices.findMongePermutation(impossible));
	}

	@Test
	public void testRandomPermutedMonge() {
		Random rand = new Random(42);
		for (int t = 0; t < 20; t++) {
			int H = 4 + rand.nextInt(5);
			int W = 4 + rand.nextInt(5);

			// Generate a random Monge matrix
			// M[i][j] = rowVal[i] + colVal[j] + rowMul[i] * colMul[j]
			// if rowMul is increasing and colMul is decreasing, it is Monge.
			long[] rowVal = new long[H];
			long[] colVal = new long[W];
			long[] rowMul = new long[H];
			long[] colMul = new long[W];
			for (int i = 0; i < H; i++) {
				rowVal[i] = rand.nextInt(100);
				rowMul[i] = i * 10L + rand.nextInt(5);
			}
			for (int j = 0; j < W; j++) {
				colVal[j] = rand.nextInt(100);
				colMul[j] = (W - j) * 10L + rand.nextInt(5);
			}
			// Sort rowMul ascending, colMul descending to be absolutely sure
			java.util.Arrays.sort(rowMul);
			// Sort colMul descending
			java.util.Arrays.sort(colMul);
			for (int i = 0; i < W / 2; i++) {
				long tmp = colMul[i];
				colMul[i] = colMul[W - 1 - i];
				colMul[W - 1 - i] = tmp;
			}

			long[][] monge = new long[H][W];
			for (int i = 0; i < H; i++) {
				for (int j = 0; j < W; j++) {
					monge[i][j] = rowVal[i] + colVal[j] + rowMul[i] * colMul[j];
				}
			}

			assertTrue(MongeMatrices.isMonge(monge), "Generated matrix should be Monge");

			// Permute
			int[] rowPerm = randomPermutation(H, rand);
			int[] colPerm = randomPermutation(W, rand);
			long[][] permuted = new long[H][W];
			for (int i = 0; i < H; i++) {
				for (int j = 0; j < W; j++) {
					permuted[i][j] = monge[rowPerm[i]][colPerm[j]];
				}
			}

			MongeMatrices.Permutation result = MongeMatrices.findMongePermutation(permuted);
			assertNotNull(result, "Should find a permutation");
			assertTrue(isAppliedMonge(permuted, result), "Applied permutation should produce Monge");
		}
	}

	private boolean isAppliedMonge(long[][] a, MongeMatrices.Permutation perm) {
		return MongeMatrices.isMonge(a, perm.rows(), perm.cols());
	}

	private int[] randomPermutation(int n, Random rand) {
		int[] p = new int[n];
		for (int i = 0; i < n; i++) {
			p[i] = i;
		}
		for (int i = n - 1; i > 0; i--) {
			int j = rand.nextInt(i + 1);
			int tmp = p[i];
			p[i] = p[j];
			p[j] = tmp;
		}
		return p;
	}
}
