package com.armedia.cmf.engine.cmis.exporter;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;

public class CmisExportContext extends ExportContext<Session, CmisObject, Property<?>> {

	CmisExportContext(CmisExportContextFactory factory, String rootId, StoredObjectType rootType, Session session,
		Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
	}
}