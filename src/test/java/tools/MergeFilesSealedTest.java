package tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import library.tools.MergeFiles;

public class MergeFilesSealedTest {

	/**
	 * sealed interface と sealed class の permits にのみ現れる型が出力され、出力がコンパイル可能であることを検証する。
	 * 計算量: O(S + C)。S は export の処理時間、C は出力のコンパイル時間。
	 *
	 * @param tempDir 出力先
	 * @throws IOException 入出力エラー
	 */
	// 未テスト
	@Test
	public void testPermittedTypesAreRetained(@TempDir Path tempDir) throws IOException {
		Path entry = Path.of("solver/src/main/java/MergeSealedEntry.java");
		Path libraryDir = Path.of("src/main/java/library/merge_sealed_test");
		Path contract = libraryDir.resolve("SealedContract.java");
		Path interfaceFirst = libraryDir.resolve("InterfaceFirst.java");
		Path interfaceSecond = libraryDir.resolve("InterfaceSecond.java");
		Path parent = libraryDir.resolve("SealedParent.java");
		Path classFirst = libraryDir.resolve("ClassFirst.java");
		Path classSecond = libraryDir.resolve("ClassSecond.java");
		Path output = tempDir.resolve("MergeSealedEntry.java");
		try {
			Files.createDirectories(entry.getParent());
			Files.createDirectories(libraryDir);
			Files.writeString(entry, """
					import library.merge_sealed_test.SealedContract;
					import library.merge_sealed_test.InterfaceFirst;
					import library.merge_sealed_test.SealedParent;
					import library.merge_sealed_test.ClassFirst;
					import library.tools.MergeFiles;
					public class MergeSealedEntry {
					    public static void main(String[] args) throws Exception {
					        SealedContract a = new InterfaceFirst();
					        SealedParent b = new ClassFirst();
					        System.out.println(a.getClass().getName() + b.getClass().getName());
					        MergeFiles.export();
					    }
					}
					""");
			Files.writeString(contract, """
					package library.merge_sealed_test;
					public sealed interface SealedContract permits InterfaceFirst, InterfaceSecond {}
					""");
			Files.writeString(interfaceFirst, """
					package library.merge_sealed_test;
					public final class InterfaceFirst implements SealedContract {}
					""");
			Files.writeString(interfaceSecond, """
					package library.merge_sealed_test;
					public final class InterfaceSecond implements SealedContract {}
					""");
			Files.writeString(parent, """
					package library.merge_sealed_test;
					public sealed abstract class SealedParent permits ClassFirst, ClassSecond {}
					""");
			Files.writeString(classFirst, """
					package library.merge_sealed_test;
					public final class ClassFirst extends SealedParent {}
					""");
			Files.writeString(classSecond, """
					package library.merge_sealed_test;
					public final class ClassSecond extends SealedParent {}
					""");

			MergeFiles.export("MergeSealedEntry", entry.toString(), output.toString());
			String merged = Files.readString(output);
			assertTrue(merged.contains("class InterfaceSecond"));
			assertTrue(merged.contains("class ClassSecond"));
			assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
					"--release", "21", "-d", tempDir.toString(), output.toString()));
		} finally {
			Files.deleteIfExists(entry);
			Files.deleteIfExists(contract);
			Files.deleteIfExists(interfaceFirst);
			Files.deleteIfExists(interfaceSecond);
			Files.deleteIfExists(parent);
			Files.deleteIfExists(classFirst);
			Files.deleteIfExists(classSecond);
			Files.deleteIfExists(libraryDir);
		}
	}
}
