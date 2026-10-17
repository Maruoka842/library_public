package library.util.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DoubleVectorDetTest {

	@Test
	public void testDetBasic() {
		DoubleVector v1 = new DoubleVector(3.0, 4.0);
		DoubleVector v2 = new DoubleVector(1.0, 2.0);

		// det = 3.0 * 2.0 - 4.0 * 1.0 = 2.0
		double expected = 2.0;
		assertEquals(expected, v1.det(v2), 1e-9);
		assertEquals(expected, v1.cross(v2), 1e-9);

		// anti-symmetry: det(v2, v1) = -det(v1, v2)
		assertEquals(-expected, v2.det(v1), 1e-9);
	}

	@Test
	public void testDetParallelAndZero() {
		DoubleVector v1 = new DoubleVector(2.0, 5.0);
		DoubleVector v2 = new DoubleVector(4.0, 10.0); // parallel to v1

		assertEquals(0.0, v1.det(v2), 1e-9);
		assertEquals(0.0, v1.det(DoubleVector.ZERO), 1e-9);
		assertEquals(0.0, DoubleVector.ZERO.det(v1), 1e-9);
	}

	@Test
	public void testDetOrthogonalBasis() {
		assertEquals(1.0, DoubleVector.EX.det(DoubleVector.EY), 1e-9);
		assertEquals(-1.0, DoubleVector.EY.det(DoubleVector.EX), 1e-9);
		assertEquals(0.0, DoubleVector.EX.det(DoubleVector.EX), 1e-9);
	}
}
