import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import library.tools.FastScanner;
import library.tools.JavaToCpp;
import library.tools.MergeFiles;
import library.tools.MyPrintWriter;
import library.util.ArrayUtils;
import library.util.Fp;
import library.util.algebra.strategy.longs.LongAddAbelianGroupStrategy;
import library.util.fold.StaticPointAddRangeSum3D;
import library.util.seq.GeneralizedSuffixTree;
import library.util.seq.SuffixTree;
import library.util.unionfind.LongEdgeValueUnionFind;
import library.util.unionfind.VertexValueUnionFind;
import library.util.unionfind.VertexValueUnionFindFactory;

public class Main implements Runnable {

	public static void main(String[] args) throws IOException {
		// Runtime runtime = Runtime.getRuntime();
// new Thread(null, new Main(), "MainThreadWithLargeStack", (1024 * 1024) *1024).start();
		new Main().run();
// long usedMemory = runtime.totalMemory() - runtime.freeMemory();
// System.err.printf("使用メモリ: %.2f MB%n", usedMemory / 1024.0 / 1024.0);
		MyPrintWriter.getInstance().flush();
		MergeFiles.export();
		JavaToCpp.export();
	}

	Random rnd = new Random();
	int N;
	@java.lang.Override
	public void run() {
		FastScanner sc = FastScanner.getInstance();
		MyPrintWriter pw = MyPrintWriter.getInstance();
		SuffixTree tree=new SuffixTree(null, null, null);
		GeneralizedSuffixTree st;
	}
	
		

	void tr(Object... objects) {
		System.out.println(Arrays.deepToString(objects));
	}
}
