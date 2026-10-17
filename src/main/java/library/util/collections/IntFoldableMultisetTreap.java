package library.util.collections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntFunction;

/**
 * int 型のモノイド積を持つ比較順の列 a[0], ..., a[S-1]（重複を含む）を保持する多重集合。
 * foldByIndex(l, r) = f(a[l]) op ... op f(a[r-1])、空列の積は identity。
 * {@code op} は結合的で identity を両側単位元とし、可換性は要求しない。
 * 比較・f・op は純粋関数とし、格納中のキーは変更しないこと。
 * 比較で等しいキーは同じ f の値を持つこと。代表キーには最初に追加した参照を用いる。
 *
 * <p>D は異なるキーの数、S は総個数、C は操作対象のキーの個数。
 * 計算量は比較・f・op が O(1) の場合。空間 O(D)。
 *
 * @param <E> キーの型
 */
public class IntFoldableMultisetTreap<E extends Comparable<? super E>> {
    /** 優先度用乱数。 */
    private static final Random RANDOM = new Random();
    /** キーの比較子。null は自然順序。 */
    private final Comparator<? super E> comparator;
    /** モノイド演算。 */
    private final IntBinaryOperator op;
    /** モノイド単位元。 */
    private final int identity;
    /** キーからモノイド要素への写像 f。 */
    private final ToIntFunction<? super E> mapping;
    /** 根。 */
    private Node<E> root;

    /** 1 個の異なるキーとその部分木。 */
    private static final class Node<E> {
        /** 代表キー。 */
        final E key;
        /** f(key)。 */
        final int value;
        /** 優先度。 */
        final int priority;
        /** key の個数。 */
        long count;
        /** 部分木の総個数。 */
        long size;
        /** 部分木の異なるキーの数。 */
        int distinct;
        /** value^count。 */
        int block;
        /** 部分木の比較順の積。 */
        int product;
        /** 左部分木。 */
        Node<E> left;
        /** 右部分木。 */
        Node<E> right;

        /** key, value, priority を持つノードを作る。計算量 O(1)。 */
        // 未テスト
        Node(E key, int value, int priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }
    }

    /** キーと個数のスナップショット。キーの参照は共有する。 */
    public static final class Entry<E> {
        /** 代表キー。 */
        public final E key;
        /** 個数。 */
        public final long value;

        /** (key, value) を保持する。計算量 O(1)。 */
        // 未テスト
        public Entry(E key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * 自然順序の空集合を作る。
     * @param op モノイド演算
     * @param identity 単位元
     * @param mapping 写像 f
     * @complexity O(1)
     */
    // 未テスト
    public IntFoldableMultisetTreap(IntBinaryOperator op, int identity,
            ToIntFunction<? super E> mapping) {
        this(null, op, identity, mapping);
    }

    /**
     * comparator の順序の空集合を作る。null は自然順序。
     * @param comparator 比較子
     * @param op モノイド演算
     * @param identity 単位元
     * @param mapping 写像 f
     * @complexity O(1)
     */
    // 未テスト
    public IntFoldableMultisetTreap(Comparator<? super E> comparator, IntBinaryOperator op,
            int identity, ToIntFunction<? super E> mapping) {
        this.comparator = comparator;
        this.op = Objects.requireNonNull(op);
        this.identity = identity;
        this.mapping = Objects.requireNonNull(mapping);
    }

    /** compare(a, b) の符号を返す。計算量 O(1)。 */
    // 未テスト
    private int compare(E a, E b) {
        return comparator == null ? a.compareTo(b) : comparator.compare(a, b);
    }

    /** 部分木の総個数（null は 0）を返す。計算量 O(1)。 */
    // 未テスト
    private long size(Node<E> node) {
        return node == null ? 0 : node.size;
    }

    /** 部分木の異なるキー数（null は 0）を返す。計算量 O(1)。 */
    // 未テスト
    private int distinct(Node<E> node) {
        return node == null ? 0 : node.distinct;
    }

    /** 部分木の比較順の積（null は identity）を返す。計算量 O(1)。 */
    // 未テスト
    private int product(Node<E> node) {
        return node == null ? identity : node.product;
    }

    /** value^n を返す。n >= 0。計算量 O(log(n + 1))。 */
    // 未テスト
    private int power(int value, long n) {
        int result = identity;
        while (n > 0) {
            if ((n & 1) != 0) result = op.applyAsInt(result, value);
            n >>>= 1;
            if (n != 0) value = op.applyAsInt(value, value);
        }
        return result;
    }

    /** 部分木の総個数・異なるキー数・積を更新する。計算量 O(1)。 */
    // 未テスト
    private void recalc(Node<E> node) {
        node.size = size(node.left) + node.count + size(node.right);
        node.distinct = distinct(node.left) + 1 + distinct(node.right);
        node.product = op.applyAsInt(op.applyAsInt(product(node.left), node.block), product(node.right));
    }

    /** 左右のキーが left < right を満たす部分木を結合する。期待計算量 O(log(D + 1))。 */
    // 未テスト
    private Node<E> merge(Node<E> left, Node<E> right) {
        if (left == null) return right;
        if (right == null) return left;
        if (left.priority >= right.priority) {
            left.right = merge(left.right, right);
            recalc(left);
            return left;
        }
        right.left = merge(left, right.left);
        recalc(right);
        return right;
    }

    /**
     * 部分木内の key の個数を count にする。count >= 0。
     * 期待計算量 O(log(D + 1) + log(count + 1))。
     */
    // 未テスト
    private Node<E> setCount(Node<E> node, E key, long count) {
        if (node == null) {
            if (count == 0) return null;
            node = new Node<>(key, mapping.applyAsInt(key), RANDOM.nextInt());
            node.count = count;
            node.block = power(node.value, count);
        } else {
            int c = compare(key, node.key);
            if (c == 0) {
                if (count == 0) return merge(node.left, node.right);
                node.count = count;
                node.block = power(node.value, count);
            } else if (c < 0) {
                node.left = setCount(node.left, key, count);
                if (node.left != null && node.left.priority > node.priority) {
                    Node<E> child = node.left;
                    node.left = child.right;
                    child.right = node;
                    recalc(node);
                    node = child;
                }
            } else {
                node.right = setCount(node.right, key, count);
                if (node.right != null && node.right.priority > node.priority) {
                    Node<E> child = node.right;
                    node.right = child.left;
                    child.left = node;
                    recalc(node);
                    node = child;
                }
            }
        }
        recalc(node);
        return node;
    }

    /** count(element) := count(element) + 1。期待計算量 O(log(D + 1) + log(C + 2))。 */
    // 未テスト
    public void add(E element) {
        add(element, 1);
    }

    /**
     * count(element) := count(element) + repeat。repeat < 0 は削除。
     * 検証に失敗した場合、集合は変更しない。
     * @param element null でないキー
     * @param repeat 個数の増分
     * @throws IllegalArgumentException 変更後の個数が負
     * @throws ArithmeticException 変更後の総個数が Long.MAX_VALUE を超える
     * @complexity 期待値 O(log(D + 1) + log(C' + 1))。C' は変更後の個数
     */
    // 未テスト
    public void add(E element, long repeat) {
        Objects.requireNonNull(element);
        if (repeat == 0) return;
        long next = Math.addExact(count(element), repeat);
        if (next < 0) throw new IllegalArgumentException("negative count");
        Math.addExact(size(), repeat);
        root = setCount(root, element, next);
    }

    /** 最大 1 個削除し、削除前の count(element) >= 1 を返す。期待計算量 O(log(D + 1) + log(C + 1))。 */
    // 未テスト
    public boolean remove(E element) {
        return remove(element, 1);
    }

    /**
     * count(element) := max(0, count(element) - repeat)。
     * @param element null でないキー
     * @param repeat 削除要求個数
     * @return 削除前の count(element) >= repeat
     * @throws IllegalArgumentException repeat < 0
     * @complexity 期待値 O(log(D + 1) + log(C + 1))
     */
    // 未テスト
    public boolean remove(E element, long repeat) {
        Objects.requireNonNull(element);
        if (repeat < 0) throw new IllegalArgumentException("negative repeat");
        long old = count(element);
        if (repeat != 0 && old != 0) root = setCount(root, element, Math.max(0, old - repeat));
        return old >= repeat;
    }

    /** element の個数を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long count(E element) {
        Objects.requireNonNull(element);
        Node<E> node = root;
        while (node != null) {
            int c = compare(element, node.key);
            if (c == 0) return node.count;
            node = c < 0 ? node.left : node.right;
        }
        return 0;
    }

    /** count(element) > 0 を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public boolean contains(E element) {
        return count(element) > 0;
    }

    /** S = sum_x count(x) を返す。計算量 O(1)。 */
    // 未テスト
    public long size() {
        return size(root);
    }

    /** D = |{x : count(x) > 0}| を返す。計算量 O(1)。 */
    // 未テスト
    public int numberDistinctElements() {
        return distinct(root);
    }

    /** S = 0 を返す。計算量 O(1)。 */
    // 未テスト
    public boolean isEmpty() {
        return root == null;
    }

    /** 全キーの個数を 0 にする。計算量 O(1)。 */
    // 未テスト
    public void clear() {
        root = null;
    }

    /** x < key（inclusive なら x <= key）の総個数を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    private long rank(E key, boolean inclusive) {
        Objects.requireNonNull(key);
        long result = 0;
        Node<E> node = root;
        while (node != null) {
            int c = compare(node.key, key);
            if (c < 0 || (inclusive && c == 0)) {
                result += size(node.left) + node.count;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return result;
    }

    /** sum_{x < element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countLt(E element) {
        return rank(element, false);
    }

    /** sum_{x <= element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countLeq(E element) {
        return rank(element, true);
    }

    /** sum_{x >= element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countGeq(E element) {
        return size() - countLt(element);
    }

    /** sum_{x > element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countGt(E element) {
        return size() - countLeq(element);
    }

    /** a[k] を返す。k が [0, S) 外なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E kthKey(long k) {
        if (k < 0 || k >= size()) return null;
        Node<E> node = root;
        while (node != null) {
            long left = size(node.left);
            if (k < left) node = node.left;
            else if (k - left < node.count) return node.key;
            else {
                k -= left + node.count;
                node = node.right;
            }
        }
        throw new AssertionError("invalid subtree size");
    }

    /** max{x : x < element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E lower(E element) {
        return kthKey(countLt(element) - 1);
    }

    /** max{x : x <= element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E floor(E element) {
        return kthKey(countLeq(element) - 1);
    }

    /** min{x : x >= element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E ceil(E element) {
        return kthKey(countLt(element));
    }

    /** min{x : x > element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E higher(E element) {
        return kthKey(countLeq(element));
    }

    /** a[0] を返す。S = 0 なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E peekFirst() {
        return kthKey(0);
    }

    /** a[S-1] を返す。S = 0 なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public E peekLast() {
        return kthKey(size() - 1);
    }

    /** a[0] を 1 個削除して返す。S = 0 なら null。期待計算量 O(log(D + 1) + log(C + 1))。 */
    // 未テスト
    public E pollFirst() {
        E key = peekFirst();
        if (key != null) remove(key);
        return key;
    }

    /** a[S-1] を 1 個削除して返す。S = 0 なら null。期待計算量 O(log(D + 1) + log(C + 1))。 */
    // 未テスト
    public E pollLast() {
        E key = peekLast();
        if (key != null) remove(key);
        return key;
    }

    /** f(a[0]) op ... op f(a[S-1]) を返す。計算量 O(1)。 */
    // 未テスト
    public int allFold() {
        return product(root);
    }

    /**
     * f(a[l]) op ... op f(a[r-1]) を返す。
     * @param l 左端（含む）
     * @param r 右端（含まない）
     * @throws IndexOutOfBoundsException 0 <= l <= r <= S を満たさない
     * @complexity 期待値 O(log(D + 1) + log(S + 1))
     */
    // 未テスト
    public int foldByIndex(long l, long r) {
        if (l < 0 || l > r || r > size()) throw new IndexOutOfBoundsException();
        return foldByIndex(root, l, r);
    }

    /** 部分木の比較順の列の [l, r) の積を返す。期待計算量 O(log(D + 1) + log(S + 1))。 */
    // 未テスト
    private int foldByIndex(Node<E> node, long l, long r) {
        if (l == r) return identity;
        if (l == 0 && r == node.size) return node.product;
        long left = size(node.left);
        long end = left + node.count;
        int result = identity;
        if (l < left) result = foldByIndex(node.left, l, Math.min(r, left));
        long from = Math.max(l, left);
        long to = Math.min(r, end);
        if (from < to) {
            int block = to - from == node.count ? node.block : power(node.value, to - from);
            result = op.applyAsInt(result, block);
        }
        if (r > end) result = op.applyAsInt(result, foldByIndex(node.right, Math.max(0, l - end), r - end));
        return result;
    }

    /**
     * lower <= x < upper の要素の比較順の積を返す。
     * @param lower 左端（含む）
     * @param upper 右端（含まない）
     * @throws IllegalArgumentException lower > upper
     * @complexity 期待値 O(log(D + 1))
     */
    // 未テスト
    public int fold(E lower, E upper) {
        Objects.requireNonNull(lower);
        Objects.requireNonNull(upper);
        if (compare(lower, upper) > 0) throw new IllegalArgumentException("reversed range");
        return foldByIndex(countLt(lower), countLt(upper));
    }

    /** x < element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public int foldLessThan(E element) {
        return foldByIndex(0, countLt(element));
    }

    /** x <= element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public int foldLessEqual(E element) {
        return foldByIndex(0, countLeq(element));
    }

    /** x >= element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public int foldGreaterEqual(E element) {
        return foldByIndex(countLt(element), size());
    }

    /** x > element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public int foldGreaterThan(E element) {
        return foldByIndex(countLeq(element), size());
    }

    /** 比較順の (代表キー, 個数) を返す。キー参照は共有し、リストは独立。計算量 O(D)。 */
    // 未テスト
    public List<Entry<E>> entryList() {
        List<Entry<E>> entries = new ArrayList<>();
        entries(root, entries);
        return entries;
    }

    /** 部分木の (代表キー, 個数) を比較順で result に追加する。計算量 O(D)。 */
    // 未テスト
    private void entries(Node<E> node, List<Entry<E>> result) {
        if (node == null) return;
        entries(node.left, result);
        result.add(new Entry<>(node.key, node.count));
        entries(node.right, result);
    }

    /** キー・関数の参照を共有し、集合の変更が独立なコピーを返す。計算量 O(D)。 */
    // 未テスト
    public IntFoldableMultisetTreap<E> copy() {
        IntFoldableMultisetTreap<E> result = new IntFoldableMultisetTreap<E>(comparator, op, identity, mapping);
        result.root = copyNode(root);
        return result;
    }

    /** 部分木の構造を複製する。キーの参照は共有する。計算量 O(D)。 */
    // 未テスト
    private Node<E> copyNode(Node<E> node) {
        if (node == null) return null;
        Node<E> result = new Node<>(node.key, node.value, node.priority);
        result.count = node.count;
        result.size = node.size;
        result.distinct = node.distinct;
        result.block = node.block;
        result.product = node.product;
        result.left = copyNode(node.left);
        result.right = copyNode(node.right);
        return result;
    }
}
