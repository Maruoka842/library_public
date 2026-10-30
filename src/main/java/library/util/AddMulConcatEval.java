package library.util;

import library.util.linalg.MatrixUtilsZ;

/**
 * 加算・乗算・文字列連結（数字列の結合）を含む数式評価の状態表現。
 *
 * <p>評価途中の状態:
 * {@code a + b * c} に対して {@code (a, b, bc, 1)} に対する線形変換を保持します。
 *
 * <p>数式 {@code a + b * c} を 4 次元状態ベクトルの線形変換（4x4 遷移行列）で管理し、
 * 演算子の挿入や数字列の連結に伴う動的評価を O(1) で行う。
 */
public final class AddMulConcatEval {

	/**
	 * 加算演算子 (+) に対応する 4x4 状態遷移行列。
	 * {@code a + b * c = (a + b * c) + 1 * 0}
	 */
	private static final AddMulConcatEval PLUS_OPERATOR =
			new AddMulConcatEval(new long[][] {
				{1, 0, 1, 0},
				{0, 0, 0, 1},
				{0, 0, 0, 0},
				{0, 0, 0, 1}
			});

	/**
	 * 乗算演算子 (*) に対応する 4x4 状態遷移行列。
	 * {@code a + b * c = a + (b * c) * 0}
	 */
	private static final AddMulConcatEval TIMES_OPERATOR =
			new AddMulConcatEval(new long[][] {
				{1, 0, 0, 0},
				{0, 0, 1, 0},
				{0, 0, 0, 0},
				{0, 0, 0, 1}
			});

	/**
	 * 4x4 状態遷移行列。
	 */
	private final long[][] matrix;

	/**
	 * 現在の数式が表す評価値。
	 */
	public final long eval;

	/**
	 * 指定された整数値に対応する数式断片を構築する。
	 *
	 * @param value 非負の整数値
	 *
	 * @complexity
	 * 時間計算量: O(log10(value))
	 * 空間計算量: O(1)
	 */
	public AddMulConcatEval(long value) {
		// 未テスト
		long pow10 = 1;
		while (pow10 <= value) {
			pow10 *= 10;
		}
		this.matrix = new long[][] {
			{1, 0, 0, 0},
			{0, 1, 0, 0},
			{0, value, pow10, 0},
			{0, 0, 0, 1}
		};
		this.eval = this.matrix[0][1] + this.matrix[0][3] + this.matrix[2][1] + this.matrix[2][3];
	}

	/**
	 * 数字列を表す断片を生成します。
	 *
	 * <p>例えば「123」には {@code (123, 1000)}、
	 * 「007」には {@code (7, 1000)} を指定します。
	 *
	 * <p>引数の整合性は検証しません。
	 *
	 * @param value 数字列の非負の整数値。{@code pow10} 未満であること
	 * @param pow10 10 の「数字列の桁数」乗。
	 *              正の桁数に対応し、{@code long} で表現可能であること
	 *
	 * @complexity
	 * 時間計算量: O(1)
	 * 空間計算量: O(1)
	 */
	public AddMulConcatEval(long value, long pow10) {
		// 未テスト
		// q=pow10とするとa+b*cをa+b*(qc+v)に
		this.matrix = new long[][] {
			{1, 0, 0, 0},
			{0, 1, 0, 0},
			{0, value, pow10, 0},
			{0, 0, 0, 1}
		};
		this.eval = this.matrix[0][1] + this.matrix[0][3] + this.matrix[2][1] + this.matrix[2][3];
	}

	/**
	 * 状態遷移行列を所有する断片を生成します。
	 *
	 * <p>配列はコピーしないため、渡した配列は以後変更しないでください。
	 *
	 * @param matrix この断片専用の 4 行 4 列の状態遷移行列
	 *
	 * @complexity
	 * 時間計算量: O(1)
	 * 空間計算量: O(1)
	 */
	private AddMulConcatEval(long[][] matrix) {
		// 未テスト
		this.matrix = matrix;
		this.eval = matrix[0][1] + matrix[0][3] + matrix[2][1] + matrix[2][3];
	}

	/**
	 * 左右の数式を、演算子を挿入せずに連結します。
	 *
	 * @param left 左側の断片
	 * @param right 右側の断片
	 * @return 左側に続けて右側を読み込む断片
	 * @throws NullPointerException いずれかの引数が {@code null} の場合
	 *
	 * @complexity
	 * 時間計算量: O(1)
	 * 空間計算量: O(1)
	 */
	public static AddMulConcatEval concat(AddMulConcatEval left, AddMulConcatEval right) {
		// 未テスト
		return new AddMulConcatEval(MatrixUtilsZ.mul(right.matrix, left.matrix));
	}

	/**
	 * 左右の数式の間に加算記号を挿入して連結します。
	 *
	 * @param left 左側の数式
	 * @param right 右側の数式
	 * @return 「左側 + 右側」を表す数式
	 * @throws NullPointerException いずれかの引数が {@code null} の場合
	 *
	 * @complexity
	 * 時間計算量: O(1)
	 * 空間計算量: O(1)
	 */
	public static AddMulConcatEval add(AddMulConcatEval left, AddMulConcatEval right) {
		// 未テスト
		return concat(concat(left, PLUS_OPERATOR), right);
	}

	/**
	 * 左右の数式の間に乗算記号を挿入して連結します。
	 *
	 * @param left 左側の数式
	 * @param right 右側の数式
	 * @return 「左側 * 右側」を表す数式
	 * @throws NullPointerException いずれかの引数が {@code null} の場合
	 *
	 * @complexity
	 * 時間計算量: O(1)
	 * 空間計算量: O(1)
	 */
	public static AddMulConcatEval mul(AddMulConcatEval left, AddMulConcatEval right) {
		// 未テスト
		return concat(concat(left, TIMES_OPERATOR), right);
	}
}
