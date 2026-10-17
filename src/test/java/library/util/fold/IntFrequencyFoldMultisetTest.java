package library.util.fold;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntFrequencyFoldMultisetTest {

    private static class SumStrategy implements IntFrequencyFoldMultiset.Strategy {
        @Override
        public long identity() {
            return 0;
        }

        @Override
        public long changeCount(long fold, int key, int oldCount, int newCount) {
            return fold + (long) key * (newCount - oldCount);
        }
    }

    @Test
    public void testClear() {
        IntFrequencyFoldMultiset set = new IntFrequencyFoldMultiset(5, new SumStrategy());
        set.add(1, 2);
        set.add(4, 3);

        set.clear();

        assertEquals(0, set.fold());
        for (int x = 0; x < 5; x++) assertEquals(0, set.count(x));
        set.add(3);
        assertEquals(3, set.fold());
    }

    @Test
    public void testBasicOperations() {
        IntFrequencyFoldMultiset set = new IntFrequencyFoldMultiset(5, new SumStrategy());
        assertEquals(0, set.fold());

        set.add(2);
        assertEquals(1, set.count(2));
        assertEquals(2, set.fold());

        set.add(2, 3);
        assertEquals(4, set.count(2));
        assertEquals(8, set.fold());

        set.remove(2);
        assertEquals(3, set.count(2));
        assertEquals(6, set.fold());

        set.remove(2, 2);
        assertEquals(1, set.count(2));
        assertEquals(2, set.fold());

        set.setCount(3, 5);
        assertEquals(5, set.count(3));
        assertEquals(17, set.fold());
    }

    @Test
    public void testCopy() {
        IntFrequencyFoldMultiset set1 = new IntFrequencyFoldMultiset(5, new SumStrategy());
        set1.add(1, 2);
        set1.add(3, 1);

        assertEquals(5, set1.fold());
        assertEquals(2, set1.count(1));
        assertEquals(1, set1.count(3));

        IntFrequencyFoldMultiset set2 = set1.copy();
        assertEquals(set1.fold(), set2.fold());
        assertEquals(set1.count(1), set2.count(1));
        assertEquals(set1.count(3), set2.count(3));

        // Modify set1 and verify set2 is unchanged
        set1.add(1);
        assertEquals(3, set1.count(1));
        assertEquals(2, set2.count(1));
        assertEquals(6, set1.fold());
        assertEquals(5, set2.fold());

        // Modify set2 and verify set1 is unchanged
        set2.add(3, 2);
        assertEquals(3, set2.count(3));
        assertEquals(1, set1.count(3));
        assertEquals(11, set2.fold());
        assertEquals(6, set1.fold());
    }

    @Test
    public void testToStringAndDump() {
        IntFrequencyFoldMultiset set = new IntFrequencyFoldMultiset(3, new SumStrategy());
        set.add(0, 2);
        set.add(2, 1);

        String str = set.toString();
        assertEquals("IntFrequencyFoldMultiset{count=[2, 0, 1], fold=2}", str);

        // Verify dump executes without exception
        assertDoesNotThrow(() -> set.dump());
    }

    @Test
    public void testExceptions() {
        IntFrequencyFoldMultiset set = new IntFrequencyFoldMultiset(3, new SumStrategy());
        assertThrows(IllegalArgumentException.class, () -> set.add(0, -1));
        assertThrows(IllegalArgumentException.class, () -> set.remove(0, -1));
        assertThrows(IllegalArgumentException.class, () -> set.remove(0));
        assertThrows(IllegalArgumentException.class, () -> set.remove(0, 2));
        assertThrows(IllegalArgumentException.class, () -> set.setCount(0, -1));
    }
}
