package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SegTreeModAddPowerSumTest {

    @Test
    public void testK0() {
        long mod = 1000000007L;
        int n = 5;
        var seg = SegTreeFactory.modAdd_powerSum(n, 0, mod);

        // Build with initial leaves having length 1
        long[][] init = new long[n][1];
        for (int i = 0; i < n; i++) {
            init[i][0] = 1;
        }
        seg.build(init);

        assertEquals(5, seg.fold(0, 5)[0]);
        assertEquals(2, seg.fold(1, 3)[0]);

        // Range add should not change element counts
        seg.act(1, 4, 10L);
        assertEquals(5, seg.fold(0, 5)[0]);
        assertEquals(3, seg.fold(1, 4)[0]);
    }

    @Test
    public void testK1() {
        long mod = 1000000007L;
        int n = 5;
        var seg = SegTreeFactory.modAdd_powerSum(n, 1, mod);

        long[][] init = new long[n][2];
        long[] initialVals = {10, 20, 30, 40, 50};
        for (int i = 0; i < n; i++) {
            init[i][0] = 1;
            init[i][1] = initialVals[i] % mod;
        }
        seg.build(init);

        // Initial fold [0, 5)
        long[] res = seg.fold(0, 5);
        assertEquals(5, res[0]);
        assertEquals(150, res[1]);

        // Range add 5 to [1, 4) -> elements become 10, 25, 35, 45, 50
        seg.act(1, 4, 5L);
        res = seg.fold(0, 5);
        assertEquals(5, res[0]);
        assertEquals(165, res[1]);

        // Fold [1, 4)
        res = seg.fold(1, 4);
        assertEquals(3, res[0]);
        assertEquals(105, res[1]);
    }

    @Test
    public void testK2PowerSum() {
        long mod = 1000000007L;
        int n = 10;
        var seg = SegTreeFactory.modAdd_powerSum(n, 2, mod);

        long[] a = new long[n];
        long[][] init = new long[n][3];
        for (int i = 0; i < n; i++) {
            a[i] = i * 7 + 3;
            init[i][0] = 1;
            init[i][1] = a[i] % mod;
            init[i][2] = (a[i] * a[i]) % mod;
        }
        seg.build(init);

        // Range add
        seg.act(2, 7, 15L);
        for (int i = 2; i < 7; i++) a[i] = (a[i] + 15) % mod;

        long[] expected = new long[3];
        expected[0] = n;
        for (int i = 0; i < n; i++) {
            expected[1] = (expected[1] + a[i]) % mod;
            expected[2] = (expected[2] + a[i] * a[i]) % mod;
        }
        assertArrayEquals(expected, seg.fold(0, n));
    }

    @Test
    public void testK3PowerSum() {
        long mod = 1000000007L;
        int n = 5;
        int k = 3;
        var seg = SegTreeFactory.modAdd_powerSum(n, k, mod);

        long[] a = {1, 2, 3, 4, 5};
        long[][] init = new long[n][k + 1];
        for (int i = 0; i < n; i++) {
            init[i][0] = 1;
            long p = 1;
            for (int m = 1; m <= k; m++) {
                p = (p * a[i]) % mod;
                init[i][m] = p;
            }
        }
        seg.build(init);

        // 0th: 5, 1st: 15, 2nd: 1^2+2^2+3^2+4^2+5^2 = 55, 3rd: 1^3+2^3+3^3+4^3+5^3 = 225
        long[] res = seg.fold(0, 5);
        assertEquals(5, res[0]);
        assertEquals(15, res[1]);
        assertEquals(55, res[2]);
        assertEquals(225, res[3]);

        // Range add +3 on [1, 4) -> a becomes [1, 5, 6, 7, 5]
        seg.act(1, 4, 3L);
        a[1] += 3;
        a[2] += 3;
        a[3] += 3;

        long[] expected = new long[k + 1];
        expected[0] = n;
        for (int j = 1; j <= k; j++) {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                long term = 1;
                for (int m = 0; m < j; m++) {
                    term = (term * a[i]) % mod;
                }
                sum = (sum + term) % mod;
            }
            expected[j] = sum;
        }

        res = seg.fold(0, 5);
        assertArrayEquals(expected, res);
    }

    @Test
    public void testNegativeKThrows() {
        assertThrows(IllegalArgumentException.class, () -> SegTreeFactory.modAdd_powerSum(5, -1, 1000000007L));
    }

    @Test
    public void testEdgeCasesModAndNegativeAdds() {
        long mod = 1000000007L;
        int n = 5;
        int k = 3;
        var seg = SegTreeFactory.modAdd_powerSum(n, k, mod);

        long[] a = {10, 20, 30, 40, 50};
        long[][] init = new long[n][k + 1];
        for (int i = 0; i < n; i++) {
            init[i][0] = 1;
            long p = 1;
            for (int m = 1; m <= k; m++) {
                p = (p * a[i]) % mod;
                init[i][m] = p;
            }
        }
        seg.build(init);

        // Add multiple of mod (mod itself) - elements should remain congruent mod mod
        seg.act(0, n, mod);
        for (int i = 0; i < n; i++) {
            a[i] = (a[i] + mod) % mod;
        }

        long[] expected = new long[k + 1];
        expected[0] = n;
        for (int j = 1; j <= k; j++) {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                long term = 1;
                for (int m = 0; m < j; m++) term = (term * a[i]) % mod;
                sum = (sum + term) % mod;
            }
            expected[j] = sum;
        }
        assertArrayEquals(expected, seg.fold(0, n));

        // Negative add
        seg.act(1, 4, -5L);
        for (int i = 1; i < 4; i++) {
            a[i] = (a[i] - 5 % mod + mod) % mod;
        }

        for (int j = 1; j <= k; j++) {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                long term = 1;
                for (int m = 0; m < j; m++) term = (term * a[i]) % mod;
                sum = (sum + term) % mod;
            }
            expected[j] = sum;
        }
        assertArrayEquals(expected, seg.fold(0, n));
    }

    @Test
    public void testStressAgainstNaive() {
        long mod = 998244353L;
        int n = 20;
        int k = 4;
        var seg = SegTreeFactory.modAdd_powerSum(n, k, mod);

        long[] a = new long[n];
        long[][] init = new long[n][k + 1];
        for (int i = 0; i < n; i++) {
            a[i] = i * 13 + 7;
            init[i][0] = 1;
            long p = 1;
            for (int m = 1; m <= k; m++) {
                p = (p * a[i]) % mod;
                init[i][m] = p;
            }
        }
        seg.build(init);

        java.util.Random rnd = new java.util.Random(42);
        for (int step = 0; step < 100; step++) {
            int l = rnd.nextInt(n);
            int r = rnd.nextInt(n - l) + l + 1;
            long add = rnd.nextInt(1000) - 500;
            seg.act(l, r, add);

            long normAdd = (add % mod + mod) % mod;
            for (int i = l; i < r; i++) {
                a[i] = (a[i] + normAdd) % mod;
            }

            int ql = rnd.nextInt(n);
            int qr = rnd.nextInt(n - ql) + ql + 1;
            long[] res = seg.fold(ql, qr);

            long[] exp = new long[k + 1];
            exp[0] = qr - ql;
            for (int j = 1; j <= k; j++) {
                long sum = 0;
                for (int i = ql; i < qr; i++) {
                    long term = 1;
                    for (int m = 0; m < j; m++) term = (term * a[i]) % mod;
                    sum = (sum + term) % mod;
                }
                exp[j] = sum;
            }
            assertArrayEquals(exp, res, "Failed at step " + step);
        }
    }
}
