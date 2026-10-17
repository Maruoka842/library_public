package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

public class MulAll2DTest {

    @Test
    public void testEmptyList() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        long[][] expected = poly.one();
        assertArrayEquals(expected, poly.mulAll(List.of()));
    }

    @Test
    public void testSingleElement() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        long[][] f = {{1, 2}, {3, 4}};
        assertTrue(poly.equals(f, poly.mulAll(List.of(new long[][][]{f}))));
    }

    @Test
    public void testIdenticalElements() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        long[][] f = {{1, 1}, {1, 0}}; // 1 + y + x
        List<long[][]> list = List.of(f, f, f);
        long[][] expected = poly.powFull(f, 3);
        assertTrue(poly.equals(expected, poly.mulAll(list)));
    }

    @Test
    public void testMixedElements() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        long[][] f1 = {{1, 1}}; // 1 + y
        long[][] f2 = {{1}, {1}}; // 1 + x
        List<long[][]> list = List.of(f1, f1, f2, f1, f2); // (1+y)^3 * (1+x)^2
        // (1+y)^3 = 1 + 3y + 3y^2 + y^3
        // (1+x)^2 = 1 + 2x + x^2
        // product = (1 + 3y + 3y^2 + y^3) * (1 + 2x + x^2)
        //         = (1 + 3y + 3y^2 + y^3) + 2x(1 + 3y + 3y^2 + y^3) + x^2(1 + 3y + 3y^2 + y^3)
        long[][] expected = {
            {1, 3, 3, 1}, // x^0
            {2, 6, 6, 2}, // x^1
            {1, 3, 3, 1}  // x^2
        };
        assertTrue(poly.equals(expected, poly.mulAll(list)));
    }

    @Test
    public void testMulArrayList() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        ArrayList<long[][]> factors = new ArrayList<>();
        factors.add(new long[][] {{1, 1}});      // 1 + y
        factors.add(new long[][] {{1}, {1}});   // 1 + x
        factors.add(new long[][] {{2, 1}, {3}}); // 2 + y + 3x
        long[][] expected = poly.mul(poly.mul(factors.get(0), factors.get(1)), factors.get(2));
        assertTrue(poly.equals(expected, poly.mul(factors)));
        assertTrue(poly.equals(poly.one(), poly.mul(new ArrayList<>())));
    }

    @Test
    public void testMulArrayListModuloPowers() {
        PolynomialFpDynamic2D poly = PolynomialFpDynamic2D.MOD998244353;
        ArrayList<long[][]> factors = new ArrayList<>();
        factors.add(new long[][] {{1, 1, 1}, {1, 2}});
        factors.add(new long[][] {{2, 3}, {4, 5}, {6}});
        factors.add(new long[][] {{7, 8}, {9}});

        long[][] full = poly.mul(poly.mul(factors.get(0), factors.get(1)), factors.get(2));
        long[][] expected = new long[2][2];
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[i].length; j++) {
                expected[i][j] = full[i][j];
            }
        }
        assertTrue(poly.equals(expected, poly.mul(factors, 2, 2)));
        assertTrue(poly.equals(poly.one(), poly.mul(new ArrayList<>(), 2, 2)));
        assertTrue(poly.equals(poly.zero(), poly.mul(factors, 0, 2)));
        assertTrue(poly.equals(poly.zero(), poly.mul(factors, 2, 0)));
    }
}
