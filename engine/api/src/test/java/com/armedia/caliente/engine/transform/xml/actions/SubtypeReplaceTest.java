package com.armedia.caliente.engine.transform.xml.actions;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestObjectFacade;
import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;

public class SubtypeReplaceTest {

	@Test
	public void test() throws TransformationException {
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

}