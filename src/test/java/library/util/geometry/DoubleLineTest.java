package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class DoubleLineTest {
	private static final double EPS = 1e-9;

	@Test
	public void intersectCircleAtTwoPoints() {
		DoubleLine line = new DoubleLine(0, 1, 0);
		DoubleCircle circle = new DoubleCircle(new DoublePoint(1, 0), 2);

		List<DoublePoint> intersections = line.intersect(circle);

		assertEquals(2, intersections.size());
		assertTrue(contains(intersections, new DoublePoint(-1, 0)));
		assertTrue(contains(intersections, new DoublePoint(3, 0)));
	}

	@Test
	public void intersectCircleAtTangentPoint() {
		DoubleLine line = new DoubleLine(0, 1, -2);
		DoubleCircle circle = new DoubleCircle(new DoublePoint(1, 0), 2);

		List<DoublePoint> intersections = line.intersect(circle);

		assertEquals(1, intersections.size());
		assertEquals(1, intersections.get(0).x(), EPS);
		assertEquals(2, intersections.get(0).y(), EPS);
	}

	@Test
	public void intersectCircleReturnsEmptyListWhenDisjoint() {
		DoubleLine line = new DoubleLine(0, 1, -3);
		DoubleCircle circle = new DoubleCircle(new DoublePoint(1, 0), 2);

		assertTrue(line.intersect(circle).isEmpty());
	}

	private static boolean contains(List<DoublePoint> points, DoublePoint expected) {
		return points.stream().anyMatch(point ->
				Math.abs(point.x() - expected.x()) <= EPS
				&& Math.abs(point.y() - expected.y()) <= EPS);
	}
}
