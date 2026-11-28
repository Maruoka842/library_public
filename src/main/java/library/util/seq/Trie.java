package library.util.seq;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.function.LongBinaryOperator;

/**
 * 多重集合を保持する Trie。
 * 既定では英小文字(c - 'a')を想定し、int[] 版は任意の alphabetSize に対応する。
 */
public class Trie {
	private final int alphabetSize;
	private int nextId = 0;
	private int size = 0; // 相異なる登録文字列の個数

	public static class Node {
		final int c;
		int hit = 0;
		final Node[] child;
		public final int id;

		Node(int c, int id, int alphabetSize) {
			this.c = c;
			this.id = id;
			this.child = new Node[alphabetSize];
		}

		public Node getChild(char c) {
			return child[c - 'a'];
		}

		public int hit() {
			return hit;
		}
	}

	/** 根を除く全ノード（生成順） */
	public final ArrayList<Node> nodes = new ArrayList<>();

	private Node root;

	public Trie() {
		this(26);
	}

	public Trie(int alphabetSize) {
		this.alphabetSize = alphabetSize;
		this.root = newRoot();
	}

	private Node newRoot() {
		return new Node(-1, nextId++, alphabetSize);
	}

	private static int[] toInts(char[] s) {
		int[] a = new int[s.length];
		for (int i = 0; i < s.length; i++) a[i] = s[i] - 'a';
		return a;
	}

	private static int[] toInts(String s) {
		int[] a = new int[s.length()];
		for (int i = 0; i < a.length; i++) a[i] = s.charAt(i) - 'a';
		return a;
	}

	private boolean inRange(int c) {
		return 0 <= c && c < alphabetSize;
	}

	/**
	 * s に対応するノードを返す。存在しなければ null。
	 * 登録済み（hit &gt; 0）とは限らない。remove でノードは消えないため、hit = 0 のノードも返り得る。
	 * <p>計算量: O(|s|)</p>
	 */
	public Node get(int[] s) {
		Node cur = root;
		for (int c : s) {
			if (!inRange(c) || (cur = cur.child[c]) == null) return null;
		}
		return cur;
	}

	/**
	 * 登録された全ての列を削除し、根のみを持つ Trie に戻す。
	 * 事後条件: {@code size() = 0} かつ {@code root().id = 0}。
	 * 参照共有・所有権: 呼出し前に取得した {@link Node} への参照は新しい Trie に属さない。
	 * 計算量: O(alphabetSize)
	 */
	// 未テスト
	public void clear() {
		nodes.clear();
		nextId = 0;
		size = 0;
		root = newRoot();
	}

	public Node root() {
		return root;
	}

	/**
	 * s を1つ追加する。
	 * <p>計算量: O(|s|)</p>
	 * @return s に対応するノード。{@code hit() == 1} なら新規の文字列。
	 */
	public Node add(int[] s) {
		Node cur = root;
		for (int c : s) {
			Node next = cur.child[c];
			if (next == null) {
				next = cur.child[c] = new Node(c, nextId++, alphabetSize);
				nodes.add(next);
			}
			cur = next;
		}
		if (cur.hit++ == 0) size++;
		return cur;
	}

	public Node add(char[] s) {
		return add(toInts(s));
	}

	public Node add(String s) {
		return add(toInts(s));
	}

	public boolean contains(int[] s) {
		Node node = get(s);
		return node != null && node.hit > 0;
	}

	public boolean contains(char[] s) {
		return contains(toInts(s));
	}

	public boolean contains(String s) {
		return contains(toInts(s));
	}

	/**
	 * 指定された配列に対応する要素を1つ削除する。
	 * <p>計算量: O(|s|)。ノード自体は削除しない（{@code nodes} は縮まない）。</p>
	 * @param s 削除する要素（整数配列）
	 * @return 要素が存在して削除された場合は true、存在しない場合は false
	 */
	// 未テスト
	public boolean remove(int[] s) {
		Node node = get(s);
		if (node == null || node.hit == 0) return false;
		if (--node.hit == 0) size--;
		return true;
	}

	/**
	 * 指定された文字配列に対応する要素を1つ削除する。
	 * <p>計算量: O(|s|)</p>
	 * @return 要素が存在して削除された場合は true、存在しない場合は false
	 */
	// 未テスト
	public boolean remove(char[] s) {
		return remove(toInts(s));
	}

	/**
	 * 指定された文字列に対応する要素を1つ削除する。
	 * <p>計算量: O(|s|)</p>
	 * @return 要素が存在して削除された場合は true、存在しない場合は false
	 */
	// 未テスト
	public boolean remove(String s) {
		return remove(toInts(s));
	}

	/**
	 * テキストの接頭辞とマッチするパターンの最大長を返す。
	 * <p>計算量: O(|s|)</p>
	 * @param s テキスト配列
	 * @return max { |p| | p is a prefix of s and p is in this Trie }. マッチしない場合は 0。
	 */
	public int maxPrefixMatchLength(int[] s) {
		int res = 0;
		Node cur = root;
		for (int i = 0; i < s.length; i++) {
			int c = s[i];
			if (!inRange(c) || (cur = cur.child[c]) == null) break;
			if (cur.hit > 0) res = i + 1;
		}
		return res;
	}

	public int maxPrefixMatchLength(char[] s) {
		return maxPrefixMatchLength(toInts(s));
	}

	public int maxPrefixMatchLength(String s) {
		return maxPrefixMatchLength(toInts(s));
	}

	/**
	 * 登録されている相異なる列の個数を返す。
	 * <p>計算量: O(1)</p>
	 */
	public int size() {
		return size;
	}

	/**
	 * 根を含むノード数。ノード id は 0..nodeCount()-1 で連続し、親の id は常に子の id より小さい。
	 */
	public int nodeCount() {
		return nodes.size() + 1;
	}

	private Node node(int id) {
		return id == 0 ? root : nodes.get(id - 1);
	}

	/**
	 * 各ノード v について、v を根とする部分木（v 自身を含む）の A の総和を返す。
	 * res[v] = A[v] op res[c1] op res[c2] op ...（c1, c2, ... は v の子を文字の昇順に並べたもの）。
	 * <p>前提: op は結合的であればよい（前順序で畳み込む）。</p>
	 * <p>計算量: O(nodeCount() × alphabetSize)</p>
	 * @param A ノード id でインデックスされた値。A.length = nodeCount()
	 * @return ノード id でインデックスされた長さ nodeCount() の配列
	 */
	// 未テスト
	public long[] subtreeSum(long[] A, LongBinaryOperator op) {
		//https://atcoder.jp/contests/abc268/submissions/81031211
		int n = nodeCount();
		if (A.length != n) throw new IllegalArgumentException("A.length != nodeCount()");
		long[] res = Arrays.copyOf(A, n);
		for (int i = n - 1; i >= 0; i--) { // 子の id は親より大きいので、降順なら子が先に確定する
			Node v = node(i);
			for (Node ch : v.child) {
				if (ch != null) res[i] = op.applyAsLong(res[i], res[ch.id]);
			}
		}
		return res;
	}

	/**
	 * 各ノード v について、根から v までの経路上（両端を含む）の A の総和を返す。
	 * res[root] = A[root]、res[v] = res[parent(v)] op A[v]。
	 * <p>前提: op は結合的であればよい（根側から v 側の順に適用）。</p>
	 * <p>計算量: O(nodeCount() × alphabetSize)</p>
	 * @param A ノード id でインデックスされた値。A.length = nodeCount()
	 * @return ノード id でインデックスされた長さ nodeCount() の配列
	 */
	// 未テスト
	public long[] ancestorSum(long[] A, LongBinaryOperator op) {
		//https://atcoder.jp/contests/abc268/submissions/81031211
		int n = nodeCount();
		if (A.length != n) throw new IllegalArgumentException("A.length != nodeCount()");
		long[] res = Arrays.copyOf(A, n);
		for (int i = 0; i < n; i++) { // 親の id は子より小さいので、昇順なら親が先に確定する
			for (Node ch : node(i).child) {
				if (ch != null) res[ch.id] = op.applyAsLong(res[i], A[ch.id]);
			}
		}
		return res;
	}

	/**
	 * トライ木の状態を文字列として表す（BFS 順）。
	 * <p>計算量: O(nodes.size() × alphabetSize)</p>
	 */
	// 未テスト
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Trie size=").append(size).append(" totalNodes=").append(nodes.size() + 1);
		Queue<Node> que = new ArrayDeque<>();
		que.add(root);
		while (!que.isEmpty()) {
			Node cur = que.poll();
			sb.append("\nNode ").append(cur.id)
				.append(": char=").append(cur == root ? "#" : String.valueOf((char) ('a' + cur.c)))
				.append(" hit=").append(cur.hit).append(" [");
			boolean first = true;
			for (int i = 0; i < alphabetSize; i++) {
				Node ch = cur.child[i];
				if (ch == null) continue;
				if (!first) sb.append(", ");
				first = false;
				sb.append((char) ('a' + i)).append("->").append(ch.id);
				que.add(ch);
			}
			sb.append(']');
		}
		return sb.toString();
	}

	/**
	 * トライ木の状態を標準出力に出力する。
	 * <p>計算量: O(nodes.size() × alphabetSize)</p>
	 */
	// 未テスト
	public void dump() {
		System.out.println(this);
	}
}
