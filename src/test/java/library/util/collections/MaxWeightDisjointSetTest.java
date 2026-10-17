package library.util.collections;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MaxWeightDisjointSetTest {
    /** clear 後の空状態、制約の維持、追加番号の再開、スナップショットを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void clearAndReuse() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(1, 2);
        sets.clear();
        var old = sets.add(new int[] {1}, 10);
        var snapshot = sets.query();
        sets.clear();
        sets.clear();
        assertEquals(0, sets.size());
        assertEquals(0, sets.representativeCount());
        assertTrue(sets.query().isEmpty());
        assertNull(sets.query(new int[0]));
        assertNull(sets.query(new int[] {1, 2}));
        assertEquals(java.util.List.of(old), snapshot);
        assertThrows(IllegalArgumentException.class, () -> sets.add(new int[] {1, 2}, 0));
        assertThrows(IllegalArgumentException.class, () -> sets.query(new int[] {1, 2, 3}));
        var next = sets.add(new int[] {2}, -10);
        assertEquals(0, next.id());
        assertSame(next, sets.query(new int[0]));
        assertEquals(1, sets.size());
        assertEquals(1, sets.representativeCount());
    }

    /** コピー間で同じ追加番号の更新を交互に行い、代表数と最大解の独立性を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void copyAndIndependentUpdates() {
        MaxWeightDisjointSet original = new MaxWeightDisjointSet(1, 2);
        var a = original.add(new int[] {1}, 10);
        var b = original.add(new int[] {2}, 9);
        MaxWeightDisjointSet copy = original.copy();
        assertNotSame(original, copy);
        assertEquals(original.size(), copy.size());
        assertEquals(original.query(), copy.query());
        assertSame(a, copy.query(new int[0]));
        assertSame(b, copy.query(new int[] {1}));
        var c = original.add(new int[] {3}, 8);
        var d = copy.add(new int[] {4}, 8);
        assertEquals(c.id(), d.id());
        assertEquals(3, original.representativeCount());
        assertEquals(3, copy.representativeCount());
        assertSame(c, original.query(new int[] {1, 2}));
        assertSame(d, copy.query(new int[] {1, 2}));
        var e = copy.add(new int[] {5}, 20);
        var f = original.add(new int[] {6}, 20);
        assertEquals(3, original.representativeCount());
        assertEquals(3, copy.representativeCount());
        assertSame(e, copy.query(new int[0]));
        assertSame(f, original.query(new int[0]));
        assertSame(b, original.query(new int[] {1, 6}));
        assertSame(b, copy.query(new int[] {1, 5}));
        original.clear();
        assertSame(e, copy.query(new int[0]));
        assertEquals(4, copy.size());
        var g = original.add(new int[0], 100);
        copy.clear();
        assertSame(g, original.query(new int[0]));
        assertEquals(0, copy.size());
    }

    /** 空構造・代表から除かれた追加・制約・コピーのコピーを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void copyPreservesCountsAndBounds() {
        MaxWeightDisjointSet empty = new MaxWeightDisjointSet(0, 1);
        MaxWeightDisjointSet emptyCopy = empty.copy();
        assertTrue(emptyCopy.query().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> emptyCopy.add(new int[] {1}, 0));
        assertThrows(IllegalArgumentException.class, () -> emptyCopy.query(new int[] {1, 2}));
        var best = empty.add(new int[0], 5);
        empty.add(new int[0], 4);
        empty.add(new int[0], 5);
        MaxWeightDisjointSet copy = empty.copy().copy();
        assertEquals(3, copy.size());
        assertEquals(1, copy.representativeCount());
        assertSame(best, copy.query(new int[0]));
        assertEquals(3, copy.add(new int[0], 5).id());
        assertSame(best, copy.query(new int[0]));
        assertEquals(3, empty.size());
        assertTrue(emptyCopy.query().isEmpty());
    }

    /** 代表集合の重複除去、参照共有、変更不能性、追加後のスナップショットを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void representativeSnapshot() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(2, 2);
        var initial = sets.query();
        assertTrue(initial.isEmpty());
        var a = sets.add(new int[] {1, 2}, 30);
        var b = sets.add(new int[] {3, 4}, 20);
        var c = sets.add(new int[0], 10);
        var snapshot = sets.query();
        assertEquals(sets.representativeCount(), snapshot.size());
        assertEquals(java.util.Set.of(a, b, c), new java.util.HashSet<>(snapshot));
        assertEquals(3, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(a));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.set(0, b));
        var iterator = snapshot.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        var dominant = sets.add(new int[0], 40);
        sets.add(new int[0], 40);
        assertEquals(java.util.List.of(dominant), sets.query());
        assertTrue(initial.isEmpty());
        assertEquals(3, snapshot.size());
        assertFalse(snapshot.contains(dominant));
        assertEquals(1, sets.representativeCount());
    }

    /** 基本操作、同重み、負の重みを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void basicOperations() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(3, 2);
        assertNull(sets.query(new int[] {1, 2}));
        var a = sets.add(new int[] {1, 2, 3}, 10);
        var b = sets.add(new int[] {2, 4}, 20);
        var c = sets.add(new int[] {5}, -7);
        assertSame(a, sets.query(new int[] {4, 5}));
        assertSame(b, sets.query(new int[] {1, 5}));
        assertSame(c, sets.query(new int[] {1, 2}));
        assertNull(sets.query(new int[] {2, 5}));
        sets.add(new int[] {2, 4}, 20);
        assertSame(b, sets.query(new int[] {1, 5}));
        var d = sets.add(new int[] {2, 4}, 21);
        assertSame(d, sets.query(new int[] {1, 5}));
        assertEquals(5, sets.size());
        assertEquals(4, d.id());
    }

    /** 空集合、整数端点、重み端点、配列の所有権を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void boundariesAndOwnership() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(Integer.MAX_VALUE, 1);
        int[] input = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        var a = sets.add(input, Long.MIN_VALUE);
        input[0] = 0;
        int[] copy = a.elements();
        copy[0] = 0;
        assertArrayEquals(new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE}, a.elements());
        assertEquals(Long.MIN_VALUE, a.weight());
        assertSame(a, sets.query(new int[] {0, 0}));
        assertNull(sets.query(new int[] {Integer.MIN_VALUE}));
        var empty = sets.add(new int[0], Long.MAX_VALUE);
        assertSame(empty, sets.query(new int[] {Integer.MAX_VALUE}));
        assertEquals(1, sets.representativeCount());
        MaxWeightDisjointSet onlyEmpty = new MaxWeightDisjointSet(0, 3);
        var minimum = onlyEmpty.add(new int[0], Long.MIN_VALUE);
        assertSame(minimum, onlyEmpty.query(new int[] {1, 2, 3}));
    }

    /** 不正な引数による失敗が既存状態を変えないことを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new MaxWeightDisjointSet(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MaxWeightDisjointSet(-1, 1));
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(1, 2);
        var a = sets.add(new int[] {3, 3}, 5);
        assertThrows(IllegalArgumentException.class, () -> sets.add(new int[] {1, 2}, 10));
        assertSame(a, sets.query(new int[] {1, 1}));
        assertThrows(IllegalArgumentException.class, () -> sets.query(new int[] {1, 2, 3}));
        assertThrows(NullPointerException.class, () -> sets.add(null, 0));
        assertThrows(NullPointerException.class, () -> sets.query(null));
        assertEquals(1, sets.size());
        assertSame(a, sets.query(new int[] {1, 2}));
    }

    /** 深さ q の探索、最大解の置換、代表から外れた集合を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void fullDepthAndReplacement() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(3, 3);
        sets.add(new int[] {1, 4, 5}, 100);
        sets.add(new int[] {2, 4, 5}, 90);
        sets.add(new int[] {3, 4, 5}, 80);
        var deepest = sets.add(new int[] {4, 5, 6}, 70);
        sets.add(new int[] {4, 5, 6}, 60);
        assertEquals(4, sets.representativeCount());
        assertSame(deepest, sets.query(new int[] {1, 2, 3}));
        var replacement = sets.add(new int[] {3, 7, 8}, 110);
        assertSame(deepest, sets.query(new int[] {1, 2, 3}));
        assertSame(replacement, sets.query(new int[] {1, 2, 4}));
        assertTrue(sets.representativeCount() <= 40);
    }

    /** 最大解の要素集合が変わっても、各除外条件の次善解が保存されることを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void changingRootPreservesAlternatives() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(2, 2);
        var a = sets.add(new int[] {1, 2}, 50);
        var b = sets.add(new int[] {3, 4}, 40);
        var c = sets.add(new int[] {5, 6}, 30);
        var d = sets.add(new int[] {1, 3}, 60);
        assertSame(b, sets.query(new int[] {1, 5}));
        assertSame(a, sets.query(new int[] {3, 5}));
        assertSame(c, sets.query(new int[] {1, 3}));
        var e = sets.add(new int[] {2, 4}, 55);
        assertSame(e, sets.query(new int[] {1, 3}));
        assertSame(c, sets.query(new int[] {2, 3}));
        sets.add(new int[] {1, 3}, 60);
        assertSame(d, sets.query(new int[] {2, 5}));
        var f = sets.add(new int[] {3, 1}, 70);
        assertSame(f, sets.query(new int[] {2, 5}));
        assertSame(c, sets.query(new int[] {2, 3}));
    }

    /** 部分木の共有後に一方の除外条件だけを改善しても、他方に影響しないことを検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void sharedSubtreesRemainIndependent() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(2, 2);
        sets.add(new int[] {1}, 50);
        var fallback = sets.add(new int[] {4}, 40);
        var root = sets.add(new int[] {2, 3}, 60);
        var improvement = sets.add(new int[] {2}, 55);
        assertSame(fallback, sets.query(new int[] {1, 2}));
        assertSame(improvement, sets.query(new int[] {1, 3}));
        assertSame(root, sets.query(new int[] {1, 4, 1}));
        var empty = sets.add(new int[0], 45);
        assertSame(empty, sets.query(new int[] {1, 2}));
        assertSame(improvement, sets.query(new int[] {1, 3}));
    }

    /** q > 3 と可変サイズの集合、単調増加する重み、空集合による支配を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void unrestrictedSizesAndLargerQ() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(Integer.MAX_VALUE, 5);
        var empty = sets.add(new int[0], Long.MIN_VALUE);
        var large = sets.add(new int[] {1, 2, 3, 4, 5, 6}, -2);
        var small = sets.add(new int[] {7}, -1);
        assertSame(empty, sets.query(new int[] {1, 7, 8, 9, 10}));
        assertSame(large, sets.query(new int[] {7, 8, 9, 10, 11}));
        assertSame(small, sets.query(new int[] {1, 2, 3, 4, 5}));
        var dominant = sets.add(new int[0], Long.MAX_VALUE);
        assertEquals(1, sets.representativeCount());
        sets.add(new int[] {Integer.MIN_VALUE}, Long.MAX_VALUE);
        assertEquals(1, sets.representativeCount());
        assertSame(dominant, sets.query(new int[] {1, 2, 3, 4, 5}));
    }

    /** 空集合から q 元集合までの検索、重複除去、上限超過を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void querySetsUpToQ() {
        MaxWeightDisjointSet sets = new MaxWeightDisjointSet(2, 3);
        assertNull(sets.query(new int[0]));
        assertNull(sets.query(new int[] {1}));
        var a = sets.add(new int[] {1, 2}, 30);
        var b = sets.add(new int[] {2, 3}, 20);
        var c = sets.add(new int[] {4}, 10);
        assertSame(a, sets.query(new int[0]));
        assertSame(b, sets.query(new int[] {1}));
        assertSame(c, sets.query(new int[] {1, 2}));
        assertSame(c, sets.query(new int[] {1, 2, 3}));
        assertNull(sets.query(new int[] {2, 4}));
        assertSame(b, sets.query(new int[] {1, 1, 1, 1}));
        assertThrows(IllegalArgumentException.class, () -> sets.query(new int[] {1, 2, 3, 4}));
        var empty = sets.add(new int[0], 40);
        sets.add(new int[] {5}, 40);
        assertSame(empty, sets.query(new int[0]));
        assertSame(empty, sets.query(new int[] {1}));
    }
}
