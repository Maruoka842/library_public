package library.util.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.Tree;

public class DominatorTreeTest {

	// Brute-force helper to compute whether u dominates v from s
	private static boolean bruteForceDominates(Digraph g, int s, int u, int v) {
		if (u == v) return true;
		// Check if v is reachable from s without using u
		boolean[] vis = new boolean[g.N];
		Queue<Integer> q = new ArrayDeque<>();
		if (s != u) {
			vis[s] = true;
			q.add(s);
		}
		while (!q.isEmpty()) {
			int curr = q.poll();
			for (int next : g.adj[curr]) {
				if (next != u && !vis[next]) {
					vis[next] = true;
					q.add(next);
				}
			}
		}
		return !vis[v];
	}

	// Brute-force helper to check reachability from s
	private static boolean bruteForceReachable(Digraph g, int s, int v) {
		boolean[] vis = new boolean[g.N];
		Queue<Integer> q = new ArrayDeque<>();
		vis[s] = true;
		q.add(s);
		while (!q.isEmpty()) {
			int curr = q.poll();
			for (int next : g.adj[curr]) {
				if (!vis[next]) {
					vis[next] = true;
					q.add(next);
				}
			}
		}
		return vis[v];
	}

	@Test
	public void testSimpleDAG() {
		// Example graph:
		// 0 -> 1 -> 3
		// 0 -> 2 -> 3
		// 3 -> 4
		Digraph g = new Digraph(5);
		g.addEdge(0, 1);
		g.addEdge(0, 2);
		g.addEdge(1, 3);
		g.addEdge(2, 3);
		g.addEdge(3, 4);

		DominatorTree dom = new DominatorTree(g, 0);

		assertEquals(0, dom.idom(0));
		assertEquals(0, dom.idom(1));
		assertEquals(0, dom.idom(2));
		assertEquals(0, dom.idom(3));
		assertEquals(3, dom.idom(4));

		assertTrue(dom.isDominator(0, 4));
		assertTrue(dom.isDominator(3, 4));
		assertFalse(dom.isDominator(1, 4));
		assertFalse(dom.isDominator(2, 4));

		assertArrayEquals(new int[] {0, 3, 4}, dom.getDominators(4));
	}

	@Test
	public void testDisconnectedAndUnreachable() {
		Digraph g = new Digraph(6);
		g.addEdge(0, 1);
		g.addEdge(1, 2);
		g.addEdge(3, 4);
		g.addEdge(4, 5);

		DominatorTree dom = new DominatorTree(g, 0);

		assertTrue(dom.isReachable(0));
		assertTrue(dom.isReachable(1));
		assertTrue(dom.isReachable(2));
		assertFalse(dom.isReachable(3));
		assertFalse(dom.isReachable(4));
		assertFalse(dom.isReachable(5));

		assertEquals(0, dom.idom(0));
		assertEquals(0, dom.idom(1));
		assertEquals(1, dom.idom(2));
		assertEquals(-1, dom.idom(3));
		assertEquals(-1, dom.idom(4));

		assertFalse(dom.isDominator(3, 4));
		assertArrayEquals(new int[0], dom.getDominators(3));

		Tree dt = dom.dominatorTree();
		assertNotNull(dt);
		assertEquals(6, dt.N);
	}

	@Test
	public void testSingleVertex() {
		Digraph g = new Digraph(1);
		DominatorTree dom = new DominatorTree(g, 0);

		assertEquals(0, dom.idom(0));
		assertTrue(dom.isReachable(0));
		assertTrue(dom.isDominator(0, 0));
		assertArrayEquals(new int[] {0}, dom.getDominators(0));
	}

	@Test
	public void testCycleAndSelfLoop() {
		Digraph g = new Digraph(4);
		g.addEdge(0, 1);
		g.addEdge(1, 1); // self-loop
		g.addEdge(1, 2);
		g.addEdge(2, 1); // cycle (1 <-> 2)
		g.addEdge(2, 3);

		DominatorTree dom = new DominatorTree(g, 0);

		assertEquals(0, dom.idom(0));
		assertEquals(0, dom.idom(1));
		assertEquals(1, dom.idom(2));
		assertEquals(2, dom.idom(3));

		assertArrayEquals(new int[] {0, 1, 2, 3}, dom.getDominators(3));
	}

	@Test
	public void testRandomGraphsVsBruteForce() {
		Random rnd = new Random(42);
		int tests = 20;

		for (int t = 0; t < tests; t++) {
			int n = rnd.nextInt(25) + 2;
			int m = rnd.nextInt(n * 3);
			Digraph g = new Digraph(n);
			for (int i = 0; i < m; i++) {
				int u = rnd.nextInt(n);
				int v = rnd.nextInt(n);
				g.addEdge(u, v);
			}

			int s = rnd.nextInt(n);
			DominatorTree dom = new DominatorTree(g, s);

			for (int v = 0; v < n; v++) {
				boolean reachable = bruteForceReachable(g, s, v);
				assertEquals(reachable, dom.isReachable(v), "Reachable mismatch at v=" + v);

				if (!reachable) {
					assertEquals(-1, dom.idom(v));
				} else if (v == s) {
					assertEquals(s, dom.idom(v));
				} else {
					int expectedIdom = -1;
					for (int u = 0; u < n; u++) {
						if (u == v) continue;
						if (bruteForceReachable(g, s, u) && bruteForceDominates(g, s, u, v)) {
							// Check if u is the immediate dominator: no other dominator d (d != u, d != v) dominates u
							boolean isImmediate = true;
							for (int d = 0; d < n; d++) {
								if (d == u || d == v) continue;
								if (bruteForceReachable(g, s, d) && bruteForceDominates(g, s, d, v)) {
									// d dominates v. If u dominates d, then d is closer to v than u.
									if (bruteForceDominates(g, s, u, d)) {
										isImmediate = false;
										break;
									}
								}
							}
							if (isImmediate) {
								expectedIdom = u;
								break;
							}
						}
					}
					assertEquals(expectedIdom, dom.idom(v), "idom mismatch at v=" + v + " for graph test " + t);
				}

				for (int u = 0; u < n; u++) {
					if (reachable && bruteForceReachable(g, s, u)) {
						boolean expectedDom = bruteForceDominates(g, s, u, v);
						assertEquals(expectedDom, dom.isDominator(u, v), "isDominator mismatch u=" + u + " v=" + v);
					}
				}
			}
		}
	}
}
