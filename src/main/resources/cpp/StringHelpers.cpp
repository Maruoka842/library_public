struct JStringTokenizer;
inline bool jisNull(const JStringTokenizer& x);

template<class T, class = void> struct jhas_ostream : false_type {};
template<class T> struct jhas_ostream<T, void_t<decltype(declval<ostream&>() << declval<const T&>())>> : true_type {};

template<class T, class = void> struct jhas_toString : false_type {};
template<class T> struct jhas_toString<T, void_t<decltype(declval<const T&>().toString())>> : true_type {};

template<class T, class = void> struct jhas_ptr_toString : false_type {};
template<class T> struct jhas_ptr_toString<T, void_t<decltype(declval<const T&>()->toString())>> : true_type {};

template<class T> string jstr(const T& x);

// 未テスト
// 計算量: O(|a|)
inline string jstr(const vector<char>& a) {
    return string(a.begin(), a.end());
}

// 未テスト
// 計算量: O(|a| * len(jstr(elem)))
template<class T> string jstr(const vector<T>& a) {
    string s = "[";
    for (size_t i = 0; i < a.size(); ++i) s += (i ? ", " : "") + jstr(a[i]);
    return s + "]";
}

// Java の Double.toString() と同一の変換規則:
//   |x| < 1e-3 または |x| >= 1e7 なら指数表記 (大文字 E, 指数に + なし)、それ以外は小数表記。
//   shortest round-trip 表現 (C++17 std::to_chars の default モード)。
//   整数値の double は ".0" を末尾に付加。
// 計算量: O(1)
inline string jdouble_to_string(double x) {
    if (isnan(x)) return "NaN";
    if (x == numeric_limits<double>::infinity()) return "Infinity";
    if (x == -numeric_limits<double>::infinity()) return "-Infinity";

    double ax = abs(x);
    bool useScientific = (x != 0.0) && (ax >= 1e7 || ax < 1e-3);

    string result;
    char buf[64];

    if (!useScientific) {
        // C++ shortest round-trip via to_chars default (chooses shorter of decimal/scientific)
        auto [ptr, ec] = to_chars(buf, buf + sizeof(buf), x);
        *ptr = '\0';
        string s(buf);
        if (s.find('e') == string::npos) {
            result = s;
        } else {
            // C++ chose scientific but Java wants decimal — find shortest decimal that round-trips
            result.clear();
            for (int p = 1; p <= 17; ++p) {
                ostringstream oss;
                oss << setprecision(p) << x;
                string t = oss.str();
                if (t.find('e') == string::npos && stod(t) == x) {
                    result = t;
                    break;
                }
            }
            if (result.empty()) {
                ostringstream oss;
                oss << setprecision(17) << x;
                result = oss.str();
            }
        }
    } else {
        // Find shortest scientific representation that round-trips
        for (int p = 1; p <= 17; ++p) {
            auto [ptr, ec] = to_chars(buf, buf + sizeof(buf), x, chars_format::scientific, p);
            *ptr = '\0';
            if (stod(buf) == x) {
                result = string(buf);
                break;
            }
        }
        if (result.empty()) {
            auto [ptr, ec] = to_chars(buf, buf + sizeof(buf), x, chars_format::scientific, 17);
            *ptr = '\0';
            result = string(buf);
        }
        // Convert C++ exponent to Java format: e+01 -> E1, e-01 -> E-1
        auto ep = result.find('e');
        if (ep != string::npos) {
            result[ep] = 'E';
            size_t after_e = ep + 1;
            bool neg_exp = (result[after_e] == '-');
            if (result[after_e] == '+' || result[after_e] == '-') after_e++;
            // Remove leading zeros in exponent magnitude
            size_t first_nz = result.find_first_not_of('0', after_e);
            if (first_nz == string::npos) first_nz = result.size() - 1;
            if (first_nz > after_e) result.erase(after_e, first_nz - after_e);
            // Remove '+' sign (keep '-')
            if (!neg_exp && result[ep + 1] == '+') result.erase(ep + 1, 1);
        }
        // Ensure "X.0ENN" form when no decimal point before exponent
        auto ep2 = result.find('E');
        if (ep2 != string::npos && result.find('.') == string::npos) {
            result.insert(ep2, ".0");
        }
    }

    // Append ".0" for integer-valued finite doubles with no decimal point and no exponent
    if (isfinite(x) && result.find('.') == string::npos && result.find('E') == string::npos) {
        result += ".0";
    }
    return result;
}

// 未テスト
// 計算量: O(len(jstr(x)))
template<class T> string jstr(const T& x) {
    if constexpr (is_same_v<decay_t<T>, JObject>) {
        if (!x.has_value()) return "null";
        if (auto p = std::any_cast<int>(&x)) return std::to_string(*p);
        if (auto p = std::any_cast<long long>(&x)) return std::to_string(*p);
        if (auto p = std::any_cast<double>(&x)) return jdouble_to_string(*p);
        if (auto p = std::any_cast<JString>(&x)) return *p;
        return "<object>";
    } else if constexpr (jhas_toString<T>::value) {
        return x.toString();
    } else if constexpr (jhas_ptr_toString<T>::value) {
        return x ? x->toString() : "null";
    } else if constexpr (jhas_ostream<T>::value) {
        if constexpr (is_floating_point_v<decay_t<T>>) {
            return jdouble_to_string(static_cast<double>(x));
        } else {
            ostringstream o;
            o << x;
            return o.str();
        }
    } else {
        return "<object>";
    }
}

template<class A, class B> JString jconcat(const A& a, const B& b) { return jstr(a) + jstr(b); }
inline JString jrepeat(const JString& s, int n) { JString r; while (n-- > 0) r += s; return r; }
