package com.armedia.mf.documentum.engine.exporter;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

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
		engine.runExport(null, objectStore, streamStore, null);
		Assert.fail("Not yet implemented");
	}
}