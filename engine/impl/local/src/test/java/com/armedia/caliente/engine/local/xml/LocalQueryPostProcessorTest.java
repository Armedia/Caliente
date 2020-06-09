package com.armedia.caliente.engine.local.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalQueryPostProcessorTest {

	public static class LQPPTest implements LocalQueryPostProcessor.Processor {
		@Override
		public String process(String value) throws Exception {
			return LocalQueryPostProcessorTest.lqppTestProcess(value);
		}
	}

	private static String lqppTestProcess(String value) {
		return "<<" + value + ">>";
	}

	public static String testProcess(String value) {
		return "[[" + value + "]]";
	}

	public static void badMethod(String value, String another) {
		// do nothing
	}

	@Test
	public void testValue() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		Assertions.assertThrows(NullPointerException.class, () -> lqpp.setValue(null));

		for (int i = 0; i < 100; i++) {
			String v = String.format("value-#%02d", i);
			lqpp.setValue(v);
			Assertions.assertEquals(v, lqpp.getValue());
		}
	}

	@Test
	public void testType() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		Assertions.assertThrows(NullPointerException.class, () -> lqpp.setValue(null));

		for (int i = 0; i < 100; i++) {
			String v = String.format("type-#%02d", i);
			lqpp.setType(v);
			Assertions.assertEquals(v, lqpp.getType());
		}
	}

	@Test
	public void testReset() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		Assertions.assertThrows(NullPointerException.class, () -> lqpp.setValue(null));

		for (int i = 0; i < 100; i++) {
			String v = String.format("type-#%02d", i);
			lqpp.setType(v);
			Assertions.assertEquals(v, lqpp.getType());
		}
	}

	@Test
	public void testPostProcess() throws Exception {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		// First things first: try with nothing, ensure it explodes
		Assertions.assertThrows(IllegalArgumentException.class, () -> lqpp.postProcess("TEST"));

		// Next, ensure it returns the same value from the given script
		lqpp.setType("jexl3");
		for (int i = 0; i < 10; i++) {
			String expected = "Value-" + i;
			lqpp.setValue("return 'Value-' + path;");
			Assertions.assertEquals(expected, lqpp.postProcess(String.valueOf(i)));
		}

		lqpp.setType("class");

		// First, just the classname
		lqpp.setValue(LQPPTest.class.getName());
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Assertions.assertEquals(LocalQueryPostProcessorTest.lqppTestProcess(path), lqpp.postProcess(path));
		}

		// Next, className & method
		lqpp.setValue(getClass().getName() + "::testProcess");
		for (int i = 0; i < 10; i++) {
			String path = String.format("path-number-%02d", i);
			Assertions.assertEquals(LocalQueryPostProcessorTest.testProcess(path), lqpp.postProcess(path));
		}

		// a class that doesn't exist
		lqpp.setValue("this.class.does.not.exist.ever.default.for.while.Class");
		Assertions.assertThrows(ClassNotFoundException.class, () -> lqpp.postProcess("kaka"));

		// bad class syntaxes
		lqpp.setValue("this classname is invalid");
		Assertions.assertThrows(IllegalArgumentException.class, () -> lqpp.postProcess("kaka"));
		lqpp.setValue("com.some.class.Name:");
		Assertions.assertThrows(IllegalArgumentException.class, () -> lqpp.postProcess("kaka"));
		lqpp.setValue("com.some.class.Name::");
		Assertions.assertThrows(IllegalArgumentException.class, () -> lqpp.postProcess("kaka"));
		lqpp.setValue("");
		Assertions.assertThrows(IllegalArgumentException.class, () -> lqpp.postProcess("kaka"));

		// Make sure the same exception is tossed up at us if we have an issue
		lqpp.setValue("this classname is invalid");
		Exception thrown = null;
		try {
			lqpp.postProcess("kaka");
		} catch (Exception e) {
			thrown = e;
		}
		try {
			lqpp.postProcess("kaka");
		} catch (Exception e) {
			Assertions.assertSame(thrown, e);
		}

		// a class that doesn't implement Processor
		lqpp.setValue("java.lang.Object");
		Assertions.assertThrows(ClassCastException.class, () -> lqpp.postProcess("kaka"));

		// a method that doesn't exist
		lqpp.setValue("java.lang.Object::process");
		Assertions.assertThrows(NoSuchMethodException.class, () -> lqpp.postProcess("kaka"));

		// a method that has the wrong signature
		lqpp.setValue(getClass().getName() + "::badMethod");
		Assertions.assertThrows(NoSuchMethodException.class, () -> lqpp.postProcess("kaka"));
	}

}