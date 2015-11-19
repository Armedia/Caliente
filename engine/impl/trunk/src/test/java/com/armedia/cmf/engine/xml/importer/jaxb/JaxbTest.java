package com.armedia.cmf.engine.xml.importer.jaxb;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.commons.utilities.XmlTools;

public class JaxbTest {

	protected static class XmlTest<T> {

		private final Class<T> c;
		private final String suffix;

		protected XmlTest(Class<T> c, String suffix) {
			this.c = c;
			this.suffix = suffix;
		}

		protected void run() throws Exception {
			InputStream in = null;
			final String schema = "import.xsd";

			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			T obj = null;
			try {
				in = cl.getResourceAsStream(String.format("sample-%s.xml", this.suffix));
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
	public void testAcl() throws Exception {
		new XmlTest<AclsT>(AclsT.class, "acls") {
			@Override
			protected void validate(AclsT obj) throws Exception {
				// TODO: validate
				super.validate(obj);
			}

			@Override
			protected void validate(AclsT obj, String str) throws Exception {
				// TODO: validate
				super.validate(obj, str);
			}
		}.run();
	}

	@Test
	public void testDocument() throws Exception {
		new XmlTest<DocumentT>(DocumentT.class, "document") {
			@Override
			protected void validate(DocumentT obj) throws Exception {
				// TODO: validate
				super.validate(obj);
			}

			@Override
			protected void validate(DocumentT obj, String str) throws Exception {
				// TODO: validate
				super.validate(obj, str);
			}
		}.run();
	}
}