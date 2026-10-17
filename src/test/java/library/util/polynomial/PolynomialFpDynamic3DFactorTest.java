package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PolynomialFpDynamic3DFactorTest {
    /** 回帰テストを実行する。未テスト。計算量: O(Σ Factor3D(f) + Σ Factor2D(f))。 */
    public static void main(String[] args) throws Exception {
        var test = new PolynomialFpDynamic3DFactorTest();
        test.recombinesAtZeroAndNonzeroPoints();
        System.out.println("3D recombination passed");
        test.recoversCharacteristicPowers();
        System.out.println("3D characteristic passed");
        test.changesVariableOrder();
        System.out.println("3D variable order passed");
        test.recoversFromUnsolvablePolynomialCorrection();
        System.out.println("3D correction fallback passed");
        test.kroneckerHandlesRepeatedImages();
        System.out.println("3D Kronecker passed");
        test.fallsBackAfterAllVariableOrders();
        System.out.println("3D all-order fallback passed");
        PolynomialFpDynamic2DEvaluationTest.main(args);
    }

    /**
     * ∏(x^2+z-c)、および y に依存する座標変換後の因子を検証する。未テスト。
     * 計算量: O(Σ Factor3D(f))。
     */
    @Test
    public void recombinesAtZeroAndNonzeroPoints() {
        var p = PolynomialFpDynamic3D.MOD998244353;
        for (int offset : new int[] {0, 1}) {
            long[][][] base = p.add(p.mul(p.x(), p.x()), p.z());
            check(p, new long[][][][] {p.sub(base, constant(offset)), p.sub(base, constant(offset == 0 ? 1 : 4))}, new int[] {1, 1});
        }
        long[][][] shiftedX = p.add(p.x(), p.y());
        long[][][] base = p.add(p.mul(shiftedX, shiftedX), p.z());
        check(p, new long[][][][] {p.sub(base, constant(1)), p.sub(base, constant(4))}, new int[] {1, 1});
    }

    /**
     * (x+y+z)^(p^k) と通常の重複因子を含む分解を検証する。未テスト。
     * 計算量: O(Σ Factor3D(f) + Σ m Mul3D(f))。
     */
    @Test
    public void recoversCharacteristicPowers() {
        for (int modulus : new int[] {2, 3, 5}) {
            var p = new PolynomialFpDynamic3D(modulus);
            long[][][] linear = p.add(p.add(p.x(), p.y()), p.z());
            check(p, new long[][][][] {linear}, new int[] {modulus});
            check(p, new long[][][][] {linear}, new int[] {modulus * modulus});
        }
        var p = new PolynomialFpDynamic3D(3);
        long[][][] linear = p.add(p.add(p.x(), p.y()), p.z());
        check(p, new long[][][][] {p.x(), p.add(linear, p.one())}, new int[] {3, 2});
        long[][][] inseparable = p.add(p.mul(p.mul(p.x(), p.x()), p.x()), p.z());
        check(p, new long[][][][] {inseparable}, new int[] {1});
    }

    /**
     * F_3 上の x(x+z^3-z) を分解し、変数交換で現れるコンテンツを回収する。未テスト。
     * 計算量: O(Factor3D(f))。
     */
    @Test
    public void changesVariableOrder() {
        var p = new PolynomialFpDynamic3D(3);
        long[][][] z3 = p.mul(p.mul(p.z(), p.z()), p.z());
        check(p, new long[][][][] {p.x(), p.add(p.x(), p.sub(z3, p.z()))}, new int[] {1, 1});
        long[][][] x3 = p.mul(p.mul(p.x(), p.x()), p.x());
        long[][][] inseparable = p.add(x3, p.add(p.y(), p.z()));
        check(p, new long[][][][] {p.x(), inseparable, p.add(inseparable, p.one())}, new int[] {1, 1, 1});
    }

    /**
     * (x^2-y^2-z)(x+2) の分解を検証する。z=0 の補正には 1∈(x-y,x+y) が必要となる。
     * 未テスト。計算量: O(Factor3D(f))。
     */
    @Test
    public void recoversFromUnsolvablePolynomialCorrection() {
        var p = PolynomialFpDynamic3D.MOD998244353;
        long[][][] quadratic = p.sub(p.sub(p.mul(p.x(), p.x()), p.mul(p.y(), p.y())), p.z());
        check(p, new long[][][][] {quadratic, p.add(p.x(), constant(2))}, new int[] {1, 1});
    }

    /**
     * 3変数の Kronecker 分解が特殊化で生じる重複因子を扱えることを検証する。未テスト。
     * 計算量: O(Kronecker3D(f))。
     */
    @Test
    public void kroneckerHandlesRepeatedImages() {
        var p = new PolynomialFpDynamic3D(2);
        long[][][] a = p.add(p.add(p.x(), p.y()), p.z());
        long[][][] b = p.add(a, p.one());
        long[][][] f = p.mul(a, b);
        var factors = p.factorByKronecker(f);
        assertEquals(2, factors.length);
        assertTrue(p.equals(factors[0].factor, a) || p.equals(factors[1].factor, a));
        assertTrue(p.equals(factors[0].factor, b) || p.equals(factors[1].factor, b));
        assertEquals(1, factors[0].multiplicity);
        assertEquals(1, factors[1].multiplicity);
        assertTrue(p.equals(f, p.mul(factors[0].factor, factors[1].factor)));
    }

    /**
     * F_2 上の (x^2+y+z)(y^2+x+z)(z^2+x+y) は各主変数に非分離因子を持つ。
     * 全6順序の失敗後も3既約因子が得られることを検証する。未テスト。
     * 計算量: O(Σ Wang(f) + Kronecker3D(f))。
     */
    @Test
    public void fallsBackAfterAllVariableOrders() throws Exception {
        var p = new PolynomialFpDynamic3D(2);
        long[][][] a = p.add(p.mul(p.x(), p.x()), p.add(p.y(), p.z()));
        long[][][] b = p.add(p.mul(p.y(), p.y()), p.add(p.x(), p.z()));
        long[][][] c = p.add(p.mul(p.z(), p.z()), p.add(p.x(), p.y()));
        long[][][] f = p.mul(p.mul(a, b), c);
        // f は全変数置換で不変。
        var attempt = PolynomialFpDynamic3D.class.getDeclaredMethod("tryFactorByWang", long[][][].class);
        attempt.setAccessible(true);
        assertNull(attempt.invoke(p, (Object) f));
        check(p, new long[][][][] {a, b, c}, new int[] {1, 1, 1});
    }

    /** 定数 c の係数配列を返す。未テスト。計算量: O(1)。 */
    private static long[][][] constant(long c) {
        return new long[][][] {{{c}}};
    }

    /**
     * f=(p-1)∏ expected_i^multiplicity_i の既約因子・重複度・復元結果を検証する。未テスト。
     * 計算量: O(Factor3D(f) + m Mul3D(f) + k^2 N)、m は重複度の総和、k は因子数、N は係数数。
     */
    private static void check(PolynomialFpDynamic3D p, long[][][][] expected, int[] multiplicities) {
        long[][][] f = constant(p.mod - 1);
        for (int i = 0; i < expected.length; i++)
            for (int j = 0; j < multiplicities[i]; j++) f = p.mul(f, expected[i]);
        var actual = p.factor(f);
        assertEquals(p.mod - 1, actual.leadingCoeff());
        assertEquals(expected.length, actual.factors().length);
        for (int i = 0; i < expected.length; i++) {
            int matches = 0;
            for (var factor : actual.factors()) {
                if (p.equals(p.monic(expected[i]), factor.factor)) {
                    matches++;
                    assertEquals(multiplicities[i], factor.multiplicity);
                }
            }
            assertEquals(1, matches, java.util.Arrays.deepToString(expected[i]));
        }
        long[][][] restored = constant(actual.leadingCoeff());
        for (var factor : actual.factors())
            for (int i = 0; i < factor.multiplicity; i++) restored = p.mul(restored, factor.factor);
        assertTrue(p.equals(f, restored));
    }
}
