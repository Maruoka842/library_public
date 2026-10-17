package library.util.fold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class StaticDiagonalSlopeAddPointGet2DTest {
	@Test
	public void addsTwiceChebyshevDistance() {
		StaticDiagonalSlopeAddPointGet2D actual =
				new StaticDiagonalSlopeAddPointGet2D(5, 6);
		long[][] expected = new long[5][6];
		actual.addDistanceFrom(3, 0, 5);
		actual.addDistanceFrom(-2, 3, 1);
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 6; j++) {
				expected[i][j] += 6L * Math.max(Math.abs(i), Math.abs(5 - j));
				expected[i][j] -= 4L * Math.max(Math.abs(3 - i), Math.abs(1 - j));
			}
		}
		actual.build();

		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 6; j++) {
				assertEquals(expected[i][j], actual.get(i, j),
						"(" + i + ", " + j + ")");
			}
		}
	}

	@Test
	public void rejectsDistanceCenterOutsideGrid() {
		StaticDiagonalSlopeAddPointGet2D actual =
				new StaticDiagonalSlopeAddPointGet2D(2, 3);
		assertThrows(IndexOutOfBoundsException.class,
				() -> actual.addDistanceFrom(1, 2, 0));
		assertThrows(IndexOutOfBoundsException.class,
				() -> actual.addDistanceFrom(1, 0, -1));
	}

	@Test
	public void addsAffineFunctionsOfBothDiagonalCoordinates() {
		StaticDiagonalSlopeAddPointGet2D actual =
				new StaticDiagonalSlopeAddPointGet2D(5, 6);
		actual.addSlope(1, 7, -3, 3, 2, -4, 9);
		actual.addSlope(3, 8, -5, 1, -3, 5);
		actual.build();

		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 6; y++) {
				int u = x + y;
				int v = x - y;
				long expected = 0;
				if (1 <= u && u < 7 && -3 <= v && v < 3) {
					expected += 2L * (u - 1) - 4L * (v + 3) + 9;
				}
				if (3 <= u && u < 8 && -5 <= v && v < 1) {
					expected += -3L * (u - 3) + 5;
				}
				assertEquals(expected, actual.get(x, y), "(" + x + ", " + y + ")");
			}
		}
	}

	@Test
	public void supportsParityRestrictedUpdates() {
		StaticDiagonalSlopeAddPointGet2D actual =
				new StaticDiagonalSlopeAddPointGet2D(4, 5);
		actual.addSlope(-2, 8, -6, 5, 1, 3, 2, -7);
		actual.addSlope(-2, 8, -6, 5, 0, -2, 0, 4);
		actual.build();

		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 5; y++) {
				int u = x + y;
				int v = x - y;
				long expected = (u & 1) == 1 ? 3L * (u + 2) + 2L * (v + 6) - 7 : 0;
				if ((u & 1) == 0) expected += -2L * (u + 2) + 4;
				assertEquals(expected, actual.get(x, y), "(" + x + ", " + y + ")");
			}
		}
	}

	@Test
	public void agreesWithNaiveRandomUpdates() {
		Random random = new Random(842);
		for (int iteration = 0; iteration < 100; iteration++) {
			int height = random.nextInt(7) + 1;
			int width = random.nextInt(7) + 1;
			StaticDiagonalSlopeAddPointGet2D actual =
					new StaticDiagonalSlopeAddPointGet2D(height, width);
			long[][] expected = new long[height][width];
			for (int query = 0; query < 100; query++) {
				int u0 = random.nextInt(height + width + 5) - 4;
				int u1 = random.nextInt(height + width + 5) - 4;
				int v0 = random.nextInt(height + width + 5) - width - 2;
				int v1 = random.nextInt(height + width + 5) - width - 2;
				long uSlope = random.nextInt(11) - 5;
				long vSlope = random.nextInt(11) - 5;
				long origin = random.nextInt(21) - 10;
				actual.addSlope(u0, u1, v0, v1, uSlope, vSlope, origin);
				for (int x = 0; x < height; x++) {
					for (int y = 0; y < width; y++) {
						int u = x + y;
						int v = x - y;
						if (u0 <= u && u < u1 && v0 <= v && v < v1) {
							expected[x][y] += uSlope * (u - u0)
									+ vSlope * (v - v0) + origin;
						}
					}
				}
			}
			actual.build();
			for (int x = 0; x < height; x++) {
				for (int y = 0; y < width; y++) {
					assertEquals(expected[x][y], actual.get(x, y),
							"iteration=" + iteration + ", (" + x + ", " + y + ")");
				}
			}
		}
	}

	@Test
	public void enforcesLifecycleParityAndCoordinates() {
		StaticDiagonalSlopeAddPointGet2D actual =
				new StaticDiagonalSlopeAddPointGet2D(2, 2);
		assertThrows(IllegalStateException.class, () -> actual.get(0, 0));
		assertThrows(IllegalArgumentException.class,
				() -> actual.addSlope(0, 1, 0, 1, 2, 1, 1, 1));
		actual.build();
		assertThrows(IllegalStateException.class,
				() -> actual.addSlope(0, 1, 0, 1, 1, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> actual.get(2, 0));
	}
}
