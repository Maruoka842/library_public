package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HamSandwichCutTest {

    private boolean isBisecting(LongLine line, LongPoint[] points) {
        int left = 0;
        int right = 0;
        for (LongPoint p : points) {
            long val = line.a * p.x + line.b * p.y + line.c;
            if (val > 0) left++;
            else if (val < 0) right++;
        }
        int maxAllowed = points.length / 2;
        return left <= maxAllowed && right <= maxAllowed;
    }

    @Test
    public void testBasicOddPoints() {
        HamSandwichCut hsc = new HamSandwichCut(42L);

        LongPoint[] p = {
            new LongPoint(0, 5),
            new LongPoint(-2, 3),
            new LongPoint(2, 4)
        };
        LongPoint[] q = {
            new LongPoint(0, -5),
            new LongPoint(-3, -2),
            new LongPoint(3, -4)
        };

        LongLine line = hsc.solve(p, q);
        assertNotNull(line);
        assertTrue(isBisecting(line, p), "Line must bisect set p");
        assertTrue(isBisecting(line, q), "Line must bisect set q");
    }

    @Test
    public void testEvenPoints() {
        HamSandwichCut hsc = new HamSandwichCut(12345L);

        LongPoint[] p = {
            new LongPoint(1, 1),
            new LongPoint(2, 3),
            new LongPoint(-1, 4),
            new LongPoint(-2, 2)
        };
        LongPoint[] q = {
            new LongPoint(3, -1),
            new LongPoint(4, -3),
            new LongPoint(-3, -2),
            new LongPoint(-4, -4)
        };

        LongLine line = hsc.solve(p, q);
        assertNotNull(line);
        assertTrue(isBisecting(line, p), "Line must bisect set p");
        assertTrue(isBisecting(line, q), "Line must bisect set q");
    }

    @Test
    public void testSameXCoordinatesInGeneralPosition() {
        // Points sharing identical x-coordinates, but no 3 points are collinear
        HamSandwichCut hsc = new HamSandwichCut(999L);

        LongPoint[] p = {
            new LongPoint(0, 1),
            new LongPoint(1, 5),
            new LongPoint(2, 4)
        };
        LongPoint[] q = {
            new LongPoint(0, -1),
            new LongPoint(-1, -5),
            new LongPoint(-2, -4)
        };

        LongLine line = hsc.solve(p, q);
        assertNotNull(line);
        assertTrue(isBisecting(line, p), "Line must bisect set p");
        assertTrue(isBisecting(line, q), "Line must bisect set q");
    }
}
