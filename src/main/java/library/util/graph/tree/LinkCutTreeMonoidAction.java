package library.util.graph.tree;

import java.util.Arrays;
import library.util.algebra.strategy.longs.LongMonoidStrategy;
import library.util.algebra.strategy.longs.LongMonoidActionStrategy;

/**
 * モノイド作用・区間作用・点更新対応リンクカット木 (Link-Cut Tree with Monoid Path Aggregate and Lazy Path Action)
 * 動的な森の状態を管理し、辺の追加・削除、パス上の非可換モノイド積の計算、点更新、およびパスへの非可換遅延区間作用を高速に行うデータ構造。
 * 左右からの非可換モノイド積（sumL, sumR）を保持し、遅延評価伝播を行うことで、高度なクエリを O(log N)（ならし時間）で処理する。
 */
public class LinkCutTreeMonoidAction {
    /** 仮想的な無効インデックスを表す定数 */
    private static final int NIL = -1;

    /** 頂点数 */
    private final int N;

    /** 各頂点に対応する Splay 木の左の子ポインタ。グローバル変数 */
    private final int[] left;

    /** 各頂点に対応する Splay 木の右の子ポインタ。グローバル変数 */
    private final int[] right;

    /** 各頂点に対応する Splay 木の親ポインタ（あるいはパスの親ポインタ）。グローバル変数 */
    private final int[] parent;

    /** 反転フラグの遅延評価用配列。グローバル変数 */
    private final boolean[] rev;

    /** 各頂点の現在の値。グローバル変数 */
    private final long[] val;

    /** Splay 部分木の順方向（左から右）のモノイド積。グローバル変数 */
    private final long[] sumL;

    /** Splay 部分木の逆方向（右から左）のモノイド積。グローバル変数 */
    private final long[] sumR;

    /** 各頂点に適用されている遅延区間作用の配列。グローバル変数 */
    private final long[] lazy;

    /** Splay 部分木の頂点数。グローバル変数 */
    private final int[] size;

    /** モノイド作用の代数戦略 */
    private final LongMonoidActionStrategy actionStrategy;

    /** 値（ターゲット）モノイドの代数戦略 */
    private final LongMonoidStrategy targetStrategy;

    /** 作用素（アクティング）モノイドの代数戦略 */
    private final LongMonoidStrategy actingStrategy;

    /**
     * 指定されたパラメータでリンクカット木を初期化する。
     * 計算量: O(N)
     *
     * @param N 頂点数
     * @param actionStrategy モノイド作用の代数戦略
     */
    public LinkCutTreeMonoidAction(int N, LongMonoidActionStrategy actionStrategy) {
        this.N = N;
        this.actionStrategy = actionStrategy;
        this.targetStrategy = actionStrategy.targetMonoidStrategy();
        this.actingStrategy = actionStrategy.actingMonoidStrategy();

        left = new int[N];
        right = new int[N];
        parent = new int[N];
        rev = new boolean[N];
        val = new long[N];
        sumL = new long[N];
        sumR = new long[N];
        lazy = new long[N];
        size = new int[N];

        Arrays.fill(left, NIL);
        Arrays.fill(right, NIL);
        Arrays.fill(parent, NIL);
        Arrays.fill(lazy, actingStrategy.identity());

        long identity = targetStrategy.identity();
        for (int i = 0; i < N; i++) {
            size[i] = 1;
            val[i] = identity;
            sumL[i] = identity;
            sumR[i] = identity;
        }
    }

    /**
     * 指定された初期値配列およびパラメータでリンクカット木を初期化する。
     * 計算量: O(N)
     *
     * @param initialValues 各頂点の初期値配列
     * @param actionStrategy モノイド作用の代数戦略
     */
    public LinkCutTreeMonoidAction(long[] initialValues, LongMonoidActionStrategy actionStrategy) {
        this(initialValues.length, actionStrategy);
        for (int i = 0; i < N; i++) {
            val[i] = initialValues[i];
            sumL[i] = initialValues[i];
            sumR[i] = initialValues[i];
        }
    }

    /**
     * 頂点 t がその属する Splay 木の根であるかを判定する。
     * 計算量: O(1)
     */
    // 未テスト
    private boolean isSplayRoot(int t) {
        if (t == NIL) return false;
        int p = parent[t];
        return p == NIL || (left[p] != t && right[p] != t);
    }

    /**
     * Splay 木のノード t の値および部分木に関する情報を更新する。
     * 計算量: O(1)
     */
    // 未テスト
    private void update(int t) {
        if (t == NIL) return;
        size[t] = 1;
        long sL = val[t];
        long sR = val[t];

        if (left[t] != NIL) {
            size[t] += size[left[t]];
            sL = targetStrategy.mul(sumL[left[t]], sL);
            sR = targetStrategy.mul(sR, sumR[left[t]]);
        }
        if (right[t] != NIL) {
            size[t] += size[right[t]];
            sL = targetStrategy.mul(sL, sumL[right[t]]);
            sR = targetStrategy.mul(sumR[right[t]], sR);
        }
        sumL[t] = sL;
        sumR[t] = sR;
    }

    /**
     * Splay 木のノード t の遅延評価フラグ・作用をその子ノードへ伝播する。
     * 計算量: O(1)
     */
    // 未テスト
    private void push(int t) {
        if (t == NIL) return;
        if (rev[t]) {
            if (left[t] != NIL) {
                int l = left[t];
                int tmp = left[l]; left[l] = right[l]; right[l] = tmp;
                long tmpSum = sumL[l]; sumL[l] = sumR[l]; sumR[l] = tmpSum;
                rev[l] = !rev[l];
            }
            if (right[t] != NIL) {
                int r = right[t];
                int tmp = left[r]; left[r] = right[r]; right[r] = tmp;
                long tmpSum = sumL[r]; sumL[r] = sumR[r]; sumR[r] = tmpSum;
                rev[r] = !rev[r];
            }
            rev[t] = false;
        }
        long lazyId = actingStrategy.identity();
        if (lazy[t] != lazyId) {
            long lz = lazy[t];
            if (left[t] != NIL) {
                int l = left[t];
                val[l] = actionStrategy.act(lz, val[l], 1);
                sumL[l] = actionStrategy.act(lz, sumL[l], size[l]);
                sumR[l] = actionStrategy.act(lz, sumR[l], size[l]);
                lazy[l] = actingStrategy.mul(lz, lazy[l]);
            }
            if (right[t] != NIL) {
                int r = right[t];
                val[r] = actionStrategy.act(lz, val[r], 1);
                sumL[r] = actionStrategy.act(lz, sumL[r], size[r]);
                sumR[r] = actionStrategy.act(lz, sumR[r], size[r]);
                lazy[r] = actingStrategy.mul(lz, lazy[r]);
            }
            lazy[t] = lazyId;
        }
    }

    /**
     * 頂点 t から Splay 木の根に向かって、すべての遅延評価フラグを順に伝播する。
     * 計算量: O(log N) ならし
     */
    // 未テスト
    private void pushAll(int t) {
        if (!isSplayRoot(t)) {
            pushAll(parent[t]);
        }
        push(t);
    }

    /**
     * Splay 木のノード t について、左または右の回転操作を行う。
     * 計算量: O(1)
     */
    // 未テスト
    private void rotate(int t) {
        int p = parent[t];
        int g = parent[p];
        push(p);
        push(t);

        if (left[p] == t) {
            left[p] = right[t];
            if (right[t] != NIL) parent[right[t]] = p;
            right[t] = p;
        } else {
            right[p] = left[t];
            if (left[t] != NIL) parent[left[t]] = p;
            left[t] = p;
        }
        parent[p] = t;
        parent[t] = g;
        if (g != NIL) {
            if (left[g] == p) left[g] = t;
            else if (right[g] == p) right[g] = t;
        }
        update(p);
        update(t);
    }

    /**
     * 頂点 t をそれが属する Splay 木の根に移動する。
     * 計算量: O(log N) ならし
     */
    // 未テスト
    private void splay(int t) {
        pushAll(t);
        while (!isSplayRoot(t)) {
            int p = parent[t];
            int g = parent[p];
            if (!isSplayRoot(p)) {
                if ((left[p] == t) == (left[g] == p)) {
                    rotate(p);
                } else {
                    rotate(t);
                }
            }
            rotate(t);
        }
    }

    /**
     * 頂点 t から、それを含む木（森の中の連結成分）の根に至るパスを Preferred Path として接続し、
     * t をその Splay 木の根にする。
     * 計算量: O(log N) ならし
     *
     * @param t 対象の頂点
     * @return 最後に変更された Preferred Path の根（LCA検出などで使用）
     */
    // 未テスト
    public int expose(int t) {
        int last = NIL;
        for (int curr = t; curr != NIL; curr = parent[curr]) {
            splay(curr);
            right[curr] = last;
            if (last != NIL) parent[last] = curr;
            update(curr);
            last = curr;
        }
        splay(t);
        return last;
    }

    /**
     * 頂点 t をそれを含む木（森の中の連結成分）の新しい根にする。
     * 計算量: O(log N) ならし
     *
     * @param t 対象の頂点
     */
    // 未テスト
    public void evert(int t) {
        expose(t);
        splay(t);
        int tmp = left[t]; left[t] = right[t]; right[t] = tmp;
        long tmpSum = sumL[t]; sumL[t] = sumR[t]; sumR[t] = tmpSum;
        rev[t] = !rev[t];
    }

    /**
     * 頂点 u と v の間に辺を張り、接続する。
     * 既に u と v が同じ連結成分内にある場合は、接続を行わず false を返す。
     * 接続に成功した場合は、u が属する木の根を evert し、その親を v として接続を構築した上で true を返す。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点1
     * @param v 対象の頂点2
     * @return 接続に成功したなら true、既に連結であったなら false
     */
    // 未テスト
    public boolean link(int u, int v) {
        if (same(u, v)) return false;
        evert(u);
        parent[u] = v;
        return true;
    }

    /**
     * 頂点 u と v の間にある直接の辺を切り離す。
     * 実際に u と v の間に直接の辺が存在して切り離しに成功した場合は true、
     * そうでない場合は何もせず false を返す。
     * 計算量: O(log N) ならし
     *
     * @param u 対象 of 頂点1
     * @param v 対象 of 頂点2
     * @return 辺の削除に成功したなら true、直接の辺が存在しなかったなら false
     */
    // 未テスト
    public boolean cut(int u, int v) {
        evert(u);
        expose(v);
        splay(v);
        if (left[v] == u && right[u] == NIL) {
            left[v] = NIL;
            parent[u] = NIL;
            update(v);
            return true;
        }
        return false;
    }

    /**
     * 頂点 u と v の間の最近共通祖先 (LCA: Lowest Common Ancestor) を返す。
     * 同一の木（連結成分）に含まれていない場合は NIL (-1) を返す。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点1
     * @param v 対象の頂点2
     * @return LCAの頂点番号、非連結なら NIL (-1)
     */
    // 未テスト
    public int lca(int u, int v) {
        if (!same(u, v)) return NIL;
        expose(u);
        return expose(v);
    }

    /**
     * 頂点 u と v が同一の木（連結成分）に含まれているかを判定する。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点1
     * @param v 対象の頂点2
     * @return 同一の木にあれば true、そうでなければ false
     */
    // 未テスト
    public boolean same(int u, int v) {
        return root(u) == root(v);
    }

    /**
     * 頂点 t を含む木の根（代表元）を返す。
     * 計算量: O(log N) ならし
     *
     * @param t 対象の頂点
     * @return 木の根の頂点番号
     */
    // 未テスト
    public int root(int t) {
        expose(t);
        while (left[t] != NIL) {
            push(t);
            t = left[t];
        }
        splay(t);
        return t;
    }

    /**
     * 頂点 t から、それを含む木の現在の根までの深さ（パスに含まれる辺の数）を返す。
     * 計算量: O(log N) ならし
     *
     * @param t 対象の頂点
     * @return 根からの深さ
     */
    // 未テスト
    public int depth(int t) {
        expose(t);
        splay(t);
        return size[t] - 1;
    }

    /**
     * 頂点 u の k 代上の祖先を返す。存在しない場合は NIL (-1) を返す。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点
     * @param k 遡上するレベル
     * @return 祖先の頂点番号、存在しない場合は NIL (-1)
     */
    // 未テスト
    public int kthAncestor(int u, int k) {
        expose(u);
        splay(u);
        if (size[u] <= k || k < 0) return NIL;
        int curr = u;
        while (curr != NIL) {
            push(curr);
            int rightSize = (right[curr] == NIL) ? 0 : size[right[curr]];
            if (rightSize == k) {
                splay(curr);
                return curr;
            } else if (rightSize > k) {
                curr = right[curr];
            } else {
                k -= (rightSize + 1);
                curr = left[curr];
            }
        }
        return NIL;
    }

    /**
     * 頂点 u の親（1代上の祖先）を返す。存在しない場合は NIL (-1) を返す。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点
     * @return 親の頂点番号、存在しない場合は NIL (-1)
     */
    // 未テスト
    public int parent(int u) {
        return kthAncestor(u, 1);
    }

    /**
     * 頂点 u の値を newVal に更新する。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点
     * @param newVal 新しい値
     */
    // 未テスト
    public void updateVertex(int u, long newVal) {
        splay(u);
        val[u] = newVal;
        update(u);
    }

    /**
     * 頂点 u の現在の値を取得する。
     * 計算量: O(log N) ならし
     *
     * @param u 対象の頂点
     * @return 頂点 u の値
     */
    // 未テスト
    public long getVertexValue(int u) {
        splay(u);
        return val[u];
    }

    /**
     * 頂点 u と v の間のパス上のすべての頂点の値を、u から v への順方向でモノイド集約した結果を返す。
     * u と v が異なる木（連結成分）にある場合は単位元を返す。
     * 計算量: O(log N) ならし
     *
     * @param u パスの始点
     * @param v パスの終点
     * @return パス上の非可換モノイド積。非連結なら identity
     */
    // 未テスト
    public long pathQuery(int u, int v) {
        if (!same(u, v)) return targetStrategy.identity();
        evert(u);
        expose(v);
        splay(v);
        return sumL[v];
    }

    /**
     * 頂点 u と v の間のパス上にあるすべての頂点に対して、遅延作用 lazyVal を適用する。
     * u と v が異なる木（連結成分）にある場合は何もせず false を返す。
     * 計算量: O(log N) ならし
     *
     * @param u パスの始点
     * @param v パスの終点
     * @param lazyVal 適用する遅延作用タグ
     * @return 作用の適用に成功したなら true、非連結なら false
     */
    // 未テスト
    public boolean pathApply(int u, int v, long lazyVal) {
        if (!same(u, v)) return false;
        evert(u);
        expose(v);
        splay(v);
        val[v] = actionStrategy.act(lazyVal, val[v], 1);
        sumL[v] = actionStrategy.act(lazyVal, sumL[v], size[v]);
        sumR[v] = actionStrategy.act(lazyVal, sumR[v], size[v]);
        lazy[v] = actingStrategy.mul(lazyVal, lazy[v]);
        return true;
    }
}