struct JInteger {
    static constexpr int MAX_VALUE = numeric_limits<int>::max();
    static constexpr int MIN_VALUE = numeric_limits<int>::min();
    static constexpr int BYTES = 4;
    static constexpr int SIZE = 32;
    static int compare(int a, int b) { return (a > b) - (a < b); }
    static int hashCode(int x) { return x; }
    static int signum(int x) { return (x > 0) - (x < 0); }
    static int sum(int a, int b) { return a + b; }
    static int max(int a, int b) { return std::max(a, b); }
    static int min(int a, int b) { return std::min(a, b); }
    static int bitCount(int x) { return __builtin_popcount((unsigned int)x); }
    static int numberOfLeadingZeros(int x) { return x ? __builtin_clz((unsigned int)x) : 32; }
    static int numberOfTrailingZeros(int x) { return x ? __builtin_ctz((unsigned int)x) : 32; }
    static int highestOneBit(int x) { return x ? int(1U << (31 - __builtin_clz((unsigned int)x))) : 0; }
    static int lowestOneBit(int x) { return x & -x; }
    // 未テスト
    // 計算量: O(1)
    static int rotateLeft(int i, int distance) {
        uint32_t u = static_cast<uint32_t>(i);
        int dist = distance & 31;
        return static_cast<int>((u << dist) | (u >> ((32 - dist) & 31)));
    }
    // 未テスト
    // 計算量: O(1)
    static int rotateRight(int i, int distance) {
        uint32_t u = static_cast<uint32_t>(i);
        int dist = distance & 31;
        return static_cast<int>((u >> dist) | (u << ((32 - dist) & 31)));
    }
    // 未テスト
    // 計算量: O(1)
    static int divideUnsigned(int dividend, int divisor) {
        return static_cast<int>(static_cast<uint32_t>(dividend) / static_cast<uint32_t>(divisor));
    }
    // 未テスト
    // 計算量: O(1)
    static int remainderUnsigned(int dividend, int divisor) {
        return static_cast<int>(static_cast<uint32_t>(dividend) % static_cast<uint32_t>(divisor));
    }
    // 未テスト
    // 計算量: O(1)
    static int compareUnsigned(int x, int y) {
        uint32_t ux = static_cast<uint32_t>(x);
        uint32_t uy = static_cast<uint32_t>(y);
        return (ux > uy) - (ux < uy);
    }
    // 未テスト
    // 計算量: O(1)
    static long long toUnsignedLong(int x) {
        return static_cast<long long>(static_cast<uint32_t>(x));
    }
    static int reverse(int x) {
        uint32_t u = static_cast<uint32_t>(x);
        u = ((u & 0x55555555U) << 1) | ((u & 0xAAAAAAAAU) >> 1);
        u = ((u & 0x33333333U) << 2) | ((u & 0xCCCCCCCCU) >> 2);
        u = ((u & 0x0F0F0F0FU) << 4) | ((u & 0xF0F0F0F0U) >> 4);
        u = ((u & 0x00FF00FFU) << 8) | ((u & 0xFF00FF00U) >> 8);
        u = (u << 16) | (u >> 16);
        return static_cast<int>(u);
    }
    static int reverseBytes(int x) {
        return static_cast<int>(__builtin_bswap32(static_cast<uint32_t>(x)));
    }
    static JString toBinaryString(int x) {
        if (x == 0) return "0";
        uint32_t u = static_cast<uint32_t>(x);
        string s;
        while (u > 0) {
            s.push_back((u & 1) ? '1' : '0');
            u >>= 1;
        }
        std::reverse(s.begin(), s.end());
        return s;
    }
    static JString toHexString(int x) {
        if (x == 0) return "0";
        uint32_t u = static_cast<uint32_t>(x);
        stringstream ss;
        ss << std::hex << u;
        return ss.str();
    }
    static JString toOctalString(int x) {
        if (x == 0) return "0";
        uint32_t u = static_cast<uint32_t>(x);
        stringstream ss;
        ss << std::oct << u;
        return ss.str();
    }
    // 未テスト
    // 計算量: O(log_radix |x|)
    static JString toUnsignedString(int x, int radix = 10) {
        uint32_t u = static_cast<uint32_t>(x);
        if (u == 0) return "0";
        string s;
        while (u > 0) {
            int rem = u % radix;
            s.push_back(rem < 10 ? '0' + rem : 'a' + (rem - 10));
            u /= radix;
        }
        std::reverse(s.begin(), s.end());
        return s;
    }
    static JString toString(int x, int radix = 10) {
        if (radix == 10) return std::to_string(x);
        if (radix == 2) return toBinaryString(x);
        if (radix == 16) return toHexString(x);
        if (radix == 8) return toOctalString(x);
        return std::to_string(x);
    }
    static int parseInt(const JString& x, int radix = 10) { return stoi(x, nullptr, radix); }
    // 未テスト
    // 計算量: O(|x|)
    static int parseUnsignedInt(const JString& x, int radix = 10) { return static_cast<int>(stoul(x, nullptr, radix)); }
    static int valueOf(const JString& x, int radix = 10) { return stoi(x, nullptr, radix); }
    static int valueOf(int x) { return x; }
};
