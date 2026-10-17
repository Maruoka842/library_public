package library.util.collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntSparseSetTest {

	@Test
	public void testInvalidCapacity() {
		assertThrows(IllegalArgumentException.class, () -> new IntSparseSet(-1));
	}

	@Test
	public void testBasicOperations() {
		IntSparseSet set = new IntSparseSet(10);
		assertEquals(10, set.capacity());
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());

		assertTrue(set.add(3));
		assertTrue(set.add(7));
		assertFalse(set.add(3)); // duplicate insertion

		assertEquals(2, set.size());
		assertFalse(set.isEmpty());

		assertTrue(set.contains(3));
		assertTrue(set.contains(7));
		assertFalse(set.contains(0));
		assertFalse(set.contains(5));

		assertTrue(set.remove(3));
		assertFalse(set.remove(3)); // already removed
		assertFalse(set.contains(3));
		assertEquals(1, set.size());

		set.clear();
		assertTrue(set.isEmpty());
		assertEquals(0, set.size());
		assertFalse(set.contains(7));
	}

	@Test
	public void testGetAndToArray() {
		IntSparseSet set = new IntSparseSet(10);
		set.add(2);
		set.add(5);
		set.add(8);

		assertEquals(3, set.size());

		int[] arr = set.toArray();
		assertEquals(3, arr.length);

		boolean[] present = new boolean[10];
		for (int i = 0; i < set.size(); i++) {
			int val = set.get(i);
			present[val] = true;
		}

		assertTrue(present[2]);
		assertTrue(present[5]);
		assertTrue(present[8]);

		assertThrows(IndexOutOfBoundsException.class, () -> set.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> set.get(3));
	}

	@Test
	public void testOutOfBounds() {
		IntSparseSet set = new IntSparseSet(5);
		assertThrows(IndexOutOfBoundsException.class, () -> set.contains(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> set.contains(5));
		assertThrows(IndexOutOfBoundsException.class, () -> set.add(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> set.add(5));
		assertThrows(IndexOutOfBoundsException.class, () -> set.remove(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> set.remove(5));
	}

	@Test
	public void testCopy() {
		IntSparseSet original = new IntSparseSet(10);
		original.add(1);
		original.add(4);
		original.add(9);

		IntSparseSet copy = original.copy();

		assertEquals(original.capacity(), copy.capacity());
		assertEquals(original.size(), copy.size());
		assertTrue(copy.contains(1));
		assertTrue(copy.contains(4));
		assertTrue(copy.contains(9));

		// Verify independence
		copy.add(2);
		copy.remove(1);

		assertTrue(original.contains(1));
		assertFalse(original.contains(2));
		assertFalse(copy.contains(1));
		assertTrue(copy.contains(2));

		original.clear();
		assertTrue(original.isEmpty());
		assertEquals(3, copy.size());
	}
}
