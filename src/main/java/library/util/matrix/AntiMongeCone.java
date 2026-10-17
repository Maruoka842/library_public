package library.util.matrix;

import java.util.Arrays;
import library.util.collections.IntArrayList;

/**
 * 非負 anti-Monge cone の生成元を加えて行列を構築する mutable object。
 *
 * <p>生成元は 1 行、1 列、NW corner、SE corner。全 weight は非負。
 *
 * // 未テスト
 */
public final class AntiMongeCone {
    private final int rows;
    private final int cols;
    private final long[] rowPotential;
    private final long[] colPotential;
    private final IntArrayList atomRowList = new IntArrayList();
    private final IntArrayList atomColList = new IntArrayList();
    private long[] atomWeightList = new long[16];
    private int atomSize = 0;

    /**
     * 指定された行数・列数の AntiMongeCone を構築する。
     *
     * @param rows 行数
     * @param cols 列数
     * @complexity O(rows + cols)
     */
    public AntiMongeCone(int rows, int cols) {
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

    /** {@code [0,rowEnd) x [0,colEnd)} に加える。 */
    public void addNorthWest(int rowEnd, int colEnd, long weight) {
        checkWeight(weight);
        checkBoundary(rowEnd, rows, "rowEnd");
        checkBoundary(colEnd, cols, "colEnd");
        if (weight == 0 || rowEnd == 0 || colEnd == 0) return;
        if (rowEnd == rows) {
            for (int j = 0; j < colEnd; j++) colPotential[j] += weight;
        } else if (colEnd == cols) {
            for (int i = 0; i < rowEnd; i++) rowPotential[i] += weight;
        } else {
            for (int i = rowEnd; i < rows; i++) rowPotential[i] -= weight;
            for (int j = 0; j < colEnd; j++) colPotential[j] += weight;
            addDensity(rowEnd - 1, colEnd - 1, weight);
        }
    }

    /** {@code [rowBegin,rows) x [colBegin,cols)} に加える。 */
    public void addSouthEast(int rowBegin, int colBegin, long weight) {
        checkWeight(weight);
        checkBoundary(rowBegin, rows, "rowBegin");
        checkBoundary(colBegin, cols, "colBegin");
        if (weight == 0 || rowBegin == rows || colBegin == cols) return;
        if (rowBegin == 0) {
            for (int j = colBegin; j < cols; j++) colPotential[j] += weight;
        } else if (colBegin == 0) {
            for (int i = rowBegin; i < rows; i++) rowPotential[i] += weight;
        } else {
            addDensity(rowBegin - 1, colBegin - 1, weight);
        }
    }

    private MongeCoreData snapshot() {
        int[] rowsArr = atomRowList.toArray();
        int[] colsArr = atomColList.toArray();
        long[] weightsArr = Arrays.copyOf(atomWeightList, atomSize);
        return new MongeCoreData(rows, cols, rowPotential.clone(), colPotential.clone(),
                rowsArr, colsArr, weightsArr);
    }

    public AntiMongeCoreLong buildCore() { return new AntiMongeCoreLong(snapshot()); }
    public DenseAntiMongeLong buildDense() { return buildCore().dense(); }
    public IndexedAntiMongeLong buildIndexed() { return buildCore().indexed(); }
}
