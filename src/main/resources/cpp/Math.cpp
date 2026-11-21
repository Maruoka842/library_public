struct JMath {
    static constexpr double PI = 3.14159265358979323846;
    static constexpr double E = 2.71828182845904523536;
    // 未テスト
    // 計算量: O(1)
    template<class A, class B> static auto min(const A& a, const B& b) { return jmin(a, b); }
    // 未テスト
    // 計算量: O(1)
    template<class A, class B> static auto max(const A& a, const B& b) { return jmax(a, b); }
    // 未テスト
    // 計算量: O(1)
    template<class T> static auto abs(T x) { return std::abs(x); }
    // 未テスト
    // 計算量: O(1)
    static double toRadians(double angdeg) { return jtoRadians(angdeg); }
    // 未テスト
    // 計算量: O(1)
    static double toDegrees(double angrad) { return jtoDegrees(angrad); }
    // 未テスト
    // 計算量: O(1)
    static long long multiplyFull(int x, int y) { return jmultiplyFull(x, y); }
    // 未テスト
    // 計算量: O(1)
    static long long multiplyHigh(long long x, long long y) { return jmultiplyHigh(x, y); }
};
