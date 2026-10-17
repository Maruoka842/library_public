package library.util.linalg;

import java.math.BigInteger;
import java.util.*;

public final class BinaryMatrixMonoidAnalyzer {
    private final int n;
    private final Matrix[] generators;

    public BinaryMatrixMonoidAnalyzer(int n, Matrix... generators) {
        if (n < 0 || n > 63)
            throw new IllegalArgumentException("0 <= n <= 63 required");
        this.n = n;
        this.generators = generators.clone();
        for (Matrix a : generators)
            if (a.n != n)
                throw new IllegalArgumentException("matrix size mismatch");
    }

    /**
     * I から U -> U A_i を繰り返して到達する row space の作用を構築する。
     *
     * 各状態は M_n(F_2) における ambient L-class (= row space) に対応する。
     * SCC ごとに、その row space 上に誘導される stabilizer group を
     * Schreier generators から厳密に計算できる。
     */
    public RowSpaceAction analyzeRowSpaces() {
        Subspace start = Subspace.full(n);

        ArrayList<Subspace> states = new ArrayList<>();
        ArrayList<Integer> parent = new ArrayList<>();
        ArrayList<Integer> parentGenerator = new ArrayList<>();
        HashMap<Subspace, Integer> id = new HashMap<>();
        IntBuffer next = new IntBuffer();

        states.add(start);
        parent.add(-1);
        parentGenerator.add(-1);
        id.put(start, 0);

        // BFS order = state id order.
        for (int u = 0; u < states.size(); u++) {
            Subspace s = states.get(u);
            for (int gi = 0; gi < generators.length; gi++) {
                Subspace t = s.rightMultiply(generators[gi]);
                Integer old = id.get(t);
                int v;
                if (old == null) {
                    v = states.size();
                    id.put(t, v);
                    states.add(t);
                    parent.add(u);
                    parentGenerator.add(gi);
                } else {
                    v = old;
                }
                next.add(v);
            }
        }

        int m = states.size();
        int[] par = new int[m];
        int[] pgen = new int[m];
        for (int i = 0; i < m; i++) {
            par[i] = parent.get(i);
            pgen[i] = parentGenerator.get(i);
        }

        int[] edges = next.toArray();
        SccData scc = stronglyConnectedComponents(m, generators.length, edges);

        int[] countByRank = new int[n + 1];
        for (Subspace s : states)
            countByRank[s.rank()]++;

        return new RowSpaceAction(
                Collections.unmodifiableList(states),
                edges,
                par,
                pgen,
                countByRank,
                scc.componentOf,
                scc.componentCount,
                scc.componentStart,
                scc.componentMembers,
                scc.reverseStart,
                scc.reverseEdges);
    }

    /** column-space 版。A_i^T の row-space action として計算する。 */
    public RowSpaceAction analyzeColumnSpaces() {
        Matrix[] tr = new Matrix[generators.length];
        for (int i = 0; i < tr.length; i++)
            tr[i] = generators[i].transpose();
        return new BinaryMatrixMonoidAnalyzer(n, tr).analyzeRowSpaces();
    }

    /**
     * row vector {@code start} の右作用 orbit
     * {@code { start P | P in <generators> }} を厳密に列挙する。
     *
     * <p>状態は vector 自身だけであり、遷移は {@code v -> v A_i}。
     * 同じ vector に到達した後の将来は完全に同じなので、vector ごとに一度だけ探索する。
     *
     * <p>状態数は高々 {@code 2^n}。したがって巨大な n に対する最悪計算量は指数的だが、
     * orbit 自体が小さい場合は row-space / Green 構造を展開するより大幅に軽い。
     *
     * @param start 初期 row vector。bit i が第 i 成分。
     * @return start から到達可能な vector の orbit
     */
    public VectorOrbit analyzeVectorOrbit(long start) {
        long mask = n == 63 ? Long.MAX_VALUE : (n == 0 ? 0 : (1L << n) - 1);
        start &= mask;

        HashMap<Long, Integer> id = new HashMap<>();
        ArrayList<Long> states = new ArrayList<>();
        ArrayList<Integer> parent = new ArrayList<>();
        ArrayList<Integer> parentGenerator = new ArrayList<>();

        id.put(start, 0);
        states.add(start);
        parent.add(-1);
        parentGenerator.add(-1);

        // BFS order = state id order.
        for (int u = 0; u < states.size(); u++) {
            long v = states.get(u);
            for (int gi = 0; gi < generators.length; gi++) {
                long w = generators[gi].leftVectorMultiply(v);
                if (id.containsKey(w))
                    continue;
                int nextId = states.size();
                id.put(w, nextId);
                states.add(w);
                parent.add(u);
                parentGenerator.add(gi);
            }
        }

        int m = states.size();
        long[] vector = new long[m];
        int[] par = new int[m];
        int[] pgen = new int[m];
        for (int i = 0; i < m; i++) {
            vector[i] = states.get(i);
            par[i] = parent.get(i);
            pgen[i] = parentGenerator.get(i);
        }
        return new VectorOrbit(start, vector, id, par, pgen);
    }

    /**
     * row/column-space action の解析結果を、人間が眺めやすい形でまとめて返す。
     *
     * <p>各 rank について、到達した部分空間数、F_2^n に存在する部分空間総数
     * (Gaussian binomial coefficient)、欠損数、到達率を表示する。
     * さらに各 SCC の rank、状態数、stabilizer action group の位数も表示する。</p>
     */
    private String analysisReport() {
        RowSpaceAction row = analyzeRowSpaces();
        RowSpaceAction col = analyzeColumnSpaces();

        StringBuilder sb = new StringBuilder();
        sb.append("Binary matrix monoid analysis\n");
        sb.append("n = ").append(n)
                .append(", generators = ").append(generators.length).append('\n');
        sb.append('\n');

        appendActionReport(sb, "ROW SPACES", row);
        sb.append('\n');
        appendActionReport(sb, "COLUMN SPACES", col);
        return sb.toString();
    }

    /** {@link #analysisReport()} を標準出力へ表示する。 */
    public void printAnalysisReport() {
        System.out.print(analysisReport());
    }

    private void appendActionReport(StringBuilder sb, String title, RowSpaceAction action) {
        sb.append("=== ").append(title).append(" ===\n");
        sb.append("reachable spaces = ").append(action.size()).append('\n');
        sb.append("SCCs             = ").append(action.componentCount()).append('\n');
        sb.append('\n');
        sb.append(String.format(Locale.ROOT,
                "%4s %18s %18s %18s %11s%n",
                "rank", "reachable", "all", "missing", "coverage"));

        int[] cnt = action.countByRank();
        for (int r = 0; r <= n; r++) {
            BigInteger all = gaussianBinomial2(n, r);
            BigInteger reached = BigInteger.valueOf(cnt[r]);
            BigInteger missing = all.subtract(reached);
            double coverage = all.signum() == 0
                    ? 1.0
                    : reached.doubleValue() / all.doubleValue();

            sb.append(String.format(Locale.ROOT,
                    "%4d %18s %18s %18s %10.6f%%%n",
                    r, reached, all, missing, coverage * 100.0));
        }

        sb.append('\n');
        sb.append("SCC details:\n");
        sb.append(String.format(Locale.ROOT,
                "%5s %5s %12s %28s%n",
                "id", "rank", "spaces", "stabilizer order"));

        for (int c = 0; c < action.componentCount(); c++) {
            int[] members = action.componentStates(c);
            int rank = action.states().get(members[0]).rank();
            BigInteger order = action.stabilizerGroup(c).order();
            sb.append(String.format(Locale.ROOT,
                    "%5d %5d %12d %28s%n",
                    c, rank, members.length, order));
        }
    }

    /** F_2^n の r 次元部分空間の総数 [n choose r]_2。 */
    private static BigInteger gaussianBinomial2(int n, int r) {
        if (r < 0 || r > n)
            return BigInteger.ZERO;
        r = Math.min(r, n - r);
        BigInteger ret = BigInteger.ONE;
        for (int i = 0; i < r; i++) {
            BigInteger numerator = BigInteger.ONE.shiftLeft(n - i).subtract(BigInteger.ONE);
            BigInteger denominator = BigInteger.ONE.shiftLeft(r - i).subtract(BigInteger.ONE);
            ret = ret.multiply(numerator).divide(denominator);
        }
        return ret;
    }

    /**
     * {@link #analyzeVectorOrbit(long)} の解析結果。
     */
    public final class VectorOrbit {
        private static final int DEFAULT_MISSING_ENUMERATION_UNIVERSE_LIMIT = 1 << 20;

        private final long start;
        private final long[] states;
        private final Map<Long, Integer> id;
        private final int[] parent;
        private final int[] parentGenerator;

        private VectorOrbit(
                long start,
                long[] states,
                Map<Long, Integer> id,
                int[] parent,
                int[] parentGenerator) {
            this.start = start;
            this.states = states;
            this.id = Collections.unmodifiableMap(id);
            this.parent = parent;
            this.parentGenerator = parentGenerator;
        }

        public long start() {
            return start;
        }

        /** 到達可能な vector の個数。 */
        public int size() {
            return states.length;
        }

        /** 到達可能な vector を BFS 発見順で返す。 */
        public long[] states() {
            return states.clone();
        }

        /** {@code vector} が orbit に属するか。 */
        public boolean contains(long vector) {
            long mask = n == 63 ? Long.MAX_VALUE : (n == 0 ? 0 : (1L << n) - 1);
            return id.containsKey(vector & mask);
        }

        /** F_2^n 全体のうち orbit に含まれない vector の個数。 */
        public BigInteger missingCount() {
            return BigInteger.ONE.shiftLeft(n).subtract(BigInteger.valueOf(size()));
        }

        /**
         * {@code vector} へ到達する生成元 index 列を返す。
         * 到達不能なら {@code null}。
         */
        public int[] wordTo(long vector) {
            long mask = n == 63 ? Long.MAX_VALUE : (n == 0 ? 0 : (1L << n) - 1);
            Integer stateId = id.get(vector & mask);
            if (stateId == null)
                return null;

            int len = 0;
            for (int v = stateId; parent[v] != -1; v = parent[v])
                len++;

            int[] word = new int[len];
            int p = len;
            for (int v = stateId; parent[v] != -1; v = parent[v])
                word[--p] = parentGenerator[v];
            return word;
        }

        /**
         * orbit に含まれない vector を列挙する。
         * 全空間の列挙が {@code universeLimit} を超える場合は例外。
         */
        public long[] missingVectors(int universeLimit) {
            if (universeLimit < 0)
                throw new IllegalArgumentException("universeLimit < 0");
            BigInteger universe = BigInteger.ONE.shiftLeft(n);
            if (universe.compareTo(BigInteger.valueOf(universeLimit)) > 0)
                throw new IllegalStateException(
                        "2^n = " + universe + " exceeds universeLimit = " + universeLimit);

            int lim = 1 << n;
            long[] ret = new long[lim - size()];
            int p = 0;
            for (int v = 0; v < lim; v++)
                if (!id.containsKey((long) v))
                    ret[p++] = v;
            return ret;
        }

        /** 小さい n 用。全空間が 2^20 以下なら欠損 vector を列挙する。 */
        public long[] missingVectors() {
            return missingVectors(DEFAULT_MISSING_ENUMERATION_UNIVERSE_LIMIT);
        }

        /** 人間が眺めるための要約。 */
        public String report() {
            BigInteger universe = BigInteger.ONE.shiftLeft(n);
            BigInteger missing = universe.subtract(BigInteger.valueOf(size()));
            double coverage = BigInteger.valueOf(size()).doubleValue() / universe.doubleValue();

            StringBuilder sb = new StringBuilder();
            sb.append("Vector orbit analysis\n");
            sb.append("n             = ").append(n).append('\n');
            sb.append("start         = ").append(formatVector(start)).append('\n');
            sb.append("orbit size    = ").append(size()).append(" / ").append(universe).append('\n');
            sb.append("missing       = ").append(missing).append('\n');
            sb.append(String.format(Locale.ROOT, "coverage      = %.6f%%%n", coverage * 100.0));

            if (universe.compareTo(BigInteger.valueOf(DEFAULT_MISSING_ENUMERATION_UNIVERSE_LIMIT)) <= 0) {
                long[] missingVectors = missingVectors(DEFAULT_MISSING_ENUMERATION_UNIVERSE_LIMIT);
                if (missingVectors.length != 0) {
                    sb.append("missing vectors:\n");
                    for (long v : missingVectors)
                        sb.append("  ").append(formatVector(v)).append('\n');
                }
            } else {
                sb.append("missing vectors: not enumerated (2^n is too large)\n");
            }
            return sb.toString();
        }

        private String formatVector(long v) {
            String s = Long.toBinaryString(v);
            if (s.length() >= n)
                return s;
            return "0".repeat(n - s.length()) + s;
        }
    }

    public final class RowSpaceAction {
        private final List<Subspace> states;
        private final int[] next;
        private final int[] parent;
        private final int[] parentGenerator;
        private final int[] countByRank;

        private final int[] componentOf;
        private final int componentCount;
        private final int[] componentStart;
        private final int[] componentMembers;

        // Reverse graph CSR. reverseEdges[] stores original edge indices u*k+gi.
        private final int[] reverseStart;
        private final int[] reverseEdges;

        private final RowSpaceStabilizerGroup[] groupCache;

        private RowSpaceAction(
                List<Subspace> states,
                int[] next,
                int[] parent,
                int[] parentGenerator,
                int[] countByRank,
                int[] componentOf,
                int componentCount,
                int[] componentStart,
                int[] componentMembers,
                int[] reverseStart,
                int[] reverseEdges) {
            this.states = states;
            this.next = next;
            this.parent = parent;
            this.parentGenerator = parentGenerator;
            this.countByRank = countByRank;
            this.componentOf = componentOf;
            this.componentCount = componentCount;
            this.componentStart = componentStart;
            this.componentMembers = componentMembers;
            this.reverseStart = reverseStart;
            this.reverseEdges = reverseEdges;
            this.groupCache = new RowSpaceStabilizerGroup[componentCount];
        }

        public int size() {
            return states.size();
        }

        /**
         * 到達可能な行空間を返す。
         *
         * <p>
         * 生成行列を {@code A_1, ..., A_k} とすると、
         * 単位行列 {@code I} から生成元を右から有限回掛けて得られる行列
         *
         * <pre>
         * P = A_(i_1) A_(i_2) ... A_(i_m)
         * </pre>
         *
         * の行空間 {@code Row(P)} を、重複なく全て保持する。
         *
         * <p>
         * 各要素は {@link Subspace} として表され、その {@link Subspace#basis()}
         * は行空間の正規化された基底を返す。
         *
         * @return 到達可能な行空間の一覧
         */
        public List<Subspace> states() {
            return states;
        }

        public int[] countByRank() {
            return countByRank.clone();
        }

        public int next(int stateId, int generatorId) {
            checkState(stateId);
            if (generatorId < 0 || generatorId >= generators.length)
                throw new IndexOutOfBoundsException();
            return next[stateId * generators.length + generatorId];
        }

        public int componentCount() {
            return componentCount;
        }

        public int componentOf(int stateId) {
            checkState(stateId);
            return componentOf[stateId];
        }

        public int componentSize(int componentId) {
            checkComponent(componentId);
            return componentStart[componentId + 1] - componentStart[componentId];
        }

        public int[] componentStates(int componentId) {
            checkComponent(componentId);
            return Arrays.copyOfRange(
                    componentMembers,
                    componentStart[componentId],
                    componentStart[componentId + 1]);
        }

        /** I から stateId へ到達する生成元 index 列。 */
        public int[] wordTo(int stateId) {
            checkState(stateId);
            int len = 0;
            for (int v = stateId; parent[v] != -1; v = parent[v])
                len++;

            int[] word = new int[len];
            int p = len;
            for (int v = stateId; parent[v] != -1; v = parent[v])
                word[--p] = parentGenerator[v];
            return word;
        }

        /**
         * component 内の任意の root row space U に対する
         * { s|_U : U s = U, s in <generators>^1 } <= GL(U)
         * を返す。
         *
         * Schreier generators を SCC の辺から構成するため、
         * 元のモノイド要素を全列挙しない。
         */
        public RowSpaceStabilizerGroup stabilizerGroup(int componentId) {
            checkComponent(componentId);
            RowSpaceStabilizerGroup cached = groupCache[componentId];
            if (cached != null)
                return cached;

            int lo = componentStart[componentId];
            int hi = componentStart[componentId + 1];
            int root = componentMembers[lo];
            Subspace rootSpace = states.get(root);

            // transporter root -> v and v -> root, represented as ambient matrices.
            Matrix[] forward = new Matrix[states.size()];
            Matrix[] backward = new Matrix[states.size()];
            int[] queue = new int[hi - lo];
            int qh = 0, qt = 0;

            forward[root] = Matrix.identity(n);
            queue[qt++] = root;
            while (qh < qt) {
                int u = queue[qh++];
                Matrix fu = forward[u];
                int base = u * generators.length;
                for (int gi = 0; gi < generators.length; gi++) {
                    int v = next[base + gi];
                    if (componentOf[v] != componentId || forward[v] != null)
                        continue;
                    forward[v] = fu.multiply(generators[gi]);
                    queue[qt++] = v;
                }
            }
            if (qt != hi - lo)
                throw new AssertionError("SCC forward traversal failed");

            qh = qt = 0;
            backward[root] = Matrix.identity(n);
            queue[qt++] = root;
            while (qh < qt) {
                int v = queue[qh++];
                Matrix bv = backward[v];
                for (int z = reverseStart[v]; z < reverseStart[v + 1]; z++) {
                    int edge = reverseEdges[z];
                    int u = edge / generators.length;
                    int gi = edge % generators.length;
                    if (componentOf[u] != componentId || backward[u] != null)
                        continue;
                    // u --gi--> v --bv--> root
                    backward[u] = generators[gi].multiply(bv);
                    queue[qt++] = u;
                }
            }
            if (qt != hi - lo)
                throw new AssertionError("SCC backward traversal failed");

            LinkedHashSet<InvertibleBinaryMatrix> schreier = new LinkedHashSet<>();
            for (int z = lo; z < hi; z++) {
                int u = componentMembers[z];
                int base = u * generators.length;
                for (int gi = 0; gi < generators.length; gi++) {
                    int v = next[base + gi];
                    if (componentOf[v] != componentId)
                        continue;

                    // root -> u -> v -> root
                    Matrix loop = forward[u]
                            .multiply(generators[gi])
                            .multiply(backward[v]);

                    InvertibleBinaryMatrix restricted = rootSpace.restrict(loop);
                    if (!restricted.isIdentity())
                        schreier.add(restricted);
                }
            }

            RowSpaceStabilizerGroup ret = new RowSpaceStabilizerGroup(
                    componentId,
                    root,
                    rootSpace,
                    new ArrayList<>(schreier));
            groupCache[componentId] = ret;
            return ret;
        }

        public RowSpaceStabilizerGroup stabilizerGroupOfState(int stateId) {
            return stabilizerGroup(componentOf(stateId));
        }

        private void checkState(int stateId) {
            if (stateId < 0 || stateId >= states.size())
                throw new IndexOutOfBoundsException();
        }

        private void checkComponent(int componentId) {
            if (componentId < 0 || componentId >= componentCount)
                throw new IndexOutOfBoundsException();
        }
    }

    /**
     * row-space SCC に付随する stabilizer action group。
     * rootSpace の RREF basis を座標基底として GL(rank, 2) の部分群で表す。
     */
    public static final class RowSpaceStabilizerGroup {
        private final int componentId;
        private final int rootStateId;
        private final Subspace rootSpace;
        private final List<InvertibleBinaryMatrix> generators;
        private final LinearGroup linearGroup;

        private RowSpaceStabilizerGroup(
                int componentId,
                int rootStateId,
                Subspace rootSpace,
                List<InvertibleBinaryMatrix> generators) {
            this.componentId = componentId;
            this.rootStateId = rootStateId;
            this.rootSpace = rootSpace;
            this.generators = Collections.unmodifiableList(generators);

            this.linearGroup = new LinearGroup(rootSpace.rank(), generators);
        }

        public int componentId() {
            return componentId;
        }

        public int rootStateId() {
            return rootStateId;
        }

        public Subspace rootSpace() {
            return rootSpace;
        }

        public int rank() {
            return rootSpace.rank();
        }

        public List<InvertibleBinaryMatrix> generators() {
            return generators;
        }

        /** stabilizer action group の位数。元のモノイド要素数ではない。 */
        public BigInteger order() {
            return linearGroup.order();
        }

        public boolean contains(InvertibleBinaryMatrix map) {
            if (map.n != rank())
                return false;
            return linearGroup.contains(map);
        }
    }

    /** F_2^n 上の可逆行列。row[i] は e_i の像。 */
    public static final class InvertibleBinaryMatrix {
        private final int n;
        private final long[] row;
        private final int hash;

        public InvertibleBinaryMatrix(int n, long... row) {
            if (n < 0 || n > 30)
                throw new IllegalArgumentException("0 <= n <= 30 required here");
            if (row.length != n)
                throw new IllegalArgumentException("row.length != n");
            long mask = n == 0 ? 0 : (1L << n) - 1;
            this.n = n;
            this.row = row.clone();
            for (int i = 0; i < n; i++)
                this.row[i] &= mask;
            if (rref(this.row, n).length != n)
                throw new IllegalArgumentException("matrix is not invertible");
            this.hash = 31 * n + Arrays.hashCode(this.row);
        }

        public int size() {
            return n;
        }

        public long[] rows() {
            return row.clone();
        }

        public long apply(long v) {
            long ret = 0;
            while (v != 0) {
                int i = Long.numberOfTrailingZeros(v);
                ret ^= row[i];
                v &= v - 1;
            }
            return ret;
        }

        public InvertibleBinaryMatrix multiply(InvertibleBinaryMatrix b) {
            if (b.n != n)
                throw new IllegalArgumentException("size mismatch");
            long[] c = new long[n];
            for (int i = 0; i < n; i++)
                c[i] = b.apply(row[i]);
            return new InvertibleBinaryMatrix(n, c);
        }

        public static InvertibleBinaryMatrix identity(int n) {
            long[] row = new long[n];
            for (int i = 0; i < n; i++)
                row[i] = 1L << i;
            return new InvertibleBinaryMatrix(n, row);
        }

        public InvertibleBinaryMatrix inverse() {
            if (2 * n > 63)
                throw new IllegalStateException("inverse requires 2*n <= 63");
            long[] a = new long[n];
            for (int i = 0; i < n; i++)
                a[i] = row[i] | (1L << (n + i));

            for (int col = 0; col < n; col++) {
                int pivot = -1;
                for (int i = col; i < n; i++) {
                    if (((a[i] >>> col) & 1L) != 0) {
                        pivot = i;
                        break;
                    }
                }
                if (pivot == -1)
                    throw new AssertionError("invertibility invariant broken");
                long tmp = a[col];
                a[col] = a[pivot];
                a[pivot] = tmp;
                for (int i = 0; i < n; i++) {
                    if (i != col && ((a[i] >>> col) & 1L) != 0)
                        a[i] ^= a[col];
                }
            }

            long mask = n == 0 ? 0 : (1L << n) - 1;
            long[] inv = new long[n];
            for (int i = 0; i < n; i++)
                inv[i] = (a[i] >>> n) & mask;
            return new InvertibleBinaryMatrix(n, inv);
        }

        public boolean isIdentity() {
            for (int i = 0; i < n; i++)
                if (row[i] != (1L << i))
                    return false;
            return true;
        }

        private int[] asPermutationOfNonzeroVectors() {
            if (n == 0)
                return new int[0];
            int degree = (1 << n) - 1;
            int[] p = new int[degree];
            boolean[] seen = new boolean[degree];
            for (int x = 1; x <= degree; x++) {
                long y = apply(x);
                if (y <= 0 || y > degree)
                    throw new AssertionError("invertibility invariant broken");
                int z = (int) y - 1;
                if (seen[z])
                    throw new AssertionError("invertibility invariant broken");
                seen[z] = true;
                p[x - 1] = z;
            }
            return p;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof InvertibleBinaryMatrix other
                    && n == other.n
                    && Arrays.equals(row, other.row);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "InvertibleBinaryMatrix(" + n + ", " + Arrays.toString(row) + ")";
        }
    }

    /** F_2^n の部分空間。basis は canonical RREF。 */
    public static final class Subspace {
        private final int n;
        private final long[] basis;
        private final int[] pivot;
        private final int hash;

        private Subspace(int n, long[] vectors) {
            this.n = n;
            this.basis = rref(vectors, n);
            this.pivot = new int[basis.length];
            for (int i = 0; i < basis.length; i++)
                this.pivot[i] = Long.numberOfTrailingZeros(basis[i]);
            this.hash = 31 * n + Arrays.hashCode(basis);
        }

        public static Subspace of(int n, long... vectors) {
            return new Subspace(n, vectors.clone());
        }

        public static Subspace full(int n) {
            long[] e = new long[n];
            for (int i = 0; i < n; i++)
                e[i] = 1L << i;
            return new Subspace(n, e);
        }

        public int ambientDimension() {
            return n;
        }

        public int rank() {
            return basis.length;
        }
        
        /**
         * この部分空間の基底を返す。
         *
         * <p>
         * 各基底ベクトルは {@code long} のビット列で表され、
         * bit {@code i} がベクトルの第 {@code i} 成分に対応する。
         *
         * <p>
         * 返される基底は RREF により正規化されているため、
         * 同じ部分空間なら同じ基底列になる。
         *
         * @return この部分空間の正規化された基底
         */
        public long[] basis() {
        	return basis.clone();
        }

        public Subspace rightMultiply(Matrix a) {
            if (a.n != n)
                throw new IllegalArgumentException("matrix size mismatch");
            long[] image = new long[basis.length];
            for (int i = 0; i < basis.length; i++)
                image[i] = a.leftVectorMultiply(basis[i]);
            return new Subspace(n, image);
        }

        /** v in this を、この RREF basis に関する係数 bitset に変換する。 */
        public long coordinatesOf(long v) {
            long x = v;
            long coord = 0;
            for (int i = 0; i < basis.length; i++) {
                if (((x >>> pivot[i]) & 1L) != 0) {
                    x ^= basis[i];
                    coord |= 1L << i;
                }
            }
            if (x != 0)
                throw new IllegalArgumentException("vector is not in subspace");
            return coord;
        }

        /** U a = U を仮定し、a|_U を RREF basis 上の行列として返す。 */
        public InvertibleBinaryMatrix restrict(Matrix a) {
            if (a.n != n)
                throw new IllegalArgumentException("matrix size mismatch");
            long[] rows = new long[basis.length];
            for (int i = 0; i < basis.length; i++) {
                long y = a.leftVectorMultiply(basis[i]);
                rows[i] = coordinatesOf(y);
            }
            InvertibleBinaryMatrix ret = new InvertibleBinaryMatrix(basis.length, rows);
            // SCC 内 loop なら必ず可逆。ここで検査してバグを早く検出する。
            ret.asPermutationOfNonzeroVectors();
            return ret;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Subspace other
                    && n == other.n
                    && Arrays.equals(basis, other.basis);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "Subspace(rank=" + rank() + ", basis=" + Arrays.toString(basis) + ")";
        }
    }

    /** F_2 上の n x n 行列。row[i] の j bit が A[i][j]。 */
    public static final class Matrix {
        private final int n;
        private final long[] row;

        public Matrix(int n, long... rows) {
            if (n < 0 || n > 63)
                throw new IllegalArgumentException("0 <= n <= 63 required");
            if (rows.length != n)
                throw new IllegalArgumentException("rows.length != n");
            long mask = n == 63 ? Long.MAX_VALUE : (n == 0 ? 0 : (1L << n) - 1);
            this.n = n;
            this.row = rows.clone();
            for (int i = 0; i < n; i++)
                this.row[i] &= mask;
        }

        public static Matrix identity(int n) {
            long[] row = new long[n];
            for (int i = 0; i < n; i++)
                row[i] = 1L << i;
            return new Matrix(n, row);
        }

        public int size() {
            return n;
        }

        public long[] rows() {
            return row.clone();
        }

        /** row vector v に右から A を掛ける: v A. */
        public long leftVectorMultiply(long v) {
            long ret = 0;
            while (v != 0) {
                int i = Long.numberOfTrailingZeros(v);
                ret ^= row[i];
                v &= v - 1;
            }
            return ret;
        }

        /** this * b. */
        public Matrix multiply(Matrix b) {
            if (b.n != n)
                throw new IllegalArgumentException("matrix size mismatch");
            long[] c = new long[n];
            for (int i = 0; i < n; i++)
                c[i] = b.leftVectorMultiply(row[i]);
            return new Matrix(n, c);
        }

        public Matrix transpose() {
            long[] tr = new long[n];
            for (int i = 0; i < n; i++) {
                long r = row[i];
                while (r != 0) {
                    int j = Long.numberOfTrailingZeros(r);
                    tr[j] |= 1L << i;
                    r &= r - 1;
                }
            }
            return new Matrix(n, tr);
        }
    }

    /**
     * Deterministic Schreier-Sims for a subgroup of GL(n,2).
     *
     * Base は e_0,...,e_(n-1)。Sims filter で Schreier generator を
     * strong generating set に必要なものだけ追加するため、
     * naive な Schreier recursion の generator explosion を避ける。
     */
    private static final class LinearGroup {
        private final int n;
        private final ArrayList<InvertibleBinaryMatrix> strongGenerators;
        private InvertibleBinaryMatrix[][] transversal;
        private InvertibleBinaryMatrix[][] invTransversal;
        private int[] orbitSize;
        private BigInteger order;

        LinearGroup(int n, List<InvertibleBinaryMatrix> generators) {
            this.n = n;
            this.strongGenerators = new ArrayList<>();
            LinkedHashSet<InvertibleBinaryMatrix> set = new LinkedHashSet<>();
            for (InvertibleBinaryMatrix g : generators) {
                if (g.n != n)
                    throw new IllegalArgumentException("size mismatch");
                g.inverse(); // validate invertibility
                if (!g.isIdentity())
                    set.add(g);
            }
            strongGenerators.addAll(set);
            closeStrongGeneratingSet();
        }

        BigInteger order() {
            return order;
        }

        boolean contains(InvertibleBinaryMatrix g) {
            if (g.n != n)
                return false;
            try {
                g.inverse();
            } catch (IllegalArgumentException ex) {
                return false;
            }
            return sift(g, 0).isIdentity();
        }

        private void closeStrongGeneratingSet() {
            HashSet<InvertibleBinaryMatrix> known = new HashSet<>(strongGenerators);

            while (true) {
                rebuildTransversals();
                InvertibleBinaryMatrix newGenerator = findMissingSchreierGenerator();
                if (newGenerator == null)
                    break;
                if (!known.add(newGenerator))
                    throw new AssertionError("Sims filter produced duplicate residue");
                strongGenerators.add(newGenerator);
            }

            order = BigInteger.ONE;
            for (int s : orbitSize)
                order = order.multiply(BigInteger.valueOf(s));
        }

        /** current strongGenerators から各 stabilizer orbit/transversal を再構築。 */
        private void rebuildTransversals() {
            int universe = 1 << n;
            transversal = new InvertibleBinaryMatrix[n][universe];
            invTransversal = new InvertibleBinaryMatrix[n][universe];
            orbitSize = new int[n];

            for (int level = 0; level < n; level++) {
                int base = 1 << level;
                int[] queue = new int[universe];
                int qh = 0, qt = 0;

                InvertibleBinaryMatrix id = InvertibleBinaryMatrix.identity(n);
                transversal[level][base] = id;
                queue[qt++] = base;

                while (qh < qt) {
                    int x = queue[qh++];
                    InvertibleBinaryMatrix tx = transversal[level][x];
                    for (InvertibleBinaryMatrix g : strongGenerators) {
                        if (firstMovedLevel(g) < level)
                            continue; // g not in pointwise stabilizer of previous base points
                        int y = (int) g.apply(x);
                        if (transversal[level][y] != null)
                            continue;
                        transversal[level][y] = tx.multiply(g);
                        queue[qt++] = y;
                    }
                }

                orbitSize[level] = qt;
                for (int i = 0; i < qt; i++) {
                    int x = queue[i];
                    invTransversal[level][x] = transversal[level][x].inverse();
                }
            }
        }

        /**
         * Schreier lemma の全候補を current BSGS で sift し、
         * subgroup を拡大する最初の residue を返す。
         */
        private InvertibleBinaryMatrix findMissingSchreierGenerator() {
            int universe = 1 << n;
            for (int level = 0; level < n; level++) {
                InvertibleBinaryMatrix[] tr = transversal[level];
                InvertibleBinaryMatrix[] itr = invTransversal[level];

                for (int x = 1; x < universe; x++) {
                    InvertibleBinaryMatrix tx = tr[x];
                    if (tx == null)
                        continue;

                    for (InvertibleBinaryMatrix g : strongGenerators) {
                        if (firstMovedLevel(g) < level)
                            continue;

                        int y = (int) g.apply(x);
                        InvertibleBinaryMatrix h = tx.multiply(g).multiply(itr[y]);
                        InvertibleBinaryMatrix residue = sift(h, level + 1);
                        if (!residue.isIdentity())
                            return residue;
                    }
                }
            }
            return null;
        }

        /** current transversal chain で g を startLevel 以降 sift する。 */
        private InvertibleBinaryMatrix sift(InvertibleBinaryMatrix g, int startLevel) {
            InvertibleBinaryMatrix cur = g;
            for (int level = startLevel; level < n; level++) {
                int base = 1 << level;
                int y = (int) cur.apply(base);
                if (y == base)
                    continue;

                InvertibleBinaryMatrix inv = invTransversal[level][y];
                if (inv == null)
                    return cur;
                cur = cur.multiply(inv);
            }
            return cur;
        }

        /** identity なら n。それ以外は最初に動かす e_i。 */
        private int firstMovedLevel(InvertibleBinaryMatrix g) {
            for (int i = 0; i < n; i++) {
                long e = 1L << i;
                if (g.apply(e) != e)
                    return i;
            }
            return n;
        }
    }

    private static final class SccData {
        int[] componentOf;
        int componentCount;
        int[] componentStart;
        int[] componentMembers;
        int[] reverseStart;
        int[] reverseEdges;
    }

    /** Iterative Kosaraju. */
    private static SccData stronglyConnectedComponents(int n, int k, int[] next) {
        int eCount = next.length;

        int[] indeg = new int[n];
        for (int v : next)
            indeg[v]++;

        int[] revStart = new int[n + 1];
        for (int i = 0; i < n; i++)
            revStart[i + 1] = revStart[i] + indeg[i];
        int[] ptr = revStart.clone();
        int[] revEdges = new int[eCount];
        for (int e = 0; e < eCount; e++) {
            int v = next[e];
            revEdges[ptr[v]++] = e;
        }

        boolean[] seen = new boolean[n];
        int[] order = new int[n];
        int orderSize = 0;
        int[] stackNode = new int[n];
        int[] stackEdge = new int[n];

        for (int s = 0; s < n; s++) {
            if (seen[s])
                continue;
            int sp = 0;
            stackNode[sp] = s;
            stackEdge[sp] = 0;
            seen[s] = true;
            sp++;

            while (sp > 0) {
                int idx = sp - 1;
                int u = stackNode[idx];
                int gi = stackEdge[idx];
                if (gi < k) {
                    stackEdge[idx] = gi + 1;
                    int v = next[u * k + gi];
                    if (!seen[v]) {
                        seen[v] = true;
                        stackNode[sp] = v;
                        stackEdge[sp] = 0;
                        sp++;
                    }
                } else {
                    sp--;
                    order[orderSize++] = u;
                }
            }
        }

        int[] comp = new int[n];
        Arrays.fill(comp, -1);
        int compCount = 0;
        int[] stack = new int[n];

        for (int oi = n - 1; oi >= 0; oi--) {
            int s = order[oi];
            if (comp[s] != -1)
                continue;
            int sp = 0;
            stack[sp++] = s;
            comp[s] = compCount;
            while (sp > 0) {
                int v = stack[--sp];
                for (int z = revStart[v]; z < revStart[v + 1]; z++) {
                    int e = revEdges[z];
                    int u = e / k;
                    if (comp[u] == -1) {
                        comp[u] = compCount;
                        stack[sp++] = u;
                    }
                }
            }
            compCount++;
        }

        int[] compSize = new int[compCount];
        for (int c : comp)
            compSize[c]++;
        int[] compStart = new int[compCount + 1];
        for (int i = 0; i < compCount; i++)
            compStart[i + 1] = compStart[i] + compSize[i];
        int[] cptr = compStart.clone();
        int[] members = new int[n];
        for (int v = 0; v < n; v++)
            members[cptr[comp[v]]++] = v;

        SccData ret = new SccData();
        ret.componentOf = comp;
        ret.componentCount = compCount;
        ret.componentStart = compStart;
        ret.componentMembers = members;
        ret.reverseStart = revStart;
        ret.reverseEdges = revEdges;
        return ret;
    }

    private static long[] rref(long[] input, int n) {
        long[] a = input.clone();
        int rank = 0;
        for (int col = 0; col < n; col++) {
            int pivot = -1;
            for (int i = rank; i < a.length; i++) {
                if (((a[i] >>> col) & 1L) != 0) {
                    pivot = i;
                    break;
                }
            }
            if (pivot == -1)
                continue;

            long tmp = a[rank];
            a[rank] = a[pivot];
            a[pivot] = tmp;

            for (int i = 0; i < a.length; i++) {
                if (i != rank && ((a[i] >>> col) & 1L) != 0)
                    a[i] ^= a[rank];
            }
            rank++;
            if (rank == a.length)
                break;
        }
        return Arrays.copyOf(a, rank);
    }

    private static final class IntBuffer {
        private int[] a = new int[16];
        private int size;

        void add(int x) {
            if (size == a.length)
                a = Arrays.copyOf(a, a.length << 1);
            a[size++] = x;
        }

        int[] toArray() {
            return Arrays.copyOf(a, size);
        }
    }

    /**
     * (x_i, x_(i+1)) -> (x_i xor x_(i+1), x_i xor x_(i+1))
     * に対応する生成行列。
     */
    public static Matrix xorMergeGenerator(int n, int i) {
        if (i < 0 || i + 1 >= n)
            throw new IndexOutOfBoundsException();
        long[] row = new long[n];
        for (int r = 0; r < n; r++)
            row[r] = 1L << r;
        row[i] = row[i + 1] = (1L << i) | (1L << (i + 1));
        return new Matrix(n, row);
    }
}