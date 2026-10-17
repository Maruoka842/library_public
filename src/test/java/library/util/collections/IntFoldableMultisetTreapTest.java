package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class IntFoldableMultisetTreapTest {
    /** 基本操作・コピー・不正な個数を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void updatesAndCopy() {
        var set = new IntFoldableMultisetTreap<Integer>((a, b) -> a + b, 0, key -> key);
        assertTrue(set.isEmpty());
        assertEquals(0, set.allFold());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
        assertEquals(0, set.foldByIndex(0, 0));
        set.add(3, 4);
        set.add(1);
        set.add(2, 3);
        assertEquals(8, set.size());
        assertEquals(3, set.numberDistinctElements());
        assertEquals(19, set.allFold());
        var copy = set.copy();
        assertTrue(set.remove(3, 2));
        set.add(2, -2);
        assertFalse(set.remove(1, 2));
        assertEquals(8, set.allFold());
        assertEquals(3, set.size());
        assertEquals(19, copy.allFold());
        assertEquals(8, copy.size());
        assertFalse(set.contains(1));
        assertEquals(2, set.entryList().get(0).key);
        assertEquals(1, set.entryList().get(0).value);
        assertTrue(set.remove(3, 0));
        assertEquals(2, set.pollFirst());
        assertEquals(3, set.pollLast());
        assertEquals(1, set.count(3));
        assertThrows(IllegalArgumentException.class, () -> set.add(3, -2));
        assertThrows(IllegalArgumentException.class, () -> set.remove(3, -1));
        assertThrows(IllegalArgumentException.class, () -> set.fold(3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(0, 2));
        assertThrows(NullPointerException.class, () -> set.add(null));
        assertEquals(1, set.size());
        assertEquals(3, set.allFold());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.allFold());
        assertEquals(0, set.numberDistinctElements());
        assertEquals(19, copy.allFold());
    }

    /** long 上限の重複数と集約値の整数演算を検証する。計算量 O(log(Long.MAX_VALUE))。 */
    // 未テスト
    @Test
    void longCountsAndOverflow() {
        var set = new IntFoldableMultisetTreap<Integer>((a, b) -> a + b, 0, key -> 1);
        set.add(1, Long.MAX_VALUE - 2);
        set.add(2, 2);
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals((int) Long.MAX_VALUE, set.allFold());
        assertEquals(Long.MAX_VALUE - 2, set.countLt(2));
        assertEquals(Long.MAX_VALUE, set.countLeq(2));
        assertEquals(2, set.kthKey(Long.MAX_VALUE - 1));
        assertEquals(3, set.foldByIndex(Long.MAX_VALUE - 3, Long.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> set.add(3));
        assertThrows(ArithmeticException.class, () -> set.add(1, 3));
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals(0, set.count(3));
        assertFalse(set.remove(1, Long.MAX_VALUE));
        assertEquals(2, set.size());
        assertEquals(2, set.allFold());
        set.add(2, -2);
        assertTrue(set.isEmpty());
    }

    /** (a,b) * (c,d) = (ac,bc+d) mod 256 を返す。計算量 O(1)。 */
    // 未テスト
    private static int compose(int x, int y) {
        int a = x >>> 8, b = x & 255, c = y >>> 8, d = y & 255;
        return ((a * c & 255) << 8) | ((b * c + d) & 255);
    }

    /** 非可換なアフィン変換の積と重複内の区間を検証する。計算量 O(S^3)。 */
    // 未テスト
    @Test
    void noncommutativeFolds() {
        var set = new IntFoldableMultisetTreap<Integer>(IntFoldableMultisetTreapTest::compose,
                256, key -> (key << 8) | (key + 1));
        set.add(3, 2);
        set.add(1, 3);
        set.add(2, 4);
        int[] keys = {1, 1, 1, 2, 2, 2, 2, 3, 3};
        for (int l = 0; l <= keys.length; l++) {
            for (int r = l; r <= keys.length; r++) {
                int expected = 256;
                for (int i = l; i < r; i++) expected = compose(expected, (keys[i] << 8) | (keys[i] + 1));
                assertEquals(expected, set.foldByIndex(l, r));
            }
        }
        assertEquals(set.foldByIndex(0, 9), set.allFold());
        assertEquals(set.foldByIndex(0, 7), set.fold(1, 3));
        assertEquals(set.foldByIndex(0, 3), set.foldLessThan(2));
        assertEquals(set.foldByIndex(0, 7), set.foldLessEqual(2));
        assertEquals(set.foldByIndex(3, 9), set.foldGreaterEqual(2));
        assertEquals(set.foldByIndex(7, 9), set.foldGreaterThan(2));
    }

    /** 固定 seed の更新列を逆順 TreeMap と照合する。計算量 O(Q S log(D + 1))。 */
    // 未テスト
    @Test
    void mixedOperationsAgainstReference() {
        var set = new IntFoldableMultisetTreap<Integer>(Comparator.reverseOrder(),
                (a, b) -> a + b, 0, key -> key);
        TreeMap<Integer, Long> reference = new TreeMap<>(Comparator.reverseOrder());
        Random random = new Random(20261001);
        for (int step = 0; step < 400; step++) {
            int key = random.nextInt(21) - 10;
            long old = reference.getOrDefault(key, 0L);
            long repeat = random.nextInt(5);
            long next;
            if (random.nextBoolean()) {
                set.add(key, repeat);
                next = old + repeat;
            } else {
                assertEquals(old >= repeat, set.remove(key, repeat));
                next = Math.max(0, old - repeat);
            }
            if (next == 0) reference.remove(key);
            else reference.put(key, next);
            List<Integer> sequence = new ArrayList<>();
            int expected = 0;
            for (var entry : reference.entrySet()) {
                for (long i = 0; i < entry.getValue(); i++) {
                    sequence.add(entry.getKey());
                    expected += entry.getKey();
                }
            }
            assertEquals(sequence.size(), set.size());
            assertEquals(reference.size(), set.numberDistinctElements());
            assertEquals(expected, set.allFold());
            assertEquals(next, set.count(key));
            assertEquals(reference.lowerKey(key), set.lower(key));
            assertEquals(reference.floorKey(key), set.floor(key));
            assertEquals(reference.ceilingKey(key), set.ceil(key));
            assertEquals(reference.higherKey(key), set.higher(key));
            assertEquals(sequence.isEmpty() ? null : sequence.get(0), set.peekFirst());
            assertEquals(sequence.isEmpty() ? null : sequence.get(sequence.size() - 1), set.peekLast());
            assertNull(set.kthKey(-1));
            assertNull(set.kthKey(sequence.size()));
            for (int i = 0; i < sequence.size(); i++) assertEquals(sequence.get(i), set.kthKey(i));
            int l = random.nextInt(sequence.size() + 1);
            int r = l + random.nextInt(sequence.size() - l + 1);
            int range = 0;
            for (int i = l; i < r; i++) range += sequence.get(i);
            assertEquals(range, set.foldByIndex(l, r));
            int upper = key - random.nextInt(8);
            int keyRange = 0, prefix = 0, suffix = 0;
            long lt = 0, leq = 0;
            for (int value : sequence) {
                if (value > key) { lt++; prefix += value; }
                if (value >= key) leq++;
                if (value < key) suffix += value;
                if (key >= value && value > upper) keyRange += value;
            }
            assertEquals(lt, set.countLt(key));
            assertEquals(leq, set.countLeq(key));
            assertEquals(sequence.size() - lt, set.countGeq(key));
            assertEquals(sequence.size() - leq, set.countGt(key));
            assertEquals(keyRange, set.fold(key, upper));
            assertEquals(prefix, set.foldLessThan(key));
            assertEquals(suffix, set.foldGreaterThan(key));
        }
        for (int key : new ArrayList<>(reference.keySet())) set.remove(key, Long.MAX_VALUE);
        assertTrue(set.isEmpty());
        assertEquals(0, set.allFold());
    }
}
