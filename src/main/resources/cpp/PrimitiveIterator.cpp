struct PrimitiveIterator {
    struct OfInt : JIterator<int> {
        using JIterator<int>::JIterator;
        OfInt() = default;
        OfInt(const JIterator<int>& it) : JIterator<int>(it) {}
        OfInt(shared_ptr<OfInt> p) : JIterator<int>(p) {}
        virtual int nextInt() override { return JIterator<int>::next(); }
        virtual int next() override { return nextInt(); }
    };
    struct OfLong : JIterator<long long> {
        using JIterator<long long>::JIterator;
        OfLong() = default;
        OfLong(const JIterator<long long>& it) : JIterator<long long>(it) {}
        OfLong(shared_ptr<OfLong> p) : JIterator<long long>(p) {}
        virtual long long nextLong() override { return JIterator<long long>::next(); }
        virtual long long next() override { return nextLong(); }
    };
    struct OfDouble : JIterator<double> {
        using JIterator<double>::JIterator;
        OfDouble() = default;
        OfDouble(const JIterator<double>& it) : JIterator<double>(it) {}
        OfDouble(shared_ptr<OfDouble> p) : JIterator<double>(p) {}
        virtual double nextDouble() override { return JIterator<double>::next(); }
        virtual double next() override { return nextDouble(); }
    };
};
using JPrimitiveIterator = PrimitiveIterator;
