package library.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * problems.txt の問題を管理・選択するユーティリティクラス。
 */
public class ProblemSelector {

    /**
     * 指定されたファイルパスから問題リストを取得し、'o' で理解完了となっていない問題を除外したリストからランダムに1つの問題名を選択して返します。
     * 行が "abc213 : ABE" や "abc213 : ABEo" や "abc213 o" や "abc213" のような形式に対応しており、
     * 出力時は問題名（コロンや原因コードを除いた部分）のみを返します。
     *
     * @param filePath 対象のファイルパス
     * @return ランダムに選択された問題名の文字列。対象の問題が存在しない場合は null
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static String getRandomProblem(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        List<String> available = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!isSolved(trimmed)) {
                available.add(extractProblemName(trimmed));
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return available.get(random.nextInt(available.size()));
    }

    /**
     * デフォルトのパス "problems.txt" から問題リストを取得し、理解完了となっていない問題からランダムに1つの問題名を選択して返します。
     *
     * @return ランダムに選択された問題名の文字列。対象の問題が存在しない場合は null
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static String getRandomProblem() throws IOException {
        return getRandomProblem("problems.txt");
    }

    /**
     * 指定されたファイルパスから問題リストを取得し、'o' がついている（理解完了した）問題の個数を返します。
     *
     * @param filePath 対象のファイルパス
     * @return 理解完了した問題の個数
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static int getSolvedCount(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && isSolved(trimmed)) {
                count++;
            }
        }
        return count;
    }

    /**
     * デフォルトのパス "problems.txt" から問題リストを取得し、理解完了した問題の個数を返します。
     *
     * @return 理解完了した問題の個数
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static int getSolvedCount() throws IOException {
        return getSolvedCount("problems.txt");
    }

    /**
     * 指定されたファイルパスから問題リストを取得し、理解完了した問題の割合（パーセンテージ 0.0〜100.0）を返します。
     *
     * @param filePath 対象のファイルパス
     * @return 理解完了した問題の割合（0.0 〜 100.0）。総問題数が 0 の場合は 0.0
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static double getSolvedPercentage(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        int total = 0;
        int solved = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                total++;
                if (isSolved(trimmed)) {
                    solved++;
                }
            }
        }
        if (total == 0) {
            return 0.0;
        }
        return (double) solved / total * 100.0;
    }

    /**
     * デフォルトのパス "problems.txt" から問題リストを取得し、理解完了した問題の割合（パーセンテージ 0.0〜100.0）を返します。
     *
     * @return 理解完了した問題の割合（0.0 〜 100.0）。総問題数が 0 の場合は 0.0
     * @throws IOException ファイル読み込みエラーが発生した場合
     * 
     * 計算量: O(N) （N はファイル全体の行数）
     */
    // 未テスト
    public static double getSolvedPercentage() throws IOException {
        return getSolvedPercentage("problems.txt");
    }

    /**
     * 行文字列から問題名部分（コロンより前の部分、または最初のトークン）のみを抽出します。
     *
     * @param line 対象の行文字列
     * @return 抽出された問題名
     * 
     * 計算量: O(L) （L は行文字列の長さ）
     */
    // 未テスト
    private static String extractProblemName(String line) {
        if (line.contains(":")) {
            return line.split(":", 2)[0].trim();
        }
        String[] parts = line.split("\\s+");
        return parts[0].trim();
    }

    /**
     * 行が理解完了（末尾が 'o' またはコロン以降に 'o' が含まれる/末尾が 'o'）しているかを判定します。
     *
     * @param line 対象の行文字列
     * @return 理解完了している場合は true
     * 
     * 計算量: O(L) （L は行文字列の長さ）
     */
    // 未テスト
    private static boolean isSolved(String line) {
        return line.trim().endsWith("o");
    }
}
