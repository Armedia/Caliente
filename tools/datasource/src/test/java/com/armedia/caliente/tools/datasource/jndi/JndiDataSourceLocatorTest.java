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
package com.armedia.caliente.tools.datasource.jndi;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.tools.datasource.DataSourceDescriptor;
import com.armedia.commons.utilities.CfgTools;

public class JndiDataSourceLocatorTest {

	private static final String NOT_A_DS_NAME = "NotAnInstanceOfDataSource";
	private static final String DSNAME_FMT = "java:/ds%02x";
	private static final Map<Integer, JdbcConnectionPool> DATA_SOURCES = new TreeMap<>();
	private static InitialContext CONTEXT = null;

	@BeforeAll
	public static void beforeAll() throws Exception {
		// Create initial context
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.naming.java.javaURLContextFactory");
		System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");
		InitialContext ic = new InitialContext();
		ic.createSubcontext("java:");
		ic.createSubcontext("java:/comp");
		ic.createSubcontext("java:/comp/env");
		ic.createSubcontext("java:/comp/env/jdbc");

		JndiDataSourceLocatorTest.DATA_SOURCES.clear();
		for (int i = 0; i < 10; i++) {
			if ((i % 2) != 0) {
				continue;
			}
			String dbname = String.format("jdbc:h2:mem:testdb_%02d", i);
			JdbcConnectionPool ds = JdbcConnectionPool.create(dbname, "sa", "sasasa");
			JndiDataSourceLocatorTest.DATA_SOURCES.put(i, ds);
			ic.bind(String.format(JndiDataSourceLocatorTest.DSNAME_FMT, i), ds);
		}
		ic.bind(JndiDataSourceLocatorTest.NOT_A_DS_NAME, UUID.randomUUID());
		JndiDataSourceLocatorTest.CONTEXT = ic;
	}

	@Test
	void testSupportsLocationType() {
		JndiDataSourceLocator loc = new JndiDataSourceLocator();
		Assertions.assertFalse(loc.supportsLocationType(null));
		Assertions.assertTrue(loc.supportsLocationType(JndiDataSourceLocator.JNDI));
		Assertions.assertTrue(loc.supportsLocationType(JndiDataSourceLocator.JNDI.toUpperCase()));
		Assertions.assertTrue(loc.supportsLocationType(JndiDataSourceLocator.JNDI.toLowerCase()));
	}

	@Test
	void testLocateDataSource() throws Exception {
		JndiDataSourceLocator loc = new JndiDataSourceLocator();
		Map<String, String> settings = new TreeMap<>();
		for (int i = 0; i < 10; i++) {
			// If it's even, then it should exist. If not, it shouldn't...
			settings.clear();
			String name = String.format(JndiDataSourceLocatorTest.DSNAME_FMT, i);
			settings.put(JndiSetting.DATASOURCE_NAME.getLabel(), name);

			if ((i % 3) == 0) {
				for (int j = 0; j < 10; j++) {
					settings.put(String.format("jndi.testSetting%02d", j), String.format("value-%02x", j));
				}
			}

			CfgTools cfg = new CfgTools(settings);

			if ((i % 2) == 0) {
				// Should exist
				DataSourceDescriptor<?> dsc = loc.locateDataSource(cfg);
				// Should exist
				Assertions.assertNotNull(dsc, name);
				Assertions.assertSame(JndiDataSourceLocatorTest.DATA_SOURCES.get(i), dsc.getDataSource(), name);
			} else {
				Assertions.assertThrows(NameNotFoundException.class, () -> loc.locateDataSource(cfg));
			}
		}

		settings.clear();
		Assertions.assertThrows(NullPointerException.class, () -> loc.locateDataSource(new CfgTools(settings)));

		settings.clear();
		settings.put(JndiSetting.DATASOURCE_NAME.getLabel(), JndiDataSourceLocatorTest.NOT_A_DS_NAME);

		Assertions.assertThrows(ClassCastException.class, () -> loc.locateDataSource(new CfgTools(settings)));
	}

	@AfterAll
	public static void afterAll() {
		JndiDataSourceLocatorTest.DATA_SOURCES.forEach((i, ds) -> {
			ds.dispose();
			try {
				JndiDataSourceLocatorTest.CONTEXT.unbind(String.format(JndiDataSourceLocatorTest.DSNAME_FMT, i));
			} catch (NamingException e) {
				// Ignore...
			}
		});
		JndiDataSourceLocatorTest.DATA_SOURCES.clear();
		JndiDataSourceLocatorTest.CONTEXT = null;
	}
}