package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MinSegArgMinTest {

	@Test
	public void testArgMinReturnsFirstMinimumInHalfOpenRange() {
		MinSeg seg = new MinSeg(6);
		seg.build(new long[] { 4, 1, 2, 1, 7, -3 });

		assertEquals(1, seg.argMin(0, 6));
		assertEquals(3, seg.argMin(2, 5));
		assertEquals(5, seg.argMin(4, 6));
	}
}
