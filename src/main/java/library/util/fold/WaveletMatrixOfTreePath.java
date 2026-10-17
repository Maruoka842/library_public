package library.util.fold;

import java.util.Arrays;

import library.util.graph.tree.Tree;

/**
 * 根付き木の頂点パスを列とみなし、静的な Wavelet Matrix クエリを処理する。
 * 頂点値は非負 {@code long} とする。
 *
 * <p>{@code root -> v} 上の多重集合を preorder prefix と postorder prefix の差で表す。
 * このクラスは問い合わせ用バッファを書き換えるため、thread-safe ではない。</p>
 *
 * <p>構築 O(N log sigma)、空間 O(N log sigma / wordSize) words。
 * 値に関する各問い合わせは O(log sigma)、木上の移動を伴う問い合わせは別途記載する。</p>
 */
public class WaveletMatrixOfTreePath {
	/** 問い合わせ対象の根付き木。 */
	final Tree tree;
	/** 頂点数。 */
	final int n;
	/** {@code value[v]} = 頂点 v の値。 */
	final long[] value;
	/** {@code prePos[v]} = preorder における頂点 v の位置。 */
	final int[] prePos;
	/** preorder および postorder 上の Wavelet Matrix。 */
	final WaveletMatrix pre, post;
	/** 値を表す bit 数。 */
	final int w;
	/** 頂点値の最大値。 */
	final long maxA;
	/** 一般パスを 4 個の root-prefix の符号付き和で表す問い合わせ用バッファ。 */
	final int[] p = new int[4], q = new int[4];
	/** root-path 専用の問い合わせ用バッファ。 */
	int rootP, rootQ;

	/**
	 * {@code value[v]} を頂点 v の値として構築する。
	 * 未テスト。
	 * 計算量: O(N log sigma)。
	 *
	 * @param tree 根付き木
	 * @param value 各頂点の非負の値
	 */
	public WaveletMatrixOfTreePath(Tree tree, int[] value) {
		this(tree, Arrays.stream(value).asLongStream().toArray());
	}

	/**
	 * {@code value[v]} を頂点 v の値として構築する。
	 * 未テスト。
	 * 計算量: O(N log sigma)。
	 *
	 * @param tree 根付き木
	 * @param value 各頂点の非負の値
	 */
	public WaveletMatrixOfTreePath(Tree tree, long[] value) {
		if (!tree.isRooted() || tree.N != value.length) throw new AssertionError();
		this.tree = tree;
		n = value.length;
		this.value = value.clone();
		prePos = new int[n];
		long[] a = new long[n], b = new long[n];
		build(a, b);
		pre = new WaveletMatrix(a);
		post = new WaveletMatrix(b);
		w = pre.w;
		maxA = pre.maxA;
		pre.inverseMapping = post.inverseMapping = null;
	}

	/**
	 * preorder の値列を {@code a}、postorder の値列を {@code b} に格納する。
	 * 未テスト。
	 * 計算量: O(N)。
	 */
	private void build(long[] a, long[] b) {
		int[] preorder = tree.preOrder(), postorder = tree.postOrder();
		for (int i = 0; i < n; i++) {
			prePos[preorder[i]] = i;
			a[i] = value[preorder[i]];
			b[i] = value[postorder[i]];
		}
	}

	/** 未テスト。一般パス用バッファを初期化する。計算量: O(1)。 */
	private void init(int u, int v, int lca) {
		setPrefix(0, u);
		setPrefix(1, v);
		setPrefix(2, lca);
		int parent = tree.parent(lca);
		if (parent == n) p[3] = q[3] = 0;
		else setPrefix(3, parent);
	}

	/** 未テスト。root-prefix を一般パス用バッファに設定する。計算量: O(1)。 */
	private void setPrefix(int i, int v) {
		p[i] = prePos[v] + 1;
		q[i] = prePos[v] - tree.depth[v];
	}

	/** 未テスト。root-path 専用バッファを初期化する。計算量: O(1)。 */
	private void initRoot(int v) {
		rootP = prePos[v] + 1;
		rootQ = prePos[v] - tree.depth[v];
	}

	/** 未テスト。現在の一般パス区間に含まれる 0-bit の個数を返す。計算量: O(1)。 */
	private int zeros(int h) {
		int ret = 0;
		for (int i = 0; i < 4; i++) {
			int count = p[i] - pre.rank1[h].prefixSum(p[i]) - q[i] + post.rank1[h].prefixSum(q[i]);
			ret += (i < 2 ? 1 : -1) * count;
		}
		return ret;
	}

	/** 未テスト。一般パス用バッファを {@code bit} 側へ写す。計算量: O(1)。 */
	private void move(int h, int bit) {
		for (int i = 0; i < 4; i++) {
			int rp = pre.rank1[h].prefixSum(p[i]);
			int rq = post.rank1[h].prefixSum(q[i]);
			if (bit == 0) {
				p[i] -= rp;
				q[i] -= rq;
			} else {
				p[i] = pre.mid[h] + rp;
				q[i] = post.mid[h] + rq;
			}
		}
	}

	/** 未テスト。現在の root-path 区間に含まれる 0-bit の個数を返す。計算量: O(1)。 */
	private int zerosRoot(int h) {
		return rootP - pre.rank1[h].prefixSum(rootP) - rootQ + post.rank1[h].prefixSum(rootQ);
	}

	/** 未テスト。root-path 専用バッファを {@code bit} 側へ写す。計算量: O(1)。 */
	private void moveRoot(int h, int bit) {
		int rp = pre.rank1[h].prefixSum(rootP);
		int rq = post.rank1[h].prefixSum(rootQ);
		if (bit == 0) {
			rootP -= rp;
			rootQ -= rq;
		} else {
			rootP = pre.mid[h] + rp;
			rootQ = post.mid[h] + rq;
		}
	}

	/** 未テスト。既知の LCA を用いて {@code value < x} の個数を返す。計算量: O(log sigma)。 */
	private int countLess(int u, int v, int lca, long x) {
		int len = tree.depth[u] + tree.depth[v] - 2 * tree.depth[lca] + 1;
		if (x <= 0) return 0;
		if (x > maxA) return len;
		init(u, v, lca);
		int ret = 0;
		for (int h = w - 1; h >= 0; h--) {
			int bit = (int) (x >>> h & 1);
			if (bit != 0) ret += zeros(h);
			move(h, bit);
		}
		return ret;
	}

	/** 未テスト。root-v パス上の {@code value < x} の個数を返す。計算量: O(log sigma)。 */
	private int countLessRoot(int v, long x) {
		if (x <= 0) return 0;
		if (x > maxA) return tree.depth[v] + 1;
		initRoot(v);
		int ret = 0;
		for (int h = w - 1; h >= 0; h--) {
			int bit = (int) (x >>> h & 1);
			if (bit != 0) ret += zerosRoot(h);
			moveRoot(h, bit);
		}
		return ret;
	}

	/** 未テスト。既知の LCA を用いて値 {@code x} の個数を返す。計算量: O(log sigma)。 */
	private int count(int u, int v, int lca, long x) {
		if (x < 0 || x > maxA) return 0;
		init(u, v, lca);
		for (int h = w - 1; h >= 0; h--) move(h, (int) (x >>> h & 1));
		int ret = 0;
		for (int i = 0; i < 4; i++) ret += (i < 2 ? 1 : -1) * (p[i] - q[i]);
		return ret;
	}

	/** 未テスト。root-v パス上の値 {@code x} の個数を返す。計算量: O(log sigma)。 */
	private int countRoot(int v, long x) {
		if (x < 0 || x > maxA) return 0;
		initRoot(v);
		for (int h = w - 1; h >= 0; h--) moveRoot(h, (int) (x >>> h & 1));
		return rootP - rootQ;
	}

	/** 未テスト。既知の LCA を用いて先頭 r 頂点での rank を返す。計算量: O(log N + log sigma)。 */
	private int rank(int u, int v, int lca, int r, long x) {
		int du = tree.depth[u] - tree.depth[lca];
		int len = du + tree.depth[v] - tree.depth[lca] + 1;
		if (r < 0 || r > len) throw new AssertionError();
		if (r == 0) return 0;
		if (r <= du + 1) {
			int z = tree.getLevelAncestor(u, r - 1);
			return count(u, z, z, x);
		}
		int z = tree.getLevelAncestor(v, len - r);
		return count(u, z, lca, x);
	}

	/**
	 * パス u-v の頂点数を返す。
	 * 未テスト。
	 * 計算量: O(log N)。
	 */
	public int pathLength(int u, int v) {
		int lca = tree.lca(u, v);
		return tree.depth[u] + tree.depth[v] - 2 * tree.depth[lca] + 1;
	}

	/** root-v パスの頂点数を返す。未テスト。計算量: O(1)。 */
	public int pathLength(int v) { return tree.depth[v] + 1; }

	/** パス u-v 上で {@code value < x} の頂点数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int countLess(int u, int v, long x) { return countLess(u, v, tree.lca(u, v), x); }

	/** root-v パス上で {@code value < x} の頂点数を返す。未テスト。計算量: O(log sigma)。 */
	public int countLess(int v, long x) { return countLessRoot(v, x); }

	/** パス u-v 上で {@code value <= x} の頂点数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int countLeq(int u, int v, long x) {
		if (x < 0) return 0;
		int lca = tree.lca(u, v);
		return x >= maxA ? tree.depth[u] + tree.depth[v] - 2 * tree.depth[lca] + 1 : countLess(u, v, lca, x + 1);
	}

	/** root-v パス上で {@code value <= x} の頂点数を返す。未テスト。計算量: O(log sigma)。 */
	public int countLeq(int v, long x) {
		if (x < 0) return 0;
		return x >= maxA ? tree.depth[v] + 1 : countLessRoot(v, x + 1);
	}

	/** パス u-v 上で {@code lo <= value < hi} の頂点数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int countRange(int u, int v, long lo, long hi) {
		if (lo >= hi) return 0;
		int lca = tree.lca(u, v);
		return countLess(u, v, lca, hi) - countLess(u, v, lca, lo);
	}

	/** root-v パス上で {@code lo <= value < hi} の頂点数を返す。未テスト。計算量: O(log sigma)。 */
	public int countRange(int v, long lo, long hi) {
		return lo >= hi ? 0 : countLessRoot(v, hi) - countLessRoot(v, lo);
	}

	/** パス u-v 上で値が x の頂点数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int count(int u, int v, long x) { return count(u, v, tree.lca(u, v), x); }

	/** root-v パス上で値が x の頂点数を返す。未テスト。計算量: O(log sigma)。 */
	public int count(int v, long x) { return countRoot(v, x); }

	/** パス u-v 上の 0-origin k 番目に小さい値を返す。未テスト。計算量: O(log N + log sigma)。 */
	public long kthSmallest(int u, int v, int k) {
		int lca = tree.lca(u, v);
		int len = tree.depth[u] + tree.depth[v] - 2 * tree.depth[lca] + 1;
		if (k < 0 || k >= len) throw new AssertionError();
		init(u, v, lca);
		long ret = 0;
		for (int h = w - 1; h >= 0; h--) {
			int z = zeros(h);
			if (k < z) move(h, 0);
			else {
				k -= z;
				ret |= 1L << h;
				move(h, 1);
			}
		}
		return ret;
	}

	/** root-v パス上の 0-origin k 番目に小さい値を返す。未テスト。計算量: O(log sigma)。 */
	public long kthSmallest(int v, int k) {
		if (k < 0 || k > tree.depth[v]) throw new AssertionError();
		initRoot(v);
		long ret = 0;
		for (int h = w - 1; h >= 0; h--) {
			int z = zerosRoot(h);
			if (k < z) moveRoot(h, 0);
			else {
				k -= z;
				ret |= 1L << h;
				moveRoot(h, 1);
			}
		}
		return ret;
	}

	/** パス u-v 上の 0-origin k 番目に大きい値を返す。未テスト。計算量: O(log N + log sigma)。 */
	public long kthLargest(int u, int v, int k) { return kthSmallest(u, v, pathLength(u, v) - 1 - k); }

	/** root-v パス上の 0-origin k 番目に大きい値を返す。未テスト。計算量: O(log sigma)。 */
	public long kthLargest(int v, int k) { return kthSmallest(v, tree.depth[v] - k); }

	/** パス u-v 上の最小値を返す。未テスト。計算量: O(log N + log sigma)。 */
	public long min(int u, int v) { return kthSmallest(u, v, 0); }

	/** root-v パス上の最小値を返す。未テスト。計算量: O(log sigma)。 */
	public long min(int v) { return kthSmallest(v, 0); }

	/** パス u-v 上の最大値を返す。未テスト。計算量: O(log N + log sigma)。 */
	public long max(int u, int v) { return kthLargest(u, v, 0); }

	/** root-v パス上の最大値を返す。未テスト。計算量: O(log sigma)。 */
	public long max(int v) { return kthLargest(v, 0); }

	/** パス u-v 上の x 未満の最大値を返す。存在しなければ -1。未テスト。計算量: O(log N + log sigma)。 */
	public long predecessor(int u, int v, long x) {
		int k = countLess(u, v, x);
		return k == 0 ? -1 : kthSmallest(u, v, k - 1);
	}

	/** root-v パス上の x 未満の最大値を返す。存在しなければ -1。未テスト。計算量: O(log sigma)。 */
	public long predecessor(int v, long x) {
		int k = countLessRoot(v, x);
		return k == 0 ? -1 : kthSmallest(v, k - 1);
	}

	/** パス u-v 上の x 以上の最小値を返す。存在しなければ -1。未テスト。計算量: O(log N + log sigma)。 */
	public long successor(int u, int v, long x) {
		int k = countLess(u, v, x);
		return k == pathLength(u, v) ? -1 : kthSmallest(u, v, k);
	}

	/** root-v パス上の x 以上の最小値を返す。存在しなければ -1。未テスト。計算量: O(log sigma)。 */
	public long successor(int v, long x) {
		int k = countLessRoot(v, x);
		return k == tree.depth[v] + 1 ? -1 : kthSmallest(v, k);
	}

	/** パス u -> v の 0-origin k 番目の値を返す。未テスト。計算量: O(log N)。 */
	public long access(int u, int v, int k) {
		int lca = tree.lca(u, v);
		int du = tree.depth[u] - tree.depth[lca], dv = tree.depth[v] - tree.depth[lca];
		if (k < 0 || k > du + dv) throw new AssertionError();
		int z = k <= du ? tree.getLevelAncestor(u, k) : tree.getLevelAncestor(v, du + dv - k);
		return value[z];
	}

	/** root -> v の 0-origin k 番目の値を返す。未テスト。計算量: O(log N)。 */
	public long access(int v, int k) {
		if (k < 0 || k > tree.depth[v]) throw new AssertionError();
		return value[tree.getLevelAncestor(v, tree.depth[v] - k)];
	}

	/** パス u -> v の先頭 r 頂点に x が現れる回数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int rank(int u, int v, int r, long x) { return rank(u, v, tree.lca(u, v), r, x); }

	/** root -> v の先頭 r 頂点に x が現れる回数を返す。未テスト。計算量: O(log N + log sigma)。 */
	public int rank(int v, int r, long x) {
		if (r < 0 || r > tree.depth[v] + 1) throw new AssertionError();
		if (r == 0) return 0;
		return countRoot(tree.getLevelAncestor(v, tree.depth[v] + 1 - r), x);
	}

	/**
	 * パス u -> v における x の 0-origin k 番目の出現位置を返す。存在しなければ -1。
	 * 未テスト。
	 * 計算量: O(log N (log N + log sigma))。
	 */
	public int select(int u, int v, long x, int k) {
		int lca = tree.lca(u, v);
		int len = tree.depth[u] + tree.depth[v] - 2 * tree.depth[lca] + 1;
		if (k < 0 || count(u, v, lca, x) <= k) return -1;
		int lo = 0, hi = len;
		while (lo < hi) {
			int mid = lo + hi >>> 1;
			if (rank(u, v, lca, mid + 1, x) > k) hi = mid;
			else lo = mid + 1;
		}
		return lo;
	}

	/**
	 * root -> v における x の 0-origin k 番目の出現位置を返す。存在しなければ -1。
	 * 未テスト。
	 * 計算量: O(log N (log N + log sigma))。
	 */
	public int select(int v, long x, int k) {
		if (k < 0 || countRoot(v, x) <= k) return -1;
		int lo = 0, hi = tree.depth[v] + 1;
		while (lo < hi) {
			int mid = lo + hi >>> 1;
			if (rank(v, mid + 1, x) > k) hi = mid;
			else lo = mid + 1;
		}
		return lo;
	}
}
