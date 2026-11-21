template<class P, class = void> struct jhas_next : false_type {};
template<class P> struct jhas_next<P, void_t<decltype(declval<P>()->next())>> : true_type {};

template<class P, class = void> struct jhas_nextInt : false_type {};
template<class P> struct jhas_nextInt<P, void_t<decltype(declval<P>()->nextInt())>> : true_type {};

template<class P, class = void> struct jhas_nextLong : false_type {};
template<class P> struct jhas_nextLong<P, void_t<decltype(declval<P>()->nextLong())>> : true_type {};

template<class P, class = void> struct jhas_nextDouble : false_type {};
template<class P> struct jhas_nextDouble<P, void_t<decltype(declval<P>()->nextDouble())>> : true_type {};

// 未テスト
// 計算量: O(1)
template<class C, class = void> struct jhas_iterator : false_type {};
template<class C> struct jhas_iterator<C, void_t<decltype(declval<C>().iterator())>> : true_type {};

// 未テスト
// 計算量: O(1)
template<class C, class = void> struct jhas_ptr_iterator : false_type {};
template<class C> struct jhas_ptr_iterator<C, void_t<decltype(declval<C>()->iterator())>> : true_type {};

// 未テスト
// 計算量: O(1)
template<class C, class = void> struct jhas_begin_end : false_type {};
template<class C> struct jhas_begin_end<C, void_t<decltype(declval<C>().begin()), decltype(declval<C>().end())>> : true_type {};

// 未テスト
// 計算量: O(1)
template<class C, class = void> struct jhas_ptr_begin_end : false_type {};
template<class C> struct jhas_ptr_begin_end<C, void_t<decltype(declval<C>()->begin()), decltype(declval<C>()->end())>> : true_type {};

template<class T> struct JIterable;

template<class T> struct JIterator {
    function<bool()> fn_hasNext;
    function<T()> fn_next;
    vector<T> items;
    size_t pos = 0;
    bool valid = false;

    JIterator() = default;
    JIterator(const vector<T>& v) : items(v), valid(true) {}
    JIterator(const JIterable<T>& it);

    template<class Ptr, class = enable_if_t<!is_same_v<decay_t<Ptr>, vector<T>> && !is_same_v<decay_t<Ptr>, JIterator> && !is_same_v<decay_t<Ptr>, JIterable<T>>>>
    JIterator(Ptr ptr) {
        if constexpr (jhas_iterator<decay_t<Ptr>>::value) {
            *this = ptr.iterator();
        } else if constexpr (jhas_ptr_iterator<decay_t<Ptr>>::value) {
            if (ptr) *this = ptr->iterator();
        } else if constexpr (jhas_begin_end<decay_t<Ptr>>::value) {
            items.assign(ptr.begin(), ptr.end());
            valid = true;
        } else if constexpr (jhas_ptr_begin_end<decay_t<Ptr>>::value) {
            if (ptr) {
                items.assign(ptr->begin(), ptr->end());
                valid = true;
            }
        } else {
            valid = bool(ptr);
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
        if constexpr (jhas_iterator<decay_t<F>>::value) {
            supplier = [f]() -> JIterator<T> {
                return f.iterator();
            };
        } else if constexpr (jhas_ptr_iterator<decay_t<F>>::value) {
            supplier = [f]() -> JIterator<T> {
                return f ? f->iterator() : JIterator<T>();
            };
        } else if constexpr (jhas_begin_end<decay_t<F>>::value) {
            supplier = [f]() -> JIterator<T> {
                vector<T> v;
                for (auto it = f.begin(); it != f.end(); ++it) v.push_back(*it);
                return JIterator<T>(v);
            };
        } else if constexpr (jhas_ptr_begin_end<decay_t<F>>::value) {
            supplier = [f]() -> JIterator<T> {
                if (!f) return JIterator<T>();
                vector<T> v;
                for (auto it = f->begin(); it != f->end(); ++it) v.push_back(*it);
                return JIterator<T>(v);
            };
        } else {
            supplier = [f]() -> JIterator<T> {
                return JIterator<T>(f());
            };
        }
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

template<class T>
inline JIterator<T>::JIterator(const JIterable<T>& it) : valid(bool(it.supplier)) {
    *this = it.iterator();
}

template<class T> JIterator(const JIterable<T>&) -> JIterator<T>;

template<class C, class = void> struct jcontainer_value_type { using type = void; };
template<class C> struct jcontainer_value_type<C, void_t<typename C::value_type>> { using type = typename C::value_type; };

template<class C> JIterator(const C&) -> JIterator<typename jcontainer_value_type<decay_t<C>>::type>;
