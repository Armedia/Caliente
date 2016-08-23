package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.exporter.ExportTarget;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class AlfImportFolderDelegate extends AlfImportFileableDelegate {

	private final AlfrescoType folderType;

	public AlfImportFolderDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(null, factory, storedObject);
		this.folderType = this.factory.schema.buildType("cm:folder", "arm:folder");
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = super.calculateTargetType(content);
		if (type == null) {
			type = this.folderType;
		}
		return type;
	}

	@Override
	protected boolean createStub(File target, String content) throws ImportException {
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

		try {
			FileUtils.forceMkdir(target);
			return true;
		} catch (IOException e) {
			throw new ImportException(String.format("Failed to create the folder for %s [%s](%s) at [%s]",
				this.cmfObject.getType(), this.cmfObject.getLabel(), this.cmfObject.getId(), target.getAbsolutePath()),
				e);
		}
	}
}