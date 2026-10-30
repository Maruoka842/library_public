struct JRandom {
    mutable mt19937_64 engine;
    JRandom() : engine(chrono::steady_clock::now().time_since_epoch().count()) {}
    explicit JRandom(long long seed) : engine(seed) {}
    // 未テスト
    // 計算量: O(1)
    void setSeed(long long seed) const { engine.seed(seed); }
    int nextInt() const { return static_cast<int>(engine()); }
    int nextInt(int bound) const { return uniform_int_distribution<int>(0, bound - 1)(engine); }
    int nextInt(int origin, int bound) const { return uniform_int_distribution<int>(origin, bound - 1)(engine); }
    long long nextLong() const { return static_cast<long long>(engine()); }
    long long nextLong(long long bound) const { return uniform_int_distribution<long long>(0, bound - 1)(engine); }
    long long nextLong(long long origin, long long bound) const { return uniform_int_distribution<long long>(origin, bound - 1)(engine); }
    double nextDouble() const { return uniform_real_distribution<double>(0.0, 1.0)(engine); }
    double nextDouble(double bound) const { return uniform_real_distribution<double>(0.0, bound)(engine); }
    double nextDouble(double origin, double bound) const { return uniform_real_distribution<double>(origin, bound)(engine); }
    float nextFloat() const { return uniform_real_distribution<float>(0.0f, 1.0f)(engine); }
    bool nextBoolean() const { return (engine() & 1) != 0; }
    double nextGaussian() const { return normal_distribution<double>(0.0, 1.0)(engine); }
};
