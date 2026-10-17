package library.util.fold;

/**
 * 整数格子 {@code [0, H) x [0, W)} に対する、
 * 静的な斜交長方形加算・一点取得を表す。
 *
 * <p>{@code u = x + y}, {@code v = x - y} とする。
 * {@link #add(int, int, int, int, long)} は
 * {@code [u0, u1) x [v0, v1)} に含まれる全格子点へ加算する。
 *
 * <p>また {@link #add(int, int, int, int, int, long)} により、
 * {@code (x + y) mod 2} を指定した加算も O(1) で行える。
 *
 * <p>build 前の {@code values} を斜め差分 {@code d} とすると、
 *
 * <pre>
 * d[x][y]
 * = a[x][y]
 * - a[x-1][y-1]
 * - a[x-1][y+1]
 * + a[x-2][y]
 * </pre>
 *
 * （盤面外の項は 0）である。
 *
 * <p>上 2 行と左右端列の {@code d} は、偶奇ごとの 2-step 差分として
 * 別配列に保持する。
 */
public class StaticDiagonalRangeAddPointGet2D {

	private final int height;
	private final int width;

	/** build 前は内部の斜め差分、build 後は各点の値。 */
	private final long[][] values;

	/** {@code d[0][y] - d[0][y-2]}。 */
	private final long[] firstRow;

	/** {@code d[1][y] - d[1][y-2]}。 */
	private final long[] secondRow;

	/** {@code d[x][0] - d[x-2][0]}。行 2 以降だけを使用する。 */
	private final long[] firstColumn;

	/** {@code d[x][W-1] - d[x-2][W-1]}。行 2 以降だけを使用する。 */
	private final long[] lastColumn;

	private boolean built;

	public StaticDiagonalRangeAddPointGet2D(int H, int W) {
		if (H < 0 || W < 0) {
			throw new IllegalArgumentException("H and W must be nonnegative");
		}
		height = H;
		width = W;

		values = new long[H][W];

		firstRow = new long[W];
		secondRow = new long[W];

		firstColumn = new long[H];
		lastColumn = new long[H];
	}

	/**
	 * {@code u0 <= x+y < u1 && v0 <= x-y < v1}
	 * を満たす全ての点へ {@code val} を加える。
	 *
	 * @implNote O(1) time
	 */
	public void add(int u0, int u1, int v0, int v1, long val) {
		checkNotBuilt();
		if (val == 0) return;
		if (height == 0 || width == 0 || u0 >= u1 || v0 >= v1) return;

		addRectangle(u0, u1, v0, v1, 0, val);
		addRectangle(u0, u1, v0, v1, 1, val);
	}

	/**
	 * {@code u0 <= x+y < u1 && v0 <= x-y < v1} かつ
	 * {@code (x+y) mod 2 == parity}
	 * を満たす全ての点へ {@code val} を加える。
	 *
	 * <p>{@code x+y} と {@code x-y} の偶奇は常に等しいため、
	 * {@code parity} は斜交座標 {@code u,v} の共通の偶奇でもある。
	 *
	 * @param parity 0 または 1
	 * @implNote O(1) time
	 */
	public void add(
			int u0, int u1,
			int v0, int v1,
			int parity, long val) {

		checkNotBuilt();

		if ((parity & ~1) != 0) {
			throw new IllegalArgumentException("parity must be 0 or 1");
		}
		if (val == 0) return;
		if (height == 0 || width == 0 || u0 >= u1 || v0 >= v1) return;

		addRectangle(u0, u1, v0, v1, parity, val);
	}

	private void addRectangle(
			int u0, int u1,
			int v0, int v1,
			int parity, long val) {

		addCorner(u0, v0, parity, val);
		addCorner(u0, v1, parity, -val);
		addCorner(u1, v0, parity, -val);
		addCorner(u1, v1, parity, val);
	}

	/**
	 * {@code x+y >= u && x-y >= v && (x+y) mod 2 == parity}
	 * の斜め差分を加える。
	 */
	private void addCorner(int u, int v, int parity, long val) {

		/*
		 * x = 0
		 *
		 * u <= y <= -v
		 */
		addParityRange(
				firstRow,
				u,
				1L - v,
				parity,
				val);

		/*
		 * x = 1
		 *
		 * d[1][y]
		 * = q(1,y) - q(0,y-1) - q(0,y+1)
		 */
		if (height >= 2) {
			int yParity = parity ^ 1;

			addParityRange(
					secondRow,
					(long) u - 1,
					2L - v,
					yParity,
					val);

			addShiftedParityRange(
					secondRow,
					u,
					1L - v,
					1,
					parity,
					-val);

			addShiftedParityRange(
					secondRow,
					u,
					1L - v,
					-1,
					parity,
					-val);
		}

		/*
		 * 左右端。
		 *
		 * 固定した y では
		 *
		 * q(x,y) = [x >= max(u-y, v+y)]
		 */
		if (height >= 3) {
			addSide(firstColumn, 0, 1, u, v, parity, val);

			if (width >= 2) {
				addSide(lastColumn, width - 1, -1, u, v, parity, val);
			}
		}

		/*
		 * 内部では指定 parity について一点だけ残る。
		 */
		long U = ceilToParity(u, parity);
		long V = ceilToParity(v, parity);

		long x = (U + V) / 2;
		long y = (U - V) / 2;

		if (2 <= x && x < height && 1 <= y && y + 1 < width) {
			values[(int) x][(int) y] += val;
		}
	}

	/**
	 * 盤面端の列 {@code y} への寄与を加える。
	 *
	 * @param inward 盤面内部方向。左端なら 1、右端なら -1
	 */
	private void addSide(
			long[] difference,
			int y,
			int inward,
			int u,
			int v,
			int parity,
			long val) {

		int xParity = parity ^ (y & 1);
		long threshold = threshold(u, v, y);

		// q(x, y)
		addParitySuffix(
				difference,
				threshold,
				xParity,
				val);

		// -q(x-1, y+inward)
		int innerY = y + inward;
		if (0 <= innerY && innerY < width) {
			addParitySuffix(
					difference,
					threshold(u, v, innerY) + 1,
					xParity,
					-val);
		}

		// +q(x-2, y)
		addParitySuffix(
				difference,
				threshold + 2,
				xParity,
				val);
	}

	private static long threshold(int u, int v, int y) {
		return Math.max((long) u - y, (long) v + y);
	}

	/**
	 * 2-step 差分に、
	 * {@code [from, to)} のうち指定 parity の添字だけへ区間加算する。
	 */
	private static void addParityRange(
			long[] difference,
			long from,
			long to,
			int parity,
			long val) {

		long l = Math.max(0, from);
		long r = Math.min(difference.length, to);

		if (l >= r) return;

		long first = ceilToParity(l, parity);
		if (first >= r) return;

		difference[(int) first] += val;

		long end = ceilToParity(r, parity);
		if (end < difference.length) {
			difference[(int) end] -= val;
		}
	}

	/**
	 * 元区間を盤面内で切り詰めた後に {@code shift} し、
	 * 2-step 差分へ加算する。
	 */
	private static void addShiftedParityRange(
			long[] difference,
			long from,
			long to,
			int shift,
			int sourceParity,
			long val) {

		long l = Math.max(Math.max(from, 0L), -(long) shift);
		long r = Math.min(
				Math.min(to, difference.length),
				(long) difference.length - shift);

		if (l >= r) return;

		addParityRange(
				difference,
				l + shift,
				r + shift,
				sourceParity ^ (shift & 1),
				val);
	}

	/**
	 * 行 2 以降のうち、{@code from} 以上かつ指定 parity の添字へ加算する。
	 */
	private static void addParitySuffix(
			long[] difference,
			long from,
			int parity,
			long val) {

		long first = ceilToParity(Math.max(2L, from), parity);

		if (first < difference.length) {
			difference[(int) first] += val;
		}
	}

	/**
	 * {@code x} 以上で {@code parity} と同じ偶奇の最小整数を返す。
	 */
	private static long ceilToParity(long x, int parity) {
		return x + ((x - parity) & 1L);
	}

	/**
	 * それ以前の全更新を確定する。
	 *
	 * @implNote O(HW) time, O(1) extra space
	 */
	public void build() {
		if (built) return;

		/*
		 * 2-step 差分を復元する。
		 * この時点で values 全体が斜め差分 d になる。
		 */
		if (height >= 1) materializeRow(firstRow, 0);
		if (height >= 2) materializeRow(secondRow, 1);

		if (width >= 1) materializeColumn(firstColumn, 0);
		if (width >= 2) materializeColumn(lastColumn, width - 1);

		/*
		 * d -> a
		 *
		 * a[x][y]
		 * = d[x][y]
		 * + a[x-1][y-1]
		 * + a[x-1][y+1]
		 * - a[x-2][y]
		 */
		for (int x = 0; x < height; x++) {
			for (int y = 0; y < width; y++) {
				if (x >= 1 && y >= 1) {
					values[x][y] += values[x - 1][y - 1];
				}
				if (x >= 1 && y + 1 < width) {
					values[x][y] += values[x - 1][y + 1];
				}
				if (x >= 2) {
					values[x][y] -= values[x - 2][y];
				}
			}
		}

		built = true;
	}

	private void materializeRow(long[] difference, int x) {
		for (int y = 0; y < width; y++) {
			if (y >= 2) {
				difference[y] += difference[y - 2];
			}
			values[x][y] += difference[y];
		}
	}

	private void materializeColumn(long[] difference, int y) {
		for (int x = 2; x < height; x++) {
			if (x >= 4) {
				difference[x] += difference[x - 2];
			}
			values[x][y] += difference[x];
		}
	}

	/**
	 * 点 {@code (x, y)} の値を返す。
	 *
	 * @implNote O(1) time
	 */
	public long get(int x, int y) {
		if (!built) {
			throw new IllegalStateException("get must be called after build");
		}
		if (x < 0 || x >= height || y < 0 || y >= width) {
			throw new IndexOutOfBoundsException("(x, y) is outside the grid");
		}
		return values[x][y];
	}

	private void checkNotBuilt() {
		if (built) {
			throw new IllegalStateException("add must be called before build");
		}
	}
}
