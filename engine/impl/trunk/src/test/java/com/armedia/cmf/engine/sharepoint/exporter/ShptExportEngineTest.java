package com.armedia.cmf.engine.sharepoint.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.cmf.engine.sharepoint.Setting;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.Stores;

public class ShptExportEngineTest {

	protected final ObjectStore<?, ?> objectStore = Stores.getObjectStore("shptTest");
	protected final ContentStore contentStore = Stores.getContentStore("shptTest");
	protected final Logger output = LoggerFactory.getLogger("console");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void exportTest() throws Exception {
		ExportEngine<?, ?, ?, ?, ?> exporter = ExportEngine.getExportEngine("shpt");
		this.objectStore.clearAllObjects();
		this.objectStore.clearAttributeMappings();
		this.contentStore.clearAllStreams();

		Map<String, String> settings = new HashMap<String, String>();
		settings.put(Setting.BASE_URL.getLabel(), "http://daltew8aapp03/sites/cmf");
		settings.put(Setting.USER.getLabel(), "drivera");
		settings.put(Setting.PASSWORD.getLabel(), "N3v3rm0r3!2");
		settings.put(Setting.DOMAIN.getLabel(), "ARMEDIA");
		settings.put(Setting.PATH.getLabel(), "/sites/cmf");
		exporter.runExport(this.output, this.objectStore, this.contentStore, settings);
	}

	@Test
	public void importTest() throws Exception {
		ImportEngine<?, ?, ?, ?, ?> importer = ImportEngine.getImportEngine("dctm");

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("docbase", "dctmvm01");
		settings.put("username", "dctmadmin");
		settings.put("password", "123");
		importer.runImport(this.output, this.objectStore, this.contentStore, settings);
	}

	@After
	public void tearDown() throws Exception {

	}
}
