package library.util.graph.grid;

/**
 * {@code n × n} 正方格子グラフ上に長い誘導サイクルを構成する。
 *
 * <p>頂点は {@code (r, c)} を {@code r * n + c} で表す。
 * {@link #construct(int)} は、選んだ頂点が誘導する部分グラフがちょうど1本の
 * サイクルとなるような頂点列を、サイクル上の順番で返す。</p>
 *
 * <p>{@code n >= 11} に対し、構成されるサイクルの長さは
 * {@code (2*n*n - 4*n + C[n%6]) / 3},
 * {@code C = {30, 26, 24, 30, 32, 30}} である。
 * これは構成的な下界であり、常に最長の誘導サイクルを返すことを保証するものではない。</p>
 *
 * @complexity O(n^2)
 */
public final class SquareGridInducedCycle {
    private SquareGridInducedCycle() {}

    private static final int[] C = {30, 26, 24, 30, 32, 30};
    private static final String B16 =
            "XXXXXX.XXXXX.XXX/X....XXX...XXX.X/X.XXX...XXX...XX/X.X.XX..X.XX..X./" +
            "X.XX.XX.XX.XX.XX/XX.XX.XX.XX.XX.X/.X..XX.XX.XX.XXX/XX...XX.XX.XX.../" +
            "X.XXX.XX.XX.XX../X.X.XX.XX.XX.XX./X.XX.XX.XX.XX.XX/XX.X..XX.XX.XX.X/" +
            ".X.XXX.XX.XX.X.X/XX...XX.XX.X.X.X/X.XXX.X..X.X.X.X/XXX.XXX..XXX.XXX";
    private static final String B12 =
            "XXX.XXX.XXXX/X.X.X.XXX..X/X.X.XX...XXX/X.XX.XX..X../XX.XX.XX.XX./.X..XX.XX.XX/" +
            "XX...XX.XX.X/X.XXX.XX.X.X/X.X.XX.XXX.X/X.XX.X.....X/X..X.X.XXX.X/XXXX.XXX.XXX";
    private static final String B15 =
            "XXXXX.XXXXX.XXX/X...XXX...XXX.X/XXXX...XXX...XX/...XX..X.XX..X./XXX.XX.XX.XX.XX/" +
            "X.XX.XX.XX.XX.X/XX.XX.XX.XX.XXX/.X..XX.XX.XX.../XX...XX.XX.XXXX/X.XXX.XX.XX...X/" +
            "X.X.XX.XX.XX.XX/X.XX.XX.XX.X.X./X..XX.XX.XXX.XX/X...X..X......X/XXXXX..XXXXXXXX";

    /**
     * {@code n x n} 正方格子上の誘導サイクルをサイクル順に返す。
     *
     * @param n 格子の一辺の長さ
     * @return 頂点 {@code (r,c)} を {@code r*n+c} で表したサイクル順の頂点列
     * @throws IllegalArgumentException {@code n < 11} の場合
     * @complexity O(n^2)
     */
    public static int[] construct(int n) {
        if (n < 11) throw new IllegalArgumentException("n < 11: " + n);
        boolean[] a = n % 6 == 0 ? family(n, 12, B12) : n % 6 == 3 ? family(n, 15, B15) : common(n);
        return order(a, n);
    }

    /**
     * {@link #construct(int)} が構成するサイクルの頂点数を返す。
     *
     * @param n 格子の一辺の長さ
     * @return 構成される誘導サイクルの頂点数
     * @throws IllegalArgumentException {@code n < 11} の場合
     * @complexity O(1)
     */
    public static long length(int n) {
        if (n < 11) throw new IllegalArgumentException("n < 11: " + n);
        return (2L * n * n - 4L * n + C[n % 6]) / 3;
    }

    private static boolean[] family(int n, int m, String s) {
        boolean[] a = base(s);
        for (; m < n; m += 6) a = extend(a, m, m % 6);
        return a;
    }

    private static boolean[] common(int n) {
        int m = 16;
        while (m < n + 6) m += 6;
        boolean[] b = family(m, 16, B16), a = new boolean[n * n];
        for (int r = 0; r < n; r++) System.arraycopy(b, r * m, a, r * n, n);

        int p = (n % 6 + 2) % 6;
        while (p < 5) p += 6;
        for (; p + 2 < n - 6; p += 6) {
            edit(a,n,true, p,-1, -1,p);
            edit(a,n,false, p+1,-2, p+1,-1, p+2,-1, -2,p+1, -1,p+1, -1,p+2);
        }
        corner(a, n);
        return a;
    }

    private static boolean[] extend(boolean[] a, int n, int r) {
        int m = n + 6;
        if (r == 4) {
            boolean[] b = cross(a,n,4,5,0);
            edit(b,m,false, 1,8,1,9,2,6,2,7,3,7, 6,2,7,2,7,3,8,1,9,1,
                    7,-2,7,-1,8,-1, -2,7,-2,10,-1,7,-1,8);
            edit(b,m,true, 0,9,1,7,2,8, 7,1,8,2,9,0, 6,-1,-1,6,-1,9);
            return b;
        }
        if (r == 0) {
            boolean[] b = cross(a,n,4,5,2);
            edit(b,m,true, 0,10,1,8,2,9,6,1,7,2,8,0,7,-1,-1,9);
            edit(b,m,false, 1,9,1,10,2,7,2,8,3,8,5,2,6,2,6,3,7,1,8,1,
                    8,-2,8,-1,9,-1,-2,10,-1,5,-1,10);
            return b;
        }
        if (r == 3) {
            boolean[] b = cross(a,n,5,7,1);
            edit(b,m,true, 0,8,1,12,2,7,6,-1,10,0,-3,11,-2,12,-2,14,-1,10);
            edit(b,m,false, 1,7,1,8,2,11,2,12,3,12,7,-2,7,-1,8,-1,8,0,9,0,9,1,
                    -4,12,-3,12,-3,13,-2,10,-2,11,-1,13);
            return b;
        }
        throw new AssertionError();
    }

    private static boolean[] cross(boolean[] a, int n, int x, int y, int phase) {
        int m = n + 6;
        boolean[] b = new boolean[m * m];
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (a[r*n+c])
            b[(r + (r >= x ? 6 : 0))*m + c + (c >= y ? 6 : 0)] = true;
        for (int r = 0; r < m; r++) for (int c = 0; c < m; c++)
            if ((x <= r && r < x+6 || y <= c && c < y+6) && Math.floorMod(r-c-phase,3) != 0)
                b[r*m+c] = true;
        return b;
    }

    private static void corner(boolean[] a, int n) {
        switch (n % 6) {
        case 1:
            edit(a,n,true, -1,0,-1,2,-1,3,-1,6,-2,5, -1,-1,-1,-7,-2,-5,-3,-3,-4,-1,-7,-1, 2,-1,3,-1);
            edit(a,n,false,-1,4,-2,1,-2,4,-2,6,-2,7,-3,6, -2,-4,-2,-6,-3,-2,-3,-4,-4,-2,-6,-2, 4,-1,4,-2,5,-1);
            break;
        case 2:
            edit(a,n,true, -1,2,-1,3,-1,4,-2,2, -1,-1,-1,-7,-2,-5,-3,-3,-4,-1,-7,-1, 2,-1,2,-2,3,-1,4,-1);
            edit(a,n,false,-1,0,-1,1,-1,5,-1,6,-2,5, -2,-4,-2,-6,-3,-2,-3,-4,-4,-2,-6,-2, 0,-1,1,-1,5,-1,5,-2,6,-1);
            break;
        case 4:
            edit(a,n,true, -1,1, -1,-1,-1,-4,-1,-7,-3,-3,-5,-2,-7,-1, 1,-1);
            edit(a,n,false,-2,-3,-2,-4,-2,-6,-4,-2,-4,-3,-6,-2);
            break;
        case 5:
            edit(a,n,true, -1,1, -1,-1,-1,-7,-2,-5,-3,-3,-4,-1,-7,-1, 1,-1,4,-1);
            edit(a,n,false,-1,3, -2,-4,-2,-6,-3,-2,-3,-4,-4,-2,-6,-2, 3,-1,5,-2);
            break;
        default: throw new AssertionError();
        }
    }

    private static boolean[] base(String s) {
        String[] z = s.split("/"); int n = z.length; boolean[] a = new boolean[n*n];
        for (int r=0;r<n;r++) for (int c=0;c<n;c++) a[r*n+c] = z[r].charAt(c)=='X';
        return a;
    }

    // 負の座標は末尾から数える。-1 は最終行/列、-2 はその一つ手前。
    private static void edit(boolean[] a, int n, boolean v, int... p) {
        for (int i=0;i<p.length;i+=2) {
            int r=p[i]<0?n+p[i]:p[i], c=p[i+1]<0?n+p[i+1]:p[i+1];
            a[r*n+c]=v;
        }
    }

    private static int[] order(boolean[] a, int n) {
        int s=0,len=0; while (!a[s]) s++; for (boolean x:a) if(x) len++;
        int[] p=new int[len]; int prev=-1,u=s;
        for (int i=0;i<len;i++) {
            p[i]=u; int r=u/n,c=u%n,nx=-1;
            int[] d={-n,n,-1,1};
            for (int v:d) {
                int w=u+v;
                if (w<0||w>=n*n||v==-1&&c==0||v==1&&c+1==n||!a[w]||w==prev||w==s&&i<2) continue;
                nx=w; break;
            }
            if(nx<0) throw new AssertionError(); prev=u; u=nx;
        }
        if(u!=s) throw new AssertionError();
        return p;
    }
}
