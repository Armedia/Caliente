package com.armedia.caliente.engine.xml.importer.jaxb;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.armedia.caliente.engine.xml.importer.jaxb.AclsT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentT;
import com.armedia.caliente.engine.xml.importer.jaxb.DocumentsT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderIndexT;
import com.armedia.caliente.engine.xml.importer.jaxb.FolderT;
import com.armedia.caliente.engine.xml.importer.jaxb.FoldersT;
import com.armedia.caliente.engine.xml.importer.jaxb.FormatsT;
import com.armedia.caliente.engine.xml.importer.jaxb.GroupsT;
import com.armedia.caliente.engine.xml.importer.jaxb.TypesT;
import com.armedia.caliente.engine.xml.importer.jaxb.UsersT;
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
			final String schema = "caliente-engine-xml.xsd";

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
