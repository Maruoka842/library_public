template<class T> void jsort(vector<T>& a) { sort(a.begin(), a.end()); }
template<class T, class C> void jsort(vector<T>& a, C compare) {
    sort(a.begin(), a.end(), [&](const T& x, const T& y) { return compare(x, y) < 0; });
}
template<class T, class U> void jfill(vector<T>& a, const U& x) { fill(a.begin(), a.end(), static_cast<T>(x)); }
template<class T> vector<T> jcopyOf(const vector<T>& a, int n) {
    vector<T> result(n);
    copy_n(a.begin(), min<int>(a.size(), n), result.begin());
    return result;
}
template<class T> vector<T> jcopyOfRange(const vector<T>& a, int from, int to) {
    vector<T> result(max(0, to - from));
    int count = max(0, min<int>(a.size(), to) - from);
    if (count > 0) copy_n(a.begin() + from, count, result.begin());
    return result;
}
template<class T> void jarraycopy(const vector<T>& src, int srcPos, vector<T>& dst, int dstPos, int length) {
    copy_n(src.begin() + srcPos, length, dst.begin() + dstPos);
}
template<class T, class F> void jsetAll(vector<T>& a, F f) {
    for (int i = 0; i < (int)a.size(); ++i) a[i] = f(i);
}
// 未テスト
// 計算量: O(N)
template<class T, class F> void jparallelSetAll(vector<T>& a, F f) { jsetAll(a, f); }

// 未テスト
// 計算量: O(min(N, M))
template<class C1, class C2> int jcompare(const C1& a, const C2& b) {
    auto it1 = a.begin(), end1 = a.end();
    auto it2 = b.begin(), end2 = b.end();
    for (; it1 != end1 && it2 != end2; ++it1, ++it2) {
        if (*it1 < *it2) return -1;
        if (*it2 < *it1) return 1;
    }
    if (it1 != end1) return 1;
    if (it2 != end2) return -1;
    return 0;
}
// 未テスト
// 計算量: O(min(aTo - aFrom, bTo - bFrom))
template<class T> int jcompare(const vector<T>& a, int aFrom, int aTo, const vector<T>& b, int bFrom, int bTo) {
    auto it1 = a.begin() + aFrom, end1 = a.begin() + aTo;
    auto it2 = b.begin() + bFrom, end2 = b.begin() + bTo;
    for (; it1 != end1 && it2 != end2; ++it1, ++it2) {
        if (*it1 < *it2) return -1;
        if (*it2 < *it1) return 1;
    }
    if (it1 != end1) return 1;
    if (it2 != end2) return -1;
    return 0;
}
// 未テスト
// 計算量: O(min(N, M))
template<class C1, class C2, class Comp> int jcompare(const C1& a, const C2& b, Comp comp) {
    auto it1 = a.begin(), end1 = a.end();
    auto it2 = b.begin(), end2 = b.end();
    for (; it1 != end1 && it2 != end2; ++it1, ++it2) {
        if (comp(*it1, *it2)) return -1;
        if (comp(*it2, *it1)) return 1;
    }
    if (it1 != end1) return 1;
    if (it2 != end2) return -1;
    return 0;
}
// 未テスト
// 計算量: O(min(aTo - aFrom, bTo - bFrom))
template<class T, class Comp> int jcompare(const vector<T>& a, int aFrom, int aTo, const vector<T>& b, int bFrom, int bTo, Comp comp) {
    auto it1 = a.begin() + aFrom, end1 = a.begin() + aTo;
    auto it2 = b.begin() + bFrom, end2 = b.begin() + bTo;
    for (; it1 != end1 && it2 != end2; ++it1, ++it2) {
        if (comp(*it1, *it2)) return -1;
        if (comp(*it2, *it1)) return 1;
    }
    if (it1 != end1) return 1;
    if (it2 != end2) return -1;
    return 0;
}

template<class T, class F> void jparallelPrefix(vector<T>& a, F op) {
    for (size_t i = 1; i < a.size(); ++i) a[i] = op(a[i - 1], a[i]);
}
template<class T, class F> void jparallelPrefix(vector<T>& a, int from, int to, F op) {
    for (int i = from + 1; i < to; ++i) a[i] = op(a[i - 1], a[i]);
}
template<class C, class T> int jbsearch(const C& a, const T& x) {
    auto it = lower_bound(a.begin(), a.end(), x);
    return it != a.end() && *it == x ? int(it - a.begin()) : -int(it - a.begin()) - 1;
}
template<class C, class T, class Comp> int jbsearch(const C& a, const T& x, Comp comp) {
    auto it = lower_bound(a.begin(), a.end(), x, comp);
    return it != a.end() && !comp(x, *it) && !comp(*it, x) ? int(it - a.begin()) : -int(it - a.begin()) - 1;
}
template<class C1, class C2> int jmismatch(const C1& a, const C2& b) {
    auto [it1, it2] = std::mismatch(a.begin(), a.end(), b.begin(), b.end());
    if (it1 == a.end() && it2 == b.end()) return -1;
    return int(distance(a.begin(), it1));
}
