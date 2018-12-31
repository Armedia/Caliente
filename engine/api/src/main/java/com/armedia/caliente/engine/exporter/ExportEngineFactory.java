package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class ExportEngineFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ExportContextFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends ExportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	ENGINE extends ExportEngine<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> extends
	TransferEngineFactory<ExportEngineListener, ExportResult, ExportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ENGINE> {

	protected ExportEngineFactory(boolean supportsDuplicateFileNames, CmfCrypt crypto) {
		super(supportsDuplicateFileNames, crypto);
	}

	public static ExportEngineFactory<?, ?, ?, ?, ?, ?> getExportEngineFactory(String targetName) {
		return TransferEngineFactory.getEngineFactory(ExportEngineFactory.class, targetName);
	}
}