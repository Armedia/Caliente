package com.armedia.caliente.engine.dfc.importer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.armedia.caliente.engine.dfc.DctmEngineTest;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.importer.ImportEngine;

public class DctmImportEngineTest extends DctmEngineTest {

	@Before
	public void setUp() throws Exception {
		this.cmfObjectStore.clearAttributeMappings();
	}

	@Test
	public void test() throws Exception {
		ImportEngine<?, ?, ?, ?, ?, ?> importer = ImportEngine.getImportEngine("dctm");

		Map<String, String> settings = new HashMap<>();
		settings.put(DctmSetting.DOCBASE.getLabel(), "dctmvm01");
		settings.put(DctmSetting.USERNAME.getLabel(), "dctmadmin");
		settings.put(DctmSetting.PASSWORD.getLabel(), "123");

		importer.runImport(this.output, null, this.baseData, this.cmfObjectStore, this.streamStore, settings);
	}

}