package library.util.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.Tree;

public class WaveletMatrixOfTreePathTest {
	@Test
	public void randomQueriesMatchBruteForce() {
		Random random = new Random(1);
		for (int iteration = 0; iteration < 30; iteration++) {
			int n = random.nextInt(15) + 1;
			Tree tree = new Tree(n);
			for (int v = 1; v < n; v++) tree.addEdge(v, random.nextInt(v));
			tree.rooted(random.nextInt(n));
			long[] value = random.longs(n, 0, 12).toArray();
			WaveletMatrixOfTreePath wm = new WaveletMatrixOfTreePath(tree, value);

			for (int u = 0; u < n; u++) for (int v = 0; v < n; v++) {
				List<Long> path = path(tree, value, u, v);
				checkQueries(wm, u, v, path);
			}
			for (int v = 0; v < n; v++) checkRootQueries(wm, v, path(tree, value, tree.root(), v));
		}
	}

	private static void checkQueries(WaveletMatrixOfTreePath wm, int u, int v, List<Long> path) {
		List<Long> sorted = new ArrayList<>(path);
		Collections.sort(sorted);
		assertEquals(path.size(), wm.pathLength(u, v));
		for (int k = 0; k < path.size(); k++) {
			assertEquals(path.get(k), wm.access(u, v, k));
			assertEquals(sorted.get(k), wm.kthSmallest(u, v, k));
			assertEquals(sorted.get(sorted.size() - 1 - k), wm.kthLargest(u, v, k));
		}
		assertEquals(sorted.get(0), wm.min(u, v));
		assertEquals(sorted.get(sorted.size() - 1), wm.max(u, v));
		for (long x = -1; x <= 13; x++) {
			long target = x;
			assertEquals(path.stream().filter(y -> y < target).count(), wm.countLess(u, v, x));
			assertEquals(path.stream().filter(y -> y <= target).count(), wm.countLeq(u, v, x));
			assertEquals(path.stream().filter(y -> y == target).count(), wm.count(u, v, x));
			assertEquals(sorted.stream().filter(y -> y < target).reduce((a, b) -> b).orElse(-1L), wm.predecessor(u, v, x));
			assertEquals(sorted.stream().filter(y -> y >= target).findFirst().orElse(-1L), wm.successor(u, v, x));
			for (int r = 0; r <= path.size(); r++) {
				assertEquals(path.subList(0, r).stream().filter(y -> y == target).count(), wm.rank(u, v, r, x));
			}
			List<Integer> positions = positions(path, x);
			for (int k = 0; k <= positions.size(); k++) {
				assertEquals(k < positions.size() ? positions.get(k) : -1, wm.select(u, v, x, k));
			}
		}
		assertEquals(path.stream().filter(x -> 3 <= x && x < 9).count(), wm.countRange(u, v, 3, 9));
	}

	private static void checkRootQueries(WaveletMatrixOfTreePath wm, int v, List<Long> path) {
		List<Long> sorted = new ArrayList<>(path);
		Collections.sort(sorted);
		assertEquals(path.size(), wm.pathLength(v));
		for (int k = 0; k < path.size(); k++) {
			assertEquals(path.get(k), wm.access(v, k));
			assertEquals(sorted.get(k), wm.kthSmallest(v, k));
			assertEquals(sorted.get(sorted.size() - 1 - k), wm.kthLargest(v, k));
		}
		assertEquals(sorted.get(0), wm.min(v));
		assertEquals(sorted.get(sorted.size() - 1), wm.max(v));
		for (long x = -1; x <= 13; x++) {
			long target = x;
			assertEquals(path.stream().filter(y -> y < target).count(), wm.countLess(v, x));
			assertEquals(path.stream().filter(y -> y <= target).count(), wm.countLeq(v, x));
			assertEquals(path.stream().filter(y -> y == target).count(), wm.count(v, x));
			assertEquals(sorted.stream().filter(y -> y < target).reduce((a, b) -> b).orElse(-1L), wm.predecessor(v, x));
			assertEquals(sorted.stream().filter(y -> y >= target).findFirst().orElse(-1L), wm.successor(v, x));
			for (int r = 0; r <= path.size(); r++) {
				assertEquals(path.subList(0, r).stream().filter(y -> y == target).count(), wm.rank(v, r, x));
			}
			List<Integer> positions = positions(path, x);
			for (int k = 0; k <= positions.size(); k++) {
				assertEquals(k < positions.size() ? positions.get(k) : -1, wm.select(v, x, k));
			}
		}
		assertEquals(path.stream().filter(x -> 3 <= x && x < 9).count(), wm.countRange(v, 3, 9));
	}

	private static List<Integer> positions(List<Long> path, long x) {
		List<Integer> ret = new ArrayList<>();
		for (int i = 0; i < path.size(); i++) if (path.get(i) == x) ret.add(i);
		return ret;
	}

	private static List<Long> path(Tree tree, long[] value, int u, int v) {
		int lca = tree.lca(u, v);
		List<Long> ret = new ArrayList<>(), suffix = new ArrayList<>();
		for (int x = u; x != lca; x = tree.parent(x)) ret.add(value[x]);
		ret.add(value[lca]);
		for (int x = v; x != lca; x = tree.parent(x)) suffix.add(value[x]);
		Collections.reverse(suffix);
		ret.addAll(suffix);
		return ret;
	}
}
