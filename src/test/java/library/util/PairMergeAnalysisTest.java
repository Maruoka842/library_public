package library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PairMergeAnalysisTest {

	@Test
	public void testIncompleteDfaThrowsException() {
		DFA dfa = new DFA(3, 2);
		dfa.addEdge(0, 1, 0);
		// Remaining transitions are left as -1
		assertThrows(IllegalStateException.class, () -> new PairMergeAnalysis(dfa));
	}

	@Test
	public void testSingleStateDfa() {
		DFA dfa = new DFA(1, 2);
		dfa.addEdge(0, 0, 0);
		dfa.addEdge(0, 0, 1);

		PairMergeAnalysis pma = new PairMergeAnalysis(dfa);
		assertTrue(pma.isSynchronizing());
		assertTrue(pma.canMerge(0, 0));
		assertEquals(0, pma.mergeDistance(0, 0));
		assertArrayEquals(new int[0], pma.restoreMergeWord(0, 0));
	}

	@Test
	public void testSynchronizingDfaCerny() {
		// Černý automaton C_4
		int n = 4;
		int k = 2;
		DFA dfa = new DFA(n, k);

		// Symbol 0: cyclic shift 0->1->2->3->0
		for (int u = 0; u < n; u++) {
			dfa.addEdge(u, (u + 1) % n, 0);
		}

		// Symbol 1: reset state 3 to 0, identity on others
		for (int u = 0; u < n; u++) {
			if (u == n - 1) {
				dfa.addEdge(u, 0, 1);
			} else {
				dfa.addEdge(u, u, 1);
			}
		}

		PairMergeAnalysis pma = new PairMergeAnalysis(dfa);
		assertTrue(pma.isSynchronizing());

		for (int u = 0; u < n; u++) {
			for (int v = 0; v < n; v++) {
				assertTrue(pma.canMerge(u, v));
				int dist = pma.mergeDistance(u, v);
				int[] word = pma.restoreMergeWord(u, v);

				assertNotNull(word);
				assertEquals(dist, word.length);

				int curU = u;
				int curV = v;
				for (int symbol : word) {
					curU = dfa.transite(curU, symbol);
					curV = dfa.transite(curV, symbol);
				}
				assertEquals(curU, curV);
			}
		}
	}

	@Test
	public void testNonSynchronizingPermutationDfa() {
		int n = 4;
		int k = 2;
		DFA dfa = new DFA(n, k);

		// Symbol 0: shift right
		for (int u = 0; u < n; u++) {
			dfa.addEdge(u, (u + 1) % n, 0);
		}
		// Symbol 1: shift left
		for (int u = 0; u < n; u++) {
			dfa.addEdge(u, (u + n - 1) % n, 1);
		}

		PairMergeAnalysis pma = new PairMergeAnalysis(dfa);
		assertFalse(pma.isSynchronizing());

		for (int u = 0; u < n; u++) {
			for (int v = 0; v < n; v++) {
				if (u == v) {
					assertTrue(pma.canMerge(u, v));
					assertEquals(0, pma.mergeDistance(u, v));
					assertArrayEquals(new int[0], pma.restoreMergeWord(u, v));
				} else {
					assertFalse(pma.canMerge(u, v));
					assertEquals(-1, pma.mergeDistance(u, v));
					assertNull(pma.restoreMergeWord(u, v));
				}
			}
		}
	}

	@Test
	public void testPartialMergeDfa() {
		DFA dfa = new DFA(3, 2);
		dfa.addEdge(0, 0, 0);
		dfa.addEdge(0, 0, 1);

		dfa.addEdge(1, 0, 0);
		dfa.addEdge(1, 2, 1);

		dfa.addEdge(2, 2, 0);
		dfa.addEdge(2, 2, 1);

		PairMergeAnalysis pma = new PairMergeAnalysis(dfa);
		assertFalse(pma.isSynchronizing());

		assertTrue(pma.canMerge(0, 1));
		assertEquals(1, pma.mergeDistance(0, 1));
		assertArrayEquals(new int[] {0}, pma.restoreMergeWord(0, 1));

		assertTrue(pma.canMerge(1, 2));
		assertEquals(1, pma.mergeDistance(1, 2));
		assertArrayEquals(new int[] {1}, pma.restoreMergeWord(1, 2));

		assertFalse(pma.canMerge(0, 2));
		assertEquals(-1, pma.mergeDistance(0, 2));
		assertNull(pma.restoreMergeWord(0, 2));
	}
}
