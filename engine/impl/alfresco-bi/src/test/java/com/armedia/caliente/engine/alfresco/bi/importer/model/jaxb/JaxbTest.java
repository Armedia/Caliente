package com.armedia.caliente.engine.alfresco.bi.importer.model.jaxb;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.mapper.AttributeMappings;
import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.Model;
import com.armedia.commons.utilities.XmlTools;

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
			InputStream in = null;

			T obj = null;
			try {
				in = this.resource.openStream();
				obj = XmlTools.unmarshal(this.c, this.schema, in);
				Assert.assertNotNull(obj);
			} finally {
				IOUtils.closeQuietly(in);
			}

			validate(obj);
			validate(obj, XmlTools.marshal(obj, this.schema, true));
		}

		protected void validate(T obj) throws Exception {
			Assert.assertNotNull(obj);
		}

		protected void validate(T obj, String str) throws Exception {
			Assert.assertNotNull(obj);
			Assert.assertNotNull(str);
		}
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
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

	@Test
	public void testAttributeMappings() throws Exception {
		final String schema = "alfresco-bi.xsd";
		String[] s = {
			"test-mappings.xml"
		};

		for (String xml : s) {
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			XmlTest<?> xmlTest = new XmlTest(AttributeMappings.class, xml, schema);
			try {
				xmlTest.run();
			} catch (Throwable t) {
				throw new Exception(String.format("Failed while processing [%s]", xml), t);
			}
		}
	}
}