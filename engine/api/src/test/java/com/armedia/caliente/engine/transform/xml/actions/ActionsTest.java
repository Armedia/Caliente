package com.armedia.caliente.engine.transform.xml.actions;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestObjectFacade;
import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationCompletedException;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;

public class ActionsTest {

	@Test
	public void testAbortTransformation() {
		try {
			new AbortTransformation().apply(new TestTransformationContext());
			Assert.fail("Failed to abort the transformation");
		} catch (TransformationException e) {
			// All is well
		}
	}

	@Test
	public void testEndTransformation() throws TransformationException {
		try {
			new EndTransformation().apply(new TestTransformationContext());
			Assert.fail("Failed to end the transformation");
		} catch (TransformationCompletedException e) {
			// All is well
		}
	}

	@Test
	public void testSubtypeSet() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();
		Assert.assertNull(object.getSubtype());

		SubtypeSet action = new SubtypeSet();
		Expression e = new Expression();
		e.setLang(null);
		String value = UUID.randomUUID().toString();
		e.setScript(value);
		action.setSubtype(e);

		action.apply(ctx);

		Assert.assertEquals(value, object.getSubtype());

		e.setScript(
			"                                                       \n   \t   \n                       \t \t   \n                               ");
		action.apply(ctx);
		// Value is an empty string, so no change...
		Assert.assertEquals(value, object.getSubtype());

		e.setScript(null);
		action.apply(ctx);
		// Value is a null string, so no change...
		Assert.assertEquals(value, object.getSubtype());
	}

	@Test
	public void testSubtypeReplace() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();
		object.setSubtype("dctm:test_subtype_value");

		SubtypeReplace action = new SubtypeReplace();

		try {
			action.apply(ctx);
			Assert.fail("Did not fail with neither a regex or a replacement");
		} catch (TransformationException e) {
			// All is well
		}

		Expression regex = new Expression();
		action.setRegex(regex);
		regex.setLang(null);
		regex.setScript("^dctm:");

		action.apply(ctx);
		Assert.assertEquals("test_subtype_value", object.getSubtype());

		// Reset the value...
		object.setSubtype("dctm:test_subtype_value");

		Expression replacement = new Expression();
		action.setReplacement(replacement);
		replacement.setLang(null);
		replacement.setScript("alfresco||");

		action.apply(ctx);
		Assert.assertEquals("alfresco||test_subtype_value", object.getSubtype());

		regex.setScript("(test_)(subtype)(_value)");
		replacement.setScript("$3$1$2");
		action.apply(ctx);
		Assert.assertEquals("alfresco||_valuetest_subtype", object.getSubtype());
	}

	@Test
	public void testDecoratorAdd() throws TransformationException {
		TestTransformationContext ctx = new TestTransformationContext();
		TestObjectFacade object = ctx.getObject();

		DecoratorAdd action = new DecoratorAdd();
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		Expression e = new Expression();
		action.setDecorator(e);
		e.setLang(null);
		e.setScript(null);
		action.apply(ctx);
		Assert.assertTrue(object.getDecorators().isEmpty());

		String testValue = UUID.randomUUID().toString();
		e.setScript(testValue);
		action.apply(ctx);
		Assert.assertFalse(object.getDecorators().isEmpty());
		Assert.assertTrue(object.getDecorators().contains(testValue));
	}

}