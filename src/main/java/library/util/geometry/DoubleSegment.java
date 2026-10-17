package library.util.geometry;

import java.util.List;

public class DoubleSegment {
	public double x1, y1, x2, y2;
	
	public DoubleSegment(double x1, double y1, double x2, double y2) {
		this.x1=x1;
		this.y1=y1;
		this.x2=x2;
		this.y2=y2;
	}
	
	public DoubleSegment(DoublePoint p, DoublePoint q) {
		this(p.x, p.y, q.x, q.y);
	}

	/**
	 * 与えられた円 {@link DoubleCircle} とこの線分の交点を計算します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param circle 対象の円
	 * @return 円と線分の交点のリスト
	 */
	// 未テスト
	public List<DoublePoint> intersect(DoubleCircle circle) {
		return circle.intersect(this);
	}

	/**
	 * この線分と直線 line の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param line 対象の直線
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleLine line) {
		return line.intersect(this);
	}

	/**
	 * この線分と半直線 ray の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param ray 対象の半直線
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleRay ray) {
		return ray.intersect(this);
	}

	/**
	 * この線分と別の線分 other の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param other 対象の線分
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleSegment other) {
		DoublePoint a1 = new DoublePoint(x1, y1);
		DoublePoint b1 = new DoublePoint(x2, y2);
		DoublePoint a2 = new DoublePoint(other.x1, other.y1);
		DoublePoint b2 = new DoublePoint(other.x2, other.y2);

		DoubleVector u = b1.sub(a1);
		DoubleVector v = b2.sub(a2);

		double det = u.det(v);
		if (Math.abs(det) < 1e-18) {
			return null;
		}

		DoubleVector w = a2.sub(a1);
		double t1 = w.det(v) / det;
		double t2 = w.det(u) / det;

		if (t1 >= -1e-9 && t1 <= 1.0 + 1e-9 && t2 >= -1e-9 && t2 <= 1.0 + 1e-9) {
			t1 = Math.max(0.0, Math.min(1.0, t1));
			return new DoublePoint(x1 + t1 * u.x(), y1 + t1 * u.y());
		}
		return null;
	}

	/**
	 * この線分と別のオブジェクトの同値性を判定します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param obj 比較対象のオブジェクト
	 * @return 同値であれば true, そうでなければ false
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DoubleSegment)) return false;
		DoubleSegment other = (DoubleSegment) obj;
		return Double.compare(x1, other.x1) == 0 &&
		       Double.compare(y1, other.y1) == 0 &&
		       Double.compare(x2, other.x2) == 0 &&
		       Double.compare(y2, other.y2) == 0;
	}

	/**
	 * この線分のハッシュコードを計算します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return ハッシュコード
	 */
	// 未テスト
	@Override
	public int hashCode() {
		return java.util.Objects.hash(x1, y1, x2, y2);
	}
}
