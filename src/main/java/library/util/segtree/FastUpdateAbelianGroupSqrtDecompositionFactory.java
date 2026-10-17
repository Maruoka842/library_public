package library.util.segtree;

import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongModAddAbelianGroupStrategy;

/** 可換群上の {@link FastUpdateAbelianGroupSqrtDecompositionLong} を生成する。 */
public final class FastUpdateAbelianGroupSqrtDecompositionFactory {
    /**
     * インスタンス化を禁止する。
     *
     * <p>計算量: O(1)</p>
     *
     * 未テスト
     */
    private FastUpdateAbelianGroupSqrtDecompositionFactory() {
    }

    /**
     * 長さ n の 0 列を加法群 (long, +, 0) 上で管理する構造を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @return 一点代入を O(1)、区間和を O(sqrt(n)) で処理する構造
     */
    public static FastUpdateAbelianGroupSqrtDecompositionLong sum(int n) {
        return new FastUpdateAbelianGroupSqrtDecompositionLong(n,
                LongAddAbelianGroupStrategy.STRATEGY);
    }

    /**
     * 列 initialValues を加法群 (long, +, 0) 上で管理する構造を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @return 一点代入を O(1)、区間和を O(sqrt(n)) で処理する構造
     */
    public static FastUpdateAbelianGroupSqrtDecompositionLong sum(long[] initialValues) {
        return new FastUpdateAbelianGroupSqrtDecompositionLong(initialValues,
                LongAddAbelianGroupStrategy.STRATEGY);
    }

    /**
     * 長さ n の 0 列を加法群 Z/modZ 上で管理する構造を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @param mod 法。mod > 0
     * @return 一点代入を O(1)、法 mod の区間和を O(sqrt(n)) で処理する構造
     */
    public static FastUpdateAbelianGroupSqrtDecompositionLong modSum(int n, long mod) {
        return new FastUpdateAbelianGroupSqrtDecompositionLong(n,
                new LongModAddAbelianGroupStrategy(mod));
    }

    /**
     * 列 initialValues を加法群 Z/modZ 上で管理する構造を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @param mod 法。mod > 0
     * @return 一点代入を O(1)、法 mod の区間和を O(sqrt(n)) で処理する構造
     */
    public static FastUpdateAbelianGroupSqrtDecompositionLong modSum(long[] initialValues, long mod) {
        return new FastUpdateAbelianGroupSqrtDecompositionLong(initialValues,
                new LongModAddAbelianGroupStrategy(mod));
    }
}
