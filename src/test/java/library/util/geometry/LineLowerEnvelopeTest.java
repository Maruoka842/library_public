package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class LineLowerEnvelopeTest {

    @Test
    public void testEmptyEnvelopeDraw() {
        LineLowerEnvelope env = new LineLowerEnvelope();
        assertThrows(NoSuchElementException.class, env::toChart);
    }

    @Test
    public void testSingleLineDraw() {
        LineLowerEnvelope env = new LineLowerEnvelope();
        // y = -x + 2 <=> x + y - 2 = 0
        env.registerLine(new LongLine(1, 1, -2));
        assertNotNull(env.toChart());

        System.setProperty("java.awt.headless", "true");
        try {
            env.draw();
        } catch (java.awt.HeadlessException e) {
            // Expected in headless environment
        }
    }

    @Test
    public void testMultipleLinesDrawAndQueries() {
        LineLowerEnvelope env = new LineLowerEnvelope();
        // Line 1: y = 2x + 10  <=> 2x - y + 10 = 0
        // Line 2: y = -x + 5   <=> -x - y + 5 = 0  => x + y - 5 = 0
        // Line 3: y = 0        <=> y = 0           => 0x + y + 0 = 0
        env.registerLine(new LongLine(2, -1, 10));
        env.registerLine(new LongLine(1, 1, -5));
        env.registerLine(new LongLine(0, 1, 0));

        env.build();
        assertEquals(3, env.size());

        assertNotNull(env.toChart());

        System.setProperty("java.awt.headless", "true");
        try {
            env.draw();
        } catch (java.awt.HeadlessException e) {
            // Expected in headless environment
        }

        // Test minYAt
        // At x = 0: y = 2x+10 (10), y = -x+5 (5), y = 0 (0) -> min is 0
        assertEquals(0, env.minYAt(0).toDouble(), 1e-9);
    }
}
