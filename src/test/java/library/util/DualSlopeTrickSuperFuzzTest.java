package library.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DualSlopeTrickSuperFuzzTest {
    static final long SEED = 0x6a09e667f3bcc909L;
    static final Random rnd = new Random(SEED);

    static final int EXTEND_RIGHT = 0;
    static final int EXTEND_LEFT = 1;
    static final int ADD_LINEAR = 2;
    static final int SHIFT = 3;

    static class Op {
        int type;
        long a, b;

        Op(int type, long a, long b) {
            this.type = type;
            this.a = a;
            this.b = b;
        }

        void apply(DualSlopeTrickSuper f) {
            switch (type) {
            case EXTEND_RIGHT:
                f.extendRight(a, b);
                break;
            case EXTEND_LEFT:
                f.extendLeft(a, b);
                break;
            case ADD_LINEAR:
                f.addLinear(a, b);
                break;
            case SHIFT:
                f.shift(a);
                break;
            default:
                throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            switch (type) {
            case EXTEND_RIGHT:
                return "extendRight(" + a + ", " + b + ")";
            case EXTEND_LEFT:
                return "extendLeft(" + a + ", " + b + ")";
            case ADD_LINEAR:
                return "addLinear(" + a + ", " + b + ")";
            case SHIFT:
                return "shift(" + a + ")";
            default:
                return "?";
            }
        }
    }

    static class Spec {
        List<Op> ops = new ArrayList<>();

        DualSlopeTrickSuper build() {
            DualSlopeTrickSuper f = new DualSlopeTrickSuper();
            for (Op op : ops) op.apply(f);
            return f;
        }

        @Override
        public String toString() {
            return ops.toString();
        }
    }

    static long randomLong(long lo, long hi) {
        return lo + rnd.nextInt((int) (hi - lo + 1));
    }

    static Spec randomSpec() {
        Spec s = new Spec();

        int n = 1 + rnd.nextInt(12);
        for (int i = 0; i < n; i++) {
            int type = rnd.nextInt(4);
            switch (type) {
            case EXTEND_RIGHT:
                // 座標を狭くして同一 breakpoint も大量に発生させる
                s.ops.add(new Op(
                        EXTEND_RIGHT,
                        randomLong(-4, 4),
                        randomLong(1, 4)));
                break;

            case EXTEND_LEFT:
                s.ops.add(new Op(
                        EXTEND_LEFT,
                        randomLong(-4, 4),
                        randomLong(1, 4)));
                break;

            case ADD_LINEAR:
                s.ops.add(new Op(
                        ADD_LINEAR,
                        randomLong(-3, 3),
                        randomLong(-5, 5)));
                break;

            case SHIFT:
                s.ops.add(new Op(
                        SHIFT,
                        randomLong(-3, 3),
                        0));
                break;
            }
        }
        return s;
    }

    static void fail(
            String what,
            long expected,
            long actual,
            int tc,
            Spec sf,
            Spec sg,
            String extra) {

        throw new AssertionError(
                what
                + "\ntc = " + tc
                + "\nseed = " + SEED
                + "\nexpected = " + expected
                + "\nactual   = " + actual
                + "\nf = " + sf
                + "\ng = " + sg
                + (extra == null ? "" : "\n" + extra));
    }

    static void eq(
            String what,
            long expected,
            long actual,
            int tc,
            Spec sf,
            Spec sg,
            String extra) {

        if (expected != actual) {
            fail(what, expected, actual, tc, sf, sg, extra);
        }
    }

    static void compareAggregate(
            String what,
            SlopeTrickSuper.Aggregate expected,
            SlopeTrickSuper.Aggregate actual,
            int tc,
            Spec sf,
            Spec sg,
            String extra) {

        if (expected.sumX() != actual.sumX()
                || expected.sumDelta() != actual.sumDelta()) {

            throw new AssertionError(
                    what
                        + "\ntc = " + tc
                        + "\nseed = " + SEED
                        + "\nexpected = " + expected
                        + "\nactual   = " + actual
                        + "\nf = " + sf
                        + "\ng = " + sg
                        + (extra == null ? "" : "\n" + extra));
        }
    }

    /**
     * DL > -INF かつ f*.domainLeft() == -INF の条件下で、view.marginal(k) が
     * 正しく restricted 後の offset を参照しているかをランダムテスト。
     */
    @org.junit.jupiter.api.Test
    void fuzzRestrictedMarginalOffset() {
        Random rnd = new Random(1);

        for (int tc = 0; tc < 10_000; tc++) {
            DualSlopeTrickSuper f1 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g1 = new DualSlopeTrickSuper();

            DualSlopeTrickSuper f2 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g2 = new DualSlopeTrickSuper();

            int nf = 5 + rnd.nextInt(20);
            for (int i = 0; i < nf; i++) {
                long slope = rnd.nextInt(21) - 10;
                long weight = 1 + rnd.nextInt(4);

                f1.extendRight(slope, weight);
                f2.extendRight(slope, weight);
            }

            int ng = 5 + rnd.nextInt(20);
            for (int i = 0; i < ng; i++) {
                long slope = rnd.nextInt(21) - 10;
                long weight = 1 + rnd.nextInt(4);

                g1.extendRight(slope, weight);
                g2.extendRight(slope, weight);
            }

            long dl = rnd.nextInt(11) - 5;
            long dr = dl + 1 + rnd.nextInt(10);

            g1.clampSlope(dl, dr);
            g2.clampSlope(dl, dr);

            var view = f1.minPlusConvolutionView(g1);

            f2.minPlusConvolveDestructive(g2);

            long total = f2.getDualSlopeTrickSuper().getTotalWeight();

            for (long k = 0; k < total; k++) {
                long expected = f2.marginal(k);
                long actual = view.marginal(k);

                if (expected != actual) {
                    throw new AssertionError(
                            "restricted marginal mismatch"
                            + "\ntc = " + tc
                            + "\ndl = " + dl
                            + "\ndr = " + dr
                            + "\nk = " + k
                            + "\ntotal = " + total
                            + "\nexpected = " + expected
                            + "\nactual   = " + actual);
                }
            }
        }
    }

    /**
     * activeDL == 0 または activeDR == 0 の時、argmin() が正常に normal cone
     * (lo = -INF や hi = INF) を返すかをテスト。
     */
    @org.junit.jupiter.api.Test
    void fuzzArgminAtDualBoundary() {
        Random rnd = new Random(2);

        for (int tc = 0; tc < 10_000; tc++) {
            DualSlopeTrickSuper f1 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g1 = new DualSlopeTrickSuper();

            DualSlopeTrickSuper f2 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g2 = new DualSlopeTrickSuper();

            int nf = 1 + rnd.nextInt(10);
            for (int i = 0; i < nf; i++) {
                long slope = rnd.nextInt(11) - 5;
                long weight = 1 + rnd.nextInt(3);

                f1.extendRight(slope, weight);
                f2.extendRight(slope, weight);
            }

            int ng = 1 + rnd.nextInt(10);
            for (int i = 0; i < ng; i++) {
                long slope = rnd.nextInt(11) - 5;
                long weight = 1 + rnd.nextInt(3);

                g1.extendRight(slope, weight);
                g2.extendRight(slope, weight);
            }

            if (rnd.nextBoolean()) {
                long r = 1 + rnd.nextInt(10);

                f1.clampSlope(0, r);
                f2.clampSlope(0, r);
            } else {
                long l = -1 - rnd.nextInt(10);

                g1.clampSlope(l, 0);
                g2.clampSlope(l, 0);
            }

            var view = f1.minPlusConvolutionView(g1);

            f2.minPlusConvolveDestructive(g2);

            SlopeTrickSuper.QueryResult expected = f2.argmin();
            SlopeTrickSuper.QueryResult actual = view.argmin();

            if (expected.min() != actual.min()
                    || expected.lo() != actual.lo()
                    || expected.hi() != actual.hi()) {

                throw new AssertionError(
                        "argmin mismatch"
                        + "\ntc = " + tc
                        + "\nexpected = " + expected
                        + "\nactual   = " + actual);
            }
        }
    }

    /**
     * 0 が必ず H* の minimizer になる構成で normal cone を検証。
     */
    @org.junit.jupiter.api.Test
    void fuzzArgminNormalCone() {
        Random rnd = new Random(3);

        for (int tc = 0; tc < 10_000; tc++) {
            DualSlopeTrickSuper f1 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g1 = new DualSlopeTrickSuper();

            DualSlopeTrickSuper f2 = new DualSlopeTrickSuper();
            DualSlopeTrickSuper g2 = new DualSlopeTrickSuper();

            int nf = 1 + rnd.nextInt(20);
            for (int i = 0; i < nf; i++) {
                long slope = rnd.nextInt(8);
                long weight = 1 + rnd.nextInt(4);

                f1.extendRight(slope, weight);
                f2.extendRight(slope, weight);
            }

            int ng = 1 + rnd.nextInt(20);
            for (int i = 0; i < ng; i++) {
                long slope = rnd.nextInt(8);
                long weight = 1 + rnd.nextInt(4);

                g1.extendRight(slope, weight);
                g2.extendRight(slope, weight);
            }

            long r1 = 1 + rnd.nextInt(10);
            long r2 = 1 + rnd.nextInt(10);

            f1.clampSlope(0, r1);
            g1.clampSlope(0, r2);

            f2.clampSlope(0, r1);
            g2.clampSlope(0, r2);

            var view = f1.minPlusConvolutionView(g1);

            f2.minPlusConvolveDestructive(g2);

            var expected = f2.argmin();
            var actual = view.argmin();

            if (!expected.equals(actual)) {
                throw new AssertionError(
                        "normal-cone argmin mismatch"
                        + "\ntc = " + tc
                        + "\nr1 = " + r1
                        + "\nr2 = " + r2
                        + "\nexpected = " + expected
                        + "\nactual   = " + actual);
            }
        }
    }

    /**
     * conjugate domain が R 全体の場合。
     *
     * 主に
     * - getAt()
     * - domain の端点
     * - ±INF を端点に持つ subgradient
     * - marginal/prod/maxRight
     *
     * を破壊的 convolution と比較する。
     */
    @org.junit.jupiter.api.Test
    void fuzzUnrestrictedConvolutionView() {
        for (int tc = 0; tc < 5_000; tc++) {
            Spec sf = randomSpec();
            Spec sg = randomSpec();

            DualSlopeTrickSuper fView = sf.build();
            DualSlopeTrickSuper gView = sg.build();

            DualSlopeTrickSuper fReal = sf.build();
            DualSlopeTrickSuper gReal = sg.build();

            DualSlopeTrickSuper.MinPlusConvolutionView view =
                    fView.minPlusConvolutionView(gView);

            fReal.minPlusConvolveDestructive(gReal);

            compareWholeView(view, fReal, tc, sf, sg);
        }
    }

    /**
     * f*, g* に異なる有限 domain を与える。
     *
     * (f Box g)* = f* + g*
     *
     * の定義域が
     *
     * dom(f*) intersection dom(g*)
     *
     * になることを view が正しく処理しているかを大量に検査する。
     */
    @org.junit.jupiter.api.Test
    void fuzzRestrictedDualDomains() {
        for (int tc = 0; tc < 5_000; tc++) {
            Spec sf = randomSpec();
            Spec sg = randomSpec();

            /*
             * 必ず 0 を共通部分に含める。
             *
             * ただし f と g の範囲は高確率で異なるようにする。
             */
            long lf = -1 - rnd.nextInt(6);
            long rf =  1 + rnd.nextInt(6);
            long lg = -1 - rnd.nextInt(6);
            long rg =  1 + rnd.nextInt(6);

            DualSlopeTrickSuper fView = sf.build();
            DualSlopeTrickSuper gView = sg.build();

            DualSlopeTrickSuper fReal = sf.build();
            DualSlopeTrickSuper gReal = sg.build();

            fView.clampSlope(lf, rf);
            gView.clampSlope(lg, rg);

            fReal.clampSlope(lf, rf);
            gReal.clampSlope(lg, rg);

            DualSlopeTrickSuper.MinPlusConvolutionView view =
                    fView.minPlusConvolutionView(gView);

            fReal.minPlusConvolveDestructive(gReal);

            try {
                compareWholeView(view, fReal, tc, sf, sg);
            } catch (AssertionError e) {
                throw new AssertionError(
                        e.getMessage()
                        + "\nf dual domain = [" + lf + ", " + rf + "]"
                        + "\ng dual domain = [" + lg + ", " + rg + "]",
                        e);
            }
        }
    }

    static void compareWholeView(
            DualSlopeTrickSuper.MinPlusConvolutionView view,
            DualSlopeTrickSuper real,
            int tc,
            Spec sf,
            Spec sg) {

        eq("domainLeft",
                real.domainLeft(),
                view.domainLeft(),
                tc, sf, sg, null);

        eq("domainRight",
                real.domainRight(),
                view.domainRight(),
                tc, sf, sg, null);

        eq("getMin",
                real.getMin(),
                view.getMin(),
                tc, sf, sg, null);

        /*
         * getAt / slope。
         *
         * 有限定義域なら境界を必ず踏む。
         * getAt() の ±INF optimizer バグを狙うため重要。
         */
        long dl = real.domainLeft();
        long dr = real.domainRight();

        long[] specialX = {
                -10, -5, -1, 0, 1, 5, 10,
                dl,
                dr,
                dl == -SlopeTrickSuper.INF ? 0 : dl + 1,
                dr == SlopeTrickSuper.INF ? 0 : dr - 1
        };

        for (long x : specialX) {
            if (x <= -SlopeTrickSuper.INF || x >= SlopeTrickSuper.INF) {
                continue;
            }

            eq("getAt(" + x + ")",
                    real.getAt(x),
                    view.getAt(x),
                    tc, sf, sg, "x = " + x);

            eq("getSlopeLeft(" + x + ")",
                    real.getSlopeLeft(x),
                    view.getSlopeLeft(x),
                    tc, sf, sg, "x = " + x);

            eq("getSlopeRight(" + x + ")",
                    real.getSlopeRight(x),
                    view.getSlopeRight(x),
                    tc, sf, sg, "x = " + x);
        }

        for (int z = 0; z < 10; z++) {
            long x = randomLong(-15, 15);

            eq("random getAt(" + x + ")",
                    real.getAt(x),
                    view.getAt(x),
                    tc, sf, sg, "x = " + x);

            eq("random getSlopeLeft(" + x + ")",
                    real.getSlopeLeft(x),
                    view.getSlopeLeft(x),
                    tc, sf, sg, "x = " + x);

            eq("random getSlopeRight(" + x + ")",
                    real.getSlopeRight(x),
                    view.getSlopeRight(x),
                    tc, sf, sg, "x = " + x);
        }

        /*
         * destructive convolution が実際に持つ marginal 数を正解とする。
         *
         * このテストを library.util package に置くことで、
         * getTotalWeight() が package-private でも呼べる。
         */
        long total = real.getDualSlopeTrickSuper().getTotalWeight();

        for (long k = 0; k < total; k++) {
            eq("marginal(" + k + ")",
                    real.marginal(k),
                    view.marginal(k),
                    tc, sf, sg, "total = " + total);
        }

        /*
         * view が intersection 外の breakpoint を余計に保持していると、
         * marginal(total) が成功してしまう。
         */
        boolean thrown = false;
        try {
            view.marginal(total);
        } catch (IndexOutOfBoundsException e) {
            thrown = true;
        }

        if (!thrown) {
            throw new AssertionError(
                    "view.marginal(total) must throw"
                        + "\ntc = " + tc
                        + "\nseed = " + SEED
                        + "\ntotal = " + total
                        + "\nf = " + sf
                        + "\ng = " + sg);
        }

        for (int z = 0; z < 30; z++) {
            long l = total == 0 ? 0 : randomLong(0, total);
            long r = randomLong(l, total);

            long expectedSum = real.sumMarginals(l, r);
            long actualSum = view.sumMarginals(l, r);

            eq("sumMarginals(" + l + ", " + r + ")",
                    expectedSum, actualSum,
                    tc, sf, sg,
                    "total = " + total);
        }

        /*
         * specialized maxRight も比較。
         */
        for (int z = 0; z < 30; z++) {
            long l = randomLong(0, total);
            long limit = randomLong(0, 40);

            eq("maxRightMarginals(" + l + ", " + limit + ")",
                    real.maxRightMarginalsSumLeq(l, limit),
                    view.maxRightMarginals(l, limit),
                    tc, sf, sg,
                    "total = " + total);
        }
    }

    /**
     * argmin() の「min = -INF なのに argmin が存在する」ケースをランダム生成する。
     *
     * conjugate domain を 0 から完全に離せば primal は下に非有界。
     */
    @org.junit.jupiter.api.Test
    void fuzzUnboundedBelowArgmin() {
        for (int tc = 0; tc < 5_000; tc++) {
            DualSlopeTrickSuper f = new DualSlopeTrickSuper();

            if (rnd.nextBoolean()) {
                long l = randomLong(1, 10);
                long r = randomLong(l, l + 10);
                f.clampSlope(l, r);
            } else {
                long r = randomLong(-10, -1);
                long l = randomLong(r - 10, r);
                f.clampSlope(l, r);
            }

            long min = f.getMin();
            if (min != -SlopeTrickSuper.INF) {
                throw new AssertionError(
                        "expected unbounded below"
                            + "\ntc = " + tc
                            + "\nseed = " + SEED
                            + "\nmin = " + min);
            }

            SlopeTrickSuper.QueryResult qr = f.argmin();

            /*
             * min = -INF なら有限の最小点は存在しない。
             * QueryResult で空区間を lo > hi と表す、という前提。
             */
            if (qr.lo() <= qr.hi()) {
                throw new AssertionError(
                        "argmin must be empty when min = -INF"
                            + "\ntc = " + tc
                            + "\nseed = " + SEED
                            + "\nargmin = " + qr);
            }
        }
    }

    /**
     * 同一 marginal 値の collision を特に高確率で作るケースで
     * sumMarginals の一致を検証。
     */
    @org.junit.jupiter.api.Test
    void fuzzDuplicateMarginalSum() {
        for (int tc = 0; tc < 5_000; tc++) {
            DualSlopeTrickSuper fView = new DualSlopeTrickSuper();
            DualSlopeTrickSuper gView = new DualSlopeTrickSuper();

            DualSlopeTrickSuper fReal = new DualSlopeTrickSuper();
            DualSlopeTrickSuper gReal = new DualSlopeTrickSuper();

            int nf = 1 + rnd.nextInt(10);
            int ng = 1 + rnd.nextInt(10);

            for (int i = 0; i < nf; i++) {
                long slope = randomLong(-2, 2);
                long weight = randomLong(1, 4);
                fView.extendRight(slope, weight);
                fReal.extendRight(slope, weight);
            }

            for (int i = 0; i < ng; i++) {
                long slope = randomLong(-2, 2);
                long weight = randomLong(1, 4);
                gView.extendRight(slope, weight);
                gReal.extendRight(slope, weight);
            }

            DualSlopeTrickSuper.MinPlusConvolutionView view =
                    fView.minPlusConvolutionView(gView);

            fReal.minPlusConvolveDestructive(gReal);

            long total = fReal.getDualSlopeTrickSuper().getTotalWeight();

            long expected = fReal.sumMarginals(0, total);
            long actual = view.sumMarginals(0, total);

            if (expected != actual) {
                throw new AssertionError(
                        "duplicate marginal sum mismatch"
                            + "\ntc = " + tc
                            + "\nseed = " + SEED
                            + "\nexpected = " + expected
                            + "\nactual   = " + actual);
            }
        }
    }
}
