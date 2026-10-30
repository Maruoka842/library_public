template<class P, class = void> struct jhas_next : false_type {};
template<class P> struct jhas_next<P, void_t<decltype(declval<P>()->next())>> : true_type {};

template<class P, class = void> struct jhas_nextInt : false_type {};
template<class P> struct jhas_nextInt<P, void_t<decltype(declval<P>()->nextInt())>> : true_type {};

template<class P, class = void> struct jhas_nextLong : false_type {};
template<class P> struct jhas_nextLong<P, void_t<decltype(declval<P>()->nextLong())>> : true_type {};

template<class P, class = void> struct jhas_nextDouble : false_type {};
template<class P> struct jhas_nextDouble<P, void_t<decltype(declval<P>()->nextDouble())>> : true_type {};

template<class T> struct JIterator {
    function<bool()> fn_hasNext;
    function<T()> fn_next;
    vector<T> items;
    size_t pos = 0;
    bool valid = false;

    JIterator() = default;
    JIterator(const vector<T>& v) : items(v), valid(true) {
        fn_hasNext = [this]() { return pos < items.size(); };
        fn_next = [this]() { return items[pos++]; };
    }
    template<class Ptr, class = enable_if_t<!is_same_v<decay_t<Ptr>, vector<T>> && !is_same_v<decay_t<Ptr>, JIterator>>>
    JIterator(Ptr ptr) : valid(bool(ptr)) {
        if (ptr) {
            fn_hasNext = [ptr]() { return ptr->hasNext(); };
            fn_next = [ptr]() {
                if constexpr (jhas_next<Ptr>::value) {
                    return ptr->next();
                } else if constexpr (jhas_nextInt<Ptr>::value) {
                    return ptr->nextInt();
                } else if constexpr (jhas_nextLong<Ptr>::value) {
                    return ptr->nextLong();
                } else if constexpr (jhas_nextDouble<Ptr>::value) {
                    return ptr->nextDouble();
                } else {
                    return T{};
                }
            };
        }
    }
    virtual bool hasNext() {
        if (fn_hasNext) return fn_hasNext();
        return pos < items.size();
    }
    virtual T next() {
        if (fn_next) return fn_next();
        return items[pos++];
    }
    virtual int nextInt() {
        if constexpr (is_convertible_v<T, int>) return static_cast<int>(next());
        else return 0;
    }
    virtual long long nextLong() {
        if constexpr (is_convertible_v<T, long long>) return static_cast<long long>(next());
        else return 0;
    }
    virtual double nextDouble() {
        if constexpr (is_convertible_v<T, double>) return static_cast<double>(next());
        else return 0.0;
    }
    bool operator==(nullptr_t) const { return !valid && !fn_hasNext; }
    bool operator!=(nullptr_t) const { return valid || bool(fn_hasNext); }
};
template<class T> struct JIterable {
    function<JIterator<T>()> supplier;
    JIterable() = default;
    template<class F, class = enable_if_t<!is_same_v<decay_t<F>, vector<T>> && !is_same_v<decay_t<F>, JIterable>>>
    JIterable(F f) {
        supplier = [f]() -> JIterator<T> {
            return JIterator<T>(f());
        };
    }
    JIterable(const vector<T>& v) : supplier([v]() { return JIterator<T>(v); }) {}
    JIterator<T> iterator() const { return supplier ? supplier() : JIterator<T>(); }
    struct CppIterator {
        JIterator<T> it;
        optional<T> current_val;
        CppIterator() = default;
        explicit CppIterator(JIterator<T> iter) : it(iter) {
            if (it.hasNext()) current_val = it.next();
        }
        bool operator!=(const CppIterator&) const { return current_val.has_value(); }
        void operator++() {
            if (it.hasNext()) current_val = it.next();
            else current_val.reset();
        }
        T operator*() const { return *current_val; }
    };
    CppIterator begin() { return CppIterator(this->iterator()); }
    CppIterator end() { return CppIterator(); }
    CppIterator begin() const { return CppIterator(iterator()); }
    CppIterator end() const { return CppIterator(); }
};
