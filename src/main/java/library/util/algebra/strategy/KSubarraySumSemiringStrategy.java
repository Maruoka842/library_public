package library.util.algebra.strategy;

import java.lang.reflect.Array;
import java.util.Arrays;
import library.util.algebra.strategy.monoid.MonoidStrategy;

/**
 * 半環上の区間から高々 k 個の互いに素な空でない部分配列 (subarray) を選び、
 * それらの部分配列内の要素の積の和を全選択肢について計算する Strategy。
 *
 * 時間計算量: マージ 1 回あたり O(k^2)
 * 空間計算量: 1 ノードあたり O(k)
 *
 * // 未テスト
 *
 * @param <S> 半環の要素の型
 */
public final class KSubarraySumSemiringStrategy<S> implements MonoidStrategy<KSubarraySumSemiringStrategy.Node<S>> {
	//https://atcoder.jp/contests/aalc001/submissions/80012245
    private final SemiRingStrategy<S> sr;
    private final int k;

    public KSubarraySumSemiringStrategy(SemiRingStrategy<S> sr, int k) {
        if (k < 0) {
            throw new IllegalArgumentException("k must be non-negative");
        }
        this.sr = sr;
        this.k = k;
    }

    public static final class Node<S> {
        public final boolean isIdentity;
        /** prefix[i]: 区間の接頭辞に接する部分配列で始まり、全体で i 個の部分配列を選ぶ時の積の和 */
        public final S[] prefix;
        /** suffix[i]: 区間の接尾辞に接する部分配列で終わり、全体で i 個の部分配列を選ぶ時の積の和 */
        public final S[] suffix;
        /** best[i]: 区間全体から i 個の部分配列を選ぶ時の積の和 */
        public final S[] best;
        /** both[i]: 区間の接頭辞から始まり接尾辞で終わる i 個の部分配列を選ぶ時の積の和 */
        public final S[] both;
        /** whole: 区間全体の要素の積 */
        public final S whole;

        public Node(boolean isIdentity, S[] prefix, S[] suffix, S[] best, S[] both, S whole) {
            this.isIdentity = isIdentity;
            this.prefix = prefix;
            this.suffix = suffix;
            this.best = best;
            this.both = both;
            this.whole = whole;
        }

        @Override
        public String toString() {
            if (isIdentity) {
                return "{identity}";
            }
            return "{prefix=" + Arrays.toString(prefix)
                    + ", suffix=" + Arrays.toString(suffix)
                    + ", best=" + Arrays.toString(best)
                    + ", both=" + Arrays.toString(both)
                    + ", whole=" + whole + "}";
        }
    }

    @SuppressWarnings("unchecked")
    private S[] createArray(Class<?> type, int size) {
        return (S[]) Array.newInstance(type, size);
    }

    /**
     * 1 要素区間を作る。
     */
    public Node<S> singleton(S value) {
        Class<?> type = value != null ? value.getClass() : Object.class;
        S[] prefix = createArray(type, k + 1);
        S[] suffix = createArray(type, k + 1);
        S[] best = createArray(type, k + 1);
        S[] both = createArray(type, k + 1);

        S zero = sr.zero();
        for (int i = 0; i <= k; i++) {
            prefix[i] = zero;
            suffix[i] = zero;
            best[i] = zero;
            both[i] = zero;
        }

        if (k >= 0) {
            best[0] = sr.one();
        }
        if (k >= 1) {
            prefix[1] = value;
            suffix[1] = value;
            best[1] = value;
            both[1] = value;
        }

        return new Node<>(false, prefix, suffix, best, both, value);
    }

    /**
     * 空区間ノード (単位元)。
     */
    public Node<S> identity() {
        S zero = sr.zero();
        S one = sr.one();
        Class<?> type = one != null ? one.getClass() : Object.class;

        S[] prefix = createArray(type, k + 1);
        S[] suffix = createArray(type, k + 1);
        S[] best = createArray(type, k + 1);
        S[] both = createArray(type, k + 1);

        for (int i = 0; i <= k; i++) {
            prefix[i] = zero;
            suffix[i] = zero;
            best[i] = zero;
            both[i] = zero;
        }

        if (k >= 0) {
            best[0] = one;
        }

        return new Node<>(true, prefix, suffix, best, both, one);
    }

    /**
     * 区間 [L..M) と [M..R) をマージする。
     */
    public Node<S> mul(Node<S> left, Node<S> right) {
        if (left == null || left.isIdentity) {
            return right;
        }
        if (right == null || right.isIdentity) {
            return left;
        }

        Class<?> type = left.whole != null ? left.whole.getClass() : (right.whole != null ? right.whole.getClass() : Object.class);
        S[] prefix = createArray(type, k + 1);
        S[] suffix = createArray(type, k + 1);
        S[] best = createArray(type, k + 1);
        S[] both = createArray(type, k + 1);

        S zero = sr.zero();
        for (int i = 0; i <= k; i++) {
            prefix[i] = zero;
            suffix[i] = zero;
            best[i] = zero;
            both[i] = zero;
        }

        S whole = sr.mul(left.whole, right.whole);

        for (int i = 0; i <= k; i++) {
            for (int j = 0; j <= k; j++) {
                // Option 1: Disjoint (中央で結合しない)
                if (i + j <= k) {
                    int c = i + j;
                    best[c] = sr.add(best[c], sr.mul(left.best[i], right.best[j]));
                    if (i >= 1) {
                        prefix[c] = sr.add(prefix[c], sr.mul(left.prefix[i], right.best[j]));
                    }
                    if (j >= 1) {
                        suffix[c] = sr.add(suffix[c], sr.mul(left.best[i], right.suffix[j]));
                    }
                    if (i >= 1 && j >= 1) {
                        both[c] = sr.add(both[c], sr.mul(left.prefix[i], right.suffix[j]));
                    }
                }

                // Option 2: Merged (中央 M の境界で left の末尾 subarray と right の先頭 subarray が結合する)
                if (i >= 1 && j >= 1 && i + j - 1 <= k) {
                    int c = i + j - 1;
                    best[c] = sr.add(best[c], sr.mul(left.suffix[i], right.prefix[j]));
                    prefix[c] = sr.add(prefix[c], sr.mul(left.both[i], right.prefix[j]));
                    suffix[c] = sr.add(suffix[c], sr.mul(left.suffix[i], right.both[j]));
                    both[c] = sr.add(both[c], sr.mul(left.both[i], right.both[j]));
                }
            }
        }

        return new Node<>(false, prefix, suffix, best, both, whole);
    }
}
