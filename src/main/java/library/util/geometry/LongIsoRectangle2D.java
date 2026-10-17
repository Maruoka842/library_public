package library.util.geometry;

import java.util.Objects;

/**
 * long 座標を持つ軸平行な閉矩形 R = [xmin, xmax] × [ymin, ymax]。
 * xmin = xmax または ymin = ymax の退化を許す。不変オブジェクト。
 */
public final class LongIsoRectangle2D implements GeometricObject {

	/** x 座標の下限。 */
	private final long xmin;
	/** y 座標の下限。 */
	private final long ymin;
	/** x 座標の上限。 */
	private final long xmax;
	/** y 座標の上限。 */
	private final long ymax;

	/**
	 * R = [min(x1, x2), max(x1, x2)] × [min(y1, y2), max(y1, y2)] を生成する。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param x1 対角点1の x 座標
	 * @param y1 対角点1の y 座標
	 * @param x2 対角点2の x 座標
	 * @param y2 対角点2の y 座標
	 */
	// 未テスト
	public LongIsoRectangle2D(long x1, long y1, long x2, long y2) {
		xmin = Math.min(x1, x2);
		ymin = Math.min(y1, y2);
		xmax = Math.max(x1, x2);
		ymax = Math.max(y1, y2);
	}

	/**
	 * 対角点 p, q を持つ閉矩形を生成する。p, q の以後の変更は R に影響しない。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param p 対角点1
	 * @param q 対角点2
	 * @throws NullPointerException p または q が null の場合
	 */
	// 未テスト
	public LongIsoRectangle2D(LongPoint p, LongPoint q) {
		this(p.x(), p.y(), q.x(), q.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return min {x | (x, y) ∈ R}
	 */
	// 未テスト
	public long xmin() {
		return xmin;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return min {y | (x, y) ∈ R}
	 */
	// 未テスト
	public long ymin() {
		return ymin;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return max {x | (x, y) ∈ R}
	 */
	// 未テスト
	public long xmax() {
		return xmax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return max {y | (x, y) ∈ R}
	 */
	// 未テスト
	public long ymax() {
		return ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return (xmin, ymin) を持つ新しい点
	 */
	// 未テスト
	public LongPoint min() {
		return new LongPoint(xmin, ymin);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return (xmax, ymax) を持つ新しい点
	 */
	// 未テスト
	public LongPoint max() {
		return new LongPoint(xmax, ymax);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return xmax - xmin
	 * @throws ArithmeticException xmax - xmin &gt; Long.MAX_VALUE の場合
	 */
	// 未テスト
	public long width() {
		return Math.subtractExact(xmax, xmin);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return ymax - ymin
	 * @throws ArithmeticException ymax - ymin &gt; Long.MAX_VALUE の場合
	 */
	// 未テスト
	public long height() {
		return Math.subtractExact(ymax, ymin);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return (xmax - xmin)(ymax - ymin)。退化している場合は 0
	 * @throws ArithmeticException 面積 &gt; Long.MAX_VALUE の場合
	 */
	// 未テスト
	public long area() {
		if (isDegenerate()) return 0;
		return Math.multiplyExact(width(), height());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return xmin = xmax または ymin = ymax
	 */
	// 未テスト
	public boolean isDegenerate() {
		return xmin == xmax || ymin == ymax;
	}

	/**
	 * i mod 4 = 0, 1, 2, 3 に対し順に
	 * (xmin, ymin), (xmax, ymin), (xmax, ymax), (xmin, ymax) を返す。
	 * 非退化時は反時計回り。負の i も許す。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param i 頂点の添字
	 * @return 指定座標を持つ新しい点
	 */
	// 未テスト
	public LongPoint vertex(int i) {
		return switch (Math.floorMod(i, 4)) {
			case 0 -> min();
			case 1 -> new LongPoint(xmax, ymin);
			case 2 -> max();
			default -> new LongPoint(xmin, ymax);
		};
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param x 点の x 座標
	 * @param y 点の y 座標
	 * @return (x, y) ∈ R（境界を含む）
	 */
	// 未テスト
	public boolean contains(long x, long y) {
		return xmin <= x && x <= xmax && ymin <= y && y <= ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param p 判定する点
	 * @return p ∈ R（境界を含む）
	 * @throws NullPointerException p が null の場合
	 */
	// 未テスト
	public boolean contains(LongPoint p) {
		return contains(p.x(), p.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param other 閉矩形 S
	 * @return S ⊆ R
	 * @throws NullPointerException other が null の場合
	 */
	// 未テスト
	public boolean contains(LongIsoRectangle2D other) {
		return xmin <= other.xmin && other.xmax <= xmax
				&& ymin <= other.ymin && other.ymax <= ymax;
	}

	/**
	 * xmin &lt; x &lt; xmax かつ ymin &lt; y &lt; ymax のとき ON_BOUNDED_SIDE、
	 * (x, y) ∈ R かつ内部でないとき ON_BOUNDARY、その他は ON_UNBOUNDED_SIDE。
	 * 退化時は内部が空であり、R 上の点はすべて ON_BOUNDARY。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param x 点の x 座標
	 * @param y 点の y 座標
	 * @return 点の位置
	 */
	// 未テスト
	public BoundedSide boundedSide(long x, long y) {
		if (!contains(x, y)) return BoundedSide.ON_UNBOUNDED_SIDE;
		if (x == xmin || x == xmax || y == ymin || y == ymax) return BoundedSide.ON_BOUNDARY;
		return BoundedSide.ON_BOUNDED_SIDE;
	}

	/**
	 * {@link #boundedSide(long, long)} と同じ分類を返す。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param p 判定する点
	 * @return 点の位置
	 * @throws NullPointerException p が null の場合
	 */
	// 未テスト
	public BoundedSide boundedSide(LongPoint p) {
		return boundedSide(p.x(), p.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param other 閉矩形 S
	 * @return R ∩ S ≠ ∅。辺・頂点のみの共有も true
	 * @throws NullPointerException other が null の場合
	 */
	// 未テスト
	public boolean isIntersect(LongIsoRectangle2D other) {
		return xmin <= other.xmax && other.xmin <= xmax
				&& ymin <= other.ymax && other.ymin <= ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param other 閉矩形 S
	 * @return R ∩ S を表す新しい矩形。R ∩ S = ∅ の場合は null
	 * @throws NullPointerException other が null の場合
	 */
	// 未テスト
	public LongIsoRectangle2D intersection(LongIsoRectangle2D other) {
		if (!isIntersect(other)) return null;
		return new LongIsoRectangle2D(Math.max(xmin, other.xmin), Math.max(ymin, other.ymin),
				Math.min(xmax, other.xmax), Math.min(ymax, other.ymax));
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param obj 比較対象
	 * @return obj が LongIsoRectangle2D であり、4つの座標限界がすべて等しい場合に true
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof LongIsoRectangle2D other)) return false;
		return xmin == other.xmin && ymin == other.ymin && xmax == other.xmax && ymax == other.ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return 等しい矩形で等しいハッシュ値
	 */
	// 未テスト
	@Override
	public int hashCode() {
		return Objects.hash(xmin, ymin, xmax, ymax);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return "LongIsoRectangle2D[(xmin,ymin),(xmax,ymax)]" の座標を置換した文字列
	 */
	// 未テスト
	@Override
	public String toString() {
		return "LongIsoRectangle2D[(" + xmin + "," + ymin + "),(" + xmax + "," + ymax + ")]";
	}
}
