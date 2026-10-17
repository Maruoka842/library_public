package library.util.mo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class MoCursorValidatorTest {

	private static class ArraySumCursor extends MoCursor<Long> {
		private final int[] a;
		private long sum = 0;

		public ArraySumCursor(int[] a) {
			this.a = a;
		}

		@Override
		public void addLeft(int i) {
			sum += a[i];
		}

		@Override
		public void popLeft(int i) {
			sum -= a[i];
		}

		@Override
		public void addRight(int i) {
			sum += a[i];
		}

		@Override
		public void popRight(int i) {
			sum -= a[i];
		}

		@Override
		public Long getValue(int queryId) {
			return sum;
		}
	}

	private static class BuggyCursor extends MoCursor<Long> {
		private final int[] a;
		private long sum = 0;

		public BuggyCursor(int[] a) {
			this.a = a;
		}

		@Override
		public void addLeft(int i) {
			sum += a[i];
		}

		@Override
		public void popLeft(int i) {
			// Bug: forgets to subtract
		}

		@Override
		public void addRight(int i) {
			sum += a[i];
		}

		@Override
		public void popRight(int i) {
			sum -= a[i];
		}

		@Override
		public Long getValue(int queryId) {
			return sum;
		}
	}

	private static class GridPointCursor extends MoCursor<Long> {
		private final long[][] grid;

		public GridPointCursor(long[][] grid) {
			this.grid = grid;
		}

		@Override
		public void addLeft(int i) {}

		@Override
		public void popLeft(int i) {}

		@Override
		public void addRight(int i) {}

		@Override
		public void popRight(int i) {}

		@Override
		public Long getValue(int queryId) {
			return grid[l][r];
		}
	}

	private static class BuggyGridCursor extends MoCursor<Long> {
		private final long[][] grid;
		private boolean corrupted = false;

		public BuggyGridCursor(long[][] grid) {
			this.grid = grid;
		}

		@Override
		public void addLeft(int i) {}

		@Override
		public void popLeft(int i) {
			corrupted = true;
		}

		@Override
		public void addRight(int i) {}

		@Override
		public void popRight(int i) {}

		@Override
		public Long getValue(int queryId) {
			return corrupted ? -100L : grid[l][r];
		}
	}

	@Test
	public void testValidCursorRangeDomain() {
		int[] a = {3, -1, 4, -1, 5, 9, -2, 6, 5, -3, 5};
		ArraySumCursor cursor = new ArraySumCursor(a);
		MoCursorValidator.validate(cursor, a.length, 0);
		assertEquals(0, cursor.l());
		assertEquals(0, cursor.r());
	}

	@Test
	public void testValidGridDomain() {
		long[][] grid = {
			{1, 2, 3},
			{4, 5, 6},
			{7, 8, 9},
			{10, 11, 12}
		};
		GridPointCursor cursor = new GridPointCursor(grid);
		MoCursorValidator.validateGrid(cursor, 4, 3, 0);
		assertEquals(0, cursor.l());
		assertEquals(0, cursor.r());

		MoCursorValidator.validateGrid(cursor, 3, 3, 0); // 3x3 square grid
		assertEquals(0, cursor.l());
		assertEquals(0, cursor.r());
	}

	@Test
	public void testBuggyGridCursorFailsValidation() {
		long[][] grid = {
			{1, 2},
			{3, 4}
		};
		BuggyGridCursor cursor = new BuggyGridCursor(grid);
		assertThrows(AssertionError.class, () -> MoCursorValidator.validateGrid(cursor, 2, 2, 0));
	}

	@Test
	public void testSmallLengths() {
		int[] a0 = {};
		ArraySumCursor cursor0 = new ArraySumCursor(a0);
		MoCursorValidator.validate(cursor0, 0, 0);

		int[] a1 = {10};
		ArraySumCursor cursor1 = new ArraySumCursor(a1);
		MoCursorValidator.validate(cursor1, 1, 0);

		int[] a2 = {10, -5};
		ArraySumCursor cursor2 = new ArraySumCursor(a2);
		MoCursorValidator.validate(cursor2, 2, 0);
	}

	@Test
	public void testPolyEvalAtFrequencyValid() {
		int[] a = {1, 2, 1, 1, 2, 3, 1, 2, 1};
		PolyEvalAtFrequency cursor = new PolyEvalAtFrequency(a);
		MoCursorValidator.validate(cursor, a.length, 0);
		assertEquals(0, cursor.l());
		assertEquals(0, cursor.r());
	}

	@Test
	public void testBuggyCursorFailsValidation() {
		int[] a = {1, 2, 3, 4, 5};
		BuggyCursor cursor = new BuggyCursor(a);
		assertThrows(AssertionError.class, () -> MoCursorValidator.validate(cursor, a.length, 0));
	}

	@Test
	public void testInvalidInitialPositionFails() {
		int[] a = {1, 2, 3};
		ArraySumCursor cursor = new ArraySumCursor(a);
		cursor.internal_addRight(); // moves to (0, 1)
		assertThrows(IllegalArgumentException.class, () -> MoCursorValidator.validate(cursor, a.length, 0));
	}

	@Test
	public void testMoAndCursorIntegration() {
		int[] a = {2, 3, 5, 7, 11};
		Mo mo = new Mo();
		mo.addQuery(0, 3); // 2 + 3 + 5 = 10
		mo.addQuery(1, 4); // 3 + 5 + 7 = 15
		mo.addQuery(2, 5); // 5 + 7 + 11 = 23
		mo.addQuery(0, 5); // 2 + 3 + 5 + 7 + 11 = 28

		ArraySumCursor cursor = new ArraySumCursor(a);
		MoCursorValidator.validate(cursor, a.length, 0);

		List<Long> results = mo.run(cursor);
		assertEquals(List.of(10L, 15L, 23L, 28L), results);
	}

	@Test
	public void testQueryIdIsPassedToCursor() {
		Mo mo = new Mo();
		mo.addQuery(0, 1);
		mo.addQuery(1, 2);
		mo.addQuery(0, 2);
		MoCursor<Integer> cursor = new MoCursor<>() {
			@Override
			public void addLeft(int i) {}

			@Override
			public void popLeft(int i) {}

			@Override
			public void addRight(int i) {}

			@Override
			public void popRight(int i) {}

			@Override
			public Integer getValue(int queryId) {
				return queryId;
			}
		};
		assertEquals(List.of(0, 1, 2), mo.run(cursor));
	}
}
