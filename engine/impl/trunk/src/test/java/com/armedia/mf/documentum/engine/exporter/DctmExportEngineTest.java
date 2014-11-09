package com.armedia.mf.documentum.engine.exporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.engine.exporter.ExportEngine;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.ObjectStoreFactory;
import com.armedia.cmf.storage.StoredObjectType;

public class DctmExportEngineTest {

	@Test
	public void test() throws Exception {
		final File currentDir = new File(System.getProperty("user.dir")).getCanonicalFile();
		ExportEngine<?, ?, ?, ?, ?> engine = ExportEngine.getExportEngine("dctm");
		ObjectStore<?, ?> objectStore = ObjectStoreFactory.getInstance("dctmTest");
		ContentStreamStore streamStore = new ContentStreamStore() {

			private final File baseDir = new File(currentDir, "tmp");

			@Override
			protected URI doAllocateHandleId(StoredObjectType objectType, String objectId) {
				String path = String.format("%s/%s", objectType.name(), objectId);
				try {
					return new URI("test", "testStore", path, null, null);
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			protected File doGetFile(URI handleId) {
				return new File(this.baseDir, handleId.getPath());
			}

			@Override
			protected InputStream doOpenInput(URI handleId) throws IOException {
				return new FileInputStream(getFile(handleId));
			}

			@Override
			protected OutputStream doOpenOutput(URI handleId) throws IOException {
				return new FileOutputStream(getFile(handleId));
			}

			@Override
			protected boolean doIsExists(URI handleId) {
				return getFile(handleId).exists();
			}

			@Override
			protected long doGetStreamSize(URI handleId) {
				File f = getFile(handleId);
				return (f.exists() ? f.length() : -1);
			}
		};

		Logger output = LoggerFactory.getLogger("console");
		output.info("Console initialized");
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