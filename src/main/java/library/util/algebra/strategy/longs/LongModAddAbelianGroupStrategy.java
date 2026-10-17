package library.util.algebra.strategy.longs;

/**
 * Z/modZ の加法アーベル群。
 */
public class LongModAddAbelianGroupStrategy implements LongAbelianGroupStrategy {
    /** 法。 */
    private final long mod;

    /**
     * Z/modZ の加法アーベル群を生成する。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param mod 法。mod > 0
     */
    public LongModAddAbelianGroupStrategy(long mod) {
        if (mod <= 0) throw new IllegalArgumentException("mod must be positive");
        this.mod = mod;
    }

    /**
     * 加法単位元 0 を返す。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @return 0
     */
    @Override
    public long identity() {
        return 0;
    }

    /**
     * a + b mod mod を返す。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param a 加数
     * @param b 加数
     * @return a + b mod mod
     */
    @Override
    public long mul(long a, long b) {
        a %= mod;
        b %= mod;
        if (a < 0) a += mod;
        if (b < 0) b += mod;
        return a >= mod - b ? a - (mod - b) : a + b;
    }

    /**
     * -a mod mod を返す。
     *
     * <p>計算量: O(1)
     *
     * 未テスト
     * @param a 元
     * @return a の加法逆元
     */
    @Override
    public long inverse(long a) {
        a %= mod;
        if (a < 0) a += mod;
        return a == 0 ? 0 : mod - a;
    }
}
