package library.util.collections;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

import org.graphstream.graph.implementations.SingleGraph;

import library.util.Longs;

/**
 * Trieに追加された値は全て長さbitLengthの01文字列として認識されて葉に蓄えられる。
 * 従って、n = (1 << bitLength) のセグ木で必要な部分だけを作った場合と同一。
 */
public class LeafyBinaryTrieFromTopBits {
	private int numberOfNodes = 0;	
	private int numberOfLeafs = 0;
	Node root = new Node();
    int bitLength = 63;
    
    
    public LeafyBinaryTrieFromTopBits(int bitLength) {
    	this.bitLength = bitLength;
    	this.root.bit = bitLength;
    }
    
    public LeafyBinaryTrieFromTopBits() {
    	this.root.bit = bitLength;
    }

	/**
	 * トライ木を空にする。
	 * O(1)
	 * // 未テスト
	 */
	public void clear() {
		numberOfNodes = 0;
		numberOfLeafs = 0;
		root = new Node();
		root.bit = bitLength;
	}
    
    public class Node {
		Node left;
		Node right;
		Node parent;
		int bit;//bit目(0-origin)で分岐して生成されたノード
		int id;
		public int hit;
		long value;
		int subtreeHit;
		
		public Node() {
			this.id = numberOfNodes++;
		}
		
		public boolean isLeftChild() {
			if (parent == null) return false;
			return parent.left == this;
		}
		
		public boolean isRightChild() {
			if (this == root) return false;
			return parent.right == this;
		}

		
		public boolean isLeftEmpty() {
			return left == null || left.subtreeHit == 0;
		}
		
		public boolean isRightEmpty() {
			return right == null || right.subtreeHit == 0;
		}
		
		public Node left() {
			if (isLeftEmpty()) throw new AssertionError();
			return left;
		}
		
		public Node right() {
			if (isRightEmpty()) throw new AssertionError();
			return right;
		}
		
		public Node parent() {
			return parent;
		}
		
		/**
		 * bit(), bit()+1, .., bitLength-1番目までのbitが決定済み
		 * @return
		 */
		public int bit() {
			return bit;
		}
		
		public long value() {
			return value;
		}
		
		public ArrayList<Node> childs() {
			ArrayList<Node> ret=new ArrayList<>();
			if (!isLeftEmpty()) ret.add(left);
			if (!isRightEmpty()) ret.add(right);
			return ret;
		}
		
		
		public int id() {
			if (id == -1) throw new AssertionError();
			return id;
		}
		
		public int subtreeHit() {
			return subtreeHit;
		}
	}
    
    public boolean isEmpty() {
    	return numberOfLeafs == 0;
    }
    
    public int size() {
    	return numberOfLeafs;
    }
    
    public Node root() {
    	return root;
    }
	
	public Node[] bfsOrder () {
		Node[] ret = new Node[numberOfNodes];
		Queue<Node> que = new ArrayDeque<>();
		que.add(root);
		int pointer = 0;
		while (!que.isEmpty()) {
			Node node = que.poll();
			ret[pointer++] = node;
			if (!node.isLeftEmpty()) que.add(node.left);
			if (!node.isRightEmpty()) que.add(node.right);
		}
		ret = Arrays.copyOf(ret, pointer);
		return ret;
	}

	public Node get(long v) {
		if (v < 0) throw new AssertionError();
		Node cur = root;
		for (int i = bitLength - 1; i >= 0; i--) {
			if (Longs.bitAt(v, i) == 0) {
				if (cur.isLeftEmpty()) {
					throw new AssertionError();
				}
				cur = cur.left;
			} else {
				if (cur.isRightEmpty()) {
					throw new AssertionError();
				}
				cur = cur.right;
			}
		}
		return cur;
	}
	
	public Node add(long v) {
		if (v < 0) throw new AssertionError();
		Node cur = root;
		cur.subtreeHit++;
		for (int i = bitLength - 1; i >= 0; i--) {
			if (Longs.bitAt(v, i) == 0) {
				if (cur.left == null) 
					cur.left = new Node();
				if (cur.isLeftEmpty()) {
					cur.left.parent = cur;
					cur.left.value = cur.value;
				}
				cur = cur.left;
				cur.bit = i;
			} else {
				if (cur.right == null) 
					cur.right = new Node();
				if (cur.isRightEmpty()) {
					cur.right.parent = cur;
					cur.right.value = cur.value | (1L << i);
				}
				cur = cur.right;
				cur.bit = i;
			}
			cur.subtreeHit++;
		}
		cur.hit++;
		numberOfLeafs++;
		return cur;
	}
	
	
	public boolean contains(long v) {
		if (v < 0) return false;
		Node cur = root;
		for (int i = bitLength - 1; i >= 0; i--) {
			if (Longs.bitAt(v, i) == 0) {
				if (cur.isLeftEmpty()) {
					return false;
				}
				cur = cur.left;
			} else {
				if (cur.isRightEmpty()) {
					return false;
				}
				cur = cur.right;
			}
		}
		return true;
	}

	/**
	 * 追加されている値を昇順に見たときのk番目(0-indexed)を返す。
	 * 重複値は重複した個数分だけ数える。
	 */
	public long kthSmallest(int k) {
		if (k < 0 || k >= numberOfLeafs) throw new AssertionError();
		Node cur = root;
		for (int i = bitLength - 1; i >= 0; i--) {
			int leftCount = cur.isLeftEmpty() ? 0 : cur.left.subtreeHit();
			if (k < leftCount) {
				cur = cur.left();
			} else {
				k -= leftCount;
				cur = cur.right();
			}
		}
		return cur.value();
	}

	/**
	 * 追加されている値 x 全体について、(x xor xorValue) を昇順に見たときの
	 * k 番目 (0-indexed) の値を返す。
	 * 重複値は重複した個数分だけ数える。
	 * 戻り値は x 自体ではなく、(x xor xorValue) の値。
	 */
	public long xorKthSmallest(long xorValue, int k) {
		if (k < 0 || k >= numberOfLeafs) throw new AssertionError();
		Node cur = root;
		long ans = 0;
		for (int i = bitLength - 1; i >= 0; i--) {
			boolean xorBit = Longs.bitAt(xorValue, i) == 1;
			Node zeroChild = xorBit ? cur.right : cur.left;
			Node oneChild = xorBit ? cur.left : cur.right;
			int zeroCount = (zeroChild == null) ? 0 : zeroChild.subtreeHit();
			if (k < zeroCount) {
				cur = zeroChild;
			} else {
				k -= zeroCount;
				ans |= 1L << i;
				cur = oneChild;
			}
		}
		return ans;
	}

	
	
	
	public boolean remove(long v) {
		if (v < 0) throw new AssertionError();
		Node cur = root;
		for (int i = bitLength - 1; i >= 0; i--) {
			if (Longs.bitAt(v, i) == 0) {
				if (cur.isLeftEmpty()) {
					return false;
				}
				cur = cur.left;
			} else {
				if (cur.isRightEmpty()) {
					return false;
				}
				cur = cur.right;
			}
		}
		if (cur.hit == 0) return false;
		cur.hit--;
		do {
			cur.subtreeHit--;
			cur = cur.parent;
		} while (cur != root);
		cur.subtreeHit--;
		numberOfLeafs--;
		return true;
	}

	
	
	/**
	 * 未テスト
	 * @param v
	 * @return
	 */
	public long minXor(long v) {
		if (numberOfLeafs == 0) throw new AssertionError();
		var node=root();
		long ans=0;
		for (int j = bitLength-1; j >= 0; j--) {
			if(Longs.bitAt(v, j)==0) {
				if(node.isLeftEmpty()) {
					ans|=1L<<j;
					node=node.right();
				} else {
					node=node.left();
				}
			} else {
				if(node.isRightEmpty()) {
					ans|=1L<<j;
					node=node.left();
				} else {
					node=node.right();
				}
			}
		}
		return ans;
	}
	
	
	public long minXorExceptSelf(long v) {
		boolean deleted=remove(v);
		if (numberOfLeafs == 0) throw new AssertionError();
		long ret=minXor(v);
		if (deleted) add(v);
		return ret;
	}

	
	
	
	/**
	 * 未テスト
	 * @param v
	 * @return
	 */
	public long maxXor(long v) {
		var node=root();
		long ans=0;
		for (int j = bitLength-1; j >= 0; j--) {
			if(Longs.bitAt(v, j)==1) {
				if(node.isLeftEmpty()) {
					node=node.right();
				} else {
					ans|=1L<<j;
					node=node.left();
				}
			} else {
				if(node.isRightEmpty()) {
					node=node.left();
				} else {
					ans|=1L<<j;
					node=node.right();
				}
			}
		}
		return ans;
	}

	/**
	 * 区間 [l, r) に含まれる各整数 i について、トライ木に存在する値 v in A との XOR の最小値の総和
	 * sum_{i = l}^{r - 1} min_{v in A} (i xor v)
	 * を計算します。
	 *
	 * @param l 範囲の左端 (inclusive)
	 * @param r 範囲の右端 (exclusive)
	 * @return sum_{i = l}^{r - 1} min_{v in A} (i xor v) (l >= r または A が空の場合は 0)
	 * 計算量: O(numberOfNodes)
	 * // 未テスト
	 */
	public long sumMinXor(long l, long r) {
		//https://atcoder.jp/contests/abc425/submissions/79895850
		if (numberOfLeafs == 0 || l >= r) return 0;
		if (l < 0) l = 0;
		if (l >= r) return 0;

		return f(root, r, bitLength) - f(root, l, bitLength);
	}

	/**
	 * 集合 A_u（ノード u 配下）と整数 M に対して、
	 * f(A_u, M, k) = sum_{x = 0}^{M - 1} min_{v in A_u} (x xor v)
	 * を計算します。M が 2^k を超える場合は、周期性（周期 2^k）を利用して一括計算します。
	 *
	 * @param u 現在のノード
	 * @param M 範囲の上限 (x < M)
	 * @param k 現在のビット長さ
	 * @return sum_{x = 0}^{M - 1} min_{v in A_u} (x xor v)
	 * 計算量: O(配下ノード数)
	 * // 未テスト
	 */
	private long f(Node u, long M, int k) {
		if (k == 0 || M <= 0 || u == null || u.subtreeHit == 0) {
			return 0;
		}

		long mid = 1L << (k - 1);
		// 片方の子集合が空の場合に加算される定数項 2^(2k-2) = 2^(k-1) * 2^(k-1)
		long two2k2 = 1L << (2 * k - 2);
		long limit = 1L << k;

		if (k != 63 && M > limit) {
			long fullBlocks = M / limit;
			long rem = M % limit;
			long sumOneBlock = f(u, limit, k);
			long total = fullBlocks * sumOneBlock + (fullBlocks * (fullBlocks - 1) / 2) * limit * limit;
			if (rem > 0) {
				total += f(u, rem, k) + rem * fullBlocks * limit;
			}
			return total;
		}

		if (k != 63 && M == limit) {
			// Case 1: M = 2^k
			if (!u.isLeftEmpty() && !u.isRightEmpty()) {
				return f(u.left, mid, k - 1) + f(u.right, mid, k - 1);
			} else if (u.isLeftEmpty()) {
				return 2 * f(u.right, mid, k - 1) + two2k2;
			} else {
				return 2 * f(u.left, mid, k - 1) + two2k2;
			}
		} else if (M <= mid) {
			// Case 2: M <= 2^{k-1}
			if (u.isLeftEmpty()) {
				return f(u.right, M, k - 1) + M * mid;
			} else {
				return f(u.left, M, k - 1);
			}
		} else {
			// Case 3: 2^{k-1} < M < 2^k
			long leftPart = u.isLeftEmpty() ? (f(u.right, mid, k - 1) + two2k2) : f(u.left, mid, k - 1);
			long rem = M - mid;
			long rightPart = u.isRightEmpty() ? (f(u.left, rem, k - 1) + rem * mid) : f(u.right, rem, k - 1);
			return leftPart + rightPart;
		}
	}

	/**
	 * x xor (xorValue) ≤ v となる x の数を数える。
	 * @param v
	 * @param xorValue
	 * @return
	 */
	public long countLeqWithXor(long v, long xorValue) {
		//https://atcoder.jp/contests/abc451/submissions/74512702
		Node node=root;
		long ans=0;
		for (int i = bitLength - 1; i >= 0 && node != null; i--) {
			if ((v>>>i)%2==0) {
				if((xorValue>>>i)%2==0) {
					node=node.left;
				} else {
					node=node.right;
				}
			} else {
				if((xorValue>>>i)%2==0) {
					if(!node.isLeftEmpty())
						ans+=node.left.subtreeHit();
					node=node.right;
				} else {
					if(!node.isRightEmpty())
						ans+=node.right.subtreeHit();
					node=node.left;
				}
			}
			if(i==0 && node != null) ans+=node.subtreeHit();
		}
		return ans;
	}
	
	
	
	/**
	 * bitLengthを小さくしないと見づらい
	 */
	public void drawGraph() {
	    System.setProperty("org.graphstream.ui", "swing");
	    org.graphstream.graph.Graph graph = new SingleGraph("BinaryTrie");

	    // bfsOrderを呼んで id を確定させる
	    Node[] order = bfsOrder();

	    // グラフにノード追加
	    for (LeafyBinaryTrieFromTopBits.Node node : order) {
	        if (node == null) continue; // 配列0番目にnullが入るかも（現状のbfsOrderの実装）
	        String id = String.valueOf(node.id());
	        org.graphstream.graph.Node gnode = graph.addNode(id);
	        gnode.setAttribute("ui.label", "id=" + node.id());
	    }

	    // 辺の追加（左右の子があれば追加）
	    for (LeafyBinaryTrieFromTopBits.Node node : order) {
	        if (node == null) continue;
	        String id = String.valueOf(node.id());
	        if (!node.isLeftEmpty()) {
	            String lid = String.valueOf(node.left.id());
	            graph.addEdge(id + ":" + lid, id, lid, true).setAttribute("ui.label", "0");
	        }
	        if (!node.isRightEmpty()) {
	            String rid = String.valueOf(node.right.id());
	            graph.addEdge(id + ":" + rid, id, rid, true).setAttribute("ui.label", "1");
	        }
	    }

	    // 見た目の調整
	    graph.setAttribute("ui.stylesheet",
	        "node {" +
	        "   fill-color: lightblue;" +
	        "   size: 40px;" +
	        "   text-alignment: center;" +
	        "   text-size: 16;" +
	        "   text-color: black;" +
	        "}" +
	        "edge {" +
	        "   text-size: 14;" +
	        "   text-color: black;" +
	        "}"
	    );

	    graph.display();
	}

	/**
	 * 内部状態を標準出力に出力する。
	 * <ul>
	 *   <li>事前条件: 特になし。</li>
	 *   <li>事後条件: 特になし。</li>
	 *   <li>副作用: 標準出力への出力。</li>
	 *   <li>計算量: O(numberOfNodes)</li>
	 *   <li>破壊的変更: なし。</li>
	 * </ul>
	 */
	// 未テスト
	public void dump() {
		Node[] order = bfsOrder();
		StringBuilder sb = new StringBuilder();
		sb.append("LeafyBinaryTrieFromTopBits { size: ").append(numberOfLeafs).append(", nodes: [");
		for (int i = 0; i < order.length; i++) {
			if (order[i] != null) {
				sb.append("(id:").append(order[i].id).append(", bit:").append(order[i].bit).append(", value:").append(order[i].value).append(", hit:").append(order[i].hit).append(", sub:").append(order[i].subtreeHit).append(")");
				if (i < order.length - 1) sb.append(", ");
			}
		}
		sb.append("] }");
		System.out.println(sb.toString());
	}

	/**
	 * このトライブと指定されたオブジェクトが等価であるか検証します。
	 * @param o 比較対象のオブジェクト
	 * @return 等価であれば true, そうでなければ false
	 * O(numberOfNodes)
	 * // 未テスト
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof LeafyBinaryTrieFromTopBits)) return false;
		LeafyBinaryTrieFromTopBits other = (LeafyBinaryTrieFromTopBits) o;
		if (this.bitLength != other.bitLength) return false;
		if (this.numberOfLeafs != other.numberOfLeafs) return false;
		return equals(this.root, other.root);
	}

	private boolean equals(Node n1, Node n2) {
		boolean empty1 = (n1 == null || n1.subtreeHit == 0);
		boolean empty2 = (n2 == null || n2.subtreeHit == 0);
		if (empty1 && empty2) return true;
		if (empty1 || empty2) return false;
		if (n1.bit != n2.bit) return false;
		if (n1.hit != n2.hit) return false;
		if (n1.value != n2.value) return false;
		return equals(n1.left, n2.left) && equals(n1.right, n2.right);
	}

	/**
	 * このトライブのハッシュコード値を返します。
	 * @return ハッシュコード値
	 * O(numberOfNodes)
	 * // 未テスト
	 */
	@Override
	public int hashCode() {
		return hashCode(root);
	}

	private int hashCode(Node node) {
		if (node == null || node.subtreeHit == 0) return 0;
		int h = Integer.hashCode(node.bit);
		h = 31 * h + Integer.hashCode(node.hit);
		h = 31 * h + Long.hashCode(node.value);
		h = 31 * h + hashCode(node.left);
		h = 31 * h + hashCode(node.right);
		return h;
	}
}
