package library.util.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import library.util.graph.tree.DoubleValueForest;
import library.util.graph.tree.Forest;
import library.util.graph.tree.LongValueForest;
import library.util.graph.tree.LongValueTree;

public class GraphCopyVariantsTest {

	@Test
	public void testForestCopyPreservesRootsAndIndependence() {
		Forest forest = new Forest(new int[] {1, -1, 3, -1});

		Forest copy = forest.copy();

		for (int i = 0; i < forest.N; i++) {
			assertEquals(forest.isRoot(i), copy.isRoot(i));
			assertNotSame(forest.adj[i], copy.adj[i]);
		}
		copy.addEdge(0, 2);
		assertEquals(2, forest.M);
		assertEquals(3, copy.M);
	}

	@Test
	public void testDagCopiesPreserveTheirTypesAndOrders() {
		DAG dag = DAG.ofNaturalOrder(3);
		dag.addEdge(0, 1);
		dag.addEdge(1, 2);
		int[] dagOrder = dag.topologicalOrder();
		DAG dagCopy = dag.copy();
		assertArrayEquals(dagOrder, dagCopy.topologicalOrder());
		assertNotSame(dagOrder, dagCopy.topologicalOrder());

		LongValueDAG longDag = LongValueDAG.ofNaturalReverseOrder(3);
		longDag.addEdge(2, 1, 7);
		longDag.addEdge(1, 0, 11);
		LongValueDAG longCopy = longDag.copy();
		assertArrayEquals(longDag.topologicalOrder(), longCopy.topologicalOrder());
		assertNotSame(longDag.topologicalOrder(), longCopy.topologicalOrder());
		longCopy.adj[2].get(0).cost = 100;
		assertEquals(7, longDag.adj[2].get(0).cost);

		DoubleValueDAG doubleDag = DoubleValueDAG.ofNaturalOrder(2);
		doubleDag.addEdge(0, 1, 1.5);
		DoubleValueDAG doubleCopy = doubleDag.copy();
		assertArrayEquals(doubleDag.topologicalOrder(), doubleCopy.topologicalOrder());
		doubleCopy.adj[0].get(0).cost = 2.5;
		assertEquals(1.5, doubleDag.adj[0].get(0).cost);
	}

	@Test
	public void testDoubleGraphCopies() {
		DoubleValueGraph graph = new DoubleValueGraph(2);
		graph.addEdge(0, 1, 3.5);
		DoubleValueGraph copy = graph.copy();
		assertNotSame(graph.adj[0], copy.adj[0]);
		assertNotSame(graph.adj[0].get(0), copy.adj[0].get(0));
		copy.adj[0].get(0).cost = 7.0;
		assertEquals(3.5, graph.adj[0].get(0).cost);

		DoubleValueDigraph digraph = new DoubleValueDigraph(2);
		digraph.addEdge(0, 1, 2.5);
		DoubleValueDigraph digraphCopy = digraph.copy();
		assertNotSame(digraph.adj[0], digraphCopy.adj[0]);
		assertNotSame(digraph.adj[0].get(0), digraphCopy.adj[0].get(0));
	}

	@Test
	public void testWeightedForestAndTreeCopies() {
		LongValueForest forest = new LongValueForest(4);
		forest.addEdge(0, 1, 3);
		forest.addEdge(2, 3, 5);
		forest.rooted(1);
		LongValueForest forestCopy = forest.copy();
		for (int i = 0; i < forest.N; i++) {
			assertEquals(forest.isRoot(i), forestCopy.isRoot(i));
		}
		forestCopy.adj[0].get(0).cost = 99;
		assertEquals(3, forest.adj[0].get(0).cost);

		LongValueTree tree = new LongValueTree(3);
		tree.addEdge(0, 1, 2);
		tree.addEdge(1, 2, 4);
		tree.rooted(2);
		LongValueTree treeCopy = tree.copy();
		assertEquals(2, treeCopy.root());
		treeCopy.adj[0].get(0).cost = 10;
		assertEquals(2, tree.adj[0].get(0).cost);

		DoubleValueForest doubleForest = new DoubleValueForest(2);
		doubleForest.addEdge(0, 1, 0.5);
		DoubleValueForest doubleCopy = doubleForest.copy();
		doubleCopy.adj[0].get(0).cost = 1.5;
		assertEquals(0.5, doubleForest.adj[0].get(0).cost);
	}

	@Test
	public void testGenericValueGraphCopiesOnlyShareWeights() {
		StringBuilder weight = new StringBuilder("w");
		ValueGraph<StringBuilder> graph = new ValueGraph<>(2);
		graph.addEdge(0, 1, weight);
		ValueGraph<StringBuilder> copy = graph.copy();
		assertNotSame(graph.adj[0], copy.adj[0]);
		assertNotSame(graph.adj[0].get(0), copy.adj[0].get(0));
		assertSame(weight, copy.adj[0].get(0).weight());

		ValueDigraph<StringBuilder> digraph = new ValueDigraph<>(2);
		digraph.addEdge(0, 1, weight);
		ValueDigraph<StringBuilder> digraphCopy = digraph.copy();
		assertNotSame(digraph.adj[0], digraphCopy.adj[0]);
		assertNotSame(digraph.adj[0].get(0), digraphCopy.adj[0].get(0));
		assertSame(weight, digraphCopy.adj[0].get(0).weight());
	}

	@Test
	public void testStaticAndFunctionalGraphCopies() {
		StaticGraph graph = new StaticGraph(3);
		graph.addEdge(0, 1);
		graph.addEdge(1, 2);
		graph.build();
		StaticGraph copy = graph.copy();
		assertNotSame(graph, copy);
		assertEquals(graph.deg(1), copy.deg(1));

		StaticDigraph digraph = new StaticDigraph(3, List.of(new int[] {0, 1}, new int[] {1, 2}));
		StaticDigraph digraphCopy = digraph.copy();
		assertArrayEquals(digraph.to, digraphCopy.to);
		assertNotSame(digraph.to, digraphCopy.to);

		FunctionalGraph functional = new FunctionalGraph(new int[] {1, 2, 0});
		functional.buildDoubling(4);
		FunctionalGraph functionalCopy = functional.copy();
		assertArrayEquals(functional.parent, functionalCopy.parent);
		assertNotSame(functional.parent, functionalCopy.parent);
		assertNotSame(functional.parentOf2powers, functionalCopy.parentOf2powers);
		assertNotSame(functional.parentOf2powers[0], functionalCopy.parentOf2powers[0]);

		LongValueFunctionalGraph longFunctional = new LongValueFunctionalGraph(new int[] {1, 0}, 3);
		longFunctional.buildDoublingOfData(new long[] {4, 6}, Long::sum);
		LongValueFunctionalGraph longFunctionalCopy = longFunctional.copy();
		assertNotSame(longFunctional.parent, longFunctionalCopy.parent);
		assertNotSame(longFunctional.data, longFunctionalCopy.data);
		assertArrayEquals(longFunctional.data, longFunctionalCopy.data);
	}

	@Test
	public void testBlockCutForestCopyPreservesTypeAndStructure() {
		Graph graph = new Graph(4);
		graph.addEdge(0, 1);
		graph.addEdge(1, 2);
		graph.addEdge(2, 0);
		graph.addEdge(2, 3);
		BlockCutForest forest = new BlockCutForest(graph);
		forest.rooted(0);

		BlockCutForest copy = forest.copy();

		assertNotSame(forest.g, copy.g);
		assertEquals(forest.N, copy.N);
		assertEquals(forest.M, copy.M);
		assertEquals(forest.numberOfConnectedComponents(), copy.numberOfConnectedComponents());
		for (int i = 0; i < forest.N; i++) {
			assertArrayEquals(forest.adj[i].toArray(), copy.adj[i].toArray());
			assertNotSame(forest.adj[i], copy.adj[i]);
		}
	}
}
