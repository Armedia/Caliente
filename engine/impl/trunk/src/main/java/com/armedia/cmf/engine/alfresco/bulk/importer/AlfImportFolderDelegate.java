package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class AlfImportFolderDelegate extends AlfImportFileableDelegate {

	public AlfImportFolderDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super("arm:folder", factory, storedObject);
	}

	@Override
	protected boolean createStub(File target) throws IOException {
		// Only do this if the target is not a cabinet.
		if ("dm_cabinet".equalsIgnoreCase(this.cmfObject.getSubtype())) { return false; }
		ExportTarget referrent = this.factory.getEngine().getReferrent(this.cmfObject);
		if (referrent != null) {
			switch (referrent.getType()) {
				case FOLDER:
				case DOCUMENT:
					break;
				default:
					// If this folder is referenced by anything other than a folder
					// or another document, we're not interested in creating it.
					return false;
			}
		}
		FileUtils.forceMkdir(target);
		return true;
	}
}