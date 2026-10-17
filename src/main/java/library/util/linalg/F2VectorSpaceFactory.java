package library.util.linalg;

/**
 * 各種 F2 ベクトル空間・生成空間オブジェクトを生成するファクトリクラス。
 */
public class F2VectorSpaceFactory {

    /**
     * k 長の連続同値ブロック反転操作 (0^k <-> 1^k) によって生成される F2 ベクトル空間を表す {@link F2BlockFlipsVectorSpace} インスタンスを生成する。
     *
     * <p>計算量: O(1)</p>
     *
     * @param k ブロック長 (k >= 1)
     * @return k 長ブロック反転操作による F2 ベクトル空間解析インスタンス
     */
    // 未テスト
    public static F2BlockFlipsVectorSpace ofBlockFlips(int k) {
        return new F2BlockFlipsVectorSpace(k);
    }
}
