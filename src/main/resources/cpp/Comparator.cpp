template<class A, class B> int jcompareTo(const A& a, const B& b);
template<class A, class B> bool jequals(const A& a, const B& b);

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
template<class F> auto jcomparing(F key) { return JComparator<F>{key}; }
