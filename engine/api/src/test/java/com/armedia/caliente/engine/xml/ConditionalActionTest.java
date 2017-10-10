package com.armedia.caliente.engine.xml;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.armedia.caliente.engine.transform.ActionException;
import com.armedia.caliente.engine.transform.ObjectContext;
import com.armedia.caliente.engine.transform.TestObjectContext;

public class ConditionalActionTest {

	@Test
	public void testConditionalAction() throws ActionException {
		final AtomicReference<Boolean> executed = new AtomicReference<>(null);
		final ObjectContext ctx = new TestObjectContext();

		ConditionalAction action = new ConditionalAction() {
			@Override
			protected void applyTransformation(ObjectContext ctx) throws ActionException {
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
		} catch (ActionException e) {
			// All is well
		}
	}
}