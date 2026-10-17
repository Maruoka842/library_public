package library.util.linalg;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatrixUtilsZTest {

    @Test
    public void testDetEmpty() {
        long[][] a = new long[0][0];
        assertEquals(1, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDet1x1() {
        long[][] a = {{7}};
        assertEquals(7, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDet2x2() {
        long[][] a = {
            {1, 2},
            {3, 4}
        };
        // 1*4 - 2*3 = -2
        assertEquals(-2, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDet3x3() {
        long[][] a = {
            {1, 2, 3},
            {0, 4, 5},
            {1, 0, 6}
        };
        // 1*(24-0) - 2*(0-5) + 3*(0-4) = 24 + 10 - 12 = 22
        assertEquals(22, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDet4x4Identity() {
        long[][] a = MatrixUtilsZ.longMatrixIdentity(4);
        assertEquals(1, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDet4x4Triangular() {
        long[][] a = {
            {2, 3, 5, 7},
            {0, 3, 11, 13},
            {0, 0, 5, 17},
            {0, 0, 0, 7}
        };
        // Product of diagonal: 2 * 3 * 5 * 7 = 210
        assertEquals(210, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDetSingular() {
        long[][] a = {
            {1, 2, 3, 4},
            {2, 4, 6, 8},
            {1, 0, 1, 0},
            {0, 1, 0, 1}
        };
        assertEquals(0, MatrixUtilsZ.det(a));
    }

    @Test
    public void testDetNonSquare() {
        long[][] a = {
            {1, 2, 3},
            {4, 5, 6}
        };
        assertEquals(0, MatrixUtilsZ.det(a));

        long[][] b = {
            {1, 2},
            {3, 4},
            {5, 6}
        };
        assertEquals(0, MatrixUtilsZ.det(b));
    }
}
