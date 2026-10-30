inline long long jcurrentTimeMillis() {
    return chrono::duration_cast<chrono::milliseconds>(chrono::system_clock::now().time_since_epoch()).count();
}
// 未テスト
// 計算量: O(1)
inline long long jnanoTime() {
    return chrono::duration_cast<chrono::nanoseconds>(chrono::steady_clock::now().time_since_epoch()).count();
}
struct JDate {
    long long time_ms = 0;
    JDate() : time_ms(jcurrentTimeMillis()) {}
    explicit JDate(long long ms) : time_ms(ms) {}
    long long getTime() const { return time_ms; }
    // 未テスト
    // 計算量: O(1)
    void setTime(long long ms) { time_ms = ms; }
    // 未テスト
    // 計算量: O(1)
    bool before(const JDate& when) const { return time_ms < when.time_ms; }
    // 未テスト
    // 計算量: O(1)
    bool after(const JDate& when) const { return time_ms > when.time_ms; }
    // 未テスト
    // 計算量: O(1)
    bool equals(const JDate& o) const { return time_ms == o.time_ms; }
    // 未テスト
    // 計算量: O(1)
    int compareTo(const JDate& o) const { return (time_ms > o.time_ms) - (time_ms < o.time_ms); }
};
