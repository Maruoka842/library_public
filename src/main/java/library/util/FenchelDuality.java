package library.util;

import java.util.ArrayList;
import java.util.List;
import library.util.geometry.LongPoint;

/**
 * Fenchel双対（凸共役 / Legendre-Fenchel変換）を計算するライブラリ。
 *
 * 1変数関数 f(x) に対し、そのFenchel双対（凸共役）f*(p) は以下のように定義される：
 * f*(p) = sup_x { px - f(x) } (p, x は整数)
 *
 * 区分線形関数 f(x) （折れ線関数、凸とは限らない、各区間の傾きは整数とは限らない）を入力とし、
 * その下側凸包（凸エンベロープ）の端点を用いて、Fenchel双対 f*(p) （これもまた区分線形凸関数）の
 * 最小限の端点（変化点 / 曲がる点）(p, f*(p)) の並んだ列（LongPoint[]）を O(N) 時間で正確に計算します。
 *
 * 入力制限・オーバーフロー：
 * 入力座標の絶対値（|x_i|, |y_i|）が 10^9 以下であれば、内部計算での 64ビット符号付き整数 (long) のオーバーフローは発生しません。
 */
public class FenchelDuality {

    /**
     * 与えられた整数列 f に対し、f 以下の最大の下に凸な整数列を返す。
     * すなわち、次の条件を満たす一意な整数列 g を返します。
     * 1. 任意の i に対し、g[i] <= f[i]
     * 2. 任意の i に対し、g[i] - g[i-1] <= g[i+1] - g[i] (下に凸)
     * 3. 1, 2 を満たす任意の整数列 h に対し、任意の i で h[i] <= g[i]

     * 前提:
     * 2 max(f) * (f.length - 1) <= Long.MAX_VALUE
     *
     * 計算量: O(N)
     *
     * @param f 入力整数列
     * @return f 以下の最大の下に凸な整数列
     */
    public static long[] integerFenchelBiconjugate(long[] f) {
    	//https://atcoder.jp/contests/abc459/submissions/78112542
    	//https://atcoder.jp/contests/arc130/submissions/78119273
        int N = f.length;
        if (N <= 2) {
            return f.clone();
        }

        LongPoint[] pts = new LongPoint[N];
        for (int i = 0; i < N; i++) {
            pts[i] = new LongPoint(i, f[i]);
        }
        List<LongPoint> hullPoints = getLowerHull(pts);

        boolean[] onHull = new boolean[N];
        for (LongPoint p : hullPoints) {
            int x = (int) p.x();
            if (x >= 0 && x < N) {
                onHull[x] = true;
            }
        }

        int[] hull = new int[N];
        int hullSize = 0;
        for (int i = 0; i < N; i++) {
            if (!onHull[i]) {
                continue;
            }
            while (hullSize >= 2) {
                int u = hull[hullSize - 2];
                int v = hull[hullSize - 1];
                int w = i;
                long dy1 = f[v] - f[u];
                long dx2 = w - v;
                long dy2 = f[w] - f[v];
                long dx1 = v - u;
                if (dy1 * dx2 >= dy2 * dx1) {
                    hullSize--;
                } else {
                    break;
                }
            }
            hull[hullSize++] = i;
        }

        long[] g = new long[N];
        int j = 0;
        long S1 = 0, S2 = 0;
        int lastJ = -1;

        for (int x = 0; x < N; x++) {
            while (x > hull[j + 1]) {
                j++;
            }
            if (j != lastJ) {
                long dy = f[hull[j + 1]] - f[hull[j]];
                long dx = hull[j + 1] - hull[j];
                S1 = Math.floorDiv(dy, dx);
                S2 = -Math.floorDiv(-dy, dx);
                lastJ = j;
            }
            long val1 = S1 * (x - hull[j]) + f[hull[j]];
            long val2 = S2 * (x - hull[j + 1]) + f[hull[j + 1]];
            g[x] = Math.max(val1, val2);
        }

        return g;
    }

    /**
     * 端点 LongPoint[] で定義される区分線形関数 f(x) （凸とは限らない、傾き整数とは限らない）に対し、
     * その Fenchel双対 f*(p) = max_x { px - f(x) } を表す最小限の端点（曲がる点）(p, f*(p)) の列を計算します。
     *
     * 事前条件：
     * 1. pts の x 座標は厳密に単調増加： pts[0].x() < pts[1].x() < ... < pts[N-1].x()
     *
     * 計算量: O(N) (下側凸包構築 O(N)、二分探索及び線形走査 O(N))
     *
     * @param pts 端点 LongPoint の配列 (長さ N >= 2)
     * @return Fenchel双対関数の端点（変化点）の配列
     */
    // 未テスト
    public static LongPoint[] fenchelConjugate(LongPoint[] pts) {
        int N = pts.length;
        if (N < 2) {
            throw new IllegalArgumentException("N must be >= 2");
        }
        // Step 1: 下側凸包（凸エンベロープ）の構築
        List<LongPoint> hull = getLowerHull(pts);
        int M = hull.size();

        // Step 2: 変化点が発生しうる傾き p の候補（各区間の傾きの床と天井）を収集
        // 下側凸包の隣接頂点間の傾きは厳密に単調増加するため、床・天井の列もソート済みとなる。
        // これにより、ソートを行うことなく O(N) で候補を収集・一意化できます。
        List<Long> sortedP = new ArrayList<>();
        for (int i = 0; i < M - 1; i++) {
            LongPoint pA = hull.get(i);
            LongPoint pB = hull.get(i + 1);
            long dx = pB.x() - pA.x();
            long dy = pB.y() - pA.y();
            long floor = Math.floorDiv(dy, dx);
            long ceil = -Math.floorDiv(-dy, dx);

            if (sortedP.isEmpty() || sortedP.get(sortedP.size() - 1) < floor) {
                sortedP.add(floor);
            }
            if (sortedP.isEmpty() || sortedP.get(sortedP.size() - 1) < ceil) {
                sortedP.add(ceil);
            }
        }

        // Step 3: 各候補 p に対する Fenchel双対の値 q = f*(p) を尺取り法で計算
        int K = sortedP.size();
        LongPoint[] conjPts = new LongPoint[K];
        int idx = 0; // 現在の最適な凸包の頂点インデックス
        for (int i = 0; i < K; i++) {
            long p = sortedP.get(i);
            while (idx < M - 1) {
                LongPoint pA = hull.get(idx);
                LongPoint pB = hull.get(idx + 1);
                long dx = pB.x() - pA.x();
                long dy = pB.y() - pA.y();
                if (p * dx >= dy) {
                    idx++;
                } else {
                    break;
                }
            }
            LongPoint opt = hull.get(idx);
            conjPts[i] = new LongPoint(p, p * opt.x() - opt.y());
        }

        // Step 4: 一直線上にある（傾きが変化しない）冗長な点を除去
        // Fenchel双対関数の任意の区間の傾きは常に整数（元の関数のx座標）となるため、
        // 傾きは正確な整数除算 dy / dx でオーバーフローなく計算可能です。
        List<LongPoint> filtered = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            if (filtered.size() < 2) {
                filtered.add(conjPts[i]);
            } else {
                LongPoint p1 = filtered.get(filtered.size() - 2);
                LongPoint p2 = filtered.get(filtered.size() - 1);
                LongPoint p3 = conjPts[i];

                long dy1 = p2.y() - p1.y();
                long dx1 = p2.x() - p1.x();
                long dy2 = p3.y() - p2.y();
                long dx2 = p3.x() - p2.x();
                if (dy1 / dx1 == dy2 / dx2) {
                    filtered.set(filtered.size() - 1, p3);
                } else {
                    filtered.add(p3);
                }
            }
        }

        return filtered.toArray(new LongPoint[0]);
    }

    /**
     * 端点 (x_i, y_i) で定義される区分線形関数 f(x) （凸とは限らない、傾き整数とは限らない）に対し、
     * その Fenchel双対 f*(p) = max_x { px - f(x) } を表す最小限の端点（曲がる点）(p, f*(p)) の列を計算します。
     *
     * 事前条件：
     * 1. x は厳密に単調増加： x_0 < x_1 < ... < x_(N-1)
     *
     * @param x 端点の x 座標の配列 (長さ N >= 2)
     * @param y 端点の y 座標の配列 (長さ N >= 2)
     * @return Fenchel双対の端点のリスト
     */
    // 未テスト
    public static LongPoint[] fenchelConjugate(long[] x, long[] y) {
        int N = x.length;
        if (N < 2) {
            throw new IllegalArgumentException("N must be >= 2");
        }
        LongPoint[] pts = new LongPoint[N];
        for (int i = 0; i < N; i++) {
            pts[i] = new LongPoint(x[i], y[i]);
        }
        return fenchelConjugate(pts);
    }

    private static List<LongPoint> getLowerHull(LongPoint[] pts) {
        int N = pts.length;
        List<LongPoint> hull = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            while (hull.size() >= 2) {
                LongPoint p1 = hull.get(hull.size() - 2);
                LongPoint p2 = hull.get(hull.size() - 1);
                LongPoint p3 = pts[i];
                long dy1 = p2.y() - p1.y();
                long dx1 = p2.x() - p1.x();
                long dy2 = p3.y() - p2.y();
                long dx2 = p3.x() - p2.x();
                if (dy2 * dx1 < dy1 * dx2) {
                    hull.remove(hull.size() - 1);
                } else {
                    break;
                }
            }
            hull.add(pts[i]);
        }
        return hull;
    }
}
