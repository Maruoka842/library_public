package library.util.polynomial;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import library.util.seq.Permutation;
import static org.junit.jupiter.api.Assertions.*;

public class RookPolynomialTest {

    private long[] rookPolynomialOfFerrersDp(long[] b, long mod) {
        int N = b.length;
        long[] dp = new long[N + 1];
        dp[0] = 1;
        for (int i = 0; i < N; i++) {
            long bi = b[i];
            long[] nextDp = new long[N + 1];
            for (int j = 0; j <= N; j++) {
                // Transition 1: no rook in row i
                nextDp[j] = (nextDp[j] + dp[j]) % mod;
                // Transition 2: place rook in row i
                if (j > 0) {
                    long ways = (bi - j + 1) % mod;
                    if (ways < 0) ways += mod;
                    nextDp[j] = (nextDp[j] + dp[j - 1] * ways) % mod;
                }
            }
            dp = nextDp;
        }
        return dp;
    }

    @Test
    void testEmptyBoard() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        long[] b = new long[0];

        // Permutation static methods
        assertArrayEquals(new long[]{1}, Permutation.rookPolynomialOfFerrers(b, poly.mod));
    }

    @Test
    void testSizeOneBoards() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;

        // b = [1]
        assertArrayEquals(new long[]{1, 1}, Permutation.rookPolynomialOfFerrers(new long[]{1}, poly.mod));

        // b = [0]
        assertArrayEquals(new long[]{1, 0}, Permutation.rookPolynomialOfFerrers(new long[]{0}, poly.mod));

    }

    @Test
    void testAllZeroRoots() {
        // c_i = b_i - i = 0 mod mod
        // Let's create b_i = i
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        int N = 5;
        long[] b = new long[N];
        for (int i = 0; i < N; i++) {
            b[i] = i;
        }

        long[] rDp = rookPolynomialOfFerrersDp(b, poly.mod);

        // Permutation static methods
        assertArrayEquals(rDp, Permutation.rookPolynomialOfFerrers(b, poly.mod));
    }

    @Test
    void testRandomSmallBoardsNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        Random rnd = new Random(42);
        int iterations = 100;

        for (int iter = 0; iter < iterations; iter++) {
            int N = rnd.nextInt(50) + 1;
            long[] b = new long[N];
            for (int i = 0; i < N; i++) {
                b[i] = rnd.nextInt(100);
            }
            Arrays.sort(b);

            long[] rDp = rookPolynomialOfFerrersDp(b, poly.mod);

            // Permutation static methods
            assertArrayEquals(rDp, Permutation.rookPolynomialOfFerrers(b, poly.mod));
        }
    }

    @Test
    void testRandomSmallBoardsNonNTT() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.of(1000000007L);
        Random rnd = new Random(12345);
        int iterations = 100;

        for (int iter = 0; iter < iterations; iter++) {
            int N = rnd.nextInt(50) + 1;
            long[] b = new long[N];
            for (int i = 0; i < N; i++) {
                b[i] = rnd.nextInt(100);
            }
            Arrays.sort(b);

            long[] rDp = rookPolynomialOfFerrersDp(b, poly.mod);

            // Permutation static methods
            assertArrayEquals(rDp, Permutation.rookPolynomialOfFerrers(b, poly.mod));
        }
    }

    @Test
    void testLargeThresholdTrigger() {
        PolynomialFpDynamic poly = PolynomialFpDynamic.MOD998244353;
        int N = 2100;
        long[] b = new long[N];
        for (int i = 0; i < N; i++) {
            b[i] = i + 1;
        }

        long[] rDp = rookPolynomialOfFerrersDp(b, poly.mod);

        // Permutation static methods
        assertArrayEquals(rDp, Permutation.rookPolynomialOfFerrers(b, poly.mod));
    }
}
