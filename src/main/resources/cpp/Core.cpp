#include <bits/stdc++.h>
using namespace std;

template<class T, class = void> struct jhas_key : false_type {};
template<class T> struct jhas_key<T, void_t<decltype(declval<T>()->key)>> : true_type {};
template<class T> struct jhas_key_obj : false_type {};
template<class T> struct jhas_key<T, void_t<decltype(declval<T>().key)>> : true_type {};

using JString = std::string;
using JObject = std::any;

struct JClass {
    const std::type_info* info = &typeid(void);
    JClass() = default;
    JClass(const std::type_info& ti) : info(&ti) {}
    bool operator==(const JClass& o) const { return info && o.info && *info == *o.info; }
    bool operator!=(const JClass& o) const { return !(*this == o); }
};

struct JNumber {
    long long l_val = 0;
    double d_val = 0.0;
    bool is_fp = false;

    JNumber() = default;
    JNumber(long long v) : l_val(v), d_val(static_cast<double>(v)), is_fp(false) {}
    JNumber(int v) : l_val(v), d_val(v), is_fp(false) {}
    JNumber(double v) : l_val(static_cast<long long>(v)), d_val(v), is_fp(true) {}
    JNumber(float v) : l_val(static_cast<long long>(v)), d_val(v), is_fp(true) {}
    JNumber(int8_t v) : l_val(v), d_val(v), is_fp(false) {}
    JNumber(int16_t v) : l_val(v), d_val(v), is_fp(false) {}
    JNumber(const JObject& obj) {
        if (!obj.has_value()) { l_val = 0; d_val = 0; is_fp = false; }
        else if (auto p = std::any_cast<int>(&obj)) { l_val = *p; d_val = *p; is_fp = false; }
        else if (auto p = std::any_cast<long long>(&obj)) { l_val = *p; d_val = *p; is_fp = false; }
        else if (auto p = std::any_cast<double>(&obj)) { d_val = *p; l_val = static_cast<long long>(*p); is_fp = true; }
        else if (auto p = std::any_cast<float>(&obj)) { d_val = *p; l_val = static_cast<long long>(*p); is_fp = true; }
    }

    long long longValue() const { return is_fp ? static_cast<long long>(d_val) : l_val; }
    int intValue() const { return static_cast<int>(longValue()); }
    double doubleValue() const { return is_fp ? d_val : static_cast<double>(l_val); }
    float floatValue() const { return static_cast<float>(doubleValue()); }
    int8_t byteValue() const { return static_cast<int8_t>(longValue()); }
    int16_t shortValue() const { return static_cast<int16_t>(longValue()); }

    bool operator==(const JNumber& o) const {
        if (is_fp || o.is_fp) return doubleValue() == o.doubleValue();
        return longValue() == o.longValue();
    }
    bool operator!=(const JNumber& o) const { return !(*this == o); }
    bool operator<(const JNumber& o) const {
        if (is_fp || o.is_fp) return doubleValue() < o.doubleValue();
        return longValue() < o.longValue();
    }
    friend ostream& operator<<(ostream& os, const JNumber& n) {
        if (n.is_fp) os << n.d_val;
        else os << n.l_val;
        return os;
    }
};

template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
bool operator==(const T& a, const U& b) {
    if constexpr (is_same_v<decay_t<U>, JObject>) return !b.has_value();
    else return !a.has_value();
}
template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
bool operator!=(const T& a, const U& b) {
    return !(a == b);
}
inline bool operator==(const JObject& a, const JObject& b) {
    if (!a.has_value() && !b.has_value()) return true;
    if (!a.has_value() || !b.has_value()) return false;
    return &a == &b;
}
inline bool operator!=(const JObject& a, const JObject& b) { return !(a == b); }

template<class T> bool jisInstance(const JObject& x) {
    return x.has_value() && (x.type() == typeid(T) || std::any_cast<T>(&x) != nullptr);
}
template<class T> bool jisInstance(const JObject& x, T& out) {
    if (x.has_value()) {
        if (auto p = std::any_cast<T>(&x)) { out = *p; return true; }
    }
    return false;
}
template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x) {
    if constexpr (std::is_base_of_v<T, U>) return x != nullptr;
    else return std::dynamic_pointer_cast<T>(x) != nullptr;
}
template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x, std::shared_ptr<T>& out) {
    if (auto p = std::dynamic_pointer_cast<T>(x)) { out = p; return true; }
    return false;
}
inline std::vector<bool>::reference operator^=(std::vector<bool>::reference ref, bool val) { ref = ref ^ val; return ref; }
inline std::vector<bool>::reference operator&=(std::vector<bool>::reference ref, bool val) { ref = ref & val; return ref; }
inline std::vector<bool>::reference operator|=(std::vector<bool>::reference ref, bool val) { ref = ref | val; return ref; }

template<class T> bool jisNull(const T&) { return false; }
template<class T> bool jisNull(const vector<T>& x) { return x.empty(); }
template<class T> bool jisNull(const shared_ptr<T>& x) { return !x; }
inline bool jisNull(const JObject& x) { return !x.has_value(); }
