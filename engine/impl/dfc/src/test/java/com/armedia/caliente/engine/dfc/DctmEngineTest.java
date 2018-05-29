package com.armedia.caliente.engine.dfc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStores;

public class DctmEngineTest {

	protected final CmfObjectStore<?, ?> cmfObjectStore = CmfStores.getObjectStore("default");
	protected final CmfContentStore<?, ?, ?> streamStore = CmfStores.getContentStore("default");
	protected final Logger output = LoggerFactory.getLogger("console");

}