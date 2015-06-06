package com.armedia.cmf.engine.cmis.importer;

import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;

import com.armedia.cmf.engine.importer.ImportException;
import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfValue;

public class CmisDocumentDelegate extends CmisFileableDelegate<Document> {

	public CmisDocumentDelegate(CmisImportDelegateFactory factory, CmfObject<CmfValue> storedObject) throws Exception {
		super(factory, Document.class, storedObject);
	}

	@Override
	protected Document createNew(CmisImportContext ctx, Folder parent, Map<String, ?> properties)
		throws ImportException {
		// TODO: prepare the content streams
		ContentStream stream = null;
		// TODO: Handle the versioning state
		VersioningState versionState = null;
		return parent.createDocument(properties, stream, versionState);
	}
}