package library.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

/**
 * SlopeTrickSuper: 区分線形下凸関数 f: R -> R U {+inf} を平衡二分木 (Treap) で管理するライブラリ。
 *
 * 関数 f は有限区間 [domainL, domainR] (-inf <= domainL <= domainR <= +inf) 上で定義され、
 * 定義域内では絶対標準形:
 * f(x) = baseIntercept + baseSlope * x + sum_(i) delta_i * max(x - x_i, 0)
 * (ただし domainL < x_i < domainR)
 * で一意に表現される。
 *
 * 各操作の計算量:
 * - N: 変化点の個数
 * - getAt(x), getMin(), minMinusLinear(p): O(log N)
 * - addConst(b), addLinear(a, b), translate(dx): O(1)
 * - addRelu(a, w), addIrelu(a, w), removeRelu(a, w), removeIrelu(a, w): O(log N)
 * - restrictDomain(l, r), clampSlope(lo, hi): O(log N)
 * - moveLeftCurve(w), moveRightCurve(w), convolveLinearSegment(...): O(log N)
 * - maxRightBreakpointsByWeight(...): O(log N + log W)
 * - maxRightBreakpointsByWeightSumLeq(...): O(log N)
 * - mergeDestructive(g), minPlusConvolveDestructive(g): O(N log N)
 */
public class SlopeTrickSuper {
    /** 正の無限大を表す定数 */
    public static final long INF = (1L << 60);

    /** 最小値および argmin 区間を表すレコード */
    public record QueryResult(long min, long lo, long hi) {
    }

    /** 変化点列の区間集約値を表すレコード */
    public record Aggregate(long sumX, long sumDelta, int count) {
    }

    /** Treap のノード構造 */
    private static class Node {
        long x;
        long delta;
        int priority;
        Node left, right;

        long sumDelta;
        long sumXDelta;
        int count;

        long lazyX;

        Node(long x, long delta, int priority) {
            this.x = x;
            this.delta = delta;
            this.priority = priority;
            this.sumDelta = delta;
            this.sumXDelta = x * delta;
            this.count = 1;
            this.lazyX = 0;
        }
    }

    /** 乱数生成器 */
    private final Random rng = new Random();

    /** 定義域の下限 */
    private long domainL;

    /** 定義域の上限 */
    private long domainR;

    /** 基本傾き a */
    private long baseSlope;

    /** 基本切片 b */
    private long baseIntercept;

    /** 変化点 (x_i, delta_i) を保持する Treap の根 (domainL < x_i < domainR) */
    private Node root;

    Node getRoot() {
        return root;
    }

    long getBaseSlope() {
        return baseSlope;
    }

    long getBaseIntercept() {
        return baseIntercept;
    }

    long getTotalWeight() {
        return getSumDelta(root);
    }

    long dualDomainLeft() {
        return domainL == -INF ? baseSlope : -INF;
    }

    long dualDomainRight() {
        return domainR == INF ? baseSlope + getSumDelta(root) : INF;
    }

    long sumDeltaLeq(long xKey) {
        if (root == null) return 0;
        Node[] sp = splitByX(root, xKey);
        long res = getSumDelta(sp[0]);
        root = merge(sp[0], sp[1]);
        return res;
    }

    long sumXDeltaLeq(long xKey) {
        if (root == null) return 0;
        Node[] sp = splitByX(root, xKey);
        long res = getSumXDelta(sp[0]);
        root = merge(sp[0], sp[1]);
        return res;
    }

    long sumDeltaGeq(long xKey) {
        if (root == null) return 0;
        Node[] sp = splitByX(root, xKey - 1);
        long res = getSumDelta(sp[1]);
        root = merge(sp[0], sp[1]);
        return res;
    }

    long sumXDeltaGeq(long xKey) {
        if (root == null) return 0;
        Node[] sp = splitByX(root, xKey - 1);
        long res = getSumXDelta(sp[1]);
        root = merge(sp[0], sp[1]);
        return res;
    }

    /**
     * 内部表現 invariants の検証を行う (テスト/デバッグ用)。
     */
    void verifyInvariants() {
        if (domainL > domainR) {
            return;
        }
        java.util.List<Node> nodes = new java.util.ArrayList<>();
        verifyNodeInvariants(root, nodes);
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n.x <= domainL || n.x >= domainR) {
                throw new AssertionError("Node x=" + n.x + " out of domain (" + domainL + ", " + domainR + ")");
            }
            if (i > 0 && nodes.get(i - 1).x >= nodes.get(i).x) {
                throw new AssertionError("Treap key order broken or duplicate x: " + nodes.get(i - 1).x + " >= " + nodes.get(i).x);
            }
        }
    }

    private static void verifyNodeInvariants(Node t, java.util.List<Node> nodes) {
        if (t == null) return;
        push(t);
        verifyNodeInvariants(t.left, nodes);

        if (t.delta <= 0) {
            throw new AssertionError("Node delta must be > 0, got " + t.delta + " at x=" + t.x);
        }
        nodes.add(t);

        long expSumD = t.delta + getSumDelta(t.left) + getSumDelta(t.right);
        if (t.sumDelta != expSumD) {
            throw new AssertionError("sumDelta invariant broken at x=" + t.x + ": " + t.sumDelta + " != " + expSumD);
        }

        long expSumXD = t.x * t.delta + getSumXDelta(t.left) + getSumXDelta(t.right);
        if (t.sumXDelta != expSumXD) {
            throw new AssertionError("sumXDelta invariant broken at x=" + t.x + ": " + t.sumXDelta + " != " + expSumXD);
        }

        int expCnt = 1 + getCount(t.left) + getCount(t.right);
        if (t.count != expCnt) {
            throw new AssertionError("count invariant broken at x=" + t.x + ": " + t.count + " != " + expCnt);
        }

        verifyNodeInvariants(t.right, nodes);
    }

    /**
     * f(x) = 0 (x in [-INF, INF]) で初期化する。
     * 計算量: O(1)
     */
    public SlopeTrickSuper() {
        clear();
    }

    /**
     * 関数を f(x) = 0 (x in [-INF, INF]) に初期化する。
     * 計算量: O(1)
     */
    public void clear() {
        this.domainL = -INF;
        this.domainR = INF;
        this.baseSlope = 0;
        this.baseIntercept = 0;
        this.root = null;
    }

    // ============================================================
    // Treap Helpers
    // ============================================================

    private static void applyLazy(Node t, long d) {
        if (t != null && d != 0) {
            t.x += d;
            t.sumXDelta += d * t.sumDelta;
            t.lazyX += d;
        }
    }

    private static void push(Node t) {
        if (t != null && t.lazyX != 0) {
            long d = t.lazyX;
            applyLazy(t.left, d);
            applyLazy(t.right, d);
            t.lazyX = 0;
        }
    }

    private static void update(Node t) {
        if (t != null) {
            push(t.left);
            push(t.right);
            t.count = 1 + getCount(t.left) + getCount(t.right);
            t.sumDelta = t.delta + getSumDelta(t.left) + getSumDelta(t.right);
            t.sumXDelta = t.x * t.delta + getSumXDelta(t.left) + getSumXDelta(t.right);
        }
    }

    private static long getSumXDelta(Node t) {
        return t == null ? 0 : t.sumXDelta;
    }

    private static long getSumDelta(Node t) {
        return t == null ? 0 : t.sumDelta;
    }

    private static int getCount(Node t) {
        return t == null ? 0 : t.count;
    }

    private static Node[] splitByX(Node t, long key) {
        if (t == null) return new Node[]{null, null};
        push(t);
        if (t.x <= key) {
            Node[] res = splitByX(t.right, key);
            t.right = res[0];
            update(t);
            return new Node[]{t, res[1]};
        } else {
            Node[] res = splitByX(t.left, key);
            t.left = res[1];
            update(t);
            return new Node[]{res[0], t};
        }
    }

    private Node[] splitByWeight(Node t, long w) {
        if (t == null || w <= 0) return new Node[]{null, t};
        long totalDelta = getSumDelta(t);
        if (w >= totalDelta) return new Node[]{t, null};

        push(t);
        long leftDelta = getSumDelta(t.left);
        if (w <= leftDelta) {
            Node[] res = splitByWeight(t.left, w);
            t.left = res[1];
            update(t);
            return new Node[]{res[0], t};
        } else if (w < leftDelta + t.delta) {
            long wLeft = w - leftDelta;
            long wRight = t.delta - wLeft;

            Node t1 = new Node(t.x, wLeft, rng.nextInt());
            Node t2 = new Node(t.x, wRight, rng.nextInt());

            t1.left = t.left;
            t2.right = t.right;

            update(t1);
            update(t2);

            return new Node[]{t1, t2};
        } else {
            Node[] res = splitByWeight(t.right, w - leftDelta - t.delta);
            t.right = res[0];
            update(t);
            return new Node[]{t, res[1]};
        }
    }

    private static Node merge(Node l, Node r) {
        if (l == null) return r;
        if (r == null) return l;
        push(l);
        push(r);
        if (l.priority > r.priority) {
            l.right = merge(l.right, r);
            update(l);
            return l;
        } else {
            r.left = merge(l, r.left);
            update(r);
            return r;
        }
    }

    private void rawInsertNode(long x, long delta) {
        if (delta <= 0) return;
        if (x <= domainL || x >= domainR) return;
        Node[] res = splitByX(root, x);
        Node[] resLeft = splitByX(res[0], x - 1);
        if (resLeft[1] != null) {
            resLeft[1].delta += delta;
            update(resLeft[1]);
            root = merge(merge(resLeft[0], resLeft[1]), res[1]);
        } else {
            Node newNode = new Node(x, delta, rng.nextInt());
            root = merge(merge(resLeft[0], newNode), res[1]);
        }
    }

    // ============================================================
    // Queries
    // ============================================================

    /**
     * 定義域の下限 domainL を取得する。
     * 計算量: O(1)
     *
     * @return domainL
     */
    public long domainLeft() {
        return domainL;
    }

    /**
     * 定義域の上限 domainR を取得する。
     * 計算量: O(1)
     *
     * @return domainR
     */
    public long domainRight() {
        return domainR;
    }

    /**
     * 指定された点 x における関数値 f(x) を取得する。
     * x が定義域外の場合は INF を返す。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return f(x)
     */
    public long getAt(long x) {
        if (x < domainL || x > domainR) {
            return INF;
        }
        Node[] res = splitByX(root, x);
        long sumD = getSumDelta(res[0]);
        long sumXD = getSumXDelta(res[0]);
        root = merge(res[0], res[1]);

        return baseIntercept + baseSlope * x + x * sumD - sumXD;
    }

    /**
     * 指定された点 x における左微分 f'_-(x) を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return 左傾き
     */
    public long getSlopeLeft(long x) {
        if (x < domainL) return -INF;
        if (x > domainR) return INF;
        Node[] res = splitByX(root, x - 1);
        long sumD = getSumDelta(res[0]);
        root = merge(res[0], res[1]);
        return baseSlope + sumD;
    }

    /**
     * 指定された点 x における右微分 f'_+(x) を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return 右傾き
     */
    public long getSlopeRight(long x) {
        if (x < domainL) return -INF;
        if (x > domainR) return INF;
        Node[] res = splitByX(root, x);
        long sumD = getSumDelta(res[0]);
        root = merge(res[0], res[1]);
        return baseSlope + sumD;
    }

    /**
     * 指定された点 x におけるサブグラディエント（劣微分）を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return サブグラディエント
     */
    public long getSubgradient(long x) {
        long sl = getSlopeLeft(x);
        long sr = getSlopeRight(x);
        if (sl <= 0 && sr >= 0) return 0;
        if (sr < 0) return sr;
        return sl;
    }

    /**
     * f(x) の最小値 min f(x) および最小値をとる区間 [lo, hi] を取得する。
     * 定義域が空の場合は min = INF, lo > hi を返す。
     * 計算量: O(log N)
     *
     * @return QueryResult(min, lo, hi)
     */
    public QueryResult getMin() {
        if (domainL > domainR) {
            return new QueryResult(INF, INF, -INF);
        }
        long slopeL = getSlopeRight(domainL);
        if (slopeL > 0) {
            if (domainL == -INF) {
                return new QueryResult(-INF, -INF, -INF);
            }
            return new QueryResult(getAt(domainL), domainL, domainL);
        }

        long slopeR = getSlopeLeft(domainR);
        if (slopeR < 0) {
            if (domainR == INF) {
                return new QueryResult(-INF, INF, INF);
            }
            return new QueryResult(getAt(domainR), domainR, domainR);
        }

        long totalW = getSumDelta(root);
        long targetW = -baseSlope;

        long loX = (targetW == 0)
                ? domainL
                : breakpointByWeight(targetW - 1);

        long hiX = (targetW == totalW)
                ? domainR
                : breakpointByWeight(targetW);

        long optLo = Math.max(domainL, Math.min(domainR, loX));
        long optHi = Math.max(domainL, Math.min(domainR, hiX));

        if (slopeL == 0 && targetW <= 0) {
            optLo = domainL;
        }
        if (slopeR == 0 && targetW >= totalW) {
            optHi = domainR;
        }

        return new QueryResult(getAt(optLo), optLo, optHi);
    }

    /**
     * min_x (f(x) - p * x) を計算する。
     * 計算量: O(log N)
     *
     * @param p 傾き
     * @return 最小値
     */
    public long minMinusLinear(long p) {
        QueryResult qr = argminMinusLinear(p);
        return qr.min();
    }

    /**
     * argmin_x (f(x) - p * x) の区間を取得する。
     * 計算量: O(log N)
     *
     * @param p 傾き
     * @return argmin 区間情報
     */
    public QueryResult argminMinusLinear(long p) {
        this.baseSlope -= p;
        QueryResult qr = getMin();
        this.baseSlope += p;
        return qr;
    }

    // ============================================================
    // Affine Operations
    // ============================================================

    /**
     * f(x) += b を行う。
     * 計算量: O(1)
     *
     * @param b 加算定数
     * @return this
     */
    public SlopeTrickSuper addConst(long b) {
        this.baseIntercept += b;
        return this;
    }

    /**
     * f(x) += a * x を行う。
     * 計算量: O(1)
     *
     * @param a 傾き
     * @return this
     */
    public SlopeTrickSuper addLinear(long a) {
        return addLinear(a, 0);
    }

    /**
     * f(x) += a * x + b を行う。
     * 計算量: O(1)
     *
     * @param a 傾き
     * @param b 切片
     * @return this
     */
    public SlopeTrickSuper addLinear(long a, long b) {
        this.baseSlope += a;
        this.baseIntercept += b;
        return this;
    }

    /**
     * f(x) <- f(x - dx) を行う。
     * 計算量: O(1)
     *
     * @param dx 平行移動量
     * @return this
     */
    public SlopeTrickSuper translate(long dx) {
        if (dx == 0) return this;
        this.baseIntercept -= this.baseSlope * dx;
        if (domainL > -INF) domainL += dx;
        if (domainR < INF) domainR += dx;
        applyLazy(root, dx);
        return this;
    }

    // ============================================================
    // Weighted ReLU Operations
    // ============================================================

    /**
     * f(x) += weight * max(x - a, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return this
     */
    public SlopeTrickSuper addRelu(long a, long weight) {
        if (weight <= 0) return this;
        if (a <= domainL) {
            return addLinear(weight, -weight * a);
        }
        if (a >= domainR) {
            return this;
        }

        rawInsertNode(a, weight);
        return this;
    }

    /**
     * f(x) += max(x - a, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @return this
     */
    public SlopeTrickSuper addRelu(long a) {
        return addRelu(a, 1L);
    }

    /**
     * f(x) += weight * max(a - x, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return this
     */
    public SlopeTrickSuper addIrelu(long a, long weight) {
        if (weight <= 0) return this;
        addRelu(a, weight);
        addLinear(-weight, weight * a);
        return this;
    }

    /**
     * f(x) += max(a - x, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @return this
     */
    public SlopeTrickSuper addIrelu(long a) {
        return addIrelu(a, 1L);
    }

    /**
     * f(x) += weight * |x - a| を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return this
     */
    public SlopeTrickSuper addAbs(long a, long weight) {
        if (weight <= 0) return this;
        addRelu(a, 2 * weight);
        addLinear(-weight, weight * a);
        return this;
    }

    /**
     * f(x) += |x - a| を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @return this
     */
    public SlopeTrickSuper addAbs(long a) {
        return addAbs(a, 1L);
    }

    /**
     * f(x) -= weight * max(x - a, 0) が実行可能か判定する。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return 実行可能な場合は true
     */
    public boolean canRemoveRelu(long a, long weight) {
        if (weight <= 0) return true;
        if (a <= domainL) return true;
        if (a >= domainR) return true;

        Node[] res = splitByX(root, a);
        Node[] resLeft = splitByX(res[0], a - 1);
        boolean ok = getSumDelta(resLeft[1]) >= weight;
        root = merge(merge(resLeft[0], resLeft[1]), res[1]);
        return ok;
    }

    /**
     * f(x) -= weight * max(x - a, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return this
     */
    public SlopeTrickSuper removeRelu(long a, long weight) {
        if (weight <= 0) return this;
        if (!canRemoveRelu(a, weight)) {
            throw new IllegalArgumentException("Cannot remove relu: insufficient weight or convex violation");
        }
        if (a <= domainL) {
            return addLinear(-weight, weight * a);
        }
        if (a >= domainR) {
            return this;
        }

        Node[] res = splitByX(root, a);
        Node[] resLeft = splitByX(res[0], a - 1);

        Node[] sub = splitByWeight(resLeft[1], weight);
        resLeft[1] = sub[1];

        root = merge(merge(resLeft[0], resLeft[1]), res[1]);
        return this;
    }

    /**
     * f(x) -= max(x - a, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @return this
     */
    public SlopeTrickSuper removeRelu(long a) {
        return removeRelu(a, 1L);
    }

    /**
     * f(x) -= weight * max(a - x, 0) が実行可能か判定する。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return 実行可能な場合は true
     */
    public boolean canRemoveIrelu(long a, long weight) {
        return canRemoveRelu(a, weight);
    }

    /**
     * f(x) -= weight * max(a - x, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @param weight 重み (>= 0)
     * @return this
     */
    public SlopeTrickSuper removeIrelu(long a, long weight) {
        if (weight <= 0) return this;
        removeRelu(a, weight);
        addLinear(weight, -weight * a);
        return this;
    }

    /**
     * f(x) -= max(a - x, 0) を行う。
     * 計算量: O(log N)
     *
     * @param a 変化点
     * @return this
     */
    public SlopeTrickSuper removeIrelu(long a) {
        return removeIrelu(a, 1L);
    }

    // ============================================================
    // Curve Movement & Segment Convolution
    // ============================================================

    /**
     * f(x) <- min_(0 <= y <= w) f(x + y) を行う。
     * 計算量: O(log N)
     *
     * @param w 移動幅 (>= 0)
     * @return this
     */
    public SlopeTrickSuper moveLeftCurve(long w) {
        if (w < 0) throw new IllegalArgumentException("w must be >= 0");
        return convolveLinearSegment(-w, 0, 0, 0);
    }

    /**
     * f(x) <- min_(0 <= y) f(x + y) を行う。
     * 計算量: O(log N)
     *
     * @return this
     */
    public SlopeTrickSuper moveLeftCurve() {
        return clampSlope(0, INF);
    }

    /**
     * f(x) <- min_(0 <= y <= w) f(x - y) を行う。
     * 計算量: O(log N)
     *
     * @param w 移動幅 (>= 0)
     * @return this
     */
    public SlopeTrickSuper moveRightCurve(long w) {
        if (w < 0) throw new IllegalArgumentException("w must be >= 0");
        return convolveLinearSegment(0, w, 0, 0);
    }

    /**
     * f(x) <- min_(0 <= y) f(x - y) を行う。
     * 計算量: O(log N)
     *
     * @return this
     */
    public SlopeTrickSuper moveRightCurve() {
        return clampSlope(-INF, 0);
    }

    /**
     * g(x) = min_(from <= d <= to) { f(x - d) + slope * d + intercept } を計算する。
     * 計算量: O(log N)
     *
     * @param from 範囲の下限
     * @param to 範囲の上限 (>= from)
     * @param slope 傾き
     * @param intercept 切片
     * @return this
     */
    public SlopeTrickSuper convolveLinearSegment(long from, long to, long slope, long intercept) {
        if (from > to) throw new IllegalArgumentException("from must be <= to");
        translate(from);
        addConst(slope * from + intercept);
        long len = to - from;
        if (len == 0 || domainL > domainR) return this;

        if (domainL == domainR) {
            long valL = getAt(domainL);
            baseSlope = slope;
            baseIntercept = valL - slope * domainL;
            if (domainR < INF) domainR += len;
            return this;
        }

        long slopeL = getSlopeRight(domainL);
        long slopeR = getSlopeLeft(domainR);

        if (domainL == domainR) {
            long valL = getAt(domainL);
            baseSlope = slope;
            baseIntercept = valL - slope * domainL;
            if (domainR < INF) domainR += len;
            return this;
        }

        if (slope <= slopeL) {
            if (domainL > -INF) {
                long valL = getAt(domainL);
                long oldL = domainL;
                applyLazy(root, len);
                long jump = slopeL - slope;
                baseSlope = slope;
                baseIntercept = valL - slope * oldL;
                if (domainR < INF) domainR += len;
                if (jump > 0) {
                    rawInsertNode(oldL + len, jump);
                }
            } else {
                applyLazy(root, len);
                baseIntercept += (slope - baseSlope) * len;
                if (domainR < INF) domainR += len;
            }
        } else if (slope >= slopeR) {
            if (domainR < INF) {
                long oldR = domainR;
                domainR += len;
                long jump = slope - slopeR;
                if (jump > 0) {
                    rawInsertNode(oldR, jump);
                }
            }
        } else {
            long targetW = slope - slopeL;
            Node[] res = splitByWeight(root, targetW);
            applyLazy(res[1], len);
            root = merge(res[0], res[1]);

            if (domainR < INF) domainR += len;
        }
        return this;
    }

    // ============================================================
    // Domain & Slope Restrictions
    // ============================================================

    /**
     * 定義域を [l, r] に制限する (x not in [l, r] => f(x) = +inf)。
     * 計算量: O(log N)
     *
     * @param l 新しい定義域の下限
     * @param r 新しい定義域の上限
     * @return this
     */
    public SlopeTrickSuper restrictDomain(long l, long r) {
        if (l > r) {
            clear();
            domainL = 1;
            domainR = 0;
            return this;
        }
        long newL = Math.max(domainL, l);
        long newR = Math.min(domainR, r);
        if (newL > newR) {
            clear();
            domainL = 1;
            domainR = 0;
            return this;
        }

        domainL = newL;
        domainR = newR;

        // x_i <= domainL のノードを baseSlope / baseIntercept に集約して捨てる
        if (domainL > -INF) {
            Node[] resL = splitByX(root, domainL);
            long sumD = getSumDelta(resL[0]);
            long sumXD = getSumXDelta(resL[0]);
            baseSlope += sumD;
            baseIntercept -= sumXD;
            root = resL[1];
        }

        // x_i >= domainR のノードを捨てる
        if (domainR < INF) {
            Node[] resR = splitByX(root, domainR - 1);
            root = resR[0];
        }

        return this;
    }

    /**
     * 関数の傾きを [lo, hi] の範囲に制限する。
     * 超過部分は対応する線形延長に置き換えられ、定義域壁は解除される。
     * 計算量: O(log N)
     *
     * @param lo 傾きの下限
     * @param hi 傾きの上限 (>= lo)
     * @return this
     */
    public SlopeTrickSuper clampSlope(long lo, long hi) {
        if (lo > hi) throw new IllegalArgumentException("lo must be <= hi");
        if (domainL > domainR) return this;

        long dualL = (domainL == -INF) ? baseSlope : -INF;
        long dualR = (domainR == INF) ? baseSlope + getSumDelta(root) : INF;

        if (hi < dualL || lo > dualR) {
            throw new IllegalArgumentException(
                    "clamped slope range has empty intersection with conjugate domain");
        }

        clampSlopeLower(lo);
        clampSlopeUpper(hi);

        return this;
    }

    private void clampSlopeLower(long lo) {
        if (lo == -INF) return;

        if (domainL == -INF && lo <= baseSlope) {
            return;
        }

        QueryResult qr = argminMinusLinear(lo);
        long x0 = qr.lo();

        long y0 = getAt(x0);
        long slopeRight = getSlopeRight(x0);

        Node[] sp = splitByX(root, x0);
        root = sp[1];

        domainL = -INF;
        baseSlope = lo;
        baseIntercept = y0 - lo * x0;

        long jump = slopeRight - lo;
        if (jump > 0) {
            rawInsertNode(x0, jump);
        }
    }

    private void clampSlopeUpper(long hi) {
        if (hi == INF) return;

        long slopeInfinity = baseSlope + getSumDelta(root);

        if (domainR == INF && hi >= slopeInfinity) {
            return;
        }

        QueryResult qr = argminMinusLinear(hi);
        long x1 = qr.hi();

        long y1 = getAt(x1);
        long slopeLeft = getSlopeLeft(x1);

        Node[] sp = splitByX(root, x1 - 1);
        root = sp[0];

        domainR = INF;

        if (slopeLeft > hi) {
            root = null;
            baseSlope = hi;
            baseIntercept = y1 - hi * x1;
        } else {
            long jump = hi - slopeLeft;
            if (jump > 0) {
                rawInsertNode(x1, jump);
            }
        }
    }

    // ============================================================
    // Breakpoint Order Statistics & Aggregates
    // ============================================================

    /**
     * 重み順で k 番目の変化点の x 座標を取得する。
     * 計算量: O(log N)
     *
     * @param k 重み順インデックス (0-indexed)
     * @return x 座標
     */
    public long breakpointByWeight(long k) {
        long totalW = getSumDelta(root);
        if (k < 0 || k >= totalW) {
            throw new IndexOutOfBoundsException("k out of bounds: " + k + " (total weight: " + totalW + ")");
        }

        Node t = root;
        while (t != null) {
            push(t);
            long leftWeight = getSumDelta(t.left);
            if (k < leftWeight) {
                t = t.left;
            } else if (k < leftWeight + t.delta) {
                return t.x;
            } else {
                k -= leftWeight + t.delta;
                t = t.right;
            }
        }
        throw new IndexOutOfBoundsException("k out of bounds: " + k);
    }

    /**
     * 重み順区間 [l, r) に含まれる変化点の x 座標の総和を計算する。
     *
     * <p>計算量: O(log N)</p>
     *
     * @param l 重み区間の下限 (0 <= l <= r <= total weight)
     * @param r 重み区間の上限 (0 <= l <= r <= total weight)
     * @return 変化点 x 座標の総和
     */
    // 未テスト
    public long sumBreakpointsByWeight(long l, long r) {
        long totalW = getSumDelta(root);
        if (l < 0 || l > r || r > totalW) {
            throw new IndexOutOfBoundsException(
                    "weight range out of bounds: [" + l + ", " + r
                    + "), total weight = " + totalW);
        }
        if (l == r) {
            return 0;
        }

        return prefixProdWeight(r).sumX() - prefixProdWeight(l).sumX();
    }

    /**
     * 重み順区間 [l, r) に含まれる変化点の集約値を計算する。
     * 計算量: O(log N)
     *
     * @param l 重み区間の下限
     * @param r 重み区間の上限
     * @return Aggregate(sumX, sumDelta, count)
     */
    int nodeRankByWeight(long w) {
        if (root == null) return 0;
        long totalW = getSumDelta(root);
        if (w < 0) return 0;
        if (w >= totalW) return getCount(root) - 1;

        int currentRank = 0;
        Node t = root;
        while (t != null) {
            push(t);
            long leftW = getSumDelta(t.left);
            if (w < leftW) {
                t = t.left;
            } else if (w < leftW + t.delta) {
                return currentRank + getCount(t.left);
            } else {
                currentRank += getCount(t.left) + 1;
                w -= leftW + t.delta;
                t = t.right;
            }
        }
        return currentRank;
    }

    private int rankByWeight(long w) {
        return nodeRankByWeight(w);
    }

    private Aggregate prefixProdWeight(long w) {
        if (w <= 0 || root == null) return new Aggregate(0, 0, 0);
        long totalW = getSumDelta(root);
        if (w >= totalW) return new Aggregate(getSumXDelta(root), totalW, getCount(root));

        long sumX = 0;
        long sumD = 0;

        Node t = root;
        while (t != null && w > 0) {
            push(t);
            long leftW = getSumDelta(t.left);
            if (w <= leftW) {
                t = t.left;
            } else if (w < leftW + t.delta) {
                long takeW = w - leftW;
                sumX += getSumXDelta(t.left) + t.x * takeW;
                sumD += leftW + takeW;
                break;
            } else {
                sumX += getSumXDelta(t.left) + t.x * t.delta;
                sumD += leftW + t.delta;
                w -= leftW + t.delta;
                t = t.right;
            }
        }
        return new Aggregate(sumX, sumD, 0);
    }

    /**
     * 重み順区間 [l, r) に含まれる変化点の集約値を計算する。
     * 計算量: O(log N)
     *
     * @param l 重み区間の下限 (0 <= l <= r <= total weight)
     * @param r 重み区間の上限 (0 <= l <= r <= total weight)
     * @return Aggregate(sumX, sumDelta, count)
     */
    public Aggregate prodBreakpointsByWeight(long l, long r) {
        long totalW = getSumDelta(root);
        if (l < 0 || l > r || r > totalW) {
            throw new IndexOutOfBoundsException(
                    "weight range out of bounds: [" + l + ", " + r
                    + "), total weight = " + totalW);
        }
        if (l == r) {
            return new Aggregate(0, 0, 0);
        }

        Aggregate prefR = prefixProdWeight(r);
        Aggregate prefL = prefixProdWeight(l);

        int cnt = rankByWeight(r - 1) - rankByWeight(l) + 1;

        return new Aggregate(
            prefR.sumX() - prefL.sumX(),
            prefR.sumDelta() - prefL.sumDelta(),
            cnt
        );
    }

    private static class MaxRightState {
        long skip;
        long sum;
        long taken;
        boolean stopped;
    }

    /**
     * 重み順区間 [l, r) の変化点 x の重み付き和が {@code limit} 以下となる
     * 最大の r を返す。
     *
     * <p>
     * 変化点を重み付き列
     *
     * <pre>
     * x_0, x_1, ..., x_(W-1)
     * </pre>
     *
     * と展開したとき、
     *
     * <pre>
     * sum_(k=l)^(r-1) x_k &lt;= limit
     * </pre>
     *
     * を満たす最大の r を返す。
     * </p>
     *
     * <p>
     * r を増加させたとき条件の真偽が true から false に
     * 高々1回だけ変化することを仮定する。
     * また空区間が条件を満たす、すなわち {@code 0 &lt;= limit} を要求する。
     * </p>
     *
     * <p>計算量: O(log N)</p>
     *
     * @param l 重み順区間の左端
     * @param limit 和の上限
     * @return 条件を満たす最大の r
     */
    // 未テスト
    public long maxRightBreakpointsByWeightSumLeq(long l, long limit) {
        long total = getSumDelta(root);
        if (l < 0 || l > total) {
            throw new IndexOutOfBoundsException("l out of bounds: " + l);
        }
        if (limit < 0) {
            throw new IllegalArgumentException("empty interval must satisfy the condition");
        }
        if (l == total) return total;

        MaxRightState state = new MaxRightState();
        state.skip = l;

        maxRightSumLeq(root, 0, limit, state);

        return l + state.taken;
    }

    /**
     * t の部分木を in-order の重み順に走査する。
     *
     * add は祖先 (t より上のノード) から伝播している x 座標への遅延加算。
     * ノード t 自身の t.lazyX による加算は t.x および t.sumXDelta に
     * 既に反映されているため、t 自身の評価には add を直接使用し、
     * 子へ降りる際に nextAdd = add + t.lazyX として引き渡す。
     *
     * push() は行わない。
     * push() すると read-only query なのに lazy 状態を書き換えるため、
     * ここでは祖先の lazyX を引数で持ち回る。
     */
    private static void maxRightSumLeq(
            Node t,
            long add,
            long limit,
            MaxRightState state) {

        if (t == null || state.stopped) return;

        long totalWeight = getSumDelta(t);

        // [0, l) に完全に含まれる部分木なら丸ごと飛ばす。
        if (state.skip >= totalWeight) {
            state.skip -= totalWeight;
            return;
        }

        /*
         * この部分木の先頭から全部採用可能なら、
         * 子へ降りずに subtree aggregate だけで処理する。
         */
        if (state.skip == 0) {
            long wholeSum = t.sumXDelta + add * t.sumDelta;

            if (state.sum + wholeSum <= limit) {
                state.sum += wholeSum;
                state.taken += totalWeight;
                return;
            }
        }

        long nextAdd = add + t.lazyX;

        // 左部分木
        maxRightSumLeq(t.left, nextAdd, limit, state);
        if (state.stopped) return;

        // 自分自身
        long d = t.delta;

        if (state.skip >= d) {
            state.skip -= d;
        } else {
            long available = d - state.skip;
            state.skip = 0;

            long x = t.x + add;
            long whole = x * available;

            if (state.sum + whole <= limit) {
                state.sum += whole;
                state.taken += available;
            } else {
                /*
                 * 同じ x が available 個並んでいる。
                 *
                 * state.sum + x * k <= limit
                 *
                 * を直接解くので、ここに二分探索は不要。
                 *
                 * prefix 条件が true -> false の一方向にしか
                 * 変化しないという事前条件の下では、
                 * このケースで x <= 0 になることはない。
                 */
                if (x <= 0) {
                    throw new IllegalStateException(
                            "prefix predicate is not monotone");
                }

                long k = (limit - state.sum) / x;
                if (k < 0) k = 0;
                if (k > available) k = available;

                state.sum += x * k;
                state.taken += k;
                state.stopped = true;
                return;
            }
        }

        // 右部分木
        maxRightSumLeq(t.right, nextAdd, limit, state);
    }

    /**
     * 重み順区間 [l, r) の変化点の重み付き和について
     * predicate が true となる最大の r を返す。
     *
     * <p>
     * r を増加させたとき predicate の真偽が
     * true から false に高々1回だけ変化することを仮定する。
     * {@code predicate.test(0)} は true でなければならない。
     * </p>
     *
     * <p>計算量: O(log N + log W)</p>
     *
     * @param l 重み順区間の左端
     * @param predicate 区間和に対する判定
     * @return 条件を満たす最大の r
     */
    // 未テスト
    public long maxRightBreakpointsByWeight(
            long l,
            LongPredicate predicate) {

        long total = getSumDelta(root);
        if (l < 0 || l > total) {
            throw new IndexOutOfBoundsException("l out of bounds: " + l);
        }
        if (!predicate.test(0)) {
            throw new IllegalArgumentException(
                    "predicate must be true for the empty interval");
        }
        if (l == total) return total;

        MaxRightState state = new MaxRightState();
        state.skip = l;

        maxRightByPredicate(root, 0, predicate, state);

        return l + state.taken;
    }

    private static void maxRightByPredicate(
            Node t,
            long add,
            LongPredicate predicate,
            MaxRightState state) {

        if (t == null || state.stopped) return;

        long totalWeight = getSumDelta(t);

        if (state.skip >= totalWeight) {
            state.skip -= totalWeight;
            return;
        }

        if (state.skip == 0) {
            long wholeSum = t.sumXDelta + add * t.sumDelta;

            if (predicate.test(state.sum + wholeSum)) {
                state.sum += wholeSum;
                state.taken += totalWeight;
                return;
            }
        }

        long nextAdd = add + t.lazyX;

        maxRightByPredicate(t.left, nextAdd, predicate, state);
        if (state.stopped) return;

        long d = t.delta;

        if (state.skip >= d) {
            state.skip -= d;
        } else {
            long available = d - state.skip;
            state.skip = 0;

            long x = t.x + add;

            if (predicate.test(state.sum + x * available)) {
                state.sum += x * available;
                state.taken += available;
            } else {
                long low = 0;
                long high = available;

                while (high - low > 1) {
                    long mid = (low + high) >>> 1;

                    if (predicate.test(state.sum + x * mid)) {
                        low = mid;
                    } else {
                        high = mid;
                    }
                }

                state.sum += x * low;
                state.taken += low;
                state.stopped = true;
                return;
            }
        }

        maxRightByPredicate(t.right, nextAdd, predicate, state);
    }

    // ============================================================
    // Virtual Merge Queries on Breakpoint Sequences
    // ============================================================

    /**
     * 変化点列の仮想マージクエリにおけるオブジェクト再利用ワークスペース。
     */
    static final class MergedBreakpointsWorkspace {
        private MarginalSeq[] pool = new MarginalSeq[64];
        private int ptr;

        final PartitionResult partition = new PartitionResult();
        final WeightSum weightSumA = new WeightSum();
        final WeightSum weightSumB = new WeightSum();

        MergedBreakpointsWorkspace() {
            for (int i = 0; i < pool.length; i++) {
                pool[i] = new MarginalSeq();
            }
        }

        void begin() {
            for (int i = 0; i < ptr; i++) {
                MarginalSeq s = pool[i];
                s.node = null;
                s.left = null;
                s.right = null;
            }
            ptr = 0;

            partition.a = 0;
            partition.b = 0;

            weightSumA.weight = 0;
            weightSumA.sum = 0;

            weightSumB.weight = 0;
            weightSumB.sum = 0;
        }

        MarginalSeq alloc() {
            if (ptr == pool.length) {
                int old = pool.length;
                MarginalSeq[] next = new MarginalSeq[old << 1];
                System.arraycopy(pool, 0, next, 0, old);

                for (int i = old; i < next.length; i++) {
                    next[i] = new MarginalSeq();
                }
                pool = next;
            }

            MarginalSeq s = pool[ptr++];

            s.node = null;
            s.left = null;
            s.right = null;

            return s;
        }
    }

    /**
     * 変化点の重み付き列を非破壊で表す内部 view。
     *
     * <p>各 view は昇順列を表し、内部ノードでは
     * left <= separator <= right が成り立つ。</p>
     */
    static final class MarginalSeq {
        static final int BLOCK = 0;
        static final int WHOLE = 1;
        static final int SUFFIX = 2;
        static final int CONCAT = 3;

        int kind;
        Node node;
        long add;

        // BLOCK
        long x;

        // CONCAT
        MarginalSeq left;
        MarginalSeq right;
        long separator;

        // aggregate
        long weight;
        long sum;
    }

    private static MarginalSeq blockSeq(long x, long weight, MergedBreakpointsWorkspace ws) {
        if (weight <= 0) return null;
        MarginalSeq s = ws.alloc();
        s.kind = MarginalSeq.BLOCK;
        s.x = x;
        s.weight = weight;
        s.sum = x * weight;
        return s;
    }

    /** t 全体を表す。add は t の祖先から未伝播の x 加算。 */
    static MarginalSeq wholeSeq(Node t, long add, MergedBreakpointsWorkspace ws) {
        if (t == null) return null;
        long x = t.x + add;
        if (t.left == null && t.right == null) {
            return blockSeq(x, t.delta, ws);
        }
        MarginalSeq s = ws.alloc();
        s.kind = MarginalSeq.WHOLE;
        s.node = t;
        s.add = add;
        s.weight = getSumDelta(t);
        s.sum = getSumXDelta(t) + add * s.weight;
        return s;
    }

    /** t 自身の block と t.right を表す。 */
    private static MarginalSeq suffixSeq(Node t, long add, MergedBreakpointsWorkspace ws) {
        if (t == null) return null;
        long x = t.x + add;
        if (t.right == null) {
            return blockSeq(x, t.delta, ws);
        }
        long childAdd = add + t.lazyX;
        long rightWeight = getSumDelta(t.right);

        MarginalSeq s = ws.alloc();
        s.kind = MarginalSeq.SUFFIX;
        s.node = t;
        s.add = add;
        s.weight = t.delta + rightWeight;
        s.sum = x * t.delta + getSumXDelta(t.right) + childAdd * rightWeight;
        return s;
    }

    private static MarginalSeq concatSeq(
            MarginalSeq left, MarginalSeq right, long separator, MergedBreakpointsWorkspace ws) {
        if (left == null) return right;
        if (right == null) return left;
        MarginalSeq s = ws.alloc();
        s.kind = MarginalSeq.CONCAT;
        s.left = left;
        s.right = right;
        s.separator = separator;
        s.weight = left.weight + right.weight;
        s.sum = left.sum + right.sum;
        return s;
    }

    private static boolean isBlockSeq(MarginalSeq s) {
        return s != null && s.kind == MarginalSeq.BLOCK;
    }

    /** 内部 view の分割値。left <= separator <= right。 */
    private static long seqSeparator(MarginalSeq s) {
        if (s.kind == MarginalSeq.CONCAT) return s.separator;
        Node t = s.node;
        return t.x + s.add;
    }

    private static MarginalSeq seqLeft(MarginalSeq s, MergedBreakpointsWorkspace ws) {
        if (s.kind == MarginalSeq.CONCAT) return s.left;

        Node t = s.node;
        long x = t.x + s.add;
        long childAdd = s.add + t.lazyX;

        if (s.kind == MarginalSeq.SUFFIX) {
            return blockSeq(x, t.delta, ws);
        }

        // WHOLE
        if (t.left != null) {
            return wholeSeq(t.left, childAdd, ws);
        }
        return blockSeq(x, t.delta, ws);
    }

    private static MarginalSeq seqRight(MarginalSeq s, MergedBreakpointsWorkspace ws) {
        if (s.kind == MarginalSeq.CONCAT) return s.right;

        Node t = s.node;
        long childAdd = s.add + t.lazyX;

        if (s.kind == MarginalSeq.SUFFIX) {
            return wholeSeq(t.right, childAdd, ws);
        }

        // WHOLE
        if (t.left != null) {
            return suffixSeq(t, s.add, ws);
        }
        return wholeSeq(t.right, childAdd, ws);
    }

    /**
     * s の先頭 weight 個を除いた suffix view を返す。
     * 元の Treap は変更しない。
     */
    static MarginalSeq dropSeqPrefix(MarginalSeq s, long weight, MergedBreakpointsWorkspace ws) {
        if (weight <= 0) return s;
        if (s == null || weight >= s.weight) return null;
        if (isBlockSeq(s)) {
            return blockSeq(s.x, s.weight - weight, ws);
        }

        MarginalSeq left = seqLeft(s, ws);
        long leftWeight = left == null ? 0 : left.weight;
        if (weight < leftWeight) {
            return concatSeq(dropSeqPrefix(left, weight, ws), seqRight(s, ws), seqSeparator(s), ws);
        }
        if (weight == leftWeight) {
            return seqRight(s, ws);
        }
        return dropSeqPrefix(seqRight(s, ws), weight - leftWeight, ws);
    }

    /**
     * s の末尾 weight 個を除いた prefix view を返す。
     * 元の Treap は変更しない。
     */
    static MarginalSeq dropSeqSuffix(MarginalSeq s, long weight, MergedBreakpointsWorkspace ws) {
        if (weight <= 0) return s;
        if (s == null || weight >= s.weight) return null;
        if (isBlockSeq(s)) {
            return blockSeq(s.x, s.weight - weight, ws);
        }

        MarginalSeq right = seqRight(s, ws);
        long rightWeight = right == null ? 0 : right.weight;
        if (weight < rightWeight) {
            return concatSeq(seqLeft(s, ws), dropSeqSuffix(right, weight, ws), seqSeparator(s), ws);
        }
        if (weight == rightWeight) {
            return seqLeft(s, ws);
        }
        return dropSeqSuffix(seqLeft(s, ws), weight - rightWeight, ws);
    }

    /**
     * s のうち値が strictly 判定 <= xVal であるプレフィックスを除外する。
     */
    static MarginalSeq dropSeqValueLeq(MarginalSeq s, long xVal, MergedBreakpointsWorkspace ws) {
        if (s == null) return null;
        if (isBlockSeq(s)) {
            if (s.x <= xVal) return null;
            return s;
        }

        long sep = seqSeparator(s);
        if (sep <= xVal) {
            return dropSeqValueLeq(seqRight(s, ws), xVal, ws);
        }
        MarginalSeq left = dropSeqValueLeq(seqLeft(s, ws), xVal, ws);
        return concatSeq(left, seqRight(s, ws), sep, ws);
    }

    /**
     * s のうち値が strictly 判定 >= xVal であるサフィックスを除外する。
     */
    static MarginalSeq dropSeqValueGeq(MarginalSeq s, long xVal, MergedBreakpointsWorkspace ws) {
        if (s == null) return null;
        if (isBlockSeq(s)) {
            if (s.x >= xVal) return null;
            return s;
        }

        long sep = seqSeparator(s);
        if (sep >= xVal) {
            return dropSeqValueGeq(seqLeft(s, ws), xVal, ws);
        }
        MarginalSeq right = dropSeqValueGeq(seqRight(s, ws), xVal, ws);
        return concatSeq(seqLeft(s, ws), right, sep, ws);
    }

    static MarginalSeq sliceSeq(Node root, long lowVal, long highVal, MergedBreakpointsWorkspace ws) {
        if (root == null || lowVal >= highVal) return null;
        MarginalSeq s = wholeSeq(root, 0, ws);
        s = dropSeqValueLeq(s, lowVal, ws);
        s = dropSeqValueGeq(s, highVal, ws);
        return s;
    }


    static final class PartitionResult {
        long a;
        long b;
    }

    /**
     * a と b の sorted multiset union の先頭 k 個が、
     * a から何個、b から何個来るかを求める dual binary search。
     */
    static void partitionFirstK(
            MarginalSeq a, MarginalSeq b, long k, PartitionResult out, MergedBreakpointsWorkspace ws) {
        if (k <= 0) return;
        if (a == null) {
            out.b += k;
            return;
        }
        if (b == null) {
            out.a += k;
            return;
        }
        if (k == a.weight + b.weight) {
            out.a += a.weight;
            out.b += b.weight;
            return;
        }

        if (isBlockSeq(a) && isBlockSeq(b)) {
            if (a.x <= b.x) {
                long takeA = Math.min(k, a.weight);
                out.a += takeA;
                k -= takeA;
                if (k > 0) out.b += k;
            } else {
                long takeB = Math.min(k, b.weight);
                out.b += takeB;
                k -= takeB;
                if (k > 0) out.a += k;
            }
            return;
        }

        if (isBlockSeq(a)) {
            MarginalSeq bl = seqLeft(b, ws);
            MarginalSeq br = seqRight(b, ws);
            long wb = bl.weight;
            long sep = seqSeparator(b);

            if (a.x < sep) {
                if (k <= a.weight + wb) {
                    partitionFirstK(a, bl, k, out, ws);
                } else {
                    out.a += a.weight;
                    out.b += wb;
                    partitionFirstK(null, br, k - a.weight - wb, out, ws);
                }
            } else {
                if (k <= wb) {
                    partitionFirstK(null, bl, k, out, ws);
                } else {
                    out.b += wb;
                    partitionFirstK(a, br, k - wb, out, ws);
                }
            }
            return;
        }

        if (isBlockSeq(b)) {
            MarginalSeq al = seqLeft(a, ws);
            MarginalSeq ar = seqRight(a, ws);
            long wa = al.weight;
            long sep = seqSeparator(a);

            if (b.x < sep) {
                if (k <= b.weight + wa) {
                    partitionFirstK(al, b, k, out, ws);
                } else {
                    out.a += wa;
                    out.b += b.weight;
                    partitionFirstK(ar, null, k - wa - b.weight, out, ws);
                }
            } else {
                if (k <= wa) {
                    partitionFirstK(al, null, k, out, ws);
                } else {
                    out.a += wa;
                    partitionFirstK(ar, b, k - wa, out, ws);
                }
            }
            return;
        }

        MarginalSeq al = seqLeft(a, ws);
        MarginalSeq ar = seqRight(a, ws);
        MarginalSeq bl = seqLeft(b, ws);
        MarginalSeq br = seqRight(b, ws);
        long wa = al.weight;
        long wb = bl.weight;
        long sa = seqSeparator(a);
        long sb = seqSeparator(b);

        if (sa < sb) {
            if (k <= wa + wb) {
                partitionFirstK(a, bl, k, out, ws);
            } else {
                out.a += wa;
                partitionFirstK(ar, b, k - wa, out, ws);
            }
        } else if (sa > sb) {
            if (k <= wa + wb) {
                partitionFirstK(al, b, k, out, ws);
            } else {
                out.b += wb;
                partitionFirstK(a, br, k - wb, out, ws);
            }
        } else {
            if (k <= wa + wb) {
                partitionFirstK(al, bl, k, out, ws);
            } else {
                out.a += wa;
                out.b += wb;
                partitionFirstK(ar, br, k - wa - wb, out, ws);
            }
        }
    }

    private static final class WeightSum {
        long weight;
        long sum;
    }

    /** s の先頭に並ぶ 0 以下の値をすべて集約する。 */
    private static void prefixNonPositive(MarginalSeq s, WeightSum out, MergedBreakpointsWorkspace ws) {
        if (s == null) return;
        if (isBlockSeq(s)) {
            if (s.x <= 0) {
                out.weight += s.weight;
                out.sum += s.sum;
            }
            return;
        }

        MarginalSeq left = seqLeft(s, ws);
        long sep = seqSeparator(s);
        if (sep > 0) {
            prefixNonPositive(left, out, ws);
        } else {
            out.weight += left.weight;
            out.sum += left.sum;
            prefixNonPositive(seqRight(s, ws), out, ws);
        }
    }

    /** s の値がすべて正であるとき、和が limit 以下の最大 prefix 長を返す。 */
    private static long maxPrefixPositive(MarginalSeq s, long limit, MergedBreakpointsWorkspace ws) {
        if (s == null) return 0;
        if (isBlockSeq(s)) {
            return Math.min(s.weight, limit / s.x);
        }

        MarginalSeq left = seqLeft(s, ws);
        if (left.sum > limit) {
            return maxPrefixPositive(left, limit, ws);
        }
        return left.weight + maxPrefixPositive(seqRight(s, ws), limit - left.sum, ws);
    }

    /**
     * a,b の値がすべて正であるとき、sorted multiset union の
     * 和が limit 以下の最大 prefix 長を返す。
     */
    private static long maxPrefixPositive(MarginalSeq a, MarginalSeq b, long limit, MergedBreakpointsWorkspace ws) {
        if (a == null) return maxPrefixPositive(b, limit, ws);
        if (b == null) return maxPrefixPositive(a, limit, ws);

        if (isBlockSeq(a) && isBlockSeq(b)) {
            if (a.x > b.x) return maxPrefixPositive(b, a, limit, ws);

            long takeA = Math.min(a.weight, limit / a.x);
            if (takeA < a.weight) return takeA;

            limit -= a.sum;
            long takeB = Math.min(b.weight, limit / b.x);
            return a.weight + takeB;
        }

        if (isBlockSeq(a)) {
            MarginalSeq bl = seqLeft(b, ws);
            MarginalSeq br = seqRight(b, ws);
            long sep = seqSeparator(b);

            if (a.x < sep) {
                if (a.sum + bl.sum > limit) {
                    return maxPrefixPositive(a, bl, limit, ws);
                }
                return bl.weight + maxPrefixPositive(a, br, limit - bl.sum, ws);
            } else {
                if (bl.sum > limit) {
                    return maxPrefixPositive(bl, limit, ws);
                }
                return bl.weight + maxPrefixPositive(a, br, limit - bl.sum, ws);
            }
        }

        if (isBlockSeq(b)) {
            return maxPrefixPositive(b, a, limit, ws);
        }

        MarginalSeq al = seqLeft(a, ws);
        MarginalSeq ar = seqRight(a, ws);
        MarginalSeq bl = seqLeft(b, ws);
        MarginalSeq br = seqRight(b, ws);
        long sa = seqSeparator(a);
        long sb = seqSeparator(b);

        if (sa < sb) {
            if (al.sum + bl.sum > limit) {
                return maxPrefixPositive(a, bl, limit, ws);
            }
            return al.weight + maxPrefixPositive(ar, b, limit - al.sum, ws);
        } else if (sa > sb) {
            if (al.sum + bl.sum > limit) {
                return maxPrefixPositive(al, b, limit, ws);
            }
            return bl.weight + maxPrefixPositive(a, br, limit - bl.sum, ws);
        } else {
            if (al.sum + bl.sum > limit) {
                return maxPrefixPositive(al, bl, limit, ws);
            }
            return al.weight + bl.weight
                    + maxPrefixPositive(ar, br, limit - al.sum - bl.sum, ws);
        }
    }

    /**
     * 2つの変化点列の sorted multiset union に対し、
     * 区間 [l,r) の重み付き和が limit 以下となる最大の r を返す。
     *
     * <p>計算量: O(log(N_a + N_b))</p>
     *
     * @param a 1つ目の変化点列
     * @param b 2つ目の変化点列
     * @param l 区間の左端（重み順）
     * @param limit 区間和の上限（&gt;= 0）
     * @param ws 再利用ワークスペース
     * @return 条件を満たす最大の r
     */
    // 未テスト
    static long maxRightMergedBreakpointsByWeightSumLeq(
            MarginalSeq sa,
            MarginalSeq sb,
            long l,
            long limit,
            MergedBreakpointsWorkspace ws) {

        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }

        long wa = sa == null ? 0 : sa.weight;
        long wb = sb == null ? 0 : sb.weight;
        long total = wa + wb;

        if (l < 0 || l > total) {
            throw new IndexOutOfBoundsException("l out of bounds: " + l);
        }
        if (l == total) return total;

        PartitionResult part = ws.partition;
        partitionFirstK(sa, sb, l, part, ws);

        sa = dropSeqPrefix(sa, part.a, ws);
        sb = dropSeqPrefix(sb, part.b, ws);

        WeightSum nonPosA = ws.weightSumA;
        WeightSum nonPosB = ws.weightSumB;

        prefixNonPositive(sa, nonPosA, ws);
        prefixNonPositive(sb, nonPosB, ws);

        long nonPosWeight = nonPosA.weight + nonPosB.weight;
        long nonPosSum = nonPosA.sum + nonPosB.sum;

        long answer = l + nonPosWeight;
        limit -= nonPosSum;

        sa = dropSeqPrefix(sa, nonPosA.weight, ws);
        sb = dropSeqPrefix(sb, nonPosB.weight, ws);

        return answer + maxPrefixPositive(sa, sb, limit, ws);
    }

    // ============================================================
    // Split / Concatenate & Merging
    // ============================================================

    /**
     * 関数を x を境界として左右の凸関数に分割する。
     * 計算量: O(log N)
     *
     * @param x 分割点
     * @return [leftPart, rightPart]
     */
    public SlopeTrickSuper[] splitDomain(long x) {
        SlopeTrickSuper leftPart = new SlopeTrickSuper();
        SlopeTrickSuper rightPart = new SlopeTrickSuper();

        long leftDL = this.domainL;
        long leftDR = Math.min(this.domainR, x);
        long rightDL = Math.max(this.domainL, x);
        long rightDR = this.domainR;

        Node[] resX1 = splitByX(root, x - 1); // resX1[0]: < x, resX1[1]: >= x
        Node[] resX2 = splitByX(resX1[1], x);  // resX2[0]: == x, resX2[1]: > x

        leftPart.root = resX1[0];
        leftPart.domainL = leftDL;
        leftPart.domainR = leftDR;
        leftPart.baseSlope = this.baseSlope;
        leftPart.baseIntercept = this.baseIntercept;

        long sumDLeq = getSumDelta(resX1[0]) + getSumDelta(resX2[0]);
        long sumXDLeq = getSumXDelta(resX1[0]) + getSumXDelta(resX2[0]);

        rightPart.root = resX2[1];
        rightPart.domainL = rightDL;
        rightPart.domainR = rightDR;
        rightPart.baseSlope = this.baseSlope + sumDLeq;
        rightPart.baseIntercept = this.baseIntercept - sumXDLeq;

        this.clear();
        return new SlopeTrickSuper[]{leftPart, rightPart};
    }

    /**
     * 2つの接続する凸関数片を連結する。
     * 計算量: O(log N)
     *
     * @param right 接続する右側の凸関数
     * @return this
     */
    public SlopeTrickSuper concat(SlopeTrickSuper right) {
        if (this.domainR != right.domainL) {
            throw new IllegalArgumentException("Domains are not adjacent");
        }
        long boundary = this.domainR;
        if (this.getAt(boundary) != right.getAt(boundary)) {
            throw new IllegalArgumentException("Function values at boundary do not match");
        }
        long leftSlope = this.getSlopeLeft(boundary);
        long rightSlope = right.getSlopeRight(boundary);
        if (leftSlope > rightSlope) {
            throw new IllegalArgumentException("Slope sequence violates convexity at boundary");
        }

        long desiredDelta = rightSlope - leftSlope;

        Node[] resL = splitByX(this.root, boundary - 1);
        this.root = resL[0];

        Node[] resR = splitByX(right.root, boundary);
        right.root = resR[1];

        this.root = merge(this.root, right.root);

        this.domainR = right.domainR;

        if (desiredDelta > 0) {
            rawInsertNode(boundary, desiredDelta);
        }

        right.clear();
        return this;
    }

    private static void inorderRawInsert(SlopeTrickSuper self, Node t) {
        if (t == null) return;
        push(t);
        inorderRawInsert(self, t.left);
        self.rawInsertNode(t.x, t.delta);
        inorderRawInsert(self, t.right);
    }

    private static void collectBreakpoints(Node t, java.util.List<Node> list) {
        if (t == null) return;
        push(t);
        collectBreakpoints(t.left, list);
        list.add(t);
        collectBreakpoints(t.right, list);
    }

    /**
     * 他の SlopeTrickSuper g を破壊的に加算する (f(x) += g(x))。
     * 計算量: O(min(N_f, N_g) log(N_f + N_g)) (Small-to-Large マージ)
     *
     * @param g 加算する関数 (破壊される)
     * @return this
     */
    public SlopeTrickSuper mergeDestructive(SlopeTrickSuper g) {
        if (this == g) throw new IllegalArgumentException("Cannot merge itself");

        long newDomainL = Math.max(this.domainL, g.domainL);
        long newDomainR = Math.min(this.domainR, g.domainR);

        if (newDomainL > newDomainR) {
            clear();
            domainL = 1;
            domainR = 0;
            g.clear();
            return this;
        }

        this.restrictDomain(newDomainL, newDomainR);
        g.restrictDomain(newDomainL, newDomainR);

        if (getCount(this.root) < getCount(g.root)) {
            Node tmpRoot = root; root = g.root; g.root = tmpRoot;
        }

        this.baseSlope += g.baseSlope;
        this.baseIntercept += g.baseIntercept;

        inorderRawInsert(this, g.root);

        g.clear();
        return this;
    }

    /**
     * 他の SlopeTrickSuper g との min-plus 畳み込みを行う ((f Box g)(x) = min_y (f(x - y) + g(y)))。
     * 計算量: O(N_g log(N_f + N_g))
     *
     * @param g 畳み込む関数 (破壊される)
     * @return this
     */
    public SlopeTrickSuper minPlusConvolveDestructive(SlopeTrickSuper g) {
        if (this == g) throw new IllegalArgumentException("Cannot convolve with itself");

        if (g.domainL > g.domainR || this.domainL > this.domainR) {
            clear();
            domainL = 1;
            domainR = 0;
            g.clear();
            return this;
        }

        java.util.List<Node> bp = new java.util.ArrayList<>();
        collectBreakpoints(g.root, bp);

        if (g.domainL == -INF) {
            long slope0 = bp.isEmpty() ? g.baseSlope : g.getSlopeLeft(bp.get(0).x);
            this.clampSlope(slope0, INF);
        }
        if (g.domainR == INF) {
            long slopeLast = bp.isEmpty() ? g.baseSlope : g.getSlopeRight(bp.get(bp.size() - 1).x);
            this.clampSlope(-INF, slopeLast);
        }

        long curX;
        long curSlope;

        if (g.domainL > -INF) {
            curX = g.domainL;
            curSlope = g.getSlopeRight(g.domainL);
            long valL = g.getAt(curX);
            this.translate(curX);
            this.addConst(valL);
        } else if (!bp.isEmpty()) {
            curX = bp.get(0).x;
            curSlope = g.getSlopeLeft(curX);
            long valX = g.getAt(curX);
            this.translate(curX);
            this.addConst(valX);
        } else {
            this.clampSlope(g.baseSlope, g.baseSlope);
            this.addConst(g.baseIntercept);
            g.clear();
            return this;
        }

        for (Node b : bp) {
            if (b.x > curX) {
                long len = b.x - curX;
                this.convolveLinearSegment(0, len, curSlope, 0);
            }
            curX = b.x;
            curSlope += b.delta;
        }

        if (g.domainR < INF && curX < g.domainR) {
            long len = g.domainR - curX;
            this.convolveLinearSegment(0, len, curSlope, 0);
        }

        g.clear();
        return this;
    }

    /**
     * h(x) = inf_(sum_i x_i = x) sum_i functions[i](x_i) を返す。
     * 空リストの場合は h(0) = 0、それ以外では h(x) = INF。
     * 空の定義域を持つ要素がある場合は h の定義域も空となる。
     * 空の定義域を持つ要素がない場合、h が -inf をとらないことを前提とする。
     * 入力のリストおよび各関数は変更せず、返り値と入力の間でノードを共有しない。
     * 同一インスタンスの重複は、それぞれ独立した項として扱う。
     * 計算量: 要素数 K、変化点数の総和 N に対して期待時間 O((N + K + 1) log(N + K + 2))、
     * 追加領域 O(N + K + 1)。
     *
     * @param functions 畳み込む関数列
     * @return h を表す新しい関数
     */
    // 未テスト
    public static SlopeTrickSuper minPlusConvolve(List<SlopeTrickSuper> functions) {
        SlopeTrickSuper result = new SlopeTrickSuper();
        result.restrictDomain(0, 0);
        for (SlopeTrickSuper f : functions) {
            if (f.domainL > f.domainR) {
                result.domainL = 1;
                result.domainR = 0;
                return result;
            }
        }
        for (SlopeTrickSuper f : functions) {
            SlopeTrickSuper copy = new SlopeTrickSuper();
            copy.domainL = f.domainL;
            copy.domainR = f.domainR;
            copy.baseSlope = f.baseSlope;
            copy.baseIntercept = f.baseIntercept;
            copy.root = copyConvolutionNodes(f.root);
            result.minPlusConvolveDestructive(copy);
        }
        return result;
    }

    /**
     * t と同じ変化点・集約値・遅延値を持ち、ノードを共有しない木を返す。
     * 計算量: ノード数 N に対して時間 O(N + 1)、追加領域 O(N + 1)。
     *
     * @param t 複製元の根（null 可）
     * @return 複製した木の根
     */
    // 未テスト
    private static Node copyConvolutionNodes(Node t) {
        if (t == null) return null;
        Node copy = new Node(t.x, t.delta, t.priority);
        copy.left = copyConvolutionNodes(t.left);
        copy.right = copyConvolutionNodes(t.right);
        copy.sumDelta = t.sumDelta;
        copy.sumXDelta = t.sumXDelta;
        copy.count = t.count;
        copy.lazyX = t.lazyX;
        return copy;
    }

    List<Long> getPrimalBreakpointsForDual() {
        List<Node> nodes = new ArrayList<>();
        collectBreakpoints(root, nodes);
        List<Long> res = new ArrayList<>();
        if (domainL > -INF) {
            long xL = getSlopeRight(domainL);
            if (Math.abs(xL) < INF) res.add(xL);
        }
        for (Node n : nodes) {
            long xl = getSlopeLeft(n.x);
            long xr = getSlopeRight(n.x);
            if (Math.abs(xl) < INF) res.add(xl);
            if (Math.abs(xr) < INF) res.add(xr);
        }
        if (domainR < INF) {
            long xR = getSlopeLeft(domainR);
            if (Math.abs(xR) < INF) res.add(xR);
        }
        return res;
    }

    private static final AtomicInteger drawCount = new AtomicInteger(0);

    /**
     * 関数 f(x) を可視化するための XYChart オブジェクトを構築する。
     *
     * <p>計算量: O(N log N)</p>
     *
     * @return 描画用の XYChart オブジェクト
     */
    // 未テスト
    public XYChart toChart() {
        List<Node> nodes = new ArrayList<>();
        collectBreakpoints(root, nodes);

        List<Long> changePoints = new ArrayList<>();
        if (domainL > -INF) {
            changePoints.add(domainL);
        }
        for (Node n : nodes) {
            changePoints.add(n.x);
        }
        if (domainR < INF) {
            changePoints.add(domainR);
        }
        Collections.sort(changePoints);

        long minX, maxX;
        if (changePoints.isEmpty()) {
            minX = -10;
            maxX = 10;
        } else {
            long low = changePoints.get(0);
            long high = changePoints.get(changePoints.size() - 1);
            if (low == high) {
                minX = low - 10;
                maxX = low + 10;
            } else {
                long diff = high - low;
                minX = (domainL > -INF) ? domainL : low - diff / 10 - 1;
                maxX = (domainR < INF) ? domainR : high + diff / 10 + 1;
            }
        }

        List<Long> xData = new ArrayList<>();
        xData.add(minX);
        for (long cp : changePoints) {
            if (cp > minX && cp < maxX) {
                xData.add(cp);
            }
        }
        xData.add(maxX);

        List<Double> distinctX = xData.stream()
                .distinct()
                .sorted()
                .map(Long::doubleValue)
                .collect(Collectors.toList());

        List<Double> distinctY = distinctX.stream()
                .map(x -> (double) getAt(Math.round(x)))
                .collect(Collectors.toList());

        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("SlopeTrickSuper-" + drawCount.incrementAndGet())
                .xAxisTitle("x")
                .yAxisTitle("f(x)")
                .build();

        chart.addSeries("f(x)", distinctX, distinctY)
                .setMarker(SeriesMarkers.CIRCLE);

        return chart;
    }

    /**
     * 関数 f(x) を可視化してウィンドウに表示する。
     *
     * <p>計算量: O(N log N)</p>
     */
    // 未テスト
    public void draw() {
        new SwingWrapper<>(toChart()).displayChart();
    }
}
