package library.util.fold;

import library.util.ArrayUtils;
import library.util.seq.MultiPermutation;

/**
 * 静的整数列に対する区間種類数クエリ
 * Static Range Distinct Count Query
 * <p>計算量: 構築 O(N log N), クエリ O(log N)
 *
 */
public class StaticRangeDistinctCount {
    private final WaveletMatrix wm;
    private final int n;

    /**
     * O(N log N)
     * @param a 構築対象の配列
     */
    public StaticRangeDistinctCount(int[] a) {
        this.n = a.length;
        if (n == 0) {
            this.wm = new WaveletMatrix(new int[0]);
        } else {
            int[] compressed = ArrayUtils.compress(a);
            this.wm = buildWM(compressed);
        }
    }

    /**
     * O(N log N)
     * @param a 構築対象の配列
     */
    public StaticRangeDistinctCount(long[] a) {
        this.n = a.length;
        if (n == 0) {
            this.wm = new WaveletMatrix(new int[0]);
        } else {
            int[] compressed = ArrayUtils.compress(a);
            this.wm = buildWM(compressed);
        }
    }

    private WaveletMatrix buildWM(int[] compressed) {
        int[] prev = MultiPermutation.prevOccurences(compressed);
        int[] c = new int[n];
        for (int i = 0; i < n; i++) {
            c[i] = prev[i] + 1;
        }
        return new WaveletMatrix(c);
    }

    /**
     * 区間 [l, r) に含まれる相異なる整数の種類数を返す。
     * O(log N)
     * @param l inclusive
     * @param r exclusive
     * @return 種類数
     */
    public int count(int l, int r) {
        if (l >= r) return 0;
        return wm.countLess(l, r, (long) l + 1);
    }

    /**
     * 区間 [m, r) に存在するが区間 [l, m) には存在しない値の種類数を返す。
     * <p>
     * 数学的には、S(a, b) を区間 [a, b) に含まれる要素の集合とするとき、
     * |S(m, r) \ S(l, m)| を計算する。
     *
     * 計算量: O(log N)
     *
     * @param l 左区間の左端（含む）
     * @param m 境界点（左区間の右端（除く）、かつ右区間の左端（含む））
     * @param r 右区間の右端（除く）
     * @return 条件を満たす値の種類数
     */
    // 未テスト
    public int countOnlyRight(int l, int m, int r) {
        if (m >= r) return 0;
        if (l >= m) return count(m, r);
        return wm.countLess(m, r, (long) l + 1);
    }

    /**
     * 区間 [l, m) に存在するが区間 [m, r) には存在しない値の種類数を返す。
     * <p>
     * 数学的には、S(a, b) を区間 [a, b) に含まれる要素の集合とするとき、
     * |S(l, m) \ S(m, r)| を計算する。
     *
     * 計算量: O(log N)
     *
     * @param l 左区間の左端（含む）
     * @param m 境界点（左区間の右端（除く）、かつ右区間の左端（含む））
     * @param r 右区間の右端（除く）
     * @return 条件を満たす値の種類数
     */
    // 未テスト
    public int countOnlyLeft(int l, int m, int r) {
        if (l >= m) return 0;
        if (m >= r) return count(l, m);
        return count(l, r) - count(m, r);
    }

    /**
     * 区間 [l, m) と区間 [m, r) の両方に存在する値の種類数を返す。
     * <p>
     * 数学的には、S(a, b) を区間 [a, b) に含まれる要素の集合とするとき、
     * |S(l, m) ∩ S(m, r)| を計算する。
     *
     * 計算量: O(log N)
     *
     * @param l 左区間の左端（含む）
     * @param m 境界点（左区間の右端（除く）、かつ右区間の左端（含む））
     * @param r 右区間の右端（除く）
     * @return 条件を満たす値の種類数
     */
    // 未テスト
    public int countCommon(int l, int m, int r) {
        if (l >= m || m >= r) return 0;
        return count(m, r) - countOnlyRight(l, m, r);
    }

    /**
     * 区間 [l, r) に現れる相異なる値（各値の最初の出現位置）のうち、先頭から k 番目（0-indexed）の要素の元の配列におけるインデックスを返す。
     * <p>
     * 該当する要素が存在しない、または k が不適切な場合は -1 を返す。
     *
     * 計算量: O(log N)
     *
     * @param l inclusive
     * @param r exclusive
     * @param k 0-indexed で何番目の distinct 要素を求めるか
     * @return 元の配列におけるインデックス。存在しない場合は -1
     */
    // 未テスト
    public int kthDistinctIndex(int l, int r, int k) {
        if (l >= r || k < 0) return -1;
        return wm.kthIndexInRangeFiltered(l, r, 0, (long) l + 1, k);
    }

    /**
     * 区間 [l, r) に現れる重複する値（各値の2回目以降の出現位置）のうち、先頭から k 番目（0-indexed）の要素の元の配列におけるインデックスを返す。
     * <p>
     * 該当する要素が存在しない、または k が不適切な場合は -1 を返す。
     *
     * 計算量: O(log N)
     *
     * @param l inclusive
     * @param r exclusive
     * @param k 0-indexed で何番目の duplicate 要素を求めるか
     * @return 元の配列におけるインデックス。存在しない場合は -1
     */
    // 未テスト
    public int kthDuplicateIndex(int l, int r, int k) {
        if (l >= r || k < 0) return -1;
        return wm.kthIndexInRangeFiltered(l, r, (long) l + 1, (long) n + 1, k);
    }

    /**
     * 区間 [l, r) に現れる相異なる値を、l より前の最終出現位置の昇順に並べたときの k 番目の出現位置を返す。
     * <p>
     * もし k 番目の相異なる値が l より前に一度も出現していなければ -1 を返す。
     * また、そのような要素が存在しない、または k が不適切な場合も -1 を返す。
     *
     * 計算量: O(log N)
     *
     * @param l inclusive
     * @param r exclusive
     * @param k 0-indexed で何番目の要素を求めるか
     * @return l より前の最終出現位置。一度も出現していなければ -1
     */
    // 未テスト
    public int kthPreviousOccurrence(int l, int r, int k) {
        if (l >= r || k < 0 || k >= r - l) return -1;
        long v = wm.quantile(l, r, k);
        if (v < l + 1) {
            return (int) v - 1;
        } else {
            return -1;
        }
    }
}
