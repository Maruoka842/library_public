package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PolynomialFpDynamic2DFactorTest {
    /** 演算対象の有限体は F_998244353。 */
    private static final PolynomialFpDynamic2D POLY = PolynomialFpDynamic2D.MOD998244353;

    /**
     * (x^2+y-1)(x^2+y-4) の既約因子と重複度を検証する。未テスト。
     * 計算量: O(Factor2D(f) + Mul2D(f))。
     */
    @Test
    public void recombinesTruncatedSeriesAtZero() {
        check(new long[][][] {{{POLY.mod - 1, 1}, {}, {1}},
                {{POLY.mod - 4, 1}, {}, {1}}}, new int[] {1, 1});
    }

    /**
     * (x^2+y)(x^2+y-1) は y=0,1 で square-free でなく、非零評価点で分解できる。未テスト。
     * 計算量: O(Factor2D(f) + Mul2D(f))。
     */
    @Test
    public void recombinesAtNonzeroEvaluationPoint() {
        check(new long[][][] {{{0, 1}, {}, {1}},
                {{POLY.mod - 1, 1}, {}, {1}}}, new int[] {1, 1});
    }

    /**
     * 3 個の既約2次因子の再結合を検証する。未テスト。
     * 計算量: O(Factor2D(f) + Mul2D(f))。
     */
    @Test
    public void recombinesThreeQuadratics() {
        check(new long[][][] {{{POLY.mod - 1, 1}, {}, {1}},
                {{POLY.mod - 4, 1}, {}, {1}},
                {{POLY.mod - 9, 1}, {}, {1}}}, new int[] {1, 1, 1});
    }

    /**
     * y のコンテンツ、非定数の x 主係数、重複因子を含む分解を検証する。未テスト。
     * 計算量: O(Factor2D(f) + Mul2D(f))。
     */
    @Test
    public void preservesContentLeadingCoefficientAndMultiplicities() {
        check(new long[][][] {{{1, 1}}, {{1}, {0, 1}},
                {{POLY.mod - 1, 1}, {}, {1}},
                {{POLY.mod - 4, 1}, {}, {1}}}, new int[] {1, 1, 2, 2});
    }

    /**
     * f=7 prod_i expected[i]^multiplicities[i] の分解が指定した monic 既約因子列と一致し、
     * 返値から f を復元できることを検証する。未テスト。
     * 計算量: O(Factor2D(f) + s Mul2D(f) + k^2 N)、s は重複度の総和、
     * k は因子数、N は f の係数配列の要素数。
     */
    private static void check(long[][][] expected, int[] multiplicities) {
        long[][] input = {{7}};
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < multiplicities[i]; j++) input = POLY.mul(input, expected[i]);
        }
        var result = POLY.factor(input);
        assertEquals(7, result.leadingCoeff());
        assertEquals(expected.length, result.factors().length);
        for (int i = 0; i < expected.length; i++) {
            boolean found = false;
            for (var actual : result.factors()) {
                if (POLY.equals(expected[i], actual.factor)) {
                    assertEquals(multiplicities[i], actual.multiplicity);
                    found = true;
                }
            }
            assertTrue(found, "Missing factor " + java.util.Arrays.deepToString(expected[i]));
        }
        long[][] restored = {{result.leadingCoeff()}};
        for (var factor : result.factors()) {
            for (int i = 0; i < factor.multiplicity; i++) restored = POLY.mul(restored, factor.factor);
        }
        assertTrue(POLY.equals(input, restored));
    }
}
