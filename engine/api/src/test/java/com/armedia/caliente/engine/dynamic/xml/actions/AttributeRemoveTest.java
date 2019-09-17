package com.armedia.caliente.engine.dynamic.xml.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue.Type;

public class AttributeRemoveTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		AttributeRemove action = new AttributeRemove();
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name");
		} catch (ActionException e) {
			// All is well
		}

		final String attributeName = "testAttribute";

		action.setName(new Expression(attributeName));
		object.getAtt().put(attributeName, new DynamicValue(attributeName, Type.BOOLEAN, true));

		Assertions.assertTrue(object.getAtt().containsKey(attributeName));
		action.apply(ctx);
		Assertions.assertFalse(object.getAtt().containsKey(attributeName));
	}
}