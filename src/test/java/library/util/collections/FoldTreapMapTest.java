package library.util.collections;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

public class FoldTreapMapTest {

    @Test
    public void testBasicMapOperations() {
        FoldTreapMap<Integer> map = new FoldTreapMap<>(Integer::sum, 0);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put(10, 100);
        map.put(20, 200);
        map.put(30, 300);

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertTrue(map.containsKey(10));
        assertTrue(map.containsKey(20));
        assertTrue(map.containsKey(30));
        assertFalse(map.containsKey(15));

        assertEquals(100, map.get(10));
        assertEquals(200, map.get(20));
        assertEquals(300, map.get(30));
        assertEquals(400, map.getOrDefault(40, 400));
        assertEquals(100, map.getOrDefault(10, 400));

        // Overwrite
        map.put(20, 250);
        assertEquals(250, map.get(20));
        assertEquals(3, map.size());

        // merge
        map.merge(20, 50, Integer::sum);
        assertEquals(300, map.get(20));

        map.merge(40, 400, Integer::sum);
        assertEquals(400, map.get(40));
        assertEquals(4, map.size());

        // remove
        map.remove(20);
        assertFalse(map.containsKey(20));
        assertEquals(3, map.size());
        assertThrows(AssertionError.class, () -> map.get(20));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testFoldQueries() {
        FoldTreapMap<Integer> map = new FoldTreapMap<>(Integer::sum, 0);
        map.put(10, 1);
        map.put(20, 2);
        map.put(30, 4);
        map.put(40, 8);
        map.put(50, 16);

        // allProd
        assertEquals(31, map.allFold());

        // prodRange
        assertEquals(7, map.fold(10, 35)); // 1 + 2 + 4
        assertEquals(0, map.fold(15, 18));
        assertEquals(14, map.fold(20, 45)); // 2 + 4 + 8
        assertEquals(31, map.fold(5, 100));

        // prodLessThan / prodLessEqual
        assertEquals(0, map.foldLessThan(10));
        assertEquals(1, map.foldLessEqual(10));
        assertEquals(1, map.foldLessThan(20));
        assertEquals(3, map.foldLessEqual(20));
        assertEquals(15, map.foldLessThan(50));
        assertEquals(31, map.foldLessEqual(50));
        assertEquals(31, map.foldLessThan(100));

        // prodGreaterThan / prodGreaterEqual
        assertEquals(31, map.foldGreaterEqual(10));
        assertEquals(30, map.foldGreaterThan(10));
        assertEquals(30, map.foldGreaterEqual(20));
        assertEquals(28, map.foldGreaterThan(20));
        assertEquals(16, map.foldGreaterEqual(50));
        assertEquals(0, map.foldGreaterThan(50));
        assertEquals(0, map.foldGreaterEqual(100));

        // prodByIndex
        assertEquals(31, map.foldByIndex(0, 5));
        assertEquals(3, map.foldByIndex(0, 2)); // 1 + 2
        assertEquals(14, map.foldByIndex(1, 4)); // 2 + 4 + 8
        assertEquals(0, map.foldByIndex(3, 3));
        assertEquals(16, map.foldByIndex(4, 5));
    }

    @Test
    public void testOrderStatisticsAndRank() {
        FoldTreapMap<Integer> map = new FoldTreapMap<>(Integer::sum, 0);
        map.put(10, 100);
        map.put(30, 300);
        map.put(20, 200);
        map.put(40, 400);

        // kthKey
        assertEquals(10L, map.kthKey(0));
        assertEquals(20L, map.kthKey(1));
        assertEquals(30L, map.kthKey(2));
        assertEquals(40L, map.kthKey(3));
        assertNull(map.kthKey(-1));
        assertNull(map.kthKey(4));

        // kthEntry
        assertEquals(10L, map.kthEntry(0).key);
        assertEquals(100, map.kthEntry(0).value);
        assertEquals(40L, map.kthEntry(3).key);
        assertEquals(400, map.kthEntry(3).value);
        assertNull(map.kthEntry(-1));
        assertNull(map.kthEntry(4));

        // firstKey / lastKey
        assertEquals(10L, map.firstKey());
        assertEquals(40L, map.lastKey());

        // firstEntry / lastEntry
        assertEquals(10L, map.firstEntry().key);
        assertEquals(100, map.firstEntry().value);
        assertEquals(40L, map.lastEntry().key);
        assertEquals(400, map.lastEntry().value);

        // predecessor / successor
        assertEquals(20L, map.floorKey(25));
        assertEquals(20L, map.floorKey(20));
        assertNull(map.floorKey(5));

        assertEquals(20L, map.lowerKey(25));
        assertEquals(10L, map.lowerKey(20));
        assertNull(map.lowerKey(10));

        assertEquals(30L, map.ceilKey(25));
        assertEquals(30L, map.ceilKey(30));
        assertNull(map.ceilKey(45));

        assertEquals(30L, map.higherKey(25));
        assertEquals(40L, map.higherKey(30));
        assertNull(map.higherKey(40));

        // countLeq / countLt
        assertEquals(0, map.countLeq(5));
        assertEquals(1, map.countLeq(10));
        assertEquals(1, map.countLeq(15));
        assertEquals(2, map.countLeq(20));
        assertEquals(4, map.countLeq(40));
        assertEquals(4, map.countLeq(50));

        assertEquals(0, map.countLt(5));
        assertEquals(0, map.countLt(10));
        assertEquals(1, map.countLt(15));
        assertEquals(1, map.countLt(20));
        assertEquals(3, map.countLt(40));
        assertEquals(4, map.countLt(50));
    }

    @Test
    public void testNonCommutativeMonoidAndBinarySearch() {
        // String concatenation is non-commutative
        FoldTreapMap<String> map = new FoldTreapMap<>((s1, s2) -> s1 + s2, "");
        map.put(10, "A");
        map.put(20, "B");
        map.put(30, "C");
        map.put(40, "D");

        assertEquals("ABCD", map.allFold());
        assertEquals("BC", map.fold(20, 40));

        // firstPrefixFailure from 10
        // inclusive = true, predicate = s -> s.length() < 3
        // prefix results:
        // at 10: "A" (len 1 < 3) -> true
        // at 20: "AB" (len 2 < 3) -> true
        // at 30: "ABC" (len 3 < 3) -> false
        Long res1 = map.firstPrefixFailure(10, true, s -> s.length() < 3);
        assertEquals(30L, res1);

        // inclusive = false, predicate = s -> s.length() < 3, from = 10
        // target elements are > 10, i.e., {20, 30, 40}
        // prefix results:
        // at 20: "B" (len 1) -> true
        // at 30: "BC" (len 2) -> true
        // at 40: "BCD" (len 3) -> false
        Long res2 = map.firstPrefixFailure(10, false, s -> s.length() < 3);
        assertEquals(40L, res2);

        // All true
        Long res3 = map.firstPrefixFailure(10, true, s -> s.length() < 10);
        assertNull(res3);

        // lastSuffixFailure from 40
        // inclusive = true, predicate = s -> s.length() < 3
        // suffix results going left from 40:
        // at 40: "D" (len 1 < 3) -> true
        // at 30: "CD" (len 2 < 3) -> true (original fold order "C"+"D")
        // at 20: "BCD" (len 3 < 3) -> false
        Long res4 = map.lastSuffixFailure(40, true, s -> s.length() < 3);
        assertEquals(20L, res4);

        // inclusive = false, predicate = s -> s.length() < 3, to = 40
        // target elements are < 40, i.e., {10, 20, 30}
        // suffix results going left from 30:
        // at 30: "C" (len 1) -> true
        // at 20: "BC" (len 2) -> true
        // at 10: "ABC" (len 3) -> false
        Long res5 = map.lastSuffixFailure(40, false, s -> s.length() < 3);
        assertEquals(10L, res5);

        // All true
        Long res6 = map.lastSuffixFailure(40, true, s -> s.length() < 10);
        assertNull(res6);

        // Exception when predicate(identity) is false
        assertThrows(IllegalArgumentException.class, () -> map.firstPrefixFailure(10, true, s -> !s.isEmpty()));
        assertThrows(IllegalArgumentException.class, () -> map.lastSuffixFailure(40, true, s -> !s.isEmpty()));
    }

    @Test
    public void testBuildFromSorted() {
        long[] keys = {10, 20, 30, 40};
        String[] values = {"W", "X", "Y", "Z"};
        FoldTreapMap<String> map = FoldTreapMap.buildFromSorted(keys, values, (s1, s2) -> s1 + s2, "");

        assertEquals(4, map.size());
        assertEquals("WXYZ", map.allFold());
        assertEquals("X", map.get(20));

        map.verify();

        // Check validation exceptions
        assertThrows(IllegalArgumentException.class, () -> {
            FoldTreapMap.buildFromSorted(new long[]{10, 20}, new String[]{"A"}, (s1, s2) -> s1 + s2, "");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FoldTreapMap.buildFromSorted(new long[]{20, 10}, new String[]{"A", "B"}, (s1, s2) -> s1 + s2, "");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FoldTreapMap.buildFromSorted(new long[]{10, 10}, new String[]{"A", "B"}, (s1, s2) -> s1 + s2, "");
        });
    }

    @Test
    public void testDeepCopyAndVerify() {
        FoldTreapMap<Integer> map = new FoldTreapMap<>(Integer::sum, 0);
        map.put(10, 100);
        map.put(30, 300);
        map.put(20, 200);

        map.verify();

        FoldTreapMap<Integer> cp = map.copy();
        cp.verify();
        assertEquals(map.size(), cp.size());
        assertEquals(map.allFold(), cp.allFold());
        assertEquals(map.entryList().toString(), cp.entryList().toString());

        // verify equals and hashCode
        assertEquals(map, cp);
        assertEquals(map.hashCode(), cp.hashCode());

        // mutate copy
        cp.put(20, 500);
        assertNotEquals(map, cp);
        assertNotEquals(map.hashCode(), cp.hashCode());
        assertEquals(200, map.get(20));
        assertEquals(500, cp.get(20));

        map.verify();
        cp.verify();
    }

    @Test
    public void testEmptyCornerCases() {
        FoldTreapMap<Integer> map = new FoldTreapMap<>(Integer::sum, 0);
        assertNull(map.firstKey());
        assertNull(map.lastKey());
        assertNull(map.firstEntry());
        assertNull(map.lastEntry());
        assertEquals(0, map.fold(10, 20));
        assertEquals(0, map.foldLessThan(15));
        assertEquals(0, map.foldLessEqual(15));
        assertEquals(0, map.foldGreaterThan(15));
        assertEquals(0, map.foldGreaterEqual(15));
        assertEquals(0, map.foldByIndex(0, 5));
        assertEquals("[]", map.toString());

        map.verify();
    }
}
