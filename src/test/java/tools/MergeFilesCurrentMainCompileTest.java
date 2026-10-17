package tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import library.tools.MergeFiles;

public class MergeFilesCurrentMainCompileTest {

	/**
	 * 現在の solver Main を出力し、Java 21 でコンパイルできることを確かめる。
	 * 計算量: O(S + C)。S は export、C はコンパイルの処理時間。
	 *
	 * @param tempDir 出力先
	 * @throws IOException 入出力エラー
	 */
	// 未テスト
	@Test
	public void compiles(@TempDir Path tempDir) throws IOException {
		Path output = tempDir.resolve("Main.java");
		MergeFiles.export("Main", "solver/src/main/java/Main.java", output.toString());
		assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
				"--release", "21", "-d", tempDir.toString(), output.toString()));
	}
}
