package com.armedia.caliente.engine.dfc.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.armedia.caliente.engine.dfc.DctmEngineTest;
import com.armedia.caliente.engine.dfc.DctmSetting;
import com.armedia.caliente.engine.dfc.common.Setting;
import com.armedia.caliente.engine.exporter.ExportEngine;

public class DctmExportEngineTest extends DctmEngineTest {

	@Before
	public void setUp() throws Exception {
		this.cmfObjectStore.clearAllObjects();
		this.streamStore.clearAllStreams();
	}

	@Test
	public void test() throws Exception {
		ExportEngine<?, ?, ?, ?, ?, ?> exporter = ExportEngine.getExportEngine("dctm");

		Map<String, String> settings = new HashMap<>();
		settings.put(DctmSetting.DOCBASE.getLabel(), "documentum");
		settings.put(DctmSetting.USERNAME.getLabel(), "dmadmin2");
		settings.put(DctmSetting.PASSWORD.getLabel(), "XZ6ZkrcrHEg=");
		settings.put(Setting.DQL.getLabel(),
			"select r_object_id from dm_sysobject where folder('/CMSMFTests', DESCEND)");

		exporter.runExport(this.output, null, this.cmfObjectStore, this.streamStore, settings);
	}

}