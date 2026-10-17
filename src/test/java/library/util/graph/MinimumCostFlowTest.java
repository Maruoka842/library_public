package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class MinimumCostFlowTest {

	@Test
	public void testMinCostFlowAtMostWithSupplyDemand() {
		MinimumCostFlow mcf = new MinimumCostFlow(4);
		mcf.addEdge(0, 1, 2, 5);
		mcf.addEdge(1, 3, 1, 5);
		mcf.addEdge(0, 2, 4, 5);
		mcf.addEdge(2, 3, 1, 5);

		// Supply 3 at vertex 0, demand 3 at vertex 3
		mcf.addExcess(0, 3);
		mcf.addExcess(3, -3);

		// Path 0->1->3 cost is 3 per unit flow, Path 0->2->3 cost is 5 per unit flow
		// Cap at most 5: flow 3 along 0->1->3, cost 3 * 3 = 9
		Long cost = mcf.minCostFlowAtMost(0, 3, 5);
		assertNotNull(cost);
		assertEquals(9, cost);
	}

	@Test
	public void testMinCostFlowAtMostWithNegativeCostEdge() {
		MinimumCostFlow mcf = new MinimumCostFlow(4);
		// Negative cost edge 0->1 with cost -10, capacity 2
		mcf.addEdge(0, 1, -10, 2);
		mcf.addEdge(1, 3, 3, 5);

		// minCostFlowAtMost adds edge 3->0 with cost 0, cap 2
		// Circulation pushes 2 flow along 0->1->3->0, total cost = (-10 + 3) * 2 = -14
		Long cost = mcf.minCostFlowAtMost(0, 3, 2);
		assertNotNull(cost);
		assertEquals(-14, cost);
	}

	@Test
	public void testMinCostFlowOnDagAtMostWithSupplyDemand() {
		MinimumCostFlow mcf = new MinimumCostFlow(4);
		mcf.addEdge(0, 1, 10, 3);
		mcf.addEdge(1, 3, 5, 3);
		mcf.addEdge(0, 2, 2, 3);
		mcf.addEdge(2, 3, 3, 3);

		// Supply 4 at vertex 0, demand 4 at vertex 3
		mcf.addExcess(0, 4);
		mcf.addExcess(3, -4);

		// Path 0->2->3 has cost 5, cap 3
		// Path 0->1->3 has cost 15, cap 3
		// Flow 4: 3 along 0->2->3 (cost 15) + 1 along 0->1->3 (cost 15) = 30
		Long cost = mcf.minCostFlowOnDagAtMost(0, 3, 5);
		assertNotNull(cost);
		assertEquals(30, cost);
	}

	@Test
	public void testInfeasibleFlow() {
		MinimumCostFlow mcf = new MinimumCostFlow(3);
		mcf.addExcess(0, 5);
		mcf.addExcess(2, -10); // excess sum is negative / imbalance, no path from supply to demand

		Long cost = mcf.minCostFlowAtMost(0, 2, 5);
		assertNull(cost);
	}
}
