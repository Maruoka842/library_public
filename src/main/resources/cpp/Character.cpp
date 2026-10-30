struct JCharacter {
    static constexpr char MIN_VALUE = '\0';
    static constexpr char MAX_VALUE = static_cast<char>(127);
    static constexpr int BYTES = 2;
    static constexpr int SIZE = 16;
    static int compare(char a, char b) { return a - b; }
    static int hashCode(char c) { return static_cast<int>(c); }
    static char toUpperCase(char c) { return static_cast<char>(::toupper(static_cast<unsigned char>(c))); }
    static char toLowerCase(char c) { return static_cast<char>(::tolower(static_cast<unsigned char>(c))); }
    static bool isDigit(char c) { return ::isdigit(static_cast<unsigned char>(c)); }
    static bool isLetter(char c) { return ::isalpha(static_cast<unsigned char>(c)); }
    static bool isAlphabetic(int c) { return ::isalpha(static_cast<unsigned char>(c)); }
    static bool isLetterOrDigit(char c) { return ::isalnum(static_cast<unsigned char>(c)); }
    static bool isWhitespace(char c) { return ::isspace(static_cast<unsigned char>(c)); }
    static bool isUpperCase(char c) { return ::isupper(static_cast<unsigned char>(c)); }
    static bool isLowerCase(char c) { return ::islower(static_cast<unsigned char>(c)); }
    // 未テスト
    // 計算量: O(1)
    static int digit(char ch, int radix) {
        if (radix < 2 || radix > 36) return -1;
        int val = -1;
        if (ch >= '0' && ch <= '9') val = ch - '0';
        else if (ch >= 'a' && ch <= 'z') val = ch - 'a' + 10;
        else if (ch >= 'A' && ch <= 'Z') val = ch - 'A' + 10;
        return (val >= 0 && val < radix) ? val : -1;
    }
    // 未テスト
    // 計算量: O(1)
    static char forDigit(int digit, int radix) {
        if (radix < 2 || radix > 36) return '\0';
        if (digit < 0 || digit >= radix) return '\0';
        return digit < 10 ? static_cast<char>('0' + digit) : static_cast<char>('a' + digit - 10);
    }
    // 未テスト
    // 計算量: O(1)
    static bool isSpaceChar(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f'; }
    // 未テスト
    // 計算量: O(1)
    static bool isISOControl(char c) { return ::iscntrl(static_cast<unsigned char>(c)); }
    // 未テスト
    // 計算量: O(1)
    static JString toString(char c) { return JString(1, c); }
};
