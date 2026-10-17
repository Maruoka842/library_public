package library.util.collections;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class TreapCapTest {

    @Test
    public void testLongTreapSetCap() {
        LongTreapSet set1 = new LongTreapSet();
        LongTreapSet set2 = new LongTreapSet();

        // 1. Both empty
        LongTreapSet cap1 = set1.cap(set2);
        assertEquals(0, cap1.size());

        // 2. One empty
        set1.add(5);
        set1.add(10);
        set1.add(15);
        LongTreapSet cap2 = set1.cap(set2);
        assertEquals(0, cap2.size());

        // 3. Disjoint
        set2.add(2);
        set2.add(4);
        set2.add(6);
        LongTreapSet cap3 = set1.cap(set2);
        assertEquals(0, cap3.size());

        // 4. Overlapping
        set2.add(10);
        set2.add(15);
        set2.add(20);
        // set1: 5, 10, 15
        // set2: 2, 4, 6, 10, 15, 20
        // intersection: 10, 15
        LongTreapSet cap4 = set1.cap(set2);
        assertEquals(2, cap4.size());
        assertTrue(cap4.contains(10));
        assertTrue(cap4.contains(15));
        assertFalse(cap4.contains(5));
        assertFalse(cap4.contains(2));

        // Verify ordering
        PrimitiveIterator.OfLong it = cap4.iterator();
        assertTrue(it.hasNext());
        assertEquals(10, it.nextLong());
        assertTrue(it.hasNext());
        assertEquals(15, it.nextLong());
        assertFalse(it.hasNext());
    }

    @Test
    public void testLongTreapSetCapRandom() {
        Random rnd = new Random(42);
        // Test both large and small random cases, including empty sets
        for (int t = 0; t < 100; t++) {
            LongTreapSet set1 = new LongTreapSet();
            LongTreapSet set2 = new LongTreapSet();
            TreeSet<Long> std1 = new TreeSet<>();
            TreeSet<Long> std2 = new TreeSet<>();

            // Decide sizes: some very small (0 to 5), some empty, some large
            int size1 = rnd.nextInt(6); // 0 to 5
            int size2 = rnd.nextInt(6); // 0 to 5
            if (t < 10) {
                // some larger cases
                size1 = 100 + rnd.nextInt(400);
                size2 = 100 + rnd.nextInt(400);
            } else if (t < 20) {
                // specifically force set1 to be empty
                size1 = 0;
            } else if (t < 30) {
                // specifically force set2 to be empty
                size2 = 0;
            } else if (t < 40) {
                // specifically force both to be empty
                size1 = 0;
                size2 = 0;
            }

            for (int i = 0; i < size1; i++) {
                long v = rnd.nextInt(20);
                set1.add(v);
                std1.add(v);
            }
            for (int i = 0; i < size2; i++) {
                long v = rnd.nextInt(20);
                set2.add(v);
                std2.add(v);
            }

            LongTreapSet cap = set1.cap(set2);
            TreeSet<Long> stdCap = new TreeSet<>(std1);
            stdCap.retainAll(std2);

            assertEquals(stdCap.size(), cap.size());
            PrimitiveIterator.OfLong it = cap.iterator();
            for (long val : stdCap) {
                assertTrue(it.hasNext());
                assertEquals(val, it.nextLong());
            }
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void testIntTreapSetCap() {
        IntTreapSet set1 = new IntTreapSet();
        IntTreapSet set2 = new IntTreapSet();

        // 1. Both empty
        IntTreapSet cap1 = set1.cap(set2);
        assertEquals(0, cap1.size());

        // 2. Overlapping
        set1.add(3);
        set1.add(7);
        set1.add(9);

        set2.add(1);
        set2.add(3);
        set2.add(9);
        set2.add(12);

        IntTreapSet cap2 = set1.cap(set2);
        assertEquals(2, cap2.size());
        assertTrue(cap2.contains(3));
        assertTrue(cap2.contains(9));
        assertFalse(cap2.contains(7));

        PrimitiveIterator.OfInt it = cap2.iterator();
        assertTrue(it.hasNext());
        assertEquals(3, it.nextInt());
        assertTrue(it.hasNext());
        assertEquals(9, it.nextInt());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIntTreapSetCapRandom() {
        Random rnd = new Random(43);
        for (int t = 0; t < 100; t++) {
            IntTreapSet set1 = new IntTreapSet();
            IntTreapSet set2 = new IntTreapSet();
            TreeSet<Integer> std1 = new TreeSet<>();
            TreeSet<Integer> std2 = new TreeSet<>();

            int size1 = rnd.nextInt(6);
            int size2 = rnd.nextInt(6);
            if (t < 10) {
                size1 = 100 + rnd.nextInt(400);
                size2 = 100 + rnd.nextInt(400);
            } else if (t < 20) {
                size1 = 0;
            } else if (t < 30) {
                size2 = 0;
            } else if (t < 40) {
                size1 = 0;
                size2 = 0;
            }

            for (int i = 0; i < size1; i++) {
                int v = rnd.nextInt(20);
                set1.add(v);
                std1.add(v);
            }
            for (int i = 0; i < size2; i++) {
                int v = rnd.nextInt(20);
                set2.add(v);
                std2.add(v);
            }

            IntTreapSet cap = set1.cap(set2);
            TreeSet<Integer> stdCap = new TreeSet<>(std1);
            stdCap.retainAll(std2);

            assertEquals(stdCap.size(), cap.size());
            PrimitiveIterator.OfInt it = cap.iterator();
            for (int val : stdCap) {
                assertTrue(it.hasNext());
                assertEquals(val, it.nextInt());
            }
            assertFalse(it.hasNext());
        }
    }

    @Test
    public void testIntTreapMultiSetCap() {
        IntTreapMultiSet set1 = new IntTreapMultiSet();
        IntTreapMultiSet set2 = new IntTreapMultiSet();

        // 1. Both empty
        IntTreapMultiSet cap1 = set1.cap(set2);
        assertEquals(0, cap1.size());

        // 2. Overlapping with multiplicities
        set1.add(5, 3); // three 5s
        set1.add(10, 1); // one 10
        set1.add(15, 4); // four 15s

        set2.add(5, 1); // one 5
        set2.add(15, 6); // six 15s
        set2.add(20, 2); // two 20s

        // Intersection should be: one 5, four 15s. Total size = 5.
        IntTreapMultiSet cap2 = set1.cap(set2);
        assertEquals(5, cap2.size());
        assertEquals(1, cap2.count(5));
        assertEquals(0, cap2.count(10));
        assertEquals(4, cap2.count(15));
        assertEquals(0, cap2.count(20));

        cap2.getTreapMap().verify();
    }

    @Test
    public void testIntTreapMultiSetCapRandom() {
        Random rnd = new Random(44);
        for (int t = 0; t < 100; t++) {
            IntTreapMultiSet set1 = new IntTreapMultiSet();
            IntTreapMultiSet set2 = new IntTreapMultiSet();
            Map<Integer, Long> std1 = new HashMap<>();
            Map<Integer, Long> std2 = new HashMap<>();

            int size1 = rnd.nextInt(6);
            int size2 = rnd.nextInt(6);
            if (t < 10) {
                size1 = 50 + rnd.nextInt(200);
                size2 = 50 + rnd.nextInt(200);
            } else if (t < 20) {
                size1 = 0;
            } else if (t < 30) {
                size2 = 0;
            } else if (t < 40) {
                size1 = 0;
                size2 = 0;
            }

            for (int i = 0; i < size1; i++) {
                int v = rnd.nextInt(15);
                long count = rnd.nextInt(5) + 1;
                set1.add(v, count);
                std1.put(v, std1.getOrDefault(v, 0L) + count);
            }
            for (int i = 0; i < size2; i++) {
                int v = rnd.nextInt(15);
                long count = rnd.nextInt(5) + 1;
                set2.add(v, count);
                std2.put(v, std2.getOrDefault(v, 0L) + count);
            }

            IntTreapMultiSet cap = set1.cap(set2);
            cap.getTreapMap().verify();

            long expectedTotalSize = 0;
            for (var entry : std1.entrySet()) {
                int key = entry.getKey();
                if (std2.containsKey(key)) {
                    long minVal = Math.min(entry.getValue(), std2.get(key));
                    assertEquals(minVal, cap.count(key));
                    expectedTotalSize += minVal;
                } else {
                    assertEquals(0, cap.count(key));
                }
            }
            assertEquals(expectedTotalSize, cap.size());
        }
    }

    @Test
    public void testLongTreapMultiSetCap() {
        LongTreapMultiSet set1 = new LongTreapMultiSet();
        LongTreapMultiSet set2 = new LongTreapMultiSet();

        // 1. Both empty
        LongTreapMultiSet cap1 = set1.cap(set2);
        assertEquals(0, cap1.size());

        // 2. Overlapping with multiplicities
        set1.add(5L, 3);
        set1.add(10L, 1);
        set1.add(15L, 4);

        set2.add(5L, 1);
        set2.add(15L, 6);
        set2.add(20L, 2);

        LongTreapMultiSet cap2 = set1.cap(set2);
        assertEquals(5, cap2.size());
        assertEquals(1, cap2.count(5L));
        assertEquals(0, cap2.count(10L));
        assertEquals(4, cap2.count(15L));
        assertEquals(0, cap2.count(20L));

        cap2.getTreapMap().verify();
    }

    @Test
    public void testLongTreapMultiSetCapRandom() {
        Random rnd = new Random(45);
        for (int t = 0; t < 100; t++) {
            LongTreapMultiSet set1 = new LongTreapMultiSet();
            LongTreapMultiSet set2 = new LongTreapMultiSet();
            Map<Long, Long> std1 = new HashMap<>();
            Map<Long, Long> std2 = new HashMap<>();

            int size1 = rnd.nextInt(6);
            int size2 = rnd.nextInt(6);
            if (t < 10) {
                size1 = 50 + rnd.nextInt(200);
                size2 = 50 + rnd.nextInt(200);
            } else if (t < 20) {
                size1 = 0;
            } else if (t < 30) {
                size2 = 0;
            } else if (t < 40) {
                size1 = 0;
                size2 = 0;
            }

            for (int i = 0; i < size1; i++) {
                long v = rnd.nextInt(15);
                long count = rnd.nextInt(5) + 1;
                set1.add(v, count);
                std1.put(v, std1.getOrDefault(v, 0L) + count);
            }
            for (int i = 0; i < size2; i++) {
                long v = rnd.nextInt(15);
                long count = rnd.nextInt(5) + 1;
                set2.add(v, count);
                std2.put(v, std2.getOrDefault(v, 0L) + count);
            }

            LongTreapMultiSet cap = set1.cap(set2);
            cap.getTreapMap().verify();

            long expectedTotalSize = 0;
            for (var entry : std1.entrySet()) {
                long key = entry.getKey();
                if (std2.containsKey(key)) {
                    long minVal = Math.min(entry.getValue(), std2.get(key));
                    assertEquals(minVal, cap.count(key));
                    expectedTotalSize += minVal;
                } else {
                    assertEquals(0, cap.count(key));
                }
            }
            assertEquals(expectedTotalSize, cap.size());
        }
    }
}
