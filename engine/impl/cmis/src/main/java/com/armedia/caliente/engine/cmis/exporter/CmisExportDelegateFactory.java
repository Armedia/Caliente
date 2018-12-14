package com.armedia.caliente.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Session;

import com.armedia.caliente.engine.cmis.CmisSessionWrapper;
import com.armedia.caliente.engine.exporter.ExportDelegateFactory;
import com.armedia.caliente.engine.exporter.ExportException;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.CfgTools;
import com.armedia.commons.utilities.Tools;

public class CmisExportDelegateFactory
	extends ExportDelegateFactory<Session, CmisSessionWrapper, CmfValue, CmisExportContext, CmisExportEngine> {

	CmisExportDelegateFactory(CmisExportEngine engine, CfgTools configuration) {
		super(engine, configuration);
	}

	protected final <T> T checkedCast(CmisObject obj, Class<T> klazz, CmfType type, String searchKey)
		throws ExportException {
		if (klazz.isInstance(obj)) { return klazz.cast(obj); }
		throw new ExportException(String.format("Object with ID [%s] (class %s) is not a %s-type (%s archetype)",
			searchKey, obj.getClass().getCanonicalName(), klazz.getSimpleName(), type));
	}

	@Override
	protected CmisExportDelegate<?> newExportDelegate(Session session, CmfType type, String searchKey)
		throws Exception {
		CmisObject obj = session.getObject(searchKey);
		switch (type) {
			case FOLDER:
				return new CmisFolderDelegate(this, session, checkedCast(obj, Folder.class, type, searchKey));

			case DOCUMENT:
				// Is this the PWC? If so, then don't include it...
				Document doc = checkedCast(obj, Document.class, type, searchKey);
				if ((doc.isPrivateWorkingCopy() == Boolean.TRUE) || Tools.equals("pwc", doc.getVersionLabel())) {
					// We will not include the PWC in an export
					doc = doc.getObjectOfLatestVersion(false);
					if (doc == null) { return null; }
				}
				return new CmisDocumentDelegate(this, session, doc);

			case TYPE:
				ObjectType objectType = checkedCast(obj, ObjectType.class, type, searchKey);
				if (objectType.isBaseType()) { return null; }
				return new CmisObjectTypeDelegate(this, session, objectType);

			case USER:
			case GROUP:
			default:
				break;
		}
		return null;
	}
}