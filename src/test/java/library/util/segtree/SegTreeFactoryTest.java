package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SegTreeFactoryTest {

	@Test
	public void testFlipCount() {
		var seg = SegTreeFactory.flip_count(10);
		assertEquals(0, seg.fold(0, 10)[0]);
		assertEquals(10, seg.fold(0, 10)[1]);

		// Flip range [2, 7)
		seg.act(2, 7, true);
		assertEquals(5, seg.fold(0, 10)[0]);
		assertEquals(5, seg.fold(0, 10)[1]);
		assertEquals(5, seg.fold(2, 7)[0]);
		assertEquals(0, seg.fold(2, 7)[1]);

		// Flip range [4, 9)
		seg.act(4, 9, true);
		assertEquals(4, seg.fold(0, 10)[0]);
		assertEquals(6, seg.fold(0, 10)[1]);
	}

	@Test
	public void testCompressedIntervalFlipCount() {
		var seg = SegTreeFactory.compressedInterval_flip_sum();
		seg.registerInterval(10, 20);
		seg.registerInterval(15, 30);
		seg.build((l, r) -> new long[] { 0, r - l });

		assertEquals(0, seg.fold(10, 30)[0]);
		assertEquals(20, seg.fold(10, 30)[1]);

		seg.act(10, 20, true);
		assertEquals(10, seg.fold(10, 30)[0]);
		assertEquals(10, seg.fold(10, 30)[1]);

		seg.act(15, 30, true);
		assertEquals(15, seg.fold(10, 30)[0]);
		assertEquals(5, seg.fold(10, 30)[1]);
	}

	@Test
	public void testCompressedIntervalAddSum() {
		var seg = SegTreeFactory.compressedInterval_addSum();
		seg.registerInterval(0, 100);
		seg.registerInterval(50, 150);
		seg.build((l, r) -> new long[] { 0, r - l });

		seg.act(0, 100, 3L);
		assertEquals(300, seg.fold(0, 100)[0]);
		assertEquals(150, seg.fold(0, 50)[0]);
	}

	@Test
	public void testCompressedIntervalAffineModSum() {
		long mod = 1_000_000_007L;
		var seg = SegTreeFactory.compressedInterval_affineModSum(mod);
		seg.registerInterval(0, 10);
		seg.registerInterval(5, 15);
		seg.build((l, r) -> new long[] { 0, r - l });

		// f(x) = 2x + 3 over [0, 10) (length 10) -> sum = 30
		seg.act(0, 10, new long[] { 3, 2 });
		assertEquals(30, seg.fold(0, 10)[0]);
		assertEquals(15, seg.fold(0, 5)[0]);
	}

	@Test
	public void testCompressedIntervalModAddModSum() {
		long mod = 100L;
		var seg = SegTreeFactory.compressedInterval_modAddModSum(mod);
		seg.registerInterval(0, 10);
		seg.registerInterval(10, 20);
		seg.build((l, r) -> new long[] { 0, r - l });

		seg.act(0, 20, 5L); // 5 * 20 = 100 % 100 = 0 (each leaf has sum 50)
		assertEquals(0, seg.fold(0, 20)[0]);

		seg.act(0, 10, 3L); // 50 + 3 * 10 = 80 % 100 = 80
		assertEquals(80, seg.fold(0, 10)[0]);
	}

	@Test
	public void testCompressedIntervalAddMinMax() {
		var segMin = SegTreeFactory.compressedInterval_addMin();
		segMin.registerInterval(0, 10);
		segMin.registerInterval(10, 20);
		segMin.build((l, r) -> 100L);

		segMin.act(0, 10, -20L);
		assertEquals(80L, segMin.fold(0, 20));

		var segMax = SegTreeFactory.compressedInterval_addMax();
		segMax.registerInterval(0, 10);
		segMax.registerInterval(10, 20);
		segMax.build((l, r) -> 100L);

		segMax.act(10, 20, 50L);
		assertEquals(150L, segMax.fold(0, 20));
	}

	@Test
	public void testCompressedIntervalAddCountMinMax() {
		var seg = SegTreeFactory.compressedInterval_addCountMin();
		seg.registerInterval(0, 10);
		seg.registerInterval(10, 20);
		seg.build((l, r) -> new long[] { 0, r - l });

		seg.act(0, 10, -5L);
		// min is -5, count is 10
		long[] fold = seg.fold(0, 20);
		assertEquals(-5L, fold[0]);
		assertEquals(10L, fold[1]);
	}

	@Test
	public void testCompressedIntervalAssignMinMaxSum() {
		var segSum = SegTreeFactory.compressedInterval_assignSum();
		segSum.registerInterval(0, 10);
		segSum.registerInterval(10, 30);
		segSum.build((l, r) -> new long[] { 0, r - l });

		segSum.act(0, 30, 5L); // assign 5 -> [0, 10) = 50, [10, 30) = 100 -> total 150
		assertEquals(150L, segSum.fold(0, 30)[0]);

		var segMin = SegTreeFactory.compressedInterval_assignMin();
		segMin.registerInterval(0, 10);
		segMin.registerInterval(10, 20);
		segMin.build((l, r) -> 100L);

		segMin.act(0, 10, 20L);
		assertEquals(20L, segMin.fold(0, 20));

		var segMax = SegTreeFactory.compressedInterval_assignMax();
		segMax.registerInterval(0, 10);
		segMax.registerInterval(10, 20);
		segMax.build((l, r) -> 100L);

		segMax.act(10, 20, 200L);
		assertEquals(200L, segMax.fold(0, 20));
	}

	@Test
	public void testCompressedIntervalModMulSum() {
		long mod = 1000L;
		var seg = SegTreeFactory.compressedInterval_modMulSum(mod);
		seg.registerInterval(0, 10);
		seg.build((l, r) -> new long[] { 5, r - l });

		seg.act(0, 10, 3L); // 5 * 3 = 15
		assertEquals(15L, seg.fold(0, 10)[0]);
	}

	@Test
	public void testAssignMax() {
		var seg = SegTreeFactory.assign_max(5);
		seg.set(0, 10L);
		seg.set(1, 20L);
		seg.set(2, 5L);

		assertEquals(20L, seg.fold(0, 3));
	}

	@Test
	public void testLogicalOr() {
		var seg = SegTreeFactory.logicalOr(5);
		assertFalse(seg.fold(0, 5));

		seg.set(2, true);
		assertTrue(seg.fold(0, 5));
		assertFalse(seg.fold(0, 2));
		assertTrue(seg.fold(2, 4));
	}
}
