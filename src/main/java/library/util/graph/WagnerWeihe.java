package library.util.graph;

import java.util.*;

/**
 * 平面地図上の単一面境界に端点を持つ需要対に対する辺素道問題 (Okamura-Seymour / Wagner-Weihe)。
 *
 * 埋め込み平面グラフ G = (V, E) および面 F の境界上に端点を持つ需要対集合 R = {(s_i, t_i) : i = 0, ..., k-1} に対し、
 * オイラー条件 forall v in V, deg_E(v) = deg_R(v) (mod 2) の下で、
 * 互いに辺素な道 P_0, ..., P_{k-1} (各 P_i は s_i と t_i を結ぶ単純道) が存在するか判定し、
 * 存在する場合は各道を求める。
 */
public final class WagnerWeihe {
    private WagnerWeihe() {}

    public static final class Result {
        /** 辺素な道が存在するか。 */
        public final boolean feasible;
        /**
         * 各需要対 i の道 (元の地図 G におけるダート列)。
         * feasible が true の場合、paths[i] は s[i] から t[i] へのダート列。
         * feasible が false の場合、null。
         */
        public final int[][] paths;

        /**
         * 結果オブジェクトを生成する。
         * 未テスト
         * 計算量: O(1)
         *
         * @param feasible 辺素な道の組が存在するか
         * @param paths    各需要対のダート列 (存在しない場合は null)
         */
        Result(boolean feasible, int[][] paths) {
            this.feasible = feasible;
            this.paths = paths;
        }
    }

    /**
    * 指定した面の境界上にある需要対を、互いに辺素な道で結ぶ。
    *
    * 各 i について s[i] から t[i] への道を求める。
    * s[i] == t[i] の場合は空の道を割り当てる。
    * 道は元のグラフにおけるダート番号の列として返す。
    *
	 * 辺素な道の組が存在するための必要十分条件は、次の2条件である
	 * （Okamura-Seymour の定理）。
	 *
	 * - オイラー条件: 任意の頂点集合 X ⊆ V について
	 *   |δ(X)| ≡ r(X) (mod 2)
	 * - カット条件: 任意の頂点集合 X ⊆ V について
	 *   |δ(X)| >= r(X)
	 *
	 * ここで δ(X) は X と V \ X を結ぶ辺の集合、
	 * * r(X) は、各需要対 (s[i], t[i]) のうち、s[i] と t[i] のちょうど一方だけが X に属するものの個数
	 * 上記のいずれかを満たさない場合は feasible == false, paths == null を返す。    *
    * s[i] != t[i] である各需要対の両端は、面 face の境界上にある必要がある。
    *
    * 計算量: O(n + m + k log k)
    *
    * @param g 平面地図
    * @param face 端点が配置されている面 ID
    * @param s 各需要対の始点
    * @param t 各需要対の終点
    * @return feasible が true なら各需要対の道、false なら paths == null
    */
    public static Result solve(PlanarMap g, int face, int[] s, int[] t) {
        if (s.length != t.length) {
            throw new IllegalArgumentException("s.length != t.length");
        }
        int n = g.vertexCount();
        int m = g.edgeCount();
        int k = s.length;

        for (int i = 0; i < k; i++) {
            if (s[i] < 0 || s[i] >= n || t[i] < 0 || t[i] >= n) {
                throw new IllegalArgumentException("vertex out of range");
            }
        }

        PlanarMap.Faces faces = g.faces();
        if (face < 0 || face >= faces.count) {
            throw new IllegalArgumentException("face out of range");
        }

        // オイラー条件の検証: forall v, deg_E(v) = deg_R(v) (mod 2)
        int[] degR = new int[n];
        for (int i = 0; i < k; i++) {
            degR[s[i]]++;
            degR[t[i]]++;
        }
        for (int v = 0; v < n; v++) {
            if ((g.degree(v) & 1) != (degR[v] & 1)) {
                return new Result(false, null);
            }
        }

        if (k == 0) {
            return new Result(true, new int[0][]);
        }

        // 面 face の境界ダートを収集 (to(a) が face 上の頂点)
        int faceStart = faces.start[face];
        int faceEnd = faces.start[face + 1];
        int[] faceDarts = new int[faceEnd - faceStart];
        for (int p = faceStart; p < faceEnd; p++) {
            faceDarts[p - faceStart] = faces.darts[p];
        }

        // 各頂点 v について、face 上で to(a) == v となるダート a を記録
        int[] dartEntering = new int[n];
        Arrays.fill(dartEntering, -1);
        for (int d : faceDarts) {
            dartEntering[g.to(d)] = d;
        }

        for (int i = 0; i < k; i++) {
            if (s[i] != t[i]) {
                if (dartEntering[s[i]] < 0) {
                    throw new IllegalArgumentException("terminal " + s[i] + " not on face " + face);
                }
                if (dartEntering[t[i]] < 0) {
                    throw new IllegalArgumentException("terminal " + t[i] + " not on face " + face);
                }
            }
        }

        // 端点葉の追加
        PlanarMap h = g.copy();
        int[] leafS = new int[k];
        int[] leafT = new int[k];
        Arrays.fill(leafS, -1);
        Arrays.fill(leafT, -1);

        for (int i = 0; i < k; i++) {
            if (s[i] == t[i]) continue;
            leafS[i] = h.addLeaf(dartEntering[s[i]]);
            leafT[i] = h.addLeaf(dartEntering[t[i]]);
        }

        // 葉の属性
        int totalVertices = h.vertexCount();
        int[] leafPair = new int[totalVertices];
        boolean[] isLeafSource = new boolean[totalVertices];
        Arrays.fill(leafPair, -1);
        for (int i = 0; i < k; i++) {
            if (s[i] == t[i]) continue;
            leafPair[leafS[i]] = i;
            isLeafSource[leafS[i]] = true;
            leafPair[leafT[i]] = i;
            isLeafSource[leafT[i]] = false;
        }

        // 面 face の境界を巡回し、葉の出現順を取得
        int startD = faceDarts[0];
        ArrayList<Integer> leafOrder = new ArrayList<>();
        int currDart = startD;
        do {
            int toV = h.to(currDart);
            if (toV >= n && leafPair[toV] >= 0) {
                leafOrder.add(toV);
            }
            currDart = h.phi(currDart);
        } while (currDart != startD);

        int nonTrivialCount = 0;
        for (int i = 0; i < k; i++) {
            if (s[i] != t[i]) nonTrivialCount++;
        }
        if (leafOrder.size() != 2 * nonTrivialCount) {
            return new Result(false, null);
        }

        // 括弧列の構築
        int L = leafOrder.size();
        boolean[] seenPair = new boolean[k];
        int[] paren = new int[L]; // 0: '(', 1: ')'
        int[] pairOfPos = new int[L];
        int[] origOpenPos = new int[k];
        int[] origClosePos = new int[k];
        for (int j = 0; j < L; j++) {
            int w = leafOrder.get(j);
            int p = leafPair[w];
            pairOfPos[j] = p;
            if (!seenPair[p]) {
                seenPair[p] = true;
                paren[j] = 0;
                origOpenPos[p] = j;
            } else {
                paren[j] = 1;
                origClosePos[p] = j;
            }
        }

        // スタックによる括弧マッチング (再ペアリング)
        int[] match = new int[L];
        ArrayList<Integer> closingOrder = new ArrayList<>();
        int[] stack = new int[L];
        int stackPtr = 0;
        for (int j = 0; j < L; j++) {
            if (paren[j] == 0) {
                stack[stackPtr++] = j;
            } else {
                if (stackPtr == 0) return new Result(false, null);
                int open = stack[--stackPtr];
                match[open] = j;
                match[j] = open;
                closingOrder.add(j);
            }
        }

        // Step 1: 再ペアに対する最右優先探索
        int Mh = h.edgeCount();
        boolean[] edgeUsed = new boolean[Mh];
        int[] dartUsedDir = new int[Mh];
        int[] degRem = new int[totalVertices];
        for (int v = 0; v < totalVertices; v++) degRem[v] = h.degree(v);

        int[] next1 = new int[2 * Mh];
        int[] prev1 = new int[2 * Mh];
        for (int d = 0; d < 2 * Mh; d++) {
            next1[d] = h.sigma(d);
            prev1[h.sigma(d)] = d;
        }

        for (int closePos : closingOrder) {
            int openPos = match[closePos];
            int sPrime = leafOrder.get(openPos);
            int tPrime = leafOrder.get(closePos);

            int curr = sPrime;
            int d0 = h.firstDart(curr);
            edgeUsed[d0 >> 1] = true;
            dartUsedDir[d0 >> 1] = d0;
            degRem[curr]--;
            degRem[h.to(d0)]--;
            removeDart(next1, prev1, d0);
            removeDart(next1, prev1, d0 ^ 1);
            curr = h.to(d0);
            int in = d0;

            while (curr != tPrime) {
                if (curr >= n) {
                    return new Result(false, null);
                }
                if (degRem[curr] == 0) {
                    return new Result(false, null);
                }
                int out = next1[in ^ 1];
                edgeUsed[out >> 1] = true;
                dartUsedDir[out >> 1] = out;
                degRem[curr]--;
                degRem[h.to(out)]--;
                removeDart(next1, prev1, in ^ 1);
                removeDart(next1, prev1, out);
                curr = h.to(out);
                in = out;
            }
        }

        // Step 2: 元の需要対を有向グラフ E_used 上で復元
        int[] next2 = new int[2 * Mh];
        int[] prev2 = new int[2 * Mh];
        int[] outDeg2 = new int[totalVertices];
        int[] anyOut = new int[totalVertices];
        Arrays.fill(anyOut, -1);

        for (int v = 0; v < totalVertices; v++) {
            int firstD = h.firstDart(v);
            if (firstD < 0) continue;
            ArrayList<Integer> outs = new ArrayList<>();
            int d = firstD;
            do {
                if (edgeUsed[d >> 1] && dartUsedDir[d >> 1] == d) {
                    outs.add(d);
                }
                d = h.sigma(d);
            } while (d != firstD);

            int sz = outs.size();
            outDeg2[v] = sz;
            if (sz > 0) {
                anyOut[v] = outs.get(0);
                for (int i = 0; i < sz; i++) {
                    int cD = outs.get(i);
                    int nD = outs.get((i + 1) % sz);
                    next2[cD] = nD;
                    prev2[nD] = cD;
                }
            }
        }

        // 元の非自明な需要対を閉じ括弧順にソート
        List<Integer> nonTrivialPairs = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            if (s[i] != t[i]) nonTrivialPairs.add(i);
        }
        nonTrivialPairs.sort(Comparator.comparingInt(p -> origClosePos[p]));

        boolean[] usedStep2 = new boolean[Mh];
        int[][] finalPaths = new int[k][];
        for (int i = 0; i < k; i++) {
            if (s[i] == t[i]) {
                finalPaths[i] = new int[0];
            }
        }

        for (int p : nonTrivialPairs) {
            // 開き括弧側の葉を src, 閉じ括弧側の葉を dst とする
            int srcLeaf = leafOrder.get(origOpenPos[p]);
            int dstLeaf = leafOrder.get(origClosePos[p]);
            boolean srcIsOrigS = isLeafSource[srcLeaf];

            int curr = srcLeaf;
            int d0 = h.firstDart(curr);
            if (d0 < 0 || !edgeUsed[d0 >> 1] || dartUsedDir[d0 >> 1] != d0 || usedStep2[d0 >> 1]) {
                return new Result(false, null);
            }
            usedStep2[d0 >> 1] = true;
            outDeg2[curr]--;
            curr = h.to(d0);
            int in = d0;

            ArrayList<Integer> path = new ArrayList<>();
            path.add(d0);

            while (curr != dstLeaf) {
                if (curr >= n || outDeg2[curr] == 0) {
                    return new Result(false, null);
                }
                int out = -1;
                if (outDeg2[curr] == 1) {
                    out = anyOut[curr];
                } else {
                    int cand = h.sigma(in ^ 1);
                    while (true) {
                        if (edgeUsed[cand >> 1] && dartUsedDir[cand >> 1] == cand && !usedStep2[cand >> 1]) {
                            out = cand;
                            break;
                        }
                        cand = h.sigma(cand);
                        if (cand == h.sigma(in ^ 1)) break;
                    }
                }
                if (out < 0) {
                    return new Result(false, null);
                }
                usedStep2[out >> 1] = true;
                removeDart(next2, prev2, out);
                outDeg2[curr]--;
                anyOut[curr] = outDeg2[curr] > 0 ? next2[out] : -1;
                path.add(out);
                curr = h.to(out);
                in = out;
            }

            // 先頭と末尾の葉ダートを除外して元のグラフのダート列を得る
            int subLen = path.size() - 2;
            int[] subPath = new int[subLen];
            for (int i = 0; i < subLen; i++) {
                subPath[i] = path.get(i + 1);
            }

            if (srcIsOrigS) {
                finalPaths[p] = subPath;
            } else {
                int[] revPath = new int[subLen];
                for (int i = 0; i < subLen; i++) {
                    revPath[i] = subPath[subLen - 1 - i] ^ 1;
                }
                finalPaths[p] = revPath;
            }
        }

        return new Result(true, finalPaths);
    }

    private static void removeDart(int[] next, int[] prev, int d) {
        next[prev[d]] = next[d];
        prev[next[d]] = prev[d];
    }

    /**
     * 端点が共通の面に属することを自動検出し、辺素な道の組を求める。
     * 未テスト
     * 計算量: O(n + m + k)
     *
     * 事前条件:
     * - s.length == t.length
     * - forall i, s[i], t[i] in V
     * - すべての端点 s[i], t[i] を境界に含む面 F が存在する
     *
     * 事後条件:
     * - feasible が true の場合:
     *   - 各 paths[i] は s[i] から t[i] への有向歩道 (ダート列) を表す。
     *   - 各辺 e in E に対し、e に属するダート 2e, 2e+1 は全 paths を通じて高々 1 回現れる (辺素)。
     * - feasible が false の場合、条件を満たす辺素道の組は存在しない。
     *
     * @param g 平面地図
     * @param s 各需要対の始点頂点配列
     * @param t 各需要対の終点頂点配列
     * @return 探索結果 (成否および求まった道)
     * @throws IllegalArgumentException s と t の長さが異なる場合、頂点が範囲外の場合、または全端点が共通の面境界を持たない場合
     */
    public static Result solve(PlanarMap g, int[] s, int[] t) {
        if (s.length != t.length) {
            throw new IllegalArgumentException("s.length != t.length");
        }
        int k = s.length;
        if (k == 0) return new Result(true, new int[0][]);

        int n = g.vertexCount();
        for (int i = 0; i < k; i++) {
            if (s[i] < 0 || s[i] >= n || t[i] < 0 || t[i] >= n) {
                throw new IllegalArgumentException("vertex out of range");
            }
        }

        PlanarMap.Faces faces = g.faces();
        int[] termCount = new int[faces.count];
        boolean[] isTerm = new boolean[g.vertexCount()];
        int nonTrivialTerminals = 0;
        for (int i = 0; i < k; i++) {
            if (s[i] != t[i]) {
                isTerm[s[i]] = true;
                isTerm[t[i]] = true;
                nonTrivialTerminals += 2;
            }
        }
        if (nonTrivialTerminals == 0) {
            int[][] empty = new int[k][0];
            return new Result(true, empty);
        }

        for (int f = 0; f < faces.count; f++) {
            int start = faces.start[f];
            int end = faces.start[f + 1];
            int count = 0;
            for (int p = start; p < end; p++) {
                int d = faces.darts[p];
                int v = g.to(d);
                for (int i = 0; i < k; i++) {
                    if (s[i] != t[i]) {
                        if (s[i] == v) count++;
                        if (t[i] == v) count++;
                    }
                }
            }
            if (count >= nonTrivialTerminals) {
                return solve(g, f, s, t);
            }
        }

        throw new IllegalArgumentException("terminals do not share a common face");
    }
}
