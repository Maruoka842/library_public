package library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.collections.LeafyBinaryTrieFromTopBits;

public class LeafyBinaryTrieFromTopBitsTest {
    @Test
    public void testKthSmallestRandom() {
        Random rnd = new Random(42);
        LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(16);
        ArrayList<Long> expected = new ArrayList<>();

        for (int t = 0; t < 5000; t++) {
            int op = rnd.nextInt(3);
            if (op == 0) {
                long v = rnd.nextInt(1 << 16);
                trie.add(v);
                expected.add(v);
            } else if (op == 1) {
                long v = rnd.nextInt(1 << 16);
                boolean got = trie.remove(v);
                boolean want = expected.remove(v);
                assertEquals(want, got, "remove mismatch at step=" + t + " value=" + v);
            } else {
                Collections.sort(expected);
                assertEquals(expected.size(), trie.size(), "size mismatch at step=" + t);
                for (int k = 0; k < expected.size(); k++) {
                    assertEquals(expected.get(k).longValue(), trie.kthSmallest(k),
                            "kth mismatch at step=" + t + " k=" + k);
                }
                assertThrows(AssertionError.class, () -> trie.kthSmallest(-1));
                assertThrows(AssertionError.class, () -> trie.kthSmallest(trie.size()));
            }
        }
    }

    @Test
    public void testXorKthSmallestRandom() {
        Random rnd = new Random(123);
        LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(16);
        ArrayList<Long> values = new ArrayList<>();

        for (int t = 0; t < 3000; t++) {
            int op = rnd.nextInt(3);
            if (op == 0) {
                long v = rnd.nextInt(1 << 16);
                trie.add(v);
                values.add(v);
            } else if (op == 1) {
                long v = rnd.nextInt(1 << 16);
                boolean got = trie.remove(v);
                boolean want = values.remove(v);
                assertEquals(want, got, "remove mismatch at step=" + t + " value=" + v);
            } else if (!values.isEmpty()) {
                long xorValue = rnd.nextInt(1 << 16);
                ArrayList<Long> expected = new ArrayList<>();
                for (long x : values) expected.add(x ^ xorValue);
                Collections.sort(expected);
                for (int k = 0; k < expected.size(); k++) {
                    assertEquals(expected.get(k).longValue(), trie.xorKthSmallest(xorValue, k),
                            "xorKth mismatch at step=" + t + " k=" + k + " xorValue=" + xorValue);
                }
                assertThrows(AssertionError.class, () -> trie.xorKthSmallest(xorValue, -1));
                assertThrows(AssertionError.class, () -> trie.xorKthSmallest(xorValue, trie.size()));
            }
        }
    }

    @Test
    public void testSumMinXorRandom() {
        Random rnd = new Random(555);
        for (int trial = 0; trial < 10; trial++) {
            int bitLength = 10;
            LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(bitLength);
            int n = rnd.nextInt(50) + 1;
            long[] vals = new long[n];
            for (int i = 0; i < n; i++) {
                vals[i] = rnd.nextInt(1 << bitLength);
                trie.add(vals[i]);
            }

            int numQueries = 100;
            long maxVal = 1L << bitLength;
            for (int q = 0; q < numQueries; q++) {
                long l = rnd.nextInt((int) maxVal + 10);
                long r = rnd.nextInt((int) maxVal + 10);
                if (l > r) {
                    long tmp = l; l = r; r = tmp;
                }

                long expectedSum = 0;
                for (long i = l; i < r; i++) {
                    long minXor = Long.MAX_VALUE;
                    for (long v : vals) {
                        minXor = Math.min(minXor, i ^ v);
                    }
                    if (minXor != Long.MAX_VALUE) {
                        expectedSum += minXor;
                    }
                }

                long gotSum = trie.sumMinXor(l, r);
                assertEquals(expectedSum, gotSum, "sumMinXor mismatch for l=" + l + " r=" + r + " trial=" + trial);
            }
        }
    }

    @Test
    public void testSumMinXorEdgeCases() {
        LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(8);
        assertEquals(0, trie.sumMinXor(0, 10));

        trie.add(5);
        assertEquals(0, trie.sumMinXor(10, 5));
        assertEquals(0, trie.sumMinXor(5, 5));

        // Single element 5 in range [0, 8)
        // i ^ 5 for i = 0..7
        long expected = 0;
        for (int i = 0; i < 8; i++) expected += (i ^ 5);
        assertEquals(expected, trie.sumMinXor(0, 8));

        trie.add(10);
        // Elements {5, 10}
        expected = 0;
        for (int i = 3; i < 12; i++) {
            expected += Math.min(i ^ 5, i ^ 10);
        }
        assertEquals(expected, trie.sumMinXor(3, 12));
    }

    @Test
    public void testSumMinXorWithRemove() {
        Random rnd = new Random(777);
        LeafyBinaryTrieFromTopBits trie = new LeafyBinaryTrieFromTopBits(10);
        ArrayList<Long> vals = new ArrayList<>();

        for (int t = 0; t < 1000; t++) {
            int op = rnd.nextInt(3);
            if (op == 0) {
                long v = rnd.nextInt(1 << 10);
                trie.add(v);
                vals.add(v);
            } else if (op == 1 && !vals.isEmpty()) {
                long v = vals.remove(rnd.nextInt(vals.size()));
                trie.remove(v);
            } else if (!vals.isEmpty()) {
                long l = rnd.nextInt(1 << 10);
                long r = rnd.nextInt(1 << 10);
                if (l > r) { long tmp = l; l = r; r = tmp; }

                long expectedSum = 0;
                for (long i = l; i < r; i++) {
                    long minXor = Long.MAX_VALUE;
                    for (long v : vals) {
                        minXor = Math.min(minXor, i ^ v);
                    }
                    expectedSum += minXor;
                }
                assertEquals(expectedSum, trie.sumMinXor(l, r), "Mismatch at step " + t);
            }
        }
    }
}
