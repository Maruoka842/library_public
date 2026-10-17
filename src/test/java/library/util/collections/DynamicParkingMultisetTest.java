package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DynamicParkingMultisetTest {

    @Test
    public void testStrategyConstructorAndBasicOperations() {
        // Define a sum strategy for displacement fold
        DynamicParkingMultiset.Strategy<Long> sumStrategy = new DynamicParkingMultiset.Strategy<Long>() {
            @Override
            public Long op(Long a, Long b) {
                return a + b;
            }

            @Override
            public Long identity() {
                return 0L;
            }

            @Override
            public Long lift(long d) {
                return d;
            }

            @Override
            public Long shift(long delta, Long aggregate, int size) {
                return aggregate + delta * size;
            }
        };

        DynamicParkingMultiset<Long> parking = new DynamicParkingMultiset<>(sumStrategy);

        assertTrue(parking.isEmpty());
        assertEquals(0, parking.size());
        assertEquals(0L, parking.parkedSum());
        assertEquals(0L, parking.displacementFold());

        // Add elements and check parked positions
        // s = {5} => parked positions a = {5}
        // d = {0}
        assertEquals(5, parking.add(5));
        assertFalse(parking.isEmpty());
        assertEquals(1, parking.size());
        assertEquals(5L, parking.parkedSum());
        assertEquals(0L, parking.displacementFold());
        assertTrue(parking.isOccupied(5));
        assertFalse(parking.isOccupied(6));
        assertEquals(1, parking.count(5));

        // Add another 5
        // s = {5, 5} => a = {5, 6}
        // d = {0, 1}
        assertEquals(6, parking.add(5));
        assertEquals(2, parking.size());
        assertEquals(11L, parking.parkedSum());
        assertEquals(1L, parking.displacementFold()); // sum of d = 1
        assertEquals(2, parking.count(5));

        // Add 4
        // s = {4, 5, 5} => a = {4, 5, 6}
        // d = {0, 0, 1}
        assertEquals(4, parking.add(4));
        assertEquals(3, parking.size());
        assertEquals(15L, parking.parkedSum());
        assertEquals(1L, parking.displacementFold());

        // Add another 5
        // s = {4, 5, 5, 5} => a = {4, 5, 6, 7}
        // d = {0, 0, 1, 2}. Sum of d = 3.
        assertEquals(7, parking.add(5));
        assertEquals(4, parking.size());
        assertEquals(22L, parking.parkedSum());
        assertEquals(3L, parking.displacementFold());

        // Remove 5
        // This removes the last inserted 5, which had displacement 2 (parked at 7).
        // Remaining: s = {4, 5, 5} => a = {4, 5, 6}, d = {0, 0, 1}. Sum of d = 1.
        assertEquals(7, parking.remove(5));
        assertEquals(3, parking.size());
        assertEquals(15L, parking.parkedSum());
        assertEquals(1L, parking.displacementFold());

        // Remove 4
        // Remaining: s = {5, 5} => a = {5, 6}, d = {0, 1}. Sum of d = 1.
        assertEquals(4, parking.remove(4));
        assertEquals(2, parking.size());
        assertEquals(11L, parking.parkedSum());
        assertEquals(1L, parking.displacementFold());
    }
}
