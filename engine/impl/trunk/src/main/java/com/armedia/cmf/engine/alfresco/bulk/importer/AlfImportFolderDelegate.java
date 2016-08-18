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

	public AlfImportFolderDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = null;
		// Not a rendition or a reference? Fine...let's identify the type
		String srcTypeName = this.cmfObject.getSubtype().toLowerCase();
		String finalTypeName = String.format("jsap:%s", srcTypeName);
		if (this.factory.schema.hasType(finalTypeName)) {
			type = this.factory.schema.buildType(finalTypeName);
		} else {
			type = this.factory.getType("jsap:folder");
		}

		return type;
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