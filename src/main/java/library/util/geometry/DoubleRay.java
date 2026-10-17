package library.util.geometry;

import java.util.Objects;

/**
 * 2次元平面上の半直線を double 精度で表すクラスです。
 *
 * <p>始点 (source) と単位方向ベクトル (direction) によって定義され、
 * パラメータ t >= 0 に対して source + t * direction 上の点を表します。</p>
 */
public class DoubleRay implements GeometricObject {

	private final DoublePoint source;
	private final DoubleVector direction;

	/**
	 * 始点と方向ベクトルを指定して半直線を構築します。
	 * 方向ベクトルは内部で単位ベクトルに正規化されます。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param source 始点
	 * @param direction 方向ベクトル (零ベクトル不可)
	 * @throws IllegalArgumentException 方向ベクトルが零ベクトルの場合
	 */
	// 未テスト
	public DoubleRay(DoublePoint source, DoubleVector direction) {
		if (source == null || direction == null) {
			throw new NullPointerException("Source and direction must not be null.");
		}
		double norm = direction.norm();
		if (norm == 0.0) {
			throw new IllegalArgumentException("Direction vector must not be zero.");
		}
		this.source = source;
		this.direction = direction.normalize();
	}

	/**
	 * 始点と通過点を指定して半直線を構築します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param source 始点
	 * @param through 半直線が通過する点 (始点と異なる点)
	 * @throws IllegalArgumentException 始点と通過点が一致する場合
	 */
	// 未テスト
	public DoubleRay(DoublePoint source, DoublePoint through) {
		if (source == null || through == null) {
			throw new NullPointerException("Source and through points must not be null.");
		}
		DoubleVector dir = through.sub(source);
		double norm = dir.norm();
		if (norm == 0.0) {
			throw new IllegalArgumentException("Source and through points must be distinct.");
		}
		this.source = source;
		this.direction = dir.normalize();
	}

	/**
	 * 始点を取得します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return 始点
	 */
	// 未テスト
	public DoublePoint source() {
		return source;
	}

	/**
	 * 単位方向ベクトルを取得します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return 単位方向ベクトル
	 */
	// 未テスト
	public DoubleVector direction() {
		return direction;
	}

	/**
	 * パラメータ t に対応する半直線上の点 source + t * direction を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param t 始点からの距離を表すパラメータ
	 * @return パラメータ t に対応する点
	 */
	// 未テスト
	public DoublePoint point(double t) {
		return source.add(direction.mul(t));
	}

	/**
	 * この半直線を包含する直線 (支持直線) を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return 支持直線
	 */
	// 未テスト
	public DoubleLine supportingLine() {
		return DoubleLine.fromPointAndDirection(source, direction.x(), direction.y());
	}

	/**
	 * 指定された点 p がこの半直線上に含まれるかを許容誤差 1e-9 で判定します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param p 判定対象の点
	 * @return 点 p が半直線上にあれば true, そうでなければ false
	 */
	// 未テスト
	public boolean contains(DoublePoint p) {
		return contains(p, 1e-9);
	}

	/**
	 * 指定された点 p がこの半直線上に含まれるかを許容誤差 eps で判定します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param p 判定対象の点
	 * @param eps 許容誤差
	 * @return 点 p が半直線上にあれば true, そうでなければ false
	 */
	// 未テスト
	public boolean contains(DoublePoint p, double eps) {
		DoubleLine line = supportingLine();
		if (!line.onLine(p, eps)) {
			return false;
		}
		DoubleVector v = p.sub(source);
		return v.dot(direction) >= -eps;
	}

	/**
	 * この半直線と直線 line の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param line 対象の直線
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleLine line) {
		double a = line.a;
		double b = line.b;

		double denom = a * direction.x() + b * direction.y();
		if (Math.abs(denom) < 1e-18) {
			return null;
		}

		double t = -line.eval(source) / denom;
		if (t >= -1e-9) {
			t = Math.max(0.0, t);
			return point(t);
		}
		return null;
	}

	/**
	 * この半直線と別の半直線 other の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param other 対象の半直線
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleRay other) {
		double det = direction.det(other.direction);
		if (Math.abs(det) < 1e-18) {
			return null;
		}

		DoubleVector v = other.source.sub(source);
		double t1 = v.det(other.direction) / det;
		double t2 = v.det(direction) / det;

		if (t1 >= -1e-9 && t2 >= -1e-9) {
			t1 = Math.max(0.0, t1);
			return point(t1);
		}
		return null;
	}

	/**
	 * この半直線と線分 seg の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param seg 対象の線分
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleSegment seg) {
		DoublePoint a = new DoublePoint(seg.x1, seg.y1);
		DoublePoint b = new DoublePoint(seg.x2, seg.y2);
		DoubleVector u = b.sub(a);

		double det = u.det(direction);
		if (Math.abs(det) < 1e-18) {
			return null;
		}

		DoubleVector v = a.sub(source);
		double t = u.det(v) / det;
		double paramU = direction.det(v) / det;

		if (t >= -1e-9 && paramU >= -1e-9 && paramU <= 1.0 + 1e-9) {
			t = Math.max(0.0, t);
			return point(t);
		}
		return null;
	}

	/**
	 * この半直線と円 circle の交点を計算します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param circle 対象の円
	 * @return 交点のリスト
	 */
	// 未テスト
	public java.util.List<DoublePoint> intersect(DoubleCircle circle) {
		DoubleVector o = source.sub(circle.center());
		double B = 2.0 * o.dot(direction);
		double C = o.squaredLength() - circle.radius() * circle.radius();

		double D = B * B - 4.0 * C;
		if (D < 0.0) {
			if (D >= -1e-9) {
				D = 0.0;
			} else {
				return java.util.List.of();
			}
		}

		double sqrtD = Math.sqrt(D);
		double t1 = (-B - sqrtD) / 2.0;
		double t2 = (-B + sqrtD) / 2.0;

		java.util.List<DoublePoint> result = new java.util.ArrayList<>();
		double eps = 1e-9;
		if (t1 >= -eps) {
			result.add(point(Math.max(0.0, t1)));
		}
		if (t2 >= -eps) {
			DoublePoint p2 = point(Math.max(0.0, t2));
			if (!result.isEmpty()) {
				DoublePoint p1 = result.get(0);
				if (p1.distance(p2) > eps) {
					result.add(p2);
				}
			} else {
				result.add(p2);
			}
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DoubleRay)) return false;
		DoubleRay other = (DoubleRay) obj;
		return Objects.equals(source, other.source) && Objects.equals(direction, other.direction);
	}

	@Override
	public int hashCode() {
		return Objects.hash(source, direction);
	}

	@Override
	public String toString() {
		return "DoubleRay[source=" + source + ", direction=" + direction + "]";
	}
}
