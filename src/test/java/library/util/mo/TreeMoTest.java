package library.util.mo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.Tree;

public class TreeMoTest {
	private static class PathSumCursor implements TreeMoCursor<Long> {
		/** 各頂点の重み。 */
		private final long[] values;
		/** 各頂点が現在の集合に含まれるか。 */
		private final boolean[] present;
		/** 現在の集合に含まれる頂点重みの総和。 */
		private long sum;

		PathSumCursor(long[] values) {
			this.values = values;
			present = new boolean[values.length];
		}

		@Override
		public void addVertex(int v) {
			if (present[v]) throw new AssertionError("duplicate addition: " + v);
			present[v] = true;
			sum += values[v];
		}

		@Override
		public void removeVertex(int v) {
			if (!present[v]) throw new AssertionError("removing absent vertex: " + v);
			present[v] = false;
			sum -= values[v];
		}

		@Override
		public Long getValue(int queryId) {
			return sum;
		}
	}

	@Test
	public void testAncestorAndNonAncestorPaths() {
		Tree tree = new Tree(7);
		tree.addEdge(0, 1);
		tree.addEdge(0, 2);
		tree.addEdge(1, 3);
		tree.addEdge(1, 4);
		tree.addEdge(2, 5);
		tree.addEdge(2, 6);
		tree.rooted(2);
		long[] values = {1, 2, 4, 8, 16, 32, 64};
		TreeMo mo = new TreeMo(tree);
		mo.addQuery(0, 4);
		mo.addQuery(3, 4);
		mo.addQuery(3, 5);
		mo.addQuery(6, 6);
		PathSumCursor cursor = new PathSumCursor(values);
		TreeMoCursorValidator.validate(tree, cursor, 0);
		assertEquals(List.of(19L, 26L, 47L, 64L), mo.run(cursor));
	}

	@Test
	public void testAllPairsAgainstTreePath() {
		Random random = new Random(20260928L);
		for (int n = 1; n <= 40; n++) {
			Tree tree = new Tree(n);
			for (int v = 1; v < n; v++) tree.addEdge(v, random.nextInt(v));
			long[] values = new long[n];
			for (int v = 0; v < n; v++) values[v] = random.nextInt(201) - 100;
			TreeMo mo = new TreeMo(tree);
			List<Long> expected = new ArrayList<>();
			for (int u = 0; u < n; u++) {
				for (int v = 0; v < n; v++) {
					mo.addQuery(u, v);
					long sum = 0;
					for (int x : tree.path(u, v)) sum += values[x];
					expected.add(sum);
				}
			}
			assertEquals(expected, mo.run(new PathSumCursor(values)), "n=" + n);
		}
	}

	@Test
	public void testEdgeQueries() {
		Tree tree = new Tree(7);
		tree.addEdge(0, 1);
		tree.addEdge(0, 2);
		tree.addEdge(1, 3);
		tree.addEdge(1, 4);
		tree.addEdge(2, 5);
		tree.addEdge(2, 6);
		tree.rooted(2);
		long[] edgeAtChild = {10, 1, 0, 3, 4, 5, 6};
		TreeMo mo = new TreeMo(tree);
		mo.addEdgeQuery(0, 4);
		mo.addEdgeQuery(3, 5);
		mo.addEdgeQuery(6, 6);
		assertEquals(List.of(5L, 19L, 0L), mo.run(new PathSumCursor(edgeAtChild)));
	}

	@Test
	public void testEmptyQueriesAndInvalidVertex() {
		TreeMo mo = new TreeMo(new Tree(1));
		assertEquals(List.of(), mo.run(new PathSumCursor(new long[] {7})));
		assertThrows(IndexOutOfBoundsException.class, () -> mo.addQuery(-1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> mo.addQuery(0, 1));
	}

	@Test
	public void testClear() {
		TreeMo mo = new TreeMo(Tree.path(3));
		mo.addQuery(0, 2);
		mo.addEdgeQuery(0, 1);
		mo.clear();
		assertEquals(List.of(), mo.run(new PathSumCursor(new long[] {1, 2, 3})));
		assertEquals(0, mo.addQuery(1, 1));
		assertEquals(List.of(2L), mo.run(new PathSumCursor(new long[] {1, 2, 3})));
	}

	@Test
	public void testQueryIdIsPassedToCursor() {
		TreeMo mo = new TreeMo(Tree.path(2));
		mo.addQuery(0, 0);
		mo.addQuery(0, 1);
		mo.addEdgeQuery(0, 1);
		TreeMoCursor<Integer> cursor = new TreeMoCursor<>() {
			@Override
			public void addVertex(int v) {}

			@Override
			public void removeVertex(int v) {}

			@Override
			public Integer getValue(int queryId) {
				return queryId;
			}
		};
		assertEquals(List.of(0, 1, 2), mo.run(cursor));
	}
}
