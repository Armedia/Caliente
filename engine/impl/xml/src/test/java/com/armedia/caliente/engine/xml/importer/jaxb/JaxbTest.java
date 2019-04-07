package com.armedia.caliente.engine.xml.importer.jaxb;

import java.io.InputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
