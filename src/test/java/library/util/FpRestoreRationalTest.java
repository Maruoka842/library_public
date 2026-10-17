package library.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class FpRestoreRationalTest {

	@Test
	public void exhaustiveForEveryResidueOfSmallPrimeModuli() {
		long[] moduli = {2, 3, 5, 7, 11, 31, 101, 997};
		for (long mod : moduli) {
			Fp fp = new Fp(mod);
			for (long v = 0; v < mod; v++) {
				assertValidRestoration(fp, v);
			}
		}
	}

	@Test
	public void acceptsNonCanonicalInputRepresentatives() {
		Fp fp = new Fp(101);
		long[] values = {
				Long.MIN_VALUE, -10_000, -102, -101, -100, -1,
				0, 1, 100, 101, 102, 10_000, Long.MAX_VALUE
		};
		for (long v : values) {
			assertValidRestoration(fp, v);
		}
	}

	@Test
	public void randomResiduesOfCommonLargePrimeModuli() {
		Random random = new Random(0x5eedL);
		long[] moduli = {998_244_353L, 1_000_000_007L, 2_305_843_009_213_693_951L};
		for (long mod : moduli) {
			Fp fp = new Fp(mod);
			for (int trial = 0; trial < 20_000; trial++) {
				assertValidRestoration(fp, random.nextLong());
			}
		}
	}

	@Test
	public void restoresResiduesConstructedFromBoundedFractions() {
		long mod = 998_244_353L;
		Fp fp = new Fp(mod);
		long lim = MathUtils.sqrt(mod);
		Random random = new Random(1L);
		for (int trial = 0; trial < 20_000; trial++) {
			long numerator = random.nextLong(-lim, lim + 1);
			long denominator = random.nextLong(1, lim + 1);
			long residue = fp.mul(numerator, fp.inv(denominator));
			assertValidRestoration(fp, residue);
		}
	}

	/**
	 * {@code restoreRational(v)} が返す {@code a/b} について、
	 * {@code |a| <= floor(sqrt(mod))}, {@code 1 <= b <= floor(sqrt(mod))},
	 * {@code gcd(|a|, b) = 1}, {@code a == b*v (mod mod)} を検証する。
	 *
	 * <p>計算量: O(log mod)</p>
	 *
	 * @param fp 検証対象
	 * @param v 復元する整数
	 */
	private static void assertValidRestoration(Fp fp, long v) {
		long mod = fp.modulus();
		long reduced = fp.reduce(v);
		String restored = fp.restoreRational(v);
		if (reduced == 0) {
			assertEquals("0", restored);
			return;
		}

		String[] parts = restored.split("/", -1);
		assertEquals(2, parts.length, "unexpected format: " + restored);
		long numerator = Long.parseLong(parts[0]);
		long denominator = Long.parseLong(parts[1]);
		long lim = MathUtils.sqrt(mod);

		assertTrue(Math.abs(numerator) <= lim,
				() -> "numerator out of range: " + restored + ", mod=" + mod);
		assertTrue(1 <= denominator && denominator <= lim,
				() -> "denominator out of range: " + restored + ", mod=" + mod);
		assertEquals(1L, MathUtils.gcd(Math.abs(numerator), denominator),
				"fraction is not reduced: " + restored);

		BigInteger modulus = BigInteger.valueOf(mod);
		BigInteger lhs = BigInteger.valueOf(numerator).mod(modulus);
		BigInteger rhs = BigInteger.valueOf(denominator)
				.multiply(BigInteger.valueOf(reduced)).mod(modulus);
		assertEquals(lhs, rhs, "restored fraction represents a different residue");
	}
}
