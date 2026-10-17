package library.util.fold;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import library.util.ArrayUtils;
import library.util.MathUtils;

public class SparseTable2DInt {
	private int[][][][] accum;
	IntBinaryOperator op;
	int H, W;
	
	public SparseTable2DInt(int[][] v, IntBinaryOperator op) {
		this.op = op;
		build(v);
	}
	
	private void build(int[][] v) {
		this.H = v.length;
		this.W = v[0].length;
		int logH = 0;
		int logW = 0;
		while (1<<logH < H) logH++;
		while (1<<logW < W) logW++;
		accum = new int[logH + 1][logW + 1][v.length][v[0].length];
		this.accum[0][0] = ArrayUtils.copy(v);
		for (int i = 0; i+1 <= logH; i++) {
			for (int h = 0; h+(1<<(i+1)) <= H; h++) {
				for (int w = 0; w < W; w++) {
					accum[i+1][0][h][w] = op.applyAsInt(accum[i][0][h][w], accum[i][0][h+(1<<i)][w]);
				}
			}
		}
		for(int i=0;i<=logH;++i) {
			for (int j = 0; j+1 <= logW; j++) {
				for (int h = 0; h < H; h++) {
					for (int w = 0; w +(1<<(j+1)) <= W; w++) {
						accum[i][j+1][h][w] = op.applyAsInt(accum[i][j][h][w], accum[i][j][h][w+(1<<j)]);
					}
				}
			}
		}
	}
	
	/**
	 * [i0, i1) × [j0, j1)
	 */
	public int fold(int i0, int j0, int i1, int j1) {
		int wi=MathUtils.floorLog2(i1-i0);
		int wj=MathUtils.floorLog2(j1-j0);
		return op.applyAsInt(op.applyAsInt(accum[wi][wj][i0][j0], accum[wi][wj][i0][j1-(1<<wj)]), op.applyAsInt(accum[wi][wj][i1-(1<<wi)][j0], accum[wi][wj][i1-(1<<wi)][j1-(1<<wj)]));
	}
	
	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}

	/**
	 * 内部状態を標準出力に出力する。
	 * O(HW log H log W)
	 */
	public void dump() {
		System.out.println("SparseTable2DLong { accum: " + java.util.Arrays.deepToString(accum) + " }");
	}
}