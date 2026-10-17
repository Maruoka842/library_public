package library.util.graph;

import library.util.graph.tree.*;

import java.awt.Component;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import library.tools.FastScanner;
import library.util.ArrayUtils;

/**
 * 無向グラフ
 */
public class DoubleValueDAG extends DoubleValueDigraph {
	/** トポロジカルソート順 */
	int[] order;
	/** 自然順序であることを示すフラグ */
	boolean isNaturalOrder;
	/** 逆自然順序であることを示すフラグ */
	boolean isNaturalReverseOrder;
	
	public DoubleValueDAG(int N) {
		super(N);
	}

	/**
	 * この DAG と同じ頂点集合、重み付き辺集合および既知のトポロジカル順序を持つ DAG を返す。
	 * 辺オブジェクト、隣接リストおよび順序配列を共有しない。
	 * O(N + M)
	 *
	 * @return この DAG の深いコピー
	 */
	@Override
	// 未テスト
	public DoubleValueDAG copy() {
		DoubleValueDAG ret = new DoubleValueDAG(N);
		ret.M = M;
		for (int i = 0; i < N; i++) {
			for (DoubleValueEdge e : adj[i]) {
				ret.adj[i].add(new DoubleValueEdge(e.src, e.dst, e.cost));
			}
		}
		ret.isNaturalOrder = isNaturalOrder;
		ret.isNaturalReverseOrder = isNaturalReverseOrder;
		ret.order = order == null ? null : order.clone();
		return ret;
	}
	
	public static DoubleValueDAG ofNaturalOrder(int N) {
		DoubleValueDAG g = new DoubleValueDAG(N);
		g.isNaturalOrder = true;
		g.order = new int[N];
		for (int i = 0; i < N; i++) g.order[i] = i;
		return g;
	}

	/**
	 * 逆自然順序 (from > to) のトポロジカル順序を持つ DAG を構築する。
	 * 各頂点 i (0 <= i < N) に対して、初期順序は N - 1 - i となる。
	 * 計算量: O(N)
	 * 未テスト。
	 *
	 * @param N 頂点数
	 * @return 構築された DAG インスタンス
	 */
	public static DoubleValueDAG ofNaturalReverseOrder(int N) {
		DoubleValueDAG g = new DoubleValueDAG(N);
		g.isNaturalReverseOrder = true;
		g.order = new int[N];
		for (int i = 0; i < N; i++) g.order[i] = N - 1 - i;
		return g;
	}
	
	@Override
	public void addEdge(int from, int to, double cost) {
		if (isNaturalOrder && from >= to) throw new AssertionError("natural order DAG requires from < to: " + from + " -> " + to);
		if (isNaturalReverseOrder && from <= to) throw new AssertionError("natural reverse order DAG requires from > to: " + from + " -> " + to);
		super.addEdge(from, to, cost);
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
			for (var e : adj[i]) {
				dp[i]=Math.max(dp[i], dp[e.dst]+1);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、任意のシンク（出次数が0の頂点）を終点とする最短パスの辺数を求める。
	 * 始点がシンクである場合は 0 と定義される。
	 * 各頂点 i に対する最短パスの辺数を dp[i] とすると、dp[i] = min_(e) (dp[e.dst] + 1) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最短パス長を格納した配列
	 */
	public int[] shortestPathLengthForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		int[] dp = new int[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			if (adj[i].isEmpty()) {
				dp[i] = 0;
			} else {
				dp[i] = Integer.MAX_VALUE;
				for (var e : adj[i]) {
					dp[i] = Math.min(dp[i], dp[e.dst] + 1);
				}
			}
		}
		return dp;
	}
	
	/**
	 * dp[s]=sを始点としたときの最長パスコストを返す。
	 * 辺のcostの総和を最大化する。
	 * 長さ0のパスも候補に含む。
	 * 未テスト。
	 * @return
	 */
	public double[] longestPathCostForEachStart() {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				dp[i]=Math.max(dp[i], dp[e.dst]+e.cost);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、任意のシンク（出次数が0の頂点）を終点とする最短パスのコストの総和を求める。
	 * 始点がシンクである場合は 0.0 と定義される。
	 * 各頂点 i に対する最短パスの最小コストを dp[i] とすると、dp[i] = min_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @return 各頂点からの最短パス最小コストを格納した配列
	 */
	public double[] shortestPathCostForEachStart() {
		// 未テスト
		if (order == null) order = topologicalOrder();
		double[] dp = new double[N];
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			if (adj[i].isEmpty()) {
				dp[i] = 0;
			} else {
				dp[i] = Double.POSITIVE_INFINITY;
				for (var e : adj[i]) {
					dp[i] = Math.min(dp[i], dp[e.dst] + e.cost);
				}
			}
		}
		return dp;
	}
	
	/**
	 * dp[s]=sを始点、endを終点としたときの最長パスコストを返す。
	 * endに到達できない頂点の値はDouble.NEGATIVE_INFINITY。
	 * dp[end]=0。
	 * @param end
	 * @return
	 */
	public double[] longestPathCostForEachStartFixingEnd(int end) {
		//https://atcoder.jp/contests/abc324/submissions/75180824
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		Arrays.fill(dp, Double.NEGATIVE_INFINITY);
		dp[end]=0;
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				if (dp[e.dst] == Double.NEGATIVE_INFINITY) continue;
				dp[i]=Math.max(dp[i], dp[e.dst]+e.cost);
			}
		}
		return dp;
	}

	/**
	 * 各頂点 s を始点、指定された終点 end を固定したときの最短パスコストの総和を求める。
	 * 終点 end に到達不可能な頂点の値は Double.POSITIVE_INFINITY と定義される。
	 * 終点自体の値は dp[end] = 0.0 となり、それ以外の頂点 i に対しては dp[i] = min_(e) (dp[e.dst] + e.cost) で定義される。
	 * 計算量: O(N + M)
	 * 未テスト。
	 *
	 * @param end 終点頂点のインデックス
	 * @return 各頂点からの最短パス最小コストを格納した配列
	 */
	public double[] shortestPathCostForEachStartFixingEnd(int end) {
		// 未テスト
		if (order == null) order = topologicalOrder();
		double[] dp = new double[N];
		Arrays.fill(dp, Double.POSITIVE_INFINITY);
		dp[end] = 0;
		for (int k = N - 1; k >= 0; k--) {
			int i = order[k];
			for (var e : adj[i]) {
				if (dp[e.dst] == Double.POSITIVE_INFINITY) continue;
				dp[i] = Math.min(dp[i], dp[e.dst] + e.cost);
			}
		}
		return dp;
	}
	
	/**
	 * dp[i]=srcを始点としてランダムウォークしたとき頂点iを通る確率
	 * 辺ijのコストcを、iからjに移動する確率と見なして計算する
	 * @param src
	 * @return
	 */
	public double[] visitProbabilityFrom(int src) {
		if (order == null) order = topologicalOrder();
		double[]dp=new double[N];
		dp[src]=1;
		for (int i:order) {
			for (var e: adj[i]) {
				dp[e.dst]+=dp[i]*e.cost;
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
			for (var e : adj[i]) {
				dp[i]+=dp[e.dst]*e.cost;
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
			for (var e: adj[i]) {
				dp[e.dst]+=(dp[i]+f[i])*e.cost;
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
		DoubleValueDAG ig=reverse();
		double[] f=visitProbabilityTo(dst);
		for (int i:ig.order) {
			for (var e: adj[i]) {
				dp[i]+=(dp[e.dst]+f[e.dst])*e.cost;
			}
		}
		return dp;
	}
	
	public DoubleValueDAG reverse() {
		DoubleValueDAG g = new DoubleValueDAG(N);
		for (int i = 0; i < N; ++i) {
			for (var e : adj[i]) {
				g.addEdge(e.dst, i, e.cost);
			}
		}
		if (order == null) order = topologicalOrder();
		g.order = Arrays.copyOf(order, N);
		ArrayUtils.reverse(g.order);
		return g;
	}


	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
