package library.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JumpMNaturalSlopeTrickTest {

	@Test
	public void testSingleton() {
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.singleton(5, 10);
		assertEquals(5, st.left());
		assertEquals(5, st.right());
		assertEquals(1, st.size());
		assertEquals(10, st.valueAt(5));
		assertEquals(10, st.minValue());
		assertEquals(5, st.argmin());
		assertArrayEquals(new long[] { 10 }, st.toArray());
		assertTrue(st.checkInternal());
	}

	@Test
	public void testOfAndValidation() {
		long[] values = { 10, 5, 2, 1, 3, 7 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(-2, values);
		assertEquals(-2, st.left());
		assertEquals(3, st.right());
		assertEquals(6, st.size());
		assertEquals(10, st.valueAt(-2));
		assertEquals(1, st.valueAt(1));
		assertEquals(1, st.minValue());
		assertEquals(1, st.argmin());
		assertArrayEquals(values, st.toArray());
		assertTrue(st.checkInternal());

		long[] invalidValues = { 10, 2, 5, 1 }; // d_0 = -8, d_2 = -4, but d_1 = 3, wait
		// d_0 = -8, d_1 = 3, d_2 = -4. d_0 = -8 <= d_2 = -4 is true.
		// Let's break d_i <= d_{i+2}:
		// d_0 = 10 -> 2 is -8
		// d_1 = 2 -> 1 is -1
		// d_2 = 1 -> -10 is -11
		// d_0 (-8) > d_2 (-11), so invalid!
		long[] nonConvex = { 10, 2, 1, -10 };
		assertThrows(IllegalArgumentException.class, () -> RopeJumpMNaturalSlopeTrick.of(0, nonConvex, true));
	}

	@Test
	public void testAddConstantAndLinearAndScaleAndShift() {
		long[] values = { 4, 1, 2, 5, 10 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, values);

		st.addConstant(3);
		assertEquals(7, st.valueAt(0));
		assertEquals(4, st.valueAt(1));
		assertTrue(st.checkInternal());

		st.addLinear(2, -1); // f(x) += 2x - 1
		// x=0: 7 + 0 - 1 = 6
		// x=1: 4 + 2 - 1 = 5
		assertEquals(6, st.valueAt(0));
		assertEquals(5, st.valueAt(1));
		assertTrue(st.checkInternal());

		st.scale(2); // f(x) *= 2
		assertEquals(12, st.valueAt(0));
		assertEquals(10, st.valueAt(1));
		assertTrue(st.checkInternal());

		st.shift(3); // domain becomes [3, 7]
		assertEquals(3, st.left());
		assertEquals(7, st.right());
		assertEquals(12, st.valueAt(3));
		assertEquals(10, st.valueAt(4));
		assertTrue(st.checkInternal());
	}

	@Test
	public void testRestrict() {
		long[] values = { 10, 8, 5, 3, 2, 4, 8 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, values);

		st.restrict(1, 5);
		assertEquals(1, st.left());
		assertEquals(5, st.right());
		assertEquals(8, st.valueAt(1));
		assertEquals(2, st.valueAt(4));
		assertEquals(4, st.valueAt(5));
		assertTrue(st.checkInternal());

		assertThrows(IllegalArgumentException.class, () -> st.restrict(10, 20));
	}

	@Test
	public void testHingesAndAbs() {
		long[] values = { 5, 3, 2, 3, 6 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, values);

		st.addRightHinge(2, 3); // f(x) += 3 * max(0, x - 2)
		// x=0: 5
		// x=1: 3
		// x=2: 2
		// x=3: 3 + 3 = 6
		// x=4: 6 + 6 = 12
		assertEquals(5, st.valueAt(0));
		assertEquals(3, st.valueAt(1));
		assertEquals(2, st.valueAt(2));
		assertEquals(6, st.valueAt(3));
		assertEquals(12, st.valueAt(4));
		assertTrue(st.checkInternal());

		st.addLeftHinge(2, 2); // f(x) += 2 * max(0, 2 - x)
		// x=0: 5 + 4 = 9
		// x=1: 3 + 2 = 5
		// x=2: 2
		assertEquals(9, st.valueAt(0));
		assertEquals(5, st.valueAt(1));
		assertEquals(2, st.valueAt(2));
		assertTrue(st.checkInternal());

		st.addAbs(2, 1); // f(x) += |x - 2|
		// x=0: 9 + 2 = 11
		// x=2: 2 + 0 = 2
		assertEquals(11, st.valueAt(0));
		assertEquals(2, st.valueAt(2));
		assertTrue(st.checkInternal());
	}

	@Test
	public void testRelax3AndRelax() {
		long[] values = { 10, 6, 3, 1, 0, 2, 5, 9 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, values);

		// Test relax: g(x) = min(f(x+1) + 1, f(x) + 0, f(x-1) + 2)
		// which is relax3(-1, 1, 0, 2)
		st.relax(1, 0, 2);
		// domain should shift by -1 to size+2-1 = size+1
		assertEquals(-1, st.left());
		assertEquals(8, st.right());
		assertTrue(st.checkInternal());

		long[] arr = st.toArray();
		// Verify min-plus convolution against naive result
		for (int x = -1; x <= 8; x++) {
			long expected = Long.MAX_VALUE;
			// j=0: f(x - (-1) - 0) + 1 = f(x + 1) + 1
			if (x + 1 >= 0 && x + 1 < values.length) {
				expected = Math.min(expected, values[x + 1] + 1);
			}
			// j=1: f(x - (-1) - 1) + 0 = f(x) + 0
			if (x >= 0 && x < values.length) {
				expected = Math.min(expected, values[x] + 0);
			}
			// j=2: f(x - (-1) - 2) + 2 = f(x - 1) + 2
			if (x - 1 >= 0 && x - 1 < values.length) {
				expected = Math.min(expected, values[x - 1] + 2);
			}
			assertEquals(expected, arr[x - (int) st.left()], "at x=" + x);
		}
	}

	@Test
	public void testArgminAndMinValue() {
		long[] values = { 10, 5, 2, 2, 2, 4, 8 };
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, values);

		assertEquals(2, st.minValue());
		assertEquals(2, st.argmin());
		assertTrue(st.checkInternal());
	}

	@Test
	public void testOutOfBoundsAndExceptions() {
		RopeJumpMNaturalSlopeTrick st = RopeJumpMNaturalSlopeTrick.of(0, new long[] { 1, 2, 3 });
		assertThrows(IllegalArgumentException.class, () -> st.valueAt(-1));
		assertThrows(IllegalArgumentException.class, () -> st.valueAt(3));
		assertThrows(IllegalArgumentException.class, () -> st.scale(-1));
		assertThrows(IllegalArgumentException.class, () -> RopeJumpMNaturalSlopeTrick.of(0, new long[0]));
	}
}
