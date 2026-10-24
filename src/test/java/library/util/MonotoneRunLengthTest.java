package library.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonotoneRunLengthTest {

	@Test
	void testConstantFunction() {
		LongUnaryOperator f = x -> 42L;
		long B = 1_000_000_000_000L;
		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, B);

		assertEquals(1, runs.size());
		MathUtils.Run run = runs.get(0);
		assertEquals(42L, run.value());
		assertEquals(0L, run.l());
		assertEquals(B, run.r());
		assertEquals(B + 1, run.count());
		assertEquals(B + 1, run.length());
	}

	@Test
	void testStrictlyIncreasingFunction() {
		int N = 100;
		LongUnaryOperator f = x -> 3 * x + 5;
		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, N - 1);

		assertEquals(N, runs.size());
		for (int i = 0; i < N; i++) {
			MathUtils.Run run = runs.get(i);
			assertEquals(3L * i + 5, run.value());
			assertEquals(i, run.l());
			assertEquals(i, run.r());
			assertEquals(1, run.count());
		}
	}

	@Test
	void testStrictlyDecreasingFunction() {
		int N = 100;
		LongUnaryOperator f = x -> 1000L - 3 * x;
		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, N - 1);

		assertEquals(N, runs.size());
		for (int i = 0; i < N; i++) {
			MathUtils.Run run = runs.get(i);
			assertEquals(1000L - 3 * i, run.value());
			assertEquals(i, run.l());
			assertEquals(i, run.r());
			assertEquals(1, run.count());
		}
	}

	@Test
	void testStepFunction() {
		// f(x) = x / 10 for 0 <= x <= 99 (non-decreasing)
		LongUnaryOperator f = x -> x / 10;
		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, 99);

		assertEquals(10, runs.size());
		for (int i = 0; i < 10; i++) {
			MathUtils.Run run = runs.get(i);
			assertEquals(i, run.value());
			assertEquals(i * 10, run.l());
			assertEquals(i * 10 + 9, run.r());
			assertEquals(10, run.count());
		}
	}

	@Test
	void testStepNonIncreasingFunction() {
		// f(x) = 9 - x / 10 for 0 <= x <= 99 (non-increasing)
		LongUnaryOperator f = x -> 9 - x / 10;
		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, 99);

		assertEquals(10, runs.size());
		for (int i = 0; i < 10; i++) {
			MathUtils.Run run = runs.get(i);
			assertEquals(9 - i, run.value());
			assertEquals(i * 10, run.l());
			assertEquals(i * 10 + 9, run.r());
			assertEquals(10, run.count());
		}
	}

	@Test
	void testEmptyAndSingleElementDomain() {
		LongUnaryOperator f = x -> x * 2;

		// Empty domain minX > maxX
		List<MathUtils.Run> empty1 = MathUtils.monotoneRunLengthCompress(f, 5, 4);
		assertTrue(empty1.isEmpty());

		// Single element domain
		List<MathUtils.Run> single = MathUtils.monotoneRunLengthCompress(f, 7, 7);
		assertEquals(1, single.size());
		assertEquals(14L, single.get(0).value());
		assertEquals(7L, single.get(0).l());
		assertEquals(7L, single.get(0).r());
		assertEquals(1L, single.get(0).count());
	}

	@Test
	void testLargeCoordinateDomain() {
		long minX = -100_000_000_000L;
		long maxX = 100_000_000_000L;
		// f(x) = floor((x - minX) / 10_000_000_000)
		LongUnaryOperator f = x -> (x - minX) / 10_000_000_000L;

		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, minX, maxX);
		assertEquals(21, runs.size());

		long expectedVal = 0;
		for (MathUtils.Run run : runs) {
			assertEquals(expectedVal, run.value());
			for (long x : new long[]{run.l(), run.r()}) {
				assertEquals(expectedVal, f.applyAsLong(x));
			}
			expectedVal++;
		}
		assertEquals(maxX, runs.get(runs.size() - 1).r());
	}

	@Test
	void testEvaluationCountBound() {
		// Test that total function evaluations is O(A log(B / A))
		long B = 10_000_000L;
		int A = 50; // 51 distinct values
		long stepSize = B / A;

		AtomicLong evalCount = new AtomicLong(0);
		LongUnaryOperator f = x -> {
			evalCount.incrementAndGet();
			return x / stepSize;
		};

		List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(f, 0, B);

		// Number of runs should be ~ A + 1
		assertTrue(runs.size() <= A + 2);

		// Bound check: Total evaluations should be <= 4 * K * log2(B / K + 2)
		int K = runs.size();
		double logTerm = Math.log((double) B / K + 2.0) / Math.log(2.0);
		double upperLimit = 4.0 * K * logTerm + 100;

		assertTrue(evalCount.get() <= upperLimit,
				"Evaluations count " + evalCount.get() + " exceeded expected bound " + upperLimit);
	}

	@Test
	void testMonotoneEqualRange() {
		// Non-decreasing step function f(x) = x / 10 for 0 <= x <= 99
		LongUnaryOperator fInc = x -> x / 10;
		MathUtils.Run rangeInc = MathUtils.monotoneEqualRange(fInc, 0, 99, 3);
		assertNotNull(rangeInc);
		assertEquals(30, rangeInc.l());
		assertEquals(40, rangeInc.r());
		assertEquals(3, rangeInc.value());

		// Value larger than all elements in non-decreasing function -> returns (maxX + 1, maxX + 1)
		MathUtils.Run notFoundInc = MathUtils.monotoneEqualRange(fInc, 0, 99, 15);
		assertNotNull(notFoundInc);
		assertEquals(100, notFoundInc.l());
		assertEquals(100, notFoundInc.r());

		// Value smaller than all elements in non-decreasing function -> returns (minX, minX)
		MathUtils.Run notFoundIncSmall = MathUtils.monotoneEqualRange(fInc, 0, 99, -5);
		assertNotNull(notFoundIncSmall);
		assertEquals(0, notFoundIncSmall.l());
		assertEquals(0, notFoundIncSmall.r());

		// Non-increasing step function f(x) = 9 - x / 10 for 0 <= x <= 99
		LongUnaryOperator fDec = x -> 9 - x / 10;
		MathUtils.Run rangeDec = MathUtils.monotoneEqualRange(fDec, 0, 99, 7);
		assertNotNull(rangeDec);
		assertEquals(20, rangeDec.l());
		assertEquals(30, rangeDec.r());
		assertEquals(7, rangeDec.value());

		// Value smaller than all elements in non-increasing function -> returns (maxX + 1, maxX + 1)
		MathUtils.Run notFoundDec = MathUtils.monotoneEqualRange(fDec, 0, 99, -5);
		assertNotNull(notFoundDec);
		assertEquals(100, notFoundDec.l());
		assertEquals(100, notFoundDec.r());

		// Value larger than all elements in non-increasing function -> returns (minX, minX)
		MathUtils.Run notFoundDecLarge = MathUtils.monotoneEqualRange(fDec, 0, 99, 15);
		assertNotNull(notFoundDecLarge);
		assertEquals(0, notFoundDecLarge.l());
		assertEquals(0, notFoundDecLarge.r());

		// Constant function
		LongUnaryOperator fConst = x -> 100L;
		MathUtils.Run rangeConst = MathUtils.monotoneEqualRange(fConst, 0, 50, 100);
		assertNotNull(rangeConst);
		assertEquals(0, rangeConst.l());
		assertEquals(51, rangeConst.r());
		assertEquals(100, rangeConst.value());

		MathUtils.Run rangeConstSmaller = MathUtils.monotoneEqualRange(fConst, 0, 50, 99);
		assertNotNull(rangeConstSmaller);
		assertEquals(0, rangeConstSmaller.l());
		assertEquals(0, rangeConstSmaller.r());

		MathUtils.Run rangeConstLarger = MathUtils.monotoneEqualRange(fConst, 0, 50, 101);
		assertNotNull(rangeConstLarger);
		assertEquals(51, rangeConstLarger.l());
		assertEquals(51, rangeConstLarger.r());
	}

	@Test
	void testMonotoneEqualRangeAbsentInMiddle() {
		// Non-decreasing function skipping odd numbers: f(x) = 2 * x for 0 <= x <= 5
		// Values: 0, 2, 4, 6, 8, 10
		LongUnaryOperator fInc = x -> 2 * x;
		MathUtils.Run rangeInc = MathUtils.monotoneEqualRange(fInc, 0, 5, 5);
		assertNotNull(rangeInc);
		// x = 3 has f(3) = 6 >= 5, so insertion boundary is l = 3, r = 3
		assertEquals(3, rangeInc.l());
		assertEquals(3, rangeInc.r());

		// Verify conditions for non-decreasing absent:
		// x < 3: f(0)=0<5, f(1)=2<5, f(2)=4<5
		// x >= 3: f(3)=6>5, f(4)=8>5, f(5)=10>5
		for (long x = 0; x < 3; x++) {
			assertTrue(fInc.applyAsLong(x) < 5);
		}
		for (long x = 3; x <= 5; x++) {
			assertTrue(fInc.applyAsLong(x) > 5);
		}

		// Non-increasing function skipping odd numbers: f(x) = 10 - 2 * x for 0 <= x <= 5
		// Values: 10, 8, 6, 4, 2, 0
		LongUnaryOperator fDec = x -> 10 - 2 * x;
		MathUtils.Run rangeDec = MathUtils.monotoneEqualRange(fDec, 0, 5, 5);
		assertNotNull(rangeDec);
		// x = 3 has f(3) = 4 <= 5, so insertion boundary is l = 3, r = 3
		assertEquals(3, rangeDec.l());
		assertEquals(3, rangeDec.r());

		// Verify conditions for non-increasing absent:
		// x < 3: f(0)=10>5, f(1)=8>5, f(2)=6>5
		// x >= 3: f(3)=4<5, f(4)=2<5, f(5)=0<5
		for (long x = 0; x < 3; x++) {
			assertTrue(fDec.applyAsLong(x) > 5);
		}
		for (long x = 3; x <= 5; x++) {
			assertTrue(fDec.applyAsLong(x) < 5);
		}
	}

	@Test
	void testRandomMonotoneFunction() {
		Random rnd = new Random(42);
		for (int t = 0; t < 50; t++) {
			int len = rnd.nextInt(500) + 1;
			long[] a = new long[len];
			long val = rnd.nextInt(10);

			boolean increasing = rnd.nextBoolean();

			for (int i = 0; i < len; i++) {
				if (rnd.nextDouble() < 0.2) {
					if (increasing) {
						val += rnd.nextInt(5) + 1;
					} else {
						val -= rnd.nextInt(5) + 1;
					}
				}
				a[i] = val;
			}

			List<MathUtils.Run> runs = MathUtils.monotoneRunLengthCompress(i -> a[(int) i], 0, len - 1);

			// Reconstruct array from runs and verify
			long[] reconstructed = new long[len];
			long totalLen = 0;
			for (MathUtils.Run run : runs) {
				for (long x = run.l(); x <= run.r(); x++) {
					reconstructed[(int) x] = run.value();
				}
				totalLen += run.count();
			}

			assertEquals(len, totalLen);
			for (int i = 0; i < len; i++) {
				assertEquals(a[i], reconstructed[i]);
			}
		}
	}
}
