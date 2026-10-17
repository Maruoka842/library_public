package library.util.geometry;

import java.util.ArrayList;
import java.util.Arrays;

import library.util.FractionUtils;
import library.util.LongFraction;
import library.util.collections.ObjectDeque;


public class LineLowerEnvelope {
	public ObjectDeque<LongLine> lines;
	boolean built=false;
	
	public LineLowerEnvelope() {
		lines=new ObjectDeque<LongLine>();
	}
	
	/**
	 * 直線の数
	 * @return
	 */
	public int size() {
		return lines.size();
	}
	
	public LongLine pollFirst() {
		return lines.pollFirst();
	}
	
	public LongLine pollLast() {
		return lines.pollLast();
	}
	
	public LongLine getLine(int i) {
		return lines.get(i);
	}
	
	/**
	 * max ax+by
	 * @param a
	 * @param b
	 * @return
	 */
	public double maximizeAxPlusByAsDouble(long a, long b) {
		//https://atcoder.jp/contests/abc356/submissions/72484853
		if(!built)throw new AssertionError();
		//ax+by=k
		//y=-ax/b+k/b
		int ok = -1;
        int ng = lines.size();
        LongFraction p = new LongFraction(-a, b);
        while (Math.abs(ok - ng) != 1) {
            int mid = (ok + ng) / 2;
            // ax+by+c=0
            // y=-(a/b)x-(c/b)
            if (new LongFraction(-lines.get(mid).a, lines.get(mid).b).compareTo(p) >= 0) {
                ok = mid;
            } else {
                ng = mid;
            }
        } 
        if (ok == -1) {
        	return Double.POSITIVE_INFINITY;
        } else if (ok == lines.size()- 1) {
            if (new LongFraction(-lines.get(ok).a, lines.get(ok).b).fastCompareTo(p) == 0) {
                double y = lines.get(ok).yIntercept();
                return y*b;
            } else {
            	return Double.POSITIVE_INFINITY;
            }
        } else {
            var xy = lines.get(ok).intersection(lines.get(ok+1));
            return a*xy.x.toDouble()+b*xy.y().toDouble();
        }

	}
	/**
	 * a x + b y + c <= 0 を追加
	 * @param line
	 */
	public void registerLine(LongLine line) {
		//https://atcoder.jp/contests/abc356/submissions/72484087
		if(line.b==0) {
			throw new AssertionError();
		}
		built=false;
		lines.addLast(line);
	}
	
	
	
	public LongFraction minYAt(long x) {
		if (lines.isEmpty()) throw new AssertionError();
		if(!built)build();
		int ng=lines.size();
		int ok=0;
		while(Math.abs(ok-ng)!=1) {
			int m=(ok+ng)/2;
			if(lines.get(m-1).intersection(lines.get(m)).x.compareTo(new LongFraction(x, 1)) <= 0) ok=m; 
			else ng=m;
		}
		// ax+by+c=0
		// y=-c/b-(ax)/b
		var line=lines.get(ok);
		return new LongFraction(-line.c, line.b).add(new LongFraction(-line.a*x, line.b));
	}
	
	//https://atcoder.jp/contests/abc356/submissions/72484087
	public void build() {
		built=true;
		if(lines.isEmpty()) return;
		// ax+by+c=0
		// y = -ax/b-c/b
		// -A[i]/B[i]の降順に並び変える
		LongLine[] array=lines.toArray(LongLine[]::new);
		lines.clear();
		Arrays.sort(array, (u, v)-> {
			if (u.a*v.b==v.a*u.b) {
                // 傾きが同じときy切片-c/bの降順
				return -FractionUtils.compareFraction(-u.c, u.b, -v.c, v.b);//new Fractionすると3倍掛かる
            } else {
				return -FractionUtils.compareFraction(-u.a, u.b, -v.a, v.b);//new Fractionすると3倍掛かる
            }
		});
		ArrayList<LongLine>list=new ArrayList<>();
		list.add(array[0]);
		for (int i = 1; i < array.length; i++) {
			while(list.size()>=1) {
				LongLine t=list.getLast();
				if(array[i].a*t.b==t.a*array[i].b) {
					list.removeLast();
				} else {
					break;
				}
			}
			while(list.size()>=2) {
				LongLine t=list.getLast();
				LongLine s=list.get(list.size()-2);
				if (t.intersection(s).x.compareTo(s.intersection(array[i]).x) >= 0) {
					list.removeLast();
				} else {
					break;
				}
			}
			list.add(array[i]);
		}
		for (int i = 0; i < list.size(); i++) {
			lines.addLast(list.get(i));
		}
	}
	
	//https://atcoder.jp/contests/abc356/submissions/72484087
	public LongLine[] toArray() {
		if(!built) build();
		return lines.toArray(LongLine[]::new);
	}
	
	/**
	 * 下側包絡線の各直線情報を文字列として表す。
	 * <ul>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 * @return 直線情報の文字列表現
	 */
	// 未テスト
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (var line : lines) {
			if (!first) sb.append("\n");
			sb.append(Arrays.deepToString(new Object[]{line}));
			first = false;
		}
		return sb.toString();
	}

	//https://atcoder.jp/contests/abc356/submissions/72484087
	public void dump() {
		System.out.println(toString());
	}
	
	static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}

	/**
	 * この下側包絡線と別のオブジェクトの同値性を判定します。
	 *
	 * <p>計算量: O(N log N)、ここで N は登録された直線の数です。</p>
	 *
	 * @param obj 比較対象のオブジェクト
	 * @return 同値であれば true, そうでなければ false
	 */
	// 未テスト
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof LineLowerEnvelope)) return false;
		LineLowerEnvelope other = (LineLowerEnvelope) obj;
		if (!this.built) this.build();
		if (!other.built) other.build();
		return this.lines.equals(other.lines);
	}

	/**
	 * この下側包絡線のハッシュコードを計算します。
	 *
	 * <p>計算量: O(N log N)、ここで N は登録された直線の数です。</p>
	 *
	 * @return ハッシュコード
	 */
	// 未テスト
	@Override
	public int hashCode() {
		if (!this.built) this.build();
		return java.util.Objects.hash(lines);
	}

	/**
	 * 描画回数を追跡するためのアトミックカウンター。
	 */
	private static final java.util.concurrent.atomic.AtomicInteger drawCount = new java.util.concurrent.atomic.AtomicInteger(0);

	/**
	 * 現在管理されている直線群と下側包絡線を可視化するための XYChart オブジェクトを構築します。
	 *
	 * <p>計算量: O(N)（構築されていない場合は O(N log N)）</p>
	 *
	 * @return 描画用の XYChart オブジェクト
	 * @throws java.util.NoSuchElementException 直線が一つも登録されていない場合
	 */
	// 未テスト
	public org.knowm.xchart.XYChart toChart() {
		if (!built) build();
		if (lines.isEmpty()) {
			throw new java.util.NoSuchElementException("No lines to draw.");
		}

		int N = lines.size();
		double[] intersections = new double[N - 1];
		for (int i = 0; i < N - 1; i++) {
			LongLine line1 = lines.get(i);
			LongLine line2 = lines.get(i + 1);
			intersections[i] = line1.xOfIntersectionAsDouble(line2);
		}

		double minX, maxX;
		if (N == 1) {
			minX = -10.0;
			maxX = 10.0;
		} else {
			double low = intersections[0];
			double high = intersections[N - 2];
			if (low == high) {
				minX = low - 10.0;
				maxX = low + 10.0;
			} else {
				double diff = high - low;
				minX = low - diff * 0.2 - 2.0;
				maxX = high + diff * 0.2 + 2.0;
			}
		}

		java.util.List<Double> envX = new java.util.ArrayList<>();
		java.util.List<Double> envY = new java.util.ArrayList<>();

		// 最初の点
		LongLine firstLine = lines.get(0);
		envX.add(minX);
		envY.add((-firstLine.a * minX - firstLine.c) / firstLine.b);

		// 交点
		for (int i = 0; i < N - 1; i++) {
			double x = intersections[i];
			LongLine line = lines.get(i);
			envX.add(x);
			envY.add((-line.a * x - line.c) / line.b);
		}

		// 最後の点
		LongLine lastLine = lines.get(N - 1);
		envX.add(maxX);
		envY.add((-lastLine.a * maxX - lastLine.c) / lastLine.b);

		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (double y : envY) {
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		if (minY == maxY) {
			minY -= 10.0;
			maxY += 10.0;
		} else {
			double yDiff = maxY - minY;
			minY = minY - yDiff * 0.2 - 1.0;
			maxY = maxY + yDiff * 0.4 + 1.0;
		}

		org.knowm.xchart.XYChart chart = new org.knowm.xchart.XYChartBuilder()
				.width(800)
				.height(600)
				.title("LineLowerEnvelope-" + drawCount.incrementAndGet())
				.xAxisTitle("x")
				.yAxisTitle("y")
				.build();

		chart.getStyler().setXAxisMin(minX);
		chart.getStyler().setXAxisMax(maxX);
		chart.getStyler().setYAxisMin(minY);
		chart.getStyler().setYAxisMax(maxY);

		for (int i = 0; i < N; i++) {
			LongLine line = lines.get(i);
			java.util.List<Double> lx = java.util.List.of(minX, maxX);
			java.util.List<Double> ly = java.util.List.of(
					(-line.a * minX - line.c) / (double) line.b,
					(-line.a * maxX - line.c) / (double) line.b
			);
			org.knowm.xchart.XYSeries series = chart.addSeries("Line " + i + ": " + line, lx, ly);
			series.setMarker(org.knowm.xchart.style.markers.SeriesMarkers.NONE);
			series.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.DASH_DASH);
			series.setLineColor(java.awt.Color.LIGHT_GRAY);
		}

		org.knowm.xchart.XYSeries envSeries = chart.addSeries("Lower Envelope", envX, envY);
		envSeries.setMarker(org.knowm.xchart.style.markers.SeriesMarkers.CIRCLE);
		envSeries.setMarkerColor(java.awt.Color.RED);
		envSeries.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.SOLID);
		envSeries.setLineColor(java.awt.Color.RED);

		return chart;
	}

	/**
	 * 管理されている直線群と下側包絡線をグラフ描画して表示します。
	 *
	 * <p>計算量: O(N)（構築されていない場合は O(N log N)）</p>
	 *
	 * @throws java.util.NoSuchElementException 直線が一つも登録されていない場合
	 */
	// 未テスト
	public void draw() {
		new org.knowm.xchart.SwingWrapper<>(toChart()).displayChart();
	}
}
