package library.util;

/**
 * 1次元の半開区間 [l, r) を表す不変な区間。
 *
 * <p>l &ge; r の場合は空区間を表す。
 * 空区間はすべて [0, 0) に正規化される。
 *
 * @param l 区間の左端（包含）
 * @param r 区間の右端（非包含）
 */
// 未テスト
public record LongInterval(long l, long r) implements Comparable<LongInterval> {

    /**
     * コンパクトコンストラクタ。
     * l &ge; r の場合、空区間として [0, 0) に正規化する。
     */
    // 未テスト
    public LongInterval {
        if (l >= r) {
            l = 0;
            r = 0;
        }
    }

    /**
     * 区間が空集合であるか判定する。
     *
     * <p>計算量: O(1)
     *
     * @return l = r (正規化後は 0 = 0) のとき true、それ以外は false
     */
    // 未テスト
    public boolean isEmpty() {
        return l == r;
    }

    /**
     * 区間の長さを返す。
     *
     * <p>計算量: O(1)
     *
     * @return r - l
     */
    // 未テスト
    public long length() {
        return r - l;
    }

    /**
     * 指定された値 x が区間 [l, r) に含まれるか判定する。
     *
     * <p>計算量: O(1)
     *
     * @param x 判定対象の値
     * @return l &le; x &lt; r のとき true、それ以外は false
     */
    // 未テスト
    public boolean contains(long x) {
        return l <= x && x < r;
    }

    /**
     * 指定された区間 other が自身の部分集合であるか判定する。
     *
     * <p>計算量: O(1)
     *
     * @param other 判定対象の区間
     * @return other ⊆ this のとき true、それ以外は false
     */
    // 未テスト
    public boolean contains(LongInterval other) {
        if (other.isEmpty()) return true;
        if (isEmpty()) return false;
        return l <= other.l && other.r <= r;
    }

    /**
     * 指定された区間 other と共通部分（正の長さの交差）を持つか判定する。
     *
     * <p>計算量: O(1)
     *
     * @param other 判定対象の区間
     * @return this &cap; other &ne; &empty; のとき true、それ以外は false
     */
    // 未テスト
    public boolean hasOverlap(LongInterval other) {
        return Math.max(l, other.l) < Math.min(r, other.r);
    }

    /**
     * 指定された区間 other との共通部分 [max(l, other.l), min(r, other.r)) を返す。
     *
     * <p>重なりが存在しない場合は空区間 [0, 0) に正規化された区間が返る。
     *
     * <p>計算量: O(1)
     *
     * @param other 対象の区間
     * @return 共通部分を表す LongInterval
     */
    // 未テスト
    public LongInterval intersection(LongInterval other) {
        return new LongInterval(
                Math.max(l, other.l),
                Math.min(r, other.r));
    }

    /**
     * 自身と指定された区間 other の両方を包含する最小の区間 [min(l, other.l), max(r, other.r)) を返す。
     *
     * <p>計算量: O(1)
     *
     * @param other 対象の区間
     * @return 最小包含区間を表す LongInterval
     */
    // 未テスト
    public LongInterval span(LongInterval other) {
        if (isEmpty()) return other;
        if (other.isEmpty()) return this;
        return new LongInterval(
                Math.min(l, other.l),
                Math.max(r, other.r));
    }

    /**
     * 区間全体を x だけ平行移動した区間 [l + x, r + x) を返す。
     *
     * <p>空区間の場合は自身の空区間 [0, 0) をそのまま返す。
     *
     * <p>計算量: O(1)
     *
     * @param x 移動量
     * @return 移動後の LongInterval
     */
    // 未テスト
    public LongInterval shift(long x) {
        if (isEmpty()) return this;
        return new LongInterval(l + x, r + x);
    }

    /**
     * 右端を x だけ拡張した区間 [l, r + x) を返す。
     *
     * <p>空区間の場合は自身の空区間 [0, 0) をそのまま返す。
     *
     * <p>計算量: O(1)
     *
     * @param x 右端の伸長量
     * @return 拡張後の LongInterval
     */
    // 未テスト
    public LongInterval expandRight(long x) {
        if (isEmpty()) return this;
        return new LongInterval(l, r + x);
    }

    /**
     * 左端を x だけ拡張した区間 [l - x, r) を返す。
     *
     * <p>空区間の場合は自身の空区間 [0, 0) をそのまま返す。
     *
     * <p>計算量: O(1)
     *
     * @param x 左端の伸長量
     * @return 拡張後の LongInterval
     */
    // 未テスト
    public LongInterval expandLeft(long x) {
        if (isEmpty()) return this;
        return new LongInterval(l - x, r);
    }

    /**
     * 左端 l の昇順、l が等しい場合は右端 r の昇順で比較する。
     *
     * <p>計算量: O(1)
     *
     * @param other 比較対象の区間
     * @return 比較結果
     */
    // 未テスト
    @Override
    public int compareTo(LongInterval other) {
        int cmp = Long.compare(this.l, other.l);
        if (cmp != 0) return cmp;
        return Long.compare(this.r, other.r);
    }
}
