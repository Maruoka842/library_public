package library.util.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.SplittableRandom;

import library.util.ArrayUtils;
import library.util.LongFraction;
import library.util.seq.Permutation;

/**
 * 2色の点集合に対する ham-sandwich cut を求める。
 *
 * <p>
 * Lo-Matousek-Steiger 型の prune-and-search に基づく
 * 期待 O(n log n) 実装。
 * 内部の数値計算は {@code long} のみで行う。
 * </p>
 *
 * <p>
 * 入力点全体は一般位置にある、すなわち全ての点が相異なり、
 * 相異なる3点が一直線上にないことを仮定する。
 * また、内部で行われる全ての {@code long} 演算が
 * オーバーフローしないことを仮定する。
 * これらの前提条件は検査しない。
 * </p>
 */
public class HamSandwichCut {

    /**
     * NewInterval で許容する区間内交点数の分母 A。
     *
     * T 内の交点数が C(n,2) / INTERSECTION_DEN 以下になるまで
     * T を狭める。
     */
    private static final long INTERSECTION_DEN = 18;

    /**
     * FindTrapezoid で target level の上下に取る順位幅の分母 E。
     *
     * offset = floor(n / LEVEL_MARGIN_DEN)。
     */
    private static final int LEVEL_MARGIN_DEN = 6;

    private final SplittableRandom random;

    private int[] rank;

    public HamSandwichCut() {
        this(new SplittableRandom());
    }

    public HamSandwichCut(long seed) {
        this(new SplittableRandom(seed));
    }

    private HamSandwichCut(SplittableRandom random) {
        this.random = random;
    }

    /**
     * 2つの点集合を同時に二等分する直線を求める。
     *
     * <p>
     * 返される直線 {@code l} は、{@code p} と {@code q} のそれぞれについて、
     * {@code l} が定める2つの開半平面のいずれにも、その点集合の要素が
     * 半数を超えて含まれないという条件を満たす。
     * すなわち、{@code s} を {@code p} または {@code q} とすると、
     * {@code l} の各側に厳密に存在する {@code s} の点数はそれぞれ
     * {@code floor(s.length / 2)} 以下である。
     * 直線上にある点はどちらの開半平面にも属さない。
     * </p>
     *
     * <p>
     * 点数が偶数である点集合については、内部で任意の1点を除去してから
     * 問題を解く。偶数個の点から1点を除いて得られる奇数個の点集合を
     * 二等分する直線は、除去前の点集合も二等分するため、
     * 返される直線は元の入力配列全体に対して上記の条件を満たす。
     * </p>
     *
     * <p>
     * 内部では各点を直線へ双対化し、2つの指定 level の交点を
     * prune-and-search により探索する。
     * 同じ x 座標を持つ入力点が存在する場合は、
     * {@code x' = x + shear * y} という shear 変換を施し、
     * 双対直線に平行な組が生じない座標系で処理する。
     * 最終的には shear 前の元の座標系における {@link LongLine} を返す。
     * </p>
     *
     * <p>
     * 入力 {@code p} と {@code q} の和集合について、以下を前提とする。
     * </p>
     *
     * <ul>
     * <li>全ての点は相異なる。</li>
     * <li>相異なる3点が一直線上に存在しない。</li>
     * <li>
     * 座標変換、直線の評価、交点の比較、分数の比較など、
     * 内部で行われる全ての {@code long} 演算がオーバーフローしない。
     * </li>
     * </ul>
     *
     * <p>
     * これらの前提条件は検査しない。
     * {@code p}, {@code q} およびそれらが参照する {@link LongPoint} は変更しない。
     * 本アルゴリズムは乱択アルゴリズムであるため、
     * 同一入力に対して異なる実行で異なる二等分線を返すことがある。
     * </p>
     *
     * <p>
     * {@code n = p.length + q.length} とすると、
     * 期待計算量は O(n log n)、追加領域計算量は O(n)。
     * </p>
     *
     * @param p 第1の点集合。
     *          {@code null} でなく、各要素も {@code null} であってはならない
     * @param q 第2の点集合。
     *          {@code null} でなく、各要素も {@code null} であってはならない
     * @return {@code p} と {@code q} の両方を二等分する直線
     *
     * @throws NullPointerException
     *         {@code p}, {@code q}、またはいずれかの配列要素が
     *         {@code null} の場合
     * @throws IllegalArgumentException
     *         {@code p} または {@code q} が空の場合
     */
    public LongLine solve(LongPoint[] p, LongPoint[] q) {
        if (p.length == 0 || q.length == 0) {
            throw new IllegalArgumentException("point sets must be non-empty");
        }

        LongPoint[] all = new LongPoint[p.length + q.length];
        System.arraycopy(p, 0, all, 0, p.length);
        System.arraycopy(q, 0, all, p.length, q.length);

        long shear = chooseShear(all);

        rank = new int[all.length];

        DLine[] h1 = dualize(p, 0, shear);
        DLine[] h2 = dualize(q, p.length, shear);

        // 偶数個なら任意の1本を除いてよい。
        if ((h1.length & 1) == 0) {
            h1 = Arrays.copyOf(h1, h1.length - 1);
        }
        if ((h2.length & 1) == 0) {
            h2 = Arrays.copyOf(h2, h2.length - 1);
        }

        // 双対直線の全交点を含む有限区間を初期 T にする。
        long max = 0;
        for (DLine g : h1) {
            max = Math.max(max, Math.max(Math.abs(g.m), Math.abs(g.b)));
        }
        for (DLine g : h2) {
            max = Math.max(max, Math.max(Math.abs(g.m), Math.abs(g.b)));
        }

        long bound = 2 * max + 1;
        Interval t = new Interval(
                LongFraction.ofCoprime(-bound, 1),
                LongFraction.ofCoprime(bound, 1));

        int p1 = (h1.length + 1) / 2;
        int p2 = (h2.length + 1) / 2;

        if (h2.length > h1.length) {
            DLine[] tmp = h1;
            h1 = h2;
            h2 = tmp;

            int tp = p1;
            p1 = p2;
            p2 = tp;
        }

        while (choose2(h1.length) > INTERSECTION_DEN) {
            NewIntervalResult ni =
                    newInterval(h1, h2, p1, p2, t);

            if (ni.answer != null) {
                return ni.answer;
            }

            t = ni.interval;

            Trapezoid trap =
                    findTrapezoid(h1, p1, t, ni.order);

            DiscardResult dr =
                    discardLines(h1, p1, trap);

            h1 = dr.lines;
            p1 = dr.p;

            if (h2.length > h1.length) {
                DLine[] tmp = h1;
                h1 = h2;
                h2 = tmp;

                int tp = p1;
                p1 = p2;
                p2 = tp;
            }
        }

        return bruteForce(h1, h2, p1, p2, t);
    }

    /**
     * x' = x + shear * y として、全点の x' が異なるようにする。
     */
    private long chooseShear(LongPoint[] points) {
        HashSet<Long> xs = new HashSet<>();

        boolean distinctX = true;
        long max = 0;

        for (LongPoint p : points) {
            if (!xs.add(p.x)) {
                distinctX = false;
            }

            max = Math.max(
                    max,
                    Math.max(Math.abs(p.x), Math.abs(p.y)));
        }

        if (distinctX) {
            return 0;
        }

        /*
         * |x_i - x_j| <= 2 max なので、
         * |shear| > 2 max なら y_i != y_j のペアについて
         *
         * x_i + shear*y_i = x_j + shear*y_j
         *
         * は起こらない。
         */
        return 2 * max + 1;
    }

    private DLine[] dualize(
            LongPoint[] points,
            int idOffset,
            long shear) {

        DLine[] res = new DLine[points.length];

        for (int i = 0; i < points.length; i++) {
            LongPoint p = points[i];

            long x = p.x + shear * p.y;
            long y = p.y;

            // (x,y)^* : v = x*u - y
            res[i] = new DLine(
                    x,
                    -y,
                    idOffset + i,
                    p);
        }

        return res;
    }

    private NewIntervalResult newInterval(
            DLine[] g1,
            DLine[] g2,
            int p1,
            int p2,
            Interval t) {

        LevelComparison leftCmp =
                compareLevelsAt(g1, p1, g2, p2, t.left);

        LevelComparison rightCmp =
                compareLevelsAt(g1, p1, g2, p2, t.right);

        if (leftCmp.sign == 0) {
            return NewIntervalResult.answer(
                    new LongLine(
                            leftCmp.a.original,
                            leftCmp.b.original));
        }

        if (rightCmp.sign == 0) {
            return NewIntervalResult.answer(
                    new LongLine(
                            rightCmp.a.original,
                            rightCmp.b.original));
        }

        int signLeft = leftCmp.sign;

        if (signLeft == rightCmp.sign) {
            throw new IllegalStateException(
                    "odd-intersection invariant is broken");
        }

        while (true) {
            OrderInfo order = orderInfo(g1, t);

            long pairs = choose2(g1.length);

            /*
             * T 内の G1-G1 交点数を
             *
             * C(|G1|, 2) / 18
             *
             * 以下まで減らす。
             */
            if (order.intersections * INTERSECTION_DEN <= pairs) {
                return new NewIntervalResult(t, order);
            }

            /*
             * 現在は T 内に全ペアの 1/18 より多くの交点がある。
             * したがってランダムな2直線の交点が T 内に入る確率は
             * 1/18 より大きく、rejection sampling の期待試行回数は
             * 18 未満。
             */
            LongFraction pivot;

            while (true) {
                int i = random.nextInt(g1.length);

                int j = random.nextInt(g1.length - 1);
                if (j >= i) {
                    j++;
                }

                pivot = intersectionX(g1[i], g1[j]);

                if (t.contains(pivot)) {
                    break;
                }
            }

            LevelComparison midCmp =
                    compareLevelsAt(
                            g1, p1,
                            g2, p2,
                            pivot);

            if (midCmp.sign == 0) {
                return NewIntervalResult.answer(
                        new LongLine(
                                midCmp.a.original,
                                midCmp.b.original));
            }

            int signMid = midCmp.sign;

            if (signLeft != signMid) {
                t = new Interval(t.left, pivot);
            } else {
                t = new Interval(pivot, t.right);
                signLeft = signMid;
            }
        }
    }

    /**
     * T の左右端直後/直前での順序と、T 内の交点数を求める。
     */
    private OrderInfo orderInfo(
            DLine[] g,
            Interval t) {

        DLine[] left = g.clone();
        DLine[] right = g.clone();

        // T は開区間なので、それぞれ区間内部側の極限を使う。
        Arrays.sort(
                left,
                boundaryComparator(t.left, true));

        Arrays.sort(
                right,
                boundaryComparator(t.right, false));

        for (int i = 0; i < left.length; i++) {
            rank[left[i].id] = i;
        }

        int[] a = new int[g.length];

        for (int i = 0; i < right.length; i++) {
            a[i] = rank[right[i].id];
        }

        long inv = Permutation.inversion(a);

        return new OrderInfo(
                inv,
                left,
                right);
    }

    private Comparator<DLine> boundaryComparator(
            LongFraction x,
            boolean leftBoundary) {

        return (a, b) -> {
            int c = compareValue(a, b, x);

            if (c != 0) {
                return c;
            }

            /*
             * 開区間 T の内部側での順序を採用する。
             *
             * 左端:
             *     x + eps なので slope 昇順。
             *
             * 右端:
             *     x - eps なので slope 降順。
             */
            return leftBoundary
                    ? Long.compare(a.m, b.m)
                    : Long.compare(b.m, a.m);
        };
    }

    private LevelComparison compareLevelsAt(
            DLine[] g1,
            int p1,
            DLine[] g2,
            int p2,
            LongFraction x) {

        DLine a = kthAt(g1, p1 - 1, x);
        DLine b = kthAt(g2, p2 - 1, x);

        return new LevelComparison(
                compareValue(a, b, x),
                a,
                b);
    }

    /**
     * x における評価値で k 番目 (0-indexed) の直線を返す。
     *
     * 期待 O(n)。
     */
    private DLine kthAt(
            DLine[] g,
            int k,
            LongFraction x) {

        DLine[] a = g.clone();
        ArrayUtils.nthElement(a, k, (u, v) -> compareAtForSelection(u, v, x));
        return a[k];
    }

    private int compareAtForSelection(
            DLine a,
            DLine b,
            LongFraction x) {

        int c = compareValue(a, b, x);

        if (c != 0) {
            return c;
        }

        return Long.compare(a.m, b.m);
    }

    private Trapezoid findTrapezoid(
            DLine[] g,
            int p,
            Interval t,
            OrderInfo order) {

        /*
         * E = 6:
         *
         * target rank p の上下 floor(|G| / 6) を
         * 台形の上下境界に使う。
         */
        int offset =
                g.length / LEVEL_MARGIN_DEN;

        int lo = p - offset;
        int hi = p + offset;

        if (lo < 1 || hi > g.length) {
            throw new IllegalStateException(
                    "level rank is too close to the boundary");
        }

        return new Trapezoid(
                t,
                order.left[lo - 1],
                order.left[hi - 1],
                order.right[lo - 1],
                order.right[hi - 1]);
    }

    private DiscardResult discardLines(
            DLine[] g,
            int p,
            Trapezoid t) {

        ArrayList<DLine> keep =
                new ArrayList<>();

        int newP = p;

        for (DLine line : g) {
            /*
             * 左右端の両方で下辺より下なら、
             * 区間全体で台形より下にある。
             */
            boolean below =
                    compareValue(
                            line,
                            t.leftLow,
                            t.interval.left) < 0
                    &&
                    compareValue(
                            line,
                            t.rightLow,
                            t.interval.right) < 0;

            if (below) {
                /*
                 * target level より常に下の直線を1本消すので、
                 * target level の rank も1つ下がる。
                 */
                newP--;
                continue;
            }

            /*
             * 左右端の両方で上辺より上なら、
             * 区間全体で台形より上にある。
             */
            boolean above =
                    compareValue(
                            line,
                            t.leftHigh,
                            t.interval.left) > 0
                    &&
                    compareValue(
                            line,
                            t.rightHigh,
                            t.interval.right) > 0;

            if (above) {
                continue;
            }

            keep.add(line);
        }

        return new DiscardResult(
                keep.toArray(new DLine[0]),
                newP);
    }

    private LongLine bruteForce(
            DLine[] g1,
            DLine[] g2,
            int p1,
            int p2,
            Interval t) {

        for (DLine a : g1) {
            for (DLine b : g2) {
                LongFraction x =
                        intersectionX(a, b);

                if (!t.contains(x)) {
                    continue;
                }

                if (isPthLevel(g1, p1, a, x)
                        && isPthLevel(g2, p2, b, x)) {

                    /*
                     * 双対交点に対応する primal cut は
                     * この2点を通る。
                     *
                     * shear の逆変換後では、
                     * 元の2点を通る直線になる。
                     */
                    return new LongLine(
                            a.original,
                            b.original);
                }
            }
        }

        throw new IllegalStateException(
                "ham-sandwich point not found");
    }

    private boolean isPthLevel(
            DLine[] g,
            int p,
            DLine through,
            LongFraction x) {

        int below = 0;

        for (DLine h : g) {
            if (compareValue(
                    h,
                    through,
                    x) < 0) {

                below++;
            }
        }

        return below == p - 1;
    }

    /**
     * x における a(x) と b(x) の大小を返す。
     *
     * x.den > 0 を仮定する。
     */
    private int compareValue(
            DLine a,
            DLine b,
            LongFraction x) {

        long v =
                (a.m - b.m) * x.numerator()
                + (a.b - b.b) * x.denominator();

        return Long.compare(v, 0);
    }

    private LongFraction intersectionX(
            DLine a,
            DLine b) {

        long num =
                b.b - a.b;

        long den =
                a.m - b.m;

        if (den == 0) {
            throw new IllegalStateException(
                    "parallel dual lines");
        }

        return new LongFraction(num, den);
    }

    private static long choose2(long n) {
        return n * (n - 1) / 2;
    }

    private static class DLine {

        final long m;
        final long b;

        final int id;

        final LongPoint original;

        DLine(
                long m,
                long b,
                int id,
                LongPoint original) {

            this.m = m;
            this.b = b;
            this.id = id;
            this.original = original;
        }
    }

    private static class Interval {

        final LongFraction left;
        final LongFraction right;

        Interval(
                LongFraction left,
                LongFraction right) {

            if (left.fastCompareTo(right) >= 0) {
                throw new IllegalArgumentException(
                        "empty interval");
            }

            this.left = left;
            this.right = right;
        }

        boolean contains(LongFraction x) {
            return left.fastCompareTo(x) < 0
                    && x.fastCompareTo(right) < 0;
        }
    }

    private static class OrderInfo {

        final long intersections;

        final DLine[] left;
        final DLine[] right;

        OrderInfo(
                long intersections,
                DLine[] left,
                DLine[] right) {

            this.intersections = intersections;
            this.left = left;
            this.right = right;
        }
    }

    private static class NewIntervalResult {

        final Interval interval;
        final OrderInfo order;
        final LongLine answer;

        NewIntervalResult(
                Interval interval,
                OrderInfo order) {

            this.interval = interval;
            this.order = order;
            this.answer = null;
        }

        private NewIntervalResult(
                LongLine answer) {

            this.interval = null;
            this.order = null;
            this.answer = answer;
        }

        static NewIntervalResult answer(
                LongLine answer) {

            return new NewIntervalResult(answer);
        }
    }

    private static class LevelComparison {

        final int sign;

        final DLine a;
        final DLine b;

        LevelComparison(
                int sign,
                DLine a,
                DLine b) {

            this.sign = sign;
            this.a = a;
            this.b = b;
        }
    }

    private static class Trapezoid {

        final Interval interval;

        final DLine leftLow;
        final DLine leftHigh;

        final DLine rightLow;
        final DLine rightHigh;

        Trapezoid(
                Interval interval,
                DLine leftLow,
                DLine leftHigh,
                DLine rightLow,
                DLine rightHigh) {

            this.interval = interval;

            this.leftLow = leftLow;
            this.leftHigh = leftHigh;

            this.rightLow = rightLow;
            this.rightHigh = rightHigh;
        }
    }

    private static class DiscardResult {

        final DLine[] lines;
        final int p;

        DiscardResult(
                DLine[] lines,
                int p) {

            this.lines = lines;
            this.p = p;
        }
    }
}