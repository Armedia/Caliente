package com.armedia.caliente.engine.documentum.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.armedia.caliente.engine.documentum.DctmEngineTest;
import com.armedia.caliente.engine.documentum.DctmSetting;
import com.armedia.caliente.engine.documentum.common.Setting;
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

		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DctmSetting.DOCBASE.getLabel(), "documentum");
		settings.put(DctmSetting.USERNAME.getLabel(), "dmadmin2");
		settings.put(DctmSetting.PASSWORD.getLabel(), "XZ6ZkrcrHEg=");
		settings.put(Setting.DQL.getLabel(),
			"select r_object_id from dm_sysobject where folder('/CMSMFTests', DESCEND)");

		exporter.runExport(this.output, this.cmfObjectStore, this.streamStore, settings);
	}

}