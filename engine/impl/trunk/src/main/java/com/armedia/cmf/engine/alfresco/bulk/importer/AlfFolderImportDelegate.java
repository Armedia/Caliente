package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class AlfFolderImportDelegate extends AlfFileableImportDelegate {

	public AlfFolderImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
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
	protected void createStub(File target) throws IOException {
		FileUtils.forceMkdir(target);
	}
}