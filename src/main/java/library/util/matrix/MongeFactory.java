package library.util.matrix;

import java.util.Comparator;

import library.util.geometry.LongPoint;
import library.util.seq.SortedArrays;

/**
 * Monge 行列および anti-Monge 行列を各種構造から構築するファクトリクラス。
 *
 * // 未テスト
 */
public final class MongeFactory {

    private MongeFactory() {}

    /**
     * 2 本の単調 chain が張る長方形に含まれる重み付き点の総和を
     * anti-Monge core として構築する。
     *
     * <p>行 i、列 j の値は
     *
     * <pre>
     * sum(weights[k] |
     *     upperChain[i].x() &lt;= points[k].x() &lt; lowerChain[j].x()
     *  &amp;&amp; lowerChain[j].y() &lt;= points[k].y() &lt; upperChain[i].y())
     * </pre>
     *
     * <p>両 chain は x, y ともに非減少で、
     *
     * <pre>
     * max upper.x &lt;= min lower.x
     * max lower.y &lt;= min upper.y
     * </pre>
     *
     * を要求する。
     *
     * @param upperChain 上側単調 chain
     * @param lowerChain 下側単調 chain
     * @param points 点の配列
     * @param weights 点の重み配列
     * @return 構築された AntiMongeCoreLong
     * @throws NullPointerException 入力または配列要素が null の場合
     * @throws IllegalArgumentException 配列長が一致しない場合、重みが負の場合、または chain の単調性・分離条件を満たさない場合
     * @complexity O(R + C + P(log R + log C))
     */
    // 未テスト
    public static AntiMongeCoreLong twoChainPointMeasureCore(
            LongPoint[] upperChain,
            LongPoint[] lowerChain,
            LongPoint[] points,
            long[] weights) {

        checkTwoChains(upperChain, lowerChain);

        if (points == null || weights == null) throw new NullPointerException();
        if (points.length != weights.length) {
            throw new IllegalArgumentException("length mismatch");
        }

        int R = upperChain.length;
        int C = lowerChain.length;
        AntiMongeCone cone = new AntiMongeCone(R, C);

        Comparator<LongPoint> cmpX = Comparator.comparingLong(LongPoint::x);
        Comparator<LongPoint> cmpY = Comparator.comparingLong(LongPoint::y);

        for (int k = 0; k < points.length; k++) {
            LongPoint p = points[k];
            if (p == null) throw new NullPointerException("points[" + k + "]");

            long w = weights[k];
            if (w < 0) {
                throw new IllegalArgumentException("weights[" + k + "]=" + w);
            }
            if (w == 0) continue;

            /*
             * 点 p が寄与する index 空間上の長方形
             *
             * [r0,r1) x [c0,c1)
             */
            int r0 = SortedArrays.floor(upperChain, new LongPoint(0, p.y()), cmpY) + 1;
            int r1 = SortedArrays.floor(upperChain, new LongPoint(p.x(), 0), cmpX) + 1;
            int c0 = SortedArrays.floor(lowerChain, new LongPoint(p.x(), 0), cmpX) + 1;
            int c1 = SortedArrays.floor(lowerChain, new LongPoint(0, p.y()), cmpY) + 1;

            if (r0 >= r1 || c0 >= c1) continue;

            /*
             * chain の分離条件から、この長方形は必ず
             *
             *   NW corner
             *   SE corner
             *   row strip
             *   column strip
             *
             * のいずれか。
             */
            if (r0 == 0 && c0 == 0) {
                cone.addNorthWest(r1, c1, w);

            } else if (r1 == R && c1 == C) {
                cone.addSouthEast(r0, c0, w);

            } else if (c0 == 0 && c1 == C) {
                for (int i = r0; i < r1; i++) {
                    cone.addRow(i, w);
                }

            } else if (r0 == 0 && r1 == R) {
                for (int j = c0; j < c1; j++) {
                    cone.addColumn(j, w);
                }

            } else {
                throw new AssertionError();
            }
        }

        return cone.buildCore();
    }

    private static void checkTwoChains(LongPoint[] upperChain, LongPoint[] lowerChain) {
        if (upperChain == null || lowerChain == null) throw new NullPointerException();

        int R = upperChain.length;
        int C = lowerChain.length;

        for (int i = 0; i < R; i++) {
            if (upperChain[i] == null) throw new NullPointerException("upperChain[" + i + "]");
            if (i > 0) {
                if (upperChain[i - 1].x() > upperChain[i].x() || upperChain[i - 1].y() > upperChain[i].y()) {
                    throw new IllegalArgumentException("upperChain is not non-decreasing in x and y");
                }
            }
        }

        for (int j = 0; j < C; j++) {
            if (lowerChain[j] == null) throw new NullPointerException("lowerChain[" + j + "]");
            if (j > 0) {
                if (lowerChain[j - 1].x() > lowerChain[j].x() || lowerChain[j - 1].y() > lowerChain[j].y()) {
                    throw new IllegalArgumentException("lowerChain is not non-decreasing in x and y");
                }
            }
        }

        if (R > 0 && C > 0) {
            if (upperChain[R - 1].x() > lowerChain[0].x()) {
                throw new IllegalArgumentException("max upper.x > min lower.x");
            }
            if (lowerChain[C - 1].y() > upperChain[0].y()) {
                throw new IllegalArgumentException("max lower.y > min upper.y");
            }
        }
    }
}
