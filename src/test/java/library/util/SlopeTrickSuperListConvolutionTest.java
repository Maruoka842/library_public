package library.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SlopeTrickSuperListConvolutionTest {
    /** 有限区間の 3 関数の畳み込みを全探索と比較する。計算量: O(B^3)、B は検査区間幅。 */
    // 未テスト
    @Test
    void finiteDomainsAndOwnership() {
        SlopeTrickSuper f = new SlopeTrickSuper().addIrelu(-1, 2).addRelu(1, 3)
                .restrictDomain(-3, 4).translate(2);
        SlopeTrickSuper g = new SlopeTrickSuper().addLinear(-2, 7).addRelu(0, 5)
                .restrictDomain(-2, 3);
        long[] beforeF = new long[21];
        long[] beforeG = new long[21];
        for (int x = -10; x <= 10; x++) {
            beforeF[x + 10] = f.getAt(x);
            beforeG[x + 10] = g.getAt(x);
        }
        SlopeTrickSuper h = SlopeTrickSuper.minPlusConvolve(new LinkedList<>(List.of(f, g, f)));
        for (int x = -10; x <= 20; x++) {
            long expected = SlopeTrickSuper.INF;
            for (int a = -1; a <= 6; a++) {
                for (int b = -2; b <= 3; b++) {
                    int c = x - a - b;
                    if (c >= -1 && c <= 6) {
                        expected = Math.min(expected, f.getAt(a) + g.getAt(b) + f.getAt(c));
                    }
                }
            }
            assertEquals(expected, h.getAt(x), "x=" + x);
        }
        h.verifyInvariants();
        h.translate(11).addRelu(0, 9);
        for (int x = -10; x <= 10; x++) {
            assertEquals(beforeF[x + 10], f.getAt(x));
            assertEquals(beforeG[x + 10], g.getAt(x));
        }
    }

    /** 空列・空定義域・単一要素・無限定義域を検証する。計算量: O(1)。 */
    // 未テスト
    @Test
    void identitiesAndUnboundedDomains() {
        SlopeTrickSuper identity = SlopeTrickSuper.minPlusConvolve(List.of());
        assertEquals(0, identity.domainLeft());
        assertEquals(0, identity.domainRight());
        assertEquals(0, identity.getAt(0));
        assertEquals(SlopeTrickSuper.INF, identity.getAt(1));
        SlopeTrickSuper f = new SlopeTrickSuper().addIrelu(0, 2).addRelu(0, 2).translate(3);
        SlopeTrickSuper single = SlopeTrickSuper.minPlusConvolve(List.of(f));
        SlopeTrickSuper twice = SlopeTrickSuper.minPlusConvolve(List.of(f, f));
        for (int x = -10; x <= 10; x++) {
            assertEquals(2 * Math.abs(x - 3), single.getAt(x));
            assertEquals(2 * Math.abs(x - 6), twice.getAt(x));
        }
        SlopeTrickSuper empty = new SlopeTrickSuper().restrictDomain(1, 0);
        SlopeTrickSuper h = SlopeTrickSuper.minPlusConvolve(List.of(f, empty));
        assertTrue(h.domainLeft() > h.domainRight());
        assertEquals(SlopeTrickSuper.INF, h.getAt(0));
        f.translate(10);
        assertEquals(0, single.getAt(3));
    }
}
