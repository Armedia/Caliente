package com.armedia.cmf.engine.alfresco.bulk.importer;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.armedia.cmf.engine.alfresco.bulk.importer.model.AlfrescoType;
import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfContentInfo;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class AlfImportDocumentDelegate extends AlfImportFileableDelegate {

	public AlfImportDocumentDelegate(AlfImportDelegateFactory factory, CmfObject<CmfValue> storedObject)
		throws Exception {
		super("arm:document", factory, storedObject);
	}

	@Override
	protected AlfrescoType calculateTargetType(CmfContentInfo content) throws ImportException {
		if (!content.isDefaultRendition() || (content.getRenditionPage() > 0)) {
			// If this is a rendition or rendition extra page...
			return this.factory.getType("arm:rendition");
		}
		return super.calculateTargetType(content);
	}

	@Override
	protected boolean createStub(File target) throws IOException {
		// If it's a folder, make the folder...else, make the file
		FileUtils.write(target, this.cmfObject.getLabel());
		return true;
	}
}