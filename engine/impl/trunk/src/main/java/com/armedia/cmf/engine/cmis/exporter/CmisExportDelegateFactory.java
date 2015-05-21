package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.cmf.engine.cmis.CmisSessionWrapper;
import com.armedia.cmf.engine.exporter.ExportDelegateFactory;
import com.armedia.cmf.engine.exporter.ExportException;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportDelegateFactory extends
ExportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	CmisExportDelegateFactory(CmisExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	@Override
	protected CmisExportDelegate<?> newExportDelegate(Session session, CmfType type, String searchKey) throws Exception {
		CmisObject obj = session.getObject(searchKey);
		switch (type) {
			case FOLDER:
				if (obj instanceof Folder) { return new CmisFolderDelegate(this, Folder.class.cast(obj)); }
				throw new ExportException(String.format("Object with ID [%s] (class %s) is not a Folder-type",
					searchKey, obj.getClass().getCanonicalName()));
			case DOCUMENT:
				if (obj instanceof Document) {
					// Is this the PWC? If so, then don't include it...
					Document doc = Document.class.cast(obj);
					if ((doc.isPrivateWorkingCopy() == Boolean.TRUE) || Tools.equals("pwc", doc.getVersionLabel())) {
						// We will not include the PWC in an export
						doc = doc.getObjectOfLatestVersion(false);
						if (doc == null) { return null; }
					}
					return new CmisDocumentDelegate(this, doc);
				}
				throw new ExportException(String.format("Object with ID [%s] (class %s) is not a Document-type",
					searchKey, obj.getClass().getCanonicalName()));
			case USER:
			case GROUP:
			default:
				break;
		}
		return null;
	}
}