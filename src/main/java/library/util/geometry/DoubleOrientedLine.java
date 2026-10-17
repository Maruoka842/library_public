package library.util.geometry;

/**
 * 向きを持つ直線を表すクラス。
 *
 * <p>
 * 直線は
 * {@code ax + by + c = 0}
 * で表され、{@code sqrt(a^2+b^2)=1} となるよう正規化される。
 * </p>
 *
 * <p>
 * この直線の向きは単位方向ベクトル {@code (b, -a)} で定める。
 * したがって {@code (a, b)} は進行方向に対する左向き単位法線ベクトルであり、
 * {@code ax+by+c > 0} の点は直線の左側、
 * {@code ax+by+c < 0} の点は直線の右側にある。
 * </p>
 */
public class DoubleOrientedLine {

	public double a, b, c;

	/**
	 * 2点 (x0, y0), (x1, y1) を通る有向直線を構築する。
	 *
	 * <p>向きは (x0, y0) から (x1, y1) への向きとなる。</p>
	 *
	 * @param x0 1点目の x 座標
	 * @param y0 1点目の y 座標
	 * @param x1 2点目の x 座標
	 * @param y1 2点目の y 座標
	 * @throws IllegalArgumentException 2点が一致する場合
	 */
	public DoubleOrientedLine(double x0, double y0, double x1, double y1) {
		this(y0 - y1, x1 - x0, -y0 * x1 + x0 * y1);
	}

	/**
	 * p0, p1 を通る有向直線を構築する。
	 *
	 * <p>向きは p0 から p1 への向きとなる。</p>
	 *
	 * @param p0 1点目
	 * @param p1 2点目
	 * @throws IllegalArgumentException 2点が一致する場合
	 */
	public DoubleOrientedLine(DoublePoint p0, DoublePoint p1) {
		this(p0.x, p0.y, p1.x, p1.y);
	}

	/**
	 * ax + by + c = 0 で表される有向直線を構築する。
	 *
	 * <p>
	 * 構築時に sqrt(a^2+b^2)=1 となるよう正規化する。
	 * 符号の正規化は行わない。
	 * </p>
	 *
	 * <p>
	 * この直線の向きは (b, -a) であり、
	 * (a, b) はその左向き法線ベクトルとなる。
	 * </p>
	 *
	 * @param a x の係数
	 * @param b y の係数
	 * @param c 定数項
	 * @throws IllegalArgumentException a = b = 0 の場合
	 */
	public DoubleOrientedLine(double a, double b, double c) {
		double norm = Math.hypot(a, b);
		if (norm == 0) {
			throw new IllegalArgumentException("a and b must not both be zero");
		}

		this.a = a / norm;
		this.b = b / norm;
		this.c = c / norm;

		// -0.0 を排除する。
		if (this.a == 0) this.a = 0;
		if (this.b == 0) this.b = 0;
		if (this.c == 0) this.c = 0;
	}

	/**
	 * ax + by + c を返す。
	 *
	 * <p>
	 * sqrt(a^2+b^2)=1 なので、返り値の絶対値は点から直線までの距離である。
	 * また、正なら有向直線の左側、負なら右側にある。
	 * </p>
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
	 *
	 * @return (b, -a)
	 */
	public DoubleVector direction() {
		return new DoubleVector(b, -a);
	}

	/**
	 * この直線の左向き単位法線ベクトルを返す。
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
	 * 点がこの有向直線の左側にあるかを判定する。
	 *
	 * @param p 点
	 * @param eps 許容誤差
	 * @return eval(p) > eps なら true
	 */
	public boolean isLeft(DoublePoint p, double eps) {
		return eval(p) > eps;
	}

	/**
	 * 点がこの有向直線の右側にあるかを判定する。
	 *
	 * @param p 点
	 * @param eps 許容誤差
	 * @return eval(p) < -eps なら true
	 */
	public boolean isRight(DoublePoint p, double eps) {
		return eval(p) < -eps;
	}

	/**
	 * 点から直線への符号付き距離を返す。
	 *
	 * <p>
	 * 左側なら正、右側なら負となる。
	 * </p>
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
	 * 向きを反転した直線を返す。
	 *
	 * <p>
	 * 幾何学的には同じ直線だが、方向ベクトルおよび左右が反転する。
	 * </p>
	 *
	 * @return 逆向きの直線
	 */
	public DoubleOrientedLine reverse() {
		return new DoubleOrientedLine(-a, -b, -c);
	}

	/**
	 * 2直線の交点を返す。
	 *
	 * @param line もう一方の直線
	 * @return 交点
	 * @throws ArithmeticException 2直線が平行または一致する場合
	 */
	public DoublePoint intersection(DoubleOrientedLine line) {
		double det = a * line.b - b * line.a;
		if (det == 0) {
			throw new ArithmeticException("parallel or coincident lines");
		}
		double x = (-line.b * c + b * line.c) / det;
		double y = (line.a * c - a * line.c) / det;
		return new DoublePoint(x, y);
	}

	/**
	 * この直線と line が平行かを判定する。
	 *
	 * <p>同方向か逆方向かは区別しない。</p>
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 平行なら true
	 */
	public boolean isParallelTo(DoubleOrientedLine line, double eps) {
		return Math.abs(a * line.b - b * line.a) <= eps;
	}

	/**
	 * この直線と line の向きが同じかを判定する。
	 *
	 * <p>直線の位置は考慮せず、方向ベクトルだけを比較する。</p>
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 方向が同じなら true
	 */
	public boolean isSameDirectionAs(DoubleOrientedLine line, double eps) {
		double cross = a * line.b - b * line.a;
		double dot = a * line.a + b * line.b;
		return Math.abs(cross) <= eps && dot > 0;
	}

	/**
	 * この直線と line の向きが逆かを判定する。
	 *
	 * <p>直線の位置は考慮せず、方向ベクトルだけを比較する。</p>
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 方向が逆なら true
	 */
	public boolean isOppositeDirectionTo(DoubleOrientedLine line, double eps) {
		double cross = a * line.b - b * line.a;
		double dot = a * line.a + b * line.b;
		return Math.abs(cross) <= eps && dot < 0;
	}

	/**
	 * この直線と line が垂直かを判定する。
	 *
	 * @param line もう一方の直線
	 * @param eps 許容誤差
	 * @return 垂直なら true
	 */
	public boolean isPerpendicularTo(DoubleOrientedLine line, double eps) {
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
	 * 点 p を通り、左向き法線ベクトルが (nx, ny) である有向直線を返す。
	 *
	 * <p>方向ベクトルは (ny, -nx) となる。</p>
	 *
	 * @param p 直線上の点
	 * @param nx 左向き法線ベクトルの x 成分
	 * @param ny 左向き法線ベクトルの y 成分
	 * @return 有向直線
	 */
	public static DoubleOrientedLine fromPointAndNormal(
			DoublePoint p, double nx, double ny) {
		return new DoubleOrientedLine(nx, ny, -nx * p.x - ny * p.y);
	}

	/**
	 * 点 p を通り、方向ベクトルが (dx, dy) である有向直線を返す。
	 *
	 * @param p 直線上の点
	 * @param dx 方向ベクトルの x 成分
	 * @param dy 方向ベクトルの y 成分
	 * @return (dx, dy) の向きを持つ有向直線
	 */
	public static DoubleOrientedLine fromPointAndDirection(
			DoublePoint p, double dx, double dy) {
		return new DoubleOrientedLine(
				-dy,
				dx,
				dy * p.x - dx * p.y
		);
	}

	/**
	 * 2点 (x0,y0), (x1,y1) を結ぶ線分の垂直二等分線を返す。
	 *
	 * <p>
	 * 法線は (x0,y0) から (x1,y1) の方向を向く。
	 * したがって (x1,y1) はこの有向直線の左側、
	 * (x0,y0) は右側となる。
	 * </p>
	 *
	 * @param x0 1点目の x 座標
	 * @param y0 1点目の y 座標
	 * @param x1 2点目の x 座標
	 * @param y1 2点目の y 座標
	 * @return 垂直二等分線
	 */
	public static DoubleOrientedLine perpendicularBisector(
			double x0, double y0, double x1, double y1) {
		return new DoubleOrientedLine(
				2 * (x1 - x0),
				2 * (y1 - y0),
				x0 * x0 + y0 * y0 - x1 * x1 - y1 * y1
		);
	}

	public static DoubleOrientedLine perpendicularBisector(
			DoublePoint p0, DoublePoint p1) {
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
	 * <p>
	 * 逆向きの直線は等しいとはみなさない。
	 * また、浮動小数点誤差を許した幾何学的な同値判定ではない。
	 * </p>
	 *
	 * @param obj 比較対象
	 * @return 係数が一致するなら true
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DoubleOrientedLine)) return false;
		DoubleOrientedLine other = (DoubleOrientedLine) obj;
		return Double.compare(a, other.a) == 0
				&& Double.compare(b, other.b) == 0
				&& Double.compare(c, other.c) == 0;
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(a, b, c);
	}
}