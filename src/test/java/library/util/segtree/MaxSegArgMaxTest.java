package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MaxSegArgMaxTest {

	@Test
	public void testArgMaxReturnsFirstMaximumInHalfOpenRange() {
		MaxSeg seg = new MaxSeg(6);
		seg.build(new long[] { 4, 9, 2, 9, 1, 7 });

		assertEquals(1, seg.argMax(0, 6));
		assertEquals(3, seg.argMax(2, 5));
		assertEquals(5, seg.argMax(4, 6));
	}
}
