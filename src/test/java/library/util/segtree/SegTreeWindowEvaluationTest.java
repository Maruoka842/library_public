package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class SegTreeWindowEvaluationTest {

    // Helper for a naive brute force window evaluation
    private static <E, R> R naiveEvaluate(E[] arr, int K, java.util.function.Function<E[], R> phi, java.util.function.BinaryOperator<R> op, R identityR) {
        if (arr.length < K) {
            return identityR;
        }
        R agg = identityR;
        boolean first = true;
        for (int i = 0; i <= arr.length - K; i++) {
            E[] sub = Arrays.copyOfRange(arr, i, i + K);
            R val = phi.apply(sub);
            if (first) {
                agg = val;
                first = false;
            } else {
                agg = op.apply(agg, val);
            }
        }
        return agg;
    }

    private static <E, R> WindowEvaluationStrategy<E, R> makeStrategy(int K, java.util.function.Function<E[], R> phi, java.util.function.BinaryOperator<R> op, R identity, Class<E> clazz) {
        return new WindowEvaluationStrategy<E, R>() {
            @Override
            public int K() { return K; }

            @Override
            public R phi(E[] elements) { return phi.apply(elements); }

            @Override
            public R op(R a, R b) { return op.apply(a, b); }

            @Override
            public R identity() { return identity; }

            @Override
            public Class<E> clazz() { return clazz; }
        };
    }

    @Test
    public void testBasicWindowEvaluationSum() {
        Integer[] arr = {1, 2, 3, 4, 5};
        int K = 3;
        // phi computes the sum of the elements in the window
        java.util.function.Function<Integer[], Integer> phi = sub -> {
            int sum = 0;
            for (int v : sub) sum += v;
            return sum;
        };
        // op is addition
        java.util.function.BinaryOperator<Integer> op = Integer::sum;
        Integer identityR = 0;

        WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);

        var seg = SegTreeFactory.windowEvaluation(arr, strategy);

        // Expected windows of size 3:
        // [1, 2, 3] -> sum = 6
        // [2, 3, 4] -> sum = 9
        // [3, 4, 5] -> sum = 12
        // Aggregate: 6 + 9 + 12 = 27
        assertEquals(27, seg.prodAll().agg);

        // Point update index 2 (value 3 -> 10)
        // Array becomes: {1, 2, 10, 4, 5}
        // Windows:
        // [1, 2, 10] -> 13
        // [2, 10, 4] -> 16
        // [10, 4, 5] -> 19
        // Aggregate: 13 + 16 + 19 = 48
        seg.setElement(2, 10);
        assertEquals(48, seg.prodAll().agg);
    }

    @Test
    public void testStringConcatenationWindow() {
        String[] arr = {"A", "B", "C", "D"};
        int K = 2;
        // phi joins with "-"
        java.util.function.Function<String[], String> phi = sub -> sub[0] + "-" + sub[1];
        // op concatenates with ","
        java.util.function.BinaryOperator<String> op = (a, b) -> a + "," + b;
        String identityR = "";

        WindowEvaluationStrategy<String, String> strategy = makeStrategy(K, phi, op, identityR, String.class);

        var seg = SegTreeFactory.windowEvaluation(arr, strategy);

        // Windows:
        // [A, B] -> "A-B"
        // [B, C] -> "B-C"
        // [C, D] -> "C-D"
        // Agg: "A-B,B-C,C-D"
        assertEquals("A-B,B-C,C-D", seg.prodAll().agg);

        // Update B -> X
        seg.setElement(1, "X");
        // Windows:
        // [A, X] -> "A-X"
        // [X, C] -> "X-C"
        // [C, D] -> "C-D"
        // Agg: "A-X,X-C,C-D"
        assertEquals("A-X,X-C,C-D", seg.prodAll().agg);
    }

    @Test
    public void testKEqualsOne() {
        Integer[] arr = {5, 10, 15};
        int K = 1;
        java.util.function.Function<Integer[], Integer> phi = sub -> sub[0] * 2;
        java.util.function.BinaryOperator<Integer> op = Integer::sum;
        Integer identityR = 0;

        WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);

        var seg = SegTreeFactory.windowEvaluation(arr, strategy);
        // [5]*2 -> 10, [10]*2 -> 20, [15]*2 -> 30. Total = 60
        assertEquals(60, seg.prodAll().agg);

        // When K=1, prefix and suffix lengths should be 0
        WindowEvaluationState<Integer, Integer> root = seg.prodAll();
        assertEquals(0, root.prefix.length);
        assertEquals(0, root.suffix.length);
        assertEquals(1, root.len); // min(L.len + R.len, K) is min(3, 1) = 1

        seg.setElement(1, 3);
        // [5]*2 -> 10, [3]*2 -> 6, [15]*2 -> 30. Total = 46
        assertEquals(46, seg.prodAll().agg);
    }

    @Test
    public void testNLessThanK() {
        Integer[] arr = {1, 2};
        int K = 3;
        java.util.function.Function<Integer[], Integer> phi = sub -> 42;
        java.util.function.BinaryOperator<Integer> op = Integer::sum;
        Integer identityR = 0;

        WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);

        var seg = SegTreeFactory.windowEvaluation(arr, strategy);
        assertEquals(0, seg.prodAll().agg);
    }

    @Test
    public void testRandomizedStressTest() {
        java.util.Random rnd = new java.util.Random(12345L);
        int N = 50;
        for (int K = 1; K <= 10; K++) {
            Integer[] arr = new Integer[N];
            for (int i = 0; i < N; i++) {
                arr[i] = rnd.nextInt(100);
            }

            java.util.function.Function<Integer[], Integer> phi = sub -> {
                int sum = 0;
                for (int v : sub) sum += v;
                return sum;
            };
            java.util.function.BinaryOperator<Integer> op = Integer::sum;
            Integer identityR = 0;

            WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);

            var seg = SegTreeFactory.windowEvaluation(arr, strategy);

            for (int step = 0; step < 100; step++) {
                int idx = rnd.nextInt(N);
                int val = rnd.nextInt(100);
                arr[idx] = val;
                seg.setElement(idx, val);

                int expected = naiveEvaluate(arr, K, phi, op, identityR);
                int actual = seg.prodAll().agg;
                assertEquals(expected, actual, "Mismatch at step " + step + " with K=" + K);
            }
        }
    }

    @Test
    public void testVaryingCountsBasic() {
        Integer[] arr = {1, 2, 3};
        int K = 3;
        java.util.function.Function<Integer[], Integer> phi = sub -> {
            int sum = 0;
            for (int v : sub) sum += v;
            return sum;
        };
        java.util.function.BinaryOperator<Integer> op = Integer::sum;
        Integer identityR = 0;

        WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);
        var seg = SegTreeFactory.windowEvaluation(arr, strategy);

        // Initially: arr = {1, 2, 3}. K = 3. Expected sum: 6.
        assertNotNull(seg.prodAll());
        assertEquals(6, seg.prodAll().agg);

        // Set index 1 to empty using setEmpty.
        // Elements become: {1, 3}. K = 3. Too short, expected 0.
        seg.setEmpty(1);
        var root = seg.prodAll();
        int agg = (root == null) ? 0 : root.agg;
        assertEquals(0, agg);

        // Set index 1 to value 5 with count 2.
        // Elements become: {1, 5, 5, 3}.
        // Windows:
        // [1, 5, 5] -> 11
        // [5, 5, 3] -> 13
        // Total sum: 24
        seg.setElement(1, 5, 2);
        assertNotNull(seg.prodAll());
        assertEquals(24, seg.prodAll().agg);
    }

    @Test
    public void testRandomizedStressTestWithVaryingCounts() {
        java.util.Random rnd = new java.util.Random(67890L);
        int N = 30;
        for (int K = 1; K <= 8; K++) {
            Integer[] values = new Integer[N];
            int[] counts = new int[N];
            for (int i = 0; i < N; i++) {
                values[i] = rnd.nextInt(50);
                counts[i] = rnd.nextInt(4); // 0, 1, 2, 3
            }

            java.util.function.Function<Integer[], Integer> phi = sub -> {
                int sum = 0;
                for (int v : sub) sum += v;
                return sum;
            };
            java.util.function.BinaryOperator<Integer> op = Integer::sum;
            Integer identityR = 0;

            WindowEvaluationStrategy<Integer, Integer> strategy = makeStrategy(K, phi, op, identityR, Integer.class);

            // Construct initially using individual elements (counts = 1)
            Integer[] initialArr = new Integer[N];
            for (int i = 0; i < N; i++) {
                initialArr[i] = values[i];
            }
            var seg = SegTreeFactory.windowEvaluation(initialArr, strategy);

            // Now, set with varying counts
            for (int i = 0; i < N; i++) {
                if (counts[i] == 0) {
                    seg.setEmpty(i);
                } else {
                    seg.setElement(i, values[i], counts[i]);
                }
            }

            for (int step = 0; step < 150; step++) {
                int idx = rnd.nextInt(N);
                int val = rnd.nextInt(50);
                int count = rnd.nextInt(5); // 0, 1, 2, 3, 4
                values[idx] = val;
                counts[idx] = count;

                if (count == 0 && rnd.nextBoolean()) {
                    // Randomly test both setEmpty(idx) and setElement(idx, val, 0)
                    seg.setEmpty(idx);
                } else {
                    seg.setElement(idx, val, count);
                }

                // Build expected expanded array
                java.util.List<Integer> expandedList = new java.util.ArrayList<>();
                for (int i = 0; i < N; i++) {
                    for (int c = 0; c < counts[i]; c++) {
                        expandedList.add(values[i]);
                    }
                }
                Integer[] expandedArr = expandedList.toArray(new Integer[0]);

                int expected = naiveEvaluate(expandedArr, K, phi, op, identityR);
                WindowEvaluationState<Integer, Integer> root = seg.prodAll();
                int actual = (root == null) ? identityR : root.agg;

                assertEquals(expected, actual, "Mismatch with varying counts at step " + step + " with K=" + K);
            }
        }
    }
}
