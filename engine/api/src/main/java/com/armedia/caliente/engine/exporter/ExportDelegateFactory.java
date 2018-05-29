package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	EXPORT_CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	EXPORT_ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, ?, ?> //
> extends TransferDelegateFactory<SESSION, VALUE, EXPORT_CONTEXT, EXPORT_ENGINE> {

	protected ExportDelegateFactory(EXPORT_ENGINE engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, EXPORT_CONTEXT, ?, EXPORT_ENGINE> newExportDelegate(SESSION session, CmfType type, String searchKey)
		throws Exception;
}