package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class F2VectorSpaceTest {

    private static BitSet toBitSet(int[] a) {
        BitSet bs = new BitSet(a.length);
        for (int i = 0; i < a.length; i++) {
            if (a[i] != 0) bs.set(i);
        }
        return bs;
    }

    @Test
    public void testF2VectorSpaceGenerators() {
        int n = 5;
        int k = 3;
        F2BlockFlipsVectorSpace space = F2VectorSpaceFactory.ofBlockFlips(k);
        assertEquals(3, space.getK());
        List<BitSet> gens = space.generators(n);

        assertEquals(3, gens.size());
        assertEquals(toBitSet(new int[]{1, 1, 1, 0, 0}), gens.get(0));
        assertEquals(toBitSet(new int[]{0, 1, 1, 1, 0}), gens.get(1));
        assertEquals(toBitSet(new int[]{0, 0, 1, 1, 1}), gens.get(2));
    }

    @Test
    public void testF2VectorSpaceBlockFlipsK2() {
        int k = 2;
        int n = 6;
        F2VectorSpace space = F2VectorSpaceFactory.ofBlockFlips(k);

        BitSet start = toBitSet(new int[]{0, 0, 0, 0, 0, 0});
        BitSet target = toBitSet(new int[]{1, 0, 1, 1, 0, 1});

        assertTrue(space.areEquivalent(start, target, n));
        assertEquals(6, space.distance(start, target, n));

        // Compare distance against BFS using generators
        Map<BitSet, Integer> dists = bfsDistances(space, start, n, k);
        for (var entry : dists.entrySet()) {
            assertEquals((int) entry.getValue(), space.distance(start, entry.getKey(), n), "Dist from start to " + entry.getKey());
        }
    }

    @Test
    public void testF2VectorSpaceBlockFlipsK2Small() {
        int k = 2;
        int n = 4;
        F2VectorSpace space = new F2BlockFlipsVectorSpace(k);
        BitSet start = toBitSet(new int[]{1, 1, 0, 0});
        BitSet target = toBitSet(new int[]{1, 0, 0, 1});
        assertEquals(2, space.distance(start, target, n));
    }

    @Test
    public void testF2VectorSpaceBlockFlipsK1() {
        int k = 1;
        int n = 5;
        F2VectorSpace space = F2VectorSpaceFactory.ofBlockFlips(k);
        BitSet start = toBitSet(new int[]{0, 1, 0, 1, 0});
        BitSet target = toBitSet(new int[]{1, 1, 0, 0, 1});
        assertEquals(3, space.distance(start, target, n));
    }

    @Test
    public void testReduceAndAreEquivalent() {
        int k = 3;
        int n = 5;
        F2VectorSpace space = F2VectorSpaceFactory.ofBlockFlips(k);
        BitSet v = toBitSet(new int[]{0, 1, 1, 1, 0});
        BitSet reduced = space.reduce(v, n);
        assertEquals(toBitSet(new int[]{0, 0}), reduced);

        BitSet u = toBitSet(new int[]{0, 0, 0, 0, 0});
        assertTrue(space.areEquivalent(u, v, n));
    }

    private static Map<BitSet, Integer> bfsDistances(F2VectorSpace space, BitSet start, int n, int k) {
        List<BitSet> gens = space.generators(n);
        Map<BitSet, Integer> dist = new HashMap<>();
        ArrayDeque<BitSet> queue = new ArrayDeque<>();
        dist.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            BitSet u = queue.poll();
            int d = dist.get(u);
            for (int i = 0; i <= n - k; i++) {
                boolean allSame = true;
                boolean firstBit = u.get(i);
                for (int j = 1; j < k; j++) {
                    if (u.get(i + j) != firstBit) {
                        allSame = false;
                        break;
                    }
                }
                if (allSame) {
                    BitSet next = (BitSet) u.clone();
                    next.xor(gens.get(i));
                    if (!dist.containsKey(next)) {
                        dist.put(next, d + 1);
                        queue.add(next);
                    }
                }
            }
        }
        return dist;
    }
}
