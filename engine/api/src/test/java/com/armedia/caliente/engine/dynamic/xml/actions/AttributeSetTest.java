package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;

public class AttributeSetTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		AttributeActions.Set action = new AttributeActions.Set();
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name");
		} catch (ActionException e) {
			// All is well
		}

		final String attributeName = "testAttribute";
		final String attributeValue = UUID.randomUUID().toString();

		action.setName(new Expression(attributeName));
		action.setValue(new Expression(attributeValue));

		Assertions.assertFalse(object.getAtt().containsKey(attributeName));
		action.apply(ctx);
		Assertions.assertTrue(object.getAtt().containsKey(attributeName));
		Assertions.assertEquals(attributeValue, object.getAtt().get(attributeName).getValue());
	}
}