package library.util.polynomial;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PolynomialFpDynamic の compWithKnownInverse メソッドのテスト。
 * 未テスト
 */
public class PolynomialFpDynamicCompInverseTest {

    private long compWithKnownInverseNaive(PolynomialFpDynamic poly, long[] A, long[] G, int n) {
        if (n == 0)
            return A.length > 0 ? poly.fp.reduce(A[0]) : 0;
        if (n < 0)
            return 0;
        long[] Adiff = poly.differentiate(A);
        long[] H = poly.divideByX(G, 1);
        long[] HinvN = poly.pow(poly.inv(Arrays.copyOf(H, n)), n);
        long[] Res = poly.mul(Arrays.copyOf(Adiff, n), HinvN);
        long coeff = (n - 1 < Res.length) ? Res[n - 1] : 0;
        return poly.fp.reduce(coeff) * poly.fp.inv(n) % poly.mod;
    }

    @Test
    void testCompWithKnownInverseRandomNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        Random rng = new Random(42);

        for (int iter = 0; iter < 100; iter++) {
            int lenA = rng.nextInt(30) + 1;
            long[] A = new long[lenA];
            for (int i = 0; i < lenA; i++) {
                A[i] = rng.nextInt(1000);
            }

            int lenG = rng.nextInt(30) + 2;
            long[] G = new long[lenG];
            G[0] = 0;
            G[1] = rng.nextInt(998244352) + 1; // G'(0) != 0
            for (int i = 2; i < lenG; i++) {
                G[i] = rng.nextInt(1000);
            }

            int n = rng.nextInt(40) + 1;

            long expected = compWithKnownInverseNaive(poly, A, G, n);
            long actual = poly.compWithKnownInverse(A, G, n);

            assertEquals(expected, actual, "Failed on mod 998244353 with n=" + n);
        }
    }

    @Test
    void testCompWithKnownInverseRandomNonNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.of(1000000007);
        Random rng = new Random(1337);

        for (int iter = 0; iter < 100; iter++) {
            int lenA = rng.nextInt(30) + 1;
            long[] A = new long[lenA];
            for (int i = 0; i < lenA; i++) {
                A[i] = rng.nextInt(1000);
            }

            int lenG = rng.nextInt(30) + 2;
            long[] G = new long[lenG];
            G[0] = 0;
            G[1] = rng.nextInt(1000000006) + 1; // G'(0) != 0
            for (int i = 2; i < lenG; i++) {
                G[i] = rng.nextInt(1000);
            }

            int n = rng.nextInt(40) + 1;

            long expected = compWithKnownInverseNaive(poly, A, G, n);
            long actual = poly.compWithKnownInverse(A, G, n);

            assertEquals(expected, actual, "Failed on mod 1000000007 with n=" + n);
        }
    }

    @Test
    void testPowerProjectionWithKnownInverse() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        Random rng = new Random(101);

        // f(0) = 0, f'(0) != 0. Let's design f(x) = x + 2x^2 + 3x^3
        // h(w) is f^{-1}(w)
        // Let's test with a small N
        for (int N = 1; N <= 15; N++) {
            // Find a random G(x) of size N+2 where G[0] = 0, G[1] != 0
            long[] G = new long[N + 2];
            G[0] = 0;
            G[1] = rng.nextInt(998244352) + 1;
            for (int i = 2; i < G.length; i++) {
                G[i] = rng.nextInt(1000);
            }

            // G(x) is h(w) = f^{-1}(w)
            // Let's compute f(x) using compInverse of G
            long[] f = poly.compInverse(G); // length G.length

            // Now compute [x^N] f(x)^i for i = 0, 1, ..., N using powerProjectionWithKnownInverse
            long[] proj = poly.powerProjectionWithKnownInverse(G, N);

            assertEquals(N + 1, proj.length, "Result must have length N + 1");

            // Compute expected using poly.pow
            for (int i = 0; i <= N; i++) {
                long[] f_pow_i = poly.pow(f, i);
                long expected = (N < f_pow_i.length) ? f_pow_i[N] : 0;
                assertEquals(expected, proj[i], "Mismatch at N=" + N + ", i=" + i);
            }
        }
    }

    @Test
    void testCompWithKnownInverseEdgeCases() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        long[] A = {1, 2, 3};
        long[] G = {0, 4, 5};

        // n = 0
        assertEquals(1, poly.compWithKnownInverse(A, G, 0));

        // n < 0
        assertEquals(0, poly.compWithKnownInverse(A, G, -1));
        assertEquals(0, poly.compWithKnownInverse(A, G, -5));

        // G(0) != 0 throws exception
        long[] G_bad0 = {1, 4, 5};
        assertThrows(IllegalArgumentException.class, () -> {
            poly.compWithKnownInverse(A, G_bad0, 3);
        });

        // G'(0) == 0 throws exception
        long[] G_bad1 = {0, 0, 5};
        assertThrows(IllegalArgumentException.class, () -> {
            poly.compWithKnownInverse(A, G_bad1, 3);
        });

        // G length < 2 throws exception
        long[] G_short = {0};
        assertThrows(IllegalArgumentException.class, () -> {
            poly.compWithKnownInverse(A, G_short, 3);
        });
    }
}
