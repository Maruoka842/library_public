package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PolynomialFpDynamic2DEvaluationTest {
    /**
     * 評価点、変数交換、再結合、標数の回帰テストを実行する。未テスト。
     * 計算量: O(Σ Factor2D(f))。
     */
    public static void main(String[] args) throws Exception {
        var test = new PolynomialFpDynamic2DEvaluationTest();
        test.swapsVariablesAndRestoresContent();
        test.handlesInseparableMainVariable();
        test.fallsBackWhenBothDirectionsFail();
        test.searchesBeyondFixedPoints();
        PolynomialFpDynamic2DCharacteristicTest.main(args);
        System.out.println("4 evaluation tests passed");
    }

    /** F_3 上の x(x+y^3-y) の2因子を検証する。未テスト。計算量: O(Factor2D(f))。 */
    @Test
    public void swapsVariablesAndRestoresContent() {
        check(3, new long[][][] {{{0}, {1}}, {{0, 2, 0, 1}, {1}}});
    }

    /** F_3 上の x^3+y は既約である。未テスト。計算量: O(Factor2D(f))。 */
    @Test
    public void handlesInseparableMainVariable() {
        check(3, new long[][][] {{{0, 1}, {0}, {0}, {1}}});
    }

    /**
     * F_2 上の ∏_(c=0,1)(x+y^2+y+c)(y+x^2+x+c) の4既約因子を検証する。
     * 両方向とも全評価点で重複因子を持つ。未テスト。計算量: O(Factor2D(f))。
     */
    @Test
    public void fallsBackWhenBothDirectionsFail() throws Exception {
        long[][][] factors = {{{0, 1, 1}, {1}}, {{1, 1, 1}, {1}},
                {{0, 1}, {1}, {1}}, {{1, 1}, {1}, {1}}};
        var poly = PolynomialFpDynamic2D.of(2);
        long[][] f = poly.one();
        for (long[][] factor : factors) f = poly.mul(f, factor);
        // f(x,y)=f(y,x) なので、一方向の失敗で両方向の失敗を確認できる。
        var attempt = PolynomialFpDynamic2D.class.getDeclaredMethod("tryFactorByWang", long[][].class);
        attempt.setAccessible(true);
        assertNull(attempt.invoke(poly, (Object) f));
        int stride = poly.degY(f) + 1;
        long[] flat = new long[poly.degX(f) * stride + poly.degY(f) + 1];
        for (int i = 0; i < f.length; i++)
            for (int j = 0; j < f[i].length; j++) flat[i * stride + j] = f[i][j];
        assertFalse(PolynomialFpDynamic.of(2).isSquareFree(flat));
        check(2, factors);
    }

    /**
     * x(x+∏_(i=0..7)(y-i)) に対し、固定点以外の良い評価点を返す。未テスト。
     * 計算量: O(EvaluationSearch(f))。
     */
    @Test
    public void searchesBeyondFixedPoints() throws Exception {
        var poly = PolynomialFpDynamic2D.MOD998244353;
        long[][] product = {{1}};
        for (int i = 0; i < 8; i++) product = poly.mul(product, new long[][] {{(poly.mod - i) % poly.mod, 1}});
        long[][] f = {{0}, product[0], {1}};
        var search = PolynomialFpDynamic2D.class.getDeclaredMethod("findGoodEvaluationPoint", long[][].class);
        search.setAccessible(true);
        long point = (long) search.invoke(poly, (Object) f);
        assertTrue(point >= 8 && point < poly.mod);
        assertTrue(PolynomialFpDynamic.MOD998244353.isSquareFree(poly.evalY(f, point)));
        check(poly.mod, new long[][][] {{{0}, {1}}, {product[0], {1}}});
    }

    /**
     * 指定した相異なる monic 既約因子の積を分解し、因子と復元結果を検証する。未テスト。
     * 計算量: O(Factor2D(f) + k Mul2D(f) + k^2 N)、k は因子数、N は係数数。
     */
    private static void check(long modulus, long[][][] expected) {
        var poly = PolynomialFpDynamic2D.of(modulus);
        long[][] f = poly.one();
        for (long[][] factor : expected) f = poly.mul(f, factor);
        var actual = poly.factor(f);
        assertEquals(expected.length, actual.factors().length);
        long[][] restored = {{actual.leadingCoeff()}};
        for (long[][] factor : expected) {
            int matches = 0;
            for (var item : actual.factors()) {
                if (poly.equals(factor, item.factor)) {
                    matches++;
                    assertEquals(1, item.multiplicity);
                }
            }
            assertEquals(1, matches, java.util.Arrays.deepToString(factor));
        }
        for (var item : actual.factors()) restored = poly.mul(restored, item.factor);
        assertTrue(poly.equals(f, restored));
    }
}
