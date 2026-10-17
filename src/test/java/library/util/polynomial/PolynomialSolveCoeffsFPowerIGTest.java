package library.util.polynomial;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

public class PolynomialSolveCoeffsFPowerIGTest {

    @Test
    void testEdgeAndValidationCases() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // Negative n should return empty array
        assertArrayEquals(new long[0], poly.diagonalPowerProjection(new long[]{1, 2}, new long[]{3, 4}, -1));
        assertArrayEquals(new long[0], poly.diagonalPowerProjection(new long[]{1, 2}, new long[]{3, 4}, 2, -1));

        // n = 0 should return a length 1 array with [x^0] g
        assertArrayEquals(new long[]{3}, poly.diagonalPowerProjection(new long[]{1, 2}, new long[]{3, 4}, 0));
        assertArrayEquals(new long[]{3}, poly.diagonalPowerProjection(new long[]{1, 2}, new long[]{3, 4}, 2, 0));
        assertArrayEquals(new long[]{0}, poly.diagonalPowerProjection(new long[]{1, 2}, null, 2, 0));

        // null/empty f and g should be handled gracefully
        assertArrayEquals(new long[]{0, 0, 0}, poly.diagonalPowerProjection(null, null, 2));
        assertArrayEquals(new long[]{0, 0, 0}, poly.diagonalPowerProjection(null, null, 2, 2));
    }

    @Test
    void testZeroConstantTermF() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // f = [0, 2], g = [3, 4], n = 2
        // f(x) = 2x, g(x) = 3 + 4x
        // i = 0: [x^0] f^0 g = [x^0] g = 3
        // i = 1: [x^1] f^1 g = [x^1] (2x * (3+4x)) = 6
        // i = 2: [x^2] f^2 g = [x^2] (4x^2 * (3+4x)) = 12
        // Expected: [3, 6, 12]
        long[] f = {0, 2};
        long[] g = {3, 4};
        long[] expected = {3, 6, 12};
        assertArrayEquals(expected, poly.diagonalPowerProjection(f, g, 2));

        // Let's test with naive solver as well
        assertArrayEquals(expected, solveCoeffsNaive(f, g, 1, 2, poly));
    }

    @Test
    void testNonZeroConstantTermF() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // f = [1, 2], g = [3, 4], n = 2
        // f(x) = 1 + 2x, g(x) = 3 + 4x
        // i = 0: [x^0] f^0 g = [x^0] g = 3
        // i = 1: [x^1] f^1 g = [x^1] ((1 + 2x)(3 + 4x)) = [x^1] (3 + 10x + 8x^2) = 10
        // i = 2: [x^2] f^2 g = [x^2] ((1 + 4x + 4x^2)(3 + 4x)) = [x^2] (3 + 16x + 28x^2 + 16x^3) = 28
        // Expected: [3, 10, 28]
        long[] f = {1, 2};
        long[] g = {3, 4};
        long[] expected = {3, 10, 28};
        assertArrayEquals(expected, poly.diagonalPowerProjection(f, g, 2));
        assertArrayEquals(expected, solveCoeffsNaive(f, g, 1, 2, poly));
    }

    @Test
    void testVariableAValuationShifts() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // f = [0, 0, 2] (v = 2), g = [3, 4], n = 2
        // f(x) = 2x^2
        // f^0 g = 3 + 4x
        // f^1 g = 2x^2 * (3 + 4x) = 6x^2 + 8x^3
        // f^2 g = 4x^4 * (3 + 4x) = 12x^4 + 16x^5

        long[] f = {0, 0, 2};
        long[] g = {3, 4};

        // Case 1: a = 3
        // d = a - v = 3 - 2 = 1 > 0
        // i = 0: [x^0] (3 + 4x) = 3
        // i = 1: [x^3] (6x^2 + 8x^3) = 8
        // i = 2: [x^6] (12x^4 + 16x^5) = 0
        // Expected: [3, 8, 0]
        assertArrayEquals(new long[]{3, 8, 0}, poly.diagonalPowerProjection(f, g, 3, 2));
        assertArrayEquals(new long[]{3, 8, 0}, solveCoeffsNaive(f, g, 3, 2, poly));

        // Case 2: a = 2
        // d = a - v = 2 - 2 = 0
        // fv0 = 2, g0 = 3
        // i = 0: [x^0] (3+4x) = 3
        // i = 1: [x^2] (6x^2+8x^3) = 6 = (fv0^1 * g0)
        // i = 2: [x^4] (12x^4+16x^5) = 12 = (fv0^2 * g0)
        // Expected: [3, 6, 12]
        assertArrayEquals(new long[]{3, 6, 12}, poly.diagonalPowerProjection(f, g, 2, 2));
        assertArrayEquals(new long[]{3, 6, 12}, solveCoeffsNaive(f, g, 2, 2, poly));

        // Case 3: a = 1
        // d = a - v = 1 - 2 = -1 < 0
        // Expected: [3, 0, 0]
        assertArrayEquals(new long[]{3, 0, 0}, poly.diagonalPowerProjection(f, g, 1, 2));
        assertArrayEquals(new long[]{3, 0, 0}, solveCoeffsNaive(f, g, 1, 2, poly));

        // Case 4: a = 0
        // Expected: [3, 0, 0]
        assertArrayEquals(new long[]{3, 0, 0}, poly.diagonalPowerProjection(f, g, 0, 2));
        assertArrayEquals(new long[]{3, 0, 0}, solveCoeffsNaive(f, g, 0, 2, poly));
    }

    @Test
    void testRandomStressMod998244353() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        runRandomStress(poly, 100, 42);
    }

    @Test
    void testRandomStressMod1000000007() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.of(1000000007L);
        // This is non-NTT-friendly, so it falls back to the naive fallback in powerProjection
        runRandomStress(poly, 30, 12345);
    }

    private void runRandomStress(PolynomialFpDynamic poly, int iterations, long seed) {
        Random rnd = new Random(seed);
        long mod = poly.mod;

        for (int step = 0; step < iterations; step++) {
            int n = rnd.nextInt(15); // maximum degree up to 14
            int a = rnd.nextInt(4);  // a in {0, 1, 2, 3}

            // Randomize f
            int lenF = rnd.nextInt(15) + 1;
            long[] f = new long[lenF];
            for (int i = 0; i < lenF; i++) {
                f[i] = (rnd.nextLong() & Long.MAX_VALUE) % mod;
            }
            // Randomly shift f by 0 to 3 zeros
            int shift = rnd.nextInt(4);
            if (shift > 0) {
                long[] shiftedF = new long[lenF + shift];
                System.arraycopy(f, 0, shiftedF, shift, lenF);
                f = shiftedF;
            }

            // Randomize g
            int lenG = rnd.nextInt(15) + 1;
            long[] g = new long[lenG];
            for (int i = 0; i < lenG; i++) {
                g[i] = (rnd.nextLong() & Long.MAX_VALUE) % mod;
            }

            long[] expected = solveCoeffsNaive(f, g, a, n, poly);
            long[] actual = poly.diagonalPowerProjection(f, g, a, n);

            assertArrayEquals(expected, actual, "Mismatch at step " + step + " with a=" + a + ", n=" + n + ", lenF=" + f.length + ", lenG=" + lenG);
        }
    }

    private long[] solveCoeffsNaive(long[] f, long[] g, int a, int n, PolynomialFpDynamic poly) {
        long[] ans = new long[n + 1];
        long[] curF = {1}; // f(x)^0
        for (int i = 0; i <= n; i++) {
            long[] prod = poly.mul(curF, g);
            int targetPower = a * i;
            if (targetPower >= 0 && targetPower < prod.length) {
                ans[i] = poly.fp.reduce(prod[targetPower]);
            } else {
                ans[i] = 0;
            }
            curF = poly.mul(curF, f);
            int maxPowerNeeded = a * n;
            if (maxPowerNeeded >= 0 && curF.length > maxPowerNeeded + 1) {
                curF = Arrays.copyOf(curF, maxPowerNeeded + 1);
            }
        }
        return ans;
    }
}
