package library.util.graph.grid;
 
/**
 * 8近傍の無限グリッド上の移動コストを扱うユーティリティ。
 * 移動コストが十分に大きい（{@code INF} 以上）方向は禁止方向（移動不可）として扱われます。
 */
public final class Grid8MovementCost {
	/** 方向 k の x 座標増分。順序は E, NE, N, NW, W, SW, S, SE。 */
	public static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
	/** 方向 k の y 座標増分。順序は E, NE, N, NW, W, SW, S, SE。 */
	public static final int[] DY = {0, 1, 1, 1, 0, -1, -1, -1};
	/** long の加算・乗算でオーバーフローしない入力に対する正の番兵値。 */
	private static final long INF = Long.MAX_VALUE / 4;
	/** 負閉路により最小コストが下に非有界であることを表す返値。 */
	public static final long NEGATIVE_INFINITY = Long.MIN_VALUE;
 
	private Grid8MovementCost() {
	}
 
	/**
	 * 8近傍の無限グリッド上で、原点から相対座標 {@code (x, y)} へ移動する最小コストを求める。
	 *
	 * <p><b>移動モデル</b>:
	 * 1 回の移動で 8 方向のいずれか 1 つへ 1 マス進み、方向 {@code d} の移動 1 回につき {@code cost[d]} を支払う。
	 * コストは位置や直前の移動に依存しない。グリッドは無限で障害物はなく、同じ方向を何度でも使える。
	 * 方向の添字 {@code 0..7} は E, NE, N, NW, W, SW, S, SE の順で、x 軸は東向き正、y 軸は北向き正とする。
	 * {@code (0,0)} に戻る負コスト閉路が存在する場合、返値 = {@link #NEGATIVE_INFINITY}
	 * 計算量: {@code O(1)} 時間。
	 */
	public static long minCost(long x, long y, long[] cost) {
		//https://atcoder.jp/contests/abc271/submissions/77797521（コスト1 or INF）
		validate(cost);
		long min = cost[0];
		for (int i = 1; i < 8; i++) {
			if (cost[i] < min) min = cost[i];
		}
		final boolean hasNeg = min < 0;
		if (hasNeg && hasNegativeCycle(cost[0], cost[1], cost[2], cost[3], cost[4], cost[5], cost[6], cost[7])) {
			return NEGATIVE_INFINITY;
		}
		long ans = minCostByAtMostTwoDirections(x, y, cost);
		if (!hasNeg && !mayNeedExtraStep(x, y, cost, ans)) return ans;
		for (int k = 0; k < 8; k++) {
			long ck = cost[k];
			if (ck >= INF) continue;
			long restX = x - DX[k];
			long restY = y - DY[k];
			if (!hasNeg) {
				if (ck >= ans) continue;
				// 全コストが min 以上（非負）で、1 手で動ける Chebyshev 距離は高々 1 なので
				// tail >= min * cheb。この下界で ans を更新できないなら tail の計算を省く。
				// （両方 2^31 未満のときだけ判定し、乗算オーバーフローを避ける）
				long cheb = Math.max(Math.abs(restX), Math.abs(restY));
				if (((cheb | min) >>> 31) == 0 && ck + min * cheb >= ans) continue;
			}
			long tail = minCostByAtMostTwoDirections(restX, restY, cost);
			if (tail < INF) {
				long total = ck + tail;
				if (total < ans) ans = total;
			}
		}
		return ans;
	}
 
	/**
	 * 契約:
	 * <ul><li>事前条件: なし。</li><li>事後条件: {@code cost.length = 8}。</li><li>副作用: なし。</li><li>計算量: {@code O(1)}。</li><li>破壊的変更: なし。</li><li>参照共有・所有権: {@code cost} の参照を保持しない。</li><li>例外: {@code cost.length != 8} の場合、{@link IllegalArgumentException}。</li><li>未定義条件: {@code cost == null}。</li></ul>
	 */
	private static void validate(long[] cost) {
		if (cost.length != 8) throw new IllegalArgumentException("cost.length must be 8");
	}
 
	/**
	 * 契約:
	 * <ul><li>事前条件: なし。</li><li>事後条件: 返値 = 「非空の移動列 {@code d_t} が存在し、{@code Σ DX[d_t] = 0}, {@code Σ DY[d_t] = 0}, {@code Σ cost[d_t] < 0}」。</li><li>副作用: なし。</li><li>計算量: {@code O(1)}。</li><li>破壊的変更: なし。</li><li>参照共有・所有権: 参照を保持しない。</li><li>例外: なし。</li><li>未定義条件: なし。</li></ul>
	 */
	private static boolean hasNegativeCycle(long c0, long c1, long c2, long c3, long c4, long c5, long c6, long c7) {
		// 4 opposite pairs
		if (safeAdd(c0, c4) < 0) return true;
		if (safeAdd(c1, c5) < 0) return true;
		if (safeAdd(c2, c6) < 0) return true;
		if (safeAdd(c3, c7) < 0) return true;
 
		// 8 triangle cycles
		if (c0 < INF && c2 < INF && c5 < INF && c0 + c2 + c5 < 0) return true;
		if (c0 < INF && c3 < INF && c5 < INF && 2 * c0 + c3 + c5 < 0) return true;
		if (c0 < INF && c3 < INF && c6 < INF && c0 + c3 + c6 < 0) return true;
		if (c1 < INF && c3 < INF && c6 < INF && c1 + c3 + 2 * c6 < 0) return true;
		if (c1 < INF && c4 < INF && c6 < INF && c1 + c4 + c6 < 0) return true;
		if (c1 < INF && c4 < INF && c7 < INF && c1 + 2 * c4 + c7 < 0) return true;
		if (c2 < INF && c4 < INF && c7 < INF && c2 + c4 + c7 < 0) return true;
		if (c2 < INF && c5 < INF && c7 < INF && 2 * c2 + c5 + c7 < 0) return true;
 
		return false;
	}
 
	/**
	 * 高々 2 方向の非負整数ステップによる (x, y) への最小移動コストを計算する。
	 * det = 1 の 20 組の基底対と det = 2 の 4 組の対角対を、係数を定数畳み込みした形で展開してある。
	 * （単一方向だけで到達する場合は、相方の回数 0 の基底対として必ず含まれるので別処理は不要。）
	 *
	 * 計算量: O(1)
	 */
	private static long minCostByAtMostTwoDirections(long x, long y, long[] cost) {
		if (x == 0 && y == 0) return 0;
		final long c0 = cost[0], c1 = cost[1], c2 = cost[2], c3 = cost[3];
		final long c4 = cost[4], c5 = cost[5], c6 = cost[6], c7 = cost[7];
		long ans = INF, na, nb, t;
 
		// (0, 1)
		na = x - y; nb = y;
		if (na >= 0 && nb >= 0) { t = pair(na, c0, nb, c1); if (t < ans) ans = t; }
		// (0, 2)
		na = x; nb = y;
		if (na >= 0 && nb >= 0) { t = pair(na, c0, nb, c2); if (t < ans) ans = t; }
		// (0, 3)
		na = x + y; nb = y;
		if (na >= 0 && nb >= 0) { t = pair(na, c0, nb, c3); if (t < ans) ans = t; }
		// (5, 0)
		na = -y; nb = x - y;
		if (na >= 0 && nb >= 0) { t = pair(na, c5, nb, c0); if (t < ans) ans = t; }
		// (6, 0)
		na = -y; nb = x;
		if (na >= 0 && nb >= 0) { t = pair(na, c6, nb, c0); if (t < ans) ans = t; }
		// (7, 0)
		na = -y; nb = x + y;
		if (na >= 0 && nb >= 0) { t = pair(na, c7, nb, c0); if (t < ans) ans = t; }
		// (1, 2)
		na = x; nb = -x + y;
		if (na >= 0 && nb >= 0) { t = pair(na, c1, nb, c2); if (t < ans) ans = t; }
		// (1, 4)
		na = y; nb = -x + y;
		if (na >= 0 && nb >= 0) { t = pair(na, c1, nb, c4); if (t < ans) ans = t; }
		// (6, 1)
		na = x - y; nb = x;
		if (na >= 0 && nb >= 0) { t = pair(na, c6, nb, c1); if (t < ans) ans = t; }
		// (2, 3)
		na = x + y; nb = -x;
		if (na >= 0 && nb >= 0) { t = pair(na, c2, nb, c3); if (t < ans) ans = t; }
		// (2, 4)
		na = y; nb = -x;
		if (na >= 0 && nb >= 0) { t = pair(na, c2, nb, c4); if (t < ans) ans = t; }
		// (2, 5)
		na = -x + y; nb = -x;
		if (na >= 0 && nb >= 0) { t = pair(na, c2, nb, c5); if (t < ans) ans = t; }
		// (7, 2)
		na = x; nb = x + y;
		if (na >= 0 && nb >= 0) { t = pair(na, c7, nb, c2); if (t < ans) ans = t; }
		// (3, 4)
		na = y; nb = -x - y;
		if (na >= 0 && nb >= 0) { t = pair(na, c3, nb, c4); if (t < ans) ans = t; }
		// (3, 6)
		na = -x; nb = -x - y;
		if (na >= 0 && nb >= 0) { t = pair(na, c3, nb, c6); if (t < ans) ans = t; }
		// (4, 5)
		na = -x + y; nb = -y;
		if (na >= 0 && nb >= 0) { t = pair(na, c4, nb, c5); if (t < ans) ans = t; }
		// (4, 6)
		na = -x; nb = -y;
		if (na >= 0 && nb >= 0) { t = pair(na, c4, nb, c6); if (t < ans) ans = t; }
		// (4, 7)
		na = -x - y; nb = -y;
		if (na >= 0 && nb >= 0) { t = pair(na, c4, nb, c7); if (t < ans) ans = t; }
		// (5, 6)
		na = -x; nb = x - y;
		if (na >= 0 && nb >= 0) { t = pair(na, c5, nb, c6); if (t < ans) ans = t; }
		// (6, 7)
		na = -x - y; nb = x;
		if (na >= 0 && nb >= 0) { t = pair(na, c6, nb, c7); if (t < ans) ans = t; }
		// (1, 3) 偶奇条件あり
		na = x + y; nb = -x + y;
		if (na >= 0 && (na & 1) == 0 && nb >= 0) { t = pair(na >> 1, c1, nb >> 1, c3); if (t < ans) ans = t; }
		// (7, 1) 偶奇条件あり
		na = x - y; nb = x + y;
		if (na >= 0 && (na & 1) == 0 && nb >= 0) { t = pair(na >> 1, c7, nb >> 1, c1); if (t < ans) ans = t; }
		// (3, 5) 偶奇条件あり
		na = -x + y; nb = -x - y;
		if (na >= 0 && (na & 1) == 0 && nb >= 0) { t = pair(na >> 1, c3, nb >> 1, c5); if (t < ans) ans = t; }
		// (5, 7) 偶奇条件あり
		na = -x - y; nb = x - y;
		if (na >= 0 && (na & 1) == 0 && nb >= 0) { t = pair(na >> 1, c5, nb >> 1, c7); if (t < ans) ans = t; }
		return ans;
	}
 
	/**
	 * 非負コストのとき、2 方向解 {@code ans} に 1 手足した解で改善する可能性があるか（false なら改善不可能が保証される）。
	 *
	 * <p>LP 緩和の最適解は高々 2 方向の基底解で、det = 1 の基底と偶奇が合う det = 2 の基底は整数解になる。
	 * 整数にならないのは det = 2 の対角対で係数が半整数になる場合（x + y が奇数）だけなので、
	 * その実数コスト {@code (na*cu + nb*cv) / 2} がすべて {@code ans} 以上なら、
	 * {@code ans} は LP 下界に一致し最適。判定に自信が持てない（オーバーフローしうる）場合は true を返す。
	 */
	private static boolean mayNeedExtraStep(long x, long y, long[] cost, long ans) {
		if (((x + y) & 1) == 0) return false;
		if (((Math.abs(x) | Math.abs(y)) >>> 29) != 0) return true;
		long s = x + y, d = x - y, ans2 = 2 * ans;
		return oddCandidateBeats(s, cost[1], -d, cost[3], ans2)
				|| oddCandidateBeats(d, cost[7], s, cost[1], ans2)
				|| oddCandidateBeats(-d, cost[3], -s, cost[5], ans2)
				|| oddCandidateBeats(-s, cost[5], d, cost[7], ans2);
	}
 
	/** det = 2 の対角対で係数が半整数になる候補の「2 倍コスト」が ans2 未満か。 */
	private static boolean oddCandidateBeats(long na, long cu, long nb, long cv, long ans2) {
		if (na < 0 || nb < 0 || cu >= INF || cv >= INF) return false;
		if (((cu | cv) >>> 31) != 0) return true;
		return na * cu + nb * cv < ans2;
	}
 
	/** a, b >= 0 の回数で方向コスト cu, cv を使う合計コスト。禁止方向を使うなら INF。 */
	private static long pair(long a, long cu, long b, long cv) {
		long p = safeMul(a, cu);
		if (p >= INF) return INF;
		long q = safeMul(b, cv);
		if (q >= INF) return INF;
		return p + q;
	}
 
	private static long safeAdd(long a, long b) {
		if (a >= INF || b >= INF) return INF;
		return a + b;
	}
 
	private static long safeMul(long a, long b) {
		if (a == 0 || b == 0) return 0;
		if (a >= INF || b >= INF) return INF;
		return a * b;
	}
}
 