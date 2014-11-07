package com.armedia.mf.documentum.engine.exporter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.ObjectStoreFactory;

public class DctmExportEngineTest {

	@Test
	public void test() throws Exception {
		File currentDir = new File(System.getProperty("user.dir")).getCanonicalFile();
		ExportEngine<?, ?, ?, ?, ?> engine = ExportEngine.getExportEngine("dctm");
		ObjectStore<?, ?> objectStore = ObjectStoreFactory.getInstance("dctmTest");
		ContentStreamStore streamStore = new ContentStreamStore(new File(currentDir, "tmp"));
		Logger output = LoggerFactory.getLogger("output");
		Map<String, String> settings = new HashMap<String, String>();
		// settings.put("dql",
		// "select r_object_id, r_object_type from dm_sysobject where folder('/CMSMFTests')");
		settings.put("dql", "select r_object_id, 'dm_acl' as r_object_type from dm_acl");
		settings.put("docbase", "documentum");
		settings.put("username", "dmadmin2");
		settings.put("password", "XZ6ZkrcrHEg=");
		engine.runExport(output, objectStore, streamStore, settings);
		settings.put("dql", "select r_object_id, 'dm_type' as r_object_type from dm_type");
		engine.runExport(output, objectStore, streamStore, settings);
		settings.put("dql", "select r_object_id, 'dm_format' as r_object_type from dm_format");
		engine.runExport(output, objectStore, streamStore, settings);
	}
}