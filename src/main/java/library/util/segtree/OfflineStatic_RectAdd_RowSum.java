package library.util.segtree;

import java.util.ArrayList;
import java.util.Collections;

import library.util.ArrayUtils;
import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;
import library.util.seq.SortedArrays;

/**
 * 二次元平面上の静的な矩形加算を x 軸方向に走査し、水平区間上の総和を求める。
 *
 * <p>矩形更新後の値を f(x, y) とすると、{@link #sum(long, long, long)} は
 * sum(x, y0, y1) = integral_[y0,y1) f(x, y) dy を返す。
 * 座標と値を整数として扱うため、これは各整数区間 [y, y+1) の値の総和にも等しい。
 *
 * <p>使用手順は、全ての {@link #rectAdd(long, long, long, long, long)} を登録し、
 * {@link #build()} を 1 回呼び、その後 x が単調非減少となる順に問い合わせる。
 * long の算術オーバーフローは検出しない。
 */
public class OfflineStatic_RectAdd_RowSum {
    /** 矩形更新の x 軸イベント。各要素は [x, y0, y1, add]。 */
    private final ArrayList<long[]> updates;
    /** 座標圧縮の対象となる y 座標。 */
    private final ArrayList<Long> ys;
    /** ソート済みかつ重複を除いた y 座標。 */
    private long[] sortedY;
    /** y 座標ごとの値の差分 d を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree difference;
    /** y 座標ごとの d*y を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree weightedDifference;
    /** 平面全体に一様に加算された値。 */
    private long base;
    /** {@link #build()} が完了しているかを表す。 */
    private boolean built;
    /** 最後に問い合わせた x 座標。 */
    private long lastX = Long.MIN_VALUE;
    /** 次に反映する x 軸イベントの添字。 */
    private int pointer;

    /**
     * 空の更新集合を生成する。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     */
    public OfflineStatic_RectAdd_RowSum() {
        this(0);
    }

    /**
     * 矩形更新を {@code rectAddQuerySize} 回登録する容量をあらかじめ確保する。
     *
     * <p>計算量: O(rectAddQuerySize)
     *
     * 未テスト
     * @param rectAddQuerySize 登録予定の矩形更新数。rectAddQuerySize ≥ 0
     */
    public OfflineStatic_RectAdd_RowSum(int rectAddQuerySize) {
        if (rectAddQuerySize < 0) throw new IllegalArgumentException("rectAddQuerySize must be non-negative");
        updates = new ArrayList<>(2 * rectAddQuerySize);
        ys = new ArrayList<>(2 * rectAddQuerySize);
    }

    /**
     * 全ての (x, y) in [x0, x1) x [y0, y1) に {@code add} を加える更新を登録する。
     * 空矩形は状態を変更しない。
     *
     * <p>計算量: O(1)
     * <p>事前条件: build() より前に呼ぶ。
     *
     * 未テスト
     * @param x0 x 座標の下端
     * @param y0 y 座標の下端
     * @param x1 x 座標の上端
     * @param y1 y 座標の上端
     * @param add 加算値
     */
    public void rectAdd(long x0, long y0, long x1, long y1, long add) {
        if (x0 < x1 && y0 < y1) {
            updates.add(new long[] {x0, y0, y1, add});
            updates.add(new long[] {x1, y0, y1, -add});
            registerY(y0);
            registerY(y1);
        }
    }

    /**
     * 全ての (x, y) に {@code add} を加える。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param add 加算値
     */
    public void addAll(long add) {
        base += add;
    }

    /**
     * {@code y} を座標圧縮の対象に加える。
     *
     * <p>計算量: O(1)
     * <p>事前条件: build() より前に呼ぶ。
     *
     * 未テスト
     * @param y 登録する y 座標
     */
    public void registerY(long y) {
        ys.add(y);
    }

    /**
     * 登録済みの更新から問い合わせ用の内部構造を構築する。
     * N を矩形更新数、Y を登録された y 座標数とする。
     *
     * <p>計算量: O(N log N + Y log Y)
     * <p>事後条件: build() より前に登録された更新を sum() で問い合わせられる。
     *
     * 未テスト
     */
    public void build() {
        Collections.sort(updates, (a, b) -> Long.compare(a[0], b[0]));
        sortedY = new long[ys.size()];
        for (int i = 0; i < ys.size(); i++) sortedY[i] = ys.get(i);
        sortedY = ArrayUtils.sortq(sortedY);
        difference = new LongAbelianGroupBinaryIndexedTree(sortedY.length, LongAddAbelianGroupStrategy.STRATEGY);
        weightedDifference = new LongAbelianGroupBinaryIndexedTree(sortedY.length, LongAddAbelianGroupStrategy.STRATEGY);
        built = true;
    }

    /**
     * sum(x, y0, y1) = integral_[y0,y1) f(x, y) dy を返す。
     *
     * <p>計算量: 償却 O((U + 1) log Y)。U はこの呼び出しで新たに反映するイベント数、
     * Y は相異なる登録済み y 座標数。
     * <p>事前条件: build() が呼ばれており、y0 ≤ y1、x ≥ 直前の問い合わせの x。
     * <p>副作用: イベントの x ≤ 現在の問い合わせの x である更新を内部構造へ反映する。
     *
     * 未テスト
     * @param x 問い合わせる x 座標
     * @param y0 y 座標の下端
     * @param y1 y 座標の上端
     * @return 水平区間 [y0, y1) 上の総和
     * @throws AssertionError build() 前、y0 > y1、または x < 直前の問い合わせの x の場合
     */
    public long sum(long x, long y0, long y1) {
        if (!built) throw new AssertionError("build() must be called before sum()");
        if (y0 > y1) throw new AssertionError("y0 must not exceed y1");
        if (lastX > x) throw new AssertionError("x must be non-decreasing");
        lastX = x;
        applyUpdatesThrough(x);
        return prefixSum(y1) - prefixSum(y0) + base * (y1 - y0);
    }

    /**
     * x 座標 ≤ {@code x} である未反映イベントを全て反映する。
     *
     * <p>計算量: O(U log Y)。U は反映するイベント数、Y は相異なる登録済み y 座標数。
     *
     * 未テスト
     * @param x 反映するイベントの x 座標上限
     */
    private void applyUpdatesThrough(long x) {
        while (pointer < updates.size() && updates.get(pointer)[0] <= x) {
            long[] update = updates.get(pointer++);
            addDifference(update[1], update[3]);
            addDifference(update[2], -update[3]);
        }
    }

    /**
     * y 座標 {@code y} に値の差分 {@code delta} を加える。
     *
     * <p>計算量: O(log Y)
     *
     * 未テスト
     * @param y 差分を置く y 座標
     * @param delta 差分への加算値
     */
    private void addDifference(long y, long delta) {
        int index = SortedArrays.ceil(sortedY, y);
        difference.add(index, delta);
        weightedDifference.add(index, delta * y);
    }

    /**
     * integral_[-infinity,y) (f(x, t) - base) dt を返す。
     * 矩形更新の台は有界であるため、この積分値は有限である。
     *
     * <p>計算量: O(log Y)
     *
     * 未テスト
     * @param y 積分区間の上端
     * @return 矩形更新による値の座標 < y における累積和
     */
    private long prefixSum(long y) {
        int count = SortedArrays.floor(sortedY, y) + 1;
        long d = difference.prefixSum(count);
        long dy = weightedDifference.prefixSum(count);
        return d * y - dy;
    }
}
