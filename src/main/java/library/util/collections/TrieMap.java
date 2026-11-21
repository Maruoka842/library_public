package library.util.collections;

import java.util.ArrayList;
import java.util.List;

public class TrieMap {
	public class Node {
		int c;
		boolean hit = false;
		Node[] child = new Node[26];
		long value;
		
		public Node(char c) {
			this.c = (int) (c - 'a');
		}
		
		public Node(int c) {
			this.c = c;
		}

		/**
		 * 添字 c に対応する子ノードを返す。
		 * <p>計算量: O(1)。</p>
		 * @param c 子ノードの添字
		 * @return 子ノード。存在しない場合は null
		 */
		// 未テスト
		public Node next(int c) {
			return child[c];
		}

		/**
		 * 添字 c に対応する子ノードを返し、存在しない場合は生成する。
		 * <p>計算量: O(1)。</p>
		 * @param c 子ノードの添字
		 * @return 添字 c に対応する子ノード
		 */
		// 未テスト
		public Node nextOrCreate(int c) {
			if (child[c] == null) child[c] = new Node(c);
			return child[c];
		}
		
		public Node nextOrCreate(char c) {
			return nextOrCreate(c-'a');
		}

		/**
		 * このノードに値を設定し、対応するキーを登録する。未登録のキーの場合はkeyCount++される。
		 * <p>計算量: O(1)。</p>
		 * @param value 設定する値
		 */
		// 未テスト
		public void setValue(long value) {
			if (!hit) ++keyCount;
			this.value = value;
			hit = true;
		}

		/**
		 * このノードに対応するキーの値を返す。
		 * <p>計算量: O(1)。</p>
		 * @return キーに設定された値
		 * @throws AssertionError キーが登録されていない場合
		 */
		// 未テスト
		public long getValue() {
			if (!hit) throw new AssertionError();
			return value;
		}

		/**
		 * このノードに対応するキーの値を返し、キーが未登録なら defaultValue を返す。
		 * <p>計算量: O(1)。</p>
		 * @param defaultValue キーが未登録の場合に返す値
		 * @return 登録値、または defaultValue
		 */
		// 未テスト
		public long getOrDefault(long defaultValue) {
			return hit ? value : defaultValue;
		}

		/**
		 * このノードに値が設定されているかを返す。
		 * <p>計算量: O(1)。</p>
		 * @return 値が設定されていれば true
		 */
		// 未テスト
		public boolean hasValue() {
			return hit;
		}

		/**
		 * このノードに対応するキーを削除する。
		 * <p>計算量: O(1)。</p>
		 * @return キーが登録されていて削除された場合は true、そうでなければ false
		 */
		// 未テスト
		public boolean removeValue() {
			if (!hit) return false;
			hit = false;
			--keyCount;
			return true;
		}
	}
	
	public Node root() {
		return root;
	}

	/** 登録済みキーの接頭辞に対応するノードとキー長。 */
	public record Prefix(Node node, int length) {}
	
	
	Node root = new Node('#');
	// 登録されている相異なるキーの個数。
	int keyCount = 0;

	/**
	 * トライ木を空にする。
	 * O(1)
	 * // 未テスト
	 */
	public void clear() {
		root = new Node('#');
		keyCount = 0;
	}
	
	public void put(char[] s, long value) {
		int[] a=new int[s.length];
		for(int i=0;i<s.length;++i)a[i]=s[i]-'a';
		put(a, value);
	}
	
	public void put(int[] s, long value) {
		Node cur = root;
		for (int i = 0; i < s.length; ++i) {
			cur = cur.nextOrCreate(s[i]);
		}
		cur.setValue(value);
	}
	
	public boolean contains(int[] s) {
		Node cur = root;
		for (int i = 0; i < s.length; ++i) {
			if (cur.child[s[i]] == null) return false;
			cur = cur.child[s[i]];
		}
		return cur.hit;
	}

	/**
	 * 指定された配列のキーを削除する。
	 * <p>計算量: O(|s|)</p>
	 * @param s 削除するキー
	 * @return キーが存在して削除された場合は true、存在しない場合は false
	 */
	// 未テスト
	public boolean remove(int[] s) {
		Node cur = root;
		for (int c : s) {
			if (c < 0 || c >= cur.child.length || cur.child[c] == null) return false;
			cur = cur.child[c];
		}
		return cur.removeValue();
	}

	/**
	 * 指定された文字配列のキーを削除する。
	 * <p>計算量: O(|s|)</p>
	 * @param s 削除するキー
	 * @return キーが存在して削除された場合は true、存在しない場合は false
	 */
	// 未テスト
	public boolean remove(char[] s) {
		int[] a = new int[s.length];
		for (int i = 0; i < s.length; ++i) a[i] = s[i] - 'a';
		return remove(a);
	}
	
	
	public long get(char[] s) {
		Node cur = root;
		for (int i = 0; i < s.length; ++i) {
			if (cur.child[s[i]-'a'] == null) throw new AssertionError();
			cur = cur.child[s[i]-'a'];
		}
		return cur.getValue();
	}
	
	public long get(int[] s) {
		Node cur = root;
		for (int i = 0; i < s.length; ++i) {
			if (cur.child[s[i]] == null) throw new AssertionError();
			cur = cur.child[s[i]];
		}
		return cur.getValue();
	}
	
	
	public boolean contains(char[] s) {
		int[] a=new int[s.length];
		for(int i=0;i<s.length;++i)a[i]=s[i]-'a';
		return contains(a);
	}	

	/**
	 * a の接頭辞である登録済みキーに対応するノードと長さを、長さの昇順で列挙する。
	 * <p>計算量: O(|a|)。</p>
	 * @param a 接頭辞を調べる配列
	 * @return a の接頭辞である登録済みキーのノードと長さ
	 */
	// 未テスト
	public List<Prefix> prefixesOf(int[] a) {
		List<Prefix> result = new ArrayList<>();
		Node cur = root;
		if (cur.hit) result.add(new Prefix(cur, 0));
		for (int i = 0; i < a.length; ++i) {
			int c = a[i];
			if (c < 0 || c >= cur.child.length || cur.child[c] == null) break;
			cur = cur.child[c];
			if (cur.hit) result.add(new Prefix(cur, i + 1));
		}
		return result;
	}

	/**
	 * a の接頭辞である登録済みキーに対応するノードと長さを、長さの昇順で列挙する。
	 * <p>計算量: O(|a|)。</p>
	 * @param a 接頭辞を調べる文字配列
	 * @return a の接頭辞である登録済みキーのノードと長さ
	 */
	// 未テスト
	public List<Prefix> prefixesOf(char[] a) {
		//https://atcoder.jp/contests/abc377/submissions/80869421
		List<Prefix> result = new ArrayList<>();
		Node cur = root;
		if (cur.hit) result.add(new Prefix(cur, 0));
		for (int i = 0; i < a.length; ++i) {
			int c = a[i] - 'a';
			if (c < 0 || c >= cur.child.length || cur.child[c] == null) break;
			cur = cur.child[c];
			if (cur.hit) result.add(new Prefix(cur, i + 1));
		}
		return result;
	}
	
	/**
	 * 登録されている相異なるキーの個数を返す。
	 * <p>計算量: O(1)</p>
	 * @return 登録されている相異なるキーの個数
	 */
	public int size() {
		return keyCount;
	}

	/**
	 * このマップと指定されたオブジェクトが等価であるか検証します。
	 * @param o 比較対象のオブジェクト
	 * @return 等価であれば true, そうでなければ false
	 * O(numberOfNodes)
	 * // 未テスト
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TrieMap)) return false;
		TrieMap other = (TrieMap) o;
		return equals(this.root, other.root);
	}

	private boolean equals(Node n1, Node n2) {
		if (n1 == n2) return true;
		if (n1 == null || n2 == null) return false;
		if (n1.c != n2.c) return false;
		if (n1.hit != n2.hit) return false;
		if (n1.hit && n1.value != n2.value) return false;
		for (int i = 0; i < 26; i++) {
			if (!equals(n1.child[i], n2.child[i])) return false;
		}
		return true;
	}

	/**
	 * このマップのハッシュコード値を返します。
	 * @return ハッシュコード値
	 * O(numberOfNodes)
	 * // 未テスト
	 */
	@Override
	public int hashCode() {
		return hashCode(root);
	}

	private int hashCode(Node node) {
		if (node == null) return 0;
		int h = Integer.hashCode(node.c);
		h = 31 * h + Boolean.hashCode(node.hit);
		if (node.hit) {
			h = 31 * h + Long.hashCode(node.value);
		}
		for (int i = 0; i < 26; i++) {
			if (node.child[i] != null) {
				h = 31 * h + hashCode(node.child[i]);
			}
		}
		return h;
	}
}
