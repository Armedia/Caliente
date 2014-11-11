package com.armedia.mf.documentum.engine.exporter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.storage.CmfStores;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;

public class DctmExportEngineTest {

	@Test
	public void test() throws Exception {
		ExportEngine<?, ?, ?, ?, ?> engine = ExportEngine.getExportEngine("dctm");
		ObjectStore<?, ?> objectStore = CmfStores.getObjectStore("dctmTest");
		ContentStore streamStore = CmfStores.getContentStore("dctmTest");

		Logger output = LoggerFactory.getLogger("console");
		output.info("Console initialized");
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("docbase", "documentum");
		settings.put("username", "dmadmin2");
		settings.put("password", "XZ6ZkrcrHEg=");

		settings.put("dql", "select r_object_id, 'dm_type' as r_object_type from dm_type");
		engine.runExport(output, objectStore, streamStore, settings);

		settings.put("dql", "select r_object_id, 'dm_format' as r_object_type from dm_format");
		engine.runExport(output, objectStore, streamStore, settings);

		// settings.put("dql", "select r_object_id, 'dm_acl' as r_object_type from dm_acl");
		settings.put("dql", "select r_object_id, r_object_type from dm_sysobject where folder('/CMSMFTests', DESCEND)");
		engine.runExport(output, objectStore, streamStore, settings);
	}
}