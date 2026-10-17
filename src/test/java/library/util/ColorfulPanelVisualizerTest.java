package library.util;

import org.junit.jupiter.api.Test;

import library.util.graph.grid.ColorfulPanelVisualizer;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.List;

public class ColorfulPanelVisualizerTest {

    @SuppressWarnings("unchecked")
    private List<int[][]> getHistory(ColorfulPanelVisualizer vis) throws Exception {
        Field history = ColorfulPanelVisualizer.class.getDeclaredField("history");
        history.setAccessible(true);
        return (List<int[][]>) history.get(vis);
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        int N = 4;
        int M = 6;
        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(N, M);
        List<int[][]> history = getHistory(vis);
        assertEquals(1, history.size());
        int[][] grid = history.get(0);
        assertEquals(N, grid.length);
        for (int i = 0; i < N; i++) {
            assertEquals(M, grid[i].length);
            for (int j = 0; j < M; j++) {
                assertEquals(0, grid[i][j]);
            }
        }
    }

    @Test
    public void testSample() {
        int N = 6;
        int M = 6;
        String[] s = {
            "515795",
            "153859",
            "833597",
            "333419",
            "333121",
            "533917"
        };
        int[][] grid = new int[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                grid[i][j] = s[i].charAt(j) - '0';
            }
        }

        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(N, M, grid);

        // Touch (5, 5) -> Color 1 (0-indexed: (4, 4) -> 1)
        vis.paintConnectedComponent(4, 4, 1);
        // Touch (5, 2) -> Color 1 (0-indexed: (4, 1) -> 1)
        vis.paintConnectedComponent(4, 1, 1);

        // To actually see it, you would call vis.draw(),
        // but we don't do that in automated tests.
    }

    @Test
    public void testPaintRectangle() throws Exception {
        int[][] grid = {
            {1, 1, 1, 1},
            {1, 2, 2, 1},
            {1, 2, 2, 1},
            {1, 1, 1, 1}
        };

        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(4, 4, grid);
        vis.paintRectangle(1, 1, 3, 4, 5);

        List<int[][]> history = getHistory(vis);
        assertEquals(2, history.size());
        assertArrayEquals(new int[][]{
            {1, 1, 1, 1},
            {1, 5, 5, 5},
            {1, 5, 5, 5},
            {1, 1, 1, 1}
        }, history.get(1));
        assertArrayEquals(new int[][]{
            {1, 1, 1, 1},
            {1, 2, 2, 1},
            {1, 2, 2, 1},
            {1, 1, 1, 1}
        }, grid);
    }

    @SuppressWarnings("unchecked")
    private List<ColorfulPanelVisualizer.VisualPath> getPaths(ColorfulPanelVisualizer vis) throws Exception {
        Field pathsField = ColorfulPanelVisualizer.class.getDeclaredField("paths");
        pathsField.setAccessible(true);
        return (List<ColorfulPanelVisualizer.VisualPath>) pathsField.get(vis);
    }

    @Test
    public void testAddPathAndClear() throws Exception {
        int[][] grid = {
            {0, 1},
            {2, 3}
        };
        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(2, 2, grid);

        int[][] p1 = { {0, 0}, {0, 1}, {1, 1} };
        int[][] p2 = { {0, 0}, {0, 1}, {1, 1} };

        vis.addPath(p1, java.awt.Color.RED);
        vis.addPath(p2, java.awt.Color.BLUE);

        List<ColorfulPanelVisualizer.VisualPath> paths = getPaths(vis);
        assertEquals(2, paths.size());
        assertEquals(java.awt.Color.RED, paths.get(0).color);
        assertEquals(java.awt.Color.BLUE, paths.get(1).color);
        assertEquals(3, paths.get(0).path.size());

        vis.clearPaths();
        assertEquals(0, getPaths(vis).size());
    }

    @Test
    public void testGridVisibleToggle() {
        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(3, 4);
        assertTrue(vis.isGridVisible());

        vis.setGridVisible(false);
        assertFalse(vis.isGridVisible());

        vis.setShowGrid(true);
        assertTrue(vis.isGridVisible());
    }

    @Test
    public void testRectangularGridOperations() throws Exception {
        int N = 3;
        int M = 5;
        int[][] grid = {
            {1, 1, 1, 0, 0},
            {1, 2, 1, 0, 3},
            {0, 0, 0, 0, 3}
        };

        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(N, M, grid);

        vis.paintConnectedComponent(0, 0, 4);
        List<int[][]> history = getHistory(vis);
        assertEquals(2, history.size());
        assertArrayEquals(new int[][]{
            {4, 4, 4, 0, 0},
            {4, 2, 4, 0, 3},
            {0, 0, 0, 0, 3}
        }, history.get(1));

        vis.paintRectangle(1, 3, 3, 5, 2);
        history = getHistory(vis);
        assertEquals(3, history.size());
        assertArrayEquals(new int[][]{
            {4, 4, 4, 0, 0},
            {4, 2, 4, 2, 2},
            {0, 0, 0, 2, 2}
        }, history.get(2));
    }

    @Test
    public void testPathRenderingInPanel() {
        int[][] grid = {
            {0, 1, 2},
            {3, 4, 5}
        };
        ColorfulPanelVisualizer vis = new ColorfulPanelVisualizer(2, 3, grid);
        vis.addPath(new int[][]{ {0, 0}, {0, 1} }, java.awt.Color.RED);
        vis.addPath(new int[][]{ {0, 0}, {0, 1} }, java.awt.Color.BLUE);
        vis.addPath(new int[][]{ {1, 0} }, java.awt.Color.GREEN);

        // Verify that rendering on a Graphics object completes without exception
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(200, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics g = img.getGraphics();

        try {
            Class<?> panelClass = null;
            for (Class<?> c : ColorfulPanelVisualizer.class.getDeclaredClasses()) {
                if (c.getSimpleName().equals("VisualPanel")) {
                    panelClass = c;
                    break;
                }
            }
            assertNotNull(panelClass);
            java.lang.reflect.Constructor<?> ctor = panelClass.getDeclaredConstructor(ColorfulPanelVisualizer.class);
            ctor.setAccessible(true);
            Object panel = ctor.newInstance(vis);

            java.lang.reflect.Method paintComponent = panelClass.getDeclaredMethod("paintComponent", java.awt.Graphics.class);
            paintComponent.setAccessible(true);
            assertDoesNotThrow(() -> paintComponent.invoke(panel, g));
        } catch (Exception e) {
            fail("Reflection or rendering failed: " + e.getMessage());
        } finally {
            g.dispose();
        }
    }

}
