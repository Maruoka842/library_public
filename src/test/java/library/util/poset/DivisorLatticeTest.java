package library.util.poset;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;

public class DivisorLatticeTest {
	@Test
	public void testStrictZetaFromZeta() {
		long[] f = { 0, 3, -2, 5, 7, -11, 13, 17, -19, 23 };
		long[] expected = new long[f.length];
		for (int x = 1; x < f.length; ++x) {
			for (int d = 1; d < x; ++d) {
				if (x % d == 0) expected[x] += f[d];
			}
		}

		long[] g = DivisorLattice.zeta(f);
		long[] original = g.clone();
		assertArrayEquals(expected, DivisorLattice.strictZetaFromZeta(g));
		assertArrayEquals(original, g);
	}

	@Test
	public void testSupsetStrictZetaFromZeta() {
		long[] f = { 0, 3, -2, 5, 7, -11, 13, 17, -19, 23 };
		long[] g = new long[f.length];
		long[] expected = new long[f.length];
		int top = f.length - 1;
		g[top] = f[top];
		for (int x = 1; x < top; ++x) {
			g[x] = f[top];
			for (int m = x; m < top; m += x) g[x] += f[m];
			expected[x] = g[x] - f[x];
		}

		long[] original = g.clone();
		assertArrayEquals(expected, DivisorLattice.supsetStrictZetaFromZeta(g));
		assertArrayEquals(original, g);
	}

	@Test
	public void testStrictZetaFromZetaModulo() {
		long mod = 11;
		long[] f = { 0, 3, 9, 5, 7, 0, 2, 6, 4, 8 };
		long[] lower = DivisorLattice.zeta(f, (x, y) -> (x + y) % mod);
		long[] upper = DivisorLattice.supsetZeta(f, (x, y) -> (x + y) % mod);
		long[] expectedLower = new long[f.length];
		long[] expectedUpper = new long[f.length];
		for (int x = 1; x < f.length; ++x) {
			for (int y = 1; y < f.length; ++y) {
				if (y != x && x % y == 0) expectedLower[x] = (expectedLower[x] + f[y]) % mod;
				if (y != x && y % x == 0) expectedUpper[x] = (expectedUpper[x] + f[y]) % mod;
			}
		}

		assertArrayEquals(expectedLower, DivisorLattice.strictZetaFromZeta(lower, mod));
		assertArrayEquals(expectedUpper, DivisorLattice.supsetStrictZetaFromZeta(upper, mod));
	}

    private static long[] naiveGcdConvolutionOrdered(long[] a, long[] b) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = i; j < n; j++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                c[g] += a[i] * b[j];
            }
        }
        return c;
    }

    private static long[] naiveGcdConvolutionOrdered(long[] a, long[] b, long mod) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = i; j < n; j++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                long term = (a[i] % mod + mod) % mod * ((b[j] % mod + mod) % mod) % mod;
                c[g] = (c[g] + term) % mod;
            }
        }
        return c;
    }

    private static long[] naiveGcdConvolutionOrderedTranspose(long[] f, long[] h) {
        int n = f.length;
        long[] d = new long[n];
        for (int j = 1; j < n; j++) {
            long sum = 0;
            for (int i = 1; i <= j; i++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                sum += f[i] * h[g];
            }
            d[j] = sum;
        }
        return d;
    }

    private static long[] naiveGcdConvolutionOrderedTranspose(long[] f, long[] h, long mod) {
        int n = f.length;
        long[] d = new long[n];
        for (int j = 1; j < n; j++) {
            long sum = 0;
            for (int i = 1; i <= j; i++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                long term = (f[i] % mod + mod) % mod * ((h[g] % mod + mod) % mod) % mod;
                sum = (sum + term) % mod;
            }
            d[j] = sum;
        }
        return d;
    }

    private static long[] naiveGcdConvolutionOrdered(int[] A, int[] B, long[] f, long[] g) {
        int n = A.length;
        int V = 0;
        for (int x : A) V = Math.max(V, x);
        for (int x : B) V = Math.max(V, x);
        long[] C = new long[V + 1];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                int gVal = (int) library.util.MathUtils.gcd(A[i], B[j]);
                C[gVal] += f[i] * g[j];
            }
        }
        return C;
    }

    private static long[] naiveGcdConvolutionOrdered(int[] A, int[] B, long[] f, long[] g, long mod) {
        int n = A.length;
        int V = 0;
        for (int x : A) V = Math.max(V, x);
        for (int x : B) V = Math.max(V, x);
        long[] C = new long[V + 1];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                int gVal = (int) library.util.MathUtils.gcd(A[i], B[j]);
                long term = (f[i] % mod + mod) % mod * ((g[j] % mod + mod) % mod) % mod;
                C[gVal] = (C[gVal] + term) % mod;
            }
        }
        return C;
    }

    private static long[] naiveGcdConvolutionOrderedTranspose(int[] A, int[] B, long[] f, long[] h) {
        int n = A.length;
        long[] D = new long[n];
        for (int j = 0; j < n; j++) {
            long sum = 0;
            for (int i = 0; i <= j; i++) {
                int gVal = (int) library.util.MathUtils.gcd(A[i], B[j]);
                sum += f[i] * h[gVal];
            }
            D[j] = sum;
        }
        return D;
    }

    private static long[] naiveGcdConvolutionOrderedTranspose(int[] A, int[] B, long[] f, long[] h, long mod) {
        int n = A.length;
        long[] D = new long[n];
        for (int j = 0; j < n; j++) {
            long sum = 0;
            for (int i = 0; i <= j; i++) {
                int gVal = (int) library.util.MathUtils.gcd(A[i], B[j]);
                long term = (f[i] % mod + mod) % mod * ((h[gVal] % mod + mod) % mod) % mod;
                sum = (sum + term) % mod;
            }
            D[j] = sum;
        }
        return D;
    }

    private static long[] naiveGcdConvolution(long[] a, long[] b) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                c[g] += a[i] * b[j];
            }
        }
        return c;
    }

    private static long[] naiveGcdConvolution(long[] a, long[] b, long mod) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                int g = (int) library.util.MathUtils.gcd(i, j);
                long term = (a[i] % mod + mod) % mod * ((b[j] % mod + mod) % mod) % mod;
                c[g] = (c[g] + term) % mod;
            }
        }
        return c;
    }

    private static long[] naiveLcmConvolution(long[] a, long[] b) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                long l = library.util.MathUtils.lcm(i, j);
                if (l < n) {
                    c[(int) l] += a[i] * b[j];
                }
            }
        }
        return c;
    }

    private static long[] naiveLcmConvolution(long[] a, long[] b, long mod) {
        int n = a.length;
        long[] c = new long[n];
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < n; j++) {
                long l = library.util.MathUtils.lcm(i, j);
                if (l < n) {
                    long term = (a[i] % mod + mod) % mod * ((b[j] % mod + mod) % mod) % mod;
                    c[(int) l] = (c[(int) l] + term) % mod;
                }
            }
        }
        return c;
    }

    private static void normalizeArray(long[] arr, long mod) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (arr[i] % mod + mod) % mod;
        }
    }

    @Test
    public void testGcdConvolutionOrderedCorrectness() {
        Random rnd = new Random(42);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        for (int size : sizes) {
            long[] a = new long[size];
            long[] b = new long[size];
            for (int i = 1; i < size; i++) {
                a[i] = rnd.nextInt(200) - 100;
                b[i] = rnd.nextInt(200) - 100;
            }

            long[] expected = naiveGcdConvolutionOrdered(a, b);
            long[] actual = DivisorLattice.gcdConvolutionOrdered(a, b);

            assertArrayEquals(expected, actual, "Failed standard on size " + size);
        }
    }

    @Test
    public void testGcdConvolutionOrderedModuloCorrectness() {
        Random rnd = new Random(43);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        long[] moduli = {2, 7, 101, 998244353L, 1000000007L};
        for (long mod : moduli) {
            for (int size : sizes) {
                long[] a = new long[size];
                long[] b = new long[size];
                for (int i = 1; i < size; i++) {
                    a[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                    b[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveGcdConvolutionOrdered(a, b, mod);
                long[] actual = DivisorLattice.gcdConvolutionOrdered(a, b, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed modulo on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testGcdConvolutionOrderedTransposeCorrectness() {
        Random rnd = new Random(101);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        for (int size : sizes) {
            long[] f = new long[size];
            long[] h = new long[size];
            for (int i = 1; i < size; i++) {
                f[i] = rnd.nextInt(200) - 100;
                h[i] = rnd.nextInt(200) - 100;
            }

            long[] expected = naiveGcdConvolutionOrderedTranspose(f, h);
            long[] actual = DivisorLattice.gcdConvolutionOrderedTranspose(f, h);

            assertArrayEquals(expected, actual, "Failed transpose standard on size " + size);
        }
    }

    @Test
    public void testGcdConvolutionOrderedTransposeModuloCorrectness() {
        Random rnd = new Random(102);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        long[] moduli = {2, 7, 101, 998244353L, 1000000007L};
        for (long mod : moduli) {
            for (int size : sizes) {
                long[] f = new long[size];
                long[] h = new long[size];
                for (int i = 1; i < size; i++) {
                    f[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                    h[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveGcdConvolutionOrderedTranspose(f, h, mod);
                long[] actual = DivisorLattice.gcdConvolutionOrderedTranspose(f, h, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed transpose modulo on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testGeneralizedGcdConvolutionOrderedCorrectness() {
        Random rnd = new Random(201);
        int[] sizes = {2, 5, 20, 50};
        for (int size : sizes) {
            int[] A = new int[size];
            int[] B = new int[size];
            long[] f = new long[size];
            long[] g = new long[size];
            for (int i = 0; i < size; i++) {
                A[i] = rnd.nextInt(100) + 1;
                B[i] = rnd.nextInt(100) + 1;
                f[i] = rnd.nextInt(200) - 100;
                g[i] = rnd.nextInt(200) - 100;
            }

            long[] expected = naiveGcdConvolutionOrdered(A, B, f, g);
            long[] actual = DivisorLattice.gcdConvolutionOrdered(A, B, f, g);

            assertArrayEquals(expected, actual, "Failed generalized on size " + size);
        }
    }

    @Test
    public void testGeneralizedGcdConvolutionOrderedModuloCorrectness() {
        Random rnd = new Random(202);
        int[] sizes = {2, 5, 20, 50};
        long[] moduli = {2, 7, 998244353L};
        for (long mod : moduli) {
            for (int size : sizes) {
                int[] A = new int[size];
                int[] B = new int[size];
                long[] f = new long[size];
                long[] g = new long[size];
                for (int i = 0; i < size; i++) {
                    A[i] = rnd.nextInt(50) + 1;
                    B[i] = rnd.nextInt(50) + 1;
                    f[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                    g[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveGcdConvolutionOrdered(A, B, f, g, mod);
                long[] actual = DivisorLattice.gcdConvolutionOrdered(A, B, f, g, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed generalized modulo on size " + size);
            }
        }
    }

    @Test
    public void testGeneralizedGcdConvolutionOrderedTransposeCorrectness() {
        Random rnd = new Random(203);
        int[] sizes = {2, 5, 20, 50};
        for (int size : sizes) {
            int[] A = new int[size];
            int[] B = new int[size];
            long[] f = new long[size];
            long[] h = new long[150]; // h needs to cover max gcd values, which is at most max(A, B)
            for (int i = 0; i < size; i++) {
                A[i] = rnd.nextInt(100) + 1;
                B[i] = rnd.nextInt(100) + 1;
                f[i] = rnd.nextInt(200) - 100;
            }
            for (int i = 0; i < h.length; i++) {
                h[i] = rnd.nextInt(200) - 100;
            }

            long[] expected = naiveGcdConvolutionOrderedTranspose(A, B, f, h);
            long[] actual = DivisorLattice.gcdConvolutionOrderedTranspose(A, B, f, h);

            assertArrayEquals(expected, actual, "Failed generalized transpose on size " + size);
        }
    }

    @Test
    public void testGeneralizedGcdConvolutionOrderedTransposeModuloCorrectness() {
        Random rnd = new Random(204);
        int[] sizes = {2, 5, 20, 50};
        long[] moduli = {2, 7, 998244353L};
        for (long mod : moduli) {
            for (int size : sizes) {
                int[] A = new int[size];
                int[] B = new int[size];
                long[] f = new long[size];
                long[] h = new long[150];
                for (int i = 0; i < size; i++) {
                    A[i] = rnd.nextInt(50) + 1;
                    B[i] = rnd.nextInt(50) + 1;
                    f[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }
                for (int i = 0; i < h.length; i++) {
                    h[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveGcdConvolutionOrderedTranspose(A, B, f, h, mod);
                long[] actual = DivisorLattice.gcdConvolutionOrderedTranspose(A, B, f, h, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed generalized transpose modulo on size " + size);
            }
        }
    }

    @Test
    public void testExistingGcdConvolutionCorrectness() {
        Random rnd = new Random(44);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        for (int size : sizes) {
            long[] a = new long[size];
            long[] b = new long[size];
            for (int i = 1; i < size; i++) {
                a[i] = rnd.nextInt(200) - 100;
                b[i] = rnd.nextInt(200) - 100;
            }

            long[] expected = naiveGcdConvolution(a, b);
            long[] actual = DivisorLattice.gcdConvolution(a, b);

            assertArrayEquals(expected, actual, "Failed existing gcdConvolution on size " + size);
        }
    }

    @Test
    public void testExistingGcdConvolutionModuloCorrectness() {
        Random rnd = new Random(45);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        long[] moduli = {2, 7, 101, 998244353L, 1000000007L};
        for (long mod : moduli) {
            for (int size : sizes) {
                long[] a = new long[size];
                long[] b = new long[size];
                for (int i = 1; i < size; i++) {
                    a[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                    b[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveGcdConvolution(a, b, mod);
                long[] actual = DivisorLattice.gcdConvolution(a, b, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed existing modulo gcdConvolution on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testExistingLcmConvolutionCorrectness() {
        Random rnd = new Random(46);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        for (int size : sizes) {
            long[] a = new long[size];
            long[] b = new long[size];
            for (int i = 1; i < size; i++) {
                a[i] = rnd.nextInt(10);
                b[i] = rnd.nextInt(10);
            }

            long[] expected = naiveLcmConvolution(a, b);
            long[] actual = DivisorLattice.lcmConvolution(a, b);

            assertArrayEquals(expected, actual, "Failed existing lcmConvolution on size " + size);
        }
    }

    @Test
    public void testExistingLcmConvolutionModuloCorrectness() {
        Random rnd = new Random(47);
        int[] sizes = {2, 3, 5, 10, 20, 50, 100};
        long[] moduli = {2, 7, 101, 998244353L, 1000000007L};
        for (long mod : moduli) {
            for (int size : sizes) {
                long[] a = new long[size];
                long[] b = new long[size];
                for (int i = 1; i < size; i++) {
                    a[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                    b[i] = rnd.nextInt((int) Math.min(mod, 1000)) - 500;
                }

                long[] expected = naiveLcmConvolution(a, b, mod);
                long[] actual = DivisorLattice.lcmConvolution(a, b, mod);

                normalizeArray(expected, mod);
                normalizeArray(actual, mod);

                assertArrayEquals(expected, actual, "Failed existing modulo lcmConvolution on size " + size + " with mod " + mod);
            }
        }
    }

    @Test
    public void testAssertionErrors() {
        // test a[0] != 0
        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{1, 0}, new long[]{0, 0});
        });

        // test b[0] != 0
        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{0, 0}, new long[]{1, 0});
        });

        // test length mismatch
        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{0, 0}, new long[]{0, 0, 0});
        });

        // modulo versions
        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{1, 0}, new long[]{0, 0}, 7);
        });

        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{0, 0}, new long[]{1, 0}, 7);
        });

        assertThrows(AssertionError.class, () -> {
            DivisorLattice.gcdConvolutionOrdered(new long[]{0, 0}, new long[]{0, 0, 0}, 7);
        });
    }
}
