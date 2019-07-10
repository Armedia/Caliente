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
package com.armedia.caliente.engine.alfresco.bi.importer.model.jaxb;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.Model;
import com.armedia.commons.utilities.xml.XmlTools;

public class JaxbTest {

	protected static class XmlTest<T> {

		private final Class<T> c;
		private final URL resource;
		private final String schema;

		protected XmlTest(Class<T> c, String resource, String schema) {
			this.c = c;
			this.resource = Thread.currentThread().getContextClassLoader().getResource(resource);
			this.schema = schema;
		}

		protected final void run() throws Exception {
			T obj = null;
			try (InputStream in = this.resource.openStream()) {
				obj = XmlTools.unmarshal(this.c, this.schema, in);
				Assertions.assertNotNull(obj);
			}

			validate(obj);
			validate(obj, XmlTools.marshal(obj, this.schema, true));
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
	public void testModels() throws Exception {
		final String schema = "alfresco-model.xsd";
		String[] s = {
			"contentModel.xml", "alfresco-model.xml", "cmisModel.xml"
		};

		for (String xml : s) {
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			XmlTest<?> xmlTest = new XmlTest(Model.class, xml, schema);
			try {
				xmlTest.run();
			} catch (Throwable t) {
				throw new Exception(String.format("Failed while processing [%s]", xml), t);
			}
		}
	}
}