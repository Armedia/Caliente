package com.armedia.caliente.engine.transform.xml;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.TestTransformationContext;
import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.ConditionalAction;

public class ConditionalActionTest {

	@Test
	public void testConditionalAction() throws TransformationException {
		final AtomicReference<Boolean> executed = new AtomicReference<>(null);
		final TransformationContext ctx = new TestTransformationContext();

		ConditionalAction action = new ConditionalAction() {
			@Override
			protected void applyTransformation(TransformationContext ctx) throws TransformationException {
				executed.set(true);
			}
		};

		action.setCondition(null);
		action.apply(ctx);
		Assert.assertNotNull(executed.get());
		Assert.assertTrue(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_TRUE);
		action.apply(ctx);
		Assert.assertNotNull(executed.get());
		Assert.assertTrue(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_FALSE);
		action.apply(ctx);
		Assert.assertNull(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_FAIL);
		try {
			action.apply(ctx);
			Assert.fail("Did not fail with an exploding condition");
		} catch (TransformationException e) {
			// All is well
		}
	}
}