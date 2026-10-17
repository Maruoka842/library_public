package library.util.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleRayTest {

	private static final double EPS = 1e-7;

	@Test
	public void testConstructorsAndAccessors() {
		DoublePoint src = new DoublePoint(1, 2);
		DoubleVector dir = new DoubleVector(3, 4);
		DoubleRay ray1 = new DoubleRay(src, dir);

		assertEquals(src, ray1.source());
		assertEquals(0.6, ray1.direction().x(), EPS);
		assertEquals(0.8, ray1.direction().y(), EPS);

		DoublePoint pThrough = new DoublePoint(4, 6);
		DoubleRay ray2 = new DoubleRay(src, pThrough);

		assertEquals(src, ray2.source());
		assertEquals(0.6, ray2.direction().x(), EPS);
		assertEquals(0.8, ray2.direction().y(), EPS);

		assertEquals(ray1, ray2);
		assertEquals(ray1.hashCode(), ray2.hashCode());

		assertThrows(IllegalArgumentException.class, () -> new DoubleRay(src, new DoubleVector(0, 0)));
		assertThrows(IllegalArgumentException.class, () -> new DoubleRay(src, src));
		assertThrows(NullPointerException.class, () -> new DoubleRay(null, dir));
	}

	@Test
	public void testPointAndSupportingLine() {
		DoubleRay ray = new DoubleRay(new DoublePoint(1, 1), new DoubleVector(1, 0));
		DoublePoint pt = ray.point(3.5);
		assertEquals(4.5, pt.x(), EPS);
		assertEquals(1.0, pt.y(), EPS);

		DoubleLine line = ray.supportingLine();
		assertTrue(line.onLine(ray.source(), EPS));
		assertTrue(line.onLine(pt, EPS));
	}

	@Test
	public void testContains() {
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 1));

		assertTrue(ray.contains(new DoublePoint(0, 0)));
		assertTrue(ray.contains(new DoublePoint(2, 2)));
		assertFalse(ray.contains(new DoublePoint(-1, -1)));
		assertFalse(ray.contains(new DoublePoint(1, 0)));
	}

	@Test
	public void testIntersectLine() {
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		DoubleLine lineVertical = new DoubleLine(1, 0, -5); // x = 5

		DoublePoint res = ray.intersect(lineVertical);
		assertNotNull(res);
		assertEquals(5.0, res.x(), EPS);
		assertEquals(0.0, res.y(), EPS);

		DoubleLine lineBehind = new DoubleLine(1, 0, 5); // x = -5
		assertNull(ray.intersect(lineBehind));
	}

	@Test
	public void testIntersectRay() {
		DoubleRay ray1 = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		DoubleRay ray2 = new DoubleRay(new DoublePoint(2, -2), new DoubleVector(0, 1));

		DoublePoint res = ray1.intersect(ray2);
		assertNotNull(res);
		assertEquals(2.0, res.x(), EPS);
		assertEquals(0.0, res.y(), EPS);

		DoubleRay rayAway = new DoubleRay(new DoublePoint(2, 2), new DoubleVector(0, 1));
		assertNull(ray1.intersect(rayAway));
	}

	@Test
	public void testIntersectSegment() {
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		DoubleSegment segCross = new DoubleSegment(3, -2, 3, 2);

		DoublePoint res = ray.intersect(segCross);
		assertNotNull(res);
		assertEquals(3.0, res.x(), EPS);
		assertEquals(0.0, res.y(), EPS);

		DoubleSegment segMiss = new DoubleSegment(-3, -2, -3, 2);
		assertNull(ray.intersect(segMiss));
	}

	@Test
	public void testIntersectCircle() {
		DoubleRay ray = new DoubleRay(new DoublePoint(-5, 0), new DoubleVector(1, 0));
		DoubleCircle circle = new DoubleCircle(new DoublePoint(0, 0), 3.0);

		List<DoublePoint> res = ray.intersect(circle);
		assertEquals(2, res.size());
		assertEquals(-3.0, res.get(0).x(), EPS);
		assertEquals(0.0, res.get(0).y(), EPS);
		assertEquals(3.0, res.get(1).x(), EPS);
		assertEquals(0.0, res.get(1).y(), EPS);

		DoubleRay rayStartInside = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		List<DoublePoint> res2 = rayStartInside.intersect(circle);
		assertEquals(1, res2.size());
		assertEquals(3.0, res2.get(0).x(), EPS);
	}
}
