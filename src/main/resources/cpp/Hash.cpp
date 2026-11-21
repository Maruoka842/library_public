size_t jhash() { return 1; }
// 未テスト
// 計算量: O(1)
template<class A, class B> size_t jhashValue(const pair<A, B>& p);
template<class T, class = void> struct jhas_hash_code : false_type {};
template<class T> struct jhas_hash_code<T, void_t<decltype(declval<T>().hashCode())>> : true_type {};

template<class T, class = void> struct jhas_begin_end_range : false_type {};
template<class T> struct jhas_begin_end_range<T, void_t<decltype(declval<T>().begin()), decltype(declval<T>().end())>> : true_type {};

template<class T> size_t jhashValue(const T& x) {
    if constexpr (is_same_v<decay_t<T>, JObject>) {
        return x.has_value() ? 1 : 0;
    } else if constexpr (jis_std_function<decay_t<T>>::value) {
        return x ? 1 : 0;
    } else if constexpr (jis_nullable_val<decay_t<T>>::value || jis_optional<decay_t<T>>::value) {
        return x.has_value() ? jhashValue(*x) : 0;
    } else if constexpr (jhas_hash_code<decay_t<T>>::value) {
        return static_cast<size_t>(x.hashCode());
    } else if constexpr (jhas_begin_end_range<decay_t<T>>::value) {
        size_t result = 0;
        for (const auto& item : x) {
            result += jhashValue(item);
        }
        return result;
    } else {
        return hash<T>{}(x);
    }
}

// 未テスト
// 計算量: O(1)
template<class A, class B> size_t jhashValue(const pair<A, B>& p) {
    return jhashValue(p.first) ^ jhashValue(p.second);
}

template<class T> size_t jhashValue(const vector<T>& values) {
    size_t result = 1;
    for (const auto& value : values) result = result * 31 + jhashValue(value);
    return result;
}
template<class T, class... U> size_t jhash(const T& x, const U&... rest) {
    return jhashValue(x) * 31 + jhash(rest...);
}
