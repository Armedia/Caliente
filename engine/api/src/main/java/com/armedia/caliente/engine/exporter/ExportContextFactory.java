package com.armedia.caliente.engine.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferContextFactory;
import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportContextFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	EXPORT_CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	EXPORT_ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, ?, ?> //
> extends TransferContextFactory<SESSION, VALUE, EXPORT_CONTEXT, EXPORT_ENGINE> {

	protected ExportContextFactory(EXPORT_ENGINE engine, CfgTools settings, SESSION session,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output, WarningTracker tracker)
		throws Exception {
		super(engine, settings, session, objectStore, contentStore, null, output, tracker);
	}

	@Override
	protected String getContextLabel() {
		return "export";
	}
}