package library.util.linalg;

/**
 * 過去との互換性を保つための facade クラス。
 *
 * <p>実際の処理は {@link DynamicMatrixRankDecompositionFp} に委譲される。</p>
 */
public final class DynamicMatrixRankFp {
    private final DynamicMatrixRankDecompositionFp decomposition;

    /**
     * 指定された行列と素数法を用いて動的ランク計算オブジェクトを構築する。
     * 未テスト。
     *
     * @param matrix 構築対象の2次元配列
     * @param mod 有限体 F_p の素数法
     *
     * 計算量: O(H * W * min(H, W))。ただし H = matrix.length、W = matrix[0].length。
     */
    // 未テスト
    public DynamicMatrixRankFp(long[][] matrix, long mod) {
        decomposition = new DynamicMatrixRankDecompositionFp(matrix, mod);
    }

    /**
     * 指定された行数、列数、行列、および素数法を用いて動的ランク計算オブジェクトを構築する。
     * 未テスト。
     *
     * @param rows 行数 H
     * @param columns 列数 W
     * @param matrix 構築対象の2次元配列
     * @param mod 有限体 F_p の素数法
     *
     * 計算量: O(H * W * min(H, W))。ただし H = rows、W = columns。
     */
    // 未テスト
    public DynamicMatrixRankFp(int rows, int columns, long[][] matrix, long mod) {
        decomposition = new DynamicMatrixRankDecompositionFp(rows, columns, matrix, mod);
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
        return decomposition.rows();
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
        return decomposition.columns();
    }

    /**
     * 現在の行列のランクを取得する。
     * 未テスト。
     *
     * @return ランク
     *
     * 計算量: O(1)。
     */
    // 未テスト
    public int rank() {
        return decomposition.rank();
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
     * 計算量: O(H * W)。
     */
    // 未テスト
    public int addOuterProduct(long[] left, long[] right, long coefficient) {
        return decomposition.addOuterProduct(left, right, coefficient);
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
     * 計算量: ランクが変化しない（適用成功）場合は O(H * W)。ランクが変化する（適用失敗）場合は O(H * r + W * r + r^2)。
     */
    // 未テスト
    public boolean tryAddOuterProductPreservingRank(long[] left, long[] right, long coefficient) {
        return decomposition.tryAddOuterProductPreservingRank(left, right, coefficient);
    }
}
