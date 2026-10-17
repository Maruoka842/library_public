package library.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class SlopeTrickSuperCounterexampleTest {

    /*
     * splitDomain の反例を Treap の乱数に依存させないためのテスト用 helper。
     */
    private static void setTreapSeed(SlopeTrickSuper st, long seed) {
        try {
            Field field = SlopeTrickSuper.class.getDeclaredField("rng");
            field.setAccessible(true);
            ((Random) field.get(st)).setSeed(seed);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * convolveLinearSegment:
     *
     * f(x) = 0  (0 <= x <= 1)
     *
     * g(x) = min_(0 <= d <= 2) { f(x-d) - d }
     *
     * domain(g) = [0, 1] + [0, 2] = [0, 3].
     */
    @Test
    void convolveLinearSegmentMustNotShiftLeftDomainEndpoint() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.restrictDomain(0, 1);

        st.convolveLinearSegment(0, 2, -1, 0);

        assertEquals(0, st.domainLeft());
        assertEquals(3, st.domainRight());

        assertEquals(0, st.getAt(0));
        assertEquals(-1, st.getAt(1));
        assertEquals(-2, st.getAt(2));
        assertEquals(-2, st.getAt(3));
    }

    /**
     * splitByWeight が同じ x の node を分割した後、
     * removeRelu は x=a の node 一個の delta しか見ていない。
     *
     * addRelu(0,10) を完全に消せるはずなのに失敗する。
     */
    @Test
    void removeReluMustWorkAfterWeightedSplit() {
        SlopeTrickSuper st = new SlopeTrickSuper();

        st.addRelu(0, 10);

        // 内部で weight=4 と残り6に分割され、再 merge される。
        assertEquals(0, st.breakpointByWeight(4));

        assertDoesNotThrow(() -> st.removeRelu(0, 10));

        // 元のゼロ関数へ戻るはず。
        assertEquals(0, st.getAt(-10));
        assertEquals(0, st.getAt(0));
        assertEquals(0, st.getAt(10));
    }

    /**
     * splitDomain:
     *
     * f(x) = |x-1| + |x-3| + |x-7|
     *
     * x=5 で split した右側でも、
     * boundary 値 f(5)=8 が保存されなければならない。
     */
    @Test
    void splitDomainMustPreserveFunctionValueAtBoundary() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        setTreapSeed(st, 0);

        st.addAbs(1);
        st.addAbs(3);
        st.addAbs(7);

        assertEquals(8, st.getAt(5));

        SlopeTrickSuper[] split = st.splitDomain(5);
        SlopeTrickSuper left = split[0];
        SlopeTrickSuper right = split[1];

        assertEquals(8, left.getAt(5));
        assertEquals(8, right.getAt(5));

        // 周囲も元の関数と一致するべき。
        assertEquals(7, left.getAt(4));
        assertEquals(9, right.getAt(6));
    }

    /**
     * concat:
     *
     * left(x)  = 0, domain [-1,0]
     * right(x) = x, domain [0,1]
     *
     * concatenate 後は
     *
     * g(x) = 0 (x <= 0)
     *        x (x >= 0)
     */
    @Test
    void concatMustInsertSlopeJumpAtBoundary() {
        SlopeTrickSuper left = new SlopeTrickSuper();
        left.restrictDomain(-1, 0);

        SlopeTrickSuper right = new SlopeTrickSuper();
        right.addLinear(1);
        right.restrictDomain(0, 1);

        left.concat(right);

        assertEquals(0, left.getAt(-1));
        assertEquals(0, left.getAt(0));
        assertEquals(1, left.getAt(1));

        assertEquals(1, left.getSlopeRight(0));
    }

    /**
     * mergeDestructive:
     *
     * f(x) = |x|
     * g(x) = |x+1|
     *
     * f+g の値:
     *
     * x=-2 : 3
     * x=-1 : 1
     * x= 0 : 1
     * x= 1 : 3
     */
    @Test
    void mergeDestructiveMustNotDoubleCountBreakpoints() {
        SlopeTrickSuper f = new SlopeTrickSuper();
        f.addAbs(0);

        SlopeTrickSuper g = new SlopeTrickSuper();
        g.addAbs(-1);

        f.mergeDestructive(g);

        assertEquals(3, f.getAt(-2));
        assertEquals(1, f.getAt(-1));
        assertEquals(1, f.getAt(0));
        assertEquals(3, f.getAt(1));
    }

    /**
     * min-plus convolution:
     *
     * f(x) = 2|x|
     * g(x) = |x|
     *
     * h(x) = min_y { 2|x-y| + |y| } = |x|.
     */
    @Test
    void minPlusConvolutionMustHandleInfiniteRightRay() {
        SlopeTrickSuper f = new SlopeTrickSuper();
        f.addAbs(0, 2);

        SlopeTrickSuper g = new SlopeTrickSuper();
        g.addAbs(0);

        f.minPlusConvolveDestructive(g);

        assertEquals(2, f.getAt(-2));
        assertEquals(1, f.getAt(-1));
        assertEquals(0, f.getAt(0));
        assertEquals(1, f.getAt(1));
        assertEquals(2, f.getAt(2));
    }

    /**
     * clampSlope:
     *
     * f(x)=|x-10|
     *
     * の slope を [0,1] に clamp すると、
     * 凸共役側を domain [0,1] に制限する意味では
     *
     * g(x)=max(x-10,0)
     */
    @Test
    void clampSlopeMustPreserveCorrectAffineConstant() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addAbs(10);

        st.clampSlope(0, 1);

        assertEquals(0, st.getAt(0));
        assertEquals(0, st.getAt(10));
        assertEquals(10, st.getAt(20));
    }

    @Test
    void convolveLinearSegmentSingleton() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.restrictDomain(10, 10);

        st.convolveLinearSegment(1, 2, 1, 2);

        assertEquals(11, st.domainLeft());
        assertEquals(12, st.domainRight());

        // g(x) = 0 + (x - 10) + 2
        assertEquals(3, st.getAt(11));
        assertEquals(4, st.getAt(12));
    }

    @Test
    void concatWhenLeftReferenceIsBoundary() {
        SlopeTrickSuper base = new SlopeTrickSuper();

        // translate
        base.translate(5);

        // left: f(x)=0, x<=0
        SlopeTrickSuper left = base.splitDomain(0)[0];

        SlopeTrickSuper right = new SlopeTrickSuper();
        right.addLinear(1);
        right.restrictDomain(0, 1);

        left.concat(right);

        // x<=0 : 0, x>=0 : x
        assertEquals(0, left.getAt(-1));
        assertEquals(0, left.getAt(0));
        assertEquals(1, left.getAt(1));
    }

    @Test
    void mergeMustDiscardBreakpointsOutsideCommonDomain() {
        SlopeTrickSuper f = new SlopeTrickSuper();
        f.addRelu(0, 2);
        f.addRelu(3, 2);
        f.restrictDomain(-5, 5);

        SlopeTrickSuper g = new SlopeTrickSuper();
        g.restrictDomain(2, 4);

        f.mergeDestructive(g);

        f.convolveLinearSegment(0, 1, 3, 0);

        assertEquals(4, f.getAt(2));
        assertEquals(6, f.getAt(3));
        assertEquals(9, f.getAt(4));
        assertEquals(13, f.getAt(5));
    }

    @Test
    void clampSlopeMustLinearlyExtendFiniteDomain() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.restrictDomain(0, 1);

        st.clampSlope(0, 0);

        assertEquals(-SlopeTrickSuper.INF, st.domainLeft());
        assertEquals(SlopeTrickSuper.INF, st.domainRight());

        assertEquals(0, st.getAt(-100));
        assertEquals(0, st.getAt(100));
    }

    @Test
    void minPlusConvolveTwoSegments() {
        SlopeTrickSuper f = new SlopeTrickSuper();
        f.restrictDomain(0, 1);

        SlopeTrickSuper g = new SlopeTrickSuper();
        g.restrictDomain(0, 1);

        f.minPlusConvolveDestructive(g);

        assertEquals(0, f.domainLeft());
        assertEquals(2, f.domainRight());

        assertEquals(0, f.getAt(0));
        assertEquals(0, f.getAt(1));
        assertEquals(0, f.getAt(2));
    }

    @Test
    void splitDomainPreservesRepresentationInvariant() {
        SlopeTrickSuper s = new SlopeTrickSuper();
        s.addRelu(3, 2);

        SlopeTrickSuper[] p = s.splitDomain(3);
        assertDoesNotThrow(p[0]::verifyInvariants);
        assertDoesNotThrow(p[1]::verifyInvariants);
    }

    @Test
    void minPlusConvolveDoubleCountingFirstBreakpointFix() {
        SlopeTrickSuper f = new SlopeTrickSuper();
        f.addRelu(0);

        SlopeTrickSuper g = new SlopeTrickSuper();
        g.addRelu(0);
        g.addRelu(1);

        f.minPlusConvolveDestructive(g);

        assertEquals(0, f.getAt(-1));
        assertEquals(0, f.getAt(0));
        assertEquals(1, f.getAt(1));
    }

    @Test
    void clampSlopeWithOutofDomainZeroAnchorFix() {
        SlopeTrickSuper s = new SlopeTrickSuper();
        s.addLinear(2, -1);
        s.restrictDomain(-1, -1);
        s.clampSlope(-4, 1);

        assertNotEquals(SlopeTrickSuper.INF, s.getAt(0));
        assertEquals(-3, s.getAt(-1));
        assertEquals(-2, s.getAt(0));
    }

    @Test
    void clampSlopeFiniteDomainLineExtension() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addLinear(2, 9);
        st.restrictDomain(-4, 2);

        st.clampSlope(-3, 1);

        assertEquals(19, st.getAt(-10));
        assertEquals(1, st.getAt(-4));
        assertEquals(7, st.getAt(2));
    }

    @Test
    void clampSlopeTreapSeedIndependenceDeterministic() {
        for (int t = 0; t < 1000; t++) {
            SlopeTrickSuper st = new SlopeTrickSuper();
            setTreapSeed(st, t);

            st.addLinear(-1, -1);
            st.addRelu(0, 7);
            st.addRelu(1, 3);
            st.addRelu(2, 2);
            st.restrictDomain(-1, 3);

            st.clampSlope(3, 4);

            assertEquals(3, st.getAt(1), "Failed at iteration " + t);
        }
    }

    @Test
    void moveLeftCurveParameterlessOnAbs() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addAbs(0);
        st.moveLeftCurve();

        assertEquals(0, st.getAt(-10));
        assertEquals(0, st.getAt(0));
        assertEquals(1, st.getAt(1));
        assertEquals(10, st.getAt(10));
    }

    @Test
    void moveRightCurveParameterlessOnAbs() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addAbs(0);
        st.moveRightCurve();

        assertEquals(10, st.getAt(-10));
        assertEquals(1, st.getAt(-1));
        assertEquals(0, st.getAt(0));
        assertEquals(0, st.getAt(10));
    }

    @Test
    void prodBreakpointsByWeightDoesNotCauseNodeFragmentation() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 100);

        assertEquals(1, st.prodBreakpointsByWeight(0, 100).count());

        for (int k = 1; k < 100; k += 3) {
            st.prodBreakpointsByWeight(k, k + 1);
        }

        assertEquals(1, st.prodBreakpointsByWeight(0, 100).count());
    }

    @Test
    void prodBreakpointsByWeightCountAccuracyOnFragment() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 100);

        SlopeTrickSuper.Aggregate agg = st.prodBreakpointsByWeight(10, 20);
        assertEquals(0, agg.sumX());
        assertEquals(10, agg.sumDelta());
        assertEquals(1, agg.count());
    }

    @Test
    void getMinAndArgminMinusLinearDoesNotCauseNodeFragmentation() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 100).addLinear(-50);

        assertEquals(1, st.prodBreakpointsByWeight(0, 100).count());

        for (int p = -49; p <= 49; p++) {
            st.argminMinusLinear(p);
        }

        assertEquals(1, st.prodBreakpointsByWeight(0, 100).count());
    }

    @Test
    void prodBreakpointsByWeightOutOfBoundsThrows() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 100);

        assertThrows(IndexOutOfBoundsException.class, () -> st.prodBreakpointsByWeight(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> st.prodBreakpointsByWeight(0, 101));
        assertThrows(IndexOutOfBoundsException.class, () -> st.prodBreakpointsByWeight(50, 40));
    }

    @Test
    void maxRightBreakpointsByWeightOutOfBoundsThrows() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 100);

        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeight(-1, x -> true));
        assertThrows(IndexOutOfBoundsException.class, () -> st.maxRightBreakpointsByWeight(101, x -> true));
    }

    @Test
    void maxRightBreakpointsByWeightNonEmptyPredicateThrows() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(-1, 1);
        st.addRelu(100, 100);

        assertThrows(IllegalArgumentException.class, () -> st.maxRightBreakpointsByWeight(0, sumX -> sumX < 0));
    }

    @Test
    void verifyInvariantsEnforcesStrictKeyUniqueness() {
        SlopeTrickSuper st = new SlopeTrickSuper();
        st.addRelu(0, 50);
        st.addRelu(0, 50);

        assertDoesNotThrow(st::verifyInvariants);
        assertEquals(1, st.prodBreakpointsByWeight(0, 100).count());
    }
}
