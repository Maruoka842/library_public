package library.util.mo;

/**
 * 
 * 多項式fと整数列Aが与える。
 * A[l:r)に対して、sum_i f(#{i : i　in A[l:r)}) を求める。 
 * verified:https://atcoder.jp/contests/abc293/submissions/70357331
 */
class PolyEvalAtFrequency extends MoCursor<Long> {
	int[] A;
	long[] cnt;
	long ret = 0;

	public PolyEvalAtFrequency(int[] A) {
		this.A = A;
		int max = 0;
		for (int x : A) {
			if (x > max) max = x;
		}
		cnt = new long[max + 1];
	}

	long f(long x) {
		return x * (x - 1) * (x - 2) / 6;
	}

	@Override
	public void addLeft(int i) {
		ret -= f(cnt[A[i]]);
		cnt[A[i]]++;
		ret += f(cnt[A[i]]);
	}

	@Override
	public void addRight(int i) {
		ret -= f(cnt[A[i]]);
		cnt[A[i]]++;
		ret += f(cnt[A[i]]);
	}

	@Override
	public void popLeft(int i) {
		ret -= f(cnt[A[i]]);
		cnt[A[i]]--;
		ret += f(cnt[A[i]]);
	}

	@Override
	public void popRight(int i) {
		ret -= f(cnt[A[i]]);
		cnt[A[i]]--;
		ret += f(cnt[A[i]]);
	}

	@Override
	public Long getValue(int queryId) {
		return ret;
	}
}
