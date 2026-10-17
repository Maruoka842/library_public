package library.util.polynomial;

import java.util.Arrays;

/** factor の固定反例。 */
public class PolynomialFpDynamic2DFactorRepro {
    /**
     * 3 個の固定入力について期待する因子数・重複度と実際の結果を出力する。未テスト。
     * 計算量: O(Σ Factor2D(f_i))、Factor2D(f) は factor(f) の計算量。
     */
    public static void main(String[] args) {
        PolynomialFpDynamic2D large = PolynomialFpDynamic2D.MOD998244353;
        long p = large.mod;
        long[][] a = {{p - 1, 1}, {}, {1}};
        long[][] b = {{p - 4, 1}, {}, {1}};
        report("(x^2+y-1)(x^2+y-4): expected 2 factors, multiplicities 1,1",
                large, large.mul(a, b));
        PolynomialFpDynamic2D small = PolynomialFpDynamic2D.of(3);
        report("F_3: x^3: expected x with multiplicity 3",
                small, new long[][] {{}, {}, {}, {1}});
        report("F_3: x(x+y^3-y): expected 2 factors, multiplicities 1,1",
                small, new long[][] {{}, {0, 2, 0, 1}, {1}});
    }

    /**
     * factor(f) の因子配列・重複度または例外を標準出力に出力する。未テスト。
     * 計算量: O(Factor2D(f) + N)、N は出力係数配列の総要素数。
     */
    private static void report(String label, PolynomialFpDynamic2D poly, long[][] f) {
        System.out.println(label);
        System.out.println("input=" + Arrays.deepToString(f));
        try {
            var result = poly.factor(f);
            System.out.println("leadingCoeff=" + result.leadingCoeff()
                    + ", factorCount=" + result.factors().length);
            for (var factor : result.factors()) {
                System.out.println(Arrays.deepToString(factor.factor)
                        + " multiplicity=" + factor.multiplicity);
            }
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
        }
    }
}
