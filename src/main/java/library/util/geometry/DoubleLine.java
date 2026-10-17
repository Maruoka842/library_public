package library.util.geometry;

import java.util.List;

public class DoubleLine {
	public double a, b, c;

	/**
	 * 2点 (x0, y0), (x1, y1) を通る直線を構築する。
	 *
	 * @param x0 1点目の x 座標
	 * @param y0 1点目の y 座標
	 * @param x1 2点目の x 座標
	 * @param y1 2点目の y 座標
	 * @throws IllegalArgumentException 2点が一致する場合
	 */
	public DoubleLine(double x0, double y0, double x1, double y1) {
		this(y0 - y1, x1 - x0, -y0 * x1 + x0 * y1);
	}

	public DoubleLine(DoublePoint p0, DoublePoint p1) {
		this(p0.x, p0.y, p1.x, p1.y);
	}

	/**
	 * ax + by + c = 0 で表される直線を構築する。
	 *
	 * <p>構築時に sqrt(a^2+b^2)=1 となるよう正規化する。
	 * また、a > 0 または a = 0, b > 0 となるよう符号を正規化する。</p>
	 *
	 * @param a x の係数
	 * @param b y の係数
	 * @param c 定数項
	 * @throws IllegalArgumentException a = b = 0 の場合
	 */
	public DoubleLine(double a, double b, double c) {
		double norm = Math.hypot(a, b);
		if (norm == 0) {
			throw new IllegalArgumentException("a and b must not both be zero");
		}

		this.a = a / norm;
		this.b = b / norm;
		this.c = c / norm;

		if (this.a < 0 || (this.a == 0 && this.b < 0)) {
			this.a = -this.a;
			this.b = -this.b;
			this.c = -this.c;
		}

		// -0.0 を排除する。
		if (this.a == 0) this.a = 0;
		if (this.b == 0) this.b = 0;
		if (this.c == 0) this.c = 0;
	}

	/**
	 * ax + by + c を返す。
	 *
	 * <p>このクラスでは sqrt(a^2+b^2)=1 に正規化されているため、
	 * 返り値は点から直線への符号付き距離でもある。</p>
	 *
	 * @param x 点の x 座標
	 * @param y 点の y 座標
	 * @return ax + by + c
	 */
	public double eval(double x, double y) {
		return a * x + b * y + c;
	}

	public double eval(DoublePoint p) {
		return eval(p.x, p.y);
	}
	
	/**
	 * この直線の単位方向ベクトルを返す。	
	 * y 成分が負になる向きに正規化される。
	 * 水平線の場合は x 成分が正になる向きとなる。
	 * @return 単位方向ベクトル
	 */
	public DoubleVector direction() {
	    return new DoubleVector(b, -a);
	}
	
	/**
	 * この直線の単位法線ベクトルを返す。
	 *
	 * @return (a, b)
	 */
	public DoubleVector normal() {
	    return new DoubleVector(a, b);
	}

	/**
	 * 点が直線上にあるかを誤差 eps を許して判定する。
	 *
	 * @param x 点の x 座標
	 * @param y 点の y 座標
	 * @param eps 許容する距離
	 * @return 直線からの距離が eps 以下なら true
	 */
	public boolean isOnLine(double x, double y, double eps) {
		return Math.abs(eval(x, y)) <= eps;
	}

	public boolean onLine(DoublePoint p, double eps) {
		return isOnLine(p.x, p.y, eps);
	}

	/**
	 * 点から直線への符号付き距離を返す。
	 *
	 * <p>符号は ax+by+c の符号と一致する。</p>
	 *
	 * @param p 点
	 * @return 符号付き距離
	 */
	public double signedDistance(DoublePoint p) {
		return eval(p);
	}

	public double signedDistance(double x, double y) {
		return eval(x, y);
	}

	/**
	 * 点から直線までの距離を返す。
	 *
	 * @param p 点
	 * @return 距離
	 */
	public double distance(DoublePoint p) {
		return Math.abs(eval(p));
	}
	
	/**
	 * 点と直線のマンハッタン距離
	 * @param p
	 * @return
	 */
	public double distanceL1(DoublePoint p) {
		return Math.abs(eval(p) / Math.max(a, b)) ;
	}

	public double distance(double x, double y) {
		return Math.abs(eval(x, y));
	}

	/**
	 * 点 p をこの直線へ正射影した点を返す。
	 *
	 * @param p 点
	 * @return 正射影
	 */
	public DoublePoint projection(DoublePoint p) {
		double d = eval(p);
		return new DoublePoint(
				p.x - a * d,
				p.y - b * d
		);
	}

	/**
	 * 点 p をこの直線について対称移動した点を返す。
	 *
	 * @param p 点
	 * @return 対称移動した点
	 */
	public DoublePoint reflection(DoublePoint p) {
		double d = 2 * eval(p);
		return new DoublePoint(
				p.x - a * d,
				p.y - b * d
		);
	}

	/**
	 * 2直線の交点を返す。
	 *
	 * @param line もう一方の直線
	 * @return 交点
	 * @throws ArithmeticException 2直線が平行または一致する場合
	 */
	public DoublePoint intersection(DoubleLine line) {
		double det = a * line.b - b * line.a;
		if (det == 0) {
			throw new ArithmeticException("parallel or coincident lines");
		}

		double x = (-line.b * c + b * line.c) / det;
		double y = (line.a * c - a * line.c) / det;
		return new DoublePoint(x, y);
	}

	/**
	 * この直線と別の直線 line の交点を返します。
	 * 平行または一致する場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param line もう一方の直線
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleLine line) {
		double det = a * line.b - b * line.a;
		if (Math.abs(det) < 1e-18) {
			return null;
		}

		double x = (-line.b * c + b * line.c) / det;
		double y = (line.a * c - a * line.c) / det;
		return new DoublePoint(x, y);
	}

	/**
	 * この直線と半直線 ray の交点を計算します。
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
	 * この直線と線分 seg の交点を計算します。
	 * 交点が存在しない場合は null を返します。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param seg 対象の線分
	 * @return 交点 (存在しない場合は null)
	 */
	// 未テスト
	public DoublePoint intersect(DoubleSegment seg) {
		DoublePoint p1 = new DoublePoint(seg.x1, seg.y1);
		double e1 = eval(p1);

		double dx = seg.x2 - seg.x1;
		double dy = seg.y2 - seg.y1;
		double denom = a * dx + b * dy;

		if (Math.abs(denom) < 1e-18) {
			return null;
		}

		double t = -e1 / denom;
		if (t >= -1e-9 && t <= 1.0 + 1e-9) {
			t = Math.max(0.0, Math.min(1.0, t));
			return new DoublePoint(seg.x1 + t * dx, seg.y1 + t * dy);
		}
		return null;
	}

	/**
	 * この直線と円 {@code circle} の交点を返す。
	 * 交点が存在しない場合は空リスト、接する場合は接点のみからなるリスト、
	 * 2点で交わる場合はその2点からなるリストを返す。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param circle 対象の円
	 * @return この直線と {@code circle} の相異なる交点のリスト
	 */
	// 未テスト
	public List<DoublePoint> intersect(DoubleCircle circle) {
		return circle.intersect(this);
	}

	/**
	 * この直線と line が平行かを判定する。
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 平行なら true
	 */
	public boolean isParallelTo(DoubleLine line, double eps) {
		return Math.abs(a * line.b - b * line.a) <= eps;
	}

	/**
	 * この直線と line が垂直かを判定する。
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 垂直なら true
	 */
	public boolean isPerpendicularTo(DoubleLine line, double eps) {
		return Math.abs(a * line.a + b * line.b) <= eps;
	}

	/**
	 * y 切片を返す。
	 *
	 * @return y 切片。垂直線の場合は ±Infinity または NaN
	 */
	public double yIntercept() {
		return -c / b;
	}

	/**
	 * x 切片を返す。
	 *
	 * @return x 切片。水平線の場合は ±Infinity または NaN
	 */
	public double xIntercept() {
		return -c / a;
	}

	/**
	 * 傾きを返す。
	 *
	 * <p>ax+by+c=0 より傾きは -a/b。</p>
	 *
	 * @return 傾き。垂直線の場合は ±Infinity
	 */
	public double slopeAsDouble() {
		return -a / b;
	}

	/**
	 * 点 p を通り、法線ベクトルが (nx, ny) である直線を返す。
	 *
	 * @param p 直線上の点
	 * @param nx 法線ベクトルの x 成分
	 * @param ny 法線ベクトルの y 成分
	 * @return 直線
	 */
	public static DoubleLine fromPointAndNormal(DoublePoint p, double nx, double ny) {
		return new DoubleLine(nx, ny, -nx * p.x - ny * p.y);
	}

	/**
	 * 点 p を通り、方向ベクトルが (dx, dy) である直線を返す。
	 *
	 * @param p 直線上の点
	 * @param dx 方向ベクトルの x 成分
	 * @param dy 方向ベクトルの y 成分
	 * @return 直線
	 */
	public static DoubleLine fromPointAndDirection(DoublePoint p, double dx, double dy) {
		return new DoubleLine(dy, -dx, -dy * p.x + dx * p.y);
	}

	/**
	 * 2点 (x0,y0), (x1,y1) を結ぶ線分の垂直二等分線を返す。
	 *
	 * @param x0 1点目の x 座標
	 * @param y0 1点目の y 座標
	 * @param x1 2点目の x 座標
	 * @param y1 2点目の y 座標
	 * @return 垂直二等分線
	 */
	public static DoubleLine perpendicularBisector(
			double x0, double y0, double x1, double y1) {
		return new DoubleLine(
				2 * (x1 - x0),
				2 * (y1 - y0),
				x0 * x0 + y0 * y0 - x1 * x1 - y1 * y1
		);
	}

	public static DoubleLine perpendicularBisector(DoublePoint p0, DoublePoint p1) {
		return perpendicularBisector(p0.x, p0.y, p1.x, p1.y);
	}

	@Override
	public String toString() {
		return a + "x" + (b >= 0 ? "+" : "") + b + "y"
				+ (c >= 0 ? "+" : "") + c + "=0";
	}

	/**
	 * 係数が完全に一致するかを判定する。
	 *
	 * <p>浮動小数点誤差を許した幾何学的な同値判定ではない。</p>
	 *
	 * @param obj 比較対象
	 * @return 係数が一致するなら true
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DoubleLine)) return false;
		DoubleLine other = (DoubleLine) obj;
		return Double.compare(a, other.a) == 0
				&& Double.compare(b, other.b) == 0
				&& Double.compare(c, other.c) == 0;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(a, b, c);
	}
}
