package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class DynamicMatrixRankDecompositionFpTest {

    private static final long MOD = 998244353L;

    @Test
    public void testEmptyAndZeroMod() {
        // Test with 0x0 matrices or small cases
        long[][] empty = new long[0][0];
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(empty, MOD);
        assertEquals(0, d.rows());
        assertEquals(0, d.columns());
        assertEquals(0, d.rank());
        assertEquals(0, d.nullity());
        assertEquals(0, d.leftNullity());

        assertThrows(IllegalArgumentException.class, () -> new DynamicMatrixRankDecompositionFp(empty, 1));
        assertThrows(IllegalArgumentException.class, () -> new DynamicMatrixRankDecompositionFp(empty, -5));
        assertThrows(IllegalArgumentException.class, () -> new DynamicMatrixRankDecompositionFp(-1, 0, empty, MOD));
    }

    @Test
    public void testRankDecompositionConstructor() {
        long[][] mat = {
            {1, 2, 3},
            {2, 4, 6},
            {3, 8, 7}
        };
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, MOD);
        assertEquals(3, d.rows());
        assertEquals(3, d.columns());
        assertEquals(2, d.rank());
        assertEquals(1, d.nullity());
        assertEquals(1, d.leftNullity());

        // Pivot columns and rows check
        int[] rows = d.pivotRows();
        int[] cols = d.pivotColumns();
        assertEquals(2, rows.length);
        assertEquals(2, cols.length);

        // Basis size check
        long[][] colBasis = d.columnSpaceBasis();
        long[][] rowBasis = d.rowSpaceBasis();
        assertEquals(2, colBasis.length);
        assertEquals(2, rowBasis.length);

        // Kernel check
        long[][] ker = d.kernelBasis();
        assertEquals(1, ker.length); // nullity = 1
        assertEquals(3, ker[0].length);

        // left kernel check
        long[][] leftKer = d.leftKernelBasis();
        assertEquals(1, leftKer.length); // leftNullity = 1
        assertEquals(3, leftKer[0].length);
    }

    @Test
    public void testGetAndImplicitMatrix() {
        long[][] mat = {
            {1, 2, 3},
            {2, 4, 6},
            {3, 8, 7}
        };
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, MOD);

        // Check that each individual element is matched via get(i, j)
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                assertEquals(mat[i][j], d.get(i, j));
            }
        }

        // Test out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> d.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> d.get(0, 3));
    }

    @Test
    public void testSpaceMembershipAndSolve() {
        long[][] mat = {
            {1, 2, 3},
            {2, 4, 6},
            {3, 8, 7}
        };
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, MOD);

        // Column space check
        long[] vecInCol = {3, 6, 9}; // 3 * col 0
        assertTrue(d.isInColumnSpace(vecInCol));
        long[] coordsCol = d.columnSpaceCoordinates(vecInCol);
        assertNotNull(coordsCol);

        long[] vecOutCol = {1, 1, 1};
        assertFalse(d.isInColumnSpace(vecOutCol));
        assertNull(d.columnSpaceCoordinates(vecOutCol));

        // Row space check
        long[] vecInRow = {4, 10, 10}; // row 0 + row 2
        assertTrue(d.isInRowSpace(vecInRow));
        long[] coordsRow = d.rowSpaceCoordinates(vecInRow);
        assertNotNull(coordsRow);

        long[] vecOutRow = {1, 0, 0};
        assertFalse(d.isInRowSpace(vecOutRow));
        assertNull(d.rowSpaceCoordinates(vecOutRow));

        // Solve check
        // Let's make an explicit rhs: 2 * col 0 + 1 * col 1
        long[] rhs = {
            (2*1 + 1*2) % MOD,
            (2*2 + 1*4) % MOD,
            (2*3 + 1*8) % MOD
        };
        long[] sol = d.solve(rhs);
        assertNotNull(sol);
        // Verify sol satisfies M * sol = rhs (mod MOD)
        long[] computedRhs = new long[3];
        for (int i = 0; i < 3; ++i) {
            long val = 0;
            for (int j = 0; j < 3; ++j) {
                val = (val + d.get(i, j) * sol[j]) % MOD;
            }
            computedRhs[i] = val;
        }
        assertArrayEquals(rhs, computedRhs);

        // Unsolvable system
        long[] unsolvableRhs = {1, 1, 1};
        assertNull(d.solve(unsolvableRhs));
    }

    @Test
    public void testRankOneUpdateAddOuterProduct() {
        // Start with a zero matrix
        long[][] mat = new long[3][3];
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, MOD);
        assertEquals(0, d.rank());

        // Update with rank-1: coefficient = 2, left = {1, 2, 3}, right = {3, 2, 1}
        long[] left = {1, 2, 3};
        long[] right = {3, 2, 1};
        int newRank = d.addOuterProduct(left, right, 2);
        assertEquals(1, newRank);
        assertEquals(1, d.rank());

        // Expected matrix:
        // [ 2*1*3, 2*1*2, 2*1*1 ] = [ 6, 4, 2 ]
        // [ 2*2*3, 2*2*2, 2*2*1 ] = [ 12, 8, 4 ]
        // [ 2*3*3, 2*3*2, 2*3*1 ] = [ 18, 12, 6 ]
        long[][] expected = {
            {6, 4, 2},
            {12, 8, 4},
            {18, 12, 6}
        };
        long[][] actual = d.toArray();
        for (int i = 0; i < 3; ++i) {
            assertArrayEquals(expected[i], actual[i]);
        }
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                assertEquals(expected[i][j], d.get(i, j));
            }
        }

        // Add another outer product to increase rank to 2
        long[] left2 = {1, 0, 1};
        long[] right2 = {0, 1, 0};
        newRank = d.addOuterProduct(left2, right2, 5);
        assertEquals(2, newRank);

        // Verification of properties on updated matrix
        assertTrue(d.rank() <= 2);
        long[][] updatedMat = d.toArray();
        // Construct another instance using the updated state to confirm initial decomp is identical
        DynamicMatrixRankDecompositionFp dVerify = new DynamicMatrixRankDecompositionFp(updatedMat, MOD);
        assertEquals(dVerify.rank(), d.rank());
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                assertEquals(dVerify.get(i, j), d.get(i, j));
            }
        }
    }

    @Test
    public void testTryAddOuterProductPreservingRank() {
        long[][] mat = {
            {1, 0},
            {0, 1}
        };
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, MOD);
        assertEquals(2, d.rank());

        // Any rank-1 addition that would increase rank is impossible since max rank is 2.
        // What about one that preserves rank?
        // Let's add something: left = {1, 0}, right = {1, 0}, coeff = 1.
        long[] left = {1, 0};
        long[] right = {1, 0};

        // This preserves rank since the image and preimage are within the column/row spaces
        int expectedRank = d.rankAfterOuterProduct(left, right, 1);
        assertEquals(2, expectedRank);

        boolean success = d.tryAddOuterProductPreservingRank(left, right, 1);
        assertTrue(success);
        assertEquals(2, d.rank());

        // Let's try to increase rank on a singular matrix
        long[][] matSingular = {
            {1, 0},
            {0, 0}
        };
        DynamicMatrixRankDecompositionFp dSingular = new DynamicMatrixRankDecompositionFp(matSingular, MOD);
        assertEquals(1, dSingular.rank());

        // Adding left={0, 1}, right={0, 1}, coeff=1 should increase rank to 2.
        long[] leftInc = {0, 1};
        long[] rightInc = {0, 1};

        // tryAddOuterProductPreservingRank should return false and not mutate the state
        boolean successInc = dSingular.tryAddOuterProductPreservingRank(leftInc, rightInc, 1);
        assertFalse(successInc);
        assertEquals(1, dSingular.rank());
        assertArrayEquals(matSingular[0], dSingular.toArray()[0]);
        assertArrayEquals(matSingular[1], dSingular.toArray()[1]);
    }

    @Test
    public void testFpArithmeticEdgeCases() {
        // Small prime modulus to stress test divisions and exactness
        long smallMod = 7;
        long[][] mat = {
            {3, 5},
            {1, 2}
        };
        // det = 3*2 - 5*1 = 1 = 1 (mod 7). invertible.
        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, smallMod);
        assertEquals(2, d.rank());

        // Solve a system
        long[] rhs = {4, 2};
        long[] sol = d.solve(rhs);
        assertNotNull(sol);

        // Check solution
        long[] check = new long[2];
        check[0] = (d.get(0, 0) * sol[0] + d.get(0, 1) * sol[1]) % smallMod;
        check[1] = (d.get(1, 0) * sol[0] + d.get(1, 1) * sol[1]) % smallMod;
        assertArrayEquals(rhs, check);

        // Perform singular operations
        // rank decrease test
        // Add something that cancels the second row:
        // row 1 is {1, 2}. To cancel it, add -1 * {1, 2} outer product?
        // Better: left = {0, 1}, right = {6, 5}, coeff = 1.
        // right: {6, 5} = {-1, -2} mod 7.
        // This should cancel the second row completely.
        long[] leftCancel = {0, 1};
        long[] rightCancel = {6, 5};
        int rankAfterCancel = d.addOuterProduct(leftCancel, rightCancel, 1);
        assertEquals(1, rankAfterCancel);
        assertEquals(1, d.rank());

        long[][] updated = d.toArray();
        assertArrayEquals(new long[]{3, 5}, updated[0]);
        assertArrayEquals(new long[]{0, 0}, updated[1]);
    }

    @Test
    public void testRandomizedStress() {
        Random rng = new Random(42);
        int rows = 10;
        int cols = 12;
        long prime = 998244353L;

        long[][] mat = new long[rows][cols];
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                mat[i][j] = rng.nextInt((int) prime);
            }
        }

        DynamicMatrixRankDecompositionFp d = new DynamicMatrixRankDecompositionFp(mat, prime);
        DynamicMatrixRankFp facade = new DynamicMatrixRankFp(mat, prime);
        assertEquals(d.rank(), facade.rank());

        // Perform 50 random rank-1 updates
        for (int step = 0; step < 50; ++step) {
            long[] left = new long[rows];
            long[] right = new long[cols];
            for (int i = 0; i < rows; ++i) left[i] = rng.nextInt((int) prime);
            for (int j = 0; j < cols; ++j) right[j] = rng.nextInt((int) prime);
            long coeff = rng.nextInt((int) prime);

            // Compute expected updated matrix manually
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    long added = (coeff * left[i]) % prime;
                    added = (added * right[j]) % prime;
                    mat[i][j] = (mat[i][j] + added) % prime;
                }
            }

            // Update d and facade
            int updatedRank = d.addOuterProduct(left, right, coeff);
            int facadeRank = facade.addOuterProduct(left, right, coeff);
            assertEquals(updatedRank, facadeRank);

            // Verify with fresh reconstruction
            DynamicMatrixRankDecompositionFp dExpected = new DynamicMatrixRankDecompositionFp(mat, prime);
            assertEquals(dExpected.rank(), d.rank());

            // Check elements via get(i, j)
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    assertEquals(mat[i][j], d.get(i, j));
                }
            }

            // Spot check bases and solutions
            if (d.rank() > 0) {
                long[][] colBasis = d.columnSpaceBasis();
                assertEquals(d.rank(), colBasis.length);

                // Random vector in column space
                long[] alpha = new long[d.rank()];
                for (int i = 0; i < d.rank(); ++i) alpha[i] = rng.nextInt((int) prime);

                long[] vecIn = new long[rows];
                for (int i = 0; i < rows; ++i) {
                    long sum = 0;
                    for (int k = 0; k < d.rank(); ++k) {
                        sum = (sum + mat[i][d.pivotColumns()[k]] * alpha[k]) % prime;
                    }
                    vecIn[i] = sum;
                }

                assertTrue(d.isInColumnSpace(vecIn));
                long[] sol = d.solve(vecIn);
                assertNotNull(sol);

                // Verify sol works
                long[] computed = new long[rows];
                for (int i = 0; i < rows; ++i) {
                    long sum = 0;
                    for (int j = 0; j < cols; ++j) {
                        sum = (sum + mat[i][j] * sol[j]) % prime;
                    }
                    computed[i] = sum;
                }
                assertArrayEquals(vecIn, computed);
            }
        }
    }
}
