template<class T> auto jsignum(T x) {
    if constexpr (std::is_floating_point_v<T>) {
        if (std::isnan(x)) return x;
        return x > T(0) ? T(1) : (x < T(0) ? T(-1) : T(0));
    } else {
        return (x > T(0)) - (x < T(0));
    }
}
template<class A, class B> auto jmin(const A& a, const B& b) {
    using C = common_type_t<A, B>;
    if constexpr (std::is_floating_point_v<C>) {
        if (std::isnan(a) || std::isnan(b)) return std::numeric_limits<C>::quiet_NaN();
    }
    return std::min<C>(a, b);
}
template<class A, class B> auto jmax(const A& a, const B& b) {
    using C = common_type_t<A, B>;
    if constexpr (std::is_floating_point_v<C>) {
        if (std::isnan(a) || std::isnan(b)) return std::numeric_limits<C>::quiet_NaN();
    }
    return std::max<C>(a, b);
}
// 未テスト
// 計算量: O(1)
template<class T> auto jround(T x) {
    if constexpr (std::is_same_v<decay_t<T>, float>) {
        return static_cast<int>(std::floor(x + 0.5f));
    } else {
        return static_cast<long long>(std::floor(x + 0.5));
    }
}

template<class A, class B> auto jfloorDiv(A a, B b) {
    using C = common_type_t<A, B>;
    C x = a, y = b;
    C res = x / y;
    C rem = x % y;
    if ((rem != 0) && ((x ^ y) < 0)) --res;
    return res;
}
template<class A, class B> auto jceilDiv(A a, B b) {
    using C = common_type_t<A, B>;
    C x = a, y = b;
    C res = x / y;
    C rem = x % y;
    if ((rem != 0) && ((x ^ y) > 0)) ++res;
    return res;
}
template<class A, class B> auto jfloorMod(A a, B b) {
    using C = common_type_t<A, B>;
    C x = a, y = b;
    C rem = x % y;
    if ((rem != 0) && ((x ^ y) < 0)) rem += y;
    return rem;
}
// 未テスト
// 計算量: O(1)
template<class T, class U>
auto jusr(T a, U b) {
    using UT = std::make_unsigned_t<T>;
    return static_cast<T>(static_cast<UT>(a) >> b);
}

template<class V, class L, class H> auto jclamp(V v, L l, H h) {
    return v < l ? l : (v > h ? h : v);
}
template<class A, class B> auto jaddExact(A a, B b) { return a + b; }
template<class A, class B> auto jsubtractExact(A a, B b) { return a - b; }
template<class A, class B> auto jmultiplyExact(A a, B b) { return a * b; }
template<class T> auto jtoIntExact(T x) { return static_cast<int>(x); }
template<class T> auto jnegateExact(T x) { return -x; }
// 未テスト
// 計算量: O(1)
template<class T> auto jabsExact(T x) { return x < 0 ? -x : x; }
// 未テスト
// 計算量: O(1)
template<class T> auto jincrementExact(T a) { return a + 1; }
// 未テスト
// 計算量: O(1)
template<class T> auto jdecrementExact(T a) { return a - 1; }
// 未テスト
// 計算量: O(1)
inline long long jmultiplyFull(int x, int y) { return static_cast<long long>(x) * y; }
// 未テスト
// 計算量: O(1)
inline long long jmultiplyHigh(long long x, long long y) {
    return static_cast<long long>((static_cast<__int128>(x) * static_cast<__int128>(y)) >> 64);
}
// 未テスト
// 計算量: O(1)
inline double jtoRadians(double angdeg) { return angdeg * 3.14159265358979323846 / 180.0; }
// 未テスト
// 計算量: O(1)
inline double jtoDegrees(double angrad) { return angrad * 180.0 / 3.14159265358979323846; }

template<class A, class B> int jcompareValue(const A& a, const B& b) {
    return (a > b) - (a < b);
}
template<class T> int jcompare(const vector<T>& a, const vector<T>& b) {
    if (lexicographical_compare(a.begin(), a.end(), b.begin(), b.end())) return -1;
    if (lexicographical_compare(b.begin(), b.end(), a.begin(), a.end())) return 1;
    return 0;
}
