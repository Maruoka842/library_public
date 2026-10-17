package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;

public class BinaryMatrixMonoidAnalyzerTest {

    @Test
    public void testMatrixOperations() {
        int n = 3;
        BinaryMatrixMonoidAnalyzer.Matrix eye = BinaryMatrixMonoidAnalyzer.Matrix.identity(n);
        assertEquals(3, eye.size());
        assertArrayEquals(new long[] {1L, 2L, 4L}, eye.rows());

        long v = 0b101L; // e_0 + e_2
        assertEquals(0b101L, eye.leftVectorMultiply(v));

        BinaryMatrixMonoidAnalyzer.Matrix m = new BinaryMatrixMonoidAnalyzer.Matrix(3, 0b011L, 0b100L, 0b001L);
        BinaryMatrixMonoidAnalyzer.Matrix prod = eye.multiply(m);
        assertArrayEquals(m.rows(), prod.rows());

        BinaryMatrixMonoidAnalyzer.Matrix mt = m.transpose();
        assertArrayEquals(new long[] {0b101L, 0b001L, 0b010L}, mt.rows());
    }

    @Test
    public void testSubspaceOperations() {
        int n = 3;
        BinaryMatrixMonoidAnalyzer.Subspace full = BinaryMatrixMonoidAnalyzer.Subspace.full(n);
        assertEquals(3, full.ambientDimension());
        assertEquals(3, full.rank());
        assertArrayEquals(new long[] {1L, 2L, 4L}, full.basis());

        BinaryMatrixMonoidAnalyzer.Subspace sub = BinaryMatrixMonoidAnalyzer.Subspace.of(n, 0b110L, 0b011L);
        assertEquals(2, sub.rank());

        long v = 0b101L; // 0b110 ^ 0b011 = 0b101
        long coord = sub.coordinatesOf(v);
        assertEquals(1L, coord);

        assertThrows(IllegalArgumentException.class, () -> sub.coordinatesOf(0b010L));

        BinaryMatrixMonoidAnalyzer.Matrix eye = BinaryMatrixMonoidAnalyzer.Matrix.identity(n);
        BinaryMatrixMonoidAnalyzer.Subspace mult = sub.rightMultiply(eye);
        assertEquals(sub, mult);

        BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix restr = full.restrict(eye);
        assertTrue(restr.isIdentity());

        assertEquals(sub.hashCode(), BinaryMatrixMonoidAnalyzer.Subspace.of(n, 0b110L, 0b011L).hashCode());
        assertNotNull(sub.toString());
    }

    @Test
    public void testBinaryLinearMapOperations() {
        int n = 3;
        BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix id = BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix.identity(n);
        assertEquals(3, id.size());
        assertTrue(id.isIdentity());

        long v = 0b110L;
        assertEquals(v, id.apply(v));

        BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix inv = id.inverse();
        assertEquals(id, inv);

        BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix map = new BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix(3, 0b010L, 0b100L, 0b001L);
        assertFalse(map.isIdentity());
        BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix mapInv = map.inverse();
        assertEquals(id, map.multiply(mapInv));

        assertEquals(map, new BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix(3, 0b010L, 0b100L, 0b001L));
        assertEquals(map.hashCode(), new BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix(3, 0b010L, 0b100L, 0b001L).hashCode());
        assertNotNull(map.toString());

        assertThrows(IllegalArgumentException.class, () -> new BinaryMatrixMonoidAnalyzer.InvertibleBinaryMatrix(3, 0b001L, 0b001L, 0b001L));
    }

    @Test
    public void testXorMergeGenerator() {
        int n = 3;
        BinaryMatrixMonoidAnalyzer.Matrix g0 = BinaryMatrixMonoidAnalyzer.xorMergeGenerator(n, 0);
        assertArrayEquals(new long[] {0b011L, 0b011L, 0b100L}, g0.rows());

        assertThrows(IndexOutOfBoundsException.class, () -> BinaryMatrixMonoidAnalyzer.xorMergeGenerator(n, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> BinaryMatrixMonoidAnalyzer.xorMergeGenerator(n, 2));
    }

    @Test
    public void testColumnSpaceAnalysis() {
        int n = 2;
        BinaryMatrixMonoidAnalyzer.Matrix g0 = BinaryMatrixMonoidAnalyzer.xorMergeGenerator(n, 0);

        BinaryMatrixMonoidAnalyzer analyzer = new BinaryMatrixMonoidAnalyzer(n, g0);
        BinaryMatrixMonoidAnalyzer.RowSpaceAction colAction = analyzer.analyzeColumnSpaces();

        assertNotNull(colAction);
        assertTrue(colAction.size() >= 1);
    }

}
