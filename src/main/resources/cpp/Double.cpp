struct JDouble {
    static constexpr double MAX_VALUE = numeric_limits<double>::max();
    static constexpr double MIN_VALUE = numeric_limits<double>::min();
    static constexpr double NaN = numeric_limits<double>::quiet_NaN();
    static constexpr double POSITIVE_INFINITY = numeric_limits<double>::infinity();
    static constexpr double NEGATIVE_INFINITY = -numeric_limits<double>::infinity();
    static constexpr int BYTES = 8;
    static constexpr int SIZE = 64;
    static int compare(double a, double b) { return (a > b) - (a < b); }
    static int hashCode(double x) {
        uint64_t u;
        memcpy(&u, &x, sizeof(x));
        return static_cast<int>(u ^ (u >> 32));
    }
    static double sum(double a, double b) { return a + b; }
    static double max(double a, double b) { return std::max(a, b); }
    static double min(double a, double b) { return std::min(a, b); }
    // 未テスト
    // 計算量: O(1)
    static bool isNaN(double v) { return std::isnan(v); }
    // 未テスト
    // 計算量: O(1)
    static bool isInfinite(double v) { return std::isinf(v); }
    // 未テスト
    // 計算量: O(1)
    static bool isFinite(double v) { return std::isfinite(v); }
    // 未テスト
    // 計算量: O(1)
    static long long doubleToLongBits(double value) {
        if (std::isnan(value)) return 0x7ff8000000000000LL;
        uint64_t u;
        memcpy(&u, &value, sizeof(value));
        return static_cast<long long>(u);
    }
    // 未テスト
    // 計算量: O(1)
    static long long doubleToRawLongBits(double value) {
        uint64_t u;
        memcpy(&u, &value, sizeof(value));
        return static_cast<long long>(u);
    }
    // 未テスト
    // 計算量: O(1)
    static double longBitsToDouble(long long bits) {
        double d;
        uint64_t u = static_cast<uint64_t>(bits);
        memcpy(&d, &u, sizeof(d));
        return d;
    }
    // 未テスト
    // 計算量: O(1)
    static JString toHexString(double d) {
        char buf[64];
        snprintf(buf, sizeof(buf), "%a", d);
        return JString(buf);
    }
    static JString toString(double x) { return std::to_string(x); }
    static double parseDouble(const JString& x) { return stod(x); }
    static double valueOf(const JString& x) { return stod(x); }
    static double valueOf(double x) { return x; }
};
