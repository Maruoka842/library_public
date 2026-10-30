struct JMath {
    static constexpr double PI = 3.14159265358979323846;
    static constexpr double E = 2.71828182845904523536;
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
