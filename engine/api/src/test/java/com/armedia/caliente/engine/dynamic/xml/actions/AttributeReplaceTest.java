package com.armedia.caliente.engine.dynamic.xml.actions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicValue;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.TestObjectFacade;
import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.caliente.engine.dynamic.xml.RegularExpression;
import com.armedia.caliente.store.CmfValue.Type;

public class AttributeReplaceTest {
	@Test
	public void test() throws ActionException {
		TestObjectContext ctx = new TestObjectContext();
		TestObjectFacade object = ctx.getDynamicObject();

		final String attribute = "targetAttribute";
		final String regex = "12345";
		final String initialValue = "!@#$%12345^&*()";
		final String replacement = "abcde";
		final String finalValue = "!@#$%abcde^&*()";
		final String finalValueRemoved = "!@#$%^&*()";
		final DynamicValue v = new DynamicValue(attribute, Type.STRING, true);
		v.setValue(initialValue);
		object.getAtt().put(attribute, v);

		AttributeReplace action = new AttributeReplace();

		action.setName(null);
		action.setRegex(null);
		action.setReplacement(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name, regex, and replacement");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(null);
		action.setRegex(null);
		action.setReplacement(new Expression(replacement));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name, and regex");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(null);
		action.setRegex(new RegularExpression(regex));
		action.setReplacement(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name, and replacement");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(null);
		action.setRegex(new RegularExpression(regex));
		action.setReplacement(new Expression(replacement));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null name");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(new Expression(attribute));
		action.setRegex(null);
		action.setReplacement(null);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null regex, and replacement");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(new Expression(attribute));
		action.setRegex(null);
		action.setReplacement(new Expression(replacement));
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with a null regex");
		} catch (ActionException e) {
			// All is well
		}

		action.setName(new Expression(attribute));
		action.setRegex(new RegularExpression(regex));
		action.setReplacement(new Expression(replacement));

		Assertions.assertTrue(object.getAtt().containsKey(attribute));
		Assertions.assertEquals(initialValue, object.getAtt().get(attribute).getValue());
		action.apply(ctx);
		Assertions.assertTrue(object.getAtt().containsKey(attribute));
		Assertions.assertEquals(finalValue, object.getAtt().get(attribute).getValue());

		action.setName(new Expression(attribute));
		action.setRegex(new RegularExpression(replacement));
		action.setReplacement(null);

		Assertions.assertTrue(object.getAtt().containsKey(attribute));
		Assertions.assertEquals(finalValue, object.getAtt().get(attribute).getValue());
		action.apply(ctx);
		Assertions.assertTrue(object.getAtt().containsKey(attribute));
		Assertions.assertEquals(finalValueRemoved, object.getAtt().get(attribute).getValue());
	}
}