package library.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;
import org.junit.jupiter.api.Test;
import library.util.collections.MeldableSkewHeap;

public class MeldableSkewHeapTest {

	@Test
	public void testComparableHeap() {
		MeldableSkewHeap<Integer> heap = new MeldableSkewHeap<>();

		MeldableSkewHeap.Node<Integer> root = null;
		root = heap.meld(root, new MeldableSkewHeap.Node<>(10));
		root = heap.meld(root, new MeldableSkewHeap.Node<>(5));
		root = heap.meld(root, new MeldableSkewHeap.Node<>(20));
		root = heap.meld(root, new MeldableSkewHeap.Node<>(3));

		assertNotNull(root);
		assertEquals(3, root.val);

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals(5, root.val);

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals(10, root.val);

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals(20, root.val);

		root = heap.pop(root);
		assertNull(root);
	}

	@Test
	public void testComparatorHeap() {
		// String length comparator
		MeldableSkewHeap<String> heap = new MeldableSkewHeap<>(Comparator.comparingInt(String::length));

		MeldableSkewHeap.Node<String> root = null;
		root = heap.meld(root, new MeldableSkewHeap.Node<>("banana"));
		root = heap.meld(root, new MeldableSkewHeap.Node<>("apple"));
		root = heap.meld(root, new MeldableSkewHeap.Node<>("pear"));
		root = heap.meld(root, new MeldableSkewHeap.Node<>("watermelon"));

		assertNotNull(root);
		assertEquals("pear", root.val); // length 4

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals("apple", root.val); // length 5

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals("banana", root.val); // length 6

		root = heap.pop(root);
		assertNotNull(root);
		assertEquals("watermelon", root.val); // length 10

		root = heap.pop(root);
		assertNull(root);
	}

	@Test
	public void testNullComparator() {
		assertThrows(IllegalArgumentException.class, () -> {
			new MeldableSkewHeap<Integer>(null);
		});
	}
}
