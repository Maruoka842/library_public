package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;

public class LazyFoldTreapMapTest {

    @Test
    public void testBasicPutAndProd() {
        // Simple addition monoid for test
        LazyFoldTreapMap<Integer, Integer, Integer> map = new LazyFoldTreapMap<>(
                Comparator.naturalOrder(),
                Integer::sum,
                0,
                Integer::sum,
                0,
                (action, aggregate, size) -> aggregate + action * size
        );

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.set(2, 10);
        map.set(5, 20);
        map.set(1, 5);

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());

        // allProd should be 5 + 10 + 20 = 35
        assertEquals(35, map.allProd());

        // prod [1, 5) => keys 1, 2 => 5 + 10 = 15
        assertEquals(15, map.prod(1, 5));

        // prod [1, 6) => keys 1, 2, 5 => 35
        assertEquals(35, map.prod(1, 6));

        // containsKey
        assertTrue(map.containsKey(2));
        assertFalse(map.containsKey(3));

        // get
        assertEquals(10, map.get(2));
        assertEquals(100, map.getOrDefault(3, 100));

        // remove
        map.remove(2);
        assertEquals(2, map.size());
        assertFalse(map.containsKey(2));
        assertEquals(25, map.allProd());
    }

    @Test
    public void testLazyPropagation() {
        // Range addition action on sum monoid
        LazyFoldTreapMap<Integer, Integer, Integer> map = new LazyFoldTreapMap<>(
                Comparator.naturalOrder(),
                Integer::sum,
                0,
                Integer::sum,
                0,
                (action, aggregate, size) -> aggregate + action * size
        );

        map.set(10, 1);
        map.set(20, 2);
        map.set(30, 3);
        map.set(40, 4);

        // Apply +5 to keys in [20, 40) (which are 20 and 30)
        map.apply(20, 40, 5);

        // check value of key 20 => 2 + 5 = 7
        assertEquals(7, map.get(20));
        // check value of key 30 => 3 + 5 = 8
        assertEquals(8, map.get(30));
        // check value of key 10 (untouched) => 1
        assertEquals(1, map.get(10));
        // check value of key 40 (untouched) => 4
        assertEquals(4, map.get(40));

        // check prod [20, 40) => 7 + 8 = 15
        assertEquals(15, map.prod(20, 40));

        // check allProd => 1 + 7 + 8 + 4 = 20
        assertEquals(20, map.allProd());
    }

    @Test
    public void testKeyMethods() {
        LazyFoldTreapMap<Integer, Integer, Integer> map = new LazyFoldTreapMap<>(
                Comparator.naturalOrder(),
                Integer::sum,
                0,
                Integer::sum,
                0,
                (action, aggregate, size) -> aggregate + action * size
        );

        map.set(10, 1);
        map.set(20, 2);
        map.set(30, 3);

        assertEquals(10, map.lowerKey(20));
        assertNull(map.lowerKey(10));

        assertEquals(20, map.floorKey(20));
        assertEquals(10, map.floorKey(15));

        assertEquals(30, map.higherKey(20));
        assertNull(map.higherKey(30));

        assertEquals(20, map.ceilKey(20));
        assertEquals(30, map.ceilKey(25));
    }
}
