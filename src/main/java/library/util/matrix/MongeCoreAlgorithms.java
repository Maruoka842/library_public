package library.util.matrix;

import java.util.Arrays;
import library.util.segtree.LazySegTreelonglong;
import library.util.segtree.SegTreeFactory;

/**
 * core atom を直接利用する Monge / anti-Monge 用アルゴリズム。
 *
 * // 未テスト
 */
public final class MongeCoreAlgorithms {
    private MongeCoreAlgorithms() {}

    /**
     * MongeCoreLong 行列の全行の最小値およびその最左 argmin を求める。
     *
     * @param a MongeCoreLong 行列
     * @return 各行の最小値と最左 argmin
     * @complexity O(rows + (cols + K) α(cols))
     */
    public static RowExtrema rowMinima(MongeCoreLong a) {
        return sweepWhole(a.data, -1, false);
    }

    /**
     * MongeCoreLong 行列の全行の最大値およびその最左 argmax を求める。
     *
     * @param a MongeCoreLong 行列
     * @return 各行の最大値と最左 argmax
     * @complexity O(rows + (cols + K) α(cols))
     */
    public static RowExtrema rowMaxima(MongeCoreLong a) {
        return sweepWhole(a.data, -1, true);
    }

    /**
     * AntiMongeCoreLong 行列の全行の最小値およびその最左 argmin を求める。
     *
     * @param a AntiMongeCoreLong 行列
     * @return 各行の最小値と最左 argmin
     * @complexity O(rows + (cols + K) α(cols))
     */
    public static RowExtrema rowMinima(AntiMongeCoreLong a) {
        return sweepWhole(a.data, +1, false);
    }

    /**
     * AntiMongeCoreLong 行列の全行の最大値およびその最左 argmax を求める。
     *
     * @param a AntiMongeCoreLong 行列
     * @return 各行の最大値と最左 argmax
     * @complexity O(rows + (cols + K) α(cols))
     */
    public static RowExtrema rowMaxima(AntiMongeCoreLong a) {
        return sweepWhole(a.data, +1, true);
    }

    /**
     * MongeCoreLong 行列の各行指定区間 [from[i], to[i]) における最小値を求める。
     */
    public static RowExtrema rowMinima(MongeCoreLong a, int[] from, int[] to) {
        return sweepRange(a.data, -1, from, to, false);
    }

    /**
     * MongeCoreLong 行列の各行指定区間 [from[i], to[i]) における最大値を求める。
     */
    public static RowExtrema rowMaxima(MongeCoreLong a, int[] from, int[] to) {
        return sweepRange(a.data, -1, from, to, true);
    }

    /**
     * AntiMongeCoreLong 行列の各行指定区間 [from[i], to[i]) における最小値を求める。
     */
    public static RowExtrema rowMinima(AntiMongeCoreLong a, int[] from, int[] to) {
        return sweepRange(a.data, +1, from, to, false);
    }

    /**
     * AntiMongeCoreLong 行列の各行指定区間 [from[i], to[i]) における最大値を求める。
     */
    public static RowExtrema rowMaxima(AntiMongeCoreLong a, int[] from, int[] to) {
        return sweepRange(a.data, +1, from, to, true);
    }

    /**
     * 各行全体の extremum を求める。
     *
     * atom (r,c,w) による行 r から r+1 への変化は
     * suffix [c+1, cols) への sign*w である。
     *
     * extremum に有利な suffix 更新だけからなる場合はそのまま処理する。
     * 不利な suffix 更新の場合は列を反転し、
     *
     * prefix ±w = 全体 ±w + suffix ∓w
     *
     * と変換して処理する。
     *
     * @complexity O(rows + (cols + K) α(cols))
     */
    private static RowExtrema sweepWhole(MongeCoreData d, int sign, boolean max) {
        long[] value = new long[d.rows];
        int[] arg = new int[d.rows];

        if (d.rows == 0) return new RowExtrema(value, arg);

        if (d.cols == 0) {
            Arrays.fill(value, max ? Long.MIN_VALUE : Long.MAX_VALUE);
            Arrays.fill(arg, -1);
            return new RowExtrema(value, arg);
        }

        /*
         * sign*w が extremum に有利な更新なら true。
         *
         * min : -w が有利
         * max : +w が有利
         */
        boolean direct = (sign > 0) == max;

        long[] initial = new long[d.cols];
        if (direct) {
            System.arraycopy(d.colPotential, 0, initial, 0, d.cols);
        } else {
            for (int j = 0; j < d.cols; j++) {
                initial[j] = d.colPotential[d.cols - 1 - j];
            }
        }

        /*
         * 列を反転した場合、元の最左 arg は反転後の最右 arg に対応する。
         */
        SuffixExtremumDSU ds = new SuffixExtremumDSU(initial, max, !direct);

        long common = 0;

        for (int i = 0; i < d.rows; i++) {
            value[i] = d.rowPotential[i] + common + ds.value();

            int j = ds.arg();
            arg[i] = direct ? j : d.cols - 1 - j;

            if (i + 1 == d.rows) continue;

            for (int e = d.rowBegin[i]; e < d.rowBegin[i + 1]; e++) {
                int c = d.atomCol[e];
                long w = d.atomWeight[e];

                if (direct) {
                    ds.improveSuffix(c + 1, w);
                } else {
                    /*
                     * 元の suffix への不利な更新を反転すると prefix への不利な更新。
                     *
                     * prefix sign*w
                     * = 全体 sign*w
                     *   + extremum に有利な suffix 更新
                     */
                    common += sign * w;
                    ds.improveSuffix(d.cols - c - 1, w);
                }
            }
        }

        return new RowExtrema(value, arg);
    }

    /**
     * suffix を extremum に有利な方向へ変化させながら、
     * 全体の extremum とその arg を管理する。
     *
     * min の場合は suffix -= w、
     * max の場合は suffix += w を行う。
     *
     * extremum の候補は右から見た suffix-extremum の位置だけである。
     * 候補が一度隣の候補に負けると、その後復活することはないため削除できる。
     */
    private static final class SuffixExtremumDSU {
        final boolean max;
        final boolean rightmost;

        /** 生存候補の successor を求める。 */
        final SuccessorDSU successor;

        /** 候補列における直前の候補。 */
        final int[] prev;

        /**
         * 候補 q と prev[q] の extremum 方向の差。
         *
         * min : a[q] - a[prev[q]]
         * max : a[prev[q]] - a[q]
         *
         * 生存中は非負。
         */
        final long[] gap;

        int head;
        long headValue;

        /**
         * @param a 初期配列
         * @param max 最大値を管理するなら true
         * @param rightmost tie のとき右端を選ぶなら true
         */
        SuffixExtremumDSU(long[] a, boolean max, boolean rightmost) {
            this.max = max;
            this.rightmost = rightmost;

            int n = a.length;
            boolean[] record = new boolean[n];

            /*
             * extremum の候補を右から列挙する。
             *
             * 最左を選ぶ場合、同値の左側も候補として残す。
             * 最右を選ぶ場合、同値の左側は不要。
             */
            boolean has = false;
            long best = 0;

            for (int i = n - 1; i >= 0; i--) {
                boolean ok = !has || (
                        max
                        ? rightmost ? a[i] > best : a[i] >= best
                        : rightmost ? a[i] < best : a[i] <= best
                );

                if (ok) {
                    record[i] = true;
                    best = a[i];
                    has = true;
                }
            }

            successor = new SuccessorDSU(record);

            prev = new int[n];
            Arrays.fill(prev, -1);
            gap = new long[n];

            int last = -1;

            for (int i = 0; i < n; i++) {
                if (!record[i]) continue;

                if (last < 0) {
                    head = i;
                    headValue = a[i];
                } else {
                    prev[i] = last;
                    gap[i] = max
                            ? a[last] - a[i]
                            : a[i] - a[last];
                }

                last = i;
            }
        }

        /**
         * suffix [from,n) を extremum に w だけ有利にする。
         *
         * min なら suffix -= w、
         * max なら suffix += w。
         */
        void improveSuffix(int from, long w) {
            if (from >= prev.length) return;

            int q = successor.next(from);
            if (q == prev.length) return;

            /*
             * head 自身から右を全部動かすなら
             * global extremum もそのまま w だけ動く。
             */
            if (prev[q] < 0) {
                headValue += max ? w : -w;
                return;
            }

            gap[q] -= w;

            /*
             * q が直前候補 r に勝ったら r は永久に不要。
             *
             * 最左 tie:
             *     同値なら r を残すので gap < 0 で削除。
             *
             * 最右 tie:
             *     同値でも q を選ぶので gap <= 0 で削除。
             */
            while (prev[q] >= 0
                    && (rightmost ? gap[q] <= 0 : gap[q] < 0)) {

                int r = prev[q];
                int p = prev[r];

                successor.erase(r);

                if (p < 0) {
                    /*
                     * r が head だった。
                     *
                     * gap[q] は更新後の
                     * q と r の extremum 方向の差。
                     */
                    headValue += max ? -gap[q] : gap[q];

                    prev[q] = -1;
                    gap[q] = 0;
                    head = q;
                    return;
                }

                /*
                 * p -> r -> q を p -> q に縮約する。
                 */
                gap[q] += gap[r];
                prev[q] = p;
            }
        }

        long value() {
            return headValue;
        }

        int arg() {
            return head;
        }
    }

    /**
     * 削除のみ可能な整数集合に対し、
     * x 以上の最小の未削除要素を求める。
     *
     * Union-Find の各連結成分に最大 index を持たせる。
     * n は番兵として常に残す。
     */
    private static final class SuccessorDSU {
        final int[] parent;
        final int[] size;
        final int[] max;

        /**
         * @param active 最初に残す要素
         */
        SuccessorDSU(boolean[] active) {
            int n = active.length;

            parent = new int[n + 1];
            size = new int[n + 1];
            max = new int[n + 1];

            for (int i = 0; i <= n; i++) {
                parent[i] = i;
                size[i] = 1;
                max[i] = i;
            }

            for (int i = 0; i < n; i++) {
                if (!active[i]) erase(i);
            }
        }

        int find(int x) {
            int r = x;
            while (parent[r] != r) r = parent[r];

            while (parent[x] != x) {
                int y = parent[x];
                parent[x] = r;
                x = y;
            }

            return r;
        }

        int next(int x) {
            return max[find(x)];
        }

        void erase(int x) {
            int a = find(x);
            int b = find(x + 1);

            if (a == b) return;

            if (size[a] < size[b]) {
                int t = a;
                a = b;
                b = t;
            }

            parent[b] = a;
            size[a] += size[b];
            max[a] = Math.max(max[a], max[b]);
        }
    }

    /**
     * 行を上から順に sweep し、各行の指定区間の最小/最大と左端 arg を求める。
     *
     * 任意区間版では SegTreeFactory の add_min / add_max セグ木を使用する。
     *
     * @complexity O((rows + K) log cols)
     */
    private static RowExtrema sweepRange(
            MongeCoreData d,
            int sign,
            int[] from,
            int[] to,
            boolean max) {

        if ((from == null) != (to == null)) throw new NullPointerException();

        if (from != null && (from.length != d.rows || to.length != d.rows)) {
            throw new IllegalArgumentException("length mismatch");
        }

        if (d.rows == 0) {
            return new RowExtrema(new long[0], new int[0]);
        }

        long[] initial = new long[d.cols];
        for (int j = 0; j < d.cols; j++) {
            initial[j] = d.rowPotential[0] + d.colPotential[j];
        }

        LazySegTreelonglong seg = max ? SegTreeFactory.add_max(d.cols) : SegTreeFactory.add_min(d.cols);
        seg.build(initial);

        long[] value = new long[d.rows];
        int[] arg = new int[d.rows];

        for (int i = 0; i < d.rows; i++) {
            int l = from == null ? 0 : from[i];
            int r = to == null ? d.cols : to[i];

            if (l < 0 || l > r || r > d.cols) {
                throw new IndexOutOfBoundsException("row=" + i);
            }

            if (l == r) {
                value[i] = max ? Long.MIN_VALUE : Long.MAX_VALUE;
                arg[i] = -1;
            } else {
                long v = seg.fold(l, r);
                value[i] = v;
                arg[i] = max ? seg.maximalRight(l, x -> x < v) : seg.maximalRight(l, x -> x > v);
            }

            if (i + 1 == d.rows) continue;

            long base = d.rowPotential[i + 1] - d.rowPotential[i];
            if (base != 0) seg.act(0, d.cols, base);

            for (int e = d.rowBegin[i]; e < d.rowBegin[i + 1]; e++) {
                int l2 = d.atomCol[e] + 1;
                if (l2 < d.cols) {
                    seg.act(l2, d.cols, sign * d.atomWeight[e]);
                }
            }
        }

        return new RowExtrema(value, arg);
    }

    public static final class RowExtrema {
        /** 各行の extremum。空区間は min なら MAX_VALUE、max なら MIN_VALUE。 */
        public final long[] value;

        /** 各行の最左 argmin / argmax。空区間は -1。 */
        public final int[] arg;

        RowExtrema(long[] value, int[] arg) {
            this.value = value;
            this.arg = arg;
        }
    }
}
