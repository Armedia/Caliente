package com.armedia.caliente.engine.dfc;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfStores;
import com.armedia.commons.utilities.Tools;

public class DctmEngineTest {

	protected final CmfObjectStore<?> cmfObjectStore = CmfStores.getObjectStore("default");
	protected final CmfContentStore<?, ?> streamStore = CmfStores.getContentStore("default");
	protected final File baseData = Tools.coalesce(this.cmfObjectStore.getStoreLocation(),
		this.streamStore.getStoreLocation());
	protected final Logger output = LoggerFactory.getLogger("console");

}