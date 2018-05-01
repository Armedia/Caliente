package com.armedia.caliente.engine.xml;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.transform.TestObjectContext;
import com.armedia.caliente.store.CmfDataType;

public class ExpressionTest {

	@Test
	public void testNull() {
		Assert.assertNull(Expression.NULL.getLang());
		Assert.assertNull(Expression.NULL.getScript());
		Expression.NULL.setLang(UUID.randomUUID().toString());
		Assert.assertNull(Expression.NULL.getLang());
		Expression.NULL.setScript(UUID.randomUUID().toString());
		Assert.assertNull(Expression.NULL.getScript());
	}

	@Test
	public void testValue() throws Exception {
		DynamicElementContext ctx = new TestObjectContext();
		Expression e = null;

		e = new Expression();
		Assert.assertNull(e.getLang());
		Assert.assertNull(e.getScript());
		Assert.assertNull(Expression.eval(e, ctx));

		String value = "               test script # 1            ";
		e.setScript(value);
		Assert.assertEquals(value, e.getScript());
		Assert.assertEquals(value, Expression.eval(e, ctx));

		for (int i = 0; i < 10; i++) {
			value = UUID.randomUUID().toString();
			e.setScript(value);
			Assert.assertEquals(value, e.getScript());
			Assert.assertEquals(value, Expression.eval(e, ctx));
		}
	}

	@Test
	public void testLang() {
		Expression e = null;

		ScriptEngineManager manager = new ScriptEngineManager();
		Set<String> languages = new TreeSet<>();
		for (ScriptEngineFactory f : manager.getEngineFactories()) {
			languages.addAll(f.getNames());
		}

		e = new Expression();
		Assert.assertNull(e.getLang());

		for (String l : languages) {
			try {
				e.setLang(l);
				Assert.assertEquals(l, e.getLang());
			} catch (IllegalArgumentException ex) {
				Assert.fail(String.format("Failed with known language [%s]", l));
			}
		}

		try {
			String badLang = String.format("This language certainly does not exist %s", UUID.randomUUID().toString());
			e.setLang(badLang);
			Assert.fail(String.format("Did not fail with known-bad language [%s]", badLang));
		} catch (IllegalArgumentException ex) {
			// All is well
		}
	}

	@Test
	public void testEval() throws Exception {
		ScriptEngineManager manager = new ScriptEngineManager();
		Set<String> languages = new TreeSet<>();
		for (ScriptEngineFactory f : manager.getEngineFactories()) {
			languages.addAll(f.getNames());
		}

		DynamicElementContext ctx = new TestObjectContext();

		Expression.eval(null, null);

		Assert.assertNull(Expression.eval(null, ctx));

		Random random = new Random(System.currentTimeMillis());
		if (languages.contains("js")) {
			Expression e = new Expression();
			e.setLang("js");

			Object expected = null;
			String script = null;
			Object actual = null;

			// Test JavaScript
			expected = UUID.randomUUID().toString();
			script = String.format("'%s'", expected);
			e.setScript(script);
			actual = Expression.eval(e, ctx);
			Assert.assertEquals(expected, actual);

			script = "vars.get('testValue').value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfDataType.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assert.assertEquals(expectedInt, n.intValue());
				} else {
					Assert.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
				}
			}
		}

		if (languages.contains("groovy")) {
			// Test Groovy
			Expression e = new Expression();
			e.setLang("groovy");

			Object expected = null;
			String script = null;
			Object actual = null;

			// Test Groovy syntax
			expected = UUID.randomUUID().toString();
			script = String.format("def tester = []\ntester << \"%s\"\ntester[0]", expected);
			e.setScript(script);
			actual = Expression.eval(e, ctx);
			Assert.assertEquals(expected, actual);

			for (int i = 0; i < 100; i++) {
				expected = random.nextInt(Integer.MAX_VALUE);
				script = String.format("def tester = []\ntester << %s\ntester[0]", expected);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assert.assertEquals(expected, actual);
			}

			script = "def tester = vars['testValue'].value\ntester";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfDataType.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assert.assertEquals(expectedInt, n.intValue());
				} else {
					Assert.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
				}
			}
		}

		if (languages.contains("bsh")) {
			// Test Beanshell
			Expression e = new Expression();
			e.setLang("groovy");

			Object expected = null;
			String script = null;
			Object actual = null;

			// Test Beanshell syntax
			expected = UUID.randomUUID().toString();
			script = String.format("return new java.lang.String(\"%s\").hashCode()", expected);
			e.setScript(script);
			actual = Expression.eval(e, ctx);
			Assert.assertEquals(expected.hashCode(), actual);

			for (int i = 0; i < 100; i++) {
				int number = random.nextInt(Integer.MAX_VALUE);
				script = String.format("return (%d >> 1)", number);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assert.assertEquals(number >> 1, actual);
			}

			script = "return vars.testValue.value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfDataType.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assert.assertEquals(expectedInt, n.intValue());
				} else {
					Assert.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
				}
			}
		}

		if (languages.contains("jexl")) {
			// Test Jexl2
			Expression e = new Expression();
			e.setLang("jexl");

			Object expected = null;
			String script = null;
			Object actual = null;

			// Test Jexl2 syntax
			expected = UUID.randomUUID().toString();
			script = String.format("'%s'", expected);
			e.setScript(script);
			actual = Expression.eval(e, ctx);
			Assert.assertEquals(expected, actual);

			for (int i = 0; i < 100; i++) {
				int number = random.nextInt(Integer.MAX_VALUE);
				script = String.format("(%d / 2)", number);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assert.assertEquals(number >> 1, actual);
			}

			script = "vars.testValue.value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfDataType.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assert.assertEquals(expectedInt, n.intValue());
				} else {
					Assert.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
				}
			}
		}
	}

	@Test
	public void listEngines() throws Exception {
		ScriptEngineManager manager = new ScriptEngineManager();
		for (ScriptEngineFactory f : manager.getEngineFactories()) {
			String name = f.getEngineName();
			String ver = f.getEngineVersion();
			String lang = f.getLanguageName();
			String lver = f.getLanguageVersion();

			System.out.printf("Engine  : %s %s%n", name, ver);
			System.out.printf("Language: %s %s%n", lang, lver);
			System.out.printf("Aliases : %s%n", f.getNames());
		}
	}
}
