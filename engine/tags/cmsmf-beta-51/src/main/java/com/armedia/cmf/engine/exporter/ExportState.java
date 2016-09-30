package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferState;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public final class ExportState extends TransferState {

	ExportState(Logger output, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore,
		CfgTools settings) {
		super(output, objectStore, streamStore, settings);
	}
}