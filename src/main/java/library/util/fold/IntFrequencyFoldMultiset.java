package library.util.fold;

import java.util.Arrays;

/**
 * 値域 {@code [0, n)} の整数からなる多重集合について、
 * 多重集合全体から定まる値を動的に管理する。
 *
 * <p>各 {@code x} の出現回数を {@code count[x]} とする。
 * {@code x} の出現回数を {@code oldCount} から {@code newCount} に変更したとき、
 * 現在の集約値から変更後の集約値を O(1) で計算できるような量を扱う。
 */
public class IntFrequencyFoldMultiset {

    /**
     * 出現回数の変更に伴う集約値の更新方法を定義する。
     */
    public interface Strategy {

        /**
         * 空の多重集合に対する集約値を返す。
         *
         * @return 空の多重集合に対する集約値
         */
        long identity();

        /**
         * {@code key} の出現回数を {@code oldCount} から {@code newCount} に変更した後の
         * 集約値を返す。
         *
         * <p>{@code fold} は変更前の多重集合全体に対する集約値である。
         * {@code oldCount}, {@code newCount} はともに 0 以上である。
         *
         * @param fold 変更前の多重集合全体に対する集約値
         * @param key 出現回数を変更する値
         * @param oldCount 変更前の {@code key} の出現回数
         * @param newCount 変更後の {@code key} の出現回数
         * @return 変更後の多重集合全体に対する集約値
         */
        long changeCount(long fold, int key, int oldCount, int newCount);
    }

    private final int[] count;
    private final Strategy strategy;
    private long fold;

    /**
     * 値域 {@code [0, n)} の空の多重集合を構築する。
     *
     * @param n 値域の大きさ
     * @param strategy 集約値の更新方法
     */
    public IntFrequencyFoldMultiset(int n, Strategy strategy) {
        count = new int[n];
        this.strategy = strategy;
        fold = strategy.identity();
    }

    private IntFrequencyFoldMultiset(int[] count, Strategy strategy, long fold) {
        this.count = count;
        this.strategy = strategy;
        this.fold = fold;
    }

    /**
     * 全ての要素を削除する。
     * 事後条件: 任意の x in [0, n) について {@code count(x) = 0} かつ
     * {@code fold() = strategy.identity()}。
     * 計算量: O(n)。ただし n は値域の大きさ。
     */
    // 未テスト
    public void clear() {
        Arrays.fill(count, 0);
        fold = strategy.identity();
    }

    /**
     * この多重集合のディープコピーを生成して返します。
     *
     * <p>計算量: O(n) (n は値域の大きさ)</p>
     *
     * @return この多重集合のディープコピー
     */
    // 未テスト
    public IntFrequencyFoldMultiset copy() {
        return new IntFrequencyFoldMultiset(count.clone(), strategy, fold);
    }

    /**
     * {@code x} を 1 個追加する。
     *
     * @param x 追加する値
     */
    public void add(int x) {
        int oldCount = count[x];
        int newCount = oldCount + 1;
        fold = strategy.changeCount(fold, x, oldCount, newCount);
        count[x] = newCount;
    }

    /**
     * {@code x} を {@code multiplicity} 個追加する。
     *
     * <p>{@code multiplicity == 0} の場合は何もしない。
     *
     * @param x 追加する値
     * @param multiplicity 追加する個数
     * @throws IllegalArgumentException {@code multiplicity < 0} の場合
     */
    public void add(int x, int multiplicity) {
        if (multiplicity < 0) {
            throw new IllegalArgumentException("negative multiplicity: " + multiplicity);
        }
        if (multiplicity == 0) {
            return;
        }

        int oldCount = count[x];
        int newCount = oldCount + multiplicity;
        fold = strategy.changeCount(fold, x, oldCount, newCount);
        count[x] = newCount;
    }

    /**
     * {@code x} を 1 個削除する。
     *
     * @param x 削除する値
     * @throws IllegalArgumentException {@code x} が存在しない場合
     */
    public void remove(int x) {
        int oldCount = count[x];
        if (oldCount == 0) {
            throw new IllegalArgumentException("element does not exist: " + x);
        }

        int newCount = oldCount - 1;
        fold = strategy.changeCount(fold, x, oldCount, newCount);
        count[x] = newCount;
    }

    /**
     * {@code x} を {@code multiplicity} 個削除する。
     *
     * <p>{@code multiplicity == 0} の場合は何もしない。
     *
     * @param x 削除する値
     * @param multiplicity 削除する個数
     * @throws IllegalArgumentException {@code multiplicity < 0} の場合、
     *         または {@code x} が {@code multiplicity} 個未満しか存在しない場合
     */
    public void remove(int x, int multiplicity) {
        if (multiplicity < 0) {
            throw new IllegalArgumentException("negative multiplicity: " + multiplicity);
        }
        if (multiplicity == 0) {
            return;
        }

        int oldCount = count[x];
        if (oldCount < multiplicity) {
            throw new IllegalArgumentException(
                    "not enough elements: x=" + x
                    + ", count=" + oldCount
                    + ", remove=" + multiplicity);
        }

        int newCount = oldCount - multiplicity;
        fold = strategy.changeCount(fold, x, oldCount, newCount);
        count[x] = newCount;
    }

    /**
     * {@code x} の出現回数を {@code newCount} に変更する。
     *
     * @param x 値
     * @param newCount 新しい出現回数
     * @throws IllegalArgumentException {@code newCount < 0} の場合
     */
    public void setCount(int x, int newCount) {
        if (newCount < 0) {
            throw new IllegalArgumentException("negative count: " + newCount);
        }

        int oldCount = count[x];
        if (oldCount == newCount) {
            return;
        }

        fold = strategy.changeCount(fold, x, oldCount, newCount);
        count[x] = newCount;
    }

    /**
     * {@code x} の出現回数を返す。
     *
     * @param x 値
     * @return {@code x} の出現回数
     */
    public int count(int x) {
        return count[x];
    }

    /**
     * 現在の多重集合全体に対する集約値を返す。
     *
     * @return 集約値
     */
    public long fold() {
        return fold;
    }

    /**
     * 多重集合の内部状態を表す文字列を返します。
     *
     * <p>計算量: O(n) (n は値域の大きさ)</p>
     *
     * @return 内部状態を表す文字列
     */
    // 未テスト
    @Override
    public String toString() {
        return "IntFrequencyFoldMultiset{count=" + java.util.Arrays.toString(count) + ", fold=" + fold + "}";
    }

    /**
     * 多重集合の内部状態を標準出力に出力します。
     *
     * <p>計算量: O(n) (n は値域の大きさ)</p>
     */
    // 未テスト
    public void dump() {
        System.out.println(toString());
    }
}
