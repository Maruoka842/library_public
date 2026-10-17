package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GraphsCanonTest {
	@Test
	public void isomorphicGraphsHaveTheSameCanonicalMatrix() {
		boolean[][] graph = {
				{false, true, false, true},
				{true, false, true, false},
				{false, true, false, false},
				{true, false, false, false}
		};
		int[] permutation = {2, 0, 3, 1};
		boolean[][] relabeled = relabel(graph, permutation);

		assertArrayEquals(canonicalMatrix(graph), canonicalMatrix(relabeled));
	}

	@Test
	public void nonIsomorphicGraphsHaveDifferentCanonicalMatrices() {
		boolean[][] path = {
				{false, true, false, false},
				{true, false, true, false},
				{false, true, false, true},
				{false, false, true, false}
		};
		boolean[][] star = {
				{false, true, true, true},
				{true, false, false, false},
				{true, false, false, false},
				{true, false, false, false}
		};

		org.junit.jupiter.api.Assertions.assertFalse(
				java.util.Arrays.deepEquals(canonicalMatrix(path), canonicalMatrix(star)));
	}

	@Test
	public void supportsDirectedEdgesLoopsAndEmptyGraph() {
		boolean[][] graph = {
				{true, true, false},
				{false, false, true},
				{true, false, false}
		};
		assertArrayEquals(canonicalMatrix(graph), canonicalMatrix(relabel(graph, new int[] {1, 2, 0})));
		assertArrayEquals(new int[0], Graphs.canonOrder(new boolean[0][0]));
	}

	@Test
	public void returnsPermutationAndRejectsNonSquareInput() {
		boolean[][] graph = {{false, true}, {true, false}};
		assertArrayEquals(new int[] {0, 1}, Graphs.canonOrder(graph));
		assertThrows(IllegalArgumentException.class,
				() -> Graphs.canonOrder(new boolean[][] {{false}, {true}}));
	}

	private static boolean[][] canonicalMatrix(boolean[][] graph) {
		return relabel(graph, Graphs.canonOrder(graph));
	}

	private static boolean[][] relabel(boolean[][] graph, int[] permutation) {
		int n = graph.length;
		boolean[][] result = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				result[i][j] = graph[permutation[i]][permutation[j]];
			}
		}
		return result;
	}
}
