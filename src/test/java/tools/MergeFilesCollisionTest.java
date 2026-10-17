package tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import library.tools.MergeFiles;

public class MergeFilesCollisionTest {

	/**
	 * 異なる package の同名型を両方参照した出力が Java ソースとしてコンパイル可能であることを検証する。
	 * 計算量: O(S + C)。S は export の処理時間、C は出力のコンパイル時間。
	 *
	 * @param tempDir 出力先
	 * @throws IOException 入出力エラー
	 */
	// 未テスト
	@Test
	public void testReachableNameCollision(@TempDir Path tempDir) throws IOException {
		Path entry = Path.of("solver/src/main/java/MergeCollisionEntry.java");
		Path first = Path.of("src/main/java/library/merge_collision_a/Node.java");
		Path second = Path.of("src/main/java/library/merge_collision_b/Node.java");
		Path output = tempDir.resolve("MergeCollisionEntry.java");
		try {
			Files.createDirectories(entry.getParent());
			Files.createDirectories(first.getParent());
			Files.createDirectories(second.getParent());
			Files.writeString(entry, """
					public class MergeCollisionEntry {
					    public static void main(String[] args) {
					        System.out.println(new library.merge_collision_a.Node().value()
					                + new library.merge_collision_b.Node().value());
					    }
					}
					""");
			Files.writeString(first, """
					package library.merge_collision_a;
					public class Node { public int value() { return 1; } }
					""");
			Files.writeString(second, """
					package library.merge_collision_b;
					public class Node { public int value() { return 2; } }
					""");

			MergeFiles.export("MergeCollisionEntry", entry.toString(), output.toString());
			String merged = Files.readString(output);
			assertTrue(merged.contains("class Node"));
			assertTrue(merged.contains("class LibraryMerge_collision_bNode"));
			assertTrue(ToolProvider.getSystemJavaCompiler().run(null, null, null,
					"-d", tempDir.toString(), output.toString()) == 0);
		} finally {
			Files.deleteIfExists(entry);
			Files.deleteIfExists(first);
			Files.deleteIfExists(second);
			Files.deleteIfExists(first.getParent());
			Files.deleteIfExists(second.getParent());
		}
	}
}
