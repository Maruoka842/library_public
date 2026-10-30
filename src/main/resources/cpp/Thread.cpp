struct JThread {
    function<void()> action;
    JThread() : action([]{}) {}
    template<class T> JThread(shared_ptr<T> target)
        : action([target]() { if (target) target->run(); }) {}
    template<class T> JThread(nullptr_t, shared_ptr<T> target, const JString&, long long)
        : action([target]() { if (target) target->run(); }) {}
    void start() { if (action) action(); }
    void run() { if (action) action(); }
    // 未テスト
    // 計算量: O(millis)
    static void sleep(long long millis) {
        std::this_thread::sleep_for(std::chrono::milliseconds(millis));
    }
};
