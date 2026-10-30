size_t jhash() { return 1; }
template<class T> size_t jhashValue(const T& x) { return hash<T>{}(x); }
template<class T> size_t jhashValue(const vector<T>& values) {
    size_t result = 1;
    for (const auto& value : values) result = result * 31 + jhashValue(value);
    return result;
}
template<class T, class... U> size_t jhash(const T& x, const U&... rest) {
    return jhashValue(x) * 31 + jhash(rest...);
}
