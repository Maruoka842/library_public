package library.util.linalg;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import library.util.collections.IntArrayList;

/**
 * k 長の連続同値ブロック反転操作 (0^k <-> 1^k) によって生成される F2 ベクトル空間を表す実装クラス。
 *
 * <p>F2 ベクトルは {@link BitSet} (次元 n) を用いて表現される。</p>
 */
public class F2BlockFlipsVectorSpace extends F2VectorSpace {

    /** ブロック長 */
    private final int k;

    /**
     * ブロック長 k を指定して初期化する。
     *
     * <p>計算量: O(1)</p>
     *
     * @param k ブロック長 (k >= 1)
     */
    // 未テスト
    public F2BlockFlipsVectorSpace(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("k must be at least 1");
        }
        this.k = k;
    }

    /**
     * ブロック長 k を返す。
     *
     * <p>計算量: O(1)</p>
     *
     * @return ブロック長 k
     */
    // 未テスト
    public int getK() {
        return k;
    }

    /**
     * 次元 n における k 長ブロック反転操作の生成元ベクトル列 (指定位置に 1^k を持つ BitSet 群) を返す。
     *
     * <p>計算量: O(n k)</p>
     *
     * @param n ベクトルの次元 (ビット長)
     * @return 生成元ベクトルのリスト (各要素は BitSet)
     */
    // 未テスト
    @Override
    public List<BitSet> generators(int n) {
        List<BitSet> gens = new ArrayList<>();
        if (n < k || k <= 0) return gens;
        for (int i = 0; i <= n - k; i++) {
            BitSet g = new BitSet(n);
            g.set(i, i + k);
            gens.add(g);
        }
        return gens;
    }

    /**
     * F2 ベクトル v に対し、指定された次元 n における k 長の連続同値ブロック 0^k および 1^k を消去する標準簡約形 (canonical reduced form) を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * @param v F2 ベクトル (BitSet)
     * @param n ベクトルの次元 (ビット長)
     * @return 簡約された F2 ベクトル (BitSet)
     */
    // 未テスト
    @Override
    public BitSet reduce(BitSet v, int n) {
        if (v == null) return null;
        IntArrayList stack = new IntArrayList(n);
        IntArrayList count = new IntArrayList(n);
        for (int p = 0; p < n; p++) {
            int val = v.get(p) ? 1 : 0;
            if (stack.isEmpty() || stack.peekLast() != val) {
                stack.add(val);
                count.add(1);
            } else {
                int lastIdx = count.size() - 1;
                count.set(lastIdx, count.get(lastIdx) + 1);
            }
            if (count.peekLast() == k) {
                stack.pollLast();
                count.pollLast();
            }
        }
        BitSet result = new BitSet();
        int idx = 0;
        for (int i = 0; i < stack.size(); i++) {
            int val = stack.get(i);
            int cnt = count.get(i);
            for (int j = 0; j < cnt; j++) {
                if (val == 1) {
                    result.set(idx);
                }
                idx++;
            }
        }
        return result;
    }

    /**
     * 次元 n における 2 つの F2 ベクトル u, v が k 長ブロック反転操作の有限回適用により互いに移行可能か（同値関係を満たすか）を判定する。
     *
     * <p>計算量: O(n)</p>
     *
     * @param u 1 つ目の F2 ベクトル (BitSet)
     * @param v 2 つ目の F2 ベクトル (BitSet)
     * @param n ベクトルの次元 (ビット長)
     * @return 移行可能であれば true、そうでなければ false
     */
    // 未テスト
    @Override
    public boolean areEquivalent(BitSet u, BitSet v, int n) {
        if (u == null || v == null) return false;
        return reduce(u, n).equals(reduce(v, n));
    }

    /**
     * 次元 n における 2 つの F2 ベクトル u, v 間の k 長ブロック反転操作による最小操作回数（書き換え距離）を計算する。
     * 互いに移行不能である場合は -1 を返す。
     *
     * <p>計算量: O(n)</p>
     *
     * @param u 始点となる F2 ベクトル (BitSet)
     * @param v 終点となる F2 ベクトル (BitSet)
     * @param n ベクトルの次元 (ビット長)
     * @return 最小操作回数。移行不能の場合は -1
     */
    // 未テスト
    @Override
    public long distance(BitSet u, BitSet v, int n) {
        if (u == null || v == null) return -1;
        if (!areEquivalent(u, v, n)) return -1;
        if (n == 0) return 0;

        IntArrayList stackA = new IntArrayList(n + 1);
        IntArrayList stackB = new IntArrayList(n + 1);
        int lcp = 0;

        long sum = 0;

        for (int p = 0; p < n; p++) {
            int bitA = (p == 0) ? (u.get(0) ? 1 : 0) : ((u.get(p - 1) ^ u.get(p)) ? 1 : 0);
            if (bitA == 1) {
                int r = p % k;
                if (!stackA.isEmpty() && stackA.peekLast() == r) {
                    stackA.pollLast();
                    if (lcp > stackA.size()) lcp = stackA.size();
                } else {
                    if (lcp == stackA.size() && stackA.size() < stackB.size() && stackB.get(stackA.size()) == r) {
                        lcp++;
                    }
                    stackA.add(r);
                }
            }

            int bitB = (p == 0) ? (v.get(0) ? 1 : 0) : ((v.get(p - 1) ^ v.get(p)) ? 1 : 0);
            if (bitB == 1) {
                int r = p % k;
                if (!stackB.isEmpty() && stackB.peekLast() == r) {
                    stackB.pollLast();
                    if (lcp > stackB.size()) lcp = stackB.size();
                } else {
                    if (lcp == stackB.size() && stackB.size() < stackA.size() && stackA.get(stackB.size()) == r) {
                        lcp++;
                    }
                    stackB.add(r);
                }
            }
            sum += stackA.size() + stackB.size() - 2L * lcp;
        }

        if (sum % k != 0) throw new AssertionError();
        return sum / k;
    }
}
