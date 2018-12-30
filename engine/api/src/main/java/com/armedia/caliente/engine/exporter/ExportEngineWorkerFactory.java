package com.armedia.caliente.engine.exporter;

import com.armedia.caliente.engine.TransferEngineWorkerFactory;

public abstract class ExportEngineWorkerFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ExportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ExportContextFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	DELEGATE_FACTORY extends ExportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	WORKER extends ExportEngineWorker<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY> //
> extends
	TransferEngineWorkerFactory<ExportEngineListener, ExportResult, ExportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, WORKER> {

}