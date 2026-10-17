package library.util.matrix;

/**
 * 全要素を 2 次元配列として保持する Monge 行列。
 *
 * // 未テスト
 */
public final class DenseMongeLong implements MongeLong {
    private final long[][] grid;

    /**
     * 指定された 2 次元配列を複製して DenseMongeLong を構築する。
     *
     * @param grid 2 次元配列
     * @throws NullPointerException grid が null の場合
     * @complexity O(rows * cols)
     */
    public DenseMongeLong(long[][] grid) {
        if (grid == null) throw new NullPointerException("grid");
        this.grid = new long[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == null) throw new NullPointerException("grid[" + i + "]");
            this.grid[i] = grid[i].clone();
        }
    }

    /**
     * 信頼された 2 次元配列から直接 DenseMongeLong を構築する内部コンストラクタ。
     *
     * @param grid 2 次元配列
     * @param trusted 参照をそのまま保持する場合 true
     * @complexity O(1)
     */
    DenseMongeLong(long[][] grid, boolean trusted) {
        this.grid = grid;
    }

    @Override
    public int rows() {
        return grid.length;
    }

    @Override
    public int cols() {
        return grid.length == 0 ? 0 : grid[0].length;
    }

    @Override
    public long get(int row, int col) {
        if (row < 0 || row >= rows() || col < 0 || col >= cols()) {
            throw new IndexOutOfBoundsException("row=" + row + ", col=" + col);
        }
        return grid[row][col];
    }
}
