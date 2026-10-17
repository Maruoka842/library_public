package library.util.seq;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PQTreeTest {
    /** 個数の厳密値、合成数の法、long の積の桁あふれ、状態保持を検証する。計算量: 時間・領域 O(1)。 */
    // 未テスト
    @Test
    void countPermutationsEdgeCasesAndLargeCounts() {
        for (int n = 0; n <= 1; n++) {
            PQTree tree = new PQTree(n);
            assertEquals(BigInteger.ONE, tree.countPermutations());
            assertEquals(1, tree.countPermutations(7));
            assertEquals(0, tree.countPermutations(1));
        }
        PQTree tree = new PQTree(6);
        assertEquals(BigInteger.valueOf(720), tree.countPermutations());
        assertTrue(tree.reduce(0, 1));
        assertEquals(BigInteger.valueOf(240), tree.countPermutations());
        assertTrue(tree.reduce(1, 2));
        assertEquals(BigInteger.valueOf(48), tree.countPermutations());
        assertTrue(tree.reduce(3, 4));
        assertEquals(BigInteger.valueOf(24), tree.countPermutations());
        PQTree.Node root = tree.getRoot();
        int[] frontier = tree.frontier();
        assertEquals(0, tree.countPermutations(12));
        assertEquals(4, tree.countPermutations(10));
        assertFalse(tree.reduce(0, 2));
        assertEquals(BigInteger.valueOf(24), tree.countPermutations());
        assertThrows(IllegalArgumentException.class, () -> tree.countPermutations(0));
        assertThrows(IllegalArgumentException.class, () -> tree.countPermutations(-1));
        assertSame(root, tree.getRoot());
        assertArrayEquals(frontier, tree.frontier());
        BigInteger factorial = BigInteger.ONE;
        for (int n = 2; n <= 100; n++) {
            factorial = factorial.multiply(BigInteger.valueOf(n));
            PQTree universal = new PQTree(n);
            assertEquals(factorial, universal.countPermutations());
            for (long mod : new long[] {1, 2, 12, 998244353, Long.MAX_VALUE, Long.MAX_VALUE - 1}) {
                assertEquals(factorial.mod(BigInteger.valueOf(mod)).longValue(), universal.countPermutations(mod));
            }
        }
    }

    /** 空集合、重複、範囲外入力と失敗時の保存を検証する。計算量: 時間・領域 O(1)。 */
    // 未テスト
    @Test
    void edgeCasesAndRollback() {
        assertThrows(IllegalArgumentException.class, () -> new PQTree(-1));
        PQTree empty = new PQTree(0);
        assertNull(empty.getRoot());
        assertEquals(0, empty.size());
        assertTrue(empty.reduce());
        assertArrayEquals(new int[0], empty.frontier());
        assertTrue(empty.contains(new int[0]));
        PQTree singleton = new PQTree(1);
        assertTrue(singleton.reduce(0, 0));
        assertArrayEquals(new int[] {0}, singleton.frontier());
        PQTree tree = new PQTree(3);
        assertTrue(tree.reduce(0, 1, 1));
        assertTrue(tree.reduce(1, 2));
        PQTree.Node snapshot = tree.getRoot();
        int[] before = tree.frontier();
        assertFalse(tree.reduce(0, 2));
        assertSame(snapshot, tree.getRoot());
        assertArrayEquals(before, tree.frontier());
        assertTrue(tree.contains(new int[] {0, 1, 2}));
        assertTrue(tree.contains(new int[] {2, 1, 0}));
        assertFalse(tree.contains(new int[] {0, 2, 1}));
        assertFalse(tree.contains(new int[] {0, 0, 2}));
        assertFalse(tree.contains(new int[] {0, 1, 3}));
        assertFalse(tree.contains(new int[] {0, 1}));
        assertThrows(IllegalArgumentException.class, () -> tree.reduce(-1));
        assertThrows(IllegalArgumentException.class, () -> tree.reduce(3));
        assertThrows(NullPointerException.class, () -> tree.reduce((int[]) null));
        assertThrows(NullPointerException.class, () -> tree.contains(null));
        assertSame(snapshot, tree.getRoot());
        assertTrue(tree.reduce(0, 1, 2));
        assertTrue(tree.reduce());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.children.clear());
    }

    /** n = 4 の全ての三制約列を全順列と照合する。計算量: 時間 O(2^(3n) n! n)、領域 O(n! n)。 */
    // 未テスト
    @Test
    void exhaustiveThreeConstraints() {
        List<int[]> permutations = permutations(4);
        for (int a = 0; a < 16; a++) for (int b = 0; b < 16; b++) for (int c = 0; c < 16; c++) {
            PQTree tree = new PQTree(4);
            List<int[]> allowed = permutations;
            for (int mask : new int[] {a, b, c}) allowed = checkReduction(tree, allowed, permutations, mask);
        }
    }

    /** 固定乱数の制約列を全順列と照合する。計算量: 時間 O(t r n! n)、領域 O(n! n)、t=100,r=12,n&lt;=6。 */
    // 未テスト
    @Test
    void randomConstraintsAgainstAllPermutations() {
        Random random = new Random(20261001);
        for (int trial = 0; trial < 100; trial++) {
            int n = 2 + random.nextInt(5);
            List<int[]> permutations = permutations(n);
            List<int[]> allowed = permutations;
            PQTree tree = new PQTree(n);
            for (int round = 0; round < 12; round++) {
                allowed = checkReduction(tree, allowed, permutations, random.nextInt(1 << n));
            }
        }
    }

    /** 解を持つ制約列を全順列と照合する。計算量: 時間 O(t r n! n)、領域 O(n! n)、t=30,r=20,n=6。 */
    // 未テスト
    @Test
    void feasibleConstraintsAgainstAllPermutations() {
        Random random = new Random(715);
        List<int[]> all = permutations(6);
        for (int trial = 0; trial < 30; trial++) {
            int[] witness = all.get(random.nextInt(all.size()));
            PQTree tree = new PQTree(6);
            PQTree.Node initial = tree.getRoot();
            List<int[]> allowed = all;
            for (int round = 0; round < 20; round++) {
                int left = random.nextInt(6), right = left + 1 + random.nextInt(6 - left);
                int mask = 0;
                for (int i = left; i < right; i++) mask |= 1 << witness[i];
                allowed = checkReduction(tree, allowed, all, mask);
                assertTrue(tree.contains(witness));
            }
            assertEquals(PQTree.NodeType.P, initial.type);
            assertEquals(6, initial.children.size());
            int[] copy = tree.frontier();
            Arrays.fill(copy, -1);
            assertTrue(tree.contains(tree.frontier()));
        }
    }

    /** 入れ子の制約と多数の葉を検証する。計算量: 時間 O(n^2)、領域 O(n)、n=2000。 */
    // 未テスト
    @Test
    void deepTreeAndLargeUniverse() {
        int n = 2000;
        PQTree deep = new PQTree(n);
        for (int i = 0; i + 1 < n; i++) assertTrue(deep.reduce(i, i + 1));
        assertTrue(deep.contains(deep.frontier()));
        assertEquals(BigInteger.TWO, deep.countPermutations());
        assertEquals(2, deep.countPermutations(Long.MAX_VALUE));
        PQTree nested = new PQTree(n);
        for (int k = 2; k <= n; k++) {
            int[] set = new int[k];
            for (int i = 0; i < k; i++) set[i] = i;
            assertTrue(nested.reduce(set));
        }
        assertTrue(nested.contains(nested.frontier()));
        BigInteger nestedCount = BigInteger.ONE.shiftLeft(n - 1);
        assertEquals(nestedCount, nested.countPermutations());
        assertEquals(nestedCount.mod(BigInteger.valueOf(998244353)).longValue(), nested.countPermutations(998244353));
        PQTree large = new PQTree(100000);
        assertTrue(large.reduce(0, 99999));
        assertTrue(large.reduce(1, 99999));
        assertTrue(large.contains(large.frontier()));
    }

    /**
     * mask の制約の成否、全順列の所属、frontier、失敗時の根を照合し、許される順列を返す。
     * 計算量: 時間 O(n! n)、追加領域 O(n! + n)。
     */
    // 未テスト
    private static List<int[]> checkReduction(PQTree tree, List<int[]> allowed, List<int[]> all, int mask) {
        int n = tree.size();
        int[] elements = new int[Integer.bitCount(mask)];
        int index = 0;
        for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) elements[index++] = i;
        List<int[]> next = new ArrayList<>();
        for (int[] permutation : allowed) if (consecutive(permutation, mask)) next.add(permutation);
        PQTree.Node before = tree.getRoot();
        boolean success = tree.reduce(elements);
        assertEquals(!next.isEmpty(), success, "mask=" + mask);
        if (!success) { assertSame(before, tree.getRoot()); next = allowed; }
        assertEquals(BigInteger.valueOf(next.size()), tree.countPermutations(), "mask=" + mask);
        for (long mod : new long[] {1, 2, 6, 7, 998244353, Long.MAX_VALUE}) {
            assertEquals(next.size() % mod, tree.countPermutations(mod), "mask=" + mask + " mod=" + mod);
        }
        IdentityHashMap<int[], Boolean> membership = new IdentityHashMap<>();
        for (int[] permutation : next) membership.put(permutation, true);
        for (int[] permutation : all) {
            boolean expected = membership.containsKey(permutation);
            assertEquals(expected, tree.contains(permutation), "mask=" + mask + " p=" + Arrays.toString(permutation));
        }
        int[] frontier = tree.frontier();
        assertTrue(tree.contains(frontier));
        boolean found = false;
        for (int[] permutation : next) if (Arrays.equals(permutation, frontier)) found = true;
        assertTrue(found);
        return next;
    }

    /** mask の要素が p で連続するか返す。計算量: 時間 O(p.length)、追加領域 O(1)。 */
    // 未テスト
    private static boolean consecutive(int[] p, int mask) {
        int min = p.length, max = -1;
        for (int i = 0; i < p.length; i++) if ((mask & (1 << p[i])) != 0) {
            min = Math.min(min, i);
            max = i;
        }
        return mask == 0 || max - min + 1 == Integer.bitCount(mask);
    }

    /** {0,...,n-1} の全順列を返す。計算量: 時間・領域 O(n! n)。 */
    // 未テスト
    private static List<int[]> permutations(int n) {
        List<int[]> result = new ArrayList<>();
        enumerate(new int[n], new boolean[n], 0, result);
        return result;
    }

    /** prefix[0,depth) を接頭辞とする順列を result に追加する。計算量: 時間・領域 O(n! n)、n=prefix.length。 */
    // 未テスト
    private static void enumerate(int[] prefix, boolean[] used, int depth, List<int[]> result) {
        if (depth == prefix.length) { result.add(prefix.clone()); return; }
        for (int value = 0; value < prefix.length; value++) if (!used[value]) {
            used[value] = true;
            prefix[depth] = value;
            enumerate(prefix, used, depth + 1, result);
            used[value] = false;
        }
    }
}
