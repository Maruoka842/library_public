template<class T> struct JStream;
template<class T> struct JDeque : deque<T> {
    JDeque() = default;
    explicit JDeque(size_t initialCapacity) {}
    JDeque(size_t count, const T& value) : deque<T>(count, value) {}
    JDeque(const deque<T>& source) : deque<T>(source) {}
    JDeque(deque<T>&& source) : deque<T>(move(source)) {}
    // 未テスト
    // 計算量: O(N)
    template<class Container, class = enable_if_t<!is_convertible_v<Container, std::size_t> && !is_same_v<decay_t<Container>, JDeque> && !is_same_v<decay_t<Container>, deque<T>>>>
    JDeque(const Container& container) {
        for (const auto& item : container) {
            if constexpr (is_same_v<decay_t<decltype(item)>, JObject>) this->push_back(any_cast<T>(item));
            else this->push_back(item);
        }
    }
    bool add(const T& x) { this->push_back(x); return true; }
    template<class Container> bool addAll(const Container& c) {
        for (const auto& item : c) this->push_back(item);
        return true;
    }
    void addFirst(const T& x) { this->push_front(x); }
    void addLast(const T& x) { this->push_back(x); }
    T pollFirst() { T x = this->front(); this->pop_front(); return x; }
    T pollLast() { T x = this->back(); this->pop_back(); return x; }
    T poll() { return pollFirst(); }
    T& peek() { return this->front(); }
    const T& peek() const { return this->front(); }
    T& peekFirst() { return this->front(); }
    const T& peekFirst() const { return this->front(); }
    T& peekLast() { return this->back(); }
    const T& peekLast() const { return this->back(); }
    T& element() { return this->front(); }
    const T& element() const { return this->front(); }
    T remove() { return pollFirst(); }
    T removeFirst() { return pollFirst(); }
    T removeLast() { return pollLast(); }
    // 未テスト
    // 計算量: O(N)
    bool removeFirstOccurrence(const T& x) {
        auto it = std::find_if(this->begin(), this->end(), [&](const T& item) { return jequals(item, x); });
        if (it != this->end()) { this->erase(it); return true; }
        return false;
    }
    // 未テスト
    // 計算量: O(N)
    bool removeLastOccurrence(const T& x) {
        auto it = std::find_if(this->rbegin(), this->rend(), [&](const T& item) { return jequals(item, x); });
        if (it != this->rend()) {
            auto forward_it = prev(it.base());
            this->erase(forward_it);
            return true;
        }
        return false;
    }
    // 未テスト
    // 計算量: O(N)
    void clear() { deque<T>::clear(); }
    T getFirst() const { return this->front(); }
    void push(const T& x) { this->push_front(x); }
    T pop() { return pollFirst(); }
    T getLast() const { return this->back(); }
    bool offer(const T& x) { return add(x); }
    bool offerFirst(const T& x) { addFirst(x); return true; }
    bool offerLast(const T& x) { addLast(x); return true; }
    template<class Arg>
    bool contains(const Arg& x) const {
        return std::find_if(this->begin(), this->end(), [&](const T& item) {
            return jequals(item, x);
        }) != this->end();
    }
    int size() const { return (int)deque<T>::size(); }
    bool isEmpty() const { return this->empty(); }
    auto stream() const { return JStream<T>(vector<T>(this->begin(), this->end())); }
    // 未テスト
    // 計算量: O(N)
    auto descendingIterator() const {
        return JIterator<T>(vector<T>(this->rbegin(), this->rend()));
    }
};
