struct JPrintWriter {
    JPrintWriter() = default;
    explicit JPrintWriter(const JInputStream&) {}
    explicit JPrintWriter(nullptr_t) {}
    template<class T> void print(const T& x) { jprint(x); }
    template<class T> void println(const T& x) { jprintln(x); }
    template<class T> void print(const vector<T>& a) { print(a, " "); }
    template<class T> void println(const vector<T>& a) { println(a, " "); }
    template<class T> void print(const vector<T>& a, const JString& separator) {
        for (size_t i = 0; i < a.size(); ++i) { if (i) jprint(separator); jprint(a[i]); }
    }
    template<class T> void println(const vector<T>& a, const JString& separator) {
        for (size_t i = 0; i < a.size(); ++i) { if (i) jprint(separator); jprint(a[i]); }
        jprintln();
    }
    void println() { jprintln(); }
    void printlnYesNo(bool x) { jprintln(x ? "Yes" : "No"); }
    template<class... Args> void printf(Args&&... args) { jprintf(forward<Args>(args)...); }
    template<class T> void write(const T& x) { jprint(x); }
    void write(int c) { cout.put(static_cast<char>(c)); }
    void write(const JString& s, int off, int len) { cout << s.substr(off, len); }
    void flush() { cout.flush(); }
    void close() { cout.flush(); }
};
