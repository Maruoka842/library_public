package library.util.segtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import library.util.ArrayUtils;
import library.util.seq.SortedArrays;

/**
 * 区間座標圧縮に対応した遅延評価セグメント木。
 *
 * <p>
 * 登録された境界点 p_0 < p_1 < ... < p_{k-1} に対し、
 * 隣接する半開区間 [p_i, p_{i+1}) (0 <= i < k - 1) を各葉として管理する。
 * 各区間長 p_{i+1} - p_i に応じた初期値の設定や、区間更新・区間取得クエリを O(log N) で処理する。
 * </p>
 *
 * @param <Acting> 作用素の型
 * @param <Acted>  要素の型
 */
public class CompressedIntervalLazySegTree<Acting, Acted> {
	/** 作用素の合成 */
	private BinaryOperator<Acting> mergeA;
	/** 要素の二項演算 */
	private BinaryOperator<Acted> mergeX;
	/** 作用素の適用 */
	private BiFunction<Acting, Acted, Acted> mergeAX;
	/** 要素の単位元 */
	Acted identityX = null;
	/** 作用素の単位元 */
	Acting identityA = null;
	/** 内部の遅延評価セグメント木 */
	LazySegTree<Acting, Acted> seg;
	/** 座標圧縮後の境界点 */
	long[] points;
	/** 登録予定の境界点 */
	ArrayList<Long> addPoints = new ArrayList<>();
	/** build済みフラグ */
	boolean built = false;
	/** 葉の初期化関数 */
	BiFunction<Long, Long, Acted> initLeaf;

	/**
	 * コンストラクタ。
	 *
	 * @param mergeA    作用素の合成関数
	 * @param mergeX    要素の二項演算関数
	 * @param mergeAX   作用素の適用関数
	 * @param identityX 要素の単位元
	 */
	// 未テスト
	public CompressedIntervalLazySegTree(BinaryOperator<Acting> mergeA, BinaryOperator<Acted> mergeX,
			BiFunction<Acting, Acted, Acted> mergeAX, Acted identityX) {
		this.mergeA = mergeA;
		this.mergeAX = mergeAX;
		this.mergeX = mergeX;
		this.identityX = identityX;
	}

	/**
	 * コンストラクタ。
	 *
	 * @param mergeA    作用素の合成関数
	 * @param mergeX    要素の二項演算関数
	 * @param mergeAX   作用素の適用関数
	 * @param identityX 要素の単位元
	 * @param identityA 作用素の単位元
	 */
	// 未テスト
	public CompressedIntervalLazySegTree(BinaryOperator<Acting> mergeA, BinaryOperator<Acted> mergeX,
			BiFunction<Acting, Acted, Acted> mergeAX, Acted identityX, Acting identityA) {
		this.mergeA = mergeA;
		this.mergeAX = mergeAX;
		this.mergeX = mergeX;
		this.identityX = identityX;
		this.identityA = identityA;
	}

	/**
	 * 戦略を用いたコンストラクタ。
	 *
	 * @param strategy 遅延セグメント木のモノイド・作用素戦略
	 */
	// 未テスト
	public CompressedIntervalLazySegTree(LazySegTreeStrategy<Acting, Acted> strategy) {
		this.identityX = strategy.identityX();
		this.identityA = strategy.identityA();
		this.mergeA = strategy::mergeA;
		this.mergeX = strategy::mergeX;
		this.mergeAX = strategy::mergeAX;
	}

	/**
	 * 境界点 k を事前に登録する。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param k 登録する境界点
	 */
	// 未テスト
	public void registerKey(long k) {
		addPoints.add(k);
	}

	/**
	 * 半開区間 [l, r) の境界点 l, r を事前に登録する。
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @param l 区間の左端（閉）
	 * @param r 区間の右端（開）
	 */
	// 未テスト
	public void registerInterval(long l, long r) {
		addPoints.add(l);
		addPoints.add(r);
	}

	/**
	 * 登録された境界点から区間座標圧縮を行い、セグメント木を構築する。
	 *
	 * <p>
	 * 各隣接区間 [p_i, p_{i+1}) に対し、initLeaf.apply(p_i, p_{i+1}) で葉の初期値を生成する。
	 * </p>
	 *
	 * <p>計算量: O(K log K) (K は登録された境界点数)</p>
	 *
	 * @param initLeaf 区間の左端 l と右端 r (半開区間 [l, r)) から要素初期値を生成する関数
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public void build(BiFunction<Long, Long, Acted> initLeaf) {
		this.initLeaf = initLeaf;
		built = true;
		points = addPoints.stream().mapToLong(Long::longValue).toArray();
		points = ArrayUtils.sortq(points);
		int n = Math.max(0, points.length - 1);
		seg = new LazySegTree<>(Math.max(1, n), mergeA, mergeX, mergeAX, identityX);
		if (n > 0) {
			Acted[] initialValues = (Acted[]) new Object[n];
			for (int i = 0; i < n; i++) {
				initialValues[i] = initLeaf.apply(points[i], points[i + 1]);
			}
			seg.build(initialValues);
		}
	}

	/**
	 * 各葉を単位元 identityX で初期化してセグメント木を構築する。
	 *
	 * <p>計算量: O(K log K)</p>
	 */
	// 未テスト
	public void build() {
		build((l, r) -> identityX);
	}

	/**
	 * 半開区間 [l, r) に作用素 add を作用させる。
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param l   区間の左端（閉）
	 * @param r   区間の右端（開）
	 * @param add 作用させる作用素
	 */
	// 未テスト
	public void act(long l, long r, Acting add) {
		if (!built)
			throw new AssertionError();
		int lid = SortedArrays.ceil(points, l);
		int rid = SortedArrays.ceil(points, r);
		if (lid < rid) {
			seg.act(lid, rid, add);
		}
	}

	/**
	 * 半開区間 [l, r) の要素の合成値を取得する。
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param l 区間の左端（閉）
	 * @param r 区間の右端（開）
	 * @return 区間 [l, r) の要素の合成値
	 */
	// 未テスト
	public Acted fold(long l, long r) {
		if (!built)
			throw new AssertionError();
		int lid = SortedArrays.ceil(points, l);
		int rid = SortedArrays.ceil(points, r);
		if (lid >= rid) {
			return identityX;
		}
		return seg.fold(lid, rid);
	}

	/**
	 * 境界区間 [l, r) に対応する葉の値を val に設定する。
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param l   登録済みの区間左端
	 * @param r   登録済みの区間右端
	 * @param val 設定する値
	 */
	// 未テスト
	public void set(long l, long r, Acted val) {
		if (!built)
			throw new AssertionError();
		int lid = SortedArrays.indexOf(points, l);
		int rid = SortedArrays.indexOf(points, r);
		if (lid == -1 || rid == -1 || rid != lid + 1) {
			throw new AssertionError();
		}
		seg.set(lid, val);
	}

	/**
	 * 座標 k を含む単一区間 [p_i, p_{i+1}) の要素の値を取得する。
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param k 座標
	 * @return 座標 k を含む区間の値
	 */
	// 未テスト
	public Acted get(long k) {
		if (!built)
			throw new AssertionError();
		int id = SortedArrays.floor(points, k);
		if (id < 0 || id >= points.length - 1) {
			return identityX;
		}
		return seg.get(id);
	}

	/**
	 * 全要素を initLeaf による初期状態に戻す。
	 *
	 * <p>計算量: O(K)</p>
	 */
	// 未テスト
	@SuppressWarnings("unchecked")
	public void clear() {
		if (!built)
			throw new AssertionError();
		int n = Math.max(0, points.length - 1);
		if (n > 0 && initLeaf != null) {
			Acted[] initialValues = (Acted[]) new Object[n];
			for (int i = 0; i < n; i++) {
				initialValues[i] = initLeaf.apply(points[i], points[i + 1]);
			}
			seg.build(initialValues);
		} else {
			seg.clear();
		}
	}

	/**
	 * f.test(fold(l, r)) が真となる最大の r を返す。
	 *
	 * <p>
	 * これ以降に登録された区間が存在せず、どこまで右に伸ばしても条件を満たす場合は
	 * Long.MAX_VALUE を返す。
	 * </p>
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param l 左端座標
	 * @param f 判定述語
	 * @return 条件を満たす最大の右端座標 r
	 */
	// 未テスト
	public long maximalRight(long l, Predicate<Acted> f) {
		if (!built)
			throw new AssertionError();

		int lid = SortedArrays.ceil(points, l);
		int n = Math.max(0, points.length - 1);

		if (lid >= n)
			return Long.MAX_VALUE;

		int rid = seg.maximalRight(lid, f);

		if (rid >= n)
			return Long.MAX_VALUE;

		return points[rid];
	}

	/**
	 * f.test(fold(l, r)) が真となる最小の l を返す。
	 *
	 * <p>
	 * 左側に登録された区間が存在せず、どこまで左に伸ばしても条件を満たす場合は
	 * Long.MIN_VALUE を返す。
	 * </p>
	 *
	 * <p>計算量: O(log K)</p>
	 *
	 * @param r 右端座標
	 * @param f 判定述語
	 * @return 条件を満たす最小の左端座標 l
	 */
	// 未テスト
	public long minimalLeft(long r, Predicate<Acted> f) {
		if (!built)
			throw new AssertionError();

		int rid = SortedArrays.ceil(points, r);

		if (rid <= 0)
			return Long.MIN_VALUE;

		int lex = seg.minimalLeft(rid - 1, f);

		if (lex == -1)
			return Long.MIN_VALUE;

		return points[lex + 1];
	}

	/**
	 * 内部状態を標準出力に出力する。
	 *
	 * <p>計算量: O(K)</p>
	 */
	// 未テスト
	public void dump() {
		System.out.println("CompressedIntervalLazySegTree { points: " + Arrays.toString(points) + " }");
		if (seg != null) {
			seg.dump();
		}
	}
}
