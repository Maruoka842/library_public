package library.util;

import java.util.Arrays;
import java.util.OptionalLong;

public final class CardinalityKnapsack {
    static final long NEG = Long.MIN_VALUE / 4;
    
    //選択個数を固定したナップサック DP の1行。
    //A[x] + voff は、重み baseWeight + x をちょうど実現する選び方のうち、価値の最大値を表す。
    private static final class Row {
        long baseWegiht = 0;// A[0] に対応する重み
        long voff = 0;// 全状態に共通する価値の加算分
        long[] A = new long[0];

        Row copy() {
            Row r = new Row();
            r.baseWegiht = baseWegiht;
            r.voff = voff;
            r.A = A.clone();
            return r;
        }
        
        /** 到達可能な状態が1つもない行にする。空のとき L, voff は意味を持たないので初期値に戻す。 */
        void clear() {
            A = new long[0];
            baseWegiht = 0;
            voff = 0;
        }

        void trim(long capacity) {
            long maxLen = capacity - baseWegiht + 1;
            if (A.length > maxLen) A = Arrays.copyOf(A, (int) maxLen);
        }
    }

    private final int K;
    private final long B;
    private final Row[] rows;

    /**
     *  @param maxCount 選択個数の上限
     *  @param capacity 重みの上限
     */
    public CardinalityKnapsack(int maxCount, long capacity) {
        if (maxCount < 0 || capacity < 0) throw new IllegalArgumentException("K and B must be >= 0");
        if (capacity >= Integer.MAX_VALUE - 8) throw new IllegalArgumentException("capacity too large");
        K = maxCount;
        B = capacity;
        rows = new Row[K + 1];
        for (int k = 0; k <= K; k++) rows[k] = new Row();
        rows[0].A = new long[] {0};
    }

    public CardinalityKnapsack copy() {
        CardinalityKnapsack c = new CardinalityKnapsack(K, B);
        for (int k = 0; k <= K; k++) c.rows[k] = rows[k].copy();
        return c;
    }

    public int maxCount() { return K; }
    public long capacity() { return B; }

    /** 商品を1個追加。O(maxCount * maxCapacity)。*/
    public void addItem(long w, long v) {
    	//https://atcoder.jp/contests/abc485/submissions/81021718
        if (w < 0) throw new IllegalArgumentException("weight must be >= 0");
        if (w > B) return;
        for (int k = K; k >= 1; k--) { // 大きい k から: rows[k-1] はまだ旧状態
            Row cur = rows[k];
            Row prev = rows[k - 1];
            if (prev.A.length == 0 || prev.baseWegiht + w > B) continue;
            boolean hasCur = cur.A.length > 0;
            long lo = prev.baseWegiht + w;
            long hi = prev.baseWegiht + prev.A.length - 1 + w;
            if (hasCur) {
                lo = Math.min(lo, cur.baseWegiht);
                hi = Math.max(hi, cur.baseWegiht + cur.A.length - 1);
            }
            hi = Math.min(hi, B);
            long[] nA = new long[(int) (hi - lo + 1)];
            Arrays.fill(nA, NEG);
            if (hasCur) {
                for (int x = 0; x < cur.A.length; x++)
                    if (cur.A[x] != NEG) nA[(int) (cur.baseWegiht + x - lo)] = cur.A[x] + cur.voff;
            }
            for (int x = 0; x < prev.A.length; x++) {
                long s = prev.baseWegiht + x + w;
                if (s > B) break;
                if (prev.A[x] == NEG) continue;
                long val = prev.A[x] + prev.voff + v;
                int idx = (int) (s - lo);
                if (val > nA[idx]) nA[idx] = val;
            }
            cur.baseWegiht = lo;
            cur.voff = 0;
            cur.A = nA;
        }
    }

    /** ちょうど k 個・重みちょうど s。O(1)。 */
    public OptionalLong getExact(int k, long s) {
        if (k < 0 || k > K || s < 0 || s > B) return OptionalLong.empty();
        Row r = rows[k];
        long x = s - r.baseWegiht;
        if (x < 0 || x >= r.A.length || r.A[(int) x] == NEG) return OptionalLong.empty();
        return OptionalLong.of(r.A[(int) x] + r.voff);
    }

    /** 登録済みの全商品の重みを d (>=0) だけ増やす。O(maxCount)。 */
    public void increaseAllWeights(long d) {
    	//https://atcoder.jp/contests/abc485/submissions/81021718
        if (d < 0) throw new IllegalArgumentException("increaseAllWeights: d must be >= 0");
        if (d == 0) return;
        for (int k = 1; k <= K; k++) {
            Row r = rows[k];
            if (r.A.length == 0) continue;
            // k*d, L はともに long に収まる範囲を想定しつつ、溢れは「容量超過」として扱う
            try {
                long nl = Math.addExact(r.baseWegiht, Math.multiplyExact((long) k, d));
                if (nl > B) {
                    r.clear();
                } else {
                    r.baseWegiht = nl;
                    r.trim(B);
                }
            } catch (ArithmeticException e) {
                r.clear();
            }
        }
    }

    /** 登録済みの全商品の価値に g を加える(負でもよい)。O(maxCount)。 */
    public void addToAllValues(long g) {
        for (int k = 1; k <= K; k++) rows[k].voff += g * k;
    }

}