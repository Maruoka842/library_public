package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

public class GeometryUtilsPolarAngleComparatorTest {
	@Test
	public void sortsDoublePointsByPolarAngle() {
		List<DoublePoint> points = new ArrayList<>(List.of(
				new DoublePoint(-1, 0),
				new DoublePoint(0, 1),
				new DoublePoint(1, 0),
				new DoublePoint(-1, -1),
				new DoublePoint(0, -1)));

		points.sort(GeometryUtils.PolarAngleComparatorForDoublePoint());

		assertEquals(List.of(
				new DoublePoint(-1, -1),
				new DoublePoint(0, -1),
				new DoublePoint(1, 0),
				new DoublePoint(0, 1),
				new DoublePoint(-1, 0)), points);
	}

	@Test
	public void treatsPointsOnSameRayAsEqual() {
		Comparator<DoublePoint> comparator =
				GeometryUtils.PolarAngleComparatorForDoublePoint();

		assertEquals(0, comparator.compare(
				new DoublePoint(1.5, 2.5), new DoublePoint(3, 5)));
		assertEquals(0, comparator.compare(
				DoublePoint.ORIGIN, new DoublePoint(2, 0)));
	}
}
