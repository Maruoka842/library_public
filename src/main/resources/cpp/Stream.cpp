template<class T> struct JStream {
    vector<T> values;
    explicit JStream(const vector<T>& source) : values(source) {}
    static JStream range(T startInclusive, T endExclusive) {
        vector<T> res;
        if (startInclusive < endExclusive) {
            res.reserve(endExclusive - startInclusive);
            for (T i = startInclusive; i < endExclusive; ++i) res.push_back(i);
        }
        return JStream(res);
    }
    static JStream rangeClosed(T startInclusive, T endInclusive) {
        vector<T> res;
        if (startInclusive <= endInclusive) {
            res.reserve(endInclusive - startInclusive + 1);
            for (T i = startInclusive; i <= endInclusive; ++i) res.push_back(i);
        }
        return JStream(res);
    }
    JStream& distinct() {
        sort(values.begin(), values.end());
        values.erase(unique(values.begin(), values.end()), values.end());
        return *this;
    }
    JStream& sorted() { sort(values.begin(), values.end()); return *this; }
    template<class F> JStream& filter(F predicate) {
        values.erase(remove_if(values.begin(), values.end(), [&](const T& x) { return !predicate(x); }), values.end());
        return *this;
    }
    template<class F> auto map(F mapper) const {
        using R = decltype(mapper(declval<T>()));
        vector<R> res;
        res.reserve(values.size());
        for (const auto& x : values) res.push_back(mapper(x));
        return JStream<R>(res);
    }
    template<class F> auto mapToLong(F mapper) const { return map(mapper); }
    template<class F> auto mapToInt(F mapper) const { return map(mapper); }
    template<class F> auto mapToDouble(F mapper) const { return map(mapper); }
    template<class F> auto mapToObj(F mapper) const { return map(mapper); }
    template<class F> void forEach(F action) const {
        for (const auto& x : values) action(x);
    }
    T sum() const {
        T res = 0;
        for (const auto& x : values) res += x;
        return res;
    }
    JOptional<T> max() const {
        if (values.empty()) return JOptional<T>{false, T{}};
        T res = values[0];
        for (const auto& x : values) if (x > res) res = x;
        return JOptional<T>{true, res};
    }
    template<class Comp> JOptional<T> max(Comp comp) const {
        if (values.empty()) return JOptional<T>{false, T{}};
        T res = values[0];
        for (const auto& x : values) if (comp(res, x) < 0) res = x;
        return JOptional<T>{true, res};
    }
    JOptional<T> min() const {
        if (values.empty()) return JOptional<T>{false, T{}};
        T res = values[0];
        for (const auto& x : values) if (x < res) res = x;
        return JOptional<T>{true, res};
    }
    template<class Comp> JOptional<T> min(Comp comp) const {
        if (values.empty()) return JOptional<T>{false, T{}};
        T res = values[0];
        for (const auto& x : values) if (comp(res, x) > 0) res = x;
        return JOptional<T>{true, res};
    }
    template<class F> JOptional<T> reduce(F accumulator) const {
        if (values.empty()) return JOptional<T>{false, T{}};
        T res = values[0];
        for (size_t i = 1; i < values.size(); ++i) res = accumulator(res, values[i]);
        return JOptional<T>{true, res};
    }
    template<class U, class F> U reduce(U identity, F accumulator) const {
        U res = identity;
        for (const auto& x : values) res = accumulator(res, x);
        return res;
    }
    template<class U, class F1, class F2> U reduce(U identity, F1 accumulator, F2 combiner) const {
        U res = identity;
        for (const auto& x : values) res = accumulator(res, x);
        return res;
    }
    template<class F> bool allMatch(F predicate) const {
        for (const auto& x : values) if (!predicate(x)) return false;
        return true;
    }
    template<class F> bool anyMatch(F predicate) const {
        for (const auto& x : values) if (predicate(x)) return true;
        return false;
    }
    template<class F> bool noneMatch(F predicate) const {
        for (const auto& x : values) if (predicate(x)) return false;
        return true;
    }
    long long count() const { return (long long)values.size(); }
    JStream<T> boxed() const { return *this; }
    vector<T> toArray() const { return values; }
    template<class Generator> auto toArray(Generator&& g) const {
        auto res = g((int)values.size());
        for (size_t i = 0; i < values.size(); ++i) res[i] = values[i];
        return res;
    }
};
using JIntStream = JStream<int>;
using JLongStream = JStream<long long>;
