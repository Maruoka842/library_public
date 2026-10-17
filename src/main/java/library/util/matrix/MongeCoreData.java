package library.util.matrix;

import java.util.Arrays;

/**
 * Monge 行列の core 表現データを管理する内部データ構造。
 *
 * // 未テスト
 */
final class MongeCoreData {
    final int rows;
    final int cols;
    final long[] rowPotential;
    final long[] colPotential;
    final int[] atomRow;
    final int[] atomCol;
    final long[] atomWeight;
    final int[] rowBegin;

    /**
     * 指定されたポテンシャルと atom 配列から MongeCoreData を構築する。
     *
     * @param rows 行数
     * @param cols 列数
     * @param rowPotential 行ポテンシャル
     * @param colPotential 列ポテンシャル
     * @param rawAtomRow atom 行インデックス
     * @param rawAtomCol atom 列インデックス
     * @param rawAtomWeight atom 重み
     * @complexity O(rows + cols + K)
     */
    MongeCoreData(int rows, int cols, long[] rowPotential, long[] colPotential,
            int[] rawAtomRow, int[] rawAtomCol, long[] rawAtomWeight) {
        if (rowPotential == null || colPotential == null || rawAtomRow == null
                || rawAtomCol == null || rawAtomWeight == null) {
            throw new NullPointerException();
        }
        if (rowPotential.length != rows || colPotential.length != cols) {
            throw new IllegalArgumentException("Dimension mismatch");
        }
        int k = rawAtomRow.length;
        if (rawAtomCol.length != k || rawAtomWeight.length != k) {
            throw new IllegalArgumentException("Atom array length mismatch");
        }
        this.rows = rows;
        this.cols = cols;
        this.rowPotential = rowPotential;
        this.colPotential = colPotential;

        for (int i = 0; i < k; i++) {
            if (rawAtomRow[i] < 0 || rawAtomRow[i] >= rows - 1) {
                throw new IndexOutOfBoundsException("atomRow[" + i + "]=" + rawAtomRow[i]);
            }
            if (rawAtomCol[i] < 0 || rawAtomCol[i] >= cols - 1) {
                throw new IndexOutOfBoundsException("atomCol[" + i + "]=" + rawAtomCol[i]);
            }
            if (rawAtomWeight[i] < 0) {
                throw new IllegalArgumentException("atomWeight[" + i + "] < 0");
            }
        }

        this.rowBegin = new int[Math.max(1, rows)];
        int[] counts = new int[Math.max(1, rows)];
        for (int i = 0; i < k; i++) {
            counts[rawAtomRow[i]]++;
        }
        for (int r = 0; r < rows - 1; r++) {
            rowBegin[r + 1] = rowBegin[r] + counts[r];
        }

        this.atomRow = new int[k];
        this.atomCol = new int[k];
        this.atomWeight = new long[k];

        int[] offset = rowBegin.clone();
        for (int i = 0; i < k; i++) {
            int r = rawAtomRow[i];
            int dest = offset[r]++;
            this.atomRow[dest] = rawAtomRow[i];
            this.atomCol[dest] = rawAtomCol[i];
            this.atomWeight[dest] = rawAtomWeight[i];
        }
    }

    /**
     * 保持している atom 数を返す。
     *
     * @return atom 数
     * @complexity O(1)
     */
    int atomCount() {
        return atomRow.length;
    }

    /**
     * 行列の全要素を materialize する。
     *
     * @param sign Monge の場合 -1、Anti-Monge の場合 +1
     * @return 2 次元配列
     * @complexity O(rows * cols + K)
     */
    long[][] dense(int sign) {
        long[][] grid = new long[rows][cols];
        if (rows == 0 || cols == 0) return grid;
        for (int j = 0; j < cols; j++) {
            grid[0][j] = rowPotential[0] + colPotential[j];
        }
        long[] diff = new long[cols];
        long[] cumSum = new long[cols];
        for (int i = 0; i < rows - 1; i++) {
            for (int e = rowBegin[i]; e < rowBegin[i + 1]; e++) {
                int c = atomCol[e];
                long w = atomWeight[e];
                if (c + 1 < cols) {
                    diff[c + 1] += sign * w;
                }
            }
            long run = 0;
            for (int j = 0; j < cols; j++) {
                run += diff[j];
                cumSum[j] += run;
            }
            Arrays.fill(diff, 0L);
            for (int j = 0; j < cols; j++) {
                grid[i + 1][j] = rowPotential[i + 1] + colPotential[j] + cumSum[j];
            }
        }
        return grid;
    }
}
