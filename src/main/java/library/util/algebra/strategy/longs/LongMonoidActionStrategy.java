package library.util.algebra.strategy.longs;

/**
 * long 型モノイドの作用を表すインターフェース。
 * 作用する側（作用素）と作用される側（値）の両方が long 型であるモノイド作用を定義する。
 */
public interface LongMonoidActionStrategy {
    /**
     * 作用される（値）モノイドの戦略を返す。
     * @return 値モノイドの戦略
     */
    // 未テスト
    LongMonoidStrategy targetMonoidStrategy();

    /**
     * 作用する（作用素）モノイドの戦略を返す。
     * @return 作用素モノイドの戦略
     */
    // 未テスト
    LongMonoidStrategy actingMonoidStrategy();

    /**
     * 作用素 f をサイズ size の値 x に作用させた結果を返す。
     * @param f 作用素
     * @param x 作用される値
     * @param size 作用対象の頂点数（区間サイズ）
     * @return 作用適用後の値
     */
    // 未テスト
    long act(long f, long x, int size);
}