package com.armedia.caliente.engine.dynamic.xml.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.RegularExpression;

public class SubtypeReplaceTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();
		object.setSubtype("dctm:test_subtype_value");

		SubtypeReplace action = new SubtypeReplace();

		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with neither a regex or a replacement");
		} catch (ActionException e) {
			// All is well
		}

		action.setRegex(new RegularExpression("^dctm:"));

		action.apply(ctx);
		Assertions.assertEquals("test_subtype_value", object.getSubtype());

		// Reset the value...
		object.setSubtype("dctm:test_subtype_value");

		action.setReplacement(new Expression("alfresco||"));

		action.apply(ctx);
		Assertions.assertEquals("alfresco||test_subtype_value", object.getSubtype());

		action.setRegex(new RegularExpression("(test_)(subtype)(_value)"));
		action.setReplacement(new Expression("$3$1$2"));
		action.apply(ctx);
		Assertions.assertEquals("alfresco||_valuetest_subtype", object.getSubtype());
	}
}