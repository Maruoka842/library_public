package library.util.graph.grid;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Grid2DTest {

    @Test
    public void testGetDir() {
        // Grid2D.dh = {1, 0, -1, 0}
        // Grid2D.dw = {0, 1, 0, -1}
        assertEquals(0, Grid2D.getDir(1, 0));
        assertEquals(1, Grid2D.getDir(0, 1));
        assertEquals(2, Grid2D.getDir(-1, 0));
        assertEquals(3, Grid2D.getDir(0, -1));

        // Unmatched directions
        assertEquals(-1, Grid2D.getDir(1, 1));
        assertEquals(-1, Grid2D.getDir(-1, -1));
        assertEquals(-1, Grid2D.getDir(2, 0));
        assertEquals(-1, Grid2D.getDir(0, 0));
    }
}
