struct Objects {
    template<class A, class B> static bool deepEquals(const A& a, const B& b) { return jequals(a, b); }
    template<class A, class B> static bool equals(const A& a, const B& b) { return jequals(a, b); }
    static int hashCode(const JObject&) { return 0; }
    template<class T> static int hashCode(const T& x) { return static_cast<int>(jhash(x)); }
    // 未テスト
    // 計算量: O(1)
    template<class T> static bool isNull(const T& obj) { return jisNull(obj); }
    // 未テスト
    // 計算量: O(1)
    template<class T> static bool nonNull(const T& obj) { return !jisNull(obj); }
    // 未テスト
    // 計算量: O(1)
    template<class T> static T requireNonNull(const T& obj, const JString& = "") { return obj; }
    // 未テスト
    // 計算量: O(1)
    template<class T> static T requireNonNullElse(const T& obj, const T& defaultObj) { return jisNull(obj) ? defaultObj : obj; }
    // 未テスト
    // 計算量: O(1)
    static int checkIndex(int index, int length) {
        if (index < 0 || index >= length) throw out_of_range("Index out of range");
        return index;
    }
    // 未テスト
    // 計算量: O(1)
    static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) throw out_of_range("Range out of bounds");
        return fromIndex;
    }
    // 未テスト
    // 計算量: O(1)
    static int checkFromIndexSize(int fromIndex, int size, int length) {
        if (fromIndex < 0 || size < 0 || fromIndex + size > length) throw out_of_range("Range out of bounds");
        return fromIndex;
    }
};
