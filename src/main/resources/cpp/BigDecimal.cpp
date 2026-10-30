struct JBigDecimal {
    double val = 0.0;
    JBigDecimal() = default;
    JBigDecimal(double v) : val(v) {}
    JBigDecimal(long long v) : val(static_cast<double>(v)) {}
    JBigDecimal(int v) : val(v) {}
    JBigDecimal(const JString& s) {
        try { val = stod(s); } catch(...) { val = 0.0; }
    }
    static JBigDecimal valueOf(double v) { return JBigDecimal(v); }
    static JBigDecimal valueOf(long long v) { return JBigDecimal(static_cast<double>(v)); }
    static constexpr int ROUND_UP = 0;
    static constexpr int ROUND_DOWN = 1;
    static constexpr int ROUND_CEILING = 2;
    static constexpr int ROUND_FLOOR = 3;
    static constexpr int ROUND_HALF_UP = 4;
    static constexpr int ROUND_HALF_DOWN = 5;
    static constexpr int ROUND_HALF_EVEN = 6;
    static constexpr int ROUND_UNNECESSARY = 7;

    static const JBigDecimal ZERO;
    static const JBigDecimal ONE;
    static const JBigDecimal TEN;

    JBigDecimal add(const JBigDecimal& o) const { return JBigDecimal(val + o.val); }
    JBigDecimal subtract(const JBigDecimal& o) const { return JBigDecimal(val - o.val); }
    JBigDecimal multiply(const JBigDecimal& o) const { return JBigDecimal(val * o.val); }
    JBigDecimal divide(const JBigDecimal& o) const { return JBigDecimal(val / (o.val == 0 ? 1 : o.val)); }
    JBigDecimal divide(const JBigDecimal& o, int, int = 0) const { return divide(o); }
    // 未テスト
    // 計算量: O(1)
    JBigDecimal remainder(const JBigDecimal& o) const { return JBigDecimal(fmod(val, o.val)); }
    // 未テスト
    // 計算量: O(1)
    JBigDecimal scaleByPowerOfTen(int n) const { return JBigDecimal(val * std::pow(10.0, n)); }
    // 未テスト
    // 計算量: O(1)
    JBigDecimal min(const JBigDecimal& o) const { return o.val < val ? o : *this; }
    // 未テスト
    // 計算量: O(1)
    JBigDecimal max(const JBigDecimal& o) const { return o.val > val ? o : *this; }
    JBigDecimal pow(int n) const { return JBigDecimal(std::pow(val, n)); }
    JBigDecimal abs() const { return JBigDecimal(std::abs(val)); }
    JBigDecimal negate() const { return JBigDecimal(-val); }
    JBigDecimal setScale(int, int = 0) const { return *this; }
    int compareTo(const JBigDecimal& o) const { return (val > o.val) - (val < o.val); }
    bool equals(const JBigDecimal& o) const { return val == o.val; }
    double doubleValue() const { return val; }
    long long longValue() const { return static_cast<long long>(val); }
    int intValue() const { return static_cast<int>(val); }
    JString toString() const { return std::to_string(val); }
    friend ostream& operator<<(ostream& os, const JBigDecimal& b) { return os << b.val; }
};
inline const JBigDecimal JBigDecimal::ZERO = JBigDecimal(0);
inline const JBigDecimal JBigDecimal::ONE = JBigDecimal(1);
inline const JBigDecimal JBigDecimal::TEN = JBigDecimal(10);
