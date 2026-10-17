package library.util.segtree;

/**
 * 静的配列に対する区間第1欠損値 (Static Range First Missing / Range Mex) クエリ。
 *
 * 与えられた配列 a に対し、任意の範囲 [l, r) において、
 * 指定された下限値 lower 以上の整数のうち、出現しない最小の値を O(log N) 時間で求めます。
 *
 * 内部的には、各 root[r] が「値 x の a[0:r) における最終出現位置 last[x]」を葉とし、
 * 内部ノードには区間内 leaf の最小値である minLast を保持する永続セグメント木を使用します。
 *
 * 未テスト
 */
public class StaticRangeFirstMissing {

    /** 最終出現位置を管理する永続セグメント木 */
    private final IntPersistentSegTree pst;

    /** 各プレフィックス a[0:r) (0 <= r <= n) に対応する永続セグメント木の根を保持する配列 */
    private final IntPersistentSegTree.Root[] roots;

    /** 入力配列の長さ */
    private final int n;

    /**
     * 長さ n の配列 a に基づき、クエリ処理に必要な前計算を行います。
     *
     * 計算量: O(N log N) (N = a.length)
     * メモリ: O(N log N)
     *
     * @param a クエリ対象 of 静的配列
     */
    // 未テスト
    public StaticRangeFirstMissing(int[] a) {
        this.n = a.length;
        this.pst = new IntPersistentSegTree(this.n + 1, Math::min, -1);
        this.roots = new IntPersistentSegTree.Root[this.n + 1];
        this.roots[0] = pst.getRoot();

        for (int i = 0; i < this.n; i++) {
            int val = a[i];
            if (0 <= val && val <= this.n) {
                this.roots[i + 1] = pst.set(this.roots[i], val, i);
            } else {
                this.roots[i + 1] = this.roots[i];
            }
        }
    }

    /**
     * 範囲 [l, r) 内において、lower 以上の整数のうち出現しない最小の値（第1欠損値）を返します。
     *
     * <p>数学的定義: min { x >= lower | x ∉ a[l:r) }</p>
     *
     * 計算量: O(log N)
     *
     * @param l 範囲の左端（inclusive）
     * @param r 範囲の右端（exclusive）
     * @param lower 検索する欠損値の下限（この値以上のものを探す）
     * @return 範囲 [l, r) 内に存在しない下限 lower 以上の最小 of 整数
     * @throws IndexOutOfBoundsException l または r が配列の境界外の場合、または l > r の場合
     */
    // 未テスト
    public int firstMissing(int l, int r, int lower) {
        if (!(0 <= l && l <= r && r <= n)) {
            throw new IndexOutOfBoundsException("Invalid range: [" + l + ", " + r + ") for length " + n);
        }
        if (lower < 0) {
            return lower;
        }
        if (lower > n) {
            return lower;
        }
        int res = queryFirstMissing(roots[r].id(), 0, pst.size - 1, lower, n, l);
        if (res == -1) {
            return n + 1;
        }
        return res;
    }

    /**
     * 永続セグメント木の配列を直接走査して、条件を満たす最小のインデックスを検索します。
     *
     * 計算量: O(log N)
     */
    // 未テスト
    private int queryFirstMissing(int cur, int curL, int curR, int lower, int limit, int threshold) {
        if (curR < lower || curL > limit || pst.values[cur] >= threshold) {
            return -1;
        }
        if (curL == curR) {
            return curL;
        }
        int mid = (curL + curR) >> 1;
        if (mid >= lower) {
            int leftRes = queryFirstMissing(pst.left[cur], curL, mid, lower, limit, threshold);
            if (leftRes != -1) {
                return leftRes;
            }
        }
        return queryFirstMissing(pst.right[cur], mid + 1, curR, lower, limit, threshold);
    }
}
