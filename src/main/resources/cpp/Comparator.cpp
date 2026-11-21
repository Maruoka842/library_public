template<class T, class = void> struct jhas_compareTo : false_type {};
template<class T> struct jhas_compareTo<T, void_t<decltype(declval<T>().compareTo(declval<T>()))>> : true_type {};

template<class T, class = void> struct jhas_ptr_compareTo : false_type {};
template<class T> struct jhas_ptr_compareTo<T, void_t<decltype(declval<T>()->compareTo(declval<T>()))>> : true_type {};

template<class T, class = void> struct jhas_equals : false_type {};
template<class T> struct jhas_equals<T, void_t<decltype(declval<T>().equals(declval<T>()))>> : true_type {};

template<class T, class = void> struct jhas_ptr_equals : false_type {};
template<class T> struct jhas_ptr_equals<T, void_t<decltype(declval<T>()->equals(declval<T>()))>> : true_type {};

// 未テスト
// 計算量: O(1)
template<class A, class B>
bool jequals(const A& a, const B& b) {
    if constexpr (is_same_v<decay_t<A>, decay_t<B>>) {
        if constexpr (jis_std_function<decay_t<A>>::value) {
            if (!a && !b) return true;
            if (!a || !b) return false;
            return a.target_type() == b.target_type();
        } else if constexpr (jhas_ptr_equals<A>::value) {
            if (a == b) return true;
            if (a && b) return a->equals(b);
            return false;
        } else if constexpr (jhas_equals<A>::value) {
            return a.equals(b);
        } else if constexpr (jhas_ptr_compareTo<A>::value) {
            if (a == b) return true;
            if (a && b) return a->compareTo(b) == 0;
            return false;
        } else if constexpr (jhas_compareTo<A>::value) {
            return a.compareTo(b) == 0;
        } else {
            return a == b;
        }
    } else if constexpr (is_convertible_v<B, A>) {
        return a == static_cast<A>(b);
    } else if constexpr (is_convertible_v<A, B>) {
        return static_cast<B>(a) == b;
    } else {
        return false;
    }
}

template<class A, class B>
int jcompareTo(const A& a, const B& b) {
    if constexpr (jhas_ptr_compareTo<A>::value) {
        return a->compareTo(b);
    } else if constexpr (jhas_compareTo<A>::value) {
        return a.compareTo(b);
    } else {
        return (a > b) - (a < b);
    }
}

template<class F> struct JComparator {
    F key;
    template<class T> int operator()(const T& a, const T& b) const {
        if constexpr (is_invocable_v<F, const T&, const T&>) {
            return key(a, b);
        } else {
            auto x = key(a), y = key(b);
            return jcompareTo(x, y);
        }
    }
    template<class G> auto thenComparing(G other) const;
    template<class G> auto thenComparingInt(G key) const { return thenComparing(key); }
    template<class G> auto thenComparingLong(G key) const { return thenComparing(key); }
    template<class G> auto thenComparingDouble(G key) const { return thenComparing(key); }
};
template<class A, class B> struct JChainedComparator {
    A first;
    B second;
    template<class T> int operator()(const T& a, const T& b) const {
        int result = first(a, b);
        return result != 0 ? result : second(a, b);
    }
    template<class G> auto thenComparing(G other) const;
    template<class G> auto thenComparingInt(G key) const { return thenComparing(key); }
    template<class G> auto thenComparingLong(G key) const { return thenComparing(key); }
    template<class G> auto thenComparingDouble(G key) const { return thenComparing(key); }
};
template<class F> template<class G> auto JComparator<F>::thenComparing(G other) const {
    return JChainedComparator<JComparator<F>, JComparator<G>>{*this, JComparator<G>{other}};
}
template<class A, class B> template<class G> auto JChainedComparator<A, B>::thenComparing(G other) const {
    return JChainedComparator<JChainedComparator<A, B>, JComparator<G>>{*this, JComparator<G>{other}};
}
template<class T> struct jis_jcomparator : false_type {};
template<class F> struct jis_jcomparator<JComparator<F>> : true_type {};
template<class A, class B> struct jis_jcomparator<JChainedComparator<A, B>> : true_type {};

template<class F> auto jcomparing(F key) {
    if constexpr (jis_jcomparator<decay_t<F>>::value) {
        return key;
    } else {
        return JComparator<F>{key};
    }
}

// 未テスト
// 計算量: O(1)
template<class F>
auto jreversed(F cmp) {
    return [cmp = std::move(cmp)](const auto& a, const auto& b) {
        return cmp(b, a);
    };
}

// 未テスト
// 計算量: O(1)
template<class T> struct JLess {
    bool operator()(const T& a, const T& b) const {
        if constexpr (is_same_v<decay_t<T>, JObject>) {
            if (!a.has_value()) return b.has_value();
            if (!b.has_value()) return false;
            if (a.type() != b.type()) return a.type().before(b.type());
            if (auto pa = std::any_cast<int>(&a)) return *pa < std::any_cast<int>(b);
            if (auto pa = std::any_cast<long long>(&a)) return *pa < std::any_cast<long long>(b);
            if (auto pa = std::any_cast<JString>(&a)) return *pa < std::any_cast<JString>(b);
            return false;
        } else if constexpr (jhas_ptr_compareTo<T>::value) {
            return a->compareTo(b) < 0;
        } else if constexpr (jhas_compareTo<T>::value) {
            return a.compareTo(b) < 0;
        } else {
            return a < b;
        }
    }
};

struct {
    template<class A, class B>
    bool operator()(const A& a, const B& b) const {
        return JLess<decay_t<A>>{}(a, b);
    }
} jless_fn;
