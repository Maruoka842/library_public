package library.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import library.util.geometry.CoordinateOrderedSet2D;
import library.util.geometry.IncrementalConvexHull;
import library.util.geometry.MonotonicConvexHullTrick;
import library.util.unionfind.UnionFindSet;

class HighPriorityClearMethodTest {

	@Test
	void incrementalPrefixMinCanBeReusedAfterClear() {
		IncrementalPrefixMin<Integer, Integer> prefixMin = new IncrementalPrefixMin<>();
		prefixMin.put(1, 5);
		prefixMin.put(3, 2);

		prefixMin.clear();

		assertNull(prefixMin.floorEntry(3));
		prefixMin.put(2, 7);
		assertEquals(7, prefixMin.floorEntry(2).getValue());
	}

	@Test
	void geometryStructuresCanBeReusedAfterClear() {
		MonotonicConvexHullTrick cht = new MonotonicConvexHullTrick();
		cht.add(10, 1);
		cht.clear();
		assertEquals(0, cht.size());
		cht.add(-10, 3);
		assertEquals(1, cht.size());

		IncrementalConvexHull hull = new IncrementalConvexHull();
		hull.add(1, 2);
		hull.add(3, 4);
		hull.clear();
		assertEquals(0, hull.size());
		hull.add(-1, -2);
		assertEquals(1, hull.size());

		CoordinateOrderedSet2D points = new CoordinateOrderedSet2D();
		points.add(1, 2);
		points.add(1, 3);
		points.clear();
		assertTrue(points.isEmpty());
		assertFalse(points.contains(1, 2));
		points.add(4, 5);
		assertEquals(1, points.size());
	}

	@Test
	void unionFindSetCanBeReusedAfterClear() {
		UnionFindSet<Integer> unionFind = new UnionFindSet<>();
		unionFind.union(1, 2);
		assertTrue(unionFind.equiv(1, 2));

		unionFind.clear();

		assertFalse(unionFind.equiv(1, 2));
		unionFind.union(2, 3);
		assertTrue(unionFind.equiv(2, 3));
	}
}
