package library.util.poset;

import java.util.Arrays;

import library.util.graph.DAG;
import library.util.graph.MaxFlow;
import library.util.graph.MinimumCostFlow;

public class Poset {
	/**
	 * 最大流になるらしいが、最小費用流で実装している。
	 * @param g
	 * @param A
	 * @return
	 */
	public static long weightedAntichain(DAG g, long[] A) {
		//https://atcoder.jp/contests/abc354/submissions/72136622
		if(g.N != A.length) throw new AssertionError();
		MinimumCostFlow mf=new MinimumCostFlow(2+2*g.N);
		int s=2*g.N;
		int t=s+1;
		long INF=Long.MAX_VALUE/3;
		for (int i=0;i<g.N;++i) {
			mf.addEdge(s, 2*i, 1, INF);
			mf.addEdge(2*i+1, t, 0, INF);
			mf.addEdge(2*i, 2*i+1, 0, INF);
			mf.addExcess(2*i+1, A[i]);
			mf.addExcess(2*i, -A[i]);
		}
		for (int i = 0; i < g.N; i++) {
			for (int j : g.adj[i]) {
				mf.addEdge(2*i+1, 2*j, 0, INF);
			}
		}
		mf.addEdge(t, s, 0, INF);
		long ret=mf.minCostCirculation();
		return ret;
	}
    
	
	
	public static int antichain(DAG g) {
		MaxFlow mf=new MaxFlow(2+2*g.N);
		int s=2*g.N;
		int t=s+1;
		for (int i=0;i<g.N;++i) {
			mf.addEdge(s, 2*i, 1);
			mf.addEdge(2*i+1, t, 1);
		}
		for (int i = 0; i < g.N; i++) {
			for (int j : g.adj[i]) {
				mf.addEdge(2*i, 2*j+1, 1);
			}
		}
		int flow=(int)mf.maxFlowValue(s, t);
		return g.N-flow;
	}
	
    static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
    
}
