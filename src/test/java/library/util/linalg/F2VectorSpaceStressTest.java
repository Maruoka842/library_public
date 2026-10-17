package library.util.linalg;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class F2VectorSpaceStressTest {

    @Test
    public void stressTest() {
        Random rnd = new Random(42);
        for (int n = 1; n <= 8; n++) {
            for (int k = 1; k <= n; k++) {
                F2BlockFlipsVectorSpace space = F2VectorSpaceFactory.ofBlockFlips(k);
                assertEquals(k, space.getK());

                for (int iter = 0; iter < 5; iter++) {
                    BitSet start = new BitSet(n);
                    for (int i = 0; i < n; i++) {
                        if (rnd.nextBoolean()) start.set(i);
                    }

                    Map<BitSet, Integer> bfsDists = bfsDistances(space, start, n, k);

                    for (int i = 0; i < (1 << n); i++) {
                        BitSet target = new BitSet(n);
                        for (int j = 0; j < n; j++) {
                            if (((i >> j) & 1) != 0) target.set(j);
                        }

                        boolean reachable = bfsDists.containsKey(target);
                        assertEquals(reachable, space.areEquivalent(start, target, n),
                            "areEquivalent fail: n=" + n + ", k=" + k + ", start=" + start + ", target=" + target);

                        if ((k == 1 || k == 2) && reachable) {
                            assertEquals((int) bfsDists.get(target), space.distance(start, target, n),
                                "distance fail: n=" + n + ", k=" + k + ", start=" + start + ", target=" + target);
                        }
                    }
                }
            }
        }
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
