package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class ASymmetricTSPTest {

	private void assertValidPermutation(int n, int[] path) {
		assertEquals(n, path.length);
		boolean[] seen = new boolean[n];
		for (int v : path) {
			assertTrue(v >= 0 && v < n);
			assertFalse(seen[v]);
			seen[v] = true;
		}
	}

	@Test
	public void testNearestNeighborAndEval() {
		long[][] dist = {
			{0, 10, 15, 20},
			{5, 0, 35, 25},
			{12, 30, 0, 30},
			{18, 22, 28, 0}
		};
		ASymmetricTSP tsp = new ASymmetricTSP(dist);
		assertEquals(4, tsp.n());

		ASymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
		assertValidPermutation(4, sol.path);
		long actualCost = tsp.eval(sol);
		assertEquals(sol.cost, actualCost);
		assertTrue(sol.toString().contains("cost=" + sol.cost));
	}

	@Test
	public void testTwoOptImprovement() {
		int n = 10;
		long[][] dist = new long[n][n];
		Random rnd = new Random(42);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j) continue;
				dist[i][j] = rnd.nextInt(100) + 1;
			}
		}

		ASymmetricTSP tsp = new ASymmetricTSP(dist);
		ASymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
		long initCost = sol.cost;

		tsp.twoOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));
		assertTrue(sol.cost <= initCost);
	}

	@Test
	public void testThreeOptAndDoubleBridge() {
		int n = 12;
		long[][] dist = new long[n][n];
		Random rnd = new Random(12345);
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (i == j) continue;
				dist[i][j] = rnd.nextInt(1000) + 1;
			}
		}

		ASymmetricTSP tsp = new ASymmetricTSP(dist);
		ASymmetricTSP.Solution sol = tsp.nearestNeighbor(0);

		tsp.twoOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));

		tsp.threeOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));

		boolean bridged = tsp.doubleBridge(sol, rnd);
		assertTrue(bridged);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));

		// Run 2-opt again after double bridge perturbation
		tsp.twoOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));
	}

	@Test
	public void testHeldKarpDistanceMatrixConstructor() {
		long[][] matrix = {
			{0, 1, 3},
			{4, 0, 2},
			{5, 6, 0}
		};
		HeldKarp.DenseDistanceMatrix denseDist = new HeldKarp.DenseDistanceMatrix(matrix);
		ASymmetricTSP tsp = new ASymmetricTSP(denseDist);

		assertEquals(3, tsp.n());
		ASymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
		assertValidPermutation(3, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));
	}

	@Test
	public void testSmallN() {
		ASymmetricTSP tsp0 = new ASymmetricTSP(new long[0][0]);
		assertEquals(0, tsp0.n());
		ASymmetricTSP.Solution sol0 = tsp0.nearestNeighbor(0);
		assertEquals(0, sol0.path.length);
		assertEquals(0, sol0.cost);
		assertFalse(tsp0.doubleBridge(sol0));

		long[][] d2 = {{0, 5}, {3, 0}};
		ASymmetricTSP tsp2 = new ASymmetricTSP(d2);
		ASymmetricTSP.Solution sol2 = tsp2.nearestNeighbor(0);
		assertEquals(2, sol2.path.length);
		assertEquals(8, sol2.cost);
		assertFalse(tsp2.doubleBridge(sol2));
	}
}
