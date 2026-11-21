package library.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 2次元配列操作のユーティリティクラス。
 */
public class ArrayUtils2D {

	public static void decrementAll(int[][] a) {
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				a[i][j]--;
			}
		}
	}

	public static void swapColumns(int i, int j, long[][] a) {
		if(i==j)return;
		for (int k = 0; k < a.length; k++) {
			var tmp = a[k][i];
			a[k][i] = a[k][j];
			a[k][j] = tmp;
		}
	}

	public static <T> void swapColumns(int i, int j, T[][] a) {
		if (i == j) return;
		for (int k = 0; k < a.length; k++) {
			T tmp = a[k][i];
			a[k][i] = a[k][j];
			a[k][j] = tmp;
		}
	}

	public static int[][] randomIntTable(int lowerBound, int upperBound, int H, int W) {
		int[][] ret = new int[H][];
		for (int i = 0; i < H; i++) {
			ret[i]=ArrayUtils.randomIntArray(lowerBound, upperBound, W);
		}
		return ret;
	}

	public static long[][] randomLongTable(long lowerBound, long upperBound, int H, int W) {
		long[][] ret = new long[H][];
		for (int i = 0; i < H; i++) {
			ret[i]=ArrayUtils.randomLongArray(lowerBound, upperBound, W);
		}
		return ret;
	}

	public static boolean checkBounds(int i, int j, int[][] a) {
		return 0 <= i && i < a.length && ArrayUtils.checkBounds(j, a[i]);
	}

    public static boolean checkBounds(int i, int j, long[][] a) {
        return 0 <= i && i < a.length && ArrayUtils.checkBounds(j, a[i]);
    }

	public static <T> void fill(T[][] a, T v) {
		for (int i = 0; i < a.length; i++) {
			Arrays.fill(a[i], v);
		}
	}

	public static void fill(int[][] a, int v) {
		for (int i = 0; i < a.length; ++i) {
			ArrayUtils.fill(a[i], v);
		}
	}

	public static void fill(double[][] a, double v) {
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				a[i][j] = v;
			}
		}
	}

	public static void fill(char[][] a, char v) {
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				a[i][j] = v;
			}
		}
	}

	public static void fill(long[][] a, long v) {
		for (int i = 0; i < a.length; ++i) {
			ArrayUtils.fill(a[i], v);
		}
	}

	public static void fill(boolean[][] a, boolean v) {
		for (int i = 0; i < a.length; ++i) {
			ArrayUtils.fill(a[i], v);
		}
	}

	/**
	 * swap(a[i][j], a[i][n-1-j]) for all j
	 * @param a
	 */
	public static void reverseHolizontally(int[][] a) {
		for (int i = 0; i < a.length; i++) {
			ArrayUtils.reverse(a[i]);
		}
	}

	public static void reverseVertically(int[][] a) {
		for (int i = 0; 2 * i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                var tmp = a[i][j];
                a[i][j] = a[a.length - 1 - i][j];
                a[a.length - 1 - i][j] = tmp;
            }
        }
	}

	public static void reverseVertically(long[][] a) {
		for (int i = 0; 2 * i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                var tmp = a[i][j];
                a[i][j] = a[a.length - 1 - i][j];
                a[a.length - 1 - i][j] = tmp;
            }
        }
	}

	public static boolean equals(int[][] a, int[][] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i) {
			if (!Arrays.equals(a[i], b[i])) return false;
		}
		return true;
	}

	public static boolean equals(long[][] a, long[][] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i) {
			if (!Arrays.equals(a[i], b[i])) return false;
		}
		return true;
	}

	public static boolean equals(boolean[][] a, boolean[][] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i) {
			if (!Arrays.equals(a[i], b[i])) return false;
		}
		return true;
	}

	public static boolean equals(char[][] a, char[][] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; ++i) {
			if (!Arrays.equals(a[i], b[i])) return false;
		}
		return true;
	}

	public static long[][] prefixMax(long[][] a) {
		long[][] b = copy(a);
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				if(i>0)b[i][j]=Math.max(b[i][j], b[i-1][j]);
				if(j>0)b[i][j]=Math.max(b[i][j], b[i][j-1]);
			}
		}
		return b;
	}

	public static long[][] prefixMaxFromLeftBottom(long[][] a) {
		long[][] b = copy(a);
		for (int i = a.length-1; i >= 0; i--) {
			for (int j = 0; j < a[i].length; j++) {
				if(i+1<a.length)b[i][j]=Math.max(b[i][j], b[i+1][j]);
				if(j>0)b[i][j]=Math.max(b[i][j], b[i][j-1]);
			}
		}
		return b;
	}

	/**
	 * b[i][j] = Σ[i'≤i and j'≤j] a[i'][j']
	 *
	 * @param a
	 * @return
	 */
	public static long[][] prefixSum(long[][] a) {
		long[][] b = new long[a.length][];
		for (int i = 0; i < b.length; ++i) {
			b[i] = ArrayUtils.prefixSum(a[i]);
		}
		for (int i = 1; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j] += b[i-1][j];
			}
		}
		return b;
	}

	/**
	 * prefixSumの逆操作
	 *
	 * a[i][j] = Σ[i'≤i and j'≤j] b[i'][j']
	 *
	 *
	 *
	 * @param a
	 * @return
	 */
	public static long[][] prefixDiff(long[][] a) {
		//a(x,y)=b(x,y)/(1-x)(1-y)
		long[][] b = new long[a.length][a[0].length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				b[i][j]=a[i][j]-(i==0?0:a[i-1][j])-(j==0?0:a[i][j-1])+(i==0||j==0?0:a[i-1][j-1]);
			}
		}
		return b;
	}

	public static long[][] copyOf(long[][] a, int length0, int length1) {
		long[][]b=new long[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static int[][] copyOf(int[][] a, int length0, int length1) {
		int[][]b=new int[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static short[][] copyOf(short[][] a, int length0, int length1) {
		short[][]b=new short[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static byte[][] copyOf(byte[][] a, int length0, int length1) {
		byte[][]b=new byte[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static char[][] copyOf(char[][] a, int length0, int length1) {
		char[][]b=new char[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static boolean[][] copyOf(boolean[][] a, int length0, int length1) {
		boolean[][]b=new boolean[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static float[][] copyOf(float[][] a, int length0, int length1) {
		float[][]b=new float[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static double[][] copyOf(double[][] a, int length0, int length1) {
		double[][]b=new double[length0][length1];
		for (int i = 0; i < Math.min(a.length, length0); i++) {
			for (int j = 0; j < Math.min(a[i].length, length1); j++) {
				b[i][j]=a[i][j];
			}
		}
		return b;
	}

	public static int[][] copyOfRange2D(int[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    int[][] b = new int[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static long[][] copyOfRange2D(long[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    long[][] b = new long[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static short[][] copyOfRange2D(short[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    short[][] b = new short[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static byte[][] copyOfRange2D(byte[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    byte[][] b = new byte[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static char[][] copyOfRange2D(char[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
		//https://atcoder.jp/contests/abc455/submissions/75278963
	    char[][] b = new char[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static boolean[][] copyOfRange2D(boolean[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    boolean[][] b = new boolean[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static float[][] copyOfRange2D(float[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    float[][] b = new float[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static double[][] copyOfRange2D(double[][] a, int rowFrom, int colFrom, int rowTo, int colTo) {
	    double[][] b = new double[rowTo - rowFrom][colTo - colFrom];
	    for (int i = rowFrom; i < rowTo; i++) {
		for (int j = colFrom; j < colTo; ++j) {
			b[i - rowFrom][j - colFrom] = a[i][j];
		}
	    }
	    return b;
	}

	public static boolean[][] copy(boolean[][] a) {
		boolean[][] b=new boolean[a.length][];
		for (int i = 0; i < a.length; i++) {
			b[i]=ArrayUtils.copy(a[i]);
		}
		return b;
	}

	public static long[][] tolong(int[][] a) {
		long[][] ret=new long[a.length][];
		for (int i = 0; i < a.length; i++) {
			ret[i] = ArrayUtils.tolong(a[i]);
		}
		return ret;
	}

	public static int[][] copy(int[][] a) {
		int[][] b = new int[a.length][];
		Arrays.setAll(b, i -> Arrays.copyOf(a[i], a[i].length));
		return b;
	}

	public static char[][] copy(char[][] a) {
		char[][] b = new char[a.length][];
		Arrays.setAll(b, i -> Arrays.copyOf(a[i], a[i].length));
		return b;
	}

	public static long[][] copy(long[][] a) {
		long[][] b = new long[a.length][];
		Arrays.setAll(b, i -> Arrays.copyOf(a[i], a[i].length));
		return b;
	}

	/**
	 * double 2次元配列のディープコピーを作成します。
	 * O(NM)
	 */
	// 未テスト
	public static double[][] copy(double[][] a) {
		double[][] b = new double[a.length][];
		for (int i = 0; i < a.length; i++) {
			b[i] = Arrays.copyOf(a[i], a[i].length);
		}
		return b;
	}

	/**
	 * 2次元 double 配列の累積和を求めます。
	 * O(NM)
	 */
	// 未テスト
	public static double[][] prefixSum(double[][] a) {
		double[][] b = new double[a.length][];
		for (int i = 0; i < a.length; i++) {
			b[i] = ArrayUtils.prefixSum(a[i]);
		}
		for (int j = 0; j < a[0].length; j++) {
			for (int i = 1; i < a.length; i++) {
				b[i][j] += b[i - 1][j];
			}
		}
		return b;
	}

	public static char[][] rotateRightJagged(char[][] a, char pad) {
		//https://atcoder.jp/contests/abc366/submissions/73573787
		int N=a.length;
        int len = 0;
        for (int i = 0; i < N; i++) {
		len = Math.max(len, a[i].length);
        }
        char[][] ret = new char[len][N];
        ArrayUtils.fill(ret, pad);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < a[i].length; j++) {
                ret[j][(N - 1) - i] = a[i][j];
            }
        }
        for (int i = 0; i < ret.length; i++) {
          int last=ret[i].length-1;
          while(ret[i][last]==pad)last--;
          ret[i]=Arrays.copyOf(ret[i], last+1);
        }
        return ret;
	}

	public static char[][] rotateRightGrid(char[][] a) {
		char[][] b = new char[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][a.length - 1 - i] = a[i][j];
			}
		}
		return b;
	}

	public static int[][] rotateRightGrid(int[][] a) {
		int[][] b = new int[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][a.length - 1 - i] = a[i][j];
			}
		}
		return b;
	}

	public static long[][] rightRotateGrid(long[][] a) {
		long[][] b = new long[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][a.length - 1 - i] = a[i][j];
			}
		}
		return b;
	}

	/**
	 * 未テスト
	 *
	 * 2次元配列を反時計回りに90度回転した2次元配列を返します。
	 * 長さ H, W の2次元配列 a に対し、戻り値の配列 b は W x H の2次元配列であり、
	 * すべての 0 <= i < H, 0 <= j < W に対し b[W - 1 - j][i] = a[i][j] が成り立ちます。
	 *
	 * 計算量: O(HW)
	 *
	 * @param a 反時計回りに回転させる2次元配列
	 * @return 反時計回りに90度回転した2次元配列
	 */
	public static char[][] rotateLeftGrid(char[][] a) {
		char[][] b = new char[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[a[i].length - 1 - j][i] = a[i][j];
			}
		}
		return b;
	}

	/**
	 * 未テスト
	 *
	 * 2次元配列を反時計回りに90度回転した2次元配列を返します。
	 * 長さ H, W の2次元配列 a に対し、戻り値の配列 b は W x H の2次元配列であり、
	 * すべての 0 <= i < H, 0 <= j < W に対し b[W - 1 - j][i] = a[i][j] が成り立ちます。
	 *
	 * 計算量: O(HW)
	 *
	 * @param a 反時計回りに回転させる2次元配列
	 * @return 反時計回りに90度回転した2次元配列
	 */
	public static int[][] rotateLeftGrid(int[][] a) {
		int[][] b = new int[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[a[i].length - 1 - j][i] = a[i][j];
			}
		}
		return b;
	}

	/**
	 * 未テスト
	 *
	 * 2次元配列を反時計回りに90度回転した2次元配列を返します。
	 * 長さ H, W の2次元配列 a に対し、戻り値の配列 b は W x H の2次元配列であり、
	 * すべての 0 <= i < H, 0 <= j < W に対し b[W - 1 - j][i] = a[i][j] が成り立ちます。
	 *
	 * 計算量: O(HW)
	 *
	 * @param a 反時計回りに回転させる2次元配列
	 * @return 反時計回りに90度回転した2次元配列
	 */
	public static long[][] rotateLeftGrid(long[][] a) {
		long[][] b = new long[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[a[i].length - 1 - j][i] = a[i][j];
			}
		}
		return b;
	}

	public static void rotate180(long[][] a) {
	    int n = a.length;
	    int m = a[0].length;
	    for (int i = 0; i < (n + 1) / 2; i++) {
	        for (int j = 0; j < m; j++) {
	            int ni = n - 1 - i;
	            int nj = m - 1 - j;
	            if (i < ni || (i == ni && j < nj)) {
			var tmp = a[i][j];
			a[i][j] = a[ni][nj];
			a[ni][nj] = tmp;
	            }
	        }
	    }
	}

	public static void rotate180(int[][] a) {
	    int n = a.length;
	    int m = a[0].length;
	    for (int i = 0; i < (n + 1) / 2; i++) {
	        for (int j = 0; j < m; j++) {
	            int ni = n - 1 - i;
	            int nj = m - 1 - j;
	            if (i < ni || (i == ni && j < nj)) {
			var tmp = a[i][j];
			a[i][j] = a[ni][nj];
			a[ni][nj] = tmp;
	            }
	        }
	    }
	}

	public static void rotate180(char[][] a) {
		//https://atcoder.jp/contests/abc455/submissions/75278963
	    int n = a.length;
	    int m = a[0].length;
	    for (int i = 0; i < (n + 1) / 2; i++) {
	        for (int j = 0; j < m; j++) {
	            int ni = n - 1 - i;
	            int nj = m - 1 - j;
	            if (i < ni || (i == ni && j < nj)) {
			var tmp = a[i][j];
			a[i][j] = a[ni][nj];
			a[ni][nj] = tmp;
	            }
	        }
	    }
	}

	public static long sum(int[][] a) {
		long ret = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				ret+=a[i][j];
			}
		}
		return ret;
	}

	public static double sum(double[][] a) {
		double ret = 0;
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				ret+=a[i][j];
			}
		}
		return ret;
	}

	public static long modSum(long[][] a, long mod) {
		Fp mo=new Fp(mod);
		long ret = 0;
		for (long[] vals : a) {
			ret += ArrayUtils.modSum(vals, mod);
			ret=mo.reduce(ret);
		}
		return ret;
	}

	public static long modSum(int[][] a, long mod) {
		Fp mo=new Fp(mod);
		long ret = 0;
		for (int[] vals : a) {
			ret += ArrayUtils.modSum(vals, mod);
			ret=mo.reduce(ret);
		}
		return ret;
	}

	public static <T> void rotateColumnUpInplace(T[][] a, int col, int start, int end, int shift) {
	    int n = end - start;
	    if (n == 0 || shift % n == 0) return;
	    int k = (shift % n + n) % n;
	    reverseColumn(a, col, start, start + k);
	    reverseColumn(a, col, start + k, end);
	    reverseColumn(a, col, start, end);
	}

	public static <T> void reverseColumn(T[][] a, int col, int start, int end) {
	    for (int l = start, r = end - 1; l < r; l++, r--) {
	        T tmp = a[l][col];
	        a[l][col] = a[r][col];
	        a[r][col] = tmp;
	    }
	}

	public static void rotateColumnUpInplace(int[][] a, int col, int start, int end, int shift) {
	    int n = end - start;
	    if (n == 0 || shift % n == 0) return;
	    int k = (shift % n + n) % n;
	    reverseColumn(a, col, start, start + k);
	    reverseColumn(a, col, start + k, end);
	    reverseColumn(a, col, start, end);
	}

	public static <T> void reverseColumn(int[][] a, int column, int start, int end) {
	    for (int l = start, r = end - 1; l < r; l++, r--) {
	        var tmp = a[l][column];
	        a[l][column] = a[r][column];
	        a[r][column] = tmp;
	    }
	}

	public static void rotateColumnUpInplace(char[][] a, int column, int startInclusive, int endExclusive, int shift) {
	    int n = endExclusive - startInclusive;
	    if (n == 0 || shift % n == 0) return;
	    int k = (shift % n + n) % n;
	    reverseColumn(a, column, startInclusive, startInclusive + k);
	    reverseColumn(a, column, startInclusive + k, endExclusive);
	    reverseColumn(a, column, startInclusive, endExclusive);
	}

	public static <T> void reverseColumn(char[][] a, int column, int start, int end) {
		for (int l = start, r = end - 1; l < r; l++, r--) {
	        var tmp = a[l][column];
	        a[l][column] = a[r][column];
	        a[r][column] = tmp;
	    }
	}

	public static char[] getColumn(char[][] a, int column) {
		char[]ret=new char[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i]=a[i][column];
		}
		return ret;
	}

	public static int[] getColumn(int[][] a, int column) {
		int[]ret=new int[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i]=a[i][column];
		}
		return ret;
	}

	public static long[] getColumn(long[][] a, int column) {
		long[]ret=new long[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i]=a[i][column];
		}
		return ret;
	}

	public static double[] getColumn(double[][] a, int column) {
		double[]ret=new double[a.length];
		for (int i = 0; i < a.length; i++) {
			ret[i]=a[i][column];
		}
		return ret;
	}

	public static long max(long[][] a) {
		long ret = Long.MIN_VALUE;
		for (int i = 0; i < a.length; ++i) {
			ret = Math.max(ret, ArrayUtils.max(a[i]));
		}
		return ret;
	}

	public static int max(int[][] a) {
		int ret = Integer.MIN_VALUE;
		for (int i = 0; i < a.length; ++i) {
			ret = Math.max(ret, ArrayUtils.max(a[i]));
		}
		return ret;
	}

	public static long min(long[][] a) {
		long ret = Long.MAX_VALUE;
		for (int i = 0; i < a.length; ++i) {
			ret = Math.min(ret, ArrayUtils.min(a[i]));
		}
		return ret;
	}

	public static int min(int[][] a) {
		int ret = Integer.MAX_VALUE;
		for (int i = 0; i < a.length; ++i) {
			ret = Math.min(ret, ArrayUtils.min(a[i]));
		}
		return ret;
	}

	/**
	 * Arrays.comapreでソート
	 */
	public static void sort(int[][] a) {
		Arrays.sort(a, (x, y)->Arrays.compare(x, y));
	}

	/**
	 * Arrays.comapreでソート
	 * https://hos.ac/blog/#blog0002
	 */
	public static void sort(char[][] a) {
		Arrays.sort(a, (x, y)->Arrays.compare(x, y));
	}

	/**
	 * Arrays.comapreでソート
	 */
	public static void sort(long[][] a) {
		Arrays.sort(a, (x, y)->Arrays.compare(x, y));
	}

	/**
	 * Arrays.comapreでソート
	 */
	public static void sort(double[][] a) {
		Arrays.sort(a, (x, y)->Arrays.compare(x, y));
	}

	/**
	 * -Arrays.comapreでソート
	 */
	public static void rsort(int[][] a) {
		Arrays.sort(a, (x, y)->-Arrays.compare(x, y));
	}

	/**
	 * -Arrays.comapreでソート
	 */
	public static void rsort(long[][] a) {
		Arrays.sort(a, (x, y)->-Arrays.compare(x, y));
	}

	/**
	 * -Arrays.comapreでソート
	 */
	public static void rsort(double[][] a) {
		Arrays.sort(a, (x, y)->-Arrays.compare(x, y));
	}

	/**
	 * -Arrays.comapreでソート
	 */
	public static void rsort(char[][] a) {
		Arrays.sort(a, (x, y)->-Arrays.compare(x, y));
	}

	/***
	 * 各行の長さが同じでない場合非対応
	 */
	public static char[][] transpose(char[][] a) {
		char[][] b = new char[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][i] = a[i][j];
			}
		}
		return b;
	}

	/***
	 * 各行の長さが同じでない場合非対応
	 */
	public static int[][] transpose(int[][] a) {
		int[][] b = new int[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][i] = a[i][j];
			}
		}
		return b;
	}

	/***
	 * 各行の長さが同じでない場合非対応
	 */
	public static long[][] transpose(long[][] a) {
		long[][] b = new long[a[0].length][a.length];
		for (int i = 0; i < a.length; ++i) {
			for (int j = 0; j < a[i].length; ++j) {
				b[j][i] = a[i][j];
			}
		}
		return b;
	}

	/**
	 * a[i][j]=ret[i*W+j]; reshapeの逆関数
	 *
	 * @param a
	 * @return
	 */
	public static int[] flatten(int[][] a) {
		int H = a.length;
		int W = a[0].length;
		int[] ret = new int[H * W];
		for (int i = 0; i < H; i++) {
			for (int j = 0; j < W; j++) {
				ret[i * W + j] = a[i][j];
			}
		}
		return ret;
	}

	public static char[][] sortq(char[][] a) {
		//https://atcoder.jp/contests/abc343/submissions/74210454
		char[][]b=copy(a);
		Arrays.sort(b, (x, y) -> Arrays.compare(x, y));
	    int pointer = 1;
	    for (int i = 1; i < b.length; i++) {
	        if (!Arrays.equals(b[pointer - 1], b[i])) {
	            b[pointer++] = b[i];
	        }
	    }
	    return Arrays.copyOf(b, pointer);
	}

	/**
	 * https://hos.ac/blog/#blog0002
	 * @param S
	 * @return
	 * verified:
	 */
	public static int[] argSort(char[][] S) {
		return ArrayUtils.argSort(S, (x, y)->Arrays.compare(x, y));
	}

	/**
	 * a配列の要素の総和を返す
	 * @param a
	 * @return
	 */
	public static long sum(long[][] a) {
		long v=0;
		for (int i = 0; i < a.length; i++) {
			v+=ArrayUtils.sum(a[i]);
		}
		return v;
	}

    /**
     * b[i]=a[indices[i]]となるbを返す。
     * @param s
     * @param indices
     * @return
     */
    public static int[][] take(int[][] s, int[] indices) {
	int[][]b=new int[indices.length][];
	for (int i = 0; i < indices.length; i++) {
			b[i]=s[indices[i]].clone();
		}
	return b;
    }

    public static char[][] take(char[][] s, int[] indices) {
	char[][]b=new char[indices.length][];
	for (int i = 0; i < indices.length; i++) {
			b[i]=s[indices[i]].clone();
		}
	return b;
    }

    public static char[][] take(char[][] s, ArrayList<Integer> indices) {
	char[][]b=new char[indices.size()][];
	for (int i = 0; i < indices.size(); i++) {
			b[i]=s[indices.get(i)].clone();
		}
	return b;
    }

    /**
     * b[i][j]=a[indices1[i]][indces2[j]]となるbを返す。
     * @param s
     * @param indices
     * @return
     */
    public static int[][] take(int[][] s, int[] indices1, int[] indices2) {
	int[][]b=new int[indices1.length][indices2.length];
	for (int i = 0; i < indices1.length; i++) {
		for (int j = 0; j < indices2.length; j++) {
				b[i][j]=s[indices1[i]][indices2[j]];
			}
		}
	return b;
    }

    /**
     * b[i][j]=a[indices1[i]][indces2[j]]となるbを返す。
     * @param s
     * @param indices
     * @return
     */
    public static boolean[][] take(boolean[][] s, int[] indices1, int[] indices2) {
	boolean[][]b=new boolean[indices1.length][indices2.length];
	for (int i = 0; i < indices1.length; i++) {
		for (int j = 0; j < indices2.length; j++) {
				b[i][j]=s[indices1[i]][indices2[j]];
			}
		}
	return b;
    }

    /**
     * b[i][j]=a[indices1[i]][indces2[j]]となるbを返す。
     * @param s
     * @param indices
     * @return
     */
    public static long[][] take(long[][] s, int[] indices1, int[] indices2) {
	long[][]b=new long[indices1.length][indices2.length];
	for (int i = 0; i < indices1.length; i++) {
		for (int j = 0; j < indices2.length; j++) {
				b[i][j]=s[indices1[i]][indices2[j]];
			}
		}
	return b;
    }

    /**
     * b[i][j]=a[indices1[i]][indces2[j]]となるbを返す。
     * @param s
     * @param indices
     * @return
     */
    public static long[][] take(long[][] s, ArrayList<Integer> indices1, ArrayList<Integer> indices2) {
	long[][]b=new long[indices1.size()][indices2.size()];
	for (int i = 0; i < indices1.size(); i++) {
		for (int j = 0; j < indices2.size(); j++) {
				b[i][j]=s[indices1.get(i)][indices2.get(j)];
			}
		}
	return b;
    }

}
