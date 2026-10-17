package library.util.collections;

import java.util.Comparator;
import java.util.function.LongFunction;

import library.util.algebra.instance.MonoidElement;
import library.util.algebra.strategy.monoid.MonoidStrategy;

/**
 * 整数の多重集合 S に対する動的 linear parking を管理する。
 *
 * <p>S を昇順に s_1 <= ... <= s_n と並べ、</p>
 *
 * <pre>
 * a_1 = s_1
 * a_i = max(a_(i-1) + 1, s_i)
 * </pre>
 *
 * <p>で定まる parked positions a_i と displacement</p>
 *
 * <pre>
 * d_i = a_i - s_i >= 0
 * </pre>
 *
 * <p>を動的に管理する。</p>
 *
 * <p>displacement に対する fold は、range shift d_i <- d_i + c に閉じる
 * acted monoid を指定することで任意に差し替えられる。</p>
 *
 * <p>計算量: 各 add/remove expected O(log N)、displacementProd() は O(1)。</p>
 */
public class DynamicParkingMultiset<T> {

    /**
     * Parking 状態を key 座標順に集約するモノイド。
     *
     * <p>Treap に key x が存在することと x が occupied であることは同値。</p>
     */
    public static record Data(
            boolean empty,
            long l,
            long r,
            long count,
            long balanceSum,
            long minPrefix,
            long occupiedPrefix,
            boolean fullyOccupied,
            long occupiedSum)
            implements MonoidElement<Data> {

        public static final Data IDENTITY =
                new Data(
                        true,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        true,
                        0);

        public static final MonoidStrategy<Data> STRATEGY =
                new MonoidStrategy<>() {
                    @Override
                    public Data identity() {
                        return IDENTITY;
                    }

                    @Override
                    public Data mul(Data a, Data b) {
                        if (a.empty)
                            return b;
                        if (b.empty)
                            return a;

                        boolean adjacent = a.r == b.l;
                        long occupiedPrefix =
                                a.fullyOccupied && adjacent
                                        ? a.occupiedPrefix + b.occupiedPrefix
                                        : a.occupiedPrefix;

                        return new Data(
                                false,
                                a.l,
                                b.r,
                                a.count + b.count,
                                a.balanceSum + b.balanceSum,
                                Math.min(
                                        a.minPrefix,
                                        a.balanceSum + b.minPrefix),
                                occupiedPrefix,
                                a.fullyOccupied && b.fullyOccupied && adjacent,
                                a.occupiedSum + b.occupiedSum);
                    }
                };

        @Override
        public MonoidStrategy<Data> parent() {
            return STRATEGY;
        }

        @Override
        public Data self() {
            return this;
        }
    }

    /** 同じ preference の要素を insertion order で区別する内部 key。 */
    private static record PreferenceKey(long preference, long id) {
        private static final Comparator<PreferenceKey> COMPARATOR =
                (a, b) -> {
                    int c = Long.compare(a.preference, b.preference);
                    return c != 0 ? c : Long.compare(a.id, b.id);
                };

        /** preference == x の全要素より後ろ、preference > x の全要素より前。 */
        static PreferenceKey after(long x) {
            return new PreferenceKey(x, Long.MAX_VALUE);
        }

        /** preference == x の全要素より前。 */
        static PreferenceKey before(long x) {
            return new PreferenceKey(x, Long.MIN_VALUE);
        }
    }

    private final FoldTreapMap<Data> tree =
            new FoldTreapMap<>(
                    Data.STRATEGY::mul,
                    Data.IDENTITY);

    private final LazyFoldTreapMap<PreferenceKey, T, Long> displacementTree;
    private final LongFunction<T> displacementLift;

    private long nextId = 0;

    /**
     * displacement fold の各演算を定義する戦略。
     *
     * @param <T> モノイド要素の型
     */
    public interface Strategy<T> {
        /**
         * displacement aggregate のモノイド積。
         *
         * @param a 1つ目の要素
         * @param b 2つ目の要素
         * @return 積
         * @complexity O(1)
         */
        // 未テスト
        T op(T a, T b);

        /**
         * displacement aggregate の単位元。
         *
         * @return 単位元
         * @complexity O(1)
         */
        // 未テスト
        T identity();

        /**
         * 1要素の displacement d を aggregate に持ち上げる。
         *
         * @param d displacement
         * @return 持ち上げられた要素
         * @complexity O(1)
         */
        // 未テスト
        T lift(long d);

        /**
         * 長さ size の aggregate の全 displacement を delta だけ平行移動した値を返す。
         *
         * @param delta 平行移動の量
         * @param aggregate 元の集約値
         * @param size 部分木のサイズ
         * @return 平行移動後の集約値
         * @complexity O(1)
         */
        // 未テスト
        T shift(long delta, T aggregate, int size);
    }

    /**
     * Strategy を指定して構築する。
     *
     * @param strategy 各種演算を定義する戦略
     * @complexity O(1)
     */
    // 未テスト
    public DynamicParkingMultiset(Strategy<T> strategy) {
        this.displacementLift = strategy::lift;
        this.displacementTree =
                new LazyFoldTreapMap<>(
                        PreferenceKey.COMPARATOR,
                        strategy::op,
                        strategy.identity(),
                        Long::sum,
                        0L,
                        strategy::shift);
    }

    /**
     * マルチセットを空にする。
     * O(1)
     * // 未テスト
     */
    public void clear() {
        tree.clear();
        displacementTree.clear();
        nextId = 0;
    }

    /** occupied な座標 x 一点に対応する Data。count は preference x の個数。 */
    private static Data singleton(long x, long count) {
        if (count < 0)
            throw new AssertionError();

        long r = Math.addExact(x, 1);
        long balance = count - 1;

        return new Data(
                false,
                x,
                r,
                count,
                balance,
                Math.min(0, balance),
                1,
                true,
                x);
    }

    private Data getPoint(long x) {
        return tree.getOrDefault(x, null);
    }

    public long count(long x) {
        Data d = getPoint(x);
        return d == null ? 0 : d.count;
    }

    public boolean isOccupied(long x) {
        return getPoint(x) != null;
    }

    /** displacement d_i の全体 fold。 */
    public T displacementFold() {
        return displacementTree.allProd();
    }

    /**
     * preference x を1個追加する。
     *
     * <p>既存 displacement のうち preference が x < s < y のものを +1 し、
     * 新しい displacement Q(x) + [x is occupied] を preference x の末尾に挿入する。</p>
     *
     * @return 新たに occupied になった位置 y
     */
    public long add(long x) {
        if (x == Long.MAX_VALUE)
            throw new IllegalArgumentException("Long.MAX_VALUE is not supported as a preference");
        if (nextId == Long.MAX_VALUE)
            throw new ArithmeticException("too many insertions");

        Data prefix = tree.foldLessEqual(x);
        long q = prefix.balanceSum;
        boolean occupiedX = isOccupied(x);
        long y = firstFreeAtLeast(x);
        if (y == Long.MAX_VALUE)
            throw new ArithmeticException("parked position overflow");

        if (x < y) {
            displacementTree.apply(
                    PreferenceKey.after(x),
                    PreferenceKey.before(y),
                    1L);
        }

        long newDisplacement = q + (occupiedX ? 1 : 0);
        PreferenceKey newKey = new PreferenceKey(x, nextId++);
        displacementTree.insert(
                newKey,
                displacementLift.apply(newDisplacement));

        Data dx = getPoint(x);
        long countX = dx == null ? 0 : dx.count;
        tree.put(x, singleton(x, Math.addExact(countX, 1)));

        if (x != y) {
            if (getPoint(y) != null)
                throw new AssertionError();
            tree.put(y, singleton(y, 0));
        }

        return y;
    }

    /**
     * preference x を1個削除する。
     *
     * <p>同じ preference のうち最後の要素を削除する。この要素の displacement は Q(x)。
     * preference が x < s < y の残りの displacement を -1 する。</p>
     *
     * @return 新たに free になった位置 y
     */
    public long remove(long x) {
        Data dx = getPoint(x);
        if (dx == null || dx.count == 0)
            throw new IllegalArgumentException("x is not contained: " + x);

        long q = tree.foldLessEqual(x).balanceSum;

        long y;
        if (q == 0) {
            y = x;
        } else {
            Long found =
                    tree.firstPrefixFailure(
                            x,
                            false,
                            d -> q + d.minPrefix > 0);
            if (found == null)
                throw new AssertionError();
            y = found;
        }

        PreferenceKey removeKey =
                displacementTree.lowerKey(PreferenceKey.after(x));
        if (removeKey == null || removeKey.preference != x)
            throw new AssertionError();
        displacementTree.remove(removeKey);

        if (x < y) {
            displacementTree.apply(
                    PreferenceKey.after(x),
                    PreferenceKey.before(y),
                    -1L);
        }

        if (q == 0) {
            if (dx.count != 1)
                throw new AssertionError();
            tree.remove(x);
            return x;
        }

        tree.put(x, singleton(x, dx.count - 1));

        Data dy = getPoint(y);
        if (dy == null || dy.count != 0)
            throw new AssertionError();
        tree.remove(y);

        return y;
    }

    /** x 以上の最初の free position。 */
    private long firstFreeAtLeast(long x) {
        Data suffix = tree.foldGreaterEqual(x);
        if (suffix.empty || suffix.l > x)
            return x;
        return Math.addExact(x, suffix.occupiedPrefix);
    }

    public long size() {
        return tree.allFold().count;
    }

    public long parkedSum() {
        return tree.allFold().occupiedSum;
    }

    public boolean isEmpty() {
        return size() == 0;
    }
}
