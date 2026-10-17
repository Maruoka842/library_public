package library.util.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class StaticDiagonalRangeAddPointGet2DTest {
	@Test
	public void addsByBothDiagonalCoordinates() {
		StaticDiagonalRangeAddPointGet2D actual = new StaticDiagonalRangeAddPointGet2D(4, 5);
		actual.add(2, 6, -2, 2, 7);
		actual.add(0, 3, 0, 4, -3);
		actual.build();

		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 5; y++) {
				long expected = 0;
				if (2 <= x + y && x + y < 6 && -2 <= x - y && x - y < 2) {
					expected += 7;
				}
				if (0 <= x + y && x + y < 3 && 0 <= x - y && x - y < 4) {
					expected -= 3;
				}
				assertEquals(expected, actual.get(x, y), "(" + x + ", " + y + ")");
			}
		}
	}

	@Test
	public void clipsUpdatesAndIgnoresEmptyRanges() {
		StaticDiagonalRangeAddPointGet2D actual = new StaticDiagonalRangeAddPointGet2D(2, 3);
		actual.add(Integer.MIN_VALUE, Integer.MAX_VALUE,
				Integer.MIN_VALUE, Integer.MAX_VALUE, 11);
		actual.add(3, 3, -100, 100, 100);
		actual.add(10, 20, 10, 20, 1000);
		actual.build();

		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 3; y++) {
				assertEquals(11, actual.get(x, y), "(" + x + ", " + y + ")");
			}
		}
	}

	@Test
	public void agreesWithNaiveRandomUpdates() {
		Random random = new Random(1);
		for (int iteration = 0; iteration < 100; iteration++) {
			int height = random.nextInt(7) + 1;
			int width = random.nextInt(7) + 1;
			StaticDiagonalRangeAddPointGet2D actual =
					new StaticDiagonalRangeAddPointGet2D(height, width);
			long[][] expected = new long[height][width];
			for (int query = 0; query < 100; query++) {
				int u0 = random.nextInt(height + width + 5) - 4;
				int u1 = random.nextInt(height + width + 5) - 4;
				int v0 = random.nextInt(height + width + 5) - width - 2;
				int v1 = random.nextInt(height + width + 5) - width - 2;
				long value = random.nextInt(21) - 10;
				actual.add(u0, u1, v0, v1, value);
				for (int x = 0; x < height; x++) {
					for (int y = 0; y < width; y++) {
						if (u0 <= x + y && x + y < u1 && v0 <= x - y && x - y < v1) {
							expected[x][y] += value;
						}
					}
				}
			}
			actual.build();
			for (int x = 0; x < height; x++) {
				for (int y = 0; y < width; y++) {
					assertEquals(expected[x][y], actual.get(x, y),
							"iteration=" + iteration + ", size=" + height + "x" + width
									+ ", (" + x + ", " + y + ")");
				}
			}
		}
	}

	@Test
	public void enforcesLifecycleAndCoordinates() {
		StaticDiagonalRangeAddPointGet2D actual = new StaticDiagonalRangeAddPointGet2D(2, 2);
		assertThrows(IllegalStateException.class, () -> actual.get(0, 0));
		actual.build();
		assertThrows(IllegalStateException.class, () -> actual.add(0, 1, 0, 1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> actual.get(-1, 0));
		assertThrows(IndexOutOfBoundsException.class, () -> actual.get(2, 0));
	}

	@Test
	public void supportsEmptyGrid() {
		StaticDiagonalRangeAddPointGet2D actual = new StaticDiagonalRangeAddPointGet2D(0, 3);
		actual.add(Integer.MIN_VALUE, Integer.MAX_VALUE,
				Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
		actual.build();
		assertThrows(IndexOutOfBoundsException.class, () -> actual.get(0, 0));
	}
}
