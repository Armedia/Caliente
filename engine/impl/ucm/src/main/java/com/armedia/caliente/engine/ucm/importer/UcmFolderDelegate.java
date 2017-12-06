package com.armedia.caliente.engine.ucm.importer;

import java.util.Collection;
import java.util.Map;

import com.armedia.caliente.engine.importer.ImportException;
import com.armedia.caliente.engine.importer.ImportOutcome;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;

public class UcmFolderDelegate extends UcmFSObjectDelegate<UcmFolder> {

	public UcmFolderDelegate(UcmImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, UcmFolder.class, storedObject);
	}

	@Override
	protected UcmFolder createNew(UcmImportContext ctx, UcmFolder parent, Map<String, Object> properties)
		throws ImportException {
		return null;
	}

	@Override
	protected boolean isMultifilable(UcmFolder existing) {
		return false;
	}

	@Override
	protected Collection<ImportOutcome> importObject(CmfAttributeTranslator<CmfValue> translator, UcmImportContext ctx)
		throws ImportException, CmfStorageException {
		return null;
	}
}