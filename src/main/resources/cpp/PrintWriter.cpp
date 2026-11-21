struct JInputStream;
// 未テスト
// 計算量: O(1)
struct JPrintWriter {
    bool is_err = false;
    JPrintWriter() = default;
    explicit JPrintWriter(bool err) : is_err(err) {}
    template<class StreamType, class = enable_if_t<is_same_v<decay_t<StreamType>, JInputStream>>> explicit JPrintWriter(const StreamType&) {}
    explicit JPrintWriter(nullptr_t) {}
    template<class T> void print(const T& x) { if (is_err) jeprint(x); else jprint(x); }
    template<class T> void println(const T& x) { if (is_err) jeprintln(x); else jprintln(x); }
    template<class T> void print(const vector<T>& a) { print(a, " "); }
    template<class T> void println(const vector<T>& a) { println(a, " "); }
    void print(const vector<char>& a) { for (char c : a) print(c); }
    void println(const vector<char>& a) { for (char c : a) print(c); println(); }
    template<class T> void print(const vector<T>& a, const JString& separator) {
        for (size_t i = 0; i < a.size(); ++i) { if (i) print(separator); print(a[i]); }
    }
    template<class T> void println(const vector<T>& a, const JString& separator) {
        for (size_t i = 0; i < a.size(); ++i) { if (i) print(separator); print(a[i]); }
        println();
    }
    void println() { if (is_err) jeprintln(); else jprintln(); }
    void printlnYesNo(bool x) { println(x ? "Yes" : "No"); }
    template<class... Args> void printf(Args&&... args) { if (is_err) jeprintf(forward<Args>(args)...); else jprintf(forward<Args>(args)...); }
    template<class T> void write(const T& x) { print(x); }
    void write(int c) { if (is_err) cerr.put(static_cast<char>(c)); else cout.put(static_cast<char>(c)); }
    void write(const JString& s, int off, int len) { if (is_err) cerr << s.substr(off, len); else cout << s.substr(off, len); }
    void flush() { if (is_err) cerr.flush(); else cout.flush(); }
    void close() { flush(); }
};
