package library.util.segtree;

public class MinSeg extends SegTreelong {
//https://atcoder.jp/contests/abc234/submissions/74021821
	public MinSeg(int n) {
		super(n, Long::min, Long.MAX_VALUE/3);
	}
	
	/**
	 * [l, r) 内で最小値をとる最小の添字 i を返す。l < r を満たすこと。
	 * 計算量: O(log n)
	 * 未テスト
	 * @param l 区間の始端
	 * @param r 区間の終端
	 * @return argmin{i | l <= i < r}
	 */
	public int argMin(int l, int r) {
		//https://atcoder.jp/contests/abc404/submissions/75005027
		long min=fold(l, r);
		int arg=maximalRight(l, v -> v > min);
		return arg;
	}
	
	/**
	 * from ≤ i かつ a[i] ≤ v を満たす最小の i を返す。存在しないときは a.length を返す。
	 * @param from
	 * @param v
	 * @return
	 */
	public int firstLeqPos(int from, long v) {
		 return super.maximalRight(from, x -> x > v);
	}
	
	/**
	 * from ≥ i かつ a[i] ≤ v を満たす最大の i を返す。存在しないときは -1 を返す。
	 * @param from
	 * @param v
	 * @return
	 */	public int lastLeqPos(int from, long v) {
		return super.minimalLeft(from, x -> x > v);
	}

	/**
	 * 内部状態を標準出力に出力する。
	 * <ul>
	 *   <li>事前条件: 特になし。</li>
	 *   <li>事後条件: 特になし。</li>
	 *   <li>副作用: 標準出力への出力。</li>
	 *   <li>計算量: O(N)</li>
	 *   <li>破壊的変更: なし。</li>
	 * </ul>
	 */
	// 未テスト
	@Override
	public void dump() {
		System.out.print("MinSeg: ");
		super.dump();
	}
}
