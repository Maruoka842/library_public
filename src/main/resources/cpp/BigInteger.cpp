struct JBigInteger {
    __int128 val = 0;
    JBigInteger() = default;
    JBigInteger(__int128 v) : val(v) {}
    JBigInteger(long long v) : val(v) {}
    JBigInteger(int v) : val(v) {}
    JBigInteger(const JString& s) {
        val = 0;
        bool neg = false;
        size_t start = 0;
        if (!s.empty() && s[0] == '-') { neg = true; start = 1; }
        for (size_t i = start; i < s.size(); ++i) {
            if (isdigit(static_cast<unsigned char>(s[i]))) val = val * 10 + (s[i] - '0');
        }
        if (neg) val = -val;
    }
    static JBigInteger valueOf(long long v) { return JBigInteger(v); }
    template<class R> static JBigInteger probablePrime(int bitLength, R&) {
        uint64_t v = (1ULL << (bitLength - 1)) | 1ULL;
        return JBigInteger(static_cast<long long>(v));
    }
    static const JBigInteger ZERO;
    static const JBigInteger ONE;
    static const JBigInteger TWO;
    static const JBigInteger TEN;

    JBigInteger add(const JBigInteger& o) const { return JBigInteger(val + o.val); }
    JBigInteger subtract(const JBigInteger& o) const { return JBigInteger(val - o.val); }
    JBigInteger multiply(const JBigInteger& o) const { return JBigInteger(val * o.val); }
    JBigInteger divide(const JBigInteger& o) const { return JBigInteger(val / o.val); }
    JBigInteger remainder(const JBigInteger& o) const { return JBigInteger(val % o.val); }
    JBigInteger mod(const JBigInteger& o) const {
        __int128 m = val % o.val;
        if (m < 0) m += (o.val < 0 ? -o.val : o.val);
        return JBigInteger(m);
    }
    // 未テスト
    // 計算量: O(log(exp))
    JBigInteger modPow(const JBigInteger& exponent, const JBigInteger& m) const {
        __int128 res = 1, base = val % m.val, exp = exponent.val;
        if (base < 0) base += m.val;
        while (exp > 0) {
            if (exp & 1) res = (res * base) % m.val;
            base = (base * base) % m.val;
            exp >>= 1;
        }
        return JBigInteger(res);
    }
    // 未テスト
    // 計算量: O(log(min(a, b)))
    JBigInteger modInverse(const JBigInteger& m) const {
        __int128 a = val % m.val;
        if (a < 0) a += m.val;
        __int128 b = m.val;
        __int128 u = 1, v = 0;
        while (b) {
            __int128 t = a / b;
            a -= t * b; std::swap(a, b);
            u -= t * v; std::swap(u, v);
        }
        if (u < 0) u += m.val;
        return JBigInteger(u);
    }
    // 未テスト
    // 計算量: O(1)
    JBigInteger min(const JBigInteger& o) const { return o.val < val ? o : *this; }
    // 未テスト
    // 計算量: O(1)
    JBigInteger max(const JBigInteger& o) const { return o.val > val ? o : *this; }

    JBigInteger pow(int exp) const {
        __int128 res = 1, base = val;
        while (exp > 0) {
            if (exp & 1) res *= base;
            base *= base;
            exp >>= 1;
        }
        return JBigInteger(res);
    }
    JBigInteger shiftLeft(int n) const { return JBigInteger(val << n); }
    JBigInteger shiftRight(int n) const { return JBigInteger(val >> n); }
    bool testBit(int n) const { return ((val >> n) & 1) != 0; }
    JBigInteger setBit(int n) const { return JBigInteger(val | ((__int128)1 << n)); }
    JBigInteger clearBit(int n) const { return JBigInteger(val & ~((__int128)1 << n)); }
    JBigInteger flipBit(int n) const { return JBigInteger(val ^ ((__int128)1 << n)); }
    JBigInteger or_(const JBigInteger& o) const { return JBigInteger(val | o.val); }
    JBigInteger and_(const JBigInteger& o) const { return JBigInteger(val & o.val); }
    JBigInteger xor_(const JBigInteger& o) const { return JBigInteger(val ^ o.val); }
    JBigInteger not_() const { return JBigInteger(~val); }
    JBigInteger andNot(const JBigInteger& o) const { return JBigInteger(val & ~o.val); }
    JBigInteger abs() const { return JBigInteger(val < 0 ? -val : val); }
    JBigInteger negate() const { return JBigInteger(-val); }
    JBigInteger gcd(const JBigInteger& o) const {
        __int128 a = val < 0 ? -val : val, b = o.val < 0 ? -o.val : o.val;
        while (b) { a %= b; std::swap(a, b); }
        return JBigInteger(a);
    }
    int bitCount() const { return __builtin_popcountll(static_cast<uint64_t>(val)) + __builtin_popcountll(static_cast<uint64_t>(val >> 64)); }
    int bitLength() const {
        __int128 v = val < 0 ? -val : val;
        int len = 0;
        while (v > 0) { len++; v >>= 1; }
        return len;
    }
    int signum() const { return (val > 0) - (val < 0); }
    int compareTo(const JBigInteger& o) const { return (val > o.val) - (val < o.val); }
    bool equals(const JBigInteger& o) const { return val == o.val; }
    bool isProbablePrime(int certainty = 10) const {
        if (val <= 1) return false;
        if (val <= 3) return true;
        if (val % 2 == 0 || val % 3 == 0) return false;
        auto power = [](__int128 base, __int128 exp, __int128 mod) {
            base %= mod;
            __int128 res = 1;
            while (exp > 0) {
                if (exp % 2 == 1) res = (res * base) % mod;
                base = (base * base) % mod;
                exp /= 2;
            }
            return res;
        };
        auto millerRabin = [&](__int128 n, int a) {
            if (n % a == 0) return false;
            __int128 d = n - 1;
            while (d % 2 == 0) d /= 2;
            __int128 x = power(a, d, n);
            if (x == 1 || x == n - 1) return true;
            while (d != n - 1) {
                x = (x * x) % n;
                d *= 2;
                if (x == n - 1) return true;
                if (x == 1) return false;
            }
            return false;
        };
        static const int bases[] = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};
        for (int a : bases) {
            if (val <= a) break;
            if (!millerRabin(val, a)) return false;
        }
        return true;
    }
    JBigInteger nextProbablePrime() const {
        if (val < 2) return JBigInteger(2);
        JBigInteger p(val + 1);
        if (p.val > 2 && p.val % 2 == 0) p.val++;
        while (!p.isProbablePrime()) {
            p.val += 2;
        }
        return p;
    }
    long long longValue() const { return static_cast<long long>(val); }
    int intValue() const { return static_cast<int>(val); }
    double doubleValue() const { return static_cast<double>(val); }
    JString toString() const {
        if (val == 0) return "0";
        __int128 v = val;
        bool neg = false;
        if (v < 0) { neg = true; v = -v; }
        string s;
        while (v > 0) { s.push_back('0' + (v % 10)); v /= 10; }
        if (neg) s.push_back('-');
        std::reverse(s.begin(), s.end());
        return s;
    }
    friend ostream& operator<<(ostream& os, const JBigInteger& b) {
        os << b.toString();
        return os;
    }
};
inline const JBigInteger JBigInteger::ZERO = JBigInteger(0);
inline const JBigInteger JBigInteger::ONE = JBigInteger(1);
inline const JBigInteger JBigInteger::TWO = JBigInteger(2);
inline const JBigInteger JBigInteger::TEN = JBigInteger(10);
