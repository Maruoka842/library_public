import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import library.tools.FastScanner;
import library.tools.JavaToCpp;
import library.tools.MergeFiles;
import library.tools.MyPrintWriter;
import library.util.ArrayStatistics;
import library.util.ArrayUtils;
import library.util.geometry.GeometryUtils;

public class Main {
	static MyPrintWriter pw = MyPrintWriter.getInstance();
	static FastScanner sc = FastScanner.getInstance();

	public static void main(String[] args) throws IOException {
		new Main().run();
		pw.flush();
		MergeFiles.export();
		JavaToCpp.export();
	}

	void run() {
		for(int i=0;i<8;++i) {
			int v=i*(i+1)%8;
			if(i%2==1)v^=2;
			if(i/2%2==1)v^=2^4;
			if(i/4%2==1)v^=4;
			System.out.println(Integer.toBinaryString(v));
		}
		//x^2+x+1
		//x(x+1)+1
		//x(x+1)は偶数
		//x(x+1)
		//(4a+2b+c)^2+(4a+2b+c)
		//=4b+4bc+4b+c+4a+2b+c
		//=4a+6b+4bc+2c

		//=2a+3b+2bc+c
		//=
	}

	
//	000
//	010
//	110
//	100
//	100
//	110
//	010
//	000

	
	
	static int nextCyclic(int[][] nxt, int cur, int val, int n) {
		if (cur + 1 < n) return nxt[cur + 1][val];
		return nxt[0][val];
	}
	
	void tr(Object... objects) {
		System.out.println(Arrays.deepToString(objects));
	}
}
