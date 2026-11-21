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


namespace std {
    inline bool operator==(const any& a, const any& b) {
        if (!a.has_value() && !b.has_value()) return true;
        if (!a.has_value() || !b.has_value()) return false;
        return &a == &b;
    }
    inline bool operator!=(const any& a, const any& b) { return !(a == b); }
}

template<class T> bool jisInstance(const JObject& x) {
    return x.has_value() && (x.type() == typeid(T) || std::any_cast<T>(&x) != nullptr);
}
template<class T> bool jisInstance(const JObject& x, T& out) {
    if (x.has_value()) {
        if (auto p = std::any_cast<T>(&x)) { out = *p; return true; }
    }
    return false;
}
// 未テスト
// 計算量: O(1)
template<class T> struct jpointer_element { using type = T; };
template<class T> struct jpointer_element<std::shared_ptr<T>> { using type = T; };
template<class T> using jpointer_element_t = typename jpointer_element<T>::type;

template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x) {
    using Target = jpointer_element_t<T>;
    if constexpr (std::is_base_of_v<Target, U>) {
        return x != nullptr;
    } else if constexpr (std::is_polymorphic_v<U> && (std::is_polymorphic_v<Target> || std::is_base_of_v<U, Target>)) {
        return std::dynamic_pointer_cast<Target>(x) != nullptr;
    } else {
        return false;
    }
}
// 未テスト
// 計算量: O(1)
template<class T, class U> bool jisInstance(const std::shared_ptr<U>&, T&) {
    return false;
}
// 未テスト
// 計算量: O(1)
template<class T, class U> bool jisInstance(const U& x) {
    if constexpr (std::is_same_v<T, U> || std::is_base_of_v<T, U>) return true;
    else return false;
}
// 未テスト
// 計算量: O(1)
template<class T, class U> bool jisInstance(const U& x, T& out) {
    if constexpr (std::is_same_v<T, U> || std::is_convertible_v<U, T>) {
        out = x;
        return true;
    } else {
        return false;
    }
}
// 未テスト
// 計算量: O(1)
template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x, std::shared_ptr<T>& out) {
    if constexpr (std::is_base_of_v<T, U>) {
        if (x != nullptr) { out = std::static_pointer_cast<T>(x); return true; }
        return false;
    } else if constexpr (std::is_polymorphic_v<U> && (std::is_polymorphic_v<T> || std::is_base_of_v<U, T>)) {
        if (auto p = std::dynamic_pointer_cast<T>(x)) { out = p; return true; }
        return false;
    } else {
        return false;
    }
}

// 未テスト
// 計算量: O(1)
template<class Target, class Source>
Target jcast(Source&& src) {
    using CleanTarget = std::decay_t<Target>;
    using CleanSource = std::decay_t<Source>;
    if constexpr (std::is_convertible_v<Source, Target>) {
        return static_cast<Target>(std::forward<Source>(src));
    } else if constexpr (std::is_constructible_v<CleanTarget, Source>) {
        return CleanTarget(std::forward<Source>(src));
    } else {
        return CleanTarget{};
    }
}
inline std::vector<bool>::reference operator^=(std::vector<bool>::reference ref, bool val) { ref = ref ^ val; return ref; }
inline std::vector<bool>::reference operator&=(std::vector<bool>::reference ref, bool val) { ref = ref & val; return ref; }
inline std::vector<bool>::reference operator|=(std::vector<bool>::reference ref, bool val) { ref = ref | val; return ref; }

inline bool operator==(std::vector<bool>::reference a, std::vector<bool>::reference b) { return bool(a) == bool(b); }
inline bool operator!=(std::vector<bool>::reference a, std::vector<bool>::reference b) { return bool(a) != bool(b); }
inline bool operator==(std::vector<bool>::reference a, bool b) { return bool(a) == b; }
inline bool operator!=(std::vector<bool>::reference a, bool b) { return bool(a) != b; }
inline bool operator==(bool a, std::vector<bool>::reference b) { return a == bool(b); }
inline bool operator!=(bool a, std::vector<bool>::reference b) { return a != bool(b); }

template<class T> struct JOptionalVal;

template<class T>
struct NullableVal : public std::optional<T> {
    using std::optional<T>::optional;
    NullableVal() : std::optional<T>(std::nullopt) {}
    NullableVal(const std::optional<T>& o) : std::optional<T>(o) {}
    NullableVal(std::nullptr_t) : std::optional<T>(std::nullopt) {}
    NullableVal& operator=(std::nullptr_t) {
        this->reset();
        return *this;
    }
    operator T&() { return this->value(); }
    operator const T&() const { return this->value(); }
    bool operator==(std::nullptr_t) const { return !this->has_value(); }
    bool operator!=(std::nullptr_t) const { return this->has_value(); }
    bool operator==(const NullableVal<T>& o) const {
        return static_cast<const std::optional<T>&>(*this) == static_cast<const std::optional<T>&>(o);
    }
    bool operator!=(const NullableVal<T>& o) const {
        return !(*this == o);
    }
};

template<class T> struct jis_nullable_val : std::false_type {};
template<class T> struct jis_nullable_val<NullableVal<T>> : std::true_type {};

template<class T> struct jis_optional : std::false_type {};
template<class T> struct jis_optional<std::optional<T>> : std::true_type {};
template<class T> struct jis_optional<JOptionalVal<T>> : std::true_type {};

template<class T> struct jis_std_function : std::false_type {};
template<class R, class... Args> struct jis_std_function<std::function<R(Args...)>> : std::true_type {};

template<class T>
struct Nullable {
    using type = std::conditional_t<std::is_arithmetic_v<T>, NullableVal<T>, T>;
};

// 未テスト
// 計算量: O(1)
template<class T> bool jisNull(const T& x) {
    return false;
}
template<class T> bool jisNull(const NullableVal<T>& x) { return !x.has_value(); }
template<class T> bool jisNull(const std::optional<T>& x) { return !x.has_value(); }
template<class T> bool jisNull(const vector<T>& x) { return x.empty(); }
template<class T> bool jisNull(const shared_ptr<T>& x) { return !x; }
inline bool jisNull(const JObject& x) { return !x.has_value(); }
template<class R, class... Args>
inline bool jisNull(const std::function<R(Args...)>& f) { return !f; }

// 未テスト
// 計算量: O(1)
template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
bool operator==(const T& a, const U& b) {
    if constexpr (is_same_v<decay_t<U>, JObject>) {
        if (!b.has_value()) return jisNull(a);
        return false;
    } else {
        return b == a;
    }
}
template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
bool operator!=(const T& a, const U& b) {
    return !(a == b);
}

template<class K, class V>
struct JEntry {
    std::shared_ptr<std::pair<K, V>> ptr;
    JEntry() : ptr(nullptr) {}
    JEntry(std::nullptr_t) : ptr(nullptr) {}
    JEntry(const std::pair<K, V>& p) : ptr(std::make_shared<std::pair<K, V>>(p)) {}
    JEntry(std::pair<K, V>&& p) : ptr(std::make_shared<std::pair<K, V>>(std::move(p))) {}
    JEntry(const std::pair<const K, V>& p) : ptr(std::make_shared<std::pair<K, V>>(p.first, p.second)) {}
    JEntry(std::shared_ptr<std::pair<K, V>> p) : ptr(std::move(p)) {}

    std::pair<K, V>& operator*() { return *ptr; }
    const std::pair<K, V>& operator*() const { return *ptr; }
    std::pair<K, V>* operator->() { return ptr.get(); }
    const std::pair<K, V>* operator->() const { return ptr.get(); }

    bool operator==(std::nullptr_t) const { return ptr == nullptr; }
    bool operator!=(std::nullptr_t) const { return ptr != nullptr; }
    friend bool operator==(std::nullptr_t, const JEntry& e) { return e.ptr == nullptr; }
    friend bool operator!=(std::nullptr_t, const JEntry& e) { return e.ptr != nullptr; }
    bool operator<(const JEntry& o) const {
        if (!ptr) return (bool)o.ptr;
        if (!o.ptr) return false;
        return ptr->first < o.ptr->first;
    }

    operator bool() const { return ptr != nullptr; }
    operator std::pair<K, V>&() { return *ptr; }
    operator const std::pair<K, V>&() const { return *ptr; }

    const K& getKey() const { return ptr->first; }
    K& getKey() { return ptr->first; }
    const V& getValue() const { return ptr->second; }
    V& getValue() { return ptr->second; }
    void setValue(const V& v) { ptr->second = v; }
};

template<class K, class V> inline bool jisNull(const JEntry<K, V>& e) { return e.ptr == nullptr; }

template<class T, class = void> struct jis_pointer_like : std::false_type {};
template<class T> struct jis_pointer_like<T, std::void_t<decltype(std::declval<T>().operator->())>> : std::true_type {};

// 未テスト
// 計算量: O(1)
template<class T>
decltype(auto) jentryKey(T&& t) {
    if constexpr (jis_pointer_like<std::decay_t<T>>::value) {
        return (t->first);
    } else {
        return (t.first);
    }
}

// 未テスト
// 計算量: O(1)
template<class T>
decltype(auto) jentryValue(T&& t) {
    if constexpr (jis_pointer_like<std::decay_t<T>>::value) {
        return (t->second);
    } else {
        return (t.second);
    }
}

// 未テスト
// 計算量: O(1)
template<class T, class V>
decltype(auto) jentrySetValue(T&& t, V&& v) {
    if constexpr (jis_pointer_like<std::decay_t<T>>::value) {
        return (t->second = std::forward<V>(v));
    } else {
        return (t.second = std::forward<V>(v));
    }
}

// 未テスト
// 計算量: O(N)
template<class Dst, class Src>
void jassign(Dst& dst, Src&& src) {
    if constexpr (std::is_assignable_v<Dst&, Src>) {
        dst = std::forward<Src>(src);
    } else if constexpr (std::is_constructible_v<Dst, Src>) {
        dst = Dst(std::forward<Src>(src));
    } else {
        dst.clear();
        for (const auto& item : src) {
            typename Dst::value_type v;
            jassign(v, item);
            dst.push_back(v);
        }
    }
}

template<class T> struct JOptionalVal {
    std::optional<T> opt;
    JOptionalVal() : opt(std::nullopt) {}
    JOptionalVal(nullptr_t) : opt(std::nullopt) {}
    JOptionalVal(const T& val) : opt(val) {}
    JOptionalVal(const std::optional<T>& o) : opt(o) {}
    bool has_value() const { return opt.has_value(); }
    const T& value() const { return opt.value(); }
    const T& operator*() const { return opt.value(); }
    T& operator*() { return opt.value(); }
    template<class U = T, class = std::enable_if_t<!std::is_same_v<U, bool>>>
    operator U() const { return opt.value_or(U{}); }
    operator bool() const {
        if constexpr (std::is_same_v<T, bool>) {
            return opt.value_or(false);
        } else {
            return opt.has_value();
        }
    }
    friend bool operator==(const JOptionalVal& a, const JOptionalVal& b) { return a.opt == b.opt; }
    friend bool operator!=(const JOptionalVal& a, const JOptionalVal& b) { return a.opt != b.opt; }
    friend bool operator==(const JOptionalVal& a, nullptr_t) { return !a.opt.has_value(); }
    friend bool operator!=(const JOptionalVal& a, nullptr_t) { return a.opt.has_value(); }
    friend bool operator==(nullptr_t, const JOptionalVal& a) { return !a.opt.has_value(); }
    friend bool operator!=(nullptr_t, const JOptionalVal& a) { return a.opt.has_value(); }
    friend ostream& operator<<(ostream& os, const JOptionalVal& a) {
        if (a.opt.has_value()) return os << a.opt.value();
        return os << "null";
    }
};
template<class T> inline bool jisNull(const JOptionalVal<T>& x) { return !x.has_value(); }
