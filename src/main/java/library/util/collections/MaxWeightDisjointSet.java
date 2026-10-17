package library.util.collections;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * 重み付き整数集合 (X, w) を追加し、|Y| <= q に対して X ∩ Y = ∅ を満たす最大重みの集合を返す。
 * 同重みでは追加順が早い集合を優先する。空集合、負の要素、負の重みも許す。
 *
 * <p>p を追加集合の要素数の上限、S = Σ_(i=0)^(q) p^i とする（p^0 = 1）。
 * 保持する異なる代表集合は高々 C(p + q, q) 個。C(n, k) = n! / (k!(n - k)!) は二項係数。
 * S は同じ代表集合の重複出現を含む木の節点数の上限で、空間計算量は O((p + 1)S)。
 * 追加済み集合の個数に依存しないが、p, q が大きい場合には適さない。</p>
 *
 * <p>代表木の各節点は、その根からの経路上の要素を含まない集合の最大解を持つ。
 * 節点の集合 X に対し、各 x ∈ X の子はさらに x を除外した問題を表す。
 * 深さ q まで保持すれば、|Y| <= q のすべてのクエリを解ける。
 * 代表集合のみを残した後に新しい集合を追加しても、すべての Y の最大解は保存される。</p>
 */
public final class MaxWeightDisjointSet {
    /** クエリ集合の要素数の上限。 */
    private final int q;
    /** 追加集合の要素数の許容上限。 */
    private final int maxSetSize;
    /** 追加した集合の総数。代表集合から外れた集合も数える。 */
    private long size;
    /** 最大解を保存する代表集合の個数。優先順位は重み降順、追加順昇順。 */
    private int representatives;
    /** 代表木の根。 */
    private Node root;

    /**
     * 重み付き集合 X を追加し、クエリ集合 Y と交差しない重み最大の X を検索する空の構造を作る。
     * 各クエリ集合 Y の要素数は q 以下、各追加集合 X の要素数は p 以下とする。
     * 要素数は重複を除いて数える。すなわち |Y| <= q、|X| <= p。
     * 計算量: O(1)。
     *
     * @param p add で追加する各集合 X の要素数の上限（非負整数）
     * @param q query に渡す集合 Y の要素数の上限（正整数）
     * @throws IllegalArgumentException q <= 0 または p < 0 の場合
     */
    // 未テスト
    public MaxWeightDisjointSet(int p, int q) {
        if (q <= 0 || p < 0)
            throw new IllegalArgumentException("q must be positive and p must be non-negative");
        this.q = q;
        this.maxSetSize = p;
    }

    /**
     * 集合 X = {elements[i]} と重み weight を追加し、その不変なエントリを返す。
     * 同じ集合も別の追加として扱う。入力配列は複製し、重複要素は一つにまとめる。
     * 代表集合に残らない場合もエントリは返す。
     *
     * <p>m = elements.length、p = 追加後の最大集合サイズ、S = Σ_(i=0)^(q) p^i として、
     * 計算量: O(m log(m + 1) + (p + 1)S log(p + 2))。
     * 一時空間: O(m + (p + 1)S)。</p>
     *
     * @param elements X の要素列
     * @param weight 重み
     * @return 追加した集合、重み、0 始まりの追加番号を持つエントリ
     * @throws IllegalArgumentException |X| がコンストラクタの p を超える場合
     * @throws IllegalStateException 追加済み個数が Long.MAX_VALUE の場合
     */
    // 未テスト
    public Entry add(int[] elements, long weight) {
        int[] set = normalize(elements);
        if (set.length > maxSetSize)
            throw new IllegalArgumentException("set size exceeds p");
        if (size == Long.MAX_VALUE)
            throw new IllegalStateException("too many entries");
        Entry added = new Entry(size, set, weight);
        Node nextRoot = insert(root, added, q);
        if (nextRoot != root) {
            root = nextRoot;
            representatives = countRepresentatives(root, new Object());
        }
        size++;
        return added;
    }

    /**
     * Y = {elements[i]} に対し、X ∩ Y = ∅ を満たす追加済み集合のうち重み最大のものを返す。
     * 同重みでは追加番号最小のものを返し、該当集合がなければ null を返す。
     * 入力配列の重複を除いた要素数は q 以下とする。入力配列は変更しない。
     * Y が空集合なら、追加済みのすべての集合のうち重み最大のものを返す。
     *
     * <p>m = elements.length、k = |Y|、p = 追加済み集合の最大サイズ（未追加なら 0）として、
     * 計算量: O(1 + m log(m + 1) + k^2 log(p + 2))。一時空間: O(m + 1)。</p>
     *
     * @param elements Y の要素列
     * @return 最大解、または null
     * @throws IllegalArgumentException |Y| > q の場合
     */
    // 未テスト
    public Entry query(int[] elements) {
        int[] forbidden = normalize(elements);
        if (forbidden.length > q)
            throw new IllegalArgumentException("query set size exceeds q");
        Node node = root;
        while (node != null) {
            int intersection = -1;
            for (int y : forbidden) {
                int index = Arrays.binarySearch(node.best.elements, y);
                if (index >= 0) {
                    intersection = index;
                    break;
                }
            }
            if (intersection < 0)
                return node.best;
            node = node.children == null ? null : node.children[intersection];
        }
        return null;
    }

    /**
     * 呼び出し時点の代表集合 U を、同じエントリを重複させずに列挙できるリストとして返す。
     * すべての |Y| <= q に対し、query(Y) != null なら query(Y) ∈ U が成り立つ。
     * したがって、U の中で Y と交差しない最大重みのエントリを選べば全追加集合に対する最大解になる。
     * 同重みでは追加番号最小を優先する。|U| = representativeCount() <= C(p + q, q)。
     * p, q はそれぞれ追加集合・クエリ集合の要素数上限、C(n, k) = n! / (k!(n - k)!) は二項係数。
     * この個数上限は達成可能である。
     * U の要素数が最小であること、列挙順序は保証しない。
     *
     * <p>戻り値は変更不能なスナップショットで、その後の add や clear によって内容は変化しない。
     * 各 Entry は add の戻り値と参照を共有する。未追加なら空リストを返す。</p>
     * <p>S = Σ_(i=0)^(q) p^i は、同じ代表集合の重複出現を含む木の節点数の上限。
     * 計算量: 期待 O((p + 1)S)、一時空間: O(S + q)。返却リストの全列挙は O(|U|)。</p>
     *
     * @return すべての許容クエリの最大解を含む代表集合 U
     */
    // 未テスト
    public List<Entry> query() {
        List<Entry> result = new ArrayList<>(representatives);
        collectRepresentatives(root, new IdentityHashMap<>(), result);
        return List.copyOf(result);
    }

    /**
     * node に現れる各エントリのうち seen にないものを seen と result に追加する。
     * node と各エントリの値は変更しない。
     * 計算量: 期待 O((p + 1)T)。T は共有部分木を出現ごとに数えた節点数、p は最大集合サイズ。
     * @param node 列挙対象の代表木
     * @param seen 列挙済みエントリの参照をキーとする集合
     * @param result 重複を除いたエントリの追加先
     */
    // 未テスト
    private static void collectRepresentatives(Node node, IdentityHashMap<Entry, Boolean> seen,
            List<Entry> result) {
        if (node == null)
            return;
        if (seen.put(node.best, Boolean.TRUE) == null)
            result.add(node.best);
        if (node.children != null) {
            for (Node child : node.children)
                collectRepresentatives(child, seen, result);
        }
    }

    /**
     * 追加済み集合の総数を返す。計算量: O(1)。
     * @return 現在の追加済み集合の総数（clear により 0 に戻る）
     */
    // 未テスト
    public long size() {
        return size;
    }

    /**
     * すべての追加済み集合を削除し、size() = representativeCount() = 0 とする。
     * p, q は維持し、次の add が返す追加番号は 0 に戻る。
     * 取得済みの Entry、query() のスナップショット、copy() の戻り値は変更しない。
     * 計算量: O(1)。
     */
    // 未テスト
    public void clear() {
        root = null;
        size = 0;
        representatives = 0;
    }

    /**
     * p, q、追加済み集合、追加番号、size()、representativeCount() が等しいコピーを返す。
     * コピー時点の各クエリ結果は元と同じ Entry を参照する。
     * 以後の add と clear は互いに影響せず、追加番号も各構造で独立に進む。
     * コピー元と不変な集合データ・代表木を共有する。
     * 計算量・追加空間: O(1)。
     * @return 独立に更新可能なコピー
     */
    // 未テスト
    public MaxWeightDisjointSet copy() {
        MaxWeightDisjointSet result = new MaxWeightDisjointSet(maxSetSize, q);
        result.root = root;
        result.size = size;
        result.representatives = representatives;
        return result;
    }

    /**
     * 保持する異なる代表集合の個数を返す。query().size() と等しい。
     * p, q をそれぞれ追加集合・クエリ集合の要素数上限とすると、
     * 0 <= representativeCount() <= C(p + q, q) が成り立ち、この上限は達成可能である。
     * C(n, k) = n! / (k!(n - k)!) は二項係数。計算量: O(1)。
     * @return 0 以上 C(p + q, q) 以下の整数
     */
    // 未テスト
    public int representativeCount() {
        return representatives;
    }

    /**
     * p, q、size()、representativeCount() と、各代表集合の追加番号・重み・要素列を標準出力に出力する。
     * 代表集合は重複なく各 1 行に出力し、要素列は昇順とする。代表集合の出力順序は保証しない。
     * p, q はそれぞれ追加集合・クエリ集合の要素数上限、S = Σ_(i=0)^(q) p^i として、
     * 計算量: 期待 O((p + 1)S)、一時空間: O(S + q + p)。
     */
    // 未テスト
    public void dump() {
        System.out.println("MaxWeightDisjointSet");
        System.out.println("  p = " + maxSetSize);
        System.out.println("  q = " + q);
        System.out.println("  size = " + size);
        System.out.println("  representativeCount = " + representatives);
        for (Entry entry : query()) {
            System.out.println("  id = " + entry.id + ", weight = " + entry.weight
                    + ", elements = " + Arrays.toString(entry.elements));
        }
    }

    /**
     * a と同じ要素集合を表す、狭義単調増加の独立した配列を返す。
     * 計算量: O(m log(m + 1))、空間: O(m)、m = a.length。
     * @param a 要素列
     * @return 重複のない昇順の配列
     */
    // 未テスト
    private static int[] normalize(int[] a) {
        int[] sorted = a.clone();
        Arrays.sort(sorted);
        int n = 0;
        for (int x : sorted) {
            if (n == 0 || sorted[n - 1] != x)
                sorted[n++] = x;
        }
        return n == sorted.length ? sorted : Arrays.copyOf(sorted, n);
    }

    /**
     * node の経路条件を満たす candidate を、その部分木の代表族に加える。
     * 事前条件: candidate の追加番号は部分木中のすべての既存エントリより大きい。
     * candidate が新しい節点の最大解にならない場合は node をそのまま返す。
     * node とその子孫は変更しない。戻り値は変更のない部分木を node と共有する。
     * 計算量: O((p + 1)T log(p + 2))。T = Σ_(i=0)^(remaining) p^i、p は最大集合サイズ。
     *
     * @param node 更新対象
     * @param candidate 追加候補
     * @param remaining 残りの除外可能要素数
     * @return 追加後の代表木
     */
    // 未テスト
    private static Node insert(Node node, Entry candidate, int remaining) {
        if (node == null || candidate.weight > node.best.weight) {
            Node[] children = remaining == 0 ? null : new Node[candidate.elements.length];
            if (node != null && children != null) {
                for (int i = 0; i < children.length; i++)
                    children[i] = exclude(node, candidate.elements[i], remaining - 1);
            }
            return new Node(candidate, children);
        }
        if (remaining == 0)
            return node;
        Node[] children = node.children;
        for (int i = 0; i < node.best.elements.length; i++) {
            if (Arrays.binarySearch(candidate.elements, node.best.elements[i]) >= 0)
                continue;
            Node child = insert(node.children[i], candidate, remaining - 1);
            if (child != node.children[i]) {
                if (children == node.children)
                    children = children.clone();
                children[i] = child;
            }
        }
        return children == node.children ? node : new Node(node.best, children);
    }

    /**
     * node の代表族から x を含む集合を除いた、深さ remaining の代表木を返す。
     * 事前条件: node は null、または深さ remaining + 1 の代表木。
     * node とその子孫は変更しない。戻り値は node の部分木を共有する場合がある。
     * 計算量: O((p + 1)T log(p + 2))。T = Σ_(i=0)^(remaining) p^i、p は最大集合サイズ。
     *
     * @param node 除外前の代表木
     * @param x 除外する要素
     * @param remaining 追加で除外可能な要素数
     * @return x を含まない集合の代表木、存在しなければ null
     */
    // 未テスト
    private static Node exclude(Node node, int x, int remaining) {
        if (node == null)
            return null;
        int index = Arrays.binarySearch(node.best.elements, x);
        if (index >= 0)
            return node.children[index];
        if (remaining == 0)
            return new Node(node.best, null);
        Node[] children = new Node[node.best.elements.length];
        for (int i = 0; i < children.length; i++)
            children[i] = exclude(node.children[i], x, remaining - 1);
        return new Node(node.best, children);
    }

    /**
     * node に現れるエントリのうち集計世代が version と異なるものを数え、その世代を version にする。
     * 事前条件: version は null でなく、コピーを含む他の集計で使用した世代と参照が異なる。
     * 計算量: O((p + 1)T)。T は共有された部分木を出現ごとに数えた節点数、p は最大集合サイズ。
     *
     * @param node 代表木
     * @param version 集計世代
     * @return 今回の呼び出しで初めて集計する異なるエントリの個数
     */
    // 未テスト
    private static int countRepresentatives(Node node, Object version) {
        if (node == null)
            return 0;
        int count = node.best.countedVersion == version ? 0 : 1;
        node.best.countedVersion = version;
        if (node.children != null) {
            for (Node child : node.children)
                count += countRepresentatives(child, version);
        }
        return count;
    }

    /** 除外条件に対する最大解と、その追加除外条件を保持する節点。 */
    private static final class Node {
        /** 経路の要素をすべて避ける最優先エントリ。 */
        private final Entry best;
        /** best.elements[i] を追加で除外した部分木。深さ q では null。 */
        private final Node[] children;

        /**
         * 経路条件に対する最大解と追加除外条件の部分木を持つ節点を作る。
         * children の配列の所有権を受け取る。各部分木は参照を共有する。
         * 計算量: O(1)。
         * @param best 最大解
         * @param children best の各要素を除外した部分木、残り深さが 0 なら null
         */
        // 未テスト
        private Node(Entry best, Node[] children) {
            this.best = best;
            this.children = children;
        }
    }

    /** 追加した整数集合、重み、追加番号。不変であり、クエリ結果と参照を共有する。 */
    public static final class Entry {
        /** 0 始まりの追加番号。 */
        private final long id;
        /** 狭義単調増加の集合要素。 */
        private final int[] elements;
        /** 集合の重み。 */
        private final long weight;
        /** 代表数の集計世代。コピー間でも参照で区別し、集合・重み・追加番号には影響しない内部管理値。 */
        private Object countedVersion;

        /**
         * 指定した値のエントリを作る。elements の所有権を受け取る。
         * 計算量: O(1)。
         * @param id 追加番号
         * @param elements 狭義単調増加の要素列
         * @param weight 重み
         */
        // 未テスト
        private Entry(long id, int[] elements, long weight) {
            this.id = id;
            this.elements = elements;
            this.weight = weight;
        }

        /**
         * 0 始まりの追加番号を返す。計算量: O(1)。
         * @return 追加番号
         */
        // 未テスト
        public long id() {
            return id;
        }

        /**
         * 集合の重みを返す。計算量: O(1)。
         * @return 重み
         */
        // 未テスト
        public long weight() {
            return weight;
        }

        /**
         * 集合 X の要素を昇順に並べた独立した配列を返す。計算量: O(|X| + 1)。
         * @return 集合の要素列
         */
        // 未テスト
        public int[] elements() {
            return elements.clone();
        }
    }
}
