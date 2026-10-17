package library.util.seq;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/**
 * U = {0, ..., n-1} の順列のうち、追加した各集合が連続区間となる順列を表す PQ Tree。
 * P の子は任意に並べ替えられ、Q の子は順序を保つか全体を反転できる。
 * 各子の内部での選択は独立。保持領域 O(n)。
 */
public final class PQTree {
    /** ノードの種類。 */
    public enum NodeType { LEAF, P, Q }

    /** 変更不能な部分木。過去に取得したノードは reduce 後も変更されない。 */
    public static final class Node {
        /** ノードの種類。 */
        public final NodeType type;
        /** 葉の要素番号。内部ノードでは -1。 */
        public final int value;
        /** 順序付きの子。変更不能。 */
        public final List<Node> children;
        /** 部分木の葉数。 */
        public final int size;

        /**
         * 指定した種類、値、子を持つノードを作る。子の配列は共有しない。
         * 計算量: 時間・追加領域 O(1 + children.size())。
         */
        // 未テスト
        private Node(NodeType type, int value, List<Node> children) {
            this.type = type;
            this.value = value;
            this.children = List.copyOf(children);
            int total = type == NodeType.LEAF ? 1 : 0;
            for (Node child : children) total += child.size;
            size = total;
        }
    }

    /** 要素数。 */
    private final int n;
    /** 現在の根。n = 0 なら null。 */
    private Node root;

    /**
     * U = {0, ..., n-1} の全順列を表す木を作る。
     * 計算量: 時間・領域 O(n)。
     * @param n 要素数
     * @throws IllegalArgumentException n &lt; 0
     */
    // 未テスト
    public PQTree(int n) {
        if (n < 0) throw new IllegalArgumentException("negative size");
        this.n = n;
        List<Node> leaves = new ArrayList<>(n);
        for (int i = 0; i < n; i++) leaves.add(new Node(NodeType.LEAF, i, List.of()));
        root = group(NodeType.P, leaves);
    }

    /**
     * |U| を返す。計算量: 時間・追加領域 O(1)。
     * @return 要素数
     */
    // 未テスト
    public int size() { return n; }

    /**
     * 現在の木の変更不能な根を返す。計算量: 時間・追加領域 O(1)。
     * @return 根。n = 0 なら null
     */
    // 未テスト
    public Node getRoot() { return root; }

    /**
     * 現在の木が表す相異なる順列の個数 C を返す。
     * P(v) を P ノード v の子数、q を Q ノード数とすると
     * C = 2^q * product(v is P, P(v)!)。空の順列も一つと数える。
     * 反転した順列は別の順列として数える。木は変更しない。
     * B = max(1, C のビット長) として、計算量: 時間 O(n B) ビット演算、
     * 追加領域 O(n) 個の参照と O(B) ビット。
     * @return 順列数の厳密値
     */
    // 未テスト
    public BigInteger countPermutations() {
        BigInteger count = BigInteger.ONE;
        for (Node node : traversal(root)) {
            int limit = node.type == NodeType.P ? node.children.size() : node.type == NodeType.Q ? 2 : 1;
            for (int factor = 2; factor <= limit; factor++) {
                count = count.multiply(BigInteger.valueOf(factor));
            }
        }
        return count;
    }

    /**
     * 現在の木が表す相異なる順列の個数 C に対し、C mod mod を返す。
     * P(v) を P ノード v の子数、q を Q ノード数とすると
     * C = 2^q * product(v is P, P(v)!)。空の順列も一つと数える。
     * 反転した順列は別の順列として数える。木は変更しない。
     * 計算量: 時間・追加領域 O(n)（固定長整数の演算を O(1) とする）。
     * @param mod 正の法。素数でなくてもよい
     * @return 0 &lt;= C mod mod &lt; mod
     * @throws IllegalArgumentException mod &lt;= 0
     */
    // 未テスト
    public long countPermutations(long mod) {
        if (mod <= 0) throw new IllegalArgumentException("nonpositive modulus");
        long count = 1 % mod;
        BigInteger modulus = BigInteger.valueOf(mod);
        for (Node node : traversal(root)) {
            int limit = node.type == NodeType.P ? node.children.size() : node.type == NodeType.Q ? 2 : 1;
            for (int factor = 2; factor <= limit; factor++) {
                if (count <= Long.MAX_VALUE / factor) count = count * factor % mod;
                else count = BigInteger.valueOf(count).multiply(BigInteger.valueOf(factor)).mod(modulus).longValue();
            }
        }
        return count;
    }

    /**
     * F を現在の順列集合、S を elements の相異なる値の集合とする。
     * F' = {p in F : S の要素が p の連続区間を占める} が空でなければ
     * 順列集合を F' に更新し true を返す。空なら状態を変更せず false を返す。
     * |S| &lt;= 1 の制約は常に成立する。引数は変更・保持しない。
     * 計算量: 時間 O(n + elements.length)、追加領域 O(n)。
     * @param elements 連続させる要素番号。重複可
     * @return 制約を追加できたか
     * @throws IllegalArgumentException U に属さない要素がある
     * @throws NullPointerException elements が null
     */
    // 未テスト
    public boolean reduce(int... elements) {
        Objects.requireNonNull(elements);
        boolean[] selected = new boolean[n];
        int total = 0;
        for (int value : elements) {
            if (value < 0 || value >= n) throw new IllegalArgumentException("element outside U: " + value);
            if (!selected[value]) { selected[value] = true; total++; }
        }
        if (total <= 1 || total == n) return true;
        List<Node> order = traversal(root);
        IdentityHashMap<Node, Reduction> states = new IdentityHashMap<>();
        for (int i = order.size() - 1; i >= 0; i--) {
            Node node = order.get(i);
            int count = 0;
            if (node.type == NodeType.LEAF) count = selected[node.value] ? 1 : 0;
            else for (Node child : node.children) count += states.get(child).count;
            Reduction state = new Reduction(count, node);
            if (count > 0 && count < node.size) {
                boolean pertinentRoot = count == total;
                int touched = 0;
                for (Node child : node.children) if (states.get(child).count > 0) touched++;
                if (pertinentRoot && touched == 1) {
                    List<Node> children = new ArrayList<>();
                    for (Node child : node.children) children.add(states.get(child).tree);
                    state.tree = group(node.type, children);
                } else {
                    Boundary boundary = node.type == NodeType.P
                            ? reduceP(node, states, pertinentRoot)
                            : reduceQ(node, states, pertinentRoot);
                    if (boundary == null) return false;
                    if (pertinentRoot) state.tree = group(NodeType.Q, boundary.toList());
                    else state.boundary = boundary;
                }
            }
            states.put(node, state);
        }
        root = states.get(root).tree;
        return true;
    }

    /**
     * 現在の木が表す順列を一つ返す。返す配列は木と共有しない。
     * 計算量: 時間・追加領域 O(n)。
     * @return 各ノードの children の順に葉を列挙した順列
     */
    // 未テスト
    public int[] frontier() {
        int[] result = new int[n];
        int index = 0;
        for (Node node : traversal(root)) if (node.type == NodeType.LEAF) result[index++] = node.value;
        return result;
    }

    /**
     * permutation が現在の木で表される U の順列なら true を返す。
     * 引数を変更・保持しない。計算量: 時間・追加領域 O(n)。
     * @param permutation 判定する配列
     * @return 順列集合に属するか。長さの相違、重複、範囲外の値には false
     * @throws NullPointerException permutation が null
     */
    // 未テスト
    public boolean contains(int[] permutation) {
        Objects.requireNonNull(permutation);
        if (permutation.length != n) return false;
        int[] position = new int[n];
        Arrays.fill(position, -1);
        for (int i = 0; i < n; i++) {
            int value = permutation[i];
            if (value < 0 || value >= n || position[value] >= 0) return false;
            position[value] = i;
        }
        IdentityHashMap<Node, Integer> left = new IdentityHashMap<>();
        List<Node> order = traversal(root);
        for (int i = order.size() - 1; i >= 0; i--) {
            Node node = order.get(i);
            if (node.type == NodeType.LEAF) { left.put(node, position[node.value]); continue; }
            int min = n, max = -1, previous = -1;
            boolean increasing = true, decreasing = true;
            for (Node child : node.children) {
                int start = left.get(child);
                min = Math.min(min, start);
                max = Math.max(max, start + child.size - 1);
                if (previous >= 0) {
                    increasing &= previous < start;
                    decreasing &= previous > start;
                }
                previous = start;
            }
            if (max - min + 1 != node.size) return false;
            if (node.type == NodeType.Q && !increasing && !decreasing) return false;
            left.put(node, min);
        }
        return true;
    }

    /**
     * children が空なら null、一つならその子、それ以外なら指定種類の根を返す。
     * 計算量: 時間・追加領域 O(1 + children.size())。
     */
    // 未テスト
    private static Node group(NodeType type, List<Node> children) {
        if (children.isEmpty()) return null;
        if (children.size() == 1) return children.get(0);
        return new Node(type, -1, children);
    }

    /**
     * root 以下のノードを親が子より先になる順で返す。null には空リストを返す。
     * 計算量: 時間・追加領域 O(部分木のノード数)。
     */
    // 未テスト
    private static List<Node> traversal(Node root) {
        List<Node> result = new ArrayList<>();
        ArrayDeque<Node> stack = new ArrayDeque<>();
        if (root != null) stack.push(root);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            result.add(node);
            for (int i = node.children.size() - 1; i >= 0; i--) stack.push(node.children.get(i));
        }
        return result;
    }

    /**
     * P の制約を満たす列を返す。不可能なら null。
     * root = false では非選択部分、選択部分の順に並ぶ境界列を返す。
     * root = true では列が一つの木を含んでもよい。
     * states 内の境界列の所有権を消費する。
     * 計算量: root = false なら O(子数)、true なら O(部分木のノード数) の時間・追加領域。
     */
    // 未テスト
    private static Boundary reduceP(Node node, IdentityHashMap<Node, Reduction> states, boolean root) {
        List<Node> empty = new ArrayList<>(), full = new ArrayList<>();
        List<Boundary> partial = new ArrayList<>();
        for (Node child : node.children) {
            Reduction state = states.get(child);
            if (state.count == 0) empty.add(child);
            else if (state.count == child.size) full.add(child);
            else partial.add(state.boundary);
        }
        if (partial.size() > (root ? 2 : 1)) return null;
        Boundary result = new Boundary();
        if (!root) result.add(group(NodeType.P, empty));
        if (!partial.isEmpty()) result.join(partial.get(0));
        result.add(group(NodeType.P, full));
        if (partial.size() == 2) {
            List<Node> right = partial.get(1).toList();
            Collections.reverse(right);
            for (Node child : right) result.add(child);
        }
        if (root) {
            empty.add(group(NodeType.Q, result.toList()));
            result = new Boundary();
            result.add(group(NodeType.P, empty));
        }
        return result;
    }

    /**
     * Q の制約を満たす列を返す。不可能なら null。
     * root = false では非選択部分、選択部分の順に並ぶ境界列を返す。
     * states 内の境界列の所有権を消費する。
     * 計算量: root = false なら O(子数)、true なら O(部分木のノード数) の時間・追加領域。
     */
    // 未テスト
    private static Boundary reduceQ(Node node, IdentityHashMap<Node, Reduction> states, boolean root) {
        int k = node.children.size();
        for (int direction = 0; direction < 2; direction++) {
            int first = k, last = -1;
            for (int i = 0; i < k; i++) {
                Node child = node.children.get(direction == 0 ? i : k - 1 - i);
                if (states.get(child).count > 0) { first = Math.min(first, i); last = i; }
            }
            if (!root && last != k - 1) continue;
            boolean valid = true;
            for (int i = first; i <= last; i++) {
                Node child = node.children.get(direction == 0 ? i : k - 1 - i);
                int count = states.get(child).count;
                if (count == 0 || (count < child.size && i != first && !(root && i == last))) valid = false;
            }
            if (!valid) continue;
            Boundary result = new Boundary();
            for (int i = 0; i < k; i++) {
                Node child = node.children.get(direction == 0 ? i : k - 1 - i);
                Reduction state = states.get(child);
                if (state.count == 0 || state.count == child.size) result.add(child);
                else if (i == first) result.join(state.boundary);
                else {
                    List<Node> right = state.boundary.toList();
                    Collections.reverse(right);
                    for (Node part : right) result.add(part);
                }
            }
            return result;
        }
        return null;
    }

    /** 一回の reduction における部分木の結果。 */
    private static final class Reduction {
        /** 選択された葉数。 */
        final int count;
        /** 完全な部分木。境界列がある場合は参照しない。 */
        Node tree;
        /** 非選択部分から選択部分へ向かう列。 */
        Boundary boundary;

        /** 葉数と部分木を記録する。計算量: 時間・追加領域 O(1)。 */
        // 未テスト
        Reduction(int count, Node tree) { this.count = count; this.tree = tree; }
    }

    /** 境界列の連結要素。 */
    private static final class Cell {
        /** 列の要素。 */
        final Node node;
        /** 次の要素。 */
        Cell next;

        /** node を持つ要素を作る。計算量: 時間・追加領域 O(1)。 */
        // 未テスト
        Cell(Node node) { this.node = node; }
    }

    /** 部分ノードの向きの依存関係を保つ境界列。 */
    private static final class Boundary {
        /** 先頭要素。 */
        Cell head;
        /** 末尾要素。 */
        Cell tail;

        /** node が非 null なら末尾へ加える。計算量: 時間・追加領域 O(1)。 */
        // 未テスト
        void add(Node node) {
            if (node == null) return;
            Cell cell = new Cell(node);
            if (tail == null) head = cell;
            else tail.next = cell;
            tail = cell;
        }

        /** other を末尾へ移し other を空にする。計算量: 時間・追加領域 O(1)。 */
        // 未テスト
        void join(Boundary other) {
            if (other.head == null) return;
            if (tail == null) head = other.head;
            else tail.next = other.head;
            tail = other.tail;
            other.head = other.tail = null;
        }

        /** 列のコピーを返す。計算量: 時間・追加領域 O(列長)。 */
        // 未テスト
        List<Node> toList() {
            List<Node> result = new ArrayList<>();
            for (Cell cell = head; cell != null; cell = cell.next) result.add(cell.node);
            return result;
        }
    }
}
