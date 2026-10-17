package library.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.markers.SeriesMarkers;

/**
 * Slope trick: 凸折れ線関数に対する高速な操作を行うライブラリ。
 *
 * 以下の形の凸関数 f(x) を管理する:
 * f(x) = min_f + Σ max(0, a_i - x) + Σ max(0, x - b_i)
 * ここで a_i は左側の傾きの変化点 (L)、b_i は右側の傾きの変化点 (R) である。
 *
 * 復元を行う場合は {@link #recordState(int)} で復元したい時点を記録し、
 * 最後に {@link #restoreMinimizer()} を呼ぶ。
 *
 * recordState() が一度も呼ばれない場合、復元用ノードは一切生成されない。
 *
 * 実装の参考:
 * - https://maspypy.com/slope-trick-1-%E8%A7%A3%E8%AA%AC%E7%B7%A8
 * - https://ei1333.github.io/library/structure/others/slope-trick.cpp
 */
public class SlopeTrick {
	public static final long INF = Long.MAX_VALUE / 3;

	private long minF;
	private long displacementL, displacementR;
	private PriorityQueue<Long> L; // max-heap
	private PriorityQueue<Long> R; // min-heap

	/*
	 * 復元履歴。
	 *
	 * null の間は復元を使用していない。
	 * 最初の recordState() が呼ばれた時点から履歴を保存する。
	 */
	private RestoreNode restoreRoot;

	/*
	 * 復元結果に必要な配列長。
	 *
	 * recordState(id) ごとに max(restoreResultSize, id + 1) を取る。
	 */
	private int restoreResultSize;

	// ============================================================
	// restoration
	// ============================================================

	private interface RestoreNode {
	}

	/**
	 * この時点での x を result[id] に記録する。
	 */
	private static class RecordRestoreNode implements RestoreNode {
		final int id;
		final RestoreNode prev;

		RecordRestoreNode(int id, RestoreNode prev) {
			this.id = id;
			this.prev = prev;
		}
	}

	/**
	 * g(x) = min_(0 <= y <= w) f(x + y)
	 * の復元情報。
	 */
	private static class MoveLeftRestoreNode implements RestoreNode {
		final long lo;
		final long w;
		final boolean bounded;
		final RestoreNode prev;

		MoveLeftRestoreNode(long lo, long w, boolean bounded, RestoreNode prev) {
			this.lo = lo;
			this.w = w;
			this.bounded = bounded;
			this.prev = prev;
		}
	}

	/**
	 * g(x) = min_(0 <= y <= w) f(x - y)
	 * の復元情報。
	 */
	private static class MoveRightRestoreNode implements RestoreNode {
		final long hi;
		final long w;
		final boolean bounded;
		final RestoreNode prev;

		MoveRightRestoreNode(long hi, long w, boolean bounded, RestoreNode prev) {
			this.hi = hi;
			this.w = w;
			this.bounded = bounded;
			this.prev = prev;
		}
	}

	/**
	 * g(x) = f(x - dx)
	 * の復元情報。
	 */
	private static class TranslateRestoreNode implements RestoreNode {
		final long dx;
		final RestoreNode prev;

		TranslateRestoreNode(long dx, RestoreNode prev) {
			this.dx = dx;
			this.prev = prev;
		}
	}

	/**
	 * f(x) + g(x) の復元情報。
	 *
	 * 同じ x を両方の部分問題に渡して復元する。
	 */
	private static class MergeRestoreNode implements RestoreNode {
		final RestoreNode a;
		final RestoreNode b;

		MergeRestoreNode(RestoreNode a, RestoreNode b) {
			this.a = a;
			this.b = b;
		}
	}

	private static class RestoreFrame {
		final RestoreNode node;
		final long x;

		RestoreFrame(RestoreNode node, long x) {
			this.node = node;
			this.x = x;
		}
	}

	public SlopeTrick() {
		this.minF = 0;
		this.displacementL = 0;
		this.displacementR = 0;
		this.L = new PriorityQueue<>(Collections.reverseOrder());
		this.R = new PriorityQueue<>();
		this.restoreRoot = null;
		this.restoreResultSize = 0;
	}

	/**
	 * 状態をクリアする。
	 * O(1)
	 * // 未テスト
	 */
	public void clear() {
		this.minF = 0;
		this.displacementL = 0;
		this.displacementR = 0;
		this.L.clear();
		this.R.clear();
		this.restoreRoot = null;
		this.restoreResultSize = 0;
	}

	private void pushR(long a) {
		R.add(a - displacementR);
	}

	private long topR() {
		return R.isEmpty() ? INF : R.peek() + displacementR;
	}

	private long popR() {
		long ret = topR();
		if (!R.isEmpty())
			R.poll();
		return ret;
	}

	private void pushL(long a) {
		L.add(a + displacementL);
	}

	private long topL() {
		return L.isEmpty() ? -INF : L.peek() - displacementL;
	}

	private long popL() {
		long ret = topL();
		if (!L.isEmpty())
			L.poll();
		return ret;
	}

	public int sizeL() {
		return L.size();
	}

	public int sizeR() {
		return R.size();
	}

	/**
	 * argmin f(x), min f(x) を取得する。
	 * [lo, hi] が f(x) が最小値をとる区間である。
	 */
	public record QueryResult(long min, long lo, long hi) {
	}

	public QueryResult getMin() {
		return new QueryResult(minF, topL(), topR());
	}

	/**
	 * 現在の状態変数 x を、復元時に result[id] として記録する。
	 *
	 * <p>
	 * 最初にこのメソッドが呼ばれた時点から復元履歴の記録を開始する。
	 * このメソッドが一度も呼ばれない場合、復元用ノードは一切生成されない。
	 *
	 * <p>
	 * id は、merge されうる全 SlopeTrick インスタンスを通して
	 * 一意であることを仮定する。
	 *
	 * @param id 復元結果を格納する添字
	 * @return this
	 */
	public SlopeTrick recordState(int id) {
		if (id < 0)
			throw new IllegalArgumentException("id must be non-negative");
		if (id == Integer.MAX_VALUE)
			throw new IllegalArgumentException("id is too large");

		restoreResultSize = Math.max(restoreResultSize, id + 1);
		restoreRoot = new RecordRestoreNode(id, restoreRoot);
		return this;
	}

	/**
	 * f(x) += b
	 */
	public SlopeTrick addConst(long b) {
		minF += b;
		return this;
	}

	/**
	 * f(x) += max(x - a, 0)  _/
	 */
	public SlopeTrick addRelu(long a) {
		minF += Math.max(0L, topL() - a);
		pushL(a);
		pushR(popL());
		return this;
	}

	/**
	 * f(x) += max(a - x, 0)  _
	 */
	public SlopeTrick addIrelu(long a) {
		minF += Math.max(0L, a - topR());
		pushR(a);
		pushL(popR());
		return this;
	}

	/**
	 * f(x) += |x - a|
	 */
	public SlopeTrick addAbs(long a) {
		return addRelu(a).addIrelu(a);
	}

	/**
	 * f(x) += a * x を行う。
	 *
	 * <p>
	 * 操作後の関数が下に有界であることを要求する。
	 * {@code a > 0} のときは L から R へ {@code a} 個、
	 * {@code a < 0} のときは R から L へ {@code -a} 個の
	 * 変化点を移す。
	 * </p>
	 *
	 * <p>計算量: O(|a| log N)</p>
	 *
	 * @param a 加算する線形関数の傾き
	 * @return this
	 * @throws IllegalArgumentException 操作後の関数が下に非有界になる場合
	 */
	// 未テスト
	public SlopeTrick addLinear(long a) {
		if (a > L.size() || a < -(long) R.size())
			throw new IllegalArgumentException("function becomes unbounded below");

		while (a > 0) {
			long x = popL();

			/*
			 * max(x - p, 0) + p
			 * = x + max(p - x, 0)
			 */
			minF += x;
			pushR(x);
			a--;
		}

		while (a < 0) {
			long x = popR();

			/*
			 * max(p - x, 0) - p
			 * = -x + max(x - p, 0)
			 */
			minF -= x;
			pushL(x);
			a++;
		}

		return this;
	}

	/**
	 * f(x) <- min_(0 <= y <= w) f(x + y)
	 * 関数の左側が w だけ左に伸びる。
	 */
	public SlopeTrick moveLeftCurve(long w) {
		if (w < 0)
			throw new IllegalArgumentException("w must be non-negative");

		if (restoreRoot != null) {
			restoreRoot = new MoveLeftRestoreNode(
					topL(),
					w,
					true,
					restoreRoot);
		}

		displacementL += w;
		return this;
	}

	/**
	 * f(x) <- min_(0 <= y) f(x + y)
	 * 関数の左側をすべて捨てる。
	 */
	public SlopeTrick moveLeftCurve() {
		if (restoreRoot != null) {
			restoreRoot = new MoveLeftRestoreNode(
					topL(),
					0,
					false,
					restoreRoot);
		}

		L.clear();
		displacementL = 0;
		return this;
	}

	/**
	 * f(x) <- min_(0 <= y <= w) f(x - y)
	 * 関数の右側が w だけ右に伸びる。
	 */
	public SlopeTrick moveRightCurve(long w) {
		if (w < 0)
			throw new IllegalArgumentException("w must be non-negative");

		if (restoreRoot != null) {
			restoreRoot = new MoveRightRestoreNode(
					topR(),
					w,
					true,
					restoreRoot);
		}

		displacementR += w;
		return this;
	}

	/**
	 * f(x) <- min_(0 <= y) f(x - y)
	 * 関数の右側をすべて捨てる。
	 */
	public SlopeTrick moveRightCurve() {
		if (restoreRoot != null) {
			restoreRoot = new MoveRightRestoreNode(
					topR(),
					0,
					false,
					restoreRoot);
		}

		R.clear();
		displacementR = 0;
		return this;
	}

	/**
	 * f(x) <- f(x - dx)
	 * 関数全体を dx だけ右に平行移動する。
	 *
	 * <pre>
	 * before:  \/
	 * after:      \/
	 * </pre>
	 */
	public SlopeTrick translate(long dx) {
		if (restoreRoot != null) {
			restoreRoot = new TranslateRestoreNode(
					dx,
					restoreRoot);
		}

		displacementL -= dx;
		displacementR += dx;
		return this;
	}

	/**
	 * f(x) の値を求める。
	 */
	public long getAt(long x) {
		long ret = minF;
		for (long val : L)
			ret += Math.max(0L, (val - displacementL) - x);
		for (long val : R)
			ret += Math.max(0L, x - (val + displacementR));
		return ret;
	}

	/**
	 * f(x) の値を求める。呼び出し後、インスタンスの状態は破壊される。
	 */
	public long getDestructive(long x) {
		long ret = getMin().min;
		while (!L.isEmpty())
			ret += Math.max(0L, popL() - x);
		while (!R.isEmpty())
			ret += Math.max(0L, x - popR());
		return ret;
	}

	/**
	 * f(x) += g(x) を行う。g は破壊される。
	 *
	 * <p>
	 * 復元を使用する場合、this と g の recordState(id) で使用した id は
	 * 互いに衝突しないことを仮定する。
	 */
	public SlopeTrick mergeDestructive(SlopeTrick g) {
		if (this == g)
			throw new IllegalArgumentException("cannot merge itself");

		if (sizeL() + sizeR() < g.sizeL() + g.sizeR()) {
			long tmpMinF = minF;
			minF = g.minF;
			g.minF = tmpMinF;

			long tmpDL = displacementL;
			displacementL = g.displacementL;
			g.displacementL = tmpDL;

			long tmpDR = displacementR;
			displacementR = g.displacementR;
			g.displacementR = tmpDR;

			PriorityQueue<Long> tmpL = L;
			L = g.L;
			g.L = tmpL;

			PriorityQueue<Long> tmpR = R;
			R = g.R;
			g.R = tmpR;

			RestoreNode tmpRestore = restoreRoot;
			restoreRoot = g.restoreRoot;
			g.restoreRoot = tmpRestore;

			int tmpRestoreResultSize = restoreResultSize;
			restoreResultSize = g.restoreResultSize;
			g.restoreResultSize = tmpRestoreResultSize;
		}

		RestoreNode restoreA = restoreRoot;
		RestoreNode restoreB = g.restoreRoot;
		int mergedRestoreResultSize = Math.max(restoreResultSize, g.restoreResultSize);

		minF += g.getMin().min;

		while (!g.L.isEmpty())
			addIrelu(g.popL());
		while (!g.R.isEmpty())
			addRelu(g.popR());

		/*
		 * h(x) = f(x) + g(x) では、固定された x に対して
		 * f 側と g 側を同じ x から独立に復元できる。
		 *
		 * 一方しか復元履歴を持たない場合は MergeRestoreNode を作らない。
		 */
		if (restoreA == null) {
			restoreRoot = restoreB;
		} else if (restoreB == null) {
			restoreRoot = restoreA;
		} else {
			restoreRoot = new MergeRestoreNode(restoreA, restoreB);
		}

		restoreResultSize = mergedRestoreResultSize;

		/*
		 * destructive merge 後の g の復元情報への参照を切る。
		 */
		g.restoreRoot = null;
		g.restoreResultSize = 0;

		return this;
	}

	private static long clamp(long x, long l, long r) {
		return Math.max(l, Math.min(r, x));
	}

	/**
	 * 現在の状態を x としたとき、それを実現する過去の状態を一つ復元する。
	 *
	 * <p>
	 * recordState(id) を呼んだ時点での状態変数が result[id] に格納される。
	 *
	 * <p>
	 * x が現在の f の最小点なら、最適解を一つ復元することになる。
	 *
	 * @param x 現在の状態変数
	 * @param result 復元結果の格納先
	 */
	public void restore(long x, long[] result) {
		if (result.length < restoreResultSize)
			throw new IllegalArgumentException(
					"result is too short: required=" + restoreResultSize + ", actual=" + result.length);

		if (restoreRoot == null)
			return;

		ArrayDeque<RestoreFrame> stack = new ArrayDeque<>();
		stack.push(new RestoreFrame(restoreRoot, x));

		while (!stack.isEmpty()) {
			RestoreFrame frame = stack.pop();

			RestoreNode node = frame.node;
			long cur = frame.x;

			if (node instanceof RecordRestoreNode) {
				RecordRestoreNode p = (RecordRestoreNode) node;

				result[p.id] = cur;

				if (p.prev != null)
					stack.push(new RestoreFrame(p.prev, cur));

			} else if (node instanceof MoveLeftRestoreNode) {
				MoveLeftRestoreNode p = (MoveLeftRestoreNode) node;

				long prev;
				if (p.bounded) {
					/*
					 * g(x) = min_{z in [x, x+w]} f(z)
					 *
					 * f の argmin の左端 lo を [x, x+w] に射影する。
					 */
					prev = clamp(p.lo, cur, cur + p.w);
				} else {
					/*
					 * g(x) = min_{z >= x} f(z)
					 */
					prev = Math.max(cur, p.lo);
				}

				if (p.prev != null)
					stack.push(new RestoreFrame(p.prev, prev));

			} else if (node instanceof MoveRightRestoreNode) {
				MoveRightRestoreNode p = (MoveRightRestoreNode) node;

				long prev;
				if (p.bounded) {
					/*
					 * g(x) = min_{z in [x-w, x]} f(z)
					 *
					 * f の argmin の右端 hi を [x-w, x] に射影する。
					 */
					prev = clamp(p.hi, cur - p.w, cur);
				} else {
					/*
					 * g(x) = min_{z <= x} f(z)
					 */
					prev = Math.min(cur, p.hi);
				}

				if (p.prev != null)
					stack.push(new RestoreFrame(p.prev, prev));

			} else if (node instanceof TranslateRestoreNode) {
				TranslateRestoreNode p = (TranslateRestoreNode) node;

				/*
				 * g(x) = f(x - dx)
				 */
				long prev = cur - p.dx;

				if (p.prev != null)
					stack.push(new RestoreFrame(p.prev, prev));

			} else if (node instanceof MergeRestoreNode) {
				MergeRestoreNode p = (MergeRestoreNode) node;

				/*
				 * h(x) = f(x) + g(x)
				 *
				 * 両方の部分問題を同じ x から復元する。
				 */
				stack.push(new RestoreFrame(p.a, cur));
				stack.push(new RestoreFrame(p.b, cur));

			} else {
				throw new AssertionError();
			}
		}
	}

	/**
	 * 現在の状態を x としたときの復元結果を返す。
	 *
	 * <p>
	 * 配列長は recordState(id) で使用された最大 id + 1 になる。
	 *
	 * @param x 現在の状態変数
	 * @return recordState(id) 時点の状態を result[id] に格納した配列
	 */
	public long[] restore(long x) {
		long[] ret = new long[restoreResultSize];
		restore(x, ret);
		return ret;
	}

	/**
	 * f(x) を最小にする x を一つ選び、記録された状態を復元する。
	 *
	 * <p>
	 * argmin が区間の場合、0 に最も近い点を選ぶ。
	 *
	 * <p>
	 * 配列長は recordState(id) で使用された最大 id + 1 になる。
	 *
	 * @return recordState(id) 時点の状態を result[id] に格納した配列
	 */
	public long[] restoreMinimizer() {
		//https://atcoder.jp/contests/arc227/submissions/78475379
		QueryResult q = getMin();
		long x = clamp(0, q.lo(), q.hi());
		return restore(x);
	}

	private static final AtomicInteger drawCount = new AtomicInteger(0);

	/**
	 * 指定された範囲 [l, r) の外にある変化点を捨てる。
	 *
	 * @param l 範囲の下限
	 * @param r 範囲の上限
	 * @return this
	 */
	public SlopeTrick discardOutside(long l, long r) {
		L.removeIf(val -> {
			long cp = val - displacementL;
			return cp < l || cp >= r;
		});
		R.removeIf(val -> {
			long cp = val + displacementR;
			return cp < l || cp >= r;
		});
		return this;
	}

	private XYChart toChart() {
		List<Long> changePoints = new ArrayList<>();
		for (long val : L)
			changePoints.add(val - displacementL);
		for (long val : R)
			changePoints.add(val + displacementR);
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
				minX = low - diff / 10 - 1;
				maxX = high + diff / 10 + 1;
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
				.title("SlopeTrick-" + drawCount.incrementAndGet())
				.xAxisTitle("x")
				.yAxisTitle("f(x)")
				.build();

		chart.addSeries("f(x)", distinctX, distinctY)
				.setMarker(SeriesMarkers.CIRCLE);

		return chart;
	}
	
	
	/**
	 * この関数の凸共役 f*(x) を、定義域の整数点すべてについて返す。
	 *
	 * result[x + sizeL()] = f*(x)
	 *
	 * 計算量: O(N log N)
	 */
	long[] conjugateValues() {
		int nl = L.size();
		int nr = R.size();

		long[] ret = new long[nl + nr + 1];
		int zero = nl;

		ret[zero] = -minF;

		PriorityQueue<Long> left = new PriorityQueue<>(Collections.reverseOrder());
		for (long v : L)
			left.add(v - displacementL);

		long cur = -minF;
		for (int k = 1; k <= nl; k++) {
			cur -= left.poll();
			ret[zero - k] = cur;
		}

		PriorityQueue<Long> right = new PriorityQueue<>();
		for (long v : R)
			right.add(v + displacementR);

		cur = -minF;
		for (int k = 1; k <= nr; k++) {
			cur += right.poll();
			ret[zero + k] = cur;
		}

		return ret;
	}
	

	public void draw() {
		new SwingWrapper<>(toChart()).displayChart();
	}
}
