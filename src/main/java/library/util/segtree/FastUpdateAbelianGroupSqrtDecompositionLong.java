package library.util.segtree;

import java.util.Arrays;

import library.util.algebra.strategy.longs.LongAbelianGroupStrategy;

/**
 * 可換群 G 上の列 A = (a_0, a_1, ..., a_(n-1)) を平方分割で管理する。
 * {@code fold(l, r)} は a_l * a_(l+1) * ... * a_(r-1) を返す。
 */
public class FastUpdateAbelianGroupSqrtDecompositionLong {
    /** 管理する列の長さ。 */
    private final int n;

    /** 1 ブロックに含まれる要素数 B。 */
    private final int blockSize;

    /** blockCount = ceil(n / B)。 */
    private final int blockCount;

    /** 可換群 G の積、単位元、逆元を与える戦略。 */
    private final LongAbelianGroupStrategy strategy;

    /** values[i] = a_i。 */
    private final long[] values;

    /** blockProducts[b] = product_(i=b * B)^(min(n, (b + 1) * B) - 1) a_i。ただし B = blockSize。 */
    private final long[] blockProducts;

    /**
     * 長さ {@code n} の定数列 (e, e, ..., e) を構築する。e は G の単位元である。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @param strategy 可換群 G の演算
     * @throws IllegalArgumentException {@code n < 0} の場合
     */
    public FastUpdateAbelianGroupSqrtDecompositionLong(int n, LongAbelianGroupStrategy strategy) {
        if (n < 0) throw new IllegalArgumentException("n must be nonnegative");
        if (strategy == null) throw new NullPointerException("strategy");
        this.n = n;
        this.blockSize = Math.max(1, (int) Math.ceil(Math.sqrt(Math.max(1, n))));
        this.blockCount = (n + blockSize - 1) / blockSize;
        this.strategy = strategy;
        this.values = new long[n];
        this.blockProducts = new long[blockCount];
        clear();
    }

    /**
     * 列 {@code initialValues} を構築する。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @param strategy 可換群 G の演算
     */
    public FastUpdateAbelianGroupSqrtDecompositionLong(long[] initialValues,
            LongAbelianGroupStrategy strategy) {
        this(initialValues.length, strategy);
        build(initialValues);
    }

    /**
     * A を {@code initialValues} と等しくする。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param initialValues 長さ n の列
     * @throws IllegalArgumentException {@code initialValues.length != n} の場合
     */
    public void build(long[] initialValues) {
        if (initialValues.length != n) {
            throw new IllegalArgumentException("initialValues.length must equal size()");
        }
        System.arraycopy(initialValues, 0, values, 0, n);
        rebuildBlocks();
    }

    /**
     * A を {@code initialValues} の各要素を long に拡張した列と等しくする。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param initialValues 長さ n の列
     * @throws IllegalArgumentException {@code initialValues.length != n} の場合
     */
    public void build(int[] initialValues) {
        if (initialValues.length != n) {
            throw new IllegalArgumentException("initialValues.length must equal size()");
        }
        for (int i = 0; i < n; i++) values[i] = initialValues[i];
        rebuildBlocks();
    }

    /**
     * 全ての i について a_i = value とする。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param value 新しい各要素の値
     */
    public void fill(long value) {
        Arrays.fill(values, value);
        rebuildBlocks();
    }

    /**
     * a_i = value とする。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @param i 0-indexed の添字。0 <= i < n
     * @param value 新しい値
     * @throws IndexOutOfBoundsException {@code i < 0 || i >= n} の場合
     */
    public void set(int i, long value) {
        checkIndex(i);
        int block = i / blockSize;
        blockProducts[block] = strategy.mul(blockProducts[block], strategy.inverse(values[i]));
        blockProducts[block] = strategy.mul(blockProducts[block], value);
        values[i] = value;
    }

    /**
     * a_i = a_i * value とする。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @param i 0-indexed の添字。0 <= i < n
     * @param value a_i に掛ける値
     * @throws IndexOutOfBoundsException {@code i < 0 || i >= n} の場合
     */
    public void mul(int i, long value) {
        checkIndex(i);
        int block = i / blockSize;
        values[i] = strategy.mul(values[i], value);
        blockProducts[block] = strategy.mul(blockProducts[block], value);
    }

    /**
     * a_i を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @param i 0-indexed の添字。0 <= i < n
     * @return a_i
     * @throws IndexOutOfBoundsException {@code i < 0 || i >= n} の場合
     */
    public long get(int i) {
        checkIndex(i);
        return values[i];
    }

    /**
     * a_l * a_(l+1) * ... * a_(r-1) を返す。l = r のとき G の単位元を返す。
     *
     * <p>計算量: O(sqrt(n))</p>
     *
     * 未テスト
     * @param l 左端。0 <= l <= r
     * @param r 右端。l <= r <= n
     * @return 半開区間 [l, r) の積
     * @throws IndexOutOfBoundsException {@code l < 0 || l > r || r > n} の場合
     */
    public long fold(int l, int r) {
        checkRange(l, r);
        long product = strategy.identity();
        while (l < r && l % blockSize != 0) product = strategy.mul(product, values[l++]);
        while (l + blockSize <= r) {
            product = strategy.mul(product, blockProducts[l / blockSize]);
            l += blockSize;
        }
        while (l < r) product = strategy.mul(product, values[l++]);
        return product;
    }

    /**
     * a_0 * a_1 * ... * a_(n-1) を返す。
     *
     * <p>計算量: O(sqrt(n))</p>
     *
     * 未テスト
     * @return 列全体の積
     */
    public long prodAll() {
        long product = strategy.identity();
        for (long blockProduct : blockProducts) product = strategy.mul(product, blockProduct);
        return product;
    }

    /**
     * a_l * a_(l+1) * ... * a_(n-1) を返す。
     *
     * <p>計算量: O(sqrt(n))</p>
     *
     * 未テスト
     * @param l 左端。0 <= l <= n
     * @return 接尾辞 [l, n) の積
     */
    public long suffixFold(int l) {
        return fold(l, n);
    }

    /**
     * 全ての i について a_i = e とする。e は G の単位元である。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     */
    public void clear() {
        Arrays.fill(values, strategy.identity());
        Arrays.fill(blockProducts, strategy.identity());
    }

    /**
     * 列の長さ n を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @return n
     */
    public int size() {
        return n;
    }

    /**
     * G の単位元 e を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @return e
     */
    public long identity() {
        return strategy.identity();
    }

    /**
     * この列が使用する可換群戦略を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @return 可換群戦略
     */
    public LongAbelianGroupStrategy strategy() {
        return strategy;
    }

    /**
     * 全ての blockProducts[b] を現在の A から再構築する。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     */
    private void rebuildBlocks() {
        Arrays.fill(blockProducts, strategy.identity());
        for (int i = 0; i < n; i++) {
            int block = i / blockSize;
            blockProducts[block] = strategy.mul(blockProducts[block], values[i]);
        }
    }

    /**
     * i が A の添字であることを検査する。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @param i 検査する値
     */
    private void checkIndex(int i) {
        if (i < 0 || i >= n) throw new IndexOutOfBoundsException("index: " + i + ", size: " + n);
    }

    /**
     * [l, r) が A の部分区間であることを検査する。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     * @param l 左端
     * @param r 右端
     */
    private void checkRange(int l, int r) {
        if (l < 0 || l > r || r > n) {
            throw new IndexOutOfBoundsException("range: [" + l + ", " + r + "), size: " + n);
        }
    }
}
