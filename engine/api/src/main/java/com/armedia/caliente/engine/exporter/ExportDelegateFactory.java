package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.store.CmfType;
import com.armedia.commons.utilities.CfgTools;

public abstract class ExportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, ?>, //
	ENGINE extends ExportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?> //
> extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	protected ExportDelegateFactory(ENGINE engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ExportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newExportDelegate(
		SESSION session, CmfType type, String searchKey) throws Exception;
}