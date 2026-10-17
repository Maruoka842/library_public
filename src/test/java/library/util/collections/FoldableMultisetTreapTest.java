package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class FoldableMultisetTreapTest {
    /** 追加・削除・空集合・コピーを検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void updatesAndCopy() {
        var set = new FoldableMultisetTreap<Integer, Long>(Long::sum, 0L, Integer::longValue);
        assertTrue(set.isEmpty());
        assertEquals(0L, set.allFold());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
        set.add(3, 4);
        set.add(1);
        set.add(2, 3);
        assertEquals(8, set.size());
        assertEquals(3, set.numberDistinctElements());
        assertEquals(19L, set.allFold());
        var copy = set.copy();
        assertTrue(set.remove(3, 2));
        set.add(2, -2);
        assertFalse(set.remove(1, 2));
        assertFalse(set.contains(1));
        assertEquals(8L, set.allFold());
        assertEquals(3, set.size());
        assertEquals(19L, copy.allFold());
        assertEquals(8, copy.size());
        assertTrue(copy.remove(2, 0));
        assertEquals(1, copy.pollFirst());
        assertEquals(3, copy.pollLast());
        assertEquals(3, copy.count(3));
        assertEquals(2, set.entryList().size());
        assertEquals(2, set.entryList().get(0).key);
        assertEquals(1, set.entryList().get(0).value);
        set.entryList().clear();
        assertEquals(3, set.size());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.numberDistinctElements());
        assertEquals(0L, set.allFold());
        assertEquals(6, copy.size());
    }

    /** 非可換積と重複の途中を端点とする添字区間を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void orderedFolds() {
        var set = new FoldableMultisetTreap<Integer, String>(String::concat, "", Object::toString);
        set.add(3, 2);
        set.add(1, 3);
        set.add(2, 4);
        String expected = "111222233";
        assertEquals(expected, set.allFold());
        for (int l = 0; l <= expected.length(); l++) {
            for (int r = l; r <= expected.length(); r++) {
                assertEquals(expected.substring(l, r), set.foldByIndex(l, r));
            }
        }
        assertEquals("1112222", set.fold(1, 3));
        assertEquals("2222", set.fold(2, 3));
        assertEquals("", set.fold(2, 2));
        assertEquals(expected, set.fold(0, 4));
        assertEquals("", set.fold(4, 5));
        assertEquals("111", set.foldLessThan(2));
        assertEquals("1112222", set.foldLessEqual(2));
        assertEquals("222233", set.foldGreaterEqual(2));
        assertEquals("33", set.foldGreaterThan(2));
        assertEquals(expected, set.allFold());
    }

    /** 順位・前後のキー・比較子による逆順を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void comparatorAndRanks() {
        var set = new FoldableMultisetTreap<Integer, String>(Comparator.reverseOrder(),
                String::concat, "", Object::toString);
        set.add(1, 2);
        set.add(3, 3);
        set.add(5);
        assertEquals("533311", set.allFold());
        assertEquals("5333", set.fold(5, 1));
        assertEquals("5", set.foldLessThan(3));
        assertEquals("5333", set.foldLessEqual(3));
        assertEquals("33311", set.foldGreaterEqual(3));
        assertEquals("11", set.foldGreaterThan(3));
        assertEquals(1, set.countLt(3));
        assertEquals(4, set.countLeq(3));
        assertEquals(5, set.countGeq(3));
        assertEquals(2, set.countGt(3));
        assertEquals(5, set.lower(3));
        assertEquals(3, set.floor(3));
        assertEquals(3, set.ceil(3));
        assertEquals(1, set.higher(3));
        assertEquals(5, set.peekFirst());
        assertEquals(1, set.peekLast());
        assertNull(set.lower(5));
        assertNull(set.higher(1));
        assertNull(set.kthKey(-1));
        assertNull(set.kthKey(6));
        assertEquals(List.of(5, 3, 3, 3, 1, 1),
                java.util.stream.LongStream.range(0, 6).mapToObj(set::kthKey).toList());
    }

    /** 比較で等しいキーの代表参照と個数を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void comparatorEquivalentKeys() {
        var set = new FoldableMultisetTreap<String, Long>(String.CASE_INSENSITIVE_ORDER,
                Long::sum, 0L, key -> (long) key.length());
        set.add("Ab", 2);
        set.add("aB", 3);
        assertEquals(1, set.numberDistinctElements());
        assertEquals(5, set.count("AB"));
        assertEquals("Ab", set.peekFirst());
        assertEquals(10L, set.allFold());
        assertTrue(set.remove("ab", 5));
        assertTrue(set.isEmpty());
    }

    /** long 上限で個数・順位・積・オーバーフロー時の不変性を検証する。計算量 O(log(Long.MAX_VALUE))。 */
    // 未テスト
    @Test
    void longMultiplicity() {
        var set = new FoldableMultisetTreap<Integer, Long>(Long::sum, 0L, key -> 1L);
        set.add(1, Long.MAX_VALUE - 2);
        set.add(2, 2);
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals(Long.MAX_VALUE, set.allFold());
        assertEquals(Long.MAX_VALUE - 2, set.countLt(2));
        assertEquals(Long.MAX_VALUE, set.countLeq(2));
        assertEquals(1, set.kthKey(Long.MAX_VALUE - 3));
        assertEquals(2, set.kthKey(Long.MAX_VALUE - 1));
        assertEquals(3L, set.foldByIndex(Long.MAX_VALUE - 3, Long.MAX_VALUE));
        assertEquals(2L, set.foldByIndex(Long.MAX_VALUE - 2, Long.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> set.add(3));
        assertThrows(ArithmeticException.class, () -> set.add(1, 3));
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals(0, set.count(3));
        assertEquals(2, set.numberDistinctElements());
        assertFalse(set.remove(1, Long.MAX_VALUE));
        assertEquals(2, set.size());
        set.add(2, -2);
        assertTrue(set.isEmpty());
    }

    /** 不正な操作の例外と集合の不変性を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void invalidOperations() {
        var set = new FoldableMultisetTreap<Integer, Long>(Long::sum, 0L, Integer::longValue);
        assertEquals(0L, set.foldByIndex(0, 0));
        assertTrue(set.remove(1, 0));
        set.add(1, 2);
        assertThrows(IllegalArgumentException.class, () -> set.add(1, -3));
        assertThrows(IllegalArgumentException.class, () -> set.add(2, -1));
        assertThrows(IllegalArgumentException.class, () -> set.add(1, Long.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> set.remove(1, -1));
        assertThrows(IllegalArgumentException.class, () -> set.fold(3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(0, 3));
        assertThrows(NullPointerException.class, () -> set.add(null));
        assertThrows(NullPointerException.class, () -> set.count(null));
        assertEquals(2, set.size());
        assertEquals(2L, set.allFold());
        assertEquals(1, set.numberDistinctElements());
    }

    /** 固定 seed の更新列を TreeMap および明示的な列と照合する。計算量 O(Q S log(D + 1))。 */
    // 未テスト
    @Test
    void mixedOperationsAgainstReference() {
        Random random = new Random(20261001);
        var set = new FoldableMultisetTreap<Integer, String>(String::concat, "", key -> "[" + key + "]");
        TreeMap<Integer, Long> reference = new TreeMap<>();
        for (int step = 0; step < 600; step++) {
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
            StringBuilder expected = new StringBuilder();
            for (var entry : reference.entrySet()) {
                for (long i = 0; i < entry.getValue(); i++) {
                    sequence.add(entry.getKey());
                    expected.append('[').append(entry.getKey()).append(']');
                }
            }
            assertEquals(sequence.size(), set.size());
            assertEquals(reference.size(), set.numberDistinctElements());
            assertEquals(expected.toString(), set.allFold());
            assertEquals(next, set.count(key));
            assertEquals(reference.lowerKey(key), set.lower(key));
            assertEquals(reference.floorKey(key), set.floor(key));
            assertEquals(reference.ceilingKey(key), set.ceil(key));
            assertEquals(reference.higherKey(key), set.higher(key));
            for (int i = 0; i < sequence.size(); i++) assertEquals(sequence.get(i), set.kthKey(i));
            int l = random.nextInt(sequence.size() + 1);
            int r = l + random.nextInt(sequence.size() - l + 1);
            StringBuilder range = new StringBuilder();
            for (int i = l; i < r; i++) range.append('[').append(sequence.get(i)).append(']');
            assertEquals(range.toString(), set.foldByIndex(l, r));
            int upper = key + random.nextInt(8);
            StringBuilder keyRange = new StringBuilder();
            StringBuilder prefix = new StringBuilder();
            StringBuilder suffix = new StringBuilder();
            long lt = 0, leq = 0;
            for (int value : sequence) {
                if (value < key) { lt++; prefix.append('[').append(value).append(']'); }
                if (value <= key) leq++;
                if (value > key) suffix.append('[').append(value).append(']');
                if (key <= value && value < upper) keyRange.append('[').append(value).append(']');
            }
            assertEquals(lt, set.countLt(key));
            assertEquals(leq, set.countLeq(key));
            assertEquals(sequence.size() - lt, set.countGeq(key));
            assertEquals(sequence.size() - leq, set.countGt(key));
            assertEquals(keyRange.toString(), set.fold(key, upper));
            assertEquals(prefix.toString(), set.foldLessThan(key));
            assertEquals(suffix.toString(), set.foldGreaterThan(key));
        }
        for (int key : new ArrayList<>(reference.keySet())) set.remove(key, Long.MAX_VALUE);
        assertTrue(set.isEmpty());
        assertEquals("", set.allFold());
    }
}
