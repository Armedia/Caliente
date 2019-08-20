package com.armedia.caliente.engine.dynamic.xml.actions;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;

public class SecondarySubtypeAddTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		SecondarySubtypeAdd action = new SecondarySubtypeAdd();
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().isEmpty());

		action.setName(new Expression());
		action.apply(ctx);
		Assertions.assertTrue(object.getSecondarySubtypes().isEmpty());

		String testValue = UUID.randomUUID().toString();
		action.setName(new Expression(testValue));
		action.apply(ctx);
		Assertions.assertFalse(object.getSecondarySubtypes().isEmpty());
		Assertions.assertTrue(object.getSecondarySubtypes().contains(testValue));
	}
}