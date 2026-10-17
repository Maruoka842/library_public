package library.util;

import java.util.Arrays;

/**
 * 区間ブロック分割を用いた (min, +) 動的計画法 (Min-Plus Block DP) を計算するクラス。
 *
 * <p>非負整数 x >= 0 に対して定義された関数 f(x) に対し、以下の遷移を繰り返し適用する:</p>
 * f_(i+1/2)(x) = min(g_i(x + 1) + c[i], g_i(x - 1) + A[i], g_i(x - 2) + 2 * A[i], ...), x >= 0
 * <p>その後、オプションとして以下の線形変換を行う:</p>
 * f_(i+1)(x) = mul[i] * f_(i+1/2)(x) + p[i] * x + q[i] (mul[i] > 0)
 *
 * <p>前提条件: 0 < c[i] < A[i]</p>
 *
 * <p>時間計算量: O(N^(3/2))</p>
 * <p>空間計算量: O(N)</p>
 */
public class MinPlusBlockDP {

    /** 無限大を表す定数。 */
    static final long INF = Long.MAX_VALUE / 4;

    /**
     * 各ステップ i で f(x) <- min(g(x + 1) + c[i], g(x - 1) + A[i], g(x - 2) + 2 * A[i], ...) の遷移を繰り返し適用し、
     * 最終状態における min(f(0), f(1)) の最小値を求める。
     * 初期状態は f_0(0) = 0, f_0(x) = INF (x > 0)。
     *
     * <p>時間計算量: O(N^(3/2))</p>
     * <p>空間計算量: O(N)</p>
     * // 未テスト
     *
     * @param A 各ステップの単位傾きコストの配列 A[i]
     * @param c 各ステップの右シフト移動コストの配列 c[i]
     * @return 最終状態での min(f(0), f(1))
     */
    public static long solve(long[] A, long[] c) {
        int n = A.length;
        return solve(A, c, ones(n), new long[n], new long[n]);
    }

    /**
     * 各ステップ i で遷移の直後に 1次関数加算 f(x) <- f(x) + p[i] * x + q[i] を適用し、
     * 最終状態における min(f(0), f(1)) の最小値を求める。
     *
     * <p>時間計算量: O(N^(3/2))</p>
     * <p>空間計算量: O(N)</p>
     * // 未テスト
     *
     * @param A 各ステップの単位傾きコストの配列 A[i]
     * @param c 各ステップの右シフト移動コストの配列 c[i]
     * @param p 各ステップの傾き加算値の配列 p[i] (p[i] >= 0)
     * @param q 各ステップの定数加算値の配列 q[i]
     * @return 最終状態での min(f(0), f(1))
     */
    public static long solve(long[] A, long[] c, long[] p, long[] q) {
        return solve(A, c, ones(A.length), p, q);
    }

    /**
     * 各ステップ i で遷移 T_(A[i],c[i]) の直後に f(x) <- mul[i] * f(x) + p[i] * x + q[i] を適用し、
     * 最終状態における min(f(0), f(1)) の最小値を求める。
     * 非限定状態の solve() では p[i] >= 0 が要求される。
     * applyBlock() 自体は出力範囲が明示的であるため任意の p を許容する。
     *
     * <p>時間計算量: O(N^(3/2))</p>
     * <p>空間計算量: O(N)</p>
     * // 未テスト
     *
     * @param A 各ステップの単位傾きコストの配列 A[i]
     * @param c 各ステップの右シフト移動コストの配列 c[i]
     * @param mul 各ステップの乗算係数の配列 mul[i] (mul[i] > 0)
     * @param p 各ステップの傾き加算値の配列 p[i] (p[i] >= 0)
     * @param q 各ステップの定数加算値の配列 q[i]
     * @return 最終状態での min(f(0), f(1))
     */
    public static long solve(long[] A, long[] c, long[] mul, long[] p, long[] q) {
        int n = A.length;
        if (c.length != n || mul.length != n || p.length != n || q.length != n)
            throw new IllegalArgumentException();
        for (int i = 0; i < n; i++)
            if (mul[i] <= 0 || p[i] < 0) throw new IllegalArgumentException("mul > 0 and p >= 0 required");
        int bs = Math.max(1, (int) Math.sqrt(n));

        long[] f = new long[n + 2];
        Arrays.fill(f, INF);
        f[0] = 0;
        for (int l = 0; l < n; l += bs) {
            int m = Math.min(bs, n - l);
            int outMax = n - (l + m) + 1;
            f = applyBlock(f, A, c, mul, p, q, l, m, outMax);
        }
        return Math.min(f[0], f.length == 1 ? INF : f[1]);
    }

    /**
     * ステップ off から m 個のステップのブロック遷移を適用する。
     *
     * <p>時間計算量: O(m * outMax + m^2)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return 遷移後の関数値配列
     */
    public static long[] applyBlock(long[] f, long[] A, long[] c, int off, int m, int outMax) {
        return rawBlock(f, A, c, off, m, outMax);
    }

    /**
     * 各ステップ直後に p[i] * x + q[i] の加算を伴う m 個のステップのブロック遷移を適用する。
     *
     * <p>時間計算量: O(m * outMax + m^2)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param p 各ステップの p[i]
     * @param q 各ステップの q[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return 遷移後の関数値配列
     */
    public static long[] applyBlock(long[] f, long[] A, long[] c, long[] p, long[] q,
                             int off, int m, int outMax) {
        return applyBlock(f, A, c, ones(A.length), p, q, off, m, outMax);
    }

    /**
     * 各ステップ直後に f <- mul[i] * T_i(f) + p[i] * x + q[i] を伴う m 個のステップのブロック遷移を適用する。
     *
     * <p>時間計算量: O(m * outMax + m^2)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param mul 各ステップの mul[i]
     * @param p 各ステップの p[i]
     * @param q 各ステップの q[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return 遷移後の関数値配列
     */
    public static long[] applyBlock(long[] f, long[] A, long[] c, long[] mul, long[] p, long[] q,
                             int off, int m, int outMax) {
        long[] aa = new long[m], cc = new long[m];
        long prod = 1, slope = 0, add = 0;
        for (int t = m - 1; t >= 0; t--) {
            long alpha = prod * p[off + t];
            slope += alpha;
            long w = prod * mul[off + t];
            aa[t] = w * A[off + t] + slope;
            cc[t] = w * c[off + t] - slope;
            add += prod * q[off + t];
            prod = w;
        }

        long[] g = f.clone();
        for (int x = 0; x < g.length; x++) if (g[x] < INF) g[x] = prod * g[x] + slope * x;
        long[] ans = rawBlock(g, aa, cc, 0, m, outMax);
        for (int x = 0; x < ans.length; x++) if (ans[x] < INF) ans[x] += add;
        return ans;
    }

    /**
     * 全要素が 1 の長さ n の long 配列を生成する。
     *
     * <p>時間計算量: O(n)</p>
     * <p>空間計算量: O(n)</p>
     * // 未テスト
     *
     * @param n 配列の長さ
     * @return 要素がすべて 1L の配列
     */
    static long[] ones(int n) {
        long[] a = new long[n];
        Arrays.fill(a, 1);
        return a;
    }

    /**
     * lowPart と highPart の結果を合成し、ブロックの出力配列を求める。
     *
     * <p>時間計算量: O(m * outMax + m^2)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return 合成後の関数値配列
     */
    static long[] rawBlock(long[] f, long[] A, long[] c, int off, int m, int outMax) {
        long[] lo = lowPart(f, A, c, off, m, outMax);
        long[] hi = highPart(f, A, c, off, m, outMax);
        for (int x = 0; x <= outMax; x++) lo[x] = Math.min(lo[x], hi[x]);
        return lo;
    }

    /**
     * 開始位置 y < m からの到達経路に対する DP 値を計算する。
     * 厳密表現 = 明示的な接頭辞 + O(m) 本の直線。
     *
     * <p>時間計算量: O(m * outMax + m^2)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return y < m からの DP 計算結果配列
     */
    static long[] lowPart(long[] f, long[] A, long[] c, int off, int m, int outMax) {
        int B = m;
        long[] v = Arrays.copyOf(f, m);
        long[] lm = new long[m], lb = new long[m];
        int ln = 0;

        for (int t = 0; t < m; t++) {
            long a = A[off + t], cc = c[off + t];
            long[] nv = new long[B + 1];
            Arrays.fill(nv, INF);
            long pref = INF;

            for (int x = 0; x <= B; x++) {
                if (x > 0 && v[x - 1] < INF) pref = Math.min(pref, v[x - 1] - a * (x - 1L));
                long z = x + 1 < B ? v[x + 1] : evalLines(lm, lb, ln, x + 1L);
                long best = z < INF ? z + cc : INF;
                if (pref < INF) best = Math.min(best, a * x + pref);
                nv[x] = best;
            }

            long addA = INF;
            for (int y = 0; y < B; y++) if (v[y] < INF)
                addA = Math.min(addA, v[y] - a * y);

            int atA = -1;
            for (int j = 0; j < ln; j++) {
                long slope = lm[j], b = lb[j];
                long nb = b + slope + cc;                  // g(x+1)+c
                if (slope < a) nb = Math.min(nb, b + a - slope);
                else addA = Math.min(addA, b + (slope - a) * B);
                lb[j] = nb;
                if (slope == a) atA = j;
            }
            if (addA < INF) {
                if (atA >= 0) lb[atA] = Math.min(lb[atA], addA);
                else { lm[ln] = a; lb[ln] = addA; ln++; }
            }
            v = nv;
            B++;
        }

        long[] res = new long[outMax + 1];
        Arrays.fill(res, INF);
        int z = Math.min(B, outMax + 1);
        System.arraycopy(v, 0, res, 0, z);
        if (B <= outMax && ln > 0) fillEnvelope(res, B, outMax, lm, lb, ln);
        return res;
    }

    /**
     * 直線群 { m[i] * x + b[i] } の x における最小値を評価する。
     *
     * <p>時間計算量: O(n)</p>
     * <p>空間計算量: O(1)</p>
     * // 未テスト
     *
     * @param m 傾きの配列
     * @param b 切片の配列
     * @param n 直線の個数
     * @param x 評価する x 座標
     * @return 最小値
     */
    static long evalLines(long[] m, long[] b, int n, long x) {
        long ans = INF;
        for (int i = 0; i < n; i++) ans = Math.min(ans, m[i] * x + b[i]);
        return ans;
    }

    /**
     * 増加する整数 x に対する直線群の下側エンベロープを計算し、配列に書き込む。
     *
     * <p>時間計算量: O(n log n + (to - from))</p>
     * <p>空間計算量: O(n)</p>
     * // 未テスト
     *
     * @param out 結果を出力する配列
     * @param from 開始 x 座標
     * @param to 終了 x 座標
     * @param m 傾きの配列
     * @param b 切片の配列
     * @param n 直線の個数
     */
    static void fillEnvelope(long[] out, int from, int to, long[] m, long[] b, int n) {
        Integer[] id = new Integer[n];
        for (int i = 0; i < n; i++) id[i] = i;
        Arrays.sort(id, (i, j) -> Long.compare(m[j], m[i])); // slope descending

        long[] hm = new long[n], hb = new long[n], start = new long[n];
        int hn = 0;
        for (int z = 0; z < n; z++) {
            long mm = m[id[z]], bb = b[id[z]];
            if (hn > 0 && hm[hn - 1] == mm) {
                if (bb >= hb[hn - 1]) continue;
                hn--;
            }
            long s = Long.MIN_VALUE / 4;
            while (hn > 0) {
                s = ceilDiv(bb - hb[hn - 1], hm[hn - 1] - mm);
                if (s > start[hn - 1]) break;
                hn--;
            }
            if (hn == 0) s = Long.MIN_VALUE / 4;
            hm[hn] = mm; hb[hn] = bb; start[hn] = s; hn++;
        }
        int p = 0;
        for (int x = from; x <= to; x++) {
            while (p + 1 < hn && start[p + 1] <= x) p++;
            out[x] = Math.min(out[x], hm[p] * x + hb[p]);
        }
    }

    /**
     * 切り上げ除算 ceil(a / b) を計算する (b > 0)。
     *
     * <p>時間計算量: O(1)</p>
     * <p>空間計算量: O(1)</p>
     * // 未テスト
     *
     * @param a 分子
     * @param b 分母 (b > 0)
     * @return 切り上げ除算結果
     */
    static long ceilDiv(long a, long b) { // b > 0
        long q = a / b, r = a % b;
        return r > 0 ? q + 1 : q;
    }

    /**
     * 開始位置 y >= m からの到達経路に対する DP 値を計算する。
     * このブロック内では境界は到達不可能である。
     *
     * <p>時間計算量: O(m * outMax)</p>
     * <p>空間計算量: O(outMax + m)</p>
     * // 未テスト
     *
     * @param f 入力関数値の配列
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @param outMax 出力範囲の上限インデックス
     * @return y >= m からの DP 計算結果配列
     */
    static long[] highPart(long[] f, long[] A, long[] c, int off, int m, int outMax) {
        long[] base = blockKernel(A, c, off, m);
        long minA = A[off];
        for (int i = 1; i < m; i++) minA = Math.min(minA, A[off + i]);

        long[] H = new long[outMax + 1];
        int lim = 2 * m;
        for (int r = 0; r <= outMax; r++)
            H[r] = r <= lim ? base[r] : base[lim] + (r - (long) lim) * minA;

        long[] ans = new long[outMax + 1];
        Arrays.fill(ans, INF);
        int inMax = f.length - 1;

        for (int px = 0; px < 2; px++) {
            int[] rows = parityRange(0, outMax, px);
            if (rows.length == 0) continue;
            for (int py = 0; py < 2; py++) {
                int[] cols = parityRange(m, inMax, py);
                if (cols.length == 0) continue;
                int[] arg = new int[outMax + 1];
                Matrix mat = (x, y) -> {
                    int r = x - y + m;
                    if (r < 0 || r > outMax || H[r] >= INF || f[y] >= INF) return INF;
                    return f[y] + H[r];
                };
                smawk(rows, cols, arg, mat);
                for (int x : rows) ans[x] = Math.min(ans[x], mat.get(x, arg[x]));
            }
        }
        return ans;
    }

    /**
     * ブロック内の相対変化量 R に対する基本コスト H(R) (0 <= R <= 2m) を計算する。
     * 1 ステップにつき r = 0 (コスト c) または r >= 2 (コスト (r - 1)A) が許容される。
     *
     * <p>時間計算量: O(m^2)</p>
     * <p>空間計算量: O(m)</p>
     * // 未テスト
     *
     * @param A 各ステップの A[i]
     * @param c 各ステップの c[i]
     * @param off ブロックの開始インデックス
     * @param m ブロックに含まれるステップ数
     * @return H(R) 配列
     */
    static long[] blockKernel(long[] A, long[] c, int off, int m) {
        int L = 2 * m;
        long[] dp = new long[L + 1];
        Arrays.fill(dp, INF);
        dp[0] = 0;
        for (int t = 0; t < m; t++) {
            long a = A[off + t], cc = c[off + t];
            long[] nd = new long[L + 1];
            Arrays.fill(nd, INF);
            long pref = INF;
            for (int r = 0; r <= L; r++) {
                if (r >= 2 && dp[r - 2] < INF)
                    pref = Math.min(pref, dp[r - 2] - (r - 2L) * a);
                if (dp[r] < INF) nd[r] = dp[r] + cc;
                if (pref < INF) nd[r] = Math.min(nd[r], (r - 1L) * a + pref);
            }
            dp = nd;
        }
        return dp;
    }

    /**
     * SMAWK アルゴリズムで使用する 2 次元行列インターフェース。
     * // 未テスト
     */
    interface Matrix {
        /**
         * 行 row、列 col の要素を取得する。
         *
         * <p>時間計算量: O(1)</p>
         * <p>空間計算量: O(1)</p>
         * // 未テスト
         *
         * @param row 行インデックス
         * @param col 列インデックス
         * @return 行列要素の値
         */
        long get(int row, int col);
    }

    /**
     * 全単調行列に対する SMAWK アルゴリズムを実行し、各行の最小値を与える列インデックスを求める。
     * 行列の各行の最小値を与える列のインデックスは O(rows + cols) 時間で計算される。
     *
     * <p>時間計算量: O(|rows| + |cols|)</p>
     * <p>空間計算量: O(|rows| + |cols|)</p>
     * // 未テスト
     *
     * @param rows 対象とする行インデックス列
     * @param cols 対象とする列インデックス列
     * @param ans 各行の最小列インデックスの格納先配列
     * @param a 全単調行列
     */
    static void smawk(int[] rows, int[] cols, int[] ans, Matrix a) {
        if (rows.length == 0) return;

        int[] st = new int[Math.min(rows.length, cols.length)];
        int z = 0;
        for (int col : cols) {
            while (z > 0 && a.get(rows[z - 1], col) < a.get(rows[z - 1], st[z - 1])) z--;
            if (z < rows.length) st[z++] = col;
        }
        int[] cs = Arrays.copyOf(st, z);
        int[] odd = new int[rows.length / 2];
        for (int i = 0; i < odd.length; i++) odd[i] = rows[2 * i + 1];
        smawk(odd, cs, ans, a);

        int left = 0;
        for (int i = 0; i < rows.length; i += 2) {
            int right = cs.length - 1;
            if (i + 1 < rows.length) {
                int target = ans[rows[i + 1]];
                right = left;
                while (cs[right] != target) right++;
            }
            int best = left;
            long bv = a.get(rows[i], cs[best]);
            for (int j = left + 1; j <= right; j++) {
                long v = a.get(rows[i], cs[j]);
                if (v < bv) { bv = v; best = j; }
            }
            ans[rows[i]] = cs[best];
            left = i + 1 < rows.length ? right : best;
        }
    }

    /**
     * 範囲 [lo, hi] 内で指定した偶奇 (parity) を持つ整数の配列を生成する。
     *
     * <p>時間計算量: O(hi - lo)</p>
     * <p>空間計算量: O(hi - lo)</p>
     * // 未テスト
     *
     * @param lo 範囲の下限
     * @param hi 範囲の上限
     * @param parity 偶奇 (0 または 1)
     * @return 偶奇にマッチする整数群の配列
     */
    static int[] parityRange(int lo, int hi, int parity) {
        int s = ((lo & 1) == parity) ? lo : lo + 1;
        if (s > hi) return new int[0];
        int[] a = new int[(hi - s) / 2 + 1];
        for (int i = 0; i < a.length; i++) a[i] = s + 2 * i;
        return a;
    }
}
