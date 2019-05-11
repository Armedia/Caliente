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