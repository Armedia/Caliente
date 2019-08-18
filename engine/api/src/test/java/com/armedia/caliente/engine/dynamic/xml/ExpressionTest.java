/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.store.CmfValue;

public class ExpressionTest {

	@Test
	public void testNull() {
		Assertions.assertNull(Expression.NULL.getLang());
		Assertions.assertNull(Expression.NULL.getScript());
		Expression.NULL.setLang(UUID.randomUUID().toString());
		Assertions.assertNull(Expression.NULL.getLang());
		Expression.NULL.setScript(UUID.randomUUID().toString());
		Assertions.assertNull(Expression.NULL.getScript());
	}

	@Test
	public void testValue() throws Exception {
		DynamicElementContext ctx = new TestObjectContext();
		Expression e = null;

		e = new Expression();
		Assertions.assertNull(e.getLang());
		Assertions.assertNull(e.getScript());
		Assertions.assertNull(Expression.eval(e, ctx));

		String value = "               test script # 1            ";
		e.setScript(value);
		Assertions.assertEquals(value, e.getScript());
		Assertions.assertEquals(value, Expression.eval(e, ctx));

		for (int i = 0; i < 10; i++) {
			value = UUID.randomUUID().toString();
			e.setScript(value);
			Assertions.assertEquals(value, e.getScript());
			Assertions.assertEquals(value, Expression.eval(e, ctx));
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
		Assertions.assertNull(e.getLang());

		for (String l : languages) {
			try {
				e.setLang(l);
				Assertions.assertEquals(l, e.getLang());
			} catch (IllegalArgumentException ex) {
				Assertions.fail(String.format("Failed with known language [%s]", l));
			}
		}

		try {
			String badLang = String.format("This language certainly does not exist %s", UUID.randomUUID().toString());
			e.setLang(badLang);
			Assertions.fail(String.format("Did not fail with known-bad language [%s]", badLang));
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

		Assertions.assertNull(Expression.eval(null, ctx));

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
			Assertions.assertEquals(expected, actual);

			script = "vars.get('testValue').value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfValue.Type.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assertions.assertEquals(expectedInt, n.intValue());
				} else {
					Assertions
						.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
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
			Assertions.assertEquals(expected, actual);

			for (int i = 0; i < 100; i++) {
				expected = random.nextInt(Integer.MAX_VALUE);
				script = String.format("def tester = []\ntester << %s\ntester[0]", expected);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assertions.assertEquals(expected, actual);
			}

			script = "def tester = vars['testValue'].value\ntester";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfValue.Type.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assertions.assertEquals(expectedInt, n.intValue());
				} else {
					Assertions
						.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
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
			Assertions.assertEquals(expected.hashCode(), actual);

			for (int i = 0; i < 100; i++) {
				int number = random.nextInt(Integer.MAX_VALUE);
				script = String.format("return (%d >> 1)", number);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assertions.assertEquals(number >> 1, actual);
			}

			script = "return vars.testValue.value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfValue.Type.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assertions.assertEquals(expectedInt, n.intValue());
				} else {
					Assertions
						.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
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
			Assertions.assertEquals(expected, actual);

			for (int i = 0; i < 100; i++) {
				int number = random.nextInt(Integer.MAX_VALUE);
				script = String.format("(%d / 2)", number);
				e.setScript(script);
				actual = Expression.eval(e, ctx);
				Assertions.assertEquals(number >> 1, actual);
			}

			script = "vars.testValue.value";
			e.setScript(script);
			for (int i = 0; i < 99; i++) {
				int expectedInt = (random.nextInt(10000) * 1000) + i;
				DynamicValue value = new DynamicValue("testValue", CmfValue.Type.INTEGER, false);
				value.setValue(expectedInt);
				ctx.getVariables().put("testValue", value);
				actual = Expression.eval(e, ctx);
				if (Number.class.isInstance(actual)) {
					Number n = Number.class.cast(actual);
					Assertions.assertEquals(expectedInt, n.intValue());
				} else {
					Assertions
						.fail(String.format("Expected the integer number [%s] but got [%s]", expectedInt, actual));
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
