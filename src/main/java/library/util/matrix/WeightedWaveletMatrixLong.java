package library.util.matrix;

import library.util.BitArray;
import library.util.Longs;

/**
 * 重み付き 2 次元矩形和クエリ用の Wavelet Matrix。
 *
 * // 未テスト
 */
final class WeightedWaveletMatrixLong {
    private final int n;
    private final int w;
    private final int maxValue;
    private final BitArray[] rank1;
    private final int[] mid;
    private final long[][] sum0;

    /**
     * 指定された値配列と重み配列から WeightedWaveletMatrixLong を構築する。
     *
     * @param values 各要素の値 (0 <= values[i] <= maxValue)
     * @param weights 各要素の重み
     * @param maxValue 最大値
     * @complexity O(N log maxValue)
     */
    WeightedWaveletMatrixLong(int[] values, long[] weights, int maxValue) {
        if (values == null || weights == null) throw new NullPointerException();
        if (values.length != weights.length) throw new IllegalArgumentException("Length mismatch");
        this.n = values.length;
        this.maxValue = Math.max(0, maxValue);

        int bits = 1;
        while ((1L << bits) <= this.maxValue) {
            bits++;
        }
        this.w = bits;

        rank1 = new BitArray[w];
        mid = new int[w];
        sum0 = new long[w][];

        int[] curVals = values.clone();
        long[] curWeights = weights.clone();

        for (int i = w - 1; i >= 0; i--) {
            rank1[i] = new BitArray(n);
            int count0 = 0;
            for (int j = 0; j < n; j++) {
                if (Longs.bitAt(curVals[j], i) == 1) {
                    rank1[i].set(j);
                } else {
                    count0++;
                }
            }
            rank1[i].build();
            mid[i] = count0;

            int[] nextVals = new int[n];
            long[] nextWeights = new long[n];
            int p0 = 0;
            int p1 = count0;

            for (int j = 0; j < n; j++) {
                if (Longs.bitAt(curVals[j], i) == 0) {
                    nextVals[p0] = curVals[j];
                    nextWeights[p0] = curWeights[j];
                    p0++;
                } else {
                    nextVals[p1] = curVals[j];
                    nextWeights[p1] = curWeights[j];
                    p1++;
                }
            }

            sum0[i] = new long[count0 + 1];
            for (int j = 0; j < count0; j++) {
                sum0[i][j + 1] = sum0[i][j] + nextWeights[j];
            }

            curVals = nextVals;
            curWeights = nextWeights;
        }
    }

    /**
     * インデックス範囲 [l, r) のうち、値が upper 未満である要素の重みの総和を返す。
     *
     * @param l 開始インデックス (inclusive)
     * @param r 終了インデックス (exclusive)
     * @param upper 上限値 (exclusive)
     * @return 条件を満たす要素の重み総和
     * @complexity O(log maxValue)
     */
    long sumLessThan(int l, int r, long upper) {
        if (l < 0) l = 0;
        if (r > n) r = n;
        if (l >= r || upper <= 0) return 0;
        if (upper > maxValue) upper = maxValue + 1L;

        long res = 0;
        for (int h = w - 1; h >= 0; h--) {
            int l1 = rank1[h].prefixSum(l);
            int r1 = rank1[h].prefixSum(r);
            int l0 = l - l1;
            int r0 = r - r1;

            if (Longs.bitAt(upper, h) == 1) {
                res += sum0[h][r0] - sum0[h][l0];
                l = mid[h] + l1;
                r = mid[h] + r1;
            } else {
                l = l0;
                r = r0;
            }
        }
        return res;
    }
}
