package library.util.graph;

/**
 * グラフの合成に伴う最大 cardinality matching のサイズを、
 * 頂点数と最大 matching サイズだけから合成するためのユーティリティ。
 *
 * <p>一般グラフについては disjoint union と complete join、
 * 二部グラフについては disjoint union と bipartite join をサポートする。
 * 各演算は、実際の辺を展開せず O(1) で summary を合成する。
 */
public class MaximumMatchingComposition {
    private MaximumMatchingComposition() {}

    /**
     * 一般グラフの summary。
     *
     * @param vertexCount 頂点数
     * @param matchingSize 最大 matching の辺数
     */
    public record Undirected(int vertexCount, int matchingSize) {}

    /**
     * 二部グラフの summary。
     *
     * @param leftSize 左側頂点数
     * @param rightSize 右側頂点数
     * @param matchingSize 最大 matching の辺数
     */
    public record Bipartite(int leftSize, int rightSize, int matchingSize) {}

    /** 1 頂点グラフの summary を返す。 */
    public static Undirected vertex() {
        return new Undirected(1, 0);
    }

    /**
     * 二つの一般グラフの disjoint union の summary を返す。
     *
     * <p>計算量: O(1)
     */
    public static Undirected disjointUnion(Undirected a, Undirected b) {
        return new Undirected(
            a.vertexCount + b.vertexCount,
            a.matchingSize + b.matchingSize
        );
    }

    /**
     * 二つの一般グラフの complete join の summary を返す。
     *
     * <p>complete join とは、二つのグラフを disjoint union した後、
     * 異なるグラフに属する任意の 2 頂点間に辺を追加する操作である。
     *
     * <p>G_i の頂点数を n_i、最大 matching サイズを m_i とすると、
     *
     * <pre>
     * nu(G_1 join G_2)
     *   = min((n_1+n_2)/2, n_2+m_1, n_1+m_2).
     * </pre>
     *
     * <p>計算量: O(1)
     */
    public static Undirected completeJoin(Undirected a, Undirected b) {
        int n = a.vertexCount + b.vertexCount;
        int m = completeJoinMatchingSize(a.vertexCount, a.matchingSize, b.vertexCount, b.matchingSize);
        return new Undirected(n, m);
    }

    /**
     * 二つの一般グラフの complete join の最大 matching サイズを返す。
     * summary オブジェクトを生成したくない hot loop 向けの primitive API。
     *
     * @param n1 第1グラフの頂点数
     * @param m1 第1グラフの最大 matching サイズ
     * @param n2 第2グラフの頂点数
     * @param m2 第2グラフの最大 matching サイズ
     * @return complete join の最大 matching サイズ
     */
    public static int completeJoinMatchingSize(int n1, int m1, int n2, int m2) {
        return Math.min(
            (n1 + n2) / 2,
            Math.min(n2 + m1, n1 + m2)
        );
    }

    /** 辺を持たない二部グラフの summary を返す。 */
    public static Bipartite emptyBipartite(int leftSize, int rightSize) {
        return new Bipartite(leftSize, rightSize, 0);
    }

    /** 完全二部グラフの summary を返す。 */
    public static Bipartite completeBipartite(int leftSize, int rightSize) {
        return new Bipartite(leftSize, rightSize, Math.min(leftSize, rightSize));
    }

    /**
     * 二つの二部グラフの disjoint union の summary を返す。
     * 左側同士、右側同士をそれぞれ同じ側として扱う。
     *
     * <p>計算量: O(1)
     */
    public static Bipartite disjointUnion(Bipartite a, Bipartite b) {
        return new Bipartite(
            a.leftSize + b.leftSize,
            a.rightSize + b.rightSize,
            a.matchingSize + b.matchingSize
        );
    }

    /**
     * 二つの二部グラフの bipartite join の summary を返す。
     *
     * <p>G_1=(L_1,R_1,E_1), G_2=(L_2,R_2,E_2) に対し、
     * bipartite join は
     *
     * <pre>
     * E_1 union E_2 union (L_1 x R_2) union (L_2 x R_1)
     * </pre>
     *
     * を辺集合とする二部グラフである。
     *
     * <p>l_i=|L_i|, r_i=|R_i|, m_i=nu(G_i) とすると、
     *
     * <pre>
     * nu(G_1 bipartiteJoin G_2)
     *   = min(l_1+l_2,
     *         r_1+r_2,
     *         l_2+r_2+m_1,
     *         l_1+r_1+m_2).
     * </pre>
     *
     * <p>計算量: O(1)
     */
    public static Bipartite bipartiteJoin(Bipartite a, Bipartite b) {
        int l = a.leftSize + b.leftSize;
        int r = a.rightSize + b.rightSize;
        int m = bipartiteJoinMatchingSize(
            a.leftSize, a.rightSize, a.matchingSize,
            b.leftSize, b.rightSize, b.matchingSize
        );
        return new Bipartite(l, r, m);
    }

    /**
     * 二つの二部グラフの bipartite join の最大 matching サイズを返す。
     * summary オブジェクトを生成したくない hot loop 向けの primitive API。
     *
     * @param l1 第1グラフの左側頂点数
     * @param r1 第1グラフの右側頂点数
     * @param m1 第1グラフの最大 matching サイズ
     * @param l2 第2グラフの左側頂点数
     * @param r2 第2グラフの右側頂点数
     * @param m2 第2グラフの最大 matching サイズ
     * @return bipartite join の最大 matching サイズ
     */
    public static int bipartiteJoinMatchingSize(
        int l1, int r1, int m1,
        int l2, int r2, int m2
    ) {
        return Math.min(
            Math.min(l1 + l2, r1 + r2),
            Math.min(l2 + r2 + m1, l1 + r1 + m2)
        );
    }

    /**
     * 二部グラフの左右の区別を忘れ、同じグラフを一般グラフとして見た summary を返す。
     *
     * <p>辺集合は変わらないため最大 matching サイズも変わらない。
     */
    public static Undirected forgetBipartition(Bipartite g) {
        return new Undirected(g.leftSize + g.rightSize, g.matchingSize);
    }
}

