package library.util.segtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import library.util.ArrayUtils;
import library.util.Zn;
import library.util.seq.SortedArrays;

/**
 * 二次元平面上の静的な矩形加算と矩形総和問い合わせを法 {@code mod} で処理する。
 * 乗算では {@code mod} 未満の 2 数の積が long の範囲内にあることを仮定する。
 */
public class OfflineStatic_RectAdd_RectModSum {
    /**
     * 半開矩形 [x0, x1) x [y0, y1) を表す。
     *
     * @param x0 x 座標の下端
     * @param y0 y 座標の下端
     * @param x1 x 座標の上端
     * @param y1 y 座標の上端
     */
    public record Rectangle(long x0, long y0, long x1, long y1) {}

    /** 矩形更新。各要素は [x0, y0, x1, y1, add]。 */
    private final ArrayList<long[]> updates;
    /** 座標圧縮の対象となる y 座標。 */
    private final ArrayList<Long> ys;
    /** 剰余環 Z/modZ。 */
    private final Zn zn;
    /** 平面全体への加算値。 */
    private long base;
    /** prefix rectangle 問い合わせ用にソートした y 座標。 */
    private long[] prefixSortedY;
    /** prefix rectangle 問い合わせ用の二次元差分点。 */
    private ArrayList<long[]> prefixPoints;
    /** delta を保持する法付き Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient00;
    /** delta*y を保持する法付き Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient01;
    /** delta*x を保持する法付き Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient10;
    /** delta*x*y を保持する法付き Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient11;
    /** 最後に prefix rectangle を問い合わせた x 座標。 */
    private long prefixLastX;
    /** 次に反映する prefix rectangle 用差分点の添字。 */
    private int prefixPointIndex;
    /** build() が完了しているかを表す。 */
    private boolean prefixBuilt;
    /** build() 後に prefixRectSum() が呼ばれたかを表す。 */
    private boolean prefixQueried;

    /**
     * 空の更新集合を生成する。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param mod 法。mod > 0
     */
    public OfflineStatic_RectAdd_RectModSum(long mod) {
        this(0, mod);
    }

    /**
     * 矩形更新を {@code rectAddQuerySize} 回登録する容量を確保する。
     *
     * <p>計算量: O(rectAddQuerySize)
     *
     * 未テスト
     * @param rectAddQuerySize 登録予定の矩形更新数。rectAddQuerySize ≥ 0
     * @param mod 法。mod > 0
     */
    public OfflineStatic_RectAdd_RectModSum(int rectAddQuerySize, long mod) {
        if (rectAddQuerySize < 0) throw new IllegalArgumentException("rectAddQuerySize must be non-negative");
        if (mod <= 0) throw new IllegalArgumentException("mod must be positive");
        updates = new ArrayList<>(rectAddQuerySize);
        ys = new ArrayList<>(2 * rectAddQuerySize);
        zn = new Zn(mod);
    }

    /**
     * 全ての (x, y) in [x0, x1) x [y0, y1) に {@code add} を加える更新を登録する。
     * 空矩形は状態を変更しない。
     *
     * <p>計算量: O(1)
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
            updates.add(new long[] {x0, y0, x1, y1, zn.reduce(add)});
            ys.add(y0);
            ys.add(y1);
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
        base = zn.add(base, add);
    }

    /**
     * 全ての更新と構築済みの内部状態を破棄する。
     *
     * <p>計算量: O(N + Y)。N は矩形更新数、Y は登録済み y 座標数。
     *
     * 未テスト
     */
    public void clear() {
        updates.clear();
        ys.clear();
        base = 0;
        prefixSortedY = null;
        prefixPoints = null;
        prefixCoefficient00 = null;
        prefixCoefficient01 = null;
        prefixCoefficient10 = null;
        prefixCoefficient11 = null;
        prefixLastX = Long.MIN_VALUE;
        prefixPointIndex = 0;
        prefixBuilt = false;
        prefixQueried = false;
    }

    /**
     * 逐次 prefix rectangle 問い合わせ用の内部構造を構築する。
     *
     * <p>計算量: O(N log N + Y log Y)。N は矩形更新数、Y は登録済み y 座標数。
     *
     * 未テスト
     */
    public void build() {
        prefixLastX = Long.MIN_VALUE;
        prefixPointIndex = 0;
        prefixQueried = false;
        prefixSortedY = new long[ys.size()];
        for (int i = 0; i < ys.size(); i++) prefixSortedY[i] = ys.get(i);
        prefixSortedY = ArrayUtils.sortq(prefixSortedY);

        prefixPoints = new ArrayList<>(4 * updates.size());
        for (long[] update : updates) {
            long inverse = zn.neg(update[4]);
            prefixPoints.add(new long[] {update[0], update[1], update[4]});
            prefixPoints.add(new long[] {update[0], update[3], inverse});
            prefixPoints.add(new long[] {update[2], update[1], inverse});
            prefixPoints.add(new long[] {update[2], update[3], update[4]});
        }
        Collections.sort(prefixPoints, (a, b) -> Long.compare(a[0], b[0]));

        prefixCoefficient00 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient01 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient10 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient11 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixBuilt = true;
    }

    /**
     * 矩形包含排除に用いる二次元累積ポテンシャル P(x, y) mod mod を返す。
     * 呼び出しは x の単調非減少順に行う。
     *
     * <p>計算量: 償却 O((U + 1) log Y)。U は新たに反映する差分点数。
     * <p>事前条件: build() が呼ばれており、x ≥ 直前の問い合わせの x。
     *
     * 未テスト
     * @param x x 座標
     * @param y y 座標
     * @return P(x, y) mod mod
     * @throws AssertionError build() 前、または x が減少した場合
     */
    public long prefixRectSum(long x, long y) {
        if (!prefixBuilt) throw new AssertionError("build() must be called before prefixRectSum()");
        if (x < prefixLastX) throw new AssertionError("x must be non-decreasing");
        prefixLastX = x;
        prefixQueried = true;

        while (prefixPointIndex < prefixPoints.size() && prefixPoints.get(prefixPointIndex)[0] < x) {
            long[] point = prefixPoints.get(prefixPointIndex++);
            int yIndex = SortedArrays.ceil(prefixSortedY, point[1]);
            long delta = point[2];
            long px = zn.reduce(point[0]);
            long py = zn.reduce(point[1]);
            prefixCoefficient00.add(yIndex, delta);
            prefixCoefficient01.add(yIndex, zn.mul(delta, py));
            prefixCoefficient10.add(yIndex, zn.mul(delta, px));
            prefixCoefficient11.add(yIndex, zn.mul(delta, px, py));
        }
        return zn.add(prefixSum(x, y), zn.mul(base, zn.reduce(x), zn.reduce(y)));
    }

    /**
     * {@code queries} の矩形和をリスト順に法 {@code mod} で返す。
     *
     * <p>計算量: O((N + Q) log(N + Q))。N は矩形更新数、Q は問い合わせ数。
     * <p>副作用: prefix rectangle 問い合わせ用の内部状態を破棄する。
     * <p>事前条件: prefixRectSum() の呼び出し前。
     *
     * 未テスト
     * @param queries 矩形総和問い合わせ。各矩形は x0 ≤ x1, y0 ≤ y1
     * @return {@code answer[i]} が {@code queries.get(i)} の矩形和 mod mod
     * @throws AssertionError prefixRectSum() の呼び出し後、または不正な矩形が存在する場合
     */
    public long[] solve(List<Rectangle> queries) {
        if (prefixQueried) throw new AssertionError("solve() cannot be called after prefixRectSum()");
        prefixBuilt = false;
        prefixSortedY = new long[ys.size()];
        for (int i = 0; i < ys.size(); i++) prefixSortedY[i] = ys.get(i);
        prefixSortedY = ArrayUtils.sortq(prefixSortedY);

        ArrayList<long[]> points = new ArrayList<>(4 * updates.size());
        for (long[] update : updates) {
            long inverse = zn.neg(update[4]);
            points.add(new long[] {update[0], update[1], update[4]});
            points.add(new long[] {update[0], update[3], inverse});
            points.add(new long[] {update[2], update[1], inverse});
            points.add(new long[] {update[2], update[3], update[4]});
        }
        Collections.sort(points, (a, b) -> Long.compare(a[0], b[0]));

        ArrayList<long[]> events = new ArrayList<>(4 * queries.size());
        for (int id = 0; id < queries.size(); id++) {
            Rectangle query = queries.get(id);
            if (query.x0() > query.x1() || query.y0() > query.y1()) {
                throw new AssertionError("rectangle bounds must be non-decreasing");
            }
            events.add(new long[] {query.x1(), query.y1(), id, 1});
            events.add(new long[] {query.x0(), query.y1(), id, -1});
            events.add(new long[] {query.x1(), query.y0(), id, -1});
            events.add(new long[] {query.x0(), query.y0(), id, 1});
        }
        Collections.sort(events, (a, b) -> Long.compare(a[0], b[0]));

        prefixCoefficient00 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient01 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient10 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        prefixCoefficient11 = SegTreeFactory.modSum(prefixSortedY.length, zn.modulus());
        long[] answer = new long[queries.size()];
        int pointIndex = 0;
        for (long[] event : events) {
            while (pointIndex < points.size() && points.get(pointIndex)[0] < event[0]) {
                long[] point = points.get(pointIndex++);
                int yIndex = SortedArrays.ceil(prefixSortedY, point[1]);
                long delta = point[2];
                long px = zn.reduce(point[0]);
                long py = zn.reduce(point[1]);
                prefixCoefficient00.add(yIndex, delta);
                prefixCoefficient01.add(yIndex, zn.mul(delta, py));
                prefixCoefficient10.add(yIndex, zn.mul(delta, px));
                prefixCoefficient11.add(yIndex, zn.mul(delta, px, py));
            }
            int id = (int) event[2];
            long value = prefixSum(event[0], event[1]);
            answer[id] = event[3] > 0 ? zn.add(answer[id], value) : zn.sub(answer[id], value);
        }
        for (int id = 0; id < queries.size(); id++) {
            Rectangle query = queries.get(id);
            long width = zn.sub(zn.reduce(query.x1()), zn.reduce(query.x0()));
            long height = zn.sub(zn.reduce(query.y1()), zn.reduce(query.y0()));
            answer[id] = zn.add(answer[id], zn.mul(base, width, height));
        }
        return answer;
    }

    /**
     * 矩形更新由来の二次元累積和を法 {@code mod} で返す。
     *
     * <p>計算量: O(log Y)
     *
     * 未テスト
     * @param x x 座標
     * @param y y 座標
     * @return 矩形更新由来の累積和 mod mod
     */
    private long prefixSum(long x, long y) {
        int count = SortedArrays.ceil(prefixSortedY, y);
        long c00 = prefixCoefficient00.prefixSum(count);
        long c01 = prefixCoefficient01.prefixSum(count);
        long c10 = prefixCoefficient10.prefixSum(count);
        long c11 = prefixCoefficient11.prefixSum(count);
        long rx = zn.reduce(x);
        long ry = zn.reduce(y);
        long value = zn.sub(zn.mul(rx, ry, c00), zn.mul(rx, c01));
        value = zn.sub(value, zn.mul(ry, c10));
        return zn.add(value, c11);
    }
}
