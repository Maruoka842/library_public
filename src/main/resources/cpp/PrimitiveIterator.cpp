struct PrimitiveIterator {
    struct OfInt : JIterator<int> { virtual int nextInt() override { return next(); } };
    struct OfLong : JIterator<long long> { virtual long long nextLong() override { return next(); } };
    struct OfDouble : JIterator<double> { virtual double nextDouble() override { return next(); } };
};
using JPrimitiveIterator = PrimitiveIterator;
