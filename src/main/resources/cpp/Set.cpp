template<class... T> auto jlistOf(T... args);
template<class T> struct JStream;

template<class T> struct JSet : set<T, function<bool(const T&, const T&)>> {
    using Base = set<T, function<bool(const T&, const T&)>>;
    JSet() : Base(JLess<T>{}) {}
    JSet(const Base& o) : Base(o) {}
    JSet(Base&& o) : Base(move(o)) {}
    template<class Container, class = enable_if_t<!is_same_v<decay_t<Container>, JSet> && !is_same_v<decay_t<Container>, Base> && !is_same_v<decay_t<Container>, vector<T>> && is_invocable_v<Container, const T&, const T&>>>
    explicit JSet(Container comp) : Base([comp](const T& a, const T& b) {
        using R = invoke_result_t<Container, const T&, const T&>;
        if constexpr (is_same_v<R, bool>) return comp(a, b);
        else return comp(a, b) < 0;
    }) {}
    template<class Container, class = enable_if_t<!is_same_v<decay_t<Container>, JSet> && !is_same_v<decay_t<Container>, Base> && !is_same_v<decay_t<Container>, vector<T>> && !is_invocable_v<Container, const T&, const T&>>>
    // 未テスト
    // 計算量: O(|c| log |c|)
    explicit JSet(const Container& c) : Base(JLess<T>{}) {
        for (const auto& x : c) this->insert(x);
    }
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
    JOptionalVal<T> lower(const T& x) const {
        auto it = this->lower_bound(x);
        if (it != this->begin()) return *prev(it);
        return nullptr;
    }
    JOptionalVal<T> floor(const T& x) const {
        auto it = this->upper_bound(x);
        if (it != this->begin()) return *prev(it);
        return nullptr;
    }
    JOptionalVal<T> ceiling(const T& x) const {
        auto it = this->lower_bound(x);
        if (it != this->end()) return *it;
        return nullptr;
    }
    JOptionalVal<T> higher(const T& x) const {
        auto it = this->upper_bound(x);
        if (it != this->end()) return *it;
        return nullptr;
    }
    int size() const { return (int)Base::size(); }
    bool isEmpty() const { return this->empty(); }
    auto stream() const { return JStream<T>(vector<T>(this->begin(), this->end())); }
    // 未テスト
    // 計算量: O(N)
    bool equals(const JObject& o) const {
        if (auto p = std::any_cast<JSet<T>>(&o)) return *this == *p;
        return false;
    }
    // 未テスト
    // 計算量: O(N)
    int hashCode() const {
        int h = 0;
        for (const auto& x : *this) h += static_cast<int>(jhash(x));
        return h;
    }
};

template<class... T> auto jsetOf(T... args) { return JSet(jlistOf(args...)); }
