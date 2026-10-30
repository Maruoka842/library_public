struct JBoolean {
    static constexpr bool TRUE = true;
    static constexpr bool FALSE = false;
    static bool logicalAnd(bool a, bool b) { return a && b; }
    static bool logicalOr(bool a, bool b) { return a || b; }
    static bool logicalXor(bool a, bool b) { return a ^ b; }
    static bool parseBoolean(const JString& s) { return s == "true"; }
    static bool valueOf(bool b) { return b; }
    static bool valueOf(const JString& s) { return parseBoolean(s); }
    static int compare(bool a, bool b) { return (a > b) - (a < b); }
    static int hashCode(bool b) { return b ? 1231 : 1237; }
    // 未テスト
    // 計算量: O(1)
    static bool getBoolean(const JString&) { return false; }
};
using Boolean = JBoolean;
