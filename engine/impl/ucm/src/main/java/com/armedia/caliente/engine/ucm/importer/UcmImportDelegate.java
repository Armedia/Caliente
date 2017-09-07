package com.armedia.caliente.engine.ucm.importer;

import java.util.Collection;

import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.ucm.IdcSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmImportDelegate<T> extends
	ImportDelegate<T, IdcSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportDelegateFactory, UcmImportEngine> {

	protected UcmImportDelegate(UcmImportDelegateFactory factory, Class<T> objectClass,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, UcmImportContext ctx)
		throws ImportException, CmfStorageException {
		return null;
	}
}