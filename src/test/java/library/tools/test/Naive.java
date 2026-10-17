package library.tools.test;

import java.io.IOException;

import org.eclipse.jdt.internal.compiler.batch.Main;

import library.tools.FastScanner;
import library.tools.MyPrintWriter;

public class Naive {
    static MyPrintWriter pw = MyPrintWriter.getInstance();

    static FastScanner sc = FastScanner.getInstance();

    public static void main(String[] args) throws IOException {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> System.exit(1));
        new Naive().run();
        // new Main().check();
		pw.flush();
	}

	void run() {
        int N = sc.nextInt();
        long S = sc.nextLong();
        long[] A = sc.nextLongs(N);
        System.out.println("Yes");
	}
		
}

