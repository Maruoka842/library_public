package library.util.fold;

/**
 * 整数格子 {@code [0, H) x [0, W)} に対する、静的な斜交長方形内一次式加算・一点取得を表す。
 *
 * <p>{@code u = x + y}, {@code v = x - y} とする。
 * {@link #addSlope(int, int, int, int, long, long, long)} は
 * {@code u0 <= u < u1 && v0 <= v < v1} を満たす各格子点へ
 * {@code uSlope * (u-u0) + vSlope * (v-v0) + valueAtOrigin} を加える。
 *
 * <p>加算は {@link #build()} より前、取得は {@code build()} より後に限る。
 *
 * <p>コンストラクタは O(HW) time, O(HW) space、各加算は O(1) time、
 * {@code build()} は O(HW) time、各取得は O(1) time である。
 */
public class StaticDiagonalSlopeAddPointGet2D {

	/** 格子の高さ。 */
	private final int height;

	/** 格子の幅。 */
	private final int width;

	/** build 前は {@code u} 係数の斜め差分、build 後は各点の {@code u} 係数。 */
	private final long[][] uCoefficients;

	/** build 前は {@code v} 係数の斜め差分、build 後は各点の {@code v} 係数。 */
	private final long[][] vCoefficients;

	/** build 前は定数項の斜め差分、build 後は各点の定数項。 */
	private final long[][] constants;

	/** 先頭行の {@code u,v,1} 係数の 2-step 差分。 */
	private final long[][] firstRow;

	/** 2行目の {@code u,v,1} 係数の 2-step 差分。 */
	private final long[][] secondRow;

	/** 左端列の {@code u,v,1} 係数の 2-step 差分。 */
	private final long[][] firstColumn;

	/** 右端列の {@code u,v,1} 係数の 2-step 差分。 */
	private final long[][] lastColumn;

	/** {@link #build()} が完了したかを表す。 */
	private boolean built;

	/**
	 * {@code H x W} の格子を構築する。
	 *
	 * @param H 格子の高さ。{@code H >= 0}
	 * @param W 格子の幅。{@code W >= 0}
	 * @throws IllegalArgumentException {@code H < 0 || W < 0} のとき
	 * @implNote O(HW) time, O(HW) space
	 */
	// 未テスト
	public StaticDiagonalSlopeAddPointGet2D(int H, int W) {
		if (H < 0 || W < 0) {
			throw new IllegalArgumentException("H and W must be nonnegative");
		}
		height = H;
		width = W;
		uCoefficients = new long[H][W];
		vCoefficients = new long[H][W];
		constants = new long[H][W];
		firstRow = new long[3][W];
		secondRow = new long[3][W];
		firstColumn = new long[3][H];
		lastColumn = new long[3][H];
	}

	/**
	 * 全ての {@code 0 <= i < H, 0 <= j < W} に対して、
	 * {@code 2 * c * max(abs(x-i), abs(y-j))} を加える。
	 *
	 * @param c 距離に掛ける係数の半分
	 * @param x 距離の基準点の行座標
	 * @param y 距離の基準点の列座標
	 * @throws IndexOutOfBoundsException {@code (x, y)} が格子外のとき
	 * @throws IllegalStateException {@code build()} 後のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	public void addDistanceFrom(long c, int x, int y) {
		checkNotBuilt();
		if (x < 0 || x >= height || y < 0 || y >= width) {
			throw new IndexOutOfBoundsException(
					"coordinate out of bounds: (" + x + ", " + y + ")");
		}

		int u = x + y;
		int v = x - y;
		int uEnd = height + width - 1;
		int vBegin = 1 - width;

		addSlope(0, u, vBegin, height, -c, 0, c * u);
		addSlope(u, uEnd, vBegin, height, c, 0, 0);
		addSlope(0, uEnd, vBegin, v, 0, -c, c * (v - vBegin));
		addSlope(0, uEnd, v, height, 0, c, 0);
	}

	/**
	 * {@code u0 <= u < u1 && v0 <= v < v1} の各点に
	 * {@code uSlope * (u-u0) + valueAtU0} を加える。
	 *
	 * @param u0 {@code u} の下端（含む）
	 * @param u1 {@code u} の上端（含まない）
	 * @param v0 {@code v} の下端（含む）
	 * @param v1 {@code v} の上端（含まない）
	 * @param uSlope {@code u} が 1 増えたときの増分
	 * @param valueAtU0 {@code u = u0} における値
	 * @throws IllegalStateException {@code build()} 後のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	public void addSlope(
			int u0, int u1, int v0, int v1,
			long uSlope, long valueAtU0) {
		addSlope(u0, u1, v0, v1, uSlope, 0, valueAtU0);
	}

	/**
	 * {@code u0 <= u < u1 && v0 <= v < v1} の各点に
	 * {@code uSlope * (u-u0) + vSlope * (v-v0) + valueAtOrigin} を加える。
	 *
	 * @param u0 {@code u} の下端（含む）
	 * @param u1 {@code u} の上端（含まない）
	 * @param v0 {@code v} の下端（含む）
	 * @param v1 {@code v} の上端（含まない）
	 * @param uSlope {@code u} が 1 増えたときの増分
	 * @param vSlope {@code v} が 1 増えたときの増分
	 * @param valueAtOrigin {@code (u, v) = (u0, v0)} における値
	 * @throws IllegalStateException {@code build()} 後のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	public void addSlope(
			int u0, int u1, int v0, int v1,
			long uSlope, long vSlope, long valueAtOrigin) {
		checkNotBuilt();
		long constant = valueAtOrigin - uSlope * u0 - vSlope * v0;
		add(u0, u1, v0, v1, uSlope, vSlope, constant);
	}

	/**
	 * {@code u0 <= u < u1 && v0 <= v < v1 && u mod 2 = parity} の各点に
	 * {@code uSlope * (u-u0) + vSlope * (v-v0) + valueAtOrigin} を加える。
	 *
	 * @param u0 {@code u} の下端（含む）
	 * @param u1 {@code u} の上端（含まない）
	 * @param v0 {@code v} の下端（含む）
	 * @param v1 {@code v} の上端（含まない）
	 * @param parity 0 または 1
	 * @param uSlope {@code u} が 1 増えたときの増分
	 * @param vSlope {@code v} が 1 増えたときの増分
	 * @param valueAtOrigin {@code (u, v) = (u0, v0)} における値
	 * @throws IllegalArgumentException {@code parity} が 0 でも 1 でもないとき
	 * @throws IllegalStateException {@code build()} 後のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	public void addSlope(
			int u0, int u1, int v0, int v1, int parity,
			long uSlope, long vSlope, long valueAtOrigin) {
		checkNotBuilt();
		long constant = valueAtOrigin - uSlope * u0 - vSlope * v0;
		if ((parity & ~1) != 0) {
			throw new IllegalArgumentException("parity must be 0 or 1");
		}
		add(u0, u1, v0, v1, parity, uSlope, vSlope, constant);
	}

	/**
	 * それ以前の全更新を確定する。複数回の呼び出しは同じ結果を保つ。
	 *
	 * @implNote O(HW) time, O(1) extra space
	 */
	// 未テスト
	public void build() {
		if (built) return;

		if (height >= 1) materializeRow(firstRow, 0);
		if (height >= 2) materializeRow(secondRow, 1);
		if (width >= 1) materializeColumn(firstColumn, 0);
		if (width >= 2) materializeColumn(lastColumn, width - 1);

		for (int x = 0; x < height; x++) {
			for (int y = 0; y < width; y++) {
				if (x >= 1 && y >= 1) {
					addCell(x, y, x - 1, y - 1, 1);
				}
				if (x >= 1 && y + 1 < width) {
					addCell(x, y, x - 1, y + 1, 1);
				}
				if (x >= 2) {
					addCell(x, y, x - 2, y, -1);
				}
			}
		}
		built = true;
	}

	/**
	 * 点 {@code (x, y)} に加算された値の総和を返す。
	 *
	 * @param x 行座標
	 * @param y 列座標
	 * @return 点 {@code (x, y)} の値
	 * @throws IllegalStateException {@code build()} より前のとき
	 * @throws IndexOutOfBoundsException {@code (x, y)} が格子外のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	public long get(int x, int y) {
		if (!built) {
			throw new IllegalStateException("get must be called after build");
		}
		long u = (long) x + y;
		long v = (long) x - y;
		if (x < 0 || x >= height || y < 0 || y >= width) {
			throw new IndexOutOfBoundsException("(x, y) is outside the grid");
		}
		return uCoefficients[x][y] * u
				+ vCoefficients[x][y] * v
				+ constants[x][y];
	}

	/**
	 * {@code u0 <= u < u1 && v0 <= v < v1} の各点の係数ベクトルへ
	 * {@code (du, dv, dc)} を加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private void add(
			int u0, int u1, int v0, int v1,
			long du, long dv, long dc) {
		if ((du | dv | dc) == 0) return;
		if (height == 0 || width == 0 || u0 >= u1 || v0 >= v1) return;

		addRectangle(u0, u1, v0, v1, 0, du, dv, dc);
		addRectangle(u0, u1, v0, v1, 1, du, dv, dc);
	}

	/**
	 * {@code u0 <= u < u1 && v0 <= v < v1 && u mod 2 = parity} の各点の
	 * 係数ベクトルへ {@code (du, dv, dc)} を加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private void add(
			int u0, int u1, int v0, int v1, int parity,
			long du, long dv, long dc) {
		if ((du | dv | dc) == 0) return;
		if (height == 0 || width == 0 || u0 >= u1 || v0 >= v1) return;
		addRectangle(u0, u1, v0, v1, parity, du, dv, dc);
	}

	/**
	 * 半開斜交長方形の四隅へ係数ベクトルの差分を加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private void addRectangle(
			int u0, int u1, int v0, int v1, int parity,
			long du, long dv, long dc) {
		addCorner(u0, v0, parity, du, dv, dc);
		addCorner(u0, v1, parity, -du, -dv, -dc);
		addCorner(u1, v0, parity, -du, -dv, -dc);
		addCorner(u1, v1, parity, du, dv, dc);
	}

	/**
	 * {@code x+y >= u && x-y >= v && (x+y) mod 2 = parity} の斜め差分へ
	 * 係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private void addCorner(
			int u, int v, int parity,
			long du, long dv, long dc) {
		addParityRange(firstRow, u, 1L - v, parity, du, dv, dc);

		if (height >= 2) {
			int yParity = parity ^ 1;
			addParityRange(secondRow, (long) u - 1, 2L - v,
					yParity, du, dv, dc);
			addShiftedParityRange(secondRow, u, 1L - v, 1,
					parity, -du, -dv, -dc);
			addShiftedParityRange(secondRow, u, 1L - v, -1,
					parity, -du, -dv, -dc);
		}

		if (height >= 3) {
			addSide(firstColumn, 0, 1, u, v, parity, du, dv, dc);
			if (width >= 2) {
				addSide(lastColumn, width - 1, -1, u, v, parity,
						du, dv, dc);
			}
		}

		long upperU = ceilToParity(u, parity);
		long upperV = ceilToParity(v, parity);
		long x = (upperU + upperV) / 2;
		long y = (upperU - upperV) / 2;
		if (2 <= x && x < height && 1 <= y && y + 1 < width) {
			addVector(uCoefficients, vCoefficients, constants,
					(int) x, (int) y, du, dv, dc);
		}
	}

	/**
	 * 盤面端の列 {@code y} の斜め差分へ係数ベクトルを加える。
	 *
	 * @param inward 盤面内部方向。左端なら 1、右端なら -1
	 * @implNote O(1) time
	 */
	// 未テスト
	private void addSide(
			long[][] difference, int y, int inward,
			int u, int v, int parity,
			long du, long dv, long dc) {
		int xParity = parity ^ (y & 1);
		long threshold = threshold(u, v, y);
		addParitySuffix(difference, threshold, xParity, du, dv, dc);

		int innerY = y + inward;
		if (0 <= innerY && innerY < width) {
			addParitySuffix(difference, threshold(u, v, innerY) + 1,
					xParity, -du, -dv, -dc);
		}
		addParitySuffix(difference, threshold + 2, xParity, du, dv, dc);
	}

	/**
	 * {@code max(u-y, v+y)} を返す。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static long threshold(int u, int v, int y) {
		return Math.max((long) u - y, (long) v + y);
	}

	/**
	 * 2-step 差分の {@code [from, to)} に係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static void addParityRange(
			long[][] difference, long from, long to, int parity,
			long du, long dv, long dc) {
		long left = Math.max(0, from);
		long right = Math.min(difference[0].length, to);
		if (left >= right) return;

		long first = ceilToParity(left, parity);
		if (first >= right) return;
		addVector(difference, (int) first, du, dv, dc);

		long end = ceilToParity(right, parity);
		if (end < difference[0].length) {
			addVector(difference, (int) end, -du, -dv, -dc);
		}
	}

	/**
	 * 元区間を盤面内で切り詰め、{@code shift} 後の 2-step 差分へ
	 * 係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static void addShiftedParityRange(
			long[][] difference, long from, long to, int shift,
			int sourceParity, long du, long dv, long dc) {
		long left = Math.max(Math.max(from, 0L), -(long) shift);
		long right = Math.min(Math.min(to, difference[0].length),
				(long) difference[0].length - shift);
		if (left >= right) return;
		addParityRange(difference, left + shift, right + shift,
				sourceParity ^ (shift & 1), du, dv, dc);
	}

	/**
	 * 行 2 以降の {@code from} 以上かつ指定 parity の最初の差分へ
	 * 係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static void addParitySuffix(
			long[][] difference, long from, int parity,
			long du, long dv, long dc) {
		long first = ceilToParity(Math.max(2L, from), parity);
		if (first < difference[0].length) {
			addVector(difference, (int) first, du, dv, dc);
		}
	}

	/**
	 * {@code x} 以上で {@code parity} と同じ偶奇の最小整数を返す。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static long ceilToParity(long x, int parity) {
		return x + ((x - parity) & 1L);
	}

	/**
	 * 行の 2-step 差分を復元して係数配列へ加える。
	 *
	 * @implNote O(W) time, O(1) extra space
	 */
	// 未テスト
	private void materializeRow(long[][] difference, int x) {
		for (int y = 0; y < width; y++) {
			if (y >= 2) addVector(difference, y, difference[0][y - 2],
					difference[1][y - 2], difference[2][y - 2]);
			addVector(uCoefficients, vCoefficients, constants, x, y,
					difference[0][y], difference[1][y], difference[2][y]);
		}
	}

	/**
	 * 列の 2-step 差分を復元して係数配列へ加える。
	 *
	 * @implNote O(H) time, O(1) extra space
	 */
	// 未テスト
	private void materializeColumn(long[][] difference, int y) {
		for (int x = 2; x < height; x++) {
			if (x >= 4) addVector(difference, x, difference[0][x - 2],
					difference[1][x - 2], difference[2][x - 2]);
			addVector(uCoefficients, vCoefficients, constants, x, y,
					difference[0][x], difference[1][x], difference[2][x]);
		}
	}

	/**
	 * 点 {@code (sourceX, sourceY)} の係数ベクトルの {@code scale} 倍を
	 * 点 {@code (targetX, targetY)} へ加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private void addCell(
			int targetX, int targetY, int sourceX, int sourceY, int scale) {
		uCoefficients[targetX][targetY] += scale * uCoefficients[sourceX][sourceY];
		vCoefficients[targetX][targetY] += scale * vCoefficients[sourceX][sourceY];
		constants[targetX][targetY] += scale * constants[sourceX][sourceY];
	}

	/**
	 * 3個の二次元配列の点 {@code (x, y)} へ係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static void addVector(
			long[][] uValues, long[][] vValues, long[][] constantValues,
			int x, int y, long du, long dv, long dc) {
		uValues[x][y] += du;
		vValues[x][y] += dv;
		constantValues[x][y] += dc;
	}

	/**
	 * 3成分の一次元配列の位置 {@code index} へ係数ベクトルを加える。
	 *
	 * @implNote O(1) time
	 */
	// 未テスト
	private static void addVector(
			long[][] values, int index, long du, long dv, long dc) {
		values[0][index] += du;
		values[1][index] += dv;
		values[2][index] += dc;
	}

	/**
	 * 更新可能な状態であることを検査する。
	 *
	 * @throws IllegalStateException {@code build()} 後のとき
	 * @implNote O(1) time
	 */
	// 未テスト
	private void checkNotBuilt() {
		if (built) {
			throw new IllegalStateException("addSlope must be called before build");
		}
	}
}
