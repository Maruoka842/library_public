package library.util.seq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

public class SortedArraysTest {

    @Test
    public void testLowerInt() {
        int[] a = {1, 3, 5, 7, 9};
        assertEquals(-1, SortedArrays.lower(a, 1));
        assertEquals(0, SortedArrays.lower(a, 2));
        assertEquals(0, SortedArrays.lower(a, 3));
        assertEquals(1, SortedArrays.lower(a, 4));
        assertEquals(4, SortedArrays.lower(a, 10));
    }

    @Test
    public void testLowerLong() {
        long[] a = {1L, 3L, 5L, 7L, 9L};
        assertEquals(-1, SortedArrays.lower(a, 1L));
        assertEquals(0, SortedArrays.lower(a, 2L));
        assertEquals(0, SortedArrays.lower(a, 3L));
        assertEquals(1, SortedArrays.lower(a, 4L));
        assertEquals(4, SortedArrays.lower(a, 10L));
    }

    @Test
    public void testLowerComparable() {
        Integer[] a = {1, 3, 5, 7, 9};
        assertEquals(-1, SortedArrays.lower(a, 1));
        assertEquals(0, SortedArrays.lower(a, 2));
        assertEquals(4, SortedArrays.lower(a, 10));
    }

    @Test
    public void testLowerList() {
        List<Integer> a = Arrays.asList(1, 3, 5, 7, 9);
        assertEquals(-1, SortedArrays.lower(a, 1));
        assertEquals(0, SortedArrays.lower(a, 2));
        assertEquals(4, SortedArrays.lower(a, 10));
    }

    @Test
    public void testChar2D() {
        char[][] a = {{'a', 'a'}, {'a', 'c'}, {'c', 'a'}};
        assertEquals(0, SortedArrays.floor(a, new char[]{'a', 'b'}));
        assertEquals(-1, SortedArrays.lower(a, new char[]{'a', 'a'}));
        assertEquals(1, SortedArrays.lower(a, new char[]{'b', 'a'}));
        assertEquals(1, SortedArrays.ceil(a, new char[]{'a', 'c'}));
        assertEquals(1, SortedArrays.ceil(a, new char[]{'a', 'b'}));
        assertEquals(2, SortedArrays.higher(a, new char[]{'a', 'c'}));
        assertEquals(2, SortedArrays.higher(a, new char[]{'b', 'a'}));
    }

    @Test
    public void testInt2D() {
        int[][] a = {{1, 1}, {1, 3}, {3, 1}};
        assertEquals(0, SortedArrays.floor(a, new int[]{1, 2}));
        assertEquals(-1, SortedArrays.lower(a, new int[]{1, 1}));
        assertEquals(1, SortedArrays.lower(a, new int[]{2, 1}));
        assertEquals(1, SortedArrays.ceil(a, new int[]{1, 3}));
        assertEquals(1, SortedArrays.ceil(a, new int[]{1, 2}));
        assertEquals(2, SortedArrays.higher(a, new int[]{1, 3}));
        assertEquals(2, SortedArrays.higher(a, new int[]{2, 1}));
    }

    @Test
    public void testLong2D() {
        long[][] a = {{1L, 1L}, {1L, 3L}, {3L, 1L}};
        assertEquals(0, SortedArrays.floor(a, new long[]{1L, 2L}));
        assertEquals(-1, SortedArrays.lower(a, new long[]{1L, 1L}));
        assertEquals(1, SortedArrays.lower(a, new long[]{2L, 1L}));
        assertEquals(1, SortedArrays.ceil(a, new long[]{1L, 3L}));
        assertEquals(1, SortedArrays.ceil(a, new long[]{1L, 2L}));
        assertEquals(2, SortedArrays.higher(a, new long[]{1L, 3L}));
        assertEquals(2, SortedArrays.higher(a, new long[]{2L, 1L}));
    }

    @Test
    public void testFunctionFloorLowerCeilHigherIntRange() {
        int[] a = {1, 3, 3, 5, 7};
        IntUnaryOperator f = i -> a[i];

        // Domain [0, 5)
        assertEquals(2, SortedArrays.floor(f, 0, 5, 3));
        assertEquals(2, SortedArrays.floor(f, 0, 5, 4));
        assertEquals(-1, SortedArrays.floor(f, 0, 5, 0));
        assertEquals(4, SortedArrays.floor(f, 0, 5, 10));

        assertEquals(0, SortedArrays.lower(f, 0, 5, 3));
        assertEquals(2, SortedArrays.lower(f, 0, 5, 4));
        assertEquals(-1, SortedArrays.lower(f, 0, 5, 1));

        assertEquals(1, SortedArrays.ceil(f, 0, 5, 3));
        assertEquals(3, SortedArrays.ceil(f, 0, 5, 4));
        assertEquals(5, SortedArrays.ceil(f, 0, 5, 10));

        assertEquals(3, SortedArrays.higher(f, 0, 5, 3));
        assertEquals(3, SortedArrays.higher(f, 0, 5, 4));
        assertEquals(0, SortedArrays.higher(f, 0, 5, 0));
        assertEquals(5, SortedArrays.higher(f, 0, 5, 7));

        // Subdomain [1, 4) -> elements a[1]=3, a[2]=3, a[3]=5
        assertEquals(2, SortedArrays.floor(f, 1, 4, 3));
        assertEquals(0, SortedArrays.floor(f, 1, 4, 2)); // left - 1 = 0
        assertEquals(2, SortedArrays.lower(f, 1, 4, 5));
        assertEquals(0, SortedArrays.lower(f, 1, 4, 3)); // left - 1 = 0
        assertEquals(1, SortedArrays.ceil(f, 1, 4, 3));
        assertEquals(3, SortedArrays.ceil(f, 1, 4, 4));
        assertEquals(4, SortedArrays.ceil(f, 1, 4, 6));  // right = 4
        assertEquals(3, SortedArrays.higher(f, 1, 4, 3));
        assertEquals(4, SortedArrays.higher(f, 1, 4, 5)); // right = 4
    }

    @Test
    public void testFunctionFloorLowerCeilHigherLongRange() {
        long[] a = {10L, 30L, 30L, 50L, 70L};
        IntToLongFunction f = i -> a[i];

        assertEquals(2, SortedArrays.floor(f, 0, 5, 30L));
        assertEquals(0, SortedArrays.lower(f, 0, 5, 30L));
        assertEquals(1, SortedArrays.ceil(f, 0, 5, 30L));
        assertEquals(3, SortedArrays.higher(f, 0, 5, 30L));

        // Subdomain [1, 4) -> elements 30, 30, 50
        assertEquals(2, SortedArrays.floor(f, 1, 4, 30L));
        assertEquals(0, SortedArrays.lower(f, 1, 4, 30L)); // left - 1 = 0
        assertEquals(1, SortedArrays.ceil(f, 1, 4, 30L));
        assertEquals(3, SortedArrays.higher(f, 1, 4, 30L));
    }

    @Test
    public void testFunctionFloorLowerCeilHigherLongDomain() {
        long[] a = {100L, 300L, 300L, 500L, 700L};
        LongUnaryOperator f = i -> a[(int) i];

        assertEquals(2L, SortedArrays.floor(f, 0L, 5L, 300L));
        assertEquals(0L, SortedArrays.lower(f, 0L, 5L, 300L));
        assertEquals(1L, SortedArrays.ceil(f, 0L, 5L, 300L));
        assertEquals(3L, SortedArrays.higher(f, 0L, 5L, 300L));

        // Subdomain [1L, 4L) -> elements 300L, 300L, 500L
        assertEquals(2L, SortedArrays.floor(f, 1L, 4L, 300L));
        assertEquals(0L, SortedArrays.lower(f, 1L, 4L, 300L)); // left - 1 = 0L
        assertEquals(1L, SortedArrays.ceil(f, 1L, 4L, 300L));
        assertEquals(3L, SortedArrays.higher(f, 1L, 4L, 300L));
    }

    @Test
    public void testFunctionFloorLowerCeilHigherLongToIntDomain() {
        int[] a = {10, 30, 30, 50, 70};
        LongToIntFunction f = i -> a[(int) i];

        assertEquals(2L, SortedArrays.floor(f, 0L, 5L, 30));
        assertEquals(0L, SortedArrays.lower(f, 0L, 5L, 30));
        assertEquals(1L, SortedArrays.ceil(f, 0L, 5L, 30));
        assertEquals(3L, SortedArrays.higher(f, 0L, 5L, 30));
    }

    @Test
    public void testFunctionFloorLowerCeilHigherDoubleRange() {
        double[] a = {1.5, 3.5, 3.5, 5.5, 7.5};
        IntToDoubleFunction f = i -> a[i];

        assertEquals(2, SortedArrays.floor(f, 0, 5, 3.5));
        assertEquals(0, SortedArrays.lower(f, 0, 5, 3.5));
        assertEquals(1, SortedArrays.ceil(f, 0, 5, 3.5));
        assertEquals(3, SortedArrays.higher(f, 0, 5, 3.5));

        // Subdomain [1, 4)
        assertEquals(2, SortedArrays.floor(f, 1, 4, 3.5));
        assertEquals(0, SortedArrays.lower(f, 1, 4, 3.5));
        assertEquals(1, SortedArrays.ceil(f, 1, 4, 3.5));
        assertEquals(3, SortedArrays.higher(f, 1, 4, 3.5));

        LongToDoubleFunction fLong = i -> a[(int) i];
        assertEquals(2L, SortedArrays.floor(fLong, 0L, 5L, 3.5));
        assertEquals(0L, SortedArrays.lower(fLong, 0L, 5L, 3.5));
        assertEquals(1L, SortedArrays.ceil(fLong, 0L, 5L, 3.5));
        assertEquals(3L, SortedArrays.higher(fLong, 0L, 5L, 3.5));
    }

    @Test
    public void testFunctionFloorLowerCeilHigherGenericRange() {
        String[] a = {"apple", "banana", "banana", "cherry"};
        IntFunction<String> f = i -> a[i];

        assertEquals(2, SortedArrays.floor(f, 0, 4, "banana"));
        assertEquals(0, SortedArrays.lower(f, 0, 4, "banana"));
        assertEquals(1, SortedArrays.ceil(f, 0, 4, "banana"));
        assertEquals(3, SortedArrays.higher(f, 0, 4, "banana"));

        Comparator<String> comp = Comparator.naturalOrder();
        assertEquals(2, SortedArrays.floor(f, 0, 4, "banana", comp));
        assertEquals(0, SortedArrays.lower(f, 0, 4, "banana", comp));
        assertEquals(1, SortedArrays.ceil(f, 0, 4, "banana", comp));
        assertEquals(3, SortedArrays.higher(f, 0, 4, "banana", comp));

        LongFunction<String> fLong = i -> a[(int) i];
        assertEquals(2L, SortedArrays.floor(fLong, 0L, 4L, "banana"));
        assertEquals(0L, SortedArrays.lower(fLong, 0L, 4L, "banana"));
        assertEquals(1L, SortedArrays.ceil(fLong, 0L, 4L, "banana"));
        assertEquals(3L, SortedArrays.higher(fLong, 0L, 4L, "banana"));
        assertEquals(2L, SortedArrays.floor(fLong, 0L, 4L, "banana", comp));
        assertEquals(0L, SortedArrays.lower(fLong, 0L, 4L, "banana", comp));
        assertEquals(1L, SortedArrays.ceil(fLong, 0L, 4L, "banana", comp));
        assertEquals(3L, SortedArrays.higher(fLong, 0L, 4L, "banana", comp));

        // Subdomain [1, 3)
        assertEquals(2, SortedArrays.floor(f, 1, 3, "banana"));
        assertEquals(0, SortedArrays.lower(f, 1, 3, "banana")); // left - 1 = 0
    }

    @Test
    public void testFunctionCeilDescRange() {
        int[] a = {9, 7, 5, 3, 1};
        IntUnaryOperator f = i -> a[i];
        assertEquals(3, SortedArrays.ceilDesc(f, 0, a.length, 3));
        assertEquals(2, SortedArrays.ceilDesc(f, 0, a.length, 4));
        assertEquals(-1, SortedArrays.ceilDesc(f, 0, a.length, 10));

        // Subdomain [1, 4) -> 7, 5, 3
        assertEquals(3, SortedArrays.ceilDesc(f, 1, 4, 3));
        assertEquals(0, SortedArrays.ceilDesc(f, 1, 4, 8)); // left - 1 = 0

        long[] la = {90L, 70L, 50L, 30L, 10L};
        IntToLongFunction fl = i -> la[i];
        assertEquals(3, SortedArrays.ceilDesc(fl, 0, la.length, 30L));
        assertEquals(2, SortedArrays.ceilDesc(fl, 0, la.length, 40L));

        LongUnaryOperator fLong = i -> la[(int) i];
        assertEquals(3L, SortedArrays.ceilDesc(fLong, 0L, (long) la.length, 30L));
        assertEquals(2L, SortedArrays.ceilDesc(fLong, 0L, (long) la.length, 40L));

        LongToIntFunction fLongInt = i -> a[(int) i];
        assertEquals(3L, SortedArrays.ceilDesc(fLongInt, 0L, (long) a.length, 3));
        assertEquals(2L, SortedArrays.ceilDesc(fLongInt, 0L, (long) a.length, 4));
    }

    @Test
    public void testFunctionIndexOfLastIndexOfRange() {
        int[] a = {1, 3, 3, 3, 5};
        IntUnaryOperator f = i -> a[i];

        assertEquals(1, SortedArrays.indexOf(f, 0, 5, 3));
        assertEquals(3, SortedArrays.lastIndexOf(f, 0, 5, 3));
        assertEquals(-1, SortedArrays.indexOf(f, 0, 5, 2));
        assertEquals(-1, SortedArrays.lastIndexOf(f, 0, 5, 2));

        // Subdomain [2, 5) -> 3, 3, 5
        assertEquals(2, SortedArrays.indexOf(f, 2, 5, 3));
        assertEquals(3, SortedArrays.lastIndexOf(f, 2, 5, 3));
        assertEquals(-1, SortedArrays.indexOf(f, 2, 5, 1));

        long[] la = {10L, 30L, 30L, 30L, 50L};
        IntToLongFunction fl = i -> la[i];
        assertEquals(1, SortedArrays.indexOf(fl, 0, 5, 30L));
        assertEquals(3, SortedArrays.lastIndexOf(fl, 0, 5, 30L));

        LongUnaryOperator fLong = i -> la[(int) i];
        assertEquals(1L, SortedArrays.indexOf(fLong, 0L, 5L, 30L));
        assertEquals(3L, SortedArrays.lastIndexOf(fLong, 0L, 5L, 30L));

        LongToIntFunction fLongInt = i -> a[(int) i];
        assertEquals(1L, SortedArrays.indexOf(fLongInt, 0L, 5L, 3));
        assertEquals(3L, SortedArrays.lastIndexOf(fLongInt, 0L, 5L, 3));

        String[] sa = {"a", "b", "b", "c"};
        IntFunction<String> fs = i -> sa[i];
        assertEquals(1, SortedArrays.indexOf(fs, 0, sa.length, "b"));
        assertEquals(2, SortedArrays.lastIndexOf(fs, 0, sa.length, "b"));
        assertEquals(1, SortedArrays.indexOf(fs, 0, sa.length, "b", Comparator.naturalOrder()));
        assertEquals(2, SortedArrays.lastIndexOf(fs, 0, sa.length, "b", Comparator.naturalOrder()));

        LongFunction<String> fsLong = i -> sa[(int) i];
        assertEquals(1L, SortedArrays.indexOf(fsLong, 0L, (long) sa.length, "b"));
        assertEquals(2L, SortedArrays.lastIndexOf(fsLong, 0L, (long) sa.length, "b"));
        assertEquals(1L, SortedArrays.indexOf(fsLong, 0L, (long) sa.length, "b", Comparator.naturalOrder()));
        assertEquals(2L, SortedArrays.lastIndexOf(fsLong, 0L, (long) sa.length, "b", Comparator.naturalOrder()));
    }

    @Test
    public void testFunctionCountsRange() {
        int[] a = {1, 3, 5, 7, 9};
        IntUnaryOperator f = i -> a[i];

        assertEquals(3, SortedArrays.countLeq(f, 0, 5, 5));
        assertEquals(2, SortedArrays.countLess(f, 0, 5, 5));
        assertEquals(3, SortedArrays.countGeq(f, 0, 5, 5));
        assertEquals(2, SortedArrays.countGreater(f, 0, 5, 5));

        // Subdomain [1, 4) -> 3, 5, 7
        assertEquals(2, SortedArrays.countLeq(f, 1, 4, 5));
        assertEquals(1, SortedArrays.countLess(f, 1, 4, 5));
        assertEquals(2, SortedArrays.countGeq(f, 1, 4, 5));
        assertEquals(1, SortedArrays.countGreater(f, 1, 4, 5));

        long[] la = {1L, 3L, 5L, 7L, 9L};
        IntToLongFunction fl = i -> la[i];
        assertEquals(3, SortedArrays.countLeq(fl, 0, 5, 5L));
        assertEquals(2, SortedArrays.countLess(fl, 0, 5, 5L));
        assertEquals(3, SortedArrays.countGeq(fl, 0, 5, 5L));
        assertEquals(2, SortedArrays.countGreater(fl, 0, 5, 5L));

        LongUnaryOperator fLong = i -> la[(int) i];
        assertEquals(3L, SortedArrays.countLeq(fLong, 0L, 5L, 5L));
        assertEquals(2L, SortedArrays.countLess(fLong, 0L, 5L, 5L));
        assertEquals(3L, SortedArrays.countGeq(fLong, 0L, 5L, 5L));
        assertEquals(2L, SortedArrays.countGreater(fLong, 0L, 5L, 5L));

        LongToIntFunction fLongInt = i -> a[(int) i];
        assertEquals(3L, SortedArrays.countLeq(fLongInt, 0L, 5L, 5));
        assertEquals(2L, SortedArrays.countLess(fLongInt, 0L, 5L, 5));
        assertEquals(3L, SortedArrays.countGeq(fLongInt, 0L, 5L, 5));
        assertEquals(2L, SortedArrays.countGreater(fLongInt, 0L, 5L, 5));

        double[] da = {1.0, 3.0, 5.0, 7.0, 9.0};
        IntToDoubleFunction fd = i -> da[i];
        assertEquals(3, SortedArrays.countLeq(fd, 0, 5, 5.0));
        assertEquals(2, SortedArrays.countLess(fd, 0, 5, 5.0));
        assertEquals(3, SortedArrays.countGeq(fd, 0, 5, 5.0));
        assertEquals(2, SortedArrays.countGreater(fd, 0, 5, 5.0));

        LongToDoubleFunction fdLong = i -> da[(int) i];
        assertEquals(3L, SortedArrays.countLeq(fdLong, 0L, 5L, 5.0));
        assertEquals(2L, SortedArrays.countLess(fdLong, 0L, 5L, 5.0));
        assertEquals(3L, SortedArrays.countGeq(fdLong, 0L, 5L, 5.0));
        assertEquals(2L, SortedArrays.countGreater(fdLong, 0L, 5L, 5.0));
    }

    @Test
    public void testFunctionKthOnComplementSetRange() {
        long[] a = {3L, 5L, 6L, 9L};
        IntToLongFunction fl = i -> a[i];
        assertEquals(4L, SortedArrays.kthOnComplementSet(fl, 0, a.length, 1L, 2L));

        LongUnaryOperator fLong = i -> a[(int) i];
        assertEquals(4L, SortedArrays.kthOnComplementSet(fLong, 0L, (long) a.length, 1L, 2L));
    }
}
