package library.util.collections;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DoubleTreapSetTest {

	@Test
	public void testAddContainsRemove() {
		DoubleTreapSet set = new DoubleTreapSet();
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());

		set.add(10.5);
		set.add(5.5);
		set.add(15.5);

		assertEquals(3, set.size());
		assertTrue(set.contains(10.5));
		assertTrue(set.contains(5.5));
		assertTrue(set.contains(15.5));
		assertFalse(set.contains(7.5));

		set.remove(10.5);
		assertEquals(2, set.size());
		assertFalse(set.contains(10.5));
	}

	@Test
	public void testMinMaxAndNavigational() {
		DoubleTreapSet set = new DoubleTreapSet();
		assertNull(set.first());
		assertNull(set.last());

		set.add(1.0);
		set.add(3.0);
		set.add(5.0);

		assertEquals(1.0, set.first());
		assertEquals(5.0, set.last());

		assertEquals(3.0, set.floor(3.0));
		assertEquals(1.0, set.lower(3.0));
		assertEquals(5.0, set.higher(3.0));
		assertEquals(3.0, set.ceil(3.0));

		assertEquals(0, set.countLess(1.0));
		assertEquals(1, set.countLess(2.0));
		assertEquals(2, set.countLeq(3.0));
		assertEquals(1, set.countGreater(3.0));

		assertEquals(1.0, set.kth(0));
		assertEquals(3.0, set.kth(1));
		assertEquals(5.0, set.kth(2));

		assertEquals(1.0, set.pollFirst());
		assertEquals(2, set.size());
		assertEquals(5.0, set.pollLast());
		assertEquals(1, set.size());
		assertEquals(3.0, set.first());
	}

	@Test
	public void testSetOperations() {
		DoubleTreapSet set1 = DoubleTreapSet.buildFromSorted(new double[]{1.1, 2.2, 3.3, 4.4});
		DoubleTreapSet set2 = DoubleTreapSet.buildFromSorted(new double[]{2.2, 4.4, 5.5});

		DoubleTreapSet cap = set1.cap(set2);
		assertEquals(2, cap.size());
		assertTrue(cap.contains(2.2));
		assertTrue(cap.contains(4.4));

		DoubleTreapSet copy = set1.copy();
		assertEquals(set1, copy);
		assertEquals(set1.hashCode(), copy.hashCode());

		set1.clear();
		assertTrue(set1.isEmpty());
	}
}
