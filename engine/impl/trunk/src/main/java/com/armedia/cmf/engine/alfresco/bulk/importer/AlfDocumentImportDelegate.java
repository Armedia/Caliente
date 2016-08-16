package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class AlfDocumentImportDelegate extends AlfFileableImportDelegate {

	public AlfDocumentImportDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super(factory, storedObject);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		AlfrescoType type = null;

		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			type = this.factory.getType("jsap:rendition");
		} else {
			// Not a rendition or a reference? Fine...let's identify the type
			String srcTypeName = this.cmfObject.getSubtype().toLowerCase();
			String finalTypeName = String.format("jsap:%s", srcTypeName);
			if (this.factory.schema.hasType(finalTypeName)) {
				type = this.factory.schema.buildType(finalTypeName);
			} else {
				type = this.factory.getType("jsap:document");
			}
		}

		return type;
	}

	@Override
	protected boolean createStub(File target) throws IOException {
		// If it's a folder, make the folder...else, make the file
		FileUtils.write(target, this.cmfObject.getLabel());
		return true;
	}
}