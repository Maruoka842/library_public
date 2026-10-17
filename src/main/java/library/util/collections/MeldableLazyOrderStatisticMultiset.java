package library.util.collections;

import java.util.ArrayList;
import java.util.List;

/**
 * ソート済み多重集合に対して、順位操作・lazy action・破壊的 meld を行う Treap。
 *
 * <p>各異なる値は 1 ノードにまとめ、その重複度を {@code multiplicity} として保持する。
 * 順位は multiplicity を展開した列に対して定義する。
 *
 * <p>作用 {@code F} は key の順序を狭義に保存することを要求する。
 * すなわち任意の action {@code f} について
 *
 * <pre>
 * a < b  =>  mapping(f, a) < mapping(f, b)
 * </pre>
 *
 * が成り立たなければならない。
 *
 * <p>{@link #applyByRank(long, long, Object)} はさらに、作用後も
 * 区間外との境界を含めて列全体のソート順が保たれる場合にのみ実行できる。
 *
 * <p>{@link #meldDestructive(MeldableLazyOrderStatisticMultiset)} は、
 * 2 つの値域が重なっていてもよい sorted multiset union を行う。
 *
 * <p>Treap の優先度は乱択される。
 *
 * @param <K> 値の型
 * @param <F> lazy action の型
 */
public class MeldableLazyOrderStatisticMultiset<K, F> {

    /**
     * 値の順序と lazy action を定義する。
     */
    public interface Strategy<K, F> {

        /**
         * 値を比較する。
         *
         * @return a < b なら負、a = b なら 0、a > b なら正
         */
        int compare(K a, K b);

        /**
         * 値に action を作用させる。
         *
         * <p>入力値自体を破壊せず、作用後の値を返すこと。
         */
        K mapping(F action, K value);

        /**
         * action を合成する。
         *
         * <p>{@code composition(f, g)} は
         * 「先に g、後に f」を作用させる action を返す。
         */
        F composition(F f, F g);
    }

    /**
     * 値と重複度。
     */
    public record Entry<K>(K value, long multiplicity) {
    }

    private static final class Node<K, F> {
        K key;

        long multiplicity;
        long weight;
        int distinctSize;

        long priority;

        F lazy;
        boolean hasLazy;

        Node<K, F> left;
        Node<K, F> right;

        Node(K key, long multiplicity, long priority) {
            this.key = key;
            this.multiplicity = multiplicity;
            this.weight = multiplicity;
            this.distinctSize = 1;
            this.priority = priority;
        }
    }

    private static final class Split<K, F> {
        Node<K, F> left;
        Node<K, F> right;

        Split(Node<K, F> left, Node<K, F> right) {
            this.left = left;
            this.right = right;
        }
    }

    private static final class Split3<K, F> {
        Node<K, F> less;
        Node<K, F> equal;
        Node<K, F> greater;

        Split3(Node<K, F> less, Node<K, F> equal, Node<K, F> greater) {
            this.less = less;
            this.equal = equal;
            this.greater = greater;
        }
    }

    private final Strategy<K, F> strategy;

    private Node<K, F> root;

    private long rngState =
            0x9E3779B97F4A7C15L ^ System.nanoTime();

    public MeldableLazyOrderStatisticMultiset(Strategy<K, F> strategy) {
        this.strategy = strategy;
    }

    /**
     * multiplicity を展開した要素数。
     *
     * @return 要素数
     * @complexity O(1)
     */
    public long size() {
        return weight(root);
    }

    /**
     * 異なる値の個数。
     *
     * @return distinct key 数
     * @complexity O(1)
     */
    public int distinctSize() {
        return distinctSize(root);
    }

    public boolean isEmpty() {
        return root == null;
    }

    public void clear() {
        root = null;
    }

    /**
     * value を 1 個追加する。
     *
     * @complexity expected O(log D)
     */
    public void add(K value) {
        add(value, 1);
    }

    /**
     * value を multiplicity 個追加する。
     *
     * @param value 値
     * @param multiplicity 追加個数
     * @complexity expected O(log D)
     */
    public void add(K value, long multiplicity) {
        if (multiplicity < 0) {
            throw new IllegalArgumentException(
                    "multiplicity must be non-negative");
        }
        if (multiplicity == 0) {
            return;
        }

        Split3<K, F> s = split3(root, value);

        if (s.equal == null) {
            s.equal = newNode(value, multiplicity);
        } else {
            s.equal.multiplicity += multiplicity;
            recalc(s.equal);
        }

        root = mergeStrict(
                mergeStrict(s.less, s.equal),
                s.greater);
    }

    /**
     * value の重複度を返す。
     *
     * @complexity expected O(log D)
     */
    public long count(K value) {
        Node<K, F> t = root;

        while (t != null) {
            push(t);

            int c = compare(value, t.key);

            if (c == 0) {
                return t.multiplicity;
            }
            t = c < 0 ? t.left : t.right;
        }

        return 0;
    }

    /**
     * multiplicity を展開したソート済み列の k 番目を返す。
     *
     * @param k 0-indexed rank
     * @complexity expected O(log D)
     */
    public K getByRank(long k) {
        long n = size();

        if (k < 0 || k >= n) {
            throw new IndexOutOfBoundsException(
                    "k = " + k + ", size = " + n);
        }

        Node<K, F> t = root;

        while (true) {
            push(t);

            long left = weight(t.left);

            if (k < left) {
                t = t.left;
            } else if (k < left + t.multiplicity) {
                return t.key;
            } else {
                k -= left + t.multiplicity;
                t = t.right;
            }
        }
    }

    /**
     * 全要素に action を作用させる。
     *
     * <p>action が順序保存なのでソート順は自動的に保たれる。
     *
     * @complexity O(1)
     */
    public void applyAll(F action) {
        allApply(root, action);
    }

    /**
     * multiplicity 展開後の順位区間 [l,r) に action を作用させる。
     *
     * <p>作用後も集合全体が strategy の順序でソート済みであることを要求する。
     * この条件は実行前に境界値だけを用いて検証する。
     *
     * @param l 左順位
     * @param r 右順位
     * @param action 作用
     * @complexity expected O(log D)
     */
    public void applyByRank(long l, long r, F action) {
        long n = size();

        if (l < 0 || l > r || r > n) {
            throw new IndexOutOfBoundsException(
                    "[" + l + ", " + r + "), size = " + n);
        }

        if (l == r) {
            return;
        }

        /*
         * action は区間内部では狭義順序を保存するので、
         * チェックすべきなのは左右境界だけ。
         */
        K first = getByRank(l);
        K last = getByRank(r - 1);

        K mappedFirst = strategy.mapping(action, first);
        K mappedLast = strategy.mapping(action, last);

        if (l > 0) {
            K left = getByRank(l - 1);
            if (compare(left, mappedFirst) > 0) {
                throw new IllegalArgumentException(
                        "action breaks ordering at left boundary");
            }
        }

        if (r < n) {
            K right = getByRank(r);
            if (compare(mappedLast, right) > 0) {
                throw new IllegalArgumentException(
                        "action breaks ordering at right boundary");
            }
        }

        Split<K, F> a = splitByRank(root, l);
        Split<K, F> b = splitByRank(a.right, r - l);

        allApply(b.left, action);

        root = concatOrdered(
                concatOrdered(a.left, b.left),
                b.right);
    }

    /**
     * other の全要素を this に破壊的に統合する。
     *
     * <p>値域が重なっていてもよい。
     * 同じ値は multiplicity を加算して 1 ノードにまとめる。
     *
     * <p>2 つの multiset は同じ Strategy インスタンスを
     * 使用していなければならない。
     *
     * @complexity
     * expected O(m log(n / m + 1)),
     * where m <= n are the distinct-key counts
     */
    public void meldDestructive(
            MeldableLazyOrderStatisticMultiset<K, F> other) {

        if (this == other) {
            throw new IllegalArgumentException("cannot meld itself");
        }

        if (this.strategy != other.strategy) {
            throw new IllegalArgumentException(
                    "both multisets must share the same Strategy instance");
        }

        root = union(root, other.root);
        other.root = null;
    }

    /**
     * comparator 順の (value, multiplicity) を返す。
     *
     * @complexity O(D)
     */
    public List<Entry<K>> entries() {
        List<Entry<K>> ret = new ArrayList<>();
        collect(root, ret);
        return ret;
    }

    // ============================================================
    // Treap internals
    // ============================================================

    private Node<K, F> newNode(K key, long multiplicity) {
        return new Node<>(key, multiplicity, nextPriority());
    }

    private int compare(K a, K b) {
        return strategy.compare(a, b);
    }

    private static <K, F> long weight(Node<K, F> t) {
        return t == null ? 0 : t.weight;
    }

    private static <K, F> int distinctSize(Node<K, F> t) {
        return t == null ? 0 : t.distinctSize;
    }

    private void recalc(Node<K, F> t) {
        if (t == null) {
            return;
        }

        t.weight =
                weight(t.left)
                + t.multiplicity
                + weight(t.right);

        t.distinctSize =
                1
                + distinctSize(t.left)
                + distinctSize(t.right);
    }

    private void allApply(Node<K, F> t, F action) {
        if (t == null) {
            return;
        }

        t.key = strategy.mapping(action, t.key);

        if (t.hasLazy) {
            t.lazy = strategy.composition(action, t.lazy);
        } else {
            t.lazy = action;
            t.hasLazy = true;
        }
    }

    private void push(Node<K, F> t) {
        if (t == null || !t.hasLazy) {
            return;
        }

        allApply(t.left, t.lazy);
        allApply(t.right, t.lazy);

        t.lazy = null;
        t.hasLazy = false;
    }

    /**
     * a の全 key < b の全 key を仮定して結合する。
     */
    private Node<K, F> mergeStrict(
            Node<K, F> a,
            Node<K, F> b) {

        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        if (a.priority > b.priority) {
            push(a);
            a.right = mergeStrict(a.right, b);
            recalc(a);
            return a;
        } else {
            push(b);
            b.left = mergeStrict(a, b.left);
            recalc(b);
            return b;
        }
    }

    /**
     * a の全 key <= b の全 key を仮定して結合する。
     *
     * <p>境界値が等しい場合は 1 ノードにまとめる。
     */
    private Node<K, F> concatOrdered(
            Node<K, F> a,
            Node<K, F> b) {

        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        K ak = lastKey(a);
        K bk = firstKey(b);

        int c = compare(ak, bk);

        if (c > 0) {
            throw new IllegalStateException(
                    "concat order invariant broken");
        }

        if (c < 0) {
            return mergeStrict(a, b);
        }

        Split3<K, F> sa = split3(a, ak);
        Split3<K, F> sb = split3(b, bk);

        sa.equal.multiplicity += sb.equal.multiplicity;
        recalc(sa.equal);

        return mergeStrict(
                mergeStrict(sa.less, sa.equal),
                sb.greater);
    }

    /**
     * key < x と key >= x に分ける。
     */
    private Split<K, F> splitLower(
            Node<K, F> t,
            K x) {

        if (t == null) {
            return new Split<>(null, null);
        }

        push(t);

        if (compare(t.key, x) < 0) {
            Split<K, F> s = splitLower(t.right, x);
            t.right = s.left;
            recalc(t);
            s.left = t;
            return s;
        } else {
            Split<K, F> s = splitLower(t.left, x);
            t.left = s.right;
            recalc(t);
            s.right = t;
            return s;
        }
    }

    /**
     * key <= x と key > x に分ける。
     */
    private Split<K, F> splitUpper(
            Node<K, F> t,
            K x) {

        if (t == null) {
            return new Split<>(null, null);
        }

        push(t);

        if (compare(t.key, x) <= 0) {
            Split<K, F> s = splitUpper(t.right, x);
            t.right = s.left;
            recalc(t);
            s.left = t;
            return s;
        } else {
            Split<K, F> s = splitUpper(t.left, x);
            t.left = s.right;
            recalc(t);
            s.right = t;
            return s;
        }
    }

    /**
     * key < x, key = x, key > x に分ける。
     */
    private Split3<K, F> split3(
            Node<K, F> t,
            K x) {

        Split<K, F> a = splitLower(t, x);
        Split<K, F> b = splitUpper(a.right, x);

        return new Split3<>(
                a.left,
                b.left,
                b.right);
    }

    /**
     * multiplicity 展開後の先頭 k 要素と残りに分ける。
     */
    private Split<K, F> splitByRank(
            Node<K, F> t,
            long k) {

        if (t == null) {
            return new Split<>(null, null);
        }

        long total = weight(t);

        if (k <= 0) {
            return new Split<>(null, t);
        }

        if (k >= total) {
            return new Split<>(t, null);
        }

        push(t);

        long leftWeight = weight(t.left);

        if (k < leftWeight) {
            Split<K, F> s = splitByRank(t.left, k);
            t.left = s.right;
            recalc(t);
            return new Split<>(s.left, t);
        }

        long through = leftWeight + t.multiplicity;

        if (k > through) {
            Split<K, F> s =
                    splitByRank(t.right, k - through);

            t.right = s.left;
            recalc(t);

            return new Split<>(t, s.right);
        }

        if (k == leftWeight) {
            Node<K, F> left = t.left;
            t.left = null;
            recalc(t);

            return new Split<>(left, t);
        }

        if (k == through) {
            Node<K, F> right = t.right;
            t.right = null;
            recalc(t);

            return new Split<>(t, right);
        }

        /*
         * multiplicity の途中で切る。
         */
        long leftMultiplicity = k - leftWeight;
        long rightMultiplicity =
                t.multiplicity - leftMultiplicity;

        Node<K, F> l =
                newNode(t.key, leftMultiplicity);

        Node<K, F> r =
                newNode(t.key, rightMultiplicity);

        Node<K, F> leftTree =
                mergeStrict(t.left, l);

        Node<K, F> rightTree =
                mergeStrict(r, t.right);

        return new Split<>(leftTree, rightTree);
    }

    /**
     * 2 つの arbitrary-overlap Treap の multiset union。
     */
    private Node<K, F> union(
            Node<K, F> a,
            Node<K, F> b) {

        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        push(a);
        push(b);

        if (a.priority < b.priority) {
            Node<K, F> tmp = a;
            a = b;
            b = tmp;
        }

        Split3<K, F> s = split3(b, a.key);

        a.left = union(a.left, s.less);
        a.right = union(a.right, s.greater);

        if (s.equal != null) {
            a.multiplicity += s.equal.multiplicity;
        }

        recalc(a);
        return a;
    }

    private K firstKey(Node<K, F> t) {
        push(t);

        while (t.left != null) {
            t = t.left;
            push(t);
        }

        return t.key;
    }

    private K lastKey(Node<K, F> t) {
        push(t);

        while (t.right != null) {
            t = t.right;
            push(t);
        }

        return t.key;
    }

    private void collect(
            Node<K, F> t,
            List<Entry<K>> out) {

        if (t == null) {
            return;
        }

        push(t);

        collect(t.left, out);
        out.add(new Entry<>(t.key, t.multiplicity));
        collect(t.right, out);
    }

    private long nextPriority() {
        long z = (rngState += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}