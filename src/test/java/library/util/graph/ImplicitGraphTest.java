package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ImplicitGraphTest {

	@Test
	public void testDist() {
		ImplicitGraph<Integer> graph = new ImplicitGraph<Integer>() {
			@Override
			public Iterable<Integer> nextStates(Integer v) {
				List<Integer> nexts = new ArrayList<>();
				if (v > 0) nexts.add(v - 1);
				if (v < 20) nexts.add(v + 1);
				return nexts;
			}
		};

		assertEquals(0, graph.dist(7, 7));
		assertEquals(17, graph.dist(1, 18));
		assertEquals(0, graph.dist(7, 7, 0));
		assertEquals(17, graph.dist(1, 18, 17));
		assertEquals(-1, graph.dist(1, 18, 16));
		assertEquals(-1, graph.dist(7, 7, -1));
	}

	@Test
	public void testDistForDisconnectedVertices() {
		ImplicitGraph<Integer> graph = new ImplicitGraph<Integer>() {
			@Override
			public Iterable<Integer> nextStates(Integer v) {
				if (v == 0) return List.of(1);
				if (v == 1) return List.of(0);
				if (v == 2) return List.of(3);
				if (v == 3) return List.of(2);
				return List.of();
			}
		};

		assertEquals(-1, graph.dist(0, 3));
		assertEquals(-1, graph.dist(0, 3, 100));
	}

	@Test
	public void testBasicSearchMethods() {
		ImplicitGraph<Integer> graph = new ImplicitGraph<Integer>() {
			@Override
			public Iterable<Integer> nextStates(Integer v) {
				if (v == 0) return List.of(1, 2);
				if (v == 1) return List.of(0, 3);
				if (v == 2) return List.of(0, 3);
				if (v == 3) return List.of(1, 2, 4);
				if (v == 4) return List.of(3);
				return List.of();
			}
		};

		assertEquals(4, graph.path(0, 4).size());
		assertEquals(null, graph.path(0, 4, 2));
		assertEquals(3, graph.path(0, 4, 3).size() - 1);
		assertEquals(3, graph.bfsDistances(0, 1).size());
		assertEquals(1, graph.bfsDistances(Arrays.asList(0, 4)).get(3));
		assertEquals(true, graph.isConnected(0, 4));
		assertEquals(false, graph.isConnected(0, 4, 2));
		assertEquals(5, graph.reachables(0).size());
		assertEquals(3, graph.countReachable(0, 1));
		assertEquals(3, graph.degree(3));
		assertEquals(5, graph.bfsOrder(0).size());
		assertEquals(3, graph.bfsOrder(0, 1).size());
	}
}
