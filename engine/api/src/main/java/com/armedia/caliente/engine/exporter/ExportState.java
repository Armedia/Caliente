package com.armedia.caliente.engine.exporter;

import java.io.File;

import org.slf4j.Logger;

import com.armedia.caliente.engine.TransferState;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public final class ExportState extends TransferState {

	ExportState(Logger output, File baseData, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore,
		CfgTools settings) {
		super(output, baseData, objectStore, streamStore, settings);
	}
}