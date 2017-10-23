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

import com.armedia.caliente.engine.alfresco.bi.importer.jaxb.model.Model;
import com.armedia.commons.utilities.XmlTools;

public class JaxbTest {

	protected static class XmlTest<T> {

		private final Class<T> c;
		private final URL resource;

		protected XmlTest(Class<T> c, String resource) {
			this.c = c;
			this.resource = Thread.currentThread().getContextClassLoader().getResource(resource);
		}

		protected void run() throws Exception {
			InputStream in = null;
			final String schema = "alfresco-model.xsd";

			T obj = null;
			try {
				in = this.resource.openStream();
				obj = XmlTools.unmarshal(this.c, schema, in);
				Assert.assertNotNull(obj);
			} finally {
				IOUtils.closeQuietly(in);
			}

			validate(obj);
			validate(obj, XmlTools.marshal(obj, schema, true));
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
	public void testAll() throws Exception {
		String[] s = {
			"contentModel.xml", "alfresco-model.xml", "cmisModel.xml"
		};

		for (String xml : s) {
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			XmlTest<?> xmlTest = new XmlTest(Model.class, xml);
			try {
				xmlTest.run();
			} catch (Throwable t) {
				throw new Exception(String.format("Failed while processing [%s]", xml), t);
			}
		}
	}
}