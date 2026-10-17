package library.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class KnapsackTest {
	@Test
	void knapsack01KeepsNegativeInfinityForUnreachableWeights() {
		assertArrayEquals(
				new long[] {0, Long.MIN_VALUE, 5, Long.MIN_VALUE},
				Knapsack.knapsack01(new int[] {2}, new long[] {5}, 3));
	}

	@Test
	void knapsack01ComputesReachableWeightsAcrossWeightGroups() {
		assertArrayEquals(
				new long[] {0, Long.MIN_VALUE, 5, 8, Long.MIN_VALUE, 13, 15, Long.MIN_VALUE},
				Knapsack.knapsack01(new int[] {2, 3, 3}, new long[] {5, 7, 8}, 7));
	}
}
