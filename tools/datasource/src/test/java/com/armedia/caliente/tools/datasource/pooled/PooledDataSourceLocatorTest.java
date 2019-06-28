/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.tools.datasource.pooled;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.commons.utilities.CfgTools;

class PooledDataSourceLocatorTest {

	private static final String DRIVER_CLASS_NAME = "org.h2.Driver";

	@Test
	void testSupportsLocationType() {
		PooledDataSourceLocator loc = new PooledDataSourceLocator();
		Assertions.assertFalse(loc.supportsLocationType(null));
		Assertions.assertTrue(loc.supportsLocationType(PooledDataSourceLocator.POOLED));
		Assertions.assertTrue(loc.supportsLocationType(PooledDataSourceLocator.POOLED.toUpperCase()));
		Assertions.assertTrue(loc.supportsLocationType(PooledDataSourceLocator.POOLED.toLowerCase()));
	}

	@Test
	void testLocateDataSourceCfgTools() throws Exception {
		PooledDataSourceLocator loc = new PooledDataSourceLocator();

		Map<String, String> settings = new TreeMap<>();

		// First, happy path
		settings.clear();
		settings.put("jdbc.driverClassName", PooledDataSourceLocatorTest.DRIVER_CLASS_NAME);
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		Assertions.assertNotNull(loc.locateDataSource(new CfgTools(settings)));

		settings.put("this.will.be.ignored", UUID.randomUUID().toString());
		settings.put("jdbc.driver", PooledDataSourceLocatorTest.DRIVER_CLASS_NAME);
		Assertions.assertNotNull(loc.locateDataSource(new CfgTools(settings)));
		settings.clear();
		settings.put("jdbc.driverClassName", PooledDataSourceLocatorTest.DRIVER_CLASS_NAME);
		Assertions.assertThrows(IllegalArgumentException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		Assertions.assertThrows(IllegalArgumentException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.driverClassName", "");
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		Assertions.assertThrows(IllegalArgumentException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.driverClassName", "                 ");
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		Assertions.assertThrows(IllegalArgumentException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.driverClassName", "some.weird.class.we.know.doesn't.exist");
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		Assertions.assertThrows(ClassNotFoundException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.driverClassName", PooledDataSourceLocatorTest.DRIVER_CLASS_NAME);
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		settings.put("jdbc.username", "testuser");
		Assertions.assertNotNull(loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put("jdbc.driverClassName", PooledDataSourceLocatorTest.DRIVER_CLASS_NAME);
		settings.put("jdbc.url", "jdbc:h2:mem:testdb");
		settings.put("jdbc.user", "testuser");
		DataSourceDescriptor<?> desc = loc.locateDataSource(new CfgTools(settings));
		Assertions.assertNotNull(desc);
		desc.close();
		desc.close();
	}

}