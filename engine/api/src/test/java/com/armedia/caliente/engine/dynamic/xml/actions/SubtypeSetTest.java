package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;

public class SubtypeSetTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();
		Assertions.assertNull(object.getSubtype());

		SubtypeSet action = new SubtypeSet();
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null expression");
		} catch (ActionException e) {
			// All is well
		}

		String value = UUID.randomUUID().toString();
		action.setName(new Expression(value));

		action.apply(ctx);

		Assertions.assertEquals(value, object.getSubtype());

		action.setName(new Expression(
			"                                                       \n   \t   \n                       \t \t   \n                               "));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a blank-valued expression");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(new Expression());
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null-valued expression");
		} catch (ActionException e) {
			// All is well
		}
	}
}