package library.util.segtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import library.util.ArrayUtils;
import library.util.seq.SortedArrays;

/**
 * 二次元平面上の静的な矩形加算と矩形総和問い合わせをオフラインで処理する。
 *
 * <p>矩形更新後の値を f(x, y) とすると、問い合わせ
 * (x0, y0, x1, y1) の答えは
 * integral_[x0,x1) integral_[y0,y1) f(x, y) dy dx である。
 * 座標と値を整数として扱うため、これは各単位正方形上の値の総和にも等しい。
 * {@link #solve(List)} は引数で与えられた問い合わせを一括処理する。
 * {@link #build()} 後の {@link #prefixRectSum(long, long)} は、
 * x の単調非減少順に prefix rectangle の答えを逐次返す。
 * long の算術オーバーフローは検出しない。
 */
public class OfflineStatic_RectAdd_RectSum {
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
    /** 平面全体に一様に加算された値。 */
    private long base;
    /** prefix rectangle 問い合わせ用にソートした y 座標。 */
    private long[] prefixSortedY;
    /** prefix rectangle 問い合わせ用の二次元差分点。 */
    private ArrayList<long[]> prefixPoints;
    /** prefix rectangle 問い合わせで delta を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient00;
    /** prefix rectangle 問い合わせで delta*y を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient01;
    /** prefix rectangle 問い合わせで delta*x を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient10;
    /** prefix rectangle 問い合わせで delta*x*y を保持する Fenwick Tree。 */
    private LongAbelianGroupBinaryIndexedTree prefixCoefficient11;
    /** 最後に prefix rectangle を問い合わせた x 座標。 */
    private long prefixLastX;
    /** 次に反映する prefix rectangle 用差分点の添字。 */
    private int prefixPointIndex;
    /** prefix rectangle 問い合わせ用の build() が完了しているかを表す。 */
    private boolean prefixBuilt;
    /** build() 後に prefixRectSum() が一度でも呼ばれたかを表す。 */
    private boolean prefixQueried;

    /**
     * 空の更新集合を生成する。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     */
    public OfflineStatic_RectAdd_RectSum() {
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
    public OfflineStatic_RectAdd_RectSum(int rectAddQuerySize) {
        if (rectAddQuerySize < 0) throw new IllegalArgumentException("rectAddQuerySize must be non-negative");
        updates = new ArrayList<>(rectAddQuerySize);
        ys = new ArrayList<>(2 * rectAddQuerySize);
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
            updates.add(new long[] {x0, y0, x1, y1, add});
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
        base += add;
    }

    /**
     * 登録済みの矩形更新、平面全体への加算、および逐次 prefix rectangle の内部状態を破棄する。
     * 呼び出し後の状態はデフォルトコンストラクタによる生成直後と等しい。
     *
     * <p>計算量: O(N + Y)。N は登録済み矩形更新数、Y は登録済み y 座標数。
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
     * <p>計算量: O(N log N + Y log Y)。N は矩形更新数、Y は登録された y 座標数。
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
            prefixPoints.add(new long[] {update[0], update[1], update[4]});
            prefixPoints.add(new long[] {update[0], update[3], -update[4]});
            prefixPoints.add(new long[] {update[2], update[1], -update[4]});
            prefixPoints.add(new long[] {update[2], update[3], update[4]});
        }
        Collections.sort(prefixPoints, (a, b) -> Long.compare(a[0], b[0]));

        prefixCoefficient00 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient01 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient10 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient11 = SegTreeFactory.sum(prefixSortedY.length);
        prefixBuilt = true;
    }

    /**
     * 矩形和の包含排除に用いる二次元累積ポテンシャル P(x, y) を返す。
     * 任意の x0 ≤ x1, y0 ≤ y1 に対し、矩形和は
     * P(x1,y1) - P(x0,y1) - P(x1,y0) + P(x0,y0) である。
     * 呼び出しは x の単調非減少順に行う。
     *
     * <p>計算量: 償却 O((U + 1) log Y)。U はこの呼び出しで新たに反映する差分点数、
     * Y は相異なる登録済み y 座標数。
     * <p>事前条件: build() が呼ばれており、x ≥ 直前の問い合わせの x。
     *
     * 未テスト
     * @param x x 座標の上端
     * @param y y 座標の上端
     * @return P(x, y)
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
            prefixCoefficient00.add(yIndex, delta);
            prefixCoefficient01.add(yIndex, delta * point[1]);
            prefixCoefficient10.add(yIndex, delta * point[0]);
            prefixCoefficient11.add(yIndex, delta * point[0] * point[1]);
        }
        return prefixSum(x, y) + base * x * y;
    }

    /**
     * {@code queries} の全問い合わせの答えをリスト順に返す。
     * N を矩形更新数、Q を矩形総和問い合わせ数とする。
     *
     * <p>計算量: O((N + Q) log(N + Q))
     * <p>空の問い合わせ矩形の答えは 0。
     * <p>副作用: prefix rectangle 問い合わせ用の内部状態を破棄する。
     * solve() 後に prefixRectSum() を呼ぶには build() を再度呼ぶ必要がある。
     * 複数回呼んでも同じ配列内容を返す。
     *
     * 未テスト
     * @param queries 矩形総和問い合わせ。各矩形は x0 ≤ x1, y0 ≤ y1
     * @return {@code answer[i]} が {@code queries.get(i)} の答えである長さ Q の配列
     * @throws AssertionError prefixRectSum() の呼び出し後、または不正な上下端を持つ問い合わせが存在する場合
     */
    public long[] solve(List<Rectangle> queries) {
        if (prefixQueried) throw new AssertionError("solve() cannot be called after prefixRectSum()");
        prefixBuilt = false;
        prefixSortedY = new long[ys.size()];
        for (int i = 0; i < ys.size(); i++) prefixSortedY[i] = ys.get(i);
        prefixSortedY = ArrayUtils.sortq(prefixSortedY);

        ArrayList<long[]> points = new ArrayList<>(4 * updates.size());
        for (long[] update : updates) {
            points.add(new long[] {update[0], update[1], update[4]});
            points.add(new long[] {update[0], update[3], -update[4]});
            points.add(new long[] {update[2], update[1], -update[4]});
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

        prefixCoefficient00 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient01 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient10 = SegTreeFactory.sum(prefixSortedY.length);
        prefixCoefficient11 = SegTreeFactory.sum(prefixSortedY.length);
        long[] answer = new long[queries.size()];
        int pointIndex = 0;
        for (long[] event : events) {
            long x = event[0];
            while (pointIndex < points.size() && points.get(pointIndex)[0] < x) {
                long[] point = points.get(pointIndex++);
                int yIndex = SortedArrays.ceil(prefixSortedY, point[1]);
                long delta = point[2];
                prefixCoefficient00.add(yIndex, delta);
                prefixCoefficient01.add(yIndex, delta * point[1]);
                prefixCoefficient10.add(yIndex, delta * point[0]);
                prefixCoefficient11.add(yIndex, delta * point[0] * point[1]);
            }
            answer[(int) event[2]] += event[3] * prefixSum(event[0], event[1]);
        }
        for (int id = 0; id < queries.size(); id++) {
            Rectangle query = queries.get(id);
            answer[id] += base * (query.x1() - query.x0()) * (query.y1() - query.y0());
        }
        return answer;
    }

    /**
     * integral_[-infinity,x) integral_[-infinity,y) f(s, t) dt ds から
     * {@code base} の寄与を除いた値を返す。
     *
     * <p>計算量: O(log Y)。Y は相異なる登録済み y 座標数。
     *
     * 未テスト
     * @param x x 座標の上端
     * @param y y 座標の上端
     * @return 二次元累積和
     */
    private long prefixSum(long x, long y) {
        int count = SortedArrays.ceil(prefixSortedY, y);
        long c00 = prefixCoefficient00.prefixSum(count);
        long c01 = prefixCoefficient01.prefixSum(count);
        long c10 = prefixCoefficient10.prefixSum(count);
        long c11 = prefixCoefficient11.prefixSum(count);
        return x * y * c00 - x * c01 - y * c10 + c11;
    }
}
