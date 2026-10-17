package library.util.unionfind;

public final class UnionFindRelabeling {
	//https://atcoder.jp/contests/abc279/submissions/79075607
    private final UnionFind uf;

    /**
     * {@code label[r]} は、代表元 {@code r} の連結成分に割り当てられたラベル。
     *
     * <p>代表元でない要素に対する値は未定義であり、参照しない。
     */
    private final int[] label;

    /**
     * {@code inv[l]} は、ラベル {@code l} が割り当てられた連結成分の代表元。
     *
     * <p>ラベル {@code l} が空の場合は {@code -1}。
     */
    private final int[] inv;

    public UnionFindRelabeling(int N) {
        this.uf = new UnionFind(N);
        label = new int[N];
        inv = new int[N];

        for (int i = 0; i < N; i++) {
            label[i] = i;
            inv[i] = i;
        }
    }

    /**
     * 要素 {@code x} が属する連結成分の現在のラベルを返す。
     *
     * @param x 要素
     * @return {@code x} が属する連結成分のラベル
     *
     * @complexity 償却 O(α(N))
     */
    public int get(int x) {
        return label[uf.root(x)];
    }

    /**
     * ラベル {@code from} を持つ連結成分を、
     * ラベル {@code to} を持つ連結成分へ併合する。
     *
     * <p>操作後、併合された連結成分のラベルは {@code to} となり、
     * ラベル {@code from} は空になる。
     *
     * <p>{@code from} が空の場合は何もしない。
     * {@code to} が空の場合は連結成分の併合は行わず、
     * {@code from} の連結成分のラベルを {@code to} に変更するだけである。
     *
     * <p>すなわち、操作前にラベル {@code from} を持っていた全要素は
     * 操作後にラベル {@code to} を持つ。
     * 操作前にラベル {@code to} を持っていた要素もそのまま
     * {@code to} を持つ。
     *
     * @param to 併合先のラベル
     * @param from 併合元のラベル
     *
     * @complexity 償却 O(α(N))
     */
    public void mergeLabels(int to, int from) {
        if (to == from) return;

        int b = inv[from];
        if (b == -1) return;

        int a = inv[to];

        inv[from] = -1;

        if (a == -1) {
            inv[to] = b;
            label[b] = to;
            return;
        }
        uf.union(a, b);
        int r = uf.root(a);

        inv[to] = r;
        label[r] = to;
    }

    /**
     * ラベル {@code x} と {@code y} を交換する。
     *
     * <p>操作前にラベル {@code x} を持っていた全要素は
     * 操作後に {@code y} を持ち、
     * ラベル {@code y} を持っていた全要素は
     * 操作後に {@code x} を持つ。
     *
     * <p>連結成分自体は変更しない。
     * 片方または両方のラベルが空であってもよい。
     *
     * @param x 一方のラベル
     * @param y もう一方のラベル
     *
     * @complexity O(1)
     */
    public void swapLabels(int x, int y) {
        if (x == y) return;

        int a = inv[x];
        int b = inv[y];

        inv[x] = b;
        inv[y] = a;

        if (a != -1) label[a] = y;
        if (b != -1) label[b] = x;
    }

    /**
     * 指定したラベルが空かを返す。
     *
     * @param x ラベル
     * @return ラベル {@code x} を持つ連結成分が存在しなければ {@code true}
     *
     * @complexity O(1)
     */
    public boolean isEmpty(int x) {
        return inv[x] == -1;
    }
}