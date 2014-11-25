package com.armedia.cmf.documentum.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.Stores;

public class DctmEngineTest {

	protected final ObjectStore<?, ?> objectStore = Stores.getObjectStore("dctmTest");
	protected final ContentStore streamStore = Stores.getContentStore("dctmTest");
	protected final Logger output = LoggerFactory.getLogger("console");

}