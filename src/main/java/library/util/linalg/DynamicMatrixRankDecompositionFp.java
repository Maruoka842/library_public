package library.util.linalg;

import java.util.Arrays;
import java.util.Objects;

/**
 * 有限体 F_p 上における、ランク1更新（外積加算）に対応した行列のランク分解を管理するデータ構造。
 *
 * <p>素数法（modulus）を mod とし、行列 M の pivot 行集合 I、列集合 J、および M[I, J] の逆行列 B を保持する。
 * 行列 M 全体を直接保持する代わりに、低ランク構造 X = M[:, J] (pivotColumnMatrix),
 * Y = M[I, :] (pivotRowMatrix), B = M[I, J]^(-1) (inverse) の3つの小さな行列による
 * 暗黙的表現 M = X * B * Y を用いることで、メモリ空間計算量を O((H+W)r + r^2) に、
 * ランク1更新の計算量を O((H+W)r + r^2) に抑える。</p>
 */
public final class DynamicMatrixRankDecompositionFp {
    // 行数 H
    private final int h;
    // 列数 W
    private final int w;
    // 有限体 F_p の素数法 mod
    private final long mod;

    // C = M[:, J] : H x r の行列
    private long[][] pivotColumnMatrix;
    // R = M[I, :] : r x W の行列
    private long[][] pivotRowMatrix;
    // B = M[I, J]^{-1} : r x r の行列
    private long[][] inverse;

    // ピボット行のインデックス配列 I
    private int[] pivotRows;
    // ピボット列 of インデックス配列 J
    private int[] pivotColumns;
    // 現在の行列のランク r
    private int rank;

    /**
     * 指定された行列と素数法を用いて、動的ランクデコンポジション構造を構築する。
     * 未テスト。
     *
     * @param matrix 構築対象の2次元配列
     * @param mod 有限体 F_p の素数法
     * @throws IllegalArgumentException mod <= 1 の場合
     * @throws NullPointerException matrix が null の場合
     *
     * 計算量: O(H * W * min(H, W))。ただし H = matrix.length、W = matrix[0].length。
     */
    // 未テスト
    public DynamicMatrixRankDecompositionFp(long[][] matrix, long mod) {
        this(
                Objects.requireNonNull(matrix, "matrix").length,
                matrix.length == 0 ? 0 : Objects.requireNonNull(matrix[0], "matrix[0]").length,
                matrix,
                mod);
    }

    /**
     * 指定された行数、列数、行列、および素数法を用いて、動的ランクデコンポジション構造を構築する。
     * 未テスト。
     *
     * @param rows 行数 H
     * @param columns 列数 W
     * @param matrix 構築対象 of 2次元配列
     * @param mod 有限体 F_p の素数法
     * @throws IllegalArgumentException rows < 0, columns < 0, mod <= 1, または配列のサイズが指定と矛盾する場合
     * @throws NullPointerException matrix が null の場合
     *
     * 計算量: O(H * W * min(H, W))。ただし H = rows、W = columns。
     */
    // 未テスト
    public DynamicMatrixRankDecompositionFp(
            int rows,
            int columns,
            long[][] matrix,
            long mod) {
        if (rows < 0 || columns < 0) {
            throw new IllegalArgumentException("rows and columns must be nonnegative");
        }
        if (mod <= 1) {
            throw new IllegalArgumentException("mod must be > 1");
        }
        Objects.requireNonNull(matrix, "matrix");
        if (matrix.length != rows) {
            throw new IllegalArgumentException("matrix.length != rows");
        }
        for (int i = 0; i < rows; ++i) {
            if (matrix[i] == null || matrix[i].length != columns) {
                throw new IllegalArgumentException("matrix is not rectangular with the specified dimensions");
            }
        }

        this.h = rows;
        this.w = columns;
        this.mod = mod;
        initializeRankDecomposition(matrix);
    }

    /**
     * 行数 H を取得する。
     * 未テスト。
     *
     * @return 行数 H
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int rows() {
        return h;
    }

    /**
     * 列数 W を取得する。
     * 未テスト。
     *
     * @return 列数 W
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int columns() {
        return w;
    }

    /**
     * 現在の行列のランク r を取得する。
     * 未テスト。
     *
     * @return ランク r
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int rank() {
        return rank;
    }

    /**
     * カーネルの次元（W - r）を取得する。
     * 未テスト。
     *
     * @return カーネルの次元
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int nullity() {
        return w - rank;
    }

    /**
     * 左カーネルの次元（H - r）を取得する。
     * 未テスト。
     *
     * @return 左カーネルの次元
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int leftNullity() {
        return h - rank;
    }

    /**
     * 行列の i 行 j 列目の要素を取得する。
     * 未テスト。
     *
     * @param i 行インデックス (0 <= i < H)
     * @param j 列インデックス (0 <= j < W)
     * @return i 行 j 列目の要素
     *
     * 計算量: O(r^2)。ただし r は現在のランク。
     */
    // 未テスト
    public long get(int i, int j) {
        if (i < 0 || i >= h || j < 0 || j >= w) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        if (rank == 0) {
            return 0;
        }
        long[] brCol = new long[rank];
        for (int rowIdx = 0; rowIdx < rank; ++rowIdx) {
            long val = 0;
            for (int k = 0; k < rank; ++k) {
                val = add(val, mul(inverse[rowIdx][k], pivotRowMatrix[k][j]));
            }
            brCol[rowIdx] = val;
        }
        long value = 0;
        for (int k = 0; k < rank; ++k) {
            value = add(value, mul(pivotColumnMatrix[i][k], brCol[k]));
        }
        return value;
    }

    /**
     * 行列 M に対し M <- M + coefficient * left * right^T のランク1更新を行い、更新後のランクを返す。
     * 未テスト。
     *
     * @param left 長さ H の左ベクトル
     * @param right 長さ W の右ベクトル
     * @param coefficient 係数
     * @return 更新後のランク
     *
     * 計算量: O((H + W) * r + r^2)。
     */
    // 未テスト
    public int addOuterProduct(long[] left, long[] right, long coefficient) {
        Analysis analysis = analyzeOuterProduct(left, right, coefficient);
        applyOuterProduct(analysis);
        return rank;
    }

    /**
     * 状態を更新することなく、ランク1更新 M + coefficient * left * right^T を適用したと仮定したときのランクを返す。
     * 未テスト。
     *
     * @param left 長さ H の左ベクトル
     * @param right 長さ W の右ベクトル
     * @param coefficient 係数
     * @return 更新したと仮定したときのランク
     *
     * 計算量: O(H * r + W * r + r^2)。ただし r は現在のランク。
     */
    // 未テスト
    public int rankAfterOuterProduct(long[] left, long[] right, long coefficient) {
        return analyzeOuterProduct(left, right, coefficient).rankAfter;
    }

    /**
     * ランク1更新によるランク変化がない（ランクが保存される）場合に限り、M <- M + coefficient * left * right^T を適用し true を返す。
     * ランクが変化する場合は、更新を一切行わずに false を返す。
     * 未テスト。
     *
     * @param left 長さ H の左ベクトル
     * @param right 長さ W の右ベクトル
     * @param coefficient 係数
     * @return 更新された場合は true、更新されなかった場合は false
     *
     * 計算量: O((H + W) * r + r^2)。
     */
    // 未テスト
    public boolean tryAddOuterProductPreservingRank(
            long[] left,
            long[] right,
            long coefficient) {
        Analysis analysis = analyzeOuterProduct(left, right, coefficient);
        if (analysis.rankAfter != rank) {
            return false;
        }
        applyOuterProduct(analysis);
        return true;
    }

    /**
     * 与えられたベクトル vector が現在の列空間（Column Space）に含まれるか判定する。
     * 未テスト。
     *
     * @param vector 長さ H のベクトル
     * @return 含まれる場合は true、そうでない場合は false
     *
     * 計算量: O(H * r + r^2)。
     */
    // 未テスト
    public boolean isInColumnSpace(long[] vector) {
        return columnSpaceCoordinates(vector) != null;
    }

    /**
     * 与えられたベクトル vector が現在の行空間（Row Space）に含まれるか判定する。
     * 未テスト。
     *
     * @param vector 長さ W のベクトル
     * @return 含まれる場合は true、そうでない場合は false
     *
     * 計算量: O(W * r + r^2)。
     */
    // 未テスト
    public boolean isInRowSpace(long[] vector) {
        return rowSpaceCoordinates(vector) != null;
    }

    /**
     * ベクトル vector が現在の列空間に含まれる場合、vector = M[:, pivotColumns] * alpha を満たす係数配列 alpha を返す。
     * 含まれない場合は null を返す。
     * 未テスト。
     *
     * @param vector 長さ H のベクトル
     * @return 係数配列 alpha、または null
     *
     * 計算量: O(H * r + r^2)。
     */
    // 未テスト
    public long[] columnSpaceCoordinates(long[] vector) {
        long[] y = normalizedVector(vector, h, "vector");
        long[] restricted = new long[rank];
        for (int i = 0; i < rank; ++i) {
            restricted[i] = y[pivotRows[i]];
        }
        long[] alpha = multiply(inverse, restricted);
        for (int i = 0; i < h; ++i) {
            long value = 0;
            for (int k = 0; k < rank; ++k) {
                value = add(value, mul(pivotColumnMatrix[i][k], alpha[k]));
            }
            if (value != y[i]) {
                return null;
            }
        }
        return alpha;
    }

    /**
     * ベクトル vector が現在の行空間に含まれる場合、vector^T = beta^T * M[pivotRows, :] を満たす係数配列 beta を返す。
     * 含まれない場合は null を返す。
     * 未テスト。
     *
     * @param vector 長さ W のベクトル
     * @return 係数配列 beta、または null
     *
     * 計算量: O(W * r + r^2)。
     */
    // 未テスト
    public long[] rowSpaceCoordinates(long[] vector) {
        long[] y = normalizedVector(vector, w, "vector");
        long[] beta = new long[rank];
        for (int j = 0; j < rank; ++j) {
            long value = 0;
            for (int k = 0; k < rank; ++k) {
                value = add(value, mul(y[pivotColumns[k]], inverse[k][j]));
            }
            beta[j] = value;
        }
        for (int j = 0; j < w; ++j) {
            long value = 0;
            for (int k = 0; k < rank; ++k) {
                value = add(value, mul(beta[k], pivotRowMatrix[k][j]));
            }
            if (value != y[j]) {
                return null;
            }
        }
        return beta;
    }

    /**
     * 線形方程式 M * x = rightHandSide の特殊解 x を返す（自由変数はすべて 0 とする）。
     * 解が存在しない（不整合な方程式）場合は null を返す。
     * 未テスト。
     *
     * @param rightHandSide 長さ H の右辺ベクトル
     * @return 特殊解 x、または null
     *
     * 計算量: O(H * r + r^2 + W)。
     */
    // 未テスト
    public long[] solve(long[] rightHandSide) {
        long[] alpha = columnSpaceCoordinates(rightHandSide);
        if (alpha == null) {
            return null;
        }
        long[] x = new long[w];
        for (int k = 0; k < rank; ++k) {
            x[pivotColumns[k]] = alpha[k];
        }
        return x;
    }

    /**
     * 現在のピボット行インデックス配列のコピーを返す。
     * 未テスト。
     *
     * @return ピボット行インデックス配列
     *
     * 計算量: O(r)。
     */
    // 未テスト
    public int[] pivotRows() {
        return pivotRows.clone();
    }

    /**
     * 現在のピボット列インデックス配列のコピーを返す。
     * 未テスト。
     *
     * @return ピボット列インデックス配列
     *
     * 計算量: O(r)。
     */
    // 未テスト
    public int[] pivotColumns() {
        return pivotColumns.clone();
    }

    /**
     * 現在の列空間の基底を格納した2次元配列（サイズ r x H）を取得する。
     * 未テスト。
     *
     * @return 列空間 of 基底配列
     *
     * 計算量: O(r * H)。
     */
    // 未テスト
    public long[][] columnSpaceBasis() {
        long[][] result = new long[rank][h];
        for (int k = 0; k < rank; ++k) {
            for (int i = 0; i < h; ++i) {
                result[k][i] = pivotColumnMatrix[i][k];
            }
        }
        return result;
    }

    /**
     * 現在の行空間の基底を格納した2次元配列（サイズ r x W）を取得する。
     * 未テスト。
     *
     * @return 行空間 of 基底配列
     *
     * 計算量: O(r * W)。
     */
    // 未テスト
    public long[][] rowSpaceBasis() {
        long[][] result = new long[rank][w];
        for (int k = 0; k < rank; ++k) {
            result[k] = pivotRowMatrix[k].clone();
        }
        return result;
    }

    /**
     * M * x = 0 を満たすカーネル（右核、零空間）の基底を取得する。
     * 返される配列の各行が 1 つの基底ベクトルを表す。
     * 未テスト。
     *
     * @return カーネル of 基底配列
     *
     * 計算量: O((W - r) * r^2 + (W - r) * W)。
     */
    // 未テスト
    public long[][] kernelBasis() {
        boolean[] isPivot = new boolean[w];
        for (int col : pivotColumns) {
            isPivot[col] = true;
        }
        long[][] result = new long[w - rank][w];
        int out = 0;
        for (int free = 0; free < w; ++free) {
            if (isPivot[free]) {
                continue;
            }
            long[] restricted = new long[rank];
            for (int i = 0; i < rank; ++i) {
                restricted[i] = pivotRowMatrix[i][free];
            }
            long[] alpha = multiply(inverse, restricted);
            long[] x = result[out++];
            x[free] = 1 % mod;
            for (int i = 0; i < rank; ++i) {
                x[pivotColumns[i]] = neg(alpha[i]);
            }
        }
        return result;
    }

    /**
     * y^T * M = 0 を満たす左カーネル（左核）の基底を取得する。
     * 返される配列の各行が 1 つの基底ベクトルを表す。
     * 未テスト。
     *
     * @return 左カーネル of 基底配列
     *
     * 計算量: O((H - r) * r^2 + (H - r) * H)。
     */
    // 未テスト
    public long[][] leftKernelBasis() {
        boolean[] isPivot = new boolean[h];
        for (int row : pivotRows) {
            isPivot[row] = true;
        }
        long[][] result = new long[h - rank][h];
        int out = 0;
        for (int free = 0; free < h; ++free) {
            if (isPivot[free]) {
                continue;
            }
            long[] beta = new long[rank];
            for (int j = 0; j < rank; ++j) {
                long value = 0;
                for (int k = 0; k < rank; ++k) {
                    value = add(value, mul(pivotColumnMatrix[free][k], inverse[k][j]));
                }
                beta[j] = value;
            }
            long[] y = result[out++];
            y[free] = 1 % mod;
            for (int i = 0; i < rank; ++i) {
                y[pivotRows[i]] = neg(beta[i]);
            }
        }
        return result;
    }

    /**
     * デバッグ・テスト用に、現在の行列全体のコピーを構築して返す。
     * 未テスト。
     *
     * @return 現在の行列のコピー
     *
     * 計算量: O(H * W * r + r^2 * W)。
     */
    // 未テスト
    public long[][] toArray() {
        long[][] result = new long[h][w];
        if (rank == 0) {
            return result;
        }
        long[][] x = new long[rank][w];
        for (int i = 0; i < rank; ++i) {
            for (int j = 0; j < w; ++j) {
                long val = 0;
                for (int k = 0; k < rank; ++k) {
                    val = add(val, mul(inverse[i][k], pivotRowMatrix[k][j]));
                }
                x[i][j] = val;
            }
        }
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                long val = 0;
                for (int k = 0; k < rank; ++k) {
                    val = add(val, mul(pivotColumnMatrix[i][k], x[k][j]));
                }
                result[i][j] = val;
            }
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------------

    private void initializeRankDecomposition(long[][] initialMatrix) {
        long[][] work = new long[h][w];
        for (int i = 0; i < h; ++i) {
            for (int j = 0; j < w; ++j) {
                work[i][j] = normalize(initialMatrix[i][j]);
            }
        }
        int[] rowPermutation = new int[h];
        for (int i = 0; i < h; ++i) {
            rowPermutation[i] = i;
        }

        int maxRank = Math.min(h, w);
        int[] rowsTmp = new int[maxRank];
        int[] colsTmp = new int[maxRank];
        int r = 0;

        for (int col = 0; col < w && r < h; ++col) {
            int pivot = -1;
            for (int row = r; row < h; ++row) {
                if (work[row][col] != 0) {
                    pivot = row;
                    break;
                }
            }
            if (pivot < 0) {
                continue;
            }
            if (pivot != r) {
                long[] tempRow = work[pivot];
                work[pivot] = work[r];
                work[r] = tempRow;
                int tempIndex = rowPermutation[pivot];
                rowPermutation[pivot] = rowPermutation[r];
                rowPermutation[r] = tempIndex;
            }

            rowsTmp[r] = rowPermutation[r];
            colsTmp[r] = col;

            long invPivot = inv(work[r][col]);
            for (int j = col; j < w; ++j) {
                work[r][j] = mul(work[r][j], invPivot);
            }
            for (int row = r + 1; row < h; ++row) {
                long factor = work[row][col];
                if (factor == 0) {
                    continue;
                }
                for (int j = col; j < w; ++j) {
                    work[row][j] = sub(work[row][j], mul(factor, work[r][j]));
                }
            }
            ++r;
        }

        rank = r;
        pivotRows = Arrays.copyOf(rowsTmp, r);
        pivotColumns = Arrays.copyOf(colsTmp, r);
        long[][] pivot = new long[r][r];
        for (int i = 0; i < r; ++i) {
            for (int j = 0; j < r; ++j) {
                pivot[i][j] = normalize(initialMatrix[pivotRows[i]][pivotColumns[j]]);
            }
        }
        inverse = invertSquare(pivot);

        // Initialize low-rank representation matrices C and R
        pivotColumnMatrix = new long[h][r];
        for (int i = 0; i < h; ++i) {
            for (int k = 0; k < r; ++k) {
                pivotColumnMatrix[i][k] = normalize(initialMatrix[i][pivotColumns[k]]);
            }
        }

        pivotRowMatrix = new long[r][w];
        for (int k = 0; k < r; ++k) {
            for (int j = 0; j < w; ++j) {
                pivotRowMatrix[k][j] = normalize(initialMatrix[pivotRows[k]][j]);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Rank-one analysis and mutation
    // ---------------------------------------------------------------------

    private static final class Analysis {
        final long[] left;
        final long[] right;
        final long coefficient;
        final long[] a; // inverse * left_I
        final long[] b; // right_J^T * inverse
        final int outsideRow;
        final int outsideColumn;
        final long leftResidualAtOutsideRow;
        final long rightResidualAtOutsideColumn;
        final long pivotDeterminantFactor; // 1 + c * right_J^T * inverse * left_I
        final int rankAfter;

        Analysis(
                long[] left,
                long[] right,
                long coefficient,
                long[] a,
                long[] b,
                int outsideRow,
                int outsideColumn,
                long leftResidualAtOutsideRow,
                long rightResidualAtOutsideColumn,
                long pivotDeterminantFactor,
                int rankAfter) {
            this.left = left;
            this.right = right;
            this.coefficient = coefficient;
            this.a = a;
            this.b = b;
            this.outsideRow = outsideRow;
            this.outsideColumn = outsideColumn;
            this.leftResidualAtOutsideRow = leftResidualAtOutsideRow;
            this.rightResidualAtOutsideColumn = rightResidualAtOutsideColumn;
            this.pivotDeterminantFactor = pivotDeterminantFactor;
            this.rankAfter = rankAfter;
        }
    }

    private Analysis analyzeOuterProduct(long[] left, long[] right, long coefficient) {
        long[] u = normalizedVector(left, h, "left");
        long[] v = normalizedVector(right, w, "right");
        long c = normalize(coefficient);

        if (c == 0) {
            return new Analysis(u, v, c, new long[rank], new long[rank],
                    -1, -1, 0, 0, 1 % mod, rank);
        }

        long[] uI = new long[rank];
        for (int i = 0; i < rank; ++i) {
            uI[i] = u[pivotRows[i]];
        }
        long[] a = multiply(inverse, uI);

        long[] b = new long[rank];
        for (int j = 0; j < rank; ++j) {
            long value = 0;
            for (int k = 0; k < rank; ++k) {
                value = add(value, mul(v[pivotColumns[k]], inverse[k][j]));
            }
            b[j] = value;
        }

        int outsideRow = -1;
        long leftResidual = 0;
        for (int i = 0; i < h; ++i) {
            long projection = 0;
            for (int k = 0; k < rank; ++k) {
                projection = add(projection, mul(pivotColumnMatrix[i][k], a[k]));
            }
            long residual = sub(u[i], projection);
            if (residual != 0) {
                outsideRow = i;
                leftResidual = residual;
                break;
            }
        }

        int outsideColumn = -1;
        long rightResidual = 0;
        for (int j = 0; j < w; ++j) {
            long projection = 0;
            for (int k = 0; k < rank; ++k) {
                projection = add(projection, mul(b[k], pivotRowMatrix[k][j]));
            }
            long residual = sub(v[j], projection);
            if (residual != 0) {
                outsideColumn = j;
                rightResidual = residual;
                break;
            }
        }

        long dot = 0;
        for (int k = 0; k < rank; ++k) {
            dot = add(dot, mul(v[pivotColumns[k]], a[k]));
        }
        long s = add(1 % mod, mul(c, dot));

        int rankAfter;
        if (outsideRow >= 0 && outsideColumn >= 0) {
            rankAfter = rank + 1;
        } else if (outsideRow >= 0 || outsideColumn >= 0) {
            rankAfter = rank;
        } else {
            rankAfter = (s == 0 ? rank - 1 : rank);
        }

        return new Analysis(
                u, v, c, a, b,
                outsideRow, outsideColumn,
                leftResidual, rightResidual,
                s, rankAfter);
    }

    private void applyOuterProduct(Analysis analysis) {
        if (analysis.coefficient == 0) {
            return;
        }

        int oldRank = rank;
        int[] newPivotRows = pivotRows;
        int[] newPivotColumns = pivotColumns;
        long[][] newInverse = inverse;

        long[][] newPivotColumnMatrix = pivotColumnMatrix;
        long[][] newPivotRowMatrix = pivotRowMatrix;

        boolean leftOutside = analysis.outsideRow >= 0;
        boolean rightOutside = analysis.outsideColumn >= 0;

        if (leftOutside && rightOutside) {
            int p = analysis.outsideRow;
            int q = analysis.outsideColumn;
            long delta = analysis.leftResidualAtOutsideRow;
            long epsilon = analysis.rightResidualAtOutsideColumn;

            long[] x = new long[oldRank]; // inverse * R[:,q]
            for (int i = 0; i < oldRank; ++i) {
                long value = 0;
                for (int k = 0; k < oldRank; ++k) {
                    value = add(value, mul(inverse[i][k], pivotRowMatrix[k][q]));
                }
                x[i] = value;
            }

            long[] y = new long[oldRank]; // C[p,:] * inverse
            for (int j = 0; j < oldRank; ++j) {
                long value = 0;
                for (int k = 0; k < oldRank; ++k) {
                    value = add(value, mul(pivotColumnMatrix[p][k], inverse[k][j]));
                }
                y[j] = value;
            }

            long invDelta = inv(delta);
            long invEpsilon = inv(epsilon);
            long gammaDenominator = mul(mul(analysis.coefficient, delta), epsilon);
            long gamma = mul(analysis.pivotDeterminantFactor, inv(gammaDenominator));

            newInverse = new long[oldRank + 1][oldRank + 1];
            for (int i = 0; i < oldRank; ++i) {
                for (int j = 0; j < oldRank; ++j) {
                    long value = inverse[i][j];
                    value = add(value, mul(mul(analysis.a[i], y[j]), invDelta));
                    value = add(value, mul(mul(x[i], analysis.b[j]), invEpsilon));
                    value = add(value, mul(mul(gamma, x[i]), y[j]));
                    newInverse[i][j] = value;
                }
                newInverse[i][oldRank] = sub(neg(mul(analysis.a[i], invDelta)), mul(gamma, x[i]));
            }
            for (int j = 0; j < oldRank; ++j) {
                newInverse[oldRank][j] = sub(neg(mul(analysis.b[j], invEpsilon)), mul(gamma, y[j]));
            }
            newInverse[oldRank][oldRank] = gamma;

            newPivotRows = Arrays.copyOf(pivotRows, oldRank + 1);
            newPivotColumns = Arrays.copyOf(pivotColumns, oldRank + 1);
            newPivotRows[oldRank] = p;
            newPivotColumns[oldRank] = q;

            // Update C' and R'
            long[] mOldColQ = new long[h];
            for (int i = 0; i < h; ++i) {
                long val = 0;
                for (int k = 0; k < oldRank; ++k) {
                    val = add(val, mul(pivotColumnMatrix[i][k], x[k]));
                }
                mOldColQ[i] = val;
            }

            long[] mOldRowP = new long[w];
            for (int j = 0; j < w; ++j) {
                long val = 0;
                for (int k = 0; k < oldRank; ++k) {
                    val = add(val, mul(y[k], pivotRowMatrix[k][j]));
                }
                mOldRowP[j] = val;
            }

            newPivotColumnMatrix = new long[h][oldRank + 1];
            for (int i = 0; i < h; ++i) {
                for (int k = 0; k < oldRank; ++k) {
                    newPivotColumnMatrix[i][k] = add(pivotColumnMatrix[i][k], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[k]]));
                }
                newPivotColumnMatrix[i][oldRank] = add(mOldColQ[i], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[q]));
            }

            newPivotRowMatrix = new long[oldRank + 1][w];
            for (int k = 0; k < oldRank; ++k) {
                for (int j = 0; j < w; ++j) {
                    newPivotRowMatrix[k][j] = add(pivotRowMatrix[k][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[k]]), analysis.right[j]));
                }
            }
            for (int j = 0; j < w; ++j) {
                newPivotRowMatrix[oldRank][j] = add(mOldRowP[j], mul(mul(analysis.coefficient, analysis.left[p]), analysis.right[j]));
            }

        } else if (leftOutside) {
            if (analysis.pivotDeterminantFactor != 0) {
                newInverse = inverseAfterShermanMorrison(analysis);

                newPivotColumnMatrix = new long[h][oldRank];
                for (int i = 0; i < h; ++i) {
                    for (int k = 0; k < oldRank; ++k) {
                        newPivotColumnMatrix[i][k] = add(pivotColumnMatrix[i][k], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[k]]));
                    }
                }

                newPivotRowMatrix = new long[oldRank][w];
                for (int k = 0; k < oldRank; ++k) {
                    for (int j = 0; j < w; ++j) {
                        newPivotRowMatrix[k][j] = add(pivotRowMatrix[k][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[k]]), analysis.right[j]));
                    }
                }
            } else {
                int p = analysis.outsideRow;
                int t = firstNonZero(analysis.b);
                if (t < 0) {
                    throw new AssertionError("singular pivot update without a left null vector");
                }
                long[] wRow = new long[oldRank];
                for (int j = 0; j < oldRank; ++j) {
                    wRow[j] = add(
                            pivotColumnMatrix[p][j],
                            mul(mul(analysis.coefficient, analysis.left[p]), analysis.right[pivotColumns[j]]));
                }
                newInverse = inverseAfterSingularRowReplacement(
                        inverse, analysis.a, analysis.b, t, wRow);
                newPivotRows = pivotRows.clone();
                newPivotRows[t] = p;

                newPivotColumnMatrix = new long[h][oldRank];
                for (int i = 0; i < h; ++i) {
                    for (int k = 0; k < oldRank; ++k) {
                        newPivotColumnMatrix[i][k] = add(pivotColumnMatrix[i][k], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[k]]));
                    }
                }

                long[] y = new long[oldRank]; // C[p,:] * inverse
                for (int j = 0; j < oldRank; ++j) {
                    long value = 0;
                    for (int k = 0; k < oldRank; ++k) {
                        value = add(value, mul(pivotColumnMatrix[p][k], inverse[k][j]));
                    }
                    y[j] = value;
                }

                long[] mOldRowP = new long[w];
                for (int j = 0; j < w; ++j) {
                    long val = 0;
                    for (int k = 0; k < oldRank; ++k) {
                        val = add(val, mul(y[k], pivotRowMatrix[k][j]));
                    }
                    mOldRowP[j] = val;
                }

                newPivotRowMatrix = new long[oldRank][w];
                for (int k = 0; k < oldRank; ++k) {
                    if (k != t) {
                        for (int j = 0; j < w; ++j) {
                            newPivotRowMatrix[k][j] = add(pivotRowMatrix[k][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[k]]), analysis.right[j]));
                        }
                    } else {
                        for (int j = 0; j < w; ++j) {
                            newPivotRowMatrix[t][j] = add(mOldRowP[j], mul(mul(analysis.coefficient, analysis.left[p]), analysis.right[j]));
                        }
                    }
                }
            }
        } else if (rightOutside) {
            if (analysis.pivotDeterminantFactor != 0) {
                newInverse = inverseAfterShermanMorrison(analysis);

                newPivotColumnMatrix = new long[h][oldRank];
                for (int i = 0; i < h; ++i) {
                    for (int k = 0; k < oldRank; ++k) {
                        newPivotColumnMatrix[i][k] = add(pivotColumnMatrix[i][k], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[k]]));
                    }
                }

                newPivotRowMatrix = new long[oldRank][w];
                for (int k = 0; k < oldRank; ++k) {
                    for (int j = 0; j < w; ++j) {
                        newPivotRowMatrix[k][j] = add(pivotRowMatrix[k][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[k]]), analysis.right[j]));
                    }
                }
            } else {
                int q = analysis.outsideColumn;
                int k = firstNonZero(analysis.a);
                if (k < 0) {
                    throw new AssertionError("singular pivot update without a right null vector");
                }
                long[] wColumn = new long[oldRank];
                for (int i = 0; i < oldRank; ++i) {
                    wColumn[i] = add(
                            pivotRowMatrix[i][q],
                            mul(mul(analysis.coefficient, analysis.left[pivotRows[i]]), analysis.right[q]));
                }
                newInverse = inverseAfterSingularColumnReplacement(
                        inverse, analysis.a, analysis.b, k, wColumn);
                newPivotColumns = pivotColumns.clone();
                newPivotColumns[k] = q;

                newPivotRowMatrix = new long[oldRank][w];
                for (int m = 0; m < oldRank; ++m) {
                    for (int j = 0; j < w; ++j) {
                        newPivotRowMatrix[m][j] = add(pivotRowMatrix[m][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[m]]), analysis.right[j]));
                    }
                }

                long[] x = new long[oldRank]; // inverse * R[:,q]
                for (int i = 0; i < oldRank; ++i) {
                    long value = 0;
                    for (int m = 0; m < oldRank; ++m) {
                        value = add(value, mul(inverse[i][m], pivotRowMatrix[m][q]));
                    }
                    x[i] = value;
                }

                long[] mOldColQ = new long[h];
                for (int i = 0; i < h; ++i) {
                    long val = 0;
                    for (int m = 0; m < oldRank; ++m) {
                        val = add(val, mul(pivotColumnMatrix[i][m], x[m]));
                    }
                    mOldColQ[i] = val;
                }

                newPivotColumnMatrix = new long[h][oldRank];
                for (int i = 0; i < h; ++i) {
                    for (int m = 0; m < oldRank; ++m) {
                        if (m != k) {
                            newPivotColumnMatrix[i][m] = add(pivotColumnMatrix[i][m], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[m]]));
                        } else {
                            newPivotColumnMatrix[i][k] = add(mOldColQ[i], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[q]));
                        }
                    }
                }
            }
        } else {
            if (analysis.pivotDeterminantFactor != 0) {
                newInverse = inverseAfterShermanMorrison(analysis);

                newPivotColumnMatrix = new long[h][oldRank];
                for (int i = 0; i < h; ++i) {
                    for (int k = 0; k < oldRank; ++k) {
                        newPivotColumnMatrix[i][k] = add(pivotColumnMatrix[i][k], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[k]]));
                    }
                }

                newPivotRowMatrix = new long[oldRank][w];
                for (int k = 0; k < oldRank; ++k) {
                    for (int j = 0; j < w; ++j) {
                        newPivotRowMatrix[k][j] = add(pivotRowMatrix[k][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[k]]), analysis.right[j]));
                    }
                }
            } else {
                int t = firstNonZero(analysis.b); // deleted pivot row
                int k = firstNonZero(analysis.a); // deleted pivot column
                if (t < 0 || k < 0) {
                    throw new AssertionError("rank decrease without null vectors");
                }
                newInverse = inverseAfterSingularDeletion(inverse, analysis.a, analysis.b, t, k);
                newPivotRows = removeAt(pivotRows, t);
                newPivotColumns = removeAt(pivotColumns, k);

                long[][] nextC = new long[h][oldRank - 1];
                for (int i = 0; i < h; ++i) {
                    int colIdx = 0;
                    for (int m = 0; m < oldRank; ++m) {
                        if (m == k) continue;
                        long val = add(pivotColumnMatrix[i][m], mul(mul(analysis.coefficient, analysis.left[i]), analysis.right[pivotColumns[m]]));
                        nextC[i][colIdx++] = val;
                    }
                }
                newPivotColumnMatrix = nextC;

                long[][] nextR = new long[oldRank - 1][w];
                int rowIdx = 0;
                for (int m = 0; m < oldRank; ++m) {
                    if (m == t) continue;
                    for (int j = 0; j < w; ++j) {
                        long val = add(pivotRowMatrix[m][j], mul(mul(analysis.coefficient, analysis.left[pivotRows[m]]), analysis.right[j]));
                        nextR[rowIdx][j] = val;
                    }
                    rowIdx++;
                }
                newPivotRowMatrix = nextR;
            }
        }

        pivotRows = newPivotRows;
        pivotColumns = newPivotColumns;
        inverse = newInverse;
        pivotColumnMatrix = newPivotColumnMatrix;
        pivotRowMatrix = newPivotRowMatrix;
        rank = analysis.rankAfter;
    }

    private long[][] inverseAfterShermanMorrison(Analysis analysis) {
        long factor = mul(analysis.coefficient, inv(analysis.pivotDeterminantFactor));
        long[][] result = new long[rank][rank];
        for (int i = 0; i < rank; ++i) {
            for (int j = 0; j < rank; ++j) {
                result[i][j] = sub(inverse[i][j], mul(mul(factor, analysis.a[i]), analysis.b[j]));
            }
        }
        return result;
    }

    /**
     * K' is singular rank r-1 with right null a and left null b.
     * Replace row t by wRow, where b[t] != 0 and wRow*a != 0.
     */
    private long[][] inverseAfterSingularRowReplacement(
            long[][] oldInverse,
            long[] a,
            long[] b,
            int t,
            long[] wRow) {
        int r = oldInverse.length;
        long invBt = inv(b[t]);
        long[][] q = new long[r][r];
        for (int i = 0; i < r; ++i) {
            for (int j = 0; j < r; ++j) {
                q[i][j] = sub(oldInverse[i][j], mul(mul(oldInverse[i][t], b[j]), invBt));
            }
        }
        long[] wQ = new long[r];
        for (int j = 0; j < r; ++j) {
            long value = 0;
            for (int i = 0; i < r; ++i) {
                value = add(value, mul(wRow[i], q[i][j]));
            }
            wQ[j] = value;
        }
        long wA = dot(wRow, a);
        long invWA = inv(wA);
        long[][] result = new long[r][r];
        for (int i = 0; i < r; ++i) {
            long scale = mul(a[i], invWA);
            for (int j = 0; j < r; ++j) {
                long correction = sub(j == t ? 1 % mod : 0, wQ[j]);
                result[i][j] = add(q[i][j], mul(scale, correction));
            }
        }
        return result;
    }

    /**
     * K' is singular rank r-1 with right null a and left null b.
     * Replace column k by wColumn, where a[k] != 0 and b*wColumn != 0.
     */
    private long[][] inverseAfterSingularColumnReplacement(
            long[][] oldInverse,
            long[] a,
            long[] b,
            int k,
            long[] wColumn) {
        int r = oldInverse.length;
        long d = dot(b, wColumn);
        long invD = inv(d);

        long[] aW = multiply(oldInverse, wColumn);
        long[][] q = new long[r][r];
        for (int i = 0; i < r; ++i) {
            for (int j = 0; j < r; ++j) {
                q[i][j] = sub(oldInverse[i][j], mul(mul(aW[i], b[j]), invD));
            }
        }

        long invAk = inv(a[k]);
        long[][] result = new long[r][r];
        for (int i = 0; i < r; ++i) {
            if (i == k) {
                for (int j = 0; j < r; ++j) {
                    result[i][j] = mul(b[j], invD);
                }
            } else {
                long scale = mul(a[i], invAk);
                for (int j = 0; j < r; ++j) {
                    result[i][j] = sub(q[i][j], mul(scale, q[k][j]));
                }
            }
        }
        return result;
    }

    /**
     * K' is singular rank r-1 with right null a and left null b.
     * Delete row t and column k; b[t] and a[k] are nonzero.
     */
    private long[][] inverseAfterSingularDeletion(
            long[][] oldInverse,
            long[] a,
            long[] b,
            int t,
            int k) {
        int r = oldInverse.length;
        long[][] result = new long[r - 1][r - 1];
        long invBt = inv(b[t]);
        long invAk = inv(a[k]);

        int oi = 0;
        for (int ci = 0; ci < r; ++ci) {
            if (ci == k) {
                continue;
            }
            int oj = 0;
            long aScale = mul(a[ci], invAk);
            for (int rj = 0; rj < r; ++rj) {
                if (rj == t) {
                    continue;
                }
                long bScale = mul(b[rj], invBt);
                long first = sub(oldInverse[ci][rj], mul(oldInverse[ci][t], bScale));
                long second = sub(oldInverse[k][rj], mul(oldInverse[k][t], bScale));
                result[oi][oj] = sub(first, mul(aScale, second));
                ++oj;
            }
            ++oi;
        }
        return result;
    }

    // ---------------------------------------------------------------------
    // Finite-field / matrix helpers
    // ---------------------------------------------------------------------

    private long normalize(long x) {
        x %= mod;
        if (x < 0) {
            x += mod;
        }
        return x;
    }

    private long add(long a, long b) {
        long x = a + b;
        if (x >= mod) {
            x -= mod;
        }
        return x;
    }

    private long sub(long a, long b) {
        long x = a - b;
        if (x < 0) {
            x += mod;
        }
        return x;
    }

    private long neg(long a) {
        return a == 0 ? 0 : mod - a;
    }

    private long mul(long a, long b) {
        return (a * b) % mod;
    }

    private long inv(long a) {
        if (a == 0) {
            throw new ArithmeticException("division by zero in F_p");
        }
        return pow(a, mod - 2);
    }

    private long pow(long a, long e) {
        long result = 1 % mod;
        long base = a;
        while (e > 0) {
            if ((e & 1L) != 0) {
                result = mul(result, base);
            }
            base = mul(base, base);
            e >>>= 1;
        }
        return result;
    }

    private long[] normalizedVector(long[] vector, int expectedLength, String name) {
        Objects.requireNonNull(vector, name);
        if (vector.length != expectedLength) {
            throw new IllegalArgumentException(name + ".length != " + expectedLength);
        }
        long[] result = new long[expectedLength];
        for (int i = 0; i < expectedLength; ++i) {
            result[i] = normalize(vector[i]);
        }
        return result;
    }

    private long[] multiply(long[][] a, long[] x) {
        int n = a.length;
        long[] result = new long[n];
        for (int i = 0; i < n; ++i) {
            long value = 0;
            for (int j = 0; j < x.length; ++j) {
                value = add(value, mul(a[i][j], x[j]));
            }
            result[i] = value;
        }
        return result;
    }

    private long dot(long[] a, long[] b) {
        long value = 0;
        for (int i = 0; i < a.length; ++i) {
            value = add(value, mul(a[i], b[i]));
        }
        return value;
    }

    private int firstNonZero(long[] a) {
        for (int i = 0; i < a.length; ++i) {
            if (a[i] != 0) {
                return i;
            }
        }
        return -1;
    }

    private static int[] removeAt(int[] a, int index) {
        int[] result = new int[a.length - 1];
        System.arraycopy(a, 0, result, 0, index);
        System.arraycopy(a, index + 1, result, index, a.length - index - 1);
        return result;
    }

    private long[][] invertSquare(long[][] a) {
        int n = a.length;
        long[][] aug = new long[n][2 * n];
        for (int i = 0; i < n; ++i) {
            if (a[i].length != n) {
                throw new IllegalArgumentException("matrix is not square");
            }
            for (int j = 0; j < n; ++j) {
                aug[i][j] = a[i][j];
            }
            aug[i][n + i] = 1 % mod;
        }

        for (int col = 0; col < n; ++col) {
            int pivot = -1;
            for (int row = col; row < n; ++row) {
                if (aug[row][col] != 0) {
                    pivot = row;
                    break;
                }
            }
            if (pivot < 0) {
                throw new ArithmeticException("pivot minor is singular");
            }
            if (pivot != col) {
                long[] tmp = aug[pivot];
                aug[pivot] = aug[col];
                aug[col] = tmp;
            }

            long invPivot = inv(aug[col][col]);
            for (int j = col; j < 2 * n; ++j) {
                aug[col][j] = mul(aug[col][j], invPivot);
            }
            for (int row = 0; row < n; ++row) {
                if (row == col) {
                    continue;
                }
                long factor = aug[row][col];
                if (factor == 0) {
                    continue;
                }
                for (int j = col; j < 2 * n; ++j) {
                    aug[row][j] = sub(aug[row][j], mul(factor, aug[col][j]));
                }
            }
        }

        long[][] result = new long[n][n];
        for (int i = 0; i < n; ++i) {
            System.arraycopy(aug[i], n, result[i], 0, n);
        }
        return result;
    }
}
