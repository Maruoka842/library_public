package library.util.graph;
import java.util.*;

/**
 * 平面グラフの組合せ地図(回転系)による表現。座標は持たない。
 *
 * ダート(向き付き半辺): 辺 i は 2i (u->v) と 2i+1 (v->u)。alpha(e) = e^1。
 * sg[e] (= σ): e の始点まわりで反時計回りに次のダート。
 * phi(e) = sg[e^1]: e の右側の面を回って進む次のダート。
 *   - 頂点 = σ のサイクル、面 = φ のサイクル。
 *   - 双対は σ を φ に置き換えるだけ(dual())。
 *   - 球面上の埋め込みとして扱うので外面の区別はない。鏡像(σ の向き反転)も有効。
 *
 * 連結成分ごとに独立した球面埋め込みと見なす。
 * 平面性(種数0)の検証: n - m + F == 2 * (連結成分数)  → isPlanar()
 */
public final class PlanarMap {
    private int n, m;
    private int[] from; // ダート -> 始点
    private int[] sg;   // σ
    private int[] first; // 頂点 -> 任意の出ダート (孤立頂点は -1)

    private PlanarMap(int n, int m) {
        this.n = n;
        this.m = m;
        from = new int[Math.max(2 * m, 4)];
        sg = new int[Math.max(2 * m, 4)];
        first = new int[Math.max(n, 2)];
        Arrays.fill(first, -1);
    }

    // ---------------------------------------------------------------- 構築

    /** 辺リストから構築。各頂点の回転順 = その頂点に接続する辺の出現順(反時計回りとみなす)。木ならこれで常に平面埋め込み。 */
    public static PlanarMap fromEdges(int n, int[] us, int[] vs) {
        int m = us.length;
        PlanarMap g = new PlanarMap(n, m);
        for (int i = 0; i < m; i++) {
            g.from[2 * i] = us[i];
            g.from[2 * i + 1] = vs[i];
        }
        int[] tail = new int[n];
        for (int d = 0; d < 2 * m; d++) {
            int v = g.from[d];
            if (g.first[v] < 0) {
                g.first[v] = d;
                g.sg[d] = d;
            } else {
                g.sg[tail[v]] = d;
                g.sg[d] = g.first[v];
            }
            tail[v] = d;
        }
        return g;
    }

    /** 回転を明示して構築。rot[v] = v を出るダート(2i or 2i+1)を反時計回りに並べたもの。 */
    public static PlanarMap fromRotations(int n, int[] us, int[] vs, int[][] rot) {
        int m = us.length;
        PlanarMap g = new PlanarMap(n, m);
        for (int i = 0; i < m; i++) {
            g.from[2 * i] = us[i];
            g.from[2 * i + 1] = vs[i];
        }
        boolean[] seen = new boolean[2 * m];
        int total = 0;
        for (int v = 0; v < n; v++) {
            int[] r = rot[v];
            int k = r.length;
            for (int i = 0; i < k; i++) {
                int d = r[i];
                if (g.from[d] != v || seen[d]) throw new IllegalArgumentException("bad rotation at vertex " + v);
                seen[d] = true;
                g.sg[d] = r[(i + 1) % k];
                if (i == 0) g.first[v] = d;
            }
            total += k;
        }
        if (total != 2 * m) throw new IllegalArgumentException("rotation does not cover all darts");
        return g;
    }

    /** h×w の格子グラフ(全マス)。頂点 id = i*w + j。 */
    public static PlanarMap grid(int h, int w) {
        boolean[][] open = new boolean[h][w];
        for (boolean[] r : open) Arrays.fill(r, true);
        return grid(open);
    }

    /** 開いているマスを頂点とする格子グラフ。頂点 id は開いているマスを行優先で数えた通し番号。 */
    public static PlanarMap grid(boolean[][] open) {
        int h = open.length, w = h == 0 ? 0 : open[0].length;
        int[][] id = new int[h][w];
        int cnt = 0;
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++) id[i][j] = open[i][j] ? cnt++ : -1;
        int[] us = new int[2 * h * w], vs = new int[2 * h * w];
        int[][] hE = new int[h][w], vE = new int[h][w];
        for (int[] r : hE) Arrays.fill(r, -1);
        for (int[] r : vE) Arrays.fill(r, -1);
        int m = 0;
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++) {
                if (id[i][j] < 0) continue;
                if (j + 1 < w && id[i][j + 1] >= 0) {
                    hE[i][j] = m; us[m] = id[i][j]; vs[m] = id[i][j + 1]; m++;
                }
                if (i + 1 < h && id[i + 1][j] >= 0) {
                    vE[i][j] = m; us[m] = id[i][j]; vs[m] = id[i + 1][j]; m++;
                }
            }
        int[][] rot = new int[cnt][];
        int[] tmp = new int[4];
        for (int i = 0; i < h; i++)
            for (int j = 0; j < w; j++) {
                if (id[i][j] < 0) continue;
                int k = 0;
                if (hE[i][j] >= 0) tmp[k++] = 2 * hE[i][j];                       // 右
                if (i > 0 && vE[i - 1][j] >= 0) tmp[k++] = 2 * vE[i - 1][j] + 1; // 上
                if (j > 0 && hE[i][j - 1] >= 0) tmp[k++] = 2 * hE[i][j - 1] + 1; // 左
                if (vE[i][j] >= 0) tmp[k++] = 2 * vE[i][j];                       // 下
                rot[id[i][j]] = Arrays.copyOf(tmp, k);
            }
        return fromRotations(cnt, Arrays.copyOf(us, m), Arrays.copyOf(vs, m), rot);
    }

    public PlanarMap copy() {
        PlanarMap c = new PlanarMap(n, m);
        c.from = from.clone();
        c.sg = sg.clone();
        c.first = first.clone();
        return c;
    }

    // ---------------------------------------------------------------- 基本クエリ

    public int vertexCount() { return n; }
    public int edgeCount() { return m; }
    public int from(int e) { return from[e]; }
    public int to(int e) { return from[e ^ 1]; }
    public int sigma(int e) { return sg[e]; }
    public int phi(int e) { return sg[e ^ 1]; }
    /** v を出るダートの1つ(孤立頂点は -1)。σ をたどると v の回転が一周できる。 */
    public int firstDart(int v) { return first[v]; }

    public int degree(int v) {
        int s = first[v];
        if (s < 0) return 0;
        int c = 0, x = s;
        do { c++; x = sg[x]; } while (x != s);
        return c;
    }

    // ---------------------------------------------------------------- 面

    public static final class Faces {
        /** ダートを持つ面の数(孤立頂点だけの成分の面は含まない)。 */
        public final int count;
        /** id[e] = ダート e の右側の面。 */
        public final int[] id;
        /** 面 f のダートは darts[start[f] .. start[f+1]) に φ の順で並ぶ。 */
        public final int[] start, darts;

        Faces(int count, int[] id, int[] start, int[] darts) {
            this.count = count; this.id = id; this.start = start; this.darts = darts;
        }

        public int size(int f) { return start[f + 1] - start[f]; }
    }

    public Faces faces() {
        int M = 2 * m;
        int[] id = new int[M];
        Arrays.fill(id, -1);
        int[] start = new int[M + 1];
        int[] darts = new int[M];
        int cnt = 0, p = 0;
        for (int e = 0; e < M; e++) {
            if (id[e] >= 0) continue;
            start[cnt] = p;
            int x = e;
            do {
                id[x] = cnt;
                darts[p++] = x;
                x = sg[x ^ 1];
            } while (x != e);
            cnt++;
        }
        start[cnt] = p;
        return new Faces(cnt, id, Arrays.copyOf(start, cnt + 1), darts);
    }

    // ---------------------------------------------------------------- 検証・双対

    /** 全連結成分で種数0(球面に埋め込めている)か。n - m + F == 2C で判定。 */
    public boolean isPlanar() {
        int[] par = new int[n];
        for (int i = 0; i < n; i++) par[i] = i;
        int comps = n;
        for (int i = 0; i < m; i++) {
            int a = find(par, from[2 * i]), b = find(par, from[2 * i + 1]);
            if (a != b) { par[a] = b; comps--; }
        }
        int isolated = 0;
        for (int v = 0; v < n; v++) if (first[v] < 0) isolated++;
        int F = faces().count + isolated;
        return n - m + F == 2 * comps;
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
        return x;
    }

    /**
     * 双対地図。双対の頂点 = 元の面(faces() の id)、双対の辺 i は元の辺 i に対応し、
     * 双対ダート e は「e の右の面 → e の左の面」。dual().dual() は元と同じ σ に戻る。
     * 孤立頂点があると定義できないので例外。
     */
    public PlanarMap dual() {
        for (int v = 0; v < n; v++)
            if (first[v] < 0) throw new IllegalStateException("isolated vertex " + v);
        Faces f = faces();
        PlanarMap d = new PlanarMap(f.count, m);
        for (int e = 0; e < 2 * m; e++) {
            d.from[e] = f.id[e];
            d.sg[e] = sg[e ^ 1];
        }
        for (int k = 0; k < f.count; k++) d.first[k] = f.darts[f.start[k]];
        return d;
    }

    // ---------------------------------------------------------------- 編集(挿入のみ O(1))

    /**
     * ダート a の右側の面の中で、a の終点と b の終点を結ぶ辺を追加する。返り値は新しい辺の id。
     * 前提: a と b は同じ面(φ のサイクル)に属する。破ると種数が上がる(isPlanar() が false になる)。
     * 面は 2 つに分かれる。a == b なら a の終点に自己ループができる。
     */
    public int addEdgeInFace(int a, int b) {
        int u = from[a ^ 1], v = from[b ^ 1];
        int d = newEdge(u, v);
        insertAfter(a ^ 1, d);
        insertAfter(b ^ 1, d ^ 1);
        return d >> 1;
    }

    /** ダート a の右側の面に、a の終点から出る新しい葉(新頂点)を追加する。返り値は新頂点の id。 */
    public int addLeaf(int a) {
        int u = from[a ^ 1];
        if (n + 1 > first.length) {
            int old = first.length;
            first = Arrays.copyOf(first, old * 2);
            Arrays.fill(first, old, first.length, -1);
        }
        int w = n++;
        int d = newEdge(u, w);
        insertAfter(a ^ 1, d);
        sg[d ^ 1] = d ^ 1;
        first[w] = d ^ 1;
        return w;
    }

    private int newEdge(int u, int v) {
        while (2 * (m + 1) > from.length) {
            from = Arrays.copyOf(from, from.length * 2);
            sg = Arrays.copyOf(sg, sg.length * 2);
        }
        int e = m++;
        from[2 * e] = u;
        from[2 * e + 1] = v;
        return 2 * e;
    }

    /** σ 上でダート x の直後にダート d を挿入。 */
    private void insertAfter(int x, int d) {
        sg[d] = sg[x];
        sg[x] = d;
    }
}