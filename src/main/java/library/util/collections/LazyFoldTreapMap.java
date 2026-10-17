package library.util.collections;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.function.BinaryOperator;

/**
 * key 順のモノイド積と key 区間への lazy action を管理する Treap Map。
 *
 * <p>各 key は高々1個だけ保持する。区間はすべて半開区間 {@code [l, r)}。</p>
 *
 * <p>作用 {@code mapping(f, x, size)} は、長さ {@code size} の列を表す集約値
 * {@code x} の全要素に action {@code f} を作用させた後の集約値を返す。</p>
 *
 * <p>{@code composition(f, g)} は「先に {@code g}、後に {@code f}」を作用させる
 * 合成 action を返す。</p>
 *
 * <p>計算量: 各操作 expected O(log N)。</p>
 *
 * @param <K> キーの型
 * @param <T> モノイド要素の型
 * @param <F> 作用作用素の型
 */
public class LazyFoldTreapMap<K, T, F> {

    /**
     * 作用を適用するための関数型インターフェース。
     *
     * @param <F> 作用作用素の型
     * @param <T> モノイド要素の型
     */
    @FunctionalInterface
    public interface Mapping<F, T> {
        /**
         * 作用を適用します。
         *
         * @param action 作用作用素
         * @param aggregate 現在の集約値
         * @param size 部分木のサイズ
         * @return 作用適用後の集約値
         */
        T apply(F action, T aggregate, int size);
    }

    private static final class Node<K, T, F> {
        K key;
        T val;
        T prod;
        F lazy;
        boolean hasLazy;
        int priority;
        int size = 1;
        Node<K, T, F> left;
        Node<K, T, F> right;

        Node(K key, T val, F actionIdentity, int priority) {
            this.key = key;
            this.val = val;
            this.prod = val;
            this.lazy = actionIdentity;
            this.priority = priority;
        }
    }

    private static final class Split<K, T, F> {
        Node<K, T, F> left;
        Node<K, T, F> right;

        Split(Node<K, T, F> left, Node<K, T, F> right) {
            this.left = left;
            this.right = right;
        }
    }

    /** キーの比較器 */
    private final Comparator<? super K> comparator;
    /** 二項演算 */
    private final BinaryOperator<T> op;
    /** モノイドの単位元 */
    private final T identity;
    /** 作用作用素の合成二項演算 */
    private final BinaryOperator<F> composition;
    /** 作用作用素の単位元 */
    private final F actionIdentity;
    /** 作用を適用する写像 */
    private final Mapping<F, T> mapping;

    /** Treap の根ノード */
    private Node<K, T, F> root;
    /** 優先度決定用の乱数生成状態 */
    private long rngState = 0x9E3779B97F4A7C15L ^ System.nanoTime();

    /**
     * LazyFoldTreapMap を構築します。
     *
     * @param comparator キーの比較器
     * @param op 二項演算
     * @param identity 単位元
     * @param composition 合成演算
     * @param actionIdentity 作用の単位元
     * @param mapping 作用の適用
     * @complexity O(1)
     */
    // 未テスト
    public LazyFoldTreapMap(
            Comparator<? super K> comparator,
            BinaryOperator<T> op,
            T identity,
            BinaryOperator<F> composition,
            F actionIdentity,
            Mapping<F, T> mapping) {
        this.comparator = comparator;
        this.op = op;
        this.identity = identity;
        this.composition = composition;
        this.actionIdentity = actionIdentity;
        this.mapping = mapping;
    }

    /**
     * 要素数を返します。
     *
     * @return 要素数
     * @complexity O(1)
     */
    // 未テスト
    public int size() {
        return size(root);
    }

    /**
     * マップが空であるかどうかを判定します。
     *
     * @return 空なら true
     * @complexity O(1)
     */
    // 未テスト
    public boolean isEmpty() {
        return root == null;
    }

    /**
     * マップを空にします。
     *
     * @complexity O(1)
     */
    // 未テスト
    public void clear() {
        root = null;
    }

    /**
     * key の値を value に置き換える。存在しなければ挿入する。
     *
     * @param key キー
     * @param value 設定する値
     * @complexity expected O(log N)
     */
    // 未テスト
    public void set(K key, T value) {
        root = set(root, key, value, false);
    }

    /**
     * key を新規挿入する。既に存在する場合は例外。
     *
     * @param key キー
     * @param value 挿入する値
     * @throws IllegalArgumentException key が既に存在する場合
     * @complexity expected O(log N)
     */
    // 未テスト
    public void insert(K key, T value) {
        root = set(root, key, value, true);
    }

    /**
     * key を削除する。存在しない場合は何もしない。
     *
     * @param key キー
     * @complexity expected O(log N)
     */
    // 未テスト
    public void remove(K key) {
        root = remove(root, key);
    }

    /**
     * 指定されたキーがマップに含まれているかどうかを判定します。
     *
     * @param key キー
     * @return 含まれていれば true
     * @complexity expected O(log N)
     */
    // 未テスト
    public boolean containsKey(K key) {
        return getNode(key) != null;
    }

    /**
     * 指定されたキーに対応する値を取得します。
     *
     * @param key キー
     * @return キーに対応する値
     * @throws NoSuchElementException キーがマップに含まれていない場合
     * @complexity expected O(log N)
     */
    // 未テスト
    public T get(K key) {
        Node<K, T, F> t = getNode(key);
        if (t == null)
            throw new NoSuchElementException("key is not contained: " + key);
        return t.val;
    }

    /**
     * 指定されたキーに対応する値を取得します。存在しない場合はデフォルト値を返します。
     *
     * @param key キー
     * @param defaultValue デフォルト値
     * @return キーに対応する値、またはデフォルト値
     * @complexity expected O(log N)
     */
    // 未テスト
    public T getOrDefault(K key, T defaultValue) {
        Node<K, T, F> t = getNode(key);
        return t == null ? defaultValue : t.val;
    }

    /**
     * 全 key の値を key 昇順に fold する。
     *
     * @return モノイドの総積
     * @complexity O(1)
     */
    // 未テスト
    public T allProd() {
        return prod(root);
    }

    /**
     * {@code l <= key < r} の値を key 昇順に fold する。
     *
     * @param l キーの下限（含む）
     * @param r キーの上限（含まない）
     * @return 区間のモノイド積
     * @complexity expected O(log N)
     */
    // 未テスト
    public T prod(K l, K r) {
        if (compare(l, r) >= 0)
            return identity;

        Split<K, T, F> p = split(root, l);
        Split<K, T, F> q = split(p.right, r);
        T ans = prod(q.left);
        root = merge(p.left, merge(q.left, q.right));
        return ans;
    }

    /**
     * {@code l <= key < r} の全要素に action を作用させる。
     *
     * @param l キーの下限（含む）
     * @param r キーの上限（含まない）
     * @param action 作用作用素
     * @complexity expected O(log N)
     */
    // 未テスト
    public void apply(K l, K r, F action) {
        if (compare(l, r) >= 0)
            return;

        Split<K, T, F> p = split(root, l);
        Split<K, T, F> q = split(p.right, r);
        allApply(q.left, action);
        root = merge(p.left, merge(q.left, q.right));
    }

    /**
     * key より真に小さい最大 key。存在しなければ null。
     *
     * @param key キー
     * @return key 未満の最大キー、または null
     * @complexity expected O(log N)
     */
    // 未テスト
    public K lowerKey(K key) {
        Node<K, T, F> t = root;
        K ans = null;
        while (t != null) {
            push(t);
            if (compare(t.key, key) < 0) {
                ans = t.key;
                t = t.right;
            } else {
                t = t.left;
            }
        }
        return ans;
    }

    /**
     * key 以下の最大 key。存在しなければ null。
     *
     * @param key キー
     * @return key 以下の最大キー、または null
     * @complexity expected O(log N)
     */
    // 未テスト
    public K floorKey(K key) {
        Node<K, T, F> t = root;
        K ans = null;
        while (t != null) {
            push(t);
            if (compare(t.key, key) <= 0) {
                ans = t.key;
                t = t.right;
            } else {
                t = t.left;
            }
        }
        return ans;
    }

    /**
     * key より真に大きい最小 key。存在しなければ null。
     *
     * @param key キー
     * @return key より大きい最小キー、または null
     * @complexity expected O(log N)
     */
    // 未テスト
    public K higherKey(K key) {
        Node<K, T, F> t = root;
        K ans = null;
        while (t != null) {
            push(t);
            if (compare(t.key, key) > 0) {
                ans = t.key;
                t = t.left;
            } else {
                t = t.right;
            }
        }
        return ans;
    }

    /**
     * key 以上の最小 key。存在しなければ null。
     *
     * @param key キー
     * @return key 以上の最小キー、または null
     * @complexity expected O(log N)
     */
    // 未テスト
    public K ceilKey(K key) {
        Node<K, T, F> t = root;
        K ans = null;
        while (t != null) {
            push(t);
            if (compare(t.key, key) >= 0) {
                ans = t.key;
                t = t.left;
            } else {
                t = t.right;
            }
        }
        return ans;
    }

    private Node<K, T, F> getNode(K key) {
        Node<K, T, F> t = root;
        while (t != null) {
            push(t);
            int c = compare(key, t.key);
            if (c == 0)
                return t;
            t = c < 0 ? t.left : t.right;
        }
        return null;
    }

    private Node<K, T, F> set(
            Node<K, T, F> t,
            K key,
            T value,
            boolean failIfExists) {
        if (t == null)
            return new Node<>(key, value, actionIdentity, nextPriority());

        push(t);
        int c = compare(key, t.key);

        if (c == 0) {
            if (failIfExists)
                throw new IllegalArgumentException("key is already contained: " + key);
            t.val = value;
            recalc(t);
            return t;
        }

        if (c < 0) {
            t.left = set(t.left, key, value, failIfExists);
            if (t.left.priority > t.priority) {
                push(t.left);
                t = rotateRight(t);
            }
        } else {
            t.right = set(t.right, key, value, failIfExists);
            if (t.right.priority > t.priority) {
                push(t.right);
                t = rotateLeft(t);
            }
        }

        recalc(t);
        return t;
    }

    private Node<K, T, F> remove(Node<K, T, F> t, K key) {
        if (t == null)
            return null;

        push(t);
        int c = compare(key, t.key);

        if (c < 0) {
            t.left = remove(t.left, key);
            recalc(t);
            return t;
        }
        if (c > 0) {
            t.right = remove(t.right, key);
            recalc(t);
            return t;
        }
        return merge(t.left, t.right);
    }

    /** key < splitKey と splitKey <= key に分ける。 */
    private Split<K, T, F> split(Node<K, T, F> t, K splitKey) {
        if (t == null)
            return new Split<>(null, null);

        push(t);

        if (compare(t.key, splitKey) < 0) {
            Split<K, T, F> p = split(t.right, splitKey);
            t.right = p.left;
            recalc(t);
            p.left = t;
            return p;
        } else {
            Split<K, T, F> p = split(t.left, splitKey);
            t.left = p.right;
            recalc(t);
            p.right = t;
            return p;
        }
    }

    /** a の全 key < b の全 key を仮定して結合する。 */
    private Node<K, T, F> merge(Node<K, T, F> a, Node<K, T, F> b) {
        if (a == null)
            return b;
        if (b == null)
            return a;

        if (a.priority > b.priority) {
            push(a);
            a.right = merge(a.right, b);
            recalc(a);
            return a;
        } else {
            push(b);
            b.left = merge(a, b.left);
            recalc(b);
            return b;
        }
    }

    private Node<K, T, F> rotateRight(Node<K, T, F> t) {
        Node<K, T, F> s = t.left;
        t.left = s.right;
        s.right = t;
        recalc(t);
        recalc(s);
        return s;
    }

    private Node<K, T, F> rotateLeft(Node<K, T, F> t) {
        Node<K, T, F> s = t.right;
        t.right = s.left;
        s.left = t;
        recalc(t);
        recalc(s);
        return s;
    }

    private void allApply(Node<K, T, F> t, F action) {
        if (t == null)
            return;

        t.val = mapping.apply(action, t.val, 1);
        t.prod = mapping.apply(action, t.prod, t.size);

        if (t.hasLazy) {
            t.lazy = composition.apply(action, t.lazy);
        } else {
            t.lazy = action;
            t.hasLazy = true;
        }
    }

    private void push(Node<K, T, F> t) {
        if (t == null || !t.hasLazy)
            return;

        allApply(t.left, t.lazy);
        allApply(t.right, t.lazy);
        t.lazy = actionIdentity;
        t.hasLazy = false;
    }

    private void recalc(Node<K, T, F> t) {
        if (t == null)
            return;
        t.size = 1 + size(t.left) + size(t.right);
        t.prod = op.apply(op.apply(prod(t.left), t.val), prod(t.right));
    }

    private int compare(K a, K b) {
        return comparator.compare(a, b);
    }

    private int size(Node<K, T, F> t) {
        return t == null ? 0 : t.size;
    }

    private T prod(Node<K, T, F> t) {
        return t == null ? identity : t.prod;
    }

    private int nextPriority() {
        long x = rngState;
        x ^= x << 7;
        x ^= x >>> 9;
        x ^= x << 8;
        rngState = x;
        return (int) x;
    }
}
