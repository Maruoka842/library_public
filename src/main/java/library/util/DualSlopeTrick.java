package library.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

import com.lowagie.text.List;

/**
 * Dual Slope Trick: 凸共役を利用した凸関数の操作。
 *
 * 凸関数 f(x) に対し、その凸共役 f*(p) = sup_x (px - f(x)) を SlopeTrick で管理する。
 * f(x) は、傾きが整数の範囲で変化し、変化点の座標も整数であるような区分線形凸関数である。
 * 未テスト
 */
public class DualSlopeTrick {
    private final SlopeTrick st;

    /**
     * f(x) = 0 (x = 0), infinity (x != 0) で初期化する。
     */
    public DualSlopeTrick() {
        this.st = new SlopeTrick();
    }

    /**
     * {@code f(x) = 0 (x = 0), +infinity (x != 0)} に戻す。
     *
     * <p>計算量: O(1)</p>
     *
     * @return this
     */
    // 未テスト
    public DualSlopeTrick clear() {
        st.clear();
        return this;
    }

    /**
     * {@code f(x)} が有限となる x の最小値を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * @return f の定義域の左端
     */
    // 未テスト
    public long domainLeft() {
        return -st.sizeL();
    }

    /**
     * {@code f(x)} が有限となる x の最大値を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * @return f の定義域の右端
     */
    // 未テスト
    public long domainRight() {
        return st.sizeR();
    }

    /**
     * {@code f(0)} と、0 における劣勾配の区間を表す。
     *
     * {@code [subgradientLo, subgradientHi] = partial f(0)}。
     */
    public record AtZeroResult(
            long value,
            long subgradientLo,
            long subgradientHi) {
    }

    /**
     * {@code f(0)} と 0 における劣勾配の区間を取得する。
     *
     * <p>計算量: O(1)</p>
     *
     * @return f(0) と partial f(0)
     */
    // 未テスト
    public AtZeroResult getAtZeroResult() {
        SlopeTrick.QueryResult q = st.getMin();
        return new AtZeroResult(
                -q.min(),
                q.lo(),
                q.hi());
    }

    /**
     * f(0) を取得する。
     * @return f(0)
     */
    public long getAtZero() {
        return -st.getMin().min();
    }

    /**
     * f(x) += b を行う。
     * @param b 加算する定数
     * @return this
     */
    public DualSlopeTrick addConst(long b) {
        st.addConst(-b);
        return this;
    }

    /**
     * {@code f(x) <- f(x - d)} を行う。
     *
     * <p>
     * 操作後も {@code f(0)} が有限、すなわち操作前の
     * {@code f(-d)} が有限であることを要求する。
     * </p>
     *
     * <p>計算量: O(|d| log N)</p>
     *
     * @param d 移動量
     * @return this
     * @throws IllegalArgumentException 操作後に {@code f(0) = +infinity} となる場合
     */
    // 未テスト
    public DualSlopeTrick shift(long d) {
        st.addLinear(d);
        return this;
    }

    /**
     * f(x) += ax + b を行う。
     * @param a 傾き
     * @param b 切片
     * @return this
     */
    public DualSlopeTrick addLinear(long a, long b) {
        st.translate(a).addConst(-b);
        return this;
    }

    /**
     * f(x) += max(c(x - a), 0) を行う。
     * @param c 傾き
     * @param a 変化点の座標
     * @return this
     */
    public DualSlopeTrick addLinearOrZero(long c, long a) {
        shift(-a);
        if (c > 0) {
            st.moveRightCurve(c);
        } else if (c < 0) {
            st.moveLeftCurve(-c);
        }
        return shift(a);
    }

    /**
     * {@code f(x) <- min_(a <= d <= b) f(x - d)} を行う。
     *
     * <p>計算量: O((|a| + |b - a|) log N)</p>
     *
     * @param a 移動量の下限
     * @param b 移動量の上限
     * @return this
     * @throws IllegalArgumentException {@code a > b} のとき
     */
    // 未テスト
    public DualSlopeTrick slideMin(long a, long b) {
        if (a > b)
            throw new IllegalArgumentException("a must be <= b");

        long diff = b - a;

        for (long t = 0; t < diff; t++)
            st.addRelu(0);

        st.addLinear(a);

        return this;
    }

    /**
     * {@code f(x) <- min_(a <= d <= b) (f(x - d) + c * d)} を行う。
     *
     * <p>
     * 線形コスト {@code c * d} を持つ区間 {@code [a, b]} との
     * min-plus convolution を行う操作である。
     * </p>
     *
     * <p>
     * 特に、
     * <ul>
     * <li>{@code slideMinLinear(0, 1, c)} は
     *     {@code g(x) = min(f(x), f(x - 1) + c)}</li>
     * <li>{@code slideMinLinear(-1, 0, c)} は
     *     {@code g(x) = min(f(x), f(x + 1) - c)}</li>
     * </ul>
     * に対応する。
     * </p>
     *
     * <p>計算量: O((|a| + |b - a|) log N)</p>
     *
     * @param a 移動量の下限
     * @param b 移動量の上限
     * @param c 移動量 1 あたりのコスト
     * @return this
     * @throws IllegalArgumentException {@code a > b} のとき、
     *         または結果の {@code f(0)} が有限でなくなるとき
     */
    // 未テスト
    public DualSlopeTrick slideMinLinear(long a, long b, long c) {
        if (a > b)
            throw new IllegalArgumentException("a must be <= b");

        st.translate(-c);
        slideMin(a, b);
        st.translate(c);

        return this;
    }

    /**
     * {@code x < 0} に対して {@code f(x) = +infinity} とする。
     *
     * <p>
     * すなわち f の定義域を非負側に制限する。
     * </p>
     *
     * <p>計算量: O(N)</p>
     *
     * @return this
     */
    // 未テスト
    public DualSlopeTrick restrictToNonNegative() {
        st.moveLeftCurve();
        return this;
    }

    /**
     * {@code x > 0} に対して {@code f(x) = +infinity} とする。
     *
     * <p>
     * すなわち f の定義域を非正側に制限する。
     * </p>
     *
     * <p>計算量: O(N)</p>
     *
     * @return this
     */
    // 未テスト
    public DualSlopeTrick restrictToNonPositive() {
        st.moveRightCurve();
        return this;
    }

    /**
     * {@code f <- f □ g} を行う。{@code g} は破壊される。
     *
     * <p>
     * ここで {@code □} は min-plus convolution であり、
     * {@code (f □ g)(x) = min_y (f(x - y) + g(y))} である。
     * </p>
     *
     * <p>計算量: O(N log N)</p>
     *
     * @param g min-plus convolution する関数。呼び出し後は破壊される
     * @return this
     * @throws IllegalArgumentException 自分自身を指定したとき
     */
    // 未テスト
    public DualSlopeTrick minPlusConvolveDestructive(DualSlopeTrick g) {
        if (this == g)
            throw new IllegalArgumentException("cannot convolve itself");

        st.mergeDestructive(g.st);
        return this;
    }

    
    /**
     * {@code f} から傾きが {@code [l, r)} に属する部分を切り出し、
     * {@code f(0)} を保つように平行移動する。
     *
     * <p>
     * 切り出した部分の定義域を {@code [a, b]} とし、
     * {@code c = min(max(0, a), b)} とすると、操作後の関数 {@code g} は
     * <pre>
     * g(x) = f(x + c) - f(c) + f(0)    (a - c <= x <= b - c)
     * </pre>
     * である。
     * </p>
     *
     * @param l 傾きの下限
     * @param r 傾きの上限（exclusive）
     * @return this
     */
    // 未テスト
    public DualSlopeTrick discardSlopesOutside(long l, long r) {
        st.discardOutside(l, r);
        return this;
    }

    /**
     * {@code min_x (f(x) - p * x)} を返す。
     *
     * <p>
     * 凸共役の定義より、この値は {@code -f*(p)} に等しい。
     * </p>
     *
     * <p>計算量: O(N)</p>
     *
     * @param p 線形項の係数
     * @return min_x (f(x) - p * x)
     */
    // 未テスト
    public long getMinMinusLinear(long p) {
        return -st.getAt(p);
    }

    /**
     * {@code min_x (f(x) - p * x)} を返す。
     * 呼び出し後、インスタンスの状態は破壊される。
     *
     * <p>計算量: O(N log N)</p>
     *
     * @param p 線形項の係数
     * @return min_x (f(x) - p * x)
     */
    // 未テスト
    public long getMinMinusLinearDestructive(long p) {
        return -st.getDestructive(p);
    }

    /**
     * f(x) の最小値を取得する。
     * @return min f(x)
     */
    public long getMin() {
        return getMinMinusLinear(0);
    }
    
    private static final AtomicInteger drawCount = new AtomicInteger();

    private XYChart toChart() {
    	long[] y = st.conjugateValues();
    	long left = domainLeft();

    	ArrayList<Long> xData = new ArrayList<>();
    	ArrayList<Long> yData = new ArrayList<>();

    	for (int i = 0; i < y.length; i++) {
    		xData.add(left + i);
    		yData.add(y[i]);
    	}

    	XYChart chart = new XYChartBuilder()
    			.width(800)
    			.height(600)
    			.title("DualSlopeTrick-" + drawCount.incrementAndGet())
    			.xAxisTitle("x")
    			.yAxisTitle("f(x)")
    			.build();

    	chart.addSeries("f(x)", xData, yData)
    			.setMarker(SeriesMarkers.CIRCLE);

    	return chart;
    }

    public void draw() {
    	new SwingWrapper<>(toChart()).displayChart();
    }
}
