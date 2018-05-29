package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	IMPORT_CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	IMPORT_ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, IMPORT_CONTEXT, ?, ?>//
> extends TransferDelegateFactory<SESSION, VALUE, IMPORT_CONTEXT, IMPORT_ENGINE> {

	protected ImportDelegateFactory(IMPORT_ENGINE engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, IMPORT_CONTEXT, ?, IMPORT_ENGINE> newImportDelegate(CmfObject<VALUE> storedObject)
		throws Exception;
}