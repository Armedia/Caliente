package com.armedia.cmf.engine.alfresco.bulk.importer.model;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AlfrescoSchemaTest {

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
	public void testAlfrescoSchema() throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL[] urls = {
			cl.getResource("systemModel.xml"), cl.getResource("contentModel.xml"), cl.getResource("cmisModel.xml"),
			cl.getResource("jsap-contentModel.xml"),
		};
		List<URI> urlList = new ArrayList<URI>();
		for (URL u : urls) {
			urlList.add(u.toURI());
		}
		new AlfrescoSchema(urlList);
	}

	@Test
	public void testGetType() {
	}
}