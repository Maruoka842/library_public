template<class T> struct JOptional {
    bool has = false;
    T val = {};
    bool isPresent() const { return has; }
    // 未テスト
    // 計算量: O(1)
    bool isEmpty() const { return !has; }
    T get() const { return val; }
    long long getAsLong() const { return static_cast<long long>(val); }
    int getAsInt() const { return static_cast<int>(val); }
    double getAsDouble() const { return static_cast<double>(val); }
    T orElse(T def) const { return has ? val : def; }
    // 未テスト
    // 計算量: O(1)
    template<class F> T orElseGet(F supplier) const { return has ? val : supplier(); }
    // 未テスト
    // 計算量: O(1)
    T orElseThrow() const {
        if (!has) throw runtime_error("No value present");
        return val;
    }
    // 未テスト
    // 計算量: O(1)
    template<class F> void ifPresent(F action) const {
        if (has) action(val);
    }
    // 未テスト
    // 計算量: O(f)
    // map: JOptional<T> x (T -> U) -> JOptional<U>
    template<class F> auto map(F f) const -> JOptional<decltype(f(val))> {
        if (!has) return JOptional<decltype(f(val))>{false, {}};
        return JOptional<decltype(f(val))>{true, f(val)};
    }
    // 未テスト
    // 計算量: O(f)
    // flatMap: JOptional<T> x (T -> JOptional<U>) -> JOptional<U>
    template<class F> auto flatMap(F f) const -> decltype(f(val)) {
        if (!has) return decltype(f(val)){false, {}};
        return f(val);
    }
};

// 任意の JOptional<T> へ暗黙変換できる空値センチネル型。
// 計算量: O(1)
struct JOptionalEmpty {
    template<class T> operator JOptional<T>() const { return JOptional<T>{false, {}}; }
};

struct Optional {
    // 未テスト
    // 計算量: O(1)
    template<class T> static JOptional<T> of(T v) { return JOptional<T>{true, v}; }
    // 未テスト
    // 計算量: O(1)
    template<class T> static JOptional<T> ofNullable(T v) { return JOptional<T>{!jisNull(v), v}; }
    // 未テスト
    // 計算量: O(1)
    // empty() は任意の JOptional<T> へ暗黙変換できる JOptionalEmpty を返す。
    static JOptionalEmpty empty() { return JOptionalEmpty{}; }
};
