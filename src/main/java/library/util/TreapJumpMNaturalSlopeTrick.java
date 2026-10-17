package library.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 有限1次元 jump M-natural-convex 関数の、min-plus convolution に特化した軽量版。
 * 任意の3点カーネル、任意座標での評価、一次式加算、非負倍、平行移動に対応する。
 * restrict とヒンジ加算は扱わない。その場合は4本のAVL Ropeを使う完全版を使う。
 *
 * 長さ1/2の因子を平均傾き順の1本のTreapで保持する。
 * relax3 は高々2ノードの挿入。relax3/valueAt/minValue/argmin は期待 O(log N)、
 * addConstant/shift/addLinear/scale は O(1)、of は O(N)。N は定義域の点数。
 * 演算途中の値を含め、long の範囲内で扱うこと。
 */
public final class TreapJumpMNaturalSlopeTrick {
    private Node root;
    private long left, f0;
    private long random = 0x123456789abcdefL;

    private TreapJumpMNaturalSlopeTrick(long left, long f0) {
        this.left = left;
        this.f0 = f0;
    }

    public static TreapJumpMNaturalSlopeTrick singleton(long x, long value) {
        return new TreapJumpMNaturalSlopeTrick(x, value);
    }

    public static TreapJumpMNaturalSlopeTrick of(long left, long[] values) {
        return of(left, values, true);
    }

    /** validate=false の場合も入力が jump M-natural-convex であることを前提とする。 */
    public static TreapJumpMNaturalSlopeTrick of(long left, long[] values, boolean validate) {
        if (values.length == 0) throw new IllegalArgumentException("values must be nonempty");
        long[] d = new long[values.length - 1];
        for (int i = 0; i < d.length; i++) d[i] = values[i + 1] - values[i];
        if (validate) for (int i = 2; i < d.length; i++)
            if (d[i - 2] > d[i]) throw new IllegalArgumentException("not jump M-natural-convex");
        TreapJumpMNaturalSlopeTrick f = singleton(left, values[0]);
        Node[] stack = new Node[d.length];
        int top = 0;
        for (int i = 0; i < d.length;) {
            Node t;
            if (i + 1 < d.length && d[i] > d[i + 1]) {
                t = new Node(2, d[i] + d[i + 1], d[i], f.nextRandom());
                i += 2;
            } else {
                t = new Node(1, d[i], d[i], f.nextRandom());
                i++;
            }
            Node last = null;
            while (top != 0 && stack[top - 1].priority < t.priority) last = stack[--top];
            t.l = last;
            if (top != 0) stack[top - 1].r = t;
            stack[top++] = t;
        }
        f.root = top == 0 ? null : stack[0];
        rebuild(f.root);
        return f;
    }

    public long left() { return left; }
    public long right() { return left + length(root); }
    public int size() { return Math.addExact(length(root), 1); }

    /** g(x)=min(f(x-shift)+c0, f(x-shift-1)+c1, f(x-shift-2)+c2)。期待 O(log N)。 */
    public void relax3(long shift, long c0, long c1, long c2) {
        long u = c1 - c0, v = c2 - c1;
        if (u <= v) {
            root = insert(root, new Node(1, u, u, nextRandom()));
            root = insert(root, new Node(1, v, v, nextRandom()));
        } else {
            root = insert(root, new Node(2, u + v, u, nextRandom()));
        }
        f0 += c0;
        left += shift;
    }

    public void relax(long leftCost, long stayCost, long rightCost) {
        relax3(-1, leftCost, stayCost, rightCost);
    }

    /** 指定した整数座標の関数値。期待 O(log N)。 */
    public long valueAt(long x) {
        if (x < left || x > right()) throw new IllegalArgumentException("x outside domain");
        int k = Math.toIntExact(x - left);
        Node t = root;
        long total = f0, prefix = Long.MAX_VALUE, suffix = Long.MAX_VALUE;
        while (t != null) {
            if (k == 0) return total;
            if (k == t.totalLength) return total + t.sum;
            push(t);
            int n = length(t.l);
            if (k < n) {
                suffix = Math.min(suffix, t.u);
                if (t.r != null) suffix = Math.min(suffix, t.r.minQ);
                t = t.l;
            } else {
                total += sum(t.l);
                if (k == n) return total;
                if (k == n + t.length) return total + t.cost;
                prefix = Math.min(prefix, p(t));
                if (t.l != null) prefix = Math.min(prefix, t.l.minP);
                if (k < n + t.length) {
                    // 長さ2の因子の途中。prefix/suffix とも対象因子を含める。
                    suffix = Math.min(suffix, t.u);
                    if (t.r != null) suffix = Math.min(suffix, t.r.minQ);
                    return total + Math.min(suffix, t.cost + prefix);
                }
                total += t.cost;
                k -= n + t.length;
                t = t.r;
            }
        }
        return total;
    }

    public void addConstant(long c) { f0 += c; }
    public void shift(long dx) { left += dx; }

    public void addLinear(long a, long b) {
        f0 += a * left + b;
        apply(root, 1, a);
    }

    public void scale(long c) {
        if (c < 0) throw new IllegalArgumentException("c must be nonnegative");
        f0 *= c;
        apply(root, c, 0);
    }

    public long minValue() {
        return minValueWithLinear(0, 0);
    }

    /**
     * {@code min_x (f(x) + a * x + b)} を返す。
     * この関数自体は変更しない。期待 O(log N)。
     *
     * @param a 加える一次式の傾き
     * @param b 加える一次式の切片
     * @return {@code min_x (f(x) + a * x + b)}
     */
    public long minValueWithLinear(long a, long b) {
        Node t = root;
        long value = f0 + a * left + b;
        while (t != null) {
            push(t);
            long cost = t.cost + a * t.length;
            if (cost < 0) {
                value += sum(t.l) + a * length(t.l) + cost;
                t = t.r;
            }
            else t = t.l;
        }
        return value;
    }

    /** 最小値をとる最小の座標。期待 O(log N)。 */
    public long argmin() {
        return argminWithLinear(0);
    }

    /**
     * {@code f(x) + a * x} を最小にする最小の x を返す。
     * この関数自体は変更しない。期待 O(log N)。
     *
     * @param a 加える一次式の傾き
     * @return {@code f(x) + a * x} を最小にする最小の x
     */
    public long argminWithLinear(long a) {
        Node t = root;
        long x = left;
        while (t != null) {
            push(t);
            if (t.cost + a * t.length < 0) {
                x += length(t.l) + t.length;
                t = t.r;
            }
            else t = t.l;
        }
        return x;
    }

    /**
     * {@code f(x) + a * x + b} を最小にする最小の x を返す。
     * 切片 b は argmin に影響しない。期待 O(log N)。
     *
     * @param a 加える一次式の傾き
     * @param b 加える一次式の切片（argmin には影響しない）
     * @return {@code f(x) + a * x + b} を最小にする最小の x
     */
    public long argminWithLinear(long a, long b) {
        return argminWithLinear(a);
    }

    public long[] toArray() {
        long[] a = new long[size()];
        for (int i = 0; i < a.length; i++) a[i] = valueAt(left + i);
        return a;
    }

    /** 診断用。O(N log N)。 */
    public boolean checkInternal() {
        if (!check(root)) return false;
        long[] a = toArray();
        for (int i = 0; i + 3 < a.length; i++)
            if (a[i + 1] - a[i] > a[i + 3] - a[i + 2]) return false;
        return true;
    }

    private long nextRandom() {
        long x = random;
        x ^= x << 13; x ^= x >>> 7; x ^= x << 17;
        return random = x;
    }

    private static final class Node {
        Node l, r;
        final long priority;
        final int length;
        long cost, u, sum, minP, minQ, mul = 1, add;
        int totalLength;
        Node(int length, long cost, long u, long priority) {
            this.length = length; this.cost = cost; this.u = u; this.priority = priority;
            pull(this);
        }
    }

    private static int length(Node t) { return t == null ? 0 : t.totalLength; }
    private static long sum(Node t) { return t == null ? 0 : t.sum; }
    private static long p(Node t) { return t.length == 1 ? -t.cost : t.u - t.cost; }

    private static void pull(Node t) {
        t.totalLength = length(t.l) + t.length + length(t.r);
        t.sum = sum(t.l) + t.cost + sum(t.r);
        t.minP = p(t); t.minQ = t.u;
        if (t.l != null) { t.minP = Math.min(t.minP, t.l.minP); t.minQ = Math.min(t.minQ, t.l.minQ); }
        if (t.r != null) { t.minP = Math.min(t.minP, t.r.minP); t.minQ = Math.min(t.minQ, t.r.minQ); }
    }

    /** 各単位長さあたり a を加算し、元の値を m 倍する。 */
    private static void apply(Node t, long m, long a) {
        if (t == null || (m == 1 && a == 0)) return;
        t.cost = m * t.cost + a * t.length;
        t.u = m * t.u + a;
        t.sum = m * t.sum + a * t.totalLength;
        t.minP = m * t.minP - a;
        t.minQ = m * t.minQ + a;
        t.mul = m * t.mul;
        t.add = m * t.add + a;
    }

    private static void push(Node t) {
        if (t.mul == 1 && t.add == 0) return;
        apply(t.l, t.mul, t.add); apply(t.r, t.mul, t.add);
        t.mul = 1; t.add = 0;
    }

    /** cost/length を倍精度や2倍を使わず比較する。 */
    private static int compare(Node a, Node b) {
        long x = a.length == 1 ? a.cost : a.cost >> 1;
        long y = b.length == 1 ? b.cost : b.cost >> 1;
        int c = Long.compare(x, y);
        if (c != 0) return c;
        return Integer.compare(a.length == 1 ? 0 : (int)(a.cost & 1),
                b.length == 1 ? 0 : (int)(b.cost & 1));
    }

    private static Node insert(Node t, Node a) {
        if (t == null) return a;
        push(t);
        if (compare(a, t) < 0) {
            t.l = insert(t.l, a);
            if (t.l.priority > t.priority) {
                Node s = t.l; t.l = s.r; s.r = t;
                pull(t); pull(s); return s;
            }
        } else {
            t.r = insert(t.r, a);
            if (t.r.priority > t.priority) {
                Node s = t.r; t.r = s.l; s.l = t;
                pull(t); pull(s); return s;
            }
        }
        pull(t); return t;
    }

    private static void rebuild(Node t) {
        if (t == null) return;
        rebuild(t.l); rebuild(t.r); pull(t);
    }

    private static boolean check(Node t) {
        if (t == null) return true;
        push(t);
        if (t.length == 2 && t.u < t.cost - t.u) return false;
        if (t.l != null && (t.l.priority > t.priority || compare(t.l, t) > 0)) return false;
        if (t.r != null && (t.r.priority > t.priority || compare(t.r, t) < 0)) return false;
        int n = t.totalLength; long s = t.sum, p = t.minP, q = t.minQ;
        if (!check(t.l) || !check(t.r)) return false;
        pull(t);
        return n == t.totalLength && s == t.sum && p == t.minP && q == t.minQ;
    }
}

