package library.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.AutomatonTreeKnapsack;
import library.util.graph.tree.Tree;

public class AutomatonTreeKnapsackTest {
	private static final long NEG = AutomatonTreeKnapsack.DEFAULT_UNREACHABLE;

	@Test
	public void accumulatesEveryVertexOfEachSubtree() {
		Tree tree = tree(6, new int[][] {{0, 1}, {0, 2}, {1, 3}, {1, 4}, {4, 5}});
		int[] weight = {1, 2, 3, 4, 5, 6};
		AutomatonTreeKnapsack solver = new AutomatonTreeKnapsack(
				tree, 1, 21, new AutomatonTreeKnapsack.Strategy() {
					@Override
					public int numPatterns(int v) {
						return 1;
					}

					@Override
					public int childState(int v, int pattern, int child) {
						return 0;
					}

					@Override
					public void finish(int v, int pattern, long[] accumulated, long[][] result) {
						for (int x = weight[v]; x < accumulated.length; x++) {
							if (accumulated[x - weight[v]] != NEG) {
								result[0][x] = accumulated[x - weight[v]] + weight[v];
							}
						}
					}
				});

		long[] actual = new long[tree.N];
		long[][][] dp = solver.solveAllSubtrees();
		for (int v = 0; v < tree.N; v++) {
			for (int x = 0; x < dp[v][0].length; x++) {
				if (dp[v][0][x] != NEG) actual[v] = dp[v][0][x];
			}
		}

		assertArrayEquals(new long[] {21, 17, 3, 4, 11, 6}, actual);
	}

	@Test
	public void supportsIgnoredChildrenAndMultipleStates() {
		Tree tree = tree(3, new int[][] {{0, 1}, {0, 2}});
		AutomatonTreeKnapsack solver = new AutomatonTreeKnapsack(
				tree, 2, 0, new AutomatonTreeKnapsack.Strategy() {
					@Override
					public int numPatterns(int v) {
						return 2;
					}

					@Override
					public int childState(int v, int pattern, int child) {
						return pattern == 0 ? AutomatonTreeKnapsack.NONE : 1;
					}

					@Override
					public void finish(int v, int pattern, long[] accumulated, long[][] result) {
						result[pattern][0] = Math.max(result[pattern][0], accumulated[0] + 1);
					}
				});

		long[][][] dp = solver.solveAllSubtrees();

		assertArrayEquals(new long[] {1, 3}, new long[] {dp[0][0][0], dp[0][1][0]});
		assertArrayEquals(new long[] {1, 1}, new long[] {dp[1][0][0], dp[1][1][0]});
		assertArrayEquals(new long[] {1, 1}, new long[] {dp[2][0][0], dp[2][1][0]});
	}

	@Test
	public void rejectsUnrootedTree() {
		AutomatonTreeKnapsack.Strategy strategy = new AutomatonTreeKnapsack.Strategy() {
			@Override
			public int numPatterns(int v) {
				return 0;
			}

			@Override
			public int childState(int v, int pattern, int child) {
				return 0;
			}

			@Override
			public void finish(int v, int pattern, long[] accumulated, long[][] result) {
			}
		};

		assertThrows(IllegalStateException.class,
				() -> new AutomatonTreeKnapsack(new Tree(1), 1, 0, strategy));
	}

	@Test
	public void returnedRowsDoNotShareReferences() {
		Tree tree = tree(2, new int[][] {{0, 1}});
		AutomatonTreeKnapsack solver = new AutomatonTreeKnapsack(
				tree, 1, 1, new AutomatonTreeKnapsack.Strategy() {
					@Override
					public int numPatterns(int v) {
						return 1;
					}

					@Override
					public int childState(int v, int pattern, int child) {
						return 0;
					}

					@Override
					public void finish(int v, int pattern, long[] accumulated, long[][] result) {
						result[0][0] = v;
					}
				});
		long[][][] dp = solver.solveAllSubtrees();
		assertArrayEquals(new long[] {0, NEG}, dp[0][0]);
		assertArrayEquals(new long[] {1, NEG}, dp[1][0]);
		dp[0][0][0] = 10;
		assertArrayEquals(new long[] {1, NEG}, dp[1][0]);
	}

	@Test
	public void restoresLightPathResultsInPreorder() {
		Tree tree = new Tree(3);
		tree.addEdge(2, 0);
		tree.addEdge(2, 1);
		tree.rooted(2);
		AutomatonTreeKnapsack solver = new AutomatonTreeKnapsack(
				tree, 1, 3, new AutomatonTreeKnapsack.Strategy() {
					@Override
					public int numPatterns(int v) {
						return 1;
					}

					@Override
					public int childState(int v, int pattern, int child) {
						return 0;
					}

					@Override
					public void finish(int v, int pattern, long[] accumulated, long[][] result) {
						for (int x = 1; x < accumulated.length; x++) {
							if (accumulated[x - 1] != NEG) result[0][x] = accumulated[x - 1] + v + 1;
						}
					}
				});
		long[][][] dp = solver.solveAllSubtrees();
		assertEquals(1, dp[0][0][1]);
		assertEquals(2, dp[1][0][1]);
		assertEquals(6, dp[2][0][3]);
	}

	private static Tree tree(int n, int[][] edges) {
		Tree tree = new Tree(n);
		for (int[] edge : edges) tree.addEdge(edge[0], edge[1]);
		tree.rooted(0);
		return tree;
	}
}
