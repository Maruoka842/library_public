package tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import library.tools.MergeFiles;

/**
 * MergeFiles において Record や final フィールドを持つクラスのコンストラクタ保持に関するユニットテスト。
 */
public class MergeFilesRecordTest {

    /**
     * final フィールドを持つクラスおよび Record がマージされた際、コンストラクタが削除されずにコンパイル可能であることを検証する。
     *
     * 計算量: O(N)
     *
     * @throws IOException 入出力エラーが発生した場合
     */
    // 未テスト
    @Test
    public void testClassAndRecordConstructorRetention() throws IOException {
        String name = "Main";
        Path solverDir = Paths.get("solver/src/main/java");
        Files.createDirectories(solverDir);
        Path input = solverDir.resolve("Main.java");

        Path libDir = Paths.get("src/main/java/library/util");
        Path dummyLib = libDir.resolve("DummySlope.java");

        Path tempDir = Files.createTempDirectory("merge_test_class_bug");
        Path output = tempDir.resolve("Main.java");

        try {
            Files.writeString(dummyLib, """
                    package library.util;

                    public class DummySlope {
                        public interface RestoreNode {
                            public static class Shift implements RestoreNode {
                                final long lo;
                                final long w;
                                final boolean bounded;
                                final RestoreNode prev;

                                public Shift(long lo, long w, boolean bounded, RestoreNode prev) {
                                    this.lo = lo;
                                    this.w = w;
                                    this.bounded = bounded;
                                    this.prev = prev;
                                }
                            }

                            public static class Add implements RestoreNode {
                                final long dx;
                                final RestoreNode prev;

                                public Add(long dx, RestoreNode prev) {
                                    this.dx = dx;
                                    this.prev = prev;
                                }
                            }

                            public static class Merge implements RestoreNode {
                                final RestoreNode a;
                                final RestoreNode b;

                                public Merge(RestoreNode a, RestoreNode b) {
                                    this.a = a;
                                    this.b = b;
                                }
                            }

                            record Point(int x, int y) implements RestoreNode {}
                        }

                        public void processShift(RestoreNode.Shift s) {
                            System.out.println(s.lo + " " + s.w + " " + s.bounded + " " + s.prev);
                        }

                        public void processAdd(RestoreNode.Add a) {
                            System.out.println(a.dx + " " + a.prev);
                        }

                        public void processMerge(RestoreNode.Merge m) {
                            System.out.println(m.a + " " + m.b);
                        }

                        public void processPoint(RestoreNode.Point p) {
                            System.out.println(p.x() + " " + p.y());
                        }

                        public void runDP() {
                            processShift(null);
                            processAdd(null);
                            processMerge(null);
                            processPoint(null);
                        }
                    }
                    """);

            Files.writeString(input, """
                    import java.io.IOException;
                    import library.tools.MergeFiles;
                    import library.util.DummySlope;

                    public class Main {
                        public static void main(String[] args) throws IOException {
                            new Main().run();
                            MergeFiles.export();
                        }

                        void run() {
                            DummySlope ds = new DummySlope();
                            ds.runDP();
                        }
                    }
                    """);

            MergeFiles.export(name, "solver/src/main/java/Main.java", output.toString());

            String mergedContent = Files.readString(output);
            boolean ok = compileJava(output);

            assertTrue(ok, "Merged Main.java failed to compile");
        } finally {
            Files.deleteIfExists(dummyLib);
            deleteDirectory(tempDir);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        }
    }

    private static boolean compileJava(Path javaFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK required");

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);

        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(javaFile.toFile());

        Path compileOut = Files.createTempDirectory("compile_out_class_bug");
        List<String> options = List.of("-d", compileOut.toString(), "--release", "21");

        JavaCompiler.CompilationTask task =
                compiler.getTask(null, fileManager, diagnostics, options, null, units);

        boolean success = task.call();
        fileManager.close();

        if (!success) {
            System.err.println("Compilation errors for " + javaFile + ":");
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                System.err.printf("  [%s] line %d: %s%n", d.getKind(), d.getLineNumber(), d.getMessage(null));
            }
        }
        return success;
    }
}
