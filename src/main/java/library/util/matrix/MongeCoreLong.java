package library.util.matrix;

/**
 * Monge 行列を row/column potential と疎な非負 core atom で表す immutable な標準形。
 *
 * <p>本クラスが表す行列は
 *
 * <pre>
 * A[i][j] = rowPotential[i] + columnPotential[j]
 *          - sum(weight[e] | atomRow[e] &lt; i, atomColumn[e] &lt; j).
 * </pre>
 *
 * <p>各 atom の重みは非負である。atom は density
 *
 * <pre>
 * A[i][j + 1] + A[i + 1][j] - A[i][j] - A[i + 1][j + 1]
 * </pre>
 *
 * の非零項に対応する。
 *
 * <p>このクラス自身は {@link LongMatrix} ではない。ランダムな要素取得用の索引を持たず、
 * core-sparse なアルゴリズムが atom を直接走査するための軽量表現である。
 * {@link #indexed()} で prefix-sum query 用の索引を付けられる。
 *
 * @complexity space O(rows + cols + K)
 * // 未テスト
 */
public final class MongeCoreLong {
    final MongeCoreData data;

    /**
     * ポテンシャルおよび atom 情報から MongeCoreLong を構築する。
     *
     * @param rowPotential 行ポテンシャル
     * @param columnPotential 列ポテンシャル
     * @param atomRow atom 行インデックス
     * @param atomColumn atom 列インデックス
     * @param atomWeight atom 重み
     * @complexity O(rows + cols + K)
     */
    public MongeCoreLong(long[] rowPotential, long[] columnPotential,
            int[] atomRow, int[] atomColumn, long[] atomWeight) {
        if (rowPotential == null || columnPotential == null || atomRow == null
                || atomColumn == null || atomWeight == null) throw new NullPointerException();
        data = new MongeCoreData(rowPotential.length, columnPotential.length,
                rowPotential, columnPotential, atomRow, atomColumn, atomWeight);
    }

    MongeCoreLong(MongeCoreData data) { this.data = data; }

    public int rows() { return data.rows; }
    public int cols() { return data.cols; }
    public int atomCount() { return data.atomCount(); }
    public long rowPotential(int row) { return data.rowPotential[row]; }
    public long columnPotential(int col) { return data.colPotential[col]; }
    public int atomRow(int atom) { return data.atomRow[atom]; }
    public int atomColumn(int atom) { return data.atomCol[atom]; }
    public long atomWeight(int atom) { return data.atomWeight[atom]; }

    /** density-row {@code row} にある atom の先頭 index。 */
    public int atomBegin(int row) {
        if (row < 0 || row >= rows() - 1) throw new IndexOutOfBoundsException("row=" + row);
        return data.rowBegin[row];
    }

    /** density-row {@code row} にある atom の末尾 index。 */
    public int atomEnd(int row) {
        if (row < 0 || row >= rows() - 1) throw new IndexOutOfBoundsException("row=" + row);
        return data.rowBegin[row + 1];
    }

    /** 全要素を materialize する。 */
    public DenseMongeLong dense() {
        return new DenseMongeLong(data.dense(-1), true);
    }

    /** Weighted Wavelet Matrix により 2 次元 prefix sum を索引化する。 */
    public IndexedMongeLong indexed() {
        return new IndexedMongeLong(this);
    }
}
