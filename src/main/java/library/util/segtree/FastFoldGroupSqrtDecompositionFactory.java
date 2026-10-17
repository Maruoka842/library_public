package library.util.segtree;

import library.util.Fp;
import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;
import library.util.algebra.strategy.longs.LongGroupStrategy;
import library.util.algebra.strategy.longs.LongModAddAbelianGroupStrategy;

/** 群上の {@link FastFoldGroupSqrtDecompositionLong} を生成する。 */
public final class FastFoldGroupSqrtDecompositionFactory {
    /**
     * インスタンス化を禁止する。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     */
    private FastFoldGroupSqrtDecompositionFactory() {
    }

    /**
     * 長さ n の 0 列を加法群 (long, +, 0) 上で管理する構造を返す。
     * long の加減算は Java のオーバーフロー規則に従う。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @return 区間和を O(1)、一点代入を O(sqrt(n)) で処理する構造
     */
    public static FastFoldGroupSqrtDecompositionLong sum(int n) {
        return new FastFoldGroupSqrtDecompositionLong(n, LongAddAbelianGroupStrategy.STRATEGY);
    }

    /**
     * 列 initialValues を加法群 (long, +, 0) 上で管理する構造を返す。
     * long の加減算は Java のオーバーフロー規則に従う。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @return 区間和を O(1)、一点代入を O(sqrt(n)) で処理する構造
     */
    public static FastFoldGroupSqrtDecompositionLong sum(long[] initialValues) {
        return new FastFoldGroupSqrtDecompositionLong(initialValues,
                LongAddAbelianGroupStrategy.STRATEGY);
    }

    /**
     * 長さ n の 0 列を加法群 Z/modZ 上で管理する構造を返す。
     * 区間和の結果は [0, mod) に正規化される。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @param mod 法。mod > 0
     * @return 法 mod の区間和を O(1)、一点代入を O(sqrt(n)) で処理する構造
     */
    public static FastFoldGroupSqrtDecompositionLong modSum(int n, long mod) {
        return new FastFoldGroupSqrtDecompositionLong(n,
                new LongModAddAbelianGroupStrategy(mod));
    }

    /**
     * 列 initialValues を加法群 Z/modZ 上で管理する構造を返す。
     * 区間和の結果は [0, mod) に正規化される。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @param mod 法。mod > 0
     * @return 法 mod の区間和を O(1)、一点代入を O(sqrt(n)) で処理する構造
     */
    public static FastFoldGroupSqrtDecompositionLong modSum(long[] initialValues, long mod) {
        return new FastFoldGroupSqrtDecompositionLong(initialValues,
                new LongModAddAbelianGroupStrategy(mod));
    }

    /**
     * 長さ n の定数列 (1, 1, ..., 1) を乗法群 F_p^* 上で管理する構造を返す。
     *
     * <p>事前条件: {@code prime} は素数。</p>
     * <p>計算量: O(n)</p>
     *
     * 未テスト
     * @param n 列の長さ。n >= 0
     * @param prime 素数 p
     * @return 区間積を逆元計算なしの O(1)、一点代入を O(sqrt(n) + log(p)) で処理する構造
     */
    public static FastFoldHeavyInverseGroupSqrtDecompositionLong modProduct(int n, long prime) {
        return new FastFoldHeavyInverseGroupSqrtDecompositionLong(n,
                modPrimeMultiplicativeGroup(prime));
    }

    /**
     * 列 initialValues を乗法群 F_p^* 上で管理する構造を返す。
     * 各 a_i は a_i != 0 (mod p) を満たすものとする。
     * {@code fold(l, r)} は product_(i=l)^(r-1) a_i mod p を返す。
     *
     * <p>事前条件: {@code prime} は素数かつ、全ての i について
     * {@code initialValues[i] != 0 (mod prime)}。</p>
     * <p>計算量: O(n log(p))</p>
     *
     * 未テスト
     * @param initialValues 初期列。以後この配列とは状態を共有しない
     * @param prime 素数 p
     * @return 区間積を逆元計算なしの O(1)、一点代入を O(sqrt(n) + log(p)) で処理する構造
     */
    public static FastFoldHeavyInverseGroupSqrtDecompositionLong modProduct(long[] initialValues,
            long prime) {
        return new FastFoldHeavyInverseGroupSqrtDecompositionLong(initialValues,
                modPrimeMultiplicativeGroup(prime));
    }

    /**
     * F_p^* = ({1, 2, ..., p - 1}, multiplication mod p) を返す。
     * inverse(a) は a = 0 (mod p) のとき例外を送出する。
     *
     * <p>事前条件: {@code prime} は素数。</p>
     * <p>計算量: 構築 O(1)、積 O(1)、逆元 O(log(p))</p>
     *
     * 未テスト
     * @param prime 素数 p
     * @return F_p^* の群演算
     */
    private static LongGroupStrategy modPrimeMultiplicativeGroup(long prime) {
        Fp fp = new Fp(prime);
        return new LongGroupStrategy() {
            /**
             * F_p^* の単位元 1 を返す。
             *
             * <p>計算量: O(1)</p>
             * 未テスト
             * @return 1
             */
            @Override
            public long identity() {
                return fp.one();
            }

            /**
             * a * b mod p を [0, p) の代表元で返す。
             *
             * <p>計算量: O(1)</p>
             * 未テスト
             * @param a F_p^* の元
             * @param b F_p^* の元
             * @return a * b mod p
             */
            @Override
            public long mul(long a, long b) {
                return fp.reduce(fp.mul(a, b));
            }

            /**
             * a^(-1) mod p を返す。
             *
             * <p>計算量: O(log(p))</p>
             * 未テスト
             * @param a F_p^* の元
             * @return a^(-1) mod p
             * @throws ArithmeticException {@code a = 0 (mod p)} の場合
             */
            @Override
            public long inverse(long a) {
                a = fp.reduce(a);
                if (a == 0) throw new ArithmeticException("0 has no multiplicative inverse modulo " + prime);
                return fp.inv(a);
            }
        };
    }
}
