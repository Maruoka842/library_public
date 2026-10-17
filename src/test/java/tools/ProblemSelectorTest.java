package tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import library.tools.ProblemSelector;

public class ProblemSelectorTest {

    @Test
    public void testProblemSelectorWithColon(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("problems.txt");
        List<String> lines = List.of(
            "abc212 : AB",
            "abc213 : ABEo",
            "abc214 : C",
            "abc215 : o",
            "abc216"
        );
        Files.write(file, lines);

        String pathStr = file.toString();

        // abc213 と abc215 が解決済み
        assertEquals(2, ProblemSelector.getSolvedCount(pathStr));
        assertEquals(40.0, ProblemSelector.getSolvedPercentage(pathStr), 1e-9);

        // 未解の問題として abc212, abc214, abc216 の問題名のみが抽出されて選出されること
        for (int i = 0; i < 30; i++) {
            String selected = ProblemSelector.getRandomProblem(pathStr);
            assertNotNull(selected);
            assertTrue(selected.equals("abc212") || selected.equals("abc214") || selected.equals("abc216"));
        }
    }

    @Test
    public void testAllSolved(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("problems_all_solved.txt");
        List<String> lines = List.of(
            "abc212 : AB o",
            "abc213 : o"
        );
        Files.write(file, lines);

        String pathStr = file.toString();

        assertEquals(2, ProblemSelector.getSolvedCount(pathStr));
        assertEquals(100.0, ProblemSelector.getSolvedPercentage(pathStr), 1e-9);
        assertNull(ProblemSelector.getRandomProblem(pathStr));
    }
}
