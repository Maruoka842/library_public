package library.util;

/**
 * 各要素に付いたラベルを、一段の間接参照を介して管理する。
 *
 * <p>内部的には各要素 {@code i} に内部ラベル {@code f(i)} を持たせ、
 * 実際のラベルを
 *
 * <pre>
 * g(f(i))
 * </pre>
 *
 * として表す。{@code g} はラベル集合上の全単射である。
 *
 * <p>この間接参照により、2つの実ラベルを全要素について交換する操作を
 * {@code O(1)} で行える。
 *
 * <p>ラベルは {@code 0 <= label < labelCount} とする。
 *
 * @complexity
 * {@code get}, {@code set}, {@code swapLabels} はすべて O(1)。
 * 空間計算量は O(n + labelCount)。
 */
public class ArrayRelabeling {

    /** f(i): 各要素が保持する内部ラベル */
    private final int[] a;

    /** g(b): 内部ラベル b に対応する実ラベル */
    private final int[] label;

    /** g^(-1)(c): 実ラベル c に対応する内部ラベル */
    private final int[] inv;

    /**
     * 全要素の初期実ラベルを 0 として構築する。
     */
    public ArrayRelabeling(int n, int labelCount) {
        a = new int[n];
        label = new int[labelCount];
        inv = new int[labelCount];
        for (int i = 0; i < labelCount; i++) {
            label[i] = inv[i] = i;
        }
    }

    /**
     * 指定された初期実ラベルから構築する。
     *
     * @param initialLabel 各要素の初期ラベル
     * @param labelCount ラベル数
     */
    public ArrayRelabeling(int[] initialLabel, int labelCount) {
        this(initialLabel.length, labelCount);
        System.arraycopy(initialLabel, 0, a, 0, initialLabel.length);
    }

    /**
     * 要素数を返す。
     */
    public int size() {
        return a.length;
    }

    /**
     * ラベル数を返す。
     */
    public int labelCount() {
        return label.length;
    }

    /**
     * 要素 {@code i} の現在の実ラベルを返す。
     */
    public int get(int i) {
        return label[a[i]];
    }

    /**
     * 要素 {@code i} の実ラベルを {@code value} に変更する。
     */
    public void set(int i, int value) {
        a[i] = inv[value];
    }

    /**
     * 実ラベル {@code x} と {@code y} を、全要素について交換する。
     *
     * <p>すなわち、操作前にラベル {@code x} だったすべての要素は
     * {@code y} になり、{@code y} だったすべての要素は {@code x} になる。
     */
    public void swapLabels(int x, int y) {
        if (x == y) return;

        int ix = inv[x];
        int iy = inv[y];

        label[ix] = y;
        label[iy] = x;

        inv[x] = iy;
        inv[y] = ix;
    }

    /**
     * 現在の実ラベルを通常の int 配列として返す。
     */
    public int[] toArray() {
        int[] res = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = label[a[i]];
        }
        return res;
    }
}