template<class T> bool jgreater_fn(const T& a, const T& b) {
    if constexpr (jhas_ptr_compareTo<T>::value) {
        return a->compareTo(b) > 0;
    } else if constexpr (jhas_compareTo<T>::value) {
        return a.compareTo(b) > 0;
    } else {
        return a > b;
    }
}

template<class T> struct JGreater {
    bool operator()(const T& a, const T& b) const { return jgreater_fn(a, b); }
};

template<class T> struct JPriorityQueue {
    function<bool(const T&, const T&)> comp = JGreater<T>{};
    priority_queue<T, vector<T>, function<bool(const T&, const T&)>> q;

    JPriorityQueue() : q(comp) {}
    template<class C>
    explicit JPriorityQueue(C c) : comp([c](const T& a, const T& b) { return c(a, b) > 0; }), q(comp) {}
    template<class C>
    JPriorityQueue(int, C c) : comp([c](const T& a, const T& b) { return c(a, b) > 0; }), q(comp) {}
    explicit JPriorityQueue(int) : q(comp) {}

    bool add(const T& x) { q.push(x); return true; }
    // 未テスト
    // 計算量: O(|c| log N)
    template<class Container> bool addAll(const Container& c) {
        for (const auto& item : c) q.push(item);
        return true;
    }
    bool offer(const T& x) { q.push(x); return true; }
    T poll() { T x = q.top(); q.pop(); return x; }
    T peek() const { return q.top(); }
    int size() const { return (int)q.size(); }
    bool isEmpty() const { return q.empty(); }
    bool empty() const { return q.empty(); }
    void clear() { q = priority_queue<T, vector<T>, function<bool(const T&, const T&)>>(comp); }
    template<class Arg>
    bool remove(const Arg& x) {
        vector<T> v;
        bool removed = false;
        while (!q.empty()) {
            T item = q.top();
            q.pop();
            if (!removed && jequals(item, x)) {
                removed = true;
            } else {
                v.push_back(item);
            }
        }
        for (const auto& item : v) q.push(item);
        return removed;
    }
};
