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

public class AttributeRenameTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		final String originalName = "oldAttribute";
		final String newName = "newAttribute";
		final UUID uuid = UUID.randomUUID();
		final DynamicValue v = new DynamicValue(originalName, Type.OTHER, true);
		v.setValue(uuid);
		object.getAtt().put(originalName, v);

		AttributeActions.Rename action = new AttributeActions.Rename();

		action.setFrom(null);
		action.setTo(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null to and from");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(null);
		action.setTo(new Expression(newName));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null from");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(new Expression(originalName));
		action.setTo(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null to");
		} catch (ActionException e) {
			// All is well
		}

		action.setFrom(new Expression(originalName));
		action.setTo(new Expression(newName));

		Assertions.assertTrue(object.getAtt().containsKey(originalName));
		Assertions.assertFalse(object.getAtt().containsKey(newName));
		Assertions.assertSame(uuid, object.getAtt().get(originalName).getValue());
		action.apply(ctx);
		Assertions.assertFalse(object.getAtt().containsKey(originalName));
		Assertions.assertTrue(object.getAtt().containsKey(newName));
		Assertions.assertSame(uuid, object.getAtt().get(newName).getValue());

		action.setFrom(new Expression(newName));
		action.setTo(new Expression(originalName));
		Assertions.assertFalse(object.getAtt().containsKey(originalName));
		Assertions.assertTrue(object.getAtt().containsKey(newName));
		Assertions.assertSame(uuid, object.getAtt().get(newName).getValue());
		action.apply(ctx);
		Assertions.assertTrue(object.getAtt().containsKey(originalName));
		Assertions.assertFalse(object.getAtt().containsKey(newName));
		Assertions.assertSame(uuid, object.getAtt().get(originalName).getValue());
	}
}