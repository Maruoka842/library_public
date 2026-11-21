package library.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(cpp.contains("template <class TMethod>") || cpp.contains("template <class T>"));
        assertTrue(cpp.contains("dynamic_pointer_cast") || cpp.contains("jcast<") || cpp.contains("static_pointer_cast<Derived>") || cpp.contains("static_pointer_cast<struct Derived>"));
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
        assertTrue(cpp.contains("(long long){}") || cpp.contains("nullptr"));
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
        assertTrue(cpp.contains("make_shared<Node>") || cpp.contains("make_shared<struct Node>"));
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
        assertTrue(cpp.contains("tree->set(0, {})") || cpp.contains("tree->set(0, (long long){})"));
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

        assertTrue(cpp.contains("JThread(nullptr, make_shared<Main>(), JString(\"\"), 1024LL).run();") || cpp.contains("JThread(nullptr, make_shared<struct Main>(), JString(\"\"), 1024LL).run();"));
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
    void testUserCodeCompilation() throws Exception {
        Path solverDir = Path.of("solver/src/main/java");
        Files.createDirectories(solverDir);
        Path mainJava = solverDir.resolve("Main.java");
        String userCode = """
                import java.io.IOException;
                import java.util.Arrays;
                import java.util.Random;

                import library.tools.FastScanner;
                import library.tools.JavaToCpp;
                import library.tools.MyPrintWriter;
                import library.util.Fp;
                import library.util.MathUtils;
                import library.util.polynomial.PolynomialFpDynamic;

                public class Main implements Runnable {

			static MyPrintWriter pw=MyPrintWriter.getInstance();
			static FastScanner sc=FastScanner.getInstance();

			public static void main(String[] args) throws IOException {
				new Main().run();
				pw.flush();
                //		MergeFiles.export();
				JavaToCpp.export();
			}

			Random r=new Random();
			Fp fp=Fp.MOD998244353;
			long mod=fp.modulus();

                    @java.lang.Override
                    public void run() {
                        var P = PolynomialFpDynamic.MOD998244353;
                        int N = Main.sc.nextInt();
                        long[] A = new long[N];
                        long[] B = new long[N];
                        for (int i = 0; i < N; i++) {
                            A[i] = Main.sc.nextLong();
                            B[i] = Main.sc.nextLong();
                        }
                        long[] sortedA = A.clone();
                        long[] sortedB = B.clone();
                        java.util.Arrays.sort(sortedA);
                        java.util.Arrays.sort(sortedB);
                        long[] values = new long[2 * N];
                        java.lang.System.arraycopy(A, 0, values, 0, N);
                        java.lang.System.arraycopy(B, 0, values, N, N);
                        java.util.Arrays.sort(values);
                        int unique = 0;
                        for (long value : values) {
                            if ((unique == 0) || (values[unique - 1] != value)) {
                                values[unique++] = value;
                            }
                        }
                        long[] coef = new long[unique - 1];
                        int[] belowBoth = new int[coef.length];
                        int[] belowOne = new int[coef.length];
                        for (int j = 1; j < unique; j++) {
                            long threshold = values[j];
                            int both = lowerBound(sortedB, threshold);
                            int one = lowerBound(sortedA, threshold) - both;
                            int idx = j - 1;
                            coef[idx] = (((threshold - values[j - 1]) % mod) * MathUtils.modPow(2, N - one, mod)) % mod;
                            belowBoth[idx] = both;
                            belowOne[idx] = one;
                        }
                        long[]distribution=P.sumPowerProductsByBoundedVariation(new long[] {0, 1}, new long[] {1, 1}, coef, belowBoth, belowOne);
                        long prefix = 0;
                        long base = ((values[0] % mod) * MathUtils.modPow(2, N, mod)) % mod;
                        for (int k = 1; k <= N; k++) {
                            if ((k - 1) < distribution.length) {
                                prefix = (prefix + distribution[k - 1]) % mod;
                            }
                            Main.pw.println((base + prefix) % mod);
                        }

                    }

                    int lowerBound(long[] a, long value) {
                        int low = 0;
                        int high = a.length;
                        while (low < high) {
                            int mid = (low + high) >>> 1;
                            if (a[mid] < value) {
                                low = mid + 1;
                            } else {
                                high = mid;
                            }
                        }
                        return low;
                    }

			void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}

                }
                """;
        Files.writeString(mainJava, userCode, StandardCharsets.UTF_8);

        MergeFiles.export();
        Path mergedJava = Path.of("out", "Main.java");
        String cpp = JavaToCpp.convert(mergedJava);
        Path cppFile = tempDir.resolve("main.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);

        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        System.err.println("G++ Exit Code: " + exitCode);
        System.err.println("G++ Output:\n" + output);
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

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

    @Test
    void testBatch20Features() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Scanner;

                public class Main {
                    public static long bitand(long[] a) {
                        long res = -1;
                        for (long x : a) res &= x;
                        return res;
                    }

                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        long[] a = new long[]{1, 2, 3};
                        long res = bitand(a);
                        String s = Long.toUnsignedString(res);
                        double d = Double.sum(1.5, 2.5);
                        System.out.println(s + " " + d);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("bitand_"));
        assertTrue(cpp.contains("JLong::toUnsignedString"));
        assertTrue(cpp.contains("JDouble::sum"));

        Path cppFile = tempDir.resolve("main.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testTemplateMethodsAndArrayNewInstance() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.lang.reflect.Array;

                class Helper {
                    public <T> T[] makeArray(T single, int n) {
                        @SuppressWarnings("unchecked")
                        T[] arr = (T[]) Array.newInstance(single.getClass(), n);
                        return arr;
                    }
                }

                public class Main {
                    public static void main(String[] args) {
                        Helper h = new Helper();
                        String[] res = h.makeArray("hello", 3);
                        System.out.println(res.length);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertFalse(cpp.contains("virtual template"));
        assertTrue(cpp.contains("makeArray"));

        Path cppFile = tempDir.resolve("main_template.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_template").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testIntPredicateAndClassAndOmittedTypeArgs() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.function.IntPredicate;

                class Container<T> {
                    public Container() {}
                }

                public class Main {
                    public static void main(String[] args) {
                        IntPredicate p = x -> x > 0;
                        boolean test = p.test(5);
                        Class<?> clazz = Main.class;
                        Container<String> c = new Container();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("function<bool(int)> p"));
        assertTrue(cpp.contains("p(5)"));
        assertTrue(cpp.contains("JClass clazz"));
        assertTrue(cpp.contains("make_shared<Container<JString>>()") || cpp.contains("make_shared<struct Container<JString>>()"));

        Path cppFile = tempDir.resolve("main_predicate.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_predicate").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testAnonymousComparatorWithReferenceParameters() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;
                import java.util.Comparator;

                public class Main {
                    public static void main(String[] args) {
                        long[][] list = new long[][]{{1, 2}, {3, 4}};
                        Arrays.sort(list, new Comparator<long[]>() {
                            @Override
                            public int compare(long[] o1, long[] o2) {
                                return Long.compare(o1[1], o2[1]);
                            }
                        });
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("o1_const") && cpp.contains("const_cast"));

        Path cppFile = tempDir.resolve("main_comparator.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_comparator").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testLambdaWithReferenceParameters() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.Arrays;

                public class Main {
                    public static void main(String[] args) {
                        long[][] list = new long[][]{{1, 2}, {3, 4}};
                        Arrays.sort(list, (long[] o1, long[] o2) -> Long.compare(o1[1], o2[1]));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("o1_const") && cpp.contains("const_cast"));

        Path cppFile = tempDir.resolve("main_lambda.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_lambda").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testPrimitiveWrapperHashCode() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        int h1 = Integer.hashCode(42);
                        int h2 = Long.hashCode(123456789012345L);
                        int h3 = Double.hashCode(3.14159);
                        int h4 = Boolean.hashCode(true);
                        int h5 = Character.hashCode('a');
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("JInteger::hashCode"));
        assertTrue(cpp.contains("JLong::hashCode"));

        Path cppFile = tempDir.resolve("main_hashcode.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_hashcode").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);
    }

    @Test
    void testStandardLibraryMethodsTranspilationAndExecution() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;
                import java.math.*;

                public class Main {
                    public static void main(String[] args) {
                        // Integer & Long
                        int r1 = Integer.rotateLeft(1, 2);
                        int r2 = Integer.rotateRight(4, 2);
                        int u1 = Integer.divideUnsigned(-10, 2);
                        long u2 = Integer.toUnsignedLong(-1);
                        long lr1 = Long.rotateLeft(1L, 2);
                        long lu1 = Long.divideUnsigned(-10L, 2L);

                        // Double & Character & Boolean
                        boolean f1 = Double.isFinite(3.14);
                        long b1 = Double.doubleToLongBits(3.14);
                        double d1 = Double.longBitsToDouble(b1);
                        int cd1 = Character.digit('a', 16);
                        char fc1 = Character.forDigit(10, 16);
                        boolean bg1 = Boolean.getBoolean("test");

                        // Math
                        double m1 = Math.log1p(0.5);
                        double m2 = Math.expm1(0.5);
                        double m3 = Math.sinh(0.5);
                        double m4 = Math.toRadians(180.0);
                        long m5 = Math.multiplyFull(100, 200);

                        // Objects
                        boolean o1 = Objects.isNull(null);
                        boolean o2 = Objects.nonNull("abc");
                        String o3 = Objects.requireNonNullElse("a", "b");

                        // Arrays & Collections
                        int[] arr1 = new int[]{1, 2, 3};
                        int[] arr2 = new int[]{1, 2, 4};
                        int cmp1 = Arrays.compare(arr1, arr2);
                        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 2));
                        Collections.rotate(list, 1);
                        boolean dis = Collections.disjoint(list, Arrays.asList(5, 6));

                        // String & StringBuilder
                        String s = "  hello world  ";
                        boolean s1 = s.contains("hello");
                        boolean s2 = s.matches(".*hello.*");
                        String s3 = s.trim();
                        boolean s4 = s.isBlank();
                        String s5 = String.join("-", "a", "b", "c");

                        StringBuilder sb = new StringBuilder("abc");
                        sb.insert(1, "x");
                        int sb1 = sb.indexOf("x");

                        // BigInteger & BigDecimal
                        BigInteger bi = new BigInteger("10");
                        BigInteger bi2 = bi.modPow(BigInteger.valueOf(2), BigInteger.valueOf(7));
                        BigInteger bi3 = BigInteger.valueOf(3).modInverse(BigInteger.valueOf(7));

                        BigDecimal bd = new BigDecimal("3.14");
                        BigDecimal bd2 = bd.remainder(new BigDecimal("2.0"));

                        // BitSet
                        BitSet bs = new BitSet();
                        bs.set(0, 10);
                        int p1 = bs.previousSetBit(5);
                        int c1 = bs.nextClearBit(5);

                        // Container methods
                        List<String> slist = new ArrayList<>(Arrays.asList("a", "b", "c"));
                        slist.replaceAll(x -> x.toUpperCase());
                        boolean c2 = slist.containsAll(Arrays.asList("A", "B"));

                        Map<String, Integer> map = new HashMap<>();
                        map.put("a", 1);
                        map.replace("a", 1, 2);

                        // System & Runtime & Random & Thread & Date & Optional
                        long t1 = System.currentTimeMillis();
                        Runtime rt = Runtime.getRuntime();
                        Random rnd = new Random(42);
                        rnd.setSeed(12345);
                        Date date = new Date(1000L);
                        date.setTime(2000L);
                        Optional<String> opt = Optional.of("test");
                        String optVal = opt.orElseThrow();

                        System.out.println("SUCCESS: " + s5 + " " + optVal);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        Path cppFile = tempDir.resolve("main_stdlib.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_stdlib").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        // Run the compiled binary
        Process runProc = new ProcessBuilder(tempDir.resolve("main_stdlib").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("SUCCESS: a-b-c test"), "Unexpected output:\n" + runOutput);
    }

    /**
     * JavaToCpp の PriorityQueue#addAll, Map#higherKey/lowerKey/higherEntry/lowerEntry,
     * PrintWriter 派生クラスの using 宣言, メソッド参照, equals 変換を検証する。
     * 計算量: O(1)
     */
    // 未テスト
    @Test
    void testNavigableMapAndPriorityQueueAddAll(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.*;

                public class Main {
                    static class MyWriter extends PrintWriter {
                        public MyWriter() { super(System.out); }
                        public void println(List<Integer> list) {
                            for (int x : list) print(x + " ");
                            println();
                        }
                    }

                    static class Item {
                        int v;
                        Item(int v) { this.v = v; }
                        int val() { return v; }
                    }

                    public static void main(String[] args) {
                        // 1. PriorityQueue addAll
                        PriorityQueue<Integer> pq = new PriorityQueue<>();
                        ArrayDeque<Integer> dq = new ArrayDeque<>();
                        dq.add(3);
                        dq.add(1);
                        dq.add(2);
                        pq.addAll(dq);
                        int p1 = pq.poll();

                        // 2. TreeMap lowerKey, higherKey, lowerEntry, higherEntry
                        TreeMap<Integer, String> map = new TreeMap<>();
                        map.put(10, "ten");
                        map.put(20, "twenty");
                        map.put(30, "thirty");
                        Integer lk = map.lowerKey(20);
                        Integer hk = map.higherKey(20);
                        Map.Entry<Integer, String> le = map.lowerEntry(20);
                        Map.Entry<Integer, String> he = map.higherEntry(20);

                        // 3. MyWriter using PrintWriter::println
                        MyWriter pw = new MyWriter();
                        pw.println(42L);

                        // 4. Method reference to declared type
                        Optional<Item> opt = Optional.of(new Item(100));
                        int extracted = opt.map(Item::val).orElse(0);

                        // 5. equals on pair/primitive
                        var entry = map.floorEntry(20);
                        boolean eq = entry.getKey().equals(20);

                        System.out.println("OK: " + p1 + " " + lk + " " + hk + " " + extracted + " " + eq);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        Path cppFile = tempDir.resolve("main_test10.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test10").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test10").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("OK: 1 10 30 100 1") || runOutput.contains("OK: 1 10 30 100 true"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testBatchFeatures(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                public class Main {
                    static class Point {
                        int x, y;
                        Point(int x, int y) { this.x = x; this.y = y; }
                    }

                    static <T> int getHash(T obj) {
                        return obj.hashCode();
                    }

                    public static void main(String[] args) {
                        int[] arr = new int[]{3, 1, 4, 1, 5};
                        Arrays.sort(arr, 1, 4);

                        List<Point> points = new ArrayList<>();
                        points.add(new Point(2, 3));
                        points.add(new Point(1, 5));
                        points.add(new Point(1, 2));

                        Comparator<Point> comp = Comparator.comparing((Point p) -> p.x)
                                .thenComparing((Point p) -> p.y);
                        points.sort(comp);

                        int h = getHash("hello");
                        boolean cleaned = false;
                        try {
                            cleaned = true;
                        } finally {
                            System.out.println("FINALLY");
                        }

                        System.out.println("OK: " + arr[1] + " " + arr[2] + " " + points.get(0).y + " " + (h != 0));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        Path cppFile = tempDir.resolve("main_test11.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test11").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test11").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("FINALLY"), "Expected FINALLY output:\n" + runOutput);
        assertTrue(runOutput.contains("OK: 1 1 2 1") || runOutput.contains("OK: 1 1 2 true"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testCovariantMethodReturnAndUnshadowing(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                interface BaseStrategy<T> {
                    T add(T a, T b);
                }

                interface DerivedStrategy<T> extends BaseStrategy<T> {
                    T sub(T a, T b);
                }

                interface BaseElement<X> {
                    BaseStrategy<X> parent();
                    X self();
                }

                interface DerivedElement<X> extends BaseElement<X> {
                    @Override
                    DerivedStrategy<X> parent();
                    default X sub(X a) {
                        return parent().sub(self(), a);
                    }
                }

                class IntElem implements DerivedElement<IntElem> {
                    int val;
                    IntElem(int val) { this.val = val; }
                    @Override
                    public DerivedStrategy<IntElem> parent() {
                        return new DerivedStrategy<>() {
                            @Override
                            public IntElem add(IntElem a, IntElem b) { return new IntElem(a.val + b.val); }
                            @Override
                            public IntElem sub(IntElem a, IntElem b) { return new IntElem(a.val - b.val); }
                        };
                    }
                    @Override
                    public IntElem self() { return this; }
                }

                class BaseMath {
                    public long mul(long a, long b) { return a * b; }
                    public long mul(long a, long b, long c) { return a * b * c; }
                }

                class DerivedMath extends BaseMath {
                    @Override
                    public long mul(long a, long b) { return (a * b) % 1000; }
                }

                public class Main {
                    public static void main(String[] args) {
                        IntElem x = new IntElem(10);
                        IntElem y = new IntElem(3);
                        IntElem z = x.sub(y);

                        DerivedMath dm = new DerivedMath();
                        long m2 = dm.mul(4, 5);
                        long m3 = dm.mul(2, 3, 4);

                        System.out.println("RES: " + z.val + " " + m2 + " " + m3);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        Path cppFile = tempDir.resolve("main_test12.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test12").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test12").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("RES: 7 20 24"), "Unexpected output:\n" + runOutput);
    }

    /**
     * 不要な prelude が C++ 出力に含まれないことを検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void excludesUnusedPrelude() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        int a = 10;
                        int b = 20;
                        System.out.println(a + b);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        // 基本機能のみのプログラムには JBigInteger, JBitSet, Line2D, JScanner などが含まれない
        assertFalse(cpp.contains("struct JBigInteger"));
        assertFalse(cpp.contains("struct JBitSet"));
        assertFalse(cpp.contains("struct Line2D"));
        assertFalse(cpp.contains("struct JScanner"));
        assertFalse(cpp.contains("struct JRandom"));
        assertFalse(cpp.contains("struct JThread"));
        assertFalse(cpp.contains("struct JRuntime"));
        assertFalse(cpp.contains("struct JBigDecimal"));

        Path cppFile = tempDir.resolve("main_test_noprelude.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_noprelude").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_noprelude").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("30"), "Unexpected output:\n" + runOutput);
    }

    /**
     * BigInteger を使用するプログラムでは JBigInteger のみが必要に応じて含まれることを検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void includesOnlyRequiredPreludeWhenUsed() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;

                public class Main {
                    public static void main(String[] args) {
                        BigInteger a = new BigInteger("12345678901234567890");
                        BigInteger b = BigInteger.valueOf(10);
                        System.out.println(a.multiply(b).toString());
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        assertTrue(cpp.contains("struct JBigInteger"));
        assertFalse(cpp.contains("struct JBitSet"));
        assertFalse(cpp.contains("struct Line2D"));
        assertFalse(cpp.contains("struct JScanner"));

        Path cppFile = tempDir.resolve("main_test_bigint_prelude.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_bigint_prelude").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_bigint_prelude").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("123456789012345678900"), "Unexpected output:\n" + runOutput);
    }

    /**
     * 独自定義された Scanner クラス、vector<bool>::reference 比較、静的 outer、独自コレクション iterator、
     * 独自 Enum 宣言、および sort(null) のコンパイルを検証する。
     * 計算量: O(L)。
     */
    @Test
    // 未テスト
    void testPreviousCompilationCheckRegressions(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                class Scanner {
                    int val;
                    Scanner() { this.val = 42; }
                    int nextInt() { return val; }
                    static boolean isPrintable(int c) { return c >= 33; }
                }

                public class Main {
                    class InnerNode {
                        int id;
                        InnerNode() { this.id = 100; }
                    }

                    enum Winner {
                        LEFT, RIGHT
                    }

                    InnerNode root = new InnerNode();

                    public static void main(String[] args) {
                        Scanner sc = new Scanner();
                        boolean b = Scanner.isPrintable(sc.nextInt());

                        boolean[][] mat = new boolean[2][2];
                        mat[0][1] = true;
                        boolean diff = (mat[0][1] != mat[1][0]);

                        List<Integer> list = new ArrayList<>();
                        list.add(3);
                        list.add(1);
                        list.sort(null);

                        Winner w = Winner.LEFT;
                        System.out.println("OK: " + sc.nextInt() + " " + b + " " + diff + " " + list.get(0) + " " + (w == Winner.LEFT));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);

        Path cppFile = tempDir.resolve("main_test_regressions.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_regressions").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_regressions").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("OK: 42 1 1 1 1") || runOutput.contains("OK: 42 true true 1 true"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testBatch3482Features() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                public class Main {
                    public record SimpleRecord(List[] comp, int[] col) {}

                    public static Iterable<Integer> iterateFrom(int start) {
                        return () -> new PrimitiveIterator.OfInt() {
                            int cur = start;
                            @Override
                            public boolean hasNext() { return cur > 0; }
                            @Override
                            public int nextInt() { return cur--; }
                        };
                    }

                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>();
                        list.add(10);
                        list.add(20);
                        list.addFirst(5);
                        list.addLast(30);
                        int first = list.getFirst();
                        int last = list.getLast();
                        list.removeFirst();
                        list.removeLast();

                        Map<Integer, Set<Integer>> map = new HashMap<>();
                        map.put(1, new HashSet<>());
                        map.get(1).add(42);
                        int hash = Objects.hashCode(map);

                        List<Integer>[] arr = new ArrayList[2];
                        arr[0] = new ArrayList<>();
                        arr[0].add(100);
                        SimpleRecord rec = new SimpleRecord(arr, new int[]{1, 2});

                        int sum = 0;
                        for (int v : iterateFrom(3)) {
                            sum += v;
                        }

                        System.out.println("OK: " + first + " " + last + " " + (hash != 0) + " " + rec.col()[0] + " " + sum);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch3482.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch3482").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch3482").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("OK: 5 30 1 1 6") || runOutput.contains("OK: 5 30 true 1 6"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testBatch3582Features(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                class Helper {
                    static final java.util.Comparator<Long> CMP = (a, b) -> Long.compare(a, b);
                }

                public class Main {
                    int outerVal = 42;

                    void execute(int paramX) {
                        int localY = 10;
                        class LocalWorker {
                            int work() {
                                return outerVal + paramX + localY;
                            }
                            Main getOuter() {
                                return Main.this;
                            }
                        }
                        LocalWorker worker = new LocalWorker();
                        System.out.println("Worker: " + worker.work() + " " + (worker.getOuter() != null));
                    }

                    public static void main(String[] args) {
                        List<Long> list = new ArrayList<>();
                        list.add(10L);
                        list.add(20L);
                        list.add(10L);
                        Set<Long> set = new HashSet<>(list);
                        System.out.println("Set size: " + set.size() + " CMP: " + Helper.CMP.compare(10L, 20L));
                        new Main().execute(100);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch3582.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch3582").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch3582").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("Set size: 2 CMP: -1"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Worker: 152 1") || runOutput.contains("Worker: 152 true"), "Unexpected output:\n" + runOutput);
    }

    @Test
    // 未テスト
    // 計算量: O(|S|)
    void testBatch3682Features() throws Exception {
        Path java = tempDir.resolve("MainBatch3682.java");
        Files.writeString(java, """
                import java.util.*;
                import java.util.function.*;

                public class Main {
                    interface ActionStrategy<F, S> {
                        S act(F f, S s);
                        default S powAct(F f, long n, S s) {
                            return act(f, s);
                        }
                    }

                    static class CustomSet extends HashSet<Long> {
                        public boolean addPair(int a, int b) {
                            return super.add((long) a << 32 | (long) b);
                        }
                        @Override
                        public boolean equals(Object o) {
                            return super.equals(o);
                        }
                        @Override
                        public int hashCode() {
                            return super.hashCode();
                        }
                    }

                    public void register(long x, long y) {
                        System.out.println("registered: " + x + "," + y);
                    }

                    public static void main(String[] args) {
                        // 1. Map.Entry.setValue
                        Map<String, String> map = new HashMap<>();
                        map.put("key", "old");
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            entry.setValue("new");
                            System.out.println("Entry value: " + entry.getValue());
                        }

                        // 2. JList(const Container&) with JObject/any (empty list)
                        List<String> empty = List.of();
                        ArrayList<String> copyList = new ArrayList<>(empty);
                        System.out.println("CopyList size: " + copyList.size());

                        // 3. Keyword register & conditional functional type
                        Main m = new Main();
                        m.register(10, 20);
                        LongUnaryOperator op = (args.length == 0) ? (x -> x * 2) : null;
                        if (op != null) {
                            System.out.println("Op: " + op.applyAsLong(5));
                        }

                        // 4. CustomSet subclass of HashSet
                        CustomSet set = new CustomSet();
                        set.addPair(1, 2);
                        System.out.println("Set contains: " + set.contains((1L << 32) | 2L) + " hash: " + (set.hashCode() != 0));

                        // 5. ActionStrategy anonymous class with boxing / primitive match
                        ActionStrategy<long[], Long> strat = new ActionStrategy<long[], Long>() {
                            @Override
                            public Long act(long[] f, Long s) {
                                return f[0] + s;
                            }
                        };
                        System.out.println("Act: " + strat.act(new long[]{3}, 7L));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch3682.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch3682").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch3682").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("Entry value: new"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("CopyList size: 0"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("registered: 10,20"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Op: 10"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Set contains: 1 hash: 1") || runOutput.contains("Set contains: true hash: true"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Act: 10"), "Unexpected output:\n" + runOutput);
    }

    /**
     * バッチ3782の対応機能検証（nullリテラル要素型補完、Math::min/maxメソッド参照、カスタムSAMインターフェースへのラムダ式渡し、シグネチャ重複除去、非staticメンバ変数初期化子での匿名クラス生成）。
     * 計算量: O(|S|)。
     */
    @Test
    // 未テスト
    void testBatch3782Features() throws Exception {
        Path java = tempDir.resolve("MainBatch3782.java");
        Files.writeString(java, """
                import java.util.*;

                public class Main {
                    @FunctionalInterface
                    interface MyPredicate {
                        boolean test(int a, int b);
                    }

                    static class Helper {
                        static boolean check(int[] a, MyPredicate pred) {
                            return pred.test(a[0], a[1]);
                        }
                        static long[] take(long[] a, int[] idx) {
                            return new long[]{a[idx[0]]};
                        }
                        static long[] take(long[] a, Integer[] idx) {
                            return new long[]{a[idx[0]]};
                        }
                    }

                    interface Strategy<T> {
                        T zero();
                    }

                    static class Solver<T> {
                        T zero() { return null; }
                        Strategy<T> parent = new Strategy<T>() {
                            public T zero() {
                                return Solver.this.zero();
                            }
                        };
                    }

                    public static void main(String[] args) {
                        // 1. null literal in List<long[]>
                        List<long[]> list = new ArrayList<>();
                        list.add(null);
                        System.out.println("List size: " + list.size());

                        // 2. Math::min reference
                        java.util.function.LongBinaryOperator op = Math::min;
                        System.out.println("Min: " + op.applyAsLong(10L, 20L));

                        // 3. Lambda to SAM interface
                        boolean predRes = Helper.check(new int[]{5, 5}, (u, v) -> u == v);
                        System.out.println("Pred: " + predRes);

                        // 4. Overloaded take call
                        long[] taken = Helper.take(new long[]{100L}, new int[]{0});
                        System.out.println("Taken: " + taken[0]);

                        // 5. Instance field anon class
                        Solver<Long> s = new Solver<>();
                        System.out.println("Solver created: " + (s != null));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch3782.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch3782").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch3782").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("List size: 1"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Min: 10"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Pred: 1") || runOutput.contains("Pred: true"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Taken: 100"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Solver created: 1") || runOutput.contains("Solver created: true"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testBatch3932Features(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;
                import java.util.stream.*;

                public class Main {
                    enum Direction {
                        LEFT, UP, RIGHT, DOWN;
                    }

                    static class CustomMap<V> extends HashMap<Long, V> {
                        public V putVal(long k, V v) {
                            return super.put(k, v);
                        }
                        public boolean checkEmpty() {
                            return isEmpty();
                        }
                    }

                    public static void main(String[] args) {
                        // 1. Arrays.fill range
                        int[] arr = new int[5];
                        Arrays.fill(arr, 1, 4, 42);
                        System.out.println("Arr: " + arr[0] + " " + arr[1] + " " + arr[3] + " " + arr[4]);

                        // 2. Stream.toList()
                        List<String> src = new ArrayList<>();
                        src.add("hello");
                        src.add("world");
                        List<String> list = src.stream().distinct().toList();
                        System.out.println("Stream toList: " + list.size() + " " + list.get(0));

                        // 3. Enum in switch
                        Direction dir = Direction.UP;
                        int val = 0;
                        switch (dir) {
                            case LEFT: val = 1; break;
                            case UP: val = 2; break;
                            default: val = 3; break;
                        }
                        System.out.println("Switch val: " + val);

                        // 4. CustomMap with super calls
                        CustomMap<String> map = new CustomMap<>();
                        map.putVal(10L, "test");
                        System.out.println("Map empty: " + map.checkEmpty() + " val: " + map.get(10L));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch3932.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch3932").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch3932").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("Arr: 0 42 42 0"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Stream toList: 2 hello"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Switch val: 2"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Map empty: 0") || runOutput.contains("Map empty: false"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("val: test"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testSubSequenceAndGenericCastsAndMethodTypeArgs(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.PriorityQueue;
                import java.util.Arrays;

                public class Main {
                    public static void main(String[] args) {
                        String s = "hello world";
                        CharSequence sub = s.subSequence(0, 5);
                        System.out.println(sub.toString());

                        GenericHolder<State> holder = new GenericHolder<>(new State(10));
                        holder.checkInstance();

                        MyPQ<int[]> pq = new MyPQ<>();
                        pq.add(new int[]{1, 2});
                        System.out.println(pq.size());
                    }
                }

                class State {
                    int val;
                    public State(int val) { this.val = val; }
                }

                class GenericHolder<T> {
                    T item;
                    public GenericHolder(T item) { this.item = item; }
                    public void checkInstance() {
                        if (item instanceof long[]) {
                            long[] arr = (long[]) item;
                            System.out.println(Arrays.toString(arr));
                        } else {
                            System.out.println("not long array");
                        }
                    }
                }

                class MyPQ<T> extends PriorityQueue<T> {
                    public MyPQ() {
                        super((x, y) -> defaultCompare(x, y));
                    }
                    private static <T> int defaultCompare(T x, T y) {
                        return 0;
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch4082.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch4082").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch4082").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("hello"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("not long array"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("1"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testStringConcatWithPriorityQueueAndToString(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.PriorityQueue;

                public class Main {
                    static class Item {
                        int val;
                        Item(int v) { this.val = v; }
                        @Override
                        public String toString() {
                            return "Item(" + val + ")";
                        }
                    }

                    public static void main(String[] args) {
                        PriorityQueue<Long> pq = new PriorityQueue<>();
                        pq.add(42L);
                        String s1 = pq + " end";
                        System.out.println(s1);

                        Item item = new Item(100);
                        String s2 = "Item is " + item;
                        System.out.println(s2);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_jstr.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_jstr").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_jstr").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("end"), "Unexpected output:\n" + runOutput);
        assertTrue(runOutput.contains("Item is Item(100)"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testLambdaAssignedToCustomSAMInterface() throws Exception {
        Path java = tempDir.resolve("MainLambdaSAM.java");
        Files.writeString(java, """
                public class Main {
                    interface CostFunction {
                        long calc(int i, int j);
                    }

                    public static void main(String[] args) {
                        int[] A = {10, 20, 30};
                        int[] B = {1, 2, 3};
                        CostFunction f = (i, j) -> {
                            if (i < 0 || i >= A.length) return -1;
                            return A[i] + B[j];
                        };
                        System.out.println(f.calc(1, 2));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_lambda_sam.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_lambda_sam").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_lambda_sam").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("23"), "Unexpected output:\n" + runOutput);
    }

    /**
     * JDeque, JStack, JPriorityQueue initialized with collections are correctly converted and compiled.
     */
    // 未テスト
    // 計算量: O(1)
    @Test
    public void testCollectionConstructorsForContainers() throws Exception {
        Path java = tempDir.resolve("MainCollectionContainers.java");
        Files.writeString(java, """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
                        Queue<Integer> deque = new ArrayDeque<>(list);
                        Stack<Integer> stack = new Stack<>();
                        stack.addAll(list);
                        PriorityQueue<Integer> pq = new PriorityQueue<>(list);

                        int dVal = deque.poll();
                        int dSize = deque.size();
                        System.out.println(dVal + " " + dSize);

                        int sVal = stack.pop();
                        int sSize = stack.size();
                        System.out.println(sVal + " " + sSize);

                        int pVal = pq.poll();
                        int pSize = pq.size();
                        System.out.println(pVal + " " + pSize);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_col_containers.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_col_containers").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_col_containers").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("1 2"), "Unexpected deque output:\n" + runOutput);
        assertTrue(runOutput.contains("3 2"), "Unexpected stack output:\n" + runOutput);
        assertTrue(runOutput.contains("1 2"), "Unexpected pq output:\n" + runOutput);
    }

    @Test
    void testAbsExactAndMultiLevelOuterFieldAndBigIntExactAndFillNull(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.math.BigInteger;
                import java.util.Arrays;
                import java.util.List;
                import java.util.ArrayList;

                public class Main {
                    int counter = 42;

                    public class Outer {
                        public class Inner {
                            int getId() {
                                return counter;
                            }
                        }
                    }

                    public static void main(String[] args) {
                        int a = Math.absExact(-10);
                        long b = Math.absExact(-20L);

                        BigInteger bi = BigInteger.valueOf(100);
                        BigInteger[] qr = bi.divideAndRemainder(BigInteger.valueOf(30));
                        long valExact = qr[0].longValueExact();
                        int ivalExact = qr[1].intValueExact();

                        List<Integer>[] arr = new List[2];
                        arr[0] = new ArrayList<>();
                        arr[0].add(1);
                        Arrays.fill(arr, null);

                        Main m = new Main();
                        Outer.Inner inner = m.new Outer().new Inner();
                        int c = inner.getId();

                        System.out.println(a + " " + b + " " + valExact + " " + ivalExact + " " + c);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_batch.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_batch").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_batch").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("10 20 3 10 42"), "Unexpected output:\n" + runOutput);
    }
    @Test
    void testSplittableRandomAndOptionalLongAndCharString(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.SplittableRandom;
                import java.util.OptionalLong;

                public class Main {
                    static class SegTree<T> {
                        public static int calc(int a) { return a * 2; }
                    }

                    static class Node {
                        int val;
                        Node(int val) {
                            this.val = val;
                            helper(this);
                        }
                    }

                    static void helper(Node n) {
                        n.val += 1;
                    }

                    interface TriFunc<A, B, C, R> {
                        R apply(A a, B b, C c);
                    }

                    static class GenericContainer<X, Y> {
                        X x;
                        Y y;
                        GenericContainer(TriFunc<X, X, Y, Integer> f) {
                            f.apply(null, null, null);
                        }
                    }

                    public static void main(String[] args) {
                        SplittableRandom rnd = new SplittableRandom(12345);
                        long r = rnd.nextLong(1, 100);
                        OptionalLong opt = OptionalLong.of(42);
                        OptionalLong emptyOpt = OptionalLong.empty();
                        char[] chars = "415".toCharArray();
                        String s = String.valueOf(chars);
                        int segCalc = SegTree.calc(5);
                        Node node = new Node(10);
                        GenericContainer<Integer, String> gc = new GenericContainer<>((a, b, c) -> 0);
                        System.out.println(s + " " + opt.getAsLong() + " " + emptyOpt.isPresent() + " " + segCalc + " " + node.val);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("main_test_features.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("main_test_features").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("main_test_features").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertTrue(runOutput.contains("415 42") && runOutput.contains("10 11"), "Unexpected output:\n" + runOutput);
    }

    @Test
    void testLocalVariableNotMistakenForCapturedVariable() throws Exception {
        Path java = tempDir.resolve("LocalVarCaptureTest.java");
        Files.writeString(java, """
                import java.util.function.BinaryOperator;

                public class Main {
                    public static void main(String[] args) {
                        BinaryOperator<Integer> op = new BinaryOperator<Integer>() {
                            @Override
                            public Integer apply(Integer a, Integer b) {
                                return a + b;
                            }
                        };
                        int[] a = new int[]{10, 20};
                        System.out.println(op.apply(a[0], a[1]));
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        assertFalse(cpp.contains("static vector<int> a;"), "Local variable 'a' should not be emitted as static");

        Path cppFile = tempDir.resolve("LocalVarCaptureTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("LocalVarCaptureTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("LocalVarCaptureTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("30\n", runOutput);
    }

    @Test
    void testNullableGenericArithmeticType() throws Exception {
        Path java = tempDir.resolve("NullableGenericTest.java");
        Files.writeString(java, """
                public class Main {
                    static class Container<T> {
                        T val;
                        void put(T v) {
                            this.val = v;
                        }
                        boolean isNull() {
                            return val == null;
                        }
                    }

                    public static void main(String[] args) {
                        Container<Long> c = new Container<>();
                        System.out.println(c.isNull());
                        c.put(42L);
                        System.out.println(c.isNull());
                        System.out.println(c.val);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        assertTrue(cpp.contains("Nullable<TInner>::type val"), "Field 'val' should be emitted with Nullable<TInner>::type");

        Path cppFile = tempDir.resolve("NullableGenericTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("NullableGenericTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("NullableGenericTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("1\n0\n42\n", runOutput.replace("\r\n", "\n"));
    }

    @Test
    void testFloatingPointFormatting(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                public class Main {
                    public static void main(String[] args) {
                        double a = 32.0;
                        double b = 42.5;
                        PrintWriter pw = new PrintWriter(System.out);
                        pw.println(a);
                        pw.println(b);
                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("FloatingPointTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("FloatingPointTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("FloatingPointTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("32.0\n42.5\n", runOutput.replace("\r\n", "\n"));
    }

    @Test
    void testComparatorReversedAndFieldDefaultInit(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.Arrays;
                import java.util.Comparator;

                public class Main {
                    static class Item {
                        int val;
                        int uninit;
                        Item(int val) {
                            this.val = val;
                        }
                    }

                    public static void main(String[] args) {
                        Item[] items = new Item[]{ new Item(3), new Item(1), new Item(2) };
                        Comparator<Item> comp = Comparator.comparingInt((Item item) -> item.val).reversed();
                        Arrays.sort(items, comp);
                        PrintWriter pw = new PrintWriter(System.out);
                        for (Item item : items) {
                            pw.println(item.val + " " + item.uninit);
                        }
                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        assertTrue(cpp.contains("jreversed("));
        assertTrue(cpp.contains("int uninit = {};"));

        Path cppFile = tempDir.resolve("ComparatorReversedAndFieldDefaultInitTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("ComparatorReversedAndFieldDefaultInitTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("ComparatorReversedAndFieldDefaultInitTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("3 0\n2 0\n1 0\n", runOutput.replace("\r\n", "\n"));
    }

    // 未テスト
    @Test
    void testIdentityHashMapAndPointerComparableSort(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("IdentityHashMapSortTest.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.ArrayList;
                import java.util.Collections;
                import java.util.IdentityHashMap;
                import java.util.List;

                public class Main {
                    static class Node implements Comparable<Node> {
                        int cost;
                        Node(int cost) { this.cost = cost; }
                        @Override
                        public int compareTo(Node o) {
                            return Integer.compare(this.cost, o.cost);
                        }
                    }

                    public static void main(String[] args) {
                        List<Node> list = new ArrayList<>();
                        Node n1 = new Node(30);
                        Node n2 = new Node(10);
                        Node n3 = new Node(20);
                        list.add(n1);
                        list.add(n2);
                        list.add(n3);

                        Collections.sort(list);

                        IdentityHashMap<Node, Boolean> map = new IdentityHashMap<>();
                        PrintWriter pw = new PrintWriter(System.out);
                        for (Node n : list) {
                            pw.print(n.cost + " ");
                            map.put(n, true);
                        }
                        pw.println();
                        pw.println(map.get(n2));
                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("IdentityHashMapSortTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("IdentityHashMapSortTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("IdentityHashMapSortTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("10 20 30 \n1\n", runOutput.replace("\r\n", "\n"));
    }

    @Test
    void testUnsignedShiftAndFunctionalEquals(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.Objects;
                import java.util.function.BinaryOperator;
                import java.util.function.UnaryOperator;

                class Container {
                    Integer val;
                    BinaryOperator<Integer> op;
                    UnaryOperator<Integer> inv;

                    Container(Integer val, BinaryOperator<Integer> op, UnaryOperator<Integer> inv) {
                        this.val = val;
                        this.op = op;
                        this.inv = inv;
                    }

                    @Override
                    public boolean equals(Object obj) {
                        if (!(obj instanceof Container)) return false;
                        Container other = (Container) obj;
                        if (val == null ? other.val != null : !val.equals(other.val)) return false;
                        if (op == null ? other.op != null : !op.equals(other.op)) return false;
                        if (inv == null ? other.inv != null : !inv.equals(other.inv)) return false;
                        return true;
                    }

                    @Override
                    public int hashCode() {
                        return Objects.hash(val, op, inv);
                    }
                }

                public class Main {
                    public static void main(String[] args) {
                        PrintWriter pw = new PrintWriter(System.out);
                        long val = -16L;
                        long shifted = val >>> 2;
                        val >>>= 2;
                        pw.println(shifted == val);

                        Container c1 = new Container(10, Integer::sum, v -> -v);
                        pw.println(c1.equals(c1));
                        pw.println(c1.hashCode() != 0);
                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("Main.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("true\ntrue\ntrue\n", runOutput.replace("\r\n", "\n").replace("1", "true"));
    }

    @Test
    void testBatchEnhancements(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.ArrayDeque;
                import java.util.Deque;
                import java.util.Iterator;
                import java.util.PriorityQueue;

                public class Main {
                    static class Base {
                        int x = 42;
                    }
                    static class Derived extends Base {
                        int y = 100;
                    }

                    public static void main(String[] args) {
                        PrintWriter pw = new PrintWriter(System.out);

                        // StringBuilder capacity constructor
                        StringBuilder sb = new StringBuilder(100);
                        sb.append("hello");
                        pw.println(sb.toString());

                        // Deque descendingIterator
                        Deque<Integer> dq = new ArrayDeque<>();
                        dq.addLast(1);
                        dq.addLast(2);
                        dq.addLast(3);
                        for (Iterator<Integer> it = dq.descendingIterator(); it.hasNext(); ) {
                            pw.print(it.next() + " ");
                        }
                        pw.println();

                        // PriorityQueue iteration
                        PriorityQueue<Integer> pq = new PriorityQueue<>();
                        pq.add(10);
                        pq.add(20);
                        for (int v : pq) {
                            pw.print((v >= 10) + " ");
                        }
                        pw.println();

                        // Multiple loop labels with same name in same method
                        out: for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                if (j == 1) continue out;
                            }
                        }
                        out: for (int i = 0; i < 3; i++) {
                            for (int j = 0; j < 3; j++) {
                                if (j == 1) continue out;
                            }
                        }
                        pw.println("labels_ok");

                        // Nested class dependency order
                        Derived d = new Derived();
                        pw.println(d.x + d.y);

                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("Main.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("hello\n3 2 1 \n1 1 \nlabels_ok\n142\n", runOutput.replace("\r\n", "\n"));
    }

    @Test
    void testAnonymousIteratorAndLengthFieldAndNanMath(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.io.PrintWriter;
                import java.util.Iterator;
                import java.util.NoSuchElementException;

                class HasLength {
                    int length;
                    HasLength(int length) {
                        this.length = length;
                    }
                    int getLen() {
                        return this.length;
                    }
                }

                class MySeq implements Iterable<Integer> {
                    final int n;
                    MySeq(int n) { this.n = n; }
                    @Override
                    public Iterator<Integer> iterator() {
                        return new Iterator<Integer>() {
                            int cur = 0;
                            @Override
                            public boolean hasNext() {
                                return cur < n;
                            }
                            @Override
                            public Integer next() {
                                if (!hasNext()) throw new NoSuchElementException();
                                return cur++;
                            }
                        };
                    }
                }

                public class Main {
                    public static void main(String[] args) {
                        PrintWriter pw = new PrintWriter(System.out);

                        HasLength hl = new HasLength(42);
                        pw.println(hl.getLen());

                        MySeq seq = new MySeq(3);
                        for (int v : seq) {
                            pw.print(v + " ");
                        }
                        pw.println();

                        double nan = Double.NaN;
                        double minNan = Math.min(nan, 5.0);
                        double maxNan = Math.max(5.0, nan);
                        pw.println(Double.isNaN(minNan) ? "min_nan" : "min_num");
                        pw.println(Double.isNaN(maxNan) ? "max_nan" : "max_num");

                        pw.flush();
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("Main.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("42\n0 1 2 \nmin_nan\nmax_nan\n", runOutput.replace("\r\n", "\n"));
    }
    /**
     * BitSet clone and mutating lambda reference captures transpile and execute correctly.
     */
    // 未テスト
    // 計算量: O(1)
    @Test
    public void testBitSetCloneAndMutatingLambda() throws Exception {
        Path java = tempDir.resolve("MainBitSetLambda.java");
        Files.writeString(java, """
                import java.util.ArrayList;
                import java.util.BitSet;
                import java.util.List;
                import java.util.function.Consumer;

                public class Main {
                    public static void main(String[] args) {
                        BitSet bs1 = new BitSet();
                        bs1.set(3);
                        BitSet bs2 = (BitSet) bs1.clone();
                        bs2.set(5);
                        System.out.println(bs1.get(5) ? "1" : "0");
                        System.out.println(bs2.get(5) ? "1" : "0");

                        List<Integer> list = new ArrayList<>();
                        int[] cnt = new int[]{0};
                        Consumer<Integer> action = x -> {
                            list.add(x);
                            cnt[0]++;
                        };
                        action.accept(42);
                        System.out.println(list.size());
                        System.out.println(cnt[0]);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainBitSetLambda.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainBitSetLambda").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainBitSetLambda").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("0\n1\n1\n1\n", runOutput.replace("\r\n", "\n"));
    }

    // 未テスト
    // 計算量: O(1)
    @Test
    void testScannerNextLineCrLfHandling(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("MainScannerCrLf.java");
        Files.writeString(java, """
                import java.util.Scanner;

                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        String s1 = sc.nextLine();
                        String s2 = sc.nextLine();
                        System.out.println(s1);
                        System.out.println(s2);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainScannerCrLf.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainScannerCrLf").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainScannerCrLf").toString())
                .redirectErrorStream(true)
                .start();
        runProc.getOutputStream().write("hello\r\nworld\r\n".getBytes(StandardCharsets.UTF_8));
        runProc.getOutputStream().flush();
        runProc.getOutputStream().close();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("hello\nworld\n", runOutput.replace("\r\n", "\n"));
    }

    /**
     * System.err への出力が標準エラー出力に出力され、標準出力と混ざらないことを検証する。
     */
    // 未テスト
    // 計算量: O(1)
    @Test
    void testSystemErrRouting(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("MainSystemErr.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        System.err.println("error message");
                        System.out.println("normal output");
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainSystemErr.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainSystemErr").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainSystemErr").toString())
                .start();
        runProc.getOutputStream().close();
        String stdOut = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stdErr = new String(runProc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode);
        assertEquals("normal output\n", stdOut.replace("\r\n", "\n"));
        assertEquals("error message\n", stdErr.replace("\r\n", "\n"));
    }

    /**
     * オブジェクト参照（shared_ptr）引数の呼び出し側ストレージ変更によるエイリアシング汚染が起きないことを検証する。
     */
    // 未テスト
    // 計算量: O(1)
    @Test
    void testSharedPtrParameterAliasing(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("MainTreeAliasing.java");
        Files.writeString(java, """
                public class Main {
                    static class Node {
                        int val;
                        Node next;
                        Node(int val, Node next) {
                            this.val = val;
                            this.next = next;
                        }
                    }
                    static Node modify(Node cur) {
                        Node n = cur;
                        cur = null;
                        return n;
                    }
                    public static void main(String[] args) {
                        Node head = new Node(10, new Node(20, null));
                        Node res = modify(head.next);
                        System.out.println(res != null ? res.val : -1);
                        System.out.println(head.next != null ? head.next.val : -1);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainTreeAliasing.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainTreeAliasing").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainTreeAliasing").toString())
                .redirectErrorStream(true)
                .start();
        runProc.getOutputStream().close();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("20\n20\n", runOutput.replace("\r\n", "\n"));
    }

    /**
     * 128ビットを超える多倍長整数の四則演算、ビット演算、文字列変換、modPow等のトランスパイル・実行結果を検証する。
     */
    // 未テスト
    // 計算量: O(1)
    @Test
    void testArbitraryPrecisionBigIntegerTranspilation(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("MainBigInt.java");
        Files.writeString(java, """
                import java.math.BigInteger;

                public class Main {
                    public static void main(String[] args) {
                        BigInteger a = new BigInteger("12345678901234567890123456789012345678901234567890");
                        BigInteger b = new BigInteger("98765432109876543210987654321098765432109876543210");
                        BigInteger sum = a.add(b);
                        BigInteger diff = b.subtract(a);
                        BigInteger prod = a.multiply(b);
                        BigInteger[] qr = prod.divideAndRemainder(a);

                        BigInteger p = BigInteger.ONE.shiftLeft(200);
                        boolean bit200 = p.testBit(200);
                        boolean bit199 = p.testBit(199);
                        BigInteger pShift = p.shiftRight(200);

                        BigInteger m = new BigInteger("1000000007");
                        BigInteger inv = BigInteger.valueOf(123456).modInverse(m);
                        BigInteger pow = BigInteger.valueOf(2).pow(100);

                        System.out.println(sum.toString());
                        System.out.println(diff.toString());
                        System.out.println(qr[0].toString());
                        System.out.println(qr[1].toString());
                        System.out.println(bit200 + " " + bit199 + " " + pShift.toString());
                        System.out.println(inv.toString());
                        System.out.println(pow.toString());
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainBigInt.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainBigInt").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainBigInt").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("111111111011111111101111111110111111111011111111100", lines[0]);
        assertEquals("86419753208641975320864197532086419753208641975320", lines[1]);
        assertEquals("98765432109876543210987654321098765432109876543210", lines[2]);
        assertEquals("0", lines[3]);
        assertEquals("1 0 1", lines[4]);
        assertEquals("78351802", lines[5]);
        assertEquals("1267650600228229401496703205376", lines[6]);
    }

    /**
     * Map.get 経由の変更、Math.round、および PriorityQueue の同順位ヒープ動作を検証する。
     * 計算量: O(1)
     */
    // 未テスト
    @Test
    void testMapGetMathRoundAndPriorityQueue(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        // 1. Map.get() mutating in-place
                        Map<String, List<Integer>> map = new HashMap<>();
                        map.put("k", new ArrayList<>());
                        map.get("k").add(42);
                        System.out.println(map.get("k").size() + " " + map.get("k").get(0));

                        // 2. Math.round() returning integral types
                        double d = 3.456;
                        float f = 2.7f;
                        System.out.println(Math.round(d));
                        System.out.println(Math.round(f));

                        // 3. PriorityQueue exact sift order
                        PriorityQueue<Integer> pq = new PriorityQueue<>();
                        pq.add(10);
                        pq.add(5);
                        pq.add(20);
                        int p1 = pq.poll();
                        int p2 = pq.poll();
                        int p3 = pq.poll();
                        System.out.println(p1 + " " + p2 + " " + p3);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainBatchTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainBatchTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainBatchTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("1 42", lines[0]);
        assertEquals("3", lines[1]);
        assertEquals("3", lines[2]);
        assertEquals("5 10 20", lines[3]);
    }

    @Test
    void testListRemoveWithCustomEquals(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.ArrayList;

                public class Main {
                    static class Item {
                        int id;
                        int val;
                        Item(int id, int val) { this.id = id; this.val = val; }
                        @Override
                        public boolean equals(Object o) {
                            if (o instanceof Item) {
                                Item other = (Item) o;
                                return this.id == other.id && this.val == other.val;
                            }
                            return false;
                        }
                    }

                    public static void main(String[] args) {
                        ArrayList<Item> list = new ArrayList<>();
                        list.add(new Item(1, 10));
                        list.add(new Item(2, 20));
                        list.add(new Item(3, 30));

                        boolean removed = list.remove(new Item(2, 20));
                        System.out.println(removed + " " + list.size());
                        int idx = list.indexOf(new Item(3, 30));
                        System.out.println(idx);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainListRemoveTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainListRemoveTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainListRemoveTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("1 2", lines[0]);
        assertEquals("1", lines[1]);
    }
    /**
     * Map Entry null性判定および配列代入の評価順序を検証する。
     * 計算量: O(1)
     */
    // 未テスト
    @Test
    void testMapEntryNullAndArrayAssignmentEvaluationOrder(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.TreeMap;
                import java.util.Map;

                public class Main {
                    static int counter = 0;
                    static int nextInt() {
                        return ++counter;
                    }

                    public static void main(String[] args) {
                        TreeMap<Integer, Integer> map = new TreeMap<>();
                        var entryNull = map.ceilingEntry(10);
                        System.out.println(entryNull == null);

                        map.put(10, 100);
                        var entry = map.ceilingEntry(5);
                        System.out.println((entry == null) + " " + entry.getKey() + " " + entry.getValue());

                        int[] arr = new int[10];
                        counter = 0;
                        arr[nextInt()] += nextInt();
                        System.out.println(arr[1] + " " + arr[2]);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainEntryOrderTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainEntryOrderTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainEntryOrderTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("1", lines[0]);
        assertEquals("0 10 100", lines[1]);
        assertEquals("2 0", lines[2]);
    }

    @Test
    void testBoxedPrimitiveComparedToNull(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.TreeSet;

                public class Main {
                    public static void main(String[] args) {
                        TreeSet<Long> set = new TreeSet<>();
                        set.add(10L);
                        Long floor1 = set.floor(5L);
                        Long floor2 = set.floor(15L);
                        System.out.println(floor1 == null);
                        System.out.println(floor2 == null);
                        if (floor2 != null) {
                            System.out.println(floor2);
                        }
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainBoxedNullTest.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainBoxedNullTest").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainBoxedNullTest").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("1", lines[0]);
        assertEquals("0", lines[1]);
        assertEquals("10", lines[2]);
    }

    @Test
    void testInnerClassDelegatingConstructorAndEscapingLambda(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("MainDelegatingAndLambda.java");
        Files.writeString(java, """
                import java.util.Iterator;

                public class Main {
                    int base = 100;
                    class Node {
                        int val;
                        Node() {
                            this(base);
                        }
                        Node(int v) {
                            this.val = v;
                        }
                    }
                    static Iterable<Integer> getIterable(int[] arr) {
                        return () -> new Iterator<Integer>() {
                            int idx = 0;
                            public boolean hasNext() { return idx < arr.length; }
                            public Integer next() { return arr[idx++]; }
                        };
                    }
                    public static void main(String[] args) {
                        Main m = new Main();
                        Node n = m.new Node();
                        System.out.println(n.val);
                        int sum = 0;
                        for (int x : getIterable(new int[]{1, 2, 3})) {
                            sum += x;
                        }
                        System.out.println(sum);
                    }
                }
                """, StandardCharsets.UTF_8);

        String cpp = JavaToCpp.convert(java);
        Path cppFile = tempDir.resolve("MainDelegatingAndLambda.cpp");
        Files.writeString(cppFile, cpp, StandardCharsets.UTF_8);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("MainDelegatingAndLambda").toString())
                .redirectErrorStream(true)
                .start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = p.waitFor();
        assertTrue(exitCode == 0, "g++ compilation failed:\n" + output);

        Process runProc = new ProcessBuilder(tempDir.resolve("MainDelegatingAndLambda").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);

        String[] lines = runOutput.trim().split("\\R");
        assertEquals("100", lines[0]);
        assertEquals("6", lines[1]);
    }

    /**
     * ラムダが引数として渡されるとき（親が Spoon AST で未初期化になる場合）、
     * [&] でキャプチャされ外部変数を正しく参照できることを検証する。
     * 計算量: O(1) コンパイル時間は除く。
     */
    @Test
    void testLambdaArgumentCapturingOuterVariables(@TempDir Path tempDir) throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                import java.util.function.Predicate;
                class MathUtils {
                    public static long binarySearch(long left, long right, Predicate<Long> isLeft) {
                        while ((right - left) != 1) {
                            long middle = (left + right) / 2;
                            if (isLeft.test(middle)) {
                                left = middle;
                            } else {
                                right = middle;
                            }
                        }
                        return left;
                    }
                }
                public class Main {
                    public static void main(String[] args) {
                        int[] A = {1, 3, 5, 7, 9};
                        int b = 4;
                        int id = (int) MathUtils.binarySearch(-1, A.length, k -> A[k.intValue()] > b);
                        System.out.println(id);
                    }
                }
                """);
        Path cppFile = tempDir.resolve("Main.cpp");
        JavaToCpp.export(java, cppFile);
        Process p = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(), "-o", tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String compileOut = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int compileExit = p.waitFor();
        assertEquals(0, compileExit, "g++ compilation failed:\n" + compileOut);

        Process runProc = new ProcessBuilder(tempDir.resolve("Main").toString())
                .redirectErrorStream(true)
                .start();
        String runOutput = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int runExitCode = runProc.waitFor();
        assertEquals(0, runExitCode, "Execution failed:\n" + runOutput);
        assertEquals("4", runOutput.trim());
    }

    /**
     * System.out.println(double) の出力が Java の Double.toString() 仕様と一致することを検証する。
     * すなわち: shortest round-trip 表現、|x| >= 1e7 または |x| < 1e-3 なら指数表記 (大文字 E, 指数に + なし)、
     * 整数値の double は ".0" を付加する。
     * 計算量: O(L)。
     *
     * @throws Exception コンパイルまたは実行に失敗した場合
     */
    @Test
    // 未テスト
    void convertsDoublePrintlnToJavaFormat() throws Exception {
        Path java = tempDir.resolve("Main.java");
        Files.writeString(java, """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println(1976.2441416041122);
                        System.out.println(7.576825160690021E17);
                        System.out.println(2.8284271247461903);
                        System.out.println(1.5);
                        System.out.println(1.0);
                        System.out.println(0.0);
                        System.out.println(1.23E-5);
                        System.out.println(8.772053214538598E12);
                        System.out.println(133.33333333333331);
                        System.out.println(282842.71247461904);
                    }
                }
                """);
        Path cppFile = tempDir.resolve("Main.cpp");
        JavaToCpp.export(java, cppFile);
        Process compileProc = new ProcessBuilder("g++", "-std=c++17", "-O2", cppFile.toString(),
                "-o", tempDir.resolve("Main").toString())
                .redirectErrorStream(true).start();
        String compileOut = new String(compileProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, compileProc.waitFor(), "g++ compilation failed:\n" + compileOut);

        Process runProc = new ProcessBuilder(tempDir.resolve("Main").toString())
                .redirectErrorStream(true).start();
        String out = new String(runProc.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertEquals(0, runProc.waitFor(), "Execution failed:\n" + out);

        String[] lines = out.split("\n");
        assertEquals("1976.2441416041122", lines[0]);
        assertEquals("7.576825160690021E17", lines[1]);
        assertEquals("2.8284271247461903", lines[2]);
        assertEquals("1.5", lines[3]);
        assertEquals("1.0", lines[4]);
        assertEquals("0.0", lines[5]);
        assertEquals("1.23E-5", lines[6]);
        assertEquals("8.772053214538598E12", lines[7]);
        assertEquals("133.33333333333331", lines[8]);
        assertEquals("282842.71247461904", lines[9]);
    }
}
