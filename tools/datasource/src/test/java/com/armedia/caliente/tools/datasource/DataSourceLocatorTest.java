package com.armedia.caliente.tools.datasource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.datasource.DataSourceLocator;

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
