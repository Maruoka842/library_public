package library.util.fold;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CircularDoubleSum2DTest {

	@Test
	public void testBasicSum2D() {
		double[][] a = {
			{1.0, 2.0},
			{3.0, 4.0}
		};
		CircularDoubleSum2D cds2d = new CircularDoubleSum2D(a);

		assertEquals(10.0, cds2d.sum(0, 0, 2, 2), 1e-9);
		assertEquals(20.0, cds2d.sum(0, 0, 2, 4), 1e-9);
		assertEquals(40.0, cds2d.sum(0, 0, 4, 4), 1e-9);

		assertEquals(1.0, cds2d.sum(0, 0, 1, 1), 1e-9);
		assertEquals(3.0, cds2d.sum(0, 0, 1, 2), 1e-9);
		assertEquals(4.0, cds2d.sum(1, 1, 2, 2), 1e-9);
		assertEquals(0.0, cds2d.sum(1, 1, 1, 1), 1e-9);
	}

	@Test
	public void testIntConstructor2D() {
		int[][] a = {
			{1, 2},
			{3, 4}
		};
		CircularDoubleSum2D cds2d = new CircularDoubleSum2D(a);
		assertEquals(10.0, cds2d.sum(0, 0, 2, 2), 1e-9);
	}
}
