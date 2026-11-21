package library.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * C++ 側の Java 競プロ互換ランタイム構造体群の定義をリソースファイルから読み込み結合するユーティリティクラス。
 */
public final class CppRuntimePrelude {
    private CppRuntimePrelude() {}

    private static final String[] PRELUDE_FILES = {
        "Core.cpp",
        "StringHelpers.cpp",
        "String.cpp",
        "Date.cpp",
        "Comparator.cpp",
        "ArrayAlgorithms.cpp",
        "InputStream.cpp",
        "Hash.cpp",
        "StringBuilder.cpp",
        "IOHelpers.cpp",
        "MathHelpers.cpp",
        "Optional.cpp",
        "List.cpp",
        "Stream.cpp",
        "Set.cpp",
        "Map.cpp",
        "Iterator.cpp",
        "Deque.cpp",
        "Stack.cpp",
        "Boolean.cpp",
        "Objects.cpp",
        "PrimitiveIterator.cpp",
        "Arrays.cpp",
        "PriorityQueue.cpp",
        "System.cpp",
        "StringTokenizer.cpp",
        "Scanner.cpp",
        "BigInteger.cpp",
        "BigDecimal.cpp",
        "PrintWriter.cpp",
        "Runtime.cpp",
        "Long.cpp",
        "Integer.cpp",
        "Collections.cpp",
        "Line2D.cpp",
        "Math.cpp",
        "Double.cpp",
        "ComparatorUtils.cpp",
        "Character.cpp",
        "Interfaces.cpp",
        "Thread.cpp",
        "Random.cpp",
        "BitSet.cpp"
    };

    /** 常に含まれる基本プレリュード群。 */
    private static final Set<String> BASE_PRELUDE_FILES = Set.of(
        "Core.cpp",
        "StringHelpers.cpp",
        "String.cpp",
        "ArrayAlgorithms.cpp",
        "Hash.cpp",
        "IOHelpers.cpp",
        "MathHelpers.cpp",
        "Comparator.cpp",
        "Interfaces.cpp",
        "Arrays.cpp"
    );

    /** 条件付きプレリュードファイルの検出トリガー文字列一覧。 */
    private static final Map<String, List<String>> FILE_TRIGGERS = Map.ofEntries(
        Map.entry("Date.cpp", List.of("JDate", "jcurrentTimeMillis", "jnanoTime")),
        Map.entry("InputStream.cpp", List.of("JInputStream")),
        Map.entry("StringBuilder.cpp", List.of("JStringBuilder")),
        Map.entry("Optional.cpp", List.of("JOptional", "Optional", "OptionalLong", "OptionalInt", "OptionalDouble")),
        Map.entry("Stream.cpp", List.of("JStream", "JIntStream", "JLongStream", ".stream()", "->stream()")),
        Map.entry("List.cpp", List.of("JList", "jlistOf")),
        Map.entry("Set.cpp", List.of("JSet", "jsetOf")),
        Map.entry("Map.cpp", List.of("JMap")),
        Map.entry("Deque.cpp", List.of("JDeque")),
        Map.entry("Stack.cpp", List.of("JStack")),
        Map.entry("Iterator.cpp", List.of("JIterator", "JIterable", "CppIterator")),
        Map.entry("Boolean.cpp", List.of("JBoolean", "Boolean::")),
        Map.entry("Objects.cpp", List.of("Objects::", "Objects.")),
        Map.entry("PrimitiveIterator.cpp", List.of("PrimitiveIterator", "JPrimitiveIterator")),
        Map.entry("PriorityQueue.cpp", List.of("JPriorityQueue")),
        Map.entry("System.cpp", List.of("System::")),
        Map.entry("StringTokenizer.cpp", List.of("JStringTokenizer")),
        Map.entry("Scanner.cpp", List.of("JScanner")),
        Map.entry("BigInteger.cpp", List.of("JBigInteger")),
        Map.entry("BigDecimal.cpp", List.of("JBigDecimal")),
        Map.entry("PrintWriter.cpp", List.of("JPrintWriter")),
        Map.entry("Runtime.cpp", List.of("JRuntime")),
        Map.entry("Long.cpp", List.of("JLong")),
        Map.entry("Integer.cpp", List.of("JInteger")),
        Map.entry("Collections.cpp", List.of("JCollections")),
        Map.entry("Line2D.cpp", List.of("Line2D")),
        Map.entry("Math.cpp", List.of("JMath")),
        Map.entry("Double.cpp", List.of("JDouble")),
        Map.entry("ComparatorUtils.cpp", List.of("JComparatorUtils")),
        Map.entry("Character.cpp", List.of("JCharacter")),
        Map.entry("Thread.cpp", List.of("JThread")),
        Map.entry("Random.cpp", List.of("JRandom")),
        Map.entry("BitSet.cpp", List.of("JBitSet"))
    );

    /** 条件付きプレリュードファイル間の依存関係。 */
    private static final Map<String, List<String>> FILE_DEPENDENCIES = Map.of(
        "PrimitiveIterator.cpp", List.of("Iterator.cpp"),
        "Deque.cpp", List.of("Iterator.cpp"),
        "Stream.cpp", List.of("Optional.cpp", "List.cpp"),
        "Map.cpp", List.of("Set.cpp", "List.cpp"),
        "PrintWriter.cpp", List.of("InputStream.cpp"),
        "Set.cpp", List.of("List.cpp"),
        "System.cpp", List.of("Date.cpp")
    );

    /**
     * 指定されたコード片で使用されている型・メソッドに基づき、必要なプレリュード定義文字列のみを取得する。
     *
     * 計算量: O(N + M)（N はプレリュード全ファイルの合計バイト数、M はコード長）
     *
     * @param bodyCode 生成された C++ 本文コード
     * @return 必要な C++ プレリュードの文字列
     */
    public static String preludeFor(String bodyCode) {
        // 未テスト
        Set<String> needed = new HashSet<>(BASE_PRELUDE_FILES);
        for (Map.Entry<String, List<String>> entry : FILE_TRIGGERS.entrySet()) {
            String file = entry.getKey();
            for (String trigger : entry.getValue()) {
                if (bodyCode.contains(trigger)) {
                    needed.add(file);
                    break;
                }
            }
        }

        // 依存関係の解決
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<String>> entry : FILE_DEPENDENCIES.entrySet()) {
                if (needed.contains(entry.getKey())) {
                    for (String dep : entry.getValue()) {
                        if (needed.add(dep)) {
                            changed = true;
                        }
                    }
                }
            }
        }

        return prelude(needed);
    }

    /**
     * 指定されたプレリュードファイル集合の定義文字列を取得する。順序は PRELUDE_FILES の定義順を維持する。
     *
     * 計算量: O(N)（N は選択されたプレリュードファイルの合計バイト数）
     *
     * @param selectedFiles 読み込むプレリュードファイル名の集合
     * @return C++ プレリュードの文字列
     */
    public static String prelude(Set<String> selectedFiles) {
        // 未テスト
        StringBuilder sb = new StringBuilder();
        for (String file : PRELUDE_FILES) {
            if (!selectedFiles.contains(file)) {
                continue;
            }
            String path = "/cpp/" + file;
            try (InputStream is = CppRuntimePrelude.class.getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalStateException("Resource not found: " + path);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read prelude resource: " + path, e);
            }
        }
        return sb.toString();
    }

    /**
     * C++ ランタイムのプレリュード定義全文字列を取得する。
     *
     * 計算量: O(N)（N はプレリュード全ファイルの合計バイト数）
     *
     * @return C++ プレリュードの文字列
     */
    public static String prelude() {
        // 未テスト
        StringBuilder sb = new StringBuilder();
        for (String file : PRELUDE_FILES) {
            String path = "/cpp/" + file;
            try (InputStream is = CppRuntimePrelude.class.getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalStateException("Resource not found: " + path);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read prelude resource: " + path, e);
            }
        }
        return sb.toString();
    }
}
