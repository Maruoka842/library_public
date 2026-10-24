package library.util.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdjacencyMatrixGraphTest {

	@Test
	public void testConstructorAndEdgeOperations() {
		AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(4);
		assertEquals(4, g.N);
		assertEquals(0, g.M);

		g.addEdge(0, 1);
		assertTrue(g.hasEdge(0, 1));
		assertTrue(g.hasEdge(1, 0));
		assertEquals(1, g.M);

		g.addEdge(0, 1); // duplicate add
		assertEquals(1, g.M);

		g.addEdge(1, 2);
		g.addEdge(2, 3);
		assertEquals(3, g.M);

		g.removeEdge(1, 2);
		assertFalse(g.hasEdge(1, 2));
		assertFalse(g.hasEdge(2, 1));
		assertEquals(2, g.M);

		g.removeEdge(1, 2); // duplicate remove
		assertEquals(2, g.M);
	}

	@Test
	public void testMatrixConstructorValidation() {
		boolean[][] mat = {
			{false, true, false},
			{true, false, true},
			{false, true, false}
		};
		AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(mat);
		assertEquals(3, g.N);
		assertEquals(2, g.M);
		assertTrue(g.hasEdge(0, 1));
		assertTrue(g.hasEdge(1, 2));

		boolean[][] nonSymmetric = {
			{false, true},
			{false, false}
		};
		assertThrows(IllegalArgumentException.class, () -> new AdjacencyMatrixGraph(nonSymmetric));
	}

	@Test
	public void testDegreeAndBfs() {
		AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(4);
		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(1, 2);

		assertEquals(2, g.degree(0));
		assertEquals(2, g.degree(1));
		assertEquals(2, g.degree(2));
		assertEquals(0, g.degree(3));

		assertArrayEquals(new int[]{2, 2, 2, 0}, g.degrees());

		int INF = Integer.MAX_VALUE / 3;
		int[] distFrom0 = g.bfsDistances(0);
		assertArrayEquals(new int[]{0, 1, 1, INF}, distFrom0);
	}

	@Test
	public void testRestoreShortestPath() {
		AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(4);
		g.addEdge(0, 1);
		g.addEdge(1, 2);

		int[] dist = g.bfsDistances(0);
		assertArrayEquals(new int[]{0, 1, 2}, g.restoreShortestPath(dist, 2));
		assertArrayEquals(new int[]{0}, g.restoreShortestPath(dist, 0));
		assertNull(g.restoreShortestPath(dist, 3));
	}

	@Test
	public void testComplementToGraphCopyAndClosure() {
		AdjacencyMatrixGraph g = new AdjacencyMatrixGraph(3);
		g.addEdge(0, 1);

		AdjacencyMatrixGraph comp = g.complement();
		assertFalse(comp.hasEdge(0, 1));
		assertTrue(comp.hasEdge(0, 2));
		assertTrue(comp.hasEdge(1, 2));

		Graph listGraph = g.toGraph();
		assertEquals(3, listGraph.N);
		assertEquals(1, listGraph.M);

		AdjacencyMatrixGraph copyG = g.copy();
		assertEquals(g, copyG);
		assertEquals(g.hashCode(), copyG.hashCode());

		copyG.addEdge(1, 2);
		assertNotEquals(g, copyG);

		boolean[][] closure = copyG.transitiveClosure();
		assertTrue(closure[0][2]);
		assertTrue(closure[2][0]);
	}
}
