package library.util.poset;

/**
 * CSR (Compressed Sparse Row) 形式で整数の約数を保持し、管理するクラス。
 *
 * <p>1 から V までのすべての整数の約数列をフラットな配列として連結し、
 * オフセット配列を用いて効率的にアクセス可能にする。
 *
 * <p>各整数 x の約数は、{@code list[offset[x]]} から {@code list[offset[x + 1]]}
 * までの半開区間に昇順で格納される。
 */
// 未テスト
public class DivisorsCSR {

    /**
     * 各整数 x に対して、その約数列における開始位置と終了位置を保持するオフセット配列。
     * offset[x] は x の最初の約数のインデックスを表す。
     */
    public static int[] offset;

    /**
     * 1 から最大値までの全整数の約数をフラットに連結した配列。
     * 各整数の約数は昇順で格納される。
     */
    public static int[] list;

    /**
     * 約数テーブルを指定された最大値まで拡張する。
     *
     * <p>1 から V までのすべての整数に対する約数リストを計算し、
     * offset および list を更新する。
     *
     * <p>計算量: O(V log V)
     *
     * @param V 最大値
     */
    public synchronized static void expand(int V) {
        if (offset != null && offset.length > V + 1) {
            return;
        }
        int curV = offset == null ? 0 : offset.length - 2;
        int nextV = Math.max(V, curV * 2);

        int[] count = new int[nextV + 1];
        for (int i = 1; i <= nextV; i++) {
            for (int j = i; j <= nextV; j += i) {
                count[j]++;
            }
        }

        int[] newOffset = new int[nextV + 2];
        for (int i = 1; i <= nextV; i++) {
            newOffset[i + 1] = newOffset[i] + count[i];
        }

        int[] newList = new int[newOffset[nextV + 1]];
        int[] cur = newOffset.clone();
        for (int i = 1; i <= nextV; i++) {
            for (int j = i; j <= nextV; j += i) {
                newList[cur[j]++] = i;
            }
        }

        offset = newOffset;
        list = newList;
    }
}
