package com.armedia.caliente.engine.ucm.importer;

import com.armedia.caliente.engine.importer.ImportDelegate;
import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.UcmSessionWrapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;

public abstract class UcmImportDelegate<T> extends
	ImportDelegate<T, UcmSession, UcmSessionWrapper, CmfValue, UcmImportContext, UcmImportDelegateFactory, UcmImportEngine> {

	protected UcmImportDelegate(UcmImportDelegateFactory factory, Class<T> objectClass,
		CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, objectClass, storedObject);
	}

}