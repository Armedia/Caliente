package com.armedia.caliente.engine.importer;

import java.util.Collection;

import com.armedia.caliente.engine.SessionWrapper;
import com.armedia.caliente.engine.TransferDelegate;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;

public abstract class ImportDelegate< //
	ECM_OBJECT, //
	SESSION, //
	SESSION_WRAPPER extends SessionWrapper<SESSION>, //
	VALUE, //
	IMPORT_CONTEXT extends ImportContext<SESSION, VALUE, ?>, //
	IMPORT_DELEGATE_FACTORY extends ImportDelegateFactory<SESSION, SESSION_WRAPPER, VALUE, IMPORT_CONTEXT, IMPORT_ENGINE>, //
	IMPORT_ENGINE extends ImportEngine<SESSION, SESSION_WRAPPER, VALUE, IMPORT_CONTEXT, ?, IMPORT_DELEGATE_FACTORY> //
> extends TransferDelegate<ECM_OBJECT, SESSION, VALUE, IMPORT_CONTEXT, IMPORT_DELEGATE_FACTORY, IMPORT_ENGINE> {

	protected final CmfObject<VALUE> cmfObject;
	protected final ImportStrategy strategy;

	protected ImportDelegate(IMPORT_DELEGATE_FACTORY factory, Class<ECM_OBJECT> objectClass, CmfObject<VALUE> storedObject) throws Exception {
		super(factory, objectClass);
		this.cmfObject = storedObject;
		this.strategy = factory.getEngine().getImportStrategy(storedObject.getType());
	}

	protected abstract Collection<ImportOutcome> importObject(CmfAttributeTranslator<VALUE> translator, IMPORT_CONTEXT ctx)
		throws ImportException, CmfStorageException;

}