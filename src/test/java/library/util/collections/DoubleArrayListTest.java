package library.util.collections;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

public class DoubleArrayListTest {

	@Test
	public void testBasicOperations() {
		DoubleArrayList list = new DoubleArrayList();
		assertTrue(list.isEmpty());
		assertFalse(list.isNonEmpty());
		assertEquals(0, list.size());

		list.add(1.5);
		list.add(2.5);
		list.add(3.5);

		assertFalse(list.isEmpty());
		assertTrue(list.isNonEmpty());
		assertEquals(3, list.size());

		assertEquals(1.5, list.get(0));
		assertEquals(2.5, list.get(1));
		assertEquals(3.5, list.get(2));

		assertEquals(1.5, list.peekFirst());
		assertEquals(3.5, list.peekLast());
		assertEquals(3.5, list.peekLastOrDefault(0.0));

		assertEquals(3.5, list.pollLast());
		assertEquals(2, list.size());

		list.set(1, 4.5);
		assertEquals(4.5, list.get(1));

		list.swap(0, 1);
		assertEquals(4.5, list.get(0));
		assertEquals(1.5, list.get(1));

		list.reverse();
		assertEquals(1.5, list.get(0));
		assertEquals(4.5, list.get(1));

		list.clear();
		assertTrue(list.isEmpty());
		assertEquals(0, list.size());
	}

	@Test
	public void testAddAllAndSort() {
		DoubleArrayList list = new DoubleArrayList();
		list.addAll(new double[]{3.3, 1.1, 2.2});
		assertEquals(3, list.size());

		list.sort();
		assertArrayEquals(new double[]{1.1, 2.2, 3.3}, list.toArray(), 1e-9);

		DoubleArrayList list2 = new DoubleArrayList();
		list2.addAll(list);
		assertEquals(3, list2.size());
		assertEquals(list, list2);
		assertEquals(list.hashCode(), list2.hashCode());

		DoubleArrayList copy = list.copy();
		assertEquals(list, copy);
	}

	@Test
	public void testExceptions() {
		DoubleArrayList list = new DoubleArrayList();
		assertThrows(NoSuchElementException.class, list::peekFirst);
		assertThrows(NoSuchElementException.class, list::peekLast);
		assertThrows(NoSuchElementException.class, list::pollLast);
		assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
		assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 1.0));
	}
}
