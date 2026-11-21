template<class T> struct JPriorityQueue {
    function<int(const T&, const T&)> comp = [](const T& a, const T& b) { return jcompareTo(a, b); };
    vector<T> data;

    void siftUp(size_t k, T x) {
        while (k > 0) {
            size_t parent = (k - 1) / 2;
            const T& e = data[parent];
            if (comp(x, e) >= 0) break;
            data[k] = std::move(data[parent]);
            k = parent;
        }
        data[k] = std::move(x);
    }

    void siftDown(size_t k, T x) {
        size_t half = data.size() / 2;
        while (k < half) {
            size_t child = (k * 2) + 1;
            size_t right = child + 1;
            if (right < data.size() && comp(data[child], data[right]) > 0) {
                child = right;
            }
            if (comp(x, data[child]) <= 0) break;
            data[k] = std::move(data[child]);
            k = child;
        }
        data[k] = std::move(x);
    }

    void heapify() {
        if (data.size() <= 1) return;
        for (int i = (static_cast<int>(data.size()) / 2) - 1; i >= 0; --i) {
            T item = std::move(data[i]);
            siftDown(static_cast<size_t>(i), std::move(item));
        }
    }

    JPriorityQueue() = default;
    template<class C, class = enable_if_t<is_invocable_v<C, const T&, const T&>>>
    explicit JPriorityQueue(C c) : comp([c](const T& a, const T& b) { return c(a, b); }) {}
    template<class C, class = enable_if_t<is_invocable_v<C, const T&, const T&>>>
    JPriorityQueue(int, C c) : comp([c](const T& a, const T& b) { return c(a, b); }) {}
    explicit JPriorityQueue(int) {}
    // 未テスト
    // 計算量: O(|c|)
    template<class Container, class = enable_if_t<!is_convertible_v<Container, std::size_t> && !is_same_v<decay_t<Container>, JPriorityQueue> && !is_invocable_v<Container, const T&, const T&>>>
    explicit JPriorityQueue(const Container& c) {
        for (const auto& item : c) {
            if constexpr (is_same_v<decay_t<decltype(item)>, JObject>) data.push_back(any_cast<T>(item));
            else data.push_back(item);
        }
        heapify();
    }

    bool add(const T& x) {
        size_t i = data.size();
        data.push_back(x);
        siftUp(i, x);
        return true;
    }
    // 未テスト
    // 計算量: O(|c| log N)
    template<class Container> bool addAll(const Container& c) {
        for (const auto& item : c) add(item);
        return true;
    }
    bool offer(const T& x) { return add(x); }
    T poll() {
        if (data.empty()) return T{};
        T result = std::move(data[0]);
        T last = std::move(data.back());
        data.pop_back();
        if (!data.empty()) {
            siftDown(0, std::move(last));
        }
        return result;
    }
    T peek() const { return data.empty() ? T{} : data[0]; }
    // 未テスト
    // 計算量: O(1)
    T& peek() { return data[0]; }
    int size() const { return (int)data.size(); }
    bool isEmpty() const { return data.empty(); }
    bool empty() const { return data.empty(); }
    void clear() { data.clear(); }
    // 未テスト
    // 計算量: O(1)
    auto begin() const { return data.begin(); }
    // 未テスト
    // 計算量: O(1)
    auto end() const { return data.end(); }
    // 未テスト
    // 計算量: O(1)
    auto begin() { return data.begin(); }
    // 未テスト
    // 計算量: O(1)
    auto end() { return data.end(); }

    template<class Arg>
    bool remove(const Arg& x) {
        for (size_t i = 0; i < data.size(); ++i) {
            if (jequals(data[i], x)) {
                T last = std::move(data.back());
                data.pop_back();
                if (i < data.size()) {
                    siftDown(i, last);
                    if (jequals(data[i], last)) {
                        siftUp(i, std::move(last));
                    }
                }
                return true;
            }
        }
        return false;
    }
};
