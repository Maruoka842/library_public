inline vector<JObject> jlistOf() { return {}; }
template<class T> auto jlistOf(const vector<T>& v) { return v; }
template<class T, class... U> auto jlistOf(T first, U... rest) {
    using V = common_type_t<T, U...>;
    return vector<V>{static_cast<V>(first), static_cast<V>(rest)...};
}

template<class T> struct JList : vector<T> {
    JList() = default;
    explicit JList(size_t initialCapacity) { this->reserve(initialCapacity); }
    JList(size_t count, const T& value) : vector<T>(count, value) {}
    JList(const vector<T>& source) : vector<T>(source) {}
    JList(vector<T>&& source) : vector<T>(move(source)) {}
    template<class U> JList(const JList<U>& source) {
        for (const auto& value : source) {
            if constexpr (is_same_v<U, JObject>) this->push_back(any_cast<T>(value));
            else this->push_back(static_cast<T>(value));
        }
    }
    template<class Iter, class = typename std::iterator_traits<Iter>::value_type>
    JList(Iter first, Iter last) : vector<T>(first, last) {}
    template<class Container, class = enable_if_t<!is_convertible_v<Container, std::size_t> && !is_same_v<decay_t<Container>, JList> && !is_same_v<decay_t<Container>, vector<T>>>>
    JList(const Container& container) {
        for (const auto& item : container) this->push_back(item);
    }
    bool add(const T& x) { this->push_back(x); return true; }
    void add(int i, const T& x) { this->insert(this->begin() + i, x); }
    T get(int i) const { return (*this)[i]; }
    T set(int i, const T& x) { T old = (*this)[i]; (*this)[i] = x; return old; }
    int size() const { return (int)vector<T>::size(); }
    bool isEmpty() const { return this->empty(); }
    T remove(int index) {
        T old = (*this)[index];
        this->erase(this->begin() + index);
        return old;
    }
    template<class Arg>
    bool remove(const Arg& x) {
        if constexpr (is_integral_v<Arg> && !is_same_v<decay_t<Arg>, bool>) {
            if constexpr (is_integral_v<T> && !is_same_v<decay_t<T>, bool>) {
                auto it = std::find(this->begin(), this->end(), static_cast<T>(x));
                if (it != this->end()) { this->erase(it); return true; }
                return false;
            } else {
                auto it = std::find_if(this->begin(), this->end(), [&](const T& item) {
                    if constexpr (is_convertible_v<Arg, T>) return item == x;
                    else if constexpr (jhas_key<T>::value) return item->key == x;
                    else if constexpr (jhas_key_obj<T>::value) return item.key == x;
                    else return false;
                });
                if (it != this->end()) { this->erase(it); return true; }
                return false;
            }
        } else {
            auto it = std::find_if(this->begin(), this->end(), [&](const T& item) {
                if constexpr (is_convertible_v<Arg, T>) return item == x;
                else if constexpr (jhas_key<T>::value) return item->key == x;
                else if constexpr (jhas_key_obj<T>::value) return item.key == x;
                else return false;
            });
            if (it != this->end()) { this->erase(it); return true; }
            return false;
        }
    }
    template<class C> bool addAll(const C& c) {
        for (const auto& x : c) this->push_back(x);
        return true;
    }
    // 未テスト
    // 計算量: O(N + M)
    template<class C> bool addAll(int index, const C& c) {
        if (index < 0 || index > (int)this->size()) return false;
        size_t pos = index;
        for (const auto& x : c) {
            this->insert(this->begin() + pos, x);
            ++pos;
        }
        return true;
    }
    // 未テスト
    // 計算量: O(N * M)
    template<class C> bool removeAll(const C& c) {
        bool modified = false;
        for (auto it = this->begin(); it != this->end(); ) {
            bool found = false;
            for (const auto& x : c) {
                if (jequals(*it, x)) { found = true; break; }
            }
            if (found) { it = this->erase(it); modified = true; }
            else { ++it; }
        }
        return modified;
    }
    // 未テスト
    // 計算量: O(N * M)
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
    // 計算量: O(N * M)
    template<class C> bool containsAll(const C& c) const {
        for (const auto& x : c) {
            if (!this->contains(x)) return false;
        }
        return true;
    }
    // 未テスト
    // 計算量: O(N)
    template<class F> void replaceAll(F operator_fn) {
        for (auto& item : *this) item = operator_fn(item);
    }
    template<class Arg>
    bool contains(const Arg& x) const {
        return std::find_if(this->begin(), this->end(), [&](const T& item) {
            return jequals(item, x);
        }) != this->end();
    }
    int indexOf(const T& x) const {
        auto it = std::find(this->begin(), this->end(), x);
        return it == this->end() ? -1 : (int)distance(this->begin(), it);
    }
    int lastIndexOf(const T& x) const {
        auto it = std::find(this->rbegin(), this->rend(), x);
        return it == this->rend() ? -1 : (int)(this->size() - 1 - distance(this->rbegin(), it));
    }
    template<class Comp> void sort(Comp comp) {
        if constexpr (is_same_v<decay_t<Comp>, nullptr_t>) {
            std::sort(this->begin(), this->end());
        } else {
            using R = invoke_result_t<Comp, const T&, const T&>;
            std::sort(this->begin(), this->end(), [&](const auto& a, const auto& b) {
                if constexpr (is_same_v<R, bool>) return comp(a, b);
                else return comp(a, b) < 0;
            });
        }
    }
    void sort(nullptr_t) {
        std::sort(this->begin(), this->end());
    }
    void sort() {
        std::sort(this->begin(), this->end());
    }
    JList<T> subList(int fromIndex, int toIndex) const {
        if (fromIndex < 0) fromIndex = 0;
        if (toIndex > (int)this->size()) toIndex = (int)this->size();
        if (fromIndex >= toIndex) return JList<T>{};
        return JList<T>(this->begin() + fromIndex, this->begin() + toIndex);
    }
    JStream<T> stream() const { return JStream<T>(*this); }
};
