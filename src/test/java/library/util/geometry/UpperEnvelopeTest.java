package library.util.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class UpperEnvelopeTest {

    @Test
    public void testEmptyEnvelopeDraw() {
        UpperEnvelope env = new UpperEnvelope();
        assertThrows(NoSuchElementException.class, env::toChart);
    }

    @Test
    public void testSingleLineDraw() {
        UpperEnvelope env = new UpperEnvelope();
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
        UpperEnvelope env = new UpperEnvelope();
        // Line 1: y = 2x + 10  <=> 2x - y + 10 = 0
        // Line 2: y = -x + 5   <=> -x - y + 5 = 0  => x + y - 5 = 0
        // Line 3: y = 0        <=> y = 0           => 0x + y + 0 = 0
        env.registerLine(new LongLine(2, -1, 10));
        env.registerLine(new LongLine(1, 1, -5));
        env.registerLine(new LongLine(0, 1, 0));

        assertNotNull(env.toChart());

        System.setProperty("java.awt.headless", "true");
        try {
            env.draw();
        } catch (java.awt.HeadlessException e) {
            // Expected in headless environment
        }

        // Test maxYAt
        // At x = 0: y = 2x+10 (10), y = -x+5 (5), y = 0 (0) -> max is 10
        assertEquals(10, env.maxYAt(0).toDouble(), 1e-9);
    }
}
