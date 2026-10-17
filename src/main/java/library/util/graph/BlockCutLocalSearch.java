package library.util.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import library.util.collections.IntArrayList;

/**
 * 二重頂点連結成分（ブロック）分解ライブラリ BlockCutForest とローカルサーチに基づくパス探索アルゴリズム。
 * グラフを BlockCutForest によりブロック木に分解し、各ブロック上でバックトラックに基づくローカルサーチを実行することで
 * 長い単純パスを構築する。
 */
public class BlockCutLocalSearch {

    /**
     * 二重頂点連結成分（ブロック）の頂点集合およびローカル隣接構造。
     */
    public static class Block {
        public int[] vertices;
        public IntArrayList[] adj;
        public int edgeCount = 0;

        // 未テスト
        /**
         * 元グラフの頂点 v のブロック内におけるローカルインデックスを返す。
         * O(log |V_b|)
         *
         * @param v 元グラフの頂点ID
         * @return ブロック内のローカルインデックス。存在しない場合は負の値
         */
        public int indexOf(int v) {
            return Arrays.binarySearch(vertices, v);
        }

        // 未テスト
        /**
         * ブロック内のすべての頂点の次数が 2 以下であるか（単純ブロックであるか）を判定する。
         * O(|V_b|)
         *
         * @return 単純ブロックである場合 true、そうでない場合 false
         */
        public boolean isSimple() {
            for (IntArrayList a : adj) {
                if (a.size() > 2) {
                    return false;
                }
            }
            return true;
        }

        // 未テスト
        /**
         * ブロックの探索優先度評価用の重みを計算する。
         * 分岐頂点（次数 3 以上）の個数 b_v に対して |V_b| * (b_v + 1) を返す。分岐頂点が存在しない場合は 0.0 を返す。
         * O(|V_b|)
         *
         * @return ブロックの重み
         */
        public double weight() {
            int branchVertices = 0;
            for (IntArrayList a : adj) {
                if (a.size() >= 3) {
                    ++branchVertices;
                }
            }
            if (branchVertices == 0) {
                return 0.0;
            }
            return (double) vertices.length * (branchVertices + 1);
        }
    }

    /**
     * ブロック内ローカルサーチの探索結果。
     */
    public static class LocalResult {
        public int score = -1;
        public IntArrayList path = new IntArrayList();
    }

    /**
     * 単一ブロック上でのバックトラックおよび二部グラフ走査上限枝刈りに基づくローカルサーチ。
     */
    public static class LocalSearch {
        private final Block block;
        private final IntArrayList[] adj;
        private final int n;
        private final int start;
        private final int finish;
        private final int required;
        private final int[] tail;
        private final int[] color;
        private final long deadlineNanos;
        private final Random rng;

        private final boolean[] used;
        private IntArrayList current;
        private final LocalResult best;

        private final int[] mark;
        private final int[] que;
        private int stamp = 0;

        private int nodes = 0;
        private int nodeLimit = 0;
        private boolean interrupted = false;

        // 未テスト
        /**
         * 単一ブロックに対するローカルサーチオブジェクトを構築する。
         * O(N)
         *
         * @param b 対象ブロック
         * @param startVertex 始点（ブロック内ローカルインデックス）
         * @param fixedFinish 終点（固定しない場合は -1）
         * @param requiredVertex 必須通過頂点（指定しない場合は -1）
         * @param terminalReward 頂点ごとの到達報酬配列
         * @param bipartiteColor 二部グラフ彩色配列
         * @param deadlineNanos 探索の制限時刻（ナノ秒）
         * @param random 乱数生成器
         */
        public LocalSearch(
            Block b,
            int startVertex,
            int fixedFinish,
            int requiredVertex,
            int[] terminalReward,
            int[] bipartiteColor,
            long deadlineNanos,
            Random random
        ) {
            this.block = b;
            this.adj = b.adj;
            this.n = b.vertices.length;
            this.start = startVertex;
            this.finish = fixedFinish;
            this.required = requiredVertex;
            this.tail = terminalReward;
            this.color = bipartiteColor;
            this.deadlineNanos = deadlineNanos;
            this.rng = random != null ? random : new Random();
            this.used = new boolean[n];
            this.mark = new int[n];
            this.que = new int[n];
            this.current = new IntArrayList();
            this.best = new LocalResult();
        }

        private void consider(boolean hitRequired) {
            int u = current.get(current.size() - 1);

            if (finish != -1 && u != finish) {
                return;
            }

            if (!hitRequired) {
                return;
            }

            int score = current.size() - 1;

            if (finish == -1) {
                score += tail[u];
            }

            if (score > best.score) {
                best.score = score;
                best.path = current.copy();
            }
        }

        private void bfs(
            int source,
            boolean[] forbidden,
            int[] distance,
            int[] parent
        ) {
            Arrays.fill(distance, -1);
            Arrays.fill(parent, -1);

            int head = 0;
            int end = 0;

            que[end++] = source;
            distance[source] = 0;

            while (head < end) {
                int u = que[head++];

                IntArrayList neighbors = adj[u];
                for (int i = 0; i < neighbors.size(); i++) {
                    int v = neighbors.get(i);
                    if (forbidden[v] || distance[v] != -1) {
                        continue;
                    }

                    distance[v] = distance[u] + 1;
                    parent[v] = u;
                    que[end++] = v;
                }
            }
        }

        private IntArrayList restorePath(int source, int target, int[] parent) {
            IntArrayList path = new IntArrayList();
            for (int v = target; v != source; v = parent[v]) {
                path.add(v);
            }
            path.add(source);
            path.reverse();
            return path;
        }

        private void makeInitialSolution() {
            boolean[] forbidden = new boolean[n];
            int[] distance = new int[n];
            int[] parent = new int[n];

            bfs(start, forbidden, distance, parent);

            int target = start;

            if (finish != -1) {
                target = finish;
            } else if (required != -1) {
                target = required;
            } else {
                for (int v = 0; v < n; ++v) {
                    if (distance[v] != -1
                        && distance[v] + tail[v] > distance[target] + tail[target]) {
                        target = v;
                    }
                }
            }

            current = restorePath(start, target, parent);

            if (finish == -1 && required != -1) {
                for (int i = 0; i < current.size(); i++) {
                    forbidden[current.get(i)] = true;
                }

                forbidden[target] = false;
                bfs(target, forbidden, distance, parent);

                int endpoint = target;

                for (int v = 0; v < n; ++v) {
                    if (distance[v] != -1
                        && distance[v] + tail[v] > distance[endpoint] + tail[endpoint]) {
                        endpoint = v;
                    }
                }

                IntArrayList extension = restorePath(target, endpoint, parent);

                for (int i = 1; i < extension.size(); ++i) {
                    current.add(extension.get(i));
                }
            }

            consider(true);
        }

        private void solveSimpleBlock() {
            current.clear();
            current.add(start);
            consider(required == -1 || required == start);

            IntArrayList startNeighbors = adj[start];
            for (int i = 0; i < startNeighbors.size(); i++) {
                int first = startNeighbors.get(i);
                current.clear();
                current.add(start);

                int previous = start;
                int u = first;
                boolean hit = required == -1 || required == start;

                while (u != start) {
                    current.add(u);
                    hit = hit || u == required;

                    consider(hit);

                    if (u == finish) {
                        break;
                    }

                    int next = -1;

                    IntArrayList uNeighbors = adj[u];
                    for (int j = 0; j < uNeighbors.size(); j++) {
                        int v = uNeighbors.get(j);
                        if (v != previous) {
                            next = v;
                            break;
                        }
                    }

                    if (next == -1) {
                        break;
                    }

                    previous = u;
                    u = next;
                }
            }
        }

        private boolean canImprove(int u, boolean hitRequired) {
            ++stamp;

            int head = 0;
            int end = 0;

            que[end++] = u;
            mark[u] = stamp;

            int[] count = new int[2];
            int[] maximumTail = new int[]{-1000000, -1000000};

            while (head < end) {
                int x = que[head++];
                int c = color[x];

                ++count[c];

                if (finish == -1) {
                    maximumTail[c] = Math.max(maximumTail[c], tail[x]);
                } else if (x == finish) {
                    maximumTail[c] = 0;
                }

                IntArrayList xNeighbors = adj[x];
                for (int i = 0; i < xNeighbors.size(); i++) {
                    int v = xNeighbors.get(i);
                    if (mark[v] == stamp) {
                        continue;
                    }

                    if (used[v] && v != u) {
                        continue;
                    }

                    mark[v] = stamp;
                    que[end++] = v;
                }
            }

            if (finish != -1 && mark[finish] != stamp) {
                return false;
            }

            if (!hitRequired && required != -1 && mark[required] != stamp) {
                return false;
            }

            int c = color[u];
            int opposite = c ^ 1;

            int sameColorVertices = 2 * Math.min(count[c] - 1, count[opposite]) + 1;
            int oppositeColorVertices = 2 * Math.min(count[c], count[opposite]);

            int base = current.size() - 2;
            int upper = -1000000;

            if (maximumTail[c] >= 0) {
                upper = Math.max(upper, base + sameColorVertices + maximumTail[c]);
            }

            if (oppositeColorVertices >= 2 && maximumTail[opposite] >= 0) {
                upper = Math.max(upper, base + oppositeColorVertices + maximumTail[opposite]);
            }

            return upper > best.score;
        }

        private void dfs(int u, boolean hitRequired) {
            if (interrupted) {
                return;
            }

            ++nodes;

            if (nodes > nodeLimit) {
                interrupted = true;
                return;
            }

            if ((nodes & 63) == 0 && System.nanoTime() >= deadlineNanos) {
                interrupted = true;
                return;
            }

            consider(hitRequired);

            if (u == finish) {
                return;
            }

            IntArrayList uNeighbors = adj[u];
            int deg = uNeighbors.size();
            long[] candidates = new long[deg];
            int candidateCount = 0;

            for (int i = 0; i < deg; i++) {
                int v = uNeighbors.get(i);
                if (used[v]) {
                    continue;
                }

                int onwardDegree = 0;
                IntArrayList vNeighbors = adj[v];
                for (int j = 0; j < vNeighbors.size(); j++) {
                    int w = vNeighbors.get(j);
                    if (!used[w]) {
                        ++onwardDegree;
                    }
                }

                int priority = onwardDegree * 16 + (rng.nextInt(16));

                if (onwardDegree == 0) {
                    priority += 96;
                }

                if (v == finish) {
                    priority += 96;
                }

                candidates[candidateCount++] = (((long) priority) << 32) | (v & 0xFFFFFFFFL);
            }

            if (candidateCount == 0) {
                return;
            }

            if (candidateCount >= 2
                || (current.size() & 7) == 0
                || current.size() == 1) {
                if (!canImprove(u, hitRequired)) {
                    return;
                }
            }

            Arrays.sort(candidates, 0, candidateCount);

            for (int i = 0; i < candidateCount; ++i) {
                int v = (int) candidates[i];

                used[v] = true;
                current.add(v);

                dfs(v, hitRequired || v == required);

                current.pollLast();
                used[v] = false;

                if (interrupted) {
                    return;
                }
            }
        }

        // 未テスト
        /**
         * ブロック上の探索を実行し、見つかった最長経路の探索結果を返す。
         * O(制限時間内に探索されたノード数)
         *
         * @return 探索結果
         */
        public LocalResult solve() {
            makeInitialSolution();

            if (block.isSimple()) {
                solveSimpleBlock();
                return best;
            }

            for (int restart = 0; System.nanoTime() < deadlineNanos; ++restart) {
                Arrays.fill(used, false);

                current.clear();
                current.add(start);
                used[start] = true;

                nodes = 0;
                interrupted = false;

                int level = Math.min(restart / 3, 6);
                nodeLimit = 2048 << level;

                dfs(start, required == -1 || required == start);

                if (!interrupted) {
                    break;
                }
            }

            return best;
        }
    }

    /**
     * 各ブロックに対する局所探索ジョブ情報。
     */
    public static class Job {
        public int blockId;
        public int type; // 0: 通常DP, 1: 終点固定, 2: Tを通る最終成分
        public int endpoint = -1;
        public double weight = 0.0;

        // 未テスト
        /**
         * ジョブ情報オブジェクトを構築する。
         * O(1)
         *
         * @param blockId ブロック ID
         * @param type ジョブ種別 (0: 通常DP, 1: 終点固定, 2: Tを通る最終成分)
         * @param endpoint 固定終点（ない場合は -1）
         * @param weight 時間配分用の重み
         */
        public Job(int blockId, int type, int endpoint, double weight) {
            this.blockId = blockId;
            this.type = type;
            this.endpoint = endpoint;
            this.weight = weight;
        }
    }

    // 未テスト
    /**
     * 無向グラフ g 上で始点 S から終点 T までの長経路を二重頂点連結成分分解ライブラリ BlockCutForest とローカルサーチにより探索する。
     * O(V + E + 時間予算)
     *
     * @param g 対象の無向グラフ
     * @param S 始点頂点 ID
     * @param T 終点頂点 ID
     * @param vertexColor 頂点ごとの二部グラフ彩色（0 または 1）
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @param rng 乱数生成器（null の場合はデフォルトの Random を生成）
     * @return 探索されたパスを構成する頂点 ID のリスト
     */
    public static IntArrayList solve(
        Graph g,
        int S,
        int T,
        int[] vertexColor,
        long timeBudgetMillis,
        Random rng
    ) {
        BlockCutForest bcf = new BlockCutForest(g);
        int V = g.N;
        int B = bcf.N - V;

        Block[] blocks = new Block[B];
        for (int b = 0; b < B; ++b) {
            IntArrayList vList = bcf.adj[V + b];
            Block block = new Block();
            block.vertices = vList.toArray();
            Arrays.sort(block.vertices);

            block.adj = new IntArrayList[block.vertices.length];
            for (int i = 0; i < block.vertices.length; ++i) {
                block.adj[i] = new IntArrayList();
            }

            int edgeCount = 0;
            for (int i = 0; i < block.vertices.length; ++i) {
                int u = block.vertices[i];
                IntArrayList uNeighbors = g.adj[u];
                for (int k = 0; k < uNeighbors.size(); ++k) {
                    int v = uNeighbors.get(k);
                    int j = block.indexOf(v);
                    if (j >= 0) {
                        block.adj[i].add(j);
                        if (i < j) {
                            edgeCount++;
                        }
                    }
                }
            }
            block.edgeCount = edgeCount;
            blocks[b] = block;
        }

        int[] parent = new int[V + B];
        Arrays.fill(parent, -2);
        IntArrayList order = new IntArrayList();

        parent[S] = -1;
        order.add(S);

        for (int i = 0; i < order.size(); ++i) {
            int u = order.get(i);
            IntArrayList neighbors = bcf.adj[u];
            for (int j = 0; j < neighbors.size(); ++j) {
                int v = neighbors.get(j);
                if (parent[v] != -2) {
                    continue;
                }
                parent[v] = u;
                order.add(v);
            }
        }

        List<Job> jobs = new ArrayList<>();
        IntArrayList prefixBlocks = new IntArrayList();

        int targetBlock = -1;
        boolean[] relevant = new boolean[V + B];

        if (T == -1 || S == T) {
            Arrays.fill(relevant, true);
        } else {
            targetBlock = parent[T] - V;

            IntArrayList spine = new IntArrayList();
            for (int u = T; u != -1; u = parent[u]) {
                spine.add(u);
            }
            spine.reverse();

            for (int i = 1; i + 1 < spine.size(); i += 2) {
                int b = spine.get(i) - V;
                if (b == targetBlock) {
                    break;
                }
                jobs.add(new Job(b, 1, spine.get(i + 1), 0.0));
                prefixBlocks.add(b);
            }

            IntArrayList stack = new IntArrayList();
            stack.add(V + targetBlock);
            relevant[V + targetBlock] = true;

            while (stack.isNonEmpty()) {
                int u = stack.pollLast();
                IntArrayList neighbors = bcf.adj[u];
                for (int i = 0; i < neighbors.size(); ++i) {
                    int v = neighbors.get(i);
                    if (parent[v] == u) {
                        relevant[v] = true;
                        stack.add(v);
                    }
                }
            }
        }

        for (int i = order.size() - 1; i >= 0; --i) {
            int u = order.get(i);
            if (u < V || !relevant[u]) {
                continue;
            }
            int b = u - V;
            if (b == targetBlock) {
                continue;
            }
            jobs.add(new Job(b, 0, -1, 0.0));
        }

        if (targetBlock != -1) {
            jobs.add(new Job(targetBlock, 2, T, 0.0));
        }

        double remainingWeight = 0.0;
        for (Job job : jobs) {
            job.weight = blocks[job.blockId].weight();
            if (job.type == 2) {
                job.weight *= 2.0;
            }
            remainingWeight += job.weight;
        }

        int[] dp = new int[V];
        int[] bestChildBlock = new int[V];
        Arrays.fill(bestChildBlock, -1);
        IntArrayList[] chosenPath = new IntArrayList[B];

        if (rng == null) {
            rng = new Random();
        }

        long globalDeadlineNanos = System.nanoTime() + timeBudgetMillis * 1_000_000L;

        for (Job job : jobs) {
            int b = job.blockId;
            Block block = blocks[b];

            int entryVertex = parent[V + b];
            int start = block.indexOf(entryVertex);

            int finish = -1;
            int required = -1;

            if (job.type == 1) {
                finish = block.indexOf(job.endpoint);
            } else if (job.type == 2) {
                required = block.indexOf(T);
            }

            int[] tail = new int[block.vertices.length];
            int[] color = new int[block.vertices.length];

            for (int i = 0; i < block.vertices.length; ++i) {
                int v = block.vertices[i];
                color[i] = vertexColor[v];

                if (job.type != 1 && v != entryVertex) {
                    tail[i] = dp[v];
                }
            }

            long nowNanos = System.nanoTime();
            long localDeadlineNanos = nowNanos;

            if (job.weight > 0.0 && remainingWeight > 0.0 && nowNanos < globalDeadlineNanos) {
                double ratio = Math.min(1.0, job.weight / remainingWeight);
                double seconds = ((double) (globalDeadlineNanos - nowNanos) / 1e9) * ratio;
                localDeadlineNanos = nowNanos + (long) (seconds * 1e9);
            }

            LocalSearch search = new LocalSearch(
                block,
                start,
                finish,
                required,
                tail,
                color,
                localDeadlineNanos,
                rng
            );

            LocalResult result = search.solve();

            IntArrayList path = new IntArrayList();
            for (int i = 0; i < result.path.size(); i++) {
                int localVertex = result.path.get(i);
                path.add(block.vertices[localVertex]);
            }
            chosenPath[b] = path;

            if (job.type == 0 && result.score > dp[entryVertex]) {
                dp[entryVertex] = result.score;
                bestChildBlock[entryVertex] = b;
            }

            remainingWeight -= job.weight;
        }

        IntArrayList answer = new IntArrayList();
        answer.add(S);

        Consumer<Integer> appendBlockPath = (Integer b) -> {
            IntArrayList path = chosenPath[b];
            for (int i = 1; i < path.size(); ++i) {
                answer.add(path.get(i));
            }
        };

        if (T != -1 && S != T) {
            for (int i = 0; i < prefixBlocks.size(); ++i) {
                appendBlockPath.accept(prefixBlocks.get(i));
            }
            appendBlockPath.accept(targetBlock);
        }

        while (true) {
            int v = answer.get(answer.size() - 1);
            int b = bestChildBlock[v];
            if (b == -1) {
                break;
            }
            appendBlockPath.accept(b);
        }

        return answer;
    }

    // 未テスト
    /**
     * 無向グラフ g 上で始点 S から任意の終点までの長経路を二重頂点連結成分分解ライブラリ BlockCutForest とローカルサーチにより探索する。
     * O(V + E + 時間予算)
     *
     * @param g 対象の無向グラフ
     * @param S 始点頂点 ID
     * @param vertexColor 頂点ごとの二部グラフ彩色（0 または 1）
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @param rng 乱数生成器（null の場合はデフォルトの Random を生成）
     * @return 探索されたパスを構成する頂点 ID のリスト
     */
    public static IntArrayList solve(
        Graph g,
        int S,
        int[] vertexColor,
        long timeBudgetMillis,
        Random rng
    ) {
        return solve(g, S, -1, vertexColor, timeBudgetMillis, rng);
    }

    // 未テスト
    /**
     * 無向グラフ g 上で始点 S から任意の終点までの長経路を二重頂点連結成分分解ライブラリ BlockCutForest とローカルサーチにより探索する。
     * O(V + E + 時間予算)
     *
     * @param g 対象の無向グラフ
     * @param S 始点頂点 ID
     * @param vertexColor 頂点ごとの二部グラフ彩色（0 または 1）
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @return 探索されたパスを構成する頂点 ID のリスト
     */
    public static IntArrayList solve(
        Graph g,
        int S,
        int[] vertexColor,
        long timeBudgetMillis
    ) {
        return solve(g, S, -1, vertexColor, timeBudgetMillis, null);
    }

    // 未テスト
    /**
     * H x W の 2次元グリッドマップ上で (si, sj) から (ti, tj) までの長経路を探索する。
     * isObstacle[r][c] が true のセルは通行不能障害物を表す。
     * O(H * W + 時間予算)
     *
     * @param H グリッドの行数
     * @param W グリッドの列数
     * @param isObstacle 障害物セルフラグ（null の場合は障害物なし）
     * @param si 始点行
     * @param sj 始点列
     * @param ti 終点行
     * @param tj 終点列
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @param rng 乱数生成器
     * @return 探索されたセル座標 (r, c) のリスト
     */
    public static List<int[]> solveGrid(
        int H,
        int W,
        boolean[][] isObstacle,
        int si,
        int sj,
        int ti,
        int tj,
        long timeBudgetMillis,
        Random rng
    ) {
        int[][] id = new int[H][W];
        List<int[]> coordinate = new ArrayList<>();

        for (int r = 0; r < H; ++r) {
            for (int c = 0; c < W; ++c) {
                if (isObstacle == null || !isObstacle[r][c]) {
                    id[r][c] = coordinate.size();
                    coordinate.add(new int[]{r, c});
                } else {
                    id[r][c] = -1;
                }
            }
        }

        int V = coordinate.size();
        int S = id[si][sj];
        int T = id[ti][tj];

        Graph graph = new Graph(V);
        int[] vertexColor = new int[V];

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int u = 0; u < V; ++u) {
            int[] coord = coordinate.get(u);
            int r = coord[0];
            int c = coord[1];
            vertexColor[u] = (r + c) & 1;

            for (int d = 0; d < 4; ++d) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (0 <= nr && nr < H && 0 <= nc && nc < W && id[nr][nc] != -1) {
                    graph.addEdge(u, id[nr][nc]);
                }
            }
        }

        IntArrayList pathIds = solve(graph, S, T, vertexColor, timeBudgetMillis, rng);
        List<int[]> resultCoords = new ArrayList<>(pathIds.size());
        for (int i = 0; i < pathIds.size(); i++) {
            resultCoords.add(coordinate.get(pathIds.get(i)));
        }
        return resultCoords;
    }

    // 未テスト
    /**
     * H x W の 2次元グリッドマップ上で (si, sj) から (ti, tj) までの長経路を探索する。
     * O(H * W + 時間予算)
     *
     * @param H グリッドの行数
     * @param W グリッドの列数
     * @param isObstacle 障害物セルフラグ（null の場合は障害物なし）
     * @param si 始点行
     * @param sj 始点列
     * @param ti 終点行
     * @param tj 終点列
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @return 探索されたセル座標 (r, c) のリスト
     */
    public static List<int[]> solveGrid(
        int H,
        int W,
        boolean[][] isObstacle,
        int si,
        int sj,
        int ti,
        int tj,
        long timeBudgetMillis
    ) {
        return solveGrid(H, W, isObstacle, si, sj, ti, tj, timeBudgetMillis, null);
    }

    // 未テスト
    /**
     * H x W の 2次元グリッドマップ上で (si, sj) から任意の終点までの長経路を探索する。
     * isObstacle[r][c] が true のセルは通行不能障害物を表す。
     * O(H * W + 時間予算)
     *
     * @param H グリッドの行数
     * @param W グリッドの列数
     * @param isObstacle 障害物セルフラグ（null の場合は障害物なし）
     * @param si 始点行
     * @param sj 始点列
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @param rng 乱数生成器
     * @return 探索されたセル座標 (r, c) のリスト
     */
    public static List<int[]> solveGrid(
        int H,
        int W,
        boolean[][] isObstacle,
        int si,
        int sj,
        long timeBudgetMillis,
        Random rng
    ) {
        int[][] id = new int[H][W];
        List<int[]> coordinate = new ArrayList<>();

        for (int r = 0; r < H; ++r) {
            for (int c = 0; c < W; ++c) {
                if (isObstacle == null || !isObstacle[r][c]) {
                    id[r][c] = coordinate.size();
                    coordinate.add(new int[]{r, c});
                } else {
                    id[r][c] = -1;
                }
            }
        }

        int V = coordinate.size();
        int S = id[si][sj];

        Graph graph = new Graph(V);
        int[] vertexColor = new int[V];

        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        for (int u = 0; u < V; ++u) {
            int[] coord = coordinate.get(u);
            int r = coord[0];
            int c = coord[1];
            vertexColor[u] = (r + c) & 1;

            for (int d = 0; d < 4; ++d) {
                int nr = r + dr[d];
                int nc = c + dc[d];

                if (0 <= nr && nr < H && 0 <= nc && nc < W && id[nr][nc] != -1) {
                    graph.addEdge(u, id[nr][nc]);
                }
            }
        }

        IntArrayList pathIds = solve(graph, S, vertexColor, timeBudgetMillis, rng);
        List<int[]> resultCoords = new ArrayList<>(pathIds.size());
        for (int i = 0; i < pathIds.size(); i++) {
            resultCoords.add(coordinate.get(pathIds.get(i)));
        }
        return resultCoords;
    }

    // 未テスト
    /**
     * H x W の 2次元グリッドマップ上で (si, sj) から任意の終点までの長経路を探索する。
     * O(H * W + 時間予算)
     *
     * @param H グリッドの行数
     * @param W グリッドの列数
     * @param isObstacle 障害物セルフラグ（null の場合は障害物なし）
     * @param si 始点行
     * @param sj 始点列
     * @param timeBudgetMillis 時間予算（ミリ秒）
     * @return 探索されたセル座標 (r, c) のリスト
     */
    public static List<int[]> solveGrid(
        int H,
        int W,
        boolean[][] isObstacle,
        int si,
        int sj,
        long timeBudgetMillis
    ) {
        return solveGrid(H, W, isObstacle, si, sj, timeBudgetMillis, null);
    }
}
