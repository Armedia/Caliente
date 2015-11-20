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
	public void testAll() throws Exception {
		Class<?>[] k = {
			AclsT.class, DocumentT.class, DocumentIndexT.class, FoldersT.class, GroupsT.class, TypesT.class,
			UsersT.class
		};
		String[] s = {
			"acls", "document", "documentIndex", "folders", "groups", "types", "users"
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