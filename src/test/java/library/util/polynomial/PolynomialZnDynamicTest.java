package library.util.polynomial;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;

public class PolynomialZnDynamicTest {

	@Test
	public void testBasicArithmetic() {
		// Test under composite modulo 12
		PolynomialZnDynamic poly = new PolynomialZnDynamic(12);

		long[] a = { 3, 5, 2 }; // 3 + 5x + 2x^2
		long[] b = { 10, 8, 11 }; // 10 + 8x + 11x^2

		// Addition mod 12: {1, 1, 1}
		long[] sum = poly.add(a, b);
		assertArrayEquals(new long[] { 1, 1, 1 }, sum);

		// Negation mod 12 of a: {9, 7, 10}
		long[] negA = poly.neg(a);
		assertArrayEquals(new long[] { 9, 7, 10 }, negA);

		// Subtraction mod 12 (a - b): {5, 9, 3}
		long[] diff = poly.sub(a, b);
		assertArrayEquals(new long[] { 5, 9, 3 }, diff);
	}

	@Test
	public void testMultiplicationComposite() {
		// Test multiplication under composite modulus 100
		PolynomialZnDynamic poly = new PolynomialZnDynamic(100);

		long[] a = { 3, 4 }; // 3 + 4x
		long[] b = { 5, 6 }; // 5 + 6x

		// (3 + 4x)(5 + 6x) = 15 + 38x + 24x^2 mod 100
		long[] expected = { 15, 38, 24 };

		long[] resNaive = poly.mulNaive(a, b);
		assertArrayEquals(expected, resNaive);

		long[] resMul = poly.mul(a, b);
		assertArrayEquals(expected, resMul);
	}

	@Test
	public void testCRTMultiplication() {
		// Test larger multiplication that falls back to CRT multiplication
		PolynomialZnDynamic poly = new PolynomialZnDynamic(1000000007L);

		// Two polynomials of size 150 (above FFT_NAIVE_THRESHOLD = 128)
		long[] a = new long[150];
		long[] b = new long[150];
		for (int i = 0; i < 150; i++) {
			a[i] = (i + 1) * 12345L % 1000000007L;
			b[i] = (i + 1) * 67890L % 1000000007L;
		}

		long[] resNaive = poly.mulNaive(a, b);
		long[] resMul = poly.mul(a, b);

		assertArrayEquals(resNaive, resMul);
	}

	@Test
	public void testExponentiation() {
		// Test powFull under composite modulus 100
		PolynomialZnDynamic poly = new PolynomialZnDynamic(100);

		long[] a = { 3, 2 }; // 3 + 2x
		// (3+2x)^3 = 27 + 54x + 36x^2 + 8x^3 mod 100
		long[] expected = { 27, 54, 36, 8 };

		long[] res = poly.powFull(a, 3);
		assertArrayEquals(expected, res);
	}

	@Test
	public void testUtilities() {
		PolynomialZnDynamic poly = new PolynomialZnDynamic(10);

		long[] a = { 0, 5, 0, 3, 0 };

		assertEquals(3, poly.deg(a));
		assertFalse(poly.isZero(a));
		assertEquals(2, poly.countTerms(a));

		long[] resized = poly.resize(a);
		assertArrayEquals(new long[] { 0, 5, 0, 3 }, resized);

		ArrayList<PolynomialZnDynamic.Term> terms = poly.getTerms(a);
		assertEquals(2, terms.size());
		assertEquals(1, terms.get(0).d);
		assertEquals(5, terms.get(0).v);
		assertEquals(3, terms.get(1).d);
		assertEquals(3, terms.get(1).v);
	}
}
