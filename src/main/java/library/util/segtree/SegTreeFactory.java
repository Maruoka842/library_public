package library.util.segtree;

import library.util.ArrayUtils;
import library.util.Zn;
import library.util.algebra.instance.MonoidElement;
import library.util.algebra.strategy.longs.LongModAddAbelianGroupStrategy;
import library.util.algebra.strategy.monoid.MonoidStrategy;
import library.util.monoid.BinaryInversionState;

/**
 *  null は identity の代わり
 */
public class SegTreeFactory {
	static class State2ndMax {
        long max;

        long max2;

        int cnt;

        int cnt2;

        public State2ndMax(long v) {
            max = v;
            max2 = Long.MIN_VALUE;
            cnt = 1;
            cnt2 = 0;
        }

        public State2ndMax(long max, long max2, int cnt, int cnt2) {
            this.max = max;
            this.max2 = max2;
            this.cnt = cnt;
            this.cnt2 = cnt2;
        }
    }
	
	public static SegTree<State2ndMax> count2ndMax(int n) {
        var seg = new SegTree<State2ndMax>(n, (x, y) -> {
            long max = Math.max(x.max, y.max);
            int cnt = 0;
            if (x.max == max) {
                cnt += x.cnt;
            }
            if (y.max == max) {
                cnt += y.cnt;
            }
            long max2 = Long.MIN_VALUE;
            for (long v : new long[]{ x.max, x.max2, y.max, y.max2 }) {
                if (v != max) {
                    max2 = Math.max(max2, v);
                }
            }
            int cnt2 = 0;
            if (x.max == max2) {
                cnt2 += x.cnt;
            }
            if (x.max2 == max2) {
                cnt2 += x.cnt2;
            }
            if (y.max == max2) {
                cnt2 += y.cnt;
            }
            if (y.max2 == max2) {
                cnt2 += y.cnt2;
            }
            return new State2ndMax(max, max2, cnt, cnt2);
        });
        return seg;
	}
	
	/**
	 * 各要素 {@code x} に対して、作用 {@code f(x) = a * x + b} を適用する。
	 * 要素は {long[] {value, cnt}} で表す。
	 * 作用は {@code long[] {b, a}} で表す。
	 * @param N 要素数
	 * @param mod 法
	 * @return 区間 affine 変換・区間和取得用の遅延セグメント木
	 */
	public static LazySegTree<long[], long[]> affine_modSum(int N, long mod) {
		//https://atcoder.jp/contests/abc332/submissions/71399167
		//https://atcoder.jp/contests/practice2/submissions/79680977
    	var seg=new LazySegTree<long[], long[]>(N, new LazySegTreeStrategy<long[], long[]>() {
			final long[] identityA = new long[] {0, 1};

			@Override
			public long[] identityX() {
				return new long[] {0, 0};
			}

			@Override
			public long[] mergeX(long[] a, long[] b) {
				return new long[] {(a[0] + b[0]) % mod, (a[1] + b[1]) % mod};
			}

			@Override
			public long[] identityA() {
				return identityA;
			}

			@Override
			public long[] mergeA(long[] newer, long[] older) {
				return new long[] {(newer[1] * older[0] + newer[0]) % mod, newer[1] * older[1] % mod};
			}

			@Override
			public long[] mergeAX(long[] a, long[] x) {
				return new long[] {(a[0] * x[1] + a[1] * x[0]) % mod, x[1]};
			}
		});
    	return seg;
	}
	
	
	
	/**
	 * 複数のminargがある場合、最小を返す。
	 * @param n
	 * @return
	 */
    public static AddArgmin add_Argmin(int n) {
    	return new AddArgmin(n);
    }
    
    public static LazySegTree<Long, long[]> add_countMin(int N) {
    	return new LazySegTree<Long, long[]>(N, new LazySegTreeStrategy<Long, long[]>() {

			@Override
			public long[] identityX() {
				return new long[] {Long.MAX_VALUE/3, 0};
			}

			@Override
			public long[] mergeX(long[] a, long[] b) {//long[]{min, minの個数}
				if(a[0]==b[0]) {
					return new long[] {a[0], a[1]+b[1]};
				} else if (a[0] < b[0]) {
					return new long[] {a[0], a[1]};
				} else {
					return new long[] {b[0], b[1]};
				}
			}

			@Override
			public Long identityA() {
				return 0L;
			}

			@Override
			public Long mergeA(Long newer, Long older) {
				return newer + older;
			}

			@Override
			public long[] mergeAX(Long a, long[] x) {
				return new long[] {x[0] + a, x[1]};
			}
			
		});
    }
    
	/**
	 * minの個数を返す。
	 * @param n
	 * @return
	 */
    public static Add_CountMax add_countMax(int n) {
    	return new Add_CountMax(n);
    }
    
    /**
     * 複数のmaxargがある場合、最小を返す。初期値は-∞,-∞,-∞,...
     * オーバーフロー対策している
     * @param n
     * @return
     */
    public static Safeadd_Argmax safeadd_Argmax(int n) {
    	return new Safeadd_Argmax(n);
    }
    
    /**
     * 複数のmaxargがある場合、最小を返す。初期値は-∞,-∞,-∞,...
     * オーバーフロー対策していない
     * @param n
     * @return
     */
    public static Add_Argmax add_Argmax(int n) {
    	return new Add_Argmax(n);
    }
    
    public static RangeAddPointGetBinaryIndexedTree add_pointget(int n) {
    	return new RangeAddPointGetBinaryIndexedTree(n);
    }

    /**
     * -∞で初期化
     * @param n
     * @return
     */
    public static LazySegTreelonglong add_min(int n) {
    	var seg=new LazySegTreelonglong(n, Long::sum, Long::min, Long::sum, 0L, Long.MAX_VALUE);
    	return seg;
    }
    
    
    public static LazySegTreelonglong add_max(int n) {
    	var seg=new LazySegTreelonglong(n, Long::sum, Long::max, Long::sum, 0L, Long.MIN_VALUE);
    	return seg;
    }
    
    
    public static LazySegTreelonglong add_max(int n, long initialValue) {
    	var seg=new LazySegTreelonglong(n, Long::sum, Long::max, Long::sum, 0L, Long.MIN_VALUE);
    	seg.fill(initialValue);
    	return seg;
    }
    
    public static SegTree<long[][]> matrixConvolutionZZn(int N, long mod) {
    	var tree=new SegTree<long[][]>(N, (x, y)->{
	    	if (x[0].length!=y.length)throw new AssertionError();
	    	long[][]z=new long[x.length][y[0].length];
	    	for (int i = 0; i < x.length; i++) {
				for (int j = 0; j < y[0].length; j++) {
					for (int k = 0; k < x[i].length; k++) {
						z[i][j]+=x[i][k]*y[k][j];
						z[i][j]%=mod;
					}
				}
			}
	    	return z;
	    });
        return tree;
    }
    

    /**
     * INF=Long.MAX_VALUE/2
     * @param n
     * @return
     */
    public static SegTree<long[][]> minplusMatrixConvolution(int n) {
        var tree=new SegTree<long[][]>(n, (x, y)->{
        	if (x[0].length!=y.length)throw new AssertionError();
        	long[][]z=new long[x.length][y[0].length];
        	ArrayUtils.fill(z, Long.MAX_VALUE/2);
        	for (int i = 0; i < x.length; i++) {
				for (int j = 0; j < y[0].length; j++) {
					for (int k = 0; k < x[i].length; k++) {
						z[i][j]=Math.min(z[i][j], x[i][k]+y[k][j]);
					}
				}
			}
        	return z;
        });
        return tree;
    }
    

    public static IntSumBinaryIndexedTree intsum(int n) {
    	//https://atcoder.jp/contests/abc426/submissions/74055406
    	//セグ木だとTLEしたのでBITに置き換え
    	var tree=new IntSumBinaryIndexedTree(n);
    	return tree;
    }

    /**
     * 長さ {@code n} の 0 列に対する一点更新・区間和取得用 Binary Indexed Tree を返す。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param n 列の長さ
     * @return 0 で初期化された Binary Indexed Tree
     */
    public static LongAbelianGroupBinaryIndexedTree sum(int n) {
        return new LongAbelianGroupBinaryIndexedTree(n,
                library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy.STRATEGY);
    }
    
   /**
    * 0で初期化
    * @param n
    * @return
    */
   public static RangeAddRangeSum add_sum(int n) {
	   return new RangeAddRangeSum(n);
   }

   /**
    * 重み付き区間加算・重み付き区間和取得を行う構造を生成する。
    * @param weights 各要素の重み c_i
    * @return 重み付き区間加算・区間和取得
    */
   public static RangeAddWeightedSum add_weightedSum(long[] weights) {
	   return new RangeAddWeightedSum(weights);
   }

   /**
    * 区間加算・区間 {@code k} 乗和取得を法 {@code mod} で行う遅延セグメント木を生成する。
    * 各要素の 0 乗和（要素数）、1 乗和、...、{@code k} 乗和を保持する。
    *
    * <p>計算量: 構築 O(n k), 1 操作あたり O(k^2 log n)</p>
    *
    * 未テスト
    * @param n 要素数
    * @param k 保持する累乗の最大次数 (k ≥ 0)
    * @param mod 法
    * @return 区間加算・区間 k 乗和取得用の遅延セグメント木
    */
   public static LazySegTree<Long, long[]> modAdd_powerSum(int n, int k, long mod) {
       if (k < 0) {
           throw new IllegalArgumentException("k must be non-negative: " + k);
       }
       long[][] C = new Zn(mod).combTable(k);

       var seg = new LazySegTree<>(n, new LazySegTreeStrategy<Long, long[]>() {
           @Override
           public long[] identityX() {
               return new long[k + 1];
           }

           @Override
           public long[] mergeX(long[] a, long[] b) {
               long[] ret = new long[k + 1];
               for (int i = 0; i <= k; i++) {
                   long sum = a[i] + b[i];
                   if (sum >= mod) sum %= mod;
                   ret[i] = sum;
               }
               return ret;
           }

           @Override
           public Long identityA() {
               return 0L;
           }

           @Override
           public Long mergeA(Long newer, Long older) {
               long sum = newer + older;
               if (sum >= mod) sum -= mod;
               return sum;
           }

           @Override
           public long[] mergeAX(Long a, long[] x) {
               long[] ret = new long[k + 1];
               if (a == 0) {
                   System.arraycopy(x, 0, ret, 0, k + 1);
                   return ret;
               }

               ret[0] = x[0];
               for (int j = 1; j <= k; j++) {
                   long sum = x[0];
                   for (int m = 1; m <= j; m++) {
                       sum = (sum * a + C[j][m] * x[m]) % mod;
                   }
                   ret[j] = sum;
               }
               return ret;
           }
       });
       return seg;
   }

   
   /**
    * 0で初期化
    * @param n
    * @return
    */
   public static RangeAddRangeModSum modAdd_modSum(int n, long mod) {
	   return new RangeAddRangeModSum(n, mod);
   }
    
    /**
     * 長さ {@code n} の 0 列に対する一点更新・区間和取得用 Binary Indexed Tree を返す。
     * 全ての値と和は法 {@code mod} で正規化される。
     *
     * <p>計算量: O(n)
     *
     * 未テスト
     * @param n 列の長さ。n ≥ 0
     * @param mod 法。mod > 0
     * @return 0 で初期化された法付き Binary Indexed Tree
     */
    public static LongAbelianGroupBinaryIndexedTree modSum(int n, long mod) {
        return new LongAbelianGroupBinaryIndexedTree(n,
                new LongModAddAbelianGroupStrategy(mod));
    }
    
    public static MaxSeg max(int n) {
    	return new MaxSeg(n);
    }
    
    /**
     * 単位元はLong.MAX_VALUE/3
     * @param n
     * @return
     */
    public static MinSeg min(int n) {
    	return new MinSeg(n);
    }

    /**
     * {@code set(i, v)} で {@code a[i] = v} を更新し、
     * {@code get(i)} で {@code min_j (|i-j| + a[j])} を返すデータ構造を生成する。
     * 初期値はすべて {@link L1DistanceMinQuery#INF}。
     *
     * @param n 配列長
     * @return L1 距離つき最小値クエリ構造
     */
    public static L1DistanceMinQuery l1DistanceMinQuery(int n) {
    	return new L1DistanceMinQuery(n);
    }
    
    /**
     * identityX=Long.MAX_VALUE
     * @param n
     * @return
     */
    public static LazySegTreelonglong min_min(int n) {
//    	var tree=new LazySegTree<Long, Long>(n, Long::min, Long::min, Long::min, Long.MAX_VALUE);
    	var tree=new LazySegTreelonglong(n, Long::min, Long::min, Long::min, Long.MAX_VALUE, Long.MAX_VALUE);
    	return tree;
    }
    
    /**
     * rangeAssignに単位元がないので、Long.MAX_Valueを便宜上の単位元にしている。
     * @param n
     * @return
     */
    public static LazySegTreelonglong assign_min(int n) {
    	return new LazySegTreelonglong(n, (x, y) -> x, Long::min, (x, y) -> x, Long.MAX_VALUE, Long.MAX_VALUE);
    }

    /**
     * rangeAssign に単位元がないため、Long.MIN_VALUE を便宜上の単位元としている区間変更・区間最大値取得用セグメント木を生成する。
     *
     * <p>計算量: O(n)</p>
     *
     * @param n 配列長
     * @return 遅延セグメント木
     */
    // 未テスト
    public static LazySegTreelonglong assign_max(int n) {
	return new LazySegTreelonglong(n, (x, y) -> x, Long::max, (x, y) -> x, Long.MIN_VALUE, Long.MIN_VALUE);
    }
    
    /**
     * rangeAssignに単位元がないので、Long.MIN_VALUEを便宜上の単位元にしている。
     * @param n
     * @return
     */
    public static IndexedLazySegTreelonglong assign_sum(int n) {
    	return new IndexedLazySegTreelonglong(n, new IndexedLazySegTreeStrategy_longlong() {
			
			@Override
			public long mergeX(long a, long b, int l, int m, int r) {
				return a+b;
			}
			
			@Override
			public long mergeAX(long a, long x, int l, int r) {
				return a*(r-l);
			}
			
			@Override
			public long mergeA(long newer, long older) {
				return newer;
			}
			
			@Override
			public long identityX() {
				return 0;
			}
			
			@Override
			public long identityA() {
				return Long.MIN_VALUE;
			}
		});
    }

    /**
     * 重み付き区間更新・重み付き区間和取得を行う構造を生成する。
     * rangeAssignに単位元がないので、Long.MIN_VALUEを便宜上の単位元にしている。
     * @param weights 各要素の重み c_i
     * @return 重み付き区間更新・区間和取得
     */
    public static RangeAssignWeightedSum assign_weightedSum(long[] weights) {
        return new RangeAssignWeightedSum(weights);
    }
    
    /**
     * 重み付き区間更新・重み付き区間和取得を行う構造を生成する。
     * rangeAssignに単位元がないので、Long.MIN_VALUEを便宜上の単位元にしている。
     * @param weights 各要素の重み c_i
     * @return 重み付き区間更新・区間和取得
     */
    public static RangeAssignWeightedModSum assign_weightedModSum(long[] weights, long mod) {
        return new RangeAssignWeightedModSum(weights, mod);
    }
    
    
    /**
     * 初期値True
     * @param n
     * @return
     */
    public static SegTree<Boolean> logicalAnd(int n) {
        return new SegTree<Boolean>(n, Boolean::logicalAnd, true);
    }

    /**
     * 初期値 false で logical OR の二項演算を行うセグメント木を生成する。
     *
     * <p>計算量: O(n)</p>
     *
     * @param n 配列長
     * @return セグメント木
     */
    // 未テスト
    public static SegTree<Boolean> logicalOr(int n) {
        return new SegTree<Boolean>(n, Boolean::logicalOr, false);
    }
    
    public static RollingHash rollinghash(int n, long radix, long mod) {
    	return new RollingHash(n, radix, mod);
    }
    
    public static LazySegTreelonglong modMul_sum(int n, long mod) {
    	//https://atcoder.jp/contests/abc450/submissions/74321230
		var seg=new LazySegTreelonglong(n, new LazySegTreeStrategy_longlong() {
			
			@Override
			public long mergeX(long a, long b) {
				long ret = a + b;
				if (ret >= mod) ret -= mod;
				return ret;
			}
			
			@Override
			public long mergeAX(long a, long x) {
				return a * x % mod;
			}
			
			@Override
			public long mergeA(long newer, long older) {
				return newer * older % mod;
			}
			
			@Override
			public long identityX() {
				return 0;
			}
			
			@Override
			public long identityA() {
				return 1;
			}
		});
		return seg;
    }

	/**
	 * 区間 01 反転・区間 1 個数取得用の遅延セグメント木を生成する。
	 *
	 * <p>各要素は long[] { ones, zeros } で表現される。</p>
	 *
	 * <p>計算量: O(N)</p>
	 *
	 * @param N 要素数
	 * @return 遅延セグメント木
	 */
	// 未テスト
	public static LazySegTree<Boolean, long[]> flip_count(int N) {
		var seg = new LazySegTree<>(N, new LazySegTreeStrategy<Boolean, long[]>() {
			@Override
			public long[] identityX() {
				return new long[] { 0, 0 };
			}

			@Override
			public long[] mergeX(long[] a, long[] b) {
				return new long[] { a[0] + b[0], a[1] + b[1] };
			}

			@Override
			public Boolean identityA() {
				return false;
			}

			@Override
			public Boolean mergeA(Boolean newer, Boolean older) {
				return newer ? !older : older;
			}

			@Override
			public long[] mergeAX(Boolean a, long[] x) {
				return a ? new long[] { x[1], x[0] } : x;
			}
		});
		long[][] initArr = new long[N][2];
		for (int i = 0; i < N; i++) {
			initArr[i] = new long[] { 0, 1 };
		}
		seg.build(initArr);
		return seg;
	}

	/**
	 * 区間座標圧縮に対応した区間 01 反転・区間 1 個数取得用の遅延セグメント木構造を生成する。
	 * <p>各葉は long[] { zeros, ones } で表現される。</p>
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Boolean, long[]> compressedInterval_flip_sum() {
		return new CompressedIntervalLazySegTree<>(
				(f1, f2) -> f1 ^ f2,
				(a, b) -> new long[] { a[0] + b[0], a[1] + b[1] },
				(flip, x) -> flip ? new long[] { x[1], x[0] } : x,
				new long[] { 0, 0 },
				false
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間和取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>各葉は long[] { sum, len } で表現され、登録された半開区間 [l, r) の長さ r - l に初期化される。</p>
	 *
	 * <p>計算量: O(1)</p>
	 *
	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_addSum() {
		return new CompressedIntervalLazySegTree<>(
				Long::sum,
				(a, b) -> new long[] { a[0] + b[0], a[1] + b[1] },
				(add, x) -> new long[] { x[0] + add * x[1], x[1] },
				new long[] { 0, 0 },
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間 affine 変換・区間和取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>各葉は long[] { sum, len } で表現され、作用は long[] { b, a } (f(x) = a * x + b) で表される。</p>
	 *
	 * <p>計算量: O(1)</p>

	 * @param mod 法
	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<long[], long[]> compressedInterval_affineModSum(long mod) {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> new long[] { (newer[1] * older[0] + newer[0]) % mod, (newer[1] * older[1]) % mod },
				(a, b) -> new long[] { (a[0] + b[0]) % mod, (a[1] + b[1]) % mod },
				(a, x) -> new long[] { (a[0] * (x[1] % mod) + a[1] * x[0]) % mod, x[1] },
				new long[] { 0, 0 },
				new long[] { 0, 1 }
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間和取得（法 mod）用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @param mod 法
	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_modAddModSum(long mod) {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> (newer + older) % mod,
				(a, b) -> new long[] { (a[0] + b[0]) % mod, (a[1] + b[1]) % mod },
				(add, x) -> new long[] { (x[0] + (add % mod) * (x[1] % mod)) % mod, x[1] },
				new long[] { 0, 0 },
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間最小値取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, Long> compressedInterval_addMin() {
		return new CompressedIntervalLazySegTree<>(
				Long::sum,
				Long::min,
				(add, min) -> min == Long.MAX_VALUE / 2 ? Long.MAX_VALUE / 2 : min + add,
				Long.MAX_VALUE / 2,
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間最大値取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, Long> compressedInterval_addMax() {
		return new CompressedIntervalLazySegTree<>(
				Long::sum,
				Long::max,
				(add, max) -> max == Long.MIN_VALUE / 2 ? Long.MIN_VALUE / 2 : max + add,
				Long.MIN_VALUE / 2,
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間最小値および個数取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_addCountMin() {
		return new CompressedIntervalLazySegTree<>(
				Long::sum,
				(a, b) -> a[0] == b[0] ? new long[] { a[0], a[1] + b[1] } : (a[0] < b[0] ? a : b),
				(add, x) -> new long[] { x[0] + add, x[1] },
				new long[] { Long.MAX_VALUE / 3, 0 },
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間加算・区間最大値および個数取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_addCountMax() {
		return new CompressedIntervalLazySegTree<>(
				Long::sum,
				(a, b) -> a[0] == b[0] ? new long[] { a[0], a[1] + b[1] } : (a[0] > b[0] ? a : b),
				(add, x) -> new long[] { x[0] + add, x[1] },
				new long[] { Long.MIN_VALUE / 3, 0 },
				0L
		);
	}

	/**
	 * 区間座標圧縮に対応した区間変更・区間最小値取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, Long> compressedInterval_assignMin() {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> newer,
				Long::min,
				(a, x) -> a == Long.MAX_VALUE ? x : a,
				Long.MAX_VALUE,
				Long.MAX_VALUE
		);
	}

	/**
	 * 区間座標圧縮に対応した区間変更・区間最大値取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, Long> compressedInterval_assignMax() {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> newer,
				Long::max,
				(a, x) -> a == Long.MIN_VALUE ? x : a,
				Long.MIN_VALUE,
				Long.MIN_VALUE
		);
	}

	/**
	 * 区間座標圧縮に対応した区間変更・区間和取得用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_assignSum() {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> newer,
				(a, b) -> new long[] { a[0] + b[0], a[1] + b[1] },
				(a, x) -> a == Long.MIN_VALUE ? x : new long[] { a * x[1], x[1] },
				new long[] { 0, 0 },
				Long.MIN_VALUE
		);
	}

	/**
	 * 区間座標圧縮に対応した区間乗算・区間和取得（法 mod）用の遅延セグメント木構造を生成する。
	 *
	 * <p>計算量: O(1)</p>

	 * @param mod 法
	 * @return 区間座標圧縮型遅延セグメント木
	 */
	// 未テスト
	public static CompressedIntervalLazySegTree<Long, long[]> compressedInterval_modMulSum(long mod) {
		return new CompressedIntervalLazySegTree<>(
				(newer, older) -> (newer * older) % mod,
				(a, b) -> new long[] { (a[0] + b[0]) % mod, (a[1] + b[1]) % mod },
				(mul, x) -> new long[] { (x[0] * mul) % mod, x[1] },
				new long[] { 0, 0 },
				1L
		);
	}

	/**
	 * 区間 01 反転・区間反転数取得用の遅延セグメント木を生成する。
	 * 未テスト。
	 * 計算量: O(N)
	 * @param N 要素数
	 * @return 遅延セグメント木
	 */
	public static LazySegTree<Boolean, BinaryInversionState> flip_inversionCount(int N) {
		return new LazySegTree<>(N, new LazySegTreeStrategy<Boolean, BinaryInversionState>() {
			@Override
			public BinaryInversionState identityX() {
				return BinaryInversionState.STRATEGY.identity();
			}

			@Override
			public BinaryInversionState mergeX(BinaryInversionState a, BinaryInversionState b) {
				return a.mul(b);
			}

			@Override
			public Boolean identityA() {
				return false;
			}

			@Override
			public Boolean mergeA(Boolean newer, Boolean older) {
				return newer ? !older : older;
			}

			@Override
			public BinaryInversionState mergeAX(Boolean a, BinaryInversionState x) {
				return a ? x.flip() : x;
			}
		});
	}

	/**
	 * 長さ N の列 A = (a_0, a_1, ..., a_(N-1)) に対して、固定長 K の各連続部分列を
	 * 関数 phi(a_i, ..., a_(i+K-1)) で評価し、それらをモノイド演算 op で集約した値
	 *   F(A) = op_(i=0)^(N-K) phi(a_i, ..., a_(i+K-1))
	 * を動的に更新・取得するセグメント木を構築する。
	 *
	 * 各セグメント木のノードは、区間内の先頭および末尾の高々 K-1 要素と、
	 * 区間に完全に含まれる全 K 窓の評価値の集約を保持する。
	 *
	 * 計算量:
	 * 点更新: O(K * (K + T_phi) * log N)
	 * ただし T_phi は関数 phi の評価に要する時間（通常 O(K)）。
	 *
	 * @param <E> 列の要素の型
	 * @param <R> 評価結果・集約値の型
	 * @param N 列の長さ
	 * @param strategy 窓評価戦略
	 * @return 構築されたセグメント木
	 */
	// 未テスト
	public static <E, R> WindowEvaluationSegTree<E, R> windowEvaluation(int N, WindowEvaluationStrategy<E, R> strategy) {
		//https://atcoder.jp/contests/jsc2019-final/submissions/78136488
		return new WindowEvaluationSegTree<>(N, strategy);
	}

	/**
	 * 長さ N の列 A = (a_0, a_1, ..., a_(N-1)) に対して、固定長 K の各連続部分列を
	 * 関数 phi(a_i, ..., a_(i+K-1)) で評価し、それらをモノイド演算 op で集約した値
	 *   F(A) = op_(i=0)^(N-K) phi(a_i, ..., a_(i+K-1))
	 * を動的に更新・取得するセグメント木を構築する（初期配列を受け取るオーバーロード）。
	 *
	 * 各セグメント木のノードは、区間内の先頭および末尾の高々 K-1 要素と、
	 * 区間に完全に含まれる全 K 窓の評価値の集約を保持する。
	 *
	 * 計算量:
	 * 構築: O(N * K + N * T_phi)
	 * 点更新: O(K * (K + T_phi) * log N)
	 * ただし T_phi は関数 phi の評価に要する時間（通常 O(K)）。
	 *
	 * @param <E> 列の要素の型
	 * @param <R> 評価結果・集約値の型
	 * @param initialArray 初期列
	 * @param strategy 窓評価戦略
	 * @return 構築されたセグメント木
	 */
	// 未テスト
	public static <E, R> WindowEvaluationSegTree<E, R> windowEvaluation(E[] initialArray, WindowEvaluationStrategy<E, R> strategy) {
		return new WindowEvaluationSegTree<>(initialArray, strategy);
	}

	/**
	 * 区間和 {@code sum} と区間最小接頭辞和 {@code minOfPrefixSum} を保持する状態。
	 */
	public static class State implements MonoidElement<State> {
		/** 単位元。 */
		public static final State IDENTITY = new State(0, Integer.MAX_VALUE);

		/**
		 * State モノイドのストラテジ。
		 */
		public static final MonoidStrategy<State> STRATEGY = new MonoidStrategy<>() {
			@Override
			public State identity() {
				return IDENTITY;
			}

			@Override
			public State mul(State a, State b) {
				if (a == IDENTITY) return b;
				if (b == IDENTITY) return a;
				return new State(a.sum + b.sum, Math.min(a.minOfPrefixSum, a.sum + b.minOfPrefixSum));
			}
		};

		/** 区間要素の総和。 */
		public int sum;
		/** 区間接頭辞和の最小値。 */
		public int minOfPrefixSum;

		/**
		 * コンストラクタ。
		 * @param sum 区間要素の総和
		 * @param minOfPrefixSum 区間接頭辞和の最小値
		 */
		public State(int sum, int minOfPrefixSum) {
			this.sum = sum;
			this.minOfPrefixSum = minOfPrefixSum;
		}

		/**
		 * 2つの状態をマージする。
		 * 未テスト。
		 * 計算量: O(1)
		 * @param b 右側の状態
		 * @return マージ後の状態
		 */
		public State merge(State b) {
			return STRATEGY.mul(this, b);
		}

		@Override
		public MonoidStrategy<State> parent() {
			return STRATEGY;
		}

		@Override
		public State self() {
			return this;
		}

		@Override
		public String toString() {
			return "(" + sum + " " + minOfPrefixSum + ")";
		}
	}

	/**
	 * 長さ {@code n} の列に対して、区間和および区間最小接頭辞和（prefix sum の最小値）を管理するセグメント木を生成する。
	 * 単位元は {@link State#IDENTITY}。
	 *
	 * 未テスト。
	 * 計算量: O(n)
	 * @param n 列の長さ
	 * @return セグメント木
	 */
	public static SegTree<State> minPrefixSum(int n) {
		return new SegTree<>(n, State::merge, State.IDENTITY);
	}

}
