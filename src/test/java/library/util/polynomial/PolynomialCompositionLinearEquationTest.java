package library.util.polynomial;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import library.util.MathUtils;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PolynomialFpDynamic#solveCompositionLinearEquation(long[], long[], long[], long[], int)} のテストクラス。
 *
 * <p>
 * d(x) f(a(x)) + b(x) f(x) = c(x) mod x^n
 * </p>
 *
 * <p>
 * 計算量: O(n^2) (検証用ナイーブ), O(n log^3 n) (高速アルゴリズム)
 * </p>
 */
public class PolynomialCompositionLinearEquationTest {

    /**
     * O(N^2) の素朴な解法。係数関係の漸化式：
     * f_i = (c_i - [x^i](d(x) f_(<i)(a(x)) + b(x) f_(<i)(x))) / (d_0 α^i + b_0) mod p
     * を用いて項を1つずつ決定する。
     */
    private long[] solveNaive(PolynomialFpDynamic poly, long[] a, long[] d, long[] b, long[] c, int n) {
        long mod = poly.mod;
        long[] f = new long[n];
        long alpha = a.length > 1 ? a[1] : 0;
        long d0 = d[0] % mod;
        long b0 = b[0] % mod;

        for (int i = 0; i < n; i++) {
            // f_{<i}(x)
            long[] fLess = Arrays.copyOf(f, i);

            // f_{<i}(a(x))
            long[] faLess = poly.comp(fLess, a, i + 1);

            // d(x) * f_{<i}(a(x))
            long[] dfaLess = poly.mul(d, faLess);

            // b(x) * f_{<i}(x)
            long[] bfLess = poly.mul(b, fLess);

            long valD = (i < dfaLess.length) ? dfaLess[i] : 0;
            long valB = (i < bfLess.length) ? bfLess[i] : 0;
            long lhs_i = (valD + valB) % mod;

            long target = (c[i] - lhs_i) % mod;
            if (target < 0) target += mod;

            long denom = (d0 * MathUtils.modPow(alpha, i, mod) + b0) % mod;
            if (denom < 0) denom += mod;

            if (denom == 0) {
                throw new ArithmeticException("Diagonal coefficient is zero.");
            }

            f[i] = target * MathUtils.modInv(denom, mod) % mod;
        }
        return f;
    }

    @Test
    void testNIsOne() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        long[] a = {0, 2};
        long[] d = {3, 1};
        long[] b = {4, 5};
        long[] c = {14, 2};

        // n = 1
        // d(0) + b(0) = 3 + 4 = 7
        // f[0] = 14 / 7 = 2
        long[] f = poly.solveCompositionLinearEquation(a, d, b, c, 1);
        assertArrayEquals(new long[] { 2 }, f);
    }

    @Test
    void testAlphaIsZero() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // a(x) = 0 (a(0) = 0, a'(0) = 0)
        long[] a = {0, 0, 5}; // a(0)=0, a'(0)=0, alpha=0
        long[] d = {3};       // d_0 = 3
        long[] b = {4};       // b_0 = 4
        long[] c = {14, 28};

        // n = 2
        // d(0) alpha^i + b(0) = 3 * 0^i + 4
        // i = 0: 3 * 1 + 4 = 7 -> f[0] = c[0]/7 = 14/7 = 2
        // i = 1: 3 * 0 + 4 = 4 -> f[1] = (c[1] - [x^1](d(x)*f0(a(x)) + b(x)*f0(x))) / 4
        // f0(a(x)) = f0(0) = 2. d(x)*f0(a(x)) = 3 * 2 = 6.
        // b(x)*f0(x) = 4 * 2 = 8.
        // d(x)*f0(a(x)) + b(x)*f0(x) = 14. [x^1] of it is 0.
        // f[1] = (28 - 0) / 4 = 7
        long[] expected = {2, 7};
        long[] actual = poly.solveCompositionLinearEquation(a, d, b, c, 2);
        assertArrayEquals(expected, actual);
        assertArrayEquals(expected, solveNaive(poly, a, d, b, c, 2));
    }

    @Test
    void testExceptionWhenUnsolvable() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        long[] a = {0, 2}; // alpha = 2
        long[] d = {1};    // d_0 = 1
        // we want d_0 alpha^i + b_0 = 0 mod p for some i.
        // Let i = 1: 1 * 2^1 + b_0 = 0 mod p -> b_0 = -2 mod p
        long[] b = { poly.mod - 2 };
        long[] c = {1, 2};

        // should throw ArithmeticException
        assertThrows(ArithmeticException.class, () -> {
            poly.solveCompositionLinearEquation(a, d, b, c, 2);
        });
    }

    @Test
    void testRandomStressNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        runRandomStress(poly, 100, 42);
    }

    @Test
    void testRandomStressNonNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.of(1000000007L);
        runRandomStress(poly, 50, 1337);
    }

    private void runRandomStress(PolynomialFpDynamic poly, int iterations, long seed) {
        Random rnd = new Random(seed);
        long mod = poly.mod;

        for (int step = 0; step < iterations; step++) {
            int n = rnd.nextInt(20) + 1; // 1 <= n <= 20

            // generate a: a(0) = 0, alpha = a'(0) must be non-zero (or zero, but we need to satisfy the condition)
            long[] a = new long[n + 2];
            a[0] = 0;
            long alpha = 0;
            while (alpha == 0) {
                alpha = rnd.nextInt((int) Math.min(mod, 1000));
            }
            a[1] = alpha;
            for (int i = 2; i < a.length; i++) {
                a[i] = rnd.nextInt((int) Math.min(mod, 1000));
            }

            // generate d and b such that d(0) alpha^i + b(0) != 0 for all 0 <= i < n
            long[] d = new long[n + 2];
            long[] b = new long[n + 2];
            while (true) {
                for (int i = 0; i < d.length; i++) {
                    d[i] = rnd.nextInt((int) Math.min(mod, 1000));
                    b[i] = rnd.nextInt((int) Math.min(mod, 1000));
                }
                boolean ok = true;
                long d0 = d[0] % mod;
                long b0 = b[0] % mod;
                for (int i = 0; i < n; i++) {
                    long term = (d0 * MathUtils.modPow(alpha, i, mod) + b0) % mod;
                    if (term == 0) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    break;
                }
            }

            // generate c
            long[] c = new long[n + 2];
            for (int i = 0; i < c.length; i++) {
                c[i] = rnd.nextInt((int) Math.min(mod, 1000));
            }

            long[] expected = solveNaive(poly, a, d, b, c, n);
            long[] actual = poly.solveCompositionLinearEquation(a, d, b, c, n);

            assertArrayEquals(expected, actual, "Mismatch at step " + step + " with n=" + n);
        }
    }
}
