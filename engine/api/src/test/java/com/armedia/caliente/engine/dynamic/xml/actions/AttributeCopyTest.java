package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.store.CmfValue.Type;

public class AttributeCopyTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		final String srcName = "oldAttribute";
		final String tgtName = "newAttribute";
		final UUID uuid = UUID.randomUUID();
		final DynamicValue v = new DynamicValue(srcName, Type.OTHER, true);
		v.setValue(uuid);
		object.getAtt().put(srcName, v);

		AttributeActions.Copy action = new AttributeActions.Copy();

		action.setFrom(null);
		action.setTo(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null to and from");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(null);
		action.setTo(new Expression(tgtName));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null from");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(new Expression(srcName));
		action.setTo(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null to");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(new Expression(srcName));
		action.setTo(new Expression(tgtName));

		Assertions.assertTrue(object.getAtt().containsKey(srcName));
		Assertions.assertFalse(object.getAtt().containsKey(tgtName));
		Assertions.assertSame(uuid, object.getAtt().get(srcName).getValue());
		action.apply(ctx);
		Assertions.assertTrue(object.getAtt().containsKey(srcName));
		Assertions.assertSame(uuid, object.getAtt().get(srcName).getValue());
		Assertions.assertTrue(object.getAtt().containsKey(tgtName));
		Assertions.assertSame(uuid, object.getAtt().get(tgtName).getValue());
		Assertions.assertNotSame(object.getAtt().get(srcName), object.getAtt().get(tgtName));
	}
}