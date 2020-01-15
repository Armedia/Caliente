package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue.Type;

public class VariableSplitTest {
	@Test
	public void test() throws ActionException {
		final Random r = new Random(System.nanoTime());
		TestObjectContext ctx = new TestObjectContext();

		VariableSplit action = new VariableSplit();
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name");
		} catch (ActionException e) {
			// All is well
		}

		final String attributeName = "testAttribute";

		final String separators = "`~!@#$%^&*()_+[]\\{}|;':\",./<>?";

		action.setName(new Expression(attributeName));

		action.setKeepEmpty(true);
		for (int i = 0; i < separators.length(); i++) {
			final char sep = separators.charAt(i);
			action.setSeparator(new Expression(String.valueOf(sep)));
			DynamicValue orig = new DynamicValue(attributeName, Type.STRING, true);
			ctx.getVariables().put(orig.getName(), orig);

			// Render the splittable value, and render its split version
			List<String> baseStrings = new ArrayList<>(8);
			List<Object> finalStrings = new ArrayList<>(8);
			for (int j = 0; j < 8; j++) {
				String str = String.format("%016x", r.nextLong());
				finalStrings.add(str);
				if ((i % 2) == 0) {
					str = String.format("%s%s", str, sep);
					finalStrings.add(StringUtils.EMPTY);
				}
				baseStrings.add(str);
			}
			// baseStrings contains 8 values, while finalStrings contains 8
			orig.setValues(baseStrings);

			action.apply(ctx);
			DynamicValue result = ctx.getVariables().get(attributeName);
			Assertions.assertEquals(finalStrings.size(), result.getSize(), () -> String.format("Separator: %s", sep));
			Assertions.assertEquals(finalStrings, result.getValues(), () -> String.format("Separator: %s", sep));
		}

		action.setKeepEmpty(false);
		for (int i = 0; i < separators.length(); i++) {
			final char sep = separators.charAt(i);
			action.setSeparator(new Expression(String.valueOf(sep)));
			DynamicValue orig = new DynamicValue(attributeName, Type.STRING, true);
			ctx.getVariables().put(orig.getName(), orig);

			// Render the splittable value, and render its split version
			List<String> baseStrings = new ArrayList<>(8);
			List<Object> finalStrings = new ArrayList<>(8);
			for (int j = 0; j < 8; j++) {
				String str = String.format("%016x", r.nextLong());
				finalStrings.add(str);
				if ((i % 2) == 0) {
					str = String.format("%s%s", str, sep);
				}
				baseStrings.add(str);
			}
			// baseStrings contains 8 values, while finalStrings contains 8
			orig.setValues(baseStrings);

			action.apply(ctx);
			DynamicValue result = ctx.getVariables().get(attributeName);
			Assertions.assertEquals(finalStrings.size(), result.getSize(), () -> String.format("Separator: %s", sep));
			Assertions.assertEquals(finalStrings, result.getValues(), () -> String.format("Separator: %s", sep));
		}

		for (int i = 0; i < separators.length(); i++) {
			final char sep = separators.charAt(i);
			action.setSeparator(new Expression(String.valueOf(sep)));
			DynamicValue orig = new DynamicValue(attributeName, Type.STRING, true);
			ctx.getVariables().put(orig.getName(), orig);

			// Render the splittable value, and render its split version
			List<String> baseStrings = new ArrayList<>(8);
			List<Object> finalStrings = new ArrayList<>(8);
			for (int j = 0; j < 8; j++) {
				String str = String.format("%016x", r.nextLong());
				finalStrings.add(str);
				if ((i % 2) == 0) {
					String str2 = String.format("%016x", r.nextLong());
					finalStrings.add(str2);
					str = String.format("%s%s%s", str, sep, str2);
				}
				baseStrings.add(str);
			}
			// baseStrings contains 8 values, while finalStrings contains 12
			orig.setValues(baseStrings);

			action.apply(ctx);
			DynamicValue result = ctx.getVariables().get(attributeName);
			Assertions.assertEquals(finalStrings.size(), result.getSize(), () -> String.format("Separator: %s", sep));
			Assertions.assertEquals(finalStrings, result.getValues(), () -> String.format("Separator: %s", sep));
		}

		for (int i = 0; i < separators.length(); i++) {
			final char sep = separators.charAt(i);
			action.setSeparator(new Expression(String.valueOf(sep)));
			DynamicValue orig = new DynamicValue(attributeName, Type.STRING, true);
			ctx.getVariables().put(orig.getName(), orig);

			// Render the splittable value, and render its split version
			List<String> baseStrings = new ArrayList<>(8);
			List<Object> finalStrings = new ArrayList<>(8);
			for (int j = 0; j < 8; j++) {
				String str = String.format("%016x", r.nextLong());
				if ((i % 2) == 0) {
					String str2 = String.format("%016x", r.nextLong());
					finalStrings.add(String.format("%s%s%s", str, sep, str2));
					str = String.format("%s\\%s%s", str, sep, str2);
				} else {
					finalStrings.add(str);
				}
				baseStrings.add(str);
			}
			// baseStrings contains 8 values, while finalStrings contains 12
			orig.setValues(baseStrings);

			action.apply(ctx);
			DynamicValue result = ctx.getVariables().get(attributeName);
			Assertions.assertEquals(finalStrings.size(), result.getSize(), () -> String.format("Separator: %s", sep));
			Assertions.assertEquals(finalStrings, result.getValues(), () -> String.format("Separator: %s", sep));
		}
	}
}