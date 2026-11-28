package library.util.seq;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 複数文字列の suffix を葉とする圧縮 trie。
 * 入力文字列を区切り文字なしで連結し、LCP を各文字列の末尾で打ち切って構築する。
 */
public class GeneralizedSuffixTree {
	/** 入力文字列を順に連結した配列。 */
	public final char[] S;
	/** 連結文字列 S の各位置 i が属する入力文字列の番号。 */
	public final int[] stringId;
	/** 連結文字列 S の各位置 i から、その位置が属する入力文字列の末尾までの文字数（位置 i 自身を含む）。 */
	public final int[] remaining;
	/** 根を含む頂点数。 */
	public final int N;
	/** 根から頂点 v までの文字列長。 */
	public final int[] rangeLCP;
	/** 頂点 v が表す文字列の出現開始位置。 */
	public final int[] startPos;
	/** 頂点 v の部分木に属する入力 suffix 数。 */
	public final int[] suffixCount;
	/** 頂点 v の最初の子。 */
	public final int[] firstChild;
	/** 頂点 v の次の兄弟。 */
	public final int[] nextSibling;
	/** 頂点 v の親。 */
	public final int[] parent;
	/** suffixNode[i] は、連結文字列 S の位置 i を開始位置とする suffix の葉の頂点番号。 */
	private final int[] suffixNode;
	/** stringStart[i] は、入力文字列 i の連結文字列 S における開始位置。 */
	private final int[] stringStart;

	/**
	 * 入力文字列の全 suffix を格納する圧縮 trie を構築する。
	 * 構築計算量: 合計長を M として O(M log M)、空間計算量: O(M)。
	 * @param strings 入力文字列列
	 */
	// 未テスト
	public GeneralizedSuffixTree(ArrayList<char[]> strings) {
		int m = 0;
		for (char[] s : strings) m += s.length;
		S = new char[m];
		stringId = new int[m];
		remaining = new int[m];
		stringStart = new int[strings.size()];
		int at = 0;
		for (int id = 0; id < strings.size(); id++) {
			char[] s = strings.get(id);
			stringStart[id] = at;
			System.arraycopy(s, 0, S, at, s.length);
			for (int i = 0; i < s.length; i++) {
				stringId[at + i] = id;
				remaining[at + i] = s.length - i;
			}
			at += s.length;
		}
		// 各文字列の後ろに 'a' より小さい区切り文字 '`' を入れた配列で SA を作る
		char[] t = new char[m + strings.size()];
		int[] toS = new int[t.length]; // t の位置 → S の位置(区切りは -1)
		int w = 0, ts = 0;
		for (char[] s : strings) {
			for (char c : s) {
				toS[w] = ts++;
				t[w++] = c;
			}
			toS[w] = -1;
			t[w++] = '`';
		}
		int[] saT = t.length == 0 ? new int[0] : StringUtils.suffixArray(t);
		int[] lcpT = t.length == 0 ? new int[0] : StringUtils.lcpArray(t, saT);
		// 区切りで始まる suffix は SA の先頭に固まる。それを除き、S 上の位置に戻す
		int[] sa = new int[m];
		int[] lcp = new int[m];
		int k = 0;
		for (int i = 0; i < saT.length; i++) {
			int p = toS[saT[i]];
			if (p < 0) continue;
			sa[k] = p;
			lcp[k] = k == 0 ? 0 : Math.min(lcpT[i], Math.min(remaining[sa[k - 1]], remaining[p]));
			k++;
		}
		int cap = Math.max(1, 2 * S.length + 1);
		int[] p = new int[cap], depth = new int[cap], start = new int[cap];
		Arrays.fill(p, -1);
		int size = 1;
		int cur = 0;
		for (int i = 0; i < sa.length; i++) {
			int common = i == 0 ? 0 : lcp[i];
			int previous = -1;
			while (depth[cur] > common) {
				previous = cur;
				cur = p[cur];
			}
			if (depth[cur] < common) {
				int internal = size++;
				p[internal] = p[previous];
				depth[internal] = common;
				start[internal] = start[previous];
				p[previous] = internal;
				cur = internal;
			}
			int leaf = size++;
			p[leaf] = cur;
			depth[leaf] = remaining[sa[i]];
			start[leaf] = sa[i];
			cur = leaf;
		}
		parent = Arrays.copyOf(p, size);
		rangeLCP = Arrays.copyOf(depth, size);
		startPos = Arrays.copyOf(start, size);
		N = size;
		firstChild = new int[N];
		nextSibling = new int[N];
		Arrays.fill(firstChild, -1);
		Arrays.fill(nextSibling, -1);
		for (int v = N - 1; v >= 1; v--) {
			nextSibling[v] = firstChild[parent[v]];
			firstChild[parent[v]] = v;
		}
		suffixCount = new int[N];
		int[] order = new int[N];
		int ptr = 0;
		order[ptr++] = 0;
		for (int i = 0; i < ptr; i++) {
			for (int u = firstChild[order[i]]; u != -1; u = nextSibling[u]) order[ptr++] = u;
		}
		for (int i = N - 1; i >= 0; i--) {
			int v = order[i];
			suffixCount[v] = v > 0 && depth[v] == remaining[start[v]] ? 1 : 0;
			for (int u = firstChild[v]; u != -1; u = nextSibling[u]) suffixCount[v] += suffixCount[u];
		}
		suffixNode = new int[m + 1];
		Arrays.fill(suffixNode, -1);
		for (int v = 1; v < N; v++) if (suffixCount[v] == 1 && depth[v] == remaining[start[v]]) suffixNode[start[v]] = v;
	}

	/**
	 * 頂点 v が表す文字列を返す。
	 * 計算量: O(|S(v)|)。
	 * @param v 頂点番号
	 * @return S[startPos[v]:startPos[v]+rangeLCP[v])
	 */
	// 未テスト
	public String getString(int v) {
		return v == 0 ? "" : new String(S, startPos[v], rangeLCP[v]);
	}

	/**
	 * 頂点 v に対応する入力文字列番号を返す。v が suffix 葉でない場合は -1。
	 * 計算量: O(1)。
	 * @param v 頂点番号
	 * @return 入力文字列番号、または -1
	 */
	// 未テスト
	public int stringIdOfSuffix(int v) {
		if (v == 0 || rangeLCP[v] != remaining[startPos[v]]) return -1;
		return stringId[startPos[v]];
	}

	/**
	 * suffix 葉 v の入力文字列内開始位置を返す。
	 * 計算量: O(1)。
	 * @param v suffix 葉
	 * @return 入力文字列内の開始位置
	 */
	// 未テスト
	public int indexInString(int v) {
		int id = stringIdOfSuffix(v);
		return id < 0 ? -1 : startPos[v] - stringStart[id];
	}
}
