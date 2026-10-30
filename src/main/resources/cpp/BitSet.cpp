struct JBitSet {
    vector<uint64_t> words;

    JBitSet() = default;
    explicit JBitSet(int nbits) {
        if (nbits > 0) words.resize((nbits + 63) / 64, 0ULL);
    }

    void set(int bitIndex) {
        if (bitIndex < 0) return;
        size_t w = bitIndex / 64;
        if (w >= words.size()) words.resize(w + 1, 0ULL);
        words[w] |= (1ULL << (bitIndex % 64));
    }
    void set(int bitIndex, bool value) {
        if (value) set(bitIndex);
        else clear(bitIndex);
    }
    void set(int fromIndex, int toIndex, bool value = true) {
        for (int i = fromIndex; i < toIndex; ++i) set(i, value);
    }
    void clear(int bitIndex) {
        if (bitIndex < 0) return;
        size_t w = bitIndex / 64;
        if (w < words.size()) words[w] &= ~(1ULL << (bitIndex % 64));
    }
    void clear(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; ++i) clear(i);
    }
    void clear() {
        std::fill(words.begin(), words.end(), 0ULL);
    }
    bool get(int bitIndex) const {
        if (bitIndex < 0) return false;
        size_t w = bitIndex / 64;
        if (w >= words.size()) return false;
        return (words[w] & (1ULL << (bitIndex % 64))) != 0;
    }
    // 未テスト
    // 計算量: O((toIndex - fromIndex) / 64)
    JBitSet get(int fromIndex, int toIndex) const {
        JBitSet res;
        for (int i = fromIndex; i < toIndex; ++i) {
            if (get(i)) res.set(i - fromIndex);
        }
        return res;
    }
    void flip(int bitIndex) {
        if (bitIndex < 0) return;
        size_t w = bitIndex / 64;
        if (w >= words.size()) words.resize(w + 1, 0ULL);
        words[w] ^= (1ULL << (bitIndex % 64));
    }
    void flip(int fromIndex, int toIndex) {
        for (int i = fromIndex; i < toIndex; ++i) flip(i);
    }
    void and_(const JBitSet& set) {
        size_t common = min(words.size(), set.words.size());
        for (size_t i = 0; i < common; ++i) words[i] &= set.words[i];
        for (size_t i = common; i < words.size(); ++i) words[i] = 0ULL;
    }
    void or_(const JBitSet& set) {
        if (set.words.size() > words.size()) words.resize(set.words.size(), 0ULL);
        for (size_t i = 0; i < set.words.size(); ++i) words[i] |= set.words[i];
    }
    void xor_(const JBitSet& set) {
        if (set.words.size() > words.size()) words.resize(set.words.size(), 0ULL);
        for (size_t i = 0; i < set.words.size(); ++i) words[i] ^= set.words[i];
    }
    void andNot(const JBitSet& set) {
        size_t common = min(words.size(), set.words.size());
        for (size_t i = 0; i < common; ++i) words[i] &= ~set.words[i];
    }
    int cardinality() const {
        int count = 0;
        for (uint64_t w : words) count += __builtin_popcountll(w);
        return count;
    }
    bool intersects(const JBitSet& set) const {
        size_t common = min(words.size(), set.words.size());
        for (size_t i = 0; i < common; ++i) {
            if ((words[i] & set.words[i]) != 0ULL) return true;
        }
        return false;
    }
    int nextSetBit(int fromIndex) const {
        if (fromIndex < 0) return -1;
        size_t w = fromIndex / 64;
        if (w >= words.size()) return -1;
        uint64_t word = words[w] & (~0ULL << (fromIndex % 64));
        if (word != 0) return static_cast<int>(w * 64 + __builtin_ctzll(word));
        for (size_t i = w + 1; i < words.size(); ++i) {
            if (words[i] != 0) return static_cast<int>(i * 64 + __builtin_ctzll(words[i]));
        }
        return -1;
    }
    // 未テスト
    // 計算量: O(N)
    int previousSetBit(int fromIndex) const {
        if (fromIndex < 0) return -1;
        size_t w = fromIndex / 64;
        if (w >= words.size()) w = words.empty() ? 0 : words.size() - 1;
        for (int i = fromIndex; i >= 0; --i) {
            if (get(i)) return i;
        }
        return -1;
    }
    // 未テスト
    // 計算量: O(N)
    int nextClearBit(int fromIndex) const {
        if (fromIndex < 0) return -1;
        for (int i = fromIndex; ; ++i) {
            if (!get(i)) return i;
        }
    }
    // 未テスト
    // 計算量: O(N)
    int previousClearBit(int fromIndex) const {
        if (fromIndex < 0) return -1;
        for (int i = fromIndex; i >= 0; --i) {
            if (!get(i)) return i;
        }
        return -1;
    }
    int length() const {
        for (int i = static_cast<int>(words.size()) - 1; i >= 0; --i) {
            if (words[i] != 0) {
                return i * 64 + 64 - __builtin_clzll(words[i]);
            }
        }
        return 0;
    }
    int size() const { return static_cast<int>(words.size() * 64); }
    bool isEmpty() const { return cardinality() == 0; }
    bool empty() const { return isEmpty(); }
    vector<int8_t> toByteArray() const {
        int len = (length() + 7) / 8;
        vector<int8_t> bytes(len, 0);
        for (int i = 0; i < len; ++i) {
            size_t w = i / 8;
            int shift = (i % 8) * 8;
            if (w < words.size()) {
                bytes[i] = static_cast<int8_t>((words[w] >> shift) & 0xFF);
            }
        }
        return bytes;
    }
    vector<long long> toLongArray() const {
        int len = (length() + 63) / 64;
        vector<long long> res(len, 0);
        for (int i = 0; i < len; ++i) {
            if ((size_t)i < words.size()) res[i] = static_cast<long long>(words[i]);
        }
        return res;
    }
    static JBitSet valueOf(const vector<long long>& longArray) {
        JBitSet bs;
        bs.words.resize(longArray.size());
        for (size_t i = 0; i < longArray.size(); ++i) {
            bs.words[i] = static_cast<uint64_t>(longArray[i]);
        }
        return bs;
    }
    JString toString() const {
        JString s = "{";
        bool first = true;
        for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
            if (!first) s += ", ";
            s += to_string(i);
            first = false;
        }
        s += "}";
        return s;
    }
    bool operator==(const JBitSet& o) const {
        size_t max_sz = max(words.size(), o.words.size());
        for (size_t i = 0; i < max_sz; ++i) {
            uint64_t w1 = i < words.size() ? words[i] : 0ULL;
            uint64_t w2 = i < o.words.size() ? o.words[i] : 0ULL;
            if (w1 != w2) return false;
        }
        return true;
    }
    bool operator!=(const JBitSet& o) const { return !(*this == o); }
};
