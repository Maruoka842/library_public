template<class T> struct JStack : vector<T> {
    JStack() = default;
    explicit JStack(size_t initialCapacity) { this->reserve(initialCapacity); }
    JStack(size_t count, const T& value) : vector<T>(count, value) {}
    JStack(const vector<T>& source) : vector<T>(source) {}
    JStack(vector<T>&& source) : vector<T>(move(source)) {}
    bool add(const T& x) { this->push_back(x); return true; }
    T push(const T& x) { this->push_back(x); return x; }
    T pop() { T x = this->back(); this->pop_back(); return x; }
    T peek() const { return this->back(); }
    // 未テスト
    // 計算量: O(N)
    int search(const T& x) const {
        for (size_t i = 0; i < this->size(); ++i) {
            if (jequals((*this)[this->size() - 1 - i], x)) return static_cast<int>(i + 1);
        }
        return -1;
    }
    int size() const { return (int)vector<T>::size(); }
    bool isEmpty() const { return this->empty(); }
    JStream<T> stream() const { return JStream<T>(*this); }
};
