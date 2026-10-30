template<class K, class V> struct JMap : map<K, V, function<bool(const K&, const K&)>> {
    using Base = map<K, V, function<bool(const K&, const K&)>>;
    JMap() : Base(JLess<K>{}) {}
    JMap(const Base& o) : Base(o) {}
    JMap(Base&& o) : Base(move(o)) {}
    template<class K2, class V2>
    JMap(const JMap<K2, V2>& source) : Base(JLess<K>{}) {
        for (const auto& [k, v] : source) {
            if constexpr (is_same_v<K2, JObject> && is_same_v<V2, JObject>) {
                this->insert({any_cast<K>(k), any_cast<V>(v)});
            } else if constexpr (is_same_v<K2, JObject>) {
                this->insert({any_cast<K>(k), static_cast<V>(v)});
            } else if constexpr (is_same_v<V2, JObject>) {
                this->insert({static_cast<K>(k), any_cast<V>(v)});
            } else {
                this->insert({static_cast<K>(k), static_cast<V>(v)});
            }
        }
    }
    template<class K2, class V2>
    JMap& operator=(const JMap<K2, V2>& source) {
        this->clear();
        for (const auto& [k, v] : source) {
            if constexpr (is_same_v<K2, JObject> && is_same_v<V2, JObject>) {
                this->insert({any_cast<K>(k), any_cast<V>(v)});
            } else if constexpr (is_same_v<K2, JObject>) {
                this->insert({any_cast<K>(k), static_cast<V>(v)});
            } else if constexpr (is_same_v<V2, JObject>) {
                this->insert({static_cast<K>(k), any_cast<V>(v)});
            } else {
                this->insert({static_cast<K>(k), static_cast<V>(v)});
            }
        }
        return *this;
    }
    template<class C, class = enable_if_t<!is_same_v<decay_t<C>, JMap> && !is_same_v<decay_t<C>, Base>>>
    explicit JMap(C comp) : Base([comp](const K& a, const K& b) {
        using R = invoke_result_t<C, const K&, const K&>;
        if constexpr (is_same_v<R, bool>) return comp(a, b);
        else return comp(a, b) < 0;
    }) {}
    template<class K2, class V2> V put(const K2& k, const V2& v) { K key = static_cast<K>(k); V val = static_cast<V>(v); V old{}; auto it = this->find(key); if (it != this->end()) old = it->second; (*this)[key] = val; return old; }
    template<class K2> V get(const K2& k) const { auto it = this->find(static_cast<K>(k)); return it == this->end() ? V{} : it->second; }
    template<class K2> V remove(const K2& k) { K key = static_cast<K>(k); V old{}; auto it = this->find(key); if (it != this->end()) { old = it->second; this->erase(it); } return old; }
    template<class K2> std::pair<K, V> floorEntry(const K2& k) const {
        auto it = this->upper_bound(static_cast<K>(k));
        if (it == this->begin()) return std::pair<K, V>{};
        return *std::prev(it);
    }
    template<class K2> std::pair<K, V> ceilingEntry(const K2& k) const {
        auto it = this->lower_bound(static_cast<K>(k));
        if (it == this->end()) return std::pair<K, V>{};
        return *it;
    }
    template<class K2> K floorKey(const K2& k) const {
        auto it = this->upper_bound(static_cast<K>(k));
        if (it == this->begin()) return K{};
        return std::prev(it)->first;
    }
    template<class K2> K ceilingKey(const K2& k) const {
        auto it = this->lower_bound(static_cast<K>(k));
        if (it == this->end()) return K{};
        return it->first;
    }
    // 未テスト
    // 計算量: O(log N)
    template<class K2> std::pair<K, V> lowerEntry(const K2& k) const {
        auto it = this->lower_bound(static_cast<K>(k));
        if (it == this->begin()) return std::pair<K, V>{};
        return *std::prev(it);
    }
    // 未テスト
    // 計算量: O(log N)
    template<class K2> std::pair<K, V> higherEntry(const K2& k) const {
        auto it = this->upper_bound(static_cast<K>(k));
        if (it == this->end()) return std::pair<K, V>{};
        return *it;
    }
    // 未テスト
    // 計算量: O(log N)
    template<class K2> K lowerKey(const K2& k) const {
        auto it = this->lower_bound(static_cast<K>(k));
        if (it == this->begin()) return K{};
        return std::prev(it)->first;
    }
    // 未テスト
    // 計算量: O(log N)
    template<class K2> K higherKey(const K2& k) const {
        auto it = this->upper_bound(static_cast<K>(k));
        if (it == this->end()) return K{};
        return it->first;
    }
    template<class K2, class V2> V getOrDefault(const K2& k, const V2& v) const { auto it = this->find(static_cast<K>(k)); return it == this->end() ? static_cast<V>(v) : it->second; }
    template<class K2> bool containsKey(const K2& k) const { return this->count(static_cast<K>(k)); }
    // 未テスト
    // 計算量: O(N)
    template<class V2> bool containsValue(const V2& v) const {
        for (const auto& [key, val] : *this) {
            if (jequals(val, v)) return true;
        }
        return false;
    }
    // 未テスト
    // 計算量: O(log N)
    bool replace(const K& key, const V& oldValue, const V& newValue) {
        auto it = this->find(key);
        if (it != this->end() && jequals(it->second, oldValue)) {
            it->second = newValue;
            return true;
        }
        return false;
    }
    // 未テスト
    // 計算量: O(log N)
    V replace(const K& key, const V& value) {
        auto it = this->find(key);
        if (it != this->end()) {
            V old = it->second;
            it->second = value;
            return old;
        }
        return V{};
    }
    // 未テスト
    // 計算量: O(N)
    template<class F> void replaceAll(F function) {
        for (auto& [key, val] : *this) {
            val = function(key, val);
        }
    }
    // 未テスト
    // 計算量: O(M log N)
    template<class M> void putAll(const M& m) {
        for (const auto& [k, v] : m) this->put(k, v);
    }
    // 未テスト
    // 計算量: O(N)
    void clear() { Base::clear(); }
    template<class F> V compute(const K& k, F&& remappingFunction) {
        V oldVal = this->count(k) ? (*this)[k] : V{};
        V newVal = remappingFunction(k, oldVal);
        (*this)[k] = newVal;
        return newVal;
    }
    template<class F> V merge(const K& k, const V& v, F&& remappingFunction) {
        auto it = this->find(k);
        if (it == this->end()) {
            (*this)[k] = v;
            return v;
        } else {
            V nv = remappingFunction(it->second, v);
            (*this)[k] = nv;
            return nv;
        }
    }
    template<class F> V computeIfAbsent(const K& k, F&& mappingFunction) {
        auto it = this->find(k);
        if (it != this->end()) return it->second;
        V v = mappingFunction(k);
        (*this)[k] = v;
        return v;
    }
    template<class F> V computeIfPresent(const K& k, F&& remappingFunction) {
        auto it = this->find(k);
        if (it == this->end()) return V{};
        V v = remappingFunction(k, it->second);
        (*this)[k] = v;
        return v;
    }
    V putIfAbsent(const K& k, const V& v) {
        auto it = this->find(k);
        if (it != this->end()) return it->second;
        (*this)[k] = v;
        return v;
    }
    JSet<std::pair<K, V>> entrySet() const {
        JSet<std::pair<K, V>> result;
        for (const auto& entry : *this) result.add(entry);
        return result;
    }
    JSet<K> keySet() const {
        JSet<K> result;
        for (const auto& entry : *this) result.add(entry.first);
        return result;
    }
    JList<V> values() const {
        JList<V> result;
        for (const auto& entry : *this) result.push_back(entry.second);
        return result;
    }
    int size() const { return (int)Base::size(); }
    bool isEmpty() const { return this->empty(); }
    K firstKey() const { return this->begin()->first; }
    K lastKey() const { return this->rbegin()->first; }
    std::pair<K, V> pollFirstEntry() {
        if (this->empty()) return {};
        auto it = this->begin();
        std::pair<K, V> res = *it;
        this->erase(it);
        return res;
    }
    std::pair<K, V> pollLastEntry() {
        if (this->empty()) return {};
        auto it = prev(this->end());
        std::pair<K, V> res = *it;
        this->erase(it);
        return res;
    }
    std::pair<K, V> firstEntry() const {
        if (this->empty()) return {};
        return *this->begin();
    }
    std::pair<K, V> lastEntry() const {
        if (this->empty()) return {};
        return *prev(this->end());
    }
    JStream<std::pair<K, V>> stream() const {
        vector<std::pair<K, V>> v(this->begin(), this->end());
        return JStream<std::pair<K, V>>(v);
    }
};
