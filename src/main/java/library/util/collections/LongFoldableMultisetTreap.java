package library.util.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongBinaryOperator;
import library.util.LongComparator;
import library.util.algebra.strategy.longs.LongMonoidStrategy;

/**
 * long 型のモノイド積を持つ比較順の列 a[0], ..., a[S-1]（重複を含む）を保持する多重集合。
 * foldByIndex(l, r) = a[l] op ... op a[r-1]、空列の積は identity。
 * {@code op} は結合的で identity を両側単位元とし、可換性は要求しない。
 * 比較・op は純粋関数とし、比較順序は集合の存続中に変更しないこと。
 * compare(a, b) = 0 と a = b は同値であること。
 *
 * <p>D は異なるキーの数、S は総個数、C は操作対象のキーの個数。
 * 計算量は比較・op が O(1) の場合。空間 O(D)。
 * P(n) は n 個の同一要素の積を求める計算量。
 * strategy を指定した場合は strategy.pow の計算量、それ以外は O(log(n + 1))。
 *
 * キー・集約値・重複数は long 型。
 */
public class LongFoldableMultisetTreap {
    /** 優先度用乱数。 */
    private static final Random RANDOM = new Random();
    /** キーの比較子。null は自然順序。 */
    private final LongComparator comparator;
    /** モノイド演算。 */
    private final LongBinaryOperator op;
    /** モノイド単位元。 */
    private final long identity;
    /** 重複分の積を定義するモノイド。null なら二項演算による累乗を使う。 */
    private final LongMonoidStrategy strategy;
    /** 根。 */
    private Node root;

    /** 1 個の異なるキーとその部分木。 */
    private static final class Node {
        /** 代表キー。fold に用いる値も key。 */
        final long key;
        /** 優先度。 */
        final int priority;
        /** key の個数。 */
        long count;
        /** 部分木の総個数。 */
        long size;
        /** 部分木の異なるキーの数。 */
        int distinct;
        /** key^count。 */
        long block;
        /** 部分木の比較順の積。 */
        long product;
        /** 左部分木。 */
        Node left;
        /** 右部分木。 */
        Node right;

        /** key, priority を持つノードを作る。計算量 O(1)。 */
        // 未テスト
        Node(long key, int priority) {
            this.key = key;
            this.priority = priority;
        }
    }

    /** キーと個数のスナップショット。キーは値として保持する。 */
    public static final class Entry {
        /** 代表キー。 */
        public final long key;
        /** 個数。 */
        public final long value;

        /** (key, value) を保持する。計算量 O(1)。 */
        // 未テスト
        public Entry(long key, long value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * 自然順序、演算 strategy.mul、単位元 strategy.identity() の空集合を作る。
     * 重複分の積は strategy.pow で定義する。
     * strategy の演算・累乗・単位元は集合の存続中に変更しないこと。
     * @param strategy long 型のモノイド
     * @complexity O(1)。strategy.identity() は O(1) とする
     */
    // 未テスト
    public LongFoldableMultisetTreap(LongMonoidStrategy strategy) {
        this(null, strategy);
    }

    /**
     * comparator の順序、演算 strategy.mul、単位元 strategy.identity() の空集合を作る。
     * comparator が null なら自然順序。重複分の積は strategy.pow で定義する。
     * strategy の演算・累乗・単位元は集合の存続中に変更しないこと。
     * @param comparator 比較子
     * @param strategy long 型のモノイド
     * @complexity O(1)。strategy.identity() は O(1) とする
     */
    // 未テスト
    public LongFoldableMultisetTreap(LongComparator comparator, LongMonoidStrategy strategy) {
        this.comparator = comparator;
        this.strategy = Objects.requireNonNull(strategy);
        this.op = strategy::mul;
        this.identity = strategy.identity();
    }

    /**
     * 自然順序の空集合を作る。
     * @param op モノイド演算
     * @param identity 単位元
     * @complexity O(1)
     */
    // 未テスト
    public LongFoldableMultisetTreap(LongBinaryOperator op, long identity) {
        this(null, op, identity);
    }

    /**
     * comparator の順序の空集合を作る。null は自然順序。
     * @param comparator 比較子
     * @param op モノイド演算
     * @param identity 単位元
     * @complexity O(1)
     */
    // 未テスト
    public LongFoldableMultisetTreap(LongComparator comparator, LongBinaryOperator op,
            long identity) {
        this.comparator = comparator;
        this.op = Objects.requireNonNull(op);
        this.identity = identity;
        this.strategy = null;
    }

    /** compare(a, b) の符号を返す。計算量 O(1)。 */
    // 未テスト
    private int compare(long a, long b) {
        return comparator == null ? Long.compare(a, b) : comparator.compare(a, b);
    }

    /** 部分木の総個数（null は 0）を返す。計算量 O(1)。 */
    // 未テスト
    private long size(Node node) {
        return node == null ? 0 : node.size;
    }

    /** 部分木の異なるキー数（null は 0）を返す。計算量 O(1)。 */
    // 未テスト
    private int distinct(Node node) {
        return node == null ? 0 : node.distinct;
    }

    /** 部分木の比較順の積（null は identity）を返す。計算量 O(1)。 */
    // 未テスト
    private long product(Node node) {
        return node == null ? identity : node.product;
    }

    /** value^n を返す。n >= 0。計算量 P(n)。 */
    // 未テスト
    private long power(long value, long n) {
        if (strategy != null) return strategy.pow(value, n);
        long result = identity;
        while (n > 0) {
            if ((n & 1) != 0) result = op.applyAsLong(result, value);
            n >>>= 1;
            if (n != 0) value = op.applyAsLong(value, value);
        }
        return result;
    }

    /** 部分木の総個数・異なるキー数・積を更新する。計算量 O(1)。 */
    // 未テスト
    private void recalc(Node node) {
        node.size = size(node.left) + node.count + size(node.right);
        node.distinct = distinct(node.left) + 1 + distinct(node.right);
        node.product = op.applyAsLong(op.applyAsLong(product(node.left), node.block), product(node.right));
    }

    /** 左右のキーが left < right を満たす部分木を結合する。期待計算量 O(log(D + 1))。 */
    // 未テスト
    private Node merge(Node left, Node right) {
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
     * 期待計算量 O(log(D + 1) + P(count))。
     */
    // 未テスト
    private Node setCount(Node node, long key, long count) {
        if (node == null) {
            if (count == 0) return null;
            node = new Node(key, RANDOM.nextInt());
            node.count = count;
            node.block = power(node.key, count);
        } else {
            int c = compare(key, node.key);
            if (c == 0) {
                if (count == 0) return merge(node.left, node.right);
                node.count = count;
                node.block = power(node.key, count);
            } else if (c < 0) {
                node.left = setCount(node.left, key, count);
                if (node.left != null && node.left.priority > node.priority) {
                    Node child = node.left;
                    node.left = child.right;
                    child.right = node;
                    recalc(node);
                    node = child;
                }
            } else {
                node.right = setCount(node.right, key, count);
                if (node.right != null && node.right.priority > node.priority) {
                    Node child = node.right;
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

    /** count(element) := count(element) + 1。期待計算量 O(log(D + 1) + P(C + 1))。 */
    // 未テスト
    public void add(long element) {
        add(element, 1);
    }

    /**
     * count(element) := count(element) + repeat。repeat < 0 は削除。
     * 検証に失敗した場合、集合は変更しない。
     * @param element キー
     * @param repeat 個数の増分
     * @throws IllegalArgumentException 変更後の個数が負
     * @throws ArithmeticException 変更後の総個数が Long.MAX_VALUE を超える
     * @complexity 期待値 O(log(D + 1) + P(C'))。C' は変更後の個数
     */
    // 未テスト
    public void add(long element, long repeat) {
        if (repeat == 0) return;
        long next = Math.addExact(count(element), repeat);
        if (next < 0) throw new IllegalArgumentException("negative count");
        Math.addExact(size(), repeat);
        root = setCount(root, element, next);
    }

    /** 最大 1 個削除し、削除前の count(element) >= 1 を返す。期待計算量 O(log(D + 1) + P(C'))。C' は削除後の個数。 */
    // 未テスト
    public boolean remove(long element) {
        return remove(element, 1);
    }

    /**
     * count(element) := max(0, count(element) - repeat)。
     * @param element キー
     * @param repeat 削除要求個数
     * @return 削除前の count(element) >= repeat
     * @throws IllegalArgumentException repeat < 0
     * @complexity 期待値 O(log(D + 1) + P(C'))。C' は削除後の個数
     */
    // 未テスト
    public boolean remove(long element, long repeat) {
        if (repeat < 0) throw new IllegalArgumentException("negative repeat");
        long old = count(element);
        if (repeat != 0 && old != 0) root = setCount(root, element, Math.max(0, old - repeat));
        return old >= repeat;
    }

    /** element の個数を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long count(long element) {
        Node node = root;
        while (node != null) {
            int c = compare(element, node.key);
            if (c == 0) return node.count;
            node = c < 0 ? node.left : node.right;
        }
        return 0;
    }

    /** count(element) > 0 を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public boolean contains(long element) {
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
    private long rank(long key, boolean inclusive) {
        long result = 0;
        Node node = root;
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
    public long countLt(long element) {
        return rank(element, false);
    }

    /** sum_{x <= element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countLeq(long element) {
        return rank(element, true);
    }

    /** sum_{x >= element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countGeq(long element) {
        return size() - countLt(element);
    }

    /** sum_{x > element} count(x) を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long countGt(long element) {
        return size() - countLeq(element);
    }

    /** a[k] を返す。k が [0, S) 外なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long kthKey(long k) {
        if (k < 0 || k >= size()) return null;
        Node node = root;
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
    public Long lower(long element) {
        return kthKey(countLt(element) - 1);
    }

    /** max{x : x <= element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long floor(long element) {
        return kthKey(countLeq(element) - 1);
    }

    /** min{x : x >= element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long ceil(long element) {
        return kthKey(countLt(element));
    }

    /** min{x : x > element} を返す。空なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long higher(long element) {
        return kthKey(countLeq(element));
    }

    /** a[0] を返す。S = 0 なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long peekFirst() {
        return kthKey(0);
    }

    /** a[S-1] を返す。S = 0 なら null。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public Long peekLast() {
        return kthKey(size() - 1);
    }

    /** a[0] を 1 個削除して返す。S = 0 なら null。期待計算量 O(log(D + 1) + P(C'))。C' は削除後の個数。 */
    // 未テスト
    public Long pollFirst() {
        Long key = peekFirst();
        if (key != null) remove(key);
        return key;
    }

    /** a[S-1] を 1 個削除して返す。S = 0 なら null。期待計算量 O(log(D + 1) + P(C'))。C' は削除後の個数。 */
    // 未テスト
    public Long pollLast() {
        Long key = peekLast();
        if (key != null) remove(key);
        return key;
    }

    /** a[0] op ... op a[S-1] を返す。計算量 O(1)。 */
    // 未テスト
    public long allFold() {
        return product(root);
    }

    /**
     * a[l] op ... op a[r-1] を返す。
     * @param l 左端（含む）
     * @param r 右端（含まない）
     * @throws IndexOutOfBoundsException 0 <= l <= r <= S を満たさない
     * @complexity 期待値 O(log(D + 1) + max_{0 <= n <= S} P(n))
     */
    // 未テスト
    public long foldByIndex(long l, long r) {
        if (l < 0 || l > r || r > size()) throw new IndexOutOfBoundsException();
        return foldByIndex(root, l, r);
    }

    /** 部分木の比較順の列の [l, r) の積を返す。期待計算量 O(log(D + 1) + max_{0 <= n <= S} P(n))。 */
    // 未テスト
    private long foldByIndex(Node node, long l, long r) {
        if (l == r) return identity;
        if (l == 0 && r == node.size) return node.product;
        long left = size(node.left);
        long end = left + node.count;
        long result = identity;
        if (l < left) result = foldByIndex(node.left, l, Math.min(r, left));
        long from = Math.max(l, left);
        long to = Math.min(r, end);
        if (from < to) {
            long block = to - from == node.count ? node.block : power(node.key, to - from);
            result = op.applyAsLong(result, block);
        }
        if (r > end) result = op.applyAsLong(result, foldByIndex(node.right, Math.max(0, l - end), r - end));
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
    public long fold(long lower, long upper) {
        if (compare(lower, upper) > 0) throw new IllegalArgumentException("reversed range");
        return foldByIndex(countLt(lower), countLt(upper));
    }

    /** x < element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long foldLessThan(long element) {
        return foldByIndex(0, countLt(element));
    }

    /** x <= element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long foldLessEqual(long element) {
        return foldByIndex(0, countLeq(element));
    }

    /** x >= element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long foldGreaterEqual(long element) {
        return foldByIndex(countLt(element), size());
    }

    /** x > element の要素の比較順の積を返す。期待計算量 O(log(D + 1))。 */
    // 未テスト
    public long foldGreaterThan(long element) {
        return foldByIndex(countLeq(element), size());
    }

    /** 比較順の (代表キー, 個数) を返す。リストは独立。計算量 O(D)。 */
    // 未テスト
    public List<Entry> entryList() {
        List<Entry> entries = new ArrayList<>();
        entries(root, entries);
        return entries;
    }

    /**
     * 比較順に各キー x を「x が count(x)個」の形式で標準出力へ 1 行ずつ出力する。
     * S = 0 なら「空集合」を 1 行出力する。
     * @complexity O(D + 1)
     */
    // 未テスト
    public void dump() {
        if (isEmpty()) {
            System.out.println("空集合");
            return;
        }
        for (Entry entry : entryList()) {
            System.out.println(entry.key + " が " + entry.value + "個");
        }
    }

    /** 部分木の (代表キー, 個数) を比較順で result に追加する。計算量 O(D)。 */
    // 未テスト
    private void entries(Node node, List<Entry> result) {
        if (node == null) return;
        entries(node.left, result);
        result.add(new Entry(node.key, node.count));
        entries(node.right, result);
    }

    /** 関数の参照を共有し、集合の変更が独立なコピーを返す。計算量 O(D)。 */
    // 未テスト
    public LongFoldableMultisetTreap copy() {
        LongFoldableMultisetTreap result = strategy == null
                ? new LongFoldableMultisetTreap(comparator, op, identity)
                : new LongFoldableMultisetTreap(comparator, strategy);
        result.root = copyNode(root);
        return result;
    }

    /** 部分木の構造を複製する。キーは値として保持する。計算量 O(D)。 */
    // 未テスト
    private Node copyNode(Node node) {
        if (node == null) return null;
        Node result = new Node(node.key, node.priority);
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
