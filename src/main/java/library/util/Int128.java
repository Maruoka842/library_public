package library.util;
import java.io.Serializable;

/**
 * 128 bit 符号付き整数。
 *
 * <p>2 の補数表現を用い、算術演算は {@code long} と同様に
 * {@code mod 2^128} で wrap around する。</p>
 */
public final class Int128 extends Number implements Comparable<Int128>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final long MASK32 = 0xffff_ffffL;
    private static final long DEC_BASE = 1_000_000_000_000_000_000L;

    public static final Int128 ZERO = new Int128(0, 0);
    public static final Int128 ONE = new Int128(0, 1);
    public static final Int128 MIN_VALUE = new Int128(Long.MIN_VALUE, 0);
    public static final Int128 MAX_VALUE = new Int128(Long.MAX_VALUE, -1L);

    /** 上位 64 bit。 */
    public final long hi;
    /** 下位 64 bit。 */
    public final long lo;

    public Int128(long hi, long lo) {
        this.hi = hi;
        this.lo = lo;
    }

    /** signed long を符号拡張して変換する。 */
    public static Int128 valueOf(long x) {
        return new Int128(x < 0 ? -1L : 0L, x);
    }

    /** long の bit pattern を unsigned 64 bit 整数として変換する。 */
    public static Int128 valueOfUnsigned(long x) {
        return new Int128(0, x);
    }

    public Int128 add(Int128 o) {
        long l = lo + o.lo;
        long carry = Long.compareUnsigned(l, lo) < 0 ? 1 : 0;
        return new Int128(hi + o.hi + carry, l);
    }

    public Int128 add(long x) {
        long l = lo + x;
        long carry = Long.compareUnsigned(l, lo) < 0 ? 1 : 0;
        return new Int128(hi + (x < 0 ? -1L : 0L) + carry, l);
    }

    public Int128 subtract(Int128 o) {
        long l = lo - o.lo;
        long borrow = Long.compareUnsigned(lo, o.lo) < 0 ? 1 : 0;
        return new Int128(hi - o.hi - borrow, l);
    }

    public Int128 subtract(long x) {
        long l = lo - x;
        long borrow = Long.compareUnsigned(lo, x) < 0 ? 1 : 0;
        return new Int128(hi - (x < 0 ? -1L : 0L) - borrow, l);
    }

    public Int128 negate() {
        long l = -lo;
        return new Int128(~hi + (l == 0 ? 1 : 0), l);
    }

    public Int128 multiply(Int128 o) {
        long l = lo * o.lo;
        long h = unsignedMultiplyHigh(lo, o.lo) + hi * o.lo + lo * o.hi;
        return new Int128(h, l);
    }

    public Int128 multiply(long x) {
        long l = lo * x;
        long h = unsignedMultiplyHigh(lo, x) + hi * x + (x < 0 ? -lo : 0);
        return new Int128(h, l);
    }

    /** unsigned 64 bit × unsigned 64 bit の積の上位 64 bit。 */
    private static long unsignedMultiplyHigh(long x, long y) {
        return Math.multiplyHigh(x, y) + ((x >> 63) & y) + ((y >> 63) & x);
    }

    public Int128 divide(Int128 o) {
        DivResult z = divideSigned(hi, lo, o.hi, o.lo);
        return new Int128(z.qHi, z.qLo);
    }

    public Int128 remainder(Int128 o) {
        DivResult z = divideSigned(hi, lo, o.hi, o.lo);
        return new Int128(z.rHi, z.rLo);
    }

    public Int128[] divideAndRemainder(Int128 o) {
        DivResult z = divideSigned(hi, lo, o.hi, o.lo);
        return new Int128[] {
            new Int128(z.qHi, z.qLo),
            new Int128(z.rHi, z.rLo)
        };
    }

    /** unsigned 128 bit 整数として除算する。 */
    public static Int128 divideUnsigned(Int128 a, Int128 b) {
        DivResult z = divideUnsigned0(a.hi, a.lo, b.hi, b.lo);
        return new Int128(z.qHi, z.qLo);
    }

    /** unsigned 128 bit 整数として剰余を求める。 */
    public static Int128 remainderUnsigned(Int128 a, Int128 b) {
        DivResult z = divideUnsigned0(a.hi, a.lo, b.hi, b.lo);
        return new Int128(z.rHi, z.rLo);
    }

    /** unsigned 128 bit 整数として商と剰余を求める。 */
    public static Int128[] divideAndRemainderUnsigned(Int128 a, Int128 b) {
        DivResult z = divideUnsigned0(a.hi, a.lo, b.hi, b.lo);
        return new Int128[] {
            new Int128(z.qHi, z.qLo),
            new Int128(z.rHi, z.rLo)
        };
    }

    private static DivResult divideSigned(long aHi, long aLo, long bHi, long bLo) {
        if ((bHi | bLo) == 0) throw new ArithmeticException("/ by zero");

        boolean negA = aHi < 0;
        boolean negB = bHi < 0;

        if (negA) {
            long l = -aLo;
            aHi = ~aHi + (l == 0 ? 1 : 0);
            aLo = l;
        }
        if (negB) {
            long l = -bLo;
            bHi = ~bHi + (l == 0 ? 1 : 0);
            bLo = l;
        }

        DivResult z = divideUnsigned0(aHi, aLo, bHi, bLo);

        if (negA ^ negB) {
            long l = -z.qLo;
            z.qHi = ~z.qHi + (l == 0 ? 1 : 0);
            z.qLo = l;
        }
        if (negA) {
            long l = -z.rLo;
            z.rHi = ~z.rHi + (l == 0 ? 1 : 0);
            z.rLo = l;
        }
        return z;
    }

    private static DivResult divideUnsigned0(long aHi, long aLo, long bHi, long bLo) {
        if ((bHi | bLo) == 0) throw new ArithmeticException("/ by zero");

        int cmp = compareUnsigned(aHi, aLo, bHi, bLo);
        if (cmp < 0) return new DivResult(0, 0, aHi, aLo);
        if (cmp == 0) return new DivResult(0, 1, 0, 0);

        if (bHi == 0 && bLo == 1) {
            return new DivResult(aHi, aLo, 0, 0);
        }

        int power = powerOfTwoExponent(bHi, bLo);
        if (power >= 0) {
            return divideUnsignedByPowerOfTwo(aHi, aLo, power);
        }

        // 64 / 64。
        if (aHi == 0) {
            long q = Long.divideUnsigned(aLo, bLo);
            long r = Long.remainderUnsigned(aLo, bLo);
            return new DivResult(0, q, 0, r);
        }

        // 128 / 64: 上位 limb を割った後、残りだけ 128/64 -> 64 にする。
        if (bHi == 0) {
            long qHi = Long.divideUnsigned(aHi, bLo);
            long remHi = Long.remainderUnsigned(aHi, bLo);
            Div64 z = divide128By64To64(remHi, aLo, bLo);
            return new DivResult(qHi, z.q, 0, z.r);
        }

        // (bHi * 2^64) で割る場合。
        if (bLo == 0) {
            long q = Long.divideUnsigned(aHi, bHi);
            long rHi = Long.remainderUnsigned(aHi, bHi);
            return new DivResult(0, q, rHi, aLo);
        }

        // divisor >= 2^64 なので quotient < 2^64。
        return divide128By128To64(aHi, aLo, bHi, bLo);
    }


    private static int powerOfTwoExponent(long hi, long lo) {
        if (hi == 0) {
            return lo != 0 && (lo & (lo - 1)) == 0 ? Long.numberOfTrailingZeros(lo) : -1;
        }
        if (lo == 0 && (hi & (hi - 1)) == 0) {
            return 64 + Long.numberOfTrailingZeros(hi);
        }
        return -1;
    }

    private static DivResult divideUnsignedByPowerOfTwo(long hi, long lo, int shift) {
        if (shift == 0) return new DivResult(hi, lo, 0, 0);
        if (shift < 64) {
            long qHi = hi >>> shift;
            long qLo = (lo >>> shift) | (hi << (64 - shift));
            long rLo = lo & (-1L >>> (64 - shift));
            return new DivResult(qHi, qLo, 0, rLo);
        }
        if (shift == 64) {
            return new DivResult(0, hi, 0, lo);
        }
        int s = shift - 64;
        long qLo = hi >>> s;
        long rHi = hi & (-1L >>> (64 - s));
        return new DivResult(0, qLo, rHi, lo);
    }

    /**
     * unsigned (numHi * 2^64 + numLo) / den を求める。
     * quotient が 64 bit に収まる、すなわち numHi < den を仮定する。
     */
    private static Div64 divide128By64To64(long numHi, long numLo, long den) {
        if (den == 0) throw new ArithmeticException("/ by zero");
        if (Long.compareUnsigned(numHi, den) >= 0) {
            throw new ArithmeticException("quotient overflow");
        }

        long originalDen = den;
        long originalLo = numLo;

        // den の最上位 bit を 1 にする。
        int shift = Long.numberOfLeadingZeros(den);
        den <<= shift;
        if (shift != 0) {
            numHi = (numHi << shift) | (numLo >>> (64 - shift));
        }
        numLo <<= shift;

        long den1 = den >>> 32;
        long den0 = den & MASK32;
        long num1 = numLo >>> 32;
        long num0 = numLo & MASK32;

        // quotient の上位 32 bit を推定して高々 2 補正する。
        long qhat = Long.divideUnsigned(numHi, den1);
        long rhat = Long.remainderUnsigned(numHi, den1);
        long c1 = qhat * den0;
        long c2 = (rhat << 32) + num1;
        if (Long.compareUnsigned(c1, c2) > 0) {
            qhat -= Long.compareUnsigned(c1 - c2, den) > 0 ? 2 : 1;
        }
        long q1 = qhat & MASK32;

        long rem = numHi * (1L << 32) + num1 - q1 * den;

        // quotient の下位 32 bit。
        qhat = Long.divideUnsigned(rem, den1);
        rhat = Long.remainderUnsigned(rem, den1);
        c1 = qhat * den0;
        c2 = (rhat << 32) + num0;
        if (Long.compareUnsigned(c1, c2) > 0) {
            qhat -= Long.compareUnsigned(c1 - c2, den) > 0 ? 2 : 1;
        }
        long q0 = qhat & MASK32;

        long q = (q1 << 32) | q0;
        long r = originalLo - q * originalDen;
        return new Div64(q, r);
    }

    /**
     * unsigned 128 / 128 -> 64 bit quotient。
     * divisor の上位 64 bit が非 0 なので quotient は必ず 64 bit に収まる。
     */
    private static DivResult divide128By128To64(long aHi, long aLo, long bHi, long bLo) {
        int shift = Long.numberOfLeadingZeros(bHi);

        // divisor を正規化したときの上位 64 bit。
        long normalizedBHi = shift == 0
            ? bHi
            : (bHi << shift) | (bLo >>> (64 - shift));

        // a / 2 を使って overflow しない近似商を作る。
        long halfAHi = aHi >>> 1;
        long halfALo = (aLo >>> 1) | (aHi << 63);
        long q1 = divide128By64To64(halfAHi, halfALo, normalizedBHi).q;

        // q0 = ((q1 << shift) >>> 63) - 1（0 の場合は引かない）。
        // 一時的に 65 bit になり得るため hi/lo で持つ。
        long qHi;
        long qLo;
        if (shift == 0) {
            qHi = 0;
            qLo = q1;
        } else {
            qHi = q1 >>> (64 - shift);
            qLo = q1 << shift;
        }
        qLo = (qLo >>> 63) | (qHi << 1);
        qHi >>>= 63;

        if ((qHi | qLo) != 0) {
            long old = qLo;
            qLo--;
            if (old == 0) qHi--;
        }

        // r = a - q * b (mod 2^128)。
        long productLo = qLo * bLo;
        long productHi = qHi * bLo + qLo * bHi + unsignedMultiplyHigh(qLo, bLo);

        long rLo = aLo - productLo;
        long borrow = Long.compareUnsigned(aLo, productLo) < 0 ? 1 : 0;
        long rHi = aHi - productHi - borrow;

        // q は正解または 1 小さい。後者なら 1 回だけ補正する。
        if (compareUnsigned(rHi, rLo, bHi, bLo) >= 0) {
            qLo++;
            if (qLo == 0) qHi++;

            long nextLo = rLo - bLo;
            borrow = Long.compareUnsigned(rLo, bLo) < 0 ? 1 : 0;
            rHi -= bHi + borrow;
            rLo = nextLo;
        }

        // 真の quotient は 64 bit 未満。
        return new DivResult(0, qLo, rHi, rLo);
    }

    private static int compareUnsigned(long aHi, long aLo, long bHi, long bLo) {
        int c = Long.compareUnsigned(aHi, bHi);
        return c != 0 ? c : Long.compareUnsigned(aLo, bLo);
    }

    private static final class Div64 {
        final long q, r;
        Div64(long q, long r) {
            this.q = q;
            this.r = r;
        }
    }

    private static final class DivResult {
        long qHi, qLo, rHi, rLo;
        DivResult(long qHi, long qLo, long rHi, long rLo) {
            this.qHi = qHi;
            this.qLo = qLo;
            this.rHi = rHi;
            this.rLo = rLo;
        }
    }

    public Int128 and(Int128 o) {
        return new Int128(hi & o.hi, lo & o.lo);
    }

    public Int128 or(Int128 o) {
        return new Int128(hi | o.hi, lo | o.lo);
    }

    public Int128 xor(Int128 o) {
        return new Int128(hi ^ o.hi, lo ^ o.lo);
    }

    public Int128 not() {
        return new Int128(~hi, ~lo);
    }

    /** {@code long} と同様に shift distance の下位 7 bit のみを見る。 */
    public Int128 shiftLeft(int distance) {
        int n = distance & 127;
        if (n == 0) return this;
        if (n < 64) {
            return new Int128((hi << n) | (lo >>> (64 - n)), lo << n);
        }
        return new Int128(lo << (n - 64), 0);
    }

    /** 算術右シフト。shift distance の下位 7 bit のみを見る。 */
    public Int128 shiftRight(int distance) {
        int n = distance & 127;
        if (n == 0) return this;
        if (n < 64) {
            return new Int128(hi >> n, (lo >>> n) | (hi << (64 - n)));
        }
        return new Int128(hi >> 63, hi >> (n - 64));
    }

    /** 論理右シフト。shift distance の下位 7 bit のみを見る。 */
    public Int128 shiftRightUnsigned(int distance) {
        int n = distance & 127;
        if (n == 0) return this;
        if (n < 64) {
            return new Int128(hi >>> n, (lo >>> n) | (hi << (64 - n)));
        }
        return new Int128(0, hi >>> (n - 64));
    }

    public int signum() {
        if (hi < 0) return -1;
        return (hi | lo) == 0 ? 0 : 1;
    }

    public boolean isZero() {
        return (hi | lo) == 0;
    }

    public boolean testBit(int bit) {
        if (bit < 0 || bit >= 128) throw new IndexOutOfBoundsException("bit = " + bit);
        return bit < 64
            ? ((lo >>> bit) & 1) != 0
            : ((hi >>> (bit - 64)) & 1) != 0;
    }

    public Int128 setBit(int bit) {
        if (bit < 0 || bit >= 128) throw new IndexOutOfBoundsException("bit = " + bit);
        return bit < 64
            ? new Int128(hi, lo | (1L << bit))
            : new Int128(hi | (1L << (bit - 64)), lo);
    }

    @Override
    public int compareTo(Int128 o) {
        int c = Long.compare(hi, o.hi);
        return c != 0 ? c : Long.compareUnsigned(lo, o.lo);
    }

    public static int compareUnsigned(Int128 a, Int128 b) {
        return compareUnsigned(a.hi, a.lo, b.hi, b.lo);
    }

    @Override
    public int intValue() {
        return (int) lo;
    }

    @Override
    public long longValue() {
        return lo;
    }

    @Override
    public float floatValue() {
        if (hi < 0) {
            Int128 x = negate();
            return -unsignedToFloat(x.hi, x.lo);
        }
        return unsignedToFloat(hi, lo);
    }

    @Override
    public double doubleValue() {
        if (hi < 0) {
            Int128 x = negate();
            return -unsignedToDouble(x.hi, x.lo);
        }
        return unsignedToDouble(hi, lo);
    }

    private static double unsignedToDouble(long hi, long lo) {
        int bitLength = unsignedBitLength(hi, lo);
        if (bitLength <= 53) return (double) lo;

        int shift = bitLength - 53;
        long top = unsignedShiftRightToLong(hi, lo, shift);
        if (shouldRoundUp(hi, lo, shift, top)) top++;
        return Math.scalb((double) top, shift);
    }

    private static float unsignedToFloat(long hi, long lo) {
        int bitLength = unsignedBitLength(hi, lo);
        if (bitLength <= 24) return (float) lo;

        int shift = bitLength - 24;
        long top = unsignedShiftRightToLong(hi, lo, shift);
        if (shouldRoundUp(hi, lo, shift, top)) top++;
        return Math.scalb((float) top, shift);
    }

    private static int unsignedBitLength(long hi, long lo) {
        if (hi != 0) return 128 - Long.numberOfLeadingZeros(hi);
        return lo == 0 ? 0 : 64 - Long.numberOfLeadingZeros(lo);
    }

    /** unsigned 128 bit 整数を右シフトし、下位 64 bit を返す。 */
    private static long unsignedShiftRightToLong(long hi, long lo, int shift) {
        if (shift == 0) return lo;
        if (shift < 64) return (lo >>> shift) | (hi << (64 - shift));
        return hi >>> (shift - 64);
    }

    /**
     * 下位 {@code shift} bit を捨てるとき、round-to-nearest-even で
     * 切り上げるべきかを返す。{@code top = x >>> shift} を仮定する。
     */
    private static boolean shouldRoundUp(long hi, long lo, int shift, long top) {
        int roundBit = shift - 1;
        if (!unsignedTestBit(hi, lo, roundBit)) return false;
        return hasAnyLowBits(hi, lo, roundBit) || (top & 1) != 0;
    }

    private static boolean unsignedTestBit(long hi, long lo, int bit) {
        return bit < 64
            ? ((lo >>> bit) & 1) != 0
            : ((hi >>> (bit - 64)) & 1) != 0;
    }

    /** 下位 {@code bits} bit のいずれかが 1 なら true。 */
    private static boolean hasAnyLowBits(long hi, long lo, int bits) {
        if (bits <= 0) return false;
        if (bits < 64) return (lo & (-1L >>> (64 - bits))) != 0;
        if (bits == 64) return lo != 0;
        int hiBits = bits - 64;
        return lo != 0 || (hi & (-1L >>> (64 - hiBits))) != 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Int128)) return false;
        Int128 o = (Int128) obj;
        return hi == o.hi && lo == o.lo;
    }

    @Override
    public int hashCode() {
        long x = hi ^ Long.rotateLeft(lo, 17);
        return (int) (x ^ (x >>> 32));
    }

    @Override
    public String toString() {
        if (isZero()) return "0";
        return hi < 0 ? "-" + negate().toUnsignedString() : toUnsignedString();
    }

    public String toUnsignedString() {
        if (hi == 0) return Long.toUnsignedString(lo);

        long[] part = new long[3];
        int n = 0;
        Int128 x = this;
        Int128 base = valueOf(DEC_BASE);
        while (!x.isZero()) {
            DivResult z = divideUnsigned0(x.hi, x.lo, base.hi, base.lo);
            part[n++] = z.rLo;
            x = new Int128(z.qHi, z.qLo);
        }

        StringBuilder sb = new StringBuilder(39);
        sb.append(part[--n]);
        while (n > 0) {
            String s = Long.toString(part[--n]);
            for (int i = s.length(); i < 18; i++) sb.append('0');
            sb.append(s);
        }
        return sb.toString();
    }
}
