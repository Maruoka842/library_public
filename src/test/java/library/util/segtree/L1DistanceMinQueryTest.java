package library.util.segtree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class L1DistanceMinQueryTest {

    @Test
    public void testL1DistanceMinQueryBasic() {
        int n = 5;
        L1DistanceMinQuery l1 = new L1DistanceMinQuery(n);
        assertEquals(5, l1.size());

        for (int i = 0; i < n; i++) {
            assertEquals(L1DistanceMinQuery.INF, l1.rawGet(i));
            assertEquals(L1DistanceMinQuery.INF, l1.get(i));
        }

        l1.set(1, 10L);
        l1.set(3, 4L);

        assertEquals(10L, l1.rawGet(1));
        assertEquals(4L, l1.rawGet(3));

        // i = 0: min(|0-1| + 10, |0-3| + 4) = min(11, 7) = 7
        assertEquals(7L, l1.get(0));
        // i = 1: min(|1-1| + 10, |1-3| + 4) = min(10, 6) = 6
        assertEquals(6L, l1.get(1));
        // i = 2: min(|2-1| + 10, |2-3| + 4) = min(11, 5) = 5
        assertEquals(5L, l1.get(2));
        // i = 3: min(|3-1| + 10, |3-3| + 4) = min(12, 4) = 4
        assertEquals(4L, l1.get(3));
        // i = 4: min(|4-1| + 10, |4-3| + 4) = min(13, 5) = 5
        assertEquals(5L, l1.get(4));
    }

    @Test
    public void testClear() {
        long[] initial = {10L, 20L, 5L, 15L};
        L1DistanceMinQuery l1 = new L1DistanceMinQuery(initial);

        assertEquals(5L, l1.get(2));
        assertEquals(6L, l1.get(3));

        l1.clear();

        for (int i = 0; i < 4; i++) {
            assertEquals(L1DistanceMinQuery.INF, l1.rawGet(i));
            assertEquals(L1DistanceMinQuery.INF, l1.get(i));
        }

        // Set values after clear
        l1.set(0, 3L);
        assertEquals(3L, l1.get(0));
        assertEquals(4L, l1.get(1));
    }

    @Test
    public void testBuildAndCircular() {
        long[] a = {10L, 2L, 10L, 10L};
        L1DistanceMinQuery l1 = new L1DistanceMinQuery(a);

        // Circular query test
        // i = 3: distance to index 1 in circular (n=4) is min(|3-1|, 4-|3-1|) = 2.
        // cost = 2 + 2 = 4.
        assertEquals(4L, l1.getCircular(3));

        l1.clear();
        assertEquals(L1DistanceMinQuery.INF, l1.getCircular(3));
    }
}
