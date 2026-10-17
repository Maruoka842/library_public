package library.util.linalg;

import java.util.Arrays;
import library.util.MathUtils;

public class PrefixLinearRankOverFp {
    private final int n;
    private final int d;
    private final long mod;

    // latest[r][b]:
    // prefix [0,r) の最新位置優先基底における pivot b の由来位置
    private final int[][] latest;

    // 構築中の基底だけを保持する
    private final long[][] basis;
    /*
     * basis[b] が i 回目に追加されたベクトル由来とすると、
     * basis[b]=(0, ..., 0, 1 (i要素目), *, ..., *) in a[i] + span(a[i], a[i+1], ..) (定数倍を許せば)
     * 
     */
    private final int[] pos;

    private int size;

    /**
     * 
     * @param n 追加するベクトル数の上限。
     * @param d 各ベクトルの次元。
     * @param mod 有限体 F_p の法。素数であることを仮定する。
     */
    public PrefixLinearRankOverFp(int n, int d, long mod) {
        this.n = n;
        this.d = d;
        this.mod = mod;

        latest = new int[n + 1][d];
        basis = new long[d][];
        pos = new int[d];

        Arrays.fill(pos, -1);
        Arrays.fill(latest[0], -1);
    }

    /**
     * 次のベクトルを追加する。
     * add が呼ばれた回数を i とすると、a は位置 i のベクトル。
     *
     * 計算量 O(d^2)
     */
    public void add(long[] a) {
        if (size >= n) {
            throw new IllegalStateException();
        }
        if (a.length != d) {
            throw new IllegalArgumentException();
        }

        long[] val = new long[d];
        for (int k = 0; k < d; k++) {
            val[k] = a[k] % mod;
            if (val[k] < 0) {
                val[k] += mod;
            }
        }

        int p = size;

        for (int b = 0; b < d; b++) {
            long valB = val[b];
            if (valB == 0) {
                continue;
            }

            if (basis[b] == null) {
                normalize(val, b, valB);
                basis[b] = val;
                pos[b] = p;
                break;
            }

            /*
             * 新しいベクトルのほうが由来位置が新しいなら、
             * pivot b の基底として新しいほうを残す。
             */
            if (p > pos[b]) {
                normalize(val, b, valB);

                long[] tmpBasis = basis[b];
                basis[b] = val;
                val = tmpBasis;

                int tmpPos = pos[b];
                pos[b] = p;
                p = tmpPos;

                // swap 後の val は既存基底なので pivot 成分は 1
                valB = 1;
            }

            long[] pivot = basis[b];

            // 新しい配列を作らず破壊的に消去する
            for (int k = b; k < d; k++) {
                val[k] = (val[k] - valB * pivot[k]) % mod;
                if (val[k] < 0) {
                    val[k] += mod;
                }
            }
        }

        size++;
        System.arraycopy(pos, 0, latest[size], 0, d);
    }

    private void normalize(long[] a, int pivot, long pivotValue) {
        long inv = MathUtils.modInv(pivotValue, mod);
        for (int k = pivot; k < d; k++) {
            a[k] = a[k] * inv % mod;
        }
    }

    public int rank(int l, int r) {
        if (l < 0 || l > r || r > size) {
            throw new IndexOutOfBoundsException();
        }

        int rank = 0;
        for (int b = 0; b < d; b++) {
            if (latest[r][b] >= l) {
                rank++;
            }
        }
        return rank;
    }
}
