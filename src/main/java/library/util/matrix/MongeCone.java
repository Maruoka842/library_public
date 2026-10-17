package library.util.matrix;

import java.util.Arrays;
import library.util.collections.IntArrayList;

/**
 * 非負 Monge cone の生成元を加えて行列を構築する mutable object。
 *
 * <p>生成元は 1 行、1 列、NE corner、SW corner。全 weight は非負。
 * 各操作は内部で row/column potential + nonnegative core atom に即座に正規化される。
 *
 * // 未テスト
 */
public final class MongeCone {
    private final int rows;
    private final int cols;
    private final long[] rowPotential;
    private final long[] colPotential;
    private final IntArrayList atomRowList = new IntArrayList();
    private final IntArrayList atomColList = new IntArrayList();
    private long[] atomWeightList = new long[16];
    private int atomSize = 0;

    /**
     * 指定された行数・列数の MongeCone を構築する。
     *
     * @param rows 行数
     * @param cols 列数
     * @complexity O(rows + cols)
     */
    public MongeCone(int rows, int cols) {
        if (rows < 0 || cols < 0) {
            throw new IllegalArgumentException("Negative dimensions: " + rows + "x" + cols);
        }
        this.rows = rows;
        this.cols = cols;
        this.rowPotential = new long[rows];
        this.colPotential = new long[cols];
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public int atomCount() { return atomSize; }

    private static void checkWeight(long weight) {
        if (weight < 0) throw new IllegalArgumentException("weight < 0: " + weight);
    }

    private static void checkBoundary(int bound, int max, String name) {
        if (bound < 0 || bound > max) throw new IndexOutOfBoundsException(name + "=" + bound + " out of bounds [0, " + max + "]");
    }

    public void addRow(int row, long weight) {
        checkWeight(weight);
        if (row < 0 || row >= rows) throw new IndexOutOfBoundsException("row=" + row);
        rowPotential[row] += weight;
    }

    public void addColumn(int col, long weight) {
        checkWeight(weight);
        if (col < 0 || col >= cols) throw new IndexOutOfBoundsException("col=" + col);
        colPotential[col] += weight;
    }


    private void addDensity(int r, int c, long weight) {
        if (r < 0 || r >= rows - 1) throw new IndexOutOfBoundsException("r=" + r);
        if (c < 0 || c >= cols - 1) throw new IndexOutOfBoundsException("c=" + c);
        if (weight == 0) return;
        atomRowList.add(r);
        atomColList.add(c);
        if (atomSize == atomWeightList.length) {
            atomWeightList = Arrays.copyOf(atomWeightList, atomWeightList.length * 2);
        }
        atomWeightList[atomSize++] = weight;
    }

    /** {@code [0,rowEnd) x [colBegin,cols)} に加える。 */
    public void addNorthEast(int rowEnd, int colBegin, long weight) {
        checkWeight(weight);
        checkBoundary(rowEnd, rows, "rowEnd");
        checkBoundary(colBegin, cols, "colBegin");
        if (weight == 0 || rowEnd == 0 || colBegin == cols) return;
        if (rowEnd == rows) {
            for (int j = colBegin; j < cols; j++) colPotential[j] += weight;
        } else if (colBegin == 0) {
            for (int i = 0; i < rowEnd; i++) rowPotential[i] += weight;
        } else {
            for (int j = colBegin; j < cols; j++) colPotential[j] += weight;
            addDensity(rowEnd - 1, colBegin - 1, weight);
        }
    }

    /** {@code [rowBegin,rows) x [0,colEnd)} に加える。 */
    public void addSouthWest(int rowBegin, int colEnd, long weight) {
        checkWeight(weight);
        checkBoundary(rowBegin, rows, "rowBegin");
        checkBoundary(colEnd, cols, "colEnd");
        if (weight == 0 || rowBegin == rows || colEnd == 0) return;
        if (rowBegin == 0) {
            for (int j = 0; j < colEnd; j++) colPotential[j] += weight;
        } else if (colEnd == cols) {
            for (int i = rowBegin; i < rows; i++) rowPotential[i] += weight;
        } else {
            for (int i = rowBegin; i < rows; i++) rowPotential[i] += weight;
            addDensity(rowBegin - 1, colEnd - 1, weight);
        }
    }

    private MongeCoreData snapshot() {
        int[] rowsArr = atomRowList.toArray();
        int[] colsArr = atomColList.toArray();
        long[] weightsArr = Arrays.copyOf(atomWeightList, atomSize);
        return new MongeCoreData(rows, cols, rowPotential.clone(), colPotential.clone(),
                rowsArr, colsArr, weightsArr);
    }

    /** raw core 表現。ランダムアクセス用索引は作らない。 */
    public MongeCoreLong buildCore() { return new MongeCoreLong(snapshot()); }

    /** 全要素を保持する dense 表現。 */
    public DenseMongeLong buildDense() { return buildCore().dense(); }

    /** raw core に Weighted Wavelet Matrix の prefix-sum index を付ける。 */
    public IndexedMongeLong buildIndexed() { return buildCore().indexed(); }
}
