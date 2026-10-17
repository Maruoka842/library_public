package library.util.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class SmallSideBoundedAssignmentTest {

    @Test
    public void testBasicSolveMax() {
        // N = 3, K = 2
        long[][] value = {
            {10, 1},
            {2, 20},
            {3, 30}
        };
        int[] lower = {1, 1};
        int[] upper = {2, 2};

        SmallSideBoundedAssignment.Result res = SmallSideBoundedAssignment.solveMax(value, lower, upper);
        assertEquals(60, res.maxValue);
        assertNotNull(res.assignment);
        assertEquals(3, res.assignment.length);

        // Check feasibility
        int[] count = new int[2];
        long computedValue = 0;
        for (int i = 0; i < 3; i++) {
            int g = res.assignment[i];
            assertTrue(g >= 0 && g < 2);
            count[g]++;
            computedValue += value[i][g];
        }
        assertEquals(60, computedValue);
        for (int g = 0; g < 2; g++) {
            assertTrue(count[g] >= lower[g]);
            assertTrue(count[g] <= upper[g]);
        }
    }

    @Test
    public void testBasicSolveMaxExact() {
        // N = 4, K = 3
        long[][] value = {
            {10, 5, 1},
            {2, 20, 4},
            {1, 3, 30},
            {100, 10, 5}
        };
        int[] quota = {1, 2, 1};

        SmallSideBoundedAssignment.Result res = SmallSideBoundedAssignment.solveMaxExact(value, quota);
        assertNotNull(res.assignment);

        int[] count = new int[3];
        long computedValue = 0;
        for (int i = 0; i < 4; i++) {
            int g = res.assignment[i];
            assertTrue(g >= 0 && g < 3);
            count[g]++;
            computedValue += value[i][g];
        }
        assertEquals(res.maxValue, computedValue);
        assertArrayEquals(quota, count);
    }

    @Test
    public void testValidationAndExceptions() {
        // Null inputs
        assertThrows(NullPointerException.class, () -> {
            SmallSideBoundedAssignment.solveMax(null, new int[]{1}, new int[]{1});
        });
        assertThrows(NullPointerException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1}}, null, new int[]{1});
        });
        assertThrows(NullPointerException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1}}, new int[]{1}, null);
        });

        // Length mismatch
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1}}, new int[]{1}, new int[]{1, 2});
        });

        // Invalid group bounds (lower > upper)
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1, 2}}, new int[]{2, 1}, new int[]{1, 2});
        });

        // Invalid group bounds (negative bounds)
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1}}, new int[]{-1}, new int[]{1});
        });

        // Infeasible bounds (sumLower > N)
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1, 2}}, new int[]{2, 2}, new int[]{2, 2});
        });

        // Infeasible bounds (sumUpper < N)
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1, 2}, {3, 4}}, new int[]{0, 0}, new int[]{0, 1});
        });

        // Invalid value array dimension (each row must be length K)
        assertThrows(IllegalArgumentException.class, () -> {
            SmallSideBoundedAssignment.solveMax(new long[][]{{1, 2}, {3}}, new int[]{1, 1}, new int[]{2, 2});
        });
    }

    @Test
    public void testRandomAgainstBruteForce() {
        Random rand = new Random(12345);
        int numTests = 100;

        for (int t = 0; t < numTests; t++) {
            int N = rand.nextInt(7) + 1; // 1 to 7
            int K = rand.nextInt(3) + 1; // 1 to 3

            long[][] value = new long[N][K];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < K; j++) {
                    value[i][j] = rand.nextInt(200) - 100; // negative values too
                }
            }

            // Generate valid lower and upper bounds
            int[] lower = new int[K];
            int[] upper = new int[K];
            while (true) {
                int sumL = 0;
                int sumU = 0;
                for (int g = 0; g < K; g++) {
                    lower[g] = rand.nextInt(N + 1);
                    upper[g] = rand.nextInt(N + 1);
                    if (lower[g] > upper[g]) {
                        int temp = lower[g];
                        lower[g] = upper[g];
                        upper[g] = temp;
                    }
                    sumL += lower[g];
                    sumU += upper[g];
                }
                if (sumL <= N && sumU >= N) {
                    break; // valid feasible bounds
                }
            }

            // Solve using SmallSideBoundedAssignment
            SmallSideBoundedAssignment.Result actual = SmallSideBoundedAssignment.solveMax(value, lower, upper);

            // Solve using brute force
            BruteForceResult expected = solveBruteForce(value, lower, upper);

            assertEquals(expected.maxValue, actual.maxValue, "Failed at test " + t + " with N=" + N + ", K=" + K);

            // Check that actual assignment is valid and achieves max value
            int[] actualCount = new int[K];
            long actualValueSum = 0;
            for (int i = 0; i < N; i++) {
                int g = actual.assignment[i];
                assertTrue(g >= 0 && g < K);
                actualCount[g]++;
                actualValueSum += value[i][g];
            }
            assertEquals(expected.maxValue, actualValueSum);
            for (int g = 0; g < K; g++) {
                assertTrue(actualCount[g] >= lower[g]);
                assertTrue(actualCount[g] <= upper[g]);
            }
        }
    }

    private static class BruteForceResult {
        long maxValue = Long.MIN_VALUE;
        int[] assignment;
    }

    private BruteForceResult solveBruteForce(long[][] value, int[] lower, int[] upper) {
        int N = value.length;
        int K = lower.length;
        BruteForceResult res = new BruteForceResult();
        int[] currentAssignment = new int[N];
        search(0, N, K, value, lower, upper, currentAssignment, res);
        return res;
    }

    private void search(int idx, int N, int K, long[][] value, int[] lower, int[] upper, int[] currentAssignment, BruteForceResult res) {
        if (idx == N) {
            // Check feasibility
            int[] count = new int[K];
            long currentSum = 0;
            for (int i = 0; i < N; i++) {
                int g = currentAssignment[i];
                count[g]++;
                currentSum += value[i][g];
            }
            for (int g = 0; g < K; g++) {
                if (count[g] < lower[g] || count[g] > upper[g]) {
                    return; // infeasible
                }
            }
            if (currentSum > res.maxValue) {
                res.maxValue = currentSum;
                res.assignment = currentAssignment.clone();
            }
            return;
        }

        for (int g = 0; g < K; g++) {
            currentAssignment[idx] = g;
            search(idx + 1, N, K, value, lower, upper, currentAssignment, res);
        }
    }
}
