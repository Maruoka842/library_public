package library.util.segtree;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CompressedIntervalLazySegTreeTest {

	// Node representation for bit flips on intervals
	static record BitNode(long ones, long len) {}

	@Test
	public void testProblemBitFlipScenario() {
		// Problem scenario: string of length 10^18, initialized to all 0s.
		// Queries: 1 l r (flip bits in [l, r]), 2 l r (count 1s in [l, r])
		CompressedIntervalLazySegTree<Boolean, BitNode> seg = new CompressedIntervalLazySegTree<>(
				(f1, f2) -> f1 ^ f2,
				(a, b) -> new BitNode(a.ones + b.ones, a.len + b.len),
				(flip, x) -> flip ? new BitNode(x.len - x.ones, x.len) : x,
				new BitNode(0, 0),
				false
		);

		// Sample queries (1-indexed inclusive [l, r], converted to half-open [l, r+1))
		// Query 1: flip [3, 8)
		// Query 2: flip [5, 10)
		// Query 3: count 1s in [1, 12)
		seg.registerInterval(3, 8);
		seg.registerInterval(5, 10);
		seg.registerInterval(1, 12);

		// Build with initial leaf value: (ones: 0, len: r - l)
		seg.build((l, r) -> new BitNode(0, r - l));

		// Flip [3, 8) -> bits 3,4,5,6,7 become 1 (count: 5)
		seg.act(3, 8, true);
		assertEquals(5, seg.fold(1, 12).ones());
		assertEquals(5, seg.fold(3, 8).ones());

		// Flip [5, 10) -> bits 3,4 remain 1, bits 5,6,7 flip back to 0, bits 8,9 become 1 (count: 4)
		seg.act(5, 10, true);
		assertEquals(4, seg.fold(1, 12).ones());
		assertEquals(2, seg.fold(3, 8).ones()); // 3, 4 are 1s
		assertEquals(2, seg.fold(8, 10).ones()); // 8, 9 are 1s

		// Clear and verify reset to all 0s
		seg.clear();
		assertEquals(0, seg.fold(1, 12).ones());
	}

	static record AddSumNode(long sum, long len) {}

	@Test
	public void testRangeAddAndRangeSum() {
		CompressedIntervalLazySegTree<Long, AddSumNode> seg = new CompressedIntervalLazySegTree<>(
				Long::sum,
				(a, b) -> new AddSumNode(a.sum + b.sum, a.len + b.len),
				(add, x) -> new AddSumNode(x.sum + add * x.len, x.len),
				new AddSumNode(0, 0),
				0L
		);

		seg.registerInterval(10, 20);
		seg.registerInterval(15, 30);
		seg.registerInterval(25, 40);

		// Initialized with all 0s
		seg.build((l, r) -> new AddSumNode(0, r - l));

		// Add 5 to range [10, 30) (length 20) -> sum = 100
		seg.act(10, 30, 5L);
		assertEquals(100, seg.fold(10, 30).sum());
		assertEquals(25, seg.fold(10, 15).sum()); // 5 * 5
		assertEquals(50, seg.fold(15, 25).sum()); // 5 * 10
		assertEquals(25, seg.fold(25, 30).sum()); // 5 * 5
		assertEquals(0, seg.fold(30, 40).sum());

		// Single element / point get query: [10, 15) has length 5, value = 5 * 5 = 25
		assertEquals(25, seg.get(12).sum());
	}

	@Test
	public void testMaximalRightAndMinimalLeft() {
		// Interval add-sum segtree over [0, 100)
		CompressedIntervalLazySegTree<Long, AddSumNode> seg = new CompressedIntervalLazySegTree<>(
				Long::sum,
				(a, b) -> new AddSumNode(a.sum + b.sum, a.len + b.len),
				(add, x) -> new AddSumNode(x.sum + add * x.len, x.len),
				new AddSumNode(0, 0),
				0L
		);

		seg.registerInterval(0, 10);
		seg.registerInterval(10, 20);
		seg.registerInterval(20, 30);

		seg.build((l, r) -> new AddSumNode(0, r - l));

		seg.act(0, 10, 2L);  // sum in [0, 10) = 20
		seg.act(10, 20, 3L); // sum in [10, 20) = 30
		seg.act(20, 30, 4L); // sum in [20, 30) = 40

		// maximalRight from 0 where sum <= 50 -> [0, 20) sum is 50
		long rBoundary = seg.maximalRight(0, node -> node.sum() <= 50);
		assertEquals(20, rBoundary);

		// minimalLeft from 30 where sum <= 70 -> [10, 30) sum is 70
		long lBoundary = seg.minimalLeft(30, node -> node.sum() <= 70);
		assertEquals(10, lBoundary);

		// Test dump does not throw exception
		seg.dump();
	}
}
