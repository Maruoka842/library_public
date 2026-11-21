// 未テスト
struct JBigInteger {
    int sign = 0; // -1, 0, 1
    std::vector<uint32_t> d; // リトルエンディアン (d[0] が最下位 32 ビット)

    void trim() {
        while (!d.empty() && d.back() == 0) d.pop_back();
        if (d.empty()) sign = 0;
    }

    // 未テスト
    // 計算量: O(1)
    JBigInteger() : sign(0) {}

    // 未テスト
    // 計算量: O(1)
    JBigInteger(int v) {
        if (v == 0) { sign = 0; }
        else if (v > 0) { sign = 1; d.push_back(static_cast<uint32_t>(v)); }
        else {
            sign = -1;
            uint32_t uv = static_cast<uint32_t>(-(static_cast<int64_t>(v)));
            d.push_back(uv);
        }
    }

    // 未テスト
    // 計算量: O(1)
    JBigInteger(long long v) {
        if (v == 0) { sign = 0; }
        else {
            unsigned long long uv;
            if (v < 0) {
                sign = -1;
                uv = static_cast<unsigned long long>(-(v + 1)) + 1ULL;
            } else {
                sign = 1;
                uv = static_cast<unsigned long long>(v);
            }
            d.push_back(static_cast<uint32_t>(uv & 0xFFFFFFFFULL));
            if ((uv >> 32) > 0) d.push_back(static_cast<uint32_t>(uv >> 32));
        }
    }

    // 未テスト
    // 計算量: O(1)
    JBigInteger(__int128 v) {
        if (v == 0) { sign = 0; }
        else {
            unsigned __int128 uv;
            if (v < 0) {
                sign = -1;
                uv = static_cast<unsigned __int128>(-(v + 1)) + 1;
            } else {
                sign = 1;
                uv = static_cast<unsigned __int128>(v);
            }
            while (uv > 0) {
                d.push_back(static_cast<uint32_t>(uv & 0xFFFFFFFFULL));
                uv >>= 32;
            }
        }
    }

    // 未テスト
    // 計算量: O(N^2) (N は桁数)
    JBigInteger(const JString& s, int radix = 10) {
        sign = 0;
        if (s.empty()) return;
        size_t start = 0;
        int sgn = 1;
        if (s[0] == '-') { sgn = -1; start = 1; }
        else if (s[0] == '+') { start = 1; }
        for (size_t i = start; i < s.size(); ++i) {
            char c = s[i];
            int digit = -1;
            if (c >= '0' && c <= '9') digit = c - '0';
            else if (c >= 'a' && c <= 'z') digit = c - 'a' + 10;
            else if (c >= 'A' && c <= 'Z') digit = c - 'A' + 10;
            if (digit < 0 || digit >= radix) continue;
            *this = this->multiply(JBigInteger(radix)).add(JBigInteger(digit));
        }
        if (sign != 0 && sgn == -1) sign = -1;
    }

    // 未テスト
    // 計算量: O(1)
    static JBigInteger valueOf(long long v) { return JBigInteger(v); }

    // 未テスト
    // 計算量: O(bitLength)
    template<class R> static JBigInteger probablePrime(int bitLength, R& rnd) {
        if (bitLength < 2) return JBigInteger(2);
        while (true) {
            JBigInteger cand;
            int words = (bitLength + 31) / 32;
            cand.d.resize(words);
            cand.sign = 1;
            for (int i = 0; i < words; ++i) cand.d[i] = static_cast<uint32_t>(rnd());
            int rem = bitLength % 32;
            if (rem != 0) {
                cand.d.back() &= (1U << rem) - 1;
            }
            cand.setBit(bitLength - 1);
            cand.setBit(0);
            cand.trim();
            if (cand.isProbablePrime(10)) return cand;
        }
    }

    static const JBigInteger ZERO;
    static const JBigInteger ONE;
    static const JBigInteger TWO;
    static const JBigInteger TEN;

    // 非負絶対値の比較: 1: |*this| > |o|, 0: 等しい, -1: |*this| < |o|
    int compareAbs(const JBigInteger& o) const {
        if (d.size() != o.d.size()) return d.size() > o.d.size() ? 1 : -1;
        for (int i = static_cast<int>(d.size()) - 1; i >= 0; --i) {
            if (d[i] != o.d[i]) return d[i] > o.d[i] ? 1 : -1;
        }
        return 0;
    }

    static JBigInteger addAbs(const JBigInteger& a, const JBigInteger& b) {
        JBigInteger r;
        r.sign = 1;
        size_t n = std::max(a.d.size(), b.d.size());
        r.d.reserve(n + 1);
        uint64_t carry = 0;
        for (size_t i = 0; i < n || carry; ++i) {
            uint64_t sum = carry;
            if (i < a.d.size()) sum += a.d[i];
            if (i < b.d.size()) sum += b.d[i];
            r.d.push_back(static_cast<uint32_t>(sum & 0xFFFFFFFFULL));
            carry = sum >> 32;
        }
        r.trim();
        return r;
    }

    // 前提: |a| >= |b|
    static JBigInteger subAbs(const JBigInteger& a, const JBigInteger& b) {
        JBigInteger r;
        r.sign = 1;
        r.d.reserve(a.d.size());
        int64_t borrow = 0;
        for (size_t i = 0; i < a.d.size(); ++i) {
            int64_t diff = static_cast<int64_t>(a.d[i]) - borrow - (i < b.d.size() ? static_cast<int64_t>(b.d[i]) : 0);
            if (diff < 0) { diff += 0x100000000LL; borrow = 1; }
            else { borrow = 0; }
            r.d.push_back(static_cast<uint32_t>(diff));
        }
        r.trim();
        return r;
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger add(const JBigInteger& o) const {
        if (sign == 0) return o;
        if (o.sign == 0) return *this;
        if (sign == o.sign) {
            JBigInteger res = addAbs(*this, o);
            res.sign = sign;
            return res;
        }
        int cmp = compareAbs(o);
        if (cmp == 0) return JBigInteger(0);
        if (cmp > 0) {
            JBigInteger res = subAbs(*this, o);
            res.sign = sign;
            return res;
        } else {
            JBigInteger res = subAbs(o, *this);
            res.sign = o.sign;
            return res;
        }
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger subtract(const JBigInteger& o) const {
        return add(o.negate());
    }

    // 未テスト
    // 計算量: O(N * M)
    JBigInteger multiply(const JBigInteger& o) const {
        if (sign == 0 || o.sign == 0) return JBigInteger(0);
        JBigInteger r;
        r.sign = (sign == o.sign) ? 1 : -1;
        r.d.assign(d.size() + o.d.size(), 0);
        for (size_t i = 0; i < d.size(); ++i) {
            uint64_t carry = 0;
            for (size_t j = 0; j < o.d.size(); ++j) {
                uint64_t cur = static_cast<uint64_t>(r.d[i + j]) + static_cast<uint64_t>(d[i]) * o.d[j] + carry;
                r.d[i + j] = static_cast<uint32_t>(cur & 0xFFFFFFFFULL);
                carry = cur >> 32;
            }
            size_t k = i + o.d.size();
            while (carry) {
                uint64_t cur = static_cast<uint64_t>(r.d[k]) + carry;
                r.d[k] = static_cast<uint32_t>(cur & 0xFFFFFFFFULL);
                carry = cur >> 32;
                ++k;
            }
        }
        r.trim();
        return r;
    }

    // Knuth Algorithm D による除算
    static void divModAbs(const JBigInteger& u_in, const JBigInteger& v_in, JBigInteger& q, JBigInteger& r) {
        if (v_in.sign == 0) throw std::domain_error("division by zero");
        int cmp = u_in.compareAbs(v_in);
        if (cmp < 0) {
            q = JBigInteger(0);
            r = u_in;
            r.sign = (r.d.empty() ? 0 : 1);
            return;
        }
        if (cmp == 0) {
            q = JBigInteger(1);
            r = JBigInteger(0);
            return;
        }
        if (v_in.d.size() == 1) {
            q.sign = 1;
            q.d.assign(u_in.d.size(), 0);
            uint64_t rem = 0;
            uint32_t divisor = v_in.d[0];
            for (int i = static_cast<int>(u_in.d.size()) - 1; i >= 0; --i) {
                uint64_t cur = (rem << 32) | u_in.d[i];
                q.d[i] = static_cast<uint32_t>(cur / divisor);
                rem = cur % divisor;
            }
            q.trim();
            if (rem > 0) {
                r.sign = 1;
                r.d = {static_cast<uint32_t>(rem)};
            } else {
                r = JBigInteger(0);
            }
            return;
        }

        int n = static_cast<int>(v_in.d.size());
        int m = static_cast<int>(u_in.d.size()) - n;
        int s = __builtin_clz(v_in.d.back());

        auto shiftLeftWords = [](const std::vector<uint32_t>& a, int shift) {
            if (shift == 0) return a;
            std::vector<uint32_t> res(a.size() + 1, 0);
            uint32_t carry = 0;
            for (size_t i = 0; i < a.size(); ++i) {
                uint64_t cur = (static_cast<uint64_t>(a[i]) << shift) | carry;
                res[i] = static_cast<uint32_t>(cur & 0xFFFFFFFFULL);
                carry = static_cast<uint32_t>(cur >> 32);
            }
            res[a.size()] = carry;
            while (!res.empty() && res.back() == 0) res.pop_back();
            return res;
        };

        std::vector<uint32_t> u = shiftLeftWords(u_in.d, s);
        std::vector<uint32_t> v = shiftLeftWords(v_in.d, s);
        while (static_cast<int>(u.size()) < m + n + 1) u.push_back(0);
        while (static_cast<int>(v.size()) < n) v.push_back(0);

        q.sign = 1;
        q.d.assign(m + 1, 0);

        uint64_t vn1 = v[n - 1];
        uint64_t vn2 = v[n - 2];

        std::vector<uint32_t> prod(n + 1, 0);

        for (int j = m; j >= 0; --j) {
            uint64_t uj = (static_cast<uint64_t>(u[j + n]) << 32) | u[j + n - 1];
            uint64_t qhat = uj / vn1;
            uint64_t rhat = uj % vn1;
            while (qhat >= 0x100000000ULL || (qhat * vn2 > ((rhat << 32) | u[j + n - 2]))) {
                --qhat;
                rhat += vn1;
                if (rhat >= 0x100000000ULL) break;
            }

            uint64_t carry = 0;
            for (int i = 0; i < n; ++i) {
                uint64_t cur = carry + qhat * static_cast<uint64_t>(v[i]);
                prod[i] = static_cast<uint32_t>(cur & 0xFFFFFFFFULL);
                carry = cur >> 32;
            }
            prod[n] = static_cast<uint32_t>(carry);

            while (true) {
                bool smaller = false;
                for (int i = n; i >= 0; --i) {
                    if (u[j + i] != prod[i]) {
                        smaller = (u[j + i] < prod[i]);
                        break;
                    }
                }
                if (!smaller) break;
                --qhat;
                int64_t borrow = 0;
                for (int i = 0; i < n; ++i) {
                    int64_t diff = static_cast<int64_t>(prod[i]) - static_cast<int64_t>(v[i]) - borrow;
                    if (diff < 0) { diff += 0x100000000LL; borrow = 1; }
                    else borrow = 0;
                    prod[i] = static_cast<uint32_t>(diff);
                }
                prod[n] = static_cast<uint32_t>(static_cast<int64_t>(prod[n]) - borrow);
            }

            int64_t borrow = 0;
            for (int i = 0; i <= n; ++i) {
                int64_t diff = static_cast<int64_t>(u[j + i]) - static_cast<int64_t>(prod[i]) - borrow;
                if (diff < 0) { diff += 0x100000000LL; borrow = 1; }
                else borrow = 0;
                u[j + i] = static_cast<uint32_t>(diff);
            }
            q.d[j] = static_cast<uint32_t>(qhat);
        }
        q.trim();

        u.resize(n);
        while (!u.empty() && u.back() == 0) u.pop_back();

        auto shiftRightWords = [](const std::vector<uint32_t>& a, int shift) {
            if (shift == 0) return a;
            std::vector<uint32_t> res(a.size(), 0);
            for (size_t i = 0; i < a.size(); ++i) {
                uint32_t cur = a[i] >> shift;
                if (i + 1 < a.size()) cur |= (a[i + 1] << (32 - shift));
                res[i] = cur;
            }
            while (!res.empty() && res.back() == 0) res.pop_back();
            return res;
        };

        r.d = shiftRightWords(u, s);
        r.sign = r.d.empty() ? 0 : 1;
    }

    // 未テスト
    // 計算量: O(N * M)
    JBigInteger divide(const JBigInteger& o) const {
        JBigInteger q, r;
        divModAbs(*this, o, q, r);
        if (q.sign != 0) q.sign = (sign == o.sign) ? 1 : -1;
        return q;
    }

    // 未テスト
    // 計算量: O(N * M)
    JBigInteger remainder(const JBigInteger& o) const {
        JBigInteger q, r;
        divModAbs(*this, o, q, r);
        if (r.sign != 0) r.sign = sign;
        return r;
    }

    // 未テスト
    // 計算量: O(N * M)
    vector<JBigInteger> divideAndRemainder(const JBigInteger& o) const {
        JBigInteger q, r;
        divModAbs(*this, o, q, r);
        if (q.sign != 0) q.sign = (sign == o.sign) ? 1 : -1;
        if (r.sign != 0) r.sign = sign;
        return {q, r};
    }

    // 未テスト
    // 計算量: O(N * M)
    JBigInteger mod(const JBigInteger& o) const {
        JBigInteger r = remainder(o);
        if (r.sign < 0) r = r.add(o.abs());
        return r;
    }

    // 未テスト
    // 計算量: O(log(exp) * M^2)
    JBigInteger modPow(const JBigInteger& exponent, const JBigInteger& m) const {
        if (exponent.sign < 0) throw std::invalid_argument("negative exponent");
        if (m.abs().compareTo(ONE) <= 0) return JBigInteger(0);
        JBigInteger res(1), base = this->mod(m);
        JBigInteger exp = exponent;
        while (exp.sign > 0) {
            if (exp.testBit(0)) res = res.multiply(base).mod(m);
            base = base.multiply(base).mod(m);
            exp = exp.shiftRight(1);
        }
        return res;
    }

    // 未テスト
    // 計算量: O(N^2)
    JBigInteger modInverse(const JBigInteger& m) const {
        JBigInteger a = this->mod(m);
        JBigInteger b = m;
        JBigInteger u = 1, v = 0;
        while (b.sign != 0) {
            auto qr = a.divideAndRemainder(b);
            a = b; b = qr[1];
            JBigInteger next_u = u.subtract(qr[0].multiply(v));
            u = v; v = next_u;
        }
        if (!a.equals(ONE)) throw std::domain_error("not invertible");
        if (u.sign < 0) u = u.add(m);
        return u;
    }

    // 未テスト
    // 計算量: O(N)
    JBigInteger min(const JBigInteger& o) const { return compareTo(o) <= 0 ? *this : o; }
    // 未テスト
    // 計算量: O(N)
    JBigInteger max(const JBigInteger& o) const { return compareTo(o) >= 0 ? *this : o; }

    // 未テスト
    // 計算量: O(exp * N^2)
    JBigInteger pow(int exp) const {
        if (exp < 0) throw std::invalid_argument("negative exponent");
        JBigInteger res(1), base = *this;
        while (exp > 0) {
            if (exp & 1) res = res.multiply(base);
            if (exp > 1) base = base.multiply(base);
            exp >>= 1;
        }
        return res;
    }

    // 未テスト
    // 計算量: O(N + n / 32)
    JBigInteger shiftLeft(int n) const {
        if (sign == 0 || n == 0) return *this;
        if (n < 0) return shiftRight(-n);
        int word_shift = n / 32;
        int bit_shift = n % 32;
        JBigInteger r;
        r.sign = sign;
        r.d.resize(d.size() + word_shift + 1, 0);
        uint32_t carry = 0;
        for (size_t i = 0; i < d.size(); ++i) {
            uint64_t cur = (static_cast<uint64_t>(d[i]) << bit_shift) | carry;
            r.d[i + word_shift] = static_cast<uint32_t>(cur & 0xFFFFFFFFULL);
            carry = static_cast<uint32_t>(cur >> 32);
        }
        if (carry) r.d[d.size() + word_shift] = carry;
        r.trim();
        return r;
    }

    // 未テスト
    // 計算量: O(N)
    JBigInteger shiftRight(int n) const {
        if (sign == 0 || n == 0) return *this;
        if (n < 0) return shiftLeft(-n);
        if (sign < 0) {
            // Java 仕様: 負数の算術右シフトは floor(val / 2^n) = -((-val - 1) >> n) - 1
            return this->negate().subtract(ONE).shiftRight(n).add(ONE).negate();
        }
        int word_shift = n / 32;
        int bit_shift = n % 32;
        if (word_shift >= static_cast<int>(d.size())) return JBigInteger(0);
        JBigInteger r;
        r.sign = 1;
        r.d.resize(d.size() - word_shift, 0);
        for (size_t i = 0; i < r.d.size(); ++i) {
            uint32_t cur = d[i + word_shift] >> bit_shift;
            if (bit_shift > 0 && i + word_shift + 1 < d.size()) {
                cur |= (d[i + word_shift + 1] << (32 - bit_shift));
            }
            r.d[i] = cur;
        }
        r.trim();
        return r;
    }

    // 未テスト
    // 計算量: O(1)
    bool testBit(int n) const {
        if (n < 0) throw std::invalid_argument("negative bit index");
        if (sign >= 0) {
            int word = n / 32;
            int bit = n % 32;
            if (word >= static_cast<int>(d.size())) return false;
            return (d[word] & (1U << bit)) != 0;
        } else {
            // 負数の場合 2の補数: ~(-val - 1)
            JBigInteger pos = this->negate().subtract(ONE);
            return !pos.testBit(n);
        }
    }

    // 2の補数表現変換ヘルパー
    std::vector<uint32_t> toTwoComplement(size_t words) const {
        std::vector<uint32_t> res(words, 0U);
        for (size_t i = 0; i < std::min(words, d.size()); ++i) res[i] = d[i];
        if (sign < 0) {
            uint64_t carry = 1;
            for (size_t i = 0; i < words; ++i) {
                uint64_t val = static_cast<uint64_t>(~res[i]) + carry;
                res[i] = static_cast<uint32_t>(val & 0xFFFFFFFFULL);
                carry = val >> 32;
            }
        }
        return res;
    }

    static JBigInteger fromTwoComplement(const std::vector<uint32_t>& words) {
        if (words.empty()) return JBigInteger(0);
        bool is_neg = (words.back() & 0x80000000U) != 0;
        JBigInteger r;
        if (!is_neg) {
            r.sign = 1;
            r.d = words;
            r.trim();
            return r;
        }
        r.sign = -1;
        r.d.resize(words.size());
        uint64_t carry = 1;
        for (size_t i = 0; i < words.size(); ++i) {
            uint64_t val = static_cast<uint64_t>(~words[i]) + carry;
            r.d[i] = static_cast<uint32_t>(val & 0xFFFFFFFFULL);
            carry = val >> 32;
        }
        r.trim();
        return r;
    }

    // 未テスト
    // 計算量: O(max(N, n/32))
    JBigInteger setBit(int n) const {
        return or_(ONE.shiftLeft(n));
    }

    // 未テスト
    // 計算量: O(max(N, n/32))
    JBigInteger clearBit(int n) const {
        return andNot(ONE.shiftLeft(n));
    }

    // 未テスト
    // 計算量: O(max(N, n/32))
    JBigInteger flipBit(int n) const {
        return xor_(ONE.shiftLeft(n));
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger or_(const JBigInteger& o) const {
        size_t words = std::max(d.size(), o.d.size()) + 2;
        auto a = toTwoComplement(words);
        auto b = o.toTwoComplement(words);
        for (size_t i = 0; i < words; ++i) a[i] |= b[i];
        return fromTwoComplement(a);
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger and_(const JBigInteger& o) const {
        size_t words = std::max(d.size(), o.d.size()) + 2;
        auto a = toTwoComplement(words);
        auto b = o.toTwoComplement(words);
        for (size_t i = 0; i < words; ++i) a[i] &= b[i];
        return fromTwoComplement(a);
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger xor_(const JBigInteger& o) const {
        size_t words = std::max(d.size(), o.d.size()) + 2;
        auto a = toTwoComplement(words);
        auto b = o.toTwoComplement(words);
        for (size_t i = 0; i < words; ++i) a[i] ^= b[i];
        return fromTwoComplement(a);
    }

    // 未テスト
    // 計算量: O(N)
    JBigInteger not_() const {
        return this->add(ONE).negate();
    }

    // 未テスト
    // 計算量: O(max(N, M))
    JBigInteger andNot(const JBigInteger& o) const {
        return and_(o.not_());
    }

    // 未テスト
    // 計算量: O(1)
    JBigInteger abs() const {
        JBigInteger r = *this;
        if (r.sign < 0) r.sign = 1;
        return r;
    }

    // 未テスト
    // 計算量: O(1)
    JBigInteger negate() const {
        JBigInteger r = *this;
        r.sign = -r.sign;
        return r;
    }

    // 未テスト
    // 計算量: O(N^2)
    JBigInteger gcd(const JBigInteger& o) const {
        JBigInteger a = this->abs(), b = o.abs();
        while (b.sign != 0) {
            a = a.remainder(b);
            std::swap(a, b);
        }
        return a;
    }

    // 未テスト
    // 計算量: O(N)
    int bitCount() const {
        if (sign >= 0) {
            int cnt = 0;
            for (uint32_t w : d) cnt += __builtin_popcount(w);
            return cnt;
        } else {
            // Java: 負数は 2の補数表現で 0 であるビットの数
            return not_().bitCount();
        }
    }

    // 未テスト
    // 計算量: O(N)
    int bitLength() const {
        if (sign == 0) return 0;
        if (sign > 0) {
            int len = (static_cast<int>(d.size()) - 1) * 32;
            len += 32 - __builtin_clz(d.back());
            return len;
        } else {
            return this->negate().subtract(ONE).bitLength();
        }
    }

    // 未テスト
    // 計算量: O(1)
    int signum() const { return sign; }

    // 未テスト
    // 計算量: O(N)
    int compareTo(const JBigInteger& o) const {
        if (sign != o.sign) return sign > o.sign ? 1 : -1;
        if (sign == 0) return 0;
        int cmp = compareAbs(o);
        return sign > 0 ? cmp : -cmp;
    }

    // 未テスト
    // 計算量: O(N)
    bool equals(const JBigInteger& o) const {
        return sign == o.sign && d == o.d;
    }

    // 未テスト
    // 計算量: O(k * N^3)
    bool isProbablePrime(int certainty = 10) const {
        if (sign <= 0) return false;
        if (compareTo(ONE) <= 0) return false;
        if (compareTo(TWO) == 0 || compareTo(JBigInteger(3)) == 0) return true;
        if (!testBit(0)) return false;

        JBigInteger d_odd = this->subtract(ONE);
        int s = 0;
        while (!d_odd.testBit(0)) {
            d_odd = d_odd.shiftRight(1);
            s++;
        }

        static const int bases[] = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};
        for (int a : bases) {
            JBigInteger a_val(a);
            if (this->compareTo(a_val) <= 0) break;
            JBigInteger x = a_val.modPow(d_odd, *this);
            if (x.equals(ONE) || x.equals(this->subtract(ONE))) continue;
            bool composite = true;
            for (int r = 1; r < s; ++r) {
                x = x.multiply(x).mod(*this);
                if (x.equals(this->subtract(ONE))) {
                    composite = false;
                    break;
                }
            }
            if (composite) return false;
        }
        return true;
    }

    // 未テスト
    // 計算量: O(N^3 * log(N))
    JBigInteger nextProbablePrime() const {
        if (compareTo(TWO) < 0) return TWO;
        JBigInteger p = this->add(ONE);
        if (!p.testBit(0)) p = p.add(ONE);
        while (!p.isProbablePrime()) {
            p = p.add(TWO);
        }
        return p;
    }

    // 未テスト
    // 計算量: O(1)
    long long longValue() const {
        if (sign == 0) return 0;
        uint64_t uv = d[0];
        if (d.size() > 1) uv |= (static_cast<uint64_t>(d[1]) << 32);
        return sign < 0 ? -static_cast<long long>(uv) : static_cast<long long>(uv);
    }

    // 未テスト
    // 計算量: O(1)
    long long longValueExact() const {
        return longValue();
    }

    // 未テスト
    // 計算量: O(1)
    int intValue() const {
        if (sign == 0) return 0;
        uint32_t uv = d[0];
        return sign < 0 ? -static_cast<int>(uv) : static_cast<int>(uv);
    }

    // 未テスト
    // 計算量: O(1)
    int intValueExact() const {
        return intValue();
    }

    // 未テスト
    // 計算量: O(N)
    double doubleValue() const {
        if (sign == 0) return 0.0;
        double res = 0.0;
        for (int i = static_cast<int>(d.size()) - 1; i >= 0; --i) {
            res = res * 4294967296.0 + d[i];
        }
        return sign < 0 ? -res : res;
    }

    // 未テスト
    // 計算量: O(N^2)
    JString toString(int radix = 10) const {
        if (sign == 0) return "0";
        JBigInteger temp = this->abs();
        string s;
        static const char digits[] = "0123456789abcdefghijklmnopqrstuvwxyz";
        while (temp.sign > 0) {
            auto qr = temp.divideAndRemainder(JBigInteger(radix));
            s.push_back(digits[qr[1].intValue()]);
            temp = qr[0];
        }
        if (sign < 0) s.push_back('-');
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
