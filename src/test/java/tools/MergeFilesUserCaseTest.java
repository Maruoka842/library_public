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

public class MergeFilesUserCaseTest {

    @Test
    public void testUserCase() throws IOException {
        String name = "Main";
        Path solverDir = Paths.get("solver/src/main/java");
        Files.createDirectories(solverDir);
        Path input = solverDir.resolve("Main.java");

        Path tempDir = Files.createTempDirectory("merge_test_user_case");
        Path output = tempDir.resolve("Main.java");

        Files.writeString(input, """
import java.io.IOException;
import java.util.Arrays;

import library.tools.FastScanner;
import library.tools.MergeFiles;
import library.tools.MyPrintWriter;
import library.util.ArrayUtils;
import library.util.Fp;
import library.util.polynomial.PolynomialFpDynamic;
import library.util.poset.BooleanLattice;
import library.util.seq.Permutation;

public class Main {
	static MyPrintWriter pw = MyPrintWriter.getInstance();
	static FastScanner sc = FastScanner.getInstance();

	public static void main(String[] args) throws IOException {
		new Main().run();
//		new Main().check();
		pw.flush();
		MergeFiles.export();
	}

    void run() {
	var P=PolynomialFpDynamic.MOD998244353;
	var fp=Fp.MOD998244353;
	long mod=P.mod;
	int N=sc.nextInt();
	long[]A=sc.nextLongs(N);
	Arrays.sort(A);
	ArrayUtils.incrementAll(A);
	long[]f=Permutation.rookPolynomialOfFerrers(A, mod);
	long[]g=new long[f.length];
	for (int i = 0; i < g.length; i++) {
			g[i]=f[i]*fp.fac(N-i)%mod;
		}
	long[]h=BooleanLattice.supsetRankedMoebius(g, mod);
	long ans=0;
	// i < A+1 OK

	for (int i = 0; i < h.length; i++) {
		if(i%2==0)
			ans+=h[i]*N;
			ans%=mod;
		}
	ans=fp.mul(ans, fp.ifac(N));
	pw.println(ans);
    }



	void tr(Object... objects) {
		System.out.println(Arrays.deepToString(objects));
	}
}
""");

        MergeFiles.export(name, "solver/src/main/java/Main.java", output.toString());

        String mergedContent = Files.readString(output);
        boolean ok = compileJava(output);

        if (!ok) {
            System.out.println("--- Generated code (first 150 lines) ---");
            String[] lines = mergedContent.split("\n");
            for (int i = 0; i < Math.min(lines.length, 150); i++) {
                System.out.println((i+1) + ": " + lines[i]);
            }
        }

        try {
            assertTrue(ok, "Merged Main.java failed to compile");
        } finally {
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

        Path compileOut = Files.createTempDirectory("compile_out_case");
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
