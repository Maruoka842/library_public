package library.tools.test;

import java.util.Random;

import library.tools.FastScanner;
import library.tools.MyPrintWriter;
import library.util.ArrayUtils;
import library.util.collections.IntTreapSet;
import library.util.fold.CircularLongSum;

public class Generator {
	static MyPrintWriter pw=MyPrintWriter.getInstance();
	static FastScanner sc=FastScanner.getInstance();
	
	public static void main(String[] args) {
        Random rnd = new Random();
        int N=rnd.nextInt(1,5);
        int[]A=ArrayUtils.randomIntArray(1, 10, N);
        var ds=new CircularLongSum(A);
        int i=rnd.nextInt(0, N);
        int j=rnd.nextInt(i+1, 5*N);
        long sum=ds.sum(i, j);
        pw.println(N+" "+sum);
        pw.println(A);
        pw.flush();
        //5+8+6+4=23
        //86%46=40
	}
}
