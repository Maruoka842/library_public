package library.util.graph.grid;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

import library.util.ArrayUtils;
import library.util.graph.Graph;
import library.util.unionfind.UnionFind;

/**
 * 4近傍の格子グラフ（グリッド）上での探索アルゴリズムを提供するクラス。
 * すべてのメソッドは静的（static）メソッドであり、インスタンス化は不要である。
 * マス間の境界に壁（blockedRight, blockedDown）が存在する状況下での通常の幅優先探索（BFS）および滑る床（icefloor）BFSをサポートする。
 *
 * blockedRight[i][j] はマス (i, j) とマス (i, j+1) の間に壁があるか (true / false) を表す。
 * blockedDown[i][j] はマス (i, j) とマス (i+1, j) の間に壁があるか (true / false) を表す。
 */
public final class Grid4neighborAlgorithms {

    // インスタンス化を防ぐためのプライベートコンストラクタ
    private Grid4neighborAlgorithms() {}

    /**
     * 格子グラフの情報（壁・障害物情報）を {@link Graph} オブジェクトに変換する。
     * 各マス (r, c) には頂点インデックス r * C + c が割り当てられる。
     * 壁や障害物が存在しない隣接マス間に無向辺が張られる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @return 変換された Graph オブジェクト
     * @complexity O(R · C)
     */
    // 未テスト
    public static Graph toGraph(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle) {
        if (R < 0 || C < 0) {
            throw new IllegalArgumentException("R and C must be non-negative");
        }
        Graph g = new Graph(R * C);
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                int u = r * C + c;
                if (c + 1 < C && isAdjacent(R, C, blockedRight, blockedDown, isObstacle, r, c, r, c + 1)) {
                    int v = r * C + (c + 1);
                    g.addEdge(u, v);
                }
                if (r + 1 < R && isAdjacent(R, C, blockedRight, blockedDown, isObstacle, r, c, r + 1, c)) {
                    int v = (r + 1) * C + c;
                    g.addEdge(u, v);
                }
            }
        }
        return g;
    }

    /**
     * 格子グラフの情報（壁情報）を {@link Graph} オブジェクトに変換する。
     * 各マス (r, c) には頂点インデックス r * C + c が割り当てられる。
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @return 変換された Graph オブジェクト
     * @complexity O(R · C)
     */
    // 未テスト
    public static Graph toGraph(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown) {
        return toGraph(R, C, blockedRight, blockedDown, null);
    }

    /**
     * 格子グラフの情報（障害物情報）を {@link Graph} オブジェクトに変換する。
     * 各マス (r, c) には頂点インデックス r * C + c が割り当てられる。
     * @param R 行数
     * @param C 列数
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @return 変換された Graph オブジェクト
     * @complexity O(R · C)
     */
    // 未テスト
    public static Graph toGraph(int R, int C, boolean[][] isObstacle) {
        return toGraph(R, C, null, null, isObstacle);
    }

    /**
     * 障害物や壁のない格子グラフを {@link Graph} オブジェクトに変換する。
     * 各マス (r, c) には頂点インデックス r * C + c が割り当てられる。
     * @param R 行数
     * @param C 列数
     * @return 変換された Graph オブジェクト
     * @complexity O(R · C)
     */
    // 未テスト
    public static Graph toGraph(int R, int C) {
        return toGraph(R, C, null, null, null);
    }

    /** 方向ベクトル：下、右、上、左（Grid2Dと共通） */
    public static final int[] dh = {1, 0, -1, 0};
    public static final int[] dw = {0, 1, 0, -1};

    /**
     * 2つのマス (r1, c1) と (r2, c2) が格子上で4近傍隣接しており、かつ間に壁や障害物が存在しないかを判定する。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param r1 点1の行インデックス
     * @param c1 点1の列インデックス
     * @param r2 点2の行インデックス
     * @param c2 点2の列インデックス
     * @return 2点が隣接し、かつ通過可能であれば true、そうでなければ false
     * @complexity O(1)
     */
    // 未テスト
    public static boolean isAdjacent(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int r1, int c1, int r2, int c2) {
        if (r1 < 0 || r1 >= R || c1 < 0 || c1 >= C || r2 < 0 || r2 >= R || c2 < 0 || c2 >= C) {
            return false;
        }
        if (isObstacle != null && (isObstacle[r1][c1] || isObstacle[r2][c2])) {
            return false;
        }
        int dr = r2 - r1;
        int dc = c2 - c1;
        if (Math.abs(dr) + Math.abs(dc) != 1) {
            return false;
        }
        if (dr == 1) { // (r1, c1) -> (r1+1, c1) : 下
            if (blockedDown != null && r1 < blockedDown.length && c1 < blockedDown[r1].length && blockedDown[r1][c1]) return false;
        } else if (dr == -1) { // (r1, c1) -> (r1-1, c1) : 上
            if (blockedDown != null && r2 < blockedDown.length && c2 < blockedDown[r2].length && blockedDown[r2][c2]) return false;
        } else if (dc == 1) { // (r1, c1) -> (r1, c1+1) : 右
            if (blockedRight != null && r1 < blockedRight.length && c1 < blockedRight[r1].length && blockedRight[r1][c1]) return false;
        } else if (dc == -1) { // (r1, c1) -> (r1, c1-1) : 左
            if (blockedRight != null && r2 < blockedRight.length && c2 < blockedRight[r2].length && blockedRight[r2][c2]) return false;
        }
        return true;
    }

    /**
     * 2つのマス (r1, c1) と (r2, c2) が格子上で4近傍隣接しており、かつ間に壁が存在しないかを判定する。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param r1 点1の行インデックス
     * @param c1 点1の列インデックス
     * @param r2 点2の行インデックス
     * @param c2 点2の列インデックス
     * @return 2点が隣接し、かつ通過可能であれば true、そうでなければ false
     * @complexity O(1)
     */
    // 未テスト
    public static boolean isAdjacent(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int r1, int c1, int r2, int c2) {
        return isAdjacent(R, C, blockedRight, blockedDown, null, r1, c1, r2, c2);
    }

    /**
     * 2つのマス (r1, c1) と (r2, c2) が格子上で4近傍隣接しており、かつ障害物が存在しないかを判定する。
     *
     * @param R 行数
     * @param C 列数
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param r1 点1の行インデックス
     * @param c1 点1の列インデックス
     * @param r2 点2の行インデックス
     * @param c2 点2の列インデックス
     * @return 2点が隣接し、かつ通過可能であれば true、そうでなければ false
     * @complexity O(1)
     */
    // 未テスト
    public static boolean isAdjacent(int R, int C, boolean[][] isObstacle, int r1, int c1, int r2, int c2) {
        return isAdjacent(R, C, null, null, isObstacle, r1, c1, r2, c2);
    }

    /**
     * マス (r, c) から方向 dir への移動が可能であるかを判定する。
     *
     * @param r 移動元の行インデックス
     * @param c 移動元の列インデックス
     * @param dir 移動方向（0: 下, 1: 左, 2: 上, 3: 右）
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param isObstacle セル障害物情報
     * @return 移動可能であれば true、そうでなければ false
     */
    // 未テスト
    private static boolean canMove(int r, int c, int dir, int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle) {
        int nr = r + dh[dir];
        int nc = c + dw[dir];
        return isAdjacent(R, C, blockedRight, blockedDown, isObstacle, r, c, nr, nc);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を通常のBFSにより計算する。
     * 壁（blockedRight, blockedDown）およびセル障害物（isObstacle）による通行不能を考慮する。
     * 到達不能なマスの最短距離は Integer.MAX_VALUE となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。blockedRight[r][c] が true ならば、マス (r, c) と (r, c + 1) の間を通過できない。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。blockedDown[r][c] が true ならば、マス (r, c) と (r + 1, c) の間を通過できない。null の場合は水平壁なし
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj) {
        if (blockedRight != null) {
            if (blockedRight.length != R) {
                throw new IllegalArgumentException("blockedRight must have R rows");
            }
            for (int r = 0; r < R; r++) {
                if (blockedRight[r] == null || blockedRight[r].length != C - 1) {
                    throw new IllegalArgumentException("each row of blockedRight must have C - 1 columns");
                }
            }
        }
        if (blockedDown != null) {
            if (blockedDown.length != R - 1) {
                throw new IllegalArgumentException("blockedDown must have R - 1 rows");
            }
            for (int r = 0; r < R - 1; r++) {
                if (blockedDown[r] == null || blockedDown[r].length != C) {
                    throw new IllegalArgumentException("each row of blockedDown must have C columns");
                }
            }
        }
        int[][] dist = new int[R][C];
        int INF = Integer.MAX_VALUE;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && isObstacle[si][sj]) {
            return dist;
        }

        Queue<int[]> que = new ArrayDeque<>();
        dist[si][sj] = 0;
        que.add(new int[]{si, sj});

        while (!que.isEmpty()) {
            int[] curr = que.poll();
            int r = curr[0];
            int c = curr[1];

            for (int dir = 0; dir < 4; dir++) {
                if (canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    if (dist[nr][nc] == INF) {
                        dist[nr][nc] = dist[r][c] + 1;
                        que.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を通常のBFSにより計算する。
     * 壁（blockedRight, blockedDown）による通行不能を考慮する。
     * 到達不能なマスの最短距離は Integer.MAX_VALUE となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。blockedRight[r][c] が true ならば、マス (r, c) と (r, c + 1) の間を通過できない。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。blockedDown[r][c] が true ならば、マス (r, c) と (r + 1, c) の間を通過できない。null の場合は水平壁なし
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj) {
        return bfs(R, C, blockedRight, blockedDown, (boolean[][]) null, si, sj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を通常のBFSにより計算する。
     * セル障害物（isObstacle）による通行不能を考慮する。
     * 到達不能なマスの最短距離は Integer.MAX_VALUE となる。
     *
     * @param R 行数
     * @param C 列数
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R * C)
     */
    // 未テスト
    public static int[][] bfs(int R, int C, boolean[][] isObstacle, int si, int sj) {
        return bfs(R, C, null, null, isObstacle, si, sj);
    }

    /**
     * {@link #bfs(int, int, boolean[][], int, int)} が返した距離配列から、
     * 始点 (si, sj) から終点 (ti, tj) への4近傍最短路を一つ復元する。
     * 戻り値を P とすると、P[0] = (si, sj)、P[P.length - 1] = (ti, tj) であり、
     * 任意の 0 < k < P.length に対して dist[P[k - 1][0]][P[k - 1][1]] = dist[P[k][0]][P[k][1]] - 1 である。
     * 終点が到達不能な場合、または dist が指定された始点からのBFS距離として整合しない場合は null を返す。
     *
     * @param dist {@code bfs(R, C, isObstacle, si, sj)} が返した R x C の距離配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return 始点と終点を含む最短路上の座標 [r, c] の配列。復元できない場合は null
     * @complexity O(L)（L は返すパスに含まれるマス数）
     */
    // 未テスト
    public static int[][] restoreShortestPath(int[][] dist, int si, int sj, int ti, int tj) {
        if (dist == null || dist.length == 0) return null;
        int R = dist.length;
        int C = dist[0] == null ? 0 : dist[0].length;
        if (C == 0) return null;
        for (int r = 0; r < R; r++) {
            if (dist[r] == null || dist[r].length != C) return null;
        }
        if (si < 0 || si >= R || sj < 0 || sj >= C
                || ti < 0 || ti >= R || tj < 0 || tj >= C
                || dist[si][sj] != 0 || dist[ti][tj] < 0 || dist[ti][tj] == Integer.MAX_VALUE) {
            return null;
        }

        int[][] path = new int[dist[ti][tj] + 1][2];
        int r = ti;
        int c = tj;
        for (int k = path.length - 1; k >= 0; k--) {
            path[k][0] = r;
            path[k][1] = c;
            if (k == 0) break;

            boolean found = false;
            for (int dir = 0; dir < 4; dir++) {
                int nr = r + dh[dir];
                int nc = c + dw[dir];
                if (nr >= 0 && nr < R && nc >= 0 && nc < C && dist[nr][nc] == k - 1) {
                    r = nr;
                    c = nc;
                    found = true;
                    break;
                }
            }
            if (!found) return null;
        }
        if (r != si || c != sj) return null;
        return path;
    }

    /**
     * マス (r, c) から方向 dir への移動における辺の重み（0 または 1）を取得する。
     * dir は Grid2D (0: 下, 1: 右, 2: 上, 3: 左) に対応する。
     *
     * @param r 移動元の行インデックス
     * @param c 移動元の列インデックス
     * @param dir 移動方向（0: 下, 1: 右, 2: 上, 3: 左）
     * @param edgeWeight R x C x 4 の 3 次元配列。edgeWeight[r][c][dir] はマス (r, c) から方向 dir への移動重み（0 または 1。null の場合はすべて 1）
     * @return 辺の重み（0 または 1）
     */
    private static int getEdgeWeight(int r, int c, int dir, int[][][] edgeWeight) {
        if (edgeWeight != null && r >= 0 && r < edgeWeight.length && edgeWeight[r] != null
                && c >= 0 && c < edgeWeight[r].length && edgeWeight[r][c] != null
                && dir >= 0 && dir < edgeWeight[r][c].length) {
            return edgeWeight[r][c][dir];
        }
        return 1;
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を01-BFSにより計算する。
     * 壁（blockedRight, blockedDown）、セル障害物（isObstacle）、および 3 次元配列の0/1辺重み edgeWeight[r][c][dir] を考慮する。
     * 到達不能なマスの最短距離は Integer.MAX_VALUE となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param edgeWeight R x C x 4 の辺重み配列。edgeWeight[r][c][dir] は Grid2D の dir 方向（0: 下, 1: 右, 2: 上, 3: 左）への重み（0 または 1。null の場合はすべて 1）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int[][][] edgeWeight, int si, int sj) {
        if (blockedRight != null) {
            if (blockedRight.length != R) {
                throw new IllegalArgumentException("blockedRight must have R rows");
            }
            for (int r = 0; r < R; r++) {
                if (blockedRight[r] == null || blockedRight[r].length != C - 1) {
                    throw new IllegalArgumentException("each row of blockedRight must have C - 1 columns");
                }
            }
        }
        if (blockedDown != null) {
            if (blockedDown.length != R - 1) {
                throw new IllegalArgumentException("blockedDown must have R - 1 rows");
            }
            for (int r = 0; r < R - 1; r++) {
                if (blockedDown[r] == null || blockedDown[r].length != C) {
                    throw new IllegalArgumentException("each row of blockedDown must have C columns");
                }
            }
        }
        int[][] dist = new int[R][C];
        int INF = Integer.MAX_VALUE;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && (si >= isObstacle.length || isObstacle[si] == null || sj >= isObstacle[si].length || isObstacle[si][sj])) {
            return dist;
        }

        ArrayDeque<int[]> dq = new ArrayDeque<>();
        dist[si][sj] = 0;
        dq.add(new int[]{si, sj});

        while (!dq.isEmpty()) {
            int[] curr = dq.pollFirst();
            int r = curr[0];
            int c = curr[1];

            for (int dir = 0; dir < 4; dir++) {
                if (canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    int w = getEdgeWeight(r, c, dir, edgeWeight);
                    if (w < 0 || w > 1) continue;
                    if (dist[r][c] + w < dist[nr][nc]) {
                        dist[nr][nc] = dist[r][c] + w;
                        if (w == 0) {
                            dq.addFirst(new int[]{nr, nc});
                        } else {
                            dq.addLast(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        return dist;
    }

    public static int[][] bfs01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int[][][] edgeWeight, int si, int sj) {
        return bfs01(R, C, blockedRight, blockedDown, null, edgeWeight, si, sj);
    }

    public static int[][] bfs01(int R, int C, boolean[][] isObstacle, int[][][] edgeWeight, int si, int sj) {
        return bfs01(R, C, null, null, isObstacle, edgeWeight, si, sj);
    }

    public static int[][] bfs01(int R, int C, int[][][] edgeWeight, int si, int sj) {
        return bfs01(R, C, null, null, null, edgeWeight, si, sj);
    }

    /**
     * 始点集合 S と終点集合 T の目で辺重みの総和が最小となる4近傍パスを01-BFSにより一つ返す。
     * 壁（blockedRight, blockedDown）、セル障害物（isObstacle）、および 3 次元配列の0/1辺重み edgeWeight[r][c][dir] を考慮する。
     * 同点の分岐（最小コストの終点が複数存在する場合、またはパス復元時に移動候補が複数存在する場合）がある場合、
     * rnd が非 null であればランダムに選択する。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。null の場合は水平壁なし
     * @param isObstacle セル障害物情報。null の場合は障害物なし
     * @param edgeWeight R x C x 4 の辺重み配列。edgeWeight[r][c][dir] は Grid2D の dir 方向（0: 下, 1: 右, 2: 上, 3: 左）への重み（0 または 1。null の場合はすべて 1）
     * @param sources 始点座標 [r, c] の配列
     * @param targets 終点座標 [r, c] の配列
     * @param rnd 同点時の分岐選択用乱数生成器（null の場合は先頭を選択）
     * @return 始点と終点を含む最小コストパス上の座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C + |S| + |T|)
     */
    // 未テスト
    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            boolean[][] isObstacle, int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        if (R <= 0 || C <= 0 || sources == null || targets == null) return null;
        int INF = Integer.MAX_VALUE;
        int[][] dist = new int[R][C];
        int[][] stepDist = new int[R][C];
        boolean[][] isSource = new boolean[R][C];
        ArrayUtils.fill(dist, INF);
        ArrayUtils.fill(stepDist, INF);
        ArrayDeque<int[]> dq = new ArrayDeque<>();

        for (int[] source : sources) {
            if (source == null || source.length != 2) continue;
            int r = source[0];
            int c = source[1];
            if (r < 0 || r >= R || c < 0 || c >= C) continue;
            if (isObstacle != null && (r >= isObstacle.length || isObstacle[r] == null || c >= isObstacle[r].length || isObstacle[r][c])) continue;
            isSource[r][c] = true;
            if (dist[r][c] != 0) {
                dist[r][c] = 0;
                stepDist[r][c] = 0;
                dq.addFirst(new int[]{r, c});
            }
        }

        while (!dq.isEmpty()) {
            int[] curr = dq.pollFirst();
            int r = curr[0];
            int c = curr[1];

            for (int dir = 0; dir < 4; dir++) {
                if (!canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) continue;
                int nr = r + dh[dir];
                int nc = c + dw[dir];
                int w = getEdgeWeight(r, c, dir, edgeWeight);
                if (w < 0 || w > 1) continue;
                if (dist[r][c] + w < dist[nr][nc]) {
                    dist[nr][nc] = dist[r][c] + w;
                    stepDist[nr][nc] = stepDist[r][c] + 1;
                    if (w == 0) {
                        dq.addFirst(new int[]{nr, nc});
                    } else {
                        dq.addLast(new int[]{nr, nc});
                    }
                } else if (dist[r][c] + w == dist[nr][nc]) {
                    if (stepDist[r][c] + 1 < stepDist[nr][nc]) {
                        stepDist[nr][nc] = stepDist[r][c] + 1;
                        if (w == 0) {
                            dq.addFirst(new int[]{nr, nc});
                        } else {
                            dq.addLast(new int[]{nr, nc});
                        }
                    }
                }
            }
        }

        java.util.ArrayList<int[]> bestTargets = new java.util.ArrayList<>();
        int bestDistance = INF;
        for (int[] candidate : targets) {
            if (candidate == null || candidate.length != 2) continue;
            int r = candidate[0];
            int c = candidate[1];
            if (r >= 0 && r < R && c >= 0 && c < C && dist[r][c] < INF) {
                if (dist[r][c] < bestDistance) {
                    bestDistance = dist[r][c];
                    bestTargets.clear();
                    bestTargets.add(new int[]{r, c});
                } else if (dist[r][c] == bestDistance) {
                    bestTargets.add(new int[]{r, c});
                }
            }
        }
        if (bestTargets.isEmpty()) return null;

        int[] chosenTarget;
        if (bestTargets.size() > 1 && rnd != null) {
            chosenTarget = bestTargets.get(rnd.nextInt(bestTargets.size()));
        } else {
            chosenTarget = bestTargets.get(0);
        }

        java.util.ArrayList<int[]> reversedPath = new java.util.ArrayList<>();
        int currR = chosenTarget[0];
        int currC = chosenTarget[1];
        reversedPath.add(new int[]{currR, currC});

        while (!isSource[currR][currC]) {
            java.util.ArrayList<int[]> candidates = new java.util.ArrayList<>();
            for (int dir = 0; dir < 4; dir++) {
                int prevR = currR - dh[dir];
                int prevC = currC - dw[dir];
                if (prevR >= 0 && prevR < R && prevC >= 0 && prevC < C) {
                    if (canMove(prevR, prevC, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                        int w = getEdgeWeight(prevR, prevC, dir, edgeWeight);
                        if (dist[prevR][prevC] != INF && dist[prevR][prevC] + w == dist[currR][currC]
                                && stepDist[prevR][prevC] == stepDist[currR][currC] - 1) {
                            candidates.add(new int[]{prevR, prevC});
                        }
                    }
                }
            }
            if (candidates.isEmpty()) break;
            int[] chosenPrev;
            if (candidates.size() > 1 && rnd != null) {
                chosenPrev = candidates.get(rnd.nextInt(candidates.size()));
            } else {
                chosenPrev = candidates.get(0);
            }
            currR = chosenPrev[0];
            currC = chosenPrev[1];
            reversedPath.add(new int[]{currR, currC});
        }

        int pathLen = reversedPath.size();
        int[][] path = new int[pathLen][2];
        for (int k = 0; k < pathLen; k++) {
            path[k] = reversedPath.get(pathLen - 1 - k);
        }
        return path;
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            boolean[][] isObstacle, int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, sources, targets, null);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, null, edgeWeight, sources, targets, rnd);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, blockedRight, blockedDown, null, edgeWeight, sources, targets, null);
    }

    public static int[][] findPath01(int R, int C, boolean[][] isObstacle,
            int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, null, null, isObstacle, edgeWeight, sources, targets, rnd);
    }

    public static int[][] findPath01(int R, int C, boolean[][] isObstacle,
            int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, null, null, isObstacle, edgeWeight, sources, targets, null);
    }

    public static int[][] findPath01(int R, int C, int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, null, null, null, edgeWeight, sources, targets, rnd);
    }

    public static int[][] findPath01(int R, int C, int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, null, null, null, edgeWeight, sources, targets, null);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, new int[][]{{si, sj}}, new int[][]{{ti, tj}}, rnd);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, null, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, blockedRight, blockedDown, null, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath01(int R, int C, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, null, null, isObstacle, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath01(int R, int C, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, null, null, isObstacle, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath01(int R, int C, int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, null, null, null, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath01(int R, int C, int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, null, null, null, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, blockedRight, blockedDown, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, int[][][] edgeWeight, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, edgeWeight, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, int[][][] edgeWeight, int si, int sj, int ti, int tj) {
        return findPath01(R, C, edgeWeight, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, edgeWeight, sources, targets, null);
    }

    public static int[][] findPath(int R, int C, int[][][] edgeWeight, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, edgeWeight, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, int[][][] edgeWeight, int[][] sources, int[][] targets) {
        return findPath01(R, C, edgeWeight, sources, targets, null);
    }

    /**
     * マス (r, c) から方向 dir への移動における辺の重み（0 または 1）を取得する。
     *
     * @param r 移動元の行インデックス
     * @param c 移動元の列インデックス
     * @param dir 移動方向（0: 下, 1: 左, 2: 上, 3: 右）
     * @param weightRight R x (C - 1) の水平辺重み配列（null の場合はすべて 1）
     * @param weightDown (R - 1) x C の垂直辺重み配列（null の場合はすべて 1）
     * @return 辺の重み（0 または 1）
     */
    private static int getEdgeWeight(int r, int c, int dir, int[][] weightRight, int[][] weightDown) {
        if (dir == 0) { // 下 (r, c) -> (r + 1, c)
            return weightDown != null && r >= 0 && r < weightDown.length && weightDown[r] != null && c >= 0 && c < weightDown[r].length ? weightDown[r][c] : 1;
        } else if (dir == 1) { // 右 (r, c) -> (r, c + 1)
            return weightRight != null && r >= 0 && r < weightRight.length && weightRight[r] != null && c >= 0 && c < weightRight[r].length ? weightRight[r][c] : 1;
        } else if (dir == 2) { // 上 (r, c) -> (r - 1, c)
            int nr = r - 1;
            return weightDown != null && nr >= 0 && nr < weightDown.length && weightDown[nr] != null && c >= 0 && c < weightDown[nr].length ? weightDown[nr][c] : 1;
        } else if (dir == 3) { // 左 (r, c) -> (r, c - 1)
            int nc = c - 1;
            return weightRight != null && r >= 0 && r < weightRight.length && weightRight[r] != null && nc >= 0 && nc < weightRight[r].length ? weightRight[r][nc] : 1;
        }
        return 1;
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を01-BFSにより計算する。
     * 壁（blockedRight, blockedDown）、セル障害物（isObstacle）、および隣接マス間の0/1辺重み（weightRight, weightDown）を考慮する。
     * 到達不能なマスの最短距離は Integer.MAX_VALUE となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param weightRight R x (C - 1) の水平辺重み配列（0 または 1。null の場合はすべて 1）
     * @param weightDown (R - 1) x C の垂直辺重み配列（0 または 1。null の場合はすべて 1）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int[][] weightRight, int[][] weightDown, int si, int sj) {
        if (blockedRight != null) {
            if (blockedRight.length != R) {
                throw new IllegalArgumentException("blockedRight must have R rows");
            }
            for (int r = 0; r < R; r++) {
                if (blockedRight[r] == null || blockedRight[r].length != C - 1) {
                    throw new IllegalArgumentException("each row of blockedRight must have C - 1 columns");
                }
            }
        }
        if (blockedDown != null) {
            if (blockedDown.length != R - 1) {
                throw new IllegalArgumentException("blockedDown must have R - 1 rows");
            }
            for (int r = 0; r < R - 1; r++) {
                if (blockedDown[r] == null || blockedDown[r].length != C) {
                    throw new IllegalArgumentException("each row of blockedDown must have C columns");
                }
            }
        }
        int[][] dist = new int[R][C];
        int INF = Integer.MAX_VALUE;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && (si >= isObstacle.length || isObstacle[si] == null || sj >= isObstacle[si].length || isObstacle[si][sj])) {
            return dist;
        }

        ArrayDeque<int[]> dq = new ArrayDeque<>();
        dist[si][sj] = 0;
        dq.add(new int[]{si, sj});

        while (!dq.isEmpty()) {
            int[] curr = dq.pollFirst();
            int r = curr[0];
            int c = curr[1];

            for (int dir = 0; dir < 4; dir++) {
                if (canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    int w = getEdgeWeight(r, c, dir, weightRight, weightDown);
                    if (w < 0 || w > 1) continue;
                    if (dist[r][c] + w < dist[nr][nc]) {
                        dist[nr][nc] = dist[r][c] + w;
                        if (w == 0) {
                            dq.addFirst(new int[]{nr, nc});
                        } else {
                            dq.addLast(new int[]{nr, nc});
                        }
                    }
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を01-BFSにより計算する。
     * 壁（blockedRight, blockedDown）および隣接マス間の0/1辺重みを考慮する。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param weightRight R x (C - 1) の水平辺重み配列（0 または 1）
     * @param weightDown (R - 1) x C の垂直辺重み配列（0 または 1）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs01(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int[][] weightRight, int[][] weightDown, int si, int sj) {
        return bfs01(R, C, blockedRight, blockedDown, null, weightRight, weightDown, si, sj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を01-BFSにより計算する。
     * セル障害物（isObstacle）および隣接マス間の0/1辺重みを考慮する。
     *
     * @param R 行数
     * @param C 列数
     * @param isObstacle セル障害物情報
     * @param weightRight R x (C - 1) の水平辺重み配列（0 または 1）
     * @param weightDown (R - 1) x C の垂直辺重み配列（0 または 1）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs01(int R, int C, boolean[][] isObstacle, int[][] weightRight, int[][] weightDown, int si, int sj) {
        return bfs01(R, C, null, null, isObstacle, weightRight, weightDown, si, sj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を01-BFSにより計算する。
     * 隣接マス間の0/1辺重みを考慮する。
     *
     * @param R 行数
     * @param C 列数
     * @param weightRight R x (C - 1) の水平辺重み配列（0 または 1）
     * @param weightDown (R - 1) x C の垂直辺重み配列（0 または 1）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] bfs01(int R, int C, int[][] weightRight, int[][] weightDown, int si, int sj) {
        return bfs01(R, C, null, null, null, weightRight, weightDown, si, sj);
    }

    /**
     * 始点 (si, sj) からの滑る床（icefloor）上での最短距離を01-BFSにより計算する。
     * 各マスにおいて、移動を開始した方向（0: 下, 1: 左, 2: 上, 3: 右）ごとに到着距離を求める。
     *
     * 一度進むと壁（blockedRight, blockedDown）や障害物、またはグリッドの境界に衝突するまで止まれない。
     * 移動中に止まらずに通過した場合、その方向から到着したとして
     * 状態（方向、行、列）の距離を求める。
     *
     * 初期状態は始点 (si, sj) からある方向に移動を開始しているとして、
     * すべての移動開始方向に対して距離を 1 とする。
     * また、止まった位置で方向転換をするごとに距離が 1 増加する。
     * 到達不能なマスの状態に対する距離は Integer.MAX_VALUE / 3 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。blockedRight[r][c] が true ならば、マス (r, c) と (r, c + 1) の間を通過できない。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。blockedDown[r][c] が true ならば、マス (r, c) と (r + 1, c) の間を通過できない。null の場合は水平壁なし
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return dist[dir][r][c]（dir方向への移動中にマス (r, c) を通過するための最小コスト）
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][][] bfsIcefloor(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj) {
        if (blockedRight != null) {
            if (blockedRight.length != R) {
                throw new IllegalArgumentException("blockedRight must have R rows");
            }
            for (int r = 0; r < R; r++) {
                if (blockedRight[r] == null || blockedRight[r].length != C - 1) {
                    throw new IllegalArgumentException("each row of blockedRight must have C - 1 columns");
                }
            }
        }
        if (blockedDown != null) {
            if (blockedDown.length != R - 1) {
                throw new IllegalArgumentException("blockedDown must have R - 1 rows");
            }
            for (int r = 0; r < R - 1; r++) {
                if (blockedDown[r] == null || blockedDown[r].length != C) {
                    throw new IllegalArgumentException("each row of blockedDown must have C columns");
                }
            }
        }
        ArrayDeque<int[]> dq = new ArrayDeque<>();
        int[][][] dist = new int[4][R][C];
        int INF = Integer.MAX_VALUE / 3;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && isObstacle[si][sj]) {
            return dist;
        }

        for (int dir = 0; dir < 4; dir++) {
            dist[dir][si][sj] = 1;
            dq.add(new int[]{dir, si, sj});
        }

        while (!dq.isEmpty()) {
            int[] state = dq.pollFirst();
            int dir = state[0];
            int x = state[1];
            int y = state[2];

            if (canMove(x, y, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                int nx = x + dh[dir];
                int ny = y + dw[dir];
                if (dist[dir][nx][ny] > dist[dir][x][y]) {
                    dist[dir][nx][ny] = dist[dir][x][y];
                    dq.addFirst(new int[]{dir, nx, ny});
                }
            } else {
                for (int ndir = 0; ndir < 4; ndir++) {
                    if (ndir == dir) continue;
                    if (dist[ndir][x][y] > 1 + dist[dir][x][y]) {
                        dist[ndir][x][y] = 1 + dist[dir][x][y];
                        dq.addLast(new int[]{ndir, x, y});
                    }
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) からの滑る床（icefloor）上での最短距離を01-BFSにより計算する。
     * 到達不能なマスの状態に対する距離は Integer.MAX_VALUE / 3 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。blockedRight[r][c] が true ならば、マス (r, c) と (r, c + 1) の間を通過できない。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。blockedDown[r][c] が true ならば、マス (r, c) と (r + 1, c) の間を通過できない。null の場合は水平壁なし
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return dist[dir][r][c]（dir方向への移動中にマス (r, c) を通過するための最小コスト）
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][][] bfsIcefloor(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj) {
        return bfsIcefloor(R, C, blockedRight, blockedDown, null, si, sj);
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への通常のBFSによる最短パス（マス目の座標列）を取得する。
     * 壁（blockedRight, blockedDown）およびセル障害物（isObstacle）による通行不能を考慮する。
     * 同点の分岐がある場合は rnd が非 null であればランダムに選択する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @param rnd 同点時の分岐選択用乱数生成器（null の場合は先頭を選択）
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findShortestPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, (int[][][]) null, si, sj, ti, tj, rnd);
    }

    public static int[][] findShortestPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, blockedRight, blockedDown, isObstacle, si, sj, ti, tj, null);
    }

    public static int[][] findShortestPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findShortestPath(R, C, blockedRight, blockedDown, null, si, sj, ti, tj, rnd);
    }

    public static int[][] findShortestPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, blockedRight, blockedDown, null, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findShortestPath(R, C, blockedRight, blockedDown, null, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, blockedRight, blockedDown, null, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findShortestPath(R, C, blockedRight, blockedDown, isObstacle, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, blockedRight, blockedDown, isObstacle, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] isObstacle, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findShortestPath(R, C, null, null, isObstacle, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] isObstacle, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, null, null, isObstacle, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, int si, int sj, int ti, int tj, java.util.Random rnd) {
        return findShortestPath(R, C, null, null, null, si, sj, ti, tj, rnd);
    }

    public static int[][] findPath(int R, int C, int si, int sj, int ti, int tj) {
        return findShortestPath(R, C, null, null, null, si, sj, ti, tj, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            boolean[][] isObstacle, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath01(R, C, blockedRight, blockedDown, isObstacle, (int[][][]) null, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            boolean[][] isObstacle, int[][] sources, int[][] targets) {
        return findPath(R, C, blockedRight, blockedDown, isObstacle, sources, targets, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath(R, C, blockedRight, blockedDown, null, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][] sources, int[][] targets) {
        return findPath(R, C, blockedRight, blockedDown, null, sources, targets, null);
    }

    public static int[][] findPath(int R, int C, boolean[][] isObstacle,
            int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath(R, C, null, null, isObstacle, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, boolean[][] isObstacle,
            int[][] sources, int[][] targets) {
        return findPath(R, C, null, null, isObstacle, sources, targets, null);
    }

    public static int[][] findPath(int R, int C, int[][] sources, int[][] targets, java.util.Random rnd) {
        return findPath(R, C, null, null, null, sources, targets, rnd);
    }

    public static int[][] findPath(int R, int C, int[][] sources, int[][] targets) {
        return findPath(R, C, null, null, null, sources, targets, null);
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への滑る床上での最短パス（通過するマス目の座標列）を取得する。
     * 移動中に通過するすべてのマスの座標（スライド中の途中経過マスを含む）が含まれる。
     * 壁（blockedRight, blockedDown）およびセル障害物（isObstacle）による通行不能を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findPathIcefloor(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj) {
        int[][][] dist = bfsIcefloor(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        if (ti < 0 || ti >= R || tj < 0 || tj >= C) {
            return null;
        }
        int minDist = Integer.MAX_VALUE / 3;
        int targetDir = -1;
        for (int dir = 0; dir < 4; dir++) {
            if (dist[dir][ti][tj] < minDist) {
                minDist = dist[dir][ti][tj];
                targetDir = dir;
            }
        }
        if (minDist == Integer.MAX_VALUE / 3) {
            return null;
        }

        java.util.ArrayList<int[]> pathList = new java.util.ArrayList<>();
        int currR = ti;
        int currC = tj;
        int currDir = targetDir;

        pathList.add(new int[]{currR, currC});

        while (!(currR == si && currC == sj && dist[currDir][currR][currC] == 1)) {
            // 0-weight スライド遷移を試みる
            int prevR = currR - dh[currDir];
            int prevC = currC - dw[currDir];
            if (prevR >= 0 && prevR < R && prevC >= 0 && prevC < C
                && canMove(prevR, prevC, currDir, R, C, blockedRight, blockedDown, isObstacle)
                && dist[currDir][prevR][prevC] == dist[currDir][currR][currC]) {
                currR = prevR;
                currC = prevC;
                pathList.add(new int[]{currR, currC});
                continue;
            }

            // 1-weight 方向転換遷移を試みる
            boolean turned = false;
            for (int prevDir = 0; prevDir < 4; prevDir++) {
                if (prevDir == currDir) continue;
                if (!canMove(currR, currC, prevDir, R, C, blockedRight, blockedDown, isObstacle)
                    && dist[prevDir][currR][currC] == dist[currDir][currR][currC] - 1) {
                    currDir = prevDir;
                    turned = true;
                    break;
                }
            }
            if (!turned) {
                // 無限ループ防止用のフォールバック
                break;
            }
        }

        // 逆順で収集したため、反転する
        int len = pathList.size();
        int[][] res = new int[len][2];
        for (int i = 0; i < len; i++) {
            res[i] = pathList.get(len - 1 - i);
        }
        return res;
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への滑る床上での最短パス（通過するマス目の座標列）を取得する。
     * 移動中に通過するすべてのマスの座標（スライド中の途中経過マスを含む）が含まれる。
     * 壁（blockedRight, blockedDown）による通行不能を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findPathIcefloor(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj) {
        return findPathIcefloor(R, C, blockedRight, blockedDown, null, si, sj, ti, tj);
    }

    /**
     * 始点 (si, sj) からの、スライドの途中（任意のマスの境界）で停止して方向転換することが可能な滑る床上での最短距離を01-BFSにより計算する。
     * 各マスにおいて、移動を開始した方向（0: 下, 1: 左, 2: 上, 3: 右）ごとに到着距離を求める。
     *
     * プレイヤーは進行方向の正面に壁や障害物がない場合でも、任意のマスでスライドを停止し、
     * 別の方向に方向転換して再び移動を開始することができる。
     *
     * 初期状態は始点 (si, sj) からある方向に移動を開始しているとして、
     * すべての移動開始方向に対して距離を 1 とする。
     * また、任意のマスで停止して方向転換をするごとに距離が 1 増加する。
     * 到達不能なマスの状態に対する距離は Integer.MAX_VALUE / 3 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return dist[dir][r][c]（dir方向への移動中にマス (r, c) を通過するための最小コスト）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][][] bfsIcefloorWithStopping(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj) {
        ArrayDeque<int[]> dq = new ArrayDeque<>();
        int[][][] dist = new int[4][R][C];
        int INF = Integer.MAX_VALUE / 3;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && isObstacle[si][sj]) {
            return dist;
        }

        for (int dir = 0; dir < 4; dir++) {
            dist[dir][si][sj] = 1;
            dq.add(new int[]{dir, si, sj});
        }

        while (!dq.isEmpty()) {
            int[] state = dq.pollFirst();
            int dir = state[0];
            int x = state[1];
            int y = state[2];

            // 1. スライド移動（コスト0、常に可能な場合に実行）
            if (canMove(x, y, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                int nx = x + dh[dir];
                int ny = y + dw[dir];
                if (dist[dir][nx][ny] == INF) {
                    dist[dir][nx][ny] = dist[dir][x][y];
                    dq.addFirst(new int[]{dir, nx, ny});
                }
            }

            // 2. 任意の場所での方向転換（コスト1、常に可能）
            for (int ndir = 0; ndir < 4; ndir++) {
                if (ndir == dir) continue;
                if (dist[ndir][x][y] == INF) {
                    dist[ndir][x][y] = 1 + dist[dir][x][y];
                    dq.addLast(new int[]{ndir, x, y});
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) からの、スライドの途中（任意のマスの境界）で停止して方向転換することが可能な滑る床上での最短距離を01-BFSにより計算する。
     * 到達不能なマスの状態に対する距離は Integer.MAX_VALUE / 3 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return dist[dir][r][c]（dir方向への移動中にマス (r, c) を通過するための最小コスト）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][][] bfsIcefloorWithStopping(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj) {
        return bfsIcefloorWithStopping(R, C, blockedRight, blockedDown, null, si, sj);
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への、スライドの途中での停止・方向転換が可能な滑る床上での最短パス（通過するマス目の座標列）を取得する。
     * 壁（blockedRight, blockedDown）およびセル障害物（isObstacle）による通行不能を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findPathIcefloorWithStopping(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle, int si, int sj, int ti, int tj) {
        int[][][] dist = bfsIcefloorWithStopping(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        if (ti < 0 || ti >= R || tj < 0 || tj >= C) {
            return null;
        }
        int minDist = Integer.MAX_VALUE / 3;
        int targetDir = -1;
        for (int dir = 0; dir < 4; dir++) {
            if (dist[dir][ti][tj] < minDist) {
                minDist = dist[dir][ti][tj];
                targetDir = dir;
            }
        }
        if (minDist == Integer.MAX_VALUE / 3) {
            return null;
        }

        java.util.ArrayList<int[]> pathList = new java.util.ArrayList<>();
        int currR = ti;
        int currC = tj;
        int currDir = targetDir;

        pathList.add(new int[]{currR, currC});

        while (!(currR == si && currC == sj && dist[currDir][currR][currC] == 1)) {
            // 0-weight スライド遷移を試みる
            int prevR = currR - dh[currDir];
            int prevC = currC - dw[currDir];
            if (prevR >= 0 && prevR < R && prevC >= 0 && prevC < C
                && canMove(prevR, prevC, currDir, R, C, blockedRight, blockedDown, isObstacle)
                && dist[currDir][prevR][prevC] == dist[currDir][currR][currC]) {
                currR = prevR;
                currC = prevC;
                pathList.add(new int[]{currR, currC});
                continue;
            }

            // 1-weight 方向転換遷移を試みる（停止可能なので、canMoveの状況に関わらず常に可能）
            boolean turned = false;
            for (int prevDir = 0; prevDir < 4; prevDir++) {
                if (prevDir == currDir) continue;
                if (dist[prevDir][currR][currC] == dist[currDir][currR][currC] - 1) {
                    currDir = prevDir;
                    turned = true;
                    break;
                }
            }
            if (!turned) {
                break;
            }
        }

        // 逆順収集を反転
        int len = pathList.size();
        int[][] res = new int[len][2];
        for (int i = 0; i < len; i++) {
            res[i] = pathList.get(len - 1 - i);
        }
        return res;
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への、スライドの途中での停止・方向転換が可能な滑る床上での最短パス（通過するマス目の座標列）を取得する。
     * 壁（blockedRight, blockedDown）による通行不能を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報
     * @param blockedDown 水平壁情報
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findPathIcefloorWithStopping(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int si, int sj, int ti, int tj) {
        return findPathIcefloorWithStopping(R, C, blockedRight, blockedDown, null, si, sj, ti, tj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を頂点重み付きダイクストラ法により計算する。
     * 壁（blockedRight, blockedDown）による通行不能および頂点重み（weight）を考慮する。
     * 頂点重みが Long.MAX_VALUE / 2 以上（または負の値）のマスは障害物（通行不能）として扱う。
     * 到達不能なマスの最短距離は Long.MAX_VALUE / 2 となる。
     * 始点 (si, sj) 自身の頂点重み weight[si][sj] も初期距離として加算される。
     *
     * 頂点重み付き最短経路の性質により、ある頂点に最初に到達した時点でその距離が確定するため、
     * 各頂点は優先度付きキューへ高々1回しか追加されない。
     * これにより二分ヒープを用いて O(M + N log N)（M は辺数、N は頂点数）で高速に計算される。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static long[][] dijkstra(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, long[][] weight, int si, int sj) {
        long[][] dist = new long[R][C];
        long INF = Long.MAX_VALUE / 2;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C || weight == null) {
            return dist;
        }
        if (weight[si][sj] >= INF || weight[si][sj] < 0) {
            return dist;
        }

        PriorityQueue<long[]> pq = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        dist[si][sj] = weight[si][sj];
        pq.add(new long[]{dist[si][sj], si, sj});

        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long d = curr[0];
            int r = (int) curr[1];
            int c = (int) curr[2];
            if (d > dist[r][c]) continue;

            for (int dir = 0; dir < 4; dir++) {
                if (canMove(r, c, dir, R, C, blockedRight, blockedDown, null)) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    if (weight[nr][nc] >= INF || weight[nr][nc] < 0) {
                        continue;
                    }
                    if (dist[r][c] + weight[nr][nc] < dist[nr][nc]) {
                        dist[nr][nc] = dist[r][c] + weight[nr][nc];
                        pq.add(new long[]{dist[nr][nc], nr, nc});
                    }
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を頂点重み付きダイクストラ法により計算する。
     * 頂点重み（weight）を考慮する。
     * 到達不能なマスの最短距離は Long.MAX_VALUE / 2 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static long[][] dijkstra(int R, int C, long[][] weight, int si, int sj) {
        return dijkstra(R, C, null, null, weight, si, sj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を頂点重み付きダイクストラ法により計算する。
     * 壁（blockedRight, blockedDown）による通行不能および頂点重み（weight）を考慮する。
     * 到達不能なマスの最短距離は Long.MAX_VALUE / 2 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static long[][] dijkstra(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int[][] weight, int si, int sj) {
        if (weight == null) {
            return dijkstra(R, C, blockedRight, blockedDown, (long[][]) null, si, sj);
        }
        long INF = Long.MAX_VALUE / 2;
        int INT_INF = Integer.MAX_VALUE / 2;
        long[][] wLong = new long[R][C];
        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                if (weight[i][j] >= INT_INF || weight[i][j] < 0) {
                    wLong[i][j] = INF;
                } else {
                    wLong[i][j] = weight[i][j];
                }
            }
        }
        return dijkstra(R, C, blockedRight, blockedDown, wLong, si, sj);
    }

    /**
     * 始点 (si, sj) から各マスへの最短距離を頂点重み付きダイクストラ法により計算する。
     * 頂点重み（weight）を考慮する。
     * 到達不能なマスの最短距離は Long.MAX_VALUE / 2 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @return 各マスへの最短距離を表す R x C の2次元配列
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static long[][] dijkstra(int R, int C, int[][] weight, int si, int sj) {
        return dijkstra(R, C, null, null, weight, si, sj);
    }


    /**
     * 始点 (si, sj) から終点 (ti, tj) への頂点重み付きグリッド上での最短パス（マス目の座標列）を取得する。
     * 壁（blockedRight, blockedDown）による通行不能および頂点重み（weight）を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, long[][] weight, int si, int sj, int ti, int tj) {
        long[][] dist = dijkstra(R, C, blockedRight, blockedDown, weight, si, sj);
        long INF = Long.MAX_VALUE / 2;
        if (ti < 0 || ti >= R || tj < 0 || tj >= C || dist[ti][tj] >= INF) {
            return null;
        }

        java.util.ArrayList<int[]> pathList = new java.util.ArrayList<>();
        int currR = ti;
        int currC = tj;
        pathList.add(new int[]{currR, currC});

        while (!(currR == si && currC == sj)) {
            boolean moved = false;
            for (int dir = 0; dir < 4; dir++) {
                int prevR = currR - dh[dir];
                int prevC = currC - dw[dir];
                if (prevR >= 0 && prevR < R && prevC >= 0 && prevC < C) {
                    if (canMove(prevR, prevC, dir, R, C, blockedRight, blockedDown, null)) {
                        if (dist[prevR][prevC] + weight[currR][currC] == dist[currR][currC]) {
                            currR = prevR;
                            currC = prevC;
                            pathList.add(new int[]{currR, currC});
                            moved = true;
                            break;
                        }
                    }
                }
            }
            if (!moved) {
                break;
            }
        }

        int len = pathList.size();
        int[][] res = new int[len][2];
        for (int i = 0; i < len; i++) {
            res[i] = pathList.get(len - 1 - i);
        }
        return res;
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への頂点重み付きグリッド上での最短パス（マス目の座標列）を取得する。
     * 頂点重み（weight）を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static int[][] findPath(int R, int C, long[][] weight, int si, int sj, int ti, int tj) {
        return findPath(R, C, null, null, weight, si, sj, ti, tj);
    }

    /**
     * 始点集合 S と終点集合 T の間で頂点重みの総和が最小となる4近傍パスを一つ返す。
     * パス P のコストは sum_{v in P} weight[v] とし、始点と終点の重みも含む。
     * 最小コストの終点が複数ある場合は targets で最初に現れるものを選ぶ。
     * 範囲外の座標、長さが2でない座標、および通行不能なマス上の座標は集合から除外する。
     * 有効な始点と終点を結ぶパスが存在しない場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。null の場合は水平壁なし
     * @param weight 各マスの頂点重み。負または Long.MAX_VALUE / 2 以上のマスは通行不能
     * @param sources 始点座標 [r, c] の配列
     * @param targets 終点座標 [r, c] の配列
     * @return 始点と終点を含む最小コストパス上の座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C) + |S| + |T|)
     */
    // 未テスト
    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            long[][] weight, int[][] sources, int[][] targets) {
        if (R <= 0 || C <= 0 || weight == null || sources == null || targets == null) return null;
        long INF = Long.MAX_VALUE / 2;
        long[][] dist = new long[R][C];
        int[][] parent = new int[R][C];
        ArrayUtils.fill(dist, INF);
        ArrayUtils.fill(parent, -2);
        PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));

        for (int[] source : sources) {
            if (source == null || source.length != 2) continue;
            int r = source[0];
            int c = source[1];
            if (r < 0 || r >= R || c < 0 || c >= C
                    || weight[r][c] < 0 || weight[r][c] >= INF || weight[r][c] >= dist[r][c]) continue;
            dist[r][c] = weight[r][c];
            parent[r][c] = -1;
            queue.add(new long[]{dist[r][c], r, c});
        }

        while (!queue.isEmpty()) {
            long[] state = queue.poll();
            long distance = state[0];
            int r = (int) state[1];
            int c = (int) state[2];
            if (distance != dist[r][c]) continue;
            for (int dir = 0; dir < 4; dir++) {
                if (!canMove(r, c, dir, R, C, blockedRight, blockedDown, null)) continue;
                int nr = r + dh[dir];
                int nc = c + dw[dir];
                long w = weight[nr][nc];
                if (w < 0 || w >= INF) continue;
                long nextDistance = distance > INF - w ? INF : distance + w;
                if (nextDistance >= dist[nr][nc]) continue;
                dist[nr][nc] = nextDistance;
                parent[nr][nc] = r * C + c;
                queue.add(new long[]{nextDistance, nr, nc});
            }
        }

        int target = -1;
        long bestDistance = INF;
        for (int[] candidate : targets) {
            if (candidate == null || candidate.length != 2) continue;
            int r = candidate[0];
            int c = candidate[1];
            if (r >= 0 && r < R && c >= 0 && c < C && dist[r][c] < bestDistance) {
                bestDistance = dist[r][c];
                target = r * C + c;
            }
        }
        if (target == -1) return null;

        java.util.ArrayList<int[]> reversedPath = new java.util.ArrayList<>();
        for (int v = target; v != -1; ) {
            int r = v / C;
            int c = v % C;
            reversedPath.add(new int[]{r, c});
            v = parent[r][c];
        }
        int[][] path = new int[reversedPath.size()][2];
        for (int k = 0; k < path.length; k++) {
            path[k] = reversedPath.get(path.length - 1 - k);
        }
        return path;
    }

    /**
     * 始点集合 S と終点集合 T の間で頂点重みの総和が最小となる4近傍パスを一つ返す。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み。負または Long.MAX_VALUE / 2 以上のマスは通行不能
     * @param sources 始点座標 [r, c] の配列
     * @param targets 終点座標 [r, c] の配列
     * @return 始点と終点を含む最小コストパス上の座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C) + |S| + |T|)
     */
    // 未テスト
    public static int[][] findPath(int R, int C, long[][] weight, int[][] sources, int[][] targets) {
        return findPath(R, C, null, null, weight, sources, targets);
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への頂点重み付きグリッド上での最短パス（マス目の座標列）を取得する。
     * 壁（blockedRight, blockedDown）による通行不能および頂点重み（weight）を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, int[][] weight, int si, int sj, int ti, int tj) {
        if (weight == null) {
            return findPath(R, C, blockedRight, blockedDown, (long[][]) null, si, sj, ti, tj);
        }
        long INF = Long.MAX_VALUE / 2;
        int INT_INF = Integer.MAX_VALUE / 2;
        long[][] wLong = new long[R][C];
        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                if (weight[i][j] >= INT_INF || weight[i][j] < 0) {
                    wLong[i][j] = INF;
                } else {
                    wLong[i][j] = weight[i][j];
                }
            }
        }
        return findPath(R, C, blockedRight, blockedDown, wLong, si, sj, ti, tj);
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への頂点重み付きグリッド上での最短パス（マス目の座標列）を取得する。
     * 頂点重み（weight）を考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み（コスト）を表す R x C の2次元配列
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @return パスを構成する各マスの座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C))
     */
    // 未テスト
    public static int[][] findPath(int R, int C, int[][] weight, int si, int sj, int ti, int tj) {
        return findPath(R, C, null, null, weight, si, sj, ti, tj);
    }

    /**
     * 始点集合 S と終点集合 T の間で頂点重みの総和が最小となる4近傍パスを一つ返す。
     * パス P のコストは sum_{v in P} weight[v] とし、始点と終点の重みも含む。
     * 負または Integer.MAX_VALUE / 2 以上の重みを持つマスは通行不能とする。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。null の場合は水平壁なし
     * @param weight 各マスの頂点重み
     * @param sources 始点座標 [r, c] の配列
     * @param targets 終点座標 [r, c] の配列
     * @return 始点と終点を含む最小コストパス上の座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C) + |S| + |T|)
     */
    // 未テスト
    public static int[][] findPath(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown,
            int[][] weight, int[][] sources, int[][] targets) {
        if (weight == null) return findPath(R, C, blockedRight, blockedDown, (long[][]) null, sources, targets);
        long INF = Long.MAX_VALUE / 2;
        int INT_INF = Integer.MAX_VALUE / 2;
        long[][] longWeight = new long[R][C];
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                longWeight[r][c] = weight[r][c] < 0 || weight[r][c] >= INT_INF ? INF : weight[r][c];
            }
        }
        return findPath(R, C, blockedRight, blockedDown, longWeight, sources, targets);
    }

    /**
     * 始点集合 S と終点集合 T の間で頂点重みの総和が最小となる4近傍パスを一つ返す。
     *
     * @param R 行数
     * @param C 列数
     * @param weight 各マスの頂点重み。負または Integer.MAX_VALUE / 2 以上のマスは通行不能
     * @param sources 始点座標 [r, c] の配列
     * @param targets 終点座標 [r, c] の配列
     * @return 始点と終点を含む最小コストパス上の座標 [r, c] の配列。到達不能時は null
     * @complexity O(R · C log(R · C) + |S| + |T|)
     */
    // 未テスト
    public static int[][] findPath(int R, int C, int[][] weight, int[][] sources, int[][] targets) {
        return findPath(R, C, null, null, weight, sources, targets);
    }
    


    /**
     * 始点からの最短路 DAG 上で、各頂点について祖先の値を集約する。
     *
     * 辺 u -> v は dist[u] + 1 = dist[v] を満たす4近傍間に張られる。
     *
     * @complexity O(R * C)
     */
    public static long[][] aggregateAncestors(
            int R, int C,
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj,
            long[][] value, LongBinaryOperator op) {
        int[][] dist = bfs(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        return aggregate(blockedRight, blockedDown, isObstacle, dist, value, op, -1);
    }

    /**
     * 始点からの最短路 DAG 上で、各頂点について子孫の値を集約する。
     *
     * 辺 u -> v は dist[u] + 1 = dist[v] を満たす4近傍間に張られる。
     *
     * @complexity O(R * C)
     */
    public static long[][] aggregateDescendants(
            int R, int C,
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj,
            long[][] value, LongBinaryOperator op) {
        int[][] dist = bfs(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        return aggregate(blockedRight, blockedDown, isObstacle, dist, value, op, 1);
    }

    private static long[][] aggregate(
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][] dist, long[][] value, LongBinaryOperator op, int delta) {

        int R = dist.length;
        int C = dist[0].length;
        long[][] res = new long[R][C];
        boolean[][] done = new boolean[R][C];

        class DFS {
            long dfs(int r, int c) {
                if (done[r][c]) return res[r][c];
                done[r][c] = true;

                long x = value[r][c];
                if (dist[r][c] == Integer.MAX_VALUE) {
                    return res[r][c] = x;
                }

                for (int dir = 0; dir < 4; dir++) {
                    if (!canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) continue;

                    int nr = r + dh[dir];
                    int nc = c + dw[dir];

                    if (dist[nr][nc] == dist[r][c] + delta) {
                        x = op.applyAsLong(x, dfs(nr, nc));
                    }
                }

                return res[r][c] = x;
            }
        }

        DFS dfs = new DFS();
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                dfs.dfs(r, c);
            }
        }
        return res;
    }
    
    
    
    /**
     * 始点からの最短路 DAG 上で、各頂点について祖先の値を集約する。
     *
     * 辺 u -> v は dist[u] + 1 = dist[v] を満たす4近傍間に張られる。
     *
     * @complexity O(R * C)
     */
    public static double[][] aggregateAncestors(
            int R, int C,
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj,
            double[][] value, DoubleBinaryOperator op) {
        int[][] dist = bfs(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        return aggregate(blockedRight, blockedDown, isObstacle, dist, value, op, -1);
    }

    /**
     * 始点からの最短路 DAG 上で、各頂点について子の値を集約する。
     * 各頂点 {@code u} の値は、{@code u} の子を列挙順に {@code v1, v2, ..., vk} として、
     * <pre> 
     * x = value[u] 
     * x = op(x, res[v1])
     * x = op(x, res[v2])
     * ...
     * x = op(x, res[vk])
     * </pre>
     * と計算される。
     * 
     * 辺 u -> v は dist[u] + 1 = dist[v] を満たす4近傍間に張られる。
     *
     * @complexity O(R * C)
     */
    public static double[][] aggregateDescendants(
            int R, int C,
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj,
            double[][] value, DoubleBinaryOperator op) {
        int[][] dist = bfs(R, C, blockedRight, blockedDown, isObstacle, si, sj);
        return aggregate(blockedRight, blockedDown, isObstacle, dist, value, op, 1);
    }

    private static double[][] aggregate(
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int[][] dist, double[][] value, DoubleBinaryOperator op, int delta) {

        int R = dist.length;
        int C = dist[0].length;
        double[][] res = new double[R][C];
        boolean[][] done = new boolean[R][C];

        class DFS {
            double dfs(int r, int c) {
                if (done[r][c]) return res[r][c];
                done[r][c] = true;

                double x = value[r][c];
                if (dist[r][c] == Integer.MAX_VALUE) {
                    return res[r][c] = x;
                }

                for (int dir = 0; dir < 4; dir++) {
                    if (!canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) continue;

                    int nr = r + dh[dir];
                    int nc = c + dw[dir];

                    if (dist[nr][nc] == dist[r][c] + delta) {
                        x = op.applyAsDouble(x, dfs(nr, nc));
                    }
                }

                return res[r][c] = x;
            }
        }

        DFS dfs = new DFS();
        for (int r = 0; r < R; r++) {
            for (int c = 0; c < C; c++) {
                dfs.dfs(r, c);
            }
        }
        return res;
    }
    
    
    /**
     * 始点 (si, sj) を固定した betweenness centrality を計算する。
     *
     * res[v] = Σ_(t != s,v) σ_st(v) / σ_st
     *
     * ここで σ_st は s から t への最短路数、
     * σ_st(v) はそのうち v を通る最短路数を表す。
     *
     * @complexity O(R * C)
     */
    public static double[][] betweennessCentralityFromSource(
            int R, int C,
            boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj) {

        double[][] res = new double[R][C];
        if (si < 0 || si >= R || sj < 0 || sj >= C
                || isObstacle != null && isObstacle[si][sj]) {
            return res;
        }

        int[][] dist = bfs(R, C, blockedRight, blockedDown, isObstacle, si, sj);

        // sigma[v] = s -> v の最短路数
        double[][] a = new double[R][C];
        a[si][sj] = 1;
        double[][] sigma = aggregate(
                blockedRight, blockedDown, isObstacle, dist, a, Double::sum, -1);

        /*
         * delta(v) = Σ_{v->w} sigma(v)/sigma(w) * (1 + delta(w))
         *
         * h(v) = (1 + delta(v)) / sigma(v)
         * と置くと
         *
         * h(v) = 1/sigma(v) + Σ_{v->w} h(w)
         *
         * となる。
         */
        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                a[i][j] = dist[i][j] == Integer.MAX_VALUE
                        ? 0
                        : 1 / sigma[i][j];
            }
        }

        double[][] h = aggregate(
                blockedRight, blockedDown, isObstacle, dist, a, Double::sum, 1);

        for (int i = 0; i < R; i++) {
            for (int j = 0; j < C; j++) {
                if (dist[i][j] != Integer.MAX_VALUE) {
                    res[i][j] = sigma[i][j] * h[i][j] - 1;
                }
            }
        }

        // betweenness では始点自身は中間点として数えない。
        res[si][sj] = 0;
        return res;
    }

    /**
     * ランダムな N x N の障害物盤面を生成する。
     *
     * 初期状態としてすべてのマスを空きマスとする。
     * 指定された固定空きマス (fixedEmptyCells、null の場合は (0, 0) および (N - 1, N - 1)) を除くすべてのマスをランダム順に走査し、
     * 4近傍の障害物数が 3 未満であり、かつ障害物に変更しても空きマス全体が連結に保たれる場合に障害物へと変更する。
     * その後、すべての障害物マスをランダム順に走査し、4近傍に空きマスが存在する障害物マスを最大 N 回空きマスに戻す。
     *
     * @param N 盤面の行数・列数 (N >= 1)
     * @param fixedEmptyCells 障害物に変更しない空きマス確定の座標一覧（null の場合は (0,0) と (N-1,N-1)）
     * @param rnd 乱数生成器
     * @return 障害物位置を表す N x N の boolean 配列（true が障害物、false が空きマス）
     * @complexity O(N^2 \alpha(N^2))
     */
    // 未テスト
    public static boolean[][] generateRandomGrid(int N, int[][] fixedEmptyCells, java.util.Random rnd) {
        if (N <= 0) return new boolean[0][0];
        boolean[][] isObstacle = new boolean[N][N];
        if (N <= 1) return isObstacle;

        boolean[][] isFixedEmpty = new boolean[N][N];
        if (fixedEmptyCells != null) {
            for (int[] cell : fixedEmptyCells) {
                if (cell != null && cell.length == 2) {
                    int r = cell[0];
                    int c = cell[1];
                    if (r >= 0 && r < N && c >= 0 && c < N) {
                        isFixedEmpty[r][c] = true;
                    }
                }
            }
        }

        int oobIndex = N * N;
        UnionFind uf = new UnionFind(N * N + 1);

        // Phase 1: マス目の走査リスト構築（固定空きマスを除く）
        java.util.ArrayList<int[]> candList = new java.util.ArrayList<>();
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (!isFixedEmpty[r][c]) {
                    candList.add(new int[]{r, c});
                }
            }
        }

        int totalCandidates = candList.size();
        int[] rawR = new int[totalCandidates];
        int[] rawC = new int[totalCandidates];
        for (int i = 0; i < totalCandidates; i++) {
            rawR[i] = candList.get(i)[0];
            rawC[i] = candList.get(i)[1];
        }

        int[] perm1 = ArrayUtils.randomPermutation(totalCandidates, rnd);
        int[] candR = ArrayUtils.take(rawR, perm1);
        int[] candC = ArrayUtils.take(rawC, perm1);

        // 8方向（時計回り順）
        int[] dr8 = {-1, -1, 0, 1, 1, 1, 0, -1};
        int[] dc8 = {0, 1, 1, 1, 0, -1, -1, -1};

        for (int idx = 0; idx < totalCandidates; idx++) {
            int r = candR[idx];
            int c = candC[idx];

            // 1. 4近傍の障害物数をカウント（範囲外は障害物扱い）
            int obs4 = 0;
            for (int dir = 0; dir < 4; dir++) {
                int nr = r + dh[dir];
                int nc = c + dw[dir];
                if (nr < 0 || nr >= N || nc < 0 || nc >= N || isObstacle[nr][nc]) {
                    obs4++;
                }
            }
            if (obs4 >= 3) continue;

            // 2. 8近傍の状態をチェックし、障害物連結成分グループを取得
            boolean[] isObs = new boolean[8];
            for (int k = 0; k < 8; k++) {
                int nr = r + dr8[k];
                int nc = c + dc8[k];
                if (nr < 0 || nr >= N || nc < 0 || nc >= N || isObstacle[nr][nc]) {
                    isObs[k] = true;
                }
            }

            int groupCount = 0;
            int[] groupStartK = new int[8];
            for (int k = 0; k < 8; k++) {
                int prevK = (k + 7) % 8;
                if (!isObs[prevK] && isObs[k]) {
                    groupStartK[groupCount++] = k;
                }
            }

            // 障害物グループが 2 つ以上ある場合、共通の UF ルートを持つか確認
            boolean canPlace = true;
            if (groupCount >= 2) {
                int[] roots = new int[groupCount];
                for (int g = 0; g < groupCount; g++) {
                    int k = groupStartK[g];
                    int nr = r + dr8[k];
                    int nc = c + dc8[k];
                    int neighborUFIdx = (nr < 0 || nr >= N || nc < 0 || nc >= N) ? oobIndex : nr * N + nc;
                    roots[g] = uf.root(neighborUFIdx);
                }
                outer:
                for (int g1 = 0; g1 < groupCount; g1++) {
                    for (int g2 = g1 + 1; g2 < groupCount; g2++) {
                        if (roots[g1] == roots[g2]) {
                            canPlace = false;
                            break outer;
                        }
                    }
                }
            }

            if (!canPlace) continue;

            // 障害物に変更し、8近傍の障害物と結合
            isObstacle[r][c] = true;
            int myUFIdx = r * N + c;
            for (int k = 0; k < 8; k++) {
                if (isObs[k]) {
                    int nr = r + dr8[k];
                    int nc = c + dc8[k];
                    int neighborUFIdx = (nr < 0 || nr >= N || nc < 0 || nc >= N) ? oobIndex : nr * N + nc;
                    uf.union(myUFIdx, neighborUFIdx);
                }
            }
        }

        // Phase 2: 障害物マスをランダム順に走査し、空きマスが4近傍にあるものを空きマスに戻す（最大 N 回）
        int obsCount = 0;
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (isObstacle[r][c]) obsCount++;
            }
        }

        if (obsCount > 0) {
            int[] rawObsR = new int[obsCount];
            int[] rawObsC = new int[obsCount];
            int oPtr = 0;
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    if (isObstacle[r][c]) {
                        rawObsR[oPtr] = r;
                        rawObsC[oPtr] = c;
                        oPtr++;
                    }
                }
            }

            int[] perm2 = ArrayUtils.randomPermutation(obsCount, rnd);
            int[] obsR = ArrayUtils.take(rawObsR, perm2);
            int[] obsC = ArrayUtils.take(rawObsC, perm2);

            int changedCount = 0;
            for (int i = 0; i < obsCount && changedCount < N; i++) {
                int r = obsR[i];
                int c = obsC[i];

                boolean hasEmpty4 = false;
                for (int dir = 0; dir < 4; dir++) {
                    int nr = r + dh[dir];
                    int nc = c + dw[dir];
                    if (nr >= 0 && nr < N && nc >= 0 && nc < N && !isObstacle[nr][nc]) {
                        hasEmpty4 = true;
                        break;
                    }
                }

                if (hasEmpty4) {
                    isObstacle[r][c] = false;
                    changedCount++;
                }
            }
        }

        return isObstacle;
    }

    /**
     * ランダムな N x N の障害物盤面を生成する。
     *
     * @param N 盤面の行数・列数 (N >= 1)
     * @param fixedEmptyCells 障害物に変更しない空きマス確定の座標一覧（null の場合は (0,0) と (N-1,N-1)）
     * @return 障害物位置を表す N x N の boolean 配列（true が障害物、false が空きマス）
     * @complexity O(N^2 \alpha(N^2))
     */
    // 未テスト
    public static boolean[][] generateRandomGrid(int N, int[][] fixedEmptyCells) {
        return generateRandomGrid(N, fixedEmptyCells, new java.util.Random());
    }

    /**
     * ランダムな N x N の障害物盤面を生成する。
     *
     * @param N 盤面の行数・列数 (N >= 1)
     * @param rnd 乱数生成器
     * @return 障害物位置を表す N x N の boolean 配列（true が障害物、false が空きマス）
     * @complexity O(N^2 \alpha(N^2))
     */
    // 未テスト
    public static boolean[][] generateRandomGrid(int N, java.util.Random rnd) {
        return generateRandomGrid(N, new int[][]{{0, 0}, {N - 1, N - 1}}, rnd);
    }

    /**
     * ランダムな N x N の障害物盤面を生成する。
     *
     * @param N 盤面の行数・列数 (N >= 1)
     * @return 障害物位置を表す N x N の boolean 配列（true が障害物、false が空きマス）
     * @complexity O(N^2 \alpha(N^2))
     */
    // 未テスト
    public static boolean[][] generateRandomGrid(int N) {
        return generateRandomGrid(N, new int[][]{{0, 0}, {N - 1, N - 1}}, new java.util.Random());
    }
    
    
    

    /**
     * 方向（0: 下, 1: 右, 2: 上, 3: 左）の差分から最小の90度方向転換回数を計算する。
     *
     * @param d1 変更前の方向（-1 の場合は初期方向指定なしとし 0 回）
     * @param d2 変更後の方向
     * @return 90度方向転換回数（0, 1, 2 のいずれか）
     */
    private static int minTurns(int d1, int d2) {
        if (d1 < 0) return 0;
        int diff = Math.abs(d1 - d2);
        if (diff == 0) return 0;
        if (diff == 1 || diff == 3) return 1;
        return 2;
    }

    /**
     * 始点 (si, sj) から各マスへの方向転換（1回あたりのコスト1）および1マス移動（コスト1）を考慮した最短距離をBFSにより計算する。
     * 各マスにおいて、直前の移動方向（0: 下, 1: 右, 2: 上, 3: 左）ごとに最小コストを求める。
     *
     * 状態空間は (dir, r, c) であり、方向転換（90度ごとにコスト1）および移動（1マスごとにコスト1）のすべての遷移コストは 1 である。
     * initialDir が -1 の場合は任意の初期方向を選択可能であり、初期状態のコストは 0 とする。
     * 0 <= initialDir < 4 の場合は指定された初期方向のコストを 0 とする。
     * 到達不能なマスの状態に対する距離は Integer.MAX_VALUE / 3 となる。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight R x (C - 1) 配列。null の場合は垂直壁なし
     * @param blockedDown (R - 1) x C 配列。null の場合は水平壁なし
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param initialDir 初期進行方向（0: 下, 1: 右, 2: 上, 3: 左。-1 の場合は自由選択）
     * @return dist[dir][r][c]（方向 dir を向いてマス (r, c) に到達するための最小コスト）
     * @throws IllegalArgumentException blockedRight が R x (C - 1) 配列でない場合、または blockedDown が (R - 1) x C 配列でない場合（null は許容）
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][][] bfsWithTurnCost(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj, int initialDir) {
        if (blockedRight != null) {
            if (blockedRight.length != R) {
                throw new IllegalArgumentException("blockedRight must have R rows");
            }
            for (int r = 0; r < R; r++) {
                if (blockedRight[r] == null || blockedRight[r].length != C - 1) {
                    throw new IllegalArgumentException("each row of blockedRight must have C - 1 columns");
                }
            }
        }
        if (blockedDown != null) {
            if (blockedDown.length != R - 1) {
                throw new IllegalArgumentException("blockedDown must have R - 1 rows");
            }
            for (int r = 0; r < R - 1; r++) {
                if (blockedDown[r] == null || blockedDown[r].length != C) {
                    throw new IllegalArgumentException("each row of blockedDown must have C columns");
                }
            }
        }
        int[][][] dist = new int[4][R][C];
        int INF = Integer.MAX_VALUE / 3;
        ArrayUtils.fill(dist, INF);
        if (si < 0 || si >= R || sj < 0 || sj >= C) {
            return dist;
        }
        if (isObstacle != null && (si >= isObstacle.length || isObstacle[si] == null || sj >= isObstacle[si].length || isObstacle[si][sj])) {
            return dist;
        }

        ArrayDeque<int[]> dq = new ArrayDeque<>();
        if (initialDir >= 0 && initialDir < 4) {
            dist[initialDir][si][sj] = 0;
            dq.add(new int[]{initialDir, si, sj});
        } else {
            for (int d = 0; d < 4; d++) {
                dist[d][si][sj] = 0;
                dq.add(new int[]{d, si, sj});
            }
        }

        while (!dq.isEmpty()) {
            int[] curr = dq.pollFirst();
            int dir = curr[0];
            int r = curr[1];
            int c = curr[2];
            int d = dist[dir][r][c];

            // 1. その場で 90 度方向転換（コスト 1）
            for (int turn = -1; turn <= 1; turn += 2) {
                int ndir = (dir + turn + 4) % 4;
                if (d + 1 < dist[ndir][r][c]) {
                    dist[ndir][r][c] = d + 1;
                    dq.addLast(new int[]{ndir, r, c});
                }
            }

            // 2. 現在の向き dir で 1 マス前進（コスト 1）
            if (canMove(r, c, dir, R, C, blockedRight, blockedDown, isObstacle)) {
                int nr = r + dh[dir];
                int nc = c + dw[dir];
                if (d + 1 < dist[dir][nr][nc]) {
                    dist[dir][nr][nc] = d + 1;
                    dq.addLast(new int[]{dir, nr, nc});
                }
            }
        }
        return dist;
    }

    /**
     * 始点 (si, sj) から終点 (ti, tj) への方向転換コストを考慮した最短パス（状態 [dir, r, c] の列）を取得する。
     * 各要素は長さ 3 の配列 [dir, r, c] であり、状態グラフ上での遷移（移動および向き変更）が順に記録される。
     * 壁（blockedRight, blockedDown）およびセル障害物（isObstacle）による通行不能、ならびに方向転換コストを考慮する。
     * 到達不可能な場合は null を返す。
     *
     * @param R 行数
     * @param C 列数
     * @param blockedRight 垂直壁情報（null の場合は壁なし）
     * @param blockedDown 水平壁情報（null の場合は壁なし）
     * @param isObstacle セル障害物情報（null の場合は障害物なし）
     * @param si 始点の行インデックス
     * @param sj 始点の列インデックス
     * @param ti 終点の行インデックス
     * @param tj 終点の列インデックス
     * @param initialDir 初期進行方向（0: 下, 1: 右, 2: 上, 3: 左。-1 の場合は自由選択）
     * @param rnd 同点時の分岐選択用乱数生成器（null の場合は先頭を選択）
     * @return パスを構成する各状態 [dir, r, c] の配列。到達不能時は null
     * @complexity O(R · C)
     */
    // 未テスト
    public static int[][] findPathWithTurnCost(int R, int C, boolean[][] blockedRight, boolean[][] blockedDown, boolean[][] isObstacle,
            int si, int sj, int ti, int tj, int initialDir, java.util.Random rnd) {
        int[][][] dist = bfsWithTurnCost(R, C, blockedRight, blockedDown, isObstacle, si, sj, initialDir);
        if (ti < 0 || ti >= R || tj < 0 || tj >= C) {
            return null;
        }
        int INF = Integer.MAX_VALUE / 3;
        int bestCost = INF;
        java.util.ArrayList<Integer> bestDirs = new java.util.ArrayList<>();
        for (int dir = 0; dir < 4; dir++) {
            if (dist[dir][ti][tj] < bestCost) {
                bestCost = dist[dir][ti][tj];
                bestDirs.clear();
                bestDirs.add(dir);
            } else if (dist[dir][ti][tj] == bestCost && bestCost < INF) {
                bestDirs.add(dir);
            }
        }
        if (bestDirs.isEmpty() || bestCost >= INF) {
            return null;
        }

        int targetDir;
        if (bestDirs.size() > 1 && rnd != null) {
            targetDir = bestDirs.get(rnd.nextInt(bestDirs.size()));
        } else {
            targetDir = bestDirs.get(0);
        }

        java.util.ArrayList<int[]> reversedPath = new java.util.ArrayList<>();
        int currR = ti;
        int currC = tj;
        int currDir = targetDir;
        reversedPath.add(new int[]{currDir, currR, currC});

        while (true) {
            if (currR == si && currC == sj) {
                boolean isInitialState = false;
                if (initialDir >= 0 && initialDir < 4) {
                    if (currDir == initialDir && dist[currDir][currR][currC] == 0) {
                        isInitialState = true;
                    }
                } else {
                    if (dist[currDir][currR][currC] == 0) {
                        isInitialState = true;
                    }
                }
                if (isInitialState) {
                    break;
                }
            }

            java.util.ArrayList<int[]> candidates = new java.util.ArrayList<>();

            // 1. 移動による手前の状態 (currDir, prevR, prevC) から前進してきたか？
            int prevR = currR - dh[currDir];
            int prevC = currC - dw[currDir];
            if (prevR >= 0 && prevR < R && prevC >= 0 && prevC < C) {
                if (canMove(prevR, prevC, currDir, R, C, blockedRight, blockedDown, isObstacle)) {
                    if (dist[currDir][prevR][prevC] < INF && dist[currDir][prevR][prevC] + 1 == dist[currDir][currR][currC]) {
                        candidates.add(new int[]{currDir, prevR, prevC});
                    }
                }
            }

            // 2. その場での 90 度方向転換前の状態 (prevDir, currR, currC) から転換してきたか？
            for (int turn = -1; turn <= 1; turn += 2) {
                int prevDir = (currDir + turn + 4) % 4;
                if (dist[prevDir][currR][currC] < INF && dist[prevDir][currR][currC] + 1 == dist[currDir][currR][currC]) {
                    candidates.add(new int[]{prevDir, currR, currC});
                }
            }

            if (candidates.isEmpty()) {
                break;
            }

            int[] chosen;
            if (candidates.size() > 1 && rnd != null) {
                chosen = candidates.get(rnd.nextInt(candidates.size()));
            } else {
                chosen = candidates.get(0);
            }

            currDir = chosen[0];
            currR = chosen[1];
            currC = chosen[2];
            reversedPath.add(new int[]{currDir, currR, currC});
        }

        int len = reversedPath.size();
        int[][] res = new int[len][3];
        for (int i = 0; i < len; i++) {
            res[i] = reversedPath.get(len - 1 - i);
        }
        return res;
    }
}
