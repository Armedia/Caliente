/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.transformer.TestObjectContext;
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