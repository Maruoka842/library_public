package library.util.segtree;

import java.util.Arrays;

import library.util.algebra.strategy.longs.LongGroupStrategy;

/**
 * 逆元計算が重い群 G 上の列 A = (a_0, a_1, ..., a_(n-1)) を平方分割で管理する。
 * {@code fold(l, r)} は a_l * a_(l+1) * ... * a_(r-1) を返し、逆元計算を行わない。
 * 演算 * は可換でなくてもよい。
 */
public class FastFoldHeavyInverseGroupSqrtDecompositionLong {
    /** 管理する列の長さ。 */
    private final int n;

    /** 1 ブロックに含まれる要素数 B。 */
    private final int blockSize;

    /** blockCount = ceil(n / B)。 */
    private final int blockCount;

    /** 群 G の積、単位元、逆元を与える戦略。 */
    private final LongGroupStrategy strategy;

    /** values[i] = a_i。 */
    private final long[] values;

    /** inverseValues[i] = a_i^(-1)。 */
    private final long[] inverseValues;

    /** prefixesInBlock[i] = product_(j=floor(i / B) * B)^i a_j。ただし B = blockSize。 */
    private final long[] prefixesInBlock;

    /** inversePrefixesInBlock[i] = prefixesInBlock[i]^(-1)。 */
    private final long[] inversePrefixesInBlock;

    /** blockProducts[b] = product_(j=b * B)^(min(n, (b + 1) * B) - 1) a_j。ただし B = blockSize。 */
    private final long[] blockProducts;

    /** inverseBlockProducts[b] = blockProducts[b]^(-1)。 */
    private final long[] inverseBlockProducts;

    /** blockPrefixes[k] = product_(b=0)^(k-1) blockProducts[b]。空積 blockPrefixes[0] は単位元。 */
    private final long[] blockPrefixes;

    /** inverseBlockPrefixes[k] = blockPrefixes[k]^(-1)。 */
    private final long[] inverseBlockPrefixes;

    /**
     * 長さ {@code n} の定数列 (e, e, ..., e) を構築する。e は G の単位元である。
     * 構築中に逆元計算は行わない。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @param strategy 群 G の演算
     * @throws IllegalArgumentException {@code n < 0} の場合
     */
    public FastFoldHeavyInverseGroupSqrtDecompositionLong(int n, LongGroupStrategy strategy) {
        if (n < 0) throw new IllegalArgumentException("n must be nonnegative");
        if (strategy == null) throw new NullPointerException("strategy");
        this.n = n;
        this.blockSize = Math.max(1, (int) Math.ceil(Math.sqrt(Math.max(1, n))));
        this.blockCount = (n + blockSize - 1) / blockSize;
        this.strategy = strategy;
        this.values = new long[n];
        this.inverseValues = new long[n];
        this.prefixesInBlock = new long[n];
        this.inversePrefixesInBlock = new long[n];
        this.blockProducts = new long[blockCount];
        this.inverseBlockProducts = new long[blockCount];
        this.blockPrefixes = new long[blockCount + 1];
        this.inverseBlockPrefixes = new long[blockCount + 1];
        clear();
    }

    /**
     * 列 {@code initialValues} を構築する。
     * 逆元計算回数はちょうど n 回である。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @param strategy 群 G の演算
     */
    public FastFoldHeavyInverseGroupSqrtDecompositionLong(long[] initialValues,
            LongGroupStrategy strategy) {
        this(initialValues.length, strategy);
        build(initialValues);
    }

    /**
     * A を {@code initialValues} と等しくする。
     * 逆元計算回数はちょうど n 回である。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param initialValues 長さ n の列
     * @throws IllegalArgumentException {@code initialValues.length != n} の場合
     */
    public void build(long[] initialValues) {
        if (initialValues.length != n) {
            throw new IllegalArgumentException("initialValues.length must equal size()");
        }
        for (int i = 0; i < n; i++) {
            values[i] = initialValues[i];
            inverseValues[i] = strategy.inverse(initialValues[i]);
        }
        rebuildAll();
    }

    /**
     * A を {@code initialValues} の各要素を long に拡張した列と等しくする。
     * 逆元計算回数はちょうど n 回である。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param initialValues 長さ n の列
     * @throws IllegalArgumentException {@code initialValues.length != n} の場合
     */
    public void build(int[] initialValues) {
        if (initialValues.length != n) {
            throw new IllegalArgumentException("initialValues.length must equal size()");
        }
        for (int i = 0; i < n; i++) {
            values[i] = initialValues[i];
            inverseValues[i] = strategy.inverse(initialValues[i]);
        }
        rebuildAll();
    }

    /**
     * 全ての i について a_i = value とする。
     * n > 0 なら逆元計算回数は1回、n = 0 なら0回である。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param value 新しい各要素の値
     */
    public void fill(long value) {
        Arrays.fill(values, value);
        if (n > 0) Arrays.fill(inverseValues, strategy.inverse(value));
        rebuildAll();
    }

    /**
     * a_i = value とする。逆元計算回数はちょうど1回である。
     *
     * <p>計算量: O(sqrt(n))
     *
     * 未テスト
     * @param i 0-indexed の添字。0 <= i < n
     * @param value 新しい値
     * @throws IndexOutOfBoundsException {@code i < 0 || i >= n} の場合
     */
    public void set(int i, long value) {
        checkIndex(i);
        long inverseValue = strategy.inverse(value);
        values[i] = value;
        inverseValues[i] = inverseValue;
        rebuildBlock(i / blockSize);
        rebuildBlockPrefixes();
    }

    /**
     * a_i = a_i * value とする。逆元計算回数はちょうど1回である。
     *
     * <p>計算量: O(sqrt(n))
     *
     * 未テスト
     * @param i 0-indexed の添字。0 <= i < n
     * @param value a_i の右から掛ける値
     * @throws IndexOutOfBoundsException {@code i < 0 || i >= n} の場合
     */
    public void mul(int i, long value) {
        checkIndex(i);
        long inverseValue = strategy.inverse(value);
        values[i] = strategy.mul(values[i], value);
        inverseValues[i] = strategy.mul(inverseValue, inverseValues[i]);
        rebuildBlock(i / blockSize);
        rebuildBlockPrefixes();
    }

    /**
     * a_i を返す。逆元計算は行わない。
     *
     * <p>計算量: O(1)
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
     * 逆元計算は行わない。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param l 左端。0 <= l <= r
     * @param r 右端。l <= r <= n
     * @return 半開区間 [l, r) の積
     * @throws IndexOutOfBoundsException {@code l < 0 || l > r || r > n} の場合
     */
    public long fold(int l, int r) {
        checkRange(l, r);
        if (l == r) return strategy.identity();
        int leftBlock = l / blockSize;
        int rightBlock = (r - 1) / blockSize;
        if (leftBlock == rightBlock) return foldInBlock(l, r);

        long left = foldInBlock(l, Math.min(n, (leftBlock + 1) * blockSize));
        long middle = strategy.mul(inverseBlockPrefixes[leftBlock + 1], blockPrefixes[rightBlock]);
        long right = foldInBlock(rightBlock * blockSize, r);
        return strategy.mul(strategy.mul(left, middle), right);
    }

    /**
     * a_0 * a_1 * ... * a_(n-1) を返す。逆元計算は行わない。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @return 列全体の積
     */
    public long prodAll() {
        return blockPrefixes[blockCount];
    }

    /**
     * a_l * a_(l+1) * ... * a_(n-1) を返す。逆元計算は行わない。
     *
     * <p>計算量: O(1)
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
     * 逆元計算は行わない。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     */
    public void clear() {
        long identity = strategy.identity();
        Arrays.fill(values, identity);
        Arrays.fill(inverseValues, identity);
        rebuildAll();
    }

    /**
     * 列の長さ n を返す。
     *
     * <p>計算量: O(1)
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
     * <p>計算量: O(1)
     *
     * 未テスト
     * @return e
     */
    public long identity() {
        return strategy.identity();
    }

    /**
     * この列が使用する群戦略を返す。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @return 群戦略
     */
    public LongGroupStrategy strategy() {
        return strategy;
    }

    /**
     * 全ブロックとブロック累積積を現在の A と A の逆元列から再構築する。
     * 逆元計算は行わない。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     */
    private void rebuildAll() {
        for (int block = 0; block < blockCount; block++) rebuildBlock(block);
        rebuildBlockPrefixes();
    }

    /**
     * 指定ブロックの累積積、その逆元、全積、その逆元を再構築する。
     * 逆元計算は行わない。
     *
     * <p>計算量: O(sqrt(n))
     *
     * 未テスト
     * @param block 0-indexed のブロック番号
     */
    private void rebuildBlock(int block) {
        int from = block * blockSize;
        int to = Math.min(n, from + blockSize);
        long product = strategy.identity();
        long inverseProduct = strategy.identity();
        for (int i = from; i < to; i++) {
            product = strategy.mul(product, values[i]);
            inverseProduct = strategy.mul(inverseValues[i], inverseProduct);
            prefixesInBlock[i] = product;
            inversePrefixesInBlock[i] = inverseProduct;
        }
        blockProducts[block] = product;
        inverseBlockProducts[block] = inverseProduct;
    }

    /**
     * blockPrefixes[k] と inverseBlockPrefixes[k] を全て再構築する。
     * 逆元計算は行わない。
     *
     * <p>計算量: O(sqrt(n))
     *
     * 未テスト
     */
    private void rebuildBlockPrefixes() {
        blockPrefixes[0] = strategy.identity();
        inverseBlockPrefixes[0] = strategy.identity();
        for (int block = 0; block < blockCount; block++) {
            blockPrefixes[block + 1] = strategy.mul(blockPrefixes[block], blockProducts[block]);
            inverseBlockPrefixes[block + 1] = strategy.mul(inverseBlockProducts[block],
                    inverseBlockPrefixes[block]);
        }
    }

    /**
     * 同一ブロック内の a_l * ... * a_(r-1) を返す。逆元計算は行わない。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param l 左端
     * @param r 右端
     * @return [l, r) の積
     */
    private long foldInBlock(int l, int r) {
        if (l == r) return strategy.identity();
        int blockStart = (l / blockSize) * blockSize;
        long inverseBefore = l == blockStart ? strategy.identity() : inversePrefixesInBlock[l - 1];
        return strategy.mul(inverseBefore, prefixesInBlock[r - 1]);
    }

    /**
     * i が A の添字であることを検査する。
     *
     * <p>計算量: O(1)
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
     * <p>計算量: O(1)
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
