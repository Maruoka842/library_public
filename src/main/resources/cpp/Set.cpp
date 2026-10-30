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
        if constexpr (jhas_ptr_equals<A>::value) {
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

template<class T> struct JSet : set<T, function<bool(const T&, const T&)>> {
    using Base = set<T, function<bool(const T&, const T&)>>;
    JSet() : Base(JLess<T>{}) {}
    JSet(const Base& o) : Base(o) {}
    JSet(Base&& o) : Base(move(o)) {}
    template<class C, class = enable_if_t<!is_same_v<decay_t<C>, JSet> && !is_same_v<decay_t<C>, Base> && !is_same_v<decay_t<C>, vector<T>>>>
    explicit JSet(C comp) : Base([comp](const T& a, const T& b) {
        using R = invoke_result_t<C, const T&, const T&>;
        if constexpr (is_same_v<R, bool>) return comp(a, b);
        else return comp(a, b) < 0;
    }) {}
    JSet(const vector<T>& v) : Base(JLess<T>{}) {
        for (const auto& x : v) this->insert(x);
    }
    JSet(initializer_list<T> init) : Base(JLess<T>{}) {
        for (const auto& x : init) this->insert(x);
    }
    bool add(const T& x) { return this->insert(x).second; }
    bool remove(const T& x) { return this->erase(x) != 0; }
    // 未テスト
    // 計算量: O(M log N)
    template<class C> bool addAll(const C& c) {
        bool modified = false;
        for (const auto& x : c) { if (add(x)) modified = true; }
        return modified;
    }
    // 未テスト
    // 計算量: O(M log N)
    template<class C> bool removeAll(const C& c) {
        bool modified = false;
        for (const auto& x : c) { if (remove(x)) modified = true; }
        return modified;
    }
    // 未テスト
    // 計算量: O(N log M)
    template<class C> bool retainAll(const C& c) {
        bool modified = false;
        for (auto it = this->begin(); it != this->end(); ) {
            bool found = false;
            for (const auto& x : c) {
                if (jequals(*it, x)) { found = true; break; }
            }
            if (!found) { it = this->erase(it); modified = true; }
            else { ++it; }
        }
        return modified;
    }
    // 未テスト
    // 計算量: O(M log N)
    template<class C> bool containsAll(const C& c) const {
        for (const auto& x : c) {
            if (!this->contains(x)) return false;
        }
        return true;
    }
    // 未テスト
    // 計算量: O(N)
    void clear() { Base::clear(); }
    template<class Arg>
    bool contains(const Arg& x) const {
        return std::find_if(this->begin(), this->end(), [&](const T& item) {
            return jequals(item, x);
        }) != this->end();
    }
    T first() const { return *this->begin(); }
    T last() const { return *this->rbegin(); }
    T pollFirst() {
        if (this->empty()) return T{};
        auto it = this->begin();
        T res = *it;
        this->erase(it);
        return res;
    }
    T pollLast() {
        if (this->empty()) return T{};
        auto it = prev(this->end());
        T res = *it;
        this->erase(it);
        return res;
    }
    T lower(const T& x) const {
        auto it = this->lower_bound(x);
        if (it != this->begin()) return *prev(it);
        return T{};
    }
    T floor(const T& x) const {
        auto it = this->upper_bound(x);
        if (it != this->begin()) return *prev(it);
        return T{};
    }
    T ceiling(const T& x) const {
        auto it = this->lower_bound(x);
        if (it != this->end()) return *it;
        return T{};
    }
    T higher(const T& x) const {
        auto it = this->upper_bound(x);
        if (it != this->end()) return *it;
        return T{};
    }
    int size() const { return (int)Base::size(); }
    bool isEmpty() const { return this->empty(); }
    JStream<T> stream() const { return JStream<T>(vector<T>(this->begin(), this->end())); }
};

template<class... T> auto jsetOf(T... args) { return JSet(jlistOf(args...)); }
