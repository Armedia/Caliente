package com.armedia.caliente.engine.dfc.importer;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.engine.dfc.DctmEngineTest;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.commons.utilities.CfgTools;

public class DctmImportEngineTest extends DctmEngineTest {

	@BeforeEach
	public void setUp() throws Exception {
		this.cmfObjectStore.clearAttributeMappings();
	}

	@Test
	public void test() throws Exception {
		DctmImportEngineFactory factory = new DctmImportEngineFactory();

		Map<String, String> settings = new HashMap<>();
		settings.put(DctmSetting.DOCBASE.getLabel(), "dctmvm01");
		settings.put(DctmSetting.USERNAME.getLabel(), "dctmadmin");
		settings.put(DctmSetting.PASSWORD.getLabel(), "123");

		factory.newInstance(this.output, null, this.baseData, this.cmfObjectStore, this.streamStore,
			new CfgTools(settings)).run(null);
	}

}