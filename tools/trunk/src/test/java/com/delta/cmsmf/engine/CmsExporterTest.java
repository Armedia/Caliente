package com.delta.cmsmf.engine;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.delta.cmsmf.cms.AbstractTest;
import com.delta.cmsmf.cms.storage.CmsObjectStore;

public class CmsExporterTest extends AbstractTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testDoExport() throws Throwable {
		CmsObjectStore store = new CmsObjectStore(getDataSource(), true);
		CmsExporter exporter = new CmsExporter(10);
		exporter.doExport(store, getSessionManager(), "from dm_user");
	}
}