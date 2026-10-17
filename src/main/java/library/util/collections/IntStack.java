package library.util.collections;

public class IntStack {

    private IntDeque d = new IntDeque();

    public void add(int v) {
        d.addLast(v);
    }

    public int poll() {
        return d.pollLast();
    }

    public int peek() {
        return d.peekLast();
    }

    public boolean isEmpty() {
        return d.isEmpty();
    }

    public int size() {
        return d.size();
    }

    /**
     * スタックを空にする。
     * O(1)
     * // 未テスト
     */
    public void clear() {
        d.clear();
    }

    public IntStack copy() {
        IntStack ret = new IntStack();
        ret.d = d.copy();
        return ret;
    }

    /**
     * スタックの内容を表す文字列を返す。
     *
     * @return スタックの内容を表す文字列
     * O(N)
     * // 未テスト
     */
    @Override
    public String toString() {
        return d.toString();
    }

    /**
     * スタックの内容を標準出力に出力する。
     * O(N)
     * // 未テスト
     */
    public void dump() {
        System.out.println(toString());
    }

    /**
     * このスタックと別のオブジェクトの同値性を判定します。
     *
     * <p>計算量: O(N)（N はスタックの要素数）</p>
     *
     * @param obj 比較対象のオブジェクト
     * @return 同値であれば true, そうでなければ false
     */
    // 未テスト
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IntStack)) return false;
        IntStack other = (IntStack) obj;
        return java.util.Objects.equals(this.d, other.d);
    }

    /**
     * このスタックのハッシュコードを計算します。
     *
     * <p>計算量: O(N)（N はスタックの要素数）</p>
     *
     * @return ハッシュコード
     */
    // 未テスト
    @Override
    public int hashCode() {
        return d.hashCode();
    }
}

