package com.armedia.cmf.engine.cmis.importer;

import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Folder;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class CmisFolderDelegate extends CmisFileableDelegate<Folder> {

	public CmisFolderDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Folder.class, storedObject);
	}

	@Override
	protected Folder createNew(CmisImportContext ctx, Folder parent, Map<String, ?> properties) throws ImportException {
		return parent.createFolder(properties);
	}

	@Override
	protected boolean isMultifilable(Folder existing) {
		return false;
	}
}