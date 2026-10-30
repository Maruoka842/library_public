struct JScanner {
    JString next() { JString x; cin >> x; return x; }
    JString nextLine() { JString s; string line; if (getline(cin >> ws, line)) s = line; return s; }
    int nextInt() { int x; cin >> x; return x; }
    long long nextLong() { long long x; cin >> x; return x; }
    double nextDouble() { double x; cin >> x; return x; }
    bool hasNext() { return bool(cin >> ws) && cin.peek() != EOF; }
    vector<int> nextInts(int n) { vector<int> a(n); for (auto& x : a) cin >> x; return a; }
    vector<vector<int>> nextInts(int h, int w) {
        vector<vector<int>> a(h, vector<int>(w));
        for (auto& row : a) for (auto& x : row) cin >> x;
        return a;
    }
    vector<long long> nextLongs(int n) { vector<long long> a(n); for (auto& x : a) cin >> x; return a; }
    vector<long long> nextUnsignedLongs(int n) { vector<long long> a(n); for (auto& x : a) { unsigned long long u; cin >> u; x = static_cast<long long>(u); } return a; }
    vector<vector<char>> nextChars(int h, int w) {
        vector<vector<char>> a(h, vector<char>(w));
        for (auto& row : a) { JString s; cin >> s; row.assign(s.begin(), s.end()); }
        return a;
    }
    void close() {}
};
