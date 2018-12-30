package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineFactory;

public abstract class ExportEngineFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ExportContextFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends ExportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	WORKER extends ExportEngine<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY> //
> extends
	TransferEngineFactory<ExportEngineListener, ExportResult, ExportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, WORKER> {

}