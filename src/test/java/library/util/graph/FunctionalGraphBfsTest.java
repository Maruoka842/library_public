package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FunctionalGraphBfsTest {

	private static final int INF = Integer.MAX_VALUE / 2;

	@Test
	public void testSimpleLine() {
		// 0 -> 1 -> 2 -> 3 -> 3 (self loop at 3)
		int[] p = new int[] { 1, 2, 3, 3 };
		FunctionalGraph fg = new FunctionalGraph(p);

		int[] dist = new int[4];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		int[] res = fg.bfs(new int[] { 0 }, dist, 2);
		assertArrayEquals(new int[] { 0, 1, 2 }, res);
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(2, dist[2]);
		assertEquals(INF, dist[3]);

		// cutoff 0
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[] { 0 }, fg.bfs(new int[] { 0 }, dist, 0));
		assertEquals(0, dist[0]);
		assertEquals(INF, dist[1]);

		// cutoff negative
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[0], fg.bfs(new int[] { 0 }, dist, -1));

		// cutoff large reaches end of trajectory
		Arrays.fill(dist, INF);
		dist[0] = 0;
		assertArrayEquals(new int[] { 0, 1, 2, 3 }, fg.bfs(new int[] { 0 }, dist, 10));
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(2, dist[2]);
		assertEquals(3, dist[3]);
	}

	@Test
	public void testIntermediateBfsState() {
		// 0 -> 1 -> 2 -> 3 -> 3
		int[] p = new int[] { 1, 2, 3, 3 };
		FunctionalGraph fg = new FunctionalGraph(p);

		int[] dist = new int[4];
		Arrays.fill(dist, INF);
		dist[1] = 10;

		int[] res = fg.bfs(new int[] { 1 }, dist, 12);
		assertArrayEquals(new int[] { 1, 2, 3 }, res);
		assertEquals(INF, dist[0]);
		assertEquals(10, dist[1]);
		assertEquals(11, dist[2]);
		assertEquals(12, dist[3]);
	}

	@Test
	public void testRelaxationSkipsWhenAlreadySmallerOrEqual() {
		// 0 -> 1 -> 2 -> 2
		int[] p = new int[] { 1, 2, 2 };
		FunctionalGraph fg = new FunctionalGraph(p);

		int[] dist = new int[3];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		dist[1] = 1; // already 1 <= 0 + 1, so visiting 1 from 0 won't relax dist[1]

		int[] res = fg.bfs(new int[] { 0 }, dist, 2);
		assertArrayEquals(new int[] { 0 }, res);
		assertEquals(0, dist[0]);
		assertEquals(1, dist[1]);
		assertEquals(INF, dist[2]);
	}

	@Test
	public void testCycle() {
		// 0 -> 1 -> 2 -> 0 (cycle of 3 nodes)
		int[] p = new int[] { 1, 2, 0 };
		FunctionalGraph fg = new FunctionalGraph(p);

		int[] dist = new int[3];
		Arrays.fill(dist, INF);
		dist[0] = 0;
		int[] res = fg.bfs(new int[] { 0 }, dist, 1);
		assertArrayEquals(new int[] { 0, 1 }, res);

		Arrays.fill(dist, INF);
		dist[0] = 0;
		res = fg.bfs(new int[] { 0 }, dist, 2);
		assertArrayEquals(new int[] { 0, 1, 2 }, res);

		Arrays.fill(dist, INF);
		dist[0] = 0;
		res = fg.bfs(new int[] { 0 }, dist, 5);
		// After visiting 0, 1, 2:
		// from 2, parent[2] = 0, but dist[0] = 0 <= dist[2] + 1 (3), so it stops.
		assertArrayEquals(new int[] { 0, 1, 2 }, res);
	}

	@Test
	public void testMultiSources() {
		// 0 -> 1 -> 2 -> 2
		// 3 -> 4 -> 2
		int[] p = new int[] { 1, 2, 2, 4, 2 };
		FunctionalGraph fg = new FunctionalGraph(p);

		int[] dist = new int[5];
		Arrays.fill(dist, INF);
		dist[0] = 4;
		dist[3] = 4;
		int[] res = fg.bfs(new int[] { 0, 3 }, dist, 5);
		assertArrayEquals(new int[] { 0, 3, 1, 4 }, res);
		assertEquals(4, dist[0]);
		assertEquals(5, dist[1]);
		assertEquals(INF, dist[2]);
		assertEquals(4, dist[3]);
		assertEquals(5, dist[4]);

		// Inconsistent sources
		dist[0] = 4;
		dist[3] = 5;
		assertThrows(AssertionError.class, () -> fg.bfs(new int[] { 0, 3 }, dist, 5));
	}

	@Test
	public void testEdgeCases() {
		FunctionalGraph empty = new FunctionalGraph(new int[0]);
		assertArrayEquals(new int[0], empty.bfs(new int[] { 0 }, new int[0], 5));

		FunctionalGraph single = new FunctionalGraph(new int[] { 0 });
		int[] dist = new int[] { 0 };
		assertArrayEquals(new int[] { 0 }, single.bfs(new int[] { 0 }, dist, 5));
		assertEquals(0, dist[0]);

		// null/empty sources
		FunctionalGraph fg = new FunctionalGraph(new int[] { 1, 0 });
		assertArrayEquals(new int[0], fg.bfs(null, new int[2], 2));
		assertArrayEquals(new int[0], fg.bfs(new int[0], new int[2], 2));
	}
}
