package library.util.graph.tree;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MinSubtreeSumAncestorThresholdCoverTest {

    @Test
    public void testSingleNode() {
        // N = 1
        Tree tree = new Tree(1);
        tree.rooted(0);

        long[] requirement = {10L};
        long[] unitCost = {3L};
        // ability[v][k]:
        // k=0 -> 5 (not enough)
        // k=1 -> 12 (enough)
        long[][] ability = {
            {5L, 12L}
        };

        long cost = TreedpFactory.minSubtreeSumAncestorCover(tree, requirement, unitCost, ability);
        // We need 1 unit of subtree sum at root 0 to have ability >= 10. Cost = 1 * 3 = 3.
        assertEquals(3L, cost);
    }

    @Test
    public void testTwoNodes() {
        // N = 2
        Tree tree = new Tree(2);
        tree.addEdge(0, 1);
        tree.rooted(0); // 0 is root, 1 is child

        long[] requirement = {10L, 20L};
        long[] unitCost = {5L, 2L}; // unit cost at child is 2, at root is 5

        // ability[v][k]
        long[][] ability = {
            {5L, 15L, 25L}, // vertex 0: k=0:5, k=1:15, k=2:25
            {8L, 22L, 30L}  // vertex 1: k=0:8, k=1:22, k=2:30
        };

        long cost = TreedpFactory.minSubtreeSumAncestorCover(tree, requirement, unitCost, ability);
        // Let's analyze:
        // Vertex 1 requirement is 20. It can be covered by:
        // - placing x_1 = 1, so S_1 = 1. ability[1][1] = 22 >= 20. Cost = 1 * 2 = 2.
        // - or ancestor 0 covering it. For ancestor 0 to cover, say S_0 = 1.
        //   Wait, but the ancestral coverage means for each t, there is SOME ancestor v with ability[v][S_v] >= requirement[t].
        //   So we don't necessarily need ability[1][S_1] >= 20.
        //   If we set x_1 = 1, x_0 = 0: S_1 = 1, S_0 = 1.
        //   For t = 1, ancestor 1 has ability[1][1] = 22 >= 20 (covered).
        //   For t = 0, ancestor 0 has ability[0][1] = 15 >= 10 (covered).
        //   Total cost = 1 * unitCost[1] = 2.
        // Let's see if x_1 = 0, x_0 = 1 works:
        //   S_1 = 0, S_0 = 1.
        //   For t = 1: ancestor 1 has ability[1][0] = 8 < 20. Ancestor 0 has ability[0][1] = 15 < 20. (Not covered!)
        //   So we must have ability[v][S_v] >= 20 for some v in {0, 1}.
        //   If v = 1 covers t = 1: we need S_1 >= 1. S_1 = 1 can cover both t=1 (ability[1][1]=22>=20) and t=0 (ability[0][1]=15>=10). Cost = 2.
        //   If v = 0 covers t = 1: we need S_0 >= 2 (ability[0][2]=25>=20).
        //   With S_0 >= 2, we can set x_1 = 2 (Cost = 4) or x_1=1, x_0=1 (Cost = 7) or x_0=2 (Cost = 10).
        //   So the minimum cost should be 2.
        assertEquals(2, cost);
    }

    @Test
    public void testAssertionIfTreeNotRooted() {
        Tree tree = new Tree(2);
        tree.addEdge(0, 1);
        // not rooted!

        long[] requirement = {10L, 20L};
        long[] unitCost = {5L, 2L};
        long[][] ability = {
            {5L, 15L, 25L},
            {8L, 22L, 30L}
        };

        assertThrows(AssertionError.class, () -> {
            TreedpFactory.minSubtreeSumAncestorCover(tree, requirement, unitCost, ability);
        });
    }

    @Test
    public void testIllegalArgumentExceptionOnInvalidLengths() {
        Tree tree = new Tree(2);
        tree.addEdge(0, 1);
        tree.rooted(0);

        long[] requirement = {10L, 20L};
        long[] unitCost = {5L}; // invalid size
        long[][] ability = {
            {5L, 15L, 25L},
            {8L, 22L, 30L}
        };

        assertThrows(IllegalArgumentException.class, () -> {
            TreedpFactory.minSubtreeSumAncestorCover(tree, requirement, unitCost, ability);
        });
    }
}
