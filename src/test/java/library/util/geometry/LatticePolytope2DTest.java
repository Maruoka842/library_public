package library.util.geometry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import library.util.geometry.LatticePolytope2D.Location;

public class LatticePolytope2DTest {

    @Test
    public void testSquare() {
        // Unit square (0,0), (1,0), (1,1), (0,1)
        LongPoint[] vertices = {
            new LongPoint(0, 0),
            new LongPoint(1, 0),
            new LongPoint(1, 1),
            new LongPoint(0, 1)
        };
        LatticePolytope2D poly = new LatticePolytope2D(vertices);

        assertEquals(2, poly.twiceArea());
        assertEquals(4, poly.boundaryLatticePoints());
        assertEquals(0, poly.interiorLatticePoints());
        assertEquals(4, poly.countLatticePoints());

        // scale by 2
        LatticePolytope2D poly2 = poly.scale(2);
        // (0,0), (2,0), (2,2), (0,2)
        assertEquals(8, poly2.twiceArea());
        assertEquals(8, poly2.boundaryLatticePoints());
        assertEquals(1, poly2.interiorLatticePoints());
        assertEquals(9, poly2.countLatticePoints());

        assertEquals(9, poly.countLatticePoints(2));
    }

    @Test
    public void testTriangle() {
        // Triangle (0,0), (2,0), (0,2)
        LongPoint[] vertices = {
            new LongPoint(0, 0),
            new LongPoint(2, 0),
            new LongPoint(0, 2)
        };
        LatticePolytope2D poly = new LatticePolytope2D(vertices);

        assertEquals(4, poly.twiceArea()); // Area = 2
        assertEquals(6, poly.boundaryLatticePoints()); // (0,0)-(2,0): 2, (2,0)-(0,2): 2, (0,2)-(0,0): 2
        assertEquals(0, poly.interiorLatticePoints()); // 2 = I + 6/2 - 1 => I = 0
        assertEquals(6, poly.countLatticePoints());
    }

    @Test
    public void testLocate() {
        // Square (0,0), (2,0), (2,2), (0,2)
        LongPoint[] vertices = {
            new LongPoint(0, 0),
            new LongPoint(2, 0),
            new LongPoint(2, 2),
            new LongPoint(0, 2)
        };
        LatticePolytope2D poly = new LatticePolytope2D(vertices);

        assertEquals(Location.INSIDE, poly.locate(new LongPoint(1, 1)));
        assertEquals(Location.ON_BOUNDARY, poly.locate(new LongPoint(0, 0)));
        assertEquals(Location.ON_BOUNDARY, poly.locate(new LongPoint(2, 2)));
        assertEquals(Location.ON_BOUNDARY, poly.locate(new LongPoint(0, 1)));
        assertEquals(Location.ON_BOUNDARY, poly.locate(new LongPoint(2, 1)));

        assertEquals(Location.OUTSIDE, poly.locate(new LongPoint(-1, 1)));
        assertEquals(Location.OUTSIDE, poly.locate(new LongPoint(3, 1)));
        assertEquals(Location.OUTSIDE, poly.locate(new LongPoint(1, -1)));
        assertEquals(Location.OUTSIDE, poly.locate(new LongPoint(1, 3)));
    }

    @Test
    public void testMinkowskiSum() {
        // Square1 (0,0)-(1,0)-(1,1)-(0,1)
        LatticePolytope2D poly1 = new LatticePolytope2D(new LongPoint[]{
            new LongPoint(0, 0),
            new LongPoint(1, 0),
            new LongPoint(1, 1),
            new LongPoint(0, 1)
        });
        // Square2 (0,0)-(1,0)-(1,1)-(0,1)
        LatticePolytope2D poly2 = new LatticePolytope2D(new LongPoint[]{
            new LongPoint(0, 0),
            new LongPoint(1, 0),
            new LongPoint(1, 1),
            new LongPoint(0, 1)
        });

        LatticePolytope2D sum = poly1.minkowskiSum(poly2);
        // Result should be (0,0)-(2,0)-(2,2)-(0,2)
        assertEquals(8, sum.twiceArea());
        assertEquals(8, sum.boundaryLatticePoints());
        assertEquals(9, sum.countLatticePoints());

        assertEquals(Location.ON_BOUNDARY, sum.locate(new LongPoint(0, 0)));
        assertEquals(Location.ON_BOUNDARY, sum.locate(new LongPoint(2, 2)));
        assertEquals(Location.INSIDE, sum.locate(new LongPoint(1, 1)));
    }

    @Test
    public void testDraw() {
        LongPoint[] vertices = {
            new LongPoint(0, 0),
            new LongPoint(2, 0),
            new LongPoint(2, 2),
            new LongPoint(0, 2)
        };
        LatticePolytope2D poly = new LatticePolytope2D(vertices);

        try {
            poly.draw();
        } catch (java.awt.HeadlessException e) {
            // HeadlessException is expected and fine when running draw() in headless mode
        } catch (Exception e) {
            fail("draw() threw an unexpected exception: " + e.getMessage());
        }
    }

    @Test
    public void testFromPoints() {
        LongPoint[] points = {
            new LongPoint(0, 0),
            new LongPoint(1, 0),
            new LongPoint(2, 0),
            new LongPoint(2, 1),
            new LongPoint(2, 2),
            new LongPoint(1, 2),
            new LongPoint(0, 2),
            new LongPoint(0, 1),
            new LongPoint(1, 1) // internal
        };
        LatticePolytope2D poly = LatticePolytope2D.fromPoints(points);
        // Should be (0,0), (2,0), (2,2), (0,2)
        assertEquals(4, poly.vertices.length);
        assertEquals(8, poly.twiceArea());
    }

    @Test
    public void testDecomposeIDP() {
        // Test 1: Unit Square
        LongPoint[] squareVertices = {
            new LongPoint(0, 0),
            new LongPoint(1, 0),
            new LongPoint(1, 1),
            new LongPoint(0, 1)
        };
        LatticePolytope2D square = new LatticePolytope2D(squareVertices);

        // k = 0, x = (0,0)
        LatticePolytope2D.Term[] terms0 = square.decomposeIDP(0, new LongPoint(0, 0));
        assertEquals(0, terms0.length);

        // k = 0, x != (0,0) throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> square.decomposeIDP(0, new LongPoint(1, 0)));

        // k < 0 throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> square.decomposeIDP(-1, new LongPoint(0, 0)));

        // k = 3, x = (2, 1) in 3P
        long k = 3;
        LongPoint x = new LongPoint(2, 1);
        LatticePolytope2D.Term[] terms = square.decomposeIDP(k, x);
        assertTrue(terms.length > 0 && terms.length <= 3);

        long sumM = 0;
        long sumX = 0;
        long sumY = 0;
        for (LatticePolytope2D.Term term : terms) {
            assertTrue(term.multiplicity() > 0);
            assertEquals(Location.INSIDE, square.locate(term.point()) == Location.OUTSIDE ? Location.OUTSIDE : Location.INSIDE);
            sumM += term.multiplicity();
            sumX += term.multiplicity() * term.point().x();
            sumY += term.multiplicity() * term.point().y();
        }
        assertEquals(k, sumM);
        assertEquals(x.x(), sumX);
        assertEquals(x.y(), sumY);

        // Test 2: Large Triangle with Non-primitive edge and large area
        // (0,0), (4,0), (1,3)
        LongPoint[] triVertices = {
            new LongPoint(0, 0),
            new LongPoint(4, 0),
            new LongPoint(1, 3)
        };
        LatticePolytope2D tri = new LatticePolytope2D(triVertices);

        // Grid scan inside 5*P
        long k5 = 5;
        // x ranges within 5*P: x in [0, 20], y in [0, 15]
        for (long px = 0; px <= 20; px++) {
            for (long py = 0; py <= 15; py++) {
                LongPoint pt = new LongPoint(px, py);
                if (tri.locate(new LongPoint(px, py)) != Location.OUTSIDE) {
                    // Check scaling by 5
                    // Note: locate checks point in P, for kP we use direct decompose
                }
                // Try decompose on points inside 5*tri
                // (0,0)-(20,0) lower, (0,0)-(5,15) left, (20,0)-(5,15) right
                // 3y <= 15 - 3/15 * x ...
                // Let's test a few specific points in 5*tri:
                // (5, 5), (10, 2), (3, 7)
            }
        }

        LongPoint[] testPoints = {
            new LongPoint(0, 0),
            new LongPoint(20, 0),
            new LongPoint(5, 15),
            new LongPoint(10, 2),
            new LongPoint(7, 6),
            new LongPoint(3, 4)
        };
        for (LongPoint tp : testPoints) {
            LatticePolytope2D.Term[] decomp = tri.decomposeIDP(5, tp);
            assertTrue(decomp.length > 0 && decomp.length <= 3);
            long mSum = 0;
            long xSum = 0;
            long ySum = 0;
            for (LatticePolytope2D.Term term : decomp) {
                assertTrue(term.multiplicity() > 0);
                assertNotEquals(Location.OUTSIDE, tri.locate(term.point()));
                mSum += term.multiplicity();
                xSum += term.multiplicity() * term.point().x();
                ySum += term.multiplicity() * term.point().y();
            }
            assertEquals(5, mSum);
            assertEquals(tp.x(), xSum);
            assertEquals(tp.y(), ySum);
        }

        // Test outside point throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> tri.decomposeIDP(5, new LongPoint(-1, 0)));
        assertThrows(IllegalArgumentException.class, () -> tri.decomposeIDP(5, new LongPoint(21, 0)));
    }
}
