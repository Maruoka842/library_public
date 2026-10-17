package library.util.poset;

import java.util.Arrays;

import library.util.ArrayStatistics;
import library.util.ArrayUtils;
import library.util.segtree.SegTreeFactory;

public class IntpairPoset {

    /**
     * {@code strict = false} では {@code x[i] <= x[j] && y[i] <= y[j]}、
     * {@code strict = true} では {@code x[i] < x[j] && y[i] < y[j]} となる chain を考える。
     * 互いに素な高々 {@code k} 本の chain に含まれる点の個数の最大値を返す。
     * {@code k = 0} のときは0を返す。
     *
     * 計算量: O(N log N + N min(k, N) log N)、空間計算量: O(N)
     *
     * @param x 各点の第1座標
     * @param y 各点の第2座標
     * @param k chain の本数の上限。0以上でなければならない
     * @param strict chain 内の座標をともに狭義増加させる場合は {@code true}、
     *               ともに広義増加させる場合は {@code false}
     * @return 高々 {@code k} 本の chain の長さの和の最大値
     */
    public static int maximumKChainsLength(int[] x, int[] y, int k, boolean strict) {
        // 未テスト
        if (x.length != y.length || k < 0) throw new AssertionError();
        int n = x.length;
        if (n == 0 || k == 0) return 0;

        int[] X = x.clone();
        int[] Y = y.clone();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (i, j) -> {
            if (X[i] != X[j]) return Integer.compare(X[i], X[j]);
            return strict ? Integer.compare(Y[j], Y[i]) : Integer.compare(Y[i], Y[j]);
        });
        int[] sortedY = new int[n];
        for (int i = 0; i < n; i++) {
            sortedY[i] = Y[order[i]];
        }

        k = Math.min(k, n);

        int[][] rows = new int[k][0];
        int[] sizes = new int[k];
        int answer = 0;
        for (int i = 0; i < n; i++) {
            int value = sortedY[i];
            for (int row = 0; row < k; row++) {
                int left = 0;
                int right = sizes[row];
                while (left < right) {
                    int mid = (left + right) >>> 1;
                    if (strict ? rows[row][mid] < value : rows[row][mid] <= value) {
                        left = mid + 1;
                    } else {
                        right = mid;
                    }
                }
                if (left == sizes[row]) {
                    if (sizes[row] == rows[row].length) {
                        int newLength = Math.max(1, rows[row].length * 2);
                        rows[row] = Arrays.copyOf(rows[row], newLength);
                    }
                    rows[row][sizes[row]++] = value;
                    answer++;
                    break;
                }
                int bumped = rows[row][left];
                rows[row][left] = value;
                value = bumped;
            }
        }
        return answer;
    }

    /**
     * {@code (x[i], y[i]) <= (x[j], y[j])} を {@code x[i] <= x[j] && y[i] <= y[j]}
     * で定める半順序において、最大サイズの antichain をひとつ返す。
     * 返り値の各要素は {@code int[] {x, y}} であり、入力に同じ点が複数ある場合でも
     * antichain には同じ点を高々ひとつしか含めない。
     *
     * @param x 各点の第1座標
     * @param y 各点の第2座標
     * @return 最大 antichain
     */
    public static int[][] maximumAntichain(int[] x, int[] y) {
    	//未テスト
    	//O(N log N)
    	if(x.length!=y.length) throw new AssertionError();
    	int N=x.length;
    	if(N==0)return new int[0][2];
    	int[] X=x.clone();
    	int[] Y=y.clone();
    	ArrayUtils.sort(X, Y);
    	int[] indices=ArrayStatistics.strictLDSIndices(Y);
    	int[][] ret=new int[indices.length][2];
    	for (int i = 0; i < indices.length; i++) {
    		int t=indices[i];
			ret[i]=new int[] {X[t], Y[t]};
		}
    	return ret;
    }

    /**
     * {@code (x[i], y[i]) <= (x[j], y[j])} を {@code x[i] <= x[j] && y[i] <= y[j]}
     * で定める半順序において、最大長の chain をひとつ返す。
     * 返り値の各要素は {@code int[] {x, y}} で、同じ点が複数ある場合は高々ひとつだけ使う。
     *
     * 計算量: O((N + max(y)) log max(y))
     * @param x 各点の第1座標。0以上10^6以下である必要がある
     * @param y 各点の第2座標。0以上10^6以下である必要がある
     * @return 最大 chain
     */
    public static int[][] longestChain(int[] x, int[] y) {
    	//verified:https://atcoder.jp/contests/abc369/tasks/abc369_f
    	int N=x.length;
    	int[] X=x.clone();
    	int[] Y=y.clone();
    	int maxY=ArrayUtils.max(Y);
    	if(ArrayUtils.max(x)>1e6) throw new AssertionError();
    	if(ArrayUtils.max(y)>1e6) throw new AssertionError();
    	if(ArrayUtils.min(x)<0) throw new AssertionError();
    	if(ArrayUtils.min(y)<0) throw new AssertionError();
    	
    	ArrayUtils.sort(X, Y);
    	var seg=SegTreeFactory.max(maxY+1);
    	int[]pre=new int[N];
    	int[]lastId=new int[maxY+1];
    	Arrays.fill(pre, -1);
    	Arrays.fill(lastId, -1);
    	for (int i = 0; i < N; i++) {
    		if(i>0&&X[i]==X[i-1]&&Y[i]==Y[i-1])continue;
    		final long max=seg.fold(0, Y[i]+1);
    		int arg=-1;
    		if(max>0) {
    			arg=seg.minimalLeft(Y[i], v->v<max);
    		}
    		seg.set(Y[i], Math.max(max, 0)+1);
    		if(arg!=-1) {
    			pre[i]=lastId[arg];
    		}
    		lastId[Y[i]]=i;
		}
    	int max=(int) seg.fold(0, maxY+1);
    	int arg=seg.minimalLeft(maxY+1, v->v<max);
    	int[][] ret=new int[max][];
    	int t = lastId[arg];
    	for (int i = 0; i < max; i++) {
			ret[max-1-i]=new int[] {X[t], Y[t]};
			t=pre[t];
    	}
    	return ret;
    }

    /**
     * 与えられた2次元の点群を、(狭義または広義の)最長増加部分列(LIS)に分解する。
     * 分解されたLIS(chain)の本数が最小となるような集合を返す。
     *
     * 狭義(strict = true)の場合、同一のLIS内の任意の2点 (x_i, y_i), (x_j, y_j) (i < j) に対して
     * x_i < x_j かつ y_i < y_j が成り立つ。
     * 広義(strict = false)の場合、同一のLIS内の任意の2点 (x_i, y_i), (x_j, y_j) (i < j) に対して
     * x_i <= x_j かつ y_i <= y_j が成り立つ。
     *
     * 計算量: O(N log N)
     *
     * @param x 各点の第1座標
     * @param y 各点の第2座標
     * @param strict 狭義のLISにする場合は true、広義のLISにする場合は false
     * @return 分解されたLISの配列。各LISは点の配列
     */
    public static long[][][] decomposeIntoLIS(long[] x, long[] y, boolean strict) {
    	//strict=falseの場合：https://atcoder.jp/contests/abc457/submissions/78098288
        // 未テスト
        if (x.length != y.length) throw new AssertionError();
        int N = x.length;
        if (N == 0) return new long[0][0][2];

        Integer[] id = new Integer[N];
        for (int i = 0; i < N; i++) {
            id[i] = i;
        }

        if (strict) {
            Arrays.sort(id, (i, j) -> {
                if (x[i] != x[j]) {
                    return Long.compare(x[i], x[j]);
                }
                return Long.compare(y[j], y[i]); // Y descending
            });
        } else {
            Arrays.sort(id, (i, j) -> {
                if (x[i] != x[j]) {
                    return Long.compare(x[i], x[j]);
                }
                return Long.compare(y[i], y[j]); // Y ascending
            });
        }

        java.util.TreeMap<Long, java.util.ArrayList<Integer>> tailYToChainIds = new java.util.TreeMap<>();
        java.util.ArrayList<java.util.ArrayList<long[]>> chains = new java.util.ArrayList<>();

        for (int i = 0; i < N; i++) {
            int idx = id[i];
            long px = x[idx];
            long py = y[idx];

            Long t;
            if (strict) {
                t = tailYToChainIds.lowerKey(py);
            } else {
                t = tailYToChainIds.floorKey(py);
            }

            if (t != null) {
                java.util.ArrayList<Integer> ids = tailYToChainIds.get(t);
                int chainId = ids.remove(ids.size() - 1);
                if (ids.isEmpty()) {
                    tailYToChainIds.remove(t);
                }
                chains.get(chainId).add(new long[] {px, py});
                tailYToChainIds.computeIfAbsent(py, k -> new java.util.ArrayList<>()).add(chainId);
            } else {
                int chainId = chains.size();
                java.util.ArrayList<long[]> newChain = new java.util.ArrayList<>();
                newChain.add(new long[] {px, py});
                chains.add(newChain);
                tailYToChainIds.computeIfAbsent(py, k -> new java.util.ArrayList<>()).add(chainId);
            }
        }

        long[][][] ret = new long[chains.size()][][];
        for (int i = 0; i < chains.size(); i++) {
            java.util.ArrayList<long[]> chainList = chains.get(i);
            ret[i] = new long[chainList.size()][2];
            for (int j = 0; j < chainList.size(); j++) {
                ret[i][j] = chainList.get(j);
            }
        }
        return ret;
    }
	
	
    static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
    
}
