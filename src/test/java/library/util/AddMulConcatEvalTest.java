package library.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AddMulConcatEvalTest {

	@Test
	public void testSingleNumber() {
		AddMulConcatEval e1 = new AddMulConcatEval(123);
		assertEquals(123, e1.eval);

		AddMulConcatEval e0 = new AddMulConcatEval(0);
		assertEquals(0, e0.eval);

		AddMulConcatEval eLeadingZeros = new AddMulConcatEval(7, 1000);
		assertEquals(7, eLeadingZeros.eval);
	}

	@Test
	public void testConcat() {
		AddMulConcatEval e1 = new AddMulConcatEval(12);
		AddMulConcatEval e2 = new AddMulConcatEval(34);
		AddMulConcatEval concat = AddMulConcatEval.concat(e1, e2);
		assertEquals(1234, concat.eval);

		AddMulConcatEval e007 = new AddMulConcatEval(7, 1000);
		AddMulConcatEval concatZeros = AddMulConcatEval.concat(e1, e007);
		assertEquals(12007, concatZeros.eval);
	}

	@Test
	public void testAdd() {
		AddMulConcatEval e1 = new AddMulConcatEval(123);
		AddMulConcatEval e2 = new AddMulConcatEval(456);
		AddMulConcatEval added = AddMulConcatEval.add(e1, e2);
		assertEquals(123 + 456, added.eval);
	}

	@Test
	public void testMul() {
		AddMulConcatEval e1 = new AddMulConcatEval(12);
		AddMulConcatEval e2 = new AddMulConcatEval(34);
		AddMulConcatEval muled = AddMulConcatEval.mul(e1, e2);
		assertEquals(12 * 34, muled.eval);
	}

	@Test
	public void testComplexExpressions() {
		// a + b * c
		// e.g. 10 + 20 * 30 = 610
		AddMulConcatEval a = new AddMulConcatEval(10);
		AddMulConcatEval b = new AddMulConcatEval(20);
		AddMulConcatEval c = new AddMulConcatEval(30);

		AddMulConcatEval bc = AddMulConcatEval.mul(b, c);
		AddMulConcatEval expr1 = AddMulConcatEval.add(a, bc);
		assertEquals(10 + 20 * 30, expr1.eval);

		// Because multiplication has higher precedence than addition,
		// add(a, b) followed by mul(..., c) evaluates to a + b * c = 10 + 20 * 30 = 610
		AddMulConcatEval ab = AddMulConcatEval.add(a, b);
		AddMulConcatEval expr2 = AddMulConcatEval.mul(ab, c);
		assertEquals(10 + 20 * 30, expr2.eval);

		// Concat within addition and multiplication
		AddMulConcatEval c1 = new AddMulConcatEval(1);
		AddMulConcatEval c2 = new AddMulConcatEval(2);
		AddMulConcatEval c3 = new AddMulConcatEval(3);

		// 12 + 3 = 15
		AddMulConcatEval c12 = AddMulConcatEval.concat(c1, c2);
		AddMulConcatEval expr3 = AddMulConcatEval.add(c12, c3);
		assertEquals(15, expr3.eval);

		// 1 + 23 = 24
		AddMulConcatEval c23 = AddMulConcatEval.concat(c2, c3);
		AddMulConcatEval expr4 = AddMulConcatEval.add(c1, c23);
		assertEquals(24, expr4.eval);

		// 1 + 2 * 34 = 1 + 68 = 69
		AddMulConcatEval c34 = AddMulConcatEval.concat(new AddMulConcatEval(3), new AddMulConcatEval(4));
		AddMulConcatEval expr5 = AddMulConcatEval.add(c1, AddMulConcatEval.mul(c2, c34));
		assertEquals(1 + 2 * 34, expr5.eval);
	}
}
