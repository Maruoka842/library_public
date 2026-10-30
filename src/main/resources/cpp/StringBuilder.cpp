struct JStringBuilder {
    JString value;
    JStringBuilder() = default;
    explicit JStringBuilder(const JString& initial) : value(initial) {}
    template<class T> JStringBuilder& append(const T& x) { value += jstr(x); return *this; }
    JStringBuilder& appendCodePoint(int cp) { value += static_cast<char>(cp); return *this; }
    // 未テスト
    // 計算量: O(|value| + |x|)
    template<class T> JStringBuilder& insert(int offset, const T& x) {
        if (offset >= 0 && offset <= static_cast<int>(value.size())) {
            value.insert(offset, jstr(x));
        }
        return *this;
    }
    // 未テスト
    // 計算量: O(|value| * |str|)
    int indexOf(const JString& str, int fromIndex = 0) const { return jindexOf(value, str, fromIndex); }
    // 未テスト
    // 計算量: O(|value| * |str|)
    int lastIndexOf(const JString& str) const { return jlastIndexOf(value, str); }
    // 未テスト
    // 計算量: O(|value| * |str|)
    int lastIndexOf(const JString& str, int fromIndex) const { return jlastIndexOf(value, str, fromIndex); }
    // 未テスト
    // 計算量: O(|value|)
    JStringBuilder& replace(int start, int end, const JString& str) {
        if (start >= 0 && start <= static_cast<int>(value.size()) && start <= end) {
            int len = min(end, static_cast<int>(value.size())) - start;
            value.replace(start, len, str);
        }
        return *this;
    }
    // 未テスト
    // 計算量: O(1)
    int capacity() const { return static_cast<int>(value.capacity()); }
    // 未テスト
    // 計算量: O(minimumCapacity)
    void ensureCapacity(int minimumCapacity) { value.reserve(minimumCapacity); }
    JStringBuilder& reverse() { std::reverse(value.begin(), value.end()); return *this; }
    int length() const { return static_cast<int>(value.size()); }
    void setLength(int newLength) { value.resize(newLength); }
    char charAt(int index) const { return value[index]; }
    void setCharAt(int index, char ch) { value[index] = ch; }
    JStringBuilder& deleteCharAt(int index) { value.erase(index, 1); return *this; }
    JStringBuilder& delete_(int start, int end) { value.erase(start, end - start); return *this; }
    JString substring(int start) const { return value.substr(start); }
    JString substring(int start, int end) const { return value.substr(start, end - start); }
    JString toString() const { return value; }
    friend ostream& operator<<(ostream& os, const JStringBuilder& sb) {
        return os << sb.value;
    }
};
