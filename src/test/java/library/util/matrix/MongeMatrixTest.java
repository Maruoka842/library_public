package library.util.matrix;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MongeMatrixTest {

	private MongeLong createSampleMonge(int r, int c) {
		return new MongeLong() {
			@Override
			public int rows() {
				return r;
			}

			@Override
			public int cols() {
				return c;
			}

			@Override
			public long get(int i, int j) {
				// (i - j)^2 is Monge
				return (long) (i - j) * (i - j);
			}
		};
	}

	private AntiMongeLong createSampleAntiMonge(int r, int c) {
		return new AntiMongeLong() {
			@Override
			public int rows() {
				return r;
			}

			@Override
			public int cols() {
				return c;
			}

			@Override
			public long get(int i, int j) {
				// -(i - j)^2 is AntiMonge
				return -(long) (i - j) * (i - j);
			}
		};
	}

	@Test
	public void testLongMatrixEntryFunction() {
		LongMatrix.EntryFunction fn = (r, c) -> r * 10L + c;
		assertEquals(23L, fn.get(2, 3));
	}

	@Test
	public void testMongeAdd() {
		MongeLong m1 = createSampleMonge(3, 3);
		MongeLong m2 = createSampleMonge(3, 3);
		MongeLong sum = m1.add(m2);

		assertEquals(3, sum.rows());
		assertEquals(3, sum.cols());
		assertEquals(0L, sum.get(0, 0));
		assertEquals(2L, sum.get(0, 1)); // 2 * (0 - 1)^2
		assertEquals(2L, sum.get(1, 0));

		MongeLong m3 = createSampleMonge(3, 4);
		assertThrows(IllegalArgumentException.class, () -> m1.add(m3));
	}

	@Test
	public void testMongeScale() {
		MongeLong m = createSampleMonge(3, 3);
		MongeLong scaled = m.scale(5);

		assertEquals(3, scaled.rows());
		assertEquals(3, scaled.cols());
		assertEquals(5L, scaled.get(0, 1));
		assertEquals(20L, scaled.get(0, 2));

		assertThrows(IllegalArgumentException.class, () -> m.scale(-1));
	}

	@Test
	public void testMongeNegate() {
		MongeLong m = createSampleMonge(3, 3);
		AntiMongeLong negated = m.negate();

		assertEquals(3, negated.rows());
		assertEquals(3, negated.cols());
		assertEquals(-1L, negated.get(0, 1));
		assertEquals(-4L, negated.get(0, 2));
	}

	@Test
	public void testMongeTranspose() {
		MongeLong m = createSampleMonge(2, 4);
		MongeLong transposed = m.transpose();

		assertEquals(4, transposed.rows());
		assertEquals(2, transposed.cols());
		assertEquals(m.get(1, 3), transposed.get(3, 1));
	}

	@Test
	public void testMongePotentials() {
		MongeLong m = createSampleMonge(2, 3);
		long[] rowPot = {10, 20};
		long[] colPot = {1, 2, 3};

		MongeLong rowMod = m.addRowPotential(rowPot);
		assertEquals(10L, rowMod.get(0, 0)); // 0 + 10
		assertEquals(11L, rowMod.get(0, 1)); // 1 + 10
		assertEquals(21L, rowMod.get(1, 0)); // 1 + 20

		MongeLong colMod = m.addColumnPotential(colPot);
		assertEquals(1L, colMod.get(0, 0)); // 0 + 1
		assertEquals(3L, colMod.get(0, 1)); // 1 + 2
		assertEquals(2L, colMod.get(1, 0)); // 1 + 1 = 2

		assertThrows(IllegalArgumentException.class, () -> m.addRowPotential(new long[]{1}));
		assertThrows(IllegalArgumentException.class, () -> m.addColumnPotential(new long[]{1, 2}));
	}

	@Test
	public void testMongeSubmatrix() {
		MongeLong m = createSampleMonge(4, 5);
		int[] selRows = {0, 2};
		int[] selCols = {1, 3, 4};

		MongeLong sub = m.submatrix(selRows, selCols);
		assertEquals(2, sub.rows());
		assertEquals(3, sub.cols());

		// sub.get(1, 1) -> m.get(2, 3) -> (2 - 3)^2 = 1
		assertEquals(m.get(2, 3), sub.get(1, 1));

		// Non-strictly-increasing tests
		assertThrows(IllegalArgumentException.class, () -> m.submatrix(new int[]{2, 1}, selCols));
		assertThrows(IllegalArgumentException.class, () -> m.submatrix(new int[]{1, 1}, selCols));
		assertThrows(IllegalArgumentException.class, () -> m.submatrix(selRows, new int[]{3, 2}));
		assertThrows(IllegalArgumentException.class, () -> m.submatrix(selRows, new int[]{1, 1}));
	}

	@Test
	public void testAntiMongeOperations() {
		AntiMongeLong a1 = createSampleAntiMonge(3, 3);
		AntiMongeLong a2 = createSampleAntiMonge(3, 3);

		// add
		AntiMongeLong sum = a1.add(a2);
		assertEquals(-2L, sum.get(0, 1));

		assertThrows(IllegalArgumentException.class, () -> a1.add(createSampleAntiMonge(3, 4)));

		// scale
		AntiMongeLong scaled = a1.scale(3);
		assertEquals(-3L, scaled.get(0, 1));
		assertThrows(IllegalArgumentException.class, () -> a1.scale(-2));

		// negate
		MongeLong negated = a1.negate();
		assertEquals(1L, negated.get(0, 1));

		// transpose
		AntiMongeLong transposed = createSampleAntiMonge(2, 4).transpose();
		assertEquals(4, transposed.rows());
		assertEquals(2, transposed.cols());

		// potentials
		AntiMongeLong rowMod = a1.addRowPotential(new long[]{100, 200, 300});
		assertEquals(99L, rowMod.get(0, 1)); // -1 + 100

		AntiMongeLong colMod = a1.addColumnPotential(new long[]{10, 20, 30});
		assertEquals(19L, colMod.get(0, 1)); // -1 + 20

		assertThrows(IllegalArgumentException.class, () -> a1.addRowPotential(new long[]{10}));
		assertThrows(IllegalArgumentException.class, () -> a1.addColumnPotential(new long[]{10}));

		// submatrix
		AntiMongeLong sub = a1.submatrix(new int[]{0, 2}, new int[]{1, 2});
		assertEquals(2, sub.rows());
		assertEquals(2, sub.cols());
		assertEquals(a1.get(2, 2), sub.get(1, 1));

		assertThrows(IllegalArgumentException.class, () -> a1.submatrix(new int[]{1, 0}, new int[]{1, 2}));
		assertThrows(IllegalArgumentException.class, () -> a1.submatrix(new int[]{0, 1}, new int[]{2, 1}));
	}
}
