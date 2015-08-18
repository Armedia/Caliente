package com.armedia.cmf.engine.documentum.importer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.armedia.cmf.engine.documentum.DctmEngineTest;
import com.armedia.cmf.engine.documentum.DctmSessionFactory;
import com.armedia.cmf.engine.importer.ImportEngine;

public class DctmImportEngineTest extends DctmEngineTest {

	@Before
	public void setUp() throws Exception {
		this.objectStore.clearAttributeMappings();
	}

	@Test
	public void test() throws Exception {
		ImportEngine<?, ?, ?, ?, ?> importer = ImportEngine.getImportEngine("dctm");

		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DctmSessionFactory.DOCBASE, "dctmvm01");
		settings.put(DctmSessionFactory.USERNAME, "dctmadmin");
		settings.put(DctmSessionFactory.PASSWORD, "123");

		importer.runImport(this.output, this.objectStore, this.streamStore, settings);
	}

}