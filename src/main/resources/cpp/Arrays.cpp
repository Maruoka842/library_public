struct JArray {
    template<class T = JObject>
    static vector<T> newInstance(const std::type_info&, int length) {
        return vector<T>(length);
    }
    template<class T>
    static vector<T> newInstance(const T&, int length) {
        return vector<T>(length);
    }
    template<class T> static int getLength(const vector<T>& v) { return static_cast<int>(v.size()); }
    static int getLength(const JObject& obj) {
        if (!obj.has_value()) return 0;
        try { return static_cast<int>(std::any_cast<vector<int>>(obj).size()); } catch(...) {}
        try { return static_cast<int>(std::any_cast<vector<long long>>(obj).size()); } catch(...) {}
        try { return static_cast<int>(std::any_cast<vector<JObject>>(obj).size()); } catch(...) {}
        return 0;
    }
    template<class T> static auto get(const vector<T>& v, int index) { return v[index]; }
    static JObject get(const JObject& obj, int index) {
        try { return std::any_cast<vector<int>>(obj)[index]; } catch(...) {}
        try { return std::any_cast<vector<long long>>(obj)[index]; } catch(...) {}
        try { return std::any_cast<vector<JObject>>(obj)[index]; } catch(...) {}
        return {};
    }
};
using Array = JArray;

template<class C> auto jtoArray(const C& values) {
    using T = typename C::value_type;
    return vector<T>(values.begin(), values.end());
}
