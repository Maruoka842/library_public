struct System {
    static void gc() {}
    // 未テスト
    // 計算量: O(1)
    static long long currentTimeMillis() { return jcurrentTimeMillis(); }
    // 未テスト
    // 計算量: O(1)
    static long long nanoTime() { return jnanoTime(); }
    // 未テスト
    // 計算量: O(1)
    static void exit(int status) { ::exit(status); }
    // 未テスト
    // 計算量: O(length)
    template<class Src, class Dest>
    static void arraycopy(const Src& src, int srcPos, Dest& dest, int destPos, int length) {
        jarraycopy(src, srcPos, dest, destPos, length);
    }
};
