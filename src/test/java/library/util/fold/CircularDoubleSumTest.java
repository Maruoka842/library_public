package library.util.fold;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CircularDoubleSumTest {

	@Test
	public void testBasicSum() {
		double[] a = {1.5, 2.5, 3.5};
		CircularDoubleSum cds = new CircularDoubleSum(a);

		assertEquals(1.5, cds.get(0), 1e-9);
		assertEquals(2.5, cds.get(1), 1e-9);
		assertEquals(1.5, cds.get(3), 1e-9);

		assertEquals(7.5, cds.sum(0, 3), 1e-9);
		assertEquals(15.0, cds.sum(0, 6), 1e-9);
		assertEquals(2.5, cds.sum(1, 2), 1e-9);
		assertEquals(6.0, cds.sum(1, 3), 1e-9);
		assertEquals(7.5, cds.sum(-3, 0), 1e-9);
	}

	@Test
	public void testIntConstructor() {
		int[] a = {1, 2, 3};
		CircularDoubleSum cds = new CircularDoubleSum(a);
		assertEquals(6.0, cds.sum(0, 3), 1e-9);
	}
}
