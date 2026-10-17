package library.util.geometry;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class IntIsoRectangle2DTest {

	/**
	 * 対角点の順序、値の等価性、頂点の巡回順序を検証する。
	 * 計算量: 時間 O(1)、追加空間 O(1)。
	 */
	// 未テスト
	@Test
	void constructionAndVertices() {
		var r = new IntIsoRectangle2D(5, -3, -2, 7);
		assertEquals(-2, r.xmin());
		assertEquals(-3, r.ymin());
		assertEquals(5, r.xmax());
		assertEquals(7, r.ymax());
		assertEquals(7, r.width());
		assertEquals(10, r.height());
		assertEquals(70, r.area());
		assertFalse(r.isDegenerate());
		assertEquals(new IntPoint(-2, -3), r.min());
		assertEquals(new IntPoint(5, 7), r.max());
		IntPoint[] expected = {new IntPoint(-2, -3), new IntPoint(5, -3),
				new IntPoint(5, 7), new IntPoint(-2, 7)};
		for (int i = -8; i < 8; i++) {
			assertEquals(expected[Math.floorMod(i, 4)], r.vertex(i));
		}
		assertEquals(expected[0], r.vertex(Integer.MIN_VALUE));
		assertEquals(expected[3], r.vertex(Integer.MAX_VALUE));
		var same = new IntIsoRectangle2D(-2, 7, 5, -3);
		assertEquals(r, r);
		assertEquals(r, same);
		assertEquals(r.hashCode(), same.hashCode());
		assertNotEquals(r, new IntIsoRectangle2D(-2, -3, 6, 7));
		assertNotEquals(r, null);
		assertNotEquals(r, new IntPoint(-2, -3));
		assertEquals("IntIsoRectangle2D[(-2,-3),(5,7)]", r.toString());
	}

	/**
	 * 入力点、返却点と矩形の座標が共有されないことを検証する。
	 * 計算量: 時間 O(1)、追加空間 O(1)。
	 */
	// 未テスト
	@Test
	void pointOwnershipAndNullArguments() {
		var p = new IntPoint(4, 6);
		var q = new IntPoint(-1, -2);
		var r = new IntIsoRectangle2D(p, q);
		p.x = 100;
		q.y = 100;
		r.min().x = 100;
		r.max().y = 100;
		r.vertex(1).x = 100;
		assertEquals(new IntIsoRectangle2D(-1, -2, 4, 6), r);
		assertThrows(NullPointerException.class, () -> new IntIsoRectangle2D(null, p));
		assertThrows(NullPointerException.class, () -> new IntIsoRectangle2D(p, null));
		assertThrows(NullPointerException.class, () -> r.contains((IntPoint) null));
		assertThrows(NullPointerException.class, () -> r.contains((IntIsoRectangle2D) null));
		assertThrows(NullPointerException.class, () -> r.boundedSide(null));
		assertThrows(NullPointerException.class, () -> r.isIntersect(null));
		assertThrows(NullPointerException.class, () -> r.intersection(null));
	}

	/**
	 * 内部・辺・頂点・外部および退化した矩形の分類を検証する。
	 * 計算量: 時間 O(1)、追加空間 O(1)。
	 */
	// 未テスト
	@Test
	void boundedSideAndDegeneracy() {
		var r = new IntIsoRectangle2D(-2, -3, 5, 7);
		assertEquals(BoundedSide.ON_BOUNDED_SIDE, r.boundedSide(new IntPoint(0, 0)));
		for (var p : new IntPoint[]{r.min(), r.max(), new IntPoint(-2, 0),
				new IntPoint(5, 0), new IntPoint(0, -3), new IntPoint(0, 7)}) {
			assertTrue(r.contains(p));
			assertEquals(BoundedSide.ON_BOUNDARY, r.boundedSide(p));
		}
		for (var p : new IntPoint[]{new IntPoint(-3, 0), new IntPoint(6, 0),
				new IntPoint(0, -4), new IntPoint(0, 8)}) {
			assertFalse(r.contains(p));
			assertEquals(BoundedSide.ON_UNBOUNDED_SIDE, r.boundedSide(p));
		}
		for (var degenerate : new IntIsoRectangle2D[]{new IntIsoRectangle2D(0, -2, 0, 2),
				new IntIsoRectangle2D(-2, 0, 2, 0), new IntIsoRectangle2D(0, 0, 0, 0)}) {
			assertTrue(degenerate.isDegenerate());
			assertEquals(0, degenerate.area());
			assertEquals(BoundedSide.ON_BOUNDARY, degenerate.boundedSide(0, 0));
			assertEquals(BoundedSide.ON_UNBOUNDED_SIDE, degenerate.boundedSide(1, 1));
		}
	}

	/**
	 * [-1, 1] 内の整数端点による全 R, S について、格子点の列挙で R ∩ S と S ⊆ R を検証する。
	 * この端点集合では非空の閉矩形は必ず格子点を含む。
	 * 計算量: 時間 O(N^2 P)、追加空間 O(N)（N: 矩形数、P: 判定する格子点数）。
	 */
	// 未テスト
	@Test
	void exhaustiveSmallIntersections() {
		var rectangles = new ArrayList<IntIsoRectangle2D>();
		for (int x1 = -1; x1 <= 1; x1++) {
			for (int x2 = x1; x2 <= 1; x2++) {
				for (int y1 = -1; y1 <= 1; y1++) {
					for (int y2 = y1; y2 <= 1; y2++) {
						rectangles.add(new IntIsoRectangle2D(x1, y1, x2, y2));
					}
				}
			}
		}
		for (var r : rectangles) {
			for (var s : rectangles) {
				var intersection = r.intersection(s);
				boolean intersects = false;
				boolean contains = true;
				for (int x = -2; x <= 2; x++) {
					for (int y = -2; y <= 2; y++) {
						boolean inR = r.xmin() <= x && x <= r.xmax() && r.ymin() <= y && y <= r.ymax();
						boolean inS = s.xmin() <= x && x <= s.xmax() && s.ymin() <= y && y <= s.ymax();
						intersects |= inR && inS;
						contains &= !inS || inR;
						assertEquals(inR && inS, intersection != null && intersection.contains(x, y));
					}
				}
				assertEquals(intersects, r.isIntersect(s));
				assertEquals(intersects, intersection != null);
				assertEquals(contains, r.contains(s));
				assertEquals(intersection, s.intersection(r));
				if (intersection != null) {
					assertNotSame(r, intersection);
					assertNotSame(s, intersection);
				}
			}
		}
	}

	/**
	 * int の端点で判定が正しいこと、および幅・高さ・面積のオーバーフローを検証する。
	 * 計算量: 時間 O(1)、追加空間 O(1)。
	 */
	// 未テスト
	@Test
	void extremeCoordinatesAndOverflow() {
		var full = new IntIsoRectangle2D(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
		assertTrue(full.contains(Integer.MIN_VALUE, Integer.MAX_VALUE));
		assertEquals(BoundedSide.ON_BOUNDED_SIDE, full.boundedSide(0, 0));
		assertEquals(BoundedSide.ON_BOUNDARY, full.boundedSide(Integer.MIN_VALUE, 0));
		assertThrows(ArithmeticException.class, full::width);
		assertThrows(ArithmeticException.class, full::height);
		assertThrows(ArithmeticException.class, full::area);
		var left = new IntIsoRectangle2D(Integer.MIN_VALUE, 0, -1, 1);
		var right = new IntIsoRectangle2D(0, 0, Integer.MAX_VALUE, 1);
		assertEquals(Integer.MAX_VALUE, left.area());
		assertEquals(Integer.MAX_VALUE, right.width());
		assertFalse(left.isIntersect(right));
		assertNull(left.intersection(right));
		assertTrue(full.contains(left));
		assertEquals(left, full.intersection(left));
		assertEquals(2L * Integer.MAX_VALUE, new IntIsoRectangle2D(0, 0, Integer.MAX_VALUE, 2).area());
		assertEquals(1L * Integer.MAX_VALUE * Integer.MAX_VALUE,
				new IntIsoRectangle2D(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE).area());
		assertEquals(4_294_967_295L,
				new IntIsoRectangle2D(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 1).area());
		assertEquals(4_294_967_295L,
				new IntIsoRectangle2D(0, Integer.MIN_VALUE, 1, Integer.MAX_VALUE).area());
		assertEquals(Long.MAX_VALUE - Integer.MAX_VALUE,
				new IntIsoRectangle2D(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, 0).area());
		assertEquals(0, new IntIsoRectangle2D(Integer.MIN_VALUE, 0, Integer.MAX_VALUE, 0).area());
		assertEquals(0, new IntIsoRectangle2D(0, Integer.MIN_VALUE, 0, Integer.MAX_VALUE).area());
	}
}
