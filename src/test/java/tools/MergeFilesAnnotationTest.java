package tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import library.tools.MergeFiles;

public class MergeFilesAnnotationTest {

    @Test
    public void testFieldAnnotationCase() throws IOException {
        Path solverDir = Paths.get("solver/src/main/java");
        Files.createDirectories(solverDir);
        Path input = solverDir.resolve("Main.java");

        Path tempDir = Files.createTempDirectory("merge_test_annotation");
        Path output = tempDir.resolve("Main.java");

        Files.writeString(input, """
import java.io.IOException;

import library.tools.MergeFiles;

public class Main {
    @interface Tag {}

    @Tag
    static int X = 1;

    public static void main(String[] args) throws IOException {
        System.out.println(X);

        MergeFiles.export();
    }
}
""");

        MergeFiles.export("Main", "solver/src/main/java/Main.java", output.toString());

        String mergedContent = Files.readString(output);
        boolean ok = compileJava(output);
        assertTrue(ok, "Merged Main.java failed to compile");
        assertTrue(mergedContent.contains("@interface Tag") || mergedContent.contains("@interface Tag {"), "Merged code should retain @interface Tag declaration");
    }

    private static boolean compileJava(Path javaFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(javaFile.toFile());
        Path compileOut = Files.createTempDirectory("compile_out_annotation");
        List<String> options = List.of("-d", compileOut.toString(), "--release", "21");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, units);
        boolean success = task.call();
        fileManager.close();
        if (!success) {
            for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                System.err.printf("  [%s] line %d: %s%n", d.getKind(), d.getLineNumber(), d.getMessage(null));
            }
        }
        return success;
    }
}
