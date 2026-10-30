package library.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        "ArrayAlgorithms.cpp",
        "InputStream.cpp",
        "Hash.cpp",
        "StringBuilder.cpp",
        "IOHelpers.cpp",
        "MathHelpers.cpp",
        "Comparator.cpp",
        "Optional.cpp",
        "Stream.cpp",
        "List.cpp",
        "Set.cpp",
        "Map.cpp",
        "Deque.cpp",
        "Stack.cpp",
        "Iterator.cpp",
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
