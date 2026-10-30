struct JLong {
    static constexpr long long MAX_VALUE = numeric_limits<long long>::max();
    static constexpr long long MIN_VALUE = numeric_limits<long long>::min();
    static constexpr int BYTES = 8;
    static constexpr int SIZE = 64;
    static int compare(long long a, long long b) { return (a > b) - (a < b); }
    static int hashCode(long long x) { return static_cast<int>(x ^ (x >> 32)); }
    static int signum(long long x) { return (x > 0) - (x < 0); }
    static long long sum(long long a, long long b) { return a + b; }
    static long long max(long long a, long long b) { return std::max(a, b); }
    static long long min(long long a, long long b) { return std::min(a, b); }
    static int bitCount(long long x) { return __builtin_popcountll(static_cast<unsigned long long>(x)); }
    static int numberOfLeadingZeros(long long x) { return x ? __builtin_clzll((unsigned long long)x) : 64; }
    static int numberOfTrailingZeros(long long x) { return x ? __builtin_ctzll((unsigned long long)x) : 64; }
    static long long highestOneBit(long long x) { return x ? (1ULL << (63 - __builtin_clzll(static_cast<unsigned long long>(x)))) : 0LL; }
    static long long lowestOneBit(long long x) { return x & -x; }
    // 未テスト
    // 計算量: O(1)
    static long long rotateLeft(long long i, int distance) {
        uint64_t u = static_cast<uint64_t>(i);
        int dist = distance & 63;
        return static_cast<long long>((u << dist) | (u >> ((64 - dist) & 63)));
    }
    // 未テスト
    // 計算量: O(1)
    static long long rotateRight(long long i, int distance) {
        uint64_t u = static_cast<uint64_t>(i);
        int dist = distance & 63;
        return static_cast<long long>((u >> dist) | (u << ((64 - dist) & 63)));
    }
    // 未テスト
    // 計算量: O(1)
    static long long divideUnsigned(long long dividend, long long divisor) {
        return static_cast<long long>(static_cast<uint64_t>(dividend) / static_cast<uint64_t>(divisor));
    }
    // 未テスト
    // 計算量: O(1)
    static long long remainderUnsigned(long long dividend, long long divisor) {
        return static_cast<long long>(static_cast<uint64_t>(dividend) % static_cast<uint64_t>(divisor));
    }
    // 未テスト
    // 計算量: O(1)
    static int compareUnsigned(long long x, long long y) {
        uint64_t ux = static_cast<uint64_t>(x);
        uint64_t uy = static_cast<uint64_t>(y);
        return (ux > uy) - (ux < uy);
    }
    static long long reverse(long long x) {
        uint64_t u = static_cast<uint64_t>(x);
        u = ((u & 0x5555555555555555ULL) << 1) | ((u & 0xAAAAAAAAAAAAAAAAULL) >> 1);
        u = ((u & 0x3333333333333333ULL) << 2) | ((u & 0xCCCCCCCCCCCCCCCCULL) >> 2);
        u = ((u & 0x0F0F0F0F0F0F0F0FULL) << 4) | ((u & 0xF0F0F0F0F0F0F0F0ULL) >> 4);
        u = ((u & 0x00FF00FF00FF00FFULL) << 8) | ((u & 0xFF00FF00FF00FF00ULL) >> 8);
        u = ((u & 0x0000FFFF0000FFFFULL) << 16) | ((u & 0xFFFF0000FFFF0000ULL) >> 16);
        u = (u << 32) | (u >> 32);
        return static_cast<long long>(u);
    }
    static long long reverseBytes(long long x) {
        return static_cast<long long>(__builtin_bswap64(static_cast<uint64_t>(x)));
    }
    static JString toBinaryString(long long x) {
        if (x == 0) return "0";
        uint64_t u = static_cast<uint64_t>(x);
        string s;
        while (u > 0) {
            s.push_back((u & 1) ? '1' : '0');
            u >>= 1;
        }
        std::reverse(s.begin(), s.end());
        return s;
    }
    static JString toHexString(long long x) {
        if (x == 0) return "0";
        uint64_t u = static_cast<uint64_t>(x);
        stringstream ss;
        ss << std::hex << u;
        return ss.str();
    }
    static JString toOctalString(long long x) {
        if (x == 0) return "0";
        uint64_t u = static_cast<uint64_t>(x);
        stringstream ss;
        ss << std::oct << u;
        return ss.str();
    }
    static JString toUnsignedString(long long x, int radix = 10) {
        if (x == 0) return "0";
        uint64_t u = static_cast<uint64_t>(x);
        if (radix == 10) return std::to_string(u);
        if (radix == 2) return toBinaryString(x);
        if (radix == 16) return toHexString(x);
        if (radix == 8) return toOctalString(x);
        return std::to_string(u);
    }
    static JString toString(long long x, int radix = 10) {
        if (radix == 10) return std::to_string(x);
        if (radix == 2) return toBinaryString(x);
        if (radix == 16) return toHexString(x);
        if (radix == 8) return toOctalString(x);
        return std::to_string(x);
    }
    static long long parseLong(const JString& x, int radix = 10) { return stoll(x, nullptr, radix); }
    // 未テスト
    // 計算量: O(|x|)
    static long long parseUnsignedLong(const JString& x, int radix = 10) { return static_cast<long long>(stoull(x, nullptr, radix)); }
    static long long valueOf(const JString& x, int radix = 10) { return stoll(x, nullptr, radix); }
    static long long valueOf(long long x) { return x; }
};
