package library.util.seq;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PermutationTreeTest {

    /**
     * 0 <= n <= 7 の全順列について、頂点区間が強共通区間と一致することを検証する。
     * 計算量: O(7! * 7^4)。
     * 未テスト
     */
    @Test
    public void testAllSmallPermutations() {
        for (int n = 0; n <= 7; n++) {
            int[] p = new int[n];
            for (int i = 0; i < n; i++) p[i] = i;
            checkPermutations(p, 0);
        }
    }

    /**
     * p[0, depth) を固定し、p[depth, n) の全順列を検証する。終了時に p を復元する。
     * 計算量: O((n - depth)! * (n + 1)^4)。n = p.length。
     * 未テスト
     * @param p 順列
     * @param depth 固定済みの接頭辞長
     */
    private void checkPermutations(int[] p, int depth) {
        if (depth == p.length) {
            checkStrongIntervals(p);
            return;
        }
        for (int i = depth; i < p.length; i++) {
            int temp = p[depth];
            p[depth] = p[i];
            p[i] = temp;
            checkPermutations(p, depth + 1);
            temp = p[depth];
            p[depth] = p[i];
            p[i] = temp;
        }
    }

    /**
     * 共通区間 I: max(p[I]) - min(p[I]) = |I| - 1。
     * 強共通区間: どの共通区間 J とも、I ∩ J = ∅、I ⊆ J、J ⊆ I のいずれか。
     * 頂点集合・子区間の分割・型をこれらの定義と照合する。
     * 計算量: 時間 O((n + 1)^4)、空間 O((n + 1)^2)。n = p.length。
     * 未テスト
     * @param p 順列
     */
    private void checkStrongIntervals(int[] p) {
        int n = p.length;
        PermutationTree tree = new PermutationTree(p);
        if (n == 0) {
            assertEquals(-1, tree.root);
            assertTrue(tree.nodes.isEmpty());
            assertEquals(0, tree.length());
            assertEquals(0, tree.nodeCount());
            assertEquals(0, tree.countCommonIntervals());
            assertArrayEquals(new int[0], tree.preOrder());
            assertArrayEquals(new int[0], tree.postOrder());
            return;
        }
        boolean[][] common = new boolean[n][n + 1];
        boolean[][] actual = new boolean[n][n + 1];
        for (int l = 0; l < n; l++) {
            int min = n, max = -1;
            for (int r = l + 1; r <= n; r++) {
                min = Math.min(min, p[r - 1]);
                max = Math.max(max, p[r - 1]);
                common[l][r] = max - min == r - l - 1;
            }
        }
        assertEquals(0, tree.nodes.get(tree.root).L);
        assertEquals(n, tree.nodes.get(tree.root).R);
        for (PermutationTree.Node node : tree.nodes) {
            assertFalse(actual[node.L][node.R]);
            actual[node.L][node.R] = true;
            int min = n, max = -1;
            for (int i = node.L; i < node.R; i++) {
                min = Math.min(min, p[i]);
                max = Math.max(max, p[i]);
            }
            assertEquals(min, node.mini);
            assertEquals(max, node.maxi);
            if (node.R - node.L == 1) {
                assertEquals(PermutationTree.NodeType.Leaf, node.tp);
                assertTrue(node.child.isEmpty());
                continue;
            }
            assertTrue(node.child.size() >= 2);
            int end = node.L;
            boolean asc = true, desc = true;
            PermutationTree.Node previous = null;
            for (int id : node.child) {
                PermutationTree.Node child = tree.nodes.get(id);
                assertEquals(end, child.L);
                end = child.R;
                if (previous != null) {
                    asc &= previous.maxi + 1 == child.mini;
                    desc &= child.maxi + 1 == previous.mini;
                }
                previous = child;
            }
            assertEquals(node.R, end);
            assertEquals(asc ? PermutationTree.NodeType.JoinAsc
                    : desc ? PermutationTree.NodeType.JoinDesc : PermutationTree.NodeType.Cut, node.tp);
        }
        for (int l = 0; l < n; l++) {
            for (int r = l + 1; r <= n; r++) {
                boolean strong = common[l][r];
                for (int a = 0; a < n && strong; a++) {
                    for (int b = a + 1; b <= n; b++) {
                        if (common[a][b] && (l < a && a < r && r < b
                                || a < l && l < b && b < r)) {
                            strong = false;
                            break;
                        }
                    }
                }
                assertEquals(strong, actual[l][r]);
            }
        }
        checkQueries(tree, common, actual);
    }

    /**
     * 区間検索・共通区間検索・祖先検索を全区間と全頂点対について検証する。
     * 計算量: 時間 O((n + 1)^4)、追加空間 O(n)。n = tree.length()。
     * 未テスト
     * @param tree 検証する木
     * @param common common[l][r] は [l, r) が共通区間か
     * @param strong strong[l][r] は [l, r) が強共通区間か
     */
    private void checkQueries(PermutationTree tree, boolean[][] common, boolean[][] strong) {
        int n = common.length, m = tree.nodes.size();
        assertEquals(n, tree.length());
        assertEquals(m, tree.nodeCount());
        int[] parents = new int[m];
        java.util.Arrays.fill(parents, -1);
        for (int v = 0; v < m; v++) {
            PermutationTree.Node node = tree.nodes.get(v);
            assertEquals(node.R - node.L, node.length());
            assertEquals(node.R - node.L == 1, node.isLeaf());
            assertEquals(node.tp == PermutationTree.NodeType.JoinAsc
                    || node.tp == PermutationTree.NodeType.JoinDesc, node.isJoin());
            for (int child : node.child) parents[child] = v;
        }
        for (int i = 0; i < n; i++) {
            PermutationTree.Node leaf = tree.nodes.get(tree.leafAt(i));
            assertEquals(i, leaf.L);
            assertEquals(i + 1, leaf.R);
            assertTrue(leaf.isLeaf());
        }
        int[] pre = tree.preOrder(), post = tree.postOrder();
        assertEquals(m, pre.length);
        assertEquals(m, post.length);
        assertEquals(tree.root, pre[0]);
        assertEquals(tree.root, post[m - 1]);
        int[] prePos = new int[m], postPos = new int[m];
        boolean[] seenPre = new boolean[m], seenPost = new boolean[m];
        for (int i = 0; i < m; i++) {
            assertFalse(seenPre[pre[i]]);
            assertFalse(seenPost[post[i]]);
            seenPre[pre[i]] = seenPost[post[i]] = true;
            prePos[pre[i]] = postPos[post[i]] = i;
        }
        for (int v = 0; v < m; v++) {
            assertEquals(parents[v], tree.parent(v));
            int depth = 0;
            for (int u = v; parents[u] != -1; u = parents[u]) depth++;
            assertEquals(depth, tree.depth(v));
            int expectedAncestor = v;
            for (int k = 0; k <= depth + 1; k++) {
                assertEquals(expectedAncestor, tree.ancestor(v, k));
                if (expectedAncestor != -1) expectedAncestor = parents[expectedAncestor];
            }
            assertEquals(-1, tree.ancestor(v, Integer.MAX_VALUE));
            int previous = -1;
            for (int child : tree.nodes.get(v).child) {
                assertTrue(prePos[v] < prePos[child]);
                assertTrue(postPos[child] < postPos[v]);
                if (previous != -1) {
                    assertTrue(prePos[previous] < prePos[child]);
                    assertTrue(postPos[previous] < postPos[child]);
                }
                previous = child;
            }
            for (int u = 0; u < m; u++) {
                boolean ancestor = false;
                for (int a = u; a != -1; a = parents[a]) ancestor |= a == v;
                assertEquals(ancestor, tree.isAncestor(v, u));
                int expected = v;
                while (true) {
                    boolean found = false;
                    for (int a = u; a != -1; a = parents[a]) found |= a == expected;
                    if (found) break;
                    expected = parents[expected];
                }
                assertEquals(expected, tree.lca(v, u));
                assertEquals(depth + tree.depth(u) - 2 * tree.depth(expected), tree.distance(v, u));
            }
        }
        pre[0] = -1;
        post[m - 1] = -1;
        assertEquals(tree.root, tree.preOrder()[0]);
        assertEquals(tree.root, tree.postOrder()[m - 1]);
        long count = 0;
        for (int l = 0; l < n; l++) {
            for (int r = l + 1; r <= n; r++) {
                assertEquals(common[l][r], tree.isCommonInterval(l, r));
                if (common[l][r]) count++;
                int expectedNode = -1;
                for (int v = 0; v < m; v++) {
                    PermutationTree.Node node = tree.nodes.get(v);
                    if (node.L <= l && r <= node.R && (expectedNode == -1
                            || node.length() < tree.nodes.get(expectedNode).length())) expectedNode = v;
                }
                assertEquals(expectedNode, tree.containingNode(l, r));
                assertEquals(strong[l][r] ? expectedNode : -1, tree.findNode(l, r));
                int bestL = 0, bestR = n;
                for (int a = 0; a <= l; a++) {
                    for (int b = r; b <= n; b++) {
                        if (common[a][b] && b - a < bestR - bestL) {
                            bestL = a;
                            bestR = b;
                        }
                    }
                }
                assertArrayEquals(new int[] {bestL, bestR}, tree.smallestCommonInterval(l, r));
            }
        }
        assertEquals(count, tree.countCommonIntervals());
    }

    /**
     * 空区間・範囲外の頂点・負の祖先距離が拒否されることを検証する。
     * 計算量: O(1)。
     * 未テスト
     */
    @Test
    public void testInvalidQueries() {
        PermutationTree empty = new PermutationTree(new int[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.leafAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.parent(0));
        assertThrows(IndexOutOfBoundsException.class, () -> empty.containingNode(0, 0));
        PermutationTree tree = new PermutationTree(new int[] {0, 1, 2});
        assertThrows(IndexOutOfBoundsException.class, () -> tree.leafAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.leafAt(3));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.parent(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.depth(tree.nodeCount()));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.lca(tree.root, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.isAncestor(-1, tree.root));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.distance(-1, tree.root));
        assertThrows(IllegalArgumentException.class, () -> tree.ancestor(tree.root, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.ancestor(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.findNode(1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.isCommonInterval(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.smallestCommonInterval(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> tree.containingNode(2, 1));
    }

    /**
     * 深さ Θ(n) の木で祖先・走査順を検証し、共通区間数が int の範囲を超える場合を検証する。
     * 計算量: 時間・空間 O(n log n)。n = 100000。
     * 未テスト
     */
    @Test
    public void testDeepTreeAndLargeCount() {
        int n = 20000;
        int[] p = new int[n];
        for (int i = 0; i < n; i++) p[i] = (i & 1) == 0 ? i / 2 : n - 1 - i / 2;
        PermutationTree tree = new PermutationTree(p);
        int leaf = tree.leafAt(n - 1);
        assertTrue(tree.depth(leaf) >= n - 1);
        assertEquals(tree.root, tree.ancestor(leaf, tree.depth(leaf)));
        assertEquals(tree.root, tree.lca(tree.leafAt(0), leaf));
        assertEquals(tree.nodeCount(), tree.preOrder().length);
        assertEquals(tree.nodeCount(), tree.postOrder().length);
        n = 100000;
        p = new int[n];
        for (int i = 0; i < n; i++) p[i] = i;
        tree = new PermutationTree(p);
        assertEquals((long) n * (n + 1) / 2, tree.countCommonIntervals());
        assertTrue(tree.isCommonInterval(123, n - 456));
        assertArrayEquals(new int[] {123, n - 456}, tree.smallestCommonInterval(123, n - 456));
    }

    @Test
    public void testIdentity() {
        int[] P = {0, 1, 2};
        PermutationTree pt = new PermutationTree(P);
        PermutationTree.Node root = pt.nodes.get(pt.root);
        assertEquals(PermutationTree.NodeType.JoinAsc, root.tp);
        assertEquals(0, root.L);
        assertEquals(3, root.R);
        assertEquals(3, root.child.size());
    }

    @Test
    public void testReverse() {
        int[] P = {2, 1, 0};
        PermutationTree pt = new PermutationTree(P);
        PermutationTree.Node root = pt.nodes.get(pt.root);
        assertEquals(PermutationTree.NodeType.JoinDesc, root.tp);
        assertEquals(0, root.L);
        assertEquals(3, root.R);
        assertEquals(3, root.child.size());
    }

    @Test
    public void testCut() {
        int[] P = {1, 3, 0, 2};
        PermutationTree pt = new PermutationTree(P);
        PermutationTree.Node root = pt.nodes.get(pt.root);
        assertEquals(PermutationTree.NodeType.Cut, root.tp);
        assertEquals(0, root.L);
        assertEquals(4, root.R);
        assertEquals(4, root.child.size());
    }

    @Test
    public void testMixed() {
        int[] P = {0, 2, 4, 1, 3};
        PermutationTree pt = new PermutationTree(P);
        PermutationTree.Node root = pt.nodes.get(pt.root);
        // [0, 2, 4, 1, 3]
        // 0 is Leaf, [2, 4, 1, 3] is Cut? No.
        // [2, 4, 1, 3] values are {1, 2, 3, 4}, so it's a common interval.
        // Actually {0, 2, 4, 1, 3} is [0, 5) values {0, 1, 2, 3, 4}.
        // 0 is [0, 1) values {0}.
        // [2, 4, 1, 3] is [1, 5) values {1, 2, 3, 4}.
        // So root should be JoinAsc with children [0, 1) and [1, 5).
        assertEquals(PermutationTree.NodeType.JoinAsc, root.tp);
        assertEquals(2, root.child.size());

        PermutationTree.Node child1 = pt.nodes.get(root.child.get(0));
        assertEquals(PermutationTree.NodeType.Leaf, child1.tp);
        assertEquals(0, child1.mini);

        PermutationTree.Node child2 = pt.nodes.get(root.child.get(1));
        assertEquals(PermutationTree.NodeType.Cut, child2.tp);
        assertEquals(4, child2.child.size());
    }

    @Test
    public void testSingle() {
        int[] P = {0};
        PermutationTree pt = new PermutationTree(P);
        PermutationTree.Node root = pt.nodes.get(pt.root);
        assertEquals(PermutationTree.NodeType.Leaf, root.tp);
        assertEquals(0, root.L);
        assertEquals(1, root.R);
    }
}
