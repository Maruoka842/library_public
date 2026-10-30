struct JComparatorUtils {
    static auto reverseOrder() { return [](const auto& a, const auto& b) { return jcompareTo(b, a); }; }
    static auto naturalOrder() { return [](const auto& a, const auto& b) { return jcompareTo(a, b); }; }
    template<class T> static int compare(const T& a, const T& b) { return jcompareTo(a, b); }
};
using Comparator = JComparatorUtils;
