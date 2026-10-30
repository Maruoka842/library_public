inline bool jstartsWith(const JString& s, const JString& prefix) {
    return s.rfind(prefix, 0) == 0;
}
inline bool jendsWith(const JString& s, const JString& suffix) {
    if (suffix.size() > s.size()) return false;
    return s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
}
// 未テスト
// 計算量: O(|s| * |target|)
inline bool jcontains(const JString& s, const JString& target) {
    return s.find(target) != JString::npos;
}
// 未テスト
// 計算量: O(|s|)
inline bool jmatches(const JString& s, const JString& regex) {
    try {
        return regex_match(s, std::regex(regex));
    } catch (...) {
        return false;
    }
}
inline JString jreplace(JString s, const JString& target, const JString& replacement) {
    if (target.empty()) return s;
    size_t pos = 0;
    while ((pos = s.find(target, pos)) != JString::npos) {
        s.replace(pos, target.length(), replacement);
        pos += replacement.length();
    }
    return s;
}
inline JString jreplace(JString s, char target, char replacement) {
    for (char& c : s) {
        if (c == target) c = replacement;
    }
    return s;
}
inline JString jreplaceAll(JString s, const JString& regex, const JString& replacement) {
    try {
        return regex_replace(s, std::regex(regex), replacement);
    } catch (...) {
        return jreplace(s, regex, replacement);
    }
}
// 未テスト
// 計算量: O(|s|)
inline JString jreplaceFirst(JString s, const JString& regex, const JString& replacement) {
    try {
        return regex_replace(s, std::regex(regex), replacement, std::regex_constants::format_first_only);
    } catch (...) {
        return jreplace(s, regex, replacement);
    }
}
// 未テスト
// 計算量: O(|s|)
inline JString jtrim(const JString& s) {
    size_t start = 0;
    while (start < s.size() && ::isspace(static_cast<unsigned char>(s[start]))) ++start;
    size_t end = s.size();
    while (end > start && ::isspace(static_cast<unsigned char>(s[end - 1]))) --end;
    return s.substr(start, end - start);
}
// 未テスト
// 計算量: O(|s|)
inline JString jstrip(const JString& s) { return jtrim(s); }
// 未テスト
// 計算量: O(|s|)
inline JString jstripLeading(const JString& s) {
    size_t start = 0;
    while (start < s.size() && ::isspace(static_cast<unsigned char>(s[start]))) ++start;
    return s.substr(start);
}
// 未テスト
// 計算量: O(|s|)
inline JString jstripTrailing(const JString& s) {
    size_t end = s.size();
    while (end > 0 && ::isspace(static_cast<unsigned char>(s[end - 1]))) --end;
    return s.substr(0, end);
}
// 未テスト
// 計算量: O(|s|)
inline bool jisBlank(const JString& s) {
    for (char c : s) {
        if (!::isspace(static_cast<unsigned char>(c))) return false;
    }
    return true;
}
// 未テスト
// 計算量: O(sum |elements|)
inline JString jjoin(const JString& delimiter, const vector<JString>& elements) {
    JString res;
    for (size_t i = 0; i < elements.size(); ++i) {
        if (i > 0) res += delimiter;
        res += elements[i];
    }
    return res;
}
// 未テスト
// 計算量: O(sum |args|)
template<class... Args>
inline JString jjoin(const JString& delimiter, const Args&... args) {
    vector<JString> elements{jstr(args)...};
    return jjoin(delimiter, elements);
}
inline vector<char> jchars(const JString& s) { return vector<char>(s.begin(), s.end()); }
inline char jtoUpperCase(char c) { return static_cast<char>(::toupper(static_cast<unsigned char>(c))); }
inline JString jtoUpperCase(JString s) {
    for (char& c : s) c = static_cast<char>(::toupper(static_cast<unsigned char>(c)));
    return s;
}
inline char jtoLowerCase(char c) { return static_cast<char>(::tolower(static_cast<unsigned char>(c))); }
inline JString jtoLowerCase(JString s) {
    for (char& c : s) c = static_cast<char>(::tolower(static_cast<unsigned char>(c)));
    return s;
}
inline bool jequalsIgnoreCase(const JString& a, const JString& b) {
    if (a.size() != b.size()) return false;
    for (size_t i = 0; i < a.size(); ++i) {
        if (::tolower(static_cast<unsigned char>(a[i])) != ::tolower(static_cast<unsigned char>(b[i]))) return false;
    }
    return true;
}
inline int jindexOf(const JString& s, const JString& target, int fromIndex = 0) {
    if (fromIndex < 0) fromIndex = 0;
    if ((size_t)fromIndex > s.length()) return -1;
    auto pos = s.find(target, fromIndex);
    return pos == JString::npos ? -1 : (int)pos;
}
inline int jindexOf(const JString& s, char ch, int fromIndex = 0) {
    if (fromIndex < 0) fromIndex = 0;
    if ((size_t)fromIndex > s.length()) return -1;
    auto pos = s.find(ch, fromIndex);
    return pos == JString::npos ? -1 : (int)pos;
}
inline int jlastIndexOf(const JString& s, const JString& target) {
    auto pos = s.rfind(target);
    return pos == JString::npos ? -1 : (int)pos;
}
inline int jlastIndexOf(const JString& s, const JString& target, int fromIndex) {
    if (fromIndex < 0) return -1;
    auto pos = s.rfind(target, fromIndex);
    return pos == JString::npos ? -1 : (int)pos;
}
inline int jlastIndexOf(const JString& s, char ch) {
    auto pos = s.rfind(ch);
    return pos == JString::npos ? -1 : (int)pos;
}
inline int jlastIndexOf(const JString& s, char ch, int fromIndex) {
    if (fromIndex < 0) return -1;
    auto pos = s.rfind(ch, fromIndex);
    return pos == JString::npos ? -1 : (int)pos;
}
inline JString jsubstring(const JString& s, int begin) { return s.substr(begin); }
inline JString jsubstring(const JString& s, int begin, int end) { return s.substr(begin, max(0, end - begin)); }
inline vector<JString> jsplit(const JString& s, const JString& regex) {
    vector<JString> result;
    std::regex re(regex);
    std::sregex_token_iterator it(s.begin(), s.end(), re, -1), end;
    for (; it != end; ++it) result.push_back(*it);
    return result;
}
