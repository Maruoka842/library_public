package library.util;

import java.util.BitSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 状態は {@code 0, 1, ..., n-1}、入力記号は {@code 0, 1, ..., alphabetSize-1}、
 * 初期状態は {@code 0} とする非決定性有限オートマトン。
 */
public class NFA {
	int n;
	int alphabetSize;
	// 遷移先の集合を状態・記号ごとに保持する。
	BitSet[][] adj;
	// 受理状態を保持する。
	boolean[] isAcceptable;

	/**
	 * 状態数 {@code n}、アルファベットサイズ {@code alphabetSize} の NFA を作る。
	 * 状態番号は {@code 0..n-1}、初期状態は {@code 0}。
	 *
	 * @param n 状態数
	 * @param alphabetSize アルファベットサイズ
	 * @implNote 未テスト。時間・空間計算量は {@code O(n * alphabetSize)}。
	 */
	public NFA(int n, int alphabetSize) {
		this.n = n;
		this.alphabetSize = alphabetSize;
		adj = new BitSet[n][alphabetSize];
		for (int state = 0; state < n; state++) {
			for (int symbol = 0; symbol < alphabetSize; symbol++) {
				adj[state][symbol] = new BitSet(n);
			}
		}
		isAcceptable = new boolean[n];
	}

	/**
	 * 状態数を返す。
	 *
	 * @return 状態数
	 * @implNote 未テスト。計算量は {@code O(1)}。
	 */
	public int size() {
		return n;
	}

	/**
	 * 状態 {@code v} を受理状態にする。
	 *
	 * @param v 状態番号
	 * @implNote 未テスト。計算量は {@code O(1)}。
	 */
	public void setAcceptable(int v) {
		isAcceptable[v] = true;
	}

	/**
	 * 記号 {@code symbol} による {@code from} から {@code to} への遷移を追加する。
	 *
	 * @param from 遷移元
	 * @param to 遷移先
	 * @param symbol 入力記号
	 * @implNote 未テスト。計算量は {@code O(1)}。
	 */
	public void addEdge(int from, int to, int symbol) {
		adj[from][symbol].set(to);
	}

	/**
	 * 記号 {@code symbol} で状態 {@code from} から到達できる状態集合を返す。
	 *
	 * @param from 遷移元
	 * @param symbol 入力記号
	 * @return 遷移先集合の複製
	 * @implNote 未テスト。計算量は {@code O(n / w)}。{@code w} はワード幅。
	 */
	public BitSet next(int from, int symbol) {
		return (BitSet) adj[from][symbol].clone();
	}

	/**
	 * 初期状態から入力列を実行し、受理状態に到達可能か判定する。
	 * 入力列 {@code a[0..L-1]} の到達状態集合は {@code S[0]={0}}、
	 * {@code S[i+1]=union_{q in S[i]} delta(q,a[i])}。
	 *
	 * @param input 入力記号列
	 * @return {@code S[input.length]} と受理状態集合の共通部分が空でない場合 {@code true}
	 * @implNote 未テスト。時間計算量は {@code O(input.length * n^2 / w)}、
	 * 空間計算量は {@code O(n / w)}。{@code w} はワード幅。
	 */
	public boolean accepts(int[] input) {
		BitSet current = new BitSet(n);
		current.set(0);
		for (int symbol : input) {
			BitSet next = new BitSet(n);
			for (int state = current.nextSetBit(0); state >= 0; state = current.nextSetBit(state + 1)) {
				next.or(adj[state][symbol]);
			}
			current = next;
			if (current.isEmpty()) return false;
		}
		for (int state = current.nextSetBit(0); state >= 0; state = current.nextSetBit(state + 1)) {
			if (isAcceptable[state]) return true;
		}
		return false;
	}

	/**
	 * 受理状態かどうかを返す。
	 *
	 * @param v 状態番号
	 * @return 状態 {@code v} が受理状態なら {@code true}
	 * @implNote 未テスト。計算量は {@code O(1)}。
	 */
	public boolean isAcceptable(int v) {
		return isAcceptable[v];
	}

	/**
	 * 部分集合構成により、同じ言語を受理する DFA を作る。
	 * DFA の状態は NFA 状態集合 {@code S} に対応し、遷移は
	 * {@code deltaD(S,a)=union_{q in S} delta(q,a)}、受理条件は
	 * {@code S} が NFA の受理状態を含むこと。初期状態は {@code {0}}。
	 * 到達可能な部分集合だけを DFA の状態として列挙する。
	 *
	 * @return この NFA と同じ言語を受理する DFA
	 * @implNote 未テスト。到達可能な部分集合数を {@code R <= 2^n} とすると、
	 * 時間計算量は {@code O(alphabetSize * R * n^2 / w)}、
	 * 空間計算量は {@code O(alphabetSize * R + R * n / w)}。{@code w} はワード幅。
	 */
	public DFA toDFA() {
		//https://atcoder.jp/contests/abc484/submissions/80866329
		List<BitSet> subsets = new ArrayList<>();
		Map<BitSet, Integer> id = new HashMap<>();
		List<int[]> transitions = new ArrayList<>();//transitions[dfaState][symbol]
		BitSet initial = new BitSet(n);
		initial.set(0);
		subsets.add(initial);
		id.put(initial, 0);

		for (int index = 0; index < subsets.size(); index++) {
			BitSet subset = subsets.get(index);
			int[] row = new int[alphabetSize];
			for (int symbol = 0; symbol < alphabetSize; symbol++) {
				// 現在の状態集合subsetに含まれるすべてのNFA状態から、
				// 入力記号symbolで遷移できる状態を集める。
				BitSet destination = new BitSet(n);
				for (int state = subset.nextSetBit(0); state >= 0; state = subset.nextSetBit(state + 1)) {
					destination.or(adj[state][symbol]);
				}
				Integer nextId = id.get(destination);
				if (nextId == null) {
					nextId = subsets.size();
					BitSet key = destination;
					subsets.add(key);
					id.put(key, nextId);
				}
				row[symbol] = nextId;
			}
			transitions.add(row);
		}

		DFA dfa = new DFA(subsets.size(), alphabetSize);
		for (int state = 0; state < subsets.size(); state++) {
			BitSet subset = subsets.get(state);
			for (int q = subset.nextSetBit(0); q >= 0; q = subset.nextSetBit(q + 1)) {
				if (isAcceptable[q]) {
					dfa.setAcceptable(state);
					break;
				}
			}
			for (int symbol = 0; symbol < alphabetSize; symbol++) {
				dfa.addEdge(state, transitions.get(state)[symbol], symbol);
			}
		}
		return dfa;
	}

	/**
	 * この NFA の状態と遷移を GraphStream で表示する。受理状態は緑色で表示し、
	 * 頂点の緑色と太い枠線はいずれも受理状態を表す。同じ始点・終点を持つ遷移の記号は
	 * 一つの辺ラベルにまとめる。
	 *
	 * @implNote 未テスト。{@code w} をワード幅、{@code e} を遷移数とすると、
	 * 時間計算量は {@code O(n * alphabetSize * n / w + e)}、空間計算量は {@code O(n + e)}。
	 */
	public void draw() {
		System.setProperty("org.graphstream.ui", "swing");
		org.graphstream.graph.Graph graph = new org.graphstream.graph.implementations.SingleGraph("NFA");
		graph.setAttribute("ui.stylesheet", """
			node {
				fill-color: white;
				size: 35px;
				text-alignment: center;
				text-size: 18;
				stroke-mode: plain;
				stroke-color: black;
				stroke-width: 1px;
				z-index: 1;
			}
			node.accept {
				stroke-width: 4px;
				fill-color: #E0FFE0;
			}
			edge {
				fill-color: black;
				text-size: 15px;
				text-background-mode: plain;
				text-background-color: white;
				arrow-size: 10px, 6px;
				shape: cubic-curve;
				text-alignment: center;
				text-offset: 8px;
			}
			edge.loop {
				text-offset: 20px;
			}
			""");

		for (int state = 0; state < n; state++) {
			org.graphstream.graph.Node node = graph.addNode(String.valueOf(state));
			node.setAttribute("ui.label", String.valueOf(state));
			if (isAcceptable[state]) node.setAttribute("ui.class", "accept");
		}

		for (int from = 0; from < n; from++) {
			Map<Integer, List<Integer>> symbolsByDestination = new HashMap<>();
			for (int symbol = 0; symbol < alphabetSize; symbol++) {
				for (int to = adj[from][symbol].nextSetBit(0); to >= 0; to = adj[from][symbol].nextSetBit(to + 1)) {
					symbolsByDestination.computeIfAbsent(to, key -> new ArrayList<>()).add(symbol);
				}
			}
			for (Map.Entry<Integer, List<Integer>> entry : symbolsByDestination.entrySet()) {
				int to = entry.getKey();
				String edgeId = from + "->" + to;
				org.graphstream.graph.Edge edge = graph.addEdge(edgeId, String.valueOf(from), String.valueOf(to), true);
				List<Integer> symbols = entry.getValue();
				StringBuilder label = new StringBuilder();
				for (int i = 0; i < symbols.size(); i++) {
					if (i > 0) label.append(',');
					label.append(symbols.get(i));
				}
				edge.setAttribute("ui.label", label.toString());
				if (from == to) edge.setAttribute("ui.class", "loop");
			}
		}
		graph.display();
	}
}
