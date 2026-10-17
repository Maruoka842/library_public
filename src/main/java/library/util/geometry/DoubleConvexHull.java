package library.util.geometry;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

/**
 * 正の次元におけるdouble座標の幾何述語と逐次構築凸包を提供する。
 * 点番号は重複除去後、辞書順に整列した座標配列の添字とする。
 */
public final class DoubleConvexHull {
    private DoubleConvexHull() { }

    // 次元ごとの記号摂動候補表を遅延生成して保持する。
    private static final Map<Integer, List<PerturbationCandidate>> CANDIDATE_TABLES = new ConcurrentHashMap<>();

    private record PerturbationCandidate(int[] rows, int[] coordinates, int matchingSign, BitSet exponent) { }
    private record MinorKey(List<Integer> rows, List<Integer> columns) { }

    /** A*x+B=0を単位法線で正規化した超平面。 */
    public record Hyperplane(List<Double> coefficients) { }

    /** 独立総当たり参照の結果。単体は点番号を整列した不変リストで表す。 */
    public record ReferenceResult(Set<List<Integer>> simplices, Set<Hyperplane> supportingHyperplanes) { }

    /** 全ファセット走査と点・可視ファセット間のconflict graphを選択する。 */
    public enum Strategy { FULL_SCAN, CONFLICT_GRAPH }

    /** 凸包構築の状態。 */
    public enum Status { OK, INSUFFICIENT_AFFINE_DIMENSION }

    /** ファセットのスナップショット。隣接番号iは頂点iを除くridgeの向こう側を指す。 */
    public record Facet(int id, List<Integer> vertices, List<Integer> neighbors) { }

    /** 摂動後の単体凸包。頂点番号は整列済み重複除去点配列の添字。 */
    public record HullResult(Status status, double[][] points, List<Integer> inputToPoint,
            List<Facet> facets, Set<Hyperplane> supportingHyperplanes) { }

    /**
     * 座標をdoubleに変換してアフィン向き行列式の符号を求める。
     * 正確に表せない整数や絶対値の小さい行列式では符号を誤る場合がある。
     * 計算量O(d!)、領域計算量O(d^2)、d>=1。
     * 未テスト
     * @param points d次元点をd+1個格納した配列
     * @return det([1,p_i])の符号{-1,0,1}
     */
    public static int orientation(long[][] points) {
        return orientation(toDouble(points));
    }

    /** double座標からアフィン向きの符号を求める。計算量O(d!)。未テスト */
    public static int orientation(double[][] points) {
        int d = validateSimplex(points);
        double[][] matrix = new double[d + 1][d + 1];
        for (int i = 0; i <= d; i++) {
            matrix[i][0] = 1.0;
            for (int j = 0; j < d; j++) matrix[i][j + 1] = points[i][j];
        }
        return Double.compare(determinant(matrix), 0.0);
    }

    /**
     * 斉次向き行列式にx_ik+=ε^(2^(d*i+k))の記号摂動を適用する。
     * 余因子を指数の昇順に調べ、同じ補小行列式は1呼び出し中に1度だけ計算する。係数演算はdouble。
     * 候補数はΣ_{k=1..d} C(d+1,k)C(d,k)k!以下、異なる補小行列式数はΣ_{k=1..d} C(d+1,k)C(d,k)以下。
     * 未テスト
     * @param points 点番号に対応する座標配列
     * @param ids 相異なるd+1個の点番号
     * @return 摂動後行列式の非零符号
     */
    public static int symbolicOrientation(long[][] points, int[] ids) {
        return symbolicOrientation(toDouble(points), ids);
    }

    /** double座標で余因子方式の記号的向きを判定する。未テスト */
    public static int symbolicOrientation(double[][] points, int[] ids) {
        int d = points.length == 0 ? 0 : points[0].length;
        if (d < 1 || ids.length != d + 1) throw new IllegalArgumentException();
        for (int id : ids) if (id < 0 || id >= points.length || points[id].length != d) throw new IllegalArgumentException();
        Set<Integer> unique = new HashSet<>();
        for (int id : ids) if (!unique.add(id)) throw new IllegalArgumentException("ids must be distinct");
        int[] sortedIds = ids.clone(); Arrays.sort(sortedIds);
        int argumentSign = permutationSign(ids, sortedIds);
        double[][] simplex = new double[d + 1][];
        for (int i = 0; i <= d; i++) simplex[i] = points[sortedIds[i]];
        int exact = orientation(simplex);
        if (exact != 0) return exact * argumentSign;
        double[][] matrix = homogeneousMatrix(simplex);
        Map<MinorKey, Double> minorCache = new HashMap<>();
        for (PerturbationCandidate candidate : CANDIDATE_TABLES.computeIfAbsent(d, DoubleConvexHull::generateCandidates)) {
            MinorKey key = minorKey(d, candidate.rows(), candidate.coordinates());
            double minor = minorCache.computeIfAbsent(key, ignored -> complementaryMinor(matrix, candidate.rows(), candidate.coordinates()));
            double coefficient = candidate.matchingSign() * minor;
            if (coefficient != 0.0) return Double.compare(coefficient, 0.0) * argumentSign;
        }
        throw new IllegalStateException("symbolic orientation has no nonzero coefficient");
    }

    /** 差分テスト用に行列式多項式を展開する低速版。計算量O((d+1)!2^(d*d))。 */
    static int symbolicOrientationSlow(long[][] points, int[] ids) {
        return symbolicOrientationSlow(toDouble(points), ids);
    }

    /** テスト参照用の低速double展開版。 */
    static int symbolicOrientationSlow(double[][] points, int[] ids) {
        int d = points.length == 0 ? 0 : points[0].length;
        int[] sorted = ids.clone(); Arrays.sort(sorted);
        int argumentSign = permutationSign(ids, sorted);
        double[][] simplex = new double[d + 1][]; for (int i = 0; i <= d; i++) simplex[i] = points[sorted[i]];
        int exact = orientation(simplex); if (exact != 0) return exact * argumentSign;
        Map<BigInteger, Double> terms = new HashMap<>();
        symbolicPermuteWeighted(points, sorted, new int[d + 1], new boolean[d + 1], 0, 0, BigInteger.ZERO, terms, 1.0);
        return terms.entrySet().stream().filter(e -> e.getValue() != 0.0).min(Map.Entry.comparingByKey())
                .map(e -> Double.compare(e.getValue(), 0.0) * (((d & 1) == 0) ? 1 : -1) * argumentSign).orElseThrow();
    }

    /**
     * 整数座標をdoubleへ変換し、各d点部分集合と残りの全点を独立に検査する。
     * 摂動後ファセット集合と元座標の重複除去済み支持超平面集合を返す。
     * 計算量O(n^(d+1))、領域計算量O(n^d)。小規模参照入力用。
     * 未テスト
     * @param input 整数座標点配列
     * @return 総当たり結果。アフィン次元がd未満なら空集合
     */
    public static ReferenceResult exhaustiveReference(long[][] input) {
        return exhaustiveReference(toDouble(input));
    }

    /** double座標に対する小規模総当たり参照実装。計算量O(n^(d+1))、領域計算量O(n^d)。未テスト */
    public static ReferenceResult exhaustiveReference(double[][] input) {
        double[][] points = deduplicate(input);
        int d = points.length == 0 ? 0 : points[0].length;
        if (d < 1) throw new IllegalArgumentException("dimension must be positive");
        Set<List<Integer>> simplices = new HashSet<>();
        Set<Hyperplane> planes = new HashSet<>();
        if (findInitialSimplex(points, d) == null) return new ReferenceResult(Set.of(), Set.of());
        int[] chosen = new int[d];
        enumerate(points, d, 0, 0, chosen, simplices, planes);
        return new ReferenceResult(Set.copyOf(simplices), Set.copyOf(planes));
    }

    /**
     * ランダム化逐次挿入とhorizon更新で単体凸包を構築する。
     * 全走査方式は各挿入でO(F)ファセットを調べ、conflict graph方式は継承候補を調べる。
     * doubleの丸めで符号を誤ると、期待計算量や位相の正しさは保証されない。未テスト
     * @param input 座標配列。点番号付与前に重複除去・辞書順整列する
     * @param dimension 正の次元。大きい次元では処理時間が急増する
     * @param seed 挿入順を決めるseed
     * @param strategy 可視ファセットの取得方式
     * @return 状態と生存単体ファセット
     */
    public static HullResult build(long[][] input, int dimension, long seed, Strategy strategy) {
        return build(toDouble(input), dimension, seed, strategy, false);
    }

    /** double座標版。前処理O(n log n)に逐次凸包構築時間を加える。未テスト */
    public static HullResult build(double[][] input, int dimension, long seed, Strategy strategy) {
        return build(input, dimension, seed, strategy, false);
    }

    /**
     * 凸包を構築し、指定時は各挿入後に多様体性・包含・conflict一覧の不変条件を検査する。
     * debug検査は挿入ごとにO(nF+E)。
     * 未テスト
     * @param input 座標配列
     * @param dimension 次元
     * @param seed 挿入順seed
     * @param strategy 可視ファセットの取得方式
     * @param debug 初期化後および各挿入後に不変条件を検査するか
     * @return 凸包結果
     */
    public static HullResult build(long[][] input, int dimension, long seed, Strategy strategy, boolean debug) {
        return build(toDouble(input), dimension, seed, strategy, debug);
    }

    /** double座標の凸包構築。必要に応じて不変条件を検査する。未テスト */
    public static HullResult build(double[][] input, int dimension, long seed, Strategy strategy, boolean debug) {
        if (dimension < 1) throw new IllegalArgumentException("dimension must be positive");
        double[][] p = deduplicate(input);
        if (p.length == 0 || p[0].length != dimension) throw new IllegalArgumentException("dimension mismatch");
        int[] initial = findInitialSimplex(p, dimension);
        if (initial == null)
            return new HullResult(Status.INSUFFICIENT_AFFINE_DIMENSION, p, inputMap(input, p), List.of(), Set.of());
        Set<Integer> base = new HashSet<>(); for (int id : initial) base.add(id);
        List<Integer> order = new ArrayList<>(); for (int i = 0; i < p.length; i++) if (!base.contains(i)) order.add(i);
        Collections.shuffle(order, new Random(seed));
        Engine engine = new Engine(p, dimension, strategy, debug);
        engine.initialize(initial);
        if (debug) engine.check();
        for (int k = 0; k < order.size(); k++) engine.insert(order.get(k));
        List<Facet> facets = engine.snapshot();
        Set<Hyperplane> planes = new HashSet<>();
        for (Facet f : facets) {
            int[] ids = f.vertices().stream().mapToInt(Integer::intValue).toArray();
            Hyperplane plane = supportingPlane(p, ids);
            if (plane != null) planes.add(plane);
        }
        return new HullResult(Status.OK, p, inputMap(input, p), facets, Set.copyOf(planes));
    }

    /** 元入力の各行を重複除去後の点番号へ写像する。時間・領域計算量O(n*d)。未テスト */
    private static List<Integer> inputMap(double[][] input, double[][] unique) {
        Map<List<Double>, Integer> ids = new HashMap<>();
        for (int i = 0; i < unique.length; i++) { List<Double> key = new ArrayList<>(); for (double x : unique[i]) key.add(x); ids.put(List.copyOf(key), i); }
        List<Integer> map = new ArrayList<>();
        for (double[] point : input) { List<Double> key = new ArrayList<>(); for (double x : point) key.add(x == 0.0 ? 0.0 : x); map.add(ids.get(List.copyOf(key))); }
        return List.copyOf(map);
    }

    /** 斉次行列[1|x]を作る。計算量O(d^2)。未テスト */
    private static double[][] homogeneousMatrix(double[][] simplex) {
        int d = simplex.length - 1;
        double[][] matrix = new double[d + 1][d + 1];
        for (int i = 0; i <= d; i++) { matrix[i][0] = 1.0; for (int j = 0; j < d; j++) matrix[i][j + 1] = simplex[i][j]; }
        return matrix;
    }

    /** 引数順から点番号昇順への置換符号を返す。計算量O(d^2)。未テスト */
    private static int permutationSign(int[] argument, int[] sorted) {
        int inversions = 0;
        for (int i = 0; i < argument.length; i++) for (int j = i + 1; j < argument.length; j++) if (argument[i] > argument[j]) inversions++;
        return (inversions & 1) == 0 ? 1 : -1;
    }

    /** 全ての部分行・座標対応を生成し指数順に整列する。候補数はΣC(d+1,k)C(d,k)k!。未テスト */
    private static List<PerturbationCandidate> generateCandidates(int d) {
        List<PerturbationCandidate> out = new ArrayList<>();
        generateCandidates(d, 0, new ArrayList<>(), new ArrayList<>(), new BitSet(d), out);
        out.sort((a, b) -> compareExponent(a.exponent(), b.exponent()));
        return List.copyOf(out);
    }

    /** 各行を未選択または座標列に対応付けて再帰列挙する。出力数は部分マッチング数。未テスト */
    private static void generateCandidates(int d, int row, List<Integer> rows, List<Integer> coordinates,
            BitSet usedCoordinates, List<PerturbationCandidate> out) {
        if (row == d + 1) {
            if (rows.isEmpty()) return;
            int inversions = 0, checkerboard = 0;
            BitSet exponent = new BitSet((d + 1) * d);
            for (int i = 0; i < rows.size(); i++) {
                checkerboard += rows.get(i) + coordinates.get(i) + 1;
                exponent.set(rows.get(i) * d + coordinates.get(i));
                for (int j = i + 1; j < coordinates.size(); j++) if (coordinates.get(i) > coordinates.get(j)) inversions++;
            }
            int sign = ((inversions + checkerboard) & 1) == 0 ? 1 : -1;
            out.add(new PerturbationCandidate(rows.stream().mapToInt(Integer::intValue).toArray(),
                    coordinates.stream().mapToInt(Integer::intValue).toArray(), sign, exponent));
            return;
        }
        generateCandidates(d, row + 1, rows, coordinates, usedCoordinates, out);
        for (int col = 0; col < d; col++) if (!usedCoordinates.get(col)) {
            usedCoordinates.set(col); rows.add(row); coordinates.add(col);
            generateCandidates(d, row + 1, rows, coordinates, usedCoordinates, out);
            rows.remove(rows.size() - 1); coordinates.remove(coordinates.size() - 1); usedCoordinates.clear(col);
        }
    }

    /** 疎な二進指数を非負整数として比較する。計算量O(d^2)。未テスト */
    private static int compareExponent(BitSet a, BitSet b) {
        int max = Math.max(a.length(), b.length());
        for (int bit = max - 1; bit >= 0; bit--) if (a.get(bit) != b.get(bit)) return a.get(bit) ? 1 : -1;
        return 0;
    }

    /** 補小行列式のキャッシュキーを作る。計算量O(d)。未テスト */
    private static MinorKey minorKey(int d, int[] selectedRows, int[] selectedCoordinates) {
        Set<Integer> rs = new HashSet<>(); for (int x : selectedRows) rs.add(x);
        Set<Integer> cs = new HashSet<>(); for (int x : selectedCoordinates) cs.add(x + 1);
        List<Integer> rows = new ArrayList<>(), cols = new ArrayList<>();
        for (int i = 0; i <= d; i++) { if (!rs.contains(i)) rows.add(i); if (!cs.contains(i)) cols.add(i); }
        return new MinorKey(List.copyOf(rows), List.copyOf(cols));
    }

    /** 補余因子の小行列式を遅延計算する。一般行列式の計算量O(d!)。未テスト */
    private static double complementaryMinor(double[][] matrix, int[] selectedRows, int[] selectedCoordinates) {
        int d = matrix.length - 1;
        Set<Integer> rs = new HashSet<>(); for (int x : selectedRows) rs.add(x);
        Set<Integer> cs = new HashSet<>(); for (int x : selectedCoordinates) cs.add(x + 1);
        List<Integer> rows = new ArrayList<>(), cols = new ArrayList<>();
        for (int i = 0; i <= d; i++) { if (!rs.contains(i)) rows.add(i); if (!cs.contains(i)) cols.add(i); }
        double[][] minor = new double[rows.size()][cols.size()];
        for (int i = 0; i < rows.size(); i++) for (int j = 0; j < cols.size(); j++) minor[i][j] = matrix[rows.get(i)][cols.get(j)];
        return determinant(minor);
    }

    private static final class MutableFacet {
        int id;
        int[] v;
        int[] n;
        int insideSign;
        boolean alive = true;
        Set<Integer> conflicts = new HashSet<>();
    }

    private static final class Engine {
        final double[][] p; final int d; final Strategy strategy; final boolean debug;
        final Map<Integer, MutableFacet> live = new HashMap<>();
        final Map<Integer, Set<Integer>> pointConflicts = new HashMap<>();
        final Set<Integer> inserted = new HashSet<>();
        int nextId;
        /** 可変凸包状態を初期化する。計算量O(1)。未テスト */
        Engine(double[][] p, int d, Strategy strategy, boolean debug) { this.p = p; this.d = d; this.strategy = strategy; this.debug = debug; }
        /** 初期単体ファセットとconflictを構築する。計算量O(n*d!)。未テスト */
        void initialize(int[] simplex) {
            for (int id : simplex) inserted.add(id);
            for (int omit = 0; omit <= d; omit++) {
                int[] v = new int[d]; int z = 0; for (int i = 0; i <= d; i++) if (i != omit) v[z++] = simplex[i];
                MutableFacet f = new MutableFacet(); f.id = nextId++; f.v = v; f.n = new int[d];
                f.insideSign = orient(v, simplex[omit]); live.put(f.id, f);
            }
            rebuildNeighbors();
            Set<Integer> initialSet = new HashSet<>(); for (int id : simplex) initialSet.add(id);
            for (int point = 0; point < p.length; point++) if (!initialSet.contains(point)) for (MutableFacet f : live.values()) addConflict(point, f, visible(f, point));
        }
        /** 可視ファセットを置換し継承候補のconflictを更新する。計算量O(F*d!+C*d!)。未テスト */
        void insert(int point) {
            Set<Integer> visible = new HashSet<>();
            if (strategy == Strategy.CONFLICT_GRAPH) visible.addAll(pointConflicts.getOrDefault(point, Set.of()));
            else for (MutableFacet f : live.values()) if (visible(f, point)) visible.add(f.id);
            if (visible.isEmpty()) { pointConflicts.remove(point); inserted.add(point); if (debug) check(); return; }
            Map<Ridge, Integer> horizon = new HashMap<>();
            Set<Integer> candidates = new HashSet<>();
            for (int fid : visible) {
                MutableFacet f = live.get(fid); candidates.addAll(f.conflicts);
                for (int i = 0; i < d; i++) if (!visible.contains(f.n[i])) {
                    MutableFacet survivor = live.get(f.n[i]); candidates.addAll(survivor.conflicts);
                    horizon.put(ridge(f.v, i), survivor.id);
                }
            }
            for (int fid : visible) { MutableFacet gone = live.remove(fid); if (gone != null) for (int q : gone.conflicts) pointConflicts.getOrDefault(q, Set.of()).remove(fid); }
            pointConflicts.remove(point);
            List<MutableFacet> fresh = new ArrayList<>();
            for (Map.Entry<Ridge, Integer> e : horizon.entrySet()) {
                MutableFacet survivor = live.get(e.getValue());
                int[] v = e.getKey().vertices.stream().mapToInt(Integer::intValue).toArray(); v = Arrays.copyOf(v, d); v[d - 1] = point;
                int opposite = opposite(survivor.v, e.getKey());
                int side = orient(v, opposite);
                if (side == 0) continue;
                MutableFacet f = new MutableFacet(); f.id = nextId++; f.v = v; f.n = new int[d]; f.insideSign = side;
                fresh.add(f); live.put(f.id, f);
            }
            rebuildNeighbors();
            if (strategy == Strategy.CONFLICT_GRAPH)
                for (int q : candidates) if (q != point) for (MutableFacet f : fresh) addConflict(q, f, visible(f, q));
            inserted.add(point);
            if (debug) check();
        }
        /** ファセットの向き付き行列式を評価する。計算量O(d!)。未テスト */
        int orient(int[] vertices, int point) { int[] ids = Arrays.copyOf(vertices, d + 1); ids[d] = point; return symbolicOrientation(p, ids); }
        /** ファセット頂点を除き、点が外側から可視か判定する。計算量O(d!)。未テスト */
        boolean visible(MutableFacet f, int point) { for (int v : f.v) if (v == point) return false; return orient(f.v, point) != f.insideSign; }
        /** 点側とファセット側の両方にconflict辺を追加する。期待計算量O(1)。未テスト */
        void addConflict(int point, MutableFacet f, boolean yes) { if (yes) { f.conflicts.add(point); pointConflicts.computeIfAbsent(point, z -> new HashSet<>()).add(f.id); } }
        /** 生存ファセットのridge隣接を再構築する。計算量O(F*d log d)。未テスト */
        void rebuildNeighbors() {
            Map<Ridge, List<int[]>> map = new HashMap<>();
            for (MutableFacet f : live.values()) { Arrays.fill(f.n, -1); for (int i = 0; i < d; i++) map.computeIfAbsent(ridge(f.v, i), z -> new ArrayList<>()).add(new int[] {f.id, i}); }
            for (List<int[]> pair : map.values()) if (pair.size() == 2) { int[] a = pair.get(0), b = pair.get(1); live.get(a[0]).n[a[1]] = b[0]; live.get(b[0]).n[b[1]] = a[0]; }
        }
        /** 不変なファセット一覧を返す。計算量O(F log F)。未テスト */
        List<Facet> snapshot() { List<Facet> out = new ArrayList<>(); for (MutableFacet f : live.values()) out.add(new Facet(f.id, Arrays.stream(f.v).boxed().toList(), Arrays.stream(f.n).boxed().toList())); out.sort((a,b) -> Integer.compare(a.id(), b.id())); return List.copyOf(out); }
        /** 指定ridgeの反対側にある頂点を返す。計算量O(d)。未テスト */
        int opposite(int[] face, Ridge ridge) { for (int v : face) if (!ridge.contains(v)) return v; throw new IllegalStateException(); }
        /** 位相・包含・conflict完全性を検査する。計算量O(nF+E)。未テスト */
        void check() {
            Map<Ridge, List<int[]>> ridges = new HashMap<>();
            for (MutableFacet f : live.values()) {
                Set<Integer> vertices = new HashSet<>(); for (int v : f.v) if (!vertices.add(v)) throw new IllegalStateException("duplicate facet vertex");
                for (int point : inserted) if (!vertices.contains(point) && visible(f, point)) throw new IllegalStateException("inserted point outside hull");
                for (int i = 0; i < d; i++) ridges.computeIfAbsent(ridge(f.v, i), z -> new ArrayList<>()).add(new int[] {f.id, i});
                if (strategy == Strategy.CONFLICT_GRAPH) for (int point : f.conflicts) {
                    if (!live.containsKey(f.id) || !pointConflicts.getOrDefault(point, Set.of()).contains(f.id) || !visible(f, point)) throw new IllegalStateException("invalid conflict edge");
                }
            }
            for (List<int[]> pair : ridges.values()) {
                if (pair.size() != 2) throw new IllegalStateException("ridge degree is not two");
                int[] a = pair.get(0), b = pair.get(1);
                if (live.get(a[0]).n[a[1]] != b[0] || live.get(b[0]).n[b[1]] != a[0]) throw new IllegalStateException("asymmetric adjacency");
            }
            if (strategy == Strategy.CONFLICT_GRAPH) {
                for (Map.Entry<Integer, Set<Integer>> e : pointConflicts.entrySet()) for (int id : e.getValue()) if (!live.containsKey(id) || !live.get(id).conflicts.contains(e.getKey())) throw new IllegalStateException("stale facet reference");
                for (int point = 0; point < p.length; point++) if (!inserted.contains(point)) for (MutableFacet f : live.values()) {
                    boolean edge = pointConflicts.getOrDefault(point, Set.of()).contains(f.id);
                    if (edge != visible(f, point)) throw new IllegalStateException("conflict graph mismatch");
                }
            }
        }
    }

    private record Ridge(List<Integer> vertices) { boolean contains(int x) { return vertices.contains(x); } }
    /** 頂点を除いたridgeの正準キーを作る。計算量O(d log d)。未テスト */
    private static Ridge ridge(int[] v, int omit) { List<Integer> a = new ArrayList<>(); for (int i = 0; i < v.length; i++) if (i != omit) a.add(v[i]); Collections.sort(a); return new Ridge(List.copyOf(a)); }

    /** フルランクな初期単体を探し、なければnullを返す。計算量O(n^(d+1))。未テスト */
    private static int[] findInitialSimplex(double[][] p, int d) {
        if (p.length < d + 1) return null;
        int[] c = new int[d + 1];
        return findInitialSimplex(p, d, c, 0, 0);
    }

    /** 初期単体候補を再帰列挙する。計算量O(n^(d+1))。未テスト */
    private static int[] findInitialSimplex(double[][] p, int d, int[] c, int at, int start) {
        if (at == c.length) return orientation(Arrays.stream(c).mapToObj(i -> p[i]).toArray(double[][]::new)) == 0 ? null : c.clone();
        for (int i = start; i <= p.length - (c.length - at); i++) { c[at] = i; int[] found = findInitialSimplex(p, d, c, at + 1, i + 1); if (found != null) return found; }
        return null;
    }

    /** 整数座標配列をdouble配列へ変換する。計算量O(n*d)。未テスト */
    private static double[][] toDouble(long[][] input) {
        double[][] out = new double[input.length][];
        for (int i = 0; i < input.length; i++) { out[i] = new double[input[i].length]; for (int j = 0; j < input[i].length; j++) out[i][j] = input[i][j]; }
        return out;
    }

    /**
     * 重複点を除去し、点番号を安定させるため辞書順に整列する。
     * 計算量O(n log n)、領域計算量O(n*d)。
     * 未テスト
     * @param input 点配列
     * @return 重複を除去した独立座標配列
     */
    public static long[][] deduplicate(long[][] input) {
        if (input.length == 0) return new long[0][];
        int d = input[0].length;
        if (d < 1) throw new IllegalArgumentException("dimension must be positive");
        List<long[]> list = new ArrayList<>();
        for (long[] p : input) {
            if (p.length != d) throw new IllegalArgumentException("inconsistent dimension");
            list.add(p.clone());
        }
        list.sort((a, b) -> { for (int i = 0; i < d; i++) { int c = Long.compare(a[i], b[i]); if (c != 0) return c; } return 0; });
        List<long[]> unique = new ArrayList<>();
        for (long[] p : list) if (unique.isEmpty() || !Arrays.equals(p, unique.get(unique.size() - 1))) unique.add(p);
        return unique.toArray(long[][]::new);
    }

    /** 有限double点の重複を除去して辞書順に整列する。計算量O(n log n)、領域計算量O(n*d)。未テスト */
    public static double[][] deduplicate(double[][] input) {
        if (input.length == 0) return new double[0][];
        int d = input[0].length;
        if (d < 1) throw new IllegalArgumentException("dimension must be positive");
        List<double[]> list = new ArrayList<>();
        for (double[] point : input) {
            if (point.length != d) throw new IllegalArgumentException("inconsistent dimension");
            double[] copy = point.clone();
            for (int j = 0; j < d; j++) {
                if (!Double.isFinite(copy[j])) throw new IllegalArgumentException("coordinates must be finite");
                if (copy[j] == 0.0) copy[j] = 0.0;
            }
            list.add(copy);
        }
        list.sort((x, y) -> { for (int j = 0; j < d; j++) { int c = Double.compare(x[j], y[j]); if (c != 0) return c; } return 0; });
        List<double[]> unique = new ArrayList<>();
        for (double[] point : list) if (unique.isEmpty() || !Arrays.equals(point, unique.get(unique.size() - 1))) unique.add(point);
        return unique.toArray(double[][]::new);
    }

    /** d点部分集合を列挙し、残りの全点が同じ側か検査する。計算量O(n^(d+1))。未テスト */
    private static void enumerate(double[][] p, int d, int at, int start, int[] c,
            Set<List<Integer>> simplices, Set<Hyperplane> planes) {
        if (at == d) {
            boolean pos = false, neg = false;
            for (int i = 0; i < p.length; i++) {
                boolean member = false; for (int x : c) if (x == i) member = true;
                if (member) continue;
                int[] ids = Arrays.copyOf(c, d + 1); ids[d] = i;
                int s = symbolicOrientation(p, ids); pos |= s > 0; neg |= s < 0;
                if (pos && neg) return;
            }
            if (!pos && !neg) return;
            int[] sorted = c.clone(); Arrays.sort(sorted);
            simplices.add(Arrays.stream(sorted).boxed().toList());
            Hyperplane hp = supportingPlane(p, c);
            if (hp != null) planes.add(hp);
            return;
        }
        for (int i = start; i <= p.length - (d - at); i++) { c[at] = i; enumerate(p, d, at + 1, i + 1, c, simplices, planes); }
    }

    /** 正規化した支持超平面を計算する。計算量O(d^3+nd)。未テスト */
    private static Hyperplane supportingPlane(double[][] p, int[] ids) {
        int d = p[0].length;
        double[] co = new double[d + 1];
        for (int col = 0; col < d; col++) {
            double[][] m = new double[d - 1][d - 1];
            for (int r = 1; r < d; r++) { int k = 0; for (int j = 0; j < d; j++) if (j != col) m[r - 1][k++] = p[ids[r]][j] - p[ids[0]][j]; }
            co[col] = determinant(m) * ((col & 1) == 0 ? 1.0 : -1.0);
        }
        double dot = 0.0;
        for (int j = 0; j < d; j++) dot += co[j] * p[ids[0]][j];
        co[d] = -dot;
        double norm = 0.0; for (int i = 0; i < d; i++) norm = Math.hypot(norm, co[i]);
        if (norm == 0.0 || !Double.isFinite(norm)) return null;
        for (int i = 0; i < co.length; i++) co[i] /= norm;
        for (double v : co) if (v != 0.0) { if (v < 0.0) for (int i = 0; i < co.length; i++) co[i] = -co[i]; break; }
        for (int i = 0; i < co.length; i++) if (co[i] == 0.0) co[i] = 0.0;
        boolean pos = false, neg = false;
        for (int i = 0; i < p.length; i++) {
            double val = co[d]; for (int j = 0; j < d; j++) val += co[j] * p[i][j];
            pos |= val > 0.0; neg |= val < 0.0;
        }
        if (pos && neg) return null;
        return new Hyperplane(Arrays.stream(co).boxed().toList());
    }

    /** 疎な指数を用いて摂動行列式の項を展開する。計算量O((d+1)!2^(d(d+1)))。未テスト */
    private static void symbolicPermuteWeighted(double[][] points, int[] ids, int[] perm, boolean[] used, int row,
            int inversions, BigInteger exponent, Map<BigInteger, Double> terms, double weight) {
        int d = points[0].length;
        if (row == d + 1) { double value = (inversions & 1) == 0 ? weight : -weight; terms.merge(exponent, value, Double::sum); return; }
        for (int col = 0; col <= d; col++) if (!used[col]) {
            int inv = inversions; for (int j = 0; j < row; j++) if (perm[j] > col) inv++;
            used[col] = true; perm[row] = col;
            if (col == d) symbolicPermuteWeighted(points, ids, perm, used, row + 1, inv, exponent, terms, weight);
            else {
                double base = points[ids[row]][col];
                if (base != 0.0) symbolicPermuteWeighted(points, ids, perm, used, row + 1, inv, exponent, terms, weight * base);
                symbolicPermuteWeighted(points, ids, perm, used, row + 1, inv,
                exponent.add(BigInteger.ONE.shiftLeft(row * d + col)), terms, weight);
            }
            used[col] = false;
        }
    }

    /** 余因子展開で行列式を計算する。計算量O(d!)、領域計算量O(d^2)。未テスト */
    private static double determinant(double[][] a) {
        int n = a.length; if (n == 0) return 1.0;
        if (n == 1) return a[0][0];
        double sum = 0.0;
        for (int j = 0; j < n; j++) { double[][] m = new double[n - 1][n - 1]; for (int r = 1; r < n; r++) { int k = 0; for (int c = 0; c < n; c++) if (c != j) m[r - 1][k++] = a[r][c]; } double t = a[0][j] * determinant(m); sum += (j & 1) == 0 ? t : -t; }
        return sum;
    }

    /** 単体配列の形状を検証して次元を返す。計算量O(d^2)。未テスト */
    private static int validateSimplex(double[][] points) {
        if (points.length < 2) throw new IllegalArgumentException();
        int d = points.length - 1; if (d < 1) throw new IllegalArgumentException();
        for (double[] p : points) if (p.length != d) throw new IllegalArgumentException();
        return d;
    }
}
