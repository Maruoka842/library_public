package library.util.linalg;

import java.util.BitSet;
import java.util.List;

/**
 * F2 ベクトルの集合や生成空間を解析する抽象基底クラスを提供する。
 *
 * <p>F2 ベクトルは {@link BitSet} (次元 n) を用いて表現される。</p>
 */
public abstract class F2VectorSpace {

    /**
     * 次元 n における生成元ベクトル列 v_1, v_2, ..., v_m (各 v_j in F2^n) を返す。
     *
     * @param n ベクトルの次元 (ビット長)
     * @return 生成元ベクトルのリスト (各要素は BitSet)
     */
    public abstract List<BitSet> generators(int n);

    /**
     * F2 ベクトル v に対し、指定された次元 n における標準簡約形 (canonical reduced form) を返す。
     *
     * @param v F2 ベクトル
     * @param n ベクトルの次元 (ビット長)
     * @return 簡約された F2 ベクトル (BitSet)
     */
    public abstract BitSet reduce(BitSet v, int n);

    /**
     * 次元 n における 2 つの F2 ベクトル u, v が同値関係（生成空間・操作の有限回適用で移行可能か）を満たすかを判定する。
     *
     * @param u 1 つ目の F2 ベクトル
     * @param v 2 つ目の F2 ベクトル
     * @param n ベクトルの次元 (ビット長)
     * @return 移行可能であれば true、そうでなければ false
     */
    public abstract boolean areEquivalent(BitSet u, BitSet v, int n);

    /**
     * 次元 n における 2 つの F2 ベクトル u, v 間の最小移行操作回数（書き換え距離）を計算する。
     * 移行不能である場合は -1 を返す。
     *
     * @param u 始点となる F2 ベクトル
     * @param v 終点となる F2 ベクトル
     * @param n ベクトルの次元 (ビット長)
     * @return 最小操作回数。移行不能の場合は -1
     */
    public abstract long distance(BitSet u, BitSet v, int n);
}
