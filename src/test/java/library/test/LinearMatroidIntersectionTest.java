package library.test;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import library.util.graph.Matroid;
import library.util.graph.MatroidIntersection;
import library.util.collections.IntArrayList;
import library.util.linalg.MatrixUtilsFp;

public class LinearMatroidIntersectionTest {

    private static class LinearMatroid implements Matroid {
        private final long[][] columns; // n x r format
        private final long mod;
        private int[] Iset;

        public LinearMatroid(long[][] columns, long mod) {
            this.columns = columns;
            this.mod = mod;
        }

        @Override
        public int size() {
            return columns.length;
        }

        @Override
        public void set(boolean[] I) {
            int count = 0;
            for (boolean b : I) {
                if (b) count++;
            }
            Iset = new int[count];
            int ptr = 0;
            for (int i = 0; i < I.length; i++) {
                if (I[i]) {
                    Iset[ptr++] = i;
                }
            }
        }

        @Override
        public IntArrayList circuit(int e) {
            int n = columns.length;
            if (n == 0) return new IntArrayList();
            int r = columns[0].length;
            int k = Iset.length;
            if (k == 0) {
                return new IntArrayList();
            }
            // AI is of size r x k (rows are coordinates, columns are variables)
            long[][] AI = new long[r][k];
            for (int i = 0; i < r; i++) {
                for (int j = 0; j < k; j++) {
                    AI[i][j] = columns[Iset[j]][i];
                }
            }
            long[] b = new long[r];
            for (int i = 0; i < r; i++) {
                b[i] = columns[e][i];
            }
            long[][] res = MatrixUtilsFp.solveLinearEquation(AI, b, mod);
            if (res == null) {
                return new IntArrayList();
            }
            long[] x = res[0];
            IntArrayList circuitList = new IntArrayList();
            circuitList.add(e);
            for (int j = 0; j < k; j++) {
                if (x[j] != 0) {
                    circuitList.add(Iset[j]);
                }
            }
            return circuitList;
        }
    }

    @Test
    public void testEmpty() {
        long mod = 998244353;
        long[][] A = new long[0][0];
        long[][] B = new long[0][0];
        assertEquals(0, MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod));
        boolean[] solved = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod);
        assertEquals(0, solved.length);
    }

    @Test
    public void testSimpleTrivial() {
        long mod = 998244353;
        // 1 element, 1-dimensional column vectors: aColumns[0] = {1}, bColumns[0] = {2}
        long[][] A = {{1}};
        long[][] B = {{2}};
        assertEquals(1, MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod));
        boolean[] solved = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod);
        assertArrayEquals(new boolean[]{true}, solved);
    }

    @Test
    public void testSimpleTrivialZero() {
        long mod = 998244353;
        // 1 element, one zero
        long[][] A = {{0}};
        long[][] B = {{2}};
        assertEquals(0, MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod));
        boolean[] solved = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod);
        assertArrayEquals(new boolean[]{false}, solved);
    }

    @Test
    public void testKnownDependent() {
        long mod = 998244353;
        // 3 elements, 2-dimensional column vectors
        long[][] A = {
            {1, 0},
            {0, 1},
            {1, 1}
        };
        long[][] B = {
            {1, 0},
            {0, 1},
            {1, 1}
        };
        // Max size should be 2 since rank of either matrix is 2.
        assertEquals(2, MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod));
        boolean[] solved = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod);
        int count = 0;
        for (boolean b : solved) if (b) count++;
        assertEquals(2, count);

        // Verify that the chosen subset is indeed independent in both A and B.
        long[][] subA = new long[2][2];
        long[][] subB = new long[2][2];
        int colIdx = 0;
        for (int i = 0; i < 3; i++) {
            if (solved[i]) {
                subA[0][colIdx] = A[i][0];
                subA[1][colIdx] = A[i][1];
                subB[0][colIdx] = B[i][0];
                subB[1][colIdx] = B[i][1];
                colIdx++;
            }
        }
        assertEquals(2, MatrixUtilsFp.rank(subA, mod));
        assertEquals(2, MatrixUtilsFp.rank(subB, mod));
    }

    @Test
    public void testDifferentDimensions() {
        long mod = 998244353;
        // 3 elements, A has 2-dimensional columns, B has 3-dimensional columns
        long[][] A = {
            {1, 0},
            {1, 1},
            {0, 1}
        }; // 3 x 2
        long[][] B = {
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1}
        }; // 3 x 3
        // Max common independent set size is 2 (limited by A's dimension).
        assertEquals(2, MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod));
        boolean[] solved = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod);
        int count = 0;
        for (boolean b : solved) if (b) count++;
        assertEquals(2, count);
    }

    @Test
    public void testRandomCompareWithGeneric() {
        Random rnd = new Random(42);
        long mod = 998244353;
        int trials = 40;

        for (int t = 0; t < trials; t++) {
            int r1 = rnd.nextInt(5) + 2;
            int r2 = rnd.nextInt(5) + 2;
            int n = rnd.nextInt(8) + 3;

            // aColumns of size n x r1, bColumns of size n x r2
            long[][] A = new long[n][r1];
            long[][] B = new long[n][r2];

            for (int j = 0; j < n; j++) {
                for (int i = 0; i < r1; i++) {
                    A[j][i] = rnd.nextLong(mod);
                }
            }
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < r2; i++) {
                    B[j][i] = rnd.nextLong(mod);
                }
            }

            // Reference size & solution using generic MatroidIntersection
            LinearMatroid m1 = new LinearMatroid(A, mod);
            LinearMatroid m2 = new LinearMatroid(B, mod);

            boolean[] expectedI = MatroidIntersection.solve(m1, m2);
            int expectedSize = 0;
            for (boolean b : expectedI) if (b) expectedSize++;

            // Our size & solution
            int actualSize = MatrixUtilsFp.linearMatroidIntersectionSize(A, B, mod, rnd.nextLong());
            assertEquals(expectedSize, actualSize, "Size mismatch on trial " + t);

            boolean[] actualI = MatrixUtilsFp.linearMatroidIntersectionSolve(A, B, mod, rnd.nextLong());
            int actualIcount = 0;
            for (boolean b : actualI) if (b) actualIcount++;
            assertEquals(expectedSize, actualIcount, "Recovered size mismatch on trial " + t);

            // Verify independence of actualI
            if (actualIcount > 0) {
                // To compute rank of submatrix, we construct r1 x actualIcount and r2 x actualIcount matrices
                long[][] subA = new long[r1][actualIcount];
                long[][] subB = new long[r2][actualIcount];
                int colIdx = 0;
                for (int i = 0; i < n; i++) {
                    if (actualI[i]) {
                        for (int r = 0; r < r1; r++) subA[r][colIdx] = A[i][r];
                        for (int r = 0; r < r2; r++) subB[r][colIdx] = B[i][r];
                        colIdx++;
                    }
                }
                assertEquals(actualIcount, MatrixUtilsFp.rank(subA, mod), "A submatrix must be independent");
                assertEquals(actualIcount, MatrixUtilsFp.rank(subB, mod), "B submatrix must be independent");
            }
        }
    }
}
