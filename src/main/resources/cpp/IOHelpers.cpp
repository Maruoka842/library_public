inline void jnoop() {}
inline void jprint() {}
template<class T, class... U> void jprint(const T& x, const U&... rest) { cout << jstr(x); jprint(rest...); }
inline void jprintln() { cout << '\n'; }
template<class T, class... U> void jprintln(const T& x, const U&... rest) { cout << jstr(x); jprint(rest...); cout << '\n'; }
template<class... Args> void jprintf(const char* fmt, Args&&... args) {
    printf(fmt, forward<Args>(args)...);
}
template<class... Args> void jprintf(const JString& fmt, Args&&... args) {
    printf(fmt.c_str(), forward<Args>(args)...);
}

template<class... Args> JString jformat(const char* fmt, Args&&... args) {
    int sz = snprintf(nullptr, 0, fmt, forward<Args>(args)...);
    if (sz <= 0) return "";
    vector<char> buf(sz + 1);
    snprintf(buf.data(), buf.size(), fmt, forward<Args>(args)...);
    return JString(buf.data(), sz);
}
template<class... Args> JString jformat(const JString& fmt, Args&&... args) {
    return jformat(fmt.c_str(), forward<Args>(args)...);
}
template<class L, class... Args>
typename enable_if<!is_convertible_v<L, JString> && !is_convertible_v<L, const char*>, JString>::type
jformat(const L&, const char* fmt, Args&&... args) {
    return jformat(fmt, forward<Args>(args)...);
}
template<class L, class... Args>
typename enable_if<!is_convertible_v<L, JString> && !is_convertible_v<L, const char*>, JString>::type
jformat(const L&, const JString& fmt, Args&&... args) {
    return jformat(fmt.c_str(), forward<Args>(args)...);
}
