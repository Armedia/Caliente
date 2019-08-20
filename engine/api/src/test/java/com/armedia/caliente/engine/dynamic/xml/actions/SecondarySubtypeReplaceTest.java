package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.RegularExpression;

public class SecondarySubtypeReplaceTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		SecondarySubtypeReplace action = new SecondarySubtypeReplace();
		action.apply(ctx);

		Set<String> secondaries = object.getSecondarySubtypes();
		Set<String> values = new TreeSet<>(Arrays.asList("abc123hij", "123def789", "1b3d5f7h9j"));
		secondaries.addAll(values);

		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with neither a regex or a replacement");
		} catch (ActionException e) {
			// All is well
		}

		action.setRegex(new RegularExpression("\\d"));

		secondaries.clear();
		secondaries.addAll(values);
		action.apply(ctx);
		Assertions.assertTrue(secondaries.contains("abchij"));
		Assertions.assertTrue(secondaries.contains("def"));
		Assertions.assertTrue(secondaries.contains("bdfhj"));

		action.setReplacement(new Expression("X"));

		secondaries.clear();
		secondaries.addAll(values);
		action.apply(ctx);
		Assertions.assertTrue(secondaries.contains("abcXXXhij"));
		Assertions.assertTrue(secondaries.contains("XXXdefXXX"));
		Assertions.assertTrue(secondaries.contains("XbXdXfXhXj"));

		secondaries.clear();

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(true));
		secondaries.addAll(values);
		action.apply(ctx);
		Assertions.assertEquals(values, secondaries);

		action.setRegex(new RegularExpression("[ABCDEFGHIJ]").setCaseSensitive(false));
		action.apply(ctx);
		Assertions.assertTrue(secondaries.contains("XXX123XXX"));
		Assertions.assertTrue(secondaries.contains("123XXX789"));
		Assertions.assertTrue(secondaries.contains("1X3X5X7X9X"));
	}
}