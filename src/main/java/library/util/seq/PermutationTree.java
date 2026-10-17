package library.util.seq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import library.util.segtree.LazySegTreelonglong;
import library.util.segtree.SegTreeFactory;

/**
 * 順列木 (Permutation Tree) を構築するクラス。
 *
 * <p>順列 P において、値が連続する区間（共通区間、Common Interval）をノードとして木構造を構築する。
 * 各ノードは以下のいずれかのタイプを持つ：
 * <ul>
 *   <li>Leaf: 1要素の区間</li>
 *   <li>JoinAsc: 子ノードの区間を結合すると値が増加していく連続した区間</li>
 *   <li>JoinDesc: 子ノードの区間を結合すると値が減少していく連続した区間</li>
 *   <li>Cut: 2つ以上の共通区間に分割できない（素な）共通区間</li>
 * </ul>
 * </p>
 *
 * <p>計算量: O(N log N)</p>
 * <p>頂点区間は強共通区間。強共通区間 I は、任意の共通区間 J に対し
 * I ∩ J = ∅、I ⊆ J、J ⊆ I のいずれかを満たす。
 * Join 頂点の連続する子区間の和も共通区間となる。</p>
 * <p>構築後に nodes、root、各 Node のフィールドを変更した場合、検索結果は未定義。
 * 索引を利用するメソッドの計算量は索引構築後のもの。
 * 初回の索引構築には時間・追加空間 O(N log(N + 1)) を要する。</p>
 */
public class PermutationTree {
    public enum NodeType {
        JoinAsc,    // Join，特に P[i] の値が増加していく
        JoinDesc,   // Join，特に P[i] の値が減少していく
        Cut,        // Cut
        Leaf,       // 葉である
        None,
    }

    public static class Node {
        public NodeType tp;             // 頂点の種類
        public int L, R;                // [L, R) : 頂点が表す区間
        public int mini, maxi;          // 区間に含まれる P[i] (L <= i < R) の最小・最大値
        public List<Integer> child = new ArrayList<>();  // 子の頂点番号（昇順）

        /**
         * |[L, R)| = R - L を返す。計算量: O(1)。
         * 未テスト
         * @return 区間長
         */
        public int length() {
            return R - L;
        }

        /**
         * tp = Leaf を返す。計算量: O(1)。
         * 未テスト
         * @return 葉であるか
         */
        public boolean isLeaf() {
            return tp == NodeType.Leaf;
        }

        /**
         * tp ∈ {JoinAsc, JoinDesc} を返す。計算量: O(1)。
         * 未テスト
         * @return Join 頂点であるか
         */
        public boolean isJoin() {
            return tp == NodeType.JoinAsc || tp == NodeType.JoinDesc;
        }

        public Node(NodeType tp, int L, int R, int mini, int maxi) {
            this.tp = tp;
            this.L = L;
            this.R = R;
            this.mini = mini;
            this.maxi = maxi;
        }

        @Override
        public String toString() {
            return String.format("Node{tp=%s, L=%d, R=%d, mini=%d, maxi=%d, child=%s}", tp, L, R, mini, maxi, child);
        }
    }

    public List<Node> nodes = new ArrayList<>(); // 頂点番号から頂点への対応
    public int root; // 根の頂点番号。空の順列では -1
    private final int length; // 入力順列の長さ
    private int[] parents; // 各頂点の親。根では -1。未構築なら null
    private int[] depths; // 根から各頂点までの辺数
    private int[] leaves; // 各順列位置に対応する葉の頂点番号
    private int[] preorder; // 子を区間順に辿る行きがけ順
    private int[] postorder; // 子を区間順に辿る帰りがけ順
    private int[][] ancestors; // ancestors[k][v] は 2^k 個上の祖先。根の上は根

	/**
	 * 順列木の状態を標準出力に出力する。
	 * <ul>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * // 未テスト
	 */
	public void dump() {
		System.out.println("PermutationTree root=" + root + " totalNodes=" + nodes.size());
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(i);
			System.out.println(String.format("Node %d: tp=%s [%d, %d) mini=%d maxi=%d child=%s",
				i, node.tp, node.L, node.R, node.mini, node.maxi, node.child));
		}
	}

    /**
     * 順列 P の共通区間木を構築する。P は {0, ..., P.length - 1} の順列。
     * P.length = 0 のとき root = -1。
     * 計算量: 時間 O(N log(N + 1))、空間 O(N)。N = P.length。
     * 未テスト
     * @param P 順列
     */
    public PermutationTree(int[] P) {
        int n = P.length;
        length = n;
        if (n == 0) {
            root = -1;
            return;
        }

        int[] maxStack = new int[n];
        int[] minStack = new int[n];
        int maxTop = 0, minTop = 0;

        LazySegTreelonglong seg = SegTreeFactory.add_min(n);
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < n; i++) {
            // Update f(j, i) = max(j, i) - min(j, i) - (i - j)
            // Initially f(i, i) = 0
            seg.set(i, 0);
            if (i > 0) seg.act(0, i, -1);

            while (maxTop > 0 && P[maxStack[maxTop - 1]] < P[i]) {
                int idx = maxStack[maxTop - 1];
                int prev = (maxTop > 1) ? maxStack[maxTop - 2] + 1 : 0;
                seg.act(prev, idx + 1, P[i] - P[idx]);
                maxTop--;
            }
            maxStack[maxTop++] = i;

            while (minTop > 0 && P[minStack[minTop - 1]] > P[i]) {
                int idx = minStack[minTop - 1];
                int prev = (minTop > 1) ? minStack[minTop - 2] + 1 : 0;
                seg.act(prev, idx + 1, P[minStack[minTop - 1]] - P[i]);
                minTop--;
            }
            minStack[minTop++] = i;

            int uIdx = nodes.size();
            nodes.add(new Node(NodeType.Leaf, i, i + 1, P[i], P[i]));

            while (!stack.isEmpty()) {
                Node u = nodes.get(uIdx);
                Node v = nodes.get(stack.peek());
                if (isContiguous(v, u)) {
                    int vIdx = stack.pop();
                    NodeType targetTp = v.mini < u.mini ? NodeType.JoinAsc : NodeType.JoinDesc;
                    if (v.tp == targetTp) {
                        v.child.add(uIdx);
                        v.R = u.R;
                        v.mini = Math.min(v.mini, u.mini);
                        v.maxi = Math.max(v.maxi, u.maxi);
                        uIdx = vIdx;
                    } else {
                        Node newNode = new Node(targetTp, v.L, u.R, Math.min(v.mini, u.mini), Math.max(v.maxi, u.maxi));
                        newNode.child.add(vIdx);
                        newNode.child.add(uIdx);
                        uIdx = nodes.size();
                        nodes.add(newNode);
                    }
                } else {
                    // f(j, i) >= 0。minimalLeft は右端を含み、全て正なら -1 を返す。
                    // 右端自身が 0 の場合は直接取得する。
                    int rightL = seg.get(u.L - 1) == 0 ? u.L - 1
                            : seg.minimalLeft(u.L - 1, value -> value > 0);
                    if (rightL != -1) {
                        Node newNode = new Node(NodeType.Cut, rightL, i + 1, -1, -1);
                        int minVal = u.mini;
                        int maxVal = u.maxi;
                        newNode.child.add(0, uIdx);
                        while (true) {
                            int topIdx = stack.pop();
                            Node top = nodes.get(topIdx);
                            newNode.child.add(0, topIdx);
                            minVal = Math.min(minVal, top.mini);
                            maxVal = Math.max(maxVal, top.maxi);
                            if (top.L == rightL) break;
                        }
                        newNode.mini = minVal;
                        newNode.maxi = maxVal;
                        uIdx = nodes.size();
                        nodes.add(newNode);
                    } else {
                        break;
                    }
                }
            }
            stack.push(uIdx);
        }
        root = stack.pop();
    }

    private boolean isContiguous(Node v, Node u) {
        return v.maxi + 1 == u.mini || u.maxi + 1 == v.mini;
    }

    /**
     * 入力順列の長さ N を返す。計算量: O(1)。
     * 未テスト
     * @return 順列長
     */
    public int length() {
        return length;
    }

    /**
     * 頂点数 |nodes| を返す。計算量: O(1)。
     * 未テスト
     * @return 頂点数
     */
    public int nodeCount() {
        return nodes.size();
    }

    /**
     * v の親を返す。v = root のとき -1。索引を利用する。計算量: O(1)。
     * 未テスト
     * @param v 頂点番号。0 <= v < nodeCount()
     * @return 親の頂点番号
     * @throws IndexOutOfBoundsException v が範囲外
     */
    public int parent(int v) {
        checkNode(v);
        buildIndex();
        return parents[v];
    }

    /**
     * root から v までの辺数を返す。索引を利用する。計算量: O(1)。
     * 未テスト
     * @param v 頂点番号。0 <= v < nodeCount()
     * @return 深さ
     * @throws IndexOutOfBoundsException v が範囲外
     */
    public int depth(int v) {
        checkNode(v);
        buildIndex();
        return depths[v];
    }

    /**
     * 区間 [position, position + 1) の葉を返す。索引を利用する。計算量: O(1)。
     * 未テスト
     * @param position 順列上の位置。0 <= position < length()
     * @return 葉の頂点番号
     * @throws IndexOutOfBoundsException position が範囲外
     */
    public int leafAt(int position) {
        if (position < 0 || position >= length) throw new IndexOutOfBoundsException();
        buildIndex();
        return leaves[position];
    }

    /**
     * v から親を k 回辿った頂点を返す。k = 0 のとき v、k > depth(v) のとき -1。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param v 頂点番号。0 <= v < nodeCount()
     * @param k 辿る辺数。k >= 0
     * @return 祖先の頂点番号、存在しなければ -1
     * @throws IndexOutOfBoundsException v が範囲外
     * @throws IllegalArgumentException k < 0
     */
    public int ancestor(int v, int k) {
        checkNode(v);
        if (k < 0) throw new IllegalArgumentException();
        buildIndex();
        if (k > depths[v]) return -1;
        for (int bit = 0; k != 0; bit++, k >>>= 1) {
            if ((k & 1) != 0) v = ancestors[bit][v];
        }
        return v;
    }

    /**
     * u が v の祖先（u = v を含む）かを返す。計算量: O(1)。
     * 未テスト
     * @param u 頂点番号。0 <= u < nodeCount()
     * @param v 頂点番号。0 <= v < nodeCount()
     * @return [v.L, v.R) ⊆ [u.L, u.R)
     * @throws IndexOutOfBoundsException 頂点番号が範囲外
     */
    public boolean isAncestor(int u, int v) {
        checkNode(u);
        checkNode(v);
        Node a = nodes.get(u), b = nodes.get(v);
        return a.L <= b.L && b.R <= a.R;
    }

    /**
     * u, v の共通祖先のうち深さが最大の頂点を返す。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param u 頂点番号。0 <= u < nodeCount()
     * @param v 頂点番号。0 <= v < nodeCount()
     * @return 最近共通祖先の頂点番号
     * @throws IndexOutOfBoundsException 頂点番号が範囲外
     */
    public int lca(int u, int v) {
        checkNode(u);
        checkNode(v);
        buildIndex();
        if (isAncestor(u, v)) return u;
        if (isAncestor(v, u)) return v;
        for (int k = ancestors.length - 1; k >= 0; k--) {
            if (!isAncestor(ancestors[k][u], v)) u = ancestors[k][u];
        }
        return parents[u];
    }

    /**
     * u, v 間の単純路の辺数を返す。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param u 頂点番号。0 <= u < nodeCount()
     * @param v 頂点番号。0 <= v < nodeCount()
     * @return depth(u) + depth(v) - 2 * depth(lca(u, v))
     * @throws IndexOutOfBoundsException 頂点番号が範囲外
     */
    public int distance(int u, int v) {
        int w = lca(u, v);
        return depths[u] + depths[v] - 2 * depths[w];
    }

    /**
     * [l, r) を含む頂点区間のうち最小のものを返す。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param l 左端。0 <= l < r <= length()
     * @param r 右端（含まない）
     * @return 最小包含頂点の番号
     * @throws IndexOutOfBoundsException 区間が範囲外、または空
     */
    public int containingNode(int l, int r) {
        checkInterval(l, r);
        return lca(leafAt(l), leafAt(r - 1));
    }

    /**
     * 頂点区間が [l, r) に一致する頂点を返す。存在しなければ -1。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param l 左端。0 <= l < r <= length()
     * @param r 右端（含まない）
     * @return 強共通区間の頂点番号、存在しなければ -1
     * @throws IndexOutOfBoundsException 区間が範囲外、または空
     */
    public int findNode(int l, int r) {
        int v = containingNode(l, r);
        Node node = nodes.get(v);
        return node.L == l && node.R == r ? v : -1;
    }

    /**
     * max(P[l, r)) - min(P[l, r)) = r - l - 1 を返す。
     * 索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param l 左端。0 <= l < r <= length()
     * @param r 右端（含まない）
     * @return 共通区間であるか
     * @throws IndexOutOfBoundsException 区間が範囲外、または空
     */
    public boolean isCommonInterval(int l, int r) {
        int v = containingNode(l, r);
        Node node = nodes.get(v);
        if (node.L == l && node.R == r) return true;
        if (!node.isJoin()) return false;
        Node left = nodes.get(childContaining(v, l));
        Node right = nodes.get(childContaining(v, r - 1));
        return left.L == l && right.R == r;
    }

    /**
     * [l, r) ⊆ [a, b) を満たす共通区間のうち包含関係で最小の [a, b) を返す。
     * 返す配列は内部状態と参照を共有しない。索引を利用する。計算量: O(log(N + 1))。
     * 未テスト
     * @param l 左端。0 <= l < r <= length()
     * @param r 右端（含まない）
     * @return new int[] {a, b}
     * @throws IndexOutOfBoundsException 区間が範囲外、または空
     */
    public int[] smallestCommonInterval(int l, int r) {
        int v = containingNode(l, r);
        Node node = nodes.get(v);
        if (!node.isJoin()) return new int[] {node.L, node.R};
        return new int[] {nodes.get(childContaining(v, l)).L,
                nodes.get(childContaining(v, r - 1)).R};
    }

    /**
     * 非空な共通区間 [l, r) の総数を返す。
     * 計算量: 時間 O(N)、追加空間 O(1)。
     * 未テスト
     * @return 共通区間数
     */
    public long countCommonIntervals() {
        long count = 0;
        for (Node node : nodes) {
            long k = node.child.size();
            count += node.isJoin() ? k * (k - 1) / 2 : 1;
        }
        return count;
    }

    /**
     * 子を区間の左から右へ辿る行きがけ順の頂点番号列を返す。
     * 返す配列は内部状態と参照を共有しない。索引を利用する。計算量: O(N)。
     * 未テスト
     * @return 行きがけ順。空の木では空配列
     */
    public int[] preOrder() {
        buildIndex();
        return preorder.clone();
    }

    /**
     * 子を区間の左から右へ辿る帰りがけ順の頂点番号列を返す。
     * 返す配列は内部状態と参照を共有しない。索引を利用する。計算量: O(N)。
     * 未テスト
     * @return 帰りがけ順。空の木では空配列
     */
    public int[] postOrder() {
        buildIndex();
        return postorder.clone();
    }

    /**
     * v の子のうち position を含む区間の頂点を返す。
     * 計算量: O(log(N + 1))。
     * 未テスト
     * @param v 内部頂点
     * @param position nodes[v].L <= position < nodes[v].R
     * @return 子の頂点番号
     */
    private int childContaining(int v, int position) {
        List<Integer> children = nodes.get(v).child;
        int lo = 0, hi = children.size();
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (nodes.get(children.get(mid)).L <= position) lo = mid;
            else hi = mid;
        }
        return children.get(lo);
    }

    /**
     * 親・深さ・葉・走査順・祖先の索引を用意する。既存の索引があれば変更しない。
     * 計算量: 初回の時間・追加空間 O(N log(N + 1))、以後 O(1)。
     * 未テスト
     */
    private void buildIndex() {
        if (parents != null) return;
        int m = nodes.size();
        parents = new int[m];
        Arrays.fill(parents, -1);
        depths = new int[m];
        leaves = new int[length];
        preorder = new int[m];
        postorder = new int[m];
        ancestors = new int[Math.max(1, 32 - Integer.numberOfLeadingZeros(m))][m];
        if (m == 0) return;
        int[] pending = new int[m];
        int top = 0, count = 0;
        pending[top++] = root;
        while (top > 0) {
            int v = pending[--top];
            preorder[count++] = v;
            Node node = nodes.get(v);
            if (node.isLeaf()) leaves[node.L] = v;
            for (int i = node.child.size() - 1; i >= 0; i--) {
                int child = node.child.get(i);
                parents[child] = v;
                depths[child] = depths[v] + 1;
                pending[top++] = child;
            }
        }
        // 右から左への行きがけ順を反転すると、左から右への帰りがけ順となる。
        top = 0;
        count = m;
        pending[top++] = root;
        while (top > 0) {
            int v = pending[--top];
            postorder[--count] = v;
            for (int child : nodes.get(v).child) pending[top++] = child;
        }
        for (int v = 0; v < m; v++) ancestors[0][v] = parents[v] < 0 ? v : parents[v];
        for (int k = 1; k < ancestors.length; k++) {
            for (int v = 0; v < m; v++) {
                ancestors[k][v] = ancestors[k - 1][ancestors[k - 1][v]];
            }
        }
    }

    /**
     * 0 <= v < nodeCount() を検証する。計算量: O(1)。
     * 未テスト
     * @param v 頂点番号
     * @throws IndexOutOfBoundsException v が範囲外
     */
    private void checkNode(int v) {
        if (v < 0 || v >= nodes.size()) throw new IndexOutOfBoundsException();
    }

    /**
     * 0 <= l < r <= length() を検証する。計算量: O(1)。
     * 未テスト
     * @param l 左端
     * @param r 右端
     * @throws IndexOutOfBoundsException 区間が範囲外、または空
     */
    private void checkInterval(int l, int r) {
        if (l < 0 || l >= r || r > length) throw new IndexOutOfBoundsException();
    }

}
