package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.TransferEngineFactory;
import com.armedia.caliente.tools.CmfCrypt;

public abstract class ImportEngineFactory< //
	SESSION, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, CONTEXT_FACTORY>, //
	CONTEXT_FACTORY extends ImportContextFactory<SESSION, ?, VALUE, CONTEXT, ?, ?>, //
	DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, ?, VALUE, CONTEXT, ?>, //
	ENGINE extends ImportEngine<SESSION, ?, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ?> //
> extends
	TransferEngineFactory<ImportEngineListener, ImportResult, ImportException, SESSION, VALUE, CONTEXT, CONTEXT_FACTORY, DELEGATE_FACTORY, ENGINE> {

	protected ImportEngineFactory(boolean supportsDuplicateFileNames, CmfCrypt crypto) {
		super(supportsDuplicateFileNames, crypto);
	}

	public static ImportEngineFactory<?, ?, ?, ?, ?, ?> getImportEngineFactory(String targetName) {
		return TransferEngineFactory.getEngineFactory(ImportEngineFactory.class, targetName);
	}
}