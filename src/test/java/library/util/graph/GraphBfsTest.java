package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class GraphBfsTest {

	private static final int INF = Integer.MAX_VALUE / 2;

	@Test
	public void testSimpleGraph() {
		// 0 - 1 - 2 - 3 - 4
		Graph g = new Graph(5);
		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(2, 3);
		g.addEdge(3, 4);

		int[] dist = new int[5];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		int[] res = g.bfs(new int[] { 0 }, dist, 2);
		assertArrayEquals(new int[] { 0, 1, 2 }, res);
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(2, dist[2]);
		assertEquals(INF, dist[3]);
		assertEquals(INF, dist[4]);

		// cutoff 0
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[] { 0 }, g.bfs(new int[] { 0 }, dist, 0));
		assertEquals(0, dist[0]);
		assertEquals(INF, dist[1]);

		// negative cutoff
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[0], g.bfs(new int[] { 0 }, dist, -1));

		// large cutoff
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, g.bfs(new int[] { 0 }, dist, 100));
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(2, dist[2]);
		assertEquals(3, dist[3]);
		assertEquals(4, dist[4]);
	}

	@Test
	public void testIntermediateBfsState() {
		// 0 - 1 - 2 - 3 - 4
		Graph g = new Graph(5);
		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(2, 3);
		g.addEdge(3, 4);

		int[] dist = new int[5];
		Arrays.fill(dist, INF);
		dist[1] = 5; // intermediate state where node 1 is at distance 5

		int[] res = g.bfs(new int[] { 1 }, dist, 7);
		assertArrayEquals(new int[] { 1, 0, 2, 3 }, res);
		assertEquals(6, dist[0]);
		assertEquals(5, dist[1]);
		assertEquals(6, dist[2]);
		assertEquals(7, dist[3]);
		assertEquals(INF, dist[4]);
	}

	@Test
	public void testRelaxationSkipsWhenAlreadySmallerOrEqual() {
		// 0 - 1 - 2
		Graph g = new Graph(3);
		g.addEdge(0, 1);
		g.addEdge(1, 2);

		int[] dist = new int[3];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		dist[1] = 1; // already 1 <= 0 + 1, so visiting 1 from 0 won't relax dist[1]
		dist[2] = INF;

		int[] res = g.bfs(new int[] { 0 }, dist, 2);
		// 0 is in sources, nexts from 0 is 1. Since dist[1] (1) is not > dist[0] + 1 (1), 1 is not visited!
		assertArrayEquals(new int[] { 0 }, res);
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(INF, dist[2]);
	}

	@Test
	public void testRelaxationUpdatesWhenLarger() {
		// 0 - 1
		Graph g = new Graph(2);
		g.addEdge(0, 1);

		int[] dist = new int[2];
		dist[0] = 2;
		dist[1] = 10; // larger than 2 + 1 = 3

		int[] res = g.bfs(new int[] { 0 }, dist, 5);
		assertArrayEquals(new int[] { 0, 1 }, res);
		assertEquals(2, dist[0]);
		assertEquals(3, dist[1]);
	}

	@Test
	public void testBranchingAndCycle() {
		//   1 - 2
		//  /     \
		// 0       4
		//  \     /
		//   3 --/
		Graph g = new Graph(5);
		g.addEdge(0, 1);
		g.addEdge(0, 3);
		g.addEdge(1, 2);
		g.addEdge(2, 4);
		g.addEdge(3, 4);

		int[] dist = new int[5];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		int[] res = g.bfs(new int[] { 0 }, dist, 1);
		assertArrayEquals(new int[] { 0, 1, 3 }, res);

		Arrays.fill(dist, INF);
		dist[0] = 0;
		res = g.bfs(new int[] { 0 }, dist, 2);
		assertArrayEquals(new int[] { 0, 1, 3, 2, 4 }, res);
	}

	@Test
	public void testMultiSources() {
		// 0 - 1   2 - 3 - 4
		Graph g = new Graph(5);
		g.addEdge(0, 1);
		g.addEdge(2, 3);
		g.addEdge(3, 4);

		int[] dist = new int[5];
		Arrays.fill(dist, INF);
		dist[0] = 3;
		dist[4] = 3;
		int[] res = g.bfs(new int[] { 0, 4 }, dist, 4);
		assertArrayEquals(new int[] { 0, 4, 1, 3 }, res);
		assertEquals(3, dist[0]);
		assertEquals(4, dist[1]);
		assertEquals(INF, dist[2]);
		assertEquals(4, dist[3]);
		assertEquals(3, dist[4]);

		// Inconsistent sources
		dist[0] = 2;
		dist[4] = 3;
		assertThrows(AssertionError.class, () -> g.bfs(new int[] { 0, 4 }, dist, 4));
	}

	@Test
	public void testEdgeCases() {
		Graph empty = new Graph(0);
		assertArrayEquals(new int[0], empty.bfs(new int[] { 0 }, new int[0], 5));

		Graph single = new Graph(1);
		int[] dist = new int[] { 0 };
		assertArrayEquals(new int[] { 0 }, single.bfs(new int[] { 0 }, dist, 5));
		assertEquals(0, dist[0]);

		// null/empty sources
		Graph g = new Graph(3);
		assertArrayEquals(new int[0], g.bfs(null, new int[3], 2));
		assertArrayEquals(new int[0], g.bfs(new int[0], new int[3], 2));
	}
}
