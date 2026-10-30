struct JStringTokenizer {
    vector<JString> tokens;
    size_t pos = 0;
    bool valid = true;

    JStringTokenizer() : valid(false) {}
    JStringTokenizer(nullptr_t) : valid(false) {}
    JStringTokenizer(const JString& str) : JStringTokenizer(str, JString(" \t\n\r\f"), false) {}
    JStringTokenizer(const JString& str, const JString& delim) : JStringTokenizer(str, delim, false) {}
    JStringTokenizer(const JString& str, const JString& delim, bool returnDelims) : valid(true) {
        string cur;
        for (char c : str) {
            if (delim.find(c) != string::npos) {
                if (!cur.empty()) {
                    tokens.push_back(cur);
                    cur.clear();
                }
                if (returnDelims) {
                    tokens.push_back(string(1, c));
                }
            } else {
                cur.push_back(c);
            }
        }
        if (!cur.empty()) tokens.push_back(cur);
    }
    bool hasMoreTokens() const { return valid && pos < tokens.size(); }
    bool hasMoreElements() const { return hasMoreTokens(); }
    JString nextToken() { return hasMoreTokens() ? tokens[pos++] : ""; }
    JString nextToken(const JString& delim) {
        JString rest;
        for (size_t i = pos; i < tokens.size(); ++i) {
            if (i > pos) rest += " ";
            rest += tokens[i];
        }
        *this = JStringTokenizer(rest, delim);
        return nextToken();
    }
    JString nextElement() { return nextToken(); }
    int countTokens() const { return valid ? static_cast<int>(tokens.size() - pos) : 0; }
    bool operator==(nullptr_t) const { return !valid; }
    bool operator!=(nullptr_t) const { return valid; }
};
inline bool jisNull(const JStringTokenizer& x) { return !x.valid; }
