package library.util.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import library.util.ArrayUtils;
import library.util.MathUtils;
import library.util.linalg.MatrixUtilsZ;

/** 32-bit整数座標に対するlong中間値の向き判定と逐次構築凸包を提供する。 */
public final class Int32ConvexHull {
    private Int32ConvexHull() { }

    /**
     * {@link #build(int[][], int, long)} が返す構築状態。
     * long演算がオーバーフローしないことを前提に、重複除去後の点集合のアフィン次元と要求次元を比較する。
     */
    public enum Status {
        /**
         * アフィン次元が要求次元と等しく、凸包を構築した状態。
         * {@link HullResult#facets()} は記号摂動後の単体facet、
         * {@link HullResult#supportingHyperplanes()} は元の整数座標で復元した支持超平面を含む。
         * {@link HullResult#points()} と {@link HullResult#inputToPoint()} は常に設定される。
         */
        FULL_DIMENSIONAL,

        /**
         * アフィン次元が要求次元未満で、指定次元の凸包を構築できなかった状態。
         * {@link HullResult#points()} と {@link HullResult#inputToPoint()} は設定され、
         * {@link HullResult#facets()} と {@link HullResult#supportingHyperplanes()} は空である。
         */
        INSUFFICIENT_AFFINE_DIMENSION
    }

    /** 頂点を含まずに正規化したlong係数の支持超平面。 */
    public record Hyperplane(List<Long> coefficients) { }

    /** 不変な単体ファセット。頂点iを除くridgeの反対側のfacet IDが隣接番号iである。 */
    public record Facet(int id, List<Integer> vertices, List<Integer> neighbors) { }

    /** 重複除去点、入力対応、構築後のfacetを保持する結果。 */
    public record HullResult(Status status, int[][] points, List<Integer> inputToPoint,
            List<Facet> facets, Set<Hyperplane> supportingHyperplanes) { }

    private record Candidate(int[] rows, int[] columns, int sign, BitSet exponent) { }
    private record Ridge(List<Integer> vertices) {
        /** ridgeに指定点が含まれるかを返す。計算量O(d)。未テスト */
        boolean contains(int point) { return vertices.contains(point); }
    }
    private record Horizon(Ridge ridge, int facetId, int omittedVertex) { }

    /**
     * 斉次行列 [1|p_i] のlong行列式の符号を返す。
     * 中間演算がlongを超える場合の結果は保証しない。
     * 計算量O((d+1)!)、領域計算量O(d^2)。
     * 未テスト
     * @param simplex d次元点をd+1個格納した配列
     * @return 行列式の符号
     */
    public static int orientation(int[][] simplex) {
        int d = validateSimplex(simplex);
        long[][] matrix = new long[d + 1][d + 1];
        for (int i = 0; i <= d; i++) {
            matrix[i][0] = 1L;
            for (int j = 0; j < d; j++) matrix[i][j + 1] = simplex[i][j];
        }
        return Long.compare(MatrixUtilsZ.det(matrix), 0L);
    }

    /**
     * 点番号に固定した摂動 x_ik += ε^(2^(d*i+k)) による斉次行列式の符号を返す。
     * 係数はlong余因子で求め、同じ指数の係数を合算する。中間演算がlongを超える場合の結果は保証しない。
     * 計算量O(d!+C*d!)、C<=Σ C(d+1,k)C(d,k)k!。
     * 未テスト
     * @param points 点番号順の座標配列
     * @param ids 相異なるd+1個の点番号
     * @return 摂動後行列式の非零符号
     */
    public static int symbolicOrientation(int[][] points, int[] ids) {
        int d = points.length == 0 ? 0 : points[0].length;
        if (d < 1 || ids.length != d + 1) throw new IllegalArgumentException("simplex size mismatch");
        Set<Integer> unique = new HashSet<>();
        for (int id : ids) {
            if (id < 0 || id >= points.length || points[id].length != d || !unique.add(id))
                throw new IllegalArgumentException("invalid point IDs");
        }
        int[] sortedIds = ids.clone();
        Arrays.sort(sortedIds);
        int argumentSign = permutationSign(ids, sortedIds);
        int[][] simplex = new int[d + 1][];
        for (int i = 0; i <= d; i++) simplex[i] = points[sortedIds[i]];
        int exact = orientation(simplex);
        if (exact != 0) return exact * argumentSign;

        long[][] matrix = homogeneousMatrix(simplex);
        List<Candidate> candidates = generateCandidates(d, sortedIds);
        for (int begin = 0; begin < candidates.size();) {
            int end = begin + 1;
            while (end < candidates.size() && candidates.get(begin).exponent().equals(candidates.get(end).exponent())) end++;
            long coefficient = 0L;
            for (int i = begin; i < end; i++) {
                Candidate candidate = candidates.get(i);
                long minor = complementaryMinor(matrix, candidate.rows(), candidate.columns());
                coefficient += candidate.sign() * minor;
            }
            if (coefficient != 0L) return Long.compare(coefficient, 0L) * argumentSign;
            begin = end;
        }
        throw new IllegalStateException("摂動行列式に非零係数がありません");
    }

    /**
     * 重複を除去してlong演算による整数凸包をランダム化逐次挿入で構築する。
     * 行列式・支持超平面の中間値がlongを超える場合の結果は保証しない。
     * nを入力点数、dを次元、Fを最大facet数、Qをconflict候補検査数、Hを総horizonサイズ、
     * P_dを1回の摂動向き判定コストとする。初期単体探索は最悪O(n^(d+1)*d!)、前処理はO(n log n)。
     * conflict graphを用い、構築計算量O((n+Q)*P_d+H*d^2)、領域計算量O(n*d+F*d+E)。
     * 未テスト
     * @param input 32-bit整数座標
     * @param dimension 正の次元
     * @param seed 挿入順を決めるseed
     * @param strategy 可視facetの取得方式
     * @return 凸包と重複除去後の入力対応
     */
    public static HullResult build(int[][] input, int dimension, long seed) {
        return build(input, dimension, seed, false);
    }

    /**
     * 凸包を構築し、指定時は各挿入後に位相・包含・conflict不変条件を検査する。
     * debug検査は各挿入でO(nF+E)。未テスト
     * @param input 32-bit整数座標
     * @param dimension 正の次元
     * @param seed 挿入順を決めるseed
     * @param debug 各挿入後に不変条件を検査するか
     * @return 凸包構築結果
     */
    public static HullResult build(int[][] input, int dimension, long seed, boolean debug) {
        if (dimension < 1) throw new IllegalArgumentException("dimension must be positive");
        if (input.length == 0 || input[0].length != dimension) throw new IllegalArgumentException("dimension mismatch");
        int[][] points = new int[input.length][];
        for (int i = 0; i < input.length; i++) {
            if (input[i].length != dimension) throw new IllegalArgumentException("inconsistent dimension");
            points[i] = input[i].clone();
        }
        points = ArrayUtils.sortq(points, Arrays::compare);
        int[] initial = findInitialSimplex(points, dimension);
        List<Integer> inputMap = inputMap(input, points);
        if (initial == null) return new HullResult(Status.INSUFFICIENT_AFFINE_DIMENSION, points, inputMap, List.of(), Set.of());
        Set<Integer> initialIds = new HashSet<>();
        for (int id : initial) initialIds.add(id);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < points.length; i++) if (!initialIds.contains(i)) order.add(i);
        Collections.shuffle(order, new Random(seed));
        Engine engine = new Engine(points, dimension, debug);
        engine.initialize(initial);
        if (debug) engine.check();
        for (int point : order) engine.insert(point);
        List<Facet> facets = engine.snapshot();
        Set<Hyperplane> planes = new HashSet<>();
        for (Facet facet : facets) {
            int[] ids = facet.vertices().stream().mapToInt(Integer::intValue).toArray();
            Hyperplane plane = supportingPlane(points, ids);
            if (plane != null) planes.add(plane);
        }
        return new HullResult(Status.FULL_DIMENSIONAL, points, inputMap, facets, Set.copyOf(planes));
    }

    /** 元入力を重複除去後の点番号へ写像する。計算量O(n*d)。未テスト */
    private static List<Integer> inputMap(int[][] input, int[][] points) {
        Map<List<Integer>, Integer> ids = new HashMap<>();
        for (int i = 0; i < points.length; i++) ids.put(Arrays.stream(points[i]).boxed().toList(), i);
        List<Integer> result = new ArrayList<>();
        for (int[] point : input) result.add(ids.get(Arrays.stream(point).boxed().toList()));
        return List.copyOf(result);
    }

    /** 単体配列の形状を検査して次元を返す。計算量O(d^2)。未テスト */
    private static int validateSimplex(int[][] simplex) {
        if (simplex.length < 2) throw new IllegalArgumentException("simplex size mismatch");
        int d = simplex.length - 1;
        for (int[] point : simplex) if (point.length != d) throw new IllegalArgumentException("dimension mismatch");
        return d;
    }

    /** 引数順から昇順への置換符号を返す。計算量O(d^2)。未テスト */
    private static int permutationSign(int[] ids, int[] sorted) {
        int inversions = 0;
        for (int i = 0; i < ids.length; i++) for (int j = i + 1; j < ids.length; j++) if (ids[i] > ids[j]) inversions++;
        return (inversions & 1) == 0 ? 1 : -1;
    }

    /** 斉次行列[1|x]を構築する。計算量O(d^2)、領域計算量O(d^2)。未テスト */
    private static long[][] homogeneousMatrix(int[][] simplex) {
        int d = simplex.length - 1;
        long[][] matrix = new long[d + 1][d + 1];
        for (int i = 0; i <= d; i++) {
            matrix[i][0] = 1L;
            for (int j = 0; j < d; j++) matrix[i][j + 1] = simplex[i][j];
        }
        return matrix;
    }

    /** 固定点IDに基づく部分マッチングを指数順に生成する。候補数C<=Σ C(d+1,k)C(d,k)k!。未テスト */
    private static List<Candidate> generateCandidates(int d, int[] pointIds) {
        List<Candidate> candidates = new ArrayList<>();
        generateCandidates(d, pointIds, 0, new ArrayList<>(), new ArrayList<>(), new BitSet(), candidates);
        candidates.sort((a, b) -> compareExponent(a.exponent(), b.exponent()));
        return candidates;
    }

    /** 部分行列の行列列対応を再帰列挙する。出力候補数をCとしてO(C*d)。未テスト */
    private static void generateCandidates(int d, int[] pointIds, int row, List<Integer> rows, List<Integer> columns,
            BitSet usedColumns, List<Candidate> out) {
        if (row == d + 1) {
            if (rows.isEmpty()) return;
            int inversions = 0, checkerboard = 0;
            BitSet exponent = new BitSet();
            for (int i = 0; i < rows.size(); i++) {
                checkerboard += rows.get(i) + columns.get(i) + 1;
                exponent.set(pointIds[rows.get(i)] * d + columns.get(i));
                for (int j = i + 1; j < columns.size(); j++) if (columns.get(i) > columns.get(j)) inversions++;
            }
            int sign = ((inversions + checkerboard) & 1) == 0 ? 1 : -1;
            out.add(new Candidate(rows.stream().mapToInt(Integer::intValue).toArray(),
                    columns.stream().mapToInt(Integer::intValue).toArray(), sign, exponent));
            return;
        }
        generateCandidates(d, pointIds, row + 1, rows, columns, usedColumns, out);
        for (int column = 0; column < d; column++) if (!usedColumns.get(column)) {
            usedColumns.set(column);
            rows.add(row);
            columns.add(column);
            generateCandidates(d, pointIds, row + 1, rows, columns, usedColumns, out);
            rows.remove(rows.size() - 1);
            columns.remove(columns.size() - 1);
            usedColumns.clear(column);
        }
    }

    /** 疎な二進指数を数値順に比較する。計算量O(d*n)。未テスト */
    private static int compareExponent(BitSet a, BitSet b) {
        int max = Math.max(a.length(), b.length());
        for (int bit = max - 1; bit >= 0; bit--) if (a.get(bit) != b.get(bit)) return a.get(bit) ? 1 : -1;
        return 0;
    }

    /** 選択摂動の符号付き補小行列式を計算する。計算量O(d!)、領域計算量O(d^2)。未テスト */
    private static long complementaryMinor(long[][] matrix, int[] selectedRows, int[] selectedColumns) {
        int d = matrix.length - 1;
        Set<Integer> rowSet = new HashSet<>(); for (int row : selectedRows) rowSet.add(row);
        Set<Integer> columnSet = new HashSet<>(); for (int column : selectedColumns) columnSet.add(column + 1);
        List<Integer> rows = new ArrayList<>(), columns = new ArrayList<>();
        for (int i = 0; i <= d; i++) { if (!rowSet.contains(i)) rows.add(i); if (!columnSet.contains(i)) columns.add(i); }
        long[][] minor = new long[rows.size()][columns.size()];
        for (int i = 0; i < rows.size(); i++) for (int j = 0; j < columns.size(); j++) minor[i][j] = matrix[rows.get(i)][columns.get(j)];
        return MatrixUtilsZ.det(minor);
    }

    /** フル次元の初期単体を探索する。計算量O(n^(d+1)*d!)。未テスト */
    private static int[] findInitialSimplex(int[][] points, int d) {
        int[] chosen = new int[d + 1];
        return findInitialSimplex(points, d, chosen, 0, 0);
    }

    /** 点部分集合を列挙してフルランク単体を探す。計算量O(n^(d+1)*d!)。未テスト */
    private static int[] findInitialSimplex(int[][] points, int d, int[] chosen, int at, int start) {
        if (at == chosen.length) {
            int[][] simplex = new int[d + 1][];
            for (int i = 0; i <= d; i++) simplex[i] = points[chosen[i]];
            return orientation(simplex) == 0 ? null : chosen.clone();
        }
        for (int i = start; i <= points.length - (chosen.length - at); i++) {
            chosen[at] = i;
            int[] found = findInitialSimplex(points, d, chosen, at + 1, i + 1);
            if (found != null) return found;
        }
        return null;
    }

    /** facet頂点を通る支持超平面を既約整数係数に正規化する。計算量O(d^3+nd^2)。未テスト */
    private static Hyperplane supportingPlane(int[][] points, int[] ids) {
        int d = points[0].length;
        long[] coefficients = new long[d + 1];
        for (int column = 0; column < d; column++) {
            long[][] minor = new long[d - 1][d - 1];
            for (int row = 1; row < d; row++) {
                int at = 0;
                for (int j = 0; j < d; j++) if (j != column)
                    minor[row - 1][at++] = (long) points[ids[row]][j] - points[ids[0]][j];
            }
            long value = MatrixUtilsZ.det(minor);
            coefficients[column] = (column & 1) == 0 ? value : -value;
        }
        long offset = 0L;
        for (int j = 0; j < d; j++) offset -= coefficients[j] * points[ids[0]][j];
        coefficients[d] = offset;
        long gcd = 0L;
        for (long coefficient : coefficients) gcd = MathUtils.gcd(gcd, coefficient);
        if (gcd == 0L) return null;
        for (int i = 0; i < coefficients.length; i++) coefficients[i] /= gcd;
        for (long coefficient : coefficients) if (coefficient != 0L) {
            if (coefficient < 0L) for (int i = 0; i < coefficients.length; i++) coefficients[i] = -coefficients[i];
            break;
        }
        boolean positive = false, negative = false;
        for (int[] point : points) {
            long value = coefficients[d];
            for (int j = 0; j < d; j++) value += coefficients[j] * point[j];
            positive |= value > 0L;
            negative |= value < 0L;
        }
        if (positive && negative) return null;
        List<Long> result = new ArrayList<>();
        for (long coefficient : coefficients) result.add(coefficient);
        return new Hyperplane(List.copyOf(result));
    }

    /** 指定頂点を除くridgeの正準キーを作る。計算量O(d log d)。未テスト */
    private static Ridge ridge(int[] vertices, int omit) {
        List<Integer> remaining = new ArrayList<>();
        for (int i = 0; i < vertices.length; i++) if (i != omit) remaining.add(vertices[i]);
        Collections.sort(remaining);
        return new Ridge(List.copyOf(remaining));
    }

    private static final class MutableFacet {
        int id;
        int[] vertices;
        /**
         * neighbors[i] : vertices[i]を除外してできる Ridge」 を共有して隣接するファセットの ID
         */
        int[] neighbors;
        int insideSign;
        /**
         * この面から見える点の集合
         */
        final Set<Integer> conflicts = new HashSet<>();
    }

    private static final class Engine {
        final int[][] points;
        final int dimension;
        final boolean debug;
        final Map<Integer, MutableFacet> idToSurvivingFacet = new HashMap<>();
        /**未処理の点をキー、その点から見えるfacetの集合をバリューとする。*/
        final Map<Integer, Set<Integer>> pointConflicts = new HashMap<>();
        // 同一の点集合に対するSoS向き判定を構築中に再利用する。
        final Map<List<Integer>, Integer> orientationCache = new HashMap<>();
        /**
         * 処理済みの点の集合
         */
        final Set<Integer> inserted = new HashSet<>();
        int nextId;

        /** 逐次凸包状態を初期化する。計算量O(1)。未テスト */
        Engine(int[][] points, int dimension, boolean debug) {
            this.points = points;
            this.dimension = dimension;
            this.debug = debug;
        }

        /** 初期単体facetとconflictを構築する。計算量O(n*d!)。未テスト */
        void initialize(int[] simplex) {
            for (int id : simplex) inserted.add(id);
            for (int omit = 0; omit <= dimension; omit++) {
                int[] vertices = new int[dimension];
                int at = 0;
                for (int i = 0; i <= dimension; i++) if (i != omit) vertices[at++] = simplex[i];
                MutableFacet facet = new MutableFacet();
                facet.id = nextId++;
                facet.vertices = vertices;
                facet.neighbors = new int[dimension];
                facet.insideSign = orient(vertices, simplex[omit]);
                idToSurvivingFacet.put(facet.id, facet);
            }
            rebuildNeighbors();
            Set<Integer> initial = new HashSet<>(); for (int id : simplex) initial.add(id);
            for (int point = 0; point < points.length; point++) if (!initial.contains(point))
                for (MutableFacet facet : idToSurvivingFacet.values()) addConflict(point, facet, visible(facet, point));
        }

        /** 可視facetをhorizonで置換しconflictを更新する。計算量O(F*d!+C*d!)。未テスト */
        void insert(int point) {
            Set<Integer> visibleFacets = new HashSet<>(pointConflicts.getOrDefault(point, Set.of()));
            if (visibleFacets.isEmpty()) {
                pointConflicts.remove(point);
                inserted.add(point);
                if (debug) check();
                return;
            }
            List<Horizon> horizon = new ArrayList<>(); // 地平線ridgeと接する生き残るfacetを保持する
            Set<Integer> pointsToRecheck = new HashSet<>();
            for (int id : visibleFacets) {
                MutableFacet facet = idToSurvivingFacet.get(id);
                pointsToRecheck.addAll(facet.conflicts);
                for (int i = 0; i < dimension; i++) if (!visibleFacets.contains(facet.neighbors[i])) {
                    MutableFacet survivor = idToSurvivingFacet.get(facet.neighbors[i]);
                    if (survivor == null) throw new IllegalStateException("horizon ridge has no surviving facet");
                    pointsToRecheck.addAll(survivor.conflicts);
                    Ridge boundary = ridge(facet.vertices, i);
                    horizon.add(new Horizon(boundary, survivor.id, ridgeIndex(survivor.vertices, boundary)));
                }
            }
            for (int id : visibleFacets) {
                MutableFacet removed = idToSurvivingFacet.remove(id);
                if (removed != null) for (int q : removed.conflicts) pointConflicts.getOrDefault(q, Set.of()).remove(id);
            }
            pointConflicts.remove(point);
            List<MutableFacet> fresh = new ArrayList<>();
            for (Horizon boundary : horizon) {
                MutableFacet survivingFacetOfBoundary = idToSurvivingFacet.get(boundary.facetId());
                int[] facetVertices = new int[dimension];
                for (int i = 0; i < dimension - 1; i++) facetVertices[i] = boundary.ridge().vertices().get(i);
                facetVertices[dimension - 1] = point;
                int side = orient(facetVertices, opposite(survivingFacetOfBoundary.vertices, boundary.ridge()));
                MutableFacet facet = new MutableFacet();
                facet.id = nextId++;
                facet.vertices = facetVertices;
                facet.neighbors = new int[dimension];
                facet.insideSign = side;
                facet.neighbors[dimension - 1] = survivingFacetOfBoundary.id;
                survivingFacetOfBoundary.neighbors[boundary.omittedVertex()] = facet.id;
                fresh.add(facet);
                idToSurvivingFacet.put(facet.id, facet);
            }
            connectNewFacets(fresh);
            for (int q : pointsToRecheck) 
            	if (q != point) 
            		for (MutableFacet facet : fresh) 
            			addConflict(q, facet, visible(facet, q));
            inserted.add(point);
            if (debug) check();
        }

        /** facet頂点と点のSoS向きを返す。計算量O(d!+C*d!)。未テスト */
        int orient(int[] vertices, int point) {
            int[] ids = Arrays.copyOf(vertices, dimension + 1);
            ids[dimension] = point;
            int[] sorted = ids.clone();
            Arrays.sort(sorted);
            List<Integer> key = Arrays.stream(sorted).boxed().toList();
            int canonical = orientationCache.computeIfAbsent(key, ignored -> symbolicOrientation(points, sorted));
            return canonical * permutationSign(ids, sorted);
        }

        /** facet頂点以外の点がfacetから見えるかを判定する。計算量O(d!+C*d!)。未テスト */
        boolean visible(MutableFacet facet, int point) {
            for (int vertex : facet.vertices) if (vertex == point) return false;
            return orient(facet.vertices, point) != facet.insideSign;
        }

        /** conflict辺を点側とfacet側に登録する。期待計算量O(1)。未テスト */
        void addConflict(int point, MutableFacet facet, boolean conflict) {
            if (conflict) {
                facet.conflicts.add(point);
                pointConflicts.computeIfAbsent(point, ignored -> new HashSet<>()).add(facet.id);
            }
        }

        /** 生存facetのridge隣接を再構築する。計算量O(F*d^2 log(Fd))。未テスト */
        void rebuildNeighbors() {
            Map<Ridge, List<int[]>> incidences = new HashMap<>();
            for (MutableFacet facet : idToSurvivingFacet.values()) {
                Arrays.fill(facet.neighbors, -1);
                for (int i = 0; i < dimension; i++)
                    incidences.computeIfAbsent(ridge(facet.vertices, i), ignored -> new ArrayList<>()).add(new int[] {facet.id, i});
            }
            for (Map.Entry<Ridge, List<int[]>> entry : incidences.entrySet()) {
                List<int[]> pair = entry.getValue();
                if (pair.size() != 2) throw new IllegalStateException("ridge incidence is " + pair.size() + " instead of 2");
                int[] a = pair.get(0), b = pair.get(1);
                idToSurvivingFacet.get(a[0]).neighbors[a[1]] = b[0];
                idToSurvivingFacet.get(b[0]).neighbors[b[1]] = a[0];
            }
        }

        /** horizon由来のfacet隣接を使い、新facet同士のridgeだけを接続する(neighbors配列を更新)。計算量O(H*d^2)。未テスト */
        void connectNewFacets(List<MutableFacet> freshFacets) {
            Map<Ridge, List<int[]>> incidences = new HashMap<>();
            for (MutableFacet facet : freshFacets) for (int i = 0; i < dimension - 1; i++)
                incidences.computeIfAbsent(ridge(facet.vertices, i), ignored -> new ArrayList<>()).add(new int[] {facet.id, i});
            for (List<int[]> pair : incidences.values()) {
            	// 頂点配列の最後 vertices[dimension] = 挿入点 point であり、
                // neighbors[dimension - 1] には既に既存の生存面（survivor）が接続されている。
                // そのため、i = 0 から dimension - 2 までの「側面 Ridge（挿入点 point を含む境界）」を調べる。
                if (pair.size() != 2) throw new IllegalStateException("new ridge incidence is not two");
                int[] a = pair.get(0), b = pair.get(1);
                idToSurvivingFacet.get(a[0]).neighbors[a[1]] = b[0];
                idToSurvivingFacet.get(b[0]).neighbors[b[1]] = a[0];
            }
        }

        /** ridgeをfacetから除く頂点添字を返す。計算量O(d^2)。未テスト */
        int ridgeIndex(int[] vertices, Ridge target) {
            for (int i = 0; i < dimension; i++) if (ridge(vertices, i).equals(target)) return i;
            throw new IllegalStateException("ridge is not a facet face");
        }

        /** 生存facetの不変スナップショットを返す。計算量O(F log F)。未テスト */
        List<Facet> snapshot() {
            List<Facet> result = new ArrayList<>();
            for (MutableFacet facet : idToSurvivingFacet.values()) result.add(new Facet(facet.id,
                    Arrays.stream(facet.vertices).boxed().toList(), Arrays.stream(facet.neighbors).boxed().toList()));
            result.sort((a, b) -> Integer.compare(a.id(), b.id()));
            return List.copyOf(result);
        }

        /** facet上でridgeに含まれない頂点を返す。計算量O(d)。未テスト */
        int opposite(int[] vertices, Ridge ridge) {
            for (int vertex : vertices) if (!ridge.contains(vertex)) return vertex;
            throw new IllegalStateException("ridge is not a facet face");
        }

        /** 多様体性・包含・conflict参照の不変条件を検査する。計算量O(nF*d!+E)。未テスト */
        void check() {
            Map<Ridge, List<int[]>> incidences = new HashMap<>();
            for (MutableFacet facet : idToSurvivingFacet.values()) {
                Set<Integer> unique = new HashSet<>();
                for (int vertex : facet.vertices) if (!unique.add(vertex)) throw new IllegalStateException("duplicate facet vertex");
                for (int point : inserted) if (!unique.contains(point) && visible(facet, point)) throw new IllegalStateException("inserted point outside hull");
                for (int i = 0; i < dimension; i++) incidences.computeIfAbsent(ridge(facet.vertices, i), ignored -> new ArrayList<>()).add(new int[] {facet.id, i});
                for (int point : facet.conflicts)
                    if (!pointConflicts.getOrDefault(point, Set.of()).contains(facet.id) || !visible(facet, point))
                        throw new IllegalStateException("invalid conflict edge");
            }
            for (List<int[]> pair : incidences.values()) {
                if (pair.size() != 2) throw new IllegalStateException("ridge degree is not two");
                int[] a = pair.get(0), b = pair.get(1);
                if (idToSurvivingFacet.get(a[0]).neighbors[a[1]] != b[0] || idToSurvivingFacet.get(b[0]).neighbors[b[1]] != a[0])
                    throw new IllegalStateException("asymmetric adjacency");
            }
            for (Map.Entry<Integer, Set<Integer>> entry : pointConflicts.entrySet()) for (int id : entry.getValue())
                if (!idToSurvivingFacet.containsKey(id) || !idToSurvivingFacet.get(id).conflicts.contains(entry.getKey())) throw new IllegalStateException("stale facet reference");
            for (int point = 0; point < points.length; point++) if (!inserted.contains(point)) for (MutableFacet facet : idToSurvivingFacet.values())
                if (pointConflicts.getOrDefault(point, Set.of()).contains(facet.id) != visible(facet, point))
                    throw new IllegalStateException("conflict graph mismatch");
        }
    }
}
