package com.armedia.mf.documentum.engine.importer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.armedia.cmf.engine.importer.ImportEngine;
import com.armedia.mf.documentum.engine.DctmEngineTest;

public class DctmImportEngineTest extends DctmEngineTest {

	@Test
	public void test() throws Exception {
		ImportEngine<?, ?, ?, ?, ?> importer = ImportEngine.getImportEngine("dctm");

		Map<String, String> settings = new HashMap<String, String>();
		settings.put("docbase", "dctmvm01");
		settings.put("username", "dctmadmin");
		settings.put("password", "123");

		importer.runImport(this.output, this.objectStore, this.streamStore, settings);
	}

}