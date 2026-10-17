package library.util.matrix;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.Random;

public class MongeCoreTest {

    @Test
    public void testMongeConeAndMatrixEvaluations() {
        int H = 5;
        int W = 6;
        MongeCone cone = new MongeCone(H, W);
        assertEquals(H, cone.rows());
        assertEquals(W, cone.cols());
        assertEquals(0, cone.atomCount());

        cone.addRow(1, 10);
        cone.addColumn(2, 5);
        cone.addNorthEast(3, 2, 7);
        cone.addSouthWest(2, 4, 3);

        assertTrue(cone.atomCount() > 0);

        MongeCoreLong core = cone.buildCore();
        assertEquals(H, core.rows());
        assertEquals(W, core.cols());

        DenseMongeLong dense = cone.buildDense();
        IndexedMongeLong indexed = cone.buildIndexed();

        assertEquals(core.rows(), indexed.core().rows());
        assertEquals(core.cols(), indexed.core().cols());
        assertEquals(core.atomCount(), indexed.core().atomCount());
        assertEquals(H, dense.rows());
        assertEquals(W, dense.cols());
        assertEquals(H, indexed.rows());
        assertEquals(W, indexed.cols());

        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                long valDense = dense.get(i, j);
                long valIndexed = indexed.get(i, j);
                assertEquals(valDense, valIndexed, "Mismatch at (" + i + ", " + j + ")");
            }
        }

        long[][] grid = new long[H][W];
        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                grid[i][j] = dense.get(i, j);
            }
        }
        assertTrue(MongeMatrices.isMonge(grid), "Generated dense matrix should satisfy Monge property");
    }

    @Test
    public void testAntiMongeConeAndMatrixEvaluations() {
        int H = 6;
        int W = 5;
        AntiMongeCone cone = new AntiMongeCone(H, W);
        assertEquals(H, cone.rows());
        assertEquals(W, cone.cols());

        cone.addRow(0, 4);
        cone.addColumn(3, 8);
        cone.addNorthWest(4, 3, 6);
        cone.addSouthEast(2, 1, 9);

        AntiMongeCoreLong core = cone.buildCore();
        DenseAntiMongeLong dense = cone.buildDense();
        IndexedAntiMongeLong indexed = cone.buildIndexed();

        assertEquals(core.rows(), indexed.core().rows());
        assertEquals(core.cols(), indexed.core().cols());
        assertEquals(core.atomCount(), indexed.core().atomCount());
        assertEquals(H, dense.rows());
        assertEquals(W, dense.cols());
        assertEquals(H, indexed.rows());
        assertEquals(W, indexed.cols());

        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                long valDense = dense.get(i, j);
                long valIndexed = indexed.get(i, j);
                assertEquals(valDense, valIndexed, "Mismatch at (" + i + ", " + j + ")");
            }
        }

        // Verify anti-Monge property: A[i][j] + A[i+1][j+1] >= A[i][j+1] + A[i+1][j]
        for (int i = 0; i + 1 < H; i++) {
            for (int j = 0; j + 1 < W; j++) {
                long sumDiag = dense.get(i, j) + dense.get(i + 1, j + 1);
                long sumOff = dense.get(i, j + 1) + dense.get(i + 1, j);
                assertTrue(sumDiag >= sumOff, "Anti-Monge property failed at (" + i + ", " + j + ")");
            }
        }
    }

    @Test
    public void testMongeCoreAlgorithmsRowMinimaAndMaxima() {
        int H = 10;
        int W = 12;
        MongeCone cone = new MongeCone(H, W);
        Random rand = new Random(12345);

        for (int i = 0; i < H; i++) {
            cone.addRow(i, rand.nextInt(50));
        }
        for (int j = 0; j < W; j++) {
            cone.addColumn(j, rand.nextInt(50));
        }
        for (int k = 0; k < 15; k++) {
            int re = 1 + rand.nextInt(H);
            int cb = rand.nextInt(W);
            cone.addNorthEast(re, cb, rand.nextInt(30));

            int rb = rand.nextInt(H);
            int ce = 1 + rand.nextInt(W);
            cone.addSouthWest(rb, ce, rand.nextInt(30));
        }

        MongeCoreLong core = cone.buildCore();
        DenseMongeLong dense = cone.buildDense();

        MongeCoreAlgorithms.RowExtrema minExtrema = MongeCoreAlgorithms.rowMinima(core);
        MongeCoreAlgorithms.RowExtrema maxExtrema = MongeCoreAlgorithms.rowMaxima(core);

        assertEquals(H, minExtrema.value.length);
        assertEquals(H, minExtrema.arg.length);

        for (int i = 0; i < H; i++) {
            long expectedMin = Long.MAX_VALUE;
            int expectedArgMin = -1;
            long expectedMax = Long.MIN_VALUE;
            int expectedArgMax = -1;

            for (int j = 0; j < W; j++) {
                long val = dense.get(i, j);
                if (val < expectedMin) {
                    expectedMin = val;
                    expectedArgMin = j;
                }
                if (val > expectedMax) {
                    expectedMax = val;
                    expectedArgMax = j;
                }
            }

            assertEquals(expectedMin, minExtrema.value[i], "Row " + i + " min value mismatch");
            assertEquals(expectedArgMin, minExtrema.arg[i], "Row " + i + " min arg mismatch");
            assertEquals(expectedMax, maxExtrema.value[i], "Row " + i + " max value mismatch");
            assertEquals(expectedArgMax, maxExtrema.arg[i], "Row " + i + " max arg mismatch");
        }
    }

    @Test
    public void testMongeCoreAlgorithmsRangeExtrema() {
        int H = 8;
        int W = 10;
        MongeCone cone = new MongeCone(H, W);
        Random rand = new Random(6789);

        for (int k = 0; k < 10; k++) {
            cone.addNorthEast(1 + rand.nextInt(H), rand.nextInt(W), rand.nextInt(20));
        }

        MongeCoreLong core = cone.buildCore();
        DenseMongeLong dense = cone.buildDense();

        int[] from = new int[H];
        int[] to = new int[H];
        for (int i = 0; i < H; i++) {
            from[i] = rand.nextInt(W);
            to[i] = from[i] + rand.nextInt(W + 1 - from[i]);
        }

        MongeCoreAlgorithms.RowExtrema minExtrema = MongeCoreAlgorithms.rowMinima(core, from, to);
        MongeCoreAlgorithms.RowExtrema maxExtrema = MongeCoreAlgorithms.rowMaxima(core, from, to);

        for (int i = 0; i < H; i++) {
            if (from[i] == to[i]) {
                assertEquals(Long.MAX_VALUE, minExtrema.value[i]);
                assertEquals(-1, minExtrema.arg[i]);
                assertEquals(Long.MIN_VALUE, maxExtrema.value[i]);
                assertEquals(-1, maxExtrema.arg[i]);
            } else {
                long expectedMin = Long.MAX_VALUE;
                int expectedArgMin = -1;
                long expectedMax = Long.MIN_VALUE;
                int expectedArgMax = -1;

                for (int j = from[i]; j < to[i]; j++) {
                    long val = dense.get(i, j);
                    if (val < expectedMin) {
                        expectedMin = val;
                        expectedArgMin = j;
                    }
                    if (val > expectedMax) {
                        expectedMax = val;
                        expectedArgMax = j;
                    }
                }

                assertEquals(expectedMin, minExtrema.value[i], "Ranged row " + i + " min value mismatch");
                assertEquals(expectedArgMin, minExtrema.arg[i], "Ranged row " + i + " min arg mismatch");
                assertEquals(expectedMax, maxExtrema.value[i], "Ranged row " + i + " max value mismatch");
                assertEquals(expectedArgMax, maxExtrema.arg[i], "Ranged row " + i + " max arg mismatch");
            }
        }
    }

    @Test
    public void testAntiMongeCoreAlgorithmsExtrema() {
        int H = 7;
        int W = 9;
        AntiMongeCone cone = new AntiMongeCone(H, W);
        Random rand = new Random(54321);

        for (int k = 0; k < 12; k++) {
            cone.addNorthWest(1 + rand.nextInt(H), 1 + rand.nextInt(W), rand.nextInt(25));
            cone.addSouthEast(rand.nextInt(H), rand.nextInt(W), rand.nextInt(25));
        }

        AntiMongeCoreLong core = cone.buildCore();
        DenseAntiMongeLong dense = cone.buildDense();

        MongeCoreAlgorithms.RowExtrema minExtrema = MongeCoreAlgorithms.rowMinima(core);
        MongeCoreAlgorithms.RowExtrema maxExtrema = MongeCoreAlgorithms.rowMaxima(core);

        for (int i = 0; i < H; i++) {
            long expectedMin = Long.MAX_VALUE;
            int expectedArgMin = -1;
            long expectedMax = Long.MIN_VALUE;
            int expectedArgMax = -1;

            for (int j = 0; j < W; j++) {
                long val = dense.get(i, j);
                if (val < expectedMin) {
                    expectedMin = val;
                    expectedArgMin = j;
                }
                if (val > expectedMax) {
                    expectedMax = val;
                    expectedArgMax = j;
                }
            }

            assertEquals(expectedMin, minExtrema.value[i]);
            assertEquals(expectedArgMin, minExtrema.arg[i]);
            assertEquals(expectedMax, maxExtrema.value[i]);
            assertEquals(expectedArgMax, maxExtrema.arg[i]);
        }
    }

    @Test
    public void testEdgeCasesAndExceptions() {
        // Empty 0x0
        MongeCone cone0 = new MongeCone(0, 0);
        assertEquals(0, cone0.rows());
        assertEquals(0, cone0.cols());
        MongeCoreLong core0 = cone0.buildCore();
        DenseMongeLong dense0 = cone0.buildDense();
        IndexedMongeLong indexed0 = cone0.buildIndexed();
        assertEquals(0, dense0.rows());
        assertEquals(0, dense0.cols());
        assertEquals(0, indexed0.rows());
        assertEquals(0, indexed0.cols());

        MongeCoreAlgorithms.RowExtrema ext0 = MongeCoreAlgorithms.rowMinima(core0);
        assertEquals(0, ext0.value.length);

        // Negative weights
        MongeCone cone = new MongeCone(3, 3);
        assertThrows(IllegalArgumentException.class, () -> cone.addRow(0, -1));
        assertThrows(IllegalArgumentException.class, () -> cone.addColumn(0, -1));
        assertThrows(IllegalArgumentException.class, () -> cone.addNorthEast(2, 1, -5));
        assertThrows(IllegalArgumentException.class, () -> cone.addSouthWest(1, 2, -5));

        // Index out of bounds in queries
        IndexedMongeLong indexed = cone.buildIndexed();
        assertThrows(IndexOutOfBoundsException.class, () -> indexed.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> indexed.get(0, 5));

        DenseMongeLong dense = cone.buildDense();
        assertThrows(IndexOutOfBoundsException.class, () -> dense.get(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dense.get(0, 5));

        // Null pointer checks
        assertThrows(NullPointerException.class, () -> new MongeCoreLong(null, new long[3], new int[0], new int[0], new long[0]));
        assertThrows(NullPointerException.class, () -> new IndexedMongeLong(null));
        assertThrows(NullPointerException.class, () -> new DenseMongeLong((long[][]) null));
    }
}
