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
package com.armedia.caliente.engine.local.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalQueryPostProcessorTest {

	@Test
	public void testValue() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		for (int i = 0; i < 100; i++) {
			String v = String.format("value-#%02d", i);
			lqpp.setValue(v);
			Assertions.assertEquals(v, lqpp.getValue());
		}

		lqpp.setValue(null);
		Assertions.assertNull(lqpp.getValue());
	}

	@Test
	public void testType() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		for (int i = 0; i < 100; i++) {
			String v = String.format("type-#%02d", i);
			lqpp.setType(v);
			Assertions.assertEquals(v, lqpp.getType());
		}

		lqpp.setType(null);
		Assertions.assertNull(lqpp.getType());
	}

	@Test
	public void testToString() {
		final LocalQueryPostProcessor lqpp = new LocalQueryPostProcessor();

		lqpp.setType("class");
		lqpp.setValue("some-value");
		Assertions.assertNotNull(lqpp.toString());
		Assertions.assertNotEquals(-1, lqpp.toString().indexOf("some-value"));

		lqpp.setType("jexl3");
		lqpp.setValue("another-value");
		Assertions.assertNotNull(lqpp.toString());
		Assertions.assertEquals(-1, lqpp.toString().indexOf("another-value"));
	}

}