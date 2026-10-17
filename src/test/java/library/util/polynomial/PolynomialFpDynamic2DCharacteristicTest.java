package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PolynomialFpDynamic2DCharacteristicTest {
    /**
     * 標数と再結合に関する7件の回帰テストを実行し、不一致なら AssertionError を送出する。未テスト。
     * 計算量: O(Σ Factor2D(f) + Σ SquareFree2D(f) + Σ Mul2D(f))。
     */
    public static void main(String[] args) {
        var characteristic = new PolynomialFpDynamic2DCharacteristicTest();
        characteristic.recoversFrobeniusPowers();
        characteristic.preservesMixedMultiplicities();
        characteristic.distinguishesZeroPartialDerivativeFromCube();
        var recombination = new PolynomialFpDynamic2DFactorTest();
        recombination.recombinesTruncatedSeriesAtZero();
        recombination.recombinesAtNonzeroEvaluationPoint();
        recombination.recombinesThreeQuadratics();
        recombination.preservesContentLeadingCoefficientAndMultiplicities();
        System.out.println("7 regression tests passed");
    }

    /**
     * F_p 上の (x+y)^(p^k) の因子と重複度を検証する。未テスト。
     * 計算量: O(Σ Factor2D(f))。p∈{2,3,5}, k∈{1,2}。
     */
    @Test
    public void recoversFrobeniusPowers() {
        for (int p : new int[] {2, 3, 5}) {
            var poly = PolynomialFpDynamic2D.of(p);
            for (int exponent : new int[] {p, p * p}) {
                long[][] f = new long[exponent + 1][1];
                f[0] = new long[exponent + 1];
                f[0][exponent] = 1;
                f[exponent][0] = 1;
                var result = poly.factor(f);
                assertEquals(1, result.leadingCoeff());
                assertEquals(1, result.factors().length);
                assertEquals(exponent, result.factors()[0].multiplicity);
                assertTrue(poly.equals(new long[][] {{0, 1}, {1}}, result.factors()[0].factor));
            }
        }
    }

    /**
     * F_3 上の x^3 および 2 x^3 (x+y+1)^2 の分解を検証する。未テスト。
     * 計算量: O(Factor2D(f) + Mul2D(f))。
     */
    @Test
    public void preservesMixedMultiplicities() {
        var poly = PolynomialFpDynamic2D.of(3);
        long[][] x3 = {{0}, {0}, {0}, {1}};
        var pure = poly.factor(x3);
        assertEquals(1, pure.factors().length);
        assertEquals(3, pure.factors()[0].multiplicity);
        assertTrue(poly.equals(new long[][] {{0}, {1}}, pure.factors()[0].factor));
        long[][] linear = {{1, 1}, {1}};
        long[][] f = poly.mul(poly.mul(x3, poly.mul(linear, linear)), 2);
        var result = poly.factor(f);
        assertEquals(2, result.leadingCoeff());
        assertEquals(2, result.factors().length);
        long[][] restored = {{result.leadingCoeff()}};
        for (var factor : result.factors()) {
            if (poly.equals(linear, factor.factor)) assertEquals(2, factor.multiplicity);
            else {
                assertTrue(poly.equals(new long[][] {{0}, {1}}, factor.factor));
                assertEquals(3, factor.multiplicity);
            }
            for (int i = 0; i < factor.multiplicity; i++) restored = poly.mul(restored, factor.factor);
        }
        assertTrue(poly.equals(f, restored));
    }

    /**
     * F_3 上の x^3+y は x 微分が零でも square-free である。未テスト。
     * 計算量: O(SquareFree2D(f))。
     */
    @Test
    public void distinguishesZeroPartialDerivativeFromCube() {
        var poly = PolynomialFpDynamic2D.of(3);
        long[][] f = {{0, 1}, {0}, {0}, {1}};
        var result = poly.squareFreeDecomposition(f);
        assertEquals(1, result.length);
        assertEquals(1, result[0].multiplicity);
        assertTrue(poly.equals(f, result[0].factor));
    }
}
