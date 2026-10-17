package library.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import library.util.graph.tree.Tree;
import library.util.graph.tree.LaminarMatroidOnTree;

public class LaminarMatroidOnTreeTest {

	@Test
	public void testSingleNode() {
		Tree tree = new Tree(1);
		tree.rooted(0);
		long[] A = {10};
		long[] C = {5};
		long[] B = {3};

		long ans = LaminarMatroidOnTree.maxSubtreePackingValue(tree, A, C, B);
		assertEquals(30, ans);

		long[] x = LaminarMatroidOnTree.maxSubtreePacking(tree, A, C, B);
		assertArrayEquals(new long[]{3}, x);
	}

	@Test
	public void testPathTwoNodes() {
		// 0 - 1, 0 is root
		Tree tree = new Tree(2);
		tree.addEdge(0, 1);
		tree.rooted(0);

		long[] A = {10, 20};
		long[] C = {5, 5};
		long[] B = {8, 4}; // B_0 = 8, B_1 = 4

		// For subtree(1) (which is just {1}): x_1 <= B_1 = 4.
		// Since A_1 = 20 (high weight), we should greedily assign x_1 = min(C_1, B_1) = min(5, 4) = 4.
		// For subtree(0) ({0, 1}): x_0 + x_1 <= B_0 = 8.
		// Since x_1 = 4, x_0 can be up to 8 - 4 = 4.
		// x_0 <= C_0 = 5, so x_0 = min(5, 4) = 4.
		// Expected x = [4, 4]. Weight = 4 * 10 + 4 * 20 = 120.

		long ans = LaminarMatroidOnTree.maxSubtreePackingValue(tree, A, C, B);
		assertEquals(120, ans);

		long[] x = LaminarMatroidOnTree.maxSubtreePacking(tree, A, C, B);
		assertArrayEquals(new long[]{4, 4}, x);
	}

	@Test
	public void testPathTwoNodesPreferRoot() {
		// 0 - 1, 0 is root
		Tree tree = new Tree(2);
		tree.addEdge(0, 1);
		tree.rooted(0);

		long[] A = {20, 10}; // root is more valuable
		long[] C = {5, 5};
		long[] B = {4, 4}; // B_0 = 4, B_1 = 4

		// For subtree(1): x_1 <= 4. We greedily set x_1 = 4.
		// For subtree(0): x_0 + x_1 <= 4.
		// Since A_0 = 20 > A_1 = 10, when we process v = 0:
		// We merge heaps: Heap at 1 has (10, 4, 1). We add (20, 5, 0).
		// Merged has elements with weight 10 and 20. Total count = 9.
		// But limit B_0 = 4. We pop elements with smallest weights first.
		// Smallest is (10, 4, 1) -> we discard it because we need to decrease totalCount to 4.
		// Remaining is (20, 5, 0) but capped to count 4.
		// So x_0 = 4, x_1 = 0.
		// Weight = 4 * 20 + 0 * 10 = 80.

		long ans = LaminarMatroidOnTree.maxSubtreePackingValue(tree, A, C, B);
		assertEquals(80, ans);

		long[] x = LaminarMatroidOnTree.maxSubtreePacking(tree, A, C, B);
		assertArrayEquals(new long[]{4, 0}, x);
	}

	@Test
	public void testZeroCapacitiesAndNegativeWeights() {
		Tree tree = new Tree(3);
		tree.addEdge(0, 1);
		tree.addEdge(0, 2);
		tree.rooted(0);

		long[] A = {-5, 10, 0};
		long[] C = {10, 10, 10};
		long[] B = {5, 5, 5};

		// A_0 <= 0 -> x_0 = 0 (handled by A_0 > 0 ? C_0 : 0 in LaminarMatroidOnTree)
		// A_2 = 0 -> x_2 = 0
		// A_1 = 10 -> x_1 = min(C_1, B_1) = min(10, 5) = 5.
		// For subtree(0), limit B_0 = 5.
		// Since total sum of x_0 + x_1 + x_2 <= 5 and x_1 = 5, other x_i remain 0.
		long ans = LaminarMatroidOnTree.maxSubtreePackingValue(tree, A, C, B);
		assertEquals(50, ans);

		long[] x = LaminarMatroidOnTree.maxSubtreePacking(tree, A, C, B);
		assertArrayEquals(new long[]{0, 5, 0}, x);
	}

	@Test
	public void testLPDualOptimality() {
		Tree tree = new Tree(3);
		tree.addEdge(0, 1);
		tree.addEdge(0, 2);
		tree.rooted(0);

		long[] A = {15, 10, 20};
		long[] C = {5, 5, 5};
		long[] B = {8, 4, 3}; // B_0=8, B_1=4, B_2=3

		// Primal LP:
		// Maximize 15 x_0 + 10 x_1 + 20 x_2
		// subject to:
		// x_0, x_1, x_2 >= 0
		// x_0 <= 5, x_1 <= 5, x_2 <= 5
		// x_1 <= 4 (subtree of 1 is {1})
		// x_2 <= 3 (subtree of 2 is {2})
		// x_0 + x_1 + x_2 <= 8 (subtree of 0 is {0, 1, 2})
		//
		// Dual LP:
		// Minimize sum_{v} C_v y_v + sum_{v} B_v z_v
		// subject to:
		// y_v, z_v >= 0
		// For v = 0: y_0 + z_0 >= 15
		// For v = 1: y_1 + z_1 + z_0 >= 10
		// For v = 2: y_2 + z_2 + z_0 >= 20

		long[] x = LaminarMatroidOnTree.maxSubtreePacking(tree, A, C, B);
		LaminarMatroidOnTree.DualResult dual = LaminarMatroidOnTree.minAncestorCover(tree, A, C, B);

		// Verify primal feasibility:
		for (int i = 0; i < tree.N; i++) {
			assertTrue(x[i] >= 0);
			assertTrue(x[i] <= C[i]);
		}
		assertTrue(x[1] <= B[1]);
		assertTrue(x[2] <= B[2]);
		assertTrue(x[0] + x[1] + x[2] <= B[0]);

		// Verify dual feasibility:
		for (int i = 0; i < tree.N; i++) {
			assertTrue(dual.y[i] >= 0);
			assertTrue(dual.z[i] >= 0);
		}
		assertTrue(dual.y[0] + dual.z[0] >= A[0]);
		assertTrue(dual.y[1] + dual.z[1] + dual.z[0] >= A[1]);
		assertTrue(dual.y[2] + dual.z[2] + dual.z[0] >= A[2]);

		// Verify strong duality (objective value matches):
		long primalVal = 0;
		for (int i = 0; i < tree.N; i++) {
			primalVal += x[i] * A[i];
		}

		long dualVal = 0;
		for (int i = 0; i < tree.N; i++) {
			dualVal += C[i] * dual.y[i] + B[i] * dual.z[i];
		}

		assertEquals(primalVal, dualVal);
	}
}
