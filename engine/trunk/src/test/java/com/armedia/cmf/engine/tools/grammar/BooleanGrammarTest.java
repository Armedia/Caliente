package com.armedia.cmf.engine.tools.grammar;

import java.io.StringReader;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.armedia.cmf.engine.tools.BooleanContext;
import com.armedia.cmf.engine.tools.BooleanExpression;
import com.armedia.cmf.engine.tools.BooleanExpressionFactory;
import com.armedia.cmf.engine.tools.BooleanUtils;
import com.armedia.cmf.engine.tools.NameExistsExpression;
import com.armedia.commons.utilities.Tools;

public class BooleanGrammarTest {

	private static class ConstantFactory implements BooleanExpressionFactory<BooleanContext> {
		@Override
		public BooleanExpression<BooleanContext> buildExpression(String name) {
			Assert.assertNotNull(name);
			return Boolean.valueOf(name.toLowerCase()) ? BooleanUtils.getTrue() : BooleanUtils.getFalse();
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private void test(Object[][] tests) throws Exception {
		for (Object[] t : tests) {
			final BooleanGrammar<BooleanContext> g = new BooleanGrammar<BooleanContext>(
				new StringReader(t[0].toString()));
			final BooleanExpression<BooleanContext> exp;
			try {
				exp = g.parse(new ConstantFactory());
				if (t.length == 1) {
					Assert.fail(String.format("Expression [%s] was expected to fail, but didn't", t[0]));
				}
			} catch (ParseException e) {
				if (t.length == 1) {
					Assert.fail(String.format("Expression [%s] was expected to fail, but didn't", t[0]));
				}
				throw new Exception(String.format("Failed to parse expression [%s]", t[0]), e);
			}
			Assert.assertEquals(t[0].toString(), t[1], exp.evaluate(null));
		}
	}

	// @Test
	public void testNOT() throws Exception {
		Object[][] tests = {
			{
				"!true", false
			}, {
				"!(true)", false
			}, {
				"!(!true)", true
			}, {
				"!!false", true
			}, {
				"!(!(!(!(true))))", true
			}, {
				"(!(!(!(!(true)))))", true
			}, {
				"!false", true
			}, {
				"!(false)", true
			}, {
				"!(!false)", false
			}, {
				"!(!(!(!(false))))", false
			}, {
				"(!(!(!(!(false)))))", false
			}
		};
		test(tests);
	}

	@Test
	public void testAND() throws Exception {
		Object[][] tests = {
			{
				"false*false*false*false", false
			}, {
				"false*false*false*true", false
			}, {
				"false*false*true*false", false
			}, {
				"false*false*true*true", false
			}, {
				"false*true*false*false", false
			}, {
				"false*true*false*true", false
			}, {
				"false*true*true*false", false
			}, {
				"false*true*true*true", false
			}, {
				"true*false*false*false", false
			}, {
				"true*false*false*true", false
			}, {
				"true*false*true*false", false
			}, {
				"true*false*true*true", false
			}, {
				"true*true*false*false", false
			}, {
				"true*true*false*true", false
			}, {
				"true*true*true*false", false
			}, {
				"true*true*true*true", true
			}
		};
		test(tests);
	}

	@Test
	public void testOR() throws Exception {
		Object[][] tests = {
			{
				"false+false+false+false", false
			}, {
				"false+false+false+true", true
			}, {
				"false+false+true+false", true
			}, {
				"false+false+true+true", true
			}, {
				"false+true+false+false", true
			}, {
				"false+true+false+true", true
			}, {
				"false+true+true+false", true
			}, {
				"false+true+true+true", true
			}, {
				"true+false+false+false", true
			}, {
				"true+false+false+true", true
			}, {
				"true+false+true+false", true
			}, {
				"true+false+true+true", true
			}, {
				"true+true+false+false", true
			}, {
				"true+true+false+true", true
			}, {
				"true+true+true+false", true
			}, {
				"true+true+true+true", true
			}
		};
		test(tests);
	}

	@Test
	public void testXOR() throws Exception {
		Object[][] tests = {
			{
				"false^false^false^false", false
			}, {
				"false^false^false^true", true
			}, {
				"false^false^true^false", true
			}, {
				"false^false^true^true", false
			}, {
				"false^true^false^false", true
			}, {
				"false^true^false^true", false
			}, {
				"false^true^true^false", false
			}, {
				"false^true^true^true", true
			}, {
				"true^false^false^false", true
			}, {
				"true^false^false^true", false
			}, {
				"true^false^true^false", false
			}, {
				"true^false^true^true", true
			}, {
				"true^true^false^false", false
			}, {
				"true^true^false^true", true
			}, {
				"true^true^true^false", true
			}, {
				"true^true^true^true", false
			}
		};
		test(tests);
	}

	@Test
	public void testPrecedence() throws Exception {
		String ex1 = "A+B*C";
		String ex2 = "A*B+C";

		boolean[][] tests = {
			{
				false, false, false, false, false
			}, {
				false, false, true, false, true
			}, {
				false, true, false, false, false
			}, {
				false, true, true, true, true
			}, {
				true, false, false, true, false
			}, {
				true, false, true, true, true
			}, {
				true, true, false, true, true
			}, {
				true, true, true, true, true
			}
		};

		final BooleanExpressionFactory<BooleanContext> f = new BooleanExpressionFactory<BooleanContext>() {
			@Override
			public BooleanExpression<BooleanContext> buildExpression(String name) {
				Assert.assertNotNull(name);
				return new NameExistsExpression<BooleanContext>(name) {
					@Override
					public boolean evaluate(BooleanContext c) {
						return super.evaluate(c) && Boolean.valueOf(Tools.toString(c.getValue(getName())));
					}
				};
			}
		};

		BooleanExpression<BooleanContext> e1 = new BooleanGrammar<BooleanContext>(new StringReader(ex1)).parse(f);
		BooleanExpression<BooleanContext> e2 = new BooleanGrammar<BooleanContext>(new StringReader(ex2)).parse(f);

		Map<String, Object> m = new TreeMap<String, Object>();
		for (boolean[] t : tests) {
			m.put("A", t[0]);
			m.put("B", t[1]);
			m.put("C", t[2]);
			final BooleanContext c = new BooleanContext(m);
			Assert.assertEquals(String.format("E=[%s] S=%s", ex1, m), t[3], e1.evaluate(c));
			Assert.assertEquals(String.format("E=[%s] S=%s", ex2, m), t[4], e2.evaluate(c));
		}
	}
}