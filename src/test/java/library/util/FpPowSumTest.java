package library.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Random;

public class FpPowSumTest {

    // Naive summation modulo mod
    private long naivePow2Sum(long start, long length, long mod) {
        if (length <= 0) return 0;
        long sum = 0;
        for (long i = 0; i < length; i++) {
            long term = (start + i) % mod;
            if (term < 0) term += mod;
            sum = (sum + term * term) % mod;
        }
        return sum;
    }

    private long naivePow3Sum(long start, long length, long mod) {
        if (length <= 0) return 0;
        long sum = 0;
        for (long i = 0; i < length; i++) {
            long term = (start + i) % mod;
            if (term < 0) term += mod;
            sum = (sum + term * term % mod * term) % mod;
        }
        return sum;
    }

    @Test
    public void testPow2SumSmallAndEdgeCases() {
        Fp fp = new Fp(998244353);

        // length <= 0
        assertEquals(0, fp.pow2Sum(10, 0));
        assertEquals(0, fp.pow2Sum(10, -5));

        // length = 1
        assertEquals(100, fp.pow2Sum(10, 1));
        assertEquals(fp.reduce(100), fp.pow2Sum(10, 1));

        // Negative start
        assertEquals(naivePow2Sum(-5, 10, 998244353), fp.pow2Sum(-5, 10));

        // Small lengths
        for (int len = 1; len <= 100; len++) {
            assertEquals(naivePow2Sum(3, len, 998244353), fp.pow2Sum(3, len));
        }
    }

    @Test
    public void testPow3SumSmallAndEdgeCases() {
        Fp fp = new Fp(998244353);

        // length <= 0
        assertEquals(0, fp.pow3Sum(10, 0));
        assertEquals(0, fp.pow3Sum(10, -5));

        // length = 1
        assertEquals(1000, fp.pow3Sum(10, 1));

        // Negative start
        assertEquals(naivePow3Sum(-5, 10, 998244353), fp.pow3Sum(-5, 10));

        // Small lengths
        for (int len = 1; len <= 100; len++) {
            assertEquals(naivePow3Sum(3, len, 998244353), fp.pow3Sum(3, len));
        }
    }

    @Test
    public void testDifferentModuli() {
        long[] moduli = {2, 3, 4, 5, 6, 7, 12, 100, 998244353, 1000000007};
        for (long mod : moduli) {
            Fp fp = new Fp(mod);
            for (long start = -10; start <= 10; start++) {
                for (long len = 1; len <= 20; len++) {
                    assertEquals(naivePow2Sum(start, len, mod), fp.pow2Sum(start, len),
                            "pow2Sum failed for start=" + start + ", len=" + len + ", mod=" + mod);
                    assertEquals(naivePow3Sum(start, len, mod), fp.pow3Sum(start, len),
                            "pow3Sum failed for start=" + start + ", len=" + len + ", mod=" + mod);
                }
            }
        }
    }

    @Test
    public void testRandomLargeInputs() {
        Random rng = new Random(42);
        long[] moduli = {2, 3, 5, 13, 998244353, 1000000007};
        for (long mod : moduli) {
            Fp fp = new Fp(mod);
            for (int i = 0; i < 500; i++) {
                long start = rng.nextLong() % 10000;
                long len = rng.nextInt(200) + 1;
                assertEquals(naivePow2Sum(start, len, mod), fp.pow2Sum(start, len));
                assertEquals(naivePow3Sum(start, len, mod), fp.pow3Sum(start, len));
            }
        }
    }

    @Test
    public void testVeryLargeLengthsAndStarts() {
        // Since we can't easily do naive sum for length = 10^18, we can test algebraic consistency:
        // sum(start, len1 + len2) == sum(start, len1) + sum(start + len1, len2)
        Fp fp = new Fp(998244353);
        long start = 123456789123456L;
        long len1 = 987654321987654L;
        long len2 = 555555555555555L;

        long sum2Combined = fp.pow2Sum(start, len1 + len2);
        long sum2Split = (fp.pow2Sum(start, len1) + fp.pow2Sum(start + len1, len2)) % fp.mod;
        assertEquals(sum2Combined, sum2Split);

        long sum3Combined = fp.pow3Sum(start, len1 + len2);
        long sum3Split = (fp.pow3Sum(start, len1) + fp.pow3Sum(start + len1, len2)) % fp.mod;
        assertEquals(sum3Combined, sum3Split);
    }
}
