package library.util.segtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import library.util.ArrayUtils;
import library.util.seq.SortedArrays;

public class CompressedSparseSegTree<T> {
	T identity;
    java.util.function.BinaryOperator<T> op;
    long[] points;
    ArrayList<Long> addPoints = new ArrayList<>();
    SegTree<T> seg;
    boolean built=false;
    		
	public CompressedSparseSegTree(java.util.function.BinaryOperator<T> op, T identity) {
		this.op = op;
		this.identity = identity;
	}

	/**
	 * モノイド戦略 {@code strategy} を用いて座標圧縮セグメント木を構築する。
	 *
	 * <p>計算量: O(1)</p>
	 * // 未テスト
	 * @param strategy モノイド戦略
	 */
	public CompressedSparseSegTree(library.util.algebra.strategy.monoid.MonoidStrategy<T> strategy) {
		this(strategy::mul, strategy.identity());
	}

	/**
	 * {@link #set(long, Object)}, {@link #mul(long, Object)}　を呼び出す点を事前に登録する。
	 * @param k
	 */
	public void registerKey(long k) {
		addPoints.add(k);
	}
	
	public void build() {
		built=true;
		points=addPoints.stream().mapToLong(Long::longValue).toArray();
		points=ArrayUtils.sortq(points);
		seg=new SegTree<T>(Math.max(1, points.length), op, identity);
	}
	
	public void set(long k, T t) {
		int id=SortedArrays.indexOf(points, k);
		if(id==-1)throw new AssertionError();
		seg.set(id, t);
	}
	
	public void mul(long k, T t) {
		int id=SortedArrays.indexOf(points, k);
		if(id==-1)throw new AssertionError();
		seg.mul(id, t);
	}

	public T fold(long l, long r) {
		if(!built) throw new AssertionError();
		int lid=SortedArrays.ceil(points, l);
		int rid=SortedArrays.ceil(points, r);
		return seg.fold(lid, rid);
	}
	
	
	/**
	 * 全要素を identity に戻す。
	 *
	 * <p>
	 * 登録済みの座標および build 状態は保持される。
	 * </p>
	 *
	 * <p>計算量: O(N)</p>
	 */
	public void clear() {
		if (!built)
			throw new AssertionError();
		seg.clear();
	}
	
	/**
	 * f.test(fold(l, r)) が真となる最大の r を返す。
	 *
	 * <p>
	 * これ以降に登録点が存在せず、どこまで右に伸ばしても条件を満たす場合は
	 * {@link Long#MAX_VALUE} を返す。
	 * </p>
	 *
	 * <p>計算量: O(log N)</p>
	 */
	public long maximalRight(long l, Predicate<T> f) {
		if (!built)
			throw new AssertionError();

		int lid = SortedArrays.ceil(points, l);

		// [l, +∞) に登録点がない
		if (lid == points.length)
			return Long.MAX_VALUE;

		int rid = seg.maximalRight(lid, f);

		// 全て取っても条件を満たす
		if (rid == points.length)
			return Long.MAX_VALUE;

		/*
		 * 圧縮上 [lid, rid) を取るには、
		 * 元座標では r = points[rid] まで伸ばせる。
		 *
		 * points[rid] 自体は [l, r) に含まれない。
		 */
		return points[rid];
	}

	/**
	 * f.test(fold(l, r)) が真となる最小の l を返す。
	 *
	 * <p>
	 * 左側に登録点が存在せず、どこまで左に伸ばしても条件を満たす場合は
	 * {@link Long#MIN_VALUE} を返す。
	 * </p>
	 *
	 * <p>計算量: O(log N)</p>
	 */
	public long minimalLeft(long r, Predicate<T> f) {
		if (!built)
			throw new AssertionError();

		// fold(l, r) に含まれ得る点は points[0, rid)
		int rid = SortedArrays.ceil(points, r);

		if (rid == 0)
			return Long.MIN_VALUE;

		/*
		 * SegTree.minimalLeft(x, f) は
		 *
		 *   f(fold(k + 1, x + 1))
		 *
		 * が真となる最小の k を返す。
		 *
		 * よって圧縮座標上の左端は +1 する。
		 */
		int lid = seg.minimalLeft(rid - 1, f) + 1;

		if (lid == 0)
			return Long.MIN_VALUE;

		/*
		 * points[lid - 1] を除外するためには
		 * l > points[lid - 1]
		 *
		 * long 整数座標なので最小値は +1。
		 */
		return points[lid - 1] + 1;
	}
	
	void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}
}
