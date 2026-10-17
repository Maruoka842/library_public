package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import library.util.ArrayUtils;
import library.util.collections.HashStrategies;
import library.util.graph.tree.Tree;

/**
 * 代表的な再構成問題（Reconfiguration Problems）に対する {@link ImplicitDigraph} の実装を提供する。
 */
public class ReconfigurationProblems {

    /**
     * 木上のスライディングパズル（15パズルの木版）。
     *
     * <p>状態：頂点にラベルが割り振られた木。ラベル 0 は空きスペース（穴）とみなす。
     * 隣接条件：ラベル 0 がある頂点 u と、ラベル 0 ではない値を持つ隣接頂点 v について、
     * ラベル P[u] と P[v] を入れ替えることができる。ラベル 0 同士の入れ替えはできない。</p>
     *
     * <p>計算量：
     * <ul>
     *   <li>{@code nextStates}: O(n)</li>
     * </ul></p>
     *
     * @param tree 木
     * @return 状態空間を表す {@link ImplicitDigraph}
     */
    public static ImplicitDigraph<int[]> treeSlidingPuzzle(Tree tree) {
        final int n = tree.N;
        return new ImplicitDigraph<int[]>(HashStrategies.INT_ARRAY) {
            @Override
            public Iterable<int[]> nextStates(int[] v) {
                List<int[]> nexts = new ArrayList<>();
                for (int u = 0; u < n; u++) {
                    if (v[u] == 0) {
                        for (int neighbor : tree.adj[u]) {
                            if (v[neighbor] != 0) {
                                int[] next = v.clone();
                                next[u] = v[neighbor];
                                next[neighbor] = 0;
                                nexts.add(next);
                            }
                        }
                    }
                }
                return nexts;
            }
        };
    }

    /**
     * 長さ n の {0, 1, 2, ...} 配列において、
     * 部分列 0112 の出現を禁止した上で、隣接要素の入れ替えを行う再構成問題。
     *
     * <p>出典: <a href="https://yukicoder.me/problems/no/3587">No.3587 0112-Free Reconfiguration - yukicoder</a></p>
     *
     * <p>隣接条件：状態 u, v が隣接するのは、ある i ∈ [0, n-2] について
     * u[i] ≠ u[i+1] かつ、それらを入れ替えた配列が 0112 を部分列（連続する要素）として
     * 含まないとき。</p>
     *
     * <p>計算量：
     * <ul>
     *   <li>{@code nextStates}: O(n^2)</li>
     *   <li>{@code onPath}: O(n)</li>
     * </ul></p>
     *
     * @return 状態空間を表す {@link ImplicitDigraph}
     */
    // 未テスト
    public static ImplicitDigraph<int[]> avoiding0112AdjacentSwapping() {
        return new ImplicitDigraph<int[]>(HashStrategies.INT_ARRAY) {
            private void validate(int[] v) {
                for (int x : v) {
                    if (x < 0 || x > 2) {
                        throw new IllegalArgumentException("Array contains invalid element: " + x + ". Only 0, 1, 2 are allowed.");
                    }
                }
            }

            @Override
            public Iterable<int[]> nextStates(int[] v) {
                validate(v);
                if (!check(v)) throw new AssertionError();
                ArrayList<int[]> list = new ArrayList<>();
                for (int i = 0; i < v.length - 1; i++) {
                    if (v[i] == v[i + 1]) continue;
                    int[] u = v.clone();
                    {
                        var tmp = u[i];
                        u[i] = u[i + 1];
                        u[i + 1] = tmp;
                    }
                    if (check(u)) list.add(u);
                }
                return list;
            }

            private boolean check(int[] v) {
                for (int i = 0; i + 3 < v.length; i++) {
                    if (v[i] == 0 && v[i + 1] == 1 && v[i + 2] == 1 && v[i + 3] == 2) return false;
                }
                return true;
            }

            @Override
            public boolean onPath(int[] src, int[] dst) {
                validate(src);
                validate(dst);
                int[] cnt0 = new int[3];
                int[] cnt1 = new int[3];
                for (int v : src) {
                    cnt0[v]++;
                }
                for (int v : dst) {
                    cnt1[v]++;
                }
                if (!Arrays.equals(cnt0, cnt1)) return false;
                if (cnt0[0] == 0 || cnt0[1] <= 1 || cnt0[2] == 0) return true;
                return type(src) == type(dst);
            }

            private int type(int[] S) {
                int a = ArrayUtils.indexOf(S, 2);
                int b = -1;
                for (int i = S.length - 1; i >= 0; i--) {
                    if (S[i] == 0) {
                        b = i;
                        break;
                    }
                }
                if (a < b) return 1;
                for (int i = 0; i + 1 < S.length; i++) {
                    if (S[i] == 0) {
                        int j = i;
                        while (j + 1 < S.length && S[j + 1] == 1) j++;
                        if (j + 1 < S.length && j - i >= 2 && S[j + 1] == 2) return 0;
                        i = j;
                    }
                }
                return 1;
            }
        };
    }
}
