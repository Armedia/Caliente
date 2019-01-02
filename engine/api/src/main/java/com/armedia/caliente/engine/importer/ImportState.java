package com.armedia.caliente.engine.importer;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.armedia.caliente.engine.TransferState;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public final class ImportState extends TransferState {

	ImportState(Logger output, Path baseData, CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore,
		CfgTools settings) {
		super(output, baseData, objectStore, streamStore, settings);
	}
}