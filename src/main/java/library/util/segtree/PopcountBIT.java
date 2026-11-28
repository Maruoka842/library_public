package library.util.segtree;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * 0/1 列に対する Binary Indexed Tree (popcount 版)。
 *
 * 64 要素を 1 ワード(long)にまとめ、ワード内は Long.bitCount で O(1) に処理し、
 * ワード単位の個数だけを Fenwick tree で管理する。
 * よって Fenwick の大きさは n/64 となり、各操作は O(log n - log 64)。
 * メモリも n/64 個の long + n/64 個の int で済む。
 */
public class PopcountBIT {
    private final int n;
    private final int words;     // ワード数
    private final long[] bits;   // 0-indexed のビット列
    private final int[] tree;    // Fenwick (1-indexed): ワード j は位置 j+1
    private int total;           // 全体の 1 の個数

    /** 全て 0 で初期化。 */
    public PopcountBIT(int n) {
        this.n = n;
        this.words = (n >>> 6) + 1; // 番兵として 1 ワード余分に確保 (sum(n) の境界処理を不要にする)
        this.bits = new long[words];
        this.tree = new int[words + 1];
    }

    /** boolean 配列から O(n/64) で構築。 */
    public PopcountBIT(boolean[] a) {
        this(a.length);
        for (int i = 0; i < n; i++) {
            if (a[i]) bits[i >>> 6] |= 1L << (i & 63);
        }
        for (int j = 0; j < words; j++) {
            int c = Long.bitCount(bits[j]);
            total += c;
            tree[j + 1] += c;
            int p = (j + 1) + ((j + 1) & -(j + 1));
            if (p <= words) tree[p] += tree[j + 1];
        }
    }

    public int size() {
        return n;
    }

    /** 全体の 1 の個数。O(1) */
    public int total() {
        return total;
    }

    /** a[i] を返す。O(1) */
    public boolean get(int i) {
        return (bits[i >>> 6] >>> (i & 63) & 1L) != 0;
    }

    /** a[i] = v。変化があった場合のみ Fenwick を更新。O(log n - log 64) */
    public void set(int i, boolean v) {
        if (get(i) == v) return;
        flip(i);
    }

    /** a[i] を反転。O(log n - log 64) */
    public void flip(int i) {
        int w = i >>> 6;
        long mask = 1L << (i & 63);
        int d = ((bits[w] & mask) == 0) ? 1 : -1;
        bits[w] ^= mask;
        total += d;
        for (int x = w + 1; x <= words; x += x & -x) tree[x] += d;
    }

    /** [0, r) の 1 の個数。O(log n - log 64) */
    public int sum(int r) {
        int w = r >>> 6;
        int s = Long.bitCount(bits[w] & ((1L << (r & 63)) - 1)); // 端数ワード (r&63==0 なら 0)
        for (int x = w; x > 0; x -= x & -x) s += tree[x];         // ワード [0, w)
        return s;
    }

    /** [l, r) の 1 の個数。 */
    public int sum(int l, int r) {
        return sum(r) - sum(l);
    }

    /**
     * 0-indexed で k 番目 (k=0 が最初) の 1 の位置を返す。存在しなければ -1。
     * Fenwick 上の二分探索 + ワード内 select。O(log n - log 64)
     */
    public int kth(int k) {
        if (k < 0 || k >= total) return -1;
        int pos = 0;
        for (int pw = Integer.highestOneBit(words); pw > 0; pw >>= 1) {
            int np = pos + pw;
            if (np <= words && tree[np] <= k) {
                pos = np;
                k -= tree[np];
            }
        }
        // pos は 0-indexed のワード番号。ワード内の k 番目の 1 を探す。
        return (pos << 6) + selectInWord(bits[pos], k);
    }

    /** x の k 番目 (0-indexed) の立っているビット位置。popcount の二分探索で 6 ステップ。 */
    private static int selectInWord(long x, int k) {
        int pos = 0;
        for (int s = 32; s > 0; s >>= 1) {
            int c = Long.bitCount(x & ((1L << s) - 1));
            if (c <= k) {
                k -= c;
                x >>>= s;
                pos += s;
            }
        }
        return pos;
    }
}
