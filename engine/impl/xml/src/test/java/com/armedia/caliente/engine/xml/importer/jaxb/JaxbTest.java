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
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.io.InputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.commons.utilities.xml.XmlTools;

public class JaxbTest {

	protected static class XmlTest<T> {

		private final Class<T> c;
		private final String suffix;

		protected XmlTest(Class<T> c, String suffix) {
			this.c = c;
			this.suffix = suffix;
		}

		protected void run() throws Exception {
			final String schema = "caliente-engine-xml.xsd";

			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			T obj = null;
			try (InputStream in = cl.getResourceAsStream(String.format("sample-%s.xml", this.suffix))) {
				obj = XmlTools.unmarshal(this.c, schema, in);
				Assertions.assertNotNull(obj);
			}

			validate(obj);
			validate(obj, XmlTools.marshal(obj, schema, true));
		}

		protected void validate(T obj) throws Exception {
			Assertions.assertNotNull(obj);
		}

		protected void validate(T obj, String str) throws Exception {
			Assertions.assertNotNull(obj);
			Assertions.assertNotNull(str);
		}
	}

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public void testAll() throws Exception {
		Class<?>[] k = {
			AclsT.class, DocumentT.class, DocumentsT.class, FolderT.class, FoldersT.class, GroupsT.class, TypesT.class,
			UsersT.class, DocumentIndexT.class, FolderIndexT.class, FormatsT.class
		};
		String[] s = {
			"acls", "document", "documents", "folder", "folders", "groups", "types", "users", "documentIndex",
			"folderIndex", "formats"
		};

		for (int i = 0; i < k.length; i++) {
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			XmlTest<?> xmlTest = new XmlTest(k[i], s[i]);
			try {
				xmlTest.run();
			} catch (Throwable t) {
				throw new Exception(String.format("Failed while processing [%s, %s]", k[i].getSimpleName(), s[i]), t);
			}
		}
	}
}
