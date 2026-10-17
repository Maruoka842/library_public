package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import library.util.algebra.strategy.longs.LongMonoidStrategy;

public class LongFoldableMultisetTreapTest {
    /** 更新・重複内区間・コピーが O(1) の strategy.pow を使うことを検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void overriddenPowerIsUsed() {
        long[] calls = new long[2];
        LongMonoidStrategy strategy = new LongMonoidStrategy() {
            /** 加法の単位元 0 を返す。計算量 O(1)。 */
            // 未テスト
            @Override
            public long identity() {
                return 0;
            }

            /** a + b を返し、calls[0] を 1 増やす。計算量 O(1)。 */
            // 未テスト
            @Override
            public long mul(long a, long b) {
                calls[0]++;
                return a + b;
            }

            /** a * n を返し、calls[1] を 1 増やす。n >= 0。計算量 O(1)。 */
            // 未テスト
            @Override
            public long pow(long a, long n) {
                calls[1]++;
                return a * n;
            }
        };
        var set = new LongFoldableMultisetTreap(strategy);
        long count = 1_000_000_000_000L;
        set.add(3, count);
        assertEquals(3 * count, set.allFold());
        assertEquals(1, calls[1]);
        assertEquals(2, calls[0]);
        set.add(3, 2);
        assertEquals(3 * (count + 2), set.allFold());
        assertEquals(2, calls[1]);
        assertEquals(9, set.foldByIndex(1, 4));
        assertEquals(3, calls[1]);
        set.remove(3, 2);
        assertEquals(3 * count, set.allFold());
        assertEquals(4, calls[1]);
        var copy = set.copy();
        assertEquals(4, calls[1]);
        copy.add(3);
        assertEquals(3 * (count + 1), copy.allFold());
        assertEquals(3 * count, set.allFold());
        assertEquals(5, calls[1]);
        assertEquals(6, copy.foldByIndex(0, 2));
        assertEquals(6, calls[1]);
        var reverse = new LongFoldableMultisetTreap((a, b) -> Long.compare(b, a), strategy);
        reverse.add(5, count);
        assertEquals(5 * count, reverse.allFold());
        assertEquals(7, calls[1]);
    }

    /** strategy の演算・非零単位元・比較子・コピーを検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void monoidStrategyConstructors() {
        LongMonoidStrategy strategy = new LongMonoidStrategy() {
            /** min の単位元 Long.MAX_VALUE を返す。計算量 O(1)。 */
            // 未テスト
            @Override
            public long identity() {
                return Long.MAX_VALUE;
            }

            /** min(a, b) を返す。計算量 O(1)。 */
            // 未テスト
            @Override
            public long mul(long a, long b) {
                return Math.min(a, b);
            }
        };
        var set = new LongFoldableMultisetTreap(strategy);
        assertEquals(Long.MAX_VALUE, set.allFold());
        set.add(7, 3);
        set.add(-2, 2);
        assertEquals(-2, set.allFold());
        assertEquals(7, set.foldByIndex(2, 4));
        assertEquals(Long.MAX_VALUE, set.foldByIndex(3, 3));
        var copy = set.copy();
        set.remove(-2, 2);
        assertEquals(7, set.allFold());
        assertEquals(-2, copy.allFold());
        var reverse = new LongFoldableMultisetTreap((a, b) -> Long.compare(b, a), strategy);
        reverse.add(7, 3);
        reverse.add(-2, 2);
        assertEquals(7, reverse.foldByIndex(0, 3));
        assertEquals(-2, reverse.foldByIndex(3, 5));
        assertEquals(-2, reverse.allFold());
        assertThrows(NullPointerException.class, () -> new LongFoldableMultisetTreap((LongMonoidStrategy) null));
        assertThrows(NullPointerException.class, () -> new LongFoldableMultisetTreap(null, (LongMonoidStrategy) null));
    }

    /** 基本操作・コピー・不正な個数を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void updatesAndCopy() {
        var set = new LongFoldableMultisetTreap(Long::sum, 0);
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
        assertEquals(2L, set.pollFirst());
        assertEquals(3L, set.pollLast());
        assertEquals(1, set.count(3));
        assertThrows(IllegalArgumentException.class, () -> set.add(3, -2));
        assertThrows(IllegalArgumentException.class, () -> set.remove(3, -1));
        assertThrows(IllegalArgumentException.class, () -> set.fold(3, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> set.foldByIndex(0, 2));
        assertThrows(NullPointerException.class, () -> new LongFoldableMultisetTreap(null, 0));
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
        var set = new LongFoldableMultisetTreap(Long::sum, 0);
        set.add(0, Long.MAX_VALUE - 2);
        set.add(1, 2);
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals(2, set.allFold());
        assertEquals(Long.MAX_VALUE - 2, set.countLt(1));
        assertEquals(Long.MAX_VALUE, set.countLeq(1));
        assertEquals(1L, set.kthKey(Long.MAX_VALUE - 1));
        assertEquals(2, set.foldByIndex(Long.MAX_VALUE - 3, Long.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> set.add(3));
        assertThrows(ArithmeticException.class, () -> set.add(0, 3));
        assertEquals(Long.MAX_VALUE, set.size());
        assertEquals(0, set.count(3));
        assertFalse(set.remove(0, Long.MAX_VALUE));
        assertEquals(2, set.size());
        assertEquals(2, set.allFold());
        set.add(1, -2);
        assertTrue(set.isEmpty());
    }

    /** long の全値域と減算によらない比較を検証する。計算量 O(1)。 */
    // 未テスト
    @Test
    void extremeKeys() {
        var set = new LongFoldableMultisetTreap(Long::sum, 0);
        set.add(Long.MAX_VALUE, 2);
        set.add(Long.MIN_VALUE, 3);
        set.add(0);
        assertEquals(Long.MIN_VALUE, set.peekFirst());
        assertEquals(Long.MAX_VALUE, set.peekLast());
        assertEquals(3, set.countLt(0));
        assertEquals(6, set.countLeq(Long.MAX_VALUE));
        assertEquals(0, set.countLt(Long.MIN_VALUE));
        assertEquals(Long.MIN_VALUE, set.lower(0));
        assertEquals(Long.MAX_VALUE, set.higher(0));
        assertNull(set.lower(Long.MIN_VALUE));
        assertNull(set.higher(Long.MAX_VALUE));
        assertEquals(Long.MAX_VALUE - 1, set.allFold());
        assertEquals(Long.MIN_VALUE, set.fold(Long.MIN_VALUE, Long.MAX_VALUE));
        assertEquals(-1, set.foldByIndex(2, 5));
        assertEquals(Long.MIN_VALUE, set.entryList().get(0).key);
        var reverse = new LongFoldableMultisetTreap((a, b) -> Long.compare(b, a),
                Long::sum, 0);
        reverse.add(Long.MIN_VALUE);
        reverse.add(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, reverse.peekFirst());
        assertEquals(Long.MIN_VALUE, reverse.peekLast());
        assertEquals(Long.MAX_VALUE, reverse.fold(Long.MAX_VALUE, Long.MIN_VALUE));
        assertEquals(Long.MAX_VALUE, reverse.pollFirst());
        assertEquals(Long.MIN_VALUE, reverse.pollLast());
        assertNull(reverse.pollFirst());
    }

    /** (a,b) * (c,d) = (ac,bc+d) mod 256 を返す。計算量 O(1)。 */
    // 未テスト
    private static long compose(long x, long y) {
        long a = x >>> 8, b = x & 255, c = y >>> 8, d = y & 255;
        return ((a * c & 255) << 8) | ((b * c + d) & 255);
    }

    /** 非可換なアフィン変換の積と重複内の区間を検証する。計算量 O(S^3)。 */
    // 未テスト
    @Test
    void noncommutativeFolds() {
        var set = new LongFoldableMultisetTreap(LongFoldableMultisetTreapTest::compose,
                256);
        set.add(772, 2);
        set.add(258, 3);
        set.add(515, 4);
        long[] keys = {258, 258, 258, 515, 515, 515, 515, 772, 772};
        for (int l = 0; l <= keys.length; l++) {
            for (int r = l; r <= keys.length; r++) {
                long expected = 256;
                for (int i = l; i < r; i++) expected = compose(expected, keys[i]);
                assertEquals(expected, set.foldByIndex(l, r));
            }
        }
        assertEquals(set.foldByIndex(0, 9), set.allFold());
        assertEquals(set.foldByIndex(0, 7), set.fold(258, 772));
        assertEquals(set.foldByIndex(0, 3), set.foldLessThan(515));
        assertEquals(set.foldByIndex(0, 7), set.foldLessEqual(515));
        assertEquals(set.foldByIndex(3, 9), set.foldGreaterEqual(515));
        assertEquals(set.foldByIndex(7, 9), set.foldGreaterThan(515));
    }

    /** 固定 seed の更新列を逆順 TreeMap と照合する。計算量 O(Q S log(D + 1))。 */
    // 未テスト
    @Test
    void mixedOperationsAgainstReference() {
        var set = new LongFoldableMultisetTreap((a, b) -> Long.compare(b, a),
                (a, b) -> a + b, 0);
        TreeMap<Long, Long> reference = new TreeMap<>(Comparator.reverseOrder());
        Random random = new Random(20261001);
        for (int step = 0; step < 400; step++) {
            long key = random.nextInt(21) - 10;
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
            List<Long> sequence = new ArrayList<>();
            long expected = 0;
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
            long range = 0;
            for (int i = l; i < r; i++) range += sequence.get(i);
            assertEquals(range, set.foldByIndex(l, r));
            long upper = key - random.nextInt(8);
            long keyRange = 0, prefix = 0, suffix = 0;
            long lt = 0, leq = 0;
            for (long value : sequence) {
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
        for (long key : new ArrayList<>(reference.keySet())) set.remove(key, Long.MAX_VALUE);
        assertTrue(set.isEmpty());
        assertEquals(0, set.allFold());
    }
}
