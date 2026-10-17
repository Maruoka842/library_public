package library.util.segtree;

public class WindowEvaluationState<E, R> {
	public E[] prefix;
	public E[] suffix;
	public int len;
	public R agg;

	public WindowEvaluationState(E[] prefix, E[] suffix, int len, R agg) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.len = len;
		this.agg = agg;
	}
}
