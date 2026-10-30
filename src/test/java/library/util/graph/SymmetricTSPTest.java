package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class SymmetricTSPTest {

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
			{10, 0, 35, 25},
			{15, 35, 0, 30},
			{20, 25, 30, 0}
		};
		SymmetricTSP tsp = new SymmetricTSP(dist);
		assertEquals(4, tsp.n());

		SymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
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
		// generate 2D Euclidean distance
		double[][] pts = new double[n][2];
		for (int i = 0; i < n; i++) {
			pts[i][0] = rnd.nextDouble() * 100;
			pts[i][1] = rnd.nextDouble() * 100;
		}
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				double dx = pts[i][0] - pts[j][0];
				double dy = pts[i][1] - pts[j][1];
				dist[i][j] = Math.round(Math.hypot(dx, dy) * 100);
			}
		}

		SymmetricTSP tsp = new SymmetricTSP(dist);
		SymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
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
			for (int j = i + 1; j < n; j++) {
				dist[i][j] = dist[j][i] = rnd.nextInt(1000) + 1;
			}
		}

		SymmetricTSP tsp = new SymmetricTSP(dist);
		SymmetricTSP.Solution sol = tsp.nearestNeighbor(0);

		tsp.twoOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));

		boolean improved3Opt = tsp.threeOpt(sol);
		assertValidPermutation(n, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));

		long costBeforeBridge = sol.cost;
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
			{1, 0, 2},
			{3, 2, 0}
		};
		HeldKarp.DenseDistanceMatrix denseDist = new HeldKarp.DenseDistanceMatrix(matrix);
		SymmetricTSP tsp = new SymmetricTSP(denseDist);

		assertEquals(3, tsp.n());
		SymmetricTSP.Solution sol = tsp.nearestNeighbor(0);
		assertValidPermutation(3, sol.path);
		assertEquals(sol.cost, tsp.eval(sol));
	}

	@Test
	public void testSmallN() {
		SymmetricTSP tsp0 = new SymmetricTSP(new long[0][0]);
		assertEquals(0, tsp0.n());
		SymmetricTSP.Solution sol0 = tsp0.nearestNeighbor(0);
		assertEquals(0, sol0.path.length);
		assertEquals(0, sol0.cost);
		assertFalse(tsp0.doubleBridge(sol0));

		long[][] d2 = {{0, 5}, {5, 0}};
		SymmetricTSP tsp2 = new SymmetricTSP(d2);
		SymmetricTSP.Solution sol2 = tsp2.nearestNeighbor(0);
		assertEquals(2, sol2.path.length);
		assertEquals(10, sol2.cost);
		assertFalse(tsp2.doubleBridge(sol2));
	}
}
