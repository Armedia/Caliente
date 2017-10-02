package com.armedia.caliente.engine.transform.xml.actions;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestObjectFacade;
import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Expression;

public class SetSubtypeTest {

	@Test
	public void test() throws TransformationException {
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
	}

}