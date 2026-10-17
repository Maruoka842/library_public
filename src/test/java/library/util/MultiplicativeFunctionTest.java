package library.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import library.util.collections.ArrayOnQuotient;

public class MultiplicativeFunctionTest {

	@Test
	public void testConstant() {
		int N = 100;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.constant());
		assertEquals(0, res[0]);
		for (int i = 1; i <= N; i++) {
			assertEquals(1, res[i]);
		}
	}

	@Test
	public void testIdentity() {
		int N = 100;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.identity());
		assertEquals(0, res[0]);
		assertEquals(1, res[1]);
		for (int i = 2; i <= N; i++) {
			assertEquals(0, res[i]);
		}
	}

	@Test
	public void testId() {
		int N = 100;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.id());
		assertEquals(0, res[0]);
		for (int i = 1; i <= N; i++) {
			assertEquals(i, res[i]);
		}

		long[] res2 = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.id(2));
		for (int i = 1; i <= N; i++) {
			assertEquals((long) i * i, res2[i]);
		}
	}

	@Test
	public void testMoebius() {
		int N = 100;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.moebius());
		assertEquals(0, res[0]);
		assertEquals(1, res[1]);

		// Compare with existing Sieve.mus
		int[] expected = Sieve.mus(N + 1);
		for (int i = 1; i <= N; i++) {
			assertEquals(expected[i], res[i]);
		}
	}

	@Test
	public void testTotient() {
		int N = 100;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.totient());
		assertEquals(0, res[0]);
		for (int i = 1; i <= N; i++) {
			assertEquals(Sieve.totient(i), res[i]);
		}
	}

	@Test
	public void testDivisor() {
		int N = 100;
		long[] sigma0 = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.divisor(0));
		int[] expectedD = Sieve.enumdivisorNums(N + 1);
		for (int i = 1; i <= N; i++) {
			assertEquals(expectedD[i], sigma0[i]);
		}

		long[] sigma1 = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.divisor(1));
		for (int i = 1; i <= N; i++) {
			long sum = 0;
			for (long d : MathUtils.divisors(i)) {
				sum += d;
			}
			assertEquals(sum, sigma1[i]);
		}
	}

	@Test
	public void testConvolveEvaluator() {
		int N = 100;
		// phi = mu * Id -> totient = convolve(moebius, id)
		var phiPP = MultiplicativeFunctionUtils.convolve(MultiplicativeFunctionUtils.moebius(), MultiplicativeFunctionUtils.id());
		long[] res = MultiplicativeFunctionUtils.enumValues(N, phiPP);
		for (int i = 1; i <= N; i++) {
			assertEquals(Sieve.totient(i), res[i]);
		}
	}

	@Test
	public void testConvolveArrays() {
		int N = 100;
		long[] mu = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.moebius());
		long[] id = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.id());
		long[] phi = MultiplicativeFunctionUtils.convolve(mu, id);
		for (int i = 1; i <= N; i++) {
			assertEquals(Sieve.totient(i), phi[i]);
		}
	}

	@Test
	public void testInverseEvaluator() {
		int N = 100;
		// constant 1 and moebius mu are dirichlet inverses of each other
		var invConstant = MultiplicativeFunctionUtils.inverse(MultiplicativeFunctionUtils.constant());
		long[] res = MultiplicativeFunctionUtils.enumValues(N, invConstant);
		assertEquals(1, res[1]);
		for (int i = 2; i <= N; i++) {
			assertEquals(Sieve.mus(N + 1)[i], res[i]);
		}
	}

	@Test
	public void testInverseArrays() {
		int N = 100;
		long[] constant = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.constant());
		long[] invConstant = MultiplicativeFunctionUtils.inverse(constant);
		assertEquals(1, invConstant[1]);
		for (int i = 2; i <= N; i++) {
			assertEquals(Sieve.mus(N + 1)[i], invConstant[i]);
		}
	}

	@Test
	public void testModulo() {
		int N = 100;
		long mod = 998244353;
		long[] res = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.totient(mod), mod);
		for (int i = 1; i <= N; i++) {
			long expected = Sieve.totient(i) % mod;
			assertEquals(expected, res[i]);
		}

		long[] divisorSq = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.divisor(2, mod), mod);
		for (int i = 1; i <= N; i++) {
			long sum = 0;
			for (long d : MathUtils.divisors(i)) {
				sum = (sum + d * d) % mod;
			}
			assertEquals(sum, divisorSq[i]);
		}
	}

	@Test
	public void testModuloConvolveAndInverse() {
		int N = 100;
		long mod = 998244353;
		long[] f = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.constant(), mod);
		long[] g = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.id(1, mod), mod);
		long[] conv = MultiplicativeFunctionUtils.convolve(f, g, mod);

		long[] expectedConv = MultiplicativeFunctionUtils.enumValues(N, MultiplicativeFunctionUtils.divisor(1, mod), mod);
		for (int i = 1; i <= N; i++) {
			assertEquals(expectedConv[i], conv[i]);
		}

		long[] invConv = MultiplicativeFunctionUtils.inverse(conv, mod);
		long[] back = MultiplicativeFunctionUtils.convolve(conv, invConv, mod);
		assertEquals(1, back[1]);
		for (int i = 2; i <= N; i++) {
			assertEquals(0, back[i]);
		}
	}

	@Test
	public void testEdgeCases() {
		assertThrows(IllegalArgumentException.class, () -> {
			MultiplicativeFunctionUtils.enumValues(-1, MultiplicativeFunctionUtils.constant());
		});

		long[] res0 = MultiplicativeFunctionUtils.enumValues(0, MultiplicativeFunctionUtils.constant());
		assertEquals(1, res0.length);
		assertEquals(0, res0[0]);

		long[] res1 = MultiplicativeFunctionUtils.enumValues(1, MultiplicativeFunctionUtils.constant());
		assertEquals(2, res1.length);
		assertEquals(0, res1[0]);
		assertEquals(1, res1[1]);
	}

	@Test
	public void testDefaultMethods() {
		MultiplicativeFunction f = MultiplicativeFunctionUtils.constant();
		MultiplicativeFunction g = MultiplicativeFunctionUtils.id();

		// evaluate(long n)
		assertEquals(1, f.evaluate(12));
		assertEquals(12, g.evaluate(12));

		// evaluate(long n, long mod)
		long mod = 10;
		assertEquals(2, g.evaluate(12, mod));

		// default convolve
		MultiplicativeFunction conv = f.convolve(g);
		// (1 * id)(12) = divisorSum(12) = 1+2+3+4+6+12 = 28
		assertEquals(28, conv.evaluate(12));

		// default convolve modulo
		MultiplicativeFunction convMod = f.convolve(g, mod);
		assertEquals(8, convMod.evaluate(12, mod));

		// default inverse
		MultiplicativeFunction inv = f.inverse();
		// inverse of 1 is Moebius mu
		assertEquals(-1, inv.evaluatePrimePower(2, 1)); // mu(p^1) = -1
		assertEquals(1, inv.evaluate(6)); // mu(6) = 1
		assertEquals(0, inv.evaluate(12)); // mu(12) = 0

		// default inverse modulo
		MultiplicativeFunction invMod = f.inverse(mod);
		assertEquals(9, invMod.evaluatePrimePower(2, 1)); // -1 mod 10 = 9
		assertEquals(1, invMod.evaluate(6, mod)); // 1 mod 10 = 1
		assertEquals(0, invMod.evaluate(12, mod));
	}

	@Test
	public void testPrimeSumToMultiplicativeFunctionSumOnFp() {
		long N = 100;
		long mod = 998244353;
		MultiplicativeFunction f = MultiplicativeFunctionUtils.id();
		ArrayOnQuotient primeSum = PrimeCount.cntForQuotients(N); // counts primes up to quotients
		ArrayOnQuotient res = f.primeSumToMultiplicativeFunctionSumOnFp(primeSum, mod);
		assertNotNull(res);
		assertEquals(N, res.getN());
	}
}
