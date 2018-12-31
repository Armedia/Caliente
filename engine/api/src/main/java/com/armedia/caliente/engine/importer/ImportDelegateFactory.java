package com.armedia.caliente.engine.importer;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegateFactory;
import com.armedia.caliente.engine.dynamic.mapper.schema.SchemaService;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.CfgTools;

public abstract class ImportDelegateFactory< //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ?, ?>//
> extends TransferDelegateFactory<SESSION, VALUE, CONTEXT, ENGINE> {

	protected ImportDelegateFactory(ENGINE engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected abstract ImportDelegate<?, SESSION, SESSION_WRAPPER, VALUE, CONTEXT, ?, ENGINE> newImportDelegate(
		CmfObject<VALUE> storedObject) throws Exception;

	protected abstract SchemaService newSchemaService(SESSION session) throws Exception;
}