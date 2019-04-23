package com.armedia.caliente.engine.dynamic.xml;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.TestObjectContext;
import com.armedia.caliente.engine.dynamic.xml.ConditionalAction;

public class ConditionalActionTest {

	@Test
	public void testConditionalAction() throws ActionException {
		final AtomicReference<Boolean> executed = new AtomicReference<>(null);
		final DynamicElementContext ctx = new TestObjectContext();

		ConditionalAction action = new ConditionalAction() {
			@Override
			protected void executeAction(DynamicElementContext ctx) throws ActionException {
				executed.set(true);
			}
		};

		action.setCondition(null);
		action.apply(ctx);
		Assertions.assertNotNull(executed.get());
		Assertions.assertTrue(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_TRUE);
		action.apply(ctx);
		Assertions.assertNotNull(executed.get());
		Assertions.assertTrue(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_FALSE);
		action.apply(ctx);
		Assertions.assertNull(executed.get());

		executed.set(null);
		action.setCondition(ConditionTools.COND_FAIL);
		try {
			action.apply(ctx);
			Assertions.fail("Did not fail with an exploding condition");
		} catch (ActionException e) {
			// All is well
		}
	}
}