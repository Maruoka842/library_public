package library.util.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeometryUtilsIntersectionTest {

	private static final double EPS = 1e-7;

	@Test
	public void testLineLine() {
		DoubleLine l1 = new DoubleLine(1, 0, -2); // x = 2
		DoubleLine l2 = new DoubleLine(0, 1, -3); // y = 3

		DoublePoint res = GeometryUtils.intersect(l1, l2);
		assertNotNull(res);
		assertEquals(2.0, res.x(), EPS);
		assertEquals(3.0, res.y(), EPS);
	}

	@Test
	public void testLineRayAndRayLine() {
		DoubleLine line = new DoubleLine(1, 0, -5); // x = 5
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));

		DoublePoint res1 = GeometryUtils.intersect(line, ray);
		DoublePoint res2 = GeometryUtils.intersect(ray, line);

		assertNotNull(res1);
		assertEquals(res1, res2);
		assertEquals(5.0, res1.x(), EPS);
		assertEquals(0.0, res1.y(), EPS);
	}

	@Test
	public void testLineSegmentAndSegmentLine() {
		DoubleLine line = new DoubleLine(0, 1, -2); // y = 2
		DoubleSegment seg = new DoubleSegment(1, 0, 1, 4);

		DoublePoint res1 = GeometryUtils.intersect(line, seg);
		DoublePoint res2 = GeometryUtils.intersect(seg, line);

		assertNotNull(res1);
		assertEquals(res1, res2);
		assertEquals(1.0, res1.x(), EPS);
		assertEquals(2.0, res1.y(), EPS);
	}

	@Test
	public void testLineCircleAndCircleLine() {
		DoubleLine line = new DoubleLine(0, 1, 0); // y = 0
		DoubleCircle circle = new DoubleCircle(new DoublePoint(0, 0), 2.0);

		List<DoublePoint> res1 = GeometryUtils.intersect(line, circle);
		List<DoublePoint> res2 = GeometryUtils.intersect(circle, line);

		assertEquals(2, res1.size());
		assertEquals(res1, res2);
	}

	@Test
	public void testRayRay() {
		DoubleRay r1 = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 1));
		DoubleRay r2 = new DoubleRay(new DoublePoint(0, 2), new DoubleVector(1, -1));

		DoublePoint res = GeometryUtils.intersect(r1, r2);
		assertNotNull(res);
		assertEquals(1.0, res.x(), EPS);
		assertEquals(1.0, res.y(), EPS);
	}

	@Test
	public void testRaySegmentAndSegmentRay() {
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		DoubleSegment seg = new DoubleSegment(4, -1, 4, 1);

		DoublePoint res1 = GeometryUtils.intersect(ray, seg);
		DoublePoint res2 = GeometryUtils.intersect(seg, ray);

		assertNotNull(res1);
		assertEquals(res1, res2);
		assertEquals(4.0, res1.x(), EPS);
		assertEquals(0.0, res1.y(), EPS);
	}

	@Test
	public void testRayCircleAndCircleRay() {
		DoubleRay ray = new DoubleRay(new DoublePoint(0, 0), new DoubleVector(1, 0));
		DoubleCircle circle = new DoubleCircle(new DoublePoint(5, 0), 2.0);

		List<DoublePoint> res1 = GeometryUtils.intersect(ray, circle);
		List<DoublePoint> res2 = GeometryUtils.intersect(circle, ray);

		assertEquals(2, res1.size());
		assertEquals(res1, res2);
		assertEquals(3.0, res1.get(0).x(), EPS);
		assertEquals(7.0, res1.get(1).x(), EPS);
	}

	@Test
	public void testSegmentSegment() {
		DoubleSegment s1 = new DoubleSegment(0, 0, 4, 4);
		DoubleSegment s2 = new DoubleSegment(0, 4, 4, 0);

		DoublePoint res = GeometryUtils.intersect(s1, s2);
		assertNotNull(res);
		assertEquals(2.0, res.x(), EPS);
		assertEquals(2.0, res.y(), EPS);
	}

	@Test
	public void testSegmentCircleAndCircleSegment() {
		DoubleSegment seg = new DoubleSegment(-3, 0, 3, 0);
		DoubleCircle circle = new DoubleCircle(new DoublePoint(0, 0), 2.0);

		List<DoublePoint> res1 = GeometryUtils.intersect(seg, circle);
		List<DoublePoint> res2 = GeometryUtils.intersect(circle, seg);

		assertEquals(2, res1.size());
		assertEquals(res1, res2);
	}

	@Test
	public void testCircleCircle() {
		DoubleCircle c1 = new DoubleCircle(new DoublePoint(0, 0), 2.0);
		DoubleCircle c2 = new DoubleCircle(new DoublePoint(2, 0), 2.0);

		List<GeometricObject> res = GeometryUtils.intersect(c1, c2);
		assertEquals(2, res.size());
	}
}
