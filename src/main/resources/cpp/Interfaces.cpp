template<class T> struct Comparable {
    virtual ~Comparable() = default;
    virtual int compareTo(T) = 0;
};
struct Runnable {
    virtual ~Runnable() = default;
    virtual void run() = 0;
};
struct AutoCloseable {
    virtual ~AutoCloseable() = default;
    virtual void close() = 0;
};
using Closeable = AutoCloseable;
struct Serializable {};
struct Cloneable {};
