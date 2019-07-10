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
package com.armedia.caliente.tools.datasource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataSourceLocatorTest {

	@Test
	void testClearCaches() {
		TestDSLCommon.TYPE_MAP.clear();
		DataSourceLocator.clearCaches();
		Assertions.assertTrue(TestDSLCommon.TYPE_MAP.isEmpty());
		testGetAllLocatorsFor();
		Assertions.assertFalse(TestDSLCommon.TYPE_MAP.isEmpty());
	}

	@Test
	void testGetFirstLocatorFor() {
		DataSourceLocator dsl = null;

		dsl = DataSourceLocator.getFirstLocatorFor(TestDSL_1.TYPE);
		Assertions.assertNotNull(dsl);

		dsl = DataSourceLocator.getFirstLocatorFor(TestDSL_2.TYPE);
		Assertions.assertNotNull(dsl);

		dsl = DataSourceLocator.getFirstLocatorFor(TestDSL_3.TYPE);
		Assertions.assertNotNull(dsl);

		dsl = DataSourceLocator.getFirstLocatorFor(UUID.randomUUID().toString());
		Assertions.assertNull(dsl);
	}

	@Test
	void testGetAllLocatorsFor() {
		List<DataSourceLocator> list = null;
		Set<String> expected = new HashSet<>();

		expected.clear();
		expected.add(TestDSL_1.class.getCanonicalName());
		expected.add(TestDSL_1_Dupe.class.getCanonicalName());
		list = DataSourceLocator.getAllLocatorsFor(TestDSL_1.TYPE);
		Assertions.assertNotNull(list);
		Assertions.assertFalse(list.isEmpty());
		Assertions.assertEquals(2, list.size());
		// Make sure exactly the two expected classes are there
		list.forEach((dsl) -> {
			Assertions.assertTrue(expected.remove(dsl.getClass().getCanonicalName()));
		});

		expected.clear();
		expected.add(TestDSL_2.class.getCanonicalName());
		expected.add(TestDSL_2_Dupe.class.getCanonicalName());
		list = DataSourceLocator.getAllLocatorsFor(TestDSL_2.TYPE);
		Assertions.assertNotNull(list);
		Assertions.assertFalse(list.isEmpty());
		Assertions.assertEquals(2, list.size());
		// Make sure exactly the two expected classes are there
		list.forEach((dsl) -> {
			Assertions.assertTrue(expected.remove(dsl.getClass().getCanonicalName()));
		});

		expected.clear();
		expected.add(TestDSL_3.class.getCanonicalName());
		expected.add(TestDSL_3_Dupe.class.getCanonicalName());
		list = DataSourceLocator.getAllLocatorsFor(TestDSL_3.TYPE);
		Assertions.assertNotNull(list);
		Assertions.assertFalse(list.isEmpty());
		Assertions.assertEquals(2, list.size());
		// Make sure exactly the two expected classes are there
		list.forEach((dsl) -> {
			Assertions.assertTrue(expected.remove(dsl.getClass().getCanonicalName()));
		});

		list = DataSourceLocator.getAllLocatorsFor(UUID.randomUUID().toString());
		Assertions.assertNotNull(list);
		Assertions.assertTrue(list.isEmpty());
	}

}
