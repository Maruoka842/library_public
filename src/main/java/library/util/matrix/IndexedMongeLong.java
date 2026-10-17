package library.util.matrix;

/**
 * {@link MongeCoreLong} の core prefix sum を Weighted Wavelet Matrix で索引化した Monge 行列。
 *
 * <p>core atom 自体も {@link #core()} に残るため、ランダムな {@link #get(int, int)} と
 * core-sparse アルゴリズムの両方に利用できる。
 *
 * @complexity build O(K log cols), get O(log cols), space O(rows + cols + K log cols)
 * // 未テスト
 */
public final class IndexedMongeLong implements MongeLong {
    private final MongeCoreLong core;
    private final WeightedWaveletMatrixLong density;

    /**
     * 指定された core から IndexedMongeLong を構築する。
     *
     * @param core MongeCoreLong インスタンス
     * @complexity O(K log cols)
     */
    public IndexedMongeLong(MongeCoreLong core) {
        if (core == null) throw new NullPointerException("core");
        this.core = core;
        MongeCoreData d = core.data;
        density = new WeightedWaveletMatrixLong(d.atomCol, d.atomWeight, Math.max(1, d.cols - 1));
    }

    public MongeCoreLong core() { return core; }
    @Override public int rows() { return core.rows(); }
    @Override public int cols() { return core.cols(); }

    @Override
    public long get(int row, int col) {
        if (row < 0 || row >= rows() || col < 0 || col >= cols()) throw new IndexOutOfBoundsException();
        MongeCoreData d = core.data;
        long s = density.sumLessThan(0, d.rowBegin[row], col);
        return d.rowPotential[row] + d.colPotential[col] - s;
    }
}
