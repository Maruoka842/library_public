package library.util.linalg;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import library.util.MathUtils;

/**
 * {@code long} を主に用いて 2 変数整数線形計画問題を厳密に解くクラス。
 *
 * <p>次の問題を解く。
 * <pre>
 * 最大化: px * x + py * y
 * 制約:   a[i] * x + b[i] * y <= c[i]
 *         x, y は整数
 * </pre>
 *
 * <p>実行可能領域そのものが非有界でも扱える。目的関数が上に有界なら最適解を返し、
 * 上に非有界なら {@link Status#UNBOUNDED} を返す。内部では浮動小数点数を使用しない。
 * 通常は {@code long} で計算し、有理数の中間演算がオーバーフローする場合だけ
 * {@link BigInteger} へ昇格する。値を実際に構成する整数演算には
 * {@link Math#addExact(long, long)}、{@link Math#subtractExact(long, long)}、
 * {@link Math#multiplyExact(long, long)} などを用いる。
 * 一方、積同士の大小比較だけが必要な箇所では、2 個の {@code long} で符号付き
 * 128 ビット積を表現して比較する。返却する座標または目的関数値自体が {@code long} に
 * 収まらない場合は {@link ArithmeticException} を送出する。
 *
 * <p>アルゴリズムは次の通り。
 * <ol>
 *   <li>整数格子を保存するユニモジュラ変換により、目的関数を {@code X} の最大化に変換する。</li>
 *   <li>各不等式を係数の最大公約数で整数点に関して同値な形まで縮約する。</li>
 *   <li>静的 CHT により上下の直線包絡線を {@code O(m log m)} で構築する。</li>
 *   <li>2 本の包絡線をマージし、上下の有効直線が固定された {@code O(m)} 個の帯状領域に分割する。</li>
 *   <li>各帯状領域の最右格子点を Kanamaru-Nishizeki-Asano 型の切り詰め・せん断変換による
 *       ユークリッド互除法型の縮約で求める。二分探索も {@code floor_sum} も用いない。</li>
 * </ol>
 *
 * <p>包絡線から得られる帯状領域の数を {@code S}、座標・係数の大きさを {@code D} とすると、
 * 算術演算回数は {@code O(m log m + S log D)}、したがって最悪
 * {@code O(m log m + m log D)}。制約数 {@code m} が定数なら {@code O(log D)} である。
 */
public final class IntegerLinearProgramming2DLong {
	//https://atcoder.jp/contests/abc374/submissions/79319102
	//https://atcoder.jp/contests/abc422/submissions/78857253
    private long[] constraintA;
    private long[] constraintB;
    private long[] constraintC;
    private int constraintCount;

    /**
     * 指定した初期容量（制約数）で空の 2 変数整数線形計画問題を作る。
     *
     * <p>容量を超えて制約を追加した場合は内部配列を自動的に拡張する。
     *
     * @param capacity 追加する制約数の初期見積もり
     * @throws IllegalArgumentException {@code capacity < 0} の場合
     */
    public IntegerLinearProgramming2DLong(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        constraintA = new long[capacity];
        constraintB = new long[capacity];
        constraintC = new long[capacity];
    }

    /**
     * 制約 {@code a * x + b * y <= c} を追加する。
     *
     * @return このオブジェクト自身
     */
    public IntegerLinearProgramming2DLong leq(long a, long b, long c) {
        ensureConstraintCapacity(constraintCount + 1);
        constraintA[constraintCount] = a;
        constraintB[constraintCount] = b;
        constraintC[constraintCount] = c;
        constraintCount++;
        return this;
    }

    /**
     * 制約 {@code a * x + b * y >= c} を追加する。
     *
     * <p>時間計算量は {@code O(1)}、追加領域計算量は {@code O(1)}。
     *
     * @param a {@code x} の係数
     * @param b {@code y} の係数
     * @param c 右辺
     * @return このオブジェクト自身
     * @throws ArithmeticException {@code a}, {@code b}, {@code c} のいずれかが
     *         {@link Long#MIN_VALUE} の場合
     */
    // 未テスト
    public IntegerLinearProgramming2DLong geq(long a, long b, long c) {
        return leq(Math.negateExact(a), Math.negateExact(b), Math.negateExact(c));
    }

    /**
     * 追加済みの全制約の下で {@code px * x + py * y} を最大化する。
     *
     * @return {@code OPTIMAL}, {@code INFEASIBLE}, {@code UNBOUNDED} のいずれかを表す求解結果
     */
    public Result maximize(long px, long py) {
        return maximize(
                Arrays.copyOf(constraintA, constraintCount),
                Arrays.copyOf(constraintB, constraintCount),
                Arrays.copyOf(constraintC, constraintCount),
                px, py);
    }

    /**
     * 追加済みの全制約の下で {@code px * x + py * y} を最小化する。
     *
     * <p>制約数を {@code m}、包絡線から得られる帯状領域の数を {@code S}、
     * 座標・係数の大きさを {@code D} とすると、時間計算量は
     * {@code O(m log m + S log D)}、追加領域計算量は {@code O(m)}。
     *
     * @param px 目的関数の {@code x} の係数
     * @param py 目的関数の {@code y} の係数
     * @return {@code OPTIMAL}, {@code INFEASIBLE}, {@code UNBOUNDED} のいずれかを表す求解結果
     * @throws ArithmeticException {@code px} または {@code py} が {@link Long#MIN_VALUE} の場合、
     *         または厳密計算で必要な中間値が {@code long} に収まらない場合
     */
    // 未テスト
    public Result minimize(long px, long py) {
        return minimize(
                Arrays.copyOf(constraintA, constraintCount),
                Arrays.copyOf(constraintB, constraintCount),
                Arrays.copyOf(constraintC, constraintCount),
                px, py);
    }

    private void ensureConstraintCapacity(int required) {
        if (required <= constraintA.length) return;
        int newCapacity = Math.max(required, Math.max(1, constraintA.length << 1));
        constraintA = Arrays.copyOf(constraintA, newCapacity);
        constraintB = Arrays.copyOf(constraintB, newCapacity);
        constraintC = Arrays.copyOf(constraintC, newCapacity);
    }

    public enum Status {
        OPTIMAL,
        INFEASIBLE,
        UNBOUNDED
    }

    /**
     * 求解結果。
     *
     * <p>{@link Status#OPTIMAL} のときだけ {@code x}, {@code y}, {@code value} が有効である。
     * {@link Status#INFEASIBLE} と {@link Status#UNBOUNDED} のとき、これら3値は 0。
     */
    public record Result(Status status, long x, long y, long value) {
        public static Result optimal(long x, long y, long value) {
            return new Result(Status.OPTIMAL, x, y, value);
        }

        public static Result infeasible() {
            return new Result(Status.INFEASIBLE, 0, 0, 0);
        }

        public static Result unbounded() {
            return new Result(Status.UNBOUNDED, 0, 0, 0);
        }
    }

    /**
     * この実装が生成する全ての中間値が符号付き {@code long} に収まることを保証するための、
     * 保守的な入力サイズ条件を満たすか判定する。
     *
     * <p>次を定義する。
     * <pre>
     * A = max_i max(|a[i]|, |b[i]|)
     * C = max_i |c[i]|
     * P = max(1, |px|, |py|)
     * K = 2 A P
     * V = 2 C K + 2
     * </pre>
     * このメソッドは、少なくとも次の量が符号付き {@code long} に収まることを要求する。
     * <pre>
     * 2 K^2
     * V
     * 2 K (K V + C)
     * </pre>
     *
     * <p>これは必要条件ではなく十分条件である。そのため {@code false} を返す入力でも、
     * 解法本体は必要な中間演算を {@link BigInteger} へ昇格して解ける場合がある。
     * この判定自体は {@code BigInteger} を使用しない。
     *
     * @return 本実装が {@code long} のみで安全に計算できることを保証できるなら {@code true}
     */
    public static boolean isGuaranteedLongSafe(long[] a, long[] b, long[] c, long px, long py) {
        if (a.length != b.length || a.length != c.length) return false;
        if (a.length == 0) return true;
        try {
            long A = 0, C = 0;
            for (int i = 0; i < a.length; i++) {
                A = Math.max(A, Math.absExact(a[i]));
                A = Math.max(A, Math.absExact(b[i]));
                C = Math.max(C, Math.absExact(c[i]));
            }
            long P = Math.max(1, Math.max(Math.absExact(px), Math.absExact(py)));
            long K = Math.multiplyExact(2L, Math.multiplyExact(A, P));
            Math.multiplyExact(2L, Math.multiplyExact(K, K));
            long V = Math.addExact(Math.multiplyExact(2L, Math.multiplyExact(C, K)), 2);
            long endpointNumeratorBound = Math.addExact(Math.multiplyExact(K, V), C);
            Math.multiplyExact(2L, Math.multiplyExact(K, endpointNumeratorBound));
            return true;
        } catch (ArithmeticException ex) {
            return false;
        }
    }

    private static final int BASE_INTEGER_COLUMNS = 4;
    private static final int MAX_EUCLID_DEPTH = 256;

    /**
     * 全ての不等式を満たす整数点上で {@code px*x + py*y} を最大化する。
     *
     * @param a 各制約 {@code a[i] * x + b[i] * y <= c[i]} の {@code x} の係数
     * @param b 各制約 {@code a[i] * x + b[i] * y <= c[i]} の {@code y} の係数
     * @param c 各制約 {@code a[i] * x + b[i] * y <= c[i]} の右辺
     * @param px 目的関数 {@code px*x + py*y} の {@code x} の係数
     * @param py 目的関数 {@code px*x + py*y} の {@code y} の係数
     * @return {@code OPTIMAL}, {@code INFEASIBLE}, {@code UNBOUNDED} のいずれかを表す求解結果
     * @throws IllegalArgumentException 配列長が一致しない場合、制約が空の場合、
     *         または入力が不正な場合
     * @throws ArithmeticException 厳密計算で必要な中間値が {@code long} に収まらない場合
     */
    public static Result maximize(long[] a, long[] b, long[] c, long px, long py) {
        int m = a.length;
        if (b.length != m || c.length != m) throw new IllegalArgumentException("配列長が一致していません");
        boolean zeroObjective = px == 0 && py == 0;
        if (m == 0) return zeroObjective ? Result.optimal(0, 0, 0) : Result.unbounded();
        long opx = zeroObjective ? 1 : px;
        long opy = zeroObjective ? 0 : py;

        Transform tr = objectiveTransform(opx, opy);
        Constraint[] cs = new Constraint[m];
        int z = 0;
        for (int i = 0; i < m; i++) {
            long A = Math.addExact(Math.multiplyExact(a[i], tr.s), Math.multiplyExact(b[i], tr.t));
            long B = Math.subtractExact(Math.multiplyExact(a[i], tr.q), Math.multiplyExact(b[i], tr.p));
            long C = c[i];

            long g = MathUtils.gcd(A, B);
            if (g == 0) {
                if (C < 0) return Result.infeasible();
                continue;
            }
            A /= g;
            B /= g;
            C = Math.floorDiv(C, g);
            cs[z++] = new Constraint(A, B, C);
        }
        cs = Arrays.copyOf(cs, z);

        if (cs.length == 0) {
            if (zeroObjective) return Result.optimal(0, 0, 0);
            return Result.unbounded();
        }

        ArrayList<Line> lowerLines = new ArrayList<>();
        ArrayList<Line> upperLines = new ArrayList<>();

        long explicitMinX = Long.MIN_VALUE;
        long explicitMaxX = Long.MAX_VALUE;
        long maxAbsC = 0;
        long maxAbsB = 0;
        boolean hasPositiveB = false, hasNegativeB = false;

        for (Constraint w : cs) {
            maxAbsC = Math.max(maxAbsC, Math.absExact(w.c));
            maxAbsB = Math.max(maxAbsB, Math.absExact(w.b));
            if (w.b > 0) {
                upperLines.add(new Line(-w.a, w.c, w.b));
                hasPositiveB = true;
            } else if (w.b < 0) {
                lowerLines.add(new Line(w.a, -w.c, -w.b));
                hasNegativeB = true;
            } else {
                if (w.a > 0) {
                    explicitMaxX = Math.min(explicitMaxX, Math.floorDiv(w.c, w.a));
                } else if (w.a < 0) {
                    explicitMinX = Math.max(explicitMinX, Math.ceilDiv(w.c, w.a));
                } else if (w.c < 0) {
                    return Result.infeasible();
                }
            }
        }

        if (explicitMinX > explicitMaxX) return Result.infeasible();

        if (!hasPositiveB || !hasNegativeB) {
            if (!zeroObjective && explicitMaxX == Long.MAX_VALUE) {
                return Result.unbounded();
            }

            long X;
            if (!zeroObjective) {
                X = explicitMaxX;
            } else if (explicitMinX != Long.MIN_VALUE) {
                X = explicitMinX;
            } else if (explicitMaxX != Long.MAX_VALUE) {
                X = explicitMaxX;
            } else {
                X = 0;
            }

            Long Y = chooseFeasibleYOneSided(cs, X, hasPositiveB, hasNegativeB);
            if (Y == null) return Result.infeasible();
            return restoreResult(tr, X, Y, px, py);
        }

        boolean unboundedObjective = !zeroObjective && hasPositiveXRecession(cs);

        long vertexBound = Math.addExact(Math.multiplyExact(2L, Math.multiplyExact(maxAbsC, maxAbsB)), 2);
        long minX = Math.max(Math.negateExact(vertexBound), explicitMinX);
        long maxX = Math.min(vertexBound, explicitMaxX);
        if (minX > maxX) return Result.infeasible();

        Envelope lower = buildMaxEnvelope(lowerLines);
        Envelope upper = buildMinEnvelope(upperLines);
        List<Slab> slabs = mergeFeasibleSlabs(lower, upper, new Frac(minX, 1), new Frac(maxX, 1));
        if (slabs.isEmpty()) return Result.infeasible();

        IntPoint best = null;
        for (int i = slabs.size() - 1; i >= 0; i--) {
            IntPoint p = rightmostLatticePoint(slabs.get(i));
            if (p == null) continue;
            if (!satisfiesAll(cs, p.x, p.y)) {
                throw new AssertionError("帯状領域または包絡線の内部処理に誤りがあります");
            }
            best = p;
            break;
        }

        if (best == null) return Result.infeasible();
        if (unboundedObjective) return Result.unbounded();
        return restoreResult(tr, best.x, best.y, px, py);
    }

    /**
     * 全ての不等式を満たす整数点上で {@code px*x + py*y} を最小化する。
     *
     * <p>制約数を {@code m}、包絡線から得られる帯状領域の数を {@code S}、
     * 座標・係数の大きさを {@code D} とすると、時間計算量は
     * {@code O(m log m + S log D)}、追加領域計算量は {@code O(m)}。
     *
     * @param a 各制約 {@code a[i] * x + b[i] * y <= c[i]} の {@code x} の係数
     * @param b 各制約 {@code a[i] * x + b[i] * y <= c[i]} の {@code y} の係数
     * @param c 各制約 {@code a[i] * x + b[i] * y <= c[i]} の右辺
     * @param px 目的関数 {@code px*x + py*y} の {@code x} の係数
     * @param py 目的関数 {@code px*x + py*y} の {@code y} の係数
     * @return {@code OPTIMAL}, {@code INFEASIBLE}, {@code UNBOUNDED} のいずれかを表す求解結果
     * @throws IllegalArgumentException 配列長が一致しない場合、または入力が不正な場合
     * @throws ArithmeticException {@code px} または {@code py} が {@link Long#MIN_VALUE} の場合、
     *         または厳密計算で必要な中間値が {@code long} に収まらない場合
     */
    // 未テスト
    public static Result minimize(long[] a, long[] b, long[] c, long px, long py) {
        Result result = maximize(a, b, c, Math.negateExact(px), Math.negateExact(py));
        if (result.status != Status.OPTIMAL) return result;
        return Result.optimal(result.x, result.y, Math.negateExact(result.value));
    }

    /**
     * {@code X} を正方向へ増加させる recession direction が存在するか判定する。
     *
     * <p>{@code dX > 0} なら尺度を {@code dX = 1} に正規化できるので、
     * {@code A_i + B_i r <= 0} を全て満たす実数 {@code r=dY/dX} の存在判定に帰着する。
     */
    private static boolean hasPositiveXRecession(Constraint[] cs) {
        Frac lower = null;
        Frac upper = null;
        for (Constraint w : cs) {
            if (w.b > 0) {
                Frac q = new Frac(Math.negateExact(w.a), w.b);
                if (upper == null || q.compareTo(upper) < 0) upper = q;
            } else if (w.b < 0) {
                Frac q = new Frac(w.a, Math.negateExact(w.b));
                if (lower == null || q.compareTo(lower) > 0) lower = q;
            } else if (w.a > 0) {
                return false;
            }
        }
        return lower == null || upper == null || lower.compareTo(upper) <= 0;
    }

    /** Y が片側にしか制限されない場合に、指定した整数 X に対する実行可能な整数 Y を1つ返す。 */
    private static Long chooseFeasibleYOneSided(
            Constraint[] cs, long x, boolean hasPositiveB, boolean hasNegativeB) {
        if (hasPositiveB && hasNegativeB) throw new IllegalArgumentException();

        if (hasPositiveB) {
            long y = Long.MAX_VALUE;
            for (Constraint w : cs) {
                if (w.b > 0) {
                    long rhs = Math.subtractExact(w.c, Math.multiplyExact(w.a, x));
                    y = Math.min(y, Math.floorDiv(rhs, w.b));
                } else if (w.b == 0 && Math.multiplyExact(w.a, x) > w.c) {
                    return null;
                }
            }
            return y;
        }

        if (hasNegativeB) {
            long y = Long.MIN_VALUE;
            for (Constraint w : cs) {
                if (w.b < 0) {
                    long rhs = Math.subtractExact(w.c, Math.multiplyExact(w.a, x));
                    y = Math.max(y, Math.ceilDiv(rhs, w.b));
                } else if (w.b == 0 && Math.multiplyExact(w.a, x) > w.c) {
                    return null;
                }
            }
            return y;
        }

        for (Constraint w : cs) {
            if (Math.multiplyExact(w.a, x) > w.c) return null;
        }
        return 0L;
    }

    private static Result restoreResult(Transform tr, long X, long Y, long px, long py) {
        long x;
        long y;
        long value;
        try {
            x = Math.addExact(Math.multiplyExact(tr.s, X), Math.multiplyExact(tr.q, Y));
            y = Math.subtractExact(Math.multiplyExact(tr.t, X), Math.multiplyExact(tr.p, Y));
            value = Math.addExact(Math.multiplyExact(px, x), Math.multiplyExact(py, y));
        } catch (ArithmeticException ex) {
            x = BigInteger.valueOf(tr.s).multiply(BigInteger.valueOf(X))
                    .add(BigInteger.valueOf(tr.q).multiply(BigInteger.valueOf(Y))).longValueExact();
            y = BigInteger.valueOf(tr.t).multiply(BigInteger.valueOf(X))
                    .subtract(BigInteger.valueOf(tr.p).multiply(BigInteger.valueOf(Y))).longValueExact();
            value = BigInteger.valueOf(px).multiply(BigInteger.valueOf(x))
                    .add(BigInteger.valueOf(py).multiply(BigInteger.valueOf(y))).longValueExact();
        }
        return Result.optimal(x, y, value);
    }

    // -------------------------------------------------------------------------
    // 目的関数の正規化
    // -------------------------------------------------------------------------

    private record Transform(long s, long t, long p, long q) {}
    private record Egcd(long g, long x, long y) {}

    private static Transform objectiveTransform(long px, long py) {
        long g = MathUtils.gcd(px, py);
        if (g == 0) throw new IllegalArgumentException("補助目的関数がゼロです");
        long p = px / g;
        long q = py / g;
        Egcd e = egcdNonnegative(Math.absExact(p), Math.absExact(q));
        long s = p < 0 ? Math.negateExact(e.x) : e.x;
        long t = q < 0 ? Math.negateExact(e.y) : e.y;
        if (Math.addExact(Math.multiplyExact(p, s), Math.multiplyExact(q, t)) != 1) throw new AssertionError();
        return new Transform(s, t, p, q);
    }

    private static Egcd egcdNonnegative(long a, long b) {
        long oldR = a, r = b;
        long oldS = 1, s = 0;
        long oldT = 0, t = 1;
        while (r != 0) {
            long q = oldR / r;
            long nr = oldR - q * r; oldR = r; r = nr;
            long ns = Math.subtractExact(oldS, Math.multiplyExact(q, s)); oldS = s; s = ns;
            long nt = Math.subtractExact(oldT, Math.multiplyExact(q, t)); oldT = t; t = nt;
        }
        return new Egcd(oldR, oldS, oldT);
    }

    // -------------------------------------------------------------------------
    // 静的 CHT による下側・上側包絡線
    // -------------------------------------------------------------------------

    /** {@code y = (a*x + b) / d} を表す直線。常に {@code d > 0}。 */
    private record Line(long a, long b, long d) {
        Line {
            if (d <= 0) throw new IllegalArgumentException("d は正でなければなりません");
            long g = MathUtils.gcd(MathUtils.gcd(a, b), d);
            if (g > 1) {
                a /= g;
                b /= g;
                d /= g;
            }
        }

        Frac at(Frac x) {
            return evalAffine(a, b, d, x);
        }

        Frac at(long x) {
            try {
                return new Frac(Math.addExact(Math.multiplyExact(a, x), b), d);
            } catch (ArithmeticException ex) {
                return new Frac(
                        BigInteger.valueOf(a).multiply(BigInteger.valueOf(x)).add(BigInteger.valueOf(b)),
                        BigInteger.valueOf(d));
            }
        }

        Line negateValue() {
            return new Line(Math.negateExact(a), Math.negateExact(b), d);
        }

        Line shearDependent(long k) {
            // せん断変換 y' = y - kx
            return new Line(Math.subtractExact(a, Math.multiplyExact(k, d)), b, d);
        }

        Line inverseMinus(long k) {
            // y=(a*x+b)/d, a>0 を x について解き、x-k*y を表す直線へ変換する。
            if (a <= 0) throw new AssertionError("軸交換前の直線は単調増加でなければなりません");
            return new Line(Math.subtractExact(d, Math.multiplyExact(k, a)), Math.negateExact(b), a);
        }
    }

    private record Envelope(Line[] line, Frac[] from) {
        int size() { return line.length; }
    }

    private static Envelope buildMinEnvelope(List<Line> lines) {
        ArrayList<Line> neg = new ArrayList<>(lines.size());
        for (Line l : lines) neg.add(l.negateValue());
        Envelope e = buildMaxEnvelope(neg);
        Line[] ret = new Line[e.line.length];
        for (int i = 0; i < ret.length; i++) ret[i] = e.line[i].negateValue();
        return new Envelope(ret, e.from);
    }

    /**
     * 直線群の上側包絡線を構築する。
     *
     * <p>返される直線は傾きの昇順に並び、{@code x} の増加に従って有効な直線も
     * この順に遷移する。傾きでソートした後は、各直線が高々 1 回ずつスタックへ追加・削除される。
     *
     * @param input 包絡線候補の直線群
     * @return 上側包絡線
     */
    private static Envelope buildMaxEnvelope(List<Line> input) {
        if (input.isEmpty()) throw new IllegalArgumentException("包絡線候補が空です");
        Line[] a = input.toArray(Line[]::new);
        Arrays.sort(a, (u, v) -> {
            int c = compareProducts128(u.a, v.d, v.a, u.d); // 傾き u.a/u.d と v.a/v.d を比較
            if (c != 0) return c;
            return compareProducts128(u.b, v.d, v.b, u.d); // 切片を比較
        });

        ArrayList<Line> unique = new ArrayList<>();
        for (Line l : a) {
            if (!unique.isEmpty() && sameSlope(unique.get(unique.size() - 1), l)) {
                Line last = unique.get(unique.size() - 1);
                if (compareProducts128(l.b, last.d, last.b, l.d) > 0) {
                    unique.set(unique.size() - 1, l);
                }
            } else {
                unique.add(l);
            }
        }

        ArrayList<Line> hull = new ArrayList<>();
        ArrayList<Frac> from = new ArrayList<>();
        for (Line l : unique) {
            while (!hull.isEmpty()) {
                Frac x = intersectionX(hull.get(hull.size() - 1), l);
                if (hull.size() == 1 || x.compareTo(from.get(from.size() - 1)) > 0) {
                    hull.add(l);
                    from.add(x);
                    l = null;
                    break;
                }
                hull.remove(hull.size() - 1);
                from.remove(from.size() - 1);
            }
            if (l != null) {
                hull.add(l);
                from.add(null); // 最初の直線は -∞ から有効
            }
        }
        return new Envelope(hull.toArray(Line[]::new), from.toArray(Frac[]::new));
    }

    private static boolean sameSlope(Line p, Line q) {
        return compareProducts128(p.a, q.d, q.a, p.d) == 0;
    }

    /**
     * 傾きが {@code p < q} である 2 直線の交点の {@code x} 座標を返す。
     */
    private static Frac intersectionX(Line p, Line q) {
        try {
            long num = Math.subtractExact(Math.multiplyExact(p.b, q.d), Math.multiplyExact(q.b, p.d));
            long den = Math.subtractExact(Math.multiplyExact(q.a, p.d), Math.multiplyExact(p.a, q.d));
            if (den <= 0) throw new AssertionError("傾きが昇順ではありません");
            return new Frac(num, den);
        } catch (ArithmeticException ex) {
            BigInteger num = BigInteger.valueOf(p.b).multiply(BigInteger.valueOf(q.d))
                    .subtract(BigInteger.valueOf(q.b).multiply(BigInteger.valueOf(p.d)));
            BigInteger den = BigInteger.valueOf(q.a).multiply(BigInteger.valueOf(p.d))
                    .subtract(BigInteger.valueOf(p.a).multiply(BigInteger.valueOf(q.d)));
            if (den.signum() <= 0) throw new AssertionError("傾きが昇順ではありません");
            return Frac.ofReducedOrBig(num, den);
        }
    }

    // -------------------------------------------------------------------------
    // 上下の包絡線を、各1本の有効直線が固定される帯状領域へ分割
    // -------------------------------------------------------------------------

    private record Slab(Frac left, Frac right, Line lower, Line upper) {}

    private static List<Slab> mergeFeasibleSlabs(Envelope lower, Envelope upper,
                                                  Frac minX, Frac maxX) {
        ArrayList<Slab> ret = new ArrayList<>();
        int il = activeAt(lower, minX);
        int iu = activeAt(upper, minX);
        Frac cur = minX;

        while (cur.compareTo(maxX) <= 0) {
            Frac next = maxX;
            if (il + 1 < lower.size() && lower.from[il + 1].compareTo(next) < 0) next = lower.from[il + 1];
            if (iu + 1 < upper.size() && upper.from[iu + 1].compareTo(next) < 0) next = upper.from[iu + 1];
            if (next.compareTo(cur) < 0) throw new AssertionError();

            Line lo = lower.line[il], up = upper.line[iu];
            Frac[] feasible = intersectWhereLowerLeUpper(cur, next, lo, up);
            if (feasible != null && feasible[0].compareTo(feasible[1]) <= 0) {
                ret.add(new Slab(feasible[0], feasible[1], lo, up));
            }

            if (next.compareTo(maxX) == 0) break;
            cur = next;
            while (il + 1 < lower.size() && lower.from[il + 1].compareTo(cur) <= 0) il++;
            while (iu + 1 < upper.size() && upper.from[iu + 1].compareTo(cur) <= 0) iu++;
        }
        return ret;
    }

    private static int activeAt(Envelope e, Frac x) {
        int lo = 0, hi = e.size();
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            Frac f = e.from[mid];
            if (f != null && f.compareTo(x) <= 0) lo = mid;
            else hi = mid;
        }
        return lo;
    }

    private static Frac[] intersectWhereLowerLeUpper(Frac l, Frac r, Line low, Line up) {
        // (up.a/up.d-low.a/low.d)*x + (up.b/up.d-low.b/low.d) >= 0
        try {
            long A = Math.subtractExact(Math.multiplyExact(up.a, low.d), Math.multiplyExact(low.a, up.d));
            long B = Math.subtractExact(Math.multiplyExact(up.b, low.d), Math.multiplyExact(low.b, up.d));
            if (A == 0) return B >= 0 ? new Frac[]{l, r} : null;
            Frac root = new Frac(Math.negateExact(B), A);
            if (A > 0) {
                Frac nl = max(l, root);
                return nl.compareTo(r) <= 0 ? new Frac[]{nl, r} : null;
            }
            Frac nr = min(r, root);
            return l.compareTo(nr) <= 0 ? new Frac[]{l, nr} : null;
        } catch (ArithmeticException ex) {
            // long の積または符号反転が表現不能な場合だけ任意精度へ昇格する。
        }
        BigInteger A = BigInteger.valueOf(up.a).multiply(BigInteger.valueOf(low.d))
                .subtract(BigInteger.valueOf(low.a).multiply(BigInteger.valueOf(up.d)));
        BigInteger B = BigInteger.valueOf(up.b).multiply(BigInteger.valueOf(low.d))
                .subtract(BigInteger.valueOf(low.b).multiply(BigInteger.valueOf(up.d)));
        if (A.signum() == 0) return B.signum() >= 0 ? new Frac[]{l, r} : null;
        Frac root = Frac.ofReducedOrBig(B.negate(), A);
        if (A.signum() > 0) {
            Frac nl = max(l, root);
            return nl.compareTo(r) <= 0 ? new Frac[]{nl, r} : null;
        } else {
            Frac nr = min(r, root);
            return l.compareTo(nr) <= 0 ? new Frac[]{l, nr} : null;
        }
    }

    // -------------------------------------------------------------------------
    // 1つの帯状領域で O(log D) で格子点を求める: 切り詰め + ユークリッド型せん断
    // -------------------------------------------------------------------------

    private record Strip(long left, long right, Line lower, Line upper) {}
    private record IntPoint(long x, long y) {}

    private static IntPoint rightmostLatticePoint(Slab slab) {
        // 必要なのは整数 X だけなので、実数区間を直ちに整数区間へ縮約する。
        // これにより、long 専用実装ではユークリッド型再帰の中で
        // 有理数の区間端点を持ち回る必要がなくなる。
        long minX = slab.left.ceil();
        long maxX = slab.right.floor();
        if (minX > maxX) return null;

        // t=-X と置くと、最大 X の探索は t を第1キーとする辞書順最小化になる。
        Strip s = new Strip(
                Math.negateExact(maxX),
                Math.negateExact(minX),
                new Line(Math.negateExact(slab.lower.a), slab.lower.b, slab.lower.d),
                new Line(Math.negateExact(slab.upper.a), slab.upper.b, slab.upper.d));

        // 以下の切り詰めアルゴリズムは、左右端の縦区間が交わらないとき、
        // 左から右へ向かって区間が上昇する向きを仮定する。必要なら従属座標を反転する。
        boolean reflectedY = false;
        Frac lL = s.lower.at(s.left), uL = s.upper.at(s.left);
        Frac lR = s.lower.at(s.right), uR = s.upper.at(s.right);
        if (uR.compareTo(lL) < 0) {
            reflectedY = true;
            s = reflectDependent(s);
        }

        IntPoint p = lexicographicallySmallestLatticePoint(s, 0);
        if (p == null) return null;
        long X = Math.negateExact(p.x);
        long Y = reflectedY ? Math.negateExact(p.y) : p.y;
        return new IntPoint(X, Y);
    }

    private static Strip reflectDependent(Strip s) {
        // y'=-y と反転すると、新しい下側は -旧上側、新しい上側は -旧下側になる。
        return new Strip(s.left, s.right, s.upper.negateValue(), s.lower.negateValue());
    }

    /**
     * 2 本の直線にはさまれた 帯状領域内の格子点のうち、まず {@code x} を最小化し、
     * その中で {@code y} を最小化した点を返す。
     *
     * <p>独立変数の区間端点は整数として保持する。これは一般性を失わない。
     * 格子点の独立座標は必ず整数なので、軸交換のたびに新しい有理数区間を
     * {@code ceil}/{@code floor} で直ちに整数区間へ縮約できるためである。
     *
     * <p>状態不変条件として、左右端の縦区間が互いに交わらない場合、
     * 右端の縦区間が左端の縦区間より上にあるものとする。
     *
     * @param s 処理対象の帯状領域
     * @param depth ユークリッド互除法型の縮約の再帰深さ
     * @return 辞書順最小格子点。格子点が存在しない場合は {@code null}
     */
    private static IntPoint lexicographicallySmallestLatticePoint(Strip s, int depth) {
        if (depth > MAX_EUCLID_DEPTH) throw new AssertionError("ユークリッド型縮約が収束しませんでした");
        if (s.left > s.right) return null;

        if (s.right - s.left <= BASE_INTEGER_COLUMNS) {
            for (long x = s.left; ; x++) {
                long yl = s.lower.at(x).ceil();
                long yu = s.upper.at(x).floor();
                if (yl <= yu) return new IntPoint(x, yl);
                if (x == s.right) break;
            }
            return null;
        }

        Frac lowL = s.lower.at(s.left);
        Frac upL = s.upper.at(s.left);
        Frac lowR = s.lower.at(s.right);
        Frac upR = s.upper.at(s.right);
        if (lowL.compareTo(upL) > 0 || lowR.compareTo(upR) > 0) throw new AssertionError();

        // 簡単な場合: 帯状領域全体を横切る水平線分が存在する。
        Frac commonLow = max(lowL, lowR);
        Frac commonHigh = min(upL, upR);
        if (commonLow.compareTo(commonHigh) <= 0) {
            Frac h = commonLow;
            return lexMin(candidateBelow(s, h), candidateAbove(s, h));
        }

        // 不変条件より、交わらない左右端の縦区間は左から右へ上昇する。
        if (upL.compareTo(lowR) >= 0) {
            throw new AssertionError("帯状領域の向きに関する不変条件が壊れています");
        }

        // 左上端 (left, upL) と右下端 (right, lowR) を結ぶ最短の接続線分。
        Frac dy = lowR.subtract(upL); // 分母は境界直線由来のものだけ
        long dx = Math.subtractExact(s.right, s.left);
        if (dy.signum() <= 0 || dx <= 0) throw new AssertionError();

        if (compareFracToLong(dy, dx) >= 0) {
            // V 型: y' = y-kx。Z^2 上の全単射であり、第1座標 x を保存する。
            long k = floorFracDivLong(dy, dx);
            if (k <= 0) throw new AssertionError();
            Strip t = new Strip(s.left, s.right,
                    s.lower.shearDependent(k), s.upper.shearDependent(k));
            IntPoint p = lexicographicallySmallestLatticePoint(t, depth + 1);
            if (p == null) return null;
            return new IntPoint(p.x, Math.addExact(p.y, Math.multiplyExact(k, p.x)));
        }

        // H 型: 両側の簡単な部分を切り落とし、中央部に x'=x-k*y を適用して軸を交換する。
        IntPoint best = null;
        best = lexMin(best, candidateBelow(s, upL));
        best = lexMin(best, candidateAbove(s, lowR));

        long k = floorLongDivFrac(dx, dy); // floor(1 / 傾き)。必ず 1 以上
        if (k <= 0) throw new AssertionError();
        if (s.lower.a <= 0 || s.upper.a <= 0) {
            throw new AssertionError("H 型切り詰めで境界が単調増加ではありません");
        }

        // 新しい独立座標は X'=旧y、従属座標は Y'=旧x-k*旧y。
        // 中央部では旧y が [upL, lowR] にある。X' は整数でなければならないため、
        // この区間を直ちに整数区間へ縮約する。これで格子点を全て保存したまま、
        // 再帰をまたいだ分母の増大を防げる。
        long newLeft = upL.ceil();
        long newRight = lowR.floor();
        if (newLeft <= newRight) {
            Strip middle = new Strip(
                    newLeft,
                    newRight,
                    s.upper.inverseMinus(k), // 軸交換後の左側・下側境界
                    s.lower.inverseMinus(k));

            IntPoint q = lexicographicallySmallestLatticePoint(middle, depth + 1);
            if (q != null) {
                // 旧y=q.x、旧x=q.y+k*q.x
                IntPoint old = new IntPoint(Math.addExact(q.y, Math.multiplyExact(k, q.x)), q.x);
                best = lexMin(best, old);
            }
        }
        return best;
    }

    /**
     * 水平な切断線 {@code y = h} より下側の 簡単部分 から辞書順最小格子点候補を求める。
     * {@code h} はこの部分領域を囲う矩形の上辺に相当する。
     */
    private static IntPoint candidateBelow(Strip s, Frac h) {
        long yScan = h.floor();
        LongInterval xs = xIntervalAtIntegerY(s, yScan);
        if (xs == null) return null;
        long x = xs.lo;
        long y = s.lower.at(x).ceil();
        if (y > s.upper.at(x).floor() || new Frac(y, 1).compareTo(h) > 0) {
            throw new AssertionError();
        }
        return new IntPoint(x, y);
    }

    /**
     * 水平な切断線 {@code y = h} より上側の 簡単部分 から辞書順最小格子点候補を求める。
     * {@code h} はこの部分領域を囲う矩形の下辺に相当する。
     */
    private static IntPoint candidateAbove(Strip s, Frac h) {
        long yScan = h.ceil();
        LongInterval xs = xIntervalAtIntegerY(s, yScan);
        if (xs == null) return null;
        return new IntPoint(xs.lo, yScan);
    }

    private record LongInterval(long lo, long hi) {}

    /**
     * 指定した整数 {@code y} に対して
     * {@code lower(x) <= y <= upper(x)} を満たす整数 {@code x} の区間を返す。
     */
    private static LongInterval xIntervalAtIntegerY(Strip s, long y) {
        Frac lo = new Frac(s.left, 1), hi = new Frac(s.right, 1);
        Frac[] z;

        z = intersectAffineLe(s.lower, y, lo, hi);
        if (z == null) return null;
        lo = z[0]; hi = z[1];
        z = intersectAffineGe(s.upper, y, lo, hi);
        if (z == null) return null;
        lo = z[0]; hi = z[1];

        long L = lo.ceil(), R = hi.floor();
        return L <= R ? new LongInterval(L, R) : null;
    }

    private static Frac[] intersectAffineLe(Line f, long y, Frac lo, Frac hi) {
        // (a*x+b)/d <= y  <=> a*x <= d*y-b.
        try {
            long rhs = Math.subtractExact(Math.multiplyExact(f.d, y), f.b);
            if (f.a == 0) return rhs >= 0 ? new Frac[]{lo, hi} : null;
            Frac root = new Frac(rhs, f.a);
            if (f.a > 0) hi = min(hi, root);
            else lo = max(lo, root);
            return lo.compareTo(hi) <= 0 ? new Frac[]{lo, hi} : null;
        } catch (ArithmeticException ex) {
            // rhs が long に収まらない場合だけ任意精度へ昇格する。
        }
        BigInteger rhs = BigInteger.valueOf(f.d).multiply(BigInteger.valueOf(y))
                .subtract(BigInteger.valueOf(f.b));
        if (f.a == 0) return rhs.signum() >= 0 ? new Frac[]{lo, hi} : null;
        Frac root = Frac.ofReducedOrBig(rhs, BigInteger.valueOf(f.a));
        if (f.a > 0) hi = min(hi, root);
        else lo = max(lo, root);
        return lo.compareTo(hi) <= 0 ? new Frac[]{lo, hi} : null;
    }

    private static Frac[] intersectAffineGe(Line f, long y, Frac lo, Frac hi) {
        // (a*x+b)/d >= y  <=> a*x >= d*y-b.
        try {
            long rhs = Math.subtractExact(Math.multiplyExact(f.d, y), f.b);
            if (f.a == 0) return rhs <= 0 ? new Frac[]{lo, hi} : null;
            Frac root = new Frac(rhs, f.a);
            if (f.a > 0) lo = max(lo, root);
            else hi = min(hi, root);
            return lo.compareTo(hi) <= 0 ? new Frac[]{lo, hi} : null;
        } catch (ArithmeticException ex) {
            // rhs が long に収まらない場合だけ任意精度へ昇格する。
        }
        BigInteger rhs = BigInteger.valueOf(f.d).multiply(BigInteger.valueOf(y))
                .subtract(BigInteger.valueOf(f.b));
        if (f.a == 0) return rhs.signum() <= 0 ? new Frac[]{lo, hi} : null;
        Frac root = Frac.ofReducedOrBig(rhs, BigInteger.valueOf(f.a));
        if (f.a > 0) lo = max(lo, root);
        else hi = min(hi, root);
        return lo.compareTo(hi) <= 0 ? new Frac[]{lo, hi} : null;
    }

    private static int compareFracToLong(Frac a, long b) {
        if (a.isBig()) {
            return a.bigN.compareTo(BigInteger.valueOf(b).multiply(a.bigD));
        }
        return compareProducts128(a.n, 1, b, a.d);
    }

    /** 正の有理数 {@code a} と正整数 {@code b} に対する {@code floor(a / b)} を返す。 */
    private static long floorFracDivLong(Frac a, long b) {
        if (a.signum() <= 0 || b <= 0) throw new IllegalArgumentException();
        if (a.isBig()) {
            return a.bigN.divide(a.bigD.multiply(BigInteger.valueOf(b))).longValueExact();
        }
        long g = MathUtils.gcd(a.n, b);
        long an = a.n / g;
        long bb = b / g;
        try {
            long den = Math.multiplyExact(a.d, bb);
            return an / den;
        } catch (ArithmeticException ex) {
            return BigInteger.valueOf(a.n)
                    .divide(BigInteger.valueOf(a.d).multiply(BigInteger.valueOf(b)))
                    .longValueExact();
        }
    }

    /** 正整数 {@code a} と正の有理数 {@code b} に対する {@code floor(a / b)} を返す。 */
    private static long floorLongDivFrac(long a, Frac b) {
        if (a <= 0 || b.signum() <= 0) throw new IllegalArgumentException();
        if (b.isBig()) {
            return BigInteger.valueOf(a).multiply(b.bigD).divide(b.bigN).longValueExact();
        }
        long g = MathUtils.gcd(a, b.n);
        long aa = a / g;
        long bn = b.n / g;
        try {
            long num = Math.multiplyExact(aa, b.d);
            return num / bn;
        } catch (ArithmeticException ex) {
            return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b.d))
                    .divide(BigInteger.valueOf(b.n)).longValueExact();
        }
    }

    private static IntPoint lexMin(IntPoint a, IntPoint b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.x != b.x) return a.x < b.x ? a : b;
        return a.y <= b.y ? a : b;
    }

    // -------------------------------------------------------------------------
    // 制約と検証
    // -------------------------------------------------------------------------

    private record Constraint(long a, long b, long c) {}

    private static boolean satisfiesAll(Constraint[] cs, long x, long y) {
        for (Constraint z : cs) {
            try {
                long lhs = Math.addExact(Math.multiplyExact(z.a, x), Math.multiplyExact(z.b, y));
                if (lhs > z.c) return false;
            } catch (ArithmeticException ex) {
                BigInteger lhs = BigInteger.valueOf(z.a).multiply(BigInteger.valueOf(x))
                        .add(BigInteger.valueOf(z.b).multiply(BigInteger.valueOf(y)));
                if (lhs.compareTo(BigInteger.valueOf(z.c)) > 0) return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // long を優先し、必要時だけ BigInteger へ昇格する厳密な有理数演算
    // -------------------------------------------------------------------------

    private static final class Frac implements Comparable<Frac> {
        final long n, d; // bigN == null のとき、d>0 かつ gcd(|n|,d)=1
        final BigInteger bigN, bigD; // bigN != null のとき、bigD>0 かつ gcd(|bigN|,bigD)=1

        Frac(long n, long d) {
            if (d == 0) throw new ArithmeticException("分母がゼロです");
            if (d < 0) {
                n = Math.negateExact(n);
                d = Math.negateExact(d);
            }
            long g = MathUtils.gcd(n, d);
            this.n = n / g;
            this.d = d / g;
            this.bigN = null;
            this.bigD = null;
        }

        private Frac(long n, long d, boolean alreadyReduced) {
            this.n = n;
            this.d = d;
            this.bigN = null;
            this.bigD = null;
        }

        /**
         * {@code n/d} を既約かつ分母が正の形で保持する。
         * ビット長を {@code L} とすると計算量は {@code O(M(L) log L)}。
         */
        // 未テスト
        Frac(BigInteger n, BigInteger d) {
            if (d.signum() == 0) throw new ArithmeticException("分母がゼロです");
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            BigInteger g = n.gcd(d);
            this.n = 0;
            this.d = 0;
            this.bigN = n.divide(g);
            this.bigD = d.divide(g);
        }

        /**
         * {@code n/d} を既約化し、収まる場合は {@code long}、それ以外は任意精度で保持する。
         * ビット長を {@code L} とすると計算量は {@code O(M(L) log L)}。
         */
        // 未テスト
        static Frac ofReducedOrBig(BigInteger n, BigInteger d) {
            if (d.signum() == 0) throw new ArithmeticException("分母がゼロです");
            if (d.signum() < 0) {
                n = n.negate();
                d = d.negate();
            }
            BigInteger g = n.gcd(d);
            n = n.divide(g);
            d = d.divide(g);
            if (n.bitLength() <= 63 && d.bitLength() <= 63) {
                return new Frac(n.longValue(), d.longValue(), true);
            }
            return new Frac(n, d);
        }

        /** 任意精度表現なら {@code true} を返す。計算量は {@code O(1)}。 */
        // 未テスト
        boolean isBig() { return bigN != null; }

        /** 分子を任意精度整数として返す。計算量は {@code O(1)}。 */
        // 未テスト
        BigInteger numerator() { return isBig() ? bigN : BigInteger.valueOf(n); }

        /** 正の分母を任意精度整数として返す。計算量は {@code O(1)}。 */
        // 未テスト
        BigInteger denominator() { return isBig() ? bigD : BigInteger.valueOf(d); }

        int signum() { return isBig() ? bigN.signum() : Long.compare(n, 0); }

        Frac negate() {
            if (isBig()) return ofReducedOrBig(bigN.negate(), bigD);
            if (n != Long.MIN_VALUE) return new Frac(-n, d, true);
            return ofReducedOrBig(BigInteger.valueOf(n).negate(), BigInteger.valueOf(d));
        }

        Frac subtract(Frac o) { return add(o.negate()); }

        Frac add(Frac o) {
            if (isBig() || o.isBig()) return addBig(o);
            try {
                return addLong(o);
            } catch (ArithmeticException ex) {
                return addBig(o);
            }
        }

        /**
         * 両オペランドが {@code long} 表現であるときに和を返す。
         * 係数の大きさを {@code D} とすると計算量は {@code O(log D)}。
         */
        // 未テスト
        private Frac addLong(Frac o) {
            long g = MathUtils.gcd(d, o.d);
            long p = d / g;
            long q = o.d / g;
            long num = Math.addExact(Math.multiplyExact(n, q), Math.multiplyExact(o.n, p));
            long den = Math.multiplyExact(p, o.d);
            long h = MathUtils.gcd(num, g);
            // p と q は互いに素なので、den と共通する残りの因子は g にしか含まれない。
            if (h > 1) {
                num /= h;
                den /= h;
            }
            return new Frac(num, den); // 念のため最後にも正規化する。コストは小さい。
        }

        /**
         * 任意精度演算で和を返し、結果が収まれば {@code long} 表現へ戻す。
         * ビット長を {@code L} とすると計算量は {@code O(M(L) log L)}。
         */
        // 未テスト
        private Frac addBig(Frac o) {
            BigInteger num = numerator().multiply(o.denominator())
                    .add(o.numerator().multiply(denominator()));
            BigInteger den = denominator().multiply(o.denominator());
            return ofReducedOrBig(num, den);
        }

        long floor() {
            if (!isBig()) return Math.floorDiv(n, d);
            BigInteger[] qr = bigN.divideAndRemainder(bigD);
            if (bigN.signum() < 0 && qr[1].signum() != 0) qr[0] = qr[0].subtract(BigInteger.ONE);
            return qr[0].longValueExact();
        }

        long ceil() {
            if (!isBig()) return Math.ceilDiv(n, d);
            BigInteger[] qr = bigN.divideAndRemainder(bigD);
            if (bigN.signum() > 0 && qr[1].signum() != 0) qr[0] = qr[0].add(BigInteger.ONE);
            return qr[0].longValueExact();
        }

        @Override public int compareTo(Frac o) {
            if (isBig() || o.isBig()) {
                return numerator().multiply(o.denominator()).compareTo(o.numerator().multiply(denominator()));
            }
            return compareProducts128(n, o.d, o.n, d);
        }

        @Override public String toString() {
            return isBig() ? bigN + "/" + bigD : n + "/" + d;
        }
    }

    /**
     * {@code (a*x + b) / d} を厳密に評価する。
     * 乗算前に可能な限り gcd で約分し、中間値の増大を抑える。
     */
    private static Frac evalAffine(long a, long b, long d, Frac x) {
        if (x.isBig()) {
            BigInteger num = BigInteger.valueOf(a).multiply(x.bigN)
                    .add(BigInteger.valueOf(b).multiply(x.bigD));
            BigInteger den = BigInteger.valueOf(d).multiply(x.bigD);
            return Frac.ofReducedOrBig(num, den);
        }
        try {
            return evalAffineLong(a, b, d, x);
        } catch (ArithmeticException ex) {
            BigInteger num = BigInteger.valueOf(a).multiply(BigInteger.valueOf(x.n))
                    .add(BigInteger.valueOf(b).multiply(BigInteger.valueOf(x.d)));
            BigInteger den = BigInteger.valueOf(d).multiply(BigInteger.valueOf(x.d));
            return Frac.ofReducedOrBig(num, den);
        }
    }

    /**
     * {@code long} 表現の {@code x} に対して {@code (a*x+b)/d} を評価する。
     * 係数の大きさを {@code D} とすると計算量は {@code O(log D)}。
     */
    // 未テスト
    private static Frac evalAffineLong(long a, long b, long d, Frac x) {
        long g1 = MathUtils.gcd(a, x.d);
        long aa = a / g1;
        long xd = x.d / g1;
        long num = Math.addExact(Math.multiplyExact(aa, x.n), Math.multiplyExact(b, xd));
        long g2 = MathUtils.gcd(num, d);
        num /= g2;
        long den = Math.multiplyExact(d / g2, xd);
        return new Frac(num, den);
    }

    private static Frac min(Frac a, Frac b) { return a.compareTo(b) <= 0 ? a : b; }
    private static Frac max(Frac a, Frac b) { return a.compareTo(b) >= 0 ? a : b; }


    /**
     * {@code a*b} と {@code c*d} を符号付き 128 ビット積として厳密に比較する。
     *
     * <p>各積を上位 64 bit と下位 64 bit の 2 個の {@code long} で扱うため、
     * オブジェクト確保も {@code BigInteger} も不要である。
     *
     * @return {@code a*b < c*d} なら負、等しければ 0、大きければ正
     */
    private static int compareProducts128(long a, long b, long c, long d) {
        long lo1 = a * b;
        long hi1 = Math.multiplyHigh(a, b);
        long lo2 = c * d;
        long hi2 = Math.multiplyHigh(c, d);
        int z = Long.compare(hi1, hi2);
        if (z != 0) return z;
        return Long.compareUnsigned(lo1, lo2);
    }
}
