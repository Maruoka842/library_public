package library.util.graph;
import java.util.*;
/**
 * 平面グラフの s-t 最小カット(= 最大流)を双対グラフの最短路で求める。
 *
 * 前提: s と t が共通の面に属する(例: 両方が外周上にある)。
 * 方法: その面の中に s→t の辺(チャード)を追加すると面が 2 つ(fa, fb)に分かれる。
 *       s-t を分離する閉曲線はチャードをちょうど1回横切るので、
 *       最小カット = 双対での fa → fb の最短路(チャードは通らない)。
 *
 * 容量は cap[dart] = そのダートの向き(from→to)の容量。
 *   無向: cap[2i] = cap[2i+1] = c_i。有向 u→v: cap[2i] = c, cap[2i+1] = 0。
 * 双対ダート y(右の面 → 左の面)の重みは cap[y^1]。
 *   (最短路の進行方向の右側が s 側になる向きなので、s 側→t 側へ向かう元ダートは y^1)
 *
 * 計算量: O(m log m)。
 */
public final class PlanarFlow {
    private PlanarFlow() {}

    public static final class Cut {
        /** 最小カットの値(= 最大流量)。 */
        public final long value;
        /** カットに含まれる元ダート(s 側 → t 側の向き、容量 > 0 のもの)。容量の総和 = value。 */
        public final int[] darts;

        Cut(long value, int[] darts) { this.value = value; this.darts = darts; }
    }

    /**
     * @param g   平面地図(連結成分ごとに球面埋め込み。s,t は同じ成分で共通の面を持つこと)
     * @param cap 長さ 2*m。cap[e] >= 0
     * @throws IllegalArgumentException s == t、または s,t が共通の面を持たない場合
     */
    public static Cut minCut(PlanarMap g, long[] cap, int s, int t) {
        int n = g.vertexCount(), m = g.edgeCount();
        if (s == t) throw new IllegalArgumentException("s == t");
        if (s < 0 || t < 0 || s >= n || t >= n) throw new IllegalArgumentException("vertex out of range");
        if (cap.length != 2 * m) throw new IllegalArgumentException("cap.length must be 2*m");

        // s, t の角を持つ共通の面を探す
        PlanarMap.Faces f0 = g.faces();
        int[] fs = new int[f0.count], ft = new int[f0.count];
        Arrays.fill(fs, -1);
        Arrays.fill(ft, -1);
        for (int e = 0; e < 2 * m; e++) {
            if (g.to(e) == s) fs[f0.id[e]] = e;
            if (g.to(e) == t) ft[f0.id[e]] = e;
        }
        int a = -1, b = -1;
        for (int f = 0; f < f0.count; f++) {
            if (fs[f] >= 0 && ft[f] >= 0) { a = fs[f]; b = ft[f]; break; }
        }
        if (a < 0) throw new IllegalArgumentException("s and t do not share a face");

        // 面の中に s→t のチャードを追加。ダート 2m が s→t。
        PlanarMap h = g.copy();
        h.addEdgeInFace(a, b);
        PlanarMap.Faces f = h.faces();
        int src = f.id[2 * m];       // チャードの右の面
        int dst = f.id[2 * m + 1];   // チャードの左の面

        final long INF = Long.MAX_VALUE / 4;
        long[] dist = new long[f.count];
        int[] prev = new int[f.count]; // その面に入ってきた双対ダート
        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);
        dist[src] = 0;
        PriorityQueue<long[]> pq = new PriorityQueue<>((x, y) -> Long.compare(x[0], y[0]));
        pq.add(new long[]{0, src});
        while (!pq.isEmpty()) {
            long[] cur = pq.poll();
            int u = (int) cur[1];
            if (cur[0] > dist[u]) continue;
            if (u == dst) break;
            for (int p = f.start[u]; p < f.start[u + 1]; p++) {
                int y = f.darts[p];
                if ((y >> 1) == m) continue; // チャードは通らない
                int v = f.id[y ^ 1];
                long nd = cur[0] + cap[y ^ 1];
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = y;
                    pq.add(new long[]{nd, v});
                }
            }
        }

        // 復元
        ArrayList<Integer> cut = new ArrayList<>();
        for (int v = dst; v != src; ) {
            int y = prev[v];
            if (cap[y ^ 1] > 0) cut.add(y ^ 1);
            v = f.id[y];
        }
        int[] darts = new int[cut.size()];
        for (int i = 0; i < darts.length; i++) darts[i] = cut.get(i);
        return new Cut(dist[dst], darts);
    }

    /** 無向グラフ用の補助: 辺 i の容量 c[i] から cap[] を作る。 */
    public static long[] undirectedCaps(long[] c) {
        long[] cap = new long[2 * c.length];
        for (int i = 0; i < c.length; i++) cap[2 * i] = cap[2 * i + 1] = c[i];
        return cap;
    }
}
