struct JCollections {
    static auto reverseOrder() {
        return [](const auto& a, const auto& b) { return a < b ? 1 : (a > b ? -1 : 0); };
    }
    template<class Comp> static auto reverseOrder(Comp comp) {
        return [comp](const auto& a, const auto& b) { return comp(b, a); };
    }
    template<class C> static void sort(C& c) { std::sort(c.begin(), c.end()); }
    template<class C, class Comp> static void sort(C& c, Comp comp) {
        if constexpr (is_same_v<decay_t<Comp>, nullptr_t>) {
            std::sort(c.begin(), c.end());
        } else {
            std::sort(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
        }
    }
    template<class C> static void sort(C& c, nullptr_t) {
        std::sort(c.begin(), c.end());
    }
    template<class T> static vector<T> singleton(const T& x) { return {x}; }
    template<class T> static vector<T> singletonList(const T& x) { return {x}; }
    // 未テスト
    // 計算量: O(1)
    template<class T> static vector<T> emptyList() { return {}; }
    // 未テスト
    // 計算量: O(1)
    template<class T> static JSet<T> emptySet() { return {}; }
    // 未テスト
    // 計算量: O(1)
    template<class K, class V> static JMap<K, V> emptyMap() { return {}; }
    template<class C> static void reverse(C& c) { std::reverse(c.begin(), c.end()); }
    template<class C> static void swap(C& c, int i, int j) { std::swap(c[i], c[j]); }
    template<class C> static auto max(const C& c) { return *std::max_element(c.begin(), c.end()); }
    template<class C, class Comp> static auto max(const C& c, Comp comp) {
        return *std::max_element(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
    }
    template<class C> static auto min(const C& c) { return *std::min_element(c.begin(), c.end()); }
    template<class C, class Comp> static auto min(const C& c, Comp comp) {
        return *std::min_element(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
    }
    template<class C, class T> static void fill(C& c, const T& val) { std::fill(c.begin(), c.end(), val); }
    template<class C, class T> static int binarySearch(const C& c, const T& key) {
        auto it = lower_bound(c.begin(), c.end(), key);
        if (it != c.end() && *it == key) return int(distance(c.begin(), it));
        return -int(distance(c.begin(), it)) - 1;
    }
    template<class C, class T, class Comp> static int binarySearch(const C& c, const T& key, Comp comp) {
        auto it = lower_bound(c.begin(), c.end(), key, [&](const auto& x, const auto& y) { return comp(x, y) < 0; });
        if (it != c.end() && comp(*it, key) == 0) return int(distance(c.begin(), it));
        return -int(distance(c.begin(), it)) - 1;
    }
    // 未テスト
    // 計算量: O(K)
    template<class C, class... Elements> static bool addAll(C& c, const Elements&... elements) {
        (c.add(elements), ...);
        return true;
    }
    // 未テスト
    // 計算量: O(N)
    template<class C, class T> static bool replaceAll(C& c, const T& oldVal, const T& newVal) {
        bool modified = false;
        for (auto& item : c) {
            if (jequals(item, oldVal)) { item = newVal; modified = true; }
        }
        return modified;
    }
    // 未テスト
    // 計算量: O(N)
    template<class C> static void rotate(C& c, int distance) {
        if (c.empty()) return;
        int n = static_cast<int>(c.size());
        distance %= n;
        if (distance < 0) distance += n;
        std::rotate(c.begin(), c.end() - distance, c.end());
    }
    // 未テスト
    // 計算量: O(N)
    template<class C1, class C2> static void copy(C1& dest, const C2& src) {
        for (size_t i = 0; i < src.size() && i < dest.size(); ++i) dest[i] = src[i];
    }
    // 未テスト
    // 計算量: O(N * M)
    template<class C1, class C2> static int indexOfSubList(const C1& source, const C2& target) {
        auto it = std::search(source.begin(), source.end(), target.begin(), target.end());
        return it != source.end() ? static_cast<int>(distance(source.begin(), it)) : -1;
    }
    // 未テスト
    // 計算量: O(N * M)
    template<class C1, class C2> static int lastIndexOfSubList(const C1& source, const C2& target) {
        auto it = std::find_end(source.begin(), source.end(), target.begin(), target.end());
        return it != source.end() ? static_cast<int>(distance(source.begin(), it)) : -1;
    }
    // 未テスト
    // 計算量: O(N)
    template<class C> static void shuffle(C& c) {
        static mt19937_64 rng(1337);
        std::shuffle(c.begin(), c.end(), rng);
    }
    // 未テスト
    // 計算量: O(N)
    template<class C, class R> static void shuffle(C& c, R& rnd) {
        std::shuffle(c.begin(), c.end(), rnd.engine);
    }
    // 未テスト
    // 計算量: O(N)
    template<class C, class T> static int frequency(const C& c, const T& o) {
        int count = 0;
        for (const auto& item : c) { if (jequals(item, o)) ++count; }
        return count;
    }
    // 未テスト
    // 計算量: O(N * M)
    template<class C1, class C2> static bool disjoint(const C1& c1, const C2& c2) {
        for (const auto& x : c1) {
            for (const auto& y : c2) {
                if (jequals(x, y)) return false;
            }
        }
        return true;
    }
};
