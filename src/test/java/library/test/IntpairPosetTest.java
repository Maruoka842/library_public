package library.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import library.util.poset.IntpairPoset;

public class IntpairPosetTest {

    @Test
    public void maximumKChainsLengthHandlesBoundariesAndDuplicates() {
        assertEquals(0, IntpairPoset.maximumKChainsLength(new int[0], new int[0], 3, false));
        assertEquals(0, IntpairPoset.maximumKChainsLength(new int[] {1}, new int[] {1}, 0, false));
        assertThrows(AssertionError.class,
                () -> IntpairPoset.maximumKChainsLength(new int[] {1}, new int[0], 1, false));
        assertThrows(AssertionError.class,
                () -> IntpairPoset.maximumKChainsLength(new int[] {1}, new int[] {1}, -1, false));

        int[] x = {1, 1, 2, 3};
        int[] y = {1, 1, 2, 3};
        assertEquals(4, IntpairPoset.maximumKChainsLength(x, y, 1, false));
        assertEquals(3, IntpairPoset.maximumKChainsLength(x, y, 1, true));
        assertEquals(4, IntpairPoset.maximumKChainsLength(x, y, 2, true));
    }

    @Test
    public void maximumKChainsLengthHandlesAntichain() {
        int[] x = {1, 2, 3, 4};
        int[] y = {4, 3, 2, 1};
        assertEquals(1, IntpairPoset.maximumKChainsLength(x, y, 1, false));
        assertEquals(2, IntpairPoset.maximumKChainsLength(x, y, 2, false));
        assertEquals(4, IntpairPoset.maximumKChainsLength(x, y, 4, false));
    }

    @Test
    public void maximumKChainsLengthDistinguishesStrictAndNonStrict() {
        int[] x = {1, 1, 2};
        int[] y = {1, 2, 2};
        assertEquals(3, IntpairPoset.maximumKChainsLength(x, y, 1, false));
        assertEquals(2, IntpairPoset.maximumKChainsLength(x, y, 1, true));
        assertEquals(3, IntpairPoset.maximumKChainsLength(x, y, 2, true));
    }

    @Test
    public void maximumKChainsLengthAgreesWithExhaustiveSearch() {
        Random random = new Random(0x5eed);
        List<Integer> cells = new ArrayList<>();
        for (int cell = 0; cell < 16; cell++) cells.add(cell);

        for (int trial = 0; trial < 100; trial++) {
            Collections.shuffle(cells, random);
            int n = 1 + random.nextInt(8);
            int[] x = new int[n];
            int[] y = new int[n];
            for (int i = 0; i < n; i++) {
                x[i] = cells.get(i) / 4;
                y[i] = cells.get(i) % 4;
            }
            for (int k = 0; k <= n; k++) {
                for (boolean strict : new boolean[] {false, true}) {
                    assertEquals(bruteForce(x, y, k, strict),
                            IntpairPoset.maximumKChainsLength(x, y, k, strict),
                            "trial=" + trial + ", k=" + k + ", strict=" + strict);
                }
            }
        }
    }

    private int bruteForce(int[] x, int[] y, int k, boolean strict) {
        int answer = 0;
        for (int selected = 0; selected < 1 << x.length; selected++) {
            if (Integer.bitCount(selected) <= answer) continue;
            int width = 0;
            for (int antichain = selected; antichain != 0; antichain = (antichain - 1) & selected) {
                if (isAntichain(x, y, antichain, strict)) {
                    width = Math.max(width, Integer.bitCount(antichain));
                }
            }
            if (width <= k) answer = Integer.bitCount(selected);
        }
        return answer;
    }

    private boolean isAntichain(int[] x, int[] y, int mask, boolean strict) {
        for (int i = 0; i < x.length; i++) {
            if ((mask & 1 << i) == 0) continue;
            for (int j = i + 1; j < x.length; j++) {
                if ((mask & 1 << j) == 0) continue;
                boolean iLeqJ = strict
                        ? x[i] < x[j] && y[i] < y[j]
                        : x[i] <= x[j] && y[i] <= y[j];
                boolean jLeqI = strict
                        ? x[j] < x[i] && y[j] < y[i]
                        : x[j] <= x[i] && y[j] <= y[i];
                if (iLeqJ || jLeqI) return false;
            }
        }
        return true;
    }
}
