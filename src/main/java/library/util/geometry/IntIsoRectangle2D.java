package library.util.geometry;

import java.util.Objects;

/**
 * int 座標を持つ軸平行な閉矩形 R = [xmin, xmax] × [ymin, ymax]。
 * xmin = xmax または ymin = ymax の退化を許す。不変オブジェクト。
 */
public final class IntIsoRectangle2D implements GeometricObject {

	/** x 座標の下限。 */
	private final int xmin;
	/** y 座標の下限。 */
	private final int ymin;
	/** x 座標の上限。 */
	private final int xmax;
	/** y 座標の上限。 */
	private final int ymax;

	/**
	 * R = [min(x1, x2), max(x1, x2)] × [min(y1, y2), max(y1, y2)] を生成する。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param x1 対角点1の x 座標
	 * @param y1 対角点1の y 座標
	 * @param x2 対角点2の x 座標
	 * @param y2 対角点2の y 座標
	 */
	// 未テスト
	public IntIsoRectangle2D(int x1, int y1, int x2, int y2) {
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
	public IntIsoRectangle2D(IntPoint p, IntPoint q) {
		this(p.x(), p.y(), q.x(), q.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return min {x | (x, y) ∈ R}
	 */
	// 未テスト
	public int xmin() {
		return xmin;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return min {y | (x, y) ∈ R}
	 */
	// 未テスト
	public int ymin() {
		return ymin;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return max {x | (x, y) ∈ R}
	 */
	// 未テスト
	public int xmax() {
		return xmax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return max {y | (x, y) ∈ R}
	 */
	// 未テスト
	public int ymax() {
		return ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return (xmin, ymin) を持つ新しい点
	 */
	// 未テスト
	public IntPoint min() {
		return new IntPoint(xmin, ymin);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return (xmax, ymax) を持つ新しい点
	 */
	// 未テスト
	public IntPoint max() {
		return new IntPoint(xmax, ymax);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return xmax - xmin
	 * @throws ArithmeticException xmax - xmin &gt; Integer.MAX_VALUE の場合
	 */
	// 未テスト
	public int width() {
		return Math.subtractExact(xmax, xmin);
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @return ymax - ymin
	 * @throws ArithmeticException ymax - ymin &gt; Integer.MAX_VALUE の場合
	 */
	// 未テスト
	public int height() {
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
		return Math.multiplyExact((long) xmax - xmin, (long) ymax - ymin);
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
	public IntPoint vertex(int i) {
		return switch (Math.floorMod(i, 4)) {
			case 0 -> min();
			case 1 -> new IntPoint(xmax, ymin);
			case 2 -> max();
			default -> new IntPoint(xmin, ymax);
		};
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param x 点の x 座標
	 * @param y 点の y 座標
	 * @return (x, y) ∈ R（境界を含む）
	 */
	// 未テスト
	public boolean contains(int x, int y) {
		return xmin <= x && x <= xmax && ymin <= y && y <= ymax;
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param p 判定する点
	 * @return p ∈ R（境界を含む）
	 * @throws NullPointerException p が null の場合
	 */
	// 未テスト
	public boolean contains(IntPoint p) {
		return contains(p.x(), p.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param other 閉矩形 S
	 * @return S ⊆ R
	 * @throws NullPointerException other が null の場合
	 */
	// 未テスト
	public boolean contains(IntIsoRectangle2D other) {
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
	public BoundedSide boundedSide(int x, int y) {
		if (!contains(x, y)) return BoundedSide.ON_UNBOUNDED_SIDE;
		if (x == xmin || x == xmax || y == ymin || y == ymax) return BoundedSide.ON_BOUNDARY;
		return BoundedSide.ON_BOUNDED_SIDE;
	}

	/**
	 * {@link #boundedSide(int, int)} と同じ分類を返す。
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param p 判定する点
	 * @return 点の位置
	 * @throws NullPointerException p が null の場合
	 */
	// 未テスト
	public BoundedSide boundedSide(IntPoint p) {
		return boundedSide(p.x(), p.y());
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param other 閉矩形 S
	 * @return R ∩ S ≠ ∅。辺・頂点のみの共有も true
	 * @throws NullPointerException other が null の場合
	 */
	// 未テスト
	public boolean isIntersect(IntIsoRectangle2D other) {
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
	public IntIsoRectangle2D intersection(IntIsoRectangle2D other) {
		if (!isIntersect(other)) return null;
		return new IntIsoRectangle2D(Math.max(xmin, other.xmin), Math.max(ymin, other.ymin),
				Math.min(xmax, other.xmax), Math.min(ymax, other.ymax));
	}

	/**
	 * <p>計算量: 時間 O(1)、追加空間 O(1)。</p>
	 * @param obj 比較対象
	 * @return obj が IntIsoRectangle2D であり、4つの座標限界がすべて等しい場合に true
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof IntIsoRectangle2D other)) return false;
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
	 * @return "IntIsoRectangle2D[(xmin,ymin),(xmax,ymax)]" の座標を置換した文字列
	 */
	// 未テスト
	@Override
	public String toString() {
		return "IntIsoRectangle2D[(" + xmin + "," + ymin + "),(" + xmax + "," + ymax + ")]";
	}
}
