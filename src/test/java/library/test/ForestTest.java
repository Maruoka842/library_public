package library.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.Forest;
import library.util.graph.tree.Tree;

public class ForestTest {

	@Test
	public void testPrefixSumSingleTree() {
		// Tree 0-1-2
		Tree tree = Tree.path(3);
		tree.rooted(0);
		long[] a = { 10, 20, 30 };
		long[] expected = { 10, 30, 60 };
		assertArrayEquals(expected, tree.prefixSum(a));
	}

	@Test
	public void testPrefixSumMultiComponentForest() {
		// Forest with 2 components:
		// Component 1: 0 - 1 - 2
		// Component 2: 3 - 4
		Forest forest = new Forest(5);
		forest.addEdge(0, 1);
		forest.addEdge(1, 2);
		forest.addEdge(3, 4);

		forest.rooted(0); // 0 is root for first component, 3 or 4 for second

		long[] a = { 1, 2, 3, 4, 5 };
		long[] res = forest.prefixSum(a);

		// Component 1 root-path sums:
		// 0: 1
		// 1: 1+2 = 3
		// 2: 1+2+3 = 6
		assertEquals(1, res[0]);
		assertEquals(3, res[1]);
		assertEquals(6, res[2]);

		// Component 2 root-path sums depend on which is root of component 2
		// If 3 is root: res[3]=4, res[4]=4+5=9.
		// If 4 is root: res[4]=5, res[3]=5+4=9.
		if (forest.isRoot(3)) {
			// 3 is root
			assertEquals(4, res[3]);
			assertEquals(9, res[4]);
		} else {
			// 4 is root
			assertEquals(5, res[4]);
			assertEquals(9, res[3]);
		}
	}

	@Test
	public void testPrefixSumFromParentArrayConstructor() {
		// parent: 0 is root (-1), 1's parent is 0, 2's parent is 0, 3 is root (-1), 4's parent is 3
		int[] parent = { -1, 0, 0, -1, 3 };
		Forest forest = new Forest(parent);

		long[] a = { 10, 20, 30, 40, 50 };
		long[] res = forest.prefixSum(a);

		// Component 1 (rooted at 0):
		// 0: 10
		// 1: 10 + 20 = 30
		// 2: 10 + 30 = 40
		// Component 2 (rooted at 3):
		// 3: 40
		// 4: 40 + 50 = 90
		long[] expected = { 10, 30, 40, 40, 90 };
		assertArrayEquals(expected, res);
	}

	@Test
	public void testPrefixSumThrowsWhenNotRooted() {
		Forest forest = new Forest(3);
		forest.addEdge(0, 1);
		forest.addEdge(1, 2);
		long[] a = { 1, 2, 3 };
		assertThrows(AssertionError.class, () -> forest.prefixSum(a));
	}

	@Test
	public void testPrefixSumWithLongBinaryOperator() {
		// Tree 0-1-2 with max operator
		Tree tree = Tree.path(3);
		tree.rooted(0);
		long[] a = { 10, 5, 20 };
		long[] resMax = tree.prefixSum(a, Long::max);
		// Path to 0: max(10) = 10
		// Path to 1: max(10, 5) = 10
		// Path to 2: max(10, 5, 20) = 20
		long[] expectedMax = { 10, 10, 20 };
		assertArrayEquals(expectedMax, resMax);

		// Tree 0-1-2 with multiplication operator
		long[] aMul = { 2, 3, 4 };
		long[] resMul = tree.prefixSum(aMul, (x, y) -> x * y);
		// Path to 0: 2
		// Path to 1: 2 * 3 = 6
		// Path to 2: 6 * 4 = 24
		long[] expectedMul = { 2, 6, 24 };
		assertArrayEquals(expectedMul, resMul);
	}

	@Test
	public void testPrefixSumWithGenericBinaryOperator() {
		// Tree 0-1-2 with String concatenation
		Tree tree = Tree.path(3);
		tree.rooted(0);
		String[] a = { "A", "B", "C" };
		String[] res = tree.prefixSum(a, (x, y) -> x + y);
		String[] expected = { "A", "AB", "ABC" };
		assertArrayEquals(expected, res);
	}
}
