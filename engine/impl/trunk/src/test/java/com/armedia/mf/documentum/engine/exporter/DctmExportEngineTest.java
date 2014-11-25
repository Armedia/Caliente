package com.armedia.mf.documentum.engine.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.armedia.cmf.documentum.engine.DctmSessionFactory;
import com.armedia.cmf.documentum.engine.common.Setting;
import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.mf.documentum.engine.DctmEngineTest;

public class DctmExportEngineTest extends DctmEngineTest {

	@Before
	public void setUp() throws Exception {
		this.objectStore.clearAllObjects();
		this.streamStore.clearAllStreams();
	}

	@Test
	public void test() throws Exception {
		ExportEngine<?, ?, ?, ?, ?> exporter = ExportEngine.getExportEngine("dctm");

		Map<String, String> settings = new HashMap<String, String>();
		settings.put(DctmSessionFactory.DOCBASE, "documentum");
		settings.put(DctmSessionFactory.USERNAME, "dmadmin2");
		settings.put(DctmSessionFactory.PASSWORD, "XZ6ZkrcrHEg=");
		settings.put(Setting.DQL.getLabel(),
			"select r_object_id from dm_sysobject where folder('/CMSMFTests', DESCEND)");

		exporter.runExport(this.output, this.objectStore, this.streamStore, settings);
	}

}