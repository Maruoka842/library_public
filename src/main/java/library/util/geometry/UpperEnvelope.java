package library.util.geometry;

import java.util.ArrayList;
import java.util.Arrays;

import library.util.FractionUtils;
import library.util.LongFraction;
import library.util.collections.ObjectDeque;


public class UpperEnvelope {
	ObjectDeque<LongLine> lines;
	
	boolean built=true;
	
	public UpperEnvelope() {
		lines=new ObjectDeque<LongLine>();
	}
	
	public void registerLine(LongLine line) {
		if(line.b==0) {
			throw new AssertionError();
		}
		built=false;
		lines.addLast(line);
	}
	
	public void addLast(LongLine line) {
		//linesは直線が傾きの昇順に並んでいる
		
		while(!lines.isEmpty()) {//同じ傾きの直線を処理
			if (line.a * lines.peekLast().b == line.b * lines.peekLast().a) {
				//ax+by+c=0についてx=0とするとy=-c/b
				if (line.yInterceptAsFraction().compareTo(lines.peekLast().yInterceptAsFraction()) <= 0) return;
				lines.pollLast();
			} else {
				break;
			}
		}
		
		
		while(lines.size() >= 2) {//同じ傾きの直線を処理
			var s=lines.get(lines.size()-2);
			var t=lines.get(lines.size()-1);
			if(s.intersection(line).x().compareTo(t.intersection(line).x()) >= 0) {
				lines.pollLast();
			} else {
				break;
			}
		}
		lines.addLast(line);
		
	}

	/**
	 * 上包絡線 y = max_i(a_i x + b_i) と ax+by+c=0 の交点を返す
	 * 限られた条件についてのみしか動かない。
	 * @param y
	 * @return
	 */
	public FractionPoint intersectionWithLine(LongLine l) {
		//https://atcoder.jp/contests/abc341/submissions/72405436
	    if (lines.isEmpty()) throw new AssertionError();
	    if (!built) build();
	    if (l.a * l.b < 0) throw new AssertionError("未実装");//傾き非負の場合以外は未実装
	    if(lines.peekFirst().a*lines.peekFirst().b>0) {//傾き負から始まり、傾き負と交差する場合以外は未実装。
	    	int ok=0;
	    	int ng=lines.size();
	    	while(ng-ok!=1) {
	    		int mid=(ok+ng)/2;
	    		if(lines.get(mid).a*lines.get(mid).b <= 0) {//傾き非負は無視
	    			ng=mid;
	    			continue;
	    		}
	    		var L=lines.get(mid);
	    		if(L.intersection(lines.get(mid-1)).x().compareTo(L.intersection(l).x()) <= 0) {// ax+by+c=0⇔x=(-by-c)/a
	    			ok=mid;
	    		} else {
	    			ng=mid;
	    		}
	    	}
	    	return lines.get(ok).intersection(l);
	    } else {
	    	throw new AssertionError("未実装");
	    }
	}
	
	//https://atcoder.jp/contests/abc289/submissions/72484278
	public LongFraction maxYAt(long x) {
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
	
	
	public LongFraction maxYAt(LongFraction x) {
		if (lines.isEmpty()) throw new AssertionError();
		if(!built)build();
		int ng=lines.size();
		int ok=0;
		while(Math.abs(ok-ng)!=1) {
			int m=(ok+ng)/2;
			if(lines.get(m-1).intersection(lines.get(m)).x.compareTo(x) <= 0) ok=m; 
			else ng=m;
		}
		// ax+by+c=0
		// y=-c/b-(ax)/b
		var line=lines.get(ok);
		return new LongFraction(-line.c, line.b).add(new LongFraction(-line.a, line.b).mul(x));
	}
	
	
	void build() {
		built=true;
		if(lines.isEmpty()) return;
		// ax+by+c=0
		// y = -ax/b-c/b
		// -A[i]/B[i]の昇順に並び変える
		LongLine[] array=lines.toArray(LongLine[]::new);
		lines.clear();
		Arrays.sort(array, (u, v)-> {
			if (u.a*v.b==v.a*u.b) {
				//傾きが同じときy切片-c/bの昇順
				return FractionUtils.compareFraction(-u.c, u.b, -v.c, v.b);
			} else {
				return FractionUtils.compareFraction(-u.a, u.b, -v.a, v.b);
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
	
	
	/**
	 * 上包絡線の各直線情報を文字列として表す。
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

	/**
	 * 上包絡線の直線情報を標準出力に出力する。
	 * <ul>
	 *   <li>計算量: O(N)</li>
	 * </ul>
	 */
	// 未テスト
	public void dump() {
		System.out.println(toString());
	}
	
	static void tr(Object...objects) {System.out.println(Arrays.deepToString(objects));}

	/**
	 * この上側包絡線と別のオブジェクトの同値性を判定します。
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
		if (!(obj instanceof UpperEnvelope)) return false;
		UpperEnvelope other = (UpperEnvelope) obj;
		if (!this.built) this.build();
		if (!other.built) other.build();
		return this.lines.equals(other.lines);
	}

	/**
	 * この上側包絡線のハッシュコードを計算します。
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
	 * 現在管理されている直線群と上側包絡線を可視化するための XYChart オブジェクトを構築します。
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
			minY = minY - yDiff * 0.4 - 1.0;
			maxY = maxY + yDiff * 0.2 + 1.0;
		}

		org.knowm.xchart.XYChart chart = new org.knowm.xchart.XYChartBuilder()
				.width(800)
				.height(600)
				.title("UpperEnvelope-" + drawCount.incrementAndGet())
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

		org.knowm.xchart.XYSeries envSeries = chart.addSeries("Upper Envelope", envX, envY);
		envSeries.setMarker(org.knowm.xchart.style.markers.SeriesMarkers.CIRCLE);
		envSeries.setMarkerColor(java.awt.Color.BLUE);
		envSeries.setLineStyle(org.knowm.xchart.style.lines.SeriesLines.SOLID);
		envSeries.setLineColor(java.awt.Color.BLUE);

		return chart;
	}

	/**
	 * 管理されている直線群と上側包絡線をグラフ描画して表示します。
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
