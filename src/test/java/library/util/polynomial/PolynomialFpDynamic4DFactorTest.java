package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PolynomialFpDynamic4DFactorTest {
    /** 回帰テストを実行する。未テスト。計算量: O(Σ Factor4D(f) + Σ Factor3D(f))。 */
    public static void main(String[] args) throws Exception {
        var test = new PolynomialFpDynamic4DFactorTest();
        test.recombinesAtZeroAndNonzeroPoints();
        System.out.println("4D recombination passed");
        test.recoversCharacteristicPowers();
        System.out.println("4D characteristic passed");
        test.changesMainVariableAndRestoresContent();
        System.out.println("4D variable order passed");
        test.recoversFromUnsolvableCorrection();
        System.out.println("4D correction fallback passed");
        test.searchesBeyondFixedPoints();
        System.out.println("4D random evaluation passed");
        test.fallsBackAfterAllVariableOrders();
        System.out.println("4D all-order fallback passed");
        PolynomialFpDynamic3DFactorTest.main(args);
    }

    /** (x^2+w-a)(x^2+w-b) と x を x+y+z に置換した分解を検証する。未テスト。計算量: O(Σ Factor4D(f))。 */
    @Test
    public void recombinesAtZeroAndNonzeroPoints() {
        var p = PolynomialFpDynamic4D.MOD998244353;
        long[][][][] base = p.add(p.mul(p.x(), p.x()), p.w());
        check(p, new long[][][][][] {base, p.sub(base, p.one())}, new int[] {1, 1});
        check(p, new long[][][][][] {p.sub(base, constant(1)), p.sub(base, constant(4))}, new int[] {1, 1});
        long[][][][] u = p.add(p.add(p.x(), p.y()), p.z());
        base = p.add(p.mul(u, u), p.w());
        check(p, new long[][][][][] {p.sub(base, constant(1)), p.sub(base, constant(4))}, new int[] {1, 1});
    }

    /** (x+y+z+w)^(p^k) と x^3+w の因子・重複度を検証する。未テスト。計算量: O(Σ Factor4D(f) + Σ m Mul4D(f))。 */
    @Test
    public void recoversCharacteristicPowers() {
        for (int modulus : new int[] {2, 3, 5}) {
            var p = new PolynomialFpDynamic4D(modulus);
            long[][][][] linear = p.add(p.add(p.x(), p.y()), p.add(p.z(), p.w()));
            check(p, new long[][][][][] {linear}, new int[] {modulus});
            if (modulus <= 3) check(p, new long[][][][][] {linear}, new int[] {modulus * modulus});
        }
        var p = new PolynomialFpDynamic4D(3);
        long[][][][] x3 = p.mul(p.mul(p.x(), p.x()), p.x());
        check(p, new long[][][][][] {p.add(x3, p.w())}, new int[] {1});
        long[][][][] linear = p.add(p.add(p.x(), p.y()), p.add(p.z(), p.w()));
        check(p, new long[][][][][] {p.x(), p.add(linear, p.one())}, new int[] {3, 2});
    }

    /** F_3 上の x(x^3+y+z+w)(x^3+y+z+w+1) の分解を検証する。未テスト。計算量: O(Factor4D(f))。 */
    @Test
    public void changesMainVariableAndRestoresContent() {
        var p = new PolynomialFpDynamic4D(3);
        long[][][][] x3 = p.mul(p.mul(p.x(), p.x()), p.x());
        long[][][][] a = p.add(x3, p.add(p.y(), p.add(p.z(), p.w())));
        check(p, new long[][][][][] {p.x(), a, p.add(a, p.one())}, new int[] {1, 1, 1});
    }

    /** (x^2-(y+z)^2-w)(x+2) の分解を検証する。未テスト。計算量: O(Factor4D(f))。 */
    @Test
    public void recoversFromUnsolvableCorrection() throws Exception {
        var p = PolynomialFpDynamic4D.MOD998244353;
        long[][][][] yz = p.add(p.y(), p.z());
        long[][][][] a = p.sub(p.sub(p.mul(p.x(), p.x()), p.mul(yz, yz)), p.w());
        long[][][][] b = p.add(p.x(), constant(2));
        var attempt = PolynomialFpDynamic4D.class.getDeclaredMethod("tryFactorByWang", long[][][][].class);
        attempt.setAccessible(true);
        assertNull(attempt.invoke(p, (Object) p.mul(a, b)));
        check(p, new long[][][][][] {a, b}, new int[] {1, 1});
    }

    /** x(x+∏_(i=0..7)(w-i)) の固定点以外の評価点と分解を検証する。未テスト。計算量: O(Factor4D(f))。 */
    @Test
    public void searchesBeyondFixedPoints() throws Exception {
        var p = PolynomialFpDynamic4D.MOD998244353;
        long[][][][] product = p.one();
        for (int i = 0; i < 8; i++) product = p.mul(product, p.sub(p.w(), constant(i)));
        long[][][][] b = p.add(p.x(), product);
        var search = PolynomialFpDynamic4D.class.getDeclaredMethod("findGoodEvaluationPointW", long[][][][].class);
        search.setAccessible(true);
        long a = (long) search.invoke(p, (Object) p.mul(p.x(), b));
        assertTrue(a >= 8 && a < p.mod);
        check(p, new long[][][][][] {p.x(), b}, new int[] {1, 1});
    }

    /**
     * F_2 上の (x^2+y^2+z+w+1)(x+y+z^2+w^2+1) は各主変数に非分離因子を持つ。
     * 全24順序での失敗後も2既約因子を返すことを検証する。未テスト。
     * 計算量: O(Σ Wang(f) + Kronecker4D(f))。
     */
    @Test
    public void fallsBackAfterAllVariableOrders() throws Exception {
        var p = new PolynomialFpDynamic4D(2);
        long[][][][] a = p.add(p.add(p.add(p.mul(p.x(), p.x()), p.mul(p.y(), p.y())), p.add(p.z(), p.w())), p.one());
        long[][][][] b = p.add(p.add(p.add(p.x(), p.y()), p.add(p.mul(p.z(), p.z()), p.mul(p.w(), p.w()))), p.one());
        long[][][][] f = p.mul(a, b);
        var attempt = PolynomialFpDynamic4D.class.getDeclaredMethod("tryFactorByWang", long[][][][].class);
        var permute = PolynomialFpDynamic4D.class.getDeclaredMethod("permuteVariables", long[][][][].class, int[].class);
        attempt.setAccessible(true);
        permute.setAccessible(true);
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
            if (i == j) continue;
            for (int k = 0; k < 4; k++) {
                if (k == i || k == j) continue;
                long[][][][] g = (long[][][][]) permute.invoke(p, f, new int[] {i, j, k, 6 - i - j - k});
                assertNull(attempt.invoke(p, (Object) g), "order=" + i + "," + j + "," + k);
            }
        }
        check(p, new long[][][][][] {a, b}, new int[] {1, 1});
    }

    /** 定数 c の係数配列を返す。未テスト。計算量: O(1)。 */
    private static long[][][][] constant(long c) {
        return new long[][][][] {{{{c}}}};
    }

    /**
     * f=(p-1)∏ expected_i^multiplicity_i の既約因子・重複度・復元結果を検証する。未テスト。
     * 計算量: O(Factor4D(f) + m Mul4D(f) + k^2 N)、m は重複度の総和、k は因子数、N は係数数。
     */
    private static void check(PolynomialFpDynamic4D p, long[][][][][] expected, int[] multiplicities) {
        long[][][][] f = constant(p.mod - 1);
        for (int i = 0; i < expected.length; i++)
            for (int j = 0; j < multiplicities[i]; j++) f = p.mul(f, expected[i]);
        var actual = p.factor(f);
        assertEquals(p.mod - 1, actual.leadingCoeff());
        assertEquals(expected.length, actual.factors().length);
        for (int i = 0; i < expected.length; i++) {
            int matches = 0;
            for (var factor : actual.factors()) if (p.equals(p.monic(expected[i]), factor.factor)) {
                matches++;
                assertEquals(multiplicities[i], factor.multiplicity);
            }
            assertEquals(1, matches, java.util.Arrays.deepToString(expected[i]));
        }
        long[][][][] restored = constant(actual.leadingCoeff());
        for (var factor : actual.factors())
            for (int i = 0; i < factor.multiplicity; i++) restored = p.mul(restored, factor.factor);
        assertTrue(p.equals(f, restored));
    }
}
