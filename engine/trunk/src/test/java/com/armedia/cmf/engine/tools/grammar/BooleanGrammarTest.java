package com.armedia.cmf.engine.tools.grammar;

import java.io.StringReader;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.armedia.cmf.engine.tools.BooleanExpression;

public class BooleanGrammarTest {

	private static final String[] NAMES = {
		"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliett", "Kilo", "Lima",
		"Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey",
		"XRay", "Yankee", "Zulu"
	};

	private static final Random RANDOM = new Random(System.currentTimeMillis());

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private void test(String[] exp) throws Exception {
		for (String s : exp) {
			BooleanGrammar g = new BooleanGrammar(new StringReader(s)) {
				@Override
				protected BooleanExpression buildNamedExpression(String name) {
					Assert.assertNotNull(name);
					return BooleanExpression.TRUE;
				}

			};
			BooleanExpression e = g.parse();
			String r = e.toString();
			Assert.assertNotNull(r);
		}
	}

	@Test
	public void testParse() throws Exception {
		String[] expressions = {
			"Alpha", "!Alpha", "!   Alpha     ", "    !  Alpha      ",
		};
		test(expressions);
	}
}