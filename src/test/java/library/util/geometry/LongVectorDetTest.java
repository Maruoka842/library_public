package library.util.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LongVectorDetTest {

	@Test
	public void testDetBasic() {
		LongVector v1 = new LongVector(3L, 4L);
		LongVector v2 = new LongVector(1L, 2L);

		// det = 3 * 2 - 4 * 1 = 2
		long expected = 2L;
		assertEquals(expected, v1.det(v2));
		assertEquals(expected, v1.cross(v2));

		// anti-symmetry: det(v2, v1) = -det(v1, v2)
		assertEquals(-expected, v2.det(v1));
	}

	@Test
	public void testDetParallelAndZero() {
		LongVector v1 = new LongVector(2L, 5L);
		LongVector v2 = new LongVector(4L, 10L); // parallel to v1

		assertEquals(0L, v1.det(v2));
		assertEquals(0L, v1.det(LongVector.ZERO));
		assertEquals(0L, LongVector.ZERO.det(v1));
	}

	@Test
	public void testDetOrthogonalBasis() {
		assertEquals(1L, LongVector.EX.det(LongVector.EY));
		assertEquals(-1L, LongVector.EY.det(LongVector.EX));
		assertEquals(0L, LongVector.EX.det(LongVector.EX));
	}
}
