package library.util.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdjacencyMatrixDigraphTest {

	@Test
	public void testConstructorAndEdgeOperations() {
		AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(3);
		assertEquals(3, g.N);
		assertEquals(0, g.M);

		g.addEdge(0, 1);
		assertTrue(g.hasEdge(0, 1));
		assertFalse(g.hasEdge(1, 0));
		assertEquals(1, g.M);

		g.addEdge(0, 1); // duplicate
		assertEquals(1, g.M);

		g.addEdge(1, 2);
		assertEquals(2, g.M);

		g.removeEdge(0, 1);
		assertFalse(g.hasEdge(0, 1));
		assertEquals(1, g.M);
	}

	@Test
	public void testDegreesAndBfs() {
		AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(3);
		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(1, 2);

		assertEquals(2, g.outDegree(0));
		assertEquals(0, g.inDegree(0));
		assertEquals(2, g.inDegree(2));

		assertArrayEquals(new int[]{2, 1, 0}, g.outDegrees());
		assertArrayEquals(new int[]{0, 1, 2}, g.inDegrees());

		int INF = Integer.MAX_VALUE / 3;
		assertArrayEquals(new int[]{0, 1, 1}, g.bfsDistances(0));
		assertArrayEquals(new int[]{INF, 0, 1}, g.bfsDistances(1));
	}

	@Test
	public void testRestoreShortestPath() {
		AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(4);
		g.addEdge(0, 1);
		g.addEdge(1, 2);

		int[] dist = g.bfsDistances(0);
		assertArrayEquals(new int[]{0, 1, 2}, g.restoreShortestPath(dist, 2));
		assertArrayEquals(new int[]{0}, g.restoreShortestPath(dist, 0));
		assertNull(g.restoreShortestPath(dist, 3));
	}

	@Test
	public void testReverseComplementAndToDigraph() {
		AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(3);
		g.addEdge(0, 1);

		AdjacencyMatrixDigraph rev = g.reverse();
		assertFalse(rev.hasEdge(0, 1));
		assertTrue(rev.hasEdge(1, 0));

		AdjacencyMatrixDigraph comp = g.complement();
		assertFalse(comp.hasEdge(0, 1));
		assertTrue(comp.hasEdge(0, 2));
		assertTrue(comp.hasEdge(1, 0));
		assertTrue(comp.hasEdge(1, 2));
		assertTrue(comp.hasEdge(2, 0));
		assertTrue(comp.hasEdge(2, 1));

		Digraph listDigraph = g.toDigraph();
		assertEquals(3, listDigraph.N);
		assertEquals(1, listDigraph.M);
	}

	@Test
	public void testTopologicalSortAndIsDAG() {
		AdjacencyMatrixDigraph dag = new AdjacencyMatrixDigraph(3);
		dag.addEdge(0, 1);
		dag.addEdge(1, 2);

		assertTrue(dag.isDAG());
		assertArrayEquals(new int[]{0, 1, 2}, dag.topologicalOrder());

		AdjacencyMatrixDigraph cycle = new AdjacencyMatrixDigraph(3);
		cycle.addEdge(0, 1);
		cycle.addEdge(1, 2);
		cycle.addEdge(2, 0);

		assertFalse(cycle.isDAG());
		assertNull(cycle.topologicalOrder());
	}

	@Test
	public void testCopyEqualsHashCodeAndClosure() {
		AdjacencyMatrixDigraph g = new AdjacencyMatrixDigraph(3);
		g.addEdge(0, 1);
		g.addEdge(1, 2);

		AdjacencyMatrixDigraph copy = g.copy();
		assertEquals(g, copy);
		assertEquals(g.hashCode(), copy.hashCode());

		copy.removeEdge(0, 1);
		assertNotEquals(g, copy);

		boolean[][] closure = g.transitiveClosure();
		assertTrue(closure[0][2]);
		assertFalse(closure[2][0]);
	}
}
