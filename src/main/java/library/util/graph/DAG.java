package library.util.graph;

import java.util.Arrays;

import library.tools.FastScanner;
import library.util.ArrayUtils;
import library.util.collections.IntArrayList;

/**
 * 無向グラフ
 */
public class DAG extends Digraph {
	int[] order;
	boolean isNaturalOrder;
	
	public DAG(int N) {
		//DAGのreverseでN=0で生成してから手動で作ってるので変える時は注意
		super(N);
	}

	/**
	 * この DAG と同じ頂点集合、辺集合および既知のトポロジカル順序を持つ DAG を返す。
	 * 内部の隣接リストと順序配列を共有しない。
	 * O(N + M)
	 *
	 * @return この DAG の深いコピー
	 */
	@Override
	// 未テスト
	public DAG copy() {
		DAG ret = new DAG(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			ret.adj[i].addAll(adj[i]);
		}
		ret.isNaturalOrder = isNaturalOrder;
		ret.order = order == null ? null : order.clone();
		return ret;
	}
	
	public static DAG ofNaturalOrder(int N) {
		DAG g = new DAG(N);
		g.isNaturalOrder = true;
		g.order = new int[N];
		for (int i = 0; i < N; i++) g.order[i] = i;
		return g;
	}
	
	public static DAG read(int N, int M) {
		DAG g = new  DAG(N);
		FastScanner sc=FastScanner.getInstance();
		for (int i = 0; i < M; i++) {
			int u=sc.nextInt()-1;
			int v=sc.nextInt()-1;
			g.addEdge(u, v);
		}
		return g;
	}
	
	@Override
	public void addEdge(int from, int to) {
		if (isNaturalOrder && from >= to) throw new AssertionError("natural order DAG requires from < to: " + from + " -> " + to);
		super.addEdge(from, to);
	}
	
	public int minPathCoverSize() {
		//https://atcoder.jp/contests/abc237/submissions/74334147
		BipartiteMatching matching=new BipartiteMatching(N, N);
		for (int i = 0; i < N; i++) {
			for (int j : adj[i]) {
				matching.addEdge(i, j);
			}
		}
		return N-matching.calc();
	}
	
	public int[] topologicalOrder() {
		if (order == null) order = super.topologicalOrder();
		return order;
	}
	
	/**
	 * dp[s]=sを始点としたときの最長パス長を返す。
	 * パス長は辺数で数える。
	 * 未テスト。
	 * @return
	 */
	public int[] longestPathLengthForEachStart() {
		if (order == null) order = topologicalOrder();
		int[]dp=new int[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (int j : adj[i]) {
				dp[i]=Math.max(dp[i], dp[j]+1);
			}
		}
		return dp;
	}
	
	public long[] countPath_long(int src) {
		if (order == null) order = topologicalOrder();
		long[]dp=new long[N];
		dp[src]=1;
		for (int i:order) {
			for (int j : adj[i]) {
				dp[j]+=dp[i];
			}
		}
		return dp;
	}
	
	public long[] countPath(int src, long mod) {
		//https://atcoder.jp/contests/ndpc/submissions/75520712
		if (order == null) order = topologicalOrder();
		long[]dp=new long[N];
		dp[src]=1;
		for (int i:order) {
			for (int j : adj[i]) {
				dp[j] += dp[i];
				dp[j] %= mod;
			}
		}
		return dp;
	}
	
	public double[] countPath_double(int src) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		dp[src]=1;
		for (int i:order) {
			for (int j : adj[i]) {
				dp[j]+=dp[i];
			}
		}
		return dp;
	}
	
	/**
	 * dp[i]=全src-iパスの長さの総和
	 * @param src
	 * @return
	 */
	public double[] countPathLength_double(int src) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		double[]path=countPath_double(src);
		for (int i:order) {
			for (int j : adj[i]) {
				dp[j] += dp[i] + path[i];
			}
		}
		return dp;
	}
	
	/**
	 * dp[i]=srcを始点としてランダムウォークしたとき頂点iを通る確率
	 * @param src
	 * @return
	 */
	public double[] visitProbabilityFrom(int src) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		dp[src]=1;
		for (int i:order) {
			double d=outDegree(i);
			for (int j : adj[i]) {
				dp[j]+=dp[i]/d;
			}
		}
		return dp;
	}
	
	/**
	 * dp[i]=iを始点としてランダムウォークしたとき頂点dstを通る確率
	 * @param src
	 * @return
	 */
	public double[] visitProbabilityTo(int dst) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		var ig=reverse();
		dp[dst]=1;
		for (int i:ig.order) {
			double d=outDegree(i);
			for (int j : adj[i]) {
				dp[i]+=dp[j]/d;
			}
		}
		return dp;
	}
	
	
	/**
	 * 各src-iパスPに対して
	 * dp[i]=(始点srcからランダムウォークでPを取る確率)×(Pの長さ)
	 * @param src
	 * @return
	 */
	public double[] expectedPathLengthFrom(int src) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		double[] f=visitProbabilityFrom(src);
		for (int i:order) {
			double d=outDegree(i);
			for (int j : adj[i]) {
				dp[j]+=dp[i]/d+f[i]/d;
			}
		}
		return dp;
	}

	/**
	 * 各i-dstパスPに対して
	 * dp[i]=(始点iからランダムウォークでPを取る確率)×(Pの長さ)
	 * @param dst
	 * @return
	 */
	public double[] expectedPathLengthTo(int dst) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		DAG ig=reverse();
		double[] f=visitProbabilityTo(dst);
		for (int i:ig.order) {
			double d=outDegree(i);
			for (int j : adj[i]) {
				dp[i]+=dp[j]/d+f[j]/d;
			}
		}
		return dp;
	}
	
	public long[] vertexValueMaxPathCostFrom(long[] cost) {
		if (order == null) order = topologicalOrder();
		long[]dp=new long[N];
		for (int i = 0; i < N; i++) {
			dp[i] = cost[i];
		}
		for (int i = order.length - 1; i >= 0; i--) {
			int from = order[i];
			for (int to : adj[from])
			dp[from] = Math.max(dp[from], cost[from] + dp[to]);
		}
		return dp;
	}
	
	
	
	
	
	public DAG reverse() {
		DAG g = new DAG(0);
		g.adj = new IntArrayList[N];
		g.N = N;
		int[]indeg=inDegrees();
		for (int i = 0; i < N; i++) {
			g.adj[i]=new IntArrayList(indeg[i]);
		}
		for (int i = 0; i < N; ++i) {
			for (int j : adj[i]) {
				g.addEdge(j, i);
			}
		}
		if (order != null) {
			g.order = Arrays.copyOf(order, N);
			ArrayUtils.reverse(g.order);
		}
		return g;
	}



	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
