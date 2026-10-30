struct JRuntime {
    long long totalMemory() const { return numeric_limits<long long>::max(); }
    long long maxMemory() const { return numeric_limits<long long>::max(); }
    long long freeMemory() const { return numeric_limits<long long>::max(); }
    // 未テスト
    // 計算量: O(1)
    static JRuntime getRuntime() { return JRuntime{}; }
    // 未テスト
    // 計算量: O(1)
    int availableProcessors() const { return 1; }
};
