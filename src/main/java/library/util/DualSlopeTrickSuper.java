package library.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

/**
 * DualSlopeTrickSuper: Legendre-Fenchel 凸共役 f*(p) = sup_x (p*x - f(x)) を介して
 * 離散凸関数 f: R -> R U {+inf} を管理するクラス。
 */
public class DualSlopeTrickSuper {

    private final SlopeTrickSuper st;

    /**
     * f(0) = 0, f(x) = +inf (x != 0) で初期化する。
     * 計算量: O(1)
     */
    public DualSlopeTrickSuper() {
        this.st = new SlopeTrickSuper();
    }

    /**
     * 関数を初期状態 f(0) = 0, f(x) = +inf (x != 0) にリセットする。
     * 計算量: O(1)
     * // 未テスト
     */
    public void clear() {
        st.clear();
    }

    /**
     * 内部の SlopeTrickSuper (凸共役 f*) を取得する。
     * @return st
     */
    public SlopeTrickSuper getDualSlopeTrickSuper() {
        return st;
    }

    // ============================================================
    // Domain & Slope Queries
    // ============================================================

    /**
     * f(x) が有限の値をとる x の下限を取得する。
     * 凸共役 f* の最小傾きに対応する。
     *
     * <p>計算量: O(1)</p>
     *
     * @return f の定義域の下限
     */
    // 未テスト
    public long domainLeft() {
        return st.dualDomainLeft();
    }

    /**
     * f(x) が有限の値をとる x の上限を取得する。
     * 凸共役 f* の最大傾きに対応する。
     *
     * <p>計算量: O(1)</p>
     *
     * @return f の定義域の上限
     */
    // 未テスト
    public long domainRight() {
        return st.dualDomainRight();
    }

    // ============================================================
    // Primal Queries
    // ============================================================

    /**
     * 原点での関数値 f(0) を取得する。
     * f(0) = (f*)*(0) = - min_p f*(p).
     * 計算量: O(log N)
     *
     * @return f(0)
     */
    public long getAtZero() {
        SlopeTrickSuper.QueryResult qr = st.getMin();
        if (qr.min() == SlopeTrickSuper.INF) return SlopeTrickSuper.INF;
        return -qr.min();
    }

    /**
     * 指定された点 x における関数値 f(x) を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return f(x)
     */
    public long getAt(long x) {
        long val = st.minMinusLinear(x);
        if (val == SlopeTrickSuper.INF || val == -SlopeTrickSuper.INF) return SlopeTrickSuper.INF;
        return -val;
    }

    /**
     * 最小値 min_x f(x) を取得する。
     * 計算量: O(log N)
     *
     * @return min f(x)
     */
    public long getMin() {
        long val = st.getAt(0);
        if (val == SlopeTrickSuper.INF) return -SlopeTrickSuper.INF;
        if (val == -SlopeTrickSuper.INF) return SlopeTrickSuper.INF;
        return -val;
    }

    /**
     * f(x) の最小値 min f(x) および最小値をとる区間 [lo, hi] を取得する。
     * 計算量: O(log N)
     *
     * @return QueryResult(min, lo, hi)
     */
    // 未テスト
    public SlopeTrickSuper.QueryResult argmin() {
        long minVal = getMin();
        if (minVal == SlopeTrickSuper.INF || minVal == -SlopeTrickSuper.INF) {
            return new SlopeTrickSuper.QueryResult(minVal, SlopeTrickSuper.INF, -SlopeTrickSuper.INF);
        }
        long DL = st.domainLeft();
        long DR = st.domainRight();

        long lo = (DL == 0) ? -SlopeTrickSuper.INF : st.getSlopeLeft(0);
        long hi = (DR == 0) ? SlopeTrickSuper.INF : st.getSlopeRight(0);
        return new SlopeTrickSuper.QueryResult(minVal, lo, hi);
    }

    /**
     * 指定された点 x における左微分 f'_-(x) を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return 左傾き
     */
    // 未テスト
    public long getSlopeLeft(long x) {
        if (x < domainLeft()) return -SlopeTrickSuper.INF;
        if (x > domainRight()) return SlopeTrickSuper.INF;
        SlopeTrickSuper.QueryResult qr = st.argminMinusLinear(x);
        return qr.lo();
    }

    /**
     * 指定された点 x における右微分 f'_+(x) を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return 右傾き
     */
    // 未テスト
    public long getSlopeRight(long x) {
        if (x < domainLeft()) return -SlopeTrickSuper.INF;
        if (x > domainRight()) return SlopeTrickSuper.INF;
        SlopeTrickSuper.QueryResult qr = st.argminMinusLinear(x);
        return qr.hi();
    }

    /**
     * 指定された点 x におけるサブグラディエント（劣微分）を取得する。
     * 計算量: O(log N)
     *
     * @param x 評価位置
     * @return サブグラディエント
     */
    // 未テスト
    public long getSubgradient(long x) {
        long sl = getSlopeLeft(x);
        long sr = getSlopeRight(x);
        if (sl <= 0 && sr >= 0) return 0;
        if (sr < 0) return sr;
        return sl;
    }

    // ============================================================
    // Affine & Shift Operations
    // ============================================================

    /**
     * f(x) += b を行う。
     * 計算量: O(1)
     *
     * @param b 切片
     * @return this
     */
    public DualSlopeTrickSuper addConst(long b) {
        st.addConst(-b);
        return this;
    }

    /**
     * f(x) += a * x + b を行う。
     * 計算量: O(1)
     *
     * @param a 傾き
     * @param b 切片
     * @return this
     */
    public DualSlopeTrickSuper addLinear(long a, long b) {
        st.translate(a);
        st.addConst(-b);
        return this;
    }

    /**
     * f(x) <- f(x - d) を行う。
     * f*(p) <- f*(p) + d * p.
     * 計算量: O(1)
     *
     * @param d 平行移動量
     * @return this
     */
    public DualSlopeTrickSuper shift(long d) {
        st.addLinear(d);
        return this;
    }

    // ============================================================
    // Marginal Extend / Shrink Operations
    // ============================================================

    /**
     * g(x) = min_(0 <= k <= length) { f(x - k) + slope * k } を計算する。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @param length 伸ばす長さ (>= 0)
     * @return this
     */
    public DualSlopeTrickSuper extendRight(long slope, long length) {
        st.addRelu(slope, length);
        return this;
    }

    /**
     * g(x) = min_(0 <= k <= 1) { f(x - k) + slope * k } を計算する。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @return this
     */
    public DualSlopeTrickSuper extendRight(long slope) {
        return extendRight(slope, 1L);
    }

    /**
     * g(x) = min_(0 <= k <= length) { f(x + k) - slope * k } を計算する。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @param length 伸ばす長さ (>= 0)
     * @return this
     */
    public DualSlopeTrickSuper extendLeft(long slope, long length) {
        st.addIrelu(slope, length);
        return this;
    }

    /**
     * g(x) = min_(0 <= k <= 1) { f(x + k) - slope * k } を計算する。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @return this
     */
    public DualSlopeTrickSuper extendLeft(long slope) {
        return extendLeft(slope, 1L);
    }

    /**
     * g(x) = min_(-length <= k <= length)
     *        { f(x - k) + slope * |k| }
     * を計算する。
     *
     * <p>
     * すなわち、各点 x の値を、距離 1 あたり {@code slope} のコストを払って
     * 左右それぞれ最大 {@code length} だけ移動させたときの最小値で更新する。
     * </p>
     *
     * <p>計算量: O(log N)</p>
     *
     * @param slope 移動距離 1 あたりのコスト
     * @param length 左右それぞれに伸ばす最大長 (>= 0)
     * @return this
     */
    public DualSlopeTrickSuper extendBoth(long slope, long length) {
        extendRight(slope, length);
        extendLeft(slope, length);
        return this;
    }

    /**
     * g(x) = min_(-1 <= k <= 1)
     *        { f(x - k) + slope * |k| }
     * を計算する。
     *
     * <p>計算量: O(log N)</p>
     *
     * @param slope 移動距離 1 あたりのコスト
     * @return this
     */
    public DualSlopeTrickSuper extendBoth(long slope) {
        return extendBoth(slope, 1L);
    }

    /**
     * extendRight(slope, length) の逆操作を行う。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @param length 長さ
     * @return this
     */
    public DualSlopeTrickSuper shrinkRight(long slope, long length) {
        st.removeRelu(slope, length);
        return this;
    }

    /**
     * extendRight(slope, 1) の逆操作を行う。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @return this
     */
    public DualSlopeTrickSuper shrinkRight(long slope) {
        return shrinkRight(slope, 1L);
    }

    /**
     * extendLeft(slope, length) の逆操作を行う。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @param length 長さ
     * @return this
     */
    public DualSlopeTrickSuper shrinkLeft(long slope, long length) {
        st.removeIrelu(slope, length);
        return this;
    }

    /**
     * extendLeft(slope, 1) の逆操作を行う。
     * 計算量: O(log N)
     *
     * @param slope 傾き
     * @return this
     */
    public DualSlopeTrickSuper shrinkLeft(long slope) {
        return shrinkLeft(slope, 1L);
    }

    // ============================================================
    // Primal ReLU Addition & Slide Min
    // ============================================================

    /**
     * f(x) += max(c * (x - a), 0) を行う。
     * 計算量: O(log N)
     *
     * @param c 傾き
     * @param a 変化点
     * @return this
     */
    public DualSlopeTrickSuper addLinearOrZero(long c, long a) {
        if (c > 0) {
            st.convolveLinearSegment(0, c, a, 0);
        } else if (c < 0) {
            st.convolveLinearSegment(c, 0, a, 0);
        }
        return this;
    }

    /**
     * f(x) <- min_(a <= d <= b) f(x - d) を行う。
     * 計算量: O(log N)
     *
     * @param a 移動範囲の下限
     * @param b 移動範囲の上限 (>= a)
     * @return this
     */
    public DualSlopeTrickSuper slideMin(long a, long b) {
        if (a > b) throw new IllegalArgumentException("a must be <= b");
        st.addLinear(a);
        if (b > a) {
            st.addRelu(0, b - a);
        }
        return this;
    }

    // ============================================================
    // Domain / Slope Restrictions
    // ============================================================

    /**
     * 定義域を [l, r] に制限する (x not in [l, r] => f(x) = +inf)。
     * 双対では f*(p) の傾き制限 clampSlope(l, r) に対応。
     * 計算量: O(log N)
     *
     * @param l 下限
     * @param r 上限
     * @return this
     */
    public DualSlopeTrickSuper restrictDomain(long l, long r) {
        st.clampSlope(l, r);
        return this;
    }

    /**
     * 傾きを [lo, hi] に制限する。
     * 双対では f*(p) の定義域制限 restrictDomain(lo, hi) に対応。
     * 計算量: O(log N)
     *
     * @param lo 傾き下限
     * @param hi 傾き上限
     * @return this
     */
    public DualSlopeTrickSuper clampSlope(long lo, long hi) {
        if (lo > hi) {
            throw new IllegalArgumentException("lo must be <= hi");
        }
        if (Math.max(st.domainLeft(), lo) > Math.min(st.domainRight(), hi)) {
            throw new IllegalArgumentException(
                    "clamped slope range has empty intersection with conjugate domain");
        }
        st.restrictDomain(lo, hi);
        return this;
    }

    // ============================================================
    // Min-Plus Convolution
    // ============================================================

    /**
     * 他の DualSlopeTrickSuper g との min-plus 畳み込みを行う ((f Box g)(x) = min_y (f(x - y) + g(y)))。
     * 凸共役では (f Box g)* = f* + g* なので st.mergeDestructive(g.st) を呼び出す。
     * 計算量: O(N_g log(N_f + N_g))
     *
     * @param g 畳み込む関数 (破壊される)
     * @return this
     */
    public DualSlopeTrickSuper minPlusConvolveDestructive(DualSlopeTrickSuper g) {
        if (this == g) throw new IllegalArgumentException("Cannot convolve with itself");
        if (Math.max(st.domainLeft(), g.st.domainLeft())
                > Math.min(st.domainRight(), g.st.domainRight())) {
            throw new IllegalArgumentException(
                    "convolution would be unbounded below");
        }
        st.mergeDestructive(g.st);
        return this;
    }

    // ============================================================
    // Marginal Order Statistics & Aggregates
    // ============================================================

    /**
     * 凸共役 f* の変化点を重みだけ重複させて昇順に並べた限界増分列 (m_0, m_1, ...) の k 番目を取得する。
     *
     * <p>
     * 特に f の定義域が有限の整数区間 [L, R] のとき、
     * m_k = f(L + k + 1) - f(L + k)
     * に対応する。
     * </p>
     *
     * <p>計算量: O(log N)</p>
     *
     * @param k 重み順インデックス (0-indexed)
     * @return 傾き (p 座標)
     */
    public long marginal(long k) {
        return st.breakpointByWeight(k);
    }

    /**
     * 限界増分列 (m_0, m_1, ...) の区間 [l, r) の総和 sum_(k=l)^(r-1) m_k を計算する。
     *
     * <p>
     * 特に f の定義域の左端 L が有限で整数区間のとき、
     * sum_(k=l)^(r-1) m_k = f(L + r) - f(L + l)
     * を返す。
     * </p>
     *
     * <p>計算量: O(log N)</p>
     *
     * @param l 区間の左端
     * @param r 区間の右端
     * @return marginal 列 [l, r) の総和
     */
    // 未テスト
    public long sumMarginals(long l, long r) {
        return st.sumBreakpointsByWeight(l, r);
    }


    /**
     * 限界増分列の区間 [l, r) の和に対して、predicate が true となる最大の r を求める。
     *
     * <p>
     * f の定義域の左端を L とし、
     *
     * m_k = f(L + k + 1) - f(L + k)
     *
     * とする。このとき
     *
     * predicate.test(sum_(k=l)^(r-1) m_k)
     *
     * が true となる最大の r を返す。
     * </p>
     *
     * <p>
     * r を増加させたとき、上の predicate の真偽が
     * true から false に高々 1 回だけ変化することを仮定する。
     * また、空区間に対して {@code predicate.test(0)} が true である必要がある。
     * </p>
     *
     * <p>計算量: O(log W log N)</p>
     * @param l 区間の左端
     * @param predicate 限界増分の区間和に対する判定関数
     * @return 条件を満たす最大の r
     */
    public long maxRightMarginals(long l, LongPredicate predicate) {
        return st.maxRightBreakpointsByWeight(l, predicate);
    }

    /**
     * 限界増分列の区間 [l, r) の和が limit 以下となる
     * 最大の r を返す。
     *
     * <p>計算量: O(log N)</p>
     *
     * @param l 区間の左端
     * @param limit 限界増分の和の上限
     * @return 条件を満たす最大の r
     */
    // 未テスト
    public long maxRightMarginalsSumLeq(long l, long limit) {
        return st.maxRightBreakpointsByWeightSumLeq(l, limit);
    }

    // ============================================================
    // Non-destructive Min-Plus Convolution View
    // ============================================================

    /**
     * this と g の min-plus 畳み込みを非破壊 view として返す。
     *
     * <p>
     * view は両方のオブジェクトへの参照だけを保持し、畳み込み自体は構築しない。
     * 生成後に元の関数を更新した場合、query には更新後の状態が反映される。
     * </p>
     *
     * <p>計算量: O(1)</p>
     *
     * @param g 畳み込む関数
     * @return min-plus 畳み込みの live view
     */
    // 未テスト
    public MinPlusConvolutionView minPlusConvolutionView(DualSlopeTrickSuper g) {
        if (g == null) throw new NullPointerException("g");
        return new MinPlusConvolutionView(this, g);
    }

    /**
     * 2つの DualSlopeTrickSuper の min-plus 畳み込みを実体化せず参照する read-only live view。
     */
    public static final class MinPlusConvolutionView {
        private final DualSlopeTrickSuper f;
        private final DualSlopeTrickSuper g;
        private final SlopeTrickSuper.MergedBreakpointsWorkspace workspace =
                new SlopeTrickSuper.MergedBreakpointsWorkspace();

        private MinPlusConvolutionView(DualSlopeTrickSuper f, DualSlopeTrickSuper g) {
            this.f = f;
            this.g = g;
        }

        private long activeDL() {
            return Math.max(f.st.domainLeft(), g.st.domainLeft());
        }

        private long activeDR() {
            return Math.min(f.st.domainRight(), g.st.domainRight());
        }

        private void checkWellDefined() {
            if (activeDL() > activeDR()) {
                throw new IllegalStateException("min-plus convolution is unbounded below");
            }
        }

        private SlopeTrickSuper.MarginalSeq buildRestrictedSeqF() {
            return SlopeTrickSuper.sliceSeq(f.st.getRoot(), activeDL(), activeDR(), workspace);
        }

        private SlopeTrickSuper.MarginalSeq buildRestrictedSeqG() {
            return SlopeTrickSuper.sliceSeq(g.st.getRoot(), activeDL(), activeDR(), workspace);
        }

        private long getBaseSlopeRestricted(SlopeTrickSuper st) {
            long DL = activeDL();
            long baseSlope = st.getBaseSlope();
            if (DL > -SlopeTrickSuper.INF) {
                baseSlope += st.sumDeltaLeq(DL);
            }
            return baseSlope;
        }

        private long getBaseInterceptRestricted(SlopeTrickSuper st) {
            long DL = activeDL();
            long baseIntercept = st.getBaseIntercept();
            if (DL > -SlopeTrickSuper.INF) {
                baseIntercept -= st.sumXDeltaLeq(DL);
            }
            return baseIntercept;
        }

        private long minMinusLinearSum(long p) {
            checkWellDefined();
            long DL = activeDL();
            long DR = activeDR();
            if (p < DL || p > DR) {
                return SlopeTrickSuper.INF;
            }
            long valF = f.st.getAt(p);
            long valG = g.st.getAt(p);
            if (valF == SlopeTrickSuper.INF || valG == SlopeTrickSuper.INF) return SlopeTrickSuper.INF;
            return valF + valG;
        }

        /**
         * 畳み込み後の関数 (f Box g)(x) が有限の値をとる x の下限を取得する。
         *
         * <p>計算量: O(1)</p>
         *
         * @return (f Box g) の定義域の下限
         */
        // 未テスト
        public long domainLeft() {
            checkWellDefined();
            long DL = activeDL();
            if (DL > -SlopeTrickSuper.INF) return -SlopeTrickSuper.INF;
            return getBaseSlopeRestricted(f.st) + getBaseSlopeRestricted(g.st);
        }

        /**
         * 畳み込み後の関数 (f Box g)(x) が有限の値をとる x の上限を取得する。
         *
         * <p>計算量: O(1)</p>
         *
         * @return (f Box g) の定義域の上限
         */
        // 未テスト
        public long domainRight() {
            checkWellDefined();
            long DR = activeDR();
            if (DR < SlopeTrickSuper.INF) return SlopeTrickSuper.INF;
            return f.st.getBaseSlope() + f.st.getTotalWeight() + g.st.getBaseSlope() + g.st.getTotalWeight();
        }

        /**
         * 畳み込み後の最小値 min_x (f Box g)(x) を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @return min (f Box g)(x)
         */
        // 未テスト
        public long getMin() {
            checkWellDefined();
            long val = minMinusLinearSum(0);
            if (val == SlopeTrickSuper.INF || val == -SlopeTrickSuper.INF) return -SlopeTrickSuper.INF;
            return -val;
        }

        /**
         * 畳み込み後の最小値 min_x (f Box g)(x) および最小値をとる区間 [lo, hi] を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @return QueryResult(min, lo, hi)
         */
        // 未テスト
        public SlopeTrickSuper.QueryResult argmin() {
            checkWellDefined();
            long minVal = getMin();
            if (minVal == SlopeTrickSuper.INF || minVal == -SlopeTrickSuper.INF) {
                return new SlopeTrickSuper.QueryResult(minVal, SlopeTrickSuper.INF, -SlopeTrickSuper.INF);
            }
            long DL = activeDL();
            long DR = activeDR();

            long lo;
            if (DL == 0) {
                lo = -SlopeTrickSuper.INF;
            } else {
                long loF = f.st.getSlopeLeft(0);
                long loG = g.st.getSlopeLeft(0);
                lo = (loF == -SlopeTrickSuper.INF || loG == -SlopeTrickSuper.INF) ? -SlopeTrickSuper.INF : loF + loG;
            }

            long hi;
            if (DR == 0) {
                hi = SlopeTrickSuper.INF;
            } else {
                long hiF = f.st.getSlopeRight(0);
                long hiG = g.st.getSlopeRight(0);
                hi = (hiF == SlopeTrickSuper.INF || hiG == SlopeTrickSuper.INF) ? SlopeTrickSuper.INF : hiF + hiG;
            }

            return new SlopeTrickSuper.QueryResult(minVal, lo, hi);
        }

        /**
         * 指定された点 x における関数値 (f Box g)(x) を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param x 評価位置
         * @return (f Box g)(x)
         */
        // 未テスト
        public long getAt(long x) {
            checkWellDefined();
            long dL = domainLeft();
            long dR = domainRight();
            if (x < dL || x > dR) {
                return SlopeTrickSuper.INF;
            }

            long baseSlopeF = getBaseSlopeRestricted(f.st);
            long baseSlopeG = getBaseSlopeRestricted(g.st);
            long targetW = x - baseSlopeF - baseSlopeG;

            workspace.begin();
            SlopeTrickSuper.MarginalSeq sa = buildRestrictedSeqF();
            SlopeTrickSuper.MarginalSeq sb = buildRestrictedSeqG();

            long wa = sa == null ? 0 : sa.weight;
            long wb = sb == null ? 0 : sb.weight;
            long totalW = wa + wb;

            long baseIntF = getBaseInterceptRestricted(f.st);
            long baseIntG = getBaseInterceptRestricted(g.st);

            long DL = activeDL();
            long DR = activeDR();

            if (targetW <= 0) {
                if (DL > -SlopeTrickSuper.INF) {
                    long fp = f.st.getAt(DL);
                    long gp = g.st.getAt(DL);
                    return DL * x - fp - gp;
                } else {
                    return -(baseIntF + baseIntG);
                }
            }

            if (targetW >= totalW) {
                if (DR < SlopeTrickSuper.INF) {
                    long fp = f.st.getAt(DR);
                    long gp = g.st.getAt(DR);
                    return DR * x - fp - gp;
                } else {
                    long sumXDeltaF = sa == null ? 0 : sa.sum;
                    long sumXDeltaG = sb == null ? 0 : sb.sum;
                    return (sumXDeltaF + sumXDeltaG) - (baseIntF + baseIntG);
                }
            }

            long pOpt = marginal(targetW - 1);
            long fp = f.st.getAt(pOpt);
            long gp = g.st.getAt(pOpt);
            return pOpt * x - fp - gp;
        }

        /**
         * 指定された点 x における左微分 (f Box g)'_-(x) を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param x 評価位置
         * @return 左傾き
         */
        // 未テスト
        public long getSlopeLeft(long x) {
            checkWellDefined();
            long dL = domainLeft();
            long dR = domainRight();
            if (x < dL) return -SlopeTrickSuper.INF;
            if (x > dR) return SlopeTrickSuper.INF;

            long targetW = x - getBaseSlopeRestricted(f.st) - getBaseSlopeRestricted(g.st);

            workspace.begin();
            SlopeTrickSuper.MarginalSeq sa = buildRestrictedSeqF();
            SlopeTrickSuper.MarginalSeq sb = buildRestrictedSeqG();

            long wa = sa == null ? 0 : sa.weight;
            long wb = sb == null ? 0 : sb.weight;
            long totalW = wa + wb;

            if (targetW <= 0) {
                return activeDL();
            } else if (targetW > totalW) {
                return activeDR();
            } else {
                return marginal(targetW - 1);
            }
        }

        /**
         * 指定された点 x における右微分 (f Box g)'_+(x) を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param x 評価位置
         * @return 右傾き
         */
        // 未テスト
        public long getSlopeRight(long x) {
            checkWellDefined();
            long dL = domainLeft();
            long dR = domainRight();
            if (x < dL) return -SlopeTrickSuper.INF;
            if (x > dR) return SlopeTrickSuper.INF;

            long targetW = x - getBaseSlopeRestricted(f.st) - getBaseSlopeRestricted(g.st);

            workspace.begin();
            SlopeTrickSuper.MarginalSeq sa = buildRestrictedSeqF();
            SlopeTrickSuper.MarginalSeq sb = buildRestrictedSeqG();

            long wa = sa == null ? 0 : sa.weight;
            long wb = sb == null ? 0 : sb.weight;
            long totalW = wa + wb;

            if (targetW < 0) {
                return activeDL();
            } else if (targetW >= totalW) {
                return activeDR();
            } else {
                return marginal(targetW);
            }
        }

        /**
         * 指定された点 x におけるサブグラディエント（劣微分）を取得する。
         *
         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param x 評価位置
         * @return サブグラディエント
         */
        // 未テスト
        public long getSubgradient(long x) {
            long sl = getSlopeLeft(x);
            long sr = getSlopeRight(x);
            if (sl <= 0 && sr >= 0) return 0;
            if (sr < 0) return sr;
            return sl;
        }

        /**
         * 畳み込み後の関数 (f Box g) の限界増分列 (m_0, m_1, ...) の k 番目 (0-indexed) を取得する。
         *
         * <p>
         * これは f と g の restricted 限界増分列の sorted multiset union の k 番目に対応する。
         * </p>

         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param k 重み順インデックス (0-indexed)
         * @return 傾き (p 座標)
         */
        // 未テスト
        public long marginal(long k) {
            checkWellDefined();
            workspace.begin();
            SlopeTrickSuper.MarginalSeq sa = buildRestrictedSeqF();
            SlopeTrickSuper.MarginalSeq sb = buildRestrictedSeqG();

            long wa = (sa == null) ? 0 : sa.weight;
            long wb = (sb == null) ? 0 : sb.weight;
            long total = wa + wb;

            if (k < 0 || k >= total) {
                throw new IndexOutOfBoundsException("k out of bounds: " + k + " (total weight: " + total + ")");
            }

            SlopeTrickSuper.PartitionResult part = workspace.partition;
            SlopeTrickSuper.partitionFirstK(sa, sb, k + 1, part, workspace);

            long DL = activeDL();
            long DR = activeDR();

            long valA = -SlopeTrickSuper.INF;
            if (part.a > 0) {
                long offsetA = (DL == -SlopeTrickSuper.INF) ? 0 : f.st.sumDeltaLeq(DL);
                valA = f.marginal(offsetA + part.a - 1);
            }

            long valB = -SlopeTrickSuper.INF;
            if (part.b > 0) {
                long offsetB = (DL == -SlopeTrickSuper.INF) ? 0 : g.st.sumDeltaLeq(DL);
                valB = g.marginal(offsetB + part.b - 1);
            }

            return Math.max(valA, valB);
        }

        /**
         * 畳み込み後の marginal 列、すなわち f と g の restricted marginal 列の
         * sorted multiset union の先頭 k 個について、
         * f 由来の個数と g 由来の個数を求める。
         *
         * <p>
         * out.a + out.b = k であり、
         * out.a が f 由来、out.b が g 由来の個数を表す。
         * </p>

         * <p>計算量: O(log(N_f + N_g))</p>
         *
         * @param k 先頭から取る marginal の個数
         * @param out 結果格納先
         */
        // 未テスト
        private void partitionFirstK(long k, SlopeTrickSuper.PartitionResult out) {
            if (k <= 0) {
                out.a = 0;
                out.b = 0;
                return;
            }
            long DL = activeDL();
            long DR = activeDR();

            long offsetA = (DL == -SlopeTrickSuper.INF) ? 0 : f.st.sumDeltaLeq(DL);
            long wa = f.st.sumDeltaLeq(DR - 1) - offsetA;
            if (wa < 0) wa = 0;

            long offsetB = (DL == -SlopeTrickSuper.INF) ? 0 : g.st.sumDeltaLeq(DL);
            long wb = g.st.sumDeltaLeq(DR - 1) - offsetB;
            if (wb < 0) wb = 0;

            long total = wa + wb;
            if (k >= total) {
                out.a = wa;
                out.b = wb;
                return;
            }

            long p = marginal(k - 1);

            long wLtA = Math.max(0, Math.min(wa, f.st.sumDeltaLeq(p - 1) - offsetA));
            long wLeA = Math.max(0, Math.min(wa, f.st.sumDeltaLeq(p) - offsetA));
            long cntA_eq = wLeA - wLtA;

            long wLtB = Math.max(0, Math.min(wb, g.st.sumDeltaLeq(p - 1) - offsetB));

            long rem = k - wLtA - wLtB;
            long takeA = Math.max(0, Math.min(rem, cntA_eq));

            out.a = wLtA + takeA;
            out.b = k - out.a;
        }

        /**
         * 畳み込み後の限界増分列の区間 [l, r) の総和 sum_(k=l)^(r-1) m_k を計算する。
         *
         * <p>
         * h = f Box g としたとき、h の定義域の左端 L が有限で整数区間の場合は
         * sum_(k=l)^(r-1) m_k = h(L + r) - h(L + l)
         * に一致する。
         * </p>
         *
         * <p>期待計算量: O(log(N_f + N_g))</p>
         *
         * @param l 区間の左端
         * @param r 区間の右端
         * @return marginal 列 [l, r) の総和
         */
        // 未テスト
        public long sumMarginals(long l, long r) {
            checkWellDefined();
            long DL = activeDL();
            long DR = activeDR();

            long offsetA = (DL == -SlopeTrickSuper.INF) ? 0 : f.st.sumDeltaLeq(DL);
            long wa = f.st.sumDeltaLeq(DR - 1) - offsetA;
            if (wa < 0) wa = 0;

            long offsetB = (DL == -SlopeTrickSuper.INF) ? 0 : g.st.sumDeltaLeq(DL);
            long wb = g.st.sumDeltaLeq(DR - 1) - offsetB;
            if (wb < 0) wb = 0;

            long total = wa + wb;

            if (l < 0 || l > r || r > total) {
                throw new IndexOutOfBoundsException(
                        "weight range out of bounds: [" + l + ", " + r
                        + "), total weight = " + total);
            }
            if (l == r) {
                return 0;
            }

            SlopeTrickSuper.PartitionResult partL = new SlopeTrickSuper.PartitionResult();
            partitionFirstK(l, partL);

            SlopeTrickSuper.PartitionResult partR = new SlopeTrickSuper.PartitionResult();
            partitionFirstK(r, partR);

            long sumF = f.sumMarginals(offsetA + partL.a, offsetA + partR.a);
            long sumG = g.sumMarginals(offsetB + partL.b, offsetB + partR.b);

            return sumF + sumG;
        }


        /**
         * this と g の min-plus 畳み込み
         *
         * h(x) = min_y { f(x - y) + g(y) }
         *
         * を考える。
         *
         * <p>
         * h の定義域の左端を L とし、その限界増分を
         *
         * m_k = h(L + k + 1) - h(L + k)
         *
         * とする。このとき
         *
         * sum_(k=l)^(r-1) m_k
         * = h(L + r) - h(L + l)
         *
         * なので、このメソッドは
         *
         * h(L + r) - h(L + l) <= limit
         *
         * を満たす最大の r を返す。
         * </p>
         *
         * <p>
         * h の marginal 列は f と g の marginal 列の
         * sorted multiset union になることを用い、
         * 畳み込み h 自体を構築せずに2本の Treap を同時に探索する。
         * </p>
         *
         * <p>
         * {@code limit >= 0} を要求する。
         * marginal 自体は負でもよい。
         * </p>
         *
         * <p>期待計算量: O(log(N_f + N_g))</p>
         *
         * @param l 基準とする点 L + l のオフセット
         * @param limit h(L + r) - h(L + l) の上限
         * @return h(L + r) - h(L + l) <= limit を満たす最大の r
         */
        public long maxRightMarginals(long l, long limit) {
            checkWellDefined();
            workspace.begin();
            SlopeTrickSuper.MarginalSeq sa = buildRestrictedSeqF();
            SlopeTrickSuper.MarginalSeq sb = buildRestrictedSeqG();
            return SlopeTrickSuper.maxRightMergedBreakpointsByWeightSumLeq(
                    sa, sb, l, limit, workspace);
        }

        /**
         * maxRightMarginals(long, long) と同じ。
         * 条件が区間和 <= limit であることを明示した別名。
         *
         * <p>期待計算量: O(log(N_f + N_g))</p>
         *
         * @param l 区間の左端（0-indexed）
         * @param limit marginal 和の上限
         * @return 条件を満たす最大の r
         */
        // 未テスト
        public long maxRightMarginalsSumLeq(long l, long limit) {
            return maxRightMarginals(l, limit);
        }
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
        List<Long> keyX = st.getPrimalBreakpointsForDual();
        keyX.add(0L);

        List<Long> validX = keyX.stream()
                .filter(x -> getAt(x) != SlopeTrickSuper.INF)
                .collect(Collectors.toList());

        long minX, maxX;
        if (validX.isEmpty()) {
            minX = -10;
            maxX = 10;
        } else {
            long low = Collections.min(validX);
            long high = Collections.max(validX);
            if (low == high) {
                minX = low - 10;
                maxX = low + 10;
            } else {
                long diff = high - low;
                minX = low - diff / 10 - 1;
                maxX = high + diff / 10 + 1;
            }
        }

        List<Long> xData = new ArrayList<>();
        xData.add(minX);
        for (long x : validX) {
            if (x > minX && x < maxX) {
                xData.add(x);
            }
        }
        xData.add(maxX);

        List<Double> distinctX = xData.stream()
                .distinct()
                .sorted()
                .map(Long::doubleValue)
                .filter(x -> getAt(Math.round(x)) != SlopeTrickSuper.INF)
                .collect(Collectors.toList());

        if (distinctX.isEmpty()) {
            distinctX.add(0.0);
        }

        List<Double> distinctY = distinctX.stream()
                .map(x -> (double) getAt(Math.round(x)))
                .collect(Collectors.toList());

        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("DualSlopeTrickSuper-" + drawCount.incrementAndGet())
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
