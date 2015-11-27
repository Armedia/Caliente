package com.armedia.cmf.engine.documentum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfStores;

public class DctmEngineTest {

	protected final CmfObjectStore<?, ?> cmfObjectStore = CmfStores.getObjectStore("dctmTest");
	protected final CmfContentStore<?, ?, ?> streamStore = CmfStores.getContentStore("dctmTest");
	protected final Logger output = LoggerFactory.getLogger("console");

}