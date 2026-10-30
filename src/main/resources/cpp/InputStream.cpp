struct JInputStream {
    vector<char> buffer;
    size_t pos = 0;
    size_t marked_pos = 0;
    bool is_bytes = false;

    JInputStream() = default;
    JInputStream(const vector<char>& b) : buffer(b), is_bytes(true) {}
    JInputStream(const vector<int8_t>& b) : buffer(b.begin(), b.end()), is_bytes(true) {}
    template<class... Args>
    JInputStream(const JInputStream& other, Args&&...) : buffer(other.buffer), pos(other.pos), marked_pos(other.marked_pos), is_bytes(other.is_bytes) {}

    int read() {
        if (pos < buffer.size()) {
            return static_cast<unsigned char>(buffer[pos++]);
        }
        if (is_bytes) return -1;
        int ch = cin.get();
        if (ch == EOF) return -1;
        buffer.push_back(static_cast<char>(ch));
        pos++;
        return ch;
    }

    int read(vector<int8_t>& b) {
        return read(b, 0, static_cast<int>(b.size()));
    }

    int read(vector<char>& b) {
        return read(b, 0, static_cast<int>(b.size()));
    }

    template<typename T>
    int read(vector<T>& b, int off, int length) {
        int cnt = 0;
        for (int i = 0; i < length; ++i) {
            int ch = read();
            if (ch == -1) break;
            b[off + i] = static_cast<T>(ch);
            cnt++;
        }
        return cnt == 0 ? -1 : cnt;
    }

    void mark(int) {
        marked_pos = pos;
    }

    void reset() {
        pos = marked_pos;
    }

    int available() const {
        return is_bytes ? max(0, static_cast<int>(buffer.size()) - static_cast<int>(pos)) : 0;
    }

    void close() {}

    long long skip(long long n) {
        long long skipped = 0;
        while (skipped < n && read() != -1) skipped++;
        return skipped;
    }

    JString readLine() {
        JString s;
        while (true) {
            int ch = read();
            if (ch == -1) return s;
            if (ch == '\r') {
                int next_ch = read();
                if (next_ch != '\n' && next_ch != -1) {
                    // Ignore or handle non-lf after cr
                }
                break;
            }
            if (ch == '\n') break;
            s.push_back(static_cast<char>(ch));
        }
        return s;
    }
};
