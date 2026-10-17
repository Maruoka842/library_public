package library.util.linalg;

import java.util.Arrays;
import library.util.MathUtils;

/**
 * F_p 上のベクトル空間における、接頭辞（Prefix）ごとの貪欲線形基底（Greedy Linear Basis）を管理するデータ構造。
 * セグメント木を使用せずに、任意の区間 [L, R) に対して線形基底のクエリを O(d^2) で処理できる。
 * ここで、d はベクトルの次元数、p = mod は素数法を表す。
 */
public class PrefixLinearBasisOverFp {
    /** 接頭辞ごとの基底ベクトル。第一次元が接頭辞の長さ、第二次元がピボット位置、第三次元がベクトルの各次元成分。 */
    private final long[][][] basis;
    /** 接頭辞ごとの基底ベクトルの由来インデックス。第一次元が接頭辞の長さ、第二次元がピボット位置。 */
    private final int[][] pos;
    /** 元の配列の長さ N。 */
    private final int n;
    /** ベクトルの次元数 d。 */
    private final int d;
    /** 有限体の法 mod（素数）。 */
    private final long mod;

    /**
     * 与えられたベクトル配列 a から、各接頭辞（Prefix）における F_p 上の貪欲線形基底（Greedy Linear Basis）を構築する。
     * ここで、ベクトル空間の次元を d とし、法を mod としたとき、
     * 任意の区間 [L, R) に対して、その区間に含まれる要素から生成される線形基底を O(d^2) で抽出可能にする。
     * 未テスト。
     *
     * @param a 構築元のベクトル配列。a[i] は長さ d のベクトルを表す。空配列であってはならない。
     * @param mod 有限体 F_p の素数法。
     * @throws IllegalArgumentException a が null の場合、a が空配列の場合、a の要素に null がある場合、a の要素の長さが不一致の場合、または mod が 1 以下の場合。
     *
     * 計算量: O(N d^2)。
     */
    public PrefixLinearBasisOverFp(long[][] a, long mod) {
        if (a == null) {
            throw new IllegalArgumentException("Input array cannot be null");
        }
        if (a.length == 0) {
            throw new IllegalArgumentException("Input array cannot be empty");
        }
        if (a[0] == null) {
            throw new IllegalArgumentException("Vector in array cannot be null");
        }
        int derivedD = a[0].length;
        if (derivedD <= 0) {
            throw new IllegalArgumentException("Dimension d must be positive");
        }
        if (mod <= 1) {
            throw new IllegalArgumentException("Modulus must be greater than 1");
        }
        this.n = a.length;
        this.d = derivedD;
        this.mod = mod;
        this.basis = new long[n + 1][d][];
        this.pos = new int[n + 1][d];

        for (int i = 0; i <= n; i++) {
            Arrays.fill(pos[i], -1);
        }

        for (int i = 0; i < n; i++) {
            if (a[i] == null) {
                throw new IllegalArgumentException("Vector in array cannot be null");
            }
            if (a[i].length != d) {
                throw new IllegalArgumentException("All vectors must have the same dimension d");
            }

            // 前のステップの状態をコピー（参照のコピー）
            System.arraycopy(basis[i], 0, basis[i + 1], 0, d);
            System.arraycopy(pos[i], 0, pos[i + 1], 0, d);

            long[] val = new long[d];
            for (int k = 0; k < d; k++) {
                val[k] = (a[i][k] % mod + mod) % mod;
            }
            int p = i;
            for (int b = 0; b < d; b++) {
                long valB = val[b];
                if (valB != 0) {
                    if (basis[i + 1][b] == null) {
                        long inv = MathUtils.modInv(valB, mod);
                        long[] normalized = new long[d];
                        for (int k = b; k < d; k++) {
                            normalized[k] = val[k] * inv % mod;
                        }
                        basis[i + 1][b] = normalized;
                        pos[i + 1][b] = p;
                        break;
                    }
                    if (p > pos[i + 1][b]) {
                        long inv = MathUtils.modInv(valB, mod);
                        long[] normalized = new long[d];
                        for (int k = b; k < d; k++) {
                            normalized[k] = val[k] * inv % mod;
                        }
                        val = normalized;
                        valB = 1;

                        int tmpP = p;
                        p = pos[i + 1][b];
                        pos[i + 1][b] = tmpP;

                        long[] tmpB = basis[i + 1][b];
                        basis[i + 1][b] = val;
                        val = tmpB;
                    }
                    long factor = valB;
                    long[] bVec = basis[i + 1][b];
                    long[] nextVal = new long[d];
                    for (int k = b; k < d; k++) {
                        nextVal[k] = (val[k] - factor * bVec[k]) % mod;
                        if (nextVal[k] < 0) {
                            nextVal[k] += mod;
                        }
                    }
                    val = nextVal;
                }
            }
        }
    }

    /**
     * 元の配列の長さ N を取得する。
     * 未テスト。
     *
     * @return 元の配列 of 長さ N。
     *
     * 計算量: O(1)。
     */
    public int size() {
        return n;
    }

    /**
     * 区間 [l, r) における線形基底ベクトルをピボット位置の昇順に取得する。
     * 未テスト。
     *
     * @param l 開始インデックス（inclusive）。
     * @param r 終了インデックス（exclusive）。
     * @return 区間 [l, r) の線形基底ベクトルを格納した2次元配列。
     *
     * 計算量: O(d^2)。
     */
    public long[][] basis(int l, int r) {
        if (l < 0 || r > n || l >= r) {
            return new long[0][d];
        }
        int count = 0;
        for (int b = 0; b < d; b++) {
            if (basis[r][b] != null && pos[r][b] >= l) {
                count++;
            }
        }
        long[][] res = new long[count][d];
        int idx = 0;
        for (int b = 0; b < d; b++) {
            if (basis[r][b] != null && pos[r][b] >= l) {
                System.arraycopy(basis[r][b], 0, res[idx++], 0, d);
            }
        }
        return res;
    }

    /**
     * 区間 [l, r) における線形基底の次元（基底ベクトルの個数、ランク）を取得する。
     * 未テスト。
     *
     * @param l 開始インデックス（inclusive）。
     * @param r 終了インデックス（exclusive）。
     * @return 区間 [l, r) の線形基底の次元。
     *
     * 計算量: O(d)。
     */
    public int rank(int l, int r) {
        if (l < 0 || r > n || l >= r) {
            return 0;
        }
        int count = 0;
        for (int b = 0; b < d; b++) {
            if (basis[r][b] != null && pos[r][b] >= l) {
                count++;
            }
        }
        return count;
    }

    /**
     * 区間 [l, r) の線形基底を用いて、ベクトル x をできるだけ消去（簡約）した結果を返す。
     * 各ピボット位置 b において、x[b] を 0 にするように簡約を行う。
     * 未テスト。
     *
     * @param l 開始インデックス（inclusive）。
     * @param r 終了インデックス（exclusive）。
     * @param x 簡約対象 of ベクトル。
     * @return 簡約された新しいベクトル。
     * @throws IllegalArgumentException x が null の場合、または x の長さが d と異なる場合。
     *
     * 計算量: O(d^2)。
     */
    public long[] reduce(int l, int r, long[] x) {
        if (x == null || x.length != d) {
            throw new IllegalArgumentException("Vector x must be of length " + d);
        }
        long[] res = new long[d];
        for (int k = 0; k < d; k++) {
            res[k] = (x[k] % mod + mod) % mod;
        }
        if (l < 0 || r > n || l >= r) {
            return res;
        }
        for (int b = 0; b < d; b++) {
            long valB = res[b];
            if (valB != 0) {
                if (basis[r][b] != null && pos[r][b] >= l) {
                    long factor = valB;
                    long[] bVec = basis[r][b];
                    for (int k = b; k < d; k++) {
                        res[k] = (res[k] - factor * bVec[k]) % mod;
                        if (res[k] < 0) {
                            res[k] += mod;
                        }
                    }
                }
            }
        }
        return res;
    }

    /**
     * 与えられたベクトル x が区間 [l, r) の要素の線形包（Linear Span）に含まれるか判定する。
     * 未テスト。
     *
     * @param l 開始インデックス（inclusive）。
     * @param r 終了インデックス（exclusive）。
     * @param x 判定対象 of ベクトル。
     * @return x が区間の線形包に含まれる場合は true、そうでない場合は false。
     * @throws IllegalArgumentException x が null の場合、または x の長さが d と異なる場合。
     *
     * 計算量: O(d^2)。
     */
    public boolean contains(int l, int r, long[] x) {
        long[] red = reduce(l, r, x);
        for (int k = 0; k < d; k++) {
            if (red[k] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 区間 [l, r) の線形基底の線形結合によって表されるベクトルのうち、辞書順（成分 0 から順に比較）で最大のベクトルを返す。
     * 未テスト。
     *
     * @param l 開始インデックス（inclusive）。
     * @param r 終了インデックス（exclusive）。
     * @return 辞書順で最大のベクトル。
     *
     * 計算量: O(d^2)。
     */
    public long[] maxVector(int l, int r) {
        long[] res = new long[d];
        if (l < 0 || r > n || l >= r) {
            return res;
        }
        for (int b = 0; b < d; b++) {
            if (basis[r][b] != null && pos[r][b] >= l) {
                long current = res[b];
                long target = mod - 1;
                long coeff = (target - current + mod) % mod;
                if (coeff != 0) {
                    long[] bVec = basis[r][b];
                    for (int k = b; k < d; k++) {
                        res[k] = (res[k] + coeff * bVec[k]) % mod;
                    }
                }
            }
        }
        return res;
    }

    /**
     * 保持している接頭辞ごとの線形基底の全状態を標準出力にダンプ（表示）する。
     * デバッグ用途に利用される。
     * 未テスト。
     *
     * 計算量: O(N d^2)。
     */
    public void dump() {
        System.out.println("PrefixLinearBasisOverFp Dump:");
        for (int i = 0; i <= n; i++) {
            System.out.print("Prefix " + i + ": ");
            boolean first = true;
            for (int b = 0; b < d; b++) {
                if (basis[i][b] != null) {
                    if (!first) {
                        System.out.print(", ");
                    }
                    System.out.print("[pivot " + b + ": val=" + Arrays.toString(basis[i][b]) + " (from idx " + pos[i][b] + ")]");
                    first = false;
                }
            }
            if (first) {
                System.out.print("empty");
            }
            System.out.println();
        }
    }
}
