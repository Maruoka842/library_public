package library.util.segtree;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;

/**
 * 整数特化型永続セグメント木 (Int-specialized Persistent Segment Tree)
 *
 * 各更新操作に対して新しい根を返し、過去の状態を保持する primitive int 特化型の永続セグメント木。
 * 全ての操作は O(log N) です。
 *
 * 未テスト
 */
public class IntPersistentSegTree {

    /** 木の特定のバージョンを表すレコード */
    public record Root(int id) {}

    /** セグメント木の葉の個数（2のべき乗） */
    final int size;

    /** 元の配列の長さ */
    private final int n;

    /** セグメント木の高さ (log2(size)) */
    private final int lg;

    /** 木に適用するモノイドの二項演算 */
    private final IntBinaryOperator op;

    /** モノイドの単位元 */
    private final int identity;

    /** 各ノードの値を保持する配列 */
    int[] values;

    /** 各ノードの左の子のノードIDを保持する配列 */
    int[] left;

    /** 各ノードの右の子のノードIDを保持する配列 */
    int[] right;

    /** 現在までに作成された総ノード数 */
    private int nodeCount;

    /**
     * 長さ n、全ての要素が identity である永続セグメント木を構築します。
     *
     * 計算量: O(N)
     *
     * @param n 長さ
     * @param op 二項演算
     * @param identity 単位元
     */
    // 未テスト
    public IntPersistentSegTree(int n, IntBinaryOperator op, int identity) {
        this.n = n;
        this.op = op;
        this.identity = identity;
        this.size = n <= 1 ? 1 : Integer.highestOneBit(n - 1) << 1;
        this.lg = Integer.numberOfTrailingZeros(size);

        int initialNodes = 2 * size;
        this.values = new int[initialNodes];
        this.left = new int[initialNodes];
        this.right = new int[initialNodes];
        Arrays.fill(left, -1);
        Arrays.fill(right, -1);
        Arrays.fill(values, identity);

        for (int i = size - 1; i >= 1; i--) {
            this.left[i] = 2 * i;
            this.right[i] = 2 * i + 1;
            this.values[i] = op.applyAsInt(this.values[2 * i], this.values[2 * i + 1]);
        }
        this.nodeCount = 2 * size;
    }

    /**
     * 配列 v を元に永続セグメント木を構築します。
     *
     * 計算量: O(N)
     *
     * @param v 初期配列
     * @param op 二項演算
     * @param identity 単位元
     */
    // 未テスト
    public IntPersistentSegTree(int[] v, IntBinaryOperator op, int identity) {
        this.n = v.length;
        this.op = op;
        this.identity = identity;
        this.size = n <= 1 ? 1 : Integer.highestOneBit(n - 1) << 1;
        this.lg = Integer.numberOfTrailingZeros(size);

        int initialNodes = 2 * size;
        this.values = new int[initialNodes];
        this.left = new int[initialNodes];
        this.right = new int[initialNodes];
        Arrays.fill(left, -1);
        Arrays.fill(right, -1);

        for (int i = 0; i < n; i++) this.values[size + i] = v[i];
        for (int i = n; i < size; i++) this.values[size + i] = identity;

        for (int i = size - 1; i >= 1; i--) {
            this.left[i] = 2 * i;
            this.right[i] = 2 * i + 1;
            this.values[i] = op.applyAsInt(this.values[2 * i], this.values[2 * i + 1]);
        }
        this.nodeCount = 2 * size;
    }

    /**
     * 新しいノードを作成し、そのIDを返します。配列容量が不足している場合は自動的に拡張します。
     *
     * 計算量: 償却 O(1)
     */
    // 未テスト
    private int newNode(int val, int l, int r) {
        if (nodeCount == values.length) {
            int newCap = values.length << 1;
            values = Arrays.copyOf(values, newCap);
            left = Arrays.copyOf(left, newCap);
            right = Arrays.copyOf(right, newCap);
        }
        values[nodeCount] = val;
        left[nodeCount] = l;
        right[nodeCount] = r;
        return nodeCount++;
    }

    /**
     * 初期状態の根を取得します。
     *
     * 計算量: O(1)
     *
     * @return 根のレコード
     */
    // 未テスト
    public Root getRoot() {
        return new Root(1);
    }

    /**
     * インデックス p の値を x に更新した新しいバージョンを作成します。
     *
     * 計算量: O(log N)
     *
     * @param root 現在の根
     * @param p インデックス
     * @param x 新しい値
     * @return 新しいバージョンの根
     */
    // 未テスト
    public Root set(Root root, int p, int x) {
        if (!(0 <= p && p < size)) throw new IndexOutOfBoundsException();

        int[] ids = new int[lg + 1];
        ids[lg] = root.id;
        for (int i = lg - 1; i >= 0; i--) {
            ids[i] = ((p >> i) & 1) == 1 ? right[ids[i + 1]] : left[ids[i + 1]];
        }

        int copyCur = newNode(x, -1, -1);
        for (int i = 1; i <= lg; i++) {
            int par = ids[i];
            int cur = ids[i - 1];
            int l = left[par] == cur ? copyCur : left[par];
            int r = right[par] == cur ? copyCur : right[par];
            copyCur = newNode(op.applyAsInt(values[l], values[r]), l, r);
        }
        return new Root(copyCur);
    }

    /**
     * インデックス p の値を取得します。
     *
     * 計算量: O(log N)
     *
     * @param root 根
     * @param p インデックス
     * @return 値
     */
    // 未テスト
    public int get(Root root, int p) {
        if (!(0 <= p && p < n)) throw new IndexOutOfBoundsException();
        int cur = root.id;
        for (int i = lg - 1; i >= 0; i--) {
            cur = ((p >> i) & 1) == 1 ? right[cur] : left[cur];
        }
        return values[cur];
    }

    /**
     * 範囲 [l, r) の積を計算します。
     *
     * 計算量: O(log N)
     *
     * @param root 根
     * @param l 左端 (inclusive)
     * @param r 右端 (exclusive)
     * @return 範囲の積
     */
    // 未テスト
    public int prod(Root root, int l, int r) {
        if (!(0 <= l && l <= r && r <= n)) throw new IndexOutOfBoundsException();
        return prod(root.id, 0, size, l, r);
    }

    // 未テスト
    private int prod(int cur, int lo, int hi, int l, int r) {
        if (r <= lo || hi <= l) return identity;
        if (l <= lo && hi <= r) return values[cur];
        int mid = (lo + hi) >> 1;
        return op.applyAsInt(prod(left[cur], lo, mid, l, r), prod(right[cur], mid, hi, l, r));
    }

    /**
     * 全範囲の積を取得します。
     *
     * 計算量: O(1)
     *
     * @param root 根
     * @return 全範囲 of 積
     */
    // 未テスト
    public int allProd(Root root) {
        return values[root.id];
    }

    /**
     * f(prod(l, r)) が真となる最大の r を返します。
     *
     * 計算量: O(log N)
     *
     * @param root 根
     * @param l 左端
     * @param f 判定関数
     * @return 最大の r
     */
    // 未テスト
    public int maxRight(Root root, int l, IntPredicate f) {
        if (!(0 <= l && l <= n)) throw new IndexOutOfBoundsException();
        if (!f.test(identity)) throw new IllegalArgumentException("f(identity) must be true");
        if (l == n) return n;

        MaxRightState state = new MaxRightState(identity);
        return Math.min(maxRightRec(root.id, 0, size, l, f, state), n);
    }

    // 未テスト
    private static class MaxRightState {
        int sm;
        MaxRightState(int sm) { this.sm = sm; }
    }

    // 未テスト
    private int maxRightRec(int cur, int lo, int hi, int l, IntPredicate f, MaxRightState state) {
        if (hi <= l) return hi;
        if (l <= lo) {
            int nxt = op.applyAsInt(state.sm, values[cur]);
            if (f.test(nxt)) {
                state.sm = nxt;
                return hi;
            }
            if (hi - lo == 1) return lo;
        }
        int mid = (lo + hi) >> 1;
        if (l < mid) {
            int leftRes = maxRightRec(left[cur], lo, mid, l, f, state);
            if (leftRes < mid) return leftRes;
        }
        return maxRightRec(right[cur], mid, hi, l, f, state);
    }

    /**
     * f(prod(l, r)) が真となる最小の l を返します。
     *
     * 計算量: O(log N)
     *
     * @param root 根
     * @param r 右端
     * @param f 判定関数
     * @return 最小の l
     */
    // 未テスト
    public int minLeft(Root root, int r, IntPredicate f) {
        if (!(0 <= r && r <= n)) throw new IndexOutOfBoundsException();
        if (!f.test(identity)) throw new IllegalArgumentException("f(identity) must be true");
        if (r == 0) return 0;

        MinLeftState state = new MinLeftState(identity);
        return minLeftRec(root.id, 0, size, r, f, state);
    }

    // 未テスト
    private static class MinLeftState {
        int sm;
        MinLeftState(int sm) { this.sm = sm; }
    }

    // 未テスト
    private int minLeftRec(int cur, int lo, int hi, int r, IntPredicate f, MinLeftState state) {
        if (r <= lo) return lo;
        if (hi <= r) {
            int nxt = op.applyAsInt(values[cur], state.sm);
            if (f.test(nxt)) {
                state.sm = nxt;
                return lo;
            }
            if (hi - lo == 1) return hi;
        }
        int mid = (lo + hi) >> 1;
        if (mid < r) {
            int rightRes = minLeftRec(right[cur], mid, hi, r, f, state);
            if (mid < rightRes) return rightRes;
        }
        return minLeftRec(left[cur], lo, mid, r, f, state);
    }
}
