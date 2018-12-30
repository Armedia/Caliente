package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineFactory;

public abstract class ImportEngineFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ImportContextFactory<SESSION, ?, VALUE, CONTEXT, ?, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	WORKER extends ImportEngine<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY> //
> extends
	TransferEngineFactory<ImportEngineListener, ImportResult, ImportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, WORKER> {
}