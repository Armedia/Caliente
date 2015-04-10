package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;

public class CmisExportContext extends ExportContext<Session, CmisObject, StoredValue> {

	CmisExportContext(CmisExportContextFactory factory, String rootId, StoredObjectType rootType, Session session,
		Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
	}
}