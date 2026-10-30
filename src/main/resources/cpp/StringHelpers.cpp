struct JStringTokenizer;
inline bool jisNull(const JStringTokenizer& x);
inline string jstr(const JObject&) { return "<object>"; }
template<class T> string jstr(const T& x) { ostringstream o; o << x; return o.str(); }
template<class T> string jstr(const vector<T>& a) {
    string s = "[";
    for (size_t i = 0; i < a.size(); ++i) s += (i ? ", " : "") + jstr(a[i]);
    return s + "]";
}
template<class A, class B> JString jconcat(const A& a, const B& b) { return jstr(a) + jstr(b); }
inline JString jrepeat(const JString& s, int n) { JString r; while (n-- > 0) r += s; return r; }
