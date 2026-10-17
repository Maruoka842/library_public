package library.util.mo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.Tree;

public class TreeMoCursorValidatorTest {
	private static class SumCursor implements TreeMoCursor<Long> {
		/** 各頂点の重み。 */
		private final long[] weights;
		/** 現在の頂点重みの総和。 */
		private long sum;

		SumCursor(long[] weights) {
			this.weights = weights;
		}

		@Override
		public void addVertex(int v) {
			sum += weights[v];
		}

		@Override
		public void removeVertex(int v) {
			sum -= weights[v];
		}

		@Override
		public Long getValue(int queryId) {
			return sum + queryId;
		}
	}

	private static class BrokenInverseCursor implements TreeMoCursor<Long> {
		/** 現在値。 */
		private long value;

		@Override
		public void addVertex(int v) {
			value += v + 1;
		}

		@Override
		public void removeVertex(int v) {}

		@Override
		public Long getValue(int queryId) {
			return value;
		}
	}

	private static class NonCommutativeCursor implements TreeMoCursor<int[]> {
		/** 現在の置換。 */
		private int[] permutation = {0, 1, 2};

		@Override
		public void addVertex(int v) {
			apply(v);
		}

		@Override
		public void removeVertex(int v) {
			apply(v);
		}

		@Override
		public int[] getValue(int queryId) {
			return permutation.clone();
		}

		private void apply(int v) {
			int[] p = (v & 1) == 0 ? new int[] {1, 0, 2} : new int[] {0, 2, 1};
			int[] next = new int[3];
			for (int i = 0; i < 3; i++) next[i] = p[permutation[i]];
			permutation = next;
		}
	}

	@Test
	public void testValidCursor() {
		Tree tree = new Tree(6);
		tree.addEdge(0, 1);
		tree.addEdge(0, 2);
		tree.addEdge(1, 3);
		tree.addEdge(1, 4);
		tree.addEdge(2, 5);
		SumCursor cursor = new SumCursor(new long[] {3, -1, 4, -1, 5, 9});
		TreeMoCursorValidator.validate(tree, cursor, 7);
		assertEquals(7L, cursor.getValue(7));
	}

	@Test
	public void testBrokenInverseFails() {
		Tree tree = Tree.path(3);
		assertThrows(AssertionError.class,
				() -> TreeMoCursorValidator.validate(tree, new BrokenInverseCursor(), 0));
	}

	@Test
	public void testNonCommutativeUpdatesFail() {
		Tree tree = Tree.path(3);
		assertThrows(AssertionError.class,
				() -> TreeMoCursorValidator.validate(tree, new NonCommutativeCursor(), 0));
	}

	@Test
	public void testEmptyTreeFails() {
		assertThrows(IllegalArgumentException.class,
				() -> TreeMoCursorValidator.validate(new Tree(0), new SumCursor(new long[0]), 0));
	}
}
