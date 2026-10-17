package library.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link JavaToCpp} の回帰テスト。 */
public class JavaToCppTest {
    /** テスト用ディレクトリ。 */
    @TempDir
    Path tempDir;

    /**
     * MergeFiles 形式のクラス、配列、ループ、入出力、文字列連結が C++17 表記へ変換されることを検証する。
     * 計算量: O(L)。L はテストソース長。
     *
     * @throws Exception 入出力または構文解析に失敗した場合
     */
    @Test
    // 未テスト
    void convertsTypicalMergedProgram() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    static MyPrintWriter pw = MyPrintWriter.getInstance();
                    static FastScanner sc = FastScanner.getInstance();
                    public static void main(String[] args) { new Main().run(args); pw.flush(); }
                    void run(String[] args) {
                        int n = sc.nextInt();
                        int[] a = sc.nextInts(n);
                        java.util.Arrays.sort(a);
                        for (int i = 0; i < a.length; i++) System.out.println("v=" + a[i]);
                    }
                }
                class FastScanner {}
                class MyPrintWriter {}
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("vector<int> a") && cpp.contains("nextInts(n)"));
        assertTrue(cpp.contains("jsort(a);"));
        assertTrue(cpp.contains("i < static_cast<int>(a.size())"));
        assertTrue(cpp.contains("jconcat(JString(\"v=\"), a[i])"));
        assertTrue(cpp.contains("Main::javaMain({});"));
    }

    /**
     * split, substring, valueOf, および三項演算子内での配列初期化子の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsStringMethodsAndArrayTernary() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        String s = "12-34";
                        String[] parts = s.split("-");
                        int a = Integer.valueOf(parts[0].substring(0, 2));
                        char[] chars = s.length() > 0 ? new char[] {'a', 'b'} : new char[] {'c'};
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("jsplit(s, JString(\"-\"))"));
        assertTrue(cpp.contains("JInteger::valueOf(jsubstring(parts[0], 0, 2))"));
        assertTrue(cpp.contains("vector<char>{'a', 'b'}"));
    }

    /**
     * Scanner.nextLine 及び IntStream.range().toArray() の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsScannerNextLineAndIntStream() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Scanner;
                import java.util.stream.IntStream;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        String line = sc.nextLine();
                        int[] range = IntStream.range(1, 10).toArray();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("sc.nextLine()"));
        assertTrue(cpp.contains("JIntStream::range(1, 10).toArray()"));
    }

    /**
     * String.equals の C++ == 演算子への変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void convertsStringEquals() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        String s = "abc";
                        if (s.equals("abc")) {
                            System.out.println("yes");
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("(s == JString(\"abc\"))"));
    }

    /**
     * toUpperCase, toLowerCase, equalsIgnoreCase, InputStream 及び ByteArrayInputStream の変換を検証する。
     * 計算量: O(L)。L はテストソース長。
     *
     * @throws Exception 入出力または構文解析に失敗した場合
     */
    @Test
    // 未テスト
    void convertsStringCasingAndInputStream() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.InputStream;
                import java.io.ByteArrayInputStream;

                public class Main {
                    static InputStream is;
                    public static void main(String[] args) throws Exception {
                        String s = "Hello";
                        String u = s.toUpperCase();
                        String l = s.toLowerCase();
                        boolean eq = s.equalsIgnoreCase("hello");
                        boolean empty = s.isEmpty();
                        double d = Double.parseDouble("3.14");
                        is = new ByteArrayInputStream(s.getBytes());
                    }
                    public char[] dummy() {
                        return null;
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("jtoUpperCase(s)"));
        assertTrue(cpp.contains("jtoLowerCase(s)"));
        assertTrue(cpp.contains("jequalsIgnoreCase(s, JString(\"hello\"))"));
        assertTrue(cpp.contains("s.empty()"));
        assertTrue(cpp.contains("JDouble::parseDouble"));
        assertTrue(cpp.contains("JInputStream(jchars(s))"));
        assertTrue(cpp.contains("return {};"));
    }

    /**
     * BufferedReader, InputStreamReader, System.out.flush() 及び Math.log の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void convertsBufferedReaderAndSystemFlush() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.BufferedReader;
                import java.io.InputStreamReader;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        String line = br.readLine();
                        System.out.println(line);
                        System.out.flush();
                        double val = Math.log(10.0);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JInputStream br = JInputStream("));
        assertTrue(cpp.contains("br.readLine()"));
        assertTrue(cpp.contains("cout.flush();"));
        assertTrue(cpp.contains("log(10.0)"));
    }

    /**
     * メソッド内のローカルクラス、JPriorityQueue、Iterator 及び Arrays.asList の変換を検証する。
     * 計算量: O(L)。
     */
    /**
     * Math.PI, Collections.sort, Stream.max, Line2D, JPrintWriter.print 及び Generic static methods の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void convertsMathPiCollectionsSortStreamMaxAndLine2D() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.awt.geom.Line2D;
                import java.util.Collections;
                import java.util.List;
                import java.util.ArrayList;

                public class Main {
                    public static void main(String[] args) {
                        double pi = Math.PI;
                        List<Integer> list = new ArrayList<>();
                        Collections.sort(list);
                        int ccw = Line2D.relativeCCW(0, 0, 1, 1, 2, 2);
                        int maxVal = list.stream().mapToInt(x -> x).max().getAsInt();
                        List<Integer>[] arr = ArrayLists.<Integer>newArrayOfArrayLists(5);
                    }
                }
                class ArrayLists {
                    public static <T> List<T>[] newArrayOfArrayLists(int n) {
                        return new List[n];
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JMath::PI"));
        assertTrue(cpp.contains("JCollections::sort(list)"));
        assertTrue(cpp.contains("Line2D::relativeCCW(0, 0, 1, 1, 2, 2)"));
        assertTrue(cpp.contains("ArrayLists::newArrayOfArrayLists"));
    }

    @Test
    void convertsLocalClassAndPriorityQueue() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;
                import java.util.Iterator;
                import java.util.PriorityQueue;

                public class Main {
                    public static void main(String[] args) {
                        class Node implements Comparable<Node> {
                            int cost;
                            Node(int cost) { this.cost = cost; }
                            public int compareTo(Node o) { return Integer.compare(cost, o.cost); }
                        }
                        PriorityQueue<Node> pq = new PriorityQueue<>();
                        pq.add(new Node(5));
                        Iterator<String> it = Arrays.asList("a", "b").iterator();
                        while (it != null && it.hasNext()) {
                            System.out.println(it.next());
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("struct Node"));
        assertTrue(cpp.contains("JPriorityQueue<shared_ptr<Node>>") || cpp.contains("JPriorityQueue<shared_ptr<struct Node>>"));
        assertTrue(cpp.contains("JIterator<JString> it = JIterator("));
    }

    /**
     * Java標準APIと同名のユーザー定義メソッドを標準API用ランタイムへ置換しないことを検証する。
     * 配列null、Arrays.asList、およびComparator付きCollections.sortの型文脈変換も検証する。
     * 計算量: O(L)。L はテストソース長。
     *
     * @throws Exception 入出力または構文解析に失敗した場合
     */
    @Test
    // 未テスト
    void usesDeclaringTypesInsteadOfMethodNames() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;
                import java.util.Collections;
                import java.util.List;

                public class Main {
                    static int[] nullable(boolean empty) {
                        return empty ? null : new int[] {1, 2};
                    }
                    public static void main(String[] args) {
                        Custom x = new Custom();
                        Custom y = new Custom();
                        x.substring(1);
                        x.toUpperCase();
                        x.isEmpty();
                        x.iterator();
                        x.equals(y);
                        List<String> values = Arrays.asList("b", "a");
                        Collections.sort(values, (a, b) -> a.compareTo(b));
                    }
                }
                class Custom {
                    String substring(int begin) { return "custom"; }
                    String toUpperCase() { return "custom"; }
                    boolean isEmpty() { return false; }
                    int iterator() { return 1; }
                    boolean equals(Custom other) { return this == other; }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("x->substring(1)"));
        assertTrue(cpp.contains("x->toUpperCase()"));
        assertTrue(cpp.contains("x->isEmpty()"));
        assertTrue(cpp.contains("x->iterator()"));
        assertTrue(cpp.contains("x->equals(y)"));
        assertTrue(cpp.contains("jlistOf(JString(\"b\"), JString(\"a\"))"));
        assertTrue(cpp.contains("return (empty ? vector<int>{} : vector<int>{1, 2});"));
    }

    /**
     * Date, Scanner.close, および System.currentTimeMillis の変換を検証する。
     * 計算量: O(L)。L はテストソース長。
     *
     * @throws Exception 入出力または構文解析に失敗した場合
     */
    @Test
    // 未テスト
    void convertsDateScannerCloseAndCurrentTimeMillis() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Date;
                import java.util.Scanner;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        Date d = new Date();
                        long t1 = d.getTime();
                        long t2 = System.currentTimeMillis();
                        long t3 = new Date().getTime();
                        sc.close();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JDate d = JDate();"));
        assertTrue(cpp.contains("jcurrentTimeMillis()"));
        assertTrue(cpp.contains("JDate().getTime()"));
        assertTrue(cpp.contains("sc.close();"));
    }

    /**
     * List.sort および Arrays.asList(String[]) の変換と C++ 利用を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsListSortAndArraysAsListVector() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;
                import java.util.List;
                import java.util.ArrayList;

                public class Main {
                    public static void main(String[] args) {
                        List<String> list = new ArrayList<>(Arrays.asList("3", "1", "2"));
                        list.sort((a, b) -> a.compareTo(b));
                        String[] arr = "a b c".split(" ");
                        List<String> list2 = Arrays.asList(arr);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("list.sort("));
        assertTrue(cpp.contains("jsplit("));
    }

    /**
     * Iterable, BigInteger, System.out.printf および遅延メソッド戻り値型のスコープ修飾の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsIterableBigIntegerAndPrintf() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;

                public class Main {
                    public static void main(String[] args) {
                        BigInteger b = new BigInteger("100");
                        BigInteger b2 = BigInteger.valueOf(10);
                        BigInteger sum = b.add(b2);
                        System.out.printf("%d\\n", sum.longValue());
                        Iterable<String> it = new java.util.ArrayList<>();
                    }
                    public Outer.Inner dummy() { return null; }
                }
                class Outer {
                    static class Inner {}
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JBigInteger b = JBigInteger(JString(\"100\"));"));
        assertTrue(cpp.contains("JBigInteger::valueOf(10)"));
        assertTrue(cpp.contains("jprintf(JString(\"%d\\n\"), sum.longValue())"));
        assertTrue(cpp.contains("JIterable<JString> it ="));
        assertTrue(cpp.contains("Outer::Inner"));
    }

    /**
     * 三項演算子での String と プリミティブの型不一致に対する jstr ラップ変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsTernaryStringAndPrimitive() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        int v = 5;
                        String res = v >= 10 ? "error" : String.valueOf(v);
                        System.out.println(v >= 10 ? "error" : v);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("((v >= 10) ? JString(\"error\") : jstr(v))"));
    }

    /**
     * LinkedList および String.indexOf / lastIndexOf の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsLinkedListAndStringIndexOf() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.LinkedList;

                public class Main {
                    public static void main(String[] args) {
                        LinkedList<String> list = new LinkedList<>();
                        list.add("hello");
                        String s = "MARCH";
                        int idx = s.indexOf('A');
                        int lidx = s.lastIndexOf("CH");
                        System.out.println(idx + lidx);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JList<JString> list = JList<JString>();"));
        assertTrue(cpp.contains("jindexOf(s, 'A')"));
        assertTrue(cpp.contains("jlastIndexOf(s, JString(\"CH\"))"));
    }

    /**
     * BiPredicate, assert, List.of/Set.of, local classes with static variables, JDeque.peek and stream methods are correctly converted.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsNewFeatures() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.ArrayDeque;
                import java.util.Deque;
                import java.util.List;
                import java.util.Set;
                import java.util.function.BiPredicate;

                public class Main {
                    public static void main(String[] args) {
                        int x = 10;
                        assert x > 0;
                        BiPredicate<Integer, Integer> pred = (a, b) -> a > b;
                        List<Integer> list = List.of(1, 2, 3);
                        Set<Integer> set = Set.of(4, 5);
                        Deque<Integer> dq = new ArrayDeque<>();
                        dq.add(10);
                        int peeked = dq.peek();

                        class LocalStruct {
                            int getX() { return x; }
                        }

                        int val = list.stream().min(Integer::compare).get();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("assert((x > 0));"));
        assertTrue(cpp.contains("function<bool(int, int)> pred"));
        assertTrue(cpp.contains("jlistOf(1, 2, 3)"));
        assertTrue(cpp.contains("jsetOf(4, 5)"));
        assertTrue(cpp.contains("dq.peek()"));
        assertTrue(cpp.contains("static int x;"));
        assertTrue(cpp.contains("list.stream()"));
    }

    /**
     * BigInteger.nextProbablePrime, String.format, and Integer/Long.reverse conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsBitReverseAndStringFormatAndNextProbablePrime() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;

                public class Main {
                    public static void main(String[] args) {
                        int r = Integer.reverse(12345);
                        long lr = Long.reverse(123456789012345L);
                        String s = String.format("%.20f", 3.14159);
                        BigInteger p = BigInteger.valueOf(100).nextProbablePrime();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JInteger::reverse(12345)"));
        assertTrue(cpp.contains("JLong::reverse(123456789012345LL)"));
        assertTrue(cpp.contains("jformat("));
        assertTrue(cpp.contains(".nextProbablePrime()"));
    }

    /**
     * Math.signum and Map.merge conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsMathSignumAndMapMerge() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.HashMap;
                import java.util.Map;

                public class Main {
                    public static void main(String[] args) {
                        double sign = Math.signum(-5.5);
                        Map<String, Integer> map = new HashMap<>();
                        map.merge("key", 1, Integer::sum);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("jsignum("));
        assertTrue(cpp.contains("map.merge("));
    }

    /**
     * IntStream.range(...).mapToLong(...) conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsStreamMapToLong() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.stream.IntStream;

                public class Main {
                    public static void main(String[] args) {
                        long[] arr = IntStream.range(0, 10).mapToLong(i -> (long) i * i).toArray();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("mapToLong("));
        assertTrue(cpp.contains(".toArray()"));
    }

    /**
     * BigInteger.TWO, Collections.reverseOrder(), compareTo, and null comparison conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void convertsNewFeaturesTest() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;
                import java.util.Collections;
                import java.util.PriorityQueue;

                public class Main {
                    public static void main(String[] args) {
                        BigInteger two = BigInteger.TWO;
                        PriorityQueue<Long> pq = new PriorityQueue<>(Collections.reverseOrder());
                        Long v = 10L;
                        if (v != null) {
                            int cmp = v.compareTo(5L);
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JBigInteger::TWO"));
        assertTrue(cpp.contains("JCollections::reverseOrder()"));
        assertTrue(cpp.contains("jcompareTo("));
        assertTrue(cpp.contains("jisNull("));
    }

    /**
     * 文字リテラルエスケープ、pattern matching instanceof、テンプレートパラメータシャドウ、
     * カスタム Iterable 範囲 loop (begin/end)、PrimitiveIterator, Array, および static_pointer_cast の変換を検証する。
     * 計算量: O(L)。
     *
     * @throws Exception 入出力または構文解析に失敗した場合
     */
    @Test
    // 未テスト
    void convertsNewFeaturesBatch200() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.lang.reflect.Array;
                import java.util.Iterator;
                import java.util.PrimitiveIterator;

                public class Main {
                    public static void main(String[] args) {
                        char cr = '\\r';
                        char nl = '\\n';
                        Object obj = new int[] {1, 2, 3};
                        if (obj instanceof int[] arr) {
                            int len = Array.getLength(arr);
                        }
                        CustomIterable container = new CustomIterable();
                        for (String item : container) {
                            System.out.println(item);
                        }
                        PrimitiveIterator.OfInt pit = null;
                        Base b = new Derived();
                        Derived d = (Derived) b;
                    }
                }
                class CustomIterable implements Iterable<String> {
                    public Iterator<String> iterator() { return null; }
                }
                class GenericOuter<T> {
                    public <T> void foo(T item) {}
                }
                class Base {}
                class Derived extends Base {}
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("'\\r'"));
        assertTrue(cpp.contains("'\\n'"));
        assertTrue(cpp.contains("jisInstance("));
        assertTrue(cpp.contains("Array::getLength"));
        assertTrue(cpp.contains("CppIterator begin()"));
        assertTrue(cpp.contains("PrimitiveIterator::OfInt"));
        assertTrue(cpp.contains("template <class TMethod>"));
        assertTrue(cpp.contains("static_pointer_cast<Derived>") || cpp.contains("static_pointer_cast<struct Derived>"));
    }

    @Test
    void testRegressionFixes(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        double sqrt = 2.0;
                        double val = Math.sqrt(sqrt);
                        Reachable r = new Reachable(null);
                        Long l = r.get();
                    }
                }
                class Reachable {
                    public Reachable(long[] arr) {}
                    public Long get() {
                        return null;
                    }
                }
                class UnreachableDeadClass {
                    public void unused() {}
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("std::sqrt"));
        assertTrue(cpp.contains("(long long){}"));
        assertTrue(cpp.contains("make_shared<Reachable>") || cpp.contains("make_shared<struct Reachable>"));
        assertTrue(cpp.contains("(vector<long long>){}"));
        assertTrue(cpp.contains("struct Reachable"));
    }

    @Test
    void testNewBatchFeaturesAndFixes(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.stream.IntStream;
                public class Main {
                    Node root = new Node();
                    public static void main(String[] args) {
                        int r = IntStream.range(0, 5).reduce((i, j) -> i + j).orElse(0);
                        String s = Long.toBinaryString(10L);
                    }
                    void dfs(int a, int b, int c, int d) {}
                    class Node {
                        void dfs(int a, int b) {
                            dfs(a, b);
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("reduce"));
        assertTrue(cpp.contains("toBinaryString"));
        assertTrue(cpp.contains("make_shared<Node>(nullptr)"));
        assertTrue(cpp.contains("outer->dfs(a, b)") || cpp.contains("this->dfs(a, b)"));
    }

    /**
     * Comparator.naturalOrder(), String::length, JList(container), outer static field access, and generic parameter null initializer conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void test100SubmissionsBatchFeatures(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Comparator;
                import java.util.List;
                import java.util.ArrayList;
                import java.util.Set;
                import java.util.HashSet;

                public class Main {
                    static long mod = 998244353;

                    static class V {
                        static void merge() {
                            long m = mod;
                        }
                    }

                    public static void main(String[] args) {
                        List<String> list = new ArrayList<>();
                        list.sort(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()));
                        Set<Long> set = new HashSet<>();
                        List<Long> listFromSet = new ArrayList<>(set);
                        SegTree<Long> tree = new SegTree<>();
                        tree.set(0, null);
                    }
                }
                class SegTree<T> {
                    public void set(int k, T val) {}
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JComparatorUtils::naturalOrder()"));
        assertTrue(cpp.contains("x.length()"));
        assertTrue(cpp.contains("Main::mod"));
        assertTrue(cpp.contains("tree->set(0, {})"));
    }

    /**
     * System.gc(), PriorityQueue.clear(), BigInteger.shiftLeft/testBit/or, Exception.printStackTrace(), StringBuilder.appendCodePoint/reverse conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void testBatch100Fixes(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;
                import java.util.PriorityQueue;

                public class Main {
                    public static void main(String[] args) {
                        System.gc();
                        PriorityQueue<Integer> pq = new PriorityQueue<>();
                        pq.clear();
                        BigInteger b = BigInteger.ONE;
                        b = b.shiftLeft(2).or(BigInteger.TEN);
                        boolean bit = b.testBit(1);
                        StringBuilder sb = new StringBuilder("abc");
                        sb.appendCodePoint(100);
                        sb.reverse();
                        try {
                            int x = 1 / 0;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("pq.clear()"));
        assertTrue(cpp.contains("shiftLeft(2)"));
        assertTrue(cpp.contains("or_("));
        assertTrue(cpp.contains("testBit(1)"));
        assertTrue(cpp.contains("appendCodePoint(100)"));
        assertTrue(cpp.contains("reverse()"));
    }

    /**
     * JRandom, JComparator.thenComparingInt, JList.subList, and Integer::intValue conversion test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void testJRandomComparatorSubListAndMethodRef(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.ArrayList;
                import java.util.Comparator;
                import java.util.List;
                import java.util.Random;

                public class Main {
                    public static void main(String[] args) {
                        Random rnd = new Random();
                        int rInt = rnd.nextInt();
                        double rDbl = rnd.nextDouble();
                        boolean rBool = rnd.nextBoolean();

                        List<Integer> list = new ArrayList<>();
                        list.add(1);
                        list.add(2);
                        list.add(3);
                        List<Integer> sub = list.subList(0, 2);

                        list.sort(Comparator.comparing(Integer::intValue).thenComparingInt(x -> x));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("rnd.nextInt()"));
        assertTrue(cpp.contains("rnd.nextDouble()"));
        assertTrue(cpp.contains("rnd.nextBoolean()"));
        assertTrue(cpp.contains("list.subList(0, 2)"));
        assertTrue(cpp.contains("thenComparingInt("));
    }

    /**
     * Math.random and raw Map assignment test.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void testMathRandomAndMapAssignment(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.HashMap;
                import java.util.Map;

                public class Main {
                    static Map[] maps = new HashMap[2];

                    public static void main(String[] args) {
                        double r = Math.random();
                        maps[0] = new HashMap<Long, Long>();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JRandom().nextDouble()"));
        assertTrue(cpp.contains("JMap<long long, long long>()"));
    }

    /**
     * Test features added in batch 2330: new String(), JDeque.contains(), recursive anonymous comparator calls, and BigDecimal.
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void testBatch2330Features(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigDecimal;
                import java.util.ArrayDeque;
                import java.util.Arrays;
                import java.util.Comparator;

                public class Main {
                    public static void main(String[] args) {
                        String s = new String();
                        ArrayDeque<Integer> dq = new ArrayDeque<>();
                        dq.add(1);
                        boolean has = dq.contains(1);

                        BigDecimal bd = BigDecimal.valueOf(3.14);
                        BigDecimal bd2 = bd.multiply(BigDecimal.valueOf(2.0)).subtract(BigDecimal.ONE);

                        Integer[] a = new Integer[]{1, 2, 3};
                        Arrays.sort(a, new Comparator<Integer>() {
                            @Override
                            public int compare(Integer o1, Integer o2) {
                                if (o1.equals(o2)) return 0;
                                return compare(o1, o2);
                            }
                        });
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JString s = JString();"));
        assertTrue(cpp.contains("dq.contains(1)"));
        assertTrue(cpp.contains("JBigDecimal bd = JBigDecimal::valueOf(3.14);"));
        assertTrue(cpp.contains("__self(__self, o1, o2)"));
    }

    /**
     * BigInteger.signum(), List.sort(null), JList.remove(Object) の変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void testBatch2360Features(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;
                import java.util.ArrayList;
                import java.util.List;

                public class Main {
                    public static void main(String[] args) {
                        BigInteger b = BigInteger.valueOf(123);
                        int s = b.signum();

                        List<Integer> list = new ArrayList<>();
                        list.add(1);
                        list.sort(null);

                        List<Pair> pairs = new ArrayList<>();
                        pairs.remove(1L);
                    }
                    static class Pair {
                        long key;
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("b.signum()"));
        assertTrue(cpp.contains("list.sort("));
        assertTrue(cpp.contains("pairs.remove(1LL)"));
    }

    /**
     * StringTokenizer および JInputStream オーバーロードの変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void testStringTokenizerSupport(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.BufferedReader;
                import java.io.InputStreamReader;
                import java.util.StringTokenizer;

                public class Main {
                    public static void main(String[] args) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in), 32768);
                        StringTokenizer st = new StringTokenizer("hello world");
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JStringTokenizer st = JStringTokenizer(JString(\"hello world\"));"));
        assertTrue(cpp.contains("st.hasMoreTokens()"));
        assertTrue(cpp.contains("st.nextToken()"));
    }

    /**
     * フィールドと同名のボディ付きメソッドがダミーゲッターに置換されず正しく出力されることを検証する。
     * 計算量: O(L)。 L は Java ソース文字列の長さ。
     */
    // 未テスト
    @Test
    void testFieldMethodNameCollisionWithBody(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    static class Node {
                        int hit = 0;
                        void hit() {
                            ++hit;
                        }
                    }
                    public static void main(String[] args) {
                        Node n = new Node();
                        n.hit();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("virtual void hit()"));
        assertTrue(cpp.contains("hit_"));
    }

    /**
     * 生の型のコンストラクタ呼び出しが変数宣言の型引数を推論して変換されることを検証する。
     * 計算量: O(L)。 L は Java ソース文字列の長さ。
     */
    // 未テスト
    @Test
    void testRawContainerConstructorTypeInference(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.PriorityQueue;

                public class Main {
                    static class S implements Comparable<S> {
                        public int compareTo(S o) { return 0; }
                    }
                    public static void main(String[] args) {
                        PriorityQueue<S> pq = new PriorityQueue();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JPriorityQueue<shared_ptr<struct S>> pq = JPriorityQueue<shared_ptr<struct S>>();"));
    }

    /**
     * Thread の run メソッドおよび追加コンストラクタオーバーロードの変換を検証する。
     * 計算量: O(L)。 L は Java ソース文字列の長さ。
     */
    // 未テスト
    @Test
    void testThreadRunAndConstructors(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main implements Runnable {
                    public void run() {}
                    public static void main(String[] args) {
                        new Thread(null, new Main(), "", 1024L).run();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JThread(nullptr, make_shared<Main>(), JString(\"\"), 1024LL).run();"));
    }

    /**
     * Math.floorDiv, Math.ceilDiv, Arrays.binarySearch の異型比較変換を検証する。
     * 計算量: O(L)。 L は Java ソース文字列の長さ。
     */
    // 未テスト
    @Test
    void testMathFloorDivAndBinarySearch(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;
                public class Main {
                    public static void main(String[] args) {
                        long a = Math.floorDiv(-5L, 2);
                        long[] x = new long[]{10L, 20L};
                        int idx = Arrays.binarySearch(x, 15);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("jfloorDiv((-5LL), 2)"));
        assertTrue(cpp.contains("jbsearch(x, 15)"));
    }

    /**
     * Stream.allMatch / anyMatch / noneMatch, Java Record field/getter name shadowing, PriorityQueue.remove, and List.contains type mismatch tests.
     * 計算量: O(L)。
     */
    @Test
    void testStreamMatchRecordAndPQRemove(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.ArrayList;
                import java.util.List;
                import java.util.PriorityQueue;
                import java.util.stream.IntStream;

                public class Main {
                    public record Point(long x, long y) {}

                    public static void main(String[] args) {
                        boolean all = IntStream.range(0, 10).allMatch(i -> i >= 0);
                        boolean any = IntStream.range(0, 10).anyMatch(i -> i == 5);
                        boolean none = IntStream.range(0, 10).noneMatch(i -> i < 0);

                        Point p = new Point(1, 2);
                        long px = p.x();

                        PriorityQueue<Integer> pq = new PriorityQueue<>();
                        pq.add(10);
                        pq.remove(10);

                        List<Integer> list = new ArrayList<>();
                        boolean containsSub = list.contains(list);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("allMatch("));
        assertTrue(cpp.contains("anyMatch("));
        assertTrue(cpp.contains("noneMatch("));
        assertTrue(cpp.contains("x_ = x"));
        assertTrue(cpp.contains("return this->x_;"));
        assertTrue(cpp.contains("pq.remove(10)"));
        assertTrue(cpp.contains("list.contains(list)"));
    }

    /**
     * java.util.BitSet の C++17 JBitSet 変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void convertsBitSet() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.BitSet;

                public class Main {
                    public static void main(String[] args) {
                        BitSet bs = new BitSet(100);
                        bs.set(5);
                        boolean b = bs.get(5);
                        int count = bs.cardinality();
                        BitSet other = new BitSet();
                        bs.and(other);
                        bs.or(other);
                        bs.xor(other);
                        int next = bs.nextSetBit(0);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JBitSet bs = JBitSet(100);"));
        assertTrue(cpp.contains("bs.set(5);"));
        assertTrue(cpp.contains("bs.get(5)"));
        assertTrue(cpp.contains("bs.cardinality()"));
        assertTrue(cpp.contains("bs.and_(other);"));
        assertTrue(cpp.contains("bs.or_(other);"));
        assertTrue(cpp.contains("bs.xor_(other);"));
        assertTrue(cpp.contains("bs.nextSetBit(0)"));
    }

    /**
     * boolean 配列（std::vector<bool>）に対する複合ビット演算子の C++17 変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void convertsBooleanArrayBitwiseCompoundAssignments() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        boolean[] pending = new boolean[10];
                        pending[0] ^= true;
                        pending[1] &= false;
                        pending[2] |= true;
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("pending[0] ^= true"));
        assertTrue(cpp.contains("pending[1] = (pending[1] & false)"));
        assertTrue(cpp.contains("pending[2] = (pending[2] | true)"));
    }

    /**
     * JPrintWriter.printf および jreplace の char 引数オーバーロードの変換を検証する。
     * 計算量: O(L)。
     */
    @Test
    void testCharReplaceAndPrintWriterPrintf() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;

                public class Main {
                    public static void main(String[] args) {
                        PrintWriter pw = new PrintWriter(System.out);
                        pw.printf("%.20f\\n", 3.14);
                        String s = "hello".replace('e', 'a');
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("pw.printf("));
        assertTrue(cpp.contains("jreplace(JString(\"hello\"), 'e', 'a')"));
    }

    /**
     * Long.signum および Integer.signum の C++ 変換を検証する。
     * 計算量: O(L)。
     */
    // 未テスト
    @Test
    void testSignumConversion() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        int a = Integer.signum(-5);
                        int b = Long.signum(10L);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JInteger::signum((-5))"));
        assertTrue(cpp.contains("JLong::signum(10LL)"));
    }

    /**
     * java.lang.Number の C++ 変換を検証する。
     * 計算量: O(L)。
     */
    // 未テスト
    @Test
    void testNumberConversion() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.List;
                public class Main {
                    public static long sum(List<? extends Number> list) {
                        long ret = 0;
                        for (Number val : list) {
                            ret += val.longValue();
                        }
                        return ret;
                    }
                    public static void main(String[] args) {
                        System.out.println(sum(List.of(1, 2, 3)));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JNumber"));
        assertTrue(cpp.contains("val.longValue()"));
    }
}
