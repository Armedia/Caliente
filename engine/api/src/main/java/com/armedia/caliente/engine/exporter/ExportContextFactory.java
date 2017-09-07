package com.armedia.caliente.engine.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportContextFactory<S, W extends SessionWrapper<S>, V, C extends ExportContext<S, V, ?>, E extends ExportEngine<S, W, V, C, ?, ?>>
	extends TransferContextFactory<S, V, C, E> {

	protected ExportContextFactory(E engine, CfgTools settings, S session, CmfObjectStore<?, ?> objectStore,
		CmfContentStore<?, ?, ?> contentStore, Logger output, WarningTracker tracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, null, output, tracker);
	}

	@Override
	protected String getContextLabel() {
		return "export";
	}
}